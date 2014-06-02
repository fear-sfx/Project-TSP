package org.projecttsp.websocket

import scala.collection.mutable.Map
import scala.collection.mutable.Set
import akka.actor.Actor
import org.mashupbots.socko.routes.WebSocketFrame
import scala.collection.immutable.Nil
import org.mashupbots.socko.webserver.WebServer
import org.mashupbots.socko.events.WebSocketFrameEvent
import org.projecttsp.clustermatchmaking.ActorMatchmakerMessage
import org.projecttsp.global.ActorGlobalNames
import akka.actor.ActorRef
import akka.actor.ActorLogging
import org.projecttsp.authentication.AuthenticationActor
import org.projecttsp.authentication.AuthenticationActorMessages
import org.projecttsp.global.ClusterInfo


object WebSocketHandlerMessage {
  case class HandleFrame(wsFrame:WebSocketFrameEvent);
  case class HandshakeComplete(webSocketId:String);
  case class WebSocketClose(webSocketId: String);
  case class SuccessfulAuthentication(username:String, webSocketId:String);
  case class BadAuthentication(webSocketId:String);
}

class WebSocketHandler(val webServer: WebServer) extends Actor with ActorLogging {
  val unidentifiedConnectionIDs = Set[String]()
  val identifiedConnectionIDs = Map[String, String]()
  val playerModels = Map[String, PlayerModel]();
  
  var authenticationHandlerActorRef:ActorRef = null;

  override def preStart() {
      println("Starting Websocko Handler")
      val selection = context.actorSelection(ClusterInfo.CLUSTER_MATCHMAKER_ADDRESS);
	  selection ! ActorMatchmakerMessage.NeedMatch(ActorGlobalNames.WEB_SOCKO_HANDLER_ACTOR_NAME, Set(ActorGlobalNames.LOGIN_HANDLER_ACTOR_NAME))
  }
  
  def authenticate(username: String, password: String, webSocketId:String) = {
    assert(authenticationHandlerActorRef != null)
    authenticationHandlerActorRef ! AuthenticationActorMessages.Authenticate(username, password, webSocketId);
  }

  def createMessage(command: String, arguments: List[String]) = {
    (command :: arguments).mkString(" ")
  }
  
  def completeHandshake(webSocketId: String): Unit = {
    unidentifiedConnectionIDs.add(webSocketId)
    webServer.webSocketConnections.writeText("identify", webSocketId)
  }

  def sendBadAuthenticationMessage(webSocketId: String) {
    webServer.webSocketConnections.writeText("bad_credantials", webSocketId)
  }

  def onSuccessfulAuthentication(username:String, webSocketId: String) {
	 unidentifiedConnectionIDs.remove(webSocketId)
	 identifiedConnectionIDs.put(webSocketId, username)
	 sendSuccessfulAuthentication(webSocketId, username);
  }
  
  def sendSuccessfulAuthentication(webSocketId: String, username: String) {
    webServer.webSocketConnections.writeText("successful_authentication " + username, webSocketId)
  }

  def closeSocket(webSocketId: String) = {
    if (unidentifiedConnectionIDs.contains(webSocketId)) {
      unidentifiedConnectionIDs.remove(webSocketId);
    } else if (identifiedConnectionIDs.contains(webSocketId)) {
      val username = identifiedConnectionIDs.get(webSocketId).get
      if (playerModels.contains(username)) {
        playerModels.remove(username)
        val command = "exit_game"
        val message = createMessage(command, List(username))
        webServer.webSocketConnections.writeText(message)
      }
      identifiedConnectionIDs.remove(webSocketId)
    }
  }
  
  
  def handleFrame(wsFrame:WebSocketFrameEvent) {
     if (wsFrame.isText) {
        val splittedMessage = wsFrame.readText.split(" ").toList
        splittedMessage match {
          case head :: tail => {
            if (head == "login" && unidentifiedConnectionIDs.contains(wsFrame.webSocketId) && !identifiedConnectionIDs.contains(wsFrame.webSocketId)) {
              if (tail.size == 2) {
                val username = tail(0)
                val password = tail(1)
                if (!identifiedConnectionIDs.values.toSet.contains(username)) {
                  authenticate(username, password, wsFrame.webSocketId)
                } else {
                  sendBadAuthenticationMessage(wsFrame.webSocketId)
                }
              } else throw new IllegalArgumentException("Bad Formatted Message")
            } else if (identifiedConnectionIDs.contains(wsFrame.webSocketId)) {
              handleMessage(wsFrame);
            } else {
              throw new SecurityException("The sender is not authorized or authenticated to send messaged");
            }
          }
          case _ => throw new IllegalArgumentException("Bad Formatted Message")
        }
      }
  }

  def handleMessage(wsFrame: WebSocketFrameEvent) {
    if (wsFrame.isText) {
      val wsFrameMessage = wsFrame.readText()
      println("Recieved Message " + wsFrameMessage)
      val splittedMessage = wsFrameMessage.split(" ")
      val messageCommand = splittedMessage.head
      val messageArguments = splittedMessage.tail
      messageCommand match {
        case "player_started_game" => {
          assert(messageArguments.length == 5)
          val username = messageArguments(0)
          val characterId = messageArguments(1)
          val x = messageArguments(2)
          val y = messageArguments(3)
          val movement = messageArguments(4)

          val command = "player_started";
          identifiedConnectionIDs.foreach(connectionIdToUsername => {
            if (connectionIdToUsername._1 != wsFrame.webSocketId) {
              val message = createMessage(command, List(username, characterId, x, y, movement))
              println("Sending Message: " + message)
              webServer.webSocketConnections.writeText(message, connectionIdToUsername._1)
            } else {
              if (playerModels.size > 0) {
                playerModels.foreach(userToPlayerModel => {
                  val playerModel = userToPlayerModel._2
                  val message = createMessage(command, playerModel.toList)
                  webServer.webSocketConnections.writeText(message, connectionIdToUsername._1)
                })
              }

            }
          })

          playerModels.put(username, new PlayerModel(username, characterId, x, y, movement))
        }

        case "move" => {
          assert(messageArguments.length == 4)
          val username = messageArguments(0)
          val x = messageArguments(1)
          val y = messageArguments(2)
          val movement = messageArguments(3)
          val characterId = playerModels.get(username).get.characterId
          playerModels.remove(username)

          val command = "player_move"
          val playerModel = new PlayerModel(username, characterId, x, y, characterId)
          playerModels.put(username, playerModel)
          identifiedConnectionIDs.foreach(connectionIdToUsername => {
            if (connectionIdToUsername._1 != wsFrame.webSocketId) {
              val message = createMessage(command, List(username, x, y, movement))
              println("Sending Message: " + message)
              webServer.webSocketConnections.writeText(message, connectionIdToUsername._1)
            }
          })
        }

        case _ => {
          throw new RuntimeException("Unrecognized command: " + messageCommand)
        }
      }
    }
  }
  
  def receive = {
     case ActorMatchmakerMessage.YourMatch(name, actorRef) => {
    	 if (name == ActorGlobalNames.LOGIN_HANDLER_ACTOR_NAME) {
    	   println("I Found my match " + name)
    	   authenticationHandlerActorRef = actorRef
    	   context.become(ready)
    	 }
	 }
     case _ => log.error("WebSocketHandler is not ready")
  }
  
  def ready : Receive = {
    case WebSocketHandlerMessage.HandleFrame(wsFrame) => handleFrame(wsFrame)
    case WebSocketHandlerMessage.HandshakeComplete(webSocketId) => completeHandshake(webSocketId)
    case WebSocketHandlerMessage.WebSocketClose(webSocketId) => closeSocket(webSocketId)
    case WebSocketHandlerMessage.SuccessfulAuthentication(username, webSocketId) => onSuccessfulAuthentication(username, webSocketId)
    case WebSocketHandlerMessage.BadAuthentication(webSocketId) => sendBadAuthenticationMessage(webSocketId)
  }
  
}
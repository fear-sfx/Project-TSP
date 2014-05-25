package org.projecttsp.websocket

import scala.collection.mutable.Map
import scala.collection.mutable.Set
import org.mashupbots.socko.routes.Path
import org.mashupbots.socko.routes.Routes
import org.mashupbots.socko.routes.WebSocketFrame
import org.mashupbots.socko.routes.WebSocketHandshake
import org.mashupbots.socko.webserver.WebServer
import org.mashupbots.socko.webserver.WebServerConfig
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.actorRef2Scala
import scala.collection.immutable.Nil
import org.mashupbots.socko.routes.WebSocketFrame
import org.mashupbots.socko.events.WebSocketFrameEvent
import akka.dispatch.Foreach

class WebSockoNode(actorSystem: ActorSystem) {

  val unidentifiedConnectionIDs = Set[String]()
  val identifiedConnectionIDs = Map[String, String]()
  val playerModels = Map[String, PlayerModel]();

  def onWebSocketHandshakeComplete(webSocketId: String): Unit = {
    unidentifiedConnectionIDs.add(webSocketId)
    webServer.webSocketConnections.writeText("identify", webSocketId)
  }

  def sendBadAuthenticationMessage(webSocketId: String) {
    webServer.webSocketConnections.writeText("bad_credantials", webSocketId)
  }

  def sendSuccessfulAuthentication(webSocketId: String, username: String) {
    webServer.webSocketConnections.writeText("successful_authentication " + username, webSocketId)
  }

  def createMessage(command: String, arguments: List[String]) = {
    (command :: arguments).mkString(" ")
  }

  def onWebSocketClose(webSocketId: String): Unit = {
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

  def authenticate(username: String, password: String): Boolean = {
    //TODO encode and decode the password when sending throw websockets
    //TODO change the simple check with more sophisticated authentication system
    println("Authenticating: " + username + " " + password);
    (username == "Petur" || username == "Todor" || username == "Stefan") && password == "1234"
  }

  val routes = Routes({

    case WebSocketHandshake(wsHandshake) => wsHandshake match {
      case Path("/websockets") => {
        // Store web socket ID for future use
        //wsHandshake.webSocketId     
        println("The Websocket ID:" + wsHandshake.webSocketId);
        // Authorize
        wsHandshake.authorize(
          onComplete = Some(onWebSocketHandshakeComplete),
          onClose = Some(onWebSocketClose))
      }
    }

    case WebSocketFrame(wsFrame) => {
      // Once handshaking has taken place, we can now process frames sent from the client
      if (wsFrame.isText) {
        val splittedMessage = wsFrame.readText.split(" ").toList
        splittedMessage match {
          case head :: tail => {
            if (head == "login" && unidentifiedConnectionIDs.contains(wsFrame.webSocketId) && !identifiedConnectionIDs.contains(wsFrame.webSocketId)) {
              if (tail.size == 2) {
                val username = tail(0)
                val password = tail(1)
                if (!identifiedConnectionIDs.values.toSet.contains(username) && authenticate(username, password)) {
                  unidentifiedConnectionIDs.remove(wsFrame.webSocketId)
                  identifiedConnectionIDs.put(wsFrame.webSocketId, username)
                  sendSuccessfulAuthentication(wsFrame.webSocketId, username);
                  //TODO inform other users that someone has logged in
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

  })

  val webServer: WebServer = new WebServer(WebServerConfig(), routes, actorSystem)

  def start(): Unit = {

    webServer.start()

    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run { webServer.stop() }
    })
  }
}
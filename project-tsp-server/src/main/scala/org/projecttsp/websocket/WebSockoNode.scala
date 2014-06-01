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
  var playerModels = Keeper.getModels

  def onWebSocketHandshakeComplete(webSocketId: String): Unit = {
    unidentifiedConnectionIDs.add(webSocketId)
    webServer.webSocketConnections.writeText("identify", webSocketId)
  }

  def sendBadAuthenticationMessage(webSocketId: String) {
    webServer.webSocketConnections.writeText("bad_credentials", webSocketId)
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
      playerModels = Keeper.getModels
      if (playerModels.contains(username)) {
        playerModels -= username
        Keeper.setModels(playerModels)
    	  val command = "exit_game"
    	  val message = createMessage(command, List(username))
    	  webServer.webSocketConnections.writeText(message)
      }
      identifiedConnectionIDs.remove(webSocketId)
    }
  }

  def handleMessage(wsFrame: WebSocketFrameEvent) {
    val handler = actorSystem.actorOf(Props(new WebSocketHandler(webServer, identifiedConnectionIDs.toMap)))

    playerModels = Keeper.getModels

    handler ! wsFrame
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

  val webServer: WebServer = new WebServer(WebServerConfig("TSP-Server", "192.168.1.2", 8888), routes, actorSystem)

  def start(): Unit = {

    webServer.start()

    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run { webServer.stop() }
    })
  }
}
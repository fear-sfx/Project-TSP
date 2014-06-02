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
import akka.actor.ActorRef

class WebSockoNode(actorSystem: ActorSystem) {

  def onWebSocketHandshakeComplete(webSocketId: String): Unit = {
	webSockoHandler ! WebSocketHandlerMessage.HandshakeComplete(webSocketId)
  }

  def onWebSocketClose(webSocketId: String): Unit = {
    webSockoHandler ! WebSocketHandlerMessage.WebSocketClose(webSocketId)
  }
  
  val routes = Routes({

    case WebSocketHandshake(wsHandshake) => wsHandshake match {
      case Path("/websockets") => {
        wsHandshake.authorize(
          onComplete = Some(onWebSocketHandshakeComplete),
          onClose = Some(onWebSocketClose))
      }
    }

    case WebSocketFrame(wsFrame) => {
     webSockoHandler ! WebSocketHandlerMessage.HandleFrame(wsFrame)
    }

  })

  val webServer: WebServer = new WebServer(WebServerConfig(), routes, actorSystem)
  val webSockoHandler = actorSystem.actorOf(Props(new WebSocketHandler(webServer)));

  def start(): Unit = {
    webServer.start()
    Runtime.getRuntime.addShutdownHook(new Thread {
      override def run { webServer.stop() }
    })
  }
}
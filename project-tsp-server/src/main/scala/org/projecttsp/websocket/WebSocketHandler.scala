package org.projecttsp.websocket

import akka.actor.Actor
import org.mashupbots.socko.routes.WebSocketFrame
import scala.collection.immutable.Nil
import org.mashupbots.socko.webserver.WebServer

class WebSocketHandler(val webServer: WebServer, val identifiedConnectionIDs: Map[String, String]) extends Actor {
  def createMessage(command: String, arguments: List[String]) = {
    (command :: arguments).mkString(" ")
  }

  def receive = {
    case WebSocketFrame(wsFrame) => {
      if (wsFrame.isText) {
        val wsFrameMessage = wsFrame.readText()
        println("Recieved Message " + wsFrameMessage)
        val splittedMessage = wsFrameMessage.split(" ")
        val messageCommand = splittedMessage.head
        val messageArguments = splittedMessage.tail
        messageCommand match {
          case "player_started_game" => {
            assert(messageArguments.length == 5)
            val arguments = Map("username" -> messageArguments(0),
              "characterId" -> messageArguments(1),
              "x" -> messageArguments(2),
              "y" -> messageArguments(3))

            val command = "player_started";

            identifiedConnectionIDs.foreach(tuple => {
              if (tuple._2 != wsFrame.webSocketId) {
                val message = createMessage(command, arguments.values.toList)
                println ("Sending Message: " + message)
                webServer.webSocketConnections.writeText(message, tuple._2)
              }
            })
          }

          case "move" => {

          }

          case _ => {
            throw new RuntimeException("Unrecognized command: " + messageCommand)
          }
        }
      }
    }
  }
}
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

            val username = messageArguments(0)
            val characterId = messageArguments(1)
            val x = messageArguments(2)
            val y = messageArguments(3)
            val movement = messageArguments(4)

            val command = "player_started";

            var models = Keeper.getModels

            identifiedConnectionIDs.foreach(connectionIdToUsername => {
            if (connectionIdToUsername._1 != wsFrame.webSocketId) {
                val message = createMessage(command, arguments.values.toList)
                println ("Sending Message: " + message)
                webServer.webSocketConnections.writeText(message, connectionIdToUsername._1)
              }else{
                models.foreach(playerName => {
                    val arguments = Map("username" -> playerName._1,
                      "characterId" -> models.apply(playerName._1).characterId,
                      "x" -> models.apply(playerName._1).x,
                      "y" -> models.apply(playerName._1).y)
                    
                    val message = createMessage(command, arguments.values.toList)
                    println ("Sending Message: " + message)
                    webServer.webSocketConnections.writeText(message, connectionIdToUsername._1)
                  })
              }
            })
            // println("printim: user: " + username + "; charID: " + characterId)
            val model = new PlayerModel(username, characterId, x, y, movement)
            models += (username -> model)
            Keeper.setModels(models)
          }

          case "move" => {

            assert(messageArguments.length == 4)
            val username = messageArguments(0)
            val x = messageArguments(1)
            val y = messageArguments(2)
            val movement = messageArguments(3)
            var models = Keeper.getModels
            val characterId = models.apply(username).characterId
            // println("unooo: " + models.size)
            models -= username
            // println("dooos: " + models.size)
            val command = "player_move"
            identifiedConnectionIDs.foreach(connectionIdToUsername => {

            val model = new PlayerModel(username, characterId, x, y, movement)
            models += (username -> model)
            Keeper.setModels(models)

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
  }
}
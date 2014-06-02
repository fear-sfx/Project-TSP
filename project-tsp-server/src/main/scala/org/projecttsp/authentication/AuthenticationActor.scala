package org.projecttsp.authentication

import akka.actor.Actor
import org.projecttsp.clustermatchmaking.ActorMatchmaker
import org.projecttsp.clustermatchmaking.ActorMatchmakerMessage
import org.projecttsp.websocket.WebSocketHandlerMessage
import org.projecttsp.global.ActorGlobalNames
import org.projecttsp.global.ClusterInfo

object AuthenticationActorMessages {
	  case class Authenticate(username: String, password: String, webSocketId:String)
	}

class AuthenticationActor extends Actor {

	override def preStart() {
	  val selection = context.actorSelection(ClusterInfo.CLUSTER_MATCHMAKER_ADDRESS);
	  selection ! ActorMatchmakerMessage.ImAlive(ActorGlobalNames.LOGIN_HANDLER_ACTOR_NAME)
	}
  
	def authenticate(username: String, password: String): Boolean = {
	    println("Authenticating: " + username + " " + password);
	    (username == "Petur" || username == "Todor" || username == "Stefan") && password == "1234"
	}
  
	def receive = {
	  case AuthenticationActorMessages.Authenticate(username, password, webSocketId) => {
		  if (authenticate(username, password)) {
		    sender ! WebSocketHandlerMessage.SuccessfulAuthentication(username, webSocketId)
		  } else {
		    sender ! WebSocketHandlerMessage.BadAuthentication(webSocketId)
		  }
	  }
	}
}
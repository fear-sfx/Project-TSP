package org.projecttsp

import akka.actor.ActorSystem
import akka.actor.Props
import org.projecttsp.authentication.AuthenticationActor
import org.projecttsp.global.ActorGlobalNames
import org.projecttsp.global.ClusterInfo

object RunAuthenticationNode {
	def main(args: Array[String]): Unit = {
	  val actorSystem = ActorSystem(ClusterInfo.CLUSTER_NAME)
	  actorSystem.actorOf(Props[AuthenticationActor], name = ActorGlobalNames.LOGIN_HANDLER_ACTOR_NAME)
	}
}
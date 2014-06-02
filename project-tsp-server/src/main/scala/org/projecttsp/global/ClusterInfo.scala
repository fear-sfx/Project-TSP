package org.projecttsp.global

object ClusterInfo {
	val CLUSTER_NAME = "GameCluster"
	val CLUSTER_MATCHMAKER_ADDRESS = "akka.tcp://GameCluster@127.0.0.1:2551/user/"+ActorGlobalNames.MATCH_MAKING_ACTOR
}
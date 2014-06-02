package org.projecttsp

import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.actor.Props
import org.projecttsp.websocket.WebSockoNode
import org.projecttsp.clustermatchmaking.ActorMatchmaker
import org.projecttsp.global.ActorGlobalNames
import org.projecttsp.global.ClusterInfo

object RunSeedNode {

  def main(args: Array[String]): Unit = {
    startup()
  }

  def createClusterActorSystem(port: String) = {
    // Override the configuration of the port
    val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).
      withFallback(ConfigFactory.load())
    ActorSystem(ClusterInfo.CLUSTER_NAME, config)
  }

  def startup(): Unit = {
    val actorSystem = createClusterActorSystem("2551")
    actorSystem.actorOf(Props[SimpleClusterListener], name = "clusterListener")
    actorSystem.actorOf(Props[ActorMatchmaker], name = ActorGlobalNames.MATCH_MAKING_ACTOR)
    
    val webSocketNode = new WebSockoNode(actorSystem);
    webSocketNode.start();
  }

}
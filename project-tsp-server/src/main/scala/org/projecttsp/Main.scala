package org.projecttsp

import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.actor.Props
import org.projecttsp.websocket.WebSockoNode

object Main {

  val clusterName = "ClusterSystem";

  def main(args: Array[String]): Unit = {
    if (args.isEmpty)
      startup(Seq("2551", "2552", "0"))
    else
      startup(args)
  }

  def createClusterActorSystem(port: String) = {
    // Override the configuration of the port
    val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).
      withFallback(ConfigFactory.load())
    ActorSystem(clusterName, config)
  }

  def startup(ports: Seq[String]): Unit = {
    ports foreach { port =>
      // Create an Akka system
      val system = createClusterActorSystem(port)
      // Create an actor that handles cluster domain events
      system.actorOf(Props[SimpleClusterListener], name = "clusterListener")
    }

    val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + 0).
      withFallback(ConfigFactory.load())
    
    val webSocketNode = new WebSockoNode(ActorSystem("WebSocko", config));
    webSocketNode.start();
  }

}
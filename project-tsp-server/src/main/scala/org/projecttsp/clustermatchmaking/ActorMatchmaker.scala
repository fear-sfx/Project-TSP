package org.projecttsp.clustermatchmaking

import scala.collection.mutable.MultiMap
import akka.actor.Actor
import akka.actor.ActorRef
import scala.collection.mutable.HashMap
import scala.collection.mutable.Set

object ActorMatchmakerMessage {
  case class ImAlive(name:String)
  case class NeedMatch(name: String, needs: Set[String])
  case class YourMatch(name: String, actorRef: ActorRef)
}

class ActorMatchmaker extends Actor {
  val nameToActor = scala.collection.mutable.Map[String, ActorRef]()
  val actorNeeds = new HashMap[String, Set[String]] with MultiMap[String, String]

  def tryAnswerSenderNeeds(name:String, needs: Set[String]) = {
    needs.foreach(need => {
      if (nameToActor.contains(need)) {
        sender ! ActorMatchmakerMessage.YourMatch(need, nameToActor.get(need).get)
      } else {
        actorNeeds.addBinding(name, need)
      }
    })
  }

  def tryAnswerOtherNeeds(name: String) = {
    actorNeeds.foreach(tuple => {
      val key = tuple._1
      val needs = tuple._2
      if (needs.contains(name)) {
        actorNeeds.removeBinding(key, name)
        val matcheeRef = nameToActor.get(name).get
        val neederRef = nameToActor.get(key).get
        neederRef ! ActorMatchmakerMessage.YourMatch(name, matcheeRef)
      }
    })
  }

  def receive = {
    case ActorMatchmakerMessage.NeedMatch(name, needs) => {
      println("Need match", name, needs)
      nameToActor.put(name, sender)
      tryAnswerSenderNeeds(name, needs)
      tryAnswerOtherNeeds(name)
    }
    
    case ActorMatchmakerMessage.ImAlive(name) => {
      println(sender + "I`m alive match", name)
      nameToActor.put(name, sender)
      tryAnswerOtherNeeds(name)
    }
  }
}
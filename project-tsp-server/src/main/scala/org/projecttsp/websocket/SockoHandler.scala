package org.projecttsp.websocket

import akka.actor.Actor
import org.mashupbots.socko.events.HttpRequestEvent
import java.util.Date

class SockoHandler extends Actor {
	def receive = {
	  case event: HttpRequestEvent =>
	    event.response.write("Hello from Socko (" + new Date().toString + ")")
        context.stop(self)
	}
}
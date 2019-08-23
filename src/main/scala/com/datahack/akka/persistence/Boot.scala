package com.datahack.akka.persistence

import akka.actor.{ActorSystem, Props}
import com.datahack.akka.persistence.actors.BasketActor
import com.datahack.akka.persistence.actors.BasketActor.{Boom, Cmd, Print, Snap}

object Boot extends App {

  val actorSystem = ActorSystem("Persistence-Actor-System")
  val persistentActor = actorSystem.actorOf(Props[BasketActor], name = "BasketActor")

  persistentActor ! Print
  persistentActor ! Cmd("foo")
  persistentActor ! Cmd("bar")
  persistentActor ! Boom
  persistentActor ! Cmd("love")
  persistentActor ! Snap
  persistentActor ! Cmd("buzz")
  persistentActor ! Print

  sys.addShutdownHook(actorSystem.terminate())

}

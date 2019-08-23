package com.datahack.akka.failrecovey.actors

import akka.actor.SupervisorStrategy.{Restart, Resume, Stop}
import akka.actor.{Actor, ActorLogging, OneForOneStrategy, Props}
import com.datahack.akka.failrecovey.errors.{RestartMeException, ResumeMeException, StopMeException}

class PrinterActorSupervisor extends Actor with ActorLogging {

  override def preStart() = log.info("The Supervisor is ready to supervise")
  override def postStop() = log.info("Bye Bye from the Supervisor")

  // supervisorStrategy() reaccionará a las diferentes excepciones que pueden lanzarse.
  // Definimos esta función como OneForOneStrategy() para que este supervisor sólo actue sobre el actor que ha fallado.
  override def supervisorStrategy = OneForOneStrategy() {
    case _: RestartMeException => Restart
    case _: ResumeMeException => Resume
    case _: StopMeException => Stop
  }

  // Este supervisor crea un actor hijo PrinterActor, cuyo ciclo de vida supervisará.
  val printer = context.actorOf(Props[PrinterActor], "printer-actor")

  // Cuando este supervisor reciba un mensaje, simplemente lo redirige a su hijo, PrinterActor.
  // forward es una palabra reservada, equivalente a hacer un tell.
  override def receive: Receive = {
    case msg => printer forward msg
  }

}
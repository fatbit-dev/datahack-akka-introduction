package com.datahack.akka.failrecovery.actors

import akka.actor.SupervisorStrategy.{Decider, Restart, Resume, Stop}
import akka.actor.{ActorSystem, Props}
import akka.testkit.{TestActorRef, TestKit}
import com.datahack.akka.failrecovey.actors.PrinterActorSupervisor
import com.datahack.akka.failrecovey.errors.{RestartMeException, ResumeMeException, StopMeException}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class PrinterActorSupervisorSpec
  extends TestKit(ActorSystem("PrinterActorSupervisorSpec"))
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll {

  val supervisor: TestActorRef[PrinterActorSupervisor] =
    TestActorRef[PrinterActorSupervisor](Props(classOf[PrinterActorSupervisor]))

  val strategy: Decider = supervisor.underlyingActor.supervisorStrategy.decider

  // Esto es básicamente lo que nos permite probar Akka con respecto al ciclo de vida de un actor y su política de
  // supervisión de actores.

  "PrinterActorSupervisor" should {

    "Take restart strategy if supervised actor throws a RestartMeException" in {
      strategy(new RestartMeException) should be (Restart)
    }

    "Take restart strategy if supervised actor throws a ResumeMeException" in {
      strategy(new ResumeMeException) should be (Resume)
    }

    "Take restart strategy if supervised actor throws a StopMeException" in {
      strategy(new StopMeException) should be (Stop)
    }

  }

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

}

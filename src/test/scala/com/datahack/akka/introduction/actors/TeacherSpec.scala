package com.datahack.akka.introduction.actors

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{TestActorRef, TestKit, TestProbe}
import main.scala.com.datahack.akka.introduction.actors.Teacher
import main.scala.com.datahack.akka.introduction.actors.Teacher.{Advice, AskAdvice, IDoNotUnderstand}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec, WordSpecLike}

import scala.concurrent.duration._

class TeacherSpec
    extends TestKit(ActorSystem("TeacherSpec"))
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll {

    val teacherActor: TestActorRef[Teacher] = TestActorRef[Teacher](new Teacher())

    "Teacher Actor" should {
        "send a response message to the sender of the message AskAdvice" in {
            val sender = TestProbe()
            implicit val senderRef: ActorRef = sender.ref

            teacherActor ! AskAdvice("Maths")

            sender.expectMsg[Advice](5 seconds, Advice("Anything worth doing is worth overdoing"))
        }

        "send IDoNotUnderstand response message to the sender of the message AskAdvice when the topic is unknown" in {
            val sender = TestProbe()
            implicit val senderRef: ActorRef = sender.ref

            teacherActor ! AskAdvice("Tortilla")

            sender.expectMsgType[IDoNotUnderstand.type](5 seconds) // Usamos el .type porque es un case object
        }
    }

    override protected def afterAll(): Unit = {
        TestKit.shutdownActorSystem(system)
    }
}

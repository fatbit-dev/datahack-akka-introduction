package com.datahack.akka.introduction.actors

import akka.actor.ActorSystem
import akka.testkit.{EventFilter, TestActorRef, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import main.scala.com.datahack.akka.introduction.actors.{Student, Teacher}
import main.scala.com.datahack.akka.introduction.actors.Student.PerformAnAdviceRequest
import main.scala.com.datahack.akka.introduction.actors.Teacher.AskAdvice
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._

class StudentSpec
    extends TestKit(
        ActorSystem("StudentSpec",
            ConfigFactory.parseString(
                """akka.loggers = ["akka.testkit.TestEventListener"]
                  | akka.test.filter-leeway = 5000
                """.stripMargin))
    )
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll {

    val teacherMock: TestProbe = TestProbe()
    val studentActorRef = TestActorRef[Student](new Student(teacherMock.ref))

    val teacherActor = TestActorRef[Teacher](new Teacher())
    val studentActorWithTeacherActor = TestActorRef[Student](new Student(teacherActor))

    "Student Spec" should {
        "send and AskAdvice message to the Teacher Actor when the Student receives the PerformAnAdviceRequest message" in {
            studentActorRef ! PerformAnAdviceRequest

            teacherMock.expectMsgType[AskAdvice](5 seconds)
        }

        "receives and Advice Message from the Teacher Actor when the Student asks for it" in {

            teacherActor.underlyingActor.advices = teacherActor.underlyingActor.advices ++
                Map[String, String] ("Biology" -> "Biology Rocks dude!") // he añadido el topic que no existía para evitar que lleguen mensajes IDoNotUnderstand

            EventFilter.info(pattern = "The requested advice is:*", occurrences = 1) intercept {
                studentActorWithTeacherActor ! PerformAnAdviceRequest
            }
        }

        "receives an IDoNotUnderstand Message from the Teacher Actor when the Student asks about an unkown" +
            "topic for the Teacher" in {
            teacherActor.underlyingActor.advices = Map.empty[String, String]

            EventFilter.error(message = "Oooops, I do not know what happens here!", occurrences = 1) intercept {
                studentActorWithTeacherActor ! PerformAnAdviceRequest
            }
        }
    }


    override protected def afterAll(): Unit = {
        TestKit.shutdownActorSystem(system)
    }
}

package main.scala.com.datahack.akka.introduction.actors

import akka.actor.{Actor, ActorLogging, ActorRef}
import main.scala.com.datahack.akka.introduction.actors.Student.PerformAnAdviceRequest
import main.scala.com.datahack.akka.introduction.actors.Teacher.{Advice, AskAdvice, IDoNotUnderstand}
import org.scalacheck.Gen


object Student {
    case object PerformAnAdviceRequest
}

class Student(teacher: ActorRef) extends Actor with ActorLogging {

    log.debug(s"${self.path} actor created")

    val genTopics: Gen[String] = Gen.oneOf(
        "History", "Maths", "Geography", "Physics", "Literature", "Biology")

    override def receive: Receive = {
        case PerformAnAdviceRequest =>
            val topic = genTopics.sample.get
            teacher ! AskAdvice(topic)

        case Advice(text) =>
            log.info(s"The requested advice is: $text")

        case IDoNotUnderstand =>
            log.error(s"Oooops, I do not know what happens here!")
    }
}

package main.scala.com.datahack.akka.introduction.actors

import akka.actor.{Actor, ActorLogging}
import main.scala.com.datahack.akka.introduction.actors.Teacher.{Advice, AskAdvice, IDoNotUnderstand}

object Teacher {
    case class AskAdvice(topic: String)
    case class Advice(text: String)
    case object IDoNotUnderstand
}

class Teacher extends Actor with ActorLogging {

    // self is an ActorRef (is a reference to itself). It's an Akka thing, not the same as "this" object.
    log.debug(s"${self.path} actor created")

    var advices: Map[String, String] = Map[String, String] (
        "History" -> "Moderation is for cowards",
        "Maths" -> "Anything worth doing is worth overdoing",
        "Physics" -> "The trouble is you think you have time",
        "Literature" -> "You never gonna know if you never even try",
        "Geography" -> "Anything worth doing is worth overdoing"
    )

    override def receive: Receive = {
        case AskAdvice(topic) =>
            val response = advices.get(topic).map(advice => Advice(advice)).getOrElse(IDoNotUnderstand)
            sender ! response
    }
}

package com.datahack.akka.introduction

import akka.actor.{ActorRef, ActorSystem, Props}
import main.scala.com.datahack.akka.introduction.actors.Student.PerformAnAdviceRequest
import main.scala.com.datahack.akka.introduction.actors.{Student, Teacher}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

object Boot extends App {

    val actorSystem = ActorSystem("UniversityMessagesSystem")

    implicit val executionContext: ExecutionContextExecutor = actorSystem.dispatcher

    // path: /user/teacher
    val teacherActorRef: ActorRef = actorSystem.actorOf(Props[Teacher], "teacher")
    // path: /user/student
    val studentActorRef: ActorRef =
        actorSystem.actorOf(Props(classOf[Student], teacherActorRef), "student")

    // The scheduler of the actor system will execute this timed task
    actorSystem.scheduler.schedule(
        5 seconds,
        15 seconds,
        studentActorRef,
        PerformAnAdviceRequest)

    // Handles SIGTERM and properly terminate the actor system
    sys.addShutdownHook(actorSystem.terminate())
}

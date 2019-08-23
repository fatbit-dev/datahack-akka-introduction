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
        // Lo que hemos hecho aquí es sobreescribir on-the-fly la configuración por defecto para el ActorSystem.
        //
        // Concretamente, hemos sobreescrito el "akka.loggers", y hemos elegido el "akka.testkit.TestEventListener" porque
        // va a permitir al test interceptar los mensajes de log, y poder hacer el match con el texto que queremos/esperemos.
        // Esto es un poco cutre, pero es la forma que tenemos de hacerlo con akka.test. Abajo se explica un poquito más.
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

            // teacherMock no tiene la lógica de un actor Teacher, por eso hemos creado arriba una referencia al
            // teacherActor, que usamos justo aquí.

            // Otra cosa, hemos añadido el Advice que nos faltaba. Esto es porque en la función gentTopics() del Student,
            // habíamos puesto un topic que no existía en el Teacher (ese topic es "Biology").
            teacherActor.underlyingActor.advices = teacherActor.underlyingActor.advices ++
                Map[String, String] ("Biology" -> "Biology Rocks dude!") // he añadido el topic que no existía para evitar que lleguen mensajes IDoNotUnderstand

            // Esta es una de las limitaciones de Akka-Test. ¿Cómo podemos comprobar que al student le llega la
            // respuesta del teacher? No hay una forma evidente, por eso tenemos que usar el logger (esto es cutre, ya).
            // Si recordamos, en el método receive() de la clase Student, teníamos un match para mensajes de tipo Advice:
            //
            //    case Advice(text) =>
            //            log.info(s"The requested advice is: $text")
            //
            // y lo que hacíamos era logging.
            // Usamos el .info porque conretamente ese mensaje de log, en la clase Student, lo imprimíamos como un INFO.
            // Además, este mensaje de log sólo debería ocurrir 1 vez.
            EventFilter.info(pattern = "The requested advice is:*", occurrences = 1) intercept {
                studentActorWithTeacherActor ! PerformAnAdviceRequest
            }
        }

        "receives an IDoNotUnderstand Message from the Teacher Actor when the Student asks about an unkown" +
            "topic for the Teacher" in {
            teacherActor.underlyingActor.advices = Map.empty[String, String]

            // Aquí hacemos algo parecido a lo de antes, comprobando que se hace logging del "Ooooops ..." Pero recogemos
            // un mensaje de tipo ERROR, y no INFO, porque este "Ooooops ..." se imprimía en el log como un ERROR.
            EventFilter.error(message = "Oooops, I do not know what happens here!", occurrences = 1) intercept {
                studentActorWithTeacherActor ! PerformAnAdviceRequest
            }
        }
    }


    override protected def afterAll(): Unit = {
        TestKit.shutdownActorSystem(system)
    }
}

package com.datahack.akka.http.controller.actors

import akka.actor.Actor
import akka.pattern.pipe
import com.datahack.akka.http.controller.actors.UserControllerActor._
import com.datahack.akka.http.model.dtos.User
import com.datahack.akka.http.service.UserService

import scala.concurrent.ExecutionContextExecutor

object UserControllerActor {  // En terminología Java, esto sería una fachada (Facade).
  case object GetAllUsers
  case class SearchUser(id: Long)
  case class CreateUser(user: User)
  case class UpdateUser(user: User)
  case class DeleteUser(id: Long)
}

class UserControllerActor(userService: UserService) extends Actor {

  implicit val executionContext: ExecutionContextExecutor = context.dispatcher

  override def receive: Receive = {

    // Cuando se resuelva el Futuro de userService.users(), devolvemos el resultado al sender (le enviamos un mensaje
    // con el SUCCESS o con el FAILURE).
    case GetAllUsers => userService.users() pipeTo sender
    case SearchUser(id) => userService.searchUser(id) pipeTo sender
    case CreateUser(user) => userService.insertUser(user) pipeTo sender
    case UpdateUser(user) => userService.updateUser(user) pipeTo sender
    case DeleteUser(id) => userService.deleteUser(id) pipeTo sender
  }
}

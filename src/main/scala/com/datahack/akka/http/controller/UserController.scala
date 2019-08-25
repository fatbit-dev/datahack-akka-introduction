package com.datahack.akka.http.controller

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server
import akka.http.scaladsl.server.{Directives, Route}
import akka.util.Timeout
import akka.pattern.ask
import com.datahack.akka.http.controller.actors.UserControllerActor._
import com.datahack.akka.http.model.dtos.{JsonSupport, User}
import com.datahack.akka.http.service.UserService._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

// Aquí están todos los métodos para atender a las peticiones REST.
class UserController(userActor: ActorRef)
                    (implicit executionContext: ExecutionContext)
  extends Directives  // Para poder usar path().
    with JsonSupport {

  // Debemos usar un timeout porque estamos usando un ASK, y es el tiempo que esperaremos aq que el Future se resuelva.
  // Lo hacemos implícito para que esté disponible en todo nuestro controlador.
  // Esto sería mejor que estuviese en un fichero de configuración.
  implicit val timeout: Timeout = Timeout(60 seconds)

  val routes: Route = getAllUsers ~ getUser ~ insertUser ~ updateUser ~ deleteUser

  def getAllUsers: server.Route =
    path("users") {
      get {
        onSuccess(userActor ? GetAllUsers) {
          case AllUsers(users) => complete(users) // Esto funciona porque pilla el userFormat implícito, definido en el trait de JsonSupport de lod DTOs.
          case _ => complete(StatusCodes.InternalServerError)
        }
      }
    }

  def getUser: server.Route =
    path("users" / LongNumber) { userId =>
      get {
        onSuccess(userActor ? SearchUser(userId)) {
          case FoundUser(user) => complete(user)
          case UserNotFound => complete(StatusCodes.NotFound)
          case _ => complete(StatusCodes.InternalServerError)
        }
      }

    }

  def insertUser: server.Route =
    path("users") {
      post {
        entity(as[User]) { user =>
          onSuccess(userActor ? CreateUser(user)) {
            case StoredUser(user) => complete(user)
            case _ => complete(StatusCodes.InternalServerError)
          }
        }
      }
    }

  def updateUser: server.Route =
    path("users" / LongNumber) { userId =>
      put {
        entity(as[User]) { user =>
          // Lo más probable es que el user que nos pasan en el BODY, no tenga el atributo id, ya que el id del usuario
          // se está especificando en la ruta (/users/25, p.ej.). Por lo tanto, para pasárselo al userActor
          onSuccess(userActor ? UpdateUser(user.copy(id = Some(userId)))) {
            case UpdatedUser(user) => complete(user)
            case UserNotFound => complete(StatusCodes.NotFound)
            case _ => complete(StatusCodes.InternalServerError)
          }

        }
      }
    }

  def deleteUser: server.Route = {
    path("users" / LongNumber )  { userId =>
      delete {
        onSuccess(userActor ? DeleteUser(userId)) {
          case UserDeleted => complete(StatusCodes.OK)
          case UserNotFound => complete(StatusCodes.NotFound)
          case _ => complete(StatusCodes.InternalServerError)
        }
      }

    }
  }

}


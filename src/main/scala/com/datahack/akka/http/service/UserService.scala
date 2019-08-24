package com.datahack.akka.http.service

import com.datahack.akka.http.model.daos.UserDao
import com.datahack.akka.http.model.dtos.User
import com.datahack.akka.http.service.UserService.{UserServiceResponse, _}

import scala.concurrent.{ExecutionContext, Future}

/*
 * Mensajes de respuesta de los métodos del servicio de usuarios.
 *
 * Todos extienden el trait UserServiceResponse, y esto nos va a permitir que los métodos del UserService
 * devuelvan todas un Future[UserServiceResponse] (que puede luego ser interpretado/casting como cualquiera
 * de los mensajes).
 */
object UserService {

  trait UserServiceResponse
  case class AllUsers(users: Seq[User]) extends UserServiceResponse
  case class FoundUser(user: User) extends UserServiceResponse
  case object UserNotFound extends UserServiceResponse
  case class StoredUser(user: Option[User]) extends UserServiceResponse
  case class UpdatedUser(user: User) extends UserServiceResponse
  case object UserDeleted extends UserServiceResponse
}

class UserService(userDao: UserDao) {

  // Todos estos métodos reciben un executionContext implícito. Esto es porque todos los métodos del DAO devuelven
  // futuros, y cada vez que vayamos a hacer un .map() por ejemplo.

  def users()(implicit executionContext: ExecutionContext): Future[UserServiceResponse] = {
    // Hacemos el .map() porque getAll() es una mónada, un futuro, y tiene el método .map() implementado.
    userDao.getAll.map(AllUsers)  // O más verboso: userDao.getAll.map(users => AllUsers(users))
  }

  def searchUser(id: Long)(implicit executionContext: ExecutionContext): Future[UserServiceResponse] = {
    // userDao.getById(id) ---> Nos devuelve un Future[Option[User]], por eso podemos aplicarle un .map().
    // userDao.getById(id).map() ---> Al aplicarle el .map(), nos devuelve un Option[User].
    // userDao.getById(id).map(_.map()) ---> Aplicamos el segundo .map() sobre lo que nos llegue (un Option[User]]),
    //   por lo que si ese option nos devuelve Some, devolveremos el FoundUser, y si nos devuelve None, devolveremos
    //   un UserNotFound. ¡Recordemnos que tanto un Future como un Option son mónadas!
    userDao.getById(id).map(_.map(FoundUser).getOrElse(UserNotFound))
  }

  def insertUser(user: User)(implicit executionContext: ExecutionContext): Future[UserServiceResponse] =  {
    // userDao.insert(user) ---> Nos devuelve el id del usuario si la inserción en DB ha ido bien.
    //   Si no, el Future fallará.

    for {
      id <- userDao.insert(user)
      userInserted <- userDao.getById(id)
    } yield StoredUser(userInserted)

    // El yield nos asegura que el StoredUser(user) se "inserte" en un Future, que es lo que queremos devolver.
  }

  def updateUser(user: User)(implicit executionContext: ExecutionContext): Future[UserServiceResponse] =  {
    (for {
      // Aquí usamos el user.id.get porque el id podría ser un None, pero yo sólo quiero seguir si el usuario ha sido
      // encontrado y me ha llegado Some.
      // En el caso de llegar un None, se rompe el for-comprehension y se lanza una excepción, que trataremos más
      // abajo en la parte de recover.
      userFound <- userDao.getById(user.id.get)
      if userFound.isDefined
      _ <- userDao.update(user)  // este update() devuelve el número de filas afectadas en la tabla de la DB.
      updatedUser <- userDao.getById(user.id.get) ///
    } yield
      updatedUser.map(UpdatedUser).get) recover { ///
      ///UpdatedUser(user)) recover {
      case _: NoSuchElementException => UserNotFound
      case e: Exception => throw e
    }
  }

  def deleteUser(id: Long)(implicit executionContext: ExecutionContext): Future[UserServiceResponse] = {
    (for {
      userFound <- userDao.getById(id)
      if userFound.isDefined
      _ <- userDao.delete(id)
    } yield UserDeleted ) recover {
      case _: NoSuchElementException => UserNotFound
    }
  }
}

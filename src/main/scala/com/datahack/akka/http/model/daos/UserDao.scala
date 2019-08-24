package com.datahack.akka.http.model.daos

import com.datahack.akka.http.model.dtos.User
import slick.lifted.{ProvenShape, Tag}
import slick.jdbc.H2Profile.api._

import scala.concurrent.Future

class UserDao {
  // Usamos una DB en memoria, llamada H2. Se crea cada vez que se lanza la app.
  val db = Database.forConfig("h2mem1")

  lazy val users: TableQuery[UserTable] = TableQuery[UserTable]

  // Al hacer que todos los métodos devuelvan Futuros, estamos usando programación reactiva. Cada petición
  // se ejecutará en un hilo diferente, de forma concurrente.
  def getAll: Future[Seq[User]] = db.run(users.result)

  def getById(id: Long): Future[Option[User]] =
    db.run(users.filter(_.id === id).result.headOption)

  def insert(user: User): Future[Long] =
    db.run(users returning users.map(_.id) += user)

  def update(user: User): Future[Int] = {
    db.run(users.filter(_.id === user.id).update(user))
    // Este método devuelve un Future[Int], que representa el número de filas que han sido actalizadas en la DB.
  }

  def delete(id: Long): Future[Int] = db.run(users.filter(_.id === id).delete)

}

// Esta clase UserTable lo que hace es mapear nuestra case-class (User) con la tabla de la DB.
class UserTable(tag: Tag) extends Table[User](tag, "user") {

  def id: Rep[Long] = column[Long]("id", O.PrimaryKey, O.AutoInc)

  def name: Rep[String] = column[String]("name")

  def email: Rep[String] = column[String]("email")

  def password: Rep[String] = column[String]("password")

  def * : ProvenShape[User] = (id.?, name, email, password) <> ((User.apply _).tupled, User.unapply)
}

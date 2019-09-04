package com.datahack.akka.http.utils

import com.datahack.akka.http.model.dtos.User
import com.datahack.akka.http.model.dtos.Product
import org.scalacheck.Gen

trait Generators {

  // Para los generadores, usamos Gen, que nos devuelve mónadas, e implementan métodos
  // como map() o flatMap(). Por eso puedo usarlo dentro de un for-comprenhension.

  // Genera nombres de usuario aleatorios: "user1", "user2", ... "user100"
  val genUserName: Gen[String] = for {
    id <- Gen.chooseNum(1, 100)
  } yield { s"user$id"}

  // Genera passwords
  val genPassword: Gen[String] = for {
    // La longitud es de 4 a 7 caracteres
    length <- Gen.chooseNum(4, 7)
    // Genero un UUID
    password <- Gen.uuid
    // Devuelvo los "length" primeros caracters del UUID
  } yield password.toString.substring(0, length)

  // Genera un usuario
  val genUser: Gen[User] = for {
    name <- genUserName
    password <- genPassword
  } yield {
    // Cuando genero el usuario, asigno None al id, para estar seguro de que la DB
    // se comporta bien al insertar un usuario, cuyo id suele ser un campo AUTO-INCREMENT.
    User(
      id = None,
      name = name,
      email = s"$name@mail.com",
      password = password
    )
  }

  def genProductName: Gen[String] = for {
    id <- Gen.chooseNum(1, 100)
  } yield s"Product $id"

  def genProducerName: Gen[String] = for {
    id <- Gen.chooseNum(1, 100)
  } yield s"Producer $id"

  def genProduct: Gen[Product] = for {
    name <- genProductName
    producer <- genProducerName
    price <- Gen.chooseNum(0.0F, 50.0F)
    units <- Gen.chooseNum(0.0F, 100.0F)
  } yield {
    Product(
      id = None,
      producer = producer,
      name = name,
      description = s"Description of $name",
      price = price,
      units = units
    )
  }
}

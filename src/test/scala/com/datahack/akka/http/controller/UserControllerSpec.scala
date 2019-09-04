package com.datahack.akka.http.controller

import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import akka.testkit.TestActorRef
import com.datahack.akka.http.controller.actors.UserControllerActor
import com.datahack.akka.http.model.daos.UserDao
import com.datahack.akka.http.model.dtos.{JsonSupport, User}
import com.datahack.akka.http.service.UserService
import com.datahack.akka.http.utils.{Generators, SqlTestUtils}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import spray.json._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class UserControllerSpec
  extends WordSpec
    with Matchers
    with Directives         // De Akka-HTTP Test kit, nos permite probar las directivas
    with ScalatestRouteTest // Para probar las rutas
    with BeforeAndAfterAll  // Para poder generar/destruir la DB en beforeAll()/afterAll()
    with Generators         // Los generadores que hemos creado antes
    with JsonSupport {      // Los conversores que hemos configurado antes (para un/marshalling)

  var schemaName: String = ""

  //implicit val tiemout = RouteTestTimeout(5 senconds)

  var users: Seq[User] = (1 to 5).map(i => genUser.sample.get.copy(id = Some(i)))

  val userDao = new UserDao()
  val userService = new UserService(userDao)
  val userControllerActor = TestActorRef[UserControllerActor](new UserControllerActor(userService))
  val userController = new UserController(userControllerActor)

  override protected def beforeAll(): Unit = {
    // Recogemos el Futuro de initDatabase() con un Await
    schemaName = Await.result(SqlTestUtils.initDatabase(), 5 seconds)
    // Future.sequence() transforma una Lista de Futuros en un Futuro de Listas
    Await.result(Future.sequence(SqlTestUtils.insertList(users.toList, schemaName)), 5 seconds)
  }

  "User Controller" should {

    "get all users signed up in the application" in {
      Get("/users") ~> userController.routes ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[Seq[User]]
        response.length shouldBe users.length
      }
    }

    "get specific signed up user by it id" in {
      Get(s"/users/${users.head.id.get}") ~> userController.routes ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[User]
        response shouldBe users.head
      }
    }

    "get not found status code when shearching a user that is not signed up" in {
      Get(s"/users/${users.length + 20}") ~> userController.routes ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }

    "sign up a new user" in {
      val userToStore = genUser.sample.get
      Post(s"/users").withEntity(
        ContentTypes.`application/json`,
        userToStore.toJson
      ) ~> userController.routes ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[User]

        response.id shouldNot be(None)
        response.name shouldBe userToStore.name
        response.email shouldBe userToStore.email
        response.password shouldBe userToStore.password
      }
    }

    "update the user data" in {
      val userToUpdate = users.last.copy(name = "new name")
      Put(s"/users/${userToUpdate.id.get}").withEntity(
        ContentTypes.`application/json`,
        userToUpdate.toJson
      ) ~> userController.routes ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[User]

        response shouldBe userToUpdate
      }
    }

    "get Not Found status code when trying to update a user that is not signed up" in {
      val userToUpdate = users.last.copy(name = "new name")
      Put(s"/users/${users.length + 20}").withEntity(
        ContentTypes.`application/json`,
        userToUpdate.toJson
      ) ~> userController.routes ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }

    "delete a signed user" in {
      val userToDelete = genUser.sample.get
      val id = Await.result(SqlTestUtils.insert(userToDelete, schemaName), 5 seconds)
      Delete(s"/users/$id") ~> userController.routes ~> check {
        status shouldBe StatusCodes.OK
      }
      val result = Await.result(SqlTestUtils.findEntity(userToDelete.copy(id = Some(id)), schemaName), 5 seconds)
      result shouldBe None
    }

    "get Not Found status code when trying to delete a user not signed up into the app" in {
      Delete(s"/users/${users.length + 20}") ~> userController.routes ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }
  }
}

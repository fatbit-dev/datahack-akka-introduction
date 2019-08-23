package com.datahack.akka.persistence.actors

import akka.actor.ActorLogging
import akka.persistence.{PersistentActor, SaveSnapshotFailure, SaveSnapshotSuccess, SnapshotOffer}
import com.datahack.akka.persistence.actors.BasketActor._


object BasketActor {
  // Mensajes que va a recibir un BasketActor, y que vamos a almacenar en el estado (journal).
  case class Cmd(data: String)    // Command.
  case class Evt(data: String)    // Event.
  case object Print               // Imprime el estado del actor.
  case object Snap                // Hace un snapshot del actor.
  case object Boom                // Lanza una excepción para que el actor se reinicie.
  case class State(state: String) // Lo vamos a usar para recuperar el estado.

  // Clase para mantener el estado del actor (encapsulamos el estado del actor dentro de esta clase).
  case class BasketActorState(events: List[String] = Nil) {
    // Esto es lo que se hace normalmente cuando tenemos un PersistenActor, de tal forma que lo que se persiste es esta
    // clase entera (en lugar de ir persistiendo campo a campo, y es por eso que el estado -variables internas de la
    // clase- no se ha declarado en el PersistentActor).

    // El método update(), añade el evento recibido a la lista de eventos, y devuelve una nueva instancia del estado,
    // y así el estado (esa lista de eventos) lo mantenemos inmutable.
    // El parámetro "evt" es el estado ya persistido.
    def update(evt: Evt): BasketActorState = copy(evt.data :: events)
    def size: Int = events.length
    override def toString: String = events.reverse.toString()
  }
}


class BasketActor extends PersistentActor with ActorLogging {

  var state = BasketActorState() // Usa el método apply() del companion object. Se irá actualizando, por eso es una var.

  def updateState(event: Evt): Unit = state = state.update(event)

  def numEvents = state.size

  override def persistenceId: String = "basket-persistent-actor"

  override def receiveRecover: Receive = {
    // Como en receiveCommand() persistimos un evento, cuando queramos recuperar el estado del journal tendremos
    // que recuperar ese evento.
    case evt: Evt =>
      updateState(evt)

    case SnapshotOffer(_, snapshot: BasketActorState) =>
      log.info(s"Offered state = $snapshot")
      state = snapshot
  }

  override def receiveCommand: Receive = {
    // Cuando llega un comando Cmd, lo persistimos:
    case Cmd(data) =>
      val event: Evt = Evt(s"$data - $numEvents")
      val newEvent: Evt = Evt(s"$data - ${numEvents + 1}")

      // Persisitimos el evento en el journal.
      persist(event)(updateState)  // o más verboso: persist(event)(evt => updateState(evt))
      persist(newEvent)(updateState)

      // Contestamos al sender con el evento y con el nuevo evento.
      sender ! event
      sender ! newEvent

    // Cuando nos llega un Snap, persistimos el estado (y se borra el journal que tenía).
    case Snap => saveSnapshot(state)

    case SaveSnapshotSuccess(metadata) =>
      log.info(s"SaveSnapshotSuccess: metadata = $metadata")

    case SaveSnapshotFailure(metadata, reason) =>
      log.info(s"SaveSnapshotFailure: metadata = $metadata, reason = $reason")

    case Print =>
      log.info(s"Print: $state")
      sender ! State(state.toString)

    case Boom =>
      throw  new Exception("BOOOOOM!")

  }


}

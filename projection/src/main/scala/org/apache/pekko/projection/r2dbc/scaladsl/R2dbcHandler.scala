/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

/*
 * Copyright (C) 2021 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.projection.r2dbc.scaladsl

import scala.concurrent.Future

import org.apache.pekko
import pekko.Done
import pekko.annotation.ApiMayChange
import pekko.projection.scaladsl.HandlerLifecycle

/**
 * Implement this interface for the Envelope handler for R2DBC Projections.
 *
 * It can be stateful, with variables and mutable data structures. It is invoked by the `Projection` machinery one
 * envelope at a time and visibility guarantees between the invocations are handled automatically, i.e. no volatile or
 * other concurrency primitives are needed for managing the state.
 *
 * Supported error handling strategies for when processing an `Envelope` fails can be defined in configuration or using
 * the `withRecoveryStrategy` method of a `Projection` implementation.
 */
@ApiMayChange
trait R2dbcHandler[Envelope] extends HandlerLifecycle {

  /**
   * The `process` method is invoked for each `Envelope`. Each time a new `Connection` is passed with a new open
   * transaction. You can use `createStatement`, `update` and other methods provided by the [[R2dbcSession]]. The
   * results of several statements can be combined with `Future` composition (e.g. `flatMap`). The transaction will be
   * automatically committed or rolled back when the returned `Future[Done]` is completed.
   *
   * One envelope is processed at a time. It will not be invoked with the next envelope until after this method returns
   * and the returned `Future[Done]` is completed.
   */
  def process(session: R2dbcSession, envelope: Envelope): Future[Done]

}

@ApiMayChange
object R2dbcHandler {

  /** R2dbcHandler that can be define from a simple function */
  private class R2dbcHandlerFunction[Envelope](handler: (R2dbcSession, Envelope) => Future[Done])
      extends R2dbcHandler[Envelope] {

    override def process(session: R2dbcSession, envelope: Envelope): Future[Done] =
      handler(session, envelope)
  }

  def apply[Envelope](handler: (R2dbcSession, Envelope) => Future[Done]): R2dbcHandler[Envelope] =
    new R2dbcHandlerFunction(handler)
}

package com.github.xvthomas.zioexample.service.helpers

import sttp.tapir.model.ServerRequest
import zio.{Cause, ZIO}

trait EndpointLogger {
  implicit class EndpointLoggerOps[Z, A](res: ZIO[Z, ProblemDetails, A]) {

    def logEndpoint(serverRequest: ServerRequest): ZIO[Z, ProblemDetails, A] =
      res.foldZIO(
        e =>
          (e match {
            case error: BadRequest => ZIO.logDebug(error.toString)
            case error: InternalServerError => ZIO.logErrorCause(error.title, Cause.fail(e))
            case error: NotFound => ZIO.logDebug(error.toString)
            case error: UnprocessableEntity => ZIO.logDebug(error.toString)
          }).provideLayer(Logger.layer) *> ZIO.fail(e),
        r =>
          ZIO.logDebug(s"${serverRequest.method} ${serverRequest.uri}")
            .provideLayer(Logger.layer) *> ZIO.succeed(r)
      )
  }
}

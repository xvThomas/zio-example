package com.github.xvthomas.zioexample.service.helpers

import zio.ZIO

trait ResponseLogger {
  implicit class ResponseLoggerOps[Z, A](res: ZIO[Z, ProblemDetails, A]) {
    def logResponse(): ZIO[Z, ProblemDetails, A] =
      res.foldZIO(
        e =>
          (e match {
            case error: BadRequest          => ZIO.logWarning(error.toString)
            case error: InternalServerError => ZIO.logError(error.toString)
            case error: NotFound            => ZIO.logWarning(error.toString)
            case error: UnprocessableEntity => ZIO.logWarning(error.toString)
          }) *> ZIO.fail(e),
        r => ZIO.logInfo(r.toString) *> ZIO.succeed(r)
      )
  }
}

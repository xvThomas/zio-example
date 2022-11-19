package com.github.xvthomas.zioexample.service.helpers

import zio.logging.{LogFormat, console}
import zio.{LogLevel, Runtime, ZLayer}

case object Logger {
  val layer: ZLayer[Any, _, Unit] = Runtime.removeDefaultLoggers >>> console(LogFormat.colored, LogLevel.Debug)
}

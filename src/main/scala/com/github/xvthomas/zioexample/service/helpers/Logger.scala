package com.github.xvthomas.zioexample.service.helpers

import zio.logging.{LogFormat, console}
import zio.{LogLevel, Runtime, ZLayer}

case object Logger {
  def layer[E]: ZLayer[Any, E, Unit] = Runtime.removeDefaultLoggers >>> console(LogFormat.colored, LogLevel.Debug)
}

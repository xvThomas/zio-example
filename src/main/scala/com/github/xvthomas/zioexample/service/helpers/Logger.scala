package com.github.xvthomas.zioexample.service.helpers

import zio.logging.{LogFormat, console}
import zio.{LogLevel, Runtime}

case object Logger {
  val layer = Runtime.removeDefaultLoggers >>> console(LogFormat.colored, LogLevel.Debug)
}

package com.github.xvthomas.zioexample.service.protocols

import sttp.tapir.Schema
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, EncoderOps, JsonDecoder, JsonEncoder}

final case class IOUser(
  login: String,
  email: String
)

trait IOUserJsonProtocol {
  implicit lazy val ioUserEncoder: JsonEncoder[IOUser] = DeriveJsonEncoder.gen[IOUser]
  implicit lazy val ioUserDecoder: JsonDecoder[IOUser] = DeriveJsonDecoder.gen[IOUser]
}

trait IOUserSchema extends IOUserJsonProtocol {
  implicit lazy val ioUserSchema: Schema[IOUser] = Schema
    .derived
    .encodedExample(IOUser("xvthomas", "xvthomas@github.com").toJson)
}

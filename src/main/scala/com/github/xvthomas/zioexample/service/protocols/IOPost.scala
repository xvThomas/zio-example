package com.github.xvthomas.zioexample.service.protocols

import sttp.tapir.Schema
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, EncoderOps, JsonDecoder, JsonEncoder}

import java.time.Instant

final case class IPost(
  userLogin: String,
  dateOfIssue: Instant,
  message: String
)

final case class IPostMessage(
  id: Long,
  message: String
)

final case class OPost(
  id: Long,
  userLogin: String,
  dateOfIssue: Instant,
  message: String
)

trait IPostJsonProtocol {
  implicit lazy val iPostEncoder: JsonEncoder[IPost] = DeriveJsonEncoder.gen[IPost]
  implicit lazy val iPostDecoder: JsonDecoder[IPost] = DeriveJsonDecoder.gen[IPost]
}

trait IPostSchema extends IPostJsonProtocol {
  implicit lazy val iPostSchema: Schema[IPost] = Schema.derived
    .encodedExample(IPost(
      userLogin = "xvthomas",
      dateOfIssue = Instant.now(),
      message = "type your message here..."
    ).toJson)
}

trait IPostMessageJsonProtocol {
  implicit lazy val iPostMessageEncoder: JsonEncoder[IPostMessage] = DeriveJsonEncoder.gen[IPostMessage]
  implicit lazy val iPostMessageDecoder: JsonDecoder[IPostMessage] = DeriveJsonDecoder.gen[IPostMessage]
}

trait IPostMessageSchema extends IPostMessageJsonProtocol {
  implicit lazy val iPostMessageSchema: Schema[IPostMessage] = Schema.derived
    .encodedExample(IPostMessage(
      id = 0,
      message = "Update your message here..."
    ).toJson)
}

trait OPostJsonProtocol {
  implicit lazy val oPostEncoder: JsonEncoder[OPost] = DeriveJsonEncoder.gen[OPost]
  implicit lazy val oPostDecoder: JsonDecoder[OPost] = DeriveJsonDecoder.gen[OPost]
}

trait OPostSchema {
  implicit lazy val oPostSchema: Schema[OPost] = Schema.derived
}

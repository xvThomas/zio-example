package com.github.xvthomas.zioexample.persistence.validation.impl.zio

import com.github.xvthomas.zioexample.persistence.PersistenceError
import com.github.xvthomas.zioexample.persistence.model.Post
import com.github.xvthomas.zioexample.persistence.validation.PostUserLoginLength.minSize
import com.github.xvthomas.zioexample.persistence.validation.{PostMessageEmpty, PostUserLoginLength, PostValidator, ValidationError}
import zio.prelude.Validation
import zio.{IO, NonEmptyChunk, ZIO, ZLayer}

import java.time.Instant

case object PostValidatorImpl {
  val layer: ZLayer[Any, Nothing, PostValidator] =
    ZLayer {
      ZIO.succeed(new PostValidatorImpl)
    }
}

private[zio] class PostValidatorImpl extends PostValidator {

  private def validateUserLogin(userLogin: String): Validation[ValidationError, String] =
    Validation.fromPredicateWith(PostUserLoginLength)(userLogin)(_.length >= minSize)

  private def validateMessage(message: String): Validation[ValidationError, String] =
    Validation.fromPredicateWith(PostMessageEmpty)(message)(_.nonEmpty)

  override def validate(
    userLogin: String,
    dateOfIssue: Instant,
    message: String
  ): IO[PersistenceError.ValidationErrors, Post] =
    ZIO.fromEither(
      Validation.validateWith(
        validateUserLogin(userLogin),
        validateMessage(message)
      )((validUserLogin, validMessage) =>
        Post.unsafe(
          userLogin = validUserLogin,
          dateOfIssue = dateOfIssue,
          message = validMessage
        )
      ).toEither
    ).mapError(PersistenceError.ValidationErrors)

  def validate(
    id: Long,
    message: String
  ): IO[PersistenceError.ValidationErrors, (Long, String)] =
    validateMessage(message)
      .toZIO
      .mapBoth(
        e => PersistenceError.ValidationErrors(NonEmptyChunk(e)),
        validMessage => (id, validMessage)
      )
}

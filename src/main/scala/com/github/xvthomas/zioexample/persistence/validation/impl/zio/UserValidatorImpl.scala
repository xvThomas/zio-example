package com.github.xvthomas.zioexample.persistence.validation.impl.zio

import com.github.xvthomas.zioexample.persistence.PersistenceError
import com.github.xvthomas.zioexample.persistence.model.User
import com.github.xvthomas.zioexample.persistence.validation.UserLoginLength.minSize
import com.github.xvthomas.zioexample.persistence.validation._
import zio.prelude.Validation
import zio.{IO, ZIO, ZLayer}

case object UserValidatorImpl {
  val layer: ZLayer[Any, Nothing, UserValidator] =
    ZLayer {
      ZIO.succeed(new UserValidatorImpl)
    }
}

private[zio] class UserValidatorImpl extends UserValidator {

  private def validateUserLogin(userLogin: String): Validation[ValidationError, String] =
    Validation.fromPredicateWith(UserLoginLength)(userLogin)(_.length >= minSize)

  private def validateEmail(email: String): Validation[ValidationError, String] =
    Validation.fromPredicateWith(UserEmailInvalid)(email)(EmailValidator(_))

  override def validate(
    login: String,
    email: String
  ): IO[PersistenceError.ValidationErrors, User] =
    ZIO.fromEither(
      Validation.validateWith(
        validateUserLogin(login),
        validateEmail(email)
      )((validLogin, validEmail) =>
        User.unsafe(
          login = validLogin,
          email = validEmail
        )
      ).toEither
    ).mapError(PersistenceError.ValidationErrors)
}

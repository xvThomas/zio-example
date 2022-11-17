package com.github.xvthomas.zioexample.persistence.model

import com.github.xvthomas.zioexample.persistence.validation.UserValidator
import com.github.xvthomas.zioexample.persistence.validation.ValidationError.ValidationErrorsOr
import zio.ZIO

sealed abstract case class User(
  login: String,
  email: String
)

case object User {
  def apply(login: String, email: String): ValidationErrorsOr[UserValidator, User] =
    ZIO.service[UserValidator].flatMap(validator =>
      validator.validate(
        login,
        email
      )
    )

  private[persistence] def unsafe(login: String, email: String) =
    new User(
      login,
      email
    ) {}
}

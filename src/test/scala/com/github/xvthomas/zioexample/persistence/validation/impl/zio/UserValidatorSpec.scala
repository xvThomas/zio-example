package com.github.xvthomas.zioexample.persistence.validation.impl.zio

import com.github.xvthomas.zioexample.persistence.PersistenceError
import com.github.xvthomas.zioexample.persistence.model.User
import com.github.xvthomas.zioexample.persistence.validation.{UserEmailInvalid, UserLoginLength, UserValidator}
import zio.{NonEmptyChunk, Scope, ZIO}
import zio.test.{Spec, TestEnvironment, TestResult, ZIOSpecDefault, assertTrue}

object UserValidatorSpec extends ZIOSpecDefault {

  def validate(
    login: String,
    email: String,
    assertResult: Either[PersistenceError.ValidationErrors, User] => TestResult
  ): ZIO[UserValidator, Nothing, TestResult] =
    for {
      validator <- ZIO.service[UserValidator]
      user      <- validator.validate(login, email).either
    } yield { assertResult(user) }

  def spec: Spec[TestEnvironment with Scope, Any] =
    suite("OrganizationValidatorSpec")(
      test("valid user should be validated")(
        validate(
          "xvthomas",
          "xvthomas@github.com",
          user => assertTrue(user.isRight)
        )
      ),
      test("invalid user must not be validated")(
        validate(
          "xvth",                // UserLoginLength!
          "xvthomas&github.com", // UserEmailInvalid!
          user =>
            assertTrue(
              user.fold(
                _ == PersistenceError.ValidationErrors(NonEmptyChunk(UserLoginLength, UserEmailInvalid)),
                _ => false
              )
            )
        )
      )
    ).provide(UserValidatorImpl.layer)
}

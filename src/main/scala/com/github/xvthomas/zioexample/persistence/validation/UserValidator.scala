package com.github.xvthomas.zioexample.persistence.validation

import com.github.xvthomas.zioexample.persistence.PersistenceError
import com.github.xvthomas.zioexample.persistence.model.User
import zio.IO

trait UserValidator {
  def validate(
    login: String,
    email: String
  ): IO[PersistenceError.ValidationErrors, User]
}

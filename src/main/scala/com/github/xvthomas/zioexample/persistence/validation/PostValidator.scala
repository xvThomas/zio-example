package com.github.xvthomas.zioexample.persistence.validation

import com.github.xvthomas.zioexample.persistence.PersistenceError
import com.github.xvthomas.zioexample.persistence.model.Post
import zio.IO

import java.time.Instant

trait PostValidator {
  def validate(
    userLogin: String,
    dateOfIssue: Instant,
    message: String
  ): IO[PersistenceError.ValidationErrors, Post]

  def validate(
    id: Long,
    message: String
  ): IO[PersistenceError.ValidationErrors, (Long, String)]
}

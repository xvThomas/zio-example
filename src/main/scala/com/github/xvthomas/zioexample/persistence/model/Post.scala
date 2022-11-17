package com.github.xvthomas.zioexample.persistence.model

import com.github.xvthomas.zioexample.persistence.validation.PostValidator
import com.github.xvthomas.zioexample.persistence.validation.ValidationError.ValidationErrorsOr
import zio.ZIO

import java.time.Instant

sealed abstract case class Post(
  id: Long = 0L,
  userLogin: String,
  dateOfIssue: Instant,
  message: String
)

case object Post {
  def apply(userLogin: String, dateOfIssue: Instant, message: String): ValidationErrorsOr[PostValidator, Post] =
    ZIO.service[PostValidator].flatMap(validator =>
      validator.validate(
        userLogin,
        dateOfIssue,
        message
      )
    )

  private[persistence] def unsafe(id: Long, userLogin: String, dateOfIssue: Instant, message: String) =
    new Post(
      id,
      userLogin,
      dateOfIssue,
      message
    ) {}

  private[persistence] def unsafe(userLogin: String, dateOfIssue: Instant, message: String): Post =
    unsafe(
      id = 0L,
      userLogin,
      dateOfIssue,
      message
    )
}

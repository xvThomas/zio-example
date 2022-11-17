package com.github.xvthomas.zioexample.persistence

import com.github.xvthomas.zioexample.persistence.validation.ValidationError
import zio.NonEmptyChunk

sealed trait PersistenceError
object PersistenceError {
  final case class NotFound(key: String, value: String)                     extends PersistenceError
  final case class AlreadyExists(key: String, value: String)                extends PersistenceError
  final case class MissingReference(key: String, value: String)             extends PersistenceError
  final case class UsedReferenceOnDelete(key: String, value: String)        extends PersistenceError
  final case class UnexpectedError(throwable: Throwable)                    extends PersistenceError
  final case class ValidationErrors(errors: NonEmptyChunk[ValidationError]) extends PersistenceError
}

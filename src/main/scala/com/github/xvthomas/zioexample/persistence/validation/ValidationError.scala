package com.github.xvthomas.zioexample.persistence.validation

import com.github.xvthomas.zioexample.persistence.PersistenceError
import zio.ZIO

trait ValidationError {
  def key: String
  def path: String
  def args: List[Any] = List.empty
}

object ValidationError {
  type ValidationErrorsOr[R, +A] = ZIO[R, PersistenceError, A]
}

case object PostUserLoginLength extends ValidationError {
  val minSize                  = 6
  override def key: String     = "post.userLogin.length"
  override def path: String    = "userLogin"
  override def args: List[Any] = List(minSize)
}

case object PostMessageEmpty extends ValidationError {
  override def key: String  = "post.message.empty"
  override def path: String = "message"
}

case object UserLoginLength extends ValidationError {
  val minSize                  = 6
  override def key: String     = "user.login.length"
  override def path: String    = "login"
  override def args: List[Any] = List(minSize)
}

case object UserEmailInvalid extends ValidationError {
  override def key: String  = "user.email.invalid"
  override def path: String = "email"
}

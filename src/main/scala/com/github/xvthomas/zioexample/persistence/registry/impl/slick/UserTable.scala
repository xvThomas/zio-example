package com.github.xvthomas.zioexample.persistence.registry.impl.slick

import com.github.xvthomas.zioexample.persistence.model.User
import com.github.xvthomas.zioexample.persistence.registry.impl.slick.UserTable.tableName
import slick.jdbc.PostgresProfile.api._
import slick.lifted.{Index, ProvenShape, Tag}

trait UserTableAlias {
  type Tuple = (String, String)
}

case object UserTable {
  val tableName: String   = "user"
  val columnLogin: String = "login"
  val columnEmail: String = "email"
}

private[slick] class UserTable(tag: Tag) extends Table[User](tag, tableName) with UserTableAlias {
  def intoUser(tuple: Tuple): User        = User.unsafe(tuple._1, tuple._2)
  def fromUser(user: User): Option[Tuple] = Some(user.login, user.email)

  val login: Rep[String] = column[String](UserTable.columnLogin, O.PrimaryKey)
  val email: Rep[String] = column[String](UserTable.columnEmail)

  def emailIndex: Index = index(name = "user_email_idx", on = email, unique = true)

  // scalastyle:off method.name
  override def * : ProvenShape[User] =
    (login, email) <> (intoUser, fromUser)
  // scalastyle:on method.name
}

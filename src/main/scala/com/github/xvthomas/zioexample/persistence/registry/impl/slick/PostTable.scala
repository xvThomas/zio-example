package com.github.xvthomas.zioexample.persistence.registry.impl.slick

import com.github.xvthomas.zioexample.persistence.model.{Post, User}
import com.github.xvthomas.zioexample.persistence.registry.impl.slick.PostTable.tableName
import slick.jdbc.PostgresProfile.api._
import slick.lifted.{ForeignKeyQuery, Index, ProvenShape, Tag}

import java.time.Instant

trait PostTableAlias {
  type Tuple = (Long, String, Instant, String)
}

case object PostTable {
  val tableName: String         = "post"
  val columnId: String          = "id"
  val columnUserLogin: String   = "userLogin"
  val columnDateOfIssue: String = "dateOfIssue"
  val columnMessage: String     = "message"
}

private[slick] class PostTable(tag: Tag) extends Table[Post](tag, tableName) with PostTableAlias {
  def intoPost(tuple: Tuple): Post        = Post.unsafe(tuple._1, tuple._2, tuple._3, tuple._4)
  def fromPost(post: Post): Option[Tuple] = Some(post.id, post.userLogin, post.dateOfIssue, post.message)

  val id: Rep[Long]             = column[Long](PostTable.columnId, O.PrimaryKey, O.AutoInc)
  val userLogin: Rep[String]    = column[String](PostTable.columnUserLogin)
  val dateOfIssue: Rep[Instant] = column[Instant](PostTable.columnDateOfIssue)
  val message: Rep[String]      = column[String](PostTable.columnMessage)

  private val users = TableQuery[UserTable]
  def fkUserLogin: ForeignKeyQuery[UserTable, User] =
    foreignKey("user_login_fk", userLogin, users)(_.login, onDelete = ForeignKeyAction.Cascade)
  def dateOfIssueIndex: Index = index(name = "post_date_idx", on = dateOfIssue, unique = false)

  // scalastyle:off method.name
  override def * : ProvenShape[Post] =
    (id, userLogin, dateOfIssue, message) <> (intoPost, fromPost)
  // scalastyle:on method.name
}

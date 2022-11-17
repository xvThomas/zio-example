package com.github.xvthomas.zioexample.persistence.registry.impl.slick

import com.github.xvthomas.zioexample.persistence.PersistenceError
import com.github.xvthomas.zioexample.persistence.model.Post
import com.github.xvthomas.zioexample.persistence.registry.PostRegistry
import org.postgresql.util.PSQLException
import slick.interop.zio.DatabaseProvider
import zio.{ULayer, ZIO, ZLayer}
import slick.interop.zio.syntax._
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global

case object PostRegistryImpl {
  val layer: ZLayer[DatabaseProvider, PersistenceError, PostRegistry] =
    ZLayer {
      for {
        databaseProvider <- ZIO.service[DatabaseProvider]
        postRegistry = new PostRegistryImpl(ZLayer.succeed(databaseProvider))
        registry <- postRegistry.create.as(postRegistry)
      } yield registry
    }
}

private[slick] class PostRegistryImpl(val databaseProviderLayer: ULayer[DatabaseProvider]) extends PostRegistry {
  val posts = TableQuery[PostTable]

  val userLoginAttributeName = "login"
  val idAttributeName        = "id"

  private class NotFoundException extends Exception

  override private[registry] def drop(): UIORegistryResult[Unit] =
    ZIO.fromDBIO(posts.schema.dropIfExists)
      .mapError(throwable => PersistenceError.UnexpectedError(throwable))
      .provide(databaseProviderLayer)

  override def create: UIORegistryResult[Unit] =
    ZIO.fromDBIO(posts.schema.createIfNotExists).catchAll({
      case error: PSQLException if error.getSQLState == PostgresErrorCodes.duplicateTable => ZIO.unit
      case error: Exception                                                               => ZIO.fail(PersistenceError.UnexpectedError(error))
    }).provide(databaseProviderLayer)

  override def insert(post: Post): UIORegistryResult[Long] = {
    ZIO.fromDBIO(posts returning posts.map(_.id) += post).mapError({
      case error: PSQLException if error.getSQLState == PostgresErrorCodes.foreignKeyViolation =>
        PersistenceError.MissingReference(userLoginAttributeName, post.userLogin)
      case error: Exception => PersistenceError.UnexpectedError(error)
    })
      .provide(databaseProviderLayer)
  }

  override def update(id: Long, message: String): UIORegistryResult[Unit] =
    ZIO.fromDBIO(
      posts.filter(_.id === id).map(_.message).update(message)
        .flatMap {
          case 1 => DBIO.successful(())
          case 0 => DBIO.failed(new NotFoundException)
        }
    ).mapError({
      case _: NotFoundException => PersistenceError.NotFound(idAttributeName, id.toString)
      case throwable            => PersistenceError.UnexpectedError(throwable)
    }).provide(databaseProviderLayer)

  override def delete(id: Long): UIORegistryResult[Unit] =
    ZIO.fromDBIO(posts.filter(_.id === id).delete)
      .mapError(error => PersistenceError.UnexpectedError(error))
      .flatMap(res => if (res > 0) ZIO.succeed(()) else ZIO.fail(PersistenceError.NotFound(idAttributeName, id.toString)))
      .provide(databaseProviderLayer)

  override def selectOne(id: Long): UIORegistryResult[Post] =
    ZIO.fromDBIO(posts.filter(_.id === id).result.headOption)
      .mapError(error => PersistenceError.UnexpectedError(error))
      .flatMap {
        case Some(post) => ZIO.succeed(post)
        case None       => ZIO.fail(PersistenceError.NotFound(idAttributeName, id.toString))
      }.provide(databaseProviderLayer)

  override def selectAllOf(userLogin: String): UIORegistryResult[Seq[Post]] =
    ZIO.fromDBIO(posts.filter(_.userLogin === userLogin).sortBy(_.dateOfIssue.desc).result)
      .mapError(error => PersistenceError.UnexpectedError(error))
      .provide(databaseProviderLayer)
}

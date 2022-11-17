package com.github.xvthomas.zioexample.persistence.registry.impl.slick

import com.github.xvthomas.zioexample.persistence.PersistenceError
import com.github.xvthomas.zioexample.persistence.model.User
import com.github.xvthomas.zioexample.persistence.registry.UserRegistry
import org.postgresql.util.PSQLException
import slick.interop.zio.DatabaseProvider
import zio.{ULayer, ZIO, ZLayer}
import slick.interop.zio.syntax._
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global

case object UserRegistryImpl {
  val layer: ZLayer[DatabaseProvider, PersistenceError, UserRegistry] =
    ZLayer {
      for {
        databaseProvider <- ZIO.service[DatabaseProvider]
        userRegistry = new UserRegistryImpl(ZLayer.succeed(databaseProvider))
        registry <- userRegistry.create.as(userRegistry)
      } yield registry
    }
}

private[slick] class UserRegistryImpl(val databaseProviderLayer: ULayer[DatabaseProvider])
  extends UserRegistry {
  val users = TableQuery[UserTable]

  val loginAttributeName = "login"

  override def drop: UIORegistryResult[Unit] =
    ZIO.fromDBIO(users.schema.dropIfExists)
      .mapError(throwable => PersistenceError.UnexpectedError(throwable))
      .provide(databaseProviderLayer)

  override def create: UIORegistryResult[Unit] =
    ZIO.fromDBIO(users.schema.createIfNotExists).catchAll({
      case error: PSQLException if error.getSQLState == PostgresErrorCodes.duplicateTable => ZIO.unit
      case error: Exception =>
        ZIO.fail(PersistenceError.UnexpectedError(error))
    }).provide(databaseProviderLayer)

  override def insert(user: User): UIORegistryResult[Unit] =
    ZIO.fromDBIO(users += user)
      .mapError({
        case error: PSQLException if error.getSQLState == PostgresErrorCodes.uniqueViolation =>
          PersistenceError.AlreadyExists(loginAttributeName, user.login)
        case error: Exception => PersistenceError.UnexpectedError(error)
      })
      .unit
      .provide(databaseProviderLayer)

  override def update(user: User): UIORegistryResult[Unit] =
    ZIO.fromDBIO(
      users.filter(_.login === user.login)
        .update(user)
        .flatMap {
          case 1 => DBIO.successful(())
          case 0 => DBIO.failed(new RuntimeException("Not Found"))
        }
    ).mapError({
      case _: RuntimeException => PersistenceError.NotFound(loginAttributeName, user.login)
      case throwable           => PersistenceError.UnexpectedError(throwable)
    }).provide(databaseProviderLayer)

  override def delete(login: String): UIORegistryResult[Unit] =
    ZIO.fromDBIO(users.filter(_.login === login).delete)
      .mapError({
        case error: PSQLException if error.getSQLState == PostgresErrorCodes.foreignKeyViolation =>
          PersistenceError.UsedReferenceOnDelete(loginAttributeName, login)
        case error: Exception =>
          PersistenceError.UnexpectedError(error)
      })
      .flatMap(res => if (res > 0) ZIO.succeed(()) else ZIO.fail(PersistenceError.NotFound(loginAttributeName, login)))
      .provide(databaseProviderLayer)

  override def selectOne(login: String): UIORegistryResult[User] =
    ZIO.fromDBIO(users.filter(_.login === login).result.headOption)
      .mapError(error => PersistenceError.UnexpectedError(error))
      .flatMap {
        case Some(site) => ZIO.succeed(site)
        case None       => ZIO.fail(PersistenceError.NotFound(loginAttributeName, login))
      }.provide(databaseProviderLayer)

  override def selectAll(): UIORegistryResult[Seq[User]] =
    ZIO.fromDBIO(users.sortBy(_.login).result)
      .mapError(error => PersistenceError.UnexpectedError(error))
      .provide(databaseProviderLayer)
}

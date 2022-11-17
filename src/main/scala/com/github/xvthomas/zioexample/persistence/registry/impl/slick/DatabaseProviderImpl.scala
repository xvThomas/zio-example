package com.github.xvthomas.zioexample.persistence.registry.impl.slick

import com.github.xvthomas.zioexample.persistence.PersistenceError
import com.typesafe.config.{Config, ConfigFactory}
import slick.interop.zio.DatabaseProvider
import slick.jdbc
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.{JdbcBackend, PostgresProfile}
import zio.{Scope, UIO, ZIO, ZLayer}

final case class DatabaseSettings(
  url: String,
  username: String,
  password: String
)

trait DatabaseProviderImpl {

  def makeScopedConnection(settings: DatabaseSettings): ZIO[Any with Scope, Nothing, jdbc.JdbcBackend.DatabaseDef] =
    ZIO.acquireRelease(
      ZIO.attempt(
        Database.forURL(settings.url, settings.username, settings.password, driver = "org.postgresql.Driver")
      ).orDie
    )(db => ZIO.succeed(db.close()))
}

case object DatabaseProviderImpl extends DatabaseProviderImpl {

  val defaultConfig: Config = ConfigFactory.load()

  val layer: ZLayer[Any, PersistenceError, DatabaseProvider] = {

    val dbProvider: ZIO[Any with Scope, Throwable, DatabaseProvider] =
      for {
        config <- ZIO.succeed(defaultConfig.getConfig("example"))
        connection <- makeScopedConnection(DatabaseSettings(
          url = config.getString("url"),
          username = config.getString("user"),
          password = config.getString("password")
        ))
      } yield new DatabaseProvider {
        override val db: UIO[JdbcBackend#Database] = ZIO.succeed(connection)
        override val profile: UIO[PostgresProfile] = ZIO.succeed(PostgresProfile)
      }

    ZLayer.scoped(dbProvider.mapError(PersistenceError.UnexpectedError))
  }
}

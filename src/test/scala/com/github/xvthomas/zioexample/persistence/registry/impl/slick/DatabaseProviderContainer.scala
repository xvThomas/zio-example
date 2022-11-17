package com.github.xvthomas.zioexample.persistence.registry.impl.slick

import com.dimafeng.testcontainers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import slick.interop.zio.DatabaseProvider
import slick.jdbc.{JdbcBackend, PostgresProfile}
import zio.{Scope, UIO, ZEnvironment, ZIO, ZLayer}

case object DatabaseProviderContainer extends DatabaseProviderImpl {

  final case class ContainerSettings(
    imageVersion: String,
    databaseName: String,
    username: String,
    password: String
  )

  val live: ZLayer[Any, Nothing, DatabaseProvider] = {

    def makeScopedContainer(settings: ContainerSettings): ZIO[Any with Scope, Nothing, PostgreSQLContainer] =
      ZIO.acquireRelease(
        ZIO.attempt {
          val containerDef = PostgreSQLContainer.Def(
            dockerImageName = DockerImageName.parse(s"postgres:${settings.imageVersion}"),
            databaseName = settings.databaseName,
            username = settings.username,
            password = settings.password
          )
          containerDef.start()
        }.orDie
      )(container =>
        ZIO
          .attempt(container.stop())
          .ignoreLogged
      )

    ZLayer.scopedEnvironment {
      for {
        container <- makeScopedContainer(ContainerSettings(
          "latest",
          PostgreSQLContainer.defaultDatabaseName,
          PostgreSQLContainer.defaultUsername,
          PostgreSQLContainer.defaultPassword
        ))
        connection <- makeScopedConnection(DatabaseSettings(
          container.jdbcUrl,
          container.username,
          container.password
        ))
      } yield {
        val databaseProvider: DatabaseProvider = new DatabaseProvider {
          override val db: UIO[JdbcBackend#Database] = ZIO.succeed(connection)
          override val profile: UIO[PostgresProfile] = ZIO.succeed(PostgresProfile)
        }
        ZEnvironment(databaseProvider)
      }
    }
  }
}

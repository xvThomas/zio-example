ThisBuild / scalaVersion     := "2.13.8"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.github.xvthomas"
ThisBuild / organizationName := "xvthomas"
ThisBuild / scapegoatVersion := "1.4.12"

lazy val root = (project in file("."))
  .settings(
    name := "zio-example",
    libraryDependencies ++= zioDependencies ++ postgresDependencies ++ httpDependencies,
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )

val zioOrganization   = "dev.zio"
val zioVersion        = "2.0.3"
val zioPreludeVersion = "1.0.0-RC16"
val zioLoggingVersion = "2.1.3"
val slf4jOrganization = "org.slf4j"
val slf4jVersion      = "2.0.3"
val zioDependencies = Seq(
  zioOrganization %% "zio-prelude" % zioPreludeVersion,
  // zioOrganization  %% "zio-logging"       % zioLoggingVersion,
  // zioOrganization  %% "zio-logging-slf4j" % zioLoggingVersion,
  slf4jOrganization % "slf4j-api"    % slf4jVersion,
  slf4jOrganization % "slf4j-simple" % slf4jVersion,
  zioOrganization  %% "zio-test"     % zioVersion % Test,
  zioOrganization  %% "zio-test-sbt" % zioVersion % Test
)

val scalaJdbcVersion           = "4.0.0"
val postgresqlVersion          = "42.4.1"
val slickVersion               = "3.4.0-M1"
val zioSlickInteropVersion     = "0.5.0"
val testContainersScalaVersion = "0.40.10"
val postgresDependencies =
  Seq(
    "com.typesafe.slick" %% "slick"                           % slickVersion,
    "org.postgresql"      % "postgresql"                      % postgresqlVersion,
    "io.scalac"          %% "zio-slick-interop"               % zioSlickInteropVersion,
    "com.dimafeng"       %% "testcontainers-scala-scalatest"  % testContainersScalaVersion % Test,
    "com.dimafeng"       %% "testcontainers-scala-postgresql" % testContainersScalaVersion % Test
  )

val tapirOrganization   = "com.softwaremill.sttp.tapir"
val tapirVersion        = "1.2.1"
val zioHttpOrganization = "io.d11"
val zioHttpVersion      = "2.0.0-RC10"
val httpDependencies = Seq(
  tapirOrganization   %% "tapir-zio"                % tapirVersion,
  tapirOrganization   %% "tapir-zio-http-server"    % tapirVersion,
  tapirOrganization   %% "tapir-prometheus-metrics" % tapirVersion,
  tapirOrganization   %% "tapir-swagger-ui-bundle"  % tapirVersion,
  tapirOrganization   %% "tapir-json-zio"           % tapirVersion,
  zioHttpOrganization %% "zhttp"                    % zioHttpVersion
)

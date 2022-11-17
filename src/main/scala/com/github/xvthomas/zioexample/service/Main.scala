package com.github.xvthomas.zioexample.service

import com.github.xvthomas.zioexample.persistence.R
import com.github.xvthomas.zioexample.persistence.R.ImplicitLayers
import com.github.xvthomas.zioexample.service.endpoints.{PostEndpoints, UserEndpoints}
import sttp.tapir.server.metrics.prometheus.PrometheusMetrics
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.ztapir.ZServerEndpoint
import zhttp.http.Middleware.cors
import zhttp.http.middleware.Cors.CorsConfig
import zhttp.service.Server
import zio.prelude.PartialOrdOps
import zio.{ExitCode, Schedule, Task, URIO, ZIO, ZIOAppDefault, durationInt}

case object Main extends ZIOAppDefault {

  def routes(r: R): List[ZServerEndpoint[Any, Any]] = {
    val routes: List[ZServerEndpoint[Any, Any]] =
      UserEndpoints(r.userRegistry, r.userValidator).routes ++
        PostEndpoints(r.postRegistry, r.postValidator).routes

    val docEndpoints: List[ZServerEndpoint[Any, Any]] = SwaggerInterpreter()
      .fromServerEndpoints[Task](
        routes,
        "zio-example",
        "1.0.0"
      )

    val prometheusMetrics: PrometheusMetrics[Task] = PrometheusMetrics.default[Task]()
    val metricsEndpoint: ZServerEndpoint[Any, Any] = prometheusMetrics.metricsEndpoint

    routes ++
      docEndpoints ++
      List(metricsEndpoint)
  }

  val config: CorsConfig = CorsConfig(allowedOrigins = _ => true)

  override def run: URIO[Any, ExitCode] = {
    val persistencePolicy = (Schedule.fixed(3.seconds) >>> Schedule.elapsed)
      .whileOutput(_ < 30.seconds)
      .tapOutput(o => ZIO.logWarning(s"Trying to connect to database [$o]"))

    val httpPort = 8080

    R()
      .flatMap(r =>
        Server.start(httpPort, ZioHttpInterpreter().toHttp(routes(r)) @@ cors(config))
          *> ZIO.logInfo(s"Server started on port $httpPort")
      )
      .providePersistenceLayers
      .retry(persistencePolicy)
      .catchAll(error => ZIO.logError(error.toString)).exitCode

  }
}

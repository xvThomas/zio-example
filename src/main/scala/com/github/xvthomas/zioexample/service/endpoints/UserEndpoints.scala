package com.github.xvthomas.zioexample.service.endpoints

import com.github.xvthomas.zioexample.persistence.PersistenceError
import com.github.xvthomas.zioexample.persistence.model.User
import com.github.xvthomas.zioexample.persistence.registry.UserRegistry
import com.github.xvthomas.zioexample.persistence.validation.UserValidator
import com.github.xvthomas.zioexample.service.helpers._
import com.github.xvthomas.zioexample.service.protocols.{IOUser, IOUserJsonProtocol, IOUserSchema}
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.model.ServerRequest
import sttp.tapir.ztapir.{RichZEndpoint, ZServerEndpoint}
import sttp.tapir.{endpoint, oneOf, path}
import zio.IO

final case class UserEndpoints(
                                userRegistry: UserRegistry,
                                userValidator: UserValidator
                              ) extends EndpointLogger
  with PublicEndpointEx with WithProblemDetails
  with IOUserJsonProtocol with IOUserSchema {

  private val endpointsTag = "User"
  private val endpointsPath = "user"

  private def validate(ioUser: IOUser): IO[PersistenceError.ValidationErrors, User] =
    userValidator.validate(login = ioUser.login, email = ioUser.email)

  private def convert(user: User): IOUser = IOUser(login = user.login, email = user.email)

  private val postUserEndpoint: PublicEndpointEx[IOUser, Unit] = endpoint.post
    .info(i = new SimpleEndpointInfo("Insert a user", endpointsTag))
    .in(endpointsPath)
    .in(jsonBody[IOUser])
    .errorOut(oneOf[ProblemDetails](badRequest, internalServerError, notFound, unprocessableEntity))
    .withServerRequest
  val postUserLogic: ((IOUser, ServerRequest)) => IO[ProblemDetails, Unit] = {
    case (ioUser, serverRequest) =>
      validate(ioUser)
        .flatMap(userRegistry.insert)
        .unit
        .mapPersistentError(serverRequest)
        .logEndpoint(serverRequest)
  }

  private val putUserEndpoint: PublicEndpointEx[IOUser, Unit] = endpoint.put
    .info(i = new SimpleEndpointInfo("Update a user", endpointsTag))
    .in(endpointsPath)
    .in(jsonBody[IOUser])
    .errorOut(oneOf[ProblemDetails](badRequest, internalServerError, notFound, unprocessableEntity))
    .withServerRequest
  val putUserLogic: ((IOUser, ServerRequest)) => IO[ProblemDetails, Unit] = {
    case (entity, serverRequest) =>
      validate(entity).flatMap(userRegistry.update)
        .unit
        .mapPersistentError(serverRequest)
        .logEndpoint(serverRequest)
  }

  val getUserEndpoint: PublicEndpointEx[String, IOUser] = endpoint.get
    .info(i = new SimpleEndpointInfo("Return a user using a given login", endpointsTag))
    .in(endpointsPath)
    .in(path[String]("login"))
    .out(jsonBody[IOUser])
    .errorOut(oneOf[ProblemDetails](internalServerError, notFound, badRequest))
    .withServerRequest
  val getUserLogic: ((String, ServerRequest)) => IO[ProblemDetails, IOUser] = {
    case (code, serverRequest) =>
      userRegistry.selectOne(code)
        .map(convert)
        .mapPersistentError(serverRequest)
        .logEndpoint(serverRequest)
  }

  val deleteUserEndpoint: PublicEndpointEx[String, Unit] = endpoint.delete
    .info(i = new SimpleEndpointInfo("Remove a user using a given login", endpointsTag))
    .in(endpointsPath)
    .in(path[String]("login"))
    .errorOut(oneOf[ProblemDetails](internalServerError, notFound, badRequest))
    .withServerRequest
  val deleteUserLogic: ((String, ServerRequest)) => IO[ProblemDetails, Unit] = {
    case (code, serverRequest) =>
      userRegistry
        .delete(code)
        .mapPersistentError(serverRequest)
        .logEndpoint(serverRequest)
  }

  val getUsersEndpoint: PublicEndpointEx[Unit, Seq[IOUser]] = endpoint.get
    .info(i = new SimpleEndpointInfo("Return all of users", endpointsTag))
    .in(endpointsPath)
    .out(jsonBody[Seq[IOUser]])
    .errorOut(oneOf[ProblemDetails](internalServerError, notFound))
    .withServerRequest
  val getUsersLogic: ((Unit, ServerRequest)) => IO[ProblemDetails, Seq[IOUser]] = {
    case (_, serverRequest) =>
      userRegistry.selectAll()
        .map(res => res.map(convert))
        .mapPersistentError(serverRequest)
        .logEndpoint(serverRequest)
  }

  def routes: List[ZServerEndpoint[Any, Any]] =
    List(
      postUserEndpoint.zServerLogic(postUserLogic),
      putUserEndpoint.zServerLogic(putUserLogic),
      getUserEndpoint.zServerLogic(getUserLogic),
      deleteUserEndpoint.zServerLogic(deleteUserLogic),
      getUsersEndpoint.zServerLogic(getUsersLogic)
    )
}

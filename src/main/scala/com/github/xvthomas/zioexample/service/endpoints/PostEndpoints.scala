package com.github.xvthomas.zioexample.service.endpoints

import com.github.xvthomas.zioexample.persistence.PersistenceError
import com.github.xvthomas.zioexample.persistence.model.Post
import com.github.xvthomas.zioexample.persistence.registry.PostRegistry
import com.github.xvthomas.zioexample.persistence.validation.PostValidator
import com.github.xvthomas.zioexample.service.helpers.{ProblemDetails, PublicEndpointEx, SimpleEndpointInfo, WithProblemDetails}
import com.github.xvthomas.zioexample.service.protocols._
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.model.ServerRequest
import sttp.tapir.ztapir.{RichZEndpoint, ZServerEndpoint}
import sttp.tapir.{endpoint, oneOf, path}
import zio.IO

final case class PostEndpoints(
  postRegistry: PostRegistry,
  postValidator: PostValidator
) extends IPostJsonProtocol with IPostSchema
  with IPostMessageJsonProtocol with IPostMessageSchema
  with OPostJsonProtocol with OPostSchema
  with PublicEndpointEx with WithProblemDetails {

  private val endpointsTag  = "Post"
  private val endpointsPath = "post"

  private def validate(iPost: IPost): IO[PersistenceError.ValidationErrors, Post] =
    postValidator.validate(iPost.userLogin, iPost.dateOfIssue, iPost.message)

  private def validate(id: Long, message: String): IO[PersistenceError.ValidationErrors, (Long, String)] =
    postValidator.validate(id, message)

  private def convert(post: Post): OPost = OPost(post.id, post.userLogin, post.dateOfIssue, post.message)

  private val postPostEndpoint: PublicEndpointEx[IPost, Unit] = endpoint.post
    .info(i = new SimpleEndpointInfo("Insert a post", endpointsTag))
    .in(endpointsPath)
    .in(jsonBody[IPost])
    .errorOut(oneOf[ProblemDetails](badRequest, internalServerError, notFound, unprocessableEntity))
    .withServerRequest
  val postPostLogic: ((IPost, ServerRequest)) => IO[ProblemDetails, Unit] = { case (iPost, serverRequest) =>
    validate(iPost)
      .flatMap(postRegistry.insert)
      .unit
      .mapPersistentError(serverRequest)
  }

  private val putPostEndpoint: PublicEndpointEx[IPostMessage, Unit] = endpoint.put
    .info(i = new SimpleEndpointInfo("Update a message post", endpointsTag))
    .in(endpointsPath)
    .in(jsonBody[IPostMessage])
    .errorOut(oneOf[ProblemDetails](badRequest, internalServerError, notFound, unprocessableEntity))
    .withServerRequest
  val putPostLogic: ((IPostMessage, ServerRequest)) => IO[ProblemDetails, Unit] = {
    case (postMessage, serverRequest) =>
      validate(postMessage.id, postMessage.message)
        .flatMap({ case (id, message) => postRegistry.update(id, message) })
        .unit
        .mapPersistentError(serverRequest)
  }

  val getPostEndpoint: PublicEndpointEx[Long, OPost] = endpoint.get
    .info(i = new SimpleEndpointInfo("Return a post using a given id", endpointsTag))
    .in(endpointsPath)
    .in(path[Long]("id"))
    .out(jsonBody[OPost])
    .errorOut(oneOf[ProblemDetails](internalServerError, notFound, badRequest))
    .withServerRequest
  val getPostLogic: ((Long, ServerRequest)) => IO[ProblemDetails, OPost] = {
    case (id, serverRequest) =>
      postRegistry.selectOne(id)
        .map(convert)
        .mapPersistentError(serverRequest)
  }

  val deletePostEndpoint: PublicEndpointEx[Long, Unit] = endpoint.delete
    .info(i = new SimpleEndpointInfo("Remove a post using a given id", endpointsTag))
    .in(endpointsPath)
    .in(path[Long]("id"))
    .errorOut(oneOf[ProblemDetails](internalServerError, notFound, badRequest))
    .withServerRequest
  val deletePostLogic: ((Long, ServerRequest)) => IO[ProblemDetails, Unit] = {
    case (id, serverRequest) =>
      postRegistry.delete(id).mapPersistentError(serverRequest)
  }

  val getPostsOfEndpoint: PublicEndpointEx[String, Seq[OPost]] = endpoint.get
    .info(i = new SimpleEndpointInfo("Return all posts of a given user login", endpointsTag))
    .in(endpointsPath)
    .in(path[String]("userLogin"))
    .out(jsonBody[Seq[OPost]])
    .errorOut(oneOf[ProblemDetails](internalServerError, notFound))
    .withServerRequest
  val getPostsOfLogic: ((String, ServerRequest)) => IO[ProblemDetails, Seq[OPost]] = {
    case (userLogin, serverRequest) =>
      postRegistry.selectAllOf(userLogin)
        .map(res => res.map(convert))
        .mapPersistentError(serverRequest)
  }

  def routes: List[ZServerEndpoint[Any, Any]] =
    List(
      postPostEndpoint.zServerLogic(postPostLogic),
      putPostEndpoint.zServerLogic(putPostLogic),
      getPostEndpoint.zServerLogic(getPostLogic),
      deletePostEndpoint.zServerLogic(deletePostLogic),
      getPostsOfEndpoint.zServerLogic(getPostsOfLogic)
    )
}

package com.github.xvthomas.zioexample.service.helpers

import com.github.xvthomas.zioexample.persistence.PersistenceError
import com.github.xvthomas.zioexample.persistence.validation.ValidationError
import sttp.model.StatusCode
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.model.ServerRequest
import sttp.tapir.{EndpointOutput, Schema, oneOfVariant}
import zhttp.http._
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}
import zio.{NonEmptyChunk, ZIO}

final case class OValidationError(path: Option[String], messages: List[String])

object OValidationError {
  private def translate(error: ValidationError): String = error.key

  def fromValidationErrorType(errors: NonEmptyChunk[ValidationError]): List[OValidationError] =
    errors
      .map(error => (error.path, translate(error)))
      .groupBy(error => error._1).toList
      .map(error => OValidationError(if (error._1.isEmpty) None else Some(error._1), error._2.map(_._2).toList))
}

trait ProblemDetails {
  val title: String
  val `type`: Option[String]
  val detail: Option[String]
  val instance: Option[String]
  val status: Int
}

private[helpers] final case class NotFound(title: String, `type`: Option[String], detail: Option[String], instance: Option[String], status: Int)
  extends ProblemDetails {
  def withRequest(request: ServerRequest): NotFound = copy(instance = Some(request.uri.toString()))
}

object NotFound {
  def apply(title: String, `type`: Option[String] = None, detail: Option[String] = None, instance: Option[String] = None): NotFound =
    new NotFound(title, `type`, detail, instance, Status.NotFound.code)
}

private[helpers] final case class BadRequest(
  title: String,
  `type`: Option[String],
  detail: Option[String],
  instance: Option[String],
  status: Int
  // errors: List[String]
) extends ProblemDetails {
  def withRequest(request: ServerRequest): BadRequest = copy(instance = Some(request.uri.toString()))
  // def withErrors(queryArgumentsInputErrors: List[String]): BadRequest =
  //  copy(errors = queryArgumentsInputErrors)
}

object BadRequest {
  def apply(title: String, `type`: Option[String] = None, detail: Option[String] = None, instance: Option[String] = None): BadRequest =
    new BadRequest(title, `type`, detail, instance, Status.BadRequest.code /*, List.empty */ )
}

private[helpers] final case class InternalServerError(
  title: String,
  `type`: Option[String],
  detail: Option[String],
  instance: Option[String],
  status: Int
) extends ProblemDetails {
  def withRequest(request: ServerRequest): InternalServerError = copy(instance = Some(request.uri.toString()))
}

object InternalServerError {
  def apply(title: String, `type`: Option[String] = None, detail: Option[String] = None, instance: Option[String] = None): InternalServerError =
    new InternalServerError(title, `type`, detail, instance, Status.InternalServerError.code)

  def apply(exception: Throwable): InternalServerError =
    new InternalServerError(exception.getClass.getName, None, Some(exception.getMessage), None, Status.InternalServerError.code)
}

private[helpers] final case class UnprocessableEntity(
  title: String,
  `type`: Option[String],
  detail: Option[String],
  instance: Option[String],
  status: Int,
  errors: List[OValidationError]
) extends ProblemDetails {
  def withRequest(request: ServerRequest): UnprocessableEntity                  = copy(instance = Some(request.uri.toString()))
  def withErrors(validationErrors: List[OValidationError]): UnprocessableEntity = copy(errors = validationErrors)
}

object UnprocessableEntity {
  def apply(
    title: String,
    `type`: Option[String] = None,
    detail: Option[String] = None,
    instance: Option[String] = None,
    errors: List[OValidationError]
  ): UnprocessableEntity =
    new UnprocessableEntity(title, `type`, detail, instance, Status.UnprocessableEntity.code, errors)
}

trait WithProblemDetails {
  implicit val notFoundEncoder: JsonEncoder[NotFound]                       = DeriveJsonEncoder.gen[NotFound]
  implicit val notFoundDecoder: JsonDecoder[NotFound]                       = DeriveJsonDecoder.gen[NotFound]
  implicit val notFoundSchema: Schema[NotFound]                             = Schema.derived[NotFound]
  implicit val internalServerErrorEncoder: JsonEncoder[InternalServerError] = DeriveJsonEncoder.gen[InternalServerError]
  implicit val internalServerErrorDecoder: JsonDecoder[InternalServerError] = DeriveJsonDecoder.gen[InternalServerError]
  implicit val internalServerErrorSchema: Schema[InternalServerError]       = Schema.derived[InternalServerError]
  implicit val badRequestEncoder: JsonEncoder[BadRequest]                   = DeriveJsonEncoder.gen[BadRequest]
  implicit val badRequestDecoder: JsonDecoder[BadRequest]                   = DeriveJsonDecoder.gen[BadRequest]
  implicit val badRequestSchema: Schema[BadRequest]                         = Schema.derived[BadRequest]
  implicit val validationErrorEncoder: JsonEncoder[OValidationError]        = DeriveJsonEncoder.gen[OValidationError]
  implicit val validationErrorDecoder: JsonDecoder[OValidationError]        = DeriveJsonDecoder.gen[OValidationError]
  implicit val validationErrorSchema: Schema[OValidationError]              = Schema.derived[OValidationError]
  implicit val unprocessableEntityEncoder: JsonEncoder[UnprocessableEntity] = DeriveJsonEncoder.gen[UnprocessableEntity]
  implicit val unprocessableEntityDecoder: JsonDecoder[UnprocessableEntity] = DeriveJsonDecoder.gen[UnprocessableEntity]
  implicit val unprocessableEntitySchema: Schema[UnprocessableEntity]       = Schema.derived[UnprocessableEntity]

  val notFound: EndpointOutput.OneOfVariant[NotFound] = oneOfVariant(StatusCode.NotFound, jsonBody[NotFound].description("Not found"))
  val internalServerError: EndpointOutput.OneOfVariant[InternalServerError] =
    oneOfVariant(StatusCode.InternalServerError, jsonBody[InternalServerError].description("Internal server Error"))
  val badRequest: EndpointOutput.OneOfVariant[BadRequest] = oneOfVariant(StatusCode.BadRequest, jsonBody[BadRequest].description("Bad Request"))
  val unprocessableEntity: EndpointOutput.OneOfVariant[UnprocessableEntity] =
    oneOfVariant(StatusCode.UnprocessableEntity, jsonBody[UnprocessableEntity].description("Unprocessable Entity"))

  implicit class PersistentErrorToProblemDetails[Z, A](res: ZIO[Z, PersistenceError, A]) {
    def mapPersistentError(serverRequest: ServerRequest): ZIO[Z, ProblemDetails, A] = {
      res.mapError(WithProblemDetails.mapPersistentError(_, serverRequest))
    }
    def mapBoth(serverRequest: ServerRequest): ZIO[Z, ProblemDetails, A] =
      res.foldZIO(
        e =>
          (e match {
            case error: PersistenceError.NotFound              => ZIO.logDebug(error.toString)
            case error: PersistenceError.AlreadyExists         => ZIO.logDebug(error.toString)
            case error: PersistenceError.MissingReference      => ZIO.logDebug(error.toString)
            case error: PersistenceError.UsedReferenceOnDelete => ZIO.logDebug(error.toString)
            case error: PersistenceError.UnexpectedError       => ZIO.logFatal(error.toString)
            case error: PersistenceError.ValidationErrors      => ZIO.logDebug(error.toString)
          }) *> ZIO.fail(WithProblemDetails.mapPersistentError(e, serverRequest)),
        r => ZIO.succeed(r)
      )
  }
}

object WithProblemDetails {

  val EntityNotFoundTitle      = "Entity not found"
  val MissingEntityTitle       = "Missing entity"
  val UnableToDeleteEntityTile = "Unable to delete entity"
  val ValidationErrorTitle     = "Validation Error"

  def mapPersistentError(error: PersistenceError, serverRequest: ServerRequest): ProblemDetails =
    error match {
      case PersistenceError.NotFound(key, value) =>
        NotFound(title = EntityNotFoundTitle, detail = Some(s""""$key"="$value" not found""")).withRequest(serverRequest)
      case PersistenceError.AlreadyExists(key, value) =>
        BadRequest(title = "Entity already exists", detail = Some(s""""$key="$value" already exists""")).withRequest(serverRequest)
      case PersistenceError.MissingReference(key, value) =>
        InternalServerError(
          title = MissingEntityTitle,
          detail = Some(s"$key=$value, foreign key is missing in dependant entity")
        ).withRequest(serverRequest)
      case PersistenceError.UsedReferenceOnDelete(key, value) =>
        BadRequest(
          title = UnableToDeleteEntityTile,
          detail = Some(s"$key=$value, $value is used by another entity")
        ).withRequest(serverRequest)

      case PersistenceError.ValidationErrors(errors) =>
        UnprocessableEntity(title = ValidationErrorTitle, errors = OValidationError.fromValidationErrorType(errors)).withRequest(serverRequest)
      case PersistenceError.UnexpectedError(throwable) =>
        InternalServerError(throwable).withRequest(serverRequest)
    }
}

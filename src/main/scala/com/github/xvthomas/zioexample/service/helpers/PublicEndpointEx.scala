package com.github.xvthomas.zioexample.service.helpers

import sttp.tapir.model.ServerRequest
import sttp.tapir.{PublicEndpoint, extractFromRequest}

// scalastyle:off class.type.parameter.name

trait PublicEndpointExAliases {
  type PublicEndpointEx[INPUT, OUTPUT] = PublicEndpoint[(INPUT, ServerRequest), ProblemDetails, OUTPUT, Any]
}

trait PublicEndpointEx extends PublicEndpointExAliases {
  implicit class Ex[INPUT, OUTPUT](ep: PublicEndpoint[INPUT, ProblemDetails, OUTPUT, Any]) {
    def withServerRequest: PublicEndpointEx[INPUT, OUTPUT] =
      ep.in(extractFromRequest(identity))
  }
}

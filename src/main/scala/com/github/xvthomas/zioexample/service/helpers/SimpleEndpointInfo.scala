package com.github.xvthomas.zioexample.service.helpers

import sttp.tapir.{AttributeMap, EndpointInfo}

class SimpleEndpointInfo(summary: String, tag: String) extends EndpointInfo(
    name = None,
    summary = Some(summary),
    description = None,
    tags = Vector(tag),
    deprecated = false,
    attributes = AttributeMap.Empty
  )

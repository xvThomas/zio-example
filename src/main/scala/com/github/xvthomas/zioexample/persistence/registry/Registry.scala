package com.github.xvthomas.zioexample.persistence.registry

import com.github.xvthomas.zioexample.persistence.PersistenceError
import zio.ZIO

class Registry {
  type UIORegistryResult[R] = ZIO[Any, PersistenceError, R]
}

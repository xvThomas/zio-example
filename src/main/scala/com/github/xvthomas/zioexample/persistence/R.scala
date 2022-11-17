package com.github.xvthomas.zioexample.persistence

import com.github.xvthomas.zioexample.persistence.registry.impl.slick.{DatabaseProviderImpl, PostRegistryImpl, UserRegistryImpl}
import com.github.xvthomas.zioexample.persistence.registry.{PostRegistry, UserRegistry}
import com.github.xvthomas.zioexample.persistence.validation.impl.zio.{PostValidatorImpl, UserValidatorImpl}
import com.github.xvthomas.zioexample.persistence.validation.{PostValidator, UserValidator}
import slick.interop.zio.DatabaseProvider
import zio.ZIO

sealed abstract case class R(
  userRegistry: UserRegistry,
  postRegistry: PostRegistry,
  userValidator: UserValidator,
  postValidator: PostValidator
)

case object R {
  type PersistenceLayers =
    DatabaseProvider with UserRegistry with PostRegistry with UserValidator with PostValidator

  def apply(): ZIO[PersistenceLayers, Nothing, R] =
    for {
      userRegistry  <- ZIO.service[UserRegistry]
      postRegistry  <- ZIO.service[PostRegistry]
      userValidator <- ZIO.service[UserValidator]
      postValidator <- ZIO.service[PostValidator]
    } yield new R(
      userRegistry,
      postRegistry,
      userValidator,
      postValidator
    ) {}

  implicit class ImplicitLayers[A](z: ZIO[PersistenceLayers, Any, A]) {
    def providePersistenceLayers: ZIO[Any, Any, A] =
      z.provide(
        DatabaseProviderImpl.layer,
        UserRegistryImpl.layer,
        PostRegistryImpl.layer,
        UserValidatorImpl.layer,
        PostValidatorImpl.layer
      )
  }
}

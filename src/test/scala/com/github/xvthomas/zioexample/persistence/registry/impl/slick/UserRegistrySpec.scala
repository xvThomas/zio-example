package com.github.xvthomas.zioexample.persistence.registry.impl.slick

import com.github.xvthomas.zioexample.persistence.PersistenceError
import com.github.xvthomas.zioexample.persistence.model.User
import com.github.xvthomas.zioexample.persistence.registry.UserRegistry
import zio.test.{Spec, TestAspect, TestEnvironment, ZIOSpecDefault, assertTrue}
import zio.{Scope, ZIO}

object UserRegistrySpec extends ZIOSpecDefault {

  def spec: Spec[TestEnvironment with Scope, Any] = {
    val user = User.unsafe("xvthomas", "xvthomas@github.com")
    suite("UserRegistrySpec")(
      test("An inserted user should be retrieved") {
        for {
          userRegistry <- ZIO.service[UserRegistry]
          _            <- userRegistry.insert(user)
          res          <- userRegistry.selectOne(user.login)
        } yield assertTrue(res == user)
      },
      test("A deleted user should not be retrieved") {
        for {
          userRegistry <- ZIO.service[UserRegistry]
          res0         <- userRegistry.selectOne(user.login)
          _            <- userRegistry.delete(user.login)
          res1         <- userRegistry.selectOne(user.login).either
        } yield {
          assertTrue(res0 == user)
          assertTrue(res1.fold(
            _ == PersistenceError.NotFound("login", user.login),
            _ => false
          ))
        }
      }
    )
      .provideShared(
        DatabaseProviderContainer.live,
        UserRegistryImpl.layer
      ) @@ TestAspect.sequential
  }

}

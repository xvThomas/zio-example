package com.github.xvthomas.zioexample.persistence.registry.impl.slick

import com.github.xvthomas.zioexample.persistence.PersistenceError
import com.github.xvthomas.zioexample.persistence.model.{Post, User}
import com.github.xvthomas.zioexample.persistence.registry.{PostRegistry, UserRegistry}
import zio.test.{Spec, TestAspect, TestEnvironment, ZIOSpecDefault, assertTrue}
import zio.{Scope, ZIO}

import java.time.Instant

// scalastyle:off method.length
object PostRegistrySpec extends ZIOSpecDefault {

  def spec: Spec[TestEnvironment with Scope, Any] = {
    val user  = User.unsafe("xvthomas", "xvthomas@github.com")
    val post0 = Post.unsafe(user.login, Instant.now(), "my first post")
    val post1 = Post.unsafe("nobody", Instant.now(), "my second post")

    suite("UserRegistrySpec")(
      test("An inserted post should be retrieved") {
        for {
          userRegistry <- ZIO.service[UserRegistry]
          postRegistry <- ZIO.service[PostRegistry]
          _            <- userRegistry.insert(user)
          id           <- postRegistry.insert(post0)
          res          <- postRegistry.selectOne(id)
        } yield assertTrue((res.userLogin, res.dateOfIssue, res.message) == (post0.userLogin, post0.dateOfIssue, post0.message))
      },
      test("A post associated to a missing user should not be inserted with MissingReference") {
        for {
          postRegistry <- ZIO.service[PostRegistry]
          res          <- postRegistry.insert(post1).either
        } yield assertTrue(res.fold(
          _ == PersistenceError.MissingReference("login", post1.userLogin),
          _ => false
        ))
      },
      test("A post should be upserted") {
        val updatedMessage = "an updated message"
        for {
          postRegistry <- ZIO.service[PostRegistry]
          posts        <- postRegistry.selectAllOf(post0.userLogin)
          post         <- ZIO.fromOption(posts.headOption)
          _            <- postRegistry.update(post.id, updatedMessage)
          res          <- postRegistry.selectOne(post.id)
        } yield {
          assertTrue(res == Post.unsafe(post.id, post.userLogin, post.dateOfIssue, updatedMessage))
        }
      },
      test("A removed post should not be retrieved") {
        for {
          postRegistry <- ZIO.service[PostRegistry]
          posts        <- postRegistry.selectAllOf(post0.userLogin)
          post         <- ZIO.fromOption(posts.headOption)
          _            <- postRegistry.delete(post.id)
          res          <- postRegistry.selectOne(post.id).either
        } yield {
          assertTrue(res.fold(
            _ == PersistenceError.NotFound("id", post.id.toString),
            _ => false
          ))
        }
      }
    )
      .provideShared(
        DatabaseProviderContainer.live,
        UserRegistryImpl.layer,
        PostRegistryImpl.layer
      ) @@ TestAspect.sequential
  }

}

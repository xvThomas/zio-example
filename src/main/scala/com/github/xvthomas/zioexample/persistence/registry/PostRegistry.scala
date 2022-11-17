package com.github.xvthomas.zioexample.persistence.registry

import com.github.xvthomas.zioexample.persistence.model.Post

trait PostRegistry extends Registry {
  private[registry] def drop(): UIORegistryResult[Unit]
  def create: UIORegistryResult[Unit]
  def insert(post: Post): UIORegistryResult[Long]
  def update(id: Long, message: String): UIORegistryResult[Unit]
  def delete(id: Long): UIORegistryResult[Unit]
  def selectOne(id: Long): UIORegistryResult[Post]
  def selectAllOf(userLogin: String): UIORegistryResult[Seq[Post]]

}

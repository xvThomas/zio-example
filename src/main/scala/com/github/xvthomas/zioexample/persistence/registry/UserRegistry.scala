package com.github.xvthomas.zioexample.persistence.registry

import com.github.xvthomas.zioexample.persistence.model.User

trait UserRegistry extends Registry {
  private[registry] def drop: UIORegistryResult[Unit]
  def create: UIORegistryResult[Unit]
  def insert(user: User): UIORegistryResult[Unit]
  def update(user: User): UIORegistryResult[Unit]
  def delete(login: String): UIORegistryResult[Unit]
  def selectOne(login: String): UIORegistryResult[User]
  def selectAll(): UIORegistryResult[Seq[User]]
}

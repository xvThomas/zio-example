package com.github.xvthomas.zioexample.persistence.registry.impl.slick

private[impl] case object PostgresErrorCodes {
  val foreignKeyViolation: String = "23503"
  val uniqueViolation: String     = "23505"
  val duplicateTable: String      = "42P07"
  val undefinedTable: String      = "42P01"
}

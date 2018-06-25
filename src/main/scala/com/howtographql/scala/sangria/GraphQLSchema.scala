package com.howtographql.scala.sangria

import sangria.schema.{ListType, ObjectType}
import models._
// #
import sangria.schema._
import sangria.macros.derive._

object GraphQLSchema {

  // 1
  implicit val LinkType = deriveObjectType[Unit, Link]()

  // 2
  val QueryType = ObjectType(
    "Query",
    fields[MyContext, Unit](
      Field("allLinks", ListType(LinkType), resolve = c => c.ctx.dao.allLinks)
    )
  )

  // 3
  val SchemaDefinition = Schema(QueryType)
}

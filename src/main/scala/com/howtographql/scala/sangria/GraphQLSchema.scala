package com.howtographql.scala.sangria

import akka.http.scaladsl.model.DateTime
import sangria.schema.{ListType, ObjectType}
import models._
import sangria.ast.StringValue
import sangria.execution.deferred.{DeferredResolver, Fetcher, HasId}
import sangria.schema._
import sangria.macros.derive._

object GraphQLSchema {

  implicit val GraphQLDateTime = ScalarType[DateTime](//1
    "DateTime",//2
    coerceOutput = (dt, _) => dt.toString, //3
    coerceInput = { //4
      case StringValue(dt, _, _ ) => DateTime.fromIsoDateTimeString(dt).toRight(DateTimeCoerceViolation)
      case _ => Left(DateTimeCoerceViolation)
    },
    coerceUserInput = { //5
      case s: String => DateTime.fromIsoDateTimeString(s).toRight(DateTimeCoerceViolation)
      case _ => Left(DateTimeCoerceViolation)
    }
  )

  val IdentifiableType = InterfaceType(
    "Identifiable",
    fields[Unit, Identifiable](
      Field("id", IntType, resolve = _.value.id)
    )
  )

  val LinkType = deriveObjectType[Unit, Link](
    Interfaces(IdentifiableType),
    ReplaceField("createdAt", Field("createdAt", GraphQLDateTime, resolve = _.value.createdAt))
  )

  val UserType = deriveObjectType[Unit, User](
    Interfaces(IdentifiableType)
  )
  val VoteType = deriveObjectType[Unit, Vote](
    Interfaces(IdentifiableType)
  )

  val linksFetcher = Fetcher(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getLinks(ids)
  )

  val usersFetcher = Fetcher(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getUsers(ids)
  )

  val votesFetcher = Fetcher(
    (ctx: MyContext, ids: Seq[Int]) => ctx.dao.getVotes(ids)
  )

  val Resolver = DeferredResolver.fetchers(linksFetcher, usersFetcher, votesFetcher)


  val Id = Argument("id", IntType)
  val Ids = Argument("ids", ListInputType(IntType))

  val QueryType = ObjectType(
    "Query",
    fields[MyContext, Unit](
      Field("allLinks", ListType(LinkType), resolve = c => c.ctx.dao.allLinks),
      Field("link",
        OptionType(LinkType),
        arguments = Id :: Nil,
        resolve = c => linksFetcher.deferOpt(c.arg(Id))
      ),
      Field("links",
        ListType(LinkType),
        arguments = Ids :: Nil,
        resolve = c => linksFetcher.deferSeq(c.arg(Ids))
      ),
      Field("users",
        ListType(UserType),
        arguments = List(Ids),
        resolve = c => usersFetcher.deferSeq(c.arg(Ids))
      ),
      Field("votes",
        ListType(VoteType),
        arguments = List(Ids),
        resolve = c => votesFetcher.deferSeq(c.arg(Ids))
      )
    )
  )

  val SchemaDefinition = Schema(QueryType)
}

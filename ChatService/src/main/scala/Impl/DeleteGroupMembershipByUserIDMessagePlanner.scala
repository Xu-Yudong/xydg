package Impl


import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import org.slf4j.LoggerFactory
import io.circe.Json
import cats.implicits._
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.joda.time.DateTime
import cats.implicits.*
import Common.DBAPI._
import Common.API.{PlanContext, Planner}
import cats.effect.IO
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}
import Common.ServiceUtils.schemaName

import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.joda.time.DateTime
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class DeleteGroupMembershipByUserIDMessagePlanner(
  userID: String,
  override val planContext: PlanContext
) extends Planner[String] {
  val logger = LoggerFactory.getLogger(
    this.getClass.getSimpleName + "_" + planContext.traceID.id
  )

  override def plan(using planContext: PlanContext): IO[String] = {
    for {
      // Log the beginning of the operation
      _ <- IO(logger.info(s"开始清理用户${userID}的群成员关系"))

      // Fetch group memberships linked to the userID
      groupMemberships <- fetchGroupMemberships()
      _ <- IO(logger.info(s"找到${groupMemberships.size}条记录关联用户ID为${userID}"))

      // Delete the fetched records
      deleteResult <- deleteGroupMemberships()
      _ <- IO(logger.info(s"用户ID为${userID}的群成员关系删除完成，结果：${deleteResult}"))
    } yield "Operation(s) done successfully"
  }

  private def fetchGroupMemberships()(using PlanContext): IO[List[Json]] = {
    val sql =
      s"""
         |SELECT * 
         |FROM ${schemaName}.group_member_table
         |WHERE user_id = ?;
       """.stripMargin
    logger.info(s"SQL查询语句: ${sql}")
    readDBRows(sql, List(SqlParameter("String", userID)))
  }

  private def deleteGroupMemberships()(using PlanContext): IO[String] = {
    val sql =
      s"""
         |DELETE 
         |FROM ${schemaName}.group_member_table
         |WHERE user_id = ?;
       """.stripMargin
    logger.info(s"SQL删除语句: ${sql}")
    writeDB(sql, List(SqlParameter("String", userID)))
  }
}
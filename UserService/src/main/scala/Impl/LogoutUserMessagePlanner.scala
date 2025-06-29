package Impl


import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import org.slf4j.LoggerFactory
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.joda.time.DateTime
import cats.implicits.*
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

import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class LogoutUserMessagePlanner(
                                     sessionToken: String,
                                     override val planContext: PlanContext
                                   ) extends Planner[String] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[String] = {
    for {
      // Step 1: Validate the sessionToken
      _ <- IO(logger.info(s"[Step 1] 验证 sessionToken: ${sessionToken} 是否有效"))
      isValid <- validateSessionToken()
      _ <- if (!isValid) IO.raiseError(new IllegalArgumentException(s"无效的 sessionToken: ${sessionToken}")) 
           else IO(logger.info("[Step 1] 验证通过, sessionToken 是有效的"))

      // Step 2: Delete session record in database
      _ <- IO(logger.info(s"[Step 2] 删除数据库中 sessionToken: ${sessionToken} 的会话记录"))
      deleteResult <- deleteSessionRecord()
      _ <- IO(logger.info(s"[Step 2] 会话记录删除结果: ${deleteResult}"))

      // Step 3: Return success message
      _ <- IO(logger.info("[Step 3] 登出成功"))
    } yield "登出成功"
  }

  private def validateSessionToken()(using PlanContext): IO[Boolean] = {
    val sql =
      s"""
         SELECT COUNT(*) 
         FROM ${schemaName}.user_session_table
         WHERE session_token = ?;
       """
    IO(logger.info(s"[validateSessionToken] SQL 查询语句: ${sql}"))
    readDBInt(sql, List(SqlParameter("String", sessionToken))).map(_ > 0)
  }

  private def deleteSessionRecord()(using PlanContext): IO[String] = {
    val sql =
      s"""
         DELETE FROM ${schemaName}.user_session_table
         WHERE session_token = ?;
       """
    IO(logger.info(s"[deleteSessionRecord] SQL 删除语句: ${sql}"))
    writeDB(sql, List(SqlParameter("String", sessionToken)))
  }
}
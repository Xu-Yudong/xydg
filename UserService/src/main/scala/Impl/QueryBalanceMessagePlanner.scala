package Impl


import Utils.UserAccountProcess.queryUserBalance
import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import org.slf4j.LoggerFactory
import org.joda.time.DateTime
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import cats.implicits._
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
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
import Utils.UserAccountProcess.queryUserBalance
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class QueryBalanceMessagePlanner(
                                       sessionToken: String,
                                       override val planContext: PlanContext
                                     ) extends Planner[Double] {
  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Double] = {
    for {
      // Step 1: 从UserSessionTable提取sessionToken对应的userID
      _ <- IO(logger.info(s"开始提取sessionToken(${sessionToken})对应的userID"))
      userID <- getSessionUserID(sessionToken)

      // Step 2: 获取用户账户余额
      _ <- IO(logger.info(s"开始查询userID(${userID})的账户余额"))
      balance <- queryUserBalance(userID)

      // Step 3: 日志记录并返回账户余额信息
      _ <- IO(logger.info(s"查询完成，userID(${userID})的账户余额为${balance}"))
    } yield balance
  }

  private def getSessionUserID(sessionToken: String)(using PlanContext): IO[String] = {
    val sql =
      s"""
         SELECT user_id
         FROM ${schemaName}.user_session_table
         WHERE session_token = ?;
       """
    val parameters = List(SqlParameter("String", sessionToken))
    for {
      _ <- IO(logger.info(s"执行SQL查询: $sql, 参数为: ${parameters}"))
      result <- readDBJsonOptional(sql, parameters)
      userID <- result match {
        case Some(json) =>
          val id = decodeField[String](json, "user_id")
          IO(logger.info(s"SQL查询成功，提取到的userID为: ${id}")).as(id)
        case None =>
          val errorMessage = s"sessionToken(${sessionToken})未找到对应的userID"
          IO(logger.error(errorMessage)) >>
            IO.raiseError(new IllegalStateException(errorMessage))
      }
    } yield userID
  }
}
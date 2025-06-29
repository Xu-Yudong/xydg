package Impl


import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import org.slf4j.LoggerFactory
import io.circe.Json
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

import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class DeleteMessagesByUserIDMessagePlanner(
                                                 userID: String,
                                                 override val planContext: PlanContext
                                               ) extends Planner[String] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[String] = {
    for {
      // Step 1: Log the start of the process
      _ <- IO(logger.info(s"开始根据用户ID[$userID]删除聊天消息"))

      // Step 1.1: Query to get all messages associated with the userID
      _ <- IO(logger.info(s"查询与用户ID[$userID]相关的所有消息记录"))
      messages <- queryMessagesByUserID()
      _ <- IO(logger.info(s"查询到的消息记录数为: ${messages.size}"))

      // Step 1.2: Delete all messages associated with the userID
      _ <- IO(logger.info(s"执行删除操作，移除用户ID[$userID]相关的消息记录"))
      deleteResult <- deleteMessagesByUserID()
      _ <- IO(logger.info(s"删除结果: ${deleteResult}"))

      // Step 2: Return success result
      result <- IO("Messages deleted successfully.")
    } yield result
  }

  private def queryMessagesByUserID()(using PlanContext): IO[List[Json]] = {
    val sql =
      s"""
         SELECT * 
         FROM ${schemaName}.chat_message_table 
         WHERE sender_id = ? OR receiver_id = ?;
       """
    IO(logger.info(s"查询消息的SQL语句: ${sql}")) >>
      readDBRows(
        sql,
        List(
          SqlParameter("String", userID),
          SqlParameter("String", userID)
        )
      )
  }

  private def deleteMessagesByUserID()(using PlanContext): IO[String] = {
    val sql =
      s"""
         DELETE FROM ${schemaName}.chat_message_table 
         WHERE sender_id = ? OR receiver_id = ?;
       """
    IO(logger.info(s"删除消息的SQL语句: ${sql}")) >>
      writeDB(
        sql,
        List(
          SqlParameter("String", userID),
          SqlParameter("String", userID)
        )
      )
  }
}
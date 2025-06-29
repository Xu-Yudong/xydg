package Impl


/**
 * Planner for SendMessageToFriendMessage: 好友聊天消息发送.
 */
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

import java.util.UUID
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class SendMessageToFriendMessagePlanner(
                                               sessionToken: String,
                                               friendID: String,
                                               content: String,
                                               override val planContext: PlanContext
                                             ) extends Planner[String] {

  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  /**
   * Implements the "plan" method based on requirements
   */
  override def plan(using planContext: PlanContext): IO[String] = for {
    // Step 1: Validate sessionToken and extract userID
    _ <- IO(logger.info(s"开始验证 sessionToken: ${sessionToken}"))
    userID <- validateSessionToken

    // Step 2: Insert a message record into ChatMessageTable
    _ <- IO(logger.info(s"生成消息记录并插入 ChatMessageTable，senderID=${userID}，receiverID=${friendID}, 消息内容=${content}"))
    _ <- insertMessageRecord(userID)

    // Step 3: Return the success information
    _ <- IO(logger.info(s"消息发送成功"))
  } yield "发送成功"

  /**
   * Validates sessionToken and retrieves userID
   */
  private def validateSessionToken(using PlanContext): IO[String] = {
    logger.info(s"开始创建验证 sessionToken 的数据库指令")
    val sql =
      s"""
        SELECT user_id
        FROM ${schemaName}.users
        WHERE session_token = ?;
      """
    logger.info(s"指令为：${sql}")
    logger.info(s"执行验证 sessionToken 的数据库指令")
    readDBJson(sql, List(SqlParameter("String", sessionToken))).map { json =>
      decodeField[String](json, "user_id") // Extract "user_id" from result JSON
    }
  }

  /**
   * Inserts a message record
   */
  private def insertMessageRecord(senderID: String)(using PlanContext): IO[Unit] = {
    logger.info("开始创建插入消息记录的数据库指令")
    val sql =
      s"""
        INSERT INTO ${schemaName}.chat_message_table
        (message_id, sender_id, receiver_id, content, timestamp, status)
        VALUES (?, ?, ?, ?, ?, ?);
      """
    val messageID = UUID.randomUUID().toString
    val timestamp = DateTime.now().getMillis.toString
    val status = "unread"
    val parameters = List(
      SqlParameter("String", messageID),
      SqlParameter("String", senderID),
      SqlParameter("String", friendID),
      SqlParameter("String", content),
      SqlParameter("DateTime", timestamp),
      SqlParameter("String", status)
    )
    logger.info(s"指令为：${sql}")
    logger.info(s"消息记录为 messageID=${messageID}, senderID=${senderID}, receiverID=${friendID}, content=${content}, timestamp=${timestamp}, status=${status}")
    writeDB(sql, parameters).void
  }
}
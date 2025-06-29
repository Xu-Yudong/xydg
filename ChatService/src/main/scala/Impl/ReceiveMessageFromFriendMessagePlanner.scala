package Impl


import Objects.ChatService.ChatMessage
import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.ParameterList
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import org.slf4j.LoggerFactory
import org.joda.time.DateTime
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import cats.implicits.*
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
import Objects.ChatService.ChatMessage
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class ReceiveMessageFromFriendMessagePlanner(
                                                   sessionToken: String,
                                                   friendID: String,
                                                   override val planContext: PlanContext
                                                 ) extends Planner[List[ChatMessage]] {

  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[List[ChatMessage]] = {
    for {
      // Step 1: Validate user session and extract userID
      _ <- IO(logger.info(s"Validating sessionToken: ${sessionToken}"))
      userID <- validateAndExtractUserID()

      // Step 2: Fetch unread messages from the database
      _ <- IO(logger.info(s"Fetching unread messages from ${friendID} for userID: ${userID}"))
      unreadMessages <- fetchUnreadMessages(userID, friendID)

      // Step 3: Update message statuses to 'delivered'
      _ <- IO(logger.info(s"Updating message statuses to 'delivered' for ${unreadMessages.size} messages"))
      _ <- updateMessageStatuses(unreadMessages)

      // Step 4: Build response result
      _ <- IO(logger.info(s"Building chat message objects for ${unreadMessages.size} messages"))
      chatMessages <- IO(buildChatMessages(unreadMessages))
    } yield chatMessages
  }

  private def validateAndExtractUserID()(using PlanContext): IO[String] = {
    for {
      isValid <- validateSessionToken(sessionToken)
      _ <- if (!isValid)
        IO.raiseError(new IllegalStateException(s"Invalid sessionToken: ${sessionToken}"))
      else
        IO.unit
      userID <- extractUserIDFromToken(sessionToken)
    } yield userID
  }

  private def fetchUnreadMessages(userID: String, friendID: String)(using PlanContext): IO[List[Json]] = {
    val query =
      s"""
        SELECT *
        FROM ${schemaName}.chat_message_table
        WHERE sender_id = ? AND receiver_id = ? AND status = 'unread';
      """
    val params = List(
      SqlParameter("String", friendID),
      SqlParameter("String", userID)
    )
    readDBRows(query, params)
  }

  private def updateMessageStatuses(messages: List[Json])(using PlanContext): IO[Unit] = {
    val updateQuery =
      s"""
        UPDATE ${schemaName}.chat_message_table
        SET status = 'delivered'
        WHERE message_id = ?;
      """
    val paramsList = messages.map { msg =>
      val messageID = decodeField[String](msg, "message_id")
      ParameterList(List(SqlParameter("String", messageID)))
    }
    writeDBList(updateQuery, paramsList).void
  }

  private def buildChatMessages(messages: List[Json]): List[ChatMessage] = {
    messages.map(decodeType[ChatMessage])
  }

  private def validateSessionToken(token: String)(using PlanContext): IO[Boolean] = {
    val query =
      s"""
        SELECT EXISTS(
          SELECT 1
          FROM ${schemaName}.sessions
          WHERE session_token = ?
        );
      """
    readDBBoolean(query, List(SqlParameter("String", token)))
  }

  private def extractUserIDFromToken(token: String)(using PlanContext): IO[String] = {
    val query =
      s"""
        SELECT user_id
        FROM ${schemaName}.sessions
        WHERE session_token = ?;
      """
    readDBString(query, List(SqlParameter("String", token)))
  }
}
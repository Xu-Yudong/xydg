package Impl


import APIs.ChatService.{DeleteGroupMembershipByUserIDMessage, DeleteMessagesByUserIDMessage}
import APIs.FriendshipService.DeleteFriendsByUserIDMessage
import Utils.UserAccountProcess.deleteUserByID
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
import APIs.ChatService.DeleteGroupMembershipByUserIDMessage
import APIs.ChatService.DeleteMessagesByUserIDMessage
import Utils.UserAccountProcess.deleteUserByID
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class DeleteUserMessagePlanner(
                                     sessionToken: String,
                                     override val planContext: PlanContext
                                   ) extends Planner[String] {
  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[String] = {
    for {
      // Step 1: Validate sessionToken and retrieve userID
      userID <- getUserIDBySessionToken(sessionToken)

      // Step 2: Delete user by ID (main cleanup process)
      _ <- deleteUserUsingID(userID)

      // Step 3.1: Clean up user's friendships
      _ <- cleanUpFriends(userID)

      // Step 3.2: Delete user's messages
      _ <- cleanUpMessages(userID)

      // Step 3.3: Remove user from group memberships
      _ <- cleanUpGroupMemberships(userID)

      // Step 4: Return success message
      result <- IO {
        val successMessage = s"用户[${userID}]及其相关数据删除成功"
        logger.info(successMessage)
        successMessage
      }
    } yield result
  }

  // Step 1: Retrieve userID based on sessionToken
  private def getUserIDBySessionToken(sessionToken: String)(using PlanContext): IO[String] = {
    logger.info(s"根据会话令牌[$sessionToken]查找用户ID")
    val sql = s"SELECT user_id FROM ${schemaName}.user_session_table WHERE session_token = ?;"
    readDBString(sql, List(SqlParameter("String", sessionToken))).map { userID =>
      logger.info(s"成功查找到用户ID：$userID")
      userID
    }
  }

  // Step 2: Delete main user data using deleteUserByID utility method
  private def deleteUserUsingID(userID: String)(using PlanContext): IO[Unit] = {
    logger.info(s"调用 deleteUserByID 清理用户ID [${userID}] 的核心数据")
    deleteUserByID(userID).map { result =>
      logger.info(s"deleteUserByID 返回结果：$result")
    }
  }

  // Step 3.1: Clean up user's friend relationships
  private def cleanUpFriends(userID: String)(using PlanContext): IO[Unit] = {
    logger.info(s"调用 DeleteFriendsByUserIDMessage 接口清理用户[${userID}]的好友关系")
    DeleteFriendsByUserIDMessage(userID).send.map { result =>
      logger.info(s"DeleteFriendsByUserIDMessage 返回结果：$result")
    }
  }

  // Step 3.2: Clean up user's messages
  private def cleanUpMessages(userID: String)(using PlanContext): IO[Unit] = {
    logger.info(s"调用 DeleteMessagesByUserIDMessage 接口清理用户[${userID}]的消息记录")
    DeleteMessagesByUserIDMessage(userID).send.map { result =>
      logger.info(s"DeleteMessagesByUserIDMessage 返回结果：$result")
    }
  }

  // Step 3.3: Remove user from group memberships
  private def cleanUpGroupMemberships(userID: String)(using PlanContext): IO[Unit] = {
    logger.info(s"调用 DeleteGroupMembershipByUserIDMessage 接口移除用户[${userID}]的群聊成员信息")
    DeleteGroupMembershipByUserIDMessage(userID).send.map { result =>
      logger.info(s"DeleteGroupMembershipByUserIDMessage 返回结果：$result")
    }
  }
}
package Impl


import Objects.ChatService.GroupRole
import Objects.UserService.UserRole
import Objects.UserService.User
import Utils.GroupManagementProcess.createGroup
import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import io.circe._
import cats.effect.IO
import org.slf4j.LoggerFactory
import org.joda.time.DateTime
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
import Objects.UserService.User
import io.circe.syntax._
import io.circe.generic.auto._
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class CreateGroupChatMessage(sessionToken: String, groupName: String) extends Common.API.API[String]("CreateGroupChat")

case class CreateGroupChatMessagePlanner(
                                          sessionToken: String,
                                          groupName: String,
                                          override val planContext: PlanContext
                                        ) extends Planner[String] {
  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[String] = {
    for {
      // Step 1: Validate sessionToken and extract userID
      _ <- IO(logger.info(s"[Step 1] 验证 sessionToken & 提取 userID - sessionToken=${sessionToken}"))
      userID <- validateSessionAndExtractUserID(sessionToken)
      _ <- IO(logger.info(s"[Step 1] 验证完成，解析到的 userID=${userID}"))

      // Step 2: Call createGroup to create the group chat
      _ <- IO(logger.info(s"[Step 2] 调用群管理工具方法 createGroup 创建群聊"))
      groupID <- createGroup(userID, groupName)
      _ <- IO(logger.info(s"[Step 2] 群聊创建成功，groupID=${groupID}"))

    } yield groupID
  }

  private def validateSessionAndExtractUserID(sessionToken: String)(using PlanContext): IO[String] = {
    for {
      _ <- IO(logger.info(s"[Step 1.1] 验证 sessionToken=${sessionToken} 的合法性"))
      sessionQuery <- IO {
        s"""
        SELECT user_id
        FROM ${schemaName}.user_session
        WHERE session_token = ?;
        """
      }
      sessionParams <- IO {
        List(SqlParameter("String", sessionToken))
      }
      userJsonOpt <- readDBJsonOptional(sessionQuery, sessionParams)
      userID <- IO {
        userJsonOpt.map(json => decodeField[String](json, "user_id"))
          .getOrElse(throw new IllegalStateException(s"[Step 1.1] sessionToken 无效或过期：${sessionToken}"))
      }
      _ <- IO(logger.info(s"[Step 1.2] 会话合法性验证通过，解析到 userID=${userID}"))
    } yield userID
  }
}
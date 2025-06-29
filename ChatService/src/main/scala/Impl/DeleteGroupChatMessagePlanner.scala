package Impl


import Utils.GroupManagementProcess.deleteGroup
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
import org.joda.time.DateTime
import cats.implicits.*
import Common.DBAPI._
import Common.API.{PlanContext, Planner}
import cats.effect.IO
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}
import Common.ServiceUtils.schemaName
import Utils.GroupManagementProcess.deleteGroup
import io.circe._
import io.circe.syntax.*
import io.circe.generic.auto._
import io.circe.syntax._
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class DeleteGroupChatMessagePlanner(
    sessionToken: String,
    groupID: String,
    override val planContext: PlanContext
) extends Planner[String] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[String] = {
    for {
      // Step 1: 验证sessionToken并提取userID
      _ <- IO(logger.info("[Step 1] 验证sessionToken，并提取userID"))
      userID <- validateSessionTokenAndExtractUserID(sessionToken)

      // Step 2: 校验是否为群聊创建者
      _ <- IO(logger.info(s"[Step 2] 验证用户是否为群聊[${groupID}]的创建者"))
      isCreator <- checkGroupCreator(groupID, userID)
      _ <- if (!isCreator) {
        val errorMsg = s"[授权错误] 用户[${userID}]不是群聊[${groupID}]的创建者，无法执行删除操作"
        IO.raiseError(new IllegalAccessException(errorMsg))
      } else IO.unit

      // Step 3: 调用deleteGroup方法删除群聊及关联数据
      _ <- IO(logger.info(s"[Step 3] 删除群聊[${groupID}]及关联数据"))
      deleteResult <- deleteGroup(groupID, userID)

      // Step 4: 返回操作成功信息
      _ <- IO(logger.info(s"[Step 4] 操作成功，返回结果: ${deleteResult}"))
    } yield deleteResult
  }

  // 子步骤 1: 验证sessionToken，并提取userID
  private def validateSessionTokenAndExtractUserID(sessionToken: String)(using PlanContext): IO[String] = {
    for {
      _ <- IO(logger.info(s"检查sessionToken的有效性: ${sessionToken}"))
      sessionQuery =
        s"SELECT user_id, token_expiry FROM ${schemaName}.user_session_table WHERE session_token = ?"
      sessionInfoOpt <- readDBJsonOptional(
        sessionQuery,
        List(SqlParameter("String", sessionToken))
      )
      userID <- sessionInfoOpt match {
        case Some(json) =>
          val userID = decodeField[String](json, "user_id")
          val tokenExpiry = decodeField[DateTime](json, "token_expiry")
          if (tokenExpiry.isBeforeNow) {
            IO.raiseError(new IllegalStateException(s"Session token已经过期: ${sessionToken}"))
          } else IO.pure(userID)
        case None =>
          IO.raiseError(new IllegalStateException(s"sessionToken[${sessionToken}]未找到, 请重新登录"))
      }
      _ <- IO(logger.info(s"当前用户ID: ${userID} 与sessionToken: ${sessionToken}匹配成功"))
    } yield userID
  }

  // 子步骤 2: 验证groupID的创建者是否为当前的userID
  private def checkGroupCreator(groupID: String, userID: String)(using PlanContext): IO[Boolean] = {
    for {
      _ <- IO(logger.info(s"从数据库中查询群聊[${groupID}]的创建者"))
      creatorQuery =
        s"SELECT creator_id FROM ${schemaName}.group_chat_table WHERE group_id = ?"
      creatorID <- readDBString(
        creatorQuery,
        List(SqlParameter("String", groupID))
      )
      _ <- IO(logger.info(s"从数据库中找到的群聊的创建者ID: ${creatorID}"))
    } yield creatorID == userID
  }
}
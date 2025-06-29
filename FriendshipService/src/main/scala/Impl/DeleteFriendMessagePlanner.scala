package Impl


import Common.API.{PlanContext, Planner}
import Utils.FriendRelationProcess.deleteFriend
import Common.DBAPI._
import Common.Object.SqlParameter
import cats.effect.IO
import org.slf4j.LoggerFactory
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.joda.time.DateTime
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import Common.ServiceUtils.schemaName
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
import Utils.FriendRelationProcess.deleteFriend
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class DeleteFriendMessagePlanner(
                                       sessionToken: String,
                                       friendID: String,
                                       override val planContext: PlanContext
                                     ) extends Planner[String] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[String] = {
    for {
      // Step 1: Validate sessionToken and extract userID
      _ <- IO(logger.info(s"[Step 1] 验证 sessionToken 并提取用户 ID，sessionToken: ${sessionToken}"))
      userID <- validateSessionTokenAndExtractUserID()

      // Step 2: Call deleteFriend method to remove the friend relationship
      _ <- IO(logger.info(s"[Step 2] 调用 deleteFriend 方法，移除好友关系，userID=${userID}, friendID=${friendID}"))
      deleteResult <- deleteFriend(userID, friendID)

      // Step 3: Return success message
      _ <- IO(logger.info(s"[Step 3] 删除好友操作结果: ${deleteResult}"))
    } yield deleteResult
  }

  private def validateSessionTokenAndExtractUserID()(using PlanContext): IO[String] = {
    val sql = s"SELECT user_id FROM ${schemaName}.user_sessions WHERE session_token = ?"
    val parameters = List(SqlParameter("String", sessionToken))

    for {
      _ <- IO(logger.info(s"执行验证 sessionToken 的 SQL 语句: ${sql}, 参数: ${parameters.map(_.value).mkString(", ")}"))
      userJsonOpt <- readDBJsonOptional(sql, parameters)
      userID <- userJsonOpt match {
        case Some(json) =>
          val extractedUserID = decodeField[String](json, "user_id")
          IO(logger.info(s"成功验证 sessionToken, 提取到的 userID: ${extractedUserID}")).as(extractedUserID)
        case None =>
          val errorMessage = s"验证失败，未找到有效的 sessionToken: ${sessionToken}"
          IO(logger.error(errorMessage)) >> IO.raiseError(new IllegalArgumentException("Invalid sessionToken."))
      }
    } yield userID
  }
}
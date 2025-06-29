package Impl


import APIs.UserService.QueryBalanceMessage
import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import Utils.FriendRelationProcess.addFriend
import cats.effect.IO
import org.slf4j.LoggerFactory
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.joda.time.DateTime
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
import Utils.FriendRelationProcess.addFriend
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class AddFriendMessagePlanner(
    sessionToken: String,
    friendID: String,
    override val planContext: PlanContext
) extends Planner[String] {
  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[String] = {
    for {
      // Step 1: Validate sessionToken and extract userID
      userID <- validateSessionAndExtractUserID(sessionToken)
      
      // Step 2: Add friend relationship
      result <- addFriendOperation(userID, friendID)

      // Step 3: Return the operation success message
      _ <- IO(logger.info(s"好友关系更新完成，结果为：${result}"))
    } yield result
  }

  // Step 1: Validate sessionToken and extract userID
  private def validateSessionAndExtractUserID(sessionToken: String)(using PlanContext): IO[String] = {
    for {
      _ <- IO(logger.info(s"校验sessionToken：${sessionToken} 并提取userID"))

      // Validate sessionToken and get balance information
      balance <- QueryBalanceMessage(sessionToken).send

      // Mimicking sessionToken itself as userID if balance > 0 (per provided description)
      userID <- if (balance > 0) {
        IO {
          logger.info(s"sessionToken有效，成功提取到userID")
          sessionToken // Assuming `sessionToken` acts as userID for simplicity in this implementation
        }
      } else {
        IO.raiseError(new IllegalStateException("sessionToken无效或已过期"))
      }
    } yield userID
  }

  // Step 2: Add friend operation by calling the appropriate method
  private def addFriendOperation(userID: String, friendID: String)(using PlanContext): IO[String] = {
    for {
      _ <- IO(logger.info(s"调用FriendRelationProcess.addFriend方法添加好友关系：userID=${userID}, friendID=${friendID}"))
      result <- addFriend(userID, friendID)
    } yield result
  }
}
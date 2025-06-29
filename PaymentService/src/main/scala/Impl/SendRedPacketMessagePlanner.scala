package Impl


import Utils.RedPacketProcess.createRedPacket
import Objects.PaymentService.AmountChangeLog
import Objects.PaymentService.ChangeType
import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.{ParameterList, SqlParameter}
import Common.ServiceUtils.schemaName
import cats.effect.IO
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import io.circe.Json
import io.circe.syntax._
import io.circe.generic.auto._
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
import Objects.PaymentService.ChangeType
import io.circe._
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class SendRedPacketMessagePlanner(
    sessionToken: String,
    groupID: String,
    amount: Double,
    participantCount: Int,
    override val planContext: PlanContext
) extends Planner[String] {
  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[String] = {
    for {
      // Step 1: 验证 sessionToken，并获取 userID
      _ <- IO(logger.info(s"Step 1: Verifying sessionToken: $sessionToken"))
      userID <- validateSessionToken(sessionToken)

      // Step 2: 验证余额是否足够
      _ <- IO(logger.info(s"Step 2: Checking if balance is sufficient for user $userID and amount $amount"))
      _ <- validateBalance(userID, amount)

      // Step 3: 创建红包记录，并扣除余额
      _ <- IO(logger.info(s"Step 3: Creating red packet record and deducting balance for user $userID"))
      redPacketID <- createRedPacketRecord(userID, groupID, amount, participantCount)

      // Step 4: 返回生成的红包ID
      _ <- IO(logger.info(s"Step 4: Successfully generated red packet ID: $redPacketID"))
    } yield redPacketID
  }

  private def validateSessionToken(sessionToken: String)(using PlanContext): IO[String] = {
    val sql =
      s"""
        SELECT user_id
        FROM ${schemaName}.user_sessions
        WHERE session_token = ? AND valid = true;
      """
    readDBJsonOptional(sql, List(SqlParameter("String", sessionToken))).flatMap {
      case Some(userIDJson) =>
        val userID = decodeField[String](userIDJson, "user_id")
        IO(logger.info(s"Session validated successfully, found user_id: ${userID}")).map(_ => userID)
      case None =>
        val errorMsg = s"Session validation failed, invalid sessionToken: ${sessionToken}"
        IO(logger.error(errorMsg)) *> IO.raiseError(new IllegalArgumentException(errorMsg))
    }
  }

  private def validateBalance(userID: String, requiredAmount: Double)(using PlanContext): IO[Unit] = {
    val sql =
      s"""
        SELECT balance
        FROM ${schemaName}.user_balance_table
        WHERE user_id = ?;
      """
    readDBJsonOptional(sql, List(SqlParameter("String", userID))).flatMap {
      case Some(balanceJson) =>
        val balance = decodeField[Double](balanceJson, "balance")
        if (balance >= requiredAmount) {
          IO(logger.info(s"Balance check passed: Current balance is ${balance}, required amount is ${requiredAmount}"))
        } else {
          val errorMsg = s"Insufficient balance for user $userID: Current balance is ${balance}, required amount is ${requiredAmount}"
          IO(logger.error(errorMsg)) *> IO.raiseError(new IllegalArgumentException(errorMsg))
        }
      case None =>
        val errorMsg = s"Balance check failed: No balance record found for user $userID"
        IO(logger.error(errorMsg)) *> IO.raiseError(new IllegalArgumentException(errorMsg))
    }
  }

  private def createRedPacketRecord(
      userID: String,
      groupID: String,
      amount: Double,
      participantCount: Int
  )(using PlanContext): IO[String] = {
    for {
      // 调用创建红包的工具方法，生成红包记录并扣减余额
      redPacketID <- createRedPacket(groupID, userID, amount, List.fill(participantCount)("")).flatTap { id =>
        IO(logger.info(s"Red packet created successfully with ID: ${id}"))
      }
    } yield redPacketID
  }
}
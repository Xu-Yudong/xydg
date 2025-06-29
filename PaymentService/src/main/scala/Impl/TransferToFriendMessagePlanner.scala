package Impl


import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import Utils.TransactionProcess.transferAmount
import APIs.UserService.QueryBalanceMessage
import APIs.AuditService.RecordActionLogMessage
import APIs.UserService.LoginUserMessage
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
import APIs.UserService.LoginUserMessage
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class TransferToFriendMessagePlanner(
  sessionToken: String,
  friendID: String,
  amount: Double,
  override val planContext: PlanContext
) extends Planner[String] {

  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[String] = {
    for {
      _ <- IO(logger.info(s"[TransferToFriendMessagePlanner] 开始处理好友转账请求，sessionToken: ${sessionToken}, friendID: ${friendID}, amount: ${amount}"))

      // Step 1: 验证 sessionToken
      userID <- validateSessionToken(sessionToken)

      // Step 2: 检查转账金额是否符合条件
      _ <- validateTransferAmount(amount)
      balance <- QueryBalanceMessage(sessionToken).send
      _ <- validateBalanceSufficiency(balance, amount)

      // Step 3: 执行转账操作
      transferResult <- executeTransfer(userID, friendID, amount)

      // Step 4: 返回操作结果
      result <- IO(s"[TransferToFriendMessagePlanner] 转账成功: ${transferResult}")
      _ <- IO(logger.info(result))
    } yield result
  }

  private def validateSessionToken(sessionToken: String)(using PlanContext): IO[String] = {
    for {
      _ <- IO(logger.info("[Step 1] 验证sessionToken"))

      // 提取sessionToken中的userID
      _ <- IO(logger.info("[Step 1.1] 解码sessionToken获取userID"))
      userID <- IO(decodeField[String](sessionToken.asJson, "userID")) // 假设sessionToken可解码获取userID

      // 调用验证接口LoginUserMessage确保sessionToken有效
      _ <- IO(logger.info("[Step 1.2] 调用LoginUserMessage验证sessionToken"))
      _ <- LoginUserMessage(userID, sessionToken).send
    } yield userID
  }

  private def validateTransferAmount(amount: Double)(using PlanContext): IO[Unit] = {
    if (amount <= 0) {
      IO(logger.error("[validateTransferAmount] 转账金额必须大于0")) >>
        IO.raiseError(new IllegalArgumentException("[validateTransferAmount] 转账金额必须大于0"))
    } else {
      IO.unit
    }
  }

  private def validateBalanceSufficiency(balance: Double, amount: Double)(using PlanContext): IO[Unit] = {
    if (balance < amount) {
      val errorMessage = s"[validateBalanceSufficiency] 用户余额不足, 当前余额: ${balance}, 转账金额: ${amount}"
      IO(logger.error(errorMessage)) >>
        IO.raiseError(new IllegalArgumentException(errorMessage))
    } else {
      IO.unit
    }
  }

  private def executeTransfer(userID: String, friendID: String, amount: Double)(using PlanContext): IO[String] = {
    for {
      // 执行账户余额更新和转账操作
      _ <- IO(logger.info("[Step 3.1] 执行账户余额更新和转账操作"))
      transferResult <- transferAmount(userID, friendID, amount)

      // 记录转账操作日志
      _ <- logTransferAction(userID, friendID, amount)
    } yield transferResult
  }

  private def logTransferAction(userID: String, friendID: String, amount: Double)(using PlanContext): IO[Unit] = {
    for {
      _ <- IO(logger.info(s"[Step 3.2] 记录转账操作日志，发送方: ${userID}，接收方: ${friendID}，金额: ${amount}"))
      _ <- RecordActionLogMessage(
        action = "Transfer",
        targetID = friendID,
        performerID = userID
      ).send
    } yield ()
  }
}
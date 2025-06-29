package Utils

//process plan import 预留标志位，不要删除
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.joda.time.DateTime
import Common.DBAPI._
import Common.ServiceUtils.schemaName
import org.slf4j.LoggerFactory
import Common.API.{PlanContext}
import Common.Object.SqlParameter
import cats.effect.IO
import cats.implicits.*
import Common.API.{PlanContext, Planner}
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}
import Objects.PaymentService.AmountChangeLog
import Objects.PaymentService.ChangeType
import Common.Object.{ParameterList, SqlParameter}
import cats.implicits._
import Objects.PaymentService.{AmountChangeLog, ChangeType}
import Common.API.PlanContext
import Common.Object.ParameterList

case object RedPacketProcess {
  private val logger = LoggerFactory.getLogger(getClass)
  //process plan code 预留标志位，不要删除
  
  
  def claimRedPacket(redPacketID: String, receiverID: String)(using PlanContext): IO[Double] = {
    for {
      _ <- IO(logger.info(s"[Step 1] 开始验证红包ID (${redPacketID}) 和领取者ID (${receiverID}) 的有效性"))
  
      // 检查红包是否存在并获取相关信息
      redPacketOptional <- readDBJsonOptional(
        s"""
        SELECT red_packet_id, total_amount, participant_count, expired_at, status
        FROM ${schemaName}.red_packet_table
        WHERE red_packet_id = ?;
        """,
        List(SqlParameter("String", redPacketID))
      )
      redPacket <- IO(redPacketOptional.getOrElse(
        throw new IllegalStateException(s"红包ID [${redPacketID}] 不存在")
      ))
      _ <- IO(logger.info(s"[Step 1.1] 红包信息: ${redPacket}"))
  
      // 验证领取者ID是否有领取权限，以及红包是否未过期且未领取完毕
      currentTime <- IO(DateTime.now())
      expiredAt <- IO(decodeField[DateTime](redPacket, "expired_at"))
      status <- IO(decodeField[String](redPacket, "status"))
      participantCount <- IO(decodeField[Int](redPacket, "participant_count"))
      _ <- {
        if (currentTime.isAfter(expiredAt)) 
          IO.raiseError(new IllegalStateException(s"红包ID [${redPacketID}] 已过期"))
        else if (status != "active")
          IO.raiseError(new IllegalStateException(s"红包ID [${redPacketID}] 状态为 [${status}]，无法领取"))
        else IO.unit
      }
      claimExists <- readDBBoolean(
        s"""
        SELECT EXISTS (
          SELECT 1 
          FROM ${schemaName}.red_packet_claim_table 
          WHERE red_packet_id = ? AND claimer_id = ?
        );
        """,
        List(SqlParameter("String", redPacketID), SqlParameter("String", receiverID))
      )
      _ <- {
        if (claimExists)
          IO.raiseError(new IllegalStateException(s"领取者ID [${receiverID}] 已经领取过红包 [${redPacketID}]"))
        else IO.unit
      }
  
      _ <- IO(logger.info(s"[Step 2] 开始分配领取金额，并更新红包领取记录"))
  
      // 计算随机领取金额
      totalAmount <- IO(decodeField[Double](redPacket, "total_amount"))
      claimedAmounts <- readDBRows(
        s"""
        SELECT amount FROM ${schemaName}.red_packet_claim_table
        WHERE red_packet_id = ?;
        """,
        List(SqlParameter("String", redPacketID))
      )
      totalClaimed <- IO(claimedAmounts.map(j => decodeField[Double](j, "amount")).sum)
      remainingAmount <- IO(totalAmount - totalClaimed)
      _ <- {
        if (remainingAmount <= 0)
          IO.raiseError(new IllegalStateException(s"红包 [${redPacketID}] 已经领取完毕"))
        else IO.unit
      }
      maxClaimAmount <- IO(math.min(remainingAmount, totalAmount / participantCount))
      claimedAmount <- IO(math.random() * maxClaimAmount)
      
      // 更新红包领取记录
      _ <- writeDB(
        s"""
        INSERT INTO ${schemaName}.red_packet_claim_table (red_packet_id, claimer_id, amount, claimed_at)
        VALUES (?, ?, ?, ?);
        """,
        List(
          SqlParameter("String", redPacketID),
          SqlParameter("String", receiverID),
          SqlParameter("Double", claimedAmount.toString),
          SqlParameter("DateTime", currentTime.getMillis.toString)
        )
      )
      
      // 更新红包剩余金额和领取人数
      updatedStatus <- IO(if (remainingAmount - claimedAmount <= 0) "claimed" else status)
      _ <- writeDB(
        s"""
        UPDATE ${schemaName}.red_packet_table
        SET total_amount = total_amount - ?, participant_count = participant_count - 1, status = ?
        WHERE red_packet_id = ?;
        """,
        List(
          SqlParameter("Double", claimedAmount.toString),
          SqlParameter("String", updatedStatus),
          SqlParameter("String", redPacketID)
        )
      )
  
      _ <- IO(logger.info(s"[Step 3] 更新领取者账户余额"))
  
      // 更新领取者余额
      _ <- writeDB(
        s"""
        UPDATE ${schemaName}.user_balance_table
        SET balance = balance + ?, updated_at = ?
        WHERE user_id = ?;
        """,
        List(
          SqlParameter("Double", claimedAmount.toString),
          SqlParameter("DateTime", currentTime.getMillis.toString),
          SqlParameter("String", receiverID)
        )
      )
  
      _ <- IO(logger.info(s"[Step 4] 红包领取完成，领取金额为 ${claimedAmount}"))
    } yield claimedAmount
  }
  
  def createRedPacket(groupID: String, senderID: String, amount: Double, recipients: List[String])(using PlanContext): IO[String] = {
    for {
      // Step 1. Validate input parameters
      _ <- IO(logger.info("[Step 1.1] Validating input parameters"))
      _ <- IO {
        if (groupID.isEmpty || senderID.isEmpty || recipients.isEmpty || recipients.exists(_.isEmpty))
          throw new IllegalArgumentException(s"Invalid parameters: groupID=${groupID}, senderID=${senderID}, recipients=${recipients}")
        if (amount <= 0) throw new IllegalArgumentException(s"Invalid amount: ${amount}")
      }
  
      // Step 2. Deduct sender's account balance
      _ <- IO(logger.info(s"[Step 2.1] Querying sender's balance for senderID=${senderID}"))
      senderBalanceJson <- readDBJson(
        s"SELECT balance, updated_at FROM ${schemaName}.user_balance_table WHERE user_id = ?",
        List(SqlParameter("String", senderID))
      )
      senderBalance <- IO { decodeField[Double](senderBalanceJson, "balance") }
      _ <- IO(logger.info(s"Sender balance is ${senderBalance}"))
      _ <- IO {
        if (senderBalance < amount) throw new IllegalStateException(s"Insufficient balance to send red packet: senderID=${senderID}, balance=${senderBalance}, amount=${amount}")
      }
  
      _ <- IO(logger.info(s"[Step 2.3] Deducting amount from balance. New balance will be ${senderBalance - amount}"))
      _ <- writeDB(
        s"UPDATE ${schemaName}.user_balance_table SET balance = ?, updated_at = ? WHERE user_id = ?",
        List(
          SqlParameter("Double", (senderBalance - amount).toString),
          SqlParameter("DateTime", DateTime.now().getMillis.toString),
          SqlParameter("String", senderID)
        )
      )
  
      // Log balance changes
      _ <- IO(logger.info("[Step 2.4] Logging balance changes"))
      _ <- writeDB(
        s"INSERT INTO ${schemaName}.amount_change_log (log_id, user_id, amount, change_type, timestamp) VALUES (?, ?, ?, ?, ?)",
        List(
          SqlParameter("String", java.util.UUID.randomUUID().toString),
          SqlParameter("String", senderID),
          SqlParameter("Double", (-amount).toString), // Deducted amount is negative
          SqlParameter("String", ChangeType.RedPacketSend.toString),
          SqlParameter("DateTime", DateTime.now().getMillis.toString)
        )
      )
  
      // Step 3. Generate red packet record
      _ <- IO(logger.info("[Step 3.1] Creating red packet record"))
      redPacketID <- IO { java.util.UUID.randomUUID().toString }
      createdAt <- IO { DateTime.now() }
      expiredAt <- IO { createdAt.plusHours(24) } // Example logic: red packet expires in 24 hours
      _ <- writeDB(
        s"INSERT INTO ${schemaName}.red_packet_table (red_packet_id, sender_id, group_id, total_amount, participant_count, created_at, expired_at, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
        List(
          SqlParameter("String", redPacketID),
          SqlParameter("String", senderID),
          SqlParameter("String", groupID),
          SqlParameter("Double", amount.toString),
          SqlParameter("Int", recipients.size.toString),
          SqlParameter("DateTime", createdAt.getMillis.toString),
          SqlParameter("DateTime", expiredAt.getMillis.toString),
          SqlParameter("String", "active")
        )
      )
  
      // Step 3.2. Pre-generate claim records for recipients
      _ <- IO(logger.info("[Step 3.2] Pre-generating claim records for recipients"))
      claimRecords <- IO {
        recipients.map { recipient =>
          ParameterList(List(
            SqlParameter("String", redPacketID),
            SqlParameter("String", recipient),
            SqlParameter("Double", "0"), // Initial amount is 0, not yet claimed
            SqlParameter("DateTime", DateTime.now().getMillis.toString) // No claim is done yet, so recorded at initialization time
          ))
        }
      }
      _ <- writeDBList(
        s"INSERT INTO ${schemaName}.red_packet_claim_table (red_packet_id, claimer_id, amount, claimed_at) VALUES (?, ?, ?, ?)",
        claimRecords
      )
  
      _ <- IO(logger.info(s"[Step 4] Successfully created red packet with ID=${redPacketID}"))
    } yield redPacketID
  }
}

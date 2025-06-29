package Utils

//process plan import 预留标志位，不要删除
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.joda.time.DateTime
import Common.DBAPI._
import Common.ServiceUtils.schemaName
import org.slf4j.LoggerFactory
import Common.API.PlanContext
import Common.Object.{SqlParameter, ParameterList}
import cats.effect.IO
import Common.API.{PlanContext, Planner}
import Common.Object.SqlParameter
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case object TransactionProcess {
  private val logger = LoggerFactory.getLogger(getClass)
  //process plan code 预留标志位，不要删除
  
  
  def transferAmount(senderID: String, recipientID: String, amount: Double)(using PlanContext): IO[String] = {
  // val logger = LoggerFactory.getLogger("TransferAmount")  // 同文后端处理: logger 统一
  
    logger.info(s"[TransferAmount] 开始处理从 ${senderID} 至 ${recipientID} 的转账，金额：${amount}")
  
    for {
      // Step 1: 验证输入参数的有效性
      _ <- IO {
        require(senderID.nonEmpty, "[TransferAmount] senderID 不能为空")
        require(recipientID.nonEmpty, "[TransferAmount] recipientID 不能为空")
        require(amount > 0, "[TransferAmount] 转账金额必须大于 0")
      }
  
      // Step 2: 查询 senderID 的余额
      senderBalanceJson <- readDBJson(
        s"SELECT balance FROM ${schemaName}.user_balance_table WHERE user_id = ?;",
        List(SqlParameter("String", senderID))
      )
      senderBalance <- IO(decodeField[Double](senderBalanceJson, "balance"))
      _ <- IO(logger.info(s"[TransferAmount] 发送方 ${senderID} 当前余额：${senderBalance}"))
  
      // Step 2.2: 检查余额是否足够
      _ <- if (senderBalance < amount) {
        IO.raiseError(new IllegalArgumentException(s"[TransferAmount] 余额不足，发送方余额：${senderBalance}，转账金额：${amount}"))
      } else {
        IO.unit
      }
  
      // Step 3: 执行转账操作
      // Step 3.1: 减少发送方的余额
      _ <- writeDB(
        s"UPDATE ${schemaName}.user_balance_table SET balance = balance - ?, updated_at = ? WHERE user_id = ?;",
        List(
          SqlParameter("Double", amount.asJson.noSpaces),
          SqlParameter("DateTime", DateTime.now.getMillis.toString),
          SqlParameter("String", senderID)
        )
      )
      _ <- IO(logger.info(s"[TransferAmount] 已扣除发送方 ${senderID} 的余额，金额：${amount}"))
  
      // Step 3.2: 增加接收方的余额
      _ <- writeDB(
        s"UPDATE ${schemaName}.user_balance_table SET balance = balance + ?, updated_at = ? WHERE user_id = ?;",
        List(
          SqlParameter("Double", amount.asJson.noSpaces),
          SqlParameter("DateTime", DateTime.now.getMillis.toString),
          SqlParameter("String", recipientID)
        )
      )
      _ <- IO(logger.info(s"[TransferAmount] 已增加接收方 ${recipientID} 的余额，金额：${amount}"))
  
      // Step 3.3: 创建转账记录
      transactionType <- IO("TRANSFER")
      _ <- writeDB(
        s"""
          INSERT INTO ${schemaName}.transaction_log_table
          (transaction_id, sender_id, receiver_id, amount, transaction_type, created_at)
          VALUES (?, ?, ?, ?, ?, ?);
        """.stripMargin,
        List(
          SqlParameter("String", java.util.UUID.randomUUID.toString),
          SqlParameter("String", senderID),
          SqlParameter("String", recipientID),
          SqlParameter("Double", amount.asJson.noSpaces),
          SqlParameter("String", transactionType),
          SqlParameter("DateTime", DateTime.now.getMillis.toString)
        )
      )
      _ <- IO(logger.info(s"[TransferAmount] 转账记录已创建，发送方：${senderID}，接收方：${recipientID}，金额：${amount}"))
  
      // Step 4: 返回操作结果
      result <- IO(s"[TransferAmount] 转账成功，发送方：${senderID}，接收方：${recipientID}，金额：${amount}")
      _ <- IO(logger.info(result))
    } yield result
  }
}

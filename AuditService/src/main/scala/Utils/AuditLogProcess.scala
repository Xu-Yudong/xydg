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
import java.util.UUID
import cats.implicits._
import cats.implicits.*
import Common.API.{PlanContext, Planner}
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}
import Objects.PaymentService.ChangeType
import Common.API.PlanContext
import Common.Object.{SqlParameter, ParameterList}
import Common.DBAPI.{writeDB}

case object AuditLogProcess {
  private val logger = LoggerFactory.getLogger(getClass)
  //process plan code 预留标志位，不要删除
  
  
  def recordActionLog(action: String, targetID: String, performerID: String)(using PlanContext): IO[String] = {
  // val logger = LoggerFactory.getLogger(getClass)  // 同文后端处理: logger 统一
  
    for {
      // Step 1: 参数校验
      _ <- IO {
        if (action.isEmpty) throw new IllegalArgumentException("Action 不能为空")
        if (targetID.isEmpty) throw new IllegalArgumentException("TargetID 不能为空")
        if (performerID.isEmpty) throw new IllegalArgumentException("PerformerID 不能为空")
        logger.info(s"参数校验通过: action=${action}, targetID=${targetID}, performerID=${performerID}")
      }
  
      // Step 2: 构造插入数据
      logID <- IO(UUID.randomUUID().toString) // 生成唯一的日志ID
      timestamp <- IO(DateTime.now())
      _ <- IO(logger.info(s"构造日志数据: logID=${logID}, timestamp=${timestamp}"))
  
      // Step 3: 插入日志记录到数据库
      sql <- IO {
        s"INSERT INTO ${schemaName}.action_log_table (log_id, action, target_id, performer_id, timestamp) VALUES (?, ?, ?, ?, ?)"
      }
      params <- IO {
        List(
          SqlParameter("String", logID),
          SqlParameter("String", action),
          SqlParameter("String", targetID),
          SqlParameter("String", performerID),
          SqlParameter("DateTime", timestamp.getMillis.toString)
        )
      }
      _ <- IO(logger.info("开始插入日志记录到数据库"))
      writeResult <- writeDB(sql, params)
      _ <- IO(logger.info(s"日志插入成功, 返回值: ${writeResult}"))
  
      // Step 4: 返回成功消息
      resultMessage <- IO(s"操作日志记录成功")
      _ <- IO(logger.info(resultMessage))
    } yield resultMessage
  }
  
  def recordAmountChange(userID: String, amount: Double, changeType: ChangeType)(using PlanContext): IO[String] = {
    for {
      // Step 1: 验证输入参数的合法性
      _ <- IO(logger.info(s"验证输入参数 - userID: ${userID}, amount: ${amount}, changeType: ${changeType}"))
      _ <- if (userID.isEmpty) 
        IO.raiseError(new IllegalArgumentException("userID不能为空")) 
      else 
        IO.unit
      _ <- if (amount == 0.0) 
        IO.raiseError(new IllegalArgumentException("amount不能为0")) 
      else 
        IO.unit
      _ <- IO(logger.info(s"输入参数验证通过 - userID: ${userID}, amount: ${amount}, changeType: ${changeType}"))
  
      // Step 2: 构造变动记录
      logID <- IO {
        val id = java.util.UUID.randomUUID().toString
        logger.info(s"生成唯一logID: ${id}")
        id
      }
      timestamp <- IO {
        val now = DateTime.now()
        logger.info(s"当前时间戳生成: ${now}")
        now
      }
  
      // Step 3: 插入日志表
      _ <- {
        val sql =
          s"""
             INSERT INTO ${schemaName}.amount_change_log_table
             (log_id, user_id, amount, change_type, timestamp)
             VALUES (?, ?, ?, ?, ?)
           """
        val params = List(
          SqlParameter("String", logID),
          SqlParameter("String", userID),
          SqlParameter("Double", amount.toString),
          SqlParameter("String", changeType.toString),
          SqlParameter("DateTime", timestamp.getMillis.toString)
        )
        IO(logger.info(s"准备插入变动记录到amount_change_log_table, SQL: ${sql}, 参数: ${params}")) >>
          writeDB(sql, params)
      }
  
      // Step 4: 返回结果
      result <- IO {
        val message = s"记录金额变动成功，logID: ${logID}"
        logger.info(message)
        message
      }
    } yield result
  }
  
  
  def recordRedPacketLog(
      redPacketID: String,
      userID: String,
      actionType: String,
      amount: Double
  )(using PlanContext): IO[String] = {
    for {
      // Step 1: Validate input parameters
      _ <- IO {
        if (redPacketID.isEmpty) throw new IllegalArgumentException("redPacketID cannot be empty.")
        if (userID.isEmpty) throw new IllegalArgumentException("userID cannot be empty.")
        if (actionType.isEmpty) throw new IllegalArgumentException("actionType cannot be empty.")
        if (amount <= 0) throw new IllegalArgumentException("amount must be a positive number.")
      }
  
      _ <- IO(logger.info(s"Input validation passed for redPacketID: ${redPacketID}, userID: ${userID}, actionType: ${actionType}, amount: ${amount}"))
      
      // Step 2: Prepare data for insertion
      currentTimestamp <- IO(DateTime.now())
      logID <- IO(java.util.UUID.randomUUID().toString) // Generate a unique log ID
      _ <- IO(logger.info(s"Generated logID: ${logID}, and timestamp: ${currentTimestamp}"))
  
      parameters <- IO {
        List(
          SqlParameter("String", logID),
          SqlParameter("String", redPacketID),
          SqlParameter("String", userID),
          SqlParameter("String", actionType),
          SqlParameter("Double", amount.toString),
          SqlParameter("DateTime", currentTimestamp.getMillis.toString)
        )
      }
      _ <- IO(logger.info(s"Prepared SQL parameters: ${parameters}"))
      
      // Step 3: Insert log into the database
      insertSQL <- IO {
        s"""
        INSERT INTO ${schemaName}.red_packet_log_table (log_id, red_packet_id, user_id, action_type, amount, timestamp)
        VALUES (?, ?, ?, ?, ?, ?);
        """
      }
      _ <- IO(logger.info(s"Executing SQL: ${insertSQL}"))
      _ <- writeDB(insertSQL, parameters)
  
      // Step 4: Return success message
      result <- IO(s"Red packet log recorded successfully with logID: ${logID}")
      _ <- IO(logger.info(result))
    } yield result
  }
  
  
  def recordMessageAction(messageID: String, actionType: String, performerID: String)(using PlanContext): IO[String] = {
  // val logger = LoggerFactory.getLogger(getClass) // Logger定义  // 同文后端处理: logger 统一
  
    // Step 1: Validate input parameters
    for {
      _ <- IO(logger.info(s"开始验证输入参数：messageID=${messageID}, actionType=${actionType}, performerID=${performerID}"))
      _ <- if (messageID.isEmpty)
        IO.raiseError(new IllegalArgumentException("messageID 不能为空"))
      else IO.unit
      _ <- if (!List("send", "retract").contains(actionType))
        IO.raiseError(new IllegalArgumentException("actionType 无效，只能为 'send' 或 'retract'"))
      else IO.unit
      _ <- if (performerID.isEmpty)
        IO.raiseError(new IllegalArgumentException("performerID 不能为空"))
      else IO.unit
  
      // Step 2: Generate log data
      timestamp <- IO(DateTime.now)
      _ <- IO(logger.info(s"生成日志数据：messageID=${messageID}, actionType=${actionType}, performerID=${performerID}, timestamp=${timestamp}"))
  
      // Step 3: Insert log entry into DB
      insertSQL <- IO {
        s"""
        INSERT INTO ${schemaName}.message_action_log_table (message_id, action_type, performer_id, timestamp)
        VALUES (?, ?, ?, ?)
        """.stripMargin
      }
      parameters <- IO {
        List(
          SqlParameter("String", messageID),
          SqlParameter("String", actionType),
          SqlParameter("String", performerID),
          SqlParameter("DateTime", timestamp.getMillis.toString)
        )
      }
      _ <- IO(logger.info(s"准备执行数据库写入操作：SQL=${insertSQL}, parameters=${parameters.map(_.value).mkString(", ")}"))
      _ <- writeDB(insertSQL, parameters)
      _ <- IO(logger.info(s"数据库写入成功: messageID=${messageID}, actionType=${actionType}, performerID=${performerID}, timestamp=${timestamp}"))
    } yield "记录成功"
  }
}

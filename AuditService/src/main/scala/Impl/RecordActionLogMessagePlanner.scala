package Impl


import Common.API.{PlanContext, Planner}
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import Utils.AuditLogProcess.recordActionLog
import org.slf4j.LoggerFactory
import io.circe.generic.auto._
import org.joda.time.DateTime
import cats.effect.IO
import cats.implicits._
import java.util.regex.Pattern
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
import Utils.AuditLogProcess.recordActionLog
import io.circe._
import io.circe.syntax._
import cats.implicits.*
import Common.DBAPI._
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class RecordActionLogMessagePlanner(
    action: String,
    targetID: String,
    performerID: String,
    override val planContext: PlanContext
) extends Planner[String] {

  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  // Helper function to validate if a string matches the required ID format
  private def isValidIDFormat(input: String): Boolean = {
    val idPattern = Pattern.compile("^[a-zA-Z0-9_]+$")
    idPattern.matcher(input).matches()
  }

  // Step 1.1: Validate Inputs
  private def validateInputs()(using PlanContext): IO[Unit] = IO {
    logger.info("[Step 1.1] 开始验证输入参数")

    if (action.isEmpty || action.trim.isEmpty) 
      throw new IllegalArgumentException("action 参数不能为空或空白")
    if (targetID.isEmpty || targetID.trim.isEmpty || !isValidIDFormat(targetID)) 
      throw new IllegalArgumentException("targetID 参数不能为空、空白，且只能包含字母、数字或下划线")
    if (performerID.isEmpty || performerID.trim.isEmpty || !isValidIDFormat(performerID)) 
      throw new IllegalArgumentException("performerID 参数不能为空、空白，且只能包含字母、数字或下划线")

    logger.info(s"[Step 1.2] 参数校验通过: action=${action}, targetID=${targetID}, performerID=${performerID}")
  }

  // Step 2: Call RecordActionLog
  private def recordLog()(using PlanContext): IO[String] = {
    logger.info("[Step 2] 开始调用日志记录方法 `recordActionLog`")
    recordActionLog(action, targetID, performerID)
  }

  // Step 3.1: Return the operation result
  override def plan(using planContext: PlanContext): IO[String] = {
    for {
      // Step 1: Validate the inputs
      _ <- validateInputs()

      // Step 2: Record the action log
      logResult <- recordLog()
      _ <- IO(logger.info(s"[Step 2.1] 日志记录结果: ${logResult}"))

      // Step 3.1: Construct and return the result
      result <- IO("日志记录成功")
      _ <- IO(logger.info(s"[Step 3] 返回结果: ${result}"))
    } yield result
  }
}
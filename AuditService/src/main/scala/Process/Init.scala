
package Process

import Common.API.{API, PlanContext, TraceID}
import Common.DBAPI.{initSchema, writeDB}
import Common.ServiceUtils.schemaName
import Global.ServerConfig
import cats.effect.IO
import io.circe.generic.auto.*
import java.util.UUID
import Global.DBConfig
import Process.ProcessUtils.server2DB
import Global.GlobalVariables

object Init {
  def init(config: ServerConfig): IO[Unit] = {
    given PlanContext = PlanContext(traceID = TraceID(UUID.randomUUID().toString), 0)
    given DBConfig = server2DB(config)

    val program: IO[Unit] = for {
      _ <- IO(GlobalVariables.isTest=config.isTest)
      _ <- API.init(config.maximumClientConnection)
      _ <- Common.DBAPI.SwitchDataSourceMessage(projectName = Global.ServiceCenter.projectName).send
      _ <- initSchema(schemaName)
            /** 审计日志表，记录操作日志信息
       * log_id: 日志的唯一ID
       * action: 操作的类型
       * target_id: 操作针对的目标ID
       * performer_id: 操作执行者的ID
       * timestamp: 操作的时间戳
       */
      _ <- writeDB(
        s"""
        CREATE TABLE IF NOT EXISTS "${schemaName}"."action_log_table" (
            log_id VARCHAR NOT NULL PRIMARY KEY,
            action TEXT NOT NULL,
            target_id TEXT NOT NULL,
            performer_id TEXT NOT NULL,
            timestamp TIMESTAMP NOT NULL
        );
         
        """,
        List()
      )
      /** 用于记录消息操作日志，例如发送和撤回操作。
       * log_id: 日志的唯一标识，主键，自增
       * message_id: 关联的消息的唯一标识
       * action_type: 操作类型，例如发送、撤回
       * performer_id: 执行操作的用户的唯一标识
       * timestamp: 记录操作发生的时间戳
       */
      _ <- writeDB(
        s"""
        CREATE TABLE IF NOT EXISTS "${schemaName}"."message_action_log_table" (
            log_id VARCHAR NOT NULL PRIMARY KEY,
            message_id TEXT NOT NULL,
            action_type TEXT NOT NULL,
            performer_id TEXT NOT NULL,
            timestamp TIMESTAMP NOT NULL
        );
         
        """,
        List()
      )
      /** 记录金额变动的日志，包括红包发送、领取、交易等。
       * log_id: 日志的唯一ID，主键
       * user_id: 用户的唯一标识
       * amount: 金额变动的数值
       * change_type: 变动类型，如发放红包、领取红包、转账等
       * timestamp: 记录的时间戳
       */
      _ <- writeDB(
        s"""
        CREATE TABLE IF NOT EXISTS "${schemaName}"."amount_change_log_table" (
            log_id VARCHAR NOT NULL PRIMARY KEY,
            user_id TEXT NOT NULL,
            amount DOUBLE PRECISION NOT NULL,
            change_type TEXT NOT NULL,
            timestamp TIMESTAMP NOT NULL
        );
         
        """,
        List()
      )
      /** 红包日志表，记录红包的发送、领取及过期情况
       * log_id: 日志的唯一ID，主键
       * red_packet_id: 红包的唯一ID
       * user_id: 用户的唯一ID
       * action_type: 操作类型，例如“发送”、“领取”、“过期”
       * amount: 红包的金额
       * timestamp: 操作发生的时间
       */
      _ <- writeDB(
        s"""
        CREATE TABLE IF NOT EXISTS "${schemaName}"."red_packet_log_table" (
            log_id VARCHAR NOT NULL PRIMARY KEY,
            red_packet_id TEXT NOT NULL,
            user_id TEXT NOT NULL,
            action_type TEXT NOT NULL,
            amount DOUBLE PRECISION NOT NULL,
            timestamp TIMESTAMP NOT NULL
        );
         
        """,
        List()
      )
    } yield ()

    program.handleErrorWith(err => IO {
      println("[Error] Process.Init.init 失败, 请检查 db-manager 是否启动及端口问题")
      err.printStackTrace()
    })
  }
}
    
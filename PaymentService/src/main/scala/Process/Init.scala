
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
            /** 红包表，包含红包的基本信息
       * red_packet_id: 红包的唯一ID，主键且自增
       * sender_id: 发送者的用户ID
       * group_id: 群组的ID，红包所属群组
       * total_amount: 红包的总金额
       * participant_count: 参与领取红包的成员数量
       * created_at: 红包的创建时间
       * expired_at: 红包的过期时间
       * status: 红包的状态，例如：active, expired, claimed
       */
      _ <- writeDB(
        s"""
        CREATE TABLE IF NOT EXISTS "${schemaName}"."red_packet_table" (
            red_packet_id VARCHAR NOT NULL PRIMARY KEY,
            sender_id TEXT NOT NULL,
            group_id TEXT NOT NULL,
            total_amount DOUBLE PRECISION NOT NULL,
            participant_count INT NOT NULL,
            created_at TIMESTAMP NOT NULL,
            expired_at TIMESTAMP NOT NULL,
            status TEXT NOT NULL
        );
         
        """,
        List()
      )
      /** 用户余额表，记录用户的当前余额信息
       * user_id: 用户的唯一ID
       * balance: 用户的余额
       * updated_at: 最后更新时间
       */
      _ <- writeDB(
        s"""
        CREATE TABLE IF NOT EXISTS "${schemaName}"."user_balance_table" (
            user_id VARCHAR NOT NULL PRIMARY KEY,
            balance DOUBLE PRECISION NOT NULL DEFAULT 0.0,
            updated_at TIMESTAMP NOT NULL
        );
         
        """,
        List()
      )
      /** 交易日志表，记录资金转账和购买等交易
       * transaction_id: 交易的唯一ID，主键，自增
       * sender_id: 发起交易的用户ID
       * receiver_id: 接收交易的用户ID
       * amount: 交易金额
       * transaction_type: 交易类型，例如转账或购买
       * created_at: 交易创建时间
       */
      _ <- writeDB(
        s"""
        CREATE TABLE IF NOT EXISTS "${schemaName}"."transaction_log_table" (
            transaction_id VARCHAR NOT NULL PRIMARY KEY,
            sender_id TEXT NOT NULL,
            receiver_id TEXT NOT NULL,
            amount DOUBLE PRECISION NOT NULL,
            transaction_type TEXT NOT NULL,
            created_at TIMESTAMP NOT NULL
        );
         
        """,
        List()
      )
      /** 表示红包领取记录的表格，保存领取人及领取信息
       * red_packet_id: 表示红包的唯一ID，与RedPacketTable关联
       * claimer_id: 表示领取红包的用户ID
       * amount: 表示领取的红包金额
       * claimed_at: 表示领取时间，存储时间戳
       */
      _ <- writeDB(
        s"""
        CREATE TABLE IF NOT EXISTS "${schemaName}"."red_packet_claim_table" (
            red_packet_id VARCHAR NOT NULL PRIMARY KEY,
            claimer_id TEXT NOT NULL,
            amount DOUBLE PRECISION NOT NULL,
            claimed_at TIMESTAMP NOT NULL
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
    
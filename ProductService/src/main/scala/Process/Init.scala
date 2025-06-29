
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
            /** 产品交易表，记录交易相关信息
       * transaction_id: 交易的唯一ID
       * product_id: 产品的唯一ID
       * buyer_id: 买家的唯一ID
       * seller_id: 卖家的唯一ID
       * amount: 交易金额
       * created_at: 交易创建时间戳
       */
      _ <- writeDB(
        s"""
        CREATE TABLE IF NOT EXISTS "${schemaName}"."product_transaction_table" (
            transaction_id VARCHAR NOT NULL PRIMARY KEY,
            product_id TEXT NOT NULL,
            buyer_id TEXT NOT NULL,
            seller_id TEXT NOT NULL,
            amount DOUBLE PRECISION NOT NULL,
            created_at TIMESTAMP NOT NULL
        );
         
        """,
        List()
      )
      /** 商品审核日志表，记录审核的操作日志信息
       * audit_log_id: 审核日志的唯一ID
       * product_id: 审核商品的唯一ID
       * reviewer_id: 审核人的唯一ID
       * action: 审核操作类型，例如：通过、拒绝、撤回
       * reason: 审核操作的理由
       * timestamp: 日志记录的时间戳
       */
      _ <- writeDB(
        s"""
        CREATE TABLE IF NOT EXISTS "${schemaName}"."product_audit_log_table" (
            audit_log_id VARCHAR NOT NULL PRIMARY KEY,
            product_id TEXT NOT NULL,
            reviewer_id TEXT NOT NULL,
            action TEXT NOT NULL,
            reason TEXT NOT NULL,
            timestamp TIMESTAMP NOT NULL
        );
         
        """,
        List()
      )
      /** 商品表，包含商品的基本信息
       * product_id: 商品的唯一ID，主键
       * name: 商品名称
       * description: 商品描述
       * price: 商品价格
       * status: 商品状态，例如：审核中、上架中、被拒绝、已下架
       * seller_id: 商品卖家的唯一ID
       * created_at: 商品创建时间
       * updated_at: 商品更新时间
       */
      _ <- writeDB(
        s"""
        CREATE TABLE IF NOT EXISTS "${schemaName}"."product_table" (
            product_id VARCHAR NOT NULL PRIMARY KEY,
            name TEXT NOT NULL,
            description TEXT,
            price DOUBLE PRECISION NOT NULL,
            status TEXT NOT NULL,
            seller_id TEXT NOT NULL,
            created_at TIMESTAMP,
            updated_at TIMESTAMP
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
    
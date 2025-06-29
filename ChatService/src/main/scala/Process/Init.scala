
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
            /** 群聊表，包含群聊的基本信息
       * group_id: 群聊的唯一ID
       * group_name: 群聊名称
       * creator_id: 群聊创建者的用户ID
       * created_at: 群聊创建时间
       */
      _ <- writeDB(
        s"""
        CREATE TABLE IF NOT EXISTS "${schemaName}"."group_chat_table" (
            group_id VARCHAR NOT NULL PRIMARY KEY,
            group_name TEXT NOT NULL,
            creator_id TEXT NOT NULL,
            created_at TIMESTAMP NOT NULL
        );
         
        """,
        List()
      )
      /** 静音记录表，记录用户在群组中的禁言信息
       * log_id: 日志的唯一ID，主键
       * group_id: 群组的唯一ID
       * user_id: 被禁言用户的ID
       * muted_by_user_id: 执行禁言操作的用户ID
       * mute_duration: 禁言时长，单位为分钟
       * created_at: 静音记录创建的时间
       * expired_at: 禁言的到期时间
       */
      _ <- writeDB(
        s"""
        CREATE TABLE IF NOT EXISTS "${schemaName}"."mute_log_table" (
            log_id VARCHAR NOT NULL PRIMARY KEY,
            group_id TEXT NOT NULL,
            user_id TEXT NOT NULL,
            muted_by_user_id TEXT NOT NULL,
            mute_duration INT NOT NULL DEFAULT 0,
            created_at TIMESTAMP NOT NULL,
            expired_at TIMESTAMP NOT NULL
        );
         
        """,
        List()
      )
      /** 聊天消息表，包含用户之间的私信记录
       * message_id: 消息的唯一ID，主键
       * sender_id: 发送消息的用户ID
       * receiver_id: 接收消息的用户ID
       * content: 消息内容
       * timestamp: 消息发送的时间戳
       * status: 消息状态，例如未读、已送达、已撤回
       */
      _ <- writeDB(
        s"""
        CREATE TABLE IF NOT EXISTS "${schemaName}"."chat_message_table" (
            message_id VARCHAR NOT NULL PRIMARY KEY,
            sender_id TEXT NOT NULL,
            receiver_id TEXT NOT NULL,
            content TEXT NOT NULL,
            timestamp TIMESTAMP NOT NULL,
            status TEXT NOT NULL DEFAULT 'unread'
        );
         
        """,
        List()
      )
      /** 群成员表，包含群组成员的角色及相关信息
       * group_id: 群组的唯一ID
       * user_id: 用户的唯一ID
       * role: 群组成员的角色，例如群主、管理员或普通成员
       * mute_until: 成员的禁言截至时间
       */
      _ <- writeDB(
        s"""
        CREATE TABLE IF NOT EXISTS "${schemaName}"."group_member_table" (
            group_id VARCHAR NOT NULL PRIMARY KEY,
            user_id TEXT NOT NULL,
            role TEXT NOT NULL DEFAULT 'Member',
            mute_until TIMESTAMP
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
    
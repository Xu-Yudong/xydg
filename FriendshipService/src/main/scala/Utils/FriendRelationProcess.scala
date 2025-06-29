package Utils

//process plan import 预留标志位，不要删除
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.joda.time.DateTime
import Common.DBAPI._
import Common.ServiceUtils.schemaName
import org.slf4j.LoggerFactory
import Common.API.{PlanContext, Planner}
import Common.Object.SqlParameter
import cats.effect.IO
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}
import Common.API.{PlanContext}
import Common.API.PlanContext

case object FriendRelationProcess {
  private val logger = LoggerFactory.getLogger(getClass)
  //process plan code 预留标志位，不要删除
  
  
  def deleteFriend(userID: String, friendID: String)(using PlanContext): IO[String] = {
    for {
      // Step 1. Validate input parameters
      _ <- IO(logger.info(s"开始检查用户输入参数，userID=${userID}, friendID=${friendID}"))
      _ <- if (userID.isEmpty || friendID.isEmpty) {
             IO.raiseError(new IllegalArgumentException(s"userID或friendID为空，请检查输入参数. userID=${userID}, friendID=${friendID}"))
           } else {
             IO(logger.info(s"输入参数检查通过，将继续进行删除操作"))
           }
  
      // Step 2. Prepare and execute the SQL query to delete friendship record
      sql <- IO(s"DELETE FROM ${schemaName}.friend_relation_table WHERE user_id = ? AND friend_id = ?;")
      parameters <- IO(List(
        SqlParameter("String", userID),
        SqlParameter("String", friendID)
      ))
      _ <- IO(logger.info(s"准备执行删除好友关系的SQL命令: ${sql}, 参数: ${parameters.map(_.value).mkString(", ")}"))
      dbResult <- writeDB(sql, parameters)
      _ <- IO(logger.info(s"数据库操作完成，结果: ${dbResult}"))
  
      // Step 3. Return success message
      result <- IO("删除成功")
      _ <- IO(logger.info(s"返回结果: ${result}"))
    } yield result
  }
  
  
  def addFriend(userID: String, friendID: String)(using PlanContext): IO[String] = {
    // Logging initialization
  // val logger = LoggerFactory.getLogger("addFriend")  // 同文后端处理: logger 统一
    
    // Step 1: Validate input parameters
    IO(logger.info(s"[addFriend] 校验输入参数有效性，userID: ${userID}, friendID: ${friendID}")) >>
    (if (userID.isEmpty || friendID.isEmpty) {
      IO.raiseError(new IllegalArgumentException("userID或friendID不能为空"))
    } else {
      // Step 2: Query to check if friendship already exists
      val checkFriendshipSql =
        s"""
        SELECT * 
        FROM ${schemaName}.friend_relation_table
        WHERE user_id = ? AND friend_id = ?
        """
      val checkParameters = List(
        SqlParameter("String", userID),
        SqlParameter("String", friendID)
      )
      
      for {
        _ <- IO(logger.info(s"[addFriend] 校验两者是否已经是好友，SQL: ${checkFriendshipSql}"))
        friendshipOpt <- readDBJsonOptional(checkFriendshipSql, checkParameters)
        
        // Step 3: Handle result of existing friendship check
        result <- friendshipOpt match {
          case Some(_) =>
            IO {
              logger.info(s"[addFriend] 两者已是好友，无需新增记录")
              "两者已是好友"
            }
          case None =>
            // Step 4: Insert friendship record
            val insertFriendshipSql =
              s"""
              INSERT INTO ${schemaName}.friend_relation_table (user_id, friend_id, created_at)
              VALUES (?, ?, ?)
              """
            val insertParameters = List(
              SqlParameter("String", userID),
              SqlParameter("String", friendID),
              SqlParameter("Long", DateTime.now().getMillis.toString)
            )
            for {
              _ <- IO(logger.info(s"[addFriend] 在表中记录好友关系，SQL: ${insertFriendshipSql}"))
              _ <- writeDB(insertFriendshipSql, insertParameters)
              successMessage <- IO {
                logger.info(s"[addFriend] 好友关系添加成功，userID: ${userID}, friendID: ${friendID}")
                "好友添加成功"
              }
            } yield successMessage
        }
      } yield result
    })
  }
}

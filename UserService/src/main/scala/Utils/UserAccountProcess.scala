package Utils

//process plan import 预留标志位，不要删除
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.joda.time.DateTime
import Common.DBAPI._
import Common.ServiceUtils.schemaName
import org.slf4j.LoggerFactory
import Objects.UserService.UserRole
import Common.API.{PlanContext, Planner}
import Common.Object.SqlParameter
import cats.effect.IO
import java.util.UUID
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}
import Common.API.PlanContext
import Common.DBAPI.{readDBJsonOptional, decodeField}
import APIs.ChatService.DeleteGroupMembershipByUserIDMessage
import APIs.FriendshipService.DeleteFriendsByUserIDMessage
import APIs.ChatService.DeleteMessagesByUserIDMessage
import cats.implicits._
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import org.mindrot.jbcrypt.BCrypt

case object UserAccountProcess {
  private val logger = LoggerFactory.getLogger(getClass)
  private val BCRYPT_WORK_FACTOR = 14
  //process plan code 预留标志位，不要删除

  // 密码哈希函数
  private def hashPassword(password: String): String = {
    BCrypt.hashpw(password, BCrypt.gensalt(BCRYPT_WORK_FACTOR))
  }

  // 密码验证函数
  private def checkPassword(candidate: String, hashed: String): Boolean = {
    BCrypt.checkpw(candidate, hashed)
  }

  private def checkUserIDUniqueness(userID: String)(using PlanContext): IO[Boolean] = {
    val sql =
      s"""
         SELECT COUNT(*) = 0
         FROM ${schemaName}.user_table
         WHERE user_id = ?;
       """.stripMargin

    val parameters = List(SqlParameter("String", userID))
    readDBBoolean(sql, parameters)
  }

  private def checkUsernameUniqueness(username: String)(using PlanContext): IO[Boolean] = {
    val sql =
      s"""
           SELECT COUNT(*) = 0
           FROM ${schemaName}.user_table
           WHERE username = ?;
         """.stripMargin

    val parameters = List(SqlParameter("String", username))
    readDBBoolean(sql, parameters)
  }
  
  def createUser(username: String, password: String, role: UserRole)(using PlanContext): IO[String] = {
    // Log the start of the method
    IO(logger.info(s"[createUser] 开始创建用户，用户名: ${username}, 角色: ${role.toString}")) >>
      // Validate inputs
      (
        if (username.isEmpty)
          IO.raiseError(new IllegalArgumentException("[createUser] 用户名为空，无法创建用户"))
        else if (password.isEmpty)
          IO.raiseError(new IllegalArgumentException("[createUser] 密码为空，无法创建用户"))
        else if (!Set(UserRole.Normal, UserRole.Business, UserRole.Reviewer).contains(role))
          IO.raiseError(new IllegalArgumentException(s"[createUser] 角色无效: ${role.toString}"))
        else
          IO.unit
      ).flatMap { _ =>  // Validation block ends here
      for {
        // Generate a UUID for the userID
        _ <- IO(logger.info("[createUser] 生成唯一用户ID"))
        userID <- IO(UUID.randomUUID().toString)

        //Check username uniqueness
        _ <- IO(logger.info(s"[createUser] 检查用户名的唯一性: ${username}"))
        isUnique <- checkUsernameUniqueness(username)
        _ <- if (!isUnique) {
          IO(logger.error(s"[createUser] 使用的用户名 ${username} 已存在，将抛出异常")) >>
            IO.raiseError(new IllegalStateException(s"用户名 ${username} 被占用，请更换用户名"))
        } else IO.unit

        //Check UUID uniqueness
        _ <- IO(logger.info(s"[createUser] 检查用户ID的唯一性: ${userID}"))
        userIDisUnique <- checkUserIDUniqueness(userID)
        _ <- if (!userIDisUnique) {
          IO(logger.error(s"[createUser] 生成的用户ID ${userID} 已存在，将抛出异常")) >>
            IO.raiseError(new IllegalStateException(s"生成的用户ID ${userID} 已存在，请重新尝试注册"))
        } else IO.unit
  
        // Prepare timestamps
        _ <- IO(logger.info("[createUser] 准备时间戳"))
        currentTime <- IO(DateTime.now())
  
        // Construct SQL for inserting the new user
        insertSQL <- IO {
          s"""
          INSERT INTO ${schemaName}.user_table (user_id, username, password, role, balance, created_at, updated_at)
          VALUES (?, ?, ?, ?, ?, ?, ?);
          """.stripMargin
        }
        _ <- IO(logger.debug(s"[createUser] 插入SQL语句: ${insertSQL}"))
  
        // Prepare SQL parameters
        params <- IO {
          List(
            SqlParameter("String", userID),
            SqlParameter("String", username),
            SqlParameter("String", hashPassword(password)),
            SqlParameter("String", role.toString),
            SqlParameter("Double", "0.0"), // Default balance
            SqlParameter("DateTime", currentTime.getMillis.toString), // created_at
            SqlParameter("DateTime", currentTime.getMillis.toString)  // updated_at
          )
        }
  
        // Insert the user into the database
        _ <- IO(logger.info("[createUser] 将新用户写入数据库"))
        writeResult <- writeDB(insertSQL, params)
  
        // Log success and return the userID
        _ <- IO(logger.info(s"[createUser] 成功创建用户，用户ID: ${userID}, 写入结果: ${writeResult}"))
      } yield userID
    }
  }
  
  
  def validateUserCredentials(username: String, password: String)(using PlanContext): IO[String] = {
    logger.info(s"[validateUserCredentials] 开始验证用户凭据，用户名: ${username}")
  
    val sql =
      s"""
        SELECT user_id, password
        FROM ${schemaName}.user_table
        WHERE username = ?;
      """
    val queryParams = List(SqlParameter("String", username))
  
    for {
      _ <- IO(logger.info(s"[validateUserCredentials] 构造 SQL 查询: ${sql}"))
      resultOption <- readDBJsonOptional(sql, queryParams)
      userID <- resultOption match {
        case Some(json) =>
          val storedHash = decodeField[String](json, "password")
          if (checkPassword(password, storedHash)) {
            val userID = decodeField[String](json, "user_id")
            IO(logger.info(s"[validateUserCredentials] 验证成功, 用户ID: ${userID}")) >> IO.pure(userID)
          } else {
            val errorMessage = s"[validateUserCredentials] 密码验证失败"
            IO(logger.error(errorMessage)) >> IO.raiseError(new Exception(errorMessage))
          }
        case None =>
          val errorMessage = s"[validateUserCredentials] 验证失败, 用户名不存在"
          IO(logger.error(errorMessage)) >>
            IO.raiseError(new Exception(errorMessage))
      }
    } yield userID
  }
  
  def deleteUserByID(userID: String)(using PlanContext): IO[String] = {
  // val logger = LoggerFactory.getLogger("DeleteUserByID")  // 同文后端处理: logger 统一
  
    logger.info(s"开始执行deleteUserByID方法 for userID=${userID}")
  
    if (userID.isEmpty) {
      logger.error("输入参数userID为空，不是有效的用户ID！")
      IO.raiseError(new IllegalArgumentException("Invalid userID provided"))
    } else {
      for {
        // Step 1: 删除用户数据
        _ <- IO(logger.info(s"开始删除用户表user_table中的数据 for userID=${userID}"))
        userDeleteResult <- writeDB(
          s"DELETE FROM ${schemaName}.user_table WHERE user_id = ?",
          List(SqlParameter("String", userID))
        )
        _ <- IO(logger.info(s"从user_table中删除用户数据成功: ${userDeleteResult}"))
  
        _ <- IO(logger.info(s"开始删除用户会话表user_session_table中的数据 for userID=${userID}"))
        sessionDeleteResult <- writeDB(
          s"DELETE FROM ${schemaName}.user_session_table WHERE user_id = ?",
          List(SqlParameter("String", userID))
        )
        _ <- IO(logger.info(s"从user_session_table中删除用户会话数据成功: ${sessionDeleteResult}"))
  
        // Step 2: 清理其他微服务相关数据
        _ <- IO(logger.info(s"调用DeleteFriendsByUserIDMessage接口删除用户好友关系 for userID=${userID}"))
        friendDeletionResponse <- DeleteFriendsByUserIDMessage(userID).send
        _ <- IO(logger.info(s"删除用户好友关系成功: ${friendDeletionResponse}"))
  
        _ <- IO(logger.info(s"调用DeleteMessagesByUserIDMessage接口删除用户聊天消息 for userID=${userID}"))
        messagesDeletionResponse <- DeleteMessagesByUserIDMessage(userID).send
        _ <- IO(logger.info(s"删除用户聊天消息成功: ${messagesDeletionResponse}"))
  
        _ <- IO(logger.info(s"调用DeleteGroupMembershipByUserIDMessage接口删除用户群聊成员信息 for userID=${userID}"))
        groupMembershipDeletionResponse <- DeleteGroupMembershipByUserIDMessage(userID).send
        _ <- IO(logger.info(s"删除用户群聊成员信息成功: ${groupMembershipDeletionResponse}"))
  
        // Step 3: 返回成功消息
        _ <- IO(logger.info(s"全部删除操作完成 for userID=${userID}"))
      } yield s"用户userID=${userID}的所有数据删除成功！"
    }
  }
  
  
  def updateUserBalance(userID: String, newBalance: Double)(using PlanContext): IO[String] = {
    // Step 1: Validate input parameters
    for {
      _ <- IO {
        if (userID == null || userID.trim.isEmpty) {
          throw new IllegalArgumentException("userID不能为空")
        }
        if (newBalance.isNaN || newBalance.isInfinity) {
          throw new IllegalArgumentException("newBalance必须是有效的数值")
        }
      }
  
      // Step 2.1: Log the validation success
  // _ <- IO(LoggerFactory.getLogger(getClass).info(s"输入参数验证通过: userID=${userID}, newBalance=${newBalance}"))  // 同文后端处理: logger 统一
  
      // Step 2.2: Construct the SQL to update the user's balance
      sql <- IO { s"UPDATE ${schemaName}.user_table SET balance = ?, updated_at = ? WHERE user_id = ?" }
  
      // Step 2.3: Prepare parameters
      params <- IO {
        List(
          SqlParameter("Double", newBalance.asJson.noSpaces),
          SqlParameter("DateTime", DateTime.now().getMillis.toString),
          SqlParameter("String", userID)
        )
      }
  
      // Step 2.4: Write to the database
      result <- writeDB(sql, params)
  // _ <- IO(LoggerFactory.getLogger(getClass).info(s"更新用户余额 SQL 成功执行: userID=${userID}, newBalance=${newBalance}, 结果=${result}"))  // 同文后端处理: logger 统一
  
      // Step 3: Return the result
    } yield "更新成功"
  }
  
  
  def queryUserBalance(userID: String)(using PlanContext): IO[Double] = {
    // Step 0: Logging the start of the function
    IO(logger.info(s"开始查询用户余额信息，userID: ${userID}"))
    
    // Step 1: 检查输入的userID是否有效
    IO {
      if (userID.isEmpty) {
        throw new IllegalArgumentException("userID不能为空")
      }
    } >>
    IO(logger.info(s"userID(${userID})不为空，继续查询")) 
    
    // Step 1.2: 检查userID在UserTable表是否存在
    {
      val userExistSQL = s"SELECT COUNT(*) FROM ${schemaName}.user_table WHERE user_id = ?"
      val userExistParams = List(SqlParameter("String", userID))
      
      readDBInt(userExistSQL, userExistParams).flatMap { userExistCount =>
        if (userExistCount == 0) {
          IO(logger.error(s"userID(${userID})在UserTable表中不存在，抛出异常")) >>
          IO.raiseError(new IllegalStateException(s"userID(${userID})不存在"))
        } else {
          IO(logger.info(s"userID(${userID})存在，将继续查询余额信息"))
        }
      }
    }
    
    // Step 2.1: 查询用户余额
    {
      val balanceSQL = s"SELECT balance FROM ${schemaName}.user_table WHERE user_id = ?"
      val balanceParams = List(SqlParameter("String", userID))
      
      readDBJson(balanceSQL, balanceParams).map { balanceJson =>
        val balance = decodeField[Double](balanceJson, "balance")
        
        // Step 3.1: 日志记录并返回余额
        logger.info(s"查询成功，userID(${userID})的余额为${balance}")
        balance
      }
    }
  }
}

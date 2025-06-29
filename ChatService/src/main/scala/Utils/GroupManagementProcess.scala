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
import cats.implicits._
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}
import Objects.ChatService.GroupRole
import Objects.UserService.UserRole
import Objects.UserService.User
import Common.Object.ParameterList
import Objects.UserService.{UserRole, User}
import Common.API.PlanContext
import Common.DBAPI.{readDBJsonOptional, writeDB, decodeField}
import Common.DBAPI.{writeDB, readDBJsonOptional, decodeField}
import Common.Object._
import Common.DBAPI.{readDBJsonOptional, writeDB}
import Common.Object.{ParameterList, SqlParameter}
import Common.API.{PlanContext}
import Common.Object.{SqlParameter, ParameterList}

case object GroupManagementProcess {
  private val logger = LoggerFactory.getLogger(getClass)
  //process plan code 预留标志位，不要删除
  
  
  def deleteGroup(groupID: String, userID: String)(using PlanContext): IO[String] = {
  // val logger = LoggerFactory.getLogger("deleteGroup")  // 同文后端处理: logger 统一
    logger.info(s"[deleteGroup] 开始删除群聊，groupID=${groupID}, userID=${userID}")
  
    val checkOwnerSQL =
      s"""
         SELECT role
         FROM ${schemaName}.group_member_table
         WHERE group_id = ? AND user_id = ?
       """
    val checkOwnerParams = List(SqlParameter("String", groupID), SqlParameter("String", userID))
  
    for {
      // 校验用户权限
      _ <- IO(logger.info(s"[Step 1] 检查用户权限，SQL=${checkOwnerSQL}, 参数为 groupID=${groupID}, userID=${userID}"))
      roleOpt <- readDBJsonOptional(checkOwnerSQL, checkOwnerParams)
      role <- IO {
        roleOpt match {
          case Some(json) => decodeField[String](json, "role")
          case None => throw new IllegalStateException(s"groupID=${groupID}, userID=${userID} 不存在或权限不足")
        }
      }
      _ <- IO(logger.info(s"[Step 1.1] 查询结果 role=${role}"))
      _ <- if (role != "Owner") {
        IO.raiseError(new IllegalStateException(s"userID=${userID} 不是群组 ${groupID} 的群主，无权删除群聊"))
      } else {
        IO(logger.info(s"userID=${userID} 是群组 ${groupID} 的群主，可以删除"))
      }
  
      // 删除群聊记录
      deleteGroupSQL =
        s"""
           DELETE FROM ${schemaName}.group_chat_table
           WHERE group_id = ?
         """
      deleteGroupParams = List(SqlParameter("String", groupID))
      _ <- IO(logger.info(s"[Step 2] 删除群聊记录 SQL=${deleteGroupSQL}, 参数为 groupID=${groupID}"))
      _ <- writeDB(deleteGroupSQL, deleteGroupParams)
  
      deleteMembersSQL =
        s"""
           DELETE FROM ${schemaName}.group_member_table
           WHERE group_id = ?
         """
      deleteMembersParams = List(SqlParameter("String", groupID))
      _ <- IO(logger.info(s"[Step 2.1] 删除群成员记录 SQL=${deleteMembersSQL}, 参数为 groupID=${groupID}"))
      _ <- writeDB(deleteMembersSQL, deleteMembersParams)
  
      // 返回操作结果
      _ <- IO(logger.info(s"[Step 3] 群聊删除成功，groupID=${groupID}"))
    } yield "删除成功"
  }
  
  def createGroup(userID: String, groupName: String)(using PlanContext): IO[String] = {
  // val logger = LoggerFactory.getLogger(getClass)  // 同文后端处理: logger 统一
    for {
      // Step 1: Verify user permissions
      _ <- IO(logger.info(s"开始验证用户权限，对userID:${userID}进行检查"))
      userOpt <- readDBJsonOptional(
        s"SELECT * FROM ${schemaName}.user WHERE user_id = ?",
        List(SqlParameter("String", userID))
      )
      user <- IO {
        userOpt match {
          case Some(json) => decodeType[User](json)
          case None       => throw new IllegalStateException(s"用户ID[${userID}]不存在")
        }
      }
      
      _ <- IO(logger.info(s"用户权限验证通过，userID:${userID}, role:${user.role}"))
  
      // Step 2: Generate new group record
      _ <- IO(logger.info(s"开始创建群聊记录，群名称: ${groupName}, 创建者: ${userID}"))
      groupID = java.util.UUID.randomUUID().toString
      creationTime = DateTime.now()
      _ <- writeDB(
        s"""
  INSERT INTO ${schemaName}.group_chat_table (group_id, group_name, creator_id, created_at)
  VALUES (?, ?, ?, ?)
  """.stripMargin,
        List(
          SqlParameter("String", groupID),
          SqlParameter("String", groupName),
          SqlParameter("String", userID),
          SqlParameter("Long", creationTime.getMillis.toString)
        )
      )
      _ <- IO(logger.info(s"群聊记录创建成功，groupID: ${groupID}"))
  
      // Step 3: Initialize group members
      _ <- IO(logger.info(s"开始初始化群聊成员，将${userID}设置为群主"))
      _ <- writeDB(
        s"""
  INSERT INTO ${schemaName}.group_member_table (group_id, user_id, role)
  VALUES (?, ?, ?)
  """.stripMargin,
        List(
          SqlParameter("String", groupID),
          SqlParameter("String", userID),
          SqlParameter("String", GroupRole.Owner.toString)
        )
      )
      _ <- IO(logger.info(s"群聊成员初始化成功，将${userID}设置为群主"))
  
      // Step 4: Return the generated groupID
      _ <- IO(logger.info(s"群聊创建完成，返回groupID: ${groupID}"))
    } yield groupID
  }
  
  def unmuteMember(groupID: String, memberID: String)(using PlanContext): IO[String] = {
  // val logger = LoggerFactory.getLogger(getClass)  // 同文后端处理: logger 统一
  
    for {
      _ <- IO(logger.info(s"[unmuteMember] 开始解除禁言，群组ID: ${groupID}, 成员ID: ${memberID}"))
  
      // Step 1: 验证用户是否有权限解除禁言
      roleQuery <- IO {
        s"""
  SELECT role 
  FROM ${schemaName}.group_member_table
  WHERE group_id = ? AND user_id = ?;
        """.stripMargin
      }
  
      roleJsonOption <- readDBJsonOptional(
        roleQuery,
        List(
          SqlParameter("String", groupID),
          SqlParameter("String", memberID)
        )
      )
  
      role <- IO(roleJsonOption match {
        case Some(json) => GroupRole.fromString(decodeField[String](json, "role"))
        case None =>
          val errorMessage = s"[unmuteMember] 用户 ${memberID} 不属于群组 ${groupID}"
          logger.error(errorMessage)
          throw new IllegalStateException(errorMessage)
      })
  
      _ <- IO(logger.info(s"[unmuteMember] 用户角色为: ${role.toString}"))
  
      _ <- if (role == GroupRole.Admin || role == GroupRole.Owner) IO.unit
           else IO.raiseError(new IllegalStateException(s"[unmuteMember] 权限不足，用户角色为: ${role.toString}"))
  
      // Step 2: 更新数据库，解除禁言
      updateQuery <- IO {
        s"""
  UPDATE ${schemaName}.group_member_table
  SET mute_until = NULL
  WHERE group_id = ? AND user_id = ?;
        """.stripMargin
      }
  
      dbResponse <- writeDB(
        updateQuery,
        List(
          SqlParameter("String", groupID),
          SqlParameter("String", memberID)
        )
      )
  
      _ <- IO(logger.info(s"[unmuteMember] 禁言解除成功，数据库操作返回: ${dbResponse}"))
  
    } yield "成功解除禁言"
  }
  
  def editGroupName(groupID: String, newGroupName: String)(using PlanContext): IO[String] = {
  // val logger = LoggerFactory.getLogger(getClass)  // 同文后端处理: logger 统一
  
    for {
      // Step 1.1: Query group information from GroupChatTable
      _ <- IO(logger.info(s"[Step 1.1] Query group information for groupID: ${groupID}"))
      groupInfo <- readDBJson(
        s"SELECT * FROM ${schemaName}.group_chat_table WHERE group_id = ?",
        List(SqlParameter("String", groupID))
      )
  
      // Step 1.2: Query user's role in the group
      _ <- IO(logger.info(s"[Step 1.2] Query user's role in group for groupID: ${groupID}"))
      roleJson <- readDBJsonOptional(
        s"SELECT role FROM ${schemaName}.group_member_table WHERE group_id = ? AND user_id = ?",
        List(
          SqlParameter("String", groupID),
          SqlParameter("String", "current_user") // Assumed to be passed somehow e.g., via session/context
        )
      )
  
      _ <- roleJson match {
        case Some(json) =>
          val userRoleString = decodeField[String](json, "role")
          val userRole = GroupRole.fromString(userRoleString) // Convert role to enum
          for {
            _ <- IO(logger.info(s"[Step 1.3] Checking user's permission: role=${userRole}"))
            _ <- if (userRole == GroupRole.Owner || userRole == GroupRole.Admin) {
              IO(logger.info("[Step 1.3] User has sufficient permissions to edit group name"))
            } else {
              IO.raiseError(new Exception("Permission denied: Only group owners or admins can edit the group name."))
            }
          } yield ()
        case None =>
          IO.raiseError(new Exception("Permission denied: User does not belong to the group or role not found."))
      }
  
      // Step 2.1: Update the group name in the GroupChatTable
      _ <- IO(logger.info(s"[Step 2.1] Updating group name to '${newGroupName}' for groupID: ${groupID}"))
      updateResult <- writeDB(
        s"UPDATE ${schemaName}.group_chat_table SET group_name = ? WHERE group_id = ?",
        List(
          SqlParameter("String", newGroupName),
          SqlParameter("String", groupID)
        )
      )
  
      // Ensure update was successful
      _ <- IO(logger.info(s"[Step 2.1] Update result: ${updateResult}"))
  
      // Step 3.1: Return success message
      _ <- IO(logger.info("[Step 3.1] Group name updated successfully."))
    } yield {
      "Group name updated successfully."
    }
  }
  
  
  def muteMember(groupID: String, memberID: String, muteUntil: Option[DateTime])(using PlanContext): IO[String] = {
  // val logger = LoggerFactory.getLogger(getClass)  // 同文后端处理: logger 统一
  
    for {
      // Step 1: 验证用户权限
      _ <- IO(logger.info(s"开始验证请求者操作权限，groupID=${groupID}, memberID=${memberID}"))
      userJson <- readDBJson(
        s"SELECT role FROM ${schemaName}.group_member_table WHERE group_id = ? AND user_id = ?",
        List(SqlParameter("String", groupID), SqlParameter("String", memberID))
      )
      userRole = decodeField[String](userJson, "role")
      _ <- {
        if (userRole != "Owner" && userRole != "Admin") {
          IO.raiseError(new IllegalAccessException("无权限操作，您没有禁言成员的权限"))
        } else {
          IO(logger.info(s"用户权限验证通过，用户角色=${userRole}"))
        }
      }
  
      // Step 2: 更新GroupMemberTable的mute_until字段
      _ <- IO(logger.info(s"开始更新成员的禁言状态，groupID=${groupID}, memberID=${memberID}, muteUntil=${muteUntil}"))
      muteUntilParam <- IO {
        muteUntil.map(date => SqlParameter("DateTime", date.getMillis.toString))
          .getOrElse(SqlParameter("Timestamp", "NULL"))
      }
      _ <- writeDB(
        s"UPDATE ${schemaName}.group_member_table SET mute_until = ? WHERE group_id = ? AND user_id = ?",
        List(muteUntilParam, SqlParameter("String", groupID), SqlParameter("String", memberID))
      )
  
      // Step 3: 记录禁言操作日志
      _ <- IO(logger.info(s"开始记录禁言操作日志"))
      mutedByUserID <- IO("") // 假设执行操作的用户ID从上下文中获取，替换为适合场景的实现
      logID <- IO(java.util.UUID.randomUUID().toString)
      createdAt <- IO(new DateTime())
      durationInMinutes <- IO {
        muteUntil.map(t => ((t.getMillis - createdAt.getMillis) / 60000).toInt).getOrElse(0)
      }
      expiredAt <- IO(muteUntil.getOrElse(new DateTime()))
      _ <- writeDB(
        s"""
           INSERT INTO ${schemaName}.mute_log_table
           (log_id, group_id, user_id, muted_by_user_id, mute_duration, created_at, expired_at)
           VALUES (?, ?, ?, ?, ?, ?, ?)
         """.stripMargin,
        List(
          SqlParameter("String", logID),
          SqlParameter("String", groupID),
          SqlParameter("String", memberID),
          SqlParameter("String", mutedByUserID),
          SqlParameter("Int", durationInMinutes.toString),
          SqlParameter("DateTime", createdAt.getMillis.toString),
          SqlParameter("DateTime", expiredAt.getMillis.toString)
        )
      )
  
      // Step 4: 返回操作结果
      result = s"禁言操作成功，用户 ${memberID} 已被禁言至 ${muteUntil.map(_.toString).getOrElse("无限期")}"
      _ <- IO(logger.info(result))
    } yield result
  }
}

package Impl


/**
 * Planner for user registration that ensures the generated userID is unique.
 * Input:
 * - username: String
 * - password: String
 * - role: UserRole
 * Output:
 * - userID: String
 */
import Objects.UserService.UserRole
import Utils.UserAccountProcess.createUser
import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import org.slf4j.LoggerFactory
import org.joda.time.DateTime
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import cats.implicits._
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
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
import Utils.UserAccountProcess.createUser
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class RegisterUserMessagePlanner(
  username: String,
  password: String,
  role: UserRole,
  override val planContext: PlanContext
) extends Planner[String] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)
  
  override def plan(using planContext: PlanContext): IO[String] = {
    for {
      // Step 1: Call `createUser` to create a user record and generate a unique userID
      _ <- IO(logger.info(s"[RegisterUserMessagePlanner] 开始创建用户，用户名: ${username}, 角色: ${role.toString}"))
      userID <- createUser(username, password, role)

      // Step 2: Log successful creation
      _ <- IO(logger.info(s"[RegisterUserMessagePlanner] 成功生成用户ID: ${userID}"))
    } yield userID
  }

  /**
   * Helper function to check if the provided userID is unique in the database
   *
   * @param userID The userID to check
   * @return IO[Boolean] indicating if the userID is unique
   */
}
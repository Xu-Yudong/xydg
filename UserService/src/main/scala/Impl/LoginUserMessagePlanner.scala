package Impl


import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import Utils.UserAccountProcess.validateUserCredentials
import cats.effect.IO
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import io.circe.syntax._
import io.circe._
import io.circe.generic.auto._
import cats.implicits.*
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
import Utils.UserAccountProcess.validateUserCredentials
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class LoginUserMessagePlanner(
                                    username: String,
                                    password: String,
                                    override val planContext: PlanContext
                                  ) extends Planner[String] {
  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[String] = {
    for {
      // Step 1: Validate username and password, get userID
      _ <- IO(logger.info(s"[LoginUserMessagePlanner] Start validating user credentials for username: ${username}"))
      userID <- validateUserCredentials(username, password)

      // Step 2: Generate a unique sessionToken
      _ <- IO(logger.info(s"[LoginUserMessagePlanner] Validated credentials successfully. Generating session token for userID: ${userID}"))
      sessionToken <- generateSessionToken()

      // Step 3: Record sessionToken in UserSessionTable
      _ <- IO(logger.info(s"[LoginUserMessagePlanner] Storing session token for userID: ${userID}"))
      _ <- storeSessionToken(userID, sessionToken)

      // Step 4: Return the sessionToken to the caller
      _ <- IO(logger.info(s"[LoginUserMessagePlanner] Successfully logged in user. Returning sessionToken: ${sessionToken}"))
    } yield sessionToken
  }

  // Function to generate a unique session token
  private def generateSessionToken()(using PlanContext): IO[String] = {
    IO {
      val token = java.util.UUID.randomUUID().toString.replace("-", "")
      logger.info(s"[generateSessionToken] Generated sessionToken: ${token}")
      token
    }
  }

  // Function to store the session token in the UserSessionTable
  private def storeSessionToken(userID: String, sessionToken: String)(using PlanContext): IO[Unit] = {
    val createdAt = DateTime.now()
    val expiredAt = createdAt.plusDays(7)

    val sql =
      s"""
INSERT INTO ${schemaName}.user_session_table
(session_token, user_id, created_at, expired_at)
VALUES (?, ?, ?, ?);
       """.stripMargin

    val parameters = List(
      SqlParameter("String", sessionToken),
      SqlParameter("String", userID),
      SqlParameter("DateTime", createdAt.getMillis.toString),
      SqlParameter("DateTime", expiredAt.getMillis.toString)
    )

    for {
      _ <- IO(logger.info(s"[storeSessionToken] SQL command: ${sql}"))
      _ <- writeDB(sql, parameters)
      _ <- IO(logger.info(s"[storeSessionToken] Successfully stored session token for userID: ${userID}."))
    } yield ()
  }
}
package Impl


import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import Utils.RedPacketProcess.claimRedPacket
import cats.effect.IO
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
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
import Utils.RedPacketProcess.claimRedPacket
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class ClaimRedPacketMessagePlanner(
                                         sessionToken: String,
                                         redPacketID: String,
                                         override val planContext: PlanContext
                                       ) extends Planner[Double] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  /** Main execution method */
  override def plan(using planContext: PlanContext): IO[Double] = {
    for {
      // Step 1: Validate the session token
      _ <- IO(logger.info(s"Validating sessionToken: ${sessionToken}"))
      userID <- validateSessionToken(sessionToken)

      // Step 2: Check the state and validity of the red packet
      _ <- IO(logger.info(s"Checking the state and validity of redPacketID: ${redPacketID}"))
      _ <- validateRedPacketState(redPacketID, userID)

      // Step 3: Claim the red packet and allocate amount
      _ <- IO(logger.info(s"Calling claimRedPacket for redPacketID: ${redPacketID}, userID: ${userID}"))
      receivedAmount <- claimRedPacket(redPacketID, userID)

      _ <- IO(logger.info(s"Red packet claimed successfully. Amount received: ${receivedAmount}"))
    } yield receivedAmount
  }

  /** Step 1: Validate session token */
  private def validateSessionToken(sessionToken: String)(using PlanContext): IO[String] = {
    for {
      // Search for sessionToken in UserSessionTable
      _ <- IO(logger.info(s"Looking for sessionToken in UserSessionTable: ${sessionToken}"))
      sessionOpt <- readDBJsonOptional(
        s"SELECT * FROM ${schemaName}.user_session_table WHERE session_token = ?;",
        List(SqlParameter("String", sessionToken))
      )

      // Verify sessionToken validity and expiration
      session <- IO(sessionOpt.getOrElse(
        throw new Exception(s"Invalid or expired sessionToken: ${sessionToken}")
      ))
      expiredAt <- IO(decodeField[DateTime](session, "expired_at"))
      _ <- if (expiredAt.isBeforeNow) {
        IO.raiseError(new Exception(s"sessionToken expired at: ${expiredAt}"))
      } else {
        IO.unit
      }

      // Extract userID
      userID <- IO(decodeField[String](session, "user_id"))
      _ <- IO(logger.info(s"sessionToken is valid. Extracted userID: ${userID}"))
    } yield userID
  }

  /** Step 2: Validate red packet state */
  private def validateRedPacketState(redPacketID: String, userID: String)(using PlanContext): IO[Unit] = {
    for {
      // Retrieve redPacket details from RedPacketTable
      _ <- IO(logger.info(s"Fetching redPacket details from RedPacketTable for redPacketID: ${redPacketID}"))
      redPacketOpt <- readDBJsonOptional(
        s"SELECT * FROM ${schemaName}.red_packet_table WHERE red_packet_id = ?;",
        List(SqlParameter("String", redPacketID))
      )
      redPacket <- IO(redPacketOpt.getOrElse(
        throw new Exception(s"Red packet with ID: ${redPacketID} does not exist")
      ))

      // Validate redPacket status and expiration
      status <- IO(decodeField[String](redPacket, "status"))
      expiredAt <- IO(decodeField[DateTime](redPacket, "expired_at"))
      _ <- if (status != "active") {
        IO.raiseError(new Exception(s"Red packet is not active. Current status: ${status}"))
      } else if (expiredAt.isBeforeNow) {
        IO.raiseError(new Exception(s"Red packet expired at: ${expiredAt}"))
      } else {
        IO.unit
      }

      // Check if the user has already claimed the red packet
      _ <- IO(logger.info(s"Checking if user: ${userID} has already claimed redPacketID: ${redPacketID}"))
      claimExists <- readDBJsonOptional(
        s"SELECT * FROM ${schemaName}.red_packet_claim_table WHERE red_packet_id = ? AND claimer_id = ?;",
        List(SqlParameter("String", redPacketID), SqlParameter("String", userID))
      )
      _ <- if (claimExists.isDefined) {
        IO.raiseError(new Exception(s"User: ${userID} has already claimed redPacketID: ${redPacketID}"))
      } else {
        IO.unit
      }

      _ <- IO(logger.info(s"Red packet validation passed for redPacketID: ${redPacketID}"))
    } yield ()
  }
}
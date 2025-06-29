package Impl


import APIs.AuditService.RecordActionLogMessage
import Utils.TransactionProcess.transferAmount
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
import Utils.TransactionProcess.transferAmount
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class TransferInGroupMessagePlanner(
    sessionToken: String,
    groupMemberID: String,
    amount: Double,
    override val planContext: PlanContext
) extends Planner[String] {
  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[String] = {
    for {
      // Step 1: Verify the sessionToken and fetch userID
      userID <- verifySessionToken(sessionToken)

      // Step 2: Validate the transfer amount
      _ <- validateTransferAmount(amount)
      _ <- ensureSufficientBalance(userID, amount)

      // Step 3: Perform the transfer operation
      transferResult <- performTransfer(userID, groupMemberID, amount)

      // Step 4: Record the action log
      _ <- recordActionLog(userID, groupMemberID)

    } yield transferResult
  }

  // Sub-step 1.1: Verify the session token validity and extract userID
  private def verifySessionToken(sessionToken: String)(using PlanContext): IO[String] = {
    logger.info(s"[verifySessionToken] Validating sessionToken=${sessionToken}")
    validateSession(sessionToken).flatMap {
      case None =>
        IO.raiseError(new Exception("Session token validation failed: Unauthorized access"))
      case Some(userID) =>
        IO {
          logger.info(s"[verifySessionToken] Successfully authenticated user: userID=${userID}")
          userID
        }
    }
  }

  private def validateSession(sessionToken: String)(using PlanContext): IO[Option[String]] = {
    val sqlQuery =
      s"SELECT user_id FROM ${schemaName}.session_token_table WHERE session_token = ? AND expiry_time > ?"
    readDBJsonOptional(
      sqlQuery,
      List(
        SqlParameter("String", sessionToken),
        SqlParameter("DateTime", DateTime.now().getMillis.toString)
      )
    ).map(_.map(json => decodeField[String](json, "user_id")))
  }

  // Sub-step 2.1 + 2.2: Validate transfer amount and check the user's balance
  private def validateTransferAmount(amount: Double)(using PlanContext): IO[Unit] = {
    if (amount <= 0) {
      IO.raiseError(new Exception("Invalid amount provided: Amount must be greater than zero"))
    } else {
      IO(logger.info(s"[validateTransferAmount] Transfer amount validated successfully: amount=${amount}"))
    }
  }

  private def ensureSufficientBalance(userID: String, amount: Double)(
      using PlanContext
  ): IO[Unit] = {
    logger.info(s"[ensureSufficientBalance] Checking user (${userID}) balance for transfer")
    val sqlQuery =
      s"SELECT balance FROM ${schemaName}.user_balance_table WHERE user_id = ?"
    readDBJsonOptional(sqlQuery, List(SqlParameter("String", userID)))
      .flatMap {
        case None =>
          IO.raiseError(new Exception(s"UserID(${userID}) not found in balance table"))
        case Some(balanceJson) =>
          val balance = decodeField[Double](balanceJson, "balance")
          if (balance < amount) {
            IO.raiseError(
              new Exception(
                s"Insufficient funds: Current balance=${balance}, required=${amount}"
              )
            )
          } else {
            IO(logger.info(s"[ensureSufficientBalance] User has sufficient balance for transfer"))
          }
      }
  }

  // Step 3: Perform the transfer operation
  private def performTransfer(userID: String, groupMemberID: String, amount: Double)(
      using PlanContext
  ): IO[String] = {
    logger.info(
      s"[performTransfer] Initiating transfer: userID=${userID}, groupMemberID=${groupMemberID}, amount=${amount}"
    )
    transferAmount(userID, groupMemberID, amount)
  }

  // Step 4: Record the action log
  private def recordActionLog(userID: String, groupMemberID: String)(using PlanContext): IO[Unit] = {
    logger.info(
      s"[recordActionLog] Recording transaction action log: userID=${userID}, groupMemberID=${groupMemberID}, action='TransferInGroup'"
    )
    RecordActionLogMessage(
      action = "TransferInGroup",
      targetID = groupMemberID,
      performerID = userID
    ).send.map(_ => ())
  }
}
package Impl


import Objects.UserService.UserRole
import Objects.UserService.User
import APIs.UserService.QueryBalanceMessage
import Utils.ProductApprovalProcess.approveProduct
import Objects.ProductService.ProductStatus
import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import org.slf4j.LoggerFactory
import org.joda.time.DateTime
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
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
import Objects.ProductService.ProductStatus
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class ApproveProductMessagePlanner(reviewerToken: String, productID: String, override val planContext: PlanContext) extends Planner[String] {
  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[String] = for {
    // Step 1: Verify reviewer's permissions
    _ <- IO(logger.info(s"[Step 1] 验证审查员权限 - reviewerToken=${reviewerToken}"))
    reviewerUser <- verifyReviewerToken()
    reviewerID = reviewerUser.userID
    _ <- IO(logger.info(s"[Step 1.2] 审查员ID=${reviewerID}权限验证通过"))

    // Step 2: Approve the product by calling ProductApprovalProcess
    _ <- IO(logger.info(s"[Step 2] 调用ProductApprovalProcess.approveProduct方法 - productID=${productID}"))
    result <- approveProduct(reviewerID, productID)

    // Step 3: Return the operation result
    _ <- IO(logger.info(s"[Step 3] 商品ID=${productID}批准成功 - result=${result}"))
  } yield result

  private def verifyReviewerToken()(using PlanContext): IO[User] = for {
    // Step 1.1: Validate token using QueryBalanceMessage
    _ <- IO(logger.info(s"[Step 1.1] 验证revieToken合法性 - reviewerToken=${reviewerToken}"))
    balance <- QueryBalanceMessage(reviewerToken).send // Validate token
    _ <- IO(logger.info(s"[Step 1.1] Token验证返回余额信息=${balance}"))

    // Step 1.2: Find user information by token
    reviewerUserInfo <- getUserByToken(reviewerToken)

    // Step 1.3: Ensure user has Reviewer role
    _ <- ensureReviewerRole(reviewerUserInfo)
  } yield reviewerUserInfo

  private def getUserByToken(token: String)(using PlanContext): IO[User] = {
    logger.info(s"[Step 1.2] 根据token查找用户信息 - token=${token}")
    val sql =
      s"""
      SELECT user_id, username, password, role, balance
      FROM ${schemaName}.user
      WHERE token = ?;
      """
    readDBJsonOptional(sql, List(SqlParameter("String", token))).flatMap {
      case Some(json) => IO(decodeType[User](json))
      case None => IO.raiseError(new IllegalStateException(s"[Step 1.2] 无法找到与token=${token}匹配的用户"))
    }
  }

  private def ensureReviewerRole(user: User)(using PlanContext): IO[Unit] = {
    if (user.role == UserRole.Reviewer) {
      IO(logger.info(s"[Step 1.3] 用户角色验证通过 - userRole=${user.role.toString}"))
    } else {
      IO.raiseError(new IllegalStateException(s"[Step 1.3] 用户role=${user.role.toString}无审核权限"))
    }
  }
}
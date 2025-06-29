package Impl


import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import Objects.UserService.{User, UserRole}
import Objects.ProductService.ProductStatus
import Utils.ProductApprovalProcess.rejectProduct
import cats.effect.IO
import org.slf4j.LoggerFactory
import org.joda.time.DateTime
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
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
import Objects.UserService.UserRole
import Objects.UserService.User
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}
import Objects.UserService.User

case class RejectProductMessagePlanner(
    reviewerToken: String,
    productID: String,
    reason: String,
    override val planContext: PlanContext
) extends Planner[String] {
  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[String] = {
    for {
      // Step 1: 验证reviewerToken的权限
      _ <- IO(logger.info(s"[Step 1] 验证reviewerToken权限，检查Token=${reviewerToken}"))
      reviewerUser <- validateReviewerToken()

      // Step 2: 调用rejectProduct更新商品状态
      _ <- IO(logger.info(s"[Step 2] 调用rejectProduct方法，更新商品状态为拒绝，并记录原因"))
      result <- rejectProduct(reviewerUser.userID, productID, reason)

      // Step 3: 返回操作成功消息
      _ <- IO(logger.info(s"[Step 3] 构建操作成功消息"))
    } yield result
  }

  // 验证reviewerToken的权限
  private def validateReviewerToken()(using PlanContext): IO[User] = {
    for {
      // Step 1.1: 检查reviewerToken是否合法且未过期
      _ <- IO(logger.info(s"检查reviewerToken是否合法且未过期"))
      userOpt <- readDBJsonOptional(
        s"SELECT * FROM ${schemaName}.user WHERE user_id = ?;",
        List(SqlParameter("String", reviewerToken))
      )
      user <- userOpt match {
        case Some(json) => IO(decodeType[User](json))
        case None =>
          val errorMessage = s"Token [${reviewerToken}] 无效或已过期"
          IO(logger.error(errorMessage)) >> IO.raiseError(new IllegalStateException(errorMessage))
      }

      // Step 1.2: 确保reviewerToken用户角色是UserRole中的Reviewer
      _ <- if (user.role == UserRole.Reviewer) {
        IO(logger.info(s"用户 [${user.userID}] 验证通过，具有审核权限"))
      } else {
        val errorMessage = s"用户 [${user.userID}] 无审核权限，其角色为 [${user.role}]"
        IO(logger.error(errorMessage)) >> IO.raiseError(new IllegalStateException(errorMessage))
      }
    } yield user
  }
}
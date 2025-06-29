package Impl


import Objects.ProductService.ProductStatus
import Objects.ProductService.Product
import Utils.ProductApprovalProcess.submitProduct
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
import Utils.ProductApprovalProcess.submitProduct
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class SubmitProductForApprovalMessagePlanner(
    sessionToken: String,
    product: Product,
    override val planContext: PlanContext
) extends Planner[String] {
  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[String] = {
    for {
      _ <- IO(logger.info(s"验证sessionToken并提取用户信息"))
      sellerID <- validateSessionAndExtractSellerID(sessionToken)

      _ <- IO(logger.info(s"执行商品提交操作"))
      productID <- submitProductForApproval(sellerID, product)

      _ <- IO(logger.info(s"返回生成的productID：${productID}"))
    } yield productID
  }

  private def validateSessionAndExtractSellerID(sessionToken: String)(using PlanContext): IO[String] = {
    for {
      _ <- IO(require(sessionToken.nonEmpty, "sessionToken不能为空"))
      _ <- IO(logger.info(s"调用validateSessionToken验证sessionToken"))

      userID <- validateSessionToken(sessionToken)

      _ <- IO(logger.info(s"验证完成，userID：${userID}"))

      hasPermission <- checkUserPermission(userID)

      _ <- IO {
        require(hasPermission, s"userID=${userID}没有提交商品的权限")
        logger.info(s"用户 ${userID} 验证通过，具备提交商品权限")
      }

      sellerID <- getSellerID(userID)
      _ <- IO(logger.info(s"提取到sellerID：${sellerID}"))
    } yield sellerID
  }

  private def validateSessionToken(sessionToken: String)(using PlanContext): IO[String] = {
    val sql =
      s"""
         SELECT user_id
         FROM ${schemaName}.session_table
         WHERE session_token = ?;
         """
    logger.info(s"创建SQL验证sessionToken：${sql}")
    readDBString(sql, List(SqlParameter("String", sessionToken)))
  }

  private def checkUserPermission(userID: String)(using PlanContext): IO[Boolean] = {
    val sql =
      s"""
         SELECT can_submit_product
         FROM ${schemaName}.user_permissions
         WHERE user_id = ?;
         """
    logger.info(s"创建SQL检查权限：${sql}")
    readDBBoolean(sql, List(SqlParameter("String", userID)))
  }

  private def getSellerID(userID: String)(using PlanContext): IO[String] = {
    val sql =
      s"""
         SELECT seller_id
         FROM ${schemaName}.user_table
         WHERE user_id = ?;
         """
    logger.info(s"创建SQL提取sellerID：${sql}")
    readDBString(sql, List(SqlParameter("String", userID)))
  }

  private def submitProductForApproval(sellerID: String, product: Product)(using PlanContext): IO[String] = {
    for {
      _ <- IO(require(product.name.nonEmpty, "商品名称不能为空"))
      _ <- IO(require(product.description.nonEmpty, "商品描述不能为空"))
      _ <- IO(require(product.price > 0, "商品价格必须大于0"))
      _ <- IO(logger.info(s"商品参数验证通过, name=${product.name}, description=${product.description}, price=${product.price}"))

      productID <- submitProduct(sellerID, product)

      _ <- IO(logger.info(s"商品提交成功，生成了productID=${productID}"))
    } yield productID
  }
}
package Utils

//process plan import 预留标志位，不要删除
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.joda.time.DateTime
import Common.DBAPI._
import Common.ServiceUtils.schemaName
import org.slf4j.LoggerFactory
import Objects.ProductService.Product
import Objects.ProductService.ProductStatus
import Common.API.{PlanContext, Planner}
import Common.Object.SqlParameter
import cats.effect.IO
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}
import Common.API.{PlanContext}
import Common.API.PlanContext
import Objects.UserService.{User, UserRole}
import cats.implicits._
import Objects.UserService.UserRole
import Objects.UserService.User
import Objects.UserService.{UserRole, User}

case object ProductApprovalProcess {
  private val logger = LoggerFactory.getLogger(getClass)
  //process plan code 预留标志位，不要删除
  
  def submitProduct(sellerID: String, product: Product)(using PlanContext): IO[String] = {
    for {
      // Step 1: Validate input parameters
      _ <- IO {
        require(sellerID.nonEmpty, "sellerID不能为空")
        require(product.name.nonEmpty, "商品名称不能为空")
        require(product.description.nonEmpty, "商品描述不能为空")
        require(product.price > 0, "商品价格必须大于0")
        logger.info(s"输入参数验证成功，sellerID=${sellerID}, product.name=${product.name}, product.price=${product.price}")
      }
  
      // Step 2: Generate unique productID and status, prepare other fields
      productID <- IO(java.util.UUID.randomUUID().toString)
      productStatus <- IO(ProductStatus.Pending.toString)
      now <- IO(DateTime.now)
  
      _ <- IO(logger.info(s"生成了唯一商品ID: ${productID}，状态设置为: ${productStatus}，当前时间戳: ${now.getMillis}"))
  
      // Step 3: Prepare parameters for database insertion
      insertParams <- IO(List(
        SqlParameter("String", productID),
        SqlParameter("String", product.name),
        SqlParameter("String", product.description),
        SqlParameter("Double", product.price.toString),
        SqlParameter("String", productStatus),
        SqlParameter("String", sellerID),
        SqlParameter("Long", now.getMillis.toString),        // created_at
        SqlParameter("Long", now.getMillis.toString)         // updated_at
      ))
      _ <- IO(logger.info(s"准备插入数据库的参数: ${insertParams}"))
  
      // Step 4: Insert the product record into the database
      _ <- writeDB(
        s"""
        INSERT INTO ${schemaName}.product_table (product_id, name, description, price, status, seller_id, created_at, updated_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """.stripMargin,
        insertParams
      )
      _ <- IO(logger.info(s"商品记录已成功写入数据库：productID=${productID}, sellerID=${sellerID}"))
  
    } yield productID
  }
  
  def rejectProduct(reviewerID: String, productID: String, reason: String)(using PlanContext): IO[String] = {
  // val logger = LoggerFactory.getLogger("rejectProduct")  // 同文后端处理: logger 统一
  
    for {
      // Step 1.1: 验证审核权限
      _ <- IO(logger.info(s"[Step 1.1] 验证审核权限，检查 reviewerID=${reviewerID} 是否具有权限"))
      reviewerUserOpt <- readDBJsonOptional(
        s"SELECT * FROM ${schemaName}.user WHERE user_id = ?;",
        List(SqlParameter("String", reviewerID))
      )
      reviewerUser <- reviewerUserOpt match {
        case Some(json) => IO(decodeType[User](json))
        case None =>
          val errorMessage = s"用户ID [${reviewerID}] 不存在"
          IO(logger.error(errorMessage)) >> IO.raiseError(new IllegalStateException(errorMessage))
      }
      _ <- if reviewerUser.role == UserRole.Reviewer then
        IO(logger.info(s"用户ID [${reviewerID}] 验证通过，具有审核权限"))
      else {
        val errorMessage = s"用户ID [${reviewerID}] 无审核权限"
        IO(logger.error(errorMessage)) >> IO.raiseError(new IllegalStateException(errorMessage))
      }
  
      // Step 2.1: 在商品表中查找商品记录
      _ <- IO(logger.info(s"[Step 2.1] 查找商品ID=${productID} 的记录"))
      productOpt <- readDBJsonOptional(
        s"SELECT * FROM ${schemaName}.product_table WHERE product_id = ?;",
        List(SqlParameter("String", productID))
      )
      product <- productOpt match {
        case Some(json) => IO(json)
        case None =>
          val errorMessage = s"商品ID [${productID}] 不存在"
          IO(logger.error(errorMessage)) >> IO.raiseError(new IllegalStateException(errorMessage))
      }
  
      // Step 2.2: 更新商品状态为拒绝，并记录拒绝原因
      _ <- IO(logger.info(s"[Step 2.2] 更新商品状态为拒绝，记录拒绝原因 [${reason}]"))
      _ <- writeDB(
        s"UPDATE ${schemaName}.product_table SET status = ?, updated_at = ? WHERE product_id = ?;",
        List(
          SqlParameter("String", ProductStatus.Rejected.toString),
          SqlParameter("DateTime", DateTime.now().getMillis.toString),
          SqlParameter("String", productID)
        )
      )
  
      // Step 3.1: 记录审核日志
      _ <- IO(logger.info(s"[Step 3.1] 在审核日志表中新增审核日志记录"))
      _ <- writeDB(
        s"""
        INSERT INTO ${schemaName}.product_audit_log_table (
          audit_log_id, product_id, reviewer_id, action, reason, timestamp
        ) VALUES (?, ?, ?, ?, ?, ?);
        """.stripMargin,
        List(
          SqlParameter("String", java.util.UUID.randomUUID().toString), // audit_log_id
          SqlParameter("String", productID), // product_id
          SqlParameter("String", reviewerID), // reviewer_id
          SqlParameter("String", "reject"), // action
          SqlParameter("String", reason), // reason
          SqlParameter("DateTime", DateTime.now().getMillis.toString) // timestamp
        )
      )
  
      // Step 4.1: 返回操作结果
      _ <- IO(logger.info(s"[Step 4.1] 商品ID=${productID} 已拒绝审核，返回成功消息"))
    } yield "操作成功: 商品已拒绝审核"
  }
  
  def approveProduct(reviewerID: String, productID: String)(using PlanContext): IO[String] = {
    for {
      // Step 1: Verify the reviewer’s permissions
      _ <- IO(logger.info(s"开始验证审核员权限，reviewerID=${reviewerID}"))
      userJson <- readDBJson(
        s"SELECT * FROM ${schemaName}.user WHERE user_id = ?;",
        List(SqlParameter("String", reviewerID))
      )
      userRole = decodeField[String](userJson, "role")
      _ <- IO(logger.debug(s"审核员的角色为：${userRole}"))
      _ <- if (List(UserRole.Reviewer.toString, UserRole.Business.toString).contains(userRole)) {
        IO(logger.info(s"审核员ID=${reviewerID}权限验证通过"))
      } else {
        IO.raiseError(new IllegalStateException(s"审核员ID=${reviewerID}权限不足"))
      }
  
      // Step 2: Validate the product status
      _ <- IO(logger.info(s"开始检查商品状态，productID=${productID}"))
      productJsonOptional <- readDBJsonOptional(
        s"SELECT * FROM ${schemaName}.product_table WHERE product_id = ?;",
        List(SqlParameter("String", productID))
      )
      productJson <- IO.fromOption(productJsonOptional)(
        new IllegalStateException(s"商品ID=${productID}不存在")
      )
      productStatus = decodeField[String](productJson, "status")
      _ <- IO(logger.debug(s"商品的当前状态为：${productStatus}"))
      _ <- if (productStatus == ProductStatus.Pending.toString) {
        IO(logger.info(s"商品ID=${productID}状态验证通过，处于待审核状态"))
      } else {
        IO.raiseError(new IllegalStateException(s"商品ID=${productID}不是待审核状态，当前状态为${productStatus}"))
      }
  
      // Step 3: Update the product status to "Available"
      _ <- IO(logger.info(s"开始更新商品ID=${productID}的状态为上架"))
      updateProductSQL =
        s"""
        UPDATE ${schemaName}.product_table
        SET status = ?, updated_at = ?
        WHERE product_id = ?;
        """
      updatedTimestamp <- IO { DateTime.now }
      updateProductParams = List(
        SqlParameter("String", ProductStatus.Available.toString),
        SqlParameter("DateTime", updatedTimestamp.getMillis.toString),
        SqlParameter("String", productID)
      )
      _ <- writeDB(updateProductSQL, updateProductParams)
      _ <- IO(logger.info(s"商品状态更新完成，商品ID=${productID}状态变更为${ProductStatus.Available}"))
  
      // Step 3.1: Insert an audit log for the product approval
      _ <- IO(logger.info(s"开始插入商品审核操作日志，审查员ID=${reviewerID}, 商品ID=${productID}"))
      insertAuditLogSQL =
        s"""
        INSERT INTO ${schemaName}.product_audit_log_table
        (audit_log_id, product_id, reviewer_id, action, reason, timestamp)
        VALUES (?, ?, ?, ?, ?, ?);
        """
      auditLogID <- IO { java.util.UUID.randomUUID().toString }
      insertAuditLogParams = List(
        SqlParameter("String", auditLogID),
        SqlParameter("String", productID),
        SqlParameter("String", reviewerID),
        SqlParameter("String", "approve"),
        SqlParameter("String", "商品审核通过"),
        SqlParameter("DateTime", updatedTimestamp.getMillis.toString)
      )
      _ <- writeDB(insertAuditLogSQL, insertAuditLogParams)
      _ <- IO(logger.info(s"审核日志插入完成，日志ID=${auditLogID}"))
  
      // Step 4: Prepare and return the result message
      resultMessage <- IO { s"商品ID=${productID}已成功上架" }
      _ <- IO(logger.info(resultMessage))
    } yield resultMessage
  }
}

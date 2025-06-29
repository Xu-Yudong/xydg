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
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}
import Common.API.{PlanContext}
import Objects.ProductService.ProductTransaction
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}

case object ProductTransactionProcess {
  private val logger = LoggerFactory.getLogger(getClass)
  //process plan code 预留标志位，不要删除
  
  
  def processPurchase(buyerID: String, productID: String)(using PlanContext): IO[String] = {
  // val logger = LoggerFactory.getLogger("processPurchase")  // 同文后端处理: logger 统一
    logger.info(s"开始处理购买请求，buyerID=${buyerID}, productID=${productID}")
  
    for {
      // Step 1: 验证购买者的账户余额是否足够
      buyerBalanceJsonOpt <- readDBJsonOptional(
        s"SELECT balance FROM ${schemaName}.user_table WHERE user_id = ?;",
        List(SqlParameter("String", buyerID))
      )
      _ <- IO(logger.info(s"验证购买者余额信息，buyerBalanceJsonOpt=${buyerBalanceJsonOpt}"))
      buyerBalance <- buyerBalanceJsonOpt match {
        case Some(json) => IO(decodeField[Double](json, "balance"))
        case None =>
          IO.raiseError(new IllegalStateException(s"无法找到buyerID为${buyerID}的账户信息"))
      }
  
      productInfoOpt <- readDBJsonOptional(
        s"SELECT status, price, seller_id FROM ${schemaName}.product_table WHERE product_id = ?;",
        List(SqlParameter("String", productID))
      )
      _ <- IO(logger.info(s"获取商品信息，productInfoOpt=${productInfoOpt}"))
      productInfo <- productInfoOpt match {
        case Some(json) =>
          IO {
            (
              decodeField[String](json, "status"),
              decodeField[Double](json, "price"),
              decodeField[String](json, "seller_id")
            )
          }
        case None =>
          IO.raiseError(new IllegalStateException(s"无法找到productID为${productID}的商品信息"))
      }
  
      (productStatus, productPrice, sellerID) = productInfo
  
      // 验证购买者是否有足够余额
      _ <- if (buyerBalance < productPrice) {
        IO.raiseError(new IllegalStateException(s"购买者余额不足，余额=${buyerBalance}，商品价格=${productPrice}"))
      } else {
        IO(logger.info(s"购买者余额充足，余额=${buyerBalance}，商品价格=${productPrice}"))
      }
  
      // Step 2: 从商品表中确认商品的合法性
      _ <- IO(logger.info(s"验证商品状态是否合法，商品状态=${productStatus}"))
      _ <- if (productStatus != "Available") {
        IO.raiseError(new IllegalStateException(s"商品状态不合法，当前状态为：${productStatus}"))
      } else {
        IO(logger.info("商品状态合法，继续处理"))
      }
  
      // Step 3: 扣除购买者账户余额并增加卖家账户金额
      _ <- IO(logger.info(s"准备更新账户信息"))
      _ <- writeDB(
        s"UPDATE ${schemaName}.user_table SET balance = balance - ? WHERE user_id = ?;",
        List(
          SqlParameter("Double", productPrice.toString),
          SqlParameter("String", buyerID)
        )
      )
      _ <- writeDB(
        s"UPDATE ${schemaName}.user_table SET balance = balance + ? WHERE user_id = ?;",
        List(
          SqlParameter("Double", productPrice.toString),
          SqlParameter("String", sellerID)
        )
      )
  
      // Step 4: 记录交易信息到ProductTransactionTable
      transactionID <- IO(java.util.UUID.randomUUID().toString)
      transactionTime <- IO(DateTime.now())
      _ <- IO(logger.info(s"记录交易信息，transactionID=${transactionID}, 时间戳=${transactionTime}"))
      _ <- writeDB(
        s"""
        INSERT INTO ${schemaName}.product_transaction_table 
        (transaction_id, product_id, buyer_id, seller_id, amount, created_at) 
        VALUES (?, ?, ?, ?, ?, ?);
        """,
        List(
          SqlParameter("String", transactionID),
          SqlParameter("String", productID),
          SqlParameter("String", buyerID),
          SqlParameter("String", sellerID),
          SqlParameter("Double", productPrice.toString),
          SqlParameter("DateTime", transactionTime.getMillis.toString)
        )
      )
  
      // Step 5: 返回交易成功消息
      _ <- IO(logger.info(s"购买商品处理完成，返回成功消息"))
    } yield "交易成功"
  }
  
  def querySalesRecord(sellerID: String)(using PlanContext): IO[List[ProductTransaction]] = {
    for {
      // Step 1: Validate input parameters
      _ <- IO(logger.info(s"验证输入参数 sellerID 的有效性"))
      _ <- if (sellerID.trim.isEmpty) IO.raiseError(new IllegalArgumentException("sellerID 不能为空")) else IO.unit
  
      // Step 2: Prepare SQL query and parameters
      _ <- IO(logger.info(s"开始查询 sellerID=${sellerID} 的销售记录"))
      sqlQuery <- IO {
        s"""
           SELECT transaction_id, product_id, buyer_id, seller_id, amount, created_at
           FROM ${schemaName}.product_transaction_table
           WHERE seller_id = ?;
         """.stripMargin
      }
      parameters <- IO {
        List(SqlParameter("String", sellerID))
      }
      _ <- IO(logger.info(s"执行 SQL 查询：${sqlQuery}，参数：${parameters.mkString(", ")}"))
  
      // Step 3: Execute the query
      rows <- readDBRows(sqlQuery, parameters)
      _ <- IO(logger.info(s"查询到 ${rows.size} 条交易记录"))
  
      // Step 4: Map rows to ProductTransaction objects
      salesRecords <- IO {
        rows.map { json =>
          val transactionID = decodeField[String](json, "transaction_id")
          val productID = decodeField[String](json, "product_id")
          val buyerID = decodeField[String](json, "buyer_id")
          val amount = decodeField[Double](json, "amount")
          val createdAtMillis = decodeField[Long](json, "created_at")
          val createdAt = new DateTime(createdAtMillis)
  
          logger.debug(s"解析记录 transactionID=${transactionID}, productID=${productID}, buyerID=${buyerID}, amount=${amount}, created_at=${createdAt}")
          ProductTransaction(
            transactionID = transactionID,
            productID = productID,
            buyerID = buyerID,
            sellerID = sellerID,
            amount = amount,
            timestamp = createdAt
          )
        }
      }
      _ <- IO(logger.info(s"成功映射 ${salesRecords.size} 条交易记录为 ProductTransaction 对象列表"))
    } yield salesRecords
  }
}

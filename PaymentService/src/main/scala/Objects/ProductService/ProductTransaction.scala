package Objects.ProductService


import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.*
import io.circe.parser.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

import com.fasterxml.jackson.core.`type`.TypeReference
import Common.Serialize.JacksonSerializeUtils

import scala.util.Try

import org.joda.time.DateTime
import java.util.UUID


/**
 * ProductTransaction
 * desc: 商品交易记录包含买家、卖家以及交易金额的信息。
 * @param transactionID: String (交易记录的唯一标识ID)
 * @param productID: String (商品的唯一标识ID)
 * @param buyerID: String (购买者的唯一标识ID)
 * @param sellerID: String (出售者的唯一标识ID)
 * @param amount: Double (交易金额)
 * @param timestamp: DateTime (交易发生的时间戳)
 */

case class ProductTransaction(
  transactionID: String,
  productID: String,
  buyerID: String,
  sellerID: String,
  amount: Double,
  timestamp: DateTime
){

  //process class code 预留标志位，不要删除


}


case object ProductTransaction{

    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[ProductTransaction] = deriveEncoder
  private val circeDecoder: Decoder[ProductTransaction] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[ProductTransaction] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[ProductTransaction] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[ProductTransaction]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given productTransactionEncoder: Encoder[ProductTransaction] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given productTransactionDecoder: Decoder[ProductTransaction] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }



  //process object code 预留标志位，不要删除


}


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
import Objects.ProductService.ProductStatus

/**
 * Product
 * desc: 商品的信息，包含名称、描述、价格等关键属性
 * @param productID: String (商品的唯一标识)
 * @param name: String (商品名称)
 * @param description: String (商品描述信息)
 * @param price: Double (商品价格)
 * @param status: ProductStatus:1117 (商品当前状态)
 * @param sellerID: String (卖家ID)
 */

case class Product(
  productID: String,
  name: String,
  description: String,
  price: Double,
  status: ProductStatus,
  sellerID: String
){

  //process class code 预留标志位，不要删除


}


case object Product{

    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[Product] = deriveEncoder
  private val circeDecoder: Decoder[Product] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[Product] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[Product] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[Product]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given productEncoder: Encoder[Product] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given productDecoder: Decoder[Product] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }



  //process object code 预留标志位，不要删除


}


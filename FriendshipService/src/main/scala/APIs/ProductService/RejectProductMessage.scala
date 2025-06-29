package APIs.ProductService

import Common.API.API
import Global.ServiceCenter.ProductServiceCode

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
 * RejectProductMessage
 * desc: 审核用户拒绝商品上架，并记录拒绝原因。
 * @param reviewerToken: String (审核用户的身份令牌，用于验证权限。)
 * @param productID: String (待审核的商品ID，标识具体商品。)
 * @param reason: String (审核拒绝的原因，供记录使用。)
 * @return result: String (操作结果消息，如审核拒绝成功。)
 */

case class RejectProductMessage(
  reviewerToken: String,
  productID: String,
  reason: String
) extends API[String](ProductServiceCode)



case object RejectProductMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[RejectProductMessage] = deriveEncoder
  private val circeDecoder: Decoder[RejectProductMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[RejectProductMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[RejectProductMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[RejectProductMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given rejectProductMessageEncoder: Encoder[RejectProductMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given rejectProductMessageDecoder: Decoder[RejectProductMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}


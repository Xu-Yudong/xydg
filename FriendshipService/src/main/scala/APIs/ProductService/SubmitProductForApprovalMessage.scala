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
import Objects.ProductService.Product

/**
 * SubmitProductForApprovalMessage
 * desc: 提交商品上架申请
 * @param sessionToken: String (会话令牌，用于验证当前用户的登录状态)
 * @param product: Product:1130 (商品信息对象，包含商品ID、名称、描述、价格和状态等信息)
 * @return productID: String (生成的商品ID，唯一标识一个商品)
 */

case class SubmitProductForApprovalMessage(
  sessionToken: String,
  product: Product
) extends API[String](ProductServiceCode)



case object SubmitProductForApprovalMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[SubmitProductForApprovalMessage] = deriveEncoder
  private val circeDecoder: Decoder[SubmitProductForApprovalMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[SubmitProductForApprovalMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[SubmitProductForApprovalMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[SubmitProductForApprovalMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given submitProductForApprovalMessageEncoder: Encoder[SubmitProductForApprovalMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given submitProductForApprovalMessageDecoder: Decoder[SubmitProductForApprovalMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}


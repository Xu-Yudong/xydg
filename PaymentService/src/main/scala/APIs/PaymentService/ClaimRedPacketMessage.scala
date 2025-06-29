package APIs.PaymentService

import Common.API.API
import Global.ServiceCenter.PaymentServiceCode

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
 * ClaimRedPacketMessage
 * desc: 系统验证sessionToken，用户领取红包后，更新账户余额及红包状态，返回领取金额。
 * @param sessionToken: String (用户会话令牌，用于验证用户身份。)
 * @param redPacketID: String (红包ID，用于唯一标识待领取的红包。)
 * @return receivedAmount: Double (领取的红包金额。)
 */

case class ClaimRedPacketMessage(
  sessionToken: String,
  redPacketID: String
) extends API[Double](PaymentServiceCode)



case object ClaimRedPacketMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[ClaimRedPacketMessage] = deriveEncoder
  private val circeDecoder: Decoder[ClaimRedPacketMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[ClaimRedPacketMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[ClaimRedPacketMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[ClaimRedPacketMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given claimRedPacketMessageEncoder: Encoder[ClaimRedPacketMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given claimRedPacketMessageDecoder: Decoder[ClaimRedPacketMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}


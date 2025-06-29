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
 * SendRedPacketMessage
 * desc: 系统验证sessionToken，扣除发红包人的账户金额，生成红包记录并返回红包ID。用于处理发送红包的需求。
 * @param sessionToken: String (用户的会话令牌，用于验证用户身份)
 * @param groupID: String (群聊的唯一标识符，用于指定红包所属的群)
 * @param amount: Double (红包总金额)
 * @param participantCount: Int (参与红包的用户数量)
 * @return redPacketID: String (生成的红包唯一标识符)
 */

case class SendRedPacketMessage(
  sessionToken: String,
  groupID: String,
  amount: Double,
  participantCount: Int
) extends API[String](PaymentServiceCode)



case object SendRedPacketMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[SendRedPacketMessage] = deriveEncoder
  private val circeDecoder: Decoder[SendRedPacketMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[SendRedPacketMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[SendRedPacketMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[SendRedPacketMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given sendRedPacketMessageEncoder: Encoder[SendRedPacketMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given sendRedPacketMessageDecoder: Decoder[SendRedPacketMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}


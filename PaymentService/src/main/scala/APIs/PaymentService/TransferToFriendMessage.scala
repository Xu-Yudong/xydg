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
 * TransferToFriendMessage
 * desc: 用于处理好友转账的需求。通过验证sessionToken完成好友间的转账操作，并更新双方的账户金额。
 * @param sessionToken: String (用于验证用户身份的会话令牌。)
 * @param friendID: String (接收转账的好友用户ID。)
 * @param amount: Double (转账金额。)
 * @return result: String (操作结果，返回转账完成状态的消息。)
 */

case class TransferToFriendMessage(
  sessionToken: String,
  friendID: String,
  amount: Double
) extends API[String](PaymentServiceCode)



case object TransferToFriendMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[TransferToFriendMessage] = deriveEncoder
  private val circeDecoder: Decoder[TransferToFriendMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[TransferToFriendMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[TransferToFriendMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[TransferToFriendMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given transferToFriendMessageEncoder: Encoder[TransferToFriendMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given transferToFriendMessageDecoder: Decoder[TransferToFriendMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}


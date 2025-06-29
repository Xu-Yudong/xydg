package APIs.ChatService

import Common.API.API
import Global.ServiceCenter.ChatServiceCode

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
 * SendMessageToFriendMessage
 * desc: 好友聊天消息发送。
 * @param sessionToken: String (用户的会话令牌，用于验证身份。)
 * @param friendID: String (目标好友的用户ID。)
 * @param content: String (聊天消息的内容。)
 * @return result: String (操作结果信息，例如是否发送成功。)
 */

case class SendMessageToFriendMessage(
  sessionToken: String,
  friendID: String,
  content: String
) extends API[String](ChatServiceCode)



case object SendMessageToFriendMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[SendMessageToFriendMessage] = deriveEncoder
  private val circeDecoder: Decoder[SendMessageToFriendMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[SendMessageToFriendMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[SendMessageToFriendMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[SendMessageToFriendMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given sendMessageToFriendMessageEncoder: Encoder[SendMessageToFriendMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given sendMessageToFriendMessageDecoder: Decoder[SendMessageToFriendMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}


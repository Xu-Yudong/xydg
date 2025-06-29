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
import Objects.ChatService.ChatMessage

/**
 * ReceiveMessageFromFriendMessage
 * desc: 好友间接收未读消息，用于处理好友聊天消息接收的需求。
 * @param sessionToken: String (用户会话令牌，用于验证登录状态和用户身份。)
 * @param friendID: String (好友的用户ID，用于查询发送方的未读消息。)
 * @return messages: ChatMessage:1019 (好友发送的未读聊天消息列表。)
 */

case class ReceiveMessageFromFriendMessage(
  sessionToken: String,
  friendID: String
) extends API[List[ChatMessage]](ChatServiceCode)



case object ReceiveMessageFromFriendMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[ReceiveMessageFromFriendMessage] = deriveEncoder
  private val circeDecoder: Decoder[ReceiveMessageFromFriendMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[ReceiveMessageFromFriendMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[ReceiveMessageFromFriendMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[ReceiveMessageFromFriendMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given receiveMessageFromFriendMessageEncoder: Encoder[ReceiveMessageFromFriendMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given receiveMessageFromFriendMessageDecoder: Decoder[ReceiveMessageFromFriendMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}


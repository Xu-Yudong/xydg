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
 * DeleteGroupChatMessage
 * desc: 删除群聊记录及所有相关成员信息
 * @param sessionToken: String (用户登录会话的令牌，用于验证用户身份)
 * @param groupID: String (群聊的唯一标识，用于定位具体群聊)
 * @return result: String (操作结果提示信息，表示群聊记录及相关成员信息删除的结果)
 */

case class DeleteGroupChatMessage(
  sessionToken: String,
  groupID: String
) extends API[String](ChatServiceCode)



case object DeleteGroupChatMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[DeleteGroupChatMessage] = deriveEncoder
  private val circeDecoder: Decoder[DeleteGroupChatMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[DeleteGroupChatMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[DeleteGroupChatMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[DeleteGroupChatMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given deleteGroupChatMessageEncoder: Encoder[DeleteGroupChatMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given deleteGroupChatMessageDecoder: Decoder[DeleteGroupChatMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}


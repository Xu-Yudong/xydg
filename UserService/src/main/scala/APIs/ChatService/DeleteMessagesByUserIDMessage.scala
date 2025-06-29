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
 * DeleteMessagesByUserIDMessage
 * desc: 删除指定用户的所有聊天消息。
 * @param userID: String (用户唯一标识，指定要删除聊天记录的用户。)
 * @return result: String (操作结果信息，表示删除是否成功。)
 */

case class DeleteMessagesByUserIDMessage(
  userID: String
) extends API[String](ChatServiceCode)



case object DeleteMessagesByUserIDMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[DeleteMessagesByUserIDMessage] = deriveEncoder
  private val circeDecoder: Decoder[DeleteMessagesByUserIDMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[DeleteMessagesByUserIDMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[DeleteMessagesByUserIDMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[DeleteMessagesByUserIDMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given deleteMessagesByUserIDMessageEncoder: Encoder[DeleteMessagesByUserIDMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given deleteMessagesByUserIDMessageDecoder: Decoder[DeleteMessagesByUserIDMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}


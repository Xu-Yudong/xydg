package APIs.FriendshipService

import Common.API.API
import Global.ServiceCenter.FriendshipServiceCode

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
 * AddFriendMessage
 * desc: 添加好友关系，用于处理添加好友的需求。
 * @param sessionToken: String (用户的会话令牌，用于识别和验证用户身份)
 * @param friendID: String (好友的用户ID，表示要添加为好友的目标用户)
 * @return result: String (添加好友操作的结果信息)
 */

case class AddFriendMessage(
  sessionToken: String,
  friendID: String
) extends API[String](FriendshipServiceCode)



case object AddFriendMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[AddFriendMessage] = deriveEncoder
  private val circeDecoder: Decoder[AddFriendMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[AddFriendMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[AddFriendMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[AddFriendMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given addFriendMessageEncoder: Encoder[AddFriendMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given addFriendMessageDecoder: Decoder[AddFriendMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}


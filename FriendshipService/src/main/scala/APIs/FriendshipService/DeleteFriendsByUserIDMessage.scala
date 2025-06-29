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
 * DeleteFriendsByUserIDMessage
 * desc: 删除指定用户的所有好友关系，用于处理用户注销时清理用户的好友关联数据。
 * @param userID: String (用户ID，用于标识需要清理好友关系的用户。)
 * @return result: String (表示删除操作的结果信息，例如成功消息。)
 */

case class DeleteFriendsByUserIDMessage(
  userID: String
) extends API[String](FriendshipServiceCode)



case object DeleteFriendsByUserIDMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[DeleteFriendsByUserIDMessage] = deriveEncoder
  private val circeDecoder: Decoder[DeleteFriendsByUserIDMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[DeleteFriendsByUserIDMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[DeleteFriendsByUserIDMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[DeleteFriendsByUserIDMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given deleteFriendsByUserIDMessageEncoder: Encoder[DeleteFriendsByUserIDMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given deleteFriendsByUserIDMessageDecoder: Decoder[DeleteFriendsByUserIDMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}


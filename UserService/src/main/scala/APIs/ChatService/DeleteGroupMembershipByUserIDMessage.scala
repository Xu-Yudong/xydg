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
 * DeleteGroupMembershipByUserIDMessage
 * desc: 移除指定用户的群聊成员角色，用于处理用户注销时清理群聊用户关系的需求。
 * @param userID: String (用户ID，表示需要移除群成员角色的目标用户。)
 * @return result: String (操作执行结果信息，返回是否成功。)
 */

case class DeleteGroupMembershipByUserIDMessage(
  userID: String
) extends API[String](ChatServiceCode)



case object DeleteGroupMembershipByUserIDMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[DeleteGroupMembershipByUserIDMessage] = deriveEncoder
  private val circeDecoder: Decoder[DeleteGroupMembershipByUserIDMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[DeleteGroupMembershipByUserIDMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[DeleteGroupMembershipByUserIDMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[DeleteGroupMembershipByUserIDMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given deleteGroupMembershipByUserIDMessageEncoder: Encoder[DeleteGroupMembershipByUserIDMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given deleteGroupMembershipByUserIDMessageDecoder: Decoder[DeleteGroupMembershipByUserIDMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}


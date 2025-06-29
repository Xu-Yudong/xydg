package Objects.ChatService


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
import Objects.ChatService.GroupRole

/**
 * GroupMember
 * desc: 群成员信息，包括用户ID、角色和禁言截止时间。
 * @param userID: String (用户的唯一标识)
 * @param role: GroupRole:1036 (用户在群聊中的角色)
 * @param muteUntil: DateTime (禁言状态的截止时间)
 */

case class GroupMember(
  userID: String,
  role: GroupRole,
  muteUntil: Option[DateTime] = None
){

  //process class code 预留标志位，不要删除


}


case object GroupMember{

    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[GroupMember] = deriveEncoder
  private val circeDecoder: Decoder[GroupMember] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[GroupMember] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[GroupMember] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[GroupMember]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given groupMemberEncoder: Encoder[GroupMember] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given groupMemberDecoder: Decoder[GroupMember] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }



  //process object code 预留标志位，不要删除


}


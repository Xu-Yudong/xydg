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
import Objects.ChatService.GroupMember

/**
 * GroupChat
 * desc: 群组聊天信息，用于管理各个聊天群的信息
 * @param groupID: String (群组唯一标识)
 * @param groupName: String (群组名称)
 * @param creatorID: String (群主用户的唯一ID)
 * @param members: GroupMember:1046 (群成员列表信息)
 */

case class GroupChat(
  groupID: String,
  groupName: String,
  creatorID: String,
  members: List[GroupMember]
){

  //process class code 预留标志位，不要删除


}


case object GroupChat{

    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[GroupChat] = deriveEncoder
  private val circeDecoder: Decoder[GroupChat] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[GroupChat] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[GroupChat] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[GroupChat]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given groupChatEncoder: Encoder[GroupChat] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given groupChatDecoder: Decoder[GroupChat] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }



  //process object code 预留标志位，不要删除


}


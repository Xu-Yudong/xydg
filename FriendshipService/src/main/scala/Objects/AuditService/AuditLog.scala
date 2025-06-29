package Objects.AuditService


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
 * AuditLog
 * desc: 审计日志，记录操作行为以及相关信息
 * @param logID: String (日志的唯一标识)
 * @param action: String (操作的类型或行为)
 * @param targetID: String (操作目标的唯一标识)
 * @param performerID: String (执行操作人的唯一标识)
 * @param timestamp: DateTime (操作发生的时间戳)
 */

case class AuditLog(
  logID: String,
  action: String,
  targetID: String,
  performerID: String,
  timestamp: DateTime
){

  //process class code 预留标志位，不要删除


}


case object AuditLog{

    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[AuditLog] = deriveEncoder
  private val circeDecoder: Decoder[AuditLog] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[AuditLog] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[AuditLog] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[AuditLog]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given auditLogEncoder: Encoder[AuditLog] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given auditLogDecoder: Decoder[AuditLog] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }



  //process object code 预留标志位，不要删除


}


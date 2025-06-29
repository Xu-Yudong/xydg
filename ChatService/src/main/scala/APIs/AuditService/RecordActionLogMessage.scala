package APIs.AuditService

import Common.API.API
import Global.ServiceCenter.AuditServiceCode

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
 * RecordActionLogMessage
 * desc: 记录操作日志信息，返回记录成功提示。
 * @param action: String (操作的具体描述，例如'登录'、'删除记录'等动作。)
 * @param targetID: String (操作所针对的目标ID，例如某记录、文件或用户ID。)
 * @param performerID: String (执行操作人员的ID。)
 * @return result: String (操作日志记录的成功信息，例如'日志记录成功'。)
 */

case class RecordActionLogMessage(
  action: String,
  targetID: String,
  performerID: String
) extends API[String](AuditServiceCode)



case object RecordActionLogMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[RecordActionLogMessage] = deriveEncoder
  private val circeDecoder: Decoder[RecordActionLogMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[RecordActionLogMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[RecordActionLogMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[RecordActionLogMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given recordActionLogMessageEncoder: Encoder[RecordActionLogMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given recordActionLogMessageDecoder: Decoder[RecordActionLogMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}


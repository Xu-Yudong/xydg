package Objects.PaymentService


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
import Objects.PaymentService.ChangeType

/**
 * AmountChangeLog
 * desc: 表示用户余额变动的记录
 * @param logID: String (日志的唯一标识)
 * @param userID: String (用户的唯一标识)
 * @param amount: Double (发生的金额变动)
 * @param changeType: ChangeType:1042 (余额变动的类型)
 * @param timestamp: DateTime (变动发生的时间戳)
 */

case class AmountChangeLog(
  logID: String,
  userID: String,
  amount: Double,
  changeType: ChangeType,
  timestamp: DateTime
){

  //process class code 预留标志位，不要删除


}


case object AmountChangeLog{

    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[AmountChangeLog] = deriveEncoder
  private val circeDecoder: Decoder[AmountChangeLog] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[AmountChangeLog] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[AmountChangeLog] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[AmountChangeLog]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given amountChangeLogEncoder: Encoder[AmountChangeLog] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given amountChangeLogDecoder: Decoder[AmountChangeLog] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }



  //process object code 预留标志位，不要删除


}


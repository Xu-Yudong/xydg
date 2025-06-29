package APIs.PaymentService

import Common.API.API
import Global.ServiceCenter.PaymentServiceCode

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
 * TransferInGroupMessage
 * desc: 系统验证sessionToken，完成群聊内用户之间的转账交易，并返回成功信息。
 * @param sessionToken: String (用户登录会话的凭证，用于验证用户身份。)
 * @param groupMemberID: String (群成员ID，用于标识目标接收转账的用户。)
 * @param amount: Double (需要转账的金额。)
 * @return result: String (转账结果的消息，指示操作是否成功。)
 */

case class TransferInGroupMessage(
  sessionToken: String,
  groupMemberID: String,
  amount: Double
) extends API[String](PaymentServiceCode)



case object TransferInGroupMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[TransferInGroupMessage] = deriveEncoder
  private val circeDecoder: Decoder[TransferInGroupMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[TransferInGroupMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[TransferInGroupMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[TransferInGroupMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given transferInGroupMessageEncoder: Encoder[TransferInGroupMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given transferInGroupMessageDecoder: Decoder[TransferInGroupMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}


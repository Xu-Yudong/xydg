package Objects.PaymentService

import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}
import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind.{DeserializationContext, JsonDeserializer, JsonSerializer, SerializerProvider}
import io.circe.{Decoder, Encoder}

@JsonSerialize(`using` = classOf[ChangeTypeSerializer])
@JsonDeserialize(`using` = classOf[ChangeTypeDeserializer])
enum ChangeType(val desc: String):

  override def toString: String = this.desc

  case RedPacketSend extends ChangeType("发放红包") // 发放红包
  case RedPacketReceive extends ChangeType("领取红包") // 领取红包
  case TransactionSend extends ChangeType("转出金额") // 转出金额
  case TransactionReceive extends ChangeType("转入金额") // 转入金额
  case ProductPurchase extends ChangeType("商品购买") // 商品购买
  case ProductSale extends ChangeType("商品销售") // 商品销售


object ChangeType:
  given encode: Encoder[ChangeType] = Encoder.encodeString.contramap[ChangeType](toString)

  given decode: Decoder[ChangeType] = Decoder.decodeString.emap(fromStringEither)

  def fromString(s: String):ChangeType  = s match
    case "发放红包" => RedPacketSend
    case "领取红包" => RedPacketReceive
    case "转出金额" => TransactionSend
    case "转入金额" => TransactionReceive
    case "商品购买" => ProductPurchase
    case "商品销售" => ProductSale
    case _ => throw Exception(s"Unknown ChangeType: $s")

  def fromStringEither(s: String):Either[String, ChangeType]  = s match
    case "发放红包" => Right(RedPacketSend)
    case "领取红包" => Right(RedPacketReceive)
    case "转出金额" => Right(TransactionSend)
    case "转入金额" => Right(TransactionReceive)
    case "商品购买" => Right(ProductPurchase)
    case "商品销售" => Right(ProductSale)
    case _ => Left(s"Unknown ChangeType: $s")

  def toString(t: ChangeType): String = t match
    case RedPacketSend => "发放红包"
    case RedPacketReceive => "领取红包"
    case TransactionSend => "转出金额"
    case TransactionReceive => "转入金额"
    case ProductPurchase => "商品购买"
    case ProductSale => "商品销售"


// Jackson 序列化器
class ChangeTypeSerializer extends JsonSerializer[ChangeType] {
  override def serialize(value: ChangeType, gen: JsonGenerator, serializers: SerializerProvider): Unit = {
    gen.writeString(ChangeType.toString(value)) // 直接写出字符串
  }
}

// Jackson 反序列化器
class ChangeTypeDeserializer extends JsonDeserializer[ChangeType] {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): ChangeType = {
    ChangeType.fromString(p.getText)
  }
}


package Objects.ProductService

import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}
import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind.{DeserializationContext, JsonDeserializer, JsonSerializer, SerializerProvider}
import io.circe.{Decoder, Encoder}

@JsonSerialize(`using` = classOf[ProductStatusSerializer])
@JsonDeserialize(`using` = classOf[ProductStatusDeserializer])
enum ProductStatus(val desc: String):

  override def toString: String = this.desc

  case Pending extends ProductStatus("审核中的商品状态") // 审核中的商品状态
  case Available extends ProductStatus("商品上架中状态") // 商品上架中状态
  case Rejected extends ProductStatus("商品被拒绝状态") // 商品被拒绝状态
  case Removed extends ProductStatus("商品已下架状态") // 商品已下架状态


object ProductStatus:
  given encode: Encoder[ProductStatus] = Encoder.encodeString.contramap[ProductStatus](toString)

  given decode: Decoder[ProductStatus] = Decoder.decodeString.emap(fromStringEither)

  def fromString(s: String):ProductStatus  = s match
    case "审核中的商品状态" => Pending
    case "商品上架中状态" => Available
    case "商品被拒绝状态" => Rejected
    case "商品已下架状态" => Removed
    case _ => throw Exception(s"Unknown ProductStatus: $s")

  def fromStringEither(s: String):Either[String, ProductStatus]  = s match
    case "审核中的商品状态" => Right(Pending)
    case "商品上架中状态" => Right(Available)
    case "商品被拒绝状态" => Right(Rejected)
    case "商品已下架状态" => Right(Removed)
    case _ => Left(s"Unknown ProductStatus: $s")

  def toString(t: ProductStatus): String = t match
    case Pending => "审核中的商品状态"
    case Available => "商品上架中状态"
    case Rejected => "商品被拒绝状态"
    case Removed => "商品已下架状态"


// Jackson 序列化器
class ProductStatusSerializer extends JsonSerializer[ProductStatus] {
  override def serialize(value: ProductStatus, gen: JsonGenerator, serializers: SerializerProvider): Unit = {
    gen.writeString(ProductStatus.toString(value)) // 直接写出字符串
  }
}

// Jackson 反序列化器
class ProductStatusDeserializer extends JsonDeserializer[ProductStatus] {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): ProductStatus = {
    ProductStatus.fromString(p.getText)
  }
}


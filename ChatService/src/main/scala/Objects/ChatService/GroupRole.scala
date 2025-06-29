package Objects.ChatService

import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}
import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind.{DeserializationContext, JsonDeserializer, JsonSerializer, SerializerProvider}
import io.circe.{Decoder, Encoder}

@JsonSerialize(`using` = classOf[GroupRoleSerializer])
@JsonDeserialize(`using` = classOf[GroupRoleDeserializer])
enum GroupRole(val desc: String):

  override def toString: String = this.desc

  case Member extends GroupRole("群聊成员") // 群聊成员
  case Admin extends GroupRole("管理员") // 管理员
  case Owner extends GroupRole("群主") // 群主


object GroupRole:
  given encode: Encoder[GroupRole] = Encoder.encodeString.contramap[GroupRole](toString)

  given decode: Decoder[GroupRole] = Decoder.decodeString.emap(fromStringEither)

  def fromString(s: String):GroupRole  = s match
    case "群聊成员" => Member
    case "管理员" => Admin
    case "群主" => Owner
    case _ => throw Exception(s"Unknown GroupRole: $s")

  def fromStringEither(s: String):Either[String, GroupRole]  = s match
    case "群聊成员" => Right(Member)
    case "管理员" => Right(Admin)
    case "群主" => Right(Owner)
    case _ => Left(s"Unknown GroupRole: $s")

  def toString(t: GroupRole): String = t match
    case Member => "群聊成员"
    case Admin => "管理员"
    case Owner => "群主"


// Jackson 序列化器
class GroupRoleSerializer extends JsonSerializer[GroupRole] {
  override def serialize(value: GroupRole, gen: JsonGenerator, serializers: SerializerProvider): Unit = {
    gen.writeString(GroupRole.toString(value)) // 直接写出字符串
  }
}

// Jackson 反序列化器
class GroupRoleDeserializer extends JsonDeserializer[GroupRole] {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): GroupRole = {
    GroupRole.fromString(p.getText)
  }
}


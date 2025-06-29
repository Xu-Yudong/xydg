package Global

object ServiceCenter {
  val projectName: String = "xydg"
  val dbManagerServiceCode = "A000001"
  val tongWenDBServiceCode = "A000002"
  val tongWenServiceCode = "A000003"

  val ProductServiceCode = "A000010"
  val FriendshipServiceCode = "A000011"
  val ChatServiceCode = "A000012"
  val UserServiceCode = "A000013"
  val PaymentServiceCode = "A000014"
  val AuditServiceCode = "A000015"

  val fullNameMap: Map[String, String] = Map(
    tongWenDBServiceCode -> "DB-Manager（DB-Manager）",
    tongWenServiceCode -> "Tong-Wen（Tong-Wen）",
    ProductServiceCode -> "ProductService（ProductService)",
    FriendshipServiceCode -> "FriendshipService（FriendshipService)",
    ChatServiceCode -> "ChatService（ChatService)",
    UserServiceCode -> "UserService（UserService)",
    PaymentServiceCode -> "PaymentService（PaymentService)",
    AuditServiceCode -> "AuditService（AuditService)"
  )

  def serviceName(serviceCode: String): String = {
    fullNameMap(serviceCode).toLowerCase
  }
}

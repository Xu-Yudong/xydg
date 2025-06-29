/**
 * SubmitProductForApprovalMessage
 * desc: 提交商品上架申请
 * @param sessionToken: String (会话令牌，用于验证当前用户的登录状态)
 * @param product: Product:1130 (商品信息对象，包含商品ID、名称、描述、价格和状态等信息)
 * @return productID: String (生成的商品ID，唯一标识一个商品)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { Product } from 'Plugins/ProductService/Objects/Product';


export class SubmitProductForApprovalMessage extends TongWenMessage {
    constructor(
        public  sessionToken: string,
        public  product: Product
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10010"
    }
}


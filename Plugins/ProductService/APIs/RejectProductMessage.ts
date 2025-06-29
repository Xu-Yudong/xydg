/**
 * RejectProductMessage
 * desc: 审核用户拒绝商品上架，并记录拒绝原因。
 * @param reviewerToken: String (审核用户的身份令牌，用于验证权限。)
 * @param productID: String (待审核的商品ID，标识具体商品。)
 * @param reason: String (审核拒绝的原因，供记录使用。)
 * @return result: String (操作结果消息，如审核拒绝成功。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class RejectProductMessage extends TongWenMessage {
    constructor(
        public  reviewerToken: string,
        public  productID: string,
        public  reason: string
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10010"
    }
}


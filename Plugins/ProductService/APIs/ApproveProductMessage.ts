/**
 * ApproveProductMessage
 * desc: 审核用户批准商品上架。
 * @param reviewerToken: String (审核用户的会话令牌，用以验证操作权限。)
 * @param productID: String (待审核商品的唯一标识符。)
 * @return result: String (操作结果信息，表示审核是否成功。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class ApproveProductMessage extends TongWenMessage {
    constructor(
        public  reviewerToken: string,
        public  productID: string
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10010"
    }
}


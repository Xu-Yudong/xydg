/**
 * QueryBalanceMessage
 * desc: 查询用户余额信息
 * @param sessionToken: String (用户的登录会话令牌，用于验证用户身份)
 * @return balance: Double (用户账户当前的余额)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class QueryBalanceMessage extends TongWenMessage {
    constructor(
        public  sessionToken: string
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10013"
    }
}


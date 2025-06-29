/**
 * TransferToFriendMessage
 * desc: 用于处理好友转账的需求。通过验证sessionToken完成好友间的转账操作，并更新双方的账户金额。
 * @param sessionToken: String (用于验证用户身份的会话令牌。)
 * @param friendID: String (接收转账的好友用户ID。)
 * @param amount: Double (转账金额。)
 * @return result: String (操作结果，返回转账完成状态的消息。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class TransferToFriendMessage extends TongWenMessage {
    constructor(
        public  sessionToken: string,
        public  friendID: string,
        public  amount: number
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10014"
    }
}


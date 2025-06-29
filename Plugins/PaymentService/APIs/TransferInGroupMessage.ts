/**
 * TransferInGroupMessage
 * desc: 系统验证sessionToken，完成群聊内用户之间的转账交易，并返回成功信息。
 * @param sessionToken: String (用户登录会话的凭证，用于验证用户身份。)
 * @param groupMemberID: String (群成员ID，用于标识目标接收转账的用户。)
 * @param amount: Double (需要转账的金额。)
 * @return result: String (转账结果的消息，指示操作是否成功。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class TransferInGroupMessage extends TongWenMessage {
    constructor(
        public  sessionToken: string,
        public  groupMemberID: string,
        public  amount: number
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10014"
    }
}


/**
 * SendRedPacketMessage
 * desc: 系统验证sessionToken，扣除发红包人的账户金额，生成红包记录并返回红包ID。用于处理发送红包的需求。
 * @param sessionToken: String (用户的会话令牌，用于验证用户身份)
 * @param groupID: String (群聊的唯一标识符，用于指定红包所属的群)
 * @param amount: Double (红包总金额)
 * @param participantCount: Int (参与红包的用户数量)
 * @return redPacketID: String (生成的红包唯一标识符)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class SendRedPacketMessage extends TongWenMessage {
    constructor(
        public  sessionToken: string,
        public  groupID: string,
        public  amount: number,
        public  participantCount: number
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10014"
    }
}


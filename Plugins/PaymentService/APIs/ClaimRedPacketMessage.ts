/**
 * ClaimRedPacketMessage
 * desc: 系统验证sessionToken，用户领取红包后，更新账户余额及红包状态，返回领取金额。
 * @param sessionToken: String (用户会话令牌，用于验证用户身份。)
 * @param redPacketID: String (红包ID，用于唯一标识待领取的红包。)
 * @return receivedAmount: Double (领取的红包金额。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class ClaimRedPacketMessage extends TongWenMessage {
    constructor(
        public  sessionToken: string,
        public  redPacketID: string
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10014"
    }
}


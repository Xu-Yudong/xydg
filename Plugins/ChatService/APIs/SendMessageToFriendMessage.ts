/**
 * SendMessageToFriendMessage
 * desc: 好友聊天消息发送。
 * @param sessionToken: String (用户的会话令牌，用于验证身份。)
 * @param friendID: String (目标好友的用户ID。)
 * @param content: String (聊天消息的内容。)
 * @return result: String (操作结果信息，例如是否发送成功。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class SendMessageToFriendMessage extends TongWenMessage {
    constructor(
        public  sessionToken: string,
        public  friendID: string,
        public  content: string
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10012"
    }
}


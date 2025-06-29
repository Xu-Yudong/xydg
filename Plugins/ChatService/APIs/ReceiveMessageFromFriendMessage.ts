/**
 * ReceiveMessageFromFriendMessage
 * desc: 好友间接收未读消息，用于处理好友聊天消息接收的需求。
 * @param sessionToken: String (用户会话令牌，用于验证登录状态和用户身份。)
 * @param friendID: String (好友的用户ID，用于查询发送方的未读消息。)
 * @return messages: ChatMessage:1019 (好友发送的未读聊天消息列表。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class ReceiveMessageFromFriendMessage extends TongWenMessage {
    constructor(
        public  sessionToken: string,
        public  friendID: string
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10012"
    }
}


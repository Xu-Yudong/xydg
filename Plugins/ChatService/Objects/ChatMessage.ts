/**
 * ChatMessage
 * desc: 单聊聊天信息
 * @param messageID: String (消息的唯一标识)
 * @param senderID: String (消息发送人的用户标识)
 * @param receiverID: String (消息接收人的用户标识)
 * @param content: String (消息的内容)
 * @param timestamp: DateTime (消息发送的时间戳)
 */
import { Serializable } from 'Plugins/CommonUtils/Send/Serializable'




export class ChatMessage extends Serializable {
    constructor(
        public  messageID: string,
        public  senderID: string,
        public  receiverID: string,
        public  content: string,
        public  timestamp: number
    ) {
        super()
    }
}



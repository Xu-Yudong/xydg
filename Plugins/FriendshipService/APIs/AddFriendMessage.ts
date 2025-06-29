/**
 * AddFriendMessage
 * desc: 添加好友关系，用于处理添加好友的需求。
 * @param sessionToken: String (用户的会话令牌，用于识别和验证用户身份)
 * @param friendID: String (好友的用户ID，表示要添加为好友的目标用户)
 * @return result: String (添加好友操作的结果信息)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class AddFriendMessage extends TongWenMessage {
    constructor(
        public  sessionToken: string,
        public  friendID: string
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10011"
    }
}


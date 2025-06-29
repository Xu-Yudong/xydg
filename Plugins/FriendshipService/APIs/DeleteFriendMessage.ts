/**
 * DeleteFriendMessage
 * desc: 删除好友关系，用于处理删除好友的需求。
 * @param sessionToken: String (用户的会话令牌，用于身份验证。)
 * @param friendID: String (好友的用户ID，用于标识需要删除的好友关系。)
 * @return result: String (操作删除好友后的结果信息。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class DeleteFriendMessage extends TongWenMessage {
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


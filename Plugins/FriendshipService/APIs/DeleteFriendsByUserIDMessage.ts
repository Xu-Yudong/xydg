/**
 * DeleteFriendsByUserIDMessage
 * desc: 删除指定用户的所有好友关系，用于处理用户注销时清理用户的好友关联数据。
 * @param userID: String (用户ID，用于标识需要清理好友关系的用户。)
 * @return result: String (表示删除操作的结果信息，例如成功消息。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class DeleteFriendsByUserIDMessage extends TongWenMessage {
    constructor(
        public  userID: string
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10011"
    }
}


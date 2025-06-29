/**
 * DeleteMessagesByUserIDMessage
 * desc: 删除指定用户的所有聊天消息。
 * @param userID: String (用户唯一标识，指定要删除聊天记录的用户。)
 * @return result: String (操作结果信息，表示删除是否成功。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class DeleteMessagesByUserIDMessage extends TongWenMessage {
    constructor(
        public  userID: string
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10012"
    }
}


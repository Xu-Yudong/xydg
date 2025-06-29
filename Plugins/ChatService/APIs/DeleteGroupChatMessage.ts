/**
 * DeleteGroupChatMessage
 * desc: 删除群聊记录及所有相关成员信息
 * @param sessionToken: String (用户登录会话的令牌，用于验证用户身份)
 * @param groupID: String (群聊的唯一标识，用于定位具体群聊)
 * @return result: String (操作结果提示信息，表示群聊记录及相关成员信息删除的结果)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class DeleteGroupChatMessage extends TongWenMessage {
    constructor(
        public  sessionToken: string,
        public  groupID: string
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10012"
    }
}


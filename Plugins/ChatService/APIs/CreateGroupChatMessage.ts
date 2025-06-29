/**
 * CreateGroupChatMessage
 * desc: 创建群聊。
 * @param sessionToken: String (用户会话令牌，用于验证用户身份。)
 * @param groupName: String (群聊名称。)
 * @return groupID: String (生成的群聊唯一标识。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class CreateGroupChatMessage extends TongWenMessage {
    constructor(
        public  sessionToken: string,
        public  groupName: string
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10012"
    }
}


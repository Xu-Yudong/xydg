/**
 * DeleteUserMessage
 * desc: 用户注销，删除用户及用户相关的数据，返回注销成功信息。
 * @param sessionToken: String (表示用户会话的唯一标识，用于验证和标识当前操作的用户。)
 * @return result: String (用户注销操作的结果信息。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class DeleteUserMessage extends TongWenMessage {
    constructor(
        public  sessionToken: string
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10013"
    }
}


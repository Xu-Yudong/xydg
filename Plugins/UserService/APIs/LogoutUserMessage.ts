/**
 * LogoutUserMessage
 * desc: 用户登出，销毁登录会话，返回登出成功信息。
 * @param sessionToken: String (用户登录会话的令牌，用于标识当前用户会话。)
 * @return result: String (登出操作的结果信息，通常为成功消息。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class LogoutUserMessage extends TongWenMessage {
    constructor(
        public  sessionToken: string
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10013"
    }
}


/**
 * LoginUserMessage
 * desc: 用户登录，验证用户名和密码，创建登录会话，返回sessionToken。
 * @param username: String (用户名，用于验证账户登录的身份)
 * @param password: String (用户密码，用于验证账户登录的身份)
 * @return sessionToken: String (登录会话的唯一标识，完成登录后分配给客户端，标识用户的登录状态)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class LoginUserMessage extends TongWenMessage {
    constructor(
        public  username: string,
        public  password: string
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10013"
    }
}


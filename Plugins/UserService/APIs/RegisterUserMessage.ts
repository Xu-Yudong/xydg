/**
 * RegisterUserMessage
 * desc: 用户注册，生成唯一的userID，创建用户记录并返回userID。
 * @param username: String (用户名，用于标识和登录用户。)
 * @param password: String (密码，用于用户登录和验证。)
 * @param role: UserRole:1132 (用户角色，例如普通用户或商家。)
 * @return userID: String (新创建用户的唯一标识。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { UserRole } from 'Plugins/UserService/Objects/UserRole';


export class RegisterUserMessage extends TongWenMessage {
    constructor(
        public  username: string,
        public  password: string,
        public  role: UserRole
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10013"
    }
}


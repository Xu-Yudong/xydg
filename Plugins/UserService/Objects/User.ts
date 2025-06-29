/**
 * User
 * desc: 用户信息，包含基本身份属性和账户余额
 * @param userID: String (用户的唯一ID)
 * @param username: String (用户名)
 * @param password: String (账户密码)
 * @param role: UserRole:1132 (用户角色)
 * @param balance: Double (用户账户余额)
 */
import { Serializable } from 'Plugins/CommonUtils/Send/Serializable'

import { UserRole } from 'Plugins/UserService/Objects/UserRole';


export class User extends Serializable {
    constructor(
        public  userID: string,
        public  username: string,
        public  password: string,
        public  role: UserRole,
        public  balance: number
    ) {
        super()
    }
}



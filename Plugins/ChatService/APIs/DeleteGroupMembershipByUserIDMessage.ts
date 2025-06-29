/**
 * DeleteGroupMembershipByUserIDMessage
 * desc: 移除指定用户的群聊成员角色，用于处理用户注销时清理群聊用户关系的需求。
 * @param userID: String (用户ID，表示需要移除群成员角色的目标用户。)
 * @return result: String (操作执行结果信息，返回是否成功。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class DeleteGroupMembershipByUserIDMessage extends TongWenMessage {
    constructor(
        public  userID: string
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10012"
    }
}


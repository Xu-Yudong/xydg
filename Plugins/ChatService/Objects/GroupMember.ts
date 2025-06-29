/**
 * GroupMember
 * desc: 群成员信息，包括用户ID、角色和禁言截止时间。
 * @param userID: String (用户的唯一标识)
 * @param role: GroupRole:1036 (用户在群聊中的角色)
 * @param muteUntil: DateTime (禁言状态的截止时间)
 */
import { Serializable } from 'Plugins/CommonUtils/Send/Serializable'

import { GroupRole } from 'Plugins/ChatService/Objects/GroupRole';


export class GroupMember extends Serializable {
    constructor(
        public  userID: string,
        public  role: GroupRole,
        public  muteUntil: number | null
    ) {
        super()
    }
}



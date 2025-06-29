/**
 * GroupChat
 * desc: 群组聊天信息，用于管理各个聊天群的信息
 * @param groupID: String (群组唯一标识)
 * @param groupName: String (群组名称)
 * @param creatorID: String (群主用户的唯一ID)
 * @param members: GroupMember:1046 (群成员列表信息)
 */
import { Serializable } from 'Plugins/CommonUtils/Send/Serializable'

import { GroupMember } from 'Plugins/ChatService/Objects/GroupMember';


export class GroupChat extends Serializable {
    constructor(
        public  groupID: string,
        public  groupName: string,
        public  creatorID: string,
        public  members: GroupMember[]
    ) {
        super()
    }
}



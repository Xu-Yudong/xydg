/**
 * FriendRelation
 * desc: 描述好友关系的实体
 * @param userID: String (用户ID，唯一标识一个用户)
 * @param friendID: String (好友的用户ID)
 */
import { Serializable } from 'Plugins/CommonUtils/Send/Serializable'




export class FriendRelation extends Serializable {
    constructor(
        public  userID: string,
        public  friendID: string
    ) {
        super()
    }
}



/**
 * AuditLog
 * desc: 审计日志，记录操作行为以及相关信息
 * @param logID: String (日志的唯一标识)
 * @param action: String (操作的类型或行为)
 * @param targetID: String (操作目标的唯一标识)
 * @param performerID: String (执行操作人的唯一标识)
 * @param timestamp: DateTime (操作发生的时间戳)
 */
import { Serializable } from 'Plugins/CommonUtils/Send/Serializable'




export class AuditLog extends Serializable {
    constructor(
        public  logID: string,
        public  action: string,
        public  targetID: string,
        public  performerID: string,
        public  timestamp: number
    ) {
        super()
    }
}



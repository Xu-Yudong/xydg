/**
 * AmountChangeLog
 * desc: 表示用户余额变动的记录
 * @param logID: String (日志的唯一标识)
 * @param userID: String (用户的唯一标识)
 * @param amount: Double (发生的金额变动)
 * @param changeType: ChangeType:1042 (余额变动的类型)
 * @param timestamp: DateTime (变动发生的时间戳)
 */
import { Serializable } from 'Plugins/CommonUtils/Send/Serializable'

import { ChangeType } from 'Plugins/PaymentService/Objects/ChangeType';


export class AmountChangeLog extends Serializable {
    constructor(
        public  logID: string,
        public  userID: string,
        public  amount: number,
        public  changeType: ChangeType,
        public  timestamp: number
    ) {
        super()
    }
}



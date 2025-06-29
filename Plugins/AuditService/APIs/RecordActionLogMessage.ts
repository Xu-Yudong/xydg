/**
 * RecordActionLogMessage
 * desc: 记录操作日志信息，返回记录成功提示。
 * @param action: String (操作的具体描述，例如'登录'、'删除记录'等动作。)
 * @param targetID: String (操作所针对的目标ID，例如某记录、文件或用户ID。)
 * @param performerID: String (执行操作人员的ID。)
 * @return result: String (操作日志记录的成功信息，例如'日志记录成功'。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class RecordActionLogMessage extends TongWenMessage {
    constructor(
        public  action: string,
        public  targetID: string,
        public  performerID: string
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10015"
    }
}


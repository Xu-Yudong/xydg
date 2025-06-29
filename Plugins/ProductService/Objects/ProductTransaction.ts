/**
 * ProductTransaction
 * desc: 商品交易记录包含买家、卖家以及交易金额的信息。
 * @param transactionID: String (交易记录的唯一标识ID)
 * @param productID: String (商品的唯一标识ID)
 * @param buyerID: String (购买者的唯一标识ID)
 * @param sellerID: String (出售者的唯一标识ID)
 * @param amount: Double (交易金额)
 * @param timestamp: DateTime (交易发生的时间戳)
 */
import { Serializable } from 'Plugins/CommonUtils/Send/Serializable'




export class ProductTransaction extends Serializable {
    constructor(
        public  transactionID: string,
        public  productID: string,
        public  buyerID: string,
        public  sellerID: string,
        public  amount: number,
        public  timestamp: number
    ) {
        super()
    }
}



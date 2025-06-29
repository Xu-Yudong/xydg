/**
 * Product
 * desc: 商品的信息，包含名称、描述、价格等关键属性
 * @param productID: String (商品的唯一标识)
 * @param name: String (商品名称)
 * @param description: String (商品描述信息)
 * @param price: Double (商品价格)
 * @param status: ProductStatus:1117 (商品当前状态)
 * @param sellerID: String (卖家ID)
 */
import { Serializable } from 'Plugins/CommonUtils/Send/Serializable'

import { ProductStatus } from 'Plugins/ProductService/Objects/ProductStatus';


export class Product extends Serializable {
    constructor(
        public  productID: string,
        public  name: string,
        public  description: string,
        public  price: number,
        public  status: ProductStatus,
        public  sellerID: string
    ) {
        super()
    }
}



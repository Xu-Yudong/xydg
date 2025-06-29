export enum ChangeType {
    redPacketSend = '发放红包',
    redPacketReceive = '领取红包',
    transactionSend = '转出金额',
    transactionReceive = '转入金额',
    productPurchase = '商品购买',
    productSale = '商品销售'
}

export const changeTypeList = Object.values(ChangeType)

export function getChangeType(newType: string): ChangeType {
    return changeTypeList.filter(t => t === newType)[0]
}

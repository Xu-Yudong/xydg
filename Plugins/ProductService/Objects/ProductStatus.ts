export enum ProductStatus {
    pending = '审核中的商品状态',
    available = '商品上架中状态',
    rejected = '商品被拒绝状态',
    removed = '商品已下架状态'
}

export const productStatusList = Object.values(ProductStatus)

export function getProductStatus(newType: string): ProductStatus {
    return productStatusList.filter(t => t === newType)[0]
}

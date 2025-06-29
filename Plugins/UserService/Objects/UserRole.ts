export enum UserRole {
    normal = '普通用户',
    business = '商家用户',
    reviewer = '审核用户'
}

export const userRoleList = Object.values(UserRole)

export function getUserRole(newType: string): UserRole {
    return userRoleList.filter(t => t === newType)[0]
}

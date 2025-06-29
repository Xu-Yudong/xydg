export enum GroupRole {
    member = '群聊成员',
    admin = '管理员',
    owner = '群主'
}

export const groupRoleList = Object.values(GroupRole)

export function getGroupRole(newType: string): GroupRole {
    return groupRoleList.filter(t => t === newType)[0]
}

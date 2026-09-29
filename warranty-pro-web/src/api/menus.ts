import request from './request'

export interface MenuNode {
  id: number
  parentId: number
  name: string
  path: string
  icon: string
  type: 'DIR' | 'MENU'
  sortOrder: number
  visible: number
  status: number
  children: MenuNode[]
}

export interface MenuInput {
  parentId: number
  name: string
  path: string
  icon: string
  type: 'DIR' | 'MENU'
  sortOrder: number
  visible: number
  status: number
}

export async function fetchMenuTree(): Promise<MenuNode[]> {
  return (await request.get('/admin/menus')) as unknown as MenuNode[]
}

export async function createMenu(input: MenuInput): Promise<void> {
  await request.post('/admin/menus', input)
}

export async function updateMenu(id: number, input: MenuInput): Promise<void> {
  await request.put(`/admin/menus/${id}`, input)
}

export async function deleteMenu(id: number): Promise<void> {
  await request.delete(`/admin/menus/${id}`)
}

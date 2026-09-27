import request from './request'

export interface LoginPayload {
  username: string
  password: string
}

export interface UserInfo {
  id: number
  username: string
  phone: string | null
  realName: string
  roles: string[]
}

export interface LoginResult {
  accessToken: string
  refreshToken: string
  user: UserInfo
}

export async function loginApi(payload: LoginPayload): Promise<LoginResult> {
  return (await request.post('/auth/login', payload)) as unknown as LoginResult
}

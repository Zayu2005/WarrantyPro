import request from './request'

export interface LoginPayload {
  phone: string
  password: string
}

export interface UserInfo {
  id: number
  phone: string
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

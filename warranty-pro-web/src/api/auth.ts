import request from './request'

export interface LoginPayload {
  username: string
  password: string
  challengeId: string
}

export interface SliderCaptcha {
  challengeId: string
  backgroundImage: string
  pieceImage: string
  imageWidth: number
  imageHeight: number
  pieceY: number
  pieceSize: number
  trackWidth: number
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

export async function fetchSliderCaptcha(): Promise<SliderCaptcha> {
  return (await request.get('/auth/slider-captcha')) as unknown as SliderCaptcha
}

export async function verifySliderCaptcha(
  challengeId: string,
  sliderOffset: number,
  trajectory?: string,
): Promise<void> {
  await request.post('/auth/slider-captcha/verify', {
    challengeId,
    sliderOffset,
    ...(trajectory ? { trajectory } : {}),
  })
}

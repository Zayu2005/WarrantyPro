import { http } from '@kit.NetworkKit';

/**
 * 全局接口配置。
 * BASE_URL 填电脑的局域网 IPv4（cmd 里 ipconfig 查看），手机 / 模拟器需与电脑在同一 Wi-Fi。
 * 生产环境必须改为 HTTPS 域名。
 */
export class ApiConfig {
  static readonly BASE_URL: string = 'http://10.10.10.6:8080';
}

export class LoginUserInfo {
  id: number = 0;
  username: string = '';
  phone: string = '';
  realName: string = '';
  roles: string[] = [];
}

export class LoginData {
  accessToken: string = '';
  refreshToken: string = '';
  user: LoginUserInfo = new LoginUserInfo();
}

/** 后端统一响应体（docs/07 §1：{ code, message, data }） */
export class ApiResult {
  code: number = -1;
  message: string = '';
  data: LoginData | null = null;
}

class LoginBody {
  username: string = '';
  password: string = '';
}

/** 用户名密码登录（POST /api/v1/auth/login）。 */
export async function loginByPassword(username: string, password: string): Promise<ApiResult> {
  const client = http.createHttp();
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  const body: LoginBody = new LoginBody();
  body.username = username;
  body.password = password;
  try {
    const resp = await client.request(`${ApiConfig.BASE_URL}/api/v1/auth/login`, {
      method: http.RequestMethod.POST,
      header: headers,
      extraData: JSON.stringify(body),
      expectDataType: http.HttpDataType.STRING,
      connectTimeout: 8000,
      readTimeout: 8000
    });
    return JSON.parse(resp.result as string) as ApiResult;
  } finally {
    client.destroy();
  }
}

import { http } from '@kit.NetworkKit';

/**
 * 全局接口配置。
 * BASE_URL 填电脑的局域网 IPv4（cmd 里 ipconfig 查看），手机 / 模拟器需与电脑在同一 Wi-Fi。
 * 生产环境必须改为 HTTPS 域名。
 */
export class ApiConfig {
  static readonly BASE_URL: string = 'http://10.10.10.6:8080';
}

// ==================== 类型（与后端 docs/07 契约对齐） ====================

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

export class ApiResult {
  code: number = -1;
  message: string = '';
  data: LoginData | null = null;
}

export class OrderItem {
  id: number = 0;
  orderNo: string = '';
  objectType: string = '';
  category: string = '';
  locationDetail: string = '';
  phenomenon: string = '';
  urgency: string = 'NORMAL';
  status: string = '';
  verdict: string = '';
  responsibleParty: string = '';
  submittedAt: string = '';
  createdAt: string = '';
}

export class OrderPageData {
  list: OrderItem[] = [];
  total: number = 0;
}

export class OrderPageResult {
  code: number = -1;
  message: string = '';
  data: OrderPageData | null = null;
}

export class HouseItem {
  houseId: number = 0;
  label: string = '';
}

export class HouseListResult {
  code: number = -1;
  message: string = '';
  data: HouseItem[] | null = null;
}

export class VerdictInfo {
  verdict: string = '';
  responsibleParty: string = '';
  basis: string = '';
  warrantyExpire: string = '';
}

export class OrderCreatedData {
  orderId: number = 0;
  orderNo: string = '';
  status: string = '';
  verdict: VerdictInfo = new VerdictInfo();
}

export class OrderCreateResult {
  code: number = -1;
  message: string = '';
  data: OrderCreatedData | null = null;
}

class LoginBody {
  username: string = '';
  password: string = '';
}

class CreateOrderBody {
  objectType: string = 'INDOOR';
  category: string = '';
  houseId: number = 0;
  locationDetail: string = '';
  phenomenon: string = '';
  urgency: string = 'NORMAL';
}

// ==================== 请求 ====================

async function getJSON(path: string, token: string): Promise<string> {
  const client = http.createHttp();
  const headers: Record<string, string> = { 'Authorization': `Bearer ${token}` };
  try {
    const resp = await client.request(`${ApiConfig.BASE_URL}${path}`, {
      method: http.RequestMethod.GET,
      header: headers,
      expectDataType: http.HttpDataType.STRING,
      connectTimeout: 8000,
      readTimeout: 8000
    });
    return resp.result as string;
  } finally {
    client.destroy();
  }
}

async function postJSON(path: string, body: object, token: string): Promise<string> {
  const client = http.createHttp();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  };
  try {
    const resp = await client.request(`${ApiConfig.BASE_URL}${path}`, {
      method: http.RequestMethod.POST,
      header: headers,
      extraData: JSON.stringify(body),
      expectDataType: http.HttpDataType.STRING,
      connectTimeout: 8000,
      readTimeout: 8000
    });
    return resp.result as string;
  } finally {
    client.destroy();
  }
}

/** 用户名密码登录（POST /api/v1/auth/login）。 */
export async function loginByPassword(username: string, password: string): Promise<ApiResult> {
  const body: LoginBody = new LoginBody();
  body.username = username;
  body.password = password;
  return JSON.parse(await postJSON('/api/v1/auth/login', body, '')) as ApiResult;
}

/** 我的工单（业主视角，GET /api/v1/orders/my）。 */
export async function fetchMyOrders(token: string): Promise<OrderPageResult> {
  return JSON.parse(await getJSON('/api/v1/orders/my?page=1&pageSize=10', token)) as OrderPageResult;
}

/** 师傅待办（GET /api/v1/worker/orders）。 */
export async function fetchWorkerOrders(token: string): Promise<OrderPageResult> {
  return JSON.parse(await getJSON('/api/v1/worker/orders?page=1&pageSize=20', token)) as OrderPageResult;
}

/** 我的房屋（GET /api/v1/houses/my，报修表单选房）。 */
export async function fetchMyHouses(token: string): Promise<HouseListResult> {
  return JSON.parse(await getJSON('/api/v1/houses/my', token)) as HouseListResult;
}

/** 提交表单报修（POST /api/v1/orders，含保修判定）。 */
export async function createOrder(token: string, category: string, houseId: number,
  locationDetail: string, phenomenon: string, urgency: string): Promise<OrderCreateResult> {
  const body: CreateOrderBody = new CreateOrderBody();
  body.category = category;
  body.houseId = houseId;
  body.locationDetail = locationDetail;
  body.phenomenon = phenomenon;
  body.urgency = urgency;
  return JSON.parse(await postJSON('/api/v1/orders', body, token)) as OrderCreateResult;
}

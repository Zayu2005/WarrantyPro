import { http } from '@kit.NetworkKit';

/**
 * 全局接口配置。
 * BASE_URL 按运行环境选择：
 *  - DevEco 预览器 / 本机模拟器（共享本机网络）：http://localhost:8080
 *  - 真机 / 远程设备：填电脑的局域网 IPv4（cmd 里 ipconfig 查看），并确保同一 Wi-Fi
 * 生产环境必须改为 HTTPS 域名。
 */
export class ApiConfig {
  static readonly BASE_URL: string = 'http://localhost:8080';
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

export class SliderCaptchaData {
  challengeId: string = '';
  backgroundImage: string = '';
  pieceImage: string = '';
  imageWidth: number = 260;
  imageHeight: number = 130;
  pieceY: number = 0;
  pieceSize: number = 44;
  trackWidth: number = 208;
}

export class SliderCaptchaResult {
  code: number = -1;
  message: string = '';
  data: SliderCaptchaData | null = null;
}

export class ApiResult {
  code: number = -1;
  message: string = '';
  data: LoginData | null = null;
}

export class FlowItem {
  id: number = 0;
  fromStatus: string = '';
  toStatus: string = '';
  operatorRole: string = '';
  action: string = '';
  remark: string = '';
  createdAt: string = '';
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
  verdictBasis: string = '';
  warrantyExpire: string = '';
  submittedAt: string = '';
  createdAt: string = '';
  flows: FlowItem[] | null = null;
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

export class OrderDetailResult {
  code: number = -1;
  message: string = '';
  data: OrderItem | null = null;
}

export class NoticeItem {
  id: number = 0;
  title: string = '';
  content: string = '';
  type: string = '';
  createdAt: string = '';
}

export class NoticeListResult {
  code: number = -1;
  message: string = '';
  data: NoticeItem[] | null = null;
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
  challengeId: string = '';
}

class SliderCaptchaVerifyBody {
  challengeId: string = '';
  sliderOffset: number = 0;
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

async function putJSON(path: string, body: object, token: string): Promise<string> {
  const client = http.createHttp();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  };
  try {
    const resp = await client.request(`${ApiConfig.BASE_URL}${path}`, {
      method: http.RequestMethod.PUT,
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

/** 获取一次性滑动拼图验证码（GET /api/v1/auth/slider-captcha）。 */
export async function fetchSliderCaptcha(): Promise<SliderCaptchaResult> {
  return JSON.parse(await getJSON('/api/v1/auth/slider-captcha', '')) as SliderCaptchaResult;
}

/** 校验滑动验证码（POST /api/v1/auth/slider-captcha/verify）。 */
export async function verifySliderCaptcha(challengeId: string, sliderOffset: number): Promise<WorkerActionResult> {
  const body: SliderCaptchaVerifyBody = new SliderCaptchaVerifyBody();
  body.challengeId = challengeId;
  body.sliderOffset = sliderOffset;
  return JSON.parse(await postJSON('/api/v1/auth/slider-captcha/verify', body, '')) as WorkerActionResult;
}

/** 用户名密码登录并消费已校验的一次性挑战。 */
export async function loginByPassword(username: string, password: string,
  challengeId: string): Promise<ApiResult> {
  const body: LoginBody = new LoginBody();
  body.username = username;
  body.password = password;
  body.challengeId = challengeId;
  return JSON.parse(await postJSON('/api/v1/auth/login', body, '')) as ApiResult;
}

/** 我的工单（业主视角，GET /api/v1/orders/my）。 */
export async function fetchMyOrders(token: string): Promise<OrderPageResult> {
  return JSON.parse(await getJSON('/api/v1/orders/my?page=1&pageSize=50', token)) as OrderPageResult;
}

/** 师傅工单（GET /api/v1/worker/orders，history=true 含已完结）。 */
export async function fetchWorkerOrders(token: string, history: boolean): Promise<OrderPageResult> {
  const path = `/api/v1/worker/orders?history=${history}&page=1&pageSize=50`;
  return JSON.parse(await getJSON(path, token)) as OrderPageResult;
}

/** 工单详情（含流转时间轴，GET /api/v1/orders/{id}）。 */
export async function fetchOrderDetail(token: string, orderId: number): Promise<OrderDetailResult> {
  return JSON.parse(await getJSON(`/api/v1/orders/${orderId}`, token)) as OrderDetailResult;
}

/** 物业公告（GET /api/v1/notices）。 */
export async function fetchNotices(token: string): Promise<NoticeListResult> {
  return JSON.parse(await getJSON('/api/v1/notices', token)) as NoticeListResult;
}

/** 我的房屋（GET /api/v1/houses/my，报修表单选房）。 */
export async function fetchMyHouses(token: string): Promise<HouseListResult> {
  return JSON.parse(await getJSON('/api/v1/houses/my', token)) as HouseListResult;
}

/** 提交表单报修（POST /api/v1/orders，统一进入派单流程）。 */
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

class EmptyBody {
}

class ConfirmOrderBody {
  pass: boolean = false;
  reason: string = '';
}

class EvaluationBody {
  stars: number = 5;
  tags: string[] = [];
  comment: string = '';
}

/** 业主验收（POST /api/v1/orders/{id}/confirm）。 */
export async function confirmOrder(token: string, orderId: number,
  pass: boolean, reason: string = ''): Promise<ApiResult> {
  const body: ConfirmOrderBody = new ConfirmOrderBody();
  body.pass = pass;
  body.reason = reason;
  return JSON.parse(await postJSON(`/api/v1/orders/${orderId}/confirm`, body, token)) as ApiResult;
}

/** 业主评价（POST /api/v1/orders/{id}/evaluation）。 */
export async function evaluateOrder(token: string, orderId: number,
  stars: number, comment: string): Promise<WorkerActionResult> {
  const body: EvaluationBody = new EvaluationBody();
  body.stars = stars;
  body.tags = stars >= 4 ? ['速度快', '一次修复'] : ['需要改进'];
  body.comment = comment;
  return JSON.parse(await postJSON(`/api/v1/orders/${orderId}/evaluation`, body, token)) as WorkerActionResult;
}

/** 师傅到场打卡（POST /api/v1/worker/orders/{id}/arrive）。 */
export async function arriveOrder(token: string, orderId: number): Promise<ApiResult> {
  return JSON.parse(await postJSON(`/api/v1/worker/orders/${orderId}/arrive`, new EmptyBody(), token)) as ApiResult;
}

class CompleteOrderBody {
  faultCause: string = '';
  measures: string = '';
  workHours: number = 0;
}

export class WorkerActionResult {
  code: number = -1;
  message: string = '';
}

class UpdateProfileBody {
  realName: string = '';
  phone: string = '';
}

class ChangePasswordBody {
  oldPassword: string = '';
  newPassword: string = '';
}

/** 修改基本资料（PUT /api/v1/users/me）。 */
export async function updateProfile(token: string, realName: string, phone: string): Promise<WorkerActionResult> {
  const body: UpdateProfileBody = new UpdateProfileBody();
  body.realName = realName;
  body.phone = phone;
  return JSON.parse(await putJSON('/api/v1/users/me', body, token)) as WorkerActionResult;
}

/** 修改密码（PUT /api/v1/users/me/password）。 */
export async function changePassword(token: string, oldPassword: string,
  newPassword: string): Promise<WorkerActionResult> {
  const body: ChangePasswordBody = new ChangePasswordBody();
  body.oldPassword = oldPassword;
  body.newPassword = newPassword;
  return JSON.parse(await putJSON('/api/v1/users/me/password', body, token)) as WorkerActionResult;
}

/** 师傅完工提交（POST /api/v1/worker/orders/{id}/complete）。 */
export async function completeOrder(token: string, orderId: number,
  faultCause: string, measures: string, workHours: number): Promise<WorkerActionResult> {
  const body: CompleteOrderBody = new CompleteOrderBody();
  body.faultCause = faultCause;
  body.measures = measures;
  body.workHours = workHours;
  return JSON.parse(await postJSON(`/api/v1/worker/orders/${orderId}/complete`, body, token)) as WorkerActionResult;
}

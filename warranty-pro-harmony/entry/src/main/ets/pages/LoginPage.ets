import { router } from '@kit.ArkUI';
import { preferences } from '@kit.ArkData';
import { loginByPassword, LoginData } from '../common/ApiClient';

/**
 * 登录页。
 * 视觉签名：工单进度条（提交—派单—维修—验收），"维修"节点琥珀脉冲——
 * 进度透明是产品的核心承诺，把它做进登录页本身（frontend-design 方案）。
 */
@Entry
@Component
struct LoginPage {
  @State username: string = '';
  @State password: string = '';
  @State remember: boolean = true;
  @State loading: boolean = false;
  @State pulse: number = 1;

  private readonly C_INK: string = '#103B3A';    // 松青 · 主色
  private readonly C_MOSS: string = '#2E6D67';   // 苔青 · 次级
  private readonly C_AMBER: string = '#E8930C';  // 琥珀 · 签名强调
  private readonly C_PAPER: string = '#F2F5F3';  // 纸灰绿 · 页面底
  private readonly C_MIST: string = '#DCE7E3';   // 雾青 · 分隔
  private readonly C_MUTED: string = '#8A968F';  // 弱文字
  private readonly nodeLabels: string[] = ['提交', '派单', '维修', '验收'];

  aboutToAppear(): void {
    this.loadSavedUsername();
    this.startPulse();
  }

  /** 统一 Toast 出口（API 12 UIContext 写法）。 */
  private toast(message: string): void {
    this.getUIContext().getPromptAction().showToast({ message: message });
  }

  /** 本地键值存储（记住用户名 / 会话令牌）。 */
  private async prefStore(name: string): Promise<preferences.Preferences> {
    const context = this.getUIContext().getHostContext();
    if (!context) {
      throw new Error('context unavailable');
    }
    return await preferences.getPreferences(context, name);
  }

  async loadSavedUsername(): Promise<void> {
    try {
      const store = await this.prefStore('wp_login');
      const saved = await store.get('saved_username', '') as string;
      if (saved !== '') {
        this.username = saved;
        this.remember = true;
      }
    } catch (err) {
      // 首次启动还没有本地存储，忽略
    }
  }

  private startPulse(): void {
    this.getUIContext().animateTo({
      duration: 1100,
      curve: Curve.EaseInOut,
      iterations: -1,
      playMode: PlayMode.Alternate
    }, () => {
      this.pulse = 1.35;
    });
  }

  private roleLabel(role: string): string {
    if (role === 'OWNER') {
      return '业主';
    }
    if (role === 'WORKER') {
      return '维修师傅';
    }
    return role;
  }

  private async onLogin(): Promise<void> {
    if (this.loading) {
      return;
    }
    if (this.username.trim() === '' || this.password === '') {
      this.toast('请输入用户名和密码');
      return;
    }
    this.loading = true;
    try {
      const result = await loginByPassword(this.username.trim(), this.password);
      if (result.code !== 0 || result.data === null) {
        this.toast(result.message === '' ? '登录失败，请稍后再试' : result.message);
        return;
      }
      await this.saveSession(result.data);
      const store = await this.prefStore('wp_login');
      if (this.remember) {
        await store.put('saved_username', this.username.trim());
      } else {
        await store.delete('saved_username');
      }
      await store.flush();

      // 会话放入全局状态，按角色进入对应工作台
      AppStorage.setOrCreate('accessToken', result.data.accessToken);
      AppStorage.setOrCreate('realName', result.data.user.realName);
      let role: string = 'OWNER';
      if (result.data.user.roles.length > 0) {
        role = result.data.user.roles[0];
      }
      AppStorage.setOrCreate('role', role);
      router.replaceUrl({ url: 'pages/MainPage' });
    } catch (err) {
      this.toast('连接不上服务：请确认与电脑在同一 Wi-Fi，且 ApiClient.ets 里的 BASE_URL 正确');
    } finally {
      this.loading = false;
    }
  }

  private async saveSession(data: LoginData): Promise<void> {
    const store = await this.prefStore('wp_session');
    await store.put('accessToken', data.accessToken);
    await store.put('refreshToken', data.refreshToken);
    await store.put('roles', data.user.roles.join(','));
    await store.flush();
  }

  @Builder
  NodeDot(idx: number) {
    if (idx === 2) {
      // 签名节点："维修中" 琥珀呼吸脉冲（用 borderRadius 画圆，兼容低版本 SDK）
      Stack() {
        Row()
          .width(26)
          .height(26)
          .borderRadius(13)
          .backgroundColor(this.C_AMBER)
          .opacity(0.22)
          .scale({ x: this.pulse, y: this.pulse })
        Row()
          .width(12)
          .height(12)
          .borderRadius(6)
          .backgroundColor(this.C_AMBER)
      }
      .width(26)
      .height(26)
    } else if (idx < 2) {
      Row()
        .width(12)
        .height(12)
        .borderRadius(6)
        .backgroundColor(this.C_MOSS)
    } else {
      Row()
        .width(12)
        .height(12)
        .borderRadius(6)
        .backgroundColor(this.C_PAPER)
        .border({ width: 2, color: this.C_MIST })
    }
  }

  @Builder
  ProgressSignature() {
    Stack() {
      // 节点圆心位于 1/8、3/8、5/8、7/8 宽度处：连接线两端各留 1/8，前两段已达成
      Row() {
        Row()
          .height(2)
          .layoutWeight(2)
          .backgroundColor(this.C_MOSS)
          .margin({ left: '12.5%' })
        Row()
          .height(2)
          .layoutWeight(1)
          .backgroundColor(this.C_MIST)
          .margin({ right: '12.5%' })
      }
      .width('100%')

      Row() {
        ForEach(this.nodeLabels, (label: string, idx: number) => {
          Column({ space: 8 }) {
            this.NodeDot(idx)
            Text(label)
              .fontSize(11)
              .fontColor(idx === 2 ? this.C_AMBER : (idx < 2 ? this.C_MOSS : this.C_MUTED))
              .fontWeight(idx === 2 ? FontWeight.Medium : FontWeight.Regular)
          }
          .layoutWeight(1)
        }, (label: string) => label)
      }
      .width('100%')
    }
    .width('100%')
    .margin({ top: 34 })
  }

  @Builder
  DemoChip(roleText: string, demoUsername: string) {
    Text(`${roleText} · ${demoUsername}`)
      .fontSize(13)
      .fontColor(this.C_MOSS)
      .padding({ left: 14, right: 14, top: 8, bottom: 8 })
      .borderRadius(16)
      .border({ width: 1, color: this.C_MIST })
      .backgroundColor(Color.White)
      .onClick(() => {
        this.username = demoUsername;
        this.password = '123456';
        this.toast(`已填入${roleText}演示账号`);
      })
  }

  build() {
    Scroll() {
      Column() {
        // ---- 品牌区 ----
        Row({ space: 10 }) {
          Text('W')
            .fontSize(14)
            .fontWeight(FontWeight.Bold)
            .fontColor(Color.White)
            .width(26)
            .height(26)
            .textAlign(TextAlign.Center)
            .borderRadius(8)
            .backgroundColor(this.C_INK)
          Text('WarrantyPro')
            .fontSize(20)
            .fontWeight(FontWeight.Bold)
            .fontColor(this.C_INK)
        }
        .margin({ top: 56 })

        Text('数字化物业保修平台')
          .fontSize(13)
          .fontColor(this.C_MUTED)
          .margin({ top: 6 })

        // ---- 签名：工单进度条 ----
        this.ProgressSignature()

        Text('报修有进度，保修有依据')
          .fontSize(16)
          .fontWeight(FontWeight.Medium)
          .fontColor(this.C_INK)
          .letterSpacing(1)
          .margin({ top: 18 })

        // ---- 表单卡 ----
        Column() {
          Text('账 号 登 录')
              .fontSize(12)
              .fontColor(this.C_MOSS)
              .letterSpacing(3)
              .margin({ bottom: 16 })

            TextInput({ placeholder: '用户名', text: this.username })
              .fontSize(16)
              .fontColor(this.C_INK)
              .caretColor(this.C_INK)
              .placeholderColor(this.C_MUTED)
              .backgroundColor(Color.Transparent)
              .height(50)
              .onChange((value: string) => {
                this.username = value;
              })

            Divider()
              .color(this.C_MIST)
              .margin({ bottom: 18 })

            TextInput({ placeholder: '密码', text: this.password })
              .type(InputType.Password)
              .fontSize(16)
              .fontColor(this.C_INK)
              .caretColor(this.C_INK)
              .placeholderColor(this.C_MUTED)
              .backgroundColor(Color.Transparent)
              .height(50)
              .onChange((value: string) => {
                this.password = value;
              })

            Divider()
              .color(this.C_MIST)
              .margin({ bottom: 20 })

            Row() {
              Row({ space: 6 }) {
                Checkbox()
                  .select(this.remember)
                  .selectedColor(this.C_INK)
                  .width(16)
                  .height(16)
                  .onChange((value: boolean) => {
                    this.remember = value;
                  })
                Text('记住用户名')
                  .fontSize(13)
                  .fontColor(this.C_MUTED)
              }
              .onClick(() => {
                this.remember = !this.remember;
              })

              Blank()

              Text('忘记密码？')
                .fontSize(13)
                .fontColor(this.C_MOSS)
                .onClick(() => {
                  this.toast('请联系管理员重置密码');
                })
            }
            .width('100%')
            .margin({ bottom: 26 })

            Button(this.loading ? '正在登录' : '登录')
              .width('100%')
              .height(50)
              .borderRadius(25)
              .backgroundColor(this.C_INK)
              .fontColor(Color.White)
              .fontSize(17)
              .fontWeight(FontWeight.Medium)
              .enabled(!this.loading)
              .onClick(() => {
                this.onLogin();
              })
        }
        .width('100%')
        .padding(22)
        .borderRadius(20)
        .backgroundColor(Color.White)
        .margin({ top: 30 })

        // ---- 演示账号 chips ----
        Row({ space: 10 }) {
          this.DemoChip('业主', 'owner')
          this.DemoChip('客服', 'kefu')
          this.DemoChip('师傅', 'shifu')
        }
        .margin({ top: 20 })

        Text('点一点即可填入演示账号（密码 123456）')
          .fontSize(11)
          .fontColor(this.C_MUTED)
          .margin({ top: 10 })

        Blank()
          .height(26)

        Text('客服与管理员请使用 PC 管理后台')
          .fontSize(12)
          .fontColor(this.C_MUTED)
      }
      .width('100%')
      .padding({ left: 28, right: 28, bottom: 20 })
    }
    .width('100%')
    .height('100%')
    .backgroundColor(this.C_PAPER)
    .scrollBar(BarState.Off)
    .edgeEffect(EdgeEffect.Spring)
  }
}

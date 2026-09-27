import { router, promptAction } from '@kit.ArkUI';
import { preferences } from '@kit.ArkData';
import { fetchMyHouses, fetchWorkerOrders, fetchMyOrders, HouseItem, OrderItem } from '../common/ApiClient';

/** 我的 Tab：用户卡 + 角色化信息（业主房屋 / 师傅统计）+ 退出登录。 */
@Component
export struct ProfilePage {
  @State realName: string = (AppStorage.get<string>('realName') ?? '');
  @State role: string = (AppStorage.get<string>('role') ?? 'OWNER');
  @State houses: HouseItem[] = [];
  @State orders: OrderItem[] = [];

  private readonly C_INK: string = '#103B3A';
  private readonly C_MOSS: string = '#2E6D67';
  private readonly C_AMBER: string = '#E8930C';
  private readonly C_PAPER: string = '#F2F5F3';
  private readonly C_MIST: string = '#DCE7E3';
  private readonly C_MUTED: string = '#8A968F';
  private readonly C_BRICK: string = '#C4472E';

  private isOwner(): boolean {
    return this.role === 'OWNER';
  }

  aboutToAppear(): void {
    this.loadData();
  }

  private async loadData(): Promise<void> {
    const token = AppStorage.get<string>('accessToken') ?? '';
    if (token === '') {
      return;
    }
    try {
      if (this.isOwner()) {
        const result = await fetchMyHouses(token);
        if (result.code === 0 && result.data !== null) {
          this.houses = result.data;
        }
      } else {
        const result = await fetchWorkerOrders(token, true);
        if (result.code === 0 && result.data !== null) {
          this.orders = result.data.list;
        }
      }
    } catch (err) {
      // 加载失败保留空态
    }
  }

  private roleText(): string {
    return this.isOwner() ? '业主' : '维修师傅';
  }

  private workerStat(kind: string): number {
    if (kind === 'todo') {
      return this.orders.filter((o: OrderItem) => o.status === 'DISPATCHED').length;
    }
    if (kind === 'doing') {
      return this.orders.filter((o: OrderItem) => o.status === 'IN_PROGRESS').length;
    }
    return this.orders.filter((o: OrderItem) => o.status === 'COMPLETED').length;
  }

  private async logout(): Promise<void> {
    // 清会话（保留记住的用户名），回登录页
    try {
      const context = this.getUIContext().getHostContext();
      if (context) {
        const store = await preferences.getPreferences(context, 'wp_session');
        await store.clear();
        await store.flush();
      }
    } catch (err) {
      // 本地存储异常不阻塞退出
    }
    AppStorage.setOrCreate('accessToken', '');
    AppStorage.setOrCreate('realName', '');
    AppStorage.setOrCreate('role', '');
    router.replaceUrl({ url: 'pages/LoginPage' });
  }

  build() {
    Scroll() {
      Column() {
        // ---- 用户卡 ----
        Row({ space: 14 }) {
          Text(this.realName === '' ? '用' : this.realName.substring(0, 1))
            .fontSize(22)
            .fontWeight(FontWeight.Bold)
            .fontColor(Color.White)
            .width(56)
            .height(56)
            .textAlign(TextAlign.Center)
            .borderRadius(28)
            .backgroundColor(this.C_INK)
          Column({ space: 4 }) {
            Text(this.realName === '' ? '未登录' : this.realName)
              .fontSize(18)
              .fontWeight(FontWeight.Bold)
              .fontColor(this.C_INK)
            Text(this.roleText())
              .fontSize(11)
              .fontColor(this.C_AMBER)
              .border({ width: 1, color: this.C_AMBER })
              .borderRadius(10)
              .padding({ left: 8, right: 8, top: 2, bottom: 2 })
          }
          .alignItems(HorizontalAlign.Start)
          .layoutWeight(1)
        }
        .width('100%')
        .padding(20)
        .borderRadius(14)
        .backgroundColor(Color.White)
        .shadow({ radius: 6, color: 'rgba(16,59,58,0.08)', offsetY: 2 })

        // ---- 业主：我的房屋 ----
        if (this.isOwner()) {
          Column({ space: 10 }) {
            Text('我的房屋')
              .fontSize(15)
              .fontWeight(FontWeight.Medium)
              .fontColor(this.C_INK)
              .width('100%')
            if (this.houses.length === 0) {
              Text('暂无绑定房屋，请联系管理员审核')
                .fontSize(13)
                .fontColor(this.C_MUTED)
                .width('100%')
            } else {
              ForEach(this.houses, (h: HouseItem) => {
                Text(h.label)
                  .fontSize(14)
                  .fontColor(this.C_INK)
                  .width('100%')
                  .padding(12)
                  .borderRadius(10)
                  .backgroundColor(this.C_PAPER)
              }, (h: HouseItem) => `${h.houseId}`)
            }
          }
          .alignItems(HorizontalAlign.Start)
          .width('100%')
          .padding(18)
          .borderRadius(14)
          .backgroundColor(Color.White)
          .shadow({ radius: 6, color: 'rgba(16,59,58,0.08)', offsetY: 2 })
          .margin({ top: 14 })
        } else {
          // ---- 师傅：接单统计 ----
          Row() {
            Column({ space: 6 }) {
              Text(`${this.workerStat('todo')}`)
                .fontSize(22)
                .fontWeight(FontWeight.Bold)
                .fontColor(this.C_AMBER)
              Text('待接单')
                .fontSize(11)
                .fontColor(this.C_MUTED)
            }
            .layoutWeight(1)

            Column({ space: 6 }) {
              Text(`${this.workerStat('doing')}`)
                .fontSize(22)
                .fontWeight(FontWeight.Bold)
                .fontColor(this.C_MOSS)
              Text('进行中')
                .fontSize(11)
                .fontColor(this.C_MUTED)
            }
            .layoutWeight(1)

            Column({ space: 6 }) {
              Text(`${this.workerStat('done')}`)
                .fontSize(22)
                .fontWeight(FontWeight.Bold)
                .fontColor(this.C_INK)
              Text('已完结')
                .fontSize(11)
                .fontColor(this.C_MUTED)
            }
            .layoutWeight(1)
          }
          .width('100%')
          .padding(18)
          .borderRadius(14)
          .backgroundColor(Color.White)
          .shadow({ radius: 6, color: 'rgba(16,59,58,0.08)', offsetY: 2 })
          .margin({ top: 14 })
        }

        // ---- 功能列表 ----
        Column() {
          Row() {
            Text('关于 WarrantyPro')
              .fontSize(14)
              .fontColor(this.C_INK)
              .layoutWeight(1)
            Text('v0.1 演示版')
              .fontSize(12)
              .fontColor(this.C_MUTED)
          }
          .width('100%')
          .padding({ top: 14, bottom: 14 })
          .border({ width: { bottom: 1 }, color: this.C_MIST })
          .onClick(() => {
            promptAction.showToast({ message: '数字化物业保修平台 · 比赛演示版' });
          })

          Text('退出登录')
            .fontSize(14)
            .fontColor(this.C_BRICK)
            .width('100%')
            .textAlign(TextAlign.Center)
            .padding({ top: 14, bottom: 14 })
            .onClick(() => {
              this.logout();
            })
        }
        .width('100%')
        .padding({ left: 18, right: 18 })
        .borderRadius(14)
        .backgroundColor(Color.White)
        .shadow({ radius: 6, color: 'rgba(16,59,58,0.08)', offsetY: 2 })
        .margin({ top: 14 })
      }
      .width('100%')
      .padding(18)
    }
    .width('100%')
    .height('100%')
    .scrollBar(BarState.Off)
    .edgeEffect(EdgeEffect.Spring)
    .align(Alignment.Top)
    .backgroundColor(this.C_PAPER)
  }
}

import { router } from '@kit.ArkUI';
import { fetchMyOrders, fetchWorkerOrders, OrderItem } from '../common/ApiClient';
import { statusLabel, statusColor, categoryLabel } from '../common/Labels';

/** 首页 Tab：按角色渲染业主 / 师傅工作台。 */
@Component
export struct HomePage {
  @State realName: string = (AppStorage.get<string>('realName') ?? '');
  @State role: string = (AppStorage.get<string>('role') ?? 'OWNER');
  @State orders: OrderItem[] = [];
  @State loading: boolean = false;

  private readonly C_INK: string = '#103B3A';
  private readonly C_MOSS: string = '#2E6D67';
  private readonly C_AMBER: string = '#E8930C';
  private readonly C_PAPER: string = '#F2F5F3';
  private readonly C_MIST: string = '#DCE7E3';
  private readonly C_MUTED: string = '#8A968F';

  private isOwner(): boolean {
    return this.role === 'OWNER';
  }

  aboutToAppear(): void {
    this.loadData();
  }

  private ongoingCount(): number {
    return this.orders.filter((o: OrderItem) =>
      o.status !== 'COMPLETED' && o.status !== 'CANCELLED').length;
  }

  private todoCount(): number {
    return this.orders.filter((o: OrderItem) => o.status === 'DISPATCHED').length;
  }

  private doingCount(): number {
    return this.orders.filter((o: OrderItem) => o.status === 'IN_PROGRESS').length;
  }

  private async loadData(): Promise<void> {
    const token = AppStorage.get<string>('accessToken') ?? '';
    if (token === '') {
      return;
    }
    this.loading = true;
    try {
      const result = this.isOwner()
        ? await fetchMyOrders(token)
        : await fetchWorkerOrders(token, false);
      if (result.code === 0 && result.data !== null) {
        this.orders = result.data.list.slice(0, 5);
      }
    } catch (err) {
      // 网络异常时保留旧列表
    } finally {
      this.loading = false;
    }
  }

  @Builder
  OrderRow(order: OrderItem) {
    Column({ space: 6 }) {
      Row() {
        Text(categoryLabel(order.category))
          .fontSize(15)
          .fontWeight(FontWeight.Medium)
          .fontColor(this.C_INK)
          .layoutWeight(1)
        Text(statusLabel(order.status))
          .fontSize(11)
          .fontColor(statusColor(order.status))
          .border({ width: 1, color: statusColor(order.status) })
          .borderRadius(10)
          .padding({ left: 8, right: 8, top: 2, bottom: 2 })
      }
      .width('100%')

      Text(order.locationDetail === '' ? order.phenomenon : `${order.locationDetail} · ${order.phenomenon}`)
        .fontSize(13)
        .fontColor(this.C_MUTED)
        .maxLines(1)
        .textOverflow({ overflow: TextOverflow.Ellipsis })
        .width('100%')

      Text(order.submittedAt !== '' ? order.submittedAt.substring(5, 16) : order.createdAt.substring(5, 16))
        .fontSize(11)
        .fontColor(this.C_MUTED)
        .width('100%')
    }
    .alignItems(HorizontalAlign.Start)
    .width('100%')
    .padding({ top: 12, bottom: 12 })
    .border({ width: { bottom: 1 }, color: this.C_MIST })
    .onClick(() => {
      router.pushUrl({ url: 'pages/OrderDetailPage', params: { orderId: order.id } as OrderDetailParams });
    })
  }

  build() {
    Scroll() {
      Column() {
        // ---- 松青头部：问候 + 统计 ----
        Column({ space: 14 }) {
          Row({ space: 10 }) {
            Text(this.realName === '' ? '你好' : `你好，${this.realName}`)
              .fontSize(22)
              .fontWeight(FontWeight.Bold)
              .fontColor(Color.White)
              .layoutWeight(1)
            Text(this.isOwner() ? '业主' : '维修师傅')
              .fontSize(11)
              .fontColor(this.C_AMBER)
              .border({ width: 1, color: this.C_AMBER })
              .borderRadius(10)
              .padding({ left: 8, right: 8, top: 2, bottom: 2 })
          }
          .width('100%')

          Text(this.isOwner() ? '家里有什么需要修的？' : '有新的派单会在这里提醒你')
            .fontSize(13)
            .fontColor('rgba(255,255,255,0.7)')
            .width('100%')

          Row({ space: 10 }) {
            Text(this.isOwner() ? `进行中 ${this.ongoingCount()} 单` : `待上门 ${this.todoCount()} 单`)
              .fontSize(13)
              .fontColor(Color.White)
              .backgroundColor('rgba(255,255,255,0.12)')
              .borderRadius(10)
              .padding({ left: 12, right: 12, top: 6, bottom: 6 })
            Text(this.isOwner() ? `累计 ${this.orders.length} 单` : `进行中 ${this.doingCount()} 单`)
              .fontSize(13)
              .fontColor(Color.White)
              .backgroundColor('rgba(255,255,255,0.12)')
              .borderRadius(10)
              .padding({ left: 12, right: 12, top: 6, bottom: 6 })
          }
          .width('100%')
        }
        .alignItems(HorizontalAlign.Start)
        .width('100%')
        .padding({ left: 24, right: 24, top: 20, bottom: 46 })
        .backgroundColor(this.C_INK)

        // ---- 白卡拉起内容 ----
        Column() {
          if (this.isOwner()) {
            Row({ space: 12 }) {
              Column({ space: 4 }) {
                Text('立即报修')
                  .fontSize(17)
                  .fontWeight(FontWeight.Bold)
                  .fontColor(this.C_INK)
                Text('描述故障，提交后安排师傅处理')
                  .fontSize(12)
                  .fontColor(this.C_MUTED)
              }
              .alignItems(HorizontalAlign.Start)
              .layoutWeight(1)

              Text('›')
                .fontSize(22)
                .fontColor(this.C_AMBER)
                .fontWeight(FontWeight.Bold)
            }
            .width('100%')
            .padding(18)
            .borderRadius(14)
            .backgroundColor(Color.White)
            .border({ width: { left: 3 }, color: this.C_AMBER })
            .shadow({ radius: 6, color: 'rgba(16,59,58,0.08)', offsetY: 2 })
            .onClick(() => {
              router.pushUrl({ url: 'pages/RepairFormPage' });
            })
          }

          Column({ space: 2 }) {
            Text(this.isOwner() ? '最近工单' : '我的工单')
              .fontSize(15)
              .fontWeight(FontWeight.Medium)
              .fontColor(this.C_INK)
              .width('100%')
              .margin({ bottom: 4 })

            if (this.orders.length === 0) {
              Text(this.loading ? '加载中…' :
                (this.isOwner() ? '还没有工单，点上方「立即报修」提交第一单' : '暂无待办工单，有派单会推送提醒'))
                .fontSize(13)
                .fontColor(this.C_MUTED)
                .width('100%')
                .margin({ top: 16, bottom: 16 })
                .textAlign(TextAlign.Center)
            } else {
              ForEach(this.orders, (o: OrderItem) => {
                this.OrderRow(o)
              }, (o: OrderItem) => `${o.id}`)
            }
          }
          .alignItems(HorizontalAlign.Start)
          .width('100%')
          .padding(18)
          .borderRadius(14)
          .backgroundColor(Color.White)
          .shadow({ radius: 6, color: 'rgba(16,59,58,0.08)', offsetY: 2 })
          .margin({ top: 14 })
        }
        .padding({ left: 18, right: 18 })
        .margin({ top: -28 })
      }
      .width('100%')
    }
    .width('100%')
    .height('100%')
    .scrollBar(BarState.Off)
    .edgeEffect(EdgeEffect.Spring)
    .align(Alignment.Top)
    .backgroundColor(this.C_PAPER)
  }
}

/** 路由参数（跳转工单详情）。 */
export class OrderDetailParams {
  orderId: number = 0;
}

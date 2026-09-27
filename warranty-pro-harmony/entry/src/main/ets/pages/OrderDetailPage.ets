import { promptAction, router } from '@kit.ArkUI';
import { arriveOrder, completeOrder, fetchOrderDetail, OrderItem, FlowItem } from '../common/ApiClient';
import { statusLabel, statusColor, categoryLabel, verdictText, actionLabel } from '../common/Labels';

/**
 * 工单详情：状态头部 + 保修判定卡 + 流转时间轴（docs/02 FR-O-04）。
 * 师傅角色在详情内直接操作：待上门 → 到场打卡；维修中 → 完工提交。
 */
@Entry
@Component
struct OrderDetailPage {
  @State order: OrderItem = new OrderItem();
  @State loaded: boolean = false;
  @State actionLoading: boolean = false;
  private orderId: number = 0;
  private role: string = (AppStorage.get<string>('role') ?? 'OWNER');

  private readonly C_INK: string = '#103B3A';
  private readonly C_MOSS: string = '#2E6D67';
  private readonly C_AMBER: string = '#E8930C';
  private readonly C_PAPER: string = '#F2F5F3';
  private readonly C_MIST: string = '#DCE7E3';
  private readonly C_MUTED: string = '#8A968F';

  aboutToAppear(): void {
    const params = router.getParams() as OrderDetailParams;
    if (params) {
      this.orderId = params.orderId;
    }
    this.loadData();
  }

  private async loadData(): Promise<void> {
    const token = AppStorage.get<string>('accessToken') ?? '';
    if (token === '' || this.orderId === 0) {
      return;
    }
    try {
      const result = await fetchOrderDetail(token, this.orderId);
      if (result.code === 0 && result.data !== null) {
        this.order = result.data;
        this.loaded = true;
      } else {
        promptAction.showToast({ message: result.message });
      }
    } catch (err) {
      promptAction.showToast({ message: '加载失败，请检查网络' });
    }
  }

  private async onArrive(): Promise<void> {
    if (this.actionLoading) {
      return;
    }
    this.actionLoading = true;
    try {
      const token = AppStorage.get<string>('accessToken') ?? '';
      const result = await arriveOrder(token, this.orderId);
      if (result.code !== 0) {
        promptAction.showToast({ message: result.message === '' ? '打卡失败，请稍后再试' : result.message });
        return;
      }
      promptAction.showToast({ message: '已确认到场，开工顺利' });
      await this.loadData();
    } catch (err) {
      promptAction.showToast({ message: '连接不上服务，请检查网络' });
    } finally {
      this.actionLoading = false;
    }
  }

  /** 师傅操作区：待上门 → 到场打卡；维修中 → 完工提交。 */
  @Builder
  WorkerActions() {
    if (this.role === 'WORKER' && this.order.status === 'DISPATCHED') {
      Button(this.actionLoading ? '正在打卡' : '到场打卡')
        .width('100%')
        .height(48)
        .borderRadius(24)
        .backgroundColor(this.C_MOSS)
        .fontColor(Color.White)
        .fontSize(16)
        .fontWeight(FontWeight.Medium)
        .margin({ top: 14 })
        .enabled(!this.actionLoading)
        .onClick(() => {
          this.onArrive();
        })
    } else if (this.role === 'WORKER' && this.order.status === 'IN_PROGRESS') {
      Button('完工提交')
        .width('100%')
        .height(48)
        .borderRadius(24)
        .backgroundColor(this.C_AMBER)
        .fontColor(Color.White)
        .fontSize(16)
        .fontWeight(FontWeight.Medium)
        .margin({ top: 14 })
        .onClick(() => {
          router.pushUrl({ url: 'pages/WorkerCompletePage', params: { orderId: this.order.id } as CompleteParams });
        })
    }
  }

  @Builder
  TimelineRow(flow: FlowItem, isLast: boolean) {
    Row({ space: 12 }) {
      // 左侧：节点 + 连接线
      Column() {
        Row()
          .width(10)
          .height(10)
          .borderRadius(5)
          .backgroundColor(statusColor(flow.toStatus))
        if (!isLast) {
          Row()
            .width(2)
            .layoutWeight(1)
            .backgroundColor(this.C_MIST)
        }
      }
      .width(10)
      .alignItems(HorizontalAlign.Center)

      // 右侧：动作 + 时间 + 备注
      Column({ space: 4 }) {
        Text(actionLabel(flow.action))
          .fontSize(14)
          .fontWeight(FontWeight.Medium)
          .fontColor(this.C_INK)
        Text(flow.createdAt.substring(0, 16))
          .fontSize(11)
          .fontColor(this.C_MUTED)
        if (flow.remark !== '') {
          Text(flow.remark)
            .fontSize(12)
            .fontColor(this.C_MUTED)
        }
      }
      .alignItems(HorizontalAlign.Start)
      .layoutWeight(1)
      .padding({ bottom: isLast ? 0 : 18 })
    }
    .alignItems(VerticalAlign.Top)
    .width('100%')
  }

  build() {
    Column() {
      // ---- 顶栏 ----
      Row({ space: 10 }) {
        Text('‹')
          .fontSize(26)
          .fontColor(this.C_INK)
          .width(36)
          .height(36)
          .textAlign(TextAlign.Center)
          .onClick(() => {
            router.back();
          })
        Text('工单详情')
          .fontSize(17)
          .fontWeight(FontWeight.Bold)
          .fontColor(this.C_INK)
          .layoutWeight(1)
      }
      .width('100%')
      .padding({ left: 12, right: 20, top: 8, bottom: 8 })

      Scroll() {
        Column() {
          if (!this.loaded) {
            Text('加载中…')
              .fontSize(13)
              .fontColor(this.C_MUTED)
              .width('100%')
              .margin({ top: 40 })
              .textAlign(TextAlign.Center)
          } else {
            // ---- 状态头部卡 ----
            Column({ space: 8 }) {
              Row() {
                Text(categoryLabel(this.order.category))
                  .fontSize(16)
                  .fontWeight(FontWeight.Bold)
                  .fontColor(Color.White)
                  .layoutWeight(1)
                Text(statusLabel(this.order.status))
                  .fontSize(12)
                  .fontColor(Color.White)
                  .border({ width: 1, color: 'rgba(255,255,255,0.6)' })
                  .borderRadius(12)
                  .padding({ left: 10, right: 10, top: 3, bottom: 3 })
              }
              .width('100%')

              Text(this.order.orderNo)
                .fontSize(12)
                .fontColor('rgba(255,255,255,0.7)')
                .width('100%')

              Text(this.order.locationDetail === '' ? this.order.phenomenon :
                `${this.order.locationDetail} · ${this.order.phenomenon}`)
                .fontSize(13)
                .fontColor('rgba(255,255,255,0.85)')
                .width('100%')
            }
            .alignItems(HorizontalAlign.Start)
            .width('100%')
            .padding(18)
            .borderRadius(14)
            .backgroundColor(statusColor(this.order.status) === '#8A968F' ? this.C_INK : this.C_INK)

            this.WorkerActions()

            // ---- 保修判定卡 ----
            Column({ space: 8 }) {
              Text('保修判定')
                .fontSize(13)
                .fontWeight(FontWeight.Medium)
                .fontColor(this.C_INK)
              Row({ space: 8 }) {
                Text(verdictText(this.order.verdict))
                  .fontSize(14)
                  .fontColor(this.order.verdict === 'IN_WARRANTY' ? this.C_MOSS : this.C_AMBER)
                  .fontWeight(FontWeight.Medium)
                  .layoutWeight(1)
              }
              .width('100%')

              if (this.order.verdictBasis !== '') {
                Text(`依据：${this.order.verdictBasis}`)
                  .fontSize(11)
                  .fontColor(this.C_MUTED)
                  .width('100%')
              }
            }
            .alignItems(HorizontalAlign.Start)
            .width('100%')
            .padding(16)
            .borderRadius(14)
            .backgroundColor(Color.White)
            .border({ width: { left: 3 }, color: this.C_AMBER })
            .shadow({ radius: 6, color: 'rgba(16,59,58,0.08)', offsetY: 2 })
            .margin({ top: 14 })

            // ---- 流转时间轴 ----
            Column({ space: 2 }) {
              Text('流转记录')
                .fontSize(15)
                .fontWeight(FontWeight.Medium)
                .fontColor(this.C_INK)
                .width('100%')
                .margin({ bottom: 14 })

              ForEach(this.order.flows ?? [], (f: FlowItem, idx: number) => {
                this.TimelineRow(f, idx === (this.order.flows ?? []).length - 1)
              }, (f: FlowItem) => `${f.id}`)
            }
            .alignItems(HorizontalAlign.Start)
            .width('100%')
            .padding(18)
            .borderRadius(14)
            .backgroundColor(Color.White)
            .shadow({ radius: 6, color: 'rgba(16,59,58,0.08)', offsetY: 2 })
            .margin({ top: 14 })
          }
        }
        .width('100%')
        .padding(18)
      }
      .layoutWeight(1)
      .scrollBar(BarState.Off)
      .edgeEffect(EdgeEffect.Spring)
      .align(Alignment.Top)
    }
    .width('100%')
    .height('100%')
    .backgroundColor(this.C_PAPER)
  }
}

class OrderDetailParams {
  orderId: number = 0;
}

class CompleteParams {
  orderId: number = 0;
}

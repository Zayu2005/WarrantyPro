import { router } from '@kit.ArkUI';
import { fetchMyOrders, fetchWorkerOrders, OrderItem } from '../common/ApiClient';
import { statusLabel, statusColor, categoryLabel } from '../common/Labels';

/** 筛选维度。 */
class FilterChip {
  key: string = '';
  label: string = '';
}

/** 订单 Tab：全量工单 + 状态筛选，点击进详情时间轴。 */
@Component
export struct OrdersPage {
  @State orders: OrderItem[] = [];
  @State filter: string = 'ALL';
  @State loading: boolean = false;
  private role: string = (AppStorage.get<string>('role') ?? 'OWNER');

  private readonly C_INK: string = '#103B3A';
  private readonly C_MOSS: string = '#2E6D67';
  private readonly C_AMBER: string = '#E8930C';
  private readonly C_PAPER: string = '#F2F5F3';
  private readonly C_MIST: string = '#DCE7E3';
  private readonly C_MUTED: string = '#8A968F';
  private readonly filters: FilterChip[] = [
    { key: 'ALL', label: '全部' } as FilterChip,
    { key: 'ONGOING', label: '进行中' } as FilterChip,
    { key: 'PENDING_CONFIRM', label: '待验收' } as FilterChip,
    { key: 'DONE', label: '已完结' } as FilterChip
  ];

  aboutToAppear(): void {
    this.loadData();
  }

  private async loadData(): Promise<void> {
    const token = AppStorage.get<string>('accessToken') ?? '';
    if (token === '') {
      return;
    }
    this.loading = true;
    try {
      const result = this.role === 'OWNER'
        ? await fetchMyOrders(token)
        : await fetchWorkerOrders(token, true);
      if (result.code === 0 && result.data !== null) {
        this.orders = result.data.list;
      }
    } catch (err) {
      // 网络异常时保留旧列表
    } finally {
      this.loading = false;
    }
  }

  private visibleOrders(): OrderItem[] {
    if (this.filter === 'ALL') {
      return this.orders;
    }
    if (this.filter === 'ONGOING') {
      return this.orders.filter((o: OrderItem) =>
        o.status === 'SUBMITTED' || o.status === 'PENDING_DISPATCH' || o.status === 'EXTERNAL_PROCESSING'
        || o.status === 'DISPATCHED' || o.status === 'IN_PROGRESS');
    }
    if (this.filter === 'PENDING_CONFIRM') {
      return this.orders.filter((o: OrderItem) => o.status === 'PENDING_CONFIRM');
    }
    return this.orders.filter((o: OrderItem) => o.status === 'COMPLETED');
  }

  build() {
    Column() {
      Text('订单')
        .fontSize(18)
        .fontWeight(FontWeight.Bold)
        .fontColor(this.C_INK)
        .width('100%')
        .margin({ top: 20 })

      // ---- 筛选 chips ----
      Row({ space: 8 }) {
        ForEach(this.filters, (f: FilterChip) => {
          Text(f.label)
            .fontSize(13)
            .fontColor(this.filter === f.key ? Color.White : this.C_INK)
            .backgroundColor(this.filter === f.key ? this.C_INK : Color.White)
            .border({ width: 1, color: this.filter === f.key ? this.C_INK : this.C_MIST })
            .borderRadius(14)
            .padding({ left: 12, right: 12, top: 5, bottom: 5 })
            .onClick(() => {
              this.filter = f.key;
            })
        }, (f: FilterChip) => f.key)
      }
      .width('100%')
      .margin({ top: 12 })

      Scroll() {
        Column({ space: 10 }) {
          if (this.visibleOrders().length === 0) {
            Text(this.loading ? '加载中…' : '该状态下暂无工单')
              .fontSize(13)
              .fontColor(this.C_MUTED)
              .width('100%')
              .margin({ top: 40 })
              .textAlign(TextAlign.Center)
          } else {
            ForEach(this.visibleOrders(), (o: OrderItem) => {
              Column({ space: 8 }) {
                Row() {
                  Text(`${categoryLabel(o.category)} · ${o.orderNo}`)
                    .fontSize(14)
                    .fontWeight(FontWeight.Medium)
                    .fontColor(this.C_INK)
                    .layoutWeight(1)
                  Text(statusLabel(o.status))
                    .fontSize(11)
                    .fontColor(statusColor(o.status))
                    .border({ width: 1, color: statusColor(o.status) })
                    .borderRadius(10)
                    .padding({ left: 8, right: 8, top: 2, bottom: 2 })
                }
                .width('100%')

                Text(o.locationDetail === '' ? o.phenomenon : `${o.locationDetail} · ${o.phenomenon}`)
                  .fontSize(12)
                  .fontColor(this.C_MUTED)
                  .maxLines(1)
                  .textOverflow({ overflow: TextOverflow.Ellipsis })
                  .width('100%')

                Text(o.submittedAt !== '' ? o.submittedAt.substring(0, 16) : o.createdAt.substring(0, 16))
                  .fontSize(11)
                  .fontColor(this.C_MUTED)
                  .width('100%')
              }
              .alignItems(HorizontalAlign.Start)
              .width('100%')
              .padding(14)
              .borderRadius(12)
              .backgroundColor(Color.White)
              .border({ width: 1, color: this.C_MIST })
              .onClick(() => {
                router.pushUrl({ url: 'pages/OrderDetailPage', params: { orderId: o.id } as OrderDetailParams });
              })
            }, (o: OrderItem) => `${o.id}-${o.status}`)
          }
        }
        .width('100%')
        .padding({ top: 14, bottom: 20 })
      }
      .layoutWeight(1)
      .scrollBar(BarState.Off)
      .edgeEffect(EdgeEffect.Spring)
      .align(Alignment.Top)
    }
    .width('100%')
    .height('100%')
    .padding({ left: 18, right: 18 })
    .backgroundColor(this.C_PAPER)
  }
}

class OrderDetailParams {
  orderId: number = 0;
}

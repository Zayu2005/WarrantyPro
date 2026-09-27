import { router } from '@kit.ArkUI';
import { fetchNotices, NoticeItem } from '../common/ApiClient';
import { categoryLabel } from '../common/Labels';

/** 报修表单路由参数：预选故障类别。 */
export class RepairParams {
  category: string = '';
}

/** 服务 Tab：热门服务宫格（预选类别直达报修表单）+ 物业公告。 */
@Component
export struct ServicePage {
  @State notices: NoticeItem[] = [];
  @State loading: boolean = false;

  private readonly C_INK: string = '#103B3A';
  private readonly C_MOSS: string = '#2E6D67';
  private readonly C_AMBER: string = '#E8930C';
  private readonly C_PAPER: string = '#F2F5F3';
  private readonly C_MIST: string = '#DCE7E3';
  private readonly C_MUTED: string = '#8A968F';
  private readonly categories: string[] =
    ['WATER_ELECTRICITY', 'CIVIL_WATERPROOF', 'DOOR_WINDOW', 'HVAC', 'ELEVATOR', 'OTHER'];
  private readonly categoryDesc: Record<string, string> = {
    'WATER_ELECTRICITY': '灯具 / 插座 / 水管漏水',
    'CIVIL_WATERPROOF': '渗漏 / 墙面开裂',
    'DOOR_WINDOW': '门锁 / 窗户五金',
    'HVAC': '空调 / 地暖故障',
    'ELEVATOR': '电梯运行异常',
    'OTHER': '其他居家维修'
  };

  aboutToAppear(): void {
    this.loadNotices();
  }

  private async loadNotices(): Promise<void> {
    const token = AppStorage.get<string>('accessToken') ?? '';
    if (token === '') {
      return;
    }
    this.loading = true;
    try {
      const result = await fetchNotices(token);
      if (result.code === 0 && result.data !== null) {
        this.notices = result.data;
      }
    } catch (err) {
      // 公告加载失败时展示空态
    } finally {
      this.loading = false;
    }
  }

  @Builder
  ServiceCard(code: string) {
    Column({ space: 6 }) {
      Text(categoryLabel(code))
        .fontSize(15)
        .fontWeight(FontWeight.Medium)
        .fontColor(this.C_INK)
      Text(this.categoryDesc[code] ?? '')
        .fontSize(11)
        .fontColor(this.C_MUTED)
        .maxLines(1)
      Text('立即报修 ›')
        .fontSize(11)
        .fontColor(this.C_AMBER)
        .margin({ top: 2 })
    }
    .alignItems(HorizontalAlign.Start)
    .width('100%')
    .padding(14)
    .borderRadius(12)
    .backgroundColor(Color.White)
    .border({ width: 1, color: this.C_MIST })
    .onClick(() => {
      router.pushUrl({ url: 'pages/RepairFormPage', params: { category: code } as RepairParams });
    })
  }

  build() {
    Scroll() {
      Column() {
        Text('热门服务')
          .fontSize(18)
          .fontWeight(FontWeight.Bold)
          .fontColor(this.C_INK)
          .width('100%')
          .margin({ top: 20 })

        Text('选择故障类别，直达报修表单')
          .fontSize(12)
          .fontColor(this.C_MUTED)
          .width('100%')
          .margin({ top: 4 })

        GridRow({ columns: 12, gutter: 10 }) {
          ForEach(this.categories, (c: string) => {
            GridCol({ span: 6 }) {
              this.ServiceCard(c)
            }
          }, (c: string) => c)
        }
        .width('100%')
        .margin({ top: 14 })

        // ---- 物业公告 ----
        Row() {
          Text('物业公告')
            .fontSize(15)
            .fontWeight(FontWeight.Medium)
            .fontColor(this.C_INK)
          Blank()
          Text(this.loading ? '加载中…' : `${this.notices.length} 条`)
            .fontSize(12)
            .fontColor(this.C_MUTED)
        }
        .width('100%')
        .margin({ top: 22, bottom: 10 })

        Column({ space: 2 }) {
          if (this.notices.length === 0) {
            Text('暂无公告')
              .fontSize(13)
              .fontColor(this.C_MUTED)
              .width('100%')
              .padding({ top: 20, bottom: 20 })
              .textAlign(TextAlign.Center)
          } else {
            ForEach(this.notices, (n: NoticeItem) => {
              Column({ space: 6 }) {
                Row({ space: 8 }) {
                  Text(n.type)
                    .fontSize(10)
                    .fontColor(this.C_MOSS)
                    .border({ width: 1, color: this.C_MIST })
                    .borderRadius(8)
                    .padding({ left: 6, right: 6, top: 1, bottom: 1 })
                  Text(n.title)
                    .fontSize(14)
                    .fontWeight(FontWeight.Medium)
                    .fontColor(this.C_INK)
                    .layoutWeight(1)
                }
                .width('100%')

                Text(n.content)
                  .fontSize(12)
                  .fontColor(this.C_MUTED)
                  .maxLines(2)
                  .textOverflow({ overflow: TextOverflow.Ellipsis })
                  .width('100%')

                Text(n.createdAt.substring(0, 10))
                  .fontSize(10)
                  .fontColor(this.C_MUTED)
                  .width('100%')
              }
              .alignItems(HorizontalAlign.Start)
              .width('100%')
              .padding({ top: 12, bottom: 12 })
              .border({ width: { bottom: 1 }, color: this.C_MIST })
            }, (n: NoticeItem) => `${n.id}`)
          }
        }
        .width('100%')
        .padding({ left: 16, right: 16, bottom: 8 })
        .borderRadius(14)
        .backgroundColor(Color.White)
        .shadow({ radius: 6, color: 'rgba(16,59,58,0.08)', offsetY: 2 })
      }
      .width('100%')
      .padding({ left: 18, right: 18, bottom: 20 })
    }
    .width('100%')
    .height('100%')
    .scrollBar(BarState.Off)
    .edgeEffect(EdgeEffect.Spring)
    .align(Alignment.Top)
    .backgroundColor(this.C_PAPER)
  }
}



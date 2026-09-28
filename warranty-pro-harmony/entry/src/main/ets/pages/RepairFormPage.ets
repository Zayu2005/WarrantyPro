import { promptAction, router } from '@kit.ArkUI';
import { createOrder, fetchMyHouses, HouseItem } from '../common/ApiClient';
import { categoryLabel } from '../common/Labels';

/** 报修表单路由参数：预选故障类别（服务宫格跳入时携带）。 */
class RepairFormParams {
  category: string = '';
}

/**
 * 表单报修页（docs/02 FR-O-03）：选房 → 故障类别 → 位置与现象 → 紧急度 → 提交。
 * 提交成功后进入统一派单流程。
 */
@Entry
@Component
struct RepairFormPage {
  @State houses: HouseItem[] = [];
  @State houseIndex: number = 0;
  @State category: string = 'WATER_ELECTRICITY';
  @State locationDetail: string = '';
  @State phenomenon: string = '';
  @State urgent: boolean = false;
  @State submitting: boolean = false;

  private readonly C_INK: string = '#103B3A';
  private readonly C_MOSS: string = '#2E6D67';
  private readonly C_AMBER: string = '#E8930C';
  private readonly C_PAPER: string = '#F2F5F3';
  private readonly C_MIST: string = '#DCE7E3';
  private readonly C_MUTED: string = '#8A968F';
  private readonly C_BRICK: string = '#C4472E';
  private readonly categories: string[] =
    ['WATER_ELECTRICITY', 'CIVIL_WATERPROOF', 'DOOR_WINDOW', 'HVAC', 'ELEVATOR', 'OTHER'];

  private token(): string {
    return AppStorage.get<string>('accessToken') ?? '';
  }

  aboutToAppear(): void {
    const params = router.getParams() as RepairFormParams | null;
    if (params && params.category !== '') {
      this.category = params.category;
    }
    this.loadHouses();
  }

  private async loadHouses(): Promise<void> {
    try {
      const result = await fetchMyHouses(this.token());
      if (result.code === 0 && result.data !== null) {
        this.houses = result.data;
      }
    } catch (err) {
      // 房屋加载失败时保留空列表，提交时给出提示
    }
  }

  private async onSubmit(): Promise<void> {
    if (this.submitting) {
      return;
    }
    if (this.houses.length === 0) {
      promptAction.showToast({ message: '还没有已审核通过的绑定房屋，请联系管理员' });
      return;
    }
    if (this.locationDetail.trim() === '' || this.phenomenon.trim() === '') {
      promptAction.showToast({ message: '请填写故障位置和现象描述' });
      return;
    }
    this.submitting = true;
    try {
      const house: HouseItem = this.houses[this.houseIndex];
      const result = await createOrder(this.token(), this.category, house.houseId,
        this.locationDetail.trim(), this.phenomenon.trim(), this.urgent ? 'URGENT' : 'NORMAL');
      if (result.code !== 0 || result.data === null) {
        promptAction.showToast({ message: result.message === '' ? '提交失败，请稍后再试' : result.message });
        return;
      }
      promptAction.showToast({ message: `${result.data.orderNo} 已提交，正在安排师傅` });
      router.back();
    } catch (err) {
      promptAction.showToast({ message: '连接不上服务，请检查网络后重试' });
    } finally {
      this.submitting = false;
    }
  }

  @Builder
  SectionLabel(text: string) {
    Text(text)
      .fontSize(13)
      .fontWeight(FontWeight.Medium)
      .fontColor(this.C_INK)
      .margin({ top: 18, bottom: 10 })
  }

  @Builder
  CategoryChip(code: string) {
    Text(categoryLabel(code))
      .fontSize(13)
      .fontColor(this.category === code ? Color.White : this.C_INK)
      .backgroundColor(this.category === code ? this.C_INK : Color.White)
      .border({ width: 1, color: this.category === code ? this.C_INK : this.C_MIST })
      .borderRadius(16)
      .padding({ left: 13, right: 13, top: 7, bottom: 7 })
      .onClick(() => {
        this.category = code;
      })
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
        Text('表单报修')
          .fontSize(17)
          .fontWeight(FontWeight.Bold)
          .fontColor(this.C_INK)
          .layoutWeight(1)
      }
      .width('100%')
      .padding({ left: 12, right: 20, top: 8, bottom: 8 })

      Scroll() {
        Column() {
          // ---- 报修房屋 ----
          this.SectionLabel('报修房屋')
          if (this.houses.length === 0) {
            Text('暂无绑定房屋，请联系管理员审核')
              .fontSize(13)
              .fontColor(this.C_BRICK)
          } else {
            Text(this.houses[this.houseIndex].label)
              .fontSize(15)
              .fontColor(this.C_INK)
              .width('100%')
              .padding(14)
              .borderRadius(12)
              .backgroundColor(Color.White)
              .border({ width: 1, color: this.C_MIST })
          }

          // ---- 故障类别 ----
          this.SectionLabel('故障类别')
          Flex({ wrap: FlexWrap.Wrap }) {
            ForEach(this.categories, (c: string) => {
              this.CategoryChip(c)
            }, (c: string) => c)
          }
          .margin({ left: -5, right: -5 })

          // ---- 位置与现象 ----
          this.SectionLabel('故障位置')
          TextInput({ placeholder: '如：主卫顶部 / 厨房水龙头', text: this.locationDetail })
            .fontSize(15)
            .fontColor(this.C_INK)
            .caretColor(this.C_INK)
            .backgroundColor(Color.White)
            .borderRadius(12)
            .height(46)
            .onChange((v: string) => {
              this.locationDetail = v;
            })

          this.SectionLabel('现象描述')
          TextArea({ placeholder: '描述故障现象与持续时间，便于快速判定责任', text: this.phenomenon })
            .fontSize(15)
            .fontColor(this.C_INK)
            .caretColor(this.C_INK)
            .backgroundColor(Color.White)
            .borderRadius(12)
            .height(96)
            .onChange((v: string) => {
              this.phenomenon = v;
            })

          // ---- 紧急程度 ----
          this.SectionLabel('紧急程度')
          Row({ space: 10 }) {
            Text('普通')
              .fontSize(13)
              .fontColor(!this.urgent ? Color.White : this.C_INK)
              .backgroundColor(!this.urgent ? this.C_INK : Color.White)
              .border({ width: 1, color: !this.urgent ? this.C_INK : this.C_MIST })
              .borderRadius(16)
              .padding({ left: 16, right: 16, top: 7, bottom: 7 })
              .onClick(() => {
                this.urgent = false;
              })
            Text('紧急')
              .fontSize(13)
              .fontColor(this.urgent ? Color.White : this.C_BRICK)
              .backgroundColor(this.urgent ? this.C_BRICK : Color.White)
              .border({ width: 1, color: this.urgent ? this.C_BRICK : this.C_MIST })
              .borderRadius(16)
              .padding({ left: 16, right: 16, top: 7, bottom: 7 })
              .onClick(() => {
                this.urgent = true;
              })
          }

          // ---- 公共设施说明 ----
          Text('公共设施（电梯 / 消防等）报修由物业登记，即将开放')
            .fontSize(11)
            .fontColor(this.C_MUTED)
            .margin({ top: 18 })

          // ---- 提交 ----
          Button(this.submitting ? '正在提交' : '提交报修')
            .width('100%')
            .height(50)
            .borderRadius(25)
            .backgroundColor(this.C_INK)
            .fontColor(Color.White)
            .fontSize(16)
            .fontWeight(FontWeight.Medium)
            .margin({ top: 24 })
            .enabled(!this.submitting)
            .onClick(() => {
              this.onSubmit();
            })

          Text('提交后进入统一派单流程，师傅到场后可追踪进度')
            .fontSize(11)
            .fontColor(this.C_MUTED)
            .margin({ top: 12 })
        }
        .alignItems(HorizontalAlign.Start)
        .width('100%')
        .padding(20)
      }
      .layoutWeight(1)
      .scrollBar(BarState.Off)
      .edgeEffect(EdgeEffect.Spring)
    }
    .width('100%')
    .height('100%')
    .backgroundColor(this.C_PAPER)
  }
}

import { promptAction, router } from '@kit.ArkUI';
import { arriveOrder, completeOrder } from '../common/ApiClient';

/**
 * 师傅完工提交页：故障原因 / 处理措施 / 工时 → 待验收。
 * 路由参数：orderId（必填）。
 */
@Entry
@Component
struct WorkerCompletePage {
  @State faultCause: string = '';
  @State measures: string = '';
  @State workHoursText: string = '0.5';
  @State submitting: boolean = false;
  private orderId: number = 0;

  private readonly C_INK: string = '#103B3A';
  private readonly C_PAPER: string = '#F2F5F3';
  private readonly C_MIST: string = '#DCE7E3';
  private readonly C_MUTED: string = '#8A968F';

  aboutToAppear(): void {
    const params = router.getParams() as CompleteParams;
    if (params) {
      this.orderId = params.orderId;
    }
  }

  private async onSubmit(): Promise<void> {
    if (this.submitting) {
      return;
    }
    if (this.faultCause.trim() === '' || this.measures.trim() === '') {
      promptAction.showToast({ message: '请填写故障原因和处理措施' });
      return;
    }
    const hours = Number.parseFloat(this.workHoursText);
    if (Number.isNaN(hours) || hours < 0) {
      promptAction.showToast({ message: '工时请填写不小于 0 的数字' });
      return;
    }
    this.submitting = true;
    try {
      const token = AppStorage.get<string>('accessToken') ?? '';
      const result = await completeOrder(token, this.orderId,
        this.faultCause.trim(), this.measures.trim(), hours);
      if (result.code !== 0) {
        promptAction.showToast({ message: result.message === '' ? '提交失败，请稍后再试' : result.message });
        return;
      }
      promptAction.showToast({ message: '完工已提交，等待业主验收' });
      router.back();
    } catch (err) {
      promptAction.showToast({ message: '连接不上服务，请检查网络后重试' });
    } finally {
      this.submitting = false;
    }
  }

  build() {
    Column() {
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
        Text('完工提交')
          .fontSize(17)
          .fontWeight(FontWeight.Bold)
          .fontColor(this.C_INK)
          .layoutWeight(1)
      }
      .width('100%')
      .padding({ left: 12, right: 20, top: 8, bottom: 8 })

      Scroll() {
        Column() {
          Text('故障原因')
            .fontSize(13)
            .fontWeight(FontWeight.Medium)
            .fontColor(this.C_INK)
            .margin({ top: 16, bottom: 10 })
          TextArea({ placeholder: '如：阀芯老化磨损导致关闭不严', text: this.faultCause })
            .fontSize(15)
            .fontColor(this.C_INK)
            .backgroundColor(Color.White)
            .borderRadius(12)
            .height(90)
            .onChange((v: string) => {
              this.faultCause = v;
            })

          Text('处理措施')
            .fontSize(13)
            .fontWeight(FontWeight.Medium)
            .fontColor(this.C_INK)
            .margin({ top: 18, bottom: 10 })
          TextArea({ placeholder: '如：更换全铜阀芯，测试无渗漏', text: this.measures })
            .fontSize(15)
            .fontColor(this.C_INK)
            .backgroundColor(Color.White)
            .borderRadius(12)
            .height(90)
            .onChange((v: string) => {
              this.measures = v;
            })

          Text('工时（小时）')
            .fontSize(13)
            .fontWeight(FontWeight.Medium)
            .fontColor(this.C_INK)
            .margin({ top: 18, bottom: 10 })
          TextInput({ text: this.workHoursText })
            .fontSize(15)
            .fontColor(this.C_INK)
            .backgroundColor(Color.White)
            .borderRadius(12)
            .height(46)
            .type(InputType.Number)
            .onChange((v: string) => {
              this.workHoursText = v;
            })

          Button(this.submitting ? '正在提交' : '提交完工')
            .width('100%')
            .height(50)
            .borderRadius(25)
            .backgroundColor(this.C_INK)
            .fontColor(Color.White)
            .fontSize(16)
            .fontWeight(FontWeight.Medium)
            .margin({ top: 26 })
            .enabled(!this.submitting)
            .onClick(() => {
              this.onSubmit();
            })

          Text('提交后工单进入待验收，业主确认后完结')
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
      .align(Alignment.Top)
    }
    .width('100%')
    .height('100%')
    .backgroundColor(this.C_PAPER)
  }
}

class CompleteParams {
  orderId: number = 0;
}

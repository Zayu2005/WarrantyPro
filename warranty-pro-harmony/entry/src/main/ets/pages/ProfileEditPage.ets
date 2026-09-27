import { promptAction, router } from '@kit.ArkUI';
import { changePassword, updateProfile } from '../common/ApiClient';

/**
 * 编辑资料 / 修改密码：基本信息（姓名、手机号）与密码修改两个分区，各自保存。
 */
@Entry
@Component
struct ProfileEditPage {
  @State realName: string = (AppStorage.get<string>('realName') ?? '');
  @State phone: string = '';
  @State oldPassword: string = '';
  @State newPassword: string = '';
  @State confirmPassword: string = '';
  @State savingInfo: boolean = false;
  @State savingPwd: boolean = false;

  private readonly C_INK: string = '#103B3A';
  private readonly C_MOSS: string = '#2E6D67';
  private readonly C_PAPER: string = '#F2F5F3';
  private readonly C_MIST: string = '#DCE7E3';
  private readonly C_MUTED: string = '#8A968F';

  private token(): string {
    return AppStorage.get<string>('accessToken') ?? '';
  }

  private async onSaveInfo(): Promise<void> {
    if (this.savingInfo) {
      return;
    }
    if (this.realName.trim() === '') {
      promptAction.showToast({ message: '姓名不能为空' });
      return;
    }
    this.savingInfo = true;
    try {
      const result = await updateProfile(this.token(), this.realName.trim(), this.phone.trim());
      if (result.code !== 0) {
        promptAction.showToast({ message: result.message === '' ? '保存失败，请稍后再试' : result.message });
        return;
      }
      AppStorage.setOrCreate('realName', this.realName.trim());
      promptAction.showToast({ message: '资料已更新' });
    } catch (err) {
      promptAction.showToast({ message: '连接不上服务，请检查网络' });
    } finally {
      this.savingInfo = false;
    }
  }

  private async onSavePassword(): Promise<void> {
    if (this.savingPwd) {
      return;
    }
    if (this.oldPassword === '' || this.newPassword === '') {
      promptAction.showToast({ message: '请填写原密码和新密码' });
      return;
    }
    if (this.newPassword.length < 6) {
      promptAction.showToast({ message: '新密码至少 6 位' });
      return;
    }
    if (this.newPassword !== this.confirmPassword) {
      promptAction.showToast({ message: '两次输入的新密码不一致' });
      return;
    }
    this.savingPwd = true;
    try {
      const result = await changePassword(this.token(), this.oldPassword, this.newPassword);
      if (result.code !== 0) {
        promptAction.showToast({ message: result.message === '' ? '修改失败，请稍后再试' : result.message });
        return;
      }
      promptAction.showToast({ message: '密码已修改，下次登录请使用新密码' });
      this.oldPassword = '';
      this.newPassword = '';
      this.confirmPassword = '';
    } catch (err) {
      promptAction.showToast({ message: '连接不上服务，请检查网络' });
    } finally {
      this.savingPwd = false;
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
        Text('编辑资料')
          .fontSize(17)
          .fontWeight(FontWeight.Bold)
          .fontColor(this.C_INK)
          .layoutWeight(1)
      }
      .width('100%')
      .padding({ left: 12, right: 20, top: 8, bottom: 8 })

      Scroll() {
        Column() {
          // ---- 基本信息 ----
          Column() {
            Text('基本信息')
              .fontSize(15)
              .fontWeight(FontWeight.Medium)
              .fontColor(this.C_INK)
              .width('100%')
              .margin({ bottom: 14 })

            Text('姓名')
              .fontSize(12)
              .fontColor(this.C_MUTED)
              .width('100%')
            TextInput({ text: this.realName, placeholder: '姓名' })
              .fontSize(15)
              .fontColor(this.C_INK)
              .backgroundColor(Color.White)
              .borderRadius(12)
              .height(46)
              .margin({ top: 6, bottom: 12 })
              .onChange((v: string) => {
                this.realName = v;
              })

            Text('手机号（联系方式）')
              .fontSize(12)
              .fontColor(this.C_MUTED)
              .width('100%')
            TextInput({ text: this.phone, placeholder: '选填' })
              .fontSize(15)
              .fontColor(this.C_INK)
              .backgroundColor(Color.White)
              .borderRadius(12)
              .height(46)
              .type(InputType.PhoneNumber)
              .margin({ top: 6 })
              .onChange((v: string) => {
                this.phone = v;
              })

            Button(this.savingInfo ? '正在保存' : '保存资料')
              .width('100%')
              .height(46)
              .borderRadius(23)
              .backgroundColor(this.C_INK)
              .fontColor(Color.White)
              .fontSize(15)
              .fontWeight(FontWeight.Medium)
              .margin({ top: 18 })
              .enabled(!this.savingInfo)
              .onClick(() => {
                this.onSaveInfo();
              })
          }
          .alignItems(HorizontalAlign.Start)
          .width('100%')
          .padding(18)
          .borderRadius(14)
          .backgroundColor(Color.White)
          .shadow({ radius: 6, color: 'rgba(16,59,58,0.08)', offsetY: 2 })

          // ---- 修改密码 ----
          Column() {
            Text('修改密码')
              .fontSize(15)
              .fontWeight(FontWeight.Medium)
              .fontColor(this.C_INK)
              .width('100%')
              .margin({ bottom: 14 })

            Text('原密码')
              .fontSize(12)
              .fontColor(this.C_MUTED)
              .width('100%')
            TextInput({ placeholder: '原密码', text: this.oldPassword })
              .type(InputType.Password)
              .fontSize(15)
              .fontColor(this.C_INK)
              .backgroundColor(Color.White)
              .borderRadius(12)
              .height(46)
              .margin({ top: 6, bottom: 12 })
              .onChange((v: string) => {
                this.oldPassword = v;
              })

            Text('新密码（6~32 位）')
              .fontSize(12)
              .fontColor(this.C_MUTED)
              .width('100%')
            TextInput({ placeholder: '新密码', text: this.newPassword })
              .type(InputType.Password)
              .fontSize(15)
              .fontColor(this.C_INK)
              .backgroundColor(Color.White)
              .borderRadius(12)
              .height(46)
              .margin({ top: 6, bottom: 12 })
              .onChange((v: string) => {
                this.newPassword = v;
              })

            Text('确认新密码')
              .fontSize(12)
              .fontColor(this.C_MUTED)
              .width('100%')
            TextInput({ placeholder: '再次输入新密码', text: this.confirmPassword })
              .type(InputType.Password)
              .fontSize(15)
              .fontColor(this.C_INK)
              .backgroundColor(Color.White)
              .borderRadius(12)
              .height(46)
              .margin({ top: 6 })
              .onChange((v: string) => {
                this.confirmPassword = v;
              })

            Button(this.savingPwd ? '正在修改' : '修改密码')
              .width('100%')
              .height(46)
              .borderRadius(23)
              .backgroundColor(this.C_MOSS)
              .fontColor(Color.White)
              .fontSize(15)
              .fontWeight(FontWeight.Medium)
              .margin({ top: 18 })
              .enabled(!this.savingPwd)
              .onClick(() => {
                this.onSavePassword();
              })
          }
          .alignItems(HorizontalAlign.Start)
          .width('100%')
          .padding(18)
          .borderRadius(14)
          .backgroundColor(Color.White)
          .shadow({ radius: 6, color: 'rgba(16,59,58,0.08)', offsetY: 2 })
          .margin({ top: 14 })
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

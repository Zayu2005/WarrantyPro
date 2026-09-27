import { HomePage } from '../components/HomePage';
import { ServicePage } from '../components/ServicePage';
import { OrdersPage } from '../components/OrdersPage';
import { ProfilePage } from '../components/ProfilePage';

/**
 * 主容器：自定义底部导航（内容区切换 + 文字导航栏）。
 * 业主 4 Tab（首页/服务/订单/我的），师傅 3 Tab（首页/订单/我的）。
 *
 * 注意：导航项必须在 build 中直接调用 @Builder（不能放进 ForEach，
 * 否则丢失 this 绑定导致状态变化不刷新）；内容区直接引用 this.current。
 */
@Entry
@Component
struct MainPage {
  @State role: string = (AppStorage.get<string>('role') ?? 'OWNER');
  @State current: number = 0;
  private tabs: string[] =
    this.role === 'OWNER' ? ['首页', '服务', '订单', '我的'] : ['首页', '订单', '我的'];

  private readonly C_INK: string = '#103B3A';
  private readonly C_AMBER: string = '#E8930C';
  private readonly C_PAPER: string = '#F2F5F3';
  private readonly C_MIST: string = '#DCE7E3';
  private readonly C_MUTED: string = '#8A968F';

  private isOwner(): boolean {
    return this.role === 'OWNER';
  }

  private tabLabel(index: number): string {
    return this.tabs[index];
  }

  @Builder
  NavItem(index: number) {
    Column({ space: 4 }) {
      Row()
        .width(16)
        .height(3)
        .borderRadius(2)
        .backgroundColor(this.current === index ? this.C_AMBER : Color.Transparent)
      Text(this.tabLabel(index))
        .fontSize(12)
        .fontWeight(this.current === index ? FontWeight.Bold : FontWeight.Regular)
        .fontColor(this.current === index ? this.C_INK : this.C_MUTED)
    }
    .width('100%')
  }

  build() {
    Column() {
      // ---- 内容区：直接按 this.current 分支 ----
      Column() {
        if (this.current === 0) {
          HomePage()
        } else if (this.isOwner() && this.current === 1) {
          ServicePage()
        } else if ((this.isOwner() && this.current === 2) || (!this.isOwner() && this.current === 1)) {
          OrdersPage()
        } else {
          ProfilePage()
        }
      }
      .layoutWeight(1)

      Divider()
        .color(this.C_MIST)

      // ---- 底部导航：显式声明，不走 ForEach ----
      Row() {
        Column() {
          this.NavItem(0)
        }
        .layoutWeight(1)
        .onClick(() => {
          this.current = 0;
        })

        Column() {
          this.NavItem(1)
        }
        .layoutWeight(1)
        .onClick(() => {
          this.current = 1;
        })

        if (this.isOwner()) {
          Column() {
            this.NavItem(2)
          }
          .layoutWeight(1)
          .onClick(() => {
            this.current = 2;
          })

          Column() {
            this.NavItem(3)
          }
          .layoutWeight(1)
          .onClick(() => {
            this.current = 3;
          })
        } else {
          Column() {
            this.NavItem(2)
          }
          .layoutWeight(1)
          .onClick(() => {
            this.current = 2;
          })
        }
      }
      .width('100%')
      .height(58)
      .backgroundColor(Color.White)
    }
    .width('100%')
    .height('100%')
    .backgroundColor(this.C_PAPER)
  }
}

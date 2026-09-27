import { HomePage } from '../components/HomePage';
import { ServicePage } from '../components/ServicePage';
import { OrdersPage } from '../components/OrdersPage';
import { ProfilePage } from '../components/ProfilePage';

/**
 * 主容器：自定义底部导航（内容区切换 + 文字导航栏）。
 * 业主 4 Tab（首页/服务/订单/我的），师傅 3 Tab（首页/订单/我的）。
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

  @Builder
  NavItem(label: string, index: number) {
    Column({ space: 4 }) {
      Row()
        .width(16)
        .height(3)
        .borderRadius(2)
        .backgroundColor(this.current === index ? this.C_AMBER : Color.Transparent)
      Text(label)
        .fontSize(12)
        .fontWeight(this.current === index ? FontWeight.Bold : FontWeight.Regular)
        .fontColor(this.current === index ? this.C_INK : this.C_MUTED)
    }
    .width('100%')
  }

  @Builder
  TabContentByLabel(label: string) {
    if (label === '首页') {
      HomePage()
    } else if (label === '服务') {
      ServicePage()
    } else if (label === '订单') {
      OrdersPage()
    } else {
      ProfilePage()
    }
  }

  build() {
    Column() {
      Column() {
        this.TabContentByLabel(this.tabs[this.current])
      }
      .layoutWeight(1)

      Divider()
        .color(this.C_MIST)

      Row() {
        ForEach(this.tabs, (label: string, idx: number) => {
          Column() {
            this.NavItem(label, idx)
          }
          .layoutWeight(1)
          .onClick(() => {
            this.current = idx;
          })
        }, (label: string) => label)
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

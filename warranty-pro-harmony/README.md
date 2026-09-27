# warranty-pro-harmony · 鸿蒙端 App

数字化物业保修平台鸿蒙端（HarmonyOS NEXT / ArkTS）。业主与维修师傅共用一个 App，登录后按角色渲染工作台。

## 当前状态

- ✅ 登录页（对接后端 `POST /api/v1/auth/login`，Preferences 持久化令牌、记住用户名、演示账号一键填充）
- ⏳ 业主工作台 / 师傅工作台（开发中）

## 运行

1. 启动后端（warranty-pro-server，默认 8080）；
2. 手机 / 模拟器与电脑连**同一 Wi-Fi**，确认 `entry/src/main/ets/common/ApiClient.ets` 里 `BASE_URL` 是电脑的局域网 IPv4（`ipconfig` 查看，当前默认 `10.10.10.6`）；
3. DevEco Studio 打开工程 → Sync → Run；或 CLI 构建：

```bash
# Git Bash（路径按本机 DevEco 安装位置调整）
export DEVECO_SDK_HOME="E:\\IDE\\DevEco Studio\\sdk"
export PATH="/e/IDE/DevEco Studio/tools/node:$PATH"
node "/e/IDE/DevEco Studio/tools/hvigor/bin/hvigorw.js" \
  --mode module -p module=entry@default -p product=default assembleHap --no-daemon
```

演示账号：`owner`（业主）/ `kefu`（客服）/ `shifu`（师傅），密码均为 `123456`。

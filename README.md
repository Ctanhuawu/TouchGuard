<p align="center">
  <img src="docs/assets/touchguard_vtuber_logo.png" width="540" alt="TouchGuard VTuber Logo" />
</p>

<h1 align="center">🛡️ TouchGuard (触控守护)</h1>

<p align="center">
  <strong>专为 Android 打造的现代、硬核、多级触控防误触与屏幕守护工具</strong>
</p>

<p align="center">
  <a href="https://github.com/Ctanhuawu/TouchGuard/releases"><img src="https://img.shields.io/github/v/release/Ctanhuawu/TouchGuard?color=blue&label=Release" alt="Latest Release" /></a>
  <img src="https://img.shields.io/badge/Android-8.0%2B%20(API%2026%2B)-green.svg" alt="Android Version" />
  <img src="https://img.shields.io/badge/Design-Miuix%20%7C%20Material%203-orange.svg" alt="Design System" />
  <img src="https://img.shields.io/badge/Language-Kotlin%20%7C%20Jetpack%20Compose-purple.svg" alt="Tech Stack" />
  <img src="https://img.shields.io/badge/Localization-zh--CN%20%7C%20zh--TW%20%7C%20en-teal.svg" alt="Localization" />
</p>

---

## 📖 简介 / Introduction

**TouchGuard（触控守护）** 是一款极具现代感且兼备底层硬核特性的 Android 触控拦截与屏幕状态保持工具。

无论你是在：
- 🚿 **洗澡/雨中观影**：杜绝水滴、飞沫引发的频繁跳跃、误触快进；
- 👶 **小孩看动画**：彻底锁死触控，防止幼儿胡乱点击、退至主屏或乱按拨号；
- 🎮 **挂机/刷剧/展示**：防止误碰屏幕中断游戏挂机或翻转跳出；
- 🏃 **口袋放歌/录音**：避免衣物摩擦唤醒屏幕与误触停止。

TouchGuard 提供从 **Linux 底层驱动独占**、**Shizuku 特权接口** 到 **免 Root 无障碍顶层遮罩** 的完整防御梯队，并首创 **双生设计语言**（Miuix HyperOS 质感 / Material 3 原生风）与多套物理按键应急解除机制，兼具绝对的拦截可靠性与优雅的交互体验。

---

## ⚡ 核心特性 / Features

### 1. 🛡️ 三大触控锁定策略（自适应多运行环境）

| 锁定策略 | 核心技术原理 | 特权要求 | 拦截级别与特性 |
| :--- | :--- | :---: | :--- |
| **底层硬件独占** | Linux Kernel `EVIOCGRAB` ioctl 独占 `/dev/input/event*` | **Root** | **绝对物理级拦截**。内核层彻底阻断触控与触控笔数据，零耗电、防穿透。 |
| **特权状态接管** | `IStatusBarService` 特权 Binder 禁用手势 + 透明全屏遮罩 | **Shizuku** | **免 Root 特权拦截**。彻底冻结顶部下拉通知栏、控制中心与全面屏导航手势，保留时钟电量显示。 |
| **顶层遮罩拦截** | `TYPE_ACCESSIBILITY_OVERLAY` (Layer 31) / 全屏浮窗 | **免 Root** | **开箱即用**。通过系统无障碍或悬浮窗建立全屏透明拦截面，吞噬全屏触控。 |

### 2. 🎮 物理按键智能应急解除（防误触与误锁死）
- 采用 **`MediaSession` 顶层按键劫持 + 窗口交互双重捕获**，锁定期间屏幕无法操作亦能精准响应实体键。
- **丰富的解除机制支持**：
  - 🔄 **连续双击音量下键**（默认推荐）
  - 🔢 **连续三击音量下键**（极佳防误触）
  - 🔼 **连续双击音量上键**
  - 🔀 **组合键：「音量加」接着「音量减」**
  - 🔀 **组合键：「音量减」接着「音量加」**
- **智能音量放行**：开启「不屏蔽音量按键」后，锁定期间依然可以通过音量键正常增减媒体音量，同时保留快速连击解锁。
- **自定义判定窗口**：按键连击时间阈值自由调节（100ms ~ 2000ms），兼顾灵敏度与防误触。

### 3. 📺 画面与屏幕保持策略
- ☀️ **保持屏幕常亮**：锁定期间申请前台 WakeLock，防止观看剧集或挂机时设备自动休眠熄屏。
- 🔄 **屏幕方向锁定**：支持 **跟随系统**、**锁定当前朝向**、**强制竖屏**、**强制横屏** 四种自适应模式。
- 💡 **亮度锁定**：固定屏幕锁定瞬间的背光亮度，防止环境光突变引发亮度闪烁。
- 🕶️ **沉浸隐藏系统栏**：一键隐藏顶部状态栏与手势导航小白条，全屏沉浸纯净视野。
- 💊 **锁定状态指示胶囊**：屏幕顶部可选悬浮胶囊，实时显示锁定状态与对应按键提示。
- 🔒 **熄屏安全自动解除**：支持按下电源键熄屏后自动解除触控锁，避免盲操困扰。

### 4. 🎨 双生设计系统与个性化体验
- **双 UI 风格一键切换**：
  - **Miuix 风格**：小米 HyperOS / MIUI 原生视觉规范，采用大圆角卡片、平滑过渡动画与毛玻璃模糊。
  - **Material 3 风格**：Google 最新 Material You 规范，纯正卡片流与现代排版。
- **5 套精心调校主题配色**：默认经典、深海湛蓝、薄荷翠绿、暖阳活力、曜石深黑。
- **OLED 纯黑深色模式**：深色模式深度适配，节电且暗光不刺眼。
- **三语言完整国际化**：原生支持 **简体中文 (zh-CN)**、**繁體中文 (zh-TW)** 与 **英语 (en)**。

### 5. 🚀 快捷系统集成
- **QS Tile 快捷磁贴**：下拉系统控制中心，一键触发触控锁定 / 解除。
- **常驻通知快捷面板**：前台守护通知支持卡片轻触直达锁定/解锁，并附带主界面快捷入口。
- **内置版本更新检查**：支持官方 GitHub Releases API 检索与国内高速镜像源（GhProxy / FastGit）直链下载。

---

## 🛠️ 技术架构 / Tech Architecture

```
com.ccwait.touchguard
├── model/                # 业务模型（语言、按键策略、屏幕方向、日志管理）
├── notification/         # 系统常驻通知与 Action 交互调度
├── receiver/             # 广播接收器（息屏监听、通知动作派发）
├── service/              # 前台守护服务 (TouchGuardForegroundService) 与 QS 磁贴 (TouchGuardTileService)
├── strategy/             # 核心锁定策略实现
│   ├── KernelEvgrabStrategy.kt      # Linux EVIOCGRAB 底层硬件独占 (Root)
│   ├── ShizukuLockTaskStrategy.kt   # IStatusBarService 特权级状态栏/手势接管 (Shizuku)
│   ├── WindowOverlayStrategy.kt     # 无障碍/浮窗全屏拦截 (免 Root)
│   ├── TouchLockOverlayView.kt      # 全屏透明渲染通道与手势排除层
│   └── TouchLockManager.kt          # 核心策略外观模式与统一分发调度
├── system/               # 底层系统与厂商适配
│   ├── adaptation/                  # 设备环境与控制面板下拉折叠策略
│   ├── AudioVolumeHelper.kt         # 音量按键拦截与真实音量流调度
│   ├── OverlayPermissionHelper.kt   # 悬浮窗与 AppOps 特权静默授权
│   └── ShizukuTaskLockHelper.kt     # IStatusBarService 动态 Binder 代理
├── ui/                   # Jetpack Compose UI
│   ├── components/                  # 通用组件、更新弹窗、导航栏
│   ├── pages/                       # 首页、策略页、日志页、设置页 (Miuix & Material 双实现)
│   ├── theme/                       # 主题调色板与配色引擎
│   └── util/                        # 动态本地化多语言管理
└── update/               # 自动更新检测与分发通道
```

- **开发语言**: Kotlin 2.0+ (JvmTarget 21)
- **UI 框架**: Jetpack Compose + Compose BOM
- **UI 组件库**: [`top.yukonga.miuix.kmp`](https://github.com/miuix-kotlin-multiplatform/miuix) + `androidx.compose.material3`
- **特权接口**: [`dev.rikka.shizuku:api`](https://github.com/RikkaApps/Shizuku) (Shizuku UID 2000 特权)
- **隐藏 API**: [`org.lsposed.hiddenapibypass:hiddenapibypass`](https://github.com/LSPosed/AndroidHiddenApiBypass)

---

## 📦 编译与构建 / Build & Setup

### 前置环境需求
- **Android Studio**: Koala / Ladybug 或更高版本
- **JDK 版本**: OpenJDK 21 或更高
- **最低支持系统**: Android 8.0 (API Level 26)
- **目标编译版本**: Target SDK 34 / Compile SDK 35

### 编译步骤

```bash
# 1. 克隆代码仓库
git clone https://github.com/Ctanhuawu/TouchGuard.git
cd TouchGuard

# 2. 编译调试版 APK
./gradlew assembleDebug

# 3. 安装到已连接的 Android 设备
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## ❓ 常见问题 / FAQ

<details>
<summary><strong>Q: 锁定了屏幕后，我该如何解锁？</strong></summary>

> 默认情况下，**快速连续按两次【音量减】按键** 即可解除锁定。你也可以在「锁定策略」页面更改为三击音量减、双击音量加或组合键，或开启「熄屏自动解除」在按电源键熄屏时自动恢复。
</details>

<details>
<summary><strong>Q: 三种锁定策略，我应该选择哪一种？</strong></summary>

> 1. **有 Root 环境**：强烈推荐 **Root 底层硬件独占**，拦截彻底、杜绝任何误触且极致省电；
> 2. **有 Shizuku**：推荐 **Shizuku 特权锁定**，无需重启 Root，能完美锁死全面屏侧滑返回、上滑回到桌面与顶部下拉面板；
> 3. **普通非 Root 设备**：使用 **无障碍 / 悬浮遮罩**，开启无障碍服务后即可获得顶层触控拦截能力。
</details>

<details>
<summary><strong>Q: 锁定期间我想调节视频播放声音怎么办？</strong></summary>

> 进入应用「锁定策略」页面，开启 **「不屏蔽音量按键」** 选项即可。开启后，单次按下音量键将正常调节媒体音量，只有在快速连续按下时才会触发解锁判断。
</details>

---

## 📄 开源许可证 / License

本项目采用 [MIT License](LICENSE) 开源协议。

---

## 🤝 致谢与参考 / Credits

- [Miuix KMP](https://github.com/miuix-kotlin-multiplatform/miuix) - 优秀的 Kotlin Multiplatform MIUI 风格 UI 组件库
- [Shizuku](https://github.com/RikkaApps/Shizuku) - 优雅强大的 Android 特权 Binder 桥接体系
- [MaterialKolor](https://github.com/jordond/MaterialKolor) - 强大的 Material You 动态调色盘引擎
- [AndroidHiddenApiBypass](https://github.com/LSPosed/AndroidHiddenApiBypass) - 纯净高效的 Android 隐藏 API 限制绕过方案

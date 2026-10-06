<p align="center">
  <img src="docs/assets/touchguard_vtuber_logo.png" width="540" alt="TouchGuard VTuber Logo" />
</p>

<h1 align="center">🛡️ TouchGuard (触控守护)</h1>

<p align="center">
  <strong>专为 Android 打造的防误触与屏幕触控锁定工具</strong>
</p>

<p align="center">
  <a href="https://github.com/Ctanhuawu/TouchGuard/releases"><img src="https://img.shields.io/github/v/release/Ctanhuawu/TouchGuard?color=blue&label=Release" alt="Latest Release" /></a>
  <img src="https://img.shields.io/badge/Android-8.0%2B-green.svg" alt="Android Version" />
  <img src="https://img.shields.io/badge/Design-Miuix%20%7C%20Material%203-orange.svg" alt="Design System" />
  <img src="https://img.shields.io/badge/License-MIT-blue.svg" alt="License" />
</p>

---

## 📖 简介 / Introduction

TouchGuard 是一个专为 Android 设备开发的防误触与屏幕锁定工具。在洗澡/雨中观影、小孩玩手机看动画、游戏挂机或口袋放歌时，锁定屏幕触控，避免误触跳出或中断。

---

## 📌 功能一览 / Features

- **屏幕触控拦截**：锁定后屏幕不再响应点击与滑动。
- **物理按键解锁**：通过机身实体按键快速解锁，支持双击音量减、三击音量减、双击音量加，以及加减组合按键。
- **支持锁定中调音量**：开启「不屏蔽音量按键」后，看视频/听歌时单次按下音量键仍可正常调节声音，只有快速连击时才会触发解锁。
- **屏幕状态保持**：支持锁定期间保持屏幕常亮、固定当前屏幕方向（或强制横竖屏）、锁定当前背光亮度、隐藏系统栏。
- **快捷开关**：支持下拉通知栏快捷磁贴（QS Tile）以及常驻通知栏一键锁定/解锁。
- **双 UI 界面风格**：提供小米 HyperOS 风格（Miuix）与原生 Android 风格（Material 3），可随时切换。支持 5 种调色盘与深色模式。
- **多语言**：原生支持简体中文、繁體中文与英文。

---

## ⚙️ 三种锁定策略说明 / Strategies

应用提供三种触控拦截机制，可根据设备环境自由选择：

| 策略 | 实现原理 | 权限要求 | 特点与局限说明 |
| :--- | :--- | :---: | :--- |
| **Root 模式** | 通过 Linux 内核 `EVIOCGRAB` 独占 `/dev/input/event*` 输入节点 | **Root** | **拦截最彻底**。内核层阻断触控与触控笔，完全杜绝任何穿透，且对系统性能无额外负担。<br>*注意：需要设备已解锁并获取 Root 权限。* |
| **Shizuku 模式** | 通过 Shizuku 动态调用系统 `IStatusBarService` 接口冻结手势 + 全屏透明遮罩 | **Shizuku** | **免 Root 推荐**。能够有效锁死全面屏侧滑返回、上滑回桌面以及顶部下拉控制中心。<br>*注意：需要设备上已安装并启动 Shizuku 服务。* |
| **无障碍模式** | 使用系统无障碍顶层悬浮窗 (`TYPE_ACCESSIBILITY_OVERLAY`) 覆盖全屏 | **免 Root** | **开箱即用**。只需在系统设置中开启无障碍服务即可使用。<br>*局限：受 Android 原生权限限制，部分机型可能无法拦截系统顶部的下拉状态栏或边缘手势。* |

---

## ⚠️ 使用注意与防锁死说明 / Precautions

1. **如何解锁？**
   - 默认解锁方式为：**快速连续按两次【音量减】按键**。
   - 你也可以在「锁定策略」页面根据习惯调整为三击或组合键，连击的有效响应间隔时间（默认 500ms）也可自由调节。

2. **万一按键失效或忘记怎么解锁？（防锁死兜底）**
   - 应用默认开启了 **「熄屏自动解除」**。
   - 如果遇到任何意外无法解锁，只需**按一下手机电源键熄屏**，屏幕熄灭时应用会自动释放触控锁，再次点亮屏幕即可正常操作。

3. **边看视频边调声音：**
   - 在策略设置里开启「不屏蔽音量按键」选项。开启后，单次按音量键会正常增减系统音量，不会触发解锁。

4. **后台保活建议：**
   - 使用无障碍或 Shizuku 模式时，建议在系统设置中允许 TouchGuard **自启动**，并将省电策略设为**无限制 / 忽略电池优化**，避免服务在后台被杀导致失效。

---

## 🛠️ 编译与构建 / Build

* **环境要求**：Android Studio Hedgehog / Ladybug+，JDK 21，最低支持 Android 8.0 (API 26)。
* **编译命令**：
  ```bash
  git clone https://github.com/Ctanhuawu/TouchGuard.git
  cd TouchGuard
  ./gradlew assembleDebug
  ```

---

## 📄 开源许可证 / License

本项目采用 [MIT License](LICENSE) 开源协议。

---

## 🤝 致谢 / Credits

- [Gemini](https://deepmind.google/technologies/gemini/) - AI 结对全栈编程与协作
- [Miuix KMP](https://github.com/miuix-kotlin-multiplatform/miuix) - Kotlin Multiplatform MIUI 风格 UI 组件库
- [Shizuku](https://github.com/RikkaApps/Shizuku) - Android 特权 API 调用框架
- [MaterialKolor](https://github.com/jordond/MaterialKolor) - Material You 动态调色盘
- [AndroidHiddenApiBypass](https://github.com/LSPosed/AndroidHiddenApiBypass) - Android 隐藏 API 访问绕过库

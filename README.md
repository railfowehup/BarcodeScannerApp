# 📱 条码扫码器 - BarcodeScanner

一款基于 **CameraX + ML Kit** 的 Android 高速条码扫码器，支持快速移动物体上的条码识别。

## ✨ 功能特性

- ⚡ **高速识别** — 使用 ML Kit 条码 API，支持快速闪过的物体
- 📷 **CameraX 相机** — 720p 高帧率分析，`STRATEGY_KEEP_ONLY_LATEST` 只处理最新帧
- 🔦 **闪光灯控制** — 暗光环境一键补光
- 📳 **震动 + 声音反馈** — 扫码成功即时提醒
- 🎯 **去重机制** — 同一码 2 秒内不重复触发
- 📐 **扫描框动画** — 绿色扫描线上下循环移动
- ✅ **结果展示** — 显示条码格式和内容，支持继续扫描

## 📋 支持的条码格式

| 格式 | 说明 |
|------|------|
| EAN-13 | 商品条码（最常见） |
| EAN-8 | 商品条码（缩短版） |
| UPC-A | 北美商品条码 |
| UPC-E | 北美商品条码（缩短版） |
| Code 39 | 工业条码 |
| Code 93 | 工业条码 |
| Code 128 | 物流/仓储条码 |
| ITF | 交叉二五条码 |
| Codabar | 图书馆/血库条码 |
| QR Code | 二维码 |
| Data Matrix | 二维矩阵码 |
| PDF417 | 堆叠式条码 |
| Aztec | 二维码 |
| 更多... | 支持所有 ML Kit 条码格式 |

## 🛠️ 技术栈

- **CameraX** — 相机预览 + 图像分析
- **ML Kit Barcode Scanning** — 条码识别
- **Material Design** — UI 组件
- **Gradle 8.13** — 构建工具
- **Java 21** — 开发语言

## 🚀 快速开始

### 环境要求

- Android Studio 或 VS Code + Java 扩展
- JDK 21+
- Android SDK 34+
- 一台 Android 真机（API 26+）

### 构建安装

```bash
# 克隆项目
git clone git@github.com:railfowehup/BarcodeScannerApp.git
cd BarcodeScannerApp

# 构建 Debug APK
./gradlew assembleDebug

# 安装到手机
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 📸 截图

| 扫码界面 | 识别成功 | 闪光灯 |
|:---:|:---:|:---:|
| ![扫码界面](screenshots/scanning.png) | ![识别成功](screenshots/result.png) | ![闪光灯](screenshots/flashlight.png) |

## 📄 许可证

MIT License

# 膳灵 NutriMuse · Android 客户端

原生 Android 客户端（Kotlin + Jetpack Compose），对接项目后端 Spring Boot API，覆盖全部功能：
**AI 流式对话 / 饮食记录 / 营养分析 / 偏好食物 / 目标设定 / 个人资料 / AI 用量统计**。

UI 采用与 Web 端一致的黑白灰极简风格，品牌名"膳灵"。

---

## 功能

底部导航 5 tab：

| Tab | 功能 |
|---|---|
| 对话 | AI 流式对话（打字机效果、一键停止、恢复历史、清空）、今日概览 |
| 记录 | 按日查看饮食记录，添加 / 编辑 / 删除（餐次、食物、热量、三大营养素） |
| 分析 | 日报 / 周报切换，热量与三大营养素统计卡、进度、智能小结 |
| 偏好 | 偏好食物卡片管理（名称 / 分类 / 每100g营养 / 备注），AI 个性化推荐数据源 |
| 我的 | 每日目标设定、个人资料、AI 用量统计看板、退出登录 |

登录 / 注册：JWT 无状态认证，token 本地持久化（DataStore），接口自动携带 `Authorization: Bearer`。

---

## 技术栈

- Kotlin 2.0 + Jetpack Compose（Material3，黑白灰主题）
- Retrofit2 + kotlinx.serialization（REST）
- OkHttp 流式读取 SSE（`/api/agent/chat/stream`，解析 `data:` 行 / `[FULL_RESULT]` / `[DONE]`，与 Web 端 useSseChat 同逻辑）
- DataStore Preferences（token 持久化）
- ViewModel + StateFlow + Navigation Compose
- AGP 8.5 / Gradle 8.7 / compileSdk 35 / minSdk 26

---

## 构建与运行（需要 Android Studio）

本机未内置 Android SDK，请在安装 **Android Studio** 后执行：

1. **安装 Android Studio**（含 Android SDK）：
   - 从 https://developer.android.com/studio 下载并安装
   - SDK Manager 中安装 **Android SDK Platform 35**（首次打开项目时按提示安装）

2. **打开项目**：Android Studio → `Open` → 选择本目录 `android/`
   - 首次打开会自动下载 Gradle 8.7 与依赖（需要网络，耐心等待）

3. **连接设备**：
   - **模拟器**：Tools → Device Manager 创建 AVD 并启动
   - **真机**：手机开启"开发者选项 → USB 调试"，数据线连接

4. **配置后端地址**（关键）：
   - 默认 `http://10.0.2.2:8080/`（模拟器访问宿主机的固定地址），模拟器直接用即可
   - **真机**需要改成电脑的局域网 IP：编辑 `app/build.gradle.kts` 中
     ```kotlin
     buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080/\"")
     ```
     改为 `"\"http://192.168.x.x:8080/\""`（你的电脑 IP），然后 **Build → Rebuild**。
   - 手机与电脑需在同一 Wi-Fi。
   - **本机当前已配置**：BASE_URL = `http://10.200.174.54:8080/`（WLAN 的 IP，2026-09-01 查询），
     防火墙已添加 8080 入站放行规则（"NutriMuse Backend 8080"）。
   - ⚠️ 局域网 IP 是 DHCP 分配的，**路由器重启后可能变化**：App 连不上后端时先在电脑跑
     `ipconfig` 看新 IP，同步改 BASE_URL 后 Rebuild。
   - ⚠️ 校园网/企业网 Wi-Fi 若开了 **AP 隔离**，手机将无法访问电脑——换手机热点（电脑连手机热点）即可。

5. **启动后端**（在项目根目录）：
   ```bash
   # 先在 .env 填好 DEEPSEEK_API_KEY / JWT_SECRET，然后
   mvn -s .mvn-online-settings.xml spring-boot:run
   ```
   后端跑在 8080。

6. **运行**：点击绿色 ▶ Run。登录 / 注册账号即可使用。

---

## 项目结构

```
android/
├── app/src/main/java/com/dietagent/android/
│   ├── DietAgentApp.kt          # Application + 手动 DI 容器
│   ├── MainActivity.kt          # 启动路由（登录/注册/主页）
│   ├── data/
│   │   ├── remote/              # ApiService（全部接口）+ DTO + AuthInterceptor + SseClient
│   │   ├── local/TokenStore.kt  # token 持久化
│   │   └── repo/                # 六个 Repository
│   └── ui/
│       ├── theme/               # 黑白灰主题
│       ├── auth/                # 登录/注册
│       ├── home/                # 底部导航
│       ├── chat/                # AI 对话（SSE 流式）
│       ├── diet/                # 饮食记录
│       ├── analysis/            # 营养分析
│       ├── favorites/           # 偏好食物
│       └── profile/             # 目标/资料/用量
└── README_android.md
```

---

## 常见问题

- **连接不上后端 / 超时**：先确认后端已在 8080 启动；真机确认 BASE_URL 是电脑局域网 IP 且已 Rebuild；检查手机与电脑同网段。
- **明文 http 被拦**：开发期已在 `AndroidManifest.xml` 开启 `android:usesCleartextTraffic="true"`，无需额外配置。
- **对话提示"发送太频繁"**：后端对对话接口按用户限流（默认 10 次/分钟），稍等再试。
- **AI 记录 / 图片**：图片富媒体卡片在安卓端以文本显示；"动嘴记录"、个性化推荐、偏好注入等 AI 能力与 Web 端一致。

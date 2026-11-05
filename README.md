# BizSpeak · 商务英语口语陪练（Android）

一个基于 **Jetpack Compose + 本地 Ollama** 的 Android 应用，用于练习**商务英语口语对话**。
内置「商务电话 / 会议 / 客户接待 / 面试 / 社交 / 价格谈判」等场景，可按 **CEFR 分级（默认初级 A2）** 自适应难度，AI 实时对话并给出**纠错**与**词汇点拨**，支持语音输入与 TTS 朗读。

> **灵感来源 / 许可证说明**：本项目的架构与「分级自适应系统提示词」思路参考自开源项目
> [MehdiKoceir/English_tutor_speaking](https://github.com/MehdiKoceir/English_tutor_speaking)
> （采用其设计思想，但**全部代码为独立重写**，新包名 `com.bizspeak.app`）。
> 原项目仓库**未附带 LICENSE 文件**（默认保留所有权利），因此本项目**不复制其源码**，
> 仅借鉴方案，可自由 fork / 改造 / 自用。

---

## 功能

- 🎭 6 个商务场景：电话、会议、接待、面试、社交、谈判
- 📊 CEFR 分级适配：入门 A1 → 初级 A2（默认）→ 中级 B1 → 中高级 B2 → 高级 C1+
- 📚 提示词对齐《剑桥商务标准英语教程》（Business Benchmark）初级/中级：每个场景在两档下有不同的
  主题域、功能语言与核心词汇域（详见 `PromptBuilder.unitFocus()`）
- 🎙️ 语音输入（**Vosk 离线识别**，内置 40MB 英文小模型；可选外部 1.8GB 大模型提升精度）
- 🔊 TTS 朗读 AI 回复（自动跳过纠错/词汇标记行）
- ✏️ AI 实时语法纠错 + 💡 商务词汇点拨
- 💻 后端用**本地 Ollama**，离线、免费、隐私友好

## 技术栈

| 层 | 技术 |
|---|---|
| UI | Jetpack Compose (Material 3) |
| 架构 | MVVM + 单向数据流 |
| 网络 | OkHttp（Ollama `/api/chat` 流式 NDJSON） |
| 语音 | **Vosk 离线识别**（`com.alphacephei:vosk-android:0.3.47`，模型内置 assets）+ TextToSpeech |
| 存储 | DataStore Preferences（设置） |
| 构建 | Gradle 8.14.3 · AGP 8.13.0 · Kotlin 2.2.10 · KSP |
| 最低/编译/目标 SDK | minSdk 24 · compileSdk/targetSdk 37 · buildTools 36.0.0 |

## 目录结构

```
bizspeak/
├── app/src/main/java/com/bizspeak/app/
│   ├── MainActivity.kt            # 入口 + 导航 + ViewModel 装配
│   ├── data/
│   │   ├── models.kt              # 消息 / 分级 / 场景模型
│   │   ├── PromptBuilder.kt      # 分级自适应商务系统提示词
│   │   ├── SettingsStore.kt       # DataStore 设置
│   │   ├── OllamaClient.kt        # 流式调用 Ollama
│   │   └── TutorRepository.kt     # 对话 + 纠错解析
│   ├── speech/
│   │   ├── SpeechManager.kt       # 语音识别封装
│   │   └── TtsManager.kt          # 朗读封装
│   ├── ui/screens/               # Home / Chat / Settings
│   └── viewmodel/                # ChatViewModel / SettingsViewModel
```

## 构建与运行

### 1. 准备环境
- 安装 **Android Studio**（含 Android SDK，compileSdk 34 / minSdk 24）
- 本机需有 **JDK 17+**（项目用 17）

### 2. 准备 Ollama（电脑端）
```bash
# 安装 Ollama 后拉取一个模型
ollama pull llama3.1:8b        # 或 qwen2.5:7b / gemma2:9b
# 确保开启监听（默认 11434）
ollama serve
```

### 3. 打开并构建
- Android Studio → Open → 选择 `bizspeak/` 目录
- 等待 Gradle 同步
- 连接真机或启动模拟器
- 运行 `app` 模块（或 `Build → Build Bundle(s) / APK(s)`）

> 首次构建若无 Gradle 发行版，会自动下载 Gradle 8.9（wrapper 已配置）。

### 4. 连接 Ollama 地址
- **模拟器**：先执行 `adb reverse tcp:11434 tcp:11434`，App 地址用默认 `http://127.0.0.1:11434`
  （注意：模拟器重启后 reverse 会失效，需重跑；本仓库实测 `10.0.2.2` 常被防火墙/NAT 拦截，reverse 更稳）
- **真机**：填电脑在局域网中的 IP，如 `http://192.168.1.20:11434`
  （确保电脑防火墙放行 11434 端口）
- 在 App「设置」页修改地址、模型名、导师口音、朗读语速

### 5. 语音识别模型

App 内置 **vosk-model-small-en-us-0.15**（40MB），开箱即用。

可选：换 **大模型**（精度显著更高，适合真机日常使用）：

```bash
# 1) 下载并解压 vosk-model-en-us-0.22
#    https://alphacephei.com/vosk/models → vosk-model-en-us-0.22.zip （约 1.74GB）
# 2) 解压后【务必删除两个可选重打分组件】，否则加载时占用 3.4GB 内存会被系统 OOM 杀掉：
#    - rescore/   （G.carpa 1.8GB + G.fst 63MB）
#    - rnnlm/     （final.raw 129MB 等）
#    删完后模型约 606MB，识别质量不受影响（核心是声学模型 70MB + HCLG.fst 471MB）
# 3) 推到设备 App 专属外部目录
adb push vosk-model-en-us-0.22 /sdcard/Android/data/com.bizspeak.app/files/
```

> **踩坑记录**：完整模型加载会吃掉 3.45GB 内存，2GB 或 4GB 内存的设备上会被
> `lowmemorykiller` 直接 kill（表现为点麦克风后 UI 卡住、麦永远红色）。
> 删除 `rescore/` 与 `rnnlm/` 后内存降至约 1GB，实测可稳定运行。
> 若用模拟器，还建议把 AVD 内存调到 4GB 以上（`hw.ramSize=4096` 或更高）。

App 启动识别时会**优先加载该目录**的大模型；目录不存在则回退内置小模型。

## 教材对齐（Business Benchmark）

`PromptBuilder.unitFocus()` 为每个场景在**初级/中级**两档下分别定义了：
- **主题域**（Themes）——对应教材相关单元的话题（电话留言、投诉处理、企业文化、营销、谈判等）
- **功能语言**（Functional language）——该场景的目标句型
- **核心词汇域**（Vocabulary field）——该单元的词汇范围

规则：
- 初级（BEGINNER/ELEMENTARY）→《Business Benchmark Elementary》(A2)
- 中级（INTERMEDIATE/UPPER）→《Business Benchmark Intermediate》(B1-B2)
- 仅借鉴教材公开的课程大纲主题与通用功能语言，**不复制教材原文/课文/习题**

想调整某场景的教学重点，直接改 `unitFocus()` 里对应分支即可。

## 如何定制到《剑桥商务·初级》单元

`PromptBuilder.kt` 的 `scenarioGuide()` 与 `levelGuide()` 是核心。
把教材单元主题（电话预约、会议发言、接待来访、简介公司等）映射为 `Scenario` 即可：
- 在 `models.kt` 的 `Scenario` 枚举里新增/调整条目（改 `starter` 开场白）
- 在 `PromptBuilder.scenarioGuide()` 增补对应场景的话术要求
- 把默认 `Level.ELEMENTARY`（A2）与教材进度对齐

## 已知限制
- 语音识别依赖设备自带的 `SpeechRecognizer`，部分机型需安装 Google 语音服务
- 真机语音识别通常需联网（系统服务）
- 本项目不含账号/云端同步；对话历史为内存态（重启清空），如需持久化可接入 Room（已留好版本依赖位）

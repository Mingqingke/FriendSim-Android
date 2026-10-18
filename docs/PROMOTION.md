# 推广文案集

> 用之前记得把 `Mingqingke` 换成你的用户名，截图路径按实际调整。

## 一、V2EX / Linux.do

**标题**：做了个 Android 聊天 App，能跟「一个像 TA 的朋友」继续聊下去

前阵子翻了翻老手机里的一些对话，突然想做个东西：如果能把某个人的说话方式留下来，让我还能跟 TA 聊两句，会是什么样。

于是有了 FriendSim。它不是聊天机器人，更像一个「角色扮演的通讯录」——你给它一张照片、一段声音、几句性格描述，它就按这个设定跟你聊。

**几个我觉得有意思的地方**

1. **不用填 Key 也能用**。自备 DeepSeek 就走上等马，没有就自动切免 Key 公共端点，全挂了还有本地预设话术。降级全程不打扰你，聊天界面一句报错都没有。

2. **人设是结构化的，不是一句「你是我朋友」**。关系、现状、性格、口头禅、喜好、雷区、共同回忆，都会被拼成一段强语境的 System Prompt。测试的时候我把雷区填成「香菜」，问它「今晚吃香菜火锅吗」，它回我「不去，我最讨厌那玩意儿」——那一刻有点毛骨悚然。

3. **UI 是照着 iMessage 抠的**。连续曲率气泡、泪滴尾巴（只有一串消息的最后一条才带）、共享元素从头像放大到聊天页顶部、分级触感。动画参数全部收在一个 MotionTokens 文件里，没有一页是自己另起一套弹簧。

4. **完全本机**。聊天记录 AES-256/GCM 加密，密钥交给 Keystore；照片和录音不出设备；一键备份成一个 zip，也可以一键扬了。

技术上是从 KernelSU 的 Manager 模块剥离出来的（只留 Compose + MVVM 那套壳，Root 相关的全拆了），所以是 GPL-3.0。

**地址**：https://github.com/Mingqingke/FriendSim-Android

欢迎拍砖。特别是动画节奏和气泡尾巴的形状，我自己看久了已经失去判断力了。

---

## 二、Reddit（r/AndroidDev 或 r/LocalLLaMA）

**Title**: I built an Android chat app where the "contact" is a persona you define (Compose, offline-first, no API key required)

**Body**:

Long story short: I wanted to see if a chat UI could feel like talking to a specific person rather than a chatbot. So I built one, and open-sourced it.

**What it is**

You create a "friend": photo, a few seconds of their voice, plus structured persona fields — relationship, current situation, speech style, catchphrases, things they love, things they hate, shared memories. That gets composed into a structured system prompt.

Messages degrade in three steps: your own key → keyless public endpoints → fully offline canned responses. The chat UI never shows an error; it just quietly switches.

**The parts I'm actually proud of**

- **Bubble geometry**: custom `Shape` with per-corner radii (the tail-side corner tightens) and a teardrop tail drawn with two quadratic béziers — 12×14dp, only on the last message of a run.
- **Shared element morph**: avatar grows from the list into the chat header via the official `SharedTransitionLayout` (still `@ExperimentalSharedTransitionApi` on Compose 1.12, worth the OptIn).
- **One motion source**: every spring lives in `MotionTokens.kt`. System "reduce motion" is respected globally.
- **Typing simulation**: replies are buffered, delayed by `1.5s + 30ms/char` (modulated by the persona's mood), then revealed 2 chars / 26ms.
- **Storage**: AES-256/GCM via a Keystore-held key, plus a zip backup that includes photos and voice clips. No Room — plain encrypted JSON files.
- **Diagnostics**: Chucker in debug builds only, so you can see exactly what goes over the wire.

**What I'd love feedback on**

The tail shape and the spring timings. Also: is the three-tier fallback the right default, or should a keyless public endpoint be opt-in?

Repo: https://github.com/Mingqingke/FriendSim-Android (GPL-3.0, APK in Releases)

---

## 三、Product Hunt

**Name**: FriendSim

**Tagline** (≤60 chars): `Talk to the friend you miss. No setup, no API key.`

**Description**:

FriendSim is a private chat app for one very specific person: the one you want to keep talking to.

Add a photo, a few seconds of their voice, and a handful of details — how you know each other, what they're like, what they can't stand — and the conversation starts to sound like them.

- Works out of the box. No account, no API key, no setup wizard. It uses free public endpoints by default and switches to fully offline replies if the network drops.
- Remembers. Long-term memory, a relationship level that changes how familiar it sounds, and a mood that affects how fast and how much it replies.
- Feels like a real chat app. iMessage-grade bubbles with teardrop tails, shared-element transitions, spring physics everywhere, and haptic feedback that's tuned per action.
- Yours only. Chats are encrypted on device, photos and voice never leave the phone, and one tap wipes everything.

Open source, GPL-3.0. Android 8.0+.

---

## 四、三条推广建议

1. **先录一段 10 秒的 GIF，再发任何帖子。**
   这个项目 80% 的卖点是「滑块手感」和「气泡形变」，静态图完全拍不出来。用 `adb shell screenrecord` 或手机录屏，截「新建朋友 → 展开深度档案 → 进聊天 → 收到逐字回复」这一段，压到 3MB 内放 README 顶部。同一个 GIF 在 V2EX、Reddit、PH 都能复用。

2. **发帖时间比文案重要。**
   V2EX 早上 9:30–10:30（通勤时段）和晚上 21:00–22:30 回复率最高，周二到周四最佳；Reddit r/AndroidDev 用美东时间上午 9–11 点，标题别带「my app」，用「I built…」或直接问技术问题更容易过；Product Hunt 必须美西时间 00:01 上线抢当天首页。

3. **找一个「小场景」做钩子，别喊口号。**
   「AI 陪伴」这个词已经被用烂了，没人点。改成一句话场景：「我把奶奶的语音喂进去，现在能听她喊我小名」或者「给异地恋做了个不会累的对话框」。同时准备好 Release 里的 APK 直链——愿意为一个 UI 装 1GB Android Studio 的人，比你想的少得多。

补充：**别在同一个账号连续发多个板块**。V2EX 和 Linux.do 隔一天发，Reddit 和 PH 隔一周，否则容易被当成 spam 折叠。

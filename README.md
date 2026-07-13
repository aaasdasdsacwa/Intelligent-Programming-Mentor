🎯 智能编程导师系统 (Smart Programming Tutor System)

Java Spring Boot Vue 3 Vite Docker

基于 Spring Boot 3.x + Vue 3 + Docker 隔离沙箱 + 大模型(LLM)流式驱动
构建的工业级全栈在线判题（OJ）与智能辅助学习导学平台。

本系统旨在打破传统 OJ 平台“只判对错，不教方法”的局限，将高安全性的多语言代码评测沙箱与全天候 AI 编程导师有机结合，构建出一个“看路线 -> 读精讲
-> 刷算法 -> AI 伴随诊断 -> 随时答疑”的现代化编程闭环生态 [1, 2]。

📐 系统架构与调用流

本系统在工程结构上采用完全的前后端分离设计，整体调用拓扑结构如下：

┌────────────────────────────────────────────────────────────────────────┐
│                       前端展示层 (Vite + Vue 3)                         │
│  ├─ 🎯 AI路径定制大屏   ├─ 📚 标准题库中心   ├─ 📜 个人评测历史           │
│  ├─ 💻 Monaco编辑器终端  ├─ 🤖 AI 智能答疑 (悬浮球)                      │
└──────────────────────────────────┬─────────────────────────────────────┘
│ HTTP / SSE / EventStream
▼
┌────────────────────────────────────────────────────────────────────────┐
│                      后端服务层 (Spring Boot 3.x)                       │
│  ├─ 🔑 用户安全服务  ├─ 🎯 智能路径引擎  ├─ 🐳 Docker调度判题核心        │
│  ├─ 🤖 AI 答疑/诊断   ├─ 💾 MyBatis-Plus 物理持久化                   │
└──────────────┬───────────────────┬──────────────────┬──────────────────┘
│                   │                  │
▼                   ▼                  ▼
┌───────────┐       ┌───────────┐      ┌───────────┐
│ Redis 缓存 │       │  MySQL 8  │      │ LLM 模型  │
│ 会话与限流  │       │ 5张核心表 │      │ (DeepSeek)│
└───────────┘       └───────────┘      └───────────┘
│
▼
┌───────────────────────────┐
│    多语言安全判题沙箱      │
│ ├─ Java  ├─ Python ├─ C++ │
│ ├─ Go    ├─ JS     └──────┤
└───────────────────────────┘

🚀 核心技术亮点

1. 🛡️ 工业级多语言安全判题沙箱 (Docker-Sandbox)

- 容器化物理安全隔离：基于 Docker-Java SDK 独立拉起微型 Linux 容器 [3.1.2]。通过配置 HostConfig
  强制硬限制最大内存（128MB）、单核 CPU，并完全封禁网络通信，防止外部网络攻击及挖矿、死循环脚本对宿主机的破坏。
- WSL2 POSIX 路径映射适配：针对 Windows + WSL2 本地开发环境下的挂载阻碍，自主编写了物理路径转换机制，自动将带有盘符和反斜杠的
  Windows 临时路径（如 D:\temp）重构清洗为符合 Linux 规范的绝对路径（如 /d/temp）并传入 Docker 绑定 [1]。
- 流阻塞超时（TLE）底层突破：针对传统 OJ 利用 withStdIn() 在宿主机与容器套接字传输数据时发生的 TLE（运行超时）挂起 Bug：
    - 避坑手段：关闭 Tty，并将容器默认常驻指令设置为 sleep 3600 [1, 2]。
    - 管道重定向：评测时在容器内部执行： sh -c "g++ ... -o /app/Main && /app/Main <
      /app/input.txt" 由容器内部系统底层执行输入重定向，当读取到文件末尾时自动触发物理
      EOF（结束符）信号闭合流，将评测耗时由超时 5000ms
      瞬间缩短到 20ms 以内 [1]！

2. 🤖 懒加载概念精讲与数据库缓存 (AI Lazy-Cache)

- 技术大纲联动：支持学生自由输入或联动选中想学的方向（如微服务、大前端），利用 LLM 定制 4 阶段的宏观技术概念路线（而非具体题干） [1]。
- 懒加载数据落盘：知识点详细精讲采用 Lazy-Load（延迟加载） 机制。用户首次点击卡片上的【简单介绍】时向 AI 发起请求，生成完毕后更新存入数据库
  path_node 表的 detail 缓存字段；后续用户点击直接秒级读取数据库缓存，避免网络等待，节省 100% 的 Token 重复调用开销
  [1, 2]。

3. 🤖 基于 SSE 协议的 AI 伴随 Code Review (SSE EventStream)

- 打字机流式输出：后端采用 Server-Sent Events（SSE）通信，利用原生 EventStream 将 AI Review
  的字符流实时推送到前端。
- 高阶缓冲区清洗：前端设计了缓冲区清洗机制（Buffer Cleaning），自动过滤并解析行级别的 event: complete
  等大模型信号，提供顺畅无闪烁的 1 对 1 编程导师代码审查体验。

4. 💾 前端持久化状态探针与内存保护

- 代码实时自动存盘：Monaco Editor 接入输入监听，代码实时以 code_problem_${id}_${lang} 分语言存入
  localStorage，防范意外断电或刷新导致的代码丢失 [1]。
- 本地通关状态探测（Client-side Cache）：题库列表页通过 getLocalProblemStatus
  探针自动读取并执行双保险引号过滤，无缝将通关状态渲染为已通过/未通过/未开始，零开发成本实现了原本需要复杂多表 SQL 联查的用户答题历史同步
  [1, 2]。
- 内存保护：在 Vue 页面销毁周期（onBeforeUnmount）中主动执行 editorInstance.dispose() 释放 Monaco
  实例，防范 Web Worker 泄露引发的浏览器 OOM（内存溢出）。

🛠️ 技术选型

1. 后端 (Backend)

- 核心开发框架：Spring Boot 3.x, Spring MVC, Spring Transaction（事务回滚保证路径节点进度点亮安全一致）
- ORM 框架：MyBatis-Plus 3.x（提供优雅的分页查询及物理操作）
- 中间件与数据库：Redis 5.x/6.x（Token保持与AI调用安全限流）、MySQL 8.0
- 大模型对接框架：LangChain4j (用于多轮对话、结构化 Schema 强类型约束输出)
- 沙箱客户端：Docker-Java Transport HTTPClient5, Docker-Java Core [3.1.2]

2. 前端 (Frontend)

- 构建与基底：Vite, Vue 3 (Composition API), Vue Router 4, Pinia
- UI 组件库：Element Plus, @element-plus/icons-vue
- 编辑器内核：Microsoft Monaco Editor (Microsoft VS Code 驱动内核)
- 网络请求：Axios, HTML5 Fetch & EventSource (EventStream 异步解析流)

🐳 多语言沙箱评测矩阵

在判题详情工作台中，系统支持 5 种主流语言的动态高亮、自拟用例调试运行以及提交评测：

| 编程语言           | 容器运行环境（Alpine 镜像）     | 容器内就地编译与执行 Pipeline 指令                                                        |
| :------------- | :-------------------- | :---------------------------------------------------------------------------- |
| **Java**       | `openjdk:17-alpine`   | `javac -encoding utf-8 /app/Main.java && java -cp /app Main < /app/input.txt` |
| **Python**     | `python:3.10-alpine`  | `python3 /app/Main.py < /app/input.txt`                                       |
| **C++**        | `frolvlad/alpine-gxx` | `g++ -O3 /app/Main.cpp -o /app/Main && /app/Main < /app/input.txt`            |
| **Go**         | `golang:1.20-alpine`  | `go run /app/Main.go < /app/input.txt`                                        |
| **JavaScript** | `node:18-alpine`      | `node /app/Main.js < /app/input.txt`                                          |

⚙️ 快速本地部署运行

1. 后端依赖环境启动

- 运行 Redis： 双击解压后的 redis-server.exe 即可，保持黑色控制台窗口在后台运行 [2.1.3]。
- 配置 Docker Desktop 2375 端口： 点击设置图标 \rightarrow General \rightarrow 勾选 Expose
  daemon on tcp://localhost:2375 without TLS 并应用重启 [3.1.2]。
- 预先拉取 5 种 Alpine 评测镜像（国内高速镜像加速开启状态下执行，用于实现秒级评测）：
  docker pull openjdk:17-alpine
  docker pull python:3.10-alpine
  docker pull frolvlad/alpine-gxx
  docker pull golang:1.20-alpine
  docker pull node:18-alpine

2. 配置国内合规镜像加速器（解决 pull python、g++ 时 TLE 或连接重置问题）

如果直连官方 Docker Hub 发生超时，请在 Docker Desktop 中点击 Settings \rightarrow Docker
Engine，在右侧黑色 JSON 配置中将 registry-mirrors 修改为国内目前活跃度最高的毫秒合规镜像源 [2.3.6]：

{
"registry-mirrors": [
"https://docker.1ms.run",
"https://dockerproxy.link",
"https://hub.rat.dev"
],
"builder": {
"gc": {
"defaultKeepStorage": "20GB",
"enabled": true
}
},
"experimental": false
}

- 点击 Apply & restart 保存，随后在 cmd 中可享受秒级的高速拉取 [2.3.6]。

3. 数据库初始化

- 在您的 MySQL 中创建名为 smart_tutor 的数据库。
- 依次执行项目后端 db/init_db.sql 中的 SQL 建表及数据初始化脚本，会自动为您注入 5 种语言相关的 15 道多语言经典测试题，并为
  path_node 完成 detail 缓存列的建立。

4. 启动后端项目

- 使用 IDEA 导入 Maven 项目。
- 打开 src/main/resources/application.yml，配置好您的本地 MySQL 和 Redis 连接。
- 配置大模型 API Key：将配置文件中的大模型/DeepSeek 模型的 API Key（当前以 your-placeholder-key
  占位表示）替换为 您自己在 DeepSeek 开放平台申请的真实有效 API Key：
  # 示例配置：
  ai:
  api-key: your-real-deepseek-api-key-here # 💡 替换为您自己的真实 Key
- 运行 SmartTutorApplication.java 启动类。

5. 启动前端项目

cd tutor-frontend
npm install
npm run dev

- 打开浏览器访问 http://localhost:5173 即可进入系统，开始您的智能编程导学与评测之旅！


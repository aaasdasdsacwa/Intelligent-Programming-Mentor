# 🎯 智能编程导师系统 (Smart Programming Tutor System)

基于 **Spring Boot** + **Vue 3** + **Docker 隔离沙箱** + **大模型(LLM)流式驱动** 构建的工业级全栈在线判题（OJ）与智能辅助学习导学平台。

---

## 🚀 核心技术亮点

### 1. 🛡️ 多语言安全隔离判题沙箱 (Docker-Sandbox)
* **安全容器隔离**：自主研发基于 Docker-Java SDK 的安全代码沙箱，通过控制 CPU 核数(1L)、物理内存(128MB)及完全禁用网络等手段，杜绝恶意代码对宿主机的攻击 [3.1.2]。
* **多语言动态调度**：动态适配 **Java, Python, C++, Go, JavaScript** 5 种语言的镜像分发路由 [1]。
* **POSIX 挂载适配**：完美解决 Windows WSL2 盘符反斜杠挂载兼容异常 [1]。
* **Linux 管道式编译重定向**：通过在 Linux 容器内执行 `sh -c "g++ ... < input.txt"` 闭环了网络 Socket 流未发送终止符导致的运行超时(TLE)大坑 [1]。

### 2. 🤖 懒加载概念精讲与数据库缓存 (AI Lazy-Cache)
* **大模型定制路线**：支持学生自由输入或联动选中想学的方向（如高并发、微服务等），利用大模型定制 4 阶段的概念路线 [1]。
* **懒加载数据落盘**：知识点详细介绍（简单介绍）采用**懒加载**。首次点击时向 AI 请求并同步写入 `path_node` 表的 `detail` 缓存字段中，后续点击**秒级从数据库读取，节省 100% 的 Token 重复开销** [1]。

### 3. 🤖 AI 服务器流式 Code Review (SSE EventStream)
* **打字机流式输出**：前端基于原生的 Fetch 和 EventStream 协议，健壮解析 AI SSE 字符块。
* **高阶缓冲区清洗**：设计了对行进行拼装及清除大模型自定义 complete 信号的缓冲区清洗算法，展示打字机流式 Code Review。

### 4. 💾 前端持久化与状态探测联动
* **本地状态缓存**：在前端通过 `localStorage` 缓存实现了**编辑代码实时自动存盘**，以及**“已通过/未通过/未开始”答题状态的完美探测联动**，极大减轻了后端多表联查的压力 [1, 2]。
* **路由缓存（KeepAlive）**：采用 Vue 3 的 `<keep-alive>` 机制，实现 Tab 切换时状态毫秒级驻留，页面永不白屏。

---

## 🛠️ 技术栈选型

* **后端 (Backend)**:
    * 核心框架: Spring Boot 3.x, Spring MVC
    * 数据库操作: MyBatis-Plus, MySQL 8.0, Redis 5.x/6.x
    * 沙箱集成: Docker-Java SDK, Docker Desktop (Port 2375) [3.1.2]
    * AI 框架: LangChain4j, OpenAI / DashScope API
    * 辅助工具: Hutool, Lombok

* **前端 (Frontend)**:
    * 核心框架: Vite, Vue 3, Vue Router 4, Pinia
    * UI 组件库: Element Plus, @element-plus/icons-vue
    * 代码编辑器: Microsoft Monaco Editor (VS Code 核心)
    * 数据请求: Axios, Native EventSource / ReadableStream

---

## ⚙️ 快速本地部署运行

### 1. 后端依赖环境启动
* **启动 Redis**：双击运行 `redis-server.exe` [2.1.3]。
* **配置 Docker 2375 端口**：
  打开 Docker Desktop $\rightarrow$ Settings $\rightarrow$ General $\rightarrow$ 勾选 `Expose daemon on tcp://localhost:2375 without TLS` 并应用重启 [3.1.2]。
* **预先拉取评测镜像**（在 cmd 窗口执行以享受秒级评测）：
  ```bash
  docker pull openjdk:17-alpine
  docker pull python:3.10-alpine
  docker pull frolvlad/alpine-gxx
  docker pull golang:1.20-alpine
  docker pull node:18-alpine
2. 数据库初始化
   在 MySQL 中创建名为 smart_tutor 的库。
   依次执行 src/resources/db/ 目录下的 SQL 脚本，完成表结构与预设 15 道多语言经典算法题的初始化。
3. 启动后端项目
   打开项目，导入 Maven 依赖。
   在 application.yml 中配置好您的 MySQL、Redis 连接信息以及您的大模型 API Key。
   运行 SmartTutorApplication.java 启动类。
4. 启动前端项目
   code
   Bash
   cd tutor-frontend
   npm install
   npm run dev
   打开浏览器访问 http://localhost:5173/ 即可进入系统开始智能编程导学与练习！
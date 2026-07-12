<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'
// 核心：引入 Monaco 编辑器
import * as monaco from 'monaco-editor'

const route = useRoute()
const router = useRouter()

const problem = ref(null)
const submitLoading = ref(false)
const aiLoading = ref(false)

// 评测与 AI 选项卡控制
const activeTab = ref('judge') // 'judge' 评测台 / 'ai' AI 报告
const judgeResult = ref(null)
const aiReport = ref('')

// 编辑器实例与 DOM 节点
const editorContainer = ref(null)
let editorInstance = null
// 自拟输入参数绑定
const customInput = ref('')
const runLoading = ref(false)

// 🌟 1. 新增：支持的多语言选项
const selectedLang = ref('java')
const langOptions = [
  { label: 'Java (JDK 17)', value: 'java' },
  { label: 'Python (Python 3)', value: 'python' },
  { label: 'C++ (G++ 17)', value: 'cpp' },
  { label: 'Go (Golang 1.20)', value: 'go' },
  { label: 'JavaScript (Node.js)', value: 'javascript' }
]

// 🌟 2. 新增：多语言专属初始化代码模板
const defaultTemplates = {
  java: `import java.util.Scanner;\n\npublic class Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        // 在此编写你的 Java 算法逻辑\n        \n    }\n}`,
  python: `# 在此编写你的 Python 3 算法逻辑\nimport sys\n\nfor line in sys.stdin:\n    # 处理标准输入流\n    pass`,
  cpp: `#include <iostream>\nusing namespace std;\n\nint main() {\n    // 在此编写你的 C++ 17 算法逻辑\n    \n    return 0;\n}`,
  go: `package main\n\nimport "fmt"\n\nfunc main() {\n    // 在此编写你的 Go 算法逻辑\n    \n}`,
  javascript: `const readline = require('readline');\nconst rl = readline.createInterface({\n    input: process.stdin,\n    output: process.stdout\n});\n\nrl.on('line', (line) => {\n    // 在此编写你的 JavaScript 算法逻辑\n    \n});`
}

// 🌟 3. 新增：切换语言时的动态处理函数 [1]
const handleLangChange = (newLang) => {
  if (!editorInstance) return

  // 动态切换 Monaco 的语法高亮 [1]
  const monacoLangMap = {
    java: 'java',
    python: 'python',
    cpp: 'cpp',
    go: 'go',
    javascript: 'javascript'
  }
  monaco.editor.setModelLanguage(editorInstance.getModel(), monacoLangMap[newLang])

  // 读取/保存对应语言的本地代码 [1]
  const localKey = `code_problem_${route.params.id}_${newLang}`
  const savedCode = localStorage.getItem(localKey)
  editorInstance.setValue(savedCode || defaultTemplates[newLang])
}

// 执行自拟运行（动态传入所选语言）
const handleRunDebug = async () => {
  if (!editorInstance) return
  const userCode = editorInstance.getValue()
  if (!userCode.trim()) {
    ElMessage.warning('代码内容不能为空')
    return
  }

  runLoading.value = true
  activeTab.value = 'judge'
  judgeResult.value = null // 清空上一次结果

  try {
    const res = await axios.post('/api/judge/run', {
      code: userCode,
      language: selectedLang.value, // 💡 动态传入所选语言
      input: customInput.value
    })
    if (res.data.code === 0) {
      judgeResult.value = {
        status: res.data.data.status !== undefined ? res.data.data.status : 0,
        output: res.data.data.output,
        errorMsg: res.data.data.errorMsg,
        runTime: res.data.data.runTime,
        isDebug: true
      }
      ElMessage.success('调试代码运行完成')
    } else {
      ElMessage.error(res.data.message || '运行失败')
    }
  } catch (err) {
    ElMessage.error('无法连接沙箱运行，请确保本地 Docker 2375 或本地执行器可用')
  } finally {
    runLoading.value = false
  }
}

// 加载题目完整详情
const fetchProblem = async () => {
  try {
    const res = await axios.get(`/api/problem/get?id=${route.params.id}`)
    if (res.data.code === 0) {
      problem.value = res.data.data
    } else {
      ElMessage.error(res.data.message || '获取题目数据失败')
    }
  } catch (err) {
    ElMessage.error('无法连接后端服务，请检查网络')
  }
}

// 初始化 Monaco Editor
const initMonaco = () => {
  // 💡 优先读取本地对应语言缓存的代码，防止多语言覆盖 [1]
  const localKey = `code_problem_${route.params.id}_${selectedLang.value}`
  const savedCode = localStorage.getItem(localKey)

  editorInstance = monaco.editor.create(editorContainer.value, {
    value: savedCode || defaultTemplates[selectedLang.value],
    language: selectedLang.value,
    theme: 'vs-dark', // 经典极客深色主题
    automaticLayout: true,
    fontFamily: 'Consolas, "Fira Code", monospace',
    fontSize: 14,
    tabSize: 4,
    insertSpaces: true,
    minimap: { enabled: false }, // 禁用缩略图，让出开发视窗
    scrollbar: {
      vertical: 'visible',
      horizontal: 'visible'
    }
  })

  // 监听并实时持久化保存当前所选语言的代码 [1]
  editorInstance.onDidChangeModelContent(() => {
    const currentCode = editorInstance.getValue()
    const currentKey = `code_problem_${route.params.id}_${selectedLang.value}`
    localStorage.setItem(currentKey, currentCode) [1]
  })
}

// 提交代码进行 Docker 沙箱评测
// 提交代码进行 Docker 沙箱评测（🌟 完美隔离业务与网络报错版）
// 提交代码进行 Docker 沙箱评测（🌟 完整状态闭环 + 异常隔离版）
const handleSubmitCode = async () => {
  if (!editorInstance) return
  const userCode = editorInstance.getValue()
  if (!userCode.trim()) {
    ElMessage.warning('提交的代码内容不能为空')
    return
  }

  submitLoading.value = true
  activeTab.value = 'judge'
  judgeResult.value = null

  // 1. 仅让 try-catch 包裹网络请求本身，只捕获纯粹的网络超时或断连 [1]
  let res;
  try {
    res = await axios.post('/api/judge/submit', {
      problemId: route.params.id,
      code: userCode,
      language: selectedLang.value
    })
  } catch (err) {
    ElMessage.error('评测超时，请确保本地 Docker 正常启动且开启了 2375 端口')
    submitLoading.value = false
    return
  }

  // 2. 处理 UI 提示与本地缓存状态（避开 try-catch，绝不误触超时提示） [1]
  if (res && res.data.code === 0) {
    judgeResult.value = res.data.data

    // 安全防空取值
    const status = judgeResult.value ? judgeResult.value.status : -1
    const isAccepted = status === 0

    ElMessage({
      message: isAccepted ? '🟢 恭喜你，测试点全部通过！' : '🔴 答案未通过，可发起 AI 辅助诊断。',
      type: isAccepted ? 'success' : 'warning'
    })

    // 🌟 核心：更新本地答题状态，写入缓存 [1]
    const cacheKey = `solved_problem_${route.params.id}`
    if (isAccepted) {
      // 一旦通过，强制在本地设为已通过（1） [1]
      localStorage.setItem(cacheKey, '1')
      console.log(`[Debug] 写入本地通关状态成功！key: ${cacheKey}`)
    } else {
      // 如果未通过，且该题目之前【从未通过】过，才将其设为未通过（2） [1]
      // 💡 这能防止之前通过了，这次手抖写错了又把状态覆盖回未通过 [1]
      const previouslySolved = localStorage.getItem(cacheKey) === '1'
      if (!previouslySolved) {
        localStorage.setItem(cacheKey, '2')
        console.log(`[Debug] 写入本地【未通过】状态成功！key: ${cacheKey}`)
      }
    }

    // 智能联动进度（增加防空 null 判定）
    if (judgeResult.value && judgeResult.value.nodeUpdated) {
      ElMessage({
        message: `🎉 联动成功！已为你自动解锁并通关路线节点：【${judgeResult.value.updatedNodeName}】！`,
        type: 'success',
        duration: 6000
      })
    }
  } else {
    ElMessage.error(res ? (res.data.message || '评测失败') : '服务器无有效响应数据')
  }

  submitLoading.value = false
}

// 基于原生 Fetch + EventStream 健壮解析大模型 SSE 字符块
const handleAiReviewStream = async () => {
  if (!editorInstance) return
  const userCode = editorInstance.getValue()

  aiLoading.value = true
  activeTab.value = 'ai'
  aiReport.value = '🤖 编程导师正在深度审查你的源码及评测日志，请稍候...'

  try {
    const response = await fetch('/api/review/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        problemId: route.params.id,
        code: userCode,
        errorMsg: judgeResult.value ? judgeResult.value.errorMsg : ''
      })
    })

    if (!response.ok) {
      aiReport.value = '❌ 调用 AI 诊断接口异常。'
      aiLoading.value = false
      return
    }

    aiReport.value = '' // 清除提示语，准备接受打字机流式字符

    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = '' // 健壮性缓冲区，解决网络拆包/多字节 UTF-8 字符截断问题

    let isCompleteEvent = false

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')

      // 保留最后一个可能未接收完整的行在缓冲区
      buffer = lines.pop() || ''

      for (const line of lines) {
        const cleanLine = line.trim()
        if (!cleanLine) continue

        // 处理自定义事件头
        if (cleanLine.startsWith('event:')) {
          const eventName = cleanLine.substring(6).trim()
          if (eventName === 'complete') {
            isCompleteEvent = true
          }
        }

        // 处理核心数据行
        if (cleanLine.startsWith('data:')) {
          // 不要过滤多余的空格，保持 markdown 文档段落缩进
          const dataToken = cleanLine.substring(5)

          if (isCompleteEvent) {
            isCompleteEvent = false
            continue // 过滤完成时的多余信号
          }

          // 将实时生成的字符块写入文本框
          aiReport.value += dataToken

          // 自动平滑滚动 AI 诊断视图到底部
          await nextTick()
          scrollAiPanelToBottom()
        }
      }
    }
  } catch (err) {
    aiReport.value += '\n\n❌ 无法建立连接或大模型握手中断。'
  } finally {
    aiLoading.value = false
  }
}

// 平滑滚动
const aiPanelDom = ref(null)
const scrollAiPanelToBottom = () => {
  if (aiPanelDom.value) {
    aiPanelDom.value.scrollTop = aiPanelDom.value.scrollHeight
  }
}

onMounted(async () => {
  await fetchProblem()
  initMonaco()
})

onBeforeUnmount(() => {
  // 必须主动销毁 Monaco 实例，释放浏览器内存，防止 OOM
  if (editorInstance) {
    editorInstance.dispose()
  }
})
</script>

<template>
  <div class="problem-detail-workspace" v-if="problem">
    <!-- 头部简洁工作台栏 -->
    <header class="work-header">
      <div class="header-left" @click="router.push('/workspace/problems')" style="cursor:pointer">
        <el-icon size="20" color="#3b82f6"><ArrowLeft /></el-icon>
        <span class="back-link">退出练习并返回题库</span>
      </div>
      <div class="problem-current-title">
        <span>{{ problem.title }}</span>
      </div>
      <div class="header-right">
        <el-button type="success" size="small" icon="Compass" @click="router.push('/workspace/roadmap')">
          我的路线图
        </el-button>
      </div>
    </header>

    <!-- 主交互工作区 (左右分栏) -->
    <div class="work-body">
      <!-- 左侧：题目详情 -->
      <div class="left-desc-panel">
        <el-card class="desc-card" shadow="never">
          <div class="desc-header">
            <h3>{{ problem.title }}</h3>
            <div class="difficulty-and-tags">
              <el-tag v-if="problem.difficulty === 'easy'" type="success" effect="dark" size="small">简单</el-tag>
              <el-tag v-else-if="problem.difficulty === 'medium'" type="warning" effect="dark" size="small">中等</el-tag>
              <el-tag v-else type="danger" effect="dark" size="small">困难</el-tag>

              <el-tag v-for="tag in (problem.tags ? problem.tags.split(',') : [])" :key="tag" type="info" size="small">
                {{ tag.trim() }}
              </el-tag>
            </div>
          </div>

          <div class="desc-body">
            <h4 class="section-title">题目描述</h4>
            <!-- 保留原始排版格式 -->
            <p class="description-text">{{ problem.description }}</p>

            <div class="case-panel" v-if="problem.inputCase">
              <h4 class="section-title">测试输入</h4>
              <pre class="case-box">{{ problem.inputCase }}</pre>
            </div>

            <div class="case-panel" v-if="problem.outputCase">
              <h4 class="section-title">标准输出</h4>
              <pre class="case-box">{{ problem.outputCase }}</pre>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 右侧：编辑器 + 交互终端 -->
      <div class="right-develop-panel">
        <!-- 1. Monaco 代码编辑器部分 -->
        <div class="monaco-wrapper">
          <div class="editor-sub-header">
            <div class="lang-tag">
              <span class="green-glow-dot"></span>
              <!-- 🌟 升级：将原来的 Java(JDK17) 文本修改为 Element-Plus 的多语言下拉框 [1] -->
              <el-select
                  v-model="selectedLang"
                  placeholder="选择编程语言"
                  size="small"
                  style="width: 140px;"
                  @change="handleLangChange"
              >
                <el-option
                    v-for="item in langOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                />
              </el-select>
            </div>
            <div class="btn-group">
              <!-- 调试调试运行按钮 -->
              <el-button type="info" icon="Cpu" :loading="runLoading" :disabled="submitLoading" @click="handleRunDebug">
                自拟测试运行
              </el-button>
              <el-button type="success" icon="VideoPlay" :loading="submitLoading" :disabled="runLoading" @click="handleSubmitCode">
                提交并编译运行
              </el-button>
              <el-button type="primary" icon="MagicPen" :disabled="submitLoading || runLoading" :loading="aiLoading" @click="handleAiReviewStream">
                智能 AI 导师诊断
              </el-button>
            </div>
          </div>
          <!-- 编辑器挂载 DOM 容器 -->
          <div ref="editorContainer" class="monaco-container-box"></div>
        </div>

        <!-- 2. 评测控制台与 AI 报告 -->
        <div class="terminal-wrapper">
          <el-tabs v-model="activeTab" class="terminal-tabs">

            <!-- 评测结果控制台 -->
            <el-tab-pane label="🚀 OJ 评测控制台" name="judge">
              <!-- 自拟输入测试参数输入栏 -->
              <div class="custom-input-section">
                <span class="custom-input-label">自拟测试输入数据 (调试运行时生效)：</span>
                <el-input
                    v-model="customInput"
                    placeholder="若有自拟输入，可在此填写（例如输入：3 5），然后点击上方的【自拟测试运行】"
                    clearable
                />
              </div>

              <div v-if="!judgeResult" class="empty-terminal">
                <el-empty description="请在上方编写代码，点击 [自拟测试运行] 或 [提交并编译运行] 检验你的成果" :image-size="50" />
              </div>

              <div v-else class="terminal-success-or-error">
                <div class="terminal-status-header">
                  <!-- 自拟调试运行状态指示 -->
                  <template v-if="judgeResult.isDebug">
                    <span class="stat-green">⚙️ Debug Mode (自拟输入运行已完成)</span>
                  </template>
                  <template v-else>
                    <span v-if="judgeResult.status === 0" class="stat-green">🟢 Accepted (全部测试点通过)</span>
                    <span v-else-if="judgeResult.status === 1" class="stat-red">🔴 Wrong Answer (答案错误)</span>
                    <span v-else-if="judgeResult.status === 2" class="stat-yellow">🟡 Compile Error (编译失败)</span>
                    <span v-else-if="judgeResult.status === 3" class="stat-orange">🟠 Runtime Error (发生运行时异常)</span>
                    <span v-else class="stat-red">❌ Time Limit Exceeded (运行超时)</span>
                  </template>

                  <span class="run-info-time" v-if="judgeResult.runTime">
                    运行时间: <b>{{ judgeResult.runTime }}</b> ms
                  </span>
                </div>

                <!-- 报错信息展示 (终端模拟黑底) -->
                <div v-if="judgeResult.errorMsg" class="compile-error-terminal">
                  <div class="terminal-title">Console Error Output:</div>
                  <pre>{{ judgeResult.errorMsg }}</pre>
                </div>

                <!-- 输出结果比对 -->
                <div v-if="judgeResult.output" class="cases-comparison">
                  <div class="compare-item">
                    <span class="label-name">你的实际输出 (stdout)：</span>
                    <pre class="output-block">{{ judgeResult.output }}</pre>
                  </div>
                  <!-- 只有在非调试模式（正式提交评测）下，才展示预期答案比对框 -->
                  <div class="compare-item" v-if="!judgeResult.isDebug && judgeResult.expectedOutput">
                    <span class="label-name">标准答案预期输出：</span>
                    <pre class="output-block expected-block">{{ judgeResult.expectedOutput }}</pre>
                  </div>
                </div>
              </div>
            </el-tab-pane>

            <!-- AI 导师报告 -->
            <el-tab-pane label="🤖 AI 导师诊断报告" name="ai">
              <div ref="aiPanelDom" class="ai-report-view">
                <!-- 逐字打字机效果报告 -->
                <pre class="ai-markdown-text">{{ aiReport }}</pre>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 保持原 CSS 结构，在最下方增加 select 深度选择器，防止 dark 主题文字重叠 */
:deep(.editor-sub-header .el-input__wrapper) {
  background-color: #0c0a09 !important;
  box-shadow: 0 0 0 1px #1c1917 inset !important;
}
:deep(.editor-sub-header .el-input__inner) {
  color: #10b981 !important;
  font-weight: bold;
}

.problem-detail-workspace {
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.work-header {
  height: 48px;
  background-color: #0f172a; /* 深邃黑蓝，专业IDE外观 */
  color: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  border-bottom: 1px solid #1e293b;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #94a3b8;
  transition: color 0.2s;
}
.header-left:hover {
  color: #fff;
}
.back-link {
  font-size: 13px;
}
.problem-current-title {
  font-weight: bold;
  font-size: 14.5px;
  color: #3b82f6;
}
.work-body {
  flex: 1;
  display: flex;
  background-color: #0f172a; /* 统一暗色基调 */
  overflow: hidden;
}
.left-desc-panel {
  width: 40%;
  padding: 10px;
  box-sizing: border-box;
  height: 100%;
}
.right-develop-panel {
  width: 60%;
  padding: 10px 10px 10px 0;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 10px;
  height: 100%;
}
.desc-card {
  height: 100%;
  border-radius: 8px;
  border: 1px solid #1e293b;
  background-color: #1e293b; /* 保持与 IDE 融为一体的暗色调 */
  color: #e2e8f0;
  overflow-y: auto;
}
.problem-header h3 {
  margin: 0 0 10px 0;
  color: #fff;
}
.difficulty-and-tags {
  display: flex;
  gap: 6px;
}
.section-title {
  font-weight: bold;
  font-size: 14px;
  color: #fff;
  border-left: 3px solid #3b82f6;
  padding-left: 8px;
  margin: 20px 0 8px 0;
}
.description-text {
  font-size: 14px;
  line-height: 1.6;
  color: #cbd5e1;
  white-space: pre-wrap;
}
.case-box {
  background-color: #0f172a;
  border: 1px solid #1e293b;
  border-radius: 6px;
  padding: 10px;
  font-family: monospace;
  font-size: 12.5px;
  color: #38bdf8;
  margin: 0;
}
.monaco-wrapper {
  height: 60%;
  border-radius: 8px;
  border: 1px solid #1e293b;
  background-color: #1e1e1e;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.editor-sub-header {
  height: 38px;
  background-color: #181818;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 15px;
  color: #94a3b8;
  font-size: 12.5px;
}
.lang-tag {
  display: flex;
  align-items: center;
  gap: 6px;
}
.green-glow-dot {
  width: 6px;
  height: 6px;
  background-color: #10b981;
  border-radius: 50%;
  box-shadow: 0 0 8px #10b981;
}
.btn-group {
  display: flex;
  gap: 8px;
}
.monaco-container-box {
  flex: 1;
}
.terminal-wrapper {
  height: 40%;
  background-color: #1e293b;
  border: 1px solid #1e293b;
  border-radius: 8px;
  padding: 8px 16px;
  box-sizing: border-box;
  overflow-y: auto;
}
:deep(.el-tabs__item) {
  color: #94a3b8 !important;
}
:deep(.el-tabs__item.is-active) {
  color: #3b82f6 !important;
  font-weight: bold;
}
:deep(.el-tabs__active-bar) {
  background-color: #3b82f6 !important;
}
.empty-terminal {
  height: 100px;
  display: flex;
  justify-content: center;
  align-items: center;
}
.terminal-status-header {
  font-weight: bold;
  font-size: 15px;
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
}
.stat-green { color: #10b981; }
.stat-red { color: #ef4444; }
.stat-yellow { color: #eab308; }
.stat-orange { color: #f97316; }
.run-info-time {
  font-size: 12px;
  color: #94a3b8;
}
.custom-input-section {
  margin-bottom: 12px;
  border-bottom: 1px dashed #1e293b;
  padding-bottom: 12px;
}
.custom-input-label {
  font-size: 12px;
  color: #94a3b8;
  font-weight: bold;
  display: block;
  margin-bottom: 6px;
}
:deep(.custom-input-section .el-input__wrapper) {
  background-color: #0f172a !important;
  box-shadow: 0 0 0 1px #1e293b inset !important;
}
:deep(.custom-input-section .el-input__inner) {
  color: #38bdf8 !important;
  font-family: monospace;
}
.compile-error-terminal {
  background-color: #0c0a09;
  border: 1px solid #1c1917;
  border-radius: 6px;
  padding: 10px;
  margin-bottom: 12px;
}
.compile-error-terminal .terminal-title {
  font-size: 12px;
  color: #b91c1c;
  font-weight: bold;
  margin-bottom: 4px;
}
.compile-error-terminal pre {
  margin: 0;
  color: #ef4444;
  font-family: monospace;
  font-size: 12px;
  white-space: pre-wrap;
}
.cases-comparison {
  display: flex;
  gap: 15px;
}
.compare-item {
  flex: 1;
}
.label-name {
  font-size: 12px;
  color: #94a3b8;
  font-weight: bold;
}
.output-block {
  background-color: #0f172a;
  border: 1px solid #1e293b;
  padding: 8px;
  border-radius: 6px;
  font-size: 12.5px;
  color: #38bdf8;
  margin-top: 4px;
  min-height: 35px;
}
.expected-block {
  color: #10b981;
}
.ai-report-view {
  height: 150px;
  overflow-y: auto;
  padding-right: 5px;
}
.ai-markdown-text {
  font-family: inherit;
  font-size: 13.5px;
  line-height: 1.6;
  color: #e2e8f0;
  white-space: pre-wrap;
  margin: 0;
}
</style>
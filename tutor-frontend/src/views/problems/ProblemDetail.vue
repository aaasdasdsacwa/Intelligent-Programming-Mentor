<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'
// 核心：引入 Monaco Editor
import * as monaco from 'monaco-editor'

const route = useRoute()
const router = useRouter()

const problem = ref(null)
const submitLoading = ref(false)
const aiLoading = ref(false)

// 评测结果与 AI 诊断反馈
const activeTab = ref('judge') // 'judge' or 'ai'
const judgeResult = ref(null)
const aiReport = ref('') // AI 流式诊断文本存储

// 编辑器容器节点与实例
const editorContainer = ref(null)
let editorInstance = null

// 加载题目详情
const fetchProblem = async () => {
  try {
    const res = await axios.get(`/api/problem/get?id=${route.params.id}`)
    if (res.data.code === 0) {
      problem.value = res.data.data
    } else {
      ElMessage.error(res.data.message || '获取题目失败')
    }
  } catch (err) {
    ElMessage.error('无法连接后端服务')
  }
}

// 初始化 Monaco Editor
const initMonaco = () => {
  // 默认填充的 Java 模板代码
  const defaultCode = `import java.util.Scanner;\n\npublic class Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        // 在这里编写你的 Java 算法代码\n        \n    }\n}`

  editorInstance = monaco.editor.create(editorContainer.value, {
    value: defaultCode,
    language: 'java',
    theme: 'vs-dark', // 深色极客主题
    automaticLayout: true,
    fontFamily: 'Consolas, Courier New, monospace',
    fontSize: 14,
    tabSize: 4,
    minimap: { enabled: false } // 关闭缩略图，腾出屏幕空间
  })
}

// 提交代码进行 Docker 评测
const handleSubmit = async () => {
  if (!editorInstance) return
  const code = editorInstance.getValue()
  if (!code.trim()) {
    ElMessage.warning('代码内容不能为空')
    return
  }

  submitLoading.value = true
  activeTab.value = 'judge'
  judgeResult.value = null

  try {
    const res = await axios.post('/api/judge/submit', {
      problemId: route.params.id,
      code: code,
      language: 'java'
    })
    if (res.data.code === 0) {
      judgeResult.value = res.data.data
      ElMessage.success('代码评测运行完毕！')

      // 如果提示路线节点已经同步变绿
      if (judgeResult.value.nodeUpdated) {
        ElMessage({
          message: `🎉 恭喜！你已通关路线节点：【${judgeResult.value.updatedNodeName}】！`,
          type: 'success',
          duration: 6000
        })
      }
    } else {
      ElMessage.error(res.data.message || '评测失败')
    }
  } catch (err) {
    ElMessage.error('评测沙箱超时，请确保本地 Docker 已启动')
  } finally {
    submitLoading.value = false
  }
}

// 核心：基于原生 Fetch 读取 Server-Sent Events (SSE) 长连接字符流
const handleAiReview = async () => {
  if (!editorInstance) return
  const code = editorInstance.getValue()

  aiLoading.value = true
  activeTab.value = 'ai'
  aiReport.value = '🤖 智能 AI 导师正在对你的代码进行诊断，请稍候...'

  try {
    const response = await fetch('/api/review/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        problemId: route.params.id,
        code: code,
        // 如果判题出错了，把报错信息带给大模型分析
        errorMsg: judgeResult.value ? judgeResult.value.errorMsg : ''
      })
    })

    if (!response.ok) {
      aiReport.value = 'AI 诊断服务请求异常。'
      aiLoading.value = false
      return
    }

    aiReport.value = '' // 清空初始载入文本，准备流式写入
    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')

    // 循环监听服务端 SSE 字符块
    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      const chunk = decoder.decode(value, { stream: true })

      // SSE 标准数据格式为: data: token
      const lines = chunk.split('\n')
      for (const line of lines) {
        if (line.startsWith('data:')) {
          const dataToken = line.replace('data:', '').trim()
          if (dataToken === '') continue

          // 追加流式字符到报告中
          aiReport.value += dataToken
        }
      }
    }
  } catch (err) {
    aiReport.value = '\n❌ 无法连接 AI 诊断服务。'
  } finally {
    aiLoading.value = false
  }
}

onMounted(async () => {
  await fetchProblem()
  initMonaco()
})

onBeforeUnmount(() => {
  if (editorInstance) {
    editorInstance.dispose()
  }
})
</script>

<template>
  <div class="problem-detail-container" v-if="problem">
    <!-- 头部栏 -->
    <header class="navbar">
      <div class="logo">
        <el-icon size="24" color="#409EFF"><Platform /></el-icon>
        <span>在线智能编程练习</span>
      </div>
      <div>
        <el-button type="primary" link @click="router.push('/problems')">← 返回题库</el-button>
        <el-button type="success" link @click="router.push('/roadmap')">去路线图</el-button>
      </div>
    </header>

    <div class="detail-content">
      <!-- 左侧：题目详情展示 -->
      <div class="left-box">
        <el-card class="card-box">
          <div class="problem-header">
            <h2>{{ problem.title }}</h2>
            <div class="header-tags">
              <el-tag v-if="problem.difficulty === 'easy'" type="success" effect="dark">简单</el-tag>
              <el-tag v-else-if="problem.difficulty === 'medium'" type="warning" effect="dark">中等</el-tag>
              <el-tag v-else type="danger" effect="dark">困难</el-tag>
              <el-tag v-for="tag in (problem.tags ? problem.tags.split(',') : [])" :key="tag" type="info">{{ tag }}</el-tag>
            </div>
          </div>

          <!-- 题目正文 -->
          <div class="problem-body">
            <div class="section-title">📘 题目要求</div>
            <p class="markdown-desc">{{ problem.description }}</p>

            <div v-if="problem.inputCase" class="case-section">
              <div class="section-title">📥 评测输入样例</div>
              <pre class="code-block">{{ problem.inputCase }}</pre>
            </div>

            <div v-if="problem.outputCase" class="case-section">
              <div class="section-title">📤 预期输出样例</div>
              <pre class="code-block">{{ problem.outputCase }}</pre>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 右侧：Monaco 编辑器 + 结果诊断区 -->
      <div class="right-box">
        <!-- 上半部分：代码编辑器 -->
        <div class="editor-section">
          <div class="editor-header">
            <span>💻 Java (OpenJDK 17) 语言</span>
            <div class="actions">
              <el-button type="success" icon="VideoPlay" :loading="submitLoading" @click="handleSubmit">
                提交运行
              </el-button>
              <el-button type="primary" icon="Cpu" :disabled="submitLoading" :loading="aiLoading" @click="handleAiReview">
                智能 AI 诊断
              </el-button>
            </div>
          </div>
          <!-- 编辑器本体容器 -->
          <div ref="editorContainer" class="monaco-editor-body"></div>
        </div>

        <!-- 下半部分：评测结果与 AI 伴随诊断报告 -->
        <div class="result-section">
          <el-tabs v-model="activeTab" class="result-tabs">
            <el-tab-pane label="🚀 评测控制台" name="judge">
              <!-- 空置状态 -->
              <div v-if="!judgeResult" class="empty-result">
                <el-empty description="编写代码后，点击上方提交评测运行" :image-size="60" />
              </div>

              <!-- 真实判题结果 -->
              <div v-else class="result-panel">
                <div class="result-title">
                  <span v-if="judgeResult.status === 0" class="status-ac">🟢 Accepted (通过)</span>
                  <span v-else-if="judgeResult.status === 1" class="status-wa">🔴 Wrong Answer (答案错误)</span>
                  <span v-else-if="judgeResult.status === 2" class="status-ce">🟡 Compile Error (编译错误)</span>
                  <span v-else-if="judgeResult.status === 3" class="status-re">🟠 Runtime Error (运行时异常)</span>
                  <span v-else class="status-wa">❌ Time Limit Exceeded (运行超时)</span>
                  <span class="run-time" v-if="judgeResult.runTime">耗时: {{ judgeResult.runTime }} ms</span>
                </div>

                <div v-if="judgeResult.errorMsg" class="error-msg-box">
                  <div class="box-title">错误诊断：</div>
                  <pre>{{ judgeResult.errorMsg }}</pre>
                </div>

                <div v-if="judgeResult.output" class="outputs-wrapper">
                  <div class="output-item">
                    <span class="label">你的代码实际输出：</span>
                    <pre>{{ judgeResult.output }}</pre>
                  </div>
                  <div class="output-item">
                    <span class="label">预期标准输出：</span>
                    <pre>{{ judgeResult.expectedOutput }}</pre>
                  </div>
                </div>
              </div>
            </el-tab-pane>

            <el-tab-pane label="🤖 AI 编程导师报告" name="ai">
              <div class="ai-report-panel">
                <!-- 支持流式加载的 Markdown 文本 -->
                <pre class="markdown-report">{{ aiReport }}</pre>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.problem-detail-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.navbar {
  height: 50px;
  background-color: #fff;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: bold;
}

.detail-content {
  flex: 1;
  display: flex;
  overflow: hidden;
  background-color: #f1f5f9;
}

.left-box {
  width: 40%;
  padding: 12px;
  box-sizing: border-box;
}

.right-box {
  width: 60%;
  display: flex;
  flex-direction: column;
  padding: 12px 12px 12px 0;
  gap: 12px;
  box-sizing: border-box;
}

.card-box {
  height: 100%;
  border-radius: 8px;
  overflow-y: auto;
}

.problem-header h2 {
  margin: 0 0 10px 0;
  color: #1e293b;
}

.header-tags {
  display: flex;
  gap: 5px;
  margin-bottom: 15px;
}

.section-title {
  font-weight: bold;
  color: #1e293b;
  margin-top: 15px;
  margin-bottom: 8px;
  border-left: 4px solid #409EFF;
  padding-left: 8px;
}

.markdown-desc {
  font-size: 15px;
  line-height: 1.6;
  color: #334155;
  white-space: pre-wrap;
}

.code-block {
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 10px;
  font-family: monospace;
  font-size: 13px;
  margin: 0;
}

.editor-section {
  height: 60%;
  background-color: #1e1e1e;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.editor-header {
  height: 40px;
  background-color: #252526;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 15px;
  color: #cccccc;
  font-size: 13px;
}

.monaco-editor-body {
  flex: 1;
}

.result-section {
  height: 40%;
  background-color: #fff;
  border-radius: 8px;
  padding: 10px 15px;
  box-sizing: border-box;
  overflow-y: auto;
}

.result-tabs {
  height: 100%;
}

.empty-result {
  height: 150px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.result-panel {
  padding: 10px 0;
}

.result-title {
  font-weight: bold;
  font-size: 16px;
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
}

.status-ac { color: #10b981; }
.status-wa { color: #ef4444; }
.status-ce { color: #f59e0b; }
.status-re { color: #f97316; }

.run-time {
  font-size: 13px;
  color: #64748b;
  font-weight: normal;
}

.error-msg-box {
  background-color: #fef2f2;
  border: 1px solid #fee2e2;
  border-radius: 6px;
  padding: 10px;
  margin-bottom: 12px;
}

.error-msg-box pre {
  color: #b91c1c;
  font-size: 12px;
  margin: 0;
  white-space: pre-wrap;
}

.outputs-wrapper {
  display: flex;
  gap: 15px;
}

.output-item {
  flex: 1;
}

.output-item .label {
  font-size: 13px;
  font-weight: bold;
  color: #64748b;
}

.output-item pre {
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  padding: 8px;
  border-radius: 6px;
  font-size: 13px;
  margin-top: 5px;
}

.ai-report-panel {
  height: 160px;
  overflow-y: auto;
}

.markdown-report {
  font-family: inherit;
  font-size: 14px;
  line-height: 1.6;
  color: #1e293b;
  white-space: pre-wrap;
  margin: 0;
}
</style>
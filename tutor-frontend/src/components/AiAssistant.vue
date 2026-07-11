<script setup>
import { ref, nextTick } from 'vue'
import { ChatDotRound, Promotion, Close, Headset } from '@element-plus/icons-vue'
import axios from 'axios'

const visible = ref(false)
const inputMsg = ref('')
const loading = ref(false)
const chatList = ref([
  { sender: 'ai', text: '你好！我是你的 AI 编程导师。无论是路线中的概念、还是写代码遇到的 Bug，都可以随时问我！' }
])

const chatContainer = ref(null)

const toggleChat = () => {
  visible.value = !visible.value
  if (visible.value) {
    scrollToBottom()
  }
}

// 发送消息，调用后端的 QaController 接口
const handleSend = async () => {
  if (!inputMsg.value.trim() || loading.value) return

  const userText = inputMsg.value
  chatList.value.push({ sender: 'user', text: userText })
  inputMsg.value = ''
  loading.value = true
  scrollToBottom()

  // 临时放入一个 AI 占位对象
  const aiMsgIndex = chatList.value.push({ sender: 'ai', text: '导师正在思考中...' }) - 1

  try {
    // 这里采用流式或普通 POST 对接后端 QaController
    // 示例接口：POST /api/qa/chat 传入问答 DTO
    const res = await axios.post('/api/qa/chat', { question: userText })
    if (res.data.code === 0) {
      chatList.value[aiMsgIndex].text = res.data.data
    } else {
      chatList.value[aiMsgIndex].text = '❌ 导师开小差了，请稍后再试：' + res.data.message
    }
  } catch (err) {
    chatList.value[aiMsgIndex].text = '❌ 无法连接到问答服务器，请检查后端 QaService 是否正常启动。'
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    if (chatContainer.value) {
      chatContainer.value.scrollTop = chatContainer.value.scrollHeight
    }
  })
}
</script>

<template>
  <div class="ai-assistant-wrapper">
    <!-- 悬浮触发气泡 -->
    <div class="assistant-trigger-btn" @click="toggleChat" :class="{ 'active': visible }">
      <el-icon size="24" v-if="!visible"><ChatDotRound /></el-icon>
      <el-icon size="24" v-else><Close /></el-icon>
      <span class="pulse-ring"></span>
    </div>

    <!-- 侧边对话弹窗 -->
    <transition name="el-zoom-in-bottom">
      <div class="chat-window-panel" v-if="visible">
        <div class="chat-header">
          <div class="header-title">
            <el-icon color="#3b82f6"><Headset /></el-icon>
            <span>AI 编程私教答疑</span>
          </div>
          <span class="status-online">在线</span>
        </div>

        <div ref="chatContainer" class="chat-body">
          <div
              v-for="(msg, idx) in chatList"
              :key="idx"
              class="chat-bubble-row"
              :class="msg.sender === 'user' ? 'row-user' : 'row-ai'"
          >
            <div class="avatar-box">
              {{ msg.sender === 'user' ? 'ME' : 'AI' }}
            </div>
            <div class="bubble-content">
              {{ msg.text }}
            </div>
          </div>
        </div>

        <div class="chat-footer">
          <el-input
              v-model="inputMsg"
              placeholder="提问编程概念、算法、报错等..."
              :disabled="loading"
              @keyup.enter="handleSend"
          >
            <template #append>
              <el-button :icon="Promotion" @click="handleSend" :loading="loading" />
            </template>
          </el-input>
        </div>
      </div>
    </transition>
  </div>
</template>

<style scoped>
.ai-assistant-wrapper {
  position: fixed;
  bottom: 30px;
  right: 30px;
  z-index: 2000;
  font-family: system-ui, -apple-system, sans-serif;
}

/* 呼吸灯悬浮按钮 */
.assistant-trigger-btn {
  width: 56px;
  height: 56px;
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  border-radius: 50%;
  color: #fff;
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  box-shadow: 0 10px 25px -5px rgba(37, 99, 235, 0.4);
  position: relative;
  transition: transform 0.2s, background 0.2s;
}

.assistant-trigger-btn:hover {
  transform: scale(1.05);
}

.assistant-trigger-btn.active {
  background: #0f172a;
}

.pulse-ring {
  position: absolute;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  border: 2px solid #2563eb;
  animation: ripple 1.8s infinite ease-out;
  pointer-events: none;
}

/* 对话框主体 */
.chat-window-panel {
  position: absolute;
  bottom: 70px;
  right: 0;
  width: 360px;
  height: 480px;
  background-color: #fff;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-header {
  height: 50px;
  background-color: #f8fafc;
  border-bottom: 1px solid #edf2f7;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 16px;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: bold;
  color: #0f172a;
  font-size: 14.5px;
}

.status-online {
  font-size: 11px;
  color: #10b981;
  background-color: #d1fae5;
  padding: 2px 8px;
  border-radius: 12px;
  font-weight: bold;
}

.chat-body {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  background-color: #f8fafc;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.chat-bubble-row {
  display: flex;
  gap: 10px;
  max-width: 85%;
}

.row-ai {
  align-self: flex-start;
}

.row-user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.avatar-box {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 11px;
  font-weight: bold;
  flex-shrink: 0;
}

.row-ai .avatar-box {
  background-color: #eff6ff;
  color: #2563eb;
  border: 1px solid #bfdbfe;
}

.row-user .avatar-box {
  background-color: #0f172a;
  color: #fff;
}

.bubble-content {
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 13px;
  line-height: 1.5;
  word-break: break-all;
}

.row-ai .bubble-content {
  background-color: #fff;
  color: #1e293b;
  border: 1px solid #e2e8f0;
  border-top-left-radius: 2px;
}

.row-user .bubble-content {
  background-color: #2563eb;
  color: #fff;
  border-top-right-radius: 2px;
}

.chat-footer {
  padding: 12px;
  background-color: #fff;
  border-top: 1px solid #edf2f7;
}

@keyframes ripple {
  0% { transform: scale(1); opacity: 0.8; }
  100% { transform: scale(1.4); opacity: 0; }
}
</style>
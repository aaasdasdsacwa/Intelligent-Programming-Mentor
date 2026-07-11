<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const router = useRouter()
const loading = ref(false)
const currentUser = ref(null)

// 意向输入表单
const generateForm = ref({
  language: 'Java',
  target: 'Spring Boot Web 后端',
  currentLevel: '零基础'
})

// 路线图数据 (包含主表和节点列表)
const activePath = ref(null)
const pathNodes = ref([])

// 获取当前登录用户
const fetchUser = async () => {
  try {
    const res = await axios.get('/api/user/get/login')
    if (res.data.code === 0) {
      currentUser.value = res.data.data
    } else {
      router.push('/login')
    }
  } catch (err) {
    router.push('/login')
  }
}

// 智能生成学习路径
const handleGenerate = async () => {
  loading.value = true
  try {
    const res = await axios.post('/api/path/generate', generateForm.value)
    if (res.data.code === 0) {
      ElMessage.success('智能路径规划生成成功！')
      const pathId = res.data.data
      await fetchPathNodes(pathId)
    } else {
      ElMessage.error(res.data.message || '生成失败')
    }
  } catch (err) {
    ElMessage.error('生成路径超时或大模型接口异常')
  } finally {
    loading.value = false
  }
}

// 获取某一路径下的节点明细
const fetchPathNodes = async (pathId) => {
  try {
    const res = await axios.get(`/api/path/nodes?pathId=${pathId}`)
    if (res.data.code === 0) {
      pathNodes.value = res.data.data
      activePath.value = { id: pathId, name: `${generateForm.value.language} + ${generateForm.value.target} 自定义路线` }
    }
  } catch (err) {
    ElMessage.error('获取路线节点失败')
  }
}

// 点击某个节点跳转题库做题
const handleGoPractice = (tag) => {
  if (!tag) {
    ElMessage.warning('该节点暂无匹配评测题目')
    return
  }
  // 带着当前节点的标签，跳转到题库页进行筛选
  router.push({ path: '/problems', query: { tag: tag } })
}

// 登出
const handleLogout = async () => {
  try {
    const res = await axios.post('/api/user/logout')
    if (res.data.code === 0) {
      ElMessage.success('注销成功')
      router.push('/login')
    }
  } catch (err) {
    ElMessage.error('注销异常')
  }
}

onMounted(() => {
  fetchUser()
})
</script>

<template>
  <div class="roadmap-container">
    <!-- 头部导航 -->
    <header class="navbar">
      <div class="logo">
        <el-icon size="24" color="#409EFF"><Platform /></el-icon>
        <span>智能编程导师系统</span>
      </div>
      <div class="user-info" v-if="currentUser">
        <span>欢迎你，{{ currentUser.nickname }}</span>
        <el-button type="danger" link @click="handleLogout">退出登录</el-button>
      </div>
    </header>

    <main class="content">
      <!-- 左侧：AI 意向提交面板 -->
      <div class="left-panel">
        <el-card class="box-card">
          <template #header>
            <div class="card-header">
              <span>🤖 AI 定制专属路线</span>
            </div>
          </template>
          <el-form :model="generateForm" label-position="top">
            <el-form-item label="你想学的编程语言">
              <el-select v-model="generateForm.language" placeholder="请选择" style="width: 100%">
                <el-option label="Java" value="Java" />
                <el-option label="Python" value="Python" />
                <el-option label="Go" value="Go" />
              </el-select>
            </el-form-item>
            <el-form-item label="你希望攻克的方向">
              <el-input v-model="generateForm.target" placeholder="例如：Spring Boot Web 后端、数据分析等" />
            </el-form-item>
            <el-form-item label="你当前的基础">
              <el-radio-group v-model="generateForm.currentLevel">
                <el-radio-button label="零基础" value="零基础" />
                <el-radio-button label="有C/C++基础" value="有C/C++基础" />
              </el-radio-group>
            </el-form-item>
            <el-button type="primary" class="gen-btn" :loading="loading" @click="handleGenerate">
              {{ loading ? '大模型深度规划中...' : '生成智能路线图' }}
            </el-button>
          </el-form>
        </el-card>
      </div>

      <!-- 右侧：可视化的 Roadmap 时间轴 -->
      <div class="right-panel">
        <el-card class="box-card" v-loading="loading">
          <template #header>
            <div class="card-header">
              <span>🗺️ 学习路线图可视化</span>
            </div>
          </template>

          <!-- 未生成时的缺省态 -->
          <div v-if="!activePath" class="empty-state">
            <el-empty description="在左侧定制你的专属编程学习路线" />
          </div>

          <!-- 生成路线后的时间轴 -->
          <div v-else class="timeline-wrapper">
            <h3>{{ activePath.name }}</h3>
            <el-timeline>
              <el-timeline-item
                  v-for="node in pathNodes"
                  :key="node.id"
                  :timestamp="`阶段 ${node.sequence}`"
                  placement="top"
                  :type="node.status === 2 ? 'success' : (node.status === 1 ? 'primary' : 'info')"
                  :hollow="node.status === 0"
              >
                <el-card class="node-card">
                  <div class="node-header">
                    <h4>{{ node.nodeName }}</h4>
                    <!-- 状态标签 -->
                    <el-tag v-if="node.status === 2" type="success" effect="dark">已通关</el-tag>
                    <el-tag v-else-if="node.status === 1" type="primary" effect="dark">进行中</el-tag>
                    <el-tag v-else type="info">未解锁</el-tag>
                  </div>
                  <p class="node-desc">{{ node.nodeDesc }}</p>
                  <div class="node-actions">
                    <span class="tag-tip">🎯 绑定标签: {{ node.matchedTag }}</span>
                    <el-button type="success" size="small" icon="Check" @click="handleGoPractice(node.matchedTag)">
                      练习匹配关卡
                    </el-button>
                  </div>
                </el-card>
              </el-timeline-item>
            </el-timeline>
          </div>
        </el-card>
      </div>
    </main>
  </div>
</template>

<style scoped>
.roadmap-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.navbar {
  height: 60px;
  background-color: #fff;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 30px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: bold;
  color: #1e293b;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 15px;
  font-size: 14px;
}

.content {
  flex: 1;
  display: flex;
  gap: 20px;
  padding: 30px;
  background-color: #f8fafc;
}

.left-panel {
  width: 350px;
}

.right-panel {
  flex: 1;
}

.box-card {
  height: 100%;
  border-radius: 12px;
}

.card-header {
  font-weight: bold;
  font-size: 16px;
  color: #1e293b;
}

.gen-btn {
  width: 100%;
  margin-top: 15px;
}

.empty-state {
  height: 400px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.timeline-wrapper h3 {
  margin: 0 0 25px 0;
  color: #409EFF;
}

.node-card {
  margin-bottom: 10px;
  border-radius: 8px;
}

.node-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.node-header h4 {
  margin: 0;
  font-size: 16px;
  color: #1e293b;
}

.node-desc {
  margin: 10px 0;
  color: #64748b;
  font-size: 14px;
}

.node-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px dashed #f1f5f9;
  padding-top: 10px;
  margin-top: 10px;
}

.tag-tip {
  font-size: 12px;
  color: #94a3b8;
}
</style>
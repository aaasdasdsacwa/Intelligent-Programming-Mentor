<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const router = useRouter()
const pageLoading = ref(false)
const submitLoading = ref(false)
const currentUser = ref(null)

// 知识点精讲抽屉相关状态
const drawerVisible = ref(false)
const detailLoading = ref(false)
const selectedNodeName = ref('')
const knowledgeDetail = ref('')

// 编程语言与技术方向的动态关联关系映射表
const directionMap = {
  Java: [
    { label: 'Spring Boot 现代 Web 后端开发', value: 'Spring Boot Web 后端' },
    { label: 'Spring Cloud 高并发微服务架构', value: '微服务架构开发' },
    { label: 'Java 虚拟机（JVM）性能调优', value: 'JVM 与底层调优' },
    { label: '大数据工程（Hadoop/Spark 实战）', value: '大数据开发工程' }
  ],
  Python: [
    { label: '机器学习与人工智能算法基础', value: '机器学习与人工智能' },
    { label: 'Python 数据科学与商业数据分析', value: '数据科学与分析' },
    { label: '自动化网络爬虫与全流程测试', value: '自动化爬虫与脚本' },
    { label: 'Django / FastAPI 高性能 Web 开发', value: 'Python Web 开发' }
  ],
  Go: [
    { label: 'Go 云原生微服务 system（Gin / K8s）', value: 'Go 云原生后端服务' },
    { label: '分布式存储与底层并发系统设计', value: '分布式系统开发' },
    { label: '区块链分布式记账技术研发', value: 'Go 区块链开发' }
  ],
  CPlusPlus: [
    { label: 'C++ 游戏引擎研发与 3D 渲染（Unreal）', value: '游戏引擎开发' },
    { label: '嵌入式系统开发与物联网底层驱动', value: '嵌入式与底层系统' },
    { label: '算法竞赛与高性能底层算力优化', value: '算法竞赛与高性能计算' }
  ],
  JavaScript: [
    { label: '现代大前端全栈开发（Vue3 / React）', value: '大前端全栈开发' },
    { label: 'Node.js 服务端高并发微服务开发', value: 'NodeJS 后端开发' },
    { label: '微信小程序及跨端移动端应用研发', value: '跨端移动研发' }
  ]
}

// 意向输入表单
const generateForm = ref({
  language: 'Java',
  target: 'Spring Boot Web 后端',
  currentLevel: '零基础'
})

// 计算属性：动态联动技术方向
const availableTargets = computed(() => {
  return directionMap[generateForm.value.language] || []
})

// 监听语言变化，重置技术方向
watch(() => generateForm.value.language, (newLang) => {
  const targets = directionMap[newLang]
  if (targets && targets.length > 0) {
    generateForm.value.target = targets[0].value
  } else {
    generateForm.value.target = ''
  }
})

// 路线图数据
const activePath = ref(null)
const pathNodes = ref([])

// 初始化获取当前登录用户
const fetchUser = async () => {
  pageLoading.value = true
  try {
    const res = await axios.get('/api/user/get/login')
    if (res.data.code === 0) {
      currentUser.value = res.data.data
      await fetchUserHistoryPath()
    } else {
      router.push('/login')
    }
  } catch (err) {
    router.push('/login')
  } finally {
    pageLoading.value = false
  }
}

// 查询该用户历史已有的路径
const fetchUserHistoryPath = async () => {
  try {
    const res = await axios.get('/api/path/nodes?pathId=latest')
    if (res.data.code === 0 && res.data.data && res.data.data.length > 0) {
      pathNodes.value = res.data.data
      activePath.value = {
        name: `我的专属 AI 定制路线`
      }
      // 💡 写入本地持久化缓存，防止页面因 latest 转换报错导致白屏 [1]
      localStorage.setItem('cached_path', JSON.stringify(activePath.value))
      localStorage.setItem('cached_nodes', JSON.stringify(pathNodes.value))
    }
  } catch (err) {
    logError('未检索到历史路径，请在左侧发起全新规划。')
  }
}

// 智能生成学习路径
const handleGenerate = async () => {
  if (!generateForm.value.target) {
    ElMessage.warning('请选择你想攻克的具体技术方向')
    return
  }

  submitLoading.value = true
  try {
    const res = await axios.post('/api/path/generate', generateForm.value)
    if (res.data.code === 0) {
      ElMessage({
        message: '🚀 AI 导师已为你生成全新学习规划！',
        type: 'success'
      })
      const pathId = res.data.data
      await fetchPathNodes(pathId)
    } else {
      ElMessage.error(res.data.message || '生成失败')
    }
  } catch (err) {
    ElMessage.error('AI 路线生成失败，请确保大模型 Key 配置正确且服务未超时。')
  } finally {
    submitLoading.value = false
  }
}

// 根据路径 ID 获取节点列表
const fetchPathNodes = async (pathId) => {
  try {
    const res = await axios.get(`/api/path/nodes?pathId=${pathId}`)
    if (res.data.code === 0) {
      pathNodes.value = res.data.data
      activePath.value = {
        name: `我的专属 AI 定制路线`
      }
      // 💡 生成新路径成功，同步写入本地缓存 [1]
      localStorage.setItem('cached_path', JSON.stringify(activePath.value))
      localStorage.setItem('cached_nodes', JSON.stringify(pathNodes.value))
    } else {
      ElMessage.error(res.data.message || '获取路径明细失败')
    }
  } catch (err) {
    ElMessage.error('无法获取路径节点明细')
  }
}

// 点击展示详细知识点概念精讲（调用 AI 动态生成 500 字科普）
const handleShowDetail = async (node) => {
  selectedNodeName.value = node.nodeName
  knowledgeDetail.value = ''
  drawerVisible.value = true
  detailLoading.value = true

  try {
    // 组装高强度的概念深度精讲指令，利用已有的 QaController 完成响应
    const prompt = `你是一位专业的计算机科学导师。请为学生深度科普并讲解技术概念/知识点：【${node.nodeName}】。\n\n`
        + "要求内容结构条理清晰，包含以下四个维度：\n"
        + "1. 核心定义与基本原理：用通俗易懂的语言解释它是什么、它是如何工作的。\n"
        + "2. 关键应用场景：在真实的工业界开发中，什么情况下我们会使用它。\n"
        + "3. 极简代码示例：给出一个极简、典型的核心代码示例（Java / Python / Go / JS 均可）直观说明其用法。\n"
        + "4. 总结：用一句话提炼它的核心价值。\n\n"
        + "字数严格控制在 500 字左右，段落分明，排版精美。";

    const res = await axios.post('/api/qa/chat', { question: prompt })
    if (res.data.code === 0) {
      knowledgeDetail.value = res.data.data
    } else {
      knowledgeDetail.value = '❌ 无法获取知识点精讲内容：' + res.data.message
    }
  } catch (err) {
    knowledgeDetail.value = '❌ 无法连接智能问答服务器，请检查后端服务是否正常运行。'
  } finally {
    detailLoading.value = false
  }
}

// 辅助打印日志方法
const logError = (msg) => {
  console.log(`[Roadmap Debug] ${msg}`)
}

onMounted(() => {
  // 💡 优先从本地缓存读取路线，避免任何因切换 Tab、强制刷新导致的白屏现象 [1]
  const cachedPath = localStorage.getItem('cached_path')
  const cachedNodes = localStorage.getItem('cached_nodes')
  if (cachedPath && cachedNodes) {
    activePath.value = JSON.parse(cachedPath)
    pathNodes.value = JSON.parse(cachedNodes)
  }
  fetchUser()
})
</script>

<template>
  <div class="roadmap-page" v-loading="pageLoading">
    <div class="roadmap-layout">
      <!-- 左侧：意向输入表单面板 -->
      <div class="left-aside">
        <el-card class="form-card" shadow="hover">
          <template #header>
            <div class="card-header-title">
              <el-icon class="icon-spark"><Cpu /></el-icon>
              <span>AI 路线定制系统</span>
            </div>
          </template>

          <el-form :model="generateForm" label-position="top">
            <el-form-item label="1. 想学的编程语言">
              <el-select v-model="generateForm.language" placeholder="请选择编程语言" style="width: 100%">
                <el-option label="Java 核心开发" value="Java" />
                <el-option label="Python 核心开发" value="Python" />
                <el-option label="Go 极速核心开发" value="Go" />
                <el-option label="C++ 系统级研发" value="CPlusPlus" />
                <el-option label="JavaScript 全栈开发" value="JavaScript" />
              </el-select>
            </el-form-item>

            <el-form-item label="2. 想攻克的技术方向">
              <el-select v-model="generateForm.target" placeholder="请选择技术方向" style="width: 100%">
                <el-option
                    v-for="item in availableTargets"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="3. 你当前的编程基础">
              <el-radio-group v-model="generateForm.currentLevel" style="width: 100%">
                <el-radio-button label="零基础" value="零基础" />
                <el-radio-button label="有其它语言基础" value="有其它语言基础" />
              </el-radio-group>
            </el-form-item>

            <el-button
                type="primary"
                class="submit-generate-btn"
                :loading="submitLoading"
                @click="handleGenerate"
            >
              {{ submitLoading ? '大模型深度规划中...' : '生成智能路线图' }}
            </el-button>
          </el-form>
        </el-card>
      </div>

      <!-- 右侧：核心可视化路线图与大屏 -->
      <div class="right-main">
        <!-- 未生成路径时的缺省占位图 -->
        <div v-if="!activePath" class="empty-state-card">
          <el-card class="placeholder-card">
            <el-empty description="在左侧选择你的意向并点击按钮，AI 将为你规划完整的学习路径节点和对应的知识点。">
              <div class="empty-icon-box">
                <el-icon size="48" color="#94a3b8"><Compass /></el-icon>
              </div>
            </el-empty>
          </el-card>
        </div>

        <!-- 路径渲染区 -->
        <div v-else class="active-roadmap-wrapper">
          <!-- 1. 进度大屏 Dashboard Banner -->
          <div class="dashboard-banner">
            <div class="dashboard-left">
              <h3>{{ activePath.name }}</h3>
              <p>系统已为你匹配了 4 个渐进式核心概念知识点，点击各节点即可展开学习宏观概念与典型示例代码。</p>

              <!-- 快捷统计（删除了闯关、手动切态逻辑，改为优雅的结构展示） -->
              <div class="stat-group">
                <div class="stat-item">
                  <span class="number count-completed">4 个</span>
                  <span class="label">规划核心阶段</span>
                </div>
                <div class="stat-item">
                  <span class="number count-learning">1 对 1</span>
                  <span class="label">专属导师辅导</span>
                </div>
              </div>
            </div>

            <!-- 环形进度条（改为纯视觉展示，代表技术掌握度的路线目标） -->
            <div class="dashboard-right">
              <el-progress
                  type="circle"
                  :percentage="100"
                  :width="100"
                  :stroke-width="8"
                  status="success"
              />
            </div>
          </div>

          <!-- 2. 核心时间轴节点展示 -->
          <div class="timeline-container">
            <el-timeline>
              <el-timeline-item
                  v-for="node in pathNodes"
                  :key="node.id"
                  placement="top"
                  :timestamp="`第 ${node.sequence} 阶段`"
              >
                <!-- 自定义节点卡片 -->
                <div class="node-interactive-card">
                  <!-- 头部：阶段名称 -->
                  <div class="node-card-header">
                    <div class="title-with-icon">
                      <el-icon class="status-icon icon-learn"><Aim /></el-icon>
                      <h4>{{ node.nodeName }}</h4>
                    </div>
                  </div>

                  <!-- 描述 -->
                  <p class="node-card-desc">{{ node.nodeDesc }}</p>

                  <!-- 底部操作与关联标签 -->
                  <div class="node-card-footer">
                    <div class="meta-tag-info">
                      <el-icon><PriceTag /></el-icon>
                      <span>技术标签：<code>{{ node.matchedTag }}</code></span>
                    </div>

                    <div class="node-btn-group">
                      <!-- 🌟 仅保留一个核心的“详细知识点”选项 -->
                      <el-button
                          type="primary"
                          size="small"
                          icon="Reading"
                          @click="handleShowDetail(node)"
                      >
                        详细知识点
                      </el-button>
                    </div>
                  </div>
                </div>
              </el-timeline-item>
            </el-timeline>
          </div>
        </div>
      </div>
    </div>

    <!-- 🌟 新增：右侧滑出的“知识点精讲”抽屉面板 -->
    <el-drawer
        v-model="drawerVisible"
        :title="`📘 知识精讲：${selectedNodeName}`"
        size="42%"
        destroy-on-close
        direction="rtl"
    >
      <div v-loading="detailLoading" class="knowledge-drawer-content">
        <!-- pre 标签完美保留大模型吐字出来的换行与代码段排版 -->
        <pre class="knowledge-text-block" v-if="knowledgeDetail">{{ knowledgeDetail }}</pre>
        <el-empty v-else-if="!detailLoading" description="暂无精讲数据" />
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.roadmap-page {
  padding: 24px;
}

.roadmap-layout {
  display: flex;
  gap: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.left-aside {
  width: 320px;
  flex-shrink: 0;
}

.right-main {
  flex: 1;
}

/* 路线规划表单样式 */
.form-card {
  border-radius: 12px;
  border: 1px solid #e2e8f0;
}

.card-header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: bold;
  color: #0f172a;
}

.icon-spark {
  color: #3b82f6;
  animation: pulse 2s infinite;
}

.submit-generate-btn {
  width: 100%;
  margin-top: 15px;
  height: 40px;
  border-radius: 8px;
  font-weight: bold;
}

/* 缺省态占位符 */
.empty-state-card {
  height: 100%;
}

.placeholder-card {
  border-radius: 12px;
  border: 1px dashed #cbd5e1;
  background-color: #f8fafc;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 450px;
}

/* 进度大屏仪表盘样式 */
.dashboard-banner {
  background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%);
  color: #fff;
  border-radius: 16px;
  padding: 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  box-shadow: 0 10px 15px -3px rgba(15, 23, 42, 0.1);
}

.dashboard-left h3 {
  margin: 0 0 8px 0;
  font-size: 20px;
  color: #3b82f6;
}

.dashboard-left p {
  margin: 0 0 20px 0;
  color: #94a3b8;
  font-size: 13.5px;
  max-width: 550px;
  line-height: 1.5;
}

.stat-group {
  display: flex;
  gap: 30px;
}

.stat-item {
  display: flex;
  flex-direction: column;
}

.stat-item .number {
  font-size: 24px;
  font-weight: bold;
  line-height: 1.2;
}

.count-completed { color: #10b981; }
.count-learning { color: #3b82f6; }

.stat-item .label {
  font-size: 12px;
  color: #64748b;
  margin-top: 4px;
}

/* 交互式时间轴卡片样式 */
.timeline-container {
  padding-left: 10px;
}

.node-interactive-card {
  background-color: #fff;
  border: 1px solid #e2e8f0;
  border-left: 5px solid #3b82f6; /* 统一高科技感的蓝色左侧条 */
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.node-interactive-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 15px -3px rgba(59, 130, 246, 0.08);
}

.node-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title-with-icon {
  display: flex;
  align-items: center;
  gap: 10px;
}

.status-icon {
  font-size: 20px;
}

.icon-learn { color: #3b82f6; }

.title-with-icon h4 {
  margin: 0;
  font-size: 16px;
  color: #0f172a;
}

.node-card-desc {
  margin: 12px 0 16px 0;
  font-size: 14px;
  color: #475569;
  line-height: 1.5;
}

.node-card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px dashed #cbd5e1;
  padding-top: 12px;
}

.meta-tag-info {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  color: #64748b;
}

.meta-tag-info code {
  background-color: #f1f5f9;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: monospace;
  color: #0f172a;
}

.node-btn-group {
  display: flex;
  gap: 10px;
  align-items: center;
}

/* 🌟 右侧知识精讲抽屉样式 */
.knowledge-drawer-content {
  min-height: 240px;
  padding: 4px 12px;
}

.knowledge-text-block {
  white-space: pre-wrap;
  font-family: inherit;
  line-height: 1.7;
  color: #334155;
  font-size: 14.5px;
  margin: 0;
}

/* 动效 */
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}
</style>
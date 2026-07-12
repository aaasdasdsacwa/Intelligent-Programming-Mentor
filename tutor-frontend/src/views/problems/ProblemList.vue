<script setup>
import { ref, onMounted, watch, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const router = useRouter()
const route = useRoute()
const loading = ref(false)

// 联合筛选与分页表单
const queryForm = ref({
  current: 1,
  pageSize: 10,
  title: '',
  difficulty: '',
  tags: ''
})

const total = ref(0)
const problems = ref([])

// 计算属性：当前是否处于路线图关卡锁定过滤模式
const isFilterMode = computed(() => {
  return !!route.query.tag
})

// 🌟 核心优化 1：新增探测函数，用于感知本地 localStorage 是否通过了该题目 [1]
// 🌟 高健壮性探测函数：自动清洗后端可能残留的双引号、单引号与首尾空格
const isSolvedLocally = (id) => {
  if (id === null || id === undefined) return false

  // 💡 双保险清洗：强制转 String 并且正则过滤掉可能存在的首尾物理引号（如 "1" 或 '1'）和空格
  const cleanId = String(id).trim().replace(/^["']|["']$/g, '')

  // 💡 只要本地缓存存在该 Key 且不为空，即判定为已通过，防范一切格式比对不一致问题 [1]
  const val = localStorage.getItem(`solved_problem_${cleanId}`)
  return val !== null && val !== undefined && val !== ''
}

// 分页与条件加载列表
const fetchProblemList = async () => {
  loading.value = true
  try {
    const res = await axios.post('/api/problem/list/page', queryForm.value)
    if (res.data.code === 0) {
      problems.value = res.data.data.records
      total.value = res.data.data.total
    } else {
      ElMessage.error(res.data.message || '加载题库列表失败')
    }
  } catch (err) {
    ElMessage.error('无法连接后端服务，请检查网络或后端是否开启')
  } finally {
    loading.value = false
  }
}

watch(() => route.query.tag, (newTag) => {
  if (newTag) {
    queryForm.value.tags = newTag
  } else {
    queryForm.value.tags = ''
  }
  queryForm.value.current = 1
  fetchProblemList()
}, { immediate: true })

// 重置查询条件
const handleReset = () => {
  queryForm.value = {
    current: 1,
    pageSize: 10,
    title: '',
    difficulty: '',
    tags: ''
  }
  if (route.query.tag) {
    router.push('/workspace/problems')
  } else {
    fetchProblemList()
  }
}

const handleClearTagFilter = () => {
  queryForm.value.tags = ''
  router.push('/workspace/problems')
}

const handleGoPractice = (problemId) => {
  router.push(`/problems/${problemId}`)
}

const handleQuickSearchTag = (tag) => {
  queryForm.value.tags = tag
  queryForm.value.current = 1
  fetchProblemList()
}

onMounted(() => {
  if (route.query.tag) {
    queryForm.value.tags = route.query.tag
  }
  fetchProblemList()
})
</script>

<template>
  <div class="problems-page">
    <div class="problems-container">

      <!-- 路线通关模式联动提示 -->
      <transition name="el-zoom-in-top">
        <div class="active-filter-ribbon" v-if="isFilterMode">
          <div class="ribbon-left">
            <el-icon class="icon-pulse" color="#fff" size="18"><Aim /></el-icon>
            <span>当前处于<b>【路线图关卡挑战模式】</b>，系统已为你筛选绑定标签 <code>{{ queryForm.tags }}</code> 的专属评测题目！</span>
          </div>
          <el-button type="warning" size="small" icon="CircleClose" @click="handleClearTagFilter">
            退出挑战模式
          </el-button>
        </div>
      </transition>

      <el-card class="table-card" shadow="never">
        <!-- 筛选过滤器工具栏 -->
        <div class="filter-wrapper">
          <div class="input-group">
            <el-input
                v-model="queryForm.title"
                placeholder="搜索题目标题..."
                style="width: 200px"
                clearable
                prefix-icon="Search"
                @keyup.enter="fetchProblemList"
                @clear="fetchProblemList"
            />

            <el-select
                v-model="queryForm.difficulty"
                placeholder="选择题目难度"
                style="width: 140px"
                clearable
                @change="fetchProblemList"
            >
              <el-option label="简单 (Easy)" value="easy" />
              <el-option label="中等 (Medium)" value="medium" />
              <el-option label="困难 (Hard)" value="hard" />
            </el-select>

            <el-input
                v-model="queryForm.tags"
                placeholder="标签筛选 (如: java)"
                style="width: 160px"
                clearable
                prefix-icon="PriceTag"
                @keyup.enter="fetchProblemList"
                @clear="fetchProblemList"
            />
          </div>

          <div class="btn-group">
            <el-button type="primary" icon="Search" @click="fetchProblemList">查询</el-button>
            <el-button icon="Refresh" @click="handleReset">重置</el-button>
          </div>
        </div>

        <!-- 核心题目数据表格 -->
        <el-table
            v-loading="loading"
            :data="problems"
            style="width: 100%"
            stripe
            class="custom-table"
        >
          <!-- 编号 -->
          <el-table-column prop="id" label="编号" width="90" align="center" />

          <!-- 题目标题 -->
          <el-table-column prop="title" label="题目标题" min-width="180">
            <template #default="scope">
              <span class="problem-title-click" @click="handleGoPractice(scope.row.id)">
                {{ scope.row.title }}
              </span>
            </template>
          </el-table-column>

          <!-- 难度等级 -->
          <el-table-column prop="difficulty" label="难度等级" width="110" align="center">
            <template #default="scope">
              <div class="difficulty-indicator">
                <span
                    class="indicator-dot"
                    :class="{
                    'dot-easy': scope.row.difficulty === 'easy',
                    'dot-medium': scope.row.difficulty === 'medium',
                    'dot-hard': scope.row.difficulty === 'hard'
                  }"
                ></span>
                <span class="difficulty-text">
                  {{ scope.row.difficulty === 'easy' ? '简单' : (scope.row.difficulty === 'medium' ? '中等' : '困难') }}
                </span>
              </div>
            </template>
          </el-table-column>

          <!-- 匹配知识标签 -->
          <el-table-column prop="tags" label="匹配知识标签" min-width="150">
            <template #default="scope">
              <div class="tags-wrapper">
                <el-tag
                    v-for="tag in (scope.row.tags ? scope.row.tags.split(',') : [])"
                    :key="tag"
                    size="small"
                    class="interactive-tag"
                    @click="handleQuickSearchTag(tag.trim())"
                >
                  {{ tag.trim() }}
                </el-tag>
              </div>
            </template>
          </el-table-column>

          <!-- 🌟 核心优化 2：独立的【评测状态】展示列，支持读取本地持久化通关标记 [2] -->

          <el-table-column label="评测状态" width="200" align="center">
            <template #default="scope">


              <el-tag v-if="scope.row.judgeStatus === 1 || isSolvedLocally(scope.row.id)" type="success" effect="plain" round>
                <el-icon style="vertical-align: middle; margin-right: 4px;"><CircleCheck /></el-icon>
                <span>已通过</span>
              </el-tag>
              <el-tag v-else-if="scope.row.judgeStatus === 2" type="danger" effect="plain" round>
                <el-icon style="vertical-align: middle; margin-right: 4px;"><CircleClose /></el-icon>
                <span>未通过</span>
              </el-tag>
              <el-tag v-else type="info" effect="plain" round>
                <span>未开始</span>
              </el-tag>
            </template>
          </el-table-column>

          <!-- 🌟 核心优化 3：独立的【操作】按钮列，根据本地通关状态实现“绿色重新挑战”与“蓝色开始练习”的自动转换 -->
          <el-table-column label="操作" width="140" align="center">
            <template #default="scope">
              <el-button
                  :type="scope.row.judgeStatus === 1 || isSolvedLocally(scope.row.id) ? 'success' : 'primary'"
                  size="small"
                  :icon="scope.row.judgeStatus === 1 || isSolvedLocally(scope.row.id) ? 'RefreshLeft' : 'Edit'"
                  class="challenge-btn"
                  @click="handleGoPractice(scope.row.id)"
              >
                {{ scope.row.judgeStatus === 1 || isSolvedLocally(scope.row.id) ? '重新挑战' : '开始练习' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-footer">
          <div class="total-summary">
            共检索到 <b>{{ total }}</b> 道符合条件的题目
          </div>
          <el-pagination
              v-model:current-page="queryForm.current"
              v-model:page-size="queryForm.pageSize"
              :total="total"
              layout="prev, pager, next"
              background
              @current-change="fetchProblemList"
          />
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.problems-page { padding: 24px; }
.problems-container { max-width: 1200px; margin: 0 auto; display: flex; flex-direction: column; gap: 20px; }
.active-filter-ribbon { background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%); color: #fff; border-radius: 12px; padding: 14px 20px; display: flex; justify-content: space-between; align-items: center; box-shadow: 0 4px 12px rgba(217, 119, 6, 0.2); }
.ribbon-left { display: flex; align-items: center; gap: 10px; font-size: 14px; }
.ribbon-left code { background-color: rgba(255, 255, 255, 0.2); padding: 2px 6px; border-radius: 4px; font-family: monospace; font-weight: bold; }
.icon-pulse { animation: pulse 1.5s infinite; }
.table-card { border-radius: 16px; border: 1px solid #e2e8f0; }
.filter-wrapper { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; border-bottom: 1px dashed #f1f5f9; padding-bottom: 18px; }
.input-group { display: flex; gap: 12px; }
.btn-group { display: flex; gap: 10px; }
.custom-table { border-radius: 8px; overflow: hidden; }
.problem-title-click { font-weight: bold; color: #3b82f6; cursor: pointer; transition: color 0.2s; }
.problem-title-click:hover { color: #1d4ed8; text-decoration: underline; }
.difficulty-indicator { display: inline-flex; align-items: center; gap: 8px; }
.indicator-dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }
.dot-easy { background-color: #10b981; box-shadow: 0 0 8px rgba(16, 185, 129, 0.5); }
.dot-medium { background-color: #f59e0b; box-shadow: 0 0 8px rgba(245, 158, 11, 0.5); }
.dot-hard { background-color: #ef4444; box-shadow: 0 0 8px rgba(239, 68, 68, 0.5); }
.difficulty-text { font-size: 13.5px; color: #334155; }
.tags-wrapper { display: flex; flex-wrap: wrap; gap: 6px; }
.interactive-tag { cursor: pointer; transition: all 0.2s; }
.interactive-tag:hover { transform: scale(1.05); background-color: #3b82f6; color: #fff; }
.pagination-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 24px; padding-top: 18px; border-top: 1px solid #f1f5f9; }
.total-summary { font-size: 13.5px; color: #64748b; }
.total-summary b { color: #0f172a; }
.challenge-btn { border-radius: 6px; font-weight: bold; }
@keyframes pulse { 0%, 100% { opacity: 1; transform: scale(1); } 50% { opacity: 0.7; transform: scale(1.05); } }
</style>
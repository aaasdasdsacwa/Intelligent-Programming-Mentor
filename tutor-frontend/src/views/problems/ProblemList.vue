<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const router = useRouter()
const route = useRoute()
const loading = ref(false)

// 分页与筛选表单
const queryForm = ref({
  current: 1,
  pageSize: 10,
  title: '',
  difficulty: '',
  tags: ''
})

const total = ref(0)
const problems = ref([])

// 加载题目列表 (分页查询)
const fetchProblemList = async () => {
  loading.value = true
  try {
    const res = await axios.post('/api/problem/list/page', queryForm.value)
    if (res.data.code === 0) {
      problems.value = res.data.data.records
      total.value = res.data.data.total
    } else {
      ElMessage.error(res.data.message || '加载题库失败')
    }
  } catch (err) {
    ElMessage.error('无法连接后端服务')
  } finally {
    loading.value = false
  }
}

// 监听路由参数变化（如点击不同路线节点重新检索题目）
watch(() => route.query.tag, (newTag) => {
  if (newTag) {
    queryForm.value.tags = newTag
  } else {
    queryForm.value.tags = ''
  }
  queryForm.value.current = 1
  fetchProblemList()
}, { immediate: true })

// 点击重置按钮
const handleReset = () => {
  queryForm.value = {
    current: 1,
    pageSize: 10,
    title: '',
    difficulty: '',
    tags: ''
  }
  // 移除浏览器 URL 的 query 标签
  router.push('/problems')
  fetchProblemList()
}

// 跳到在线练习页
const handlePractice = (problemId) => {
  router.push(`/problems/${problemId}`)
}

onMounted(() => {
  // 如果 URL 含有标签就先过滤，没有就查全部
  if (route.query.tag) {
    queryForm.value.tags = route.query.tag
  }
  fetchProblemList()
})
</script>

<template>
  <div class="problem-list-container">
    <header class="navbar">
      <div class="logo" @click="router.push('/roadmap')" style="cursor:pointer">
        <el-icon size="24" color="#409EFF"><Platform /></el-icon>
        <span>智能编程导师系统</span>
      </div>
      <el-button type="primary" link @click="router.push('/roadmap')">← 返回路线图</el-button>
    </header>

    <main class="main-content">
      <el-card class="table-card">
        <!-- 搜索与筛选表单栏 -->
        <div class="filter-bar">
          <el-input v-model="queryForm.title" placeholder="输入题目标题进行搜索" style="width: 200px" @keyup.enter="fetchProblemList" />
          <el-select v-model="queryForm.difficulty" placeholder="选择难度" style="width: 150px" clearable @change="fetchProblemList">
            <el-option label="简单 (Easy)" value="easy" />
            <el-option label="中等 (Medium)" value="medium" />
            <el-option label="困难 (Hard)" value="hard" />
          </el-select>
          <el-input v-model="queryForm.tags" placeholder="标签(如: java)" style="width: 150px" @keyup.enter="fetchProblemList" />
          <el-button type="primary" icon="Search" @click="fetchProblemList">搜索</el-button>
          <el-button icon="Refresh" @click="handleReset">重置</el-button>
        </div>

        <!-- 题目列表表格 -->
        <el-table :data="problems" v-loading="loading" style="width: 100%" stripe>
          <el-table-column prop="id" label="题目ID" width="100" />
          <el-table-column prop="title" label="题目标题">
            <template #default="scope">
              <span class="problem-title-btn" @click="handlePractice(scope.row.id)">{{ scope.row.title }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="difficulty" label="难度" width="120">
            <template #default="scope">
              <el-tag v-if="scope.row.difficulty === 'easy'" type="success" effect="dark">简单</el-tag>
              <el-tag v-else-if="scope.row.difficulty === 'medium'" type="warning" effect="dark">中等</el-tag>
              <el-tag v-else type="danger" effect="dark">困难</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="tags" label="标签">
            <template #default="scope">
              <el-tag v-for="tag in (scope.row.tags ? scope.row.tags.split(',') : [])" :key="tag" class="tag-item">
                {{ tag }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" align="center">
            <template #default="scope">
              <el-button type="primary" size="small" icon="Edit" @click="handlePractice(scope.row.id)">
                去挑战
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页栏 -->
        <div class="pagination-wrapper">
          <el-pagination
              v-model:current-page="queryForm.current"
              v-model:page-size="queryForm.pageSize"
              :total="total"
              layout="total, prev, pager, next"
              background
              @current-change="fetchProblemList"
          />
        </div>
      </el-card>
    </main>
  </div>
</template>

<style scoped>
.problem-list-container {
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
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: bold;
}

.main-content {
  flex: 1;
  padding: 30px;
  background-color: #f8fafc;
}

.table-card {
  border-radius: 12px;
}

.filter-bar {
  display: flex;
  gap: 15px;
  margin-bottom: 25px;
}

.problem-title-btn {
  font-weight: bold;
  color: #409EFF;
  cursor: pointer;
}

.problem-title-btn:hover {
  text-decoration: underline;
}

.tag-item {
  margin-right: 5px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 25px;
}
</style>
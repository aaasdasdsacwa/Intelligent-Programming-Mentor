<script setup>
import { ref, onMounted } from 'vue'
import { Clock, Search, Refresh, View, CircleCheck, CircleClose, Warning, InfoFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const loading = ref(false)
const listData = ref([])
const total = ref(0)

// 查询表单
const queryForm = ref({
  current: 1,
  pageSize: 10,
  problemId: null,
  language: '',
  status: null
})

// 查看详情弹窗状态
const detailVisible = ref(false)
const selectedSubmit = ref(null)

const fetchList = async () => {
  loading.value = true
  try {
    const res = await axios.post('/api/judge/list/page', queryForm.value)
    if (res.data.code === 0) {
      listData.value = res.data.data.records
      total.value = res.data.data.total
    } else {
      ElMessage.error(res.data.message || '加载提交历史失败')
    }
  } catch (err) {
    ElMessage.error('无法连接后端服务，请确认后端已成功编译')
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  queryForm.value = {
    current: 1,
    pageSize: 10,
    problemId: null,
    language: '',
    status: null
  }
  fetchList()
}

// 格式化时间
const formatTime = (timeStr) => {
  if (!timeStr) return ''
  return timeStr.replace('T', ' ')
}

// 查看单次提交详情
const handleShowDetail = (row) => {
  selectedSubmit.value = row
  detailVisible.value = true
}

onMounted(() => {
  fetchList()
})
</script>

<template>
  <div class="submissions-page">
    <div class="submissions-container">
      <el-card class="table-card" shadow="never">

        <!-- 筛选工具栏 -->
        <div class="filter-wrapper">
          <div class="input-group">
            <el-input
                v-model.number="queryForm.problemId"
                placeholder="按题目编号搜索..."
                style="width: 180px"
                clearable
                prefix-icon="Search"
                @keyup.enter="fetchList"
                @clear="fetchList"
            />

            <el-select
                v-model="queryForm.language"
                placeholder="选择编程语言"
                style="width: 150px"
                clearable
                @change="fetchList"
            >
              <el-option label="Java" value="java" />
              <el-option label="Python" value="python" />
              <el-option label="C++" value="cpp" />
              <el-option label="Go" value="go" />
              <el-option label="JavaScript" value="javascript" />
            </el-select>

            <el-select
                v-model="queryForm.status"
                placeholder="选择评测结果"
                style="width: 160px"
                clearable
                @change="fetchList"
            >
              <el-option label="通过 (Accepted)" :value="0" />
              <el-option label="答案错误 (WA)" :value="1" />
              <el-option label="编译错误 (CE)" :value="2" />
              <el-option label="运行时异常 (RE)" :value="3" />
              <el-option label="运行超时 (TLE)" :value="4" />
            </el-select>
          </div>

          <div class="btn-group">
            <el-button type="primary" icon="Search" @click="fetchList">查询</el-button>
            <el-button icon="Refresh" @click="handleReset">重置</el-button>
          </div>
        </div>

        <!-- 历史表格 -->
        <el-table
            v-loading="loading"
            :data="listData"
            style="width: 100%"
            stripe
            class="custom-table"
        >
          <el-table-column prop="id" label="提交编号" width="100" align="center" />

          <el-table-column label="题目名称" min-width="180">
            <template #default="scope">
              <span class="problem-title-text">
                [#{{ scope.row.problemId }}] {{ scope.row.problemTitle || '未知题目' }}
              </span>
            </template>
          </el-table-column>

          <el-table-column prop="language" label="语言" width="120" align="center">
            <template #default="scope">
              <el-tag type="info" size="small" effect="plain" class="lang-tag-item">
                {{ scope.row.language }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="运行时间" width="120" align="center">
            <template #default="scope">
              <span v-if="scope.row.runTime !== null" class="time-text">
                {{ scope.row.runTime }} ms
              </span>
              <span v-else class="time-text">--</span>
            </template>
          </el-table-column>

          <el-table-column label="评测结果" width="150" align="center">
            <template #default="scope">
              <el-tag v-if="scope.row.status === 0" type="success" effect="plain" round>
                <el-icon style="vertical-align: middle; margin-right: 4px;"><CircleCheck /></el-icon>
                <span>Accepted</span>
              </el-tag>
              <el-tag v-else-if="scope.row.status === 1" type="danger" effect="plain" round>
                <el-icon style="vertical-align: middle; margin-right: 4px;"><CircleClose /></el-icon>
                <span>Wrong Answer</span>
              </el-tag>
              <el-tag v-else-if="scope.row.status === 2" type="warning" effect="plain" round>
                <el-icon style="vertical-align: middle; margin-right: 4px;"><Warning /></el-icon>
                <span>Compile Error</span>
              </el-tag>
              <el-tag v-else-if="scope.row.status === 3" type="warning" effect="plain" round>
                <el-icon style="vertical-align: middle; margin-right: 4px;"><Warning /></el-icon>
                <span>Runtime Error</span>
              </el-tag>
              <el-tag v-else type="danger" effect="plain" round>
                <el-icon style="vertical-align: middle; margin-right: 4px;"><Clock /></el-icon>
                <span>Time Limit Exceeded</span>
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="提交时间" width="180" align="center">
            <template #default="scope">
              <span class="date-text">{{ formatTime(scope.row.createTime) }}</span>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="120" align="center">
            <template #default="scope">
              <el-button
                  type="primary"
                  size="small"
                  link
                  :icon="View"
                  @click="handleShowDetail(scope.row)"
              >
                查看代码
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="pagination-footer">
          <div class="total-summary">
            共计提交 <b>{{ total }}</b> 次记录
          </div>
          <el-pagination
              v-model:current-page="queryForm.current"
              v-model:page-size="queryForm.pageSize"
              :total="total"
              layout="prev, pager, next"
              background
              @current-change="fetchList"
          />
        </div>
      </el-card>
    </div>

    <!-- 🌟 查看代码与日志详情 el-dialog -->
    <el-dialog
        v-model="detailVisible"
        :title="`📜 评测详情 - 提交编号 #${selectedSubmit?.id}`"
        width="720px"
        destroy-on-close
    >
      <div v-if="selectedSubmit" class="submit-detail-dialog-body">
        <!-- 元数据统计条 -->
        <div class="meta-row">
          <span>题目：<b>{{ selectedSubmit.problemTitle }}</b></span>
          <span>语言：<el-tag size="small" type="info">{{ selectedSubmit.language }}</el-tag></span>
          <span>用时：<b style="color: #3b82f6;">{{ selectedSubmit.runTime || '--' }} ms</b></span>
        </div>

        <!-- 编译/运行报错控制台（仅在非 Accepted 时且有报错日志时展现，黑客终端风格） -->
        <div v-if="selectedSubmit.status !== 0 && selectedSubmit.errorMsg" class="error-terminal-console">
          <div class="terminal-header">
            <el-icon color="#ef4444" style="margin-right: 6px;"><InfoFilled /></el-icon>
            <span>沙箱评测控制台报错日志：</span>
          </div>
          <pre class="terminal-body">{{ selectedSubmit.errorMsg }}</pre>
        </div>

        <!-- 历史代码展示面板 -->
        <div class="code-editor-header">
          <span>历史提交源码：</span>
        </div>
        <div class="code-view-container">
          <pre class="code-viewer-pre"><code>{{ selectedSubmit.code }}</code></pre>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.submissions-page {
  padding: 24px;
}

.submissions-container {
  max-width: 1200px;
  margin: 0 auto;
}

.table-card {
  border-radius: 16px;
  border: 1px solid #e2e8f0;
}

.filter-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  border-bottom: 1px dashed #f1f5f9;
  padding-bottom: 18px;
}

.input-group {
  display: flex;
  gap: 12px;
}

.btn-group {
  display: flex;
  gap: 10px;
}

/* 表格内微调样式 */
.custom-table {
  border-radius: 8px;
  overflow: hidden;
}

.problem-title-text {
  font-weight: bold;
  color: #1e293b;
}

.time-text {
  font-family: monospace;
  font-weight: 500;
  color: #475569;
}

.date-text {
  font-size: 13px;
  color: #64748b;
}

.lang-tag-item {
  text-transform: uppercase;
  font-weight: bold;
}

.pagination-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 24px;
  padding-top: 18px;
  border-top: 1px solid #f1f5f9;
}

.total-summary {
  font-size: 13.5px;
  color: #64748b;
}

.total-summary b {
  color: #0f172a;
}

/* 🌟 对话框内部样式 */
.submit-detail-dialog-body {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.meta-row {
  display: flex;
  gap: 24px;
  font-size: 14px;
  color: #475569;
  border-bottom: 1px solid #f1f5f9;
  padding-bottom: 12px;
}

.meta-row b {
  color: #0f172a;
}

/* 黑底终端控制台样式 */
.error-terminal-console {
  background-color: #0c0a09;
  border-radius: 8px;
  border: 1px solid #1c1917;
  padding: 12px 16px;
}

.terminal-header {
  font-size: 12.5px;
  color: #ef4444;
  font-weight: bold;
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.terminal-body {
  margin: 0;
  color: #f87171;
  font-family: Consolas, "Fira Code", monospace;
  font-size: 12.5px;
  white-space: pre-wrap;
  max-height: 140px;
  overflow-y: auto;
}

/* 极客深色代码阅读器样式 */
.code-editor-header {
  font-size: 13px;
  font-weight: bold;
  color: #475569;
}

.code-view-container {
  background-color: #1e1e1e;
  border-radius: 8px;
  padding: 16px;
  border: 1px solid #1e293b;
  max-height: 320px;
  overflow-y: auto;
}

.code-viewer-pre {
  margin: 0;
}

.code-viewer-pre code {
  font-family: Consolas, "Fira Code", monospace;
  font-size: 13.5px;
  color: #9cdcfe; /* 经典 VS Code 浅蓝代码色 */
  line-height: 1.6;
}
</style>
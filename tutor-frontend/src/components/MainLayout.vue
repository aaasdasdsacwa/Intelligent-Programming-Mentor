<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Compass, Document, User, SwitchButton, Cpu, Edit, InfoFilled } from '@element-plus/icons-vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import AiAssistant from './AiAssistant.vue' // 引入 AI 问答组件

const router = useRouter()
const route = useRoute()
const currentUser = ref(null)

// 动态绑定高亮
const activeMenu = ref(route.path)

// 修改密码弹窗相关状态
const passwordDialogVisible = ref(false)
const passwordLoading = ref(false)
const passwordFormRef = ref(null)
const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 表单前端规则判定
const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请设置新密码', trigger: 'blur' },
    { min: 6, message: '新密码不能少于 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码确认', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.value.newPassword) {
          callback(new Error('两次输入的新密码不一致！'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

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

// 退出登录
const handleLogout = async () => {
  try {
    const res = await axios.post('/api/user/logout')
    if (res.data.code === 0) {
      ElMessage.success('已安全退出登录')
      localStorage.removeItem('cached_path')
      localStorage.removeItem('cached_nodes')
      router.push('/login')
    }
  } catch (err) {
    router.push('/login')
  }
}

// 🌟 新增：提交修改密码请求
const handleUpdatePassword = () => {
  if (!passwordFormRef.value) return

  passwordFormRef.value.validate(async (valid) => {
    if (!valid) return

    passwordLoading.value = true
    try {
      const res = await axios.post('/api/user/update/password', {
        oldPassword: passwordForm.value.oldPassword,
        newPassword: passwordForm.value.newPassword
      })

      if (res.data.code === 0) {
        ElMessage({
          message: '🎉 密码修改成功！请使用新密码重新登录。',
          type: 'success',
          duration: 3000
        })
        passwordDialogVisible.value = false
        // 修改密码成功后，强制退出会话并返回登录大厅
        await handleLogout()
      } else {
        ElMessage.error(res.data.message || '密码修改失败')
      }
    } catch (err) {
      ElMessage.error('无法连接密码服务，请确认后端 update/password 接口已编译启动')
    } finally {
      passwordLoading.value = false
    }
  })
}

onMounted(() => {
  fetchUser()
})
</script>

<template>
  <el-container class="global-layout-container">
    <!-- 左侧高科技导航边栏 -->
    <el-aside width="240px" class="layout-sidebar">
      <div class="sidebar-brand-logo">
        <el-icon class="logo-icon"><Cpu /></el-icon>
        <span class="logo-text">智能编程导师</span>
      </div>

      <el-menu
          :default-active="activeMenu"
          class="sidebar-menu"
          background-color="#0f172a"
          text-color="#94a3b8"
          active-text-color="#3b82f6"
          router
      >
        <el-menu-item index="/workspace/roadmap">
          <el-icon><Compass /></el-icon>
          <span>🎯 智能学习路径</span>
        </el-menu-item>

        <el-menu-item index="/workspace/problems">
          <el-icon><Document /></el-icon>
          <span>📚 标准题库中心</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- 右侧内容与顶部信息栏 -->
    <el-container class="layout-main-section">
      <!-- 顶部 Header 用户状态栏 -->
      <el-header class="layout-header">
        <div class="header-left-title">
          <span>{{ route.name === 'Roadmap' ? '个性化路线规划' : '算法评测训练库' }}</span>
        </div>

        <div class="header-right-user" v-if="currentUser">
          <!-- 用户名 -->
          <div class="user-info-chip">
            <el-icon class="user-icon"><User /></el-icon>
            <span class="nickname">{{ currentUser.nickname || currentUser.username }}</span>
          </div>

          <el-divider direction="vertical" />

          <!-- 🌟 新增：修改密码按钮入口 -->
          <el-button
              type="primary"
              link
              :icon="Edit"
              class="password-link-btn"
              @click="passwordDialogVisible = true"
          >
            修改密码
          </el-button>

          <el-divider direction="vertical" />

          <!-- 退出登录 -->
          <el-button
              type="danger"
              link
              :icon="SwitchButton"
              class="logout-link-btn"
              @click="handleLogout"
          >
            退出
          </el-button>
        </div>
      </el-header>

      <!-- 子路由渲染区（开启 keep-alive 页面状态驻存） -->
      <el-main class="layout-content-viewer">
        <router-view v-slot="{ Component }">
          <keep-alive>
            <component :is="Component" :key="$route.fullPath" />
          </keep-alive>
        </router-view>
      </el-main>
    </el-container>

    <!-- 全局挂载：AI 编程答疑助手悬浮窗 -->
    <AiAssistant />

    <!-- 🌟 新增：修改密码 el-dialog 对话表单 -->
    <el-dialog
        v-model="passwordDialogVisible"
        title="🔐 修改账户登录密码"
        width="450px"
        destroy-on-close
    >
      <el-form
          ref="passwordFormRef"
          :model="passwordForm"
          :rules="passwordRules"
          label-position="top"
      >
        <el-form-item label="当前原密码" prop="oldPassword" required>
          <el-input
              v-model="passwordForm.oldPassword"
              type="password"
              placeholder="请输入您当前的登录密码"
              show-password
          />
        </el-form-item>

        <el-form-item label="设置新密码" prop="newPassword" required>
          <el-input
              v-model="passwordForm.newPassword"
              type="password"
              placeholder="请设置新的账户密码（不低于6位）"
              show-password
          />
        </el-form-item>

        <el-form-item label="再次输入新密码" prop="confirmPassword" required>
          <el-input
              v-model="passwordForm.confirmPassword"
              type="password"
              placeholder="请再次输入新密码以进行确认"
              show-password
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="passwordDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="passwordLoading" @click="handleUpdatePassword">
            确认修改
          </el-button>
        </span>
      </template>
    </el-dialog>
  </el-container>
</template>

<style scoped>
.global-layout-container {
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  background-color: #f8fafc;
}

/* 侧边栏样式 */
.layout-sidebar {
  background-color: #0f172a;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #1e293b;
}

.sidebar-brand-logo {
  height: 64px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 24px;
  border-bottom: 1px solid #1e293b;
}

.logo-icon {
  font-size: 22px;
  color: #3b82f6;
}

.logo-text {
  font-weight: bold;
  font-size: 16px;
  color: #fff;
  letter-spacing: 0.5px;
}

.sidebar-menu {
  border-right: none;
  padding-top: 10px;
}

:deep(.el-menu-item.is-active) {
  background-color: #1e293b !important;
  border-left: 4px solid #3b82f6;
  font-weight: bold;
}

.layout-main-section {
  display: flex;
  flex-direction: column;
}

.layout-header {
  height: 64px;
  background-color: #fff;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
}

.header-left-title {
  font-size: 16px;
  font-weight: bold;
  color: #0f172a;
}

.header-right-user {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-info-chip {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #475569;
}

.user-icon {
  color: #64748b;
}

.nickname {
  font-weight: 500;
}

.logout-link-btn {
  font-size: 13.5px;
}

/* 修改密码按钮微调 */
.password-link-btn {
  font-size: 13.5px;
  color: #3b82f6;
}

.password-link-btn:hover {
  color: #1d4ed8;
}

.layout-content-viewer {
  flex: 1;
  overflow-y: auto;
  padding: 0;
}
</style>
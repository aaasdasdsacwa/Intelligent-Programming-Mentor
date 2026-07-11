<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Compass, Document, User, SwitchButton, Cpu } from '@element-plus/icons-vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import AiAssistant from './AiAssistant.vue' // 引入我们写好的 AI 问答组件

const router = useRouter()
const route = useRoute()
const currentUser = ref(null)

// 根据路由路径高亮对应的菜单项
const activeMenu = ref(route.path)

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
      // 清空本地缓存的路线
      localStorage.removeItem('cached_path')
      localStorage.removeItem('cached_nodes')
      router.push('/login')
    }
  } catch (err) {
    router.push('/login')
  }
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
          <div class="user-info-chip">
            <el-icon class="user-icon"><User /></el-icon>
            <span class="nickname">{{ currentUser.nickname || currentUser.username }}</span>
          </div>
          <el-divider direction="vertical" />
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

      <!-- 子路由渲染区 -->
      <el-main class="layout-content-viewer">
        <!-- 🌟 核心优化：使用 Vue3 的 keep-alive 对主导航子页面进行视图缓存，切换 tab 页面不会被销毁重刷 -->
        <router-view v-slot="{ Component }">
          <keep-alive>
            <component :is="Component" :key="$route.fullPath" />
          </keep-alive>
        </router-view>
      </el-main>
    </el-container>

    <!-- 全局挂载：AI 编程答疑助手悬浮窗 -->
    <AiAssistant />
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

.layout-content-viewer {
  flex: 1;
  overflow-y: auto;
  padding: 0;
}
</style>
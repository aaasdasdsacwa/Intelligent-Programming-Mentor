<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const router = useRouter()
const isLoginMode = ref(true) // 切换登录/注册表单

// 表单数据
const loginForm = ref({
  username: '',
  password: ''
})

const registerForm = ref({
  username: '',
  password: '',
  checkPassword: ''
})

// 登录提交
const handleLogin = async () => {
  if (!loginForm.value.username || !loginForm.value.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  try {
    const res = await axios.post('/api/user/login', loginForm.value)
    if (res.data.code === 0) {
      ElMessage.success('登录成功！')
      // 成功登录后跳转到路线图规划页面（后续会写）
      router.push('/roadmap')
    } else {
      ElMessage.error(res.data.message || '登录失败')
    }
  } catch (err) {
    ElMessage.error('服务器响应异常，请检查后端是否开启')
  }
}

// 注册提交
const handleRegister = async () => {
  const { username, password, checkPassword } = registerForm.value
  if (!username || !password || !checkPassword) {
    ElMessage.warning('请填写完整的注册信息')
    return
  }
  if (password !== checkPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  try {
    const res = await axios.post('/api/user/register', registerForm.value)
    if (res.data.code === 0) {
      ElMessage.success('注册成功，已自动为你切换到登录！')
      // 注册成功，切换到登录面板，并预填好账号
      loginForm.value.username = username
      isLoginMode.value = true
    } else {
      ElMessage.error(res.data.message || '注册失败')
    }
  } catch (err) {
    ElMessage.error('服务器响应异常')
  }
}
</script>

<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <el-icon size="40" color="#409EFF"><Platform /></el-icon>
        <h2>智能编程导师系统</h2>
        <p>AI 助力每一行代码的成长</p>
      </div>

      <!-- 登录模式 -->
      <div v-if="isLoginMode" class="form-wrapper">
        <el-form :model="loginForm" label-position="top">
          <el-form-item label="账号">
            <el-input v-model="loginForm.username" placeholder="请输入用户名" prefix-icon="User" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="loginForm.password" type="password" show-password placeholder="请输入密码" prefix-icon="Lock" @keyup.enter="handleLogin" />
          </el-form-item>
          <el-button type="primary" class="submit-btn" @click="handleLogin">登 录</el-button>
        </el-form>
        <div class="switch-tip">
          还没有账号？<span @click="isLoginMode = false">立即注册</span>
        </div>
      </div>

      <!-- 注册模式 -->
      <div v-else class="form-wrapper">
        <el-form :model="registerForm" label-position="top">
          <el-form-item label="设置账号">
            <el-input v-model="registerForm.username" placeholder="长度大于 4 位" prefix-icon="User" />
          </el-form-item>
          <el-form-item label="设置密码">
            <el-input v-model="registerForm.password" type="password" show-password placeholder="密码需大于 6 位" prefix-icon="Lock" />
          </el-form-item>
          <el-form-item label="确认密码">
            <el-input v-model="registerForm.checkPassword" type="password" show-password placeholder="请再次输入密码" prefix-icon="Lock" @keyup.enter="handleRegister" />
          </el-form-item>
          <el-button type="success" class="submit-btn" @click="handleRegister">注 册</el-button>
        </el-form>
        <div class="switch-tip">
          已有账号？<span @click="isLoginMode = true">返回登录</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #e0f2fe 0%, #bae6fd 100%);
}

.login-card {
  width: 400px;
  background: rgba(255, 255, 255, 0.95);
  padding: 40px;
  border-radius: 16px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.login-header h2 {
  margin: 10px 0 5px 0;
  color: #1e293b;
  font-size: 24px;
}

.login-header p {
  margin: 0;
  color: #64748b;
  font-size: 14px;
}

.form-wrapper {
  margin-top: 10px;
}

.submit-btn {
  width: 100%;
  padding: 12px;
  font-size: 16px;
  margin-top: 10px;
  border-radius: 8px;
}

.switch-tip {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: #64748b;
}

.switch-tip span {
  color: #409EFF;
  cursor: pointer;
  font-weight: bold;
}

.switch-tip span:hover {
  text-decoration: underline;
}
</style>
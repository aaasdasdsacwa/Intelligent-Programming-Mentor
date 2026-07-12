import { createRouter, createWebHistory } from 'vue-router'
import axios from 'axios'

// 路由表定义
const routes = [
    {
        path: '/login',
        name: 'Login',
        component: () => import('../views/login/Index.vue')
    },
    {
        // 需要主导航栏的页面都放在 /workspace 这个父路由下
        path: '/workspace',
        component: () => import('../components/MainLayout.vue'),
        redirect: '/workspace/roadmap',
        children: [
            {
                path: 'roadmap',
                name: 'Roadmap',
                component: () => import('../views/roadmap/Index.vue')
            },
            {
                path: 'problems',
                name: 'ProblemList',
                component: () => import('../views/problems/ProblemList.vue')
            },
            {
                path: 'submissions',
                name: 'SubmissionHistory',
                component: () => import('../views/problems/SubmissionHistory.vue')
            }
        ]
    },
    {
        // 做题工作台需要沉浸式满屏，因此独立出来，不使用 MainLayout
        path: '/problems/:id',
        name: 'ProblemDetail',
        component: () => import('../views/problems/ProblemDetail.vue')
    },
    {
        // 默认重定向到工作空间
        path: '/:pathMatch(.*)*',
        redirect: '/workspace/roadmap'
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

// 简易路由守卫：校验登录状态
router.beforeEach(async (to, from, next) => {
    if (to.path === '/login') {
        next()
        return
    }
    try {
        const res = await axios.get('/api/user/get/login')
        if (res.data.code === 0) {
            next()
        } else {
            next('/login')
        }
    } catch (err) {
        next('/login')
    }
})

export default router
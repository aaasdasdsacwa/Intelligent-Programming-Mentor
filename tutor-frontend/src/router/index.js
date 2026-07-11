import { createRouter, createWebHistory } from 'vue-router'

const routes = [
    {
        path: '/',
        redirect: '/login'
    },
    {
        path: '/login',
        name: 'Login',
        component: () => import('../views/login/Index.vue'),
        meta: { title: '登录 - 智能编程导师' }
    },
    {
        path: '/problems',
        name: 'Problems',
        component: () => import('../views/problems/ProblemList.vue'),
        meta: { title: '题库中心' }
    },
    {
        path: '/problems/:id',
        name: 'ProblemDetail',
        component: () => import('../views/problems/ProblemDetail.vue'),
        meta: { title: '在线练习' }
    },
    {
        path: '/roadmap',
        name: 'Roadmap',
        component: () => import('../views/roadmap/Index.vue'),
        meta: { title: '学习路线图' }
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

// 动态修改浏览器标签页标题
router.beforeEach((to, from, next) => {
    if (to.meta.title) {
        document.title = to.meta.title
    }
    next()
})

export default router
import { createRouter, createWebHistory } from 'vue-router'
import BasicLayout from '@/layout/BasicLayout.vue'
import { ElMessage } from 'element-plus'
import { getUserInfo, clearAuth } from '@/utils/auth'

// 导入面试官路由配置
import interviewerRoutes from './D2_index.ts'

// 定义面试者系统的路由
const intervieweeRoutes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/login.vue')
  },

  {
    path: '/',
    component: BasicLayout,
    meta: { requiresAuth: true, role: 'CANDIDATE' },
    children: [
      {
        path: '',
        name: 'dashboard',
        component: () => import('@/views/dashboard.vue')
      },
      {
        path: 'interview',
        name: 'interview',
        component: () => import('@/views/interview.vue')
      },
      {
        path: 'interview-analysis-report',
        name: 'interview-analysis-report',
        component: () => import('@/views/interview/InterviewAnalysisReport.vue')
      },
      {
        path: 'jobs',
        name: 'jobs',
        component: () => import('@/views/jobs.vue')
      },
      {
        path: 'companies',
        name: 'companies',
        component: () => import('@/views/companies.vue')
      },
      {
        path: 'company/:id',
        name: 'company-detail',
        component: () => import('@/views/company/detail.vue')
      },
      {
        path: 'job/:id',
        name: 'job-detail',
        component: () => import('@/views/JobDetail.vue'),
        props: true
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('@/views/profile.vue')
      },
      {
        path: 'report',
        name: 'report',
        component: () => import('@/views/report.vue')
      },
      {
        path: 'resume-edit',
        name: 'resume-edit',
        component: () => import('@/views/resume-edit.vue')
      }
    ]
  }
];

// 创建路由实例，结合两套路由
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    ...intervieweeRoutes,
    ...interviewerRoutes,
    // 添加通配符路由用于重定向未匹配的URL
    {
      path: '/:pathMatch(.*)*',
      redirect: '/login'
    }
  ]
})

// 导出面试者路由配置
export { intervieweeRoutes };
export default router;

// 简化的路由守卫
router.beforeEach((to, from, next) => {
  // 如果目标路由需要认证
  if (to.matched.some(record => record.meta.requiresAuth)) {
    // 检查用户信息是否存在
    const userInfo = getUserInfo()
    
    // 如果用户未登录
    if (!userInfo) {
      console.error('请先登录')
      next({
        path: '/login',
        query: { redirect: to.fullPath } // 保存原本要访问的页面，登录后可以重定向回去
      })
      return
    }
    
    // 检查角色要求
    if (to.meta.role && to.meta.role !== userInfo.role) {
      if (userInfo.role === 'INTERVIEWER') {
        next('/interviewer/data-center/recruitment-analysis')
      } else {
        next('/')
      }
      return
    }
    
    // 通过验证，继续导航
    next()
  } 
  // 如果是登录页，但用户已登录
  else if (to.path === '/login') {
    const userInfo = getUserInfo()
    
    if (userInfo) {
      // 如果有重定向参数，则重定向到原本要访问的页面
      const redirectPath = to.query.redirect || (userInfo.role === 'INTERVIEWER' ? '/interviewer/data-center/recruitment-analysis' : '/')
      next(redirectPath)
      return
    }
  
    // 未登录，允许访问登录页
    next()
  } 
  else {
    // 检查是否访问根路径，需要根据用户角色重定向
    if (to.path === '/') {
      const userInfo = getUserInfo()
      
      if (userInfo && userInfo.role === 'INTERVIEWER') {
        // 面试官用户访问根路径，重定向到面试官端
        next('/interviewer/data-center/recruitment-analysis')
        return
      }
      // 应聘者或未登录用户访问根路径，正常显示应聘者端（但仍需登录验证）
    }
    
    // 其他页面正常导航
    next()
  }
}) 
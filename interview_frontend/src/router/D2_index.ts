import type { RouteRecordRaw } from 'vue-router';

// 定义面试官系统的路由
const interviewerRoutes: Array<RouteRecordRaw> = [
  {
    path: '/interviewer',
    component: () => import('../layouts/InterviewerLayout.vue'),
    meta: { requiresAuth: true, role: 'INTERVIEWER' },
    children: [
      {
        path: '',
        redirect: '/interviewer/talent-management/ai-screening'
      },
      // 人才管理
      {
        path: 'talent-management/ai-screening',
        name: 'InterviewerAIScreening',
        component: () => import('../views/D2-interviewer/talent-management/AIScreening.vue'),
        meta: { title: 'AI初选库', requiresAuth: true, role: 'INTERVIEWER' }
      },

      
      // 面试管理
      {
        path: 'interview-management/schedule',
        name: 'InterviewerInterviewSchedule',
        component: () => import('../views/D2-interviewer/interview-management/InterviewSchedule.vue'),
        meta: { title: '面试安排', requiresAuth: true, role: 'INTERVIEWER' }
      },

      
      // 数据中心
      {
        path: 'data-center/recruitment-analysis',
        name: 'InterviewerRecruitmentAnalysis',
        component: () => import('../views/D2-interviewer/data-center/RecruitmentAnalysis.vue'),
        meta: { title: '招聘分析', requiresAuth: true, role: 'INTERVIEWER' }
      },
      {
        path: 'data-center/question-analysis',
        name: 'InterviewerQuestionAnalysis',
        component: () => import('../views/D2-interviewer/data-center/QuestionAnalysis.vue'),
        meta: { title: '题库分析', requiresAuth: true, role: 'INTERVIEWER' }
      },

      
      // 系统设置

      {
        path: 'system-settings/company-info',
        name: 'InterviewerCompanyInfo',
        component: () => import('../views/D2-interviewer/system-settings/CompanyInfo.vue'),
        meta: { title: '企业信息', requiresAuth: true, role: 'INTERVIEWER' }
      },
      
      // 岗位管理
      {
        path: 'position-management/position-list',
        name: 'InterviewerPositionList',
        component: () => import('../views/D2-interviewer/position-management/PositionList.vue'),
        meta: { title: '岗位列表', requiresAuth: true, role: 'INTERVIEWER' }
      },

      {
        path: 'position-management/detail/:id',
        name: 'InterviewerPositionDetail',
        component: () => import('../views/D2-interviewer/position-management/PositionDetail.vue'),
        meta: { title: '岗位详情', requiresAuth: true, role: 'INTERVIEWER' }
      },
      
      // 题库中心
      {
        path: 'question-bank/management',
        name: 'InterviewerQuestionManagement',
        component: () => import('../views/D2-interviewer/question-bank/QuestionManagement.vue'),
        meta: { title: '题目管理', requiresAuth: true, role: 'INTERVIEWER' }
      },

    ]
  }
];

export default interviewerRoutes; 
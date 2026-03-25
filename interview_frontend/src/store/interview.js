import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 面试状态管理
 * @returns {Object} 面试状态和方法
 */
export const useInterviewStore = defineStore('interview', () => {
  // 修改状态定义：none(未投递) | applied/pending1(已投递) | pending2(待面试) | ongoing(已面试) | completed(分析报告)
  const status = ref('none') 
  const job = ref(null) // 当前申请的岗位
  const sessionId = ref(null) // 面试会话ID
  const interviewTime = ref(null) // 添加面试时间字段
  const applicationId = ref(null) // 申请记录ID - 用于关联面试会话

  // 保存状态到localStorage
  const saveState = () => {
    try {
      // 使用单次操作，减少多次写入localStorage
      const state = {
        status: status.value,
        job: job.value,
        sessionId: sessionId.value,
        interviewTime: interviewTime.value,
        applicationId: applicationId.value
      }

      // 单次操作保存状态对象
      localStorage.setItem('interviewState', JSON.stringify(state))
      console.log('面试状态已保存到localStorage (优化批量写入)')

      // 为了向后兼容，也单独保存各个状态
      localStorage.setItem('interviewStatus', status.value)
      console.log('保存面试状态到localStorage:', status.value)
      
      // 保存岗位信息
      if (job.value) {
        try {
          // 在存储前检查和修复可能导致JSON序列化失败的问题
          const jobCopy = { ...job.value }

          // 修复循环引用、特殊对象等
          const safeJob = JSON.parse(JSON.stringify(jobCopy))
          localStorage.setItem('pendingInterviewJob', JSON.stringify(safeJob))
          console.log('保存岗位信息到localStorage成功')
        } catch (jobError) {
          console.error('保存岗位信息失败，尝试备份关键字段:', jobError)

          // 如果完整对象存储失败，至少保存关键字段
          const essentialData = {
            id: job.value.id,
            title: job.value.title,
            companyName: job.value.companyName,
            applicationId: job.value.applicationId
          }
          localStorage.setItem('pendingInterviewJob', JSON.stringify(essentialData))
          console.log('已保存简化版岗位信息')
        }
      }
      
      // 保存会话ID
      if (sessionId.value) {
        localStorage.setItem('interviewSessionId', String(sessionId.value))
        console.log('保存会话ID到localStorage:', sessionId.value)
      }
      
      // 保存面试时间
      if (interviewTime.value) {
        localStorage.setItem('interviewTime', interviewTime.value)
      }

      if (applicationId.value) {
        localStorage.setItem('applicationId', String(applicationId.value))
      }
    } catch (error) {
      console.error('保存状态到localStorage失败:', error)
    }
  }
  
  // 从本地存储恢复状态
  const initState = () => {
    try {
      // 首先尝试从优化后的单一状态对象中恢复
      const storedState = localStorage.getItem('interviewState')

      if (storedState) {
        try {
          const parsedState = JSON.parse(storedState)
          if (parsedState) {
            status.value = parsedState.status || 'none'
            job.value = parsedState.job || null
            sessionId.value = parsedState.sessionId ? Number(parsedState.sessionId) : null
            interviewTime.value = parsedState.interviewTime || null
            applicationId.value = parsedState.applicationId ? Number(parsedState.applicationId) : null

            console.log('从优化存储中恢复完整面试状态')
            return // 成功恢复，提前返回
          }
        } catch (e) {
          console.error('解析优化存储状态失败，将尝试传统方式:', e)
        }
      }

      // 传统方式：单独恢复各个状态（向后兼容）
      const storedStatus = localStorage.getItem('interviewStatus')
      if (storedStatus) {
        status.value = storedStatus
        console.log('从localStorage恢复面试状态:', status.value)
      } else {
        console.log('localStorage中没有面试状态，使用默认值:', status.value)
      }
      
      // 恢复岗位信息
      const storedJob = localStorage.getItem('pendingInterviewJob')
      if (storedJob) {
        try {
          job.value = JSON.parse(storedJob)

          // 检查和修复可能的数据类型问题
          if (job.value && typeof job.value.id === 'string' && !isNaN(Number(job.value.id))) {
            job.value.id = Number(job.value.id)
            console.log('修正job.id的数据类型为数字')
          }

          if (job.value && job.value.applicationId &&
              typeof job.value.applicationId === 'string' &&
              !isNaN(Number(job.value.applicationId))) {
            job.value.applicationId = Number(job.value.applicationId)
            console.log('修正job.applicationId的数据类型为数字')
          }

          console.log('从localStorage恢复岗位信息:', job.value)
        } catch (e) {
          console.error('解析岗位信息失败:', e)
          // 解析失败时清除可能损坏的数据
          localStorage.removeItem('pendingInterviewJob')
          job.value = null
        }
      } else {
        console.log('localStorage中没有岗位信息')
      }
      
      // 恢复会话ID
      const storedSessionId = localStorage.getItem('interviewSessionId')
      if (storedSessionId) {
        try {
          sessionId.value = Number(storedSessionId)
          console.log('从localStorage恢复会话ID:', sessionId.value)
          // 检查是否是有效数字
          if (isNaN(sessionId.value)) {
            console.warn('会话ID不是有效数字，重置为null')
            sessionId.value = null
          }
        } catch (e) {
          console.error('解析会话ID失败:', e)
          sessionId.value = null
        }
      } else {
        console.log('localStorage中没有会话ID')
      }
      
      // 恢复面试时间
      const storedInterviewTime = localStorage.getItem('interviewTime')
      if (storedInterviewTime) {
        interviewTime.value = storedInterviewTime
        console.log('从localStorage恢复面试时间:', interviewTime.value)
      }

      const storedApplicationId = localStorage.getItem('applicationId')
      if (storedApplicationId) {
        applicationId.value = Number(storedApplicationId)
        console.log('从localStorage恢复申请记录ID:', applicationId.value)
      }
    } catch (error) {
      console.error('从localStorage恢复状态失败:', error)
    }
  }
  
  // 设置面试状态
  const setStatus = (newStatus) => {
    status.value = newStatus
    saveState()
  }
  
  // 设置岗位信息
  const setJob = (newJob) => {
    job.value = newJob
    saveState()
  }
  
  // 更新岗位信息(添加或更新某些属性)
  const updateJob = (updatedJobData) => {
    if (!job.value) return;
    job.value = { ...job.value, ...updatedJobData };
    console.log('岗位信息已更新:', job.value);
    saveState();
  }
  
  // 设置会话ID
  const setSessionId = (newSessionId) => {
    sessionId.value = newSessionId
    saveState()
  }
  
  // 设置面试时间
  const setInterviewTime = (newTime) => {
    interviewTime.value = newTime
    saveState()
  }
  
  // 设置申请记录ID
  const setApplicationId = (newApplicationId) => {
    applicationId.value = newApplicationId
    saveState()
  }

  // 重置状态
  const resetState = () => {
    status.value = 'none'
    job.value = null
    sessionId.value = null
    interviewTime.value = null
    applicationId.value = null

    // 清除localStorage - 一次性清除优化存储
    localStorage.removeItem('interviewState')

    // 清除传统存储（兼容）
    localStorage.removeItem('interviewStatus')
    localStorage.removeItem('pendingInterviewJob')
    localStorage.removeItem('interviewSessionId')
    localStorage.removeItem('lastReportId') // 添加清除报告ID
    localStorage.removeItem('interviewTime')
    localStorage.removeItem('applicationId')

    console.log('已重置面试状态')
  }
  
  // 初始化
  initState()
  
  return { 
    status, 
    job, 
    sessionId,
    interviewTime,
    applicationId,
    setStatus,
    setJob,
    updateJob,
    setSessionId,
    setInterviewTime,
    setApplicationId,
    resetState
  }
})

// 导出类型定义，方便其他文件引用
export const InterviewTypes = {}
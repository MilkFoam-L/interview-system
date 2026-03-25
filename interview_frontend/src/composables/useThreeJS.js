import { ref, onUnmounted } from 'vue'

/**
 * Three.js 性能优化和设备检测 Composable
 */
export function useThreeJS() {
  const isLowEndDevice = ref(false)
  const gpuInfo = ref('')
  const performanceScore = ref(0)
  
  // 检测设备性能
  const detectDevicePerformance = () => {
    try {
      // 检测硬件并发数
      const cores = navigator.hardwareConcurrency || 2
      
      // 检测设备类型
      const isMobile = /iPhone|iPad|iPod|Android/i.test(navigator.userAgent)
      const isTablet = /iPad|Android.*(?=.*Tablet)/i.test(navigator.userAgent)
      
      // 检测内存（如果支持）
      const memory = navigator.deviceMemory || 4
      
      // 检测WebGL支持
      const canvas = document.createElement('canvas')
      const gl = canvas.getContext('webgl') || canvas.getContext('experimental-webgl')
      
      if (!gl) {
        isLowEndDevice.value = true
        performanceScore.value = 0
        return
      }
      
      // 获取GPU信息
      const debugInfo = gl.getExtension('WEBGL_debug_renderer_info')
      if (debugInfo) {
        const renderer = gl.getParameter(debugInfo.UNMASKED_RENDERER_WEBGL)
        gpuInfo.value = renderer
        console.log('GPU:', renderer)
        
        // 简单的GPU性能评分
        if (renderer.includes('Intel') && renderer.includes('HD')) {
          performanceScore.value = 2 // 集成显卡
        } else if (renderer.includes('Intel') && renderer.includes('Iris')) {
          performanceScore.value = 4 // 较好的集成显卡
        } else if (renderer.includes('NVIDIA') || renderer.includes('AMD') || renderer.includes('Radeon')) {
          performanceScore.value = 7 // 独立显卡
        } else {
          performanceScore.value = 3 // 未知GPU
        }
      }
      
      // 综合评分
      let totalScore = performanceScore.value
      totalScore += Math.min(cores / 2, 4) // CPU核心加分，最多4分
      totalScore += Math.min(memory / 2, 4) // 内存加分，最多4分
      
      if (isMobile) totalScore -= 3 // 移动设备减分
      if (isTablet) totalScore -= 1 // 平板设备轻微减分
      
      // 设置性能等级
      if (totalScore < 6 || isMobile) {
        isLowEndDevice.value = true
      }
      
      console.log('Performance Score:', totalScore, 'Low End:', isLowEndDevice.value)
      
    } catch (error) {
      console.warn('Performance detection failed:', error)
      isLowEndDevice.value = true // 默认为低端设备
    }
  }
  
  // 获取优化的Three.js配置
  const getOptimizedConfig = () => {
    const lowEnd = isLowEndDevice.value
    
    return {
      // 渲染器配置
      renderer: {
        antialias: !lowEnd,
        alpha: true,
        powerPreference: lowEnd ? 'low-power' : 'high-performance',
        pixelRatio: lowEnd ? 1 : Math.min(window.devicePixelRatio, 2),
        shadowMap: !lowEnd,
        shadowMapType: lowEnd ? null : 'PCFSoftShadowMap'
      },
      
      // 几何体配置
      geometry: {
        segments: lowEnd ? 8 : 16, // 降低几何体细节
        detail: lowEnd ? 1 : 2
      },
      
      // 材质配置
      material: {
        roughness: lowEnd ? 0.8 : 0.4,
        metalness: lowEnd ? 0.2 : 0.6,
        envMapIntensity: lowEnd ? 0.5 : 1.0
      },
      
      // 动画配置
      animation: {
        maxFPS: lowEnd ? 30 : 60,
        updateFrequency: lowEnd ? 2 : 1 // 更新频率倍数
      },
      
      // 灯光配置
      lighting: {
        maxLights: lowEnd ? 2 : 4,
        shadowResolution: lowEnd ? 512 : 1024
      }
    }
  }
  
  // 帧率监控
  const frameRateMonitor = {
    lastTime: 0,
    frameCount: 0,
    currentFPS: 0,
    targetFPS: 60,
    
    update() {
      const now = performance.now()
      this.frameCount++
      
      if (now - this.lastTime >= 1000) {
        this.currentFPS = Math.round((this.frameCount * 1000) / (now - this.lastTime))
        this.frameCount = 0
        this.lastTime = now
        
        // 如果帧率过低，建议降级
        if (this.currentFPS < 20 && !isLowEndDevice.value) {
          console.warn('Low FPS detected, consider enabling low-end mode')
        }
      }
    },
    
    getFPS() {
      return this.currentFPS
    }
  }
  
  // 内存监控
  const memoryMonitor = {
    check() {
      if (performance.memory) {
        const memory = performance.memory
        return {
          used: Math.round(memory.usedJSHeapSize / 1048576), // MB
          total: Math.round(memory.totalJSHeapSize / 1048576), // MB
          limit: Math.round(memory.jsHeapSizeLimit / 1048576) // MB
        }
      }
      return null
    },
    
    isMemoryPressure() {
      const info = this.check()
      if (!info) return false
      
      return (info.used / info.limit) > 0.8 // 超过80%内存使用率
    }
  }
  
  // 自适应质量调整
  const adaptiveQuality = {
    currentLevel: 'high',
    levels: ['low', 'medium', 'high'],
    
    adjustQuality(fps, memoryPressure) {
      if (fps < 20 || memoryPressure) {
        this.currentLevel = 'low'
      } else if (fps < 45) {
        this.currentLevel = 'medium'
      } else {
        this.currentLevel = 'high'
      }
      
      return this.currentLevel
    },
    
    getQualitySettings(level) {
      const settings = {
        low: {
          pixelRatio: 0.75,
          shadowMap: false,
          antialias: false,
          animationFPS: 30
        },
        medium: {
          pixelRatio: 1,
          shadowMap: true,
          antialias: false,
          animationFPS: 45
        },
        high: {
          pixelRatio: Math.min(window.devicePixelRatio, 2),
          shadowMap: true,
          antialias: true,
          animationFPS: 60
        }
      }
      
      return settings[level] || settings.medium
    }
  }
  
  // 初始化检测
  detectDevicePerformance()
  
  // 清理函数
  onUnmounted(() => {
    // 清理监控器
    frameRateMonitor.frameCount = 0
    frameRateMonitor.lastTime = 0
  })
  
  return {
    // 性能状态
    isLowEndDevice,
    gpuInfo,
    performanceScore,
    
    // 配置获取
    getOptimizedConfig,
    
    // 监控器
    frameRateMonitor,
    memoryMonitor,
    adaptiveQuality,
    
    // 工具函数
    detectDevicePerformance
  }
}

/**
 * 3D模型优化工具函数
 */
export function optimizeModel(model, isLowEnd = false) {
  model.traverse((child) => {
    if (child.isMesh) {
      // 设置阴影
      child.castShadow = !isLowEnd
      child.receiveShadow = !isLowEnd
      
      // 优化材质
      if (child.material) {
        if (isLowEnd) {
          // 低端设备简化材质
          child.material.roughness = 0.8
          child.material.metalness = 0.2
          child.material.envMapIntensity = 0.5
        }
        
        child.material.needsUpdate = true
      }
      
      // 几何体优化
      if (child.geometry && isLowEnd) {
        // 可以在这里添加几何体简化逻辑
        child.geometry.computeBoundingSphere()
      }
    }
  })
  
  return model
}
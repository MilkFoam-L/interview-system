<template>
  <div class="avatar-3d-wrapper">
    <!-- 加载状态 -->
    <div v-if="isLoading" class="avatar-loading">
      <el-icon class="is-loading loading-icon"><Loading /></el-icon>
      <span class="loading-text">加载虚拟形象中...</span>
    </div>
    
    <!-- 3D容器 -->
    <div 
      v-show="!isLoading" 
      ref="containerRef" 
      class="avatar-3d-container"
      @mousemove="onMouseMove"
      @mouseleave="onMouseLeave"
    ></div>
    
    <!-- 错误状态回退到图标 -->
    <div v-if="hasError" class="avatar-fallback">
      <el-icon class="fallback-icon"><User /></el-icon>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { Loading, User } from '@element-plus/icons-vue'
import * as THREE from 'three'
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader.js'
import { useThreeJS } from '@/composables/useThreeJS'

// Props
const props = defineProps({
  modelPath: {
    type: String,
    default: '/models/AI_image02.glb'
  },
  isPlaying: {
    type: Boolean,
    default: false
  },
  width: {
    type: Number,
    default: 200
  },
  height: {
    type: Number,
    default: 200
  },
  audioElement: {
    type: HTMLAudioElement,
    default: null
  },
  enableSpeechAnimation: {
    type: Boolean,
    default: true
  }
})

// Refs
const containerRef = ref(null)
const hasError = ref(false)
const isLoading = ref(true)

// 使用性能优化 composables
const { isLowEndDevice, getOptimizedConfig, frameRateMonitor, memoryMonitor } = useThreeJS()

// Three.js 相关变量
let scene, camera, renderer, model, mixer, clock
let isInitialized = false
let animationId = null

// 音频分析相关变量
let audioContext = null
let analyser = null
let audioSource = null
let frequencyData = null
let audioVolume = 0
let isAnalyzing = false

// 动画相关变量
let mouthAnimation = null
let idleAnimation = null
let currentSpeechIntensity = 0

// 动画参数
const MOUTH_ANIMATION_INTENSITY = 0.8
const SPEECH_THRESHOLD = 0.01 // 语音检测阈值

// 初始化Three.js场景
const initThreeJS = async () => {
  if (!containerRef.value || isInitialized) return

  try {
    // 获取优化配置
    const config = getOptimizedConfig()
    
    // 创建场景
    scene = new THREE.Scene()
    
    // 创建相机
    camera = new THREE.PerspectiveCamera(
      45, 
      props.width / props.height, 
      0.1, 
      1000
    )
    camera.position.set(0, 0, 2.5)

    // 创建渲染器
    renderer = new THREE.WebGLRenderer(config.renderer)
    renderer.setSize(props.width, props.height)
    renderer.setPixelRatio(config.renderer.pixelRatio)
    renderer.outputColorSpace = THREE.SRGBColorSpace
    
    // 启用阴影
    if (config.renderer.shadowMap) {
      renderer.shadowMap.enabled = true
      renderer.shadowMap.type = THREE.PCFSoftShadowMap
    }

    // 添加到DOM
    containerRef.value.appendChild(renderer.domElement)

    // 添加灯光
    const ambientLight = new THREE.AmbientLight(0xffffff, 0.6)
    scene.add(ambientLight)

    const directionalLight = new THREE.DirectionalLight(0xffffff, 0.8)
    directionalLight.position.set(1, 1, 1)
    if (config.renderer.shadowMap) {
      directionalLight.castShadow = true
      directionalLight.shadow.mapSize.width = config.lighting.shadowResolution
      directionalLight.shadow.mapSize.height = config.lighting.shadowResolution
    }
    scene.add(directionalLight)

    // 添加补光
    const fillLight = new THREE.DirectionalLight(0xffffff, 0.3)
    fillLight.position.set(-1, 0, 1)
    scene.add(fillLight)

    // 创建时钟用于动画
    clock = new THREE.Clock()

    isInitialized = true
    
    // 加载GLB模型
    await loadModel()
    
    // 开始渲染循环
    animate()
    
  } catch (error) {
    console.error('Three.js initialization failed:', error)
    hasError.value = true
    isLoading.value = false
  }
}

// 初始化音频分析器
const initAudioAnalyzer = () => {
  if (!props.audioElement || !props.enableSpeechAnimation) return
  
  try {
    // 创建音频上下文
    if (!audioContext) {
      audioContext = new (window.AudioContext || window.webkitAudioContext)()
    }
    
    // 如果音频上下文被暂停，尝试恢复
    if (audioContext.state === 'suspended') {
      audioContext.resume()
    }
    
    // 创建分析器节点
    if (!analyser) {
      analyser = audioContext.createAnalyser()
      analyser.fftSize = 256
      analyser.minDecibels = -100
      analyser.maxDecibels = -10
      analyser.smoothingTimeConstant = 0.85
      
      // 创建频域数组
      frequencyData = new Uint8Array(analyser.frequencyBinCount)
    }
    
    // 连接音频源
    if (!audioSource && props.audioElement) {
      audioSource = audioContext.createMediaElementSource(props.audioElement)
      audioSource.connect(analyser)
      analyser.connect(audioContext.destination)
    }
    
    isAnalyzing = true
    console.log('🎤 音频分析器初始化成功')
    
  } catch (error) {
    console.warn('音频分析器初始化失败:', error)
    isAnalyzing = false
  }
}

// 清理音频分析器
const cleanupAudioAnalyzer = () => {
  isAnalyzing = false
  
  if (audioSource) {
    audioSource.disconnect()
    audioSource = null
  }
  
  if (analyser) {
    analyser.disconnect()
    analyser = null
  }
  
  if (audioContext && audioContext.state !== 'closed') {
    audioContext.close()
    audioContext = null
  }
  
  frequencyData = null
  audioVolume = 0
}

// 分析音频获取音量
const analyzeAudio = () => {
  if (!isAnalyzing || !analyser || !frequencyData) return 0
  
  try {
    // 获取频域数据
    analyser.getByteFrequencyData(frequencyData)
    
    // 计算平均音量
    let sum = 0
    for (let i = 0; i < frequencyData.length; i++) {
      sum += frequencyData[i]
    }
    
    // 归一化音量值 (0-1)
    const volume = sum / (frequencyData.length * 255)
    
    // 平滑处理
    audioVolume = audioVolume * 0.7 + volume * 0.3
    
    return audioVolume
    
  } catch (error) {
    console.warn('音频分析失败:', error)
    return 0
  }
}

// 加载GLB模型
const loadModel = async () => {
  const loader = new GLTFLoader()
  
  try {
    const gltf = await new Promise((resolve, reject) => {
      loader.load(
        props.modelPath,
        resolve,
        (progress) => {
          console.log('Loading progress:', (progress.loaded / progress.total * 100) + '%')
        },
        reject
      )
    })

    model = gltf.scene
    
    // 调整模型大小和位置
    const box = new THREE.Box3().setFromObject(model)
    const center = box.getCenter(new THREE.Vector3())
    const size = box.getSize(new THREE.Vector3())
    
    // 居中模型
    model.position.sub(center)
    
    // 缩放模型以适应容器
    const maxSize = Math.max(size.x, size.y, size.z)
    const scale = 1.5 / maxSize
    model.scale.setScalar(scale)

    // 设置模型接收和投射阴影
    model.traverse((child) => {
      if (child.isMesh) {
        child.castShadow = !isLowEndDevice.value
        child.receiveShadow = !isLowEndDevice.value
        
        // 优化材质
        if (child.material) {
          if (isLowEndDevice.value) {
            // 低端设备简化材质
            child.material.roughness = 0.8
            child.material.metalness = 0.2
          }
          child.material.needsUpdate = true
        }
      }
    })

    scene.add(model)
    
    // 设置动画混合器
    if (gltf.animations && gltf.animations.length > 0) {
      mixer = new THREE.AnimationMixer(model)
      
      gltf.animations.forEach((clip) => {
        const action = mixer.clipAction(clip)
        const clipName = clip.name.toLowerCase()
        
        // 分类动画并保存引用
        if (clipName.includes('talk') || clipName.includes('speak') || clipName.includes('mouth')) {
          // 嘴巴说话动画
          mouthAnimation = action
          action.setLoop(THREE.LoopRepeat)
          action.weight = 0 // 初始权重为0
          action.play()
        } else if (clipName.includes('idle') || clipName.includes('breath') || clipName.includes('blink')) {
          // 空闲动画始终播放
          idleAnimation = action
          action.setLoop(THREE.LoopRepeat)
          action.weight = 1
          action.play()
        } else {
          // 其他动画，默认循环播放
          action.setLoop(THREE.LoopRepeat)
          action.play()
        }
      })
    }
    
    // 如果没有找到预定义动画，创建程序化动画
    if (!mouthAnimation) {
      console.log('未找到嘴巴动画，将使用程序化嘴巴动画')
    }
    
    isLoading.value = false
    console.log('🎭 3D虚拟人形象加载成功 - 新用户和老用户都能看到！')
    console.log('📊 模型动画数量:', gltf.animations ? gltf.animations.length : 0)
    console.log('🎬 3D角色已准备就绪，支持语音同步动画')
    
  } catch (error) {
    console.error('Model loading failed:', error)
    hasError.value = true
    isLoading.value = false
    
    // 回退到图标显示
    setTimeout(() => {
      hasError.value = true
    }, 1000)
  }
}

// 程序化嘴巴动画（当没有预定义动画时）
const updateProceduralMouthAnimation = (intensity) => {
  if (!model) return
  
  // 查找嘴巴相关的骨骼或网格
  let foundMouthParts = false
  
  model.traverse((child) => {
    if (child.isMesh && child.name) {
      const name = child.name.toLowerCase()
      
      // 检查是否是嘴巴相关部件
      if (name.includes('mouth') || name.includes('jaw') || 
          name.includes('lip') || name.includes('teeth') ||
          name.includes('tongue') || name.includes('face')) {
        
        foundMouthParts = true
        
        // 嘴巴张合效果 - 使用多种变换
        const scaleY = 1 + intensity * 0.25
        const scaleZ = 1 + intensity * 0.15
        
        child.scale.y = THREE.MathUtils.lerp(child.scale.y, scaleY, 0.12)
        child.scale.z = THREE.MathUtils.lerp(child.scale.z, scaleZ, 0.08)
        
        // 轻微的位置偏移
        if (name.includes('jaw') || name.includes('mouth')) {
          const offsetY = intensity * 0.02
          child.position.y = THREE.MathUtils.lerp(child.position.y, offsetY - 0.01, 0.1)
        }
      }
    }
    
    // 如果是带骨骼的模型，尝试通过骨骼控制
    if (child.isSkinnedMesh && child.skeleton) {
      const bones = child.skeleton.bones
      
      bones.forEach(bone => {
        const boneName = bone.name.toLowerCase()
        
        if (boneName.includes('jaw') || boneName.includes('mouth') || 
            boneName.includes('lip') || boneName.includes('mandible')) {
          
          foundMouthParts = true
          
          // 下颚骨骼旋转
          if (boneName.includes('jaw') || boneName.includes('mandible')) {
            const targetRotation = intensity * 0.3
            bone.rotation.x = THREE.MathUtils.lerp(bone.rotation.x, targetRotation, 0.15)
          }
          
          // 嘴唇骨骼位置调整
          if (boneName.includes('lip')) {
            const offsetScale = intensity * 0.02
            bone.position.y = THREE.MathUtils.lerp(bone.position.y, offsetScale, 0.1)
          }
        }
      })
    }
  })
  
  // 如果没有找到特定的嘴巴部件，对整个头部进行轻微变形
  if (!foundMouthParts) {
    model.traverse((child) => {
      if (child.isMesh && child.name) {
        const name = child.name.toLowerCase()
        
        if (name.includes('head') || name.includes('face') || name === 'mesh' || !child.name) {
          // 轻微的整体缩放变化模拟说话
          const scaleVariation = 1 + intensity * 0.015
          child.scale.y = THREE.MathUtils.lerp(child.scale.y, scaleVariation, 0.08)
          
          // 轻微的Y轴旋转
          const rotationVariation = (Math.sin(Date.now() * 0.01) * intensity * 0.02)
          child.rotation.y = THREE.MathUtils.lerp(child.rotation.y, rotationVariation, 0.05)
        }
      }
    })
  }
}



// 动画循环
const animate = () => {
  if (!renderer || !scene || !camera) return
  
  animationId = requestAnimationFrame(animate)
  
  // 性能监控
  frameRateMonitor.update()
  
  // 检查内存压力
  if (frameRateMonitor.getFPS() < 20 || memoryMonitor.isMemoryPressure()) {
    console.warn('Performance warning: Low FPS or Memory pressure detected')
  }
  
  const delta = clock.getDelta()
  const currentTime = clock.getElapsedTime()
  
  // 音频分析和语音动画
  if (props.isPlaying && props.enableSpeechAnimation) {
    // 分析音频获取音量
    const volume = analyzeAudio()
    
    // 计算语音强度（带一些随机性增加自然感）
    currentSpeechIntensity = volume + Math.random() * 0.1
    
    // 控制嘴巴动画
    if (mouthAnimation) {
      // 使用预定义动画
      const targetWeight = Math.max(currentSpeechIntensity * MOUTH_ANIMATION_INTENSITY, 0.1)
      mouthAnimation.weight = THREE.MathUtils.lerp(mouthAnimation.weight, targetWeight, 0.1)
    } else {
      // 使用程序化动画
      updateProceduralMouthAnimation(currentSpeechIntensity)
    }
    

    
  } else {
    // 语音停止时重置动画
    currentSpeechIntensity = 0
    
    if (mouthAnimation) {
      mouthAnimation.weight = THREE.MathUtils.lerp(mouthAnimation.weight, 0, 0.1)
    }
    
    // 重置程序化动画
    updateProceduralMouthAnimation(0)
  }
  
  // 更新动画混合器
  if (mixer) {
    mixer.update(delta)
  }
  
  // 简单的自转动画（仅在非播放状态）
  if (model && !props.isPlaying) {
    model.rotation.y += 0.003
  }
  
  renderer.render(scene, camera)
}

// 鼠标交互
const onMouseMove = (event) => {
  if (!model) return
  
  const rect = containerRef.value.getBoundingClientRect()
  const x = ((event.clientX - rect.left) / rect.width) * 2 - 1
  const y = -((event.clientY - rect.top) / rect.height) * 2 + 1
  
  // 轻微的头部跟随鼠标效果
  if (model) {
    model.rotation.x = y * 0.05
    model.rotation.y += (x * 0.1 - model.rotation.y) * 0.05
  }
}

const onMouseLeave = () => {
  if (!model) return
  
  // 恢复默认姿态
  model.rotation.x = 0
  model.rotation.y = 0
}

// 监听播放状态变化
watch(() => props.isPlaying, (isPlaying) => {
  console.log('🎭 语音播放状态变化:', isPlaying ? '开始播放' : '停止播放')
  
  if (isPlaying) {
    // 开始播放时初始化音频分析器
    if (props.audioElement && props.enableSpeechAnimation) {
      setTimeout(() => {
        initAudioAnalyzer()
      }, 100) // 稍微延迟确保音频元素准备就绪
    }
    
    // 控制模型的额外动画效果
    if (model) {
      // 播放时稍微放大
      model.scale.multiplyScalar(1.02)
    }
    
    console.log('🗣️ 开始语音同步动画 - 嘴巴张合动画已启用')
    
  } else {
    // 停止播放时清理音频分析器
    cleanupAudioAnalyzer()
    
    // 重置动画状态
    currentSpeechIntensity = 0
    
    // 停止所有语音相关动画
    if (mouthAnimation) {
      mouthAnimation.weight = 0
    }
    
    // 控制模型的额外动画效果
    if (model) {
      // 停止时恢复原大小
      model.scale.multiplyScalar(1 / 1.02)
    }
    
    console.log('🤐 语音动画已停止 - 虚拟人回到静息状态')
  }
})

// 监听音频元素变化
watch(() => props.audioElement, (newAudioElement, oldAudioElement) => {
  if (oldAudioElement) {
    cleanupAudioAnalyzer()
  }
  
  if (newAudioElement && props.isPlaying && props.enableSpeechAnimation) {
    setTimeout(() => {
      initAudioAnalyzer()
    }, 100)
  }
})

// 清理函数
const cleanup = () => {
  // 清理动画
  if (animationId) {
    cancelAnimationFrame(animationId)
    animationId = null
  }
  
  // 清理音频分析器
  cleanupAudioAnalyzer()
  
  // 清理动画混合器
  if (mixer) {
    mixer.stopAllAction()
    mixer = null
  }
  
  // 清理Three.js对象
  if (renderer) {
    renderer.dispose()
    renderer = null
  }
  
  if (scene) {
    scene.clear()
    scene = null
  }
  
  // 重置变量
  camera = null
  model = null
  clock = null
  mouthAnimation = null
  idleAnimation = null
  currentSpeechIntensity = 0
  
  console.log('🧹 Avatar3D 组件清理完成')
}

// 生命周期
onMounted(async () => {
  await nextTick()
  setTimeout(() => {
    initThreeJS()
  }, 100) // 稍微延迟确保DOM已渲染
})

onUnmounted(() => {
  cleanup()
})
</script>

<style scoped>
.avatar-3d-wrapper {
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
}

.avatar-3d-container {
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.3s ease;
}

.avatar-3d-container:hover {
  transform: scale(1.05);
}

.avatar-3d-container canvas {
  border-radius: 50%;
  display: block;
}

.avatar-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: white;
}

.loading-icon {
  font-size: 32px;
  animation: rotate 2s linear infinite;
}

.loading-text {
  font-size: 14px;
  opacity: 0.8;
}

.avatar-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  backdrop-filter: blur(10px);
}

.fallback-icon {
  font-size: 60px;
  color: white;
  opacity: 0.8;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .loading-icon {
    font-size: 24px;
  }
  
  .loading-text {
    font-size: 12px;
  }
  
  .fallback-icon {
    font-size: 45px;
  }
}
</style>
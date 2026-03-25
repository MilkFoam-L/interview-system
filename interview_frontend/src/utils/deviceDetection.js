/**
 * 设备检测工具函数
 * 用于检测摄像头、麦克风等设备状态
 */

// 检测摄像头是否可用
export async function detectCamera() {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ 
      video: true,
      audio: false 
    })
    
    // 检测成功，停止流
    stream.getTracks().forEach(track => track.stop())
    
    console.log('✅ 摄像头检测成功')
    return {
      available: true,
      message: '摄像头设备正常'
    }
  } catch (error) {
    console.error('❌ 摄像头检测失败:', error)
    
    let message = '摄像头不可用'
    
    if (error.name === 'NotAllowedError') {
      message = '摄像头权限被拒绝，请在浏览器设置中允许摄像头访问'
    } else if (error.name === 'NotFoundError') {
      message = '未检测到摄像头设备，请确认设备连接'
    } else if (error.name === 'NotReadableError') {
      message = '摄像头被其他程序占用，请关闭其他使用摄像头的应用'
    }
    
    return {
      available: false,
      message,
      error: error.name
    }
  }
}

// 检测麦克风是否可用
export async function detectMicrophone() {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ 
      video: false,
      audio: true 
    })
    
    // 检测成功，停止流
    stream.getTracks().forEach(track => track.stop())
    
    console.log('✅ 麦克风检测成功')
    return {
      available: true,
      message: '麦克风设备正常'
    }
  } catch (error) {
    console.error('❌ 麦克风检测失败:', error)
    
    let message = '麦克风不可用'
    
    if (error.name === 'NotAllowedError') {
      message = '麦克风权限被拒绝，请在浏览器设置中允许麦克风访问'
    } else if (error.name === 'NotFoundError') {
      message = '未检测到麦克风设备，请确认设备连接'
    } else if (error.name === 'NotReadableError') {
      message = '麦克风被其他程序占用，请关闭其他使用麦克风的应用'
    }
    
    return {
      available: false,
      message,
      error: error.name
    }
  }
}

// 检测网络连接状态
export function detectNetwork() {
  const isOnline = navigator.onLine
  
  console.log(`${isOnline ? '✅' : '❌'} 网络连接${isOnline ? '正常' : '异常'}`)
  
  return {
    available: isOnline,
    message: isOnline ? '网络连接正常' : '网络连接异常，请检查网络设置'
  }
}

// 获取媒体设备列表
export async function getMediaDevices() {
  try {
    // 首先请求权限
    await navigator.mediaDevices.getUserMedia({ video: true, audio: true })
    
    // 获取设备列表
    const devices = await navigator.mediaDevices.enumerateDevices()
    
    const cameras = devices.filter(device => device.kind === 'videoinput')
    const microphones = devices.filter(device => device.kind === 'audioinput')
    const speakers = devices.filter(device => device.kind === 'audiooutput')
    
    console.log('📱 检测到的设备:', {
      cameras: cameras.length,
      microphones: microphones.length,
      speakers: speakers.length
    })
    
    return {
      cameras,
      microphones,
      speakers,
      total: devices.length
    }
  } catch (error) {
    console.error('❌ 获取设备列表失败:', error)
    return {
      cameras: [],
      microphones: [],
      speakers: [],
      total: 0,
      error: error.message
    }
  }
}

// 检测浏览器支持情况
export function detectBrowserSupport() {
  const support = {
    getUserMedia: !!(navigator.mediaDevices && navigator.mediaDevices.getUserMedia),
    webRTC: !!(window.RTCPeerConnection || window.mozRTCPeerConnection || window.webkitRTCPeerConnection),
    webAudio: !!(window.AudioContext || window.webkitAudioContext),
    canvas: !!document.createElement('canvas').getContext,
    webGL: !!document.createElement('canvas').getContext('webgl')
  }
  
  const allSupported = Object.values(support).every(Boolean)
  
  console.log('🌐 浏览器支持情况:', support)
  
  return {
    support,
    allSupported,
    message: allSupported ? '浏览器支持所有必要功能' : '浏览器不支持某些功能，可能影响面试体验'
  }
}

// 综合设备检测
export async function detectAllDevices() {
  console.log('🔍 开始综合设备检测...')
  
  const results = {
    camera: await detectCamera(),
    microphone: await detectMicrophone(),
    network: detectNetwork(),
    browser: detectBrowserSupport()
  }
  
  try {
    results.devices = await getMediaDevices()
  } catch (error) {
    console.error('获取设备信息失败:', error)
    results.devices = { error: error.message }
  }
  
  const allReady = results.camera.available && 
                   results.microphone.available && 
                   results.network.available &&
                   results.browser.allSupported
  
  console.log('🔍 设备检测完成:', {
    camera: results.camera.available,
    microphone: results.microphone.available,
    network: results.network.available,
    browser: results.browser.allSupported,
    allReady
  })
  
  return {
    ...results,
    allReady,
    summary: allReady ? '所有设备检测通过，可以开始面试' : '部分设备检测失败，请检查后重试'
  }
}

// 监听网络状态变化
export function setupNetworkListener(onStatusChange) {
  const handleOnline = () => {
    console.log('✅ 网络已连接')
    onStatusChange({ online: true, message: '网络连接已恢复' })
  }
  
  const handleOffline = () => {
    console.log('❌ 网络已断开')
    onStatusChange({ online: false, message: '网络连接已断开' })
  }
  
  window.addEventListener('online', handleOnline)
  window.addEventListener('offline', handleOffline)
  
  // 返回清理函数
  return () => {
    window.removeEventListener('online', handleOnline)
    window.removeEventListener('offline', handleOffline)
  }
}

// 测试音频输入音量
export async function testMicrophoneVolume(duration = 3000) {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    const audioContext = new (window.AudioContext || window.webkitAudioContext)()
    const analyser = audioContext.createAnalyser()
    const microphone = audioContext.createMediaStreamSource(stream)
    const dataArray = new Uint8Array(analyser.frequencyBinCount)
    
    microphone.connect(analyser)
    analyser.fftSize = 256
    
    let maxVolume = 0
    let volumeReadings = []
    
    return new Promise((resolve) => {
      const startTime = Date.now()
      
      const checkVolume = () => {
        analyser.getByteFrequencyData(dataArray)
        const sum = dataArray.reduce((a, b) => a + b, 0)
        const average = sum / dataArray.length
        const volume = Math.round((average / 255) * 100)
        
        maxVolume = Math.max(maxVolume, volume)
        volumeReadings.push(volume)
        
        if (Date.now() - startTime < duration) {
          requestAnimationFrame(checkVolume)
        } else {
          // 停止音频流
          stream.getTracks().forEach(track => track.stop())
          audioContext.close()
          
          const avgVolume = Math.round(volumeReadings.reduce((a, b) => a + b, 0) / volumeReadings.length)
          
          resolve({
            maxVolume,
            avgVolume,
            readings: volumeReadings,
            duration,
            status: maxVolume > 10 ? 'good' : maxVolume > 5 ? 'low' : 'very_low'
          })
        }
      }
      
      checkVolume()
    })
  } catch (error) {
    console.error('麦克风音量测试失败:', error)
    throw error
  }
}
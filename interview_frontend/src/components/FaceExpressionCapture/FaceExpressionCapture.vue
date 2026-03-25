<template>
  <!-- 这是一个隐藏的组件，不会显示在UI中 -->
  <div class="face-expression-capture" style="display: none;"></div>
</template>

<script>
import { ref, onMounted, onUnmounted, watch } from 'vue';
import { analyzeFaceExpression } from '@/api/faceExpression';

export default {
  name: 'FaceExpressionCapture',
  props: {
    // 是否启用表情捕获
    enabled: {
      type: Boolean,
      default: false
    },
    // 视频元素引用
    videoRef: {
      type: Object,
      required: true
    },
    // 会话ID
    sessionId: {
      type: [Number, String],
      required: true
    },
    // 用户ID
    userId: {
      type: [Number, String],
      required: true
    },
    // 候选人姓名
    candidateName: {
      type: String,
      default: ''
    },
    // 候选人身份证号
    candidateIdNumber: {
      type: String,
      default: ''
    },
    // 剩余时间（秒）
    remainingTime: {
      type: Number,
      default: 0
    },
    // 捕获间隔（毫秒）
    captureInterval: {
      type: Number,
      default: 10000 // 10秒
    }
  },
  emits: ['capture-success', 'capture-error', 'capture-start', 'capture-stop'],
  setup(props, { emit }) {
    const isCapturing = ref(false);
    const captureTimer = ref(null);
    const captureCount = ref(0);

    // 开始捕获表情
    const startCapturing = () => {
      // 立即清除任何现有的定时器，避免重复定时器
      if (captureTimer.value) {
        clearInterval(captureTimer.value);
        captureTimer.value = null;
      }
      
      if (isCapturing.value || !props.enabled) {
        console.log('无法启动表情捕获:',
          isCapturing.value ? '已经在捕获中' : '捕获未启用',
          '启用状态:', props.enabled
        );
        return;
      }
      
      // 确保视频元素已就绪
      const videoElement = getVideoElement();
      if (!videoElement) {
        console.warn('无法获取视频元素，尝试延迟启动捕获');
        
        // 延迟三秒后再次尝试，给视频更多时间初始化
        setTimeout(() => {
          const retryVideo = getVideoElement();
          if (retryVideo) {
            console.log('延迟后成功获取视频元素，开始捕获');
            doStartCapturing(retryVideo);
          } else {
            console.error('即使在延迟后仍无法获取视频元素，尝试查找面试页面上的任何视频元素');
            
            // 最后的尝试 - 查找页面上的任何视频元素
            const anyVideoEl = document.querySelector('video');
            if (anyVideoEl) {
              console.log('找到页面上的视频元素，尝试使用它');
              doStartCapturing(anyVideoEl);
            } else {
              console.error('无法找到任何视频元素，取消表情捕获');
            }
          }
        }, 3000);
        
        return;
      }
      
      // 如果视频元素已就绪，开始捕获
      doStartCapturing(videoElement);
      
      // 实际启动捕获的内部函数
      function doStartCapturing(videoEl) {
        console.log(`开始表情捕获，使用视频元素:`, 
          '就绪状态:', videoEl.readyState,
          '尺寸:', `${videoEl.videoWidth}x${videoEl.videoHeight}`,
          '是否暂停:', videoEl.paused
        );
        
        // 发送开始捕获事件
        emit('capture-start');
        isCapturing.value = true;
        captureCount.value = 0;
        
        // 延迟3秒后开始第一次捕获，确保视频流已完全初始化
        setTimeout(() => {
          // 检查视频元素是否真正就绪
          if (!videoEl.videoWidth || !videoEl.videoHeight) {
            console.warn('视频尺寸仍为0，再等待2秒后尝试捕获');
            
            setTimeout(() => {
              console.log('延迟后再次尝试捕获，视频状态:', 
                '尺寸:', `${videoEl.videoWidth}x${videoEl.videoHeight}`);
              
              // 不论视频就绪与否，尝试执行一次捕获
              captureFace();
              
              // 设置周期性捕获定时器
              setupCaptureTimer();
            }, 2000);
          } else {
            // 视频已就绪，立即开始捕获
            captureFace();
            setupCaptureTimer();
          }
        }, 3000);
      }
      
      // 设置周期性捕获定时器的函数
      function setupCaptureTimer() {
        // 清除可能存在的定时器
        if (captureTimer.value) {
          clearInterval(captureTimer.value);
        }
        
        // 设置新定时器
        captureTimer.value = setInterval(() => {
          console.log('定时执行表情捕获，当前时间:', new Date().toLocaleTimeString(), 
            '启用状态:', props.enabled, 
            '捕获状态:', isCapturing.value);
            
          if (props.enabled && isCapturing.value) {
            captureFace();
          } else {
            console.log('定时器触发但跳过捕获');
          }
        }, props.captureInterval);
        
        console.log(`表情捕获定时器已设置，间隔: ${props.captureInterval}ms，ID:`, captureTimer.value);
      }
      
      console.log('面部表情捕获已开始初始化，间隔:', props.captureInterval, 'ms');
    };
    
    // 停止捕获表情
    const stopCapturing = () => {
      if (!isCapturing.value) return;
      
      // 清除定时器
      if (captureTimer.value) {
        clearInterval(captureTimer.value);
        captureTimer.value = null;
      }
      
      isCapturing.value = false;
      emit('capture-stop', { count: captureCount.value });
      console.log('面部表情捕获已停止，共捕获:', captureCount.value, '次');
    };
    
    // 获取视频元素的函数
    const getVideoElement = () => {
      // 首先检查props.videoRef是否可用
      if (props.videoRef && props.videoRef.value) {
        return props.videoRef.value;
      }
      
      // 如果不可用，尝试从DOM中获取
      try {
        // 查找面试页面上的视频元素
        const videoEl = document.getElementById('formalCameraRef');
        if (videoEl) {
          console.log('已通过DOM ID找到视频元素');
          return videoEl;
        }
        
        // 如果没找到特定ID，尝试查找任何视频元素
        const videoEls = document.querySelectorAll('video');
        if (videoEls && videoEls.length > 0) {
          console.log('通过DOM标签找到视频元素');
          return videoEls[0];
        }
      } catch (e) {
        console.error('尝试从DOM获取视频元素失败:', e);
      }
      
      return null;
    };
    
    // 捕获面部表情
    const captureFace = async () => {
      // 获取视频元素
      const videoElement = getVideoElement();
      
      console.log('尝试捕获表情 - 检查条件:', 
        '视频元素存在:', !!videoElement, 
        '正在捕获:', isCapturing.value, 
        '启用状态:', props.enabled
      );
      
      if (!videoElement) {
        console.error('捕获失败: 无法获取视频元素');
        return;
      }
      
      if (!isCapturing.value) {
        console.error('捕获失败: 捕获状态为false');
        return;
      }
      
      if (!props.enabled) {
        console.error('捕获失败: 组件未启用');
        return;
      }
      
      try {
        // 检查视频元素是否准备就绪
        console.log('视频元素状态:', 
          'videoWidth:', videoElement.videoWidth,
          'videoHeight:', videoElement.videoHeight,
          'readyState:', videoElement.readyState,
          'paused:', videoElement.paused
        );
        
        if (!videoElement.videoWidth || !videoElement.videoHeight) {
          console.warn('视频元素尚未准备好，宽度或高度为0');
          return;
        }
        
        console.log(`准备捕获表情图像，会话ID: ${props.sessionId}, 用户ID: ${props.userId}`);
        
        // 从视频捕获图像 - 确保图像质量和尺寸适合API要求
        const canvas = document.createElement('canvas');
        
        // 获取原始视频尺寸
        const videoWidth = videoElement.videoWidth;
        const videoHeight = videoElement.videoHeight;
        
        // 为了优化性能和减少数据量，根据需要缩放图像
        // 一般人脸识别API推荐图像中的人脸至少为40x40像素
        const maxWidth = 640;  // 最大宽度限制
        const maxHeight = 480; // 最大高度限制
        
        // 计算缩放后的尺寸，保持宽高比
        let scaledWidth = videoWidth;
        let scaledHeight = videoHeight;
        
        if (scaledWidth > maxWidth) {
          const ratio = maxWidth / scaledWidth;
          scaledWidth = maxWidth;
          scaledHeight = Math.round(scaledHeight * ratio);
        }
        
        if (scaledHeight > maxHeight) {
          const ratio = maxHeight / scaledHeight;
          scaledHeight = maxHeight;
          scaledWidth = Math.round(scaledWidth * ratio);
        }
        
        // 设置画布尺寸
        canvas.width = scaledWidth;
        canvas.height = scaledHeight;
        
        // 获取绘图上下文并设置图像平滑质量
        const ctx = canvas.getContext('2d');
        ctx.imageSmoothingEnabled = true;
        ctx.imageSmoothingQuality = 'high';
        
        // 绘制图像
        ctx.drawImage(videoElement, 0, 0, scaledWidth, scaledHeight);
        
        // 转换为高质量JPEG Base64
        const dataUrl = canvas.toDataURL('image/jpeg', 0.85); // 使用较高质量
        const imageBase64 = dataUrl.split(',')[1];
        
        console.log(`图像已捕获并转换为Base64，尺寸: ${scaledWidth}x${scaledHeight}, 长度: ${imageBase64.length}字节`);
        
        // 构建请求参数 - 确保包含必要字段，特别是添加timestamp和count
        const timestamp = new Date().getTime(); // 使用数字时间戳
        
        // 为每次请求生成唯一ID以避免数据库冲突
        // 生成或使用上次失败时创建的捕获ID
        const uniqueRequestId = lastCaptureId.value || `${props.sessionId}_${timestamp}_${captureCount.value + 1}`;
        // 使用后清空，避免重复使用
        if (lastCaptureId.value) {
          console.log('使用之前生成的捕获ID:', lastCaptureId.value);
          lastCaptureId.value = '';
        }
        
        const requestData = {
          sessionId: props.sessionId,
          userId: props.userId,
          capture_id: uniqueRequestId, // 只保留这个统一的字段名
          candidateName: props.candidateName || '候选人',
          candidateIdNumber: props.candidateIdNumber || '',
          imageBase64: imageBase64,
          remainingTime: props.remainingTime,
          timestamp: timestamp,
          count: captureCount.value + 1,
          imageWidth: scaledWidth,
          imageHeight: scaledHeight,
          capture_time: new Date().toISOString()
        };
        
        console.log('发送表情分析请求，参数:', JSON.stringify({
          sessionId: requestData.sessionId,
          userId: requestData.userId,
          candidateName: requestData.candidateName,
          candidateIdNumber: requestData.candidateIdNumber,
          remainingTime: requestData.remainingTime,
          capture_id: requestData.capture_id,
          count: requestData.count,
          imageBase64: '(已省略)'
        }));
        
        // 发送到后端进行分析
        const response = await analyzeFaceExpression(requestData);
        
        console.log('表情分析响应:', response);
        
        // 计数并发送成功事件
        captureCount.value++;
        emit('capture-success', { 
          count: captureCount.value, 
          response: response,
          expressionName: response.expressionName,
          expressionCode: response.expressionCode,
          imagePath: response.imagePath
        });
        
        console.log(`面部表情捕获成功 (#${captureCount.value}): ${response.expressionName}`);
      } catch (error) {
        handleCaptureError(error);
      }
    };

    // 重试计数和上一次的捕获ID
    const retryCount = ref(0);
    const lastCaptureId = ref('');
    
    // 处理捕获错误
    const handleCaptureError = (error) => {
      console.error('表情捕获失败:', error);
      
      // 检查是否有详细错误信息
      let errorMessage = '';
      if (error && error.response) {
        const responseData = error.response.data;
        console.error('错误响应:', responseData);
        
        if (typeof responseData === 'string') {
          errorMessage = responseData;
        } else if (responseData && responseData.message) {
          errorMessage = responseData.message;
        }
      }
      
      // 检查是否是数据库错误
      if (errorMessage.includes('Duplicate entry') || errorMessage.includes('error')) {
        
        // 如果是数据库错误，且重试次数小于3，则自动重试
        if (retryCount.value < 3) {
          retryCount.value++;
          console.log(`检测到数据库错误，尝试第${retryCount.value}次重试...`);
          
          // 延迟一秒后重试
          setTimeout(() => {
            console.log('生成新的捕获ID重试...');
            const newCaptureId = `retry_capture_${props.sessionId}_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`;
            
            console.log('使用新的捕获ID:', newCaptureId);
            
            // 记住这个新ID用于下次捕获
            lastCaptureId.value = newCaptureId;
            
            // 重新尝试捕获，使用相同的参数但新的捕获ID
            captureFace();
          }, 1000);
          
          return;
        }
      }
      
      // 达到最大重试次数或非数据库约束错误，通知错误
      retryCount.value = 0; // 重置重试计数
      emit('capture-error', { error });
      
      // 记录详细的错误信息便于调试
      if (error && error.response) {
        console.error('错误详情:', error.response.data || error.response);
      }
    };
    
    // 监听props变化
    watch(() => props.enabled, (newValue) => {
      console.log('表情捕获启用状态变化:', newValue);
      if (newValue && !isCapturing.value) {
        startCapturing();
      } else if (!newValue && isCapturing.value) {
        stopCapturing();
      }
    });

    // 组件挂载时处理
    onMounted(() => {
      console.log('表情捕获组件已挂载，启用状态:', props.enabled, '会话ID:', props.sessionId);
      
      // 确保在组件挂载后视频元素完全准备就绪
      if (props.enabled) {
        // 延迟启动，确保DOM和视频元素已完全初始化
        setTimeout(() => {
          const videoEl = getVideoElement();
          if (videoEl) {
            console.log('组件挂载后找到视频元素:', 
              '就绪状态:', videoEl.readyState,
              '尺寸:', `${videoEl.videoWidth}x${videoEl.videoHeight}`
            );
          } else {
            console.warn('组件挂载后未找到视频元素');
          }
          
          startCapturing();
        }, 2000);
      }
    });
    
    // 组件卸载时清理资源
    onUnmounted(() => {
      stopCapturing();
    });

    return {
      isCapturing,
      captureCount,
      startCapturing,
      stopCapturing,
      captureFace
    };
  }
};
</script> 
import request from '@/utils/request'

/**
 * 分析面部表情
 * 
 * @param {Object} data - 请求数据
 * @param {Number} data.sessionId - 会话ID
 * @param {Number} data.userId - 用户ID
 * @param {String} data.candidateName - 候选人姓名
 * @param {String} data.candidateIdNumber - 候选人身份证号
 * @param {String} data.imageBase64 - 图片Base64数据
 * @param {Number} data.remainingTime - 剩余时间（秒）
 * @returns {Promise} 表情分析结果
 */
export function analyzeFaceExpression(data) {
  // 生成请求的唯一标识符
  const uniqueId = data.capture_id || `capture_${data.sessionId}_${Date.now()}_${Math.random().toString(36).substring(2, 10)}`;
  
  console.log('调用analyzeFaceExpression API，参数:',
    '会话ID:', data.sessionId,
    '用户ID:', data.userId,
    '捕获ID:', uniqueId,
    '图像尺寸:', `${data.imageWidth || 'unknown'}x${data.imageHeight || 'unknown'}`,
    'Base64数据长度:', data.imageBase64 ? data.imageBase64.length : 0
  );
  
  // 添加调试日志到后端
  const requestWithLogs = {
    ...data,
    debug: true,
    clientInfo: {
      timestamp: new Date().toISOString(),
      browser: navigator.userAgent,
      videoAvailable: !!data.imageBase64,
      imageSize: data.imageBase64 ? data.imageBase64.length : 0,
      dimensions: `${data.imageWidth || 'unknown'}x${data.imageHeight || 'unknown'}`
    }
  };
  
  // 检查数据大小，防止请求过大
  if (data.imageBase64 && data.imageBase64.length > 100000) {
    console.warn('图像数据过大，可能导致API调用失败，长度:', data.imageBase64.length);
  }
  
  // 设置当前时间，以便各种时间字段保持一致
  const currentTime = new Date();
  const numericTimestamp = currentTime.getTime();
  const isoTimestamp = currentTime.toISOString();
  
  // 构建包含必要字段的请求
  const simplifiedRequest = {
    // 必需的会话信息
    sessionId: data.sessionId,
    userId: data.userId,
    // 必需的个人信息
    candidateName: data.candidateName || '测试用户',
    candidateIdNumber: data.candidateIdNumber || '0',
    // 图像数据
    imageBase64: data.imageBase64,
    // 时间信息
    timestamp: numericTimestamp,
    capture_time: isoTimestamp,
    // 唯一捕获ID - 统一使用capture_id字段
    capture_id: data.capture_id || uniqueId,
    // 剩余时间信息
    remainingTime: data.remainingTime || 0,
    // 计数
    count: data.count || data.retry || 1,
    // 调试标志
    debug: true
  };
  
  // 添加可选的图像信息
  if (data.imageWidth) simplifiedRequest.imageWidth = data.imageWidth;
  if (data.imageHeight) simplifiedRequest.imageHeight = data.imageHeight;
  
  // 打印请求信息（不包含图像数据）
  console.log('发送表情分析请求:', {
    ...simplifiedRequest,
    imageBase64: '(已省略)',
    时间戳: numericTimestamp,
    ISO时间: isoTimestamp,
    字段数量: Object.keys(simplifiedRequest).length
  });
  
  // 真实API调用
  return request({
    url: '/api/face-expression/analyze',
    method: 'post',
    data: simplifiedRequest,
    timeout: 30000, // 增加超时时间到30秒
    headers: {
      'Content-Type': 'application/json',
      'X-Image-Size': data.imageBase64 ? data.imageBase64.length.toString() : '0'
    }
  }).catch(error => {
    console.error('表情分析API调用失败:', error);
    
    // 尝试解析错误响应
    let errorMessage = error.message || '未知错误';
    if (error.response && error.response.data) {
      console.error('错误响应数据:', error.response.data);
      if (typeof error.response.data === 'string') {
        errorMessage = error.response.data;
      } else if (error.response.data.message) {
        errorMessage = error.response.data.message;
      } else if (error.response.data.error) {
        errorMessage = error.response.data.error;
      }
    }
    
         // 检查各种可能的错误情况
     // 1. capture_time错误
     if (errorMessage.includes('capture_time')) {
       console.log('检测到capture_time字段错误，尝试使用更简化的请求');
       
       // 重新尝试，使用带有capture_time的请求数据
       const currentTime = new Date();
       const retryId = `retry_${data.sessionId}_${currentTime.getTime()}_${Math.random().toString(36).substring(2, 10)}`;
       
       const minimalRequest = {
         sessionId: data.sessionId,
         userId: data.userId,
         imageBase64: data.imageBase64,
         timestamp: currentTime.getTime(),
         capture_time: currentTime.toISOString(),
         capture_id: retryId, // 添加新的捕获ID
         count: 1
       };
     } 
     // 2. 唯一约束错误 - 数据库已修改，不应再出现此类错误
     else if (errorMessage.includes('Duplicate entry')) {
       console.log('检测到数据库唯一约束冲突，使用新的捕获ID重试');
       
       // 生成新的捕获ID重试
       const retryId = `retry_${data.sessionId}_${Date.now()}_${Math.random().toString(36).substring(2, 15)}`;
       const currentTime = new Date();
       const retryRequest = {
         sessionId: data.sessionId,
         userId: data.userId,
         imageBase64: data.imageBase64,
         timestamp: currentTime.getTime(),
         capture_time: currentTime.toISOString(),
         capture_id: retryId, // 使用新的捕获ID
         count: 1
       };
       
       // 递归调用，使用简化请求
       return request({
         url: '/api/face-expression/analyze',
         method: 'post',
         data: minimalRequest,
         timeout: 30000,
         headers: {
           'Content-Type': 'application/json',
           'X-Image-Size': data.imageBase64 ? data.imageBase64.length.toString() : '0'
         }
       }).catch(innerError => {
         console.error('极简请求也失败了:', innerError);
         return {
           success: false,
           message: '表情分析失败: 多次尝试后仍然失败',
           expressionName: '未知',
           expressionCode: 0,
           error: innerError.message || '未知错误'
         };
       });
     }
           // 如果是任何类型的唯一约束冲突，尝试使用新的捕获ID重试
      else if (errorMessage.includes('Duplicate entry')) {
        // 使用新的捕获ID重试原请求
        const newCaptureId = `retry_${Date.now()}_${Math.random().toString(36).substring(2, 15)}`;
        console.log('尝试使用新的捕获ID重试请求:', newCaptureId);
        
        return analyzeFaceExpression({
          ...data,
          captureId: newCaptureId
        });
      }
    
    // 返回一个模拟响应，以便前端能继续工作
    return {
      success: false,
      message: '表情分析失败: ' + errorMessage,
      expressionName: '未知',
      expressionCode: 0,
      error: errorMessage,
      timestamp: new Date().toISOString()
    };
  });
}

/**
 * 获取表情分析摘要
 * 
 * @param {Number} sessionId - 会话ID
 * @returns {Promise} 表情分析摘要
 */
export function getExpressionSummary(sessionId) {
  console.log('获取表情分析摘要，会话ID:', sessionId);
  
  return request({
    url: `/api/face-expression/summary/${sessionId}`,
    method: 'get',
    params: {
      debug: true, // 指示后端记录详细日志
      timestamp: Date.now() // 防止缓存
    },
    timeout: 10000
  }).catch(error => {
    console.error('获取表情分析摘要失败:', error);
    // 返回模拟数据以便前端显示
    return {
      success: false,
      message: '获取表情分析失败: ' + (error.message || '未知错误'),
      dominantExpression: '未知',
      totalSamples: 0,
      expressionDistribution: { '未知': 0 },
      error: true
    };
  });
}

/**
 * 测试表情分析API可用性
 * 
 * @returns {Promise} 测试结果
 */
export function testFaceExpressionApi() {
  return request({
    url: '/api/face-expression/test',
    method: 'get'
  })
} 
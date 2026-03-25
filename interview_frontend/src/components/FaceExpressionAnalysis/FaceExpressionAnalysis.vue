<template>
  <div class="face-expression-analysis">
    <div class="analysis-content">
      <div class="analysis-header" v-if="$parent && $parent.$el && $parent.$el.className && !$parent.$el.className.includes('report-dashboard')">
        <h3>
          <el-icon><CircleCheckFilled /></el-icon>
          表情分析报告
        </h3>
        <el-button v-if="loading" :loading="true" type="info" text>加载中...</el-button>
        <el-button v-else-if="error" type="danger" @click="loadExpressionData" text>
          <el-icon><Refresh /></el-icon>
          重新加载
        </el-button>
        <el-button v-else type="primary" @click="loadExpressionData" text>
          <el-icon><Refresh /></el-icon>
          刷新数据
        </el-button>
      </div>
      <div class="refresh-btn-container" v-else>
        <el-button v-if="loading" :loading="true" type="info" size="small">加载中</el-button>
        <el-button v-else type="primary" size="small" @click="loadExpressionData">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>

      <!-- 加载中状态 -->
      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="6" animated />
      </div>

      <!-- 加载错误状态 -->
      <div v-else-if="error" class="error-container">
        <el-empty description="加载表情分析数据失败">
          <template #description>
            <p>{{ error }}</p>
          </template>
          <el-button type="primary" @click="loadExpressionData">重试</el-button>
        </el-empty>
      </div>

      <!-- 无数据状态 -->
      <div v-else-if="!expressionData || expressionData.totalSamples === 0" class="empty-container">
        <el-empty description="暂无表情分析数据">
          <template #description>
            <p>面试者可能尚未完成自我介绍环节</p>
          </template>
        </el-empty>
      </div>

      <!-- 有数据状态 -->
      <div v-else class="expression-content">
        <!-- 表情分析摘要 -->
        <div class="expression-summary">
          <div v-if="expressionReport" class="summary-report">
            {{ expressionReport }}
          </div>
          
          <div class="summary-stats">
            <div class="stat-item">
              <div class="stat-value">{{ expressionData.totalSamples }}</div>
              <div class="stat-label">表情样本数</div>
            </div>
            <div class="stat-item dominant">
              <div class="stat-value">{{ expressionData.dominantExpression }}</div>
              <div class="stat-label">主要表情</div>
            </div>
          </div>
        </div>

        <!-- 表情分布图 -->
        <div class="chart-container">
          <div ref="expressionChartRef" class="expression-chart"></div>
        </div>

        <!-- 表情样本图片 -->
        <div v-if="expressionData.sampleImagePaths && expressionData.sampleImagePaths.length > 0" class="sample-images">
          <h4>表情样本 ({{ Math.min(expressionData.sampleImagePaths.length, 4) }}张)</h4>
          <el-carousel 
            :autoplay="false" 
            indicator-position="outside" 
            height="200px"
            :initial-index="0"
            :interval="5000">
            <el-carousel-item v-for="(imagePath, index) in getSampleImagePaths()" :key="index">
              <div class="image-item">
                <img :src="getImageUrl(imagePath)" alt="表情样本" class="sample-image" />
                <div class="image-caption">样本 {{ index + 1 }}</div>
              </div>
            </el-carousel-item>
          </el-carousel>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, onUnmounted, computed, nextTick } from 'vue';
import * as echarts from 'echarts';
import { getExpressionSummary } from '@/api/faceExpression';
import { CircleCheckFilled, Refresh, Check } from '@element-plus/icons-vue';

export default {
  name: 'FaceExpressionAnalysis',
  components: {
    CircleCheckFilled,
    Refresh,
    Check
  },
  props: {
    sessionId: {
      type: [Number, String],
      required: true
    }
  },
  setup(props) {
    const loading = ref(false);
    const error = ref('');
    const expressionData = ref(null);
    const expressionChartRef = ref(null);
    const chartInstance = ref(null);

    // 生成表情分析报告
    const expressionReport = computed(() => {
      if (!expressionData.value || !expressionData.value.dominantExpression) return '';

      const dominant = expressionData.value.dominantExpression;
      const totalSamples = expressionData.value.totalSamples;
      
      if (dominant === '高兴') {
        return `面试者在自我介绍环节表现积极自信，共捕获${totalSamples}次表情，以微笑为主，给人留下积极向上的第一印象。`;
      } else if (dominant === '正常' || dominant === '中性') {
        return `面试者在自我介绍环节表现稳定自然，共捕获${totalSamples}次表情，保持了平和的表情状态，展现了良好的职业素养。`;
      } else if (dominant === '惊讶') {
        return `面试者在自我介绍环节表现出${totalSamples}次表情变化，以惊讶表情为主，可能对面试环节感到些许意外或紧张。`;
      } else if (dominant === '害怕' || dominant === '悲伤') {
        return `面试者在自我介绍环节显得略为紧张，共捕获${totalSamples}次表情，建议未来面试前做好充分的心理准备。`;
      } else {
        return `面试者在自我介绍环节表现出${totalSamples}次表情变化，主要表情为${dominant}，表情变化丰富多样。`;
      }
    });

    // 加载表情分析数据
    const loadExpressionData = async () => {
      if (loading.value) return;
      
      loading.value = true;
      error.value = '';
      
      try {
        const response = await getExpressionSummary(props.sessionId);
        
        if (response && response.success) {
          expressionData.value = {
            dominantExpression: response.dominantExpression || '中性',
            totalSamples: response.totalSamples || 0,
            expressionDistribution: response.expressionDistribution || {},
            sampleImagePaths: response.sampleImagePaths || []
          };
          
          // 初始化或更新图表
          nextTick(() => {
            initChart();
          });
        } else {
          error.value = response?.message || '获取表情分析数据失败';
        }
      } catch (err) {
        console.error('获取表情分析数据失败:', err);
        error.value = '获取表情分析数据失败，请稍后重试';
      } finally {
        loading.value = false;
      }
    };
    
    // 获取图片URL
    const getImageUrl = (path) => {
      if (path && path.startsWith('http')) {
        return path;
      } else if (path && (path.includes('interview_images') || path.includes('D:\\interview_images'))) {
        const normalizedPath = path.replace(/\\/g, '/');
        const imageName = normalizedPath.split('/').pop();
        
        const sessionUserIdPattern = /face_images2[\/\\](\d+)[\/\\](\d+)/;
        const match = normalizedPath.match(sessionUserIdPattern);
        
        if (match && match.length >= 3) {
          const sessionId = match[1];
          const userId = match[2];
          return `/api/images/face/${sessionId}/${userId}/${imageName}`;
        }
        
        return `/api/images/${normalizedPath}`;
      } else {
        return `/api/images/${path}`;
      }
    };
    
    // 获取样本图片路径（最多显示4张）
    const getSampleImagePaths = () => {
      if (!expressionData.value || !expressionData.value.sampleImagePaths) {
        return [];
      }
      return expressionData.value.sampleImagePaths.slice(0, 4);
    };

    // 初始化图表
    const initChart = () => {
      if (!expressionChartRef.value || !expressionData.value) return;
      
      // 销毁旧图表实例
      if (chartInstance.value) {
        chartInstance.value.dispose();
      }
      
      // 创建新图表实例
      chartInstance.value = echarts.init(expressionChartRef.value);
      
      // 准备数据
      const distribution = expressionData.value.expressionDistribution;
      const data = Object.entries(distribution)
        .filter(([_, value]) => value > 0) // 只显示有数据的表情
        .map(([name, value]) => ({ name, value }));
      
      // 如果没有数据，显示空图表
      if (data.length === 0) {
        data.push({ name: '暂无数据', value: 1 });
      }
      
      // 定义颜色映射
      const colorMap = {
        '高兴': '#67c23a',
        '微笑': '#85ce61',
        '正常': '#409EFF',
        '中性': '#79bbff',
        '严肃': '#E6A23C',
        '专注': '#5cb87a',
        '思考': '#909399',
        '惊讶': '#F56C6C',
        '紧张': '#f78989',
        '困惑': '#c45656',
        '悲伤': '#606266',
        '害怕': '#909399'
      };
      
      // 根据表情生成颜色列表
      const colors = data.map(item => colorMap[item.name] || '#909399');
      
      // 图表配置
      // 检查是否在报告页面中
  const isInReportPage = !!document.querySelector('.report-dashboard');
  
  const option = {
        title: {
          text: isInReportPage ? '' : '面试表情分布', // 在报告页面中不显示标题
          left: 'center',
          top: 0,
          textStyle: {
            fontSize: 16,
            fontWeight: 'normal'
          }
        },
        tooltip: {
          trigger: 'item',
          formatter: '{a} <br/>{b}: {c}次 ({d}%)'
        },
        legend: {
          orient: 'horizontal',
          bottom: '5%',
          left: 'center',
          itemWidth: isInReportPage ? 10 : 14,
          itemHeight: isInReportPage ? 10 : 14,
          textStyle: {
            fontSize: isInReportPage ? 10 : 12
          },
          data: data.map(item => item.name),
          // 在报告页面中减少图例项目数量
          formatter: isInReportPage ? (name) => {
            return name.length > 2 ? name.substring(0, 2) + '..' : name;
          } : null
        },
        series: [
          {
            name: '表情分布',
            type: 'pie',
            radius: isInReportPage ? ['30%', '70%'] : ['35%', '65%'], // 在报告页面中调整饼图大小
            center: ['50%', '45%'],
            avoidLabelOverlap: true,
            itemStyle: {
              borderRadius: 8,
              borderColor: '#fff',
              borderWidth: 2,
              shadowBlur: 10,
              shadowColor: 'rgba(0, 0, 0, 0.1)'
            },
            label: {
              show: !isInReportPage, // 在报告页面中不显示标签
              position: 'outside',
              formatter: '{b}: {c}次\n{d}%',
              fontSize: 12,
              fontWeight: 'bold'
            },
            labelLine: {
              length: 15,
              length2: 10,
              smooth: true
            },
            emphasis: {
              focus: 'series',
              scaleSize: 10,
              label: {
                show: true,
                fontSize: 16,
                fontWeight: 'bold'
              }
            },
            data: data,
            color: colors
          }
        ]
      };
      
      // 设置图表配置
      chartInstance.value.setOption(option);
      
      // 响应窗口大小变化
      window.addEventListener('resize', handleResize);
    };
    
    // 处理窗口大小变化
    const handleResize = () => {
      if (chartInstance.value) {
        chartInstance.value.resize();
      }
    };

          // 检查是否在报告页面中
      const isInReportPage = () => {
        try {
          const parent = document.querySelector('.report-dashboard');
          return !!parent;
        } catch (e) {
          return false;
        }
      };

      // 组件挂载时加载数据
      onMounted(() => {
        // 短暂延迟，确保DOM已经渲染
        setTimeout(() => {
          console.log('FaceExpressionAnalysis mounted, in report page:', isInReportPage());
          loadExpressionData();
        }, 100);
      });
    
    // 组件卸载时清理资源
    onUnmounted(() => {
      window.removeEventListener('resize', handleResize);
      if (chartInstance.value) {
        chartInstance.value.dispose();
        chartInstance.value = null;
      }
    });

    return {
      loading,
      error,
      expressionData,
      expressionChartRef,
      expressionReport,
      loadExpressionData,
      getImageUrl,
      getSampleImagePaths
    };
  }
};
</script>

<style scoped>
.face-expression-analysis {
  margin-bottom: 0;
  height: 100%;
}

.analysis-content {
  padding: 15px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.analysis-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.analysis-header h3 {
  margin: 0;
  font-size: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #303133;
}

.refresh-btn-container {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 10px;
}

.loading-container, .error-container, .empty-container {
  min-height: 300px;
  display: flex;
  justify-content: center;
  align-items: center;
  flex-direction: column;
}

.expression-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.expression-summary {
  display: flex;
  flex-direction: column;
  gap: 15px;
  margin-bottom: 10px;
}

.summary-report {
  padding: 12px 15px;
  background-color: #f0f9eb;
  border-radius: 8px;
  color: #67c23a;
  font-size: 14px;
  line-height: 1.6;
  border-left: 4px solid #67c23a;
}

.summary-stats {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 40px;
  margin-top: 5px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 20px;
  background-color: #f5f7fa;
  border-radius: 8px;
  transition: transform 0.3s ease;
}

.stat-item:hover {
  transform: translateY(-3px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.stat-item.dominant {
  background-color: #ecf5ff;
  border: 1px solid #d9ecff;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 4px;
}

.stat-item.dominant .stat-value {
  color: #409EFF;
}

.stat-label {
  font-size: 12px;
  color: #909399;
}

.chart-container {
  height: 320px;
  width: 100%;
  margin: 10px 0 20px;
  flex: 1; /* 允许图表容器伸展以填充可用空间 */
}

/* 在报告页面中的特殊样式 */
:deep(.report-dashboard) .chart-container {
  height: 220px; /* 在报告页面中减少图表高度 */
  margin: 0;
}

:deep(.report-dashboard) .summary-report {
  font-size: 12px;
  padding: 8px 12px;
}

:deep(.report-dashboard) .summary-stats {
  gap: 20px;
}

:deep(.report-dashboard) .stat-value {
  font-size: 18px;
}

:deep(.report-dashboard) .stat-item {
  padding: 5px 15px;
}

.expression-chart {
  height: 100%;
  width: 100%;
}

.sample-images {
  margin-top: 10px;
}

.sample-images h4 {
  margin-top: 0;
  margin-bottom: 10px;
  font-size: 14px;
  color: #606266;
  font-weight: 600;
  text-align: center;
}

.image-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
}

.sample-image {
  max-height: 160px;
  max-width: 100%;
  object-fit: contain;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.image-caption {
  margin-top: 8px;
  color: #909399;
  font-size: 12px;
}

@media (max-width: 768px) {
  .summary-stats {
    flex-direction: column;
    gap: 10px;
  }
  
  .stat-item {
    width: 100%;
  }
  
  .chart-container {
    height: 280px;
  }
}
</style> 
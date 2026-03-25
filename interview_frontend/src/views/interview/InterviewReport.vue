<template>
  <div class="interview-report-container">
    <el-card class="report-card" shadow="hover" v-loading="loading">
      <template #header>
        <div class="report-header">
          <h2>{{ reportTitle }}</h2>
          <div class="report-actions">
            <el-button type="primary" @click="completeInterview" :disabled="reportGenerated">
              {{ reportGenerated ? '报告已生成' : '生成报告' }}
            </el-button>
          </div>
        </div>
      </template>
      
      <div v-if="expressionData">
        <h3>表情分析结果</h3>
        <el-row :gutter="20">
          <el-col :span="12">
            <div ref="expressionChart" style="height: 300px; width: 100%;"></div>
          </el-col>
          <el-col :span="12">
            <div class="expression-summary">
              <p><strong>主要情绪:</strong> {{ expressionData.dominantExpression || '未知' }}</p>
              <p><strong>总表情采样数:</strong> {{ expressionData.totalCount || 0 }}</p>
              <p><strong>候选人姓名:</strong> {{ expressionData.candidateName || '未知' }}</p>
              
              <el-divider></el-divider>
              
              <h4>表情详细分布</h4>
              <el-table :data="expressionTableData" style="width: 100%" border>
                <el-table-column prop="expression" label="表情" width="120"></el-table-column>
                <el-table-column prop="count" label="次数"></el-table-column>
                <el-table-column prop="percentage" label="百分比"></el-table-column>
              </el-table>
            </div>
          </el-col>
        </el-row>
      </div>
      
      <div v-else class="no-data-message">
        <el-empty description="暂无表情分析数据，请完成面试后查看"></el-empty>
      </div>
    </el-card>
  </div>
</template>

<script>
import * as echarts from 'echarts';

export default {
  name: 'InterviewReport',
  props: {
    sessionId: {
      type: [Number, String],
      required: true
    }
  },
  data() {
    return {
      loading: false,
      reportTitle: '面试表情分析报告',
      expressionData: null,
      chart: null,
      reportGenerated: false
    };
  },
  computed: {
    expressionTableData() {
      if (!this.expressionData || !this.expressionData.expressionCounts) {
        return [];
      }
      
      const totalCount = this.expressionData.totalCount || 0;
      return Object.entries(this.expressionData.expressionCounts).map(([expression, count]) => {
        return {
          expression,
          count,
          percentage: totalCount > 0 ? `${((count / totalCount) * 100).toFixed(1)}%` : '0%'
        };
      }).sort((a, b) => b.count - a.count);
    }
  },
  mounted() {
    this.fetchExpressionData();
  },
  methods: {
    async fetchExpressionData() {
      try {
        this.loading = true;
        // 移除API调用，替换为静态数据
        this.expressionData = {
          dominantExpression: '无数据',
          totalCount: 0,
          expressionCounts: {
            '功能已移除': 1
          },
          candidateName: '用户'
        };
        this.loading = false;
        
        // 检查是否已经生成报告
        this.checkReportStatus();
        
        this.$nextTick(() => {
          this.initChart();
        });
      } catch (error) {
        console.error('表情分析功能已被移除');
        this.loading = false;
      }
    },
    
    initChart() {
      if (!this.expressionData || !this.expressionData.expressionCounts) {
        return;
      }
      
      if (this.chart) {
        this.chart.dispose();
      }
      
      const chartDom = this.$refs.expressionChart;
      this.chart = echarts.init(chartDom);
      
      const options = {
        title: {
          text: '表情分布饼图',
          left: 'center'
        },
        tooltip: {
          trigger: 'item',
          formatter: '{a} <br/>{b} : {c} ({d}%)'
        },
        legend: {
          orient: 'vertical',
          left: 'left',
          data: Object.keys(this.expressionData.expressionCounts)
        },
        series: [
          {
            name: '表情分布',
            type: 'pie',
            radius: '55%',
            center: ['50%', '60%'],
            data: Object.entries(this.expressionData.expressionCounts).map(([name, value]) => {
              return { name, value };
            }),
            emphasis: {
              itemStyle: {
                shadowBlur: 10,
                shadowOffsetX: 0,
                shadowColor: 'rgba(0, 0, 0, 0.5)'
              }
            }
          }
        ]
      };
      
      this.chart.setOption(options);
    },
    
    // 检查是否已经生成报告
    async checkReportStatus() {
      try {
        await this.$axios.get(`/api/reports/session/${this.sessionId}`);
        this.reportGenerated = true;
      } catch (error) {
        // 如果报告不存在，则设置为false
        this.reportGenerated = false;
      }
    },
    
    // 完成面试并生成报告
    async completeInterview() {
      try {
        this.loading = true;
        this.$message.info('正在生成面试报告，请稍候...');
        
        // 先调用表情分析汇总接口
        await this.$axios.post(`/api/expressions/sessions/${this.sessionId}/finalize`);
        
        // 然后生成报告
        const response = await this.$axios.post('/api/reports/generate', {
          sessionId: this.sessionId
        });
        
        this.loading = false;
        this.reportGenerated = true;
        this.$message.success('面试报告生成成功！');
        
        // 跳转到报告页面
        this.$router.push(`/report/view/${response.data.id}`);
      } catch (error) {
        this.loading = false;
        console.error('生成报告失败:', error);
        this.$message.error(`生成报告失败: ${error.message}`);
      }
    }
  }
}
</script>

<style scoped>
.interview-report-container {
  padding: 20px;
}

.report-card {
  margin-bottom: 20px;
}

.report-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.report-actions {
  display: flex;
  gap: 10px;
}

.expression-summary {
  padding: 10px;
}

.no-data-message {
  padding: 40px 0;
  text-align: center;
}
</style> 
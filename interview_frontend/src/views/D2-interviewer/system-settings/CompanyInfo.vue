<template>
  <div class="company-info-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <div>
          <h1>企业信息中心</h1>
          <p>完善公司信息，打造专业的企业形象和招聘门户</p>
        </div>
        <div class="header-actions">
          <el-button type="primary" :loading="saving" @click="saveCompanyInfo">
            <el-icon><Document /></el-icon>
            保存信息
          </el-button>
          <el-button v-if="companyInfo.id" @click="previewCompany">
            <el-icon><View /></el-icon>
            预览门户
          </el-button>
        </div>
      </div>
    </div>
    
    <!-- 主要内容区域 -->
    <div class="main-content">
      <!-- 左侧表单区域 -->
      <div class="form-section">
        <el-form
          ref="companyFormRef"
          :model="companyInfo"
          :rules="formRules"
          label-width="120px"
          label-position="left"
          size="large"
        >
          <!-- 基本信息模块 -->
          <el-card class="info-card">
            <template #header>
              <div class="card-header">
                <el-icon class="header-icon"><OfficeBuilding /></el-icon>
                <span class="header-title">基本信息</span>
              </div>
            </template>
            
            <el-row :gutter="24">
              <el-col :span="12">
                <el-form-item label="公司名称" prop="companyName" required>
                  <el-input
                    v-model="companyInfo.companyName"
                    placeholder="请输入公司全称"
                    clearable
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="法定名称" prop="legalName">
                  <el-input
                    v-model="companyInfo.legalName"
                    placeholder="请输入法定名称"
                    clearable
                  />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="24">
              <el-col :span="12">
                <el-form-item label="所属行业" prop="industry" required>
                  <el-select v-model="companyInfo.industry" placeholder="选择行业" style="width: 100%">
                    <el-option label="互联网" value="互联网" />
                    <el-option label="人工智能" value="人工智能" />
                    <el-option label="大数据" value="大数据" />
                    <el-option label="物联网" value="物联网" />
                    <el-option label="智能系统" value="智能系统" />
                    <el-option label="金融科技" value="金融科技" />
                    <el-option label="教育培训" value="教育培训" />
                    <el-option label="医疗健康" value="医疗健康" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="公司类型" prop="companyType">
                  <el-select v-model="companyInfo.companyType" placeholder="选择类型" style="width: 100%">
                    <el-option label="民营企业" value="民营" />
                    <el-option label="国有企业" value="国企" />
                    <el-option label="外资企业" value="外资" />
                    <el-option label="合资企业" value="合资" />
                    <el-option label="事业单位" value="事业单位" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="24">
              <el-col :span="12">
                <el-form-item label="公司规模" prop="companySize">
                  <el-select v-model="companyInfo.companySize" placeholder="选择规模" style="width: 100%">
                    <el-option label="1-20人" value="1-20人" />
                    <el-option label="21-99人" value="21-99人" />
                    <el-option label="100-499人" value="100-499人" />
                    <el-option label="500-999人" value="500-999人" />
                    <el-option label="1000-9999人" value="1000-9999人" />
                    <el-option label="10000人以上" value="10000人以上" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="融资阶段" prop="financingStage">
                  <el-select v-model="companyInfo.financingStage" placeholder="选择融资阶段" style="width: 100%">
                    <el-option label="未融资" value="未融资" />
                    <el-option label="天使轮" value="天使轮" />
                    <el-option label="A轮" value="A轮" />
                    <el-option label="B轮" value="B轮" />
                    <el-option label="C轮" value="C轮" />
                    <el-option label="已上市" value="上市公司" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="24">
              <el-col :span="12">
                <el-form-item label="成立时间" prop="establishedDate">
                  <el-date-picker
                    v-model="companyInfo.establishedDate"
                    type="date"
                    placeholder="选择成立日期"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="上市状态" prop="listedStatus">
                  <el-switch
                    v-model="companyInfo.listedStatus"
                    active-text="已上市"
                    inactive-text="未上市"
                    inline-prompt
                  />
                  <el-input
                    v-if="companyInfo.listedStatus"
                    v-model="companyInfo.stockCode"
                    placeholder="股票代码"
                    style="width: 150px; margin-left: 12px"
                  />
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item label="统一社会信用代码" prop="registrationNumber">
              <el-input
                v-model="companyInfo.registrationNumber"
                placeholder="请输入统一社会信用代码"
                maxlength="18"
                clearable
              />
            </el-form-item>
          </el-card>

          <!-- 联系信息模块 -->
          <el-card class="info-card">
            <template #header>
              <div class="card-header">
                <el-icon class="header-icon"><Phone /></el-icon>
                <span class="header-title">联系信息</span>
              </div>
            </template>
            
            <el-form-item label="详细地址" prop="detailedAddress">
              <el-input
                v-model="companyInfo.detailedAddress"
                placeholder="请输入公司详细地址"
                clearable
              />
            </el-form-item>

            <el-row :gutter="24">
              <el-col :span="12">
                <el-form-item label="联系邮箱" prop="contactEmail">
                  <el-input
                    v-model="companyInfo.contactEmail"
                    placeholder="company@example.com"
                    clearable
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="联系电话" prop="contactPhone">
                  <el-input
                    v-model="companyInfo.contactPhone"
                    placeholder="请输入联系电话"
                    clearable
                  />
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item label="官方网站" prop="websiteUrl">
              <el-input
                v-model="companyInfo.websiteUrl"
                placeholder="https://www.company.com"
                clearable
              >
                <template #prepend>
                  <el-icon><Link /></el-icon>
                </template>
              </el-input>
            </el-form-item>
          </el-card>

          <!-- 品牌形象模块 -->
          <el-card class="info-card">
            <template #header>
              <div class="card-header">
                <el-icon class="header-icon"><PictureRounded /></el-icon>
                <span class="header-title">品牌形象</span>
              </div>
            </template>
            
            <el-form-item label="公司标语" prop="companySlogan">
              <el-input
                v-model="companyInfo.companySlogan"
                placeholder="请输入公司标语或理念"
                clearable
              />
            </el-form-item>

            <el-form-item label="公司简介" prop="companyDescription">
              <el-input
                v-model="companyInfo.companyDescription"
                type="textarea"
                :rows="4"
                placeholder="请介绍公司的主营业务、发展历程、企业愿景等"
                maxlength="1000"
                show-word-limit
              />
            </el-form-item>

            <el-form-item label="企业文化" prop="companyCulture">
              <el-input
                v-model="companyInfo.companyCulture"
                type="textarea"
                :rows="4"
                placeholder="请描述公司的价值观、工作氛围、团队文化等"
                maxlength="1000"
                show-word-limit
              />
            </el-form-item>

            <el-form-item label="产品服务" prop="productDescription">
              <el-input
                v-model="companyInfo.productDescription"
                type="textarea"
                :rows="4"
                placeholder="请介绍公司的主要产品或服务"
                maxlength="1000"
                show-word-limit
              />
            </el-form-item>
          </el-card>

          <!-- 福利待遇模块 -->
          <el-card class="info-card">
            <template #header>
              <div class="card-header">
                <el-icon class="header-icon"><Present /></el-icon>
                <span class="header-title">福利待遇</span>
              </div>
            </template>
            
            <el-form-item label="工作时间" prop="workTime">
              <el-radio-group v-model="companyInfo.workTime">
                <el-radio label="标准工时">标准工时（8小时/天）</el-radio>
                <el-radio label="弹性工作">弹性工作制</el-radio>
                <el-radio label="混合工作">混合办公模式</el-radio>
                <el-radio label="远程工作">支持远程工作</el-radio>
              </el-radio-group>
            </el-form-item>

            <el-form-item label="员工福利">
              <div class="benefits-container">
                <el-checkbox-group v-model="selectedBenefits" class="benefits-grid">
                  <el-checkbox label="五险一金">五险一金</el-checkbox>
                  <el-checkbox label="补充医疗">补充医疗保险</el-checkbox>
                  <el-checkbox label="年终奖金">年终奖金</el-checkbox>
                  <el-checkbox label="股票期权">股票期权</el-checkbox>
                  <el-checkbox label="带薪年假">带薪年假</el-checkbox>
                  <el-checkbox label="免费三餐">免费三餐</el-checkbox>
                  <el-checkbox label="健身房">公司健身房</el-checkbox>
                  <el-checkbox label="员工旅游">员工旅游</el-checkbox>
                  <el-checkbox label="培训机会">培训学习机会</el-checkbox>
                  <el-checkbox label="住房补贴">住房补贴</el-checkbox>
                  <el-checkbox label="交通补贴">交通补贴</el-checkbox>
                  <el-checkbox label="节日福利">节日福利</el-checkbox>
                </el-checkbox-group>
              </div>
            </el-form-item>
          </el-card>
        </el-form>
      </div>

      <!-- 右侧预览区域 -->
      <div class="preview-section">
        <!-- Logo上传卡片 -->
        <el-card class="logo-card">
          <template #header>
            <div class="card-header">
              <span class="header-title">公司Logo</span>
            </div>
          </template>
          
          <div class="logo-upload-container">
            <div class="logo-uploader" @click="triggerLogoUpload">
              <img v-if="companyInfo.logoUrl" :src="companyInfo.logoUrl" class="logo-image" />
              <el-icon v-else class="logo-uploader-icon"><Plus /></el-icon>
            </div>
            <input
              ref="logoInputRef"
              type="file"
              accept="image/jpeg,image/png"
              style="display: none"
              @change="handleLogoUpload"
            />
            <div class="logo-tips">
              <p>建议尺寸：200×200像素</p>
              <p>支持格式：JPG、PNG</p>
              <p>文件大小：不超过2MB</p>
            </div>
          </div>
        </el-card>

        <!-- 认证状态卡片 -->
        <el-card class="status-card">
          <template #header>
            <div class="card-header">
              <span class="header-title">认证状态</span>
            </div>
          </template>
          
          <div class="status-content">
            <div class="status-item">
              <el-icon :class="['status-icon', companyInfo.isVerified ? 'verified' : 'pending']">
                <CircleCheck v-if="companyInfo.isVerified" />
                <Clock v-else />
              </el-icon>
              <div class="status-info">
                <div class="status-title">企业认证</div>
                <div class="status-desc">
                  {{ companyInfo.isVerified ? '已认证' : '待认证' }}
                </div>
              </div>
            </div>
            
            <el-divider />
            
            <div class="license-upload">
              <el-button type="primary" size="small" @click="triggerLicenseUpload">
                <el-icon><Upload /></el-icon>
                上传营业执照
              </el-button>
              <input
                ref="licenseInputRef"
                type="file"
                accept="image/jpeg,image/png"
                style="display: none"
                @change="handleLicenseUpload"
              />
              <div class="license-tips">
                <p>请上传清晰的营业执照照片</p>
                <p v-if="companyInfo.businessLicenseUrl" class="license-status">
                  ✅ 已上传
                </p>
              </div>
            </div>
          </div>
        </el-card>

        <!-- 数据统计卡片 -->
        <el-card class="stats-card">
          <template #header>
            <div class="card-header">
              <span class="header-title">信息完善度</span>
            </div>
          </template>
          
          <div class="stats-content">
            <div class="progress-circle">
              <el-progress
                type="circle"
                :percentage="completionRate"
                :width="120"
                :color="progressColors"
              >
                <template #default="{ percentage }">
                  <span class="percentage-text">{{ percentage }}%</span>
                </template>
              </el-progress>
    </div>
    
            <div class="stats-items">
              <div class="stats-item">
                <span class="stats-label">基本信息</span>
                <span class="stats-value">{{ basicInfoCount }}/6</span>
              </div>
              <div class="stats-item">
                <span class="stats-label">联系信息</span>
                <span class="stats-value">{{ contactInfoCount }}/4</span>
              </div>
              <div class="stats-item">
                <span class="stats-label">品牌形象</span>
                <span class="stats-value">{{ brandInfoCount }}/4</span>
              </div>
              <div class="stats-item">
                <span class="stats-label">福利待遇</span>
                <span class="stats-value">{{ benefitsCount }}/2</span>
              </div>
          </div>
        </div>
      </el-card>
    </div>
    </div>

    <!-- 预览对话框 -->
    <el-dialog
      v-model="previewVisible"
      title="企业门户预览"
      width="80%"
      :before-close="handlePreviewClose"
    >
      <div class="preview-content">
        <div class="company-portal">
          <div class="portal-header">
            <div class="portal-logo">
              <img v-if="companyInfo.logoUrl" :src="companyInfo.logoUrl" alt="公司Logo" />
              <div v-else class="portal-logo-placeholder">
                <el-icon><OfficeBuilding /></el-icon>
              </div>
            </div>
            <div class="portal-info">
              <h2 class="portal-title">{{ companyInfo.companyName || '公司名称' }}</h2>
              <p class="portal-slogan">{{ companyInfo.companySlogan || '公司标语' }}</p>
              <div class="portal-tags">
                <el-tag v-if="companyInfo.industry" type="primary">{{ companyInfo.industry }}</el-tag>
                <el-tag v-if="companyInfo.companySize">{{ companyInfo.companySize }}</el-tag>
                <el-tag v-if="companyInfo.financingStage" type="success">{{ companyInfo.financingStage }}</el-tag>
              </div>
            </div>
          </div>
          
          <div class="portal-content">
            <div class="portal-section">
              <h3>公司简介</h3>
              <p>{{ companyInfo.companyDescription || '暂无介绍' }}</p>
            </div>
            
            <div class="portal-section">
              <h3>企业文化</h3>
              <p>{{ companyInfo.companyCulture || '暂无介绍' }}</p>
            </div>
            
            <div class="portal-section">
              <h3>福利待遇</h3>
              <div class="benefits-preview">
                <el-tag
                  v-for="benefit in selectedBenefits"
                  :key="benefit"
                  class="benefit-tag"
                  type="info"
                >
                  {{ benefit }}
                </el-tag>
                <span v-if="selectedBenefits.length === 0" class="no-benefits">暂无福利信息</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Document,
  View,
  OfficeBuilding,
  Phone,
  PictureRounded,
  Present,
  Plus,
  Link,
  Upload,
  CircleCheck,
  Clock
} from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getMyCompanyInfo,
  createCompanyInfo,
  updateCompanyInfo,
  uploadCompanyLogo,
  uploadBusinessLicense
} from '@/api/companyInfo'

type CompanyInfoApiPayload = {
  id?: number
  companyName?: string
  legalName?: string
  registrationNumber?: string
  industry?: string
  companyType?: string
  establishedDate?: string | number | Date | null
  companySize?: string
  financingStage?: string
  listedStatus?: boolean
  stockCode?: string
  detailedAddress?: string
  contactEmail?: string
  contactPhone?: string
  websiteUrl?: string
  logoUrl?: string
  companySlogan?: string
  companyDescription?: string
  companyCulture?: string
  productDescription?: string
  workTime?: string
  isVerified?: boolean
  businessLicenseUrl?: string
  benefits?: Record<string, string>
  welfare?: string[]
  name?: string
  type?: string
  size?: string
  financing?: string
  location?: string
  email?: string
  phone?: string
  slogan?: string
  description?: string
  culture?: string
}

const companyFormRef = ref<FormInstance>()
const logoInputRef = ref<HTMLInputElement>()
const licenseInputRef = ref<HTMLInputElement>()
const saving = ref(false)
const previewVisible = ref(false)
const selectedBenefits = ref<string[]>([])

const companyInfo = reactive({
  id: null as number | null,
  companyName: '',
  legalName: '',
  registrationNumber: '',
  industry: '',
  companyType: '',
  establishedDate: null as Date | null,
  companySize: '',
  financingStage: '',
  listedStatus: false,
  stockCode: '',
  detailedAddress: '',
  contactEmail: '',
  contactPhone: '',
  websiteUrl: '',
  logoUrl: '',
  companySlogan: '',
  companyDescription: '',
  companyCulture: '',
  productDescription: '',
  workTime: '标准工时',
  isVerified: false,
  businessLicenseUrl: '',
  benefits: {} as Record<string, string>
})

const formRules = reactive<FormRules>({
  companyName: [
    { required: true, message: '请输入公司名称', trigger: 'blur' }
  ],
  industry: [
    { required: true, message: '请选择所属行业', trigger: 'change' }
  ],
  contactEmail: [
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  websiteUrl: [
    { type: 'url', message: '请输入正确的网址格式', trigger: 'blur' }
  ],
  contactPhone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ]
})

const progressColors = [
  { color: '#AFC7D9', percentage: 100 },
  { color: '#B8D6D0', percentage: 80 },
  { color: '#F1C9BB', percentage: 60 },
  { color: '#D9E1E8', percentage: 40 }
]

const basicInfoCount = computed(() => {
  let count = 0
  if (companyInfo.companyName) count++
  if (companyInfo.industry) count++
  if (companyInfo.companyType) count++
  if (companyInfo.companySize) count++
  if (companyInfo.financingStage) count++
  if (companyInfo.establishedDate) count++
  return count
})

const contactInfoCount = computed(() => {
  let count = 0
  if (companyInfo.detailedAddress) count++
  if (companyInfo.contactEmail) count++
  if (companyInfo.contactPhone) count++
  if (companyInfo.websiteUrl) count++
  return count
})

const brandInfoCount = computed(() => {
  let count = 0
  if (companyInfo.companySlogan) count++
  if (companyInfo.companyDescription) count++
  if (companyInfo.companyCulture) count++
  if (companyInfo.logoUrl) count++
  return count
})

const benefitsCount = computed(() => {
  let count = 0
  if (companyInfo.workTime) count++
  if (selectedBenefits.value.length > 0) count++
  return count
})

const completionRate = computed(() => {
  const total = 16 // 总字段数
  const completed = basicInfoCount.value + contactInfoCount.value + brandInfoCount.value + benefitsCount.value
  return Math.round((completed / total) * 100)
})

// 生命周期
onMounted(() => {
  loadCompanyInfo()
})

watch(selectedBenefits, (newBenefits) => {
  const benefitsObj: Record<string, string> = {}
  newBenefits.forEach(benefit => {
    benefitsObj[benefit] = '有'
  })
  companyInfo.benefits = benefitsObj
}, { deep: true })

watch(() => companyInfo.contactPhone, (newValue, oldValue) => {
  console.log('contactPhone变化:', { 
    oldValue, 
    newValue, 
    type: typeof newValue,
    isArray: Array.isArray(newValue)
  })
  if (Array.isArray(newValue)) {
    console.error('检测到contactPhone被错误赋值为数组，将重置为空字符串')
    companyInfo.contactPhone = ''
  }
})

// 方法
const loadCompanyInfo = async () => {
  try {
    const apiRes: any = await getMyCompanyInfo()
    const data = (apiRes && apiRes.data !== undefined ? apiRes.data : apiRes) as Partial<CompanyInfoApiPayload> & Record<string, any>
    if (data) {
      const safeStringValue = (value: any, defaultValue: string = ''): string => {
        if (typeof value === 'string') return value
        if (value === null || value === undefined) return defaultValue
        if (Array.isArray(value)) {
          console.error('加载数据时发现数组类型字段，使用默认值:', value)
          return defaultValue
        }
        if (typeof value === 'object') {
          console.warn('加载数据时发现对象类型字段，使用默认值:', value)
          return defaultValue
        }
        return String(value)
      }
      
      Object.assign(companyInfo, {
        id: data.id ?? null,
        companyName: safeStringValue(data.companyName || data.name),
        legalName: safeStringValue(data.legalName),
        registrationNumber: safeStringValue(data.registrationNumber),
        industry: safeStringValue(data.industry),
        companyType: safeStringValue(data.companyType || data.type),
        establishedDate: data.establishedDate ? new Date(data.establishedDate) : null,
        companySize: safeStringValue(data.companySize || data.size),
        financingStage: safeStringValue(data.financingStage || data.financing),
        listedStatus: Boolean(data.listedStatus),
        stockCode: safeStringValue(data.stockCode),
        detailedAddress: safeStringValue(data.detailedAddress || data.location),
        contactEmail: safeStringValue(data.contactEmail || data.email),
        contactPhone: safeStringValue(data.contactPhone || data.phone),
        websiteUrl: safeStringValue(data.websiteUrl),
        logoUrl: safeStringValue(data.logoUrl),
        companySlogan: safeStringValue(data.companySlogan || data.slogan),
        companyDescription: safeStringValue(data.companyDescription || data.description),
        companyCulture: safeStringValue(data.companyCulture || data.culture),
        productDescription: safeStringValue(data.productDescription),
        workTime: safeStringValue(data.workTime, '标准工时'),
        isVerified: Boolean(data.isVerified),
        businessLicenseUrl: safeStringValue(data.businessLicenseUrl)
      })
      
      console.log('数据加载后的companyInfo:', JSON.stringify(companyInfo, null, 2))

      if (data.benefits && typeof data.benefits === 'object') {
        selectedBenefits.value = Object.keys(data.benefits)
      } else if (data.welfare && Array.isArray(data.welfare)) {
        selectedBenefits.value = data.welfare
      }
    }
  } catch (error: any) {
    if (error.response?.status === 404) {
      console.log('用户尚未创建公司信息')
    } else {
      console.error('加载公司信息失败:', error)
      console.error('加载公司信息失败')
    }
  }
}

const saveCompanyInfo = async () => {
  if (!companyFormRef.value) return
  
  try {
    console.log('保存前强制检查所有字段类型...')
    const fieldsToCheck = [
      'companyName', 'legalName', 'registrationNumber', 'industry', 'companyType',
      'companySize', 'financingStage', 'stockCode', 'detailedAddress', 'contactEmail',
      'contactPhone', 'websiteUrl', 'logoUrl', 'companySlogan', 'companyDescription',
      'companyCulture', 'productDescription', 'workTime', 'businessLicenseUrl'
    ]
    
    fieldsToCheck.forEach(field => {
      const value = companyInfo[field as keyof typeof companyInfo]
      if (Array.isArray(value)) {
        console.error(`强制修复字段 ${field}:`, value)
        ;(companyInfo as any)[field] = ''
      }
    })
    
    await companyFormRef.value.validate()
    
    saving.value = true

    console.log('保存前的companyInfo数据:', JSON.stringify(companyInfo, null, 2))

    const cleanStringField = (value: any): string => {
      if (typeof value === 'string') return value
      if (value === null || value === undefined) return ''
      if (Array.isArray(value)) {
        console.error('发现数组类型字段（可能是表单验证规则），将转换为空字符串:', value)
        return ''
      }
      if (typeof value === 'object') {
        console.warn('发现对象类型字段，将转换为空字符串:', value)
        return ''
      }
      return String(value)
    }
    
    const saveData = {
      companyName: cleanStringField(companyInfo.companyName),
      legalName: cleanStringField(companyInfo.legalName),
      registrationNumber: cleanStringField(companyInfo.registrationNumber),
      industry: cleanStringField(companyInfo.industry),
      companyType: cleanStringField(companyInfo.companyType),
      establishedDate: companyInfo.establishedDate,
      companySize: cleanStringField(companyInfo.companySize),
      financingStage: cleanStringField(companyInfo.financingStage),
      listedStatus: Boolean(companyInfo.listedStatus),
      stockCode: cleanStringField(companyInfo.stockCode),
      detailedAddress: cleanStringField(companyInfo.detailedAddress),
      contactEmail: cleanStringField(companyInfo.contactEmail),
      contactPhone: cleanStringField(companyInfo.contactPhone),
      websiteUrl: cleanStringField(companyInfo.websiteUrl),
      logoUrl: cleanStringField(companyInfo.logoUrl),
      companySlogan: cleanStringField(companyInfo.companySlogan),
      companyDescription: cleanStringField(companyInfo.companyDescription),
      companyCulture: cleanStringField(companyInfo.companyCulture),
      productDescription: cleanStringField(companyInfo.productDescription),
      workTime: cleanStringField(companyInfo.workTime),
      benefits: JSON.stringify(companyInfo.benefits || {}),
      businessLicenseUrl: cleanStringField(companyInfo.businessLicenseUrl)
    }
    
    console.log('准备发送的saveData:', JSON.stringify(saveData, null, 2))
    
    let response
    if (companyInfo.id) {
      response = await updateCompanyInfo(companyInfo.id, saveData)
    } else {
      response = await createCompanyInfo(saveData)
    }
    
    if (response?.data) {
      if (response.data.id) {
        companyInfo.id = response.data.id
      }
      console.log('公司信息保存成功！')
    } else {
      throw new Error('保存失败')
    }
  } catch (error) {
    console.error('保存失败:', error)
    if (error.message) {
      console.error(`保存失败：${error.message}`)
    } else {
      console.error('保存失败，请检查表单信息')
    }
  } finally {
    saving.value = false
  }
}

const previewCompany = () => {
  previewVisible.value = true
}

const handlePreviewClose = () => {
  previewVisible.value = false
}

// 文件上传处理
const triggerLogoUpload = () => {
  logoInputRef.value?.click()
}

const handleLogoUpload = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  
  if (!file) return
  
  if (file.type !== 'image/jpeg' && file.type !== 'image/png') {
    console.error('Logo必须是JPG或PNG格式!')
    return
  }
  
  if (file.size > 2 * 1024 * 1024) {
    console.error('Logo大小不能超过2MB!')
    return
  }

  const reader = new FileReader()
  reader.onload = (e) => {
    companyInfo.logoUrl = e.target?.result as string
  }
  reader.readAsDataURL(file)
  
  try {
    if (!companyInfo.id) {
      console.error('请先保存基本信息后再上传Logo')
      return
    }

    const response = await uploadCompanyLogo(companyInfo.id, file)
    if (response?.data) {
      companyInfo.logoUrl = response.data
      console.log('Logo上传成功!')
    } else {
      throw new Error('上传失败')
    }
  } catch (error) {
    console.error('Logo上传失败:', error)
    console.error(`Logo上传失败：${error.message || '请重试'}`)
  }
}

const triggerLicenseUpload = () => {
  licenseInputRef.value?.click()
}

const handleLicenseUpload = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  
  if (!file) return
  
  if (file.type !== 'image/jpeg' && file.type !== 'image/png') {
    console.error('营业执照必须是JPG或PNG格式!')
    return
  }
  
  if (file.size > 5 * 1024 * 1024) {
    console.error('文件大小不能超过5MB!')
    return
  }
  
  try {
    if (!companyInfo.id) {
      console.error('请先保存基本信息后再上传营业执照')
      return
    }

    const response = await uploadBusinessLicense(companyInfo.id, file)
    if (response?.data) {
      companyInfo.businessLicenseUrl = response.data
      console.log('营业执照上传成功!')
    } else {
      throw new Error('上传失败')
    }
  } catch (error) {
    console.error('营业执照上传失败:', error)
    console.error(`营业执照上传失败：${error.message || '请重试'}`)
  }
}
</script>

<style scoped>
.company-info-container {
  --ink-green: #2E4E3F;
  --soft-green: #6FA892;
  --morandi-blue: #AFC7D9;
  --morandi-teal: #B8D6D0;
  --morandi-salmon: #F1C9BB;
  --morandi-gray: #D9E1E8;

  padding: 20px;
  background: transparent;
  min-height: calc(100vh - 60px);
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 24px;
}


.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: transparent;
  backdrop-filter: none;
  padding: 0;
  border-radius: 0;
  box-shadow: none;
  /* 响应式对齐 */
  flex-wrap: wrap;
  gap: 16px;
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-shrink: 0;
}

.main-content {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 24px;
  align-items: start;
}

.form-section {
  min-width: 0;
  /* 防止内容溢出 */
  overflow: hidden;
}

.preview-section {
  width: 320px;
  flex-shrink: 0;
  position: sticky;
  top: 20px;
  max-width: 100%;
}

.info-card {
  margin-bottom: 24px;
  border: none;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(46, 78, 63, 0.08);
  overflow: hidden;
  transition: all 0.3s ease;
}

.info-card:first-child { margin-top: 0; }

.info-card:hover {
  box-shadow: 0 8px 32px rgba(46, 78, 63, 0.15);
  transform: translateY(-2px);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: var(--ink-green);
}

.header-icon {
  font-size: 18px;
  color: var(--soft-green);
}

.header-title {
  font-size: 16px;
}

:deep(.el-form-item__label) {
  color: var(--ink-green);
  font-weight: 500;
}

:deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 4px 12px rgba(111, 168, 146, 0.15);
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 4px 12px rgba(111, 168, 146, 0.25);
}

:deep(.el-input-group__prepend) {
  background-color: var(--morandi-teal);
  border: 1px solid var(--morandi-gray);
  border-right: none;
  border-radius: 8px 0 0 8px;
  padding: 0 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 40px;
  transition: all 0.3s ease;
}

:deep(.el-input-group__prepend .el-icon) {
  color: var(--ink-green);
  font-size: 16px;
}

:deep(.el-input-group .el-input__wrapper) {
  border-left: none;
  border-radius: 0 8px 8px 0;
}

:deep(.el-input-group:hover .el-input-group__prepend) {
  background-color: var(--morandi-blue);
}

:deep(.el-select) {
  width: 100%;
}

:deep(.el-textarea__inner) {
  border-radius: 8px;
  border: 1px solid #dcdfe6;
  transition: all 0.3s ease;
}

:deep(.el-textarea__inner:hover) {
  border-color: var(--soft-green);
}

:deep(.el-textarea__inner:focus) {
  border-color: var(--soft-green);
  box-shadow: 0 0 0 2px rgba(111, 168, 146, 0.2);
}

.benefits-container {
  padding: 20px;
  background: linear-gradient(135deg, rgba(175, 199, 217, 0.1) 0%, rgba(217, 225, 232, 0.1) 100%);
  border-radius: 12px;
  border: 1px solid rgba(175, 199, 217, 0.3);
  margin-top: 8px;
}

.benefits-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16px 20px;
  justify-content: flex-start;
  align-items: center;
}

:deep(.el-checkbox) {
  margin-right: 0;
  margin-bottom: 0;
  padding: 8px 16px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(175, 199, 217, 0.4);
  transition: all 0.3s ease;
}

:deep(.el-checkbox:hover) {
  background: rgba(111, 168, 146, 0.1);
  border-color: var(--soft-green);
  transform: translateY(-1px);
}

:deep(.el-checkbox.is-checked) {
  background: var(--morandi-teal);
  border-color: var(--soft-green);
  box-shadow: 0 2px 8px rgba(111, 168, 146, 0.2);
}

:deep(.el-checkbox__label) {
  color: var(--ink-green);
  font-weight: 500;
  font-size: 14px;
  padding-left: 8px;
}

:deep(.el-checkbox.is-checked .el-checkbox__label) {
  color: var(--ink-green);
  font-weight: 600;
}

:deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background-color: var(--soft-green);
  border-color: var(--soft-green);
}

.logo-card {
  margin-bottom: 20px;
  border-radius: 16px;
  border: none;
  box-shadow: 0 4px 20px rgba(46, 78, 63, 0.08);
}

.logo-upload-container {
  text-align: center;
}

.logo-uploader {
  border: 2px dashed var(--morandi-blue);
  border-radius: 12px;
  width: 200px;
  height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  margin: 0 auto 16px;
  cursor: pointer;
}

.logo-uploader:hover {
  border-color: var(--soft-green);
  background-color: rgba(111, 168, 146, 0.05);
}

.logo-uploader-icon {
  font-size: 48px;
  color: var(--morandi-blue);
}

.logo-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 10px;
}

.logo-tips {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}

.status-card {
  margin-bottom: 20px;
  border-radius: 16px;
  border: none;
  box-shadow: 0 4px 20px rgba(46, 78, 63, 0.08);
}

.status-content {
  padding: 4px 0;
}

.status-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
}

.status-icon {
  font-size: 24px;
  padding: 8px;
  border-radius: 50%;
}

.status-icon.verified {
  color: #67c23a;
  background-color: rgba(103, 194, 58, 0.1);
}

.status-icon.pending {
  color: #e6a23c;
  background-color: rgba(230, 162, 60, 0.1);
}

.status-info {
  flex: 1;
}

.status-title {
  font-weight: 600;
  color: var(--ink-green);
  margin-bottom: 4px;
}

.status-desc {
  font-size: 14px;
  color: #909399;
}

.license-upload {
  text-align: center;
  padding: 16px 0;
}

.license-tips {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}

.license-status {
  color: #67c23a !important;
  font-weight: 500;
}

.stats-card {
  border-radius: 16px;
  border: none;
  box-shadow: 0 4px 20px rgba(46, 78, 63, 0.08);
}

.stats-content {
  text-align: center;
}

.progress-circle {
  margin-bottom: 20px;
}

.percentage-text {
  font-size: 20px;
  font-weight: 700;
  color: var(--ink-green);
}

.stats-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.stats-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: linear-gradient(135deg, rgba(175, 199, 217, 0.1) 0%, rgba(217, 225, 232, 0.1) 100%);
  border-radius: 8px;
}

.stats-label {
  font-size: 14px;
  color: var(--ink-green);
}

.stats-value {
  font-weight: 600;
  color: var(--soft-green);
}

.preview-content {
  background: #f8f9fa;
  border-radius: 12px;
  overflow: hidden;
}

.company-portal {
  background: white;
  min-height: 500px;
}

.portal-header {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 32px;
  background: linear-gradient(135deg, var(--morandi-blue) 0%, var(--morandi-teal) 100%);
  color: white;
}

.portal-logo {
  width: 80px;
  height: 80px;
  border-radius: 12px;
  overflow: hidden;
  background: white;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.portal-logo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.portal-logo-placeholder {
  color: var(--morandi-blue);
  font-size: 32px;
}

.portal-info {
  flex: 1;
}

.portal-title {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 8px;
  color: white;
}

.portal-slogan {
  font-size: 16px;
  margin-bottom: 16px;
  opacity: 0.9;
}

.portal-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.portal-content {
  padding: 32px;
}

.portal-section {
  margin-bottom: 32px;
}

.portal-section h3 {
  color: var(--ink-green);
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 2px solid var(--morandi-blue);
}

.portal-section p {
  line-height: 1.6;
  color: #666;
}

.benefits-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.benefit-tag {
  margin: 0;
}

.no-benefits {
  color: #999;
  font-style: italic;
}

@media (max-width: 1240px) {
  .company-info-container {
    max-width: calc(100vw - 40px);
    padding: 20px;
  }
}

@media (max-width: 1200px) {
  .company-info-container {
    padding: 16px;
  }
  
  .main-content {
    grid-template-columns: 1fr;
    gap: 20px;
  }
  
  .preview-section {
    width: 100%;
    position: static;
    max-width: 600px;
    margin: 0 auto;
  }
}

@media (max-width: 900px) {
  .company-info-container {
    padding: 16px;
  }
  
  .benefits-grid {
    gap: 12px 16px;
  }
  
  :deep(.el-checkbox) {
    padding: 6px 12px;
    font-size: 13px;
  }
}

@media (max-width: 768px) {
  .company-info-container {
    padding: 16px;
  }
  
  .header-content {
    flex-direction: column;
    gap: 16px;
    align-items: stretch;
  }
  
  .header-actions {
    justify-content: center;
  }
  
  .benefits-grid {
    gap: 10px 12px;
  }
  
  :deep(.el-checkbox) {
    padding: 4px 8px;
    font-size: 12px;
  }
  
  .portal-header {
    flex-direction: column;
    text-align: center;
  }
  
  .logo-uploader {
    width: 150px;
    height: 150px;
  }
}

@media (max-width: 480px) {
  .benefits-grid {
    flex-direction: column;
    align-items: stretch;
  }
  
  :deep(.el-checkbox) {
    justify-content: center;
    text-align: center;
  }
}

.info-card {
  animation: slideInUp 0.6s ease-out;
}

.info-card:nth-child(1) { animation-delay: 0.1s; }
.info-card:nth-child(2) { animation-delay: 0.2s; }
.info-card:nth-child(3) { animation-delay: 0.3s; }
.info-card:nth-child(4) { animation-delay: 0.4s; }

@keyframes slideInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

:deep(.el-button--primary) {
  background: linear-gradient(135deg, var(--soft-green) 0%, var(--ink-green) 100%);
  border: none;
  border-radius: 8px;
  font-weight: 500;
  transition: all 0.3s ease;
}

:deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, var(--ink-green) 0%, var(--soft-green) 100%);
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(46, 78, 63, 0.3);
}
</style>

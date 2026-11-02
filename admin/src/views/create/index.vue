<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import api from '@/api'
import type { Category, Customer, Grade, Order } from '@/types'
import { useUserStore } from '@/stores/user'
import { fmtMoney, fmtWeight, today } from '@/utils/format'

const userStore = useUserStore()

// ========== 基础数据 ==========
const categories = ref<Category[]>([])
const customers = ref<Customer[]>([])
const grades = ref<Grade[]>([])
const loadingData = ref(false)

// ========== 开单表单 ==========
const saving = ref(false)
const formRef = ref<FormInstance>()

function defaultForm() {
  return {
    orderDate: today(),
    customerId: undefined as number | undefined,
    categoryId: undefined as number | undefined,
    gradeId: undefined as number | undefined,
    gradeName: '',
    grossWeight: undefined as number | undefined,
    tareWeight: 0,
    deductWeight: 0,
    price: undefined as number | undefined,
    listPrice: undefined as number | undefined,
    remark: '',
    photoUrl: ''
  }
}

const form = reactive(defaultForm())

const rules: FormRules = {
  orderDate: [{ required: true, message: '请选择收购日期', trigger: 'change' }],
  customerId: [{ required: true, message: '请选择供货客户', trigger: 'change' }],
  categoryId: [{ required: true, message: '请选择品类', trigger: 'change' }],
  grossWeight: [{ required: true, message: '请填写毛重', trigger: 'blur' }]
}

// 计价重量 = 毛重 − 皮重 − 扣重；金额 = 计价重量 × 单价（四舍五入 2 位）
const netWeight = computed(() =>
  Math.max(Number(form.grossWeight || 0) - Number(form.tareWeight || 0) - Number(form.deductWeight || 0), 0)
)
const amount = computed(() => Math.round(netWeight.value * Number(form.price || 0) * 100) / 100)
// 改价提示：与档案带出价不一致
const priceChanged = computed(
  () => form.listPrice != null && form.price != null && Number(form.price) !== Number(form.listPrice)
)

// 同客户当日重复开单提醒（不硬拦）
const dupTip = ref('')

async function checkDup() {
  dupTip.value = ''
  if (!form.customerId || !form.orderDate) return
  try {
    const res = await api.orders.list({
      customerId: form.customerId,
      dateFrom: form.orderDate,
      dateTo: form.orderDate,
      page: 1,
      size: 5
    })
    if ((res.total || 0) > 0) {
      dupTip.value = `该客户 ${form.orderDate} 已有 ${res.total} 笔单据，请注意是否重复开单`
    }
  } catch {
    // 忽略提醒查询失败
  }
}

// ========== 客户 / 品类 / 等级联动 ==========
async function onCustomerChange() {
  await checkDup()
}

async function onCategoryChange() {
  form.gradeId = undefined
  form.gradeName = ''
  form.price = undefined
  form.listPrice = undefined
  grades.value = []
  if (!form.categoryId) return
  try {
    grades.value = await api.grades.list({ categoryId: form.categoryId })
  } catch {
    grades.value = []
  }
  // 自动带出该品类当日最合适的生效价
  await autoPickPrice()
}

async function autoPickPrice() {
  if (!form.categoryId) return
  try {
    const hit = await api.grades.effective(form.categoryId, form.orderDate)
    if (hit) {
      form.gradeId = hit.id
      form.gradeName = hit.grade
      form.price = hit.price
      form.listPrice = hit.price
    }
  } catch {
    // 无生效价：手填
  }
}

function onDateChange() {
  autoPickPrice()
  checkDup()
}

function onGradeChange(id: number | undefined) {
  const g = grades.value.find((x) => x.id === id)
  if (!g) return
  form.gradeName = g.grade
  // 校验该等级价格是否覆盖收购日期
  const d = form.orderDate
  const covers =
    d >= g.effectiveDate && (!g.expiryDate || d <= g.expiryDate)
  if (covers) {
    form.price = g.price
    form.listPrice = g.price
  } else {
    form.listPrice = undefined
    ElMessage.warning('所选等级价格对该收购日期未生效，请手填单价')
  }
}

function resetForm() {
  Object.assign(form, defaultForm())
  dupTip.value = ''
  formRef.value?.clearValidate()
}

async function save(draft: boolean) {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  if (netWeight.value <= 0) {
    ElMessage.error('计价重量必须大于 0（毛重 > 皮重 + 扣重）')
    return
  }
  if (form.price == null) {
    ElMessage.error('请填写单价（该品类当日无生效价）')
    return
  }
  saving.value = true
  try {
    await api.orders.create({
      orderDate: form.orderDate,
      customerId: form.customerId,
      categoryId: form.categoryId,
      gradeId: form.gradeId,
      gradeName: form.gradeName,
      grossWeight: form.grossWeight,
      tareWeight: form.tareWeight,
      deductWeight: form.deductWeight,
      price: form.price,
      remark: form.remark,
      photoUrl: form.photoUrl,
      draft
    })
    ElMessage.success(draft ? '已保存草稿' : '开单成功，已提交复核')
    resetForm()
    loadToday()
  } finally {
    saving.value = false
  }
}

// ========== 上传照片 ==========
const uploadHeaders = computed(() => ({ Authorization: `Bearer ${userStore.token}` }))
function onUploadSuccess(res: { code: number; data?: { url: string }; msg?: string }) {
  if (res.code === 0 && res.data) {
    form.photoUrl = res.data.url
    ElMessage.success('照片已上传')
  } else {
    ElMessage.error(res.msg || '上传失败')
  }
}

// ========== 当日单据列表 ==========
const todayList = ref<Order[]>([])
const todayLoading = ref(false)

async function loadToday() {
  todayLoading.value = true
  try {
    const res = await api.orders.list({ dateFrom: today(), dateTo: today(), page: 1, size: 50 })
    todayList.value = res.list || []
  } finally {
    todayLoading.value = false
  }
}

function statusTag(status?: string) {
  const map: Record<string, { label: string; type: 'success' | 'warning' | 'info' | 'danger' }> = {
    DRAFT: { label: '草稿', type: 'info' },
    PENDING: { label: '待复核', type: 'warning' },
    CONFIRMED: { label: '已确认', type: 'success' },
    VOID: { label: '已作废', type: 'danger' }
  }
  return map[status || ''] || { label: status || '-', type: 'info' as const }
}

onMounted(async () => {
  loadingData.value = true
  try {
    const [cats, custs] = await Promise.all([
      api.categories.list({ enabled: 1 }),
      api.customers.list({ status: 1 })
    ])
    categories.value = cats || []
    customers.value = custs || []
  } finally {
    loadingData.value = false
  }
  loadToday()
})
</script>

<template>
  <div v-loading="loadingData">
    <div class="page-header">
      <h3>收购开单</h3>
      <div class="page-header-desc">
        选品类+等级自动带出生效价（可改价留痕），金额自动计算，保存即提交复核
      </div>
    </div>

    <el-alert v-if="dupTip" :title="dupTip" type="warning" :closable="true" class="dup-tip" show-icon />

    <div class="page-card">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" label-position="top">
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12" :md="6">
            <el-form-item label="收购日期" prop="orderDate">
              <el-date-picker v-model="form.orderDate" type="date" value-format="YYYY-MM-DD"
                :clearable="false" style="width: 100%" @change="onDateChange" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="9">
            <el-form-item label="供货客户" prop="customerId">
              <el-select v-model="form.customerId" filterable placeholder="选择客户" style="width: 100%"
                @change="onCustomerChange">
                <el-option v-for="c in customers" :key="c.id"
                  :label="`${c.name}（${c.type}）`" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="9">
            <el-form-item label="品类" prop="categoryId">
              <el-select v-model="form.categoryId" filterable placeholder="选择品类" style="width: 100%"
                @change="onCategoryChange">
                <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :xs="24" :sm="12" :md="6">
            <el-form-item label="等级">
              <el-select v-model="form.gradeId" filterable placeholder="选择等级" style="width: 100%"
                @change="onGradeChange">
                <el-option v-for="g in grades" :key="g.id"
                  :label="`${g.grade} ¥${g.price}/kg（${g.effectiveDate}~${g.expiryDate || '长期'}）`"
                  :value="g.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="8" :sm="6" :md="3">
            <el-form-item label="毛重(kg)" prop="grossWeight">
              <el-input-number v-model="form.grossWeight" :min="0" :precision="2" :controls="false" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :xs="8" :sm="6" :md="3">
            <el-form-item label="皮重(kg)">
              <el-input-number v-model="form.tareWeight" :min="0" :precision="2" :controls="false" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :xs="8" :sm="6" :md="3">
            <el-form-item label="扣重(kg)">
              <el-input-number v-model="form.deductWeight" :min="0" :precision="2" :controls="false" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :xs="12" :sm="6" :md="3">
            <el-form-item label="单价(元/kg)">
              <el-input-number v-model="form.price" :min="0" :precision="2" :controls="false" style="width: 100%" />
            </el-form-item>
          </el-col>

          <el-col :xs="12" :sm="8" :md="4">
            <el-form-item label="计价重量">
              <div class="calc-box">{{ fmtWeight(netWeight) }} kg</div>
            </el-form-item>
          </el-col>
          <el-col :xs="12" :sm="8" :md="4">
            <el-form-item label="金额">
              <div class="calc-box amount">¥ {{ fmtMoney(amount) }}</div>
            </el-form-item>
          </el-col>
          <el-col :xs="12" :sm="8" :md="6">
            <el-form-item label="价格来源">
              <div class="calc-box">
                <span v-if="form.listPrice != null">带出价 {{ fmtMoney(form.listPrice) }}</span>
                <span v-else class="warn-text">无生效价 · 手填</span>
              </div>
            </el-form-item>
          </el-col>

          <el-col :xs="24" :sm="12" :md="7">
            <el-form-item label="附件照片">
              <el-upload :action="'/api/upload'" :headers="uploadHeaders" :show-file-list="false"
                :on-success="onUploadSuccess" accept="image/*">
                <el-button>拍照 / 选择图片</el-button>
              </el-upload>
              <span v-if="form.photoUrl" class="photo-ok">已上传</span>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="7">
            <el-form-item label="备注">
              <el-input v-model="form.remark" placeholder="备注（可选）" clearable />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <el-alert v-if="priceChanged" type="warning" :closable="false" show-icon class="price-tip"
        :title="`改价提醒：档案带出价 ${fmtMoney(form.listPrice)} → 成交价 ${fmtMoney(form.price)}（元/kg），保存后自动留痕`" />

      <div class="form-actions">
        <el-button type="primary" :loading="saving" @click="save(false)">保存并提交复核</el-button>
        <el-button :loading="saving" @click="save(true)">存为草稿</el-button>
        <el-button @click="resetForm">清空重填</el-button>
      </div>
    </div>

    <!-- 当日单据列表 -->
    <div class="page-card">
      <div class="list-title">今日单据</div>
      <el-table v-loading="todayLoading" :data="todayList" stripe style="width: 100%">
        <el-table-column prop="orderNo" label="单号" width="150" />
        <el-table-column prop="customerName" label="客户" min-width="110" />
        <el-table-column label="品类/等级" min-width="140">
          <template #default="{ row }">{{ row.categoryName }} / {{ row.gradeName || '-' }}</template>
        </el-table-column>
        <el-table-column label="计价重量(kg)" width="110" align="right">
          <template #default="{ row }">{{ fmtWeight(row.netWeight) }}</template>
        </el-table-column>
        <el-table-column label="单价" width="90" align="right">
          <template #default="{ row }">{{ fmtMoney(row.price) }}</template>
        </el-table-column>
        <el-table-column label="金额(元)" width="110" align="right">
          <template #default="{ row }"><b>{{ fmtMoney(row.amount) }}</b></template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status).type" size="small">{{ statusTag(row.status).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdByName" label="开单人" width="100" />
        <template #empty>
          <el-empty description="今天还没有开单" />
        </template>
      </el-table>
    </div>
  </div>
</template>

<style scoped>
.dup-tip {
  margin-bottom: 14px;
}

.calc-box {
  line-height: 32px;
  font-weight: 600;
  color: var(--cyt-text);
}

.calc-box.amount {
  color: #e5484d;
  font-size: 16px;
}

.warn-text {
  color: var(--cyt-warning);
  font-weight: 500;
}

.photo-ok {
  color: var(--cyt-primary);
  font-size: 12px;
  margin-left: 8px;
}

.price-tip {
  margin-bottom: 12px;
}

.form-actions {
  display: flex;
  gap: 10px;
  padding-top: 4px;
}

.list-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--cyt-text);
  margin-bottom: 12px;
}
</style>

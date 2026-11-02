<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import api from '@/api'
import type { Customer, Statement } from '@/types'
import { fmtMoney, fmtWeight, today } from '@/utils/format'

// ========== 查询条件 ==========
const customers = ref<Customer[]>([])
const customerId = ref<number | undefined>()
const dateRange = ref<[string, string] | null>(null)
const loading = ref(false)
const exporting = ref(false)
const statement = ref<Statement | null>(null)

function defaultRange(): [string, string] {
  const d = new Date()
  const first = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-01`
  return [first, today()]
}

async function search() {
  if (!customerId.value) {
    ElMessage.warning('请选择客户')
    return
  }
  loading.value = true
  try {
    statement.value = await api.reconcile.statement({
      customerId: customerId.value,
      dateFrom: dateRange.value?.[0] || '',
      dateTo: dateRange.value?.[1] || ''
    })
  } finally {
    loading.value = false
  }
}

async function exportExcel() {
  if (!customerId.value || !statement.value) {
    ElMessage.warning('请先查询对账单')
    return
  }
  exporting.value = true
  try {
    const blob = await api.export.statement({
      customerId: customerId.value,
      dateFrom: dateRange.value?.[0] || '',
      dateTo: dateRange.value?.[1] || ''
    })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `结算对账_${statement.value.customerName}_${dateRange.value?.[0] || ''}_${dateRange.value?.[1] || ''}.xlsx`
    document.body.appendChild(a)
    a.click()
    a.remove()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功，内容与页面一致')
  } finally {
    exporting.value = false
  }
}

function statusTag(s?: string) {
  const map: Record<string, { label: string; type: 'success' | 'warning' | 'info' | 'danger' }> = {
    DRAFT: { label: '草稿', type: 'info' },
    PENDING: { label: '待复核', type: 'warning' },
    CONFIRMED: { label: '已确认', type: 'success' },
    VOID: { label: '已作废', type: 'danger' }
  }
  return map[s || ''] || { label: s || '-', type: 'info' as const }
}

onMounted(async () => {
  dateRange.value = defaultRange()
  try {
    customers.value = await api.customers.list({})
  } catch {
    customers.value = []
  }
})
</script>

<template>
  <div>
    <div class="page-header">
      <h3>结算对账</h3>
      <div class="page-header-desc">
        客户 + 期间 → 期初应付 / 笔数 / 总重 / 总额 / 已付 / 期末应付；口径 = 已确认单据
      </div>
    </div>

    <!-- 查询条件 -->
    <div class="page-card query-bar">
      <el-select v-model="customerId" filterable placeholder="选择客户" style="width: 220px">
        <el-option v-for="c in customers" :key="c.id" :label="`${c.name}（${c.type}）`" :value="c.id" />
      </el-select>
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        value-format="YYYY-MM-DD"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        :clearable="true"
      />
      <el-button type="primary" :loading="loading" @click="search">查询对账</el-button>
      <div style="flex: 1"></div>
      <el-button type="primary" :loading="exporting" :disabled="!statement" @click="exportExcel">
        一键导出 Excel
      </el-button>
    </div>

    <template v-if="statement">
      <!-- 未复核提示 -->
      <el-alert
        v-if="Number(statement.pendingCount) > 0"
        type="warning"
        show-icon
        :closable="false"
        class="pending-alert"
        :title="`该期间有 ${statement.pendingCount} 笔待复核单据未纳入对账（金额未计入），请先复核后再对账`"
      />

      <!-- 汇总卡片 -->
      <el-row :gutter="16" class="sum-row">
        <el-col :xs="12" :sm="8" :md="4">
          <div class="page-card sum-card"><div class="sum-label">期初应付</div><div class="sum-value">¥ {{ fmtMoney(statement.opening) }}</div></div>
        </el-col>
        <el-col :xs="12" :sm="8" :md="4">
          <div class="page-card sum-card"><div class="sum-label">期间笔数</div><div class="sum-value">{{ statement.count }}</div></div>
        </el-col>
        <el-col :xs="12" :sm="8" :md="4">
          <div class="page-card sum-card"><div class="sum-label">总重(kg)</div><div class="sum-value">{{ fmtWeight(statement.totalWeight) }}</div></div>
        </el-col>
        <el-col :xs="12" :sm="8" :md="4">
          <div class="page-card sum-card"><div class="sum-label">期间总额</div><div class="sum-value">¥ {{ fmtMoney(statement.totalAmount) }}</div></div>
        </el-col>
        <el-col :xs="12" :sm="8" :md="4">
          <div class="page-card sum-card"><div class="sum-label">已付</div><div class="sum-value">¥ {{ fmtMoney(statement.paid) }}</div></div>
        </el-col>
        <el-col :xs="12" :sm="8" :md="4">
          <div class="page-card sum-card highlight"><div class="sum-label">期末应付</div><div class="sum-value">¥ {{ fmtMoney(statement.closing) }}</div></div>
        </el-col>
      </el-row>

      <!-- 逐笔明细 -->
      <div class="page-card">
        <div class="list-title">
          逐笔明细（已确认）
          <span class="list-sub">{{ statement.customerName }} · {{ statement.dateFrom || '起始' }} ~ {{ statement.dateTo }}</span>
        </div>
        <el-table :data="statement.details" stripe style="width: 100%">
          <el-table-column prop="orderNo" label="单号" width="150" />
          <el-table-column prop="orderDate" label="收购日期" width="110" />
          <el-table-column label="品类/等级" min-width="140">
            <template #default="{ row }">{{ row.categoryName }} / {{ row.gradeName || '-' }}</template>
          </el-table-column>
          <el-table-column label="计价重量(kg)" width="110" align="right">
            <template #default="{ row }">{{ fmtWeight(row.netWeight) }}</template>
          </el-table-column>
          <el-table-column label="单价(元/kg)" width="100" align="right">
            <template #default="{ row }">{{ fmtMoney(row.price) }}</template>
          </el-table-column>
          <el-table-column label="金额(元)" width="110" align="right">
            <template #default="{ row }"><b>{{ fmtMoney(row.amount) }}</b></template>
          </el-table-column>
          <el-table-column prop="createdByName" label="开单人" width="90" />
          <el-table-column prop="reviewedByName" label="复核人" width="90" />
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="statusTag(row.status).type" size="small">{{ statusTag(row.status).label }}</el-tag>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="该期间没有已确认单据" />
          </template>
        </el-table>
      </div>

      <!-- 付款明细 -->
      <div class="page-card">
        <div class="list-title">期间付款明细</div>
        <el-table :data="statement.payments" stripe style="width: 100%">
          <el-table-column prop="payDate" label="付款日期" width="120" />
          <el-table-column label="金额(元)" width="120" align="right">
            <template #default="{ row }"><b>{{ fmtMoney(row.amount) }}</b></template>
          </el-table-column>
          <el-table-column prop="method" label="方式" width="100" />
          <el-table-column prop="handler" label="经办人" width="110">
            <template #default="{ row }">{{ row.handler || '-' }}</template>
          </el-table-column>
          <el-table-column prop="createdByName" label="登记人" width="110" />
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 'VOID' ? 'danger' : 'success'" size="small">
                {{ row.status === 'VOID' ? '已作废' : '已登记' }}
              </el-tag>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="该期间没有付款记录" />
          </template>
        </el-table>
      </div>
    </template>

    <el-empty v-else-if="!loading" description="选择客户与期间后点击「查询对账」" />
  </div>
</template>

<style scoped>
.pending-alert {
  margin-bottom: 14px;
}

.sum-row {
  margin-bottom: 16px;
}

.sum-card {
  margin-bottom: 0;
  text-align: center;
}

.sum-label {
  font-size: 12px;
  color: var(--cyt-text-sub);
  margin-bottom: 6px;
}

.sum-value {
  font-size: 18px;
  font-weight: 700;
  color: var(--cyt-text);
}

.sum-card.highlight .sum-value {
  color: #e5484d;
}

.list-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--cyt-text);
  margin-bottom: 12px;
}

.list-sub {
  font-size: 12px;
  font-weight: 400;
  color: var(--cyt-text-sub);
  margin-left: 8px;
}
</style>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import api from '@/api'
import type { DailyReport } from '@/types'
import { fmtMoney, fmtWeight, today } from '@/utils/format'

const date = ref<string>(today())
const loading = ref(false)
const data = ref<DailyReport | null>(null)

async function load() {
  if (!date.value) return
  loading.value = true
  try {
    data.value = await api.reports.daily(date.value)
  } finally {
    loading.value = false
  }
}

// 切换日期自动刷新
watch(date, load)
onMounted(load)
</script>

<template>
  <div>
    <div class="page-header">
      <h3>日结报表</h3>
      <div class="page-header-desc">按天汇总收购金额、单数、净重与未付欠款</div>
    </div>

    <!-- 日期选择 -->
    <div class="page-card query-bar" style="margin-bottom: 16px">
      <span class="date-label">报表日期</span>
      <el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" :clearable="false" />
      <el-button type="primary" :loading="loading" @click="load">查询</el-button>
    </div>

    <div v-loading="loading">
      <!-- 汇总卡片 -->
      <el-row :gutter="16" class="kpi-row">
        <el-col :xs="12" :sm="6">
          <div class="page-card kpi-card">
            <div class="kpi-label">收购总额</div>
            <div class="kpi-value">¥ {{ fmtMoney(data?.summary.amount) }}</div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="page-card kpi-card">
            <div class="kpi-label">单数</div>
            <div class="kpi-value">{{ data?.summary.count ?? 0 }}</div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="page-card kpi-card">
            <div class="kpi-label">净重</div>
            <div class="kpi-value">{{ fmtWeight(data?.summary.netWeight) }}<span class="unit"> 斤</span></div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="page-card kpi-card">
            <div class="kpi-label">未付额</div>
            <div class="kpi-value warn">¥ {{ fmtMoney(data?.summary.unpaidAmount) }}</div>
          </div>
        </el-col>
      </el-row>

      <!-- 按品类汇总 -->
      <div class="page-card" style="margin-bottom: 16px">
        <div class="section-title">按品类汇总</div>
        <el-table :data="data?.byFruit || []" stripe style="width: 100%">
          <el-table-column prop="fruitName" label="品类" min-width="120" />
          <el-table-column prop="spec" label="规格" min-width="100">
            <template #default="{ row }">{{ row.spec || '-' }}</template>
          </el-table-column>
          <el-table-column prop="count" label="单数" width="100" align="right" />
          <el-table-column label="净重(斤)" width="130" align="right">
            <template #default="{ row }">{{ fmtWeight(row.netWeight) }}</template>
          </el-table-column>
          <el-table-column label="金额(元)" width="140" align="right">
            <template #default="{ row }">
              <b>{{ fmtMoney(row.amount) }}</b>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="当日无数据" />
          </template>
        </el-table>
      </div>

      <!-- 按果农汇总 -->
      <div class="page-card">
        <div class="section-title">按果农汇总</div>
        <el-table :data="data?.byFarmer || []" stripe style="width: 100%">
          <el-table-column prop="farmerName" label="果农" min-width="120" />
          <el-table-column prop="phone" label="电话" min-width="130">
            <template #default="{ row }">{{ row.phone || '-' }}</template>
          </el-table-column>
          <el-table-column prop="count" label="单数" width="100" align="right" />
          <el-table-column label="金额(元)" width="140" align="right">
            <template #default="{ row }">
              <b>{{ fmtMoney(row.amount) }}</b>
            </template>
          </el-table-column>
          <el-table-column label="未付额(元)" width="140" align="right">
            <template #default="{ row }">
              <span :class="{ 'warn-text': Number(row.unpaidAmount) > 0 }">{{ fmtMoney(row.unpaidAmount) }}</span>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="当日无数据" />
          </template>
        </el-table>
      </div>
    </div>
  </div>
</template>

<style scoped>
.date-label {
  font-size: 14px;
  color: var(--cyt-text);
}

.kpi-row {
  margin-bottom: 16px;
}

.kpi-card {
  margin-bottom: 0;
}

.kpi-value .unit {
  font-size: 14px;
  color: var(--cyt-text-sub);
  font-weight: 400;
}

.kpi-value.warn {
  color: var(--cyt-warning);
}

.section-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--cyt-text);
  margin: 0 0 12px;
}

.warn-text {
  color: var(--cyt-warning);
  font-weight: 600;
}
</style>

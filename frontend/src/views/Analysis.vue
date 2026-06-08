<template>
  <div class="analysis-page" v-if="data">
    <!-- 面包屑导航 -->
    <el-breadcrumb separator="/" class="breadcrumb">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item
        v-if="data.project_id"
        :to="{ name: 'project-detail', params: { id: data.project_id } }"
      >{{ data.project_name }}</el-breadcrumb-item>
      <el-breadcrumb-item>{{ data.filename }}</el-breadcrumb-item>
    </el-breadcrumb>

    <el-row :gutter="20">
      <!-- Summary -->
      <el-col :span="24">
        <el-card class="summary-card">
          <template #header>
            <div class="card-header">
              <span>{{ data.filename || '数据分析结果' }}</span>
              <div>
                <el-tag v-if="data.data_type === 'bill'" type="success" style="margin-right: 8px">账单数据</el-tag>
                <el-tag v-else-if="data.data_type === 'call_record'" type="warning" style="margin-right: 8px">话单数据</el-tag>
                <el-tag>{{ data.row_count }} 行 × {{ data.col_count }} 列</el-tag>
              </div>
            </div>
          </template>
          <p class="summary-text">{{ data.summary }}</p>
        </el-card>
      </el-col>
    </el-row>

    <!-- Charts -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="12" v-for="(chart, idx) in data.charts" :key="idx">
        <el-card>
          <template #header><span>{{ chart.title }}</span></template>
          <div :ref="(el) => setChartRef(el, idx)" :class="chart.type === 'heatmap' ? 'chart-container-heatmap' : 'chart-container'"></div>
          <p class="chart-desc">{{ chart.description }}</p>
        </el-card>
      </el-col>
    </el-row>

    <!-- Column Info -->
    <el-row :gutter="20">
      <el-col :span="24">
        <el-card>
          <template #header><span>数据列信息</span></template>
          <el-table :data="data.columns" stripe size="small">
            <el-table-column prop="name" label="列名" />
            <el-table-column prop="dtype" label="类型" width="120" />
            <el-table-column prop="null_count" label="空值数" width="100" />
            <el-table-column prop="unique_count" label="唯一值" width="100" />
            <el-table-column label="最小值" width="120">
              <template #default="{ row }">{{ row.min ?? '-' }}</template>
            </el-table-column>
            <el-table-column label="最大值" width="120">
              <template #default="{ row }">{{ row.max ?? '-' }}</template>
            </el-table-column>
            <el-table-column label="均值" width="120">
              <template #default="{ row }">{{ row.mean != null ? row.mean.toFixed(2) : '-' }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- Q&A -->
    <el-row :gutter="20" class="qa-row">
      <el-col :span="24">
        <el-card>
          <template #header><span>向数据提问</span></template>
          <div class="qa-input">
            <el-input
              v-model="question"
              placeholder="用自然语言提问，例如：哪个产品线增长最快？"
              @keyup.enter="doAsk"
              :disabled="asking"
              size="large"
            >
              <template #append>
                <el-button :loading="asking" @click="doAsk">提问</el-button>
              </template>
            </el-input>
          </div>

          <div v-if="qaHistory.length" class="qa-history">
            <div v-for="(item, idx) in qaHistory" :key="idx" class="qa-item">
              <div class="qa-question">
                <el-icon><i class="el-icon-chat-dot-round"></i></el-icon>
                {{ item.question }}
              </div>
              <div class="qa-answer">{{ item.answer }}</div>
              <div v-if="item.chart" :ref="(el) => setQAChartRef(el, idx)" class="qa-chart"></div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import * as echarts from 'echarts'
import { getFile, askQuestion } from '@/api/client'
import type { FileUploadResponse, QAResponse } from '@/api/client'
import { ElMessage } from 'element-plus'

const route = useRoute()
const data = ref<FileUploadResponse | null>(null)
const question = ref('')
const asking = ref(false)
const qaHistory = ref<{ question: string; answer: string; chart?: Record<string, unknown> }[]>([])
const chartRefs: Record<number, HTMLDivElement> = {}
const qaChartRefs: Record<number, HTMLDivElement> = {}

function setChartRef(el: unknown, idx: number) {
  if (el) chartRefs[idx] = el as HTMLDivElement
}

function setQAChartRef(el: unknown, idx: number) {
  if (el) qaChartRefs[idx] = el as HTMLDivElement
}

onMounted(async () => {
  if (route.params.id) {
    try {
      data.value = await getFile(Number(route.params.id))
    } catch (e) {
      ElMessage.error('加载文件数据失败')
    }
  }

  await nextTick()
  renderCharts()
})

function renderCharts() {
  if (!data.value?.charts) return
  data.value.charts.forEach((chart, idx) => {
    const el = chartRefs[idx]
    if (!el || !chart.data) return

    const chartInstance = echarts.init(el)
    let option: echarts.EChartsOption

    if (chart.type === 'line') {
      const d = chart.data as { x: string; y: number }[]
      option = {
        xAxis: { type: 'category', data: d.map(i => i.x) },
        yAxis: { type: 'value' },
        series: [{ data: d.map(i => i.y), type: 'line', smooth: true }],
        tooltip: { trigger: 'axis' },
      }
    } else if (chart.type === 'bar') {
      const d = chart.data as { x: string; y: number }[]
      option = {
        xAxis: { type: 'category', data: d.map(i => i.x), axisLabel: { rotate: 30 } },
        yAxis: { type: 'value' },
        series: [{ data: d.map(i => i.y), type: 'bar' }],
        tooltip: { trigger: 'axis' },
      }
    } else if (chart.type === 'pie') {
      const d = chart.data as { name: string; value: number }[]
      option = {
        series: [{ type: 'pie', radius: '60%', data: d }],
        tooltip: { trigger: 'item' },
      }
    } else if (chart.type === 'heatmap') {
      const fields = chart.fields || []
      const d = chart.data as number[][]
      option = {
        xAxis: { type: 'category', data: fields, axisLabel: { rotate: 30 } },
        yAxis: { type: 'category', data: fields },
        visualMap: { min: -1, max: 1, calculable: true, orient: 'horizontal', left: 'center', bottom: 0 },
        series: [{
          type: 'heatmap',
          data: d,
          label: { show: true, formatter: (p: { value: number[] }) => p.value[2]?.toFixed(2) || '' },
        }],
        tooltip: { formatter: (p: { value: number[] }) => `${fields[p.value[0]]} × ${fields[p.value[1]]}: ${p.value[2]?.toFixed(2)}` },
      }
    } else {
      return
    }
    chartInstance.setOption(option)
    window.addEventListener('resize', () => chartInstance.resize())
  })
}

async function doAsk() {
  if (!question.value.trim() || !data.value) return
  asking.value = true

  try {
    const fileId = Number(route.params.id) || 1
    const result: QAResponse = await askQuestion(fileId, question.value)
    qaHistory.value.unshift({
      question: result.question,
      answer: result.answer,
      chart: result.chart || undefined,
    })

    question.value = ''
    await nextTick()
    // Render QA charts
    qaHistory.value.forEach((item, idx) => {
      if (item.chart && qaChartRefs[idx]) {
        const chartInstance = echarts.init(qaChartRefs[idx])
        chartInstance.setOption(item.chart as echarts.EChartsOption)
      }
    })
  } catch (e: any) {
    ElMessage.error('提问失败: ' + (e.response?.data?.error || e.message))
  } finally {
    asking.value = false
  }
}
</script>

<style scoped>
.analysis-page { padding: 20px; }
.breadcrumb { margin-bottom: 20px; }
.summary-card { margin-bottom: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.summary-text { line-height: 1.8; color: #606266; font-size: 15px; }
.chart-row { margin-top: 20px; }
.chart-container { height: 300px; }
.chart-container-heatmap { height: 400px; }
.chart-desc { color: #909399; font-size: 13px; margin-top: 8px; }
.qa-row { margin-top: 20px; }
.qa-input { margin-bottom: 20px; }
.qa-history { max-height: 500px; overflow-y: auto; }
.qa-item { margin-bottom: 16px; padding: 12px; background: #f5f7fa; border-radius: 8px; }
.qa-question { font-weight: 600; color: #409eff; margin-bottom: 8px; }
.qa-answer { color: #303133; line-height: 1.6; }
.qa-chart { height: 250px; margin-top: 12px; }
</style>

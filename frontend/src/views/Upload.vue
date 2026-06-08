<template>
  <div class="upload-page">
    <div class="hero">
      <h2>上传 Excel / CSV 文件</h2>
      <p>AI 自动分析数据，生成图表和洞察</p>
    </div>

    <!-- 项目选择器 -->
    <div class="project-selector">
      <el-select
        v-model="selectedProjectId"
        placeholder="选择项目（可选）"
        clearable
        style="width: 300px;"
      >
        <el-option
          v-for="p in projects"
          :key="p.id"
          :label="p.name"
          :value="p.id"
        />
      </el-select>
      <el-button text type="primary" @click="showCreateDialog = true">+ 新建项目</el-button>
    </div>

    <el-upload
      class="upload-area"
      drag
      :auto-upload="false"
      :on-change="handleFileChange"
      :show-file-list="false"
      accept=".xlsx,.xls,.csv"
    >
      <div class="upload-inner">
        <el-icon class="el-icon--upload"><i class="el-icon-upload"></i></el-icon>
        <div class="el-upload__text">
          拖拽文件到此处，或 <em>点击选择文件</em>
        </div>
      </div>
      <template #tip>
        <div class="el-upload__tip">支持 .xlsx、.xls、.csv 格式，最大 50MB</div>
      </template>
    </el-upload>

    <el-button
      v-if="selectedFile"
      type="primary"
      size="large"
      :loading="uploading"
      @click="doUpload"
      class="upload-btn"
    >
      {{ uploading ? '正在分析...' : '开始分析' }}
    </el-button>

    <div v-if="uploading" class="progress-info">
      <el-progress :percentage="progress" :stroke-width="8" />
      <p>{{ progressText }}</p>
    </div>

    <div v-if="recentFiles.length" class="recent-files">
      <h3>最近分析的文件</h3>
      <el-table :data="recentFiles" @row-click="goToAnalysis" stripe>
        <el-table-column prop="filename" label="文件名" />
        <el-table-column prop="project_name" label="项目" width="160">
          <template #default="{ row }">{{ row.project_name || '-' }}</template>
        </el-table-column>
        <el-table-column prop="row_count" label="行数" width="120" />
        <el-table-column prop="created_at" label="分析时间" width="200" />
      </el-table>
    </div>

    <!-- 新建项目对话框 -->
    <el-dialog v-model="showCreateDialog" title="新建项目" width="420px">
      <el-form label-width="80px">
        <el-form-item label="项目名称">
          <el-input v-model="newProjectName" placeholder="请输入项目名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="newProjectDesc" type="textarea" :rows="3" placeholder="项目描述（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreateProject">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { uploadFile, listFiles, listProjects, createProject } from '@/api/client'
import type { Project } from '@/api/client'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const selectedFile = ref<File | null>(null)
const uploading = ref(false)
const progress = ref(0)
const progressText = ref('')
const recentFiles = ref<{ id: number; filename: string; row_count: number; created_at: string; project_name?: string }[]>([])
const projects = ref<Project[]>([])
const selectedProjectId = ref<number | undefined>(undefined)
const showCreateDialog = ref(false)
const newProjectName = ref('')
const newProjectDesc = ref('')
const creating = ref(false)

onMounted(async () => {
  // 从 URL query 预选项目
  const queryProjectId = route.query.projectId
  if (queryProjectId) {
    selectedProjectId.value = Number(queryProjectId)
  }

  try {
    const [files, projectList] = await Promise.all([
      listFiles(),
      listProjects(),
    ])
    recentFiles.value = files
    projects.value = projectList
  } catch (e) {
    // ignore
  }
})

function handleFileChange(file: { raw: File }) {
  selectedFile.value = file.raw
}

async function doUpload() {
  if (!selectedFile.value) return
  uploading.value = true
  progress.value = 10
  progressText.value = '正在上传文件...'

  const timer = setInterval(() => {
    if (progress.value < 80) {
      progress.value += 5
      if (progress.value > 30) progressText.value = '正在分析数据结构...'
      if (progress.value > 50) progressText.value = 'AI 正在生成分析摘要...'
      if (progress.value > 70) progressText.value = '正在生成图表...'
    }
  }, 300)

  try {
    const result = await uploadFile(selectedFile.value, selectedProjectId.value)
    clearInterval(timer)
    progress.value = 100
    progressText.value = '分析完成！'

    // Navigate using file ID from backend
    setTimeout(() => {
      router.push({ name: 'analysis', params: { id: result.id } })
    }, 500)
  } catch (e: any) {
    clearInterval(timer)
    ElMessage.error('上传失败: ' + (e.response?.data?.error || e.message))
    uploading.value = false
    progress.value = 0
  }
}

function goToAnalysis(row: { id: number }) {
  router.push({ name: 'analysis', params: { id: row.id } })
}

async function handleCreateProject() {
  if (!newProjectName.value.trim()) {
    ElMessage.warning('请输入项目名称')
    return
  }
  creating.value = true
  try {
    const project = await createProject(newProjectName.value.trim(), newProjectDesc.value.trim() || undefined)
    projects.value.unshift(project)
    selectedProjectId.value = project.id
    showCreateDialog.value = false
    newProjectName.value = ''
    newProjectDesc.value = ''
    ElMessage.success('项目创建成功')
  } catch (e: any) {
    ElMessage.error('创建失败: ' + (e.response?.data?.error || e.message))
  } finally {
    creating.value = false
  }
}
</script>

<style scoped>
.upload-page { max-width: 800px; margin: 0 auto; padding: 40px 20px; }
.hero { text-align: center; margin-bottom: 40px; }
.hero h2 { font-size: 28px; color: #303133; margin-bottom: 8px; }
.hero p { color: #909399; font-size: 16px; }
.project-selector { display: flex; align-items: center; gap: 8px; margin-bottom: 20px; }
.upload-area { width: 100%; }
.upload-btn { margin-top: 20px; width: 200px; }
.progress-info { margin-top: 20px; }
.progress-info p { color: #909399; margin-top: 8px; text-align: center; }
.recent-files { margin-top: 40px; }
.recent-files h3 { margin-bottom: 16px; color: #303133; }
</style>

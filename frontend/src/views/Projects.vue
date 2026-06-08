<template>
  <div class="projects-page">
    <div class="hero">
      <h2>我的项目</h2>
      <p>管理你的数据分析项目</p>
    </div>

    <el-row :gutter="20">
      <!-- "全部文件" 卡片 -->
      <el-col :xs="24" :sm="12" :md="8" :lg="6" class="card-col">
        <el-card class="project-card all-files-card" shadow="hover" @click="router.push('/upload')">
          <div class="card-content">
            <el-icon class="card-icon"><i class="el-icon-folder-opened"></i></el-icon>
            <h3>全部文件</h3>
            <p>查看所有已上传的文件</p>
          </div>
        </el-card>
      </el-col>

      <!-- 项目卡片 -->
      <el-col
        v-for="project in projects"
        :key="project.id"
        :xs="24" :sm="12" :md="8" :lg="6"
        class="card-col"
      >
        <el-card class="project-card" shadow="hover" @click="goToProject(project.id)">
          <div class="card-content">
            <h3>{{ project.name }}</h3>
            <p class="desc">{{ project.description || '暂无描述' }}</p>
            <div class="card-meta">
              <span><el-icon><i class="el-icon-document"></i></el-icon> {{ project.file_count ?? 0 }} 个文件</span>
              <span>{{ formatDate(project.created_at) }}</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 新建项目 "+" 卡片 -->
      <el-col :xs="24" :sm="12" :md="8" :lg="6" class="card-col">
        <el-card class="project-card add-card" shadow="hover" @click="showCreateDialog = true">
          <div class="card-content">
            <el-icon class="card-icon add-icon"><i class="el-icon-plus"></i></el-icon>
            <h3>新建项目</h3>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 创建项目对话框 -->
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
        <el-button type="primary" :loading="creating" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listProjects, createProject } from '@/api/client'
import type { Project } from '@/api/client'
import { ElMessage } from 'element-plus'

const router = useRouter()
const projects = ref<Project[]>([])
const showCreateDialog = ref(false)
const newProjectName = ref('')
const newProjectDesc = ref('')
const creating = ref(false)

onMounted(async () => {
  try {
    projects.value = await listProjects()
  } catch (e) {
    // ignore
  }
})

function goToProject(id: number) {
  router.push({ name: 'project-detail', params: { id } })
}

function formatDate(dateStr: string) {
  if (!dateStr) return ''
  return dateStr.slice(0, 10)
}

async function handleCreate() {
  if (!newProjectName.value.trim()) {
    ElMessage.warning('请输入项目名称')
    return
  }
  creating.value = true
  try {
    const project = await createProject(newProjectName.value.trim(), newProjectDesc.value.trim() || undefined)
    projects.value.unshift(project)
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
.projects-page { max-width: 1200px; margin: 0 auto; padding: 40px 20px; }
.hero { text-align: center; margin-bottom: 40px; }
.hero h2 { font-size: 28px; color: #303133; margin-bottom: 8px; }
.hero p { color: #909399; font-size: 16px; }
.card-col { margin-bottom: 20px; }
.project-card { cursor: pointer; height: 160px; transition: transform 0.2s; }
.project-card:hover { transform: translateY(-4px); }
.card-content { display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; text-align: center; }
.card-content h3 { font-size: 18px; color: #303133; margin-bottom: 8px; }
.card-content p { color: #909399; font-size: 14px; }
.card-content .desc { margin-bottom: 12px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 100%; }
.card-icon { font-size: 40px; color: #409eff; margin-bottom: 12px; }
.card-meta { display: flex; gap: 16px; font-size: 12px; color: #909399; margin-top: 4px; }
.card-meta .el-icon { margin-right: 2px; }
.all-files-card .card-icon { color: #67c23a; }
.add-card { border-style: dashed; }
.add-icon { color: #c0c4cc; }
</style>

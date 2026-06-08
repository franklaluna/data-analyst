<template>
  <div class="project-detail-page" v-if="project">
    <!-- 顶部信息 -->
    <div class="project-header">
      <div class="project-info" v-if="!editing">
        <h2>{{ project.name }}</h2>
        <p class="desc">{{ project.description || '暂无描述' }}</p>
      </div>
      <div class="project-info" v-else>
        <el-input v-model="editName" size="large" style="max-width: 400px; margin-bottom: 8px;" />
        <el-input v-model="editDesc" placeholder="项目描述（可选）" />
      </div>
      <div class="project-actions">
        <template v-if="!editing">
          <el-button @click="startEdit">编辑</el-button>
          <el-popconfirm title="确定删除此项目？文件不会被删除。" @confirm="handleDelete">
            <template #reference>
              <el-button type="danger" plain>删除</el-button>
            </template>
          </el-popconfirm>
        </template>
        <template v-else>
          <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
          <el-button @click="editing = false">取消</el-button>
        </template>
      </div>
    </div>

    <!-- 操作栏 -->
    <div class="action-bar">
      <el-button type="primary" @click="goUpload">上传到此项目</el-button>
    </div>

    <!-- 文件列表 -->
    <el-card>
      <template #header><span>项目文件</span></template>
      <el-table :data="project.files" stripe @row-click="goToAnalysis" style="cursor: pointer;">
        <el-table-column prop="filename" label="文件名" />
        <el-table-column prop="row_count" label="行数" width="120" />
        <el-table-column prop="col_count" label="列数" width="120" />
        <el-table-column prop="created_at" label="上传时间" width="200" />
      </el-table>
      <el-empty v-if="!project.files.length" description="暂无文件，点击上方按钮上传" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProject, updateProject, deleteProject } from '@/api/client'
import type { ProjectDetail } from '@/api/client'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const project = ref<ProjectDetail | null>(null)
const editing = ref(false)
const editName = ref('')
const editDesc = ref('')
const saving = ref(false)

const projectId = Number(route.params.id)

onMounted(async () => {
  try {
    project.value = await getProject(projectId)
  } catch (e) {
    ElMessage.error('加载项目失败')
  }
})

function startEdit() {
  if (!project.value) return
  editName.value = project.value.name
  editDesc.value = project.value.description || ''
  editing.value = true
}

async function handleSave() {
  if (!editName.value.trim()) {
    ElMessage.warning('请输入项目名称')
    return
  }
  saving.value = true
  try {
    await updateProject(projectId, editName.value.trim(), editDesc.value.trim() || undefined)
    if (project.value) {
      project.value.name = editName.value.trim()
      project.value.description = editDesc.value.trim()
    }
    editing.value = false
    ElMessage.success('保存成功')
  } catch (e: any) {
    ElMessage.error('保存失败: ' + (e.response?.data?.error || e.message))
  } finally {
    saving.value = false
  }
}

async function handleDelete() {
  try {
    await deleteProject(projectId)
    ElMessage.success('项目已删除')
    router.push('/')
  } catch (e: any) {
    ElMessage.error('删除失败: ' + (e.response?.data?.error || e.message))
  }
}

function goUpload() {
  router.push({ name: 'upload', query: { projectId: String(projectId) } })
}

function goToAnalysis(row: { id: number }) {
  router.push({ name: 'analysis', params: { id: row.id } })
}
</script>

<style scoped>
.project-detail-page { max-width: 1000px; margin: 0 auto; padding: 40px 20px; }
.project-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; }
.project-info h2 { font-size: 24px; color: #303133; margin-bottom: 6px; }
.project-info .desc { color: #909399; font-size: 14px; }
.project-actions { display: flex; gap: 8px; flex-shrink: 0; }
.action-bar { margin-bottom: 20px; }
</style>

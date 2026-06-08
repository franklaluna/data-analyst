import axios from 'axios'

const api = axios.create({ baseURL: '/api' })

export interface FileUploadResponse {
  id: number
  filename: string
  row_count: number
  col_count: number
  data_type?: string
  summary: string
  columns: ColumnInfo[]
  charts: ChartInfo[]
  sample_data: Record<string, unknown>[]
  correlation?: { fields: string[]; matrix: number[][] }
  project_id?: number | null
  project_name?: string | null
}

export interface ColumnInfo {
  name: string
  dtype: string
  null_count: number
  unique_count: number
  min?: number | string
  max?: number | string
  mean?: number
  sum?: number
}

export interface ChartInfo {
  type: 'line' | 'bar' | 'pie' | 'heatmap'
  title: string
  x_axis?: string
  y_axis?: string
  name_field?: string
  value_field?: string
  fields?: string[]
  description: string
  data?: Record<string, unknown>[]
}

export interface QAResponse {
  question: string
  answer: string
  code: string
  result?: unknown
  chart?: Record<string, unknown>
}

export interface Project {
  id: number
  name: string
  description: string
  created_at: string
  file_count?: number
}

export interface ProjectDetail extends Project {
  files: { id: number; filename: string; row_count: number; col_count: number; created_at: string }[]
}

export function uploadFile(file: File, projectId?: number): Promise<FileUploadResponse> {
  const formData = new FormData()
  formData.append('file', file)
  if (projectId != null) formData.append('project_id', String(projectId))
  return api.post('/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  }).then(r => r.data)
}

export function getFile(id: number): Promise<FileUploadResponse> {
  return api.get(`/files/${id}`).then(r => r.data)
}

export function listFiles(projectId?: number): Promise<{ id: number; filename: string; row_count: number; created_at: string; project_name?: string }[]> {
  const params = projectId != null ? { project_id: projectId } : undefined
  return api.get('/files', { params }).then(r => r.data)
}

export function askQuestion(fileId: number, question: string): Promise<QAResponse> {
  return api.post('/qa', { fileId, question }).then(r => r.data)
}

export function getQAHistory(fileId: number): Promise<{ question: string; answer: string; chart_json: string }[]> {
  return api.get(`/qa/history/${fileId}`).then(r => r.data)
}

export function listProjects(): Promise<Project[]> {
  return api.get('/projects').then(r => r.data)
}

export function createProject(name: string, description?: string): Promise<Project> {
  return api.post('/projects', { name, description }).then(r => r.data)
}

export function getProject(id: number): Promise<ProjectDetail> {
  return api.get(`/projects/${id}`).then(r => r.data)
}

export function updateProject(id: number, name: string, description?: string): Promise<void> {
  return api.put(`/projects/${id}`, { name, description })
}

export function deleteProject(id: number): Promise<void> {
  return api.delete(`/projects/${id}`)
}

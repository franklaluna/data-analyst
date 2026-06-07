import axios from 'axios'

const api = axios.create({ baseURL: '/api' })

export interface FileUploadResponse {
  id: number
  filename: string
  row_count: number
  col_count: number
  summary: string
  columns: ColumnInfo[]
  charts: ChartInfo[]
  sample_data: Record<string, unknown>[]
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
  type: 'line' | 'bar' | 'pie'
  title: string
  x_axis?: string
  y_axis?: string
  name_field?: string
  value_field?: string
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

export function uploadFile(file: File): Promise<FileUploadResponse> {
  const formData = new FormData()
  formData.append('file', file)
  return api.post('/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  }).then(r => r.data)
}

export function getFile(id: number): Promise<FileUploadResponse> {
  return api.get(`/files/${id}`).then(r => r.data)
}

export function listFiles(): Promise<{ id: number; filename: string; row_count: number; created_at: string }[]> {
  return api.get('/files').then(r => r.data)
}

export function askQuestion(fileId: number, question: string): Promise<QAResponse> {
  return api.post('/qa', { fileId, question }).then(r => r.data)
}

export function getQAHistory(fileId: number): Promise<{ question: string; answer: string; chart_json: string }[]> {
  return api.get(`/qa/history/${fileId}`).then(r => r.data)
}

# API 接口文档

## POST /api/files/upload
- Content-Type: multipart/form-data
- 参数: file (Excel/CSV 文件)
- 返回:
```json
{
  "filename": "string",
  "row_count": 12,
  "col_count": 4,
  "summary": "AI 分析摘要",
  "columns": [{"name": "string", "dtype": "string", "null_count": 0, "unique_count": 6, "min": 0, "max": 100, "mean": 50, "sum": 600}],
  "charts": [{"type": "line|bar|pie", "title": "string", "x_axis": "string", "y_axis": "string", "description": "string", "data": {}}],
  "sample_data": [{}]
}
```

## GET /api/files
- 返回文件列表: `[{id, filename, row_count, col_count, created_at}]`

## GET /api/files/{id}
- 返回文件的完整分析结果（同 upload 返回格式）

## POST /api/qa
- Content-Type: application/json
- Body: `{ "fileId": 1, "question": "哪个产品线增长最快？" }`
- 返回:
```json
{
  "question": "string",
  "answer": "AI 回答",
  "code": "生成的 pandas 代码",
  "result": {},
  "chart": {}
}
```

## GET /api/qa/history/{fileId}
- 返回问答历史: `[{question, answer, chart_json, created_at}]`

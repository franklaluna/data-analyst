from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from profiler import profile_file
from executor import execute_code

app = FastAPI(title="Data Analyst Python Service")


class AnalyzeRequest(BaseModel):
    file_path: str


class ExecuteRequest(BaseModel):
    code: str
    file_path: str


@app.post("/analyze")
def analyze(req: AnalyzeRequest):
    try:
        result = profile_file(req.file_path)
        return result
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@app.post("/execute")
def execute(req: ExecuteRequest):
    try:
        result = execute_code(req.code, req.file_path)
        return result
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@app.get("/health")
def health():
    return {"status": "ok"}

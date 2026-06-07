import subprocess
import sys
import json
import os
import re
import tempfile
import pathlib


UPLOAD_DIR = "/tmp/data-analyst-uploads"


def execute_code(code: str, file_path: str) -> dict:
    # Validate file_path: must be within allowed directory
    resolved = pathlib.Path(file_path).resolve()
    allowed = pathlib.Path(UPLOAD_DIR).resolve()
    if not str(resolved).startswith(str(allowed)):
        raise ValueError("File path outside allowed directory")
    if not resolved.exists():
        raise FileNotFoundError(f"File not found: {file_path}")

    safe_path = str(resolved).replace("\\", "/")

    # Validate code: whitelist approach - only allow safe operations
    forbidden = ['import os', 'import sys', 'import subprocess', 'open(',
                  'exec(', 'eval(', '__import__', 'compile(',
                  'import shutil', 'import pathlib', 'import socket',
                  'import importlib', 'getattr(', 'setattr(',
                  '__builtins__', '__globals__', '__locals__']
    for keyword in forbidden:
        if keyword in code:
            raise ValueError(f"Forbidden operation: {keyword}")

    # Additional: check for obfuscated patterns
    if re.search(r'__[a-z]+__', code):
        raise ValueError("Dunder attributes are not allowed")

    ext = os.path.splitext(safe_path)[1].lower()
    if ext not in ('.xlsx', '.xls', '.csv'):
        raise ValueError(f"Unsupported file format: {ext}")

    # Wrap code to load data and capture result
    # Use json.dumps to safely encode the file path
    safe_path_json = json.dumps(safe_path)
    wrapper = f"""
import pandas as pd
import numpy as np
import json
import sys

def _safe_serialize(obj):
    if isinstance(obj, pd.DataFrame):
        return obj.head(100).to_dict(orient='records')
    elif isinstance(obj, pd.Series):
        return obj.head(100).to_dict()
    elif isinstance(obj, (np.integer,)):
        return int(obj)
    elif isinstance(obj, (np.floating,)):
        return float(obj)
    elif isinstance(obj, np.ndarray):
        return obj.tolist()
    return obj

try:
    _file_path = {safe_path_json}
    _ext = _file_path.rsplit('.', 1)[-1].lower() if '.' in _file_path else ''
    if _ext in ('xlsx', 'xls'):
        df = pd.read_excel(_file_path, nrows=100000)
    elif _ext == 'csv':
        df = pd.read_csv(_file_path, nrows=100000)
    else:
        raise ValueError(f"Unsupported: {{_ext}}")

    result = None
    chart = None

{indent_code(code, 4)}

    output = {{"result": _safe_serialize(result), "chart": chart}}
    print("__RESULT_START__")
    print(json.dumps(output, ensure_ascii=False, default=str))
    print("__RESULT_END__")
except Exception as e:
    print("__RESULT_START__")
    print(json.dumps({{"error": str(e)}}, ensure_ascii=False))
    print("__RESULT_END__")
"""

    with tempfile.NamedTemporaryFile(mode='w', suffix='.py', delete=False) as f:
        f.write(wrapper)
        tmp_path = f.name

    try:
        proc = subprocess.run(
            [sys.executable, tmp_path],
            capture_output=True, text=True, timeout=10
        )

        if proc.returncode != 0:
            return {"error": proc.stderr or "Execution failed"}

        # Extract result between markers
        stdout = proc.stdout
        start = stdout.find("__RESULT_START__")
        end = stdout.find("__RESULT_END__")
        if start >= 0 and end > start:
            json_str = stdout[start + len("__RESULT_START__"):end].strip()
            return json.loads(json_str)

        return {"error": "No result returned", "stdout": stdout[:500]}
    except subprocess.TimeoutExpired:
        return {"error": "Code execution timed out (10s limit)"}
    finally:
        os.unlink(tmp_path)


def indent_code(code: str, spaces: int) -> str:
    prefix = " " * spaces
    lines = code.split('\n')
    return '\n'.join(prefix + line for line in lines)

import subprocess
import sys
import json
import os
import tempfile


def execute_code(code: str, file_path: str) -> dict:
    # Validate code: whitelist approach - only allow safe operations
    forbidden = ['import os', 'import sys', 'import subprocess', 'open(',
                  'exec(', 'eval(', '__import__', 'compile(',
                  'import shutil', 'import pathlib', 'import socket']
    for keyword in forbidden:
        if keyword in code:
            raise ValueError(f"Forbidden operation: {keyword}")

    # Wrap code to load data and capture result
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
    ext = '{os.path.splitext(file_path)[1].lower()}'
    if ext in ('.xlsx', '.xls'):
        df = pd.read_excel('{file_path}', nrows=100000)
    elif ext == '.csv':
        df = pd.read_csv('{file_path}', nrows=100000)
    else:
        raise ValueError(f"Unsupported: {{ext}}")

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

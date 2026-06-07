import pandas as pd
import numpy as np
import json
import os


def profile_file(file_path: str) -> dict:
    ext = os.path.splitext(file_path)[1].lower()
    if ext in ('.xlsx', '.xls'):
        df = pd.read_excel(file_path, nrows=50000)
    elif ext == '.csv':
        df = pd.read_csv(file_path, nrows=50000)
    else:
        raise ValueError(f"Unsupported file format: {ext}")

    # Convert extension dtypes to numpy dtypes for compatibility
    df = df.convert_dtypes(convert_integer=False, convert_floating=False,
                           convert_boolean=False, convert_string=False)

    row_count = len(df)
    col_count = len(df.columns)

    columns_info = []
    for col in df.columns:
        dtype = str(df[col].dtype)
        null_count = int(df[col].isnull().sum())
        unique_count = int(df[col].nunique())

        info = {
            "name": str(col),
            "dtype": dtype,
            "null_count": null_count,
            "unique_count": unique_count,
        }

        if pd.api.types.is_numeric_dtype(df[col]):
            info["min"] = _safe_float(df[col].min())
            info["max"] = _safe_float(df[col].max())
            info["mean"] = _safe_float(df[col].mean())
            info["sum"] = _safe_float(df[col].sum())
            info["std"] = _safe_float(df[col].std())
        elif _is_date_column(df[col]):
            info["dtype"] = "datetime"
            try:
                dates = pd.to_datetime(df[col], errors='coerce')
                info["min"] = str(dates.min())
                info["max"] = str(dates.max())
            except Exception:
                pass

        columns_info.append(info)

    # Generate suggested charts
    charts = _suggest_charts(df, columns_info)

    # Sample data for LLM context
    sample_data = df.head(5).to_dict(orient='records')
    for row in sample_data:
        for k, v in row.items():
            if isinstance(v, (np.integer,)):
                row[k] = int(v)
            elif isinstance(v, (np.floating,)):
                row[k] = _safe_float(v)
            elif isinstance(v, pd.Timestamp):
                row[k] = str(v)
            elif v is None or (isinstance(v, float) and np.isnan(v)):
                row[k] = None

    return {
        "row_count": row_count,
        "col_count": col_count,
        "columns": columns_info,
        "charts": charts,
        "sample_data": sample_data,
    }


def _safe_float(val):
    try:
        if val is None or (isinstance(val, float) and np.isnan(val)):
            return None
        if np.isinf(val):
            return None
        return round(float(val), 4)
    except Exception:
        return None


def _is_date_column(series):
    if pd.api.types.is_string_dtype(series) or series.dtype == 'object':
        sample = series.dropna().head(20)
        try:
            pd.to_datetime(sample, errors='raise')
            return True
        except Exception:
            return False
    return False


def _suggest_charts(df, columns_info):
    charts = []
    numeric_cols = [c for c in columns_info if pd.api.types.is_numeric_dtype(df[c["name"]])]
    date_cols = [c for c in columns_info if c["dtype"] == "datetime"]
    categorical_cols = [c for c in columns_info
                        if (c["dtype"] in ("object", "string")) and c["unique_count"] < 50]

    # Time series line chart
    if date_cols and numeric_cols:
        charts.append({
            "type": "line",
            "title": f"{numeric_cols[0]['name']} 趋势",
            "x_axis": date_cols[0]["name"],
            "y_axis": numeric_cols[0]["name"],
            "description": f"按 {date_cols[0]['name']} 展示 {numeric_cols[0]['name']} 的变化趋势"
        })

    # Categorical bar chart
    if categorical_cols and numeric_cols:
        charts.append({
            "type": "bar",
            "title": f"各{categorical_cols[0]['name']}的{numeric_cols[0]['name']}",
            "x_axis": categorical_cols[0]["name"],
            "y_axis": numeric_cols[0]["name"],
            "description": f"按 {categorical_cols[0]['name']} 分组统计 {numeric_cols[0]['name']}"
        })

    # Pie chart for categorical with few values
    if categorical_cols:
        cat = categorical_cols[0]
        if cat["unique_count"] <= 10 and numeric_cols:
            charts.append({
                "type": "pie",
                "title": f"{numeric_cols[0]['name']} 占比",
                "name_field": cat["name"],
                "value_field": numeric_cols[0]["name"],
                "description": f"按 {cat['name']} 展示 {numeric_cols[0]['name']} 的占比分布"
            })

    # Second numeric comparison if available
    if len(numeric_cols) >= 2 and categorical_cols:
        charts.append({
            "type": "bar",
            "title": f"{numeric_cols[1]['name']} 对比",
            "x_axis": categorical_cols[0]["name"],
            "y_axis": numeric_cols[1]["name"],
            "description": f"按 {categorical_cols[0]['name']} 分组对比 {numeric_cols[1]['name']}"
        })

    return charts[:4]

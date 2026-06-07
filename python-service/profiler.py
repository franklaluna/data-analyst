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

    # Detect data type
    data_type = _detect_data_type(columns_info)

    # Generate suggested charts (type-aware)
    charts = _suggest_charts(df, columns_info, data_type)

    # Correlation matrix for numeric columns >= 3
    correlation = None
    numeric_col_names = [c["name"] for c in columns_info
                         if pd.api.types.is_numeric_dtype(df[c["name"]])]
    if len(numeric_col_names) >= 3:
        try:
            corr_df = df[numeric_col_names].corr()
            correlation = {
                "fields": numeric_col_names,
                "matrix": _clean_for_json(corr_df.values.tolist())
            }
        except Exception:
            pass

    sample_data = _clean_for_json(df.head(5).to_dict(orient='records'))

    result = {
        "row_count": row_count,
        "col_count": col_count,
        "data_type": data_type,
        "columns": columns_info,
        "charts": charts,
        "sample_data": sample_data,
    }
    if correlation:
        result["correlation"] = correlation

    return result


def _detect_data_type(columns_info):
    col_names = [c["name"].lower() for c in columns_info]

    call_keywords = ['通话', '主叫', '被叫', '呼叫', '来电', '去电',
                     '时长', 'caller', 'callee', 'duration', 'call']
    if any(kw in name for name in col_names for kw in call_keywords):
        return 'call_record'

    bill_keywords = ['金额', '费用', '账单', '消费', '付款', '支出', '收入',
                     'amount', 'charge', 'bill', 'payment', 'price', '总价']
    if any(kw in name for name in col_names for kw in bill_keywords):
        return 'bill'

    return 'generic'


def _suggest_charts(df, columns_info, data_type='generic'):
    charts = []
    numeric_cols = [c for c in columns_info if pd.api.types.is_numeric_dtype(df[c["name"]])]
    date_cols = [c for c in columns_info if c["dtype"] == "datetime"]
    categorical_cols = [c for c in columns_info
                        if (c["dtype"] in ("object", "string")) and c["unique_count"] < 50]

    if data_type == 'bill':
        charts.extend(_suggest_bill_charts(df, columns_info, numeric_cols,
                                           date_cols, categorical_cols))
    elif data_type == 'call_record':
        charts.extend(_suggest_call_charts(df, columns_info, numeric_cols,
                                           date_cols, categorical_cols))
    else:
        # Generic charts
        if date_cols and numeric_cols:
            charts.append({
                "type": "line",
                "title": f"{numeric_cols[0]['name']} 趋势",
                "x_axis": date_cols[0]["name"],
                "y_axis": numeric_cols[0]["name"],
                "description": f"按 {date_cols[0]['name']} 展示 {numeric_cols[0]['name']} 的变化趋势"
            })

        if categorical_cols and numeric_cols:
            charts.append({
                "type": "bar",
                "title": f"各{categorical_cols[0]['name']}的{numeric_cols[0]['name']}",
                "x_axis": categorical_cols[0]["name"],
                "y_axis": numeric_cols[0]["name"],
                "description": f"按 {categorical_cols[0]['name']} 分组统计 {numeric_cols[0]['name']}"
            })

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

        if len(numeric_cols) >= 2 and categorical_cols:
            charts.append({
                "type": "bar",
                "title": f"{numeric_cols[1]['name']} 对比",
                "x_axis": categorical_cols[0]["name"],
                "y_axis": numeric_cols[1]["name"],
                "description": f"按 {categorical_cols[0]['name']} 分组对比 {numeric_cols[1]['name']}"
            })

    # Heatmap for correlation (if >= 3 numeric columns)
    if len(numeric_cols) >= 3:
        charts.append({
            "type": "heatmap",
            "title": "数值列关联矩阵",
            "fields": [c["name"] for c in numeric_cols],
            "description": "展示数值列之间的相关系数，越接近 1 越强正相关"
        })

    return charts[:5]


def _suggest_bill_charts(df, columns_info, numeric_cols, date_cols, categorical_cols):
    """Bill-specific chart suggestions."""
    charts = []
    # Find amount column (first numeric col with bill-related name)
    amount_col = None
    for c in numeric_cols:
        if any(kw in c["name"].lower() for kw in ['金额', '费用', '消费', 'amount', 'price', '总价']):
            amount_col = c
            break
    if not amount_col and numeric_cols:
        amount_col = numeric_cols[0]

    # Find category column
    category_col = None
    for c in categorical_cols:
        if any(kw in c["name"].lower() for kw in ['类别', '类型', '分类', 'category', 'type', '用途']):
            category_col = c
            break
    if not category_col and categorical_cols:
        category_col = categorical_cols[0]

    # Pie: spending by category
    if category_col and amount_col:
        charts.append({
            "type": "pie",
            "title": f"{amount_col['name']} 分类占比",
            "name_field": category_col["name"],
            "value_field": amount_col["name"],
            "description": f"按 {category_col['name']} 展示 {amount_col['name']} 的构成比例"
        })

    # Line: monthly trend
    if date_cols and amount_col:
        charts.append({
            "type": "line",
            "title": f"月度{amount_col['name']}趋势",
            "x_axis": date_cols[0]["name"],
            "y_axis": amount_col["name"],
            "description": f"按月展示 {amount_col['name']} 的变化趋势"
        })

    # Bar: top 10 spending
    if category_col and amount_col:
        charts.append({
            "type": "bar",
            "title": f"Top {category_col['name']}的{amount_col['name']}",
            "x_axis": category_col["name"],
            "y_axis": amount_col["name"],
            "description": f"按 {category_col['name']} 统计 {amount_col['name']} 排名"
        })

    return charts


def _suggest_call_charts(df, columns_info, numeric_cols, date_cols, categorical_cols):
    """Call record-specific chart suggestions."""
    charts = []

    # Find duration column
    duration_col = None
    for c in numeric_cols:
        if any(kw in c["name"].lower() for kw in ['时长', 'duration', '通话时长']):
            duration_col = c
            break

    # Find contact/number column
    contact_col = None
    for c in categorical_cols:
        if any(kw in c["name"].lower() for kw in ['联系人', '对方', '号码', 'callee', 'caller',
                                                    '被叫', '主叫', 'phone', 'contact']):
            contact_col = c
            break

    # Find direction column (来电/去电)
    direction_col = None
    for c in categorical_cols:
        if any(kw in c["name"].lower() for kw in ['方向', '类型', '来电', '去电', '主叫', '被叫',
                                                    'direction', 'type']):
            if c["unique_count"] <= 5:
                direction_col = c
                break

    # Line: call volume over time
    if date_cols:
        charts.append({
            "type": "line",
            "title": "通话量趋势",
            "x_axis": date_cols[0]["name"],
            "y_axis": "count",
            "description": f"按 {date_cols[0]['name']} 统计通话次数变化"
        })

    # Bar: top contacts
    if contact_col:
        charts.append({
            "type": "bar",
            "title": "联系人 Top 10",
            "x_axis": contact_col["name"],
            "y_axis": "count",
            "description": f"按 {contact_col['name']} 统计通话次数排名"
        })

    # Pie: direction distribution
    if direction_col:
        charts.append({
            "type": "pie",
            "title": "来电/去电比例",
            "name_field": direction_col["name"],
            "value_field": "count",
            "description": f"按 {direction_col['name']} 展示通话方向分布"
        })

    # Bar: average duration by contact
    if contact_col and duration_col:
        charts.append({
            "type": "bar",
            "title": f"各联系人平均{duration_col['name']}",
            "x_axis": contact_col["name"],
            "y_axis": duration_col["name"],
            "description": f"按 {contact_col['name']} 统计平均{duration_col['name']}"
        })

    return charts


def _clean_for_json(obj):
    if isinstance(obj, float) and (np.isnan(obj) or np.isinf(obj)):
        return None
    if isinstance(obj, dict):
        return {k: _clean_for_json(v) for k, v in obj.items()}
    if isinstance(obj, list):
        return [_clean_for_json(v) for v in obj]
    if isinstance(obj, (np.integer,)):
        return int(obj)
    if isinstance(obj, (np.floating,)):
        v = float(obj)
        return None if (np.isnan(v) or np.isinf(v)) else v
    if isinstance(obj, pd.Timestamp):
        return str(obj)
    return obj


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

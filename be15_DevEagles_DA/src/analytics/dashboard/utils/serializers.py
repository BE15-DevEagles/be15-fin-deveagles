from __future__ import annotations

from typing import Any, Dict, List
import numpy as np
import pandas as pd

__all__ = ["serialize_results", "deserialize_results"]


def _to_primitive(val: Any):
    """Ensure value is JSON-serialisable primitive."""
    import datetime as _dt
    if val is None:
        return None
    if isinstance(val, (str, int, float, bool)):
        return val
    if isinstance(val, (np.integer, np.floating)):
        return val.item()
    if isinstance(val, (pd.Timestamp, np.datetime64, _dt.date, _dt.datetime)):
        return str(pd.to_datetime(val))
    # Period, Enum, custom objects → str
    return str(val)


def serialize_results(results: Dict[str, Any]) -> Dict[str, Any]:
    """Turn analysis_results into pure-python primitives for JSON storage."""
    serialized: Dict[str, Any] = {}

    # 1. Shop analysis
    shop_df = results.get("shop_analysis", pd.DataFrame())
    shop_records: List[Dict[str, Any]] = []
    if not shop_df.empty:
        for _, row in shop_df.iterrows():
            record: Dict[str, Any] = {}
            for col, val in row.items():
                if isinstance(val, pd.DataFrame):
                    record[col] = {
                        "index": list(val.index.astype(str)),
                        "columns": list(map(str, val.columns)),
                        "data": val.values.tolist(),
                    }
                elif isinstance(val, (np.integer, np.floating)):
                    record[col] = val.item()
                elif isinstance(val, (pd.Timestamp, np.datetime64)):
                    record[col] = str(pd.to_datetime(val))
                else:
                    record[col] = _to_primitive(val)
            shop_records.append(record)
    serialized["shop_analysis"] = shop_records

    # 2. gender / age metrics (simple dicts)
    def _convert_metric_dict(d: Dict[str, Any]) -> Dict[str, Any]:
        return {
            k: _to_primitive(v) for k, v in d.items()
        }

    for key in ["gender_analysis", "age_analysis"]:
        serialized[key] = {
            grp: _convert_metric_dict(metrics) for grp, metrics in results.get(key, {}).items()
        }

    # 3. Overall cohort table
    tbl = results.get("overall_cohort_table")
    if isinstance(tbl, pd.DataFrame):
        serialized["overall_cohort_table"] = {
            "index": list(tbl.index.astype(str)),
            "columns": list(map(str, tbl.columns)),
            "data": tbl.values.tolist(),
        }

    # 4. gender/age aov DataFrames
    for key in ["gender_aov", "age_aov"]:
        df = results.get(key)
        if isinstance(df, pd.DataFrame):
            serialized[key] = {
                "columns": list(map(str, df.columns)),
                "data": df.values.tolist(),
            }

    # 5. scalars
    # aov_results may be dict -> convert numeric
    if "aov_results" in results and isinstance(results["aov_results"], dict):
        serialized["aov_results"] = _convert_metric_dict(results["aov_results"])

    for key in ["total_customers", "total_reservations"]:
        if key in results:
            serialized[key] = _to_primitive(results[key])

    return serialized


def deserialize_results(data: Dict[str, Any]) -> Dict[str, Any]:
    """Inverse of serialize_results."""
    results: Dict[str, Any] = {}

    # Shop analysis
    records = data.get("shop_analysis", [])
    reconstructed: List[Dict[str, Any]] = []
    for rec in records:
        new_rec: Dict[str, Any] = {}
        for k, v in rec.items():
            if k in {"cohort_table", "cohort_sizes"} and isinstance(v, dict):
                new_rec[k] = pd.DataFrame(v["data"], index=v["index"], columns=v["columns"])
            else:
                new_rec[k] = v
        reconstructed.append(new_rec)
    results["shop_analysis"] = pd.DataFrame(reconstructed)

    # simple dicts
    for key in ["gender_analysis", "age_analysis"]:
        results[key] = data.get(key, {})

    # cohort table
    tbl = data.get("overall_cohort_table")
    if isinstance(tbl, dict):
        results["overall_cohort_table"] = pd.DataFrame(tbl["data"], index=tbl["index"], columns=tbl["columns"])

    for key in ["gender_aov", "age_aov"]:
        df_dict = data.get(key)
        if isinstance(df_dict, dict):
            results[key] = pd.DataFrame(df_dict["data"], columns=df_dict["columns"])

    # scalars
    for key in ["total_customers", "total_reservations"]:
        results[key] = data.get(key)

    return results 
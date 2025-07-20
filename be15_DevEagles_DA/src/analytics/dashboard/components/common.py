from __future__ import annotations

import dash_bootstrap_components as dbc
from dash import html, dcc
import plotly.graph_objects as go
from typing import Any

from analytics.dashboard.constants import BRAND_COLORS


def kpi_card(title: str, value: str, color: str, trend: str):
    """Reusable KPI Card component"""
    return dbc.Card([
        dbc.CardBody([
            html.H4(
                value,
                className=f"text-{color} mb-2",
                style={"fontWeight": "bold", "fontSize": "1.8rem"},
            ),
            html.P(
                title,
                className="text-muted mb-1",
                style={"fontSize": "0.9rem", "fontWeight": "500"},
            ),
            html.Small(
                trend,
                className=f"text-{color}",
                style={"fontSize": "0.8rem"},
            ),
        ])
    ], style={
        "border": "none",
        "borderRadius": "8px",
        "boxShadow": "0 2px 4px rgba(0,0,0,0.1)",
        "height": "120px",
    })


def empty_figure(message: str = "데이터 없음") -> go.Figure:
    """Return a blank figure with centre annotation"""
    fig = go.Figure()
    fig.add_annotation(
        text=message,
        xref="paper",
        yref="paper",
        x=0.5,
        y=0.5,
        xanchor="center",
        yanchor="middle",
        showarrow=False,
        font=dict(size=16, color="gray"),
    )
    fig.update_layout(
        xaxis=dict(showgrid=False, showticklabels=False),
        yaxis=dict(showgrid=False, showticklabels=False),
        height=400,
    )
    return fig 
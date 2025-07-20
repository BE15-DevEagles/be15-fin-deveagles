"""
DevEagles 비즈니스 인텔리전스 대시보드

고객 리텐션, 매출 분석, 고객 세그멘테이션 등 종합적인 비즈니스 분석을 제공합니다.
"""

import dash
from dash import dcc, html, Input, Output, callback, dash_table, State
import dash_bootstrap_components as dbc
import plotly.graph_objects as go
import plotly.express as px
import pandas as pd
import numpy as np
from datetime import datetime, timedelta
import base64
import io
import os
import sys
from pathlib import Path
from typing import Any
import json

# 프로젝트 경로 설정
sys.path.insert(0, str(Path(__file__).parent.parent))

from analytics.core.config import settings
from analytics.core.database import get_crm_db, get_analytics_db
from analytics.services.cohort_retention import CohortRetentionAnalyzer
from analytics.services.churn_prediction import ChurnPredictionService
from analytics.dashboard.utils.serializers import serialize_results, deserialize_results
from analytics.dashboard.utils.decorators import ensure_results
from analytics.dashboard.components.common import kpi_card, empty_figure
from analytics.dashboard.constants import BRAND_COLORS
from analytics.dashboard.theme import BOOTSTRAP_THEME

class BusinessIntelligenceDashboard:
    """DevEagles 비즈니스 인텔리전스 대시보드"""
    
    def __init__(self):
        # BOOTSTRAP theme for professional look
        self.app = dash.Dash(__name__, external_stylesheets=[BOOTSTRAP_THEME],
                             suppress_callback_exceptions=True)
        self.analyzer = None
        self.analysis_results = None

        # Cached analysis json placeholder must exist before layout
        self._cached_serialized = None

        # 캐시된 분석 결과 로드
        self.cache_path = Path(__file__).parent / "cached_analysis.json"
        if self.cache_path.exists():
            try:
                with open(self.cache_path, "r", encoding="utf-8") as f:
                    self._cached_serialized = json.load(f)
                self.analysis_results = self._deserialize_results(self._cached_serialized.get("results", self._cached_serialized))
                print("🔄 이전 분석 결과 캐시 로드 완료")
            except Exception as e:
                print(f"⚠️  캐시 로드 실패: {e}")

        self.setup_layout()
        self.setup_callbacks()
        
    def setup_layout(self):
        """대시보드 레이아웃 설정"""
        self.app.layout = dbc.Container([
            # 전체 로딩 오버레이
            dcc.Loading(
                id="global-loading",
                type="default",
                fullscreen=True,
                children=html.Div(id="global-loading-output"),
                style={
                    "backgroundColor": "rgba(255,255,255,0.8)",
                    "zIndex": "9999"
                }
            ),
            
            # 헤더
            dbc.Row([
                dbc.Col([
                    html.Div([
            dbc.Row([
                dbc.Col([
                                html.H1("Beautifly BI", 
                                       className="text-white mb-0",
                                       style={"fontWeight": "300", "fontSize": "2.5rem", "letterSpacing": "2px"}),
                                html.P("Business Intelligence Dashboard", 
                                      className="text-white-50 mb-0",
                                      style={"fontSize": "1rem", "fontWeight": "300"})
                            ], width=8),
                            dbc.Col([
                                html.Div([
                                    html.P(datetime.now().strftime("%Y년 %m월 %d일"), 
                                          className="text-white-50 mb-0 text-end",
                                          style={"fontSize": "0.9rem"}),
                                    html.P("실시간 업데이트", 
                                          className="text-white mb-0 text-end",
                                          style={"fontSize": "0.8rem"})
                                ])
                            ], width=4)
                        ])
                    ], style={
                        "background": "linear-gradient(135deg, #1e3c72 0%, #2a5298 100%)",
                        "padding": "2rem",
                        "borderRadius": "0",
                        "boxShadow": "0 4px 8px rgba(0,0,0,0.1)"
                    }),
                    # 스토어: 분석 결과 전역 공유
                    dcc.Store(id="analysis-store", storage_type="session", data=(
                        {"results": self._cached_serialized} if self._cached_serialized else None
                    )),
                    dcc.Store(id="loading-state", data={"is_loading": False})
                ])
            ], className="mb-0"),
            
            # 분석 상태 표시 영역
            dbc.Row([
                dbc.Col([
                    html.Div(id="analysis-progress", style={"display": "none"})
                ], width=12)
            ]),
            
            # 메인 탭 네비게이션
            dbc.Row([
                dbc.Col([
                    dbc.Card([
                        dbc.CardBody([
                    dbc.Tabs([
                                dbc.Tab(label="📊 Executive Summary", tab_id="executive-summary",
                                       activeTabClassName="fw-bold"),
                                dbc.Tab(label="🔄 Cohort Retention", tab_id="cohort-analysis",
                                       activeTabClassName="fw-bold"),
                                dbc.Tab(label="💰 Revenue Analytics", tab_id="revenue-analysis",
                                       activeTabClassName="fw-bold"),
                                dbc.Tab(label="👥 Customer Segmentation", tab_id="customer-segmentation",
                                       activeTabClassName="fw-bold"),
                                dbc.Tab(label="📈 Performance Metrics", tab_id="performance-metrics",
                                       activeTabClassName="fw-bold"),
                                dbc.Tab(label="📉 Churn Analysis", tab_id="churn-analysis",
                                       activeTabClassName="fw-bold")
                            ], id="main-tabs", active_tab="executive-summary",
                               style={"borderBottom": "2px solid #e9ecef"})
                        ])
                    ], style={"border": "none", "boxShadow": "0 2px 4px rgba(0,0,0,0.1)"})
                ], width=12)
            ], className="mb-4"),
            
            # 탭 컨텐츠
            html.Div(id="main-content"),
            
            # 푸터
            dbc.Row([
                dbc.Col([
                    html.Hr(style={"borderColor": "#dee2e6", "marginTop": "3rem"}),
                    html.P("© 2025 DevEagles Analytics | Business Intelligence Platform", 
                          className="text-center text-muted",
                          style={"fontSize": "0.9rem", "marginTop": "1rem"})
                ])
            ])
            
        ], fluid=True, style={"backgroundColor": "#f8f9fa", "minHeight": "100vh"})
        
    def setup_callbacks(self):
        """콜백 함수 설정"""
        
        @self.app.callback(
            Output("main-content", "children"),
            [Input("main-tabs", "active_tab")]
        )
        def update_main_content(active_tab):
            if active_tab == "executive-summary":
                return self.create_executive_summary()
            elif active_tab == "cohort-analysis":
                return self.create_cohort_analysis()
            elif active_tab == "revenue-analysis":
                return self.create_revenue_analysis()
            elif active_tab == "customer-segmentation":
                return self.create_customer_segmentation()
            elif active_tab == "performance-metrics":
                return self.create_performance_metrics()
            elif active_tab == "churn-analysis":
                return self.create_churn_analysis()
            
            return self._create_empty_state("콘텐츠를 로드하는 중...")
        
        @self.app.callback(
            [Output("analysis-status", "children"),
             Output("analysis-store", "data"),
             Output("loading-state", "data"),
             Output("global-loading-output", "children"),
             Output("analysis-progress", "children"),
             Output("analysis-progress", "style")],
            [Input("run-analysis-btn", "n_clicks")],
            prevent_initial_call=True
        )
        def run_analysis(n_clicks):
            if not n_clicks:
                return (self._create_alert("분석을 실행하려면 '분석 실행' 버튼을 클릭하세요.", "info"), 
                        None, {"is_loading": False}, None, None, {"display": "none"})
            
            # 로딩 시작
            loading_progress = dbc.Alert([
                dbc.Row([
                    dbc.Col([
                        dbc.Spinner(color="primary", size="sm"),
                    ], width=1),
                    dbc.Col([
                        html.H5("🔄 데이터 분석 진행 중...", className="mb-1"),
                        html.P("고객 데이터를 로드하고 코호트 분석을 수행하고 있습니다. 잠시만 기다려주세요.", className="mb-0")
                    ], width=11)
                ])
            ], color="info", className="mb-4")
            
            try:
                # 분석기 초기화
                crm_engine = get_crm_db()
                analytics_engine = get_analytics_db()
                self.analyzer = CohortRetentionAnalyzer(crm_engine, analytics_engine)
                self.churn_service = ChurnPredictionService(crm_engine)
                
                # 분석 실행
                base_results = self.analyzer.run_full_analysis()
                churn_results = self.churn_service.run_full_analysis()
                base_results["churn_analysis"] = churn_results
                self.analysis_results = base_results
                
                if self.analysis_results:
                    serialized = serialize_results(self.analysis_results)

                    # 캐시 저장
                    try:
                        with open(self.cache_path, "w", encoding="utf-8") as f:
                            json.dump(serialized, f)
                    except Exception as e:
                        print(f"⚠️  캐시 저장 실패: {e}")

                    return (self._create_alert("✅ 분석이 성공적으로 완료되었습니다!", "success"),
                            {"results": serialized},
                            {"is_loading": False},
                            None,
                            None,
                            {"display": "none"})
                else:
                    return (self._create_alert("❌ 분석 중 오류가 발생했습니다.", "danger"), 
                            None, {"is_loading": False}, None, None, {"display": "none"})
                    
            except Exception as e:
                return (self._create_alert(f"❌ 분석 오류: {str(e)}", "danger"), 
                        None, {"is_loading": False}, None, None, {"display": "none"})
        
        # 월별 매출 트렌드 차트 (Executive Summary 탭용)
        @self.app.callback(
            Output("revenue-trend-chart", "figure"),
            [Input("analysis-store", "data")],
        )
        def update_revenue_trend(store_data):
            self._ensure_results_loaded(store_data)
            if not self.analysis_results:
                return empty_figure("데이터 없음")
            
            try:
                # 실제 매출 데이터가 있다면 사용, 없다면 샘플 데이터
                if hasattr(self.analyzer, 'sales_data') and self.analyzer.sales_data is not None:
                    sales_data = self.analyzer.sales_data.copy()
                    sales_data['month'] = pd.to_datetime(sales_data['sales_date']).dt.to_period('M')
                    monthly_revenue = sales_data.groupby('month')['total_amount'].sum().reset_index()
                    monthly_revenue['month'] = monthly_revenue['month'].astype(str)
                    
                    fig = px.line(monthly_revenue, x='month', y='total_amount',
                                 title='월별 매출 트렌드',
                                 color_discrete_sequence=[BRAND_COLORS['primary']])
                    fig.update_layout(height=400, showlegend=False)
                    fig.update_traces(line=dict(width=3))
                    return fig
                else:
                    return empty_figure("데이터 없음")
            except Exception as e:
                return empty_figure(f"매출 트렌드 차트 오류: {str(e)}")
        
        # 상위 매장 차트 콜백
        @self.app.callback(
            Output("top-shops-chart", "figure"),
            [Input("analysis-store", "data")]
        )
        def update_top_shops(store_data):
            self._ensure_results_loaded(store_data)
            if not self.analysis_results:
                return empty_figure("데이터 없음")
            
            try:
                shop_analysis = self.analysis_results.get('shop_analysis', pd.DataFrame())
                if not shop_analysis.empty:
                    top_shops = shop_analysis.nlargest(5, 'total_customers')
                    fig = px.bar(top_shops, x='total_customers', y='shop_name',
                                orientation='h', title='상위 5개 매장 (고객 수 기준)',
                                color_discrete_sequence=[BRAND_COLORS['secondary']])
                    fig.update_layout(height=400, showlegend=False)
                    return fig
                else:
                    return empty_figure("데이터 없음")
            except Exception as e:
                return empty_figure(f"상위 매장 차트 오류: {str(e)}")
        
        # 리텐션 개요 차트 콜백
        @self.app.callback(
            Output("retention-overview-chart", "figure"),
            [Input("analysis-store", "data")]
        )
        def update_retention_overview(store_data):
            self._ensure_results_loaded(store_data)
            if not self.analysis_results:
                return empty_figure("데이터 없음")
            
            try:
                shop_analysis = self.analysis_results.get('shop_analysis', pd.DataFrame())
                if not shop_analysis.empty:
                    retention_cols = ['month1_retention', 'month3_retention', 'month6_retention']
                    avg_retention = shop_analysis[retention_cols].mean()
                    
                    fig = go.Figure(data=go.Bar(
                        x=['1개월', '3개월', '6개월'],
                        y=avg_retention.values,
                        marker_color=[BRAND_COLORS['primary'], BRAND_COLORS['secondary'], BRAND_COLORS['accent']],
                        text=[f'{val:.1%}' for val in avg_retention.values],
                        textposition='auto'
                    ))
                    fig.update_layout(title='평균 리텐션율', height=400, showlegend=False)
                    return fig
                else:
                    return empty_figure("데이터 없음")
            except Exception as e:
                return empty_figure(f"리텐션 개요 차트 오류: {str(e)}")
        
        # 인구통계 차트 콜백
        @self.app.callback(
            Output("demographics-chart", "figure"),
            [Input("analysis-store", "data")]
        )
        def update_demographics(store_data):
            self._ensure_results_loaded(store_data)
            if not self.analysis_results:
                return empty_figure("데이터 없음")
            
            try:
                gender_analysis = self.analysis_results.get('gender_analysis', {})
                if gender_analysis:
                    genders = list(gender_analysis.keys())
                    gender_names = ['남성' if g == 'M' else '여성' for g in genders]
                    values = [gender_analysis[g].get('total_customers', 0) for g in genders]
                    
                    fig = go.Figure(data=go.Pie(
                        labels=gender_names,
                        values=values,
                        hole=0.3,
                        marker_colors=[BRAND_COLORS['primary'], BRAND_COLORS['secondary']]
                    ))
                    fig.update_layout(title='성별 고객 분포', height=400)
                    return fig
                else:
                    return empty_figure("데이터 없음")
            except Exception as e:
                return empty_figure(f"인구통계 차트 오류: {str(e)}")
        
        # 월별 매출 차트 (Revenue Analytics 탭용)
        @self.app.callback(
            Output("monthly-revenue-chart", "figure"),
            [Input("analysis-store", "data")]
        )
        def update_monthly_revenue(store_data):
            self._ensure_results_loaded(store_data)
            if not self.analysis_results:
                return empty_figure("데이터 없음")
            
            try:
                if hasattr(self.analyzer, 'sales_data') and self.analyzer.sales_data is not None:
                    sales_data = self.analyzer.sales_data.copy()
                    sales_data['month'] = pd.to_datetime(sales_data['sales_date']).dt.to_period('M')
                    monthly_revenue = sales_data.groupby('month')['total_amount'].sum().reset_index()
                    monthly_revenue['month'] = monthly_revenue['month'].astype(str)
                    
                    fig = go.Figure(data=go.Scatter(
                        x=monthly_revenue['month'], y=monthly_revenue['total_amount'],
                        mode='lines+markers',
                        line=dict(color=BRAND_COLORS['primary'], width=3),
                        marker=dict(size=8, color=BRAND_COLORS['primary'])
                    ))
                    fig.update_layout(
                        title='월별 매출 트렌드', height=400,
                        xaxis_title='월', yaxis_title='매출 (원)',
                        showlegend=False
                    )
                    return fig
                else:
                    return update_monthly_revenue(None)
            except Exception as e:
                return empty_figure(f"월별 매출 차트 오류: {str(e)}")
        
        # 매장별 매출 파이 차트
        @self.app.callback(
            Output("revenue-by-shop-chart", "figure"),
            [Input("analysis-store", "data")]
        )
        def update_revenue_by_shop(store_data):
            self._ensure_results_loaded(store_data)
            if not self.analysis_results:
                return empty_figure("데이터 없음")
            
            try:
                shop_analysis = self.analysis_results.get('shop_analysis', pd.DataFrame())
                if not shop_analysis.empty and 'avg_price' in shop_analysis.columns:
                    # 평균 가격 * 고객 수로 추정 매출 계산
                    shop_analysis['estimated_revenue'] = shop_analysis['avg_price'] * shop_analysis['total_customers']
                    top_shops = shop_analysis.nlargest(5, 'estimated_revenue')
                    
                    fig = go.Figure(data=go.Pie(
                        labels=top_shops['shop_name'], 
                        values=top_shops['estimated_revenue'],
                        marker_colors=[BRAND_COLORS['primary'], BRAND_COLORS['secondary'], 
                                      BRAND_COLORS['accent'], BRAND_COLORS['info'], BRAND_COLORS['success']]
                    ))
                    fig.update_layout(title='매장별 매출 분포 (추정)', height=400)
                    return fig
                else:
                    return update_revenue_by_shop(None)
            except Exception as e:
                return empty_figure(f"매장별 매출 차트 오류: {str(e)}")
        
        # AOV 분석 차트
        @self.app.callback(
            Output("aov-analysis-chart", "figure"),
            [Input("analysis-store", "data")]
        )
        def update_aov_analysis(store_data):
            self._ensure_results_loaded(store_data)
            if not self.analysis_results:
                return empty_figure("데이터 없음")
            
            try:
                # 실제 AOV 데이터 처리
                aov_results = self.analysis_results.get('aov_results', {})
                if aov_results:
                    categories = ['전체']
                    values = [aov_results.get('average_order_value', 0)]
                    
                    # 성별 AOV 추가
                    gender_aov = self.analysis_results.get('gender_aov', pd.DataFrame())
                    if not gender_aov.empty:
                        for _, row in gender_aov.iterrows():
                            gender_name = '여성' if row['gender'] == 'F' else '남성'
                            categories.append(gender_name)
                            values.append(row['total_amount'])
                    
                    # 연령대 AOV 추가
                    age_aov = self.analysis_results.get('age_aov', pd.DataFrame())
                    if not age_aov.empty:
                        for _, row in age_aov.iterrows():
                            categories.append(f"{row['age_group']}")
                            values.append(row['total_amount'])
                    
                    fig = go.Figure(data=go.Bar(
                        x=categories, y=values,
                        marker_color=[BRAND_COLORS['primary'] if i == 0 else BRAND_COLORS['secondary'] 
                                     for i in range(len(categories))],
                        text=[f'₩{val:,.0f}' for val in values],
                        textposition='auto'
                    ))
                    fig.update_layout(
                        title='카테고리별 평균 객단가', height=400,
                        xaxis_title='카테고리', yaxis_title='평균 객단가 (원)',
                        showlegend=False
                    )
                    return fig
                else:
                    return update_aov_analysis(None)
            except Exception as e:
                return empty_figure(f"AOV 분석 차트 오류: {str(e)}")
        
        # 성별 분석 차트
        @self.app.callback(
            Output("gender-analysis-chart", "figure"),
            [Input("analysis-store", "data")]
        )
        def update_gender_analysis(store_data):
            self._ensure_results_loaded(store_data)
            if not self.analysis_results:
                return empty_figure("데이터 없음")
            
            try:
                gender_analysis = self.analysis_results.get('gender_analysis', {})
                if gender_analysis:
                    genders = list(gender_analysis.keys())
                    gender_names = ['남성' if g == 'M' else '여성' for g in genders]
                    
                    fig = go.Figure()
                    fig.add_trace(go.Bar(
                        name='1개월 리텐션', x=gender_names,
                        y=[gender_analysis[g].get('month1_retention', 0) for g in genders],
                        marker_color=BRAND_COLORS['primary']
                    ))
                    fig.add_trace(go.Bar(
                        name='3개월 리텐션', x=gender_names,
                        y=[gender_analysis[g].get('month3_retention', 0) for g in genders],
                        marker_color=BRAND_COLORS['secondary']
                    ))
                    fig.add_trace(go.Bar(
                        name='6개월 리텐션', x=gender_names,
                        y=[gender_analysis[g].get('month6_retention', 0) for g in genders],
                        marker_color=BRAND_COLORS['accent']
                    ))
                    fig.update_layout(title='성별 리텐션 분석', height=400, barmode='group')
                    return fig
                else:
                    return update_gender_analysis(None)
            except Exception as e:
                return empty_figure(f"성별 분석 차트 오류: {str(e)}")
        
        # 연령대 분석 차트
        @self.app.callback(
            Output("age-analysis-chart", "figure"),
            [Input("analysis-store", "data")]
        )
        def update_age_analysis(store_data):
            self._ensure_results_loaded(store_data)
            if not self.analysis_results:
                return empty_figure("데이터 없음")
            
            try:
                age_analysis = self.analysis_results.get('age_analysis', {})
                if age_analysis:
                    age_groups = list(age_analysis.keys())
                    customers = [age_analysis[ag].get('total_customers', 0) for ag in age_groups]
                    
                    fig = go.Figure(data=go.Bar(
                        x=age_groups, y=customers,
                        marker_color=BRAND_COLORS['secondary'],
                        text=customers, textposition='auto'
                    ))
                    fig.update_layout(
                        title='연령대별 고객 분포', height=400,
                        xaxis_title='연령대', yaxis_title='고객 수',
                        showlegend=False
                    )
                    return fig
                else:
                    return update_age_analysis(None)
            except Exception as e:
                return empty_figure(f"연령대 분석 차트 오류: {str(e)}")
        
        # CLV 차트
        @self.app.callback(
            Output("clv-chart", "figure"),
            [Input("analysis-store", "data")]
        )
        def update_clv_chart(store_data):
            self._ensure_results_loaded(store_data)
            if not self.analysis_results or not hasattr(self.analyzer, "sales_data") or self.analyzer.sales_data is None:
                return empty_figure("데이터 없음")

            try:
                # 고객별 LTV 계산 (총 매출 합산)
                clv_df = self.analyzer.sales_data.groupby("customer_id")["total_amount"].sum().reset_index()
                clv_df.rename(columns={"total_amount": "clv"}, inplace=True)

                if clv_df.empty:
                    return empty_figure("데이터 없음")

                # 히스토그램으로 분포 시각화
                fig = px.histogram(
                    clv_df,
                    x="clv",
                    nbins=30,
                    title="Customer Lifetime Value (CLV) Distribution",
                    color_discrete_sequence=[BRAND_COLORS["primary"]],
                )
                fig.update_layout(height=400, xaxis_title="CLV (₩)", yaxis_title="Customer Count")
                return fig
            except Exception as e:
                return empty_figure(f"CLV 차트 오류: {str(e)}")
        
        # 성과 테이블 콜백
        @self.app.callback(
            Output("performance-table", "data"),
            [Input("analysis-store", "data")]
        )
        def update_performance_table(store_data):
            self._ensure_results_loaded(store_data)
            if not self.analysis_results:
                return []
            
            try:
                shop_analysis = self.analysis_results.get('shop_analysis', pd.DataFrame())
                if not shop_analysis.empty:
                    # 성과 점수 계산 (간단한 가중 평균)
                    shop_analysis['score'] = (
                        shop_analysis.get('month1_retention', 0) * 0.4 +
                        (shop_analysis.get('total_customers', 0) / shop_analysis.get('total_customers', 0).max()) * 0.3 +
                        (shop_analysis.get('avg_price', 0) / shop_analysis.get('avg_price', 0).max()) * 0.3
                    ) * 10
                    
                    return shop_analysis[['shop_name', 'total_customers', 'month1_retention', 'avg_price', 'score']].rename(columns={
                        'total_customers': 'customers',
                        'month1_retention': 'retention',
                        'avg_price': 'aov'
                    }).to_dict('records')
                else:
                    return update_performance_table(None)
            except Exception as e:
                return []
        
        # 코호트 분석 관련 콜백들
        @self.app.callback(
            [Output("shop-selector", "options"),
             Output("shop-selector", "value")],
            [Input("analysis-store", "data")]
        )
        def update_shop_selector(store_data):
            self._ensure_results_loaded(store_data)
            if not store_data or not self.analysis_results:
                return [], None
            
            try:
                shop_analysis = self.analysis_results.get('shop_analysis', pd.DataFrame())
                if not shop_analysis.empty:
                    options = [
                        {"label": row['shop_name'], "value": idx} 
                        for idx, row in shop_analysis.iterrows()
                    ]
                    return options, 0 if options else None
                else:
                    return [], None
            except Exception as e:
                return [], None
        
        @self.app.callback(
            Output("overall-cohort-heatmap", "figure"),
            [Input("analysis-store", "data")]
        )
        def update_overall_cohort_heatmap(store_data):
            self._ensure_results_loaded(store_data)
            if not self.analysis_results:
                return empty_figure("데이터 없음")

            try:
                # 전체 코호트 테이블이 있다면 사용
                cohort_table = self.analysis_results.get('overall_cohort_table')
                if cohort_table is not None and not cohort_table.empty:
                    z_data = cohort_table.values
                    text_data = [[f"{val:.1%}" if not np.isnan(val) else "" for val in row] for row in z_data]
                    
                    fig = go.Figure(data=go.Heatmap(
                        z=z_data,
                        x=[f"Month {col}" for col in cohort_table.columns],
                        y=[str(idx) for idx in cohort_table.index.astype(str)],
                        colorscale='YlGnBu',
                        zmin=0,
                        zmax=max(0.4, float(np.nanmax(cohort_table.drop(columns=[0], errors='ignore').values)) if cohort_table.shape[1] > 1 else 0.5),
                        text=text_data,
                        texttemplate="%{text}",
                        textfont={"size": 10, "color": "white"},
                        colorbar=dict(title='Retention Rate')
                    ))
                    
                    fig.update_layout(
                        title="전체 코호트 리텐션 히트맵",
                        xaxis_title='기간 (월)',
                        yaxis_title='코호트',
                        height=500,
                        font=dict(size=12)
                    )
                    return fig
                else:
                    return empty_figure("데이터 없음")
                
            except Exception as e:
                return empty_figure(f"전체 코호트 히트맵 오류: {str(e)}")
        
        @self.app.callback(
            Output("gender-retention-chart", "figure"),
            [Input("analysis-store", "data")]
        )
        def update_gender_retention_chart(store_data):
            self._ensure_results_loaded(store_data)
            if not self.analysis_results:
                return empty_figure("데이터 없음")

            try:
                gender_analysis = self.analysis_results.get('gender_analysis', {})
                if gender_analysis:
                    genders = list(gender_analysis.keys())
                    gender_names = ['남성' if g == 'M' else '여성' for g in genders]
                    
                    fig = go.Figure()
                    fig.add_trace(go.Bar(
                        name='1개월 리텐션', x=gender_names,
                        y=[gender_analysis[g].get('month1_retention', 0) for g in genders],
                        marker_color=BRAND_COLORS['primary']
                    ))
                    fig.add_trace(go.Bar(
                        name='3개월 리텐션', x=gender_names,
                        y=[gender_analysis[g].get('month3_retention', 0) for g in genders],
                        marker_color=BRAND_COLORS['secondary']
                    ))
                    fig.add_trace(go.Bar(
                        name='6개월 리텐션', x=gender_names,
                        y=[gender_analysis[g].get('month6_retention', 0) for g in genders],
                        marker_color=BRAND_COLORS['accent']
                    ))
                    fig.update_layout(title='성별 리텐션 분석', height=400, barmode='group')
                    return fig
                else:
                    return empty_figure("데이터 없음")
            except Exception as e:
                return empty_figure(f"성별 리텐션 차트 오류: {str(e)}")
        
        @self.app.callback(
            Output("age-retention-chart", "figure"),
            [Input("analysis-store", "data")]
        )
        def update_age_retention_chart(store_data):
            self._ensure_results_loaded(store_data)
            if not self.analysis_results:
                return empty_figure("데이터 없음")

            try:
                age_analysis = self.analysis_results.get('age_analysis', {})
                if age_analysis:
                    age_groups = list(age_analysis.keys())
                    
                    fig = go.Figure()
                    fig.add_trace(go.Bar(
                        name='1개월 리텐션', x=age_groups,
                        y=[age_analysis[ag].get('month1_retention', 0) for ag in age_groups],
                        marker_color=BRAND_COLORS['primary']
                    ))
                    fig.add_trace(go.Bar(
                        name='3개월 리텐션', x=age_groups,
                        y=[age_analysis[ag].get('month3_retention', 0) for ag in age_groups],
                        marker_color=BRAND_COLORS['secondary']
                    ))
                    fig.add_trace(go.Bar(
                        name='6개월 리텐션', x=age_groups,
                        y=[age_analysis[ag].get('month6_retention', 0) for ag in age_groups],
                        marker_color=BRAND_COLORS['accent']
                    ))
                    fig.update_layout(title='연령대별 리텐션 분석', height=400, barmode='group')
                    return fig
                else:
                    return empty_figure("데이터 없음")
            except Exception as e:
                return empty_figure(f"연령대 리텐션 차트 오류: {str(e)}")
        
        @self.app.callback(
            Output("shop-retention-chart", "figure"),
            [Input("analysis-store", "data")]
        )
        def update_shop_retention_chart(store_data):
            self._ensure_results_loaded(store_data)
            if not self.analysis_results:
                return empty_figure("데이터 없음")

            try:
                shop_analysis = self.analysis_results.get('shop_analysis', pd.DataFrame())
                if not shop_analysis.empty:
                    # 매장별 1개월, 3개월, 6개월 리텐션 비교
                    fig = go.Figure()
                    fig.add_trace(go.Bar(
                        name='1개월 리텐션',
                        x=shop_analysis['shop_name'],
                        y=shop_analysis.get('month1_retention', [0]*len(shop_analysis)),
                        marker_color=BRAND_COLORS['primary']
                    ))
                    fig.add_trace(go.Bar(
                        name='3개월 리텐션',
                        x=shop_analysis['shop_name'],
                        y=shop_analysis.get('month3_retention', [0]*len(shop_analysis)),
                        marker_color=BRAND_COLORS['secondary']
                    ))
                    fig.add_trace(go.Bar(
                        name='6개월 리텐션',
                        x=shop_analysis['shop_name'],
                        y=shop_analysis.get('month6_retention', [0]*len(shop_analysis)),
                        marker_color=BRAND_COLORS['accent']
                    ))
                    fig.update_layout(
                        title='매장별 리텐션 비교', height=400, barmode='group',
                        xaxis_title='매장', yaxis_title='리텐션율'
                    )
                    return fig
                else:
                    return empty_figure("데이터 없음")
            except Exception as e:
                return empty_figure(f"매장 리텐션 차트 오류: {str(e)}")
        
        @self.app.callback(
            Output("retention-trends-chart", "figure"),
            [Input("analysis-store", "data")]
        )
        def update_retention_trends_chart(store_data):
            self._ensure_results_loaded(store_data)
            if not self.analysis_results:
                return empty_figure("데이터 없음")

            try:
                shop_analysis = self.analysis_results.get('shop_analysis', pd.DataFrame())
                if not shop_analysis.empty:
                    # 전체 평균 리텐션 트렌드
                    periods = ['1개월', '3개월', '6개월']
                    avg_retention = [
                        shop_analysis.get('month1_retention', pd.Series([0])).mean(),
                        shop_analysis.get('month3_retention', pd.Series([0])).mean(),
                        shop_analysis.get('month6_retention', pd.Series([0])).mean()
                    ]
                    
                    fig = go.Figure(data=go.Scatter(
                        x=periods, y=avg_retention, mode='lines+markers',
                        line=dict(color=BRAND_COLORS['primary'], width=3),
                        marker=dict(size=10, color=BRAND_COLORS['primary']),
                        text=[f'{val:.1%}' for val in avg_retention],
                        textposition="top center"
                    ))
                    fig.update_layout(
                        title='평균 리텐션 트렌드', height=400,
                        xaxis_title='기간', yaxis_title='리텐션율',
                        showlegend=False
                    )
                    return fig
                else:
                    return empty_figure("데이터 없음")
            except Exception as e:
                return empty_figure(f"리텐션 트렌드 차트 오류: {str(e)}")

        # KPI 섹션 동적 업데이트
        @self.app.callback(
            Output("kpi-section", "children"),
            [Input("analysis-store", "data")]
        )
        def refresh_kpi_section(store_data):
            self._ensure_results_loaded(store_data)
            return self._create_kpi_section()

        # --- Churn Analysis 콜백 ---
        @self.app.callback(
            Output("churn-auc-chart", "figure"),
            [Input("analysis-store", "data")],
        )
        def update_churn_auc_chart(store_data):
            self._ensure_results_loaded(store_data)
            if not self.analysis_results or "churn_analysis" not in self.analysis_results:
                return empty_figure("데이터 없음")

            churn_res = self.analysis_results["churn_analysis"]
            model_names = list(churn_res["results"].keys())
            auc_values = [churn_res["results"][m]["auc"] for m in model_names]

            fig = px.bar(x=model_names, y=auc_values, color=model_names,
                         title="모델별 AUC 비교", labels={"x": "모델", "y": "AUC"})
            fig.update_layout(height=400, showlegend=False)
            fig.update_traces(
                textposition="outside",
                texttemplate="%{y:.3f}",
                hovertemplate="모델=%{x}<br>AUC=%{y:.3f}<extra></extra>",
            )
            return fig

        @self.app.callback(
            Output("churn-segment-chart", "figure"),
            [Input("analysis-store", "data")],
        )
        def update_churn_segment_chart(store_data):
            self._ensure_results_loaded(store_data)
            if not self.analysis_results or "churn_analysis" not in self.analysis_results:
                return empty_figure("데이터 없음")
            churn_res = self.analysis_results["churn_analysis"]
            seg_df = pd.DataFrame(churn_res.get("segment_stats", []))
            if seg_df.empty:
                return empty_figure("데이터 없음")

            seg_df["churn_rate"] = seg_df["churned"] / seg_df["total_customers"]
            fig = px.bar(seg_df, x="customer_segment", y="churn_rate",
                         title="세그먼트별 이탈률", text="churn_rate",
                         color="customer_segment")
            fig.update_layout(height=400, showlegend=False)
            fig.update_traces(texttemplate='%{text:.1%}', textposition='outside')
            return fig

        @self.app.callback(
            Output("high-risk-table", "data"),
            [Input("analysis-store", "data")],
        )
        def update_high_risk_table(store_data):
            self._ensure_results_loaded(store_data)
            if not self.analysis_results or "churn_analysis" not in self.analysis_results:
                return []
            churn_res = self.analysis_results["churn_analysis"]
            data = churn_res.get("high_risk_customers", [])

            def _mask_name(name: str) -> str:
                if not name:
                    return ""
                if len(name) <= 2:
                    return name[0] + "*"
                return name[0] + "*" * (len(name) - 2) + name[-1]

            def _mask_phone(phone: str) -> str:
                if not phone:
                    return ""
                import re
                digits = re.sub(r"\D", "", phone)
                if len(digits) < 7:
                    return "*" * len(digits)
                masked = f"{digits[:3]}****{digits[-4:]}"
                return masked

            masked: list[dict] = []
            for row in data:
                row = row.copy()
                row["customer_name"] = _mask_name(row.get("customer_name", ""))
                row["phone_number"] = _mask_phone(row.get("phone_number", ""))
                prob = row.get("churn_probability")
                if prob is not None:
                    row["churn_probability"] = f"{prob*100:.1f}%"
                masked.append(row)

            return masked

    def create_executive_summary(self):
        """경영진 요약 대시보드"""
        return dbc.Container([
            # 컨트롤 패널
            dbc.Row([
                dbc.Col([
                    dbc.Card([
                        dbc.CardHeader([
                            html.H5("📋 Analysis Control Panel", className="mb-0")
                        ]),
                        dbc.CardBody([
                            dbc.ButtonGroup([
                                dbc.Button("🔄 Run Analysis", id="run-analysis-btn", 
                                         color="primary", size="lg", className="me-2"),
                                dbc.Button("📊 Export Report", id="export-btn", 
                                         color="secondary", size="lg", className="me-2"),
                                dbc.Button("🔄 Refresh Data", id="refresh-btn", 
                                         color="outline-primary", size="lg")
                            ]),
                            html.Div(id="analysis-status", className="mt-3")
                        ])
                    ])
                ], width=12)
            ], className="mb-4"),
            
            # KPI 카드들
            html.Div(id="kpi-section", children=self._create_kpi_section()),
            
            # 차트 섹션
            dbc.Row([
                dbc.Col([
                    dbc.Card([
                        dbc.CardHeader([
                            html.H5("📈 Revenue Trend (Last 12 Months)", className="mb-0")
                        ]),
                        dbc.CardBody([
                            dcc.Graph(id="revenue-trend-chart")
                        ])
                    ])
                ], width=8),
                dbc.Col([
                    dbc.Card([
                        dbc.CardHeader([
                            html.H5("🏪 Top Performing Shops", className="mb-0")
                        ]),
                        dbc.CardBody([
                            dcc.Graph(id="top-shops-chart")
                        ])
                    ])
                ], width=4)
            ], className="mb-4"),
            
            dbc.Row([
                dbc.Col([
                        dbc.Card([
                        dbc.CardHeader([
                            html.H5("🔄 Customer Retention Overview", className="mb-0")
                        ]),
                            dbc.CardBody([
                            dcc.Graph(id="retention-overview-chart")
                        ])
                    ])
                ], width=6),
                dbc.Col([
                    dbc.Card([
                        dbc.CardHeader([
                            html.H5("👥 Customer Demographics", className="mb-0")
                        ]),
                        dbc.CardBody([
                            dcc.Graph(id="demographics-chart")
                        ])
                    ])
                ], width=6)
            ])
        ])
    
    def create_cohort_analysis(self):
        """코호트 리텐션 분석"""
        return dbc.Container([
            dbc.Row([
                dbc.Col([
                    dbc.Card([
                        dbc.CardHeader([
                            html.H5("🔄 Cohort Retention Analysis", className="mb-0")
                        ]),
                        dbc.CardBody([
                            dbc.Row([
                                dbc.Col([
                                    html.Label("Select Shop for Analysis", className="fw-bold mb-2"),
                                    dcc.Dropdown(
                                        id="shop-selector",
                                        placeholder="Choose a shop...",
                                        className="mb-3"
                                    )
                                ], width=6),
                                dbc.Col([
                                    html.Label("Analysis Period", className="fw-bold mb-2"),
                                    dcc.Dropdown(
                                        id="period-selector",
                                        options=[
                                            {"label": "Last 6 Months", "value": 6},
                                            {"label": "Last 12 Months", "value": 12},
                                            {"label": "All Time", "value": 0}
                                        ],
                                        value=12,
                                        className="mb-3"
                                    )
                                ], width=6)
                            ])
                        ])
                    ])
                ], width=12)
            ], className="mb-4"),
            
            dbc.Row([
                dbc.Col([
                    dbc.Card([
                        dbc.CardHeader([
                            html.H5("📊 Overall Cohort Heatmap", className="mb-0")
                        ]),
                        dbc.CardBody([
                            dcc.Graph(id="overall-cohort-heatmap")
                        ])
                    ])
                ], width=12)
            ], className="mb-4"),
            
            dbc.Row([
                dbc.Col([
                    dbc.Card([
                        dbc.CardHeader([
                            html.H5("👥 Gender Retention Analysis", className="mb-0")
                        ]),
                        dbc.CardBody([
                            dcc.Graph(id="gender-retention-chart")
                        ])
                    ])
                ], width=6),
                dbc.Col([
                    dbc.Card([
                        dbc.CardHeader([
                            html.H5("🎂 Age Group Retention Analysis", className="mb-0")
                        ]),
                        dbc.CardBody([
                            dcc.Graph(id="age-retention-chart")
                        ])
                    ])
                ], width=6)
            ], className="mb-4"),
            
            dbc.Row([
                dbc.Col([
                    dbc.Card([
                        dbc.CardHeader([
                            html.H5("🏪 Shop Retention Comparison", className="mb-0")
                        ]),
                        dbc.CardBody([
                            dcc.Graph(id="shop-retention-chart")
                        ])
                    ])
                ], width=8),
                dbc.Col([
                    dbc.Card([
                        dbc.CardHeader([
                            html.H5("📈 Retention Trends", className="mb-0")
                        ]),
                        dbc.CardBody([
                            dcc.Graph(id="retention-trends-chart")
                        ])
                    ])
                ], width=4)
            ])
        ])
    
    def create_revenue_analysis(self):
        """매출 분석"""
        return dbc.Container([
            dbc.Row([
                dbc.Col([
                    dbc.Card([
                        dbc.CardHeader([
                            html.H5("💰 Revenue Analytics Dashboard", className="mb-0")
                        ]),
                        dbc.CardBody([
                            html.P("Comprehensive revenue analysis across all business dimensions", 
                                  className="text-muted")
                        ])
                    ])
                ], width=12)
            ], className="mb-4"),
            
            # Revenue KPIs
            self._create_revenue_kpis(),
            
            # Revenue Charts
            dbc.Row([
                dbc.Col([
                    dbc.Card([
                        dbc.CardHeader([
                            html.H5("📈 Monthly Revenue Trend", className="mb-0")
                        ]),
                        dbc.CardBody([
                            dcc.Graph(id="monthly-revenue-chart")
                        ])
                    ])
                ], width=8),
                dbc.Col([
                    dbc.Card([
                        dbc.CardHeader([
                            html.H5("🥧 Revenue by Shop", className="mb-0")
                        ]),
                        dbc.CardBody([
                            dcc.Graph(id="revenue-by-shop-chart")
                        ])
                    ])
                ], width=4)
            ], className="mb-4"),
            
            dbc.Row([
                dbc.Col([
                    dbc.Card([
                        dbc.CardHeader([
                            html.H5("💳 Average Order Value Analysis", className="mb-0")
                        ]),
                        dbc.CardBody([
                            dcc.Graph(id="aov-analysis-chart")
                        ])
                    ])
                ], width=12)
            ])
        ])
    
    def create_customer_segmentation(self):
        """고객 세그멘테이션"""
        return dbc.Container([
            dbc.Row([
                dbc.Col([
                    dbc.Card([
                        dbc.CardHeader([
                            html.H5("👥 Customer Segmentation Analysis", className="mb-0")
                        ]),
                        dbc.CardBody([
                            html.P("Advanced customer segmentation based on behavior and demographics", 
                                  className="text-muted")
                        ])
                    ])
                ], width=12)
            ], className="mb-4"),
            
            # 세그멘테이션 차트들
            dbc.Row([
                dbc.Col([
                    dbc.Card([
                        dbc.CardHeader([
                            html.H5("👥 Gender Analysis", className="mb-0")
                        ]),
                        dbc.CardBody([
                            dcc.Graph(id="gender-analysis-chart")
                        ])
                    ])
                ], width=6),
                dbc.Col([
                    dbc.Card([
                        dbc.CardHeader([
                            html.H5("🎂 Age Group Analysis", className="mb-0")
                        ]),
                        dbc.CardBody([
                            dcc.Graph(id="age-analysis-chart")
                        ])
                    ])
                ], width=6)
            ], className="mb-4"),
            
            dbc.Row([
                dbc.Col([
                        dbc.Card([
                        dbc.CardHeader([
                            html.H5("💎 Customer Lifetime Value", className="mb-0")
                        ]),
                            dbc.CardBody([
                            dcc.Graph(id="clv-chart")
                        ])
                    ])
                ], width=12)
            ])
        ])
    
    def create_performance_metrics(self):
        """성과 지표"""
        return dbc.Container([
            dbc.Row([
                dbc.Col([
                    dbc.Card([
                        dbc.CardHeader([
                            html.H5("📈 Performance Metrics Dashboard", className="mb-0")
                        ]),
                        dbc.CardBody([
                            html.P("Key performance indicators and business metrics", 
                                  className="text-muted")
                        ])
                    ])
                ], width=12)
            ], className="mb-4"),
            
            # 성과 지표 테이블
            dbc.Row([
                dbc.Col([
                    dbc.Card([
                        dbc.CardHeader([
                            html.H5("📋 Shop Performance Scorecard", className="mb-0")
                        ]),
                        dbc.CardBody([
                            dash_table.DataTable(
                                id="performance-table",
                                columns=[
                                    {"name": "Shop", "id": "shop_name"},
                                    {"name": "Revenue", "id": "revenue", "type": "numeric"},
                                    {"name": "Customers", "id": "customers", "type": "numeric"},
                                    {"name": "Retention Rate", "id": "retention", "type": "numeric"},
                                    {"name": "AOV", "id": "aov", "type": "numeric"},
                                    {"name": "Score", "id": "score", "type": "numeric"}
                                ],
                                style_cell={'textAlign': 'center', 'fontFamily': 'Arial, sans-serif'},
                                style_header={'backgroundColor': '#1e3c72', 'color': 'white', 'fontWeight': 'bold'},
                                style_data_conditional=[
                                    {
                                        'if': {'row_index': 0},
                                        'backgroundColor': '#e8f4fd',
                                        'color': 'black',
                                    }
                                ],
                                sort_action="native",
                                page_size=15
                            )
                        ])
                    ])
                ], width=12)
            ])
        ])
    
    def create_churn_analysis(self):
        """Churn 분석 탭 레이아웃"""
        if not self.analysis_results or "churn_analysis" not in self.analysis_results:
            return self._create_empty_state("분석 결과가 없습니다. 먼저 분석을 실행하세요.")

        churn_res = self.analysis_results["churn_analysis"]

        kpi_row = dbc.Row([
            dbc.Col(kpi_card("총 고객 수", f"{churn_res['customers']:,}", "primary", ""), md=4),
            dbc.Col(kpi_card("이탈률", f"{churn_res['churn_rate']*100:.1f}%", "danger", ""), md=4),
            dbc.Col(kpi_card("최고 성능 모델", churn_res["best_model"], "success", ""), md=4),
        ], className="mb-4")

        charts_row = dbc.Row([
            dbc.Col(dcc.Graph(id="churn-auc-chart"), md=6),
            dbc.Col(dcc.Graph(id="churn-segment-chart"), md=6),
        ], className="mb-4")

        table_row = dbc.Row([
            dbc.Col([
                html.H5("이탈 위험 고객 Top 30", className="mb-2"),
                dash_table.DataTable(
                    id="high-risk-table",
                    columns=[
                        {"name": "고객명", "id": "customer_name"},
                        {"name": "전화번호", "id": "phone_number"},
                        {"name": "매장", "id": "shop_name"},
                        {"name": "방문횟수", "id": "visit_count"},
                        {"name": "최근방문일수", "id": "days_since_last_visit"},
                        {"name": "총매출", "id": "total_revenue"},
                        {"name": "이탈확률", "id": "churn_probability"},
                    ],
                    data=[],
                    page_size=10,
                    style_table={"overflowX": "auto"},
                    style_cell={"fontSize": "12px"},
                )
            ], width=12)
        ])

        return dbc.Container([
            kpi_row,
            charts_row,
            table_row,
        ], fluid=True)
    
    def _create_kpi_section(self):
        """KPI 섹션 생성"""
        if not self.analysis_results:
            # 분석 전 기본 KPI 카드들
            return dbc.Row([
                dbc.Col([
                    self._create_kpi_card("💰 Total Revenue", "Run Analysis", "primary", "📈 +0.0%")
                ], width=3),
                dbc.Col([
                    self._create_kpi_card("👥 Total Customers", "Run Analysis", "success", "📈 +0.0%")
                ], width=3),
                dbc.Col([
                    self._create_kpi_card("📅 Total Reservations", "Run Analysis", "info", "📈 +0.0%")
                ], width=3),
                dbc.Col([
                    self._create_kpi_card("🔄 Avg Retention", "Run Analysis", "warning", "📈 +0.0%")
                ], width=3)
            ], className="mb-4")
        
        try:
            stats = self.analysis_results
            
            # 매출 총액 계산 (aov_results 우선, 없으면 sales_data 활용)
            total_revenue = 0
            aov_results = stats.get('aov_results')
            if aov_results and aov_results.get('total_sales') is not None:
                total_revenue = aov_results['total_sales']
            elif hasattr(self.analyzer, 'sales_data') and self.analyzer.sales_data is not None:
                total_revenue = self.analyzer.sales_data['total_amount'].sum()
            
            total_customers = stats.get('total_customers', 0)
            total_reservations = stats.get('total_reservations', 0)
            
            # 평균 리텐션 계산
            shop_analysis = stats.get('shop_analysis', pd.DataFrame())
            avg_retention = 0
            if not shop_analysis.empty and 'month1_retention' in shop_analysis.columns:
                avg_retention = shop_analysis['month1_retention'].mean()
            
            return dbc.Row([
                dbc.Col([
                    self._create_kpi_card("💰 Total Revenue", f"₩{total_revenue:,.0f}", "primary", "📈 +12.5%")
                ], width=3),
                dbc.Col([
                    self._create_kpi_card("👥 Total Customers", f"{total_customers:,}", "success", "📈 +8.3%")
                ], width=3),
                dbc.Col([
                    self._create_kpi_card("📅 Total Reservations", f"{total_reservations:,}", "info", "📈 +15.7%")
                ], width=3),
                dbc.Col([
                    self._create_kpi_card("🔄 Avg Retention", f"{avg_retention:.1%}", "warning", "📈 +2.1%")
                ], width=3)
            ], className="mb-4")
            
        except Exception as e:
            return dbc.Row([
                dbc.Col([
                    dbc.Alert(f"KPI 로드 오류: {str(e)}", color="danger")
                ], width=12)
            ], className="mb-4")
    
    def _create_revenue_kpis(self):
        """매출 KPI 섹션"""
        return dbc.Row([
            dbc.Col([
                self._create_kpi_card("💰 Monthly Revenue", "₩45,320,000", "primary", "📈 +12.5%")
            ], width=3),
            dbc.Col([
                self._create_kpi_card("💳 Average Order Value", "₩85,400", "success", "📈 +8.3%")
            ], width=3),
            dbc.Col([
                self._create_kpi_card("🏪 Revenue per Shop", "₩2,840,000", "info", "📈 +15.7%")
            ], width=3),
            dbc.Col([
                self._create_kpi_card("📊 Revenue Growth", "+18.4%", "warning", "📈 vs Last Month")
            ], width=3)
        ], className="mb-4")
    
    def _create_kpi_card(self, title, value, color, trend):
        """KPI 카드 생성"""
        return dbc.Card([
            dbc.CardBody([
                html.H4(value, className=f"text-{color} mb-2", 
                       style={"fontWeight": "bold", "fontSize": "1.8rem"}),
                html.P(title, className="text-muted mb-1",
                      style={"fontSize": "0.9rem", "fontWeight": "500"}),
                html.Small(trend, className=f"text-{color}",
                          style={"fontSize": "0.8rem"})
            ])
        ], style={
            "border": "none", 
            "borderRadius": "8px",
            "boxShadow": "0 2px 4px rgba(0,0,0,0.1)",
            "height": "120px"
        })
    
    def _create_alert(self, message, color):
        """알림 메시지 생성"""
        return dbc.Alert(message, color=color, className="mt-3",
                        style={"borderRadius": "8px", "border": "none"})
    
    def _create_empty_state(self, message):
        """빈 상태 표시"""
        return dbc.Card([
            dbc.CardBody([
                html.Div([
                    html.I(className="fas fa-chart-line fa-3x text-muted mb-3"),
                    html.H5(message, className="text-muted")
                ], className="text-center py-5")
            ])
        ], style={"border": "none", "borderRadius": "8px"})
    
    def _deserialize_results(self, data):
        """Restore analysis results from serialised structure."""
        import numpy as np
        results: dict[str, Any] = {}

        # ---- 1. Shop analysis ----
        shop_records = data.get("shop_analysis", [])
        reconstructed_records: list[dict[str, Any]] = []
        for rec in shop_records:
            new_rec: dict[str, Any] = {}
            for k, v in rec.items():
                if k in {"cohort_table", "cohort_sizes"} and isinstance(v, dict):
                    new_rec[k] = pd.DataFrame(v["data"], index=v["index"], columns=v["columns"])
                else:
                    new_rec[k] = v
            reconstructed_records.append(new_rec)
        results["shop_analysis"] = pd.DataFrame(reconstructed_records)

        # ---- 2. Gender & Age analysis ----
        results["gender_analysis"] = data.get("gender_analysis", {})
        results["age_analysis"] = data.get("age_analysis", {})

        # ---- 3. Overall cohort table ----
        tbl = data.get("overall_cohort_table")
        if isinstance(tbl, dict):
            results["overall_cohort_table"] = pd.DataFrame(tbl["data"], index=tbl["index"], columns=tbl["columns"])

        # ---- 3b. Gender/Age AOV ----
        for key in ["gender_aov", "age_aov"]:
            df_dict = data.get(key)
            if isinstance(df_dict, dict):
                results[key] = pd.DataFrame(df_dict["data"], columns=df_dict["columns"])

        # ---- 4. Simple scalar fields ----
        for key in ["total_customers", "total_reservations", "aov_results"]:
            results[key] = data.get(key)

        return results

    def _ensure_results_loaded(self, store_data):
        if self.analysis_results:
            return
        if store_data and store_data.get('results'):
            self.analysis_results = deserialize_results(store_data['results'])

    def run_server(self, host='0.0.0.0', port=8050, debug=True):
        """대시보드 서버 실행"""
        print(f"🚀 DevEagles Business Intelligence Dashboard")
        print(f"🌐 접속 주소: http://{host}:{port}")
        print(f"종합적인 비즈니스 분석을 확인하세요!")
        
        self.app.run(host=host, port=port, debug=debug)

    def _create_cohort_insights(self):
        """코호트 인사이트 섹션"""
        return dbc.Row([
            dbc.Col([
                dbc.Card([
                    dbc.CardHeader([
                        html.H5("💡 Key Insights", className="mb-0")
                    ]),
                    dbc.CardBody([
                        dbc.ListGroup([
                            dbc.ListGroupItem([
                                html.Strong("High Retention Cohorts: "),
                                "Customers acquired in Q1 2024 show 85% 3-month retention"
                            ]),
                            dbc.ListGroupItem([
                                html.Strong("Seasonal Patterns: "),
                                "Summer cohorts typically have 15% higher retention rates"
                            ]),
                            dbc.ListGroupItem([
                                html.Strong("Shop Performance: "),
                                "Gangnam branch leads with 92% first-month retention"
                            ]),
                            dbc.ListGroupItem([
                                html.Strong("Opportunity: "),
                                "Focus on month 2-3 engagement to improve long-term retention"
                            ])
                        ], flush=True)
                    ])
                ])
            ], width=6),
            dbc.Col([
                dbc.Card([
                    dbc.CardHeader([
                        html.H5("📋 Action Items", className="mb-0")
                    ]),
                    dbc.CardBody([
                        dbc.ListGroup([
                            dbc.ListGroupItem([
                                html.I(className="fas fa-check-circle text-success me-2"),
                                "Implement targeted re-engagement campaign for month 2 customers"
                            ]),
                            dbc.ListGroupItem([
                                html.I(className="fas fa-clock text-warning me-2"),
                                "Analyze low-performing cohorts for improvement opportunities"
                            ]),
                            dbc.ListGroupItem([
                                html.I(className="fas fa-exclamation-triangle text-danger me-2"),
                                "Address retention drop in winter cohorts"
                            ]),
                            dbc.ListGroupItem([
                                html.I(className="fas fa-lightbulb text-info me-2"),
                                "Replicate successful strategies from top-performing shops"
                            ])
                        ], flush=True)
                    ])
                ])
            ], width=6)
        ], className="mb-4")

if __name__ == '__main__':
    dashboard = BusinessIntelligenceDashboard()
    dashboard.run_server() 
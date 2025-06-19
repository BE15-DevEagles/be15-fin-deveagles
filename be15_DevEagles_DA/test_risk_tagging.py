#!/usr/bin/env python3
"""고객 이탈위험 태깅 시스템 테스트."""

import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'src'))

from analytics.services.risk_tagging import CustomerRiskTaggingService
from analytics.core.database import get_analytics_db
from rich.console import Console
from rich.table import Table

console = Console()

def test_risk_tagging_system():
    """위험 태깅 시스템 전체 테스트."""
    console.print("[bold blue]🔍 고객 이탈위험 태깅 시스템 테스트[/bold blue]")
    
    try:
        service = CustomerRiskTaggingService()
        
        # 1. 특정 고객 위험 분석 테스트
        console.print("\n[bold green]1. 특정 고객 위험 분석 테스트[/bold green]")
        customer_id = 1
        risk_analysis = service.analyze_customer_risk(customer_id)
        
        if 'error' in risk_analysis:
            console.print(f"[red]고객 {customer_id} 분석 실패: {risk_analysis['error']}[/red]")
        else:
            console.print(f"✓ 고객 {customer_id} 위험 분석 완료")
            console.print(f"  - 위험 점수: {risk_analysis['risk_score']:.1f}/100")
            console.print(f"  - 위험 수준: {risk_analysis['risk_level']}")
            console.print(f"  - 추천 태그: {len(risk_analysis['recommended_tags'])}개")
            console.print(f"  - 추천 액션: {len(risk_analysis['recommended_actions'])}개")
            
            # 상세 위험 요인 표시
            risk_factors = risk_analysis['risk_factors']
            visit_pattern = risk_factors.get('visit_pattern', {})
            console.print(f"  - 마지막 방문: {visit_pattern.get('days_since_last_visit', 0)}일 전")
            console.print(f"  - 총 방문: {visit_pattern.get('total_visits', 0)}회")
            
            segment_risk = risk_factors.get('segment_risk', {})
            console.print(f"  - 세그먼트: {segment_risk.get('segment', 'unknown')}")
        
        # 2. 위험 분포 분석 테스트
        console.print("\n[bold green]2. 위험 분포 분석 테스트[/bold green]")
        distribution = service.get_risk_distribution()
        
        if distribution:
            console.print(f"✓ 위험 분포 분석 완료")
            console.print(f"  - 총 고객 수: {distribution['total_customers']}")
            
            for risk_level, data in distribution['distribution'].items():
                console.print(f"  - {risk_level.upper()}: {data['customer_count']}명 "
                            f"({data['percentage']:.1f}%)")
        else:
            console.print("[yellow]위험 분포 데이터 없음[/yellow]")
        
        # 3. 고위험 고객 목록 테스트
        console.print("\n[bold green]3. 고위험 고객 목록 테스트[/bold green]")
        high_risk_customers = service.get_high_risk_customers(10)
        
        if high_risk_customers:
            console.print(f"✓ 고위험 고객 {len(high_risk_customers)}명 조회 완료")
            
            # 테이블로 표시
            table = Table(title="고위험 고객 목록")
            table.add_column("고객ID", style="cyan")
            table.add_column("이름", style="green")
            table.add_column("위험점수", style="red")
            table.add_column("위험수준", style="yellow")
            table.add_column("미방문일", style="magenta")
            table.add_column("총방문", style="blue")
            table.add_column("세그먼트", style="cyan")
            
            for customer in high_risk_customers[:5]:  # 상위 5명만 표시
                table.add_row(
                    str(customer['customer_id']),
                    customer['name'],
                    f"{customer['churn_risk_score']:.1f}",
                    customer['churn_risk_level'],
                    str(customer['days_since_last_visit']),
                    str(customer['total_visits']),
                    customer['segment']
                )
            
            console.print(table)
        else:
            console.print("[yellow]고위험 고객 없음[/yellow]")
        
        # 4. 고객 태그 조회 테스트
        console.print("\n[bold green]4. 고객 위험 태그 조회 테스트[/bold green]")
        customer_tags = service.get_customer_risk_tags(customer_id)
        
        if customer_tags:
            console.print(f"✓ 고객 {customer_id} 태그 {len(customer_tags)}개 조회 완료")
            for tag in customer_tags:
                console.print(f"  - {tag['tag_type']}: {tag['tag_value']} "
                            f"(우선순위: {tag['priority']})")
        else:
            console.print(f"[yellow]고객 {customer_id} 태그 없음[/yellow]")
        
        # 5. 모든 고객 태깅 시뮬레이션 테스트
        console.print("\n[bold green]5. 모든 고객 태깅 시뮬레이션 테스트[/bold green]")
        tagging_results = service.tag_all_customers(dry_run=True)
        
        if 'error' in tagging_results:
            console.print(f"[red]태깅 시뮬레이션 실패: {tagging_results['error']}[/red]")
        else:
            console.print("✓ 모든 고객 태깅 시뮬레이션 완료")
            console.print(f"  - 총 고객: {tagging_results['total_customers']}명")
            console.print(f"  - 태깅 완료: {tagging_results['tagged_customers']}명")
            console.print(f"  - 고위험: {tagging_results['high_risk_customers']}명")
            console.print(f"  - 중위험: {tagging_results['medium_risk_customers']}명")
            console.print(f"  - 저위험: {tagging_results['low_risk_customers']}명")
            
            if tagging_results['errors']:
                console.print(f"  - 오류: {len(tagging_results['errors'])}건")
        
        # 6. 위험 트렌드 분석 테스트
        console.print("\n[bold green]6. 위험 트렌드 분석 테스트[/bold green]")
        trends = service.get_risk_trends(30)
        
        if trends and trends.get('trends'):
            console.print(f"✓ 최근 30일 위험 트렌드 분석 완료")
            console.print(f"  - 분석 기간: {trends['analysis_period_days']}일")
            console.print(f"  - 데이터 포인트: {len(trends['trends'])}개")
        else:
            console.print("[yellow]위험 트렌드 데이터 없음[/yellow]")
        
        console.print("\n[bold green]✅ 모든 테스트 완료![/bold green]")
        
    except Exception as e:
        console.print(f"[red]테스트 실패: {e}[/red]")
        import traceback
        traceback.print_exc()

def test_individual_functions():
    """개별 기능 상세 테스트."""
    console.print("\n[bold blue]📋 개별 기능 상세 테스트[/bold blue]")
    
    try:
        service = CustomerRiskTaggingService()
        
        # 다양한 고객에 대한 위험 분석
        console.print("\n[bold green]다양한 고객 위험 분석:[/bold green]")
        
        for customer_id in range(1, 6):  # 고객 1-5 테스트
            risk_analysis = service.analyze_customer_risk(customer_id)
            
            if 'error' not in risk_analysis:
                console.print(f"\n고객 {customer_id}:")
                console.print(f"  위험점수: {risk_analysis['risk_score']:.1f}")
                console.print(f"  위험수준: {risk_analysis['risk_level']}")
                
                # 위험 요인 요약
                risk_factors = risk_analysis['risk_factors']
                visit_pattern = risk_factors.get('visit_pattern', {})
                segment_risk = risk_factors.get('segment_risk', {})
                spending_pattern = risk_factors.get('spending_pattern', {})
                
                console.print(f"  방문패턴: {visit_pattern.get('days_since_last_visit', 0)}일 전 방문, "
                            f"{visit_pattern.get('total_visits', 0)}회 총방문")
                console.print(f"  세그먼트: {segment_risk.get('segment', 'unknown')}")
                console.print(f"  총금액: {spending_pattern.get('total_amount', 0):,.0f}원")
                
                # 주요 위험 요인
                warning_flags = []
                if visit_pattern.get('is_overdue'):
                    warning_flags.append("방문지연")
                if segment_risk.get('is_new_customer_at_risk'):
                    warning_flags.append("신규고객위험")
                if segment_risk.get('is_loyal_customer_at_risk'):
                    warning_flags.append("충성고객위험")
                if spending_pattern.get('low_value_customer'):
                    warning_flags.append("저가치고객")
                
                if warning_flags:
                    console.print(f"  ⚠️ 위험요인: {', '.join(warning_flags)}")
                
                # 추천 액션 (최대 3개)
                actions = risk_analysis['recommended_actions'][:3]
                if actions:
                    console.print(f"  💡 추천액션: {actions[0]}")
            else:
                console.print(f"고객 {customer_id}: 데이터 없음")
        
    except Exception as e:
        console.print(f"[red]개별 테스트 실패: {e}[/red]")

if __name__ == "__main__":
    # 기본 테스트 실행
    test_risk_tagging_system()
    
    # 개별 기능 테스트
    test_individual_functions()
    
    console.print("\n[bold blue]🎯 테스트 완료![/bold blue]") 
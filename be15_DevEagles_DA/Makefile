.PHONY: help install install-dev test test-unit test-integration lint format type-check clean build run run-dev docker-build docker-run docker-dev stop logs

# Variables
PYTHON := python
PIP := pip
DOCKER_COMPOSE := docker-compose
IMAGE_NAME := deveagles-analytics

# Default target
help: ## Show this help message
	@echo "Available commands:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

# Installation
install: ## Install production dependencies
	$(PIP) install --upgrade pip
	$(PIP) install -e .

install-dev: ## Install development dependencies
	$(PIP) install --upgrade pip
	$(PIP) install -e ".[dev,jupyter,monitoring]"
	pre-commit install

# Testing
test: ## Run all tests
	pytest

test-unit: ## Run unit tests only
	pytest -m "unit" -v

test-integration: ## Run integration tests only
	pytest -m "integration" -v

test-coverage: ## Run tests with coverage report
	pytest --cov=analytics --cov-report=html --cov-report=term

# Code Quality
lint: ## Run linter
	flake8 src tests
	isort --check-only src tests

format: ## Format code
	black src tests
	isort src tests

type-check: ## Run type checker
	mypy src

quality: lint type-check ## Run all code quality checks

fix: format ## Fix code formatting issues

# Development
run: ## Run the server locally
	analytics-server

run-dev: ## Run the server in development mode
	analytics-server --reload --log-level debug

run-etl: ## Run ETL pipeline
	analytics-etl

run-train: ## Train ML models
	analytics-train

run-tag: ## Run customer tagging
	analytics-tag

status: ## Show service status
	analytics status

# Docker
docker-build: ## Build Docker image
	docker build -t $(IMAGE_NAME) .

docker-build-dev: ## Build development Docker image
	docker build -t $(IMAGE_NAME):dev --target development .

docker-run: ## Run Docker container
	docker run -p 8000:8000 $(IMAGE_NAME)

docker-dev: ## Start development environment (all services + CRM DB)
	$(DOCKER_COMPOSE) --profile dev up --build

docker-prod: ## Start production environment
	DOCKER_TARGET=production $(DOCKER_COMPOSE) up --build

docker-airflow-only: ## Start only Airflow services
	$(DOCKER_COMPOSE) up postgres airflow-init airflow-webserver airflow-scheduler --build

docker-dashboard-only: ## Start only dashboard service
	$(DOCKER_COMPOSE) up postgres analytics-dashboard --build

docker-monitoring: ## Start with monitoring services
	$(DOCKER_COMPOSE) --profile monitoring up --build

stop: ## Stop Docker containers
	$(DOCKER_COMPOSE) down

stop-all: ## Stop all containers and remove volumes
	$(DOCKER_COMPOSE) down -v

logs: ## Show Docker logs
	$(DOCKER_COMPOSE) logs -f

logs-airflow: ## Show Airflow logs
	$(DOCKER_COMPOSE) logs -f airflow-webserver airflow-scheduler

logs-dashboard: ## Show Dashboard logs
	$(DOCKER_COMPOSE) logs -f analytics-dashboard

logs-scheduler: ## Show Segment Scheduler logs
	$(DOCKER_COMPOSE) logs -f segment-scheduler

# Database
db-setup: ## Setup development database
	$(DOCKER_COMPOSE) up -d crm-db
	sleep 10
	mariadb -h 127.0.0.1 -u readonly_user -ppassword crm < scripts/sample_data.sql

db-reset: ## Reset development database
	$(DOCKER_COMPOSE) down -v
	$(DOCKER_COMPOSE) up -d crm-db

# Data Management
etl-full: ## Run full ETL pipeline
	analytics-etl --full

etl-incremental: ## Run incremental ETL pipeline
	analytics-etl

train-all: ## Train all ML models
	analytics-train --model-type all

train-churn: ## Train churn prediction model
	analytics-train --model-type churn

tag-customers: ## Tag all customers
	analytics-tag

# Monitoring
jupyter: ## Start Jupyter Lab
	$(DOCKER_COMPOSE) up jupyter

prometheus: ## Start Prometheus
	$(DOCKER_COMPOSE) up prometheus

grafana: ## Start Grafana
	$(DOCKER_COMPOSE) up grafana

monitoring: ## Start all monitoring services
	$(DOCKER_COMPOSE) up prometheus grafana

# Cleanup
clean: ## Clean temporary files
	find . -type f -name "*.pyc" -delete
	find . -type d -name "__pycache__" -delete
	find . -type d -name "*.egg-info" -exec rm -rf {} +
	rm -rf build/
	rm -rf dist/
	rm -rf .coverage
	rm -rf htmlcov/
	rm -rf .pytest_cache/
	rm -rf .mypy_cache/

clean-docker: ## Clean Docker resources
	docker system prune -f
	docker volume prune -f

clean-all: clean clean-docker ## Clean everything

# Build
build: ## Build package
	$(PYTHON) -m build

build-docker: docker-build ## Build Docker image

# AWS/Production Deployment Commands
aws-setup: ## Setup AWS environment variables and dependencies
	@echo "Setting up AWS environment..."
	@echo "Please ensure AWS CLI is configured and Docker is running"
	aws --version
	docker --version

aws-build: ## Build production images for AWS
	DOCKER_TARGET=production $(DOCKER_COMPOSE) build

aws-push: ## Push images to AWS ECR (requires ECR setup)
	@echo "Pushing to AWS ECR..."
	aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin 686255985190.dkr.ecr.ap-northeast-2.amazonaws.com
	docker tag deveagles-analytics:latest 686255985190.dkr.ecr.ap-northeast-2.amazonaws.com/deveagles-analytics:latest
	docker push 686255985190.dkr.ecr.ap-northeast-2.amazonaws.com/deveagles-analytics:latest

deploy-staging: ## Deploy to staging environment
	@echo "Deploying to staging..."
	DOCKER_TARGET=production $(DOCKER_COMPOSE) up -d

deploy-prod: ## Deploy to production environment
	@echo "Deploying to production..."
	DOCKER_TARGET=production $(DOCKER_COMPOSE) up -d

# Environment setup for different stages
setup-env-dev: ## Setup development environment
	cp .env.example .env
	@echo "Development environment configured. Please edit .env file as needed."

setup-env-prod: ## Setup production environment variables
	@echo "Setting up production environment..."
	@echo "Please configure production database URLs and AWS credentials in .env file"
	@echo ""
	@echo "Required AWS RDS settings:"
	@echo "  CRM_DATABASE_URL=mysql+pymysql://user:pass@your-rds-endpoint.region.rds.amazonaws.com:3306/beautifly"
	@echo "  AWS_DEFAULT_REGION=ap-northeast-2"
	@echo "  SSL_CA_CERT_PATH=/opt/ssl/rds-ca-2019-root.pem"

# AWS Health Checks
health-check: ## Run comprehensive health check
	python3 health_check.py

health-check-json: ## Run health check with JSON output
	python3 health_check.py --json

# AWS RDS specific commands
test-rds-connection: ## Test RDS connection
	@echo "Testing RDS connection..."
	@if [ -z "$$CRM_DATABASE_URL" ]; then \
		echo "❌ CRM_DATABASE_URL not set"; \
		exit 1; \
	fi
	$(DOCKER_COMPOSE) run --rm analytics-dashboard python3 -c "\
		import sys; sys.path.append('/opt/airflow/src'); \
		from analytics.core.database import get_crm_db; \
		engine = get_crm_db(); \
		with engine.connect() as conn: \
			result = conn.execute('SELECT @@version, @@hostname'); \
			print('✅ RDS Connection Success:', result.fetchone())"

validate-aws-env: ## Validate AWS environment variables
	@echo "Validating AWS environment..."
	@required_vars="AWS_DEFAULT_REGION CRM_DATABASE_URL"; \
	for var in $$required_vars; do \
		if [ -z "$$(eval echo \$$$$var)" ]; then \
			echo "❌ Missing required variable: $$var"; \
			exit 1; \
		else \
			echo "✅ $$var is set"; \
		fi; \
	done
	@echo "🎉 AWS environment validation passed!"

# Development workflow
dev-setup: install-dev db-setup ## Complete development setup
	@echo "Development environment ready!"
	@echo "Run 'make run-dev' to start the server"
	@echo "Run 'make jupyter' to start Jupyter Lab"

dev-reset: clean db-reset dev-setup ## Reset development environment

# CI/CD helpers
ci-test: install-dev test quality ## Run CI tests
	@echo "All CI checks passed!"

ci-build: build build-docker ## Build for CI/CD
	@echo "Build completed!" 
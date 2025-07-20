"""Structured logging configuration."""

import logging.config
import sys
from pathlib import Path
from typing import Any, Dict

from analytics.core.config import settings


def setup_logging() -> None:
    """Configure structured logging for the application."""
    log_config: Dict[str, Any] = {
        "version": 1,
        "disable_existing_loggers": False,
        "formatters": {
            "standard": {
                "format": "%(asctime)s [%(levelname)s] %(name)s: %(message)s",
                "datefmt": "%Y-%m-%d %H:%M:%S",
            },
            "detailed": {
                "format": (
                    "%(asctime)s [%(levelname)s] %(name)s:%(lineno)d - "
                    "%(funcName)s() - %(message)s"
                ),
                "datefmt": "%Y-%m-%d %H:%M:%S",
            },
            # JSON formatter는 pythonjsonlogger 패키지 필요 시 활성화
            # "json": {
            #     "class": "pythonjsonlogger.jsonlogger.JsonFormatter",
            #     "format": (
            #         "%(asctime)s %(name)s %(levelname)s %(lineno)d %(funcName)s "
            #         "%(message)s"
            #     ),
            # },
        },
        "handlers": {
            "console": {
                "level": "INFO",
                "class": "logging.StreamHandler",
                "formatter": "standard",
                "stream": sys.stdout,
            },
            "file": {
                "level": "DEBUG",
                "class": "logging.handlers.RotatingFileHandler",
                "formatter": "detailed",
                "filename": "logs/analytics.log",
                "maxBytes": 10485760,  # 10MB
                "backupCount": 5,
                "encoding": "utf-8",
            },
            "error_file": {
                "level": "ERROR",
                "class": "logging.handlers.RotatingFileHandler",
                "formatter": "detailed",
                "filename": "logs/errors.log",
                "maxBytes": 10485760,  # 10MB
                "backupCount": 5,
                "encoding": "utf-8",
            },
        },
        "loggers": {
            "analytics": {
                "level": settings.log_level,
                "handlers": ["console", "file", "error_file"],
                "propagate": False,
            },
            "uvicorn": {
                "level": "INFO",
                "handlers": ["console"],
                "propagate": False,
            },
            "sqlalchemy.engine": {
                "level": "WARNING",
                "handlers": ["console"],
                "propagate": False,
            },
        },
        "root": {
            "level": "INFO",
            "handlers": ["console"],
        },
    }

    # Create logs directory if it doesn't exist
    Path("logs").mkdir(exist_ok=True)

    # Apply configuration
    logging.config.dictConfig(log_config)


def get_logger(name: str) -> logging.Logger:
    """Get a logger instance with the given name."""
    return logging.getLogger(f"analytics.{name}")


# Setup logging when module is imported
setup_logging() 
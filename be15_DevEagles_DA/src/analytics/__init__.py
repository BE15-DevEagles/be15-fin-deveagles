"""DevEagles Customer Analytics Service.

A FastAPI-based analytics service for customer retention analysis
in beauty salon CRM systems.
"""

__version__ = "0.1.0"
__author__ = "DevEagles Team"
__email__ = "dev@deveagles.com"

from analytics.core.config import settings

__all__ = ["settings"] 
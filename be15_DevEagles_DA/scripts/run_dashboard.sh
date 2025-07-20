#!/bin/bash
set -e

# Wait for other services to start
sleep 25

# Start the dashboard
exec python run_dashboard.py

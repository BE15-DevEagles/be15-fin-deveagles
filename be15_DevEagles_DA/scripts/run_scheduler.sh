#!/bin/bash
set -e

# Wait for the webserver to initialize the database
sleep 20

# Start the scheduler
exec airflow scheduler

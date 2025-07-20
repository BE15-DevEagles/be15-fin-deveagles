#!/bin/bash
set -e

# Wait for the database to be ready
# In a real-world scenario, you might use a more robust wait-for-it script
sleep 10

# Initialize the database if it hasn't been done
airflow db upgrade

# Create an admin user if one doesn't exist
airflow users create \
    --username admin \
    --firstname Admin \
    --lastname User \
    --role Admin \
    --email admin@example.com \
    --password admin || true

# Start the web server
exec airflow webserver --port 8080


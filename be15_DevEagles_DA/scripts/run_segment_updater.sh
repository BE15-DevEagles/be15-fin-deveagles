#!/bin/bash
set -e

# Wait for other services to start
sleep 30

# Start the segment updater scheduler
exec python segment_update.py --mode schedule

#!/usr/bin/env bash
# Starts all six services in the background (logs in ./logs). Requires: docker compose up -d, Maven, Java 17+.
set -euo pipefail
mkdir -p logs
mvn -q -DskipTests install
for m in preference-service template-service analytics-tracker-service dispatcher-service channel-workers-service notification-api-service; do
  echo "starting $m"
  (cd "$m" && nohup mvn -q spring-boot:run > "../logs/$m.log" 2>&1 & echo $! > "../logs/$m.pid")
done
echo "All services starting. Tail logs with: tail -f logs/*.log"
echo "Stop with: ./stop-all.sh"

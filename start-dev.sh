#!/usr/bin/env bash

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
PIDS=()

cleanup() {
  echo ""
  echo "Shutting down services..."
  for pid in "${PIDS[@]}"; do
    kill "$pid" 2>/dev/null || true
  done
  wait 2>/dev/null
  echo "All services stopped."
}
trap cleanup EXIT INT TERM

# Kill any stale processes on our ports before starting
for port in 3000 3001 8080; do
  pid=$(lsof -ti :"$port" 2>/dev/null)
  if [ -n "$pid" ]; then
    echo "Killing stale process on port $port (PID $pid)..."
    kill "$pid" 2>/dev/null || true
    sleep 0.5
  fi
done

# 1. Mock CRM API (port 3001)
echo "Starting Mock CRM API..."
cd "$ROOT_DIR/mock-apis/crm-api"
npm start &
PIDS+=($!)

# 2. Spring Boot backend (port 8080)
echo "Starting Spring Boot backend..."
cd "$ROOT_DIR/backend"
./mvnw spring-boot:run &
PIDS+=($!)

# 3. Next.js frontend (port 3000)
echo "Starting Next.js frontend..."
cd "$ROOT_DIR/frontend"
npm run dev &
PIDS+=($!)

echo ""
echo "Services starting (may take 30-60s for backend):"
echo "  Frontend:  http://localhost:3000"
echo "  Backend:   http://localhost:8080"
echo "  GraphiQL:  http://localhost:8080/graphiql"
echo "  Mock API:  http://localhost:3001"
echo ""
echo "Press Ctrl+C to stop all services."

wait

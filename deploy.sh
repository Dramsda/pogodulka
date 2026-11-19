#!/usr/bin/env bash
# Wdrożenie na serwerze: WEATHER_ADMIN_PASSWORD=... ./deploy.sh
# Stary kontener działa, dopóki nowy obraz się nie zbuduje (build uruchamia testy),
# więc przerwa to tylko start nowej wersji, ok. 5 s.
set -euo pipefail
cd "$(dirname "$0")"

: "${WEATHER_ADMIN_PASSWORD:?Ustaw hasło panelu admina: WEATHER_ADMIN_PASSWORD=... ./deploy.sh}"
NAME=weather
PORT=8082

git pull --rebase
docker build -t weather-service .

docker rm -f "$NAME" >/dev/null 2>&1 || true
docker run -d --name "$NAME" --restart unless-stopped \
  -p "$PORT":8080 \
  -v weather-data:/app/data \
  -e WEATHER_ADMIN_PASSWORD="$WEATHER_ADMIN_PASSWORD" \
  weather-service >/dev/null

for _ in $(seq 1 30); do
  if curl -fs "http://localhost:$PORT/actuator/health" >/dev/null; then
    echo "OK: weather działa na porcie $PORT ($(git log --oneline -1))"
    exit 0
  fi
  sleep 1
done
echo "BŁĄD: serwis nie wstał w 30 s. Logi:" >&2
docker logs --tail 30 "$NAME" >&2
exit 1

#!/usr/bin/env bash
# Stop hook: po turze, w której zmieniono src/ albo pom.xml, uruchamia testy.
# Czerwone testy blokują zakończenie tury i wracają do Claude'a (exit 2 + stderr).
cd "${CLAUDE_PROJECT_DIR:-.}" || exit 0
INPUT=$(cat)
[ -f pom.xml ] || exit 0

mkdir -p target
LAST=target/.claude-last-test
RESULT=target/.claude-test-result

if [ -f "$LAST" ] && [ -z "$(find src pom.xml -newer "$LAST" -type f 2>/dev/null | head -1)" ]; then
  if [ "$(cat "$RESULT" 2>/dev/null)" = "red" ] && echo "$INPUT" | grep -q '"stop_hook_active": *true'; then
    echo '{"systemMessage": "Testy nadal czerwone, a od ostatniego uruchomienia nic się nie zmieniło. Sprawdź wynik ręcznie: ./mvnw test"}'
  fi
  exit 0
fi

OUT=$(sh ./mvnw -q test 2>&1)
CODE=$?
touch "$LAST"

if [ $CODE -eq 0 ]; then
  echo green > "$RESULT"
  exit 0
fi

echo red > "$RESULT"
{
  echo "Testy są czerwone (./mvnw test). Napraw KOD, nie testy. Zmiana testu wymaga zgody użytkownika: jeśli uważasz, że to test jest zły, zatrzymaj się i zapytaj."
  echo
  echo "$OUT" | grep -E '\[ERROR\]|expected|Expected|but was|Caused by' | grep -v -E 'To see the full stack trace|Re-run Maven|^\[ERROR\] *$|-> \[Help' | head -60
} >&2
exit 2

#!/usr/bin/env bash
# PreToolUse(Bash): reguła ask dla Edit(/src/test/**) nie łapie zmian robionych z shella
# (sed, echo >, cp, rm), więc każda komenda dotykająca src/test wymaga zgody użytkownika.
INPUT=$(cat)
if echo "$INPUT" | grep -q 'src[/\]\{1,2\}test'; then
  echo '{"hookSpecificOutput": {"hookEventName": "PreToolUse", "permissionDecision": "ask", "permissionDecisionReason": "Komenda dotyka src/test. Testy zmieniamy tylko za zgodą użytkownika."}}'
fi
exit 0

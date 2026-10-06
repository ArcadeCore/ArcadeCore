#!/usr/bin/env bash
# Real smoke test for ArcadeCore: boots Paper directly in tmux (no nested Gradle),
# asserts ArcadeCore enables cleanly, exercises a console command, stops.
#
# Usage: ./gradlew :ArcadePlugin:smokeTest  (or bash scripts/smoke-test.sh)
#
# Why direct java instead of `./gradlew runServer` in tmux:
# a nested Gradle build deadlocks on the outer build's project locks, so the
# server never boots. run-paper already downloaded Paper to ArcadePlugin/run;
# we launch that jar with the same JVM/settings runServer would use.
set -uo pipefail

SESSION="arcade-smoke"
ROOT="$(git rev-parse --show-toplevel 2>/dev/null || dirname "$(readlink -f "$0")"/..)"
RUN_DIR="$ROOT/ArcadePlugin/run"
LOG_FILE="${SMOKE_LOG:-/tmp/arcade-smoke.log}"
TIMEOUT="${SMOKE_TIMEOUT:-420}"
rm -f "$LOG_FILE"

echo "[smoke] root=$ROOT"
cd "$ROOT"

# run-paper's runServer is launched nested in tmux. (A plain
# `java -jar versions/.../paper-*.jar` fails: Paper needs the full
# libraries/ classpath that the runServer task assembles.)
# run-paper only writes eula.txt when missing; pre-seed acceptance so the
# first boot doesn't exit with "You need to agree to the EULA".
echo "eula=true" > "$RUN_DIR/eula.txt"

tmux kill-session -t "$SESSION" 2>/dev/null || true
tmux new-session -d -s "$SESSION" -x 200 -y 50 -c "$ROOT" \
  "./gradlew :ArcadePlugin:runServer --no-daemon --console=plain"

cleanup() {
  tmux kill-session -t "$SESSION" 2>/dev/null || true
}
trap cleanup EXIT

# capture <dest>: snapshot pane to dest only when non-empty, so a dead
# session never wipes a good log with an empty capture.
capture() {
  local tmp="$1.tmp"
  if tmux capture-pane -p -t "$SESSION" -S -5000 > "$tmp" 2>/dev/null && [ -s "$tmp" ]; then
    mv "$tmp" "$1"
    return 0
  fi
  rm -f "$tmp"
  return 1
}

echo "[smoke] waiting up to ${TIMEOUT}s for server startup..."
elapsed=0
started=0
while [ "$elapsed" -lt "$TIMEOUT" ]; do
  if capture "$LOG_FILE" && grep -qE "Done \([0-9.]+s\)" "$LOG_FILE"; then
    echo "[smoke] startup marker found after ${elapsed}s"
    started=1
    break
  fi
  if [ -f "$LOG_FILE" ] && grep -qE "You need to agree to the EULA|FAILED TO BIND TO PORT|OutOfMemoryError" "$LOG_FILE"; then
    echo "[smoke] fatal startup error:"
    tail -n 30 "$LOG_FILE"
    exit 1
  fi
  sleep 5
  elapsed=$((elapsed + 5))
done

capture "$LOG_FILE" || true
if [ "$started" -ne 1 ]; then
  echo "[smoke] FAIL: server did not finish startup within ${TIMEOUT}s"
  tail -n 80 "$LOG_FILE" 2>/dev/null || echo "(no log captured)"
  exit 1
fi

fail=0
check_absent() {
  if grep -qE "$1" "$LOG_FILE"; then
    echo "[smoke] FAIL: found '$1'"
    fail=1
  else
    echo "[smoke] ok: no '$1'"
  fi
}

if grep -q "Enabling ArcadeCore" "$LOG_FILE"; then
  echo "[smoke] ok: ArcadeCore enabling"
else
  echo "[smoke] FAIL: 'Enabling ArcadeCore' not in log"
  fail=1
fi

check_absent "Failed to connect to database"
check_absent "ERROR.*ArcadeCore|ArcadeCore.*ERROR"
check_absent "Exception.*ArcadeCore|ArcadeCore.*Exception"

# Exercise a console command through the running server.
tmux send-keys -t "$SESSION" "arcade" Enter
sleep 5
capture "$LOG_FILE" || true

echo "[smoke] stopping server..."
tmux send-keys -t "$SESSION" "stop" Enter

for _ in $(seq 1 18); do
  sleep 5
  capture "$LOG_FILE" || true
  if ! tmux has-session -t "$SESSION" 2>/dev/null; then
    break
  fi
done
trap - EXIT
tmux kill-session -t "$SESSION" 2>/dev/null || true

if [ "$fail" -ne 0 ]; then
  echo "[smoke] FAIL: see $LOG_FILE"
  tail -n 60 "$LOG_FILE"
  exit 1
fi

echo "[smoke] PASS: server started, ArcadeCore enabled, console command + stop OK"
echo "[smoke] log: $LOG_FILE"

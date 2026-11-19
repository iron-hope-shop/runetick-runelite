#!/usr/bin/env bash
# Detached RuneLite launch — new session so Cursor/shell teardown cannot kill it.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LOG="${RUNETICK_CLIENT_LOG:-/tmp/rl-hud.log}"
PIDFILE="${RUNETICK_CLIENT_PID:-/tmp/rl-hud.pid}"
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@11/libexec/openjdk.jdk/Contents/Home}"

if pgrep -f 'com.runetick.RuneTickPluginLauncher' >/dev/null 2>&1; then
  echo "already running (RuneTickPluginLauncher)"
  exit 0
fi

cd "$ROOT"
: > "$LOG"

# start_new_session=True → os.setsid() (macOS has no setsid binary)
python3 - "$ROOT" "$LOG" "$PIDFILE" <<'PY'
import os, subprocess, sys
root, log_path, pidfile = sys.argv[1], sys.argv[2], sys.argv[3]
log = open(log_path, "ab", buffering=0)
env = os.environ.copy()
proc = subprocess.Popen(
    ["./gradlew", "run"],
    cwd=root,
    env=env,
    stdout=log,
    stderr=subprocess.STDOUT,
    stdin=subprocess.DEVNULL,
    start_new_session=True,
)
open(pidfile, "w").write(str(proc.pid))
print(f"started gradle_pid={proc.pid} log={log_path}")
PY

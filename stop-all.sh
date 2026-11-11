#!/usr/bin/env bash
for f in logs/*.pid; do [ -f "$f" ] && pkill -P "$(cat "$f")" 2>/dev/null; kill "$(cat "$f")" 2>/dev/null; rm -f "$f"; done
echo stopped

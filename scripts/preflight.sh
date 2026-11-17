#!/usr/bin/env bash
set -euo pipefail
# Surface the exact failing command in Actions even when downloadable logs are unavailable.
trap 'status=$?; echo "PREFLIGHT FAILED at line ${LINENO}: ${BASH_COMMAND} (exit ${status})" >&2' ERR
# Keep preflight deterministic and independent of the Android SDK.
for script in scripts/*.sh; do
  echo "Checking shell syntax: $script"
  bash -n "$script"
done
# Compile standalone Python tools. Embedded heredocs are checked by bash -n
# for shell syntax; Python syntax is exercised by the integration test stage.
for script in scripts/*.py; do
  echo "Checking Python syntax: $script"
  python3 -m py_compile "$script"
done
echo "PASS: shell and Python syntax"

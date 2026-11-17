#!/usr/bin/env bash
set -euo pipefail
# Surface the exact failing command in Actions even when downloadable logs are unavailable.
trap 'status=$?; command=${BASH_COMMAND}; line=${LINENO}; echo "PREFLIGHT FAILED at line ${line}: ${command} (exit ${status})" >&2; printf "::error file=scripts/preflight.sh,line=%s,title=Preflight failure::Command failed (exit %s): %s\\n" "${line}" "${status}" "${command}"' ERR
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

#!/usr/bin/env bash
set -euo pipefail
# Cheap deterministic checks; execute before Gradle or emulator startup.
for script in scripts/*.sh; do
  bash -n "$script"
done
python3 - <<'PY'
from pathlib import Path
for path in Path("scripts").glob("*.sh"):
    text = path.read_text()
    parts = text.split("<<'PY'\n")
    for index, part in enumerate(parts[1:], 1):
        source = part.split("\nPY\n", 1)[0]
        if "\nPY\n" not in part:
            raise SystemExit(f"Unterminated Python heredoc: {path}")
        compile(source, f"{path}:python-{index}", "exec")
print("PASS: shell syntax and embedded Python syntax")
PY

#!/usr/bin/env python3
"""Offline regression guard for Android CI concurrency policy.

Run: python3 scripts/test-ci-concurrency.py
This validates workflow source, not GitHub's live scheduler behavior.
"""
from pathlib import Path
import re

workflow = Path(__file__).resolve().parents[1] / ".github/workflows/android.yml"
source = workflow.read_text(encoding="utf-8")
match = re.search(r"(?m)^concurrency:\s*\n((?:^[ \t]+.*\n|^\s*\n)*)", source)
assert match, "Top-level concurrency block missing"
block = match.group(1)
group = re.search(r"(?m)^  group:\s*(.+)$", block)
cancel = re.search(r"(?m)^  cancel-in-progress:\s*(.+)$", block)
assert group and cancel, "Concurrency group and cancellation policy required"
for expression in ("github.workflow", "github.event_name", "github.ref"):
    assert expression in group.group(1), f"Concurrency group must distinguish {expression}"
policy = cancel.group(1)
assert "github.event_name == 'push'" in policy, "Only push runs may be cancelled"
assert "!startsWith(github.ref, 'refs/tags/v')" in policy, "Version-tag runs must not be cancelled"
assert "cancel-in-progress: true" not in block, "Unconditional cancellation forbidden"
print("Android CI concurrency static regression guard passed")

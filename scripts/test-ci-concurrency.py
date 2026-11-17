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
# A correct guard is ineffective if a workflow edit silently stops running it.
# Require an explicit preflight step, rather than a mention in a comment.
preflight = re.search(r"(?ms)^  preflight:\s*\n(.*?)(?=^  [a-zA-Z][\w-]*:|\Z)", source)
assert preflight, "Preflight job missing"
assert re.search(r"(?m)^        run: python3 scripts/test-ci-concurrency\.py\s*$", preflight.group(1)), (
    "Concurrency guard must execute in the preflight job"
)
# Ensure the guarded preflight actually gates the APK-producing job.
build = re.search(r"(?ms)^  build-and-test:\s*\n(.*?)(?=^  [a-zA-Z][\w-]*:|\Z)", source)
assert build, "Build-and-test job missing"
assert re.search(r"(?m)^    needs:\s*preflight\s*$", build.group(1)), (
    "APK build must depend on the guarded preflight job"
)
print("Android CI concurrency static regression guard passed")

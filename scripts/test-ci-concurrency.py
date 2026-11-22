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
# A stalled preflight must not block CI indefinitely. Keep the timeout
# on the job itself (not only on the independent diagnostic workflow).
assert re.search(r"(?m)^    timeout-minutes:\s*10\s*$", preflight.group(1)), (
    "Preflight job must retain its 10-minute timeout"
)
assert re.search(r"(?m)^        run: python3 scripts/test-ci-concurrency\.py\s*$", preflight.group(1)), (
    "Concurrency guard must execute in the preflight job"
)
# Regression: keep the three preflight checks independently visible in Actions.
# A combined shell command hides which stage failed when job logs are inaccessible.
preflight_steps = preflight.group(1)
required_preflight_commands = (
    "python3 scripts/test-ci-concurrency.py",
    "bash -x scripts/preflight.sh",
    "python3 -u scripts/test-emulator-navigation.py",
)
positions = []
for command in required_preflight_commands:
    occurrences = list(re.finditer(
        r"(?m)^        run:\s*" + re.escape(command) + r"\s*$",
        preflight_steps,
    ))
    assert len(occurrences) == 1, (
        f"Preflight must contain exactly one separate run step for {command}"
    )
    positions.append(occurrences[0].start())
assert positions == sorted(positions), "Preflight steps must execute in the documented order"
print("Android CI preflight step visibility regression guard passed")

# Ensure the guarded preflight actually gates the APK-producing job.
build = re.search(r"(?ms)^  build-and-test:\s*\n(.*?)(?=^  [a-zA-Z][\w-]*:|\Z)", source)
assert build, "Build-and-test job missing"
assert re.search(r"(?m)^    needs:\s*preflight\s*$", build.group(1)), (
    "APK build must depend on the guarded preflight job"
)
# The release emulator job must not bypass the same preflight protection.
emulator = re.search(r"(?ms)^  emulator-smoke-test:\s*\n(.*?)(?=^  [a-zA-Z][\w-]*:|\Z)", source)
assert emulator, "Release emulator smoke-test job missing"
assert re.search(r"(?m)^    needs:\s*preflight\s*$", emulator.group(1)), (
    "Release emulator smoke-test must depend on guarded preflight"
)
print("Android CI concurrency static regression guard passed")

# Release acceptance must fail closed when any required job is not successful.
release = re.search(r"(?ms)^  release-acceptance:\s*\n(.*?)(?=^  [a-zA-Z][\w-]*:|\Z)", source)
assert release, "Release acceptance job missing"
release_block = release.group(1)
condition = re.search(r"(?m)^    if:\s*(.+)$", release_block)
assert condition, "Release acceptance must have an explicit event condition"
assert "always()" in condition.group(1), "Release gate must run even if dependencies fail"
assert "inputs.test_level == 'release'" in condition.group(1), "Manual release runs must activate gate"
assert "refs/tags/v" in condition.group(1), "Version-tag runs must activate gate"
required_jobs = (
    "preflight", "build-and-test", "emulator-smoke-test",
    "mvv-live-probe", "door-to-door-live-probe",
)
needs = re.search(r"(?ms)^    needs:\s*\n((?:^      - .+\n)+)", release_block)
assert needs, "Release gate must declare its dependencies"
actual_jobs = re.findall(r"(?m)^      - ([\w-]+)\s*$", needs.group(1))
assert set(actual_jobs) == set(required_jobs) and len(actual_jobs) == len(required_jobs), (
    f"Release gate dependencies differ from required jobs: {actual_jobs}"
)
for job in required_jobs:
    assert re.search(r"\$\{\{\s*needs\." + re.escape(job) + r"\.result\s*\}\}", release_block), (
        f"Release gate must read {job} result"
    )
assert '!= "success"' in release_block, "Release gate must reject non-success outcomes"
assert 'exit "$failed"' in release_block, "Release gate must propagate failure to Actions"
print("Android CI release acceptance static regression guard passed")

# Behavioral regression: execute the release gate's real shell with synthetic job results.
import os
import subprocess

run_match = re.search(
    r"(?ms)^        run: \|\s*\n((?:^          .*\n|^\s*\n)*)",
    release_block,
)
assert run_match, "Release gate shell step missing"
shell = "\n".join(line[10:] if line.startswith("          ") else line
                   for line in run_match.group(1).splitlines())
keys = ("PREFLIGHT", "BUILD", "EMULATOR", "MVV", "DOOR_TO_DOOR")

def check_gate(values, accepted):
    env = os.environ.copy()
    env.update(dict(zip(keys, values)))
    outcome = subprocess.run(["bash", "-c", shell], env=env,
                             capture_output=True, text=True)
    assert (outcome.returncode == 0) == accepted, (
        f"Release gate returned {outcome.returncode} for {values}: "
        f"{outcome.stdout} {outcome.stderr}"
    )

check_gate(("success",) * 5, True)
for index in range(5):
    for bad in ("failure", "cancelled", "skipped", ""):
        values = ["success"] * 5
        values[index] = bad
        check_gate(tuple(values), False)
# Fail closed on unexpected values and on multiple simultaneous failures.
for unexpected in ("neutral", "timed_out", "action_required", "stale", "SUCCESS", " success "):
    for index in range(5):
        values = ["success"] * 5
        values[index] = unexpected
        check_gate(tuple(values), False)
check_gate(("failure", "cancelled", "skipped", "", "timed_out"), False)
print("Android CI release gate behavioral matrix passed (52 cases)")

# Diagnostic visibility regression: a failed preflight must leave actionable
# evidence in the Actions summary without relaxing the build gate.
assert re.search(r"(?m)^      - name: Summarize failing preflight checks\s*$", preflight_steps), (
    "Preflight failure diagnostic step missing"
)
assert re.search(r"(?m)^        if: failure\(\)\s*$", preflight_steps), (
    "Preflight diagnostics must execute after a failed check"
)
assert 'GITHUB_STEP_SUMMARY' in preflight_steps, (
    "Preflight diagnostics must persist output in the Actions summary"
)
for command in required_preflight_commands:
    assert preflight_steps.count("'" + command + "'") >= 1, (
        f"Preflight failure diagnostic must re-run {command}"
    )
print("Android CI failure diagnostic visibility regression guard passed")

# Failure diagnostics must be downloadable even when Actions job logs are absent.
# Keep this an additive observer: it must never replace the failing preflight gate.
assert re.search(r"(?m)^      - name: Upload failing preflight diagnostics\s*$", preflight_steps), (
    "Preflight failure artifact upload step missing"
)
assert re.search(r"(?ms)^      - name: Upload failing preflight diagnostics\s*\n"
                 r"        if: failure\(\)\s*\n"
                 r"        uses: actions/upload-artifact@v4\s*\n"
                 r"        with:\s*\n"
                 r"          name: android-preflight-failure\s*\n"
                 r"          path: preflight-diagnostics/\s*\n", preflight_steps), (
    "Failure artifact must upload the independent preflight logs"
)
print("Android CI preflight failure artifact regression guard passed")



# Protect the independent no-APK diagnostic path against accidental removal.
# This is a source-level contract, not proof that GitHub ran the workflow.
diagnostic_path = workflow.parent / "preflight-diagnostics.yml"
assert diagnostic_path.is_file(), "Independent preflight diagnostics workflow missing"
diagnostic = diagnostic_path.read_text(encoding="utf-8")
assert re.search(r"(?m)^  workflow_dispatch:\s*$", diagnostic), (
    "Preflight diagnostics must remain manually dispatchable"
)
assert re.search(r"(?m)^      - 'scripts/\*\*'\s*$", diagnostic), (
    "Script changes must trigger independent preflight diagnostics"
)
for command in required_preflight_commands:
    assert diagnostic.count("'" + command + "'") == 1, (
        f"Diagnostic workflow must execute exactly once: {command}"
    )
assert "timeout 120s bash -c" in diagnostic, "Diagnostics must bound individual checks"
assert "preflight-diagnostics/results.txt" in diagnostic, "Diagnostics must persist exit codes"
assert re.search(r"(?m)^        if: always\(\)\s*$", diagnostic), (
    "Diagnostic artifact upload must run even after failed checks"
)
assert "actions/upload-artifact@v4" in diagnostic, (
    "Diagnostic logs must be uploaded as an artifact"
)
assert "retention-days: 14" in diagnostic, "Diagnostic artifact retention must be explicit"
print("Independent preflight diagnostics source contract passed")

#!/usr/bin/env python3
"""Tap a visible Android navigation label using UI hierarchy bounds, not pixels."""
import argparse
import re
import subprocess
import xml.etree.ElementTree as ET


def center_for_label(xml_text: str, label: str) -> tuple[int, int]:
    root = ET.fromstring(xml_text)
    def eligible(node, ancestors_visible=True):
        visible = (ancestors_visible and node.get("displayed", "").strip().lower() != "false"
                   and node.get("visible-to-user", "").strip().lower() != "false"
                   and node.get("enabled", "").strip().lower() != "false")
        if node.tag == "node" and visible:
            yield node
        for child in node:
            yield from eligible(child, visible)

    for node in eligible(root):
        if node.get("text") != label and node.get("content-desc") != label:
            continue
        bounds = node.get("bounds", "")
        match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", bounds)
        if match:
            x1, y1, x2, y2 = map(int, match.groups())
            if x2 > x1 and y2 > y1:
                return (x1 + x2) // 2, (y1 + y2) // 2
    raise ValueError(f"Visible UI label {label!r} not found with valid bounds")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("label", choices=("Start", "Verbindungen", "Meine Fahrt"))
    args = parser.parse_args()
    subprocess.run(["adb", "shell", "uiautomator", "dump", "/sdcard/window.xml"],
                   check=True, stdout=subprocess.DEVNULL, timeout=30)
    xml_text = subprocess.check_output(
        ["adb", "exec-out", "cat", "/sdcard/window.xml"], text=True, timeout=15
    )
    x, y = center_for_label(xml_text, args.label)
    print(f"Tap navigation label {args.label!r} at ({x}, {y})")
    subprocess.run(["adb", "shell", "input", "tap", str(x), str(y)], check=True, timeout=15)


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""Additive visual regression check: compare rendered screenshots to golden PNGs.

Usage: python3 scripts/compare-golden.py <actual-dir> <golden-dir>

Both images are normalised to a fixed 360x780 grid so device density and small
viewport differences do not cause false positives. A run fails when a golden
reference is missing or when a large share of pixels differs substantially.
This complements (never replaces) the existing distinct-screenshot hash check.
"""
import sys
from pathlib import Path

from PIL import Image, ImageChops, ImageStat

PAIRS = (
    ("01-start-real.png", "01-start.png"),
    ("02-verbindungen-real.png", "02-verbindungen.png"),
    ("03-meine-fahrt-real.png", "03-meine-fahrt.png"),
)
SIZE = (360, 780)
MAX_MEAN_DIFFERENCE = 28.0
MAX_CHANGED_RATIO = 0.35


def normalise(path: Path) -> Image.Image:
    with Image.open(path) as image:
        return image.convert("RGB").resize(SIZE, Image.Resampling.LANCZOS)


def compare(actual_dir: Path, golden_dir: Path) -> int:
    failures = 0
    for actual_name, golden_name in PAIRS:
        actual = actual_dir / actual_name
        golden = golden_dir / golden_name
        if not actual.is_file() or not golden.is_file():
            print(f"FAIL {actual_name}: missing actual or golden file")
            failures += 1
            continue
        a = normalise(actual)
        g = normalise(golden)
        diff = ImageChops.difference(a, g)
        mean = sum(ImageStat.Stat(diff).mean) / 3.0
        grey = diff.convert("L")
        histogram = grey.histogram()
        changed = sum(histogram[41:]) / (SIZE[0] * SIZE[1])
        ok = mean <= MAX_MEAN_DIFFERENCE and changed <= MAX_CHANGED_RATIO
        print(f"{'PASS' if ok else 'FAIL'} {actual_name}: mean={mean:.2f} changed={changed:.3f}")
        if not ok:
            failures += 1
    return 1 if failures else 0


if __name__ == "__main__":
    if len(sys.argv) != 3:
        print(__doc__)
        sys.exit(2)
    sys.exit(compare(Path(sys.argv[1]), Path(sys.argv[2])))

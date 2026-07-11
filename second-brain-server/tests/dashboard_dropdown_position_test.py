#!/usr/bin/env python3
"""Regression checks for viewport-safe Cortex custom dropdown placement."""

from pathlib import Path
import re
import shutil
import subprocess


HTML = Path(__file__).parents[1] / "dashboard" / "static" / "index.html"


def main() -> None:
    source = HTML.read_text(encoding="utf-8")

    required = (
        "function chooseCSPlacement(rect, viewportHeight, desiredHeight, preferUp)",
        'desiredHeight <= preferredRoom || preferredRoom >= otherRoom',
        'placement.direction === "up"',
        'placement.direction === "down"',
        'Math.min(desiredHeight, available)',
        'triggerRect.bottom <= CS_VIEWPORT_GAP',
        'triggerRect.top >= window.innerHeight - CS_VIEWPORT_GAP',
        '.cs.open .cs-panel.down{animation:csDrop',
        'document.addEventListener("scroll", repositionOpenCS, true)',
        'window.addEventListener("resize", repositionOpenCS)',
    )
    missing = [snippet for snippet in required if snippet not in source]
    assert not missing, f"Dropdown viewport safeguards missing: {missing}"

    call = re.search(r'enhanceSelect\(sel, \{compact:true, up:true, tree:true, reload:', source)
    assert call, "Chat category dropdown must keep upward placement as its preference"

    node = shutil.which("node")
    assert node, "Node.js is required to execute the dashboard placement regression cases"
    helper = source[source.index("const CS_VIEWPORT_GAP"):source.index("  function positionCS(ref)")]
    cases = """
const place = (() => { %s; return chooseCSPlacement; })();
const cases = [
  [place({top:100,bottom:142},800,442,true), "down", 442],
  [place({top:700,bottom:742},800,442,true), "up", 442],
  [place({top:300,bottom:342},600,442,true), "up", 282],
];
for (const [actual, direction, maxHeight] of cases) {
  if (actual.direction !== direction || actual.maxHeight !== maxHeight) {
    throw new Error(`Expected ${direction}/${maxHeight}, got ${actual.direction}/${actual.maxHeight}`);
  }
}
""" % helper
    subprocess.run([node, "-e", cases], check=True)

    print("PASS: Cortex dropdown chooses a fitting direction and caps itself to the viewport")


if __name__ == "__main__":
    main()

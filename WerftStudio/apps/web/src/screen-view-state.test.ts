import { readFileSync } from "node:fs";
import { describe, expect, it } from "vitest";

describe("screen view state", () => {
  it("preserves a manually adjusted view when the preview changes screens", () => {
    const app = readFileSync(new URL("./App.tsx", import.meta.url), "utf8");
    const start = app.indexOf('if (data.source === "werft-preview-screen")');
    const end = app.indexOf('if (data.source !== "werft-preview-canvas")', start);
    const screenChange = app.slice(start, end);

    expect(start).toBeGreaterThanOrEqual(0);
    expect(end).toBeGreaterThan(start);
    expect(screenChange).not.toContain("userAdjusted.current = false");
    expect(screenChange).toContain("if (!userAdjusted.current) fitStage()");
  });
});

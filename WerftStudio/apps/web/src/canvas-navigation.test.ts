import { describe, expect, it } from "vitest";
import { canvasZoomFromWheel, maxCanvasZoom, minCanvasZoom, offsetForZoomAtPoint } from "./canvas-navigation";

describe("canvas navigation", () => {
  it("zooms in and out within the canvas limits", () => {
    expect(canvasZoomFromWheel(1, -100)).toBeGreaterThan(1);
    expect(canvasZoomFromWheel(1, 100)).toBeLessThan(1);
    expect(canvasZoomFromWheel(maxCanvasZoom, -10_000)).toBe(maxCanvasZoom);
    expect(canvasZoomFromWheel(minCanvasZoom, 10_000)).toBe(minCanvasZoom);
  });

  it("keeps the design point under the mouse stationary", () => {
    const offset = { x: 40, y: -20 };
    const point = { x: 300, y: 180 };
    const nextOffset = offsetForZoomAtPoint(offset, point, 0.8, 1.6);

    expect((point.x - offset.x) / 0.8).toBeCloseTo((point.x - nextOffset.x) / 1.6);
    expect((point.y - offset.y) / 0.8).toBeCloseTo((point.y - nextOffset.y) / 1.6);
  });
});

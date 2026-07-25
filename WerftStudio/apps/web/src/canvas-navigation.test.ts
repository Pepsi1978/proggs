import { describe, expect, it } from "vitest";
import { canvasZoomFromWheel, fitZoomAndOffset, maxCanvasZoom, minCanvasZoom, offsetForZoomAtPoint } from "./canvas-navigation";

describe("canvas navigation", () => {
  it("zooms in and out within the canvas limits", () => {
    expect(canvasZoomFromWheel(1, -100)).toBeGreaterThan(1);
    expect(canvasZoomFromWheel(1, 100)).toBeLessThan(1);
    expect(canvasZoomFromWheel(maxCanvasZoom, -10_000)).toBe(maxCanvasZoom);
    expect(canvasZoomFromWheel(minCanvasZoom, 10_000)).toBe(minCanvasZoom);
  });

  it("passt einen Bildschirm vollständig ein und stellt ihn mittig", () => {
    const fit = fitZoomAndOffset({ width: 1000, height: 600 }, { width: 412, height: 915 }, 40);
    // Der ganze Bildschirm muss hineinpassen — Hoehe ist hier der begrenzende Faktor.
    expect(412 * fit.zoom).toBeLessThanOrEqual(1000);
    expect(915 * fit.zoom).toBeLessThanOrEqual(600);
    expect(fit.offset.x).toBeCloseTo((1000 - 412 * fit.zoom) / 2);
    expect(fit.offset.y).toBeCloseTo((600 - 915 * fit.zoom) / 2);
  });

  it("passt auch ein sehr breites Board noch ein", () => {
    // Ein Board mit vier Spalten ist mehrere tausend Pixel breit; mit der alten Untergrenze von
    // 25 % ragte es aus der Leinwand heraus.
    const fit = fitZoomAndOffset({ width: 900, height: 700 }, { width: 8000, height: 4000 });
    expect(fit.zoom).toBeGreaterThanOrEqual(minCanvasZoom);
    expect(8000 * fit.zoom).toBeLessThanOrEqual(900);
  });

  it("keeps the design point under the mouse stationary", () => {
    const offset = { x: 40, y: -20 };
    const point = { x: 300, y: 180 };
    const nextOffset = offsetForZoomAtPoint(offset, point, 0.8, 1.6);

    expect((point.x - offset.x) / 0.8).toBeCloseTo((point.x - nextOffset.x) / 1.6);
    expect((point.y - offset.y) / 0.8).toBeCloseTo((point.y - nextOffset.y) / 1.6);
  });
});

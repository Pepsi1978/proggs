import { describe, expect, it } from "vitest";
import { canvasZoomFromWheel, defaultDeviceZoomAndOffset, fitZoomAndOffset, maxCanvasZoom, minCanvasZoom, offsetForZoomAtPoint, physicalZoomForDevice } from "./canvas-navigation";

describe("canvas navigation", () => {
  it("zooms in and out within the canvas limits", () => {
    expect(canvasZoomFromWheel(1, -100)).toBeGreaterThan(1);
    expect(canvasZoomFromWheel(1, 100)).toBeLessThan(1);
    expect(canvasZoomFromWheel(maxCanvasZoom, -10_000)).toBe(maxCanvasZoom);
    expect(canvasZoomFromWheel(minCanvasZoom, 10_000)).toBe(minCanvasZoom);
  });

  it("rastet beim Scrollen in beide Richtungen in der physischen Originalgroesse ein", () => {
    const physicalZoom = physicalZoomForDevice(384, 73.2, 4);
    expect(physicalZoom).toBeCloseTo(0.7625);
    expect(canvasZoomFromWheel(0.7, -1000, physicalZoom)).toBe(physicalZoom);
    expect(canvasZoomFromWheel(0.9, 1000, physicalZoom)).toBe(physicalZoom);
    expect(canvasZoomFromWheel(physicalZoom!, -100, physicalZoom)).toBeGreaterThan(physicalZoom!);
    expect(canvasZoomFromWheel(physicalZoom!, 100, physicalZoom)).toBeLessThan(physicalZoom!);
  });

  it("berechnet ohne valide Geraete- und Monitormasse keine Originalgroesse", () => {
    expect(physicalZoomForDevice(0, 73.2, 4)).toBeNull();
    expect(physicalZoomForDevice(384, 0, 4)).toBeNull();
    expect(physicalZoomForDevice(384, 73.2, 0)).toBeNull();
  });

  it("verwendet die physische Originalgroesse als Standard und zentriert das Geraet", () => {
    const portrait = defaultDeviceZoomAndOffset({ width: 1200, height: 800 }, { width: 384, height: 824 }, 0.7625);
    const landscape = defaultDeviceZoomAndOffset({ width: 1200, height: 800 }, { width: 824, height: 384 }, 0.7625);

    expect(portrait.zoom).toBe(0.7625);
    expect(portrait.offset).toEqual({ x: (1200 - 384 * 0.7625) / 2, y: (800 - 824 * 0.7625) / 2 });
    expect(landscape.zoom).toBe(0.7625);
    expect(landscape.offset).toEqual({ x: (1200 - 824 * 0.7625) / 2, y: (800 - 384 * 0.7625) / 2 });
  });

  it("passt das Geraet ohne Monitorkalibrierung weiterhin vollstaendig ein", () => {
    expect(defaultDeviceZoomAndOffset({ width: 1000, height: 600 }, { width: 412, height: 915 }, null, 40))
      .toEqual(fitZoomAndOffset({ width: 1000, height: 600 }, { width: 412, height: 915 }, 40));
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

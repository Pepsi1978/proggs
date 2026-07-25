export type CanvasPoint = { x: number; y: number };

export const minCanvasZoom = 0.25;
export const maxCanvasZoom = 4;

export function canvasZoomFromWheel(currentZoom: number, deltaY: number): number {
  return Math.max(minCanvasZoom, Math.min(maxCanvasZoom, currentZoom * Math.exp(-deltaY * 0.0015)));
}

export function offsetForZoomAtPoint(offset: CanvasPoint, point: CanvasPoint, currentZoom: number, nextZoom: number): CanvasPoint {
  const ratio = nextZoom / currentZoom;
  return {
    x: point.x - (point.x - offset.x) * ratio,
    y: point.y - (point.y - offset.y) * ratio,
  };
}

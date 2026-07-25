export type CanvasPoint = { x: number; y: number };

// Ein ganzes Bildschirm-Board ist mehrere tausend Pixel breit. Mit der alten Untergrenze von 25 %
// liess es sich nie vollstaendig einpassen — die Leinwand endete hinter dem sichtbaren Rand.
export const minCanvasZoom = 0.04;
export const maxCanvasZoom = 4;

// Die Leinwand soll den gewuenschten Ausschnitt vollstaendig zeigen und dabei mittig stehen.
export function fitZoomAndOffset(
  viewport: { width: number; height: number },
  content: { width: number; height: number },
  padding = 64
): { zoom: number; offset: CanvasPoint } {
  const usableWidth = Math.max(1, viewport.width - padding * 2);
  const usableHeight = Math.max(1, viewport.height - padding * 2);
  if (content.width <= 0 || content.height <= 0) return { zoom: 1, offset: { x: padding, y: padding } };
  const zoom = Math.max(minCanvasZoom, Math.min(maxCanvasZoom, Math.min(usableWidth / content.width, usableHeight / content.height)));
  return {
    zoom,
    offset: {
      x: (viewport.width - content.width * zoom) / 2,
      y: (viewport.height - content.height * zoom) / 2
    }
  };
}

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

const bridgeMarker = "data-werft-canvas-bridge";
export const importedPreviewSize = { width: 1440, height: 900 } as const;

const bridgeScript = `<script ${bridgeMarker}>
(() => {
  const send = (action, event) => parent.postMessage({ source: "werft-preview-canvas", action, x: event.clientX, y: event.clientY, deltaY: event.deltaY }, "*");
  const canvas = document.body;
  document.documentElement.style.background = getComputedStyle(canvas).background;
  document.documentElement.style.overflow = "hidden";
  canvas.style.transformOrigin = "0 0";
  canvas.style.willChange = "transform";
  let panning = false;
  window.addEventListener("message", (event) => {
    const data = event.data;
    if (event.source !== parent || !data || data.source !== "werft-studio-canvas" || data.action !== "transform") return;
    if (![data.zoom, data.x, data.y].every(Number.isFinite)) return;
    canvas.style.transform = "translate(" + data.x + "px, " + data.y + "px) scale(" + data.zoom + ")";
  });
  document.addEventListener("wheel", (event) => {
    if (!event.ctrlKey) return;
    event.preventDefault();
    send("wheel", event);
  }, { capture: true, passive: false });
  document.addEventListener("pointerdown", (event) => {
    if (event.button !== 1) return;
    event.preventDefault();
    panning = true;
    document.documentElement.setPointerCapture(event.pointerId);
    document.documentElement.style.cursor = "grabbing";
    send("pan-start", event);
  }, true);
  document.addEventListener("pointermove", (event) => {
    if (!panning) return;
    event.preventDefault();
    send("pan-move", event);
  }, true);
  const endPan = (event) => {
    if (!panning) return;
    panning = false;
    if (document.documentElement.hasPointerCapture(event.pointerId)) document.documentElement.releasePointerCapture(event.pointerId);
    document.documentElement.style.cursor = "";
    send("pan-end", event);
  };
  document.addEventListener("pointerup", endPan, true);
  document.addEventListener("pointercancel", endPan, true);
  document.addEventListener("auxclick", (event) => { if (event.button === 1) event.preventDefault(); }, true);
})();
</script>`;

export function injectPreviewCanvasBridge(html: string): string {
  if (html.includes(bridgeMarker)) return html;
  const bodyEnd = html.search(/<\/body\s*>/i);
  if (bodyEnd >= 0) return `${html.slice(0, bodyEnd)}${bridgeScript}${html.slice(bodyEnd)}`;
  return `${html}${bridgeScript}`;
}

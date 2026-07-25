const bridgeMarker = "data-werft-canvas-bridge";

const bridgeScript = `<script ${bridgeMarker}>
(() => {
  const send = (action, event) => parent.postMessage({ source: "werft-preview-canvas", action, x: event.clientX, y: event.clientY, deltaY: event.deltaY }, "*");
  let panning = false;
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

const normalizedBase = (previewBase: string) => previewBase.endsWith("/") ? previewBase : `${previewBase}/`;

export function rewriteRootRelativeCss(css: string, previewBase: string): string {
  const base = normalizedBase(previewBase);
  return css.replace(/(url\(\s*["']?)\/(?!\/)/gi, `$1${base}`);
}

export function rewriteRootRelativeJavaScript(code: string, previewBase: string): string {
  const base = normalizedBase(previewBase);
  return code
    .replace(/(\bfrom\s*["'])\/(?!\/)/g, `$1${base}`)
    .replace(/(\bimport\s*["'])\/(?!\/)/g, `$1${base}`)
    .replace(/(\bimport\s*\(\s*["'])\/(?!\/)/g, `$1${base}`);
}

export function rewriteRootRelativeAssets(html: string, previewBase: string): string {
  const base = normalizedBase(previewBase);
  return html
    .replace(/<[^>]+>/g, (tag) => tag
      .replace(/(\b(?:src|href|poster|action)\s*=\s*["'])\/(?!\/)/gi, `$1${base}`)
      .replace(/(\bsrcset\s*=\s*)(["'])(.*?)\2/gi, (_all, prefix: string, quote: string, value: string) => `${prefix}${quote}${value.split(",").map((candidate) => candidate.trim().replace(/^\/(?!\/)/, base)).join(", ")}${quote}`)
      .replace(/(\bstyle\s*=\s*)(["'])(.*?)\2/gi, (_all, prefix: string, quote: string, value: string) => `${prefix}${quote}${rewriteRootRelativeCss(value, base)}${quote}`))
    .replace(/(<style\b[^>]*>)([\s\S]*?)(<\/style\s*>)/gi, (_all, open: string, css: string, close: string) => `${open}${rewriteRootRelativeCss(css, base)}${close}`)
    .replace(/(<script\b[^>]*>)([\s\S]*?)(<\/script\s*>)/gi, (_all, open: string, code: string, close: string) => `${open}${rewriteRootRelativeJavaScript(code, base)}${close}`);
}

export function injectPreviewCanvasBridge(html: string, previewBase?: string): string {
  const withAssets = previewBase ? rewriteRootRelativeAssets(html, previewBase) : html;
  if (withAssets.includes(bridgeMarker)) return withAssets;
  const bodyEnd = withAssets.search(/<\/body\s*>/i);
  if (bodyEnd >= 0) return `${withAssets.slice(0, bodyEnd)}${bridgeScript}${withAssets.slice(bodyEnd)}`;
  return `${withAssets}${bridgeScript}`;
}

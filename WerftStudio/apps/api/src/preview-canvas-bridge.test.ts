import { describe, expect, it } from "vitest";
import { injectPreviewCanvasBridge, rewriteRootRelativeAssets, rewriteRootRelativeCss, rewriteRootRelativeJavaScript } from "./preview-canvas-bridge.js";

describe("preview canvas bridge", () => {
  it("injects the bridge before the closing body", () => {
    const result = injectPreviewCanvasBridge("<!doctype html><body><main>Design</main></body>");
    expect(result).toContain("data-werft-canvas-bridge");
    expect(result).not.toContain("document.body.style.transform");
    expect(result).not.toContain("overflow = \"hidden\"");
    expect(result.indexOf("data-werft-canvas-bridge")).toBeLessThan(result.indexOf("</body>"));
  });

  it("zoomt am Mausrad ohne Zusatztaste und meldet Bildschirme, Größe und Klickziele", () => {
    const result = injectPreviewCanvasBridge("<!doctype html><body><section class=\"werft-screen\"></section></body>");
    // Ohne Strg-Zwang: der Radlauf wird immer an die Leinwand gemeldet, ausser der Inhalt scrollt selbst.
    expect(result).not.toContain("if (!event.ctrlKey) return;");
    expect(result).toContain("scrollableUnder");
    expect(result).toContain('action: "screens"');
    expect(result).toContain('post({ action: "size"');
    expect(result).toContain('action: "navigate"');
    expect(result).toContain("werftFrame");
    expect(result).toContain("werftScreen");
    expect(result).toContain("data-werft-highlight");
  });

  it("liefert syntaktisch gültiges JavaScript aus", () => {
    // Ein Tippfehler im eingebetteten Skript würde JEDE Vorschau lahmlegen und wäre in einem
    // reinen Textvergleich unsichtbar. `new Function` parst den Code, ohne ihn auszuführen.
    const code = /<script data-werft-canvas-bridge>([\s\S]*?)<\/script>/.exec(injectPreviewCanvasBridge("<body></body>"))?.[1];
    expect(code?.length).toBeGreaterThan(500);
    expect(() => new Function(code!)).not.toThrow();
  });

  it("does not inject the bridge twice", () => {
    const once = injectPreviewCanvasBridge("<main>Design</main>");
    expect(injectPreviewCanvasBridge(once)).toBe(once);
  });

  it("keeps root-relative assets inside the signed project preview", () => {
    const result = rewriteRootRelativeAssets('<link href="/assets/app.css"><img src="/images/a.png" srcset="/a.png 1x, /b.png 2x"><style>.x{background:url(\'/bg.png\')}</style><script>const src="/api"; import("/assets/lazy.js")</script>', "/api/v1/previews/project/token");
    expect(result).toContain('href="/api/v1/previews/project/token/assets/app.css"');
    expect(result).toContain('src="/api/v1/previews/project/token/images/a.png"');
    expect(result).toContain("url('/api/v1/previews/project/token/bg.png')");
    expect(result).toContain('srcset="/api/v1/previews/project/token/a.png 1x, /api/v1/previews/project/token/b.png 2x"');
    expect(result).toContain('const src="/api"');
    expect(result).toContain('import("/api/v1/previews/project/token/assets/lazy.js")');
  });

  it("rewrites root-relative resources in separately served CSS and ES modules", () => {
    expect(rewriteRootRelativeCss(".hero{background:url(/images/hero.png)}", "/preview/root")).toContain("url(/preview/root/images/hero.png)");
    expect(rewriteRootRelativeJavaScript('import x from "/chunks/x.js"; export { y } from "/chunks/y.js";', "/preview/root")).toBe('import x from "/preview/root/chunks/x.js"; export { y } from "/preview/root/chunks/y.js";');
  });

  // Die Bruecke ist ein String — kein Compiler prueft sie. Ein Syntaxfehler darin wuerde JEDE
  // Vorschau lahmlegen, ohne dass Typecheck oder Build etwas melden.
  it("keeps the embedded bridge script parsable and offers marking mode", () => {
    const html = injectPreviewCanvasBridge("<!doctype html><body></body>");
    const code = /<script data-werft-canvas-bridge>([\s\S]*?)<\/script>/.exec(html)?.[1];
    expect(code, "Bruecken-Skript nicht gefunden").toBeTruthy();
    expect(() => new Function(code!)).not.toThrow();
    expect(code).toContain('action: "mark-target"');
    expect(code).toContain("selectorFor");
    expect(code).toContain("markMode");
  });
});

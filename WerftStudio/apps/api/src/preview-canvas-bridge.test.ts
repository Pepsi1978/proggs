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
});

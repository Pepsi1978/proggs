import { describe, expect, it } from "vitest";
import { injectPreviewCanvasBridge } from "./preview-canvas-bridge.js";

describe("preview canvas bridge", () => {
  it("injects the bridge before the closing body", () => {
    const result = injectPreviewCanvasBridge("<!doctype html><body><main>Design</main></body>");
    expect(result).toContain("data-werft-canvas-bridge");
    expect(result).toContain('data.source !== "werft-studio-canvas"');
    expect(result).toContain("canvas.style.transform");
    expect(result.indexOf("data-werft-canvas-bridge")).toBeLessThan(result.indexOf("</body>"));
  });

  it("does not inject the bridge twice", () => {
    const once = injectPreviewCanvasBridge("<main>Design</main>");
    expect(injectPreviewCanvasBridge(once)).toBe(once);
  });
});

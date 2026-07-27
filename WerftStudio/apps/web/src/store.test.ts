import { afterEach, describe, expect, it, vi } from "vitest";

afterEach(() => {
  vi.unstubAllGlobals();
  vi.resetModules();
});

describe("UI-Zustand", () => {
  it("behält Theme und Monitorkalibrierung, aber nicht den Zoom über einen Reload", async () => {
    const values = new Map<string, string>();
    const localStorage = {
      getItem: (key: string) => values.get(key) ?? null,
      setItem: (key: string, value: string) => values.set(key, value),
      removeItem: (key: string) => values.delete(key)
    };
    vi.stubGlobal("window", { localStorage });

    const first = await import("./store");
    first.useUi.getState().setTheme("dark");
    first.useUi.getState().setMonitorPixelsPerMillimeter(4.25);
    first.useUi.getState().setZoom(2);
    expect(JSON.parse(values.get("werft-ui")!).state).toEqual({ theme: "dark", monitorPixelsPerMillimeter: 4.25 });

    vi.resetModules();
    const reloaded = await import("./store");
    expect(reloaded.useUi.getState().theme).toBe("dark");
    expect(reloaded.useUi.getState().monitorPixelsPerMillimeter).toBe(4.25);
    expect(reloaded.useUi.getState().zoom).toBe(.72);
  });
});

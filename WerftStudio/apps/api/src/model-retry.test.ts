import { describe, expect, it } from "vitest";
import { isOverloadError, maxModelAttempts, retryDelayMs, UpstreamThrottle } from "./model-retry.js";
import { codexHttpError } from "./codex-stream.js";

describe("retry budget", () => {
  it("survives a provider outage that lasts longer than a few seconds", () => {
    let total = 0;
    for (let attempt = 1; attempt < maxModelAttempts; attempt += 1) total += retryDelayMs(attempt, undefined, () => 1);
    expect(maxModelAttempts).toBeGreaterThan(3);
    expect(total).toBeGreaterThan(60_000);
  });

  it("grows exponentially and never fires two attempts at the same moment", () => {
    expect(retryDelayMs(1, undefined, () => 0)).toBeLessThan(retryDelayMs(3, undefined, () => 0));
    expect(retryDelayMs(3, undefined, () => 0)).not.toBe(retryDelayMs(3, undefined, () => 1));
    expect(retryDelayMs(99, undefined, () => 1)).toBeLessThanOrEqual(45_000);
  });

  it("never waits shorter than the provider asked for", () => {
    expect(retryDelayMs(1, 20_000, () => 0)).toBe(20_000);
    expect(retryDelayMs(1, 0, () => 1)).toBe(2_000);
  });
});

describe("overload detection", () => {
  it("recognises exactly the states that mean the provider is out of capacity", () => {
    expect(isOverloadError(codexHttpError(503))).toBe(true);
    expect(isOverloadError(codexHttpError(429))).toBe(true);
    expect(isOverloadError(codexHttpError(400))).toBe(false);
    expect(isOverloadError(new Error("kein Statuscode"))).toBe(false);
  });
});

describe("adaptive throttle", () => {
  it("cuts concurrency after an overload and restores it once the provider recovered", async () => {
    let now = 0;
    const throttle = new UpstreamThrottle(8, 2, 60_000, () => now);
    expect(throttle.state.activeLimit).toBe(8);
    throttle.penalize();
    expect(throttle.state.activeLimit).toBe(2);
    now += 59_000;
    expect(throttle.state.activeLimit).toBe(2);
    now += 2_000;
    expect(throttle.state.activeLimit).toBe(8);
  });

  it("lets waiting steps through as soon as a slot frees up", async () => {
    const throttle = new UpstreamThrottle(1);
    const first = await throttle.acquire();
    let secondEntered = false;
    const second = throttle.acquire().then((release) => { secondEntered = true; return release; });
    await Promise.resolve();
    expect(secondEntered).toBe(false);
    first();
    (await second)();
    expect(secondEntered).toBe(true);
  });
});

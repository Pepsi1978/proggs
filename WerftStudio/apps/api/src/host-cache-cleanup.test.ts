import { mkdtemp, readFile, rename, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { describe, expect, it } from "vitest";
import { requestHostCacheCleanup } from "./host-cache-cleanup.js";

describe("host cache cleanup bridge", () => {
  it("waits for the result belonging to its fixed cleanup request", async () => {
    const directory = await mkdtemp(join(tmpdir(), "werft-cache-cleanup-"));
    try {
      const pending = requestHostCacheCleanup(directory, 1_000, 10);
      let requestId = "";
      for (let attempt = 0; attempt < 50 && !requestId; attempt += 1) {
        try { requestId = await readFile(join(directory, "prune.request"), "utf8"); }
        catch { await new Promise((resolve) => setTimeout(resolve, 10)); }
      }
      expect(requestId).toMatch(/^[0-9a-f-]{36}$/);
      const temporaryResult = join(directory, "prune.result.tmp");
      await writeFile(temporaryResult, JSON.stringify({ requestId, freedBytes: 12_345, usedBytes: 98_765, ok: true }));
      await rename(temporaryResult, join(directory, "prune.result"));
      await expect(pending).resolves.toEqual({ requestId, freedBytes: 12_345, usedBytes: 98_765, ok: true });
    } finally {
      await rm(directory, { recursive: true, force: true });
    }
  });
});

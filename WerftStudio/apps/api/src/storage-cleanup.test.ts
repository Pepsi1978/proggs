import { describe, expect, it, vi } from "vitest";
import { cleanupOrphanedObjects } from "./storage-cleanup.js";

describe("storage cleanup", () => {
  it("removes only incomplete uploads and objects not referenced by a design", async () => {
    const removeIncompleteUpload = vi.fn(async () => undefined);
    const removeObject = vi.fn(async () => undefined);
    const incompleteUploads = (async function* () { yield { key: "unfinished" }; })();
    const objects = (async function* () {
      yield { name: "active/source.kt", size: 100 };
      yield { name: "orphan/cache.bin", size: 2048 };
      yield { size: 4096 };
    })();

    const result = await cleanupOrphanedObjects({
      knownObjects: new Set(["active/source.kt"]),
      incompleteUploads,
      objects,
      removeIncompleteUpload,
      removeObject
    });

    expect(removeIncompleteUpload).toHaveBeenCalledWith("unfinished");
    expect(removeObject).toHaveBeenCalledOnce();
    expect(removeObject).toHaveBeenCalledWith("orphan/cache.bin");
    expect(result).toEqual({ removedIncompleteUploads: 1, removedObjects: 1, freedBytes: 2048 });
  });

  it("protects recent objects and running uploads during a manual cleanup", async () => {
    const removeIncompleteUpload = vi.fn(async () => undefined);
    const removeObject = vi.fn(async () => undefined);
    const incompleteUploads = (async function* () { yield { key: "running" }; })();
    const objects = (async function* () {
      yield { name: "recent-upload", size: 100, lastModified: new Date("2026-07-27T19:30:00Z") };
      yield { name: "old-orphan", size: 200, lastModified: new Date("2026-07-25T19:30:00Z") };
    })();

    const result = await cleanupOrphanedObjects({
      knownObjects: new Set(),
      incompleteUploads,
      objects,
      removeIncompleteUpload,
      removeObject,
      removeIncompleteUploads: false,
      minimumAgeMs: 24 * 60 * 60 * 1000,
      now: new Date("2026-07-27T20:00:00Z").getTime()
    });

    expect(removeIncompleteUpload).not.toHaveBeenCalled();
    expect(removeObject).toHaveBeenCalledOnce();
    expect(removeObject).toHaveBeenCalledWith("old-orphan");
    expect(result).toEqual({ removedIncompleteUploads: 0, removedObjects: 1, freedBytes: 200 });
  });
});

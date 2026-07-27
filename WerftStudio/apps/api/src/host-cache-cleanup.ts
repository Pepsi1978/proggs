import { randomUUID } from "node:crypto";
import { access, readFile, rename, writeFile } from "node:fs/promises";
import { join } from "node:path";

export type HostCacheCleanupResult = { requestId: string; freedBytes: number; usedBytes: number; ok: boolean };

export async function requestHostCacheCleanup(directory: string, timeoutMs = 180_000, pollMs = 250): Promise<HostCacheCleanupResult> {
  await access(directory);
  const requestId = randomUUID();
  const temporaryRequest = join(directory, `prune.request.${requestId}`);
  await writeFile(temporaryRequest, requestId, { encoding: "utf8", mode: 0o600, flag: "wx" });
  await rename(temporaryRequest, join(directory, "prune.request"));
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    try {
      const result = JSON.parse(await readFile(join(directory, "prune.result"), "utf8")) as Partial<HostCacheCleanupResult>;
      if (result.requestId === requestId && Number.isSafeInteger(result.freedBytes) && result.freedBytes! >= 0 && Number.isSafeInteger(result.usedBytes) && result.usedBytes! >= 0 && typeof result.ok === "boolean") {
        if (!result.ok) throw new Error("Der Host-Dienst konnte den Docker-Build-Cache nicht bereinigen.");
        return result as HostCacheCleanupResult;
      }
    } catch (error) {
      if ((error as NodeJS.ErrnoException).code !== "ENOENT") throw error;
    }
    await new Promise((resolve) => setTimeout(resolve, pollMs));
  }
  throw new Error("Der Host-Dienst hat die Cache-Bereinigung nicht rechtzeitig bestätigt.");
}

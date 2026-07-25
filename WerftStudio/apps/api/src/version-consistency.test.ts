import { readFileSync } from "node:fs";
import { describe, expect, it } from "vitest";

describe("visible application version", () => {
  it("keeps package, API and visible web build timestamps synchronized", () => {
    const packageJson = JSON.parse(readFileSync(new URL("../../../package.json", import.meta.url), "utf8")) as { version: string };
    const match = /^(\d+\.\d+\.\d+)-(\d{4})(\d{2})(\d{2})\.(\d{2})(\d{2})$/.exec(packageJson.version);
    expect(match).not.toBeNull();
    const [, version, year, month, day, hour, minute] = match!;
    const visibleCss = readFileSync(new URL("../../web/src/version.css", import.meta.url), "utf8");
    const serverSource = readFileSync(new URL("./server.ts", import.meta.url), "utf8");
    expect(visibleCss).toContain(`--werft-version: "Werft v${version}"`);
    expect(visibleCss).toContain(`--werft-build-time: "${day}.${month}.${year} ${hour}:${minute} Uhr"`);
    expect(serverSource).toContain(`version: "${packageJson.version}"`);
  });
});

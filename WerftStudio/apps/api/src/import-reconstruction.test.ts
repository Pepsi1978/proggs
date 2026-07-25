import { describe, expect, it } from "vitest";
import { buildSourceBatches, previewProfileFromHtml, previewProfiles, reconstructionSourceFiles } from "./import-reconstruction.js";

const file = (path: string, size = 10, mime = "application/octet-stream") => ({ path, size, mime });

describe("native UI reconstruction", () => {
  it("uses platform viewports instead of a desktop size for every import", () => {
    expect(previewProfiles.android).toMatchObject({ width: 412, height: 915 });
    expect(previewProfiles.windows).toMatchObject({ width: 1280, height: 800 });
    expect(previewProfiles.web).toMatchObject({ width: 1440, height: 900 });
  });

  it("uses source-derived preview geometry when reconstructed HTML declares it", () => {
    const html = `<meta name="werft-preview" content='{&quot;width&quot;:1080,&quot;height&quot;:720,&quot;device&quot;:&quot;Original Window&quot;,&quot;density&quot;:1.5}'>`;
    expect(previewProfileFromHtml(html, previewProfiles.windows)).toEqual({ width: 1080, height: 720, device: "Original Window", density: 1.5 });
    expect(previewProfileFromHtml("<meta name='werft-preview' content='invalid'>", previewProfiles.android)).toBe(previewProfiles.android);
  });

  it("keeps first-party UI sources across supported native frameworks", () => {
    const sources = reconstructionSourceFiles([
      file("app/src/main/java/com/acme/MainActivity.kt"),
      file("app/src/main/res/values/themes.xml"),
      file("ios/App.swift"),
      file("windows/MainWindow.xaml"),
      file("windows/MainViewModel.cs"),
      file("flutter/pubspec.yaml"),
      file("qt/Main.qml"),
      file("node_modules/library/index.ts"),
      file("app/build/generated/source.kt")
    ]).map((item) => item.path);
    expect(sources).toEqual(expect.arrayContaining([
      "app/src/main/java/com/acme/MainActivity.kt",
      "app/src/main/res/values/themes.xml",
      "ios/App.swift",
      "windows/MainWindow.xaml",
      "windows/MainViewModel.cs",
      "flutter/pubspec.yaml",
      "qt/Main.qml"
    ]));
    expect(sources).not.toContain("node_modules/library/index.ts");
    expect(sources).not.toContain("app/build/generated/source.kt");
  });

  it("chunks every source without silently skipping large files", async () => {
    const files = [file("ui/large.kt", 12), file("ui/theme.xml", 4)];
    const contents = new Map([["ui/large.kt", "A".repeat(25)], ["ui/theme.xml", "theme"]]);
    const batches = [];
    for await (const batch of buildSourceBatches(files, async (item) => contents.get(item.path)!, 30, 10)) batches.push(batch);
    const joined = batches.map((batch) => batch.text).join("");
    expect((joined.match(/A/g) ?? []).length).toBeGreaterThanOrEqual(25);
    expect(joined).toContain("theme");
    expect(batches.reduce((sum, batch) => sum + batch.completedBytes, 0)).toBe(16);
    expect(batches.reduce((sum, batch) => sum + batch.completedFiles, 0)).toBe(2);
  });

  it("reads streamed source chunks without buffering an entire file contract", async () => {
    async function* stream() { yield Buffer.from("first-"); yield Buffer.from("second-third"); }
    const batches = [];
    for await (const batch of buildSourceBatches([file("ui/main.py", 18)], async () => stream(), 80, 8, 2)) batches.push(batch);
    const joined = batches.map((batch) => batch.text).join("");
    expect(joined).toContain("first-");
    expect(joined).toContain("second");
    expect(joined).toContain("third");
    expect(batches.reduce((sum, batch) => sum + batch.completedBytes, 0)).toBe(18);
  });
});

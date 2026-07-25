import { describe, expect, it } from "vitest";
import { chooseEntryPath, isFrontendFile, isGeneratedArtifact, mimeForPath, normalizeImportPath, stripCommonRoot, validateImportFiles } from "./import-project.js";

const file = (path: string, text = "x") => ({ path, data: Buffer.from(text), mime: "text/plain" });

describe("project import validation", () => {
  it("rejects absolute and traversing paths", () => {
    expect(() => normalizeImportPath("../secret.txt")).toThrow(/Unsicherer/);
    expect(() => normalizeImportPath("C:\\secret.txt")).toThrow(/Ungültiger/);
    expect(() => normalizeImportPath("/etc/passwd")).toThrow(/Ungültiger/);
  });

  it("preserves a project tree while removing its common upload root", () => {
    expect(stripCommonRoot([file("Demo/index.html"), file("Demo/audio/click.wav")]).map((item) => item.path)).toEqual(["index.html", "audio/click.wav"]);
  });

  it("selects index.html before DC and arbitrary HTML entries", () => {
    expect(chooseEntryPath([file("pages/demo.html"), file("Studio.dc.html"), file("app/index.html")])).toBe("app/index.html");
  });

  it("never prefers tooling leftovers like skill reviews over real pages", () => {
    expect(chooseEntryPath([file("Profiles/skills/cowork-workspace/iteration-1/review.html"), file("start.html")])).toBe("start.html");
    expect(chooseEntryPath([file("plugins/foo/template.html"), file("web/dashboard.html")])).toBe("web/dashboard.html");
  });

  it("reports no entry when only tooling leftovers exist (e.g. desktop apps)", () => {
    expect(chooseEntryPath([file("Profiles/skills/cowork-workspace/iteration-1/review.html"), file("plugins/foo/template.html")])).toBeUndefined();
    expect(chooseEntryPath([file("App.xaml"), file("Models/Config.cs")])).toBeUndefined();
  });

  it("rejects duplicate paths independent of case", () => {
    expect(() => validateImportFiles([file("Logo.svg"), file("logo.svg")])).toThrow(/mehrfach/);
  });

  it("keeps all first-party UI sources while excluding dependencies and native build output", () => {
    for (const path of ["app/src/main/java/acme/MainActivity.kt", "app/src/main/java/acme/NavGraph.java", "windows/MainViewModel.cs", "ios/App.swift", "src/main/res/values/themes.xml", "flutter/pubspec.yaml", "qt/Main.qml", "desktop/gui.py", "src/app.rs", "Views/Home.razor", "unity/Main.uxml"]) expect(isFrontendFile(path, "android"), path).toBe(true);
    expect(isFrontendFile("node_modules/lib/Button.tsx", "web")).toBe(false);
    expect(isFrontendFile("backend/server.py", "windows")).toBe(false);
    expect(isFrontendFile("tests/App.test.tsx", "web")).toBe(false);
    expect(isFrontendFile("app/build/generated/Binding.java", "android")).toBe(false);
    expect(isFrontendFile("dist/index.html", "web")).toBe(true);
    expect(isFrontendFile("app.webmanifest", "web")).toBe(true);
    expect(isFrontendFile("design.werft", "android")).toBe(true);
    expect(isFrontendFile("runtime/app.mjs", "web")).toBe(true);
    expect(isFrontendFile("runtime/app.wasm", "web")).toBe(true);
  });

  it("never shows a Gradle or tooling report instead of the app design", () => {
    // Der Gradle-Testreport gewann die Startseitenwahl (index.html + build/ als Pluspunkt) und
    // wurde als „HTML originalgetreu aufgebaut" angezeigt — statt der App.
    const report = file("app/build/reports/tests/testDebugUnitTest/index.html");
    expect(chooseEntryPath([report, file("app/src/main/res/layout/activity_main.xml")], "android")).toBeUndefined();
    expect(chooseEntryPath([report, file("www/index.html")], "android")).toBe("www/index.html");
    expect(chooseEntryPath([file("build/reports/jacoco/test/html/index.html"), file("public/index.html")], "web")).toBe("public/index.html");
    expect(chooseEntryPath([file("build/dokka/html/index.html")], "android")).toBeUndefined();
  });

  it("drops generated artifacts and dependencies even when the frontend filter is off", () => {
    for (const path of ["app/build/reports/tests/test/index.html", "app/build/intermediates/merged_res/values.xml", "node_modules/lib/index.js", ".gradle/caches/x.bin", "ios/DerivedData/App/x.plist", "Pods/Alamofire/Source.swift", "app/build/outputs/apk/app.apk", "obj/Debug/App.dll", "coverage/lcov-report/index.html"]) {
      expect(isGeneratedArtifact(path, "android"), path).toBe(true);
    }
    // Echte Quellen und Web-Bundles bleiben erhalten.
    expect(isGeneratedArtifact("app/src/main/res/values/colors.xml", "android")).toBe(false);
    expect(isGeneratedArtifact("dist/index.html", "web")).toBe(false);
    expect(isGeneratedArtifact("build/index.html", "web")).toBe(false);
    expect(isGeneratedArtifact("src/components/Button.tsx", "web")).toBe(false);
  });

  it("marks native UI sources as editable text", () => {
    expect(mimeForPath("MainActivity.kt")).toMatch(/^text\//);
    expect(mimeForPath("MainWindow.xaml")).toMatch(/xml/);
    expect(mimeForPath("App.swift")).toMatch(/^text\//);
  });
});

import { describe, expect, it } from "vitest";
import { chooseEntryPath, normalizeImportPath, stripCommonRoot, validateImportFiles } from "./import-project.js";

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

  it("rejects duplicate paths independent of case", () => {
    expect(() => validateImportFiles([file("Logo.svg"), file("logo.svg")])).toThrow(/mehrfach/);
  });
});

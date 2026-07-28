import { describe, expect, it } from "vitest";
import { analysisKey, checkpointIsUseful, checkpointPrefix, checkpointSummary, describeCheckpoint, emptyCheckpoint, evidenceKey, mergeCheckpointPart, parseCheckpointObject, resumableAnalyses, resumableScreens, screenKey } from "./reconstruction-checkpoint.js";

const scope = { projectId: "p1", revision: 7, selectionHash: "abc123", viewportKey: "basis" };
const prefix = checkpointPrefix(scope);

describe("checkpoint keys", () => {
  it("binds the checkpoint to revision, model selection and target format", () => {
    expect(prefix).toContain("p1");
    expect(prefix).toContain("7-abc123-basis");
    expect(checkpointPrefix({ ...scope, revision: 8 })).not.toBe(prefix);
    expect(checkpointPrefix({ ...scope, selectionHash: "other" })).not.toBe(prefix);
    expect(checkpointPrefix({ ...scope, viewportKey: "1024x768" })).not.toBe(prefix);
  });

  it("keeps analysis packages sortable and screen ids filesystem safe", () => {
    expect(analysisKey(prefix, 2) < analysisKey(prefix, 10)).toBe(true);
    expect(screenKey(prefix, "home/main screen")).toBe(`${prefix}screen-home_main_screen.json`);
  });
});

describe("checkpoint parsing", () => {
  it("reads analyses, evidence and screens back", () => {
    expect(parseCheckpointObject(analysisKey(prefix, 3), prefix, "protokoll")).toEqual({ analyses: [{ number: 3, text: "protokoll" }] });
    expect(parseCheckpointObject(evidenceKey(prefix), prefix, "evidenz")).toEqual({ evidence: "evidenz" });
    expect(parseCheckpointObject(screenKey(prefix, "home"), prefix, JSON.stringify({ id: "home", markup: "<div/>", css: ".a{}" })))
      .toEqual({ screens: [{ id: "home", markup: "<div/>", css: ".a{}" }] });
  });

  it("drops damaged or empty screen fragments instead of counting them as finished", () => {
    expect(parseCheckpointObject(screenKey(prefix, "home"), prefix, "{kaputt")).toBeUndefined();
    expect(parseCheckpointObject(screenKey(prefix, "home"), prefix, JSON.stringify({ id: "home", markup: "   " }))).toBeUndefined();
    expect(parseCheckpointObject("fremder/pfad.txt", prefix, "x")).toBeUndefined();
  });
});

describe("resuming", () => {
  const parts = [
    { analyses: [{ number: 2, text: "zwei" }] },
    { analyses: [{ number: 1, text: "eins" }] },
    { screens: [{ id: "home", markup: "<h1/>", css: "" }] }
  ].reduce(mergeCheckpointPart, emptyCheckpoint);

  it("takes analysis packages only as a gapless sequence starting at one", () => {
    expect(resumableAnalyses(parts)).toEqual(["eins", "zwei"]);
    const withGap = mergeCheckpointPart(parts, { analyses: [{ number: 5, text: "fuenf" }] });
    expect(resumableAnalyses(withGap)).toEqual(["eins", "zwei"]);
  });

  it("reuses only screens that are still planned", () => {
    expect([...resumableScreens(parts, ["home", "settings"]).keys()]).toEqual(["home"]);
    expect(resumableScreens(parts, ["settings"]).size).toBe(0);
  });

  it("summarises what survives a failed run", () => {
    const summary = checkpointSummary(parts);
    expect(summary).toEqual({ analyses: 2, screens: 1, hasEvidence: false });
    expect(checkpointIsUseful(summary)).toBe(true);
    expect(checkpointIsUseful(checkpointSummary(emptyCheckpoint))).toBe(false);
    expect(describeCheckpoint(summary, 4)).toBe("Übernommen aus dem letzten Lauf: 2 Analysepakete, 1 von 4 fertiger Bildschirm.");
  });
});

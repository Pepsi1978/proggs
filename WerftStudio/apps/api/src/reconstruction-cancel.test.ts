import { describe, expect, it } from "vitest";
import { abortReconstruction, cancelledCode, cancelMessage, cancelReason, endsRun, isCancelled, registerReconstruction, releaseReconstruction } from "./reconstruction-cancel.js";
import { canRestartReconstructionJob } from "./import-reconstruction.js";

describe("Abbruch im laufenden Prozess", () => {
  it("erreicht einen registrierten Lauf sofort", () => {
    const controller = registerReconstruction("job-1");
    expect(controller.signal.aborted).toBe(false);
    expect(abortReconstruction("job-1")).toBe(true);
    expect(controller.signal.aborted).toBe(true);
    expect((controller.signal.reason as { code?: string }).code).toBe(cancelledCode);
    releaseReconstruction("job-1", controller);
  });

  it("meldet ehrlich, wenn hier gar kein Lauf liegt", () => {
    expect(abortReconstruction("job-unbekannt")).toBe(false);
  });

  it("bricht denselben Lauf nicht zweimal ab", () => {
    const controller = registerReconstruction("job-2");
    expect(abortReconstruction("job-2")).toBe(true);
    expect(abortReconstruction("job-2")).toBe(false);
    releaseReconstruction("job-2", controller);
  });

  it("gibt einen zweiten Lauf derselben Kennung den Vorrang und beendet den ersten", () => {
    const erster = registerReconstruction("job-3");
    const zweiter = registerReconstruction("job-3");
    expect(erster.signal.aborted).toBe(true);
    expect(zweiter.signal.aborted).toBe(false);
    expect(abortReconstruction("job-3")).toBe(true);
    expect(zweiter.signal.aborted).toBe(true);
    releaseReconstruction("job-3", zweiter);
  });

  it("räumt nur den eigenen Eintrag weg", () => {
    const erster = registerReconstruction("job-4");
    const zweiter = registerReconstruction("job-4");
    // Der alte Lauf raeumt spaet auf — der Abbruchweg des neuen muss trotzdem stehen bleiben.
    releaseReconstruction("job-4", erster);
    expect(abortReconstruction("job-4")).toBe(true);
    expect(zweiter.signal.aborted).toBe(true);
    releaseReconstruction("job-4", zweiter);
  });

  it("ist nach dem Aufräumen nicht mehr erreichbar", () => {
    const controller = registerReconstruction("job-5");
    releaseReconstruction("job-5", controller);
    expect(abortReconstruction("job-5")).toBe(false);
    expect(controller.signal.aborted).toBe(false);
  });
});

describe("Deutung der Kennungen", () => {
  it("erkennt den Abbruch", () => {
    expect(isCancelled(cancelledCode)).toBe(true);
    expect(isCancelled("RECONSTRUCT_FAILED")).toBe(false);
    expect(cancelReason().code).toBe(cancelledCode);
  });

  it("beendet den Lauf bei Abbruch und bei verlorener Berechtigung", () => {
    expect(endsRun(cancelledCode)).toBe(true);
    expect(endsRun("RECONSTRUCT_LEASE_LOST")).toBe(true);
    expect(endsRun("RECONSTRUCT_STEP_FAILED")).toBe(false);
    expect(endsRun(undefined)).toBe(false);
  });

  it("meldet den Abbruch als Entscheidung, nicht als Fehler", () => {
    expect(cancelMessage("")).toBe("Der Aufbau wurde abgebrochen.");
    expect(cancelMessage("3 Bildschirme sind gesichert.")).toContain("3 Bildschirme sind gesichert.");
  });
});

describe("Neustart nach einem Abbruch", () => {
  it("lässt einen abgebrochenen Lauf ohne Umweg neu starten", () => {
    expect(canRestartReconstructionJob("failed", false, false, cancelledCode)).toBe(true);
  });

  it("lässt einen echten Fehlversuch nur als Wiederholung neu starten", () => {
    expect(canRestartReconstructionJob("failed", false, false, "RECONSTRUCT_FAILED")).toBe(false);
    expect(canRestartReconstructionJob("failed", true, false, "RECONSTRUCT_FAILED")).toBe(true);
  });

  it("lässt einen noch laufenden Aufbau nicht doppelt starten", () => {
    expect(canRestartReconstructionJob("running", true, true, cancelledCode)).toBe(false);
    expect(canRestartReconstructionJob("queued", true, true, cancelledCode)).toBe(false);
  });

  it("bleibt für die bisherigen Aufrufe ohne Kennung unverändert", () => {
    expect(canRestartReconstructionJob("failed", true)).toBe(true);
    expect(canRestartReconstructionJob("failed", false)).toBe(false);
    expect(canRestartReconstructionJob("completed", false, true)).toBe(true);
  });
});

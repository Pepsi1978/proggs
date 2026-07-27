import { describe, expect, it } from "vitest";
import { applyOperationsSchema, createProjectSchema } from "./index.js";

describe("versionierte Verträge", () => {
  it("lehnt leere Plattformlisten ab", () => {
    expect(() => createProjectSchema.parse({
      name: "Test", type: "prototype", fidelity: "wireframe", platforms: [], aiProfile: "standard"
    })).toThrow();
  });

  it("übernimmt ein Android-Startlayout nur für Android-Projekte", () => {
    const project = { name: "Test", type: "prototype", fidelity: "high_fidelity", platforms: ["android"], aiProfile: "standard", viewport: { width: 1224, height: 914, device: "Galaxy Z Fold 8 · aufgeklappt · quer" } };
    expect(createProjectSchema.parse(project).viewport).toEqual(project.viewport);
    expect(() => createProjectSchema.parse({ ...project, platforms: ["ios"] })).toThrow();
  });

  it("verlangt Revisionsschutz für Designoperationen", () => {
    expect(() => applyOperationsSchema.parse({ actionId: crypto.randomUUID(), operations: [] })).toThrow();
  });
});

import { describe, expect, it } from "vitest";
import { applyOperationsSchema, createProjectSchema } from "./index.js";

describe("versionierte Verträge", () => {
  it("lehnt leere Plattformlisten ab", () => {
    expect(() => createProjectSchema.parse({
      name: "Test", type: "prototype", fidelity: "wireframe", platforms: [], aiProfile: "standard"
    })).toThrow();
  });

  it("verlangt Revisionsschutz für Designoperationen", () => {
    expect(() => applyOperationsSchema.parse({ actionId: crypto.randomUUID(), operations: [] })).toThrow();
  });
});

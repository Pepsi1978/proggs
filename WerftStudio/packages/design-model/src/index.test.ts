import { describe, expect, it } from "vitest";
import type { DesignDocument } from "@werft/contracts";
import { applyDesignOperations } from "./index.js";

const projectId = "00000000-0000-4000-8000-000000000001";
const pageId = "00000000-0000-4000-8000-000000000002";
const frameId = "00000000-0000-4000-8000-000000000003";
const nodeId = "00000000-0000-4000-8000-000000000004";
const fixture: DesignDocument = {
  schemaVersion: 1, projectId, projectType: "prototype", fidelity: "high_fidelity", platforms: ["web"], designSystemVersionId: null,
  themes: [{ id: "light", name: "Hell", tokens: {} }], pages: [{ id: pageId, name: "Start", type: "screen", frameIds: [frameId] }],
  frames: [{ id: frameId, pageId, name: "Desktop", platform: "web", device: "Desktop", width: 1440, height: 900, theme: "light", locale: "de-DE", rootNodeId: nodeId, canvasX: 0, canvasY: 0 }],
  nodes: [{ id: nodeId, name: "Root", parentId: null, childIds: [], bounds: { x: 0, y: 0, width: 1440, height: 900 }, visible: true, locked: false, tokenBindings: {}, semantics: {}, type: "container", layout: "column", gap: 0, padding: [0, 0, 0, 0] }],
  assets: [], interactions: [], metadata: { createdAt: "2026-07-20T18:00:00.000Z", compilerVersion: "0.1.0" }
};

describe("Designoperationen", () => {
  it("wendet erlaubte atomare Änderungen an", () => {
    const result = applyDesignOperations(fixture, [{ type: "update_node", nodeId, patch: { name: "App" } }]);
    expect(result.nodes[0]?.name).toBe("App");
    expect(fixture.nodes[0]?.name).toBe("Root");
  });
  it("blockiert unbekannte Felder", () => {
    expect(() => applyDesignOperations(fixture, [{ type: "update_node", nodeId, patch: { id: crypto.randomUUID() } }])).toThrow();
  });
});

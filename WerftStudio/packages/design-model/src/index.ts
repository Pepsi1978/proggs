import { designDocumentSchema, type DesignDocument, type DesignOperation } from "@werft/contracts";

const patchableNodeKeys = new Set(["name", "bounds", "visible", "locked", "tokenBindings", "semantics", "text", "fontFamily", "fontSize", "fontWeight", "lineHeight", "color", "layout", "gap", "padding", "fill", "radius", "fit", "variant", "state"]);
const patchableFrameKeys = new Set(["name", "device", "width", "height", "theme", "locale", "canvasX", "canvasY"]);

function safePatch<T extends object>(target: T, patch: Record<string, unknown>, allowed: Set<string>): T {
  for (const key of Object.keys(patch)) {
    if (!allowed.has(key)) throw new Error(`Nicht erlaubtes Patch-Feld: ${key}`);
  }
  return { ...target, ...patch };
}

export function applyDesignOperations(document: DesignDocument, operations: DesignOperation[]): DesignDocument {
  let next = structuredClone(document);
  for (const operation of operations) {
    switch (operation.type) {
      case "add_node":
        if (next.nodes.some((node) => node.id === operation.node.id)) throw new Error("Node-ID existiert bereits");
        next.nodes.push(operation.node);
        break;
      case "update_node": {
        const index = next.nodes.findIndex((node) => node.id === operation.nodeId);
        if (index < 0) throw new Error("Knoten nicht gefunden");
        next.nodes[index] = safePatch(next.nodes[index]!, operation.patch, patchableNodeKeys);
        break;
      }
      case "remove_node":
        if (next.frames.some((frame) => frame.rootNodeId === operation.nodeId)) throw new Error("Frame-Wurzel kann nicht entfernt werden");
        next.nodes = next.nodes.filter((node) => node.id !== operation.nodeId);
        next.nodes = next.nodes.map((node) => ({ ...node, childIds: node.childIds.filter((id) => id !== operation.nodeId) }));
        break;
      case "update_frame": {
        const index = next.frames.findIndex((frame) => frame.id === operation.frameId);
        if (index < 0) throw new Error("Frame nicht gefunden");
        next.frames[index] = safePatch(next.frames[index]!, operation.patch, patchableFrameKeys);
        break;
      }
      case "set_token": {
        const theme = next.themes.find((item) => item.id === operation.themeId);
        if (!theme) throw new Error("Theme nicht gefunden");
        theme.tokens[operation.token] = operation.value;
        break;
      }
    }
  }
  return designDocumentSchema.parse(next);
}

export function validateDesignReferences(document: DesignDocument): string[] {
  const errors: string[] = [];
  const nodeIds = new Set(document.nodes.map((node) => node.id));
  const frameIds = new Set(document.frames.map((frame) => frame.id));
  for (const frame of document.frames) if (!nodeIds.has(frame.rootNodeId)) errors.push(`Frame ${frame.id}: rootNodeId fehlt`);
  for (const page of document.pages) for (const id of page.frameIds) if (!frameIds.has(id)) errors.push(`Page ${page.id}: Frame ${id} fehlt`);
  for (const node of document.nodes) for (const id of node.childIds) if (!nodeIds.has(id)) errors.push(`Node ${node.id}: Child ${id} fehlt`);
  return errors;
}

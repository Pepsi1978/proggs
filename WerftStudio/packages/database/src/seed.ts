import { hash } from "@node-rs/argon2";
import { v7 as uuidv7 } from "uuid";
import type { DesignDocument } from "@werft/contracts";
import { createDatabase, drafts, memberships, organizations, projects, users, versions } from "./index.js";
const { db, client } = createDatabase();
const organizationId = "0190f3d8-5800-7000-8000-000000000001";
const userId = "0190f3d8-5800-7000-8000-000000000002";
const projectId = "0190f3d8-5800-7000-8000-000000000003";
const pageId = "0190f3d8-5800-7000-8000-000000000004";
const frameId = "0190f3d8-5800-7000-8000-000000000005";
const nodeId = "0190f3d8-5800-7000-8000-000000000006";
const document: DesignDocument = {
  schemaVersion: 1, projectId, projectType: "prototype", fidelity: "high_fidelity", platforms: ["ios", "android", "windows", "web"], designSystemVersionId: null,
  themes: [{ id: "light", name: "Hell", tokens: { "color.bg": "#F5F7FA", "color.surface": "#FFFFFF", "color.accent": "#3157D5" } }, { id: "dark", name: "Dunkel", tokens: { "color.bg": "#12181F", "color.surface": "#1A222C", "color.accent": "#3157D5" } }],
  pages: [{ id: pageId, name: "Fluss", type: "screen", frameIds: [frameId] }],
  frames: [{ id: frameId, pageId, name: "Onboarding", platform: "ios", device: "iPhone 15", width: 390, height: 760, theme: "light", locale: "de-DE", rootNodeId: nodeId, canvasX: 0, canvasY: 0 }],
  nodes: [{ id: nodeId, name: "Onboarding", parentId: null, childIds: [], bounds: { x: 0, y: 0, width: 390, height: 760 }, visible: true, locked: false, tokenBindings: { fill: "color.surface" }, semantics: { role: "main", label: "Fluss Onboarding" }, type: "container", layout: "column", gap: 16, padding: [32, 32, 32, 32], fill: "color.surface", radius: 28 }],
  assets: [], interactions: [], metadata: { createdAt: "2026-07-20T18:00:00.000Z", compilerVersion: "0.1.0" }
};
await db.insert(organizations).values({ id: organizationId, name: "Werft Demo", slug: "werft-demo" }).onConflictDoNothing();
await db.insert(users).values({ id: userId, email: "frank@example.de", displayName: "Frank K.", passwordHash: await hash("werft-local-demo", { memoryCost: 19456, timeCost: 2, parallelism: 1 }) }).onConflictDoNothing();
await db.insert(memberships).values({ organizationId, userId, role: "organization_admin" }).onConflictDoNothing();
await db.insert(projects).values({ id: projectId, organizationId, name: "Fluss – Mobile Banking", type: "prototype", fidelity: "high_fidelity", platforms: ["ios", "android", "windows", "web"], ownerId: userId, activeVersion: 14 }).onConflictDoNothing();
await db.insert(drafts).values({ id: "0190f3d8-5800-7000-8000-000000000007", projectId, organizationId, revision: 0, document, updatedBy: userId }).onConflictDoNothing();
await db.insert(versions).values({ id: "0190f3d8-5800-7000-8000-000000000008", organizationId, projectId, number: 14, reason: "Referenzfixture", authorId: userId, document, snapshotHash: "209b09aeae0b7fafc1b4d86805283d6e3d9a8f0ad1ef08ea18ae41040cb3ec25" }).onConflictDoNothing();
console.log(`Seed ${uuidv7()}: frank@example.de / werft-local-demo (nur lokale Entwicklung)`);
await client.end();

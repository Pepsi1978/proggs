import { createHash, createHmac, randomBytes, timingSafeEqual } from "node:crypto";
import { verify } from "@node-rs/argon2";
import cookie from "@fastify/cookie";
import cors from "@fastify/cors";
import multipart from "@fastify/multipart";
import rateLimit from "@fastify/rate-limit";
import sensible from "@fastify/sensible";
import swagger from "@fastify/swagger";
import swaggerUi from "@fastify/swagger-ui";
import { can, type Role } from "@werft/authz";
import { applyOperationsSchema, createProjectSchema, designDocumentSchema, type DesignDocument } from "@werft/contracts";
import { auditEvents, createDatabase, designOperations, drafts, memberships, organizations, outboxEvents, projectImports, projects, sessions, userPreferences, users, versions } from "@werft/database";
import { applyDesignOperations, validateDesignReferences } from "@werft/design-model";
import { and, desc, eq, isNull, sql } from "drizzle-orm";
import Fastify, { type FastifyRequest } from "fastify";
import { Client as MinioClient } from "minio";
import { v7 as uuidv7 } from "uuid";
import { z } from "zod";
import { chooseEntryPath, expandZip, importLimits, mimeForPath, normalizeImportPath, type ImportedFile, validateImportFiles } from "./import-project.js";

type Actor = { userId: string; organizationId: string; role: Role };
declare module "fastify" { interface FastifyRequest { actor?: Actor } }
const localSecret = "local-development-secret-change-me";
const env = z.object({
  NODE_ENV: z.enum(["development", "test", "production"]).default("development"),
  API_PORT: z.coerce.number().int().positive().default(4100),
  WEB_ORIGIN: z.string().url().default("http://localhost:5173"),
  SESSION_SECRET: z.string().min(32).default(localSecret),
  DATABASE_URL: z.string().min(1).default("postgres://werft:werft@localhost:5432/werft"),
  S3_ENDPOINT: z.string().url().default("http://localhost:9000"),
  S3_BUCKET: z.string().min(3).default("werft"),
  S3_ACCESS_KEY: z.string().min(3).default("werft-local"),
  S3_SECRET_KEY: z.string().min(8).default("werft-local-secret")
}).parse(process.env);
if (env.NODE_ENV === "production" && env.SESSION_SECRET === localSecret) throw new Error("SESSION_SECRET muss in Produktion explizit gesetzt sein");
const { db, client } = createDatabase(env.DATABASE_URL);
const app = Fastify({ logger: { redact: ["req.headers.authorization", "req.headers.cookie", "body.password", "body.credential"] }, genReqId: () => uuidv7() });
const s3Endpoint = new URL(env.S3_ENDPOINT);
const objectStore = new MinioClient({ endPoint: s3Endpoint.hostname, port: Number(s3Endpoint.port || (s3Endpoint.protocol === "https:" ? 443 : 80)), useSSL: s3Endpoint.protocol === "https:", accessKey: env.S3_ACCESS_KEY, secretKey: env.S3_SECRET_KEY });
let bucketReady: Promise<void> | undefined;

await app.register(cors, { origin: env.WEB_ORIGIN, credentials: true, methods: ["GET", "POST", "PATCH", "PUT", "DELETE"] });
await app.register(multipart, { preservePath: true, limits: { files: importLimits.maxFiles, fileSize: importLimits.maxFileBytes, fields: 10 } });
await app.register(cookie, { secret: env.SESSION_SECRET, hook: "onRequest" });
await app.register(rateLimit, { max: 300, timeWindow: "1 minute" });
await app.register(sensible);
await app.register(swagger, { openapi: { info: { title: "Werft Studio API", version: "1.0.0" }, servers: [{ url: "/api/v1" }] } });
await app.register(swaggerUi, { routePrefix: "/docs" });

const hashToken = (token: string) => createHash("sha256").update(token).digest("hex");
const hashJson = (value: unknown) => createHash("sha256").update(JSON.stringify(value)).digest("hex");
const previewToken = (projectId: string) => createHmac("sha256", env.SESSION_SECRET).update(`preview:${projectId}`).digest("base64url");
const validPreviewToken = (projectId: string, token: string) => { const expected = Buffer.from(previewToken(projectId)); const actual = Buffer.from(token); return expected.length === actual.length && timingSafeEqual(expected, actual); };
const ensureBucket = () => bucketReady ??= (async () => { if (!(await objectStore.bucketExists(env.S3_BUCKET))) await objectStore.makeBucket(env.S3_BUCKET); })();
function fail(code: string, statusCode: number, message: string, retryable = false): never { throw Object.assign(new Error(message), { code, statusCode, retryable }); }

app.setErrorHandler((error, request, reply) => {
  const details = error && typeof error === "object" ? error as Record<string, unknown> : {};
  const normalized = error instanceof Error ? error : new Error("Unbekannter Serverfehler");
  const statusCode = typeof details.statusCode === "number" ? details.statusCode : error instanceof z.ZodError ? 400 : 500;
  const code = typeof details.code === "string" ? details.code : error instanceof z.ZodError ? "VALIDATION_FAILED" : "INTERNAL_ERROR";
  request.log.error({ err: error, code }, "request failed");
  return reply.status(statusCode).send({ code, message: statusCode >= 500 ? "Die Anfrage konnte nicht abgeschlossen werden." : normalized.message, retryable: details.retryable === true, correlationId: request.id });
});

app.addHook("onSend", async (request, reply) => { reply.header("x-correlation-id", request.id); });
app.addHook("preHandler", async (request) => {
  if (!request.url.startsWith("/api/v1/") || request.url.startsWith("/api/v1/previews/") || ["/api/v1/auth/login", "/api/v1/health/live", "/api/v1/health/ready"].includes(request.url.split("?")[0]!)) return;
  const raw = request.cookies.werft_session;
  if (!raw) fail("AUTH_REQUIRED", 401, "Bitte erneut anmelden.");
  const rows = await db.select({ userId: sessions.userId, organizationId: sessions.organizationId, role: memberships.role }).from(sessions).innerJoin(memberships, and(eq(memberships.userId, sessions.userId), eq(memberships.organizationId, sessions.organizationId))).where(and(eq(sessions.tokenHash, hashToken(raw)), isNull(sessions.revokedAt), sql`${sessions.expiresAt} > now()`, eq(memberships.status, "active"))).limit(1);
  const actor = rows[0];
  if (!actor) fail("SESSION_EXPIRED", 401, "Die Sitzung ist abgelaufen.");
  request.actor = { userId: actor.userId, organizationId: actor.organizationId, role: actor.role as Role };
});

function actorOf(request: FastifyRequest): Actor { if (!request.actor) fail("AUTH_REQUIRED", 401, "Bitte anmelden."); return request.actor; }
function requireActorPermission(request: FastifyRequest, permission: Parameters<typeof can>[1]) { const actor = actorOf(request); if (!can(actor.role, permission)) fail("FORBIDDEN", 403, "Diese Aktion ist für deine Rolle nicht erlaubt."); return actor; }
function importedDesignDocument(projectId: string, name: string): DesignDocument {
  const pageId = uuidv7(), frameId = uuidv7(), nodeId = uuidv7();
  return {
    schemaVersion: 1, projectId, projectType: "prototype", fidelity: "high_fidelity", platforms: ["web"], designSystemVersionId: null,
    themes: [{ id: "light", name: "Original", tokens: { "color.bg": "#F5F7FA", "color.surface": "#FFFFFF", "color.accent": "#3157D5" } }],
    pages: [{ id: pageId, name, type: "screen", frameIds: [frameId] }],
    frames: [{ id: frameId, pageId, name, platform: "web", device: "Importierte Originalgröße", width: 1440, height: 900, theme: "light", locale: "de-DE", rootNodeId: nodeId, canvasX: 0, canvasY: 0 }],
    nodes: [{ id: nodeId, name: "Interaktive HTML-Vorschau", parentId: null, childIds: [], bounds: { x: 0, y: 0, width: 1440, height: 900 }, visible: true, locked: false, tokenBindings: {}, semantics: { role: "main", label: name }, type: "container", layout: "absolute", gap: 0, padding: [0, 0, 0, 0], fill: "color.surface" }],
    assets: [], interactions: [], metadata: { createdAt: new Date().toISOString(), compilerVersion: "0.1.0" }
  };
}

async function readImportParts(request: FastifyRequest): Promise<{ name: string; files: ImportedFile[] }> {
  let name = "Importiertes Design", totalBytes = 0;
  const files: ImportedFile[] = [];
  for await (const part of request.parts()) {
    if (part.type === "field") {
      if (part.fieldname === "name" && typeof part.value === "string") name = part.value.trim().slice(0, 120) || name;
      continue;
    }
    const chunks: Buffer[] = [];
    for await (const chunk of part.file) {
      const buffer = Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk);
      totalBytes += buffer.byteLength;
      if (totalBytes > importLimits.maxTotalBytes) fail("IMPORT_TOO_LARGE", 413, "Das Importpaket ist größer als 100 MB.");
      chunks.push(buffer);
    }
    if (part.file.truncated) fail("IMPORT_FILE_TOO_LARGE", 413, `Die Datei „${part.filename}“ ist größer als 50 MB.`);
    const filePath = normalizeImportPath(part.filename);
    files.push({ path: filePath, data: Buffer.concat(chunks), mime: part.mimetype || mimeForPath(filePath) });
  }
  if (files.length === 1 && files[0]!.path.toLowerCase().endsWith(".zip")) return { name, files: await expandZip(files[0]!.data) };
  if (files.some((file) => file.path.toLowerCase().endsWith(".zip"))) fail("IMPORT_ARCHIVE_MIXED", 400, "Ein ZIP muss einzeln ausgewählt werden; Ordnerdateien bitte ohne zusätzliches ZIP importieren.");
  return { name, files: validateImportFiles(files) };
}

app.get("/api/v1/health/live", async () => ({ status: "ok", version: "0.1.10-20260720.2105" }));
app.get("/api/v1/health/ready", async () => { await client`select 1`; return { status: "ready", database: "ok" }; });
app.get("/api/v1/previews/:projectId/:token/*", { config: { rateLimit: false } }, async (request, reply) => {
  const params = z.object({ projectId: z.string().uuid(), token: z.string().min(40), "*": z.string() }).parse(request.params);
  if (!validPreviewToken(params.projectId, params.token)) fail("PREVIEW_NOT_FOUND", 404, "Vorschau nicht gefunden.");
  const imported = (await db.select().from(projectImports).where(eq(projectImports.projectId, params.projectId)).limit(1))[0];
  if (!imported) fail("PREVIEW_NOT_FOUND", 404, "Vorschau nicht gefunden.");
  const requestedPath = normalizeImportPath(params["*"] || imported.entryPath);
  let item = imported.manifest.find((file) => file.path === requestedPath);
  if (!item && !requestedPath.split("/").at(-1)?.includes(".")) item = imported.manifest.find((file) => file.path === imported.entryPath);
  if (!item) fail("PREVIEW_FILE_NOT_FOUND", 404, "Vorschaudatei nicht gefunden.");
  reply.headers({ "cache-control": "no-store", "x-content-type-options": "nosniff", "referrer-policy": "no-referrer" }).type(item.mime);
  if (item.mime.startsWith("text/html")) reply.header("content-security-policy", "default-src 'self' data: blob: https: http:; script-src 'self' 'unsafe-inline' 'unsafe-eval' blob: https: http:; style-src 'self' 'unsafe-inline' blob: https: http:; img-src 'self' data: blob: https: http:; font-src 'self' data: blob: https: http:; media-src 'self' data: blob: https: http:; connect-src 'self' https: http: wss: ws:; frame-src 'self' blob: https: http:; frame-ancestors https://10.8.0.1:8443 http://localhost:5173");
  return reply.send(await objectStore.getObject(env.S3_BUCKET, `${imported.objectPrefix}${item.path}`));
});

app.post("/api/v1/auth/login", { config: { rateLimit: { max: 10, timeWindow: "5 minutes" } } }, async (request, reply) => {
  const body = z.object({ email: z.string().email(), password: z.string().min(1) }).strict().parse(request.body);
  const rows = await db.select({ userId: users.id, organizationId: memberships.organizationId, passwordHash: users.passwordHash }).from(users).innerJoin(memberships, eq(memberships.userId, users.id)).where(and(eq(users.email, body.email.toLowerCase()), eq(memberships.status, "active"))).limit(1);
  const account = rows[0];
  if (!account?.passwordHash || !(await verify(account.passwordHash, body.password))) fail("AUTH_INVALID", 401, "E-Mail oder Passwort ist ungültig.");
  const token = randomBytes(32).toString("base64url");
  await db.insert(sessions).values({ id: uuidv7(), userId: account.userId, organizationId: account.organizationId, tokenHash: hashToken(token), expiresAt: new Date(Date.now() + 8 * 60 * 60 * 1000) });
  reply.setCookie("werft_session", token, { httpOnly: true, secure: env.NODE_ENV === "production", sameSite: "strict", path: "/", maxAge: 8 * 60 * 60 });
  return reply.send({ authenticated: true });
});

app.post("/api/v1/auth/logout", async (request, reply) => { const raw = request.cookies.werft_session; if (raw) await db.update(sessions).set({ revokedAt: new Date() }).where(eq(sessions.tokenHash, hashToken(raw))); reply.clearCookie("werft_session", { path: "/" }); return { authenticated: false }; });
app.get("/api/v1/me", async (request) => { const actor = actorOf(request); const rows = await db.select({ id: users.id, email: users.email, name: users.displayName, locale: users.locale, timeZone: users.timeZone, organizationName: organizations.name }).from(users).innerJoin(organizations, eq(organizations.id, actor.organizationId)).where(eq(users.id, actor.userId)).limit(1); return { ...rows[0], role: actor.role }; });
app.get("/api/v1/me/preferences", async (request) => { const actor = actorOf(request); const rows = await db.select().from(userPreferences).where(and(eq(userPreferences.userId, actor.userId), eq(userPreferences.organizationId, actor.organizationId))).limit(1); return rows[0] ?? { revision: 0, values: {} }; });
app.patch("/api/v1/me/preferences", async (request) => { const actor = actorOf(request); const body = z.object({ baseRevision: z.number().int().nonnegative(), values: z.record(z.string(), z.unknown()) }).parse(request.body); const updated = await db.insert(userPreferences).values({ userId: actor.userId, organizationId: actor.organizationId, revision: 1, values: body.values }).onConflictDoUpdate({ target: [userPreferences.userId, userPreferences.organizationId], set: { revision: sql`${userPreferences.revision} + 1`, values: body.values, updatedAt: new Date() }, setWhere: eq(userPreferences.revision, body.baseRevision) }).returning(); if (!updated[0]) fail("REVISION_CONFLICT", 409, "Einstellungen wurden in einer anderen Sitzung geändert."); return updated[0]; });

app.get("/api/v1/projects", async (request) => { const actor = requireActorPermission(request, "project.read"); return db.select().from(projects).where(and(eq(projects.organizationId, actor.organizationId), isNull(projects.deletedAt))).orderBy(desc(projects.updatedAt)).limit(100); });
app.post("/api/v1/projects", async (request, reply) => {
  const actor = requireActorPermission(request, "project.update"); const input = createProjectSchema.parse(request.body); const projectId = uuidv7(), pageId = uuidv7(), frameId = uuidv7(), nodeId = uuidv7(), draftId = uuidv7(), versionId = uuidv7();
  const document: DesignDocument = { schemaVersion: 1, projectId, projectType: input.type, fidelity: input.fidelity, platforms: input.platforms, designSystemVersionId: input.designSystemVersionId, themes: [{ id: "light", name: "Hell", tokens: { "color.bg": "#F5F7FA", "color.surface": "#FFFFFF", "color.accent": "#3157D5" } }], pages: [{ id: pageId, name: "Start", type: input.type === "presentation" ? "slide" : input.type === "document" ? "document-page" : input.type === "canvas" ? "canvas" : "screen", frameIds: [frameId] }], frames: [{ id: frameId, pageId, name: "Start", platform: input.platforms[0]!, device: "Standard", width: 390, height: 760, theme: "light", locale: "de-DE", rootNodeId: nodeId, canvasX: 0, canvasY: 0 }], nodes: [{ id: nodeId, name: "Start", parentId: null, childIds: [], bounds: { x: 0, y: 0, width: 390, height: 760 }, visible: true, locked: false, tokenBindings: {}, semantics: { role: "main", label: "Start" }, type: "container", layout: "column", gap: 16, padding: [24, 24, 24, 24], fill: "color.surface" }], assets: [], interactions: [], metadata: { createdAt: new Date().toISOString(), compilerVersion: "0.1.0" } };
  await db.transaction(async (tx) => { await tx.insert(projects).values({ id: projectId, organizationId: actor.organizationId, name: input.name, type: input.type, fidelity: input.fidelity, platforms: input.platforms, ownerId: actor.userId }); await tx.insert(drafts).values({ id: draftId, projectId, organizationId: actor.organizationId, document, updatedBy: actor.userId }); await tx.insert(versions).values({ id: versionId, organizationId: actor.organizationId, projectId, number: 1, reason: "Projekt angelegt", authorId: actor.userId, document, snapshotHash: hashJson(document) }); await tx.insert(auditEvents).values({ id: uuidv7(), organizationId: actor.organizationId, actorId: actor.userId, action: "project.created", targetType: "project", targetId: projectId, result: "success", correlationId: request.id }); });
  return reply.status(201).send({ projectId, version: 1, revision: 0 });
});

app.post("/api/v1/imports", { bodyLimit: importLimits.maxTotalBytes + 1024 * 1024 }, async (request, reply) => {
  const actor = requireActorPermission(request, "project.update");
  if (!request.isMultipart()) fail("IMPORT_MULTIPART_REQUIRED", 415, "Bitte Dateien oder ein ZIP-Paket auswählen.");
  const { name, files } = await readImportParts(request);
  const projectId = uuidv7(), draftId = uuidv7(), versionId = uuidv7();
  const nativeFile = files.find((file) => /(^|\/)(design-document\.json|[^/]+\.werft)$/i.test(file.path)) ?? (files.length === 1 && files[0]!.path.toLowerCase().endsWith(".json") ? files[0] : undefined);
  let candidate: unknown;
  if (nativeFile) {
    try { candidate = JSON.parse(nativeFile.data.toString("utf8")); }
    catch { fail("IMPORT_JSON_INVALID", 400, "Die ausgewählte Werft-/JSON-Datei enthält kein gültiges JSON."); }
  }
  const parsedNative = candidate === undefined ? undefined : designDocumentSchema.safeParse(typeof candidate === "object" && candidate !== null && "document" in candidate ? (candidate as { document: unknown }).document : candidate);
  if (parsedNative && !parsedNative.success) fail("IMPORT_DOCUMENT_INVALID", 400, "Die Werft-/JSON-Datei ist kein gültiges DesignDocument.");
  const nativeDocument = parsedNative?.success ? designDocumentSchema.parse({ ...parsedNative.data, projectId }) : undefined;
  const entryPath = nativeDocument ? undefined : chooseEntryPath(files);
  if (!nativeDocument && !entryPath) fail("IMPORT_ENTRY_MISSING", 400, "Kein startbares HTML gefunden. Wähle den vollständigen Claude-Designs-Ordner oder ein ZIP mit mindestens einer HTML-Datei.");
  const document = nativeDocument ?? importedDesignDocument(projectId, name);
  const storedKeys: string[] = [];
  const objectPrefix = `${actor.organizationId}/${projectId}/`;
  try {
    if (entryPath) {
      await ensureBucket();
      for (const file of files) {
        const key = `${objectPrefix}${file.path}`;
        await objectStore.putObject(env.S3_BUCKET, key, file.data, file.data.byteLength, { "content-type": file.mime });
        storedKeys.push(key);
      }
    }
    await db.transaction(async (tx) => {
      await tx.insert(projects).values({ id: projectId, organizationId: actor.organizationId, name, type: document.projectType, fidelity: document.fidelity, platforms: document.platforms, ownerId: actor.userId });
      await tx.insert(drafts).values({ id: draftId, projectId, organizationId: actor.organizationId, document, updatedBy: actor.userId });
      await tx.insert(versions).values({ id: versionId, organizationId: actor.organizationId, projectId, number: 1, reason: "Projekt importiert", authorId: actor.userId, document, snapshotHash: hashJson(document) });
      if (entryPath) await tx.insert(projectImports).values({ projectId, organizationId: actor.organizationId, format: "html-project", entryPath, objectPrefix, manifest: files.map((file) => ({ path: file.path, size: file.data.byteLength, mime: file.mime })), fileCount: files.length, totalBytes: files.reduce((sum, file) => sum + file.data.byteLength, 0) });
      await tx.insert(auditEvents).values({ id: uuidv7(), organizationId: actor.organizationId, actorId: actor.userId, action: "project.imported", targetType: "project", targetId: projectId, result: "success", metadata: { format: entryPath ? "html-project" : "design-document", fileCount: files.length }, correlationId: request.id });
    });
  } catch (error) {
    await Promise.allSettled(storedKeys.map((key) => objectStore.removeObject(env.S3_BUCKET, key)));
    throw error;
  }
  return reply.status(201).send({ projectId, kind: entryPath ? "html" : "native" });
});

app.get("/api/v1/projects/:projectId", async (request) => { const actor = requireActorPermission(request, "project.read"); const { projectId } = z.object({ projectId: z.string().uuid() }).parse(request.params); const rows = await db.select().from(projects).where(and(eq(projects.id, projectId), eq(projects.organizationId, actor.organizationId), isNull(projects.deletedAt))).limit(1); if (!rows[0]) fail("NOT_FOUND", 404, "Projekt nicht gefunden."); return rows[0]; });
app.get("/api/v1/projects/:projectId/import", async (request) => { const actor = requireActorPermission(request, "project.read"); const { projectId } = z.object({ projectId: z.string().uuid() }).parse(request.params); const imported = (await db.select().from(projectImports).where(and(eq(projectImports.projectId, projectId), eq(projectImports.organizationId, actor.organizationId))).limit(1))[0]; if (!imported) return { imported: false as const }; return { imported: true as const, entryPath: imported.entryPath, fileCount: imported.fileCount, totalBytes: imported.totalBytes, previewPath: `/api/v1/previews/${projectId}/${previewToken(projectId)}/${imported.entryPath}` }; });
app.get("/api/v1/projects/:projectId/design-document", async (request) => { const actor = requireActorPermission(request, "design.read"); const { projectId } = z.object({ projectId: z.string().uuid() }).parse(request.params); const rows = await db.select({ revision: drafts.revision, document: drafts.document }).from(drafts).where(and(eq(drafts.projectId, projectId), eq(drafts.organizationId, actor.organizationId))).limit(1); if (!rows[0]) fail("NOT_FOUND", 404, "Design nicht gefunden."); return { revision: rows[0].revision, document: designDocumentSchema.parse(rows[0].document) }; });
app.post("/api/v1/projects/:projectId/design-operations", async (request) => {
  const actor = requireActorPermission(request, "design.edit"); const { projectId } = z.object({ projectId: z.string().uuid() }).parse(request.params); const command = applyOperationsSchema.parse(request.body);
  return db.transaction(async (tx) => {
    const existing = await tx.select({ revision: designOperations.revision }).from(designOperations).where(and(eq(designOperations.projectId, projectId), eq(designOperations.actionId, command.actionId), eq(designOperations.organizationId, actor.organizationId))).limit(1); if (existing[0]) return { revision: existing[0].revision, idempotent: true };
    await tx.execute(sql`select id from drafts where project_id = ${projectId} and organization_id = ${actor.organizationId} for update`);
    const rows = await tx.select().from(drafts).where(and(eq(drafts.projectId, projectId), eq(drafts.organizationId, actor.organizationId))).limit(1); const draft = rows[0]; if (!draft) fail("NOT_FOUND", 404, "Design nicht gefunden."); if (draft.revision !== command.baseRevision) fail("REVISION_CONFLICT", 409, `Aktuelle Revision ist ${draft.revision}.`);
    const document = applyDesignOperations(designDocumentSchema.parse(draft.document), command.operations); const references = validateDesignReferences(document); if (references.length) fail("VALIDATION_FAILED", 400, references.join("; "));
    const revision = draft.revision + 1; await tx.update(drafts).set({ revision, document, updatedBy: actor.userId, updatedAt: new Date() }).where(eq(drafts.id, draft.id)); await tx.insert(designOperations).values({ id: uuidv7(), organizationId: actor.organizationId, projectId, draftId: draft.id, revision, actionId: command.actionId, actorId: actor.userId, operations: command.operations }); await tx.insert(auditEvents).values({ id: uuidv7(), organizationId: actor.organizationId, actorId: actor.userId, action: "design.operation.applied", targetType: "project", targetId: projectId, result: "success", metadata: { revision, operationCount: command.operations.length }, correlationId: request.id }); await tx.insert(outboxEvents).values({ id: uuidv7(), organizationId: actor.organizationId, aggregateId: projectId, type: "draft.operation.applied", payload: { projectId, revision, actorId: actor.userId } }); return { revision, idempotent: false };
  });
});

app.get("/api/v1/projects/:projectId/versions", async (request) => { const actor = requireActorPermission(request, "design.read"); const { projectId } = z.object({ projectId: z.string().uuid() }).parse(request.params); return db.select({ id: versions.id, number: versions.number, reason: versions.reason, createdAt: versions.createdAt, snapshotHash: versions.snapshotHash }).from(versions).where(and(eq(versions.projectId, projectId), eq(versions.organizationId, actor.organizationId))).orderBy(desc(versions.number)); });
app.post("/api/v1/projects/:projectId/versions", async (request) => { const actor = requireActorPermission(request, "version.create"); const { projectId } = z.object({ projectId: z.string().uuid() }).parse(request.params); const body = z.object({ reason: z.string().min(1).max(300), baseRevision: z.number().int().nonnegative() }).parse(request.body); return db.transaction(async (tx) => { await tx.execute(sql`select id from projects where id = ${projectId} and organization_id = ${actor.organizationId} for update`); const project = (await tx.select().from(projects).where(and(eq(projects.id, projectId), eq(projects.organizationId, actor.organizationId))).limit(1))[0]; const draft = (await tx.select().from(drafts).where(and(eq(drafts.projectId, projectId), eq(drafts.organizationId, actor.organizationId))).limit(1))[0]; if (!project || !draft) fail("NOT_FOUND", 404, "Projekt nicht gefunden."); if (draft.revision !== body.baseRevision) fail("REVISION_CONFLICT", 409, "Der Entwurf wurde inzwischen geändert."); const number = project.activeVersion + 1; const document = designDocumentSchema.parse(draft.document); const row = { id: uuidv7(), organizationId: actor.organizationId, projectId, number, reason: body.reason, authorId: actor.userId, document, snapshotHash: hashJson(document) }; await tx.insert(versions).values(row); await tx.update(projects).set({ activeVersion: number, updatedAt: new Date() }).where(eq(projects.id, projectId)); await tx.insert(outboxEvents).values({ id: uuidv7(), organizationId: actor.organizationId, aggregateId: projectId, type: "version.created", payload: { projectId, number, actorId: actor.userId } }); return { id: row.id, number }; }); });
app.get("/api/v1/audit-events", async (request) => { const actor = requireActorPermission(request, "audit.read"); return db.select().from(auditEvents).where(eq(auditEvents.organizationId, actor.organizationId)).orderBy(desc(auditEvents.createdAt)).limit(100); });

const shutdown = async () => { await app.close(); await client.end(); };
process.on("SIGINT", shutdown); process.on("SIGTERM", shutdown);
await app.listen({ port: env.API_PORT, host: "0.0.0.0" });

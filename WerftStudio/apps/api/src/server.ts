import { createHash, createHmac, timingSafeEqual } from "node:crypto";
import cors from "@fastify/cors";
import multipart from "@fastify/multipart";
import rateLimit from "@fastify/rate-limit";
import sensible from "@fastify/sensible";
import swagger from "@fastify/swagger";
import swaggerUi from "@fastify/swagger-ui";
import { can, type Role } from "@werft/authz";
import { applyOperationsSchema, createProjectSchema, designDocumentSchema, type DesignDocument } from "@werft/contracts";
import { auditEvents, createDatabase, designOperations, drafts, memberships, organizations, outboxEvents, projectImports, projects, providerConnections, userPreferences, users, versions } from "@werft/database";
import { applyDesignOperations, validateDesignReferences } from "@werft/design-model";
import { and, desc, eq, isNull, sql } from "drizzle-orm";
import Fastify, { type FastifyRequest } from "fastify";
import { Client as MinioClient } from "minio";
import { v7 as uuidv7 } from "uuid";
import { z } from "zod";
import { codexAuth, codexEfforts, codexModelFields, codexModels, decryptCredentials, encryptCredentials, tokenIdentity } from "./codex-auth.js";
import { chooseEntryPath, expandZip, importLimits, mimeForPath, normalizeImportPath, type ImportedFile, validateImportFiles } from "./import-project.js";

type Actor = { userId: string; organizationId: string; role: Role };
type PendingCodexAuth = { userId: string; organizationId: string; deviceAuthId: string; userCode: string; expiresAt: number; interval: number };
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
const pendingCodexAuth = new Map<string, PendingCodexAuth>();

await app.register(cors, { origin: env.WEB_ORIGIN, credentials: true, methods: ["GET", "POST", "PATCH", "PUT", "DELETE"] });
await app.register(multipart, { preservePath: true, limits: { files: importLimits.maxFiles, fileSize: importLimits.maxFileBytes, fields: 10 } });
await app.register(rateLimit, { max: 300, timeWindow: "1 minute" });
await app.register(sensible);
await app.register(swagger, { openapi: { info: { title: "Werft Studio API", version: "1.0.0" }, servers: [{ url: "/api/v1" }] } });
await app.register(swaggerUi, { routePrefix: "/docs" });

const hashJson = (value: unknown) => createHash("sha256").update(JSON.stringify(value)).digest("hex");
const previewToken = (projectId: string) => createHmac("sha256", env.SESSION_SECRET).update(`preview:${projectId}`).digest("base64url");
const validPreviewToken = (projectId: string, token: string) => { const expected = Buffer.from(previewToken(projectId)); const actual = Buffer.from(token); return expected.length === actual.length && timingSafeEqual(expected, actual); };
const ensureBucket = () => bucketReady ??= (async () => { if (!(await objectStore.bucketExists(env.S3_BUCKET))) await objectStore.makeBucket(env.S3_BUCKET); })();
const editableImportMime = (mime: string) => mime.startsWith("text/") || ["application/json", "image/svg+xml"].some((type) => mime.startsWith(type));
const readObject = async (key: string) => { const chunks: Buffer[] = []; for await (const chunk of await objectStore.getObject(env.S3_BUCKET, key)) chunks.push(Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk)); return Buffer.concat(chunks); };
const codexPost = async (url: string, body: unknown, form = false) => fetch(url, { method: "POST", signal: AbortSignal.timeout(15_000), headers: { "content-type": form ? "application/x-www-form-urlencoded" : "application/json", accept: "application/json" }, body: form ? new URLSearchParams(body as Record<string, string>) : JSON.stringify(body) });
const clearExpiredCodexAuth = () => { const now = Date.now(); for (const [id, pending] of pendingCodexAuth) if (pending.expiresAt <= now) pendingCodexAuth.delete(id); };
async function validCodexConnection(organizationId: string) {
  const row = (await db.select().from(providerConnections).where(and(eq(providerConnections.organizationId, organizationId), eq(providerConnections.provider, "openai-codex"))).limit(1))[0];
  if (!row) fail("OPENAI_NOT_CONNECTED", 409, "Bitte zuerst OpenAI verbinden.");
  let credentials = decryptCredentials(row.credentials, env.SESSION_SECRET), expiresAt = row.expiresAt;
  if (!expiresAt || expiresAt.getTime() <= Date.now() + 120_000) {
    if (!credentials.refreshToken) fail("OPENAI_REAUTH_REQUIRED", 401, "Die OpenAI-Anmeldung ist abgelaufen. Bitte erneut verbinden.");
    const refreshed = await codexPost(codexAuth.tokenUrl, { grant_type: "refresh_token", refresh_token: credentials.refreshToken, client_id: codexAuth.clientId }, true);
    if (!refreshed.ok) fail("OPENAI_REAUTH_REQUIRED", 401, "OpenAI konnte die Anmeldung nicht erneuern. Bitte erneut verbinden.");
    const tokens = await refreshed.json() as { access_token?: string; refresh_token?: string; id_token?: string; expires_in?: number };
    if (!tokens.access_token) fail("OPENAI_TOKEN_INVALID", 502, "OpenAI hat beim Erneuern keinen Zugriffstoken geliefert.");
    credentials = { accessToken: tokens.access_token, ...(tokens.refresh_token || credentials.refreshToken ? { refreshToken: tokens.refresh_token || credentials.refreshToken } : {}), ...(tokens.id_token || credentials.idToken ? { idToken: tokens.id_token || credentials.idToken } : {}) };
    expiresAt = new Date(Date.now() + (Number(tokens.expires_in) || 3600) * 1000);
    const identity = tokenIdentity(credentials.accessToken, credentials.idToken);
    await db.update(providerConnections).set({ credentials: encryptCredentials(credentials, env.SESSION_SECRET), expiresAt, accountId: identity.accountId || row.accountId, email: identity.email || row.email, updatedAt: new Date() }).where(eq(providerConnections.id, row.id));
  }
  return { ...row, credentials, expiresAt };
}
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
// Einzelbenutzer-Betrieb hinter WireGuard: keine Anmeldung, der Akteur kommt aus der Seed-Mitgliedschaft.
let defaultActor: Actor | undefined;
async function resolveDefaultActor(): Promise<Actor> {
  if (defaultActor) return defaultActor;
  const rows = await db.select({ userId: memberships.userId, organizationId: memberships.organizationId, role: memberships.role }).from(memberships).where(eq(memberships.status, "active")).orderBy(sql`case when ${memberships.role} = 'organization_admin' then 0 else 1 end`, memberships.createdAt).limit(1);
  const row = rows[0];
  if (!row) fail("SETUP_REQUIRED", 503, "Kein Benutzerkonto vorhanden. Bitte den Seed ausführen.", true);
  defaultActor = { userId: row.userId, organizationId: row.organizationId, role: row.role as Role };
  return defaultActor;
}
app.addHook("preHandler", async (request) => {
  if (!request.url.startsWith("/api/v1/") || request.url.startsWith("/api/v1/previews/") || ["/api/v1/health/live", "/api/v1/health/ready"].includes(request.url.split("?")[0]!)) return;
  request.actor = await resolveDefaultActor();
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

app.get("/api/v1/health/live", async () => ({ status: "ok", version: "0.1.17-20260721.1835" }));
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

app.get("/api/v1/me", async (request) => { const actor = actorOf(request); const rows = await db.select({ id: users.id, email: users.email, name: users.displayName, locale: users.locale, timeZone: users.timeZone, organizationName: organizations.name }).from(users).innerJoin(organizations, eq(organizations.id, actor.organizationId)).where(eq(users.id, actor.userId)).limit(1); return { ...rows[0], role: actor.role }; });
app.get("/api/v1/me/preferences", async (request) => { const actor = actorOf(request); const rows = await db.select().from(userPreferences).where(and(eq(userPreferences.userId, actor.userId), eq(userPreferences.organizationId, actor.organizationId))).limit(1); return rows[0] ?? { revision: 0, values: {} }; });
app.patch("/api/v1/me/preferences", async (request) => { const actor = actorOf(request); const body = z.object({ baseRevision: z.number().int().nonnegative(), values: z.record(z.string(), z.unknown()) }).parse(request.body); const updated = await db.insert(userPreferences).values({ userId: actor.userId, organizationId: actor.organizationId, revision: 1, values: body.values }).onConflictDoUpdate({ target: [userPreferences.userId, userPreferences.organizationId], set: { revision: sql`${userPreferences.revision} + 1`, values: body.values, updatedAt: new Date() }, setWhere: eq(userPreferences.revision, body.baseRevision) }).returning(); if (!updated[0]) fail("REVISION_CONFLICT", 409, "Einstellungen wurden in einer anderen Sitzung geändert."); return updated[0]; });

app.get("/api/v1/providers/openai", async (request) => { const actor = requireActorPermission(request, "provider.read"); const row = (await db.select({ status: providerConnections.status, email: providerConnections.email, accountId: providerConnections.accountId, expiresAt: providerConnections.expiresAt, settings: providerConnections.settings }).from(providerConnections).where(and(eq(providerConnections.organizationId, actor.organizationId), eq(providerConnections.provider, "openai-codex"))).limit(1))[0]; return row ? { connected: row.status === "connected", ...row } : { connected: false, status: "disconnected", settings: {} }; });
app.patch("/api/v1/providers/openai/settings", async (request) => { const actor = requireActorPermission(request, "provider.manage"); const settings = z.object({ model: z.enum(codexModels), effort: z.enum(codexEfforts), fast: z.boolean() }).strict().parse(request.body); const updated = await db.update(providerConnections).set({ settings, updatedAt: new Date() }).where(and(eq(providerConnections.organizationId, actor.organizationId), eq(providerConnections.provider, "openai-codex"))).returning({ id: providerConnections.id }); if (!updated[0]) fail("OPENAI_NOT_CONNECTED", 409, "Bitte zuerst OpenAI verbinden."); return { settings }; });
app.post("/api/v1/providers/openai/test", { config: { rateLimit: { max: 5, timeWindow: "5 minutes" } } }, async (request) => {
  const actor = requireActorPermission(request, "provider.use"), connection = await validCodexConnection(actor.organizationId);
  const settings = z.object({ model: z.enum(codexModels).default("gpt-5.6-sol"), effort: z.enum(codexEfforts).default("high"), fast: z.boolean().default(false) }).parse(connection.settings);
  const accountId = connection.accountId || tokenIdentity(connection.credentials.accessToken, connection.credentials.idToken).accountId;
  if (!accountId) fail("OPENAI_ACCOUNT_MISSING", 401, "Im OpenAI-Token fehlt die ChatGPT-Account-ID. Bitte erneut verbinden.");
  const body: Record<string, unknown> = { ...codexModelFields(settings.model, settings.fast), instructions: "Antworte ausschließlich mit OK.", input: [{ role: "user", content: "Verbindungstest" }], store: false, stream: true };
  if (settings.effort !== "none") { body.reasoning = { effort: settings.effort, summary: "auto" }; body.include = ["reasoning.encrypted_content"]; }
  const started = Date.now();
  const response = await fetch(codexAuth.responsesUrl, { method: "POST", signal: AbortSignal.timeout(120_000), headers: { authorization: `Bearer ${connection.credentials.accessToken}`, "chatgpt-account-id": accountId, originator: "codex_cli_rs", "user-agent": "codex_cli_rs/0.0.0 (Werft Studio)", accept: "text/event-stream", "content-type": "application/json" }, body: JSON.stringify(body) });
  const responseText = await response.text();
  if (!response.ok) fail("OPENAI_TEST_FAILED", 502, `OpenAI hat den Verbindungstest abgelehnt (HTTP ${response.status}).`);
  if (!responseText.includes("response.completed")) fail("OPENAI_TEST_INCOMPLETE", 502, "OpenAI hat den Verbindungstest nicht vollständig abgeschlossen.", true);
  return { ok: true, model: settings.model, effort: settings.effort, fast: settings.fast, elapsedMs: Date.now() - started };
});
app.post("/api/v1/providers/openai/auth/start", { config: { rateLimit: { max: 5, timeWindow: "10 minutes" } } }, async (request) => {
  const actor = requireActorPermission(request, "provider.manage");
  clearExpiredCodexAuth();
  const response = await codexPost(codexAuth.userCodeUrl, { client_id: codexAuth.clientId });
  if (!response.ok) fail("OPENAI_AUTH_START_FAILED", 502, `OpenAI-Geräteanmeldung konnte nicht gestartet werden (HTTP ${response.status}).`, true);
  const data = await response.json() as { user_code?: string; device_auth_id?: string; interval?: number };
  const userCode = data.user_code?.trim(), deviceAuthId = data.device_auth_id?.trim();
  if (!userCode || !deviceAuthId) fail("OPENAI_AUTH_INVALID", 502, "OpenAI hat keinen vollständigen Gerätecode geliefert.");
  const authId = uuidv7(), expiresAt = Date.now() + 15 * 60 * 1000, interval = Math.max(3, Math.min(30, Number(data.interval) || 5));
  pendingCodexAuth.set(authId, { userId: actor.userId, organizationId: actor.organizationId, deviceAuthId, userCode, expiresAt, interval });
  return { authId, userCode, verificationUri: codexAuth.verificationUrl, expiresAt: new Date(expiresAt).toISOString(), interval };
});
app.post("/api/v1/providers/openai/auth/poll", { config: { rateLimit: { max: 120, timeWindow: "15 minutes" } } }, async (request) => {
  const actor = requireActorPermission(request, "provider.manage");
  const { authId } = z.object({ authId: z.string().uuid() }).strict().parse(request.body);
  clearExpiredCodexAuth();
  const pending = pendingCodexAuth.get(authId);
  if (!pending || pending.organizationId !== actor.organizationId || pending.userId !== actor.userId) return { status: "expired", connected: false };
  let poll: Response;
  try { poll = await codexPost(codexAuth.deviceTokenUrl, { device_auth_id: pending.deviceAuthId, user_code: pending.userCode }); }
  catch { return { status: "pending", connected: false, interval: Math.min(30, pending.interval + 2) }; }
  if ([403, 404, 429].includes(poll.status) || poll.status >= 500) return { status: "pending", connected: false, interval: Math.min(30, pending.interval + (poll.status === 429 ? 5 : 0)) };
  if (!poll.ok) fail("OPENAI_AUTH_REJECTED", 400, "Die OpenAI-Anmeldung wurde abgelehnt oder ist abgelaufen.");
  const authorization = await poll.json() as { authorization_code?: string; code_verifier?: string };
  if (!authorization.authorization_code || !authorization.code_verifier) fail("OPENAI_AUTH_INVALID", 502, "OpenAI hat keinen vollständigen Autorisierungscode geliefert.");
  const tokenResponse = await codexPost(codexAuth.tokenUrl, { grant_type: "authorization_code", code: authorization.authorization_code, redirect_uri: codexAuth.redirectUrl, client_id: codexAuth.clientId, code_verifier: authorization.code_verifier }, true);
  if (!tokenResponse.ok) return { status: "pending", connected: false, interval: Math.min(30, pending.interval + 2) };
  const tokens = await tokenResponse.json() as { access_token?: string; refresh_token?: string; id_token?: string; expires_in?: number };
  if (!tokens.access_token) fail("OPENAI_TOKEN_INVALID", 502, "OpenAI hat keinen Zugriffstoken geliefert.");
  const identity = tokenIdentity(tokens.access_token, tokens.id_token), expiresAt = new Date(Date.now() + (Number(tokens.expires_in) || 3600) * 1000);
  const credentials = encryptCredentials({ accessToken: tokens.access_token, ...(tokens.refresh_token ? { refreshToken: tokens.refresh_token } : {}), ...(tokens.id_token ? { idToken: tokens.id_token } : {}) }, env.SESSION_SECRET);
  await db.transaction(async (tx) => { await tx.insert(providerConnections).values({ id: uuidv7(), organizationId: actor.organizationId, provider: "openai-codex", credentials, accountId: identity.accountId, email: identity.email, expiresAt }).onConflictDoUpdate({ target: [providerConnections.organizationId, providerConnections.provider], set: { credentials, accountId: identity.accountId, email: identity.email, expiresAt, status: "connected", updatedAt: new Date() } }); await tx.insert(auditEvents).values({ id: uuidv7(), organizationId: actor.organizationId, actorId: actor.userId, action: "provider.openai.connected", targetType: "provider", result: "success", metadata: { provider: "openai-codex" }, correlationId: request.id }); });
  pendingCodexAuth.delete(authId);
  return { status: "connected", connected: true, email: identity.email, accountId: identity.accountId };
});
app.delete("/api/v1/providers/openai", async (request) => { const actor = requireActorPermission(request, "provider.manage"); await db.delete(providerConnections).where(and(eq(providerConnections.organizationId, actor.organizationId), eq(providerConnections.provider, "openai-codex"))); await db.insert(auditEvents).values({ id: uuidv7(), organizationId: actor.organizationId, actorId: actor.userId, action: "provider.openai.disconnected", targetType: "provider", result: "success", correlationId: request.id }); return { connected: false }; });

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
app.get("/api/v1/projects/:projectId/import", async (request) => { const actor = requireActorPermission(request, "project.read"); const { projectId } = z.object({ projectId: z.string().uuid() }).parse(request.params); const row = (await db.select({ imported: projectImports, revision: projects.revision }).from(projectImports).innerJoin(projects, eq(projects.id, projectImports.projectId)).where(and(eq(projectImports.projectId, projectId), eq(projectImports.organizationId, actor.organizationId))).limit(1))[0]; if (!row) return { imported: false as const }; return { imported: true as const, entryPath: row.imported.entryPath, fileCount: row.imported.fileCount, totalBytes: row.imported.totalBytes, revision: row.revision, files: row.imported.manifest, previewPath: `/api/v1/previews/${projectId}/${previewToken(projectId)}/${row.imported.entryPath}?revision=${row.revision}` }; });
app.get("/api/v1/projects/:projectId/import/file", async (request) => { const actor = requireActorPermission(request, "project.read"); const { projectId } = z.object({ projectId: z.string().uuid() }).parse(request.params); const { path: filePath } = z.object({ path: z.string().min(1).max(512) }).parse(request.query); const row = (await db.select({ imported: projectImports, revision: projects.revision }).from(projectImports).innerJoin(projects, eq(projects.id, projectImports.projectId)).where(and(eq(projectImports.projectId, projectId), eq(projectImports.organizationId, actor.organizationId))).limit(1))[0]; const item = row?.imported.manifest.find((file) => file.path === filePath); if (!row || !item) fail("IMPORT_FILE_NOT_FOUND", 404, "Importdatei nicht gefunden."); if (!editableImportMime(item.mime)) fail("IMPORT_FILE_BINARY", 415, "Diese Binärdatei kann nur in der Vorschau verwendet werden."); const content = (await readObject(`${row.imported.objectPrefix}${item.path}`)).toString("utf8"); return { path: item.path, content, revision: row.revision }; });
app.put("/api/v1/projects/:projectId/import/file", { bodyLimit: 5 * 1024 * 1024 }, async (request) => {
  const actor = requireActorPermission(request, "design.edit");
  const { projectId } = z.object({ projectId: z.string().uuid() }).parse(request.params);
  const body = z.object({ path: z.string().min(1).max(512), content: z.string().max(5 * 1024 * 1024), baseRevision: z.number().int().nonnegative() }).strict().parse(request.body);
  let previous: Buffer | undefined, objectKey: string | undefined, wroteObject = false;
  try {
    return await db.transaction(async (tx) => {
      await tx.execute(sql`select id from projects where id = ${projectId} and organization_id = ${actor.organizationId} for update`);
      const row = (await tx.select({ imported: projectImports, revision: projects.revision }).from(projectImports).innerJoin(projects, eq(projects.id, projectImports.projectId)).where(and(eq(projectImports.projectId, projectId), eq(projectImports.organizationId, actor.organizationId))).limit(1))[0];
      const item = row?.imported.manifest.find((file) => file.path === body.path);
      if (!row || !item) fail("IMPORT_FILE_NOT_FOUND", 404, "Importdatei nicht gefunden.");
      if (!editableImportMime(item.mime)) fail("IMPORT_FILE_BINARY", 415, "Diese Binärdatei kann nicht als Text gespeichert werden.");
      if (row.revision !== body.baseRevision) fail("REVISION_CONFLICT", 409, `Aktuelle Revision ist ${row.revision}.`);
      const data = Buffer.from(body.content, "utf8");
      objectKey = `${row.imported.objectPrefix}${item.path}`;
      previous = await readObject(objectKey);
      await objectStore.putObject(env.S3_BUCKET, objectKey, data, data.byteLength, { "content-type": item.mime });
      wroteObject = true;
      const revision = row.revision + 1;
      const manifest = row.imported.manifest.map((file) => file.path === item.path ? { ...file, size: data.byteLength } : file);
      await tx.update(projectImports).set({ manifest, totalBytes: manifest.reduce((sum, file) => sum + file.size, 0) }).where(eq(projectImports.projectId, projectId));
      await tx.update(projects).set({ revision, updatedAt: new Date() }).where(eq(projects.id, projectId));
      await tx.insert(auditEvents).values({ id: uuidv7(), organizationId: actor.organizationId, actorId: actor.userId, action: "import.file.updated", targetType: "project", targetId: projectId, result: "success", metadata: { path: item.path, revision, bytes: data.byteLength }, correlationId: request.id });
      return { revision };
    });
  } catch (error) {
    if (wroteObject && previous && objectKey) await objectStore.putObject(env.S3_BUCKET, objectKey, previous, previous.byteLength);
    throw error;
  }
});
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
const shutdown = async () => { await app.close(); await client.end(); };
process.on("SIGINT", shutdown); process.on("SIGTERM", shutdown);
await app.listen({ port: env.API_PORT, host: "0.0.0.0" });

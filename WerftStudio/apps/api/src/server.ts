import { createHash, createHmac, timingSafeEqual } from "node:crypto";
import { Transform } from "node:stream";
import archiver from "archiver";
import cors from "@fastify/cors";
import multipart from "@fastify/multipart";
import rateLimit from "@fastify/rate-limit";
import sensible from "@fastify/sensible";
import swagger from "@fastify/swagger";
import swaggerUi from "@fastify/swagger-ui";
import { can, type Role } from "@werft/authz";
import { applyOperationsSchema, createProjectSchema, designDocumentSchema, type DesignDocument } from "@werft/contracts";
import { auditEvents, createDatabase, designOperations, drafts, jobs, memberships, organizations, outboxEvents, projectImports, projects, providerConnections, userPreferences, users, versions } from "@werft/database";
import { applyDesignOperations, validateDesignReferences } from "@werft/design-model";
import { and, desc, eq, isNull, sql } from "drizzle-orm";
import Fastify, { type FastifyRequest } from "fastify";
import { Client as MinioClient } from "minio";
import { v7 as uuidv7 } from "uuid";
import { z } from "zod";
import { codexAuth, codexEfforts, codexModelFields, codexModels, decryptCredentials, encryptCredentials, tokenIdentity } from "./codex-auth.js";
import { codexHttpError, isRetryableCodexError, parseCodexEventStream } from "./codex-stream.js";
import { buildSourceBatches, canRestartReconstructionJob, previewProfileFromHtml, previewProfiles, reconstructionSourceFiles, reconstructionTodos, type ImportManifestFile, type ImportPlatform } from "./import-reconstruction.js";
import { chooseEntryPath, expandZip, importLimits, isFrontendFile, mimeForPath, normalizeImportPath, validateImportFiles } from "./import-project.js";
import { injectPreviewCanvasBridge, rewriteRootRelativeCss, rewriteRootRelativeJavaScript } from "./preview-canvas-bridge.js";

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
// bodyLimit 32 MB: grosse Design-Dokumente und Operations-Batches sprengen sonst das 1-MB-Fastify-Standardlimit.
const app = Fastify({ logger: { redact: ["req.headers.authorization", "req.headers.cookie", "body.password", "body.credential"] }, genReqId: () => uuidv7(), bodyLimit: 32 * 1024 * 1024 });
const s3Endpoint = new URL(env.S3_ENDPOINT);
const objectStore = new MinioClient({ endPoint: s3Endpoint.hostname, port: Number(s3Endpoint.port || (s3Endpoint.protocol === "https:" ? 443 : 80)), useSSL: s3Endpoint.protocol === "https:", accessKey: env.S3_ACCESS_KEY, secretKey: env.S3_SECRET_KEY });
let bucketReady: Promise<void> | undefined;
const pendingCodexAuth = new Map<string, PendingCodexAuth>();

await app.register(cors, { origin: env.WEB_ORIGIN, credentials: true, methods: ["GET", "POST", "PATCH", "PUT", "DELETE"] });
// parts (= Felder + Dateien) explizit setzen: busboy begrenzt sonst still auf 1000 Parts.
// Es duerfen deutlich mehr Dateien ANKOMMEN als uebernommen werden — der Frontend-Filter
// sortiert direkt im Upload-Strom aus, ohne die Grenzen zu belasten.
await app.register(multipart, { preservePath: true, limits: { files: importLimits.maxUploadFiles, fileSize: importLimits.maxFileBytes, fields: importLimits.maxUploadFiles + 10, parts: importLimits.maxUploadFiles * 2 + 20 } });
await app.register(rateLimit, { max: 300, timeWindow: "1 minute" });
await app.register(sensible);
await app.register(swagger, { openapi: { info: { title: "Werft Studio API", version: "1.0.0" }, servers: [{ url: "/api/v1" }] } });
await app.register(swaggerUi, { routePrefix: "/docs" });

const hashJson = (value: unknown) => createHash("sha256").update(JSON.stringify(value)).digest("hex");
const previewToken = (projectId: string) => createHmac("sha256", env.SESSION_SECRET).update(`preview:${projectId}`).digest("base64url");
const validPreviewToken = (projectId: string, token: string) => { const expected = Buffer.from(previewToken(projectId)); const actual = Buffer.from(token); return expected.length === actual.length && timingSafeEqual(expected, actual); };
const ensureBucket = () => bucketReady ??= (async () => { if (!(await objectStore.bucketExists(env.S3_BUCKET))) await objectStore.makeBucket(env.S3_BUCKET); })();
async function cleanupOrphanImportObjects() {
  await ensureBucket();
  const imports = await db.select({ objectPrefix: projectImports.objectPrefix, manifest: projectImports.manifest }).from(projectImports);
  const knownObjects = new Set(imports.flatMap((row) => row.manifest.map((file) => `${row.objectPrefix}${file.path}`)));
  for await (const upload of objectStore.listIncompleteUploads(env.S3_BUCKET, "", true)) await objectStore.removeIncompleteUpload(env.S3_BUCKET, upload.key);
  for await (const item of objectStore.listObjectsV2(env.S3_BUCKET, "", true)) {
    if (!item.name) continue;
    if (!knownObjects.has(item.name)) await objectStore.removeObject(env.S3_BUCKET, item.name);
  }
}
const editableImportMime = (mime: string) => mime.startsWith("text/") || ["application/json", "image/svg+xml"].some((type) => mime.startsWith(type));
const readObject = async (key: string) => { const chunks: Buffer[] = []; for await (const chunk of await objectStore.getObject(env.S3_BUCKET, key)) chunks.push(Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk)); return Buffer.concat(chunks); };
const codexPost = async (url: string, body: unknown, form = false) => {
  try {
    return await fetch(url, { method: "POST", signal: AbortSignal.timeout(15_000), headers: { "content-type": form ? "application/x-www-form-urlencoded" : "application/json", accept: "application/json" }, body: form ? new URLSearchParams(body as Record<string, string>) : JSON.stringify(body) });
  } catch (error) {
    app.log.warn({ err: error, url }, "OpenAI nicht erreichbar");
    fail("OPENAI_UNREACHABLE", 502, "OpenAI ist vom Server aus nicht erreichbar (Netzwerk/DNS). Bitte Egress-Netz des API-Containers prüfen.", true);
  }
};
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
type ImportType = "prototype" | "presentation" | "document" | "template" | "canvas";
const nativeUiSourcePattern = /\.(?:xaml|axaml|kt|kts|java|swift|storyboard|dart|cs|razor|qml|ui|uxml|py|rs)$/i;
function importedDesignDocument(projectId: string, name: string, platform: ImportPlatform, projectType: ImportType): DesignDocument {
  const pageId = uuidv7(), frameId = uuidv7(), nodeId = uuidv7();
  const pageType = projectType === "presentation" ? "slide" : projectType === "document" ? "document-page" : projectType === "canvas" ? "canvas" : "screen";
  const profile = previewProfiles[platform];
  return {
    schemaVersion: 1, projectId, projectType, fidelity: "high_fidelity", platforms: [platform], designSystemVersionId: null,
    themes: [{ id: "light", name: "Original", tokens: { "color.bg": "#F5F7FA", "color.surface": "#FFFFFF", "color.accent": "#3157D5" } }],
    pages: [{ id: pageId, name, type: pageType, frameIds: [frameId] }],
    frames: [{ id: frameId, pageId, name, platform, device: profile.device, width: profile.width, height: profile.height, theme: "light", locale: "de-DE", rootNodeId: nodeId, canvasX: 0, canvasY: 0 }],
    nodes: [{ id: nodeId, name: "Interaktive HTML-Vorschau", parentId: null, childIds: [], bounds: { x: 0, y: 0, width: profile.width, height: profile.height }, visible: true, locked: false, tokenBindings: {}, semantics: { role: "main", label: name }, type: "container", layout: "absolute", gap: 0, padding: [0, 0, 0, 0], fill: "color.surface" }],
    assets: [], interactions: [], metadata: { createdAt: new Date().toISOString(), compilerVersion: "0.1.0" }
  };
}

// Frontend-Extraktion beim Import: aus App-Quellcode nur die designrelevanten Dateien uebernehmen
// (Markup, Styles, Themes, UI-Code, Bilder/Fonts/Medien) — Backend, Build-Artefakte und Werkzeug-
// Ordner bleiben draussen. So bleiben auch grosse Programme schlank importierbar.
async function readImportParts(request: FastifyRequest, objectPrefix: string, storedKeys: string[]): Promise<{ name: string; platform: ImportPlatform; projectType: ImportType; frontendOnly: boolean; files: ImportManifestFile[] }> {
  let name = "Importiertes Design";
  let platform: ImportPlatform = "web", projectType: ImportType = "prototype", frontendOnly = false;
  const platformValues = ["web", "android", "ios", "ipados", "macos", "windows"] as const;
  const typeValues = ["prototype", "presentation", "document", "template", "canvas"] as const;
  let files: ImportManifestFile[] = [];
  let pendingFileSize: number | undefined;
  const seenPaths = new Set<string>();
  await ensureBucket();
  try {
    for await (const part of request.parts()) {
      if (part.type === "field") {
        if (part.fieldname === "name" && typeof part.value === "string") name = part.value.trim().slice(0, 120) || name;
        if (part.fieldname === "platform" && typeof part.value === "string" && (platformValues as readonly string[]).includes(part.value)) platform = part.value as ImportPlatform;
        if (part.fieldname === "type" && typeof part.value === "string" && (typeValues as readonly string[]).includes(part.value)) projectType = part.value as ImportType;
        if (part.fieldname === "frontendOnly" && part.value === "true") frontendOnly = true;
        if (part.fieldname === "fileSize" && typeof part.value === "string") {
          const value = Number(part.value);
          pendingFileSize = Number.isSafeInteger(value) && value >= 0 ? value : undefined;
        }
        continue;
      }
      const candidatePath = (part.filename ?? "").replaceAll("\\", "/");
      const expectedSize = pendingFileSize;
      pendingFileSize = undefined;
      if (frontendOnly && candidatePath && !candidatePath.toLowerCase().endsWith(".zip") && !isFrontendFile(candidatePath, platform)) {
        for await (const chunk of part.file) void chunk;
        continue;
      }
      const filePath = normalizeImportPath(part.filename);
      const pathKey = filePath.toLocaleLowerCase("en-US");
      if (seenPaths.has(pathKey)) fail("IMPORT_PATH_DUPLICATE", 400, `Der Dateipfad „${filePath}“ kommt mehrfach vor.`);
      seenPaths.add(pathKey);
      const mime = !part.mimetype || part.mimetype === "application/octet-stream" ? mimeForPath(filePath) : part.mimetype;
      const objectKey = `${objectPrefix}${filePath}`;
      if (expectedSize === undefined) {
        for await (const chunk of part.file) void chunk;
        fail("IMPORT_SIZE_REQUIRED", 400, `Für die Datei „${filePath}“ fehlt die Größenangabe.`);
      }
      let size = 0;
      if (expectedSize === 0) {
        for await (const chunk of part.file) size += Buffer.byteLength(chunk);
        if (size === 0) await objectStore.putObject(env.S3_BUCKET, objectKey, Buffer.alloc(0), 0, { "content-type": mime });
      } else {
        const meter = new Transform({ transform(chunk, _encoding, callback) { size += Buffer.byteLength(chunk); callback(null, chunk); } });
        await objectStore.putObject(env.S3_BUCKET, objectKey, part.file.pipe(meter), expectedSize, { "content-type": mime });
      }
      if (part.file.truncated || (expectedSize !== undefined && expectedSize !== size)) {
        await objectStore.removeObject(env.S3_BUCKET, objectKey);
        fail("IMPORT_FILE_INCOMPLETE", 413, `Die Datei „${filePath}“ wurde nicht vollständig übertragen.`);
      }
      storedKeys.push(objectKey);
      files.push({ path: filePath, size, mime });
    }
    if (!files.length) fail("IMPORT_EMPTY", 400, frontendOnly ? "Im gewählten Ordner wurden keine Frontend-/UI-Dateien erkannt." : "Der Import enthält keine Dateien.");
    if (files.length === 1 && files[0]!.path.toLowerCase().endsWith(".zip")) {
      const zip = files[0]!;
      if (zip.size > importLimits.maxArchiveFileBytes) fail("IMPORT_ARCHIVE_TOO_LARGE", 413, "Das ZIP ist zu groß für sichere In-Memory-Entpackung. Bitte den Projektordner direkt streamen.");
      const unpacked = await expandZip(await readObject(`${objectPrefix}${zip.path}`));
      const selected = frontendOnly ? unpacked.filter((file) => isFrontendFile(file.path, platform)) : unpacked;
      if (!selected.length) fail("IMPORT_EMPTY", 400, "Das ZIP enthält keine Frontend-/UI-Dateien.");
      const expanded = validateImportFiles(selected);
      await objectStore.removeObject(env.S3_BUCKET, `${objectPrefix}${zip.path}`);
      const zipKeyIndex = storedKeys.indexOf(`${objectPrefix}${zip.path}`);
      if (zipKeyIndex >= 0) storedKeys.splice(zipKeyIndex, 1);
      files = [];
      seenPaths.clear();
      for (const file of expanded) {
        const key = file.path.toLocaleLowerCase("en-US");
        if (seenPaths.has(key)) fail("IMPORT_PATH_DUPLICATE", 400, `Der Dateipfad „${file.path}“ kommt mehrfach vor.`);
        seenPaths.add(key);
        const objectKey = `${objectPrefix}${file.path}`;
        await objectStore.putObject(env.S3_BUCKET, objectKey, file.data, file.data.byteLength, { "content-type": file.mime });
        storedKeys.push(objectKey);
        files.push({ path: file.path, size: file.data.byteLength, mime: file.mime });
      }
    }
    return { name, platform, projectType, frontendOnly, files };
  } catch (error) {
    await Promise.allSettled(storedKeys.map((key) => objectStore.removeObject(env.S3_BUCKET, key)));
    throw error;
  }
}

app.get("/api/v1/health/live", async () => ({ status: "ok", version: "0.4.2-20260725.1543" }));
app.get("/api/v1/health/ready", async () => { await client`select 1`; return { status: "ready", database: "ok" }; });
app.get("/api/v1/previews/:projectId/:token/*", { config: { rateLimit: false } }, async (request, reply) => {
  const params = z.object({ projectId: z.string().uuid(), token: z.string().min(40), "*": z.string() }).parse(request.params);
  if (!validPreviewToken(params.projectId, params.token)) fail("PREVIEW_NOT_FOUND", 404, "Vorschau nicht gefunden.");
  const row = (await db.select({ imported: projectImports, revision: projects.revision }).from(projectImports).innerJoin(projects, eq(projects.id, projectImports.projectId)).where(eq(projectImports.projectId, params.projectId)).limit(1))[0];
  if (!row) fail("PREVIEW_NOT_FOUND", 404, "Vorschau nicht gefunden.");
  const imported = row.imported;
  const generatedEntry = imported.entryPath.startsWith(".werft-generated/");
  const previewRootDirectory = !generatedEntry && imported.entryPath.includes("/") ? imported.entryPath.slice(0, imported.entryPath.lastIndexOf("/") + 1) : "";
  const rawRequestedPath = params["*"] || imported.entryPath;
  if (!rawRequestedPath) fail("PREVIEW_FILE_NOT_FOUND", 404, "Für dieses Projekt ist keine Startseite festgelegt.");
  const requestedPath = normalizeImportPath(rawRequestedPath);
  let item = imported.manifest.find((file) => file.path === requestedPath);
  if (!item && imported.entryPath && !requestedPath.split("/").at(-1)?.includes(".")) item = imported.manifest.find((file) => file.path === imported.entryPath);
  if (!item) fail("PREVIEW_FILE_NOT_FOUND", 404, "Vorschaudatei nicht gefunden.");
  // ETag pro Projektrevision: Browser darf cachen und bekommt 304 statt kompletter Neu-Downloads;
  // nach jeder KI-/Editor-Aenderung steigt die Revision und die Vorschau wird frisch geladen.
  const etag = `W/"${params.projectId}:${row.revision}:${item.path}:canvas-bridge-v3"`;
  if (request.headers["if-none-match"] === etag) return reply.code(304).header("etag", etag).header("cache-control", "private, no-cache").send();
  reply.headers({ "cache-control": "private, no-cache", etag, "x-content-type-options": "nosniff", "referrer-policy": "no-referrer" }).type(item.mime);
  if (item.mime.startsWith("text/html")) {
    reply.header("content-security-policy", "default-src 'self' data: blob: https: http:; script-src 'self' 'unsafe-inline' 'unsafe-eval' blob: https: http:; style-src 'self' 'unsafe-inline' blob: https: http:; img-src 'self' data: blob: https: http:; font-src 'self' data: blob: https: http:; media-src 'self' data: blob: https: http:; connect-src 'self' https: http: wss: ws:; frame-src 'self' blob: https: http:; frame-ancestors https://10.8.0.1:8443 http://localhost:5173");
    const html = (await readObject(`${imported.objectPrefix}${item.path}`)).toString("utf8");
    return reply.send(injectPreviewCanvasBridge(html, `/api/v1/previews/${params.projectId}/${params.token}/${previewRootDirectory}`));
  }
  if (item.mime.startsWith("text/css") || item.mime.startsWith("text/javascript")) {
    const source = (await readObject(`${imported.objectPrefix}${item.path}`)).toString("utf8");
    const base = `/api/v1/previews/${params.projectId}/${params.token}/${previewRootDirectory}`;
    return reply.send(item.mime.startsWith("text/css") ? rewriteRootRelativeCss(source, base) : rewriteRootRelativeJavaScript(source, base));
  }
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

app.get("/api/v1/projects", async (request) => {
  const actor = requireActorPermission(request, "project.read");
  const rows = await db.select({ project: projects, entryPath: projectImports.entryPath }).from(projects).leftJoin(projectImports, eq(projectImports.projectId, projects.id)).where(and(eq(projects.organizationId, actor.organizationId), isNull(projects.deletedAt))).orderBy(desc(projects.updatedAt)).limit(100);
  return rows.map(({ project, entryPath }) => ({ ...project, ...(entryPath ? { previewPath: `/api/v1/previews/${project.id}/${previewToken(project.id)}/${entryPath}?revision=${project.revision}` } : {}) }));
});
app.post("/api/v1/projects", async (request, reply) => {
  const actor = requireActorPermission(request, "project.update"); const input = createProjectSchema.parse(request.body); const projectId = uuidv7(), pageId = uuidv7(), frameId = uuidv7(), nodeId = uuidv7(), draftId = uuidv7(), versionId = uuidv7();
  const document: DesignDocument = { schemaVersion: 1, projectId, projectType: input.type, fidelity: input.fidelity, platforms: input.platforms, designSystemVersionId: input.designSystemVersionId, themes: [{ id: "light", name: "Hell", tokens: { "color.bg": "#F5F7FA", "color.surface": "#FFFFFF", "color.accent": "#3157D5" } }], pages: [{ id: pageId, name: "Start", type: input.type === "presentation" ? "slide" : input.type === "document" ? "document-page" : input.type === "canvas" ? "canvas" : "screen", frameIds: [frameId] }], frames: [{ id: frameId, pageId, name: "Start", platform: input.platforms[0]!, device: "Standard", width: 390, height: 760, theme: "light", locale: "de-DE", rootNodeId: nodeId, canvasX: 0, canvasY: 0 }], nodes: [{ id: nodeId, name: "Start", parentId: null, childIds: [], bounds: { x: 0, y: 0, width: 390, height: 760 }, visible: true, locked: false, tokenBindings: {}, semantics: { role: "main", label: "Start" }, type: "container", layout: "column", gap: 16, padding: [24, 24, 24, 24], fill: "color.surface" }], assets: [], interactions: [], metadata: { createdAt: new Date().toISOString(), compilerVersion: "0.1.0" } };
  await db.transaction(async (tx) => { await tx.insert(projects).values({ id: projectId, organizationId: actor.organizationId, name: input.name, type: input.type, fidelity: input.fidelity, platforms: input.platforms, ownerId: actor.userId }); await tx.insert(drafts).values({ id: draftId, projectId, organizationId: actor.organizationId, document, updatedBy: actor.userId }); await tx.insert(versions).values({ id: versionId, organizationId: actor.organizationId, projectId, number: 1, reason: "Projekt angelegt", authorId: actor.userId, document, snapshotHash: hashJson(document) }); await tx.insert(auditEvents).values({ id: uuidv7(), organizationId: actor.organizationId, actorId: actor.userId, action: "project.created", targetType: "project", targetId: projectId, result: "success", correlationId: request.id }); });
  return reply.status(201).send({ projectId, version: 1, revision: 0 });
});

// Ordnerimporte werden Datei fuer Datei direkt in den Objektspeicher gestreamt. Weder das gesamte
// Projekt noch einzelne grosse Quellen muessen dadurch im API-Speicher liegen.
app.post("/api/v1/imports", { bodyLimit: Number.MAX_SAFE_INTEGER }, async (request, reply) => {
  const actor = requireActorPermission(request, "project.update");
  if (!request.isMultipart()) fail("IMPORT_MULTIPART_REQUIRED", 415, "Bitte Dateien oder ein ZIP-Paket auswählen.");
  const projectId = uuidv7(), draftId = uuidv7(), versionId = uuidv7();
  const storedKeys: string[] = [];
  const objectPrefix = `${actor.organizationId}/${projectId}/`;
  try {
    const { name, platform, projectType, files } = await readImportParts(request, objectPrefix, storedKeys);
    const explicitNativeFile = files.find((file) => /(^|\/)(design-document\.json|[^/]+\.werft)$/i.test(file.path));
    const nativeFile = explicitNativeFile ?? (files.length === 1 && files[0]!.path.toLowerCase().endsWith(".json") ? files[0] : undefined);
    let candidate: unknown;
    if (nativeFile) {
      try { candidate = JSON.parse((await readObject(`${objectPrefix}${nativeFile.path}`)).toString("utf8")); }
      catch { if (explicitNativeFile) fail("IMPORT_JSON_INVALID", 400, "Die ausgewählte Werft-/JSON-Datei enthält kein gültiges JSON."); }
    }
    const parsedNative = candidate === undefined ? undefined : designDocumentSchema.safeParse(typeof candidate === "object" && candidate !== null && "document" in candidate ? (candidate as { document: unknown }).document : candidate);
    if (explicitNativeFile && parsedNative && !parsedNative.success) fail("IMPORT_DOCUMENT_INVALID", 400, "Die Werft-/JSON-Datei ist kein gültiges DesignDocument.");
    const nativeDocument = parsedNative?.success ? designDocumentSchema.parse({ ...parsedNative.data, projectId }) : undefined;
    const storeAsFiles = !nativeDocument;
    const entryPath = storeAsFiles ? chooseEntryPath(files) ?? "" : undefined;
    const document = nativeDocument ?? importedDesignDocument(projectId, name, platform, projectType);
    if (!storeAsFiles) {
      await Promise.all(storedKeys.map((key) => objectStore.removeObject(env.S3_BUCKET, key)));
      storedKeys.length = 0;
    }
    await db.transaction(async (tx) => {
      await tx.insert(projects).values({ id: projectId, organizationId: actor.organizationId, name, type: document.projectType, fidelity: document.fidelity, platforms: document.platforms, ownerId: actor.userId });
      await tx.insert(drafts).values({ id: draftId, projectId, organizationId: actor.organizationId, document, updatedBy: actor.userId });
      await tx.insert(versions).values({ id: versionId, organizationId: actor.organizationId, projectId, number: 1, reason: "Projekt importiert", authorId: actor.userId, document, snapshotHash: hashJson(document) });
      if (storeAsFiles) await tx.insert(projectImports).values({ projectId, organizationId: actor.organizationId, format: "ui-project", entryPath: entryPath ?? "", objectPrefix, manifest: files, fileCount: files.length, totalBytes: files.reduce((sum, file) => sum + file.size, 0) });
      await tx.insert(auditEvents).values({ id: uuidv7(), organizationId: actor.organizationId, actorId: actor.userId, action: "project.imported", targetType: "project", targetId: projectId, result: "success", metadata: { format: storeAsFiles ? "ui-project" : "design-document", fileCount: files.length, entryDetected: Boolean(entryPath), streamed: true }, correlationId: request.id });
    });
    const hasNativeUiSources = files.some((file) => nativeUiSourcePattern.test(file.path));
    return reply.status(201).send({ projectId, kind: storeAsFiles ? (entryPath ? "html" : "files") : "native", requiresReconstruction: storeAsFiles && (!entryPath || (platform !== "web" && hasNativeUiSources)) });
  } catch (error) {
    await Promise.allSettled(storedKeys.map((key) => objectStore.removeObject(env.S3_BUCKET, key)));
    throw error;
  }
});

app.delete("/api/v1/projects/:projectId", async (request) => {
  const actor = requireActorPermission(request, "project.delete");
  const { projectId } = z.object({ projectId: z.string().uuid() }).parse(request.params);
  const project = (await db.select({ id: projects.id, name: projects.name }).from(projects).where(and(eq(projects.id, projectId), eq(projects.organizationId, actor.organizationId), isNull(projects.deletedAt))).limit(1))[0];
  if (!project) fail("NOT_FOUND", 404, "Projekt nicht gefunden.");
  const imported = (await db.select().from(projectImports).where(and(eq(projectImports.projectId, projectId), eq(projectImports.organizationId, actor.organizationId))).limit(1))[0];
  if (imported) {
    await Promise.allSettled(imported.manifest.map((file) => objectStore.removeObject(env.S3_BUCKET, `${imported.objectPrefix}${file.path}`)));
    await db.delete(projectImports).where(eq(projectImports.projectId, projectId));
  }
  await db.update(projects).set({ deletedAt: new Date(), updatedAt: new Date() }).where(eq(projects.id, projectId));
  await db.insert(auditEvents).values({ id: uuidv7(), organizationId: actor.organizationId, actorId: actor.userId, action: "project.deleted", targetType: "project", targetId: projectId, result: "success", metadata: { name: project.name, importedFiles: imported?.fileCount ?? 0 }, correlationId: request.id });
  return { deleted: true };
});
app.get("/api/v1/projects/:projectId", async (request) => { const actor = requireActorPermission(request, "project.read"); const { projectId } = z.object({ projectId: z.string().uuid() }).parse(request.params); const rows = await db.select().from(projects).where(and(eq(projects.id, projectId), eq(projects.organizationId, actor.organizationId), isNull(projects.deletedAt))).limit(1); if (!rows[0]) fail("NOT_FOUND", 404, "Projekt nicht gefunden."); return rows[0]; });
app.get("/api/v1/projects/:projectId/import", async (request) => {
  const actor = requireActorPermission(request, "project.read");
  const { projectId } = z.object({ projectId: z.string().uuid() }).parse(request.params);
  const row = (await db.select({ imported: projectImports, revision: projects.revision, platforms: projects.platforms }).from(projectImports).innerJoin(projects, eq(projects.id, projectImports.projectId)).where(and(eq(projectImports.projectId, projectId), eq(projectImports.organizationId, actor.organizationId))).limit(1))[0];
  if (!row) return { imported: false as const };
  const platform = (Array.isArray(row.platforms) ? row.platforms[0] : "web") as ImportPlatform;
  const fallbackProfile = previewProfiles[platform] ?? previewProfiles.web;
  let profile = fallbackProfile;
  if (row.imported.entryPath) {
    try { profile = previewProfileFromHtml((await readObject(`${row.imported.objectPrefix}${row.imported.entryPath}`)).toString("utf8"), fallbackProfile); }
    catch (error) { request.log.warn({ err: error, projectId }, "Vorschau-Metadaten nicht lesbar; Plattformprofil wird verwendet"); }
  }
  return { imported: true as const, entryPath: row.imported.entryPath, fileCount: row.imported.fileCount, totalBytes: row.imported.totalBytes, revision: row.revision, files: row.imported.manifest, previewWidth: profile.width, previewHeight: profile.height, previewDevice: profile.device, ...(row.imported.entryPath ? { previewPath: `/api/v1/previews/${projectId}/${previewToken(projectId)}/${row.imported.entryPath}?revision=${row.revision}` } : {}) };
});
app.get("/api/v1/projects/:projectId/import/file", async (request) => { const actor = requireActorPermission(request, "project.read"); const { projectId } = z.object({ projectId: z.string().uuid() }).parse(request.params); const { path: filePath } = z.object({ path: z.string().min(1).max(512) }).parse(request.query); const row = (await db.select({ imported: projectImports, revision: projects.revision }).from(projectImports).innerJoin(projects, eq(projects.id, projectImports.projectId)).where(and(eq(projectImports.projectId, projectId), eq(projectImports.organizationId, actor.organizationId))).limit(1))[0]; const item = row?.imported.manifest.find((file) => file.path === filePath); if (!row || !item) fail("IMPORT_FILE_NOT_FOUND", 404, "Importdatei nicht gefunden."); if (!editableImportMime(item.mime)) fail("IMPORT_FILE_BINARY", 415, "Diese Binärdatei kann nur in der Vorschau verwendet werden."); const content = (await readObject(`${row.imported.objectPrefix}${item.path}`)).toString("utf8"); return { path: item.path, content, revision: row.revision }; });
app.put("/api/v1/projects/:projectId/import/file", { bodyLimit: 20 * 1024 * 1024 }, async (request) => {
  const actor = requireActorPermission(request, "design.edit");
  const { projectId } = z.object({ projectId: z.string().uuid() }).parse(request.params);
  const body = z.object({ path: z.string().min(1).max(512), content: z.string().max(20 * 1024 * 1024), baseRevision: z.number().int().nonnegative() }).strict().parse(request.body);
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
app.get("/api/v1/projects/:projectId/export.zip", async (request, reply) => {
  const actor = requireActorPermission(request, "project.export");
  const { projectId } = z.object({ projectId: z.string().uuid() }).parse(request.params);
  const row = (await db.select({ imported: projectImports, name: projects.name }).from(projectImports).innerJoin(projects, eq(projects.id, projectImports.projectId)).where(and(eq(projectImports.projectId, projectId), eq(projectImports.organizationId, actor.organizationId))).limit(1))[0];
  if (!row) fail("EXPORT_NOT_AVAILABLE", 404, "Für dieses Projekt liegt kein importiertes Dateipaket vor.");
  const fileName = `${row.name.replaceAll(/[^\p{L}\p{N} _.-]/gu, "").trim().slice(0, 80) || "werft-projekt"}.zip`;
  const archive = archiver("zip", { zlib: { level: 6 } });
  for (const file of row.imported.manifest) archive.append(await readObject(`${row.imported.objectPrefix}${file.path}`), { name: file.path });
  void archive.finalize();
  return reply.header("content-type", "application/zip").header("content-disposition", `attachment; filename="${encodeURIComponent(fileName)}"`).send(archive);
});
app.patch("/api/v1/projects/:projectId/import/entry", async (request) => {
  const actor = requireActorPermission(request, "design.edit");
  const { projectId } = z.object({ projectId: z.string().uuid() }).parse(request.params);
  const body = z.object({ path: z.string().min(1).max(512) }).strict().parse(request.body);
  const row = (await db.select().from(projectImports).where(and(eq(projectImports.projectId, projectId), eq(projectImports.organizationId, actor.organizationId))).limit(1))[0];
  if (!row) fail("IMPORT_NOT_FOUND", 404, "Für dieses Projekt liegt kein Import vor.");
  const item = row.manifest.find((file) => file.path === body.path);
  if (!item || !/\.html?$/i.test(item.path)) fail("IMPORT_ENTRY_INVALID", 400, "Als Startseite eignet sich nur eine HTML-Datei aus dem Import.");
  await db.update(projectImports).set({ entryPath: item.path }).where(eq(projectImports.projectId, projectId));
  await db.insert(auditEvents).values({ id: uuidv7(), organizationId: actor.organizationId, actorId: actor.userId, action: "import.entry.updated", targetType: "project", targetId: projectId, result: "success", metadata: { entryPath: item.path }, correlationId: request.id });
  return { entryPath: item.path };
});
const chatEditableMime = (mime: string) => mime.startsWith("text/") || mime.startsWith("application/json") || mime.startsWith("image/svg+xml");
const chatInstructions = [
  "Du bist der Design-Editor von Werft Studio, einem selbst gehosteten Designwerkzeug.",
  "Du erhältst die Textdateien eines importierten HTML-Design-Projekts und einen Änderungswunsch des Benutzers.",
  "Setze den Wunsch als praezise Suchen/Ersetzen-Edits um. Antworte AUSSCHLIESSLICH mit einem einzigen JSON-Objekt ohne Markdown und ohne Codefences:",
  '{"reply": "kurze deutsche Zusammenfassung", "changes": [{"path": "pfad/wie/geliefert", "edits": [{"find": "exakter vorhandener Ausschnitt", "replace": "neuer Ausschnitt"}]}]}',
  "Regeln: find muss WOERTLICH so im aktuellen Dateiinhalt vorkommen und durch genug Kontext EINDEUTIG sein.",
  "Nutze viele kleine Edits statt grosser Bloecke. Gib NIEMALS komplette Dateien zurueck.",
  "Erhalte Struktur, Funktionen und alle nicht betroffenen Inhalte vollstaendig. Pfade exakt wie geliefert."
].join(" ");
function extractJsonObject(text: string): string {
  const start = text.indexOf("{"), end = text.lastIndexOf("}");
  return start >= 0 && end > start ? text.slice(start, end + 1) : text;
}
async function codexRun(organizationId: string, instructions: string, input: string, log: FastifyRequest["log"]): Promise<{ text: string; model: string }> {
  const connection = await validCodexConnection(organizationId);
  const settings = z.object({ model: z.enum(codexModels).default("gpt-5.6-sol"), effort: z.enum(codexEfforts).default("high"), fast: z.boolean().default(false) }).parse(connection.settings);
  const accountId = connection.accountId || tokenIdentity(connection.credentials.accessToken, connection.credentials.idToken).accountId;
  if (!accountId) fail("OPENAI_ACCOUNT_MISSING", 401, "Im OpenAI-Token fehlt die ChatGPT-Account-ID. Bitte erneut verbinden.");
  const body: Record<string, unknown> = { ...codexModelFields(settings.model, settings.fast), instructions, input: [{ role: "user", content: input }], store: false, stream: true };
  if (settings.effort !== "none") { body.reasoning = { effort: settings.effort, summary: "auto" }; body.include = ["reasoning.encrypted_content"]; }
  for (let attempt = 1; attempt <= 3; attempt += 1) {
    try {
      const response = await fetch(codexAuth.responsesUrl, { method: "POST", signal: AbortSignal.timeout(540_000), headers: { authorization: `Bearer ${connection.credentials.accessToken}`, "chatgpt-account-id": accountId, originator: "codex_cli_rs", "user-agent": "codex_cli_rs/0.0.0 (Werft Studio)", accept: "text/event-stream", "content-type": "application/json" }, body: JSON.stringify(body) });
      const raw = await response.text();
      if (!response.ok) throw codexHttpError(response.status);
      const outputText = parseCodexEventStream(raw);
      if (!outputText.trim()) fail("CHAT_EMPTY", 502, "OpenAI hat keine verwertbare Antwort geliefert. Bitte erneut versuchen.", true);
      return { text: outputText, model: settings.model };
    } catch (error) {
      if (!isRetryableCodexError(error) || attempt === 3) {
        const details = error && typeof error === "object" ? error as Record<string, unknown> : {};
        log.warn({ err: error, attempt }, "KI-Lauf dauerhaft fehlgeschlagen");
        if (typeof details.code === "string") throw error;
        fail("CHAT_UPSTREAM", 502, "OpenAI hat die Verbindung während des KI-Laufs beendet oder nicht rechtzeitig geantwortet. Bitte erneut versuchen.", true);
      }
      const delayMs = attempt * 2_000;
      log.warn({ err: error, attempt, nextAttempt: attempt + 1, delayMs }, "KI-Lauf transient unterbrochen; Wiederholung wird gestartet");
      await new Promise((resolve) => setTimeout(resolve, delayMs));
    }
  }
  fail("CHAT_UPSTREAM", 502, "OpenAI hat den KI-Lauf nicht abgeschlossen.", true);
}
app.post("/api/v1/projects/:projectId/chat", { config: { rateLimit: { max: 20, timeWindow: "5 minutes" } } }, async (request) => {
  const actor = requireActorPermission(request, "design.edit");
  const { projectId } = z.object({ projectId: z.string().uuid() }).parse(request.params);
  const { message } = z.object({ message: z.string().min(1).max(8000) }).strict().parse(request.body);
  const row = (await db.select({ imported: projectImports, revision: projects.revision }).from(projectImports).innerJoin(projects, eq(projects.id, projectImports.projectId)).where(and(eq(projectImports.projectId, projectId), eq(projectImports.organizationId, actor.organizationId))).limit(1))[0];
  if (!row) fail("CHAT_NOT_SUPPORTED", 400, "Die KI-Bearbeitung ist aktuell für importierte HTML-Projekte verfügbar.");
  const texts: Array<{ path: string; content: string }> = [];
  let budget = 700_000;
  const candidates = [...row.imported.manifest].filter((file) => chatEditableMime(file.mime)).sort((a, b) => (a.path === row.imported.entryPath ? -1 : b.path === row.imported.entryPath ? 1 : a.size - b.size));
  for (const file of candidates) {
    if (file.size > 300_000 || file.size > budget) continue;
    texts.push({ path: file.path, content: (await readObject(`${row.imported.objectPrefix}${file.path}`)).toString("utf8") });
    budget -= file.size;
  }
  if (!texts.length) fail("CHAT_NO_TEXT_FILES", 400, "Dieses Projekt enthält keine bearbeitbaren Textdateien.");
  const inputText = `Änderungswunsch:\n${message}\n\n=== PROJEKTDATEIEN ===\n${texts.map((text) => `--- ${text.path} ---\n${text.content}`).join("\n\n")}`;
  const { text: outputText, model } = await codexRun(actor.organizationId, chatInstructions, inputText, request.log);
  const settings = { model };
  const cleaned = extractJsonObject(outputText.trim().replace(/^```(?:json)?\s*/i, "").replace(/\s*```$/, ""));
  let parsed: { reply?: string; changes?: Array<{ path?: string; edits?: Array<{ find?: string; replace?: string }> }>; files?: Array<{ path?: string; content?: string }> };
  try { parsed = JSON.parse(cleaned) as typeof parsed; }
  catch { return { reply: outputText.trim().slice(0, 4000), changedFiles: [], skipped: ["Antwort der KI war kein gültiges JSON — es wurde nichts geändert."], revision: row.revision }; }
  const editChanges = (parsed.changes ?? []).filter((change): change is { path: string; edits: Array<{ find: string; replace: string }> } => typeof change?.path === "string" && Array.isArray(change?.edits) && change.edits.every((edit) => typeof edit?.find === "string" && edit.find.length > 0 && typeof edit?.replace === "string"));
  const fullChanges = (parsed.files ?? []).filter((file): file is { path: string; content: string } => typeof file?.path === "string" && typeof file?.content === "string" && file.content.length <= 20 * 1024 * 1024);
  if (!editChanges.length && !fullChanges.length) return { reply: parsed.reply?.trim() || "Es war keine Dateiänderung nötig.", changedFiles: [], skipped: [], revision: row.revision };
  const applied: string[] = [];
  const skipped: string[] = [];
  const previous: Array<{ key: string; mime: string; data: Buffer }> = [];
  try {
    const result = await db.transaction(async (tx) => {
      await tx.execute(sql`select id from projects where id = ${projectId} and organization_id = ${actor.organizationId} for update`);
      const current = (await tx.select({ imported: projectImports, revision: projects.revision }).from(projectImports).innerJoin(projects, eq(projects.id, projectImports.projectId)).where(and(eq(projectImports.projectId, projectId), eq(projectImports.organizationId, actor.organizationId))).limit(1))[0];
      if (!current) fail("IMPORT_NOT_FOUND", 404, "Für dieses Projekt liegt kein Import vor.");
      let manifest = current.imported.manifest;
      const writeFile = async (path: string, mime: string, data: Buffer, previousData: Buffer) => {
        const key = `${current.imported.objectPrefix}${path}`;
        previous.push({ key, mime, data: previousData });
        await objectStore.putObject(env.S3_BUCKET, key, data, data.byteLength, { "content-type": mime });
        manifest = manifest.map((file) => (file.path === path ? { ...file, size: data.byteLength } : file));
        applied.push(path);
      };
      for (const change of editChanges) {
        const item = manifest.find((file) => file.path === change.path);
        if (!item || !chatEditableMime(item.mime)) { skipped.push(`${change.path}: Datei nicht bearbeitbar oder unbekannt.`); continue; }
        const before = await readObject(`${current.imported.objectPrefix}${item.path}`);
        let content = before.toString("utf8");
        let editsApplied = 0;
        for (const edit of change.edits) {
          if (content.includes(edit.find)) { content = content.replace(edit.find, edit.replace); editsApplied += 1; }
          else skipped.push(`${change.path}: Ein Edit fand seine Textstelle nicht („${edit.find.slice(0, 60)}…“).`);
        }
        if (editsApplied > 0) await writeFile(item.path, item.mime, Buffer.from(content, "utf8"), before);
      }
      for (const change of fullChanges) {
        const item = manifest.find((file) => file.path === change.path);
        if (!item || !chatEditableMime(item.mime)) { skipped.push(`${change.path}: Datei nicht bearbeitbar oder unbekannt.`); continue; }
        const before = await readObject(`${current.imported.objectPrefix}${item.path}`);
        await writeFile(item.path, item.mime, Buffer.from(change.content, "utf8"), before);
      }
      if (!applied.length) return { revision: current.revision };
      const revision = current.revision + 1;
      await tx.update(projectImports).set({ manifest, totalBytes: manifest.reduce((sum, file) => sum + file.size, 0) }).where(eq(projectImports.projectId, projectId));
      await tx.update(projects).set({ revision, updatedAt: new Date() }).where(eq(projects.id, projectId));
      await tx.insert(auditEvents).values({ id: uuidv7(), organizationId: actor.organizationId, actorId: actor.userId, action: "design.ai.applied", targetType: "project", targetId: projectId, result: "success", metadata: { files: applied, skipped: skipped.length, revision, model: settings.model }, correlationId: request.id });
      return { revision };
    });
    return { reply: parsed.reply?.trim() || `Änderungen umgesetzt (${applied.length} Datei(en)).`, changedFiles: [...new Set(applied)], skipped, revision: result.revision };
  } catch (error) {
    await Promise.allSettled(previous.map((entry) => objectStore.putObject(env.S3_BUCKET, entry.key, entry.data, entry.data.byteLength, { "content-type": entry.mime })));
    throw error;
  }
});
type ReconstructionTodo = { label: string; status: "pending" | "running" | "completed" };
type ReconstructionState = {
  phase: string;
  message: string;
  processedFiles: number;
  totalFiles: number;
  processedBytes: number;
  totalBytes: number;
  todos: ReconstructionTodo[];
  entryPath?: string;
  revision?: number;
};
const reconstructionState = (phase: string, message: string, completedTodos: number, processedFiles: number, totalFiles: number, processedBytes: number, totalBytes: number, extra: Partial<ReconstructionState> = {}): ReconstructionState => ({
  phase, message, processedFiles, totalFiles, processedBytes, totalBytes,
  todos: reconstructionTodos.map((label, index) => ({ label, status: index < completedTodos ? "completed" : index === completedTodos ? "running" : "pending" })),
  ...extra
});
async function updateReconstructionJob(jobId: string, status: "queued" | "running" | "completed" | "failed", progress: number, result: ReconstructionState, errorCode?: string) {
  await db.update(jobs).set({ status, progress: Math.max(0, Math.min(100, Math.round(progress))), result, errorCode: errorCode ?? null, heartbeatAt: new Date(), updatedAt: new Date() }).where(eq(jobs.id, jobId));
}
const cleanHtmlResponse = (text: string) => text.trim().replace(/^```(?:html)?\s*/i, "").replace(/\s*```$/, "");
const reconstructionAnalysisInstructions = [
  "Du analysierst einen fortlaufenden Teil eines App-Projekts für eine pixelgenaue HTML-Rekonstruktion.",
  "Erstelle ein kompaktes Evidenzprotokoll, keine HTML-Datei und kein Markdown-Gerede.",
  "Bewahre JEDE exakte sichtbare Konstante: Breite, Höhe, x/y-Ausrichtung, Padding, Margin, Gap, Insets, Radius, Stroke, Schatten, Schriftfamilie/-größe/-gewicht/-Zeilenhöhe, Farbe inklusive Alpha, Icon-/Assetpfad und Z-Order.",
  "Dokumentiere Komponentenbaum, Constraints/Modifier-Reihenfolge, Navigation, Zustände, Texte sowie sämtliche Light-/Dark-/Zusatz-Themes.",
  "Nichts schätzen. Unsichere Beziehungen mit Quellpfad markieren. Maximal 12000 Zeichen, aber keine exakten Designwerte weglassen."
].join(" ");
async function compactReconstructionEvidence(organizationId: string, summaries: string[]): Promise<string[]> {
  let current = summaries;
  while (current.join("\n").length > 320_000) {
    const next: string[] = [];
    for (let index = 0; index < current.length; index += 18) {
      const group = current.slice(index, index + 18).join("\n\n");
      const { text } = await codexRun(organizationId, "Verdichte die folgenden UI-Evidenzprotokolle verlustfrei. Behalte alle exakten Maße, Koordinaten, Abstände, Farben, Typografie-, Theme-, Asset-, Hierarchie- und Zustandsangaben mit ihren Quellpfaden. Entferne nur Wiederholungen. Maximal 18000 Zeichen.", group, app.log);
      next.push(text.trim());
    }
    current = next;
  }
  return current;
}
async function runReconstructionJob(jobId: string, actor: Actor, projectId: string, correlationId: string) {
  try {
    const row = (await db.select({ imported: projectImports, revision: projects.revision, name: projects.name, platforms: projects.platforms }).from(projectImports).innerJoin(projects, eq(projects.id, projectImports.projectId)).where(and(eq(projectImports.projectId, projectId), eq(projectImports.organizationId, actor.organizationId))).limit(1))[0];
    if (!row) fail("IMPORT_NOT_FOUND", 404, "Für dieses Projekt liegt kein Import vor.");
    const platform = (Array.isArray(row.platforms) ? row.platforms[0] : "web") as ImportPlatform;
    const profile = previewProfiles[platform] ?? previewProfiles.web;
    const sources = reconstructionSourceFiles(row.imported.manifest);
    if (!sources.length) fail("RECONSTRUCT_NO_SOURCES", 400, "Im Projekt wurden keine lesbaren UI-Quellen gefunden.");
    const totalBytes = sources.reduce((sum, file) => sum + file.size, 0);
    await updateReconstructionJob(jobId, "running", 5, reconstructionState("inventory", "Projektdateien und UI-Ressourcen werden inventarisiert.", 0, 0, sources.length, 0, totalBytes));
    const summaries: string[] = [];
    let processedBytes = 0, processedFiles = 0, batchNumber = 0;
    for await (const batch of buildSourceBatches(sources, async (file) => objectStore.getObject(env.S3_BUCKET, `${row.imported.objectPrefix}${file.path}`))) {
      batchNumber += 1;
      const input = `Zielplattform: ${platform}; Referenzgerät: ${profile.device}; logischer Viewport: ${profile.width}x${profile.height}; Dichte: ${profile.density}.\nDies ist Analysepaket ${batchNumber}.\n${batch.text}`;
      const { text } = await codexRun(actor.organizationId, reconstructionAnalysisInstructions, input, app.log);
      summaries.push(text.trim());
      processedBytes += batch.completedBytes;
      processedFiles += batch.completedFiles;
      const ratio = totalBytes ? processedBytes / totalBytes : 1;
      await updateReconstructionJob(jobId, "running", 10 + ratio * 58, reconstructionState("analyze", `${processedFiles} von ${sources.length} UI-Dateien gründlich ausgewertet.`, ratio >= 1 ? 4 : 1, processedFiles, sources.length, processedBytes, totalBytes));
    }
    const binaryAssets = row.imported.manifest.filter((file) => !sources.some((source) => source.path === file.path));
    for (let index = 0; index < binaryAssets.length; index += 1500) summaries.push(`ASSET-INVENTAR:\n${binaryAssets.slice(index, index + 1500).map((file) => `${file.path} | ${file.mime} | ${file.size} Bytes`).join("\n")}`);
    await updateReconstructionJob(jobId, "running", 72, reconstructionState("resolve", "Themes, Assets und plattformspezifische Geometrie werden zusammengeführt.", 4, processedFiles, sources.length, processedBytes, totalBytes));
    const evidence = (await compactReconstructionEvidence(actor.organizationId, summaries)).join("\n\n");
    const reconstructionInstructions = [
      "Du bist der pixelgenaue Design-Rekonstrukteur von Werft Studio.",
      `Erzeuge EINE vollständige, direkt bearbeitbare HTML-Datei für ${platform}. Fallback-Viewport: ${profile.width}x${profile.height} (${profile.device}, Dichte ${profile.density}); wenn die Quell-Evidenz eine konkrete Fenster-, Preview- oder Gerätegeometrie nennt, ist stattdessen exakt diese Geometrie verbindlich.`,
      "Die Quell-Evidenz ist verbindlich: Komponenten müssen in derselben Hierarchie, Reihenfolge und Z-Order stehen. Übernimm alle Maße, Koordinaten, Alignments, Insets, Abstände, Farben, Typografie, Radien, Schatten, Icons, Bilder und Zustände exakt; nichts optisch 'verbessern' oder frei schätzen.",
      "Ein logisches dp/pt entspricht einem CSS-Pixel. Verwende border-box, body margin 0 und keine Zentrierung oder Skalierung, die Quellkoordinaten verschiebt. Positioniere absolute/constraint-basierte Oberflächen auch in CSS exakt; Flex/Grid nur wenn es die Original-Constraints identisch ausdrückt.",
      "Bilde alle gefundenen Screens, Dialoge und Interaktionen bearbeitbar ab. Der initial sichtbare Zustand muss dem App-Start entsprechen. Navigation und Zustandswechsel folgen dem Original.",
      "Implementiere sämtliche gefundenen Farbvarianten über prefers-color-scheme und data-theme, ohne zusätzliche sichtbare Werft-Bedienelemente einzubauen. Importierte Assets als root-relative /<exakter Manifestpfad> referenzieren; Styles und notwendige Logik sonst inline halten.",
      "Setze <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"> und exakt ein maschinenlesbares <meta name=\"werft-preview\" content='{\"platform\":\"...\",\"width\":ZAHL,\"height\":ZAHL,\"device\":\"...\",\"density\":ZAHL}'> mit der tatsächlich rekonstruierten Quellgeometrie.",
      "Antworte ausschließlich mit dem vollständigen HTML-Dokument ab <!doctype html>, ohne Markdown oder Erklärung."
    ].join(" ");
    await updateReconstructionJob(jobId, "running", 82, reconstructionState("build", "Die bearbeitbare HTML-Version wird aus der vollständigen UI-Evidenz aufgebaut.", 4, processedFiles, sources.length, processedBytes, totalBytes));
    const generated = await codexRun(actor.organizationId, reconstructionInstructions, `Projekt: ${row.name}\n\nVOLLSTÄNDIGE UI-EVIDENZ:\n${evidence}`, app.log);
    let html = cleanHtmlResponse(generated.text);
    if (!/<!doctype html|<html/i.test(html)) fail("RECONSTRUCT_INVALID", 502, "Die KI hat keine gültige HTML-Version geliefert.", true);
    await updateReconstructionJob(jobId, "running", 92, reconstructionState("verify", "Abmessungen, Positionen, Themes und Assets werden gegen die Quellen geprüft.", 5, processedFiles, sources.length, processedBytes, totalBytes));
    try {
      const verification = await codexRun(actor.organizationId, "Du bist die abschließende Pixel-Fidelity-Prüfung. Vergleiche das HTML streng mit der UI-Evidenz. Korrigiere jede abweichende Position, Größe, Ausrichtung, Farbe, Typografie, Theme-Variante, Hierarchie und jeden fehlenden Screen oder Zustand. Keine gestalterischen Änderungen. Antworte ausschließlich mit dem vollständigen korrigierten HTML-Dokument.", `UI-EVIDENZ:\n${evidence}\n\nZU PRÜFENDES HTML:\n${html}`, app.log);
      const corrected = cleanHtmlResponse(verification.text);
      if (/<!doctype html|<html/i.test(corrected)) html = corrected;
    } catch (error) {
      app.log.warn({ err: error, projectId }, "HTML-Fidelity-Prüfung fehlgeschlagen; gültige Erstrekonstruktion bleibt erhalten");
    }
    const designPath = `.werft-generated/${jobId}/design.html`;
    const data = Buffer.from(html, "utf8");
    const designObjectKey = `${row.imported.objectPrefix}${designPath}`;
    const oldGeneratedPaths = row.imported.manifest.filter((file) => /^\.werft-generated\/[0-9a-f-]+\/design\.html$/i.test(file.path) && file.path !== designPath).map((file) => file.path);
    let stored: { revision: number };
    try {
      await objectStore.putObject(env.S3_BUCKET, designObjectKey, data, data.byteLength, { "content-type": "text/html; charset=utf-8" });
      stored = await db.transaction(async (tx) => {
        await tx.execute(sql`select id from projects where id = ${projectId} and organization_id = ${actor.organizationId} for update`);
        const current = (await tx.select({ imported: projectImports, revision: projects.revision }).from(projectImports).innerJoin(projects, eq(projects.id, projectImports.projectId)).where(and(eq(projectImports.projectId, projectId), eq(projectImports.organizationId, actor.organizationId))).limit(1))[0];
        if (!current) fail("IMPORT_NOT_FOUND", 404, "Für dieses Projekt liegt kein Import vor.");
        if (current.revision !== row.revision) fail("REVISION_CONFLICT", 409, "Das importierte UI wurde während der Rekonstruktion geändert. Die Analyse wird auf dem aktuellen Stand neu gestartet.", true);
        const manifest = [...current.imported.manifest.filter((file) => !/^\.werft-generated\/[0-9a-f-]+\/design\.html$/i.test(file.path)), { path: designPath, size: data.byteLength, mime: "text/html; charset=utf-8" }];
        const revision = current.revision + 1;
        await tx.update(projectImports).set({ entryPath: designPath, manifest, totalBytes: manifest.reduce((sum, file) => sum + file.size, 0), fileCount: manifest.length }).where(eq(projectImports.projectId, projectId));
        await tx.update(projects).set({ revision, updatedAt: new Date() }).where(eq(projects.id, projectId));
        await tx.insert(auditEvents).values({ id: uuidv7(), organizationId: actor.organizationId, actorId: actor.userId, action: "design.reconstructed", targetType: "project", targetId: projectId, result: "success", metadata: { model: generated.model, sourceFiles: sources.length, sourceBytes: totalBytes, outputBytes: data.byteLength, revision, platform, profile }, correlationId });
        return { revision };
      });
    } catch (error) {
      let committed = false, commitStatusKnown = false;
      try {
        const latest = (await db.select({ manifest: projectImports.manifest }).from(projectImports).where(and(eq(projectImports.projectId, projectId), eq(projectImports.organizationId, actor.organizationId))).limit(1))[0];
        commitStatusKnown = Boolean(latest);
        committed = latest?.manifest.some((file) => file.path === designPath) === true;
      } catch (statusError) { app.log.warn({ err: statusError, projectId, designPath }, "Commit-Status der Rekonstruktion ist unklar; generiertes Objekt bleibt sicher erhalten"); }
      if (commitStatusKnown && !committed) await objectStore.removeObject(env.S3_BUCKET, designObjectKey);
      throw error;
    }
    await Promise.allSettled(oldGeneratedPaths.map((path) => objectStore.removeObject(env.S3_BUCKET, `${row.imported.objectPrefix}${path}`)));
    await updateReconstructionJob(jobId, "completed", 100, reconstructionState("completed", "HTML-Design vollständig aufgebaut und geprüft.", reconstructionTodos.length, sources.length, sources.length, totalBytes, totalBytes, { entryPath: designPath, revision: stored.revision }));
  } catch (error) {
    const details = error && typeof error === "object" ? error as Record<string, unknown> : {};
    const message = error instanceof Error ? error.message : "Die HTML-Rekonstruktion ist fehlgeschlagen.";
    const code = typeof details.code === "string" ? details.code : error instanceof Error ? error.name : "RECONSTRUCT_FAILED";
    await updateReconstructionJob(jobId, "failed", 100, reconstructionState("failed", message, 0, 0, 0, 0, 0), code);
    app.log.error({ err: error, jobId, projectId }, "Design-Rekonstruktion fehlgeschlagen");
  }
}
// In-Process-Laeufe koennen einen API-Neustart nicht ueberleben. Persistierte Zwischenzustaende
// werden deshalb beim Start freigegeben; der idempotente POST startet sie beim naechsten Poll/Öffnen neu.
await db.update(jobs).set({ status: "failed", errorCode: "RECONSTRUCT_INTERRUPTED", result: reconstructionState("interrupted", "Der Verarbeitungslauf wurde durch einen Serverneustart unterbrochen und kann sicher neu gestartet werden.", 0, 0, 0, 0, 0), updatedAt: new Date() }).where(and(eq(jobs.kind, "design-reconstruction"), sql`${jobs.status} in ('queued', 'running')`));
app.post("/api/v1/projects/:projectId/design/reconstruct", { config: { rateLimit: { max: 6, timeWindow: "10 minutes" } } }, async (request, reply) => {
  const actor = requireActorPermission(request, "design.edit");
  const { projectId } = z.object({ projectId: z.string().uuid() }).parse(request.params);
  const { retryFailed } = z.object({ retryFailed: z.boolean().optional().default(false) }).strict().parse(request.body ?? {});
  const imported = (await db.select({ revision: projects.revision }).from(projectImports).innerJoin(projects, eq(projects.id, projectImports.projectId)).where(and(eq(projectImports.projectId, projectId), eq(projectImports.organizationId, actor.organizationId))).limit(1))[0];
  if (!imported) fail("IMPORT_NOT_FOUND", 404, "Für dieses Projekt liegt kein Import vor.");
  const idempotencyKey = `${projectId}:${imported.revision}`;
  const queuedState = reconstructionState("queued", "HTML-Rekonstruktion wird vorbereitet.", 0, 0, 0, 0, 0);
  const candidateId = uuidv7();
  const inserted = await db.insert(jobs).values({ id: candidateId, organizationId: actor.organizationId, projectId, kind: "design-reconstruction", status: "queued", progress: 0, idempotencyKey, input: { projectId, revision: imported.revision }, result: queuedState, attempts: 1 }).onConflictDoNothing().returning({ id: jobs.id });
  if (inserted[0]) {
    setTimeout(() => void runReconstructionJob(candidateId, actor, projectId, request.id), 0);
    return reply.status(202).send({ jobId: candidateId, status: "queued" });
  }
  const existing = (await db.select().from(jobs).where(and(eq(jobs.organizationId, actor.organizationId), eq(jobs.kind, "design-reconstruction"), eq(jobs.idempotencyKey, idempotencyKey))).limit(1))[0];
  if (!existing) fail("JOB_CLAIM_FAILED", 409, "Der Verarbeitungslauf konnte nicht übernommen werden. Bitte erneut versuchen.", true);
  if (existing.status === "queued" || existing.status === "running" || existing.status === "completed") return reply.status(202).send({ jobId: existing.id, status: existing.status });
  if (!canRestartReconstructionJob(existing.status, retryFailed)) return reply.status(202).send({ jobId: existing.id, status: existing.status });
  const claimed = await db.update(jobs).set({ status: "queued", progress: 0, result: queuedState, errorCode: null, attempts: sql`${jobs.attempts} + 1`, updatedAt: new Date() }).where(and(eq(jobs.id, existing.id), eq(jobs.status, existing.status))).returning({ id: jobs.id });
  if (claimed[0]) setTimeout(() => void runReconstructionJob(existing.id, actor, projectId, request.id), 0);
  return reply.status(202).send({ jobId: existing.id, status: claimed[0] ? "queued" : "running" });
});
app.get("/api/v1/jobs/:jobId", async (request) => {
  const actor = requireActorPermission(request, "project.read");
  const { jobId } = z.object({ jobId: z.string().uuid() }).parse(request.params);
  const row = (await db.select({ id: jobs.id, projectId: jobs.projectId, kind: jobs.kind, status: jobs.status, progress: jobs.progress, result: jobs.result, errorCode: jobs.errorCode, updatedAt: jobs.updatedAt }).from(jobs).where(and(eq(jobs.id, jobId), eq(jobs.organizationId, actor.organizationId))).limit(1))[0];
  if (!row) fail("JOB_NOT_FOUND", 404, "Verarbeitungslauf nicht gefunden.");
  return row;
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
await cleanupOrphanImportObjects();
await app.listen({ port: env.API_PORT, host: "0.0.0.0" });

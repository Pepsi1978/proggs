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
import { and, desc, eq, isNull, lt, or, sql } from "drizzle-orm";
import Fastify, { type FastifyRequest } from "fastify";
import { Client as MinioClient } from "minio";
import { designFileName, designVariantsOf, generatedDesignPathPattern, sameVariantPattern } from "./design-variants.js";
import { v7 as uuidv7 } from "uuid";
import { z } from "zod";
import { codexAuth, codexEfforts, codexModelFields, codexModels, decryptCredentials, encryptCredentials, tokenIdentity } from "./codex-auth.js";
import { codexHttpError, isRetryableCodexError, parseCodexEventStream } from "./codex-stream.js";
import { extractDesignFacts, factCandidatePaths, orderedScreens } from "./design-extract.js";
import { factCount, renderAssetLibrary, renderFactSheet } from "./design-facts.js";
import { effectGuidance } from "./effect-catalog.js";
import { checkFidelity, fidelityAcceptable, hasIssuesForSources, renderFidelityInstructions } from "./fidelity-check.js";
import { analysisBudget, buildSourceBatches, canRestartReconstructionJob, estimateAnalysisCallCount, mapWithConcurrency, maxAnalysisBatches, previewProfileFromHtml, previewProfiles, reconstructionConcurrency, reconstructionSourceFiles, reconstructionTiming, reconstructionTodos, type ImportManifestFile, type ImportPlatform, type PreviewProfile, type ReconstructionOperationKind, type ReconstructionTimingSample } from "./import-reconstruction.js";
import { composeScreens, extractScreenFragment, screenPlanFrom, themeStyles, themeVariants } from "./screen-composer.js";
import { themeOverrideCss } from "./theme-override.js";
import { chooseEntryPath, expandZip, importLimits, isFrontendFile, isGeneratedArtifact, mimeForPath, normalizeImportPath, scoreEntryPath, validateImportFiles } from "./import-project.js";
import { paginatedObjects, type ObjectListPage } from "./paginated-objects.js";
import { previewBridgeVersion, injectPreviewCanvasBridge, rewriteRootRelativeCss, rewriteRootRelativeJavaScript } from "./preview-canvas-bridge.js";
import { cleanupOrphanedObjects } from "./storage-cleanup.js";
import { requestHostCacheCleanup } from "./host-cache-cleanup.js";

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
  S3_SECRET_KEY: z.string().min(8).default("werft-local-secret"),
  HOST_MAINTENANCE_DIR: z.string().min(1).default("/maintenance")
}).parse(process.env);
if (env.NODE_ENV === "production" && env.SESSION_SECRET === localSecret) throw new Error("SESSION_SECRET muss in Produktion explizit gesetzt sein");
const { db, client } = createDatabase(env.DATABASE_URL);
// bodyLimit 32 MB: grosse Design-Dokumente und Operations-Batches sprengen sonst das 1-MB-Fastify-Standardlimit.
const app = Fastify({ logger: { redact: ["req.headers.authorization", "req.headers.cookie", "body.password", "body.credential"] }, genReqId: () => uuidv7(), bodyLimit: 32 * 1024 * 1024 });
const s3Endpoint = new URL(env.S3_ENDPOINT);
const objectStore = new MinioClient({ endPoint: s3Endpoint.hostname, port: Number(s3Endpoint.port || (s3Endpoint.protocol === "https:" ? 443 : 80)), useSSL: s3Endpoint.protocol === "https:", accessKey: env.S3_ACCESS_KEY, secretKey: env.S3_SECRET_KEY });
let bucketReady: Promise<void> | undefined;
const pendingCodexAuth = new Map<string, PendingCodexAuth>();
let activeHostCacheCleanup: Promise<Awaited<ReturnType<typeof requestHostCacheCleanup>>> | undefined;

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
async function cleanupHostBuildCache() {
  activeHostCacheCleanup ??= requestHostCacheCleanup(env.HOST_MAINTENANCE_DIR);
  const cleanup = activeHostCacheCleanup;
  try { return await cleanup; }
  finally { if (activeHostCacheCleanup === cleanup) activeHostCacheCleanup = undefined; }
}
async function cleanupOrphanImportObjects(manual = false) {
  await ensureBucket();
  const imports = await db.select({ objectPrefix: projectImports.objectPrefix, manifest: projectImports.manifest }).from(projectImports);
  const knownObjects = new Set(imports.flatMap((row) => row.manifest.map((file) => `${row.objectPrefix}${file.path}`)));
  type ListedObject = { name?: string; size?: number; lastModified?: Date };
  type PaginatedMinio = { listObjectsV2Query(bucket: string, prefix: string, continuationToken: string, delimiter: string, maxKeys: number, startAfter: string): AsyncIterable<ObjectListPage<ListedObject>> };
  const paginatedStore = objectStore as unknown as PaginatedMinio;
  return cleanupOrphanedObjects({
    knownObjects,
    incompleteUploads: objectStore.listIncompleteUploads(env.S3_BUCKET, "", true),
    objects: paginatedObjects((token, maxKeys) => paginatedStore.listObjectsV2Query(env.S3_BUCKET, "", token, "", maxKeys, "")),
    removeIncompleteUpload: (key) => objectStore.removeIncompleteUpload(env.S3_BUCKET, key),
    removeObject: (key) => objectStore.removeObject(env.S3_BUCKET, key),
    removeIncompleteUploads: !manual,
    minimumAgeMs: manual ? 24 * 60 * 60 * 1000 : 0
  });
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
const nativeUiSourcePattern = /\.(?:xaml|axaml|kt|kts|java|swift|storyboard|xib|dart|cs|razor|qml|ui|uxml|py|rs)$/i;
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
      const isArchive = candidatePath.toLowerCase().endsWith(".zip");
      // Build-Ausgaben und Fremdabhaengigkeiten werden verworfen, bevor sie Bandbreite und
      // Objektspeicher kosten — sie sind nie Teil des Designs.
      if (candidatePath && !isArchive && ((frontendOnly && !isFrontendFile(candidatePath, platform)) || isGeneratedArtifact(candidatePath, platform))) {
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
      const selected = unpacked.filter((file) => !isGeneratedArtifact(file.path, platform) && (!frontendOnly || isFrontendFile(file.path, platform)));
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

app.get("/api/v1/health/live", async () => ({ status: "ok", version: "0.24.9-20260727.2124" }));
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
  const etag = `W/"${params.projectId}:${row.revision}:${item.path}:${previewBridgeVersion}"`;
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

app.post("/api/v1/storage/cleanup", async (request) => {
  const actor = requireActorPermission(request, "project.delete");
  let host;
  try { host = await cleanupHostBuildCache(); }
  catch (error) {
    request.log.error({ err: error, event: "storage.host_cleanup_failed" }, "Docker-Build-Cache konnte nicht bereinigt werden");
    fail("HOST_CACHE_CLEANUP_FAILED", 503, "Der Server-Cache konnte nicht bereinigt werden. Bitte den Host-Dienst prüfen.", true);
  }
  const objects = await cleanupOrphanImportObjects(true);
  const result = { ...objects, objectStoreFreedBytes: objects.freedBytes, buildCacheFreedBytes: host.freedBytes, freedBytes: objects.freedBytes + host.freedBytes, usedBytes: host.usedBytes };
  await db.insert(auditEvents).values({ id: uuidv7(), organizationId: actor.organizationId, actorId: actor.userId, action: "storage.cleaned", targetType: "organization", result: "success", metadata: result, correlationId: request.id });
  return result;
});

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
    const hasNativeUiSources = files.some((file) => nativeUiSourcePattern.test(file.path));
    // Eine native App mit eigenen UI-Quellen wird IMMER rekonstruiert. Ein zufaellig gefundenes
    // HTML (Werkzeugbericht, Doku, eingebettete Hilfeseite) darf dann nicht als Startseite gelten —
    // sonst zeigt die Leinwand eine Fremdseite und meldet sie faelschlich als fertiges Design.
    const needsReconstruction = storeAsFiles && (platform !== "web" && hasNativeUiSources);
    const detectedEntry = storeAsFiles && !needsReconstruction ? chooseEntryPath(files, platform) ?? "" : undefined;
    const entryPath = storeAsFiles ? detectedEntry ?? "" : undefined;
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
    return reply.status(201).send({ projectId, kind: storeAsFiles ? (entryPath ? "html" : "files") : "native", requiresReconstruction: storeAsFiles && (needsReconstruction || !entryPath) });
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
  // `reconstructed` unterscheidet das aus den Quellen aufgebaute Design von einer nur gefundenen
  // HTML-Datei; nur Ersteres darf als originalgetreu aufgebaut angezeigt werden.
  const reconstructed = row.imported.entryPath.startsWith(".werft-generated/");
  // Welche Geraeteformate bereits eigens aufgebaut sind. Die Buehne zeigt zum gewaehlten Geraet
  // dessen Fassung; fuer die uebrigen bleibt die Grundfassung sichtbar, bis sie gebaut werden.
  const previewBase = `/api/v1/previews/${projectId}/${previewToken(projectId)}/`;
  const variants = designVariantsOf(row.imported.manifest.map((file) => file.path))
    .map(({ width, height, path }) => ({ width, height, previewPath: `${previewBase}${path}?revision=${row.revision}` }));
  return { imported: true as const, entryPath: row.imported.entryPath, reconstructed, fileCount: row.imported.fileCount, totalBytes: row.imported.totalBytes, revision: row.revision, files: row.imported.manifest, previewWidth: profile.width, previewHeight: profile.height, previewDevice: profile.device, variants, ...(row.imported.entryPath ? { previewPath: `${previewBase}${row.imported.entryPath}?revision=${row.revision}` } : {}) };
});
// Alle Erscheinungen eines Projekts — Hell, Dunkel und eigene Farbthemes — deterministisch aus den
// Quellen gemessen, ohne KI. Ein Design, das vor dieser Fassung aufgebaut wurde, kennt nur EIN Theme;
// über diesen Weg bekommt es die übrigen nachgereicht, statt auf einen teuren Neuaufbau zu warten.
app.get("/api/v1/projects/:projectId/themes", async (request) => {
  const actor = requireActorPermission(request, "project.read");
  const { projectId } = z.object({ projectId: z.string().uuid() }).parse(request.params);
  const row = (await db.select({ imported: projectImports, platforms: projects.platforms }).from(projectImports).innerJoin(projects, eq(projects.id, projectImports.projectId)).where(and(eq(projectImports.projectId, projectId), eq(projectImports.organizationId, actor.organizationId))).limit(1))[0];
  if (!row) return { themes: [], css: "" };
  const platform = (Array.isArray(row.platforms) ? row.platforms[0] : "web") as ImportPlatform;
  const factPaths = new Set(factCandidatePaths(platform, row.imported.manifest.map((file) => file.path)));
  const factFiles = row.imported.manifest.filter((file) => factPaths.has(file.path) && file.size <= maxFactFileBytes).slice(0, maxFactFiles);
  const factTexts = await mapWithConcurrency(factFiles, factReadConcurrency, async (file) => ({ path: file.path, text: (await readObject(`${row.imported.objectPrefix}${file.path}`)).toString("utf8") }));
  const facts = extractDesignFacts(platform, factTexts);
  const variants = themeVariants(facts.themes);
  // Trägt das ausgelieferte Design seine Farben fest im Stylesheet, hilft ein Variablenblock nicht.
  // Dann wird zusätzlich eine Farbabbildung erzeugt — dieselben Regeln, andere Farben.
  let overrideCss = "";
  if (variants.length > 1 && row.imported.entryPath) {
    try {
      const html = (await readObject(`${row.imported.objectPrefix}${row.imported.entryPath}`)).toString("utf8");
      overrideCss = themeOverrideCss(html, variants);
    } catch (error) { request.log.warn({ err: error, projectId }, "Vorschaudokument für die Farbabbildung nicht lesbar"); }
  }
  request.log.info({ event: "themes.measured", projectId, platform, themes: variants.length, factFiles: factFiles.length, overrideBytes: overrideCss.length }, "Erscheinungen des Designs gemessen");
  return { themes: variants.map(({ id, name, kind, color }) => ({ id, name, kind, color })), css: themeStyles(facts), overrideCss };
});
// Rückfragen entstehen AUS dem importierten Projekt, nicht aus einer festen Liste: gefragt wird nur,
// was für die richtige Darstellung dieses Projekts wirklich fehlt — und jede Antwort ist sofort
// anwendbar (Startseite setzen, aus Quellen aufbauen).
type ImportQuestion = { id: string; kind: "entry" | "reconstruct"; question: string; why: string; options: Array<{ value: string; label: string; hint?: string }> };
app.get("/api/v1/projects/:projectId/import/questions", async (request) => {
  const actor = requireActorPermission(request, "project.read");
  const { projectId } = z.object({ projectId: z.string().uuid() }).parse(request.params);
  const row = (await db.select({ imported: projectImports, platforms: projects.platforms }).from(projectImports).innerJoin(projects, eq(projects.id, projectImports.projectId)).where(and(eq(projectImports.projectId, projectId), eq(projectImports.organizationId, actor.organizationId))).limit(1))[0];
  if (!row) return { questions: [] as ImportQuestion[] };
  const platform = (Array.isArray(row.platforms) ? row.platforms[0] : "web") as ImportPlatform;
  const { entryPath, manifest } = row.imported;
  const htmlFiles = manifest.filter((file) => /\.html?$/i.test(file.path) && !file.path.startsWith(".werft-generated/"))
    .sort((left, right) => scoreEntryPath(right.path, platform) - scoreEntryPath(left.path, platform));
  // Gezaehlt werden NUR echte native Oberflaechenquellen (Compose, SwiftUI, XAML, Flutter, Interface
  // Builder) und Android-Ressourcen-XML. HTML, CSS und JS gehoeren nicht dazu: die zeigt die Vorschau
  // bereits — sie als "Quelle zum Aufbauen" zu melden, wuerde auf die gerade sichtbare Datei zeigen.
  const uiSources = manifest.map((file) => file.path).filter((path) => !path.startsWith(".werft-generated/")
    && (nativeUiSourcePattern.test(path) || /(^|\/)res\/(values|layout|drawable|navigation|color|font|mipmap)[^/]*\/[^/]+\.xml$/i.test(path)));
  const reconstructed = entryPath.startsWith(".werft-generated/");
  const questions: ImportQuestion[] = [];
  const plural = (count: number, one: string, many: string) => `${count} ${count === 1 ? one : many}`;
  // Ohne Startseite baut die Leinwand ohnehin selbsttaetig auf — dann waere die Frage nur eine
  // Doppelung des laufenden Vorgangs. Gefragt wird nur, wenn gerade eine GEFUNDENE HTML-Datei
  // gezeigt wird, obwohl das Projekt eigene Oberflaechenquellen mitbringt.
  if (uiSources.length > 0 && !reconstructed && entryPath) questions.push({
    id: "reconstruct",
    kind: "reconstruct",
    question: `Dieses Projekt bringt ${plural(uiSources.length, "eigene Oberflächenquelle", "eigene Oberflächenquellen")} mit. Soll das Design daraus aufgebaut werden?`,
    why: `Erkannt: ${[...new Set(uiSources.map((path) => path.replace(/^.*(\.[^.]+)$/, "$1").toLowerCase()))].slice(0, 6).join(", ")}. Ohne diesen Schritt zeigt die Vorschau nur eine gefundene HTML-Datei statt der echten App.`,
    options: [{ value: "reconstruct", label: "Aus den Quellen aufbauen", hint: `${plural(uiSources.length, "Datei", "Dateien")}, dauert je nach Umfang einige Minuten` }, ...(entryPath ? [{ value: "keep", label: `Bei „${entryPath}“ bleiben` }] : [])]
  });
  // Nur fragen, wenn die Startseite wirklich offen ist. Bringt das Projekt eigene Oberflaechen-
  // quellen mit, baut die Leinwand selbsttaetig auf und bestimmt den Einstieg dabei — eine Frage
  // waere dann nicht nur ueberfluessig, sie boete auch noch beliebiges Werkzeug-HTML zur Auswahl an.
  if (!entryPath && htmlFiles.length > 0 && uiSources.length === 0) questions.push({
    id: "entry",
    kind: "entry",
    question: "Welche Datei ist der Einstieg in dieses Design?",
    why: "Für dieses Projekt liess sich keine eindeutige Startseite bestimmen; ohne sie bleibt die Vorschau leer.",
    options: htmlFiles.slice(0, 8).map((file) => ({ value: file.path, label: file.path, hint: `${Math.max(1, Math.round(file.size / 1024))} KB` }))
  });
  return { questions };
});
app.get("/api/v1/projects/:projectId/import/file", async (request) => { const actor = requireActorPermission(request, "project.read"); const { projectId } = z.object({ projectId: z.string().uuid() }).parse(request.params); const { path: filePath } = z.object({ path: z.string().min(1).max(512) }).parse(request.query); const row = (await db.select({ imported: projectImports, revision: projects.revision }).from(projectImports).innerJoin(projects, eq(projects.id, projectImports.projectId)).where(and(eq(projectImports.projectId, projectId), eq(projectImports.organizationId, actor.organizationId))).limit(1))[0]; const item = row?.imported.manifest.find((file) => file.path === filePath); if (!row || !item) fail("IMPORT_FILE_NOT_FOUND", 404, "Importdatei nicht gefunden."); if (!editableImportMime(item.mime)) fail("IMPORT_FILE_BINARY", 415, "Diese Binärdatei kann nur in der Vorschau verwendet werden."); const content = (await readObject(`${row.imported.objectPrefix}${item.path}`)).toString("utf8"); return { path: item.path, content, revision: row.revision }; });
// Eine Textänderung ist keine Interpretationsaufgabe: alter und neuer Wortlaut stehen exakt fest.
// Sie läuft deshalb OHNE KI — deterministisch, in Millisekunden und ohne Provider-Verbindung.
// Ersetzt wird nur, wenn der alte Wortlaut genau EINMAL im Projekt vorkommt; sonst träfe die
// Änderung womöglich eine gleichlautende Stelle auf einem anderen Bildschirm.
app.post("/api/v1/projects/:projectId/import/text", async (request) => {
  const actor = requireActorPermission(request, "design.edit");
  const { projectId } = z.object({ projectId: z.string().uuid() }).parse(request.params);
  const { before, after } = z.object({ before: z.string().min(1).max(2000), after: z.string().max(2000) }).strict().parse(request.body);
  if (before === after) return { applied: false as const, reason: "unverändert" };
  return db.transaction(async (tx) => {
    await tx.execute(sql`select id from projects where id = ${projectId} and organization_id = ${actor.organizationId} for update`);
    const row = (await tx.select({ imported: projectImports, revision: projects.revision }).from(projectImports).innerJoin(projects, eq(projects.id, projectImports.projectId)).where(and(eq(projectImports.projectId, projectId), eq(projectImports.organizationId, actor.organizationId))).limit(1))[0];
    if (!row) fail("IMPORT_NOT_FOUND", 404, "Für dieses Projekt liegt kein Import vor.");
    const hits: Array<{ path: string; mime: string; content: string; count: number }> = [];
    for (const file of row.imported.manifest.filter((item) => chatEditableMime(item.mime) && item.size <= 4 * 1024 * 1024)) {
      const content = (await readObject(`${row.imported.objectPrefix}${file.path}`)).toString("utf8");
      const count = content.split(before).length - 1;
      if (count > 0) hits.push({ path: file.path, mime: file.mime, content, count });
    }
    const total = hits.reduce((sum, hit) => sum + hit.count, 0);
    if (total === 0) fail("TEXT_NOT_FOUND", 409, "Dieser Wortlaut steht so nicht in den Projektdateien.");
    if (total > 1) fail("TEXT_AMBIGUOUS", 409, `Der Wortlaut kommt ${total}-mal vor. Bitte über einen markierten Bereich ändern, damit die richtige Stelle getroffen wird.`);
    const hit = hits[0]!;
    const data = Buffer.from(hit.content.replace(before, after), "utf8");
    await objectStore.putObject(env.S3_BUCKET, `${row.imported.objectPrefix}${hit.path}`, data, data.byteLength, { "content-type": hit.mime });
    const manifest = row.imported.manifest.map((file) => file.path === hit.path ? { ...file, size: data.byteLength } : file);
    const revision = row.revision + 1;
    await tx.update(projectImports).set({ manifest, totalBytes: manifest.reduce((sum, file) => sum + file.size, 0) }).where(eq(projectImports.projectId, projectId));
    await tx.update(projects).set({ revision, updatedAt: new Date() }).where(eq(projects.id, projectId));
    await tx.insert(auditEvents).values({ id: uuidv7(), organizationId: actor.organizationId, actorId: actor.userId, action: "design.text.edited", targetType: "project", targetId: projectId, result: "success", metadata: { path: hit.path, revision }, correlationId: request.id });
    return { applied: true as const, path: hit.path, revision };
  });
});
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
  "Erhalte Struktur, Funktionen und alle nicht betroffenen Inhalte vollstaendig. Pfade exakt wie geliefert.",
  "Ist der Wunsch mehrdeutig (unklar WELCHES Element, WELCHER Wert oder WELCHER Bildschirm gemeint ist), aendere NICHTS und stelle stattdessen in \"reply\" hoechstens drei kurze, nummerierte Rueckfragen. Lieber einmal nachfragen als am falschen Element arbeiten.",
  "Bezieht sich der Wunsch erkennbar auf den gerade angezeigten Bildschirm, aendere NUR dessen Abschnitt (die passende <section class=\"werft-screen\">) und keinen anderen.",
  "Ist ein MARKIERTER BEREICH angegeben, gilt er als eindeutig: aendere ausschliesslich dieses Element und frage NICHT nach, welches Element gemeint ist. Der mitgelieferte Ausschnitt stammt woertlich aus der Datei — nimm ihn als Grundlage fuer find und lasse alles ausserhalb unveraendert."
].join(" ");
function extractJsonObject(text: string): string {
  const start = text.indexOf("{"), end = text.lastIndexOf("}");
  return start >= 0 && end > start ? text.slice(start, end + 1) : text;
}
type CodexRunOptions = { operation?: string; jobId?: string; signal?: AbortSignal; onAttempt?: (attempt: number) => void };
type CodexRunResult = { text: string; model: string; attempts: number; durationMs: number; inputChars: number; outputChars: number };
async function codexRun(organizationId: string, instructions: string, input: string, log: FastifyRequest["log"], options: CodexRunOptions = {}): Promise<CodexRunResult> {
  const runStartedAt = Date.now();
  const connection = await validCodexConnection(organizationId);
  const settings = z.object({ model: z.enum(codexModels).default("gpt-5.6-sol"), effort: z.enum(codexEfforts).default("high"), fast: z.boolean().default(false) }).parse(connection.settings);
  const accountId = connection.accountId || tokenIdentity(connection.credentials.accessToken, connection.credentials.idToken).accountId;
  if (!accountId) fail("OPENAI_ACCOUNT_MISSING", 401, "Im OpenAI-Token fehlt die ChatGPT-Account-ID. Bitte erneut verbinden.");
  const body: Record<string, unknown> = { ...codexModelFields(settings.model, settings.fast), instructions, input: [{ role: "user", content: input }], store: false, stream: true };
  if (settings.effort !== "none") { body.reasoning = { effort: settings.effort, summary: "auto" }; body.include = ["reasoning.encrypted_content"]; }
  for (let attempt = 1; attempt <= 3; attempt += 1) {
    options.onAttempt?.(attempt);
    const attemptStartedAt = Date.now();
    log.info({ event: "codex.request.started", jobId: options.jobId, operation: options.operation, model: settings.model, effort: settings.effort, fast: settings.fast, attempt, maxAttempts: 3, inputChars: input.length, instructionChars: instructions.length }, "KI-Lauf gestartet");
    try {
      const signal = options.signal ? AbortSignal.any([options.signal, AbortSignal.timeout(540_000)]) : AbortSignal.timeout(540_000);
      const response = await fetch(codexAuth.responsesUrl, { method: "POST", signal, headers: { authorization: `Bearer ${connection.credentials.accessToken}`, "chatgpt-account-id": accountId, originator: "codex_cli_rs", "user-agent": "codex_cli_rs/0.0.0 (Werft Studio)", accept: "text/event-stream", "content-type": "application/json" }, body: JSON.stringify(body) });
      const raw = await response.text();
      if (!response.ok) throw codexHttpError(response.status);
      const outputText = parseCodexEventStream(raw);
      if (!outputText.trim()) fail("CHAT_EMPTY", 502, "OpenAI hat keine verwertbare Antwort geliefert. Bitte erneut versuchen.", true);
      const durationMs = Date.now() - runStartedAt;
      log.info({ event: "codex.request.completed", jobId: options.jobId, operation: options.operation, model: settings.model, attempt, attemptDurationMs: Date.now() - attemptStartedAt, durationMs, inputChars: input.length, outputChars: outputText.length, responseBytes: Buffer.byteLength(raw) }, "KI-Lauf abgeschlossen");
      return { text: outputText, model: settings.model, attempts: attempt, durationMs, inputChars: input.length, outputChars: outputText.length };
    } catch (error) {
      if (!isRetryableCodexError(error) || attempt === 3) {
        const details = error && typeof error === "object" ? error as Record<string, unknown> : {};
        log.warn({ err: error, event: "codex.request.failed", jobId: options.jobId, operation: options.operation, attempt, durationMs: Date.now() - runStartedAt }, "KI-Lauf dauerhaft fehlgeschlagen");
        if (typeof details.code === "string") throw error;
        fail("CHAT_UPSTREAM", 502, "OpenAI hat die Verbindung während des KI-Laufs beendet oder nicht rechtzeitig geantwortet. Bitte erneut versuchen.", true);
      }
      const delayMs = attempt * 2_000;
      log.warn({ err: error, event: "codex.request.retry", jobId: options.jobId, operation: options.operation, attempt, nextAttempt: attempt + 1, delayMs, attemptDurationMs: Date.now() - attemptStartedAt }, "KI-Lauf transient unterbrochen; Wiederholung wird gestartet");
      await new Promise((resolve) => setTimeout(resolve, delayMs));
    }
  }
  fail("CHAT_UPSTREAM", 502, "OpenAI hat den KI-Lauf nicht abgeschlossen.", true);
}
app.post("/api/v1/projects/:projectId/chat", { config: { rateLimit: { max: 20, timeWindow: "5 minutes" } } }, async (request) => {
  const actor = requireActorPermission(request, "design.edit");
  const { projectId } = z.object({ projectId: z.string().uuid() }).parse(request.params);
  const { message, screen, target } = z.object({
    message: z.string().min(1).max(8000),
    screen: z.string().max(200).optional(),
    // Der markierte Bereich aus der Vorschau: sein woertlicher Ausschnitt macht den Aenderungswunsch
    // eindeutig, ohne dass die KI raten oder nachfragen muss.
    target: z.object({ selector: z.string().max(600), html: z.string().max(8000), label: z.string().max(300).optional(), screenName: z.string().max(200).optional() }).strict().optional()
  }).strict().parse(request.body);
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
  const markedRegion = target
    ? `\n\n=== MARKIERTER BEREICH (genau hier anwenden) ===\nBildschirm: ${target.screenName ?? screen ?? "unbekannt"}\nElement: ${target.label ?? target.selector}\nCSS-Pfad: ${target.selector}\nWörtlicher Ausschnitt aus der Datei:\n${target.html}`
    : "";
  const inputText = `Änderungswunsch:\n${message}${markedRegion}\n\n=== PROJEKTDATEIEN ===\n${texts.map((text) => `--- ${text.path} ---\n${text.content}`).join("\n\n")}`;
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
type ReconstructionProbe = {
  runAttempt: number;
  operation: string;
  phase: string;
  status: "running" | "retrying" | "completed" | "failed";
  startedAt: string;
  completedAt?: string;
  durationMs: number;
  attempts: number;
  inputChars: number;
  outputChars?: number;
  sourceFiles?: number;
  sourceBytes?: number;
  rssBytes?: number;
  heapUsedBytes?: number;
  errorCode?: string;
};
type ReconstructionState = {
  phase: string;
  message: string;
  processedFiles: number;
  totalFiles: number;
  processedBytes: number;
  totalBytes: number;
  todos: ReconstructionTodo[];
  phaseProgress: number | null;
  elapsedMs: number;
  estimatedRemainingMs: number | null;
  completedOperations: number;
  totalOperations: number;
  retryCount: number;
  probes: ReconstructionProbe[];
  startedAt?: string;
  currentOperation?: string;
  entryPath?: string;
  revision?: number;
};
const reconstructionState = (phase: string, message: string, completedTodos: number, processedFiles: number, totalFiles: number, processedBytes: number, totalBytes: number, extra: Partial<ReconstructionState> = {}): ReconstructionState => ({
  phase, message, processedFiles, totalFiles, processedBytes, totalBytes,
  todos: reconstructionTodos.map((label, index) => ({ label, status: index < completedTodos ? "completed" : index === completedTodos ? "running" : "pending" })),
  phaseProgress: null, elapsedMs: 0, estimatedRemainingMs: null, completedOperations: 0, totalOperations: 0, retryCount: 0, probes: [],
  ...extra
});
async function updateReconstructionJob(jobId: string, runAttempt: number, status: "queued" | "running" | "completed" | "failed", progress: number, result: ReconstructionState, errorCode?: string) {
  const updated = await db.update(jobs).set({ status, progress: Math.max(0, Math.min(100, Math.round(progress))), result, errorCode: errorCode ?? null, heartbeatAt: new Date(), updatedAt: new Date() }).where(and(eq(jobs.id, jobId), eq(jobs.attempts, runAttempt), sql`${jobs.status} in ('queued', 'running')`)).returning({ id: jobs.id });
  if (!updated[0]) fail("RECONSTRUCT_LEASE_LOST", 409, "Dieser Rekonstruktionslauf wurde durch einen neueren Versuch ersetzt.");
}
// Grenzen der deterministischen Messung: sie soll Sekunden dauern und darf den Speicher nicht sprengen.
const maxFactFileBytes = 2 * 1024 * 1024, maxFactFiles = 6_000, factReadConcurrency = 12, maxReconstructedScreens = 24;
const maxScreenSourceChars = 90_000, maxPublishedProbes = 40, analysisBatchChars = 220_000;
const reconstructionAnalysisInstructions = [
  "Du analysierst einen fortlaufenden Teil eines App-Projekts für eine pixelgenaue HTML-Rekonstruktion.",
  "Erstelle ein kompaktes Evidenzprotokoll, keine HTML-Datei und kein Markdown-Gerede.",
  "Die mitgelieferten DESIGN-FAKTEN sind bereits exakt gemessen — wiederhole sie nicht, sondern beschreibe, WELCHES Element WELCHEN dieser Werte benutzt.",
  "Bewahre JEDE exakte sichtbare Konstante, die in den Fakten noch fehlt: Breite, Höhe, x/y-Ausrichtung, Padding, Margin, Gap, Insets, Radius, Stroke, Schatten, Schriftfamilie/-größe/-gewicht/-Zeilenhöhe, Farbe inklusive Alpha, Icon-/Assetpfad und Z-Order.",
  "Dokumentiere Komponentenbaum, Constraints/Modifier-Reihenfolge, Navigation, Zustände, Texte sowie sämtliche Light-/Dark-/Zusatz-Themes — jeweils mit dem Bildschirm, zu dem sie gehören.",
  "Halte JEDE Bewegung und JEDE Zustandsreaktion fest: Dauerschleifen (pulsierende Ringe, Wellen), Ein-/Ausblenden, Übergänge, Ripple, gedrückte und ausgewählte Zustände, Aufklapper und Blätter — mit Dauer, Kurve und dem Element, an dem sie hängen. Sie gehören zum Design und fehlen sonst vollständig.",
  "Nichts schätzen. Unsichere Beziehungen mit Quellpfad markieren. Maximal 12000 Zeichen, aber keine exakten Designwerte weglassen."
].join(" ");

// Bildschirm-Aufbau: bewusst ein FRAGMENT statt eines ganzen Dokuments — nur so passt jeder
// Bildschirm vollstaendig in eine Antwort, und der Zusammenbau bleibt deterministisch.
function buildScreenInstructions(platform: ImportPlatform, profile: PreviewProfile, screenIds: string[]): string {
  return [
    "Du bist der pixelgenaue Design-Rekonstrukteur von Werft Studio und baust GENAU EINEN Bildschirm einer bestehenden App nach.",
    `Zielplattform ${platform}, Bildschirmfläche exakt ${profile.width}x${profile.height} CSS-Pixel (${profile.device}, Dichte ${profile.density}). Ein logisches dp/sp/pt/DIP ist exakt ein CSS-Pixel.`,
    "Die DESIGN-FAKTEN sind gemessene Quellwerte und damit verbindlich: übernimm Farben, Maße, Abstände, Radien, Schriftgrößen und Effekte ZEICHENGENAU aus ihnen. Nichts runden, nichts „verschönern“, nichts erfinden.",
    "Die ORIGINALQUELLEN dieses Bildschirms schlagen im Zweifel jede verdichtete Evidenz.",
    "Positioniere so, wie es das Original tut: Constraint-/absolut-basierte Oberflächen absolut, Stack-/Flow-Layouts als Flex/Grid mit exakt denselben Gaps und Insets. Kein zusätzliches Zentrieren, kein Skalieren, keine eigenen Außenabstände.",
    "Original-Icons aus der Icon-Bibliothek inline als <svg> einsetzen; niemals Ersatzsymbole, Emoji oder Fremd-Icons. Bitmap-Assets als /<exakter Manifestpfad> referenzieren.",
    effectGuidance,
    `Verlinke Navigationsziele über data-werft-navigate="ZIEL-ID"; gültige IDs sind: ${screenIds.join("; ") || "keine"}.`,
    "JEDES Bedienelement, das im Original einen anderen Bildschirm öffnet, MUSS data-werft-navigate tragen — auch Zurück-Pfeile, Listeneinträge, Karten, Kacheln, Symbole und Leisten-Einträge. Ohne diese Verknüpfungen ist die Rekonstruktion nicht durchklickbar.",
    "Farben IMMER über die bereitgestellten CSS-Variablen der Themes, wo die Quelle ein Theme-Token benutzt — sonst den exakten Farbwert. Feste Farbwerte statt Variablen machen das Design unumschaltbar.",
    "Bringt das Original einen Umschalter für Hell/Dunkel oder die Darstellung mit (Mond-/Sonnensymbol, Eintrag „Darstellung“/„Erscheinungsbild“), baue ihn nach UND gib ihm data-werft-theme-toggle. Erfinde KEINEN zusätzlichen Umschalter, keine Werkzeugleisten und keine Hinweistexte.",
    "Antworte AUSSCHLIESSLICH mit dem HTML-Fragment dieses einen Bildschirms (optional ein <style>-Block davor, dessen Selektoren eindeutig zu diesem Bildschirm gehören). Kein <!doctype>, kein <html>, kein <body>, kein Markdown, keine Erklärung."
  ].join("\n");
}

const fidelityRepairInstructions = [
  "Du bist die Nachmessung der Design-Rekonstruktion von Werft Studio.",
  "Die aufgeführten Abweichungen wurden PROGRAMMATISCH gemessen: jeder Punkt nennt einen Quellwert, der im Markup fehlt oder falsch ist.",
  "Korrigiere ausschließlich diese Punkte, soweit sie diesen Bildschirm betreffen. Ändere nichts an Aufbau, Inhalt oder Gestaltung, was nicht in der Liste steht.",
  "Betrifft ein Punkt einen anderen Bildschirm, lass ihn unverändert — erfinde dafür keine Elemente.",
  effectGuidance,
  "Antworte AUSSCHLIESSLICH mit dem vollständigen korrigierten Fragment dieses Bildschirms (optional ein <style>-Block davor). Kein <!doctype>, kein <html>, kein <body>, kein Markdown."
].join("\n");

// Beim screenweisen Aufbau bekommt jeder Bildschirm seinen eigenen, UNVERDICHTETEN Quelltext —
// genau das rettet die Details, die eine Evidenz-Verdichtung sonst wegkuerzt.
async function readScreenSources(objectPrefix: string, manifest: ImportManifestFile[], wanted: string[]): Promise<string> {
  if (!wanted.length) return "";
  const files = manifest.filter((file) => wanted.some((entry) => file.path === entry || file.path.endsWith(`/${entry}`) || file.path.endsWith(`/${entry}.xml`)));
  const parts: string[] = [];
  let budget = maxScreenSourceChars;
  for (const file of files) {
    if (budget <= 0) break;
    try {
      const text = (await readObject(`${objectPrefix}${file.path}`)).toString("utf8").slice(0, budget);
      budget -= text.length;
      parts.push(`--- ${file.path} ---\n${text}`);
    } catch (error) { app.log.warn({ err: error, path: file.path }, "Bildschirmquelle nicht lesbar"); }
  }
  return parts.join("\n\n");
}

type ReconstructionCodexRunner = (operation: string, instructions: string, input: string, remainingCompactionCalls: number, newlyPlannedCalls: number) => Promise<CodexRunResult>;
const compactionInstructions = "Verdichte die folgenden UI-Evidenzprotokolle verlustfrei. Behalte alle exakten Maße, Koordinaten, Abstände, Farben, Typografie-, Theme-, Asset-, Hierarchie- und Zustandsangaben mit ihren Quellpfaden sowie die Zuordnung zu Bildschirmen. Entferne nur Wiederholungen. Maximal 18000 Zeichen.";
// Die Verdichtungsgruppen einer Runde sind voneinander unabhaengig und laufen deshalb nebenlaeufig.
async function compactReconstructionEvidence(summaries: string[], run: ReconstructionCodexRunner): Promise<string[]> {
  let current = summaries;
  let round = 0;
  while (current.join("\n").length > 320_000) {
    round += 1;
    const groups: string[][] = [];
    for (let index = 0; index < current.length; index += 18) groups.push(current.slice(index, index + 18));
    const groupCount = groups.length;
    current = await mapWithConcurrency(groups, reconstructionConcurrency, async (group, index) => {
      const { text } = await run(`Evidenz verdichten ${round}.${index + 1}`, compactionInstructions, group.join("\n\n"), groupCount - index - 1, index === 0 ? groupCount : 0);
      return text.trim();
    });
  }
  return current;
}
async function runReconstructionJob(jobId: string, runAttempt: number, actor: Actor, projectId: string, correlationId: string, previousProbes: ReconstructionProbe[] = [], targetViewport?: { width: number; height: number; device: string }) {
  const jobStartedAt = Date.now();
  const startedAt = new Date(jobStartedAt).toISOString();
  const probes: ReconstructionProbe[] = previousProbes.map((probe) => ({ ...probe }));
  const timingSamples: ReconstructionTimingSample[] = [];
  let completedOperations = 0, totalOperations = 0, retryCount = 0, latestProgress = 0;
  let latestState = reconstructionState("starting", "HTML-Rekonstruktion wird gestartet.", 0, 0, 0, 0, 0, { startedAt });
  const runtimeState = (base: ReconstructionState, phaseProgress: number | null, estimatedRemainingMs: number | null, currentOperation?: string): ReconstructionState => ({
    ...base,
    phaseProgress,
    elapsedMs: Date.now() - jobStartedAt,
    estimatedRemainingMs,
    completedOperations,
    totalOperations,
    retryCount,
    // Ein Lauf erzeugt jetzt Dutzende Schritte. Der veroeffentlichte Zustand wird alle zwei Sekunden
    // je laufendem Schritt geschrieben — ohne Begrenzung waechst jede dieser Schreibungen mit.
    probes: probes.slice(-maxPublishedProbes).map((probe) => ({ ...probe })),
    startedAt,
    ...(currentOperation ? { currentOperation } : {})
  });
  const publish = async (status: "queued" | "running" | "completed" | "failed", progress: number, state: ReconstructionState, errorCode?: string) => {
    // Mehrere Schritte laufen jetzt gleichzeitig und melden eigene Fortschrittswerte. Der Balken
    // darf dabei nie zurueckspringen, sonst wirkt der Lauf haengend.
    const effective = status === "running" ? Math.max(latestProgress, progress) : progress;
    latestProgress = effective;
    latestState = state;
    await updateReconstructionJob(jobId, runAttempt, status, effective, state, errorCode);
  };
  const publishMilestone = async (progress: number, state: ReconstructionState, remainingWeight: number, phaseProgress: number | null = 100, showEta = true) => {
    const timing = reconstructionTiming(timingSamples, null, 0, remainingWeight);
    await publish("running", progress, runtimeState(state, phaseProgress, showEta ? timing.estimatedRemainingMs : null));
  };
  const runCodexStep = async ({ operation, phase, kind, instructions, input, progress, remainingWeight, state, sourceFiles, sourceBytes, validate, showEta }: {
    operation: string;
    phase: string;
    kind: ReconstructionOperationKind;
    instructions: string;
    input: string;
    progress: number;
    remainingWeight: number;
    state: () => ReconstructionState;
    validate?: (result: CodexRunResult) => void;
    showEta?: boolean;
    sourceFiles?: number;
    sourceBytes?: number;
  }): Promise<CodexRunResult> => {
    const operationStartedAt = Date.now();
    const controller = new AbortController();
    let attempts = 0, settled = false, failed = false, failure: unknown, result: CodexRunResult | undefined;
    let lastHeartbeatLogAt = 0;
    const probe: ReconstructionProbe = { runAttempt, operation, phase, status: "running", startedAt: new Date(operationStartedAt).toISOString(), durationMs: 0, attempts: 0, inputChars: input.length, ...(sourceFiles === undefined ? {} : { sourceFiles }), ...(sourceBytes === undefined ? {} : { sourceBytes }) };
    probes.push(probe);
    const initialTiming = reconstructionTiming(timingSamples, kind, 0, remainingWeight);
    await publish("running", progress, runtimeState(state(), initialTiming.phaseProgress, showEta === false ? null : initialTiming.estimatedRemainingMs, operation));
    const tracked = codexRun(actor.organizationId, instructions, input, app.log, {
      operation,
      jobId,
      signal: controller.signal,
      onAttempt: (attempt) => {
        if (attempts > 0 && attempt > attempts) retryCount += attempt - attempts;
        attempts = attempt;
        probe.attempts = attempt;
        probe.status = attempt > 1 ? "retrying" : "running";
      }
    }).then((value) => { result = value; }, (error: unknown) => { failed = true; failure = error; }).finally(() => { settled = true; });
    while (!settled) {
      await Promise.race([tracked, new Promise((resolve) => setTimeout(resolve, 2_000))]);
      if (settled) break;
      const elapsedMs = Date.now() - operationStartedAt;
      probe.durationMs = elapsedMs;
      const timing = reconstructionTiming(timingSamples, kind, elapsedMs, remainingWeight);
      const base = state();
      if (attempts > 1) base.message = `${base.message} Verbindungswiederholung ${attempts} von 3.`;
      try {
        await publish("running", progress, runtimeState(base, timing.phaseProgress, showEta === false ? null : timing.estimatedRemainingMs, operation));
      } catch (heartbeatError) {
        const heartbeatDetails = heartbeatError && typeof heartbeatError === "object" ? heartbeatError as Record<string, unknown> : {};
        if (heartbeatDetails.code === "RECONSTRUCT_LEASE_LOST") {
          controller.abort(heartbeatError);
          await tracked;
          throw heartbeatError;
        }
        app.log.warn({ err: heartbeatError, event: "reconstruction.heartbeat.persist_failed", jobId, projectId, operation, runAttempt }, "Rekonstruktions-Heartbeat konnte nicht gespeichert werden");
      }
      if (Date.now() - lastHeartbeatLogAt >= 15_000) {
        lastHeartbeatLogAt = Date.now();
        const memory = process.memoryUsage();
        app.log.info({ event: "reconstruction.heartbeat", jobId, projectId, operation, phase, attempts, elapsedMs, progress: latestProgress, phaseProgress: timing.phaseProgress, estimatedRemainingMs: timing.estimatedRemainingMs, rssBytes: memory.rss, heapUsedBytes: memory.heapUsed }, "Rekonstruktionsschritt läuft");
      }
    }
    await tracked;
    const memory = process.memoryUsage();
    probe.completedAt = new Date().toISOString();
    probe.durationMs = Date.now() - operationStartedAt;
    probe.attempts = attempts;
    probe.rssBytes = memory.rss;
    probe.heapUsedBytes = memory.heapUsed;
    if (failed || !result) {
      const details = failure && typeof failure === "object" ? failure as Record<string, unknown> : {};
      probe.status = "failed";
      probe.errorCode = typeof details.code === "string" ? details.code : failure instanceof Error ? failure.name : "RECONSTRUCT_STEP_FAILED";
      throw failure;
    }
    probe.outputChars = result.outputChars;
    try { validate?.(result); }
    catch (validationError) {
      const validationDetails = validationError && typeof validationError === "object" ? validationError as Record<string, unknown> : {};
      probe.status = "failed";
      probe.errorCode = typeof validationDetails.code === "string" ? validationDetails.code : validationError instanceof Error ? validationError.name : "RECONSTRUCT_INVALID";
      throw validationError;
    }
    probe.status = "completed";
    timingSamples.push({ kind, durationMs: result.durationMs });
    completedOperations += 1;
    return result;
  };
  try {
    const row = (await db.select({ imported: projectImports, revision: projects.revision, name: projects.name, platforms: projects.platforms }).from(projectImports).innerJoin(projects, eq(projects.id, projectImports.projectId)).where(and(eq(projectImports.projectId, projectId), eq(projectImports.organizationId, actor.organizationId))).limit(1))[0];
    if (!row) fail("IMPORT_NOT_FOUND", 404, "Für dieses Projekt liegt kein Import vor.");
    const platform = (Array.isArray(row.platforms) ? row.platforms[0] : "web") as ImportPlatform;
    let profile = previewProfiles[platform] ?? previewProfiles.web;
    const uiSources = reconstructionSourceFiles(row.imported.manifest);
    if (!uiSources.length) fail("RECONSTRUCT_NO_SOURCES", 400, "Im Projekt wurden keine lesbaren UI-Quellen gefunden.");
    const { analyzed: sources, skipped: skippedSources } = analysisBudget(uiSources, maxAnalysisBatches, analysisBatchChars);
    const totalBytes = sources.reduce((sum, file) => sum + file.size, 0);
    const estimatedAnalysisCalls = Math.min(maxAnalysisBatches, estimateAnalysisCallCount(totalBytes, analysisBatchChars));
    if (skippedSources.length) app.log.warn({ event: "reconstruction.analysis_capped", jobId, projectId, analyzed: sources.length, skipped: skippedSources.length, skippedExamples: skippedSources.slice(0, 10).map((file) => file.path) }, "Analysebudget erreicht; weitere UI-Quellen gehen nur über den bildschirmweisen Aufbau ein");
    app.log.info({ event: "reconstruction.started", jobId, projectId, platform, uiSourceFiles: uiSources.length, analyzedFiles: sources.length, sourceBytes: totalBytes, estimatedAnalysisCalls, totalManifestFiles: row.imported.manifest.length }, "Design-Rekonstruktion gestartet");
    await publishMilestone(4, reconstructionState("inventory", `${row.imported.manifest.length} Projektdateien inventarisiert; ${uiSources.length} davon beschreiben Oberfläche.`, 1, 0, sources.length, 0, totalBytes), estimatedAnalysisCalls + 5);

    // Schritt 1: exakt messen statt schaetzen. Farben, Maße, Typografie, Formen, Effekte, Icons und
    // Screens werden deterministisch aus den Quellen geparst — ohne KI und in Sekunden. Diese Werte
    // ueberleben jede spaetere Verdichtung und sind fuer Aufbau und Nachpruefung verbindlich.
    const factPaths = new Set(factCandidatePaths(platform, row.imported.manifest.map((file) => file.path)));
    const factFiles = row.imported.manifest.filter((file) => factPaths.has(file.path) && file.size <= maxFactFileBytes).slice(0, maxFactFiles);
    const factTexts = await mapWithConcurrency(factFiles, factReadConcurrency, async (file) => ({ path: file.path, text: (await readObject(`${row.imported.objectPrefix}${file.path}`)).toString("utf8") }));
    const facts = extractDesignFacts(platform, factTexts);
    if (facts.viewport) profile = { width: facts.viewport.width, height: facts.viewport.height, device: facts.viewport.device, density: facts.viewport.density };
    // Ist ein Referenzgeraet gewaehlt, wird FUER DESSEN Flaeche gebaut: nur so entsteht ein Design,
    // das die Breite eines aufgeklappten Foldables oder das Querformat wirklich nutzt, statt in der
    // Ecke des Rahmens zu stehen. Die gemessene Punktdichte des Projekts bleibt dabei erhalten.
    if (targetViewport) profile = { width: targetViewport.width, height: targetViewport.height, device: targetViewport.device, density: profile.density };
    const factSheet = renderFactSheet(facts);
    const assetLibrary = renderAssetLibrary(facts);
    const measuredValues = factCount(facts);
    app.log.info({ event: "reconstruction.facts_measured", jobId, projectId, platform, measuredValues, colors: facts.colors.length, dimensions: facts.dimensions.length, effects: facts.effects.length, icons: facts.assets.filter((asset) => asset.svg).length, screens: facts.screens.length, factFiles: factFiles.length }, "Designwerte deterministisch gemessen");
    await publishMilestone(9, reconstructionState("measure", `${measuredValues} exakte Designwerte aus ${factFiles.length} Quelldateien gemessen: ${facts.colors.length} Farben, ${facts.dimensions.length} Maße, ${facts.effects.length} Effekte, ${facts.screens.length} Bildschirme.`, 2, 0, sources.length, 0, totalBytes), estimatedAnalysisCalls + 4);

    // Schritt 2: Analysepakete laufen jetzt nebenlaeufig statt streng nacheinander — das ist der
    // groesste Zeitgewinn; die Reihenfolge der Evidenz bleibt trotzdem stabil.
    const summaries: string[] = [];
    let processedBytes = 0, processedFiles = 0, batchNumber = 0;
    const pendingBatches: Array<{ number: number; text: string; completedBytes: number; completedFiles: number }> = [];
    const flushBatches = async () => {
      if (!pendingBatches.length) return;
      const window = pendingBatches.splice(0, pendingBatches.length);
      const ratioBefore = totalBytes ? processedBytes / totalBytes : 0;
      const texts = await mapWithConcurrency(window, reconstructionConcurrency, async (batch) => {
        const input = `Zielplattform: ${platform}; Referenzgerät: ${profile.device}; logischer Viewport: ${profile.width}x${profile.height}; Dichte: ${profile.density}.\nDies ist Analysepaket ${batch.number}.\n\n${factSheet}\n\n=== QUELLTEXT DIESES PAKETS ===\n${batch.text}`;
        const { text } = await runCodexStep({ operation: `UI-Analysepaket ${batch.number}`, phase: "analyze", kind: "analysis", instructions: reconstructionAnalysisInstructions, input, progress: 10 + ratioBefore * 50, remainingWeight: 5, showEta: false, sourceFiles: batch.completedFiles, sourceBytes: batch.completedBytes, state: () => reconstructionState("analyze", `${window.length} UI-Analysepakete werden gleichzeitig ausgewertet; ${processedFiles} von ${sources.length} Dateien sind abgeschlossen.`, 2, processedFiles, sources.length, processedBytes, totalBytes) });
        return text.trim();
      });
      summaries.push(...texts);
      for (const batch of window) { processedBytes += batch.completedBytes; processedFiles += batch.completedFiles; }
      const ratio = totalBytes ? processedBytes / totalBytes : 1;
      await publishMilestone(10 + ratio * 50, reconstructionState("analyze", `${processedFiles} von ${sources.length} UI-Dateien gründlich ausgewertet${skippedSources.length ? `; ${skippedSources.length} weitere gehen direkt in den bildschirmweisen Aufbau ein` : ""}.`, ratio >= 1 ? 3 : 2, processedFiles, sources.length, processedBytes, totalBytes), 5, 100, false);
    };
    for await (const batch of buildSourceBatches(sources, async (file) => objectStore.getObject(env.S3_BUCKET, `${row.imported.objectPrefix}${file.path}`))) {
      batchNumber += 1;
      pendingBatches.push({ ...batch, number: batchNumber });
      if (pendingBatches.length >= reconstructionConcurrency) await flushBatches();
    }
    await flushBatches();
    const binaryAssets = row.imported.manifest.filter((file) => !sources.some((source) => source.path === file.path));
    for (let index = 0; index < binaryAssets.length; index += 1500) summaries.push(`ASSET-INVENTAR:\n${binaryAssets.slice(index, index + 1500).map((file) => `${file.path} | ${file.mime} | ${file.size} Bytes`).join("\n")}`);
    await publishMilestone(62, reconstructionState("resolve", "Themes, Assets und plattformspezifische Geometrie werden zusammengeführt.", 3, processedFiles, sources.length, processedBytes, totalBytes), 5, 0);
    const evidence = (await compactReconstructionEvidence(summaries, async (operation, instructions, input, remainingCompactionCalls, newlyPlannedCalls) => {
      totalOperations += newlyPlannedCalls;
      return runCodexStep({ operation, phase: "resolve", kind: "compaction", instructions, input, progress: 62, remainingWeight: remainingCompactionCalls * 0.8 + 5, state: () => reconstructionState("resolve", "Exakte UI-Evidenz wird verlustfrei verdichtet.", 3, processedFiles, sources.length, processedBytes, totalBytes) });
    })).join("\n\n");

    // Schritt 3: jeder Bildschirm wird EINZELN und nebenlaeufig gebaut. Vorher entstand die ganze
    // App in einem Aufruf — der lief in die Ausgabegrenze, liess Screens weg und rundete Werte.
    const allScreens = orderedScreens(facts);
    const screenPlan = screenPlanFrom({ ...facts, screens: allScreens }, row.name).slice(0, maxReconstructedScreens);
    if (allScreens.length > screenPlan.length) app.log.warn({ event: "reconstruction.screens_capped", jobId, projectId, found: allScreens.length, built: screenPlan.length, skipped: allScreens.slice(screenPlan.length).map((screen) => screen.id) }, "Mehr Bildschirme gefunden als aufgebaut werden");
    totalOperations = completedOperations + screenPlan.length + 1;
    const screenIndex = new Map(screenPlan.map((screen, index) => [screen.id, index] as const));
    const screenInstructions = buildScreenInstructions(platform, profile, screenPlan.map((screen) => `${screen.id} = „${screen.name}“`));
    await publishMilestone(66, reconstructionState("build", `${screenPlan.length} Bildschirm(e) werden einzeln und originalgetreu aufgebaut.`, 4, processedFiles, sources.length, processedBytes, totalBytes), screenPlan.length + 1, 0);
    let builtScreens = 0;
    const fragments = await mapWithConcurrency(screenPlan, reconstructionConcurrency, async (screen, index) => {
      const screenSources = await readScreenSources(row.imported.objectPrefix, row.imported.manifest, screen.files);
      const navigation = screen.navigatesTo.filter((target) => screenIndex.has(target));
      const input = [
        `Projekt: ${row.name}`,
        `Aufzubauender Bildschirm ${index + 1} von ${screenPlan.length}: „${screen.name}“ (id=${screen.id}, Art=${screen.kind}${screen.isStart ? ", STARTBILDSCHIRM" : ""}${screen.route ? `, route=${screen.route}` : ""}), Quelle ${screen.source}.`,
        navigation.length ? `Von hier aus erreichbar (als data-werft-navigate="ZIEL-ID" verlinken): ${navigation.join(", ")}` : "",
        factSheet,
        assetLibrary,
        screenSources ? `\n# ORIGINALQUELLEN GENAU DIESES BILDSCHIRMS (verbindlich, unverdichtet)\n${screenSources}` : "",
        `\n# GESAMT-EVIDENZ DES PROJEKTS (Kontext)\n${evidence}`
      ].filter(Boolean).join("\n");
      const result = await runCodexStep({
        operation: `Bildschirm „${screen.name}“ aufbauen`, phase: "build", kind: "build", instructions: screenInstructions, input,
        progress: 66 + (builtScreens / screenPlan.length) * 22, remainingWeight: 1,
        state: () => reconstructionState("build", `${builtScreens} von ${screenPlan.length} Bildschirmen originalgetreu aufgebaut.`, 4, processedFiles, sources.length, processedBytes, totalBytes),
        validate: (value) => { if (!extractScreenFragment(value.text).markup) fail("RECONSTRUCT_INVALID", 502, `Für den Bildschirm „${screen.name}“ kam kein verwertbares Markup zurück.`, true); }
      });
      builtScreens += 1;
      const fragment = extractScreenFragment(result.text);
      return { screen, markup: fragment.markup, css: fragment.css };
    });

    // Schritt 4: nachmessen statt nur nachfragen. Das Ergebnis wird gegen die geparsten Quellwerte
    // geprueft; nur die tatsaechlich abweichenden Punkte gehen in einen gezielten Korrekturlauf.
    const composeAll = (parts: typeof fragments) => composeScreens(parts.map(({ screen, markup }) => ({ id: screen.id, name: screen.name, markup, isStart: screen.isStart, navigatesTo: screen.navigatesTo })), { title: row.name, platform, width: profile.width, height: profile.height, device: profile.device, density: profile.density, facts, sharedCss: parts.map((part) => part.css).filter(Boolean).join("\n") });
    let html = composeAll(fragments);
    let report = checkFidelity(html, facts);
    app.log.info({ event: "reconstruction.fidelity", jobId, projectId, round: 0, score: report.score, checked: report.checked, matched: report.matched, issues: report.issues.length }, "Fidelity gemessen");
    await publishMilestone(88, reconstructionState("verify", `Nachmessung: ${report.score} % der gemessenen Quellwerte stimmen exakt (${report.matched} von ${report.checked}).`, 5, processedFiles, sources.length, processedBytes, totalBytes), 1, 0);
    if (!fidelityAcceptable(report)) {
      // Nur Bildschirme nachbessern, die wirklich betroffen sind — und jeder bekommt AUSSCHLIESSLICH
      // die Abweichungen aus seinen eigenen Quelldateien. Sonst baut Bildschirm B Werte ein, die zu
      // Bildschirm A gehoeren, und die Rekonstruktion wird schlechter statt besser.
      const affected = fragments.filter((fragment) => hasIssuesForSources(report, fragment.screen.files));
      totalOperations += affected.length;
      let correctedScreens = 0;
      app.log.info({ event: "reconstruction.repair_planned", jobId, projectId, affected: affected.length, total: fragments.length, issues: report.issues.length }, "Gezielter Korrekturlauf geplant");
      const repairedByScreen = new Map((await mapWithConcurrency(affected, reconstructionConcurrency, async (fragment) => {
        const corrections = renderFidelityInstructions(report, { sources: fragment.screen.files });
        const result = await runCodexStep({
          operation: `Bildschirm „${fragment.screen.name}“ nachmessen`, phase: "verify", kind: "verification",
          instructions: fidelityRepairInstructions, input: [`Bildschirm: „${fragment.screen.name}“ (id=${fragment.screen.id}).`, factSheet, `\n# GEMESSENE ABWEICHUNGEN DIESES BILDSCHIRMS\n${corrections}`, `\n# ZU KORRIGIERENDES MARKUP DIESES BILDSCHIRMS\n<style>${fragment.css}</style>\n${fragment.markup}`].join("\n"),
          progress: 88 + (correctedScreens / Math.max(1, affected.length)) * 8, remainingWeight: 0,
          state: () => reconstructionState("verify", `${correctedScreens} von ${affected.length} betroffenen Bildschirmen gegen die gemessenen Quellwerte korrigiert.`, 5, processedFiles, sources.length, processedBytes, totalBytes),
          validate: (value) => { if (!extractScreenFragment(value.text).markup) fail("RECONSTRUCT_INVALID", 502, `Die Nachmessung von „${fragment.screen.name}“ lieferte kein verwertbares Markup.`, true); }
        });
        correctedScreens += 1;
        const repairedFragment = extractScreenFragment(result.text);
        return [fragment.screen.id, { screen: fragment.screen, markup: repairedFragment.markup, css: repairedFragment.css || fragment.css }] as const;
      })));
      const repaired = fragments.map((fragment) => repairedByScreen.get(fragment.screen.id) ?? fragment);
      const repairedHtml = composeAll(repaired);
      const repairedReport = checkFidelity(repairedHtml, facts);
      app.log.info({ event: "reconstruction.fidelity", jobId, projectId, round: 1, score: repairedReport.score, before: report.score, issues: repairedReport.issues.length }, "Fidelity nach Korrekturlauf gemessen");
      // Nur uebernehmen, wenn die Korrektur messbar besser ist — sonst bleibt der bessere Stand.
      if (repairedReport.score >= report.score) { html = repairedHtml; report = repairedReport; }
    }
    const fidelityScore = report.score;
    const designFile = designFileName(targetViewport);
    const variantPattern = sameVariantPattern(designFile);
    const designPath = `.werft-generated/${jobId}/${runAttempt}/${designFile}`;
    const data = Buffer.from(html, "utf8");
    const designObjectKey = `${row.imported.objectPrefix}${designPath}`;
    // Aufgeraeumt wird nur die VORHERIGE Fassung DIESES Formats — die Fassungen der anderen Formate
    // bleiben liegen, sonst waere nach jedem Aufbau nur noch ein einziges Format vorhanden.
    const oldGeneratedPaths = row.imported.manifest.filter((file) => variantPattern.test(file.path) && file.path !== designPath).map((file) => file.path);
    let stored: { revision: number; state: ReconstructionState } | undefined;
    await publishMilestone(98, reconstructionState("store", "Das vollständig geprüfte HTML wird atomar gespeichert.", reconstructionTodos.length, processedFiles, sources.length, processedBytes, totalBytes), 0, 0);
    try {
      await objectStore.putObject(env.S3_BUCKET, designObjectKey, data, data.byteLength, { "content-type": "text/html; charset=utf-8" });
      stored = await db.transaction(async (tx) => {
        await tx.execute(sql`select id from projects where id = ${projectId} and organization_id = ${actor.organizationId} for update`);
        const current = (await tx.select({ imported: projectImports, revision: projects.revision }).from(projectImports).innerJoin(projects, eq(projects.id, projectImports.projectId)).where(and(eq(projectImports.projectId, projectId), eq(projectImports.organizationId, actor.organizationId))).limit(1))[0];
        if (!current) fail("IMPORT_NOT_FOUND", 404, "Für dieses Projekt liegt kein Import vor.");
        if (current.revision !== row.revision) fail("REVISION_CONFLICT", 409, "Das importierte UI wurde während der Rekonstruktion geändert. Die Analyse wird auf dem aktuellen Stand neu gestartet.", true);
        const manifest = [...current.imported.manifest.filter((file) => !variantPattern.test(file.path)), { path: designPath, size: data.byteLength, mime: "text/html; charset=utf-8" }];
        const revision = current.revision + 1;
        // Die Startseite bleibt die Grundfassung: eine Formatfassung ist eine ZUSAETZLICHE Ansicht,
        // kein Ersatz — sonst waere nach dem Aufbau fuer ein Foldable das Grundformat verschwunden.
        const nextEntryPath = targetViewport ? current.imported.entryPath : designPath;
        await tx.update(projectImports).set({ entryPath: nextEntryPath, manifest, totalBytes: manifest.reduce((sum, file) => sum + file.size, 0), fileCount: manifest.length }).where(eq(projectImports.projectId, projectId));
        await tx.update(projects).set({ revision, updatedAt: new Date() }).where(eq(projects.id, projectId));
        await tx.insert(auditEvents).values({ id: uuidv7(), organizationId: actor.organizationId, actorId: actor.userId, action: "design.reconstructed", targetType: "project", targetId: projectId, result: "success", metadata: { sourceFiles: sources.length, sourceBytes: totalBytes, outputBytes: data.byteLength, revision, platform, profile, measuredValues, screens: screenPlan.length, fidelityScore, openIssues: report.issues.length, elapsedMs: Date.now() - jobStartedAt, retryCount, probes }, correlationId });
        const state = runtimeState(reconstructionState("completed", `Alle ${screenPlan.length} Bildschirme aufgebaut und nachgemessen: ${fidelityScore} % der ${report.checked} geprüften Quellwerte stimmen exakt.`, reconstructionTodos.length, sources.length, sources.length, totalBytes, totalBytes, { entryPath: designPath, revision }), 100, 0);
        const completedJob = await tx.update(jobs).set({ status: "completed", progress: 100, result: state, errorCode: null, heartbeatAt: new Date(), updatedAt: new Date() }).where(and(eq(jobs.id, jobId), eq(jobs.attempts, runAttempt), eq(jobs.status, "running"))).returning({ id: jobs.id });
        if (!completedJob[0]) fail("RECONSTRUCT_LEASE_LOST", 409, "Dieser Rekonstruktionslauf wurde durch einen neueren Versuch ersetzt.");
        return { revision, state };
      });
    } catch (error) {
      let committed = false, commitStatusKnown = false;
      try {
        const latest = (await db.select({ manifest: projectImports.manifest, revision: projects.revision }).from(projectImports).innerJoin(projects, eq(projects.id, projectImports.projectId)).where(and(eq(projectImports.projectId, projectId), eq(projectImports.organizationId, actor.organizationId))).limit(1))[0];
        const latestJob = (await db.select({ status: jobs.status, result: jobs.result }).from(jobs).where(and(eq(jobs.id, jobId), eq(jobs.attempts, runAttempt))).limit(1))[0];
        commitStatusKnown = Boolean(latest && latestJob);
        committed = latest?.manifest.some((file) => file.path === designPath) === true && latestJob?.status === "completed" && Boolean(latestJob.result && typeof latestJob.result === "object");
        if (committed && latest && latestJob?.result && typeof latestJob.result === "object") {
          stored = { revision: latest.revision, state: latestJob.result as ReconstructionState };
          app.log.warn({ err: error, event: "reconstruction.commit_ack_lost", jobId, projectId, runAttempt }, "Rekonstruktion war trotz verlorener Commit-Antwort erfolgreich");
        }
      } catch (statusError) { app.log.warn({ err: statusError, projectId, designPath }, "Commit-Status der Rekonstruktion ist unklar; generiertes Objekt bleibt sicher erhalten"); }
      if (commitStatusKnown && !committed) await objectStore.removeObject(env.S3_BUCKET, designObjectKey);
      if (!committed) throw error;
    }
    if (!stored) fail("RECONSTRUCT_COMMIT_UNKNOWN", 500, "Der Speicherstatus der Rekonstruktion konnte nicht bestätigt werden.", true);
    await Promise.allSettled(oldGeneratedPaths.map((path) => objectStore.removeObject(env.S3_BUCKET, `${row.imported.objectPrefix}${path}`)));
    latestProgress = 100;
    latestState = stored.state;
    app.log.info({ event: "reconstruction.completed", jobId, projectId, elapsedMs: Date.now() - jobStartedAt, retryCount, operations: completedOperations, sourceFiles: sources.length, sourceBytes: totalBytes, outputBytes: data.byteLength }, "Design-Rekonstruktion abgeschlossen");
  } catch (error) {
    const details = error && typeof error === "object" ? error as Record<string, unknown> : {};
    const message = error instanceof Error ? error.message : "Die HTML-Rekonstruktion ist fehlgeschlagen.";
    const code = typeof details.code === "string" ? details.code : error instanceof Error ? error.name : "RECONSTRUCT_FAILED";
    if (code === "RECONSTRUCT_LEASE_LOST") {
      app.log.warn({ event: "reconstruction.lease_lost", jobId, projectId, runAttempt, elapsedMs: Date.now() - jobStartedAt }, "Veralteter Rekonstruktionslauf wurde beendet");
      return;
    }
    const failedState = runtimeState({ ...latestState, phase: "failed", message }, null, null, latestState.currentOperation);
    let failurePersisted = false;
    for (let persistAttempt = 1; persistAttempt <= 3 && !failurePersisted; persistAttempt += 1) {
      try { await publish("failed", Math.min(99, latestProgress), failedState, code); failurePersisted = true; }
      catch (persistError) {
        app.log.error({ err: persistError, jobId, projectId, runAttempt, persistAttempt }, "Fehlerstatus der Rekonstruktion konnte nicht gespeichert werden");
        if (persistAttempt < 3) await new Promise((resolve) => setTimeout(resolve, persistAttempt * 1_000));
      }
    }
    try {
      await db.insert(auditEvents).values({ id: uuidv7(), organizationId: actor.organizationId, actorId: actor.userId, action: "design.reconstruction.failed", targetType: "project", targetId: projectId, result: "failure", metadata: { code, message, elapsedMs: Date.now() - jobStartedAt, retryCount, probes }, correlationId });
    } catch (auditError) { app.log.warn({ err: auditError, jobId, projectId }, "Fehler-Audit der Rekonstruktion konnte nicht gespeichert werden"); }
    const memory = process.memoryUsage();
    app.log.error({ err: error, event: "reconstruction.failed", jobId, projectId, code, progress: latestProgress, elapsedMs: Date.now() - jobStartedAt, retryCount, probes, rssBytes: memory.rss, heapUsedBytes: memory.heapUsed }, "Design-Rekonstruktion fehlgeschlagen");
  }
}
// In-Process-Laeufe koennen einen API-Neustart nicht ueberleben. Persistierte Zwischenzustaende
// werden nach ausbleibenden Heartbeats freigegeben; aktive Parallelprozesse bleiben unangetastet.
const reconstructionHeartbeatTimeoutMs = 15_000;
async function interruptStaleReconstructionJobs() {
  const staleBefore = new Date(Date.now() - reconstructionHeartbeatTimeoutMs);
  const stale = await db.select({ id: jobs.id, result: jobs.result }).from(jobs).where(and(eq(jobs.kind, "design-reconstruction"), sql`${jobs.status} in ('queued', 'running')`, or(isNull(jobs.heartbeatAt), lt(jobs.heartbeatAt, staleBefore))));
  let interrupted = 0;
  for (const row of stale) {
    const existing = reconstructionState("interrupted", "Der Verarbeitungslauf wurde unterbrochen und kann sicher neu gestartet werden.", 0, 0, 0, 0, 0, row.result && typeof row.result === "object" ? row.result as Partial<ReconstructionState> : {});
    const completedAt = new Date().toISOString();
    const state: ReconstructionState = {
      ...existing,
      phase: "interrupted",
      message: "Der Verarbeitungslauf wurde durch einen Serverabbruch unterbrochen und kann sicher neu gestartet werden.",
      phaseProgress: null,
      estimatedRemainingMs: null,
      probes: existing.probes.map((probe) => probe.status === "running" || probe.status === "retrying" ? { ...probe, status: "failed", completedAt, errorCode: "RECONSTRUCT_INTERRUPTED" } : probe)
    };
    const updated = await db.update(jobs).set({ status: "failed", errorCode: "RECONSTRUCT_INTERRUPTED", result: state, heartbeatAt: new Date(), updatedAt: new Date() }).where(and(eq(jobs.id, row.id), sql`${jobs.status} in ('queued', 'running')`, or(isNull(jobs.heartbeatAt), lt(jobs.heartbeatAt, staleBefore)))).returning({ id: jobs.id });
    interrupted += updated.length;
  }
  if (interrupted) app.log.warn({ event: "reconstruction.stale_interrupted", interrupted, staleBefore }, "Verwaiste Rekonstruktionsläufe wurden freigegeben");
}
await interruptStaleReconstructionJobs();
const reconstructionReaper = setInterval(() => { void interruptStaleReconstructionJobs().catch((error) => app.log.error({ err: error }, "Stale-Job-Reaper fehlgeschlagen")); }, 10_000);
reconstructionReaper.unref();
app.post("/api/v1/projects/:projectId/design/reconstruct", { config: { rateLimit: { max: 6, timeWindow: "10 minutes" } } }, async (request, reply) => {
  const actor = requireActorPermission(request, "design.edit");
  const { projectId } = z.object({ projectId: z.string().uuid() }).parse(request.params);
  // Das Zielformat entscheidet, FUER WELCHE Flaeche gebaut wird. Ohne Angabe entsteht die
  // Grundfassung in der Groesse, die das Projekt selbst nennt.
  const { retryFailed, force, viewport } = z.object({
    retryFailed: z.boolean().optional().default(false),
    force: z.boolean().optional().default(false),
    viewport: z.object({ width: z.number().int().min(240).max(4096), height: z.number().int().min(240).max(4096), device: z.string().min(1).max(80) }).optional()
  }).strict().parse(request.body ?? {});
  const imported = (await db.select({ revision: projects.revision }).from(projectImports).innerJoin(projects, eq(projects.id, projectImports.projectId)).where(and(eq(projectImports.projectId, projectId), eq(projectImports.organizationId, actor.organizationId))).limit(1))[0];
  if (!imported) fail("IMPORT_NOT_FOUND", 404, "Für dieses Projekt liegt kein Import vor.");
  // Das Format gehoert in den Idempotenzschluessel: sonst gaebe der Aufbau fuer ein zweites Format
  // den Lauf des ersten zurueck, und die zweite Fassung entstuende nie.
  const idempotencyKey = `${projectId}:${imported.revision}:${viewport ? `${viewport.width}x${viewport.height}` : "basis"}`;
  const queuedState = reconstructionState("queued", "HTML-Rekonstruktion wird vorbereitet.", 0, 0, 0, 0, 0);
  const candidateId = uuidv7();
  const inserted = await db.insert(jobs).values({ id: candidateId, organizationId: actor.organizationId, projectId, kind: "design-reconstruction", status: "queued", progress: 0, idempotencyKey, input: { projectId, revision: imported.revision, ...(viewport ? { viewport } : {}) }, result: queuedState, attempts: 1, heartbeatAt: new Date() }).onConflictDoNothing().returning({ id: jobs.id });
  if (inserted[0]) {
    setTimeout(() => { void runReconstructionJob(candidateId, 1, actor, projectId, request.id, [], viewport).catch((error) => app.log.error({ err: error, jobId: candidateId, projectId }, "Unbehandelter Rekonstruktionsfehler")); }, 0);
    return reply.status(202).send({ jobId: candidateId, status: "queued" });
  }
  const existing = (await db.select().from(jobs).where(and(eq(jobs.organizationId, actor.organizationId), eq(jobs.kind, "design-reconstruction"), eq(jobs.idempotencyKey, idempotencyKey))).limit(1))[0];
  if (!existing) fail("JOB_CLAIM_FAILED", 409, "Der Verarbeitungslauf konnte nicht übernommen werden. Bitte erneut versuchen.", true);
  if (existing.status === "queued" || existing.status === "running") return reply.status(202).send({ jobId: existing.id, status: existing.status });
  if (!canRestartReconstructionJob(existing.status, retryFailed, force)) return reply.status(202).send({ jobId: existing.id, status: existing.status });
  const existingResult = existing.result && typeof existing.result === "object" ? existing.result as Partial<ReconstructionState> : null;
  const previousProbes = Array.isArray(existingResult?.probes) ? existingResult.probes : [];
  const retryQueuedState = { ...queuedState, probes: previousProbes.map((probe) => ({ ...probe })) };
  const claimed = await db.update(jobs).set({ status: "queued", progress: 0, result: retryQueuedState, errorCode: null, attempts: sql`${jobs.attempts} + 1`, heartbeatAt: new Date(), updatedAt: new Date() }).where(and(eq(jobs.id, existing.id), eq(jobs.status, existing.status))).returning({ id: jobs.id, attempts: jobs.attempts });
  if (claimed[0]) setTimeout(() => { void runReconstructionJob(existing.id, claimed[0]!.attempts, actor, projectId, request.id, previousProbes, viewport).catch((error) => app.log.error({ err: error, jobId: existing.id, projectId }, "Unbehandelter Rekonstruktionsfehler")); }, 0);
  return reply.status(202).send({ jobId: existing.id, status: claimed[0] ? "queued" : "running" });
});
app.get("/api/v1/jobs/:jobId", async (request) => {
  const actor = requireActorPermission(request, "project.read");
  const { jobId } = z.object({ jobId: z.string().uuid() }).parse(request.params);
  const row = (await db.select({ id: jobs.id, projectId: jobs.projectId, kind: jobs.kind, status: jobs.status, progress: jobs.progress, result: jobs.result, errorCode: jobs.errorCode, attempts: jobs.attempts, heartbeatAt: jobs.heartbeatAt, updatedAt: jobs.updatedAt }).from(jobs).where(and(eq(jobs.id, jobId), eq(jobs.organizationId, actor.organizationId))).limit(1))[0];
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
const shutdown = async () => { clearInterval(reconstructionReaper); await app.close(); await client.end(); };
process.on("SIGINT", shutdown); process.on("SIGTERM", shutdown);
try { await cleanupOrphanImportObjects(); }
catch (error) { app.log.error({ err: error, event: "storage.orphan_cleanup_failed" }, "Verwaiste Importobjekte konnten nicht bereinigt werden; API startet ohne Datenverlust weiter"); }
await app.listen({ port: env.API_PORT, host: "0.0.0.0" });

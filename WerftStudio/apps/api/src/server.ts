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
import { auditEvents, createDatabase, designOperations, designSnapshots, drafts, jobs, memberships, organizations, outboxEvents, projectImports, projects, providerConnections, userPreferences, users, versions } from "@werft/database";
import { applyDesignOperations, validateDesignReferences } from "@werft/design-model";
import { and, desc, eq, isNull, lt, notInArray, or, sql } from "drizzle-orm";
import Fastify, { type FastifyRequest } from "fastify";
import { Client as MinioClient } from "minio";
import { designFileName, designVariantsOf, generatedDesignPathPattern, sameVariantPattern } from "./design-variants.js";
import { v7 as uuidv7 } from "uuid";
import { z } from "zod";
import { codexAuth, codexEfforts, codexModels, codexRequestFields, decryptCredentials, encryptCredentials, tokenIdentity } from "./codex-auth.js";
import { codexHttpError, isRetryableCodexError, parseCodexEventStream } from "./codex-stream.js";
import { assertOpenRouterContext, normalizeOpenRouterModelDetails, openRouterApi, openRouterHttpError, openRouterRequest, parseOpenRouterEventStream, type OpenRouterEndpoint, type OpenRouterModel, type OpenRouterModelDetails, type OpenRouterStreamResult } from "./openrouter.js";
import { assertZenContext, normalizeZenFreeModels, parseZenEventStream, withVerifiedZenReasoning, zenApi, zenHttpError, zenRequest, type ZenModel } from "./opencode-zen.js";
import { applyChatEdits, parseChatResponse, repairBriefing, verifyFileWrite, type ChatEdit, type FileReport, type ParsedChatResponse } from "./chat-edit.js";
import { designMap, designScreens, excerptDesign, globalScope, inputCharBudget, reservedOutputTokens, selectChatFiles } from "./chat-scope.js";
import { designBriefing, summariseEffect } from "./css-effect.js";
import { extractDesignFacts, factCandidatePaths, orderedScreens } from "./design-extract.js";
import { factCount, renderAssetLibrary, renderFactSheet } from "./design-facts.js";
import { effectGuidance } from "./effect-catalog.js";
import { buildExportPackage, type PackageReport } from "./export-package.js";
import type { Vorlage } from "./spec-package.js";
import { checkFidelity, fidelityAcceptable, hasIssuesForSources, renderFidelityInstructions } from "./fidelity-check.js";
import { analysisBudget, buildSourceBatches, canRestartReconstructionJob, estimateAnalysisCallCount, mapWithConcurrency, maxAnalysisBatches, previewProfileFromHtml, previewProfiles, reconstructionConcurrency, reconstructionSourceFiles, reconstructionTiming, specSourceFile, reconstructionTodos, type ImportManifestFile, type ImportPlatform, type PreviewProfile, type ReconstructionOperationKind, type ReconstructionTimingSample } from "./import-reconstruction.js";
import { isOverloadError, maxModelAttempts, retryDelayMs, UpstreamThrottle } from "./model-retry.js";
import { analysisKey, checkpointIsUseful, checkpointPrefix, checkpointRoot, checkpointSummary, describeCheckpoint, emptyCheckpoint, evidenceKey, mergeCheckpointPart, parseCheckpointObject, resumableAnalyses, resumableScreens, screenKey, type CheckpointParts, type CheckpointScope, type CheckpointSummary } from "./reconstruction-checkpoint.js";
import { composeScreens, extractScreenFragment, screenPlanFrom, themeStyles, themeVariants } from "./screen-composer.js";
import { checkShellConsistency, renderShellInstructions, shellReference, type ShellIssue } from "./shell-consistency.js";
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
const app = Fastify({ logger: { redact: ["req.headers.authorization", "req.headers.cookie", "body.password", "body.credential", "body.apiKey"] }, genReqId: () => uuidv7(), bodyLimit: 32 * 1024 * 1024 });
const s3Endpoint = new URL(env.S3_ENDPOINT);
const objectStore = new MinioClient({ endPoint: s3Endpoint.hostname, port: Number(s3Endpoint.port || (s3Endpoint.protocol === "https:" ? 443 : 80)), useSSL: s3Endpoint.protocol === "https:", accessKey: env.S3_ACCESS_KEY, secretKey: env.S3_SECRET_KEY });
let bucketReady: Promise<void> | undefined;
const pendingCodexAuth = new Map<string, PendingCodexAuth>();
let activeHostCacheCleanup: Promise<Awaited<ReturnType<typeof requestHostCacheCleanup>>> | undefined;
// Gemeinsame Drossel ALLER KI-Laeufe dieses Prozesses. Sie deckelt normalerweise auf die geplante
// Nebenlaeufigkeit und faellt bei Ueberlastmeldungen des Anbieters selbsttaetig auf zwei zurueck.
const modelThrottle = new UpstreamThrottle(reconstructionConcurrency);

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
    minimumAgeMs: manual ? 24 * 60 * 60 * 1000 : 0,
    protectedPrefixes: [checkpointRoot]
  });
}
// Zwischenstaende eines Rekonstruktionslaufs. Sie liegen bewusst NEBEN dem Import (eigener
// Praefix, nicht im Manifest): der Benutzer soll sie nie als Projektdatei sehen, der Lauf aber
// jederzeit darauf aufsetzen koennen. Ein Fehler beim Sichern darf einen laufenden Aufbau NIEMALS
// abbrechen — der Zwischenstand ist Komfort, das Ergebnis ist Pflicht.
const saveCheckpointPart = async (key: string, body: string) => {
  try {
    await ensureBucket();
    const data = Buffer.from(body, "utf8");
    await objectStore.putObject(env.S3_BUCKET, key, data, data.byteLength, { "content-type": key.endsWith(".json") ? "application/json; charset=utf-8" : "text/plain; charset=utf-8" });
    return true;
  } catch (error) {
    app.log.warn({ err: error, event: "reconstruction.checkpoint.save_failed", key }, "Zwischenstand der Rekonstruktion konnte nicht gesichert werden");
    return false;
  }
};
const loadCheckpointParts = async (prefix: string): Promise<CheckpointParts> => {
  let parts = emptyCheckpoint;
  try {
    await ensureBucket();
    const keys: string[] = [];
    for await (const object of objectStore.listObjectsV2(env.S3_BUCKET, prefix, true)) if (object.name) keys.push(object.name);
    for (const key of keys) {
      try { parts = mergeCheckpointPart(parts, parseCheckpointObject(key, prefix, (await readObject(key)).toString("utf8"))); }
      catch (error) { app.log.warn({ err: error, event: "reconstruction.checkpoint.part_unreadable", key }, "Ein Teil des Zwischenstands war nicht lesbar und wird neu berechnet"); }
    }
  } catch (error) {
    app.log.warn({ err: error, event: "reconstruction.checkpoint.load_failed", prefix }, "Zwischenstand der Rekonstruktion konnte nicht gelesen werden");
  }
  return parts;
};
const removeCheckpoint = async (prefix: string) => {
  try {
    const keys: string[] = [];
    for await (const object of objectStore.listObjectsV2(env.S3_BUCKET, prefix, true)) if (object.name) keys.push(object.name);
    await Promise.allSettled(keys.map((key) => objectStore.removeObject(env.S3_BUCKET, key)));
  } catch (error) {
    app.log.warn({ err: error, event: "reconstruction.checkpoint.cleanup_failed", prefix }, "Zwischenstand der Rekonstruktion konnte nicht aufgeräumt werden");
  }
};
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
type ProviderId = "openai-codex" | "openrouter" | "opencode-zen";
type ModelSelection = { provider: ProviderId; model: string; effort?: string };
type ProviderModel = OpenRouterModel | ZenModel | { provider: "openai-codex"; id: typeof codexModels[number]; name: string; efforts: string[]; defaultEffort: string };
const providerIds = ["openai-codex", "openrouter", "opencode-zen"] as const;
const providerDisplayName = (provider: ProviderId) => provider === "openai-codex" ? "OpenAI" : provider === "openrouter" ? "OpenRouter" : "OpenCode Zen";
const openRouterCredentialsSchema = z.object({ apiKey: z.string().min(10).max(500) }).strict();
const zenCredentialsSchema = z.object({ apiKey: z.literal("public") }).strict();
const modelSelectionSchema = z.object({ provider: z.enum(providerIds), model: z.string().min(1).max(300), effort: z.string().min(1).max(20).optional() }).strict();
const openRouterModelIdSchema = z.string().trim().min(3).max(300).regex(/^[^\s/]+\/[^\s/]+$/, "Füge die kopierte OpenRouter-Modell-ID im Format anbieter/modell ein.");
const openRouterEndpointSchema = z.object({
  providerName: z.string().min(1).max(200), providerSlug: z.string().min(1).max(200), tag: z.string().max(400), endpointId: z.string().max(400),
  promptPerToken: z.number(), completionPerToken: z.number(), cacheReadPerToken: z.number(), contextLength: z.number().int().nonnegative(),
  maxCompletionTokens: z.number().int().nonnegative().optional(), quantization: z.string().max(100), throughputLast30m: z.number().optional(), uptimeLast5m: z.number().optional(), status: z.number().int()
}).strict();
const storedOpenRouterModelSchema = z.object({ provider: z.literal("openrouter"), id: openRouterModelIdSchema, name: z.string().min(1).max(300), contextLength: z.number().int().positive().optional(), efforts: z.array(z.string().min(1).max(20)).max(20), defaultEffort: z.string().min(1).max(20).optional(), endpoint: openRouterEndpointSchema }).strict();
const storedZenModelSchema = z.object({ provider: z.literal("opencode-zen"), id: z.string().min(1).max(300), name: z.string().min(1).max(300), contextLength: z.number().int().positive().optional(), inputTokenLimit: z.number().int().positive().optional(), maxOutputTokens: z.number().int().positive().optional(), efforts: z.array(z.string().min(1).max(20)).max(20), reasoning: z.boolean(), reasoningControl: z.enum(["toggle", "effort"]).optional() }).strict();
const openRouterEndpointSelectionSchema = z.object({ model: openRouterModelIdSchema, endpointId: z.string().max(400).optional(), providerTag: z.string().max(400).optional(), providerSlug: z.string().max(200).optional() }).strict().refine((value) => value.endpointId || value.providerTag || value.providerSlug, { message: "Wähle einen Provider für das Modell aus." });
const codexModelNames: Record<typeof codexModels[number], string> = { "gpt-5.6-sol": "GPT-5.6 Sol", "gpt-5.6-terra": "GPT-5.6 Terra", "gpt-5.6-luna": "GPT-5.6 Luna" };
const codexProviderModels = (): ProviderModel[] => codexModels.map((id) => ({ provider: "openai-codex", id, name: codexModelNames[id], efforts: [...codexEfforts], defaultEffort: "medium" }));

function openRouterSettings(value: unknown): { model?: string; effort?: string; models: OpenRouterModel[] } {
  const raw = value && typeof value === "object" && !Array.isArray(value) ? value as Record<string, unknown> : {};
  const parsed = z.array(storedOpenRouterModelSchema).safeParse(raw.models);
  return {
    ...(typeof raw.model === "string" ? { model: raw.model } : {}),
    ...(typeof raw.effort === "string" ? { effort: raw.effort } : {}),
    models: parsed.success ? parsed.data as unknown as OpenRouterModel[] : []
  };
}

function zenSettings(value: unknown): { model?: string; effort?: string; models: ZenModel[] } {
  const raw = value && typeof value === "object" && !Array.isArray(value) ? value as Record<string, unknown> : {};
  const parsed = z.array(storedZenModelSchema).safeParse(raw.models);
  return {
    ...(typeof raw.model === "string" ? { model: raw.model } : {}),
    ...(typeof raw.effort === "string" ? { effort: raw.effort } : {}),
    models: parsed.success ? (parsed.data as ZenModel[]).map(withVerifiedZenReasoning) : []
  };
}

async function validOpenRouterConnection(organizationId: string) {
  const row = (await db.select().from(providerConnections).where(and(eq(providerConnections.organizationId, organizationId), eq(providerConnections.provider, "openrouter"))).limit(1))[0];
  if (!row) fail("OPENROUTER_NOT_CONNECTED", 409, "Bitte zuerst OpenRouter verbinden.");
  const credentials = openRouterCredentialsSchema.parse(decryptCredentials(row.credentials, env.SESSION_SECRET));
  return { ...row, credentials };
}
async function validZenConnection(organizationId: string) {
  const row = (await db.select().from(providerConnections).where(and(eq(providerConnections.organizationId, organizationId), eq(providerConnections.provider, "opencode-zen"))).limit(1))[0];
  if (!row) fail("ZEN_NOT_CONNECTED", 409, "Bitte zuerst OpenCode Zen Free aktivieren.");
  const credentials = zenCredentialsSchema.parse(decryptCredentials(row.credentials, env.SESSION_SECRET));
  return { ...row, credentials };
}
async function zenFetch(url: string, apiKey: string | undefined, init: RequestInit = {}) {
  try {
    return await fetch(url, { ...init, signal: init.signal ?? AbortSignal.timeout(30_000), headers: { ...(apiKey ? { authorization: `Bearer ${apiKey}` } : {}), ...init.headers } });
  } catch (error) {
    if (init.signal?.aborted) throw init.signal.reason ?? error;
    app.log.warn({ err: error, url }, "OpenCode Zen nicht erreichbar");
    fail("ZEN_UNREACHABLE", 502, "OpenCode Zen ist vom Server aus nicht erreichbar.", true);
  }
}
let zenCatalogCache: { models: ZenModel[]; expiresAt: number } | undefined;
let zenCatalogLoading: Promise<ZenModel[]> | undefined;
async function fetchZenFreeModels(force = false): Promise<ZenModel[]> {
  if (!force && zenCatalogCache && zenCatalogCache.expiresAt > Date.now()) return zenCatalogCache.models;
  if (zenCatalogLoading) return zenCatalogLoading;
  zenCatalogLoading = (async () => {
    const [availableResponse, metadataResponse] = await Promise.all([zenFetch(zenApi.modelsUrl, "public"), zenFetch(zenApi.metadataUrl, undefined)]);
    const availableRaw = await availableResponse.text(), metadataRaw = await metadataResponse.text();
    if (!availableResponse.ok) throw zenHttpError(availableResponse.status, availableResponse.headers.get("retry-after"), availableRaw);
    if (!metadataResponse.ok) fail("ZEN_METADATA_UNAVAILABLE", 502, "Die OpenCode-Zen-Modellinformationen sind gerade nicht verfügbar.", true);
    const models = normalizeZenFreeModels(JSON.parse(availableRaw), JSON.parse(metadataRaw));
    zenCatalogCache = { models, expiresAt: Date.now() + 5 * 60_000 };
    return models;
  })();
  try { return await zenCatalogLoading; }
  finally { zenCatalogLoading = undefined; }
}
async function zenModelsForConnection(connection: { settings: unknown }, force = false): Promise<ZenModel[]> {
  try { return await fetchZenFreeModels(force); }
  catch (error) {
    const stored = zenSettings(connection.settings).models;
    const details = error && typeof error === "object" ? error as { retryable?: unknown } : {};
    if (!force && details.retryable !== false && stored.length) return stored;
    throw error;
  }
}
function defaultFallback(rows: Array<{ id: string; provider: string; settings: unknown }>) {
  const usable = rows.filter((row) => row.provider === "openai-codex" || row.provider === "opencode-zen" && zenSettings(row.settings).models.length > 0 || row.provider === "openrouter" && openRouterSettings(row.settings).models.length > 0);
  return ["openai-codex", "opencode-zen", "openrouter"].map((provider) => usable.find((row) => row.provider === provider)).find(Boolean);
}
async function openRouterFetch(url: string, apiKey: string, init: RequestInit = {}) {
  try {
    return await fetch(url, { ...init, signal: init.signal ?? AbortSignal.timeout(30_000), headers: { authorization: `Bearer ${apiKey}`, "HTTP-Referer": env.WEB_ORIGIN, "X-OpenRouter-Title": "Werft Studio", ...init.headers } });
  } catch (error) {
    if (init.signal?.aborted) throw init.signal.reason ?? error;
    app.log.warn({ err: error, url }, "OpenRouter nicht erreichbar");
    fail("OPENROUTER_UNREACHABLE", 502, "OpenRouter ist vom Server aus nicht erreichbar.", true);
  }
}
async function openRouterKeyInfo(apiKey: string) {
  const response = await openRouterFetch(openRouterApi.keyUrl, apiKey);
  if (!response.ok) throw openRouterHttpError(response.status);
  return await response.json() as { data?: { label?: string; limit_remaining?: number | null; is_free_tier?: boolean } };
}
async function openRouterModelDetails(apiKey: string, modelId: string): Promise<OpenRouterModelDetails> {
  const path = modelId.split("/").map(encodeURIComponent).join("/");
  const response = await openRouterFetch(`${openRouterApi.modelsUrl}/${path}/endpoints`, apiKey);
  if (response.status === 404) fail("OPENROUTER_MODEL_NOT_FOUND", 404, "OpenRouter kennt diese Modell-ID nicht. Kopiere die ID erneut von der Modellseite.");
  if (!response.ok) throw openRouterHttpError(response.status);
  const details = normalizeOpenRouterModelDetails(await response.json(), modelId);
  if (!details) fail("OPENROUTER_MODEL_INVALID", 400, "Dieses OpenRouter-Modell hat keine verwendbaren Text-Provider.");
  await enrichOpenRouterThroughput(apiKey, path, details);
  return details;
}

async function enrichOpenRouterThroughput(apiKey: string, modelPath: string, details: OpenRouterModelDetails): Promise<void> {
  if (details.endpoints.every((endpoint) => endpoint.throughputLast30m !== undefined)) return;
  try {
    const page = await openRouterFetch(`https://openrouter.ai/${modelPath}/providers`, apiKey);
    if (!page.ok) return;
    const html = await page.text(), tagsByEndpoint = new Map(details.endpoints.filter((endpoint) => endpoint.endpointId).map((endpoint) => [endpoint.endpointId, endpoint.tag]));
    const endpointPatterns = [
      /id\\":\\"([0-9a-f-]{36})\\",\\"name\\":\\"[^\\"]+\\"[\s\S]{0,40000}?provider_slug\\":\\"([^\\"]+)\\"/gi,
      /"id":"([0-9a-f-]{36})","name":"[^"]+"[\s\S]{0,40000}?"provider_slug":"([^"]+)"/gi
    ];
    for (const pattern of endpointPatterns) for (const match of html.matchAll(pattern)) tagsByEndpoint.set(match[1]!, match[2]!);
    let chart: unknown;
    for (const suffix of ["/v1/stats/throughput-comparison", "/stats/throughput-comparison"]) {
      const response = await openRouterFetch(`${openRouterApi.frontendUrl}${suffix}?permaslug=${encodeURIComponent(details.permaslug)}`, apiKey);
      if (!response.ok) continue;
      chart = await response.json();
      break;
    }
    const rows = chart && typeof chart === "object" && Array.isArray((chart as { data?: unknown }).data) ? (chart as { data: unknown[] }).data : [];
    const latest = new Map<string, number>();
    for (let index = rows.length - 1; index >= 0; index -= 1) {
      const row = rows[index], values = row && typeof row === "object" && (row as { y?: unknown }).y && typeof (row as { y: unknown }).y === "object" ? (row as { y: Record<string, unknown> }).y : undefined;
      if (!values) continue;
      for (const [metric, raw] of Object.entries(values)) {
        const endpointId = metric.split("::", 1)[0]!, value = typeof raw === "number" ? raw : Number(raw);
        if (!latest.has(endpointId) && Number.isFinite(value)) latest.set(endpointId, value);
      }
    }
    const byTag = new Map<string, number>();
    for (const [endpointId, throughput] of latest) { const tag = tagsByEndpoint.get(endpointId); if (tag) byTag.set(tag, throughput); }
    for (const endpoint of details.endpoints) {
      if (endpoint.throughputLast30m !== undefined) continue;
      const throughput = latest.get(endpoint.endpointId) ?? byTag.get(endpoint.tag);
      if (throughput !== undefined) endpoint.throughputLast30m = throughput;
    }
  } catch (error) {
    app.log.warn({ err: error, model: details.model.id }, "OpenRouter-Throughput konnte nicht ergänzt werden");
  }
}
async function providerCatalog(organizationId: string) {
  const rows = await db.select().from(providerConnections).where(and(eq(providerConnections.organizationId, organizationId), eq(providerConnections.status, "connected")));
  const models: ProviderModel[] = [];
  const codex = rows.find((row) => row.provider === "openai-codex");
  const router = rows.find((row) => row.provider === "openrouter");
  const zen = rows.find((row) => row.provider === "opencode-zen");
  if (codex) models.push(...codexProviderModels());
  const routerModels = router ? openRouterSettings(router.settings).models : [];
  models.push(...routerModels);
  let zenModels: ZenModel[] = [];
  let zenError: string | undefined;
  if (zen) {
    try { zenModels = await fetchZenFreeModels(); }
    catch (error) {
      const details = error && typeof error === "object" ? error as { retryable?: unknown } : {};
      if (details.retryable === false) throw error;
      zenModels = zenSettings(zen.settings).models;
      zenError = "Die aktuelle Zen-Modellliste konnte nicht geladen werden. Angezeigt wird der zuletzt geladene Stand.";
    }
    models.push(...zenModels);
  }
  const selected = rows.find((row) => row.isDefault) ?? codex ?? zen ?? router;
  const settings = selected?.settings && typeof selected.settings === "object" ? selected.settings : {};
  const selectedZenModel = selected?.provider === "opencode-zen" && typeof settings.model === "string" ? zenModels.find((model) => model.id === settings.model) : undefined;
  const selection = selected?.provider === "openai-codex"
    ? { provider: "openai-codex" as const, model: codexModels.includes(settings.model as typeof codexModels[number]) ? settings.model! : "gpt-5.6-sol", effort: codexEfforts.includes(settings.effort as typeof codexEfforts[number]) ? settings.effort! : "medium" }
    : selected?.provider === "openrouter" && typeof settings.model === "string" && routerModels.some((model) => model.id === settings.model) ? { provider: "openrouter" as const, model: settings.model, ...(typeof settings.effort === "string" ? { effort: settings.effort } : {}) }
      : selectedZenModel ? { provider: "opencode-zen" as const, model: selectedZenModel.id, ...(typeof settings.effort === "string" && selectedZenModel.efforts.includes(settings.effort) ? { effort: settings.effort } : {}) } : undefined;
  const selectionError = selected?.provider === "openrouter" && typeof settings.model === "string" && routerModels.length && !routerModels.some((model) => model.id === settings.model)
    ? "Das bisherige OpenRouter-Standardmodell ist nicht mehr verfügbar. Bitte wähle ein neues Modell."
    : selected?.provider === "opencode-zen" && (!zenModels.length || typeof settings.model === "string" && !selectedZenModel) ? "Das bisherige OpenCode-Zen-Standardmodell ist nicht mehr kostenlos verfügbar. Bitte wähle ein neues Modell." : undefined;
  return { models, selection, ...(zenError ? { zenError } : {}), ...(selectionError ? { selectionError } : {}) };
}
async function resolveModelSelection(organizationId: string, requested?: ModelSelection): Promise<ModelSelection> {
  if (requested) {
    if (requested.provider === "openai-codex" && !codexModels.includes(requested.model as typeof codexModels[number])) fail("MODEL_INVALID", 400, "Dieses Codex-Modell wird nicht unterstützt.");
    if (requested.provider === "openai-codex" && requested.effort && !codexEfforts.includes(requested.effort as typeof codexEfforts[number])) fail("EFFORT_INVALID", 400, "Dieser Effort wird nicht unterstützt.");
    if (requested.provider === "openrouter") {
      const connection = await validOpenRouterConnection(organizationId);
      const model = openRouterSettings(connection.settings).models.find((entry) => entry.id === requested.model);
      if (!model) fail("MODEL_NOT_AVAILABLE", 409, "Das gewählte OpenRouter-Modell wurde nicht in den Einstellungen hinzugefügt.");
      if (requested.effort && !model.efforts.includes(requested.effort)) fail("EFFORT_INVALID", 400, "Der gewählte Effort wird von diesem OpenRouter-Modell nicht unterstützt.");
    } else if (requested.provider === "opencode-zen") {
      const connection = await validZenConnection(organizationId);
      const model = (await zenModelsForConnection(connection)).find((entry) => entry.id === requested.model);
      if (!model) fail("MODEL_NOT_AVAILABLE", 409, "Das gewählte Modell ist bei OpenCode Zen nicht mehr kostenlos verfügbar.");
      if (requested.effort && !model.efforts.includes(requested.effort)) fail("EFFORT_INVALID", 400, "Der gewählte Effort wird von diesem OpenCode-Zen-Modell nicht unterstützt.");
    } else {
      const connected = (await db.select({ id: providerConnections.id }).from(providerConnections).where(and(eq(providerConnections.organizationId, organizationId), eq(providerConnections.provider, requested.provider), eq(providerConnections.status, "connected"))).limit(1))[0];
      if (!connected) fail("PROVIDER_NOT_CONNECTED", 409, "Der gewählte Modellprovider ist nicht verbunden.");
    }
    return requested;
  }
  const rows = await db.select().from(providerConnections).where(and(eq(providerConnections.organizationId, organizationId), eq(providerConnections.status, "connected")));
  const row = rows.find((entry) => entry.isDefault) ?? rows.find((entry) => entry.provider === "openai-codex") ?? rows[0];
  if (!row) fail("PROVIDER_NOT_CONNECTED", 409, "Bitte zuerst einen Modellprovider verbinden.");
  const settings = row.settings && typeof row.settings === "object" ? row.settings : {};
  if (row.provider === "openai-codex") return { provider: "openai-codex", model: codexModels.includes(settings.model as typeof codexModels[number]) ? settings.model! : "gpt-5.6-sol", effort: codexEfforts.includes(settings.effort as typeof codexEfforts[number]) ? settings.effort! : "medium" };
  if (row.provider === "openrouter" && typeof settings.model === "string") {
    const connection = await validOpenRouterConnection(organizationId);
    const model = openRouterSettings(connection.settings).models.find((entry) => entry.id === settings.model);
    if (!model) fail("MODEL_NOT_AVAILABLE", 409, "Das OpenRouter-Standardmodell wurde nicht in den Einstellungen hinzugefügt.");
    const effort = typeof settings.effort === "string" && model.efforts.includes(settings.effort) ? settings.effort : model.defaultEffort ?? model.efforts[0];
    return { provider: "openrouter", model: model.id, ...(effort ? { effort } : {}) };
  }
  if (row.provider === "opencode-zen" && typeof settings.model === "string") {
    const connection = await validZenConnection(organizationId);
    const model = (await zenModelsForConnection(connection)).find((entry) => entry.id === settings.model);
    if (!model) fail("MODEL_NOT_AVAILABLE", 409, "Das OpenCode-Zen-Standardmodell ist nicht mehr kostenlos verfügbar.");
    const effort = typeof settings.effort === "string" && model.efforts.includes(settings.effort) ? settings.effort : undefined;
    return { provider: "opencode-zen", model: model.id, ...(effort ? { effort } : {}) };
  }
  fail("MODEL_NOT_SELECTED", 409, "Bitte zuerst ein Standardmodell auswählen.");
}
function fail(code: string, statusCode: number, message: string, retryable = false): never { throw Object.assign(new Error(message), { code, statusCode, retryable }); }

app.setErrorHandler((error, request, reply) => {
  const details = error && typeof error === "object" ? error as Record<string, unknown> : {};
  const normalized = error instanceof Error ? error : new Error("Unbekannter Serverfehler");
  const statusCode = typeof details.statusCode === "number" ? details.statusCode : error instanceof z.ZodError ? 400 : 500;
  const code = typeof details.code === "string" ? details.code : error instanceof z.ZodError ? "VALIDATION_FAILED" : "INTERNAL_ERROR";
  request.log.error({ err: error, code }, "request failed");
  // `expose` kennzeichnet Meldungen, die nichts Internes verraten, aber alles sagen, was der Benutzer
  // wissen muss — etwa welcher Modellanbieter abgebrochen hat. Ohne diese Ausnahme landete jeder
  // Anbieterausfall als nichtssagendes „Die Anfrage konnte nicht abgeschlossen werden." beim Benutzer.
  const message = statusCode < 500 || details.expose === true ? normalized.message : "Die Anfrage konnte nicht abgeschlossen werden.";
  return reply.status(statusCode).send({ code, message, retryable: details.retryable === true, correlationId: request.id });
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

app.get("/api/v1/health/live", async () => ({ status: "ok", version: "0.36.0-20260814.1048" }));
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

app.get("/api/v1/providers/openai", async (request) => { const actor = requireActorPermission(request, "provider.read"); const row = (await db.select({ status: providerConnections.status, email: providerConnections.email, accountId: providerConnections.accountId, expiresAt: providerConnections.expiresAt, settings: providerConnections.settings, isDefault: providerConnections.isDefault }).from(providerConnections).where(and(eq(providerConnections.organizationId, actor.organizationId), eq(providerConnections.provider, "openai-codex"))).limit(1))[0]; return row ? { connected: row.status === "connected", ...row } : { connected: false, status: "disconnected", settings: {}, isDefault: false }; });
app.patch("/api/v1/providers/openai/settings", async (request) => { const actor = requireActorPermission(request, "provider.manage"); const settings = z.object({ model: z.enum(codexModels), effort: z.enum(codexEfforts) }).strict().parse(request.body); const updated = await db.update(providerConnections).set({ settings, updatedAt: new Date() }).where(and(eq(providerConnections.organizationId, actor.organizationId), eq(providerConnections.provider, "openai-codex"))).returning({ id: providerConnections.id }); if (!updated[0]) fail("OPENAI_NOT_CONNECTED", 409, "Bitte zuerst OpenAI verbinden."); return { settings }; });
app.post("/api/v1/providers/openai/test", { config: { rateLimit: { max: 5, timeWindow: "5 minutes" } } }, async (request) => {
  const actor = requireActorPermission(request, "provider.use"), connection = await validCodexConnection(actor.organizationId);
  const settings = z.object({ model: z.enum(codexModels).default("gpt-5.6-sol"), effort: z.enum(codexEfforts).default("medium") }).parse(connection.settings);
  const accountId = connection.accountId || tokenIdentity(connection.credentials.accessToken, connection.credentials.idToken).accountId;
  if (!accountId) fail("OPENAI_ACCOUNT_MISSING", 401, "Im OpenAI-Token fehlt die ChatGPT-Account-ID. Bitte erneut verbinden.");
  const body: Record<string, unknown> = { ...codexRequestFields(settings.model, settings.effort), instructions: "Antworte ausschließlich mit OK.", input: [{ role: "user", content: "Verbindungstest" }], store: false, stream: true };
  const started = Date.now();
  const response = await fetch(codexAuth.responsesUrl, { method: "POST", signal: AbortSignal.timeout(120_000), headers: { authorization: `Bearer ${connection.credentials.accessToken}`, "chatgpt-account-id": accountId, originator: "codex_cli_rs", "user-agent": "codex_cli_rs/0.0.0 (Werft Studio)", accept: "text/event-stream", "content-type": "application/json" }, body: JSON.stringify(body) });
  const responseText = await response.text();
  if (!response.ok) fail("OPENAI_TEST_FAILED", 502, `OpenAI hat den Verbindungstest abgelehnt (HTTP ${response.status}).`);
  if (!responseText.includes("response.completed")) fail("OPENAI_TEST_INCOMPLETE", 502, "OpenAI hat den Verbindungstest nicht vollständig abgeschlossen.", true);
  return { ok: true, model: settings.model, effort: settings.effort, elapsedMs: Date.now() - started };
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
  await db.transaction(async (tx) => { await tx.execute(sql`select id from organizations where id = ${actor.organizationId} for update`); const hasDefault = (await tx.select({ id: providerConnections.id }).from(providerConnections).where(and(eq(providerConnections.organizationId, actor.organizationId), eq(providerConnections.isDefault, true))).limit(1))[0]; await tx.insert(providerConnections).values({ id: uuidv7(), organizationId: actor.organizationId, provider: "openai-codex", credentials, accountId: identity.accountId, email: identity.email, expiresAt, settings: { model: "gpt-5.6-sol", effort: "medium" }, isDefault: !hasDefault }).onConflictDoUpdate({ target: [providerConnections.organizationId, providerConnections.provider], set: { credentials, accountId: identity.accountId, email: identity.email, expiresAt, status: "connected", updatedAt: new Date() } }); await tx.insert(auditEvents).values({ id: uuidv7(), organizationId: actor.organizationId, actorId: actor.userId, action: "provider.openai.connected", targetType: "provider", result: "success", metadata: { provider: "openai-codex" }, correlationId: request.id }); });
  pendingCodexAuth.delete(authId);
  return { status: "connected", connected: true, email: identity.email, accountId: identity.accountId };
});
app.delete("/api/v1/providers/openai", async (request) => { const actor = requireActorPermission(request, "provider.manage"); await db.transaction(async (tx) => { await tx.execute(sql`select id from organizations where id = ${actor.organizationId} for update`); const removed = (await tx.delete(providerConnections).where(and(eq(providerConnections.organizationId, actor.organizationId), eq(providerConnections.provider, "openai-codex"))).returning({ isDefault: providerConnections.isDefault }))[0]; if (removed?.isDefault) { const fallback = defaultFallback(await tx.select({ id: providerConnections.id, provider: providerConnections.provider, settings: providerConnections.settings }).from(providerConnections).where(and(eq(providerConnections.organizationId, actor.organizationId), eq(providerConnections.status, "connected")))); if (fallback) await tx.update(providerConnections).set({ isDefault: true, updatedAt: new Date() }).where(eq(providerConnections.id, fallback.id)); } await tx.insert(auditEvents).values({ id: uuidv7(), organizationId: actor.organizationId, actorId: actor.userId, action: "provider.openai.disconnected", targetType: "provider", result: "success", correlationId: request.id }); }); return { connected: false }; });

app.get("/api/v1/providers/openrouter", async (request) => { const actor = requireActorPermission(request, "provider.read"); const row = (await db.select({ status: providerConnections.status, label: providerConnections.accountId, settings: providerConnections.settings, isDefault: providerConnections.isDefault }).from(providerConnections).where(and(eq(providerConnections.organizationId, actor.organizationId), eq(providerConnections.provider, "openrouter"))).limit(1))[0]; return row ? { connected: row.status === "connected", ...row } : { connected: false, status: "disconnected", settings: {}, isDefault: false }; });
app.post("/api/v1/providers/openrouter", { config: { rateLimit: { max: 5, timeWindow: "10 minutes" } } }, async (request, reply) => {
  const actor = requireActorPermission(request, "provider.manage");
  const { apiKey } = openRouterCredentialsSchema.parse(request.body);
  const started = Date.now();
  const key = await openRouterKeyInfo(apiKey);
  const credentials = encryptCredentials({ apiKey }, env.SESSION_SECRET);
  let existed = false, modelCount = 0;
  await db.transaction(async (tx) => {
    await tx.execute(sql`select id from organizations where id = ${actor.organizationId} for update`);
    const existing = (await tx.select({ settings: providerConnections.settings }).from(providerConnections).where(and(eq(providerConnections.organizationId, actor.organizationId), eq(providerConnections.provider, "openrouter"))).limit(1))[0];
    existed = Boolean(existing);
    const previousSettings = existing?.settings && typeof existing.settings === "object" ? existing.settings : {};
    modelCount = openRouterSettings(previousSettings).models.length;
    const hasDefault = (await tx.select({ id: providerConnections.id }).from(providerConnections).where(and(eq(providerConnections.organizationId, actor.organizationId), eq(providerConnections.isDefault, true))).limit(1))[0];
    await tx.insert(providerConnections).values({ id: uuidv7(), organizationId: actor.organizationId, provider: "openrouter", credentials, accountId: key.data?.label, settings: previousSettings, isDefault: !hasDefault }).onConflictDoUpdate({ target: [providerConnections.organizationId, providerConnections.provider], set: { credentials, accountId: key.data?.label, settings: previousSettings, status: "connected", updatedAt: new Date() } });
    await tx.insert(auditEvents).values({ id: uuidv7(), organizationId: actor.organizationId, actorId: actor.userId, action: "provider.openrouter.connected", targetType: "provider", result: "success", metadata: { provider: "openrouter", models: modelCount }, correlationId: request.id });
  });
  return reply.status(existed ? 200 : 201).send({ connected: true, label: key.data?.label, modelCount, elapsedMs: Date.now() - started });
});
app.post("/api/v1/providers/openrouter/test", { config: { rateLimit: { max: 10, timeWindow: "5 minutes" } } }, async (request) => { const actor = requireActorPermission(request, "provider.use"), connection = await validOpenRouterConnection(actor.organizationId); const started = Date.now(); const key = await openRouterKeyInfo(connection.credentials.apiKey); return { ok: true, label: key.data?.label, limitRemaining: key.data?.limit_remaining, freeTier: key.data?.is_free_tier, elapsedMs: Date.now() - started }; });
app.post("/api/v1/providers/openrouter/models/inspect", { config: { rateLimit: { max: 20, timeWindow: "5 minutes" } } }, async (request) => {
  const actor = requireActorPermission(request, "provider.read"), model = z.object({ model: openRouterModelIdSchema }).strict().parse(request.body).model;
  const connection = await validOpenRouterConnection(actor.organizationId);
  return openRouterModelDetails(connection.credentials.apiKey, model);
});
app.put("/api/v1/providers/openrouter/models", { config: { rateLimit: { max: 20, timeWindow: "5 minutes" } } }, async (request) => {
  const actor = requireActorPermission(request, "provider.manage"), selected = openRouterEndpointSelectionSchema.parse(request.body);
  const connection = await validOpenRouterConnection(actor.organizationId), details = await openRouterModelDetails(connection.credentials.apiKey, selected.model);
  const endpoint = details.endpoints.find((entry) => selected.endpointId ? entry.endpointId === selected.endpointId : selected.providerTag ? entry.tag === selected.providerTag : entry.providerSlug === selected.providerSlug);
  if (!endpoint) fail("OPENROUTER_PROVIDER_NOT_FOUND", 409, "Dieser Provider ist für das Modell nicht mehr verfügbar. Lade die Providerliste erneut.");
  const stored: OpenRouterModel = { ...details.model, ...(endpoint.contextLength ? { contextLength: endpoint.contextLength } : {}), endpoint };
  await db.transaction(async (tx) => {
    await tx.execute(sql`select id from organizations where id = ${actor.organizationId} for update`);
    const current = (await tx.select({ settings: providerConnections.settings }).from(providerConnections).where(and(eq(providerConnections.id, connection.id), eq(providerConnections.organizationId, actor.organizationId), eq(providerConnections.status, "connected"))).limit(1))[0];
    if (!current) fail("OPENROUTER_NOT_CONNECTED", 409, "OpenRouter wurde inzwischen getrennt.");
    const raw = current.settings && typeof current.settings === "object" ? current.settings : {}, previous = openRouterSettings(raw);
    const models = [...previous.models.filter((model) => model.id !== stored.id), stored];
    const active = models.find((model) => model.id === previous.model) ?? stored;
    const effort = previous.effort && active.efforts.includes(previous.effort) ? previous.effort : active.defaultEffort ?? active.efforts[0];
    const { effort: _oldEffort, ...withoutEffort } = raw;
    await tx.update(providerConnections).set({ settings: { ...withoutEffort, models, model: active.id, ...(effort ? { effort } : {}) }, updatedAt: new Date() }).where(eq(providerConnections.id, connection.id));
    await tx.insert(auditEvents).values({ id: uuidv7(), organizationId: actor.organizationId, actorId: actor.userId, action: "provider.openrouter.model.saved", targetType: "provider", targetId: connection.id, result: "success", metadata: { model: stored.id, provider: endpoint.providerSlug }, correlationId: request.id });
  });
  return { model: stored };
});
app.delete("/api/v1/providers/openrouter/models", async (request) => {
  const actor = requireActorPermission(request, "provider.manage"), modelId = z.object({ model: openRouterModelIdSchema }).strict().parse(request.body).model;
  const connection = await validOpenRouterConnection(actor.organizationId);
  await db.transaction(async (tx) => {
    await tx.execute(sql`select id from organizations where id = ${actor.organizationId} for update`);
    const current = (await tx.select({ settings: providerConnections.settings, isDefault: providerConnections.isDefault }).from(providerConnections).where(and(eq(providerConnections.id, connection.id), eq(providerConnections.organizationId, actor.organizationId), eq(providerConnections.status, "connected"))).limit(1))[0];
    if (!current) fail("OPENROUTER_NOT_CONNECTED", 409, "OpenRouter wurde inzwischen getrennt.");
    const raw = current.settings && typeof current.settings === "object" ? current.settings : {}, previous = openRouterSettings(raw), models = previous.models.filter((model) => model.id !== modelId);
    if (models.length === previous.models.length) fail("OPENROUTER_MODEL_NOT_SAVED", 404, "Dieses Modell ist nicht in deinen OpenRouter-Modellen gespeichert.");
    const active = previous.model === modelId ? models[0] : models.find((model) => model.id === previous.model);
    const effort = active && previous.effort && active.efforts.includes(previous.effort) ? previous.effort : active?.defaultEffort ?? active?.efforts[0];
    const { model: _oldModel, effort: _oldEffort, ...withoutSelection } = raw;
    const fallback = !models.length && current.isDefault ? defaultFallback(await tx.select({ id: providerConnections.id, provider: providerConnections.provider, settings: providerConnections.settings }).from(providerConnections).where(and(eq(providerConnections.organizationId, actor.organizationId), eq(providerConnections.status, "connected"), notInArray(providerConnections.id, [connection.id])))) : undefined;
    await tx.update(providerConnections).set({ settings: { ...withoutSelection, models, ...(active ? { model: active.id } : {}), ...(effort ? { effort } : {}) }, ...(fallback ? { isDefault: false } : {}), updatedAt: new Date() }).where(eq(providerConnections.id, connection.id));
    if (fallback) await tx.update(providerConnections).set({ isDefault: true, updatedAt: new Date() }).where(eq(providerConnections.id, fallback.id));
    await tx.insert(auditEvents).values({ id: uuidv7(), organizationId: actor.organizationId, actorId: actor.userId, action: "provider.openrouter.model.removed", targetType: "provider", targetId: connection.id, result: "success", metadata: { model: modelId }, correlationId: request.id });
  });
  return { removed: true };
});
app.delete("/api/v1/providers/openrouter", async (request) => { const actor = requireActorPermission(request, "provider.manage"); await db.transaction(async (tx) => { await tx.execute(sql`select id from organizations where id = ${actor.organizationId} for update`); const removed = (await tx.delete(providerConnections).where(and(eq(providerConnections.organizationId, actor.organizationId), eq(providerConnections.provider, "openrouter"))).returning({ isDefault: providerConnections.isDefault }))[0]; if (removed?.isDefault) { const fallback = defaultFallback(await tx.select({ id: providerConnections.id, provider: providerConnections.provider, settings: providerConnections.settings }).from(providerConnections).where(and(eq(providerConnections.organizationId, actor.organizationId), eq(providerConnections.status, "connected")))); if (fallback) await tx.update(providerConnections).set({ isDefault: true, updatedAt: new Date() }).where(eq(providerConnections.id, fallback.id)); } await tx.insert(auditEvents).values({ id: uuidv7(), organizationId: actor.organizationId, actorId: actor.userId, action: "provider.openrouter.disconnected", targetType: "provider", result: "success", correlationId: request.id }); }); return { connected: false }; });

app.get("/api/v1/providers/zen", async (request) => { const actor = requireActorPermission(request, "provider.read"); const row = (await db.select({ status: providerConnections.status, settings: providerConnections.settings, isDefault: providerConnections.isDefault }).from(providerConnections).where(and(eq(providerConnections.organizationId, actor.organizationId), eq(providerConnections.provider, "opencode-zen"))).limit(1))[0]; return row ? { connected: row.status === "connected", modelCount: zenSettings(row.settings).models.length, ...row } : { connected: false, status: "disconnected", modelCount: 0, settings: {}, isDefault: false }; });
app.post("/api/v1/providers/zen", { config: { rateLimit: { max: 5, timeWindow: "10 minutes" } } }, async (request, reply) => {
  const actor = requireActorPermission(request, "provider.manage"), models = await fetchZenFreeModels(true);
  if (!models.length) fail("ZEN_MODELS_UNAVAILABLE", 503, "OpenCode Zen meldet derzeit keine kostenlosen Chat-Completions-Modelle.", true);
  const credentials = encryptCredentials({ apiKey: "public" }, env.SESSION_SECRET);
  let existed = false;
  await db.transaction(async (tx) => {
    await tx.execute(sql`select id from organizations where id = ${actor.organizationId} for update`);
    const existing = (await tx.select({ settings: providerConnections.settings }).from(providerConnections).where(and(eq(providerConnections.organizationId, actor.organizationId), eq(providerConnections.provider, "opencode-zen"))).limit(1))[0];
    existed = Boolean(existing);
    const previous = zenSettings(existing?.settings), active = models.find((model) => model.id === previous.model) ?? models[0]!;
    const effort = previous.effort && active.efforts.includes(previous.effort) ? previous.effort : undefined;
    const settings = { models, model: active.id, ...(effort ? { effort } : {}) };
    const hasDefault = (await tx.select({ id: providerConnections.id }).from(providerConnections).where(and(eq(providerConnections.organizationId, actor.organizationId), eq(providerConnections.isDefault, true))).limit(1))[0];
    await tx.insert(providerConnections).values({ id: uuidv7(), organizationId: actor.organizationId, provider: "opencode-zen", credentials, settings, isDefault: !hasDefault }).onConflictDoUpdate({ target: [providerConnections.organizationId, providerConnections.provider], set: { credentials, settings, status: "connected", updatedAt: new Date() } });
    await tx.insert(auditEvents).values({ id: uuidv7(), organizationId: actor.organizationId, actorId: actor.userId, action: "provider.zen.connected", targetType: "provider", result: "success", metadata: { provider: "opencode-zen", models: models.length }, correlationId: request.id });
  });
  return reply.status(existed ? 200 : 201).send({ connected: true, modelCount: models.length });
});
app.post("/api/v1/providers/zen/test", { config: { rateLimit: { max: 10, timeWindow: "5 minutes" } } }, async (request) => {
  const actor = requireActorPermission(request, "provider.use"), connection = await validZenConnection(actor.organizationId), models = await zenModelsForConnection(connection, true);
  if (!models.length) fail("ZEN_MODELS_UNAVAILABLE", 503, "OpenCode Zen meldet derzeit keine kostenlosen Chat-Completions-Modelle.", true);
  const previous = zenSettings(connection.settings), model = models.find((entry) => entry.id === previous.model) ?? models[0]!;
  const effort = previous.effort && model.efforts.includes(previous.effort) ? previous.effort : undefined;
  const started = Date.now();
  const response = await zenFetch(zenApi.chatUrl, connection.credentials.apiKey, { method: "POST", signal: AbortSignal.timeout(120_000), headers: { accept: "text/event-stream", "content-type": "application/json" }, body: JSON.stringify(zenRequest(model.id, "Antworte ausschließlich mit OK.", "Verbindungstest", effort, 16)) });
  const raw = await response.text();
  if (!response.ok) throw zenHttpError(response.status, response.headers.get("retry-after"), raw);
  if (!parseZenEventStream(raw).text.trim()) fail("ZEN_TEST_INCOMPLETE", 502, "OpenCode Zen hat den Verbindungstest nicht vollständig abgeschlossen.", true);
  await db.transaction(async (tx) => {
    await tx.execute(sql`select id from organizations where id = ${actor.organizationId} for update`);
    const current = (await tx.select({ settings: providerConnections.settings }).from(providerConnections).where(and(eq(providerConnections.id, connection.id), eq(providerConnections.status, "connected"))).limit(1))[0];
    if (!current) return;
    const raw = current.settings && typeof current.settings === "object" ? current.settings : {}, currentSettings = zenSettings(raw);
    const active = models.find((entry) => entry.id === currentSettings.model) ?? model;
    const activeEffort = currentSettings.effort && active.efforts.includes(currentSettings.effort) ? currentSettings.effort : undefined;
    const { effort: _oldEffort, ...withoutEffort } = raw;
    await tx.update(providerConnections).set({ settings: { ...withoutEffort, models, model: active.id, ...(activeEffort ? { effort: activeEffort } : {}) }, updatedAt: new Date() }).where(eq(providerConnections.id, connection.id));
  });
  return { ok: true, model: model.id, modelCount: models.length, elapsedMs: Date.now() - started };
});
app.delete("/api/v1/providers/zen", async (request) => { const actor = requireActorPermission(request, "provider.manage"); await db.transaction(async (tx) => { await tx.execute(sql`select id from organizations where id = ${actor.organizationId} for update`); const removed = (await tx.delete(providerConnections).where(and(eq(providerConnections.organizationId, actor.organizationId), eq(providerConnections.provider, "opencode-zen"))).returning({ isDefault: providerConnections.isDefault }))[0]; if (removed?.isDefault) { const fallback = defaultFallback(await tx.select({ id: providerConnections.id, provider: providerConnections.provider, settings: providerConnections.settings }).from(providerConnections).where(and(eq(providerConnections.organizationId, actor.organizationId), eq(providerConnections.status, "connected")))); if (fallback) await tx.update(providerConnections).set({ isDefault: true, updatedAt: new Date() }).where(eq(providerConnections.id, fallback.id)); } await tx.insert(auditEvents).values({ id: uuidv7(), organizationId: actor.organizationId, actorId: actor.userId, action: "provider.zen.disconnected", targetType: "provider", result: "success", correlationId: request.id }); }); return { connected: false }; });
app.get("/api/v1/providers/models", async (request) => { const actor = requireActorPermission(request, "provider.read"); return providerCatalog(actor.organizationId); });
app.patch("/api/v1/providers/default-model", async (request) => {
  const actor = requireActorPermission(request, "provider.manage");
  const requested = modelSelectionSchema.parse(request.body);
  const connection = (await db.select().from(providerConnections).where(and(eq(providerConnections.organizationId, actor.organizationId), eq(providerConnections.provider, requested.provider), eq(providerConnections.status, "connected"))).limit(1))[0];
  if (!connection) fail("PROVIDER_NOT_CONNECTED", 409, "Der gewählte Modellprovider ist nicht verbunden.");
  let selection: ModelSelection;
  if (requested.provider === "openai-codex") {
    if (!codexModels.includes(requested.model as typeof codexModels[number])) fail("MODEL_INVALID", 400, "Dieses Codex-Modell wird nicht unterstützt.");
    if (!requested.effort || !codexEfforts.includes(requested.effort as typeof codexEfforts[number])) fail("EFFORT_INVALID", 400, "Bitte einen gültigen Codex-Effort auswählen.");
    selection = { provider: "openai-codex", model: requested.model, effort: requested.effort };
  } else if (requested.provider === "openrouter") {
    const model = openRouterSettings(connection.settings).models.find((entry) => entry.id === requested.model);
    if (!model) fail("MODEL_INVALID", 400, "Dieses OpenRouter-Modell wurde nicht in den Einstellungen hinzugefügt.");
    if (requested.effort && !model.efforts.includes(requested.effort)) fail("EFFORT_INVALID", 400, "Der gewählte Effort wird von diesem OpenRouter-Modell nicht unterstützt.");
    selection = { provider: "openrouter", model: model.id, ...(requested.effort ? { effort: requested.effort } : {}) };
  } else {
    const model = (await zenModelsForConnection(connection)).find((entry) => entry.id === requested.model);
    if (!model) fail("MODEL_INVALID", 400, "Dieses Modell ist bei OpenCode Zen nicht mehr kostenlos verfügbar.");
    if (requested.effort && !model.efforts.includes(requested.effort)) fail("EFFORT_INVALID", 400, "Der gewählte Effort wird von diesem OpenCode-Zen-Modell nicht unterstützt.");
    selection = { provider: "opencode-zen", model: model.id, ...(requested.effort ? { effort: requested.effort } : {}) };
  }
  const rawSettings = connection.settings && typeof connection.settings === "object" ? connection.settings : {};
  const { effort: _oldEffort, ...withoutEffort } = rawSettings;
  const settings = { ...withoutEffort, model: selection.model, ...(selection.effort ? { effort: selection.effort } : {}) };
  await db.transaction(async (tx) => {
    await tx.execute(sql`select id from organizations where id = ${actor.organizationId} for update`);
    const current = (await tx.select({ updatedAt: providerConnections.updatedAt }).from(providerConnections).where(and(eq(providerConnections.id, connection.id), eq(providerConnections.organizationId, actor.organizationId), eq(providerConnections.status, "connected"))).limit(1))[0];
    if (!current) fail("PROVIDER_NOT_CONNECTED", 409, "Der gewählte Modellprovider wurde inzwischen getrennt.");
    if (current.updatedAt.getTime() !== connection.updatedAt.getTime()) fail("PROVIDER_CHANGED", 409, "Die Provider-Verbindung wurde gleichzeitig geändert. Bitte speichere die Modellwahl erneut.");
    await tx.update(providerConnections).set({ isDefault: false, updatedAt: new Date() }).where(eq(providerConnections.organizationId, actor.organizationId));
    const updated = await tx.update(providerConnections).set({ isDefault: true, settings, updatedAt: new Date() }).where(and(eq(providerConnections.id, connection.id), eq(providerConnections.organizationId, actor.organizationId))).returning({ id: providerConnections.id });
    if (!updated[0]) fail("PROVIDER_NOT_CONNECTED", 409, "Der gewählte Modellprovider wurde inzwischen getrennt.");
    await tx.insert(auditEvents).values({ id: uuidv7(), organizationId: actor.organizationId, actorId: actor.userId, action: "provider.default_model.updated", targetType: "provider", targetId: connection.id, result: "success", metadata: selection, correlationId: request.id });
  });
  return { selection };
});

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
  const initialFrame = input.viewport ?? { width: 390, height: 760, device: "Standard" };
  const initialPlatform = input.viewport ? "android" : input.platforms[0]!;
  const document: DesignDocument = { schemaVersion: 1, projectId, projectType: input.type, fidelity: input.fidelity, platforms: input.platforms, designSystemVersionId: input.designSystemVersionId, themes: [{ id: "light", name: "Hell", tokens: { "color.bg": "#F5F7FA", "color.surface": "#FFFFFF", "color.accent": "#3157D5" } }], pages: [{ id: pageId, name: "Start", type: input.type === "presentation" ? "slide" : input.type === "document" ? "document-page" : input.type === "canvas" ? "canvas" : "screen", frameIds: [frameId] }], frames: [{ id: frameId, pageId, name: "Start", platform: initialPlatform, device: initialFrame.device, width: initialFrame.width, height: initialFrame.height, theme: "light", locale: "de-DE", rootNodeId: nodeId, canvasX: 0, canvasY: 0 }], nodes: [{ id: nodeId, name: "Start", parentId: null, childIds: [], bounds: { x: 0, y: 0, width: initialFrame.width, height: initialFrame.height }, visible: true, locked: false, tokenBindings: {}, semantics: { role: "main", label: "Start" }, type: "container", layout: "column", gap: 16, padding: [24, 24, 24, 24], fill: "color.surface" }], assets: [], interactions: [], metadata: { createdAt: new Date().toISOString(), compilerVersion: "0.1.0" } };
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
    // Ein Spec-Paket beschreibt eine Software, die es noch nicht gibt. Es gibt darin nichts zu
    // finden, was man als Startseite anzeigen koennte — das Design muss daraus erst GEBAUT werden.
    // Deshalb loest es den Aufbau immer aus, unabhaengig von der Zielplattform.
    const hasSpecSources = files.some((file) => specSourceFile.test(file.path));
    const needsReconstruction = storeAsFiles && (hasSpecSources || (platform !== "web" && hasNativeUiSources));
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
    await db.delete(designSnapshots).where(eq(designSnapshots.projectId, projectId));
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
  const undo = (await db.select({ label: designSnapshots.label, createdAt: designSnapshots.createdAt }).from(designSnapshots).where(and(eq(designSnapshots.projectId, projectId), eq(designSnapshots.organizationId, actor.organizationId))).orderBy(desc(designSnapshots.createdAt)).limit(1))[0];
  return { imported: true as const, ...(undo ? { undo: { label: undo.label, createdAt: undo.createdAt.toISOString() } } : {}), entryPath: row.imported.entryPath, reconstructed, fileCount: row.imported.fileCount, totalBytes: row.imported.totalBytes, revision: row.revision, files: row.imported.manifest, previewWidth: profile.width, previewHeight: profile.height, previewDevice: profile.device, variants, ...(row.imported.entryPath ? { previewPath: `${previewBase}${row.imported.entryPath}?revision=${row.revision}` } : {}) };
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
// Der Download ist die Übergabe an den Design-Umsetzer — er muss das GANZE Design enthalten, nicht
// nur die eine Datei, die das Studio gerade zeigt. Deshalb werden hier zusätzlich die gemessenen
// Design-Fakten und je Bildschirm eine eigene Datei je Erscheinung erzeugt. Ohne diesen Schritt kam
// beim Öffnen nur die eingebaute (meist dunkle) Erscheinung an, weil die Farbumschaltung des Studios
// erst in der Vorschau entsteht und nie in der gespeicherten Datei landet.
app.get("/api/v1/projects/:projectId/export.zip", async (request, reply) => {
  const actor = requireActorPermission(request, "project.export");
  const { projectId } = z.object({ projectId: z.string().uuid() }).parse(request.params);
  const row = (await db.select({ imported: projectImports, name: projects.name, platforms: projects.platforms }).from(projectImports).innerJoin(projects, eq(projects.id, projectImports.projectId)).where(and(eq(projectImports.projectId, projectId), eq(projectImports.organizationId, actor.organizationId))).limit(1))[0];
  if (!row) fail("EXPORT_NOT_AVAILABLE", 404, "Für dieses Projekt liegt kein importiertes Dateipaket vor.");
  const dateiBasis = row.name.replaceAll(/[^\p{L}\p{N} _.-]/gu, "").trim().slice(0, 80) || "werft-projekt";
  const platform = (Array.isArray(row.platforms) ? row.platforms[0] : "web") as ImportPlatform;
  const designPaths = row.imported.manifest.map((file) => file.path).filter((path) => generatedDesignPathPattern.test(path));
  // Die Fakten werden hier neu gemessen statt gespeichert: derselbe deterministische Weg wie in der
  // Erscheinungs-Abfrage, damit Export und Studio nie auseinanderlaufen.
  let designFiles: Array<{ path: string; content: string }> = [];
  let designReport: PackageReport | undefined;
  const { ziel } = zielSchema.parse(request.query ?? {});
  if (designPaths.length) {
    try {
      const built = await buildDesignExport(row.imported, row.name, platform, designPaths, ziel);
      designFiles = built.files;
      designReport = built.report;
    } catch (error) { request.log.error({ err: error, projectId }, "Designpaket für den Export konnte nicht erzeugt werden; das Rohpaket wird trotzdem ausgeliefert"); }
  }
  // Stufe 2 sucht in der Outbox nach `<App>-SPEC-v2.zip`. Enthaelt das Paket die Specs, traegt der
  // Download denselben Namen — sonst muesste die Datei nach jedem Herunterladen umbenannt werden.
  const fileName = `${dateiBasis}${designFiles.some((file) => file.path === "SPEC.md") ? "-SPEC-v2" : ""}.zip`;
  request.log.info({ event: "export.package", projectId, files: row.imported.manifest.length, designFiles: designFiles.length, ...(designReport ?? {}) }, "Projektexport zusammengestellt");
  const archive = archiver("zip", { zlib: { level: 6 } });
  // Wurde ein frueherer Export wieder importiert, liegt sein Designpaket als Quelldatei im Projekt.
  // Dann gilt das FRISCH erzeugte — zwei gleichnamige Eintraege im Archiv waeren sonst unaufloesbar.
  const ersetzt = new Set(designFiles.map((file) => file.path));
  for (const file of row.imported.manifest) {
    if (ersetzt.has(file.path)) continue;
    archive.append(await readObject(`${row.imported.objectPrefix}${file.path}`), { name: file.path });
  }
  for (const file of designFiles) archive.append(Buffer.from(file.content, "utf8"), { name: file.path });
  void archive.finalize();
  return reply.header("content-type", "application/zip").header("content-disposition", `attachment; filename="${encodeURIComponent(fileName)}"`).send(archive);
});
// Beim Herunterladen wird ausdruecklich gefragt, FUER welches System das Spec geschrieben werden
// soll. Ohne diese Angabe uebersetzt der Spec-Schreiber in die Richtung, aus der importiert wurde —
// das ist bei einem aus Android-Quellen aufgebauten Design, das nach Windows soll, genau verkehrt.
const zielPlattformen = ["android", "windows", "macos", "ios", "ipados", "web"] as const;
const zielSchema = z.object({ ziel: z.enum(zielPlattformen).optional() });
async function buildDesignExport(imported: { objectPrefix: string; manifest: ImportManifestFile[]; entryPath: string }, projectName: string, platform: ImportPlatform, designPaths: string[], ziel?: string) {
  const factPaths = new Set(factCandidatePaths(platform, imported.manifest.map((file) => file.path)));
  const factFiles = imported.manifest.filter((file) => factPaths.has(file.path) && file.size <= maxFactFileBytes).slice(0, maxFactFiles);
  const factTexts = await mapWithConcurrency(factFiles, factReadConcurrency, async (file) => ({ path: file.path, text: (await readObject(`${imported.objectPrefix}${file.path}`)).toString("utf8") }));
  const facts = extractDesignFacts(platform, factTexts);
  const designs = await mapWithConcurrency(designPaths, factReadConcurrency, async (path) => ({
    path,
    label: path.slice(path.lastIndexOf("/") + 1),
    html: (await readObject(`${imported.objectPrefix}${path}`)).toString("utf8")
  }));
  // Kam das Projekt aus einem Spec-ZIP der Inbox, liegen die Erst-Specs als gewoehnliche Dateien im
  // Import. Sie sind die Vorlage, gegen die der Export „neu gezeichnet" von „war schon da" trennt.
  const vorlage = await leseVorlage(imported);
  const built = buildExportPackage({
    projectName, platform, facts, designs,
    entryPath: designPaths.includes(imported.entryPath) ? imported.entryPath : designPaths[0]!,
    sourceFiles: imported.manifest.map((file) => ({ path: file.path, size: file.size })),
    ...(vorlage ? { vorlage } : {}), ...(ziel ? { zielPlattform: ziel } : {})
  });
  return { files: built.files, report: built.report };
}
const vorlagenDateien = {
  projekt: /(^|\/)00-PROJEKT\.md$/i, funktion: /(^|\/)01-FUNKTIONS-SPEC\.md$/i,
  ui: /(^|\/)02-UI-SPEC\.md$/i, motion: /(^|\/)03-MOTION-SPEC\.md$/i,
  onboarding: /(^|\/)04-ONBOARDING-SPEC\.md$/i, recht: /(^|\/)05-RECHT-SPEC\.md$/i
} as const;
async function leseVorlage(imported: { objectPrefix: string; manifest: ImportManifestFile[] }): Promise<Vorlage | undefined> {
  const vorlage: Vorlage = {};
  for (const [schluessel, muster] of Object.entries(vorlagenDateien) as Array<[keyof Vorlage, RegExp]>) {
    const treffer = imported.manifest.find((file) => muster.test(file.path) && file.size <= maxFactFileBytes);
    if (treffer) vorlage[schluessel] = (await readObject(`${imported.objectPrefix}${treffer.path}`)).toString("utf8");
  }
  return Object.keys(vorlage).length ? vorlage : undefined;
}
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
// Der Auftrag an das Modell ist ARBEITEN, nicht fragen. Die frühere Fassung verlangte bei jeder
// Mehrdeutigkeit nummerierte Rückfragen — das Ergebnis waren Läufe, die drei Fragen stellten und
// keine einzige Zeile änderten. Rückfragen sind jetzt der ausdrückliche Ausnahmefall.
function buildChatInstructions(scope: "global" | "screen" | "marked"): string {
  return [
    "Du bist der Design-Editor von Werft Studio und arbeitest direkt an den Dateien eines Design-Projekts.",
    "Dein Auftrag ist es, den Änderungswunsch UMZUSETZEN. Du lieferst in jeder Antwort echte Änderungen.",
    "",
    "AUSGABEFORMAT — antworte AUSSCHLIESSLICH mit EINEM JSON-Objekt, ohne Markdown, ohne Codefences, ohne Vorrede:",
    '{"reply":"kurze deutsche Zusammenfassung, was du geändert hast","naechste":["konkreter nächster Schritt","noch einer"],"changes":[{"path":"pfad/wie/geliefert","edits":[{"find":"wörtlich vorhandener Ausschnitt","replace":"neuer Ausschnitt"},{"find":"…","replace":"…","all":true}]}]}',
    '"naechste": zwei bis drei KONKRETE nächste Design-Schritte, die du an diesem Design als Nächstes empfehlen würdest — jeweils ein kurzer, sofort ausführbarer Auftrag in der Ich-Form des Benutzers (z. B. „Die Trennlinien in den Listen dezenter machen"). Keine Rückfragen, keine Floskeln.',
    '"all": true ersetzt ALLE Vorkommen der Stelle — genau richtig für einen Wert, der überall gleich angepasst werden soll.',
    "",
    "REGELN FÜR find:",
    "- find muss wörtlich im aktuellen Dateiinhalt vorkommen. Kopiere den Ausschnitt aus den gelieferten Dateien, schreibe ihn nicht aus dem Gedächtnis.",
    "- Nimm so viel Kontext mit, dass die Stelle eindeutig ist — oder setze \"all\": true, wenn alle Vorkommen gemeint sind.",
    "- Kleine, gezielte Edits statt großer Blöcke. Höchstens 25 Edits pro Antwort; brauchst du mehr, liefere die wichtigsten zuerst — du wirst automatisch erneut aufgerufen, um weiterzumachen.",
    "- Ändere nichts, was der Wunsch nicht verlangt. Struktur, Texte, Funktionen und Skripte bleiben erhalten.",
    "- Kein Bildschirm, kein <section class=\"werft-screen\">-Abschnitt und kein <script> darf verschwinden.",
    "",
    // Aus diesem Design wird spaeter ein Spec und daraus eine echte Software. Ein Bedienelement, zu
    // dem niemand aufgeschrieben hat, was es tun soll, wird dort zu einem toten Knopf — der Benutzer
    // muesste es beim Rueckimport noch einmal erklaeren. Genau das verhindert diese Regel: was er dir
    // hier sagt, bleibt am Element haengen.
    "BEDIENELEMENTE BRAUCHEN EINE AUFGABE — sonst entsteht daraus später ein toter Knopf:",
    "- Fügst du ein anfassbares Element ein (Knopf, Schalter, Link, Eingabefeld, Listeneintrag), gib ihm ein Attribut `data-werft-funktion=\"…\"` mit EINEM Satz, was es tun soll — in den Worten des Wunsches. Beispiel: <button data-werft-funktion=\"pausiert die laufende Sitzung und merkt sich die Stelle\">Pause</button>.",
    "- Führt das Element nur zu einem anderen Bildschirm, reicht `data-werft-navigate=\"<ziel-id>\"`; dann braucht es kein data-werft-funktion.",
    "- Ein vorhandenes `data-werft-funktion` NIE entfernen und nicht überschreiben, wenn darin eine Kennung wie `F-07` steht — das ist die Verbindung zur bereits beschriebenen Funktion.",
    "- Sagt der Wunsch nicht, was das neue Element tun soll, schreibe deine beste Annahme hinein statt das Attribut wegzulassen, und erwähne die Annahme in \"reply\".",
    "",
    scope === "global"
      ? "UMFANG: Der Wunsch gilt dem GESAMTEN Design. Ändere ihn auf ALLEN Bildschirmen und in ALLEN Unterbildschirmen. Am wirkungsvollsten sind gemeinsam genutzte CSS-Regeln und Theme-Variablen: eine geänderte gemeinsame Regel wirkt sofort überall. Gibt es zusätzlich bildschirmeigene Regeln mit demselben Zweck, ändere sie mit. Beschränke dich NICHT auf den gerade sichtbaren Bildschirm."
      : scope === "marked"
        ? "UMFANG: Es ist ein MARKIERTER BEREICH angegeben. Er ist eindeutig — ändere genau dieses Element und frage nicht nach, welches gemeint ist. Der mitgelieferte Ausschnitt stammt wörtlich aus der Datei und ist die Grundlage für find."
        : "UMFANG: Der Wunsch bezieht sich auf den gerade sichtbaren Bildschirm. Ändere dessen <section class=\"werft-screen\">-Abschnitt und die Regeln, die nur ihn betreffen. Ist eine Regel mit anderen Bildschirmen geteilt und würde eine Änderung dort schaden, lege stattdessen eine bildschirmeigene Regel an.",
    "",
    "BEI UNKLARHEIT: Frage NICHT zurück. Triff die fachlich beste Annahme, setze sie um und schreibe in \"reply\" in einem Satz, wovon du ausgegangen bist. Nur wenn eine Umsetzung technisch unmöglich ist (die genannte Sache existiert im Design nicht), lieferst du \"changes\": [] und erklärst kurz, was fehlt.",
    "",
    "DAMIT DEINE ÄNDERUNG WIRKLICH ANKOMMT — das ist der häufigste Grund, warum ein Lauf nichts bewirkt:",
    "- Unter === DESIGN-ANALYSE === stehen die KASKADENFALLEN dieses Designs: Regeln, die über einen Elementnamen greifen (z. B. `.pm-start-screen button { padding: 0 }`) und damit JEDE reine Klassenregel schlagen. Eine Klassenregel dagegen zu ändern sieht richtig aus und tut NICHTS.",
    "- Prüfe vor jedem Edit: Steht die Eigenschaft, die du änderst, für dieses Element in einer solchen Regel? Dann ändere entweder genau diese Regel — oder schreibe deine Regel spezifischer, indem du den Bildschirm-Container voranstellst.",
    "- Farben: Definiert das Design seine Farben über CSS-Variablen, ändere die Variablen. Stehen zusätzlich feste Farbwerte in einzelnen Regeln, ändere sie mit — sonst bleibt die Hälfte des Designs unverändert.",
    "",
    "GESTALTERISCHES HANDWERK — du bist Designer, nicht nur Textersetzer:",
    "- Bleib in der Formensprache des Designs (siehe DESIGN-ANALYSE): benutze die dort gelisteten Abstands-, Radien- und Schriftwerte, statt krumme Zwischenwerte einzustreuen.",
    "- „Klebt am Rand“ heißt: der CONTAINER braucht mehr Innenabstand, nicht das Kind mehr Außenabstand. Innenabstand zuerst, Außenabstand nur für den Abstand ZWISCHEN Geschwistern.",
    "- Abstand ist hierarchisch: zwischen Gruppen mehr als innerhalb einer Gruppe. Gleiche Bedeutung bekommt gleichen Abstand — Unregelmäßigkeit liest sich als Fehler.",
    "- Bedienelemente bleiben mindestens 44 px hoch bzw. breit; wächst der Innenabstand, wächst die Fläche mit, nicht der Text.",
    "- Optischer Ausgleich: Symbole und runde Formen brauchen etwas weniger Abstand als Text, um gleich weit weg zu WIRKEN.",
    "- Rechtsbündige Werte in Listenzeilen brauchen Luft zum Rand und zum Trennzeichen; klebt ein Wert am Rand, erhöhe den Innenabstand der Zeile statt den Text zu kürzen.",
    "- Ändere Gestaltung, nicht Inhalt: Texte, Beschriftungen und Funktionen bleiben unangetastet, solange nicht ausdrücklich danach gefragt wird.",
    "Antworte auf Deutsch."
  ].join("\n");
}
type ModelRunOptions = { operation?: string; jobId?: string; signal?: AbortSignal; onAttempt?: (attempt: number) => void; selection?: ModelSelection };
type ModelRunResult = { text: string; provider: ProviderId; model: string; effort?: string; attempts: number; durationMs: number; inputChars: number; outputChars: number; truncated: boolean; servedBy?: string };
async function modelRun(organizationId: string, instructions: string, input: string, log: FastifyRequest["log"], options: ModelRunOptions = {}): Promise<ModelRunResult> {
  const runStartedAt = Date.now();
  const selection = await resolveModelSelection(organizationId, options.selection);
  const codexConnection = selection.provider === "openai-codex" ? await validCodexConnection(organizationId) : undefined;
  const routerConnection = selection.provider === "openrouter" ? await validOpenRouterConnection(organizationId) : undefined;
  const zenConnection = selection.provider === "opencode-zen" ? await validZenConnection(organizationId) : undefined;
  const routerModel = routerConnection ? openRouterSettings(routerConnection.settings).models.find((model) => model.id === selection.model) : undefined;
  const zenModel = zenConnection ? (await zenModelsForConnection(zenConnection)).find((model) => model.id === selection.model) : undefined;
  if (routerConnection && !routerModel) fail("MODEL_NOT_AVAILABLE", 409, "Das gewählte OpenRouter-Modell wurde nicht in den Einstellungen hinzugefügt.");
  if (zenConnection && !zenModel) fail("MODEL_NOT_AVAILABLE", 409, "Das gewählte Modell ist bei OpenCode Zen nicht mehr kostenlos verfügbar.");
  // Der Ausgaberaum wird AUSDRUECKLICH reserviert und mitgeschickt: ohne ihn deckeln viele
  // OpenRouter-Anbieter still auf ihren Standardwert und die Antwort bricht mitten im JSON ab.
  const routerOutputTokens = routerModel ? reservedOutputTokens(routerModel.contextLength, routerModel.endpoint?.maxCompletionTokens) : undefined;
  const zenOutputTokens = zenModel ? reservedOutputTokens(zenModel.contextLength, zenModel.maxOutputTokens) : undefined;
  if (routerModel) assertOpenRouterContext(routerModel, instructions, input, routerOutputTokens);
  if (zenModel) assertZenContext(zenModel, instructions, input, zenOutputTokens);
  const codexModel = selection.provider === "openai-codex" ? z.enum(codexModels).parse(selection.model) : undefined;
  const codexEffort = selection.provider === "openai-codex" ? z.enum(codexEfforts).parse(selection.effort ?? "medium") : undefined;
  const accountId = codexConnection ? codexConnection.accountId || tokenIdentity(codexConnection.credentials.accessToken, codexConnection.credentials.idToken).accountId : undefined;
  if (codexConnection && !accountId) fail("OPENAI_ACCOUNT_MISSING", 401, "Im OpenAI-Token fehlt die ChatGPT-Account-ID. Bitte erneut verbinden.");
  for (let attempt = 1; attempt <= maxModelAttempts; attempt += 1) {
    options.onAttempt?.(attempt);
    const attemptStartedAt = Date.now();
    // Der Platz in der Drossel wird VOR dem Aufruf geholt und danach sofort wieder freigegeben —
    // gewartet wird ausserhalb, sonst blockierte eine wartende Wiederholung die uebrigen Schritte.
    const release = await modelThrottle.acquire();
    let waitMs = 0;
    log.info({ event: "model.request.started", jobId: options.jobId, operation: options.operation, provider: selection.provider, model: selection.model, effort: selection.effort, attempt, maxAttempts: maxModelAttempts, concurrencyLimit: modelThrottle.state.activeLimit, inputChars: input.length, instructionChars: instructions.length }, "KI-Lauf gestartet");
    try {
      const signal = options.signal ? AbortSignal.any([options.signal, AbortSignal.timeout(540_000)]) : AbortSignal.timeout(540_000);
      const response = codexConnection
        ? await fetch(codexAuth.responsesUrl, { method: "POST", signal, headers: { authorization: `Bearer ${codexConnection.credentials.accessToken}`, "chatgpt-account-id": accountId!, originator: "codex_cli_rs", "user-agent": "codex_cli_rs/0.0.0 (Werft Studio)", accept: "text/event-stream", "content-type": "application/json" }, body: JSON.stringify({ ...codexRequestFields(codexModel!, codexEffort!), instructions, input: [{ role: "user", content: input }], store: false, stream: true }) })
        // Ab dem zweiten Versuch darf OpenRouter auf einen anderen Anbieter ausweichen: der gewählte
        // bleibt erste Wahl, aber ein Ausfall bei ihm soll nicht den ganzen Lauf kosten.
        : routerConnection ? await openRouterFetch(openRouterApi.chatUrl, routerConnection.credentials.apiKey, { method: "POST", signal, headers: { accept: "text/event-stream", "content-type": "application/json" }, body: JSON.stringify(openRouterRequest(selection.model, instructions, input, selection.effort, routerModel!.endpoint!.providerSlug, routerOutputTokens, attempt > 1)) })
          : await zenFetch(zenApi.chatUrl, zenConnection!.credentials.apiKey, { method: "POST", signal, headers: { accept: "text/event-stream", "content-type": "application/json" }, body: JSON.stringify(zenRequest(selection.model, instructions, input, selection.effort, zenOutputTokens)) });
      const raw = await response.text();
      if (!response.ok) throw selection.provider === "openai-codex" ? codexHttpError(response.status, response.headers.get("retry-after")) : selection.provider === "openrouter" ? openRouterHttpError(response.status, response.headers.get("retry-after")) : zenHttpError(response.status, response.headers.get("retry-after"), raw);
      const stream = selection.provider === "openai-codex" ? parseCodexEventStream(raw) : selection.provider === "openrouter" ? parseOpenRouterEventStream(raw) : parseZenEventStream(raw);
      const outputText = stream.text;
      if (!outputText.trim()) fail("CHAT_EMPTY", 502, `${providerDisplayName(selection.provider)} hat keine verwertbare Antwort geliefert. Bitte erneut versuchen.`, true);
      const durationMs = Date.now() - runStartedAt;
      const servedBy = selection.provider === "openrouter" ? (stream as OpenRouterStreamResult).servedBy : undefined;
      log.info({ event: "model.request.completed", jobId: options.jobId, operation: options.operation, provider: selection.provider, model: selection.model, attempt, attemptDurationMs: Date.now() - attemptStartedAt, durationMs, inputChars: input.length, outputChars: outputText.length, responseBytes: Buffer.byteLength(raw), truncated: stream.truncated, ...(servedBy ? { servedBy } : {}) }, "KI-Lauf abgeschlossen");
      return { text: outputText, provider: selection.provider, model: selection.model, ...(selection.effort ? { effort: selection.effort } : {}), attempts: attempt, durationMs, inputChars: input.length, outputChars: outputText.length, truncated: stream.truncated, ...(servedBy ? { servedBy } : {}) };
    } catch (error) {
      // Ueberlast des Anbieters (429/503) drosselt SOFORT die Nebenlaeufigkeit — auch dann, wenn
      // dieser Versuch gleich noch gelingt. Acht gleichzeitige Streams auf ein Konto sind die
      // Ursache, die Wiederholung allein kuriert nur das Symptom.
      if (isOverloadError(error)) modelThrottle.penalize();
      if (!isRetryableCodexError(error) || attempt === maxModelAttempts) {
        const details = error && typeof error === "object" ? error as Record<string, unknown> : {};
        log.warn({ err: error, event: "model.request.failed", jobId: options.jobId, operation: options.operation, provider: selection.provider, attempt, durationMs: Date.now() - runStartedAt }, "KI-Lauf dauerhaft fehlgeschlagen");
        // Der Benutzer soll wissen, dass es NICHT an seinem Wunsch lag und was er tun kann. Ohne diesen
        // Zusatz stand da nur die nackte Anbietermeldung ohne jeden Hinweis auf den nächsten Schritt.
        if (details.expose === true && error instanceof Error && attempt > 1) error.message += ` Auch ${attempt} Versuche blieben erfolglos. Der bereits erarbeitete Zwischenstand bleibt erhalten und wird beim Fortsetzen wiederverwendet.`;
        if (typeof details.code === "string") throw error;
        fail("CHAT_UPSTREAM", 502, `${providerDisplayName(selection.provider)} hat die Verbindung während des KI-Laufs beendet oder nicht rechtzeitig geantwortet. Bitte erneut versuchen.`, true);
      }
      const details = error && typeof error === "object" ? error as { retryAfterMs?: unknown } : {};
      waitMs = retryDelayMs(attempt, typeof details.retryAfterMs === "number" ? details.retryAfterMs : undefined);
      log.warn({ err: error, event: "model.request.retry", jobId: options.jobId, operation: options.operation, provider: selection.provider, attempt, nextAttempt: attempt + 1, delayMs: waitMs, concurrencyLimit: modelThrottle.state.activeLimit, attemptDurationMs: Date.now() - attemptStartedAt }, "KI-Lauf transient unterbrochen; Wiederholung wird gestartet");
    } finally { release(); }
    await new Promise((resolve) => setTimeout(resolve, waitMs));
  }
  fail("CHAT_UPSTREAM", 502, `${providerDisplayName(selection.provider)} hat den KI-Lauf nicht abgeschlossen.`, true);
}
// Ein Modell schreibt den Pfad gern verkuerzt („design.html" statt
// „.werft-generated/<id>/1/design.html"). Bisher galt das als „Datei unbekannt" und die ganze
// Aenderung fiel aus. Der Pfad wird deshalb aufgeloest, solange die Zuordnung EINDEUTIG bleibt.
function resolveManifestPath(manifest: ImportManifestFile[], wanted: string): ImportManifestFile | undefined {
  const path = wanted.replaceAll("\\", "/").replace(/^\.?\//, "");
  const exact = manifest.find((file) => file.path === path);
  if (exact) return exact;
  const suffix = manifest.filter((file) => file.path.endsWith(`/${path}`));
  if (suffix.length === 1) return suffix[0];
  const base = path.split("/").at(-1);
  const byName = base ? manifest.filter((file) => file.path.split("/").at(-1) === base) : [];
  return byName.length === 1 ? byName[0] : undefined;
}

type ChatRoundProbe = { round: number; provider: ProviderId; model: string; durationMs: number; outputChars: number; truncated: boolean; salvaged: boolean; editsApplied: number; editsFailed: number; filesWritten: number };

// Das Schreiben einer Runde: eine Transaktion, vorher Struktur-Pruefung je Datei, bei einem Fehler
// wird der vorherige Objektstand zurueckgerollt. Eine Datei, die die Pruefung nicht besteht, wird
// GAR NICHT geschrieben — ein Fix darf nie Funktionalitaet aus dem Design entfernen.
// Höchstens fünf Rückschritte je Projekt, und nur solange der gesicherte Stand handlich bleibt:
// ein Design von 278 KB kostet so gut 1,4 MB — ein Vielfaches davon wäre den Rückweg nicht wert.
const maxDesignSnapshots = 5, maxSnapshotBytes = 8 * 1024 * 1024;
async function applyChatWrites(actor: Actor, projectId: string, correlationId: string, parsed: ParsedChatResponse, settings: Record<string, unknown>, label: string) {
  const previous: Array<{ key: string; path: string; mime: string; data: Buffer }> = [];
  const reports: FileReport[] = [];
  const applied: string[] = [];
  // Vorher/Nachher der Auszeichnungsdateien: nur damit laesst sich hinterher pruefen, ob eine
  // Aenderung im Design wirklich ANKOMMT — oder ob sie die Kaskade verliert.
  const htmlChanges: Array<{ path: string; before: string; after: string }> = [];
  try {
    return await db.transaction(async (tx) => {
      await tx.execute(sql`select id from projects where id = ${projectId} and organization_id = ${actor.organizationId} for update`);
      const current = (await tx.select({ imported: projectImports, revision: projects.revision }).from(projectImports).innerJoin(projects, eq(projects.id, projectImports.projectId)).where(and(eq(projectImports.projectId, projectId), eq(projectImports.organizationId, actor.organizationId))).limit(1))[0];
      if (!current) fail("IMPORT_NOT_FOUND", 404, "Für dieses Projekt liegt kein Import vor.");
      let manifest = current.imported.manifest;
      const writeFile = async (path: string, mime: string, text: string, previousData: Buffer) => {
        const key = `${current.imported.objectPrefix}${path}`;
        const data = Buffer.from(text, "utf8");
        previous.push({ key, path, mime, data: previousData });
        await objectStore.putObject(env.S3_BUCKET, key, data, data.byteLength, { "content-type": mime });
        manifest = manifest.map((file) => (file.path === path ? { ...file, size: data.byteLength } : file));
        applied.push(path);
      };
      const targets = [
        ...parsed.changes.map((change) => ({ path: change.path, edits: change.edits, content: undefined as string | undefined })),
        ...parsed.files.map((file) => ({ path: file.path, edits: [] as ChatEdit[], content: file.content }))
      ];
      for (const change of targets) {
        const item = resolveManifestPath(manifest, change.path);
        if (!item || !chatEditableMime(item.mime)) { reports.push({ path: change.path, outcomes: [], written: false, issues: ["Diese Datei gibt es im Projekt nicht (oder sie ist keine Textdatei). Verwende einen Pfad genau so, wie er unter === PROJEKTDATEIEN === steht."] }); continue; }
        const before = await readObject(`${current.imported.objectPrefix}${item.path}`);
        const beforeText = before.toString("utf8");
        const result = change.content !== undefined ? { content: change.content, outcomes: [] } : applyChatEdits(beforeText, change.edits);
        const issues = result.content === beforeText ? [] : verifyFileWrite(item.path, beforeText, result.content);
        const written = result.content !== beforeText && !issues.length;
        if (written) {
          await writeFile(item.path, item.mime, result.content, before);
          if (/\.html?$/i.test(item.path)) htmlChanges.push({ path: item.path, before: beforeText, after: result.content });
        }
        reports.push({ path: item.path, outcomes: result.outcomes, written, issues });
      }
      if (!applied.length) return { revision: current.revision, reports, applied, htmlChanges };
      const revision = current.revision + 1;
      // Der Rückweg: der Stand VOR dieser Änderung wird gesichert, bevor die neue Fassung gilt.
      const snapshotBytes = previous.reduce((sum, entry) => sum + entry.data.byteLength, 0);
      if (snapshotBytes <= maxSnapshotBytes) {
        await tx.insert(designSnapshots).values({ id: uuidv7(), organizationId: actor.organizationId, projectId, revision: current.revision, label, files: previous.map((entry) => ({ path: entry.path, mime: entry.mime, content: entry.data.toString("utf8") })) });
        const keep = await tx.select({ id: designSnapshots.id }).from(designSnapshots).where(eq(designSnapshots.projectId, projectId)).orderBy(desc(designSnapshots.createdAt)).limit(maxDesignSnapshots);
        if (keep.length) await tx.delete(designSnapshots).where(and(eq(designSnapshots.projectId, projectId), notInArray(designSnapshots.id, keep.map((row) => row.id))));
      }
      await tx.update(projectImports).set({ manifest, totalBytes: manifest.reduce((sum, file) => sum + file.size, 0) }).where(eq(projectImports.projectId, projectId));
      await tx.update(projects).set({ revision, updatedAt: new Date() }).where(eq(projects.id, projectId));
      await tx.insert(auditEvents).values({ id: uuidv7(), organizationId: actor.organizationId, actorId: actor.userId, action: "design.ai.applied", targetType: "project", targetId: projectId, result: "success", metadata: { files: applied, revision, ...settings }, correlationId });
      return { revision, reports, applied, htmlChanges };
    });
  } catch (error) {
    await Promise.allSettled(previous.map((entry) => objectStore.putObject(env.S3_BUCKET, entry.key, entry.data, entry.data.byteLength, { "content-type": entry.mime })));
    throw error;
  }
}

// Der Rückweg aus einer KI-Änderung. Ohne ihn ist „du hast freie Hand" ein Risiko, das man nur
// ungern eingeht; mit ihm wird aus jedem Versuch ein gefahrloser Versuch.
app.post("/api/v1/projects/:projectId/design/undo", async (request) => {
  const actor = requireActorPermission(request, "design.edit");
  const { projectId } = z.object({ projectId: z.string().uuid() }).parse(request.params);
  const restored: string[] = [];
  const written: Array<{ key: string; mime: string; data: Buffer }> = [];
  try {
    const result = await db.transaction(async (tx) => {
      await tx.execute(sql`select id from projects where id = ${projectId} and organization_id = ${actor.organizationId} for update`);
      const snapshot = (await tx.select().from(designSnapshots).where(and(eq(designSnapshots.projectId, projectId), eq(designSnapshots.organizationId, actor.organizationId))).orderBy(desc(designSnapshots.createdAt)).limit(1))[0];
      if (!snapshot) fail("UNDO_NOT_AVAILABLE", 409, "Es gibt keinen gespeicherten Stand, zu dem zurückgegangen werden könnte.");
      const current = (await tx.select({ imported: projectImports, revision: projects.revision }).from(projectImports).innerJoin(projects, eq(projects.id, projectImports.projectId)).where(and(eq(projectImports.projectId, projectId), eq(projectImports.organizationId, actor.organizationId))).limit(1))[0];
      if (!current) fail("IMPORT_NOT_FOUND", 404, "Für dieses Projekt liegt kein Import vor.");
      let manifest = current.imported.manifest;
      for (const file of snapshot.files) {
        const key = `${current.imported.objectPrefix}${file.path}`;
        const data = Buffer.from(file.content, "utf8");
        // Was gerade dort steht, wird vorher gemerkt: bricht der Vorgang ab, ist nichts halb erledigt.
        written.push({ key, mime: file.mime, data: await readObject(key).catch(() => Buffer.alloc(0)) });
        await objectStore.putObject(env.S3_BUCKET, key, data, data.byteLength, { "content-type": file.mime });
        manifest = manifest.map((entry) => (entry.path === file.path ? { ...entry, size: data.byteLength } : entry));
        restored.push(file.path);
      }
      const revision = current.revision + 1;
      await tx.update(projectImports).set({ manifest, totalBytes: manifest.reduce((sum, file) => sum + file.size, 0) }).where(eq(projectImports.projectId, projectId));
      await tx.update(projects).set({ revision, updatedAt: new Date() }).where(eq(projects.id, projectId));
      await tx.delete(designSnapshots).where(eq(designSnapshots.id, snapshot.id));
      await tx.insert(auditEvents).values({ id: uuidv7(), organizationId: actor.organizationId, actorId: actor.userId, action: "design.undo", targetType: "project", targetId: projectId, result: "success", metadata: { files: restored, label: snapshot.label, revision }, correlationId: request.id });
      return { revision, label: snapshot.label };
    });
    request.log.info({ event: "design.undo", projectId, files: restored.length, revision: result.revision }, "KI-Änderung zurückgenommen");
    return { ...result, restoredFiles: restored };
  } catch (error) {
    await Promise.allSettled(written.filter((entry) => entry.data.byteLength).map((entry) => objectStore.putObject(env.S3_BUCKET, entry.key, entry.data, entry.data.byteLength, { "content-type": entry.mime })));
    throw error;
  }
});

// Wie viel Eingabe vertraegt das gewaehlte Modell wirklich? Danach richtet sich, wie viele
// Projektdateien mitgegeben werden — statt eines festen Limits, das grosse Designs abgewiesen hat.
async function chatContextBudget(organizationId: string, selection: ModelSelection): Promise<{ inputChars: number; contextLength?: number }> {
  if (selection.provider === "openai-codex") return { inputChars: 900_000 };
  const model = selection.provider === "openrouter"
    ? openRouterSettings((await validOpenRouterConnection(organizationId)).settings).models.find((entry) => entry.id === selection.model)
    : (await zenModelsForConnection(await validZenConnection(organizationId))).find((entry) => entry.id === selection.model);
  const output = reservedOutputTokens(model?.contextLength, model?.provider === "openrouter" ? model.endpoint?.maxCompletionTokens : model?.maxOutputTokens);
  const providerInputLimit = model?.provider === "opencode-zen" && model.inputTokenLimit ? model.inputTokenLimit * 3 : Number.POSITIVE_INFINITY;
  return { inputChars: Math.min(1_200_000, inputCharBudget(model?.contextLength, output), providerInputLimit), ...(model?.contextLength ? { contextLength: model.contextLength } : {}) };
}

// Bis zu drei Runden je Anfrage: Runde 1 setzt um, jede weitere raeumt nach, was nicht gegriffen hat,
// oder setzt eine von der Ausgabegrenze abgeschnittene Antwort fort. Die Schleife endet, sobald es
// nichts mehr nachzuarbeiten gibt oder eine Runde keinen Fortschritt mehr bringt.
const maxChatRounds = 3;
// Eine neue Runde wird nur BEGONNEN, solange noch Zeit ist. Sonst könnten drei langsame Läufe die
// Anfrage über jede vernünftige Wartezeit hinaus offen halten; angefangene Arbeit ist ohnehin schon
// geschrieben, und der nächste Zuruf macht dort weiter.
const chatRoundDeadlineMs = 300_000;
const chatBodySchema = z.object({
  message: z.string().min(1).max(8000),
  screen: z.string().max(200).optional(),
  provider: z.enum(providerIds).optional(),
  model: z.string().min(1).max(300).optional(),
  effort: z.string().min(1).max(20).optional(),
  // Ohne Verlauf begann jede Nachricht bei null: stellte die KI eine Rueckfrage, kannte sie die
  // eigene Frage beim naechsten Mal nicht mehr. Der Verlauf macht aus Einzelschuessen ein Gespraech.
  history: z.array(z.object({ role: z.enum(["user", "assistant"]), text: z.string().max(6000) }).strict()).max(24).optional(),
  // Der markierte Bereich aus der Vorschau: sein woertlicher Ausschnitt macht den Aenderungswunsch
  // eindeutig, ohne dass die KI raten oder nachfragen muss.
  target: z.object({ selector: z.string().max(600), html: z.string().max(8000), label: z.string().max(300).optional(), screenName: z.string().max(200).optional() }).strict().optional()
}).strict();
// Ein Lauf dauert je nach Modell eine knappe bis zwei Minuten. Ohne Zwischenmeldungen sieht man in
// dieser Zeit NICHTS und weiss nicht, ob überhaupt gearbeitet wird. Die Meldungen dieses Rückrufs
// gehen live in das Gespräch; im JSON-Betrieb läuft er ins Leere.
type ChatProgress = (event: string, data: Record<string, unknown>) => void;
async function runChatPipeline(request: FastifyRequest, actor: Actor, projectId: string, body: z.infer<typeof chatBodySchema>, emit: ChatProgress, signal?: AbortSignal) {
  const chatStartedAt = Date.now();
  const { message, screen, target, provider, model, effort, history } = body;
  if (Boolean(provider) !== Boolean(model)) fail("MODEL_SELECTION_INCOMPLETE", 400, "Provider und Modell müssen gemeinsam ausgewählt werden.");
  const row = (await db.select({ imported: projectImports, revision: projects.revision }).from(projectImports).innerJoin(projects, eq(projects.id, projectImports.projectId)).where(and(eq(projectImports.projectId, projectId), eq(projectImports.organizationId, actor.organizationId))).limit(1))[0];
  if (!row) fail("CHAT_NOT_SUPPORTED", 400, "Die KI-Bearbeitung ist aktuell für importierte HTML-Projekte verfügbar.");
  const selection = await resolveModelSelection(actor.organizationId, provider && model ? { provider, model, ...(effort ? { effort } : {}) } : undefined);
  const budget = await chatContextBudget(actor.organizationId, selection);
  const settings = { provider: selection.provider, model: selection.model, ...(selection.effort ? { effort: selection.effort } : {}) };
  const scope = target ? "marked" as const : globalScope(message) ? "global" as const : screen ? "screen" as const : "global" as const;
  const instructions = buildChatInstructions(scope);
  // Bewusst KEIN Gespraechsverlauf im Auftrag: frueher wanderten damit auch alte Fehlermeldungen und
  // Statuszeilen in den Prompt und lenkten das Modell von der eigentlichen Aufgabe ab. Der Zustand des
  // Designs steht in den Dateien — die Aufgabe steht fuer sich allein.
  void history;
  const conversation: Array<{ role: "user" | "assistant"; text: string }> = [];
  const markedRegion = target
    ? `\n\n=== MARKIERTER BEREICH (genau hier anwenden) ===\nBildschirm: ${target.screenName ?? screen ?? "unbekannt"}\nElement: ${target.label ?? target.selector}\nCSS-Pfad: ${target.selector}\nWörtlicher Ausschnitt aus der Datei:\n${target.html}`
    : "";
  const probes: ChatRoundProbe[] = [];
  const changedFiles = new Set<string>();
  // Auf welchen Bildschirmen sich wirklich etwas TUT — das ist die Auskunft, die man sehen will.
  const sichtbareScreens = new Set<string>();
  const naechsteSchritte: string[] = [];
  let letzteWirkungsmeldung = "";
  const notes: string[] = [];
  let replyText = "";
  let revision = row.revision;
  let briefing: string | undefined;
  let round = 0;
  emit("start", { scope, provider: selection.provider, model: selection.model, ...(selection.effort ? { effort: selection.effort } : {}), maxRounds: maxChatRounds, screen: screen ?? null, marked: Boolean(target) });
  for (; round < maxChatRounds; round += 1) {
    // Jede Runde liest den FRISCHEN Stand: die Edits der vorherigen Runde stehen schon in den Dateien,
    // und das Modell soll gegen das arbeiten, was jetzt wirklich dort steht.
    const currentImport = (await db.select({ imported: projectImports }).from(projectImports).where(and(eq(projectImports.projectId, projectId), eq(projectImports.organizationId, actor.organizationId))).limit(1))[0];
    if (!currentImport) fail("IMPORT_NOT_FOUND", 404, "Für dieses Projekt liegt kein Import vor.");
    const { manifest, objectPrefix, entryPath } = currentImport.imported;
    const overhead = instructions.length + message.length + markedRegion.length + (briefing?.length ?? 0) + conversation.reduce((sum, entry) => sum + entry.text.length, 0) + 8_000;
    const { selected, omitted } = selectChatFiles(manifest.filter((file) => chatEditableMime(file.mime)), entryPath, Math.max(40_000, budget.inputChars - overhead));
    if (!selected.length) fail("CHAT_NO_TEXT_FILES", 400, "Dieses Projekt enthält keine bearbeitbaren Textdateien.");
    const texts = await mapWithConcurrency(selected, 8, async (file) => ({ path: file.path, content: (await readObject(`${objectPrefix}${file.path}`)).toString("utf8") }));
    const entryText = texts.find((text) => text.path === entryPath);
    const entryHtml = entryText?.content ?? "";
    // Passt das Design als Ganzes nicht in das Fenster des gewählten Modells, wird nicht abgebrochen:
    // Stile und Skripte bleiben vollständig, von den Bildschirmen bleibt der gefragte stehen. So kann
    // auch ein kleines Modell weiterarbeiten — Textstellen-Edits treffen die echte Datei trotzdem.
    let excerptNote = "";
    const entryBudget = Math.max(40_000, budget.inputChars - overhead - selected.filter((file) => file.path !== entryPath).reduce((sum, file) => sum + file.size, 0));
    if (entryText && entryHtml.length > entryBudget) {
      const { text, omittedScreens } = excerptDesign(entryHtml, screen ? [screen, target?.screenName ?? ""] : []);
      if (omittedScreens.length && text.length < entryHtml.length) {
        entryText.content = text;
        excerptNote = `=== HINWEIS ZUM AUSSCHNITT ===\nVon der Startdatei siehst du die Stile, den Kopf und die Skripte VOLLSTÄNDIG, aber nur einen Teil der Bildschirm-Abschnitte. Ausgelassen sind: ${omittedScreens.join(", ")}. Zitiere in "find" nur Text, den du hier wirklich siehst. Eine Änderung an einer gemeinsam genutzten CSS-Regel wirkt trotzdem auf allen Bildschirmen — sie ist bei einem Wunsch für das gesamte Design der richtige Weg.`;
        request.log.info({ event: "chat.excerpt", projectId, omittedScreens: omittedScreens.length, fromChars: entryHtml.length, toChars: text.length }, "Design für das Kontextfenster gekürzt");
      }
    }
    const input = [
      conversation.length ? `=== BISHERIGES GESPRÄCH ===\n${conversation.map((entry) => `${entry.role === "user" ? "Benutzer" : "Du"}: ${entry.text.trim()}`).join("\n")}` : "",
      `=== AUFGABE ===\n${message}${markedRegion}`,
      entryHtml ? `=== DESIGN-LANDKARTE ===\n${designMap(entryHtml, entryPath, screen)}` : "",
      entryHtml ? `=== DESIGN-ANALYSE (gemessen, verbindlich) ===\n${designBriefing(entryHtml)}` : "",
      excerptNote,
      briefing ? `=== RÜCKMELDUNG AUS DEINEM LETZTEN VERSUCH (bitte beheben) ===\n${briefing}` : "",
      omitted.length ? `=== NICHT MITGELIEFERT (passen nicht in dein Kontextfenster; ändere sie nicht) ===\n${omitted.map((file) => file.path).join(", ")}` : "",
      `=== PROJEKTDATEIEN ===\n${texts.map((text) => `--- ${text.path} ---\n${text.content}`).join("\n\n")}`
    ].filter(Boolean).join("\n\n");
    emit("denkt", { round: round + 1, files: selected.length, inputChars: input.length, screens: designScreens(entryHtml).length, omitted: omitted.length, excerpt: Boolean(excerptNote) });
    let result: ModelRunResult;
    try {
      result = await modelRun(actor.organizationId, instructions, input, request.log, { selection, operation: `chat.runde.${round + 1}`, ...(signal ? { signal } : {}) });
    } catch (error) {
      // Fällt eine NACHARBEITSRUNDE aus, ist die Arbeit der vorherigen Runden längst geschrieben.
      // Sie mit einer Ausnahme wegzuwerfen, wäre der schlimmste aller Ausgänge: der Benutzer sähe
      // eine reine Fehlermeldung, obwohl sein Design bereits verändert wurde.
      if (!changedFiles.size) throw error;
      notes.push(`Die Nacharbeit konnte nicht abgeschlossen werden: ${error instanceof Error ? error.message : "unbekannter Fehler"} Das bereits Übernommene ist gespeichert.`);
      request.log.warn({ err: error, event: "chat.round.failed", projectId, round: round + 1, changedFiles: changedFiles.size }, "Nacharbeitsrunde fehlgeschlagen; Teilstand bleibt erhalten");
      break;
    }
    if (result.servedBy && result.attempts > 1) notes.push(`Der gewählte Anbieter war nicht erreichbar — der Lauf wurde von „${result.servedBy}“ bedient.`);
    emit("uebernimmt", { round: round + 1, outputChars: result.outputChars, durationMs: result.durationMs, ...(result.servedBy ? { servedBy: result.servedBy } : {}) });
    const parsed = parseChatResponse(result.text);
    const truncated = parsed.truncated || result.truncated;
    // Die Zusammenfassung der ERSTEN Runde beschreibt die eigentliche Arbeit; eine Nacharbeitsrunde
    // würde sie sonst durch ihr eigenes, viel kleineres Fazit ersetzen.
    if (parsed.reply && !replyText) replyText = parsed.reply;
    if (parsed.naechste.length && !naechsteSchritte.length) naechsteSchritte.push(...parsed.naechste);
    if (!parsed.changes.length && !parsed.files.length) {
      if (!replyText) replyText = result.text.trim().slice(0, 4000);
      if (truncated) notes.push("Die Antwort des Modells wurde von seiner Ausgabegrenze abgeschnitten, bevor eine verwertbare Änderung darin stand. Ein Modell mit größerem Ausgabefenster oder ein kleinerer Änderungswunsch hilft hier.");
      probes.push({ round: round + 1, provider: result.provider, model: result.model, durationMs: result.durationMs, outputChars: result.outputChars, truncated, salvaged: parsed.salvaged, editsApplied: 0, editsFailed: 0, filesWritten: 0 });
      break;
    }
    const applyResult = await applyChatWrites(actor, projectId, request.id, parsed, settings, message.trim().slice(0, 160));
    revision = applyResult.revision;
    for (const path of applyResult.applied) changedFiles.add(path);
    const outcomes = applyResult.reports.flatMap((report) => report.outcomes);
    probes.push({
      round: round + 1, provider: result.provider, model: result.model, durationMs: result.durationMs, outputChars: result.outputChars, truncated, salvaged: parsed.salvaged,
      editsApplied: outcomes.filter((outcome) => outcome.status === "applied").length,
      editsFailed: outcomes.filter((outcome) => outcome.status === "not-found" || outcome.status === "ambiguous").length,
      filesWritten: applyResult.applied.length
    });
    request.log.info({ event: "chat.round.completed", projectId, scope, ...probes.at(-1) }, "KI-Bearbeitungsrunde abgeschlossen");
    emit("runde", { ...probes.at(-1), changedFiles: applyResult.applied.length });
    // Die entscheidende Frage ist nicht „wurde Text ersetzt", sondern „ist im Design etwas ANDERS".
    // Genau hier scheiterten Laeufe, die dreissig Aenderungen meldeten und nichts bewirkten: die
    // geaenderte Regel verlor die Kaskade gegen einen bildschirmweiten Reset.
    const wirkungen = applyResult.htmlChanges.map((change) => summariseEffect(change.before, change.after));
    const wirksam = wirkungen.reduce((summe, wirkung) => summe + wirkung.wirksam, 0);
    const wirkungslos = wirkungen.reduce((summe, wirkung) => summe + wirkung.tot, 0);
    for (const name of wirkungen.flatMap((wirkung) => wirkung.screens)) sichtbareScreens.add(name);
    emit("wirkung", { round: round + 1, wirksam, wirkungslos, screens: [...sichtbareScreens] });
    letzteWirkungsmeldung = wirkungen.map((wirkung) => wirkung.hinweis).filter(Boolean).join(" ");
    briefing = [repairBriefing(applyResult.reports, truncated), ...wirkungen.map((wirkung) => wirkung.briefing).filter(Boolean)].filter(Boolean).join("\n\n") || undefined;
    if (!briefing) break;
    emit("nacharbeit", { round: round + 2, offen: applyResult.reports.flatMap((report) => report.outcomes).filter((outcome) => outcome.status === "not-found" || outcome.status === "ambiguous").length, truncated });
    // Kein Fortschritt mehr: eine weitere Runde mit derselben Rückmeldung wäre nur teurer Leerlauf.
    if (!applyResult.applied.length && round > 0) {
      notes.push(...applyResult.reports.flatMap((report) => [...report.issues.map((issue) => `${report.path}: ${issue}`), ...report.outcomes.filter((outcome) => outcome.status === "not-found").map((outcome) => `${report.path}: Die Textstelle „${outcome.find.replace(/\s+/g, " ").slice(0, 70)}…“ war nicht auffindbar.`)]).slice(0, 8));
      break;
    }
    if (round + 1 === maxChatRounds) { notes.push(`Nach ${maxChatRounds} Runden blieben Restpunkte offen. Schick die Nachricht einfach nochmal ab, dann wird dort weitergearbeitet.`); continue; }
    if (Date.now() - chatStartedAt > chatRoundDeadlineMs) { notes.push("Die Nacharbeit wurde nach der vereinbarten Wartezeit beendet. Das bisher Geänderte ist gespeichert — schick die Nachricht nochmal ab, um weiterzumachen."); break; }
  }
  if (letzteWirkungsmeldung) notes.unshift(letzteWirkungsmeldung);
  const totalApplied = probes.reduce((sum, probe) => sum + probe.editsApplied, 0);
  request.log.info({ event: "chat.completed", projectId, scope, rounds: probes.length, changedFiles: changedFiles.size, editsApplied: totalApplied, durationMs: Date.now() - chatStartedAt, ...settings }, "KI-Bearbeitung abgeschlossen");
  return {
    reply: replyText || (changedFiles.size ? `Änderungen umgesetzt (${changedFiles.size} Datei(en)).` : "Es war keine Dateiänderung nötig."),
    changedFiles: [...changedFiles],
    screens: [...sichtbareScreens],
    naechste: naechsteSchritte,
    skipped: notes,
    revision,
    rounds: probes.length,
    editsApplied: totalApplied,
    durationMs: Date.now() - chatStartedAt
  };
}
app.post("/api/v1/projects/:projectId/chat", { config: { rateLimit: { max: 30, timeWindow: "5 minutes" } } }, async (request, reply) => {
  const actor = requireActorPermission(request, "design.edit");
  const { projectId } = z.object({ projectId: z.string().uuid() }).parse(request.params);
  const body = chatBodySchema.parse(request.body);
  // Wer den Fortschritt sehen will, fragt einen Ereignisstrom an. Wer das nicht tut (Skripte, ältere
  // Oberflächen), bekommt unverändert die eine JSON-Antwort am Ende.
  if (!String(request.headers.accept ?? "").includes("text/event-stream")) return runChatPipeline(request, actor, projectId, body, () => {});
  reply.hijack();
  const stream = reply.raw;
  stream.writeHead(200, { "content-type": "text/event-stream; charset=utf-8", "cache-control": "no-cache, no-transform", connection: "keep-alive", "x-accel-buffering": "no", "x-correlation-id": request.id });
  const send = (event: string, data: unknown) => { if (!stream.writableEnded) stream.write(`event: ${event}\ndata: ${JSON.stringify(data)}\n\n`); };
  // Ein Zeichen alle fünf Sekunden: es hält Proxys davon ab, die stille Verbindung zu kappen, und
  // beweist der Oberfläche, dass der Lauf noch lebt.
  const heartbeat = setInterval(() => { if (!stream.writableEnded) stream.write(`: puls ${Date.now()}\n\n`); }, 5_000);
  // Bricht der Benutzer ab (Fenster zu, Knopf „Abbrechen"), wird auch der laufende Modellaufruf
  // beendet — statt ihn im Hintergrund weiterlaufen und Geld kosten zu lassen.
  const controller = new AbortController();
  request.raw.on("close", () => { if (!stream.writableEnded) controller.abort(Object.assign(new Error("Der Lauf wurde abgebrochen."), { code: "CHAT_ABORTED" })); });
  try {
    send("done", await runChatPipeline(request, actor, projectId, body, send, controller.signal));
  } catch (error) {
    const details = error && typeof error === "object" ? error as Record<string, unknown> : {};
    const aborted = controller.signal.aborted;
    if (!aborted) request.log.error({ err: error, projectId }, "KI-Bearbeitung fehlgeschlagen");
    send("error", { code: typeof details.code === "string" ? details.code : "INTERNAL_ERROR", message: aborted ? "Der Lauf wurde abgebrochen. Bereits übernommene Änderungen bleiben erhalten." : error instanceof Error && typeof details.statusCode === "number" && details.statusCode < 500 ? error.message : "Die Anfrage konnte nicht abgeschlossen werden.", retryable: details.retryable === true });
  } finally {
    clearInterval(heartbeat);
    if (!stream.writableEnded) stream.end();
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
  // Was von diesem Lauf gesichert ist. Scheitert er, entscheidet genau dieser Wert darueber, ob dem
  // Benutzer „an der letzten Stelle weitermachen" angeboten wird statt nur „alles neu".
  checkpoint?: CheckpointSummary;
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
// Der Bildschirm-Deckel schuetzt vor endlosen Laeufen — bei 24 fielen aber echte Bildschirme grosser
// Apps still weg, und das Design war im Download nur zur Haelfte da. Jeder Bildschirm ist ein eigener,
// nebenlaeufiger Aufruf; 60 bleiben tragbar, und was darueber liegt, benennt der Export ausdruecklich.
const maxFactFileBytes = 2 * 1024 * 1024, maxFactFiles = 6_000, factReadConcurrency = 12, maxReconstructedScreens = 60;
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
    // Jeder Bildschirm entsteht in einem EIGENEN Aufruf, der die anderen nicht sieht. Ohne diese
    // Auflage erfindet jeder Aufruf die wiederkehrenden Leisten neu — mit anderer Hoehe, anderen
    // Symbolen und anderen Schriftgroessen. Im Studio wirkt die App dann bei jedem Reiterwechsel wie
    // eine andere Anwendung, obwohl alle Bildschirme aus derselben Beschreibung stammen.
    "WIEDERKEHRENDE LEISTEN SIND AUF ALLEN BILDSCHIRMEN IDENTISCH. Reiterleiste, Kopfleiste, Navigationsleiste und jedes andere Element, das die App auf mehreren Bildschirmen zeigt, werden ZEICHENGLEICH aufgebaut: dieselbe Höhe, dieselben Innenabstände, dieselben Symbole in derselben Größe, dieselben Schriftgrößen und -gewichte, derselbe Hintergrund, dieselben Radien und Effekte. Unterscheiden darf sich AUSSCHLIESSLICH, welcher Eintrag als aktiv markiert ist.",
    "Steht die Leiste in den Originalquellen dieses Bildschirms, wird sie von dort ABGESCHRIEBEN — nicht nach der Beschreibung neu entworfen. Wähle für ihre Maße niemals einen eigenen Wert, wenn ein gemessener vorliegt.",
    "Original-Icons aus der Icon-Bibliothek inline als <svg> einsetzen; niemals Ersatzsymbole, Emoji oder Fremd-Icons. Bitmap-Assets als /<exakter Manifestpfad> referenzieren.",
    effectGuidance,
    `Verlinke Navigationsziele über data-werft-navigate="ZIEL-ID"; gültige IDs sind: ${screenIds.join("; ") || "keine"}.`,
    "JEDES Bedienelement, das im Original einen anderen Bildschirm öffnet, MUSS data-werft-navigate tragen — auch Zurück-Pfeile, Listeneinträge, Karten, Kacheln, Symbole und Leisten-Einträge. Ohne diese Verknüpfungen ist die Rekonstruktion nicht durchklickbar.",
    // Aus diesem Aufbau wird spaeter wieder ein Spec und daraus echte Software. Ein Bedienelement,
    // das weder Ziel noch beschriebene Aufgabe traegt, laesst sich dort nicht zuordnen und wird zum
    // toten Knopf. Steht die Aufgabe im Funktions-Spec, gehoert ihre Kennung ans Element.
    "Bedienelemente, die keinen Bildschirm öffnen, sondern etwas TUN, tragen `data-werft-funktion`: steht die Aufgabe als `F-<Nummer>` in den gelieferten Fakten oder im Spec, trage genau diese Kennung ein (data-werft-funktion=\"F-07\"); sonst einen kurzen Satz, was das Element tun soll.",
    // Die Bewegungs-Kennung steckt im Namen der Animation (`m-04-atmen`). Erfindet der Aufbau eigene
    // Namen, geht sie verloren, und der Ruecklauf kann eine Bewegung nicht mehr derselben M-Kennung
    // zuordnen wie im Erst-Spec — die Kette der Kennungen reisst genau hier.
    "Bewegungen: Steht unter den Effekten eine `animation: <name> …`, verwende GENAU diesen Namen und lege dazu einen passenden `@keyframes <name>`-Block an. Erfinde keine eigenen Animationsnamen — der Name trägt die Kennung der Bewegung (z. B. `m-04-atmen`) und muss erhalten bleiben. Dauer, Kurve und Wiederholung werden exakt übernommen.",
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
// Ein Spec nennt die Bildschirmdatei mitunter abgekuerzt (`bildschirme/hell/…-heute.html`), weil die
// laufende Nummer erst beim Export entsteht. Ohne Platzhalter traf so ein Eintrag keine Datei — der
// Aufbau bekam sein fertiges Original nie zu sehen und musste den Bildschirm frei nacherfinden.
function screenSourceMatcher(entry: string): RegExp {
  const escaped = entry.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  return new RegExp(`(?:^|/)${escaped.replace(/…|\\\.\\\.\\\./g, "[^/]*")}(?:\\.xml)?$`, "i");
}

async function readScreenSources(objectPrefix: string, manifest: ImportManifestFile[], wanted: string[]): Promise<string> {
  if (!wanted.length) return "";
  // In der Reihenfolge der Wunschliste, nicht in der des Archivs: reicht das Zeichenbudget nicht fuer
  // alles, wird zuerst gelesen, was diesen einen Bildschirm wirklich beschreibt.
  const files: ImportManifestFile[] = [];
  const gesehen = new Set<string>();
  for (const entry of wanted) {
    const matcher = screenSourceMatcher(entry);
    for (const file of manifest) if (!gesehen.has(file.path) && matcher.test(file.path)) { gesehen.add(file.path); files.push(file); }
  }
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

type ReconstructionModelRunner = (operation: string, instructions: string, input: string, remainingCompactionCalls: number, newlyPlannedCalls: number) => Promise<ModelRunResult>;
const compactionInstructions = "Verdichte die folgenden UI-Evidenzprotokolle verlustfrei. Behalte alle exakten Maße, Koordinaten, Abstände, Farben, Typografie-, Theme-, Asset-, Hierarchie- und Zustandsangaben mit ihren Quellpfaden sowie die Zuordnung zu Bildschirmen. Entferne nur Wiederholungen. Maximal 18000 Zeichen.";
// Die Verdichtungsgruppen einer Runde sind voneinander unabhaengig und laufen deshalb nebenlaeufig.
async function compactReconstructionEvidence(summaries: string[], run: ReconstructionModelRunner): Promise<string[]> {
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
async function runReconstructionJob(jobId: string, runAttempt: number, actor: Actor, projectId: string, correlationId: string, selection: ModelSelection, previousProbes: ReconstructionProbe[] = [], targetViewport?: { width: number; height: number; device: string }, resume = true) {
  const jobStartedAt = Date.now();
  const startedAt = new Date(jobStartedAt).toISOString();
  const probes: ReconstructionProbe[] = previousProbes.map((probe) => ({ ...probe }));
  const timingSamples: ReconstructionTimingSample[] = [];
  let completedOperations = 0, totalOperations = 0, retryCount = 0, latestProgress = 0;
  let savedCheckpoint: CheckpointSummary = { analyses: 0, screens: 0, hasEvidence: false };
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
    checkpoint: { ...savedCheckpoint },
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
  const runModelStep = async ({ operation, phase, kind, instructions, input, progress, remainingWeight, state, sourceFiles, sourceBytes, validate, showEta }: {
    operation: string;
    phase: string;
    kind: ReconstructionOperationKind;
    instructions: string;
    input: string;
    progress: number;
    remainingWeight: number;
    state: () => ReconstructionState;
    validate?: (result: ModelRunResult) => void;
    showEta?: boolean;
    sourceFiles?: number;
    sourceBytes?: number;
  }): Promise<ModelRunResult> => {
    const operationStartedAt = Date.now();
    const controller = new AbortController();
    let attempts = 0, settled = false, failed = false, failure: unknown, result: ModelRunResult | undefined;
    let lastHeartbeatLogAt = 0;
    const probe: ReconstructionProbe = { runAttempt, operation, phase, status: "running", startedAt: new Date(operationStartedAt).toISOString(), durationMs: 0, attempts: 0, inputChars: input.length, ...(sourceFiles === undefined ? {} : { sourceFiles }), ...(sourceBytes === undefined ? {} : { sourceBytes }) };
    probes.push(probe);
    const initialTiming = reconstructionTiming(timingSamples, kind, 0, remainingWeight);
    await publish("running", progress, runtimeState(state(), initialTiming.phaseProgress, showEta === false ? null : initialTiming.estimatedRemainingMs, operation));
    const tracked = modelRun(actor.organizationId, instructions, input, app.log, {
      operation,
      jobId,
      selection,
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
    // Der Zwischenstand haengt an GENAU dieser Ausgangslage. Neuer Import, anderes Modell oder ein
    // anderes Zielformat ergeben einen anderen Schluessel — dann wird nichts wiederverwendet.
    const checkpointScope: CheckpointScope = { projectId, revision: row.revision, selectionHash: hashJson(selection).slice(0, 16), viewportKey: targetViewport ? `${targetViewport.width}x${targetViewport.height}` : "basis" };
    const checkpointAt = checkpointPrefix(checkpointScope);
    // Ein ausdruecklicher Neuaufbau soll wirklich neu bauen: der alte Stand wird verworfen, nicht
    // stillschweigend weiterverwendet.
    if (!resume) await removeCheckpoint(checkpointAt);
    const checkpoint = resume ? await loadCheckpointParts(checkpointAt) : emptyCheckpoint;
    savedCheckpoint = checkpointSummary(checkpoint);
    const resumedAnalyses = new Map(resumableAnalyses(checkpoint).map((text, index) => [index + 1, text] as const));
    const resumedEvidence = checkpoint.evidence?.trim() ? checkpoint.evidence : undefined;
    const rememberAnalysis = async (batchNumber: number, text: string) => { if (await saveCheckpointPart(analysisKey(checkpointAt, batchNumber), text)) savedCheckpoint = { ...savedCheckpoint, analyses: savedCheckpoint.analyses + 1 }; };
    // Ein nachgemessener Bildschirm ueberschreibt seinen eigenen Zwischenstand — gezaehlt wird er
    // trotzdem nur einmal, sonst meldete die Anzeige mehr fertige Bildschirme als es gibt.
    const savedScreenIds = new Set(checkpoint.screens.map((screen) => screen.id));
    const rememberScreen = async (screenId: string, fragment: { markup: string; css: string }) => {
      if (!await saveCheckpointPart(screenKey(checkpointAt, screenId), JSON.stringify({ id: screenId, markup: fragment.markup, css: fragment.css }))) return;
      savedScreenIds.add(screenId);
      savedCheckpoint = { ...savedCheckpoint, screens: savedScreenIds.size };
    };
    if (checkpointIsUseful(savedCheckpoint)) app.log.info({ event: "reconstruction.checkpoint.resumed", jobId, projectId, ...savedCheckpoint }, "Zwischenstand des letzten Laufs wird wiederverwendet");
    if (skippedSources.length) app.log.warn({ event: "reconstruction.analysis_capped", jobId, projectId, analyzed: sources.length, skipped: skippedSources.length, skippedExamples: skippedSources.slice(0, 10).map((file) => file.path) }, "Analysebudget erreicht; weitere UI-Quellen gehen nur über den bildschirmweisen Aufbau ein");
    app.log.info({ event: "reconstruction.started", jobId, projectId, platform, uiSourceFiles: uiSources.length, analyzedFiles: sources.length, sourceBytes: totalBytes, estimatedAnalysisCalls, totalManifestFiles: row.imported.manifest.length }, "Design-Rekonstruktion gestartet");
    await publishMilestone(4, reconstructionState("inventory", `${row.imported.manifest.length} Projektdateien inventarisiert; ${uiSources.length} davon beschreiben Oberfläche.${checkpointIsUseful(savedCheckpoint) ? ` ${describeCheckpoint(savedCheckpoint)}` : ""}`, 1, 0, sources.length, 0, totalBytes), estimatedAnalysisCalls + 5);

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
        // Bereits ausgewertete Pakete werden NICHT erneut bezahlt. Sie stammen aus demselben
        // Quellstand und derselben Paketnummer, sind also Wort fuer Wort dasselbe Ergebnis.
        const reused = resumedAnalyses.get(batch.number);
        if (reused) return reused;
        const input = `Zielplattform: ${platform}; Referenzgerät: ${profile.device}; logischer Viewport: ${profile.width}x${profile.height}; Dichte: ${profile.density}.\nDies ist Analysepaket ${batch.number}.\n\n${factSheet}\n\n=== QUELLTEXT DIESES PAKETS ===\n${batch.text}`;
        const { text } = await runModelStep({ operation: `UI-Analysepaket ${batch.number}`, phase: "analyze", kind: "analysis", instructions: reconstructionAnalysisInstructions, input, progress: 10 + ratioBefore * 50, remainingWeight: 5, showEta: false, sourceFiles: batch.completedFiles, sourceBytes: batch.completedBytes, state: () => reconstructionState("analyze", `${window.length} UI-Analysepakete werden gleichzeitig ausgewertet; ${processedFiles} von ${sources.length} Dateien sind abgeschlossen.`, 2, processedFiles, sources.length, processedBytes, totalBytes) });
        const summary = text.trim();
        await rememberAnalysis(batch.number, summary);
        return summary;
      });
      summaries.push(...texts);
      for (const batch of window) { processedBytes += batch.completedBytes; processedFiles += batch.completedFiles; }
      const ratio = totalBytes ? processedBytes / totalBytes : 1;
      await publishMilestone(10 + ratio * 50, reconstructionState("analyze", `${processedFiles} von ${sources.length} UI-Dateien gründlich ausgewertet${skippedSources.length ? `; ${skippedSources.length} weitere gehen direkt in den bildschirmweisen Aufbau ein` : ""}.`, ratio >= 1 ? 3 : 2, processedFiles, sources.length, processedBytes, totalBytes), 5, 100, false);
    };
    // Liegt die fertig verdichtete Evidenz schon vor, ist die gesamte Analysephase erledigt: sie
    // entsteht erst NACH dem letzten Analysepaket und stammt damit aus genau diesen Quellen.
    // Dann werden weder Pakete gelesen noch Modelle befragt — der Lauf steigt direkt beim Aufbau ein.
    if (!resumedEvidence) {
      for await (const batch of buildSourceBatches(sources, async (file) => objectStore.getObject(env.S3_BUCKET, `${row.imported.objectPrefix}${file.path}`))) {
        batchNumber += 1;
        pendingBatches.push({ ...batch, number: batchNumber });
        if (pendingBatches.length >= reconstructionConcurrency) await flushBatches();
      }
      await flushBatches();
      const binaryAssets = row.imported.manifest.filter((file) => !sources.some((source) => source.path === file.path));
      for (let index = 0; index < binaryAssets.length; index += 1500) summaries.push(`ASSET-INVENTAR:\n${binaryAssets.slice(index, index + 1500).map((file) => `${file.path} | ${file.mime} | ${file.size} Bytes`).join("\n")}`);
    } else {
      processedFiles = sources.length;
      processedBytes = totalBytes;
    }
    await publishMilestone(62, reconstructionState("resolve", resumedEvidence ? "Die verdichtete UI-Evidenz des letzten Laufs wird übernommen." : "Themes, Assets und plattformspezifische Geometrie werden zusammengeführt.", 3, processedFiles, sources.length, processedBytes, totalBytes), 5, 0);
    const evidence = resumedEvidence ?? (await compactReconstructionEvidence(summaries, async (operation, instructions, input, remainingCompactionCalls, newlyPlannedCalls) => {
      totalOperations += newlyPlannedCalls;
      return runModelStep({ operation, phase: "resolve", kind: "compaction", instructions, input, progress: 62, remainingWeight: remainingCompactionCalls * 0.8 + 5, state: () => reconstructionState("resolve", "Exakte UI-Evidenz wird verlustfrei verdichtet.", 3, processedFiles, sources.length, processedBytes, totalBytes) });
    })).join("\n\n");
    if (!resumedEvidence && evidence.trim() && await saveCheckpointPart(evidenceKey(checkpointAt), evidence)) savedCheckpoint = { ...savedCheckpoint, hasEvidence: true };

    // Schritt 3: jeder Bildschirm wird EINZELN und nebenlaeufig gebaut. Vorher entstand die ganze
    // App in einem Aufruf — der lief in die Ausgabegrenze, liess Screens weg und rundete Werte.
    const allScreens = orderedScreens(facts);
    const screenPlan = screenPlanFrom({ ...facts, screens: allScreens }, row.name).slice(0, maxReconstructedScreens);
    if (allScreens.length > screenPlan.length) app.log.warn({ event: "reconstruction.screens_capped", jobId, projectId, found: allScreens.length, built: screenPlan.length, skipped: allScreens.slice(screenPlan.length).map((screen) => screen.id) }, "Mehr Bildschirme gefunden als aufgebaut werden");
    totalOperations = completedOperations + screenPlan.length + 1;
    const screenIndex = new Map(screenPlan.map((screen, index) => [screen.id, index] as const));
    const screenInstructions = buildScreenInstructions(platform, profile, screenPlan.map((screen) => `${screen.id} = „${screen.name}“`));
    // Fertige Bildschirme aus dem letzten Lauf sind der groesste Einzelposten: jeder ist ein
    // vollstaendiger, teurer KI-Aufruf, und sie haengen nicht voneinander ab.
    const resumedScreens = resumableScreens(checkpoint, screenPlan.map((screen) => screen.id));
    const reusableScreens = [...resumedScreens.keys()].length;
    if (reusableScreens) app.log.info({ event: "reconstruction.checkpoint.screens_reused", jobId, projectId, reused: reusableScreens, planned: screenPlan.length }, "Bereits aufgebaute Bildschirme werden übernommen");
    await publishMilestone(66, reconstructionState("build", reusableScreens ? `${describeCheckpoint(savedCheckpoint, screenPlan.length)} Es fehlen noch ${screenPlan.length - reusableScreens} Bildschirm(e).` : `${screenPlan.length} Bildschirm(e) werden einzeln und originalgetreu aufgebaut.`, 4, processedFiles, sources.length, processedBytes, totalBytes), screenPlan.length + 1 - reusableScreens, 0);
    let builtScreens = reusableScreens;
    const fragments = await mapWithConcurrency(screenPlan, reconstructionConcurrency, async (screen, index) => {
      const reused = resumedScreens.get(screen.id);
      if (reused) return { screen, markup: reused.markup, css: reused.css };
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
      const result = await runModelStep({
        operation: `Bildschirm „${screen.name}“ aufbauen`, phase: "build", kind: "build", instructions: screenInstructions, input,
        progress: 66 + (builtScreens / screenPlan.length) * 22, remainingWeight: 1,
        state: () => reconstructionState("build", `${builtScreens} von ${screenPlan.length} Bildschirmen originalgetreu aufgebaut.`, 4, processedFiles, sources.length, processedBytes, totalBytes),
        validate: (value) => { if (!extractScreenFragment(value.text).markup) fail("RECONSTRUCT_INVALID", 502, `Für den Bildschirm „${screen.name}“ kam kein verwertbares Markup zurück.`, true); }
      });
      builtScreens += 1;
      const fragment = extractScreenFragment(result.text);
      // Sofort sichern: bricht der Anbieter beim naechsten Bildschirm weg, ist DIESER hier gerettet.
      await rememberScreen(screen.id, fragment);
      return { screen, markup: fragment.markup, css: fragment.css };
    });

    // Schritt 4: nachmessen statt nur nachfragen. Das Ergebnis wird gegen die geparsten Quellwerte
    // geprueft; nur die tatsaechlich abweichenden Punkte gehen in einen gezielten Korrekturlauf.
    const composeAll = (parts: typeof fragments) => composeScreens(parts.map(({ screen, markup }) => ({ id: screen.id, name: screen.name, markup, isStart: screen.isStart, navigatesTo: screen.navigatesTo })), { title: row.name, platform, width: profile.width, height: profile.height, device: profile.device, density: profile.density, facts, sharedCss: parts.map((part) => part.css).filter(Boolean).join("\n") });
    let html = composeAll(fragments);
    let report = checkFidelity(html, facts);
    // Die Nachmessung prueft jeden Bildschirm gegen die QUELLEN — sie kann nicht sehen, dass die
    // Bildschirme UNTEREINANDER auseinanderlaufen. Genau das passiert aber, weil jeder in einem
    // eigenen Aufruf entsteht: die Reiterleiste faellt auf jedem Bildschirm etwas anders aus. Dieser
    // zweite, programmatische Blick vergleicht sie miteinander und schickt die Abweichungen in
    // denselben Korrekturlauf.
    const shellScreensOf = (parts: typeof fragments) => parts.map(({ screen, markup }) => ({ id: screen.id, name: screen.name, markup, isStart: screen.isStart }));
    const sharedCssOf = (parts: typeof fragments) => parts.map((part) => part.css).filter(Boolean).join("\n");
    const measureShell = (parts: typeof fragments) => checkShellConsistency(shellScreensOf(parts), sharedCssOf(parts));
    let shellIssues = measureShell(fragments);
    const shellReferenceName = shellReference(shellScreensOf(fragments), sharedCssOf(fragments))?.name ?? "";
    if (shellIssues.length) app.log.info({ event: "reconstruction.shell_drift", jobId, projectId, issues: shellIssues.length, screens: new Set(shellIssues.map((issue) => issue.screenId)).size }, "Wiederkehrende Leisten laufen auseinander");
    app.log.info({ event: "reconstruction.fidelity", jobId, projectId, round: 0, score: report.score, checked: report.checked, matched: report.matched, issues: report.issues.length }, "Fidelity gemessen");
    await publishMilestone(88, reconstructionState("verify", `Nachmessung: ${report.score} % der gemessenen Quellwerte stimmen exakt (${report.matched} von ${report.checked})${shellIssues.length ? `; ${shellIssues.length} Abweichung(en) an den wiederkehrenden Leisten` : ""}.`, 5, processedFiles, sources.length, processedBytes, totalBytes), 1, 0);
    const shellByScreen = new Map<string, ShellIssue[]>();
    for (const issue of shellIssues) shellByScreen.set(issue.screenId, [...(shellByScreen.get(issue.screenId) ?? []), issue]);
    if (!fidelityAcceptable(report) || shellIssues.length) {
      // Nur Bildschirme nachbessern, die wirklich betroffen sind — und jeder bekommt AUSSCHLIESSLICH
      // die Abweichungen aus seinen eigenen Quelldateien. Sonst baut Bildschirm B Werte ein, die zu
      // Bildschirm A gehoeren, und die Rekonstruktion wird schlechter statt besser.
      const affected = fragments.filter((fragment) => hasIssuesForSources(report, fragment.screen.files) || shellByScreen.has(fragment.screen.id));
      totalOperations += affected.length;
      let correctedScreens = 0;
      app.log.info({ event: "reconstruction.repair_planned", jobId, projectId, affected: affected.length, total: fragments.length, issues: report.issues.length, shellIssues: shellIssues.length }, "Gezielter Korrekturlauf geplant");
      const repairedByScreen = new Map((await mapWithConcurrency(affected, reconstructionConcurrency, async (fragment) => {
        const corrections = renderFidelityInstructions(report, { sources: fragment.screen.files });
        const shellCorrections = renderShellInstructions(shellByScreen.get(fragment.screen.id) ?? [], shellReferenceName);
        const result = await runModelStep({
          operation: `Bildschirm „${fragment.screen.name}“ nachmessen`, phase: "verify", kind: "verification",
          instructions: fidelityRepairInstructions, input: [`Bildschirm: „${fragment.screen.name}“ (id=${fragment.screen.id}).`, factSheet, `\n# GEMESSENE ABWEICHUNGEN DIESES BILDSCHIRMS\n${corrections}`, shellCorrections ? `\n# ABWEICHUNGEN DER WIEDERKEHRENDEN LEISTE (gegen „${shellReferenceName}“ gemessen)\n${shellCorrections}` : "", `\n# ZU KORRIGIERENDES MARKUP DIESES BILDSCHIRMS\n<style>${fragment.css}</style>\n${fragment.markup}`].filter(Boolean).join("\n"),
          progress: 88 + (correctedScreens / Math.max(1, affected.length)) * 8, remainingWeight: 0,
          state: () => reconstructionState("verify", `${correctedScreens} von ${affected.length} betroffenen Bildschirmen gegen die gemessenen Quellwerte korrigiert.`, 5, processedFiles, sources.length, processedBytes, totalBytes),
          validate: (value) => { if (!extractScreenFragment(value.text).markup) fail("RECONSTRUCT_INVALID", 502, `Die Nachmessung von „${fragment.screen.name}“ lieferte kein verwertbares Markup.`, true); }
        });
        correctedScreens += 1;
        const repairedFragment = extractScreenFragment(result.text);
        const corrected = { markup: repairedFragment.markup, css: repairedFragment.css || fragment.css };
        // Auch die Nachmessung wird gesichert, sonst ginge sie bei einem Abbruch kurz vor dem
        // Speichern verloren und muesste komplett neu bezahlt werden.
        await rememberScreen(fragment.screen.id, corrected);
        return [fragment.screen.id, { screen: fragment.screen, ...corrected }] as const;
      })));
      const repaired = fragments.map((fragment) => repairedByScreen.get(fragment.screen.id) ?? fragment);
      const repairedHtml = composeAll(repaired);
      const repairedReport = checkFidelity(repairedHtml, facts);
      const repairedShell = measureShell(repaired);
      app.log.info({ event: "reconstruction.fidelity", jobId, projectId, round: 1, score: repairedReport.score, before: report.score, issues: repairedReport.issues.length, shellIssues: repairedShell.length, shellBefore: shellIssues.length }, "Fidelity nach Korrekturlauf gemessen");
      // Nur uebernehmen, wenn die Korrektur messbar besser ist — sonst bleibt der bessere Stand.
      // Beides zaehlt: die Treue zu den Quellen UND die Einheitlichkeit der Bildschirme untereinander.
      // Ein Lauf, der die Leisten angleicht, aber die gemessenen Werte verschlechtert, wird verworfen.
      if (repairedReport.score >= report.score && repairedShell.length <= shellIssues.length) { html = repairedHtml; report = repairedReport; shellIssues = repairedShell; }
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
        await tx.insert(auditEvents).values({ id: uuidv7(), organizationId: actor.organizationId, actorId: actor.userId, action: "design.reconstructed", targetType: "project", targetId: projectId, result: "success", metadata: { sourceFiles: sources.length, sourceBytes: totalBytes, outputBytes: data.byteLength, revision, platform, profile, selection, measuredValues, screens: screenPlan.length, fidelityScore, openIssues: report.issues.length, elapsedMs: Date.now() - jobStartedAt, retryCount, probes }, correlationId });
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
    // Erst wenn das Design nachweislich gespeichert ist, wird der Zwischenstand weggeraeumt — vorher
    // waere er die einzige Rettung, falls das Speichern doch noch scheitert.
    await removeCheckpoint(checkpointAt);
    savedCheckpoint = { analyses: 0, screens: 0, hasEvidence: false };
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
    // Der Benutzer soll am Fehler ablesen koennen, dass seine bisherige Arbeit NICHT verloren ist.
    const resumeHint = checkpointIsUseful(savedCheckpoint) ? ` ${describeCheckpoint(savedCheckpoint)} Der Lauf kann an dieser Stelle fortgesetzt werden.` : "";
    const failedState = runtimeState({ ...latestState, phase: "failed", message: `${message}${resumeHint}` }, null, null, latestState.currentOperation);
    let failurePersisted = false;
    for (let persistAttempt = 1; persistAttempt <= 3 && !failurePersisted; persistAttempt += 1) {
      try { await publish("failed", Math.min(99, latestProgress), failedState, code); failurePersisted = true; }
      catch (persistError) {
        app.log.error({ err: persistError, jobId, projectId, runAttempt, persistAttempt }, "Fehlerstatus der Rekonstruktion konnte nicht gespeichert werden");
        if (persistAttempt < 3) await new Promise((resolve) => setTimeout(resolve, persistAttempt * 1_000));
      }
    }
    try {
      await db.insert(auditEvents).values({ id: uuidv7(), organizationId: actor.organizationId, actorId: actor.userId, action: "design.reconstruction.failed", targetType: "project", targetId: projectId, result: "failure", metadata: { code, message, selection, elapsedMs: Date.now() - jobStartedAt, retryCount, probes }, correlationId });
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
  // `resume` ist der Normalfall: ein abgebrochener Lauf setzt an seiner letzten gesicherten Stelle
  // auf. Nur der ausdrueckliche Neuaufbau (`force`) verwirft den Zwischenstand.
  const { retryFailed, force, resume, viewport } = z.object({
    retryFailed: z.boolean().optional().default(false),
    force: z.boolean().optional().default(false),
    resume: z.boolean().optional().default(true),
    viewport: z.object({ width: z.number().int().min(240).max(4096), height: z.number().int().min(240).max(4096), device: z.string().min(1).max(80) }).optional()
  }).strict().parse(request.body ?? {});
  const resumeFromCheckpoint = resume && !force;
  const imported = (await db.select({ revision: projects.revision }).from(projectImports).innerJoin(projects, eq(projects.id, projectImports.projectId)).where(and(eq(projectImports.projectId, projectId), eq(projectImports.organizationId, actor.organizationId))).limit(1))[0];
  if (!imported) fail("IMPORT_NOT_FOUND", 404, "Für dieses Projekt liegt kein Import vor.");
  const selection = await resolveModelSelection(actor.organizationId);
  // Das Format gehoert in den Idempotenzschluessel: sonst gaebe der Aufbau fuer ein zweites Format
  // den Lauf des ersten zurueck, und die zweite Fassung entstuende nie.
  const idempotencyKey = `${projectId}:${imported.revision}:${viewport ? `${viewport.width}x${viewport.height}` : "basis"}:${hashJson(selection).slice(0, 16)}`;
  const queuedState = reconstructionState("queued", "HTML-Rekonstruktion wird vorbereitet.", 0, 0, 0, 0, 0);
  const candidateId = uuidv7();
  const inserted = await db.insert(jobs).values({ id: candidateId, organizationId: actor.organizationId, projectId, kind: "design-reconstruction", status: "queued", progress: 0, idempotencyKey, input: { projectId, revision: imported.revision, selection, ...(viewport ? { viewport } : {}) }, result: queuedState, attempts: 1, heartbeatAt: new Date() }).onConflictDoNothing().returning({ id: jobs.id });
  if (inserted[0]) {
    setTimeout(() => { void runReconstructionJob(candidateId, 1, actor, projectId, request.id, selection, [], viewport, resumeFromCheckpoint).catch((error) => app.log.error({ err: error, jobId: candidateId, projectId }, "Unbehandelter Rekonstruktionsfehler")); }, 0);
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
  if (claimed[0]) setTimeout(() => { void runReconstructionJob(existing.id, claimed[0]!.attempts, actor, projectId, request.id, selection, previousProbes, viewport, resumeFromCheckpoint).catch((error) => app.log.error({ err: error, jobId: existing.id, projectId }, "Unbehandelter Rekonstruktionsfehler")); }, 0);
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

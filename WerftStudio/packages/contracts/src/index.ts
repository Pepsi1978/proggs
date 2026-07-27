import { z } from "zod";

export const idSchema = z.string().uuid();
export const revisionSchema = z.number().int().nonnegative();
export const platformSchema = z.enum(["web", "android", "ios", "ipados", "macos", "windows", "custom"]);
export const projectTypeSchema = z.enum(["prototype", "presentation", "document", "template", "canvas"]);
export const fidelitySchema = z.enum(["wireframe", "high_fidelity"]);

export const boundsSchema = z.object({
  x: z.number().finite(),
  y: z.number().finite(),
  width: z.number().positive().max(100_000),
  height: z.number().positive().max(100_000)
}).strict();

const nodeBase = z.object({
  id: idSchema,
  name: z.string().min(1).max(160),
  parentId: idSchema.nullable(),
  childIds: z.array(idSchema).max(10_000),
  bounds: boundsSchema,
  visible: z.boolean(),
  locked: z.boolean(),
  tokenBindings: z.record(z.string(), z.string()).default({}),
  semantics: z.object({
    role: z.string().max(80).optional(),
    label: z.string().max(500).optional(),
    altText: z.string().max(2_000).optional(),
    focusOrder: z.number().int().optional()
  }).strict().default({})
});

export const designNodeSchema = z.discriminatedUnion("type", [
  nodeBase.extend({
    type: z.literal("container"),
    layout: z.enum(["row", "column", "grid", "stack", "absolute"]),
    gap: z.number().nonnegative().max(10_000),
    padding: z.tuple([z.number(), z.number(), z.number(), z.number()]),
    fill: z.string().max(120).optional(),
    radius: z.number().nonnegative().max(10_000).optional()
  }).strict(),
  nodeBase.extend({
    type: z.literal("text"),
    text: z.string().max(100_000),
    fontFamily: z.string().max(160),
    fontSize: z.number().positive().max(1_000),
    fontWeight: z.number().int().min(100).max(1_000),
    lineHeight: z.number().positive().max(10),
    color: z.string().max(120)
  }).strict(),
  nodeBase.extend({
    type: z.literal("image"),
    assetId: idSchema,
    fit: z.enum(["contain", "cover", "fill"])
  }).strict(),
  nodeBase.extend({
    type: z.literal("control"),
    control: z.enum(["button", "input", "list", "table", "chart", "platform"]),
    variant: z.string().max(160),
    state: z.string().max(160)
  }).strict()
]);

export const frameSchema = z.object({
  id: idSchema,
  pageId: idSchema,
  name: z.string().min(1).max(160),
  platform: platformSchema,
  device: z.string().min(1).max(160),
  width: z.number().int().positive().max(10_000),
  height: z.number().int().positive().max(10_000),
  theme: z.string().min(1).max(80),
  locale: z.string().min(2).max(35),
  rootNodeId: idSchema,
  canvasX: z.number().finite(),
  canvasY: z.number().finite()
}).strict();

export const designDocumentSchema = z.object({
  schemaVersion: z.literal(1),
  projectId: idSchema,
  projectType: projectTypeSchema,
  fidelity: fidelitySchema,
  platforms: z.array(platformSchema).min(1),
  designSystemVersionId: idSchema.nullable(),
  themes: z.array(z.object({ id: z.string(), name: z.string(), tokens: z.record(z.string(), z.unknown()) }).strict()),
  pages: z.array(z.object({ id: idSchema, name: z.string(), type: z.enum(["screen", "slide", "document-page", "canvas"]), frameIds: z.array(idSchema) }).strict()),
  frames: z.array(frameSchema).max(1_000),
  nodes: z.array(designNodeSchema).max(100_000),
  assets: z.array(z.object({ id: idSchema, hash: z.string().length(64), mime: z.string(), altText: z.string().optional() }).strict()),
  interactions: z.array(z.object({ id: idSchema, sourceNodeId: idSchema, trigger: z.string(), action: z.string(), targetId: idSchema.optional() }).strict()),
  metadata: z.object({ createdAt: z.string().datetime(), compilerVersion: z.string() }).strict()
}).strict();

export const designOperationSchema = z.discriminatedUnion("type", [
  z.object({ type: z.literal("add_node"), node: designNodeSchema }).strict(),
  z.object({ type: z.literal("update_node"), nodeId: idSchema, patch: z.record(z.string(), z.unknown()) }).strict(),
  z.object({ type: z.literal("remove_node"), nodeId: idSchema }).strict(),
  z.object({ type: z.literal("update_frame"), frameId: idSchema, patch: z.record(z.string(), z.unknown()) }).strict(),
  z.object({ type: z.literal("set_token"), themeId: z.string(), token: z.string(), value: z.unknown() }).strict()
]);

export const applyOperationsSchema = z.object({
  actionId: idSchema,
  baseRevision: revisionSchema,
  operations: z.array(designOperationSchema).min(1).max(200)
}).strict();

export const createProjectSchema = z.object({
  name: z.string().trim().min(1).max(120),
  type: projectTypeSchema,
  fidelity: fidelitySchema,
  platforms: z.array(platformSchema).min(1),
  prompt: z.string().max(50_000).default(""),
  designSystemVersionId: idSchema.nullable().default(null),
  aiProfile: z.enum(["economy", "standard", "quality", "local", "custom"]),
  viewport: z.object({
    width: z.number().int().min(240).max(4096),
    height: z.number().int().min(240).max(4096),
    device: z.string().min(1).max(80)
  }).strict().optional()
}).strict().superRefine((input, context) => {
  if (input.viewport && !input.platforms.includes("android")) context.addIssue({ code: "custom", path: ["viewport"], message: "Ein Android-Viewport erfordert die Plattform Android." });
});

export const apiErrorSchema = z.object({
  code: z.string(),
  message: z.string(),
  retryable: z.boolean(),
  correlationId: z.string(),
  fieldErrors: z.record(z.string(), z.array(z.string())).optional()
}).strict();

export const realtimeEventSchema = z.object({
  eventId: idSchema,
  projectId: idSchema,
  sequence: z.number().int().nonnegative(),
  occurredAt: z.string().datetime(),
  type: z.enum(["draft.operation.applied", "draft.operation.conflict", "comment.created", "version.created", "run.updated", "preview.updated", "permission.changed"]),
  actorId: idSchema,
  payload: z.record(z.string(), z.unknown())
}).strict();

export type DesignDocument = z.infer<typeof designDocumentSchema>;
export type DesignNode = z.infer<typeof designNodeSchema>;
export type DesignOperation = z.infer<typeof designOperationSchema>;
export type ApplyOperations = z.infer<typeof applyOperationsSchema>;
export type CreateProject = z.infer<typeof createProjectSchema>;
export type RealtimeEvent = z.infer<typeof realtimeEventSchema>;

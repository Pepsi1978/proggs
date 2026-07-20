import type { z } from "zod";
export type ModelCapability = "text"|"vision"|"structured_output"|"tools"|"image"|"streaming";
export interface ModelRequest<T extends z.ZodType> { role: string; instructions: string; input: unknown[]; outputSchema: T; maxCostMicros: number; signal?: AbortSignal }
export interface ModelResult<T> { output: T; providerRequestId: string; inputUnits: number; outputUnits: number; costMicros: number }
export interface ModelProvider { id: string; capabilities: ReadonlySet<ModelCapability>; invoke<T extends z.ZodType>(request: ModelRequest<T>): Promise<ModelResult<z.infer<T>>> }
export class ModelGateway {
  constructor(private readonly routes: ReadonlyMap<string, readonly ModelProvider[]>) {}
  async invoke<T extends z.ZodType>(request: ModelRequest<T>): Promise<ModelResult<z.infer<T>>> {
    const providers = this.routes.get(request.role) ?? [];
    for (const provider of providers) {
      if (!provider.capabilities.has("structured_output")) continue;
      try { const result = await provider.invoke(request); return { ...result, output: request.outputSchema.parse(result.output) }; }
      catch (error) { if (request.signal?.aborted) throw error; }
    }
    throw Object.assign(new Error("Kein richtlinienkonformer Modellprovider verfügbar"), { code: "PROVIDER_UNAVAILABLE" });
  }
}

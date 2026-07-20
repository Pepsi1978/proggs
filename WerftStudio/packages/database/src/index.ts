import postgres from "postgres";
import { drizzle } from "drizzle-orm/postgres-js";
import * as schema from "./schema.js";

export function createDatabase(url = process.env.DATABASE_URL) {
  const resolvedUrl = url ?? (process.env.NODE_ENV === "production" ? undefined : "postgres://werft:werft@localhost:5432/werft");
  if (!resolvedUrl) throw new Error("DATABASE_URL fehlt");
  const client = postgres(resolvedUrl, { max: 10, idle_timeout: 20, connect_timeout: 10 });
  return { db: drizzle(client, { schema }), client };
}
export * from "./schema.js";

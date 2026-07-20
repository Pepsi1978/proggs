import { fileURLToPath } from "node:url";
import { migrate } from "drizzle-orm/postgres-js/migrator";
import { createDatabase } from "./index.js";
const { db, client } = createDatabase();
await migrate(db, { migrationsFolder: fileURLToPath(new URL("../migrations/", import.meta.url)) });
await client.end();

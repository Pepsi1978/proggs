ALTER TABLE "provider_connections" ADD COLUMN "is_default" boolean DEFAULT false NOT NULL;--> statement-breakpoint
UPDATE "provider_connections" SET "is_default" = true WHERE "id" IN (
	SELECT DISTINCT ON ("organization_id") "id" FROM "provider_connections"
	ORDER BY "organization_id", CASE WHEN "provider" = 'openai-codex' THEN 0 ELSE 1 END, "created_at"
);--> statement-breakpoint
CREATE UNIQUE INDEX "provider_connection_one_default" ON "provider_connections" USING btree ("organization_id") WHERE "provider_connections"."is_default" = true;

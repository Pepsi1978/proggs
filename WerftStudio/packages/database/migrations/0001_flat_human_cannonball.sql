CREATE TABLE "project_imports" (
	"project_id" uuid PRIMARY KEY NOT NULL,
	"organization_id" uuid NOT NULL,
	"format" text NOT NULL,
	"entry_path" text NOT NULL,
	"object_prefix" text NOT NULL,
	"manifest" jsonb NOT NULL,
	"file_count" integer NOT NULL,
	"total_bytes" bigint NOT NULL,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL
);
--> statement-breakpoint
ALTER TABLE "project_imports" ADD CONSTRAINT "project_imports_project_id_projects_id_fk" FOREIGN KEY ("project_id") REFERENCES "public"."projects"("id") ON DELETE no action ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "project_imports" ADD CONSTRAINT "project_imports_organization_id_organizations_id_fk" FOREIGN KEY ("organization_id") REFERENCES "public"."organizations"("id") ON DELETE no action ON UPDATE no action;--> statement-breakpoint
CREATE INDEX "project_imports_org_idx" ON "project_imports" USING btree ("organization_id","created_at");
export const roles = ["viewer", "commenter", "editor", "manager", "organization_admin"] as const;
export type Role = typeof roles[number];

export const permissions = [
  "project.read", "project.update", "project.archive", "project.delete", "project.share", "project.export", "project.download_source",
  "design.read", "design.edit", "design.publish", "run.create", "run.approve", "run.cancel", "run.read_sensitive_log",
  "comment.create", "comment.resolve", "comment.moderate", "version.create", "version.restore", "branch.manage",
  "design_system.create", "design_system.review", "design_system.publish", "provider.read", "provider.manage", "provider.use",
  "member.invite", "member.manage", "policy.manage", "audit.read"
] as const;
export type Permission = typeof permissions[number];

const matrix: Record<Role, ReadonlySet<Permission>> = {
  viewer: new Set(["project.read", "design.read"]),
  commenter: new Set(["project.read", "design.read", "comment.create", "comment.resolve"]),
  editor: new Set(["project.read", "project.update", "project.export", "project.download_source", "design.read", "design.edit", "run.create", "run.approve", "run.cancel", "comment.create", "comment.resolve", "version.create", "version.restore"]),
  manager: new Set(permissions.filter((permission) => !["provider.manage", "member.manage", "policy.manage", "audit.read"].includes(permission))),
  organization_admin: new Set(permissions)
};

export function can(role: Role, permission: Permission): boolean {
  return matrix[role].has(permission);
}

export function requirePermission(role: Role, permission: Permission): void {
  if (!can(role, permission)) throw Object.assign(new Error("Nicht erlaubt"), { code: "FORBIDDEN" });
}

import { describe, expect, it } from "vitest";
import { can } from "./index.js";
describe("Rollenmatrix", () => {
  it("verhindert Mutationen durch Betrachter", () => expect(can("viewer", "design.edit")).toBe(false));
  it("erlaubt Organisationsadmins Providerverwaltung", () => expect(can("organization_admin", "provider.manage")).toBe(true));
});

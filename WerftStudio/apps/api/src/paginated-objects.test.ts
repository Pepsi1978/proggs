import { describe, expect, it } from "vitest";
import { paginatedObjects, type ObjectListPage } from "./paginated-objects.js";

describe("paginated MinIO object listing", () => {
  it("uses bounded pages and follows continuation tokens without omissions", async () => {
    const calls: Array<{ token: string; maxKeys: number }> = [];
    const pages = new Map<string, ObjectListPage<{ name: string }>>([
      ["", { objects: [{ name: "a" }, { name: "b" }], isTruncated: true, nextContinuationToken: "next" }],
      ["next", { objects: [{ name: "c" }], isTruncated: false }]
    ]);
    const query = (token: string, maxKeys: number) => (async function* () {
      calls.push({ token, maxKeys });
      yield pages.get(token)!;
    })();
    const names: string[] = [];
    for await (const item of paginatedObjects(query)) names.push(item.name);
    expect(names).toEqual(["a", "b", "c"]);
    expect(calls).toEqual([{ token: "", maxKeys: 100 }, { token: "next", maxKeys: 100 }]);
  });

  it("rejects truncated pages without a continuation token", async () => {
    const query = () => (async function* () { yield { objects: [], isTruncated: true }; })();
    const consume = async () => { for await (const item of paginatedObjects(query)) void item; };
    await expect(consume()).rejects.toThrow("Fortsetzungstoken");
  });
});

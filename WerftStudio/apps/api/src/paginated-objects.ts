export type ObjectListPage<T> = {
  objects: T[];
  isTruncated: boolean;
  nextContinuationToken?: string;
};

export async function* paginatedObjects<T>(
  query: (continuationToken: string, maxKeys: number) => AsyncIterable<ObjectListPage<T>>,
  pageSize = 100
): AsyncGenerator<T> {
  let continuationToken = "";
  for (;;) {
    let page: ObjectListPage<T> | undefined;
    for await (const result of query(continuationToken, pageSize)) { page = result; break; }
    if (!page) return;
    for (const item of page.objects) yield item;
    if (!page.isTruncated) return;
    if (!page.nextContinuationToken) throw new Error("MinIO lieferte für eine abgeschnittene Objektseite keinen Fortsetzungstoken.");
    continuationToken = page.nextContinuationToken;
  }
}

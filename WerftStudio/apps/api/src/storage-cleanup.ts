export type StoredObject = { name?: string; size?: number; lastModified?: Date };

export async function cleanupOrphanedObjects(options: {
  knownObjects: ReadonlySet<string>;
  incompleteUploads: AsyncIterable<{ key: string }>;
  objects: AsyncIterable<StoredObject>;
  removeIncompleteUpload: (key: string) => Promise<void>;
  removeObject: (key: string) => Promise<void>;
  removeIncompleteUploads?: boolean;
  minimumAgeMs?: number;
  now?: number;
  // Zwischenstaende laufender oder abgebrochener Rekonstruktionen stehen in keinem Manifest und
  // saehen deshalb wie Muell aus. Wer sie wegraeumt, nimmt dem Benutzer genau die Arbeit weg, die
  // das Fortsetzen retten soll.
  protectedPrefixes?: readonly string[];
}) {
  let removedIncompleteUploads = 0;
  let removedObjects = 0;
  let freedBytes = 0;
  if (options.removeIncompleteUploads !== false) {
    for await (const upload of options.incompleteUploads) {
      await options.removeIncompleteUpload(upload.key);
      removedIncompleteUploads += 1;
    }
  }
  for await (const object of options.objects) {
    if (!object.name || options.knownObjects.has(object.name)) continue;
    if (options.protectedPrefixes?.some((prefix) => object.name!.startsWith(prefix))) continue;
    if (options.minimumAgeMs && (!object.lastModified || (options.now ?? Date.now()) - object.lastModified.getTime() < options.minimumAgeMs)) continue;
    await options.removeObject(object.name);
    removedObjects += 1;
    freedBytes += object.size ?? 0;
  }
  return { removedIncompleteUploads, removedObjects, freedBytes };
}

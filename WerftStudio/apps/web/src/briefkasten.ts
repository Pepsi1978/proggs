// Die Pipeline uebergibt in beide Richtungen ueber zwei Ordner auf dem Rechner des Benutzers:
// `Designs/Inbox/<App>-SPEC-v1.zip` geht in den Designer, `Designs/Outbox/<App>-SPEC-v2.zip` kommt
// zurueck. Werft laeuft aber auf dem Server und kommt an diese Ordner nicht heran, und ein
// gewoehnlicher Dateidialog laesst sich aus einer Webseite nicht auf einen Ordner vorbelegen — das
// verbietet der Browser. Der einzige Weg, der das leistet, ist der Ordnerzugriff von Chrome/Edge:
// der Benutzer gibt den Designs-Ordner EINMAL frei, der Browser merkt sich die Freigabe dauerhaft,
// und ab da liest und schreibt Werft direkt darin — ohne Dialog, ohne Verschieben, ohne Umbenennen.

type Erlaubnis = "granted" | "denied" | "prompt";
type VerzeichnisGriff = FileSystemDirectoryHandle & {
  queryPermission?(optionen: { mode: "read" | "readwrite" }): Promise<Erlaubnis>;
  requestPermission?(optionen: { mode: "read" | "readwrite" }): Promise<Erlaubnis>;
  values?(): AsyncIterableIterator<FileSystemHandle>;
};

declare global {
  interface Window {
    showDirectoryPicker?(optionen?: { id?: string; mode?: "read" | "readwrite" }): Promise<FileSystemDirectoryHandle>;
  }
}

export const briefkastenMoeglich = (): boolean => typeof window !== "undefined" && typeof window.showDirectoryPicker === "function";

// Ein Verzeichnis-Griff ueberlebt das Neuladen der Seite nur, wenn er gespeichert wird. Er ist kein
// JSON-Wert; IndexedDB kann ihn als strukturierte Kopie ablegen, localStorage nicht.
const datenbank = "werft-briefkasten";
const lager = "griffe";
const schluessel = "designs";

function oeffneDatenbank(): Promise<IDBDatabase> {
  return new Promise((loese, brich) => {
    const anfrage = indexedDB.open(datenbank, 1);
    anfrage.onupgradeneeded = () => { if (!anfrage.result.objectStoreNames.contains(lager)) anfrage.result.createObjectStore(lager); };
    anfrage.onsuccess = () => loese(anfrage.result);
    anfrage.onerror = () => brich(anfrage.error ?? new Error("IndexedDB nicht verfügbar"));
  });
}

async function ablegen(griff: FileSystemDirectoryHandle | undefined): Promise<void> {
  const db = await oeffneDatenbank();
  await new Promise<void>((loese, brich) => {
    const vorgang = db.transaction(lager, "readwrite");
    const speicher = vorgang.objectStore(lager);
    if (griff) speicher.put(griff, schluessel); else speicher.delete(schluessel);
    vorgang.oncomplete = () => loese();
    vorgang.onerror = () => brich(vorgang.error ?? new Error("Ordner konnte nicht gemerkt werden"));
  });
  db.close();
}

async function holen(): Promise<VerzeichnisGriff | undefined> {
  const db = await oeffneDatenbank();
  const griff = await new Promise<VerzeichnisGriff | undefined>((loese, brich) => {
    const anfrage = db.transaction(lager, "readonly").objectStore(lager).get(schluessel);
    anfrage.onsuccess = () => loese((anfrage.result as VerzeichnisGriff | undefined) ?? undefined);
    anfrage.onerror = () => brich(anfrage.error ?? new Error("Ordner nicht lesbar"));
  });
  db.close();
  return griff;
}

// Chrome behaelt die Freigabe, fragt aber nach einem Neustart des Browsers erneut nach. `nachfragen`
// steuert, ob das im Hintergrund still scheitern darf (beim Laden) oder ob der Benutzer gefragt
// werden soll (wenn er gerade etwas anklickt) — ungefragte Berechtigungsdialoge sind sonst Zufall.
async function erlaubt(griff: VerzeichnisGriff, nachfragen: boolean): Promise<boolean> {
  const stand = (await griff.queryPermission?.({ mode: "readwrite" })) ?? "granted";
  if (stand === "granted") return true;
  if (!nachfragen) return false;
  return ((await griff.requestPermission?.({ mode: "readwrite" })) ?? "denied") === "granted";
}

export async function designsOrdner(nachfragen = false): Promise<VerzeichnisGriff | undefined> {
  const griff = await holen().catch(() => undefined);
  if (!griff) return undefined;
  return (await erlaubt(griff, nachfragen)) ? griff : undefined;
}

export async function ordnerWaehlen(): Promise<VerzeichnisGriff | undefined> {
  if (!briefkastenMoeglich()) return undefined;
  const griff = (await window.showDirectoryPicker!({ id: "werft-designs", mode: "readwrite" })) as VerzeichnisGriff;
  await ablegen(griff);
  return griff;
}

export async function ordnerVergessen(): Promise<void> {
  await ablegen(undefined).catch(() => undefined);
}

export type InboxEintrag = { datei: string; bytes: number; geaendert: number };

export async function inboxListe(griff: VerzeichnisGriff): Promise<InboxEintrag[]> {
  const inbox = await griff.getDirectoryHandle("Inbox").catch(() => undefined);
  if (!inbox) return [];
  const eintraege: InboxEintrag[] = [];
  for await (const eintrag of (inbox as VerzeichnisGriff).values?.() ?? []) {
    if (eintrag.kind !== "file" || !/\.zip$/i.test(eintrag.name)) continue;
    const datei = await (eintrag as FileSystemFileHandle).getFile();
    eintraege.push({ datei: eintrag.name, bytes: datei.size, geaendert: datei.lastModified });
  }
  // Neueste zuerst: der Benutzer legt gerade ein Spec ab und will genau das sehen.
  return eintraege.sort((links, rechts) => rechts.geaendert - links.geaendert);
}

export async function inboxDatei(griff: VerzeichnisGriff, name: string): Promise<File> {
  const inbox = await griff.getDirectoryHandle("Inbox");
  return (await inbox.getFileHandle(name)).getFile();
}

export async function inOutboxSchreiben(griff: VerzeichnisGriff, name: string, inhalt: Blob): Promise<string> {
  const outbox = await griff.getDirectoryHandle("Outbox", { create: true });
  const datei = await outbox.getFileHandle(name, { create: true });
  const strom = await datei.createWritable();
  await strom.write(inhalt);
  await strom.close();
  return `${griff.name}/Outbox/${name}`;
}

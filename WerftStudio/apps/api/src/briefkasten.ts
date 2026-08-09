import { createWriteStream } from "node:fs";
import fs from "node:fs/promises";
import path from "node:path";

// Die Pipeline uebergibt in beide Richtungen als ZIP: `Designs/Inbox/<App>-SPEC-v1.zip` geht in den
// Designer, `Designs/Outbox/<App>-SPEC-v2.zip` kommt zurueck. Ein Browser kann weder den Startordner
// eines Dateidialogs setzen noch das Ziel eines Downloads bestimmen — beides muss deshalb der Server
// erledigen. Ohne gesetzte Ordner bleibt der Briefkasten schlicht aus; nichts anderes aendert sich.
export const briefkastenOrdner = {
  inbox: process.env.WERFT_INBOX_DIR?.trim() ?? "",
  outbox: process.env.WERFT_OUTBOX_DIR?.trim() ?? ""
};

export type BriefkastenEintrag = { datei: string; bytes: number; geaendert: string };

// Der Dateiname kommt aus der URL. Ein Pfadtrenner oder `..` darin wuerde aus dem Briefkasten
// ausbrechen — deshalb wird nur ein blosser Name mit .zip akzeptiert, nichts sonst.
export function sichererZipName(name: string): string {
  const wert = name.normalize("NFC");
  if (!wert || wert.length > 200 || wert !== path.basename(wert) || wert.startsWith(".") || !/\.zip$/i.test(wert)) {
    throw new Error("Ungültiger Dateiname für den Briefkasten.");
  }
  return wert;
}

export async function inboxListe(): Promise<BriefkastenEintrag[]> {
  if (!briefkastenOrdner.inbox) return [];
  const namen = await fs.readdir(briefkastenOrdner.inbox).catch(() => [] as string[]);
  const eintraege: BriefkastenEintrag[] = [];
  for (const name of namen.filter((eintrag) => /\.zip$/i.test(eintrag))) {
    const status = await fs.stat(path.join(briefkastenOrdner.inbox, name)).catch(() => undefined);
    if (status?.isFile()) eintraege.push({ datei: name, bytes: status.size, geaendert: status.mtime.toISOString() });
  }
  // Neueste zuerst: der Benutzer legt gerade eben ein Spec ab und will genau das sehen.
  return eintraege.sort((links, rechts) => rechts.geaendert.localeCompare(links.geaendert));
}

export async function inboxDatei(name: string): Promise<Buffer> {
  if (!briefkastenOrdner.inbox) throw new Error("Es ist kein Inbox-Ordner eingerichtet.");
  return fs.readFile(path.join(briefkastenOrdner.inbox, sichererZipName(name)));
}

export async function outboxZiel(name: string): Promise<string> {
  if (!briefkastenOrdner.outbox) throw new Error("Es ist kein Outbox-Ordner eingerichtet.");
  await fs.mkdir(briefkastenOrdner.outbox, { recursive: true });
  return path.join(briefkastenOrdner.outbox, sichererZipName(name));
}

export const outboxStrom = (ziel: string) => createWriteStream(ziel);

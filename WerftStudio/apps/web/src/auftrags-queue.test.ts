import { describe, expect, it } from "vitest";
import { istOffen, laufende, parallelGrenze, startbereit, warteschlangenText, wartende, type AuftragStatus } from "./auftrags-queue";

const auftraege = (...status: AuftragStatus[]) => status.map((wert, index) => ({ id: `A${index}`, status: wert }));

describe("startbereit", () => {
  it("startet bis zur Grenze und lässt den Rest warten", () => {
    const liste = auftraege("wartet", "wartet", "wartet", "wartet", "wartet", "wartet", "wartet");
    expect(startbereit(liste).map((auftrag) => auftrag.id)).toEqual(["A0", "A1", "A2", "A3", "A4"]);
  });

  it("rechnet bereits laufende Aufträge gegen die Grenze", () => {
    const liste = auftraege("läuft", "läuft", "läuft", "wartet", "wartet", "wartet");
    expect(startbereit(liste).map((auftrag) => auftrag.id)).toEqual(["A3", "A4"]);
  });

  it("startet nichts, wenn die Grenze erreicht ist", () => {
    expect(startbereit(auftraege("läuft", "läuft", "läuft", "läuft", "läuft", "wartet"))).toEqual([]);
  });

  it("zieht nach, sobald ein Auftrag fertig ist", () => {
    expect(startbereit(auftraege("angewendet", "läuft", "läuft", "läuft", "läuft", "wartet")).map((auftrag) => auftrag.id)).toEqual(["A5"]);
  });

  it("hält die Reihenfolge des Absendens ein", () => {
    expect(startbereit(auftraege("wartet", "wartet"), 1).map((auftrag) => auftrag.id)).toEqual(["A0"]);
  });

  it("ignoriert abgeschlossene und fehlgeschlagene Aufträge", () => {
    expect(startbereit(auftraege("angewendet", "fehlgeschlagen", "ohne Änderung", "abgebrochen"))).toEqual([]);
  });
});

describe("Zählungen", () => {
  it("zählt laufende und wartende getrennt", () => {
    const liste = auftraege("läuft", "läuft", "wartet", "angewendet");
    expect(laufende(liste)).toBe(2);
    expect(wartende(liste)).toBe(1);
  });

  it("erkennt offene Aufträge", () => {
    expect(istOffen("wartet")).toBe(true);
    expect(istOffen("läuft")).toBe(true);
    expect(istOffen("angewendet")).toBe(false);
    expect(istOffen("abgebrochen")).toBe(false);
  });

  it("erlaubt fünf gleichzeitig", () => {
    expect(parallelGrenze).toBe(5);
  });
});

describe("warteschlangenText", () => {
  it("nennt laufende und wartende Aufträge", () => {
    expect(warteschlangenText(auftraege("läuft", "läuft", "wartet"))).toBe("2 laufen · 1 wartet");
  });

  it("setzt die Einzahl richtig", () => {
    expect(warteschlangenText(auftraege("läuft"))).toBe("1 läuft");
  });

  it("bleibt leer, wenn nichts offen ist", () => {
    expect(warteschlangenText(auftraege("angewendet", "fehlgeschlagen"))).toBe("");
  });
});

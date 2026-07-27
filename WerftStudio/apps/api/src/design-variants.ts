// Ein Design kann fuer MEHRERE Geraeteformate aufgebaut sein: ein fuer 412 Punkte Breite gebauter
// Bildschirm nutzt die Flaeche eines aufgeklappten Foldables nicht — er stuende dort nur in einer
// Ecke. Jede Formatfassung liegt deshalb als eigene Datei neben der Grundfassung, und die Buehne
// zeigt die zum gewaehlten Geraet passende. `design.html` ist die Grundfassung,
// `design-928x1080.html` die Fassung fuer genau dieses Format.
export const generatedDesignPathPattern = /^\.werft-generated\/[0-9a-f-]+(?:\/\d+)?\/design(?:-\d+x\d+)?\.html$/i;
export const designVariantPathPattern = /^\.werft-generated\/[0-9a-f-]+(?:\/\d+)?\/design-(\d+)x(\d+)\.html$/i;

export type DesignViewport = { width: number; height: number };
export const designFileName = (viewport?: DesignViewport) => viewport ? `design-${viewport.width}x${viewport.height}.html` : "design.html";

// Beim Speichern wird NUR die gleichnamige Fassung ersetzt; die Fassungen der anderen Formate
// bleiben liegen. Ohne diese Trennung waere nach jedem Aufbau nur noch ein einziges Format da.
export const sameVariantPattern = (fileName: string) => new RegExp(`^\\.werft-generated/[0-9a-f-]+(?:/\\d+)?/${fileName.replace(".", "\\.")}$`, "i");

export type DesignVariantFile = { width: number; height: number; path: string };
export function designVariantsOf(paths: string[]): DesignVariantFile[] {
  return paths.flatMap((path) => {
    const match = designVariantPathPattern.exec(path);
    return match ? [{ width: Number(match[1]), height: Number(match[2]), path }] : [];
  });
}

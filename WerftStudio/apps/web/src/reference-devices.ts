// Referenzgeraete fuer die Buehne. Massgeblich ist NICHT die Pixelaufloesung, sondern die logische
// Flaeche in CSS-Pixeln (dp) — genau die steht einer App zum Zeichnen zur Verfuegung, und nur mit ihr
// sieht das Design so aus wie auf dem Geraet. Sie ist die Aufloesung geteilt durch die Anzeigedichte.
// Belegt: S23 Ultra 1440x3088 bei 3,75x = 384x824 (blisk.io); Galaxy Z Fold 7 1968x2184 bei 2,0x =
// 984x1092 (yesviz + offizielle Samsung-Emulator-Skins) — dieselbe Dichte tragen alle Fold-Innen-
// bildschirme. Die Aussenbildschirme folgen ihrer gemessenen Punktdichte (Fold 6 2,625x, Fold 8
// 2,75x; beide ergeben rund 160 dp je Zoll, wie es die Android-Definition vorsieht).
// Aufloesungen laut GSMArena; Galaxy Z Fold 8 vorgestellt am 22.07.2026.
export type ReferenceDevice = { id: string; name: string; state?: string; width: number; height: number; physicalWidthMm: number; physicalHeightMm: number; source: string };
export const referenceDevices: ReferenceDevice[] = [
  { id: "s23-ultra", name: "Galaxy S23 Ultra", width: 384, height: 824, physicalWidthMm: 73.2, physicalHeightMm: 156.9, source: "1440 × 3088 px · 3,75×" },
  { id: "fold8-cover", name: "Galaxy Z Fold 8", state: "zugeklappt", width: 454, height: 717, physicalWidthMm: 74.7, physicalHeightMm: 118.0, source: "1248 × 1972 px · 2,75×" },
  { id: "fold8-main", name: "Galaxy Z Fold 8", state: "aufgeklappt", width: 914, height: 1224, physicalWidthMm: 115.5, physicalHeightMm: 154.6, source: "1828 × 2448 px · 2,0×" },
  { id: "fold6-cover", name: "Galaxy Z Fold 6", state: "zugeklappt", width: 369, height: 905, physicalWidthMm: 60.0, physicalHeightMm: 147.2, source: "968 × 2376 px · 2,625×" },
  { id: "fold6-main", name: "Galaxy Z Fold 6", state: "aufgeklappt", width: 928, height: 1080, physicalWidthMm: 126.1, physicalHeightMm: 146.7, source: "1856 × 2160 px · 2,0×" },
];
export const androidStartDevices = referenceDevices.filter((device) => !device.state || device.state === "zugeklappt");
export const deviceLabel = (device: ReferenceDevice) => device.state ? `${device.name} · ${device.state}` : device.name;

// Quer heisst: das Geraet um 90 Grad gedreht. Aufgeklappt und gedreht sind zwei getrennte Achsen —
// beide Fold-Zustaende gibt es deshalb hoch UND quer.
export type StageDevice = { id: string; label: string; width: number; height: number; physicalWidthMm?: number; physicalHeightMm?: number } | null;
export function stageDeviceForDesign(width: number, height: number, landscape: boolean): StageDevice {
  const shortSide = Math.min(width, height);
  const longSide = Math.max(width, height);
  return {
    id: "design-size",
    label: `Größe des Designs · ${landscape ? "quer" : "hoch"}`,
    width: landscape ? longSide : shortSide,
    height: landscape ? shortSide : longSide
  };
}

export function stageDeviceFor(deviceId: string, landscape: boolean): StageDevice {
  const device = referenceDevices.find((entry) => entry.id === deviceId);
  if (!device) return null;
  return {
    id: device.id,
    label: `${deviceLabel(device)} · ${landscape ? "quer" : "hoch"}`,
    width: landscape ? device.height : device.width,
    height: landscape ? device.width : device.height,
    physicalWidthMm: landscape ? device.physicalHeightMm : device.physicalWidthMm,
    physicalHeightMm: landscape ? device.physicalWidthMm : device.physicalHeightMm
  };
}

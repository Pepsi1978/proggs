# macOS-Effort-Auswahl (macfix.2)

Der macOS-Patch hatte die Modell-API in den Typen und im TUI-Adapter,
aber nicht in `packages/opencode/src/plugin/tui/runtime.ts::pluginApi`.
Dieser Wrapper erstellt die API jedes Plugins neu. Ohne `model: api.model`
bekam die Seitenleiste keine Modell-API und blendete die Effort-Auswahl
bei allen Modellen aus. Ein Config-Eintrag allein behebt das nicht.

Die Weiterleitung ist identisch mit dem Windows-Patch. Die Stufen stammen
weiterhin aus `local.model.variant.list()`, und Auswahl sowie aktive Markierung
verwenden denselben nativen Zustand wie der Variantenwechsel per Tastatur.
Modelle ohne Varianten bleiben ohne Effort-Auswahl. Keine festen Modelllisten
oder nur optisch wirksamen Ersatzschalter in der Seitenleiste anlegen.

Verwandte Stellen: TuiModel-Typ, Adapter, Plugin-Wrapper und EffortSelector.
Der fehlende Wrapper war die Unterbrechung; Windows reicht die API bereits
weiter. macfix.2 kennzeichnet den korrigierten Build, damit macfix.1 nicht als
aktuell behandelt wird. Vorhandene macOS-Maus- und Zwischenablagefunktionen
bleiben unverändert.

Der gemeinsame Cache-Patch setzt inzwischen einen neueren Session-Processor
voraus. Für 1.18.23 wird deshalb ausschließlich sein Processor-Abschnitt durch
`opencode-1.18.23-cache-processor.patch` ersetzt. Die Metadaten-Zusammenführung
und Speicherung bleiben erhalten; der Windows-Patch wird nicht verändert.

Am 10.09.2026 neu gebaut und lokal installiert: OpenCode 1.18.30-macfix.2
(neueste stabile npm-Version bei der Installation) mit Sidebar 1.14.8.
Der Launcher-Zeiger verweist auf diesen Build. Die Astra-Varianten aus der
gemeinsamen Konfiguration sind auch lokal eingetragen.

Build und Installation im Schnellmodus:
`bash opencode-setup/build-install-macos-tuifix.sh --version 1.18.30 --skip-checks`

Typechecks und Regressionstests werden nur mit diesem ausdrücklichen Schalter
ausgelassen. Der Standardlauf behält sie bei. Die sichtbare Bedienprüfung
erfolgt durch den Benutzer in einer neu gestarteten OpenCode-TUI.

# Größere Umsetzungen begleiten

Diese Anleitung gilt innerhalb eines bereits beauftragten Dialogs. Sie verlangt weder zusätzliche Modellrunden noch eine neue Nutzerfreigabe für bereits autorisierte Arbeit.

## Vor der ersten großen Umsetzung

Leite aus den Nutzervorgaben eine kurze Abdeckungsliste ab: Varianten, betroffene Bildschirmbereiche, Hell/Dunkel, Bilder und ausdrücklich unveränderte Standardansicht. Halte gemeinsame Funktionen und bestehende Anzeige- oder Datumsregeln als zu bewahrende Eigenschaften fest. Keine zusätzlichen Varianten oder Anforderungen erfinden. Gib diese Liste als Kriterien des Zielvertrags ([Programmierloop](../../ins-terminal-einfuegen/references/programmier-loop.md)) im eigentlichen Umsetzungsauftrag mit; eine separate Planbestätigung ist nicht nötig.

Gleiche die Liste an tatsächlichen Datei-Meilensteinen mit Code und vorhandenen Vorlagen ab. Farben und Formen allein erfüllen keinen Auftrag für unterschiedliche Layouts. Änderungen an gemeinsam genutzten Bausteinen auch gegen die ausdrücklich unveränderte Standardvariante prüfen.

## Review bündeln

Prüfe nach der ersten zusammenhängenden Umsetzung am Review-Checkpoint risikobasiert gemäß [Kontingent sparsam nutzen](../../ins-terminal-einfuegen/references/kontingent-sparen.md): Mindestprüfung immer, Stichprobe oder vollständiger Diff einschließlich neuer Dateien nach den dortigen Auslösern. Bündele zusammengehörige Befunde in einer Rückmeldung: Datei/Funktion, konkreter Auslöser, Auswirkung und kleinster notwendiger Korrekturumfang. Vermeide nacheinander entdeckte Stilwünsche und kleinteilige Promptserien. Neue belegte Fehler innerhalb derselben Runde bleiben korrigierbar; erfinde keine Folgearbeit, wenn der Auftrag erfüllt ist.

## Gewünschten Advisor nachweisen

Wünscht der Nutzer einen bestimmten Advisor, übergib das an dieselbe Claude-Sitzung und prüfe tatsächlichen Aufruf und Ergebnis. Eine sichtbare Modellbezeichnung am Aufruf ist ein Beleg, auch wenn das Tool selbst keine Modellmetadaten zurückgibt. Fehlende Metadaten allein bedeuten nicht, dass der Advisor unbenutzt blieb. Ohne belastbaren Beleg die Unsicherheit benennen; kein stiller Modelltausch oder zusätzlicher Ersatzagent.

## Ergebnisse genau benennen

Unterscheide Vorlagen-/Assetprüfung, Codeprüfung, erfolgreichen Build, Installation und visuelle Geräteabnahme. Ein freigestelltes Bild auf hellem und dunklem Hintergrund zu betrachten belegt das Asset, nicht dessen tatsächliche Darstellung auf dem Handy. Ein Build belegt weder vollständige Designabdeckung noch Bedienbarkeit aller Geräteansichten. Berichte nur die tatsächlich vorhandenen Nachweise und ihre Grenzen; keine unbelegte pixelgenaue Übereinstimmung oder Kontingentersparnis.

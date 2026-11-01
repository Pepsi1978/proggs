# Sehr ausführliches Review VU5J6: Benachrichtigungen und Erinnerungsprofile

## Umfang

Statische Prüfung aller Änderungen aus `a42097d82` (1.8.549), ihrer Aufrufer und der Korrekturen dieser Sitzung: Meldungsarten und Profile, Einmalmigration, Benachrichtigungsbildschirm, Sprachkatalog, Wecktonwiedergabe, Erinnerungsplaner, Klingeldienst, Ersatzweg, Offline-/Systemstimme, Timer, Stoppuhr, Update-/Installationsansagen, Weckwort-Hinweise, Gemini-Schlüsselwechsel, Rückfragen und Agenten-Zusammenfassungen. Die beim Anlegen gespeicherte Erinnerungsform bleibt maßgeblich. Fremde Änderungen anderer Sitzungen sind nicht Teil des Reviews.

## Befunde und Korrekturen

| Nr. | Priorität | Konkreter Fehlerpfad | Korrektur |
|---|---|---|---|
| 1 | P2 | Ohne explizit gespeicherte Tondauer wechselte deren Standard beim An-/Abschalten von Vorlesen zwischen 10 und 0 Sekunden. Die beiden Optionen waren damit indirekt gekoppelt. | Stabiler, vom aktuellen Vorlese-Schalter unabhängiger Standard; gespeicherte Dauern bleiben erhalten. |
| 2 | P1 | Beginnt Live oder eine Aufnahme während der Tonphase oder Netzsynthese, konnte der Erinnerungswecker kurz dennoch zu sprechen beginnen. Auch generische Wecktöne berücksichtigten Aufnahmen nicht. | Gemeinsamer Fokuswächter, Prüfungen vor und nach der Synthese sowie vor jedem Text; nicht dringende Wecktöne warten bzw. verstummen. |
| 3 | P1 | Wird von mehreren klingelnden Erinnerungen die gerade gesprochene bestätigt, gelöscht oder geändert, lief ihr aus einer alten Liste entnommener Text weiter. | Laufende Liste im Planer aktualisieren; aktuelle Auslösung und Inhalt beim Sprechen überwachen und den betroffenen Sprachjob abbrechen. |
| 4 | P1 | Scheitert der Vordergrunddienst, endete der generische Ersatzweckton nach seiner kurzen Meldungsdauer. Besonders bei ausgeschaltetem Vorlesen blieb der unbestätigte Wecker anschließend still. | Derselbe dauerhafte Erinnerungs-Weckablauf im Dienst und Ersatzweg, bis zur Bestätigung; beide verwenden dasselbe getrennte Profil. |
| 5 | P1 | Bei Ton aus und Vorlesen an hält kein dauerhaft spielender MediaPlayer das Gerät wach. Die Wiederholung per Coroutine konnte im Tiefschlaf ausbleiben. | Begrenzte, während eines aktiven Weckers erneuerte Wachhaltung; Freigabe beim Abbruch und Ende. |
| 6 | P2 | Der ausgewählte Weckton wurde im Erinnerungs- bzw. Timerdienst synchron auf dem Hauptfaden vorbereitet; langsam lesbare Inhalte konnten die Bedienung blockieren. | Gemeinsame Vorbereitung auf IO; der Dienst zeigt vorher seine Pflichtmeldung. Beim Timer wird ein nach Abbruch fertig geladener Spieler freigegeben. |
| 7 | P1 | WeckerTon startete den asynchron geladenen Spieler vor der Prüfung, ob genau dieses Klingeln noch gültig war. Abbruch konnte deshalb einen späten kurzen Ton bzw. eine spät beginnende Vibration hinterlassen. | Start, Übernahme, Stopp und Vibration unter derselben Sperre; erst nach Gültigkeitsprüfung abspielen, veraltete Spieler nur freigeben. |
| 8 | P2 | Bereits laufende generische Wecktöne ignorierten nachträglich gewähltes Lautlos, geänderte Ausgabeform oder ein Abschalten der Meldung; anfangs gab es zusätzlich eine ungeprüfte Wartefrist. | Aktuelle Ton-/Form-/An-/Ruhezeitwahl laufend prüfen, auch während der Sichtbarkeitsfrist. Dauer mit monotoner Uhr statt verstellbarer Wanduhr. |
| 9 | P2 | Das pauschale Warten auf sämtliche Wecktöne derselben Art verzögerte eine andere, unabhängige Meldung mit. | Nur auf die eigene Meldungsnummer warten; Agenten-Zusammenfassung und Timer explizit zuordnen. |
| 10 | P2 | notify-Ausnahmen wurden geschluckt und zeige meldete dennoch Erfolg; anschließend konnte ein Weckton für die nicht veröffentlichte Meldung starten. | Ausnahme ins Logbuch, tatsächlichen Aufruferfolg zurückgeben und bei Ausnahme keinen Weckton starten. Android kann Meldungen trotz erfolgreichem API-Aufruf weiterhin aufgrund seiner Berechtigungen unterdrücken. |
| 11 | P2 | Ein beim Start als lange verpasst erkannter Timer kam im neuen gemeinsamen Ausgabeweg dennoch mit laut=true an. | Verpasste Timer nur still wiederherstellen, ohne Ansage. |
| 12 | P2 | Ein normaler Timer mit Ton aus und Vorlesen aus stoppte trotzdem eine gerade gesprochene Gesprächsantwort. | Gesprächsunterbrechung nur bei tatsächlich gewünschter akustischer Ausgabe. |
| 13 | P2 | Timer-Vorlesen wurde vor dem Posten und vor dem Start des zugehörigen Ersatzwecktons eingereiht. Die Warteprüfung konnte den noch nicht registrierten Ton übersehen. | Vorlesen erst nach der Meldung bzw. Dienstübergabe anstoßen, mit eigener Wecktonnummer. |
| 14 | P2 | Wechselte die Stimme während einer Netzsynthese, wurde der Dateiname erst danach aus der neuen Wahl gebildet; alter Ton konnte so als neue Stimme gespeichert werden. | Cache-Kennung vor der Netzarbeit bestimmen. |
| 15 | P2 | Vorbereiten, direkte Wiedergabe und Probe konnten dieselbe fehlende Sprachdatei parallel erzeugen; Bereinigung konnte gleichzeitig laufende Schreibarbeit löschen. | Gemeinsame Synthese-/Bereinigungssperre und erneute Cacheprüfung innerhalb der Sperre. |
| 16 | P2 | Die Systemstimme als Rückfallweg ignorierte das nun getrennt wählbare Tempo; bei sofortigem TTS-Fehler wartete sie bis zur Frist. | Tempo des jeweiligen Profils setzen und direkten speak-Fehler sofort beenden. Abbruch der Netzsynthese wird ausdrücklich weitergereicht. |
| 17 | P3 | Unter der eingeschobenen Überschrift Erinnerungen standen anschließend optisch auch Stoppuhr und Update-Ansagen. „In Android“ öffnete bei stiller Weckerform und beim Timer teilweise den falschen Kanal. | Eigene Erinnerungsgruppe mit genau zwei Profilkarten; Kanallink nach Ausgabeform und tatsächlichem Timerdienstkanal bestimmen. |
| 18 | P3 | Die Stimmen-Anbieteranzeige blieb nach einer Sprachänderung im remember-Zustand auf dem alten Anbieter. Die Erinnerungsansicht versprach bei Vorlesen aus weiter die Erzeugung einer Sprachdatei. | Anbieter direkt aus der gespeicherten Wahl ableiten; ausgeschaltetes Vorlesen verständlich anzeigen. |
| 19 | P2 | Bei erneutem Start einer schon laufenden Stoppuhr unterdrückte setOnlyAlertOnce(true) den neu eingestellten Hinweiston. | Nur stille Wiederherstellungen/Änderungen auf einmaliges Alarmieren beschränken. |
| 20 | P3 | Die normale Erinnerungsprobe begann sofort mit dem Text, während die echte Benachrichtigung vorher eine kurze Tonphase abwartete. | Auch bei der Probe erst den kurzen Hinweiston abwarten; weiterhin keine neue Erinnerung anlegen. |

Alle 20 dokumentierten Befunde sind im Code adressiert. Prioritäten bezeichnen die konkrete mögliche Folge, keine gemessene Auftretenshäufigkeit. Die Zahl ist keine künstliche Auffüllung auf die Obergrenze von 100.

## Kontrollfluss und Bestandsschutz

- Ton und Vorlesen sind weiterhin unabhängig; die feste Erinnerungsform verhindert globale Umdeutung gespeicherter Erinnerungen.
- Stimme, Tempo und Lautstärke der Weckerform werden einmalig aus bisherigen Stimmparametern übernommen und danach unabhängig gespeichert. Bestehende explizite Weckerwerte werden nicht überschrieben.
- Dienst und Ersatzweg lesen aktuelle Profile; der gemeinsame Weckablauf berücksichtigt alle vier Kombinationen aus Ton an/aus und Vorlesen an/aus. Scheitern alle Töne, werden Ladeversuche gedrosselt statt alle 200 Millisekunden wiederholt.
- Fremde Bestätigung und Formwechsel beenden den betroffenen Ton und Sprachjob. Mehrere Kurzfassungen verwenden weiterhin eine gemeinsame Sprachreihe.
- Wecktöne fallen bei nicht lesbarer eigener URI auf verfügbare Systemtöne zurück. Gewählte Handy-/Vorleselautstärken und deren Rückstellung bleiben erhalten.
- Android-Pflichtanzeigen laufender Dienste bleiben stille Statusanzeigen. Normale Ereigniskarten haben Ton, Ausgabeform und Vorlesen in derselben Reihenfolge, Wecker zusätzlich Tonwahl.
- Der Gemini-Schlüsselwechsel nutzt nur die zentrale Meldungsausgabe; Audio-Pufferung, Routing, Erkennung und Live-Verbindungslogik wurden nicht verändert.

## Abnahme und Grenzen

Die korrigierten Kotlin-Quellen wurden mit `:app:assembleDebug` erfolgreich gebaut. Der versionierte Abschluss wird erneut gebaut und über `Werkzeuge/jarvis-install/jarvis-install.ps1` installiert. APK-Update wird erneut aufgerufen; die bestehende Drive-Aussetzung wird respektiert. Im Schnellmodus keine automatisierten Tests und keine akustischen oder Live-Gerätetests: Frank übernimmt die praktische Prüfung. Der Build belegt Kompilierung und Paketierung, nicht akustisches Verhalten sämtlicher Android-/Nicht-stören-Zustände. Der bisherige Gradle-Hinweis zu compileSdk 36 und veraltete Android-API-Hinweise bestehen unabhängig von diesen Änderungen.

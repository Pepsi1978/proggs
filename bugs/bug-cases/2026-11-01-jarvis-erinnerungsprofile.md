# Jarvis: getrennte Benachrichtigungs- und Erinnerungsprofile

## Ursache
Die Oberfläche blendete Basisoptionen anhand unterschiedlicher Fähigkeiten der Meldungsarten aus. ErinnerungStimme las unabhängig von einem Vorlese-Schalter, ErinnerungKlingelDienst verwendete ausschließlich den System-Weckton. Dadurch genügte eine reine Ergänzung der Oberfläche nicht. Timer, Weckwort-Hinweise und Update-Ansagen hatten ebenfalls eigene Ausgabewege.

## Korrektur
Ereignismeldungen besitzen standardmäßig Ton, Ausgabeform und Vorlesen. Android-Pflichtanzeigen bleiben stille Statusanzeigen. Beide Erinnerungsformen haben feste, getrennte Profile; ihre bisherigen Stimmparameter werden einmalig kopiert, niemals dynamisch aus dem anderen Profil gelesen. ErinnerungStimme wählt Cache, Synthese, Vorlese-Schalter und Lautstärke passend zur Form. Der Klingeldienst berücksichtigt Tonwahl, Ton aus, Vorlesen aus sowie Live-/Aufnahmevorrang. Normale Ereignisse nutzen gemeinsame Ausgabe und eigene HIGH-Kanäle, weil die Wichtigkeit alter LOW-Statuskanäle nicht angehoben werden kann.

## Absicherung
Die feste Form verhindert, dass globale Einstellungen die beim Anlegen gewählte Erinnerungsform ändern. Nicht abspielbare eigene Wecktöne fallen auf den Standard zurück. Ausgeschaltetes Vorlesen wird vor der Erzeugung und vor der Wiedergabe erneut berücksichtigt. Stille Wiederherstellungen normaler Erinnerungen lösen keinen Ton aus.

## Abschluss
Debug-Build erfolgreich. Im Schnellmodus keine Geräte-Funktionstests; die Bedienung und akustische Ausgabe prüft Frank selbst.

## Ergänzung aus dem sehr ausführlichen Review

Siehe `Jarvis/REVIEW-VU5J6.md` mit 20 konkreten Befunden. Dienst und Ersatzweg verwenden jetzt denselben `ErinnerungsWeckablauf`: dauerhaft bis zur Bestätigung, Wachhaltung auch bei Ton aus, Live-/Aufnahmevorrang und Abbruch inzwischen bestätigter oder geänderter Texte. Tondauer ist unabhängig vom Vorlese-Schalter. WeckerTon bereitet auf IO vor und prüft unter einer gemeinsamen Sperre vor dem tatsächlichen Start, ob der Lauf noch gültig ist; dadurch kein spät wiederkehrender Ton nach Stopp. Vorlese-Cache vor Netzarbeit kennzeichnen und Erzeugung/Bereinigung synchronisieren. Eine gemeinsame Meldungsart allein ist keine Identität für wartendes Vorlesen: immer die eigene Meldungsnummer zuordnen.

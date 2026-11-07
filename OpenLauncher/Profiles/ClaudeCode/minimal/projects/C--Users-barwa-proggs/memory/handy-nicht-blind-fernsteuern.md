---
name: handy-nicht-blind-fernsteuern
description: "Vor adb-Eingaben (tap, text, keyevent) prüfen, welche App vorn ist; Frank nutzt das Handy oft gleichzeitig"
metadata:
  node_type: memory
  type: feedback
  originSessionId: e79775e1-35fa-4ac6-972c-82b8f09a1290
  modified: 2026-10-08T14:20:50.680Z
---

Vor jeder adb-Eingabe (`adb shell input`, auch `am start`) prüfen, dass die erwartete App vorn ist.

**Why:** 08.10.2026: Tipp, Testtext und zweimal „Zurück" per `adb shell input` landeten in Franks Browser, wo er gerade die ChatGPT-Anmeldung machte. Nachtrag 08.10.2026: Jarvis per `am start` geöffnet, obwohl `dumpsys window` unmittelbar davor ChatGPT im Vordergrund zeigte. Frank bedient das Handy während einer Sitzung oft selbst; Installieren, Logcat und Screenshots stören nicht, Eingaben schon.

**How to apply:**
- Vor jeder Eingabe und nach jedem Schritt erneut per `uiautomator dump` oder `dumpsys window` prüfen.
- Zeigt die Prüfung eine fremde App: Schritt lassen, Frank bitten, die App selbst zu öffnen.
- Findet die Suche ein Bedienelement nicht: sofort abbrechen, nicht weitertippen.
- Besser: Test ohne Bedienung wählen (z. B. Aufruf von außen über den Server).
- Meine Deutung: im Zweifel Frank bitten, den Schritt selbst zu tippen.

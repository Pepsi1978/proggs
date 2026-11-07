---
name: handy-nicht-blind-fernsteuern
description: "Vor adb-Eingaben (tap, text, keyevent) prüfen, welche App vorn ist; Frank nutzt das Handy oft gleichzeitig"
metadata:
  node_type: memory
  type: feedback
  originSessionId: e79775e1-35fa-4ac6-972c-82b8f09a1290
  modified: 2026-10-08T14:20:50.680Z
---

Am 08.10.2026 habe ich per `adb shell input` einen Tipp, einen Testtext und zweimal „Zurück" ans Handy geschickt, während Frank dort gerade im Browser die ChatGPT-Anmeldung machte. Die Eingaben landeten in seinem Browser.

**Why:** Frank bedient das Handy während einer Sitzung oft selbst. Installieren, Logcat und Screenshots stören nicht, Eingaben schon.

**How to apply:** Vor jeder Eingabe per adb mit `uiautomator dump` oder `dumpsys window` prüfen, dass die erwartete App vorn ist, und nach jedem Schritt erneut. Liefert die Suche nach einem Bedienelement nichts, sofort abbrechen, statt weiterzutippen. Besser: einen Test wählen, der ohne Bedienung auskommt (z. B. Aufruf von außen über den Server). Meine Deutung: Im Zweifel Frank bitten, den Schritt selbst zu tippen.

Nachtrag 08.10.2026: Auch `am start` zählt. Ich habe Jarvis geöffnet, obwohl `dumpsys window` unmittelbar davor ChatGPT im Vordergrund zeigte. Zeigt die Prüfung eine fremde App, den Schritt lassen und Frank bitten, die App selbst zu öffnen.

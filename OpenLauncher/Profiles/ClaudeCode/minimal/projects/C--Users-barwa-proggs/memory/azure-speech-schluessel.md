---
name: azure-speech-schluessel
description: "Baue Azure ein" = Azure-Speech-Schlüssel und Region aus C:\Users\barwa\SK\Azure\azure-speech.json nehmen
metadata:
  type: reference
---

Sagt der Nutzer „baue Azure ein“ (Neural-Stimmen/TTS oder Spracherkennung), liegen Schlüssel, Region (`germanywestcentral`) und Endpunkte in `C:\Users\barwa\SK\Azure\azure-speech.json` (nur Key 1: `azure-speech-key.txt`). Details in `C:\Users\barwa\SK\README.md`, Abschnitt `Azure/`.

Ressource `jarvis-speech-barwa`, Tarif Free F0, angelegt 23.10.2026 als 30-Tage-Testabo; nach Ablauf muss das Abo auf Pay-as-you-go umgestellt werden, sonst liefert der Schlüssel 401.

Schlüssel nie ins Repo kopieren, zur Laufzeit aus SK lesen.

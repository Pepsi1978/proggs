# update.ps1 — Weiterleitung an ..\rebuild-overlay.ps1 TVO (Stand 23.10.2026, 11:40 Uhr)
#
# Ein Update des Overlays gibt es nur über rebuild-overlay.ps1: Es zeigt zuerst das Ja/Nein-Fenster
# und baut erst nach dem Klick auf "Ja". Dieses Skript hatte früher einen eigenen Weg ohne
# Rückfrage; er ist bewusst entfernt, damit kein Aufruf am Fenster vorbei aktualisiert.
& pwsh -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot '..\rebuild-overlay.ps1') TVO @args
exit $LASTEXITCODE

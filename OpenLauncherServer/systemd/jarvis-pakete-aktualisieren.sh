#!/bin/bash
# Systempakete für das Programmieren: alle täglich ab 4:30 Uhr Berliner Zeit, danach jede Stunde nur, was übersprungen wurde oder scheiterte.
# Läuft als root. installieren.sh kopiert es deshalb nach /usr/local/sbin; aus dem Repo, das dem Benutzer jarvis gehört, darf root es nie direkt ausführen.
# tmux fehlt bewusst: Eine neue Fassung spräche nicht mehr mit dem laufenden tmux-Server aller Sitzungen.
set -u
export DEBIAN_FRONTEND=noninteractive TZ=Europe/Berlin
# Kein Neustart von Diensten durch needrestart: Er könnte jarvis-tmux treffen und damit alle Sitzungen beenden.
export NEEDRESTART_SUSPEND=1
ALLE="gh git git-lfs jq ripgrep unzip curl wget python3-aiohttp python3-pip python3-venv pipx gcc g++ make cmake ninja-build ffmpeg shellcheck shfmt bat fzf tree git-delta xvfb x11vnc novnc openbox google-chrome-stable dotnet-sdk-8.0 dotnet-sdk-10.0 openjdk-17-jdk-headless openjdk-21-jdk-headless powershell"
Z=/var/lib/jarvis-pakete
mkdir -p "$Z"
HEUTE=$(date +%F)
TAG=$(cat "$Z/tag" 2>/dev/null || true)
OFFEN=$(cat "$Z/offen" 2>/dev/null || true)

if [ "$TAG" != "$HEUTE" ] && [ "$(date +%H%M)" -ge 430 ]; then
  LISTE=$ALLE
  # Vorab vermerken: Endet der Lauf vorzeitig, holt die nächste Stunde den Rest nach.
  echo "$HEUTE" > "$Z/tag"
  echo "$ALLE" > "$Z/offen"
elif [ -n "$OFFEN" ]; then
  LISTE=$OFFEN
else
  exit 0
fi

laeuft() {
  case "$1" in
    google-chrome-stable) pgrep -x chrome >/dev/null ;;
    dotnet-sdk-*) pgrep -x dotnet >/dev/null ;;
    openjdk-*) pgrep -x java >/dev/null ;;
    powershell) pgrep -x pwsh >/dev/null ;;
    *) return 1 ;;
  esac
}

apt-get update -qq || echo "Paketquellen teilweise nicht erreichbar"
NOCH_OFFEN=""
# Einzeln: Ein Name, den es in den Quellen nicht gibt, hielte sonst alle anderen auf.
for PAKET in $LISTE; do
  if [ -z "$(apt-cache policy "$PAKET" | sed -n 's/^ *Candidate: //p' | grep -v '(none)')" ]; then
    echo "$PAKET: nicht in den Paketquellen"
  elif laeuft "$PAKET"; then
    echo "$PAKET: läuft gerade, nächste Stunde"
    NOCH_OFFEN="$NOCH_OFFEN $PAKET"
  elif ! apt-get install -y -qq -o DPkg::Lock::Timeout=300 --only-upgrade "$PAKET"; then
    echo "$PAKET: fehlgeschlagen, nächste Stunde"
    NOCH_OFFEN="$NOCH_OFFEN $PAKET"
  fi
done
echo "$NOCH_OFFEN" | sed 's/^ *//' > "$Z/offen"
exit 0

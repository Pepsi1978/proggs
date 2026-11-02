#!/bin/bash
# Installiert oder aktualisiert Werkzeuge, die weder npm noch ein Paketarchiv aktuell liefern, in ~/.local/bin. Aufruf aus clis.py: trivy, lazygit oder rclone.
# Nur bei neuer Version wird geladen. Heruntergeladenes wird vor dem Austausch per Prüfsumme kontrolliert; ein Abbruch lässt den alten Stand unberührt.
set -euo pipefail
ZIEL="$HOME/.local/bin"
mkdir -p "$ZIEL"
T=$(mktemp -d)
trap 'rm -rf "$T"' EXIT
cd "$T"

neueste() {
  local v
  v=$(curl -fsSL "https://api.github.com/repos/$1/releases/latest" | jq -r '.tag_name // empty')
  [ -n "$v" ] || { echo "Keine Version von $1 gefunden"; exit 1; }
  echo "$v"
}

case "${1:-}" in
  trivy)
    V=$(neueste aquasecurity/trivy)
    NUMMER=${V#v}
    if [ "$(trivy --version 2>/dev/null | awk 'NR==1 {print $2}')" != "$NUMMER" ]; then
      D="trivy_${NUMMER}_Linux-64bit.tar.gz"
      curl -fsSLO "https://github.com/aquasecurity/trivy/releases/download/$V/$D"
      curl -fsSLO "https://github.com/aquasecurity/trivy/releases/download/$V/trivy_${NUMMER}_checksums.txt"
      grep "  $D\$" "trivy_${NUMMER}_checksums.txt" | sha256sum -c --quiet -
      tar -xzf "$D" trivy
      install -m 755 trivy "$ZIEL/trivy"
    fi
    ;;
  lazygit)
    V=$(neueste jesseduffield/lazygit)
    NUMMER=${V#v}
    if ! lazygit --version 2>/dev/null | grep -q "version=$NUMMER,"; then
      D="lazygit_${NUMMER}_linux_x86_64.tar.gz"
      curl -fsSLO "https://github.com/jesseduffield/lazygit/releases/download/$V/$D"
      curl -fsSLO "https://github.com/jesseduffield/lazygit/releases/download/$V/checksums.txt"
      grep " $D\$" checksums.txt | sha256sum -c --quiet -
      tar -xzf "$D" lazygit
      install -m 755 lazygit "$ZIEL/lazygit"
    fi
    ;;
  rclone)
    # Ubuntus Fassung in /usr/bin bleibt für Dienste von root liegen; ~/.local/bin steht für die Sitzungen im PATH davor.
    if [ -x "$ZIEL/rclone" ]; then
      "$ZIEL/rclone" selfupdate --stable
    else
      V=$(curl -fsSL https://downloads.rclone.org/version.txt | awk '{print $2}')
      [ -n "$V" ] || { echo "Keine Rclone-Version gefunden"; exit 1; }
      D="rclone-$V-linux-amd64.zip"
      curl -fsSLO "https://downloads.rclone.org/$V/$D"
      curl -fsSLO "https://downloads.rclone.org/$V/SHA256SUMS"
      grep "  $D\$" SHA256SUMS | sha256sum -c --quiet -
      unzip -qo "$D"
      install -m 755 "rclone-$V-linux-amd64/rclone" "$ZIEL/rclone"
    fi
    ;;
  *)
    echo "Unbekanntes Werkzeug: ${1:-}"
    exit 2
    ;;
esac

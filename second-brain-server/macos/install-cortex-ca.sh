#!/usr/bin/env bash
# Cortex-Root-CA auf diesem Mac vertrauenswuerdig machen.
#
# Warum: Das Cockpit laeuft hinter Caddy per HTTPS auf https://10.8.0.1, das Zertifikat kommt aber
# von Caddys EIGENER interner CA (bewusst kein Let's Encrypt — es gibt keinen oeffentlichen DNS-Namen
# und keine offenen Ports; siehe bugs/server/reverse-proxy-tls.md §1). Diese CA kennt kein Browser
# ab Werk -> Chrome schreibt "Nicht sicher". Nach diesem Skript kennt der Mac sie und Chrome zeigt
# ein normales Schloss.
#
# Idempotent: beliebig oft ausfuehrbar. Holt die CA frisch vom Server, wenn der Tunnel steht,
# sonst nimmt es die Sicherungskopie aus ~/SK/second-brain/.
#
#   bash ~/proggs/second-brain-server/macos/install-cortex-ca.sh
#
# Kein sudo noetig: Der Import geht in den Login-Schluesselbund des Benutzers; macOS wertet
# User-Trust-Settings genauso aus wie System-Trust (Chrome und Safari lesen beides).
set -euo pipefail

VPS_IP="168.231.83.205"
WG_IP="10.8.0.1"
SSH_KEY="$HOME/SK/second-brain/id_ed25519"
CA_FILE="$HOME/SK/second-brain/caddy-root-ca.crt"
KEYCHAIN="$HOME/Library/Keychains/login.keychain-db"

echo "== Cortex Root-CA einrichten =="

# 1) CA frisch holen (nur wenn SSH klappt) — sonst mit der Sicherungskopie weiterarbeiten.
if ssh -o BatchMode=yes -o ConnectTimeout=8 -i "$SSH_KEY" "root@$VPS_IP" true 2>/dev/null; then
  tmp="$(mktemp)"
  if ssh -o BatchMode=yes -i "$SSH_KEY" "root@$VPS_IP" \
       "docker exec sb-caddy cat /data/caddy/pki/authorities/local/root.crt" > "$tmp" 2>/dev/null \
     && openssl x509 -in "$tmp" -noout >/dev/null 2>&1; then
    mv "$tmp" "$CA_FILE"; chmod 644 "$CA_FILE"
    echo "  CA frisch vom Server geholt -> $CA_FILE"
  else
    rm -f "$tmp"; echo "  WARNUNG: Abholen fehlgeschlagen, nehme vorhandene Kopie."
  fi
else
  echo "  Server per SSH nicht erreichbar (Full-Tunnel-VPN an?) — nehme vorhandene Kopie."
fi

[ -f "$CA_FILE" ] || { echo "FEHLER: Keine CA-Datei unter $CA_FILE."; exit 1; }

FP="$(openssl x509 -in "$CA_FILE" -noout -fingerprint -sha256 | cut -d= -f2)"
echo "  Fingerabdruck: $FP"
echo "  Gueltig bis:   $(openssl x509 -in "$CA_FILE" -noout -enddate | cut -d= -f2)"

# 2) Alte Kopien derselben CA entfernen (sonst sammeln sich Duplikate mit altem Trust an).
while security find-certificate -c "Caddy Local Authority" -Z "$KEYCHAIN" >/dev/null 2>&1; do
  sha1="$(security find-certificate -c "Caddy Local Authority" -Z "$KEYCHAIN" \
          | awk '/SHA-1 hash:/ {print $3; exit}')"
  [ -n "$sha1" ] || break
  security delete-certificate -Z "$sha1" "$KEYCHAIN" >/dev/null 2>&1 || break
  echo "  alte CA-Kopie entfernt ($sha1)"
done

# 3) Importieren + als Root vertrauen.
security add-trusted-cert -r trustRoot -k "$KEYCHAIN" "$CA_FILE"
echo "  CA importiert und als vertrauenswuerdig markiert."

# 4) Beweis antreten: echte TLS-Pruefung OHNE -k gegen das Cockpit.
if /usr/bin/curl -sS -o /dev/null -w '' "https://$WG_IP/" 2>/dev/null; then
  code="$(/usr/bin/curl -s -o /dev/null -w '%{http_code}' "https://$WG_IP/")"
  echo "  PRUEFUNG OK: https://$WG_IP antwortet mit HTTP $code, Zertifikat wird akzeptiert."
  echo "CORTEX_CA_OK"
else
  echo "  PRUEFUNG: https://$WG_IP nicht erreichbar (WireGuard-Tunnel aus?) — Import ist trotzdem gesetzt."
fi

echo
echo "Chrome benutzt ab sofort: https://$WG_IP  (nicht mehr http://$WG_IP:8003)"
echo "Chrome muss einmal komplett neu gestartet werden (Cmd+Q), damit er den Schluesselbund neu liest."

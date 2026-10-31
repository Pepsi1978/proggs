#!/bin/bash
# Richtet OpenLauncher Server ein oder bringt ihn auf den Stand. Als root auf dem Hostinger-Server, beliebig oft ausführbar.
# Vorher vom Mac: Zugänge nach /home/jarvis/SK kopieren (siehe README.md). Die Abo-Anmeldungen erfolgen danach in der Server-Konsole.
set -euo pipefail
BENUTZER=jarvis
ZUHAUSE=/home/$BENUTZER
REPO=$ZUHAUSE/proggs
als() { sudo -u "$BENUTZER" -H env HOME="$ZUHAUSE" NPM_CONFIG_PREFIX="$ZUHAUSE/.local" PATH="$ZUHAUSE/.local/bin:$ZUHAUSE/.local/node/bin:$ZUHAUSE/android-sdk/cmdline-tools/latest/bin:$ZUHAUSE/android-sdk/platform-tools:/usr/local/bin:/usr/bin:/bin" bash -c "cd ~; $1"; }

apt-get update -qq
DEBIAN_FRONTEND=noninteractive apt-get install -y -qq tmux git git-lfs jq ripgrep unzip curl python3-aiohttp openjdk-17-jdk-headless wget libatomic1

# Swap: Ohne ihn beendet der Kernel bei Speicherspitzen beliebige Dienste.
if ! swapon --show | grep -q /swapfile; then
  [ -f /swapfile ] || { fallocate -l 4G /swapfile; chmod 600 /swapfile; mkswap /swapfile >/dev/null; }
  swapon /swapfile
  grep -q '^/swapfile' /etc/fstab || echo '/swapfile none swap sw 0 0' >> /etc/fstab
  echo 'vm.swappiness=10' > /etc/sysctl.d/90-jarvis-swap.conf; sysctl -q -p /etc/sysctl.d/90-jarvis-swap.conf
fi

# Eigener Benutzer: nie root, nicht in der Gruppe docker (Sitzungen laufen ohne Rückfragen).
id "$BENUTZER" >/dev/null 2>&1 || useradd -m -s /bin/bash "$BENUTZER"
gpasswd -d "$BENUTZER" docker >/dev/null 2>&1 || true
chmod 750 "$ZUHAUSE"

# Node.js LTS im Benutzerordner: Ubuntus Paket ist zu alt für die CLIs.
if [ ! -x "$ZUHAUSE/.local/node/bin/node" ]; then
  V=$(curl -fsSL https://nodejs.org/dist/index.json | jq -r '[.[] | select(.lts)][0].version')
  als "mkdir -p ~/.local/node ~/.local/bin && curl -fsSL https://nodejs.org/dist/$V/node-$V-linux-x64.tar.xz | tar -xJ --strip-components=1 -C ~/.local/node"
fi

# PATH und Zeitzone für jede Shell des Benutzers; der Block wird bei jedem Lauf erneuert.
touch "$ZUHAUSE/.profile"
sed -i '/^# OpenLauncherServer$/,/^# OpenLauncherServer Ende$/d; /^export PATH=.*\.kimi-code\/bin/d; /^export ANDROID_HOME=.*JARVIS_SERVER/d' "$ZUHAUSE/.profile"
cat >> "$ZUHAUSE/.profile" <<'P'
# OpenLauncherServer
export PATH="$HOME/.local/bin:$HOME/.local/node/bin:$HOME/.kimi-code/bin:$HOME/.agy/bin:$HOME/android-sdk/platform-tools:$HOME/android-sdk/cmdline-tools/latest/bin:$PATH"
export ANDROID_HOME="$HOME/android-sdk" ANDROID_SDK_ROOT="$HOME/android-sdk" NPM_CONFIG_PREFIX="$HOME/.local" TZ=Europe/Berlin JARVIS_SERVER=1 JARVIS_RECHNER=Hostinger-Server
# OpenLauncherServer Ende
P
chown "$BENUTZER": "$ZUHAUSE/.profile"

# Android SDK (Regel: bauen und per Tunnel installieren geht auch von hier).
if [ ! -x "$ZUHAUSE/android-sdk/platform-tools/adb" ]; then
  als 'mkdir -p ~/android-sdk/cmdline-tools && cd ~/android-sdk/cmdline-tools && curl -fsSL -o tools.zip https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip && unzip -qo tools.zip && rm -rf latest && mv cmdline-tools latest && rm tools.zip'
  als 'yes | sdkmanager --licenses >/dev/null 2>&1 || true; sdkmanager "platform-tools" "platforms;android-36" "build-tools;36.0.0" >/dev/null'
fi
als 'mkdir -p ~/.gradle ~/.android && cat > ~/.gradle/gradle.properties <<G
# Auf den Server begrenzt (2 vCPU, Speicherdeckel in jarvis-tmux.service).
org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=768m -Dfile.encoding=UTF-8
org.gradle.workers.max=2
org.gradle.parallel=false
kotlin.daemon.jvmargs=-Xmx1536m
org.gradle.daemon.idletimeout=300000
G'
[ -f "$ZUHAUSE/SK/Android/debug-shared.keystore" ] && install -o "$BENUTZER" -g "$BENUTZER" -m 600 "$ZUHAUSE/SK/Android/debug-shared.keystore" "$ZUHAUSE/.android/debug.keystore"

# Repo über einen eigenen Schreibschlüssel (Deploy-Key, gilt nur für dieses Repo). LFS-Dateien nur bei Bedarf holen.
als 'mkdir -p ~/.ssh && chmod 700 ~/.ssh; [ -f ~/.ssh/id_ed25519 ] || ssh-keygen -q -t ed25519 -N "" -C jarvis-server -f ~/.ssh/id_ed25519; curl -fsSL https://api.github.com/meta | jq -r ".ssh_keys[]" | sed "s/^/github.com /" > ~/.ssh/known_hosts.neu; [ -s ~/.ssh/known_hosts.neu ] && mv ~/.ssh/known_hosts.neu ~/.ssh/known_hosts; rm -f ~/.ssh/known_hosts.neu; chmod 600 ~/.ssh/known_hosts'
als 'git config --global user.name "Jarvis Server"; git config --global user.email jarvis-server@users.noreply.github.com; git config --global pull.rebase true; git config --global init.defaultBranch main; git lfs install --skip-smudge >/dev/null'
if [ ! -d "$REPO/.git" ]; then
  # ssh -T endet bei GitHub auch im Erfolgsfall mit Code 1: Nur die Meldung zählt.
  ANTWORT=$(als "ssh -o BatchMode=yes -T git@github.com" 2>&1 || true)
  if ! grep -q 'successfully authenticated' <<<"$ANTWORT"; then
    echo "DEPLOY_KEY_FEHLT:"; cat "$ZUHAUSE/.ssh/id_ed25519.pub"; exit 3
  fi
  als "git clone --filter=blob:none --quiet git@github.com:Pepsi1978/proggs.git $REPO"
fi
als "git -C $REPO pull --ff-only --quiet" || true

# CLIs und Dienste
als "mkdir -p ~/.openlauncher/sitzungen ~/.openlauncher/start ~/.local/bin"
als 'command -v claude >/dev/null || curl -fsSL https://claude.ai/install.sh | bash'
als 'command -v agy >/dev/null || curl -fsSL https://antigravity.google/cli/install.sh | bash' || echo "Antigravity CLI nicht installiert"
for P in @openai/codex opencode-ai; do als "npm install -g --silent $P@latest" || echo "$P nicht installiert"; done
als 'npm install -g --silent --allow-scripts=@moonshot-ai/kimi-code,node-pty @moonshot-ai/kimi-code@latest' || echo "Kimi Code nicht installiert"

# PowerShell aus dem Microsoft-Paketarchiv (nur als wählbare Shell, nichts hängt davon ab).
if ! command -v pwsh >/dev/null; then
  curl -fsSL -o /tmp/ms.deb https://packages.microsoft.com/config/ubuntu/24.04/packages-microsoft-prod.deb && dpkg -i /tmp/ms.deb >/dev/null && apt-get update -qq && apt-get install -y -qq powershell || echo "PowerShell nicht installiert"
  rm -f /tmp/ms.deb
fi

install -m 644 "$REPO"/OpenLauncherServer/systemd/*.service "$REPO"/OpenLauncherServer/systemd/*.timer /etc/systemd/system/
systemctl daemon-reload
if command -v ufw >/dev/null; then ufw allow in on wg0 to any port 8090 proto tcp comment 'Server-Konsole' >/dev/null; else echo "ufw fehlt: Port 8090 auf wg0 selbst freigeben"; fi
systemctl enable --now jarvis-tmux.service jarvis-konsole.service jarvis-server-bruecke.service jarvis-clis.timer jarvis-repo.timer
systemctl restart jarvis-konsole.service jarvis-server-bruecke.service
# tmux nicht neu starten, das beendete alle Sitzungen: Die Konfiguration nur neu einlesen.
sudo -u "$BENUTZER" -H tmux -L jarvis source-file "$REPO/OpenLauncherServer/systemd/tmux.conf" || true
echo "FERTIG"

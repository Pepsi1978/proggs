#!/usr/bin/env bash
# scan-legal-signals.sh — Vollscan einer Android-App nach rechtlich relevanten Signalen.
#
# Wird vom Rechtssicherheits-Skill in Schritt 4 (Vollscan) aufgerufen.
# Liefert kategorisierte Treffer fuer: Legal-Dokumente, Permissions, SDKs, Datenverarbeitung,
# Health, KI, Kinder, UGC, Tracking, Ads, Cleartext, Secrets, WebViews.
#
# Verwendung: bash scan-legal-signals.sh <APP_DIR>
#
# Direktive 3: Skript ist defensiv geschrieben — fehlt rg, faellt es kontrolliert zurueck
# auf grep -rn. Ungueltige Pfade fuehren zu klarem Fehler.

set -u

APP_DIR="${1:-}"

if [ -z "$APP_DIR" ]; then
    echo "Verwendung: $0 <APP_DIR>" >&2
    exit 2
fi

if [ ! -d "$APP_DIR" ]; then
    echo "Fehler: '$APP_DIR' ist kein Verzeichnis." >&2
    exit 2
fi

# Suche-Strategie: bevorzugt rg (schneller), fallback auf grep -E (extended regex — Pflicht
# damit `|` als ODER funktioniert). `command -v rg` muss ein BINARY finden, nicht eine
# Bash-Funktion — Claude Code injiziert eine `rg`-Funktion in interaktive Shells, die in
# Subshells nicht verfuegbar ist. Direktive 3: ohne -E waeren alle Pipe-Patterns unwirksam.
if command -v rg >/dev/null 2>&1 && ! type -t rg 2>/dev/null | grep -q function; then
    SEARCH="rg -n -i --no-heading"
    FILES="rg --files"
else
    # grep -E damit `|` als ODER funktioniert; Build-/Cache-Dirs ausschliessen; Binary-Files ueberspringen
    SEARCH="grep -E -rn -i --color=never --binary-files=without-match --exclude-dir=.gradle --exclude-dir=.git --exclude-dir=build --exclude-dir=node_modules --exclude-dir=.idea --exclude-dir=captures --exclude-dir=.claude --exclude-dir=.kotlin --exclude-dir=.gemini"
    FILES="find . -type f"
fi

echo "==> Datei-Inventur"
$FILES "$APP_DIR" 2>/dev/null | \
    grep -Ei '\.(kt|java|xml|gradle|kts|json|properties|md|html|js|ts|tsx|jsx|yaml|yml|txt|csv)$' | \
    grep -Ev '/(build|\.gradle|node_modules|\.idea|captures)/' | head -200
echo ""

echo "==> Legal-Signale (Datenschutz, Terms, Impressum, Widerruf, Consent, Loeschung)"
$SEARCH "privacy|datenschutz|dsgvo|gdpr|ccpa|cpra|consent|einwilligung|widerruf|withdraw|terms|nutzungsbedingungen|agb|impressum|anbieter|legal|policy|delete account|account deletion|datenloesch|loesch" "$APP_DIR" 2>/dev/null | head -100
echo ""

echo "==> Permissions & Manifest (kritische Permissions, exported, allowBackup, etc.)"
$SEARCH "uses-permission|exported|allowBackup|fullBackupContent|dataExtractionRules|networkSecurityConfig|usesCleartextTraffic|debuggable" "$APP_DIR" 2>/dev/null | head -100
echo ""

echo "==> SDKs & Drittanbieter"
$SEARCH "firebase|analytics|crashlytics|admob|ads|facebook|meta|adjust|appsflyer|sentry|amplitude|mixpanel|onesignal|revenuecat|billing|stripe|openai|anthropic|googleapis|okhttp|retrofit|ktor|webview" "$APP_DIR" 2>/dev/null | head -100
echo ""

echo "==> Tracking, Telemetry, Werbe-IDs"
$SEARCH "tracking|telemetry|ad_id|advertising[-_ ]id|aaid|gaid|idfa" "$APP_DIR" 2>/dev/null | head -50
echo ""

echo "==> Sensitive Permissions (Standort, Kamera, Mikrofon, Kontakte, Kalender, SMS, Health)"
$SEARCH "ACCESS_FINE_LOCATION|ACCESS_BACKGROUND_LOCATION|READ_CONTACTS|READ_CALENDAR|READ_PHONE_STATE|CAMERA|RECORD_AUDIO|READ_MEDIA_IMAGES|READ_MEDIA_VIDEO|MANAGE_EXTERNAL_STORAGE|POST_NOTIFICATIONS|QUERY_ALL_PACKAGES|AD_ID|HealthConnect|health" "$APP_DIR" 2>/dev/null | head -50
echo ""

echo "==> KI / GenAI / Chatbot"
$SEARCH "openai|anthropic|claude|gemini|llm|chatbot|generative|deepfake|image generation|tts|voice cloning" "$APP_DIR" 2>/dev/null | head -50
echo ""

echo "==> Kinder / Families / COPPA"
$SEARCH "children|kids|families|coppa|gdpr-k|parental consent|age gate|targetSdkVersion" "$APP_DIR" 2>/dev/null | head -30
echo ""

echo "==> UGC, Chat, Sharing, Moderation"
$SEARCH "ugc|user generated|moderation|report|melden|chat|post|comment|share|forum" "$APP_DIR" 2>/dev/null | head -30
echo ""

echo "==> Backups, Logs, Secrets"
$SEARCH "Log\\.|Timber|println|printStackTrace|Encrypted|KeyStore|MasterKey|api[_-]?key|secret|token|Authorization|Bearer|client_secret" "$APP_DIR" 2>/dev/null | head -50
echo ""

echo "==> Cleartext-Verbindungen (http:// statt https://)"
$SEARCH "http://" "$APP_DIR" 2>/dev/null | head -30
echo ""

echo "==> WebViews"
$SEARCH "WebView|webview|setJavaScriptEnabled|addJavascriptInterface" "$APP_DIR" 2>/dev/null | head -30
echo ""

echo "==> Billing, Abos, Paywall, IAP"
$SEARCH "billing|subscription|abo|refund|iap|in-app|paywall|trial|auto[-_ ]renewal" "$APP_DIR" 2>/dev/null | head -30
echo ""

echo "==> Barrierefreiheit (ContentDescription, Touch-Target)"
$SEARCH "contentDescription|content_description|touchTarget|wcag|accessibility" "$APP_DIR" 2>/dev/null | head -30
echo ""

echo "==> Fertig. Beruecksichtige diese Treffer in Schritt 5 (Detail-Pruefung) und der Code-vs-Text-vs-Play-Matrix."
exit 0

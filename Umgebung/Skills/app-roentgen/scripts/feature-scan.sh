#!/usr/bin/env bash
# app-roentgen: Master-Scan-Skript fuer Android-Apps
# Aufruf: bash feature-scan.sh <pfad-zur-android-app>
# Schreibt strukturierten Initial-Bericht in <app>/app-roentgen-initial-scan.md

# Kein 'set -e' (partial-failure ist gewollt — grep gibt 1 zurueck bei keinen Treffern,
# das soll den Scan nicht abbrechen). 'set -u' faengt vergessene Variablen, 'pipefail'
# faengt Fehler in Pipelines. Style-Warnings (SC2126, SC2155) sind bewusst akzeptiert.
# shellcheck disable=SC2155,SC2126,SC2086
set -uo pipefail

APP_DIR="${1:-}"
if [[ -z "$APP_DIR" ]]; then
    echo "Fehler: Bitte App-Verzeichnis als Parameter angeben." >&2
    echo "Aufruf: bash feature-scan.sh <pfad-zur-android-app>" >&2
    exit 1
fi

if [[ ! -d "$APP_DIR" ]]; then
    echo "Fehler: Verzeichnis '$APP_DIR' existiert nicht." >&2
    exit 1
fi

# === Preflight-Check (FIX H4) =================================================
# Pflicht-Tools VOR der eigentlichen Arbeit pruefen. Ohne diesen Check faellt
# das Skript erst mitten im Scan auf — nach 30+ Sekunden Wartezeit auf einen
# defekten grep, mit Muell-Output. Mit dem Check: klare Fehlermeldung in 50ms.
PREFLIGHT_MISSING=()
for _tool in grep find sort sed awk; do
    command -v "$_tool" >/dev/null 2>&1 || PREFLIGHT_MISSING+=("$_tool")
done
if [ ${#PREFLIGHT_MISSING[@]} -gt 0 ]; then
    echo "FEHLER: Diese Pflicht-Tools fehlen: ${PREFLIGHT_MISSING[*]}" >&2
    echo "Hinweis: Auf Windows Git Bash, macOS und Linux sollten diese Tools" >&2
    echo "  standardmaessig vorhanden sein. Falls nicht, PATH pruefen." >&2
    exit 1
fi

# Optionale Tools erkennen (Skript laeuft auch ohne, ist aber schneller / nuetzlicher mit)
HAS_RG=0
command -v rg >/dev/null 2>&1 && HAS_RG=1
HAS_JQ=0
command -v jq >/dev/null 2>&1 && HAS_JQ=1

# FIN-003: RG_CMD fuer direkten rg/grep-RE-Aufruf (Assets-Scan, einfache Grep-Stellen)
# GREP_R() deckt .kt/.java-Suchen ab; RG_CMD ist fuer einfache find-aehnliche Glob-Scans.
if command -v rg >/dev/null 2>&1; then
    RG_CMD="rg"
else
    RG_CMD="grep -RE"
fi

if [ "$HAS_RG" = "0" ]; then
    echo "Hinweis: ripgrep (rg) nicht gefunden — falle auf grep zurueck (5-10x langsamer)." >&2
    echo "         Installation: 'brew install ripgrep' (macOS) oder 'winget install ripgrep'" >&2
fi
if [ "$HAS_JQ" = "0" ]; then
    echo "Hinweis: jq nicht gefunden — JSON-Pretty-Print im Bericht wird uebersprungen." >&2
fi

# json_pretty filtert JSON-Strings durch jq wenn verfuegbar, sonst unveraendert. Erlaubt
# spaeter (z.B. wenn der Audit JSON-Schnipsel vom export-json.py einbettet) maschinen-
# lesbare und gleichzeitig menschlich gut lesbare Bloecke. Wird in Schicht 7 (Marketing)
# fuer eingebettete JSON-Statistik genutzt — verhindert tote HAS_JQ-Variable (SC2034).
json_pretty() {
    if [ "$HAS_JQ" = "1" ]; then
        jq '.' 2>/dev/null || cat
    else
        cat
    fi
}
# =============================================================================

# Defense in Depth: || exit verhindert dass das Skript im AUFRUFER-Verzeichnis weiterlaeuft
# wenn cd fehlschlaegt (z.B. Permission-Denied, defekter Symlink). Ohne diesen Guard
# wuerde der Scan dann das falsche Verzeichnis durchforsten und Muell-Output erzeugen.
cd "$APP_DIR" || {
    echo "Fehler: cd nach '$APP_DIR' fehlgeschlagen (Permission?)." >&2
    exit 1
}

# === ripgrep-Fallback (FIX M5) ================================================
# Helper-Funktion: nutzt rg wenn verfuegbar (5-10x schneller), sonst grep.
# Beide Varianten respektieren --include='*.kt' und Multi-Pattern-Syntax.
GREP_R() {
    # $1 = pattern, $2+ = optionale grep-Args (z.B. -l, -i, -A 5)
    local pattern="$1"; shift
    if [ "$HAS_RG" = "1" ]; then
        # rg parsts grep-OR ('a\|b') anders — wir verlassen uns auf grep-Syntax
        # und uebergeben sie ueber --regexp + --hidden=false. rg matched UTF-8
        # standardmaessig und respektiert .gitignore.
        # FIX W2 (Audit 6): vorher nur --type kotlin. Java-Legacy-SDKs (RevenueCat
        # Java, Firebase Java, alte Modules) blieben unsichtbar. Jetzt beide Sprachen.
        rg --type kotlin --type java --regexp "$pattern" "$@" 2>/dev/null
    else
        grep -rn --include='*.kt' --include='*.java' "$pattern" "$@" . 2>/dev/null
    fi
}

# === Multi-Module-Unterstuetzung (FIX T7) ====================================
# Moderne Android-Apps haben oft mehrere Module mit eigenen strings.xml:
#   app/src/main/res/values/strings.xml          (Single-Module)
#   feature/auth/src/main/res/values/strings.xml (Feature-Modul)
#   core/ui/src/main/res/values/strings.xml      (Library-Modul)
# Diese Helper-Funktionen finden alle strings.xml-Dateien projektweit, nicht nur
# unter app/. Wenn der Auditor die alten 'app/src/main/'-Pfade noch braucht
# (Single-Module): er funktioniert weiterhin als Fallback.

# Findet alle Default-Locale strings.xml im Projekt (Multi-Module-faehig)
# FIX X2 (Audit 7): KMP-Projekte mit src/androidMain/res werden jetzt auch erkannt
find_default_strings_xml() {
    find . \( -path '*/src/main/res/values/strings.xml' -o -path '*/src/androidMain/res/values/strings.xml' \) -not -path '*/build/*' 2>/dev/null
}

# Findet alle Locale-Verzeichnisse (values-*) projektweit
# FIX X2 (Audit 7): KMP-Pfad ergaenzt
find_locale_dirs() {
    find . -type d \( -path '*/src/main/res/values-*' -o -path '*/src/androidMain/res/values-*' \) -not -path '*/build/*' 2>/dev/null
}

# Findet alle Locale-strings.xml (multi-module-faehig)
# FIX X2 (Audit 7): KMP-Pfad ergaenzt
find_translated_strings_xml() {
    find . \( -path '*/src/main/res/values-*/strings.xml' -o -path '*/src/androidMain/res/values-*/strings.xml' \) -not -path '*/build/*' 2>/dev/null
}

# Listet alle Modul-Wurzeln (Verzeichnisse die ein src/main/ ODER src/androidMain enthalten)
# FIX X2 (Audit 7): KMP-Module mit src/androidMain werden mitgezaehlt
find_module_roots() {
    find . -type d \( -path '*/src/main' -o -path '*/src/androidMain' \) -not -path '*/build/*' 2>/dev/null \
        | sed -E 's|/src/(main|androidMain)$||' | sort -u
}

# Findet die englische strings.xml in irgendeinem Modul (fuer EN-Vergleich)
# FIX X2 (Audit 7): KMP-Pfad ergaenzt
find_en_strings_xml() {
    find . \( -path '*/src/main/res/values-en/strings.xml' -o -path '*/src/androidMain/res/values-en/strings.xml' \) -not -path '*/build/*' 2>/dev/null | head -1
}

# Listet alle Regional-Varianten (z.B. pt-rBR, zh-rCN) projektweit
# FIX X2 (Audit 7): KMP-Pfade in find_locale_dirs werden automatisch mit gescannt
find_regional_locales() {
    find_locale_dirs | grep -oE 'values-[a-z]{2,3}-r[A-Z]{2}' | sed 's/values-//' | sort -u
}

# Findet eine spezifische Locale-strings.xml in irgendeinem Modul
# FIX X2 (Audit 7): KMP-Pfad ergaenzt
find_locale_strings_xml() {
    local loc="$1"
    find . \( -path "*/src/main/res/values-$loc/strings.xml" -o -path "*/src/androidMain/res/values-$loc/strings.xml" \) -not -path '*/build/*' 2>/dev/null | head -1
}
# =============================================================================

# Manifest finden (kann an mehreren Stellen liegen)
MANIFEST=""
for candidate in "app/src/main/AndroidManifest.xml" "src/main/AndroidManifest.xml" "AndroidManifest.xml"; do
    if [[ -f "$candidate" ]]; then
        MANIFEST="$candidate"
        break
    fi
done

if [[ -z "$MANIFEST" ]]; then
    # Fallback: erste Manifest-Datei suchen die nicht im build-Verzeichnis liegt
    MANIFEST=$(find . -name AndroidManifest.xml -not -path '*/build/*' 2>/dev/null | head -1)
fi

# Build-Gradle finden
GRADLE_FILE=""
for candidate in "app/build.gradle.kts" "app/build.gradle" "build.gradle.kts" "build.gradle"; do
    if [[ -f "$candidate" ]]; then
        GRADLE_FILE="$candidate"
        break
    fi
done

# Strings finden
STRINGS_XML=""
for candidate in "app/src/main/res/values/strings.xml" "src/main/res/values/strings.xml"; do
    if [[ -f "$candidate" ]]; then
        STRINGS_XML="$candidate"
        break
    fi
done

OUTPUT="app-roentgen-initial-scan.md"
DATE=$(date +%Y-%m-%d)

# Helper: zaehle Treffer fuer Pattern (gibt 0 zurueck bei Fehler)
count_grep() {
    # FIX T6: vorher nutzte count_grep direkt grep -rln und umging den GREP_R-Helper.
    # FIX X4 (Audit 7): Wie GREP_R jetzt auch Java mitscannen — Java-Legacy-SDKs
    # waren in der Capability-Tabelle (Section 2.4) unsichtbar.
    local pattern="$1"
    local count
    if [ "${HAS_RG:-0}" = "1" ]; then
        count=$(rg --type kotlin --type java -l "$pattern" 2>/dev/null | wc -l)
    else
        count=$(grep -rln --include='*.kt' --include='*.java' "$pattern" . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    fi
    echo "${count:-0}"
}

count_in_file() {
    local pattern="$1"
    local file="$2"
    if [[ ! -f "$file" ]]; then echo "0"; return; fi
    local count=$(grep -c "$pattern" "$file" 2>/dev/null)
    echo "${count:-0}"
}

# === Bericht schreiben ===
{
    echo "# App-Roentgen Initial-Scan"
    echo ""
    echo "**Datum:** $DATE"
    echo "**App-Verzeichnis:** $APP_DIR"
    echo "**Manifest:** ${MANIFEST:-NICHT GEFUNDEN}"
    echo "**Build-Gradle:** ${GRADLE_FILE:-NICHT GEFUNDEN}"
    echo "**Strings.xml:** ${STRINGS_XML:-NICHT GEFUNDEN}"
    echo ""
    echo "Dieser Initial-Scan ist die maschinelle Vorarbeit fuer die 7-Schichten-Tiefenanalyse."
    echo "Die Detail-Auswertung macht Claude in einem zweiten Schritt."
    echo ""
    echo "---"
    echo ""

    # === GROESSEN-ORDNUNG ===
    echo "## 0. App-Groesse"
    echo ""
    KOTLIN_FILES=$(find . -name '*.kt' -not -path '*/build/*' -not -path '*/test/*' 2>/dev/null | wc -l)
    JAVA_FILES=$(find . -name '*.java' -not -path '*/build/*' -not -path '*/test/*' 2>/dev/null | wc -l)
    XML_FILES=$(find . -name '*.xml' -path '*/res/*' -not -path '*/build/*' 2>/dev/null | wc -l)
    GRADLE_FILES=$(find . -name 'build.gradle*' -not -path '*/build/*' 2>/dev/null | wc -l)
    # FIX U4: find_module_roots wird jetzt produktiv genutzt — zeigt Multi-Module-Struktur
    MODULES=$(find_module_roots)
    MODULE_COUNT=$(echo "$MODULES" | grep -c '.' 2>/dev/null || echo 0)
    echo "| Datei-Typ | Anzahl |"
    echo "|-----------|--------|"
    echo "| Kotlin (.kt) | $KOTLIN_FILES |"
    echo "| Java (.java) | $JAVA_FILES |"
    echo "| Resource XML | $XML_FILES |"
    echo "| build.gradle* | $GRADLE_FILES |"
    echo "| Gradle-Module (mit src/main/) | $MODULE_COUNT |"
    echo ""
    if [[ "$MODULE_COUNT" -gt 1 ]]; then
        echo "**Multi-Module-App erkannt** ($MODULE_COUNT Module). Alle Layer-Scans wurden mit den Multi-Module-Helpern (find_*_strings_xml, find_locale_dirs, find_module_roots) durchgefuehrt — sie aggregieren ueber alle Module."
        echo ""
        echo "**Gefundene Module:**"
        echo "\`\`\`"
        echo "$MODULES"
        echo "\`\`\`"
        echo ""
    fi

    # === SCHICHT 1: MANIFEST ===
    echo "## Schicht 1 — Manifest-Daten"
    echo ""
    if [[ -n "$MANIFEST" ]] && [[ -f "$MANIFEST" ]]; then
        echo "### 1.1 Permissions"
        echo ""
        echo "\`\`\`"
        grep -oE 'android.permission.[A-Z_]+' "$MANIFEST" 2>/dev/null | sort -u || echo "(keine gefunden)"
        # Custom Permissions (z.B. com.android.vending.BILLING)
        grep -oE 'com\.[a-z\.]+\.permission\.[A-Z_]+' "$MANIFEST" 2>/dev/null | sort -u
        echo "\`\`\`"
        echo ""

        echo "### 1.2 Activities"
        echo ""
        echo "\`\`\`"
        grep -E 'android:name=' "$MANIFEST" 2>/dev/null | grep -i 'activity' | sed 's/.*android:name="\([^"]*\)".*/\1/' | sort -u || echo "(keine gefunden)"
        echo "\`\`\`"
        echo ""

        echo "### 1.3 Services"
        echo ""
        echo "\`\`\`"
        grep -B1 '<service' "$MANIFEST" 2>/dev/null | grep -oE 'android:name="[^"]*"' | sort -u || echo "(keine gefunden)"
        echo "\`\`\`"
        echo ""

        echo "### 1.4 Receiver"
        echo ""
        echo "\`\`\`"
        grep -B1 '<receiver' "$MANIFEST" 2>/dev/null | grep -oE 'android:name="[^"]*"' | sort -u || echo "(keine gefunden)"
        echo "\`\`\`"
        echo ""

        echo "### 1.5 Provider"
        echo ""
        echo "\`\`\`"
        grep -oE 'android:authorities="[^"]*"' "$MANIFEST" 2>/dev/null | sort -u || echo "(keine gefunden)"
        echo "\`\`\`"
        echo ""

        echo "### 1.6 Deep-Links und Intent-Filter-Schemes"
        echo ""
        echo "\`\`\`"
        grep -E 'data android:scheme=|data android:host=' "$MANIFEST" 2>/dev/null | sort -u || echo "(keine gefunden)"
        echo "\`\`\`"
        echo ""

        echo "### 1.7 Intent-Actions in der App"
        echo ""
        echo "\`\`\`"
        grep -oE 'action android:name="[^"]*"' "$MANIFEST" 2>/dev/null | sort -u | head -30 || echo "(keine gefunden)"
        echo "\`\`\`"
        echo ""

        echo "### 1.8 Backup-Konfig"
        echo ""
        BACKUP_LINE=$(grep -E 'allowBackup|fullBackupContent|dataExtractionRules' "$MANIFEST" 2>/dev/null | head -3)
        if [[ -n "$BACKUP_LINE" ]]; then
            echo "\`\`\`"
            echo "$BACKUP_LINE"
            echo "\`\`\`"
        else
            echo "(keine Backup-Konfig gefunden)"
        fi
        echo ""
    else
        echo "Kein AndroidManifest.xml gefunden — manueller Check noetig."
        echo ""
    fi

    # === SCHICHT 1.5: ASSETS-SCAN (FIN-014) ======================================
    # Scannt app/src/main/assets/ nach ausgelieferten Legal- und Web-Dokumenten.
    # Ergebnis fliesst in roentgen-report.json unter Key "layer1_5_assets".
    # Der Rechtssicherheits-Skill liest diesen Block VOR der missingDocs-Generierung
    # um False-Positives zu vermeiden (z.B. 81 legal HTML in 27 Sprachen = kein Fund).
    echo "## Schicht 1.5 — Assets-Scan (Legal- und Web-Dokumente)"
    echo ""

    # Hilfsfunktion: Doktyp-Detection aus Dateinamen (case-insensitive)
    _asset_doctype() {
        local fname
        fname=$(basename "$1" | tr '[:lower:]' '[:upper:]')
        case "$fname" in
            *DATENSCHUTZ*|*PRIVACY*|*PRIVACY_POLICY*) echo "privacy" ;;
            *IMPRESSUM*|*IMPRINT*|*LEGAL_NOTICE*)      echo "imprint" ;;
            *NUTZUNGSBEDINGUNGEN*|*TERMS*|*TOS*|*AGB*) echo "terms" ;;
            *HEALTH_DISCLAIMER*|*MEDICAL*|*HEALTH*)    echo "health-disclaimer" ;;
            *)                                          echo "other" ;;
        esac
    }

    # Hilfsfunktion: Sprach-Detection aus Pfad-Locale-Segment
    # Beispiel: assets/legal/de/DATENSCHUTZ.html -> "de"
    _asset_locale() {
        local p="$1"
        # Suche nach zweistelligem Locale-Segment im Pfad (z.B. /de/, /en/, /fr/)
        local loc
        loc=$(echo "$p" | grep -oE '/[a-z]{2}/' | head -1 | tr -d '/')
        if [[ -n "$loc" ]]; then
            echo "$loc"
        else
            echo "default"
        fi
    }

    ASSETS_DIR="app/src/main/assets"
    if [[ -d "$ASSETS_DIR" ]]; then
        echo "Assets-Verzeichnis: \`$ASSETS_DIR\`"
        echo ""

        # Alle relevanten Dateitypen finden (portabel mit find, kein Bash-Glob noetig)
        ASSET_FILES=$(find "$ASSETS_DIR" \( -name '*.html' -o -name '*.htm' -o -name '*.md' -o -name '*.txt' \) -type f 2>/dev/null | sort)

        if [[ -z "$ASSET_FILES" ]]; then
            echo "Keine html/htm/md/txt-Dateien in assets/ gefunden."
            echo ""
        else
            # Zaehl-Uebersicht
            ASSET_COUNT=$(echo "$ASSET_FILES" | wc -l | tr -d ' ')
            echo "Gefundene Dateien: $ASSET_COUNT"
            echo ""

            # Doktyp-Karten aufbauen (privacy, imprint, terms, health-disclaimer, other)
            declare -A PRIVACY_MAP IMPRINT_MAP TERMS_MAP HEALTH_MAP
            OTHER_LIST=""

            while IFS= read -r f; do
                doctype=$(_asset_doctype "$f")
                locale=$(_asset_locale "$f")
                rel="${f#./}"
                case "$doctype" in
                    privacy)           PRIVACY_MAP["$locale"]="$rel" ;;
                    imprint)           IMPRINT_MAP["$locale"]="$rel" ;;
                    terms)             TERMS_MAP["$locale"]="$rel" ;;
                    health-disclaimer) HEALTH_MAP["$locale"]="$rel" ;;
                    other)             OTHER_LIST="$OTHER_LIST\n  - $rel" ;;
                esac
            done <<< "$ASSET_FILES"

            echo "### 1.5.1 Privacy-Dokumente"
            if [[ ${#PRIVACY_MAP[@]} -gt 0 ]]; then
                for loc in "${!PRIVACY_MAP[@]}"; do echo "  [$loc] ${PRIVACY_MAP[$loc]}"; done | sort
            else
                echo "  (keine gefunden)"
            fi
            echo ""

            echo "### 1.5.2 Impressum-Dokumente"
            if [[ ${#IMPRINT_MAP[@]} -gt 0 ]]; then
                for loc in "${!IMPRINT_MAP[@]}"; do echo "  [$loc] ${IMPRINT_MAP[$loc]}"; done | sort
            else
                echo "  (keine gefunden)"
            fi
            echo ""

            echo "### 1.5.3 Terms/AGB-Dokumente"
            if [[ ${#TERMS_MAP[@]} -gt 0 ]]; then
                for loc in "${!TERMS_MAP[@]}"; do echo "  [$loc] ${TERMS_MAP[$loc]}"; done | sort
            else
                echo "  (keine gefunden)"
            fi
            echo ""

            echo "### 1.5.4 Health-Disclaimer-Dokumente"
            if [[ ${#HEALTH_MAP[@]} -gt 0 ]]; then
                for loc in "${!HEALTH_MAP[@]}"; do echo "  [$loc] ${HEALTH_MAP[$loc]}"; done | sort
            else
                echo "  (keine gefunden)"
            fi
            echo ""

            if [[ -n "$OTHER_LIST" ]]; then
                echo "### 1.5.5 Sonstige Assets"
                echo -e "$OTHER_LIST"
                echo ""
            fi

            # JSON-Output fuer roentgen-report.json (layer1_5_assets)
            # Wird vom Rechtssicherheits-Skill als missingDocs-Guard ausgewertet.
            echo "### 1.5.6 JSON-Export (layer1_5_assets)"
            echo ""
            echo '```json'
            echo '{'
            echo '  "layer1_5_assets": {'

            # privacy
            printf '    "privacy": {'
            FIRST=1
            for loc in $(echo "${!PRIVACY_MAP[@]}" | tr ' ' '\n' | sort); do
                [[ "$FIRST" = "0" ]] && printf ','
                printf '"%s": "%s"' "$loc" "${PRIVACY_MAP[$loc]}"
                FIRST=0
            done
            printf '},\n'

            # imprint
            printf '    "imprint": {'
            FIRST=1
            for loc in $(echo "${!IMPRINT_MAP[@]}" | tr ' ' '\n' | sort); do
                [[ "$FIRST" = "0" ]] && printf ','
                printf '"%s": "%s"' "$loc" "${IMPRINT_MAP[$loc]}"
                FIRST=0
            done
            printf '},\n'

            # terms
            printf '    "terms": {'
            FIRST=1
            for loc in $(echo "${!TERMS_MAP[@]}" | tr ' ' '\n' | sort); do
                [[ "$FIRST" = "0" ]] && printf ','
                printf '"%s": "%s"' "$loc" "${TERMS_MAP[$loc]}"
                FIRST=0
            done
            printf '},\n'

            # health-disclaimer
            printf '    "health-disclaimer": {'
            FIRST=1
            for loc in $(echo "${!HEALTH_MAP[@]}" | tr ' ' '\n' | sort); do
                [[ "$FIRST" = "0" ]] && printf ','
                printf '"%s": "%s"' "$loc" "${HEALTH_MAP[$loc]}"
                FIRST=0
            done
            printf '},\n'

            # other (array)
            printf '    "other": ['
            FIRST=1
            if [[ -n "$OTHER_LIST" ]]; then
                while IFS= read -r f; do
                    rel="${f#./}"
                    [[ "$FIRST" = "0" ]] && printf ','
                    printf '"%s"' "$rel"
                    FIRST=0
                done < <(echo "$ASSET_FILES" | while IFS= read -r f; do
                    [[ "$(_asset_doctype "$f")" = "other" ]] && echo "$f"
                done)
            fi
            printf ']\n'

            echo '  }'
            echo '}'
            echo '```'
            echo ""
        fi

        unset PRIVACY_MAP IMPRINT_MAP TERMS_MAP HEALTH_MAP
    else
        echo "Kein \`app/src/main/assets/\`-Verzeichnis gefunden — Assets-Scan uebersprungen."
        echo ""
    fi
    # === ENDE SCHICHT 1.5 ========================================================

    # === SCHICHT 2: DEPENDENCIES ===
    echo "## Schicht 2 — Dependencies"
    echo ""
    if [[ -n "$GRADLE_FILE" ]] && [[ -f "$GRADLE_FILE" ]]; then
        echo "### 2.1 Plugins"
        echo ""
        echo "\`\`\`"
        grep -E 'apply\(|alias\(|id\("' "$GRADLE_FILE" 2>/dev/null | grep -v '//' | head -20 || echo "(keine gefunden)"
        echo "\`\`\`"
        echo ""

        echo "### 2.2 Implementations / API"
        echo ""
        echo "\`\`\`"
        # FIX V3 (Audit 5): vorher 'find | xargs grep' — Spaces in Pfaden brachen den Aufruf
        # (SC2038). Jetzt mit -print0 + xargs -0 robust gegen Sonderzeichen in Dateinamen.
        find . -name 'build.gradle*' -not -path '*/build/*' -print0 2>/dev/null | xargs -0 grep -hE '^\s+(implementation|api|kapt|ksp)\s' 2>/dev/null | sort -u || echo "(keine gefunden)"
        echo "\`\`\`"
        echo ""
    fi

    if [[ -f "gradle/libs.versions.toml" ]]; then
        echo "### 2.3 Version-Catalog (libs.versions.toml)"
        echo ""
        echo "\`\`\`"
        grep -E '^[a-zA-Z]' gradle/libs.versions.toml | head -100 || echo "(leer)"
        echo "\`\`\`"
        echo ""
    fi

    echo "### 2.4 Capability-Indikatoren (zaehle Imports im Code)"
    echo ""
    echo "| Capability | Treffer |"
    echo "|-----------|---------|"
    echo "| Firebase Analytics | $(count_grep 'FirebaseAnalytics') |"
    echo "| Firebase Crashlytics | $(count_grep 'Crashlytics') |"
    echo "| Firebase Messaging (FCM Push) | $(count_grep 'FirebaseMessaging') |"
    echo "| Firebase Remote Config | $(count_grep 'RemoteConfig') |"
    echo "| Firebase Auth | $(count_grep 'FirebaseAuth') |"
    echo "| Firebase Firestore | $(count_grep 'FirebaseFirestore\|firestore') |"
    echo "| Firebase Functions | $(count_grep 'FirebaseFunctions\|httpsCallable') |"
    echo "| Google Play Billing | $(count_grep 'BillingClient\|ProductDetails') |"
    echo "| Google Play Review | $(count_grep 'ReviewManager') |"
    echo "| Room Database | $(count_grep '@Entity\|@Dao\|@Database') |"
    echo "| DataStore | $(count_grep 'preferencesDataStore\|stringPreferencesKey') |"
    echo "| WorkManager | $(count_grep 'CoroutineWorker\|class.*Worker') |"
    echo "| Hilt DI | $(count_grep '@HiltAndroidApp\|@HiltViewModel\|@Inject') |"
    echo "| Retrofit | $(count_grep 'Retrofit\.Builder\|@GET\|@POST') |"
    echo "| Ktor | $(count_grep 'HttpClient\|io\.ktor') |"
    echo "| Coil (Bilder) | $(count_grep 'AsyncImage\|coil') |"
    echo "| Compose Navigation | $(count_grep 'NavHost\|composable\(') |"
    echo "| ML Kit | $(count_grep 'mlkit') |"
    echo "| Google Gemini | $(count_grep 'GenerativeModel\|GeminiClient') |"
    echo "| OpenAI | $(count_grep 'OpenAI\|openai') |"
    echo "| Whisper | $(count_grep 'Whisper\|whisper') |"
    echo "| Health Connect | $(count_grep 'HealthConnect') |"
    echo "| Biometric | $(count_grep 'BiometricPrompt') |"
    echo "| AdMob | $(count_grep 'AdMob\|InterstitialAd\|RewardedAd\|BannerAd') |"
    echo ""

    # === SCHICHT 3: ARCHITEKTUR ===
    echo "## Schicht 3 — Architektur-Inventar"
    echo ""

    echo "### 3.1 ViewModels"
    echo ""
    echo "\`\`\`"
    grep -rln '@HiltViewModel\|class.*ViewModel\(' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | sort | head -50 || echo "(keine gefunden)"
    echo "\`\`\`"
    echo ""

    echo "### 3.2 Repositories"
    echo ""
    echo "\`\`\`"
    grep -rln 'class.*Repository\|interface.*Repository' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | sort | head -50 || echo "(keine gefunden)"
    echo "\`\`\`"
    echo ""

    echo "### 3.3 UseCases / Interactors"
    echo ""
    echo "\`\`\`"
    # FIX T5: vorher 'find . -name X -o -name Y -not -path ...' — POSIX-find bindet -not nur an
    # den letzten -name-Term. UseCase.kt-Dateien in build/test wurden ungefiltert mitgenommen.
    # Jetzt mit Klammern \( ... \): beide Name-Patterns werden gleich gefiltert.
    find . \( -name '*UseCase.kt' -o -name '*Interactor.kt' \) -not -path '*/build/*' -not -path '*/test/*' 2>/dev/null | sort | head -50 || echo "(keine gefunden)"
    echo "\`\`\`"
    echo ""

    echo "### 3.4 Hilt-Module"
    echo ""
    echo "\`\`\`"
    grep -rln '@Module' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | sort | head -30 || echo "(keine gefunden)"
    echo "\`\`\`"
    echo ""

    echo "### 3.5 Room Entities"
    echo ""
    echo "\`\`\`"
    grep -rln '@Entity\b' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | sort | head -30 || echo "(keine gefunden)"
    echo "\`\`\`"
    echo ""

    echo "### 3.6 Workers (Background)"
    echo ""
    echo "\`\`\`"
    grep -rln 'class.*Worker\b\|: CoroutineWorker\|: ListenableWorker' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | sort | head -30 || echo "(keine gefunden)"
    echo "\`\`\`"
    echo ""

    # === SCHICHT 4: SCREENS ===
    echo "## Schicht 4 — Bildschirme und Navigation"
    echo ""

    echo "### 4.1 Compose-Screens (Datei-Namen enden auf Screen.kt)"
    echo ""
    echo "\`\`\`"
    find . -name '*Screen.kt' -not -path '*/build/*' -not -path '*/test/*' 2>/dev/null | sort | head -100 || echo "(keine gefunden)"
    echo "\`\`\`"
    echo ""

    echo "### 4.2 Compose-Funktionen mit Uppercase-Namen (Screen/Dialog/Sheet/...)"
    echo ""
    echo "\`\`\`"
    # FIX R4 + X7 (Audit 7): vorher nur @Composable allein auf eigener Zeile gematched.
    # Same-line-Style `@Composable fun ScreenName(` (haeufigster Compose-Stil) wurde
    # uebersehen. Jetzt beide Patterns mit grep -E vereinigt.
    # Variante A: @Composable auf eigener Zeile, Funktion in Folgezeile
    # Variante B: @Composable + fun in derselben Zeile
    {
        grep -rEnA1 '^[[:space:]]*@Composable[[:space:]]*$' --include='*.kt' . 2>/dev/null \
            | grep -v '/build/' | grep -v '/test/' \
            | grep -oE 'fun [A-Z][A-Za-z0-9_]*\('
        grep -rEn '@Composable[[:space:]]+fun[[:space:]]+[A-Z]' --include='*.kt' . 2>/dev/null \
            | grep -v '/build/' | grep -v '/test/' \
            | grep -oE 'fun [A-Z][A-Za-z0-9_]*\('
    } | sed 's/fun \([^(]*\)(/\1/' | sort -u | head -50 \
        || echo "(keine gefunden — XML-only-App ohne Compose? Layer 4 nutzt klassische View-Patterns)"
    echo "\`\`\`"
    echo ""

    echo "### 4.3 Navigation-Targets"
    echo ""
    echo "\`\`\`"
    grep -rn 'composable(\|composable<' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | head -50 || echo "(keine gefunden)"
    echo "\`\`\`"
    echo ""

    echo "### 4.4 Navigation-Calls (.navigate)"
    echo ""
    NAVIGATE_COUNT=$(grep -rn '\.navigate(' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    echo "Anzahl navigate-Aufrufe: $NAVIGATE_COUNT"
    echo ""

    echo "### 4.5 Click-Handler-Anzahl"
    echo ""
    ONCLICK_COUNT=$(grep -rn 'onClick = {' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    CLICKABLE_COUNT=$(grep -rn '\.clickable {' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    LONGCLICK_COUNT=$(grep -rn 'onLongClick\|combinedClickable' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    echo "| Pattern | Treffer |"
    echo "|---------|---------|"
    echo "| onClick = { | $ONCLICK_COUNT |"
    echo "| .clickable { | $CLICKABLE_COUNT |"
    echo "| onLongClick / combinedClickable | $LONGCLICK_COUNT |"
    echo ""

    # === SCHICHT 5: PAYWALL ===
    echo "## Schicht 5 — Paywall-Hinweise"
    echo ""

    echo "### 5.1 Billing-relevante Dateien"
    echo ""
    echo "\`\`\`"
    grep -rln 'BillingClient\|ProductDetails\|SubscriptionOfferDetails' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | sort || echo "(keine gefunden)"
    echo "\`\`\`"
    echo ""

    echo "### 5.2 Paywall-/Premium-Bildschirme"
    echo ""
    echo "\`\`\`"
    find . \( -name '*Paywall*.kt' -o -name '*Premium*.kt' -o -name '*Subscription*.kt' -o -name '*Churn*.kt' \) -not -path '*/build/*' -not -path '*/test/*' 2>/dev/null | sort
    echo "\`\`\`"
    echo ""

    echo "### 5.3 Premium-Strings"
    echo ""
    if [[ -n "$STRINGS_XML" ]] && [[ -f "$STRINGS_XML" ]]; then
        echo "\`\`\`"
        grep -E '<string name="(paywall|premium|pro|trial|subscribe|upgrade|churn|cancel|billing)' "$STRINGS_XML" 2>/dev/null | head -50 || echo "(keine gefunden)"
        echo "\`\`\`"
    fi
    echo ""

    # === SCHICHT 6: HIDDEN FEATURES ===
    echo "## Schicht 6 — Hidden Features"
    echo ""

    echo "### 6.1 Worker-Klassen"
    echo ""
    echo "\`\`\`"
    grep -rln 'class.*Worker\b\|: CoroutineWorker' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | sort
    echo "\`\`\`"
    echo ""

    echo "### 6.2 Notification Channels"
    echo ""
    echo "\`\`\`"
    grep -rn 'NotificationChannel\|createNotificationChannel' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | head -20 || echo "(keine gefunden)"
    echo "\`\`\`"
    echo ""

    echo "### 6.3 Long-Click-Trigger (Debug-Menus?)"
    echo ""
    echo "\`\`\`"
    grep -rn 'setOnLongClickListener\|onLongClick\|combinedClickable' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | head -20 || echo "(keine gefunden)"
    echo "\`\`\`"
    echo ""

    echo "### 6.4 Feature-Flags / Remote Config Aufrufe"
    echo ""
    echo "\`\`\`"
    grep -rn 'remoteConfig\.\|FirebaseRemoteConfig\|isFeatureEnabled' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | head -30 || echo "(keine gefunden)"
    echo "\`\`\`"
    echo ""

    echo "### 6.5 Account-Loeschung (DSGVO)"
    echo ""
    echo "\`\`\`"
    grep -rn 'deleteAccount\|removeUser\|clearAllData\|gdprDelete' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | head -10 || echo "(KEINE ACCOUNT-DELETION GEFUNDEN — DSGVO/Play-Policy-Risiko!)"
    echo "\`\`\`"
    echo ""

    echo "### 6.6 Widgets / Tile / Shortcuts"
    echo ""
    echo "\`\`\`"
    WIDGETS=$(grep -rln 'AppWidgetProvider' --include='*.kt' --include='*.java' . 2>/dev/null | grep -v '/build/' | head -5)
    TILES=$(grep -rln 'TileService' --include='*.kt' . 2>/dev/null | grep -v '/build/' | head -5)
    SHORTCUTS=$(find . -name 'shortcuts.xml' -path '*/res/xml/*' 2>/dev/null | head -5)
    echo "Widgets:    ${WIDGETS:-(keine)}"
    echo "Tiles:      ${TILES:-(keine)}"
    echo "Shortcuts:  ${SHORTCUTS:-(keine)}"
    echo "\`\`\`"
    echo ""

    # === SCHICHT 7: WERBEAUSSAGEN-VOR-AUDIT ===
    echo "## Schicht 7 — Werbeaussagen-Vor-Audit"
    echo ""

    if [[ -n "$STRINGS_XML" ]] && [[ -f "$STRINGS_XML" ]]; then
        TOTAL_STRINGS=$(grep -c '<string name=' "$STRINGS_XML" 2>/dev/null | tr -d '[:space:]')
        echo "Gesamt-Strings (Hauptsprache): $TOTAL_STRINGS"
        echo ""

        # Sprachen
        echo "### 7.1 Sprach-Varianten"
        echo ""
        echo "\`\`\`"
        # FIX W3 (Audit 6): Legacy-ls-Fallback entfernt — find_locale_dirs deckt Single-Module
        # bereits ab (findet auch app/src/main/res/values-*). Der ls war ein Duplikat.
        find_locale_dirs | sort -u || echo "(keine uebersetzten Sprachen)"
        echo "\`\`\`"
        echo ""

        echo "### 7.2 KRITISCH-Keywords (in Hauptsprache)"
        echo ""
        echo "\`\`\`"
        grep -iE '(unlimited|unbegrenzt|all features|alle Features|always|immer|24/7|forever|lifetime|lebenslang|niemals|never)' "$STRINGS_XML" 2>/dev/null | head -30 || echo "(keine kritischen Keywords gefunden)"
        echo "\`\`\`"
        echo ""

        echo "### 7.3 HOCH-Keywords"
        echo ""
        echo "\`\`\`"
        grep -iE '(\bAI\b|\bKI\b|smart|intelligent|offline|private|privat|secure|sicher|ad-free|werbefrei|encrypted|verschluesselt|kostenlos|gratis)' "$STRINGS_XML" 2>/dev/null | head -50 || echo "(keine HOCH-Keywords gefunden)"
        echo "\`\`\`"
        echo ""

        echo "### 7.4 MITTEL-Keywords"
        echo ""
        echo "\`\`\`"
        grep -iE '(premium|\bpro\b|best|fastest|schnellste|complete|exclusive|professional|profi)' "$STRINGS_XML" 2>/dev/null | head -30 || echo "(keine MITTEL-Keywords gefunden)"
        echo "\`\`\`"
        echo ""
    else
        echo "Keine strings.xml gefunden — manueller Check noetig."
        echo ""
    fi

    # === SCHICHT 4b: WORTLAUT-INDEX ===
    echo "## Schicht 4b — Wortlaut-Index pro Bereich"
    echo ""
    echo "Dieser Block ist die Vorarbeit fuer das 1:1-Wortlaut-Mapping. Die Tiefenauswertung pro Bereich folgt durch Claude."
    echo ""

    # 4b.1 String-Resources global
    if [[ -n "$STRINGS_XML" ]] && [[ -f "$STRINGS_XML" ]]; then
        echo "### 4b.1 Globale Resource-Zaehlung"
        echo ""
        STR_COUNT=$(grep -c '<string name=' "$STRINGS_XML" 2>/dev/null | tr -d '[:space:]')
        PLR_COUNT=$(grep -c '<plurals name=' "$STRINGS_XML" 2>/dev/null | tr -d '[:space:]')
        ARR_COUNT=$(grep -c '<string-array name=' "$STRINGS_XML" 2>/dev/null | tr -d '[:space:]')
        echo "| Resource-Typ | Anzahl in Hauptsprache |"
        echo "|--------------|------------------------|"
        echo "| <string> | $STR_COUNT |"
        echo "| <plurals> | $PLR_COUNT |"
        echo "| <string-array> | $ARR_COUNT |"
        echo ""

        echo "### 4b.2 String-Anzahl pro Sprache"
        echo ""
        echo "\`\`\`"
        # FIX W3 (Audit 6): Legacy-ls-Fallback entfernt — find_translated_strings_xml +
        # find_default_strings_xml decken Single-Module bereits ab.
        for f in $({ find_translated_strings_xml; find_default_strings_xml; } | sort -u); do
            COUNT=$(grep -c '<string name=' "$f" 2>/dev/null | tr -d '[:space:]')
            printf "%-60s %s\n" "$f" "$COUNT"
        done
        echo "\`\`\`"
        echo ""
    fi

    # 4b.3 String-Resource-Aufrufe im Code
    echo "### 4b.3 String-Resource-Nutzungen im Code"
    echo ""
    COMPOSE_STR=$(grep -rn 'stringResource(\s*R\.string\.' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    COMPOSE_PLR=$(grep -rn 'pluralStringResource(\s*R\.plurals\.' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    COMPOSE_ARR=$(grep -rn 'stringArrayResource(\s*R\.array\.' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    CLASSIC_STR=$(grep -rn 'getString(\s*R\.string\.' --include='*.kt' --include='*.java' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    CLASSIC_PLR=$(grep -rn 'getQuantityString(\s*R\.plurals\.' --include='*.kt' --include='*.java' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    XML_STR=$(grep -rn 'android:text="@string/\|android:hint="@string/\|android:contentDescription="@string/' --include='*.xml' res/ 2>/dev/null | wc -l)
    echo "| Aufruf-Typ | Treffer |"
    echo "|-----------|---------|"
    echo "| Compose stringResource | $COMPOSE_STR |"
    echo "| Compose pluralStringResource | $COMPOSE_PLR |"
    echo "| Compose stringArrayResource | $COMPOSE_ARR |"
    echo "| Klassisch getString | $CLASSIC_STR |"
    echo "| Klassisch getQuantityString | $CLASSIC_PLR |"
    echo "| XML @string-References | $XML_STR |"
    echo ""

    # 4b.4 Bereiche
    echo "### 4b.4 UI-Bereiche fuer Wortlaut-Mapping"
    echo ""

    # Dialoge
    COMPOSE_DLG=$(grep -rln 'AlertDialog(\|BasicAlertDialog(' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    BUILDER_DLG=$(grep -rln 'AlertDialog\.Builder\|MaterialAlertDialogBuilder' --include='*.kt' --include='*.java' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    BOTTOM_SHEET=$(grep -rln 'ModalBottomSheet(\|BottomSheetScaffold' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    SNACKBAR=$(grep -rn 'snackbarHostState\.showSnackbar\|Snackbar\.make' --include='*.kt' --include='*.java' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    TOAST=$(grep -rn 'Toast\.makeText(' --include='*.kt' --include='*.java' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    NOTIF=$(grep -rn '\.setContentTitle(\|\.setContentText(' --include='*.kt' --include='*.java' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    echo "| Bereich | Treffer |"
    echo "|---------|---------|"
    echo "| Compose Dialoge | $COMPOSE_DLG |"
    echo "| Klassische Dialog-Builder | $BUILDER_DLG |"
    echo "| Bottom Sheets | $BOTTOM_SHEET |"
    echo "| Snackbar-Aufrufe | $SNACKBAR |"
    echo "| Toast-Aufrufe | $TOAST |"
    echo "| Notification-Inhalte (Title/Text) | $NOTIF |"
    echo ""

    # 4b.5 Menue-Hierarchien
    echo "### 4b.5 Menue- und Settings-Hierarchien"
    echo ""
    MENU_XML_COUNT=$(find . -path '*/res/menu/*.xml' -not -path '*/build/*' 2>/dev/null | wc -l)
    PREF_XML_COUNT=$(find . -path '*/res/xml/*.xml' -not -path '*/build/*' 2>/dev/null | wc -l)
    DD_MENU=$(grep -rln 'DropdownMenu(' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    NAV_BAR=$(grep -rln 'NavigationBar(\|NavigationBarItem(' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    DRAWER=$(grep -rln 'NavigationDrawer\|ModalDrawerSheet\|NavigationDrawerItem' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    echo "| Komponente | Dateien |"
    echo "|-----------|---------|"
    echo "| res/menu/*.xml | $MENU_XML_COUNT |"
    echo "| res/xml/*.xml (Preferences) | $PREF_XML_COUNT |"
    echo "| Compose DropdownMenu | $DD_MENU |"
    echo "| NavigationBar/Item | $NAV_BAR |"
    echo "| NavigationDrawer | $DRAWER |"
    echo ""

    echo "#### Menue-Dateien (komplette Liste)"
    echo ""
    echo "\`\`\`"
    find . -path '*/res/menu/*.xml' -not -path '*/build/*' 2>/dev/null | sort
    echo "\`\`\`"
    echo ""

    echo "#### Preference-Dateien (komplette Liste)"
    echo ""
    echo "\`\`\`"
    find . -path '*/res/xml/*.xml' -not -path '*/build/*' 2>/dev/null | sort
    echo "\`\`\`"
    echo ""

    # 4b.6 Hardcoded Strings (sollten 0 sein!)
    echo "### 4b.6 Hardcoded Strings (Warnung wenn >0)"
    echo ""
    HC_TEXT=$(grep -rn 'Text(\s*"[A-Za-zÄÖÜäöü0-9]' --include='*.kt' . 2>/dev/null | grep -v '/test/' | grep -v '/build/' | grep -v 'Text(text =' | wc -l)
    HC_SETTEXT=$(grep -rn '\.setText("[A-Za-zÄÖÜäöü0-9][^"]\{3,\}"' --include='*.kt' --include='*.java' . 2>/dev/null | grep -v '/test/' | wc -l)
    HC_TOAST=$(grep -rn 'Toast\.makeText([^,]*,\s*"' --include='*.kt' --include='*.java' . 2>/dev/null | grep -v '/test/' | wc -l)
    HC_SNACK=$(grep -rn 'showSnackbar(\s*"' --include='*.kt' . 2>/dev/null | grep -v '/test/' | wc -l)
    HC_DLG=$(grep -rn '\.setTitle("\|\.setMessage("' --include='*.kt' --include='*.java' . 2>/dev/null | grep -v '/test/' | wc -l)
    echo "| Pattern | Treffer |"
    echo "|---------|---------|"
    echo "| Compose Text(\"literal\") | $HC_TEXT |"
    echo "| View .setText(\"literal\") | $HC_SETTEXT |"
    echo "| Toast mit Literal | $HC_TOAST |"
    echo "| showSnackbar mit Literal | $HC_SNACK |"
    echo "| Dialog .setTitle/.setMessage Literal | $HC_DLG |"
    echo ""
    echo "Hinweis: Jeder Treffer >0 ist eine nicht-internationalisierte Werbeaussage-Quelle und MUSS in 4b zitiert werden."
    echo ""

    # 4b.7 Tote / Fehlende Keys (max je 30 zeigen)
    if [[ -n "$STRINGS_XML" ]] && [[ -f "$STRINGS_XML" ]]; then
        echo "### 4b.7 Tote Keys (in strings.xml definiert, im Code nicht verwendet)"
        echo ""
        echo "\`\`\`"
        TMP_KEYS=$(mktemp 2>/dev/null || echo "/tmp/keys.$$")
        grep -oE '<string name="[^"]+"' "$STRINGS_XML" 2>/dev/null | sed 's/<string name="//' | sed 's/"$//' | sort -u > "$TMP_KEYS"
        DEAD_COUNT=0
        while read -r k; do
            if ! grep -rq "R\.string\.$k\b" --include='*.kt' --include='*.java' --include='*.xml' . 2>/dev/null; then
                echo "$k"
                DEAD_COUNT=$((DEAD_COUNT + 1))
                if [[ $DEAD_COUNT -ge 30 ]]; then
                    echo "(... weitere abgeschnitten — siehe Vollscan)"
                    break
                fi
            fi
        done < "$TMP_KEYS"
        rm -f "$TMP_KEYS" 2>/dev/null
        if [[ $DEAD_COUNT -eq 0 ]]; then echo "(keine toten Keys)"; fi
        echo "\`\`\`"
        echo ""

        echo "### 4b.8 Fehlende Keys (im Code referenziert, in strings.xml nicht definiert)"
        echo ""
        echo "\`\`\`"
        TMP_USED=$(mktemp 2>/dev/null || echo "/tmp/used.$$")
        grep -rohE 'R\.string\.[a-zA-Z0-9_]+' --include='*.kt' --include='*.java' . 2>/dev/null | sed 's/R\.string\.//' | sort -u > "$TMP_USED"
        MISS_COUNT=0
        while read -r k; do
            if [[ -n "$k" ]] && ! grep -q "<string name=\"$k\"" "$STRINGS_XML" 2>/dev/null; then
                echo "$k"
                MISS_COUNT=$((MISS_COUNT + 1))
                if [[ $MISS_COUNT -ge 30 ]]; then
                    echo "(... weitere abgeschnitten — siehe Vollscan)"
                    break
                fi
            fi
        done < "$TMP_USED"
        rm -f "$TMP_USED" 2>/dev/null
        if [[ $MISS_COUNT -eq 0 ]]; then echo "(keine fehlenden Keys)"; fi
        echo "\`\`\`"
        echo ""
    fi

    # === SCHICHT 4c: TRANSLATION-CONTEXT ===
    echo "## Schicht 4c — Translation-Context"
    echo ""
    echo "Vorarbeit fuer den Uebersetzungs-Skill. Die Tiefenauswertung folgt durch Claude."
    echo ""

    if [[ -n "$STRINGS_XML" ]] && [[ -f "$STRINGS_XML" ]]; then
        # 4c.1 translatable=false
        echo "### 4c.1 Nicht-uebersetzbare Strings"
        echo ""
        UNTRANS=$(grep -c 'translatable="false"' "$STRINGS_XML" 2>/dev/null | tr -d '[:space:]')
        echo "**Anzahl:** $UNTRANS"
        echo ""
        if [[ "$UNTRANS" -gt 0 ]]; then
            echo "\`\`\`"
            grep 'translatable="false"' "$STRINGS_XML" | sed 's/^[[:space:]]*//' | head -30
            echo "\`\`\`"
        fi
        echo ""

        # 4c.2 xliff:g-Tags
        echo "### 4c.2 xliff:g-Tags (Inline-Schutz)"
        echo ""
        XLIFF_DECL=$(grep -c 'xmlns:xliff' "$STRINGS_XML" 2>/dev/null | tr -d '[:space:]')
        XLIFF_USES=$(grep -c '<xliff:g' "$STRINGS_XML" 2>/dev/null | tr -d '[:space:]')
        FORMAT_NO_XLIFF=$(grep -E '<string[^>]*>[^<]*%[0-9]\$[sdf]' "$STRINGS_XML" 2>/dev/null | grep -v 'xliff:g' | wc -l)
        echo "| Metrik | Wert |"
        echo "|--------|------|"
        echo "| xmlns:xliff deklariert | $([[ $XLIFF_DECL -gt 0 ]] && echo "JA" || echo "NEIN") |"
        echo "| xliff:g-Tags verwendet | $XLIFF_USES |"
        echo "| Format-Strings OHNE xliff:g (Kandidaten) | $FORMAT_NO_XLIFF |"
        echo ""
        if [[ "$XLIFF_USES" -gt 0 ]]; then
            echo "**Beispiele:**"
            echo "\`\`\`"
            grep -E '<xliff:g' "$STRINGS_XML" | head -10
            echo "\`\`\`"
            echo ""
        fi

        # 4c.3 Uebersetzer-Notizen (XML-Kommentare vor String-Tags)
        echo "### 4c.3 Uebersetzer-Notizen (XML-Kommentare)"
        echo ""
        TOTAL_STRINGS=$(grep -c '<string name=' "$STRINGS_XML" 2>/dev/null | tr -d '[:space:]')
        COMMENTED=$(awk 'BEGIN{c=0; total=0} /<!--/{flag=1; next} /<string name=/{if(flag) c++; flag=0; total++} /^[[:space:]]*[^<!]/{flag=0} END{print c}' "$STRINGS_XML" 2>/dev/null | tr -d '[:space:]')
        if [[ "$TOTAL_STRINGS" -gt 0 ]]; then
            PCT=$(awk "BEGIN { printf \"%.1f\", $COMMENTED * 100 / $TOTAL_STRINGS }")
        else
            PCT="0"
        fi
        echo "**Strings mit vorangestelltem Kommentar:** $COMMENTED / $TOTAL_STRINGS ($PCT%)"
        echo ""

        # 4c.4 Plural-Audit
        echo "### 4c.4 Plural-Resources und CLDR-Vollstaendigkeit"
        echo ""
        PLURAL_KEYS=$(grep -oE '<plurals name="[^"]+"' "$STRINGS_XML" 2>/dev/null | sed 's/<plurals name="//' | sed 's/"$//' | sort -u)
        PLURAL_COUNT=$(echo "$PLURAL_KEYS" | grep -c . || echo 0)
        echo "**Anzahl Plural-Keys:** $PLURAL_COUNT"
        echo ""

        if [[ "$PLURAL_COUNT" -gt 0 ]]; then
            echo "**Quantitaeten pro Sprache und Plural-Key:**"
            echo ""
            echo "\`\`\`"
            # FIX T7: Multi-Module-faehig
            for lang_dir in $({ find_locale_dirs; find . -type d -path '*/src/main/res/values' -not -path '*/build/*' 2>/dev/null; ls -d app/src/main/res/values*/ 2>/dev/null; } | sort -u); do
                lang=$(basename "$lang_dir" | sed 's/values//' | sed 's/^-//')
                [[ -z "$lang" ]] && lang="default"
                f="$lang_dir/strings.xml"
                [[ ! -f "$f" ]] && continue
                for key in $PLURAL_KEYS; do
                    QUANTITIES=$(awk "/<plurals name=\"$key\"/,/<\/plurals>/" "$f" 2>/dev/null | grep -oE 'quantity="[^"]+"' | sed 's/quantity="//;s/"//' | sort -u | tr '\n' ',' | sed 's/,$//')
                    [[ -z "$QUANTITIES" ]] && QUANTITIES="(fehlt)"
                    printf "%-15s | %-40s | %s\n" "$lang" "$key" "$QUANTITIES"
                done
            done | head -200
            echo "\`\`\`"
            echo ""
            echo "**CLDR-Referenz:**"
            echo "- de/en/es/it/nl/pt/tr/zh/ja/ko: \`one, other\`"
            echo "- fr/pt-rBR: \`one, many, other\`"
            echo "- ru/uk/pl/cs/sk: \`one, few, many, other\`"
            echo "- ar: \`zero, one, two, few, many, other\`"
            echo ""
        fi

        # 4c.5 HTML/CDATA in Strings
        echo "### 4c.5 HTML / CDATA in Strings"
        echo ""
        HTML_STR=$(grep -cE '<string[^>]*>[^<]*<(b|i|u|br|a|font|strong|em|span|p)\b' "$STRINGS_XML" 2>/dev/null | tr -d '[:space:]')
        CDATA_STR=$(grep -c '<!\[CDATA\[' "$STRINGS_XML" 2>/dev/null | tr -d '[:space:]')
        ENTITY_STR=$(grep -cE '&(lt|gt|amp|quot|apos|nbsp|#[0-9]+);' "$STRINGS_XML" 2>/dev/null | tr -d '[:space:]')
        echo "| Metrik | Wert |"
        echo "|--------|------|"
        echo "| Strings mit HTML-Tags | $HTML_STR |"
        echo "| Strings mit CDATA | $CDATA_STR |"
        echo "| Strings mit HTML-Entities | $ENTITY_STR |"
        echo ""

        # 4c.6 Format-Argumente
        echo "### 4c.6 Format-Argumente"
        echo ""
        POSITIONAL_FMT=$(grep -cE '%[0-9]+\$[sdf]' "$STRINGS_XML" 2>/dev/null | tr -d '[:space:]')
        GENERIC_FMT=$(grep -cE '<string[^>]*>[^<]*%[sdf]' "$STRINGS_XML" 2>/dev/null | tr -d '[:space:]')
        echo "| Format-Typ | Anzahl |"
        echo "|-----------|--------|"
        echo "| Positional (%1\$s, %2\$d) | $POSITIONAL_FMT |"
        echo "| Generic (%s, %d) | $GENERIC_FMT |"
        echo ""

        # 4c.7 Glossar — Top deutsche Substantive
        echo "### 4c.7 Glossar-Kandidaten (Top-30 Begriffe)"
        echo ""
        echo "**Top-30 deutsche Substantive (in Strings, Vorkommen):**"
        echo ""
        echo "\`\`\`"
        grep -oE '<string name="[^"]+">[^<]+</string>' "$STRINGS_XML" 2>/dev/null | \
          sed 's/<[^>]*>//g' | grep -oE '\b[A-ZÄÖÜ][a-zäöüß]+\b' | \
          sort | uniq -c | sort -rn | head -30
        echo "\`\`\`"
        echo ""

        # FIX U4: Multi-Module-faehig — sucht englische strings.xml in allen Modulen
        EN_FILE=$(find_en_strings_xml)
        EN_FILE="${EN_FILE:-app/src/main/res/values-en/strings.xml}"
        if [[ -f "$EN_FILE" ]]; then
            echo "**Top-30 englische Schlagwoerter (lowercase, >3 Zeichen):**"
            echo ""
            echo "\`\`\`"
            grep -oE '<string name="[^"]+">[^<]+</string>' "$EN_FILE" 2>/dev/null | \
              sed 's/<[^>]*>//g' | tr ' ' '\n' | tr -cd '[:alpha:]\n' | \
              awk '{ print tolower($0) }' | grep -E '^.{4,}$' | \
              sort | uniq -c | sort -rn | head -30
            echo "\`\`\`"
            echo ""
        fi

        # 4c.8 Region-Differenzen — direkter Paarvergleich, nicht ueber Base-Variante
        echo "### 4c.8 Region-Differenzen (Verdacht fehlende Lokalisierung)"
        echo ""
        # Alle Sprachen mit Regional-Code finden, dann nach Basissprache gruppieren
        # FIX U4: Multi-Module-faehig via find_regional_locales (statt 'ls' das nur app/ scannt)
        REGIONAL_LANGS=$(find_regional_locales)
        if [[ -n "$REGIONAL_LANGS" ]]; then
            # Basissprache extrahieren und Varianten gruppieren
            BASE_LANGS=$(echo "$REGIONAL_LANGS" | sed 's/-r.*//' | sort -u)
            FOUND_PAIR=0
            for base in $BASE_LANGS; do
                # Alle Varianten dieser Basissprache
                VARIANTS=$(echo "$REGIONAL_LANGS" | grep "^${base}-r" | sort)
                VAR_COUNT=$(echo "$VARIANTS" | wc -l)
                if [[ "$VAR_COUNT" -ge 2 ]]; then
                    if [[ "$FOUND_PAIR" -eq 0 ]]; then
                        echo "| Sprach-Paar | Strings im 1. | Strings im 2. | Diff-Zeilen | Status |"
                        echo "|-------------|---------------|---------------|-------------|--------|"
                        FOUND_PAIR=1
                    fi
                    # Vergleiche jedes Paar
                    VAR_ARR=($VARIANTS)
                    for ((i=0; i<${#VAR_ARR[@]}; i++)); do
                        for ((j=i+1; j<${#VAR_ARR[@]}; j++)); do
                            VA="${VAR_ARR[$i]}"
                            VB="${VAR_ARR[$j]}"
                            # FIX U4: Multi-Module-faehig via find_locale_strings_xml
                            FA=$(find_locale_strings_xml "$VA"); FA="${FA:-app/src/main/res/values-$VA/strings.xml}"
                            FB=$(find_locale_strings_xml "$VB"); FB="${FB:-app/src/main/res/values-$VB/strings.xml}"
                            [[ ! -f "$FA" ]] || [[ ! -f "$FB" ]] && continue
                            CA=$(grep -c '<string name=' "$FA" 2>/dev/null | tr -d '[:space:]')
                            CB=$(grep -c '<string name=' "$FB" 2>/dev/null | tr -d '[:space:]')
                            DIFFS=$(diff <(grep -oE '<string name="[^"]+">[^<]+' "$FA" 2>/dev/null | sort) \
                                          <(grep -oE '<string name="[^"]+">[^<]+' "$FB" 2>/dev/null | sort) 2>/dev/null | wc -l)
                            if [[ "$DIFFS" -lt 20 ]] && [[ "$CA" -gt 100 ]]; then
                                STATUS="⚠ Verdacht: zu wenig Unterschied (nur $DIFFS Diff-Zeilen)"
                            else
                                STATUS="OK ($DIFFS Diff-Zeilen)"
                            fi
                            echo "| $VA vs $VB | $CA | $CB | $DIFFS | $STATUS |"
                        done
                    done
                fi
            done
            if [[ "$FOUND_PAIR" -eq 0 ]]; then
                echo "(Regional-Varianten gefunden, aber keine paarweisen Varianten derselben Basissprache)"
                echo ""
                echo "Gefundene Regional-Varianten: $(echo "$REGIONAL_LANGS" | tr '\n' ' ')"
            fi
        else
            echo "(keine Regional-Varianten gefunden)"
        fi
        echo ""

        # 4c.9 Du/Sie-Konsistenz fuer Deutsch
        echo "### 4c.9 Du/Sie-Konsistenz (Deutsch)"
        echo ""
        DU_COUNT=$(grep -cE '\b(du|dein|deine|deinem|deinen|deiner|dir|dich)\b' "$STRINGS_XML" 2>/dev/null | tr -d '[:space:]')
        SIE_COUNT=$(grep -cE '\b(Sie|Ihr|Ihre|Ihrem|Ihren|Ihrer|Ihnen)\b' "$STRINGS_XML" 2>/dev/null | tr -d '[:space:]')
        echo "| Anrede-Form | Treffer |"
        echo "|------------|---------|"
        echo "| Du-Form (du/dein/dir/dich) | $DU_COUNT |"
        echo "| Sie-Form (Sie/Ihr/Ihnen) | $SIE_COUNT |"
        echo ""
        if [[ "$DU_COUNT" -gt 0 ]] && [[ "$SIE_COUNT" -gt 0 ]]; then
            echo "**⚠ MISCHANREDE erkannt** — beide Anredeformen kommen vor. Beispiel-Zeilen:"
            echo ""
            echo "\`\`\`"
            echo "[Du-Beispiele]"
            grep -nE '\b(du|dein|deine)\b' "$STRINGS_XML" 2>/dev/null | head -5
            echo ""
            echo "[Sie-Beispiele]"
            grep -nE '\b(Sie|Ihr|Ihre|Ihnen)\b' "$STRINGS_XML" 2>/dev/null | head -5
            echo "\`\`\`"
        else
            echo "**OK** — durchgaengige Anrede."
        fi
        echo ""

        # 4c.10 Slot-Laengen-Audit (nur Buttons + Push-Titles als Stichprobe)
        echo "### 4c.10 Slot-Laengen-Stichprobe"
        echo ""
        echo "Format pro Zeile: \`<Laenge> | <Key> | \"<Wortlaut>\"\` — sortiert nach Laenge"
        echo ""
        echo "**Laengste Buttons / CTAs in Hauptsprache (Top 15, Soft-Limit ~18 Zeichen DE):**"
        echo "\`\`\`"
        # Python-Helper fuer praezise Extraktion (key + textinhalt)
        python3 - "$STRINGS_XML" <<'PY' 2>/dev/null || echo "(Python nicht verfuegbar — bitte manuell pruefen)"
import sys, re
path = sys.argv[1]
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()
# regex: <string name="..." ...>TEXT</string>, key matched gegen button/cta/action
pattern = re.compile(r'<string\s+name="([^"]*(?:button|cta|action)[^"]*)"[^>]*>(.*?)</string>', re.DOTALL)
items = []
for m in pattern.finditer(content):
    key, text = m.group(1), m.group(2)
    # HTML-Tags und xliff:g-Wrapper entfernen fuer reine Laenge
    plain = re.sub(r'<[^>]+>', '', text)
    items.append((len(plain), key, plain))
items.sort(key=lambda x: -x[0])
for length, key, text in items[:15]:
    print(f'{length:3d} | {key:50s} | "{text}"')
PY
        echo "\`\`\`"
        echo ""

        echo "**Laengste Push-Notification-Titles in Hauptsprache (Top 10, Soft-Limit ~65 Zeichen DE):**"
        echo "\`\`\`"
        python3 - "$STRINGS_XML" <<'PY' 2>/dev/null || echo "(Python nicht verfuegbar — bitte manuell pruefen)"
import sys, re
path = sys.argv[1]
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()
pattern = re.compile(r'<string\s+name="([^"]*notif[^"]*title[^"]*)"[^>]*>(.*?)</string>', re.DOTALL)
items = []
for m in pattern.finditer(content):
    key, text = m.group(1), m.group(2)
    plain = re.sub(r'<[^>]+>', '', text)
    items.append((len(plain), key, plain))
items.sort(key=lambda x: -x[0])
for length, key, text in items[:10]:
    print(f'{length:3d} | {key:50s} | "{text}"')
PY
        echo "\`\`\`"
        echo ""

        echo "**Laengste TopBar-Titles in Hauptsprache (Top 10, Soft-Limit ~25 Zeichen DE):**"
        echo "\`\`\`"
        python3 - "$STRINGS_XML" <<'PY' 2>/dev/null || echo "(Python nicht verfuegbar — bitte manuell pruefen)"
import sys, re
path = sys.argv[1]
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()
pattern = re.compile(r'<string\s+name="([^"]*(?:topbar|app_bar|toolbar|title)[^"]*)"[^>]*>(.*?)</string>', re.DOTALL)
items = []
for m in pattern.finditer(content):
    key, text = m.group(1), m.group(2)
    plain = re.sub(r'<[^>]+>', '', text)
    items.append((len(plain), key, plain))
items.sort(key=lambda x: -x[0])
for length, key, text in items[:10]:
    print(f'{length:3d} | {key:50s} | "{text}"')
PY
        echo "\`\`\`"
        echo ""
    fi

    # === SCHICHT 4d: LEGAL-TEXT-INVENTAR ===
    echo "## Schicht 4d — Legal-Text-Inventar"
    echo ""
    echo "Vorarbeit fuer den Rechtssicherheits-Skill."
    echo ""

    # 4d.1 Permission-Rationale
    echo "### 4d.1 Permission-Rationale-Indikatoren"
    echo ""
    RATIONALE_CODE=$(grep -rln 'rememberPermissionState\|rememberMultiplePermissionsState\|shouldShowRequestPermissionRationale' --include='*.kt' --include='*.java' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    RATIONALE_STRINGS=0
    if [[ -n "$STRINGS_XML" ]] && [[ -f "$STRINGS_XML" ]]; then
        RATIONALE_STRINGS=$(grep -cE '<string name="[^"]*(?:permission|rationale|perm_)[^"]*"' "$STRINGS_XML" 2>/dev/null | tr -d '[:space:]')
    fi
    echo "| Indikator | Anzahl |"
    echo "|-----------|--------|"
    echo "| Dateien mit Runtime-Permission-Aufrufen | $RATIONALE_CODE |"
    echo "| Permission-/Rationale-Strings in strings.xml | $RATIONALE_STRINGS |"
    echo ""

    # 4d.2 Consent-Banner
    echo "### 4d.2 Consent-Banner-Indikatoren"
    echo ""
    UMP=$(grep -rln 'UserMessagingPlatform\|ConsentInformation' --include='*.kt' --include='*.java' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    FB_CONSENT=$(grep -rln 'setAnalyticsCollectionEnabled\|setConsent\|FirebaseAnalytics.*setConsent' --include='*.kt' --include='*.java' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    CUSTOM_CONSENT=$(grep -rln 'ConsentScreen\|ConsentDialog\|TrackingConsent\|CookieBanner\|GdprConsent' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    CONSENT_STRINGS=0
    if [[ -n "$STRINGS_XML" ]] && [[ -f "$STRINGS_XML" ]]; then
        CONSENT_STRINGS=$(grep -ciE '<string name="[^"]*(consent|tracking|analytics_opt|cookie|gdpr|einwillig)' "$STRINGS_XML" 2>/dev/null | tr -d '[:space:]')
    fi
    echo "| Indikator | Anzahl |"
    echo "|-----------|--------|"
    echo "| Google UMP (User Messaging Platform) | $UMP |"
    echo "| Firebase Consent-API | $FB_CONSENT |"
    echo "| Custom Consent-Komponenten | $CUSTOM_CONSENT |"
    echo "| Consent-Strings in strings.xml | $CONSENT_STRINGS |"
    echo ""

    # 4d.3 Rechtstexte-Links
    echo "### 4d.3 Rechtstexte (Links + Strings)"
    echo ""
    if [[ -n "$STRINGS_XML" ]] && [[ -f "$STRINGS_XML" ]]; then
        TERMS_STR=$(grep -ciE '<string name="[^"]*(terms|agb|nutzungsbedingung)' "$STRINGS_XML" 2>/dev/null | tr -d '[:space:]')
        PRIVACY_STR=$(grep -ciE '<string name="[^"]*(privacy|datenschutz)' "$STRINGS_XML" 2>/dev/null | tr -d '[:space:]')
        IMPRINT_STR=$(grep -ciE '<string name="[^"]*(imprint|impressum)' "$STRINGS_XML" 2>/dev/null | tr -d '[:space:]')
        WITHDRAW_STR=$(grep -ciE '<string name="[^"]*(widerruf|withdraw|refund|cancellation_right)' "$STRINGS_XML" 2>/dev/null | tr -d '[:space:]')
        URL_STRINGS=$(grep -cE '<string[^>]*>https?://' "$STRINGS_XML" 2>/dev/null | tr -d '[:space:]')
        echo "| Rechtstext | String-Treffer |"
        echo "|-----------|----------------|"
        echo "| AGB / Terms / Nutzungsbedingungen | $TERMS_STR |"
        echo "| Datenschutz / Privacy | $PRIVACY_STR |"
        echo "| Impressum / Imprint | $IMPRINT_STR |"
        echo "| Widerruf / Withdrawal | $WITHDRAW_STR |"
        echo "| URL-Strings (http/https) | $URL_STRINGS |"
        echo ""

        echo "**URLs in strings.xml (Stichprobe):**"
        echo "\`\`\`"
        grep -E '<string[^>]*>https?://' "$STRINGS_XML" 2>/dev/null | head -15
        echo "\`\`\`"
        echo ""
    fi

    # 4d.4 Health-Indikatoren
    echo "### 4d.4 Health-Indikatoren"
    echo ""
    HC_SDK=$(grep -rln 'HealthConnect\|GoogleFit' --include='*.kt' --include='*.java' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    HEALTH_TERMS=0
    if [[ -n "$STRINGS_XML" ]] && [[ -f "$STRINGS_XML" ]]; then
        HEALTH_TERMS=$(grep -ciE 'diagnose|therapie|behandlung|medikament|krankheit|symptom|aerzt|psycholog|mental_health|fitness|workout|meditation' "$STRINGS_XML" 2>/dev/null | tr -d '[:space:]')
    fi
    DISCLAIMER_STR=0
    if [[ -n "$STRINGS_XML" ]] && [[ -f "$STRINGS_XML" ]]; then
        DISCLAIMER_STR=$(grep -ciE '<string name="[^"]*(disclaimer|haftungsausschluss|keine_medizinische|not_medical)' "$STRINGS_XML" 2>/dev/null | tr -d '[:space:]')
    fi
    echo "| Indikator | Anzahl |"
    echo "|-----------|--------|"
    echo "| Health Connect / GoogleFit SDK | $HC_SDK |"
    echo "| Health-Begriffe in Strings | $HEALTH_TERMS |"
    echo "| Disclaimer-Strings | $DISCLAIMER_STR |"
    if [[ "$HC_SDK" -gt 0 ]] || [[ "$HEALTH_TERMS" -gt 10 ]]; then
        echo ""
        echo "**⚠ Health-Disclaimer-Pflicht moeglich** — manueller Audit empfohlen"
    fi
    echo ""

    # 4d.5 AI-Indikatoren
    echo "### 4d.5 AI-Indikatoren"
    echo ""
    AI_SDK=$(grep -rln 'GenerativeModel\|GeminiClient\|OpenAI\|Anthropic\|generateContent\|chat\.completions' --include='*.kt' --include='*.java' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    AI_DISCLAIMER=0
    if [[ -n "$STRINGS_XML" ]] && [[ -f "$STRINGS_XML" ]]; then
        AI_DISCLAIMER=$(grep -ciE '<string name="[^"]*(ai_disclaimer|ki_disclaimer|ai_warning|ki_warning|ai_hint|ki_hint)' "$STRINGS_XML" 2>/dev/null | tr -d '[:space:]')
    fi
    echo "| Indikator | Anzahl |"
    echo "|-----------|--------|"
    echo "| AI-SDK-Aufrufe | $AI_SDK |"
    echo "| AI-Disclaimer-Strings | $AI_DISCLAIMER |"
    if [[ "$AI_SDK" -gt 0 ]] && [[ "$AI_DISCLAIMER" -eq 0 ]]; then
        echo ""
        echo "**⚠ AI-SDK verwendet aber KEINE Disclaimer-Strings gefunden** — EU AI Act Pflicht!"
    fi
    echo ""

    # 4d.6 Werbe-Indikatoren
    echo "### 4d.6 Werbe-Indikatoren"
    echo ""
    AD_SDK=$(grep -rln 'AdMob\|AdSense\|InterstitialAd\|RewardedAd\|BannerAd\|MoPub' --include='*.kt' --include='*.java' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    AD_PERMISSION=$(grep -c 'com.google.android.gms.permission.AD_ID' "${MANIFEST:-/dev/null}" 2>/dev/null | tr -d '[:space:]')
    AD_MARKING_STR=0
    if [[ -n "$STRINGS_XML" ]] && [[ -f "$STRINGS_XML" ]]; then
        AD_MARKING_STR=$(grep -ciE '<string name="[^"]*(werbung|anzeige|gesponsert|sponsored|advertisement)' "$STRINGS_XML" 2>/dev/null | tr -d '[:space:]')
    fi
    echo "| Indikator | Anzahl |"
    echo "|-----------|--------|"
    echo "| Ad-SDK-Aufrufe | $AD_SDK |"
    echo "| AD_ID Permission im Manifest | $AD_PERMISSION |"
    echo "| Werbe-Markierungs-Strings | $AD_MARKING_STR |"
    if [[ "$AD_SDK" -gt 0 ]] && [[ "$AD_MARKING_STR" -eq 0 ]]; then
        echo ""
        echo "**⚠ Ad-SDK verwendet aber KEINE Werbe-Markierungs-Strings** — UWG §5a Verstoss!"
    fi
    echo ""

    # 4d.7 Account-Deletion
    echo "### 4d.7 Account-Deletion (DSGVO Art. 17)"
    echo ""
    DEL_CODE=$(grep -rln 'deleteAccount\|removeUser\|clearAllData\|gdprDelete\|requestAccountDeletion' --include='*.kt' --include='*.java' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    DEL_STRINGS=0
    if [[ -n "$STRINGS_XML" ]] && [[ -f "$STRINGS_XML" ]]; then
        DEL_STRINGS=$(grep -ciE '<string name="[^"]*(delete_account|account_delete|kontoloesch|konto_loesch)' "$STRINGS_XML" 2>/dev/null | tr -d '[:space:]')
    fi
    echo "| Indikator | Anzahl |"
    echo "|-----------|--------|"
    echo "| Loeschungs-Code-Dateien | $DEL_CODE |"
    echo "| Loeschungs-Strings | $DEL_STRINGS |"
    if [[ "$DEL_CODE" -eq 0 ]] || [[ "$DEL_STRINGS" -eq 0 ]]; then
        echo ""
        echo "**⚠ Account-Deletion fehlt oder unvollstaendig** — Play Store Policy 2024 Pflicht!"
    fi
    echo ""

    # 4d.8 Standort
    echo "### 4d.8 Standort-Verwendung"
    echo ""
    LOC_PERMS=$(grep -cE 'android.permission.ACCESS_(FINE|COARSE|BACKGROUND)_LOCATION' "${MANIFEST:-/dev/null}" 2>/dev/null | tr -d '[:space:]')
    LOC_CODE=$(grep -rln 'LocationManager\|FusedLocationProviderClient' --include='*.kt' --include='*.java' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    echo "| Indikator | Anzahl |"
    echo "|-----------|--------|"
    echo "| Location-Permissions im Manifest | $LOC_PERMS |"
    echo "| LocationManager/FusedLocation-Code | $LOC_CODE |"
    if [[ "$LOC_PERMS" -gt 0 ]] && [[ "$LOC_CODE" -eq 0 ]]; then
        echo ""
        echo "**⚠ Location-Permission deklariert aber NIE verwendet** — aus Manifest entfernen!"
    fi
    echo ""

    # === SCHICHT 4e: EXTERNE INHALTE ===
    echo "## Schicht 4e — Externe Inhalte"
    echo ""
    echo "Vorarbeit fuer den Audit. Frank muss zusaetzlich Inhalte ausserhalb des Repos beitragen."
    echo ""

    # 4e.1 Fastlane Metadata
    echo "### 4e.1 Fastlane Play-Store-Metadaten (falls vorhanden)"
    echo ""
    FASTLANE_DIRS=$(ls -d fastlane/metadata/android/*/ 2>/dev/null | wc -l)
    echo "Fastlane-Sprachen-Verzeichnisse: $FASTLANE_DIRS"
    if [[ "$FASTLANE_DIRS" -gt 0 ]]; then
        echo "\`\`\`"
        ls -d fastlane/metadata/android/*/ 2>/dev/null
        echo "\`\`\`"
    fi
    echo ""

    # 4e.2 Remote Config
    echo "### 4e.2 Firebase Remote Config"
    echo ""
    RC_CODE=$(grep -rln 'FirebaseRemoteConfig\|remoteConfig\.' --include='*.kt' --include='*.java' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    RC_DEFAULTS=$(find . -name 'remote_config_defaults.xml' -o -name 'config_defaults*.xml' 2>/dev/null | grep -v '/build/' | wc -l)
    echo "| Indikator | Anzahl |"
    echo "|-----------|--------|"
    echo "| RemoteConfig-Code-Dateien | $RC_CODE |"
    echo "| Default-Wert-XML-Dateien | $RC_DEFAULTS |"
    echo ""
    if [[ "$RC_CODE" -gt 0 ]]; then
        echo "**Remote-Config-Keys im Code:**"
        echo "\`\`\`"
        grep -rohE 'remoteConfig\.getString\("[^"]+"\)' --include='*.kt' . 2>/dev/null | sed 's/remoteConfig\.getString("//' | sed 's/")//' | sort -u | head -30
        echo "\`\`\`"
        echo ""
    fi

    # 4e.3 Cloud Functions
    echo "### 4e.3 Cloud Functions"
    echo ""
    FUNCTIONS_DIR=$(find . -name 'functions' -type d -maxdepth 3 -not -path '*/node_modules/*' 2>/dev/null | head -1)
    if [[ -n "$FUNCTIONS_DIR" ]]; then
        echo "Functions-Verzeichnis: \`$FUNCTIONS_DIR\`"
        echo ""
        NOTIF_SENDS=$(grep -rn 'sendNotification\|messaging\.send\|admin\.messaging' --include='*.ts' --include='*.js' "$FUNCTIONS_DIR" 2>/dev/null | wc -l)
        echo "Notification-Send-Aufrufe: $NOTIF_SENDS"
    else
        echo "Kein Functions-Verzeichnis gefunden (Cloud-Functions nicht im Repo)."
    fi
    echo ""

    # 4e.4 WebView
    echo "### 4e.4 WebView-Inhalte"
    echo ""
    WV_CODE=$(grep -rln 'WebView\|loadUrl\|loadData\|loadDataWithBaseURL' --include='*.kt' --include='*.java' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    ASSET_HTML=$(find . -path '*/assets/*' \( -name '*.html' -o -name '*.htm' \) -not -path '*/build/*' 2>/dev/null | wc -l)
    ASSET_MD=$(find . -path '*/assets/*.md' -not -path '*/build/*' 2>/dev/null | wc -l)
    echo "| Indikator | Anzahl |"
    echo "|-----------|--------|"
    echo "| WebView-Komponenten im Code | $WV_CODE |"
    echo "| HTML-Dateien in assets/ | $ASSET_HTML |"
    echo "| Markdown-Dateien in assets/ | $ASSET_MD |"
    echo ""
    if [[ "$ASSET_HTML" -gt 0 ]]; then
        echo "**Asset-HTML-Dateien:**"
        echo "\`\`\`"
        find . -path '*/assets/*' \( -name '*.html' -o -name '*.htm' \) -not -path '*/build/*' 2>/dev/null
        echo "\`\`\`"
        echo ""
    fi

    # 4e.5 PDF-Export
    echo "### 4e.5 PDF-Export"
    echo ""
    PDF_CODE=$(grep -rln 'PdfDocument\|iText\|itextpdf\|PrintDocumentAdapter' --include='*.kt' --include='*.java' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    echo "| Indikator | Anzahl |"
    echo "|-----------|--------|"
    echo "| PDF-Generierungs-Code | $PDF_CODE |"
    echo ""

    # 4e.6 Customer-Support
    echo "### 4e.6 Customer-Support-System"
    echo ""
    SUPPORT=$(grep -rln 'Intercom\|Zendesk\|Helpshift\|Crisp\|Tawk' --include='*.kt' --include='*.java' --include='*.gradle*' . 2>/dev/null | head -3)
    if [[ -n "$SUPPORT" ]]; then
        echo "**Erkannte Systeme:**"
        echo "\`\`\`"
        echo "$SUPPORT"
        echo "\`\`\`"
    else
        echo "Kein externes Customer-Support-System erkannt (vermutlich Email-only oder kein Support)."
    fi
    echo ""

    # === SCHICHT 4b ERWEITERT: NEUE UI-KOMPONENTEN ===
    echo "## Schicht 4b — Erweiterte UI-Komponenten (Bereiche 13-20)"
    echo ""

    TF_COUNT=$(grep -rln 'OutlinedTextField(\|TextField(' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    TF_LABEL=$(grep -rn 'label\s*=\s*{' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    TF_PLACE=$(grep -rn 'placeholder\s*=\s*{' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    TF_SUPP=$(grep -rn 'supportingText\s*=\s*{' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    CHIP_COUNT=$(grep -rln 'FilterChip(\|AssistChip(\|InputChip(\|SuggestionChip(' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    TOOLTIP_COUNT=$(grep -rln 'TooltipBox(\|PlainTooltip(\|RichTooltip(' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    SEARCHBAR_COUNT=$(grep -rln 'SearchBar(\|DockedSearchBar(' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    SEMANTICS_COUNT=$(grep -rn 'Modifier\.semantics\s*{\|semantics\s*{' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    SLIDER_COUNT=$(grep -rln 'Slider(\|RangeSlider(' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    DATE_PICK=$(grep -rln 'DatePicker(\|DatePickerDialog(\|rememberDatePickerState' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    TIME_PICK=$(grep -rln 'TimePicker(\|TimePickerDialog(\|rememberTimePickerState' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)
    ANNOT_STR=$(grep -rln 'buildAnnotatedString\|AnnotatedString\.Builder' --include='*.kt' . 2>/dev/null | grep -v '/build/' | grep -v '/test/' | wc -l)

    echo "| UI-Komponente | Treffer |"
    echo "|---------------|---------|"
    echo "| TextField (Dateien mit TextField/OutlinedTextField) | $TF_COUNT |"
    echo "|   davon label-Slots | $TF_LABEL |"
    echo "|   davon placeholder-Slots | $TF_PLACE |"
    echo "|   davon supportingText-Slots | $TF_SUPP |"
    echo "| Chips (Filter/Assist/Input/Suggestion) | $CHIP_COUNT |"
    echo "| Material 3 Tooltips | $TOOLTIP_COUNT |"
    echo "| SearchBar / DockedSearchBar | $SEARCHBAR_COUNT |"
    echo "| semantics-Block (a11y) | $SEMANTICS_COUNT |"
    echo "| Slider / RangeSlider | $SLIDER_COUNT |"
    echo "| DatePicker | $DATE_PICK |"
    echo "| TimePicker | $TIME_PICK |"
    echo "| AnnotatedString-Builder | $ANNOT_STR |"
    echo ""

    # === ABSCHLUSS ===
    echo "---"
    echo ""
    echo "## Hinweise fuer die Tiefenanalyse"
    echo ""
    echo "Dieser Initial-Scan ist die Vorarbeit. Die Tiefenanalyse muss:"
    echo "1. Jede der 7 Schichten gemaess \`references/layer-N-*.md\` durchlaufen"
    echo "2. **Schicht 4b vollstaendig ausfuehren** — fuer JEDEN Bereich (20 Bereichstypen inkl. TextField-Slots, Chips, Tooltips, SearchBar, semantics, Slider, Picker, AnnotatedString) eine eigene 1:1-Wortlaut-Tabelle erstellen"
    echo "3. **Menues rekursiv ausrollen** — JEDE Untermenue-Ebene mit Breadcrumb-Pfad als eigene Zeile, beliebige Tiefe, keine Abkuerzung"
    echo "4. **Schicht 4c vollstaendig ausfuehren** — Slot-Laengen, translatable=false, xliff:g, Uebersetzer-Notes, CLDR-Plurals, HTML/CDATA, Format-Args, Glossar, Region-Differenzen, Du/Sie-Konsistenz"
    echo "5. **Schicht 4d vollstaendig ausfuehren** — Permission-Rationale, Consent, Rechtstexte, Health/AI/Werbe-Disclaimer, Account-Deletion, Newsletter, Widerruf, Standort, Altersfreigabe"
    echo "6. **Schicht 4e vollstaendig ausfuehren** — Play-Store-Listing, Remote-Config-Live-Werte, Cloud-Functions-Templates, Email-Templates, WebView-Inhalte, PDF-Vorlagen, Marketing-Material (Frank-Aufgaben dokumentieren)"
    echo "7. Pro Befund Datei + Zeilennummer als Beleg liefern"
    echo "8. Die Don't-Miss-Checkliste (\`references/dont-miss-checklist.md\`) abschliessend pruefen — inkl. Block I, J, K, L"
    echo "9. Den finalen Bericht nach \`assets/audit-report-template.md\` strukturieren"
    echo ""
    echo "**Naechster Schritt:** Claude liest diesen Initial-Scan und arbeitet die Schichten 1-7 (inkl. 4b, 4c, 4d, 4e) detailliert durch."
} > "$OUTPUT"

echo "Initial-Scan geschrieben: $APP_DIR/$OUTPUT"
echo "Naechster Schritt: Claude liest die Datei und macht die Tiefenanalyse."

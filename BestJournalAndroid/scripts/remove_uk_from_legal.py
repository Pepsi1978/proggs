"""
Removes UK references from all legal documents (privacy + terms) in 27 locales.

Strategy:
- PRIVACY.html: remove the entire "8a.4 ... UK GDPR" section (h3-h2/h3-boundary)
- TERMS.html: remove single line containing "UK Consumer Rights Act 2015" or
  the localized equivalent (matched by the unchanged English app-name reference)
- DATENSCHUTZ.html (DE only): remove the supervisory-authority UK paragraph
- Crisis hotline lists: remove the "UK: 116 123" entry

Idempotent: re-running does nothing once UK references are gone.
"""
import re
import glob
from pathlib import Path

REPO_ROOT = Path.home() / "proggs" / "BestJournalAndroid"
PRIVACY_FILES = sorted(glob.glob(str(REPO_ROOT / "app/src/main/assets/legal/*/PRIVACY.html")))
TERMS_FILES = sorted(glob.glob(str(REPO_ROOT / "app/src/main/assets/legal/*/TERMS.html")))
# DE files have different filenames
DE_DSE = REPO_ROOT / "app/src/main/assets/legal/de/DATENSCHUTZ.html"
DE_TOS = REPO_ROOT / "app/src/main/assets/legal/de/NUTZUNGSBEDINGUNGEN.html"
# Repo doc copies
DOCS_FILES = [
    REPO_ROOT / "docs/DATENSCHUTZ.html",
    REPO_ROOT / "docs/NUTZUNGSBEDINGUNGEN.html",
    REPO_ROOT / "docs/PRIVACY.en.html",
    REPO_ROOT / "docs/TERMS.en.html",
]

# Section 8a.4 in PRIVACY.html (any language) — matches from <h3>...8a.4...</h3>
# until the next <h3> or <h2> tag (next section).
# This works across all 27 languages because the section number "8a.4" is stable.
# Requires DOTALL for multi-line content between header and next header.
SECTION_8A4_RE = re.compile(
    r"<h3>\s*8a\.4[^<]*</h3>.*?(?=<h3>|<h2>)",
    re.DOTALL | re.IGNORECASE,
)

# Localized hint for "UK GDPR" in section header (some translations may omit "8a.4")
# but we observed all 27 files use the numeric prefix so SECTION_8A4_RE is enough.

# Crisis hotline line referencing UK / Samaritans (English assets)
CRISIS_UK_RE = re.compile(
    r"\s*<strong>UK:</strong>\s*116\s*123\s*\(Samaritans\)\s*[—-]?\s*",
    re.IGNORECASE,
)
# Inline list-item variant (different separators between countries)
CRISIS_UK_LI_RE = re.compile(
    r"\s*<strong>UK:</strong>\s*116\s*123\s*\(Samaritans\)\s*",
    re.IGNORECASE,
)

# Single line in TERMS.html: <li>...UK Consumer Rights Act 2015...</li>
# matched conservatively across all languages because the law name is stable English.
# Toleriert <strong>-Tags und Newlines innerhalb des <li>-Eintrags.
TERMS_UK_LINE_RE = re.compile(
    r"\s*<li>(?:(?!</li>).)*UK Consumer Rights Act 2015(?:(?!</li>).)*</li>\n?",
    re.DOTALL,
)

# DE-DSE specific: supervisory authority paragraph with ICO
DE_ICO_RE = re.compile(
    r"\s*<p><strong>Vereinigtes Königreich \(UK\):</strong></p>.*?<p>Nutzer im UK[^<]*</p>",
    re.DOTALL,
)

# DE-TOS specific: single li with "UK Consumer Rights Act 2015"
DE_TOS_UK_RE = re.compile(
    r"\s*<li>den <strong>UK Consumer Rights Act 2015</strong>[^<]*</li>\n?",
    re.DOTALL,
)

# Locale-Kurzfassungen erwaehnen "UK GDPR for the United Kingdom" als Beispiel
# in einer Aufzaehlung. Wir entfernen diesen Listenpunkt mitsamt vorangehendem
# Trennzeichen (Komma, ASCII oder CJK).
# Beispiele die das matched:
#   ", UK GDPR pour le Royaume-Uni"
#   "、UK GDPR、英国"
#   "; UK GDPR for the United Kingdom"
UK_LIST_ITEM_RE = re.compile(
    r"[,，、；؛،][^,，、；؛،)）]*UK\s*GDPR[^,，、；؛،)）]*",
    re.UNICODE,
)

# Locale-spezifische Crisis-Hotline-Eintraege "<strong>{Land}:</strong> 116 123 (Samaritans)"
# Trifft alle Sprachen weil "116 123 (Samaritans)" stabil englisch ist.
# Toleriert vorangehende em-dash/dash-Trennzeichen.
CRISIS_UK_LOCALIZED_RE = re.compile(
    r"\s*[—–-]?\s*<strong>[^<]+:</strong>\s*116\s*123\s*\(Samaritans\)\s*",
    re.UNICODE,
)
# Ganze <li>-Variante (pt-BR Stil)
CRISIS_UK_LI_LOCALIZED_RE = re.compile(
    r"\s*<li>\s*<strong>[^<]+:</strong>\s*116\s*123\s*\(Samaritans\)\s*</li>\s*",
    re.UNICODE,
)


def edit_file(path: Path, transforms: list, label: str) -> bool:
    if not path.exists():
        print(f"SKIP (not found): {path}")
        return False
    content = path.read_text(encoding="utf-8")
    original = content
    for regex, replacement in transforms:
        content = regex.sub(replacement, content)
    if content != original:
        # Collapse 3+ consecutive empty lines to 2
        content = re.sub(r"\n{4,}", "\n\n\n", content)
        path.write_text(content, encoding="utf-8", newline="\n")
        print(f"FIXED ({label}): {path.relative_to(REPO_ROOT)}")
        return True
    return False


changes = 0

# 1) All PRIVACY.html files (27 locales): remove section 8a.4 + UK crisis line
#    + UK-list-item in der Cross-Jurisdiction-Aufzaehlung + locale-spezifische Crisis-Hotline.
for p in PRIVACY_FILES:
    pp = Path(p)
    if edit_file(
        pp,
        [
            (SECTION_8A4_RE, ""),
            (CRISIS_UK_LI_LOCALIZED_RE, ""),  # Liste-Variante zuerst (greift ganzes <li>)
            (CRISIS_UK_LOCALIZED_RE, ""),     # Inline-Variante danach
            (CRISIS_UK_RE, ""),                # EN-Original-Variante
            (CRISIS_UK_LI_RE, ""),
            (UK_LIST_ITEM_RE, ""),
        ],
        "privacy",
    ):
        changes += 1

# 2) All TERMS.html files: remove UK Consumer Rights Act line
for p in TERMS_FILES:
    pp = Path(p)
    if edit_file(pp, [(TERMS_UK_LINE_RE, "")], "terms"):
        changes += 1

# 3) DE specific
if edit_file(DE_DSE, [(DE_ICO_RE, "")], "de-dse"):
    changes += 1
if edit_file(DE_TOS, [(DE_TOS_UK_RE, "")], "de-tos"):
    changes += 1

# 4) Repo docs
for p in DOCS_FILES:
    if edit_file(
        p,
        [
            (SECTION_8A4_RE, ""),
            (CRISIS_UK_RE, ""),
            (CRISIS_UK_LI_RE, ""),
            (DE_ICO_RE, ""),
            (DE_TOS_UK_RE, ""),
            (TERMS_UK_LINE_RE, ""),
        ],
        "docs",
    ):
        changes += 1

print(f"\nTotal files modified: {changes}")

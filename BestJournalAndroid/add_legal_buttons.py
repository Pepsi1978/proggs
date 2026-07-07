"""
Insert a prominent button-row with the full English and German legal versions
into every localized legal summary (25 languages x 3 documents = 75 files).
Also update the 3 English master templates in docs/.
"""
import re
import os

LOCALES = ["fr", "es", "pt-BR", "pt-PT", "it", "nl", "pl", "uk", "tr", "ja", "ko",
           "zh-CN", "zh-TW", "ar", "hi", "th", "id", "bn", "te", "mr", "ta", "ur",
           "gu", "kn", "ml"]
FILES = ["PRIVACY.html", "TERMS.html", "IMPRINT.html"]

BASE_ASSETS = os.path.expanduser("~/proggs/BestJournalAndroid/app/src/main/assets/legal")
BASE_DOCS = os.path.expanduser("~/proggs/BestJournalAndroid/docs/legal-summary-templates")

DOC_ICON = "\U0001F4C4"   # page-facing-up
ARROW = "→"          # rightwards arrow

NEW_CSS = (
    "  .legal-buttons { margin: 1.25rem 0 1.5rem; display: flex; "
    "flex-direction: column; gap: 0.5rem; }\n"
    "  .btn-legal { display: block; padding: 0.95rem 1.15rem; "
    "text-decoration: none; color: #ffffff; border-radius: 8px; "
    "font-weight: 600; font-size: 1rem; text-align: center; word-break: normal; }\n"
    "  .btn-legal:hover { text-decoration: none; opacity: 0.92; }\n"
    "  .btn-en { background: #1e40af; }\n"
    "  .btn-de { background: #047857; }\n"
)


def strip_strong(s):
    s = re.sub(r"</?strong>", "", s)
    return re.sub(r"\s+", " ", s).strip()


def process_file(path):
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()

    if ".legal-buttons" in content:
        return False, "already-has-buttons"

    en_match = re.search(
        r'<a href="(file:///android_asset/legal/en/[^"]+)"[^>]*>(.*?)</a>',
        content, re.DOTALL,
    )
    de_match = re.search(
        r'<a href="(file:///android_asset/legal/de/[^"]+)"[^>]*>(.*?)</a>',
        content, re.DOTALL,
    )
    if not en_match or not de_match:
        return False, "no-links-found"

    en_url = en_match.group(1)
    de_url = de_match.group(1)
    en_text = strip_strong(en_match.group(2))
    de_text = strip_strong(de_match.group(2))

    buttons = (
        "\n<div class=\"legal-buttons\">\n"
        "<a class=\"btn-legal btn-en\" href=\"" + en_url + "\">"
        + DOC_ICON + " " + en_text + " " + ARROW + "</a>\n"
        "<a class=\"btn-legal btn-de\" href=\"" + de_url + "\">"
        + DOC_ICON + " " + de_text + " " + ARROW + "</a>\n"
        "</div>\n"
    )

    new_content, n = re.subn(
        r'(<p class="subtitle">[^<]*</p>)',
        lambda m: m.group(1) + buttons,
        content,
        count=1,
    )
    if n == 0:
        return False, "no-subtitle"

    new_content, n = re.subn(
        r'(  \.footer \{ color:)',
        NEW_CSS + r"\1",
        new_content,
        count=1,
    )
    if n == 0:
        return False, "no-footer-css"

    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(new_content)
    return True, "ok"


def main():
    fixed = 0
    skipped = 0
    for locale in LOCALES:
        for fname in FILES:
            path = os.path.join(BASE_ASSETS, locale, fname)
            if not os.path.exists(path):
                print("MISS " + locale + "/" + fname)
                continue
            ok, status = process_file(path)
            if ok:
                fixed += 1
                print("OK   " + locale + "/" + fname)
            else:
                skipped += 1
                print("SKIP " + locale + "/" + fname + " (" + status + ")")

    for tpl in ["PRIVACY.en.html", "TERMS.en.html", "IMPRINT.en.html"]:
        path = os.path.join(BASE_DOCS, tpl)
        if not os.path.exists(path):
            continue
        ok, status = process_file(path)
        if ok:
            fixed += 1
            print("OK   template/" + tpl)
        else:
            skipped += 1
            print("SKIP template/" + tpl + " (" + status + ")")

    print("\nTotal: " + str(fixed) + " fixed, " + str(skipped) + " skipped")


if __name__ == "__main__":
    main()

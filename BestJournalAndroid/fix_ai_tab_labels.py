"""Set label_improved + follow_up_tab_improved to '✨ {AI-Word} {Improved-Adj}'
in all 26 languages. Two short words after the sparkles — fits in tab labels.

Per-language reasoning:
- Latin script: AI/IA + past participle (verbessert, mejorado, ...)
- CJK: AI + Verb-noun (改善 / 개선 / 优化) — space between AI and CJK helps readability
- Indic: AI + improved-adjective (no native digits, Arabic numerals only)
- RTL (ar/ur): source code stays LTR, Android renders RTL automatically
- Turkish: YZ (Yapay Zeka), Ukrainian: ШІ (Штучний Інтелект), French/Italian/Spanish/Port: IA
"""
import os, re, tempfile

VALUE = {
    # locale-folder-name (or "" for default/de): value
    "":       "✨ KI verbessert",
    "en":     "✨ AI Enhanced",
    "fr":     "✨ IA Amélioré",
    "es":     "✨ IA Mejorado",
    "pt-rBR": "✨ IA Melhorado",
    "pt-rPT": "✨ IA Melhorado",
    "it":     "✨ IA Migliorato",
    "nl":     "✨ AI Verbeterd",
    "pl":     "✨ AI Ulepszone",
    "uk":     "✨ ШІ Покращено",
    "tr":     "✨ YZ Geliştirildi",
    "ja":     "✨ AI 改善",
    "ko":     "✨ AI 개선",
    "zh-rCN": "✨ AI 优化",
    "zh-rTW": "✨ AI 優化",
    "ar":     "✨ AI محسَّن",
    "hi":     "✨ AI सुधरा",
    "th":     "✨ AI ปรับปรุง",
    "in":     "✨ AI Diperbaiki",
    "bn":     "✨ AI উন্নত",
    "te":     "✨ AI మెరుగైన",
    "mr":     "✨ AI सुधारित",
    "ta":     "✨ AI மேம்பட்டது",
    "ur":     "✨ AI بہتر",
    "gu":     "✨ AI સુધારેલ",
    "kn":     "✨ AI ಸುಧಾರಿತ",
    "ml":     "✨ AI മെച്ചപ്പെട്ട",
}

BASE = os.path.expanduser("~/proggs/BestJournalAndroid/app/src/main/res")
KEYS = ["label_improved", "follow_up_tab_improved"]

modified, errors = 0, []

for locale, val in VALUE.items():
    folder = "values" if locale == "" else f"values-{locale}"
    path = os.path.join(BASE, folder, "strings.xml")
    if not os.path.exists(path):
        errors.append(f"FILE NOT FOUND: {path}")
        continue
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()
    original = content
    for key in KEYS:
        pat = re.compile(rf'(<string name="{key}">)([^<]*)(</string>)')
        content, n = pat.subn(lambda m: m.group(1) + val + m.group(3), content, count=1)
        if n == 0:
            errors.append(f"{locale or 'de'}: key '{key}' not found")
    if content != original:
        d = os.path.dirname(path)
        with tempfile.NamedTemporaryFile("w", dir=d, suffix=".tmp",
                                          delete=False, encoding="utf-8") as tmp:
            tmp.write(content); tmp_path = tmp.name
        os.replace(tmp_path, path)
        modified += 1
        print(f"OK {locale or 'de':>7}: '{val}' ({len(val)} chars)")
    else:
        print(f"unchanged {locale or 'de'}")

print(f"\nModified: {modified} | Errors: {len(errors)}")
for e in errors:
    print(f"  - {e}")

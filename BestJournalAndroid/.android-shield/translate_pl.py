#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Polnisch-Uebersetzungs-Patcher fuer BestJournal strings.xml
Atomic write via tempfile + os.replace
Fix: Echte Unicode-Zeichen statt \\u-Escapes in Replacement-Strings (regex-safe)
"""
import os
import re
import tempfile
import xml.etree.ElementTree as ET

STRINGS_PATH = r"C:\Users\barwa\proggs\BestJournalAndroid\app\src\main\res\values-pl\strings.xml"

# 22 uebersetzte Strings mit echten Unicode-Zeichen (keine \\u-Escapes)
TRANSLATIONS = {
    "dashboard_weekly_review_upsell_body": "Miałeś pracowity tydzień. Z Premium widzisz pełną analizę: odkrywaj wzorce, znajdź wnioski i zrozum, co cię zajmuje.",
    "legend_insight_deep": "Głęboka analiza — powtarzające się wzorce i przekonania",
    "profile_insight_desc": "Ujawnia powtarzające się wzorce myślenia i odczuwania",
    "profile_entropy_long": "AI szuka w Twoich wpisach stresu, obciążenia i chaosu.\\n\\nOtrzymasz:\\n\\n• Analizę głównych źródeł obciążenia\\n• 5 konkretnych kroków do posprzatania\\n• Wskazówki pomocne w codziennym życiu\\n\\nIdealnie, gdy masz wrażenie, że wszystkiego jest za dużo.",
    "profile_insight_long": "AI sięga głębiej niż tylko do wydarzeń. W Twoich wpisach dostrzega:\\n\\n• Wzorce myślenia i przekonania w Twoich słowach\\n• Powtarzające się uczucia i reakcje\\n• Mocne strony widoczne w Twoich wpisach\\n• Wartości kierujące Twoim działaniem\\n\\nDla tych, którzy chcą się lepiej poznać i rozwijać się.",
    "settings_premium_cancelled_desc": "Twoje funkcje Premium pozostaną aktywne do końca okresu rozliczeniowego. Możesz cofnąć anulowanie przez Google Play.",
    "settings_premium_feature_5_perspectives_desc": "Od podsumowania do samopoznania",
    "privacy_gate_gemini_body": "Cieszymy się, że chcesz korzystać z inteligentnych funkcji Best Journal! Do tworzenia podsumówań, przeglądów, wglądów w panelu i ulepszań tekstu Twoje treści są wysyłane zaszyfrowane do Google Gemini.\\n\\n✓ Brak trenowania AI na Twoich danych\\n✓ Zapytania nie są trwale przechowywane\\n✓ Szyfrowanie zgodne z EU-US Data Privacy Framework\\n\\nWażna informacja: ponieważ Twoje wpisy mogą zawierać szczególnie wrażliwe dane (zdrowie, religia, relacje osobiste — Art. 9 RODO), prosimy o Twoją wyraźną zgodę na przetwarzanie przez AI. Wyniki AI to sugestie, a nie profesjonalne porady i nie zastępują specjalisty.\\n\\nMożesz to zmienić w dowolnym momencie w ustawieniach.",
    "settings_delete_account_force_local": "Tylko lokalnie (kopia zapasowa Drive pozostanie)",
    "retro_monthly_p3": "Pokazuje tendencje z Twoich wpisów",
    "retro_yearly_p2": "AI podsumowuje główne tematy Twojego roku",
    "onboarding_subtitle": "Twój osobisty dziennik AI\\ndla większej jasności w codziennym życiu",
    "onboarding_how_step4_desc": "Panel AI pokazuje powiązania z Twoich wpisów.",
    "onboarding_premium_feature_patterns": "Wzorce na wierzch: AI pokazuje powtarzające się wzorce myślenia i odczuwania w Twoich wpisach",
    "onboarding_free_trial": "Bezpłatnie, możliwe do anulowania w dowolnym momencie",
    "onboarding_try_premium": "8 dni za darmo, później od 3,99 €/miesiąc",
    "paywall_yearly_note": "Następnie %1$s rocznie, odnawia się automatycznie\\nW okresie próbnym możliwe do anulowania w dowolnym momencie",
    "paywall_most_popular": "Polecane",
    "paywall_free_note": "Aplikacja działa również bez subskrypcji.",
    "paywall_feature_patterns": "Odkryj powtarzające się wzorce w swoim myśleniu",
    "churn_exclusive_offer": "Oferta dla Ciebie",
    "paywall_subscribe_button": "Subskrybuj (płatność wymagana)",
}


def patch_strings_xml(path: str, translations: dict) -> None:
    with open(path, "r", encoding="utf-8") as fh:
        content = fh.read()

    replaced = set()

    for key, value in translations.items():
        # Regex findet den Tag; Replacement via Funktion (kein re.sub mit Backslash-Strings)
        pattern = r'(<string\s+name="{key}"[^>]*>)(.*?)(</string>)'.format(key=re.escape(key))
        # Lambda verhindert Regex-Interpretation des Replacement-Strings
        def make_replacer(opening_group, closing_group, val):
            def replacer(m):
                return m.group(1) + val + m.group(3)
            return replacer

        new_content = re.sub(pattern, make_replacer(1, 3, value), content, flags=re.DOTALL)
        if new_content != content:
            content = new_content
            replaced.add(key)

    # Fehlende Keys vor </resources> einsetzen
    missing = [k for k in translations if k not in replaced]
    if missing:
        insert_lines = []
        for key in missing:
            insert_lines.append('    <string name="{}">{}</string>'.format(key, translations[key]))
        insert_block = "\n".join(insert_lines) + "\n"
        content = content.replace("</resources>", insert_block + "</resources>")

    # Atomic write
    dir_name = os.path.dirname(path)
    with tempfile.NamedTemporaryFile("w", dir=dir_name, suffix=".tmp",
                                     delete=False, encoding="utf-8") as tmp:
        tmp.write(content)
        tmp_path = tmp.name
    os.replace(tmp_path, path)

    print("Ersetzt: {}, Eingefuegt: {}".format(len(replaced), len(missing)))
    print("Ersetzt-Keys: {}".format(sorted(replaced)))
    if missing:
        print("Eingefuegt-Keys: {}".format(missing))


def validate_xml(path: str) -> bool:
    try:
        ET.parse(path)
        print("XML valide: OK")
        return True
    except ET.ParseError as e:
        print("XML-Fehler: {}".format(e))
        return False


if __name__ == "__main__":
    patch_strings_xml(STRINGS_PATH, TRANSLATIONS)
    validate_xml(STRINGS_PATH)

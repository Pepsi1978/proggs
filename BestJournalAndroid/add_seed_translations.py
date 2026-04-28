#!/usr/bin/env python3
"""
Batch script: insert 6 new dev_seed_* strings into all 25 locale strings.xml files
+ update settings_about_version to V0.15.0.

Replaces übersetzung skill's per-language ceremony for this trivial 6-string addition.
Honors the skill's mandatory checks 7/8/9 for Indic/CJK/PT variants — translations
hand-crafted with those rules (Indic = arabic 0-9 only, CJK = full-width punctuation,
pt-PT vs pt-BR vocabulary clearly separated).
"""
import re
import os
import tempfile
import sys

APP_DIR = os.path.expanduser("~/proggs/BestJournalAndroid/app/src/main/res")
NEW_VERSION = "Best Journal V0.15.0"

# 6 strings × 25 locales. Format: (key, text). Indic stays Arabic 0-9, CJK full-width.
TRANSLATIONS = {
    "ar": [
        ("dev_seed_create_label", "إنشاء 70 إدخالاً اختبارياً"),
        ("dev_seed_delete_label", "حذف 70 إدخالاً اختبارياً"),
        ("dev_seed_running", "جارٍ إنشاء 70 إدخالاً اختبارياً بلغتك…"),
        ("dev_seed_done", "تم إنشاء %1$d إدخال اختباري"),
        ("dev_seed_deleted", "تم حذف %1$d إدخال اختباري"),
        ("dev_seed_error", "خطأ: %1$s"),
    ],
    "bn": [
        ("dev_seed_create_label", "70টি পরীক্ষামূলক এন্ট্রি তৈরি করুন"),
        ("dev_seed_delete_label", "70টি পরীক্ষামূলক এন্ট্রি মুছুন"),
        ("dev_seed_running", "আপনার ভাষায় 70টি পরীক্ষামূলক এন্ট্রি তৈরি করা হচ্ছে…"),
        ("dev_seed_done", "%1$dটি পরীক্ষামূলক এন্ট্রি তৈরি হয়েছে"),
        ("dev_seed_deleted", "%1$dটি পরীক্ষামূলক এন্ট্রি মুছে ফেলা হয়েছে"),
        ("dev_seed_error", "ত্রুটি: %1$s"),
    ],
    "es": [
        ("dev_seed_create_label", "Generar 70 entradas de prueba"),
        ("dev_seed_delete_label", "Eliminar 70 entradas de prueba"),
        ("dev_seed_running", "Generando 70 entradas de prueba en tu idioma…"),
        ("dev_seed_done", "%1$d entradas de prueba creadas"),
        ("dev_seed_deleted", "%1$d entradas de prueba eliminadas"),
        ("dev_seed_error", "Error: %1$s"),
    ],
    "fr": [
        ("dev_seed_create_label", "Générer 70 entrées de test"),
        ("dev_seed_delete_label", "Supprimer 70 entrées de test"),
        ("dev_seed_running", "Génération de 70 entrées de test dans votre langue…"),
        ("dev_seed_done", "%1$d entrées de test créées"),
        ("dev_seed_deleted", "%1$d entrées de test supprimées"),
        ("dev_seed_error", "Erreur : %1$s"),
    ],
    "gu": [
        ("dev_seed_create_label", "70 પરીક્ષણ એન્ટ્રીઓ બનાવો"),
        ("dev_seed_delete_label", "70 પરીક્ષણ એન્ટ્રીઓ કાઢી નાખો"),
        ("dev_seed_running", "તમારી ભાષામાં 70 પરીક્ષણ એન્ટ્રીઓ બનાવી રહ્યું છે…"),
        ("dev_seed_done", "%1$d પરીક્ષણ એન્ટ્રીઓ બનાવી"),
        ("dev_seed_deleted", "%1$d પરીક્ષણ એન્ટ્રીઓ કાઢી નાખી"),
        ("dev_seed_error", "ભૂલ: %1$s"),
    ],
    "hi": [
        ("dev_seed_create_label", "70 परीक्षण प्रविष्टियाँ बनाएँ"),
        ("dev_seed_delete_label", "70 परीक्षण प्रविष्टियाँ हटाएँ"),
        ("dev_seed_running", "आपकी भाषा में 70 परीक्षण प्रविष्टियाँ बनाई जा रही हैं…"),
        ("dev_seed_done", "%1$d परीक्षण प्रविष्टियाँ बनाई गईं"),
        ("dev_seed_deleted", "%1$d परीक्षण प्रविष्टियाँ हटाई गईं"),
        ("dev_seed_error", "त्रुटि: %1$s"),
    ],
    "in": [  # Android legacy code for Indonesian
        ("dev_seed_create_label", "Buat 70 entri uji"),
        ("dev_seed_delete_label", "Hapus 70 entri uji"),
        ("dev_seed_running", "Membuat 70 entri uji dalam bahasa Anda…"),
        ("dev_seed_done", "%1$d entri uji dibuat"),
        ("dev_seed_deleted", "%1$d entri uji dihapus"),
        ("dev_seed_error", "Kesalahan: %1$s"),
    ],
    "it": [
        ("dev_seed_create_label", "Genera 70 voci di prova"),
        ("dev_seed_delete_label", "Elimina 70 voci di prova"),
        ("dev_seed_running", "Generazione di 70 voci di prova nella tua lingua…"),
        ("dev_seed_done", "%1$d voci di prova create"),
        ("dev_seed_deleted", "%1$d voci di prova eliminate"),
        ("dev_seed_error", "Errore: %1$s"),
    ],
    "ja": [
        ("dev_seed_create_label", "70件のテストエントリを作成"),
        ("dev_seed_delete_label", "70件のテストエントリを削除"),
        ("dev_seed_running", "お使いの言語で70件のテストエントリを作成しています…"),
        ("dev_seed_done", "%1$d件のテストエントリを作成しました"),
        ("dev_seed_deleted", "%1$d件のテストエントリを削除しました"),
        ("dev_seed_error", "エラー：%1$s"),
    ],
    "kn": [
        ("dev_seed_create_label", "70 ಪರೀಕ್ಷಾ ನಮೂದುಗಳನ್ನು ರಚಿಸಿ"),
        ("dev_seed_delete_label", "70 ಪರೀಕ್ಷಾ ನಮೂದುಗಳನ್ನು ಅಳಿಸಿ"),
        ("dev_seed_running", "ನಿಮ್ಮ ಭಾಷೆಯಲ್ಲಿ 70 ಪರೀಕ್ಷಾ ನಮೂದುಗಳನ್ನು ರಚಿಸಲಾಗುತ್ತಿದೆ…"),
        ("dev_seed_done", "%1$d ಪರೀಕ್ಷಾ ನಮೂದುಗಳನ್ನು ರಚಿಸಲಾಗಿದೆ"),
        ("dev_seed_deleted", "%1$d ಪರೀಕ್ಷಾ ನಮೂದುಗಳನ್ನು ಅಳಿಸಲಾಗಿದೆ"),
        ("dev_seed_error", "ದೋಷ: %1$s"),
    ],
    "ko": [
        ("dev_seed_create_label", "70개 테스트 항목 생성"),
        ("dev_seed_delete_label", "70개 테스트 항목 삭제"),
        ("dev_seed_running", "사용자 언어로 70개 테스트 항목을 생성 중…"),
        ("dev_seed_done", "테스트 항목 %1$d개가 생성됨"),
        ("dev_seed_deleted", "테스트 항목 %1$d개가 삭제됨"),
        ("dev_seed_error", "오류: %1$s"),
    ],
    "ml": [
        ("dev_seed_create_label", "70 ടെസ്റ്റ് എൻട്രികൾ സൃഷ്ടിക്കുക"),
        ("dev_seed_delete_label", "70 ടെസ്റ്റ് എൻട്രികൾ ഇല്ലാതാക്കുക"),
        ("dev_seed_running", "നിങ്ങളുടെ ഭാഷയിൽ 70 ടെസ്റ്റ് എൻട്രികൾ സൃഷ്ടിക്കുന്നു…"),
        ("dev_seed_done", "%1$d ടെസ്റ്റ് എൻട്രികൾ സൃഷ്ടിച്ചു"),
        ("dev_seed_deleted", "%1$d ടെസ്റ്റ് എൻട്രികൾ ഇല്ലാതാക്കി"),
        ("dev_seed_error", "പിശക്: %1$s"),
    ],
    "mr": [
        ("dev_seed_create_label", "70 चाचणी नोंदी तयार करा"),
        ("dev_seed_delete_label", "70 चाचणी नोंदी हटवा"),
        ("dev_seed_running", "तुमच्या भाषेत 70 चाचणी नोंदी तयार करत आहे…"),
        ("dev_seed_done", "%1$d चाचणी नोंदी तयार केल्या"),
        ("dev_seed_deleted", "%1$d चाचणी नोंदी हटवल्या"),
        ("dev_seed_error", "त्रुटी: %1$s"),
    ],
    "nl": [
        ("dev_seed_create_label", "70 testvermeldingen genereren"),
        ("dev_seed_delete_label", "70 testvermeldingen verwijderen"),
        ("dev_seed_running", "70 testvermeldingen in je taal genereren…"),
        ("dev_seed_done", "%1$d testvermeldingen aangemaakt"),
        ("dev_seed_deleted", "%1$d testvermeldingen verwijderd"),
        ("dev_seed_error", "Fout: %1$s"),
    ],
    "pl": [
        ("dev_seed_create_label", "Wygeneruj 70 wpisów testowych"),
        ("dev_seed_delete_label", "Usuń 70 wpisów testowych"),
        ("dev_seed_running", "Generowanie 70 wpisów testowych w Twoim języku…"),
        ("dev_seed_done", "Utworzono %1$d wpisów testowych"),
        ("dev_seed_deleted", "Usunięto %1$d wpisów testowych"),
        ("dev_seed_error", "Błąd: %1$s"),
    ],
    "pt-rBR": [  # Brazilian: gerar/excluir, gerundium "Gerando", "criadas/excluídas"
        ("dev_seed_create_label", "Gerar 70 entradas de teste"),
        ("dev_seed_delete_label", "Excluir 70 entradas de teste"),
        ("dev_seed_running", "Gerando 70 entradas de teste no seu idioma…"),
        ("dev_seed_done", "%1$d entradas de teste criadas"),
        ("dev_seed_deleted", "%1$d entradas de teste excluídas"),
        ("dev_seed_error", "Erro: %1$s"),
    ],
    "pt-rPT": [  # European: gerar/eliminar, "A gerar" infinitive, "criadas/eliminadas"
        ("dev_seed_create_label", "Gerar 70 entradas de teste"),
        ("dev_seed_delete_label", "Eliminar 70 entradas de teste"),
        ("dev_seed_running", "A gerar 70 entradas de teste no seu idioma…"),
        ("dev_seed_done", "%1$d entradas de teste criadas"),
        ("dev_seed_deleted", "%1$d entradas de teste eliminadas"),
        ("dev_seed_error", "Erro: %1$s"),
    ],
    "ta": [
        ("dev_seed_create_label", "70 சோதனை உள்ளீடுகளை உருவாக்கு"),
        ("dev_seed_delete_label", "70 சோதனை உள்ளீடுகளை நீக்கு"),
        ("dev_seed_running", "உங்கள் மொழியில் 70 சோதனை உள்ளீடுகளை உருவாக்குகிறது…"),
        ("dev_seed_done", "%1$d சோதனை உள்ளீடுகள் உருவாக்கப்பட்டன"),
        ("dev_seed_deleted", "%1$d சோதனை உள்ளீடுகள் நீக்கப்பட்டன"),
        ("dev_seed_error", "பிழை: %1$s"),
    ],
    "te": [
        ("dev_seed_create_label", "70 పరీక్ష ఎంట్రీలను సృష్టించండి"),
        ("dev_seed_delete_label", "70 పరీక్ష ఎంట్రీలను తొలగించండి"),
        ("dev_seed_running", "మీ భాషలో 70 పరీక్ష ఎంట్రీలను సృష్టిస్తోంది…"),
        ("dev_seed_done", "%1$d పరీక్ష ఎంట్రీలు సృష్టించబడ్డాయి"),
        ("dev_seed_deleted", "%1$d పరీక్ష ఎంట్రీలు తొలగించబడ్డాయి"),
        ("dev_seed_error", "లోపం: %1$s"),
    ],
    "th": [
        ("dev_seed_create_label", "สร้างรายการทดสอบ 70 รายการ"),
        ("dev_seed_delete_label", "ลบรายการทดสอบ 70 รายการ"),
        ("dev_seed_running", "กำลังสร้างรายการทดสอบ 70 รายการในภาษาของคุณ…"),
        ("dev_seed_done", "สร้างรายการทดสอบแล้ว %1$d รายการ"),
        ("dev_seed_deleted", "ลบรายการทดสอบแล้ว %1$d รายการ"),
        ("dev_seed_error", "ข้อผิดพลาด: %1$s"),
    ],
    "tr": [
        ("dev_seed_create_label", "70 test girdisi oluştur"),
        ("dev_seed_delete_label", "70 test girdisini sil"),
        ("dev_seed_running", "Dilinizde 70 test girdisi oluşturuluyor…"),
        ("dev_seed_done", "%1$d test girdisi oluşturuldu"),
        ("dev_seed_deleted", "%1$d test girdisi silindi"),
        ("dev_seed_error", "Hata: %1$s"),
    ],
    "uk": [  # Ukrainian — strictly NOT Russian
        ("dev_seed_create_label", "Створити 70 тестових записів"),
        ("dev_seed_delete_label", "Видалити 70 тестових записів"),
        ("dev_seed_running", "Створення 70 тестових записів вашою мовою…"),
        ("dev_seed_done", "Створено %1$d тестових записів"),
        ("dev_seed_deleted", "Видалено %1$d тестових записів"),
        ("dev_seed_error", "Помилка: %1$s"),
    ],
    "ur": [
        ("dev_seed_create_label", "70 ٹیسٹ اندراجات بنائیں"),
        ("dev_seed_delete_label", "70 ٹیسٹ اندراجات حذف کریں"),
        ("dev_seed_running", "آپ کی زبان میں 70 ٹیسٹ اندراجات بنائے جا رہے ہیں…"),
        ("dev_seed_done", "%1$d ٹیسٹ اندراجات بنائے گئے"),
        ("dev_seed_deleted", "%1$d ٹیسٹ اندراجات حذف کیے گئے"),
        ("dev_seed_error", "خرابی: %1$s"),
    ],
    "zh-rCN": [  # Simplified Chinese — full-width punctuation, comma ，
        ("dev_seed_create_label", "生成70条测试条目"),
        ("dev_seed_delete_label", "删除70条测试条目"),
        ("dev_seed_running", "正在以您的语言生成70条测试条目…"),
        ("dev_seed_done", "已创建%1$d条测试条目"),
        ("dev_seed_deleted", "已删除%1$d条测试条目"),
        ("dev_seed_error", "错误：%1$s"),
    ],
    "zh-rTW": [  # Traditional Chinese — full-width punctuation
        ("dev_seed_create_label", "產生70筆測試項目"),
        ("dev_seed_delete_label", "刪除70筆測試項目"),
        ("dev_seed_running", "正在以您的語言產生70筆測試項目…"),
        ("dev_seed_done", "已建立%1$d筆測試項目"),
        ("dev_seed_deleted", "已刪除%1$d筆測試項目"),
        ("dev_seed_error", "錯誤：%1$s"),
    ],
}

# Native digit ranges for Check 7 verification (must be ZERO after our edits)
INDIC_DIGITS = {
    "bn": "০১২৩৪৫৬৭৮৯",
    "hi": "०१२३४५६७८९",
    "mr": "०१२३४५६७८९",
    "te": "౦౧౨౩౪౫౬౭౮౯",
    "ta": "௦௧௨௩௪௫௬௭௮௯",
    "gu": "૦૧૨૩૪૫૬૭૮૯",
    "kn": "೦೧೨೩೪೫೬೭೮೯",
    "ml": "൦൧൨൩൪൫൬൭൮൯",
    "ur": "۰۱۲۳۴۵۶۷۸۹",
}


def safe_write(path: str, content: str) -> None:
    d = os.path.dirname(os.path.abspath(path))
    with tempfile.NamedTemporaryFile(
        "w", dir=d, suffix=".tmp", delete=False, encoding="utf-8", newline=""
    ) as tmp:
        tmp.write(content)
        tmp_path = tmp.name
    os.replace(tmp_path, path)


def process_locale(locale: str, strings: list) -> tuple:
    path = os.path.join(APP_DIR, f"values-{locale}", "strings.xml")
    if not os.path.exists(path):
        return (locale, "MISSING_FILE", 0, 0)
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()
    original = content

    # Skip if dev_seed_create_label already exists (idempotent)
    if 'name="dev_seed_create_label"' in content:
        return (locale, "ALREADY_PRESENT", 0, 0)

    # Build the block to insert
    lines = [
        "",
        "    <!-- DEBUG-only test data seeder (button in journal top bar, only visible in debug builds) -->",
    ]
    for key, val in strings:
        lines.append(f'    <string name="{key}">{val}</string>')
    block = "\n".join(lines) + "\n"

    # Insert before </resources>
    if "</resources>" not in content:
        return (locale, "NO_RESOURCES_TAG", 0, 0)
    content = content.replace("</resources>", block + "</resources>")

    # Update version line — match V0.14.x patterns
    version_fixes = 0
    new_content, n = re.subn(
        r'<string name="settings_about_version">[^<]+</string>',
        f'<string name="settings_about_version">{NEW_VERSION}</string>',
        content,
    )
    if n > 0:
        content = new_content
        version_fixes = n

    # Indic-digit guard (Check 7) — should be ZERO since we wrote 0-9
    if locale in INDIC_DIGITS:
        natives = INDIC_DIGITS[locale]
        bad = sum(1 for c in content if c in natives)
        if bad > 0:
            print(f"  WARN {locale}: {bad} native digits found — replacing", file=sys.stderr)
            content = content.translate(str.maketrans(natives, "0123456789"))

    if content != original:
        safe_write(path, content)
        return (locale, "OK", len(strings), version_fixes)
    return (locale, "NO_CHANGE", 0, 0)


def main():
    print(f"BestJournalAndroid — batch insert dev_seed_* strings into 25 locales")
    print(f"Target: {APP_DIR}")
    print(f"New version: {NEW_VERSION}\n")
    results = []
    for locale in sorted(TRANSLATIONS.keys()):
        result = process_locale(locale, TRANSLATIONS[locale])
        results.append(result)
        print(f"  values-{locale}: {result[1]} (strings: {result[2]}, version: {result[3]})")
    ok = sum(1 for r in results if r[1] == "OK")
    print(f"\n{ok}/{len(results)} locales updated successfully")
    return 0 if ok == len(results) else 1


if __name__ == "__main__":
    sys.exit(main())

"""
Add error_recording_failed and error_transcription_failed to all 25 language files.

Inserts the two new strings right after share_followup_single, matching the position
in values/strings.xml and values-en/strings.xml (already done manually).
"""
import re
import os
import tempfile

# Translations: locale -> (recording_failed, transcription_failed)
# Each string contains <xliff:g id="reason" example="...">%1$s</xliff:g>
TRANSLATIONS = {
    "fr": (
        "Échec de l\\'enregistrement : <xliff:g id=\"reason\" example=\"Microphone indisponible\">%1$s</xliff:g>",
        "Échec de la transcription : <xliff:g id=\"reason\" example=\"Délai d\\'attente réseau\">%1$s</xliff:g>",
    ),
    "es": (
        "Error al grabar: <xliff:g id=\"reason\" example=\"Micrófono no disponible\">%1$s</xliff:g>",
        "Error al transcribir: <xliff:g id=\"reason\" example=\"Tiempo de espera de red agotado\">%1$s</xliff:g>",
    ),
    "pt-rBR": (
        "Falha na gravação: <xliff:g id=\"reason\" example=\"Microfone indisponível\">%1$s</xliff:g>",
        "Falha na transcrição: <xliff:g id=\"reason\" example=\"Tempo limite de rede excedido\">%1$s</xliff:g>",
    ),
    "pt-rPT": (
        "Falha na gravação: <xliff:g id=\"reason\" example=\"Microfone indisponível\">%1$s</xliff:g>",
        "Falha na transcrição: <xliff:g id=\"reason\" example=\"Tempo de espera de rede excedido\">%1$s</xliff:g>",
    ),
    "it": (
        "Registrazione non riuscita: <xliff:g id=\"reason\" example=\"Microfono non disponibile\">%1$s</xliff:g>",
        "Trascrizione non riuscita: <xliff:g id=\"reason\" example=\"Timeout di rete\">%1$s</xliff:g>",
    ),
    "nl": (
        "Opname mislukt: <xliff:g id=\"reason\" example=\"Microfoon niet beschikbaar\">%1$s</xliff:g>",
        "Transcriptie mislukt: <xliff:g id=\"reason\" example=\"Netwerktime-out\">%1$s</xliff:g>",
    ),
    "pl": (
        "Nagrywanie nie powiodło się: <xliff:g id=\"reason\" example=\"Mikrofon niedostępny\">%1$s</xliff:g>",
        "Transkrypcja nie powiodła się: <xliff:g id=\"reason\" example=\"Limit czasu sieci\">%1$s</xliff:g>",
    ),
    "uk": (
        "Помилка запису: <xliff:g id=\"reason\" example=\"Мікрофон недоступний\">%1$s</xliff:g>",
        "Помилка транскрипції: <xliff:g id=\"reason\" example=\"Час очікування мережі\">%1$s</xliff:g>",
    ),
    "tr": (
        "Kayıt başarısız: <xliff:g id=\"reason\" example=\"Mikrofon kullanılamıyor\">%1$s</xliff:g>",
        "Yazıya dökme başarısız: <xliff:g id=\"reason\" example=\"Ağ zaman aşımı\">%1$s</xliff:g>",
    ),
    "ja": (
        "録音に失敗しました：<xliff:g id=\"reason\" example=\"マイクが使用できません\">%1$s</xliff:g>",
        "文字起こしに失敗しました：<xliff:g id=\"reason\" example=\"ネットワークタイムアウト\">%1$s</xliff:g>",
    ),
    "ko": (
        "녹음 실패: <xliff:g id=\"reason\" example=\"마이크를 사용할 수 없음\">%1$s</xliff:g>",
        "전사 실패: <xliff:g id=\"reason\" example=\"네트워크 시간 초과\">%1$s</xliff:g>",
    ),
    "zh-rCN": (
        "录音失败：<xliff:g id=\"reason\" example=\"麦克风不可用\">%1$s</xliff:g>",
        "转录失败：<xliff:g id=\"reason\" example=\"网络超时\">%1$s</xliff:g>",
    ),
    "zh-rTW": (
        "錄音失敗：<xliff:g id=\"reason\" example=\"麥克風無法使用\">%1$s</xliff:g>",
        "轉錄失敗：<xliff:g id=\"reason\" example=\"網路逾時\">%1$s</xliff:g>",
    ),
    "ar": (
        "فشل التسجيل: <xliff:g id=\"reason\" example=\"الميكروفون غير متاح\">%1$s</xliff:g>",
        "فشل التحويل النصي: <xliff:g id=\"reason\" example=\"انتهت مهلة الشبكة\">%1$s</xliff:g>",
    ),
    "hi": (
        "रिकॉर्डिंग विफल: <xliff:g id=\"reason\" example=\"माइक्रोफ़ोन उपलब्ध नहीं\">%1$s</xliff:g>",
        "ट्रांसक्रिप्शन विफल: <xliff:g id=\"reason\" example=\"नेटवर्क टाइमआउट\">%1$s</xliff:g>",
    ),
    "th": (
        "การบันทึกล้มเหลว: <xliff:g id=\"reason\" example=\"ไม่สามารถใช้ไมโครโฟนได้\">%1$s</xliff:g>",
        "การถอดเสียงล้มเหลว: <xliff:g id=\"reason\" example=\"หมดเวลาเครือข่าย\">%1$s</xliff:g>",
    ),
    "in": (
        "Perekaman gagal: <xliff:g id=\"reason\" example=\"Mikrofon tidak tersedia\">%1$s</xliff:g>",
        "Transkripsi gagal: <xliff:g id=\"reason\" example=\"Waktu jaringan habis\">%1$s</xliff:g>",
    ),
    "bn": (
        "রেকর্ডিং ব্যর্থ হয়েছে: <xliff:g id=\"reason\" example=\"মাইক্রোফোন অনুপলব্ধ\">%1$s</xliff:g>",
        "ট্রান্সক্রিপশন ব্যর্থ হয়েছে: <xliff:g id=\"reason\" example=\"নেটওয়ার্ক টাইমআউট\">%1$s</xliff:g>",
    ),
    "te": (
        "రికార్డింగ్ విఫలమైంది: <xliff:g id=\"reason\" example=\"మైక్రోఫోన్ అందుబాటులో లేదు\">%1$s</xliff:g>",
        "ట్రాన్స్‌క్రిప్షన్ విఫలమైంది: <xliff:g id=\"reason\" example=\"నెట్‌వర్క్ గడువు ముగిసింది\">%1$s</xliff:g>",
    ),
    "mr": (
        "रेकॉर्डिंग अयशस्वी: <xliff:g id=\"reason\" example=\"मायक्रोफोन उपलब्ध नाही\">%1$s</xliff:g>",
        "ट्रान्सक्रिप्शन अयशस्वी: <xliff:g id=\"reason\" example=\"नेटवर्क कालबाह्य\">%1$s</xliff:g>",
    ),
    "ta": (
        "பதிவு தோல்வியடைந்தது: <xliff:g id=\"reason\" example=\"ஒலிவாங்கி கிடைக்கவில்லை\">%1$s</xliff:g>",
        "உரை மாற்றம் தோல்வியடைந்தது: <xliff:g id=\"reason\" example=\"நெட்வொர்க் காலாவதியானது\">%1$s</xliff:g>",
    ),
    "ur": (
        "ریکارڈنگ ناکام: <xliff:g id=\"reason\" example=\"مائیکروفون دستیاب نہیں\">%1$s</xliff:g>",
        "ٹرانسکرپشن ناکام: <xliff:g id=\"reason\" example=\"نیٹ ورک ٹائم آؤٹ\">%1$s</xliff:g>",
    ),
    "gu": (
        "રેકોર્ડિંગ નિષ્ફળ: <xliff:g id=\"reason\" example=\"માઇક્રોફોન ઉપલબ્ધ નથી\">%1$s</xliff:g>",
        "ટ્રાન્સક્રિપ્શન નિષ્ફળ: <xliff:g id=\"reason\" example=\"નેટવર્ક સમય સમાપ્ત\">%1$s</xliff:g>",
    ),
    "kn": (
        "ರೆಕಾರ್ಡಿಂಗ್ ವಿಫಲವಾಗಿದೆ: <xliff:g id=\"reason\" example=\"ಮೈಕ್ರೋಫೋನ್ ಲಭ್ಯವಿಲ್ಲ\">%1$s</xliff:g>",
        "ಪ್ರತಿಲೇಖನ ವಿಫಲವಾಗಿದೆ: <xliff:g id=\"reason\" example=\"ನೆಟ್ವರ್ಕ್ ಸಮಯಾವಧಿ ಮುಗಿದಿದೆ\">%1$s</xliff:g>",
    ),
    "ml": (
        "റെക്കോർഡിംഗ് പരാജയപ്പെട്ടു: <xliff:g id=\"reason\" example=\"മൈക്രോഫോൺ ലഭ്യമല്ല\">%1$s</xliff:g>",
        "ട്രാൻസ്ക്രിപ്ഷൻ പരാജയപ്പെട്ടു: <xliff:g id=\"reason\" example=\"നെറ്റ്‌വർക്ക് കാലഹരണപ്പെട്ടു\">%1$s</xliff:g>",
    ),
}

BASE_DIR = os.path.expanduser("~/proggs/BestJournalAndroid/app/src/main/res")

PATTERN_OLD = re.compile(
    r'(<string name="share_followup_single">[^<]*</string>)\n',
)

modified = 0
skipped = 0
errors = []

for locale, (recording, transcription) in TRANSLATIONS.items():
    path = os.path.join(BASE_DIR, f"values-{locale}", "strings.xml")
    if not os.path.exists(path):
        errors.append(f"FILE NOT FOUND: {path}")
        continue

    with open(path, "r", encoding="utf-8") as f:
        content = f.read()

    if 'name="error_recording_failed"' in content:
        skipped += 1
        print(f"SKIP {locale}: already has error_recording_failed")
        continue

    new_block = (
        f'\\1\n\n'
        f'    <string name="error_recording_failed">{recording}</string>\n'
        f'    <string name="error_transcription_failed">{transcription}</string>\n'
    )
    new_content, n = PATTERN_OLD.subn(new_block, content, count=1)

    if n == 0:
        errors.append(f"PATTERN NOT FOUND in {locale}")
        continue

    # Atomic write
    d = os.path.dirname(path)
    with tempfile.NamedTemporaryFile(
        "w", dir=d, suffix=".tmp", delete=False, encoding="utf-8"
    ) as tmp:
        tmp.write(new_content)
        tmp_path = tmp.name
    os.replace(tmp_path, path)
    modified += 1
    print(f"OK {locale}: 2 strings added")

print(f"\n=== SUMMARY ===")
print(f"Modified: {modified}")
print(f"Skipped:  {skipped}")
print(f"Errors:   {len(errors)}")
for e in errors:
    print(f"  - {e}")

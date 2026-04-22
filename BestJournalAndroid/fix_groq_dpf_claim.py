"""
Fix DPF claim for Groq across all 26 locales.

Root cause: consent_card2_body and (in de/en) privacy_gate_groq_body falsely
claimed EU-US Data Privacy Framework coverage for Groq. Groq is NOT DPF
certified (verified via Groq's own DPA: console.groq.com/docs/legal/customer-
data-processing-addendum — it uses EU SCCs Module 2/3, not DPF).

This script:
1. Replaces consent_card2_body in ALL 26 locales with a version that scopes
   DPF only to Google Gemini + Microsoft Edge, and declares Groq separately
   under Standard Contractual Clauses.
2. Replaces privacy_gate_groq_body in EN only (DE already patched manually,
   other locales don't contain the DPF claim for Groq).
"""
from __future__ import annotations

import os
import re
from pathlib import Path

RES_DIR = Path(os.path.expanduser("~/proggs/BestJournalAndroid/app/src/main/res"))

# Note: DE (values/) already patched manually, not included here.
CONSENT_CARD2_BODY: dict[str, str] = {
    "en": "Optionally, text is sent to Google Gemini and read-aloud text to Microsoft Edge in the USA (EU-US Data Privacy Framework). Voice recordings are sent to Groq (USA) under Standard Contractual Clauses (EU SCCs).",
    "fr": "Tes textes peuvent être envoyés à Google Gemini et les textes à lire à Microsoft Edge aux États-Unis (cadre EU-US Data Privacy Framework). Les enregistrements vocaux sont transmis à Groq (États-Unis) sous clauses contractuelles types (EU SCCs).",
    "es": "De forma opcional se envían textos a Google Gemini y textos de lectura en voz alta a Microsoft Edge en EE. UU. (marco EU-US Data Privacy Framework). Las grabaciones de voz se envían a Groq (EE. UU.) conforme a las cláusulas contractuales tipo (EU SCCs).",
    "it": "Facoltativamente, i testi vengono inviati a Google Gemini e i testi da leggere ad alta voce a Microsoft Edge negli USA (EU-US Data Privacy Framework). Le registrazioni vocali vengono inviate a Groq (USA) in base alle clausole contrattuali tipo (EU SCCs).",
    "pt-rPT": "Opcionalmente, os textos são enviados ao Google Gemini e os textos para leitura em voz alta à Microsoft Edge nos EUA (EU-US Data Privacy Framework). As gravações de voz são enviadas à Groq (EUA) com cláusulas contratuais-tipo (EU SCCs).",
    "pt-rBR": "Opcionalmente, os textos são enviados ao Google Gemini e os textos para leitura em voz alta à Microsoft Edge nos EUA (EU-US Data Privacy Framework). As gravações de voz são enviadas à Groq (EUA) com cláusulas contratuais padrão (EU SCCs).",
    "nl": "Optioneel worden teksten verzonden naar Google Gemini en voorgelezen teksten naar Microsoft Edge in de VS (EU-US Data Privacy Framework). Spraakopnames gaan naar Groq (VS) onder Standaard Contractbepalingen (EU SCCs).",
    "pl": "Opcjonalnie teksty są wysyłane do Google Gemini, a teksty do odczytu do Microsoft Edge w USA (EU-US Data Privacy Framework). Nagrania głosowe trafiają do Groq (USA) na podstawie standardowych klauzul umownych (EU SCCs).",
    "tr": "İsteğe bağlı olarak metinler Google Gemini\\'ye ve seslendirilecek metinler Microsoft Edge\\'e ABD\\'de gönderilir (EU-US Data Privacy Framework). Ses kayıtları, Standart Sözleşme Hükümleri (EU SCCs) kapsamında Groq\\'a (ABD) gönderilir.",
    "uk": "За бажанням тексти надсилаються до Google Gemini, а тексти для озвучення — до Microsoft Edge у США (EU-US Data Privacy Framework). Голосові записи надсилаються до Groq (США) на підставі стандартних договірних положень (EU SCCs).",
    "ja": "任意でテキストはGoogle Geminiへ、読み上げ用テキストはMicrosoft Edgeへ米国に送信されます（EU-US Data Privacy Framework）。音声録音は標準契約条項（EU SCCs）に基づきGroq（米国）に送信されます。",
    "ko": "선택적으로 텍스트는 Google Gemini로, 읽어주기 텍스트는 Microsoft Edge로 미국에 전송됩니다 (EU-US Data Privacy Framework). 음성 녹음은 표준계약조항 (EU SCCs) 에 따라 Groq (미국) 로 전송됩니다.",
    "zh-rCN": "可选地,文本会发送到 Google Gemini,朗读文本发送到 Microsoft Edge 位于美国的服务器(EU-US Data Privacy Framework)。语音录音会依据标准合同条款(EU SCCs)发送到 Groq(美国)。",
    "zh-rTW": "選用時,文字會傳送到位於美國的 Google Gemini,朗讀文字則傳送到 Microsoft Edge(EU-US Data Privacy Framework)。語音錄音依據標準合約條款(EU SCCs)傳送到 Groq(美國)。",
    "ar": "اختياريًا تُرسل النصوص إلى Google Gemini ونصوص القراءة بصوت عالٍ إلى Microsoft Edge في الولايات المتحدة (إطار EU-US Data Privacy Framework). التسجيلات الصوتية تُرسل إلى Groq (الولايات المتحدة) وفق الشروط التعاقدية النموذجية (EU SCCs).",
    "hi": "वैकल्पिक रूप से टेक्स्ट Google Gemini को और टेक्स्ट-टू-स्पीच के टेक्स्ट Microsoft Edge को USA में भेजे जाते हैं (EU-US Data Privacy Framework)। वॉयस रिकॉर्डिंग Standard Contractual Clauses (EU SCCs) के तहत Groq (USA) को भेजी जाती हैं।",
    "ur": "اختیاری طور پر متن Google Gemini اور پڑھنے کے متن Microsoft Edge کو امریکہ میں بھیجے جاتے ہیں (EU-US Data Privacy Framework)۔ آواز کی ریکارڈنگ معیاری معاہدہ شرائط (EU SCCs) کے تحت Groq (امریکہ) کو بھیجی جاتی ہے۔",
    "bn": "ঐচ্ছিকভাবে টেক্সট Google Gemini-তে এবং জোরে পড়ার টেক্সট Microsoft Edge-এ USA-তে পাঠানো হয় (EU-US Data Privacy Framework)। ভয়েস রেকর্ডিং Standard Contractual Clauses (EU SCCs) অনুযায়ী Groq (USA)-এ পাঠানো হয়।",
    "ta": "விருப்பப்படி உரைகள் Google Gemini-க்கும், உரக்கப் படிக்கும் உரைகள் Microsoft Edge-க்கும் USA-வில் அனுப்பப்படும் (EU-US Data Privacy Framework). குரல் பதிவுகள் Standard Contractual Clauses (EU SCCs)-ன் கீழ் Groq (USA)-க்கு அனுப்பப்படும்.",
    "te": "ఐచ్ఛికంగా టెక్స్ట్‌లు Google Gemini కి మరియు చదివే టెక్స్ట్‌లు Microsoft Edge కి USA లో పంపబడతాయి (EU-US Data Privacy Framework). వాయిస్ రికార్డింగ్‌లు Standard Contractual Clauses (EU SCCs) ప్రకారం Groq (USA) కి పంపబడతాయి.",
    "kn": "ಐಚ್ಛಿಕವಾಗಿ ಪಠ್ಯಗಳು Google Gemini ಗೆ ಮತ್ತು ಧ್ವನಿ-ಓದುವ ಪಠ್ಯಗಳು Microsoft Edge ಗೆ USA ಗೆ ಕಳುಹಿಸಲಾಗುತ್ತವೆ (EU-US Data Privacy Framework). ಧ್ವನಿ ರೆಕಾರ್ಡಿಂಗ್‌ಗಳು Standard Contractual Clauses (EU SCCs) ಅಡಿಯಲ್ಲಿ Groq (USA) ಗೆ ಕಳುಹಿಸಲಾಗುತ್ತವೆ.",
    "ml": "ഐച്ഛികമായി ടെക്‌സ്റ്റുകൾ Google Gemini-ലേക്കും ഉച്ചത്തിൽ വായിക്കാനുള്ള ടെക്‌സ്റ്റുകൾ Microsoft Edge-ലേക്കും USA-യിലേക്ക് അയക്കുന്നു (EU-US Data Privacy Framework). വോയ്സ് റെക്കോർഡിംഗുകൾ Standard Contractual Clauses (EU SCCs) പ്രകാരം Groq (USA)-ലേക്ക് അയക്കുന്നു.",
    "mr": "पर्यायाने मजकूर Google Gemini ला आणि मोठ्याने वाचायचे मजकूर Microsoft Edge ला USA मध्ये पाठवले जातात (EU-US Data Privacy Framework). आवाज रेकॉर्डिंग मानक करार कलमांखाली (EU SCCs) Groq (USA) ला पाठवली जातात.",
    "gu": "વૈકલ્પિક રીતે ટેક્સ્ટ Google Gemini ને અને મોટેથી વાંચવાનો ટેક્સ્ટ Microsoft Edge ને USA માં મોકલવામાં આવે છે (EU-US Data Privacy Framework). વૉઇસ રેકોર્ડિંગ માનક કરાર કલમો (EU SCCs) હેઠળ Groq (USA) ને મોકલવામાં આવે છે.",
    "th": "เลือกได้ว่าจะส่งข้อความไปยัง Google Gemini และข้อความสำหรับอ่านออกเสียงไปยัง Microsoft Edge ในสหรัฐอเมริกา (EU-US Data Privacy Framework) การบันทึกเสียงจะถูกส่งไปยัง Groq (สหรัฐอเมริกา) ภายใต้ข้อสัญญามาตรฐาน (EU SCCs)",
    "in": "Secara opsional, teks dikirim ke Google Gemini dan teks untuk dibacakan ke Microsoft Edge di AS (EU-US Data Privacy Framework). Rekaman suara dikirim ke Groq (AS) berdasarkan Klausul Kontrak Standar (EU SCCs).",
}

# privacy_gate_groq_body replacements: only EN needs a change.
# The existing EN text contains "✓ Encrypted under EU-US Data Privacy Framework"
# which must be replaced. We replace the whole string to also drop the
# concrete DPF bullet while keeping the other two ✓-bullets.
PRIVACY_GATE_GROQ_BODY_EN = (
    "This feature sends your voice recording encrypted to Groq (USA) and "
    "converts it to text — faster and more accurate than on-device recognition."
    "\\n\\n"
    "✓ Recording is deleted immediately after conversion\\n"
    "✓ Never used to train AI models\\n"
    "✓ Legal basis: Standard Contractual Clauses (EU SCCs)"
    "\\n\\n"
    "Alternative: on-device offline recognition in Settings."
)


def _replace_string(content: str, name: str, new_body: str) -> tuple[str, bool]:
    """Replace the *body* of the <string name="..."> element. Returns (content, changed)."""
    pattern = re.compile(
        r'(<string\s+name="' + re.escape(name) + r'"[^>]*>)(.*?)(</string>)',
        re.DOTALL,
    )
    replaced = False

    def _sub(match: re.Match[str]) -> str:
        nonlocal replaced
        if match.group(2) == new_body:
            return match.group(0)
        replaced = True
        return match.group(1) + new_body + match.group(3)

    new_content = pattern.sub(_sub, content, count=1)
    return new_content, replaced


def _patch_file(path: Path, name: str, new_body: str) -> bool:
    text = path.read_text(encoding="utf-8")
    new_text, changed = _replace_string(text, name, new_body)
    if changed:
        path.write_text(new_text, encoding="utf-8", newline="\n")
    return changed


def main() -> None:
    changed_files: list[str] = []
    missing_files: list[str] = []

    for locale, new_body in CONSENT_CARD2_BODY.items():
        xml = RES_DIR / f"values-{locale}" / "strings.xml"
        if not xml.exists():
            missing_files.append(str(xml))
            continue
        if _patch_file(xml, "consent_card2_body", new_body):
            changed_files.append(f"consent_card2_body: {xml.relative_to(RES_DIR)}")

    en_xml = RES_DIR / "values-en" / "strings.xml"
    if en_xml.exists():
        if _patch_file(en_xml, "privacy_gate_groq_body", PRIVACY_GATE_GROQ_BODY_EN):
            changed_files.append(f"privacy_gate_groq_body: {en_xml.relative_to(RES_DIR)}")

    print(f"Patched {len(changed_files)} string(s):")
    for entry in changed_files:
        print(f"  - {entry}")
    if missing_files:
        print("Missing files (skipped):")
        for path in missing_files:
            print(f"  - {path}")


if __name__ == "__main__":
    main()

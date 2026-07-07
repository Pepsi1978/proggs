"""Add settings_report_ai_dialog_title and _desc to all 25 language files.

Inserts the two new strings right after settings_report_ai_body, matching the
position in values/strings.xml and values-en/strings.xml (already done manually).
"""
import os, re, tempfile

# (title, desc) per locale
TR = {
    "fr": ("Signaler une réponse de l\\'IA",
           "Décrivez la réponse inappropriée ou incorrecte de l\\'IA. Votre signalement sera envoyé directement à notre équipe d\\'assistance. Vous devez être connecté avec votre compte Google."),
    "es": ("Reportar respuesta de la IA",
           "Describe la respuesta inapropiada o incorrecta de la IA. Tu reporte se envía directamente a nuestro equipo de soporte. Debes haber iniciado sesión con tu cuenta de Google."),
    "pt-rBR": ("Reportar resposta da IA",
               "Descreva a resposta inadequada ou incorreta da IA. Seu relatório é enviado diretamente para nossa equipe de suporte. Você precisa estar conectado com sua conta do Google."),
    "pt-rPT": ("Comunicar resposta da IA",
               "Descreva a resposta inadequada ou incorreta da IA. A sua comunicação é enviada diretamente para a nossa equipa de apoio. Tem de ter sessão iniciada com a sua conta Google."),
    "it": ("Segnala risposta dell\\'IA",
           "Descrivi la risposta inappropriata o errata dell\\'IA. La tua segnalazione viene inviata direttamente al nostro team di assistenza. Devi aver effettuato l\\'accesso con il tuo account Google."),
    "nl": ("AI-antwoord melden",
           "Beschrijf het ongepaste of onjuiste AI-antwoord. Je melding wordt direct naar ons supportteam gestuurd. Je moet ingelogd zijn met je Google-account."),
    "pl": ("Zgłoś odpowiedź AI",
           "Opisz nieodpowiednią lub błędną odpowiedź AI. Twoje zgłoszenie zostanie wysłane bezpośrednio do naszego zespołu wsparcia. Musisz być zalogowany na swoje konto Google."),
    "uk": ("Повідомити про відповідь ШІ",
           "Опишіть неприйнятну або неправильну відповідь ШІ. Ваше повідомлення буде надіслано безпосередньо нашій службі підтримки. Ви повинні бути в обліковому записі Google."),
    "tr": ("Yapay zeka yanıtını bildir",
           "Uygunsuz veya hatalı yapay zeka yanıtını açıklayın. Bildiriminiz doğrudan destek ekibimize gönderilir. Google hesabınızla giriş yapmış olmanız gerekir."),
    "ja": ("AI応答を報告",
           "不適切または誤ったAI応答について説明してください。報告は直接サポートチームに送信されます。Googleアカウントでサインインしている必要があります。"),
    "ko": ("AI 응답 신고",
           "부적절하거나 잘못된 AI 응답을 설명해 주세요. 신고는 지원팀에 직접 전송됩니다. Google 계정으로 로그인되어 있어야 합니다。"),
    "zh-rCN": ("举报AI回答",
               "描述不当或错误的AI回答。您的举报将直接发送给我们的支持团队。您需要使用Google账户登录。"),
    "zh-rTW": ("檢舉AI回應",
               "描述不當或錯誤的AI回應。您的檢舉會直接傳送給我們的支援團隊。您必須使用Google帳戶登入。"),
    "ar": ("الإبلاغ عن استجابة الذكاء الاصطناعي",
           "صف استجابة الذكاء الاصطناعي غير الملائمة أو غير الصحيحة. يتم إرسال بلاغك مباشرة إلى فريق الدعم لدينا. يجب أن تكون مسجل الدخول بحساب Google الخاص بك."),
    "hi": ("AI प्रतिक्रिया रिपोर्ट करें",
           "अनुचित या गलत AI प्रतिक्रिया का वर्णन करें। आपकी रिपोर्ट सीधे हमारी सहायता टीम को भेजी जाती है। आपको अपने Google खाते से साइन इन होना चाहिए।"),
    "th": ("รายงานคำตอบ AI",
           "อธิบายคำตอบ AI ที่ไม่เหมาะสมหรือไม่ถูกต้อง รายงานของคุณจะถูกส่งตรงไปยังทีมสนับสนุนของเรา คุณต้องลงชื่อเข้าใช้ด้วยบัญชี Google ของคุณ"),
    "in": ("Laporkan respons AI",
           "Jelaskan respons AI yang tidak pantas atau salah. Laporan Anda dikirim langsung ke tim dukungan kami. Anda harus masuk dengan akun Google Anda."),
    "bn": ("AI প্রতিক্রিয়া রিপোর্ট করুন",
           "অনুপযুক্ত বা ভুল AI প্রতিক্রিয়ার বর্ণনা দিন। আপনার রিপোর্ট সরাসরি আমাদের সাপোর্ট টিমে পাঠানো হবে। আপনাকে আপনার Google অ্যাকাউন্ট দিয়ে সাইন ইন করতে হবে।"),
    "te": ("AI ప్రతిస్పందనను నివేదించండి",
           "అనుచితమైన లేదా తప్పుడు AI ప్రతిస్పందనను వివరించండి. మీ నివేదిక నేరుగా మా మద్దతు బృందానికి పంపబడుతుంది. మీరు మీ Google ఖాతాతో సైన్ ఇన్ అయి ఉండాలి."),
    "mr": ("AI प्रतिसाद नोंदवा",
           "अयोग्य किंवा चुकीच्या AI प्रतिसादाचे वर्णन करा. तुमचा अहवाल थेट आमच्या सपोर्ट टीमला पाठवला जातो. तुम्ही तुमच्या Google खात्याने साइन इन केलेले असले पाहिजे."),
    "ta": ("AI பதிலைப் புகாரளி",
           "பொருத்தமற்ற அல்லது தவறான AI பதிலை விவரிக்கவும். உங்கள் புகார் நேரடியாக எங்கள் ஆதரவுக் குழுவுக்கு அனுப்பப்படுகிறது. நீங்கள் உங்கள் Google கணக்கில் உள்நுழைந்திருக்க வேண்டும்."),
    "ur": ("AI جواب کی رپورٹ کریں",
           "نامناسب یا غلط AI جواب کی وضاحت کریں۔ آپ کی رپورٹ براہ راست ہماری سپورٹ ٹیم کو بھیجی جاتی ہے۔ آپ کا اپنے Google اکاؤنٹ سے سائن ان ہونا ضروری ہے۔"),
    "gu": ("AI પ્રતિસાદની જાણ કરો",
           "અયોગ્ય અથવા ખોટા AI પ્રતિસાદનું વર્ણન કરો. તમારો અહેવાલ સીધો અમારી સપોર્ટ ટીમને મોકલવામાં આવે છે. તમે તમારા Google એકાઉન્ટથી સાઇન ઇન કરેલા હોવા જોઈએ."),
    "kn": ("AI ಪ್ರತಿಕ್ರಿಯೆಯನ್ನು ವರದಿ ಮಾಡಿ",
           "ಸೂಕ್ತವಲ್ಲದ ಅಥವಾ ತಪ್ಪಾದ AI ಪ್ರತಿಕ್ರಿಯೆಯನ್ನು ವಿವರಿಸಿ. ನಿಮ್ಮ ವರದಿಯನ್ನು ನೇರವಾಗಿ ನಮ್ಮ ಬೆಂಬಲ ತಂಡಕ್ಕೆ ಕಳುಹಿಸಲಾಗುತ್ತದೆ. ನೀವು ನಿಮ್ಮ Google ಖಾತೆಯೊಂದಿಗೆ ಸೈನ್ ಇನ್ ಆಗಿರಬೇಕು."),
    "ml": ("AI പ്രതികരണം റിപ്പോർട്ട് ചെയ്യുക",
           "അനുചിതമോ തെറ്റോ ആയ AI പ്രതികരണം വിവരിക്കുക. നിങ്ങളുടെ റിപ്പോർട്ട് നേരിട്ട് ഞങ്ങളുടെ പിന്തുണാ ടീമിന് അയയ്ക്കും. നിങ്ങൾ Google അക്കൗണ്ടിൽ സൈൻ ഇൻ ചെയ്തിരിക്കണം."),
}

BASE = os.path.expanduser("~/proggs/BestJournalAndroid/app/src/main/res")

# Match the line that ends with </string> after settings_report_ai_body, then insert.
PAT = re.compile(
    r'(<string name="settings_report_ai_body">.*?</string>)\n',
    re.DOTALL,
)

modified = 0
errors = []

for locale, (title, desc) in TR.items():
    path = os.path.join(BASE, f"values-{locale}", "strings.xml")
    if not os.path.exists(path):
        errors.append(f"FILE NOT FOUND: {path}")
        continue
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()
    if 'name="settings_report_ai_dialog_title"' in content:
        print(f"SKIP {locale}: already has settings_report_ai_dialog_title")
        continue
    new_block = (
        f'\\1\n'
        f'    <string name="settings_report_ai_dialog_title">{title}</string>\n'
        f'    <string name="settings_report_ai_dialog_desc">{desc}</string>\n'
    )
    new_content, n = PAT.subn(new_block, content, count=1)
    if n == 0:
        errors.append(f"PATTERN NOT FOUND in {locale}")
        continue
    d = os.path.dirname(path)
    with tempfile.NamedTemporaryFile("w", dir=d, suffix=".tmp", delete=False, encoding="utf-8") as tmp:
        tmp.write(new_content)
        tmp_path = tmp.name
    os.replace(tmp_path, path)
    modified += 1
    print(f"OK {locale}: 2 strings added")

print(f"\nModified: {modified} | Errors: {len(errors)}")
for e in errors:
    print(f"  - {e}")

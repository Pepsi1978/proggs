#!/usr/bin/env python3
"""Batch 5: ta, ur, gu, kn, ml."""
import os, re, tempfile

APP_DIR = os.path.expanduser("~/proggs/BestJournalAndroid/app/src/main/res")
TRANSLATIONS = {}

# ═══════════ TAMIL (ta) — நீங்கள், Tamil script, native Tamil ═══════════
TRANSLATIONS["ta"] = r"""
    <!-- ════════════════════════════════════════════════════════════════════
         CONSENT SCREEN + PRIVACY GATES + PRIVACY SECTION (April 2026)
         ════════════════════════════════════════════════════════════════════ -->
    <string name="consent_title">தனியுரிமை மற்றும் ஒப்புதல்</string>
    <string name="consent_intro">உங்கள் நாட்குறிப்பு ஒரு தனிப்பட்ட இடம், அதை நாங்கள் மதிக்கிறோம். Best Journal உங்கள் தரவை எப்படிக் கையாள்கிறது என்பதை இங்கு வெளிப்படையாகப் பார்க்கலாம்.</string>
    <string name="consent_card1_title">உள்ளமை சேமிப்பு</string>
    <string name="consent_card1_body">உங்கள் பதிவுகள் உங்கள் சாதனத்திலேயே இருக்கும்.</string>
    <string name="consent_card2_title">AI அம்சங்கள் (USA)</string>
    <string name="consent_card2_body">விருப்பமாக, உரை Google Gemini-க்கும், குரல் பதிவுகள் Groq-க்கும், உரை-வாசிப்பு Microsoft Edge-க்கும் USA-வில் அனுப்பப்படும் (EU-US Data Privacy Framework + Standard Contractual Clauses).</string>
    <string name="consent_card3_title">அடையாளமற்ற புள்ளிவிவரங்கள்</string>
    <string name="consent_card3_body">Firebase Analytics, விருப்பத்தேர்வு, அமைப்புகளில் எப்போது வேண்டுமானாலும் மாற்றலாம்.</string>
    <string name="consent_links_header">எங்கள் சட்டப்பூர்வ ஆவணங்கள்:</string>
    <string name="consent_accept_all">ஒப்புக்கொண்டு தொடங்கு</string>
    <string name="consent_disable_stats">புள்ளிவிவரங்களை முடக்கித் தொடரு</string>
    <string name="consent_confirmation">\"ஒப்புக்கொண்டு தொடங்கு\" என்பதைத் தட்டுவதன் மூலம் நீங்கள் தனியுரிமைக் கொள்கை, பயன்பாட்டு விதிமுறைகள் மற்றும் சட்டப்பூர்வ தகவலைப் படித்ததாகவும், விவரித்த தரவு செயலாக்கத்திற்கு ஒப்புக்கொள்கிறீர்கள் என்றும் உறுதிப்படுத்துகிறீர்கள். உங்கள் முடிவை அமைப்புகளில் எப்போது வேண்டுமானாலும் மாற்றலாம்.</string>

    <string name="privacy_gate_groq_title">Groq-க்கு குரல் பதிவை அனுப்பவா?</string>
    <string name="privacy_gate_groq_body">கிளவுட் டிரான்ஸ்கிரிப்ஷனுக்காக உங்கள் குரல் பதிவு குறியாக்கப்பட்டு Groq, Inc.-க்கு (Mountain View, USA) அனுப்பப்பட்டு அங்கு உரையாக மாற்றப்படும். ஆடியோ கோப்பு செயலாக்கத்திற்குப் பிறகு அழிக்கப்படும், பயிற்சிக்குப் பயன்படுத்தப்படாது.\n\nமாற்று: சாதனத்தில் உள்ளமை டிரான்ஸ்கிரிப்ஷனைப் பயன்படுத்துங்கள் (ஆஃப்லைன், தரவு பரிமாற்றம் இல்லை), Settings → AI-ல் மாற்றலாம்.</string>
    <string name="privacy_gate_groq_accept">ஒப்புக்கொண்டு அனுப்பு</string>
    <string name="privacy_gate_groq_local">அதற்குப் பதிலாக உள்ளமையில் டிரான்ஸ்கிரைப் செய்</string>

    <string name="privacy_gate_gemini_title">Google Gemini-க்கு உரையை அனுப்பவா?</string>
    <string name="privacy_gate_gemini_body">AI அம்சங்களுக்காக (டாஷ்போர்டு, சுருக்கங்கள், பின்னோக்கம், உரை மேம்பாடு) உங்கள் பதிவுகளின் பகுதிகள் குறியாக்கப்பட்டு Google Gemini-க்கு (Firebase AI, USA) அனுப்பப்படும். சட்ட அடிப்படை: EU-US Data Privacy Framework + Standard Contractual Clauses. கோரிக்கைகள் செயலாக்கத்திற்குப் பிறகு அழிக்கப்படும், பயிற்சிக்குப் பயன்படுத்தப்படாது.</string>
    <string name="privacy_gate_gemini_accept">ஒப்புக்கொண்டு அனுப்பு</string>
    <string name="privacy_gate_gemini_cancel">ரத்து</string>

    <string name="privacy_gate_tts_title">Microsoft-க்கு உரையை அனுப்பவா?</string>
    <string name="privacy_gate_tts_body">வாசித்துக் காட்ட, உரை குறியாக்கப்பட்டு Microsoft Bing Speech-க்கு (USA) அனுப்பப்பட்டு ஆடியோவாகத் திரும்பி வரும். சட்ட அடிப்படை: EU-US Data Privacy Framework + Standard Contractual Clauses.\n\nமாற்று: Android-ன் உள்ளமைந்த ஆஃப்லைன் TTS-ஐப் பயன்படுத்துங்கள்.</string>
    <string name="privacy_gate_tts_accept">ஒப்புக்கொண்டு வாசி</string>
    <string name="privacy_gate_tts_cancel">ரத்து</string>

    <string name="settings_privacy_header">தனியுரிமை</string>
    <string name="settings_analytics_title">அடையாளமற்ற புள்ளிவிவரங்கள்</string>
    <string name="settings_analytics_subtitle">பிழை பகுப்பாய்வு மற்றும் தயாரிப்பு மேம்பாட்டிற்கான Firebase Analytics</string>

    <string name="settings_delete_account_title">கணக்கையும் தரவையும் நீக்கு</string>
    <string name="settings_delete_account_subtitle">அனைத்து உள்ளமை தரவு, Google கணக்கு மற்றும் Drive காப்புப்பிரதியை நிரந்தரமாக நீக்குகிறது</string>
    <string name="settings_delete_account_confirm_title">கணக்கை நிரந்தரமாக நீக்கவா?</string>
    <string name="settings_delete_account_confirm_body">இந்தச் செயலை மீட்க முடியாது மற்றும் நீக்குவது:\n\n• அனைத்து உள்ளமை நாட்குறிப்புப் பதிவுகள், புகைப்படங்கள் மற்றும் வீடியோக்கள்\n• உங்கள் Firebase கணக்கு\n• பயன்பாட்டின் Google Drive காப்புப்பிரதி\n\nஅதன்பிறகு பயன்பாடு புதிய நிறுவலாக மீண்டும் தொடங்கும்.</string>
    <string name="settings_delete_account_cancel">ரத்து</string>
    <string name="settings_delete_account_confirm">ஆம், அனைத்தையும் நீக்கு</string>

    <string name="settings_report_ai_title">AI பதிலைப் புகாரளி</string>
    <string name="settings_report_ai_subtitle">தகாத அல்லது தவறான AI வெளியீடு</string>
    <string name="settings_report_ai_confirm_title">Support-க்கு மின்னஞ்சலைத் திறக்கவா?</string>
    <string name="settings_report_ai_confirm_body">உங்கள் மின்னஞ்சல் பயன்பாடு dev.app.support@gmail.com-க்கு தயார் செய்யப்பட்ட செய்தியுடன் திறக்கும். அனுப்புவதற்கு முன் விளக்கத்தை சேர்க்கலாம். வேலை நாட்களில் 24 மணி நேரத்திற்குள் பதிலளிப்போம்.\n\nதயவுசெய்து இங்கு புகாரளிக்கவும்: டாஷ்போர்டு, சுருக்கங்கள், பின்னோக்கம் அல்லது உரை மேம்பாட்டிலிருந்து தகாத, புண்படுத்தும், தவறான அல்லது தவறாக வழிநடத்தும் AI வெளியீடுகள்.</string>
    <string name="settings_report_ai_confirm">புகார் உருவாக்கு</string>
    <string name="settings_report_ai_cancel">ரத்து</string>
    <string name="settings_report_ai_no_email">மின்னஞ்சல் பயன்பாடு கிடைக்கவில்லை. தயவுசெய்து புகாரை dev.app.support@gmail.com-க்கு அனுப்பவும்.</string>
    <string name="settings_report_ai_subject">Best Journal: தகாத AI பதில்</string>
    <string name="settings_report_ai_body">வணக்கம்,\n\nBest Journal-ல் ஒரு தகாத அல்லது தவறான AI பதிலைப் புகாரளிக்க விரும்புகிறேன்.\n\nபிரச்சனையின் விளக்கம்:\n[தயவுசெய்து நிரப்பவும்]\n\nசூழல் (எந்த அம்சம், எந்த உள்ளீடு):\n[தயவுசெய்து நிரப்பவும்]\n\nநன்றி.</string>

    <string name="settings_revoke_title">ஒப்பந்த ரத்து</string>
    <string name="settings_revoke_subtitle">Premium வாங்குதல்</string>
    <string name="settings_revoke_confirm_title">Support-க்கு மின்னஞ்சலைத் திறக்கவா?</string>
    <string name="settings_revoke_confirm_body">உங்கள் மின்னஞ்சல் பயன்பாடு dev.app.support@gmail.com-க்கு தயார் செய்யப்பட்ட செய்தியுடன் திறக்கும். வேலை நாட்களில் 24 மணி நேரத்திற்குள் பதிலளிப்போம்.\n\nரத்து உரிமை பற்றிய முழுமையான தகவல் பயன்பாட்டு விதிமுறைகளில் (§ 16) உள்ளது. சந்தாக்களுக்கு Google Play → Subscriptions மூலமாகவும் ரத்து செய்யுங்கள்.</string>
    <string name="settings_revoke_cancel">ரத்து</string>
    <string name="settings_revoke_confirm">ரத்து உருவாக்கு</string>
    <string name="settings_revoke_no_email">மின்னஞ்சல் பயன்பாடு கிடைக்கவில்லை. தயவுசெய்து ரத்தை dev.app.support@gmail.com-க்கு அனுப்பவும்.</string>
    <string name="settings_revoke_email_subject">Best Journal Premium ஒப்பந்த ரத்து</string>
    <string name="settings_revoke_email_body">இதன் மூலம் Best Journal-ன் Premium அம்சங்கள் தொடர்பான ஒப்பந்தத்தை நான் திரும்பப் பெறுவதை அறிவிக்கிறேன்.\n\nஅனுப்புநர் (Google கணக்கு): %1$s\nதிரும்பப் பெறும் நேரம்: %2$s\n\nஇந்த திரும்பப் பெறல், பயன்பாட்டின் § 356a BGB-க்கு உடன்பட்ட இரு-அடுக்கு திரும்பப் பெறும் பொத்தானின் மூலம் தொடங்கப்பட்டு Gmail API வழியாக தானாக அனுப்பப்பட்டது.</string>
    <string name="settings_revoke_confirm_subject">உங்கள் பெறல் உறுதி: Best Journal இல் திரும்பப் பெறல்</string>
    <string name="settings_revoke_confirm_user_body">வணக்கம்,\n\n%1$s அன்று செய்யப்பட்ட உங்கள் திரும்பப் பெறலை நாங்கள் பெற்றுள்ளோம். இது § 356a BGB படி உங்கள் பெறல் உறுதிப்படுத்தல்.\n\nஉங்கள் திரும்பப் பெறலை மிக விரைவாக செயல்படுத்துவோம்; கேள்விகள் இருந்தால் dev.app.support@gmail.com மூலம் உங்களைத் தொடர்புகொள்வோம்.\n\nமேலும் கட்டணங்கள் வராமல் இருக்க, Google Play Store இல் "Subscriptions" கீழும் உங்கள் சந்தாவை ரத்து செய்யவும்.\n\nநன்றி மற்றும் அன்புடன்\nBest Journal (Frank Barwandt)</string>
    <string name="settings_revoke_no_account">உள்நுழைந்த Google கணக்கு முகவரி எதுவும் கிடைக்கவில்லை. தயவுசெய்து Settings இல் உங்கள் Google கணக்கில் உள்நுழையவும் அல்லது திரும்பப் பெறலை dev.app.support@gmail.com க்கு கையால் அனுப்பவும்.</string>
    <string name="settings_revoke_sending">திரும்பப் பெறல் அனுப்பப்படுகிறது…</string>
    <string name="settings_revoke_success_title">திரும்பப் பெறல் பெறப்பட்டது</string>
    <string name="settings_revoke_success_body">உங்கள் திரும்பப் பெறல் dev.app.support@gmail.com க்கு அனுப்பப்பட்டது. பெறல் உறுதிப்படுத்தலும் உங்கள் இன்பாக்ஸில் உள்ளது.</string>
    <string name="settings_revoke_success_close">மூடு</string>
    <string name="settings_revoke_error_title">திரும்பப் பெறலை அனுப்ப முடியவில்லை</string>
    <string name="settings_revoke_error_body">தானியங்கி அனுப்பல் தோல்வியடைந்தது: %1$s\n\nமாற்றாக நீங்கள் dev.app.support@gmail.com க்கு கையால் ஒரு மின்னஞ்சலை அனுப்பலாம். அதற்கு "மின்னஞ்சல் பயன்பாட்டைத் திற" என்பதைத் தட்டவும்.</string>
    <string name="settings_revoke_error_email_fallback">மின்னஞ்சல் பயன்பாட்டைத் திற</string>
"""

# ═══════════ URDU (ur) — آپ, Arabic script, RTL, Urdu-Persian vocabulary ═══════════
TRANSLATIONS["ur"] = r"""
    <!-- ════════════════════════════════════════════════════════════════════
         CONSENT SCREEN + PRIVACY GATES + PRIVACY SECTION (April 2026)
         ════════════════════════════════════════════════════════════════════ -->
    <string name="consent_title">رازداری اور رضامندی</string>
    <string name="consent_intro">آپ کی ڈائری ایک ذاتی جگہ ہے، اور ہم اس کا احترام کرتے ہیں۔ یہاں آپ شفاف طریقے سے دیکھ سکتے ہیں کہ Best Journal آپ کے ڈیٹا کے ساتھ کیسے پیش آتا ہے۔</string>
    <string name="consent_card1_title">مقامی ذخیرہ</string>
    <string name="consent_card1_body">آپ کی اندراجات آپ کے آلے پر رہتی ہیں۔</string>
    <string name="consent_card2_title">AI خصوصیات (امریکہ)</string>
    <string name="consent_card2_body">اختیاری طور پر متن Google Gemini، آواز کی ریکارڈنگز Groq اور پڑھ کر سنانے کا متن Microsoft Edge کو امریکہ میں بھیجا جاتا ہے (EU-US Data Privacy Framework + Standard Contractual Clauses)۔</string>
    <string name="consent_card3_title">گمنام شماریات</string>
    <string name="consent_card3_body">Firebase Analytics، اختیاری، ترتیبات میں کسی بھی وقت تبدیل کر سکتے ہیں۔</string>
    <string name="consent_links_header">ہماری قانونی دستاویزات:</string>
    <string name="consent_accept_all">اتفاق اور شروع کریں</string>
    <string name="consent_disable_stats">شماریات بند کر کے جاری رکھیں</string>
    <string name="consent_confirmation">\"اتفاق اور شروع کریں\" پر ٹیپ کرنے سے آپ تصدیق کرتے ہیں کہ آپ نے رازداری پالیسی، استعمال کی شرائط اور قانونی نوٹس پڑھ لیا ہے اور بیان کردہ ڈیٹا پروسیسنگ پر رضامند ہیں۔ آپ اپنا فیصلہ ترتیبات میں کسی بھی وقت تبدیل کر سکتے ہیں۔</string>

    <string name="privacy_gate_groq_title">آواز کی ریکارڈنگ Groq کو بھیجیں؟</string>
    <string name="privacy_gate_groq_body">کلاؤڈ ٹرانسکرپشن کے لیے آپ کی آواز کی ریکارڈنگ خفیہ کر کے Groq, Inc. (ماؤنٹین ویو، امریکہ) کو بھیجی جاتی ہے اور وہاں متن میں تبدیل کی جاتی ہے۔ آڈیو فائل پروسیسنگ کے بعد حذف کر دی جاتی ہے اور تربیت کے لیے استعمال نہیں ہوتی۔\n\nمتبادل: آلے پر مقامی ٹرانسکرپشن استعمال کریں (آف لائن، ڈیٹا منتقل نہیں ہوتا)، Settings → AI میں تبدیل کیا جا سکتا ہے۔</string>
    <string name="privacy_gate_groq_accept">اتفاق اور بھیجیں</string>
    <string name="privacy_gate_groq_local">اس کے بجائے مقامی طور پر ٹرانسکرائب کریں</string>

    <string name="privacy_gate_gemini_title">متن Google Gemini کو بھیجیں؟</string>
    <string name="privacy_gate_gemini_body">AI خصوصیات (ڈیش بورڈ، خلاصے، جائزہ، متن کی بہتری) کے لیے آپ کی اندراجات کے اقتباسات خفیہ کر کے Google Gemini (Firebase AI، امریکہ) کو بھیجے جاتے ہیں۔ قانونی بنیاد: EU-US Data Privacy Framework + Standard Contractual Clauses۔ درخواستیں پروسیسنگ کے بعد حذف کر دی جاتی ہیں اور تربیت کے لیے استعمال نہیں ہوتیں۔</string>
    <string name="privacy_gate_gemini_accept">اتفاق اور بھیجیں</string>
    <string name="privacy_gate_gemini_cancel">منسوخ</string>

    <string name="privacy_gate_tts_title">متن Microsoft کو بھیجیں؟</string>
    <string name="privacy_gate_tts_body">پڑھ کر سنانے کے لیے متن خفیہ کر کے Microsoft Bing Speech (امریکہ) کو بھیجا جاتا ہے اور آڈیو کے طور پر واپس آتا ہے۔ قانونی بنیاد: EU-US Data Privacy Framework + Standard Contractual Clauses۔\n\nمتبادل: Android کی بلٹ-ان آف لائن TTS استعمال کریں۔</string>
    <string name="privacy_gate_tts_accept">اتفاق اور پڑھیں</string>
    <string name="privacy_gate_tts_cancel">منسوخ</string>

    <string name="settings_privacy_header">رازداری</string>
    <string name="settings_analytics_title">گمنام شماریات</string>
    <string name="settings_analytics_subtitle">نقص کے تجزیہ اور پروڈکٹ کی بہتری کے لیے Firebase Analytics</string>

    <string name="settings_delete_account_title">اکاؤنٹ اور ڈیٹا حذف کریں</string>
    <string name="settings_delete_account_subtitle">تمام مقامی ڈیٹا، Google اکاؤنٹ اور Drive بیک اپ کو مستقل طور پر حذف کرتا ہے</string>
    <string name="settings_delete_account_confirm_title">اکاؤنٹ مستقل طور پر حذف کریں؟</string>
    <string name="settings_delete_account_confirm_body">یہ عمل واپس نہیں کیا جا سکتا اور درج ذیل کو حذف کرتا ہے:\n\n• تمام مقامی ڈائری اندراجات، تصاویر اور ویڈیوز\n• آپ کا Firebase اکاؤنٹ\n• ایپ کا Google Drive بیک اپ\n\nاس کے بعد ایپ نئے انسٹالیشن کے طور پر دوبارہ شروع ہو گی۔</string>
    <string name="settings_delete_account_cancel">منسوخ</string>
    <string name="settings_delete_account_confirm">ہاں، سب کچھ حذف کریں</string>

    <string name="settings_report_ai_title">AI جواب کی رپورٹ کریں</string>
    <string name="settings_report_ai_subtitle">نامناسب یا غلط AI آؤٹ پٹ</string>
    <string name="settings_report_ai_confirm_title">سپورٹ کو ای میل کھولیں؟</string>
    <string name="settings_report_ai_confirm_body">آپ کی ای میل ایپ dev.app.support@gmail.com کے لیے تیار پیغام کے ساتھ کھلے گی۔ بھیجنے سے پہلے تفصیل شامل کر سکتے ہیں۔ ہم کام کے دنوں میں 24 گھنٹے کے اندر جواب دیتے ہیں۔\n\nبراہ کرم یہاں رپورٹ کریں: ڈیش بورڈ، خلاصے، جائزہ یا متن کی بہتری سے نامناسب، توہین آمیز، غلط یا گمراہ کن AI آؤٹ پٹ۔</string>
    <string name="settings_report_ai_confirm">رپورٹ بنائیں</string>
    <string name="settings_report_ai_cancel">منسوخ</string>
    <string name="settings_report_ai_no_email">ای میل ایپ نہیں ملی۔ براہ کرم رپورٹ dev.app.support@gmail.com کو بھیجیں۔</string>
    <string name="settings_report_ai_subject">Best Journal: نامناسب AI جواب</string>
    <string name="settings_report_ai_body">سلام،\n\nمیں Best Journal میں ایک نامناسب یا غلط AI جواب کی رپورٹ کرنا چاہتا ہوں۔\n\nمسئلے کی تفصیل:\n[براہ کرم بھریں]\n\nسیاق و سباق (کون سی خصوصیت، کون سا ان پٹ):\n[براہ کرم بھریں]\n\nشکریہ۔</string>

    <string name="settings_revoke_title">معاہدے کی منسوخی</string>
    <string name="settings_revoke_subtitle">Premium خریداری</string>
    <string name="settings_revoke_confirm_title">سپورٹ کو ای میل کھولیں؟</string>
    <string name="settings_revoke_confirm_body">آپ کی ای میل ایپ dev.app.support@gmail.com کے لیے تیار پیغام کے ساتھ کھلے گی۔ ہم کام کے دنوں میں 24 گھنٹے کے اندر جواب دیتے ہیں۔\n\nمنسوخی کے حق پر مکمل معلومات استعمال کی شرائط (§ 16) میں موجود ہیں۔ سبسکرپشنز کے لیے Google Play → Subscriptions سے بھی منسوخ کریں۔</string>
    <string name="settings_revoke_cancel">منسوخ</string>
    <string name="settings_revoke_confirm">منسوخی بنائیں</string>
    <string name="settings_revoke_no_email">ای میل ایپ نہیں ملی۔ براہ کرم منسوخی dev.app.support@gmail.com کو بھیجیں۔</string>
    <string name="settings_revoke_email_subject">Best Journal Premium معاہدے کی منسوخی</string>
    <string name="settings_revoke_email_body">اس کے ذریعے میں Best Journal کی Premium خصوصیات سے متعلق اپنے معاہدے سے دستبرداری کی اطلاع دیتا ہوں۔\n\nبھیجنے والا (Google اکاؤنٹ): %1$s\nدستبرداری کا وقت: %2$s\n\nیہ دستبرداری ایپ میں موجود § 356a BGB کے مطابق دو مرحلہ وار دستبرداری کے بٹن کے ذریعے شروع کی گئی اور Gmail API کے ذریعے خودکار طور پر بھیجی گئی۔</string>
    <string name="settings_revoke_confirm_subject">آپ کی وصولی کی تصدیق: Best Journal میں دستبرداری</string>
    <string name="settings_revoke_confirm_user_body">سلام،\n\nہمیں %1$s کی آپ کی دستبرداری موصول ہو گئی ہے۔ یہ § 356a BGB کے مطابق آپ کی وصولی کی تصدیق ہے۔\n\nہم آپ کی دستبرداری کو جلد از جلد پراسیس کریں گے، اور اگر کوئی سوال ہوا تو dev.app.support@gmail.com کے ذریعے آپ سے رابطہ کریں گے۔\n\nمزید بلنگ روکنے کے لیے، براہ کرم Google Play Store میں "Subscriptions" کے تحت اپنی سبسکرپشن بھی منسوخ کریں۔\n\nشکریہ اور نیک تمنائیں\nBest Journal (Frank Barwandt)</string>
    <string name="settings_revoke_no_account">سائن اِن شدہ Google اکاؤنٹ کا پتہ نہیں ملا۔ براہ کرم Settings میں اپنے Google اکاؤنٹ سے سائن اِن کریں یا دستبرداری کو dev.app.support@gmail.com پر دستی طور پر بھیجیں۔</string>
    <string name="settings_revoke_sending">دستبرداری بھیجی جا رہی ہے…</string>
    <string name="settings_revoke_success_title">دستبرداری موصول ہو گئی</string>
    <string name="settings_revoke_success_body">آپ کی دستبرداری dev.app.support@gmail.com پر بھیج دی گئی ہے۔ وصولی کی تصدیق بھی آپ کے اِن باکس میں موجود ہے۔</string>
    <string name="settings_revoke_success_close">بند کریں</string>
    <string name="settings_revoke_error_title">دستبرداری نہیں بھیجی جا سکی</string>
    <string name="settings_revoke_error_body">خودکار ارسال ناکام رہا: %1$s\n\nمتبادل کے طور پر آپ dev.app.support@gmail.com پر دستی ای میل بھیج سکتے ہیں۔ اس کے لیے "ای میل ایپ کھولیں" پر ٹیپ کریں۔</string>
    <string name="settings_revoke_error_email_fallback">ای میل ایپ کھولیں</string>
"""

# ═══════════ GUJARATI (gu) — તમે, Gujarati script, Arabic numerals ═══════════
TRANSLATIONS["gu"] = r"""
    <!-- ════════════════════════════════════════════════════════════════════
         CONSENT SCREEN + PRIVACY GATES + PRIVACY SECTION (April 2026)
         ════════════════════════════════════════════════════════════════════ -->
    <string name="consent_title">ગોપનીયતા અને સંમતિ</string>
    <string name="consent_intro">તમારી ડાયરી એ એક ખાનગી જગ્યા છે અને અમે તેનો આદર કરીએ છીએ. અહીં તમે પારદર્શક રીતે જોઈ શકો છો કે Best Journal તમારા ડેટાનું કેવી રીતે સંચાલન કરે છે.</string>
    <string name="consent_card1_title">સ્થાનિક સ્ટોરેજ</string>
    <string name="consent_card1_body">તમારી એન્ટ્રીઓ તમારા ઉપકરણ પર જ રહે છે.</string>
    <string name="consent_card2_title">AI ફીચર્સ (USA)</string>
    <string name="consent_card2_body">વૈકલ્પિક રીતે, ટેક્સ્ટ Google Gemini ને, વૉઇસ રેકોર્ડિંગ Groq ને અને વાંચીને સંભળાવવાનો ટેક્સ્ટ Microsoft Edge ને USA માં મોકલવામાં આવે છે (EU-US Data Privacy Framework + Standard Contractual Clauses).</string>
    <string name="consent_card3_title">અજ્ઞાત આંકડા</string>
    <string name="consent_card3_body">Firebase Analytics, વૈકલ્પિક, સેટિંગ્સમાં ગમે ત્યારે બદલી શકાય છે.</string>
    <string name="consent_links_header">અમારા કાયદેસર દસ્તાવેજો:</string>
    <string name="consent_accept_all">સંમત અને શરૂ કરો</string>
    <string name="consent_disable_stats">આંકડા બંધ કરીને ચાલુ રાખો</string>
    <string name="consent_confirmation">\"સંમત અને શરૂ કરો\" પર ટેપ કરીને તમે પુષ્ટિ કરો છો કે તમે ગોપનીયતા નીતિ, ઉપયોગની શરતો અને કાનૂની સૂચના વાંચી છે અને વર્ણવેલ ડેટા પ્રોસેસિંગ સાથે સંમત છો. તમે તમારો નિર્ણય સેટિંગ્સમાં ગમે ત્યારે બદલી શકો છો.</string>

    <string name="privacy_gate_groq_title">વૉઇસ રેકોર્ડિંગ Groq ને મોકલવું?</string>
    <string name="privacy_gate_groq_body">ક્લાઉડ ટ્રાન્સક્રિપ્શન માટે તમારું વૉઇસ રેકોર્ડિંગ એન્ક્રિપ્ટેડ સ્વરૂપમાં Groq, Inc. (Mountain View, USA) ને મોકલવામાં આવે છે અને ત્યાં ટેક્સ્ટમાં રૂપાંતરિત થાય છે. ઑડિઓ ફાઇલ પ્રક્રિયા પછી કાઢી નાખવામાં આવે છે અને તાલીમ માટે ઉપયોગમાં લેવાતી નથી.\n\nવિકલ્પ: ઉપકરણ પર સ્થાનિક ટ્રાન્સક્રિપ્શનનો ઉપયોગ કરો (ઑફલાઇન, કોઈ ડેટા ટ્રાન્સફર નહીં), Settings → AI માં બદલી શકાય છે.</string>
    <string name="privacy_gate_groq_accept">સંમત અને મોકલો</string>
    <string name="privacy_gate_groq_local">તેના બદલે સ્થાનિક રીતે ટ્રાન્સક્રાઇબ કરો</string>

    <string name="privacy_gate_gemini_title">ટેક્સ્ટ Google Gemini ને મોકલવો?</string>
    <string name="privacy_gate_gemini_body">AI ફીચર્સ (ડેશબોર્ડ, સારાંશ, રેટ્રોસ્પેક્ટિવ, ટેક્સ્ટ સુધારો) માટે તમારી એન્ટ્રીઓના અંશો એન્ક્રિપ્ટેડ સ્વરૂપમાં Google Gemini (Firebase AI, USA) ને મોકલવામાં આવે છે. કાનૂની આધાર: EU-US Data Privacy Framework + Standard Contractual Clauses. વિનંતીઓ પ્રક્રિયા પછી કાઢી નાખવામાં આવે છે અને તાલીમ માટે ઉપયોગમાં લેવાતી નથી.</string>
    <string name="privacy_gate_gemini_accept">સંમત અને મોકલો</string>
    <string name="privacy_gate_gemini_cancel">રદ કરો</string>

    <string name="privacy_gate_tts_title">ટેક્સ્ટ Microsoft ને મોકલવો?</string>
    <string name="privacy_gate_tts_body">વાંચીને સંભળાવવા માટે ટેક્સ્ટ એન્ક્રિપ્ટેડ સ્વરૂપમાં Microsoft Bing Speech (USA) ને મોકલવામાં આવે છે અને ઑડિઓ તરીકે પાછો આવે છે. કાનૂની આધાર: EU-US Data Privacy Framework + Standard Contractual Clauses.\n\nવિકલ્પ: Android નું બિલ્ટ-ઇન ઑફલાઇન TTS વાપરો.</string>
    <string name="privacy_gate_tts_accept">સંમત અને વાંચો</string>
    <string name="privacy_gate_tts_cancel">રદ કરો</string>

    <string name="settings_privacy_header">ગોપનીયતા</string>
    <string name="settings_analytics_title">અજ્ઞાત આંકડા</string>
    <string name="settings_analytics_subtitle">ભૂલ વિશ્લેષણ અને ઉત્પાદન સુધારણા માટે Firebase Analytics</string>

    <string name="settings_delete_account_title">એકાઉન્ટ અને ડેટા કાઢી નાખો</string>
    <string name="settings_delete_account_subtitle">બધા સ્થાનિક ડેટા, Google એકાઉન્ટ અને Drive બેકઅપને કાયમી રૂપે કાઢી નાખે છે</string>
    <string name="settings_delete_account_confirm_title">એકાઉન્ટ કાયમી રીતે કાઢવું?</string>
    <string name="settings_delete_account_confirm_body">આ ક્રિયા પાછી મેળવી શકાતી નથી અને આ કાઢી નાખે છે:\n\n• બધી સ્થાનિક ડાયરી એન્ટ્રીઓ, ફોટા અને વીડિયો\n• તમારું Firebase એકાઉન્ટ\n• ઍપનો Google Drive બેકઅપ\n\nઍપ નવા ઇન્સ્ટોલેશન તરીકે ફરીથી શરૂ થશે.</string>
    <string name="settings_delete_account_cancel">રદ કરો</string>
    <string name="settings_delete_account_confirm">હા, બધું કાઢો</string>

    <string name="settings_report_ai_title">AI જવાબની જાણ કરો</string>
    <string name="settings_report_ai_subtitle">અયોગ્ય અથવા ખોટું AI આઉટપુટ</string>
    <string name="settings_report_ai_confirm_title">Support ને ઇમેઇલ ખોલવો?</string>
    <string name="settings_report_ai_confirm_body">તમારી ઇમેઇલ ઍપ dev.app.support@gmail.com ને તૈયાર સંદેશ સાથે ખુલશે. મોકલતા પહેલા વર્ણન ઉમેરી શકો છો. અમે કાર્ય દિવસોમાં 24 કલાકમાં જવાબ આપીએ છીએ.\n\nકૃપા કરીને અહીં જાણ કરો: ડેશબોર્ડ, સારાંશ, રેટ્રોસ્પેક્ટિવ અથવા ટેક્સ્ટ સુધારામાંથી અયોગ્ય, આક્રમક, ખોટા અથવા ગેરમાર્ગે દોરતા AI આઉટપુટ.</string>
    <string name="settings_report_ai_confirm">જાણ બનાવો</string>
    <string name="settings_report_ai_cancel">રદ કરો</string>
    <string name="settings_report_ai_no_email">કોઈ ઇમેઇલ ઍપ મળી નથી. કૃપા કરીને જાણ dev.app.support@gmail.com પર મોકલો.</string>
    <string name="settings_report_ai_subject">Best Journal: અયોગ્ય AI જવાબ</string>
    <string name="settings_report_ai_body">નમસ્તે,\n\nહું Best Journal માં એક અયોગ્ય અથવા ખોટા AI જવાબની જાણ કરવા માંગું છું.\n\nસમસ્યાનું વર્ણન:\n[કૃપા કરીને ભરો]\n\nસંદર્ભ (કઈ સુવિધા, કયું ઇનપુટ):\n[કૃપા કરીને ભરો]\n\nઆભાર.</string>

    <string name="settings_revoke_title">કરાર રદ કરો</string>
    <string name="settings_revoke_subtitle">Premium ખરીદી</string>
    <string name="settings_revoke_confirm_title">Support ને ઇમેઇલ ખોલવો?</string>
    <string name="settings_revoke_confirm_body">તમારી ઇમેઇલ ઍપ dev.app.support@gmail.com ને તૈયાર સંદેશ સાથે ખુલશે. અમે કાર્ય દિવસોમાં 24 કલાકમાં જવાબ આપીએ છીએ.\n\nરદ કરવાના અધિકારની સંપૂર્ણ માહિતી ઉપયોગની શરતોમાં (§ 16) છે. સબ્સ્ક્રિપ્શન માટે વધુમાં Google Play → Subscriptions માંથી પણ રદ કરો.</string>
    <string name="settings_revoke_cancel">રદ કરો</string>
    <string name="settings_revoke_confirm">રદ બનાવો</string>
    <string name="settings_revoke_no_email">કોઈ ઇમેઇલ ઍપ મળી નથી. કૃપા કરીને રદ dev.app.support@gmail.com પર મોકલો.</string>
    <string name="settings_revoke_email_subject">Best Journal Premium કરાર રદ</string>
    <string name="settings_revoke_email_body">આ દ્વારા હું Best Journal ની Premium સુવિધાઓ સંબંધિત મારા કરારના રદ કરવાની સૂચના આપું છું.\n\nમોકલનાર (Google એકાઉન્ટ): %1$s\nરદ કરવાની ક્ષણ: %2$s\n\nઆ રદ પ્રક્રિયા એપમાં આવેલા § 356a BGB-અનુરૂપ બે-પગલાના રદ બટન દ્વારા શરૂ કરવામાં આવી હતી અને Gmail API મારફતે આપમેળે મોકલવામાં આવી હતી.</string>
    <string name="settings_revoke_confirm_subject">તમારી પ્રાપ્તી પુષ્ટિ: Best Journal માં રદ</string>
    <string name="settings_revoke_confirm_user_body">નમસ્તે,\n\n%1$sની તમારી રદ કરવાની જાણ અમને મળી ગઈ છે. આ § 356a BGB મુજબ તમારી પ્રાપ્તી પુષ્ટિ છે.\n\nઅમે તમારી રદ પ્રક્રિયા શક્ય તેટલી વહેલી તકે કરીશું અને કોઈ પ્રશ્ન હોય તો dev.app.support@gmail.com દ્વારા તમારો સંપર્ક કરીશું.\n\nઆગળ કોઈ નવી બિલિંગ ન થાય તે માટે, કૃપા કરીને Google Play Store માં "Subscriptions" હેઠળ તમારી સબ્સ્ક્રિપ્શન પણ રદ કરો.\n\nઆભાર અને શુભેચ્છાઓ\nBest Journal (Frank Barwandt)</string>
    <string name="settings_revoke_no_account">સાઇન-ઇન થયેલ Google ખાતાનું સરનામું મળ્યું નથી. કૃપા કરીને Settings માં તમારા Google ખાતાથી સાઇન ઇન કરો અથવા રદ કરવાની જાણ dev.app.support@gmail.com પર હાથેથી મોકલો.</string>
    <string name="settings_revoke_sending">રદ મોકલાઈ રહ્યું છે…</string>
    <string name="settings_revoke_success_title">રદ પ્રાપ્ત થયું</string>
    <string name="settings_revoke_success_body">તમારી રદ કરવાની જાણ dev.app.support@gmail.com પર મોકલવામાં આવી છે. પ્રાપ્તી પુષ્ટિ પણ તમારા ઇનબોક્સમાં છે.</string>
    <string name="settings_revoke_success_close">બંધ કરો</string>
    <string name="settings_revoke_error_title">રદ મોકલી શકાયું નથી</string>
    <string name="settings_revoke_error_body">સ્વચાલિત મોકલાણ નિષ્ફળ ગયું: %1$s\n\nવૈકલ્પિક રીતે તમે dev.app.support@gmail.com પર હાથેથી ઇમેઇલ મોકલી શકો છો. તેના માટે "ઇમેઇલ એપ ખોલો" પર ટેપ કરો.</string>
    <string name="settings_revoke_error_email_fallback">ઇમેઇલ એપ ખોલો</string>
"""

# ═══════════ KANNADA (kn) — ನೀವು, Kannada script ═══════════
TRANSLATIONS["kn"] = r"""
    <!-- ════════════════════════════════════════════════════════════════════
         CONSENT SCREEN + PRIVACY GATES + PRIVACY SECTION (April 2026)
         ════════════════════════════════════════════════════════════════════ -->
    <string name="consent_title">ಗೌಪ್ಯತೆ ಮತ್ತು ಸಮ್ಮತಿ</string>
    <string name="consent_intro">ನಿಮ್ಮ ಡೈರಿ ವೈಯಕ್ತಿಕ ಜಾಗವಾಗಿದೆ ಮತ್ತು ಅದನ್ನು ನಾವು ಗೌರವಿಸುತ್ತೇವೆ. Best Journal ನಿಮ್ಮ ಡೇಟಾವನ್ನು ಹೇಗೆ ನಿರ್ವಹಿಸುತ್ತದೆ ಎಂಬುದನ್ನು ಇಲ್ಲಿ ಪಾರದರ್ಶಕವಾಗಿ ನೋಡಬಹುದು.</string>
    <string name="consent_card1_title">ಸ್ಥಳೀಯ ಸಂಗ್ರಹ</string>
    <string name="consent_card1_body">ನಿಮ್ಮ ಪ್ರವೇಶಗಳು ನಿಮ್ಮ ಸಾಧನದಲ್ಲೇ ಉಳಿಯುತ್ತವೆ.</string>
    <string name="consent_card2_title">AI ವೈಶಿಷ್ಟ್ಯಗಳು (USA)</string>
    <string name="consent_card2_body">ಐಚ್ಛಿಕವಾಗಿ, ಪಠ್ಯವನ್ನು Google Gemini ಗೆ, ಧ್ವನಿ ರೆಕಾರ್ಡಿಂಗ್‌ಗಳನ್ನು Groq ಗೆ ಮತ್ತು ಓದಿಸುವ ಪಠ್ಯವನ್ನು Microsoft Edge ಗೆ USA ನಲ್ಲಿ ಕಳುಹಿಸಲಾಗುತ್ತದೆ (EU-US Data Privacy Framework + Standard Contractual Clauses).</string>
    <string name="consent_card3_title">ಅನಾಮಧೇಯ ಅಂಕಿಅಂಶಗಳು</string>
    <string name="consent_card3_body">Firebase Analytics, ಐಚ್ಛಿಕ, ಸೆಟ್ಟಿಂಗ್‌ಗಳಲ್ಲಿ ಯಾವಾಗ ಬೇಕಾದರೂ ಬದಲಾಯಿಸಬಹುದು.</string>
    <string name="consent_links_header">ನಮ್ಮ ಕಾನೂನು ದಾಖಲೆಗಳು:</string>
    <string name="consent_accept_all">ಒಪ್ಪಿ ಪ್ರಾರಂಭಿಸಿ</string>
    <string name="consent_disable_stats">ಅಂಕಿಅಂಶಗಳನ್ನು ಆಫ್ ಮಾಡಿ ಮುಂದುವರಿಸಿ</string>
    <string name="consent_confirmation">\"ಒಪ್ಪಿ ಪ್ರಾರಂಭಿಸಿ\" ಅನ್ನು ಟ್ಯಾಪ್ ಮಾಡುವ ಮೂಲಕ ನೀವು ಗೌಪ್ಯತೆ ನೀತಿ, ಬಳಕೆ ನಿಯಮಗಳು ಮತ್ತು ಕಾನೂನು ಸೂಚನೆಯನ್ನು ಓದಿದ್ದೀರಿ ಮತ್ತು ವಿವರಿಸಿದ ಡೇಟಾ ಸಂಸ್ಕರಣೆಗೆ ಒಪ್ಪುತ್ತೀರಿ ಎಂದು ದೃಢೀಕರಿಸುತ್ತೀರಿ. ನಿಮ್ಮ ನಿರ್ಧಾರವನ್ನು ಸೆಟ್ಟಿಂಗ್‌ಗಳಲ್ಲಿ ಯಾವಾಗ ಬೇಕಾದರೂ ಬದಲಾಯಿಸಬಹುದು.</string>

    <string name="privacy_gate_groq_title">Groq ಗೆ ಧ್ವನಿ ರೆಕಾರ್ಡಿಂಗ್ ಕಳುಹಿಸುವುದೇ?</string>
    <string name="privacy_gate_groq_body">ಕ್ಲೌಡ್ ಪ್ರತಿಲೇಖನಕ್ಕಾಗಿ ನಿಮ್ಮ ಧ್ವನಿ ರೆಕಾರ್ಡಿಂಗ್ ಎನ್‌ಕ್ರಿಪ್ಟ್ ಆಗಿ Groq, Inc. (Mountain View, USA) ಗೆ ಕಳುಹಿಸಲಾಗುತ್ತದೆ ಮತ್ತು ಅಲ್ಲಿ ಪಠ್ಯವಾಗಿ ಪರಿವರ್ತಿಸಲಾಗುತ್ತದೆ. ಆಡಿಯೊ ಫೈಲ್ ಸಂಸ್ಕರಣೆಯ ನಂತರ ಅಳಿಸಲಾಗುತ್ತದೆ ಮತ್ತು ತರಬೇತಿಗಾಗಿ ಬಳಸಲಾಗುವುದಿಲ್ಲ.\n\nಪರ್ಯಾಯ: ಸಾಧನದಲ್ಲಿ ಸ್ಥಳೀಯ ಪ್ರತಿಲೇಖನ ಬಳಸಿ (ಆಫ್‌ಲೈನ್, ಡೇಟಾ ವರ್ಗಾವಣೆ ಇಲ್ಲ), Settings → AI ನಲ್ಲಿ ಬದಲಾಯಿಸಬಹುದು.</string>
    <string name="privacy_gate_groq_accept">ಒಪ್ಪಿ ಕಳುಹಿಸಿ</string>
    <string name="privacy_gate_groq_local">ಬದಲಿಗೆ ಸ್ಥಳೀಯವಾಗಿ ಪ್ರತಿಲೇಖಿಸಿ</string>

    <string name="privacy_gate_gemini_title">Google Gemini ಗೆ ಪಠ್ಯ ಕಳುಹಿಸುವುದೇ?</string>
    <string name="privacy_gate_gemini_body">AI ವೈಶಿಷ್ಟ್ಯಗಳಿಗಾಗಿ (ಡ್ಯಾಶ್‌ಬೋರ್ಡ್, ಸಾರಾಂಶಗಳು, ಹಿಂತಿರುಗಿ ನೋಡು, ಪಠ್ಯ ಸುಧಾರಣೆ) ನಿಮ್ಮ ಪ್ರವೇಶಗಳ ಭಾಗಗಳು ಎನ್‌ಕ್ರಿಪ್ಟ್ ಆಗಿ Google Gemini (Firebase AI, USA) ಗೆ ಕಳುಹಿಸಲಾಗುತ್ತದೆ. ಕಾನೂನು ಆಧಾರ: EU-US Data Privacy Framework + Standard Contractual Clauses. ವಿನಂತಿಗಳು ಸಂಸ್ಕರಣೆಯ ನಂತರ ಅಳಿಸಲಾಗುತ್ತವೆ ಮತ್ತು ತರಬೇತಿಗಾಗಿ ಬಳಸಲಾಗುವುದಿಲ್ಲ.</string>
    <string name="privacy_gate_gemini_accept">ಒಪ್ಪಿ ಕಳುಹಿಸಿ</string>
    <string name="privacy_gate_gemini_cancel">ರದ್ದುಮಾಡಿ</string>

    <string name="privacy_gate_tts_title">Microsoft ಗೆ ಪಠ್ಯ ಕಳುಹಿಸುವುದೇ?</string>
    <string name="privacy_gate_tts_body">ಓದಿಸಲು ಪಠ್ಯವನ್ನು ಎನ್‌ಕ್ರಿಪ್ಟ್ ಆಗಿ Microsoft Bing Speech (USA) ಗೆ ಕಳುಹಿಸಲಾಗುತ್ತದೆ ಮತ್ತು ಆಡಿಯೊ ಆಗಿ ಹಿಂತಿರುಗುತ್ತದೆ. ಕಾನೂನು ಆಧಾರ: EU-US Data Privacy Framework + Standard Contractual Clauses.\n\nಪರ್ಯಾಯ: Android ನ ಅಂತರ್ನಿರ್ಮಿತ ಆಫ್‌ಲೈನ್ TTS ಬಳಸಿ.</string>
    <string name="privacy_gate_tts_accept">ಒಪ್ಪಿ ಓದಿರಿ</string>
    <string name="privacy_gate_tts_cancel">ರದ್ದುಮಾಡಿ</string>

    <string name="settings_privacy_header">ಗೌಪ್ಯತೆ</string>
    <string name="settings_analytics_title">ಅನಾಮಧೇಯ ಅಂಕಿಅಂಶಗಳು</string>
    <string name="settings_analytics_subtitle">ದೋಷ ವಿಶ್ಲೇಷಣೆ ಮತ್ತು ಉತ್ಪನ್ನ ಸುಧಾರಣೆಗಾಗಿ Firebase Analytics</string>

    <string name="settings_delete_account_title">ಖಾತೆ ಮತ್ತು ಡೇಟಾ ಅಳಿಸಿ</string>
    <string name="settings_delete_account_subtitle">ಎಲ್ಲಾ ಸ್ಥಳೀಯ ಡೇಟಾ, Google ಖಾತೆ ಮತ್ತು Drive ಬ್ಯಾಕಪ್ ಅನ್ನು ಶಾಶ್ವತವಾಗಿ ಅಳಿಸುತ್ತದೆ</string>
    <string name="settings_delete_account_confirm_title">ಖಾತೆಯನ್ನು ಶಾಶ್ವತವಾಗಿ ಅಳಿಸುವುದೇ?</string>
    <string name="settings_delete_account_confirm_body">ಈ ಕ್ರಿಯೆಯನ್ನು ಹಿಂಪಡೆಯಲು ಸಾಧ್ಯವಿಲ್ಲ ಮತ್ತು ಇದನ್ನು ಅಳಿಸುತ್ತದೆ:\n\n• ಎಲ್ಲಾ ಸ್ಥಳೀಯ ಡೈರಿ ಪ್ರವೇಶಗಳು, ಫೋಟೋಗಳು ಮತ್ತು ವೀಡಿಯೊಗಳು\n• ನಿಮ್ಮ Firebase ಖಾತೆ\n• ಆ್ಯಪ್‌ನ Google Drive ಬ್ಯಾಕಪ್\n\nಆ್ಯಪ್ ಹೊಸ ಸ್ಥಾಪನೆಯಂತೆ ಮತ್ತೆ ಪ್ರಾರಂಭವಾಗುತ್ತದೆ.</string>
    <string name="settings_delete_account_cancel">ರದ್ದುಮಾಡಿ</string>
    <string name="settings_delete_account_confirm">ಹೌದು, ಎಲ್ಲವನ್ನೂ ಅಳಿಸಿ</string>

    <string name="settings_report_ai_title">AI ಪ್ರತಿಕ್ರಿಯೆಯನ್ನು ವರದಿ ಮಾಡಿ</string>
    <string name="settings_report_ai_subtitle">ಅಸಮರ್ಪಕ ಅಥವಾ ತಪ್ಪಾದ AI ಔಟ್‌ಪುಟ್</string>
    <string name="settings_report_ai_confirm_title">Support ಗೆ ಇಮೇಲ್ ತೆರೆಯಿರಾ?</string>
    <string name="settings_report_ai_confirm_body">ನಿಮ್ಮ ಇಮೇಲ್ ಆ್ಯಪ್ dev.app.support@gmail.com ಗೆ ಸಿದ್ಧಪಡಿಸಿದ ಸಂದೇಶದೊಂದಿಗೆ ತೆರೆಯುತ್ತದೆ. ಕಳುಹಿಸುವ ಮೊದಲು ವಿವರಣೆಯನ್ನು ಸೇರಿಸಬಹುದು. ಕೆಲಸದ ದಿನಗಳಲ್ಲಿ 24 ಗಂಟೆಗಳಲ್ಲಿ ಉತ್ತರಿಸುತ್ತೇವೆ.\n\nಇಲ್ಲಿ ವರದಿ ಮಾಡಿ: ಡ್ಯಾಶ್‌ಬೋರ್ಡ್, ಸಾರಾಂಶಗಳು, ಹಿಂತಿರುಗಿ ನೋಡು ಅಥವಾ ಪಠ್ಯ ಸುಧಾರಣೆಯಿಂದ ಅಸಮರ್ಪಕ, ಆಕ್ಷೇಪಾರ್ಹ, ತಪ್ಪಾದ ಅಥವಾ ದಾರಿ ತಪ್ಪಿಸುವ AI ಔಟ್‌ಪುಟ್‌ಗಳು.</string>
    <string name="settings_report_ai_confirm">ವರದಿ ರಚಿಸಿ</string>
    <string name="settings_report_ai_cancel">ರದ್ದುಮಾಡಿ</string>
    <string name="settings_report_ai_no_email">ಇಮೇಲ್ ಆ್ಯಪ್ ಕಂಡುಬಂದಿಲ್ಲ. ದಯವಿಟ್ಟು ವರದಿಯನ್ನು dev.app.support@gmail.com ಗೆ ಕಳುಹಿಸಿ.</string>
    <string name="settings_report_ai_subject">Best Journal: ಅಸಮರ್ಪಕ AI ಪ್ರತಿಕ್ರಿಯೆ</string>
    <string name="settings_report_ai_body">ನಮಸ್ಕಾರ,\n\nನಾನು Best Journal ನಲ್ಲಿ ಅಸಮರ್ಪಕ ಅಥವಾ ತಪ್ಪಾದ AI ಪ್ರತಿಕ್ರಿಯೆಯನ್ನು ವರದಿ ಮಾಡಲು ಬಯಸುತ್ತೇನೆ.\n\nಸಮಸ್ಯೆಯ ವಿವರಣೆ:\n[ದಯವಿಟ್ಟು ಭರ್ತಿ ಮಾಡಿ]\n\nಸನ್ನಿವೇಶ (ಯಾವ ವೈಶಿಷ್ಟ್ಯ, ಯಾವ ಇನ್‌ಪುಟ್):\n[ದಯವಿಟ್ಟು ಭರ್ತಿ ಮಾಡಿ]\n\nಧನ್ಯವಾದಗಳು.</string>

    <string name="settings_revoke_title">ಒಪ್ಪಂದ ರದ್ದು</string>
    <string name="settings_revoke_subtitle">Premium ಖರೀದಿ</string>
    <string name="settings_revoke_confirm_title">Support ಗೆ ಇಮೇಲ್ ತೆರೆಯಿರಾ?</string>
    <string name="settings_revoke_confirm_body">ನಿಮ್ಮ ಇಮೇಲ್ ಆ್ಯಪ್ dev.app.support@gmail.com ಗೆ ಸಿದ್ಧಪಡಿಸಿದ ಸಂದೇಶದೊಂದಿಗೆ ತೆರೆಯುತ್ತದೆ. ಕೆಲಸದ ದಿನಗಳಲ್ಲಿ 24 ಗಂಟೆಗಳಲ್ಲಿ ಉತ್ತರಿಸುತ್ತೇವೆ.\n\nರದ್ದು ಹಕ್ಕಿನ ಪೂರ್ಣ ಮಾಹಿತಿ ಬಳಕೆ ನಿಯಮಗಳಲ್ಲಿ (§ 16) ಇದೆ. ಚಂದಾದಾರಿಕೆಗಳಿಗಾಗಿ Google Play → Subscriptions ನಿಂದ ಹೆಚ್ಚುವರಿಯಾಗಿ ರದ್ದುಗೊಳಿಸಿ.</string>
    <string name="settings_revoke_cancel">ರದ್ದುಮಾಡಿ</string>
    <string name="settings_revoke_confirm">ರದ್ದು ರಚಿಸಿ</string>
    <string name="settings_revoke_no_email">ಇಮೇಲ್ ಆ್ಯಪ್ ಕಂಡುಬಂದಿಲ್ಲ. ದಯವಿಟ್ಟು ರದ್ದನ್ನು dev.app.support@gmail.com ಗೆ ಕಳುಹಿಸಿ.</string>
    <string name="settings_revoke_email_subject">Best Journal Premium ಒಪ್ಪಂದ ರದ್ದು</string>
    <string name="settings_revoke_email_body">ಇದರ ಮೂಲಕ Best Journal ನ Premium ವೈಶಿಷ್ಟ್ಯಗಳಿಗೆ ಸಂಬಂಧಿಸಿದ ನನ್ನ ಒಪ್ಪಂದದಿಂದ ಹಿಂತೆಗೆದುಕೊಳ್ಳುವ ಕುರಿತು ತಿಳಿಸುತ್ತೇನೆ.\n\nಕಳುಹಿಸಿದವರು (Google ಖಾತೆ): %1$s\nಹಿಂತೆಗೆದುಕೊಳ್ಳುವ ಸಮಯ: %2$s\n\nಈ ಹಿಂತೆಗೆದುಕೊಳ್ಳುವಿಕೆ ಆ್ಯಪ್‌ನ § 356a BGB ಗೆ ಅನುಗುಣವಾದ ಎರಡು ಹಂತದ ಹಿಂತೆಗೆದುಕೊಳ್ಳುವ ಬಟನ್ ಮೂಲಕ ಪ್ರಾರಂಭಗೊಂಡು Gmail API ಮೂಲಕ ಸ್ವಯಂಚಾಲಿತವಾಗಿ ಕಳುಹಿಸಲಾಯಿತು.</string>
    <string name="settings_revoke_confirm_subject">ನಿಮ್ಮ ಸ್ವೀಕೃತಿ ದೃಢೀಕರಣ: Best Journal ನಲ್ಲಿ ಹಿಂತೆಗೆದುಕೊಳ್ಳುವಿಕೆ</string>
    <string name="settings_revoke_confirm_user_body">ನಮಸ್ಕಾರ,\n\n%1$sರ ನಿಮ್ಮ ಹಿಂತೆಗೆದುಕೊಳ್ಳುವಿಕೆ ನಮಗೆ ತಲುಪಿದೆ. ಇದು § 356a BGB ಪ್ರಕಾರ ನಿಮ್ಮ ಸ್ವೀಕೃತಿ ದೃಢೀಕರಣವಾಗಿದೆ.\n\nನಾವು ನಿಮ್ಮ ಹಿಂತೆಗೆದುಕೊಳ್ಳುವಿಕೆಯನ್ನು ಸಾಧ್ಯವಾದಷ್ಟು ಬೇಗ ಪ್ರಕ್ರಿಯೆಗೊಳಿಸುತ್ತೇವೆ ಮತ್ತು ಯಾವುದೇ ಪ್ರಶ್ನೆಗಳಿದ್ದರೆ dev.app.support@gmail.com ಮೂಲಕ ನಿಮ್ಮನ್ನು ಸಂಪರ್ಕಿಸುತ್ತೇವೆ.\n\nಮುಂದಿನ ಬಿಲ್ಲಿಂಗ್ ಆಗದಂತೆ, ದಯವಿಟ್ಟು Google Play Store ನಲ್ಲಿ "Subscriptions" ಅಡಿಯಲ್ಲಿ ನಿಮ್ಮ ಚಂದಾದಾರಿಕೆಯನ್ನು ಸಹ ರದ್ದುಮಾಡಿ.\n\nಧನ್ಯವಾದಗಳು ಮತ್ತು ಶುಭಾಶಯಗಳು\nBest Journal (Frank Barwandt)</string>
    <string name="settings_revoke_no_account">ಸೈನ್ ಇನ್ ಆಗಿರುವ Google ಖಾತೆಯ ವಿಳಾಸ ಕಂಡುಬಂದಿಲ್ಲ. ದಯವಿಟ್ಟು Settings ನಲ್ಲಿ ನಿಮ್ಮ Google ಖಾತೆಯಿಂದ ಸೈನ್ ಇನ್ ಮಾಡಿ ಅಥವಾ ಹಿಂತೆಗೆದುಕೊಳ್ಳುವಿಕೆಯನ್ನು dev.app.support@gmail.com ಗೆ ಕೈಯಾರೆ ಕಳುಹಿಸಿ.</string>
    <string name="settings_revoke_sending">ಹಿಂತೆಗೆದುಕೊಳ್ಳುವಿಕೆ ಕಳುಹಿಸಲಾಗುತ್ತಿದೆ…</string>
    <string name="settings_revoke_success_title">ಹಿಂತೆಗೆದುಕೊಳ್ಳುವಿಕೆ ಸ್ವೀಕರಿಸಲಾಗಿದೆ</string>
    <string name="settings_revoke_success_body">ನಿಮ್ಮ ಹಿಂತೆಗೆದುಕೊಳ್ಳುವಿಕೆಯನ್ನು dev.app.support@gmail.com ಗೆ ಕಳುಹಿಸಲಾಗಿದೆ. ಸ್ವೀಕೃತಿ ದೃಢೀಕರಣವೂ ನಿಮ್ಮ ಇನ್‌ಬಾಕ್ಸ್‌ನಲ್ಲಿ ಇದೆ.</string>
    <string name="settings_revoke_success_close">ಮುಚ್ಚಿ</string>
    <string name="settings_revoke_error_title">ಹಿಂತೆಗೆದುಕೊಳ್ಳುವಿಕೆಯನ್ನು ಕಳುಹಿಸಲಾಗಲಿಲ್ಲ</string>
    <string name="settings_revoke_error_body">ಸ್ವಯಂಚಾಲಿತ ಕಳುಹಿಸುವಿಕೆ ವಿಫಲವಾಯಿತು: %1$s\n\nಪರ್ಯಾಯವಾಗಿ ನೀವು dev.app.support@gmail.com ಗೆ ಕೈಯಾರೆ ಇಮೇಲ್ ಕಳುಹಿಸಬಹುದು. ಅದಕ್ಕಾಗಿ "ಇಮೇಲ್ ಆಪ್ ತೆರೆಯಿರಿ" ಅನ್ನು ತಟ್ಟಿರಿ.</string>
    <string name="settings_revoke_error_email_fallback">ಇಮೇಲ್ ಆಪ್ ತೆರೆಯಿರಿ</string>
"""

# ═══════════ MALAYALAM (ml) — നിങ്ങൾ, Malayalam script, simplified orthography ═══════════
TRANSLATIONS["ml"] = r"""
    <!-- ════════════════════════════════════════════════════════════════════
         CONSENT SCREEN + PRIVACY GATES + PRIVACY SECTION (April 2026)
         ════════════════════════════════════════════════════════════════════ -->
    <string name="consent_title">സ്വകാര്യതയും സമ്മതവും</string>
    <string name="consent_intro">നിങ്ങളുടെ ഡയറി ഒരു വ്യക്തിഗത സ്ഥലമാണ്, അത് ഞങ്ങൾ മാനിക്കുന്നു. Best Journal നിങ്ങളുടെ ഡാറ്റയെ എങ്ങനെ കൈകാര്യം ചെയ്യുന്നു എന്നത് ഇവിടെ സുതാര്യമായി കാണാം.</string>
    <string name="consent_card1_title">ലോക്കൽ സ്റ്റോറേജ്</string>
    <string name="consent_card1_body">നിങ്ങളുടെ എൻട്രികൾ നിങ്ങളുടെ ഉപകരണത്തിൽ തന്നെ നിലനിൽക്കും.</string>
    <string name="consent_card2_title">AI ഫീച്ചറുകൾ (USA)</string>
    <string name="consent_card2_body">ഓപ്ഷണലായി ടെക്സ്റ്റ് Google Gemini-യിലേക്കും, വോയ്സ് റെക്കോർഡിംഗുകൾ Groq-ലേക്കും, വായിച്ചു കേൾപ്പിക്കാനുള്ള ടെക്സ്റ്റ് Microsoft Edge-ലേക്കും USA-യിൽ അയയ്ക്കുന്നു (EU-US Data Privacy Framework + Standard Contractual Clauses).</string>
    <string name="consent_card3_title">അജ്ഞാത സ്ഥിതിവിവരക്കണക്കുകൾ</string>
    <string name="consent_card3_body">Firebase Analytics, ഓപ്ഷണൽ, സെറ്റിംഗ്സിൽ എപ്പോൾ വേണമെങ്കിലും മാറ്റാം.</string>
    <string name="consent_links_header">ഞങ്ങളുടെ നിയമ രേഖകൾ:</string>
    <string name="consent_accept_all">സമ്മതിച്ച് തുടങ്ങാം</string>
    <string name="consent_disable_stats">സ്ഥിതിവിവരക്കണക്കുകൾ ഓഫ് ചെയ്ത് തുടരുക</string>
    <string name="consent_confirmation">\"സമ്മതിച്ച് തുടങ്ങാം\" അമർത്തുന്നത് വഴി സ്വകാര്യതാ നയം, ഉപയോഗ നിബന്ധനകൾ, നിയമ വിവരം എന്നിവ വായിച്ചുവെന്നും വിവരിച്ചിട്ടുള്ള ഡാറ്റ പ്രോസസിംഗിന് നിങ്ങൾ സമ്മതിക്കുന്നു എന്നും സ്ഥിരീകരിക്കുന്നു. തീരുമാനം സെറ്റിംഗ്സിൽ എപ്പോൾ വേണമെങ്കിലും മാറ്റാം.</string>

    <string name="privacy_gate_groq_title">വോയ്സ് റെക്കോർഡിംഗ് Groq-ലേക്ക് അയയ്ക്കണോ?</string>
    <string name="privacy_gate_groq_body">ക്ലൗഡ് ട്രാൻസ്ക്രിപ്ഷനു വേണ്ടി നിങ്ങളുടെ വോയ്സ് റെക്കോർഡിംഗ് എൻക്രിപ്റ്റ് ചെയ്ത് Groq, Inc.-ലേക്ക് (Mountain View, USA) അയക്കുകയും അവിടെ ടെക്സ്റ്റ് ആയി മാറ്റുകയും ചെയ്യുന്നു. ഓഡിയോ ഫയൽ പ്രോസസിംഗിനു ശേഷം നീക്കം ചെയ്യും, പരിശീലനത്തിന് ഉപയോഗിക്കില്ല.\n\nബദൽ: ഉപകരണത്തിൽ ലോക്കൽ ട്രാൻസ്ക്രിപ്ഷൻ ഉപയോഗിക്കുക (ഓഫ്‌ലൈൻ, ഡാറ്റ കൈമാറ്റം ഇല്ല), Settings → AI-യിൽ മാറ്റാം.</string>
    <string name="privacy_gate_groq_accept">സമ്മതിച്ച് അയയ്ക്കുക</string>
    <string name="privacy_gate_groq_local">പകരം ലോക്കലായി ട്രാൻസ്ക്രൈബ് ചെയ്യുക</string>

    <string name="privacy_gate_gemini_title">ടെക്സ്റ്റ് Google Gemini-ലേക്ക് അയയ്ക്കണോ?</string>
    <string name="privacy_gate_gemini_body">AI ഫീച്ചറുകൾക്ക് (ഡാഷ്ബോർഡ്, സംഗ്രഹങ്ങൾ, പുനരവലോകനം, ടെക്സ്റ്റ് മെച്ചപ്പെടുത്തൽ) നിങ്ങളുടെ എൻട്രികളുടെ ഭാഗങ്ങൾ എൻക്രിപ്റ്റ് ചെയ്ത് Google Gemini (Firebase AI, USA)-ലേക്ക് അയയ്ക്കുന്നു. നിയമപരമായ അടിസ്ഥാനം: EU-US Data Privacy Framework + Standard Contractual Clauses. അഭ്യർത്ഥനകൾ പ്രോസസിംഗിനു ശേഷം നീക്കം ചെയ്യും, പരിശീലനത്തിന് ഉപയോഗിക്കില്ല.</string>
    <string name="privacy_gate_gemini_accept">സമ്മതിച്ച് അയയ്ക്കുക</string>
    <string name="privacy_gate_gemini_cancel">റദ്ദാക്കുക</string>

    <string name="privacy_gate_tts_title">ടെക്സ്റ്റ് Microsoft-ലേക്ക് അയയ്ക്കണോ?</string>
    <string name="privacy_gate_tts_body">വായിച്ചു കേൾപ്പിക്കാനായി ടെക്സ്റ്റ് എൻക്രിപ്റ്റ് ചെയ്ത് Microsoft Bing Speech (USA)-ലേക്ക് അയച്ച് ഓഡിയോ ആയി തിരികെ ലഭിക്കുന്നു. നിയമപരമായ അടിസ്ഥാനം: EU-US Data Privacy Framework + Standard Contractual Clauses.\n\nബദൽ: Android-ന്റെ ബിൽറ്റ്-ഇൻ ഓഫ്‌ലൈൻ TTS ഉപയോഗിക്കുക.</string>
    <string name="privacy_gate_tts_accept">സമ്മതിച്ച് വായിക്കുക</string>
    <string name="privacy_gate_tts_cancel">റദ്ദാക്കുക</string>

    <string name="settings_privacy_header">സ്വകാര്യത</string>
    <string name="settings_analytics_title">അജ്ഞാത സ്ഥിതിവിവരക്കണക്കുകൾ</string>
    <string name="settings_analytics_subtitle">പിശക് വിശകലനത്തിനും ഉൽപ്പന്ന മെച്ചപ്പെടുത്തലിനും ഉള്ള Firebase Analytics</string>

    <string name="settings_delete_account_title">അക്കൗണ്ടും ഡാറ്റയും നീക്കം ചെയ്യുക</string>
    <string name="settings_delete_account_subtitle">എല്ലാ ലോക്കൽ ഡാറ്റ, Google അക്കൗണ്ട്, Drive ബാക്കപ്പ് എന്നിവ ശാശ്വതമായി നീക്കം ചെയ്യുന്നു</string>
    <string name="settings_delete_account_confirm_title">അക്കൗണ്ട് ശാശ്വതമായി നീക്കം ചെയ്യണോ?</string>
    <string name="settings_delete_account_confirm_body">ഈ പ്രവർത്തനം തിരിച്ചെടുക്കാൻ കഴിയില്ല, ഇത് നീക്കം ചെയ്യുന്നു:\n\n• എല്ലാ ലോക്കൽ ഡയറി എൻട്രികൾ, ഫോട്ടോകൾ, വീഡിയോകൾ\n• നിങ്ങളുടെ Firebase അക്കൗണ്ട്\n• ആപ്പിന്റെ Google Drive ബാക്കപ്പ്\n\nആപ്പ് പുതിയ ഇൻസ്റ്റാളേഷൻ പോലെ വീണ്ടും ആരംഭിക്കും.</string>
    <string name="settings_delete_account_cancel">റദ്ദാക്കുക</string>
    <string name="settings_delete_account_confirm">അതെ, എല്ലാം നീക്കം ചെയ്യുക</string>

    <string name="settings_report_ai_title">AI പ്രതികരണം റിപ്പോർട്ട് ചെയ്യുക</string>
    <string name="settings_report_ai_subtitle">അനുചിതമോ തെറ്റായതോ ആയ AI ഔട്ട്പുട്ട്</string>
    <string name="settings_report_ai_confirm_title">Support-ന് ഇമെയിൽ തുറക്കണോ?</string>
    <string name="settings_report_ai_confirm_body">നിങ്ങളുടെ ഇമെയിൽ ആപ്പ് dev.app.support@gmail.com-ന് തയ്യാറായ സന്ദേശത്തോടൊപ്പം തുറക്കും. അയയ്ക്കുന്നതിനു മുമ്പ് വിവരണം ചേർക്കാം. പ്രവൃത്തി ദിവസങ്ങളിൽ 24 മണിക്കൂറിനുള്ളിൽ ഞങ്ങൾ മറുപടി നൽകും.\n\nദയവായി ഇവിടെ റിപ്പോർട്ട് ചെയ്യുക: ഡാഷ്ബോർഡ്, സംഗ്രഹങ്ങൾ, പുനരവലോകനം, ടെക്സ്റ്റ് മെച്ചപ്പെടുത്തൽ എന്നിവയിൽ നിന്നുള്ള അനുചിത, ആക്ഷേപകര, തെറ്റായ അല്ലെങ്കിൽ തെറ്റിദ്ധാരണ ഉണ്ടാക്കുന്ന AI ഔട്ട്പുട്ടുകൾ.</string>
    <string name="settings_report_ai_confirm">റിപ്പോർട്ട് സൃഷ്ടിക്കുക</string>
    <string name="settings_report_ai_cancel">റദ്ദാക്കുക</string>
    <string name="settings_report_ai_no_email">ഇമെയിൽ ആപ്പ് കണ്ടെത്തിയില്ല. ദയവായി റിപ്പോർട്ട് dev.app.support@gmail.com-ലേക്ക് അയയ്ക്കുക.</string>
    <string name="settings_report_ai_subject">Best Journal: അനുചിത AI പ്രതികരണം</string>
    <string name="settings_report_ai_body">നമസ്കാരം,\n\nBest Journal-ൽ ഒരു അനുചിതമോ തെറ്റായതോ ആയ AI പ്രതികരണം റിപ്പോർട്ട് ചെയ്യാൻ ഞാൻ ആഗ്രഹിക്കുന്നു.\n\nപ്രശ്നത്തിന്റെ വിവരണം:\n[ദയവായി പൂരിപ്പിക്കുക]\n\nസന്ദർഭം (ഏത് ഫീച്ചർ, ഏത് ഇൻപുട്ട്):\n[ദയവായി പൂരിപ്പിക്കുക]\n\nനന്ദി.</string>

    <string name="settings_revoke_title">കരാർ റദ്ദാക്കൽ</string>
    <string name="settings_revoke_subtitle">Premium വാങ്ങൽ</string>
    <string name="settings_revoke_confirm_title">Support-ന് ഇമെയിൽ തുറക്കണോ?</string>
    <string name="settings_revoke_confirm_body">നിങ്ങളുടെ ഇമെയിൽ ആപ്പ് dev.app.support@gmail.com-ന് തയ്യാറായ സന്ദേശത്തോടൊപ്പം തുറക്കും. പ്രവൃത്തി ദിവസങ്ങളിൽ 24 മണിക്കൂറിനുള്ളിൽ ഞങ്ങൾ മറുപടി നൽകും.\n\nറദ്ദാക്കൽ അവകാശത്തെക്കുറിച്ചുള്ള പൂർണ്ണ വിവരങ്ങൾ ഉപയോഗ നിബന്ധനകളിൽ (§ 16) ഉണ്ട്. സബ്സ്ക്രിപ്ഷനുകൾക്കായി Google Play → Subscriptions വഴിയും റദ്ദാക്കുക.</string>
    <string name="settings_revoke_cancel">റദ്ദാക്കുക</string>
    <string name="settings_revoke_confirm">റദ്ദാക്കൽ സൃഷ്ടിക്കുക</string>
    <string name="settings_revoke_no_email">ഇമെയിൽ ആപ്പ് കണ്ടെത്തിയില്ല. ദയവായി റദ്ദാക്കൽ dev.app.support@gmail.com-ലേക്ക് അയയ്ക്കുക.</string>
    <string name="settings_revoke_email_subject">Best Journal Premium കരാർ റദ്ദാക്കൽ</string>
    <string name="settings_revoke_email_body">ഇതിനാൽ Best Journal-ന്റെ Premium ഫീച്ചറുകളുമായി ബന്ധപ്പെട്ട കരാറിൽ നിന്നുള്ള എന്റെ പിന്മാറ്റം അറിയിക്കുന്നു.\n\nഅയച്ചയാൾ (Google അക്കൗണ്ട്): %1$s\nപിന്മാറ്റ സമയം: %2$s\n\nഈ പിന്മാറ്റം ആപ്പിലെ § 356a BGB-അനുസൃതമായ രണ്ട് ഘട്ട പിന്മാറ്റ ബട്ടൺ വഴി ആരംഭിക്കുകയും Gmail API വഴി സ്വയമേവ അയയ്ക്കപ്പെടുകയും ചെയ്തു.</string>
    <string name="settings_revoke_confirm_subject">നിങ്ങളുടെ ലഭ്യത സ്ഥിരീകരണം: Best Journal ലെ പിന്മാറ്റം</string>
    <string name="settings_revoke_confirm_user_body">നമസ്കാരം,\n\n%1$s ലെ നിങ്ങളുടെ പിന്മാറ്റം ഞങ്ങൾക്ക് ലഭിച്ചു. ഇത് § 356a BGB പ്രകാരമുള്ള നിങ്ങളുടെ ലഭ്യത സ്ഥിരീകരണമാണ്.\n\nനിങ്ങളുടെ പിന്മാറ്റം എത്രയും വേഗം പ്രോസസ് ചെയ്യും; ചോദ്യങ്ങളുണ്ടെങ്കിൽ dev.app.support@gmail.com വഴി നിങ്ങളെ ബന്ധപ്പെടും.\n\nഇനി പുതിയ ബില്ലിംഗ് ഉണ്ടാകാതിരിക്കാനായി, ദയവായി Google Play Store ലെ "Subscriptions" വിഭാഗത്തിലും നിങ്ങളുടെ സബ്സ്ക്രിപ്ഷൻ റദ്ദാക്കുക.\n\nനന്ദിയും ആശംസകളും\nBest Journal (Frank Barwandt)</string>
    <string name="settings_revoke_no_account">സൈൻ ഇൻ ചെയ്ത Google അക്കൗണ്ട് വിലാസം കണ്ടെത്തിയില്ല. ദയവായി Settings ൽ നിങ്ങളുടെ Google അക്കൗണ്ടിൽ സൈൻ ഇൻ ചെയ്യുക അല്ലെങ്കിൽ പിന്മാറ്റം dev.app.support@gmail.com ലേക്ക് കൈയോടെ അയയ്ക്കുക.</string>
    <string name="settings_revoke_sending">പിന്മാറ്റം അയയ്ക്കുന്നു…</string>
    <string name="settings_revoke_success_title">പിന്മാറ്റം ലഭിച്ചു</string>
    <string name="settings_revoke_success_body">നിങ്ങളുടെ പിന്മാറ്റം dev.app.support@gmail.com ലേക്ക് അയച്ചു. ലഭ്യത സ്ഥിരീകരണവും നിങ്ങളുടെ ഇൻബോക്സിലുണ്ട്.</string>
    <string name="settings_revoke_success_close">അടയ്ക്കുക</string>
    <string name="settings_revoke_error_title">പിന്മാറ്റം അയയ്ക്കാനായില്ല</string>
    <string name="settings_revoke_error_body">സ്വയമേവയുള്ള അയയ്ക്കൽ പരാജയപ്പെട്ടു: %1$s\n\nമാറ്റായി നിങ്ങൾ dev.app.support@gmail.com ലേക്ക് കൈയോടെ ഒരു ഇമെയിൽ അയയ്ക്കാം. അതിന് "ഇമെയിൽ ആപ്പ് തുറക്കുക" അമർത്തുക.</string>
    <string name="settings_revoke_error_email_fallback">ഇമെയിൽ ആപ്പ് തുറക്കുക</string>
"""


def insert(path, block):
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()
    if "CONSENT SCREEN + PRIVACY GATES + PRIVACY SECTION" in content:
        return f"SKIP: {path}"
    new_content = content.replace("</resources>", block + "\n\n</resources>")
    d = os.path.dirname(os.path.abspath(path))
    with tempfile.NamedTemporaryFile("w", dir=d, suffix=".tmp", delete=False, encoding="utf-8", newline="\n") as tmp:
        tmp.write(new_content); tmp_path = tmp.name
    os.replace(tmp_path, path)
    return f"OK: {path}"


for locale, block in TRANSLATIONS.items():
    target = os.path.join(APP_DIR, f"values-{locale}", "strings.xml")
    print(insert(target, block))

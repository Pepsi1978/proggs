#!/usr/bin/env python3
"""Batch 2: nl, pl, uk, tr, ja."""
import os, re, tempfile

APP_DIR = os.path.expanduser("~/proggs/BestJournalAndroid/app/src/main/res")
TRANSLATIONS = {}

# ═══════════ DUTCH (nl) — Informal "je/jij", 0-5% longer ═══════════
TRANSLATIONS["nl"] = r"""
    <!-- ════════════════════════════════════════════════════════════════════
         CONSENT SCREEN + PRIVACY GATES + PRIVACY SECTION (April 2026)
         ════════════════════════════════════════════════════════════════════ -->
    <string name="consent_title">Privacy en toestemming</string>
    <string name="consent_intro">Je dagboek is een persoonlijke ruimte, en dat respecteren we. Hier zie je transparant hoe Best Journal met je gegevens omgaat.</string>
    <string name="consent_card1_title">Lokale opslag</string>
    <string name="consent_card1_body">Je invoer blijft op je apparaat.</string>
    <string name="consent_card2_title">AI-functies (VS)</string>
    <string name="consent_card2_body">Optioneel worden teksten naar Google Gemini, spraakopnames naar Groq en voorleesteksten naar Microsoft Edge in de VS gestuurd (EU-US Data Privacy Framework + standaard contractbepalingen).</string>
    <string name="consent_card3_title">Anonieme statistieken</string>
    <string name="consent_card3_body">Firebase Analytics, optioneel, op elk moment aan te passen in de instellingen.</string>
    <string name="consent_links_header">Onze juridische teksten:</string>
    <string name="consent_accept_all">Akkoord en beginnen</string>
    <string name="consent_disable_stats">Statistieken uitschakelen en doorgaan</string>
    <string name="consent_confirmation">Door op \"Akkoord en beginnen\" te tikken bevestig je dat je het privacybeleid, de gebruiksvoorwaarden en het impressum hebt gelezen en akkoord gaat met de beschreven gegevensverwerking. Je kunt je beslissing op elk moment aanpassen in de instellingen.</string>

    <string name="privacy_gate_groq_title">Spraakopname naar Groq sturen?</string>
    <string name="privacy_gate_groq_body">Voor cloud-transcriptie wordt je spraakopname versleuteld naar Groq, Inc. (Mountain View, VS) verzonden en daar naar tekst omgezet. Het audiobestand wordt na verwerking verwijderd en niet voor training gebruikt.\n\nAlternatief: gebruik lokale transcriptie op het apparaat (offline, geen gegevensoverdracht), instelbaar bij Instellingen → AI.</string>
    <string name="privacy_gate_groq_accept">Akkoord en verzenden</string>
    <string name="privacy_gate_groq_local">In plaats daarvan lokaal transcriberen</string>

    <string name="privacy_gate_gemini_title">Tekst naar Google Gemini sturen?</string>
    <string name="privacy_gate_gemini_body">Voor AI-functies (dashboard, samenvattingen, terugblikken, tekstverbetering) worden fragmenten van je invoer versleuteld naar Google Gemini (Firebase AI, VS) gestuurd. Rechtsgrond: EU-US Data Privacy Framework + standaard contractbepalingen. Verzoeken worden na verwerking verwijderd en niet voor training gebruikt.</string>
    <string name="privacy_gate_gemini_accept">Akkoord en verzenden</string>
    <string name="privacy_gate_gemini_cancel">Annuleren</string>

    <string name="privacy_gate_tts_title">Tekst naar Microsoft sturen?</string>
    <string name="privacy_gate_tts_body">Voor voorlezen wordt de tekst versleuteld naar Microsoft Bing Speech (VS) verzonden en als audio teruggegeven. Rechtsgrond: EU-US Data Privacy Framework + standaard contractbepalingen.\n\nAlternatief: gebruik de ingebouwde offline-TTS van Android.</string>
    <string name="privacy_gate_tts_accept">Akkoord en voorlezen</string>
    <string name="privacy_gate_tts_cancel">Annuleren</string>

    <string name="settings_privacy_header">Privacy</string>
    <string name="settings_analytics_title">Anonieme statistieken</string>
    <string name="settings_analytics_subtitle">Firebase Analytics voor foutanalyse en productverbetering</string>

    <string name="settings_delete_account_title">Account en gegevens verwijderen</string>
    <string name="settings_delete_account_subtitle">Verwijdert onomkeerbaar alle lokale gegevens, je Google-account en de Drive-back-up</string>
    <string name="settings_delete_account_confirm_title">Account definitief verwijderen?</string>
    <string name="settings_delete_account_confirm_body">Deze actie is onomkeerbaar en verwijdert:\n\n• Alle lokale dagboekinvoer, foto\'s en video\'s\n• Je Firebase-account\n• De Google Drive-back-up van de app\n\nDe app start daarna opnieuw op als verse installatie.</string>
    <string name="settings_delete_account_cancel">Annuleren</string>
    <string name="settings_delete_account_confirm">Ja, alles verwijderen</string>

    <string name="settings_report_ai_title">AI-antwoord melden</string>
    <string name="settings_report_ai_subtitle">Ongepaste of foutieve AI-uitvoer</string>
    <string name="settings_report_ai_confirm_title">E-mail naar support openen?</string>
    <string name="settings_report_ai_confirm_body">Je e-mailapp opent met een voorbereid bericht aan dev.app.support@gmail.com. Je kunt de beschrijving aanvullen voordat je verzendt. We reageren binnen 24 uur op werkdagen.\n\nMeld hier: ongepaste, aanstootgevende, onjuiste of misleidende AI-uitvoer uit het dashboard, samenvattingen, terugblikken of tekstverbetering.</string>
    <string name="settings_report_ai_confirm">Melding aanmaken</string>
    <string name="settings_report_ai_cancel">Annuleren</string>
    <string name="settings_report_ai_no_email">Geen e-mailapp gevonden. Stuur de melding naar dev.app.support@gmail.com.</string>
    <string name="settings_report_ai_subject">Best Journal: ongepast AI-antwoord</string>
    <string name="settings_report_ai_body">Hallo,\n\nik wil een ongepast of foutief AI-antwoord in Best Journal melden.\n\nBeschrijving van het probleem:\n[In te vullen]\n\nContext (welke functie, welke invoer):\n[In te vullen]\n\nBedankt.</string>

    <string name="settings_revoke_title">Herroeping</string>
    <string name="settings_revoke_subtitle">Premium-aankoop</string>
    <string name="settings_revoke_confirm_title">E-mail naar support openen?</string>
    <string name="settings_revoke_confirm_body">Je e-mailapp opent met een voorbereid bericht aan dev.app.support@gmail.com. We reageren binnen 24 uur op werkdagen.\n\nVolledige informatie over het herroepingsrecht (EU-richtlijn 2011/83) vind je in de gebruiksvoorwaarden (§ 16). Zeg abonnementen ook op via Google Play → Abonnementen.</string>
    <string name="settings_revoke_cancel">Annuleren</string>
    <string name="settings_revoke_confirm">Herroeping aanmaken</string>
    <string name="settings_revoke_no_email">Geen e-mailapp gevonden. Stuur de herroeping naar dev.app.support@gmail.com.</string>
    <string name="settings_revoke_email_subject">Herroeping Premium-contract Best Journal</string>
    <string name="settings_revoke_email_body">Hallo,\n\nhierbij herroep ik het door mij afgesloten contract over de Premium-functies van Best Journal.\n\nBesteld op: [in te vullen]\nGoogle-account-e-mail: [in te vullen indien afwijkend]\nNaam: [in te vullen]\n\nDatum: [vandaag]</string>
"""

# ═══════════ POLISH (pl) — Informal "ty", prefer impersonal ═══════════
TRANSLATIONS["pl"] = r"""
    <!-- ════════════════════════════════════════════════════════════════════
         CONSENT SCREEN + PRIVACY GATES + PRIVACY SECTION (April 2026)
         ════════════════════════════════════════════════════════════════════ -->
    <string name="consent_title">Prywatność i zgoda</string>
    <string name="consent_intro">Twój dziennik to przestrzeń osobista i szanujemy to. Tutaj widzisz w przejrzysty sposób, jak Best Journal obchodzi się z Twoimi danymi.</string>
    <string name="consent_card1_title">Zapis lokalny</string>
    <string name="consent_card1_body">Twoje wpisy pozostają na Twoim urządzeniu.</string>
    <string name="consent_card2_title">Funkcje AI (USA)</string>
    <string name="consent_card2_body">Opcjonalnie teksty są wysyłane do Google Gemini, nagrania głosowe do Groq, a teksty do odczytu na głos do Microsoft Edge w USA (EU-US Data Privacy Framework + standardowe klauzule umowne).</string>
    <string name="consent_card3_title">Anonimowe statystyki</string>
    <string name="consent_card3_body">Firebase Analytics, opcjonalnie, możesz zmienić w dowolnej chwili w ustawieniach.</string>
    <string name="consent_links_header">Nasze dokumenty prawne:</string>
    <string name="consent_accept_all">Zgadzam się i zaczynam</string>
    <string name="consent_disable_stats">Wyłącz statystyki i kontynuuj</string>
    <string name="consent_confirmation">Dotykając „Zgadzam się i zaczynam\" potwierdzasz zapoznanie się z polityką prywatności, warunkami użytkowania oraz notą prawną i akceptujesz opisane przetwarzanie danych. Swoją decyzję możesz zmienić w każdej chwili w ustawieniach.</string>

    <string name="privacy_gate_groq_title">Wysłać nagranie głosu do Groq?</string>
    <string name="privacy_gate_groq_body">W celu transkrypcji w chmurze Twoje nagranie głosu jest wysyłane zaszyfrowane do Groq, Inc. (Mountain View, USA) i zamieniane na tekst. Plik audio jest usuwany po przetworzeniu i nie jest używany do trenowania.\n\nAlternatywa: użyj lokalnej transkrypcji na urządzeniu (offline, bez przesyłania danych), ustawialnej w Ustawienia → AI.</string>
    <string name="privacy_gate_groq_accept">Zgadzam się i wysyłam</string>
    <string name="privacy_gate_groq_local">Transkrybuj lokalnie</string>

    <string name="privacy_gate_gemini_title">Wysłać tekst do Google Gemini?</string>
    <string name="privacy_gate_gemini_body">Dla funkcji AI (pulpit, podsumowania, retrospekcje, poprawa tekstu) fragmenty Twoich wpisów są wysyłane zaszyfrowane do Google Gemini (Firebase AI, USA). Podstawa prawna: EU-US Data Privacy Framework + standardowe klauzule umowne. Zapytania są usuwane po przetworzeniu i nie są używane do trenowania.</string>
    <string name="privacy_gate_gemini_accept">Zgadzam się i wysyłam</string>
    <string name="privacy_gate_gemini_cancel">Anuluj</string>

    <string name="privacy_gate_tts_title">Wysłać tekst do Microsoftu?</string>
    <string name="privacy_gate_tts_body">Do odczytu na głos tekst jest wysyłany zaszyfrowany do Microsoft Bing Speech (USA) i zwracany jako audio. Podstawa prawna: EU-US Data Privacy Framework + standardowe klauzule umowne.\n\nAlternatywa: użyj systemowej syntezy mowy offline z Androida.</string>
    <string name="privacy_gate_tts_accept">Zgadzam się i czytaj</string>
    <string name="privacy_gate_tts_cancel">Anuluj</string>

    <string name="settings_privacy_header">Prywatność</string>
    <string name="settings_analytics_title">Anonimowe statystyki</string>
    <string name="settings_analytics_subtitle">Firebase Analytics do analizy błędów i ulepszania produktu</string>

    <string name="settings_delete_account_title">Usuń konto i dane</string>
    <string name="settings_delete_account_subtitle">Nieodwracalnie usuwa wszystkie lokalne dane, Twoje konto Google i kopię zapasową Drive</string>
    <string name="settings_delete_account_confirm_title">Usunąć konto na stałe?</string>
    <string name="settings_delete_account_confirm_body">Ta operacja jest nieodwracalna i usuwa:\n\n• Wszystkie lokalne wpisy, zdjęcia i filmy\n• Twoje konto Firebase\n• Kopię zapasową aplikacji w Google Drive\n\nAplikacja uruchomi się ponownie jako świeża instalacja.</string>
    <string name="settings_delete_account_cancel">Anuluj</string>
    <string name="settings_delete_account_confirm">Tak, usuń wszystko</string>

    <string name="settings_report_ai_title">Zgłoś odpowiedź AI</string>
    <string name="settings_report_ai_subtitle">Nieodpowiednia lub błędna odpowiedź AI</string>
    <string name="settings_report_ai_confirm_title">Otworzyć e-mail do pomocy technicznej?</string>
    <string name="settings_report_ai_confirm_body">Twoja aplikacja pocztowa otworzy się z przygotowaną wiadomością do dev.app.support@gmail.com. Możesz uzupełnić opis przed wysłaniem. Odpowiadamy w ciągu 24 godzin w dni robocze.\n\nZgłoś tutaj: nieodpowiednie, obraźliwe, nieprawdziwe lub wprowadzające w błąd odpowiedzi AI z pulpitu, podsumowań, retrospekcji lub poprawy tekstu.</string>
    <string name="settings_report_ai_confirm">Utwórz zgłoszenie</string>
    <string name="settings_report_ai_cancel">Anuluj</string>
    <string name="settings_report_ai_no_email">Nie znaleziono aplikacji pocztowej. Wyślij zgłoszenie na dev.app.support@gmail.com.</string>
    <string name="settings_report_ai_subject">Best Journal: nieodpowiednia odpowiedź AI</string>
    <string name="settings_report_ai_body">Dzień dobry,\n\nchcę zgłosić nieodpowiednią lub błędną odpowiedź AI w Best Journal.\n\nOpis problemu:\n[Do uzupełnienia]\n\nKontekst (która funkcja, które wejście):\n[Do uzupełnienia]\n\nDziękuję.</string>

    <string name="settings_revoke_title">Odstąpienie od umowy</string>
    <string name="settings_revoke_subtitle">Zakup Premium</string>
    <string name="settings_revoke_confirm_title">Otworzyć e-mail do pomocy technicznej?</string>
    <string name="settings_revoke_confirm_body">Twoja aplikacja pocztowa otworzy się z przygotowaną wiadomością do dev.app.support@gmail.com. Odpowiadamy w ciągu 24 godzin w dni robocze.\n\nPełne informacje o prawie odstąpienia od umowy (dyrektywa UE 2011/83) znajdziesz w warunkach użytkowania (§ 16). Abonamenty anuluj dodatkowo w Google Play → Subskrypcje.</string>
    <string name="settings_revoke_cancel">Anuluj</string>
    <string name="settings_revoke_confirm">Utwórz odstąpienie</string>
    <string name="settings_revoke_no_email">Nie znaleziono aplikacji pocztowej. Wyślij odstąpienie na dev.app.support@gmail.com.</string>
    <string name="settings_revoke_email_subject">Odstąpienie od umowy Premium Best Journal</string>
    <string name="settings_revoke_email_body">Dzień dobry,\n\nniniejszym odstępuję od zawartej przeze mnie umowy dotyczącej funkcji Premium w Best Journal.\n\nZamówiono dnia: [do uzupełnienia]\nE-mail konta Google: [do uzupełnienia, jeśli inny niż nadawca]\nImię i nazwisko: [do uzupełnienia]\n\nData: [dzisiaj]</string>
"""

# ═══════════ UKRAINIAN (uk) — Formal Ви, NO Russisms, Ukrainian Cyrillic ═══════════
TRANSLATIONS["uk"] = r"""
    <!-- ════════════════════════════════════════════════════════════════════
         CONSENT SCREEN + PRIVACY GATES + PRIVACY SECTION (April 2026)
         ════════════════════════════════════════════════════════════════════ -->
    <string name="consent_title">Конфіденційність і згода</string>
    <string name="consent_intro">Ваш щоденник — це особистий простір, і ми це поважаємо. Тут Ви прозоро бачите, як Best Journal поводиться з Вашими даними.</string>
    <string name="consent_card1_title">Локальне зберігання</string>
    <string name="consent_card1_body">Ваші записи залишаються на Вашому пристрої.</string>
    <string name="consent_card2_title">Функції ШІ (США)</string>
    <string name="consent_card2_body">За бажанням тексти надсилаються до Google Gemini, голосові записи — до Groq, а тексти для озвучення — до Microsoft Edge у США (EU-US Data Privacy Framework + стандартні договірні положення).</string>
    <string name="consent_card3_title">Анонімна статистика</string>
    <string name="consent_card3_body">Firebase Analytics, за бажанням, можна змінити будь-коли в налаштуваннях.</string>
    <string name="consent_links_header">Наші правові документи:</string>
    <string name="consent_accept_all">Погоджуюсь і починаю</string>
    <string name="consent_disable_stats">Вимкнути статистику та продовжити</string>
    <string name="consent_confirmation">Натискаючи «Погоджуюсь і починаю», Ви підтверджуєте, що ознайомилися з політикою конфіденційності, умовами використання та правовою інформацією, і погоджуєтесь з описаною обробкою даних. Ви можете змінити своє рішення будь-коли в налаштуваннях.</string>

    <string name="privacy_gate_groq_title">Надіслати голосовий запис до Groq?</string>
    <string name="privacy_gate_groq_body">Для хмарної транскрипції Ваш голосовий запис надсилається зашифрованим до Groq, Inc. (Маунтін-В\'ю, США) і перетворюється там на текст. Аудіофайл видаляється після обробки і не використовується для навчання.\n\nАльтернатива: скористайтесь локальною транскрипцією на пристрої (офлайн, без передачі даних), доступна в Налаштування → ШІ.</string>
    <string name="privacy_gate_groq_accept">Погоджуюсь і надсилаю</string>
    <string name="privacy_gate_groq_local">Транскрибувати локально</string>

    <string name="privacy_gate_gemini_title">Надіслати текст до Google Gemini?</string>
    <string name="privacy_gate_gemini_body">Для функцій ШІ (панель, підсумки, ретроспективи, покращення тексту) фрагменти Ваших записів надсилаються зашифрованими до Google Gemini (Firebase AI, США). Правова підстава: EU-US Data Privacy Framework + стандартні договірні положення. Запити видаляються після обробки і не використовуються для навчання.</string>
    <string name="privacy_gate_gemini_accept">Погоджуюсь і надсилаю</string>
    <string name="privacy_gate_gemini_cancel">Скасувати</string>

    <string name="privacy_gate_tts_title">Надіслати текст до Microsoft?</string>
    <string name="privacy_gate_tts_body">Для озвучення текст надсилається зашифрованим до Microsoft Bing Speech (США) і повертається як аудіо. Правова підстава: EU-US Data Privacy Framework + стандартні договірні положення.\n\nАльтернатива: скористайтесь вбудованим офлайн-синтезатором мовлення Android.</string>
    <string name="privacy_gate_tts_accept">Погоджуюсь і озвучити</string>
    <string name="privacy_gate_tts_cancel">Скасувати</string>

    <string name="settings_privacy_header">Конфіденційність</string>
    <string name="settings_analytics_title">Анонімна статистика</string>
    <string name="settings_analytics_subtitle">Firebase Analytics для аналізу помилок і покращення продукту</string>

    <string name="settings_delete_account_title">Видалити обліковий запис і дані</string>
    <string name="settings_delete_account_subtitle">Безповоротно видаляє всі локальні дані, Ваш обліковий запис Google та резервну копію Drive</string>
    <string name="settings_delete_account_confirm_title">Видалити обліковий запис остаточно?</string>
    <string name="settings_delete_account_confirm_body">Ця дія незворотна і видаляє:\n\n• Усі локальні записи, фото й відео\n• Ваш обліковий запис Firebase\n• Резервну копію застосунку в Google Drive\n\nПісля цього застосунок запуститься як нова інсталяція.</string>
    <string name="settings_delete_account_cancel">Скасувати</string>
    <string name="settings_delete_account_confirm">Так, видалити все</string>

    <string name="settings_report_ai_title">Поскаржитись на відповідь ШІ</string>
    <string name="settings_report_ai_subtitle">Недоречна або помилкова відповідь ШІ</string>
    <string name="settings_report_ai_confirm_title">Відкрити електронний лист до підтримки?</string>
    <string name="settings_report_ai_confirm_body">Ваш поштовий застосунок відкриється з підготовленим листом до dev.app.support@gmail.com. Ви можете доповнити опис перед відправленням. Ми відповідаємо протягом 24 годин у робочі дні.\n\nПовідомте тут про: недоречні, образливі, неправдиві або оманливі відповіді ШІ з панелі, підсумків, ретроспектив або покращення тексту.</string>
    <string name="settings_report_ai_confirm">Створити скаргу</string>
    <string name="settings_report_ai_cancel">Скасувати</string>
    <string name="settings_report_ai_no_email">Поштовий застосунок не знайдено. Надішліть скаргу на dev.app.support@gmail.com.</string>
    <string name="settings_report_ai_subject">Best Journal: недоречна відповідь ШІ</string>
    <string name="settings_report_ai_body">Доброго дня,\n\nхочу повідомити про недоречну або помилкову відповідь ШІ в Best Journal.\n\nОпис проблеми:\n[Заповнити]\n\nКонтекст (яка функція, який ввід):\n[Заповнити]\n\nДякую.</string>

    <string name="settings_revoke_title">Відкликання</string>
    <string name="settings_revoke_subtitle">Купівля Premium</string>
    <string name="settings_revoke_confirm_title">Відкрити електронний лист до підтримки?</string>
    <string name="settings_revoke_confirm_body">Ваш поштовий застосунок відкриється з підготовленим листом до dev.app.support@gmail.com. Ми відповідаємо протягом 24 годин у робочі дні.\n\nПовна інформація про право на відкликання (Директива ЄС 2011/83) міститься в умовах використання (§ 16). Підписки додатково скасовуйте через Google Play → Підписки.</string>
    <string name="settings_revoke_cancel">Скасувати</string>
    <string name="settings_revoke_confirm">Створити відкликання</string>
    <string name="settings_revoke_no_email">Поштовий застосунок не знайдено. Надішліть відкликання на dev.app.support@gmail.com.</string>
    <string name="settings_revoke_email_subject">Відкликання контракту Premium Best Journal</string>
    <string name="settings_revoke_email_body">Доброго дня,\n\nцим повідомляю про відкликання укладеного мною договору щодо функцій Premium у Best Journal.\n\nЗамовлено: [заповнити]\nE-mail облікового запису Google: [заповнити, якщо відрізняється від відправника]\nІм\'я: [заповнити]\n\nДата: [сьогодні]</string>
"""

# ═══════════ TURKISH (tr) — Informal "sen", vowel harmony ═══════════
TRANSLATIONS["tr"] = r"""
    <!-- ════════════════════════════════════════════════════════════════════
         CONSENT SCREEN + PRIVACY GATES + PRIVACY SECTION (April 2026)
         ════════════════════════════════════════════════════════════════════ -->
    <string name="consent_title">Gizlilik ve onay</string>
    <string name="consent_intro">Günlüğün kişisel bir alandır ve buna saygı duyuyoruz. Best Journal verilerini nasıl işlediğini burada şeffaf bir şekilde görebilirsin.</string>
    <string name="consent_card1_title">Yerel depolama</string>
    <string name="consent_card1_body">Girişlerin cihazında kalır.</string>
    <string name="consent_card2_title">Yapay zekâ özellikleri (ABD)</string>
    <string name="consent_card2_body">Opsiyonel olarak metinler Google Gemini\'ye, ses kayıtları Groq\'a ve sesli okuma metinleri Microsoft Edge\'e ABD\'de gönderilir (EU-US Data Privacy Framework + standart sözleşme hükümleri).</string>
    <string name="consent_card3_title">Anonim istatistikler</string>
    <string name="consent_card3_body">Firebase Analytics, isteğe bağlı, ayarlardan istediğin zaman değiştirebilirsin.</string>
    <string name="consent_links_header">Yasal metinlerimiz:</string>
    <string name="consent_accept_all">Kabul et ve başla</string>
    <string name="consent_disable_stats">İstatistikleri devre dışı bırak ve devam et</string>
    <string name="consent_confirmation">«Kabul et ve başla»ya dokunarak gizlilik politikasını, kullanım koşullarını ve yasal bilgileri okuduğunu ve açıklanan veri işleme faaliyetlerini kabul ettiğini onaylarsın. Kararını istediğin zaman ayarlardan değiştirebilirsin.</string>

    <string name="privacy_gate_groq_title">Ses kaydı Groq\'a gönderilsin mi?</string>
    <string name="privacy_gate_groq_body">Bulut transkripsiyonu için ses kaydın şifrelenmiş olarak Groq, Inc.\'e (Mountain View, ABD) gönderilir ve orada metne dönüştürülür. Ses dosyası işlendikten sonra silinir ve eğitim için kullanılmaz.\n\nAlternatif: cihazda yerel transkripsiyonu kullan (çevrimdışı, veri aktarımı yok), Ayarlar → Yapay zekâ bölümünden ayarlanabilir.</string>
    <string name="privacy_gate_groq_accept">Kabul et ve gönder</string>
    <string name="privacy_gate_groq_local">Yerel olarak transkribe et</string>

    <string name="privacy_gate_gemini_title">Metin Google Gemini\'ye gönderilsin mi?</string>
    <string name="privacy_gate_gemini_body">Yapay zekâ özellikleri (pano, özetler, retrospektifler, metin iyileştirme) için girişlerinin parçaları şifrelenmiş olarak Google Gemini\'ye (Firebase AI, ABD) gönderilir. Yasal dayanak: EU-US Data Privacy Framework + standart sözleşme hükümleri. Talepler işlendikten sonra silinir ve eğitim için kullanılmaz.</string>
    <string name="privacy_gate_gemini_accept">Kabul et ve gönder</string>
    <string name="privacy_gate_gemini_cancel">İptal</string>

    <string name="privacy_gate_tts_title">Metin Microsoft\'a gönderilsin mi?</string>
    <string name="privacy_gate_tts_body">Sesli okuma için metin şifrelenmiş olarak Microsoft Bing Speech\'e (ABD) gönderilir ve ses olarak geri döner. Yasal dayanak: EU-US Data Privacy Framework + standart sözleşme hükümleri.\n\nAlternatif: Android\'in yerleşik çevrimdışı sesli okuma özelliğini kullan.</string>
    <string name="privacy_gate_tts_accept">Kabul et ve oku</string>
    <string name="privacy_gate_tts_cancel">İptal</string>

    <string name="settings_privacy_header">Gizlilik</string>
    <string name="settings_analytics_title">Anonim istatistikler</string>
    <string name="settings_analytics_subtitle">Hata analizi ve ürün iyileştirme için Firebase Analytics</string>

    <string name="settings_delete_account_title">Hesabı ve verileri sil</string>
    <string name="settings_delete_account_subtitle">Tüm yerel verileri, Google hesabını ve Drive yedeğini geri dönüşü olmadan siler</string>
    <string name="settings_delete_account_confirm_title">Hesap kalıcı olarak silinsin mi?</string>
    <string name="settings_delete_account_confirm_body">Bu işlem geri alınamaz ve şunları siler:\n\n• Tüm yerel günlük girişleri, fotoğraflar ve videolar\n• Firebase hesabın\n• Uygulamanın Google Drive yedeği\n\nUygulama, yeni bir kurulum gibi yeniden başlar.</string>
    <string name="settings_delete_account_cancel">İptal</string>
    <string name="settings_delete_account_confirm">Evet, her şeyi sil</string>

    <string name="settings_report_ai_title">Yapay zekâ yanıtını bildir</string>
    <string name="settings_report_ai_subtitle">Uygunsuz veya hatalı yapay zekâ çıktısı</string>
    <string name="settings_report_ai_confirm_title">Destek ekibine e-posta açılsın mı?</string>
    <string name="settings_report_ai_confirm_body">E-posta uygulaman dev.app.support@gmail.com için hazırlanmış bir mesajla açılır. Göndermeden önce açıklamayı tamamlayabilirsin. İş günlerinde 24 saat içinde yanıt veririz.\n\nŞunları buradan bildir: panodan, özetlerden, retrospektiflerden veya metin iyileştirmeden kaynaklanan uygunsuz, saldırgan, yanlış veya yanıltıcı yapay zekâ çıktıları.</string>
    <string name="settings_report_ai_confirm">Bildirim oluştur</string>
    <string name="settings_report_ai_cancel">İptal</string>
    <string name="settings_report_ai_no_email">E-posta uygulaması bulunamadı. Bildirimi dev.app.support@gmail.com adresine gönder.</string>
    <string name="settings_report_ai_subject">Best Journal: uygunsuz yapay zekâ yanıtı</string>
    <string name="settings_report_ai_body">Merhaba,\n\nBest Journal\'da uygunsuz veya hatalı bir yapay zekâ yanıtını bildirmek istiyorum.\n\nSorunun açıklaması:\n[Doldurulacak]\n\nBağlam (hangi özellik, hangi giriş):\n[Doldurulacak]\n\nTeşekkürler.</string>

    <string name="settings_revoke_title">Cayma hakkı</string>
    <string name="settings_revoke_subtitle">Premium satın alma</string>
    <string name="settings_revoke_confirm_title">Destek ekibine e-posta açılsın mı?</string>
    <string name="settings_revoke_confirm_body">E-posta uygulaman dev.app.support@gmail.com için hazırlanmış bir mesajla açılır. İş günlerinde 24 saat içinde yanıt veririz.\n\nCayma hakkına ilişkin tam bilgiyi (yürürlükteki tüketici koruma mevzuatı) kullanım koşullarında (§ 16) bulabilirsin. Abonelikler için ayrıca Google Play → Abonelikler üzerinden iptal et.</string>
    <string name="settings_revoke_cancel">İptal</string>
    <string name="settings_revoke_confirm">Cayma oluştur</string>
    <string name="settings_revoke_no_email">E-posta uygulaması bulunamadı. Cayma bildirimini dev.app.support@gmail.com adresine gönder.</string>
    <string name="settings_revoke_email_subject">Best Journal Premium sözleşme cayması</string>
    <string name="settings_revoke_email_body">Merhaba,\n\nişbu yazıyla Best Journal\'ın Premium özelliklerine ilişkin sözleşmeden caydığımı bildiririm.\n\nSipariş tarihi: [doldurulacak]\nGoogle hesap e-postası: [gönderenden farklıysa doldurulacak]\nAd Soyad: [doldurulacak]\n\nTarih: [bugün]</string>
"""

# ═══════════ JAPANESE (ja) — Teineigo (です/ます), full-width punctuation ═══════════
TRANSLATIONS["ja"] = r"""
    <!-- ════════════════════════════════════════════════════════════════════
         CONSENT SCREEN + PRIVACY GATES + PRIVACY SECTION (April 2026)
         ════════════════════════════════════════════════════════════════════ -->
    <string name="consent_title">プライバシーと同意</string>
    <string name="consent_intro">日記はあなたの個人的な空間です。私たちはそれを尊重します。Best Journalがあなたのデータをどのように扱うかを、ここで透明に確認できます。</string>
    <string name="consent_card1_title">ローカル保存</string>
    <string name="consent_card1_body">入力内容はデバイスに保存されます。</string>
    <string name="consent_card2_title">AI機能(米国)</string>
    <string name="consent_card2_body">任意でテキストはGoogle Geminiへ、音声録音はGroqへ、読み上げ用テキストはMicrosoft Edgeへ米国に送信されます(EU-US Data Privacy Framework + 標準契約条項)。</string>
    <string name="consent_card3_title">匿名の統計情報</string>
    <string name="consent_card3_body">Firebase Analytics、任意、設定からいつでも変更できます。</string>
    <string name="consent_links_header">当社の法的文書:</string>
    <string name="consent_accept_all">同意して始める</string>
    <string name="consent_disable_stats">統計を無効にして続ける</string>
    <string name="consent_confirmation">「同意して始める」をタップすると、プライバシーポリシー、利用規約、特定商取引法表記を読み、記載されたデータ処理に同意したことになります。設定からいつでも変更できます。</string>

    <string name="privacy_gate_groq_title">音声録音をGroqに送信しますか?</string>
    <string name="privacy_gate_groq_body">クラウド文字起こしのため、音声録音は暗号化されてGroq, Inc.(マウンテンビュー、米国)に送信され、そこでテキストに変換されます。音声ファイルは処理後に削除され、トレーニングには使用されません。\n\n代替案:デバイス上のローカル文字起こし(オフライン、データ転送なし)を使用してください。設定 → AI で切り替えられます。</string>
    <string name="privacy_gate_groq_accept">同意して送信</string>
    <string name="privacy_gate_groq_local">ローカルで文字起こしする</string>

    <string name="privacy_gate_gemini_title">テキストをGoogle Geminiに送信しますか?</string>
    <string name="privacy_gate_gemini_body">AI機能(ダッシュボード、要約、振り返り、テキスト改善)のため、記録の一部が暗号化されてGoogle Gemini(Firebase AI、米国)に送信されます。法的根拠:EU-US Data Privacy Framework + 標準契約条項。リクエストは処理後に削除され、トレーニングには使用されません。</string>
    <string name="privacy_gate_gemini_accept">同意して送信</string>
    <string name="privacy_gate_gemini_cancel">キャンセル</string>

    <string name="privacy_gate_tts_title">テキストをMicrosoftに送信しますか?</string>
    <string name="privacy_gate_tts_body">読み上げのため、テキストは暗号化されてMicrosoft Bing Speech(米国)に送信され、音声として返されます。法的根拠:EU-US Data Privacy Framework + 標準契約条項。\n\n代替案:Android標準のオフライン読み上げを使用してください。</string>
    <string name="privacy_gate_tts_accept">同意して読み上げ</string>
    <string name="privacy_gate_tts_cancel">キャンセル</string>

    <string name="settings_privacy_header">プライバシー</string>
    <string name="settings_analytics_title">匿名の統計情報</string>
    <string name="settings_analytics_subtitle">エラー分析と製品改善のためのFirebase Analytics</string>

    <string name="settings_delete_account_title">アカウントとデータの削除</string>
    <string name="settings_delete_account_subtitle">すべてのローカルデータ、Googleアカウント、Driveバックアップを元に戻せない形で削除します</string>
    <string name="settings_delete_account_confirm_title">アカウントを完全に削除しますか?</string>
    <string name="settings_delete_account_confirm_body">この操作は元に戻せず、以下を削除します:\n\n• すべてのローカル日記エントリー、写真、動画\n• Firebaseアカウント\n• アプリのGoogle Driveバックアップ\n\nアプリは新規インストール状態で再起動します。</string>
    <string name="settings_delete_account_cancel">キャンセル</string>
    <string name="settings_delete_account_confirm">はい、すべて削除</string>

    <string name="settings_report_ai_title">AI応答を報告</string>
    <string name="settings_report_ai_subtitle">不適切または誤ったAI出力</string>
    <string name="settings_report_ai_confirm_title">サポートへのメールを開きますか?</string>
    <string name="settings_report_ai_confirm_body">dev.app.support@gmail.com 宛ての下書きメールが開きます。送信前に説明を追記できます。営業日24時間以内に返信します。\n\nここで報告してください:ダッシュボード、要約、振り返り、テキスト改善からの不適切、不快、誤った、または誤解を招くAI出力。</string>
    <string name="settings_report_ai_confirm">報告を作成</string>
    <string name="settings_report_ai_cancel">キャンセル</string>
    <string name="settings_report_ai_no_email">メールアプリが見つかりません。dev.app.support@gmail.com に報告を送信してください。</string>
    <string name="settings_report_ai_subject">Best Journal:不適切なAI応答</string>
    <string name="settings_report_ai_body">こんにちは、\n\nBest Journalで不適切または誤ったAI応答を報告したいです。\n\n問題の説明:\n[記入してください]\n\nコンテキスト(どの機能、どの入力):\n[記入してください]\n\nありがとうございます。</string>

    <string name="settings_revoke_title">契約解除</string>
    <string name="settings_revoke_subtitle">プレミアム購入</string>
    <string name="settings_revoke_confirm_title">サポートへのメールを開きますか?</string>
    <string name="settings_revoke_confirm_body">dev.app.support@gmail.com 宛ての下書きメールが開きます。営業日24時間以内に返信します。\n\n契約解除権に関する詳細情報は、利用規約(§ 16)に記載されています。サブスクリプションはGoogle Play → 定期購入からもキャンセルしてください。</string>
    <string name="settings_revoke_cancel">キャンセル</string>
    <string name="settings_revoke_confirm">解除通知を作成</string>
    <string name="settings_revoke_no_email">メールアプリが見つかりません。dev.app.support@gmail.com に解除通知を送信してください。</string>
    <string name="settings_revoke_email_subject">Best Journal プレミアム契約解除</string>
    <string name="settings_revoke_email_body">こんにちは、\n\n本通知により、Best Journal のプレミアム機能に関する契約の解除を通知します。\n\n注文日:[記入してください]\nGoogleアカウントのメール:[送信者と異なる場合は記入してください]\n氏名:[記入してください]\n\n日付:[今日]</string>
"""


def insert(path, block):
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()
    if "CONSENT SCREEN + PRIVACY GATES + PRIVACY SECTION" in content:
        return f"SKIP: already present: {path}"
    new_content = content.replace("</resources>", block + "\n\n</resources>")
    d = os.path.dirname(os.path.abspath(path))
    with tempfile.NamedTemporaryFile("w", dir=d, suffix=".tmp", delete=False, encoding="utf-8", newline="\n") as tmp:
        tmp.write(new_content)
        tmp_path = tmp.name
    os.replace(tmp_path, path)
    return f"OK: {path}"


for locale, block in TRANSLATIONS.items():
    target = os.path.join(APP_DIR, f"values-{locale}", "strings.xml")
    print(insert(target, block))

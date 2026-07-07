# -*- coding: utf-8 -*-
"""finale Phase3 Translation-Worker pl (Polnisch) — apply script.
Schreibt NUR values-pl/strings.xml. rfind-Insert fuer neuen Key, Regex-Replace fuer Bestands-Keys.
Format-Args EXAKT wie aktuelle DE-Referenz: nur custom_intro/goals_rules/paywall_from_per_day/
paywall_monthly_note tragen %1$s. Die 6 Ex-Mismatch-Keys (4x dashboard_profile_focus,
custom_schema, rerank_system) tragen 0 Format-Args (alte literale %1$s verschwinden).
"""
import re, sys, io

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
PL = 'app/src/main/res/values-pl/strings.xml'  # relativ zu ~/proggs/BestJournalAndroid (cwd)

# --- Uebersetzungen: key -> XML-escapter String-Body (inkl. \n als Literal-Backslash-n) ---
# WICHTIG: In Python-Strings ist '\\n' das, was als literales \n in der XML steht (Android-Newline).
# Doppelte Anfuehrungszeichen im Text -> \" (XML/Android). Polnische Gaensefuesschen „ " bleiben.

S = {}

S['ai_prompt_custom_intro'] = (
    "Jesteś inteligentnym, uważnym analitykiem dziennika ORAZ wykonawcą zadań.\\n\\n"
    "METODA W DWÓCH KROKACH:\\n\\n"
    "KROK 1 — PRZYJMIJ KONTEKST:\\n"
    "Najpierw przeczytaj w całości WSZYSTKIE wpisy użytkownika. Zrozum sytuację, wzorce, tematy, "
    "wymienione rzeczy, problemy, pragnienia. Wpisy są Twoim KONTEKSTEM, Twoim punktem wyjścia, Twoją "
    "podstawą. Nie ograniczają Cię jednak.\\n\\n"
    "KROK 2 — WYKONAJ ZADANIE:\\n%1$s\\n\\n"
    "Powyższe to Twoje właściwe ZADANIE. Wykonaj je na bazie kontekstu z kroku 1. Jeśli zadanie wymaga "
    "researchu, pomysłów, alternatyw, sugestii lub nowych informacji, dostarcz je AKTYWNIE, nawet jeśli "
    "nie pojawiają się we wpisach. Nie podawaj przy tym ŻADNYCH diagnoz ani wiążących porad medycznych, "
    "prawnych czy finansowych; w takich tematach odsyłaj do specjalistów. Wpisy informują Twój wynik, "
    "nie ograniczają go.\\n\\n"
    "ZASADA KLUCZOWA — PROFIL STANOWI CO NAJMNIEJ 50 PROCENT:\\n"
    "Cały wynik (analiza ogólna, najważniejsze działania, kategorie, porady, postępy) MUSI w co najmniej "
    "50 procentach być wyraźnie powiązany z zadaniem użytkownika. Wnioski, punkty lub tematy, które NIE "
    "mają treściowego związku z zadaniem, pomiń — nie wypełniaj wyniku ogólnymi tematami życiowymi tylko "
    "po to, by JSON był pełny. Lepiej mniej punktów z jasnym powiązaniem niż wiele ogólnikowych.\\n\\n"
    "Ważne co do języka: wyrażaj powiązanie RÓŻNORODNIE. Używaj synonimów, pokrewnych pojęć, sąsiedztw "
    "tematycznych i parafraz. NIE powtarzaj mechanicznie dokładnych słów z zadania — to brzmi nachalnie. "
    "Przykład zadania „cele\\\": mów też o zamierzeniach, pragnieniach, ambicjach, o tym, co chcesz "
    "osiągnąć, o swoim kursie, krokach rozwoju. Przykład zadania „sport\\\": mów też o ruchu, treningu, "
    "aktywności fizycznej, wytrzymałości, kondycji, zdrowiu. Powiązanie z profilem musi być WYCZUWALNE, "
    "nie dosłowne.\\n\\n"
    "DECYDUJĄCE:\\n"
    "- Jeśli w zadaniu jest „analizuj\\\", „podsumuj\\\", „co rzuca się w oczy\\\" — trzymaj się blisko wpisów.\\n"
    "- Jeśli w zadaniu jest „zbadaj\\\", „znajdź alternatywy\\\", „zaproponuj\\\", „poleć\\\", „uzupełnij\\\", "
    "„co by było gdyby\\\", „nowe pomysły\\\" — wyjdź AKTYWNIE poza wpisy i wnieś własne, nowe treści.\\n"
    "- Zadanie ma pierwszeństwo. Wpisy są fundamentem, nie murem."
)

S['ai_prompt_custom_rules'] = (
    "ZASADA KONTEKSTU — KAŻDY WPIS JEST CZYTANY:\\n"
    "Otrzymujesz ponumerowane wpisy. Przeczytaj KAŻDY POJEDYNCZY w całości i przyjmij je jako kontekst. "
    "Każdy istotny szczegół wpływa na Twój wynik. Ale: wynik może i powinien wychodzić poza wpisy, jeśli "
    "zadanie tego wymaga.\\n\\n"
    "ZASADA ZADANIA — ZADANIE ZOSTAJE SPEŁNIONE:\\n"
    "Wykonaj zadanie użytkownika dosłownie. Jeśli zadanie prosi o alternatywy, dostarcz prawdziwe nowe "
    "alternatywy, nie tylko rzeczy z wpisów. Jeśli zadanie prosi o rekomendacje, zbadaj i poleć aktywnie. "
    "Wpisy NIE są jedynym źródłem Twojej odpowiedzi.\\n\\n"
    "ROZRÓŻNIENIE W WYNIKU:\\n"
    "Oznacz wyraźnie, co pochodzi z wpisów, a co dodajesz od siebie. Dzięki temu użytkownik wie, które "
    "myśli są jego własne, a co jest Twoim wkładem.\\n\\n"
    "ZASADA PORADNICTWA SPECJALISTYCZNEGO — ŻADNYCH DIAGNOZ:\\n"
    "NIEZALEŻNIE od zadania nie podajesz ŻADNYCH diagnoz medycznych, zaleceń terapeutycznych, porad "
    "prawnych ani finansowych. W takich tematach odsyłaj neutralnie do wykwalifikowanych specjalistów.\\n\\n"
    "ZASADY JĘZYKOWE:\\n"
    "- Prosto, jasno. Bez słów obcych.\\n"
    "- Empatycznie i bezpośrednio.\\n"
    "- Bez długich myślników (—). Używaj przecinków lub kropek.\\n\\n"
    "ZASADA ILOŚCI:\\n"
    "Co najmniej 10 wniosków łącznie — ale każdy pojedynczy musi treściowo pasować do zadania. Jeśli nie "
    "znajdziesz 10 wniosków z prawdziwym powiązaniem z profilem, dostarcz lepiej 7 mocnych niż 10 "
    "rozcieńczonych. Przy czysto analitycznych zadaniach wnioski pochodzą z wpisów, przy zadaniach "
    "kreatywnych możesz wnosić nowe treści."
)

# custom_schema: 0 Format-Args! Die alten %1$s entfallen.
S['ai_prompt_custom_schema'] = (
    "SCHEMAT WYJŚCIA JSON:\\n{\\n"
    "  \\\"ueberschrift_top5\\\": \\\"Kreatywny nagłówek pasujący do wyniku zadania, maks. 3 słowa\\\",\\n"
    "  \\\"ueberschrift_analyse\\\": \\\"Kreatywny nagłówek pasujący do wyniku zadania, maks. 3 słowa\\\",\\n"
    "  \\\"ueberschrift_ergebnisse\\\": \\\"Kreatywny nagłówek pasujący do wyniku zadania, maks. 3 słowa\\\",\\n"
    "  \\\"fokus_kern\\\": \\\"Jedno zdanie oddające istotę zadania własnymi słowami — jako widoczna kotwica dla pulpitu.\\\",\\n"
    "  \\\"fokus_zitate\\\": [\\\"...\\\", \\\"...\\\"],\\n"
    "  \\\"gesamt_entropie\\\": 0.0,\\n"
    "  \\\"trend\\\": \\\"rosnący|stabilny|malejący|nieznany\\\",\\n"
    "  \\\"gesamtanalyse\\\": \\\"...\\\",\\n"
    "  \\\"fortschritte\\\": [...],\\n"
    "  \\\"top_massnahmen\\\": [...],\\n"
    "  \\\"kategorien\\\": [...]\\n}\\n\\n"
    "1) \\\"ueberschrift_top5/analyse/ergebnisse\\\": OBOWIĄZKOWE. Kreatywne, konkretne, maks. 3 słowa. MUSI "
    "odzwierciedlać WYNIK zadania, nie tylko temat. ŻADNYCH ogólnych tytułów.\\n\\n"
    "2) \\\"fokus_kern\\\": OBOWIĄZKOWE. Pojedyncze zdanie (15–30 słów), które pokazuje użytkownikowi: "
    "„Zrozumiałem Twoje zadanie — TO jest myśl przewodnia.\\\" Nie cytat zadania, lecz własne sformułowanie "
    "Twoimi słowami. Wyświetla się u góry pulpitu i jest dowodem, że profil działa.\\n\\n"
    "3) \\\"fokus_zitate\\\": OBOWIĄZKOWE, od 3 do 5 sztuk. Krótkie, dosłownie lub niemal dosłownie wzięte "
    "fragmenty z wpisów dziennika, które bezpośrednio pasują do zadania. Format każdego: „[data] krótki "
    "cytat lub sparafrazowany fragment\\\". Jeśli przy zadaniach kreatywno-badawczych nie ma dosłownych "
    "trafień, weź 3–5 wpisów o największej bliskości tematycznej i opisz powiązanie w jednej linii. Jeśli "
    "mniej niż 3 wpisy pasują tematycznie, mimo to podaj przynajmniej te i ewentualnie dodaj wskazówkę "
    "„(bliskość tematyczna)\\\" — nigdy nie zostawiaj pustego.\\n\\n"
    "4) \\\"gesamt_entropie\\\" (0.0 do 1.0): jak silnie temat zadania jest reprezentowany we wpisach?\\n\\n"
    "5) \\\"trend\\\": tylko przy 3+ wpisach. Jak rozwija się temat zadania we wpisach?\\n\\n"
    "6) \\\"gesamtanalyse\\\" (15–25 zdań): dwie części wyraźnie rozpoznawalne. CO NAJMNIEJ 50 procent tekstu "
    "dotyczy bezpośrednio lub parafrazując zadania.\\n"
    "   Część A (kontekst z wpisów): co stoi we wpisach na ten temat? Nazwij istotne szczegóły.\\n"
    "   Część B (wynik zadania): jaka jest Twoja odpowiedź na zadanie? Co dostarczasz nowego, dodatkowo lub "
    "jako rekomendację?\\n"
    "   Połącz obie części, aby użytkownik widział myśl przewodnią.\\n\\n"
    "7) \\\"fortschritte\\\" (0–5): wzorce lub rozwój z wpisów, ważne dla zadania. Co najmniej połowa wpisów "
    "postępu musi wyraźnie należeć do zadania.\\n"
    "   { \\\"titel\\\": \\\"maks. 5 słów\\\", \\\"beschreibung\\\": \\\"2–3 zdania\\\", \\\"bezug\\\": \\\"1 zdanie\\\" }\\n\\n"
    "8) \\\"top_massnahmen\\\" (dokładnie 5): najważniejsze WYNIKI zadania. CO NAJMNIEJ 3 z 5 muszą jednoznacznie "
    "należeć do zadania. Mogą to być nowe propozycje, alternatywy, rekomendacje lub wnioski, których "
    "użytkownik NIE wspomniał we wpisach, jeśli zadanie tego wymaga. Przy czysto analitycznych zadaniach "
    "pochodzą z wpisów.\\n"
    "   {\\n"
    "     \\\"titel\\\": \\\"maks. 6 słów\\\",\\n"
    "     \\\"beschreibung\\\": \\\"13–21 słów, zwięźle na temat.\\\",\\n"
    "     \\\"erklaerung\\\": \\\"5–8 zdań szczegółowo. Jeśli treść jest nowa, uzasadnij, dlaczego pasuje do "
    "kontekstu użytkownika. Jeśli treść pochodzi z wpisów, podaj konkretne powiązanie.\\\"\\n"
    "   }\\n\\n"
    "9) \\\"kategorien\\\": grupy tematyczne strukturyzujące zadanie (dynamiczne). CO NAJMNIEJ połowa kategorii "
    "musi treściowo należeć do zadania. Zrezygnuj ze „standardowych tematów życiowych\\\" jak praca, sen, "
    "czas wolny, jeśli nie pasują do zadania.\\n"
    "   {\\n"
    "     \\\"name\\\": \\\"maks. 12 znaków\\\", \\\"icon\\\": \\\"material_icon_name\\\", \\\"farbe\\\": \\\"#HEX\\\",\\n"
    "     \\\"entropie_level\\\": 0.0,\\n"
    "     \\\"zusammenfassung\\\": \\\"3–5 zdań\\\",\\n"
    "     \\\"ratschlaege\\\": [{\\n"
    "       \\\"titel\\\": \\\"maks. 6 słów\\\",\\n"
    "       \\\"beschreibung\\\": \\\"13–21 słów\\\",\\n"
    "       \\\"prioritaet\\\": \\\"wysoki|średni|niski\\\",\\n"
    "       \\\"verknuepfung\\\": \\\"Powiązanie z innym tematem lub null\\\",\\n"
    "       \\\"herleitung\\\": [{\\\"datum\\\":\\\"...\\\",\\\"zusammenfassung\\\":\\\"1–2 zdania\\\"}]\\n"
    "     }]\\n"
    "   }\\n\\n"
    "WYWÓD — POCHODZENIE TREŚCI:\\n"
    "- Jeśli treść pochodzi z wpisu: ustaw „datum\\\" na datę wpisu, „zusammenfassung\\\" podaje konkretne "
    "powiązanie.\\n"
    "- Jeśli treść jest nowa (np. zbadana alternatywa, własna propozycja): ustaw „datum\\\" na „neu\\\", "
    "„zusammenfassung\\\" wyjaśnia w 1–2 zdaniach, dlaczego ta propozycja pasuje do kontekstu użytkownika."
)

S['ai_prompt_entropy_intro'] = (
    "Jesteś empatycznym, bardzo inteligentnym, wrażliwym analitykiem porządku i wzorców.\\n\\n"
    "TWOJE ZADANIE:\\n"
    "Przeanalizuj wpisy dziennika użytkownika. Znajdź powtarzające się źródła stresu, bałaganu i "
    "obciążenia. Utwórz z tego strukturalny panel rad w formacie JSON.\\n\\n"
    "CZEGO SZUKAMY — OBCIĄŻENIE OSOBISTE:\\n"
    "Wszystko, co w życiu użytkownika generuje bałagan, stres, utratę energii, przeciążenie, presję czasu, "
    "ciężar emocjonalny lub utratę kontroli.\\n\\n"
    "NIE podajesz żadnych porad medycznych, terapeutycznych ani diagnostycznych. W tematach zdrowotnych "
    "odsyłasz neutralnie do specjalistów."
)

S['ai_prompt_goals_rules'] = (
    "ZASADY JĘZYKOWE:\\n- %1$s\\n"
    "- Motywująco i uczciwie, świętuj postęp, nie koloryzuj niczego.\\n"
    "- Bez długich myślników (—). Używaj przecinków lub kropek.\\n"
    "- Przy celach zdrowotnych lub dotyczących ciała towarzyszysz tylko motywująco; NIE podajesz zaleceń "
    "dietetycznych, lekowych ani terapeutycznych.\\n\\n"
    "ZASADA ILOŚCI, KAŻDY CEL SIĘ LICZY:\\n"
    "Rozpoznaj WSZYSTKIE cele — także drobne. NIE łącz ich."
)

S['ai_prompt_insight_attitude'] = (
    "TWOJA POSTAWA:\\n"
    "Jesteś życzliwym lustrem. Uczciwie pokazujesz użytkownikowi, co widzisz w jego wpisach — ale zawsze "
    "z celem, by mógł z tego rosnąć. Każdy wgląd ma mu pomóc lepiej zrozumieć samego siebie. Także trudne "
    "wzorce nazywasz jasno, ale konstruktywnie i bez zarzutu. Skupienie: czego użytkownik może nauczyć się "
    "o sobie z własnych słów?\\n\\n"
    "NIE stawiasz żadnych diagnoz psychologicznych i nie nazywasz zaburzeń ani chorób. Opisujesz tylko "
    "obserwowalne wzorce z własnych słów użytkownika.\\n\\n"
    "CZEGO SZUKASZ:\\n"
    "- Powtarzające się uczucia: jakie emocje wracają wciąż na nowo?\\n"
    "- Wzorce myślenia: jak użytkownik myśli o sobie, innych, świecie?\\n"
    "- Wzorce unikania: czego użytkownik omija? O czym nigdy nie pisze?\\n"
    "- Mocne strony: co użytkownik robi dobrze, nawet jeśli sam tego nie widzi?\\n"
    "- Wartości: co jest dla użytkownika naprawdę ważne (widać po działaniu, nie po słowach)?\\n"
    "- Wyzwalacze: co wywołuje silne reakcje — pozytywne i negatywne?\\n"
    "- Sprzeczności: czy użytkownik mówi jedno, a działa inaczej?\\n"
    "- Potrzeby: czego użytkownik potrzebuje, co przebija między wierszami?\\n"
    "- Wzrost: gdzie zmieniło się spojrzenie użytkownika?"
)

S['ai_prompt_no_dates_rule'] = (
    "PRZYJAZNE DLA CZYTANIA NA GŁOS (OBOWIĄZKOWE — ZAKAZ DAT W WIDOCZNYCH TEKSTACH):\\n"
    "We WSZYSTKICH widocznych polach tekstowych (beschreibung, erklaerung, zusammenfassung, gesamtanalyse) "
    "NIE WOLNO Ci używać dat — ani „3.4.\\\", „23.04.\\\", „28.4.\\\", „24.04.\\\", „30.4.\\\", ani „wpis z 3 "
    "kwietnia\\\", „w dniu 23.04.\\\", „z 28.4.\\\" czy podobnych odniesień do konkretnych dat wpisów. Te teksty "
    "są czytane użytkownikowi na głos — ciągi dat brzmią nienaturalnie i przerywają płynność wglądu.\\n\\n"
    "ZAMIAST TEGO SFORMUŁUJ CZYSTY WGLĄD:\\n"
    "- ŹLE: „Wpisy z 3.4., 28.4. i 23.4. dowodzą, że Twój biorytm cierpi przez późne godziny snu.\\\"\\n"
    "- DOBRZE: „Późne godziny snu systematycznie niszczą Twój biorytm.\\\"\\n"
    "- ŹLE: „Wpis z 30. pokazuje, że Twoja skrzynka od 28.4. się przepełnia i kradnie Ci czas.\\\"\\n"
    "- DOBRZE: „Twoja skrzynka od dni się przepełnia i kradnie Ci czas.\\\"\\n"
    "- ŹLE: „Więcej przeglądu w codzienności czujesz od akcji porządkowych z 24.04.\\\"\\n"
    "- DOBRZE: „Ruch daje Ci wyczuwalnie więcej przeglądu w codzienności.\\\"\\n\\n"
    "Treściowy wgląd zostaje — odpadają tylko konkretne odniesienia do dat. Nadal możesz używać ogólnych "
    "odniesień czasowych („powracający\\\", „ostatnio\\\", „przez kilka dni\\\"). Daty należą TYLKO do pola "
    "„herleitung\\\" (proweniencja, używane wewnętrznie, niewyświetlane)."
)

S['ai_prompt_rerank_actions_header'] = "=== AKTUALNA TABLICA NAJWAŻNIEJSZYCH DZIAŁAŃ (5 wpisów) ==="
S['ai_prompt_rerank_entries_header'] = "=== WPISY DZIENNIKA (źródło mocnych profilowo punktów zastępczych) ==="
S['ai_prompt_rerank_user_focus_header'] = "=== FOKUS PROFILU UŻYTKOWNIKA ==="

# rerank_system: 0 Format-Args! Voll-Neuuebersetzung der aktuellen DE (das alte (%1$s) entfaellt).
S['ai_prompt_rerank_system'] = (
    "Jesteś re-rankerem profilowym. Otrzymujesz tablicę JSON z 5 najważniejszymi działaniami z analizy "
    "pulpitu, fokus profilu użytkownika oraz oryginalne wpisy dziennika.\\n\\n"
    "Twoje zadanie w 3 krokach:\\n"
    "1) Posortuj 5 działań tak, aby te o najwyraźniejszym powiązaniu z fokusem profilu były pierwsze.\\n"
    "2) Sprawdź, czy do 2 z 5 działań NIE ma treściowego powiązania z fokusem profilu. Jeśli tak, zastąp je "
    "mocniejszymi profilowo działaniami, które wyprowadzisz z wpisów dziennika. Trzymaj się tej samej "
    "struktury JSON każdego działania (titel, beschreibung, erklaerung).\\n"
    "3) Wyrażaj powiązanie z profilem RÓŻNORODNIE, używając synonimów i sąsiedztw tematycznych, NIE "
    "mechanicznego powtarzania słów profilu. Powiązanie musi być wyczuwalne, nie dosłowne.\\n\\n"
    "WAŻNE:\\n"
    "- Na końcu zwróć DOKŁADNIE 5 wpisów.\\n"
    "- Zmiany tylko, gdy brakuje prawdziwego powiązania z profilem — przy już dobrym dopasowaniu tylko "
    "sortuj tablicę, nic nie zastępuj.\\n"
    "- Zachowaj strukturę JSON każdego wpisu (te same klucze: titel, beschreibung, erklaerung).\\n"
    "- Język polski, bez myślników, beschreibung 13-21 słów.\\n"
    "- Nie podawaj ŻADNYCH diagnoz ani wiążących porad medycznych, prawnych czy finansowych; w takich "
    "tematach odsyłaj neutralnie do specjalistów.\\n\\n"
    "PRZYJAZNE DLA CZYTANIA NA GŁOS (OBOWIĄZKOWE — ZAKAZ DAT):\\n"
    "W beschreibung i erklaerung NIE używaj dat („3.4.\\\", „23.04.\\\", „wpis z 30.\\\"). Te teksty są czytane "
    "na głos — ciągi dat brzmią nienaturalnie. Sformułuj czysty wgląd bez konkretnych dat wpisów. "
    "Przykłady:\\n"
    "- ŹLE: „Wpisy z 3.4. i 23.04. dowodzą, że Twój biorytm cierpi.\\\"\\n"
    "- DOBRZE: „Późne godziny snu systematycznie niszczą Twój biorytm.\\\"\\n"
    "Ogólne odniesienia czasowe są dozwolone („powracający\\\", „ostatnio\\\", „przez kilka dni\\\"), daty nie.\\n\\n"
    "FORMAT WYJŚCIA:\\n"
    "- TYLKO tablica JSON (nawiasy kwadratowe na poziomie głównym).\\n"
    "- Bez backticków Markdown, bez tekstu przed/po.\\n"
    "- Zacznij bezpośrednio od [ i zakończ na ]."
)

S['churn_offer_feature_perspectives'] = "Wszystkie 4 predefiniowane profile AI + tyle własnych, ile potrzebujesz"
S['consent_card3_title'] = "Pseudonimowe statystyki"
S['consent_toggle_analytics_body'] = (
    "Firebase Analytics — pomaga nam ulepszać aplikację. Żadnych treści dziennika, tylko pseudonimowe "
    "statystyki użytkowania (np. liczby kliknięć). Transfer danych: Google USA (Data Privacy Framework)."
)
S['consent_toggle_analytics_title'] = "Pseudonimowa analiza użytkowania"
S['consent_toggle_do_not_sell_body'] = (
    "Ten przełącznik jest przeznaczony tylko dla mieszkańców amerykańskiego stanu Kalifornia. Kalifornijska "
    "ustawa o ochronie danych (CCPA/CPRA) przyznaje mieszkańcom prawo do sprzeciwu. Po aktywacji wszystkie "
    "opcjonalne funkcje chmurowe (AI, kopia zapasowa, rozpoznawanie mowy, analiza) zostają wyłączone — "
    "aktywne pozostają tylko funkcje lokalne.\\n\\n"
    "Jeśli mieszkasz poza Kalifornią, możesz zignorować ten przełącznik: Best Journal nigdzie na świecie "
    "nie sprzedaje ani nie wprowadza do obrotu Twoich danych."
)

# 4x dashboard_profile_focus_*: 0 Format-Args! Die alten %1$s entfallen.
S['dashboard_profile_focus_entropy'] = (
    "Porządkuje Twoje życie: rozpoznaje stres, bałagan i obciążenie oraz pokazuje, co możesz odpuścić lub "
    "wyjaśnić."
)
S['dashboard_profile_focus_goals'] = "Motywujący towarzysz celów z widokiem postępów."
S['dashboard_profile_focus_insight'] = "Głębokie samopoznanie: wzorce myślenia i ukryte mocne strony."
S['dashboard_profile_focus_summary'] = "Narracyjne podsumowanie najważniejszych wydarzeń."

S['onboarding_feature_secure'] = (
    "Twoje wpisy zostają na urządzeniu; zapytania AI są przesyłane w postaci zaszyfrowanej"
)
S['onboarding_premium_feature_noads'] = "Pisz i refleksjuj bez zakłóceń"
S['paywall_feature_noads'] = "Pisz i refleksjuj bez zakłóceń"
S['paywall_feature_profiles'] = "Własne profile AI bez limitu liczby"
S['paywall_from_per_day'] = "Abonament roczny, %1$s rocznie"
S['paywall_headline_clarity_sub'] = "AI uwidacznia powracające wzorce w Twoich wpisach"
S['paywall_monthly_note'] = "Następnie %1$s miesięcznie, odnawia się automatycznie\\nMożesz anulować w każdej chwili"

S['privacy_gate_groq_body'] = (
    "Ta funkcja wysyła Twoje nagranie głosu w postaci zaszyfrowanej do Groq (USA) i zamienia je tam na "
    "tekst — szybciej i dokładniej niż rozpoznawanie lokalne.\\n\\n"
    "✓ Nagranie jest usuwane od razu po zamianie\\n"
    "✓ Brak trenowania AI na Twoich nagraniach\\n"
    "✓ Podstawa prawna: standardowe klauzule umowne (EU SCCs)\\n\\n"
    "Alternatywa: lokalne rozpoznawanie offline w ustawieniach."
)
S['privacy_gate_tts_body'] = (
    "Z głosami Premium czytanie na głos brzmi naturalniej. Twój tekst jest wysyłany w postaci zaszyfrowanej "
    "(HTTPS/TLS) do usługi mowy Microsoft w USA i zwracany jako audio.\\n\\n"
    "✓ Przesył wyłącznie w celu syntezy mowy\\n"
    "✓ Przekazanie do państwa trzeciego USA (Microsoft jest certyfikowany w ramach EU-US Data Privacy "
    "Framework)"
)
S['privacy_gate_tts_title'] = "Włącz głosy Premium"
S['profile_entropy_long'] = (
    "AI znajduje to, co obciąża Cię w codzienności, i porządkuje to. Otrzymujesz przegląd swoich "
    "największych źródeł obciążenia i 5 konkretnych kroków do uporządkowania. Idealnie, gdy chcesz "
    "odzyskać przegląd."
)
S['retro_benefit_weekly'] = (
    "Wszystkie podsumowania tygodniowe zamiast tylko pierwszych 2 — w ramach Twojego dziennego limitu AI"
)
S['settings_analytics_title'] = "Pseudonimowa analiza użytkowania"
S['settings_delete_account_confirm_body'] = (
    "Ta operacja jest nieodwracalna i usuwa:\\n\\n"
    "• Wszystkie lokalne wpisy dziennika, zdjęcia, filmy i nagrania audio\\n"
    "• Kopię zapasową aplikacji w Google Drive (tylko dane aplikacji, nie Twoje konto Google)\\n"
    "• Twoje logowanie do aplikacji\\n\\n"
    "Samo Twoje konto Google pozostaje. Aplikacja uruchomi się następnie od nowa jako świeża instalacja."
)
S['settings_delete_account_subtitle'] = (
    "Nieodwracalnie usuwa wszystkie lokalne dane i kopię zapasową Drive. Samo Twoje konto Google pozostaje."
)
S['settings_feedback_dialog_desc'] = (
    "Twoja wiadomość do deweloperów — czytamy każdą wiadomość i odzywamy się tak szybko, jak to możliwe."
)
S['settings_premium_feature_5_perspectives'] = "Wszystkie 4 predefiniowane profile AI + tyle własnych, ile potrzebujesz"
S['settings_premium_feature_profiles'] = "Własne profile AI bez limitu liczby"
S['settings_report_ai_confirm_body'] = (
    "Otworzy się Twój program pocztowy z przygotowaną wiadomością do dev.app.support@gmail.com. Możesz "
    "jeszcze uzupełnić opis przed wysłaniem. Zwykle odpowiadamy w ciągu 24 godzin w dni robocze.\\n\\n"
    "Zgłaszaj w ten sposób: nieodpowiednie, obraźliwe, nieprawdziwe lub wprowadzające w błąd wyniki AI z "
    "pulpitu, podsumowań, retrospekcji lub poprawy tekstu."
)
S['settings_revoke_confirm_body'] = (
    "Klikając „Potwierdź odstąpienie\\\", wysyłamy Twoje odstąpienie bezpośrednio do dev.app.support@gmail.com. "
    "Od razu otrzymasz potwierdzenie wpłynięcia e-mailem.\\n\\n"
    "Pełne pouczenie o prawie odstąpienia od umowy znajdziesz w warunkach użytkowania (§ 16). Abonamenty "
    "anuluj dodatkowo w Google Play → Subskrypcje, aby nie dochodziło do dalszych rozliczeń."
)
S['settings_revoke_confirm_title'] = "Odstąpić od umowy?"
S['settings_revoke_subtitle'] = "§ 356a BGB (niem. KC) — odstąpienie bezpośrednio przez aplikację"

# --- Plurals (CLDR: one/few/many/other) ---
PLURALS = {
    'datetime_months_relative': {
        'one':   '%1$d miesiąc temu',
        'few':   '%1$d miesiące temu',
        'many':  '%1$d miesięcy temu',
        'other': '%1$d miesiąca temu',
    },
    'datetime_years_relative': {
        'one':   '%1$d rok temu',
        'few':   '%1$d lata temu',
        'many':  '%1$d lat temu',
        'other': '%1$d roku temu',
    },
}


def build_string_xml(key, body):
    return f'    <string name="{key}">{body}</string>'


def build_plural_xml(key, forms):
    lines = [f'    <plurals name="{key}">']
    for q in ('one', 'few', 'many', 'other'):
        lines.append(f'        <item quantity="{q}">{forms[q]}</item>')
    lines.append('    </plurals>')
    return '\n'.join(lines)


def main():
    with open(PL, 'r', encoding='utf-8') as f:
        content = f.read()

    applied = []
    inserted = []

    # 1) Bestands-Strings ersetzen (regex auf gesamtes <string name="key">...</string>)
    for key, body in S.items():
        pat = re.compile(r'<string name="' + re.escape(key) + r'">.*?</string>', re.DOTALL)
        new_line = build_string_xml(key, body)
        if pat.search(content):
            content = pat.sub(lambda m: new_line, content, count=1)
            applied.append(key)
        else:
            # NEU: vor </resources> einfuegen (rfind)
            idx = content.rfind('</resources>')
            if idx == -1:
                raise RuntimeError(f'{PL}: kein </resources> gefunden')
            content = content[:idx] + new_line + '\n' + content[idx:]
            inserted.append(key)

    # 2) Plurals ersetzen
    for key, forms in PLURALS.items():
        pat = re.compile(r'<plurals name="' + re.escape(key) + r'">.*?</plurals>', re.DOTALL)
        new_block = build_plural_xml(key, forms)
        if pat.search(content):
            content = pat.sub(lambda m: new_block, content, count=1)
            applied.append(key)
        else:
            idx = content.rfind('</resources>')
            content = content[:idx] + new_block + '\n' + content[idx:]
            inserted.append(key)

    with open(PL, 'w', encoding='utf-8', newline='\n') as f:
        f.write(content)

    print('APPLIED (replaced):', len(applied))
    print('INSERTED (new):', inserted)


if __name__ == '__main__':
    main()

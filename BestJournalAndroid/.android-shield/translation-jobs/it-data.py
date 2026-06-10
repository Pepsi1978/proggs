# -*- coding: utf-8 -*-
# Italian translations for BestJournalAndroid — finale Phase 3 tw-it
# tu-form (informal), apostrophes escaped \', placeholders exact, legal 1:1.
# NOTE: values below are the FINAL XML inner text (apostrophes already \'-escaped,
# \n as literal backslash-n which is the Android newline escape).

STRINGS = {

"ai_prompt_custom_intro":
"Sei un analista di diario intelligente e attento E un esecutore di compiti.\\n\\nMETODO DI LAVORO IN DUE FASI:\\n\\nFASE 1 — ACQUISIRE IL CONTESTO:\\nLeggi prima TUTTE le voci di diario dell\\'utente per intero. Comprendi la situazione, gli schemi, i temi, le cose menzionate, i problemi, i desideri. Le voci sono il tuo CONTESTO, il tuo punto di partenza, la tua base. Ma non ti limitano.\\n\\nFASE 2 — ESEGUIRE IL COMPITO:\\n%1$s\\n\\nQuello sopra è il tuo vero INCARICO. Esegui questo incarico sulla base del contesto della Fase 1. Se l\\'incarico richiede ricerca, idee, alternative, proposte o nuove informazioni, allora forniscile ATTIVAMENTE, anche se non compaiono nelle voci. Non dare però diagnosi o consigli vincolanti medici, legali o finanziari; per questi temi rimanda a professionisti qualificati. Le voci informano il tuo output, non lo limitano.\\n\\nREGOLA FONDAMENTALE — IL PROFILO PESA ALMENO IL 50 PER CENTO:\\nL\\'intero output (analisi complessiva, misure principali, categorie, consigli, progressi) DEVE essere collegato per almeno il 50 per cento in modo chiaramente riconoscibile all\\'incarico dell\\'utente. Lascia perdere intuizioni, punti o temi che NON hanno alcun legame di contenuto con l\\'incarico — non riempire l\\'output con temi di vita generici solo per riempire il JSON. Meglio pochi punti con un legame chiaro che molti generici.\\n\\nImportante sulla lingua: esprimi il legame in modo VARIO. Usa sinonimi, termini affini, vicinanze tematiche e parafrasi. NON ripetere meccanicamente le parole esatte dell\\'incarico — risulta invadente. Esempio incarico \\\"Obiettivi\\\": parla anche di propositi, desideri, ambizioni, di ciò che vuoi raggiungere, della tua rotta, dei passi di sviluppo. Esempio incarico \\\"Sport\\\": parla anche di movimento, allenamento, attività fisica, resistenza, fitness, salute. Il legame col profilo deve essere PERCEPIBILE, non letterale.\\n\\nDECISIVO:\\n- Se l\\'incarico dice \\\"analizza\\\", \\\"riassumi\\\", \\\"cosa si nota\\\" — resta vicino alle voci.\\n- Se l\\'incarico dice \\\"ricerca\\\", \\\"trova alternative\\\", \\\"proponi\\\", \\\"consiglia\\\", \\\"integra\\\", \\\"cosa accadrebbe se\\\", \\\"nuove idee\\\" — vai ATTIVAMENTE oltre le voci e porta contenuti tuoi, nuovi.\\n- L\\'incarico ha la priorità. Le voci sono le fondamenta, non il muro.",

"ai_prompt_custom_rules":
"REGOLA DI CONTESTO — OGNI VOCE VIENE LETTA:\\nRicevi voci numerate. Leggi OGNI SINGOLA voce per intero e acquisiscila come contesto. Ogni dettaglio rilevante confluisce nel tuo output. Ma: l\\'output può e deve andare oltre le voci, se l\\'incarico lo richiede.\\n\\nREGOLA DELL\\'INCARICO — L\\'INCARICO VIENE SODDISFATTO:\\nEsegui l\\'incarico dell\\'utente alla lettera. Se l\\'incarico chiede alternative, fornisci vere alternative nuove, non solo cose tratte dalle voci. Se l\\'incarico chiede raccomandazioni, ricerca e consiglia attivamente. Le voci NON sono l\\'unica fonte della tua risposta.\\n\\nDISTINZIONE NELL\\'OUTPUT:\\nIndica chiaramente cosa proviene dalle voci e cosa aggiungi di nuovo. Così l\\'utente sa quali sono i suoi pensieri e qual è il tuo contributo.\\n\\nREGOLA DELLA CONSULENZA SPECIALISTICA — NESSUNA DIAGNOSI:\\nINDIPENDENTEMENTE dall\\'incarico non dai diagnosi mediche, raccomandazioni terapeutiche, consulenza legale o finanziaria. Per questi temi rimanda in modo neutrale a professionisti qualificati.\\n\\nREGOLE LINGUISTICHE:\\n- Semplice, chiaro. Niente parole straniere.\\n- Empatico e diretto.\\n- Niente trattini lunghi (—). Usa virgole o punti.\\n\\nREGOLA SULLA QUANTITÀ:\\nAlmeno 10 intuizioni in totale — ma ognuna deve essere pertinente all\\'incarico per contenuto. Se non trovi 10 intuizioni con un vero legame col profilo, fornisci piuttosto 7 forti invece di 10 annacquate. Negli incarichi di pura analisi le intuizioni provengono dalle voci, negli incarichi creativi puoi introdurre contenuti nuovi.",

"ai_prompt_entropy_intro":
"Sei un analista di ordine e schemi empatico, molto intelligente e sensibile.\\n\\nIL TUO COMPITO:\\nAnalizza le voci di diario di un utente. Trova fonti ricorrenti di stress, disordine e peso. Crea da esse una dashboard di consigli strutturata in formato JSON.\\n\\nCOSA CERCHIAMO — IL PESO PERSONALE:\\nTutto ciò che genera disordine, stress, perdita di energia, sovraccarico, pressione del tempo, peso emotivo o perdita di controllo nella vita dell\\'utente.\\n\\nNon dai consigli medici, terapeutici o diagnostici. Per i temi di salute rimandi in modo neutrale a professionisti qualificati.",

"ai_prompt_goals_rules":
"REGOLE LINGUISTICHE:\\n- %1$s\\n- Motivante e sincero, festeggia i progressi, non abbellire nulla.\\n- Niente trattini lunghi (—). Usa virgole o punti.\\n- Per obiettivi di salute o legati al corpo accompagni solo in modo motivante; NON dare raccomandazioni su diete, farmaci o terapie.\\n\\nREGOLA SULLA QUANTITÀ, OGNI OBIETTIVO CONTA:\\nRiconosci TUTTI gli obiettivi — anche quelli piccoli. NON riassumere.",

"ai_prompt_insight_attitude":
"IL TUO ATTEGGIAMENTO:\\nSei uno specchio benevolo. Mostri all\\'utente con sincerità ciò che riconosci nelle sue voci — ma sempre con l\\'obiettivo che possa crescere da esse. Ogni intuizione deve aiutarlo a capirsi meglio. Anche gli schemi difficili li nomini con chiarezza, ma in modo costruttivo e senza rimprovero. Punto focale: cosa può imparare l\\'utente su di sé dalle proprie parole?\\n\\nNon fai diagnosi psicologiche e non nomini disturbi o malattie. Descrivi solo schemi osservabili dalle parole stesse dell\\'utente.\\n\\nCOSA CERCHI:\\n- Emozioni ricorrenti: quali emozioni riaffiorano di continuo?\\n- Schemi di pensiero: come pensa l\\'utente di sé, degli altri, del mondo?\\n- Schemi di evitamento: cosa evita l\\'utente? Di cosa non scrive mai?\\n- Punti di forza: cosa fa bene l\\'utente, anche se non se ne accorge?\\n- Valori: cosa è davvero importante per l\\'utente (si mostra dalle azioni, non dalle parole)?\\n- Fattori scatenanti: cosa innesca reazioni forti — sia positive che negative?\\n- Contraddizioni: l\\'utente dice una cosa ma agisce diversamente?\\n- Bisogni: di cosa ha bisogno l\\'utente, che traspare tra le righe?\\n- Crescita: dove è cambiata la prospettiva dell\\'utente?",

"ai_prompt_no_dates_rule":
"ADATTO ALLA LETTURA TTS (OBBLIGATORIO — DIVIETO DI DATE NEI TESTI VISIBILI):\\nIn TUTTI i campi di testo visibili (descrizione, spiegazione, riassunto, analisi complessiva) NON PUOI usare date — né \\\"3.4.\\\", \\\"23.04.\\\", \\\"28.4.\\\", \\\"24.04.\\\", \\\"30.4.\\\" né \\\"voce del 3 aprile\\\", \\\"il 23.04.\\\", \\\"del 28.4.\\\" o riferimenti simili a date di voci concrete. Questi testi vengono letti all\\'utente — le sequenze di date lette suonano innaturali e spezzano il flusso delle intuizioni.\\n\\nFORMULA INVECE LA PURA INTUIZIONE:\\n- SBAGLIATO: \\\"Le voci del 3.4., 28.4. e 23.4. dimostrano che il tuo bioritmo soffre per gli orari di sonno tardivi.\\\"\\n- GIUSTO: \\\"Gli orari di sonno tardivi distruggono sistematicamente il tuo bioritmo.\\\"\\n- SBAGLIATO: \\\"La voce del 30 mostra che la tua casella di posta dal 28.4. trabocca e ti ruba tempo.\\\"\\n- GIUSTO: \\\"La tua casella di posta trabocca da giorni e ti ruba tempo.\\\"\\n- SBAGLIATO: \\\"Più visione d\\'insieme nella quotidianità la senti dalle azioni di riordino del 24.04.\\\"\\n- GIUSTO: \\\"Il movimento ti dà sensibilmente più visione d\\'insieme nella quotidianità.\\\"\\n\\nL\\'intuizione di contenuto resta — cadono solo i riferimenti a date concrete. Puoi continuare a usare riferimenti temporali generali (\\\"ricorrente\\\", \\\"di recente\\\", \\\"per più giorni\\\"). Le date vanno SOLO nel campo \\\"herleitung\\\" (provenienza, usato internamente, non mostrato).",

"ai_prompt_rerank_system":
"Sei un re-ranker di profilo. Ricevi un array JSON di 5 misure principali da un\\'analisi della dashboard, il focus di profilo dell\\'utente e le voci di diario originali.\\n\\nIl tuo compito in 3 passi:\\n1) Ordina le 5 misure in modo che vengano per prime quelle col legame più chiaro al focus di profilo.\\n2) Verifica se fino a 2 delle 5 misure NON hanno alcun legame di contenuto col focus di profilo. Se sì, sostituiscile con misure più forti per il profilo, che ricavi dalle voci di diario. Mantieni la stessa struttura JSON di ogni misura (titel, beschreibung, erklaerung).\\n3) Esprimi il legame col profilo in modo VARIO con sinonimi e vicinanze tematiche, NON con la ripetizione meccanica delle parole del profilo. Il legame deve essere percepibile, non letterale.\\n\\nIMPORTANTE:\\n- Restituisci alla fine ESATTAMENTE 5 voci.\\n- Modifiche solo se manca un vero legame col profilo — se la corrispondenza è già buona, ordina solo l\\'array, non sostituire nulla.\\n- Mantieni la struttura JSON di ogni voce (stesse chiavi: titel, beschreibung, erklaerung).\\n- Lingua italiana, niente trattini lunghi, beschreibung 13-21 parole.\\n- NON dare diagnosi o consigli vincolanti medici, legali o finanziari; per questi temi rimanda in modo neutrale a professionisti qualificati.\\n\\nADATTO ALLA LETTURA TTS (OBBLIGATORIO — DIVIETO DI DATE):\\nIn beschreibung e erklaerung non usare date (\\\"3.4.\\\", \\\"23.04.\\\", \\\"voce del 30.\\\"). Questi testi vengono letti — le sequenze di date suonano innaturali. Formula la pura intuizione senza date di voci concrete. Esempi:\\n- SBAGLIATO: \\\"Le voci del 3.4. e 23.04. dimostrano che il tuo bioritmo soffre.\\\"\\n- GIUSTO: \\\"Gli orari di sonno tardivi distruggono sistematicamente il tuo bioritmo.\\\"\\nI riferimenti temporali generali sono consentiti (\\\"ricorrente\\\", \\\"di recente\\\", \\\"per più giorni\\\"), le date no.\\n\\nFORMATO DI USCITA:\\n- SOLO un array JSON (parentesi quadre al livello radice).\\n- Niente backtick Markdown, nessun testo prima/dopo.\\n- Inizia direttamente con [ e termina con ].",

"churn_offer_feature_perspectives":
"Tutti e 4 i profili IA predefiniti + quanti ne vuoi di tuoi",

"consent_card3_title":
"Statistiche pseudonime",

"consent_toggle_analytics_body":
"Firebase Analytics — ci aiuta a migliorare l\\'app. Nessun contenuto del diario, solo statistiche di utilizzo pseudonime (ad es. numero di clic). Trasferimento dati: Google USA (Data Privacy Framework).",

"consent_toggle_analytics_title":
"Analisi d\\'uso pseudonima",

"consent_toggle_do_not_sell_body":
"Questo interruttore è pensato solo per i residenti dello Stato USA della California. La legge sulla privacy della California (CCPA/CPRA) riconosce ai residenti un diritto di opposizione. Se attivato, tutte le funzioni cloud opzionali (IA, backup, riconoscimento vocale, analisi) vengono disattivate — restano attive solo le funzioni locali.\\n\\nSe vivi fuori dalla California, puoi ignorare questo interruttore: Best Journal non vende né commercializza i tuoi dati in nessuna parte del mondo.",

"onboarding_feature_secure":
"Le tue voci restano sul dispositivo; le richieste IA vengono trasmesse in modo cifrato",

"onboarding_premium_feature_noads":
"Scrivi e rifletti senza disturbi",

"paywall_feature_noads":
"Scrivi e rifletti senza disturbi",

"paywall_feature_profiles":
"Profili IA personali senza limite di numero",

"paywall_from_per_day":
"Abbonamento annuale, %1$s all\\'anno",

"paywall_headline_clarity_sub":
"L\\'IA rende visibili gli schemi ricorrenti nelle tue voci",

"paywall_monthly_note":
"Poi %1$s al mese, si rinnova automaticamente\\nDisdicibile in qualsiasi momento",

"privacy_gate_groq_body":
"Questa funzione invia la tua registrazione vocale in modo cifrato a Groq (USA) e lì la converte in testo — più veloce e precisa del riconoscimento locale.\\n\\n✓ La registrazione viene cancellata subito dopo la conversione\\n✓ Nessun addestramento IA con le tue registrazioni\\n✓ Base giuridica: clausole contrattuali standard (SCC UE)\\n\\nAlternativa: riconoscimento locale offline nelle impostazioni.",

"privacy_gate_tts_body":
"Con le voci premium la lettura suona più naturale. Il tuo testo viene inviato in modo cifrato (HTTPS/TLS) a un servizio vocale Microsoft negli USA e restituito come audio.\\n\\n✓ Trasmissione esclusivamente per la sintesi vocale\\n✓ Trasferimento verso un Paese terzo USA (Microsoft è certificata secondo l\\'EU-US Data Privacy Framework)",

"privacy_gate_tts_title":
"Attiva le voci premium",

"profile_entropy_long":
"L\\'IA trova ciò che ti pesa nella quotidianità e lo mette in ordine. Ottieni una panoramica delle tue maggiori fonti di peso e 5 passi concreti di riordino. Ideale se vuoi recuperare la visione d\\'insieme.",

"retro_benefit_weekly":
"Tutti i resoconti settimanali invece dei soli primi 2 – nei limiti del tuo contingente IA giornaliero",

"settings_analytics_title":
"Analisi d\\'uso pseudonima",

"settings_delete_account_confirm_body":
"Questa operazione è irreversibile e cancella:\\n\\n• Tutte le voci di diario locali, foto, video e registrazioni audio\\n• Il backup su Google Drive dell\\'app (solo i dati dell\\'app, non il tuo account Google)\\n• Il tuo accesso all\\'app\\n\\nIl tuo account Google rimane. L\\'app poi riparte come una nuova installazione.",

"settings_delete_account_subtitle":
"Rimuove in modo irreversibile tutti i dati locali e il backup su Drive. Il tuo account Google rimane.",

"settings_feedback_dialog_desc":
"Il tuo messaggio agli sviluppatori — leggiamo ogni messaggio e ti rispondiamo il prima possibile.",

"settings_premium_feature_5_perspectives":
"Tutti e 4 i profili IA predefiniti + quanti ne vuoi di tuoi",

"settings_premium_feature_profiles":
"Profili IA personali senza limite di numero",

"settings_report_ai_confirm_body":
"Si apre il tuo programma di posta con un messaggio già pronto per dev.app.support@gmail.com. Puoi ancora integrare la descrizione prima di inviare. Di norma rispondiamo entro 24 ore nei giorni lavorativi.\\n\\nSegnala così: output IA inappropriati, offensivi, falsi o fuorvianti da dashboard, riassunti, resoconti o dal miglioramento del testo.",

"settings_revoke_confirm_body":
"Con un clic su «Conferma il recesso» inviamo il tuo recesso direttamente a dev.app.support@gmail.com. Ricevi subito una conferma di ricezione via e-mail.\\n\\nTrovi le istruzioni complete sul recesso nelle Condizioni d\\'uso (§ 16). Per gli abbonamenti disdici inoltre tramite Google Play → Abbonamenti, così non avvengono ulteriori addebiti.",

"settings_revoke_confirm_title":
"Recedere dal contratto?",

"settings_revoke_subtitle":
"§ 356a BGB — recesso direttamente dall\\'app",

}

PLURALS = {
"datetime_months_relative": {"one": "%1$d mese fa", "other": "%1$d mesi fa"},
"datetime_years_relative":  {"one": "%1$d anno fa", "other": "%1$d anni fa"},
}

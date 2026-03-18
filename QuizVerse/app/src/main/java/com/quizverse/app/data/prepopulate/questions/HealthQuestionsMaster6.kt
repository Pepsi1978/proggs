package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun healthQuestionsMaster6(): List<Question> = listOf(

    // --- NOBELPREISE IN MEDIZIN ---

    // Question 1 - Yamanaka-Faktoren
    Question(
        categoryId = 16,
        questionText = "Shinya Yamanaka erhielt 2012 den Nobelpreis fuer die Entdeckung, dass reife Zellen in pluripotente Stammzellen umprogrammiert werden koennen. Welche vier Transkriptionsfaktoren (sog. Yamanaka-Faktoren) sind dabei entscheidend?",
        answerA = "Oct4, Sox2, Klf4 und c-Myc",
        answerB = "Nanog, Rex1, Utf1 und Esrrb",
        answerC = "p53, Rb, BRCA1 und APC",
        answerD = "HIF-1alpha, VEGF, STAT3 und NFkappaB",
        correctAnswer = 0,
        explanation = "Yamanaka zeigte 2006, dass die gleichzeitige Expression von Oct4, Sox2, Klf4 und c-Myc in Maus-Fibroblasten ausreicht, um induzierte pluripotente Stammzellen (iPSC) zu erzeugen. Diese Entdeckung revolutionierte die Stammzellforschung und ermoeoecht patientenspezifische Zelltherapien ohne ethische Einwaende gegenueber embryonalen Stammzellen.",
        difficulty = 5,
        funFact = "c-Myc ist ein bekanntes Onkogen -- eine der groessten Sorgen bei fruehen iPSC-Protokollen war das erhoehte Tumorrisiko. Spaetere Protokolle konnten c-Myc weglassen oder durch kleine Molekuele ersetzen, um die Sicherheit zu verbessern."
    ),

    // Question 2 - CRISPR-Cas9 Nobelpreis
    Question(
        categoryId = 16,
        questionText = "Jennifer Doudna und Emmanuelle Charpentier erhielten 2020 den Nobelpreis fuer Chemie fuer CRISPR-Cas9. Welche Funktion hat die tracrRNA (trans-activating crRNA) im nativen CRISPR-Cas9-System von Streptococcus pyogenes?",
        answerA = "tracrRNA ist das eigentliche Schneidenzym, das die DNA-Doppelstrangbrueche erzeugt",
        answerB = "tracrRNA hybridisiert mit der crRNA und bildet zusammen mit ihr die duale RNA-Struktur, die Cas9 rekrutiert und aktiviert",
        answerC = "tracrRNA kodiert fuer die PAM-Erkennungsdomaene von Cas9",
        answerD = "tracrRNA inhibiert off-target-Aktivitaet durch kompetitive Hemmung",
        correctAnswer = 1,
        explanation = "Im nativen Typ-II-CRISPR-System hybridisiert die tracrRNA mit der CRISPR-RNA (crRNA) ueber einen gemeinsamen komplementaeren Bereich. Dieser crRNA:tracrRNA-Duplex bindet Cas9 und aktiviert dessen Nuklease-Aktivitaet. Doudna und Charpentier zeigten 2012, dass beide RNAs zu einer einzigen Guide-RNA (sgRNA) fusioniert werden koennen -- die Grundlage aller modernen CRISPR-Werkzeuge.",
        difficulty = 5,
        funFact = "Die PAM-Sequenz (Protospacer Adjacent Motif -- bei SpCas9: NGG) liegt auf der Nicht-Ziel-DNA-Seite und verhindert, dass Cas9 das eigene CRISPR-Locus im Bakteriengenom schneidet. Varianten wie SaCas9 oder Cas12a erkennen andere PAM-Sequenzen und erweitern das targetierbare Genom."
    ),

    // Question 3 - Allison und Honjo -- Checkpoint-Therapie
    Question(
        categoryId = 16,
        questionText = "James Allison und Tasuku Honjo erhielten 2018 den Nobelpreis fuer ihre Entdeckung der Krebstherapie durch Hemmung negativer Immunregulation. Welches Molekuel entdeckte Honjo, und welche Liganden aktivieren es?",
        answerA = "CTLA-4, aktiviert durch CD80/CD86 auf antigenpraesentierende Zellen",
        answerB = "PD-1, aktiviert durch PD-L1 (CD274) und PD-L2 (CD273) auf Tumorzellen und Stromazellen",
        answerC = "LAG-3, aktiviert durch MHC-Klasse-II-Molekuele",
        answerD = "TIM-3, aktiviert durch Galectin-9 und Phosphatidylserin",
        correctAnswer = 1,
        explanation = "Tasuku Honjo entdeckte PD-1 (Programmed Cell Death Protein 1) 1992 als Molekuel, das bei der T-Zell-Apoptose hochreguliert wird. Spaeter zeigte er, dass PD-1 bei Bindung seiner Liganden PD-L1 und PD-L2 T-Zellen hemmt. Viele Tumore exprimieren PD-L1, um der Immunabwehr zu entgehen. Allison hatte parallel CTLA-4 als Checkpoint entdeckt -- beide Mechanismen sind heute Grundlage einer ganzen Klasse von Krebsmedikamenten.",
        difficulty = 5,
        funFact = "Pembrolizumab (Keytruda) und Nivolumab (Opdivo) waren die ersten anti-PD-1-Antikoerper. Beim metastasierten Melanom, das fruehher fast immer toedlich war, ueberleben heute 20-30 % der Patienten dank Checkpoint-Inhibition dauerhaft -- ein Paradigmenwechsel in der Onkologie."
    ),

    // Question 4 - Nobelpreis Telomere -- Blackburn, Greider, Szostak
    Question(
        categoryId = 16,
        questionText = "Elizabeth Blackburn, Carol Greider und Jack Szostak erhielten 2009 den Nobelpreis fuer Medizin fuer die Entdeckung der Telomere und der Telomerase. Was ist die katalytische Komponente der Telomerase, und welche RNA-Untereinheit dient als Matrize?",
        answerA = "TERT (Telomerase Reverse Transcriptase) ist die katalytische Untereinheit; TERC (Telomerase RNA Component) dient als Matrize mit der Sequenz 3'-AAUCCC-5'",
        answerB = "Telomerase besteht ausschliesslich aus Proteinen; die Matrize ist ein DNA-Primer im Zellkern",
        answerC = "TERT ist die RNA-Komponente; TERC ist die Protein-Untereinheit",
        answerD = "Die Matrize stammt von einer mitochondrialen RNA und wird durch den TERRA-Komplex bereitgestellt",
        correctAnswer = 0,
        explanation = "Die Telomerase ist ein Ribonukleoprotein-Komplex. TERT (Telomerase Reverse Transcriptase) ist die katalytische Protein-Untereinheit mit reverser Transkriptase-Aktivitaet. TERC liefert die RNA-Matrize, anhand derer TERT die TTAGGG-Wiederholungen (beim Menschen) an die Chromosomenenden anhaengt. Blackburn hatte Telomer-Sequenzen bei Tetrahymena entdeckt; Greider wies die Enzymaktivitaet nach.",
        difficulty = 5,
        funFact = "Keimbahnzellen, Stammzellen und Krebszellen exprimieren Telomerase -- das erklaert ihre Unsterblichkeit. In somatischen Zellen ist TERT epigenetisch still, weshalb Telomere mit jeder Zellteilung kuerzer werden -- ein Mechanismus der zellulaerem Altern zugrunde liegt."
    ),

    // Question 5 - Nobelpreis Zirkadiane Uhr
    Question(
        categoryId = 16,
        questionText = "Hall, Rosbash und Young erhielten 2017 den Nobelpreis fuer die molekularen Mechanismen der zirkadianen Uhr. Welcher negative Rueckkopplungsmechanismus liegt dem Kernoszillator bei Drosophila zugrunde?",
        answerA = "PER und TIM akkumulieren tagsuebers, hemmen dann CLK/CYC und werden nachts durch DBT-vermittelte Phosphorylierung abgebaut",
        answerB = "CLK und CYC akkumulieren tagsuebers, hemmen dann PER/TIM und werden durch CLOCK/BMAL1 degradiert",
        answerC = "BMAL1 und PER bilden nachts einen Komplex, der CLK-Transkription direkt blockiert",
        answerD = "CRY allein bildet den negativen Feedback-Arm durch direkte Hemmung der RNA-Polymerase II",
        correctAnswer = 0,
        explanation = "CLK und CYC (CLOCK/BMAL1-Homologe) aktivieren per- und tim-Transkription. PER- und TIM-Proteine akkumulieren im Zytoplasma, bilden Heterodimere und translozieren in den Kern, wo sie CLK/CYC hemmen -- das ist der negative Feedback. Die Kinase DBT (DOUBLETIME, Casein-Kinase-I-epsilon-Homolog) phosphoryliert PER und triggert dessen proteasomalen Abbau, wodurch der Zyklus neu beginnen kann.",
        difficulty = 5,
        funFact = "Der menschliche Uhren-Mechanismus ist aehnlich, nutzt aber CLOCK/BMAL1 als positiven Arm und PER1/2/3 sowie CRY1/2 als negativen Arm. Mutationen in CK1delta oder PER2 verursachen beim Menschen familiaerem fortgeschrittenem Schlafphasensyndrom (FASP)."
    ),

    // --- GESCHICHTE DER ANAESTHESIE ---

    // Question 6 - Morton und Aethernarkose
    Question(
        categoryId = 16,
        questionText = "Am 16. Oktober 1846 demonstrierte William T.G. Morton erstmals oeffentlich die Narkose mit Diethylether im Massachusetts General Hospital. Welcher Chirurg fuehrte die Operation durch, und welche Art von Eingriff war es?",
        answerA = "John Collins Warren entfernte einen vaskulaeren Tumor am Hals von Patient Edward Abbott",
        answerB = "Oliver Wendell Holmes entfernte einen Blasenstein; er pragte auch den Begriff 'Anaesthesia'",
        answerC = "Crawford Long entfernte einen Hauttumor -- er hatte Ether bereits 1842 privat genutzt",
        answerD = "James Young Simpson amputierte ein Bein und demonstrierte gleichzeitig Chloroform als Alternative",
        correctAnswer = 0,
        explanation = "John Collins Warren, Chefchirurg am MGH, entfernte einen submandibulaeren vaskulaeren Tumor (Haemangiom) am Hals von Edward Abbott, waehrend Morton Ether-Narkose applizierte. Warren soll danach gesagt haben: 'Gentlemen, this is no humbug.' Oliver Wendell Holmes Holmes Sr. pragte spaeter den Begriff 'Anaesthesia' fuer diesen Zustand.",
        difficulty = 5,
        funFact = "Crawford Long hatte Ether bereits 1842 in Jefferson, Georgia, zur Schmerzausschaltung bei einer Zystenentfernung genutzt, publizierte aber erst 1849. Haette er frueher publiziert, wuerde heute sein Name mit der Entdeckung verbunden."
    ),

    // Question 7 - Curare in der Anaesthesie
    Question(
        categoryId = 16,
        questionText = "Harold Griffith und Enid Johnson fuehrten 1942 erstmals Curare (d-Tubocurarin) in die klinische Anaesthesie ein. Welchen Mechanismus nutzt d-Tubocurarin, und welches Problem loeste es bei Operationen?",
        answerA = "d-Tubocurarin ist ein nicht-depolarisierender nAChR-Antagonist an der neuromuskulaeren Endplatte; es ermoeglichte Muskelrelaxation ohne tiefe Narkose, was Chirurgen bessere Operationsbedingungen bei sichereren Narkosedosen gab",
        answerB = "d-Tubocurarin blockiert Natriumkanaele im ZNS und vertieft die Narkose durch Potenzierung von Halothan",
        answerC = "d-Tubocurarin ist ein depolarisierendes Muskelrelaxans wie Succinylcholin und erzeugt initialen Faszikulationen",
        answerD = "d-Tubocurarin hemmt Acetylcholinesterase und verlaengert dadurch die Wirkung endogenen Acetylcholins",
        correctAnswer = 0,
        explanation = "d-Tubocurarin konkurriert kompetitiv mit Acetylcholin um die nikotinischen Acetylcholinrezeptoren (nAChR) an der motorischen Endplatte, ohne diese zu aktivieren (nicht-depolarisierend). Vor 1942 mussten Chirurgen sehr tiefe Narkosen einsetzen, um ausreichende Muskelrelaxation zu erzielen -- mit gefaehrlichem Risiko fuer Atemdepression. Curare erlaubte erstmals die Trennung von Bewusstseinsverlust und Muskelrelaxation.",
        difficulty = 5,
        funFact = "Suedamerikanische Indigene nutzten Curare Jahrhunderte lang als Pfeilgift zur Jagd. Der erste Europaeische Bericht stammte von Walter Raleigh (1595). Die Strukturaufklaerung von d-Tubocurarin gelang Harold King 1948."
    ),

    // Question 8 - Simpson und Chloroform
    Question(
        categoryId = 16,
        questionText = "James Young Simpson fuehrte 1847 Chloroform als Narkosemittel ein, besonders fuer Geburten. Welcher royale Rueckenwind half, Chloroform gegen kirchliche und medizinische Opposition durchzusetzen?",
        answerA = "Koenigin Victoria liess sich 1853 bei der Geburt von Prinz Leopold Chloroform-Narkose geben; danach galt die Methode als gesellschaftlich akzeptabel",
        answerB = "Koenig Friedrich Wilhelm IV. von Preussen ordnete 1850 Chloroform als Pflicht fuer Militaeroperationen an",
        answerC = "Papst Pius IX. erliess 1849 eine Bulle, die Schmerzerleichterung bei der Geburt fuer Frauen erlaubte",
        answerD = "Zarin Alexandra Fjodorowna liess sich in St. Petersburg behandeln und lobte Chloroform oeffentlich",
        correctAnswer = 0,
        explanation = "Koenigin Victoria erhielt bei der Geburt ihres achten Kindes, Prinz Leopold (7. April 1853), Chloroform-Anaesthesie durch ihren Leibarzt John Snow. Ihre positive Bewertung -- sie beschrieb die Wirkung als 'soothing, quieting and delightful beyond measure' -- beendete wirksam die oeffentliche Debatte. Der Einwand, Geburtsschmerz sei gottgewollt (nach Genesis 3:16), verlor an Kraft.",
        difficulty = 5,
        funFact = "John Snow, der Chloroform bei Koenigin Victoria applizierte, ist derselbe Epidemiologe, der 1854 die Broad-Street-Cholera-Epidemie in London durch Entfernung des Pumpenschwengels eindaemmte -- einer der Gruendungsmythen der modernen Epidemiologie."
    ),

    // --- GESCHICHTE DER CHIRURGIE ---

    // Question 9 - Lister und Antisepsis
    Question(
        categoryId = 16,
        questionText = "Joseph Lister entwickelte ab 1865 die antiseptische Methode basierend auf Pasteurs Keimtheorie. Welche Substanz verwendete Lister primaer, und wie erklaerte er ihr Wirkprinzip in Bezug auf Pasteurs Arbeit?",
        answerA = "Lister verwendete Karbolsaeure (Phenol) und argumentierte, dass Mikroorganismen -- nicht 'Miasmen' -- Wundinfektionen verursachen; Phenol toete diese Keime ab",
        answerB = "Lister verwendete Iod-Tinktur und stuetzte sich auf Semmelweis' Beobachtungen zum Kindbettfieber",
        answerC = "Lister verwendete Chlor-Wasserloesungen (nach Semmelweis) und Alkohol; die Keimtheorie war ihm noch unbekannt",
        answerD = "Lister verwendete Quecksilberchlorid (Sublimat) und erklaerte es mit Fermenting-Theorien nach Liebig",
        correctAnswer = 0,
        explanation = "Lister las Pasteurs Publikationen ueber Gaerung und Faeulung durch Mikroorganismen und folgerte, dass Wundinfektionen (Sepsis, Gangraen) dieselbe Ursache haben. Er trank Karbolsaeure (Phenol) als Mittel der Wahl, da sie damals zur Abwasserbehandlung eingesetzt wurde. Er bespruehte Operationssaele, Instrumente und Wunden damit. Seine Amputations-Mortalitaet sank von ca. 46 % auf 15 %.",
        difficulty = 5,
        funFact = "Lister publizierte seine Ergebnisse 1867 im Lancet. Trotzdem lehnte ein Grossteil der Chirurgen, besonders in den USA, seine Methode jahrelang ab -- teils aus Skepsis, teils weil der penetrante Phenolgeruch als laestig galt. Robert Koch und Louis Pasteur unterstuetzten ihn oeffentlich."
    ),

    // Question 10 - Billroth und Magenresektion
    Question(
        categoryId = 16,
        questionText = "Theodor Billroth fuehrte 1881 die erste erfolgreiche Magenresektion durch. Wie heissen die beiden nach ihm benannten Rekonstruktionsverfahren nach distaler Gastrektomie?",
        answerA = "Billroth I (Gastroduodenostomie -- End-zu-End-Anastomose des Magenrestes mit dem Duodenum) und Billroth II (Gastrojejunostomie -- Magenrest wird mit einer Jejunumschlinge verbunden, Duodenalstumpf blind verschlossen)",
        answerB = "Billroth I (Roux-en-Y-Rekonstruktion) und Billroth II (Interposition eines Jejunumsegments zwischen Magen und Duodenum)",
        answerC = "Billroth I und II bezeichnen verschiedene Schnittfuehrungen am Mageneingang (Kardia), keine Anastomosenverfahren",
        answerD = "Billroth I (totale Gastrektomie mit oesophagojejunalem Anschluss) und Billroth II (subtotale Resektion mit Duodenostomie)",
        correctAnswer = 0,
        explanation = "Billroth I beschreibt die direkte End-zu-End-Verbindung des Magenrestes mit dem Duodenum nach distaler Resektion -- physiologischer, da Duodenum und Pankreas die Speise weiterhin beeinflussen. Billroth II bezeichnet die Gastrojejunostomie, bei der der Magenrest an eine Jejunumschlinge genaehte wird und das Duodenum blind verschlossen bleibt. Beide Verfahren werden noch heute verwendet.",
        difficulty = 5,
        funFact = "Billroth war auch ein versierter Kammermusiker und enger Freund von Johannes Brahms. Er gruendete in Wien einen Musikzirkel -- ein seltenes Beispiel eines Spitzenchirurgen, der gleichzeitig kuenstlerisch auf hoechstem Niveau taetig war."
    ),

    // Question 11 - Halsted und moderne Chirurgie
    Question(
        categoryId = 16,
        questionText = "William Stewart Halsted gilt als Begruender der modernen amerikanischen Chirurgie. Welche vier Prinzipien charakterisieren die 'Halstedianische' Chirurgie, und welche tragische persoenliche Geschichte verbindet er mit der Einfuehrung von Kokain als Lokalanaesthetikum?",
        answerA = "Gewebeschonung, haemostyptische Praepaeration, spannungsfreie Anastomosen, aseptische Technik; Halsted wurde bei Kokain-Selbstversuchen zur Nervenblockade kokainsueuchtig und spaeter morphinsueuchtig",
        answerB = "Schnelligkeit, minimalinvasive Schnitte, keine Drainagen, Fruehmobilisation; Halsted starb an einer Kokain-Ueberdosis beim ersten Lokalanaesthesie-Versuch",
        answerC = "Radikale Resektion, ausgedehnte Lymphadenektomie, Strahlentherapie intraoperativ; Halsted war nicht an der Kokain-Einfuehrung beteiligt",
        answerD = "Drainage, Trockenlegung, Nahtentlastung, Druckverband; Halsted nutzte Ether als Lokalanaesthetikum",
        correctAnswer = 0,
        explanation = "Halsteds Prinzipien -- sorgfaeltige Blutstillung, zartes Gewebehandling, spannungsfreie Naehte, sterile Technik -- sind bis heute Grundlage der Chirurgie. 1884 experimentierte er mit Kokain als Nervenblockade (nach Koellers Entdeckung als Lokalanaesthetikum fuer das Auge). Er injizierte Kokain in Nerven bei sich selbst und Kollegen und wurde dabei abhaengig. Die Behandlung mit Morphin fuerhrte zu einer zweiten Abhaengigkeit -- er arbeitete sein Leben lang darunter.",
        difficulty = 5,
        funFact = "Halsted erfand auch den Gummihandschuh fuer den OP -- nicht primaer fuer Sterilizaet, sondern weil Krankenschwester Caroline Hampton (seine spaetere Frau) eine Kontaktdermatitis durch Carbolloesung hatte. Er bat Goodyear, Gummihandschuhe herzustellen."
    ),

    // --- LANDMARK CLINICAL TRIALS ---

    // Question 12 - CAST-Studie
    Question(
        categoryId = 16,
        questionText = "Die CAST-Studie (Cardiac Arrhythmia Suppression Trial, 1989) ist eines der schockierendsten Ergebnisse der Kardiologie. Was war die Hypothese, was das Ergebnis, und welches Konzept widerlegte sie?",
        answerA = "Hypothese: Antiarrhythmika (Encainide, Flecainide) reduzieren nach Myokardinfarkt ventrikulaere Extrasystolen und senken damit Mortalitaet. Ergebnis: Die Mortalitaet stieg unter Therapie signifikant -- CAST widerlegte den 'Surrogatmarker-Trugschluss' (Arrhythmie-Suppression ist kein Ersatz fuer Mortalitaet als Endpunkt)",
        answerB = "Hypothese: Beta-Blocker reduzieren Arrhythmien nach Infarkt. Ergebnis: Mortalitaet sank um 30 % -- CAST bestaete die Notwendigkeit echter Mortalitaetsendpunkte",
        answerC = "Hypothese: Statine reduzieren Arrhythmien post-MI. Ergebnis: Kein Effekt -- CAST zeigte, dass Lipidsenkung keinen Einfluss auf Herzrhythmus hat",
        answerD = "Hypothese: Amiodaron ist anderen Antiarrhythmika ueberlegen. Ergebnis: Amiodaron war unterlegen -- CAST fuehrte zur Zurueckziehung von Amiodaron",
        correctAnswer = 0,
        explanation = "CAST testete die Hypothese, dass Unterdruckung ventrikulaerer Arrhythmien nach Myokardinfarkt (einem Surrogatmarker) die Sterblichkeit senkt. Das Gegenteil war der Fall: Encainide und Flecainide erhoehten die Mortalitaet (2,3x in der Behandlungsgruppe). Die Studie wurde fruehzeitig abgebrochen. CAST ist ein Paradebeispiel fuer den Surrogatmarker-Trugschluss und hat die Anforderungen der FDA an Mortalitaetsdaten fuer Antiarrhythmika fundamental veraendert.",
        difficulty = 5,
        funFact = "Der CAST-Befund war so unvermuteet, dass viele Kardiologen die Ergebnisse anfangs nicht glauben wollten. Die Studie veraenderte die gesamte Denkweise: Klinische Endpunkte (Sterblichkeit, Herzinfarkt) sind Surrogatmarkern (Laborwerte, EKG-Befunde) vorzuziehen."
    ),

    // Question 13 - ISIS-2 Studie
    Question(
        categoryId = 16,
        questionText = "Die ISIS-2-Studie (Second International Study of Infarct Survival, 1988) war eine wegweisende 2x2-faktoriell angelegte Studie. Was waren die vier Behandlungsgruppen, und was war das Hauptergebnis bezueglich Aspirin?",
        answerA = "Die vier Gruppen erhielten: (1) Streptokinase + Aspirin, (2) Streptokinase + Placebo, (3) Placebo + Aspirin, (4) Placebo + Placebo. Aspirin allein reduzierte 5-Wochen-Mortalitaet um 23 %; kombiniert mit Streptokinase um 42 % -- additiver Effekt",
        answerB = "ISIS-2 testete vier verschiedene Thrombolytika (Streptokinase, Alteplase, Urokinase, Reteplase) ohne Aspirin-Arm; Alteplase war am effektivsten",
        answerC = "ISIS-2 verglich Aspirin mit Heparin, Warfarin und Placebo nach Myokardinfarkt; Warfarin war die ueberlegene Therapie",
        answerD = "ISIS-2 war eine Statin-Studie, die erstmals zeigte, dass fruehe Lipidsenkung die Infarktmortalitaet senkt",
        correctAnswer = 0,
        explanation = "ISIS-2 randomisierte 17.187 Patienten mit akutem Myokardinfarkt in einem 2x2-Design zu intravenoeser Streptokinase (1,5 Mio. E ueber 1 h), oralem Aspirin (160 mg/Tag fuer 1 Monat), beiden oder keiner Therapie. Aspirin allein senkte die 35-Tage-Mortalitaet um 23 %, Streptokinase allein um 25 %, kombiniert um 42 % -- ein klassischer Nachweis der Additivitaet zweier Mechanismen (Thrombolyse + Plaettchenhemmung).",
        difficulty = 5,
        funFact = "ISIS-2 etablierte Aspirin als Standard bei akutem Myokardinfarkt -- eine Empfehlung, die bis heute gilt und weltweit mehr Leben gerettet haben duerfte als fast jede andere Einzelintervention in der Kardiologie."
    ),

    // Question 14 - SPRINT-Studie
    Question(
        categoryId = 16,
        questionText = "Die SPRINT-Studie (Systolic Blood Pressure Intervention Trial, 2015) untersuchte, ob ein Blutdruckzielwert von <120 mmHg gegenueber <140 mmHg Vorteile bringt. Was waren die Hauptergebnisse, und welche wichtige Einschraenkung der Studienmethodik wurde spaeter kritisiert?",
        answerA = "Intensivere Blutdrucksenkung reduzierte kardiovaskulaere Ereignisse und Mortalitaet signifikant; spaeter wurde kritisiert, dass SPRINT unbeaufsichtigte automatische Blutdruckmessungen nutzte (AOBP), die ca. 10 mmHg niedriger liegen als Standardmessungen -- der 'intensive' Zielwert entspricht tatsaechlich ca. <130 mmHg nach konventioneller Messung",
        answerB = "SPRINT zeigte keinen Unterschied zwischen den Gruppen; spaeter stellte sich heraus, dass die Randomisierung fehlerhaft war",
        answerC = "Der intensive Zielwert schadete durch Ueberbehandlung (Sturze, Nierenversagen) ohne Mortalitaetsvorteil; SPRINT wurde daher als gescheitert bewertet",
        answerD = "SPRINT wurde vorzeitig abgebrochen wegen exzessiver Sterblichkeit in der Kontrollgruppe; die Studie bewies, dass <140 mmHg gefaehrlich ist",
        correctAnswer = 0,
        explanation = "SPRINT zeigte signifikante Reduktion von kardiovaskulaeren Ereignissen (25 %) und Gesamtmortalitaet (27 %) unter intensiver Therapie (<120 mmHg Ziel). Die methodische Kritik: SPRINT nutzte unbeaufsichtigte automatische Blutdruckmessungen (AOBP), die reproduzierbar 5-10 mmHg unter Standard-Praxis-Messungen liegen. Der nominale Zielwert <120 mmHg in SPRINT entspricht damit faktisch ca. <130-135 mmHg in der normalen Praxis -- was die Interpretation der Leitlinienempfehlungen beeinflusst.",
        difficulty = 5,
        funFact = "SPRINT schloss explizit keine Diabetiker ein -- fuer Diabetiker gilt nach ACCORD-BP-Studie kein Vorteil intensiver Blutdrucksenkung unter <120 mmHg. Das ist ein wichtiger Unterschied: verschiedene Populationen, verschiedene Ergebnisse."
    ),

    // Question 15 - Women's Health Initiative
    Question(
        categoryId = 16,
        questionText = "Die Women's Health Initiative (WHI) veroeffentlichte 2002 bahnbrechende Ergebnisse zur postmenopausalen Hormontherapie. Welche Kombination wurde getestet, welche Risiken wurden gefunden, und was war die klinische Konsequenz?",
        answerA = "Konjugierte Oestrogene plus Medroxyprogesteronacetat (MPA) erhoehten Brustkrebs-, KHK-, Schlaganfall- und Lungenembolierisiko; die Studie wurde vorzeitig abgebrochen und fuehrte zu einem dramatischen Rueckgang der Hormontherapie weltweit",
        answerB = "Transdermal-Oestradiol plus mikronisiertes Progesteron zeigte neutrale Effekte; die WHI gilt daher als Bestaetigung der Sicherheit natuerlicher Hormone",
        answerC = "Nur Oestrogen (bei Frauen ohne Uterus) erhoehte Brustkrebs deutlich; Progesteron hatte keine Wirkung auf Risiken",
        answerD = "WHI zeigte, dass Hormontherapie Alzheimer-Risiko halbiert, was die Empfehlungen zur Primaerpraevention veraenderte",
        correctAnswer = 0,
        explanation = "WHI testete konjugierte equine Oestrogene (CEE, 0,625 mg/Tag) plus Medroxyprogesteronacetat (MPA, 2,5 mg/Tag) bei 16.608 postmenopausalen Frauen. Die Studie wurde 2002 nach 5,2 Jahren vorzeitig abgebrochen, da das globale Risiko-Nutzen-Profil unguenstig war: Brustkrebs (+26 %), KHK (+29 %), Schlaganfall (+41 %), Lungenembolie (+213 %), bei Reduktion von Darmkrebs (-37 %) und Hueftfrakturen (-33 %). Das Ergebnis fuehrte zu einem globalen Rueckgang der Hormonersatztherapie um ca. 50-80 %.",
        difficulty = 5,
        funFact = "Spaetere Reanalysen zeigten, dass Timing entscheidend ist ('Window of Opportunity'): Frauen, die Therapie kurz nach der Menopause begannen (<60 Jahre oder <10 Jahre nach letzter Menstruation), hatten guenstigere Ergebnisse. Das WHI-Kollektiv war mit Durchschnittsalter 63 Jahren deutlich aelter."
    ),

    // --- MEDIZINISCHE SKANDALE ---

    // Question 16 - Thalidomid-Skandal
    Question(
        categoryId = 16,
        questionText = "Der Thalidomid-Skandal (Contergan, 1957-1961) ist einer der groessten Arzneimittelkatastrophen. Welches biochemische Prinzip erklaert die Teratogenitaet, und warum wurde Thalidomid trotz tierexperimenteller Tests nicht rechtzeitig entdeckt?",
        answerA = "Thalidomid hemmt Angiogenese und Cereblon-vermittelte Ubiquitinierung entwicklungsrelevanter Proteine (SALL4, IKZF1); Ratten und Maeuse metabolisieren Thalidomid anders und zeigen keine Gliedmassenmissbildungen -- nur Kaninchen und Primaten sind anfaellig",
        answerB = "Thalidomid blockiert den Folsaeure-Rezeptor und verhindert Neuralrohrschluss; tierexperimentell wurde es an Fischen getestet, die keinen Neuralrohr-Defekt zeigen",
        answerC = "Thalidomid ist ein irreversibler COX-2-Hemmer, der Prostaglandine der Plazenta blockiert; Hunde und Katzen haben andere COX-2-Isoformen",
        answerD = "Thalidomid akkumuliert im Fruchtwasser und bildet dort toxische Reaktionsprodukte; in tierischen Plazenten wird es vor dem Eintritt ins Fruchtwasser vollstaendig abgebaut",
        correctAnswer = 0,
        explanation = "Thalidomid wirkt teratogen durch Hemmung des E3-Ubiquitin-Ligase-Komplexes, dessen Substratrezeptor Cereblon (CRBN) direkt von Thalidomid gebunden wird. Cereblon ubiquitiniert Entwicklungsproteine wie SALL4 (fuer Gliedmassenentwicklung), sodass Thalidomid deren Abbau foerdert. Ratten und Maeuse haben strukturelle Unterschiede in der CRBN-Bindungstasche, weshalb sie nicht anfaellig sind -- ein kritischer Mangel der damaligen Pruefstrategien.",
        difficulty = 5,
        funFact = "Frances Kelsey von der FDA verhinderte 1961 die Zulassung in den USA, da sie die Sicherheitsdaten fuer ungenuegend hielt -- eine der wenigen Entscheidungen, die tausende amerikanische Kinder vor Missbildungen schuetzte. Ironie: Heute ist Thalidomid als Lenalidomid-Vorgaenger gegen multiples Myelom zugelassen."
    ),

    // Question 17 - Vioxx-Skandal
    Question(
        categoryId = 16,
        questionText = "Rofecoxib (Vioxx) wurde 2004 nach dem Bekanntwerden kardiovaskulaerer Risiken zurueckgezogen. Welcher Mechanismus erklaert das erhoehte Herzinfarktrisiko, und welche Studiendaten hatte Merck fruehzeitig als besorgniserregend erkannt?",
        answerA = "COX-2-selektive Hemmung reduziert Prostacyclin (PGI2) in Endothelzellen, ohne Thromboxan A2 (TXA2) in Thrombozyten zu hemmen (die kein COX-2 exprimieren) -- das verschiebt das Gleichgewicht Richtung Thrombose; die VIGOR-Studie (2000) zeigte 5x mehr Myokardinfarkte gegenueber Naproxen",
        answerB = "COX-2-Hemmung erhoehte direkt Cholesterin-Biosynthese durch Hemmung von HMG-CoA-Reduktase; Merck wusste dies seit der fruehen Phase-I-Studie",
        answerC = "Rofecoxib blockierte auch Calcium-Kanaele am Herzen; dieses Off-Target-Profil wurde erst nach Markteinfuehrung erkannt",
        answerD = "Vioxx erhoeht Fibrinogen-Spiegel durch Induktion von CRP; die Kaskadenreaktion fuehrt zu Hyperkoagulabilitaet durch IL-6-vermittelte Leberreaktion",
        correctAnswer = 0,
        explanation = "COX-2 wird in Endothelzellen exprimiert und erzeugt dort vasodilatatorisches, antithrombotisches Prostacyclin (PGI2). Thrombozyten exprimieren kein COX-2, bilden aber ueber COX-1 prothrombotisches Thromboxan A2 (TXA2). Selektive COX-2-Hemmung stoert dieses Gleichgewicht. Die VIGOR-Studie (Vioxx vs. Naproxen bei Rheuma-Patienten) zeigte 2000 signifikant mehr Myokardinfarkte -- Merck attributierte dies 'Naproxens Schutzwirkung', nicht Rofecoxibs Schaedlichkeit.",
        difficulty = 5,
        funFact = "Schatzungen zufolge verursachte Vioxx weltweit 88.000-139.000 Herzinfarkte, davon 38.000-61.000 toedlich. Merck einigte sich 2007 auf einen 4,85-Milliarden-Dollar-Vergleich -- einer der teuersten Arzneimittel-Rueckzuege der Geschichte."
    ),

    // Question 18 - Opioid-Krise und Purdue Pharma
    Question(
        categoryId = 16,
        questionText = "Die US-amerikanische Opioid-Krise toetete seit 1999 ueber 500.000 Menschen. Welche Fehlinformation ueber OxyContin (Oxycodon-Retardformulierung) verbreitete Purdue Pharma gezielt beim Vertrieb, und welche forensisch-pharmakologische Eigenschaft machte OxyContin besonders missbrauchsanfaellig?",
        answerA = "Purdue verbreitete die Falschaussage, dass Retardformulierungen ein 'weniger als 1 %-iges' Abhaengigkeitspotential haben; die Retard-Galenik konnte jedoch durch Zerkauen oder Aufloesen umgangen werden, was eine sofortige Hochdosis-Opioid-Freisetzung ermoeglichte",
        answerB = "Purdue behauptete, OxyContin enthalte Naloxon als eingebauten Missbrauchsschutz; das Naloxon war jedoch oral inaktiv und bot keinen Schutz bei intravenoeser Nutzung",
        answerC = "Purdue finanzierte gefaelschte Studien, die zeigten, dass Oxycodon keine Atemdepression verursacht; die Wirkung war vergleichbar mit schwachen Opioiden wie Codein",
        answerD = "Purdue verbreitete, dass OxyContin fuer Krebspatienten reserviert sei; tatsaechlich wurde es fuer akute postoperative Schmerzen massenhaft verschrieben",
        correctAnswer = 0,
        explanation = "Purdue Pharma vermarktete OxyContin ab 1996 aggressiv mit der Behauptung, Retardformulierungen seien kaum missbrauchsanfaellig ('less than one percent' Abhaengigkeitspotential -- eine Aussage, die auf einer Fehlinterpretation eines Leserbriefs im NEJM basierte). Die Galenik liess sich trivial umgehen: Zerkauen oder Aufloesen setzte die gesamte Dosis auf einmal frei. Purdue-Gruender pleideten 2022 schuldig.",
        difficulty = 5,
        funFact = "Das 'Porter-Jick Letter' (NEJM 1980) -- ein 101-Wort-Leserbrief, der berichtete, dass bei hospitalisierten Patienten selten Sucht entstand -- wurde von der Pharmaindustrie mehr als 600 Mal zitiert, um die Sicherheit von Opioiden zu behaupten. Die Autoren hatten explizit nicht Langzeit-ambulante-Behandlung gemeint."
    ),

    // --- MODERNSTE THERAPIEN ---

    // Question 19 - mRNA-Therapeutika
    Question(
        categoryId = 16,
        questionText = "Katalin Kariko und Drew Weissman entwickelten die Grundlage fuer therapeutische mRNA. Welche spezifische Modifikation der Nukleosidbasen machte mRNA fuer therapeutische Zwecke nutzbar, und welcher Mechanismus erklaert die verbesserte Immuntoleranz?",
        answerA = "Austausch von Uridin gegen N1-Methylpseudouridin (m1Psi) verhindert Aktivierung von Toll-like-Rezeptoren (TLR3, TLR7, TLR8), die normalerweise fremde RNA erkennen -- die modifizierte mRNA entgeht der angeborenen Immunantwort und wird stabiler translatiert",
        answerB = "Austausch von Adenosin gegen Inosin verhindert ADAR-vermittelte RNA-Editierung und stabilisiert die Sekundaerstruktur fuer bessere Ribosomenbindung",
        answerC = "5'-Cap-Modifikation mit einem 2'-O-Methyl-Trinucleotid-Cap verhindert XRN1-vermittelten 5'-Abbau und ist allein verantwortlich fuer Immuntoleranz",
        answerD = "Pseudouridin-Modifikation veraendert die Kodierungsspezifitaet, sodass modifizierte mRNA als Nicht-Selbst-RNA von Ribosomen erkannt und bevorzugt translatiert wird",
        correctAnswer = 0,
        explanation = "Kariko und Weissman entdeckten 2005, dass der Austausch natuerlicher Uridine gegen Pseudouridin -- und spaeter N1-Methylpseudouridin -- in synthetischer mRNA die Aktivierung von TLRs verhindert. TLR3 erkennt doppelstraengige RNA-Verunreinigungen, TLR7/8 erkennen einzelstraengige RNA mit bestimmten Sequenzmotiven. Modifizierte mRNA wird nicht als fremd erkannt, entgeht dem Abbau durch RNasen und wird effizienter translatiert. Dies ist die Kerngrundlage der COVID-19-mRNA-Impfstoffe.",
        difficulty = 5,
        funFact = "Kariko und Weissman erhielten 2023 den Nobelpreis fuer Medizin. Karikos Karriere war zuvor alles andere als reibungslos: Sie wurde 1995 von der University of Pennsylvania auf eine niedrigere Stelle versetzt, weil sie keine Foerdergelder einwerben konnte -- ihre mRNA-Forschung galt als zu spekulativ."
    ),

    // Question 20 - Gene Drives
    Question(
        categoryId = 16,
        questionText = "Gene Drives sind CRISPR-basierte Systeme, die sich in natuerlichen Populationen ausbreiten koennen. Welcher Mechanismus erklaert die nicht-mendelsche Vererbung eines Gene Drive, und welche medizinische Anwendung wird am intensivsten erforscht?",
        answerA = "Ein Gene Drive kodiert fuer Cas9 und eine Guide-RNA, die das Wildtyp-Allel auf dem homologen Chromosom schneidet; durch homologe Rekombination wird das Wildtyp-Allel durch den Drive ersetzt -- Heterozygote produzieren damit fast 100 % Drive-tragende Nachkommen statt der mendeleschen 50 %; Hauptanwendung: Malaria-Bekaempfung durch Unfruchtbarkeit oder Vektorkompetenz-Verlust bei Anopheles gambiae",
        answerB = "Gene Drives nutzen RNA-Interferenz, um dominante Wildtyp-Allele stillzulegen; sie werden hauptsaechlich gegen Krebs in somatischen Zellen entwickelt",
        answerC = "Gene Drives sind Plasmide, die sich horizontal zwischen Bakterienarten uebertragen und Antibiotikaresistenz-Gene eliminieren -- Hauptanwendung ist Bekaempfung multiresistenter Keime",
        answerD = "Gene Drives basieren auf Homing-Endonukleasen ohne CRISPR und wurden bereits in Feldversuchen gegen Dengue-Moskitos eingesetzt",
        correctAnswer = 0,
        explanation = "Ein CRISPR-Gene Drive besteht aus Cas9 und sgRNA, die im heterozygoten Organismus das korrespondierende Wildtyp-Allel schneiden. Die Zelle repariert den Doppelstrangbruch bevorzugt durch HDR unter Verwendung des Drive-Allels als Matrize -- das Ergebnis ist ein homozygoter Drive-Traeger. Mit fast 100 % Uebertragungsrate breitet sich der Drive durch eine Population aus. Bei Anopheles gambiae werden Sterilitaetsgene oder Malaria-Resistenzgene eingefuehrt.",
        difficulty = 5,
        funFact = "Das Target Malaria-Konsortium hat 2019 erste eingeschraenkte Feldversuche mit sterilen Maennchen (keine Drive) in Burkina Faso begonnen. Echter Gene Drive in Wildpopulationen blieb bisher auf Laborexperimente beschraenkt -- die ethischen und ekologischen Risiken irreversibler Populationsveraenderungen sind erheblich."
    ),

    // Question 21 - Xenotransplantation
    Question(
        categoryId = 16,
        questionText = "Im Januar 2022 erhielt Patient David Bennett Sr. am University of Maryland Medical Center ein genetisch modifiziertes Schweineherz. Welche zehn genetischen Modifikationen enthielt das Tier (hergestellt von Revivicor), und warum starb Bennett nach 60 Tagen?",
        answerA = "4 Schweine-Gene wurden ausgeschaltet (GGTA1, CMAH, B4GalNT2, GHR), 6 menschliche Gene eingebracht (hCD46, hCD55, hTBM, hEPCR, hCD47, hHO1) fuer Immuntoleranz; Bennett starb an einer Infektion mit Porcine Cytomegalovirus (PCMV), das im Organspendertier nicht ausreichend ausgeschlossen worden war",
        answerB = "10 Schweine-Gene wurden durch CRISPR ausgeschaltet, darunter alle MHC-I-Gene; Bennett starb an hyperakuter Abstossung durch residuelle Xeno-Antigene",
        answerC = "Das Schwein erhielt das gesamte menschliche HLA-System durch Knock-in; Bennett starb an Graft-versus-Host-Disease durch migrierte Schweine-Immunzellen",
        answerD = "5 Komplement-Inhibitoren wurden eingebracht; Bennett starb an einer chronischen Abstossung durch CD8-T-Zellen gegen ein nicht erkanntes Schweine-Antigen",
        correctAnswer = 0,
        explanation = "Das Revivicor-Schwein (GalSafe-Linie plus) trug 4 Knock-outs: GGTA1 (alpha-Gal-Epitop), CMAH (Neu5Gc), B4GalNT2 (SDa-Antigen) und GHR (Wachstumshormonrezeptor fuer normale Herzgroesse). Dazu 6 menschliche Transgene fuer Komplementschutz und Antikoagulation (hCD46, hCD55, hTBM, hEPCR, hCD47, hHO1). Retrospektive Analysen zeigten reaktiviertes PCMV im Transplantat -- ein Viren-Screening-Protokoll-Fehler.",
        difficulty = 5,
        funFact = "Bennett war zu krank fuer ein konventionelles Spenderherz. Er sagte vor der Operation: 'Es war entweder sterben oder diese Transplantation durchfuehren.' Das Ereignis gilt als Meilenstein trotz des Scheiterns -- die Abwesenheit hyperakuter Abstossung war ein historischer Erfolg."
    ),

    // --- GLOBALE GESUNDHEITSMEILENSTEINE ---

    // Question 22 - Pocken-Eradikation
    Question(
        categoryId = 16,
        questionText = "Die WHO erklaerte Pocken 1980 als ausgerottet. Welche epidemiologische Eigenschaft von Variola major machte die Eradikation durch Ringe-Impfung (Surveillance-Containment) moeglich, die bei anderen Infektionskrankheiten nicht funktioniert?",
        answerA = "Pocken haben kein tierisches Reservoir, keinen asymptomatischen Traeger und sind erst NACH Ausbruch eines gut sichtbaren Exanthems ansteckend -- was Erkennung und Ringimpfung ermoeglicht, bevor Kontaktpersonen infiziert werden",
        answerB = "Das Variola-Virus mutiert extrem langsam (niedriger Mutationsrate), sodass ein einziger Impfstoff dauerhaft schutzt ohne Varianten-Drift",
        answerC = "Pocken wurden durch Kamele als einzigem Reservoir uebertragen -- nach Eliminierung infizierter Kamelherden verschwand das Virus",
        answerD = "Der Pocken-Impfstoff (Vaccinia) erzeugt sterile Immunitat; geimpfte Personen koennen das Virus weder tragen noch weitergeben",
        correctAnswer = 0,
        explanation = "Variola ist anthroponotisch (kein Tierreservoir), verursacht keine asymptomatischen Infektionen, und die groesste Infektioesitaet tritt mit dem charakteristischen Pustelexanthem auf -- also NACH dem Erkennen des Falles. Dadurch konnten Epidemiologen Kontakte identifizieren und impfen, bevor das Virus weitersprang. Diese Kombination ist einzigartig: Masern z.B. sind VOR dem Ausschlag ansteckend, was Surveillance-Containment unmoeglich macht.",
        difficulty = 5,
        funFact = "Der letzte natuerliche Fall trat am 26. Oktober 1977 in Merca, Somalia, auf: Ali Maow Maalin, ein Krankenhausporter, erkrankte und genas. Ein Jahr spaeter, 1978, gab es einen Laborunfall in Birmingham -- Janet Parker starb an Pocken, als Variola-Viren aus einem Untergeschoss durch das Lueftungssystem stiegen."
    ),

    // Question 23 - Polio-Bekaempfung
    Question(
        categoryId = 16,
        questionText = "Bei der globalen Polio-Bekaempfung konkurrieren der inaktivierte Polio-Impfstoff (IPV, Salk) und der orale Lebendimpfstoff (OPV, Sabin). Welches Phaenomen erklaert, warum OPV trotz fast vollstaendiger Eradikation weiterhin Polio-Faelle verursacht?",
        answerA = "OPV-Viren koennen in Populationen mit niedrigem Impfstatus zirkulieren und dabei zu virulenten Vaccine-Derived Polioviruses (cVDPV) revertieren, die klinisch nicht von Wildtyp-Polio unterscheidbar sind und Laehmungen verursachen",
        answerB = "IPV-Immunisierung schuetzt nicht vor intestinaler Infektion, sodass OPV als einziger Impfstoff gilt, der Darmausscheidung des Wildtyp-Virus verhindert",
        answerC = "OPV verursacht in immungesunden Kindern regelhaft eine asymptomatische Polio-Infektion, die nach 20 Jahren reaktivieren kann",
        answerD = "Das Sabin-Virus mutiert in Sauerstoff-armen Umgebungen (z.B. Abwasser) zu Wildtyp-Virulenz -- daher ist OPV in tropischen Laendern verboten",
        correctAnswer = 0,
        explanation = "Das attenuierte Sabin-Virus in OPV repliziert im Darm und wird im Stuhl ausgeschieden. In Gemeinschaften mit niedrigen OPV-Impfraten zirkuliert dieses Virus ueber laengere Zeit und kann durch Akkumulation von Mutationen (besonders im VP1-Protein und der 5'-UTR) zu cirkulierenden Vaccine-Derived Polioviruses (cVDPV) revertieren. cVDPV2 (Type-2-Stamm) ist aktuell die haeufigste Ursache von Polio-Laehmungen weltweit -- ein paradoxes Ergebnis des Impferfolgs.",
        difficulty = 5,
        funFact = "Die Globale Polio-Eradikationsinitiative wechselte 2016 von trivalentem OPV (tOPV) zu bivalentem OPV (bOPV, ohne Typ-2-Stamm), da Wildtyp-2 bereits eradiziert war. Dieser Wechsel sollte cVDPV2 eliminieren -- paradoxerweise fuehrte er kurzfristig zu mehr cVDPV2-Ausbruechen in Laendern, wo Typ-2-Immunitaet rasch sank."
    ),

    // --- KUENSTLICHE INTELLIGENZ IN DER MEDIZIN ---

    // Question 24 - Radiologie-KI
    Question(
        categoryId = 16,
        questionText = "Die FDA-zugelassene KI fuer Radiologie basiert hauptsaechlich auf Convolutional Neural Networks (CNNs). Welches spezifische Problem der klinischen Validierung hat zu intensiver Kritik gefuehrt, und wie wird es als 'Datenleck' (Data Leakage) beschrieben?",
        answerA = "CNNs werden auf Bildern trainiert und getestet, die von denselben Krankenhaeusern oder Bildgebungsgeraeten stammen -- das Modell lernt Artefakte und Geraetefingerabdruecke statt diagnostische Merkmale; beim Einsatz an anderen Kliniken bricht die Leistung dramatisch ein",
        answerB = "Radiologie-KI-Modelle werden ausschliesslich auf synthetischen GAN-Bildern trainiert, die keine echten klinischen Befunde repraesentieren",
        answerC = "Data Leakage in Radiologie-KI bedeutet, dass Patientendaten ungeschluesselt auf Cloud-Servern gespeichert werden -- ein Datenschutzproblem, kein Validierungsproblem",
        answerD = "CNNs werden auf Bildern trainiert, die manuell von Radiologen nachbearbeitet wurden -- echte klinische Bilder haben andere Kontrast-Eigenschaften und werden fehlklassifiziert",
        correctAnswer = 0,
        explanation = "Das groesste Validierungsproblem in der Radiologie-KI ist mangelnde externe Generalisierbarkeit durch implizites Data Leakage: Trainings- und Testdaten kommen oft aus denselben Institutionen, sodass Modelle institutionsspezifische Merkmale (Scanner-Modell, Belichtungsprotokoll, Patientenpopulation) lernen statt allgemeine diagnostische Muster. Rettig-Studien zeigen regelmaessig, dass top-performende Modelle bei externen Kohorten oft nur noch zufaellige Leistung zeigen.",
        difficulty = 5,
        funFact = "CheXNet (Stanford, 2017) behauptete, radiologische Pneumonie-Diagnosen zu uebertreffen. Unabhaengige Replikationsversuche zeigten, dass das Modell teilweise den Literaturtext auf gescannten Roentgenbildern las, der Diagnosen verriet -- ein klassisches Datenleck-Beispiel in der KI-Medizin."
    ),

    // Question 25 - Clinical Decision Support
    Question(
        categoryId = 16,
        questionText = "Das Epic Sepsis Model (ESM) wurde in hunderten US-Kliniken eingesetzt und als KI-Meilenstein gefeiert. Eine Untersuchung der University of Michigan (Prospekt. Studie 2021, JAMA Internal Medicine) kam zu ernuechternden Ergebnissen. Was zeigten die Ergebnisse?",
        answerA = "Das ESM hatte eine niedrige Sensitivitaet (33 %) fuer tatsaechliche Sepsis-Faelle und eine hohe Falsch-Positiv-Rate -- 67 % aller ESM-Alarme waren falsch-positiv; das Modell erkannte nur 7 % der Sepsis-Todesfaelle rechtzeitig, ohne klinische Verbesserung gegenueber bestehenden Scores wie SOFA oder SIRS",
        answerB = "Das ESM war exzellent validiert (AUC 0,92) und reduzierte Sepsis-Mortalitaet um 18 % in allen validierten Kohorten",
        answerC = "Das ESM funktionierte nur bei Patienten ueber 65 Jahre und unterschaetzte systematisch Sepsis bei Kindern -- Epic hat daher ein separates paediatrisches Modell entwickelt",
        answerD = "Das ESM zeigte, dass praediktive KI-Modelle in der Intensivmedizin Laborwerte nicht korrekt integrieren koennen und daher bildgebungsbasierte Modelle ueberlegen sind",
        correctAnswer = 0,
        explanation = "Wong et al. (JAMA Internal Medicine 2021) untersuchten das ESM prospektiv an 38.455 Erwachsenen. Schluessel-Ergebnisse: Sensitivitaet 33 % (d.h. 67 % aller echten Sepsis-Faelle wurden verpasst), PPV 12 % (88 % der Alarme falsch positiv), AUC 0,74. Von 7.745 ESM-Alarmen loesten 45 % keine klinische Aktion aus. Das Modell erkannte nur 109 von 1.552 Sepsis-Todesfaellen rechtzeitig. Die Publikation wurde zu einer vielzitierten Warnung vor unkritischer KI-Adoption.",
        difficulty = 5,
        funFact = "Epic hatte den internen Quellcode und Trainingsparameter nicht oeffentlich gemacht -- Kliniken hatten ein 'Blackbox'-Modell eingesetzt, ohne seine Leistung unabhaengig validieren zu koennen. Der Fall wurde zum Argument fuer verpflichtende externe Validierung vor dem klinischen Einsatz von KI-Entscheidungssystemen."
    ),

    // --- REGENERATIVE MEDIZIN ---

    // Question 26 - 3D-Bioprinting
    Question(
        categoryId = 16,
        questionText = "3D-Bioprinting von Gewebe verwendet Bioinks, die lebende Zellen enthalten. Welches ist das kritische ungoeloeste Problem beim Druck grosser Organe (>1 cm Dicke), das die klinische Anwendung bisher verhindert?",
        answerA = "Vaskularisierung: Ohne ein eingebettetes Kapillarnetz sterben Zellen in mehr als ca. 200 Mikrometern Abstand von der naechsten Blutversorgung durch Hypoxie und Naehrstoffmangel ab -- gedruckte Organe ueber 1 cm Dicke nekrotisieren nach dem Druck",
        answerB = "Mechanische Stabilitaet: Gedruckte Gewebe sind zu fragil fuer chirurgische Implantation und werden durch Naehte zerissen",
        answerC = "Immunologische Abstossung: Bioinks enthalten bovines Kollagen, das starke Xeno-Immunreaktionen auslost",
        answerD = "Sterilisation: Der Druckprozess kann nicht unter vollstaendig aseptischen Bedingungen stattfinden, da Nozzle-Temperaturen Zellen abtoten",
        correctAnswer = 0,
        explanation = "Die 200-Mikrometer-Regel gilt fuer passive Diffusion: Sauerstoff und Naehrstoffe erreichen Zellen jenseits dieser Distanz nicht mehr ausreichend. In nativem Gewebe uebernimmt ein dichtes Kapillarnetz diese Versorgung. Im 3D-Druck fehlt dieses Netzwerk -- ohne Co-Druck von Vaskularstrukturen oder nachtraeglicher Angiogenese entsteht eine avaskulaere Nekrose groesserer Gewebebloecke. Aktuelle Forschungsansaetze umfassen Fugenbioprinting mit auswaschbaren Opferkanalen und die Co-Kultur mit Endothelzellen.",
        difficulty = 5,
        funFact = "2019 gelang es einer israelischen Gruppe (Tel Aviv University), ein miniaturisiertes Herz mit Gefaessen, Vorhoefen und Kammern aus patienteneigenen Fett-Stammzellen zu biodrucken -- das erste vollstaendig personalisierte kleine Herz, allerdings nur 1,5 cm gross und ohne Pumpleistung fuer eine Transplantation."
    ),

    // Question 27 - Organoid-Technologie
    Question(
        categoryId = 16,
        questionText = "Organoids sind dreidimensionale Miniaturorgane, die aus Stammzellen generiert werden. Hans Clevers Labor beschrieb 2009 erstmals Darm-Organoids. Welches Signalmolekuel ist entscheidend fuer die Stammzell-Nische im Darmkryptus, und wie wird es in der Organoid-Kultur simuliert?",
        answerA = "Wnt-Liganden (besonders Wnt3a) sind essenziell fuer die Stammzell-Nische; in der Organoid-Kultur wird dies durch Zugabe von konditioniertem L-Wnt3a-Zell-Medium, R-Spondin-1 (RSPO1, ein Wnt-Verstaerker) und Noggin (BMP-Inhibitor) simuliert -- zusammen als 'ENR'-Cocktail bekannt",
        answerB = "EGF allein ist ausreichend fuer Darm-Stamzell-Kultur; Wnt-Signale werden durch synthetische GSK3-Inhibitoren vollstaendig ersetzt",
        answerC = "Notch-Signale sind dominant; Delta-like-Liganden auf Paneth-Zellen aktivieren Hes1 in Stammzellen -- in Organoid-Kultur durch rekombinantesDLL1/4 ersetzt",
        answerD = "TGF-beta aktiviert die Stammzell-Nische; seine Hemmung durch SB431542 ist Grundbedingung jeder Organoid-Kultur",
        correctAnswer = 0,
        explanation = "Im nativen Darm sezernieren Paneth-Zellen und Mesenchym Wnt3a und RSPO1, die gemeinsam den Wnt-Signalweg in Lgr5+ Stammzellen aktivieren. BMP-Signale aus der Submukosa supprimieren die Stammzellaktivitaet ausserhalb der Krypte -- Noggin blockiert BMP. EGF (aus Mesenchym und Paneth-Zellen) foerdert Proliferation. Clevers Gruppe kombinierte ENR (EGF, Noggin, RSPO1 -- aus L-Wnt3a-konditioniertem Medium) und Matrigel als dreidimensionale Matrix.",
        difficulty = 5,
        funFact = "Organoids aus Patienten mit Mukoviszidose (CFTR-Mutationen) reagieren messbar auf CFTR-Korrektoren wie Ivacaftor -- das Organoid-Aufblaehen (Schwellung durch CFTR-abhaengige Chloridsekretion) korreliert mit der klinischen Wirksamkeit. Organoids werden daher als personalisierte Praediktionstools fuer Mukoviszidose-Patienten eingesetzt."
    ),

    // Question 28 - Tissue Engineering
    Question(
        categoryId = 16,
        questionText = "Im Juni 2011 erhielt Andemariam Teklesenbet Beyene als erster Mensch eine vollstaendig tissue-engineered Trachea transplantiert. Welche Technik wurde verwendet, und welches Graftversagen-Problem trat spaeter auf?",
        answerA = "Ein dekellularisiertes Spender-Trachea-Geruest wurde mit Beyenes eigenen Stammzellen (aus Knochenmark) besiedelt und mit bioreaktiver Kultur vorgereift; spaetere Faelle mit synthetischen Polymergerippen (Macchiarini) zeigten schwere Komplikationen bis zum Tod der Patienten, und Paolo Macchiarini wurde des Fehlverhaltens ueberuerrt",
        answerB = "Eine synthetische Silikon-Prothese wurde mit autologen Chondrozyten beschichtet und transplantiert; das Implantat versagte nach 6 Monaten durch mechanische Fraktur",
        answerC = "Ein 3D-gedrucktes Titangeruest mit allogenen Epithelzellen wurde transplantiert; Abstossung durch NK-Zellen gegen allogenem HLA war das Hauptproblem",
        answerD = "Beyene erhielt eine rein zellulaere Injektion ohne Geruest; Stammzellen organisierten sich in situ zur Trachea-Struktur -- ein Prozess der 'In-situ-Organogenese'",
        correctAnswer = 0,
        explanation = "Beyene erhielt 2011 am Karolinska-Institut (Paolo Macchiarini) eine Trachea aus einem dezellularisierten Spendergeruest, das mit Beyenes eigenen Stammzellen besiedelt wurde. Das Verfahren schien zunaechst erfolgreich. Macchiarini fuegte spaeter neun weitere Patienten mit synthetischen Polymergeruesten zu. Sieben der neun Patienten starben. 2016 wurde aufgedeckt, dass Macchiarini Ergebnisse faelschte und Komplikationen verschwieg -- einer der groessten Betrugskandale der regenerativen Medizin.",
        difficulty = 5,
        funFact = "Die Macchiarini-Affaere fuehrte zum Ruecktritt mehrerer Karolinska-Direktoren und veraenderte die Aufsicht ueber klinische Forschung in Schweden grundlegend. Eine Netflix-Dokumentation ('The Surgeon's Cut', 2020) und das Buch 'The con man's last case' dokumentierten den Fall."
    ),

    // --- ONE HEALTH UND PANDEMIEVORSORGE ---

    // Question 29 - One Health Konzept
    Question(
        categoryId = 16,
        questionText = "Das One-Health-Konzept anerkennt die untrennbare Verbindung von menschlicher, tierischer und Umweltgesundheit. Wie hoch ist der Anteil neu auftretender Infektionskrankheiten (EIDs), die zoonotischen Ursprungs sind, und welche Oekosystem-Veraenderung gilt als wichtigster Treiber?",
        answerA = "Ca. 60-75 % aller neu auftretenden Infektionskrankheiten sind zoonotisch; Landnutzungsveraenderung (Entwaldung, Agrarexpansion in Wildlandschaften) ist der staerkste Treiber, da sie den Kontakt zwischen Wildtieren, Nutztieren und Menschen erhoht",
        answerB = "Ca. 15-20 % sind zoonotisch; der wichtigste Treiber ist der internationale Flugverkehr, der geografisch isolierte Tierpopulationen in Kontakt bringt",
        answerC = "Ca. 90 % sind zoonotisch, hauptsaechlich durch Antibiotikaresistenz-Uebertragung von Nutztieren auf Menschen",
        answerD = "Ca. 40 % sind zoonotisch; der Haupttreiber sind Wildtiermaerkte, weshalb deren globales Verbot die Hauptempfehlung der WHO ist",
        correctAnswer = 0,
        explanation = "Jones et al. (Nature 2008) zeigten, dass 60,3 % der EIDs zoonotischen Ursprungs sind; bei Ausbruechen tierischer Erkrankungen sind es 71,8 %. Epidemiologische Modellierungen identifizieren Landnutzungsveraenderung konsistent als staerksten Treiber: Entwaldung bringt Menschen in Kontakt mit Reservoirtieren (Fledermaeuse, Nagetiere), die sonst isoliert leben. COVID-19, Ebola, Nipah, SARS-1 -- alle zeigen dieses Muster.",
        difficulty = 5,
        funFact = "Das Spillover-Risiko ist nicht gleichmaessig verteilt: Tropische Biodiversitaetszentren (Zentralafrika, Suedostasien, Amazonas) haben das hoechste zoonotische Spillover-Potential, da sie groesste Artenvielfalt mit wachsendem menschlichen Footprint kombinieren -- das sog. 'Planetary Boundaries'-Konzept erfasst dieses Risiko."
    ),

    // Question 30 - Pandemievorsorge CEPI und 100-Days-Mission
    Question(
        categoryId = 16,
        questionText = "Nach der COVID-19-Pandemie formulierte die Coalition for Epidemic Preparedness Innovations (CEPI) die '100 Days Mission'. Was ist das Ziel, und welche technologische Plattform ist entscheidend dafuer?",
        answerA = "Binnen 100 Tagen nach Identifikation eines neuen Pathogens sollen sichere und wirksame Impfstoffkandidaten fuer klinische Tests bereitstehen; mRNA-Plattformen ermoeglichen dies durch schnelles Austauschen der Antigensequenz ohne Neuentwicklung des gesamten Herstellungsprozesses",
        answerB = "Binnen 100 Tagen soll ein globales Lockdown-Protokoll koordiniert ausgeloest werden -- die Plattform ist ein WHO-unterstuetztes Frueharnsystem mit KI-Surveillance",
        answerC = "Binnen 100 Tagen sollen Antiviralia fuer neue Viren synthetisiert und klinisch getestet werden; CRISPR-basierte Diagnostik ist die Schlueisseltechnologie",
        answerD = "Binnen 100 Tagen sollen 100 Millionen Dosen einer konventionellen Tot-Impfung produziert werden; der Fortschritt haengt von globalen cGMP-Produktionskapazitaeten ab",
        correctAnswer = 0,
        explanation = "Die 100-Days-Mission (G7-unterstuetzt, 2021) zielt darauf, innerhalb von 100 Tagen nach Ausrufung eines PHEIC sichere Impfstoffkandidaten in Phase-I-Studien zu bringen. Der COVID-Impfstoff von BioNTech/Pfizer benootigte 266 Tage von Sequenzierung bis Notfallzulassung -- die Mission will diese Zeit auf unter 100 Tage verkuerzen. mRNA ist central, da nur die Antigensequenz veraendert werden muss; Laufprozesse, Lipidnanopartikel-Formulierung und Qualitaetskontrolle bleiben gleich.",
        difficulty = 5,
        funFact = "CEPI schatzt, dass die Kosten fuer vollstaendige Pandemie-Praeventionsinfrastruktur (Plattform-Impfstoffe fuer alle bekannten Virusfamilien) ca. 3,5 Milliarden Dollar betraegen -- ein Bruchteil der geschaetzten 28 Billionen Dollar wirtschaftlichen Schadens durch COVID-19."
    ),

    // --- WEITERE NOBELPREIS-THEMEN ---

    // Question 31 - Nobelpreis H. pylori
    Question(
        categoryId = 16,
        questionText = "Barry Marshall und Robin Warren erhielten 2005 den Nobelpreis fuer die Entdeckung von Helicobacter pylori als Magenulkus-Ursache. Welche dramatische Selbst-Experiment-Methode nutzte Marshall 1984, und welche Koch'schen Postulate mussten formal erfuellt werden?",
        answerA = "Marshall trank eine H.-pylori-Suspension und entwickelte eine akute Gastritis (endoskopisch und histologisch bestaetigt), behandelte sich dann selbst mit Antibiotika und heilte; Koch'sche Postulate: Keim isoliert, in Reinkultur gezuechtet, Gastritis beim Wirt erzeugt, Keim aus erkranktem Wirt re-isoliert",
        answerB = "Marshall injizierte sich intravenos H.-pylori-Extrakt und entwickelte eine haematoene Gastritis -- ein Tiermodell war zuvor gescheitert; Nobel wurde fuer die alternative Infektionsroute vergeben",
        answerC = "Marshall infizierte Freiwillige ohne deren Wissen mit H. pylori als Geschmacksstoff in Studiengetraenken -- ein Ethikskandal, der heute als Nobelpreis-Anomalie gilt",
        answerD = "Marshall fuehrte sich H. pylori per Magenspuelung zu und entwickelte sofort ein Magenulkus; Warren lieferte den histologischen Nachweis der Bakterien im Ulkusrand",
        correctAnswer = 0,
        explanation = "Marshall trankam 12. Juli 1984 eine Loesung mit 10^9 H.-pylori-Bakterien. Nach 5 Tagen entwickelte er Nausea und Erbrechen; nach 10 Tagen zeigte eine Endoskopie Gastritis. Re-Isolierung des Keims erfuellte das dritte und vierte Koch'sche Postulat. Marshall behandelte sich dann mit Tinidazol. Das Experiment ueberzeugte die Fachwelt, die die Hypothese eines infektioesen Magenulkus jahrelang abgelehnt hatte.",
        difficulty = 5,
        funFact = "Das Magenulkus galt jahrzehntelang als stressbedingte Erkrankung -- der Ratschlag lautete: Stressvermeidung, Diaet und Antazida. Heute gelten ca. 90 % der Ulcera duodeni und 70 % der Magenulzera als H.-pylori-bedingt und sind mit Eradikationstherapie heilbar."
    ),

    // Question 32 - Nobelpreis Prionen -- Prusiner
    Question(
        categoryId = 16,
        questionText = "Stanley Prusiner erhielt 1997 den Nobelpreis fuer die Entdeckung der Prionen als ursaechliche Agenzien von Prionerkrankungen. Welches molekulare Merkmal unterscheidet das pathologische PrPSc vom normalen PrPC, und wie propagiert sich PrPSc ohne Nucleinsaeure?",
        answerA = "PrPC ist alpha-helikal und loeslich; PrPSc ist beta-faltblattreich, aggregiert zu Amyloidfibrillen und ist protease-resistent. PrPSc wirkt als Schablone und induziert Konformationsaenderung von PrPC zu PrPSc -- kein Nucleinsaeure-Replikationszyklus noetig",
        answerB = "PrPSc traegt eine einzigartige posttranslationale Modifikation (Phosphorylierung an Ser231), die seine Struktur veraendert; normale Kinasen produzieren gelegentlich zufaellig PrPSc",
        answerC = "PrPC und PrPSc haben identische Primaerstruktur und Glykosylierung, unterscheiden sich aber in der Lipid-Rafts-Assoziation -- PrPSc ist raftgebunden und stabil gegen Verdauung",
        answerD = "PrPSc enthaelt ein kleines RNA-Molekuel (Prion-assoziierte RNA, PaRNA), das seine Replikation ermoeglicht -- Prusiner uebersah dieses RNA-Molekuel in seiner Originalarbeit",
        correctAnswer = 0,
        explanation = "PrPC und PrPSc haben identische Aminosaeuresequenz (kodiert durch das PRNP-Gen) aber unterschiedliche Tertiaerstruktur: PrPC ist ca. 42 % alpha-Helix, 3 % beta-Faltblatt, loeslich und GPI-verankert. PrPSc ist ca. 43 % beta-Faltblatt, 30 % alpha-Helix, wasserunloeslich, Detergenz-resistent und partiell Proteinase-K-resistent. PrPSc bindet PrPC und katalysiert dessen Umfaltung -- ein autokatalytischer Konformationstransfer ohne Nucleinsaeure.",
        difficulty = 5,
        funFact = "Prusiners Prion-Hypothese stiess bei Erstpublikation (1982) auf heftigen Widerstand -- die Vorstellung, eine infektioese Erkrankung ohne Nucleinsaeure koennte existieren, widersprach dem Zentraldogma. Colchester und andere Kritiker argumentierten jahrelang fuer eine Slow-Virus-Theorie. Der Nobel-Preis wurde kontrovers diskutiert, da Beweise fuer reine Proteinkopie noch nicht vollstaendig erbracht worden waren."
    ),

    // --- WEITERE MEDIZINISCHE SKANDALE ---

    // Question 33 - Tuskegee-Syphilis-Studie
    Question(
        categoryId = 16,
        questionText = "Die Tuskegee-Syphilis-Studie (1932-1972) ist das groesste Ethikversagen der US-amerikanischen Forschungsgeschichte. Was geschah 1947 -- sechs Jahre nach der Veroeffentlichung der Penicillin-Wirksamkeit gegen Syphilis -- mit den Studienteilnehmern?",
        answerA = "Die 399 syphiliskranken afroamerikanischen Maenner erhielten auch nach 1947 kein Penicillin, obwohl es als Standardtherapie etabliert war; die Forscher wollten den 'natuerlichen Verlauf' weiterverfolgen -- aktive Taeussung verhinderte, dass Teilnehmer anderswo Behandlung suchten",
        answerB = "1947 wurde die Studie nach Protesten afroamerikanischer Aerzte beendet und alle Teilnehmer sofort mit Penicillin behandelt -- ein Muster, das spaeter als Erfolg der Buergerrechtsbewegung gewertet wurde",
        answerC = "1947 wurde Penicillin getestet, aber nur an der Kontrollgruppe (syphilisfreie Maenner) -- ein falsches Protokoll, das die Forscher versehentlich einfuehrten",
        answerD = "Die Studienleiter behandelten die Teilnehmer 1947 heimlich mit Penicillin-Unterdosen, um sicherzustellen, dass die Studie nicht aufflog -- ein ethisches Delikt, das erst 1972 aufgedeckt wurde",
        correctAnswer = 0,
        explanation = "1945-1947 wurde Penicillin als wirksame Behandlung gegen Syphilis etabliert. Die Tuskegee-Studienleiter entschieden sich bewusst, die 399 Maenner der Beobachtungsgruppe nicht zu behandeln. Mehr noch: Als 1944 ein Wehrpflichtprogramm Syphilitikern standardmaessig Penicillin anbot, verhinderten die Forscher aktiv, dass Studienteilnehmer einbezogen wurden. Die Studie wurde erst 1972 nach dem Whistleblowing durch Peter Buxtun gestoppt.",
        difficulty = 5,
        funFact = "Die Tuskegee-Studie fuehrte 1974 zum National Research Act und dem Belmont-Bericht (1979), der die drei Grundprinzipien biomedizinischer Forschungsethik codifizierte: Respekt fuer Personen (informed consent), Benefizenz und Gerechtigkeit. Diese Prinzipien sind bis heute Grundlage aller IRB-Protokolle weltweit."
    ),

    // Question 34 - Wakefield-MMR-Skandal
    Question(
        categoryId = 16,
        questionText = "Andrew Wakefields 1998 veroeffentlichte Lancet-Studie, die einen Zusammenhang zwischen MMR-Impfung und Autismus behauptete, ist eine der schadlichsten Wissenschaftsfaelschungen. Welche spezifischen ethischen Vergehen und Datenfaelschungen wurden dokumentiert?",
        answerA = "Wakefield hatte ein Patent auf einen rivalisierenden Masern-Einzelimpfstoff; er beschaffte Kinderdaten ohne ausreichende Ethikvoten, manipulierte histologische Befunde und verzeichnete falsche Symptom-Zeitverlaeufe -- darueber hinaus zahlte er Kindern Geld fuer Blutproben auf seiner Sohnes Geburtstagsparty",
        answerB = "Wakefield hatte nur die statistische Auswertung manipuliert, alle anderen Daten waren korrekt -- das Lancet zog die Studie wegen unzureichendem Peer-Review, nicht wegen Faelschung, zurueck",
        answerC = "Wakefield hatte keine Interessenkonflikte, aber die Stichprobe (12 Kinder) war zu klein -- das Lancet zog die Studie wegen Underpowering zurueck, nicht wegen Faelschung",
        answerD = "Wakefield faelschte nur die Sequenzierung des Masernvirus im Darmgewebe; klinische Daten der 12 Kinder waren unveraendert",
        correctAnswer = 0,
        explanation = "Brian Deer deckte auf: (1) Wakefield hatte ein Patent auf einen Einzelimpfstoff gegen Masern und ein finanzielles Interesse daran, den MMR-Kombistoff zu diskreditieren. (2) Er erhielt 435.643 Pfund von einem Anwalt, der Eltern bei Klagen gegen Impfstoffhersteller vertrat. (3) Er beschaffte Blutproben von Kindern auf einer Geburtstagsparty. (4) Symptomzeitverlaeufe wurden rueckwirkend veraendert. (5) Histologische Befunde wurden umgeschrieben. Das Lancet zog die Studie 2010 zurueck.",
        difficulty = 5,
        funFact = "Die Konsequenzen der Faelschung halten bis heute an: MMR-Impfraten sanken in Grossbritannien von 92 % (1996) auf 80 % (2004). Masern-Ausbrueche kehrten in Europa zurueck. WHO schaetzt, dass Anti-Impfbewegungen, zu denen Wakefields Studie beitrug, jaehrlich hunderttausende vermeidbare Todesafaelle verursachen."
    ),

    // --- SCHNITTMENGE FORSCHUNG UND KLINIK ---

    // Question 35 - Liquid Biopsy
    Question(
        categoryId = 16,
        questionText = "Liquid Biopsy bezeichnet den Nachweis zirkulierender Tumor-DNA (ctDNA) im Blut. Welche technische Limitierung erklaert, warum Liquid Biopsy fuer fruehe Krebsstadien (Stage I) noch unzuverlaessig ist, und welcher Ansatz verspricht Verbesserung?",
        answerA = "ctDNA-Fragmente machen in Stage I nur 0,01-0,1 % der gesamten zellfreien DNA aus (Variant Allele Frequency); background-Fehlerrate herkoemmlicher NGS (ca. 0,1 %) uebersteigt das Signal -- fehler-korrekturierte Sequenzierverfahren (Duplex-Sequenzierung, CancerSEEK-Ansatz mit Protein-Biomarker-Kombination) verbessern Sensitivitaet",
        answerB = "ctDNA hat eine Halbwertszeit von nur 10 Minuten im Blut -- standardmaessige Phlebotomie-Protokolle ermoeglichen keine repraesentativen Proben; kryogene Sofortfixierung waere noetig",
        answerC = "Liquid Biopsy versagt in Stage I, weil Tumore zu klein sind, um Zellen freizusetzen -- erst ab Stage II gibt es haematogene Dissemination",
        answerD = "Stage-I-Tumore produzieren ausreichend ctDNA, aber diagnostische Algorithmen koennen Tumor-DNA von altersassoziierten Klonalitaetsmutationen (CHIP) nicht unterscheiden -- eine Limitierung die durch Epigenomics-Sequenzierung ueberbrueckt wird",
        correctAnswer = 0,
        explanation = "In Stage I betraegt die Varianten-Allelfrequenz (VAF) von ctDNA oft 0,01 % oder weniger. Konventionelle NGS hat eine Fehlerrate von ca. 0,1 % pro Base -- damit ist das Signal im Rauschen verborgen. Duplex-Sequenzierung (beide DNA-Straenge unabhaengig sequenziert) reduziert die Fehlerrate auf 10^-7. CancerSEEK (Cohen et al., Science 2018) kombiniert ctDNA mit acht Protein-Biomarkern und erreichte Sensitivitaet von 70 % ueber alle Krebsarten.",
        difficulty = 5,
        funFact = "CHIP (Clonal Hematopoiesis of Indeterminate Potential) ist tatsaechlich ein relevantes Interpretationsproblem: Altersbedingte haematopoetische Klone tragen somatische Mutationen (DNMT3A, TET2, ASXL1), die Tumor-Mutationen vortaeuschen koennen. Alle modernen Liquid-Biopsy-Tests muessen CHIP-Varianten bioinformatisch herausfiltern."
    ),

    // Question 36 - CAR-T-Zelltherapie
    Question(
        categoryId = 16,
        questionText = "CAR-T-Zelltherapien (Chimeric Antigen Receptor T cells) revolutionierten die Behandlung haematologischer Malignome. Welche Komplikation ist spezifisch fuer CAR-T-Therapie, und welcher Zytokin-Mechanismus liegt ihr zugrunde?",
        answerA = "Zytokin-Release-Syndrom (CRS): massiver Anstieg von IL-6, IL-1, IFN-gamma durch aktivierte Makrophagen und CAR-T-Zellen; behandelt mit Tocilizumab (anti-IL-6R) -- und CAR-T-spezifisch ist die Immune Effector Cell-Associated Neurotoxicity Syndrome (ICANS)",
        answerB = "Tumor-Lysis-Syndrom (TLS) ist die spezifische CAR-T-Komplikation, da alle Zielzellen gleichzeitig lysiert werden -- Behandlung: Rasburicase und aggressive Hydrierung",
        answerC = "Graft-versus-Host-Disease (GvHD) ist die Hauptkomplikation, da allogene T-Zellen Wirtsgewebe angreifen -- beim autologen Ansatz entfaellt dieses Risiko vollstaendig",
        answerD = "CAR-T-spezifisch ist das Makrophagenaktivierungssyndrom (MAS/HLH) ohne Zytokin-Beteiligung -- Behandlung: Hochdosis-Kortikosteroide und Ruxolitinib",
        correctAnswer = 0,
        explanation = "CRS entsteht durch massenhafte T-Zell- und Makrophagen-Aktivierung: IL-6 (hauptsaechlich von Makrophagen), IL-1, IFN-gamma und andere Zytokine steigen dramatisch an. Klinisch: Fieber, Hypotension, Hypoxie bis zum ARDS. Tocilizumab (anti-IL-6R-Antikoerper) ist die Standardbehandlung. ICANS (Immune Effector Cell-Associated Neurotoxicity Syndrome) -- Verwirrung, Aphasie, Krampfanfaelle -- ist eine separate, CAR-T-spezifische neurologische Komplikation mit aehnlichen Zytokin-Mustern.",
        difficulty = 5,
        funFact = "Emily Whitehead war 2012 die erste Kinderpatientin, die mit CAR-T-Zellen gegen akute lymphatische Leukaemie behandelt wurde. Sie entwickelte schweres CRS und lag im Sterben -- bis Oncologist Carl June Tocilizumab (damals nur fuer rheumatoide Arthritis zugelassen) off-label einsetzte. Emily ist heute Teenagerin und gilt als Ikone der CAR-T-Geschichte."
    ),

    // Question 37 - Epigenetische Uhren
    Question(
        categoryId = 16,
        questionText = "Steve Horvath entwickelte 2013 eine epigenetische Uhr basierend auf DNA-Methylierungsmustern. Was ist der 'epigenetische Altersversatz' (Epigenetic Age Acceleration), und welche klinische Relevanz hat ein positiver Versatz (epigenetically older als chronological age)?",
        answerA = "Positiver Altersversatz bedeutet, das das biologische (epigenetische) Alter hoeher ist als das Lebensalter; er korreliert mit erhoehtem Risiko fuer altersassoziierte Erkrankungen (Diabetes, kardiovaskulaere Erkrankungen, Krebsmortalitaet) und mit fruehzeitigem Tod -- unabhaengig von herkoemmlichen Risikofaktoren",
        answerB = "Positiver Altersversatz ist bei Athleten haeufig, da intensives Training DNA-Methylierung beschleunigt -- er gilt als Marker fuer koerperliche Fitness und nicht als Krankheitsrisiko",
        answerC = "Epigenetischer Altersversatz wird ausschliesslich durch Ernaehrung bestimmt; caloric restriction kann ihn vollstaendig negativ machen (juenger als chronologisches Alter)",
        answerD = "Der Altersversatz misst nur DNA-Methylierung in Immunzellen und hat keine Aussagekraft ueber Gewebe-spezifisches Altern oder Gesamtmortalitaet",
        correctAnswer = 0,
        explanation = "Horvaths pan-tissue epigenetic clock (2013) nutzt 353 CpG-Sites zur Schaetzung des biologischen Alters aus DNA-Methylierung. Epigenetic Age Acceleration (EAA = biologisches minus chronologisches Alter) ist positiv, wenn Zellen 'aelter' methyliert sind als erwartet. Mehrere Kohortenstudien (einschliesslich Framingham Heart Study) zeigten, dass EAA Gesamtmortalitaet und krankheitsspezifische Mortalitaet vorhersagt, unabhaengig von Rauchen, BMI und anderen Faktoren.",
        difficulty = 5,
        funFact = "Horvaths Uhr wurde aus 8.000 Proben aus 51 verschiedenen Geweben trainiert -- bemerkenswert ist ihre gewebsuebergreifende Genauigkeit. Neuere Uhren (GrimAge, PhenoAge) kombinieren Methylierungsdaten mit klinischen Biomarkern und verbessern die Mortalitaetsvorhersage weiter."
    ),

    // Question 38 - Senolytika
    Question(
        categoryId = 16,
        questionText = "Senolytika sind Substanzen, die selektiv seneszente Zellen eliminieren. Welches molekulare Merkmal seneszenter Zellen ermoeglicht die gezielte Elimination durch Dasatinib und Quercetin (D+Q), und was ist der SASP?",
        answerA = "Seneszente Zellen ueberexprimieren anti-apoptotische Signalwege (BCL-2, BCL-XL, PI3K/AKT via FOXO4-p53-Interaktion) um trotz DNA-Schaeden zu ueberleben -- D+Q hemmt diese Wege; SASP (Senescence-Associated Secretory Phenotype) bezeichnet das proinflammatorische Sekretom seneszenter Zellen (IL-6, IL-8, MMPs), das Nachbarzellen schaedigt",
        answerB = "Seneszente Zellen haben erhoehte Telomerase-Aktivitaet -- Dasatinib hemmt TERT direkt; SASP ist der Stopp des Zellzyklus durch p21/p16",
        answerC = "Seneszente Zellen exprimieren CD47 ('Don't eat me'-Signal) auf der Oberflaeche -- D+Q blockiert CD47 und ermoeglicht Makrophagen-Phagozytose; SASP beschreibt die epigenetische Reprogrammierung seneszenter Zellen",
        answerD = "Seneszente Zellen haeufen Lipofuszin an -- Quercetin loest dieses Pigment auf; SASP bezeichnet die Sekretion von Lipidmediatoren (Ceramid, Sphingolipide)",
        correctAnswer = 0,
        explanation = "Seneszente Zellen aktivieren pro-survival-Pathways (BCL-2, BCL-XL, BCL-W, survivin, PI3K/AKT), um apoptoseresistent zu sein. Dasatinib hemmt Tyrosinkinasen (src, BCR-ABL) die an FOXO4-p53-Interaktion beteiligt sind; Quercetin hemmt PI3K/AKT/BCL-2. Der SASP beinhaltet Hunderte sekretierter Faktoren -- IL-6, IL-8, GRO-alpha, MMPs, VEGF -- die chronische sterile Inflammation ('Inflammaging') und para-seneszente Ausbreitung vermitteln.",
        difficulty = 5,
        funFact = "Die ersten klinischen Senolytika-Studien (Mayo Clinic, James Kirkland) zeigten in kleinen Phase-I-Studien vielversprechende Ergebnisse bei idiopathischer Lungenfibrose und diabetischer Nephropathie. Die grosse offene Frage: Sind seneszente Zellen ausschliesslich schaedlich? Studien zeigen, dass sie bei Wundheilung und Embryonalentwicklung wichtige Rollen spielen."
    ),

    // Question 39 - Mikrobiom und FMT
    Question(
        categoryId = 16,
        questionText = "Faekal-Mikrobiota-Transplantation (FMT) zeigt hohe Wirksamkeit gegen rezidivierende Clostridioides-difficile-Infektionen (rCDI). Welcher Mechanismus erklaert die Wirksamkeit, und welche FDA-Entscheidung 2022 veraenderte den regulatorischen Status von FMT?",
        answerA = "FMT stellt das diverse Darmmikrobiom wieder her, das sekundaere Gallensaeuren produziert, die C.-difficile-Sporenkeimung hemmen, und konkurrenzfaehige Kolonisierung durch Resistenz-Kommensale foerdert; 2022 erhielt Rebyota (RBX2660, Ferring) als erste FDA-zugelassene FMT-Praeparation die Biologics License",
        answerB = "FMT neutralisiert C.-difficile-Toxine A und B durch polyklonale Antikoerper im Spenderstuhl; 2022 verbat die FDA alle nicht-standardisierten FMT-Anwendungen",
        answerC = "FMT funktioniert durch Uebertragung von Bakteriophagen, die spezifisch C.-difficile-Staemme infizieren; FDA bewertet Phagen als Medizinprodukt, nicht als biologisches Praeparat",
        answerD = "FMT stellt Butyrat-produzierende Bakterien wieder her, die Kolonozyten-Energieversorgung sichern und C.-difficile durch Epithelstaerkung hemmen; FMT erhielt 2022 FDA-Breakthrough-Therapy-Status",
        correctAnswer = 0,
        explanation = "C. difficile gedeiht in antibiotikagestoerem Mikrobiom, da Wettbewerber fehlen und primaere Gallensaeuren (die C.-difficile-Sporenkeimung foerdern) nicht zu sekundaeren Gallensaeuren (Desoxycholat, Lithocholat -- Keimungshemmer) konvertiert werden. FMT stellt diverse Kommensalen (Bacteroidetes, Firmicutes) und Gallensaeuren-metabolisierende Bakterien (Clostridium scindens) wieder her. Rebyota wurde im November 2022 von der FDA als erstes standardisiertes FMT-Praeparat (mikrobieller Einlauf aus gescreentem Spenderstuhl) zugelassen.",
        difficulty = 5,
        funFact = "FMT-Heilungsraten bei rCDI betragen 85-90 % -- deutlich hoeher als Vancomycin (40-60 %) oder Fidaxomicin. Die erste dokumentierte FMT fand 1958 statt, als Ben Eiseman vier Patienten mit pseudomembranoser Kolitis durch Stuhlklistiere rettete -- Jahrzehnte bevor C. difficile als Erreger identifiziert war."
    ),

    // Question 40 - Praezisionsmedizin und Pharmakogenomik
    Question(
        categoryId = 16,
        questionText = "Clopidogrel (Plavix) ist ein Prodrug, das hepatisch aktiviert werden muss. Welches Enzym ist dabei verantwortlich, und welcher klinische Schweregrad entsteht bei Loss-of-Function-Varianten (CYP2C19 *2, *3)?",
        answerA = "CYP2C19 ist das Hauptenzym der aktiven Metaboliten-Bildung; homozygote Loss-of-Function-Traeger (Poor Metabolizer) produzieren kaum aktiven Metaboliten, haben bis zu 3x erhoehtes Risiko fuer Stent-Thrombose nach PCI -- FDA verlangt seit 2010 einen Black-Box-Hinweis und empfiehlt Alternativtherapie (Prasugrel, Ticagrelor)",
        answerB = "CYP3A4 aktiviert Clopidogrel; da CYP3A4 kaum polymorphe Varianten zeigt, ist Pharmakogenomik fuer Clopidogrel klinisch irrelevant",
        answerC = "UGT1A1 glukuronidiert den aktiven Metaboliten und inaktiviert ihn; Gilbert-Syndrom-Patienten (UGT1A1 *28/*28) haben erhoehten Plasmaspiegel und Blutungsrisiko",
        answerD = "CYP2D6 ist verantwortlich; Ultrarapid Metabolizer produzieren zu viel aktiven Metaboliten und haben erhoehtes Blutungsrisiko -- klinisch wichtiger als Poor Metabolizer",
        correctAnswer = 0,
        explanation = "Clopidogrel ist ein Thienopyridin-Prodrug, das in zwei Schritten durch CYP2C19 (dominant), CYP3A4, CYP1A2 und Esterasen zum aktiven Thiol-Metaboliten umgewandelt wird. CYP2C19 *2 (rs4244285) und *3 (rs4986893) sind Loss-of-Function-Allele. Heterozygote (Intermediate Metabolizer) und Homozygote (Poor Metabolizer) haben reduzierte Plattchenhemmung und erhoehtes ischaemisches Risiko nach PCI. FDA fuegte 2010 eine Boxed Warning hinzu; Alternativtherapien sind Prasugrel und Ticagrelor (keine CYP2C19-Abhaengigkeit).",
        difficulty = 5,
        funFact = "CYP2C19-Polymorphismen sind in der asiatischen Bevoelkerung besonders haeufig: 15-25 % der Han-Chinesen und Japaner sind Poor Metabolizer (vs. 2-5 % der Kaukasier). In asiatischen Laendern wurde Clopidogrel trotzdem jahrelang ohne pharmakogenomisches Testing verschrieben -- ein Problem der ungleichen Umsetzung von Praezisionsmedizin."
    ),

    // Question 41 - AlphaFold und Proteinstruktur
    Question(
        categoryId = 16,
        questionText = "DeepMinds AlphaFold2 (2021) gilt als Durchbruch in der Proteinstrukturvorhersage. Welche architektonische Innovation unterscheidet AlphaFold2 von frueheren Methoden, und welche Limitierung besteht bei der Vorhersage dynamischer Proteinzustaende?",
        answerA = "AlphaFold2 nutzt Evoformer-Transformer-Bloecke, die multiple sequence alignments (MSA) und pair representation gemeinsam verarbeiten; Limitierung: Es sagt primaer den energetisch guenstigsten statischen Zustand vorher und erfasst Konformationsdynamik, intrinsisch unstrukturierte Regionen (IDRs) und ligandinduzierte Konformationsaenderungen nur unzureichend",
        answerB = "AlphaFold2 nutzt ausschliesslich homologie-basiertes Modelling (Komparativ-Modellierung); sein Durchbruch liegt in der automatischen Template-Selektion aus PDB, nicht in neuen Lernverfahren",
        answerC = "AlphaFold2 sagt Primaer- und Sekundaerstruktur vorher, aber keine Tertiaerstruktur -- der Beitrag liegt in exakter Helix/Faltblatt-Annotation, nicht in 3D-Koordinaten",
        answerD = "AlphaFold2 ist ein MD-basiertes Verfahren (Molecular Dynamics in silico), das Boltzmann-Sampling der Konformationsraeume durchfuehrt und damit dynamische Zustands-Ensembles voraussagt",
        correctAnswer = 0,
        explanation = "AlphaFold2s Kernarchitektur besteht aus Evoformer-Bloecken, die gleichzeitig auf MSA-Ebene (Koevolution) und Residuenpaare-Ebene (geometrische Constraints) Attention-Mechanismen anwenden. Ein nachgeschaltetes Structure Module sagt dann 3D-Koordinaten vorher. Median TM-Score auf CASP14 war 0,92 -- naeher an experimentellen Strukturen als je zuvor. Limitierung: IDRs (ca. 30-40 % des menschlichen Proteoms) bleiben schlecht vorhergesagt; Allosterie und Konformationsensembles sind kaum darstellbar.",
        difficulty = 5,
        funFact = "AlphaFold2 sagte innerhalb von zwei Jahren ueber 200 Millionen Proteinstrukturen vorher -- mehr als alle experimentellen Methoden (Roentgenkristallographie, NMR, Kryo-EM) in 50 Jahren. Die European Bioinformatics Institute (EMBL-EBI) pflegt die AlphaFold Protein Structure Database als freien Zugang fuer die Wissenschaft."
    ),

    // Question 42 - Kryo-Elektronen-Mikroskopie
    Question(
        categoryId = 16,
        questionText = "Jacques Dubochet, Joachim Frank und Richard Henderson erhielten 2017 den Nobelpreis fuer Chemie fuer die Entwicklung der Kryo-EM. Welches technische Problem der klassischen Elektronenmikroskopie loest die Kryo-EM, und was ist 'single-particle analysis'?",
        answerA = "Klassische EM benoetigt chemische Fixierung und Schwermetall-Kontrastierung, die biologische Strukturen artifizieren; Kryo-EM kuehlte Proben in vitrifiziertem (glasartigem) Eis ein -- kein Kristall, keine Schaeden. Single-particle analysis: Tausende Partikelbilder aus verschiedenen Orientierungen werden rechengestuetzt zu einer 3D-Struktur rekonstruiert",
        answerB = "Klassische EM konnte nur Proteine unter 100 kDa visualisieren; Kryo-EM loeste das Groessproblem durch Verwendung laengerer Elektronenwellenlaengen",
        answerC = "Kryo-EM loeste das Vakuum-Problem der EM: Biologische Proben verdampfen im EM-Vakuum -- Kryofixierung haelt sie fest. Single-particle analysis bedeutet, ein einziges Protein-Molekuel ueber Stunden zu beobachten",
        answerD = "Klassische EM war auf Membranen beschraenkt; Kryo-EM erlaubt erstmals die Visualisierung loeslicher Proteine durch Negativkontrast-Faerbung in wasserahnlichen Puffern",
        correctAnswer = 0,
        explanation = "Das Hauptproblem war Strahlen-Schaeden und Dehydratationsartefakte durch klassische Praeparation. Dubochet entwickelte die Vitrifikation: Proben werden in fluessigem Ethan (-180 degC) so schnell eingefroren, dass Wasser glasartig erstarrt (kein Eiskristall-Artefakt). Franks 'single-particle analysis' sammelt 2D-Projektionsbilder tausender identischer Molekuele in verschiedenen Orientierungen und rekonstruiert daraus per Algorithmen (3D-Rekonstruktion via BackProjection und Iterative-Refinement) eine 3D-Struktur.",
        difficulty = 5,
        funFact = "Die 'resolution revolution' in der Kryo-EM (ca. 2013) wurde durch bessere direkte Elektronendetektoren (DED) ausgeloest, die Elektronen ohne Zwischenkonversion in Licht detektieren -- damit sank Rauschen drastisch und sub-3-Angstroem-Strukturen wurden routinemaessig erreichbar. Heute werden Medikamente gegen COVID-19, Influenza und Krebs mithilfe von Kryo-EM-Strukturen entwickelt."
    ),

    // Question 43 - Optogenetik
    Question(
        categoryId = 16,
        questionText = "Optogenetik (Deisseroth, Boyden) ermoeglicht Lichtkontrolle von Neuronen. Welches erste therapeutische Anwendungsgebiet beim Menschen wurde 2021 erprobt, und welches Ergebnis wurde im Nature Medicine veroeffentlicht?",
        answerA = "Teilweise Sehwiederherstellung bei Netzhautdegeneration (Retinitis pigmentosa): Channelrhodopsin-Variante (ChrimsonR) in restliche retinale Ganglienzellen eingeschleust via AAV-Injektion; Patient wahrnahm nach Monaten mit spezieller Lichtbrille Objekte -- erste klinische Optogenetik-Erfolg beim Menschen",
        answerB = "Schmerztherapie bei Phantomschmerz: Halorhodopsin in periphere Nozizeptoren injiziert, Schmerz durch gruenes Licht supprimiert -- 12/12 Patienten zeigten vollstaendige Schmerzfreiheit",
        answerC = "Parkinson-Therapie: ChR2 in Subthalamischen Nucleus eingeschleust als Alternative zu DBS; Zittern wurde durch blaues Licht im gleichen Mass kontrolliert wie elektrische Stimulation",
        answerD = "Depression-Behandlung: Halorhodopsin in den praefrontalen Kortex injiziert; Patienten mit therapieresistenter Depression zeigten vollstaendige Remission nach lichtbasierter Inhibition",
        correctAnswer = 0,
        explanation = "Sahel et al. (Nature Medicine, 2021) behandelten einen Patienten mit fortgeschrittener Retinitis pigmentosa. AAV2-7m8 uebertrug das ChrimsonR-Gen (rotlicht-sensitive Kanalrhodopsin-Variante) in retinale Ganglienzellen. Nach ca. 7 Monaten Trainingsprozess mit einer speziellen Lichtverstaerkungsbrille (die Umgebungslicht in 590-nm-Impulse konvertiert) nahm der Patient Objekte wahr und identifizierte einen Gegenstand auf einem Tisch. Es war der erste Beweis fuer optogenetisch-vermittelte visuelle Wahrnehmung beim Menschen.",
        difficulty = 5,
        funFact = "Karl Deisseroth und Ed Boyden erhielten 2021 den Lasker-DeBakey-Preis -- oft als Vorstufe des Nobelpreises gesehen. Channelrhodopsine wurden urspruenglich in der Gruenalge Chlamydomonas reinhardtii entdeckt; ihre Anwendung in Saeuger-Neuronen war die entscheidende Idee Deisseroths."
    ),

    // Question 44 - Nanotechnologie in der Medizin
    Question(
        categoryId = 16,
        questionText = "Lipid-Nanopartikel (LNPs) sind der Schluessel fuer mRNA-Impfstoff-Delivery. Welche vier Komponenten bilden typischerweise LNPs (am Beispiel BNT162b2/mRNA-1273), und welche physiochemische Eigenschaft ist entscheidend fuer endosomales Escape?",
        answerA = "Ionisierbare Lipide (z.B. ALC-0315 oder SM-102), Phospholipide (DSPC), Cholesterol und PEG-Lipide; die ionisierbaren Lipide sind bei pH 7,4 neutral (keine Ladung im Blut), nehmen aber im sauren Endosom (pH ~5.5) positive Ladung an, was Membranfusion und endosomales Escape ermoeglicht",
        answerB = "Kationische Lipide, Phospholipide, Triglycerin und PLGA-Polymere; kationische Lipide sind stets positiv geladen und binden direkt an negativ geladene mRNA",
        answerC = "Sphingolipide, Ceramid, Cholesterol und Apolipoprotein E; ApoE vermittelt Leber-Targeting und ermoeglicht hepatisches Uptake via LDL-Rezeptor",
        answerD = "Glycerophospholipide, Lysophospholipide, Sphingomyelin und PEI-Polymer; PEI (Polyethylenimine) ist das aktive Endosom-Escape-Agens durch Protonenschwamm-Mechanismus",
        correctAnswer = 0,
        explanation = "LNPs bestehen aus: (1) Ionisierbarem Lipid -- bei physiologischem pH neutral, bei endosomalem pH positiv geladen; (2) Phospholipid (DSPC) -- Membranstruktur; (3) Cholesterol -- Membranstabilitaet; (4) PEG-Lipid (PEG2000-DMG oder ALC-0159) -- sterische Stabilisierung und lange Zirkulation. Der pH-sensitive Mechanismus der ionisierbaren Lipide ist entscheidend: Im Endosom protonoieren sie, stoeren die endosomale Membran durch inversen Hexagonal-Phasenuebergang und setzen mRNA ins Zytoplasma frei.",
        difficulty = 5,
        funFact = "ALC-0315 (Pfizer/BioNTech) und SM-102 (Moderna) sind die ionisierbaren Lipide der COVID-Impfstoffe. Ihre Entwicklung basiert auf Jahrzehnten Lipid-Nanopartikel-Forschung fuer siRNA-Delivery -- Patisiran (Onpattro, 2018) war der erste FDA-zugelassene siRNA-LNP-Wirkstoff, ein direkter Vorlaeufer der Impfstoff-LNP-Technologie."
    ),

    // Question 45 - Senolyse und Altersforschung
    Question(
        categoryId = 16,
        questionText = "David Sinclairs 'Information Theory of Aging' besagt, dass Altern primaer durch Verlust epigenetischer Information entsteht. Welche experimentelle Beobachtung aus Sinclairs Labor (Cell, 2023) unterstuetzte diese Theorie am dramatischsten?",
        answerA = "Durch gezieltes epigenetisches 'Rauschen' (ICE -- Inducible Changes in the Epigenome durch DSB-Reparatur) wurden Maeuse vorzeitig gealtert; anschliessende epigenetische Reprogrammierung durch partielle Yamanaka-Faktor-Expression (OSK ohne c-Myc) stellte junges Genexpressionsmuster und visuelle Funktion wieder her",
        answerB = "Sinclair klonierte Thelomere durch TERT-Ueberexpression in Maeusen und erzielte eine Lebensverlaengerung von 40 % -- ohne Erhoehung des Tumorrisikos durch Kombination mit TP53-Superexpression",
        answerC = "Durch vollstaendige In-vivo-Reprogrammierung mit allen vier Yamanaka-Faktoren (OKSM) wurden 2-jaehrige Maeuse auf das biologische Alter von 6-Monats-Maeuse zurueckgesetzt -- ohne jegliche Nebenwirkungen",
        answerD = "Sinclair identifizierte NAD+-Abfall als einzige Ursache des Alterns; NAD+-Supplementierung durch NMN erzielte vollstaendige Verjuengung in allen Maeuse-Organen gleichzeitig",
        correctAnswer = 0,
        explanation = "Yang et al. (Cell, 2023) verwendeten das ICE-System: Durch nicht-mutagene DSBs (I-SceI-Endonuklease-Schnittstellen in bestimmten Loci) wurde epigenetisches Rauschen erzeugt -- Maeuse alterten vorzeitig (Methylom, Transkriptom, Physiologie). Dann wurde durch AAV-vermittelte Expression von Oct4, Sox2 und Klf4 (OSK-Faktoren, ohne c-Myc) die epigenetische Uhr zurueckgestellt. Sehvermoegensverlust bei aelteren Maeuse und nach ICE wurde durch OSK-Therapie partiell restauriert -- ein Proof-of-Concept fuer 'epigenetische Reprogrammierung als Antiaging-Therapie'.",
        difficulty = 5,
        funFact = "Die Frage, ob vollstaendige Reprogrammierung (alle 4 Yamanaka-Faktoren) zu iPSC und Krebsrisiko fuehrt, loeste Sinclair durch Wahl von OSK (ohne c-Myc). Juan Carlos Belmonte-Izpisua zeigte 2016 in Nature, dass kurze in-vivo-OSKM-Pulse Progerien-Maeuse verjuengen koennen -- aber auch zu Teratomen fuehren, wenn zu lang aktiviert."
    ),

    // Question 46 - Neuartige Antibiotika
    Question(
        categoryId = 16,
        questionText = "Teixobactin (2015, Lewis/Ling) wurde als erstes neues Antibiotikum seit Jahrzehnten entdeckt, das keine Resistenz erzeugt. Welcher Mechanismus erklaert die Resistenzarmut, und wie wurde Teixobactin entdeckt?",
        answerA = "Teixobactin bindet an Lipid-II (den Pyrophosphat-Precursor der Zellwandbiosynthese) und Lipid-III (fuer Teichonsaeure-Biosynthese) -- beides sind unveraenderliche Lipid-Substrate statt Protein-Enzyme, Mutationen koennen die Bindung kaum verhindern. Entdeckung: iChip-Technologie kultiviert unkultivierbare Bodenbakterien in situ",
        answerB = "Teixobactin ist ein Ribosom-Inhibitor, der an 23S-rRNA bindet; Resistenz scheitert weil mehrere rRNA-Gene gleichzeitig mutieren muessen. Entdeckung: Hochdurchsatz-Screening einer Naturstoff-Bibliothek",
        answerC = "Teixobactin bildet Membranporen durch Oligomerisierung -- Resistenz erfordert vollstaendige Aenderung der Membranzusammensetzung, was fuer Bakterien letal waere. Entdeckung: Synthetische Chemie basierend auf Vancomycin-Strukturanalogien",
        answerD = "Teixobactin inaktiviert Beta-Laktamasen irreversibel durch kovalente Bindung; da Beta-Laktamasen durch horizontalen Gentransfer erworben werden, blockiert Teixobactin diesen Transfer durch Integrase-Hemmung",
        correctAnswer = 0,
        explanation = "Teixobactin, produziert von Eleftheria terrae, bindet an Lipid-II (Undecaprenylpyrophosphat-MurNAc-Pentapeptid) und Lipid-III (Undecaprenylpyrophosphat-GlcNAc), die Substrate fuer Peptidoglykan- und Wandteichonsaeure-Biosynthese. Diese Lipide sind evolutionaer hochkonserviert -- Mutationen in den Enzymen, die sie herstellen, beeintraechtigen kaum die Teixobactin-Bindung. iChip (isolation chip) von Slava Epstein ermoeglicht Kultivierung von >50 % bisher unkultivierbarer Bodenmikroben in situ.",
        difficulty = 5,
        funFact = "Schatzungen zufolge sind nur 1 % aller Bodenbakterien mit klassischen Methoden kultivierbar -- die anderen 99 % (das 'Dunkle Mikrobiom') bleiben ein riesiges ungenutztes Reservoir fuer neue Antibiotika. iChip koennte diesen blinden Fleck erschliessen -- bisher wurden nach Teixobactin weitere neue Klassen entdeckt (Darobactin, Clovibactin)."
    ),

    // Question 47 - Transplantationsimmunologie HLA-Matching
    Question(
        categoryId = 16,
        questionText = "Das HLA-System (MHC beim Menschen) ist entscheidend fuer Transplantationserfolg. Welche HLA-Loci werden standardmaessig fuer Nieren-Transplantation gematcht (sog. 'six-antigen-match'), und was ist 'virtual crossmatch' im modernen Transplantationswesen?",
        answerA = "Standardmatching: HLA-A, -B (Klasse I) und HLA-DR (Klasse II) -- je 2 Allele pro Locus ergibt 6 Antigene. Virtual Crossmatch: Bekannte Spender-HLA-Typen werden gegen Empfaenger-spezifische Antikoerper (DSA via Luminex-Einzelantigentests) in silico geprueft -- physische Lymphozyten-Kreuzprobe wird damit oft ersetzt",
        answerB = "Matching: HLA-A, -B, -C, -DR, -DQ, -DP -- alle 6 Loci; Virtual Crossmatch ist ein bioinformatisches Tool zur Vorhersage des Abstossungsrisikos basierend auf Genomsequenzierung des Empfaengers",
        answerC = "Standardmatching: Nur HLA-DR (1 Locus) ist transplantationsrelevant; A und B werden fuer Bluttransfusionen, nicht fuer Organe gematcht. Virtual Crossmatch basiert auf Kalziumfluenz-Assays an Empfaenger-Lymphozyten",
        answerD = "HLA-Matching ist bei moderner Immunsuppression (Tacrolimus + MMF) klinisch obsolet geworden; der 'Virtual Match' bezeichnet stattdessen die KI-basierte Berechnung der optimalen Tacrolimus-Dosis",
        correctAnswer = 0,
        explanation = "Der klassische 'six-antigen-match' bei Nierentransplantation erfasst HLA-A, HLA-B (Klasse I, je 2 Antigene) und HLA-DR (Klasse II, 2 Antigene). Zusaetzlich werden heute oft HLA-DQ und -C beruecksichtigt. Der Virtual Crossmatch nutzt Luminex-Einzelantigen-Bead-Assays, um Empfaenger-Antikoerper gegen alle bekannten HLA-Antigene zu screenen. Wenn Spender-HLA-Typen bekannt sind, kann das Labor ohne physischen Zellkontakt in silico pruefen, ob Donor-Spezifische Antikoerper (DSA) vorhanden sind -- das beschleunigt die Organallokation erheblich.",
        difficulty = 5,
        funFact = "0-Antigen-Mismatch-Transplantationen (Spender und Empfaenger stimmen an allen 6 klassischen HLA-Antigenen ueberein) haben nach 10 Jahren ein Transplantat-Ueberleben von ca. 70 % vs. ca. 55 % bei 6 Mismatches -- trotz moderner Immunsuppression bleibt HLA-Kompatibilitaet klinisch relevant."
    ),

    // Question 48 - Epigenetik und Krebsentstehung
    Question(
        categoryId = 16,
        questionText = "Epigenetische Veraenderungen spielen bei der Krebsentstehung eine fundamentale Rolle. Welche spezifische epigenetische Veraenderung wurde als 'driver' in CIMP-positiven Kolorektalkarzinomen (CpG Island Methylator Phenotype) identifiziert, und welches Mismatch-Repair-Gen ist besonders betroffen?",
        answerA = "Hyperemethylierung der Promotor-CpG-Inseln des MLH1-Gens silenziert das Mismatch-Repair-Protein MLH1 -- dadurch entsteht Mikrosatelliteninstabilitaet (MSI-H), die in CIMP-positiven Tumoren einen Grossteil der Lynch-Syndrom-aehnlichen Phaenotypen ohne Keimbahnmutation erklaert",
        answerB = "Hypomethylierung von KRAS-Enhancern fuehrt zu Ueberexpression -- KRAS-Hyperaktivierung ist der CIMP-spezifische Onkogen-Aktivierungsmechanismus",
        answerC = "Hypermethylierung von H3K4me3-Histonmarkern an Tumorsuppressor-Promotoren ersetzt DNA-Methylierung als CIMP-Treiber; MLH1 ist dabei nicht betroffen",
        answerD = "CIMP-positive Tumore zeigen globale Histon-Deacetylierung durch HDAC-Amplifikation; Mismatch-Repair-Defizite entstehen durch post-transkriptionelle MLH1-Suppression via miRNA-155",
        correctAnswer = 0,
        explanation = "CIMP in kolorektalen Karzinomen ist durch simultane Hypermethylierung multipler Promotor-CpG-Inseln charakterisiert. Das wichtigste Zielgen ist MLH1 (MutL Homolog 1): Promotor-Methylierung schaltet MLH1-Expression ab, das MMR-System versagt, und es entsteht Mikrosatelliteninstabilitaet (MSI-High). Diese sporadischen MSI-H-Tumore ahmen den Lynch-Syndrom-Phaenotyp nach (Keimbahnmutationen in MLH1, MSH2 usw.) -- sind aber epigenetisch, nicht genetisch verursacht.",
        difficulty = 5,
        funFact = "MSI-H-Tumoren -- ob durch CIMP oder Lynch-Syndrom verursacht -- sprechen besonders gut auf PD-1-Checkpoint-Inhibitoren an. Das FDA-Approval von Pembrolizumab 2017 fuer MSI-H/dMMR-Tumore war das erste Tumor-agnostische Zulassung -- unabhaengig von der Organlokalisation, allein basierend auf einem molekularen Biomarker."
    ),

    // Question 49 - Neuroimmunologie und MS
    Question(
        categoryId = 16,
        questionText = "Bei Multipler Sklerose (MS) entscheidet die Immunpathologie ueber Verlauf und Therapie. Welche zellulare Hypothese dominiert aktuell die progressive MS-Forschung, und welchen Mechanismus nutzt Ocrelizumab (anti-CD20) in der pPPMS-Behandlung?",
        answerA = "Meningeale lymphoide Aggregate (ektopische B-Zell-Follikel) in subarachnoidalen Raeumen unterhalten progressive kortikale Demyelinisierung durch lokale Antigen-Praesentation und Zytokin-Sekretion -- Ocrelizumab depleziert reife B-Zellen und Plasmazellen via ADCC/CDC, was die lokale CNS-B-Zell-Population reduziert",
        answerB = "Mikroglia-Hyperaktivierung mit M1-Polarisierung ist der Haupttreiber progressiver MS; Ocrelizumab wirkt indirekt durch Reduktion peripherer T-Helfer-Zellen, die Mikroglia aktivieren",
        answerC = "Axonaler Kalzium-Influx durch ephaptische Leitung an demyelinisierten Axonen verursacht Neurodegeneration; Ocrelizumab blockiert NMDA-Rezeptoren auf Axonen",
        answerD = "Astrozyten-Reaktivitaet (A1-Astrozyten) ist der alleinige Treiber progressiver MS; B-Zellen spielen bei pPPMS keine Rolle, weshalb Ocrelizumab in ppMS durch Blockade von LIF-Signaling an Astrozyten wirkt",
        correctAnswer = 0,
        explanation = "Die Entdeckung meningealer lymphoider Aggregate (ektopische tertiare Lymphoid-Strukturen) im subarachnoidalen Raum progressiver MS-Patienten durch Magliozzi et al. (2007) war wegweisend. Diese Strukturen, beherbergen B-Zellen, T-Zellen und Follikulaere dendritische Zellen, produzieren Antikoeoper und unterhalten subpiale kortikale Demyelinisierung. Ocrelizumab (anti-CD20-Antikoerper) eliminiert CD20+ B-Zellen und Gedaechtnis-B-Zellen durch ADCC/CDC und war das erste zugelassene Medikament fuer primaer progressive MS (pPPMS).",
        difficulty = 5,
        funFact = "CD20 wird von reifen B-Zellen exprimiert, aber nicht von Plasmazellen (die IgG produzieren) oder pro-B-Zellen. Daher wirkt Ocrelizumab praefeenziell auf den Antigen-praesentation-kompetenten B-Zell-Arm und weniger auf das Antikoeorper-Repertoire selbst -- erklaert, warum MS-Therapie mit anti-CD20 ohne drastische Hypogammaglobulinaemie moeglich ist."
    ),

    // Question 50 - Fruehgeschichte der Epidemiologie
    Question(
        categoryId = 16,
        questionText = "John Snows Untersuchung der Broad-Street-Cholera-Epidemie 1854 gilt als Gruendungsakt der modernen Epidemiologie. Welche methodische Innovation setzte Snow ein, die noch heute Grundlage epidemiologischer Felduntersuchungen ist, und welchen mechanistischen Fehler enthielt seine Schlussfolgerung trotz korrekter praventiver Massnahme?",
        answerA = "Snow nutzte raeumliche Dot-Maps (geocodierte Fallverteilung) und ein natuerliches Experiment (Wasserversorgungs-Quasi-Randomisierung zweier Wasserwerke in London) als methodische Innovationen; sein Fehler: Er identifizierte Wasser als Uebertragungsweg korrekt, glaubte aber an eine 'Choleramiasmen' im Wasser -- das Bakterium Vibrio cholerae wurde erst 1883 durch Robert Koch identifiziert",
        answerB = "Snow nutzte erstmals kontrollierte Experimente mit Placebo-Gruppen und entdeckte dabei die Doppelblindmethode; sein Fehler lag in der Kontamination der Kontrollgruppe",
        answerC = "Snow einfuehrte die Odds Ratio als statistisches Mass -- sein Fehler war die falsche Berechnung der Bevoelkerungsdichte, was seine Ergebnisse quantitativ verfaelschte",
        answerD = "Snow verwendete erstmals serologische Tests (Antikoeorpertiter im Serum exponierter Personen) zur Fallidentifikation; sein Irrtum: Er nahm an, Cholera werde durch Atemluft uebertragen",
        correctAnswer = 0,
        explanation = "Snows zwei methodische Innovationen: (1) Spot-Maps der Cholerafaelle um die Broad-Street-Pumpe -- eine der ersten geographischen Krankheitsdarstellungen; (2) Vergleich der Choleramortalitaet in Haushalten, die von der Southwark & Vauxhall Company (Themse-Wasser stromabwaerts Abwaesser) vs. Lambeth Company (saubereres Wasser stromaufwaerts) beliefert wurden -- ein naturliches Quasi-Experiment. Snow erkannte fakalienverunreinigtes Wasser als Ursache, nicht Miasmen. Das Konzept 'specifischer Krankheitserreger' war noch nicht etabliert -- Koch-Postulate und Vibrio cholerae kamen erst 1883.",
        difficulty = 5,
        funFact = "Snow entfernte den Pumpenschwengel der Broad-Street-Pumpe -- eine der bekanntesten praventivmedizinischen Massnahmen der Geschichte. Ironie: Die Epidemie klang ohnehin gerade ab. Trotzdem war der symbolische Akt ein Meilenstein: Praevention ohne Kenntnis des spezifischen Erregers, allein durch epidemiologisches Reasoning."
    )

)

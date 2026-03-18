package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun healthQuestionsMaster3(): List<Question> = listOf(

    // --- KARDIALE ELEKTROPHYSIOLOGIE ---

    // Question 1 - Natriumkanal-Inaktivierungstor
    Question(
        categoryId = 16,
        questionText = "Welches strukturelle Merkmal des spannungsgesteuerten Natriumkanals (Nav1.5) ist fuer die schnelle Inaktivierung nach Depolarisation verantwortlich?",
        answerA = "Die S4-Segmente aller vier Domaenen falten sich nach innen und blockieren die Pore",
        answerB = "Das IFM-Motiv (Isoleucin-Phenylalanin-Methionin) des intrazellularen DIII-DIV-Linkers blockiert den Kanal wie ein Ball-and-Chain",
        answerC = "Calmodulin bindet an das C-terminale EF-Hand-Motiv und zieht die Aktivierungsschleife zurueck",
        answerD = "Phosphorylierung durch PKA inaktiviert den Kanal durch Konformationsaenderung der Selektivitaetsfilterregion",
        correctAnswer = 1,
        explanation = "Die schnelle (N-Typ) Inaktivierung von Nav1.5 erfolgt durch den hydrophoben IFM-Trimethy am DIII-DIV-Linker, der wie ein Stoessel den inneren Kanalmund blockiert (Ball-and-Chain-Mechanismus). Mutationen in diesem Motiv zerstoeren die schnelle Inaktivierung vollstaendig.",
        difficulty = 5,
        funFact = "Mutationen im SCN5A-Gen, das Nav1.5 kodiert, verursachen mindestens sechs verschiedene Kanalerkrankungen: Brugada-Syndrom, Long-QT-Syndrom Typ 3, progressives kardiakales Leitungsdefizit, Sick-Sinus-Syndrom, dilatativer Kardiomyopathie und atrialer Standstill."
    ),

    // Question 2 - Funny Current If
    Question(
        categoryId = 16,
        questionText = "Welcher Ionenkanal traegt den sogenannten 'Funny Current' (If) im Sinusknoten, und welche pharmakologische Klasse hemmt ihn selektiv?",
        answerA = "HCN4-Kanal (hyperpolarisationsaktivierter zyklisch-Nukleotid-regulierter Kanal); Ivabradin",
        answerB = "Kir3.1/3.4 (GIRK-Kanal); Adenosin",
        answerC = "Cav1.3 (L-Typ-Calciumkanal); Verapamil",
        answerD = "Kv4.3 (Ito-Kanal); Flecainid",
        correctAnswer = 0,
        explanation = "HCN4 ist der primaere Schrittmacherkanal im Sinusknoten -- er oeffnet bei Hyperpolarisation und traegt einen einwaerts gerichteten Na+/K+-Mischstrom (If), der die diastolische Depolarisation antreibt. Ivabradin hemmt selektiv HCN4 von innen und senkt die Herzfrequenz ohne Inotropie oder Blutdruck zu beeinflussen.",
        difficulty = 5,
        funFact = "Gain-of-Function-Mutationen im HCN4-Gen koennen paradoxerweise Bradykardie verursachen -- durch veraenderte cAMP-Sensitivitaet wird der Kanal bei Ruhe zu stark aktiviert und stoert den normalen Schrittmacherrhythmus."
    ),

    // Question 3 - Reentry-Kreislauf
    Question(
        categoryId = 16,
        questionText = "Welche drei Bedingungen muessen fuer einen funktionellen Reentry-Kreislauf im Myokard gleichzeitig erfuellt sein?",
        answerA = "Heterogene Refraktaerperioden, einseitiger Leitungsblock und ausreichend langsame Leitungsgeschwindigkeit",
        answerB = "Homogene Refraktaerperioden, beidseitiger Leitungsblock und hohe Leitungsgeschwindigkeit",
        answerC = "Erhoehte Automatie, getriggerte Aktivitaet und Nachpotentiale",
        answerD = "Hypokaliemie, Hypokalzaemie und Hypomagnesiemie gleichzeitig",
        correctAnswer = 0,
        explanation = "Reentry erfordert: (1) eine Leitungsschleife, (2) einen unidirektionalen Block in einem Schenkel (einseitig, oft durch heterogene Refraktaerperioden), und (3) ausreichend langsame Leitung im anderen Schenkel, damit der blockierte Schenkel Zeit hat, seine Refraktaerperiode zu beenden und das Impuls retrograd zu leiten.",
        difficulty = 5,
        funFact = "Die Wellenfronttheorie von Allessie (1977) zeigte erstmals, dass ein anatomisches Hindernis nicht noetig ist -- ein rein funktioneller Reentry kann durch heterogene Erregbarkeit entstehen, was erklaert, warum Kammerflimmern ohne strukturelle Herzerkrankung auftreten kann."
    ),

    // Question 4 - Langes QT-Syndrom Typ 2
    Question(
        categoryId = 16,
        questionText = "LQTS Typ 2 wird durch Mutationen im KCNH2-Gen (hERG) verursacht. Welche spezifische pharmakologische Gefahr besteht bei diesen Patienten, und warum?",
        answerA = "QRS-Verbreiterung durch Natriumkanal-Blockade -- hERG-Mutanten sind ueberempfindlich gegen Klasse-I-Antiarrhythmika",
        answerB = "Acquired LQTS durch viele Nicht-Herzmedikamente, da hERG einen atypisch grossen Bindepocket hat, in den diverse Molekuele eintreten und IKr blockieren koennen",
        answerC = "AV-Leitungsblockierung -- hERG-Mutationen beeintraechtigen auch den AV-Knoten und erhoehen die AV-Leitungszeit",
        answerD = "Erhoehte Empfindlichkeit gegenueber Hyperkaliemie, da hERG-Kanaele als K+-Sensoren fungieren",
        correctAnswer = 1,
        explanation = "Der hERG-Kanal hat eine ungewoehnlich grosse innere Pore mit aromatischen Resten (Y652, F656), die viele structurally diverse Medikamente binden -- von Antihistaminika (Terfenadin) ueber Antibiotika (Azithromycin) bis zu Antipsychotika (Haloperidol). KCNH2-Traeger mit reduzierter IKr-Reserve sind besonders anfaellig fuer medikamentenbedingtes Torsades de Pointes.",
        difficulty = 5,
        funFact = "Das kardiotoxische Antihistaminikum Terfenadin wurde 1997 vom Markt genommen, weil es hERG blockiert und bei Coadministration mit CYP3A4-Inhibitoren (Ketoconazol, Erythromycin) toedliche Torsades verursachte -- dies gab den Anstoss zur heutigen Pflicht-hERG-Testung aller Neuentwicklungen."
    ),

    // Question 5 - Aktionspotential Plateau-Phase
    Question(
        categoryId = 16,
        questionText = "Welcher Kalziumstrom ist primaer fuer die Plateau-Phase (Phase 2) des ventrikulaeren Aktionspotentials verantwortlich, und welche Rezeptoren modulieren ihn am staerksten?",
        answerA = "T-Typ-Calciumstrom (ICaT); moduliert durch alpha1-Adrenorezeptoren und Endothelin",
        answerB = "L-Typ-Calciumstrom (ICaL, Cav1.2); moduliert durch beta1-Adrenorezeptoren (cAMP/PKA) und Muscarin-M2-Rezeptoren",
        answerC = "LTCC-unabhaengiger Ca2+-Einstrom durch NCX im Rueckwaertsmodus; reguliert durch Na+-Konzentration",
        answerD = "Ryanodin-Rezeptor (RyR2)-vermittelte Ca2+-Freisetzung aus dem SR; direkt durch Phospholamban reguliert",
        correctAnswer = 1,
        explanation = "Der L-Typ-Calciumkanal (Cav1.2, kodiert durch CACNA1C) erzeugt den langen Einwaertsstrom der Plateau-Phase. Beta1-Adrenorezeptoren steigern via Gs/cAMP/PKA die Phosphorylierung des Kanals und erhoehen so den Spitzenstrom sowie die Ca2+-induzierte Ca2+-Freisetzung (CICR) aus dem SR.",
        difficulty = 5,
        funFact = "Timothy-Syndrom (LQTS Typ 8) wird durch eine Gain-of-Function-Mutation (G406R) in CACNA1C verursacht, die die Inaktivierung des L-Typ-Kanals verhindert -- Betroffene zeigen QT-Verlaengerung, Syndaktylie, Autismus-Spektrum und Immundefekte, eine ungewoehnliche Kombination, die zeigt, wie weit Cav1.2 im Koerper exprimiert wird."
    ),

    // --- FORTGESCHRITTENE HERZINSUFFIZIENZ ---

    // Question 6 - LVAD-Prinzip
    Question(
        categoryId = 16,
        questionText = "Welcher haemodynamische Parameter ist bei einem kontinuierlich arbeitenden LVAD (z.B. HeartMate 3) typischerweise veraendert, und welche klinische Konsequenz ergibt sich daraus?",
        answerA = "Erhoehter pulmonalkapillarer Verschlussdruck durch retrograden Fluss; Lungenoedem als Konsequenz",
        answerB = "Fehlende oder stark gedaempfte Pulsdruck-Welligkeit (non-pulsatile flow); Aortenklappe kann permanent geschlossen bleiben und entwickelt eine Fusion der Segel",
        answerC = "Dauerhaft erhoehter mittlerer arterieller Druck; Hypertensive Enzephalopathie als Hauptkomplikation",
        answerD = "Reduzierte linksventrikulaere Fuellung durch Saugeffekt; Kollaps der Mitralklappe als Konsequenz",
        correctAnswer = 1,
        explanation = "Axialfluss-LVADs erzeugen kontinuierlichen, nicht-pulsatilen Fluss. Da der LV entlastet wird und wenig Volumen pumpt, bleibt die Aortenklappe oft dauerhaft geschlossen -- bei prolongiertem LVAD-Einsatz koennen die Klappensegel fusionieren (Commissurenfusion), was bei spaeterer Explantation eine Aortenklappeninsuffizienz hinterlassen kann.",
        difficulty = 5,
        funFact = "Das HeartMate 3 ist das erste LVAD mit einem integrierten 'artifiziellen Puls' -- alle zwei Sekunden wird die Pumpdrehzahl kurz gesenkt, um pulsatilen Fluss zu simulieren und die Aortenklappe periodisch zu oeffnen, was die Rate der Aortenklappe-Fusion drastisch reduziert hat."
    ),

    // Question 7 - Herztransplantation: Kontraindikationen
    Question(
        categoryId = 16,
        questionText = "Welcher haemodynamische Parameter ist die wichtigste Kontraindikation fuer eine orthotope Herztransplantation, und welchen Grenzwert gilt als absolute Kontraindikation?",
        answerA = "Erhoehter systemischer Gefaesswiderstand (SVR > 2000 dyn*s*cm-5); Spenderherz toleriert hohen Afterload nicht",
        answerB = "Fixierter pulmonaler Gefaesswiderstand (PVR) > 5-6 Wood-Einheiten oder transpulmonaler Gradient > 15 mmHg; Rechtsversagen des Spenderherzens",
        answerC = "Erniedrigte linksventrikulaere Ejektionsfraktion < 10%; zu wenig residualer Eigenleistung fuer Ueberbrueckung",
        answerD = "Pulmonalkapillaerer Verschlussdruck (PCWP) > 30 mmHg ohne Vasodilatatoren; Oedemrisiko im Transplantat",
        correctAnswer = 1,
        explanation = "Ein fixes PVR > 5-6 Wood-Einheiten (oder TPG > 15 mmHg), der nicht auf Vasodilatatoren (NO, Prostacyclin, Nitroprussid) anspricht, ist eine absolute Kontraindikation, da das normale Spenderherz akutes Rechtsherzversagen entwickelt, wenn es gegen den hohen pulmonalen Widerstand pumpen muss.",
        difficulty = 5,
        funFact = "Patienten mit grenzwertigem PVR (3-5 Wood-Einheiten) koennen ueberbrueckend ein LVAD erhalten -- die kardiale Dekompression senkt den PCWP und oft auch den pulmonalen Widerstand, sodass nach Monaten eine Transplantation moeglich wird."
    ),

    // Question 8 - Bridge to Transplant vs. Destination Therapy
    Question(
        categoryId = 16,
        questionText = "Was unterscheidet 'Bridge to Transplant' von 'Destination Therapy' beim LVAD-Einsatz, und welches Kriterium schliesst Destination Therapy per Definition aus?",
        answerA = "Bridge to Transplant setzt volle Transplantationseignung voraus; Destination Therapy ist fuer Patienten, die nie transplantierbar sein werden (z.B. irreversibler PVR, Alter, Komorbiditaeten)",
        answerB = "Bridge to Transplant nutzt nur externe LVADs; Destination Therapy nur implantierbare Systeme",
        answerC = "Destination Therapy ist zeitlich auf 2 Jahre begrenzt; Bridge to Transplant hat keine Zeitlimitierung",
        answerD = "Bridge to Transplant erfordert INTERMACS-Profil 1-2; Destination Therapy wird nur bei Profil 5-7 eingesetzt",
        correctAnswer = 0,
        explanation = "Bridge to Transplant impliziert, dass der Patient als Transplantationskandidat gelistet ist oder werden kann. Destination Therapy hingegen bezeichnet den dauerhaften LVAD-Einsatz als Endtherapie bei Patienten, die aus medizinischen Gruenden (z.B. fixierter PVR, Malignome in Remission, hohes Alter, psychosoziale Faktoren) nie fuer eine Transplantation infrage kommen.",
        difficulty = 5,
        funFact = "Die REMATCH-Studie (2001) etablierte Destination Therapy als legitime Option -- LVAD-Patienten hatten nach 2 Jahren eine doppelt so hohe Ueberlebensrate wie optimal medikamentoes behandelte Kontrollpatienten (23% vs. 8%), trotz hoher Komplikationsraten."
    ),

    // --- PULMONALE HYPERTONIE ---

    // Question 9 - WHO-Klassifikation PH
    Question(
        categoryId = 16,
        questionText = "Ein Patient hat idiopathische pulmonale arterielle Hypertonie (IPAH). Welche WHO-Gruppe entspricht das, und was unterscheidet diese Gruppe pathophysiologisch von WHO-Gruppe 2?",
        answerA = "WHO-Gruppe 3 (Hypoxie); Gruppe 2 hat Linksherz-Erkrankung als Ursache, Gruppe 3 hat primaere Lungenparenchy-Erkrankung",
        answerB = "WHO-Gruppe 1 (PAH); Gruppe 2 entsteht durch erhoeht linkskardialen Fuellungsdruck (passiv-venos), waehrend Gruppe 1 praekapillare Obstruktion durch vaskulaeres Remodeling hat",
        answerC = "WHO-Gruppe 4 (CTEPH); Gruppe 2 hat akute Embolien, Gruppe 4 hat chronisch-thromboembolische Obstruktion",
        answerD = "WHO-Gruppe 5 (unklare Ursache); Gruppe 2 hat bekannte Ursache, Gruppe 5 ist idiopathisch wie IPAH",
        correctAnswer = 1,
        explanation = "IPAH gehoert zu WHO-Gruppe 1 (PAH) -- praekapillare PH durch vaskulaeres Remodeling der kleinen Pulmonalarterien (Media-Hypertrophie, Intimaproliferation, plexiforme Laesionen). WHO-Gruppe 2 entsteht durch erhoeht linkskardialen Fuellungsdruck, der passiv auf die pulmonale Strombahn uebertragen wird (post-kapillaere PH).",
        difficulty = 5,
        funFact = "In 70-80% der hereditaeren PAH-Faelle finden sich Mutationen im BMPR2-Gen (Bone Morphogenetic Protein Receptor 2), einem Rezeptor des TGF-beta-Signalwegs -- nur 20% der Mutationstraeger entwickeln jedoch eine klinische PH, was zeigt, dass weitere Triggerfaktoren noetig sind."
    ),

    // Question 10 - Prostacyclin-Signalweg
    Question(
        categoryId = 16,
        questionText = "Wie wirkt Epoprostenol (Prostacyclin I2) auf die pulmonale Gefaessmuskulatur, und warum muss es kontinuierlich intravenoees verabreicht werden?",
        answerA = "Epoprostenol hemmt Endothelin-Rezeptoren kompetitiv; Halbwertszeit 4-6 Stunden erlaubt taeglich einmalige i.v.-Gabe",
        answerB = "Epoprostenol bindet IP-Rezeptoren, erhoehen cAMP via Gs/Adenylcyclase, bewirkt Vasodilatation und hemmt Thrombozytenaggregation; Halbwertszeit von 2-3 Minuten erzwingt kontinuierliche Infusion",
        answerC = "Epoprostenol aktiviert sGC und erhoehen cGMP, aehnlich wie NO; orale Resorption ist gut, aber hepatischer First-Pass zu hoch fuer orale Gabe",
        answerD = "Epoprostenol blockiert Phosphodiesterase-5 und verlaengert cGMP-Wirkung; muss kontinuierlich verabreicht werden, da es im Plasma sofort durch PDE5 abgebaut wird",
        correctAnswer = 1,
        explanation = "Prostacyclin bindet an IP-Rezeptoren (Gs-gekoppelt), aktiviert Adenylcyclase, erhoehen cAMP und aktiviert PKA -- dies bewirkt glattmuskulaere Relaxation und hemmt Thrombozytenaggregation. Die extrem kurze Plasma-Halbwertszeit (2-3 Minuten) durch enzymatische Hydrolyse und Spontanhydrolyse erzwingt eine permanente intravenoes Infusion ueber implantierten Katheter.",
        difficulty = 5,
        funFact = "Das abrupte Absetzen von Epoprostenol kann eine lebensbedrohliche Rebound-PH mit schnellen Rechtsversagen ausloesen -- Patienten tragen deshalb einen zweiten Reservepump und werden geschult, den Katheter niemals zu diskonnektieren, auch nicht im Schlaf."
    ),

    // Question 11 - Endothelin-Rezeptor-Antagonisten
    Question(
        categoryId = 16,
        questionText = "Was ist der Unterschied zwischen einem dualen Endothelin-Rezeptor-Antagonisten (Bosentan) und einem selektiven ETA-Antagonisten (Ambrisentan) hinsichtlich des ETB-Rezeptors?",
        answerA = "ETB-Blockade ist immer vorteilhaft; Ambrisentan blockiert ETB mit hoeherer Affinitaet als Bosentan",
        answerB = "Der ETB-Rezeptor auf Endothelzellen vermittelt ET-1-Clearance und NO/Prostacyclin-Freisetzung (vasoprotektiv); Ambrisentan blockiert nur ETA (vasokontriktorisch/proliferativ) und laesst ETB intakt, was theoretisch vorteilhafter ist",
        answerC = "Beide haben identische Wirkung; der Unterschied liegt nur in Pharmakokinetik und Dosierung",
        answerD = "ETB-Antagonismus durch Bosentan erhoehen NO-Synthese; Ambrisentan fehlt dieser Mechanismus",
        correctAnswer = 1,
        explanation = "ET-1 wirkt ueber ETA (Vasokonstriktion, Proliferation auf glatten Muskelzellen) und ETB (vasoprotektiv: NO/Prostacyclin-Freisetzung auf Endothelzellen und ET-1-Clearance). Ambrisentan als selektiver ETA-Antagonist laesst den protektiven ETB-Signalweg intakt. Klinische Studien zeigen jedoch aehnliche Effektivitaet beider Substanzklassen.",
        difficulty = 5,
        funFact = "Bosentan war das erste oral verfuegbare PAH-Medikament (2001) und revolutionierte die Therapie -- vor seiner Einfuehrung waren Patienten auf die schwierige Dauerinfusion von Epoprostenol angewiesen, was die Lebensqualitaet massiv beeintraechtigte."
    ),

    // --- BEATMUNG / ECMO ---

    // Question 12 - ECMO-Konfigurationen
    Question(
        categoryId = 16,
        questionText = "Was ist der fundamentale Unterschied zwischen venovenoeser (VV-ECMO) und venoarterioser (VA-ECMO) hinsichtlich kardialem Unterstuetzungseffekt?",
        answerA = "VV-ECMO unterstuetzt Herz und Lunge; VA-ECMO unterstuetzt nur die Lunge",
        answerB = "VV-ECMO unterstuetzt ausschliesslich den Gasaustausch (kein Kreislaufsupport); VA-ECMO unterstuetzt sowohl Gasaustausch als auch Kreislauf durch Blutentnahme venoes und Reinfusion in das arterielle System",
        answerC = "VV-ECMO hat hoehere Flussvoluemna und ist daher effektiver bei schwerem kardiogenen Schock",
        answerD = "VA-ECMO wird ausschliesslich bei Neugeborenen verwendet; VV-ECMO ist die Standardform bei Erwachsenen mit Herzversagen",
        correctAnswer = 1,
        explanation = "VV-ECMO drainiert venoes Blut, oxygeniert es und gibt es zurueck in die Vene -- der Sauerstofftransport verbessert sich, aber der Herzauswurf bleibt unveraendert. VA-ECMO drainiert venoes und pumpt in eine Arterie (typisch: Femoral), was den systemischen Fluss tatsaechlich mechanisch unterstuetzt und den linken Ventrikel entlastet.",
        difficulty = 5,
        funFact = "VA-ECMO kann paradoxerweise den linken Ventrikel ueberladen, wenn das Herz noch etwas pumpt -- der erhoehte arterielle Druck durch ECMO erhoehen den LV-Afterload. Daher wird bei kardiogenem Schock oft ein Impella-Pumpe kombiniert (ECMELLA), um den LV aktiv zu entlasten."
    ),

    // Question 13 - Rekrutierungsmaneuver
    Question(
        categoryId = 16,
        questionText = "Welcher physiologische Mechanismus liegt dem Atelektasenrekrutierungsmaneuver bei ARDS zugrunde, und was ist die Hauptkomplikation?",
        answerA = "Kurzfristige Hyperoxygenierung loest Surfactant-Freisetzung aus; Hauptkomplikation ist Hyperoxie-induzierte Absorption-Atelektase",
        answerB = "Kurzzeitige Erhoehung des transpulmonalen Drucks (z.B. 40 cmH2O CPAP fuer 40 Sekunden) oeffnet kollabierte Alveolen durch Ueberwindung der kritischen Oeffnungsdrucke; Hauptkomplikation ist barotraumainduzierter Pneumothorax und transienter Blutdruckabfall",
        answerC = "Prone Positioning erhoehen den transpulmonalen Druck durch Schwerkraftumlagerung; Hauptkomplikation ist Drucklaesionen der Haut",
        answerD = "Erhoeahung der Atemfrequenz ueberwindet Atelektasen durch erhoehten Air-trapping-Effekt; Hauptkomplikation ist Hyperkapnie",
        correctAnswer = 1,
        explanation = "Kollabierte Alveolen haben kritische Oeffnungsdrucke, die den normalen Beatmungsdruck uebersteigen. Ein Rekrutierungsmaneuver (z.B. Sustained Inflation oder stufenweise PEEP-Erhoehung) ueberwindet diese Oeffnungsdrucke transienter. Hauptrisiken sind Barotrauma (Pneumothorax) und haemodynamische Instabilitaet durch reduzierten venosen Rueckstrom.",
        difficulty = 5,
        funFact = "Die ART-Studie (2017) mit 1000 ARDS-Patienten zeigte ueberraschenderweise erhoehte 28-Tage-Mortalitaet bei aggressiven Rekrutierungsmaneuver mit hohem PEEP -- was die zuvor verbreitete Praxis grundlegend in Frage stellte und die ARDS-Beatmungsstrategien neu ausgerichtet hat."
    ),

    // Question 14 - Hochfrequenz-Oszillationsbeatmung
    Question(
        categoryId = 16,
        questionText = "Welches Prinzip nutzt die Hochfrequenz-Oszillations-Ventilation (HFOV), um den Gasaustausch trotz extrem kleiner Atemzugvolumina (< Totraumvolumen) aufrechtzuerhalten?",
        answerA = "Diffusion allein reicht bei sehr hohen Frequenzen (3-15 Hz) aus, da die O2-Loslichkeit bei hohem Frequenzdruck ansteigt",
        answerB = "Asymmetrisches Geschwindigkeitsprofil (Pendelluft), molekulare Diffusion, Taylor-Dispersion und kardiogene Mixis ermoelichen Gas-Transport auch mit VT < anatomischer Totraum",
        answerC = "HFOV nutzt negatives intrathorakales Druckgefaelle durch den Sog der oszillierenden Membran fuer Massenmixis",
        answerD = "Ultraschnelles Atemzugvolumen erzeugt Turbulenzen im Atemweg, die effektiveres Mixing erzeugen als laminarer Fluss bei normaler Beatmung",
        correctAnswer = 1,
        explanation = "HFOV nutzt mehrere Transportmechanismen gleichzeitig: asymmetrische Geschwindigkeitsprofile im Bronchialbaum (Inspirationsfluss axial-zentral, Exspirationfluss peripher), Taylor-Dispersion (Konvektions-Diffusions-Interaktion), Pendelluft zwischen benachbarten Alveolen mit unterschiedlichen Zeitkonstanten und direkte molekulare Diffusion in Alveolarnaeher.",
        difficulty = 5,
        funFact = "HFOV wurde ursprueglich fuer Fruehgeborene entwickelt, wo Lungenvolumen-Schutz entscheidend ist -- ein Neugeborenes kann mit Atemzugvolumina von 1-2 ml beatmet werden, was weniger als dem Totraumvolumen entspricht, was bei konventioneller Beatmung unmoeglich waere."
    ),

    // --- ZYSTISCHE FIBROSE ---

    // Question 15 - CFTR-Klassen-Mutationen
    Question(
        categoryId = 16,
        questionText = "Einem CFTR-Mutations-Patienten mit Klasse-II-Mutation (F508del homozygot) wird ein CFTR-Modulator verschrieben. Welches Medikamentenprinzip adressiert den spezifischen Defekt dieser Mutation?",
        answerA = "Ein Potentiator (z.B. Ivacaftor allein), der den Kanal offenhaelt -- F508del hat normales Gating, aber reduzierte Oeffnungsdauer",
        answerB = "Ein Korrektor (z.B. Lumacaftor, Tezacaftor), der das Protein-Faltungsdefizit von F508del korrigiert und den Transport zur Zelloberflaech ermoeoelicht; oft kombiniert mit Potentiator",
        answerC = "Ein Amplifier, der die CFTR-mRNA-Transkription hochreguliert und so mehr fehlgefaltetes Protein erzeugt",
        answerD = "Ein Read-Through-Agent (z.B. Ataluren) ueberbrueckt das vorzeitige Stoppcodon -- F508del ist eine Nonsense-Mutation die diesen Mechanismus erfordert",
        correctAnswer = 1,
        explanation = "F508del (Deletion des Phenylalanins an Position 508) ist eine Klasse-II-Mutation: Das Protein wird synthetisiert aber falsch gefaltet, sodass es im ER haengenbleibt und proteasomal abgebaut wird statt die Zelloberflaech zu erreichen. Korrektoren (Chaperona) stabilisieren das gefaltete Protein und ermoeglichen den Erreichen der Plasmamembran; Potentiatoren steigern dann die Kanaloffenwahrscheinlichkeit des oberflaechenstaendigen CFTR.",
        difficulty = 5,
        funFact = "Kaftrio (Elexacaftor/Tezacaftor/Ivacaftor, 2019) war ein Meilenstein: Diese Dreifachkombination verbesserte FEV1 durchschnittlich um 14% absolut und verminderte Exazerbationen um 63% -- erstmals sprach man von einer 'Funktionellen Heilung' fuer F508del-homozygote Patienten, auch wenn die Grunderkrankung bleibt."
    ),

    // Question 16 - CFTR als Ionenkanal
    Question(
        categoryId = 16,
        questionText = "CFTR ist strukturell ein ABC-Transporter, fungiert aber als Ionenkanal. Welches Ion transportiert er primaer, und welche zwei NBD-abhaengigen Schritte regulieren sein Oeffnen?",
        answerA = "Na+-Kanal; reguliert durch NBD1 (Oeffnen durch ATP-Bindung) und NBD2 (Schliessen durch Ca2+)",
        answerB = "Cl--Kanal; reguliert durch PKA-Phosphorylierung der R-Domaene (Aktivierung) und ATP-Bindung/Hydrolyse an NBD1/NBD2 (Gating)",
        answerC = "HCO3--Kanal ausschliesslich; reguliert durch pH-Gradient und NBD1-Dimerisation",
        answerD = "K+-Kanal; reguliert durch Membranpotential (spannungsabhaengig) und NBD-Autophosphorylierung",
        correctAnswer = 1,
        explanation = "CFTR ist ein Cl--Kanal (auch etwas HCO3--permeabel). Er wird durch PKA-Phosphorylierung der regulatorischen R-Domaene fuer ATP-Gating zugaenglich gemacht, dann oeffnet ATP-Bindung an NBD1/NBD2 den Kanal durch Dimerisation, und ATP-Hydrolyse an NBD2 schliesst ihn wieder. Ohne Phosphorylierung der R-Domaene kann ATP den Kanal nicht oeffnen.",
        difficulty = 5,
        funFact = "Ivacaftor (Kalydeco) war das erste krankheitsmodifizierende CF-Medikament (2012) und wirkt ausschliesslich bei Patienten mit Klasse-III-Mutationen (Gating-Defekte, z.B. G551D) -- es erhoehen die Kanaloffenwahrscheinlichkeit 10-fach. Nur 4-5% aller CF-Patienten tragen diese Mutation, weshalb das Medikament anfangs als 'zu teuer' diskutiert wurde."
    ),

    // Question 17 - CF-Komplikationen
    Question(
        categoryId = 16,
        questionText = "Welcher Mechanismus erklaert die erhoehte Infektanfaelligkeit der CF-Lunge mit Pseudomonas aeruginosa, insbesondere warum Pseudomonas so schwer eliminierbar ist?",
        answerA = "Reduzierte mukoziliare Clearance durch eingedieckten Mukus foerdert Besiedlung; Pseudomonas bildet Biofilme mit Alginathuelle, die Antibiotika, Immunzellen und Opsonine weitgehend ausschliesst",
        answerB = "CF-Neutrophile sind intrinsisch dysfunktional und koennen Pseudomonas nicht phagozytieren trotz normaler Mukus-Clearance",
        answerC = "CF-Patienten haben erhoehte Interferon-gamma-Spiegel, die Pseudomonas-Wachstum paradoxerweise foerdern",
        answerD = "Chloridmangel im ASL (Airway Surface Liquid) inhibiert antimikrobielle Peptide, was Pseudomonas-Besiedlung ermoeoelicht, aber Biofilmbildung spielt keine Rolle",
        correctAnswer = 0,
        explanation = "Fehlerhaftes CFTR reduziert die Chloridsekretion in die Atemwegsoberflaeche und trocknet den Mukus ein. Der zaehere Schleim behindert den Mukus-Zilientransport, sodass Bakterien nicht ausreichend eliminiert werden. Einmal etabliert, bildet Pseudomonas aeruginosa in CF-Lungen einen mukoiden Phaenotyp mit Alginat-Biofilm, der antibiotische Penetration und Phagozytose massiv hemmt.",
        difficulty = 5,
        funFact = "Pseudomonas aeruginosa kann in CF-Lungen jahrzehntelang persistieren -- genetische Analysen zeigen, dass ein einziger Klon beim ersten Kontakt in der Kindheit die Lunge besiedelt und sich dann ueber Jahrzehnte klonal weiterentwickelt, waehrend er aeussere Klone verdraengt."
    ),

    // --- INTERSTITIELLE LUNGENERKRANKUNGEN ---

    // Question 18 - UIP-Muster in der HRCT
    Question(
        categoryId = 16,
        questionText = "Welches radiologische Muster im HRCT ist fuer ein 'definites UIP pattern' (Usual Interstitial Pneumonia) bei IPF charakteristisch, und welche Befunde schliessen UIP aus?",
        answerA = "Milchglas-Trubung > Retikulaermuster; ausgeschlossen durch vorwiegend basale Verteilung",
        answerB = "Bibasal-periphere Retikulaermuster mit Honigwaben (oft mit Traktionsbronchiektasen); ausgeschlossen durch oberlappen-betonte oder peribronchovaskulaere Verteilung, Konsolidierungen, extensive Milchglas-Trubung (> Retikulaermuster) und Mikronoduli",
        answerC = "Bilateral symmetrische Konsolidierungen; ausgeschlossen durch Retikulaermuster",
        answerD = "Verdickung der interlobularen Septen zentral betont; ausgeschlossen durch periphere Verteilung",
        correctAnswer = 1,
        explanation = "Definites UIP im HRCT zeigt bibasal-periphere, subpleurale Retikulaermuster mit Honigwaben (Cystic airspace clustering) +/- Traktionsbronchiektasen. Atypische Merkmale die gegen UIP sprechen: oberlappendominant, peribronchovaskulaer, extensive Milchglas-Trubung, bilaterale Konsolidierungen, Zentrilobulaere Mikronoduli, oberlappen-Zysten.",
        difficulty = 5,
        funFact = "Das Honigwaben-Muster (Honeycombing) bei UIP repraesentiert vollstaendig umgebauetes Lungengewebe mit verlorenem Alveolaerarchitektur -- einmal vorhanden, ist es irreversibel, was erklaert, warum Antifibrotika wie Nintedanib und Pirfenidon nur die Progression verlangsamen, aber keine Heilung bewirken koennen."
    ),

    // Question 19 - Sarkoidose-Pathologie
    Question(
        categoryId = 16,
        questionText = "Was ist die pathologische Grundlaeison der Sarkoidose, und welche Zelltypen bilden das charakteristische Granulom?",
        answerA = "Lymphoplasmazytaere Infiltrate ohne granulomatoese Struktur; B-Zellen dominieren",
        answerB = "Nicht-verkaesendes Granulom aus epitheloiden Makrophagen, mehrkernigen Riesenzellen (Langhans-Typ), CD4+ T-Helfer-Zellen und peripherem Lymphozytenwall -- ohne zentrale Nekrose (im Gegensatz zu Tuberkulose)",
        answerC = "Verkaesendes Granulom identisch mit Tuberkulose; Unterscheidung nur durch Kultur moeglich",
        answerD = "Eosinophile Granulome aus Langerhans-Zellen; identisch mit pulmonaler Langerhans-Zell-Histiozytose",
        correctAnswer = 1,
        explanation = "Sarkoidose-Granulome sind epitheloid, nicht-verkaesend (kein zentrales Nekrose), bestehen aus aktivierten Makrophagen (Epitheloidzellen), mehrkernigen Riesenzellen und sind von CD4+ T-Zellen umgeben. ACE (Angiotensin-Converting-Enzyme) wird von Epitheloidzellen produziert und dient als Serummarker.",
        difficulty = 5,
        funFact = "Sarkoidose ist eine der grossen Imitatoren in der Medizin -- sie kann fast jedes Organ befallen und prasentiert sich als Uveitis, Fazialisparese, Hyperkalzaemie, Herzrhythmusstoeungen oder Hautknoten. Die Loefgren-Trias (Erythema nodosum, bilaterale Hiladenopathie, Arthritis) hat eine exzellente Prognose mit Spontanremission in 90% der Faelle."
    ),

    // Question 20 - IPF-Therapie
    Question(
        categoryId = 16,
        questionText = "Nintedanib hemmt bei IPF spezifisch welche Rezeptortyrosinkinasen, und ueber welchen Mechanismus foerdert es Fibrose-Progression normalerweise?",
        answerA = "Nintedanib hemmt EGF-R, HER2 und HER3; diese Rezeptoren foerdern Fibrose durch Myofibroblasten-Differenzierung via TGF-beta",
        answerB = "Nintedanib hemmt PDGF-R, VEGF-R und FGF-R; diese Wachstumsfaktorrezeptoren stimulieren Fibroblasten-Proliferation, Migration und Myofibroblasten-Differenzierung",
        answerC = "Nintedanib hemmt ausschliesslich TGF-beta-Rezeptoren (ALK5) und blockiert so die profibrotikasche Smad2/3-Signalkaskade",
        answerD = "Nintedanib hemmt Kollagen-Cross-Linking-Enzyme (LOX) und verhindert so die Versteifung der extrazellularen Matrix",
        correctAnswer = 1,
        explanation = "Nintedanib ist ein intrazellulaerer Kinase-Inhibitor, der PDGF-Ra/b, VEGF-R1/2/3 und FGF-R1/2/3 blockiert. Diese Wachstumsfaktoren foerdern Fibroblasten-Proliferation, Einwanderung in das Gewebe und Myofibroblasten-Differenzierung (die Fibrose-treibenden Zellen). Die Hemmung dieser Pfade verlangsamt den FVC-Abfall bei IPF um ca. 50%.",
        difficulty = 5,
        funFact = "Nintedanib (Ofev) wurde urspruenglich als Krebsmedikament entwickelt -- es ist auch fuer nichtkleinzelliges Lungenkarzinom zugelassen (als Vargatef). Die Erkenntnis, dass dieselben Wachstumsfaktor-Signalwege, die Tumorwachstum treiben, auch Fibrose foerdern, war ein konzeptueller Durchbruch."
    ),

    // --- FORTGESCHRITTENE NEPHROLOGIE ---

    // Question 21 - IgA-Nephropathie Pathomechanismus
    Question(
        categoryId = 16,
        questionText = "Was ist der aktuell akzeptierte 'Multi-Hit'-Mechanismus der IgA-Nephropathie (Berger-Erkrankung)?",
        answerA = "Direktes Einlagern von IgA1 in das Mesangium durch erhoehten IgA-Serumspiegl; keine Immunreaktion noetig",
        answerB = "Hit 1: galaktorsedefizientes IgA1 (Gd-IgA1) wird produziert; Hit 2: Autoantikoper gegen Gd-IgA1 entstehen; Hit 3: IgA-IgG-Immunkomplexe; Hit 4: mesangiale Ablagerung mit Komplement-Aktivierung und Entzuendung",
        answerC = "Streptokokken-Antigene kreuzreagieren mit mesangialem Antigen; identisch mit Post-Strep-GN",
        answerD = "Mukosal-IgA wird durch Transepitheltransport direkt aus dem Darm in die Niere transportiert",
        correctAnswer = 1,
        explanation = "Der 4-Hit-Mechanismus: (1) Abnormal galakorsedefizientes IgA1 (Gd-IgA1) wird produziert (genetisch/epigenetisch reguliert), (2) Autoantikoper der IgG/IgA-Klasse gegen den Gd-IgA1-Hinge-Bereich entstehen, (3) Immunkomplexe aus Gd-IgA1 und diesen Autoantikopern bilden sich, (4) Komplexe lagern sich im Mesangium ab, aktivieren Komplement und foerdern Proliferation und Entzuendung.",
        difficulty = 5,
        funFact = "IgA-Nephropathie ist die weltweit haeufigste primaere Glomerulonephritis, aber ihre Entdeckung ist einem Zufallsbefund zu verdanken -- Jean Berger und Nicole Hinglais beschrieben 1968 zunaechst lediglich 'Ablagerungen von IgA im Mesangium' ohne die klinische Bedeutung zu ahnen, die sich erst spaeter enthullte."
    ),

    // Question 22 - Minimal-Change-Erkrankung
    Question(
        categoryId = 16,
        questionText = "Welche Zellpopulation gilt als Haupttreiberder Minimal-Change-Erkrankung (MCD), und wie erklaert dies das gute Ansprechen auf Corticosteroide?",
        answerA = "B-Zellen produzieren direkt nephrotoxische Antikoerper; Corticosteroide hemmen B-Zell-Proliferation",
        answerB = "CD4+ T-Zellen oder T-regulatorische Zellstoerung produzieren einen unbekannten 'Permeabilitaetsfaktor', der Podozyten-Schlitzmembranen schaedigt; Corticosteroide supprimieren diese T-Zell-Dysregulation",
        answerC = "Mastzellen degranulieren in der Niere und setzen Heparin frei, das die GBM destabilisiert; Corticosteroide hemmen Mastzellaktivierung",
        answerD = "Mesangialzellen proliferieren und produzieren TGF-beta, das Podozyten direkt schaedigt; Steroide hemmen Mesangialzellwachstum",
        correctAnswer = 1,
        explanation = "MCD wird klassischerweise als T-Zell-Erkrankung betrachtet -- ein hypothetischer zirkulierender Faktor (moeglicherweise IL-13, IL-4 oder ein noch unbekanntes Lymphokin) von dysregulierten T-Zellen schaedigt die Podozyten und verursacht Schlitzmembran-Dysfunktion mit Fusionierung der Fussfortsaetze (nur im EM sichtbar). Corticosteroide normalisieren diese T-Zell-Funktion, was die exzellente Remissionsrate erklaert.",
        difficulty = 5,
        funFact = "MCD heisst nicht, dass keine Aenderungen vorhanden sind -- der Name bezieht sich auf die Lichtmikroskopie, die normal erscheint. Erst im Elektronenmikroskop sieht man die charakteristische diffuse Podozyten-Fussfortsatz-Fusion (effacement), die bei gesunden Nieren nie vorkommt."
    ),

    // Question 23 - FSGS Pathogenese
    Question(
        categoryId = 16,
        questionText = "Ein Patient entwickelt nach Nierentransplantation innerhalb von Stunden ein nephrotisches Syndrom mit Nachweis von FSGS im Transplantat. Was ist die wahrscheinlichste Ursache, und welche Therapie ist indiziert?",
        answerA = "De-novo-FSGS durch Calcineurin-Inhibitor-Toxizitaet; Tacrolimus-Dosisreduktion",
        answerB = "Rezidiv-FSGS durch rekurrierenden zirkulierenden Permeabilitaetsfaktor (moeglicherweise suPAR oder CLCF1); Plasmaaustausch zur Faktorelimination",
        answerC = "Hyperfiltrationsschaden im Einzelnierensystem; konservative Therapie mit ACE-Hemmern",
        answerD = "Transplantatabstossungsreaktion Typ IIb mit FSGS-aehnlichem Muster; Stosspulstherapie mit Methylprednisolon",
        correctAnswer = 1,
        explanation = "Rekurrenz-FSGS tritt bei 20-40% der primaeren FSGS-Patienten nach Tx auf, oft innerhalb von Stunden bis Tagen -- das dramatische Timing impliziert einen praeformierten zirkulierenden Faktor (suPAR -- soluble urokinase-type plasminogen activator receptor, CLCF1 oder andere). Plasmaaustausch ist die etablierteste Ersttherapie, um diesen Faktor zu entfernen.",
        difficulty = 5,
        funFact = "Beim primaeren FSGS wurden multiple zirkulierende Faktoren vorgeschlagen, aber keiner wurde eindeutig bewiesen -- suPAR war vielversprechend, aber spaetere Studien zeigten erhoehte Spiegel auch bei anderen Nierenerkrankungen. Die Suche nach dem 'echten' FSGS-Faktor ist eine der ungeoloesten Fragen der Nephrologie."
    ),

    // --- DIALYSE ---

    // Question 24 - Peritonealdialyse-Membran
    Question(
        categoryId = 16,
        questionText = "Welches mathematische Modell beschreibt den peritonealen Transport am besten, und was sind die drei Membrantypen (Aquaporine ausgenommen) nach dem Dreiporen-Modell?",
        answerA = "Hagen-Poiseuille-Modell; Poren sind: Kapillar-Endothelzellporen, Mesothelzellporen, Submesothelialporen",
        answerB = "Dreiporen-Modell (Three-Pore Model); Poren sind: kleine Poren (NaCl, Harnstoff, Kreatinin), grosse Poren (Albumin, grosse Molekule), ultrakleine Poren/Aquaporin-1 (freies Wasser)",
        answerC = "Zwei-Komparten-Modell mit linearem Transportkoeffizient; Poren sind: hydrophile und hydrophobe Kanaele",
        answerD = "Diffusions-Konvektions-Modell ohne Poreneinteilung; Transport haengt ausschliesslich vom osmotischen Gradienten ab",
        correctAnswer = 1,
        explanation = "Das Dreiporen-Modell (Rippe & Krediet) postuliert: kleine Poren (radius ~4.3 nm, 95% Poren -- transportieren kleine Soluets diffusiv), grosse Poren (radius ~25 nm, 5% Poren -- Albumin und makromolekularer Transport) und Aquaporin-1-Kanaele (ultraklein -- selektiv freier Wassertransport bei hyperosmolaren Austauschloesungen, verantwortlich fuer Aquaporin-vermittelten Wasserfluss).",
        difficulty = 5,
        funFact = "Icodextrin als kolloidales Osmotikum (Glukosepolymer) nutzt ausschliesslich die grossen Poren und erzeugt so langanhaltenden Ultrafiltrations-Effekt ueber 8-12 Stunden -- ideal fuer den langen Nachtwechsel bei CAPD, da Glukose-basierte Loesungen nach 4-6 Stunden absorbiert werden und die Ultrafiltration sistiert."
    ),

    // Question 25 - CRRT-Prinzip
    Question(
        categoryId = 16,
        questionText = "Was unterscheidet haemofiltrationbasierte CRRT (CVVH) von hamodiafiltrationbasierter CRRT (CVVHDF) hinsichtlich Soluttransportmechanismus und Indikation?",
        answerA = "CVVH nutzt ausschliesslich Diffusion ueber Konzentrationsgradient; CVVHDF nutzt ausschliesslich Konvektion; indiziert bei identischen Krankheitsbildern",
        answerB = "CVVH nutzt ausschliesslich Konvektion (Solute werden mit dem Ultrafiltrat mitgezogen -- Sieving); CVVHDF kombiniert Konvektion und Diffusion (Dialysat gegenlaeufig); letztere ist effizienter fuer kleine Molekule wie Harnstoff",
        answerC = "CVVH hat hoeheren Blutfluss und ist fuer Hypervolumie indiziert; CVVHDF hat niedrigeren Blutfluss fuer Elektrolytstoeungen",
        answerD = "CVVH erfordert Antikoagulation; CVVHDF ist antikoagulationsfrei moeglich da Dialysat Membranbefeuchtung verhindert",
        correctAnswer = 1,
        explanation = "CVVH (Haemofiltration) arbeitet rein konvektiv -- hohes Ultrafiltratvolumen (20-35 ml/kg/h) wird erzeugt und mit Substitutionslosung ersetzt; mittelgrosse Molekule werden effizient eliminiert. CVVHDF kombiniert Konvektion mit Dialysat-Gegenstrom (Diffusion), was besonders kleine Molekule wie Harnstoff effizienter entfernt, da Diffusion fuer kleine hydrophile Soluets am guenstigsten ist.",
        difficulty = 5,
        funFact = "Bei Sepsis-assoziiertem Multiorganversagen wurde hohe CRRT-Dosis (> 35 ml/kg/h) mit verbessertem Outcome assoziiert -- spaetere randomisierte Studien (ATN, RENAL) konnten diesen Vorteil nicht bestaetigen, was zeigt, wie trickreich es ist, fuer Intensivpatienten adaequate Studienergebnisse zu generieren."
    ),

    // Question 26 - Haemodiafiltration Online
    Question(
        categoryId = 16,
        questionText = "Was bedeutet 'Online-Haemodiafiltration' (OL-HDF), und welchen klinischen Vorteil hat sie gegenueber der Standard-Hamodialyse bei Langzeit-Dialysepatienten?",
        answerA = "Online-HDF bedeutet, dass die Dialyse durch eine Telemedizin-Verbindung ueberwacht wird; kein klinischer Vorteil ausser Sicherheit",
        answerB = "Online-HDF bereitet hochreines Substitutionsvolumen direkt aus dem Dialysewasser auf (> 20-30 L/Sitzung); klinische Vorteile: bessere Elimination mittelgroser Molekuele (beta2-Mikroglobulin, FGF-23), haemodynamische Stabilitaet und moegliche Mortalitaetsreduktion",
        answerC = "Online-HDF nutzt online-berechnete individuelle Dialyseparameter; Vorteil ist praezisere Dosissteuerung",
        answerD = "Online-HDF ersetzt den Dialysator waehrend der Sitzung automatisch; Vorteil ist verlaengerte Sitzungszeit ohne Koagulation",
        correctAnswer = 1,
        explanation = "Bei OL-HDF wird ultrareines Wasser direkt am Geraet durch Umkehrosmose und Ultrafiltration zu Substitutionslosung aufbereitet, was Infusionsvolumina von 20-30 L pro Sitzung erlaubt. Der konvektive Transport eliminiert mittelgrosse Molekule (500-60.000 Dalton) wie beta2-Mikroglobulin, FGF-23 und inflammatorische Mediatoren besser als Haemoidialyse, was mit reduzierter Mortalitaet in mehreren Studien assoziiert war.",
        difficulty = 5,
        funFact = "FGF-23 (Fibroblast Growth Factor 23) ist ein bei Dialysepatienten massiv erhoehtes Phosphatonin, das mit kardialer Hypertrophie und Mortalitaet korreliert und durch OL-HDF besser als durch Standard-HD eliminiert wird -- was moeglicherweise den Ueberlebensvorteil teilerklaert."
    ),

    // --- NIERENTRANSPLANTATION ---

    // Question 27 - Desensibilisierungsprotokoll
    Question(
        categoryId = 16,
        questionText = "Ein Patient hat hohe praeformierte donorspezifische Antikoerper (DSA). Welche Massnahmen umfasst ein typisches Desensibilisierungsprotokoll vor ABO-kompatibler Nierentransplantation?",
        answerA = "Nur orale Corticosteroide fuer 4 Wochen vor Transplantation",
        answerB = "Plasmaaustausch oder Immunadsorption zur DSA-Reduktion, kombiniert mit hochdosiertem IVIG (intravenoes Immunglobulin), oft plus Rituximab (Anti-CD20) -- Ziel ist DSA-Titre unter einem definierten Grenzwert vor Transplantation",
        answerC = "Bortezomib (Proteasominhibitor) allein -- eliminiert plasmazellen die DSA produzieren",
        answerD = "Eculizumab (Komplementinhibitor) praeoperativ -- verhindert komplementvermittelte Abstosung trotz persistierender DSA",
        correctAnswer = 1,
        explanation = "Desensibilisierungsprotokoll bei hohem DSA: (1) Plasmaaustausch (PE) oder Immunadsorption (IA) entfernen akut DSA aus dem Plasma, (2) IVIG supprimiert B-Zell-Reaktivierung und hat immunmodulatorische Effekte, (3) Rituximab depletiert CD20+ B-Zellen als Vorlaeuferzellen der DSA-produzierenden Plasmazellen. Ziel: ausreichende DSA-Reduktion fuer ein akzeptables Transplantationsrisiko.",
        difficulty = 5,
        funFact = "Das Johns Hopkins Programm pionierte ABO-inkompatible und stark sensibilisierte Transplantationen (Donor-spezifische Antikoerper) -- die damals revolutionaere Erkenntnis, dass Hochrisikopatienten durch aggressive Desensibilisierung erfolgreich transplantiert werden koennen, hat weltweit Transplantationsmoeglichkeiten fuer zuvor 'untransplantierbare' Patienten eroeffnet."
    ),

    // Question 28 - ABO-inkompatible Transplantation
    Question(
        categoryId = 16,
        questionText = "Welche spezifische Herausforderung bei der ABO-inkompatiblen Nierentransplantation (ABOi-NTx) wurde mit der Einfuehrung von Anti-CD20 (Rituximab) teilweise geloest?",
        answerA = "ABO-Isohaemagglutinine konnten nicht durch Plasmaaustausch entfernt werden; Rituximab eliminiert direkt Isohaemagglutinine",
        answerB = "Praeformierte ABO-Antikoerper wurden nach Transplantation durch residuale Memory-B-Zellen schnell resynthetisiert; Rituximab depletiert CD20+ B-Zellen und reduziert die Rebound-Antikoerperproduktion nachhaltig",
        answerC = "Das Komplement-Kaskade konnte bei ABOi nicht gehemmt werden; Rituximab blockt Komplement C3 direkt",
        answerD = "Splenoektomie war zuvor obligat; Rituximab ersetzt die Milzentfernung vollstaendig ohne jedes Risiko",
        correctAnswer = 1,
        explanation = "Das historische Problem bei ABOi-NTx: Auch nach ausreichendem Plasmaaustausch zur Antikoerperentfernung produzierten persistierende Gedaechtnis-B-Zellen rasch neue Isohaemaglutinine. Rituximab (Anti-CD20) depletiert naive und Memory-B-Zellen effektiv, was die Rebound-Antikoerper-Produktion nach Transplantation stark reduziert und langfristige Antikoerper-Akkommodation ermoeoelicht.",
        difficulty = 5,
        funFact = "Japan hat die groesste Erfahrung mit ABOi-Nierentransplantation -- da lebend-verwandte Transplantationen in Japan bevorzugt werden und kompatible Spender selten sind, wurde ABOi-NTx dort fruehzeitig perfektioniert, mit Langzeitergebnissen die ABO-kompatiblen Transplantationen inzwischen entsprechen."
    ),

    // Question 29 - Calcineurin-Inhibitor-Toxizitaet
    Question(
        categoryId = 16,
        questionText = "Welcher strukturelle Nierenveraenderung erklaert die chronische Calcineurin-Inhibitor-Nephrotoxizitaet (Tacrolimus/Ciclosporin), und welche spezifische vaskulaere Laesion ist pathognomonisch?",
        answerA = "Diffuse mesangiale Expansion identisch mit diabetischer Nephropathie; keine spezifische vaskulaere Laesion",
        answerB = "Interstitielle Fibrose und tubulaere Atrophie (IFTA) als Endstrecke; pathognomone Laesion ist die nodulaer-hyaline Arteriolopathie (Afferentarterioloskleurose mit PAS-positivem Material in der Gefaesswand)",
        answerC = "Membranoproliferative Glomerulonephritis durch Immunablagerungen; CNI aktivieren Komplementsystem",
        answerD = "Fokale Podozytendefekte durch direkte CNI-Toxizitaet auf TRPC-Kanaele; fuhrt zu FSGS-aehnlichem Bild",
        correctAnswer = 1,
        explanation = "Chronische CNI-Nephrotoxizitaet zeigt im Biopsat: IFTA als unspezifische Endstrecke, aber pathognomonisch ist die 'striped fibrosis' (streifenfoermige Fibrose parallel zu den Arteriolen) und die nodulaere Hyalinose der Afferentarteriolenwand mit PAS-positivem eosinophilem Material (hyaline Einlagerungen) -- ein Muster, das nur durch CNI und Diabetes vorkommt.",
        difficulty = 5,
        funFact = "Tacrolimus und Ciclosporin haben paradoxerweise unterschiedliche vaskulaere Toxizitaetsmuster trotz identischem Wirkungsmechanismus (Calcineurin-Hemmung) -- Ciclosporin verursacht ausgepraegtenere Arteriolopathie, Tacrolimus mehr nodulaere Hyalinose, was auf zusaetzliche, calcineurinunabhaengige Toxizitaetsmechanismen hindeutet."
    ),

    // --- SELTENE NIERENERKRANKUNGEN ---

    // Question 30 - Fabry-Erkrankung Nierenmanifestationen
    Question(
        categoryId = 16,
        questionText = "Welcher enzymatische Defekt liegt der Fabry-Erkrankung (Anderson-Fabry) zugrunde, und wie erklaert er die renale Pathologie?",
        answerA = "Mangelhafte Hexosaminidase A; Ablagerung von GM2-Gangliosiden in Podozyten fuehrt zu FSGS",
        answerB = "Mangelhafte alpha-Galaktosidase A (GLA-Gen, X-chromosomal); Akkumulation von Globotriaosylceramid (Gb3) in Endothelzellen, Podozyten und Mesangialzellen fuehrt zu Proteinurie, Haematurie und progressivem Nierenversagen",
        answerC = "Mangelhafte Galactosylcerebrosidase; Ceramidablagerungen in proximalen Tubulusepithelzellen verursachen Fanconi-Syndrom",
        answerD = "Mangelhafte Sphingomyelinase; Sphingomyelin-Akkumulation in Mesangium verursacht mesangiale Proliferation und IgA-aehnliche Nephropathie",
        correctAnswer = 1,
        explanation = "Fabry-Erkrankung (GLA-Mutations) fuehrt zu Mangel an alpha-Galaktosidase A, die Gb3 (Globotriaosylceramid) abbaut. Gb3 akkumuliert in allen Koerperzellen, besonders in Endothelzellen, Podozyten, Glattmuskelzellen und Tubuluszellen der Niere. Bioptisch typisch: Schaumzellen in Podozyten und Endothelien, PAS-positive granulare Einschluesse, EM: 'zebra bodies' (konzentrische Myelinfiguren).",
        difficulty = 5,
        funFact = "Fabry-Erkrankung ist die zweithaeufigste lysosomale Speicherkrankheit und wird oft jahrelang verkannt -- Maenner mit Angiokeratomen, neuropathischen Schmerzen, Kornea-Wirbel (Cornea verticillata) und Nierenproblemen werden haeuflg zunachst als Rheumapatienten oder mit multipler Sklerose behandelt, bevor der enzymatische Defekt gemessen wird."
    ),

    // Question 31 - Alport-Syndrom
    Question(
        categoryId = 16,
        questionText = "Welche Kollagenkomponente der glomerulaeren Basalmembran (GBM) ist bei Alport-Syndrom defekt, und welches EM-Muster ist charakteristisch?",
        answerA = "Kollagen Typ IV alpha3/alpha4/alpha5-Ketten (COL4A3-A5); EM zeigt GBM-Verduennung und -Verdickung im Wechsel mit Aufsplitterung der Lamina densa (Korbgeflecht-Muster)",
        answerB = "Laminin beta2 (LAMB2); EM zeigt diffuse GBM-Verduennung ohne Aufsplitterung",
        answerC = "Kollagen Typ I und III; EM zeigt mesangiale Einlagerungen von striated fibrils",
        answerD = "Nephrin und Podocin der Schlitzmembran; EM zeigt Podozyten-Fussfortsatz-Fusion ohne GBM-Aenderungen",
        correctAnswer = 0,
        explanation = "Alport-Syndrom entsteht durch Mutationen in COL4A3, COL4A4 (autosomal-rezessiv) oder COL4A5 (X-chromosomal-dominant bei Maennern). Die normalen Col4a3/4/5-Ketten der adulten GBM fehlen oder sind dysfunktional. EM zeigt das pathognomonische 'basketweave'-Muster: Verdickung und Verduennung der GBM im Wechsel mit lamellaerer Aufsplitterung der Lamina densa in feine Fibrillen.",
        difficulty = 5,
        funFact = "Bei X-chromosomalem Alport entwickeln heterozygote Frauen (Traegerinnen) in 15-30% der Faelle ebenfalls Nierenversagen -- fruehr galt es als 'nur Maennererkrankung', aber langfristige Verlaufsstudien zeigten, dass Traegerinnen eine signifikante Morbidstaet haben und ebenfalls ACE-Hemmer-Therapie benoetigen."
    ),

    // Question 32 - Autosomal-dominante polyzystische Nierenerkrankung
    Question(
        categoryId = 16,
        questionText = "Welche Signalwege sind bei ADPKD durch defektes Polycystin-1 (PKD1) und Polycystin-2 (PKD2) fehlreguliert, und welche Therapie zielt auf den effizientesten davon?",
        answerA = "VEGF-Signalweg (Angiogenese) ist der Hauptmechanismus; Bevacizumab ist die Zieltherapie",
        answerB = "mTOR-Signalweg (Zellproliferation/Wachstum) und cAMP-Signalweg (Chloridsekretion/Zystenfluid); Tolvaptan (V2-Rezeptor-Antagonist) senkt cAMP in Sammelrohr-Epithelzellen und verlangsamt Zystenwachstum",
        answerC = "NFkappaB-Signalweg (Entzuendung) ist primaer; Colchicin verlangsamt ADPKD-Progression durch NFkappaB-Hemmung",
        answerD = "Wnt-Signalweg (Tubulogenese) ist defekt; Beta-Catenin-Inhibitoren sind in Phase-III-Studien",
        correctAnswer = 1,
        explanation = "Defektes Polycystin-1/2 im primaeren Zilium stoert intrazellulaere Ca2+-Signale, was zu erhoehtem cAMP in Sammelrohr-Epithelzellen fuehrt (via ADH/cAMP). Erhoehtes cAMP aktiviert Cl--Sekretion (Zystenfluid-Produktion) und MAPK/B-Raf-Proliferationssignale. Tolvaptan, ein V2-Vasopressin-Rezeptor-Antagonist, blokkiert das cAMP-treibende ADH-Signal und verlangsamt messbar das totale Nierenvolumenwachstum.",
        difficulty = 5,
        funFact = "ADPKD ist die haeufigste monogene Nierenerkrankung weltweit (1:400-1:1000) und die vierhaefigste Ursache fuer terminale Niereninsuffizienz -- das Tolvaptan-Ergebnis aus der TEMPO-Studie (2012) war historisch, da es zum ersten Mal eine pharmakologische Verlangsamung dieser langjahrigen Erkrankung zeigte, auch wenn das Mittel aufgrund von Leberenzym-Erhoehungen nicht fuer alle Patienten geeignet ist."
    ),

    // --- KARDIALE ELEKTROPHYSIOLOGIE VERTIEFT ---

    // Question 33 - His-Purkinje-System
    Question(
        categoryId = 16,
        questionText = "Was erklaert den paradoxen Befund, dass bei einem linksschenkelblock (LBBB) der QRS-Komplex trotz verlangsamter Erregungsausbreitung normal beginnt?",
        answerA = "Linksschenkelblock betrifft nur die Tertiarzweige; der Anfangsvektor bleibt durch den intakten Fasciculus anterior normaler",
        answerB = "Der erste Erregunsvektor (septale Aktivierung 0-20 ms) wird vom rechten Buendel durch das intakte rechte His-Purkinje-System von rechts nach links aktiviert -- das LBBB verzoegert erst die freie Linkswand, nicht den Beginn des QRS",
        answerC = "LBBB betrifft nur Myokardzellen des LV, nicht das Reizleitungssystem; daher beginnt das QRS normal",
        answerD = "Das AV-Knoten-His-Buendel-System kompensiert den Linksschenkelblock durch schnellere Leitung im linken Faszkel",
        correctAnswer = 1,
        explanation = "Bei LBBB wird das interventrikulaere Septum initial durch den rechten Purkinje-Schenkel von rechts nach links aktiviert (0-20 ms) -- dieser erste Vektor beginnt normal. Die nachfolgende freie Linkswand wird dann langsam durch myokardiale Erregungsausbreitung (statt Purkinje) aktiviert, was zur QRS-Verbreiterung und charakteristischen LBBB-Morphologie fuehrt.",
        difficulty = 5,
        funFact = "His-Bundle-Pacing (HBP) ist eine neue Technik, bei der der Stimulationselektrod direkt im His-Buendel platziert wird -- bei LBBB kann HBP die normale koordinierte LV-Aktivierung wiederherstellen und hat bei CRT-Non-Respondern vergleichbare Ergebnisse wie biventrikulaeres Pacing, bei geringerem Implantationsaufwand."
    ),

    // Question 34 - Brugada-Syndrom Mechanismus
    Question(
        categoryId = 16,
        questionText = "Welche molekulare Anomalie erklaert im Repolarisationsmodell des Brugada-Syndroms die charakteristische RBBB-aehnliche ST-Elevation in V1-V3 und das Risiko fuer Kammerflimmern?",
        answerA = "Ueberschuss an Ito (transienter Auswaertsstrom) in der epikardialen RVOT-Region, der bei reduziertem INa (Nav1.5-Verlustmutation) die Plateau-Phase vorzeitig abkuerzt -- ein verstaerkter Ito-INa-Ungleichgewicht erzeugt Phase-2-Reentry",
        answerB = "Ueberschuss an IKr in der subepikardialen Schicht verursacht frihe Repolarisation und heterogene ERP",
        answerC = "Erhoeahter ICaL in der epikardialen Schicht verlaengert die Plateau-Phase und erzeugt Early Afterdepolarizations",
        answerD = "Struktureller Umbau mit fibroeser Auflockerung des RVOT verursacht Leitungsverzoegarungen als Substrate fuer Reentry",
        correctAnswer = 0,
        explanation = "Im Repolarisationsmodell: Die epikardiale RVOT-Region hat physiologisch mehr Ito als das Endokard. Bei Nav1.5-Verlustmutationen ist der Plateau-stabilisierende INa reduziert. Ito ueberwiegt und kuerzt die epikardiale Plateau-Phase vorzeitig ab, waehrend das Endokard normalen Plateau hat. Der epi-endo-Gradient erzeugt das Brugada-EKG-Muster. Wenn der epikardiale Plateau-Verlust nicht uniform ist, entsteht Phase-2-Reentry und Kammerflimmern.",
        difficulty = 5,
        funFact = "Fieber ist einer der staerksten Brugada-Trigger -- Natriumkanaele sind temperaturempfindlich und funktionieren bei 39-40 Grad deutlich schlechter, was den INa weiter reduziert und das Brugada-Muster 'enthullt'. Eltern mit unbekanntem Brugada-Syndrom werden deshalb auf der Intensivstation risikiert, wenn ihr Fieber nicht aggressiv behandelt wird."
    ),

    // Question 35 - Torsades de Pointes
    Question(
        categoryId = 16,
        questionText = "Was ist der elektrophysiologische Mechanismus, der Torsades-de-Pointes-Tachykardie initiiert, und welches Substrat muss dafuer im Myokard vorhanden sein?",
        answerA = "Spatnachdepolarisationen (DADs) durch Ca2+-Ueberlastung; Substrat ist erhoehter intrazellulaerer Kalziumgehalt",
        answerB = "Fruehe Nachpotentiale (EADs) entstehen durch verlaengerte Aktionspotentialdauer (QT-Verlaengerung) in der Plateau-Phase (Phase 2-3); Substrat ist hetrogenice Repolarisation (M-Zell-dominant) die das 'vulnerable window' fuer Reentry schafft",
        answerC = "Automatische Aktivitaet aus hyperaktiven Purkinje-Fasern ohne QT-Verlaengerung; Substrat ist myo-Purkinje-Diskonnexion",
        answerD = "Schenkelblock-bedingter Reentry im His-Purkinje-System; Substrat ist alternating LBBB/RBBB",
        correctAnswer = 1,
        explanation = "QT-Verlaengerung verlaengert das Aktionspotential, besonders in M-Zellen (mittventrikulaer). In der verlaengerten Plateau/Repolarisationsphase koennen reaktivierte L-Typ-Calciumkanaele (ICaL) oder Na+-Fenster-Strome EADs generieren. Der EAD trifft auf heterogen repolarisiertes Myokard und loest einen Reentry mit typischer Sinusoidal-Morphologie (Torsade) aus.",
        difficulty = 5,
        funFact = "Der Name 'Torsades de Pointes' (frz. 'Drehung der Spitzen') wurde von Francois Dessertenne 1966 gepraegt, der die charakteristische EKG-Morphologie mit um die isoelektrische Linie rotierenden QRS-Spitzen treffend beschrieb -- obwohl die Erscheinung damals ein Roentgenbild war, das er zunaechst fuer ein Artefakt hielt."
    ),

    // --- PULMONALE HYPERTONIE VERTIEFT ---

    // Question 36 - PDE5-Inhibitoren bei PAH
    Question(
        categoryId = 16,
        questionText = "Ueber welchen Mechanismus wirken Sildenafil und Tadalafil bei pulmonalarterieller Hypertonie, und welcher endogene Stimulus wird dadurch potenziert?",
        answerA = "PDE5-Inhibitoren hemmen Phospholipase C und reduzieren IP3-vermittelte Vasokonstriktion; potenzieren Endothelin-abhangige Vasodilatation",
        answerB = "PDE5-Inhibitoren hemmen die Phosphodiesterase-5 (die cGMP abbaut) und verlaengern so die Wirkung von NO-stimuliertem cGMP; potenzieren den endogenen NO-sGC-cGMP-Signalweg in der pulmonalen Gefaessmuskulatur",
        answerC = "PDE5-Inhibitoren hemmen cAMP-Abbau und potenzieren Prostacyclin-vermittelte Vasodilatation",
        answerD = "PDE5-Inhibitoren aktivieren Guanylatzyklase direkt ohne NO als Kofaktor; der endogene Stimulus ist ANP (atriales natriuretisches Peptid)",
        correctAnswer = 1,
        explanation = "Endotheliales NO aktiviert sGC (soluble Guanylatzyclase) in glatten Muskelzellen, die cGMP aus GTP bildet. cGMP aktiviert PKG, die Myosin-Leichtketten-Kinase hemmt und so Vasodilatation bewirkt. PDE5 baut cGMP ab -- PDE5-Inhibitoren verlaengern die cGMP-Halbwertszeit und potenzieren so den NO-Effekt besonders in der pulmonalen Strombahn, wo PDE5 hochexprimiert ist.",
        difficulty = 5,
        funFact = "Sildenafil wurde urspruenglich als koronarer Vasodilatator entwickelt -- in Phase-I-Studien fiel auf, dass es Peniserektionen verursachte. Das Entwicklerteam wechselte die Indikation zu erektiler Dysfunktion, und 1998 wurde Viagra zugelassen. Die PAH-Zulassung (als Revatio) folgte erst 2005, nachdem die pulmonale Gefaessdilatation systematisch untersucht worden war."
    ),

    // Question 37 - Riociguat: sGC-Stimulator
    Question(
        categoryId = 16,
        questionText = "Wie unterscheidet sich Riociguat (sGC-Stimulator) mechanistisch von PDE5-Inhibitoren, und fuer welche besondere PAH-Untergruppe ist es zugelassen?",
        answerA = "Riociguat hemmt ebenfalls PDE5 aber mit hoeaherer Spezifitaet; zugelassen ausschliesslich fuer WHO-Gruppe 1 PAH",
        answerB = "Riociguat stimuliert die sGC direkt und NO-unabhaengig (auch wenn NO fehlt) sowie synergistisch mit vorhandenem NO; zugelassen fuer PAH (WHO-Gruppe 1) UND CTEPH (WHO-Gruppe 4)",
        answerC = "Riociguat aktiviert Adenylcyclase statt Guanylcyclase; zugelassen ausschliesslich fuer inoperables CTEPH",
        answerD = "Riociguat ist ein Dual-Hemmer (PDE5 + PDE3) mit additiver Wirkung; zugelassen fuer alle WHO-PH-Gruppen",
        correctAnswer = 1,
        explanation = "Riociguat stimuliert sGC direkt durch Bindung an eine allosterische Stelle (NO-sensitisierend) und kann sGC auch aktivieren, wenn NO gering oder fehlt -- dadurch wirkt es in NO-defizienten Zustaenden effektiver als PDE5-Inhibitoren. Es ist als einziges PAH-Medikament auch fuer inoperables oder residuales CTEPH nach Pulmonalarterien-Endarteriektomie zugelassen.",
        difficulty = 5,
        funFact = "Die gleichzeitige Einnahme von Riociguat und PDE5-Inhibitoren ist absolut kontraindiziert -- beide potenzieren cGMP und koennen zusammen schwere, therapierefraktaere Hypotension verursachen. Diese Kombination war in fruehen Studien toedlich und wird in der Fachinformation explizit als schwarze Warnung aufgefuehrt."
    ),

    // --- DIALYSE VERTIEFT ---

    // Question 38 - Adaequatheit der Hamodialyse
    Question(
        categoryId = 16,
        questionText = "Was bedeutet der Parameter Kt/V bei der Haemoidialyse, und welchen Mindestwert fordert die aktuelle Leitlinie fuer eine adaequate Dialyse?",
        answerA = "K = Dialysatfluss (L/h), t = Zeit (h), V = Patientengewicht (kg); Mindestwert 0.7 pro Sitzung",
        answerB = "K = Dialysatoer-Clearance fuer Harnstoff (ml/min), t = Dialysezeit (min), V = Harnstoff-Verteilungsvolumen (~Koerperwasser, ml); Mindestwert spKt/V >= 1.2 (KDOQI) pro Sitzung bei 3 x woeachentlicher Dialyse",
        answerC = "K = Kalium-Clearance, t = Zeit, V = Zellvolumen; misst Elektrolytclearance, Mindestwert 2.0",
        answerD = "K/t/V ist eine dimensionslose Zahl fuer die Dialysatfluessigkeitsfluss-Effizienz; Norm ist 1.0 bei 4-Stunden-Sitzung",
        correctAnswer = 1,
        explanation = "Kt/V ist ein dimensionsloser Index der Harnstoff-Dialysedosis: K (Harnstoff-Clearance des Dialysators), t (Dialysezeit), V (Verteilungsvolumen = totales Koerperwasser ~60% Koerpergewicht). spKt/V (single-pool) >= 1.2 pro Sitzung (3 x woeachentlich) ist der KDOQI-Mindestwert. Darunter ist das Ueberleben signifikant schlechter, darueber kein klinisch relevanter Zusatznutzen.",
        difficulty = 5,
        funFact = "Die Entwicklung von Kt/V als Dialyseadaequatheitsparameter durch Frank Gotch und John Sargent (1985) war eine Revolution -- vorher wurde die Dialyseadaequatheit nur durch Zeit und Symptome beurteilt. Die mathematische Formalisierung ermoeglichte erst die systematische Optimierung der Dialysedosis und erklaerte, warum kuerzere aber haeufigere Dialysen gleichwertig oder besser als laengere seltene sein koennen."
    ),

    // Question 39 - Peritonitis bei Peritonealdialyse
    Question(
        categoryId = 16,
        questionText = "Welche Keim-Konstellation bei Peritonitis unter Peritonealdialyse erfordert zwingend die Katheterentfernung ohne weiteren Therapieversuch?",
        answerA = "Pseudomonas-Peritonitis -- immer Katheterentfernung unabhaengig vom klinischen Verlauf",
        answerB = "Pilzperitonitis (Candida, Aspergillus) -- Antimykotika allein sind nicht ausreichend, da der Biofilm auf dem Katheter eliminiert werden muss; Katheterentfernung plus Antimykotika sind obligat",
        answerC = "MRSA-Peritonitis -- Vancomycin reicht nicht aus, Katheter muss immer sofort entfernt werden",
        answerD = "Koagulase-negative Staphylokokken-Peritonitis -- haeufiger Keim, aber immer Katheterentfernung noetig da Antibiotika nicht wirken",
        correctAnswer = 1,
        explanation = "Pilzperitonitis (meist Candida spp.) ist eine absolute Indikation zur sofortigen Katheterentfernung -- Antimykotika (Fluconazol, Echinocandine) allein koennen keinen Biofilm auf dem Silikonkatheter eradizieren. Persistierender Biofilm fuehrt zu therapierefraktaerer Peritonitis und lebensbedrohlicher Sepsis. Auch nach Katheterentfernung wird Antimykotika fuer mindestens 2 Wochen fortgefuehrt.",
        difficulty = 5,
        funFact = "Vor dem laparoskopischen Zeitalter war Peritonealdialyse-Peritonitis die haeufigste Ursache fuer PD-Therapieversagen -- moderne Schulungsprogramme, verbesserte Konnektionssysteme und Tunnelkatheterpflege haben die Peritonitisrate von 1 Episode/12 Monate auf < 1 Episode/24-36 Monate reduziert."
    ),

    // --- KARDIOLOGIE WEITER ---

    // Question 40 - Sinusknoten-Erkrankung
    Question(
        categoryId = 16,
        questionText = "Welcher Ionenkanal ist der Hauptinitiator der diastolischen Depolarisation im Sinusknoten, und welche zwei komplementaeren Mechanismen (Membranuhren und Calciumuhr) erklaeren sie?",
        answerA = "Kv4.3 (Ito) ist der Hauptinitiator; Membranuhren-Theorie (langsame K+-Auswaertsstroeme) und Ca2+-uhr (IP3-vermittelte SR-Ca2+-Freisetzung)",
        answerB = "HCN4 (If) ist zentral fuer die Membranuhren (If-Aktivierung nach Hyperpolarisation); Ca2+-Uhr = spontane rhythmische Ca2+-Freisetzung aus dem SR (via RyR2) aktiviert NCX-Einwaertsstrom und beschleunigt finale Depolarisation",
        answerC = "Cav3.1 (T-Typ Ca-Kanal) ist der Hauptinitiator; Membranuhren-Theorie und Ca2+-Uhr sind nicht unterscheidbar",
        answerD = "NCX im Vorwaertsmodus ist der Hauptantreiber der Membranuhren-Theorie; Calciumuhr bezieht sich auf Calmodulin-Konformationsaenderungen",
        correctAnswer = 1,
        explanation = "Zwei komplementaere Mechanismen: (1) 'Membranuhren': HCN4-If (einwaerts, aktiviert bei Hyperpolarisation), ICaT (T-Typ-Ca), verminderter IKr -- erzeugen die Phase-4-Depolarisation. (2) 'Calciumuhr': Spontane rhythmische RyR2-vermittelte Ca2+-Freisetzung aus dem SR aktiviert NCX im Vorwaerts-Modus (3Na+ ein, 1Ca2+ aus), was einen depolarisierenden Einwaertsstrom erzeugt, der die finale diastolische Depolarisation beschleunigt.",
        difficulty = 5,
        funFact = "HCN4-Knockoutmaeuese sterben embryonal -- HCN4 ist fuer die embryonale Herzentwicklung und fruehe Schrittmacheraktivitaet essentiell, bevor das autonome Nervensystem die Herzfrequenzregulation uebernimmt. Dies erklaert, warum HCN4-Inhibitoren (Ivabradin) nur bei post-embryonalen Herzen therapeutisch einsetzbar sind."
    ),

    // --- CYSTIC FIBROSIS FURTHER ---

    // Question 41 - CF-Klassen-Uebersicht
    Question(
        categoryId = 16,
        questionText = "Welche CFTR-Mutationsklasse hat die guenstigste Prognose und warum, verglichen mit Klasse I und II?",
        answerA = "Klasse V (reduzierte mRNA/Protein-Menge, partielle Funktion) -- ausreichend funktionaeles CFTR erreicht die Zelloberflaech, was mildere Phenotypen mit Restfunktion erklaert",
        answerB = "Klasse I (Nonsense-Mutation, kein Protein) hat beste Prognose da kein toxisches Protein akkumuliert",
        answerC = "Klasse III (Gating-Defekt) hat beste Prognose da das Protein korrekt zur Membrane gelangt",
        answerD = "Alle Klassen haben identische Prognose; Phaenotyeptyp haengt ausschliesslich von Modifier-Genen ab",
        correctAnswer = 0,
        explanation = "Klasse V (z.B. 3849+10kbC>T) und Klasse VI-Mutationen produzieren zumindest geringe Mengen funktionaeles CFTR -- selbst 1-5% residuale Aktivitaet korreliert mit signifikant milderem Phaenotyep (oft erhaltene Pankreasfunktion, spaeterer Lungenerkrankung). Klasse I und II produzieren kein bzw. kein oberflaechenstaendiges CFTR und sind mit den schwersten Krankheitsverlaufen assoziiert.",
        difficulty = 5,
        funFact = "Patienten mit zwei Klasse-V-Mutationen (compound heterozygot) koennen bis ins Erwachsenenalter ohne signifikante Symptome bleiben und werden manchmal als 'CF-Traeger' fehldiagnostiziert, bis Infertilitaet (kongenitale bilaterale Vas-deferens-Aplasie) oder milde Bronchiektasen sie zur Diagnose bringen."
    ),

    // Question 42 - Mukoviszidose Pankreas
    Question(
        categoryId = 16,
        questionText = "Warum entwickeln die meisten CF-Patienten eine exokrine Pankreasinsuffizienz, aber nur eine Minderheit einen CF-assoziierten Diabetes (CFRD)?",
        answerA = "Exokrine Pankreas-Azinuszellen sind gegenueber CFTR-Dysfunktion empfindlicher als Inselzellen; Inselzellen haben hoehere CFTR-Reservekapazitaet",
        answerB = "Exokrine Insuffizienz beginnt in utero durch viskoesen Sekretrueckstau in den Pankreasgaengen (autolytisch) -- beginnt frueh und progressiv. CFRD entsteht erst spaeter durch fibrotischen Umbau der Bauchspeicheldrueseals Insulinzellen (Typ 1-aehnlich) plus periphere Insulinresistenz (Typ-2-aehnlich), was Jahre braucht.",
        answerC = "Pankreasinsuffizienz ist genetisch unabhaengig von CF; CFRD entsteht durch autoimmune Zerstoerung der Betazellen",
        answerD = "Exokrine Insuffizienz ist durch Ernaeahrungstherapie reversibel; CFRD ist unvermeidlich bei allen Patienten mit schwerem Genotyp nach 30 Jahren",
        correctAnswer = 1,
        explanation = "In Pankreasgaengen foerdert CFTR Bikarbonat- und Fluessigkeitssekretion. Bei CFTR-Defekt akkumuliert viskoeses Sekret, verstopft Gaenge und fuehrt zu autodigestiver Pankreatitis schon in utero/frueh postnatal, mit progressivem Verlust der Azinuszellen. CFRD entsteht sekundaer durch zunehmende fibrotisch-zystische Destruktion der Inselzellen plus Insulinsekretion-Stoerung durch amyloidaehnliche Deposits.",
        difficulty = 5,
        funFact = "CFRD ist die haeufigste Komorbidaet bei aelteren CF-Patienten (>30 Jahre) und hat einen paradoxen Verlauf: Hunger-Blutzucker ist oft normal, aber postprandiale Spitzen sind erhoeht -- Insulingabe verbessert nicht nur den Blutzucker, sondern auch das Koerpergewicht und die Lungenfunktion, da Insulin anabol wirkt und der Katabolis CF-typisch verstaerkt ist."
    ),

    // --- INTERSTITIAL LUNG DISEASE FURTHER ---

    // Question 43 - Hypersensitivitaetspneumonitis
    Question(
        categoryId = 16,
        questionText = "Was unterscheidet die chronische fibrotische Hypersensitivitaetspneumonitis (cHP) radiologisch und pathologisch von IPF, und warum ist die Unterscheidung klinisch wichtig?",
        answerA = "cHP zeigt ausschliesslich UIP-Muster; identisch mit IPF -- Unterscheidung nur durch Serologie moeglich",
        answerB = "cHP zeigt haeffiger oberlappen- oder peribronchovaeskulaere Verteilung, Mosaikmuster, Mikronoduli und kann ein UIP-aehnliches Muster haben ABER mit zusaetzlichen Zeichen -- Differenzierung klinisch wichtig da Antigenelimination und Immunsuppression (Corticosteroide) bei cHP wirksam sind, nicht aber bei IPF",
        answerC = "cHP ist immer akut und reversibel; IPF ist per Definition chronisch-progressiv -- keine chronische cHP existiert",
        answerD = "cHP befaellt ausschliesslich das untere Drittel der Lunge symmetrisch; IPF ist asymmetrisch und bevorzugt Oberlappen",
        correctAnswer = 1,
        explanation = "Chronische HP kann ein UIP-aehnliches radiologisches und histologisches Bild zeigen, das IPF imitiert. Hinweise auf HP: oberlappenbetonte oder diffuse Verteilung (nicht nur basal), peribronchovaskulaere Beteiligung, Mosaikmuster (Air Trapping), zentrilobulaere Noduli, positive Antigenexpositionsanamnese. Die Unterscheidung ist entscheidend: Bei HP koennen Antigenelimination und Steroide die Progression stoppen, bei IPF sind Steroide schaedlich.",
        difficulty = 5,
        funFact = "Vogelhalter-Lunge (durch Vogelkot-Antigene) und Farmerlunge (durch Schimmelpilze im Heu) sind die klassischen HP-Formen -- in der modernen Medizin sind Heimbefeuchterlunge (Methanobacterium-Antigene aus Luftbefeuchtern) und HP durch Schimmelpilze in Arbeitsplaetzen zunehmend. Selbst Echtholzboeden (Hund-Antigene) wurden beschrieben."
    ),

    // --- NEPHROLOGIE FURTHER ---

    // Question 44 - Komplementsystem bei Nierenerkrankungen
    Question(
        categoryId = 16,
        questionText = "Welche Nierenerkrankung entsteht durch unkontrollierte Aktivierung des alternativen Komplementweges aufgrund einer C3-Konvertase-Regulator-Defizienz, und welche Therapie zielt auf diesen Mechanismus?",
        answerA = "IgA-Nephropathie durch C1q-Defizienz; Rituximab als Therapie",
        answerB = "C3-Glomerulopathie (dense deposit disease, C3-GN) durch Defizienz oder Auto-Antikoerper gegen CFH (Komplementfaktor H) oder CFI; Eculizumab als C5-Inhibitor",
        answerC = "Lupus-Nephritis durch klassischen Komplementweg-Ueberaktivitaet; Belimumab als Therapie",
        answerD = "Anti-GBM-Erkrankung durch alternativen Komplementweg; Plasmaaustausch eliminiert Komplementaktivatoren",
        correctAnswer = 1,
        explanation = "C3-Glomerulopathie umfasst C3-Glomerulonephritis und Dense Deposit Disease (DDD). Ursache: Defizienz oder Autoantikoper gegen CFH (haemolytisch-uraehmisches Syndrom assoziiert) oder CFI -- diese regulieren die alternative C3-Konvertase (C3bBb). Ohne Regulation: unkontrollierte C3-Spaltung, C3-Ablagerungen im Mesangium ohne Ig. Eculizumab (Anti-C5) hemmt terminale Komplementaktivierung und ist bei einigen Faellen therapeutisch.",
        difficulty = 5,
        funFact = "Eculizumab war das erste zugelassene Komplementinhibitor-Medikament (2007, fuer PNH) und ist mit ca. 500.000 EUR/Jahr eines der teuersten Medikamente der Welt -- es hat jedoch fuer atypisches HUS und C3-Glomerulopathie eine lebensrettende Wirkung bei Patienten, die sonst rasch zur Dialysepflicht fortschreiten."
    ),

    // Question 45 - Goodpasture-Syndrom
    Question(
        categoryId = 16,
        questionText = "Gegen welches spezifische Antigen sind die Autoantikoper beim Goodpasture-Syndrom (Anti-GBM-Erkrankung) gerichtet, und warum betrifft die Erkrankung sowohl Niere als auch Lunge?",
        answerA = "Gegen Nephrin (Schlitzmembranprotein); betrifft nur Niere da Nephrin lungenspezifisch nicht exprimiert wird",
        answerB = "Gegen die NC1-Domaene der alpha3-Kette von Kollagen Typ IV -- dieses Epitop (Goodpasture-Antigen) ist in der GBM und in der alveolaren Basalmembran exprimiert, was Glomerulonephritis (linear IgG-Ablagerungen) und haemorrhagische Alveolitis erklaert",
        answerC = "Gegen MPO (Myeloperoxidase); betrifft Niere und Lunge durch ANCA-Vaskulitis-Mechanismus identisch mit GPA",
        answerD = "Gegen Laminin in der Basalmembran; da Laminin ubiquitaer ist, sind alle Basalmembranen betroffen, erklaert aber auch Hautblasen",
        correctAnswer = 1,
        explanation = "Anti-GBM-Antikoerper richten sich gegen das NC1-Epitop der alpha3-Kette von Kollagen IV -- diese Kette ist in der glomerulaeren Basalmembran und der alveolaren Basalmembran konzentriert, aber in anderen Geweben weniger exponiert. Die Bindung aktiviert Komplement und rekrutiert Neutrophile, was crescentische Glomerulonephritis (halbmondfoermige Nephritis) und pulmonale Haemorrhagie erklaert.",
        difficulty = 5,
        funFact = "Rauchen ist ein klassischer Trigger fuer die Lungenmanifestation des Goodpasture-Syndroms -- Rauchen erhoeht die pulmonale Membranpermeabilitaet, sodass zirkulierende Anti-GBM-Antikoerper die alveolare Basalmembran erreichen koennen, was bei Nichtrauchern mit derselben Autoantikoper-Konstellation nur selten zur Lungenhaemorrhagie fuehrt."
    ),

    // --- TRANSPLANTATION FURTHER ---

    // Question 46 - Akute Transplantatabstossungstypen
    Question(
        categoryId = 16,
        questionText = "Was unterscheidet eine T-Zell-vermittelte Abstossungsreaktion (TCMR) von einer antikoerper-vermittelten Abstossung (ABMR) im Nierentransplantat histologisch und therapeutisch?",
        answerA = "TCMR zeigt C4d-Ablagerungen in Peritubulaerkapillaren; ABMR zeigt interstitielle Lymphozyteninfiltration -- Therapie identisch (Steroide)",
        answerB = "TCMR zeigt tubulitis und interstitielle Lymphozyteninfiltration (Banff-Kriterien); ABMR zeigt Mikrovaskularentzuendung, C4d-Ablagerungen (peritubulare Kapillaren) und DSA -- Therapie: TCMR mit Steroiden, ABMR mit Plasmaaustausch, IVIG, Anti-CD20",
        answerC = "TCMR und ABMR sind histologisch nicht unterscheidbar; Differenzierung nur durch DSA-Serum-Messung moeglich",
        answerD = "ABMR tritt nur in den ersten 6 Monaten auf; TCMR ist eine Spaetreaktion jenseits Jahr 5",
        correctAnswer = 1,
        explanation = "Banff-Klassifikation: TCMR = tubulitis (t>=1) + interstitielle Entzuendung (i>=1), keine DSA, kein C4d. ABMR = Mikrovaskularentzuendung (g + ptc), Peritubulare C4d-Ablagerung (oder DSA), Zeichen von Gewebeschaden. Therapie unterscheidet sich fundamental: TCMR anspricht gut auf hochdosierte Corticosteroide; ABMR benoetigt DSA-Elimination (PE) + B-Zell-Suppression (Rituximab, IVIG).",
        difficulty = 5,
        funFact = "C4d als Biomarker fuer ABMR war ein Durchbruch der 1990er Jahre durch die Pathologin Lorraine Racusen -- C4d ist ein kovalent gebundenes Spaltprodukt der Komplementaktivierung durch Antikoerper und markiert 'Tatort' der Antikoerper-Komplementreaktion noch lange nach dem initialen Ereignis, da es sich stabil an Endothelzellen bindet."
    ),

    // --- SPEZIELLE KARDIOLOGIE ---

    // Question 47 - Hypertropher Kardiomyopathie Elektrophysiologie
    Question(
        categoryId = 16,
        questionText = "Welche elektrophysiologischen Veraenderungen in der HCM (hypertrophen Kardiomyopathie) erhoehen das Risiko fuer ploetzlichen Herztod, und welche klinische Risikomarkierung gilt als wichtigster Einzelparameter?",
        answerA = "QT-Verlaengerung durch Ionenkanal-Remodeling; wichtigster Marker ist PQ-Zeit > 200 ms",
        answerB = "Myofibrillaere Disarray und interstitielle Fibrose erzeugen heterogene Depolarisation und Repolarisation als Arrhythmisubstrat; nicht-anhaltende ventrikulaere Tachykardie (NSVT) im Langzeit-EKG gilt als einer der wichtigsten Einzelrisikomarker, kombiniert mit max. LV-Wanddicke, Synkopen, Familienanamnese",
        answerC = "AV-Leitungsblock durch hypertrophes Septum; Herzblock-Grad 2 ist der wichtigste Marker",
        answerD = "Atrialer Thrombus durch diastolische Dysfunktion; Thromboembolic risk score ist der einzige validierte Risikomarker",
        correctAnswer = 1,
        explanation = "HCM-Arrhythmiesubstrat: myozytaere Disarray (chaotische Muskelfaseranordnung), interstitielle Fibrose (Narbensubstrat fuer Reentry) und Mikrovaskualererkrankung mit Narbenbildung. Der HCM-Risk-SCD-Score (ESC) kombiniert 8 Parameter, aber NSVT im 24h-Holter gilt neben max. Wanddicke, unerklarten Synkopen, Familienanamnese und LVOTO als Hauptrisikomarker fuer ICD-Indikation.",
        difficulty = 5,
        funFact = "HCM ist die haeufigste Ursache des ploetzlichen Herztodes bei jungen Sportlern (< 35 Jahre) in Amerika -- im Gegensatz zu Europa, wo koronare Anomalien und ARVC haeufiger als Ursachen gefunden werden. Diese geographische Diskrepanz wird durch unterschiedliche Screeningmethoden und genetische Hintergruende erklaert."
    ),

    // --- RENAL DISEASE VERTIEFT ---

    // Question 48 - Rapidly Progressive GN
    Question(
        categoryId = 16,
        questionText = "Welche histologische Gemeinsamkeit definiert die Rapidly Progressive Glomerulonephritis (RPGN), und welche drei immunpathologischen Klassen (I, II, III) werden unterschieden?",
        answerA = "Mesangiale IgA-Ablagerungen in > 50% der Glomeruli; Klassen nach Fibrose-Anteil, Komplementstatus und Ig-Subklasse",
        answerB = "Extrakapillaere Proliferation (Halbmonde/Crescents) in > 50% der Glomeruli; Klasse I = Anti-GBM (lineares IgG); Klasse II = Immunkomplex-RPGN (granulaeres Muster, z.B. Lupus, IgA); Klasse III = Pauci-immun (kein Ig, ANCA-positiv, z.B. GPA, MPA)",
        answerC = "Diffuse endokapillaere Proliferation in allen Glomeruli; Klassen nach klinischem Verlauf (akut, subakut, chronisch)",
        answerD = "Fibrinoid-Nekrose in Arteriolen > 50% der Proben; Klassen nach betroffenem Gefaesstyp (Arteriole, Arterie, Kapillare)",
        correctAnswer = 1,
        explanation = "RPGN = klinisch rasch fortschreitende GN + histologisch extrakapillaere Proliferation (Halbmonde aus Parietalzellenproliferation + Monozyten). Drei Immuntypen: I (Anti-GBM-Antikoerper, lineares IgG; Goodpasture), II (Immunkomplex-Ablagerungen, granulaer; Lupus, Post-Strep, IgA, SLE), III (Pauci-immun, kein/wenig Ig; meist ANCA-assoziiert -- GPA, MPA, EGPA).",
        difficulty = 5,
        funFact = "Pauci-immune RPGN (Klasse III, ANCA-assoziiert) ist die haeufigste Ursache fuer RPGN in Westeuropa und Nordamerika -- obwohl im Biopsat wenig Immunkomplex vorhanden sind (pauci = wenig), ist der Verlauf oft dramatisch mit Nierenfunktionsverlust innerhalb von Tagen bis Wochen, was unverzuegliche Immunsuppression erfordert."
    ),

    // Question 49 - Tubulosaere Azidose Typen
    Question(
        categoryId = 16,
        questionText = "Was unterscheidet proximale renale tubulaere Azidose (RTA Typ II) von distaler RTA (Typ I) hinsichtlich Serum-Kalium, Urin-pH und Pathomechanismus?",
        answerA = "Typ I: Hyperkalaemie, Urin-pH < 5.5 (sauer); Typ II: Hypokalaemie, Urin-pH variabel",
        answerB = "Typ II (proximal): Hypokalaemie, Urin-pH variabel (initial alkalisch bei normaler Bikarbonat-Beladung, dann sauer nach Bikarbonat-Wasting unter den Schwellenwert); Defekt: reduzierte NaHCO3-Reabsorption im PCT. Typ I (distal): Hypokalaemie, Urin-pH immer > 5.5 (unfaehig Urin anzusaeuern); Defekt: Defekte H+-ATPase oder Rueckdiffusion von H+",
        answerC = "Typ I und II sind klinisch identisch; Unterscheidung nur durch genetische Testung moeglich",
        answerD = "Typ II zeigt Hyperchloraemie; Typ I zeigt Hypernatraemie -- das ist das einzige klinische Unterscheidungsmerkmal",
        correctAnswer = 1,
        explanation = "RTA Typ II: proximaler Tubulus reabsorbiert HCO3- ungenuegend (Schwellenwert erniedrigt). Bei normaler Bikarbonatbeladung flutet HCO3- ins Tubulussystem und der Urin ist alkalisch; nach Bikarbonatverlust (unter Schwellenwert) wird der restliche HCO3- normal reabsorbiert, Urin wird sauer. RTA Typ I: distaler Tubulus kann kein saures Milieu erzeugen (H+-ATPase-Defekt), Urin bleibt immer > pH 5.5 trotz systemischer Azidose.",
        difficulty = 5,
        funFact = "RTA Typ IV (Hypoaldosteronismus-assoziiert) ist die haeufigste RTA-Form -- sie entsteht klassisch beim diabetischen Nephropathie-Patienten mit Hyporeninisierung und zeigt die einzige Hyperkalaemie unter den RTAs. Die Hyperkalaemie selbst hemmt Ammoniak-Synthese im Tubulus und erklaert die Azidose moeglicherweise mehr als der Aldosteronmangel."
    ),

    // Question 50 - Haemolytisch-uraehmisches Syndrom
    Question(
        categoryId = 16,
        questionText = "Was ist der mechanistische Unterschied zwischen typischem HUS (STEC-HUS) und atypischem HUS (aHUS), und warum ist Eculizumab nur bei aHUS, nicht aber bei STEC-HUS indiziert?",
        answerA = "STEC-HUS entsteht durch Autoimmunantikoper; aHUS durch bakterielle Exotoxine -- Eculizumab hemmt Bakterienwachstum, daher nur bei atypischem HUS nuetzlich",
        answerB = "STEC-HUS: Shiga-Toxin direkt schaedigt Endothelzellen (Toxin-induziert, selbstlimitierend); aHUS: unkontrollierte alternative Komplementaktivierung durch CFH/CFI/MCP-Defizienz oder CFB/C3-Gain-of-Function -- Eculizumab hemmt C5, das nur bei aHUS chronisch ueberaktiviert ist, bei STEC-HUS jedoch allenfalls sekundaer beteiligt und sich spontan erholt",
        answerC = "Beide HUS-Formen haben identischen Mechanismus; Eculizumab ist bei beiden wirksam, aber bei STEC-HUS kontraindiziert wegen Meningokokken-Risiko",
        answerD = "aHUS ist immer genetisch, STEC-HUS immer erworben; Eculizumab wirkt nur bei genetischen Erkrankungen",
        correctAnswer = 1,
        explanation = "Typisches HUS: EHEC-Shiga-Toxin bindet Gb3-Rezeptoren auf renalen Endothelzellen, loest endotheliale Apoptose aus -- haemolytische Anaemie, Thrombozytopenie und Nierenversagen. Selbstlimitierend. Atypisches HUS: Deregulierte alternative Komplementaktivierung (CFH/I/MCP-Mutationen oder Autoantikoper gegen CFH) zerstoert chronisch Endothelzellen. Eculizumab blockiert C5, das terminale Komplement, und ist lebenslang indiziert um Schuebe zu verhindern.",
        difficulty = 5,
        funFact = "Der EHEC-Ausbruch 2011 in Deutschland (Bockshornklee-Sprossen) war der groesste STEC-HUS-Ausbruch der Geschichte mit ueber 850 HUS-Faellen -- in diesem Ausbruch wurde Eculizumab ausnahmsweise off-label auch bei STEC-HUS eingesetzt, da die Erkrankungsschwere ausserordentlich war, obwohl der Nutzen bis heute kontrovers diskutiert wird."
    )

)

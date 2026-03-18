package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun sportQuestionsMaster3(): List<Question> = listOf(

    // ── MASTER (difficulty = 5) ── 50 questions ──────────────────────────────

    // ── Sportepigenetik / Molekularbiologie (10) ──────────────────────────────

    // Question 1 – Epigenetics: DNA methylation in endurance athletes
    Question(
        categoryId = 6,
        questionText = "Welcher epigenetische Mechanismus erklaert, warum Ausdauertraining ueber mehrere Generationen hinweg nachweisbar vererbte Anpassungen an Skelettmuskulatur produzieren kann, ohne die DNA-Sequenz zu veraendern?",
        answerA = "Telomerverlaengerung durch Telomerase-Aktivierung in Muskelstammzellen",
        answerB = "DNA-Methylierungsveraenderungen an CpG-Inseln in regulatorischen Regionen von Mitochondriengenen wie PGC-1alpha",
        answerC = "Histondeacetylierung in Promotorregionen von Glykolysegenen, was anaerobe Kapazitaet steigert",
        answerD = "Retroviraler Gentransfer von Sauerstoffbindungsgenen zwischen Muskelzellen",
        correctAnswer = 1,
        explanation = "Studien (Alibegovic et al., 2010; Nitert et al., 2012) zeigten, dass Ausdauertraining zu spezifischen DNA-Methylierungsveraenderungen an CpG-Inseln in Promotoren von PGC-1alpha fuehrt, dem Masterregulator der Mitochondrienbiogenese. Diese Muster koennen ueber Spermien/Eizellen weitergegeben werden, was transgenerationale epigenetische Vererbung erklaert.",
        difficulty = 5,
        funFact = "PGC-1alpha wird in der Sportwissenschaft als 'Ausdauergen-Schalter' bezeichnet. Maeuseexperimente zeigten, dass Nachwuchs von trainierten Eltern ohne eigenes Training bessere Mitochondriendichte hatten – ein Phänomen, das als 'epigenetisches Trainingsgedaechtnis' diskutiert wird."
    ),

    // Question 2 – Epigenetics: ACTN3 gene and sprint performance
    Question(
        categoryId = 6,
        questionText = "Welche Funktion hat das ACTN3-Gen (R577X-Polymorphismus) in der Leistungsphysiologie, und warum fehlt das funktionale Protein bei ca. 18% der Weltbevoelkerung ohne Nachteile im Alltag?",
        answerA = "ACTN3 kodiert fuer Alpha-Actinin-3, ein schnelles Muskelprotein; Traeger des XX-Genotyps zeigen keine Einschraenkung, da Alpha-Actinin-2 kompensiert, aber verlieren messbar Sprintleistung",
        answerB = "ACTN3 reguliert die Calciumausschüttung im sarkoplasmatischen Retikulum; XX-Traeger sind resistenter gegen Muskelkater",
        answerC = "ACTN3 steuert die Satellitenzellproliferation; Deletion fuehrt zu hyperthrophen Muskeln die mehr Sauerstoff verbrauchen",
        answerD = "ACTN3 ist ausschliesslich in Herzmuskelzellen aktiv und hat keinen Einfluss auf Skelettmuskel-Leistung",
        correctAnswer = 0,
        explanation = "Alpha-Actinin-3 stabilisiert Z-Scheiben in schnellen Typ-II-Muskelfasern. Der R577X-Polymorphismus erzeugt ein Stopp-Codon; homozygote XX-Traeger haben kein Alpha-Actinin-3. Alpha-Actinin-2 (ACTN2) kompensiert vollstaendig fuer Alltagsfunktionen, aber Sprinter und Kraftathletinnen zeigen statistisch haeufiger den RR-Genotyp (Yang et al., 2003).",
        difficulty = 5,
        funFact = "In einer australischen Studie hatten 95% der Elitesprinter den RR- oder RX-Genotyp, waehrend unter Elite-Ausdauerathletinnen der XX-Genotyp ueberrepraesentiert war. Das ACTN3-Gen gilt als das am besten untersuchte sportbezogene Gen – ueber 200 Studien wurden dazu publiziert."
    ),

    // Question 3 – Epigenetics: miRNA in muscle adaptation
    Question(
        categoryId = 6,
        questionText = "Welche Rolle spielen Mikro-RNAs (miRNAs) wie miR-206 in der sportinduzierten Skelettmuskeladaptation, und wie unterscheidet sich ihr Wirkmechanismus von klassischen Transkriptionsfaktoren?",
        answerA = "miRNAs erhoehen direkt die Transkriptionsrate von Aktin- und Myosyngenen durch Bindung an RNA-Polymerase-II",
        answerB = "miRNAs binden komplementaer an 3'-UTR-Regionen von Ziel-mRNAs und hemmen deren Translation oder foerdern deren Abbau, wodurch sie posttranskriptionell ganze Genregulations-Netzwerke koordinieren",
        answerC = "miRNAs fungieren als Hormontransporter im Blut und aktivieren Androgenrezeptoren in Muskelkernen",
        answerD = "miRNAs synthetisieren direkt Strukturproteine und umgehen damit den Translationsapparat",
        correctAnswer = 1,
        explanation = "miRNAs sind ca. 22 Nukleotide lang und binden durch komplementaere Basenpaarung an 3'-UTR-Regionen von Ziel-mRNAs. miR-206 ist muskelspezifisch ('myomiR') und reguliert MyoD, Connexin43 und Follistatin. Nach Krafttraining steigt miR-206 signifikant, was Myoblasten-Differenzierung foerdert und Myostatinsignaling daempft.",
        difficulty = 5,
        funFact = "Exosomale miRNAs aus Skelettmuskel koennen im Blut zirkulieren und andere Organe beeinflussen – Herz, Leber, Gehirn. Diese 'Myokine auf RNA-Basis' koennen erklaeren, warum Sport systemische Anti-Aging-Effekte hat, die weit ueber den Muskel hinausgehen."
    ),

    // Question 4 – Molecular biology: mTOR pathway in hypertrophy
    Question(
        categoryId = 6,
        questionText = "Welcher Molekularweg ist der zentrale Sensor fuer mechanische Belastung in Skelettmuskelzellen und integriert Aminosaeure-Verfuegbarkeit mit Muskelproteinsynthese?",
        answerA = "AMPK-PGC-1alpha-Achse, die ausschliesslich bei Energiemangel aktiv wird und Katabolismus verhindert",
        answerB = "mTORC1 (mechanistic Target of Rapamycin Complex 1), das durch PI3K-Akt-Signaling, mechanoaktivierte Integrine und Aminosaeuresensoren (Ragulator-Rag-Komplex) aktiviert wird",
        answerC = "NF-kappaB-Signalweg, der proinflammatorische Zytokine nach Mikroverletzung ausschuettet",
        answerD = "MAPK-ERK-Kaskade, die ausschliesslich durch Wachstumshormone aus der Hypophyse aktiviert wird",
        correctAnswer = 1,
        explanation = "mTORC1 ist der Hauptintegrator von Muskelwachstumssignalen: mechanischer Stress aktiviert Integrin-FAK-Signaling, Aminosaeuren (besonders Leucin) aktivieren den Ragulator-Komplex an Lysosomenmembranen, und IGF-1 aktiviert PI3K-Akt. Alle Wege konvergieren auf mTORC1, das S6K1 und 4E-BP1 phosphoryliert und Proteinsynthese ankurbelt.",
        difficulty = 5,
        funFact = "Rapamycin (Sirolimus), ein Immunsuppressivum, hemmt mTOR und blockiert Muskelhypertrophie bei Krafttraining. Dies fuehrt zu einer interessanten Forschungsfrage: Wenn mTOR-Inhibitoren Krebs hemmen, muss man Krebspatienten von Sport abraten? Die Antwort ist Nein – andere Signalwege kompensieren."
    ),

    // Question 5 – Epigenetics: Telomere length in athletes
    Question(
        categoryId = 6,
        questionText = "Welcher Zusammenhang besteht zwischen Ausdauersport und Telomerlaenge in Leukozyten, und welcher molekulare Mechanismus erklaert diesen Effekt?",
        answerA = "Intensives Training verkuerzt Telomere durch oxidativen Stress, was biologisches Altern beschleunigt",
        answerB = "Moderates Ausdauertraining erhoert Telomeraseaktivitaet (TERT-Expression) und verlaengert oder stabilisiert Telomere gegenueber Inaktivitaet",
        answerC = "Telomerlaenge ist rein genetisch determiniert und wird durch Training nicht beeinflusst",
        answerD = "Krafttraining verlaengert Telomere, Ausdauertraining hat keinen Einfluss",
        correctAnswer = 1,
        explanation = "Mehrere Meta-Analysen (Arsenis et al., 2017) zeigten, dass Ausdauersportler signifikant laengere Telomere haben als Inaktive. Der Mechanismus: Ausdauertraining aktiviert den TERT-Promotor durch PGC-1alpha und reduziert oxidativen Stress durch Hochregulation von Superoxiddismutase und Katalase, was die Telomerabbaurate senkt.",
        difficulty = 5,
        funFact = "Spitzensportler im Ausdauersport haben biologisch bis zu 10 Jahre juengere Leukozyten-Telomere als gleichaltrige Inaktive. Ein Marathonlaeufer mit 50 Jahren kann die Telomerlaenge eines 40-Jaehrigen Bueroarbeiters haben – ein konkreter molekularer Anti-Aging-Marker."
    ),

    // Question 6 – Molecular biology: Heat shock proteins in sport
    Question(
        categoryId = 6,
        questionText = "Welche Funktion uebernehmen Hitzeschockproteine (HSPs), insbesondere Hsp70, bei intensivem koerperlichem Training, und warum sind sie fuer die Sportphysiologie therapeutisch relevant?",
        answerA = "Hsp70 transportiert Sauerstoff in Muskeln und ersetzt Myoglobin bei Hypoxie",
        answerB = "Hsp70 fungiert als Chaperone: Verhindert Fehlfaltung hitzestress-geschaedigter Proteine, unterstuetzt Proteinneubildung nach Training und aktiviert Zelltodwege bei schwerer Hyperthermie",
        answerC = "Hsp70 reguliert ausschliesslich die Glucoseaufnahme in Muskelzellen und hat keine strukturelle Funktion",
        answerD = "Hsp70 ist ein Entzuendungsmarker ohne direkte Schutzfunktion im Muskel",
        correctAnswer = 1,
        explanation = "Intensives Training erzeugt thermischen und oxidativen Stress; denaturierte Proteine akkumulieren. Hsp70 erkennt hydrophobe Bereiche fehlgefalteter Proteine, bindet sie (Chaperone-Funktion) und hilft bei Refolding oder markiert sie fuer proteasomalen Abbau. Chronisches Training erhoert basale Hsp70-Spiegel, was Hitzeschlagresistenz verbessert.",
        difficulty = 5,
        funFact = "Vor der Saison trainieren Profiradfahrer manchmal in Hitzekammern, um HSP-Induktion zu erzielen – das sogenannte 'Heat Acclimation Training'. Es erhoert Blutvolumen, verbessert Schwitzeffizienz und erhoert Hsp70, was messbar Leistung bei Hitzerennen steigert."
    ),

    // Question 7 – Epigenetics: Muscle memory at epigenetic level
    Question(
        categoryId = 6,
        questionText = "Wie erklaert die Epigenetik das Phaenomen des 'Muskelgedaechtnisses' – also die schnellere Wiederherstellung von Muskelgroesse nach einer Trainingspause im Vergleich zum erstmaligen Aufbau?",
        answerA = "Durch gespeicherte Aminosaeurereserven in Muskelfasern, die bei Detraining nicht vollstaendig abgebaut werden",
        answerB = "Durch persistente DNA-Hypomethylierung an Promotoren von Muskelhypertrophiegenen: Bei erneutem Training werden diese Gene schneller reaktiviert",
        answerC = "Durch Langzeitpotenzierung in Motoneuronen des Rueckenmarks, die motorische Programme dauerhaft speichern",
        answerD = "Durch erhoehte Kapillardichte, die bei Trainingspausen erhalten bleibt und sofortigen Naehrstofftransport ermoeglicht",
        correctAnswer = 1,
        explanation = "Kristoffer Toldnes Cumming et al. (2016) und folgende Studien zeigten, dass nach Hypertrophietraining Promotoren von Muskelgenen (IGF-1, Myogenin) dauerhaft hypomethyliert bleiben – auch nach Detraining. Bei Retraining werden diese Gene daher schneller transkribiert, was Hypertrophie 2-3x schneller als bei Ersttraining erzeugt.",
        difficulty = 5,
        funFact = "Das Konzept des epigenetischen Muskelgedaechtnisses koennte Anti-Doping-Implikationen haben: Wenn frueheres Doping epigenetische Spuren hinterlaesst, koennte man theoretisch auch Jahre spaeter noch Dopingvorteile nachweisen – eine Idee, die Forscher an der Universitaet von Oslo aktiv untersuchen."
    ),

    // Question 8 – Molecular biology: VEGF and angiogenesis
    Question(
        categoryId = 6,
        questionText = "Welcher primaere Signalweg induziert trainingsbedingtes Kapillarwachstum (Angiogenese) im Skelettmuskel, und welche Hypoxie-sensitiven Molekuele aktivieren ihn?",
        answerA = "Erythropoetin (EPO) bindet an Muskelzellen und triggert VEGF-Ausschuettung durch MAPK-Aktivierung",
        answerB = "Muskelkontraktionen senken PO2, was HIF-1alpha stabilisiert, das seinerseits VEGF-Transkription induziert; VEGF bindet dann an Endothelzellen und stimuliert Kapillarsprossung",
        answerC = "Insulin-like Growth Factor-1 aktiviert eNOS in Endothelzellen, was direkt Gefaessproliferation ohne VEGF ausloest",
        answerD = "Laktatatakkumulation blockiert Prolyl-Hydroxylasen, senkt Sauerstoffverbrauch und verhindert Angiogenese",
        correctAnswer = 1,
        explanation = "Bei intensiver Muskelarbeit sinkt pO2 in Muskelfasern. Unter Hypoxie werden Prolyl-Hydroxylasen (PHDs) gehemmt, die normalerweise HIF-1alpha hydroxylieren und fuer proteasomalen Abbau markieren. Stabiles HIF-1alpha translokt in den Kern und aktiviert VEGF-A-Transkription. VEGF bindet an VEGFR2 auf Endothelzellen, was Proliferation, Migration und Tubulusmorphogenese triggert.",
        difficulty = 5,
        funFact = "EPO-Doping simuliert Hochhoehentraining durch erythrozytaere Effekte, beeinflusst aber auch direkt VEGF: Einige Studien zeigten, dass rekombinantes EPO Angiogenese foerdert, unabhaengig von Erythropoese. Das erklaert, warum EPO-Doping moeglicherweise laengerfristige Effekte auf Kapillardichte hat als bislang gedacht."
    ),

    // Question 9 – Epigenetics: Histone modification and exercise
    Question(
        categoryId = 6,
        questionText = "Welche Histonmodifikation wird unmittelbar (binnen Minuten) nach akutem Ausdauertraining in Skelettmuskelzellen beobachtet und welchen Transkriptions-Effekt hat sie?",
        answerA = "Histon-H3-Lysine-9-Trimethylierung (H3K9me3): Foerdert Heterochromatinbildung und schaltet Entzuendungsgene dauerhaft ab",
        answerB = "Histon-H3-Lysine-36-Acetylierung (H3K36ac) durch PCAF: Aktiviert Fettsaeure-Oxidationsgene fuer schnellere Energiebereitstellung",
        answerC = "Histon-H3-Lysine-4-Trimethylierung (H3K4me3) korreliert mit trainingsinduzierten Genen; Histondeacetylasen (HDACs) werden durch AMPK-Phosphorylierung aus dem Kern exportiert, was Histonacetylierung erhoert und Chromatin oeffnet",
        answerD = "Histon-H2B-Ubiquitinierung durch Training, die Replikationsgene aktiviert und Satellitenzellproliferation erzwingt",
        correctAnswer = 2,
        explanation = "Akutes Training aktiviert AMPK, das HDAC4 und HDAC5 phosphoryliert und deren Export aus dem Kern bedingt. Ohne HDACs bleiben Histone acetyliert (offenes Chromatin), was Transkriptionsfaktoren wie MEF2 und CREB Zugang zu Promotoren von Mitochondriengenen ermoegt. H3K4me3 markiert aktive Promotoren und steigt parallel.",
        difficulty = 5,
        funFact = "HDAC-Inhibitoren sind in der Onkologie als Krebsmedikamente zugelassen. Die Entdeckung, dass Training natuerliche HDAC-Inhibition erzeugt, fuehrte zu Studien ueber 'Exercise as medicine' – Sport produziert epigenetische Effekte aehnlich Chemotherapeutika, aber ohne Toxizitaet."
    ),

    // Question 10 – Molecular biology: Myostatin and athletic performance
    Question(
        categoryId = 6,
        questionText = "Myostatin (GDF-8) ist ein negativer Regulator der Skelettmuskelmasse. Welcher Signalweg vermittelt seine hemmende Wirkung, und wie exploitiert die Sportwissenschaft dieses Wissen?",
        answerA = "Myostatin aktiviert Akt/mTOR direkt und induziert Muskelhypertrophie; Myostatin-Ueberexpression kaempft gegen Atrophie",
        answerB = "Myostatin bindet an Activin-Typ-II-Rezeptoren, aktiviert Smad2/3-Signaling, das IGF-1-Rezeptorexpression unterdrückt und Proteinsynthese hemmt; Sport hemmt Myostatin durch Follistatin-Hochregulation",
        answerC = "Myostatin wird ausschliesslich in Herzmuskel exprimiert und hat keine Funktion im Skelettmuskel",
        answerD = "Myostatin foerdert Fettzellentwicklung aus Muskelstammzellen; Sport hat keinen Einfluss auf Myostatin-Spiegel",
        correctAnswer = 1,
        explanation = "Myostatin bindet an ActRIIA/B, was Smad2/3 phosphoryliert. Phospho-Smad2/3 transloziert in den Kern und reprimiert IGF-1-Signaling und Proteinsynthesegene. Krafttraining erhoert Follistatin, den natuerlichen Myostatin-Antagonisten, und senkt zirkulierendes Myostatin. Myostatin-Null-Maeuse haben 2-3x normales Muskelgewicht – der sogenannte 'Rinderherde-Phaenotyup' tritt auch bei Menschen mit Mutationen auf.",
        difficulty = 5,
        funFact = "2004 wurde in Deutschland ein Kleinkind geboren, das eine Myostatin-Genmutation hatte: Mit 4 Jahren konnte er 3-kg-Hanteln horizontal halten. Seine Mutter war Sprinterin. Myostatin-Inhibitoren werden als Therapeutika bei Muskeldystrophie entwickelt und stehen im Fokus des Anti-Doping-Radars der WADA."
    ),

    // ── Sportrecht / Verfassungsrecht (10) ───────────────────────────────────

    // Question 11 – Sports law: CAS arbitration jurisdiction
    Question(
        categoryId = 6,
        questionText = "Auf welcher Rechtsgrundlage beruht die obligatorische Zustaendigkeit des Court of Arbitration for Sport (CAS/TAS) fuer Athleten, und welches grundlegende Spannungsverhaeltnis zu nationalen Verfassungsrechten wurde 2018 im Pechstein-Fall hoechstrichterlich diskutiert?",
        answerA = "CAS-Zustaendigkeit beruht auf UN-Voelkerrecht; staatliche Gerichte sind grundsaetzlich ausgeschlossen ohne Rechtsmittelmoeglichkeit",
        answerB = "Athleten unterzeichnen Schiedsvereinbarungen als Teilnahmebedingung; das BVerfG pruefte ob diese erzwungene Schiedsvereinbarung mit Art. 2 Abs. 1 GG (allgemeine Handlungsfreiheit) und dem Justizgewaehrungsanspruch vereinbar ist",
        answerC = "CAS-Entscheidungen sind unverbindliche Empfehlungen; nationale Gerichte koennen sie frei ueberschreiben",
        answerD = "Die Zustaendigkeit basiert auf internationalem Sportrecht als eigenstaendiger Rechtsordnung (lex sportiva) ohne Anknuepfung an Staatsrecht",
        correctAnswer = 1,
        explanation = "Im Fall Claudia Pechstein (Eisschnelllaeufen, Dopingsperre) prueft der BGH 2016 und das BVerfG 2018, ob die erzwungene CAS-Schiedsklausel (Athlet muss unterzeichnen, sonst keine Teilnahme an Wettkampf) gegen das Grundgesetz verstoesst. Das BVerfG sah keine Verletzung, da die Sportverbandsmitgliedschaft freiwillig ist und CAS ausreichend institutionelle Unabhaengigkeit besitzt.",
        difficulty = 5,
        funFact = "Der CAS in Lausanne entschied 2022 ueber die gesperrte russische Olympiadelegation und wurde damit zum wichtigsten Sportgerichtshof der Geschichte. Clauda Pechstein kaempfte 14 Jahre lang rechtlich um ihre Ehre – ihre zweijährige Dopingsperre (2009-2011) basierte auf biologischen Passportdaten, nicht auf direktem Doping-Nachweis."
    ),

    // Question 12 – Sports law: Bosman ruling implications
    Question(
        categoryId = 6,
        questionText = "Welche drei konkreten Rechtswirkungen hatte das EuGH-Urteil im Fall Bosman (1995, Rs. C-415/93) auf das europaeische Profisportrecht, und welcher EU-Rechtsgrundsatz war der entscheidende Hebel?",
        answerA = "Es schuetzte Spieler vor Doping-Vorwuerfen durch Beweislastumkehr, basierend auf EU-Datenschutzrecht",
        answerB = "Es untersagte Transferentschaedigungen nach Vertragsende, schaffte Auslaenderbeschraenkungen fuer EU-Buerger ab und begruendete das Recht auf freie Spielerberatung – Rechtsgrundlage war Art. 45 AEUV (Freizuegigkeit der Arbeitnehmer)",
        answerC = "Es verpflichtete Vereine zur Mindestlohnzahlung gemaess EU-Sozialcharta und schuf einen europaeischen Mindestvertrag fuer Profisportler",
        answerD = "Es gab Verbanden das Recht, Transfersperren gegen nicht-kooperierende Vereine zu verhaengen",
        correctAnswer = 1,
        explanation = "Jean-Marc Bosman klagte gegen Transferentschaedigung nach Vertragsablauf und Auslaenderbeschraenkungen im belgischen Fussball. Der EuGH erkannte: (1) Transferentschaedigungen nach Vertragsende verstoessen gegen Arbeitnehmerfreizuegigkeit, (2) Auslaenderbeschraenkungen fuer EU-Buerger sind rechtswidrig, (3) Spieler sind schutzbeduerfte Arbeitnehmer. Dies transformierte den europaeischen Transfermarkt fundamental.",
        difficulty = 5,
        funFact = "Vor Bosman konnten Vereine beliebig viele Auslaender verpflichten, aber nur 3 im Kader einsetzen. Nach Bosman explodierte der Spielermarkt: Spitzenvereine kauften europaweit ein. Das Urteil gilt als groesste juristische Transformation des europaeischen Fussballs und kostete Jean-Marc Bosman selbst eine Karriere – kein Verein wollte ihn nach dem Rechtsstreit verpflichten."
    ),

    // Question 13 – Sports law: WADA Code constitutional issues
    Question(
        categoryId = 6,
        questionText = "Welches verfassungsrechtliche Problem ergibt sich aus der strikten Haftung (strict liability) im WADA-Code, und wie haben staatliche Gerichte in Deutschland diese Problematik bewertet?",
        answerA = "Strict liability verstoesst gegen EU-Datenschutzrecht (DSGVO), da Athleten biologische Daten ohne Einwilligung offenbaren muessen",
        answerB = "Strict liability kehrt die Beweislast um und bestraft ohne Schuldnachweis; das OLG Muenchen (Pechstein, 2015) und spaetere Gerichte prueften Vereinbarkeit mit dem Schuldprinzip aus Art. 1 GG (Menschenwuerde) sowie dem Verhaeltnismaessigkeitsprinzip",
        answerC = "Strict liability verstoesst gegen internationale Handelsabkommen, da Athleten als Wirtschaftsgut behandelt werden",
        answerD = "Strict liability ist verfassungsrechtlich unproblematisch, da Sport kein Grundrecht geniesst",
        correctAnswer = 1,
        explanation = "Das Schuldprinzip als Ausfluss der Menschenwuerde (Art. 1 GG) fordert grundsaetzlich, dass Strafen ein Verschulden voraussetzen. Strict liability bestraft auch ohne Vorsatz oder Fahrlaessigkeit. Deutsche Gerichte sahen dies als problematisch, akzeptierten aber den Interessenausgleich: Dopingkontrolle waere bei subjektivem Schulderfordernis faktisch unmoeglich, daher ist strict liability verhaeltnismaessig unter Sportrecht-Vorbehalt.",
        difficulty = 5,
        funFact = "Der Tennis-Star Maria Scharapowa wurde 2016 fuer 15 Monate gesperrt, weil Meldonium auf die Verbotsliste gesetzt wurde, waehrend sie es noch einnahm. Strict liability bedeutete: kein Vorsatznachweis noetig. Der CAS reduzierte die Sperre auf 15 Monate, da sie nachweislich nicht wusste, dass es verboten war – ein seltenes Zugestaendnis an den Schuldgedanken."
    ),

    // Question 14 – Sports law: Olympic Charter and state sovereignty
    Question(
        categoryId = 6,
        questionText = "Welchen Rechtscharakter hat die Olympische Charta und in welchem Spannungsverhaeltnis steht die IOC-Regel 50 (Verbot politischer Statements auf dem Wettkampffeld) zu nationalen Grundrechten auf Meinungsfreiheit?",
        answerA = "Die Olympische Charta ist ein voelkerrechtlicher Vertrag; Regel 50 ist bindend fuer alle Vertragsstaaten und geniesst Vorrang vor nationalem Verfassungsrecht",
        answerB = "Die Olympische Charta ist privatrechtliches Verbandsrecht des IOC als Schweizer Verein; Regel 50 bindet Athleten vertraglich, nicht staatlich; die verfassungsrechtliche Dimension ergibt sich aus mittelbarer Drittwirkung von Art. 5 GG",
        answerC = "Die Olympische Charta ist unverbindliches soft law ohne Sanktionsmechanismus; Athleten koennen politische Statements ungehindert machen",
        answerD = "Als UN-Sonderorganisation hat das IOC quasi-staatliche Befugnisse und kann Athleten strafrechtlich belangen",
        correctAnswer = 1,
        explanation = "Das IOC ist ein privater Verein nach Schweizer Recht (Artikel 60ff. ZGB). Die Olympische Charta ist privates Verbandsrecht. Athleten unterschreiben verbindliche Zustimmungserklaerungen. In Deutschland gilt ueber mittelbare Drittwirkung: Grundrechte (Art. 5 GG Meinungsfreiheit) strahlen auf privatrechtliche Verhaeltnisse aus. Gerichte wuerden abwaegen: legitimes Ziel der Neutralitaet vs. Meinungsfreiheit.",
        difficulty = 5,
        funFact = "Nach den Black-Power-Saluts von Tommie Smith und John Carlos 1968 wurden beide lebenslang von den Olympischen Spielen ausgeschlossen. 2020 lockerte das IOC Regel 50 geringfuegig, erlaubte aber immer noch keine Gesten waehrend Wettkampf und Podium. Atleten-Kommissionen weltweit kritisieren Regel 50 als unverhältnismaessigen Eingriff in Grundrechte."
    ),

    // Question 15 – Sports law: Doping as criminal law issue
    Question(
        categoryId = 6,
        questionText = "Das Anti-Doping-Gesetz (AntiDopG) von 2015 in Deutschland kriminalisierte Eigenverantwortliches Doping zum Zweck des Wettkampfbetrugs. Welche verfassungsrechtliche Debatte entfachte das, und auf welches Grundrecht berief sich die Gegenposition?",
        answerA = "Das AntiDopG verstoesst gegen Art. 12 GG (Berufsfreiheit), da Athleten als Berufssportler kriminalisiert werden",
        answerB = "Die Kriminalisierung eigenkörperschaedigenden Verhaltens kollidiert mit Art. 2 Abs. 2 GG (koerperliche Unversehrtheit als Abwehrrecht) und Art. 2 Abs. 1 GG (allgemeine Handlungsfreiheit/Selbstbestimmungsrecht); Paternalismusdebatte: darf der Staat mich vor mir selbst schuetzen?",
        answerC = "Das AntiDopG verstoesst gegen Art. 3 GG (Gleichheitsgrundsatz), da nur Leistungssportler und nicht Freizeitsportler kriminalisiert werden",
        answerD = "Keine verfassungsrechtliche Debatte; das AntiDopG wurde einstimmig als unproblematisch akzeptiert",
        correctAnswer = 1,
        explanation = "Das AntiDopG stellt Eigenverantwortliches Doping im organisierten Wettkampf unter Strafe. Die verfassungsrechtliche Kritik: Art. 2 Abs. 1 GG schutzt auch die Freiheit, sich selbst zu schaedigen (volenti non fit iniuria). Der Staat muss einen legitimen Zweck nachweisen, der ueber Paternalismus hinausgeht. Gegenargument: Betrug an anderen Athleten und Publikum ist legitimer Schutzgrund.",
        difficulty = 5,
        funFact = "Das AntiDopG (2015) fuehrte auch 'Umfeld-Doping' ein: Trainer, Aerzte, Manager koennen fuer Athleten-Doping strafrechtlich haften. Dies war international ein Novum. Bis 2023 gab es in Deutschland mehrere Verurteilungen nach AntiDopG, meist gegen Betreuer – ein Zeichen, dass das Gesetz tatsaechlich Anwendung findet."
    ),

    // Question 16 – Sports law: Gender verification history
    Question(
        categoryId = 6,
        questionText = "Welche wissenschaftlichen und Menschenrechts-Probleme fuehrten zur Abschaffung des obligatorischen Geschlechtsverifikationstests (Barr-Body-Test) im internationalen Sport, und welche Nachfolgeregelungen entstanden?",
        answerA = "Der Barr-Body-Test war zu teuer; er wurde durch Passkontrollen ersetzt ohne wissenschaftliche Diskussion",
        answerB = "Barr-Body-Tests prueften Geschlechtschromatin, klassifizierten aber Frauen mit Klinefelter oder AIS (Androgen-Insensitivitaets-Syndrom) als 'maennlich' obwohl sie phaenotypisch weiblich waren; Entwuerdigung und Fehlerquote fuehrten zu Abschaffung 1999; IAAF/IOC wechselten zu Testosteronschwellen (Hyperandrogenie-Regeln)",
        answerC = "IOC schaffte Tests 1992 ab, weil bei den Barceloner Spielen keine Irregularitaeten gefunden wurden",
        answerD = "Genetische Geschlechtsbestimmung wurde durch Blutgruppenanalyse ersetzt, die zuverlaessigere Marker liefert",
        correctAnswer = 1,
        explanation = "Barr-Body-Tests (1968-1992 olympisch) erkannten Geschlechtschromatinkörper. Frauen mit AIS (Androgen-Insensitivitaetssyndrom, XY-Chromosomen, weiblicher Phänotyp) wurden als 'nicht weiblich' klassifiziert – trotz lebenslanger weiblicher Sozialisation. Die IAAF schaffte Tests 1992, IOC 1999 ab. Nachfolger: Testosteronschwellenwerte (aktuell 5 nmol/L fuer DSD-Athletinnen nach Caster Semenya-Fall).",
        difficulty = 5,
        funFact = "Caster Semenya (suedafrikanische Laeuferinn) klagte vor dem CAS und dem EuGH gegen IAAF-Hyperandrogenie-Regeln. Sie verlor beim CAS 2019 (notwendige Einschraenkung fuer fairen Wettbewerb), gewann aber 2023 vor dem Europaeischen Gerichtshof fuer Menschenrechte – das groesste Rechtspuzzle im Sportrecht des Jahrzehnts."
    ),

    // Question 17 – Sports law: Salary cap constitutional issues
    Question(
        categoryId = 6,
        questionText = "Wie vereinbaren europaeische Ligen Salary-Cap-Regelungen mit EU-Wettbewerbsrecht (Art. 101 AEUV – Kartellverbot), und welche Ausnahmelogik nutzen Sportverbände?",
        answerA = "Salary Caps sind grundsaetzlich nach EU-Recht verboten und existieren in Europa nicht",
        answerB = "Sportverbände argumentieren mit der 'Meca-Medina'-Doktrin (EuGH 2006): Sportregeln koennen trotz wettbewerbsbeschraenkender Wirkung EU-rechtskonform sein, wenn sie sportlicher Eigenart dienen, verhaeltnismaessig sind und zwingende Wettbewerbsbalance-Interessen verfolgen",
        answerC = "Salary Caps gelten als arbeitsrechtliche Massnahmen, nicht als Wettbewerbsbeschraenkung, und sind daher von Art. 101 AEUV ausgenommen",
        answerD = "Die EU hat Sport explizit aus ihrem Wettbewerbsrecht ausgenommen (Art. 165 AEUV schuetzt Sportautonomie absolut)",
        correctAnswer = 1,
        explanation = "Im Meca-Medina-Urteil (2006) lehnte der EuGH eine generelle Sportausnahme vom EU-Recht ab, schuf aber einen Pruefrahmen: Sportregeln sind EU-rechtskonform, wenn (1) legitimes sportliches Ziel (Wettbewerbsbalance), (2) Massnahme inherent verbunden mit Sportziel, (3) verhaeltnismaessig. Salary Caps werden darunter als vertretbar angesehen, muessen aber offen und diskriminierungsfrei ausgestaltet sein.",
        difficulty = 5,
        funFact = "Die NFL hat in den USA einen harten Salary Cap, der juristisch durch die Tarifautonomie (Collective Bargaining Agreement zwischen NFL und NFLPA) gestuetzt ist. In Europa existieren 'Financial Fair Play'-Regeln (UEFA) als weichere Alternative, die auf Profitabilitaet statt absolutem Lohndeckel setzen – aktuell als 'Financial Sustainability Regulations' neu gestaltet."
    ),

    // Question 18 – Sports law: Lex sportiva concept
    Question(
        categoryId = 6,
        questionText = "Was versteht die Rechtswissenschaft unter 'Lex Sportiva' als internationalem Rechtsphänomen, und welche Kritik formuliert die staatsrechtliche Dogmatik an diesem Konzept?",
        answerA = "Lex Sportiva ist das nationale Sportgesetz jedes Landes; Kritik betrifft fehlende Harmonisierung",
        answerB = "Lex Sportiva bezeichnet ein transnational entstehendes Sportprivatrecht durch CAS-Praezedenzfaelle und Verbandsrecht, das faktisch staatliches Recht ersetzt; Kritik: demokratisch nicht legitimiert, keine Gewaltentrennung, begrenzte Rechtsschutzmoeglichkeiten fuer Athleten",
        answerC = "Lex Sportiva ist ein akademisches Konstrukt ohne praktische Rechtswirkung; Verbandssatzungen sind nur vertragsrechtlich bindend",
        answerD = "Lex Sportiva ist identisch mit olympischem Voelkerrecht und wird durch den UN-Sicherheitsrat durchgesetzt",
        correctAnswer = 1,
        explanation = "Der Begriff 'Lex Sportiva' (gepraegt u.a. von Gardiner und Nafziger) bezeichnet das sich durch CAS-Rechtsprechung und internationale Verbandsregel-Auslegung bildende transnationale Sportrecht. Kritiker wie Nicola Porro und Jan Osterhaus betonen: Keine demokratische Legitimation, kein unabhaengiges Richteramt, eingeschraenkte Transparenz. Das IOC hat als Privatverein mehr regulatorische Macht als manche Kleinstaaten.",
        difficulty = 5,
        funFact = "Der CAS hat seit 1984 ueber 10.000 Schiedsurteile gefaellt, die zusammen eine umfangreiche Lex-Sportiva-Literatur ergeben. Bei Olympia 2020 (Tokio) entschied der CAS binnen Stunden ueber Doping-Notfaelle – eine Schiedsgeschwindigkeit, die kein staatliches Gericht der Welt erreichen koennte."
    ),

    // Question 19 – Sports law: Media rights and competition law
    Question(
        categoryId = 6,
        questionText = "Warum wurde die zentrale Vermarktung von Bundesliga-TV-Rechten durch die DFL lange als Kartellverstos gewertet, und welche Ausnahmeentscheidung erteilte das Bundeskartellamt?",
        answerA = "Zentrale TV-Rechtevermarktung ist grundsaetzlich erlaubt; es gab keine kartellrechtliche Pruefung",
        answerB = "Jeder Verein ist ein eigenstaendiges Unternehmen; gemeinsame Rechte-Vermarktung ist Preisabsprache (GWB § 1 / Art. 101 AEUV); das BKartA erteilte jedoch Freistellung, weil ohne Zentralvermarktung schwache Vereine nicht finanzierbar waeren und sportliche Balance verloren ginge",
        answerC = "TV-Rechte sind Persoenlichkeitsrechte der Spieler, nicht der Vereine; eine kartellrechtliche Problematik existiert nicht",
        answerD = "Das Kartellamt hat Zentralvermarktung dauerhaft verboten; Vereine vermarkten individuell",
        correctAnswer = 1,
        explanation = "Die DFL buendelt TV-Rechte aller 36 Bundesligavereine und verkauft sie zentral an Sky, DAZN etc. Ohne Buendelung waere ein Horizontalabkommen konkurrierender Unternehmen (Vereine) eine Preisabsprache. Das BKartA erkannte aber den sogenannten 'Solidaritaetsvorbehalt': Ohne Umverteilung wuerden kleine Vereine nicht ueberleben, was die Liga zerstoert. Daher Freistellung nach § 2 GWB.",
        difficulty = 5,
        funFact = "Die DFL vergibt TV-Rechte in 4-Jahres-Paketen. 2021-2025 zahlten Sky und DAZN zusammen ca. 1,1 Mrd. Euro/Saison. Zum Vergleich: Die englische Premier League erzielt 3+ Mrd. Euro/Saison – trotz gleicher Spielerzahl, dank globaler Markenpraesenz."
    ),

    // Question 20 – Sports law: Integrity of competition
    Question(
        categoryId = 6,
        questionText = "Das Konzept der 'Spielmanipulation' (Match Fixing) wird in Deutschland strafrechtlich unterschiedlich behandelt je nach Tatbestand. Welche Paragraph-Kombination ist relevant und was ist das systematische Problem?",
        answerA = "Ausschliesslich § 263 StGB (Betrug) bei Sportwetten-Manipulation; reine sportliche Manipulation ohne Wettbetrug ist in Deutschland nicht strafbar",
        answerB = "§ 265c/265d StGB (Sportwettbetrug und Manipulation beruflicher Sportveranstaltungen seit 2017) regulieren direkte Manipulation; davor war nur § 263 StGB (Betrug) einschlaegig, was das Problem hatte, dass sportinterne Manipulation ohne Wettbeteiligung straflos war",
        answerC = "§ 299 StGB (Bestechung im Wirtschaftsverkehr) deckt alle Faelle ab; § 265c StGB ist verfassungswidrig",
        answerD = "Spielmanipulation ist rein sportrechtlich reguliert; deutsches Strafrecht ist generell nicht anwendbar",
        correctAnswer = 1,
        explanation = "Bis 2017 fehlte ein spezifischer Straftatbestand: § 263 StGB erforderte Vermoegensnachteil (Wettender betrogen). Reine sportliche Manipulation (Schiedsrichterkorruption ohne Wettbetrug) war straflos. Die §§ 265c und 265d StGB fuellten diese Luecke: 265c schuetzt Sportwetten-Integritaet, 265d den fairen Berufssport als oeffentliches Gut an sich – ein strafrechtliches Novum.",
        difficulty = 5,
        funFact = "Im Bochum-Skandal (Hoyzer-Affaere 2004/05) wurden Bundesliga-Schiedsrichter bestochen. Robert Hoyzer wurde wegen Beihilfe zum Betrug (nicht Manipulation selbst) zu 2 Jahren 5 Monaten Haft verurteilt. Der Skandal loeste die Gesetzesdebatte aus, die 13 Jahre spaeter in §§ 265c/d StGB muendete."
    ),

    // ── Historische Sportwissenschaft (10) ───────────────────────────────────

    // Question 21 – History of sports science: Guts Muths
    Question(
        categoryId = 6,
        questionText = "Johann Christoph Friedrich GutsMuths gilt als Begruender der wissenschaftlichen Gymnastikmethodik. Welches Werk (1793) und welches padagogische Prinzip unterscheiden seine Methode fundamental von militaerischem Exerzieren?",
        answerA = "Sein Werk 'Turnkunst fuer die Jugend' begruendete Nationalturnen als Wehrertuechtigung; Drill und Disziplin als Kernprinzip",
        answerB = "'Gymnastik fuer die Jugend' (1793) integrierte Bewegungsuebungen systematisch in Erziehungsprogramme; Leitsatz: Gesundheitsfoerderung, Freude und ganzheitliche Menschenbildung statt militaerischen Nutzens",
        answerC = "'Spiele zur Uebung des Koerpers' (1796) war ein Regelkatalog fuer Volksspiele ohne padagogische Theorie",
        answerD = "GutsMuths begruendete modernes Turnen als wettkampfbasiertes System mit Leistungsvergleich als Kernmotivation",
        correctAnswer = 1,
        explanation = "GutsMuths' 'Gymnastik fuer die Jugend' (1793, Philanthropinum Schnepfenthal) war das erste systematische padagogische Sportlehrbuch. Sein Ansatz: Bewegung als Bildungsmittel fuer Koerper, Seele und Verstand, nicht fuer militaerische oder wirtschaftliche Nutzung. Er systematisierte Leibesuebungen in Kategorien (Klettern, Springen, Laufen) und begruendete damit Sportpaedagogik als Wissenschaft.",
        difficulty = 5,
        funFact = "GutsMuths Werk wurde ins Englische, Franzoesische und Daenische uebersetzt und beeinflusste Per Henrik Ling in Schweden (Schwedische Gymnastikmethode) sowie Phokion Heinrich Clias in der Schweiz. Die 'Gymnastikmethode' des 18. Jahrhunderts war damit die erste global rezipierte Sportwissenschaft."
    ),

    // Question 22 – History of sports science: Etienne-Jules Marey
    Question(
        categoryId = 6,
        questionText = "Etienne-Jules Mareys chronophotographische Studien (1882-1895) revolutionierten die Sportwissenschaft des 19. Jahrhunderts. Welche methodische Innovation erlaubte erstmals die Bewegungsanalyse, und welchen konkreten Einfluss hatte er auf Trainingstheorie?",
        answerA = "Marey entwickelte das Elektrokardiogramm und mass erstmals Herzfrequenz waehrend sportlicher Belastung",
        answerB = "Marey nutzte eine Serienkamera (fusil photographique) mit bis zu 12 Bildern/Sekunde um menschliche und tierische Bewegungen zu analysieren; sein Schluss: Lauftechnik ist optimierbar und 'natuerliche' Bewegung oft ineffizient",
        answerC = "Marey entwickelte Spirometrie als Leistungsdiagnostik und schuf den ersten VO2max-Messdurchlauf",
        answerD = "Marey einfuehrte biomechanische Berechnungen auf Basis newtonscher Mechanik ohne Bildgebung",
        correctAnswer = 1,
        explanation = "Mareys Chronophotographie (ab 1882, Physiological Station Paris) ermoeglichte erstmals die Zerlegung von Bewegung in Einzelphasen. Sein 'fusil photographique' schoss 12 Bilder/Sekunde auf eine rotierende Platte. Erkenntnisse: Pferde haben kurze vierbeinige Flugphasen, Voegel nutzen aerodynamische Strategien. Direkter Einfluss auf Trainingstheorie: Schwimmen, Laufen und Radfahren wurden biomechanisch optimiert.",
        difficulty = 5,
        funFact = "Edweard Muybridges Serienfotos (1877, galoppierendes Pferd) entstanden zugleich und unabhaengig in den USA. Marey und Muybridge korrespondierten; ihre Methoden vereinten sich zum Fundament moderner Bewegungswissenschaft und filmischer Chronophotographie. Muybridges Pferde-Studie loeste eine Wette: Ob alle vier Hufe gleichzeitig in der Luft sind – ja, tatsaechlich."
    ),

    // Question 23 – History of sports science: Angelo Mosso
    Question(
        categoryId = 6,
        questionText = "Der italienische Physiologe Angelo Mosso (1846-1910) begruendete die Ermuedungsforschung im Sport. Was war sein methodischer Beitrag durch den 'Ergographen', und welche Theorie der Muedigkeit vertrat er?",
        answerA = "Mosso entwickelte den Ergographen als Kraftmessgerat zur Messung maximaler Muskelkraft; er erklaerte Muedigkeit rein durch Milchsaeure-Akkumulation in Muskeln",
        answerB = "Der Ergograph mass rhythmische Fingerbeuger-Kontraktionen unter Standardbedingungen bis zur Ermuedung; Mosso postulierte, dass Muedigkeit primar eine zerebrale Erscheinung (Gehirnmuedigkeit) ist, nicht nur ein lokales Muskelversagen",
        answerC = "Mosso einfuehrte Elektrostimulation als Ermuedungstest und wies nach, dass muede Muskeln mehr Strom benoetigen",
        answerD = "Mosso entwickelte Spirometrie-Ermuedungstests und war der erste, der Sauerstoffschuld nach Belastung mass",
        correctAnswer = 1,
        explanation = "Mossos Ergograph (1884, Turin) mass Fingerflexor-Ermuedung durch rhythmische Gewichtshebung bis zum Versagen. Revolutionaer: Mosso erkannte, dass Ermuedung auch durch Zirkulationsveraenderungen und zentralnervale Hemmung entsteht. Sein Buch 'La Fatica' (1891) praegte Generationen von Sportphysiologen und inspirierte spater Wahrnehmungen von 'zentraler Governor-Theorie' (Tim Noakes).",
        difficulty = 5,
        funFact = "Mosso experimentierte auch mit Hochhoehen-Physiologie: Er bestieg den Mont-Rosa und studierte Hoehenerkrankung. Sein Ergraph-Konzept lebte weiter im modernen Handkraft-Dynamometer und im Wingate-Anaerobaertest. Die zentrale Ermuedungstheorie wurde 2000 von Tim Noakes als 'Central Governor Model' neu belebt."
    ),

    // Question 24 – History of sports science: Archibald Vivian Hill
    Question(
        categoryId = 6,
        questionText = "A.V. Hill erhielt 1922 den Nobelpreis fuer Physiologie fuer Arbeiten, die direkte Auswirkungen auf Sportphysiologie hatten. Was waren seine zentralen Entdeckungen und inwiefern widerlegte er Milchsaeuretheorien seiner Zeit?",
        answerA = "Hill entdeckte ATP als universellen Energietraeger und wies nach, dass Adenosindiphosphat-Spaltung Muskelkraft erzeugt",
        answerB = "Hill mass Waermeproduktion in Muskeln und bewies, dass Kontraktionswaerme und Erholungswaerme getrennte chemische Phasen sind; er begruendete das Konzept der maximalen Sauerstoffaufnahme (VO2max) und widerlegte, dass Milchsaeure direkte Kontraktionsursache ist",
        answerC = "Hill entwickelte die Kreatin-Phosphat-Theorie der Muskelkontraktion und wies Kreatin als primaere Energiequelle bei Sprints nach",
        answerD = "Hill entdeckte Erythropoetin als Hohenanpassungshormon durch Bergexpeditionen in den Alpen",
        correctAnswer = 1,
        explanation = "Hills Arbeiten (mit Otto Meyerhof, daher gemeinsamer Nobelpreis) zeigten: Muskelwaerme entsteht in zwei Phasen (Kontraktionswaerme + Erholungswaerme). Erholungswaerme entspricht aerober Oxidation. Hill folgerte: Milchsaeure wird aerob rueckgebaut, nicht direkt fuer Kontraktion verwendet. Er begruendete den Begriff 'Sauerstoffschuld' und schuf mit seinen Oxymeter-Messungen das Fundament fuer VO2max-Konzept.",
        difficulty = 5,
        funFact = "A.V. Hill testierte auch eigene Laufleistungen auf einer provisorischen Bahn in London als Selbstexperiment. Er mass seine VO2max auf ca. 70 ml/kg/min – fuer einen nicht-trainierten Wissenschaftler ausserordentlich. Hills Methoden-Manuell wurde zum Standardwerk der Sportphysiologie der 1920er bis 1950er Jahre."
    ),

    // Question 25 – History of sports science: First sports medicine congress
    Question(
        categoryId = 6,
        questionText = "Wann und wo fand der erste internationale Sportmedizin-Kongress statt, welche Organisation entstand daraus, und welche politische Dimension hatte der historische Kontext?",
        answerA = "1906 in Athen, zeitgleich mit den Zwischen-Olympiaden; die IOC-Aerzte-Kommission entstand daraus",
        answerB = "1928 in St. Moritz anlaeasslich der II. Olympischen Winterspiele; die FIMS (Federation Internationale de Medicine Sportive) wurde gegruendet; Kontext: Wachstum des Profisports und erste Dopingdebatte der Moderne",
        answerC = "1912 in Stockholm waehrend der V. Olympischen Spiele; WHO-Sportzusatz-Kommission wurde geschaffen",
        answerD = "1900 in Paris, zeitgleich mit Weltausstellung; internationaler Aerzteverband des Sports entstand als Unterabteilung der IOC",
        correctAnswer = 1,
        explanation = "Der erste Internationale Kongress fuer Sportmedizin fand 1928 in St. Moritz statt. Die FIMS (heute World Association of Exercise and Sport Science, WASM) wurde als Dachverband gegruendet. Historischer Kontext: Olympische Winterspiele 1928, wachsender Profisport, erste Berichte ueber Stimulanzienmissbrauch bei Radrennen und Box-Kaempfen.",
        difficulty = 5,
        funFact = "Deutschland spielte in der fruehen Sportmedizin eine Vorreiterrolle: Das Deutsche Institut fuer Sportmedizin in Berlin (1919) war die erste derartige staatliche Einrichtung weltweit. Die Nationalsozialisten nutzten Sportmedizin fuer Rassenideologie, was nach 1945 die Disziplin belastete und zur kritischen Aufarbeitung der eigenen Geschichte zwang."
    ),

    // Question 26 – History of sports science: Roger Bannister
    Question(
        categoryId = 6,
        questionText = "Roger Bannisters Unterschreitung der 4-Minuten-Meile 1954 gilt als Meilenstein der Sportgeschichte. Welche sportphysiologische Vorbereitung nutzte er, die fuer ihre Zeit methodisch voraus war, und warum war er als Neurologe besonders interessiert an der Leistungsgrenze?",
        answerA = "Bannister nutzte Hochhoehentraining in Kenia und war primae an Kreislauf-Grenzen interessiert; Neurologie war irrelevant",
        answerB = "Bannister wendete strukturiertes Intervalltraining (als Oxford-Medizinstudent mit begrenzter Trainingszeit) an; als Neurologe war er an der psychologischen Leistungsbarriere interessiert: Er glaubte, die 4-Minuten-Meile sei eine mentale, nicht nur physische Grenze",
        answerC = "Bannister verwendete anabole Steroide unter aerzlicher Aufsicht; als Mediziner konnte er legal Substanzen testen",
        answerD = "Bannister nutzte ausschliesslich laengere Dauerlaeufe ohne Intervallmethode; die 4-Minuten-Grenze war fuer ihn rein biomechanisch definiert",
        correctAnswer = 1,
        explanation = "Bannister trainierte tatsaechlich nur ca. 30-45 Minuten taeglich als vielbeschaeftigter Medizinstudent (Oxford) und spaeter Neurologe in London. Er nutzte systematisches Intervalltraining auf Cinder-Bahn. Seine neurologische Perspektive: Die 4-Minuten-Grenze war psychologisch verankert – kaum jemand hatte sie gewagt. Sobald er sie brach (3:59.4 am 6. Mai 1954), folgten John Landy (46 Tage spaeter) und viele andere.",
        difficulty = 5,
        funFact = "Bannister setzte seinen Rekord in Oxford (Iffley Road Track) bei widrigen Wetterbedingungen (Wind). Er trainierte nie mehr als 1 Stunde pro Tag und wurde Neuro-Kliniker. Sein Weltrekord stand nur 46 Tage. Er schrieb als erster von der 'Schwelle zwischen Moeglichem und Unmöglichem' – ein Konzept, das die Sportpsychologie bis heute praegt."
    ),

    // Question 27 – History of sports science: Cold War sports science
    Question(
        categoryId = 6,
        questionText = "Wie nutzte die DDR systematisch Sportwissenschaft als Staatsprojekt, und welche wissenschaftlich-ethischen Erkenntnisse wurden nach 1990 bei der Aufarbeitung gewonnen?",
        answerA = "Die DDR fuehrte hauptsaechlich biomechanische Optimierungsstudien durch; staatliches Doping war ein Mythos der westlichen Propaganda",
        answerB = "Das FKS Leipzig (Forschungsinstitut fuer Koerperkultur und Sport) war Zentrum eines systematischen Dopingprogramms (Staatsplan 14.25); Aufarbeitung zeigte: Athleten wurden ohne informierte Einwilligung mit Anabolika (Oral-Turinabol) versorgt; Langzeitschaeden bei Frauen dokumentiert",
        answerC = "Die DDR kooperierte offen mit WADA-Vorlaeufern und war fuehrend in legaler Leistungsoptimierung durch Ernaehrungswissenschaft",
        answerD = "DDR-Sportwissenschaft beschraenkte sich auf padagogische Talentsichtung; Doping existierte nur im Profifussball",
        correctAnswer = 1,
        explanation = "Staatsplan 14.25 (seit 1974) organisierte systematisches Doping in der DDR. Das FKS Leipzig entwickelte Dosierungsprotokolle fuer Oral-Turinabol (Chlordehydromethyltestosteron). Nach 1990 wurden Akten des Ministeriums fuer Staatssicherheit ausgewertet. Ergebnisse: Ca. 10.000 Athleten betroffen, Frauen litten unter Virilisierung, Fortpflanzungsschaeden, erhoehten Krebsraten.",
        difficulty = 5,
        funFact = "Der BRD-Sportausschuss zahlte nach 1990 Entschaedigungen an DDR-Dopingopfer. Heidi Krieger (Kugelstosserin, mehrfach dopingversorgt) unterzog sich spaeter einer Geschlechtsumwandlung und ist heute Andreas Krieger. Er/sie klagte erfolgreich: 2000 wurden DDR-Sportfunktionaere verurteilt – das erste Strafurteil fuer staatlich organisierten Sportbetrug."
    ),

    // Question 28 – History of sports science: Paavo Nurmi training
    Question(
        categoryId = 6,
        questionText = "Paavo Nurmi (Finnland, 9 olympische Goldmedaillen 1920-1928) gilt als erster moderner Wissenschaftsathlet. Welche Trainingsmethode und welche technische Innovation nutzte er, die fuer seine Zeit revolutionaer waren?",
        answerA = "Nurmi trainierte ausschliesslich intuitiv und lehnte Zeitmessung ab; seine Leistungen basierten rein auf genetischer Ueberlegen",
        answerB = "Nurmi lief als erster systematisch mit einer Stoppuhr in der Hand und steuerte Renntempo exakt; er trainierte nach Intervallprinzipien und variierte Pacestrategien wissenschaftlich",
        answerC = "Nurmi nutzte Sauerstoffmasken-Training in Druckkammern und war erster Anwender von Hoehenlager",
        answerD = "Nurmi entwickelte gemeinsam mit finnischen Wissenschaftlern die erste Herzfrequenzmessung waehrend des Laufens",
        correctAnswer = 1,
        explanation = "Nurmi trug als erster Laeufer bei Rennen eine Stoppuhr und plante sein Tempo mathematisch exakt, um gleichmaessige Teilzeitzeiten zu erzielen. Sein Training: Taeglich bis zu 20 km, strukturiert mit Tempolaeufen und Erholungsphasen – revolutionaer fuer eine Zeit, in der Athleten 'nach Gefuehl' trainierten. Sein Coach Lauri Pikhala entwickelte nordische Trainingstheorien, die Nurmi systematisch umsetzte.",
        difficulty = 5,
        funFact = "Nurmi wurde von der IAAF 1932 kurz vor den Olympischen Spielen in Los Angeles wegen Professionalismus gesperrt – er hatte angeblich Aufwandsentschaedigungen erhalten. Es gilt als eine der groessten Sportrechts-Ungerechtigkeit des 20. Jahrhunderts. Bei den Spielen wuerde er voraussichtlich seinen zehnten Goldmedaillen-Lauf absolviert haben."
    ),

    // Question 29 – History of sports science: First sport psychology institute
    Question(
        categoryId = 6,
        questionText = "Wo und wann wurde das erste sportpsychologische Institut der Welt gegruendet, und welche Methoden standen im Mittelpunkt der Forschung des Gruenders Carl Diem?",
        answerA = "1920 in Wien durch Alfred Adler; Leistungsmotivation aus individueller Kompensationspsychologie war zentrales Thema",
        answerB = "1920 in Berlin (Deutsche Hochschule fuer Leibesuebungen) durch Robert Werner Schulte; Reaktionszeitmessung, Willenstaerke-Tests und Konzentrationsmessung als Hauptmethoden",
        answerC = "1925 in Moskau durch A.Z. Puni fuer kommunistische Leistungsoptimierung; Konditionierungsprinzipien nach Pawlow",
        answerD = "1930 in Leipzig durch Carl Diem als Vorbereitung fuer Olympia 1936; Rasse und Koerper als Forschungsfokus",
        correctAnswer = 1,
        explanation = "Robert Werner Schulte grundete 1920 an der Deutschen Hochschule fuer Leibesuebungen in Berlin das weltweit erste sportpsychologische Institut. Schulte entwickelte Apparate zur Reaktionszeitbestimmung und Konzentrationstest-Batterien fuer Athleten. Parallel entstanden in der Sowjetunion (A.Z. Puni) erste sportpsychologische Arbeiten, die ideologisch gepraeagt waren.",
        difficulty = 5,
        funFact = "Carl Diem, Generalsekretaer der Deutschen Olympischen Gesellschaft und Vater des olympischen Fackellaufs (erfunden fuer 1936), war kein Sportpsychologe, aber entscheidend fuer Institutionalisierung der Sportwissenschaft. Sein Verhaeltnis zum NS-Regime wird bis heute historisch kontrovers diskutiert."
    ),

    // Question 30 – History of sports science: Biomechanics founding
    Question(
        categoryId = 6,
        questionText = "Giovanni Alfonso Borelli (1608-1679) gilt als Vater der Biomechanik. Welches Werk und welche Methode grundete er, und welchen Irrtum ueber Muskelkraft enthielt sein Modell?",
        answerA = "Borelli schrieb 'De Motu Animalium' (1680/81) und berechnete Muskelkraefte durch Hebelgesetze; er unterschaetzte Muskelkraft systematisch weil er Muskeln als simple Seile (nicht als aktive Kontraktoren) modellierte",
        answerB = "Borelli entwickelte Wirbelsaeulenmechanik und war erster Chiropraktiker; seine Methode war rein klinisch ohne Mathematik",
        answerC = "Borelli mass Herzfrequenz bei Tieren mit einer Wasseruhr und schloss auf Muskelarbeit aus Pulsfrequenz",
        answerD = "Borelli benutzlegte Cadaverstudien und begruendete anatomisch-deskriptive Sportmorphologie ohne Mechanik-Bezug",
        correctAnswer = 0,
        explanation = "'De Motu Animalium' (posthum 1680/81) war das erste Werk, das Bewegung von Tieren und Menschen durch Mechanik und Mathematik (Hebelgesetze, Statik) analysierte. Borelis Irrtum: Er modellierte Muskeln als Seile mit konstanter Kraft, ohne die aktive Laengen-Spannungs-Kurve zu kennen. Dennoch: Seine Berechnungen der Huefte als Hebelwerk waren fuer die Zeit primaer richtig und grundlegend.",
        difficulty = 5,
        funFact = "Borelli berechnete, dass ein Mensch beim Heben eines Gewichts etwa zehnmal mehr Kraft aufwenden muss als das Gewicht selbst, wegen der Hebelverhaeltnisse der Muskeln. Dies erklaerte erstmals, warum Muskeln so viel Kraft erzeugen muessen und warum Knochen so stabil sein muessen – ein Paradigmenwechsel in Anatomie und Physiologie."
    ),

    // ── Sportphilosophie / Aesthetik (10) ────────────────────────────────────

    // Question 31 – Sports philosophy: Suits' definition of game
    Question(
        categoryId = 6,
        questionText = "Bernard Suits definierte in 'The Grasshopper: Games, Life and Utopia' (1978) Spielen durch vier notwendige und hinreichende Bedingungen. Welche Komponente nennt er 'Lusory Attitude' und warum ist sie philosophisch zentral?",
        answerA = "Lusory Attitude ist das Streben nach Sieg um jeden Preis; sie erklaert, warum Sportler cheaten",
        answerB = "Lusory Attitude ist die freiwillige Akzeptanz ineffizienter Mittel (Regelwerk) um das Spielziel zu erreichen; ohne diese Haltung gibt es kein Spiel, sondern nur Zweckrationalitaet",
        answerC = "Lusory Attitude bezeichnet die aesthetische Erfahrung beim Zuschauen; Spielen ist ohne Publikum philosophisch unvollstaendig",
        answerD = "Lusory Attitude ist Suits Begriff fuer Kooperationsbereitschaft; ohne sie degeneriert Sport zu Krieg",
        correctAnswer = 1,
        explanation = "Suits' Definition: Spielen = (1) vorzurechtes Ziel (Vorstand), (2) Mittelverbot ausser denen im Regelwerk, (3) konstitutive Regeln, (4) Lusory Attitude. Lusory Attitude: Der Spieler akzeptiert freiwillig, in bestimmten Bahnen zu spielen, obwohl er das Ziel effizienter erreichen koennte (z.B. Ball mit Hand statt Fuss tragen im Fussball). Diese Akzeptanz konstituiert erst Spiel.",
        difficulty = 5,
        funFact = "Suits' 'Grasshopper' ist ein Philosophie-Klassiker im Dialog-Format. Der Grasshopper argumentiert, dass spielen die perfekte menschliche Aktivitaet ist – reine Aktivitaet ohne instrumentellen Wert. Er stirbt gluecklich, weil er sein Leben spielend verbracht hat. Das Buch inspirierte Philosophen von John Rawls bis Alasdair MacIntyre."
    ),

    // Question 32 – Sports philosophy: Internalism vs. externalism in sport
    Question(
        categoryId = 6,
        questionText = "Was unterscheidet Internalismus und Externalismus in der Sportethik-Debatte um den intrinsischen Wert von Sport, und welche Position vertritt William Morgan in 'Leftist Theories of Sport' (1994)?",
        answerA = "Internalisten glauben, Sport hat keinen intrinsischen Wert; Externalisten sehen Sport als wertvoll fuer Gesundheit. Morgan ist Externalist.",
        answerB = "Internalisten (Formalists/Mutualists) meinen, der Wert des Sports liegt in seinen internen Gutern (Exzellenz, Faertigkeiten, Herausforderung); Externalisten sehen Sport instrumentell (Gesundheit, Gemeinschaft). Morgan verteidigt einen 'Leftist Internalism': Sport hat intrinsische Werte, aber sie sind historisch-sozial konstituiert, nicht abstraktformal",
        answerC = "Beide Positionen stimmen ueberein; Morgan schuef eine Synthese-Theorie ohne Unterschied",
        answerD = "Internalismus bezieht sich auf nationale Sportsysteme; Externalismus bezeichnet internationale Sportbewegungen",
        correctAnswer = 1,
        explanation = "Der Formalism (Suits, D'Agostino) sieht Sport-Regeln als konstitutiv fuer den Wert; Mutualism (Morgan) erweitert: Sport hat interne Gueter (virtue, craft, excellence) a la MacIntyre, aber diese entstehen in sozialen Praktiken, nicht im Vakuum. Morgans 'Leftist Internalism' koppelt intrinsische Gueter an konkrete historische Praktiken und kritisiert kommerzielle Korrumpierung dieser Gueter.",
        difficulty = 5,
        funFact = "Alasdair MacIntyres Begriff 'internal goods of practices' aus 'After Virtue' (1981) war Grundlage fuer Morgans Sportphilosophie. MacIntyre selbst erwaehnte Sport nur als Randbeispiel – es war Morgan, der diesen Begriff systematisch auf den Sport anwandte und eine ganze Schule der Sportethik begruendete."
    ),

    // Question 33 – Sports philosophy: Aesthetics of sport
    Question(
        categoryId = 6,
        questionText = "Welchen Unterschied macht David Best (1974) zwischen 'aesthetic sports' und 'purposive sports' in Bezug auf aesthetische Erfahrung, und welche Konsequenz hat dies fuer die philosophische Frage: Kann Fussball schoen sein?",
        answerA = "Aesthetic sports sind visuell attraktiv; purposive sports sind haesslich; Fussball kann nie aesthetisch sein",
        answerB = "Aesthetic sports (Kunstturnen, Eiskunstlauf) haben Aesthetik als konstitutives Ziel; purposive sports (Fussball, Boxen) verfolgen ein ausserästhetisches Ziel (Tor, Niederschlag), koennen aber zufaellig aesthetisch sein; Schoenheit in Fussball ist 'incidental aesthetic quality'",
        answerC = "Best unterscheidet nicht zwischen Sportarten; alle Sportarten haben gleiche aesthetische Kapazitaet",
        answerD = "Purposive sports sind im Gegensatz zu aesthetics sports von essentiell mathematischer Natur und koennen nur durch Statistik bewertet werden",
        correctAnswer = 1,
        explanation = "Bests Differenzierung: In aesthetischen Sports (Eiskunstlauf, Rhythmische Sportgymnastik) IST Aesthetik das Leistungskriterium (Kampfrichter bewerten Aesthetik). In Zwecksports wie Fussball ist das Ziel (Tor schiessen) unabhaengig von Aesthetik. Ein schoener Fallrückzieher ist nicht mehr wert als ein unschoener Kopfball – beide zaehlen ein Tor. Aesthetik in Fussball ist 'incidental' – sie begleitet Leistung, ohne sie zu konstituieren.",
        difficulty = 5,
        funFact = "Diese Unterscheidung hat praktische Bedeutung: Sollte Kunstturnen mit absolutem Regelwerk bewertet werden (wie Hochsprung: entweder drueber oder nicht)? Oder mit aesthetischem Urteil? Seit 2006 haben die Verbände umstrukturiert: Turnen hat jetzt numerische Schwierigkeit (D-Score) getrennt von Ausfuehrungsschoere (E-Score) – ein Kompromiss zwischen Best's beiden Kategorien."
    ),

    // Question 34 – Sports philosophy: Paternalism in sport rules
    Question(
        categoryId = 6,
        questionText = "Mit welchem philosophischen Argument rechtfertigt Norbert Claeys (und aehnlich Mike McNamee) Schutzregeln im Sport (Helmpflicht, Gewichtsklassen) gegen den Paternalismusvorwurf?",
        answerA = "Schutzregeln sind nicht paternalistisch, weil Athleten sie durch Verbandsmitgliedschaft implizit akzeptieren; Einwilligung hebt Paternalismusproblematik vollstaendig auf",
        answerB = "Weicher Paternalismus ist gerechtfertigt: Wenn Entscheidungsbedingungen nicht vollstaendig informiert/autonom sind (Wettkampfdruck, Hierarchieabhaengigkeit), darf der Staat oder Verband eingreifen um echte Autonomie zu schuetzen",
        answerC = "Sport ist kein Bereich fuer Paternalismusdebatte; Regeln sind rein technischer Natur ohne moralische Dimension",
        answerD = "Schutzregeln sind moralisch immer unzulaessig; nur Athleten selbst duerfen Risikoentscheidungen treffen",
        correctAnswer = 1,
        explanation = "Mill'scher Paternalismus-Einwand: Staat soll Erwachsene nicht vor sich selbst schuetzen (Harm Principle). McNamee und andere antworten mit 'weichem Paternalismus' (soft paternalism): Wenn Bedingungen echter Autonomie fehlen (z.B. Athlet unter Karrieredruck, soziales Umfeld normalisiert Risiko), ist Intervention gerechtfertigt um 'Bedingungen echter Autonomie zu ermoglichen', nicht um Autonomie zu ersetzen.",
        difficulty = 5,
        funFact = "Die Helmpflicht im Radsport wurde erst 2003 nach dem Tod von Andrei Kivilev (Sturz ohne Helm) verpflichtend. Jahrzehntelang weigerten sich Profiradfahrer aus Traditionsg ruenden und Kuhlungswuenschen. Die Debatte: Ist Helmpflicht paternalistisch, oder schuetzt sie Athleten vor sozialem Druck, helmetlos zu fahren? Heute gilt Helmpflicht als unumstritten – ein Sieg des weichen Paternalismus."
    ),

    // Question 35 – Sports philosophy: Fair play concept origins
    Question(
        categoryId = 6,
        questionText = "Was ist der philosophische Ursprung des Konzepts 'Fair Play' in der angelsaechsischen Sporttradition des 19. Jahrhunderts, und wie unterscheidet es sich von blosser Regelkonformitaet?",
        answerA = "Fair Play entstammt der Arbeiterklassenbewegung als Protest gegen aristokratische Regelsetzung; es ist identisch mit Gesetzestreue",
        answerB = "Fair Play entstammt aristokratischem 'gentlemanship' und viktorianischer Sportethik (Thomas Arnold, Rugby School); es bedeutet nicht nur Regeleinhaltung, sondern den Geist der Regeln hochhalten, auch wenn formale Verletzung unentdeckt bliebe – virtue-basiert, nicht regelbasiert",
        answerC = "Fair Play ist ein IOC-Konzept aus 1896, das Doping verbietet und Gewaltfreiheit fordert",
        answerD = "Fair Play bezeichnet ausschliesslich gleiche Startbedingungen (Chancengleichheit) ohne ethische Komponente",
        correctAnswer = 1,
        explanation = "Thomas Arnold (Headmaster Rugby School, 1828-1842) praegte Sport als Charakterbildungsmittel: 'Play up! play up! and play the game!' (Henry Newbolt, 1897). Fair Play im viktorianischen Sinn geht ueber Regeleinhaltung hinaus: Ein Gentleman gesteht eigene Fehler ein, respektiert Gegner und Schiedsrichter auch bei Unrecht gegen sich selbst. Diese virtue-ethics-Tradition unterscheidet sich von modernem rule-following.",
        difficulty = 5,
        funFact = "Der beruehrmteste Fair-Play-Moment der Sportgeschichte: 1920 Olympia, Antwerpen, Segelrennen. Der britische Segler Briggs-Whaley hatte einen Konkurrenten von Kentern gerettet und verlor dadurch das Rennen. Er wurde trotzdem mit Fair-Play-Preis geehrt. Dieses Ethos – Sieg dem Leben opfern – verkoerpert die viktorianische Tradition."
    ),

    // Question 36 – Sports philosophy: Philosophy of doping
    Question(
        categoryId = 6,
        questionText = "Welches philosophische Argument bringt Claudio Tamburrini (2000) fuer eine Liberalisierung des Dopings im Sport vor, und welche Gegenthese formuliert McNamee zur Frage der Authentizitaet sportlicher Leistung?",
        answerA = "Tamburrini argumentiert, Doping ist Betrug; McNamee stimmt zu und fordert strengere Kontrollen",
        answerB = "Tamburrini: Dopingverbote verletzen Koerperautonomie und Selbstbestimmung erwachsener Athleten; zudem sei Technologie (Schuhe, Ernaehrung) ohnehin integraler Bestandteil moderner Athletik. McNamee kontert: Dopte Leistung ist keine authentische menschliche Leistung mehr; Sport zelebriert menschliche Natur, nicht pharmazeutische Optimierung",
        answerC = "Tamburrini vertritt religioeseArgument gegen Doping als 'Gottes Gabe verderben'; McNamee widerspricht saekular",
        answerD = "Beide Philosophen unterstuetzen Doping unter aerztlicher Aufsicht; der Dissens betrifft nur Kontrollmechanismen",
        correctAnswer = 1,
        explanation = "Tamburrinis libertaere Position: Verbote sind paternalistisch und verletzen Koerperautonomie; Athleten mit Vorwissen sollten eigenverantwortlich entscheiden duerfen. McNamees Gegenargument basiert auf Authentizitaet: Sport feiert menschliche Natur-Exzellenz (natuerliche Koerper in agonalem Kontext). Doping wuerde dieses fundamentale Gut veraendern – Athlet wuerde Ausdruck pharmazeutischer Macht, nicht menschlicher Tugend.",
        difficulty = 5,
        funFact = "Julian Savulescu (Oxford) geht noch weiter als Tamburrini: Er schlaegt vor, alle Leistungssportler sollten Doping erlaubt sein, solange es medizinisch sicher ist. Er nennt dies 'genetic enhancement argument'. Dieser Standpunkt wird zunehmend in Bioethik-Kreisen diskutiert, bleibt aber sportpolitisch weit von jeder Umsetzung entfernt."
    ),

    // Question 37 – Sports philosophy: Body in sport phenomenology
    Question(
        categoryId = 6,
        questionText = "Wie beschreibt Maurice Merleau-Ponty das Verhaeltnis von Koerper, Bewegung und Welt in 'Phaeaomenologie der Wahrnehmung' (1945), und welche Konsequenz hat dies fuer das sportphilosophische Verstehnis von motorischem Lernen?",
        answerA = "Merleau-Ponty sieht Koerper als Maschine; motorisches Lernen ist reine Reflexkonditionierung ohne Bedeutungsdimension",
        answerB = "Merleau-Ponty: Der Koerper ist 'gelebter Koerper' (corps vecu), kein Objekt-Koerper; Bewegung ist intentional und weltbezogen ('Motorisches Intentionalitaet'); motorisches Lernen im Sport ist Erwerb von Bedeutungs- und Moeglichkeitshorizonten, nicht bloss Reflexspeicherung",
        answerC = "Merleau-Pontys Phaeaomenologie lehnt Koerper-Erfahrung als philosophisch irrelevant ab; nur kognitive Prozesse zaehlen",
        answerD = "Merleau-Ponty entwickelte Phaenomenologie als Widerspruch zu Sport; er sah Sport als Entfremdung des Koerpers",
        correctAnswer = 1,
        explanation = "Merleau-Ponty unterscheidet 'Koerper-Objekt' (corps objectif – von aussen betrachtet) und 'Koerper-Subjekt' (corps propre – gelebter Leib). Bewegung ist keine Reaktionskette, sondern 'motorische Intentionalitaet': Der Koerper greift intentional zur Welt. Fuer Sport: Exzellenz bedeutet nicht, Regeln zu kennen, sondern eine neue Welt-Moeglichkeit zu verkörpern – der erfahrene Tennisspieler 'sieht' den Ball anders als der Anfaenger.",
        difficulty = 5,
        funFact = "Drew Hyland (Philosophy and Sports, 1984) wendete Merleau-Ponty auf Sport an und praegte 'stance of play' – die Art, wie ein Athlet leiblich offen zur Welt steht. Hans-Georg Gadamers Spielphilosophie (Spiel als Seinsweise) ergaenzt Merleau-Ponty: Im Spiel uebernimmt das Spiel selbst die Kontrolle – der Spieler wird gespielt."
    ),

    // Question 38 – Sports philosophy: Olympic ideals critique
    Question(
        categoryId = 6,
        questionText = "Welche fundamentale philosophische Spannung identifiziert Sigmund Loland zwischen Coubertins 'olympischen Idealen' und der realem Struktur moderner olympischer Spiele, und welche philosophische Loesung schlaegt er vor?",
        answerA = "Loland sieht keine Spannung; die olympischen Ideale werden in der modernen Olympiade perfekt verwirklicht",
        answerB = "Spannung: Olympia propagiert 'Teilnahme ist alles' und Amateurgeist, waehrend es tatsaechlich Hochleistungskommerz mit professionellen Athleten ist. Loland schlaegt 'Fair Play als regulative Idee' vor: Kein Absolutanspruch, aber ein normatives Leitbild, das Kritik an Abweichungen ermoeglicht",
        answerC = "Loland kritisiert Olympia als irrelevant fuer moderne Sport-Philosophie; er fordert Abschaffung der Spiele",
        answerD = "Die Spannung liegt nur in finanzieller Korruption; Lolands Loesung ist reine Finanzreform ohne philosophische Tiefe",
        correctAnswer = 1,
        explanation = "Coubertin proklamierte 'das Wichtigste bei den Olympischen Spielen ist nicht der Sieg, sondern die Teilnahme' – eine Idealisierung von Fair Play und Amateurismus. Moderne Olympia: Profis, massive Medienvertraege, nationale Prestigeprojekte. Loland (Fair Play in Sport, 2002) schlaegt 'regulative Ideen' vor: Ideale muessen nicht vollstaendig verwirklicht sein, um normativ wirksam zu sein – sie geben Richtung und ermoeglichten normative Kritik.",
        difficulty = 5,
        funFact = "Coubertin erfand das 'Teilnahme ist alles'-Zitat nicht selbst – er zitierte 1908 den Bishop of Pennsylvania. Der Satz hatte urspruenglich christlichen Hintergrund (Teilnahme am Gottesreich). Coubertin saekularisierte ihn zu olympischem Ethos. Ironischerweise war Coubertin selbst zutiefst elitaer und meinte mit 'Teilnahme' primaer westliche Bildungsbaerger, nicht alle Menschen."
    ),

    // Question 39 – Sports philosophy: Emotions in sport spectatorship
    Question(
        categoryId = 6,
        questionText = "Welches philosophische Problem stellt Derek Matravers fuer die Emotionen von Sportzuschauern, und wie loesen Kendall Walton oder Alex Neill es mit dem Konzept der 'quasi-emotions'?",
        answerA = "Matravers: Zuschauer koennen keine echten Emotionen haben, weil Sport fiktiv ist; Walton: Sport ist Wirklichkeit, also kein Problem",
        answerB = "Matravers identifiziert das Paradox: Zuschauer wissen, dass Ergebnisse fuer sie folgenlos sind, erleben aber intensive Emotionen wie Angst/Freude. Walton erklaert Fiktions-Emotionen als 'quasi-emotions' – funktional aequivalent zu echten Emotionen, aber ohne die kognitive Ueberzeugung eines echten Beliefs; dieses Konzept laesst sich auf Sport anwenden",
        answerC = "Das Problem existiert nicht; Zuschauer haben triviale Emotionen ohne philosophische Relevanz",
        answerD = "Matravers loest das Problem selbst: Zuschauer identifizieren sich mit Spielern und erleben stellvertretend echte Risiken",
        correctAnswer = 1,
        explanation = "Das 'Paradox of Fiction' (Kendall Walton): Warum haben wir Emotionen ueber fiktive/irrelevante Ereignisse? Waltons Antwort: 'Quasi-emotions' – wir spielen ein Spiel des make-believe, in dem unsere emotionalen Systeme reagieren, obwohl wir wissen, dass kein Segen bedroht ist. Bei Sport: Kein Zuschauer glaubt wirklich, sein Leben haenge vom Spielergebnis ab, aber emotionale Systeme reagieren als ob.",
        difficulty = 5,
        funFact = "Hooligan-Forschung zeigt: Menschen mit unsicherer Identitaet erleben Mannschaftserfolge staerker als personliche Erfolge. Joseph Maguires 'Quest for Excitement' (nach Norbert Elias) erklaert Sportzuschauen als kontrollierte Erregungssuche in durchrationa-lisierten modernen Gesellschaften – die einzig 'erlaubte' emotionale Entgrenzung."
    ),

    // Question 40 – Sports philosophy: Virtue ethics and sport
    Question(
        categoryId = 6,
        questionText = "Wie verwendet William Morgan den aristotelischen Begriff 'Eudaimonia' (Glueckseligkeit) in seiner Sportphilosophie, und in welchem Spannungsverhaeltnis steht dies zur modernen Leistungsorientierung des Hochleistungssports?",
        answerA = "Morgan lehnt Aristoteles ab; Eudaimonia ist ein antikes Konzept ohne Relevanz fuer modernen Sport",
        answerB = "Morgan: Sport als Praxis (a la MacIntyre) realisiert interne Gueter (Exzellenz, Handwerkskunst, Gemeinschaft), die genuine Eudaimonia foerdern; Spannung: Hochleistungssport korrumpiert diese internen Gueter durch externe Gueter (Geld, Ruhm), was sportliche Eudaimonia unmoeglich macht, solange externe Gueter dominieren",
        answerC = "Morgan verwendet Eudaimonia als Synonym fuer maximale Wettkampfleistung; je mehr Siege, desto mehr Eudaimonia",
        answerD = "Aristoteles' Eudaimonia und Sport sind philosophisch unvereinbar; Morgan trennt beide Sphären",
        correctAnswer = 1,
        explanation = "MacIntyre unterscheidet 'internal goods' (Exzellenz, Tugend, Gemeinschaft – nur zugaenglich durch Teilnahme an Praxis) und 'external goods' (Geld, Ruhm – von aussen zugaenglich). Aristotelische Eudaimonia resultiert aus Tugendverwirklichung in Praktiken. Morgan: Sport-als-Praxis realisiert interne Gueter und kann Eudaimonia foerdern. Aber Profisport korrumpiert Praxis durch externe Gueter-Dominanz: Athleten spielen fuer Geld, nicht fuer Exzellenz.",
        difficulty = 5,
        funFact = "Aristoteles selbst erwaehnte sportliche Wettkaeampfe mehrfach in der Nikomachischen Ethik als Beispiele fuer Exzellenz (Arete). Die Olympic Games Athens 776 BC – die Aristokraten, die gewannen, gewannen Ehre (Time) als externem Gut, aber auch Arete als intern. Die Spannung zwischen Ehre und Tugend ist also so alt wie die Olympischen Spiele selbst."
    ),

    // ── Sportkybernetik / Systemtheorie (10) ─────────────────────────────────

    // Question 41 – Sport cybernetics: Feedback loops in motor control
    Question(
        categoryId = 6,
        questionText = "Welches kybernetische Modell beschreibt Jack Adams' 'closed-loop theory of motor learning' (1971), und welche Fehlerkorrekturmechanismus steht im Zentrum?",
        answerA = "Adams beschreibt offene Regelkreise ohne Sensorrueckkopplung; Fehlerkorrektur erfolgt ausschliesslich vor Bewegungsbeginn",
        answerB = "Adams' Closed-Loop-Theorie: Sensorische Rueckkopplung (propriozeptiv, visuell) wird mit einer gespeicherten Referenz ('perceptual trace') verglichen; Abweichungen generieren Fehlersignale, die Bewegung online korrigieren – Grundlage fuer servomechanisches Lernen",
        answerC = "Adams entwickelte Schema-Theorie mit Generalisierung ueber Bewegungsklassen ohne gespeicherte spezifische Referenzen",
        answerD = "Adams' Theorie ist rein kognitionspsychologisch; Kybernetik-Konzepte sind irrelevant fuer motorisches Lernen",
        correctAnswer = 1,
        explanation = "Adams (1971) unterschied 'memory trace' (programmiert Bewegungsbeginn) und 'perceptual trace' (sensorische Soll-Referenz fuer laufende Korrektur). Sensorisches Feedback wird gegen perceptual trace verglichen; Diskrepanz erzeugt Korrekturimpuls. Diese Servomechanismus-Analogie war direkter Transfer aus Norbert Wieners Kybernetik auf Motorik-Theorie.",
        difficulty = 5,
        funFact = "Richard Schmidt kritisierte Adams 1975 mit seiner Schema-Theorie: Wir speichern nicht jede Bewegung als spezifische Referenz, sondern generalisierte Schemas (Regelwerke). Heute gilt ein integriertes Modell: Schnelle Bewegungen (ballistische Movts) sind open-loop, langsame mit online-Feedback closed-loop. Die Adams/Schmidt-Debatte strukturierte die Motorik-Psychologie der 1970er-90er Jahre."
    ),

    // Question 42 – Sport cybernetics: Dynamic systems theory in sport
    Question(
        categoryId = 6,
        questionText = "Was erklaert die Dynamische Systemtheorie (Kelso, Thelen) am Beispiel von Schrittfrequenz-Gaituebergaengen (Walk zu Trab), und welche Konsequenz hat dies fuer das sportliche Training der Koordination?",
        answerA = "Gait-Uebergaenge sind programmierte Reaktionen auf bewusste Befehle; Training muss explizites Wissen ueber Gangmuster foerdern",
        answerB = "Kelsos Dynamische Systemtheorie: Gangmuster sind Attraktoren in Phasenraeumen; Gait-Uebergaenge entstehen als Phasenuebergaenge wenn Kontrollparameter (Geschwindigkeit) Stabilitaetsschwellen kreuzen; Training optimiert Attractoren-Stabilität und Flexibilitaet zwischen Basins",
        answerC = "Dynamische Systemtheorie leugnet motorisches Lernen; Gangmuster sind angeboren und unveraenderlich",
        answerD = "Schrittfrequenz-Uebergaenge werden durch zentrales Muster-Programm im Kleinhirn gesteuert; externe Dynamik ist irrelevant",
        correctAnswer = 1,
        explanation = "Kelso (1984) und Thelen zeigten: Gait-Uebergaenge Wandeln sich abrupt wenn metabolische Kosten eines Gangs einen Schwellenwert ueberschreiten – selbst ohne bewusste Entscheidung. Im Phasenraummodell: Jeder Gangmodus ist ein Attraktor; schnellere Geschwindigkeit destabilisiert den Walk-Attraktor, bis das System in den Trab-Attraktor springt. Sport-Konsequenz: Techniktraining = Neue Attraktoren etablieren, bestehende modifizieren.",
        difficulty = 5,
        funFact = "Kelso's HKB-Modell (Haken-Kelso-Bunz, 1985) beschreibt Fingerkopplung als nichtlineares dynamisches System mit Bifurkationen. Dieses Modell erklaerte erstmals mathematisch, warum bestimmte Bimanual-Koordinationsmuster stabil sind und andere nicht – ein Grundstein der Sportkoordinationsforschung."
    ),

    // Question 43 – Sport cybernetics: Perception-action coupling
    Question(
        categoryId = 6,
        questionText = "James Gibsons 'Ecological Approach to Visual Perception' (1979) und das Konzept der 'Affordanzen' (affordances) hat direkte Implikationen fuer Sporttraining. Was versteht Gibson unter Affordanz und wie erklaert es das Expertinnenwahrnehmung im Sport?",
        answerA = "Affordanzen sind kognitive Repräsentationen von Handlungsmoeglichkeiten; Expertinnen haben mehr Repraesentationen gespeichert",
        answerB = "Affordanzen sind direkt wahrnehmbare Handlungsmoeglichkeiten der Umwelt (nicht repraesentiert, sondern wahrgenommen); Expertinnen 'sehen' mehr/andere Affordanzen im Spielfeld als Anfaenger – Wahrnehmung und Handlung sind direkt gekoppelt ohne kognitive Vermittlung",
        answerC = "Affordanzen beschreiben physikalische Gegebenheiten der Umwelt; Expertise veraendert Wahrnehmung nicht, nur Reaktionsgeschwindigkeit",
        answerD = "Gibson lehnte den Affordanz-Begriff spaeter selbst ab; das Konzept ist fuer Sport nicht anwendbar",
        correctAnswer = 1,
        explanation = "Gibson: Affordanzen sind relationale Eigenschaften zwischen Umwelt und Organismus – der Boden 'affordet' Gehen fuer den Menschen, nicht fuer den Fisch. Ein Basketballkorb 'affordet' Wurf fuer erfahrene Spielerinnen auf andere Weise als fuer Anf. Expert attackers see gaps-to-run-through, not individual defenders. Trainingsprinzip: Realitaetnahe Uebungsumgebungen (Representative Learning Design, Pinder) um genuine Affordanz-Wahrnehmung zu trainieren.",
        difficulty = 5,
        funFact = "Pep Guardiolas Trainingsansatz – kleinflächige, komplexe Spielsituationen ('Rondos') statt isolierter Techniken – ist unbewusst gibsonianisch: Er trainiert Wahrnehmung-Handlungs-Kopplung in echten Spielaffordanz-Strukturen, nicht dekontextualisierte Techniken. Die Ecological Dynamics Bewegung (Rob Gray, Duarte Araujo) macht dies explizit wissenschaftlich."
    ),

    // Question 44 – Sport cybernetics: Constraints-led approach
    Question(
        categoryId = 6,
        questionText = "Was versteht Newell (1986) unter dem 'Constraints-led Approach' zur Motorikentwicklung, und welche drei Constraint-Kategorien strukturieren seine Theorie?",
        answerA = "Constraints sind Verbote und Einschraenkungen; Motorik entsteht trotz, nicht durch Constraints; Newells Modell ist behavioristisch",
        answerB = "Constraints sind gestaltgebende Grenzen; drei Kategorien: (1) Organismus-Constraints (Koerpergroesse, Kraft), (2) Aufgaben-Constraints (Regelwerk, Geraet), (3) Umwelt-Constraints (Boden, Luft, Gegner); Motorische Loesung entsteht als Selbstorganisation im Schnittpunkt aller drei",
        answerC = "Newell beschreibt nur biologische Constraints; Umwelt und Aufgabe sind irrelevant fuer Motorikentwicklung",
        answerD = "Constraints-led Approach ist identisch mit behavioristischem Konditionieren; nur externe Reize formen Bewegung",
        correctAnswer = 1,
        explanation = "Newells (1986) Constraint-Model ist kybernetische Systemtheorie angewandt auf Motorik: Bewegungsloesungen emergieren nicht durch Programmierung, sondern durch Selbstorganisation unter dem Einfluss aller drei Constraint-Typen. Fuer Coaching: Modifiziere Aufgaben-Constraints (z.B. kleinere Netzhoeahe beim Tennis fuer Nachwuchs) um optimale Motorikentwicklung zu foerdern.",
        difficulty = 5,
        funFact = "Kleinkinder wechseln von Krabbeln zu Gehen nicht durch Entscheidung, sondern durch veraenderte Organismus-Constraints (Beinlaenge, Muskelkraft) und Umwelt-Constraints (Bodenoberflaeche). Newell erklaerte: Entwicklung IST Constraints-Veraenderung. Dies erklaert, warum Kinder-Ausruestung (kleinere Baelle, niedrigere Koerbe) wichtig ist – falsche Constraints behindern natuuerliche motorische Loesungen."
    ),

    // Question 45 – Sport cybernetics: Chaos theory in sports
    Question(
        categoryId = 6,
        questionText = "Welche Konsequenz hat die Chaostheorie (sensitive Abhaengigkeit von Anfangsbedingungen, Butterfly-Effekt) fuer die Vorhersagbarkeit von Spielergebnissen und sportliche Trainingsplanung?",
        answerA = "Chaostheorie beweist, dass Sportspiele rein zufallig sind; Training ist sinnlos",
        answerB = "Komplexe Sportsysteme (Mannschaftssport) weisen chaotische Eigenschaften auf: Kleine Anfangsbedingungen (eine fruehe Verletzung, Windrichtung) koennen disproportionale Ergebnisveraenderungen bewirken; Training muss Robustheit und Anpassungsfaehigkeit foerdern, nicht optimale Startbedingungen maximieren",
        answerC = "Chaostheorie ist nur relevant fuer Wettervorhersage; Sport ist ein determiniertes System ohne Chaos",
        answerD = "Chaostheorie belegt, dass gute Teams immer gewinnen weil sie Anfangsbedingungen kontrollieren",
        correctAnswer = 1,
        explanation = "Mannschaftssportsysteme sind komplexe nichtlineare Systeme mit Charakteristika deterministischen Chaos: Geringe Sensitivitaet auf Initialkonditionen kann Ergebnisse drastisch veraendern. Duarte Araujo und Keith Davids (2016) zeigten: Sportsysteme operieren 'am Rand des Chaos' (edge of chaos) – genug Ordnung fuer Koordination, genug Chaos fuer kreative Problemloesung. Training: Robustheit unter variablen Bedingungen foerdern.",
        difficulty = 5,
        funFact = "In der Fussball-Bundesliga gewinnt das bessere Team laut Simulationen nur ca. 70% der Zeit – weit unter 100%, obwohl Leistungsunterschiede real sind. Chaotische Sensitivitaet erklaert Upset-Ergebnisse. Der sogenannte 'Lotteri-Effekt' im Pokal-Sport ist chaostheoretisch erklaerbar: Kurzere Spielserien verstaerken chaotische Varianz gegenueber saisonalen Langzeit-Ergebnissen."
    ),

    // Question 46 – Sport cybernetics: Game theory in competitive sport
    Question(
        categoryId = 6,
        questionText = "Wie erklaert Spieltheorie das Phaenomen der 'Mixed Strategy Equilibria' im Tennis (beim Aufschlag) und was bestaetigen empirische Daten dazu?",
        answerA = "Tennis-Aufschlag ist rein habitsbezogen; Spieltheorie ist auf Tennis nicht anwendbar",
        answerB = "Nash-Gleichgewicht bei Mixed Strategies: Aufschlaeger muss Richtungswahl (links/rechts/Mitte) unvorhersehbar mischen damit Empfaenger keinen strategischen Vorteil hat; Walker und Wooders (2001) analysierten Wimbledon-Daten und fanden: Profi-Spieler naehern sich tatsaechlich Nash-Gleichgewicht in Aufschlag-Richtung und Erwiderung",
        answerC = "Spieltheorie sagt, dass Aufschlaeger immer dieselbe starke Seite praeaferieren muessen; Randomisierung ist suboptimal",
        answerD = "Mixed Strategy Equilibria sind nur in Laborexperimenten beobachtbar; reale Sportler weichen systematisch davon ab",
        correctAnswer = 1,
        explanation = "Von Neumann/Nash Mixed Strategy: In Nullsummenspielen mit zwei Spielern und unvollstaendiger Information ist das Gleichgewicht eine Randomisierung, die den Gegner indifferent macht. Walker und Wooders (2001) prueften 2946 Punkte aus Wimbledon und fanden: Spieler weichen nicht signifikant von optimaler Randomisierung ab – eine bemerkenswerte Bestaetigung rationaler Spieltheorie im realen Hochleistungssport.",
        difficulty = 5,
        funFact = "Fussball-Elfmeter wurden aehnlich analysiert: Palacios-Huerta (2003) fand, dass Profi-Schiesser und -Torhueter sich nahe am Nash-Gleichgewicht befinden. Er schlug dies Jose Mourinho vor Chelsea vor Halbfinale gegen Liverpool 2005 vor. Mourinhos Spieler befolgten die Empfehlung – und gewannen die Elfmeter-Entscheidung. Spieltheorie in live eingesetzt."
    ),

    // Question 47 – Sport cybernetics: Variability in motor performance
    Question(
        categoryId = 6,
        questionText = "Warum ist Variabilitaet in motorischen Leistungen (z.B. Schrittvariabilitaet beim Laufen) nach moderner Systemtheorie KEIN Defizit, sondern ein Index von motorischer Kompetenz?",
        answerA = "Variabilitaet ist immer ein Defizit; perfekte Reproduzierbarkeit ist das Ziel hochwertigen Trainings",
        answerB = "Nikolai Bernstein und dynamische Systemtheoretiker (Meeuwsen, Davids): Motorische Variabilitaet reflektiert das Explorationspotenzial eines Systems; funktionale Variabilitaet zeigt, dass das System Loesungsraum durchsucht; zu wenig Variabilitaet (Stereotypie) bedeutet Verlust adaptiver Kapazitaet",
        answerC = "Variabilitaet ist ausschliesslich Rauschen und hat keinen funktionalen Wert; Computermodelle beweisen, dass Reproduzierbarkeit optimal ist",
        answerD = "Variabilitaet ist nur bei Freizeit-Sport wichtig; Elite-Athleten sollten maximal konsistent sein",
        correctAnswer = 1,
        explanation = "Nikolai Bernsteins 'repetition without repetition': Exzellente Athleten erzielen konsistente Ergebnisse durch variable Ausfuehrungswege, nicht durch identische Bewegungsreplikation. Bernstein (1967): 'The Coordination and Regulation of Movement'. Moderne Anschlussforscher (Hamill, Ferber) zeigten: Laeaufer mit monotoner Schrittvariabilitaet haben hoehere Verletzungsraten. Variabilitaet im Normalbereich = gesundes exploratives System.",
        difficulty = 5,
        funFact = "Bernsteins Arbeit entstand in der Sowjetunion und wurde im Westen erst in den 1960ern bekannt. Er identifizierte das 'Degrees-of-Freedom-Problem': Wie kontrolliert das Gehirn die unendlichen Freiheitsgrade des Koerpers? Seine Antwort – Synergien und funktionale Variabilitaet – revolutionierte Motorikwissenschaft und wird in modernen Exoskeletten und Robotik umgesetzt."
    ),

    // Question 48 – Sport cybernetics: Information theory in sport
    Question(
        categoryId = 6,
        questionText = "Welche Aussage macht Fitts' Gesetz (1954) ueber die Relation von Bewegungsgeschwindigkeit und -praezision, und in welchen Sportsituationen ist es direkt relevant?",
        answerA = "Fitts' Gesetz: Schnellere Bewegungen sind immer praeziser weil Inertialkraefte Zielabweichungen reduzieren",
        answerB = "Fitts' Gesetz (ID = log2(2A/W)): Bewegungszeit waechst logarithmisch mit Amplitude und sinkt mit Zielbreite; klassischer Speed-Accuracy-Tradeoff; relevant bei Tennis-Volley (grosses Ziel = schnell), Bogen (kleines Ziel = langsam, praezise), Chirurgie und feine Motorik",
        answerC = "Fitts' Gesetz gilt nur fuer Zeigerbewegungen am Computer; Sportbewegungen folgen anderen Gesetzen",
        answerD = "Fitts' Gesetz berechnet maximale Herzfrequenz aus Bewegungsamplitude; es ist ein physiologisches, kein kybernetisches Gesetz",
        correctAnswer = 1,
        explanation = "Fitts (1954) formalisierte informationstheoretisch: Bewegungszeit (MT) = a + b * log2(2A/W), wobei A=Amplitude, W=Zielbreite, Informationsgehalt ID in Bits. Je kleiner das Ziel oder groesser die Distanz, desto mehr 'Information' muss die Bewegung kodieren, desto laenger dauert sie. Im Sport: Dartpfeil-Werfen, Fussabdruck im Turnen, Schlagpraezision im Cricket – Speed-Accuracy-Tradeoff ist allgegenwärtig.",
        difficulty = 5,
        funFact = "Fitts' Gesetz wurde zuerst formuliert um menschliche Informatiosnverarbeitungskapazitaet zu messen – er nannte es 'channel capacity' analog zu Shannon's Informationstheorie. Heute gilt es als das robusteste Gesetz der menschlichen Motorik: Es wurde in Bogenschiessen, Chirurgie, Videospiele und Computermaussteuerung repliziert."
    ),

    // Question 49 – Sport cybernetics: Anticipation and pattern recognition
    Question(
        categoryId = 6,
        questionText = "Wie unterscheidet sich Antizipation von Reaktion bei Hochleistungssportlern, und welche Forschungsmethode hat Bruce Abernethy verwendet um Antizipationsvorteile bei Experten zu quantifizieren?",
        answerA = "Antizipation und Reaktion sind identisch; Expertinnen haben lediglich schnellere neuronale Verarbeitung",
        answerB = "Antizipation nutzt fruehzeitige Informationsextraktion (Koerperhaltung, Racketwinkel) bevor das kritische Reizereignis (Ballkontakt) eintritt; Abernethy's Occlusion-Paradigma: Videosequenzen werden an definierten Zeitpunkten vor Kontakt abgeschnitten; Experten zeigen signifikant bessere Vorwegnahme als Anfaenger bei gleichen Reaktionszeiten",
        answerC = "Antizipation ist ausschliesslich eine kognitive Faehigkeit; Training kann sie nicht verbessern",
        answerD = "Abernethy zeigte, dass Blickbewegungen irrelevant fuer Antizipation sind; Gelenkinformation ist der einzige Praediktor",
        correctAnswer = 1,
        explanation = "Abernethy's Occlusion-Studien (Squash, Tennis, Kricket) zeigten: Experten extrahieren entscheidende Informationen aus proximal vorgelagerten Bewegungsmerkmalen (Schulter, Hueften) bis zu 200-300ms vor Ballkontakt. Anfaenger warten auf distale Merkmale (Racket, Ball). Dies erklaert nicht kuerzere Reaktionszeiten bei Experten, sondern frueheren Entscheidungsbeginn – effektiv laengere Verarbeitungszeit.",
        difficulty = 5,
        funFact = "Ausgeruestes-Torhüter-Studien (Poulter et al.) zeigten: Top-Torhueter beginnen Elfmeter-Antizipation anhand von Becken- und Koerperrotation des Schiesers – 150ms vor Ballkontakt. Training fuer Antizipation: Video-Occlusion-Sessions verbessern Antizipationsleistung messbar bei Jugendtorhueter bereits nach 8 Wochen."
    ),

    // Question 50 – Sport cybernetics: Ecological dynamics and representative design
    Question(
        categoryId = 6,
        questionText = "Was versteht Rob Gray unter 'Representative Learning Design' und welche fundamentale Kritik ubt er an traditionellem, isoliertem Techniktraining aus systemtheoretischer Perspektive?",
        answerA = "Representative Learning Design bedeutet, bekannte Techniken durch erfahrene Coaches repraesentativ vorzuzeigen; Gray kritisiert digitale Technologien im Training",
        answerB = "Representative Learning Design (RLD) fordert, dass Trainingsaufgaben die fuer Wettkampf entscheidenden Wahrnehmungs-Handlungs-Kopplungen repraesentieren; Kritik: Isoliertes Techniktraining dekontextualisiert Bewegung, sodass im Wettkampf gelernte Techniken nicht transferieren, weil die Affordanzstruktur fehlt",
        answerC = "RLD ist identisch mit spielnahem Training; Gray kritisiert ausschliesslich Krafttraining als unsportspezifisch",
        answerD = "Rob Gray lehnt alle technologischen Hilfsmittel im Training ab; RLD bedeutet natuerliches Lernen ohne Coaching",
        correctAnswer = 1,
        explanation = "Grays RLD (basierend auf Brunswiks 'representative design' aus Wahrnehmungspsychologie) fordert: Uebungsaufgaben muessen die oekologischen Informationsstrukturen des Wettkampfs enthalten. Isoliertes Dribbling ohne Verteidiger trainiert Bewegungsablauf, aber nicht die Wahrnehmungs-Entscheidungs-Kopplung, die im Spiel relevant ist. Konsequenz: Small-Sided Games sind oekologisch repraesentativer als isolierte Technikuebungen.",
        difficulty = 5,
        funFact = "Neymar wurde von Wissenschaftlern der Universitaet Barcelona getestet: Sein Gehirn aktiviert bei Ballkontroll-Entscheidungen fast keine bewussten kortikalen Areale – Antworten kommen direkt aus automatisierten sensomotorischen Schleifen. Dies ist das neurophysiologische Korrelat von 'expert intuition' und bestaetigt: Jahrelange variationsreiche Praxis in repraesentativen Kontexten baut echte motorische Expertise auf."
    )
)

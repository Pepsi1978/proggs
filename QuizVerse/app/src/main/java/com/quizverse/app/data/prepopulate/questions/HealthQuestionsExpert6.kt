package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun healthQuestionsExpert6(): List<Question> = listOf(

    // Question 1 -- Study Designs: Cohort Study
    Question(
        categoryId = 16,
        questionText = "Was ist das entscheidende Merkmal einer prospektiven Kohortenstudie gegenueber einer Fall-Kontroll-Studie?",
        answerA = "In der Kohortenstudie werden nur Personen mit der Erkrankung eingeschlossen",
        answerB = "Die Exposition wird vor dem Auftreten des Endpunkts gemessen, sodass eine zeitliche Kausalitaetsrichtung hergestellt werden kann",
        answerC = "Kohortenstudien sind immer randomisiert und kontrolliert",
        answerD = "Kohortenstudien messen nur genetische Risikofaktoren",
        correctAnswer = 1,
        explanation = "In prospektiven Kohortenstudien werden Personen ohne den Endpunkt zu Beginn nach Expositionsstatus gruppiert und ueber die Zeit beobachtet -- die zeitliche Reihenfolge (Exposition vor Erkrankung) ist damit klar definiert. Fall-Kontroll-Studien beginnen dagegen beim Endpunkt und schauen rueckwirkend auf die Exposition.",
        difficulty = 4,
        funFact = "Die Framingham Heart Study (1948) ist die beruehrmteste prospektive Kohortenstudie -- sie folgt Bewohnern einer US-Kleinstadt ueber Generationen und hat massgeblich die kardiovaskulaeren Risikofaktoren wie Hypertonie und Hypercholesterinaemie identifiziert."
    ),

    // Question 2 -- Study Designs: Case-Control Study
    Question(
        categoryId = 16,
        questionText = "Welches Mass fuer die Staerke des Zusammenhangs wird in einer Fall-Kontroll-Studie direkt berechnet und warum nicht das relative Risiko?",
        answerA = "Das relative Risiko, weil die Erkrankungsrate in der Gesamtbevoelkerung bekannt ist",
        answerB = "Die Odds Ratio, weil die Inzidenzrate nicht ermittelt werden kann -- man startet mit Faellen und Kontrollen, nicht mit einer Kohorte",
        answerC = "Der Attributable Risk Percent, weil nur Exponierte eingeschlossen werden",
        answerD = "Der Number Needed to Harm, weil Fall-Kontroll-Studien immer Interventionen untersuchen",
        correctAnswer = 1,
        explanation = "In Fall-Kontroll-Studien werden Faelle und Kontrollen gezielt ausgewaehlt; man kennt nicht die wahre Inzidenz in der Bevoelkerung, daher kann kein absolutes Risiko berechnet werden. Die Odds Ratio schaetzt das relative Risiko an -- bei seltenen Erkrankungen ist die Odds Ratio eine gute Naeherung des relativen Risikos.",
        difficulty = 4,
        funFact = "Sir Richard Doll und Austin Bradford Hill nutzten 1950 eine Fall-Kontroll-Studie, um den Zusammenhang zwischen Rauchen und Lungenkrebs zu zeigen -- einer der wichtigsten epidemiologischen Meilensteine des 20. Jahrhunderts."
    ),

    // Question 3 -- Study Designs: RCT
    Question(
        categoryId = 16,
        questionText = "Was ist der primaere Vorteil der Randomisierung in einem RCT gegenueber Beobachtungsstudien?",
        answerA = "Randomisierung eliminiert Messfehler bei den Ergebnissen",
        answerB = "Randomisierung stellt sicher, dass bekannte und unbekannte Confounder gleichmaessig auf die Studienarme verteilt werden",
        answerC = "Randomisierung erhoehnt die externe Validitaet durch repraesentative Stichproben",
        answerD = "Randomisierung verhindert Dropout der Teilnehmer",
        correctAnswer = 1,
        explanation = "Durch zufaellige Zuteilung werden sowohl bekannte als auch unbekannte Confounder gleichmaessig zwischen Behandlungs- und Kontrollgruppe verteilt. Dies ist der entscheidende methodische Vorteil, der kausale Rueckschluesse ermoeglicht -- ein Merkmal, das Beobachtungsstudien grundsaetzlich fehlt.",
        difficulty = 4,
        funFact = "Das erste grosse klinische RCT der Geschichte war die Streptomycin-Tuberkulose-Studie der britischen Medical Research Council (1948) -- geleitet von Austin Bradford Hill, der auch die nach ihm benannten Kausalitaetskriterien entwickelte."
    ),

    // Question 4 -- Study Designs: Cross-Sectional
    Question(
        categoryId = 16,
        questionText = "Welche wesentliche Limitierung hat eine Querschnittsstudie bei der Untersuchung von Krankheitsursachen?",
        answerA = "Sie kann nur genetische Faktoren, nicht Umweltfaktoren untersuchen",
        answerB = "Sie erfasst Exposition und Erkrankung gleichzeitig -- kausale Richtung (Huhn oder Ei) bleibt unklar",
        answerC = "Sie benoetigt eine Kontrollgruppe, die schwer zu definieren ist",
        answerD = "Sie ist nur fuer Infektionskrankheiten geeignet",
        correctAnswer = 1,
        explanation = "Querschnittsstudien erheben Daten zu einem einzigen Zeitpunkt; damit ist nicht feststellbar, ob die Exposition der Erkrankung vorausging oder umgekehrt. Dies macht kausale Interpretationen methodisch schwierig -- es koennen lediglich Assoziationen beschrieben werden.",
        difficulty = 4,
        funFact = "Querschnittsstudien sind jedoch ideal fuer die Praevalenzschaetzung -- zum Beispiel erhebt die WHO-Studie STEPS regelmaessig Praevalenzdaten zu chronischen Krankheiten und Risikofaktoren in Laendern mit niedrigem und mittlerem Einkommen."
    ),

    // Question 5 -- Bias: Selection Bias
    Question(
        categoryId = 16,
        questionText = "Was beschreibt der 'Healthy Worker Effect' als Form des Selektionsbias?",
        answerA = "Berufstaetige Probanden verweigern haeufiger die Studienteilnahme als Rentner",
        answerB = "Berufstaetige Populationen sind im Durchschnitt gesuender als die Allgemeinbevoelkerung, weil schwer Kranke seltener erwerbstaetig sind -- Vergleiche mit der Allgemeinbevoelkerung unterschaetzen daher Berufsrisiken",
        answerC = "Gesunde Mitarbeiter werden bevorzugt in expositionsorientierte Studiengruppen eingeteilt",
        answerD = "Arbeitgeber waehlen selektiv gesunde Mitarbeiter fuer Berufsgesundheitsstudien aus",
        correctAnswer = 1,
        explanation = "Der Healthy Worker Effect entsteht, weil nur arbeitsfaehige -- also vergleichsweise gesunde -- Personen in Beschaeftigungsstudien erfasst werden. Schwer Kranke scheiden aus dem Arbeitsmarkt aus. Vergleicht man die Mortalitaet Berufstaetiger mit der Allgemeinbevoelkerung, erscheinen Berufsgruppen faelschlicherweise gesuender.",
        difficulty = 4,
        funFact = "Der Healthy Worker Effect fuehrt in der Arbeitsmedizin systematisch zur Unterschaetzung von Berufserkrankungen -- eine wichtige Erkenntnis bei der Interpretation von Studien zu Berufsrisiken wie Asbestexposition."
    ),

    // Question 6 -- Bias: Recall Bias
    Question(
        categoryId = 16,
        questionText = "Warum sind Fall-Kontroll-Studien besonders anfaellig fuer Recall Bias?",
        answerA = "Weil Faelle juenger sind als Kontrollen und sich weniger gut erinnern",
        answerB = "Weil Erkrankte (Faelle) ihre vergangene Exposition intensiver durchdenken und haeufiger berichten als gesunde Kontrollen, was zu einer Verzerrung des Assoziationsmasses fuehrt",
        answerC = "Weil die Datenerhebung immer telefonisch erfolgt und Faelle besser erreichbar sind",
        answerD = "Weil Kontrollen generell weniger kooperativ bei der Studienteilnahme sind",
        correctAnswer = 1,
        explanation = "Erkrankte neigen dazu, ihre Vorgeschichte intensiver zu reflektieren (Rumination), um eine Erklaerung fuer ihre Krankheit zu finden. Dies fuehrt dazu, dass Faelle Expositionen haeufiger und detaillierter berichten als Kontrollen -- unabhaengig vom wahren Expositionsstatus. Das verzerrt die Odds Ratio in Richtung eines staerkeren Zusammenhangs.",
        difficulty = 4,
        funFact = "Recall Bias ist besonders relevant bei Studien zu Schwangerschaftskomplikationen: Muetter von Kindern mit Fehlbildungen erinnern sich signifikant haeufiger an Medikamenteneinnahmen im ersten Trimester als Muetter gesunder Kinder."
    ),

    // Question 7 -- Bias: Observer Bias
    Question(
        categoryId = 16,
        questionText = "Was ist der Unterschied zwischen einfacher Verblindung und doppelter Verblindung in einem RCT bezueglich des Observer Bias?",
        answerA = "Einfache Verblindung verblendet nur die Labortechniker, doppelte auch den Biostatistiker",
        answerB = "Einfache Verblindung verblendet nur Teilnehmer -- Observer Bias beim Untersucher bleibt moeglich. Doppelte Verblindung blendet sowohl Teilnehmer als auch Untersucher, was Observer Bias und Performance Bias gleichzeitig kontrolliert",
        answerC = "Doppelte Verblindung bedeutet, dass zwei unabhaengige Untersucher alle Messungen durchfuehren",
        answerD = "Bei doppelter Verblindung sind auch die Daten anonymisiert und koennen nicht rueckverfolgt werden",
        correctAnswer = 1,
        explanation = "Bei einfacher Verblindung kennt der Untersucher die Gruppenzugehoerigkeit und kann (bewusst oder unbewusst) Messungen oder Beurteilungen systematisch verzerren (Observer Bias). Doppelblindung verhindert dies, da weder Patient noch Untersucher die Zuteilung kennen -- beides sind separate Quellen verzerrter Beurteilung.",
        difficulty = 4,
        funFact = "In Studien zu subjektiven Endpunkten wie Schmerz oder Depression kann allein die Erwartungshaltung des Untersuchers die gemessenen Ergebnisse um 20-30% verfaelschen -- ein Grund, warum Placebo-kontrollierte Doppelblindstudien als Gold-Standard gelten."
    ),

    // Question 8 -- Bias: Publication Bias
    Question(
        categoryId = 16,
        questionText = "Wie wird Publication Bias in einem Funnel Plot erkannt?",
        answerA = "Durch einen perfekt symmetrischen Trichter, der auf Heterogenitaet hinweist",
        answerB = "Durch eine asymmetrische Verteilung -- kleine Studien mit negativen Ergebnissen fehlen auf einer Seite des Trichters, weil sie seltener publiziert werden",
        answerC = "Durch den Ausschluss grosser Studien mit hoher Praezision aus der Analyse",
        answerD = "Durch eine bimodale Verteilung der Effektgrossen in kleinen Studien",
        correctAnswer = 1,
        explanation = "Im Funnel Plot werden Effektgroessen (x-Achse) gegen ein Praezisionsmass wie Standardfehler (y-Achse) aufgetragen. Ohne Publication Bias entsteht ein symmetrischer Trichter. Fehlen kleine Studien mit negativen Ergebnissen (untere linke Ecke), deutet die Asymmetrie auf Publication Bias hin.",
        difficulty = 4,
        funFact = "Publication Bias ist so weitverbreitet, dass Meta-Analysen positive Effekte systematisch ueberschaetzen -- Studien mit positiven Ergebnissen werden etwa dreimal haeufiger publiziert als solche mit negativen Ergebnissen (Johansson et al., 2018)."
    ),

    // Question 9 -- Statistical Concepts: p-Value
    Question(
        categoryId = 16,
        questionText = "Was ist die korrekte Interpretation eines p-Werts von 0,03 in einer klinischen Studie?",
        answerA = "Die Wahrscheinlichkeit, dass die Alternativhypothese wahr ist, betraegt 97%",
        answerB = "Wenn die Nullhypothese wahr waere, betraegt die Wahrscheinlichkeit, ein Ergebnis so extrem wie das beobachtete (oder extremer) zu erhalten, 3%",
        answerC = "Die Wahrscheinlichkeit eines Fehlers 1. Art betraegt genau 3%",
        answerD = "Der beobachtete Effekt ist klinisch bedeutsam mit einer Sicherheit von 97%",
        correctAnswer = 1,
        explanation = "Der p-Wert ist die Wahrscheinlichkeit, das beobachtete (oder ein noch extremeres) Ergebnis zu erhalten, wenn die Nullhypothese wahr ist. Er sagt nichts ueber die klinische Relevanz des Effekts aus und auch nichts ueber die Wahrscheinlichkeit, dass die Nullhypothese falsch ist.",
        difficulty = 4,
        funFact = "Das 'p < 0,05'-Kriterium wurde von Ronald Fisher arbitraer als Konvention eingefuehrt -- er selbst schrieb 1926, ein Ergebnis sei 'significant' bei dieser Schwelle, aber empfahl nie, es als absolute Entscheidungsgrenze zu nutzen."
    ),

    // Question 10 -- Statistical Concepts: Confidence Interval
    Question(
        categoryId = 16,
        questionText = "Was bedeutet ein 95%-Konfidenzintervall von 1,2 bis 3,4 fuer ein relatives Risiko?",
        answerA = "Mit 95% Wahrscheinlichkeit liegt der wahre Wert des relativen Risikos zwischen 1,2 und 3,4",
        answerB = "Wuerde man das Experiment unendlich oft wiederholen, wuerden 95% der berechneten Konfidenzintervalle den wahren Parameterwert enthalten -- das Ergebnis ist statistisch signifikant, da das Intervall 1,0 nicht einschliesst",
        answerC = "95% der Studienpopulation hat ein relatives Risiko zwischen 1,2 und 3,4",
        answerD = "Das Konfidenzintervall zeigt, dass der Effekt in 95% der Subgruppenanalysen reproduzierbar ist",
        correctAnswer = 1,
        explanation = "Ein 95%-KI bedeutet frequentistisch: Bei wiederholter Anwendung der Methode wuerden 95% der berechneten Intervalle den wahren Parameterwert einschliessen. Da das Intervall (1,2; 3,4) die 1,0 nicht enthaelt, ist das relative Risiko statistisch signifikant von 1,0 verschieden.",
        difficulty = 4,
        funFact = "Ein breites Konfidenzintervall (z.B. 0,5 bis 8,0) zeigt eine grosse Unsicherheit der Schaetzung an und laesst kaum klinische Schluesse zu -- selbst wenn der Punktschaetzer imposant wirkt. Kliniker sollten immer das Intervall, nicht nur den Punktschaetzer betrachten."
    ),

    // Question 11 -- Statistical Concepts: NNT
    Question(
        categoryId = 16,
        questionText = "Wie wird der Number Needed to Treat (NNT) berechnet und wie ist er zu interpretieren?",
        answerA = "NNT = 1 / relatives Risiko; er gibt an, wie viel mehr Patienten behandelt werden muessen als in der Kontrolle",
        answerB = "NNT = 1 / absolute Risikoreduktion (ARR); er gibt an, wie viele Patienten behandelt werden muessen, um ein Ereignis zu verhindern",
        answerC = "NNT = relatives Risiko x 100; er beschreibt den prozentualen Nutzen der Behandlung",
        answerD = "NNT = 1 / Odds Ratio; er ist nur bei Fall-Kontroll-Studien anwendbar",
        correctAnswer = 1,
        explanation = "ARR = Ereignisrate Kontrolle minus Ereignisrate Behandlung. NNT = 1/ARR. Ein NNT von 10 bedeutet: 10 Patienten muessen behandelt werden, damit einem Patienten ein Ereignis erspart wird. Kleinere NNT-Werte zeigen groesseren Nutzen an.",
        difficulty = 4,
        funFact = "Der NNT fuer Statine zur primaeren Praevention kardiovaskulaerer Ereignisse liegt bei etwa 50-100 ueber 5 Jahre -- d.h. 50 bis 100 Patienten muessen behandelt werden, damit ein Herzinfarkt verhindert wird. Diese Zahl ueberrascht viele Patienten und Aerzte."
    ),

    // Question 12 -- Statistical Concepts: NNH
    Question(
        categoryId = 16,
        questionText = "Was beschreibt der Number Needed to Harm (NNH) und wie unterscheidet er sich konzeptionell vom NNT?",
        answerA = "NNH beschreibt die Anzahl der Kontrollpatienten, die einen Schaden erlitten, NNT die der Behandelten",
        answerB = "NNH = 1 / absolute Risikosteigerung (ARI) durch die Behandlung; er gibt an, wie viele Patienten behandelt werden muessen, bis ein zusaetzlicher Schadensfall eintritt -- das Gegenstueck zum NNT auf der Schadensseite",
        answerC = "NNH ist der reziproke Wert des NNT und gibt an, wann eine Behandlung nicht mehr nutzt",
        answerD = "NNH wird nur in Beobachtungsstudien berechnet, NNT nur in RCTs",
        correctAnswer = 1,
        explanation = "Der NNH ist das Gegenstueck zum NNT: NNH = 1 / ARI, wobei ARI die absolute Erhoehung der Schadensrate durch die Behandlung ist. Ein NNH von 50 bedeutet: Bei 50 behandelten Patienten tritt ein zusaetzlicher Schaden auf, der ohne Behandlung nicht aufgetreten waere. Das Verhaeltnis NNT:NNH hilft bei Nutzen-Risiko-Abwaegungen.",
        difficulty = 4,
        funFact = "Das Verhaeltnis von NNT zu NNH ist ein praktisches Instrument fuer gemeinsame Entscheidungsfindung: Wenn NNT fuer Nutzen = 20 und NNH fuer Schaden = 200, ist das Nutzen-Risiko-Verhaeltnis 10:1 -- die Behandlung nutzt zehnmal haeufiger als sie schadet."
    ),

    // Question 13 -- Sensitivity and Specificity
    Question(
        categoryId = 16,
        questionText = "Ein Screening-Test hat eine Sensitivitaet von 95% und eine Spezifitaet von 80%. Die Praevalenz der Erkrankung betraegt 1%. Welche Aussage ueber den positiven praediktiven Wert (PPV) ist korrekt?",
        answerA = "Der PPV betraegt 95%, weil die Sensitivitaet sehr hoch ist",
        answerB = "Der PPV ist niedrig (ca. 4-5%), weil bei niedriger Praevalenz die meisten Positiven falsch-positiv sind -- viele Gesunde werden faelschlicherweise als krank identifiziert",
        answerC = "Der PPV betraegt 80%, entsprechend der Spezifitaet",
        answerD = "Der PPV kann aus diesen Daten nicht berechnet werden",
        correctAnswer = 1,
        explanation = "Bei 1% Praevalenz in 10.000 Personen: 100 Kranke (95 richtig positiv) und 9.900 Gesunde (1.980 falsch positiv bei 80% Spezifitaet). PPV = 95 / (95 + 1.980) = ca. 4,6%. Die Mehrheit der positiven Testergebnisse ist falsch positiv -- die sogenannte Basis-Raten-Falle.",
        difficulty = 4,
        funFact = "Das Basis-Raten-Problem erklaert, warum Massenscreenings auf seltene Erkrankungen oft mehr Schaden (Ueberdiagnostik, Angst, invasive Abklaerungen) als Nutzen anrichten -- ein zentrales Argument gegen undifferenziertes Screening-Programm."
    ),

    // Question 14 -- Sensitivity and Specificity: Clinical Application
    Question(
        categoryId = 16,
        questionText = "In welcher klinischen Situation wird ein Test mit hoher Sensitivitaet bevorzugt und warum?",
        answerA = "Bei der Bestaetigung einer Diagnose, weil hohe Sensitivitaet viele Falsch-Positive ausschliesst",
        answerB = "Beim Ausschluss einer gefaehrlichen Erkrankung (Ruling Out), weil ein Test mit hoher Sensitivitaet bei negativem Ergebnis die Erkrankung zuverlaessig ausschliesst -- wenige Erkrankte werden verpasst",
        answerC = "Bei selten vorkommenden Erkrankungen, weil hohe Sensitivitaet den PPV maximiert",
        answerD = "Beim Massenscreening, weil hohe Sensitivitaet die Spezifitaet automatisch erhoeht",
        correctAnswer = 1,
        explanation = "Eine hohe Sensitivitaet (wenige falsch-negative Ergebnisse) ist wertvoll, wenn man eine gefaehrliche Erkrankung ausschliessen moechte -- ein negatives Ergebnis macht die Erkrankung sehr unwahrscheinlich. Mnemonic: SnNOut (Sensitivity: Negative result rules Out). Beim Bestaetigen wird dagegen hohe Spezifitaet benoetigt (SpPIn).",
        difficulty = 4,
        funFact = "Der d-Dimer-Test zur Ausschlussdiagnostik einer Lungenembolie nutzt genau dieses Prinzip: hohe Sensitivitaet (~97%) bedeutet, dass ein negatives Ergebnis eine Lungenembolie mit grosser Sicherheit ausschliesst -- ohne teure Bildgebung."
    ),

    // Question 15 -- ROC Curves
    Question(
        categoryId = 16,
        questionText = "Was beschreibt die Area Under the Curve (AUC) einer ROC-Kurve und wie wird ein AUC-Wert von 0,85 interpretiert?",
        answerA = "AUC = 0,85 bedeutet, dass der Test 85% der Erkrankten identifiziert",
        answerB = "AUC ist die Wahrscheinlichkeit, dass ein zufaellig ausgewaehlter Erkrankter einen hoeheren Testwert hat als ein zufaellig ausgewaehlter Gesunder -- AUC = 0,85 bedeutet sehr gute Diskriminationsstaerke",
        answerC = "AUC = 0,85 zeigt, dass der Test bei 85% der Trennwerte besser ist als ein Zufallstest",
        answerD = "AUC = 0,85 entspricht einer Sensitivitaet von 85% bei optimaler Spezifitaet",
        correctAnswer = 1,
        explanation = "Die AUC der ROC-Kurve ist die C-Statistik und misst die Diskriminationsstaerke eines Tests. AUC = 0,5 entspricht Zufall; AUC = 1,0 ist perfekte Diskrimination. Ein Wert von 0,85 bedeutet: Bei 85% der Paare aus einem Kranken und einem Gesunden hat der Kranke den hoeheren Testwert -- gilt als guter Biomarker.",
        difficulty = 4,
        funFact = "ROC steht fuer Receiver Operating Characteristic -- die Methode wurde im Zweiten Weltkrieg zur Unterscheidung von Radarsignalen (Flugzeug vs. Rauschen) entwickelt und spaeter in die Medizin uebertragen."
    ),

    // Question 16 -- Relative Risk vs Odds Ratio
    Question(
        categoryId = 16,
        questionText = "Wann ist die Odds Ratio eine gute Naeherung fuer das relative Risiko und wann unterschaetzen Kliniker den Effekt bei direkter Interpretation als RR?",
        answerA = "Die OR naeherung gilt immer; sie ueberschaetzt das RR bei seltenen Ereignissen",
        answerB = "Die OR naeherung gilt bei seltenen Ereignissen (Praevalenz < 10%) -- bei haeufigen Ereignissen ueberschaetzt die OR das wahre relative Risiko erheblich, was zur Fehleinterpretung als groesserem Effekt fuehrt",
        answerC = "Die OR ist immer kleiner als das RR, daher unterschaetzt man den Effekt immer",
        answerD = "Die OR und das RR sind mathematisch identisch bei Praevalenzen unter 50%",
        correctAnswer = 1,
        explanation = "Bei niedrigen Ereignisraten (< 10%) gilt: OR naeherungsweise = RR. Bei haeufigen Ereignissen (z.B. 40%) divergieren OR und RR stark -- die OR ueberschaetzt das RR. Ein OR von 3,0 bei 40% Ereignisrate entspricht einem RR von nur etwa 1,9. Kliniker, die OR direkt als RR lesen, ueberschaetzen dann den Effekt.",
        difficulty = 4,
        funFact = "Diese Ueberschaetzung ist relevant bei Studien zu Krebsdiagnosen, wo Ereignisraten hoch sind. Zahlreiche publizierte OR-Schaetzer wurden in Medienberichten faelschlicherweise als RR interpretiert -- was zu erheblicher Verzerrung der oeffentlichen Wahrnehmung fuehrt."
    ),

    // Question 17 -- Kaplan-Meier Curves
    Question(
        categoryId = 16,
        questionText = "Was zeigt eine Kaplan-Meier-Kurve und wie wird der Unterschied zwischen zwei Gruppen statistisch getestet?",
        answerA = "Sie zeigt kumulative Inzidenz ueber die Zeit; Gruppenvergleich erfolgt mit dem t-Test",
        answerB = "Sie zeigt die Ueberlebensfunktion (Wahrscheinlichkeit, zum Zeitpunkt t noch ereignisfrei zu sein) und bereinigt zensierte Beobachtungen; Gruppenvergleiche werden mit dem Log-Rank-Test durchgefuehrt",
        answerC = "Sie zeigt mittlere Ueberlebenszeiten; Gruppenvergleich erfolgt mit dem Wilcoxon-Test",
        answerD = "Sie zeigt Hazard-Raten zu jedem Zeitpunkt; Gruppenvergleich erfolgt mit dem Chi-Quadrat-Test",
        correctAnswer = 1,
        explanation = "Kaplan-Meier-Kurven schaetzen die Ueberlebensfunktion S(t) unter Beruecksichtigung zensierter Daten (Personen, die aus der Beobachtung ausscheiden ohne das Ereignis erlebt zu haben). Der Log-Rank-Test prueft, ob zwei Kurven sich signifikant unterscheiden -- er gibt den Kurven gleiche Gewichtung ueber alle Zeitpunkte.",
        difficulty = 4,
        funFact = "Paul Kaplan und E.L. Meier publizierten ihre Methode 1958 im Journal of the American Statistical Association -- heute ist es eines der am haeufigsten zitierten statistischen Papiere in der medizinischen Literatur."
    ),

    // Question 18 -- Hazard Ratio
    Question(
        categoryId = 16,
        questionText = "Wie unterscheidet sich die Hazard Ratio konzeptionell vom relativen Risiko in Ueberlebenszeitanalysen?",
        answerA = "Die Hazard Ratio ist das Risiko zu einem festen Zeitpunkt, das RR ist der Durchschnitt ueber alle Zeitpunkte",
        answerB = "Die Hazard Ratio ist das Verhaeltnis der momentanen Ereignisraten (Hazard) zwischen zwei Gruppen zu jedem Zeitpunkt -- sie ist zeitvariabel und beruecksichtigt das Ueberleben bis zu diesem Zeitpunkt. Das RR ist ein kumulatives Mass",
        answerC = "Hazard Ratio und RR sind identisch, wenn keine zensierten Beobachtungen vorliegen",
        answerD = "Die Hazard Ratio wird nur bei nicht-parametrischen Tests benoetigt",
        correctAnswer = 1,
        explanation = "Der Hazard beschreibt die momentane Ereignisrate bei einer Person, die das Ereignis noch nicht erlebt hat. Die HR ist das Verhaeltnis der Hazards zweier Gruppen. Im Cox-Regressionsmodell wird eine konstante HR angenommen (Proportional Hazards Assumption) -- eine Annahme, die geprueft werden muss.",
        difficulty = 4,
        funFact = "Sir David Cox entwickelte das nach ihm benannte Cox-Proportional-Hazards-Modell 1972 -- es ist heute das meistgenutzte Verfahren zur Analyse von Ueberlebensdaten und ermoeglicht die simultane Kontrolle mehrerer Confounder."
    ),

    // Question 19 -- GRADE Evidence System
    Question(
        categoryId = 16,
        questionText = "Welche vier Faktoren koennen die Qualitaet von Evidenz im GRADE-System nach unten stufen?",
        answerA = "Geringe Stichprobengroesse, hohe Dropout-Rate, fehlende Verblindung, unklare Randomisierung",
        answerB = "Studiendesign-Risiko (Risk of Bias), Inkonsistenz der Ergebnisse, Indirektheit der Evidenz und Impraezision der Schaetzung -- dazu kann Publikationsbias als fuenfter Faktor kommen",
        answerC = "Fehlende Intention-to-Treat-Analyse, unzureichende Followup-Dauer, kleine Studienpopulation, fehlende Multivarianzanalyse",
        answerD = "Unzureichende statistische Power, fehlende Sensitivitaetsanalyse, retrospektives Design, unklare Endpunktdefinition",
        correctAnswer = 1,
        explanation = "GRADE stuft Evidenz von hoch bis sehr niedrig ein. Vier Faktoren koennen Evidenz downgraden: (1) Risk of Bias in Primaetstudien, (2) Inkonsistenz (heterogene Ergebnisse), (3) Indirektheit (Population/Intervention/Endpunkt weichen von der Frage ab) und (4) Impraezision (breite KI). Als fuenfter Faktor kann Publikationsbias hinzukommen.",
        difficulty = 4,
        funFact = "Das GRADE-System wurde 2004 von einer internationalen Arbeitsgruppe entwickelt und wird heute von ueber 100 Organisationen genutzt, darunter die WHO, Cochrane Collaboration und die meisten nationalen Leitlinien-Organisationen."
    ),

    // Question 20 -- GRADE: Upgrading Evidence
    Question(
        categoryId = 16,
        questionText = "Welche drei Faktoren koennen im GRADE-System die Qualitaet von Evidenz aus Beobachtungsstudien nach oben stufen?",
        answerA = "Grosse Stichprobe, lange Followup-Dauer, Multicenter-Design",
        answerB = "Grosser Effekt (HR oder RR > 2 oder < 0,5), Dosis-Wirkungs-Beziehung und Kontrolle aller plausiblen Confounder die den Effekt unterschaetzen",
        answerC = "Systematische Literatursuche, konsistente Ergebnisse, registriertes Studienprotokoll",
        answerD = "Peer-Review-Publication, mehrstufige Datenvalidierung, unabhaengige Replikation",
        correctAnswer = 1,
        explanation = "GRADE erlaubt das Upgrading von Beobachtungsstudien bei: (1) sehr grossen Effekten (RR/HR >/= 2 oder </= 0,5), da wenig Confounding einen solchen Effekt erklaeren koennte; (2) klarer Dosis-Wirkungs-Beziehung als Plausibilitaetshinweis; (3) wenn alle plausiblen Confounder den wahren Effekt eher verringern wuerden.",
        difficulty = 4,
        funFact = "Klassisches Upgrade-Beispiel: Der Zusammenhang zwischen Rauchen und Lungenkrebs erreicht trotz fehlender RCT-Evidenz ein hohes GRADE-Level -- der Effekt ist riesig (RR ~15), es gibt eine Dosis-Wirkungs-Beziehung, und Confounder wuerden eher zur Unterschaetzung fuehren."
    ),

    // Question 21 -- Framingham Heart Study
    Question(
        categoryId = 16,
        questionText = "Welchen wissenschaftlichen Beitrag hat die Framingham Heart Study (seit 1948) zur kardiovaskulaeren Medizin geleistet?",
        answerA = "Sie war das erste RCT, das Statine zur Herzinfarktpraevention evaluierte",
        answerB = "Sie identifizierte durch Langzeit-Kohorten-Beobachtung die wichtigsten kardiovaskulaeren Risikofaktoren (Hypertonie, Hypercholesterinaemie, Rauchen, Diabetes) und entwickelte den Framingham-Risk-Score",
        answerC = "Sie bewies erstmals, dass Aspirin Herzinfarkte verhindert durch ein placebo-kontrolliertes Design",
        answerD = "Sie zeigte, dass genetische Faktoren allein fuer 90% des Herzinfarktrisikos verantwortlich sind",
        correctAnswer = 1,
        explanation = "Die Framingham Heart Study begann 1948 mit 5.209 Bewohnern aus Framingham, Massachusetts. Als prospektive Kohortenstudie (heute in der dritten Generation) identifizierte sie systematisch modifizierbare Risikofaktoren fuer Herz-Kreislauf-Erkrankungen und war wegweisend fuer das Konzept des Risikofaktor-basierten Praevention.",
        difficulty = 4,
        funFact = "Der Begriff 'Risikofaktor' wurde massgeblich durch die Framingham-Forscher gepraegt -- heute selbstverstaendlich in der Medizin, war das Konzept, dass Krankheiten vorhersagbare Vorstufen haben, in den 1950ern revolutionaer."
    ),

    // Question 22 -- Nurses Health Study
    Question(
        categoryId = 16,
        questionText = "Was ist die Nurses' Health Study und welche methodischen Besonderheiten zeichnen sie aus?",
        answerA = "Ein RCT mit Krankenschwestern als Kontrollgruppe fuer pharmakologische Studien",
        answerB = "Eine grosse prospektive Kohortenstudie (1976, 121.700 Krankenschwestern), die durch regelmaessige Fragebogenerhebungen Ernaehrung, Hormoneinsatz und Erkrankungsrisiken untersuchte -- Grundlage fuer hormonale Kontrazeptiva- und Hormonersatztherapie-Forschung",
        answerC = "Eine Fall-Kontroll-Studie zu Berufsrisiken in der Pflege mit Fokus auf Nadelstichverletzungen",
        answerD = "Eine Querschnittsstudie zu psychischer Gesundheit in Pflegeberufen in den USA",
        correctAnswer = 1,
        explanation = "Die Nurses' Health Study wurde 1976 von Frank Speizer und Walter Willett gestartet und ist eine der groessten prospektiven Kohortenstudien weltweit. Sie lieferte grundlegende Erkenntnisse zu oralen Kontrazeptiva, Hormongaben in der Menopause, Ernaehrung und Krebsrisiko -- und beeinflusste massgeblich die Empfehlungen zur Hormonersatztherapie.",
        difficulty = 4,
        funFact = "Die Nurses' Health Study zeigte in den 1990ern, dass postmenopausale Hormonersatztherapie das kardiovaskulaere Risiko erhoeht -- ein Ergebnis, das spaeter durch die Women's Health Initiative (RCT) bestaetigt und die weltweite Verordnungspraxis fundamental veraendert hat."
    ),

    // Question 23 -- Bioethics: Autonomy
    Question(
        categoryId = 16,
        questionText = "Was umfasst das Prinzip der Patientenautonomie nach Beauchamp und Childress und welche praktische Anforderung ergibt sich fuer den klinischen Alltag?",
        answerA = "Autonomie bedeutet, dass Patienten alle medizinischen Entscheidungen alleine treffen, ohne aerztliche Empfehlung",
        answerB = "Autonomie ist das Recht informierter und entscheidungsfaehiger Patienten, medizinische Massnahmen zu akzeptieren oder abzulehnen -- es begruendet die Pflicht zur Informed Consent vor jeden Eingriff",
        answerC = "Autonomie gilt nur fuer urteilsfaehige erwachsene Patienten und entfaellt bei Notfaellen vollstaendig",
        answerD = "Autonomie ist das Recht, einen anderen Arzt zu waehlen, nicht aber Behandlungen abzulehnen",
        correctAnswer = 1,
        explanation = "Das Autonomieprinzip nach Beauchamp und Childress ('Principles of Biomedical Ethics', 1979) begruendet die Pflicht zur Aufklaerung und zum informierten Einverstaendnis. Voraussetzungen der Entscheidungsfaehigkeit sind: Verstehen, Einordnen, Schlussfolgern und Kommunizieren der Entscheidung.",
        difficulty = 4,
        funFact = "Das erste internationale Dokument, das Patientenautonomie und Informed Consent kodifizierte, war der Nuerberger Kodex (1947) -- entstanden als direkte Reaktion auf die verbrecherischen Menschenversuche der Nazis im Zweiten Weltkrieg."
    ),

    // Question 24 -- Bioethics: Beneficence and Non-maleficence
    Question(
        categoryId = 16,
        questionText = "Wie unterscheiden sich Beneficence und Non-maleficence als ethische Prinzipien und in welcher Situation koennen sie in Konflikt geraten?",
        answerA = "Beneficence bedeutet Schadensvermeidung, Non-maleficence aktives Handeln zum Patientenwohl -- beide sind identisch",
        answerB = "Beneficence fordert aktives Handeln zum Wohl des Patienten, Non-maleficence fordert Schaden zu unterlassen -- Konflikt entsteht z.B. bei zytotoxischer Chemotherapie, die Schaden verursacht um groesseren Schaden zu verhindern",
        answerC = "Non-maleficence gilt nur fuer Aerzte, Beneficence nur fuer Pflegepersonal",
        answerD = "Beide Prinzipien gelten nur in der Forschungsethik, nicht in der klinischen Praxis",
        correctAnswer = 1,
        explanation = "Beneficence (tue Gutes) und Non-maleficence (tue keinen Schaden) bilden zusammen das klassische aerztliche Ethos. Bei vielen Therapien -- Chemotherapie, Operationen, Intensivmedizin -- muss ein kalkulierter Schaden in Kauf genommen werden, um einen groesseren Nutzen zu erzielen. Die Abwaegung ist ein zentrales ethisches Problem der klinischen Praxis.",
        difficulty = 4,
        funFact = "Primum non nocere (zuerst nicht schaden) wird Hippokrates zugeschrieben, findet sich aber in dieser Formulierung nicht im hippokratischen Eid -- der Satz entstammt dem 19. Jahrhundert und wurde Thomas Sydenham zugeschrieben."
    ),

    // Question 25 -- Bioethics: Justice
    Question(
        categoryId = 16,
        questionText = "Was bedeutet Gerechtigkeit (Justice) als ethisches Prinzip in der Medizin und welche praktische Dimension hat es in der Ressourcenverteilung?",
        answerA = "Gerechtigkeit bedeutet, dass alle Patienten identische Behandlungen erhalten muessen",
        answerB = "Gerechtigkeit fordert faire Verteilung von Gesundheitsleistungen und Lasten -- sie begruendet Verteilungsprinzipien wie Gleichheit, Beduerfnisorientiertheit und Nutzensmaximierung und ist zentral in Triage und Priorisierungsdebatten",
        answerC = "Gerechtigkeit gilt nur auf Systemebene und hat keinen Einfluss auf individuelle Arzt-Patient-Entscheidungen",
        answerD = "Gerechtigkeit bedeutet, dass keine Patientengruppe von Studienteilnahmen ausgeschlossen werden darf",
        correctAnswer = 1,
        explanation = "Das Gerechtigkeitsprinzip nach Beauchamp und Childress adressiert faire Verteilung von Vorteilen und Belastungen. In der Gesundheitsversorgung umfasst es formale Gleichheit (gleiche Faelle gleich behandeln) und materiale Gerechtigkeit (nach Bedarf, Verdienst oder Nutzensmaximierung). Es ist zentral in Triage, QALY-Abwaegungen und Pandemie-Ressourcenplanung.",
        difficulty = 4,
        funFact = "Der Belmont Report (1979) -- entstanden nach dem Tuskegee-Skandal -- kodifizierte Justice als eines von drei Kernprinzipien der Forschungsethik und fuehrte zur Pflicht, vulnerable Gruppen sowohl zu schuetzen als auch in Forschung einzubeziehen."
    ),

    // Question 26 -- Tuskegee Study
    Question(
        categoryId = 16,
        questionText = "Welche ethischen Grundprinzipien verletzte die Tuskegee Syphilis-Studie (1932-1972) der US Public Health Service?",
        answerA = "Sie verletzte nur das Datenschutzprinzip durch unerlaubte Datenweitergabe",
        answerB = "Sie verletzte Autonomie (kein Informed Consent), Non-maleficence (bewusste Behandlungsvorenthaltung auch nach Verfuegbarkeit von Penicillin) und Justice (systematische Benachteiligung Schwarzer Maenner)",
        answerC = "Sie verletzte ausschliesslich das Beneficence-Prinzip durch ungeeignete Studiendesigns",
        answerD = "Sie verletzte das Gerechtigkeitsprinzip durch Ausschluss von Frauen aus der Studie",
        correctAnswer = 1,
        explanation = "400 afroamerikanische Maenner mit Syphilis wurden beobachtet, ohne informiertes Einverstaendnis und ohne Behandlung -- selbst nachdem Penicillin 1947 verfuegbar war. Die Studie lief 40 Jahre und fuehrte zu massivem Vertrauensverlust in medizinische Forschung. Sie war direkter Anlass fuer den Belmont Report und das National Research Act.",
        difficulty = 4,
        funFact = "Die Nachwirkungen der Tuskegee-Studie sind bis heute messbar: Afroamerikanische Maenner zeigen signifikant weniger Vertrauen in das Gesundheitssystem -- epidemiologische Analysen zeigen, dass dies mit hoeherem Misstrauen gegenueber COVID-19-Impfungen und geringerer Inanspruchnahme praeventiver Medizin assoziiert ist."
    ),

    // Question 27 -- Declaration of Helsinki
    Question(
        categoryId = 16,
        questionText = "Was regelt die Deklaration von Helsinki (1964, rev. 2013) und worin unterscheidet sie sich vom Nuerberger Kodex?",
        answerA = "Helsinki regelt nur industrielle Pharmaforschung, der Nuerberger Kodex nur staatliche Forschung",
        answerB = "Helsinki gilt fuer jede medizinische Forschung am Menschen einschliesslich nicht-therapeutischer Studien; im Unterschied zum Nuerberger Kodex erlaubt sie unter strengen Bedingungen auch Forschung an nicht einwilligungsfaehigen Personen",
        answerC = "Helsinki gilt nur fuer Forschung in Entwicklungslaendern, der Nuerberger Kodex fuer alle Nationen",
        answerD = "Helsinki ersetzte den Nuerberger Kodex vollstaendig und hat groessere Rechtskraft",
        correctAnswer = 1,
        explanation = "Die Deklaration von Helsinki des Weltaerztebundes reguliert ethische Grundsaetze fuer medizinische Forschung. Gegenueber dem Nuerberger Kodex (1947) erlaubt sie unter bestimmten Voraussetzungen Forschung an nicht einwilligungsfaehigen Personen (z.B. Kinder, Bewusstlose) mit Einwilligung gesetzlicher Vertreter -- eine wichtige Erweiterung fuer die Paediatrie.",
        difficulty = 4,
        funFact = "Die Helsinki-Deklaration wurde seit 1964 achtmal revidiert -- die Revision von 2000 war besonders kontrovers, da sie Placebo-Kontrollen nur noch erlaubt, wenn keine bewiesenermassen wirksame Therapie verfuegbar ist. Diese Aenderung loeste internationale Debatten ueber Forschung in ressourcenarmen Laendern aus."
    ),

    // Question 28 -- Nobel Prize: Fleming
    Question(
        categoryId = 16,
        questionText = "Alexander Fleming, Howard Florey und Ernst Chain erhielten 1945 den Nobelpreis fuer die Entdeckung des Penicillins. Was war Flemings urspruengliche Beobachtung von 1928?",
        answerA = "Fleming synthetisierte Penicillin chemisch aus Schimmelpilzextrakten und testete es an Maeusesepsis",
        answerB = "Fleming beobachtete, dass Kolonien von Staphylococcus aureus in der Naehe einer Penicillium notatum-Kontamination lysiert wurden -- er schloss auf eine bakterizide Substanz, konnte sie aber nicht isolieren",
        answerC = "Fleming entwickelte die ersten klinischen Protokolle zur Penicillintherapie bei Syphilis",
        answerD = "Fleming entdeckte Penicillin durch gezieltes Suchen nach Antibiotika im Rahmen des Ersten Weltkriegs",
        correctAnswer = 1,
        explanation = "Fleming beobachtete 1928 zunaechst, dass eine Penicillium-Schimmelkontamination auf einer Bakterienkulturplatte einen klaren Hemmhof bildete. Er erkannte die antimikrobielle Wirkung und nannte die aktive Substanz Penicillin -- konnte sie aber nicht stabil isolieren. Erst Florey und Chain reinigten und stabilisierten das Penicillin fuer den klinischen Einsatz.",
        difficulty = 4,
        funFact = "Flemings Entdeckung war ein Zufallsfund -- er hatte vergessen, eine Kulturplatte vor dem Urlaub abzudecken. Diese Geschichte betont, dass viele wissenschaftliche Durchbrueche auf Beobachtungsgeist und Offenheit fuer Unerwartetes beruhen -- nicht nur auf systematischer Planung."
    ),

    // Question 29 -- Nobel Prize: Banting and Best
    Question(
        categoryId = 16,
        questionText = "Fredrick Banting und Charles Best isolierten 1921 Insulin. Welchen experimentellen Schluesselbeitrag lieferten sie?",
        answerA = "Sie synthetisierten Insulin chemisch und testeten es in Zellkulturen",
        answerB = "Sie ligierten die Pankreasgaenge von Hunden, gewannen dabei insulinreiche Extrakte aus degenerierten Azini und zeigten, dass diese Extrakte den Blutzucker pankreatektomierter Hunde senkten",
        answerC = "Sie entdeckten die Insulinrezeptoren auf Muskelzellen und klaerten den Signalweg auf",
        answerD = "Sie entwickelten den ersten Radioimmunoassay zur Insulinmessung im Serum",
        correctAnswer = 1,
        explanation = "Banting und Best (unter MacLeods Supervision) ligierten Hundepankreasgaenge, sodass die exokrinen Azini degenerierten -- die endokrinen Langerhans-Inseln blieben erhalten. Aus diesen Pankreas-Resten gewannen sie den 'Isletin'-Extrakt (spaeter Insulin genannt) und zeigten durch Blutglukose-Messungen bei pankreatektomierten Hunden seine blutzuckersenkende Wirkung.",
        difficulty = 4,
        funFact = "Der erste menschliche Patient, dem Insulin verabreicht wurde, war Leonard Thompson (14 Jahre alt, Toronter Krankenhaus, Januar 1922). Nach dem ersten Versuch (unreiner Extrakt, allergische Reaktion) heilte der zweite, gereinigtere Versuch seine diabetische Ketoazidose innerhalb von Stunden -- einer der dramatischsten therapeutischen Erfolge der Medizingeschichte."
    ),

    // Question 30 -- Nobel Prize: Watson and Crick
    Question(
        categoryId = 16,
        questionText = "Watson und Crick erhielten 1962 den Nobelpreis fuer die Doppelhelixstruktur der DNA. Welche Schluesseldaten waren entscheidend fuer ihr Modell?",
        answerA = "Ergebnisse aus Elektronenmikroskopie und Proteinkristallographie von Linus Pauling",
        answerB = "Rosalind Franklins Roentgenbeugungsaufnahme (Photo 51) sowie Chargaffs Regelmaessigkeiten (A=T, G=C) lieferten die entscheidenden strukturellen Einschraenkungen fuer das Modell",
        answerC = "Biochemische Sequenzierungsdaten von Frederick Sanger zur Basenreihenfolge",
        answerD = "Optische Absorptionsspektren von Erwin Chargaff zum UV-Absorptionsverhalten der Basen",
        correctAnswer = 1,
        explanation = "Photo 51 (Franklin/Gosling, 1952) zeigte die Kreuzstruktur typisch fuer eine Helix und lieferte die Gitterkonstanten. Chargaffs Regeln (A immer = T, G immer = C) implizierten komplementaere Basenpaarung. Beide Erkenntnisse zusammen ermoeglichten Watson und Crick 1953 ihr Modell der antiparallelen Doppelhelix.",
        difficulty = 4,
        funFact = "Rosalind Franklin erhielt den Nobelpreis nicht -- sie starb 1958 an Ovarialkarzinom, vier Jahre vor der Verleihung. Der Nobelpreis wird nicht posthum vergeben. Die Umstaende, unter denen Watson und Crick ohne Franklins Wissen Zugang zu Photo 51 erhielten, sind bis heute Gegenstand ethischer Debatten ueber wissenschaftliche Intergritaet."
    ),

    // Question 31 -- Confounding
    Question(
        categoryId = 16,
        questionText = "Was ist ein Confounder und welche drei Kriterien muss eine Variable erfuellen, um als Confounder zu gelten?",
        answerA = "Ein Confounder ist jede Variable, die mit dem Ergebnis assoziiert ist, unabhaengig von der Exposition",
        answerB = "Ein Confounder muss (1) mit der Exposition assoziiert sein, (2) unabhaengig von der Exposition ein Risikofaktor fuer den Endpunkt sein, und (3) darf nicht im kausalen Pfad zwischen Exposition und Endpunkt liegen",
        answerC = "Ein Confounder ist eine Variable, die den Stichprobenfehler erhoehrt und die Power der Studie senkt",
        answerD = "Ein Confounder tritt nur in randomisierten Studien auf und wird durch Schichtung kontrolliert",
        correctAnswer = 1,
        explanation = "Ein klassischer Confounder muss alle drei Kriterien erfuellen: (1) Assoziation mit der Exposition (z.B. Rauchen und Kaffeekonsum), (2) unabhaengiger Einfluss auf den Endpunkt (Rauchen und Lungenkrebs), (3) kein Mediator (kein Mechanismus ueber den die Exposition wirkt). Wird ein Confounder nicht kontrolliert, wird der wahre Effekt verzerrt.",
        difficulty = 4,
        funFact = "Das klassische Confounder-Beispiel ist der 'gelbe Finger'-Confounder in fruehen Lungenkrebsstudien: Nikotinflecken an Fingern waren assoziiert mit Lungenkrebs -- aber nicht als Ursache, sondern als Marker fuer intensives Rauchen. Wer nur die gelben Finger sah, waere zum falschen Schluss gekommen."
    ),

    // Question 32 -- Effect Modification / Interaction
    Question(
        categoryId = 16,
        questionText = "Was ist der Unterschied zwischen Confounding und Effektmodifikation (Interaction) und wie geht man methodisch mit beidem um?",
        answerA = "Confounding und Effektmodifikation sind identisch und werden beide durch Stratifizierung behandelt",
        answerB = "Confounding ist eine Verzerrung die kontrolliert (entfernt) werden soll; Effektmodifikation ist ein biologisches Phaenomen das berichtet (beschrieben) werden soll -- beim Confounder sind stratifizierte Schaetzer aehnlich, beim Effektmodifikator verschieden",
        answerC = "Confounding tritt nur bei prospektiven Studien auf, Effektmodifikation nur bei retrospektiven",
        answerD = "Effektmodifikation ist eine Form des Information Bias und wird durch Verblindung kontrolliert",
        correctAnswer = 1,
        explanation = "Confounding verzerrt den wahren Zusammenhang -- der stratifizierte Schaetzer weicht vom crude Schaetzer ab, stratifizierte Schaetzer sind aber untereinander aehnlich. Effektmodifikation bedeutet, dass der Effekt der Exposition in verschiedenen Subgruppen unterschiedlich stark ist -- die stratifizierten Schaetzer unterscheiden sich voneinander. Effektmodifikation ist ein inhaltlich wichtiges Ergebnis, das nicht wegkontrolliert werden soll.",
        difficulty = 4,
        funFact = "Ein bekanntes Beispiel fuer Effektmodifikation: Der Zusammenhang zwischen Alkohol und kolorektalem Karzinom ist bei Traegern des MTHFR 677TT-Genotyps staerker als bei anderen -- die genetische Variante modifiziert den Alkohol-Krebs-Effekt. Solche Gen-Umwelt-Interaktionen sind Kern der Epigenetik-Forschung."
    ),

    // Question 33 -- Internal vs External Validity
    Question(
        categoryId = 16,
        questionText = "Was bedeutet der Unterschied zwischen interner und externer Validitaet einer Studie und warum stehen sie oft in einem Spannungsverhaeltnis?",
        answerA = "Interne Validitaet beschreibt die Stichprobengroesse, externe Validitaet die statistische Power",
        answerB = "Interne Validitaet bedeutet, dass Kausalschluesse innerhalb der Studie korrekt sind (keine Verzerrung, kein Confounding). Externe Validitaet bedeutet Generalisierbarkeit auf andere Populationen -- hoch kontrollierte RCTs maximieren interne Validitaet, aber enge Einschlusskriterien begrenzen die externe Validitaet",
        answerC = "Interne Validitaet bezieht sich auf die Konsistenz der Messinstrumente, externe Validitaet auf die Reproduzierbarkeit in anderen Labors",
        answerD = "Beide Konzepte sind synonym und werden synonym mit Reliabilitaet verwendet",
        correctAnswer = 1,
        explanation = "Der Konflikt entsteht, weil Massnahmen zur Sicherung der internen Validitaet (strenge Ein-/Ausschlusskriterien, standardisierte Bedingungen) die Repraesentativitaet der Stichprobe einschraenken. Pragmatische Studien versuchen, diesen Konflikt zu reduzieren, indem sie unter realen Versorgungsbedingungen durchgefuehrt werden.",
        difficulty = 4,
        funFact = "RCTs haben haeufig Probleme mit externer Validitaet: Frauen, aeltere Patienten und Patienten mit Komorbiditaeten werden systematisch aus Studien ausgeschlossen. So sind viele Leitlinienempfehlungen fuer Multimorbide auf Evidenz gestuetzt, die an 'sauberen' Studienpopulationen gewonnen wurde."
    ),

    // Question 34 -- Intention-to-Treat vs Per-Protocol
    Question(
        categoryId = 16,
        questionText = "Warum bevorzugt man in konfirmativen RCTs die Intention-to-Treat (ITT)-Analyse gegenueber der Per-Protocol-Analyse?",
        answerA = "Weil die ITT-Analyse eine groessere Stichprobe hat und daher eine hoehere Power zeigt",
        answerB = "Die ITT-Analyse wertet alle randomisierten Patienten in ihrer urspruenglichen Gruppe aus -- sie behaelt die durch Randomisierung erzeugte Vergleichbarkeit und spiegelt den realen klinischen Nutzen inkl. Non-Compliance wider. Per-Protocol-Analyse kann Selektionsbias einfuehren",
        answerC = "Per-Protocol-Analysen sind methodisch veraltet und werden nur noch in Aequivalenzstudien eingesetzt",
        answerD = "ITT-Analysen sind vorgeschrieben fuer pharmazeutische Zulassungsstudien, aber nicht fuer akademische Forschung",
        correctAnswer = 1,
        explanation = "Bei der ITT-Analyse werden alle randomisierten Teilnehmer in ihrer zugeteilten Gruppe analysiert, auch wenn sie die Intervention nicht eingehalten haben. Dies behaelt die Vergleichbarkeit der Gruppen und verhindert, dass selektive Ausschlueche (z.B. Non-Responder nur in der Behandlungsgruppe) das Ergebnis verfaelschen. Per-Protocol-Analysen koennen ergaenzend berichtet werden.",
        difficulty = 4
    ),

    // Question 35 -- Systematic Review and Meta-Analysis
    Question(
        categoryId = 16,
        questionText = "Was ist der methodische Unterschied zwischen einem systematischen Review und einer Meta-Analyse?",
        answerA = "Systematische Reviews nutzen nur qualitative Methoden, Meta-Analysen nur quantitative",
        answerB = "Ein systematischer Review ist eine strukturierte Literaturzusammenfassung mit definierten Suchstrategien; eine Meta-Analyse ist eine statistische Methode zur quantitativen Zusammenfuehrung der Ergebnisse aus mehreren Studien -- jede Meta-Analyse sollte auf einem systematischen Review basieren",
        answerC = "Meta-Analysen umfassen immer mehr Studien als systematische Reviews",
        answerD = "Systematische Reviews werden nur von Cochrane durchgefuehrt, Meta-Analysen von pharmazeutischen Unternehmen",
        correctAnswer = 1,
        explanation = "Ein systematischer Review beschreibt den Prozess der Literatursuche und -bewertung (PICO-Frage, Suchstrategie, Selektionskriterien, Risiko-of-Bias-Bewertung). Die Meta-Analyse ist die optionale statistische Synthese der Ergebnisse. Nicht jeder systematische Review endet in einer Meta-Analyse -- zum Beispiel wenn die Studien zu heterogen sind.",
        difficulty = 4,
        funFact = "Die Cochrane Collaboration wurde 1993 von Iain Chalmers gegruendet und ist heute die groesste internationale Organisation fuer systematische Reviews. Ihr Name ehrt Archibald Cochrane, der als Pionier der evidenzbasierten Medizin gilt und 1972 kritisierte, dass medizinische Praxis zu wenig auf kontrollierter Evidenz beruhe."
    ),

    // Question 36 -- Heterogeneity in Meta-Analysis
    Question(
        categoryId = 16,
        questionText = "Was ist I-squared (I2) in einer Meta-Analyse und wie wird ein I2 von 75% interpretiert?",
        answerA = "I2 = 75% bedeutet, dass 75% der Studien einen statistisch signifikanten Effekt zeigen",
        answerB = "I2 misst den Anteil der Gesamtvarianz zwischen Studien, der auf wahre Heterogenitaet (und nicht auf Zufall) zurueckzufuehren ist -- I2 = 75% zeigt erhebliche Heterogenitaet an und stellt die Sinnhaftigkeit einer gepoolten Schaetzung in Frage",
        answerC = "I2 = 75% bedeutet, dass 75% der Studien vom gepoolten Effekt abweichen",
        answerD = "I2 misst den Anteil der Varianz der durch Publikationsbias erklaert wird",
        correctAnswer = 1,
        explanation = "I2 nach Higgins quantifiziert den Anteil der Gesamtvarianz, der auf Heterogenitaet zwischen Studien zurueckzufuehren ist (und nicht auf Stichprobenfehler). Konventionell: I2 < 25% = gering, 25-50% = moderat, 50-75% = erheblich, > 75% = sehr erheblich. Bei hoher Heterogenitaet sollte man eher ein Random-Effects-Modell waehlen und Subgruppenanalysen durchfuehren.",
        difficulty = 4,
        funFact = "Hohe I2-Werte bedeuten nicht, dass eine Meta-Analyse sinnlos ist -- sie fordert aber zur Exploration der Heterogenitaetsquellen auf (z.B. unterschiedliche Patientenpopulationen, Dosierungen, Follow-up-Zeiten). Diese Exploration kann wertvoller sein als die gepoolte Schaetzung selbst."
    ),

    // Question 37 -- Randomization Methods
    Question(
        categoryId = 16,
        questionText = "Was ist der Unterschied zwischen einfacher Randomisierung und Block-Randomisierung und wann wird Letztere bevorzugt?",
        answerA = "Block-Randomisierung ist aufwendiger, aber produziert identische Ergebnisse wie einfache Randomisierung",
        answerB = "Block-Randomisierung garantiert ausgeglichene Gruppengroessen nach jedem Block -- wichtig bei kleinen Studien oder erwarteter Zeittrend-Verzerrung (Seasonal Bias), da einfache Randomisierung bei kleinen n zufaellig sehr ungleiche Gruppen erzeugen kann",
        answerC = "Einfache Randomisierung wird nur bei Studien mit mehr als 500 Teilnehmern empfohlen",
        answerD = "Block-Randomisierung ist identisch mit Stratifizierung nach Risikofaktoren",
        correctAnswer = 1,
        explanation = "Bei einfacher Randomisierung koennte bei kleinen Studien zufaellig eine starke Unbalance entstehen (z.B. 20:30 statt 25:25). Block-Randomisierung stellt sicher, dass nach jedem Block (z.B. 4er oder 6er-Gruppen) gleich viele Teilnehmer in jeder Gruppe sind. Sie wird besonders bei kleinen Studien, Mehrphasenstudien oder Zeittrends empfohlen.",
        difficulty = 4,
        funFact = "Bei offener Block-Randomisierung mit bekannter Blockgroesse kann der Randomisierungsplan vorhergesagt werden -- wenn z.B. bei 4er-Blöcken 3 Zuweisungen bekannt sind, ist die vierte deterministisch. Daher werden Blockgroessen oft variiert und verdeckt gehalten."
    ),

    // Question 38 -- Ecological Fallacy
    Question(
        categoryId = 16,
        questionText = "Was ist der 'Ecological Fallacy' (oekologischer Fehlschluss) und in welchen Studien tritt er typischerweise auf?",
        answerA = "Der oekologische Fehlschluss bezeichnet den Fehler, Labordaten auf klinische Kontexte zu uebertragen",
        answerB = "Ergebnisse auf Gruppenebene (aggregierte Daten) werden faelschlicherweise auf Individuen uebertragen -- z.B. hohe Fettzufuhr auf Laenderebene korreliert mit Herzinfarktrate, bedeutet aber nicht, dass individuelle Fettesser ein hoeheres Herzinfarktrisiko haben",
        answerC = "Der Fehlschluss tritt auf, wenn Ergebnisse aus Tierexperimenten direkt auf Menschen uebertragen werden",
        answerD = "Oekologische Fehlschluesse entstehen ausschliesslich bei multinationalen Studien durch kulturelle Missverstaendnisse",
        correctAnswer = 1,
        explanation = "Oekologische Studien nutzen aggregierte Daten (z.B. laenderspezifische Durchschnittswerte) -- Assoziationen auf dieser Ebene koennen aufgrund von Confounding und Aggregationseffekten nicht auf Individualebene uebertragen werden. Der Fehlschluss wurde prominent von W.S. Robinson (1950) beschrieben, der zeigte, dass Analphabetenraten auf Staatsebene negativ mit Geburtsort korrelieren -- ein Scheineffekt der Aggregation.",
        difficulty = 4,
        funFact = "Das beruehrmteste oekologische Paradoxon in der Ernaehrungsepidemiologie: Laender mit hohem Fettverzehr (z.B. Frankreich) haben trotzdem niedrige Herzinfarktmortalitaet (French Paradox) -- ein Befund aus aggregierten Daten, der nicht bedeutet, dass Fett auf Individualebene schuetzt."
    ),

    // Question 39 -- Berkson's Bias
    Question(
        categoryId = 16,
        questionText = "Was ist Berksons Bias und in welchem Studienkontext tritt er auf?",
        answerA = "Berksons Bias ist ein Informationsbias durch fehlerhafte Labormessungen in Krankenhausproben",
        answerB = "Berksons Bias ist ein Selektionsbias in krankenhaus-basierten Fall-Kontroll-Studien: Krankenhauspateinten haben haeufiger Komorbiditaeten als die Allgemeinbevoelkerung -- werden Kontrollen ebenfalls aus dem Krankenhaus rekrutiert, kann eine falsche Assoziation entstehen",
        answerC = "Berksons Bias tritt auf, wenn expositionierte Faelle und nicht-exponierte Kontrollen aus verschiedenen Zeithorizonten stammen",
        answerD = "Berksons Bias bezeichnet den Fehler, Confounder retrospektiv zu adjustieren",
        correctAnswer = 1,
        explanation = "Joseph Berkson beschrieb 1946, dass in krankenhaus-basierten Studien die Expositions-Endpunkt-Assoziation verfaelscht wird, weil Hospitalisierungswahrscheinlichkeit von Exposition UND Endpunkt abhaengt. Wenn zwei Erkrankungen zusammen haeufiger zur Hospitalisierung fuehren, erscheinen sie in Krankenhausstichproben assoziiert -- auch wenn sie in der Allgemeinbevoelkerung unabhaengig sind.",
        difficulty = 4,
        funFact = "Berksons Bias erklaert, warum viele fruehe Befunde aus Krankenhaus-Populationen spaeter nicht reproduzierbar waren -- ein wichtiger Grund, warum bevoelkerungsbasierte Kontrollen gegenueber Krankenhauskontrollen methodisch vorzuziehen sind."
    ),

    // Question 40 -- Neyman Bias
    Question(
        categoryId = 16,
        questionText = "Was ist Neyman Bias (Inzidenz-Praevalenz-Bias) und wie beeinflusst er die Schaetzung von Risikoassoziationen?",
        answerA = "Neyman Bias entsteht durch selektive Studienrekrutierung von Patienten mit langer Wartezeit",
        answerB = "Neyman Bias entsteht, wenn Praevalenzfaelle statt Inzidenzfaelle untersucht werden -- Erkrankte mit raschem Tod oder Heilung werden nicht erfasst, was Exposition-Endpunkt-Assoziationen verzerrt, da Ueberlebende moeglicherweise eine untypische Exposition haben",
        answerC = "Neyman Bias ist die systematische Unterschaetzung von Inzidenzen durch administrative Datenverluste",
        answerD = "Neyman Bias beschreibt die Verzerrung durch zeitlich verzoegerte Diagnosestellung bei chronischen Krankheiten",
        correctAnswer = 1,
        explanation = "Bei der Verwendung von Praevalenzfaellen (Querschnitt) werden Personen mit kurzer Krankheitsdauer (durch Tod oder rasche Heilung) unterrepraesentiert. Wenn die Exposition die Krankheitsdauer (nicht nur die Inzidenz) beeinflusst, kann die Assoziation verzerrt erscheinen. Jerzy Neyman beschrieb diesen Effekt in Bezug auf koronare Herzkrankheit und Cholesterin.",
        difficulty = 4,
        funFact = "Das klassische Beispiel fuer Neyman Bias: Bei einem Herzinfarkt-Erreger der rasch toedlich ist, wuerden Praevalenzstudien faelschlicherweise einen protektiven Effekt des Erregers zeigen -- nur Ueberlebende (ohne den Erreger oder mit mildem Verlauf) werden erfasst."
    ),

    // Question 41 -- Hawthorne Effect
    Question(
        categoryId = 16,
        questionText = "Was ist der Hawthorne-Effekt und welche methodische Herausforderung stellt er in klinischen Studien dar?",
        answerA = "Der Hawthorne-Effekt bezeichnet die Tendenz von Aerzten, bei klinischen Studien strengere Protokolle einzuhalten als in der Praxis",
        answerB = "Studienteilnehmer veraendern ihr Verhalten allein durch das Bewusstsein, beobachtet zu werden -- dies verfaelscht den gemessenen Interventionseffekt, da Verhaltensaenderungen beider Gruppen (Treatment und Kontrolle) auftreten koennen",
        answerC = "Der Hawthorne-Effekt beschreibt die Regression zur Mitte bei Messwiederholungen",
        answerD = "Der Effekt bezeichnet systematische Verbesserungen in Beobachtungszeiten durch erhoehte Ressourcenzuteilung",
        correctAnswer = 1,
        explanation = "Der Hawthorne-Effekt (benannt nach einer Elektronikstudie in den 1920ern) beschreibt Verhaltensveraenderungen durch Beobachtung. In klinischen Studien koennen Teilnehmer gesundheitsfoerderlicheres Verhalten zeigen, nur weil sie sich beobachtet fuehlen -- unabhaengig von der eigentlichen Intervention. Kontrollgruppen-Designs mindern, eliminieren aber nicht vollstaendig diesen Effekt.",
        difficulty = 4,
        funFact = "Ironischerweise sind die originalen Hawthorne-Daten (Western Electric Company, 1924-1932) methodisch sehr schwach und die Schlussfolgerungen wurden spaeter stark revidiert -- der 'Hawthorne-Effekt' ist paradoxerweise selbst ein Beispiel fuer fragwuerdig interpretierte Beobachtungsdaten."
    ),

    // Question 42 -- Power and Sample Size
    Question(
        categoryId = 16,
        questionText = "Was beschreibt die statistische Power (1-beta) einer Studie und welche vier Faktoren bestimmen sie?",
        answerA = "Power = Wahrscheinlichkeit, einen signifikanten p-Wert bei der Nullhypothese zu erhalten; sie haengt von Signifikanzniveau und Stichprobengroesse ab",
        answerB = "Power = Wahrscheinlichkeit, einen wahren Effekt zu entdecken (korrekte Ablehnung der Nullhypothese). Sie steigt mit: (1) groesserer Stichprobe, (2) groesserem wahren Effekt, (3) niedrigerem alpha (NEIN: hoeherem alpha/Signifikanzniveau), und (4) niedrigerer Variabilitaet der Messung",
        answerC = "Power = 1 minus der Fehler 1. Art; sie wird nur durch das Signifikanzniveau bestimmt",
        answerD = "Power beschreibt die Repraesentativitaet der Stichprobe fuer die Zielpopulation",
        correctAnswer = 1,
        explanation = "Die Power (1-beta) ist die Wahrscheinlichkeit, einen wahren Effekt statistisch signifikant zu detektieren. Sie haengt ab von: (1) Stichprobengroesse n (groesser = mehr Power), (2) Effektgroesse delta (groesser = mehr Power), (3) alpha (hoeher = mehr Power, aber mehr Fehler 1. Art), (4) Standardabweichung sigma (kleiner = mehr Power). Konventionell wird Power von 80% oder 90% angestrebt.",
        difficulty = 4,
        funFact = "Viele klinische Studien sind unterpowert -- eine Analyse von 2.000 negativen klinischen Studien zeigte, dass ueber 60% nicht ausreichend gepowert waren, um einen klinisch relevanten Effekt zu entdecken. Negative Studien sind oft falsch-negative Studien (Fehler 2. Art)."
    ),

    // Question 43 -- Multiple Testing Problem
    Question(
        categoryId = 16,
        questionText = "Was ist das Problem des multiplen Testens und wie korrigiert die Bonferroni-Methode dafuer?",
        answerA = "Multiples Testen erhoehrt die Power der Studie; Bonferroni-Korrektur ist eine Power-Analyse-Methode",
        answerB = "Bei m unabhaengigen Tests mit alpha = 0,05 steigt die familienweise Fehlerrate (FWER) auf 1-(0,95)^m -- Bonferroni dividiert das Signifikanzniveau durch die Anzahl der Tests (alpha/m), um FWER auf 0,05 zu kontrollieren, auf Kosten der Power",
        answerC = "Bonferroni-Korrektur addiert alle p-Werte und dividiert durch die Testanzahl",
        answerD = "Multiples Testen ist nur bei explorativen Studien ein Problem, nicht bei konfirmativen",
        correctAnswer = 1,
        explanation = "Bei 20 simultanen Tests mit alpha = 0,05 erwartet man rein zufaellig etwa 1 falsch-positives Ergebnis. Die Bonferroni-Korrektur loest dies durch Anpassung: neues alpha = 0,05/20 = 0,0025. Sie ist konservativ (erhoehrt Fehler 2. Art) und eignet sich am besten, wenn Tests vorab geplant wurden. Fuer explorative Analysen werden Methoden wie FDR (False Discovery Rate) bevorzugt.",
        difficulty = 4,
        funFact = "Das Problem des multiplen Testens ist in der Omics-Forschung besonders akut: Genome-Wide Association Studies (GWAS) testen >1 Million SNPs gleichzeitig -- hier hat sich ein Schwellenwert von p < 5x10^-8 als Konvention etabliert (entspricht Bonferroni-Korrektur fuer 1 Million Tests)."
    ),

    // Question 44 -- Surrogate Endpoints
    Question(
        categoryId = 16,
        questionText = "Was ist ein Surrogatendpunkt und welches methodische Risiko birgt seine Verwendung in klinischen Studien?",
        answerA = "Surrogatendpunkte sind sekundaere Endpunkte, die die Praezision der primaeren Endpunkt-Analyse erhoehen",
        answerB = "Ein Surrogatendpunkt ist ein Biomarker der anstelle des eigentlichen klinischen Endpunkts (Mortalitaet, Morbidaet) gemessen wird -- das Risiko: Verbesserungen im Surrogat reflektieren nicht immer klinischen Nutzen, und Surrogate koennen durch Interventionen unerwartete Dissoziation zeigen",
        answerC = "Surrogatendpunkte sind nur in Phase-I-Studien zugelassen und muessen in Phase-III durch harte Endpunkte ersetzt werden",
        answerD = "Surrogate sind statistische Praediktoren des primaeren Endpunkts und immer valide, wenn die Korrelation r > 0,7 betraegt",
        correctAnswer = 1,
        explanation = "Das klassische Risiko: Antiarrhythmika senkten in der CAST-Studie (1989) ventrikulaere Extrasystolen (Surrogat), erhoehten aber paradoxerweise die Mortalitaet (echter Endpunkt). Ein Surrogatendpunkt muss im kausalen Pfad liegen und die gesamte Behandlungswirkung vermitteln -- das ist selten vollstaendig validiert.",
        difficulty = 4,
        funFact = "Die FDA genehmigt Medikamente teilweise beschleunigt auf Basis von Surrogatendpunkten (Accelerated Approval) unter der Bedingung, dass konfirmatorische Studien mit echten Endpunkten nachgereicht werden -- ein regulatorischer Kompromiss zwischen Schnelligkeit und Sicherheit."
    ),

    // Question 45 -- Bradford Hill Criteria
    Question(
        categoryId = 16,
        questionText = "Nach welchen Hauptkriterien von Austin Bradford Hill (1965) kann ein beobachteter Zusammenhang als kausal interpretiert werden?",
        answerA = "Reproduzierbarkeit, Objektivitaet, statistische Signifikanz, Allgemeingueltigkeit und Reversibilitaet",
        answerB = "Zu den wichtigsten Kriterien zaehlen: Staerke der Assoziation, Konsistenz (Reproduzierbarkeit), Spezifitaet, zeitliche Reihenfolge (Temporalitaet), biologischer Gradient (Dosis-Wirkungs), Plausibilitaet, Koehaerenz, experimenteller Nachweis und Analogie",
        answerC = "Bradford Hills Kriterien umfassen ausschliesslich statistische Massnahmen wie Effektgroesse, Konfidenzintervall und p-Wert",
        answerD = "Die Kriterien beschraenken sich auf Temporalitaet, biologische Plausibilitaet und Konsistenz",
        correctAnswer = 1,
        explanation = "Bradford Hill formulierte 1965 neun Kriterien zur Beurteilung von Kausalitaet aus Beobachtungsdaten. Temporalitaet (Exposition vor Erkrankung) ist das einzige obligatorische Kriterium. Die uebrigen sind unterstuetzende Indizien. Die Kriterien wurden im Kontext des Rauchens und Lungenkrebses entwickelt.",
        difficulty = 4,
        funFact = "Bradford Hills Kriterien werden bis heute kontrovers diskutiert -- sie sind kein Algorithmus, sondern Heuristiken. Epidemiologen wie Rothman kritisierten, dass 'Kausalitaet' in der Biomedizin nie bewiesen, sondern nur unterstuetzt werden kann. Die Debatte um Kausalitaet vs. Assoziation bleibt zentral in der Epidemiologie."
    ),

    // Question 46 -- Mendelian Randomization
    Question(
        categoryId = 16,
        questionText = "Was ist Mendelische Randomisierung und welches methodische Problem loest sie im Vergleich zu Beobachtungsstudien?",
        answerA = "Mendelische Randomisierung ist ein genetisches Screening-Verfahren zur Identifizierung von Krankheitsgenen",
        answerB = "Mendelische Randomisierung nutzt genetische Varianten als Instrumental Variables fuer Expositionen -- da Gene zufaellig bei der Meiose verteilt werden, sind sie unabhaengig von Umweltconfoundern und ermoeglicht kausale Effektschaetzung ohne Randomisierung",
        answerC = "Es ist ein Verfahren zur Randomisierung von Studienteilnehmern basierend auf ihrem Genotyp",
        answerD = "Mendelische Randomisierung ist eine Form der Metaanalyse genetischer Epidemiologiestudien",
        correctAnswer = 1,
        explanation = "Genetische Varianten (SNPs) werden von den Eltern zufaellig vererbt (entsprechend dem Mendelschen Gesetz) und sind daher unabhaengig von Lebensstilfaktoren und anderen Confoundern. Wenn ein SNP die Exposition (z.B. Cholesterin) beeinflusst, kann er als Instrumentalvariable genutzt werden, um kausale Effekte zu schaetzen -- aehnlich einem natuerlichen Experiment.",
        difficulty = 4,
        funFact = "Mendelische Randomisierung lieferte starke Evidenz dafuer, dass LDL-Cholesterin kausal Herzinfarkte verursacht -- Personen mit genetisch niedrigem LDL (PCSK9-Mutationen) haben lebenslang weniger Herzinfarkte, proportional zu ihrer Exposition-Reduktion, ohne Confounder-Einfluss."
    ),

    // Question 47 -- Regression to the Mean
    Question(
        categoryId = 16,
        questionText = "Was ist Regression zur Mitte (Regression to the Mean) und warum ist sie in klinischen Studien relevant?",
        answerA = "Sie beschreibt die Tendenz von Studienpopulationen, sich im Zeitverlauf auf den Populationsmittelwert zuzubewegen -- dies kann faelschlicherweise als Behandlungseffekt interpretiert werden, wenn keine Kontrollgruppe vorhanden ist",
        answerB = "Regression zur Mitte ist ein statistisches Artefakt bei der Log-Transformation von Daten",
        answerC = "Es bezeichnet den Fehler, wenn Konfidenzintervalle von Folgestudien breiter werden als die der Ursprungsstudie",
        answerD = "Es beschreibt das Phaenomen, dass extreme Laborwerte durch Messfehler bei Wiederholung naeher am Mittelwert liegen, was aber klinisch nicht relevant ist",
        correctAnswer = 0,
        explanation = "Wenn Patienten wegen extremer Messwerte (z.B. sehr hoher Blutdruck) in eine Studie aufgenommen werden, tendieren ihre Folgemessungen natuerlich zum Mittelwert -- selbst ohne Intervention. Studien ohne Kontrollgruppe koennen diesen natuerlichen Effekt faelschlicherweise als Behandlungseffekt interpretieren. Randomisierte Kontrollgruppen sind der einzige zuverlaessige Schutz.",
        difficulty = 4,
        funFact = "Regression zur Mitte wurde 1886 von Francis Galton bei der Untersuchung der Koerpergroessen von Eltern und Kindern entdeckt -- grosse Eltern haben im Durchschnitt etwas kleinere Kinder als sie selbst, kleine Eltern etwas groessere. Er nannte es 'regression towards mediocrity'."
    ),

    // Question 48 -- Lead Time Bias
    Question(
        categoryId = 16,
        questionText = "Was ist Lead-Time-Bias und warum verfaelscht er Screening-Studien systematisch?",
        answerA = "Lead-Time-Bias entsteht, wenn Screeningprogramme aeltere Patienten bevorzugt rekrutieren und damit das Durchschnittsalter der Studienpopulation erhoehen",
        answerB = "Screening entdeckt Erkrankungen frueher -- wenn die Ueberlebenszeit ab Diagnose gemessen wird, erscheinen Ueberlebenszeiten laenger, auch wenn der Zeitpunkt des Todes unveraendert ist -- der gewonnene 'Lead Time' ist in die Ueberlebenszeit einberechnet",
        answerC = "Lead-Time-Bias beschreibt die Verzoegerung zwischen ersten Symptomen und Diagnosestellung bei Krebserkrankungen",
        answerD = "Er entsteht, wenn Screeningstudien nur Patienten mit positivem Erstbefund weiterverfolgen",
        correctAnswer = 1,
        explanation = "Bei Screening wird eine Erkrankung vor dem klinischen Auftreten entdeckt. Wenn die Ueberlebenszeit ab Diagnose gemessen wird und der Tod zum gleichen Zeitpunkt eintritt, erscheint das Screening durch die fruehere Diagnose einen Ueberlebensvorteil zu bringen -- obwohl kein echter Nutzen vorliegt. Valide Endpunkte sind krankheitsspezifische Mortalitaet oder Gesamtmortalitaet in der Gesamtpopulation.",
        difficulty = 4,
        funFact = "Lead-Time-Bias ist ein Hauptgrund, warum 5-Jahres-Ueberlebensraten bei Krebs nicht zwingend bessere Therapien anzeigen -- fruehere Diagnose durch Screening erhoehrt die 5-Jahres-Ueberlebensrate automatisch, auch ohne Lebenszeitgewinn."
    ),

    // Question 49 -- Length-Time Bias
    Question(
        categoryId = 16,
        questionText = "Was ist Length-Time-Bias (Laengenzeit-Bias) in Screeningstudien?",
        answerA = "Screeningprogramme dauern laenger als geplant und erhoehen damit Studienkosten systematisch",
        answerB = "Langsam wachsende Tumoren (laengere asymptomatische Periode) werden durch periodisches Screening haeufiger entdeckt als schnell wachsende -- Screeningstudien ueberrepraesentieren langsam wachsende, gutartigere Tumoren und erscheinen dadurch effektiver als sie sind",
        answerC = "Laengere Followup-Zeiten fuehren zu systematischer Unterschaetzung von Krebsinzidenzen in Screeningstudien",
        answerD = "Length-Time-Bias entsteht durch unterschiedlich lange Zeitintervalle zwischen Screening-Runden",
        correctAnswer = 1,
        explanation = "Tumoren mit laenger dauernder asymptomatischer Phase (langsames Wachstum) haben eine hoehere Wahrscheinlichkeit, bei einem periodischen Screening entdeckt zu werden. Schnell wachsende, aggressive Tumoren entstehen und werden symptomatisch zwischen Screening-Runden (Intervallkarzinome). Dadurch sieht das Screening in Kohortenstudien effektiver aus als es ist.",
        difficulty = 4,
        funFact = "Length-Time-Bias ist einer der Gruende, warum die Mortalitaetsreduktion durch Mammographie-Screening geringer ausfaellt als fruehere Studien vermuten liessen -- aggressive Tumortypen werden durch Screening nicht ausreichend erfasst, da sie im Intervall zwischen Screening-Untersuchungen auftreten."
    ),

    // Question 50 -- Cochrane Collaboration and Evidence Synthesis
    Question(
        categoryId = 16,
        questionText = "Was ist die methodische Grundlage fuer die Erstellung von Cochrane Reviews und nach welchem Prinzip werden Studien fuer die Synthese ausgewaehlt?",
        answerA = "Cochrane Reviews umfassen nur RCTs aus den letzten 10 Jahren mit Impact-Faktor > 5",
        answerB = "Cochrane Reviews basieren auf vorab registrierten Protokollen (PROSPERO) mit definierten PICO-Kriterien, exhaustiver Mehrsprachiger Datenbanksuche und vordefinierten Ein-/Ausschlusskriterien -- alle Studien werden nach Risiko-of-Bias bewertet und die Evidenzqualitaet mit GRADE eingestuft",
        answerC = "Cochrane Reviews priorisieren grosse multinationale Studien automatisch und schliessen Einzellandstudien aus",
        answerD = "Cochrane Reviews basieren auf narrativer Expertenzusammenfassung ohne formale Suchstrategie",
        correctAnswer = 1,
        explanation = "Cochrane Reviews folgen strengen methodischen Standards: prospektives Protokoll, systematische Suche in Medline, Embase, CENTRAL und anderen Datenbanken, doppelt unabhaengige Selektion und Datextraktion, RoB-Bewertung (Risk of Bias Tool), statistische Synthese wenn sinnvoll, GRADE-Einstufung. Sie gelten als hoechste Evidenzform fuer therapeutische Fragen.",
        difficulty = 4,
        funFact = "Die Cochrane Collaboration hat ueber 10.000 systematische Reviews in der Cochrane Library veroffentlicht. Studien zeigen, dass Aerzte, die Cochrane Reviews konsultieren, signifikant besser informierte Behandlungsentscheidungen treffen als solche, die sich nur auf einzelne Studien oder Expertenmeinung stuetzen."
    )

)

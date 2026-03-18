package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun scienceQuestionsMaster6(): List<Question> = listOf(

    // ── MASTER 6 (difficulty = 5) ── 44 questions ────────────────────────────

    // ── BLOCK 1: Quantum Error Correction (5 questions) ──────────────────────

    // Question 1 – Surface Codes  [correct = A]
    Question(
        categoryId = 2,
        questionText = "Der Surface Code gilt als vielversprechendster Ansatz zur fehlertoleranten Quanteninformatik. Welche geometrische Eigenschaft ermöglicht es dem Surface Code, sowohl Bit-Flip- als auch Phasen-Flip-Fehler unabhängig zu erkennen, und wie hängt die Fehlerkorrekturkapazität mit der Codedistanz d zusammen?",
        answerA = "Der Surface Code codiert ein logisches Qubit auf einem d×d-Gitter physischer Qubits; Bit-Flip-Fehler werden durch Z-Stabilisatoren an Plaquettes erkannt, Phasen-Flip-Fehler durch X-Stabilisatoren an Vertices; der Code toleriert bis zu ⌊(d-1)/2⌋ beliebige Fehler",
        answerB = "Der Surface Code verwendet ein lineares 1D-Gitter aus d physischen Qubits und codiert alle Fehlertypen simultan in der Parität benachbarter Qubits; die Fehlerkorrekturkapazität skaliert linear mit d",
        answerC = "Die geometrische Dualität des Surface Codes zwischen Vertices und Faces entspricht einer algebraischen Toric-Code-Struktur, die ausschließlich topologische Fehler (Anyonen-Paare) erkennt; die Korrekturkapazität beträgt exakt d/2 für gerades d",
        answerD = "Der Surface Code platziert Dataqubits auf Gitterplätzen und Messequbits auf Verbindungen; Bit-Flip-Fehler bilden Loops zwischen Boundaries, Phasen-Flip-Fehler bilden offene Strings; die Fehlerkorrekturkapazität ist unabhängig von d und hängt nur von der Fehlerrate der Messung ab",
        correctAnswer = 0, // A
        explanation = "Der planare Surface Code von Kitaev/Fowler codiert ein logisches Qubit auf einem d×d-Gitter mit d² Dataqubits und ~2d²-1 Messequbits. Z-Stabilisatoren (Plaquettes) messen das Produkt von Z-Operatoren an den vier Eckqubits eines Quadrats und erkennen X-(Bit-Flip-)Fehler. X-Stabilisatoren (Vertices) erkennen Z-(Phasen-Flip-)Fehler analog. Fehler propagieren als Defekte (Syndrome), und der Minimalgewicht-Perfect-Matching-Algorithmus findet die wahrscheinlichste Fehlerkorrektur. Die Codedistanz d entspricht der minimalen Länge einer logischen Fehleroperator-Kette über das Gitter, und der Code toleriert bis zu ⌊(d-1)/2⌋ beliebige Einzelqubit-Fehler. Googles Willow-Chip (2024) demonstrierte erstmals fehlerunterdrücktes Verhalten beim Skalieren von d=3 auf d=5 und d=7.",
        difficulty = 5,
        funFact = "Googles Willow-Quantenchip demonstrierte 2024 erstmals, dass der Surface Code beim Vergrößern des Gitters tatsächlich fehlertoleranter wird — ein zentraler Meilenstein auf dem Weg zum fehlertoleranten Quantencomputer."
    ),

    // Question 2 – Logical Qubits and Fault-Tolerant Threshold  [correct = C]
    Question(
        categoryId = 2,
        questionText = "Die Schwellenwerttheorie (Threshold Theorem) für fehlertolerantes Quantenrechnen besagt, dass beliebig lange Quantenberechnungen möglich sind, wenn die physische Fehlerrate unter einem bestimmten Schwellenwert liegt. Welche Aussage beschreibt die Grundannahmen und Implikationen korrekt?",
        answerA = "Der Schwellenwert liegt für alle bekannten Fehlerkorrekturcodes exakt bei p_th = 1%, und unterhalb dieser Fehlerrate kann jede beliebige Quantenberechnung mit konstant vielen physischen Qubits pro logischem Qubit durchgeführt werden",
        answerB = "Der Schwellenwert hängt nur von der Topologie des Codes ab, nicht von der physischen Implementierung; der Overhead an physischen Qubits pro logischem Qubit ist konstant unabhängig von der gewünschten logischen Fehlerrate",
        answerC = "Das Threshold-Theorem setzt voraus, dass Fehler unkorreliert und unabhängig auftreten; für den Surface Code liegt p_th ≈ 1% bei depolarisierenden Fehlern; unterhalb p_th kann beliebig lange Berechnung mit polynomialem Overhead in physischen Qubits und Gatteroperationen durchgeführt werden",
        answerD = "Das Threshold-Theorem gilt ausschließlich für Stabilisatorcodes; für nicht-stabilisatorbasierte Codes wie topologische Codes gibt es keinen definierten Schwellenwert, da diese auf kontinuierlichen Symmetrien basieren",
        correctAnswer = 2, // C
        explanation = "Das Threshold-Theorem (Aharonov & Ben-Or 1997, Knill et al. 1998) besagt: Wenn die physische Fehlerrate p unter einem codespezifischen Schwellenwert p_th liegt, kann man durch konkatenierende Fehlerkorrektur die effektive logische Fehlerrate beliebig stark unterdrücken. Der Overhead wächst dabei polylogarithmisch in der Zuverlässigkeit. Für den Surface Code liegt p_th ≈ 1% unter realistischen Annahmen (depolarisierende Fehler, fehlerhafte Messungen). Korrelierte Fehler, die typischerweise in realen Systemen auftreten (z.B. durch Crosstalk oder gemeinsame Bäder), können den effektiven Schwellenwert erheblich reduzieren. Aktuelle supraleitende Qubits erreichen Einzelqubit-Gate-Fehlerraten von ~0,1%, liegen also theoretisch unterhalb des Schwellenwerts.",
        difficulty = 5,
        funFact = null
    ),

    // Question 3 – Magic State Distillation  [correct = D]
    Question(
        categoryId = 2,
        questionText = "Stabilisatorcodes wie der Surface Code können Clifford-Gatter (H, CNOT, S) fehlertolerant implementieren, aber nicht das T-Gatter (π/8-Gatter). Welche Methode überbrückt diese Lücke zur universellen fehlertoleranten Quantenberechnung, und warum ist sie ressourcenintensiv?",
        answerA = "Das T-Gatter wird durch direktes Einbringen eines physischen T-Gatters in die Fehlerkorrekturschicht implementiert; dies ist möglich, weil Clifford-Gatter die Fehlerkorrektur nicht stören und das T-Gatter als störungsfreie Perturbation behandelt werden kann",
        answerB = "Das Gottesman-Knill-Theorem beweist, dass T-Gatter für universelles Rechnen nicht benötigt werden; stattdessen kann der Toffoli-Code als Ersatz eingesetzt werden, der direkt aus Stabilisatorgatter-Concatenation entsteht",
        answerC = "Magic State Distillation funktioniert durch Quantenfehlerkorrektur auf Zustandsebene: Ein |T⟩-Zustand wird in einen größeren Stabilisatorcode kodiert, und mehrfache Syndrom-Messungen projizieren den verrauschten Zustand auf den reinen magischen Zustand; der Overhead liegt bei genau 5 physischen magic states pro logischem",
        answerD = "Magic State Distillation (Bravyi & Kitaev 2005) destilliert viele verrauschte magische Zustände |T⟩ = (|0⟩+e^{iπ/4}|1⟩)/√2 zu wenigen hochreinen |T⟩-Zuständen durch Clifford-Kreisläufe; anschließend wird das T-Gatter durch Gatterteleportation konsumiert; die Ressourcenintensität entsteht, weil Hunderte bis Tausende noisy magic states pro distilliertem Zustand benötigt werden",
        correctAnswer = 3, // D
        explanation = "Das Gottesman-Knill-Theorem zeigt, dass Clifford-Kreisläufe klassisch effizient simulierbar sind — sie allein reichen also nicht für universelles Quantenrechnen. Das T-Gatter (oder äquivalent der Toffoli-Gatter) ist nötig für Universalität, liegt aber außerhalb des Stabilisatorformalismus und kann nicht direkt fehlertolerant implementiert werden. Bravyi und Kitaev (2005) lösten dies durch Magic State Distillation: Aus O(k) noisy magic states |T⟩ mit Fehlerrate ε wird 1 reiner |T⟩-Zustand mit Fehlerrate O(ε^k) destilliert. Standardprotokolle (15-zu-1-Destillation) benötigen 15 verrauschte magic states für einen guten. Für ein Quantenprogramm mit 10⁶ T-Gattern und ε=10⁻³ werden Millionen von Destillationsrunden nötig, was den größten Ressourcenanforderungen fehlertoleranter Quantenrechnung entspricht.",
        difficulty = 5,
        funFact = "Schätzungen für einen fehlertoleranten Quantencomputer, der RSA-2048 in 8 Stunden knackt, gehen von über 20 Millionen physischen Qubits aus — fast alle davon für Magic State Distillation."
    ),

    // Question 4 – Color Codes  [correct = B]
    Question(
        categoryId = 2,
        questionText = "Farb-Codes (Color Codes) sind eine Klasse topologischer Quantenfehlerkorrektur-Codes, die auf 3-färbbaren Gittern definiert sind. Welcher entscheidende Vorteil gegenüber dem Surface Code rechtfertigt das Interesse an Farb-Codes trotz ihres höheren Overheads?",
        answerA = "Farb-Codes haben einen höheren Fehlerschwellenwert als Surface Codes (p_th ≈ 2,8% vs. 1%) und sind deshalb die bevorzugte Wahl für Systeme mit hohen Fehlerraten",
        answerB = "2D-Farb-Codes können die gesamte Clifford-Gruppe fehlertolerant durch transversale Gatter implementieren — bei Surface Codes ist nur ein Subset transversal möglich; in 3D-Farb-Codes sind sogar transversale nicht-Clifford-Gatter (T-Gatter) möglich, was den Destillations-Overhead eliminiert",
        answerC = "Farb-Codes verwenden eine geringere Anzahl physischer Qubits als Surface Codes für gleiche Codedistanz, da die 3-Farbbarkeit des Gitters eine kompaktere Encodierung erlaubt",
        answerD = "Farb-Codes sind ausschließlich für ionenfallen-basierte Quantencomputer geeignet, da die 3D-Gatterstruktur nur durch All-to-All-Konnektivität realisiert werden kann; für supraleitende Qubits bieten sie keine Vorteile",
        correctAnswer = 1, // B
        explanation = "2D-Farb-Codes auf hexagonalen oder anderen 3-farbigen Gittern haben den einzigartigen Vorteil, dass alle Clifford-Gatter (H, S, CNOT) transversal implementiert werden können, d.h. durch unabhängige Operationen auf den physischen Qubits ohne Wechselwirkung zwischen Blöcken. Dies ist fundamental für fehlertolerante Gatter, da transversale Gatter keine Fehlerausbreitung zwischen Qubits innerhalb eines Blocks ermöglichen. 3D-Farb-Codes (Bombin & Martin-Delgado, 2006) erlauben zusätzlich transversale T-Gatter, was Magic State Distillation überflüssig macht. Der Preis ist ein höherer Qubit-Overhead und schwierigere physische Realisierung. Aktuelle Forschung (z.B. bei Microsoft und IBM) untersucht Hybrid-Ansätze, die Vorteile von Surface und Color Codes kombinieren.",
        difficulty = 5,
        funFact = null
    ),

    // Question 5 – Concatenated Codes and Logical Error Rate  [correct = A]
    Question(
        categoryId = 2,
        questionText = "Concatenated Codes (konkatinierte Codes) erreichen fehlertolerantes Rechnen durch hierarchische Schachtelung von Fehlerkorrekturen. Wie verhält sich die logische Fehlerrate bei k-facher Konkatenation eines Codes mit physischer Fehlerrate p (unterhalb des Schwellenwerts p_th)?",
        answerA = "Die logische Fehlerrate skaliert als p_L ≈ (1/p_th) × (p/p_th)^{2^k} — doubly-exponentiell mit der Konkatenationstiefe k; der physische Qubit-Overhead wächst als n^k für einen n-Qubit-Basiscode",
        answerB = "Die logische Fehlerrate nach k-facher Konkatenation ist p_L = p_th × (p/p_th)^k, sodass die Verbesserung nur linear mit der Konkatenationstiefe k skaliert",
        answerC = "Bei k-facher Konkatenation gilt p_L ≈ p_th × (p/p_th)^{d^k}, das heißt die Fehlerrate fällt superexponentiell; der Ressourcenaufwand an physischen Qubits wächst jedoch als n^k",
        answerD = "Konkatenierte Codes sind weniger effizient als topologische Codes bei gleichem Qubit-Overhead; die logische Fehlerrate fällt nur polynomiell mit k, was sie für praktische Anwendungen ungeeignet macht",
        correctAnswer = 0, // A
        explanation = "Für einen Basiscode, der Fehler bis Gewicht t korrigiert, gilt bei k-facher Konkatenation: p_L ≈ (1/p_th) × (p/p_th)^{(t+1)^k}. Für den einfachsten Fall (7-Qubit-Steane-Code, t=1) ergibt sich p_L ∝ (p/p_th)^{2^k} — ein doubly-exponentieller Abfall. Der physische Qubit-Overhead wächst als 7^k (Steane) oder n^k allgemein. Der Schwellenwert für den Steane-Code liegt bei etwa p_th ≈ 10^{-4} bis 10^{-3}, abhängig von den Fehlermodell-Annahmen. Topologische Codes wie der Surface Code haben im Vergleich bessere Schwellenwerte (~1%) und geringeren Qubit-Overhead bei gleicher logischer Fehlerrate, weswegen die Forschung sich mehrheitlich auf topologische Codes konzentriert hat. Dennoch sind konkatenierte Codes wichtig für theoretische Beweise und bestimmte physikalische Plattformen.",
        difficulty = 5,
        funFact = "Der erste vollständige Beweis des Threshold-Theorems verwendete konkatenierte Codes und wurde 1996-1997 unabhängig von Aharonov & Ben-Or, Knill & Laflamme & Zurek sowie Gottesman publiziert."
    ),

    // ── BLOCK 2: Epigenetic Aging (5 questions) ──────────────────────────────

    // Question 6 – Horvath Clock  [correct = C]
    Question(
        categoryId = 2,
        questionText = "Steve Horvaths epigenetische Uhr (2013) ist der genaueste bekannte biologische Altersschätzer. Auf welchem molekularen Mechanismus basiert sie, und was macht sie so präzise?",
        answerA = "Die Horvath-Uhr misst die kumulative Anzahl an Telomerverlängerungs- und -verkürzungszyklen in somatischen Zellen; die Präzision entsteht durch Integration über alle Chromosomen, was zufällige Messfehler mittelt",
        answerB = "Horvaths Uhr misst Histon-Modifikationsmuster (H3K27me3 und H3K4me3) an Promotoren von Altersgenen; die genomweite Balance dieser Modifikationen korreliert linear mit dem Alter",
        answerC = "Die Uhr basiert auf DNA-Methylierungsmustern an 353 spezifischen CpG-Stellen im Genom, die über Gewebe hinweg konsistent altern; ein elastisches Netz-Regressionsmodell auf Methylierungsdaten aus Blut, Gehirn, Leber u.a. sagt das chronologische Alter mit mittlerem Fehler von ~3,6 Jahren vorher",
        answerD = "Die Uhr verwendet einen Ensemble-Ansatz aus mRNA-Expressionsprofilen von 500 Altersgenen; die relative Expression dieser Gene im Vergleich zu Stammzellen ergibt einen Altersscore, der gewebespezifisch kalibriert wird",
        correctAnswer = 2, // C
        explanation = "Horvath (2013, Genome Biology) entwickelte die erste pangewebliche epigenetische Uhr aus DNA-Methylierungsdaten von über 8000 Proben aus 51 verschiedenen Geweben. Ein elastisches Netz-Algorithmus selektierte 353 CpG-Stellen, von denen manche mit zunehmendem Alter hyper-, andere hypomethyliert werden. Entscheidend ist, dass diese 353 CpGs gewebetypübergreifend konsistent altern, was eine einzige universelle Formel ermöglicht. Die mittlere absolute Abweichung vom chronologischen Alter beträgt ~3,6 Jahre. 'Biologisches Altern' wird als Differenz zwischen epigenetischem und chronologischem Alter definiert: Positive Differenz (epigenetically older) korreliert mit erhöhter Mortalität, Krebs und altersbedingten Erkrankungen. Neuere Second-Generation-Clocks (GrimAge, PhenoAge) sagen stärker Mortalität und Morbidität vorher.",
        difficulty = 5,
        funFact = "Horvaths Uhr kann das biologische Alter aus einem einzigen Bluttropfen auf ±3,6 Jahre genau bestimmen — präziser als die meisten klinischen Biomarker. Interessanterweise altert das Gehirn epigenetisch langsamer als andere Gewebe."
    ),

    // Question 7 – Rejuvenation by Partial Reprogramming  [correct = D]
    Question(
        categoryId = 2,
        questionText = "Verjüngungsexperimente mit Yamanaka-Faktoren (OSKM) zeigen eine Rücksetzung des epigenetischen Alters in Zellen und Organismen. Welche molekularen Erkenntnisse belegen, dass epigenetisches Altern zumindest teilweise reversibel ist, und welche Vorsichtsmaßnahmen sind bei der partiellen Reprogrammierung nötig?",
        answerA = "Vollständige OSKM-Expression in adulten Zellen setzt das epigenetische Alter auf embryonalen Zustand zurück ohne negative Nebeneffekte; Langzeitexperimente in Mäusen zeigen Lebensverlängerung um 50% ohne Tumorbildung",
        answerB = "Epigenetisches Altern ist irreversibel, da DNA-Methylierungsmuster durch 5-Methylcytosin-Oxidation dauerhaft fixiert werden; OSKM-Faktoren verjüngen Zellen nur phänotypisch durch Genexpressionsveränderungen, nicht durch tatsächliche Methylierungsänderungen",
        answerC = "Verjüngung durch Reprogrammierung funktioniert ausschließlich in postmitotischen Zellen wie Neuronen, da teilende Zellen ihre Methylierungsmuster bei der Replikation verlieren; für proliferierende Zellen sind andere Ansätze wie Senolytika effektiver",
        answerD = "Kurzfristige (partielle) OSKM-Expression kann das epigenetische Alter in Mäusen signifikant reduzieren (Ocampo et al., 2016) und das biologische Alter in Fibroblasten um Jahre zurücksetzen, ohne die zelluläre Identität zu verlieren; vollständige Reprogrammierung riskiert jedoch Tumorigenese (iPSC-Bildung, Entdifferenzierung), weshalb zeitlich kontrollierte Pulse nötig sind",
        correctAnswer = 3, // D
        explanation = "Ocampo et al. (Cell, 2016) zeigten in einem Progeria-Mausmodell mit inducibly exprimierten OSKM-Faktoren (kurze On/Off-Zyklen), dass partielles Reprogrammieren die Gewebefunktion verbessert und Lebensspanne verlängert. Der epigenetische Altersscore sank signifikant. Yang et al. (Cell, 2020) demonstrierten Sehnervregeneration nach Verletzung durch AAV-vermittelte OSK-Expression in Retinazellen — unter Beibehaltung der Zellidentität. Das zentrale Problem: Die vier Yamanaka-Faktoren treiben normalerweise Pluripotenz-Reprogrammierung; unkontrollierte Expression führt zu Teratomen und Entdifferenzierung. Partielle Reprogrammierung mit zeitlicher Kontrolle (2-4 Tage vs. >10 Tage), Auslassung von cMyc oder Verwendung von nur OSK vermeidet diese Risiken. Altos Labs, Calico und andere investieren Milliarden in die klinische Translation.",
        difficulty = 5,
        funFact = null
    ),

    // Question 8 – Cellular Senescence Markers and SASP  [correct = A]
    Question(
        categoryId = 2,
        questionText = "Zelluläre Seneszenz ist ein stabiler Wachstumsstopp, der durch diverse Stressoren induziert wird. Welcher Biomarker-Komplex wird als zuverlässigstes Seneszenz-Identifikationsmerkmal angesehen, und welche Rolle spielt der SASP bei altersbedingten Erkrankungen?",
        answerA = "Der SASP-Komplex (IL-6, IL-8, MMP-3, PAI-1 u.a.) zusammen mit SA-β-Galaktosidase, p16^{INK4a}-Hochregulierung, persistierenden DNA-Schäden-Foci (γH2AX, 53BP1) und SAHF-Bildung bilden den zuverlässigsten Biomarker-Komplex; SASP fördert chronische Entzündung (Inflammaging), Gewebedysfunktion, Krebsprogression und Stammzell-Erschöpfung in benachbarten Zellen durch Parakrin-Signaling",
        answerB = "p21/CDKN1A-Hochregulierung allein ist das spezifischste Seneszenz-Merkmal; der SASP ist ausschließlich vorteilhaft, da er Immunzellen zur Beseitigung seneszenter Zellen rekrutiert",
        answerC = "Einziger zuverlässiger Seneszenz-Marker ist die Telomerlänge unter einem kritischen Schwellenwert von 5 kb; SASP tritt nur in replikativer Seneszenz auf, nicht in stressinduzierten Seneszenzformen",
        answerD = "Seneszente Zellen sind vollständig inert und sezernieren keine Faktoren; ihre einzige messbare Eigenschaft ist der irreversible Zellzyklusarrest durch Rb-Hypophosphorylierung",
        correctAnswer = 0, // A
        explanation = "Zelluläre Seneszenz ist durch multiple Kennzeichen definiert: Permanenter Wachstumsstopp (p16^{INK4a}/Rb und p21/p53-Signalwege), SA-β-Galaktosidase-Aktivität bei pH 6 (lysosomale β-Gal-Überlastung), persistente DNA-Schadensfoci (γH2AX an Telomeren = TAF, telomere-associated foci), SAHF und der SASP. Der SASP umfasst Dutzende von Zytokinen (IL-6, IL-8), Chemokine, Wachstumsfaktoren und MMPs. Seine Rolle ist kontextuell: In Wundheilung und Embryonalentwicklung ist SASP vorteilhaft. Im Alter akkumulieren seneszente Zellen und der chronische SASP treibt Inflammaging. Senolytika (Dasatinib+Quercetin, Navitoclax) eliminieren gezielt seneszente Zellen und verbessern in Tiermodellen Gewebefunktion und Lebensspanne.",
        difficulty = 5,
        funFact = "Seneszente Zellen machen im alten Organismus weniger als 1% aller Zellen aus — aber ihre SASP-Sekretion genügt, um Nachbarzellen in einen dysfunktionalen Zustand zu versetzen. Schon wenige transplantierte seneszente Zellen können bei jungen Mäusen Gebrechlichkeit auslösen."
    ),

    // Question 9 – Telomere Dynamics  [correct = B]
    Question(
        categoryId = 2,
        questionText = "Telomere-Dysfunktion ist einer der Haupttreiber zellulärer Seneszenz und des Alterns. Welche molekulare Kaskade wird durch ein kritisch kurzes oder entkapptes Telomer ausgelöst, und wie unterscheidet sich diese Reaktion von normalen DNA-Doppelstrangbrüchen?",
        answerA = "Kurze Telomere aktivieren direkt den mTOR-Signalweg, der Autophagie supprimiert und zu Protein-Aggregation führt; dieser Mechanismus ist vollständig unabhängig von der DNA-Schadensantwort",
        answerB = "Entkappte Telomere (TRF2-Verlust oder kritische Kürze) werden als Doppelstrangbrüche erkannt: ATM/ATR-Kinasen phosphorylieren H2AX (γH2AX) und aktivieren CHK1/CHK2; im Unterschied zu regulären DSBs können Telomer-Foci nicht durch NHEJ oder HR repariert werden, da Schutzkomplexe wie TRF2/POT1 dies unterdrücken — dies führt zu persistenter DDR-Aktivierung und Seneszenz-Induktion via p53→p21 und p16^{INK4a}→Rb",
        answerC = "Telomerverlust aktiviert ausschließlich den p16^{INK4a}/Rb-Signalweg ohne DNA-Schadensantwort; ATM spielt bei Telomer-induzierter Seneszenz keine Rolle, da Telomere außerhalb der normalen Replikationsdomänen liegen",
        answerD = "Kritisch kurze Telomere führen zur Aktivierung der Telomerase in allen somatischen Zellen, was einen negativen Feedback-Mechanismus darstellt; wenn Telomerase-Aktivierung unzureichend ist, tritt Apoptose (nicht Seneszenz) als primäre zelluläre Antwort auf",
        correctAnswer = 1, // B
        explanation = "Das Shelterin-Komplex (TRF1, TRF2, POT1, TIN2, RAP1, TPP1) schützt Telomere vor der DNA-Schadensantwort. TRF2 unterdrückt ATM-Aktivierung, POT1 unterdrückt ATR. Wenn Telomere kritisch kurz werden oder TRF2 verloren geht, werden sie als Doppelstrangbrüche erkannt. Der entscheidende Unterschied zu normalen DSBs: Der DDR-Fokus an Telomeren persistiert. 53BP1, γH2AX etc. bleiben für Stunden bis Tage stabil (telomere dysfunction-induced foci, TIF). Normale DSBs werden innerhalb von Stunden repariert. Die persistente ATM/CHK2→p53→p21-Aktivierung etabliert den irreversiblen Wachstumsstopp. Gleichzeitig kann p16^{INK4a} hochreguliert werden, was über Rb zu E2F-Repression führt. In Tumorzellen umgeht Telomerase-Reaktivierung diesen Mechanismus — in ~85% der Krebsarten ist Telomerase abnorm aktiv.",
        difficulty = 5,
        funFact = null
    ),

    // Question 10 – GrimAge and Second-Generation Clocks  [correct = C]
    Question(
        categoryId = 2,
        questionText = "Second-generation epigenetische Uhren wie GrimAge und PhenoAge wurden entwickelt, um Mortalität und biologisches Altern besser vorherzusagen als die erste Horvath-Uhr. Welche konzeptuellen Unterschiede bestehen zwischen diesen Generationen, und welche biologischen Prozesse bildet GrimAge ab?",
        answerA = "GrimAge und PhenoAge sind identisch mit der Horvath-Uhr, wurden aber mit größeren Datensätzen trainiert; der einzige Unterschied ist die Anzahl der verwendeten CpG-Stellen",
        answerB = "GrimAge verwendet ausschließlich Methylierungsdaten aus Leukozytensubpopulationen; es wurde entwickelt, um Immunseneszenz zu messen, nicht gesamtes biologisches Altern",
        answerC = "Second-generation Uhren wurden direkt gegen Mortalitäts- und Morbiditäts-Endpunkte trainiert (statt chronologisches Alter); GrimAge schätzt 'Years of Life Lost' durch einen Composite aus DNAm-Surrogaten für Plasmaproteine (PAI-1, GDF-15, Leptin u.a.) und Rauchpackjahre; GrimAge übertrifft alle anderen Uhren in der Mortalitätsvorhersage",
        answerD = "PhenoAge ist älter als die Horvath-Uhr und bildete das Grundmodell; GrimAge ist die neueste Version mit 1000 CpG-Stellen und wurde für forensische Altersbestimmung entwickelt",
        correctAnswer = 2, // C
        explanation = "Lu et al. (Nature Aging, 2019) entwickelten GrimAge durch zweistufiges Training: Zunächst wurden DNAm-Surrogate für 10 Plasmaproteine und Rauchpackjahre trainiert (über Elastic Net Regression), dann wurde ein gewichteter Composite zu einem einzelnen Alters-Score aggregiert, der direkt auf Mortalität trainiert wurde. GrimAge-Acceleration (Differenz zu chronologischem Alter) sagt das Sterberisiko mit HR ~1.3 pro Jahrzehnt vorher — stärker als Horvath-Uhr, PhenoAge oder klassische Risikomarker. Biologisch reflektiert GrimAge hauptsächlich inflammatorische Alterung und Gerinnungsstörungen. Levine et al. (Aging, 2018) entwickelten PhenoAge parallel, trainiert auf klinische Blutbiomarker (Albumin, Kreatinin, CRP etc.) als Proxy. Diese Uhren revolutionieren klinische Trials für Anti-Aging-Interventionen.",
        difficulty = 5,
        funFact = "GrimAge kann vorhersagen, ob jemand in den nächsten 10 Jahren sterben wird — und zwar genauer als die meisten klinischen Tests. Eine Studie zeigte: Menschen, die 10+ Jahre jünger als ihr chronologisches Alter aussehen, haben tatsächlich ein niedrigeres GrimAge."
    ),

    // ── BLOCK 3: Advanced Particle Detectors (4 questions) ──────────────────

    // Question 11 – Silicon Tracking Detectors  [correct = D]
    Question(
        categoryId = 2,
        questionText = "Silizium-Spurdetektoren bilden den innersten Layer großer Teilchendetektor-Experimente wie dem CMS oder ATLAS am LHC. Welche physikalischen Prinzipien und technischen Herausforderungen dominieren das Design des HL-LHC-Upgrades der Inner Tracker (ITk)?",
        answerA = "Silizium-Spurdetektoren nutzen den photoelektrischen Effekt: Teilchen erzeugen Photonen in Silizium, die in Photodioden nachgewiesen werden; die Herausforderung beim HL-LHC ist die hohe Photonen-Flussdichte von über 10^16 Photonen/cm²",
        answerB = "Das Inner Tracker des HL-LHC basiert ausschließlich auf GaAs-Detektoren statt Silizium, da GaAs bei hoher Strahlendosis strahlungshärter ist und höhere Ladungsmobilität hat",
        answerC = "Silizium-Spurdetektoren beim HL-LHC nutzen supraleitende Materialien bei 4K, um Rauschen zu minimieren; die primäre Herausforderung ist die Kryostat-Integration und Wärmeleitfähigkeit",
        answerD = "Geladene Teilchen ionisieren Silizium (Erzeugung von Elektron-Loch-Paaren, ~3,6 eV/Paar); ein angelegtes elektrisches Feld driftet die Ladungsträger zu Elektroden und erzeugt messbaren Strom; beim HL-LHC dominieren extreme Strahlungsschäden (bis 2×10^{16} n_{eq}/cm²), hohe Granularität (50×50 μm² Pixel) und ~200 Pile-up-Ereignisse pro Bunch-Crossing die Design-Anforderungen",
        correctAnswer = 3, // D
        explanation = "Silizium-Streifendetektoren und Pixel-Detektoren nutzen die Halbleitereigenschaft von Silizium: Ein geladenes Teilchen erzeugt entlang seiner Bahn ~80 Elektron-Loch-Paare pro Mikrometer. Bei einer Bias-Spannung (typisch 150-300V) driften diese zur Anode/Kathode und erzeugen einen Strompuls von ~20.000 Elektronen pro Pixel. Das HL-LHC-Upgrade (ab 2029) erhöht die Luminosität 10-fach. Die zentralen Herausforderungen: (1) Strahlungsschäden — Silizium-Kristallgitter wird durch hochenergetische Teilchen beschädigt, was Ladungssammlung reduziert und Leckstrom erhöht; (2) Datenrate — 40 MHz Bunch-Crossing-Rate mit ~200 Pile-up-Ereignissen erfordert On-Sensor-Datenkompression; (3) Granularität — 50×50 μm² Pixel im ATLAS ITk mit ~5 Milliarden Kanälen; (4) Kühlung auf -30°C für Leckstrom-Kontrolle.",
        difficulty = 5,
        funFact = "Der CMS-Spurendetektor beim HL-LHC wird der größte Siliziumdetektor der Welt sein — mit einer aktiven Fläche von etwa 200 Quadratmetern und ~5 Milliarden elektronischen Kanälen."
    ),

    // Question 12 – Hadron Calorimetry and e/h Compensation  [correct = A]
    Question(
        categoryId = 2,
        questionText = "Elektromagnetische und hadronische Kalorimeter messen die Gesamtenergie von Teilchenschauern. Welcher fundamentale Unterschied zwischen EM-Schauern (e±, γ) und hadronischen Schauern (π±, p) macht hadronische Kalorimetrie intrinsisch schwieriger, und was versteht man unter der e/h-Compensation?",
        answerA = "Hadronische Schauer enthalten neben EM-Komponenten (π⁰→γγ) auch eine unsichtbare Energiekomponente (Kernbindungsenergie-Dissipation, Neutrinos aus hadronischen Zerfällen), die nicht kalorimetrisch messbar ist; dies führt zu e/h > 1 (EM-Antwort > hadronische Antwort); kompensierte Kalorimeter (e/h=1) durch spezifische Absorber/Szintillator-Kombinationen (z.B. Pb/Szintillator-Verhältnis) oder dual-readout-Techniken verbessern die Energieauflösung signifikant",
        answerB = "Hadronische Schauer haben eine kürzere Schauerlänge als EM-Schauer und sind daher einfacher einzudämmen; das Hauptproblem ist die hohe Energiedichte, die Szintillatoren zerstört",
        answerC = "Der Unterschied zwischen EM- und hadronischen Schauern liegt ausschließlich in der Schauerlänge (X₀ vs. λ_I); Kalibrierungsprobleme entstehen nur durch Geometrie-Effekte am Rand des Kalorimeters",
        answerD = "e/h-Kompensation bezieht sich auf den Ausgleich zwischen Elektronen- und Hadronenenergien im Beam; kompensierte Kalorimeter verwenden aktive Medien bei kryogenen Temperaturen",
        correctAnswer = 0, // A
        explanation = "Der zentrale Unterschied: EM-Schauer entstehen durch Bremsstrahlung und Paarproduktion — alle Energie wird kalorimetrisch deponiert. Hadronische Schauer haben multiple Komponenten: ~30% EM (durch π⁰→2γ), ~70% hadronisch. Die hadronische Komponente enthält 'invisible energy': Kernbindungsenergie bei Spallation (~1-2 GeV pro Nukleon), Neutrinos aus Pionzerfällen (~10%), Neutronen außerhalb der Akzeptanz. Da unsichtbare Energie statistisch fluktuiert, ist hadronische Energieauflösung (σ/E ~ 50-100%/√E) viel schlechter als EM (σ/E ~ 5-15%/√E). e/h = 1 bei kompensierten Kalorimetern (ZEUS UCAL mit U/Szintillator) durch spezifische Absorberdicken, die Neutronen durch (n,γ) im Szintillator zusätzliche Energie deponieren lassen. Dual-readout-Kalorimeter (Crystal DREAM) messen EM- und hadronische Komponenten simultan durch Szintillations- und Cherenkov-Licht.",
        difficulty = 5,
        funFact = null
    ),

    // Question 13 – Muon Chambers and CMS Trigger  [correct = B]
    Question(
        categoryId = 2,
        questionText = "Myonkammern bilden die äußerste Schicht von Kollisionsdetektoren wie ATLAS. Warum durchqueren Myonen das gesamte Kalorimeter nahezu ungehindert, und wie nutzt das zweistufige CMS-Trigger-System (L1/HLT) diese Eigenschaft zur Ereignisselektion?",
        answerA = "Myonen durchqueren Kalorimeter, weil sie eine positive Ladung tragen, die durch magnetische Felder abgelenkt wird, bis sie das Kalorimeter verlassen; der Trigger selektiert Ereignisse mit maximaler Magnetfeldablenkung",
        answerB = "Myonen (mμ = 105 MeV/c²) sind ~200× schwerer als Elektronen; nach der Bethe-Bloch-Gleichung ist der Energieverlust durch Bremsstrahlung ∝ 1/m², daher verlieren Myonen im Kalorimeter nur ~3 GeV als minimalionisierende Teilchen (MIPs); CMS L1-Trigger analysiert innerhalb von 4 μs Kalorimeter- und Myonsystem-Daten in FPGA-basierter Hardware und reduziert von 40 MHz auf 100 kHz; der HLT führt in 150 ms vollständige Software-Rekonstruktion durch und reduziert auf ~1 kHz für permanente Datenspeicherung",
        answerC = "Myonen wechselwirken nicht elektromagnetisch und werden nur durch die schwache Wechselwirkung gestoppt; Myonkammern sind deshalb Szintillationsdetektoren, die Cherenkov-Strahlung der schwachen Wechselwirkung messen",
        answerD = "Der CMS-Trigger hat nur eine Stufe (L1) und verwendet ausschließlich Myonsystem-Daten; Kalorimeter-Informationen werden erst bei der Offline-Analyse verwendet",
        correctAnswer = 1, // B
        explanation = "Myonen sind Leptonen mit Masse 105,7 MeV/c² (207× Elektronenmasse). Der Energieverlust durch Bremsstrahlung skaliert als ΔE/E ∝ E/m²; für Myonen ist dieser Prozess im LHC-Energiebereich vernachlässigbar. Der dominante Verlustmechanismus ist Ionisation nach Bethe-Bloch (~2 MeV·cm²/g in Eisen), was bedeutet, dass ein GeV-Myon ~10 Wechselwirkungslängen Eisen überquert. Der CMS-L1-Trigger: 40 MHz Bunch-Crossing → FPGA-basierte Pattern-Recognition für Myon-pT, Jet- und Missing-ET-Trigger → Ausgabe in 3,8 μs Latenz auf 100 kHz. HLT: CPU-Farm mit ~30.000 Kernen, 150 ms pro Ereignis, vollständige Rekonstruktion → ~1,5 kHz zu Datenbanken.",
        difficulty = 5,
        funFact = "Der CMS-Detektor allein erzeugt in einer Sekunde Rohdaten, die 1 Petabyte ausfüllen würden — das weltweite LHC-Gitternetzwerk speichert nach der Trigger-Selektion ~25 Petabyte pro Jahr."
    ),

    // Question 14 – b-Tagging with DeepJet  [correct = C]
    Question(
        categoryId = 2,
        questionText = "b-Tagging (Identifizierung von Quark-Jets aus b-Quarks) ist entscheidend für Higgs-Physik und Suche nach neuer Physik am LHC. Welche Eigenschaft von B-Hadronen ermöglicht b-Tagging, und wie nutzen moderne Multi-Variate-Algorithmen (DeepJet) diese Information?",
        answerA = "b-Quarks tragen eine negative elektrische Ladung von -1/3 e, die über Kalorimetrieladungs-Asymmetrie in Jets gemessen wird; DeepJet analysiert die Ladungsverteilung im Jet",
        answerB = "b-Quarks emittieren charakteristische Soft-Photonen durch QCD-Gluon-Strahlung bei niedriger Energie; DeepJet analysiert die Photon-Multiplizität und -Energieverteilung innerhalb eines Jets",
        answerC = "B-Hadronen haben Lebensdauern von ~1,5 ps und zerfallen mit messbaren Flugdistanzen von 1-5 mm; der sekundäre Vertex (SV) liegt nachweisbar versetzt vom primären Vertex (PV); DeepJet nutzt einen Deep-Neural-Network-Ansatz mit Eingaben aus allen Jetspuren (Auftreffparameter d₀/z₀, Vertex-Kandidaten, Jet-Kinematik) und erzielt Effizienz >85% bei Fehlidentifikation von Leichtquark-Jets von ~0,1%",
        answerD = "B-Meson-Zerfälle emittieren immer ein hochenergetisches Myon (semimuonischer Zerfall ist 100%); b-Tagging basiert ausschließlich auf der Myon-Identifikation innerhalb des Jet-Kegels",
        correctAnswer = 2, // C
        explanation = "B-Hadronen (B⁰, B⁺, B_s etc.) enthalten ein b-Quark, das durch den schwachen Zerfall (b→cW⁻) mit charakteristischer Lebensdauer τ ≈ 1,5 ps zerfällt. Der Lorentz-Boost führt bei p_T = 50 GeV zu einer mittleren Flugdistanz Lxy ≈ βγcτ ≈ 3-4 mm — messbar mit Silizium-Pixeldetektoren (Auflösung ~10-15 μm). Der signifikante Auftreffparameter d₀ und sekundäre Vertices sind die primären b-Tag-Signaturen. DeepJet (CMS, 2019) verwendet eine Multi-Input-Architektur: Globale Jet-Features + Tracks + Secondary Vertices + Charged Particle Flow candidates als separate Input-Streams in einem Deep Neural Network. Das Training auf MC-Simulationen liefert eine Working-Point-Effizienz von 68-85% bei 0.1-1% Fehlidentifikationsrate für Leichtjets. b-Tagging ist zentral für H→bb̄-Analysen (größter Higgs-Zerfall, BR~58%).",
        difficulty = 5,
        funFact = null
    ),

    // ── BLOCK 4: Metagenomics (5 questions) ──────────────────────────────────

    // Question 15 – Shotgun Metagenomics  [correct = D]
    Question(
        categoryId = 2,
        questionText = "Schrotschuss-Metagenomik (Shotgun Metagenomics) ermöglicht die taxonomische und funktionelle Charakterisierung mikrobieller Gemeinschaften ohne Kultivierung. Welche bioinformatischen Herausforderungen unterscheiden die Analyse metagenomischer Daten von der Analyse einzelner Genomsequenzen?",
        answerA = "Metagenomische Datensätze sind kleiner als Einzelgenom-Datensätze, da nur die häufigsten Organismen sequenziert werden; die Hauptherausforderung ist die PCR-Amplifikationsbias, die durch biologische Repliken korrigiert wird",
        answerB = "Die Hauptherausforderung metagenomischer Analysen ist der Mangel an Referenzgenomen; alle bioinformatischen Methoden basieren auf vollständigen Referenzdatenbanken, die für Umweltproben nicht existieren; ohne Referenzen ist keine Analyse möglich",
        answerC = "Metagenomische Assembly ist einfacher als Einzelgenom-Assembly, da die höhere Sequenziertiefe durch multiple Organismen Assembly-Fehler statistisch ausgleicht; die Herausforderung liegt ausschließlich in der Datenvolumenverwaltung",
        answerD = "Shotgun-Metagenomik erzeugt ein komplexes Gemisch kurzer Reads aus hunderten bis tausenden von Organismen in unbekanntem Verhältnis; Herausforderungen: (1) Assembly — chimärische Contigs durch ähnliche genomische Regionen verschiedener Spezies; (2) Binning — Zuordnung von Contigs zu Genomen (MAGs) anhand von GC-Gehalt, k-mer-Frequenzen, Tiefe und Co-Abundance; (3) Strain-level-Auflösung — nahe verwandte Stämme sind kaum trennbar; (4) funktionelle Annotation — viele Gene ohne bekannte Funktion",
        correctAnswer = 3, // D
        explanation = "Shotgun-Metagenomik sequenziert die Gesamt-DNA einer Gemeinschaft, produziert aber ein statistisches Gemisch: 50% eines dominanten Organismus, 0,001% eines seltenen Symbionten. Assembly-Programme wie metaSPAdes müssen mit variablen Tiefen umgehen, was chimärische Assemblies durch ähnliche Sequenzregionen produziert. Binning-Algorithmen (MaxBin, MetaBAT2, CONCOCT) nutzen Tetranukleotid-Frequenz-Clustering und differentielle Abundanz über Proben-Zeit-Reihen. Checkm2 bewertet MAG-Completeness und Contamination durch Single-Copy-Gene. Strain-level-Auflösung erfordert Long-Read-Sequenzierung (Oxford Nanopore, PacBio HiFi). Das Human Microbiome Project katalogisierte >10.000 Referenzgenome, aber Bodenproben enthalten Millionen noch unsequenzierter Spezies.",
        difficulty = 5,
        funFact = "Ein einziger Liter Meerwasser enthält schätzungsweise 10 Millionen verschiedene Viren und 1 Million verschiedene Bakterienstämme — die meisten davon sind noch nicht entdeckt, geschweige denn kultiviert worden."
    ),

    // Question 16 – Metagenome-Assembled Genomes (MAGs)  [correct = A]
    Question(
        categoryId = 2,
        questionText = "Metagenom-assemblierte Genome (MAGs) ermöglichen genomische Charakterisierung von Organismen, die nicht kultivierbar sind. Nach welchen Qualitätskriterien werden MAGs klassifiziert, und welche biologischen Grenzen haben MAGs im Vergleich zu Isolat-Genomen?",
        answerA = "MIMAG-Standards klassifizieren MAGs als: High-Quality (≥90% Vollständigkeit, <5% Kontamination, enthält 5S/16S/23S rRNA und ≥18 tRNA), Medium-Quality (≥50% Vollständigkeit, <10% Kontamination), Low-Quality (<50% oder >10%); biologische Grenzen: MAGs repräsentieren Konsensussequenzen über multiple Stämme (Strain-Heterogenität kann Assemblierung verhindern), mobile genetische Elemente fehlen häufig durch Assembly-Schwierigkeiten",
        answerB = "MAGs werden ausschließlich nach ihrer Sequenziertiefe klassifiziert: >50× Tiefe gilt als hochqualitativ (HQ), 10-50× als mittelqualitativ (MQ); biologische Grenzen bestehen nicht, da MAG-Sequenzen identisch mit Isolat-Sequenzen sind",
        answerC = "MAG-Qualität wird ausschließlich durch CheckM-Contamination-Score definiert: <1% Kontamination = HQ, 1-5% = MQ; biologische Grenzen sind primär auf RNA-Viren beschränkt",
        answerD = "Alle MAGs werden als gleichwertig behandelt; das einzige Qualitätskriterium ist die N50-Contig-Länge (>10 kb = HQ); biologische Grenzen bestehen hauptsächlich in der taxonomischen Klassifizierung",
        correctAnswer = 0, // A
        explanation = "MIMAG (Minimum Information about a Metagenome-Assembled Genome, Bowers et al. 2017, Nature Biotechnology) definiert Standards: HQ-MAGs (≥90% Completeness, <5% Contamination, vollständige rRNA-Gene und ≥18 tRNA-Gene) entsprechen Draft-Genome-Qualität. CheckM schätzt Qualität über Single-Copy-Marker-Gene in phylogenetischen Clades. Biologische Grenzen sind real: Strain-Level-Variation in Populationen führt zu 'chimeric bins' die Sequenzen mehrerer nahe verwandter Stämme mischen. Deletionsvarianten und mobile Elemente (Plasmide, Phagen) werden durch Assembly-Schwierigkeiten oft verloren. Long-Read-Metagenomik (HiFi, Nanopore) verbessert MAG-Qualität erheblich. GTDB klassifizierte 2023 >400.000 MAGs taxonomisch, wobei ~30% neue Phyla repräsentieren.",
        difficulty = 5,
        funFact = null
    ),

    // Question 17 – Viral Dark Matter  [correct = B]
    Question(
        categoryId = 2,
        questionText = "Virales 'Dark Matter' bezeichnet virale Sequenzen in Metagenomen, die keine Ähnlichkeit zu bekannten Viren zeigen. Welche bioinformatischen Strategien ermöglichen die Identifikation solcher Sequenzen, und was sagen jüngste Entdeckungen über die tatsächliche Vielfalt der Virosphäre aus?",
        answerA = "Virales Dark Matter wird durch Subtraktions-Metagenomik identifiziert: Alle bekannten Sequenzen werden entfernt, und Reste werden als viral klassifiziert; jüngste Studien zeigen, dass das Virus-Universum vollständig kartiert ist",
        answerB = "Werkzeuge wie VirSorter2, VIBRANT und DeepVirFinder nutzen maschinelles Lernen (k-mer-Frequenzen, Protein-Profil-HMMs, Genomstrukturmerkmale wie Capsid-Assemblierungs-Gene) zur Virus-Identifikation ohne Sequenzähnlichkeit; Studien wie IMG/VR und die Tara Oceans-Studie zeigten, dass >80% der Ozean-Viren keine Ähnlichkeit zu bekannten Viren haben — die globale Virosphäre ist um Größenordnungen diverser als bekannt",
        answerC = "Virales Dark Matter besteht ausschließlich aus defekten Phagen-Sequenzen (DVGs), die entstehen, wenn Phagengenome durch Wirtsmutationen fragmentiert werden; sie haben keine eigene Replikationskompetenz und sind biologisch inert",
        answerD = "Die Identifikation viraler Dark-Matter-Sequenzen ist ausschließlich durch Ultrazentrifugation möglich; Sequenzen ohne bekannte Homologie aus dem Virionen-Kompartiment definieren das virale Dark Matter",
        correctAnswer = 1, // B
        explanation = "Die meisten Viren in Umweltproben sind unbekannt. Klassische Sequenzähnlichkeits-basierte Methoden (BLAST gegen NCBI) finden nur ~20% der viralen Sequenzen in Ozeanproben. Machine-Learning-Methoden nutzen: (1) k-mer-Kompositions-Abweichungen; (2) Protein-Familien-HMMs — Capsid-Proteine, Terminasen als virale Marker; (3) Genomische Kontextmerkmale. IMG/VR v4 (2023) enthält >25 Millionen nicht-redundante virale Sequenzen. Dramatic discoveries: Crass-Phagen, Huge Phagen (>0,5 Mb Genome, entdeckt 2020 in Grundwasser), RNA-Viren in marinen Protisten. Die Tara-Oceans-Studie (Science, 2015) verdoppelte die bekannte virale Sequenzvielfalt. Schätzungen gehen davon aus, dass 10^31 Viruspartikel auf der Erde existieren.",
        difficulty = 5,
        funFact = "2020 wurden 'Huge Phages' entdeckt — Viren mit Genomen über 500.000 Basenpaaren (größer als manche Bakterien), die sogar eigene CRISPR-Systeme und Translations-Maschinerie kodieren. Sie infizieren Bakterien und scheinen Boten zwischen viraler und zellulärer Welt zu sein."
    ),

    // Question 18 – CRISPR Arrays in Metagenomes  [correct = C]
    Question(
        categoryId = 2,
        questionText = "CRISPR-Arrays in Metagenomen dienen als molekulare Immungedächtnisse von Bakterien. Wie werden sie für ökologische und evolutionäre Analysen genutzt, und was ermöglicht 'spacer'-basiertes Virus-Tracking in mikrobiellen Gemeinschaften?",
        answerA = "CRISPR-Arrays können in Metagenomen nicht analysiert werden, da die Wiederholungssequenzen identisch sind und Assembly-Algorithmen sie kollabieren; nur Isolat-Gensequenzierung erlaubt CRISPR-Analyse",
        answerB = "CRISPR-Arrays in Metagenomen werden ausschließlich zur Identifizierung neuer Cas-Proteine verwendet; ökologische Analysen sind nicht möglich, da Spacer-Sequenzen zu kurz für phylogenetische Analysen sind",
        answerC = "Metagenomische CRISPR-Spacer (einzigartige Sequenzen zwischen konservierten Repeats) kodieren virale Protospacer-Sequenzen aus vergangenen Infektionen; durch Spacer-Matching gegen virale Metagenome kann man rekonstruieren, welche Viren welche Bakterien infizierten; zeitliche Spacer-Akquisitions-Raten spiegeln aktuelle Koevolutionsdynamiken wider; Tools wie CRISPRFinder und CRASS ermöglichen systematische Analyse in Metagenomen",
        answerD = "Spacer-basiertes Virus-Tracking zeigt ausschließlich historische Infektionen, die mehr als 1 Million Jahre zurückliegen; aktuelle Koevolutionsdynamiken können nicht erfasst werden, da Spacer-Turnover zu langsam ist",
        correctAnswer = 2, // C
        explanation = "CRISPR-Cas-Systeme speichern kurze virales DNA-Fragmente (Spacer, 20-50 bp) in geordneten Arrays. Neue Spacer werden am 5'-Ende (leader-proximal) eingefügt — das Array ist damit ein chronologisches Protokoll viraler Angriffe. In Metagenomen können CRISPRFinder, PILER-CR und MinCED Arrays in Contigs identifizieren. Da Spacer einzigartig sind (im Gegensatz zu Repeats), können sie assembliert werden. Matching von Spacern gegen gleichzeitig sequenzierte virale Metagenome erlaubt Virus-Host-Zuordnung ohne Kultivierung. Studien zeigen: In Metagenomen aus Salzmarschen können 30-60% der Phagen einem Wirt zugeordnet werden. Zeitreihen-Metagenomik zeigt, wie Spacer-Frequenzen auf akute Phagenausbrüche reagieren (Arms-Race vs. Flu-Cycling-Modell).",
        difficulty = 5,
        funFact = null
    ),

    // Question 19 – Functional Metagenomics  [correct = D]
    Question(
        categoryId = 2,
        questionText = "Funktionelle Metagenomik kloniert Umwelt-DNA-Fragmente in Expressionsvektoren und selektiert auf gewünschte Aktivitäten. Welche Limitierungen der sequenzbasierten Metagenomik überbrückt dieser Ansatz, und welche bekannten Entdeckungen gehen auf diese Methode zurück?",
        answerA = "Funktionelle Metagenomik überbrückt ausschließlich Sequenzierkosten-Limitierungen; wichtigste Entdeckungen sind neue 16S-rRNA-Sequenzen",
        answerB = "Funktionelle Metagenomik ist identisch mit Two-Hybrid-Screen-Methoden; sie testet ausschließlich Protein-Protein-Wechselwirkungen und kann keine Enzymaktivitäten nachweisen",
        answerC = "Die Methode funktioniert ausschließlich für thermophile Organismen, da E. coli als Expressionswirt bei 37°C die meisten mesophilen Gene nicht korrekt exprimieren kann",
        answerD = "Durch Expression fremder Gene in E. coli und Selektion auf Aktivität können Funktionen ohne Sequenzähnlichkeit zu bekannten Genen entdeckt werden; die Methode überbrückt die 'sequence-to-function'-Lücke und ermöglichte Entdeckungen wie neue Antibiotika-Resistenzgene, neuartige Lytische Enzyme und Proteorhodopsin (erstes prokaryotisches Licht-aktiviertes Proton-Pump-Protein, entdeckt 2000 aus Ozean-Metagenom)",
        correctAnswer = 3, // D
        explanation = "Sequenz-basierte Metagenomik versagt bei Genen ohne bekannte Homologie. Funktionelle Metagenomik (Brady Lab, Handelsman Lab) kloniert DNA-Fragmente (5-50 kb) in Cosmide/Fosmide und exprimiert sie in E. coli. Selektion auf Phenotyp: Antibiotikatoleranz, Farbveränderung, Substratabbau. Durchbrüche: Proteorhodopsin (Beja et al., Science 2000) — ein Licht-getriebenes Proton-Pump in marinen Gammaproteobacteria, das lichtgetriebene Primärproduktion in Ozeanen revolutionierte. Neue Antibiotikaresistenz-Gene aus menschlichem Darmmikrobiom. Neue Cellulase-Komplexe aus Termitendarm. Limitierung: E. coli exprimiert nur ~40% heterologer prokaryotischer Gene korrekt; Promotor-Inkompatibilität, Codon-Bias, fehlende Post-translationelle Modifikationen. Lösung: Heterologe Expressions-Hosts (Streptomyces, Bacillus) oder cell-free Expression Systeme.",
        difficulty = 5,
        funFact = "Proteorhodopsin — entdeckt aus einem einzigen Metagenom-Fragment aus dem Pazifik — ist heute das am weitesten verbreitete lichtsammelnde Protein auf der Erde. Es ist in schätzungsweise 1% aller Bakterien auf der Welt vorhanden und verändert unser Verständnis des marinen Kohlenstoffzyklus fundamental."
    ),

    // ── BLOCK 5: Advanced Optics (5 questions) ──────────────────────────────

    // Question 20 – Adaptive Optics  [correct = A]
    Question(
        categoryId = 2,
        questionText = "Adaptive Optik (AO) korrigiert atmosphärische Turbulenz in Echtzeit bei erdgebundenen Teleskopen. Welche Komponenten bilden ein vollständiges AO-System, und wie limitiert der Fried-Parameter r₀ die erreichbare Korrektionsqualität?",
        answerA = "Ein AO-System umfasst: Wellenfrontsensor (WFS, typisch Shack-Hartmann: Linslet-Array das Wellenfrontneigung in Subaperturen misst), deformierbaren Spiegel (DM mit ~500-5000 Aktuatoren) und Echtzeit-Regelung (<1 ms Latenz); der Fried-Parameter r₀ (typisch 10-20 cm bei 500nm, 80 cm bei 2 μm) ist die atmosphärische Kohärenzlänge: r₀ bestimmt die erforderliche Aktuatordichte (N_act ∝ (D/r₀)²) und die Bandbreite (f_G ∝ v_wind/r₀); AO korrigiert im Nahinfrarot effizienter als im Sichtbaren",
        answerB = "AO-Systeme bestehen ausschließlich aus einem deformierbaren Spiegel und einem Rechner; der Fried-Parameter r₀ ist irrelevant, da moderne Algorithmen turbulenzunabhängig korrigieren können",
        answerC = "Der Fried-Parameter beschreibt die zeitliche Kohärenz der Atmosphäre, nicht die räumliche; AO-Systeme korrigieren ausschließlich zeitliche Phasenschwankungen durch Pulskompression; räumliche Variationen erfordern Interferometrie",
        answerD = "Moderne ELTs verzichten auf adaptive Optik, da fortgeschrittene Spiegelpolier-Techniken ausreichende Abbildungsqualität ohne atmosphärische Korrektur liefern",
        correctAnswer = 0, // A
        explanation = "Der Fried-Parameter r₀ (Fried, 1966) charakterisiert räumliche Kohärenz der atmosphärischen Turbulenz. Wellenlängenabhängigkeit: r₀ ∝ λ^{6/5}, daher ist AO bei 2 μm (r₀ ≈ 80 cm, N_akt ≈ 100) viel einfacher als bei 500 nm (r₀ ≈ 15 cm, N_akt ≈ 5000). Greenwood-Frequenz f_G ≈ 0,43 v_wind/r₀ gibt die Korrektur-Bandbreite vor. Das VLT SPHERE-Instrument hat 1377 Subaperturen und 1377 Aktuatoren auf dem DM, liefert Strehl-Ratio >90% im H-Band. Laser-Guide-Stars (LGS, Natrium-D-Resonanz bei 589 nm, Höhe ~90 km) ermöglichen AO ohne hellen Feldstern. Extreme AO für direktes Exoplaneten-Imaging erreicht Kontraste von 10⁻⁶ in Winkelabständen von ~0,1 Bogensekunden.",
        difficulty = 5,
        funFact = "Die adaptive Optik des VLT SPHERE-Instruments schafft es, einen Planeten direkt zu fotografieren, der 10 Millionen Mal dunkler ist als sein Stern — äquivalent dazu, eine Kerze neben einem Leuchtturm aus 100 km Entfernung zu sehen."
    ),

    // Question 21 – Coronagraphs  [correct = B]
    Question(
        categoryId = 2,
        questionText = "Koronagraphen unterdrücken Sternlicht für die direkte Exoplanetenbeobachtung. Welcher fundamentale optische Unterschied besteht zwischen einem Lyot-Koronagraphen und einem Vortex-Koronagraphen, und welche theoretische Kontrast-Grenze setzt der habitable Bereich?",
        answerA = "Lyot- und Vortex-Koronagraphen sind identisch in Funktion; der Unterschied liegt ausschließlich im Herstellungsmaterial der Okultationsmaske",
        answerB = "Der Lyot-Koronagraph blockiert Sternlicht durch eine undurchsichtige Fokalmaske gefolgt von einer Lyot-Stop-Blende in der re-imagierten Pupille; der Vortex-Koronagraph verwendet eine spirale Phasenmaske (Topological Charge l=2) in der Fokalebene, die Sternlicht durch destruktive Interferenz aus der Pupille räumt — theoretisch mit vollständiger Unterdrückung; für Erd-Analoga erfordert der habitable Bereich Kontraste von ~10⁻¹⁰ bei Winkelabständen von ~100 mas (bei 10 pc) — erreichbar nur mit Space-Teleskopen und extremer wavefront control",
        answerC = "Koronagraphen funktionieren ausschließlich im Radiobereich, da optische Interferenz die Unterdrückungseffizienz auf maximal 10⁻⁴ begrenzt",
        answerD = "Der Vortex-Koronagraph verwendet magnetische Felder zur Ablenkung von Sternphotonen; beide Typen erzielen maximal 10⁻⁶ Kontrast",
        correctAnswer = 1, // B
        explanation = "Lyot-Koronagraphen (1939 für Solarkorona entwickelt) blockieren das Beugungsmuster des Sternlichts: Eine kreisförmige Fokalmaske blockiert den zentralen Stern (1-3 λ/D), danach entfernt eine Lyot-Stop-Blende Beugungsringe in der Pupillenebene. Vortex-Koronagraphen (Mawet et al., 2005) nutzen eine spirale Phasenrampe: Ein helicales Wellenfrontstörung e^{ilφ} (l=2) sendet Sternlicht vollständig in äußere Bereiche der Pupille — theoretisch IWA = 0 und totale Unterdrückung für ein perfektes System. Real limitieren Aberrationen (Wavefront-Error <1 nm RMS) und Chromatik den erreichbaren Kontrast. Für ein Erd-Analogon (10⁻¹⁰ Kontrast, 100 mas bei 10 pc) bei 550 nm: Nur das geplante NASA-Habitable Worlds Observatory (HWO, 6-m Apertur, Launch ~2040) kann dies mit aktiven Spiegelkontrollsystemen (<50 pm RMS) erreichen.",
        difficulty = 5,
        funFact = null
    ),

    // Question 22 – Nulling Interferometry  [correct = C]
    Question(
        categoryId = 2,
        questionText = "Nullende Interferometrie kombiniert Teleskope so, dass Sternlicht durch destruktive Interferenz unterdrückt wird, während Planetenlicht konstruktiv überlagert wird. Welches grundlegende Prinzip und welche Herausforderungen prägen den Darwin/TPF-Ansatz für die Detektion erdähnlicher Planeten?",
        answerA = "Nullende Interferometrie basiert auf dem Prinzip, dass Planeten heißer als ihre Sterne sind und daher im Radiobereich heller strahlen; Teleskope werden kombiniert, um Radiowellen-Signale von Planeten zu verstärken",
        answerB = "Die Transmission-Map der nullenden Interferometrie ist identisch für alle Wellenlängen; achromatische Nulls sind trivial durch symmetrische Strahlteilung zu erreichen; die Hauptherausforderung ist die Erdatmosphäre, nicht die Instrumentenstabilität",
        answerC = "Durch phasengenaue Kombination von Teleskopen mit π-Phasenverschiebung zwischen den Armen wird Sternlicht (aus einer Punktquelle auf Achse) destruktiv eliminiert; Planeten in einem Winkelabstand θ_planet = λ/(2B) erzeugen konstruktive Überlagerung; Darwin/TPF-I (Infrarot, 6-17 μm) mit 4-5 Teleskopen auf einem 100-200 m-Baseline-Array erfordert: Phasenstabilität <λ/1000 (~5 nm bei 10 μm), Amplitudenstabilität <10⁻³, aktive Vibrationsisolation und Strahlteiler-Achromasie über breites Spektrum",
        answerD = "Nullende Interferometrie kann ausschließlich am Boden eingesetzt werden, da in der Raumfahrt die Kühlung der Teleskope auf Millikelvin-Temperaturen erforderlich wäre; Darwin und TPF sind Bodenprojekte in der Atacama-Wüste",
        correctAnswer = 2, // C
        explanation = "Bracewell (1978) schlug die nullende Interferometrie vor: Ein Stern im Fokus zweier durch einen π-Phasenschiefer verbundener Teleskope wird vollständig durch destruktive Interferenz ausgelöscht. Ein Planet bei Winkelabstand θ = λ/(2B) erfährt konstruktive Interferenz (maximale Transmission). Darwin (ESA) und TPF-I (NASA) planten 4-5 freifliegende Teleskope (je 3-4 m, B = 50-200 m) im Infrarot, wo biomedizinische Biosignaturen (O₃ bei 9,6 μm, CO₂, H₂O, CH₄) spektroskopierbar sind. Herausforderungen: Nulltiefe > 10⁵ erfordert phasenkohärente Kombination auf <λ/1000 nm; Exozodiakal-Licht setzt Kontrastlimit; Formation Flying mit ~mm-Genauigkeit. Beide Missionen wurden ~2010 aufgeschoben, kehren jetzt als LIFE-Mission (ESA Voyage 2050) zurück.",
        difficulty = 5,
        funFact = "Die Atmosphäre eines 12 Lichtjahre entfernten Erd-Analogen würde im Infrarot etwa 100.000 Mal dunkler als der Stern erscheinen — eine nullende Interferometrie mit 100 m Basis könnte das Signal dennoch in einer Nacht sammeln."
    ),

    // Question 23 – Speckle Imaging  [correct = D]
    Question(
        categoryId = 2,
        questionText = "Speckle-Bildgebung überwindet atmosphärische Turbulenz durch Analyse schneller Kurzzeitbelichtungen. Auf welchem statistischen Prinzip basiert die Speckle-Masking-Methode (Knox-Thompson und Closure Phase), und wie ermöglicht sie beugungsbegrenzte Auflösung?",
        answerA = "Speckle-Bildgebung verwendet einen einzelnen langen Belichtungsframe und trennt atmosphärische Turbulenz durch Fourier-Analyse der Zeitfrequenzen; das Speckle-Muster ist ausschließlich eine chromatische Aberration",
        answerB = "Speckle-Imaging funktioniert nur bei monochromatischem Licht; bei weißem Licht überlagern Speckle-Muster verschiedener Wellenlängen destruktiv und kein Signal bleibt übrig; Breitband-Beobachtungen erfordern Schmalbandfilter <0,1 nm",
        answerC = "Die Closure-Phase-Methode wurde für Radioteleskop-Interferometrie entwickelt und kann nicht auf optische Teleskope angewandt werden, da optische Detektoren keine Phasen-Information direkt messen können",
        answerD = "Bei kurzen Belichtungen (< Kohärenzzeit der Atmosphäre, t < 10 ms) friert die Atmosphäre ein und erzeugt ein Speckle-Muster, das Fourier-Informationen bis zur Beugungsgrenze enthält; Knox-Thompson-Algorithmus: Kreuzkorrelation des Speckle-Leistungsspektrums über viele Frames rekonstruiert Fourier-Amplituden und -Phasen; Closure-Phase-Methode: Aus Dreiecken von Basislinien summieren sich atmosphärische Phasenfehler zu null, was atmosphären-unabhängige Phasenrelationen liefert und beugungslimitierte Bilder ohne AO ermöglicht",
        correctAnswer = 3, // D
        explanation = "Labeyrie (1970) zeigte, dass kurze Belichtungen (1-20 ms) 'eingefrorene' Speckle-Muster erzeugen, deren Leistungsspektrum das wahre Objektspektrum bis zur Teleskop-Beugungsgrenze enthält. Knox-Thompson (1974): Das Kreuzspektrum ⟨Î*(u) · Î(u+Δu)⟩ über viele Frames erhält Phaseninformationen für kleine Δu. Closure Phase: Bei einem Teleskop-Triplet (ABC) ist die Summe der Basislinien-Phasen φ_AB + φ_BC + φ_CA unabhängig von atmosphärischen Phasenfehlern. Moderne optical aperture masking interferometry (AMI) an VLT und Keck verwendet nichtredundante Masken (7-21 Löcher), die Interferenzstreifen verschiedener Basislinien trennen. Anwendungen: Direkte Auflösung von Sternenoberflächen (z.B. Betelgeuse bei 50 mas), nahe Begleiter (brown dwarfs, Planeten), Massensterne in jungen Sternhaufen.",
        difficulty = 5,
        funFact = null
    ),

    // Question 24 – VLTI GRAVITY and Angular Resolution  [correct = A]
    Question(
        categoryId = 2,
        questionText = "Das Very Large Telescope Interferometer (VLTI) mit GRAVITY+-Instrument erreichte Winkelauflösungen von ~10 Mikrobogensekunden bei 2 μm Wellenlänge. Welche fundamentale Physik setzt die Basis-Anforderungen für diese Auflösung, und welche historische Beobachtung machte GRAVITY?",
        answerA = "Beugungslimitierte Winkelauflösung eines Interferometers: θ ≈ λ/(2B) mit Baseline B; für VLTI (B_max = 130 m, λ = 2,2 μm): θ ≈ 3,5 mas; mit GRAVITY und Astrometrie: relative Positionspräzision von ~10 μas; GRAVITY verfolgte S2-Stern um Sgr A* und detektierte relativistische Orbitalvorpräzession (Schwarzschild-Präzession), sowie den ersten direkten Test des General-Relativistic Redshift bei S2-Periastron-Passage (2018)",
        answerB = "Die Auflösung des VLTI basiert auf der Rayleigh-Formel θ ≈ λ/D mit D = Spiegeldurchmesser; GRAVITY beobachtete erstmals einen Asteroideneinschlag auf dem Mars und bestimmte seine chemische Zusammensetzung",
        answerC = "VLTI/GRAVITY erreichte 10 Mikrobogensekunden durch adaptierte Wellenlänge unter 100 nm; GRAVITY beobachtete erstmals die Ereignishorizonstruktur von Sgr A* direkt als aufgelöstes Bild",
        answerD = "Interferometrische Winkelauflösung ist theoretisch unbegrenzt; praktische Limits werden nur durch Detektorrauschen gesetzt; GRAVITY detektierte erstmals ein Exoplanet-Astrometrie-Signal in 1 pc Entfernung",
        correctAnswer = 0, // A
        explanation = "Interferometrie überwindet die Single-Dish-Beugungsgrenze θ ≈ λ/D durch Korrelation von Signalen an räumlich getrennten Teleskopen. Die Fringe-Periode λ/B ist die Auflösung. VLTI kombiniert bis zu 4 Unit Telescopes (8,2 m Ø) oder 4 Auxiliary Telescopes (1,8 m Ø), Baselines 8-130 m. GRAVITY (2016) kombiniert alle 4 UTs kohärent und erreicht astrometrische Präzision von 10-30 μas. 2018: S2-Periastron-Passage nur 120 AU von Sgr A* (4 Mio. Sonnenmassen) → gravitative Rotverschiebung v_grav/c = 0,2% detektiert (Gravity Collaboration, Astronomy & Astrophysics 2018). 2020: Schwarzschild-Präzession des S2-Orbits (Periastron-Drehung von 12'/Umlauf) — erster Test der ART im starken Gravitationsfeld am galaktischen Zentrum. Diese Beobachtungen trugen zum Nobelpreis 2020 für Ghez und Genzel bei.",
        difficulty = 5,
        funFact = "GRAVITY am VLTI ist genau genug, um einem Astronaut auf dem Mond beim Lesen einer Zeitung zuzuschauen — eine Auflösung von 10 Mikrobogensekunden entspricht einem Menschenhaar aus 10.000 km Entfernung."
    ),

    // ── BLOCK 6: Theoretical Ecology (4 questions) ──────────────────────────

    // Question 25 – Neutral Theory of Biodiversity  [correct = B]
    Question(
        categoryId = 2,
        questionText = "Stephen Hubbells Neutrale Theorie der Biodiversität (2001) revolutionierte die theoretische Ökologie. Welche Grundannahmen unterscheiden sie von der klassischen Nischentheorie, und welche empirischen Muster erklärt sie erfolgreich, welche nicht?",
        answerA = "Die Neutrale Theorie nimmt an, dass alle Individuen gleich kompetitiv sind und keine Nischen existieren; sie erklärt ausschließlich tropische Artenvielfalt, scheitert aber bei gemäßigten Klimazonen vollständig",
        answerB = "Die Neutrale Theorie behandelt alle Individuen als trophisch äquivalent (perindividual gleiche Geburts-, Sterbe-, Migrationsraten); Artdiversität entsteht durch stochastische Drift im lokalen Pool + Speziation im Metakommunity-Pool; sie erklärt erfolgreich Species-Abundance-Distributions (SADs), Spezies-Areal-Kurven und β-Diversitätsmuster; scheitert an: Koexistenz nahe verwandter Spezies, phylogenetische Overdispersion in Gemeinschaften, und zeigt zu langsame Artenwechselraten im Vergleich zur Realität",
        answerC = "Die Neutrale Theorie basiert auf der Annahme, dass Spezies in festen Nischen koexistieren und Konkurrenz die einzige treibende Kraft ist; sie ist identisch mit Lotka-Volterra-Gleichungen für Multispecies-Systeme",
        answerD = "Hubbells Theorie erklärt ausschließlich die Artenzusammensetzung von Regenwaldbäumen und ist nicht generalisierbar; sie wurde von Allen et al. (2002) vollständig widerlegt",
        correctAnswer = 1, // B
        explanation = "Hubbell (2001, Monographs in Population Biology) vereinte makroökologische Muster durch eine radikal vereinfachte Annahme: Alle Individuen in einer trophischen Gilde sind per Individuum ökologisch äquivalent (gleiche Fitness). Artenreichtum entsteht durch Drift (zufälliges Sterben, Geburten) + Speziation (Point-Mutation-Mode: ν = Speziationsrate) + Immigration. Das UNTB produziert Vorhersagen für: Fisher's Log-Series (Grenzfall), Preston's Log-Normal SAD, Spezies-Areal-Kurven (S ∝ A^z), β-Diversität zwischen Communities. Empirisch erfolgreiche Anpassungen in Regenwald-Baumgemeinschaften (BCI-Plots, Pasoh). Versagen: Phylogenetische Analysen zeigen Overdispersion (Nischenpartitionierung), nicht Clustering (Drift würde keine Overdispersion erzeugen). Die Theorie ist heute ein Nullmodell, gegen das nischenbasierte Prozesse getestet werden.",
        difficulty = 5,
        funFact = null
    ),

    // Question 26 – Metabolic Theory of Ecology  [correct = C]
    Question(
        categoryId = 2,
        questionText = "Die Metabolische Theorie der Ökologie (MTE, Brown et al. 2004) leitet ökologische Gesetzmäßigkeiten aus der individuellen Metabolismusrate ab. Welche Skalierungsgesetze verbindet sie, und welche Kontroverse umgibt die theoretische Begründung des 3/4-Exponenten?",
        answerA = "Die MTE sagt voraus, dass alle ökologischen Größen mit der Körpermasse M^{1/3} skalieren; der 3/4-Exponent ist ein Näherungswert für Säugetiere, gilt aber nicht für andere Taxa",
        answerB = "MTE erklärt ausschließlich aquatische Ökosysteme, da terrestrische Tiere durch Bewegungsenergie dominiert werden, die nicht von Metabolismus abhängt",
        answerC = "MTE verknüpft individuelle Metabolismusrate B ∝ M^{3/4} e^{-E_a/k_BT} (Körpermassen-Scaling + Arrhenius-Temperaturabhängigkeit) mit Populationsdichte (∝ M^{-3/4}), Wachstumsrate, Sterberate und evolutionären Raten; die theoretische Begründung des 3/4-Exponenten durch fraktale Netzwerkgeometrie (West, Brown, Enquist) ist kontrovers, da empirische Daten von 0,5 bis >1,0 über verschiedene Taxa streuen",
        answerD = "MTE wurde 2010 vollständig falsifiziert durch meta-analytische Studien; der 3/4-Exponent wurde als statistisches Artefakt der log-log-Regression entlarvt",
        correctAnswer = 2, // C
        explanation = "West, Brown & Enquist (Science, 1997) leiteten den 3/4-Exponenten durch fraktale Netzwerk-Optimierung her: Energieversorgung minimiert Transportkosten in einem hierarchisch verzweigten vaskulären Netzwerk. Die MTE-Vorhersagen: B ∝ M^{3/4} e^{-E/kT} (E_a ≈ 0,6-0,7 eV für aerobe Metabolismus). Populationsdichte N ∝ M^{-3/4} (Energieverwendungs-Äquivalenz über alle Körpergrößen). Generationszeit τ ∝ M^{1/4} e^{E/kT}. Brown et al. (Ecology, 2004) erweiterten dies zu einer umfassenden Theorie. Die 3/4-Kontroverse: Dodds et al. (2001) argumentieren für 2/3 (geometrische Skalierung). Glazier (2005) zeigt aktive Tiere nahe 1. Meta-Analysen zeigen Exponent-Variation von 0,5-1,0 über Taxa, abhängig von Aktivitätslevel und Phylogenie. Die theoretische Eleganz macht MTE trotzdem zum einflussreichsten ökologischen Rahmen der letzten 20 Jahre.",
        difficulty = 5,
        funFact = "Nach der Metabolischen Theorie der Ökologie würde die gesamte Energie, die alle Säugetiere auf der Erde verbrauchen, exakt der Energie entsprechen, die alle Insekten verbrauchen — obwohl Insekten 10.000× kleiner sind. Körpermasse und Metabolismus skalieren so, dass sich alles ausgleicht."
    ),

    // Question 27 – Metacommunity Theory  [correct = D]
    Question(
        categoryId = 2,
        questionText = "Die Metakommunity-Theorie integriert lokale Ökologie mit räumlicher Dynamik. Welche vier konzeptuellen Paradigmen unterscheidet Leibold et al. (2004), und wie determinieren Dispersionsrate und Umweltheterogenität, welches Paradigma in einer gegebenen Landschaft dominiert?",
        answerA = "Metakommunity-Theorie unterscheidet zwei Paradigmen: Quelle-Senke-Dynamik (source-sink dynamics) und Masseneffekte (mass effects); Dispersionsrate und Heterogenität sind irrelevant für die Paradigmenselektion",
        answerB = "Metakommunity-Theorie basiert ausschließlich auf der Dispersionsrate und ignoriert Umweltheterogenität; alle vier Paradigmen unterscheiden sich nur in der Dispersionsrate, nicht in ökologischen Prozessen",
        answerC = "Die vier Paradigmen der Metakommunity-Theorie wurden 2010 durch eine Meta-Analyse falsifiziert; aktuelle Metakommunity-Theorie verwendet ausschließlich numerische Simulationsmodelle ohne konzeptuellen Rahmen",
        answerD = "Leibold et al. (2004) unterscheiden: (1) Patch Dynamics (homogene Patches, Kolonisierungs-Extinktions-Dynamik, geringe Dispersionsrate); (2) Species Sorting (heterogene Umwelt, jede Spezies in optimaler Nische — dominiert bei intermediärer Dispersionsrate); (3) Mass Effects (Quellenhabitate subsidieren Senken durch hohe Dispersionsrate, Koexistenz trotz Rangunterschieden); (4) Neutrales Modell (homogene Umwelt, äquivalente Spezies); Übergangsdiagramm: niedrige Dispersionsrate → Patch Dynamics; intermediär → Species Sorting; hoch → Mass Effects",
        correctAnswer = 3, // D
        explanation = "Leibold et al. (Ecology Letters, 2004) synthetisierten verschiedene räumliche Ökologieansätze in einem einheitlichen Rahmen. Species Sorting (Leibold & Mikkelson) ist empirisch am relevantesten: Artengemeinschaften sind durch lokale Nischenbedingungen gefiltert, aber Dispersionsrate muss hoch genug sein für Neubesiedlung nach lokaler Extinktion. Bei zu hoher Dispersionsrate übertrumpft Mass Effects die Nischenselektion: Schlechte Konkurrenten überleben in Senkenhabitaten durch Immigration. Neutral Theory gilt als Grenzfall, wenn α-Diversität rein durch lokale Drift determiniert ist. Moderne Erweiterungen nutzen phylogenetische Community-Struktur, funktionelle Traits und explizite Landschaftsstruktur. Cottenie (2005) Partitioning-Ansatz trennt Environment- vs. Space-Determinanten in Feldstudien.",
        difficulty = 5,
        funFact = null
    ),

    // Question 28 – Body Size Scaling and Trophic Cascades  [correct = A]
    Question(
        categoryId = 2,
        questionText = "Allometrische Skalierungsgesetze verbinden Körpergröße mit fast allen biologischen und ökologischen Eigenschaften. Welche ökologischen Vorhersagen leitet die Size-Efficiency Hypothesis (SEH) und verwandte Theorien für die Struktur aquatischer Nahrungsnetze ab?",
        answerA = "Die Size-Efficiency Hypothesis (Brooks & Dodson, 1965) postuliert: Große Zooplankton-Filtrierer sind effizienter als kleine bei gleichen Ressourcen; Fischprädation auf große Zooplankter führt zu Dominanz kleiner Arten und zur Verschiebung des gesamten Nahrungsnetzes (Trophic Cascade); allometrische Energie-Flussmodelle sagen vor, dass Netto-Energietransfer-Effizienz (~10%) und Körpergrößen-Hierarchie (Predator/Prey-Größenverhältnis ~100:1-10.000:1) universelle Strukturmerkmale aquatischer Nahrungsnetze sind",
        answerB = "Körpergröße hat in aquatischen Systemen keinen Einfluss auf die Nahrungsnetzstruktur; Ernährungsbeziehungen werden ausschließlich durch taxonomische Verwandtschaft determiniert",
        answerC = "Das Size-Efficiency-Modell gilt ausschließlich für marine Systeme; in Süßwasserseen wird die Nahrungsnetzstruktur durch Nährstoffverfügbarkeit, nicht durch Körpergröße determiniert",
        answerD = "Allometrische Skalierung in Nahrungsnetzen bedeutet, dass die Körpermasse aller Organismen auf jeder trophischen Ebene identisch ist; Nahrungsnetze sind perfekt ausbalancierte Pyramiden mit gleichen Biomassen auf jeder Ebene",
        correctAnswer = 0, // A
        explanation = "Brooks & Dodson (1965, Science) zeigten in Manipulationsexperimenten: Einführung planktivorer Fische in Seen erzeugte Verschiebung von großen Cladocera (Daphnia) zu kleinen Zooplankton (Bosmina, Ceriodaphnia) → reduzierte Phytoplankton-Kontrolle → Algenblüten (Trophic Cascade, bestätigt von Carpenter et al. 1985). Allometrische Theorie der Nahrungsnetze: Yodzis & Innes (1992) formulierten Konsumenten-Ressourcen-Modelle mit Körpergrößen-abhängigen Interaktionsstärken. Brose et al. (2006) zeigten, dass Predator-Prey-Körpermassenverhältnisse (PPMR) von ~100 für Invertebraten bis ~10.000 für Vertebraten universelle Energiefluss-Effizienz erzeugen. Diese Erkenntnisse revolutionierten das Verständnis von Seen-Restaurierung (Biomanipulation: Fischeintrag/Entnahme steuert Zooplankton und damit Algenbiomasse).",
        difficulty = 5,
        funFact = "Durch Einführung von nur wenigen Raubfischen in einen nährstoffreichen See kann man eine Algenpest kontrollieren — nicht direkt, sondern durch eine Kaskade: Fische fressen Zooplankton → weniger Grazer → mehr Algen. Oder umgekehrt: Entfernung von Planktivoren führt zu Algenblüte-Kontrolle ohne Chemikalien."
    ),

    // ── BLOCK 7: Advanced Polymer Synthesis (4 questions) ────────────────────

    // Question 29 – RAFT Polymerization  [correct = C]
    Question(
        categoryId = 2,
        questionText = "Reversible Addition-Fragmentation Chain Transfer (RAFT) ist eine kontrollierte radikalische Polymerisationsmethode. Welcher Mechanismus ermöglicht die enge Molmassenverteilung (PDI < 1,2), und wie unterscheidet sich RAFT von klassischer freier radikalischer Polymerisation?",
        answerA = "RAFT verwendet ionische Intermediate statt Radikale; die enge Verteilung entsteht, weil ionische Kettenenden keine Terminierungsreaktionen eingehen; RAFT ist damit identisch mit anionischer Polymerisation",
        answerB = "RAFT funktioniert durch reversible kovalente Bindung des Radikals an einen Katalysator; die enge Verteilung entsteht durch Pulssequenzierung der UV-Belichtung; RAFT kann nur bei kryogenen Temperaturen durchgeführt werden",
        answerC = "Das RAFT-Agens (Dithiocarbonat oder Trithiocarbonat) etabliert ein dynamisches Gleichgewicht: Wachsende Ketten addieren reversibel an die C=S-Bindung → Radikalintermediat fragmentiert zum ruhenden 'macro-RAFT-Agens'; alle Ketten haben gleich lange Aktivzeiten, was zu enger PDI führt; im Gegensatz zu freier Radikalpolymerisation (PDI 1,5-2,0) durch stochastische Terminierung werden bei RAFT Terminierungsreaktionen durch das Gleichgewicht minimiert",
        answerD = "Das RAFT-Agens wirkt als Kettenübertragungsagens, das alle Ketten gleichzeitig startet und gleichzeitig terminiert; die enge Verteilung ist ein thermodynamisches Gleichgewicht zwischen Monomeren und Polymer",
        correctAnswer = 2, // C
        explanation = "RAFT (Chiefari et al., Macromolecules 1998) beruht auf reversiblem Kettentransfer. Das RAFT-Agens (Z-C(=S)-S-R) reagiert mit Propagationsradikalen: Rad• + Z-C(=S)-S-R → Z-C(•)(S-Rad)(S-R) → Z-C(=S)-S-Rad + R•. Dieser Gleichgewichtsmechanismus hält die Konzentration aktiver Radikale niedrig (~10⁻⁸ M) und stellt sicher, dass alle Ketten statistisch gleich lang wachsen. Die Lebend-Charakteristik: Mn = [Monomer]₀/[RAFT]₀ × M_Monomer × Umsatz, PDI → 1+1/DP_n. Vorteile gegenüber ATRP: Kein Übergangsmetall-Katalysator (für Bio-Anwendungen wichtig), breite Monomer-Kompatibilität (Acrylate, Methacrylate, Styrol, Acrylamide, Vinylester). Anwendungen: Block-Copolymere, Sternpolymere, Poly-NIPAM für Thermosensoren, PEG-Konjugate für Nanomedicine.",
        difficulty = 5,
        funFact = null
    ),

    // Question 30 – ATRP and eATRP  [correct = D]
    Question(
        categoryId = 2,
        questionText = "Atom Transfer Radical Polymerization (ATRP) ist die am meisten verwendete kontrollierte Polymerisationsmethode. Welche Rolle spielt das Übergangsmetall-Redox-Paar, und wie lösten eATRP und SARA-ATRP das Problem des hohen Metallkatalysator-Anteils?",
        answerA = "In ATRP dient das Übergangsmetall (Cu, Fe, Ru) als Photoinitiator; beim Lichtabsorption werden Radikale erzeugt; eATRP verwendet elektrischen Strom statt Licht als Energiequelle — der Mechanismus bleibt sonst identisch",
        answerB = "ATRP verwendet Mangan-Verbindungen als essentiellen Cofaktor; ohne Mn²⁺ ist keine ATRP möglich; eATRP ist eine Variante, die statt Mn ein elektrochemisch regenerierbares Fe-ATRP-Agens verwendet",
        answerC = "ATRP-Katalysatoren werden bei der Polymerisation vollständig verbraucht; der hohe Metallanteil ist inhärent und kann nicht reduziert werden; eATRP und SARA-ATRP sind nicht für industrielle Anwendungen geeignet",
        answerD = "In ATRP aktiviert Cu(I)/L das Alkylhalogenid-Initiator durch Einelektron-Oxidation zum Cu(II)/L-X-Komplex + Radikal; Cu(II) deaktiviert das Radikal reversibel (ATRP-Gleichgewicht K_ATRP = k_act/k_deact); eATRP regeneriert Cu(I) elektrochemisch durch Reduktion von Cu(II), was Katalysatormengen von ppm statt 10³ ppm ermöglicht; SARA-ATRP verwendet Cu(0)-Draht/Pulver als Oberflächenreduzens für kontinuierliche Cu(I)-Regeneration",
        correctAnswer = 3, // D
        explanation = "ATRP (Matyjaszewski & Wang, JACS 1995; Kato et al. JACS 1995) basiert auf dem Gleichgewicht: P_n-X + Cu(I)L ⇌ P_n• + Cu(II)(X)L mit K_ATRP = 10⁻⁸ bis 10⁻⁶ je nach Initiator und Ligand. Da K_ATRP klein ist, überwiegen ruhende Ketten (~10⁶ : 1 gegenüber aktiven), was Terminierungsreaktionen minimiert. Problem: Klassisches ATRP braucht 10.000-50.000 ppm Cu, was biomedizinische Anwendungen schwierig macht. eATRP (Bortolamei, Matyjaszewski, 2011): Elektrochemische Zelle regeneriert Cu(I) aus Cu(II) kontinuierlich; <50 ppm Cu ausreichend. SARA-ATRP (Percec, 2012): Cu(0)-Oberflächen disproportionieren zu Cu(I) + Cu-bulk und halten Cu(I)-Konzentration stabil. Diese Methoden ermöglichten ATRP in Wasser für Protein-Polymer-Konjugate, Biomedizin und Scale-up-Produktion.",
        difficulty = 5,
        funFact = "ATRP wird heute genutzt, um einzelne Proteine mit präzisen Polymer-Ketten 'aufzupfropfen' — für gezielte Krebstherapien, bei denen ein Wirkstoff-Polymer-Konjugat nur in Tumorzellen aktiviert wird."
    ),

    // Question 31 – Stereoselective ROP and sc-PLA  [correct = A]
    Question(
        categoryId = 2,
        questionText = "Ringöffnungs-Polymerisation (ROP) von Lactiden und ε-Caprolacton erzeugt biologisch abbaubare Polyester (PLA, PCL). Welche Katalysatorsysteme ermöglichen stereoselektive ROP von rac-Lactid zu stereokomplexem (sc-)PLA, und warum ist das bedeutsam?",
        answerA = "Koordinations-Insertions-ROP mit stereoselektiven Metallkatalysatoren (z.B. C₂-symmetrische Al-Salen-Komplexe von Coates; Zr-Binol-Katalysatoren) polymerisieren rac-Lactid zu iso- oder syndiotaktischem PLA mit hoher Isotaktizität (Pm > 0,9 durch enantiomorphe Site Control); stereokomplexes PLA (sc-PLA) aus 1:1-Mischung iso-PLLA + iso-PDLA hat Schmelzpunkt 230°C (vs. 180°C Homopolymer) durch Stereocomplex-Kristallisation — wichtig für Hochtemperaturanwendungen biologisch abbaubarer Kunststoffe",
        answerB = "ROP von Lactid verwendet ausschließlich saure Katalysatoren (HCl, H₂SO₄) bei hohen Temperaturen (>200°C); Stereoselektivität ist bei ROP nicht möglich, da Lactid keine prochiralen Zentren hat",
        answerC = "Stereoselektive ROP erfordert ausschließlich enzymatische Katalyse durch Lipasen; chemische Katalysatoren können keine Stereoselektivität in zyklischen Estern erzielen; sc-PLA wird durch post-polymerisations-Epimerisierung hergestellt",
        answerD = "Alle ROP-Produkte aus rac-Lactid sind ataktisch und amorph; Stereoselektivität in Lactid-Polymerisation ist physikalisch nicht möglich, da das freie Monomer racemisiert bevor es polymerisiert",
        correctAnswer = 0, // A
        explanation = "Coates et al. (JACS, 1998) entwickelten C₂-symmetrische Aluminium-Salen-Komplexe für hochisotaktische PLA-Synthese aus rac-Lactid. Mechanismus enantiomorphe Site Control: Die chirale Metallumgebung diskriminiert zwischen D- und L-Lactid, sodass bevorzugt ein Enantiomer eingebaut wird (Isotaktizität). Alternativ ermöglicht Chain-End-Control Syndiotaktizität. Quantitative Messung durch Homonuklear-entkoppeltes ¹H-NMR: Pm (probability of meso-diads) = Isotaktizitätsparameter. Stereokomplexes PLA (Ikada et al., 1987): iso-PLLA und iso-PDLA bilden beim Mischen 3:1-Helix-Komplexe → Schmelzpunkt 230°C vs. 180°C der Homopolymere → verbesserte Thermomechanik, Hydrolyseresistenz und Transparenz. Bedeutung: PLA ist der am schnellsten wachsende bioabbaubare Kunststoff (>750.000 t/Jahr), sc-PLA für Hochtemperatur-Anwendungen (Automobil, medizinische Implantate).",
        difficulty = 5,
        funFact = null
    ),

    // Question 32 – Living Anionic Polymerization  [correct = B]
    Question(
        categoryId = 2,
        questionText = "Lebende anionische Polymerisation erlaubt die Synthese von Block-Copolymeren mit exakt definierten Architekturen. Welche Anforderungen an Reinheit und Reaktionsbedingungen stellt diese Methode, und welche modernen Amphiphil-Strukturen werden damit für biomedizinische Anwendungen synthetisiert?",
        answerA = "Anionische Polymerisation kann unter Luftatmosphäre in Wasser durchgeführt werden; sie erzeugt PDI > 2,0 durch ionische Nebenreaktionen; Blockcopolymere sind durch anionische Polymerisation nicht zugänglich",
        answerB = "Anionische Polymerisation erfordert absolut wasserfreie, sauerstofffreie Bedingungen (<1 ppm H₂O, Inert-Gas-Atmosphäre in sorgfältig gereinigten Glasgefäßen), da Protonen und Sauerstoff die lebenden Carbanionen terminieren; charakteristisch ist PDI ≈ 1,01-1,10 und Mn = [Monomer]₀/[Initiator]₀ × M_Monomer; Block-Copolymere entstehen durch sequentielle Monomer-Addition; biomedizinisch relevante Strukturen: PEO-b-PLA, PEO-b-PCL Nano-Mizellen für hydrophobe Wirkstoffkapselung, Polymer-Vesikel (Polymersomen), sternartige PEG-b-PS für DNA-Transfektion",
        answerC = "Anionische Polymerisation ist ausschließlich auf Styrol und Diene anwendbar; Acrylate, Methacrylate und Lactone können nicht anionisch polymerisiert werden; Blockcopolymere werden ausschließlich durch Kupplungsreaktionen synthetisiert",
        answerD = "Die lebende anionische Polymerisation erfordert Temperaturen unter -196°C (flüssiger Stickstoff) für alle Monomere; bei Raumtemperatur ist keine Kontrolle möglich, da thermische Energie die lebenden Kettenenden terminiert",
        correctAnswer = 1, // B
        explanation = "Lebende Polymerisation (Szwarc, 1956, Nature) ist durch drei Merkmale definiert: keine Terminierung, keine Übertragung, Initiierung schneller als Propagation → PDI → 1 + 1/DP_n. Anionische lebende Polymerisation mit BuLi in THF/Benzol: n-BuLi initiiert Polystyrolkette (PS-Li⁺); Zugabe von 2. Monomer (z.B. Isopren) bildet PS-b-PI. Reinheitsanforderungen: H₂O (<1 ppm) terminiert Carbanionen zu C-H; CO₂ bildet Carboxylat; O₂ oxidiert. Typische Reinigung: Dest. von THF über Na/Benzophenon, Vakuumtransfer aller Lösungsmittel. Block-Copolymere mit anionischer Polymerisation: Kraton (PS-b-PI-b-PS, Thermoplastische Elastomere), SBS. Biomedizin: PEO-b-PCL Polymersomen (Discher et al., 2002, Science) als robuste Membran-Vesikel für Protein-Kapselung; PEG-b-PLGA für kontrollierten Wirkstofffreisetzung.",
        difficulty = 5,
        funFact = "Lebende anionische Polymerisation wurde 1956 von Michael Szwarc entdeckt, als er beobachtete, dass anionische Polystyrolketten auch nach vollständigem Monomerverbrauch reaktionsfähig blieben — sie warteten förmlich auf mehr Monomer. Das führte zum Konzept der 'lebenden' Polymere."
    ),

    // ── BLOCK 8: Geodynamics (4 questions) ──────────────────────────────────

    // Question 33 – Mantle Rheology  [correct = C]
    Question(
        categoryId = 2,
        questionText = "Das Erdmantelgestein verhält sich je nach Zeitskala entweder elastisch, viskos oder plastisch. Welche Deformationsmechanismen dominieren in verschiedenen Manteltiefen, und wie beeinflusst Wassergehalt im Olivin die effektive Viskosität?",
        answerA = "Der Erdmantel ist vollständig inkompressibel und isotrop; Deformation erfolgt ausschließlich durch duktile Strömung bei konstanter Viskosität von 10²¹ Pa·s; Wassergehalt hat keinen Einfluss auf Viskosität",
        answerB = "Olivin im Erdmantel deformiert ausschließlich durch Sprödbruch bis 410 km Tiefe; unterhalb der 410-km-Diskontinuität tritt flüssiges Gestein auf, das keine Scherspannung überträgt",
        answerC = "Im oberen Mantel (<410 km) dominiert Dislokations-Kriechen (power-law creep, ε̇ ∝ σⁿ mit n≈3-5 in Olivin) mit Foliation-Bildung (LPO); im unteren Mantel (>660 km, Perovskit/Post-Perovskit) dominiert Diffusions-Kriechen (newtonsches Fließen, n=1); Wassergehalt (Hydroxyl-Defekte in Olivin-Kristallgitter) reduziert Diffusions-Koeffizienten drastisch: 'wet' Olivin hat 1-3 Größenordnungen niedrigere Viskosität als trockenes Olivin bei gleicher Temperatur",
        answerD = "Diffusions-Kriechen dominiert im gesamten Mantel; Dislokations-Kriechen ist nur in Laborbedingungen bei extremen Deformationsraten beobachtbar; Wassergehalt erhöht die Viskosität durch Verstärkung der Kristallgitterstruktur",
        correctAnswer = 2, // C
        explanation = "Mantelrheologie ist komplex und tiefenabhängig. Dislokationskriechen (Hirth & Kohlstedt, 2003): ε̇ = A σⁿ d⁻ᵐ f_{H₂O}^r exp(-E*/RT) mit n≈3,5 für trockenes Olivin. Diese nichtlineare Rheologie führt zu Stresskonzentration und LPO (kristallographische Vorzugsorientierung) — beobachtbar durch seismische Anisotropie. Unter 410 km: Phasenübergang Olivin→Wadsleyit; unter 660 km: Mg-Perovskit + Ferroperiklas (postspinell). Diffusionskriechen n=1 (Newton'sch), korngrößenabhängig. Wassergehalt: Karato (1990) zeigte, dass Wasserstoff als Punktdefekt (OH-) in Olivin-Kristallgitter eingebaut wird (bis 100-800 H/10⁶ Si) und die Diffusion von Si und O beschleunigt. 'Wet' Olivin hat η ≈ 10¹⁸⁻¹⁹ Pa·s vs. 10²¹ Pa·s trocken bei 1300°C.",
        difficulty = 5,
        funFact = null
    ),

    // Question 34 – Plate Driving Forces and Slab Rollback  [correct = D]
    Question(
        categoryId = 2,
        questionText = "Der Antrieb der Plattentektonik ist seit Jahrzehnten kontrovers. Welche Kräfte dominieren quantitativ den Plattenantrieb, und wie erklärt der Mechanismus des 'Slab Rollback' die Entstehung von Backarc-Becken?",
        answerA = "Reibungskräfte zwischen Platten und Mantel (Mantelschlepp) sind der dominierende Antriebsmechanismus; Subduktionsplatten spielen eine passive Rolle und werden nur mitgezogen; Backarc-Becken entstehen durch vulkanische Intrusion",
        answerB = "Mantelplumes (Hotspots wie Hawaii) sind der primäre Antrieb der Plattentektonik; Ridge Push und Slab Pull sind vernachlässigbare Sekundärkräfte; Backarc-Becken entstehen durch erhöhten Wärmefluss aus dem Kern",
        answerC = "Alle Plattenkräfte sind gleich stark und heben sich zu Null auf; Platten bewegen sich durch astronomische Gezeitenkräfte; Slab Rollback ist ein theoretisches Konzept ohne beobachtbare Auswirkungen",
        answerD = "Slab Pull (negativer Auftrieb der kühlen, dichten subduzierten Platte) ist der dominierende Antrieb (~3× stärker als Ridge Push); Slab Rollback tritt auf, wenn sich der Subduktionstrencher rückwärts bewegt, weil der Slab senkrecht sinkt und die Platte nicht schnell genug nachrollt; dies erzeugt Extension im Überplattenbogen → Backarc-Rifting → Backarc-Spreading-Center (z.B. Mariana Trog, Japan See)",
        correctAnswer = 3, // D
        explanation = "Forsyth & Uyeda (1975) quantifizierten Plattenkräfte: Slab Pull (F_SP ~ -30 × 10¹² N/m entlang Subduktionszone) überwiegt deutlich Ridge Push (F_RP ~ 2-3 × 10¹² N/m). Evidenz: Schnelle Platten (Pazifik, Philippinen) haben lange Subduktionszonen; langsame (Eurasien, Antarktis) haben wenig Subduktion. Slab Rollback (Garfunkel et al. 1986): Wenn ein Slab senkrecht in den Mantel sinkt, wandert der Trencher in Richtung der ozeanischen Platte (Rollback). Die überreitende Platte wird gedehnt. Im Westen des Pazifik: Philippine Sea Plate, Japan Sea (25 Ma), Mariana Trough (aktives Backarc-Spreading, ~45 mm/Jahr). Tomographie zeigt rollback-konforme Slab-Geometrien. Slab Suction (Mantelströmung in Richtung des Slabs) kann zusätzlich Extension erzeugen.",
        difficulty = 5,
        funFact = "Der Pazifik schrumpft jedes Jahr um mehrere Zentimeter, weil ozeanische Kruste an seinen Rändern subduziert wird — gleichzeitig wächst der Atlantik durch Seafloor-Spreading am Mittelatlantischen Rücken. In 250 Millionen Jahren wird es möglicherweise wieder einen einzigen Superkontinent geben: Pangäa Proxima."
    ),

    // Question 35 – Triple Junction Stability  [correct = A]
    Question(
        categoryId = 2,
        questionText = "Tripel-Verbindungen (Triple Junctions) sind Punkte, wo drei tektonische Platten zusammentreffen. Welche geometrischen Stabilitätsbedingungen formulierte McKenzie & Morgan (1969), und welche der drei häufigsten Typen (RRR, RTF, TTT) sind typischerweise stabil?",
        answerA = "McKenzie & Morgan (1969) entwickelten eine graphische Stabilitätsanalyse auf Grundlage von Plattenbewegungsvektoren: Eine Triple Junction ist stabil, wenn ein gemeinsamer Treffpunkt der drei Plattenbewegungslinien in der Geschwindigkeitstafel existiert; RRR (Ridge-Ridge-Ridge) ist immer stabil (Afrikanische Triple Junction bei Afar ist ein Beispiel); TTT ist nur stabil, wenn alle drei Trencher geometrisch konvergieren (wie bei Japan)",
        answerB = "Alle Triple-Junction-Typen sind geometrisch stabil; McKenzie & Morgan zeigten, dass Stabilität ausschließlich von der Plattengröße abhängt; RRR-Verbindungen sind immer instabil",
        answerC = "Triple Junctions können nur aus drei gleichen Plattenrand-Typen bestehen (RRR, FFF, TTT); gemischte Typen (RFT etc.) sind physikalisch unmöglich und nicht beobachtbar",
        answerD = "Die Stabilität von Triple Junctions wird ausschließlich durch Mantelkonvektion unter der Verbindung bestimmt; geometrische Vektoranalyse ist unzureichend, da sie Viskositätseffekte ignoriert",
        correctAnswer = 0, // A
        explanation = "McKenzie & Morgan (1969, Nature) analysierten Triple Junctions durch kinematische Vektoranalyse. Jede Plattengrenze hat eine charakteristische Geschwindigkeit (Ridge: Spreizungsvektor; Fault: Translation; Trencher: Konvergenzvektor). In der 'Velocity Space' (Hodograph) muss die Triple Junction einem Schnittpunkt der Geschwindigkeitslinien aller drei Grenzen entsprechen, damit sie stabil ist. RRR (Afar Tripel Junction, Galapagos): Immer stabil, da drei Spreizungsvektoren immer gemeinsamen Punkt haben. RTF (Ridge-Transform-Fault): Oft stabil, häufigster Typ. TTT: Speziell — Japan ist ein bekanntes Beispiel (Pazifik, Philippinen, Eurasien). Stabile Triple Junctions bewegen sich in der Ebene; instabile verändern ihren Charakter (z.B. von RTF zu RFR). Diese Analyse war revolutionär für das Verständnis der Komplexität der Plattenrand-Evolution.",
        difficulty = 5,
        funFact = null
    ),

    // Question 36 – Post-Perovskite Transition and D″ Layer  [correct = B]
    Question(
        categoryId = 2,
        questionText = "Die Post-Perovskit-Phasenübergang in MgSiO₃ bei ~125 GPa und ~2500 K wurde 2004 entdeckt. Welche seismologischen Anomalien erklärt er, und welche geodynamischen Konsequenzen hat er für Mantelkonvektion?",
        answerA = "Post-Perovskit hat keine seismologischen Konsequenzen, da der Phasenübergang nur bei Temperaturen auftritt, die im realen Erdmantel nicht existieren; die D″-Schicht wird durch chemische Heterogenität erklärt",
        answerB = "Post-Perovskit (ppv, MgSiO₃, CaIrO₃-Struktur) bildet sich bei Bedingungen der D″-Schicht (~2600-2900 km Tiefe); es erklärt: (1) Seismische Diskontinuität am D″-Horizont (V_S-Geschwindigkeitssprung, beobachtbar als globale Diskontinuität ~250 km über CMB); (2) Anti-Korrelation zwischen S-Wellen und P-Wellen-Anomalien; geodynamisch: ppv-Übergangszone wirkt als thermischer 'Ventil' — kühle Subduktionsplatten treffen den ppv-Übergang tiefer, was lokale Auftriebsunterschiede und Mantel-Heterogenität verstärkt",
        answerC = "Post-Perovskit wurde nicht experimentell bestätigt; der Begriff bezeichnet eine hypothetische Phase, die in Diamond-Anvil-Cell-Experimenten durch verunreinigte Proben vorgetäuscht wurde",
        answerD = "Die D″-Schicht besteht aus erstarrtem äußeren Kern-Material; Post-Perovskit ist die korrekte Bezeichnung für dieses erstarrte Material, das ein metallisches Glas ist",
        correctAnswer = 1, // B
        explanation = "Murakami et al. (Science, 2004) entdeckten durch Laser-Heiz-Diamantstempel-Experimente (LH-DAC) den ppv-Übergang in MgSiO₃ bei 125 GPa/2500 K. Die CaIrO₃-Struktur (orthorhombisch) hat andere elastische Eigenschaften als Perovskit: Positive dV_S/dP-Ableitung und negative dV_S/dT über dem Übergang. Dies erklärt die seismische D″-Diskontinuität (Lay-Helmberger 1983): Velocitätssprung von ~+1-3% in V_S ~250 km über dem CMB. Anti-Korrelation S-P in D″: In heißen Regionen (LLSVPs) ist ppv zurück zu Perovskit transformiert → niedrige V_S, hohe V_P durch kompensatorischen thermischen Effekt. Geodynamisch: ppv-Rinne als lokaler Wärmewiderstand, Mantelplume-Initiierung begünstigt. CMB-Topographie (bis ±10 km) durch ppv-Phasenfront kontrolliert.",
        difficulty = 5,
        funFact = "Die Entdeckung der Post-Perovskit-Phase 2004 war eine der größten Überraschungen der Geophysik der letzten Jahrzehnte — sie löste ein 20 Jahre altes Rätsel über mysteriöse seismische Signale aus der tiefsten Mantelschicht, die niemand erklären konnte."
    ),

    // ── BLOCK 9: Advanced Photochemistry (4 questions) ────────────────────

    // Question 37 – Singlet Oxygen Generation  [correct = C]
    Question(
        categoryId = 2,
        questionText = "Singulett-Sauerstoff (¹O₂) ist ein reaktiver Sauerstoff-Zustand, der in Photodynamischer Therapie (PDT) und organischer Synthese genutzt wird. Welcher Mechanismus führt zur ¹O₂-Bildung durch Sensibilisatoren, und wie unterscheidet sich Typ-I von Typ-II-Photosensibilisierung?",
        answerA = "Singulett-Sauerstoff entsteht ausschließlich durch direkte UV-Absorption durch O₂; Sensibilisatoren spielen keine Rolle; Typ-I und Typ-II sind identische Prozesse mit verschiedenen Bezeichnungen",
        answerB = "Typ-I PDT erzeugt ¹O₂ direkt durch Photolyse von H₂O₂; Typ-II PDT erzeugt ausschließlich •OH-Radikale durch Fenton-Reaktion; Sensibilisatoren dienen als Photokatalysatoren für diese Wasserlyse",
        answerC = "Bei Typ-II PDT: Sensibilisator (PS) absorbiert Photon → S₁ → ISC → T₁ (langer Triplett-Lebensdauer, μs-ms); T₁-PS überträgt Energie auf ³O₂ (Grundzustand-Triplett) → ¹O₂ + PS-Grundzustand; ¹O₂ reagiert selektiv mit elektronenreichen Biomolekülen (Tryptophan, Cys, Guanin); Typ-I PDT: T₁-PS reagiert mit Substrat durch Elektronen/Protonentransfer → Radikale, die O₂ zu O₂•⁻ reduzieren → H₂O₂ → •OH; unter Hypoxie ist Typ-I effektiver",
        answerD = "Singulett-Sauerstoff kann nicht in biologischen Systemen erzeugt werden, da Proteine und Lipide als Quencher wirken und ¹O₂-Lebensdauer auf Femtosekunden reduzieren; PDT nutzt ausschließlich ³O₂",
        correctAnswer = 2, // C
        explanation = "Spin-verbotene direkte Anregung von O₂ (³Σg⁻ → ¹Δg) ist schwach (ε ~ 0,5 M⁻¹cm⁻¹). Sensibilisatoren ermöglichen effiziente ¹O₂-Bildung durch Triplett-Energietransfer (TET). ISC schwerer Atome (Halogenierung, Metallkomplexe wie Porphyrin-Zn²⁺) begünstigt T₁-Populierung. ¹O₂-Quantenausbeute (Φ_Δ): Porphyrine ~0,6-0,8, Chlorin e6 ~0,64, Rose Bengal ~0,86. In Wasser: τ(¹O₂) = 3-4 μs; in biologischem Gewebe ~0,1-0,3 μs durch Quenching durch Proteine/Lipide. Typ-I vs. Typ-II hängt von O₂-Konzentration und Substratreduktionspotential ab. Photofrin (hämatoporphyrin-Derivat) war erstes klinisch zugelassenes PDT-Photosensibilisator (1993). Second-generation PS: Temoporfin (m-THPC), Visudyne (Verteporfin) für Makuladegeneration.",
        difficulty = 5,
        funFact = null
    ),

    // Question 38 – Photoredox Catalysis  [correct = D]
    Question(
        categoryId = 2,
        questionText = "Photoredox-Katalyse nutzt angeregte Zustände von Übergangsmetallkomplexen oder organischen Farbstoffen als Einelektron-Reduktions/Oxidationsmittel. Wie ermöglicht der Iridium-Photoredox-Katalysator [Ir(ppy)₂(dtbbpy)]PF₆ die Aktivierung von C-H-Bindungen, und welchen Beitrag leistete MacMillan für die asymmetrische organische Synthese?",
        answerA = "Photoredox-Katalyse verwendet UV-Licht zur direkten Photolyse von C-H-Bindungen; Iridium dient als Träger der Photonen, nicht als Elektronentransfer-Mediator",
        answerB = "Photoredox-Katalyse ist identisch mit klassischer Elektrochemie; Licht dient nur als Energiequelle, die in elektrischen Strom umgewandelt wird; Iridium-Komplexe sind Ersatz für Elektroden",
        answerC = "MacMillans Beitrag beschränkte sich auf die Verwendung von Iridium als Katalysator statt Ruthenium; inhaltlich wurde keine neue Chemie entwickelt; asymmetrische Photoredox-Katalyse war bereits vor 2008 bekannt",
        answerD = "Bei Photoredox-Katalyse: [Ir]²⁺-Komplex absorbiert sichtbares Licht → [Ir]²⁺* (MLCT, stärkeres Oxidans und Reduktans als Grundzustand); MacMillan et al. (Science 2008) kombinierten Photoredox mit Enamin-Katalyse zur asymmetrischen α-Alkylierung von Aldehyden durch Radikal-Enamin-Kupplung — erste direkte enantioselektive Photoredox-Synthese; das Konzept 'dual catalysis' (photoredox + Organokatalyse/Ni-Katalyse) eröffnete neue Syntheserouten für die pharmazeutische Industrie",
        correctAnswer = 3, // D
        explanation = "Metallkomplex-Photosensibilisatoren wie [Ru(bpy)₃]²⁺ und [Ir(ppy)₂(dtbbpy)]⁺ absorbieren sichtbares Licht und erreichen langlebige angeregte MLCT-Zustände (τ > 1 μs). Im angeregten Zustand sind sie gleichzeitig stärkere Oxidans und Reduktans als im Grundzustand durch Umkehr beider Redoxpotentiale. MacMillan & Nicewicz (Science, 2008): Kombination von [Ir(ppy)₃] (Photoredox) mit sekundärem Amin-Enamin-Katalysator (Imidazolidinon). Mechanismus: Imin-Amin kondensiert mit Aldehyd → Enamin. Photoredox-reduziertes [Ir]⁰ gibt Elektron an α-Brommalonate → Radikal. Radikal addiert an Enamin → α-alkylierten Aldehyden (ee > 90%). 2008 explodierte dieses Gebiet: Ni/Photoredox für C-N, C-O, C-C Kupplungen, C-H-Aktivierung, Decarboxylierung. MacMillan erhielt 2021 Nobelpreis für asymmetrische Organokatalyse.",
        difficulty = 5,
        funFact = "MacMillans Photoredox-Katalyse traf auf so großes Interesse in der pharmazeutischen Industrie, dass Merck & Co. eine Multi-Millionen-Dollar-Partnerschaft mit seiner Gruppe einging — und mehrere Arzneimittelkandidaten nun durch Photoredox-Schlüsselschritte hergestellt werden."
    ),

    // Question 39 – Photoswitches  [correct = A]
    Question(
        categoryId = 2,
        questionText = "Molekulare Photoschalter ermöglichen reversible Licht-induzierte Strukturveränderungen. Welche photochemischen Mechanismen liegen der E/Z-Isomerisierung von Azobenzen und der elektrozyklischen Ringschluss-Reaktion von Diarylethenen zugrunde, und wie unterscheiden sich ihre Eigenschaften für Anwendungen?",
        answerA = "Azobenzen (n-π* und π-π*-Absorption): Trans (E, planar, stabil) → UV-Bestrahlung → Cis (Z, gewinkelt, metastabil, thermisch rückschaltend in Dunkelheit — T-type Schalter); Diarylethene (z.B. Dithienylethylen): photochemisch reversibler Ringschluss — beide Isomere sind thermisch stabil ('P-type' Schalter); Diarylethene schaltbar auch mit sichtbarem Licht durch Push-Pull-Substitution; Anwendung: Diarylethene für Datenspeicherung, Azobenzen für optogenetische Kontrolle von Biomolekülen",
        answerB = "Azobenzen und Diarylethene haben identische Schaltmechanismen durch Radikal-Photolyse; beide reagieren nur mit UV-Licht und sind für Anwendungen mit sichtbarem Licht ungeeignet",
        answerC = "Diarylethene sind ausschließlich P-type-Schalter mit thermischer Stabilität; Azobenzen ist ausschließlich T-type; beide können nur im Festkörper, nicht in Lösung geschaltet werden",
        answerD = "Der Ringschluss von Diarylethenen ist irreversibel; die 'Rückschalt'-Reaktion erfordert chemische Reagentien, nicht Licht; Azobenzen ist ausschließlich durch thermische Relaxation zurückschaltbar und kann nicht durch Licht in die trans-Form zurückgebracht werden",
        correctAnswer = 0, // A
        explanation = "Azobenzen-Photochemie: Franck-Condon-Anregung in S₂ (π-π*, 320 nm) oder S₁ (n-π*, 430 nm, schwach) → Rotation um N=N-Bindung oder Inversion → Cis-Isomer (τ_thermal ~ 1 h bis Jahre, je nach Substituenten). Azobenzene mit ortho-Fluorsubstitution (Hecht-Gruppe) schalten mit sichtbarem Licht und haben verlängerte Cis-Halbwertszeiten. Biomolekuläre Anwendungen: Fotoschaltbares Penicillin (Trauner Lab), lichtgesteuerter Ionenkanal (SPARK-Rezeptoren). Diarylethene (Irie, 1990): Konrotatorischer elektrozyklischer Ringschluss in der hexatriensystemartigen offenen Form unter UV → geschlossene konjugierte Form (Absorption ~550-700 nm, gefärbt). Ringöffnung mit sichtbarem Licht. Lebensdauer: >10⁴ Schaltzyklen, thermische Stabilität >80°C. Anwendungen: Holografische Datenspeicher, molekulare Logikgatter, Photopharmakologie.",
        difficulty = 5,
        funFact = null
    ),

    // Question 40 – Marcus Theory and Inverted Region  [correct = B]
    Question(
        categoryId = 2,
        questionText = "Die Marcus-Theorie des Elektronentransfers beschreibt die Ratenabhängigkeit von Reorganisationsenergie und Triebkraft. Was besagt der 'inverted region'-Effekt, und welche experimentelle Bestätigung lieferten Miller, Calcaterra und Closs (1984)?",
        answerA = "Die Marcus-Theorie sagt voraus, dass Elektronentransfer-Rate mit zunehmender Triebkraft (|ΔG°|) immer monoton steigt; der 'inverted region' ist ein theoretisches Konzept, das experimentell widerlegt wurde",
        answerB = "Marcus-Theorie: k_ET = (4π²/h) |H_DA|² (4πλk_BT)^{-1/2} exp[-(ΔG°+λ)²/(4λk_BT)]; optimale Rate bei ΔG° = -λ; für |ΔG°| > λ ('inverted region') sinkt die Rate trotz größerer Triebkraft, da die Franck-Condon-Überlappung zwischen Ausgangs- und Produktpotential gering wird; Miller et al. (1984, JACS) synthethisierten starre Donor-Brücken-Akzeptor-Moleküle (Biphenyl-Brücken) und maßen k_ET über 8 Dekaden |ΔG°| — erste klare Demonstration der Glocken-Kurve mit invertierter Region",
        answerC = "Die invertierte Region tritt nur in protischen Lösungsmitteln auf, weil Protonenübertragung den Elektronentransfer-Mechanismus bei großen Triebkräften ändert; in aprotischen Lösungsmitteln gilt ausschließlich lineare Marcus-Theorie",
        answerD = "Miller, Calcaterra und Closs verwendeten Radioaktivitäts-Messungen für den Nachweis der invertierten Region; Elektronentransfer-Raten können nicht direkt gemessen werden und müssen durch Zerfallsprodukt-Quantifizierung indirekt bestimmt werden",
        correctAnswer = 1, // B
        explanation = "Marcus (1956, JCP) leitete aus parabolischen Potentialenergiekurven ab: Die Aktivierungsenergie ΔG‡ = (ΔG°+λ)²/(4λ). Für schwache Triebkraft (|ΔG°| < λ): ΔG‡ sinkt mit |ΔG°| (normale Region). Optimum: ΔG‡ = 0 bei ΔG° = -λ. Jenseits: Inverted Region — ΔG‡ steigt wieder, k_ET sinkt. Miller, Calcaterra & Closs (1984, JACS) lösten das Problem durch rigide Donor-Brücken-Akzeptor-Moleküle (keine diffusions-kontrollierte Kinetik, definierter Abstand). Durch Variation der Akzeptoren (8 verschiedene) über |ΔG°| = 0,5-2,5 eV wurde die Glocken-Kurve eindeutig gezeigt. Diese Entdeckung ist relevant für: Photosynthese (Ladungsseparation optimiert nahe λ, Rekombination in invertierter Region gehemmt), Solarzellen, Langstrecken-Elektronentransfer in Proteinen. Marcus erhielt 1992 den Chemienobelpreis.",
        difficulty = 5,
        funFact = "Die invertierte Marcus-Region erklärt, warum die Photosynthese so effizient ist: Die primäre Ladungstrennung (gewünschte Reaktion) findet im Normal-Bereich optimal statt, während die schädliche Ladungsrekombination in der invertierten Region liegt — und daher viel langsamer abläuft, als man naiv erwarten würde."
    ),

    // ── BLOCK 10: Cognitive Genomics (4 questions) ──────────────────────────

    // Question 41 – GWAS for Intelligence  [correct = C]
    Question(
        categoryId = 2,
        questionText = "Genomweite Assoziationsstudien (GWAS) für kognitive Fähigkeiten haben Tausende von Common Varianten identifiziert. Was beschreibt das Konzept der 'missing heritability', und wie viel der phänotypischen Varianz in allgemeiner Intelligenz (g) wird durch SNPs erklärt?",
        answerA = "GWAS für Intelligenz ist methodisch nicht möglich, da kognitive Fähigkeiten keine genetische Grundlage haben; alle Heritabilitätsstudien wurden durch Konfundierung widerlegt",
        answerB = "SNP-Heritabilität für g beträgt ~95%, was Familien-Heritabilitätsschätzungen vollständig erklärt; 'missing heritability' ist ein obsoletes Konzept; GWAS hat alle genetischen Varianten für Intelligenz bereits vollständig kartiert",
        answerC = "Familien-/Zwillingsstudien schätzen Heritabilität von g auf ~50-80%; GWAS-SNP-Heritabilität (h²_SNP, alle Common Variants >1% MAF) erklärt ~25-35% der phänotypischen Varianz; 'missing heritability' = Differenz zwischen Familien- und SNP-Heritabilität; mögliche Erklärungen: seltene Varianten (<1% MAF), Dominanz/Epistasis, Genotyp-Umwelt-Interaktion; Savage et al. (2018, Nature Genetics) GWAS mit 1,1 Mio. Individuen identifizierte >1200 unabhängige Signalstellen",
        answerD = "GWAS-Studien für Intelligenz haben keine reproduzierbaren Befunde geliefert; alle publizierten Loci wurden in Replikationsstudien widerlegt; Intelligenz ist zu komplex für genetische Analyse",
        correctAnswer = 2, // C
        explanation = "Kognitive Fähigkeiten zeigen robuste Heritabilitäten in Zwillingsstudien: h² ≈ 50% im Kindesalter, >80% im Erwachsenenalter (genomweite Regressionsanalysen, GCTA). GWAS-SNP-Heritabilität ist geringer, weil Common SNPs nicht alle kausalen Varianten perfekt taggen. Lee et al. (Nature Genetics, 2018) GWAS Education Attainment mit 1,1 Mio. Individuen: 1.271 unabhängige Loci, R² ~ 11% für Bildung, ~3% für kognitive Leistung — jeder Locus hat winzig kleinen Effekt (β < 0,02 SD). Polygenic Score (PGS) aus 10^6 SNPs erklärt ~12-15% der phänotypischen Varianz in Testdaten. 'Missing heritability'-Hypothesen: (1) Rare variants; (2) Gemeinsame Varianten in LD ohne genotypisierte Loci; (3) Epistasis kaum nachweisbar bei aktuellem Powerbereich. GWAS revolutionierte Verständnis von g als polygene Eigenschaft mit tausenden von kleinen additiven Beiträgen.",
        difficulty = 5,
        funFact = "Der Polygenic Score für Bildungserfolg aus GWAS-Daten erklärt mehr Varianz in Schulnoten als ein einzelnes Gen jemals könnte — aber er sagt doch nur ~12% der Unterschiede vorher. Das bedeutet: Genetik ist wichtig, aber Umwelt, Familie und Zufall spielen eine noch größere Rolle."
    ),

    // Question 42 – Within-Family GWAS and Genetic Nurture  [correct = D]
    Question(
        categoryId = 2,
        questionText = "Bildungsjahre (Educational Attainment, EA) sind der am besten untersuchte Proxy für kognitive Fähigkeiten in GWAS. Welche methodischen Herausforderungen entstehen durch Stratifikation und assortatives Mating, und was zeigen Within-Family-GWAS im Vergleich zu Population-Level-GWAS?",
        answerA = "Population-stratification ist kein Problem für GWAS, da alle modernen Studien Blut-basierte Proben verwenden; assortatives Mating verstärkt genetische Signale und erhöht statistische Power; Within-Family-Studien sind methodisch schwächer",
        answerB = "Within-Family-GWAS zeigen identische Effektgrößen wie Population-Level-GWAS; assortatives Mating hat in Bildungs-GWAS keinen Einfluss, da es ausschließlich phänotypisches Assortieren, keine genetische Korrelation erzeugt",
        answerC = "Population-Stratifikation wurde durch ancestry-informative markers vollständig gelöst; alle modernen GWAS haben keine Stratifikationsprobleme mehr; Within-Family-Designs sind ausschließlich für seltene Erkrankungen geeignet",
        answerD = "Population-Stratifikation: Geografische/ethnische Unterschiede in Allelfrequenzen korrelieren mit Unterschieden in Bildung durch sozialen Kontext → falsch-positive GWAS-Signale; Korrektur durch PCA-Hauptkomponenten. Assortatives Mating für Bildung (r ~ 0,4-0,5) führt zu Inflation von additiver SNP-Heritabilität. Within-Family-GWAS (Kong et al. 2018, Science: 'genetic nurture'): Elterliche Allele, die nicht ans Kind weitergegeben wurden, korrelieren dennoch mit Kind-Bildung durch Genotyp-Umwelt-Korrelation (rGE); direkte genetische Effekte im Kind sind kleiner als scheinbare Population-Effekte",
        correctAnswer = 3, // D
        explanation = "Kong et al. (Science, 2018) genotypisierten 21.637 isländische Familien-Trios und berechneten 'nicht-übertragene Allele' (NTA = elterliche Allele, die nicht ans Kind gingen). Der Polygenic Score aus NTA korrelierte signifikant mit Kind-Bildung — trotz keiner direkten genetischen Weitergabe. Diese 'Genetic Nurture' erklärt, warum Population-GWAS-Effekte ~30% größer sind als direkte kindliche Effekte. Mechanismen: Eltern mit hohem PGS für Bildung kaufen mehr Bücher, sprechen mehr, haben höheres Einkommen → rGE (passive Genotyp-Umwelt-Korrelation). Stratifikations-Korrektur: PCA (Price et al. 2006) korrigiert für ancestry-assoziierte Allele, aber soziale Stratifikation innerhalb ethnischer Gruppen bleibt. Assortatives Mating: r_EA-EA ≈ 0,4 erhöht LD zwischen Bildungs-SNPs → Inflation von h²_SNP.",
        difficulty = 5,
        funFact = null
    ),

    // Question 43 – Gene-Environment Correlation (rGE)  [correct = A]
    Question(
        categoryId = 2,
        questionText = "Gen-Umwelt-Korrelation (rGE) beschreibt, wie genetische Dispositionen Umwelterfahrungen beeinflussen. Welche drei Typen unterscheidet Scarr & McCartney (1983), und wie verändert sich ihre relative Bedeutung über die Lebensspanne nach dem Modell der 'Gene-Environment Correlation Cascade'?",
        answerA = "Scarr & McCartney (1983, Child Development) unterscheiden: (1) Passive rGE: Eltern mit hohem IQ-Genotyp schaffen kognitiv anregendes Umfeld für Kinder durch positive Korrelation zwischen eigenem Genotyp und selbst-gestaltetem Umfeld; (2) Evokative rGE: Individuum-Eigenschaften (z.B. Neugier, genetisch beeinflusst) lösen Umweltreaktionen aus (Lehreraufmerksamkeit, Peerselektion); (3) Aktive rGE: Individuen wählen aktiv Umgebungen passend zu ihrem Genotyp (Nischenselektion); Lebensspann-Wandel: Im Kindesalter dominiert passive rGE; im Erwachsenenalter nimmt aktive rGE zu, weshalb Heritabilität mit Alter steigt",
        answerB = "rGE hat nur einen Typ: die passive Korrelation durch genetisch verwandte Eltern; aktive und evokative Korrelationen wurden 2005 widerlegt; rGE spielt keine Rolle bei kognitiver Entwicklung im Erwachsenenalter",
        answerC = "Alle drei rGE-Typen werden gleich wichtig über die gesamte Lebensspanne; es gibt keinen systematischen Entwicklungstrend; die Unterscheidung der Typen ist rein begrifflich ohne empirische Konsequenzen",
        answerD = "Gen-Umwelt-Korrelation ist synonym mit Gen-Umwelt-Interaktion (GxE); Scarr & McCartney beschrieben ausschließlich GxE-Prozesse; der Begriff 'rGE' als eigenständiges Konzept hat nichts mit dem 1983-Modell zu tun",
        correctAnswer = 0, // A
        explanation = "Scarr & McCartney's Modell ist fundamental für Verhaltensgenetik. Passive rGE: Genetisch verwandte Eltern-Kind-Paare teilen Allele UND Umwelt. Evokative rGE: Ein Kind mit hohem Sprachgeschick erhält mehr Feedback, Lesebücher, Förderung — unabhängig von den Eltern. Aktive rGE: Mit zunehmendem Alter wählen Individuen Umgebungen, Berufe, Partner und Freizeitaktivitäten, die zu ihrem Genotyp passen. Empirisch: Tucker-Drob & Bates (2016, Psychological Science) Meta-Analyse: Heritabilität des IQ steigt von ~40% im Kindesalter auf ~80% im Erwachsenenalter; Einfluss geteilter Umwelt sinkt von ~25% auf ~0% — konsistent mit zunehmendem aktiven rGE und sinkender passiver rGE. Implikation: 'Genetischer Determinismus' ist falsch — Gene beeinflussen, welche Umwelterfahrungen gemacht werden, nicht direkt die kognitive Leistung.",
        difficulty = 5,
        funFact = "Eine Studie mit adoptierten Kindern zeigte: Im Kleinkindalter gleicht der IQ dem der Adoptiveltern an (passive rGE durch deren Umwelt), aber bis zum Erwachsenenalter gleicht er sich dem der biologischen Eltern an (aktive rGE durch eigene Nischenwahl) — selbst ohne jeden Kontakt zu ihnen."
    ),

    // Question 44 – Pleiotropy and LD Score Regression  [correct = B]
    Question(
        categoryId = 2,
        questionText = "Genetische Pleiotropie — ein Allel beeinflusst mehrere phänotypische Merkmale — ist in kognitiver Genomik allgegenwärtig. Welche Methode erlaubt es, genetische Korrelationen zwischen Intelligenz, psychischen Erkrankungen und Persönlichkeitseigenschaften zu schätzen, und was zeigen die Befunde über die biologische Organisation kognitiver Merkmale?",
        answerA = "Pleiotropie in kognitiven Merkmalen kann nur durch direkte Gensequenzierung nachgewiesen werden; GWAS-Methoden sind zu ungenau um Überlappungen zwischen phänotypischen Domänen zu detektieren",
        answerB = "LD Score Regression (Bulik-Sullivan et al. 2015, Nature Genetics) schätzt genetische Korrelation r_g aus GWAS-Zusammenfassungsstatistiken: r_g(Intelligenz, Bildung) ≈ 0,70; r_g(Intelligenz, Schizophrenie) ≈ +0,18-0,23; r_g(Intelligenz, Neurotizismus) ≈ -0,20; positive r_g zwischen Intelligenz und ASD, ADHD; diese Befunde deuten auf gemeinsame biologische Mechanismen hin (Synapsen-Entwicklung, Myelinisierung, GABA/Glutamat-Balance); Genomic SEM (Grotzinger 2019) modelliert latente genetische Faktoren über Phänotypen",
        answerC = "Genetische Korrelationen zwischen Intelligenz und psychischen Erkrankungen sind exakt null; diese Phänotypen sind vollständig genetisch unabhängig, trotz phänotypischer Überlappung in klinischen Stichproben",
        answerD = "Alle kognitiven Merkmale haben eine genetische Korrelation von r_g = 1,0 zueinander, da sie alle durch den allgemeinen Intelligenzfaktor g kontrolliert werden; separate GWAS für verschiedene kognitive Tests sind daher redundant",
        correctAnswer = 1, // B
        explanation = "LD Score Regression (LDSC) nutzt die Tatsache, dass Teststatistiken in Regionen hohem LD durch Pleiotropie oder genetische Korrelation zwischen Phänotypen erhöht sind. Cross-trait LDSC: r_g = Kovarianz der Effektgrößen geteilt durch Produkt der Standardabweichungen. Wichtige Befunde: Intelligenz korreliert positiv mit Bildungsjahren (r_g = 0,70), Leseleistung (0,75), Wortschatzmessung (0,80) — ein gemeinsamer latenter Faktor. Psychiatrische Korrelationen: r_g(IQ, SCZ) = +0,20 — überraschend, da SCZ mit kognitiven Defiziten assoziiert; erklärt durch Allele die beide hochregulieren (neuronale Aktivität) aber mit psychosem Risiko. Genomic Structural Equation Modeling (Grotzinger et al. 2019, Nature Human Behaviour): Latente gemeinsame genetische Faktoren über Phänotypen modellieren, äquivalent zu faktoranalytischen Modellen. Gemeinsame SNP-Genetik von kognitiven Merkmalen konzentriert sich auf Synapsen-Entwicklungsgene, Myelinisierung und präfrontale Kortex-Reifung.",
        difficulty = 5,
        funFact = "Genetische Varianten, die Intelligenz erhöhen, erhöhen statistisch auch leicht das Schizophrenie-Risiko — und Varianten, die Kreativität fördern, erhöhen das Bipolar-Risiko. Evolution hat offenbar kognitive Leistung auf einem schmalen Grat zwischen Genie und psychischer Anfälligkeit ausbalanciert."
    ),

    // ── ADDITIONAL MASTER QUESTION ────────────────────────────────────────────

    // Question 45 – Topological Insulators and Quantum Hall Effect  [correct = C]
    Question(
        categoryId = 2,
        questionText = "Topologische Isolatoren sind Quantenmaterialien, die im Inneren isolieren, aber an ihrer Oberflaeche oder Grenzflaeche metallische Zustaeende aufweisen, die durch Zeitumkehr-Symmetrie schuetzt sind. Welcher topologische Invariant klassifiziert 3D-topologische Isolatoren, und wie erklaert die Bulk-Boundary-Correspondence die Robustheit der Oberflaechenzustaende gegenueber nichtmagnetischen Stoerungens?",
        answerA = "3D-topologische Isolatoren werden durch den TKNN-Invariant (Chern-Zahl n) klassifiziert, analog zum Integer Quantum Hall Effect; Oberflaechenzustaende sind robust, weil der Chern-Zahl-Unterschied zwischen Vakuum (n=0) und Bulk (n=1) topologisch erzwungene Grenzflaechen-Leitungsband-Ueberschneidungen erzeugt",
        answerB = "3D-topologische Isolatoren werden durch die magnetische Suszeptibilitaet klassifiziert; Oberflaechenzustaende entstehen durch Brechung der Zeitumkehr-Symmetrie an der Grenzflaeche, wodurch Spin-Bahn-Kopplung metallische Zustaende erzwingt",
        answerC = "3D-topologische Isolatoren werden durch einen Z2-Invariant (nu = 0 oder 1, starker TI) klassifiziert, der aus dem Paritaetseigenschaft der Kramers-Entartungspunkte (TRIM) in der Brillouin-Zone berechnet wird; die Bulk-Boundary-Correspondence garantiert, dass ein nu=1-Bulk eine ungerade Anzahl Dirac-Kegel-Oberflaechenzustaende traegt, die durch Zeitumkehr-Symmetrie nicht mit Rueckstreuung bei nichtmagnetischen Stoerungen vernichtet werden koennen",
        answerD = "3D-topologische Isolatoren werden durch den Winding-Number der Berry-Phase um die Fermi-Flaeche klassifiziert; ihre Oberflaechenzustaende sind robust, weil sie im Energiespalt des Bulk liegen und daher keine Streuzentren in der Naehe haben",
        correctAnswer = 2, // C
        explanation = "3D-Topologische Isolatoren (TI) wie Bi2Se3 oder Bi2Te3 werden durch einen Z2-topologischen Invariant klassifiziert, der vier Invarianten (nu0; nu1, nu2, nu3) umfasst. Der starke TI hat nu0 = 1. Dieser Invariant wird aus den Paritaetseigenwerten der Bloch-Wellenfunktionen an den acht zeitumkehrinvarianten Momenten (TRIM) in der Brillouin-Zone berechnet: nu0 = Produkt von (-1)^{delta_i} mod 2. Die Bulk-Boundary-Correspondence (BBC) besagt: Ein topologisch nicht-trivialer Bulk (nu0=1) erzwingt an jeder Grenzflaeche zu einem trivialen Isolator (Vakuum, nu0=0) eine ungerade Anzahl von Dirac-Kegel-Oberflaechenzustaenden. Diese Oberflaechenzustaende haben spin-momentum-locking (Spin steht senkrecht zum Impuls) und sind durch Kramers-Theorem zeitumkehr-geschuetzt: Rueckstreuung (k → -k) wuerde Spin-Flip erfordern, was von nichtmagnetischen Stoerungen nicht geleistet werden kann. Experimentell nachgewiesen durch ARPES (winkelaufgeloeste Photoelektronenspektroskopie): klarer linearer Dirac-Kegel an der Bi2Se3-Oberflaeche.",
        difficulty = 5,
        funFact = "Topologische Isolatoren wurden 2007 theoretisch vorhergesagt (Fu, Kane, Mele) und 2008 experimentell in BiSb bestaetigt. Sie koennten die Grundlage fuer Majorana-Fermionen-basierte topologische Qubits bilden — die robusteste Variante von Quantenbits, immun gegen lokale Stoerungen."
    ),

)

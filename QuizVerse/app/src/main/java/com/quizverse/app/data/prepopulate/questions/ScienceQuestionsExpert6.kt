package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun scienceQuestionsExpert6(): List<Question> = listOf(

    // ── Biostatistics 1 ────────────────────────────────────────────────────────
    // Question 1 — Bayesian vs frequentist  [correct = B → 1]
    Question(
        categoryId = 2,
        questionText = "Was ist der fundamentale konzeptionelle Unterschied zwischen dem bayesianischen und dem frequentistischen Ansatz in der statistischen Inferenz?",
        answerA = "Frequentisten nutzen Stichproben, Bayesianer analysieren ausschließlich Populationen",
        answerB = "Frequentisten behandeln Parameter als feste, unbekannte Konstanten und Wahrscheinlichkeit als relative Häufigkeit; Bayesianer behandeln Parameter als Zufallsvariablen mit einer Prior-Verteilung, die durch Daten zu einer Posterior-Verteilung aktualisiert wird",
        answerC = "Bayesianer verwenden keine Konfidenzintervalle, während Frequentisten ausschließlich Bayes-Faktoren nutzen",
        answerD = "Der frequentistische Ansatz ist ausschließlich für kategoriale Daten geeignet, der bayesianische für kontinuierliche",
        correctAnswer = 1, // B
        explanation = "Im frequentistischen Paradigma sind Parameter fixe (wenn unbekannte) Konstanten, und Wahrscheinlichkeit beschreibt die Häufigkeit von Ereignissen in wiederholten Experimenten. Bayesianer kodieren vorhandenes Wissen im Prior P(θ), kombinieren diesen mit der Likelihood P(D|θ) und erhalten via Satz von Bayes die Posterior P(θ|D) ∝ P(D|θ)·P(θ).",
        difficulty = 4,
        funFact = "Thomas Bayes veröffentlichte seinen Satz posthum 1763 — Richard Price gab das Manuskript heraus, nachdem Bayes verstorben war."
    ),

    // Question 2 — p-value interpretation  [correct = C → 2]
    Question(
        categoryId = 2,
        questionText = "Was bedeutet ein p-Wert von 0,03 in einem zweiseitigen t-Test korrekt interpretiert?",
        answerA = "Die Wahrscheinlichkeit, dass die Nullhypothese wahr ist, beträgt 3 %",
        answerB = "Die Wahrscheinlichkeit, dass der beobachtete Effekt durch Zufall entstanden ist, beträgt 3 %",
        answerC = "Unter der Annahme, dass die Nullhypothese wahr ist, würden 3 % aller Zufallsstichproben eine Teststatistik ergeben, die mindestens so extrem ist wie die beobachtete",
        answerD = "Die Wahrscheinlichkeit, dass die Alternativhypothese wahr ist, beträgt 97 %",
        correctAnswer = 2, // C
        explanation = "Der p-Wert ist keine Wahrscheinlichkeit für die Nullhypothese (frequentistische Paradigma: Hypothesen sind nicht zufällig). Er ist P(Daten ≥ beobachtet | H₀ wahr) — ein bedingtes Wahrscheinlichkeitsmaß über den Datenraum. Die Antworten A, B und D sind klassische Fehlinterpretationen.",
        difficulty = 4,
        funFact = null
    ),

    // Question 3 — Multiple testing correction  [correct = A → 0]
    Question(
        categoryId = 2,
        questionText = "Welche Korrekturmethode für multiples Testen kontrolliert die False Discovery Rate (FDR) statt der familienweisen Fehlerrate (FWER) und ist daher bei Hochdurchsatz-Genomik-Studien der Bonferroni-Korrektur vorzuziehen?",
        answerA = "Benjamini-Hochberg-Prozedur",
        answerB = "Šidák-Korrektur",
        answerC = "Holm-Bonferroni-Methode",
        answerD = "Dunnett-Test",
        correctAnswer = 0, // A
        explanation = "Die Benjamini-Hochberg-Prozedur (1995) kontrolliert den Erwartungswert des Anteils falsch-positiver Befunde unter allen positiven Befunden (FDR = E[FP/(FP+TP)]). Bei tausenden simultaner Tests in Genomik-Studien ist die FWER-Kontrolle (Bonferroni) zu konservativ und erhöht die falsch-negativ-Rate erheblich.",
        difficulty = 4,
        funFact = "Yoav Benjamini und Yosef Hochberg publizierten die FDR-Kontrolle 1995 — heute einer der am häufigsten zitierten statistischen Artikel."
    ),

    // Question 4 — Survival analysis  [correct = D → 3]
    Question(
        categoryId = 2,
        questionText = "Welche Annahme ist für den Log-Rank-Test beim Vergleich von Kaplan-Meier-Überlebenskurven zweier Gruppen zwingend erforderlich?",
        answerA = "Die Überlebenszeiten müssen normalverteilt sein",
        answerB = "Es darf keine zensierten Beobachtungen geben",
        answerC = "Die Stichprobengrößen beider Gruppen müssen identisch sein",
        answerD = "Die Hazard-Raten beider Gruppen müssen proportional sein (proportional hazards assumption)",
        correctAnswer = 3, // D
        explanation = "Der Log-Rank-Test ist ein non-parametrischer Test auf Gleichheit zweier Überlebensfunktionen, setzt aber proportionale Hazards voraus (d.h. die Hazard Ratio zwischen den Gruppen ist konstant über die Zeit). Bei sich kreuzenden Überlebenskurven ist er wenig sensitiv; dann sind gewichtete Tests (Flemington-Harrington) geeigneter.",
        difficulty = 4,
        funFact = null
    ),

    // Question 5 — Power analysis  [correct = B → 1]
    Question(
        categoryId = 2,
        questionText = "Welche vier Parameter bestimmen gemeinsam die statistische Power (1−β) eines Zwei-Stichproben-t-Tests, und wie beeinflusst eine Verdoppelung der Stichprobengröße n die Power?",
        answerA = "α, β, n und die Varianz; Verdoppelung von n halbiert den β-Fehler linear",
        answerB = "Effektgröße δ, Signifikanzniveau α, Stichprobengröße n und Streuung σ; Verdoppelung von n erhöht die Power, weil der nicht-zentrale Parameter √n·δ/σ mit √2 skaliert",
        answerC = "Nur α und n; Verdoppelung von n verdoppelt die Power",
        answerD = "Effektgröße, Fallzahl, Messwiederholungen und Interaktionsterm; Verdoppelung von n garantiert Power = 0,99",
        correctAnswer = 1, // B
        explanation = "Power = P(H₀ ablehnen | H₁ wahr) = 1 − β. Die nicht-zentrale t-Verteilung hat als Nicht-Zentralitätsparameter λ = δ√n/σ. Da n unter der Wurzel steht, skaliert die Power mit √n, nicht linear mit n. Verdopplung von n → Faktor √2 ≈ 1,41 mehr Nicht-Zentralität.",
        difficulty = 4,
        funFact = "Eine Power von 80 % (β = 0,20) ist der traditionelle Mindestwert — das bedeutet aber 20 % Chance, einen echten Effekt zu verpassen."
    ),

    // ── Nanotechnology 2 ───────────────────────────────────────────────────────
    // Question 6 — Quantum confinement  [correct = C → 2]
    Question(
        categoryId = 2,
        questionText = "Was ist das physikalische Prinzip hinter der größenabhängigen Fluoreszenzfarbe von Cadmiumselenid-Quantenpunkten?",
        answerA = "Größere Quantenpunkte haben mehr Oberflächendefekte, die Rotverschiebung verursachen",
        answerB = "Die Bandlücke verkleinert sich mit abnehmendem Partikelradius durch verstärkte Elektron-Loch-Wechselwirkung",
        answerC = "Wenn der Partikelradius kleiner als der Bohr-Exzitonradius wird, führt die räumliche Einschränkung der Ladungsträger zur Quantisierung der Energieniveaus und vergrößert die effektive Bandlücke",
        answerD = "Die Oberflächenspannung verändert die Gitterkonstante und damit den Brechungsindex",
        correctAnswer = 2, // C
        explanation = "Quantum Confinement tritt auf, wenn die Partikelgröße in den Bereich des Bohr-Exzitonradius (bei CdSe ≈ 5,6 nm) fällt. Durch Einschränkung in allen drei Dimensionen werden diskrete Energieniveaus gebildet. Kleinere Partikel → größere Energielücke → kurzwelligeres (blaues) Licht; größere Partikel → rötliches Licht.",
        difficulty = 4,
        funFact = "CdSe-Quantenpunkte leuchten in 2 nm Größe blau, bei 7 nm rot — das gesamte sichtbare Spektrum durch bloße Größenänderung."
    ),

    // Question 7 — Self-assembly  [correct = A → 0]
    Question(
        categoryId = 2,
        questionText = "Welcher thermodynamische Antrieb liegt der amphiphilen Selbstorganisation von Blockcopolymeren zu Mizellen oder Vesikeln in wässriger Lösung zugrunde?",
        answerA = "Der hydrophobe Effekt: Ausschluss hydrophober Segmente aus der Wasserphase minimiert die Störung der Wasserstruktur und erhöht die Gesamtentropie des Systems",
        answerB = "Kovalente Bindungsbildung zwischen den hydrophoben Blöcken treibt die Aggregation",
        answerC = "Elektrostatische Abstoßung zwischen den hydrophilen Blöcken erzwingt kugelförmige Strukturen",
        answerD = "Magnetische Dipolwechselwirkungen zwischen Polymerketten orientieren die Selbstorganisation",
        correctAnswer = 0, // A
        explanation = "Der hydrophobe Effekt ist entropisch getrieben: Wasser muss um unpolare Gruppen geordnete Käfigstrukturen bilden (Clathrat-Wasser), was die Entropie senkt. Durch Aggregation der hydrophoben Blöcke wird dieser Entropieverlust minimiert. Die freie Energie ΔG = ΔH − TΔS des Systems wird durch den TΔS-Term dominiert.",
        difficulty = 4,
        funFact = null
    ),

    // Question 8 — Nanowires  [correct = D → 3]
    Question(
        categoryId = 2,
        questionText = "Welcher Mechanismus beschreibt das Wachstum von Halbleiter-Nanodrähten im Vapor-Liquid-Solid (VLS)-Prozess?",
        answerA = "Dampfphasenmoleküle kondensieren direkt auf der Substratoberfläche zu einkristallinen Drähten",
        answerB = "Elektrochemische Abscheidung von Metall in porösen Aluminiumoxid-Templates",
        answerC = "Laserablation erzeugt Plasma, das durch Magnetfelder zu Drähten geformt wird",
        answerD = "Metallkatalysatortröpfchen (z.B. Au) absorbieren Vorläuferdampf, bilden eine flüssige Legierung und scheiden Halbleitermaterial gerichtet an der Flüssig-Fest-Grenzfläche aus, wodurch unidirektionales Drahtwachstum entsteht",
        correctAnswer = 3, // D
        explanation = "Im VLS-Mechanismus (Wagner & Ellis, 1964) dient ein Metallkatalysatortröpfchen als flüssige Phase. Vorläufermoleküle (Vapor) lösen sich bevorzugt im Metallkatalysator (Liquid), übersättigen die Legierung und kristallisieren an der Grenzfläche zum Substrat (Solid) aus. Der Katalysator bleibt an der Drahts­pitze, der Draht wächst axial.",
        difficulty = 4,
        funFact = "Charles Lieber am Harvard nutzte VLS-Nanowire zur Herstellung der ersten nanodrahtbasierten Transistoren mit nur 20 nm Durchmesser."
    ),

    // Question 9 — Drug delivery nanoparticles  [correct = B → 1]
    Question(
        categoryId = 2,
        questionText = "Welches Phänomen ermöglicht die passive Anreicherung von Nanopartikeln (100–200 nm) in soliden Tumoren ohne aktives Targeting?",
        answerA = "Magnetische Führung durch endogene Eisenablagerungen im Tumorgewebe",
        answerB = "Enhanced Permeability and Retention (EPR)-Effekt: fehlerhafte, hyperpermeabler Tumor-Neovaskulatur und fehlendes Lymphsystem ermöglichen Nanopartikel-Akkumulation",
        answerC = "Tumorspezifische Endozytose durch überexprimierte Clathrin-Coated-Pits",
        answerD = "pH-abhängige Auflösung von Nanopartikeln ausschließlich im sauren Tumor-Mikromilieu",
        correctAnswer = 1, // B
        explanation = "Tumore bilden schnell, aber unregelm'äßig neue Blutgefäße (Angiogenese) mit großen Poren (200–2000 nm) und fehlendem Lymphabfluss. Makromoleküle und Nanopartikel dringen durch diese Lücken ein (Enhanced Permeability = erhöhte Permeabilität) und werden nicht abtransportiert (Retention). Der EPR-Effekt ist die Grundlage für nanopharmazeutische Krebstherapie.",
        difficulty = 4,
        funFact = "Liposomales Doxorubicin (Doxil) war das erste FDA-zugelassene Nanomedizel (1995) — es nutzt genau den EPR-Effekt."
    ),

    // Question 10 — Nanocomposites  [correct = A → 0]
    Question(
        categoryId = 2,
        questionText = "Warum verbessern Tonmineralien (Montmorillonit) als Nanofüllstoffe in Polymer-Nanokompositen die mechanischen Eigenschaften bei deutlich geringerer Füllstoffmenge als konventionelle Mikropartikel?",
        answerA = "Das extrem hohe Aspektverhältnis (>100) der exfolierten Tonplättchen erzeugt eine massive Grenzfläche; die nanoskalige Verteilung immobilisiert Polymerketten in einem großvolumigen Interphase-Bereich",
        answerB = "Tonmineralien bilden kovalente Bindungen mit der Polymermatrix, während Mikropartikel nur physikalisch eingebettet sind",
        answerC = "Die kristalline Struktur von Ton überträgt sich auf das amorphe Polymer und erhöht seinen Kristallinitätsgrad",
        answerD = "Nanoton reflektiert UV-Strahlung und verhindert so Polymerkettenspaltung, was höhere scheinbare Festigkeit ergibt",
        correctAnswer = 0, // A
        explanation = "Exfolierte Montmorillonit-Plättchen (1 nm Dicke, 100–1000 nm lateral) haben Aspektverhältnisse >100. Bei 5 Gew.-% ergibt sich eine spezifische Oberfläche von ~750 m²/g. Die immense Polymer-Füllstoff-Grenzfläche schränkt die Kettenmobilität ein (Interphase-Effekt) und erhöht Steifigkeit, Zugfestigkeit und Barriereeigenschaften.",
        difficulty = 4,
        funFact = null
    ),

    // ── Climate Modeling 3 ─────────────────────────────────────────────────────
    // Question 11 — General circulation models  [correct = C → 2]
    Question(
        categoryId = 2,
        questionText = "Welche Grundgleichungen bilden den dynamischen Kern (Dynamical Core) eines atmosphärischen allgemeinen Zirkulationsmodells (AGCM)?",
        answerA = "Schrödinger-Gleichung und Maxwell-Gleichungen für elektromagnetische Strahlungstransporte",
        answerB = "Boltzmann-Transportgleichung für Luftmoleküle und Fourier-Gesetz für Wärmeleitung",
        answerC = "Primitive Gleichungen: hydrostatische Näherung der Navier-Stokes-Gleichungen für großskalige atmosphärische Bewegungen, kombiniert mit Kontinuitäts-, Thermodynamik- und Wasserdampfgleichungen",
        answerD = "Eikonal-Gleichungen für Schallwellenausbreitung kombiniert mit Stefan-Boltzmann-Strahlungsgesetz",
        correctAnswer = 2, // C
        explanation = "AGCMs lösen die primitiven Gleichungen: hydrostatisches Gleichgewicht (dp/dz = −ρg), horizontale Impulsgleichungen mit Coriolis-Term, Kontinuitätsgleichung, thermodynamische Gleichung (1. Hauptsatz) und Feuchtegleichung. Die hydrostatische Näherung gilt für großskalige Bewegungen (horizontale Skala ≫ vertikale Skala).",
        difficulty = 4,
        funFact = "Moderne AGCMs wie ECMWF-IFS haben horizontale Auflösungen von ~9 km — eine einzelne Modelllauf für 10 Tage benötigt 10⁶ CPU-Stunden."
    ),

    // Question 12 — Parameterization schemes  [correct = D → 3]
    Question(
        categoryId = 2,
        questionText = "Warum müssen Konvektion und Wolkenbildung in Klimamodellen mit globaler Auflösung von 50–100 km parametrisiert (statt explizit berechnet) werden?",
        answerA = "Konvektionsprozesse sind chaotisch und daher prinzipiell nicht berechenbar",
        answerB = "Wolken sind transparente Gasphasenphänomene, die keine diskrete Gitterpunktedarstellung erlauben",
        answerC = "Konvektion ist ein rein lokales Phänomen, das keine Auswirkungen auf Nachbargitterpunkte hat",
        answerD = "Konvektive Wolkentürme (km-Skala) sind kleiner als die Modellgitterzellen (50–100 km); ihre kollektiven statistischen Effekte auf Energietransport und Feuchteumverteilung werden durch Parameterisierungen (z.B. Kuo, Arakawa-Schubert, Betts-Miller) näherungsweise erfasst",
        correctAnswer = 3, // D
        explanation = "Subgitterskalenprozesse können nicht explizit aufgelöst werden. Konvektionsparameterisierungen stellen empirische Beziehungen zwischen großskaligen Variablen (Temperatur, Feuchte) und dem mittleren Effekt konvektiver Ensembles her (Konvektionsschluss-Annahmen). Dies ist die größte Unsicherheitsquelle in Klimaprojektionen.",
        difficulty = 4,
        funFact = null
    ),

    // Question 13 — Coupled models  [correct = A → 0]
    Question(
        categoryId = 2,
        questionText = "Welche Komponenten werden in einem vollständig gekoppelten Erdsystemmodell (ESM) miteinander verbunden, und welche Schnittstelle koordiniert den Datenaustausch?",
        answerA = "Atmosphäre, Ozean, Meereis, Landoberfläche, Biosphäre, Kohlenstoffzyklus und ggf. Eisschildmodelle; ein Koppler (z.B. OASIS3-MCT) synchronisiert den Fluss von Energie-, Impuls- und Masseflüssen zwischen den Komponenten",
        answerB = "Nur Atmosphäre und Ozean; die Kopplung erfolgt durch direkte Variablenweitergabe ohne separaten Koppler",
        answerC = "Atmosphäre, Ozean und Magnetosphäre; der Koppler ist ausschließlich für elektromagnetische Flüsse zuständig",
        answerD = "Alle Komponenten teilen einen gemeinsamen Gitterraum ohne separate Kopplung",
        correctAnswer = 0, // A
        explanation = "Moderne ESMs koppeln Atmosphären-, Ozean-, Meereis-, Landoberflächen-, Dynamische-Vegetation-, Kohlenstoffzyklus- und Gletschereiskomponenten. Koppler wie OASIS3-MCT, MCT oder ESMF interpolieren Felder zwischen unterschiedlichen Gittern und Auflösungen und übertragen Wärme-, Impuls- und Süßwasserflüsse.",
        difficulty = 4,
        funFact = "Das CMIP6-Projekt koordinierte über 100 ESM-Simulationen weltweit — die Daten füllen mehrere Petabyte."
    ),

    // Question 14 — Ensemble methods  [correct = B → 1]
    Question(
        categoryId = 2,
        questionText = "Welchen Zweck erfüllen Ensemble-Klimasimulationen, und wie unterscheidet sich ein Initial-Condition-Ensemble von einem Multi-Model-Ensemble?",
        answerA = "Ensembles werden ausschließlich für Wettervorhersagen genutzt, nicht für Klimaprojektionen",
        answerB = "Ensembles quantifizieren Unsicherheiten: Initial-Condition-Ensembles (gleiche Modellphysik, leicht veränderte Anfangsbedingungen) erfassen interne Klimavariabilität; Multi-Model-Ensembles (verschiedene Modelle) erfassen strukturelle Modellunsicherheit",
        answerC = "Ensembles dienen der Beschleunigung der Simulation durch parallele Berechnung identischer Modellläufe",
        answerD = "Nur Multi-Model-Ensembles haben wissenschaftliche Relevanz; Initial-Condition-Ensembles produzieren statistisch identische Ergebnisse",
        correctAnswer = 1, // B
        explanation = "Initial-Condition-Ensembles (ICE) variieren die Anfangszustände minimal und zeigen, wie chaotische interne Variabilität die Projektionsunsicherheit beeinflusst. Multi-Model-Ensembles (MME, z.B. CMIP) kombinieren strukturell verschiedene Modelle und erfassen Parameterisierungs-Unsicherheiten. Das 'Ensemble of Opportunity' kombiniert beide Quellen.",
        difficulty = 4,
        funFact = null
    ),

    // Question 15 — Climate sensitivity  [correct = C → 2]
    Question(
        categoryId = 2,
        questionText = "Was ist der Unterschied zwischen der Equilibrium Climate Sensitivity (ECS) und der Transient Climate Response (TCR), und welche Einheit tragen beide?",
        answerA = "ECS misst die Temperaturänderung pro Dekade, TCR die Gesamterwärmung bis 2100; Einheit: °C/Dekade",
        answerB = "ECS und TCR sind identisch, da das Klima keine Trägheit hat",
        answerC = "ECS: Gleichgewichtserwärmung (K) nach CO₂-Verdopplung, wenn das gesamte Klimasystem Gleichgewicht erreicht hat (Jahrhunderte); TCR: Erwärmung (K) zum Zeitpunkt der CO₂-Verdopplung in einem 1%/Jahr-Anstiegsexperiment — TCR < ECS wegen der thermischen Trägheit des Ozeans",
        answerD = "TCR beschreibt tropische, ECS polare Temperaturänderungen; Einheit jeweils W/m²",
        correctAnswer = 2, // C
        explanation = "ECS (2–5 K laut IPCC AR6, Beste Schätzung 3 K) ist das langfristige Gleichgewicht — der Ozean braucht Jahrhunderte bis Jahrtausende zum Ausgleich. TCR (1,4–2,2 K) ist kleiner, weil die Ozeane zum Verdopplungszeitpunkt noch Wärme aufnehmen. Der Unterschied zwischen TCR und ECS heißt unrealized warming.",
        difficulty = 4,
        funFact = "Die Spanne der ECS-Schätzung (2,5–4 K) ist trotz jahrzehntelanger Forschung kaum schmaler geworden — sie spiegelt fundamentale Unsicherheiten bei Wolken-Rückkopplungen wider."
    ),

    // ── Enzyme Engineering 4 ───────────────────────────────────────────────────
    // Question 16 — Directed evolution  [correct = D → 3]
    Question(
        categoryId = 2,
        questionText = "Welche Schritte umfasst ein klassischer gerichteter Evolutionszyklus für Enzyme, und welche molekularbiologische Methode wird zur zufälligen Mutagenese am häufigsten eingesetzt?",
        answerA = "Kristallstrukturanalyse → Computersimulation → Synthese; keine Zufallsmutagenese nötig",
        answerB = "Gesamtgenom-Sequenzierung → CRISPR-Knockout → Selektionskultur; Mutagenese durch Bestrahlung",
        answerC = "Ribosom-Display → Proteinfaltungssimulation → Phagenbank; Mutagenese durch UV-Licht",
        answerD = "Zufallsmutagenese (häufig error-prone PCR mit Mn²⁺ oder chemische Mutagenese) → Expression der Varianten-Bibliothek → Hochdurchsatz-Screening/Selektion auf gewünschte Eigenschaft → beste Variante als Ausgangspunkt für nächsten Zyklus",
        correctAnswer = 3, // D
        explanation = "In der gerichteten Evolution (Frances Arnold, Nobelpreis 2018) ahmt man Evolution im Labor nach: Error-prone PCR (epPCR) führt 1–5 Aminosäuresubstitutionen pro Gen ein. Die Bibliothek wird in Bakterien oder Hefe exprimiert, mit einem Hochdurchsatz-Assay gescreent und die beste Variante für den nächsten Runde genutzt (iterative Verbesserung).",
        difficulty = 4,
        funFact = "Frances Arnold gewann 2018 den Chemie-Nobelpreis für gerichtete Evolution — die erste Frau in diesem Fachbereich seit 55 Jahren."
    ),

    // Question 17 — Rational design  [correct = A → 0]
    Question(
        categoryId = 2,
        questionText = "Was unterscheidet rationales Enzymdesign von gerichteter Evolution, und welche Haupteinschränkung hat das rationale Design?",
        answerA = "Rationales Design nutzt Strukturdaten und Mechanismuswissen, um gezielte Mutationen zu planen; die Haupteinschränkung ist die unvollständige Vorhersagbarkeit von Faltungsänderungen und entfernten Konformationseffekten",
        answerB = "Rationales Design durchsucht alle möglichen Sequenzen; gerichtete Evolution nur zufällige Mutanten",
        answerC = "Rationales Design funktioniert nur für RNA-Enzyme (Ribozyme); gerichtete Evolution nur für Proteinenzyme",
        answerD = "Es gibt keinen Unterschied; beide Begriffe beschreiben denselben Ansatz mit unterschiedlichen Namen",
        correctAnswer = 0, // A
        explanation = "Rationales Design basiert auf strukturellen und mechanistischen Kenntnissen (Röntgenkristallographie, NMR, MD-Simulationen), um spezifische Aminosäuren zu mutieren. Das Problem: Proteine sind allosterisch vernetzt — eine Mutation kann durch das Proteinnetzwerk unvorhersehbare Ferneffekte auf Stabilität oder Aktivität haben. Dies limitiert die Erfolgsrate ohne iterative Experimente.",
        difficulty = 4,
        funFact = null
    ),

    // Question 18 — Computational enzyme design  [correct = B → 1]
    Question(
        categoryId = 2,
        questionText = "Welches Software-Framework, entwickelt am Baker-Lab, wird primär für das de-novo-Design neuer Enzymaktivitäten genutzt, und welche Methode liegt seiner Sequenzoptimierung zugrunde?",
        answerA = "AlphaFold3 mit MCMC-Sequenzoptimierung; nutzt ausschließlich evolutionäre Kopplungsinformationen",
        answerB = "Rosetta mit Monte-Carlo-basiertem Rotamer-Packing; optimiert Sequenz und Seitenkettenkonfigurationen auf ein Energie-Scoringsystem",
        answerC = "GROMACS mit Molecular-Dynamics-basierten Sequenzvorschlägen; optimiert thermodynamische Stabilität durch MD-Simulationen",
        answerD = "AMBER mit genetischem Algorithmus; optimiert ausschließlich Backbone-Geometrie",
        correctAnswer = 1, // B
        explanation = "Rosetta (Baker Lab, UW) nutzt ein Energiefunktional aus Van-der-Waals-, Elektrostatik-, Wasserstoffbrücken- und Solvationierungstermen. Der RosettaDesign-Algorithmus optimiert Aminosäuresequenzen durch stochastisches Rotamer-Packing und Monte-Carlo-Sampling. 2008 demonstrierte David Baker damit erstmals de-novo-Design einer Kemp-Eliminase.",
        difficulty = 4,
        funFact = "David Baker erhielt 2024 den Chemie-Nobelpreis für computergestütztes Proteindesign — zusammen mit dem AlphaFold-Team."
    ),

    // Question 19 — Immobilized enzymes  [correct = C → 2]
    Question(
        categoryId = 2,
        questionText = "Welche Immobilisierungsstrategie bietet den besten Schutz vor Enzymauslaugung aus einem Träger bei industriellen Biokatalyse-Prozessen?",
        answerA = "Adsorption an Ionenaustauscherharz durch elektrostatische Wechselwirkungen",
        answerB = "Physikalischer Einschluss in Silikatgel ohne chemische Bindungen",
        answerC = "Kovalente Bindung an aktivierte Trägeroberflächen (z.B. Epoxy- oder Glutaraldehyd-funktionalisiertes Silica) oder Vernetzung zu unlöslichen Träger-freien Aggregaten (CLEAs)",
        answerD = "Magnetische Adsorption an Eisenoxid-Nanopartikel ohne Aktivierungsschritt",
        correctAnswer = 2, // C
        explanation = "Kovalente Bindung (z.B. Lysoseitenkettenreaktion mit Epoxidgruppen) verhindert Auslaugung, da das Enzym fest mit dem Träger verknüpft ist. CLEAs (Cross-Linked Enzyme Aggregates, Sheldon 2000) vernetzt fällte Enzymaggregate mit Glutaraldehyd zu stabilen, trägerfreien Biokatalysatoren mit hoher Aktivitätsdichte.",
        difficulty = 4,
        funFact = null
    ),

    // Question 20 — Artificial metalloenzymes  [correct = D → 3]
    Question(
        categoryId = 2,
        questionText = "Wie werden künstliche Metalloenzyme (ArMs) konstruiert, und welche chemische Reaktion demonstrierte das Thomas-Ward-Labor als Proof-of-Concept?",
        answerA = "Durch genetischen Code-Erweiterung wird Metall direkt in natürliche Aminosäuren eingebaut",
        answerB = "Ein synthetisches Metalloporphyrin ersetzt enzymatisch den Häm-Cofaktor in Myoglobin",
        answerC = "Metallkatalysatoren werden kovalent an DNA-Aptamere gebunden, die spezifisch Substrate erkennen",
        answerD = "Ein synthetischer Metallkatalysator (z.B. biotinylierter Pd- oder Rh-Komplex) wird in ein Wirtprotein (z.B. Streptavidin) eingebettet; Ward-Labor zeigte asymmetrische Transferhydrierung von Ketonen mit ee > 90 %",
        correctAnswer = 3, // D
        explanation = "Artificielle Metalloenzyme kombinieren die Reaktivität organometallischer Katalysatoren mit der Selektivitätsumgebung eines Proteins. T.R. Ward (Basel) nutzte die extrem hohe Biotin-Streptavidin-Affinität (Kd ≈ 10⁻¹⁵ M), um biotinylierte Übergangsmetallkatalysatoren präzise im Streptavidin-Bindungstunnel zu positionieren — die Proteinumgebung steuert Enantioselektivität.",
        difficulty = 4,
        funFact = "Die Biotin-Streptavidin-Bindung ist mit Kd ≈ 10⁻¹⁵ M eine der stärksten nichtkovalenten Bindungen in der Natur."
    ),

    // ── Lunar & Planetary Science 5 ───────────────────────────────────────────
    // Question 21 — Lunar formation  [correct = A → 0]
    Question(
        categoryId = 2,
        questionText = "Welches Szenario für die Mondentstehung ist heute das wissenschaftliche Konsens-Modell, und welche Isotopendaten stützen es am stärksten?",
        answerA = "Giant-Impact-Hypothese (Theia-Kollision): Ein marsgroßer Protoplanet kollidierte schräg mit der frühen Erde; die nahezu identischen Sauerstoff-Isotopenverhältnisse (δ¹⁷O, δ¹⁸O) von Erde und Mond stützen gemeinsames Quellmaterial",
        answerB = "Ko-Akkretions-Modell: Erde und Mond bildeten sich gleichzeitig aus demselben Materie-Scheibenabschnitt ohne Impakt",
        answerC = "Fission-Modell: Die Erde rotierte so schnell, dass sie Material ins All schleuderte, das gravitativ zum Mond kollabierte",
        answerD = "Einfang-Hypothese: Der Mond entstand im Asteroidengürtel und wurde von der Erdschwerkraft eingefangen",
        correctAnswer = 0, // A
        explanation = "Die Giant-Impact-Hypothese (Hartmann & Davis, 1975; Cameron & Ward, 1976) erklärt Mondmasse, Drehimpuls des Systems, den kleinen Eisenkern des Mondes und den Silika-reichen Charakter. Die nahezu identischen δ¹⁷O-Werte von Erde und Mondgestein (Differenz < 0,012 ‰) belegen gemeinsames Quellmaterial, setzen aber voraus, dass Theia isotopisch der Erde ähnlich war.",
        difficulty = 4,
        funFact = "Die hypothetische Protoplanet Theia ist nach der Titanengöttin der griechischen Mythologie benannt — Mutter des Mondgottes Selene."
    ),

    // Question 22 — Mars water history  [correct = C → 2]
    Question(
        categoryId = 2,
        questionText = "Welche mineralogischen Evidenzen aus Mars-Orbitern und Rovern belegen eine frühe, flüssigwasserreiche Phase auf dem Mars (Noachian, >3,5 Mrd. Jahre)?",
        answerA = "Nachweis von Karbonaten und Evaporiten über die gesamte Marsoberfläche in gleichmäßiger Schichtung",
        answerB = "Direkte Messung von flüssigem Wasser unter der Südpolkappe durch Radarreflexionen (MARSIS)",
        answerC = "Phyllosilikate (Tone wie Smektit und Kaolinit) in alten Terrains, detected by OMEGA/CRISM; Sulfate (Jarosit) in jüngeren Schichten; Hematit-Konkretionen ('Blueberries') die Opus et al. mit wässriger Diagenese erklären",
        answerD = "Ausgedehnte Eiskerne im Marsmantel, die durch Seismometer des InSight-Landers nachgewiesen wurden",
        correctAnswer = 2, // C
        explanation = "OMEGA (Mars Express) und CRISM (MRO) identifizierten Phyllosilikate in Noachian-Gesteinen, die Bildungstemperaturen von 0–150 °C und wässrige Umgebung belegen. Der Opportunity-Rover fand Jarosit [KFe₃(SO₄)₂(OH)₆] — ein Mineral, das nur in saurer, wässriger Umgebung stabil ist. Die Hematit-Konkretionen (Sphären) deuten auf diagenetische Fällung hin.",
        difficulty = 4,
        funFact = null
    ),

    // Question 23 — Asteroid mining  [correct = B → 1]
    Question(
        categoryId = 2,
        questionText = "Welche Asteroidenklasse gilt als primäres Ziel für den kommerziellen Abbau von Platingruppenmetallen (PGMs), und warum übersteigt deren Konzentration die terrestrischen Lagerstätten?",
        answerA = "C-Typ-Asteroiden; sie enthalten PGMs wegen ihrer kohlenstoffreichen organischen Matrix in hoher Konzentration",
        answerB = "M-Typ-Asteroiden (metallreiche, weitgehend differenzierte Eisenmeteorite-Äquivalente); PGMs konzentrierten sich beim Kernbildungsprozess im Eisenkern, da sie siderophil sind — auf der Erde versank der PGM-Reichtum in den Erdkern",
        answerC = "S-Typ-Asteroiden (steinig-metallisch); PGMs stammen aus der Silikathülle differenzierter Körper",
        answerD = "D-Typ-Asteroiden (trans-neptunische Objekte); PGMs wurden durch kosmische Strahlung erzeugt",
        correctAnswer = 1, // B
        explanation = "PGMs (Pt, Pd, Ir, Os, Rh, Ru) sind hochgradig siderophil — sie folgten Eisen beim Metallkern-Silicatmantel-Gleichgewicht in den Erdkern. Die geringen terrestrischen Mantelkonzentrationen stammen aus spätem meteoritischem Bombardement (Late Veneer). M-Typ-Asteroiden (Kernfragmente differenzierter Mutterkörper) besitzen dementsprechend hohe PGM-Gehalte (~100×terrestrische Erzlagerstätten).",
        difficulty = 4,
        funFact = "Der M-Typ-Asteroid Psyche (Durchmesser ~226 km) könnte mehr Eisen und Nickel enthalten als die gesamte menschliche Metallproduktion über Millionen Jahre."
    ),

    // Question 24 — Space weathering  [correct = D → 3]
    Question(
        categoryId = 2,
        questionText = "Welche Prozesse verursachen Space Weathering auf airless bodies wie dem Mond oder Asteroiden, und welche optischen Effekte sind die Folge?",
        answerA = "Ultraviolette Strahlung oxidiert Oberflächenmineralien und bleicht deren Farbe aus",
        answerB = "Radioaktiver Zerfall von Oberflächenisotopen erzeugt Wärme, die zu Entglasung führt",
        answerC = "Kosmische Strahlung ionisiert Oberflächensilika und erzeugt blaue Lumineszenz",
        answerD = "Mikrometeoritenimpakte und Solarwind-Ionenbeschuss erzeugen nanometergroße metallische Eisenpartikel (npFe⁰) in einer Schmelzglas-Agglutinate-Schicht (Regolith); Folge: spektrale Rötung, Albedoverringerung und Dämpfung von Absorptionsbanden",
        correctAnswer = 3, // D
        explanation = "Space Weathering (Cassidy & Hapke) umfasst: (1) Solarwind-Implantation (H⁺, He²⁺) erzeugt nano-Phase-Eisen durch Reduktion von Fe²⁺; (2) Mikroimpakte schmelzen und rekondensieren Silika zu Agglutinaten. Nanometergroße Fe⁰-Partikel in Glaskügelchen absorbieren breitbandig und röten sowie dämpfen Spektren — erklärt den Unterschied zwischen frischem und gereiftem Mondboden.",
        difficulty = 4,
        funFact = null
    ),

    // ── Molecular Ecology 6 ────────────────────────────────────────────────────
    // Question 25 — Environmental DNA  [correct = A → 0]
    Question(
        categoryId = 2,
        questionText = "Was ist environmental DNA (eDNA), und welche methodischen Herausforderungen bestehen bei der quantitativen Artdetektion in aquatischen Systemen?",
        answerA = "eDNA bezeichnet genetisches Material, das Organismen in die Umwelt abgeben (Schuppen, Schleim, Fäkalien); Herausforderungen sind Degradation (Halbwertszeit Stunden bis Tage), Kontamination, Transport durch Wasserbewegung und PCR-Inhibitoren in Sedimentproben",
        answerB = "eDNA ist eine spezifische DNA-Markierung in Umweltschutz-Datenbanken; Herausforderung ist der Datenbankzugang",
        answerC = "eDNA bezeichnet ausschließlich ancient DNA aus Sedimentkernen; Herausforderungen sind Deamination und Fragmentierung",
        answerD = "eDNA ist synthetische DNA, die zur Umweltüberwachung ausgebracht wird; Herausforderungen sind Regulierungsfragen",
        correctAnswer = 0, // A
        explanation = "eDNA-Methodik (Ficetola et al., 2008) revolutionierte die Biodiversitätserfassung: Wasser- oder Bodenproben werden gesammelt, DNA extrahiert und mit artspezifischen Primern (qPCR/ddPCR) oder Metabarcoding (Amplicon-Sequenzierung) analysiert. Kritisch: eDNA-Signal ist räumlich und zeitlich variabel, PCR-Inhibitoren (Huminsäuren) und Carry-over-Kontamination erfordern strenge Kontrollen.",
        difficulty = 4,
        funFact = "Mit eDNA-Metabarcoding einer einzelnen Meerwasserprobe können Hunderte von Fischarten gleichzeitig nachgewiesen werden."
    ),

    // Question 26 — Population structure  [correct = C → 2]
    Question(
        categoryId = 2,
        questionText = "Welche statistische Größe quantifiziert genetische Differenzierung zwischen Subpopulationen, und was bedeutet ein Wert von FST = 0,3?",
        answerA = "FST ist die absolute Allelfrequenz einer Subpopulation; FST = 0,3 bedeutet 30 % der Allele sind fixiert",
        answerB = "FST ist der Inzuchtskoeffizient einer Individuen-Population; FST = 0,3 bedeutet 30 % Inzucht",
        answerC = "FST (Wright's Fixierungsindex) ist der Anteil der Gesamtvariation, der auf Unterschiede zwischen Subpopulationen zurückzuführen ist; FST = 0,3 bedeutet 30 % der genetischen Variation liegt zwischen (nicht innerhalb) der Subpopulationen — moderate bis hohe Differenzierung",
        answerD = "FST ist die Migrationsrate pro Generation; FST = 0,3 bedeutet 30 % Migration pro Generation",
        correctAnswer = 2, // C
        explanation = "FST = (HT − HS)/HT, wobei HT die erwartete Heterozygotie der Gesamtpopulation und HS der Mittelwert innerhalb der Subpopulationen ist. FST = 0 → panmiktische Gesamtpopulation; FST = 1 → vollständige Fixierung unterschiedlicher Allele in jeder Subpopulation. FST = 0,3 zeigt signifikante Barriere für Genfluss (Nm ≈ (1−FST)/(4FST) ≈ 0,6 Migranten/Generation).",
        difficulty = 4,
        funFact = null
    ),

    // Question 27 — Landscape genetics  [correct = D → 3]
    Question(
        categoryId = 2,
        questionText = "Wie verbindet Landschaftsgenetik Populationsgenetik und Landschaftsökologie, und welche Analysemethode wird häufig zur Erfassung von Genfluss-Barrieren genutzt?",
        answerA = "Landschaftsgenetik analysiert ausschließlich fossile Pollen zur Rekonstruktion historischer Baumarten-Ausbreitung",
        answerB = "Die Disziplin nutzt Satelliten-Fernerkundung, um direkt die Genmutation in Organismen durch UV-Strahlung zu messen",
        answerC = "Landschaftsgenetik kombiniert GPS-Telemetrie-Daten mit Blutgruppen-Serologie zur Kartierung von Tierwanderrouten",
        answerD = "Landschaftsgenetik verbindet Genotypdaten (SNPs, Mikrosatelliten) mit räumlichen Umweltvariablen (Landbedeckung, Topographie) durch Widerstandsoberflächen (resistance surfaces) und Circuit Theory-Analysen (z.B. Circuitscape), um Korridore und Barrieren für Genfluss zu identifizieren",
        correctAnswer = 3, // D
        explanation = "Landschaftsgenetik (Manel et al., 2003) modelliert, wie Landschaftsstruktur genetische Differenzierung beeinflusst. Circuit Theory (McRae 2006) behandelt die Landschaft als elektrisches Netzwerk: Widerstand ≈ genetische Barriere, Strom ≈ Genfluss-Wahrscheinlichkeit. Korrelation genetischer Distanzmatrizen mit Landschafts-Widerstandsmatrizen (MFMM, Mantel-Test) identifiziert effektive Korridore.",
        difficulty = 4,
        funFact = "Circuitscape wurde ursprünglich für Cougar-Populationen in Südkalifornien entwickelt — heute ist es ein Standard-GIS-Tool für Naturschutzplanung."
    ),

    // Question 28 — Conservation genomics  [correct = B → 1]
    Question(
        categoryId = 2,
        questionText = "Was sind Inbreeding Depression und Outbreeding Depression, und wie nutzt Conservation Genomics genomische Daten, um optimale Paarungsstrategien für bedrohte Arten zu planen?",
        answerA = "Beide Phänomene beschreiben denselben Effekt; Genomics hilft dabei, bunte Farbvarianten zu züchten",
        answerB = "Inbreeding Depression: Fitnessverlust durch Häufung homozygoter deletärer Allele bei Verwandtschaftspaarung; Outbreeding Depression: Fitnessverringerung durch Zerbrechen ko-adaptierter Genkomplexe bei zu entfernten Kreuzungen; Genomics identifiziert Runs of Homozygosity (ROH) als Inzuchtmaß und wählt Paarungspartner, die ROH minimieren ohne lokal-adaptierte Loci zu stören",
        answerC = "Inbreeding Depression betrifft nur Pflanzen; Outbreeding Depression nur Meerestiere; Genomics wird für beide Fälle nicht genutzt",
        answerD = "Beide Phänomene sind evolutionsbiologisch irrelevant; in Gefangenschaft kommt es nie zu Fitnessdepression",
        correctAnswer = 1, // B
        explanation = "Runs of Homozygosity (ROH) — lange homozygote Chromosomenblöcke — sind ein direktes Maß für Inzucht (FROH). Genomik-gestütztes Zuchtmanagement (Genetic Rescue) wählt Paarungspartner, die FROH minimieren. Outbreeding Depression ist real: Drosophila- und Fisch-Studien zeigen reduzierte Fitness bei F2-Hybriden aus sehr divergenten Populationen durch Rekombination ko-adaptierter Epistasie-Komplexe.",
        difficulty = 4,
        funFact = null
    ),

    // ── Medical Genetics 7 ─────────────────────────────────────────────────────
    // Question 29 — Whole genome sequencing  [correct = A → 0]
    Question(
        categoryId = 2,
        questionText = "Welchen Vorteil bietet Whole Genome Sequencing (WGS) gegenüber Exom-Sequenzierung (WES) bei der Diagnose seltener Mendelischer Erkrankungen, und was ist die typische Detektionsrate diagnostischer Varianten bei WGS in ungelösten Seltenkrankheits-Kohorten?",
        answerA = "WGS erfasst nicht-kodierende Varianten (Promotoren, Enhancer, Spleißstellen, Deep-Intronic), strukturelle Varianten (SVs) und Copy-Number-Varianten (CNVs) vollständiger; in ungelösten WES-Kohorten liefert WGS zusätzliche Diagnosen in 8–15 % der Fälle",
        answerB = "WGS ist identisch zu WES; der einzige Unterschied ist der Preis",
        answerC = "WGS dekodiert ausschließlich mitochondriale DNA, WES nur nukleäre Gene",
        answerD = "WGS hat eine niedrigere Sensitivität bei kodierenden Varianten als WES, da es die Sequenztiefe auf das gesamte Genom verteilt",
        correctAnswer = 0, // A
        explanation = "WES erfasst ≈1 % des Genoms (kodierend), übersieht regulatorische Regionen und hat schwächere SV-Detektion. WGS (Abdeckung ≈30–40×) ermöglicht Analyse von Deep-Intronic-Varianten (z.B. Pseudoexon-Aktivierung), Enhancer-Mutationen und SVs >50 bp. Mehrere Studien (Smedley 2021, Turro 2020) berichten 8–15 % zusätzliche Diagnosen in vorher unlösbaren Seltenkrankheitsfällen.",
        difficulty = 4,
        funFact = "Das erste vollständige menschliche Genom kostete im Jahr 2000 drei Milliarden Dollar — heute kostet WGS weniger als 200 Euro."
    ),

    // Question 30 — Variant interpretation  [correct = C → 2]
    Question(
        categoryId = 2,
        questionText = "Welches Klassifizierungssystem verwendet die klinische Genetik zur Einordnung genomischer Varianten, und welche Evidenztypen fließen in die Bewertung einer Variante als 'Pathogen' ein?",
        answerA = "ICD-11-Kodierungssystem; nur Frequenz in der Allgemeinbevölkerung wird berücksichtigt",
        answerB = "HGMD-Score; ausschließlich bioinformatische In-silico-Vorhersagen",
        answerC = "ACMG/AMP-Leitlinien (Richards et al., 2015) mit 5 Klassen (Pathogen, Wahrscheinlich Pathogen, VUS, Wahrscheinlich Benigne, Benigne); Evidenzen: Populationsfrequenz (gnomAD), funktionelle Daten, Segregationsanalyse, Literatur, In-silico-Vorhersagen, Typ der Variante (Nonsense, Missense)",
        answerD = "WHO-Risikoklassifikation; nur CADD-Score und ESP-Populationsdaten",
        correctAnswer = 2, // C
        explanation = "Die ACMG/AMP-Richtlinien definieren Evidenz-Kategorien (PS1–PS4, PM1–PM6, PP1–PP5 für pathogene Evidenz; BA1, BS1–BS4, BP1–BP7 für benigne). Die kombinierte Evidenzgewichtung ergibt die Klassifikation. gnomAD-Allelhäufigkeit >5 % in Kontrollkohorten ist starke benigne Evidenz (BA1); Nonsense-Variante in haploinsuffizienz-sensitiven Genen + Segregation mit Phänotyp = starke pathogene Evidenz.",
        difficulty = 4,
        funFact = null
    ),

    // Question 31 — Polygenic risk scores  [correct = D → 3]
    Question(
        categoryId = 2,
        questionText = "Was ist ein Polygener Risiko-Score (PRS), und welche methodische Limitation schränkt seine klinische Nützlichkeit in diversen Bevölkerungsgruppen ein?",
        answerA = "PRS ist ein einzelner Marker-SNP, der Monogen-Erkrankungen vorhersagt; keine Populationsbeschränkung",
        answerB = "PRS addiert SNP-Zählungen ohne Gewichtung; er funktioniert in allen Populationen gleich gut",
        answerC = "PRS ist eine familienbasierte Segregationsanalyse; er gilt nur für Verwandte ersten Grades",
        answerD = "PRS ist die gewichtete Summe von Risiko-Allelen über das Genom (PRS = Σ βᵢ·Gᵢ); Limitation: GWAS-Entdeckungskohorten sind meist europäischer Abstammung, weshalb LD-Muster, Allelfrequenzen und Effektgrößen in anderen Populationen abweichen — Transfer-PRS überschätzt oder unterschätzt das Risiko in afrikanischen/asiatischen Kohorten",
        correctAnswer = 3, // D
        explanation = "Da GWAS vorwiegend Europäer untersuchten (85–90 % der GWAS-Probanden bis 2019), basieren PRS-Gewichte auf europäischem Linkage-Disequilibrium. In afrikanischen Populationen mit geringerem LD blockweise andere Kausalvarianten taggen dieselben assoziierten SNPs schlechter, was die prädiktive Genauigkeit (AUC) um 30–50 % verringert.",
        difficulty = 4,
        funFact = "Das 'GWAS Diversity Monitor' zeigt, dass seit 2016 der Anteil nicht-europäischer Kohorten auf ≈29 % gestiegen ist — ein Fortschritt, aber noch unzureichend."
    ),

    // Question 32 — Genetic counseling ethics  [correct = A → 0]
    Question(
        categoryId = 2,
        questionText = "Was ist das Prinzip des 'Right not to know' in der genetischen Beratung, und in welchem ethischen Spannungsfeld steht es bei prädiktiver Diagnostik für spät-manifestierende Erkrankungen wie Huntington?",
        answerA = "Das Recht auf Nicht-Wissen garantiert, dass Personen genetische Informationen ablehnen können; Spannungsfeld: Familienangehörige könnten durch die Entscheidung einer Person indirekt ihren eigenen Status erfahren oder davon ausgeschlossen werden (downstream disclosure problem)",
        answerB = "Es beschreibt das Recht von Ärzten, Patienten vor schlechten Nachrichten zu schützen; ethisch unproblematisch",
        answerC = "Es ist ein gesetzliches Verbot zur Weitergabe genetischer Daten an Versicherungsunternehmen",
        answerD = "Das Recht gilt nur für Minderjährige und spielt in der Erwachsenenmedizin keine Rolle",
        correctAnswer = 0, // A
        explanation = "Das 'Right not to know' (UNESCO 1997, ESHG Guidelines) ist ein autonomiebasiertes Recht: Personen müssen keine Risikoinformationen annehmen. Problem bei AD-Erkrankungen wie Huntington (autosomal-dominant): Eine erwachsene Tochter, die nicht getestet werden will, wird indirekt über den Status ihres Vaters informiert, wenn ihre eigene Tochter positiv testet (Kaskade). Dies erfordert sorgfältige Familienberatung und Datenschutzprotokolle.",
        difficulty = 4,
        funFact = null
    ),

    // ── Photovoltaics advanced 8 ───────────────────────────────────────────────
    // Question 33 — Tandem solar cells  [correct = B → 1]
    Question(
        categoryId = 2,
        questionText = "Warum überschreiten Tandem-Solarzellen theoretisch die Shockley-Queisser-Grenze für Einzelübergang-Solarzellen (≈33 %), und welche Kombination erzielte 2023 in Labormessungen Wirkungsgrade über 33 %?",
        answerA = "Tandems verwenden Lichtkozentratoren statt Halbleiter; sie umgehen das Shockley-Queisser-Limit durch optische Tricks",
        answerB = "Durch Übereinanderstapeln mehrerer pn-Übergänge mit unterschiedlichen Bandlücken werden verschiedene Spektralbe­reiche des Sonnenlichts effizienter genutzt; thermalisierungsverluste werden reduziert; Perowskit/Silizium-Tandems (Bandlücken ≈1,7 eV / 1,1 eV) überschritten 33 % im Labor (Helmholtz-Zentrum, 2023)",
        answerC = "Tandems nutzen Quanteninterferenz zwischen zwei überlagerten Wellenfunktionen, um die Effizienzgrenze zu durchbrechen",
        answerD = "Das Shockley-Queisser-Limit gilt nur für organische Solarzellen; Silizium-Einfachzellen haben keine theoretische Obergrenze",
        correctAnswer = 1, // B
        explanation = "Das Shockley-Queisser-Limit (33,7 % für AM1.5G) gilt für einen einzigen Bandabstand und ergibt sich aus der Unvereinbarkeit von Absorption und Thermalisierungsverlusten. Zwei-Übergangs-Tandems haben ein theoretisches Limit von ≈46 %, da Thermalisierungsverluste durch Nutzung unterschiedlicher Photonenenergien reduziert werden. HZB/REC erzielte 2023 33,9 % mit Perowskit/Si-Monolithik-Tandem.",
        difficulty = 4,
        funFact = "Perowskit/Silizium-Tandemzellen stiegen in 5 Jahren von 13 % auf über 33 % Effizienz — eine der schnellsten Effizienzsteigerungen in der Photovoltaikgeschichte."
    ),

    // Question 34 — Quantum dot solar  [correct = D → 3]
    Question(
        categoryId = 2,
        questionText = "Welches Phänomen in Quantenpunkt-Solarzellen verspricht theoretisch Wirkungsgrade weit über 100 % externe Quanteneffizienz für hochenergetische Photonen, und wie funktioniert es?",
        answerA = "Singlet-Fission in organischen Chromophoren erzeugt 2 Tripletts aus einem Singulett-Exziton",
        answerB = "Upconversion: Zwei Infrarot-Photonen werden zu einem sichtbaren Photon kombiniert",
        answerC = "Förster-Resonanzenergietransfer zwischen Quantenpunkten verstärkt die Absorption",
        answerD = "Multiple Exciton Generation (MEG): Ein hochenergetisches Photon erzeugt durch Auger-artigen Prozess mehrere Elektron-Loch-Paare; theoretisch können >100 % EQE erreicht werden, da ein UV-Photon bis zu 3 Exzitonen generieren kann",
        correctAnswer = 3, // D
        explanation = "MEG (auch IMAMEE = Impact-Ionization) tritt auf, wenn die Photonenenergie ≥2Eg beträgt: Das primäre 'heiße' Exziton gibt überschüssige Energie durch Stoßionisation an valenzgebundene Elektronen ab, statt sie als Phononen zu verlieren. In PbS-Quantenpunkten wurde MEG-Schwellwert bei ≈2,3Eg gemessen. Praktische Nutzung ist jedoch durch schnelle Auger-Rekombination limitiert.",
        difficulty = 4,
        funFact = null
    ),

    // Question 35 — Organic photovoltaics  [correct = C → 2]
    Question(
        categoryId = 2,
        questionText = "Warum müssen in organischen Solarzellen (OPV) Donor- und Akzeptor-Materialien in einer Bulk-Heterojunction-Morphologie gemischt werden, und welcher Prozess limitiert typischerweise die externe Quanteneffizienz?",
        answerA = "Donor und Akzeptor müssen gemischt werden, um mechanische Stabilität zu gewährleisten; die EQE wird durch Absorption limitiert",
        answerB = "Die Mischung verhindert Photooxidation; der limitierende Prozess ist geminate Rekombination an Farbstoffmolekülen",
        answerC = "Da Exzitonen in organischen Halbleitern stark gebunden sind (Exziton-Bindungsenergie 0,3–1 eV) und kurze Diffusionslängen (5–20 nm) haben, müssen Donor-Akzeptor-Grenzflächen in Abständen < Diffusionslänge vorhanden sein (BHJ-Morphologie); typisch limitierend: bimolekulare Ladungsträger-Rekombination (Langevin-Rekombination) und unzureichende Ladungsträgermobilität",
        answerD = "BHJ-Morphologie dient ausschließlich der Verbesserung der Transparenz; Ladungsträgermobilität ist irrelevant für OPV-Effizienz",
        correctAnswer = 2, // C
        explanation = "Im Gegensatz zu anorganischen Halbleitern (Exziton-BE < kT) ist die Exziton-Bindungsenergie in organischen Halbleitern 0,3–1 eV. Exzitonen diffundieren mit kurzer Diffusionslänge (L_D ≈ 10 nm) und müssen vor Rekombination eine Donor-Akzeptor-Grenzfläche erreichen, um durch LUMO-HOMO-Energie-Offset (≥0,3 eV) in freie Ladungsträger getrennt zu werden. BHJ mit ~100 nm-Phasenseparation ist der Kompromiss.",
        difficulty = 4,
        funFact = "Die erste BHJ-Solarzelle (Yu, Heeger 1995) hatte <3 % PCE — heute erreichen Non-Fullerene-Akzeptor-BHJs >19 %."
    ),

    // Question 36 — Hot carrier cells  [correct = A → 0]
    Question(
        categoryId = 2,
        questionText = "Was ist das Kernprinzip einer Heißträger-Solarzelle (Hot Carrier Solar Cell), und warum übersteigt ihr theoretisches Effizienzlimit (≈66 % für einstrahliges Sonnenlicht) das Shockley-Queisser-Limit erheblich?",
        answerA = "Heißträger werden extrahiert, bevor sie thermalisieren (Phonon-Emission), d.h. ihre überschüssige kinetische Energie wird als elektrische Energie genutzt statt als Wärme dissipiert; dies erfordert Energy-Selective-Contacts und ultrakurze Extraktionszeiten (sub-ps)",
        answerB = "Heißträgerzellen verwenden supraleitende Elektroden, die Phonon-Emission unterdrücken",
        answerC = "Das Prinzip basiert auf thermoelektrischer Konversion des Temperaturgradienten zwischen heißem Absorber und kalter Elektrode",
        answerD = "Heißträgerzellen nutzen stimulierte Emission (wie Laser) statt spontane Elektron-Loch-Rekombination",
        correctAnswer = 0, // A
        explanation = "Im konventionellen Halbleiter thermalisieren heiße Träger in ~1 ps durch Carrier-Phonon-Streuung (Elektronenenergie → Wärme). In Heißträgerzellen (Ross & Nozik, 1982) sollen Energy-Selective-Contacts nur Träger einer bestimmten Energie extrahieren — ähnlich Carnot-Effizienz für Photovoltaik. Praktische Umsetzung scheitert bisher an der Schwierigkeit, Thermalisierung ausreichend zu verlangsamen.",
        difficulty = 4,
        funFact = null
    ),

    // ── Advanced Biophysics 9 ──────────────────────────────────────────────────
    // Question 37 — Optical tweezers applications  [correct = B → 1]
    Question(
        categoryId = 2,
        questionText = "Welchen physikalischen Mechanismus nutzen optische Pinzetten (Optical Tweezers) zur Manipulation einzelner Moleküle, und welche wichtige biologische Erkenntnis lieferten sie über motorische Proteine?",
        answerA = "Magnetisches Dipolfeld eines fokussierten Lasers zieht diamagnetische Partikel an; Myosin-Schrittgröße wurde zu 50 nm bestimmt",
        answerB = "Gradient-Kraft im Fokus eines Laserstrahls (Strahlungsdruck-Gradient) fängt dielektrische Partikel; an einzelnen Kinesin-Molekülen wurden diskrete 8-nm-Schritte entlang Mikrotubuli und eine Stall-Force von ≈6 pN gemessen",
        answerC = "Van-der-Waals-Kräfte zwischen Laseroptik und Fluorochrom-markierten Proteinen; RNA-Polymerase-Schrittgröße von 0,34 nm/bp wurde bestimmt",
        answerD = "Photoelektrischer Effekt: Laserphotonen ionisieren Partikel, die dann elektrostatisch eingeschlossen werden",
        correctAnswer = 1, // B
        explanation = "Optische Pinzetten nutzen die Gradient-Kraft: Im fokussierten Gaußschen Strahl zieht der Intensitätsgradient dielektrische Kugeln zum Fokus (Fokalpunkt = Energieminimum für Partikel mit n > n_medium). Block et al. (1990) maßen erstmals Kinesin-Einzelmolekül-Mechanik: 8-nm-Schritte (entspricht Tubulin-Dimer-Länge) und ~5–7 pN Stall-Force.",
        difficulty = 4,
        funFact = "Arthur Ashkin erhielt 2018 den Physik-Nobelpreis für die Erfindung optischer Pinzetten — im Alter von 96 Jahren, der älteste Nobelpreisträger aller Zeiten."
    ),

    // Question 38 — Patch clamp details  [correct = C → 2]
    Question(
        categoryId = 2,
        questionText = "Welche vier Konfigurationen der Patch-Clamp-Technik gibt es, und was misst die 'Inside-Out'-Konfiguration spezifisch?",
        answerA = "Whole-Cell, Perforiert, Außen-Seite-Außen, Locker-Patch; Inside-Out misst Kanalströme auf der äußeren Membranseite",
        answerB = "Cell-Attached, Whole-Cell, Outside-Out, Makropatch; Inside-Out misst Gesamtmembrankapazität",
        answerC = "Cell-Attached, Whole-Cell, Inside-Out, Outside-Out; Inside-Out exponiert die intrazelluläre Membranseite der Bad-Lösung — ideal zur Untersuchung intrazellulärer Regulatoren (Ca²⁺, ATP, second messenger) auf Kanalaktivität im Einzelkanal-Modus",
        answerD = "Nur Whole-Cell und Cell-Attached sind etablierte Konfigurationen; Inside-Out und Outside-Out sind Artefakte",
        correctAnswer = 2, // C
        explanation = "Neher & Sakmann (Nobelpreis 1991) entwickelten vier Konfigurationen: (1) Cell-Attached: Pipette auf intakter Zelle; (2) Whole-Cell: Membran durchbrochen, Gesamtstrom; (3) Inside-Out: Patch aus Zelle herausgezogen, Innenseite zugänglich; (4) Outside-Out: Membranstreifen mit Außenseite nach außen. Inside-Out erlaubt definierte Kontrolle der intrazellulären Seite (z.B. [Ca²⁺], cAMP) bei Einzelkanal-Auflösung.",
        difficulty = 4,
        funFact = null
    ),

    // Question 39 — FRET applications  [correct = D → 3]
    Question(
        categoryId = 2,
        questionText = "Welche molekulare Information liefert FRET (Förster Resonance Energy Transfer) in der Einzelmolekül-Biophysik, und von welcher Beziehung zwischen Effizienz E und Abstand r hängt die Methode ab?",
        answerA = "FRET misst absolute Proteinkonzentrationen; E ist linear abhängig von r: E = r/R₀",
        answerB = "FRET detektiert Mutationsstellen in DNA; Effizienz folgt einer Gaussian-Kurve",
        answerC = "FRET misst Ligandenbindungsaffinitäten; E hängt von der Dipolausrichtung ab (κ² = 2/3 angenommen)",
        answerD = "FRET misst molekulare Abstände (1–10 nm) durch nicht-strahlenden Energietransfer zwischen Donor und Akzeptor-Fluorophor; E = 1/[1+(r/R₀)⁶] — extrem abstandsabhängig (6. Potenz), weshalb FRET als 'spektroskopisches Lineal' gilt; smFRET an einzelnen RNA-Faltungsintermediaten zeigt diskrete Konformationszustände",
        correctAnswer = 3, // D
        explanation = "Förster (1948) leitete die E = 1/[1+(r/R₀)⁶]-Formel aus Dipolkopplung her. R₀ (Förster-Radius, typisch 2–10 nm) ist der Abstand bei 50 % Effizienz. Die r⁻⁶-Abhängigkeit macht FRET zum präzisen Abstandsmesser im Nanometerbereich. smFRET (Ha et al., 1996) verfolgt Konformationsänderungen einzelner Moleküle in Echtzeit — z.B. RNA-Helicase-Schritte, DNA-Hairpin-Fluktuationen.",
        difficulty = 4,
        funFact = "Der Förster-Radius R₀ für Cy3/Cy5 (gängiges FRET-Paar) beträgt ~5,4 nm — fast exakt die Dicke einer Lipiddoppelschicht."
    ),

    // ── Marine Geochemistry 10 ─────────────────────────────────────────────────
    // Question 40 — Ocean alkalinity  [correct = A → 0]
    Question(
        categoryId = 2,
        questionText = "Was ist die Gesamtalkalität (Total Alkalinity, TA) des Meerwassers, und welche Rolle spielt sie im marinen Kohlenstoffzyklus?",
        answerA = "TA ist die Kapazität des Meerwassers, Protonen zu puffern (TA ≈ [HCO₃⁻] + 2[CO₃²⁻] + [B(OH)₄⁻] + …); hohe TA erhöht die CO₂-Aufnahmekapazität des Ozeans und kontrolliert den pH sowie die Karbonatsättigungszustand (Ω) für biogene Kalkbildner",
        answerB = "TA misst nur den pH-Wert des Meerwassers; sie hat keine Verbindung zum Kohlenstoffzyklus",
        answerC = "TA ist das Verhältnis von organischem zu anorganischem Kohlenstoff im Ozean",
        answerD = "TA bezeichnet die Salzkonzentration; höhere TA bedeutet höherer Salzgehalt",
        correctAnswer = 0, // A
        explanation = "TA = [HCO₃⁻] + 2[CO₃²⁻] + [B(OH)₄⁻] + [OH⁻] − [H⁺] + kleinere Terme. Typisch ~2300 µmol/kg im offenen Ozean. Das Karbonatsystem DIC und TA bestimmen pH und CO₃²⁻-Konzentration, damit die Aragonit-Sättigungszustand Ω_arag = [Ca²⁺][CO₃²⁻]/K'sp — kritisch für Korallen und Pteropoden. Ocean Acidification senkt TA-Verhältnis und bedroht Kalzifizierung.",
        difficulty = 4,
        funFact = "Ocean Alkalinity Enhancement (OAE) — Zugabe von verwittertem Gestein ins Meer — wird als Methode zur CO₂-Entnahme erforscht und nutzt direkt die TA-CO₂-Chemie."
    ),

    // Question 41 — Carbonate compensation depth  [correct = C → 2]
    Question(
        categoryId = 2,
        questionText = "Was ist die Karbonatkompensationstiefe (Carbonate Compensation Depth, CCD) im Ozean, und welcher Mechanismus erklärt ihre vertikale Position?",
        answerA = "Die CCD ist die Tiefe, bei der Sauerstoff vollständig aus dem Tiefenwasser verschwindet; Mechanismus: biogener Sauerstoffverbrauch übersteigt physikalische Lösung",
        answerB = "Die CCD ist die Druckzone, bei der Kalziumionen vollständig als Ionenpaar mit Sulfat vorliegen und nicht mehr für Kalkausfällung zur Verfügung stehen",
        answerC = "Die CCD (typisch 3,5–5 km) ist die Tiefe, bei der die Auflösungsrate von Kalziumkarbonat (CaCO₃) gleich der Zufuhr aus dem Pelagial entspricht; mit steigender Tiefe sinkt Temperatur (erhöht CO₂-Löslichkeit und Acidität) und steigt Druck (erhöht K'sp) — Ω_calcit sinkt unter 1 und CaCO₃ löst sich vollständig auf",
        answerD = "Die CCD ist die Thermokline, unterhalb derer keine biologische Kalkproduktion stattfindet",
        correctAnswer = 2, // C
        explanation = "Unterhalb der Lysokline (partielle Auflösung) und CCD liegt das Ω_calcit < 1: Die Löslichkeit von Kalzit/Aragonit steigt mit Druck und sinkender Temperatur (Drucklöslichkeit, Le-Chatelier). Biogene Kalkschalen (Foraminiferen, Coccolithen) lösen sich vollständig auf — darunter liegen rote Tonablagerungen statt karbonatreicher Ooze. Die CCD-Position variiert je nach Ozean und Zeit: im Pazifik ≈3,5 km, im Atlantik ≈4,5–5 km.",
        difficulty = 4,
        funFact = "Während des Paläozän-Eozän-Thermalen Maximums (PETM) vor 56 Millionen Jahren stieg die CCD um mehrere Kilometer, was massenhaftes Auflösen von Meeresbodensedimenten verursachte — ein warnendes Beispiel für die Auswirkungen rascher CO₂-Freisetzung."
    ),

    // ── ADDITIONAL EXPERT QUESTION ────────────────────────────────────────────

    // Question — CRISPR-Cas9: Off-Target-Effekte und Spezifitaet  [correct: B → correctAnswer=1]
    Question(
        categoryId = 2,
        questionText = "CRISPR-Cas9 revolutioniert die Gentechnik, aber Off-Target-Schnitte sind ein kritisches Sicherheitsproblem. Welche molekularen Mechanismen ermoeglichen Off-Target-Aktivitaet, und welche High-Fidelity-Strategien reduzieren sie?",
        answerA = "Off-Target-Effekte entstehen ausschliesslich durch Fehler in der chemischen Synthese der sgRNA; High-Fidelity-Ansatz: Verwendung von RNA statt DNA als Template fuer die Cas9-Beladung, wodurch Synthesefehler eliminiert werden",
        answerB = "Off-Target-Aktivitaet entsteht durch tolerierte Fehlpaarungen zwischen sgRNA und genomischer DNA (besonders in Positionen 1–12 proximal zum PAM) sowie durch unspezifische DNA-Bindung; High-Fidelity-Cas9-Varianten (eSpCas9, HiFi Cas9) enthalten Mutationen, die die Wasserstoffbrueckenbindungskapazitaet zu ungepaarter DNA senken und so die Schnittaktivitaet von Substrat-Fehlpaarungen eliminieren",
        answerC = "Off-Target-Schnitte entstehen nur wenn Cas9 in der S-Phase des Zellzyklus aktiv ist, weil dort die DNA-Doppelstrangstruktur entspiralisiert ist; High-Fidelity-Ansatz: Zellzyklus-spezifische Cas9-Aktivierung durch synthetische G1-Phase-gebundene Promotoren",
        answerD = "Off-Target-Effekte entstehen durch immunologische Reaktionen des Wirts auf das bakterielle Cas9-Protein, das im menschlichen Zellkern als Fremdprotein erkannt wird und unspezifische Nukleasen aktiviert; Loesung: Humanisierung der Cas9-Aminosaeursequenz durch synonyme Codon-Optimierung",
        correctAnswer = 1, // B
        explanation = "CRISPR-Cas9-Off-Target-Effekte entstehen durch molekulare Toleranz des Cas9-sgRNA-Komplexes gegenueber Fehlpaarungen. Die PAM-proximalen Positionen (1–12 nt der sgRNA, 3'-PAM-Seite) sind fuer Spezifitaet kritisch; PAM-distale Positionen (13–20) tolerieren mehr Fehlpaarungen. Cas9 kann an Stellen mit bis zu 5 Fehlpaarungen schneiden, besonders wenn GU-Wobble-Paarungen oder nahe PAM-Sequenzen vorliegen. High-Fidelity-Varianten: SpCas9-HF1 (Kleinstner et al. 2016) mit Mutationen N497A/R661A/Q695A/Q926A reduziert unspezifische DNA-Kontakte. eSpCas9 (Slaymaker et al. 2016) mit K848A/K1003A/R1060A reduziert nichtspezifische positive Ladungen. Beide Varianten behalten On-Target-Aktivitaet bei, eliminieren aber die meisten Off-Target-Schnitte. Zusaetzliche Strategien: Cas9-Nickase-Paare, truncated sgRNAs (17-18 nt), Anti-CRISPR-Proteine.",
        difficulty = 4,
        funFact = "Das menschliche Genom enthaelt ~3 Milliarden Basenpaare. Ein CRISPR-Cas9-System, das eine 20-Nukleotid-Sequenz sucht, muss theoretisch 150 Millionen moegliche PAM-Positionen im Genom pruefen. Dabei genuegt schon eine Chance von 1:1000 fuer Off-Target-Schnitte, um mehrere hundert ungewollte Schnitte pro Zelle zu verursachen."
    ),

)

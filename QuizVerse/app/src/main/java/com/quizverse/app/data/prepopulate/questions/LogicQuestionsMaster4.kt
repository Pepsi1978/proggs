package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun logicQuestionsMaster4(): List<Question> = listOf(

    // --- MODALLOGIK & KRIPKE-SEMANTIK ---

    // Question 1
    Question(
        categoryId = 12,
        questionText = "In der Kripke-Semantik der Modallogik besteht ein Modell aus einem Tripel (W, R, V). Was repräsentiert die Relation R in diesem Modell?",
        answerA = "Die Zugänglichkeitsrelation (Erreichbarkeitsrelation) zwischen möglichen Welten, die bestimmt, welche Welten von einer gegebenen Welt aus 'gesehen' werden",
        answerB = "Eine Wahrheitswertzuweisung für alle Aussagenvariablen in allen Welten",
        answerC = "Die Menge aller wahren Aussagen in der aktualen Welt",
        answerD = "Eine Ordnungsrelation zwischen Welten nach ihrer Ähnlichkeit zur tatsächlichen Welt",
        correctAnswer = 0,
        explanation = "Ein Kripke-Modell M = (W, R, V) besteht aus: W = Menge möglicher Welten, R ⊆ W×W = Zugänglichkeitsrelation (w R v bedeutet: von Welt w aus ist Welt v zugänglich), V = Bewertungsfunktion (V(p, w) ∈ {wahr, falsch}). Eine modale Aussage □φ ist in Welt w wahr genau dann, wenn φ in allen Welten v wahr ist, für die gilt w R v. Für ◇φ: φ ist in mindestens einer zugänglichen Welt wahr.",
        difficulty = 5,
        funFact = "Saul Kripke entwickelte diese Semantik mit 18 Jahren als Gymnasiast — seine erste Veröffentlichung zur Modallogik erschien 1959, bevor er sein Studium in Harvard begann."
    ),

    // Question 2
    Question(
        categoryId = 12,
        questionText = "Das modallogische System S5 unterscheidet sich von S4 durch eine zusätzliche Axiomenschemata. Welches Axiom charakterisiert S5 und was bedeutet es semantisch?",
        answerA = "Axiom 5: ◇φ → □◇φ — wenn etwas möglich ist, dann ist es notwendig möglich; semantisch: die Zugänglichkeitsrelation ist eine Äquivalenzrelation",
        answerB = "Axiom D: □φ → ◇φ — jede notwendige Aussage ist möglich",
        answerC = "Axiom B: φ → □◇φ — was wahr ist, ist notwendigerweise möglich",
        answerD = "Axiom T: □φ → φ — was notwendig ist, ist wahr",
        correctAnswer = 0,
        explanation = "S5 fügt zu S4 das Axiom 5 hinzu: ◇φ → □◇φ. Dies bedeutet semantisch, dass die Zugänglichkeitsrelation R eine Äquivalenzrelation ist (reflexiv, symmetrisch, transitiv). In S5 kollabieren alle modalen Iterationen: □◇□φ ↔ □φ. S5 ist das stärkste der klassischen modalen Systeme und wird oft für logische oder metaphysische Notwendigkeit verwendet. S4 hat nur eine präordnungsartige Relation (reflexiv und transitiv).",
        difficulty = 5,
        funFact = "In S5 gilt: Wenn eine Aussage möglich ist, ist sie in allen möglichen Welten möglich. Das klingt trivial, hat aber tiefgreifende Konsequenzen für das ontologische Argument (Plantinga): Wenn Gottes Existenz möglich ist und Gott notwendig existiert, folgt in S5 die tatsächliche Existenz."
    ),

    // Question 3
    Question(
        categoryId = 12,
        questionText = "Was versteht man unter 'De-re'- versus 'De-dicto'-Modalität in der Philosophie der Sprache?",
        answerA = "De-dicto-Modalität betrifft die Notwendigkeit einer Aussage als ganzer ('Es ist notwendig, dass der Bundespräsident aus Deutschland stammt'); De-re-Modalität betrifft die notwendigen Eigenschaften eines Gegenstandes selbst ('Der Bundespräsident, wer auch immer das ist, stammt notwendigerweise aus Deutschland')",
        answerB = "De-re-Modalität bezieht sich auf die Notwendigkeit einer Aussage als ganzer; De-dicto-Modalität auf die Eigenschaft eines einzelnen Objekts",
        answerC = "Beide Begriffe sind bedeutungsgleich und werden synonym verwendet",
        answerD = "De-re bezieht sich auf vergangene, De-dicto auf zukünftige modale Aussagen",
        correctAnswer = 0,
        explanation = "Der Unterschied ist subtil aber wichtig: De-dicto (lat. 'über das Gesagte'): □[Der F ist G] — die ganze Aussage steht im Bereich des Modaloperators. De-re (lat. 'über die Sache'): [Der F] □ist G — das Objekt selbst hat eine notwendige Eigenschaft. Beispiel: De-dicto: 'Notwendigerweise ist der größte Planet eine Kugel' (wahr, analytisch). De-re: 'Jupiter ist notwendigerweise eine Kugel' (kontrovers — könnte Jupiter so entstanden sein, dass er keine Kugel ist?).",
        difficulty = 5,
        funFact = "W.V.O. Quine lehnte De-re-Modalität grundsätzlich ab, weil sie seiner Meinung nach den Begriff der Essenz (was ein Objekt notwendigerweise ist) voraussetzt, der philosophisch nicht sauber bestimmbar sei. Kripke rehabilitierte De-re-Modalität in 'Naming and Necessity' (1980)."
    ),

    // Question 4
    Question(
        categoryId = 12,
        questionText = "Was sind 'starre Bezeichnungen' (rigid designators) nach Saul Kripke und warum sind Eigennamen Beispiele dafür?",
        answerA = "Eigennamen sind starre Bezeichnungen, weil sie in allen möglichen Welten dasselbe Objekt bezeichnen, in denen dieses Objekt existiert — im Gegensatz zu Kennzeichnungen wie 'der größte Philisoph'",
        answerB = "Starre Bezeichnungen sind solche, deren Bedeutung durch Definition festgelegt ist",
        answerC = "Eigennamen sind starr, weil sie etymologisch unveränderlich sind",
        answerD = "Starre Bezeichnungen bezeichnen immer notwendig existierende Objekte",
        correctAnswer = 0,
        explanation = "Kripke (Naming and Necessity, 1970/1980) argumentiert: Ein Ausdruck ist ein 'rigid designator', wenn er in allen möglichen Welten dasselbe Objekt bezeichnet (sofern das Objekt existiert). 'Aristoteles' bezeichnet in allen möglichen Welten genau den griechischen Philosophen, nicht aber etwa 'der Lehrer Alexanders des Großen' — denn in einer möglichen Welt könnte ein anderer Lehrer Alexanders sein. Kennzeichnungen (descriptions) sind typisch nicht-starr ('der US-Präsident 2024' bezeichnet in anderen Welten andere Personen).",
        difficulty = 5,
        funFact = "Kripkes Theorie widerlegt Freges Unterscheidung von Sinn und Bedeutung für Eigennamen: 'Phosphorus' (Abendstern) und 'Hesperus' (Morgenstern) haben laut Frege verschiedenen Sinn, aber Kripke zeigt, dass sie in allen möglichen Welten dieselbe Venus bezeichnen — ihre Identität ist notwendig, nicht kontingent."
    ),

    // --- PRÄDIKATENLOGIK HÖHERER STUFE ---

    // Question 5
    Question(
        categoryId = 12,
        questionText = "Warum kann die Peano-Arithmetik in Prädikatenlogik erster Stufe (PA₁) nicht vollständig axiomatisiert werden, wohl aber mit Hilfe der Prädikatenlogik zweiter Stufe (PA₂)?",
        answerA = "PA₂ kann über Mengen von natürlichen Zahlen quantifizieren und damit das Induktionsaxiom als einzelne Aussage formulieren; PA₁ braucht ein unendliches Axiomenschema. PA₂ hat ein einziges kategorisches Modell (bis auf Isomorphie), PA₁ hat Nicht-Standard-Modelle",
        answerB = "PA₁ hat keinen vollständigen Beweiskalkül, PA₂ schon — deshalb ist PA₂ stärker",
        answerC = "PA₁ ist vollständig, hat aber kein Modell; PA₂ ist unvollständig, hat aber ein Modell",
        answerD = "Der Unterschied liegt nur in der Ausdrucksstärke für Primzahlen",
        correctAnswer = 0,
        explanation = "In PA₂ lautet das Induktionsaxiom: ∀X[(X(0) ∧ ∀n(X(n) → X(n+1))) → ∀n X(n)] — eine einzige Aussage, die über alle Teilmengen quantifiziert. In PA₁ ist es ein Schema mit unendlich vielen Axiomen, eines für jede definierbare Eigenschaft. PA₂ ist kategorisch: Alle Modelle von PA₂ sind isomorph zu ℕ. PA₁ hat per Löwenheim-Skolem abzählbare und überabzählbare Modelle mit 'unendlich großen' Zahlen.",
        difficulty = 5,
        funFact = "Der Preis für die Kategoriezität von PA₂: Es gibt keinen vollständigen rekursiv aufzählbaren Beweiskalkül. Logik zweiter Stufe ist nach Gödels Unvollständigkeitssatz wesentlich schwerer als Logik erster Stufe — Kategoriezität und Vollständigkeit schließen sich aus."
    ),

    // Question 6
    Question(
        categoryId = 12,
        questionText = "Was besagt der Satz von Lindström (1969) über die Einzigartigkeit der Prädikatenlogik erster Stufe?",
        answerA = "Prädikatenlogik erster Stufe ist (bis auf Äquivalenz) die stärkste Logik, die sowohl den Gödel-Vollständigkeitssatz als auch den abwärts gerichteten Löwenheim-Skolem-Satz erfüllt — jede Erweiterung verliert eine dieser Eigenschaften",
        answerB = "Prädikatenlogik erster Stufe ist die schwächste Logik mit dem Vollständigkeitssatz",
        answerC = "Nur Prädikatenlogik erster Stufe hat einen entscheidbaren Theorembeweis",
        answerD = "Der Satz gilt nur für einsortige Logiken ohne Gleichheit",
        correctAnswer = 0,
        explanation = "Lindström (1969) bewies: Sei L eine Logik, die (1) alle FO-Formeln ausdrückt, (2) den Vollständigkeitssatz erfüllt (konsistente Theorien haben Modelle) und (3) den abwärts L-S-Satz erfüllt (abzählbare Theorien haben abzählbare Modelle). Dann ist L äquivalent zu FO. Jede echte Erweiterung (etwa Logik mit dem Quantor 'Es gibt unabzählbar viele') verliert entweder Vollständigkeit oder L-S. Dieser Satz erklärt den 'ausgezeichneten Status' von FO in der Mathematik.",
        difficulty = 5,
        funFact = "Per Lindström hat die Prädikatenlogik erster Stufe einen einzigartigen 'Sweet Spot': stark genug für fast alle Mathematik, aber schwach genug für effektive Beweisführung. Monadische Logik zweiter Stufe ist entscheidbar für Bäume, aber verliert den L-S-Satz."
    ),

    // Question 7
    Question(
        categoryId = 12,
        questionText = "Was ist das Skolem-Paradoxon und wie wird es aufgelöst?",
        answerA = "ZFC beweist die Existenz überabzählbarer Mengen; der Löwenheim-Skolem-Satz garantiert ein abzählbares Modell von ZFC. Auflösung: 'Abzählbarkeit' ist modellrelativ — im Modell gibt es keine bijektive Funktion auf ℕ, aber diese Funktion existiert im Metatheorie-Universum",
        answerB = "Das Paradoxon entsteht dadurch, dass Mengenlehre in einer abzählbaren Sprache formuliert wird, aber überabzählbare Mengen beweist",
        answerC = "Das Paradoxon zeigt, dass ZFC inkonsistent ist",
        answerD = "Das Paradoxon beweist, dass überabzählbare Mengen nicht existieren können",
        correctAnswer = 0,
        explanation = "Das Skolem-Paradoxon (Skolem 1922): ZFC hat (wenn konsistent) ein abzählbares Modell M. In M gilt 'ℝ ist überabzählbar'. Aber M selbst ist abzählbar! Auflösung: 'Überabzählbar' bedeutet 'keine bijektive Funktion auf ℕ existiert'. In M gibt es keine solche Funktion (als Element von M), aber im Meta-Universum gibt es eine solche Bijektion. Die Nicht-Existenz der Bijektion ist modell-intern, ihre Existenz ist meta-extern. Dies zeigt die fundamentale Relativität mengentheoretischer Konzepte.",
        difficulty = 5,
        funFact = "Skolem selbst folgerte aus diesem Resultat, dass Mengen kein absolutes mathematisches Fundament bilden können. Er favorisierte Berechenbarkeitstheorie als robusteres Fundament — eine Ansicht, die als 'Skolemismus' bekannt ist."
    ),

    // --- SELBSTREFERENZ & PARADOXA ---

    // Question 8
    Question(
        categoryId = 12,
        questionText = "Was ist Currys Paradoxon und worin unterscheidet es sich vom klassischen Lügner-Paradoxon?",
        answerA = "Currys Paradoxon verwendet keine Negation: Betrachte den Satz C = 'Wenn C wahr ist, dann ist Paris die Hauptstadt Australiens'. Aus der Annahme, C sei wahr, folgt durch Modus Ponens, dass Paris die Hauptstadt Australiens ist — jede beliebige Aussage ist beweisbar",
        answerB = "Currys Paradoxon ist eine Variante des Lügner-Paradoxons mit zwei Sätzen",
        answerC = "Currys Paradoxon zeigt, dass Selbstreferenz immer zu einem Widerspruch führt",
        answerD = "Currys Paradoxon beweist, dass die klassische Logik inkonsistent ist",
        correctAnswer = 0,
        explanation = "Curry (1942): Sei C ↔ (C → ⊥) für beliebiges ⊥. Moderner: C = 'Wenn C wahr ist, dann gilt X' für beliebiges X. Beweis: (1) Annahme: C ist wahr. (2) Also gilt: wenn C wahr ist, dann X (per Definition von C). (3) Also gilt X (Modus Ponens aus 1,2). (4) Aus 1-3 folgt: Wenn C wahr ist, dann X. (5) Also ist C wahr (per Definition). (6) Also gilt X. Das Paradoxon braucht keine Negation und ist daher robuster als das Lügner-Paradoxon gegen Dialethismus (die Theorie, die wahre Widersprüche akzeptiert).",
        difficulty = 5,
        funFact = "Currys Paradoxon zeigt, dass naive Komprehensionstaxiome nicht nur die Russellsche Antinomie erzeugen, sondern jede Aussage beweisbar machen. Parakonsistente Logiken müssen Currys Paradoxon blockieren, indem sie entweder Modus Ponens oder die Kontraktionsregel einschränken."
    ),

    // Question 9
    Question(
        categoryId = 12,
        questionText = "Wie löst Alfred Tarski das Lügner-Paradoxon durch seine Theorie der Wahrheit?",
        answerA = "Tarski unterscheidet streng zwischen Objektsprache und Metasprache: 'Schnee ist weiß' ist ein Objekt-Satz, '"Schnee ist weiß" ist wahr' ist ein Meta-Satz. Der Wahrheitsprädikat T darf nur in der Metasprache auf Sätze der Objektsprache angewendet werden — Selbstanwendung ist syntaktisch verboten",
        answerB = "Tarski zeigt, dass das Lügner-Paradoxon keinen echten Widerspruch erzeugt, wenn man dreiwertige Logik verwendet",
        answerC = "Tarski beweist, dass das Lügner-Paradoxon eine falsche Aussage ist",
        answerD = "Tarski eliminiert das Wahrheitsprädikat aus der Logik vollständig",
        correctAnswer = 0,
        explanation = "Tarskis Wahrheitsdefinition ('Der Wahrheitsbegriff in den formalisierten Sprachen', 1936): Eine Sprache L kann kein eigenes Wahrheitsprädikat ausdrücken. Die Wahrheit für L wird in einer Metasprache L' definiert. Das T-Schema: T(⌜φ⌝) ↔ φ gilt für alle Sätze φ der Objektsprache, aber nicht für Sätze der Metasprache selbst. 'Dieser Satz ist falsch' ist in einer hierarchisch aufgebauten Sprache nicht formulierbar, da kein Satz auf seine eigene Wahrheit in derselben Sprachstufe referieren kann.",
        difficulty = 5,
        funFact = "Tarski bewies auch, dass die Wahrheit für die Arithmetik in der Arithmetik selbst nicht definierbar ist (Undefinierbarkeitssatz) — ein engverwandter Satz zu Gödels Unvollständigkeitssatz, der jedoch von Tarski früher und auf andere Weise bewiesen wurde."
    ),

    // Question 10
    Question(
        categoryId = 12,
        questionText = "Was ist das Jourdain-Kartenproblem und warum ist es eine interessante Variante des Lügner-Paradoxons?",
        answerA = "Das Kartenproblem ist ein Rätsel über Wahrscheinlichkeiten, kein logisches Paradoxon",
        answerB = "Das Problem beweist, dass jede Art von indirekter Selbstreferenz paradox ist",
        answerC = "Eine Karte hat auf der Vorderseite 'Der Satz auf der Rückseite ist wahr' und auf der Rückseite 'Der Satz auf der Vorderseite ist falsch'. Es entsteht dasselbe Paradoxon ohne direkten Selbstbezug — zeigt, dass gegenseitige Referenz genügt",
        answerD = "Das Kartenproblem hat eine eindeutige Lösung: Beide Sätze sind sinnlos",
        correctAnswer = 2,
        explanation = "Philip Jourdains Kartenproblem (1913): Karte A: 'Der Satz auf Karte B ist wahr'. Karte B: 'Der Satz auf Karte A ist falsch'. Wenn A wahr ist → B ist wahr → A ist falsch → Widerspruch. Wenn A falsch ist → B ist falsch → A ist wahr → Widerspruch. Das Paradoxon entsteht ohne direkte Selbstreferenz, nur durch zirkuläre gegenseitige Referenz. Dies zeigt, dass Tarskis Hierarchielösung nicht nur direkte Selbstreferenz, sondern alle Zirkularitäten verbieten muss.",
        difficulty = 5,
        funFact = "Yablo (1993) konstruierte ein Paradoxon mit einer unendlichen Folge von Sätzen S₁, S₂, ..., wobei Sₙ sagt 'Für alle m > n ist Sₘ falsch'. Diese Folge ist paradox, obwohl kein Satz auf sich selbst referiert — direkte oder indirekte Zirkularität ist keine notwendige Bedingung für Paradoxa."
    ),

    // Question 11
    Question(
        categoryId = 12,
        questionText = "Was ist die Russellsche Antinomie und welche zwei Hauptlösungsstrategien gibt es?",
        answerA = "Das Paradoxon wird durch dreiwertige Logik gelöst: R hat einen undefinierten Wahrheitswert",
        answerB = "Das Paradoxon ist kein echtes Problem, weil R keine Menge ist",
        answerC = "Russells Lösung bestand darin, Mengen durch Prädikate zu ersetzen",
        answerD = "R = {x | x ∉ x} führt zu R ∈ R ↔ R ∉ R. Lösungen: (1) Typentheorie (Russell): Objekte werden in Typen eingeteilt, Mengen können nur Objekte niedrigerer Typen enthalten; (2) ZFC-Axiomatik: Das Aussonderungsaxiom erlaubt nur Teilmengen bestehender Mengen, nicht beliebige Klassen",
        correctAnswer = 3,
        explanation = "Russells Paradoxon (1901): Die Menge aller Mengen, die sich nicht selbst enthalten: R = {x | x ∉ x}. Dann gilt R ∈ R ↔ R ∉ R — ein Widerspruch. Lösung 1 (Typentheorie, Russell 1908): Objekte sind in Typen geschichtet. Typ 0: Individuen. Typ 1: Mengen von Individuen. Typ n+1: Mengen von Typ-n-Objekten. Mengen können nur Objekte des unmittelbar niedrigeren Typs enthalten — R ist nicht formulierbar. Lösung 2 (ZFC): Nur 'Aussonderung' (∃y∀z(z∈y ↔ z∈x ∧ φ(z))) — keine beliebige Komprehension.",
        difficulty = 5,
        funFact = "Russell entdeckte das Paradoxon kurz vor Freges zweitem Band der 'Grundgesetze der Arithmetik' (1903). Frege ergänzte das Buch um ein Nachwort mit den Worten 'Einem Wissenschaftler kann kaum etwas Unerwünschteres begegnen, als wenn ihm gegen Ende der Arbeit das Fundament erschüttert wird.' — ein wissenschaftlich galanter Umgang mit einer vernichtenden Kritik."
    ),

    // Question 12
    Question(
        categoryId = 12,
        questionText = "Was ist der 'verstärkte Lügner' (strengthened liar) und warum scheitern einfache Lösungsstrategien daran?",
        answerA = "Der verstärkte Lügner ist eine besonders starke Form des Paradoxons, die Gödel-Nummerierung verwendet",
        answerB = "Der verstärkte Lügner lautet: 'Dieser Satz ist nicht wahr'. Lösungen via Wahrheitswertelücken (gap theories) scheitern, weil der Satz dann weder wahr noch falsch ist — aber dann ist er 'nicht wahr', was ihn paradox wahr macht",
        answerC = "Der verstärkte Lügner beweist, dass Parakonsistenz die einzige Lösung ist",
        answerD = "Der Begriff bezieht sich auf Lügner-Aussagen in Systemen mit mehr als drei Wahrheitswerten",
        correctAnswer = 1,
        explanation = "Gap-Strategien (Priest, Kripke): Der Lügner-Satz L = 'L ist falsch' bekommt den Wert 'unbestimmt' (weder wahr noch falsch). Problem: Der verstärkte Lügner L* = 'L* ist nicht wahr' (i.e. falsch oder unbestimmt). Wenn L* unbestimmt ist, dann ist L* nicht wahr → L* ist wahr — Widerspruch. Gap-Theorien können zwar den klassischen Lügner domestizieren, aber L* erzeugt ein neues Paradoxon auf einer höheren Metaebene. Dieses 'Revenge Problem' zeigt, dass naive Erweiterungen scheitern.",
        difficulty = 5,
        funFact = "Kripkes Lösungsversuch in 'Outline of a Theory of Truth' (1975) verwendet Fixpunkttheorie: Man beginnt mit keinen Wahrheitswerten und fügt sie iterativ hinzu, bis ein Fixpunkt erreicht wird. Dieser Ansatz scheitert am verstärkten Lügner, aber Kripkes Methode ist dennoch ein Meilenstein der philosophischen Logik."
    ),

    // --- KONTRAFAKTUALE LOGIK ---

    // Question 13
    Question(
        categoryId = 12,
        questionText = "David Lewis definiert die Wahrheitsbedingungen für kontrafaktuale Konditionale 'Wenn A der Fall gewesen wäre, wäre B der Fall gewesen' (A □→ B). Welche Bedingung ist korrekt?",
        answerA = "A □→ B ist wahr, wenn in der ähnlichsten möglichen Welt, in der A gilt, auch B gilt; 'ähnlichste' wird durch eine kontextuell bestimmte Ähnlichkeitsrelation auf möglichen Welten definiert",
        answerB = "A □→ B ist äquivalent zu □(A → B) in der Modallogik S5",
        answerC = "A □→ B ist wahr, wenn es keine mögliche Welt gibt, in der A und ¬B beide gelten",
        answerD = "A □→ B ist wahr, wenn A und B beide in der aktualen Welt wahr sind",
        correctAnswer = 0,
        explanation = "Lewis' Analyse (Counterfactuals, 1973): A □→ B ist wahr in der aktualen Welt w, wenn entweder (1) es keine A-Welten gibt (Vacuous truth), oder (2) es eine A-Welt gibt, die B enthält und die w-ähnlicher ist als jede A-Welt, in der ¬B gilt. Die Ähnlichkeitsrelation ≤_w auf Welten ist entscheidend. Stalnakers frühere Analyse (1968) nimmt genau eine 'nächste' A-Welt an; Lewis erlaubt unendlich viele gleich-ähnliche Welten (Limit assumption kann scheitern).",
        difficulty = 5,
        funFact = "Lewis' Ähnlichkeitsrelation ist kontextuell variabel: 'Wenn Nixon den Atomknopf gedrückt hätte, wäre ein Krieg ausgebrochen' — hier zählt politische Ähnlichkeit. 'Wenn die Erde keine Monde hätte, wäre sie größer' — hier zählt astronomische Ähnlichkeit. Kontrafaktuale sind radikaler kontextabhängig als man denkt."
    ),

    // Question 14
    Question(
        categoryId = 12,
        questionText = "Welches Problem stellt Nelson Goodman mit seinem 'Grue'-Paradoxon für kontrafaktuale und induktive Logik?",
        answerA = "Das Grue-Paradoxon zeigt, dass nicht alle Regelfolge induktiv projizierbar sind: 'Grue' bedeutet 'grün, wenn vor 2030 betrachtet, blau wenn danach'. Alle bisher beobachteten Smaragde sind sowohl grün als auch grue, aber 'alle Smaragde sind grün' und 'alle Smaragde sind grue' sind incompatible Verallgemeinerungen",
        answerB = "Das Paradoxon zeigt, dass Farbbegriffe nicht in der Logik formalisierbar sind",
        answerC = "Goodmans Paradoxon ist eine Variante des Hempel-Paradoxons über die Bestätigung von Naturgesetzen",
        answerD = "Das Grue-Paradoxon beweist, dass Induktion grundsätzlich ungültig ist",
        correctAnswer = 0,
        explanation = "Goodman (Fact, Fiction, and Forecast, 1955) definiert: Ein Ding ist 'grue', wenn es vor Zeit t grün ist oder nach t blau ist. Alle bisher beobachteten Smaragde (t noch nicht erreicht) sind sowohl grün als auch grue. Warum verallgemeinern wir zu 'alle Smaragde sind grün' statt 'alle Smaragde sind grue'? Goodman fordert eine Theorie 'verankerter' (entrenched) Prädikate: 'Grün' hat eine lange induktive Geschichte, 'Grue' nicht. Das Problem der 'Neuen Induktion' ist bis heute ungelöst.",
        difficulty = 5,
        funFact = "Goodmans Paradoxon hat direkte Relevanz für Maschinelles Lernen: Ein Algorithmus, der auf grünen Smaragden trainiert wurde, kann nicht zwischen 'grün' und 'grue' unterscheiden — das Overfitting-Problem ist philosophisch tief verankert im Problem der induktiven Projektion."
    ),

    // Question 15
    Question(
        categoryId = 12,
        questionText = "Was ist das Problem der 'möglichen Welten' als ontologische Theorie bei David Lewis (Modaler Realismus) und was ist die Hauptkritik?",
        answerA = "Lewis postuliert mögliche Welten als mentale Konstrukte des Denkens",
        answerB = "Lewis' Theorie behauptet, dass mögliche Welten Teilmengen der aktualen Welt sind",
        answerC = "Lewis behauptet: Alle möglichen Welten existieren genauso konkret wie unsere aktuale Welt ('Genuine Modal Realism'). Hauptkritik: Das epistemische Zugangsproblem — wie können wir Wissen über kausale unverbundene Welten haben, und warum ist unsere Welt 'aktuell'?",
        answerD = "Der modale Realismus ist eine rein formalistische Position ohne ontologische Implikationen",
        correctAnswer = 2,
        explanation = "Lewis (On the Plurality of Worlds, 1986): Es gibt unendlich viele mögliche Welten, jede genauso real wie unsere. Modaloperatoren werden damit zu Quantoren über Welten: 'Möglicherweise P' = 'Es gibt eine Welt, in der P'. Vorteile: Elegante Reduktion von Modallogik auf mengentheoretische Quantifikation. Kritiken: (1) Ontologische Überinflation — warum so viele Entitäten annehmen? (2) Kausalität: Welten sind kausal isoliert — wie rechtfertigen wir Glauben über sie? (3) Indexikalität: 'Aktuell' ist für jede Welt der Name ihrer eigenen Welt.",
        difficulty = 5,
        funFact = "Lewis' Entgegnung auf die Ontologie-Kritik: Modaler Realismus ist sparsamer als er wirkt — wir verzichten auf sui generis modale Tatsachen und erklären alles durch konkrete Welten. 'Ontological commitment is measured by the ideology, not the ontology' — der konzeptuelle Aufwand, nicht die Anzahl der Objekte, zählt."
    ),

    // --- PHILOSOPHISCHE LOGIK: DEDUKTION, INDUKTION, ABDUKTION ---

    // Question 16
    Question(
        categoryId = 12,
        questionText = "Was ist 'Abduktion' (Inferenz zur besten Erklärung) nach Charles Sanders Peirce und worin liegt ihr erkenntnistheoretischer Status?",
        answerA = "Abduktion ist ein deduktiv gültiger Schluss der Form: Wenn H, dann E; E ist wahr; also H",
        answerB = "Abduktion ist das Schließen von einer Beobachtung auf die plausibelste Erklärung: 'Das Ergebnis ist überraschend, aber wenn H wäre, wäre es selbstverständlich — also vermuten wir H'. Sie ist weder deduktiv gültig noch induktiv vollständig, aber erkenntnistheoretisch unverzichtbar",
        answerC = "Abduktion ist Peirces Name für Induktion durch Enumeration",
        answerD = "Abduktion ist in der formalen Logik äquivalent zu Modus Tollens",
        correctAnswer = 1,
        explanation = "Peirce unterschied drei Schlussformen: Deduktion (notwendig gültig, keine neuen Fakten), Induktion (vom Besonderen zum Allgemeinen, ampliativ), Abduktion (schließt auf die beste Erklärung). Schema der Abduktion: (1) E ist beobachtet (überraschend). (2) Wenn H, wäre E zu erwarten. (3) Also gibt es Grund, H zu vermuten. Abduktion ist der Motor der wissenschaftlichen Hypothesenbildung — wie ein Detektiv, der vom Tatort auf den Täter schließt. Sie ist rational, aber nicht garantiert korrekt.",
        difficulty = 5,
        funFact = "Sherlock Holmes' berühmter 'Deduktionismus' ist eigentlich Abduktion: Er beobachtet eine Schmutzfleck auf dem Schuh und schlussfolgert 'Sie waren in Afghanistan'. Das ist keine Deduktion (nicht logisch notwendig), sondern eine Inferenz zur besten Erklärung."
    ),

    // Question 17
    Question(
        categoryId = 12,
        questionText = "Was ist das 'Induktionsproblem' (Humes Problem) und wie lautet Poppers Antwort darauf?",
        answerA = "Das Induktionsproblem wurde von Hume durch das Prinzip der Gleichförmigkeit der Natur gelöst",
        answerB = "Hume: Kein induktiver Schluss vom Besonderen auf das Allgemeine ist logisch gültig; die Wiederholung von Beobachtungen rechtfertigt keine Naturgesetze. Popper: Wissenschaft induiziert nicht — sie falsifiziert. Gesetze sind mutige Vermutungen, die durch scheiternde Falsifizierungsversuche corroboriert werden",
        answerC = "Humes Problem gilt nur für moralische Schlüsse, nicht für empirische",
        answerD = "Poppers Antwort lautet: Wahrscheinlichkeitsrechnung löst das Induktionsproblem durch Bayes-Theorem",
        correctAnswer = 1,
        explanation = "Hume (Enquiry, 1748): Wir beobachten 1000 weiße Schwäne und schließen 'Alle Schwäne sind weiß'. Dieser Schluss ist nicht deduktiv gültig (keine logische Notwendigkeit). Die Annahme 'Die Natur ist gleichförmig' wird selbst induktiv begründet — Zirkelschluss. Popper (Logik der Forschung, 1934): Wissenschaft sucht Falsifikationen, nicht Bestätigungen. Eine Theorie ist wissenschaftlich, wenn sie falsifizierbar ist. Induktion ist nicht der Motor der Wissenschaft — kühne Hypothesen + strenge Tests schon.",
        difficulty = 5,
        funFact = "Nassim Taleb verweist auf 'schwarze Schwäne' (Black Swan) als Metapher für seltene, hochimpakte Ereignisse — ein direkter Verweis auf Humes Problem: Alle europäischen Schwäne waren weiß, bis 1697 in Australien schwarze Schwäne entdeckt wurden. Ein Gegenbeispiel reicht, um 'alle Schwäne sind weiß' zu falsifizieren."
    ),

    // Question 18
    Question(
        categoryId = 12,
        questionText = "Was ist der Unterschied zwischen 'formaler' und 'materialer' Logik in der aristotelischen Tradition?",
        answerA = "Materiale Logik ist ein veralteter Begriff für Psychologismus in der Logik",
        answerB = "Formale Logik untersucht die Formen gültiger Schlüsse unabhängig vom Inhalt; materiale Logik untersucht die inhaltliche Wahrheit der Prämissen und die spezifischen Schlussregeln für bestimmte Wissensbereiche",
        answerC = "Formale Logik gilt nur für Mathematik, materiale Logik für empirische Wissenschaften",
        answerD = "Der Unterschied ist rein terminologisch, beide bezeichnen dasselbe Gebiet",
        correctAnswer = 1,
        explanation = "Die Unterscheidung geht auf Kant zurück, der die allgemeine formale Logik von der speziellen materialen (Organon der Wissenschaften) trennte. Formale Logik: Untersucht Gültigkeit ohne Inhalt — der Schluss 'Alle A sind B; alle B sind C; also alle A sind C' ist gültig unabhängig davon, ob A, B, C Hunde, Zahlen oder Planeten sind. Materiale Logik: Inhaltliche Schlussregeln eines Fachgebiets — z.B. kausal-nomologische Schlüsse in der Physik, diachrone Schlüsse in der Geschichtswissenschaft.",
        difficulty = 5,
        funFact = "Hegel lehnte die formale Logik als 'Schullogik' ab und entwickelte seine 'Wissenschaft der Logik' als materiale Ontologie des Denkens selbst — Logik als Selbstentfaltung des Begriffs. Dies führte zu einem bis heute anhaltenden Streit zwischen analytischer und kontinentaler Philosophie."
    ),

    // Question 19
    Question(
        categoryId = 12,
        questionText = "Was ist das 'Rabe-Paradoxon' (Hempel-Paradoxon) und was zeigt es über Bestätigung und Induktion?",
        answerA = "Das Paradoxon zeigt, dass Bestätigung transitiv ist",
        answerB = "Jede Beobachtung eines nicht-schwarzen, nicht-raben-artigen Objekts (z.B. ein roter Schuh) bestätigt logisch 'Alle Raben sind schwarz', weil sie 'Alle nicht-schwarzen Dinge sind keine Raben' bestätigt, was logisch äquivalent ist",
        answerC = "Hempel löste das Paradoxon durch Bayes-Theorem vollständig",
        answerD = "Das Paradoxon beweist, dass Farbaussagen nicht induktiv bestätigbar sind",
        correctAnswer = 1,
        explanation = "Hempel (1945): 'Alle Raben sind schwarz' ist logisch äquivalent zu 'Alle nicht-schwarzen Dinge sind keine Raben'. Wenn ein roter Schuh 'Alle nicht-schwarzen Dinge sind keine Raben' bestätigt, muss er per Äquivalenz auch 'Alle Raben sind schwarz' bestätigen. Aber ein roter Schuh scheint 'Alle Raben sind schwarz' nicht zu bestätigen! Das Paradoxon zeigt das Spannungsverhältnis zwischen logischer Äquivalenz und unserer intuitiven Bestätigungstheorie. Bayesianismus erklärt: Ein roter Schuh bestätigt 'alle Raben sind schwarz', aber infinitesimal schwach.",
        difficulty = 5,
        funFact = "Das Paradoxon hat praktische Relevanz für Data Mining: Ein Algorithmus, der nach 'Alle X sind Y' sucht, sucht äquivalent nach Nicht-Y-Objekten, die keine X sind. Wie man Bestätigung gewichtet, ist keine rein logische, sondern eine Wahrscheinlichkeitsfrage."
    ),

    // Question 20
    Question(
        categoryId = 12,
        questionText = "Was ist der Unterschied zwischen einem 'gültigen' und einem 'stichhaltig/triftigen' Argument in der formalen Logik?",
        answerA = "Gültig und stichaltig bedeuten dasselbe — beide Begriffe beschreiben logisch korrekte Schlüsse",
        answerB = "Gültig bezieht sich auf deduktive, stichaltig auf induktive Argumente",
        answerC = "Ein Argument ist gültig (valid), wenn die Konklusion notwendig folgt, falls die Prämissen wahr sind — unabhängig davon, ob die Prämissen tatsächlich wahr sind. Ein Argument ist stichaltig/triftig (sound), wenn es gültig ist UND alle Prämissen tatsächlich wahr sind",
        answerD = "Stichaltig ist ein stärkerer Gültigkeitsbegriff, der zusätzlich Relevanz der Prämissen fordert",
        correctAnswer = 2,
        explanation = "Zentraler Unterschied: Ein Argument kann gültig sein, ohne stichaltig zu sein. Beispiel: 'Alle Einhörner sind blau; Sokrates ist ein Einhorn; also ist Sokrates blau' — gültig (Konklusion folgt notwendig), aber nicht stichaltig (Prämissen sind falsch). Ein stichaltig es Argument ist immer gültig und hat wahre Prämissen, also ist die Konklusion ebenfalls wahr. In der Praxis suchen wir stichhaltige Argumente; in der formalen Logik interessiert uns oft nur Gültigkeit (Struktur).",
        difficulty = 5,
        funFact = "Viele politische und alltägliche Argumente sind gültig aber nicht stichaltig: Die Schlussform ist korrekt, aber eine Prämisse ist falsch. Logische Kritik muss stets klarstellen, ob man die Gültigkeit (Schlussform) oder die Stichhaltig keit (Wahrheit der Prämissen) angreift."
    ),

    // --- WEITERE PHILOSOPHISCHE LOGIK-THEMEN ---

    // Question 21
    Question(
        categoryId = 12,
        questionText = "Was ist 'parakonsistente Logik' und wie behandelt sie Widersprüche anders als die klassische Logik?",
        answerA = "Parakonsistente Logik ist eine Logik, in der Ex Contradictione Quodlibet (aus einem Widerspruch folgt alles) nicht gilt. Widersprüche führen nicht zum logischen Kollaps — das System bleibt informativ trotz inkonsistenter Prämissen",
        answerB = "Parakonsistente Logik ist eine Logik mit mehr als zwei Wahrheitswerten",
        answerC = "Parakonsistente Logik ist äquivalent zur klassischen Logik, verwendet aber andere Axiome",
        answerD = "Parakonsistente Logik erlaubt es, dass eine Aussage gleichzeitig wahr und falsch ist",
        correctAnswer = 0,
        explanation = "Klassische Logik: Aus {P, ¬P} folgt alles (Ex Contradictione Quodlibet / Explosion). Dies macht klassische Systeme bei Inkonsistenz trivial. Parakonsistente Logik (Priest, da Costa): Die Explosionsregel gilt nicht. Aus P und ¬P folgt nicht beliebiges Q. Widersprüche sind 'lokalisierbar'. Motivation: Inkonsistente Datenbanken, Rechtssysteme (mit widersprüchlichen Gesetzen), naive Mengenlehre. Dialethismus (Priest) geht weiter: Manche Aussagen sind sowohl wahr als auch falsch.",
        difficulty = 5,
        funFact = "Parakonsistente Logik findet Anwendungen in der Informatik: Wissensrepräsentation mit inkonsistenten Fakten (z.B. aus mehreren Datenquellen), automatisiertes Schlussfolgern in Rechtssystemen und die Behandlung widersprüchlicher Spezifikationen in der Software-Verifikation."
    ),

    // Question 22
    Question(
        categoryId = 12,
        questionText = "Was ist der Unterschied zwischen 'klassischer' und 'intuitionistischer' Logik und was bedeutet das Fehlen des Satzes vom ausgeschlossenen Dritten?",
        answerA = "Intuitionistische Logik ist ein Teilsystem der klassischen Logik ohne die Negationsregeln",
        answerB = "Der Unterschied ist rein philosophisch und hat keine Konsequenzen für die Mathematik",
        answerC = "Intuitionistische Logik akzeptiert nur endliche Beweise",
        answerD = "Intuitionistische Logik (Brouwer, Heyting) lehnt das Gesetz des ausgeschlossenen Dritten (φ ∨ ¬φ) als allgemein gültig ab. Eine Aussage gilt als wahr nur, wenn ein konstruktiver Beweis vorliegt; 'wahr' bedeutet 'beweisbar', nicht 'nicht-falsch'",
        correctAnswer = 3,
        explanation = "Brouwer (1908): Mathematische Existenz erfordert Konstruktion. ¬¬P → P gilt nicht (doppelte Negationselimination scheitert). φ ∨ ¬φ gilt nicht: Man darf nicht sagen 'Entweder P₀ oder ¬P₀', bis man weiß welches. Beispiel: 'Es gibt irrationale Zahlen x, y mit x^y rational' — klassisch bewiesen durch √2^√2 (ohne zu wissen ob dieser Wert rational ist), aber nicht-konstruktiv. Konsequenzen: Kein Widerspruchsbeweis ohne Konstruktion, Mengenlehre ohne Auswahlaxiom, andere Topologie (offene Mengen statt Punkte).",
        difficulty = 5,
        funFact = "Der Curry-Howard-Isomorphismus verbindet intuitionistische Logik und Typentheorie: Ein Beweis von φ ∨ ¬φ würde ein Programm liefern, das für jede mathematische Aussage entscheidet, ob sie wahr oder falsch ist — also einen Algorithmus für das Halteproblem. Dass es dieses Programm nicht geben kann, zeigt, warum φ ∨ ¬φ in der Logik des Beweisens nicht allgemein gilt."
    ),

    // Question 23
    Question(
        categoryId = 12,
        questionText = "Was ist der 'Sorites-Paradoxon' (Haufen-Paradoxon) und welche Logiken versuchen es zu lösen?",
        answerA = "Das Sorites-Paradoxon zeigt, dass Zählbegriffe in der Logik undefiniert sind",
        answerB = "1 Sandkorn bildet keinen Haufen. Wenn n Körner keinen Haufen bilden, auch nicht n+1. Also bilden auch 10 Millionen Körner keinen Haufen. Das Paradoxon entsteht durch vage Prädikate. Fuzzy-Logik (Zadeh) löst es mit graduellen Wahrheitswerten; Supervaluationism mit überabzählbar vielen Präzisierungen",
        answerC = "Das Paradoxon wird durch das Induktionsprinzip der Mathematik gelöst",
        answerD = "Das Paradoxon beweist, dass keine Natursprache logisch formalisierbar ist",
        correctAnswer = 1,
        explanation = "Das Sorites-Paradoxon (griech. σωρός = Haufen, Eubulides, 4. Jh. v. Chr.): P1: 1 Korn ist kein Haufen. P2: Wenn n Körner kein Haufen, auch n+1 nicht. C: Beliebig viele Körner bilden keinen Haufen. P2 (Toleranzprinzip) ist für vage Prädikate intuitiv plausibel, aber führt zur Kontradiktion. Lösungen: (1) Fuzzy-Logik: Wahrheitswerte in [0,1], Induktionsschritt hat einen Wahrheitswert < 1. (2) Supervaluationism (Fine): Ausdruck ist wahr, wenn in allen Präzisierungen wahr. (3) Epistemizismus (Williamson): Es gibt eine scharfe Grenze, wir kennen sie nur nicht.",
        difficulty = 5,
        funFact = "Vage Prädikate sind in der KI-Forschung ein ernsthaftes Problem: Wenn ein autonomes Fahrzeug entscheiden muss 'Ist das Objekt ein Fußgänger?', braucht es einen Umgang mit Vagheit. Fuzzy-Logik wird in industriellen Steuerungssystemen weltweit eingesetzt — z.B. in Waschmaschinen, Klimaanlagen und Bremsassistenten."
    ),

    // Question 24
    Question(
        categoryId = 12,
        questionText = "Was ist 'deontische Logik' und welches fundamentale Paradoxon enthält sie (Ross-Paradoxon)?",
        answerA = "Deontische Logik ist eine modale Logik für zeitliche Aussagen",
        answerB = "Das Ross-Paradoxon zeigt, dass Deontik in S5-Modallogik formalisierbar ist",
        answerC = "Deontische Logik formalisiert normative Begriffe (Pflicht, Erlaubnis, Verbot). Das Ross-Paradoxon: Aus 'Du sollst den Brief einwerfen' folgt logisch 'Du sollst entweder den Brief einwerfen oder verbrennen' — die Einführungsregel für Disjunktionen führt zu kontraintuitiven normativen Schlüssen",
        answerD = "Deontische Logik kann nur für Rechtssysteme, nicht für Ethik verwendet werden",
        correctAnswer = 2,
        explanation = "Deontische Logik (von Wright, 1951): Operatoren O (obligatorisch), P (erlaubt), F (verboten). O und P verhalten sich wie □ und ◇ in S5. Das Ross-Paradoxon (1941): O(p) ⊢ O(p ∨ q) durch Disjunktionseinführung. Also: 'Schick den Brief' ⊢ 'Schick den Brief oder verbrenn ihn'. Das ist kontraintuitiv: Die normative Kraft kann sich nicht auf willkürliche Alternativen ausweiten. Weitere Paradoxien: Good Samaritan Paradoxon ('Jemand der stiehlt soll nicht stehlen' impliziert 'jemand stiehlt'), Chisholm-Paradoxon (Konflikte zwischen Prima-Facie-Pflichten).",
        difficulty = 5,
        funFact = "Deontische Logik ist Grundlage der Rechtscomputerwissenschaft: Formalisierung von Gesetzen, Verträgen und Regulierung. Die EU-Datenschutzgrundverordnung wurde teilweise in deontischer Logik formalisiert, um Konsistenz zu prüfen — mit überraschend vielen entdeckten Widersprüchen."
    ),

    // Question 25
    Question(
        categoryId = 12,
        questionText = "Was ist epistemische Logik und was bedeutet das Prinzip 'Wenn ich weiß, dass ich nicht weiß' (Negatives Wissen)?",
        answerA = "Epistemische Logik ist identisch mit Glaubenslogik und hat kein separates Wissenskonzept",
        answerB = "Negatives Wissen ist in der epistemischen Logik prinzipiell nicht formalisierbar",
        answerC = "Epistemische Logik (Hintikka, 1962) formalisiert Wissen und Glauben mit Operatoren K_i (Agent i weiß) und B_i (Agent i glaubt). Negatives Wissen: K_i(¬K_i φ) — ich weiß, dass ich φ nicht weiß. In S4-Epistemik gilt K_i φ → K_i K_i φ (positives Introspektion), aber ¬K_i φ → K_i ¬K_i φ ist stärker (negatives Introspektionsprinzip)",
        answerD = "K_i φ → K_i K_i φ ist das einzige Axiom der epistemischen Logik",
        correctAnswer = 2,
        explanation = "Hintikkas epistemische Logik: K_i φ bedeutet 'Agent i weiß φ'. Axiom T: K_i φ → φ (Wissen impliziert Wahrheit). Axiom 4: K_i φ → K_i K_i φ (positives Introspektionsprinzip — ich weiß, was ich weiß). Axiom 5: ¬K_i φ → K_i ¬K_i φ (negatives Introspektionsprinzip — ich weiß, was ich nicht weiß). Das System S5 mit T+4+5 ist die Standard-Epistemic Logik. Das negative Introspektionsprinzip ist philosophisch kontrovers: Wissen wir immer, was wir nicht wissen?",
        difficulty = 5,
        funFact = "Donald Rumsfelds berühmte Rede über 'Known unknowns' und 'Unknown unknowns' (2002) entspricht genau dem negativen Introspektionsprinzip: K¬Kφ (Ich weiß, dass ich φ nicht weiß) vs. ¬K¬Kφ (Ich weiß nicht einmal, dass ich φ nicht weiß). Die Philosophie hatte dies 40 Jahre früher formalisiert."
    ),

    // Question 26
    Question(
        categoryId = 12,
        questionText = "Was ist das 'Problem der impliziten Schlüsse' (Logical Omniscience) in der epistemischen Logik und wie ist es zu lösen?",
        answerA = "Logische Allwissenheit ist eine erwünschte Eigenschaft rationaler Agenten und kein Problem",
        answerB = "Das Problem entsteht nur in Logiken mit unendlich vielen Axiomen",
        answerC = "Logische Allwissenheit kann durch Einschränkung auf endliche Schlussfolgerungsregeln gelöst werden",
        answerD = "Epistemische Logik impliziert, dass ein Agent alle logischen Konsequenzen seines Wissens kennt: K(φ) ∧ (φ ⊨ ψ) → K(ψ). Dies ist für reale Agenten unrealistisch. Lösungen: Partielle Logik, explizites Glaubensnetz, Bounded Rationality",
        correctAnswer = 3,
        explanation = "In Standard-S5-Epistemik: Falls K_i φ und φ → ψ ist logisch gültig, dann K_i ψ. Das heißt: Ein Wissens-Agent kennt alle logischen Konsequenzen seines Wissens, alle Tautologien und alle Theoreme der Logik. Für reale kognitive Agenten ist das absurd — Menschen kennen nicht alle Folgen ihrer Überzeugungen. Lösungsansätze: (1) Situation Theory (Barwise/Perry): Wissen ist situationsspezifisch, keine Konsequenzenschließung. (2) Explicit Knowledge: Unterscheidung zwischen explizitem K_e und implizitem K_i Wissen. (3) Resource-Bounded Reasoning: Schließen kostet Aufwand.",
        difficulty = 5,
        funFact = "Das Problem der Logical Omniscience ist zentral für KI-Planung: Ein Planer, der alle Konsequenzen kennt, ist omniszient aber unrealisierbar. Praktische Planer (wie in autonomen Fahrzeugen) operieren mit unvollständigem Wissen und begrenzter Schlussfolgerungskapazität."
    ),

    // Question 27
    Question(
        categoryId = 12,
        questionText = "Was ist 'temporale Logik' (LTL/CTL) und wie wird sie in der Verifikation eingesetzt?",
        answerA = "Temporale Logik ist eine historische Bezeichnung für Modallogik vor 1960",
        answerB = "LTL und CTL sind äquivalent ausdrucksstark",
        answerC = "Temporale Logik erweitert die Modallogik um Zeitoperatoren: G (immer), F (irgendwann), X (im nächsten Zustand), U (bis). LTL (Linear Temporal Logic) betrachtet lineare Zeitpfade; CTL (Computation Tree Logic) verzweigte Zeit. Model Checking verifiziert automatisch, ob ein System eine temporale Spezifikation erfüllt",
        answerD = "Temporale Logik kann nur endliche Zeitstrukturen beschreiben",
        correctAnswer = 2,
        explanation = "LTL (Pnueli, 1977): Operatoren G ('globally', immer), F ('finally', irgendwann), X ('next', nächster Schritt), U ('until', bis). Beispiel: G(request → F response) = 'Auf jede Anfrage folgt irgendwann eine Antwort'. CTL (Clarke/Emerson, 1981): Quantifizierung über Pfade: AG (auf allen Pfaden immer), EF (es gibt einen Pfad mit irgendwann). Model Checking: Automatischer Algorithmus, der prüft, ob ein Finite-State-System M ⊨ φ. Anwendung: Verifikation von Chipdesigns, Protokollen, Software.",
        difficulty = 5,
        funFact = "Model Checking führte 2007 zum Turing Award für Clarke, Emerson und Sifakis. Die Methode wurde eingesetzt, um Fehler im Intel Pentium FPU-Chip zu finden und NASA-Raumsondensoftware zu verifizieren. Ohne temporale Logik wäre formale Hardware-Verifikation kaum möglich."
    ),

    // Question 28
    Question(
        categoryId = 12,
        questionText = "Was unterscheidet 'synthetische' von 'analytischen' Urteilen nach Kant und wie kritisiert Quine diese Unterscheidung?",
        answerA = "Analytische Urteile sind a priori, synthetische a posteriori — diese Unterscheidung ist absolut und unproblematisch",
        answerB = "Analytisch: Das Prädikat ist im Subjektbegriff enthalten ('Alle Junggesellen sind unverheiratet'). Synthetisch: Das Prädikat erweitert das Subjekt ('Alle Körper sind schwer'). Quine (Two Dogmas of Empiricism, 1951): Die Grenze ist nicht scharf ziehbar — Bedeutung und Empirie sind nicht trennbar (Holismus)",
        answerC = "Quine akzeptiert Kants Unterscheidung und verfeinert sie nur",
        answerD = "Die Unterscheidung gilt nur für Prädikatenlogik erster Stufe",
        correctAnswer = 1,
        explanation = "Kant (KrV, 1781): Analytisch = Prädikatsinhalt im Subjektbegriff; synthetisch = Prädikate erweitern Wissen. A priori = unabhängig von Erfahrung; a posteriori = durch Erfahrung. Kant behauptet synthetisch-a-priori-Urteile (z.B. Mathematik, reine Naturwissenschaft). Quine (1951): (1) Bedeutungsholismus: Aussagen haben keine isolierte empirische Bedeutung, sondern stehen im Netz aller Überzeugungen. (2) Empirischer Unterdetermination: Jede Theorie ist durch Beobachtungen unterdeterminiert. Beide Dogmen (Analytisch/Synthetisch und Reduktionismus) sind unhaltbar.",
        difficulty = 5,
        funFact = "Quines 'Two Dogmas of Empiricism' (1951) ist einer der einflussreichsten philosophischen Aufsätze des 20. Jahrhunderts. Er veränderte die Epistemologie grundlegend: Kein Satz ist immun gegen Revision angesichts von Erfahrungen — sogar logische Wahrheiten könnten aufgegeben werden, wenn es den 'Theorieball' als Ganzes besser macht."
    ),

    // Question 29
    Question(
        categoryId = 12,
        questionText = "Was ist der 'Münchhausen-Trilemma' (Agrippa-Trilemma) in der Erkenntnistheorie?",
        answerA = "Das Trilemma zeigt, dass alle drei Schlussformen (Deduktion, Induktion, Abduktion) scheitern",
        answerB = "Das Agrippa-Trilemma ist ein Paradoxon über Selbstreferenz in der Logik",
        answerC = "Jede Begründung einer Aussage führt zu einem der drei problematischen Ausgänge: (1) unendlicher Regress (jede Begründung braucht eine weitere), (2) Zirkelschluss (die Begründung setzt das zu Begründende voraus), (3) dogmatisches Abbrechen (unbegründete Axiome akzeptieren)",
        answerD = "Das Trilemma gilt nur für empirische, nicht für a priori Begründungen",
        correctAnswer = 2,
        explanation = "Agrippa (1. Jh. v. Chr.): Fünf Tropen des Skeptizismus, drei davon zum Begründungsproblem. Hans Albert (1968) systematisiert: Münchhausen-Trilemma für wissenschaftliche Begründungen. (1) Infiniter Regress: A begründet B begründet C ... → keine endliche Begründung. (2) Zirkel: A begründet B, B begründet A. (3) Dogmatischer Abbruch: A ist unbegründetes Axiom. Antworten: Kohärentismus (Neurath/Quine): Begründungen bilden kohärentes Netz, kein Fundament. Fallibilismus (Popper): Keine endgültige Begründung nötig, nur Falsifizierbarkeit.",
        difficulty = 5,
        funFact = "Karl Poppers Fallibilismus ist sein Antwort auf das Münchhausen-Trilemma: Wissenschaft begründet nicht, sie testet. Der Baron von Münchhausen zieht sich an seinen eigenen Haaren aus dem Sumpf — eine treffende Metapher für Selbstbegründungsversuche."
    ),

    // Question 30
    Question(
        categoryId = 12,
        questionText = "Was ist die 'Ramsey-Theorie' in der Kombinatorik und was ist ihr philosophisches Analogon?",
        answerA = "Ramsey-Theorie ist ein logisches System für Wahrscheinlichkeitsaussagen",
        answerB = "Ramsey-Theorie beweist die Endlichkeit aller vollständigen Graphen",
        answerC = "Das philosophische Analogon ist Kants Kategorienlehre",
        answerD = "Ramsey-Theorie besagt: In ausreichend großen Strukturen sind geordnete Teilstrukturen unvermeidbar. Ramsey (1930): In jeder hinreichend großen 2-Färbung eines vollständigen Graphen gibt es einen einfarbigen Teilgraphen. Philosophisches Analogon: 'Vollständige Unordnung ist unmöglich'",
        correctAnswer = 3,
        explanation = "Frank Ramsey (1928): R(s,t) = kleinste n, sodass jede rot-blaue Kantenfärbung von K_n einen roten K_s oder blauen K_t enthält. R(3,3) = 6. Für R(5,5): 43 ≤ R(5,5) ≤ 48 (bis heute offen). Erdős: 'Gott kennt R(5,5), aber wenn Außerirdische kämen und sagten, sie würden uns vernichten, wenn wir R(5,5) nicht nennen, sollten wir alle Mathematiker der Welt versammeln.' Philosophische Bedeutung: Selbst in chaotischen Systemen entstehen zwangsläufig geordnete Muster.",
        difficulty = 5,
        funFact = "Frank Ramsey starb 1930 mit nur 26 Jahren. In seinen kurzen Lebensjahren leistete er fundamentale Beiträge zu Logik, Mathematik, Ökonomie und Sprachphilosophie. Seine Theorie subjektiver Wahrscheinlichkeit begründete den modernen Bayesianismus."
    ),

    // Question 31
    Question(
        categoryId = 12,
        questionText = "Was ist das 'Frege-Geach Problem' (Einbettungsproblem) in der Metaethik?",
        answerA = "Das Problem zeigt, dass moralische Logik keine formalen Schlüsse erlaubt",
        answerB = "Frege-Geach beweist, dass Metaethik nur mit Modallogik formalisierbar ist",
        answerC = "Das Problem betrifft nur konditionale moralische Aussagen, nicht disjunktive",
        answerD = "Expressivisten (Emotivsisten) behaupten, moralische Sätze drücken Gefühle aus, keine Tatsachen. Das Frege-Geach Problem: In 'Wenn Mord falsch ist, dann ist die Anstiftung zu Mord falsch' hat 'Mord ist falsch' im Antezedens nicht-assertorische Kraft — aber dieselbe semantische Rolle wie im eigenständigen Urteil",
        correctAnswer = 3,
        explanation = "Frege (1892) bemerkte: Das Wort 'gut' in 'Ist das Essen gut?' und 'Das Essen ist gut' hat dieselbe Bedeutung, aber verschiedene Kraft (assertorisch vs. interrogativ). Geach (1965): Wenn 'X ist falsch' in 'Wenn X falsch, dann Y falsch' keine Assertion ist (wie Expressivisten behaupten), muss die Bedeutung dieselbe sein wie in der Assertion — sonst funktioniert Modus Ponens nicht. Expressivisten müssen erklären, wie moralische Ausdrücke in eingebetteten Kontexten funktionieren.",
        difficulty = 5,
        funFact = "Das Frege-Geach Problem ist eines der hartnäckigsten Probleme der analytischen Philosophie. Neuere Expressivisten (Gibbard, Blackburn) entwickelten 'Quasi-Realismus': Man kann logisch mit moralischen Sätzen schließen, ohne moralischen Realismus zu akzeptieren — eine elegante, aber kontroverse Lösung."
    ),

    // Question 32
    Question(
        categoryId = 12,
        questionText = "Was ist Wittgensteins 'Regelfolge-Paradoxon' und was sind seine Implikationen für die Logik?",
        answerA = "Das Paradoxon zeigt, dass formale Regeln in natürlicher Sprache nicht ausdrückbar sind",
        answerB = "Wittgenstein beweist, dass Regelfolgen immer eine bewusste Entscheidung ist",
        answerC = "In Philosophische Untersuchungen §201-202: Keine Interpretation einer Regel bestimmt ihre Anwendung, denn jede Interpretation kann durch eine neue Regel interpretiert werden — ein infiniter Regress. Eine Regel 'regiert' meine Handlung nicht durch Interpretation, sondern durch blindes Gehorchen in einer Praxis",
        answerD = "Das Paradoxon gilt nur für mathematische Regeln, nicht für sprachliche",
        correctAnswer = 2,
        explanation = "Wittgenstein (PU §201): Jede Handlung lässt sich mit irgendeiner Regel vereinbaren. 'Ich folge der Regel +2: 1000, 1002, 1004...' — Aber was wenn jemand bei 1000 springt zu 1004, dann 1008? Er kann sagen 'Ich folge +2 bis 1000, dann +4'. Kripkesteins Interpretation: Die Regel bestimmt nicht, welche Anwendung korrekt ist. Implikationen: Bedeutung ist keine private mentale Eigenschaft, sondern eine soziale Praxis. Regeln sind nicht in Symbolen selbst, sondern in Gemeinschaftspraktiken verankert.",
        difficulty = 5,
        funFact = "Kripke (Wittgenstein on Rules and Private Language, 1982) interpretiert das Paradoxon als 'skeptische Lösung': Es gibt keine objektive Tatsache darüber, was die richtige Regelanwendung ist — nur eine gemeinschaftliche Praxis der Normierung. Dies hatte tiefe Wirkung auf Linguistik, Rechtsphilosophie und Sprachphilosophie."
    ),

    // Question 33
    Question(
        categoryId = 12,
        questionText = "Was ist das 'Kompositionalitätsprinzip' (Frege-Prinzip) und welche Herausforderungen stellen idiomatische Ausdrücke und Kontexte für es dar?",
        answerA = "Das Kompositionalitätsprinzip gilt nur für aussagenlogische Formeln",
        answerB = "Kompositionalität: Die Bedeutung eines komplexen Ausdrucks ist eine Funktion der Bedeutungen seiner Teile und der Struktur ihrer Zusammensetzung. Idiome ('einen Korb geben') und opake Kontexte ('Hans glaubt, dass...') scheinen zu verletzen — die Bedeutung ergibt sich nicht aus den Teilbedeutungen",
        answerC = "Idiome beweisen, dass Natürliche Sprache nicht kompositional ist",
        answerD = "Kompositionalität ist identisch mit dem Substitutionsprinzip für co-referente Ausdrücke",
        correctAnswer = 1,
        explanation = "Freges Kompositionalitätsprinzip (manchmal 'Funktionsprinzip'): [[f(a)]] = [[f]]([[a]]). Herausforderungen: (1) Idiome: 'Er hat einen Korb bekommen' bedeutet nicht Kompositum aus [[Korb]] und [[bekommen]]. Lösung: Idiome haben lexikalisch gespeicherte Gesamtbedeutungen — kein echtes Komposionalitätsproblem. (2) Intensionale Kontexte: In 'Hans glaubt, dass Phosphorus ein Stern ist' kann man Phosphorus nicht durch Hesperus ersetzen. Lösung: Propositionale Einstellungen quantifizieren über Intensionen (mögliche-Welten-Funktionen), nicht Extensionen.",
        difficulty = 5,
        funFact = "Die Kompositionalität ist die Grundlage für alle formalen Semantiken natürlicher Sprachen (Montague-Grammatik, Typentheoretische Semantik). Ohne Kompositionalität könnten wir nicht verstehen, warum endlich viele Wörter unendlich viele Sätze erzeugen können — Chomskys 'creative aspect of language use'."
    ),

    // Question 34
    Question(
        categoryId = 12,
        questionText = "Was ist das 'Gettier-Problem' und warum erschüttert es die klassische Wissensdefinition?",
        answerA = "Das Gettier-Problem zeigt, dass Wissen unmöglich ist",
        answerB = "Gettier beweist, dass Rechtfertigung für Wissen nicht notwendig ist",
        answerC = "Gettier (1963): Die klassische Definition 'Wissen = wahre gerechtfertigte Überzeugung' ist unzureichend. Gegenbeispiel: Smith glaubt (wahrhaftig und gerechtfertigt) 'Der Mann, der den Job bekommt, hat 10 Münzen in der Tasche'. Jones hat 10 Münzen. Aber Smith bekommt den Job und zufällig hat auch Smith 10 Münzen — er weiß es nicht. Smiths Glaube ist wahr und gerechtfertigt, aber kein Wissen",
        answerD = "Das Problem betrifft nur empirisches Wissen, nicht mathematisches",
        correctAnswer = 2,
        explanation = "Gettiers kurzer Aufsatz (1963, nur 3 Seiten) leitete eine der fruchtbarsten Debatten der Erkenntnistheorie ein. Die klassische Tripartite Definition (Platon, Theaitetos): Wissen = wahre + gerechtfertigte + geglaubte Aussage. Gettier zeigt: Man kann etwas glauben, es ist wahr, und man hat gute Gründe (Rechtfertigung) — und trotzdem weiß man es nicht, weil die Verbindung zwischen Rechtfertigung und Wahrheit zufällig ist. Seitdem wurden viele 'vierte Bedingungen' vorgeschlagen: Causalismus, Reliabilismus, Kontextualismus, Sicherheitsanforderungen.",
        difficulty = 5,
        funFact = "Edmund Gettier ist für diesen Aufsatz weltberühmt und hat danach fast nichts mehr veröffentlicht — ein Kuriosum in der Philosophiegeschichte. Der Aufsatz 'Is Justified True Belief Knowledge?' (1963) hat mehr als 10.000 Zitierungen und ist einer der meistzitierten philosophischen Texte aller Zeiten."
    ),

    // Question 35
    Question(
        categoryId = 12,
        questionText = "Was ist 'epistemischer Kontextualismus' und wie antwortet er auf das Skeptizismusproblem?",
        answerA = "Kontextualismus behauptet, dass Wissen immer relativ zu einer kulturellen Gruppe ist",
        answerB = "Kontextualismus ist eine Form des Relativismus, der Objektivität aufgibt",
        answerC = "Der Kontextualismus wurde von Wittgenstein in den Philosophischen Untersuchungen entwickelt",
        answerD = "Kontextualismus (DeRose, Cohen): 'Wissen' ist ein indexikalischer Begriff — was als Wissen zählt, hängt vom Kontext der Zuschreibung ab. Im Alltag zählt niedrige Evidenz, in philosophischer Diskussion hohe. Skeptiker heben die Standards an; im Alltag schlägt das Skeptizismus-Argument fehl",
        correctAnswer = 3,
        explanation = "DeRose (1995): 'Wissen' variiert wie 'groß' je nach Kontext. Im Restaurant: 'Weiß ich, dass der Koch kein Gift zugefügt hat?' — Nein, aber ich esse trotzdem. In der Arztpraxis: Ich weiß, dass ich das Medikament nehme. Die Skeptiker-Herausforderung: 'Du weißt nicht, dass du kein Gehirn-im-Tank bist; also weißt du nichts über die Außenwelt.' Kontextualismus: In philosophischen Kontexten werden die Standards für Wissen so hochgesetzt, dass dieser Einwand greift; im Alltag gelten niedrigere Standards — dort sind Skeptiker-Argumente irrelevant.",
        difficulty = 5,
        funFact = "Kontextualismus erklärt eine merkwürdige Intuition: Wir sagen im Alltag 'Ich weiß, dass es regnen wird', aber in der Philosophiestunde 'Ich kann nicht wirklich wissen, ob es regnen wird'. Beide Aussagen sind je nach Kontext wahr — 'Wissen' ist polysem, wie viele andere Adjektive."
    ),

    // Question 36
    Question(
        categoryId = 12,
        questionText = "Was ist das 'Paradoxon des Vorankündigers' (Surprise Exam Paradox) und was zeigt es über praktisches Schließen?",
        answerA = "Das Paradoxon zeigt, dass Ankündigungen logisch selbstwidersprüchlich sind",
        answerB = "Ein Lehrer kündigt an: 'In dieser Woche gibt es eine Überraschungsklausur — ihr werdet es nicht im Voraus wissen.' Durch Rückwärtsinduktion: Am Freitag wäre es keine Überraschung. Also nicht Freitag. Dann auch nicht Donnerstag (dann wäre es keine Überraschung) usw. Fazit: Die Klausur kann an keinem Tag sein — aber der Lehrer hält sie Mittwoch",
        answerC = "Das Paradoxon beweist, dass das Induktionsprinzip für temporale Schlüsse ungültig ist",
        answerD = "Das Paradoxon hat eine einfache Lösung: Der Lehrer lügt notwendigerweise",
        correctAnswer = 1,
        explanation = "Das Paradoxon (verschiedene Formulierungen, 1940er): Schüler schlussfolgern durch Rückwärtsinduktion, dass die Klausur nicht stattfinden kann. Dann, wenn sie die Annahme aufgeben, findet sie statt — und überrascht sie doch. Das Paradoxon zeigt Probleme mit: (1) Iteriertem Wissen über zukünftige Wissenslosigkeit, (2) Selbstwiderlegendem Wissen (die Ankündigung selbst macht ihre Bedingung schwerer erfüllbar), (3) Epistemische Logik und temporale Logik. Smullyan: Die Ankündigung ist performativ paradox, nicht logisch unmöglich.",
        difficulty = 5,
        funFact = "Das Surprise Exam Paradox ist verwandt mit dem Lügner-Paradoxon: Die Ankündigung 'Ihr werdet überrascht sein' ist selbstreferentiell — die Existenz der Ankündigung selbst beeinflusst, ob die Bedingung erfüllt ist. Epistemische Zeitlogik kann das Paradoxon präzise formalisieren."
    ),

    // Question 37
    Question(
        categoryId = 12,
        questionText = "Was ist das 'Newcombs Problem' und wie illustriert es den Konflikt zwischen Kausalentscheidungstheorie und Evidenzentscheidungstheorie?",
        answerA = "Das Problem hat eine eindeutige Lösung durch die Spieltheorie",
        answerB = "Newcombs Problem zeigt, dass Entscheidungstheorie nur für endliche Mengen gilt",
        answerC = "Ein Wahrsager hat Box A (1000€) und Box B (1.000.000€ oder leer). Er sagte korrekt vorher, ob du B nimmst. Wenn er vorhersagte, du nimmst beide → B leer. Wenn er vorhersagte, du nimmst nur B → B voll. Evidenztheorie: Nimm nur B (maximiert Erwartungsnutzen). Kausaltheorie: Nimm beide (deine Wahl kausal beeinflusst das Ergebnis nicht mehr)",
        answerD = "Das Problem ist unlösbar, weil perfekte Vorhersage logisch unmöglich ist",
        correctAnswer = 2,
        explanation = "Newcombs Problem (Newcomb, 1960; publiziert Nozick, 1969): Kausalentscheidungstheorie (CDT): Man soll beide Boxen nehmen. Die Vorhersage ist bereits gemacht — sie ändert sich nicht durch deine Wahl. 'Nehme ich B oder beide, ändert das den Inhalt nicht.' Dominanzargument für CDT: In jedem Szenario (B voll/leer) bringt 'beide nehmen' 1000€ mehr. Evidenzentscheidungstheorie (EDT): Nimm nur B, denn nehmen von nur B ist stark mit B=voll korreliert. EDT maximiert conditional expected utility. Das Problem ist ungeklärt; beide Theorien haben starke Intuitionen für sich.",
        difficulty = 5,
        funFact = "Newcombs Problem ist direkt auf das Gefangenendilemma und die Frage nach Rationalität bei Vorhersagbarkeit verwandt. Es ist ein Testfall für alle Entscheidungstheorien. Robert Nozick schrieb 1969 'es gibt keine Standard-Theorie, die dieses Problem klar löst' — das gilt bis heute."
    ),

    // Question 38
    Question(
        categoryId = 12,
        questionText = "Was ist das 'Preface-Paradoxon' (Vorwort-Paradoxon) in der Erkenntnistheorie?",
        answerA = "Das Vorwort-Paradoxon beweist, dass Autoren immer lügen",
        answerB = "Das Paradoxon zeigt, dass Wahrscheinlichkeit und Glaube äquivalent sind",
        answerC = "Das Vorwort-Paradoxon ist identisch mit dem Sorites-Paradoxon für Überzeugungen",
        answerD = "Ein Autor glaubt jede einzelne Aussage in seinem Buch (p₁, p₂, ..., pₙ). Im Vorwort schreibt er 'Sicher enthält dieses Buch Fehler'. Er glaubt also zugleich ∀i: pᵢ und ¬(p₁ ∧ p₂ ∧ ... ∧ pₙ) — ein rationaler Widerspruch der logischen Überzeugungen",
        correctAnswer = 3,
        explanation = "Makinson (1965): Sei p₁,...,pₙ die Aussagen des Buchs. Der Autor glaubt jede pᵢ. Rational? Ja, denn jede wurde sorgfältig geprüft. Er glaubt auch: 'Das Buch enthält Fehler' = ¬(p₁ ∧ ... ∧ pₙ). Rational? Ja, denn kein umfangreiches Buch ist fehlerfrei. Aber {p₁, ..., pₙ, ¬(p₁ ∧ ... ∧ pₙ)} ist inkonsistent! Das Paradoxon motiviert: Überzeugungen müssen nicht deduktiv geschlossen sein (Überzeugungsmenge ≠ logisch abgeschlossene Menge). Wahrscheinlichkeits-Epistemologie: Glaube hat Grade; pᵢ kann mit Pr(pᵢ) < 1 geglaubt werden.",
        difficulty = 5,
        funFact = "Das Vorwort-Paradoxon motiviert probabilistische Epistemologie (Jeffrey, 1965): Statt binärer Überzeugungen (glaubt/glaubt nicht) verwendet man Wahrscheinlichkeiten. Dann ist es rational, Pr(pᵢ) = 0.99 für jedes pᵢ zu haben und gleichzeitig Pr(∀i: pᵢ) sehr klein — kein Widerspruch in Wahrscheinlichkeitstermen."
    ),

    // Question 39
    Question(
        categoryId = 12,
        questionText = "Was ist das 'Problem der Relevanzlogik' und wie versucht Relevanzlogik die klassische Implikation zu reformieren?",
        answerA = "Relevanzlogik eliminiert das Konditional aus der Logik vollständig",
        answerB = "Klassische Implikation: Eine falsche Aussage impliziert alles (ex falso quodlibet), und eine Tautologie wird von allem impliziert. Relevanzlogik (Anderson, Belnap) fordert: Prämissen und Konklusion müssen topisch zusammenhängen. Das Implicationsrelation ist präziser als materiale Konditionale",
        answerC = "Relevanzlogik ist eine Unterklasse der modalen Logik S4",
        answerD = "Das Problem entsteht nur in mehrwertigen Logiken",
        correctAnswer = 1,
        explanation = "Klassische Tautologien, die intuitiv falsch erscheinen: (1) P → (Q → P) — jede wahre Aussage wird von allem impliziert. (2) ¬P → (P → Q) — jede falsche Aussage impliziert alles. (3) (P ∧ ¬P) → Q. Relevanzlogik (Anderson & Belnap, 1975): Führt einen 'Relevanzoperator' ein, der verlangt, dass Variablen in Prämissen und Konklusion 'topisch verbunden' sind. Systeme: R, E, T. Die Deduktionsrelation ist schwächer als in klassischer Logik, aber intuitiver.",
        difficulty = 5,
        funFact = "Relevanzlogik findet Anwendungen in der Informatik: In einem Typsystem mit 'lineare Typen' (Linear Logic, Girard 1987) wird jede Ressource genau einmal verwendet — eine logische Formalisierung von Ressourcensensitivität. Linear Logic ist die logische Basis für Rust's Ownership-System."
    ),

    // Question 40
    Question(
        categoryId = 12,
        questionText = "Was ist Freilands Argument 'Kripke-Wittgenstein' zur Unmöglichkeit privater Sprache und was ist seine logische Konsequenz?",
        answerA = "Kripkes Argument beweist, dass Privatsprachen logisch konsistent sind",
        answerB = "Das Argument zeigt, dass Regelfolgen unmöglich sind",
        answerC = "Das Argument gilt nur für ostensive Definitionen von Schmerzwörtern",
        answerD = "Wittgenstein (PU §293): 'Angenommen, jeder hätte einen Kasten mit etwas darin: wir nennen es einen Käfer.' Wenn niemand außer mir meinen Kasten kennt, fällt der Begriff 'Käfer' aus der Sprache heraus. Private Qualia können keinen stabilen Sprachgebrauch begründen — Bedeutung ist öffentlich",
        correctAnswer = 3,
        explanation = "Wittgensteins Käfer-im-Kasten-Argument (PU §293): Man stelle sich vor, jeder hat einen Kasten mit 'Käfer' drin. Niemand kann in den Kasten des anderen schauen. Wenn das Wort 'Käfer' nach Inhalt des Kastens benannt wird, aber keiner den Inhalt kennt, fällt 'Käfer' aus der Bedeutung heraus — der Kasten kann leer sein, 'Käfer' bedeutet dann nur die Kiste selbst. Analog: Wenn 'Schmerz' durch rein private Empfindung definiert ist, können keine öffentlichen Korrektheitsstandards existieren — keine stabile Bedeutung.",
        difficulty = 5,
        funFact = "Das Privatsprachen-Argument hat direkte Bedeutung für die KI-Philosophie: Wenn Bedeutung öffentlich ist, dann brauchen KI-Systeme, die Sprache verstehen sollen, nicht-private, intersubjektive Grounding. Dies motiviert embodied cognition und soziale Sprachtheorien in der KI-Forschung."
    ),

    // Question 41
    Question(
        categoryId = 12,
        questionText = "Was ist der 'Löwenheim-Skolem Satz aufwärts' und welche Konsequenz hat er für die Axiomatisierbarkeit reeller Zahlen?",
        answerA = "Der aufwärts L-S-Satz gilt nur für endliche Strukturen",
        answerB = "Jede unendliche Struktur in einer abzählbaren FO-Sprache hat Elementarerweiterungen beliebiger Kardinalzahl. Konsequenz: Die Theorie der reellen Zahlen hat überabzählbare Modelle, aber auch abzählbare — 'Die reellen Zahlen' können nicht durch FO-Axiome eingeschränkt werden",
        answerC = "Die reellen Zahlen haben genau ein Modell in jeder FO-Theorie",
        answerD = "Der Satz gilt nur für Theorien ohne Gleichheit",
        correctAnswer = 1,
        explanation = "Aufwärts Löwenheim-Skolem (Tarski): Wenn eine Theorie T ein unendliches Modell hat und κ > |T| eine Kardinalzahl ist, hat T ein Modell der Mächtigkeit κ. Konsequenz für ℝ: Die Theorie der reellen Zahlen (Th(ℝ)) hat abzählbare Modelle (abwärts L-S) und überabzählbare Modelle jeder Kardinalzahl (aufwärts L-S). Kein FO-System kann die reellen Zahlen 'bis auf Isomorphie' charakterisieren — dafür bräuchte man Logik zweiter Stufe (dann keine Vollständigkeit mehr).",
        difficulty = 5,
        funFact = "Tarski bewies 1948 die Vollständigkeit und Entscheidbarkeit der elementaren euklidischen Geometrie in erster Stufe. Die reelle Analysis ist entscheidbar als FO-Theorie geordneter Körper — aber ohne Mengenlehre, also ohne Analysis im vollen Sinne."
    ),

    // Question 42
    Question(
        categoryId = 12,
        questionText = "Was versteht man unter dem 'Prinzip des hinreichenden Grundes' (Leibniz) und wie kritisiert Schopenhauer es?",
        answerA = "Das Prinzip besagt, dass alle Aussagen beweisbar sind",
        answerB = "Schopenhauer akzeptiert das Prinzip, kritisiert nur seine Anwendung auf Gott",
        answerC = "Das Prinzip ist nur für analytische Urteile gültig",
        answerD = "Leibniz: Nichts geschieht ohne hinreichenden Grund, warum es so und nicht anders ist. Schopenhauer (Über die vierfache Wurzel, 1813): Unterscheidet vier Formen: Kausalität (Natur), logischer Grund (Denken), Motivation (Handeln), Seinsgrund (Mathematik). Leibniz vermischt diese vier Formen",
        correctAnswer = 3,
        explanation = "Leibniz (Monadologie, 1714): Principium Rationis Sufficientis — 'Keine Tatsache kann wahr oder existent sein, keine Aussage wahr, ohne dass es einen hinreichenden Grund gibt, warum es so und nicht anders ist.' Logisch: Für jede Aussage p gibt es einen Grund. Schopenhauer (Dissertation, 1813): Es gibt VIER Formen des Grundes: (1) Causa efficiens (Kausalität in der Natur). (2) Erkenntnisgrund (in der Logik). (3) Motiv (in menschlichen Handlungen). (4) Seinsgrund (mathematischer Zusammenhang). Leibniz vermische diese vier irrtümlich zu einem einheitlichen Prinzip.",
        difficulty = 5,
        funFact = "Das Prinzip des hinreichenden Grundes ist logisch nicht beweisbar — es ist selbst ein grundloser Grund. Bertrand Russell zeigte, dass das kosmologische Argument für Gottes Existenz den Fehler begeht, den Satz 'Alles hat eine Ursache' auf das Universum als Ganzes anzuwenden."
    ),

    // Question 43
    Question(
        categoryId = 12,
        questionText = "Was ist das 'Problem der Transworld Identity' (Identität in möglichen Welten) und wie unterscheiden sich Lewis und Kripke in ihrer Antwort?",
        answerA = "Beide Philosophen stimmen überein, dass Identität zwischen Welten unmöglich ist",
        answerB = "Lewis akzeptiert transworld identity, Kripke lehnt sie ab",
        answerC = "Kripke: Identität in möglichen Welten ist trivial — 'Aristoteles hätte kein Philosoph sein können' bedeutet, wir stipulieren dieselbe Person in einer anderen Welt. Lewis: Verschiedene Welten sind kausal isoliert; 'Aristoteles' in einer anderen Welt ist ein Gegenstück (counterpart), nicht identisch",
        answerD = "Das Problem ist nur für physische Objekte, nicht für abstrakte Entitäten relevant",
        correctAnswer = 2,
        explanation = "Lewis' Gegenstandstheorie (Counterpart Theory, 1968): Objekte existieren nur in einer Welt. Wenn wir sagen 'Aristoteles hätte Schuster sein können', meinen wir: In einer ähnlichen möglichen Welt gibt es ein Gegenstück (counterpart) von Aristoteles, das Schuster ist. Kripkes Kritik: Das Gegenstück-Modell macht Modal-Aussagen abhängig von der Ähnlichkeitsrelation — willkürlich. Stattdessen: Wir stipulieren direkt, dass derselbe Aristoteles in einer möglichen Welt Schuster ist. Kripke: Identität ist notwendig — wenn a = b, dann notwendigerweise a = b.",
        difficulty = 5,
        funFact = "Die Debatte über Transworld Identity ist relevant für die Frage nach personaler Identität: Wenn mein Gegenstück in einer anderen Welt eine ganz andere Person ist, bin ich wirklich dieselbe Person zu verschiedenen Zeiten? Parfit (Reasons and Persons, 1984) argumentiert, dass personale Identität keine fundamentale Tatsache ist."
    ),

    // Question 44
    Question(
        categoryId = 12,
        questionText = "Was ist das 'Problem der ethischen Gegenstandslosigkeit' (Mackie's Error Theory)?",
        answerA = "Mackie behauptet, moralische Aussagen sind sinnlos und nicht wahrheitsfähig",
        answerB = "Error Theory ist identisch mit emotivistischem Expressivismus",
        answerC = "Mackie (Ethics, 1977): Moralische Aussagen beanspruchen, objektive Tatsachen zu beschreiben ('Mord ist falsch' = 'Mord hat die Eigenschaft der Falschheit'). Aber es gibt keine solchen objektiven moralischen Tatsachen im Universum — alle moralischen Aussagen sind daher falsch. Dies ist eine Form des Moral-Realismus mit negativem Ausgang",
        answerD = "Mackie argumentiert, dass nur religiöse Moral objektiv ist",
        correctAnswer = 2,
        explanation = "Mackies 'Irrtums-Theorie' (Error Theory): (1) Kognitivismus: Moralische Sätze sind echte Behauptungen mit Wahrheitswert. (2) Anti-Realismus: Es gibt keine objektiven moralischen Tatsachen. (3) Konklusion: Alle positiven moralischen Aussagen sind falsch. Argument aus Seltsamkeit (Argument from Queerness): Objektive moralische Tatsachen wären metaphysisch 'seltsam' — nicht-natürliche Entitäten mit intrinsischer Normativität. Argument aus Variabilität: Die kulturelle Variation moralischer Überzeugungen spricht gegen objektive Wahrheit.",
        difficulty = 5,
        funFact = "Mackies Error Theory ist logisch konsistent: Man kann moralisch handeln und reden, ohne objektive Wahrheit zu postulieren. Evolutionäre Debunking-Argumente (Sharon Street) verfeinern Mackie: Wenn unsere moralischen Intuitionen evolutionär entstanden sind, haben wir keinen Grund, ihnen Objektivität zuzuschreiben."
    ),

    // Question 45
    Question(
        categoryId = 12,
        questionText = "Was ist das 'Problem des normativen Regresses' in der Erkenntnistheorie (Inferentialismus)?",
        answerA = "Der normative Regress tritt nur bei deduktiven Schlüssen auf",
        answerB = "Brandom (Making It Explicit, 1994): Wenn die Gültigkeit eines Schlusses von Normen abhängt, die die Norm selbst begründen, droht ein unendlicher Regress. Lösung: Schlussnormen sind nicht durch weitere Normen, sondern durch soziale Praxis normiert — eine 'sich-verpflichten' (commitment) und 'sich-berechtigen' (entitlement) Praxis",
        answerC = "Das Problem wird durch das Prinzip des hinreichenden Grundes gelöst",
        answerD = "Inferentialismus ist eine Form des Empirismus ohne normative Komponente",
        correctAnswer = 1,
        explanation = "Brandom (Inferentialism): Bedeutung wird durch inferentielle Rolle definiert — der Inhalt einer Aussage ist bestimmt durch ihre Inferenz-Beziehungen zu anderen Aussagen. Das normative Regress-Problem: Was rechtfertigt die Schlussregel R? Eine weitere Regel R'. Was rechtfertigt R'? Und so weiter. Brandom (nach Wittgenstein): Regeln werden nicht durch weitere Regeln, sondern durch normative Praxis der Gemeinschaft begründet — es gibt einen impliziten sozialen 'score-keeping' zwischen Gesprächspartnern.",
        difficulty = 5,
        funFact = "Inferentialismus ist Grundlage für neue Ansätze in der KI-Semantik: Large Language Models lernen Bedeutung durch Ko-Auftreten von Wörtern — eine Art statistischer Inferentialism. Brandoms normative Dimension fehlt allerdings: LLMs 'halten sich verpflichtet' zu nichts."
    ),

    // Question 46
    Question(
        categoryId = 12,
        questionText = "Was ist das 'Sokal-Hoax' und welche Frage über die Grenze von Wissenschaft und Pseudowissenschaft stellt er auf?",
        answerA = "Der Sokal-Hoax beweist, dass Quantenmechanik philosophisch nutzlos ist",
        answerB = "Der Hoax zeigt, dass peer review grundsätzlich unmöglich ist",
        answerC = "Sokal (1996) reichte einen absichtlich sinnlosen Aufsatz über 'Quantengravitation als befreiende postmoderne Praxis' bei einer kulturwissenschaftlichen Zeitschrift ein — er wurde angenommen. Dies zeigt das Abgrenzungsproblem: Wie unterscheidet man echte von pseudo-wissenschaftlicher Argumentation?",
        answerD = "Sokal intendierte eine Satire auf analytic philosophy, nicht auf continental philosophy",
        correctAnswer = 2,
        explanation = "Sokal veröffentlichte 'Transgressing the Boundaries: Towards a Transformative Hermeneutics of Quantum Gravity' (Social Text, 1996) — voller physikalischer Unsinn, verpackt in postmoderne Rhetorik. Das Demarcation Problem (Popper): Was trennt Wissenschaft von Pseudo-Wissenschaft? Popper: Falsifizierbarkeit. Kuhn: Paradigmenzugehörigkeit. Lakatos: Progressive vs. degenerative Forschungsprogramme. Der Sokal-Hoax zeigt, dass soziale Überzeugungsmittel und formale Korrektheit auseinanderfallen können.",
        difficulty = 5,
        funFact = "Die 'Sokal-Affair' löste einen Kulturkrieg zwischen Natur- und Geisteswissenschaften aus ('Science Wars'). 2017 wurden drei Philosophen berühmt, die ähnliche Aufsätze in Zeitschriften für Gender Studies, Fat Studies und Queer Theory platzierten — das 'Grievance Studies Affair' (Peter Boghossian, James Lindsay, Helen Pluckrose)."
    ),

    // Question 47
    Question(
        categoryId = 12,
        questionText = "Was besagt das 'Church-Fitch Paradox' (Paradoxon der Erkennbarkeit)?",
        answerA = "Das Paradoxon zeigt, dass Wissen und Wahrheit unverbunden sind",
        answerB = "Church-Fitch beweist, dass formale Logik kein Epistemik-Operator K ausdrücken kann",
        answerC = "Das Paradoxon gilt nur für mathematische, nicht für empirische Wahrheiten",
        answerD = "Aus dem harmlosen Prinzip 'Wenn φ wahr ist, könnte es prinzipiell gewusst werden' folgt mit epistemischer Logik der absurde Schluss, dass alle wahren Aussagen tatsächlich irgendwann gewusst werden — Allwissenheit für die Menschheit",
        correctAnswer = 3,
        explanation = "Fitch (1963): Sei KP = 'Knowability Principle': ∀φ(φ → ◇Kφ). Sei φ eine unbekannte Wahrheit: φ ∧ ¬Kφ. Per KP: ◇K(φ ∧ ¬Kφ). Aber K(φ ∧ ¬Kφ) impliziert K(φ) ∧ K(¬Kφ) impliziert Kφ ∧ ¬Kφ — Widerspruch. Also ¬◇K(φ ∧ ¬Kφ), d.h. es ist nicht möglich, eine unbekannte Wahrheit zu kennen. Also ∀φ(φ → Kφ) — alle Wahrheiten sind bekannt! Das KP-Prinzip impliziert Allwissenheit.",
        difficulty = 5,
        funFact = "Das Church-Fitch-Paradoxon ist besonders für Anti-Realisten problematisch: Dummett und andere behaupteten, Wahrheit sei erkennbar (verification principle). Fitch zeigt: Dieses Prinzip ist entweder trivial falsch oder führt zur Allwissenheit. Anti-Realisten müssen ihr Erkennbarkeitsprinzip daher schwächen."
    ),

    // Question 48
    Question(
        categoryId = 12,
        questionText = "Was ist der 'Moore-Satz' ('Moore's Paradox') und wie analysiert Wittgenstein ihn?",
        answerA = "Moores Paradoxon zeigt, dass Erstperson-Aussagen keine Wahrheitswerte haben",
        answerB = "G.E. Moore bemerkte: 'Es regnet, aber ich glaube es nicht' (p ∧ ¬B(p)) ist keine logische Kontradiktion, aber absurd zu behaupten. Wittgenstein: Der Satz ist in Ordnung geschrieben, aber nicht assertierbar — da Behaupten Implikatur der eigenen Überzeugung trägt",
        answerC = "Das Paradoxon beweist, dass Selbstzuschreibungen immer wahr sind",
        answerD = "Wittgenstein lehnte das Paradoxon als Scheinproblem ab",
        correctAnswer = 1,
        explanation = "G.E. Moore (1940er): 'Es regnet, aber ich glaube, es regnet nicht.' Formal: p ∧ ¬B_i(p). Dies ist nicht logisch widersprüchlich — es könnte regen, während man es nicht glaubt. Aber es ist nicht behauptbar (unassertable). Warum? Grice's Kooperationsmaximen: Behaupten trägt die Implikatur, dass man es glaubt. 'Ich behaupte p' impliziert 'Ich glaube p'. Also ist 'p ∧ ich glaube nicht p' pragmatisch inkohärent. Wittgenstein: Dies zeigt, dass Behaupten nicht nur Wahrheitswert, sondern performative Dimension hat.",
        difficulty = 5,
        funFact = "Moores Paradoxon ist Ausgangspunkt für die moderne Sprechakttheorie (Austin, Searle): Behaupten, Fragen, Befehlen sind verschiedene Sprechakte mit verschiedenen Erfüllungsbedingungen. 'Es regnet, aber ich glaube es nicht' verletzt die Aufrichtigkeitsbedingung des Behauptens."
    ),

    // Question 49
    Question(
        categoryId = 12,
        questionText = "Was ist Fregés 'Sinn' und 'Bedeutung' (Sinn und Referenz) und welches Rätsel motiviert die Unterscheidung?",
        answerA = "Sinn und Bedeutung sind Freges Übersetzung von Platos Idee und Erscheinung",
        answerB = "Die Unterscheidung gilt nur für Eigennamen, nicht für Sätze",
        answerC = "Frege unterscheidet Sinn (Wahrheitswert) und Bedeutung (propositionalen Inhalt)",
        answerD = "Frege (Über Sinn und Bedeutung, 1892): 'Morgenstern = Abendstern' ist informativ, 'Morgenstern = Morgenstern' trivial — obwohl dieselbe Venus gemeint ist. Also haben Ausdrücke zwei semantische Ebenen: Bedeutung (Referenz/Denotat) und Sinn (Art des Gegebenseins). Nur der Sinn unterscheidet die beiden",
        correctAnswer = 3,
        explanation = "Frege (1892): Sei a = 'Morgenstern', b = 'Abendstern'. Beide referieren auf Venus. 'a = a' ist trivial (analytisch wahr, uninformativ). 'a = b' ist informativ (astronomische Entdeckung). Wenn 'a' und 'b' dieselbe Bedeutung (Venus) hätten, müssten beide Identitäten denselben kognitiven Wert haben. Also müssen sie verschiedenen Sinn haben: Verschiedene 'Modi des Gegebenseins' derselben Referenz. Für Sätze: Bedeutung = Wahrheitswert (wahr/falsch); Sinn = Gedanke/Proposition. In intensionalen Kontexten wird Sinn, nicht Bedeutung substituiert.",
        difficulty = 5,
        funFact = "Freges Sinn-Bedeutungs-Unterscheidung ist eine der fundamentalsten in der Sprachphilosophie. Russell lehnte sie ab und ersetzte sie durch seine Theorie der Kennzeichnungen. Kripke widerlegte beide durch starre Bezeichnungen. Heute ist die Debatte noch aktiv: Namen haben für die meisten Semantiker reine Referenz ohne deskriptiven Sinn."
    ),

    // Question 50
    Question(
        categoryId = 12,
        questionText = "Was ist das 'Paradoxon des Schüffelhaufens' der Identität: Das Schiff des Theseus, und welche logischen Positionen gibt es dazu?",
        answerA = "Das Schiff des Theseus ist nur ein ästhetisches Problem ohne logische Relevanz",
        answerB = "Die Lösung liegt im Eigentumsrecht — wer Eigentumsrechte hat, bestimmt Identität",
        answerC = "Ein Schiff wird Teil für Teil ersetzt, bis kein Original-Teil mehr vorhanden ist. Jede sukzessive Änderung scheint Identität zu erhalten (Sorites-Prinzip), aber am Ende ist das Schiff komplett anders. Positionen: (1) Perduranten (Lewis): Objekte sind vierdimensionale Wurmkörper durch Zeit; (2) Enduranten: Identität ist intrinsisch und durch sukzessive Änderungen erhalten; (3) Eliminativismus: Es gibt keine Schiffe, nur Atomhaufen",
        answerD = "Das Paradoxon beweist, dass zeitliche Identität nicht formalisierbar ist",
        correctAnswer = 2,
        explanation = "Das Schiff des Theseus (Plutarch): Wenn jede Planke sukzessive ersetzt wird, bis keine Original-Planke übrig ist, ist es dasselbe Schiff? Variation: Die alten Planken werden aufgesammelt und zu einem neuen Schiff zusammengesetzt — welches ist das 'echte' Schiff? Positionen: (1) Four-Dimensionalism (Lewis, Sider): Objekte erstrecken sich durch Zeit; 'dasselbe Schiff' bezeichnet verschiedene zeitliche Teile desselben 4D-Wurms. (2) Endurantism: Objekte existieren vollständig zu jedem Zeitpunkt — Identität durch Kontinuität von Eigenschaften. (3) Fission: Wenn beide Schiffe existieren, hat Identität sich aufgespalten.",
        difficulty = 5,
        funFact = "Das Schiff des Theseus hat praktische juristische Relevanz: Bei einer Restaurierung eines Oldtimers, bei der 80% ersetzt werden, gilt er steuerlich noch als Original? Gerichte in verschiedenen Ländern haben unterschiedliche Antworten — ein Zeichen dafür, dass das philosophische Problem nicht trivial ist."
    )
)

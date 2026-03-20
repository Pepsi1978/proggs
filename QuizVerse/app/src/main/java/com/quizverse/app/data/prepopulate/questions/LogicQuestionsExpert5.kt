package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun logicQuestionsExpert5(): List<Question> = listOf(

    // Mengenlehre, Unendlichkeit, Cantor, Hilbert — 50 Expert questions (difficulty = 4)
    // Internet-verified: Wikipedia DE, aleph1.info, mathepedia.de, biancahoegel.de, spektrum.de

    // ── FRAGE 1 ──────────────────────────────────────────────────────────────
    Question(
        categoryId = 12,
        questionText = "Was beweist Cantors zweites Diagonalargument (1877)?",
        answerA = "Die Menge der rationalen Zahlen ist abzählbar unendlich.",
        answerB = "Die Menge der reellen Zahlen ist überabzählbar — es gibt verschiedene 'Größen' von Unendlichkeit.",
        answerC = "Jede unendliche Menge ist gleichmächtig zur Menge der natürlichen Zahlen.",
        answerD = "Die Menge der ganzen Zahlen ist größer als die der natürlichen Zahlen.",
        correctAnswer = 1,
        explanation = "Cantors zweites Diagonalargument ist ein Widerspruchsbeweis: Nimmt man an, alle reellen Zahlen im Intervall (0,1) ließen sich auflisten, konstruiert man per Diagonale eine Zahl, die in keiner Zeile vorkommt — Widerspruch. Damit ist ℝ überabzählbar und 'größer' als ℕ.",
        difficulty = 4,
        funFact = "Cantor veröffentlichte diesen Beweis 1891 in seiner zweiten Form. Der Beweis gilt als einer der elegantesten in der gesamten Mathematikgeschichte."
    ),

    // ── FRAGE 2 ──────────────────────────────────────────────────────────────
    Question(
        categoryId = 12,
        questionText = "Welche der folgenden Mengen ist NICHT abzählbar unendlich?",
        answerA = "Die Menge der ganzen Zahlen ℤ",
        answerB = "Die Menge der rationalen Zahlen ℚ",
        answerC = "Die Menge der algebraischen Zahlen",
        answerD = "Die Menge der reellen Zahlen ℝ",
        correctAnswer = 3,
        explanation = "ℕ, ℤ, ℚ und sogar die algebraischen Zahlen sind alle abzählbar (es gibt eine Bijektion zu ℕ). Die reellen Zahlen ℝ hingegen sind überabzählbar — das beweist Cantors Diagonalargument.",
        difficulty = 4,
        funFact = "Dass ℚ abzählbar ist, wirkt paradox: Zwischen je zwei rationalen Zahlen liegen unendlich viele — trotzdem lassen sie sich in einer Folge auflisten (erstes Cantorsches Diagonalverfahren, 1874)."
    ),

    // ── FRAGE 3 ──────────────────────────────────────────────────────────────
    Question(
        categoryId = 12,
        questionText = "Was ist ℵ₀ (Aleph-Null)?",
        answerA = "Eine sehr große natürliche Zahl ohne konkreten Wert.",
        answerB = "Das Symbol für die Unendlichkeit in der Analysis (Grenzwerte).",
        answerC = "Die Kardinalzahl der Menge der natürlichen Zahlen — die kleinste unendliche Kardinalzahl.",
        answerD = "Die Kardinalzahl der Menge der reellen Zahlen.",
        correctAnswer = 2,
        explanation = "Georg Cantor führte ℵ₀ (Aleph-Null) als Bezeichnung für die Mächtigkeit von ℕ ein. Es ist die kleinste transfinite Kardinalzahl. Alle abzählbar unendlichen Mengen haben diese Mächtigkeit. Die reellen Zahlen hingegen haben Mächtigkeit 2^ℵ₀ = ℭ (Kontinuum).",
        difficulty = 4,
        funFact = "Cantor wählte den hebräischen Buchstaben Aleph (ℵ) für seine transfiniten Zahlen — angeblich, weil er einen Buchstaben wollte, der in keiner anderen mathematischen Notation vorkam."
    ),

    // ── FRAGE 4 ──────────────────────────────────────────────────────────────
    Question(
        categoryId = 12,
        questionText = "Hilberts Hotel ist voll besetzt mit unendlich vielen Gästen. Ein neuer Gast erscheint. Wie wird er untergebracht, ohne jemanden hinauszuwerfen?",
        answerA = "Das ist unmöglich — das Hotel ist ja bereits voll.",
        answerB = "Jeder Gast rückt vom Zimmer n in Zimmer n+1; Zimmer 1 wird frei.",
        answerC = "Der neue Gast bekommt Zimmer 0, das bislang nicht genutzt wurde.",
        answerD = "Die Hälfte der Gäste zieht in gerade Zimmer, die andere in ungerade.",
        correctAnswer = 1,
        explanation = "Bei einem unendlichen Hotel kann jeder Gast aus Zimmer n in Zimmer n+1 umziehen. Diese Verschiebung ist für alle n ∈ ℕ definiert, Zimmer 1 wird frei. Das zeigt: Für unendliche Mengen gilt 'voll' nicht im endlichen Sinne — ℕ und ℕ∪{ein neues Element} sind gleichmächtig.",
        difficulty = 4,
        funFact = "David Hilbert trug dieses Gedankenexperiment 1924/25 in einer Vorlesung vor. Es veranschaulicht, warum Unendlichkeit kein Maximum im gewöhnlichen Sinn hat."
    ),

    // ── FRAGE 5 ──────────────────────────────────────────────────────────────
    Question(
        categoryId = 12,
        questionText = "Was besagt die Kontinuumshypothese (Cantor, 1878)?",
        answerA = "Es gibt unendlich viele Primzahlen.",
        answerB = "Die Menge der reellen Zahlen ist abzählbar.",
        answerC = "Es gibt keine Menge, deren Mächtigkeit echt zwischen ℵ₀ und der Mächtigkeit von ℝ liegt.",
        answerD = "Jede Teilmenge der reellen Zahlen ist entweder endlich oder gleichmächtig zu ℝ.",
        correctAnswer = 2,
        explanation = "Die Kontinuumshypothese (KH) besagt: Es gibt keine Kardinalzahl κ mit ℵ₀ < κ < 2^ℵ₀. Mit anderen Worten: Die nächste unendliche Kardinalzahl nach ℵ₀ ist bereits die Mächtigkeit von ℝ (= ℵ₁ unter der KH).",
        difficulty = 4,
        funFact = "Die KH stand als erstes Problem auf Hilberts berühmter Liste der 23 ungelösten Probleme von 1900."
    ),

    // ── FRAGE 6 ──────────────────────────────────────────────────────────────
    Question(
        categoryId = 12,
        questionText = "Kurt Gödel (1938) und Paul Cohen (1963) bewiesen zusammen etwas Verblüffendes über die Kontinuumshypothese. Was?",
        answerA = "Die KH ist wahr — es gibt keine Zwischenmenge.",
        answerB = "Die KH ist falsch — es gibt tatsächlich eine Zwischenmenge.",
        answerC = "Die KH ist von den Standardaxiomen ZFC unabhängig: Sie kann weder bewiesen noch widerlegt werden.",
        answerD = "Die KH ist äquivalent zum Auswahlaxiom.",
        correctAnswer = 2,
        explanation = "Gödel zeigte 1938: KH ist konsistent mit ZFC (man kann sie nicht widerlegen). Cohen zeigte 1963 mit der Forcing-Methode: ¬KH ist auch konsistent mit ZFC (man kann sie auch nicht beweisen). Also ist KH unabhängig von ZFC — weder wahr noch falsch innerhalb dieser Axiomatik.",
        difficulty = 4,
        funFact = "Cohen erhielt 1966 für diesen Beweis die Fields-Medaille — die höchste Auszeichnung in der Mathematik. Er war der einzige Logiker, der sie je bekam."
    ),

    // ── FRAGE 7 ──────────────────────────────────────────────────────────────
    Question(
        categoryId = 12,
        questionText = "Welche Aussage über die Russellsche Antinomie ist korrekt?",
        answerA = "Russell bewies, dass jede Menge sich selbst enthalten muss.",
        answerB = "Die Antinomie zeigt einen Widerspruch in der naiven Mengenlehre: Die 'Menge aller Mengen, die sich nicht selbst enthalten' führt zu einem Paradoxon.",
        answerC = "Russell bewies, dass es nur eine einzige unendliche Menge gibt.",
        answerD = "Die Antinomie betrifft nur Mengen mit überabzählbar vielen Elementen.",
        correctAnswer = 1,
        explanation = "Sei R = {x | x ∉ x}. Dann gilt: R ∈ R ↔ R ∉ R — ein direkter Widerspruch. Das zeigt, dass die naive Annahme 'für jede Eigenschaft P gibt es die Menge {x | P(x)}' inkonsistent ist. Lösung: Zermelo-Fraenkel-Axiome mit dem Aussonderungsaxiom.",
        difficulty = 4,
        funFact = "Russell schrieb 1902 einen Brief an Frege, der gerade sein zweibändiges Grundlagenwerk fertiggestellt hatte — und teilte ihm mit, dass die gesamte Konstruktion kollabiert."
    ),

    // ── FRAGE 8 ──────────────────────────────────────────────────────────────
    Question(
        categoryId = 12,
        questionText = "Wie lautet Cantors Satz über die Potenzmenge?",
        answerA = "Die Potenzmenge P(M) einer Menge M hat immer genau doppelt so viele Elemente wie M.",
        answerB = "Für jede Menge M gilt: |M| < |P(M)|. Die Potenzmenge ist immer mächtiger als die Ausgangsmenge.",
        answerC = "Die Potenzmenge einer abzählbaren Menge ist ebenfalls abzählbar.",
        answerD = "Für endliche Mengen gilt |P(M)| = |M|².",
        correctAnswer = 1,
        explanation = "Cantors Satz (1891): Für jede Menge M gilt |M| < |P(M)|. Der Beweis ist ein Diagonalargument: Jede Funktion f: M → P(M) ist nicht surjektiv, denn {x ∈ M | x ∉ f(x)} ist nicht im Bild von f. Für M = ℕ folgt: |P(ℕ)| = 2^ℵ₀ = |ℝ|.",
        difficulty = 4,
        funFact = "Cantors Satz liefert eine aufsteigende Kette von Unendlichkeiten: ℵ₀ < 2^ℵ₀ < 2^(2^ℵ₀) < ... — es gibt unendlich viele 'Stufen' der Unendlichkeit."
    ),

    // ── FRAGE 9 ──────────────────────────────────────────────────────────────
    Question(
        categoryId = 12,
        questionText = "Zwei Mengen heißen 'gleichmächtig'. Was bedeutet das?",
        answerA = "Sie haben die gleiche Anzahl an Elementen (nur für endliche Mengen definiert).",
        answerB = "Es existiert eine bijektive Funktion zwischen ihnen — eine umkehrbar eindeutige Zuordnung jedes Elements.",
        answerC = "Eine Menge ist Teilmenge der anderen.",
        answerD = "Beide Mengen sind Teilmengen der reellen Zahlen.",
        correctAnswer = 1,
        explanation = "Zwei Mengen A und B sind gleichmächtig (|A| = |B|), wenn es eine Bijektion f: A → B gibt. Das funktioniert auch für unendliche Mengen: ℕ und ℤ sind gleichmächtig, obwohl ℕ ⊂ ℤ — eine kontraintuitive Konsequenz der Cantorschen Mengenlehre.",
        difficulty = 4,
        funFact = "Galileo Galilei beobachtete 1638 bereits dieses 'Paradoxon': Die geraden Zahlen und alle natürlichen Zahlen können eins-zu-eins zugeordnet werden. Er schloss, dass man Unendlichkeiten nicht vergleichen kann — Cantor zeigte 200 Jahre später das Gegenteil."
    ),

    // ── FRAGE 10 ──────────────────────────────────────────────────────────────
    Question(
        categoryId = 12,
        questionText = "Was ist die kleinste transfinite Ordinalzahl ω?",
        answerA = "ω ist eine sehr große natürliche Zahl, die alle anderen übertrifft.",
        answerB = "ω ist die Ordinalzahl, die auf alle natürlichen Zahlen folgt — die kleinste Ordinalzahl größer als jede natürliche Zahl.",
        answerC = "ω ist identisch mit ℵ₀ und bezeichnet dieselbe Menge.",
        answerD = "ω ist das Unendlichkeitszeichen ∞ aus der Analysis.",
        correctAnswer = 1,
        explanation = "ω ist die erste transfinite Ordinalzahl: 0, 1, 2, 3, ..., ω, ω+1, ω+2, ..., ω·2, ... Ordinalzahlen beschreiben Ordnungstypen. ω und ℵ₀ haben dieselbe Grundmenge (ℕ), aber ω als Ordinalzahl berücksichtigt die Reihenfolge, ℵ₀ als Kardinalzahl nur die Mächtigkeit.",
        difficulty = 4,
        funFact = "In der transfiniten Arithmetik gilt ω + 1 ≠ 1 + ω: Addition von Ordinalzahlen ist nicht kommutativ! 1 + ω = ω, aber ω + 1 > ω."
    ),

    // ── FRAGE 11 ──────────────────────────────────────────────────────────────
    Question(
        categoryId = 12,
        questionText = "Hilberts Hotel ist voll. Unendlich viele Busse fahren vor, jeder mit unendlich vielen Gästen. Können alle untergebracht werden?",
        answerA = "Nein — unendlich viele Busse mit unendlich vielen Gästen sind zu viele.",
        answerB = "Ja — indem man die Gäste mit dem Cantorschen Diagonalverfahren für ℚ aufzählt und in entsprechende Zimmer verteilt.",
        answerC = "Ja — aber nur, wenn die Gäste nummeriert sind.",
        answerD = "Nein — eine abzählbar unendliche Menge kann keine überabzählbar unendliche aufnehmen.",
        correctAnswer = 1,
        explanation = "Abzählbar viele Busse mit je abzählbar vielen Gästen bilden eine abzählbare Vereinigung abzählbarer Mengen — die wieder abzählbar ist. Via Cantors Diagonalaufzählung (wie für ℚ) weist man jedem Gast eine eindeutige natürliche Zahl zu und damit ein Zimmer.",
        difficulty = 4,
        funFact = "Dieses Ergebnis zeigt: ℕ×ℕ ist gleichmächtig zu ℕ. Cantor bewies dies 1874 mit seinem ersten Diagonalverfahren — dem Zickzack-Pfad durch eine unendliche Matrix."
    ),

    // ── FRAGE 12 ──────────────────────────────────────────────────────────────
    Question(
        categoryId = 12,
        questionText = "Was ist ein Wohlordnungssatz und wer bewies ihn?",
        answerA = "Jede endliche Menge kann geordnet werden — bewiesen von Euklid.",
        answerB = "Jede Menge kann so geordnet werden, dass jede nichtleere Teilmenge ein kleinstes Element hat — äquivalent zum Auswahlaxiom, gezeigt von Zermelo 1904.",
        answerC = "Jede geordnete Menge hat ein größtes Element — bewiesen von Cantor.",
        answerD = "Unendliche Mengen können nicht wohlgeordnet werden — bewiesenals Konsequenz der Russellschen Antinomie.",
        correctAnswer = 1,
        explanation = "Ernst Zermelo bewies 1904, dass jede Menge wohlgeordnet werden kann. Dieser Satz ist äquivalent zum Auswahlaxiom (AC). Das Auswahlaxiom ist seinerseits von ZF unabhängig (Gödel 1938, Cohen 1963), daher auch der Wohlordnungssatz.",
        difficulty = 4,
        funFact = "Die Wohlordnung der reellen Zahlen existiert (unter AC), kann aber nicht explizit konstruiert werden — man kann keine konkrete Wohlordnung von ℝ hinschreiben."
    ),

    // ── FRAGE 13 ──────────────────────────────────────────────────────────────
    Question(
        categoryId = 12,
        questionText = "Was versteht man unter einer 'bijektiven' Funktion?",
        answerA = "Eine Funktion, die nur positive Werte annimmt.",
        answerB = "Eine Funktion f: A → B, die sowohl injektiv (eins-zu-eins) als auch surjektiv (auf) ist — jedes Element in B hat genau ein Urbild.",
        answerC = "Eine Funktion, bei der zwei verschiedene Argumente denselben Wert haben dürfen.",
        answerD = "Eine Funktion, die nur für endliche Mengen definiert ist.",
        correctAnswer = 1,
        explanation = "Bijektionen sind das Werkzeug für 'Gleichmächtigkeit'. f: A → B bijektiv bedeutet: (1) injektiv: f(x) = f(y) ⟹ x = y, und (2) surjektiv: für jedes b ∈ B gibt es a ∈ A mit f(a) = b. Damit entspricht jedes Element in A genau einem in B und umgekehrt.",
        difficulty = 4,
        funFact = null
    ),

    // ── FRAGE 14 ──────────────────────────────────────────────────────────────
    Question(
        categoryId = 12,
        questionText = "Welche der folgenden Aussagen über die transfinite Addition von Ordinalzahlen ist korrekt?",
        answerA = "ω + 1 = 1 + ω, da Addition kommutativ ist.",
        answerB = "1 + ω = ω, aber ω + 1 ≠ ω — Addition von Ordinalzahlen ist nicht kommutativ.",
        answerC = "ω + ω = ω, da Unendlichkeit plus Unendlichkeit gleich Unendlichkeit ist.",
        answerD = "Transfinite Addition ist nicht definiert.",
        correctAnswer = 1,
        explanation = "In der Ordinalzahlarithmetik gilt: 1 + ω = ω (man 'klebt' eine Einheit vor eine ω-Folge — das Ordnungstyp ändert sich nicht). Aber ω + 1 > ω (man hängt ein Element ans Ende — das ergibt einen neuen Ordnungstyp). Daher ist Addition nicht kommutativ.",
        difficulty = 4,
        funFact = "Bei der Multiplikation: 2·ω = ω, aber ω·2 = ω+ω > ω. Die Reihenfolge der Faktoren entscheidet, ob man 'zwei Kopien von ω' nebeneinander oder ω Kopien von 2 meint."
    ),

    // ── FRAGE 15 ──────────────────────────────────────────────────────────────
    Question(
        categoryId = 12,
        questionText = "Was ist das Auswahlaxiom (Axiom of Choice, AC)?",
        answerA = "Für jede nichtleere Menge gibt es immer ein kleinstes Element.",
        answerB = "Für jede Familie nichtleerer Mengen gibt es eine Auswahlfunktion, die aus jeder Menge genau ein Element auswählt.",
        answerC = "Jede Menge ist eine Teilmenge der reellen Zahlen.",
        answerD = "Für jede Eigenschaft P gibt es die Menge aller Objekte mit dieser Eigenschaft.",
        correctAnswer = 1,
        explanation = "Das Auswahlaxiom (AC) postuliert: Für eine beliebige Familie {Aᵢ}ᵢ∈I nichtleerer Mengen existiert eine Funktion f mit f(i) ∈ Aᵢ für alle i. Es ist für endliche Familien trivial wahr, für unendliche Familien aber nicht konstruktiv beweisbar — daher ein eigenständiges Axiom.",
        difficulty = 4,
        funFact = "Das Auswahlaxiom hat scheinbar seltsame Konsequenzen: das Banach-Tarski-Paradoxon, das behauptet, man könne eine Kugel in endlich viele Teile zerlegen und daraus zwei gleichgroße Kugeln zusammensetzen."
    ),

    // ── FRAGE 16 ──────────────────────────────────────────────────────────────
    Question(
        categoryId = 12,
        questionText = "Wie lautet das Schröder-Bernstein-Theorem (auch Cantor-Schröder-Bernstein)?",
        answerA = "Wenn |A| ≤ |B| und |B| ≤ |A|, dann folgt |A| = |B| — es gibt eine Bijektion zwischen A und B.",
        answerB = "Wenn A Teilmenge von B ist, dann ist A kleiner als B.",
        answerC = "Zwei unendliche Mengen sind immer gleichmächtig.",
        answerD = "Für endliche Mengen gilt |A| = |B| genau dann, wenn A = B.",
        correctAnswer = 0,
        explanation = "Das Schröder-Bernstein-Theorem: Gibt es Injektionen f: A → B und g: B → A, dann sind A und B gleichmächtig. Dieses Theorem ist fundamentale Grundlage für Gleichmächtigkeitsbeweise und gilt ohne das Auswahlaxiom (es folgt aus ZF allein).",
        difficulty = 4,
        funFact = "Der Beweis wurde 1887 von Cantor skizziert, von Schröder 1896 und Bernstein 1897 bewiesen — und von Dedekind sogar unabhängig 1887 gefunden, jedoch nicht veröffentlicht."
    ),

    // ── FRAGE 17 ──────────────────────────────────────────────────────────────
    Question(
        categoryId = 12,
        questionText = "Was ist ein 'Limes-Ordinal' in der Mengenlehre?",
        answerA = "Eine Ordinalzahl, die größer ist als alle anderen Ordinalzahlen.",
        answerB = "Eine Ordinalzahl α > 0, die kein direktes Vorgänger-Ordinal hat — also der Limes einer aufsteigenden Folge von Ordinalzahlen ist.",
        answerC = "Die größte Ordinalzahl innerhalb einer gegebenen Kardinalzahl.",
        answerD = "Eine Ordinalzahl, die sich selbst als Element enthält.",
        correctAnswer = 1,
        explanation = "Ordinalzahlen sind entweder 0, ein Nachfolger-Ordinal (α+1) oder ein Limes-Ordinal. ω ist das erste Limes-Ordinal: Es hat keinen direkten Vorgänger — es ist der Limes der Folge 0,1,2,3,... Weitere Limes-Ordinale: ω·2, ω², ωᵒᵒ, ε₀ usw.",
        difficulty = 4,
        funFact = null
    ),

    // ── FRAGE 18 ──────────────────────────────────────────────────────────────
    Question(
        categoryId = 12,
        questionText = "Was besagt das Burali-Forti-Paradoxon?",
        answerA = "Die Menge aller natürlichen Zahlen ist widersprüchlich.",
        answerB = "Die 'Menge aller Ordinalzahlen' kann keine Menge sein, da sie selbst eine Ordinalzahl wäre, die dann nicht in ihr enthalten wäre.",
        answerC = "Jede wohlgeordnete Menge hat ein größtes Element.",
        answerD = "Ordinalzahlen und Kardinalzahlen stimmen nie überein.",
        correctAnswer = 1,
        explanation = "Angenommen, Ω wäre die Menge aller Ordinalzahlen. Dann wäre Ω selbst wohlgeordnet durch ∈, also eine Ordinalzahl, und müsste Ω ∈ Ω gelten — Widerspruch. Lösung: Ω ist eine echte Klasse (Ordinals), keine Menge. Dieses Paradoxon entdeckte Cesare Burali-Forti 1897.",
        difficulty = 4,
        funFact = "Ähnlich wie Russells Antinomie zeigt dies, dass nicht jede intuitiv formulierbare Kollektion eine 'Menge' ist — die moderne Mengenlehre unterscheidet streng zwischen Mengen und echten Klassen."
    ),

    // ── FRAGE 19 ──────────────────────────────────────────────────────────────
    Question(
        categoryId = 12,
        questionText = "Welche Mächtigkeit hat die Potenzmenge von ℕ, also P(ℕ)?",
        answerA = "ℵ₁ (Aleph-Eins) per Definition.",
        answerB = "ℵ₀² = ℵ₀, da Unendlichkeit durch sich selbst multipliziert gleich bleibt.",
        answerC = "2^ℵ₀, was gleich der Mächtigkeit von ℝ ist — also überabzählbar.",
        answerD = "Unendlich, aber kleiner als |ℝ|.",
        correctAnswer = 2,
        explanation = "Jede Teilmenge von ℕ entspricht einer 0/1-Folge (Charakteristikfunktion). Die Menge aller solchen Folgen hat Mächtigkeit 2^ℵ₀. Es lässt sich zeigen: 2^ℵ₀ = |ℝ| (Mächtigkeit des Kontinuums). Ob 2^ℵ₀ = ℵ₁ gilt (Kontinuumshypothese), ist von ZFC unabhängig.",
        difficulty = 4,
        funFact = "Die Bijektion P(ℕ) → ℝ kann explizit konstruiert werden: Man ordnet jeder Teilmenge A ⊆ ℕ die reelle Zahl Σ_{n∈A} 2⁻ⁿ zu — eine Art Binärdarstellung."
    ),

    // ── FRAGE 20 ──────────────────────────────────────────────────────────────
    Question(
        categoryId = 12,
        questionText = "Was ist der Unterschied zwischen einer abzählbar unendlichen und einer überabzählbar unendlichen Menge?",
        answerA = "Eine abzählbare Menge hat weniger Elemente als eine überabzählbare, auch wenn beide unendlich sind.",
        answerB = "Abzählbare Mengen sind nur ℕ und ℤ — alle anderen unendlichen Mengen sind überabzählbar.",
        answerC = "Es gibt überhaupt keinen Unterschied — alle unendlichen Mengen sind gleichmächtig.",
        answerD = "Abzählbar unendlich bedeutet, dass die Menge genau ℵ₀ Elemente hat und in einer Folge aufgelistet werden kann.",
        correctAnswer = 3,
        explanation = "Eine Menge ist abzählbar unendlich, wenn es eine Bijektion zu ℕ gibt — also |M| = ℵ₀. Sie lässt sich als Folge a₁, a₂, a₃, ... schreiben. Eine überabzählbare Menge (|M| > ℵ₀) kann nicht durch eine solche Folge vollständig erfasst werden, wie Cantors Diagonalbeweis zeigt.",
        difficulty = 4,
        funFact = null
    ),

    // ── FRAGE 21 ──────────────────────────────────────────────────────────────
    Question(
        categoryId = 12,
        questionText = "Welche Zermelo-Fraenkel-Axiome ersetzen die fehlerhafte naive Mengenbildung und verhindern Russells Paradoxon?",
        answerA = "Das Unendlichkeitsaxiom und das Paarungsaxiom.",
        answerB = "Das Extensionalitätsaxiom und das Fundierungsaxiom.",
        answerC = "Das Aussonderungsaxiom (Separation): Mengen dürfen nur aus bereits existierenden Mengen ausgesondert werden.",
        answerD = "Das Ersetzungsaxiom und das Auswahlaxiom.",
        correctAnswer = 2,
        explanation = "Das Aussonderungsaxiom (ZF-Axiomschema) erlaubt: Aus einer bestehenden Menge A darf man alle Elemente mit Eigenschaft P aussondern: {x ∈ A | P(x)}. Ohne die Einschränkung 'x ∈ A' (naive Version: {x | P(x)}) entsteht Russells Paradoxon. Die Einschränkung verhindert es.",
        difficulty = 4,
        funFact = "Zermelo entwickelte sein Axiomensystem 1908 nicht zuletzt, um Russells Paradoxon zu umgehen. Fraenkel ergänzte es 1921/22 um das Ersetzungsaxiom — zusammen bilden sie ZF (ohne Auswahlaxiom)."
    ),

    // ── FRAGE 22 ──────────────────────────────────────────────────────────────
    Question(
        categoryId = 12,
        questionText = "Was bedeutet es, dass eine Menge 'sich selbst enthält' (im Sinne von x ∈ x)?",
        answerA = "Jede Menge enthält sich selbst als Element — das ist in der Mengenlehre normal.",
        answerB = "Das Fundierungsaxiom (Regularitätsaxiom) in ZF verbietet x ∈ x: Jede Menge gründet auf elementfremden Elementen, keine unendlichen Abstiegsketten.",
        answerC = "Nur die leere Menge enthält sich selbst.",
        answerD = "Selbstreferenz ist nur bei Klassen, nicht bei Mengen möglich.",
        correctAnswer = 1,
        explanation = "Das Fundierungsaxiom (ZF) besagt: Jede nichtleere Menge A enthält ein Element m mit A ∩ m = ∅. Damit ist x ∈ x verboten (würde eine unendliche ∈-Kette erzeugen). Dieses Axiom schließt viele Selbstreferenz-Paradoxa aus, ist aber für die meisten mathematischen Anwendungen irrelevant.",
        difficulty = 4,
        funFact = null
    ),

    // ── FRAGE 23 ──────────────────────────────────────────────────────────────
    Question(
        categoryId = 12,
        questionText = "Was ist die 'Forcing'-Methode, die Paul Cohen 1963 erfand?",
        answerA = "Eine Methode, um Axiome zu beweisen, indem man alle Modelle der ZFC untersucht.",
        answerB = "Eine Technik zur Konstruktion neuer Modelle der Mengenlehre, in denen bestimmte Aussagen gelten oder nicht gelten — damit bewies Cohen die Unabhängigkeit der Kontinuumshypothese.",
        answerC = "Eine numerische Methode zur Approximation transfiniter Kardinalzahlen.",
        answerD = "Cohens Beweis, dass die Mengenlehre widerspruchsfrei ist.",
        correctAnswer = 1,
        explanation = "Forcing ist eine modelltheoretische Technik: Ausgehend von einem Modell M von ZFC konstruiert man ein erweitertes Modell M[G], indem man 'generische' Mengen hinzufügt. Cohen zeigte damit: Es gibt ein Modell von ZFC, in dem die Kontinuumshypothese gilt, und eines, in dem sie nicht gilt.",
        difficulty = 4,
        funFact = "Cohen gilt als einer der brillantesten Mathematiker des 20. Jahrhunderts. Er war ursprünglich Analysis-Spezialist und hatte keine Vorkenntnisse in Mengenlehre, als er das Forcing entwickelte — er löste das Problem in nur einem Jahr."
    ),

    // ── FRAGE 24 ──────────────────────────────────────────────────────────────
    Question(
        categoryId = 12,
        questionText = "Welche Kardinalzahl bezeichnet man mit ℵ₁?",
        answerA = "Die Mächtigkeit der reellen Zahlen — unabhängig von der Kontinuumshypothese.",
        answerB = "Die nächste unendliche Kardinalzahl nach ℵ₀ — die kleinste überabzählbare Kardinalzahl.",
        answerC = "2·ℵ₀ — das Doppelte von ℵ₀.",
        answerD = "Die Mächtigkeit der Menge aller Ordinalzahlen.",
        correctAnswer = 1,
        explanation = "ℵ₁ ist die kleinste überabzählbare Kardinalzahl — die erste Aleph-Zahl nach ℵ₀. Sie ist die Mächtigkeit der Menge aller abzählbaren Ordinalzahlen. Ob ℵ₁ = 2^ℵ₀ (= |ℝ|) gilt, ist die Kontinuumshypothese — und von ZFC unentscheidbar.",
        difficulty = 4,
        funFact = "Die Aleph-Folge ℵ₀, ℵ₁, ℵ₂, ... ist durch das Auswahlaxiom wohldefiniert. Man kann zeigen, dass ℵᵢ = ωᵢ (als Ordinalzahl betrachtet) für alle Ordinale i gilt."
    ),

    // ── FRAGE 25 ──────────────────────────────────────────────────────────────
    Question(
        categoryId = 12,
        questionText = "Was ist das 'Banach-Tarski-Paradoxon' und welches Axiom braucht es?",
        answerA = "Eine Kugel kann durch unendlich viele Schnitte in zwei gleich große Kugeln geteilt werden — ohne jedes Axiom.",
        answerB = "Eine Einheitskugel kann in endlich viele Teile zerlegt und zu zwei Einheitskugeln zusammengesetzt werden — dies setzt das Auswahlaxiom voraus.",
        answerC = "Zwei Kugeln unterschiedlicher Größe sind immer gleichmächtig — bewiesen ohne das Auswahlaxiom.",
        answerD = "Jede Menge reeller Zahlen besitzt ein Lebesgue-Maß — bewiesen von Banach und Tarski.",
        correctAnswer = 1,
        explanation = "Banach und Tarski bewiesen 1924: Eine 3D-Kugel lässt sich in 5 (nicht-messbare!) Teile zerlegen, die durch Drehungen und Verschiebungen zu zwei gleichgroßen Kugeln zusammengesetzt werden können. Dieser Satz verwendet das Auswahlaxiom wesentlich — ohne AC ist er nicht beweisbar.",
        difficulty = 4,
        funFact = "Das Paradoxon ist physikalisch nicht realisierbar, da die Teile nicht messbar (kein Volumen haben) und daher nicht physikalisch konstruierbar sind. Es zeigt, wie das Auswahlaxiom mathematische Objekte erzeugen kann, die kein physikalisches Pendant haben."
    ),

    // ── FRAGE 26 ──────────────────────────────────────────────────────────────
    Question(
        categoryId = 12,
        questionText = "Was versteht man unter 'Kardinalzahlarithmetik' für unendliche Mengen?",
        answerA = "Dieselben Regeln wie für natürliche Zahlen: ℵ₀ + 1 > ℵ₀, ℵ₀ · 2 = 2·ℵ₀.",
        answerB = "Für unendliche Kardinalzahlen gilt: ℵ₀ + ℵ₀ = ℵ₀, ℵ₀ · ℵ₀ = ℵ₀, aber 2^ℵ₀ > ℵ₀.",
        answerC = "Unendliche Kardinalzahlen können nicht addiert oder multipliziert werden.",
        answerD = "ℵ₀ · ℵ₀ = ℵ₁ ist der Standardsatz der Kardinalzahlarithmetik.",
        correctAnswer = 1,
        explanation = "Für unendliche Kardinalzahlen κ gilt: κ + κ = κ und κ · κ = κ (diese Identitäten folgen aus AC). Das bedeutet: ℵ₀ + ℵ₀ = ℵ₀ (ℤ ist abzählbar), ℵ₀ · ℵ₀ = ℵ₀ (ℕ×ℕ ist abzählbar). Aber 2^ℵ₀ > ℵ₀ (Cantors Satz).",
        difficulty = 4,
        funFact = null
    ),

    // ── FRAGE 27 ──────────────────────────────────────────────────────────────
    Question(
        categoryId = 12,
        questionText = "Was ist ein 'inaccessible cardinal' (unerreichbare Kardinalzahl)?",
        answerA = "Eine Kardinalzahl, die nicht durch endliche Operationen aus ℵ₀ erreicht werden kann.",
        answerB = "Eine reguläre Grenzkardinalzahl κ > ℵ₀, die nicht durch kleinere Kardinalzahlen durch Potenzmenge oder Summen erreicht werden kann — ihre Existenz ist von ZFC unbeweisbar.",
        answerC = "Eine Kardinalzahl, die größer als alle Aleph-Zahlen ist.",
        answerD = "Jede überabzählbare Kardinalzahl, die durch das Auswahlaxiom entsteht.",
        correctAnswer = 1,
        explanation = "Eine stark unerreichbare Kardinalzahl κ ist regulär (kofinal zu keinem kleineren Ordinal), ein Grenzordinal, und für alle λ < κ gilt 2^λ < κ. Die Existenz solcher Kardinalzahlen kann in ZFC weder bewiesen noch widerlegt werden — sie sind 'große Kardinalzahlen' und begründen Large-Cardinal-Axiome.",
        difficulty = 4,
        funFact = "Wenn inaccessible cardinals existieren, dann folgt die Konsistenz von ZFC daraus — was nach Gödels Unvollständigkeitssatz in ZFC selbst nicht beweisbar ist. Sie liegen also 'jenseits' von ZFC."
    ),

    // ── FRAGE 28 ──────────────────────────────────────────────────────────────
    Question(
        categoryId = 12,
        questionText = "Wie lautet Cantors erstes Diagonalargument und was beweist es?",
        answerA = "Es beweist, dass die reellen Zahlen überabzählbar sind, durch ein Diagonalkonstruktionsargument.",
        answerB = "Es zeigt, dass ℕ×ℕ abzählbar ist — durch einen Zickzack-Pfad durch die Matrix der rationalen Zahlen, womit ℚ abzählbar ist.",
        answerC = "Es beweist, dass jede Menge gleichmächtig zu ihrer Potenzmenge ist.",
        answerD = "Es zeigt, dass es keine kleinste überabzählbare Menge gibt.",
        correctAnswer = 1,
        explanation = "Cantors erstes Diagonalverfahren (1874) zählt alle positiven rationalen Zahlen auf: Man ordnet sie in einer unendlichen Matrix an (Zähler × Nenner) und geht diagonal durch die Felder, überspringt Wiederholungen. Das zeigt: |ℕ×ℕ| = ℵ₀ und damit |ℚ| = ℵ₀.",
        difficulty = 4,
        funFact = "Das zweite Diagonalverfahren (1891) beweist das Gegenteil für ℝ: kein Auflistungsversuch kann alle reellen Zahlen erfassen. Die beiden Verfahren sind konzeptuell verschieden, obwohl beide 'diagonal' genannt werden."
    ),

    // ── FRAGE 29 ──────────────────────────────────────────────────────────────
    Question(
        categoryId = 12,
        questionText = "Was ist der Unterschied zwischen einer 'echten Klasse' und einer 'Menge' in ZFC?",
        answerA = "Es gibt keinen Unterschied — beides sind Sammlungen von Objekten.",
        answerB = "Eine echte Klasse ist einfach eine sehr große Menge.",
        answerC = "Mengen sind Element anderer Mengen; echte Klassen (wie die Klasse aller Mengen V) können nicht selbst Element einer Menge sein — sonst entstehen Paradoxa.",
        answerD = "Klassen existieren nur in der Philosophie, nicht in der Mathematik.",
        correctAnswer = 2,
        explanation = "In ZFC gibt es nur Mengen — 'Klassen' sind ein informelles Konzept für 'zu große' Sammlungen. In formalen Systemen wie NBG (von Neumann-Bernays-Gödel) sind echte Klassen explizit: Sie existieren, können aber kein Element sein. Beispiele echter Klassen: die Klasse aller Mengen V, die Klasse aller Ordinalzahlen On.",
        difficulty = 4,
        funFact = "John von Neumann führte echte Klassen 1925 formell in die Mathematik ein, um Paradoxa wie Russells und Burali-Fortis zu vermeiden, ohne dabei auf nützliche Konzepte zu verzichten."
    ),

    // ── FRAGE 30 ──────────────────────────────────────────────────────────────
    Question(
        categoryId = 12,
        questionText = "Was ist ε₀ (Epsilon-Null) in der transfiniten Ordinalzahlarithmetik?",
        answerA = "Die kleinste überabzählbare Ordinalzahl — identisch mit ω₁.",
        answerB = "Die kleinste Ordinalzahl ε mit ωᵉ = ε, also der kleinste Fixpunkt der Funktion α ↦ ωᵅ.",
        answerC = "Eine sehr kleine positive reelle Zahl nahe 0, ähnlich wie in der Analysis.",
        answerD = "Die Ordinalzahl ω^ω — das erste transfinite Potenzieren.",
        correctAnswer = 1,
        explanation = "ε₀ ist der kleinste Fixpunkt der Exponentialfunktion zur Basis ω: ε₀ = ω^(ω^(ω^...)) (unendlicher Turm). Es gilt ω^ε₀ = ε₀. ε₀ ist zählbar, erscheint aber in der Beweistheorie: Gentzen bewies 1936, dass die Widerspruchsfreiheit der Peano-Arithmetik transfinite Induktion bis ε₀ erfordert.",
        difficulty = 4,
        funFact = "ε₀ ist zählbar — kleiner als ω₁. Dennoch benötigt man ε₀ für Konsistenzbeweise der Arithmetik. Das zeigt, wie reich die Struktur abzählbarer Ordinalzahlen ist."
    ),

    // ── FRAGE 31 ──────────────────────────────────────────────────────────────
    Question(
        categoryId = 12,
        questionText = "Was sagt Königs Lemma (im Kontext der Mengenlehre) über Kardinalzahlen?",
        answerA = "Jede unendliche Menge enthält eine abzählbare Teilmenge.",
        answerB = "Für Kardinalzahlen gilt: Σᵢ κᵢ < Πᵢ λᵢ, wenn κᵢ < λᵢ für alle i und die Indexmenge abzählbar ist.",
        answerC = "Jeder unendliche Baum mit endlichen Ebenen hat einen unendlichen Pfad.",
        answerD = "Die Summe zweier Kardinalzahlen ist immer die größere der beiden.",
        correctAnswer = 2,
        explanation = "Königs Theorem (1905): Wenn κᵢ < λᵢ für alle i ∈ I, dann Σᵢ κᵢ < Πᵢ λᵢ. Eine wichtige Konsequenz: cf(2^ℵ₀) > ℵ₀ — die Kofinalität von 2^ℵ₀ ist überabzählbar. Damit kann z.B. 2^ℵ₀ = ℵ_ω nicht gelten (da cf(ℵ_ω) = ω = ℵ₀).",
        difficulty = 4,
        funFact = null
    ),

    // ── FRAGE 32 ──────────────────────────────────────────────────────────────
    Question(
        categoryId = 12,
        questionText = "Was ist die 'Kofinalität' einer Ordinalzahl?",
        answerA = "Die Mächtigkeit der größten Teilmenge einer Ordinalzahl.",
        answerB = "Die kleinste Kardinalzahl κ, sodass es eine unbeschränkte Funktion von κ in die Ordinalzahl gibt — ein Maß für die 'Erreichbarkeit von oben'.",
        answerC = "Der Nachfolger einer Ordinalzahl in der Aleph-Hierarchie.",
        answerD = "Das Komplement einer Ordinalzahl bezüglich der Klasse aller Ordinalzahlen.",
        correctAnswer = 1,
        explanation = "cf(α) ist die Kofinalität von α: die kleinste Ordinalzahl β für die es eine unbeschränkte Folge (aᵢ)ᵢ<β mit aᵢ < α und sup aᵢ = α gibt. Beispiel: cf(ω) = ω, cf(ω₁) = ω₁ (reguläre Kardinalzahl), cf(ℵ_ω) = ω (singuläre Kardinalzahl).",
        difficulty = 4,
        funFact = "Reguläre Kardinalzahlen (cf(κ) = κ) und singuläre Kardinalzahlen (cf(κ) < κ) haben fundamental verschiedene Eigenschaften. ℵ_ω ist die kleinste singuläre Kardinalzahl."
    ),

    // ── FRAGE 33 ──────────────────────────────────────────────────────────────
    Question(
        categoryId = 12,
        questionText = "Was beweist das 'Theorem von Cantor-Bendixson'?",
        answerA = "Jede abzählbare Menge reeller Zahlen ist nirgends dicht.",
        answerB = "Jede abgeschlossene Menge reeller Zahlen lässt sich in eine perfekte Menge und eine abzählbare Menge zerlegen.",
        answerC = "Jede überabzählbare Menge enthält eine Kopie von ℝ.",
        answerD = "Abzählbare Mengen haben kein Häufungspunkt.",
        correctAnswer = 1,
        explanation = "Cantor-Bendixson (1883): Jede abgeschlossene Menge F ⊆ ℝ lässt sich eindeutig schreiben als F = P ∪ C, wobei P perfekt (abgeschlossen, keine isolierten Punkte) und C abzählbar ist. Dies ist eine frühe Strukturaussage über Mengen — und ein Vorläufer der deskriptiven Mengenlehre.",
        difficulty = 4,
        funFact = "Ivar Otto Bendixson (1861–1935) arbeitete in Stockholm und leistete wichtige Beiträge zur Analysis und Mengenlehre. Das Theorem ist ein Meilenstein auf dem Weg zur modernen Topologie."
    ),

    // ── FRAGE 34 ──────────────────────────────────────────────────────────────
    Question(
        categoryId = 12,
        questionText = "Was ist ein 'Fixpunktsatz' im Kontext transfiniter Mengenlehre (Knaster-Tarski)?",
        answerA = "Jede stetige Funktion auf ℝ hat mindestens einen Fixpunkt.",
        answerB = "Jede monotone Funktion auf einem vollständigen Verband hat mindestens einen Fixpunkt — und die Menge aller Fixpunkte bildet selbst einen vollständigen Verband.",
        answerC = "Ordinalzahlen sind Fixpunkte der Nachfolgerfunktion.",
        answerD = "Die leere Menge ist der einzige Fixpunkt der Potenzmengenfunktion.",
        correctAnswer = 1,
        explanation = "Der Fixpunktsatz von Knaster-Tarski (1955): Wenn f: L → L monoton auf einem vollständigen Verband L ist, hat f einen größten und einen kleinsten Fixpunkt. Dieser Satz wird in der Mengenlehre, Informatik (Semantik rekursiver Programme) und Logik (Definierbarkeit) verwendet.",
        difficulty = 4,
        funFact = null
    ),

    // ── FRAGE 35 ──────────────────────────────────────────────────────────────
    Question(
        categoryId = 12,
        questionText = "Was ist das 'Unendlichkeitsaxiom' in ZFC?",
        answerA = "Es postuliert, dass Gott unendlich ist — ein philosophisches Axiom.",
        answerB = "Es garantiert die Existenz einer unendlichen Menge, nämlich die Menge aller natürlichen Zahlen ℕ.",
        answerC = "Es besagt, dass jede Menge eine unendliche Potenzmenge hat.",
        answerD = "Es erklärt, dass zwischen je zwei natürlichen Zahlen unendlich viele rationale Zahlen liegen.",
        correctAnswer = 1,
        explanation = "Das Unendlichkeitsaxiom in ZF: Es gibt eine Menge I, sodass ∅ ∈ I und für alle x ∈ I gilt x ∪ {x} ∈ I. Dies garantiert die Existenz einer Menge, die alle natürlichen Zahlen enthält (in von-Neumann-Kodierung: 0=∅, 1={∅}, 2={∅,{∅}}, ...). Ohne dieses Axiom lässt sich ℕ nicht beweisen.",
        difficulty = 4,
        funFact = "Die Axiome von ZFC reichen für nahezu alle 'gewöhnlichen' Mathematik aus. Nur in sehr speziellen Bereichen (große Kardinale, gewisse Topologiefragen) werden stärkere Annahmen benötigt."
    ),

    // ── FRAGE 36 ──────────────────────────────────────────────────────────────
    Question(
        categoryId = 12,
        questionText = "Was ist die 'verallgemeinerte Kontinuumshypothese' (GCH)?",
        answerA = "Für alle Kardinalzahlen κ gilt: 2^κ = κ⁺ — die nächste Kardinalzahl nach κ.",
        answerB = "Für alle n ∈ ℕ gilt: 2^ℵₙ = ℵₙ₊₁.",
        answerC = "Beide obigen Aussagen (A und B) sind äquivalente Formulierungen der GCH.",
        answerD = "Die GCH besagt, dass alle Potenzmengenkardinalzahlen abzählbar sind.",
        correctAnswer = 2,
        explanation = "Die GCH verallgemeinert die KH: Für jede unendliche Kardinalzahl κ gilt 2^κ = κ⁺ (der direkte Nachfolger). Für κ = ℵ₀ ist das gerade die Kontinuumshypothese. Wie die KH ist die GCH von ZFC unabhängig (Gödel + Cohen). Gödel zeigte: GCH ist konsistent mit ZFC.",
        difficulty = 4,
        funFact = "Unter der GCH vereinfacht sich die Kardinalzahlarithmetik dramatisch: 2^ℵ_α = ℵ_{α+1} für alle Ordinalzahlen α. Das macht viele Beweise in der Topologie und Algebra kürzer."
    ),

    // ── FRAGE 37 ──────────────────────────────────────────────────────────────
    Question(
        categoryId = 12,
        questionText = "Cantor geriet wegen seiner Mengenlehre in Konflikt mit dem Mathematiker Leopold Kronecker. Was war Kroneckers Hauptkritik?",
        answerA = "Cantor hätte mehr Axiome verwenden sollen.",
        answerB = "Kronecker lehnte aktuale Unendlichkeit ab: Nur endliche, konstruktiv aufbaubare Objekte seien in der Mathematik legitim — unendliche Mengen seien sinnlos.",
        answerC = "Kronecker fand Cantors Notation für Kardinalzahlen unelegant.",
        answerD = "Kronecker zweifelte an der Gültigkeit von Cantors Diagonalbeweis für rationale Zahlen.",
        correctAnswer = 1,
        explanation = "Kronecker war Vertreter des mathematischen Finitismus und lehnte vollendete (aktuale) Unendlichkeit ab. Sein berühmtes Diktum: 'Die ganzen Zahlen hat der liebe Gott gemacht, alles andere ist Menschenwerk.' Er bezeichnete Cantor abfällig als 'Verderber der Jugend'.",
        difficulty = 4,
        funFact = "Cantor litt schwer unter Kroneckers Ablehnung und unter akademischer Isolierung. Er verbrachte mehrere Jahre in psychiatrischen Einrichtungen — möglicherweise durch den anhaltenden Stress des akademischen Konflikts mitverursacht."
    ),

    // ── FRAGE 38 ──────────────────────────────────────────────────────────────
    Question(
        categoryId = 12,
        questionText = "Was ist ein 'Ultrafilter' und welche Rolle spielt er in der Mengenlehre?",
        answerA = "Ein Filter, der alle Mengen außer der leeren Menge enthält.",
        answerB = "Ein maximaler Filter auf einer Menge — für jede Teilmenge A ist entweder A oder ihr Komplement im Ultrafilter. Ultrafilter werden für Modelltheorie (Ultraprodukte) und nicht-standardmäßige Analysis genutzt.",
        answerC = "Ein Filter, der nur endliche Mengen enthält.",
        answerD = "Eine Ordnung auf unendlichen Mengen, die transitive Vergleiche erlaubt.",
        correctAnswer = 1,
        explanation = "Ein Ultrafilter U auf einer Menge X ist ein Filter, sodass für alle A ⊆ X gilt: A ∈ U oder X∖A ∈ U (aber nicht beides). Freie Ultrafilter (kein endliches Element) existieren nur unter AC. Sie erlauben Ultraprodukte — Grundlage der nicht-standardmäßigen Analysis von Abraham Robinson (1960).",
        difficulty = 4,
        funFact = "Mit Ultrafiltern konstruierte Robinson 1966 eine rigorose Version der Infinitesimalrechnung von Leibniz und Newton — mit echten 'unendlich kleinen' Zahlen, die mathematisch präzise sind."
    ),

    // ── FRAGE 39 ──────────────────────────────────────────────────────────────
    Question(
        categoryId = 12,
        questionText = "Was sagt Gödels erster Unvollständigkeitssatz über formale Systeme wie die Mengenlehre?",
        answerA = "Jedes konsistente formale System, das die Arithmetik enthält, ist vollständig — alle wahren Aussagen sind beweisbar.",
        answerB = "Jedes hinreichend starke konsistente formale System enthält wahre Aussagen, die in diesem System nicht beweisbar sind.",
        answerC = "Die Mengenlehre ist widersprüchlich, wie Russells Paradoxon zeigt.",
        answerD = "Formale Systeme können niemals vollständig sein, weil es unendlich viele Axiome braucht.",
        correctAnswer = 1,
        explanation = "Gödels erster Unvollständigkeitssatz (1931): Jedes konsistente formale System F, das die Peano-Arithmetik enthält, ist unvollständig — es gibt einen Satz G_F, der in F weder beweisbar noch widerlegbar ist (die 'Gödel-Aussage' der Art 'Ich bin nicht beweisbar'). Die KH ist ein konkretes Beispiel in ZFC.",
        difficulty = 4,
        funFact = "Gödel war 25 Jahre alt, als er diesen Satz bewies. Er erschütterte die Grundlagen der Mathematik, da Hilbert kurz zuvor das Programm gestartet hatte, alle Mathematik auf ein vollständiges, widerspruchsfreies Axiomensystem zu gründen."
    ),

    // ── FRAGE 40 ──────────────────────────────────────────────────────────────
    Question(
        categoryId = 12,
        questionText = "Was ist der 'konstruierbare Universums' L von Gödel?",
        answerA = "Die kleinste transitive Klasse, die alle Ordinalzahlen enthält und unter den ZF-Operationen abgeschlossen ist — ein Modell von ZFC + GCH.",
        answerB = "Eine Menge aller konstruierbaren reellen Zahlen, die von den irrationalen Zahlen verschieden sind.",
        answerC = "Das Universum aller endlichen Mengen, ohne die aktuale Unendlichkeit.",
        answerD = "Die von Cantor konstruierte Klasse aller wohlgeordneten Mengen.",
        correctAnswer = 0,
        explanation = "Gödels konstruierbares Universum L (1938) wird durch transfinite Induktion aufgebaut: L₀ = ∅, L_{α+1} = {X ⊆ Lα | X ist in Lα definierbar}, L = ∪ Lα. In L gelten AC und GCH — damit zeigte Gödel deren Konsistenz mit ZF. Das Axiom 'V = L' (jede Menge ist konstruierbar) ist konsistent, aber viele Mengenlehre-Experten glauben es nicht.",
        difficulty = 4,
        funFact = "Hugh Woodin und andere Mengenlehre-Forscher arbeiten heute an 'V ≠ L' — dem Versuch zu zeigen, dass das mathematische Universum reicher ist als Gödels konstruierbares L."
    ),

    // ── FRAGE 41 ──────────────────────────────────────────────────────────────
    Question(
        categoryId = 12,
        questionText = "Was ist die 'Mächtigkeit des Kontinuums' (|ℝ| = 𝔠)?",
        answerA = "𝔠 = ℵ₁ — das ist die Definition des Kontinuums.",
        answerB = "𝔠 = 2^ℵ₀ — die Mächtigkeit der Potenzmenge von ℕ, also die Anzahl der reellen Zahlen.",
        answerC = "𝔠 = ℵ₀² — das Quadrat der abzählbaren Unendlichkeit.",
        answerD = "𝔠 ist undefiniert, da ℝ überabzählbar ist.",
        correctAnswer = 1,
        explanation = "Die Mächtigkeit des Kontinuums 𝔠 = |ℝ| = 2^ℵ₀. Diese Gleichheit folgt aus der Bijektion zwischen ℝ und P(ℕ): Jede reelle Zahl hat eine eindeutige Binärdarstellung (mit Ausnahmen der Dyadischen, die vernachlässigbar sind). Ob 𝔠 = ℵ₁ (KH), ist von ZFC unabhängig.",
        difficulty = 4,
        funFact = "Es gibt einfachere Bijektionen zwischen (0,1) und ℝ: zum Beispiel x ↦ tan(π(x−1/2)). Das zeigt, dass das offene Intervall (0,1) und die gesamte Zahlengerade dieselbe Mächtigkeit haben."
    ),

    // ── FRAGE 42 ──────────────────────────────────────────────────────────────
    Question(
        categoryId = 12,
        questionText = "Was ist 'transfinite Induktion' und wo wird sie angewendet?",
        answerA = "Eine Verallgemeinerung der vollständigen Induktion auf alle Ordinalzahlen — nicht nur auf natürliche Zahlen.",
        answerB = "Eine Induktion, die für unendliche Schritte keinen Basisfall benötigt.",
        answerC = "Dasselbe wie gewöhnliche Induktion, nur mit anderen Symbolen.",
        answerD = "Eine Methode, die nur für überabzählbare Mengen gilt.",
        correctAnswer = 0,
        explanation = "Transfinite Induktion: Um zu beweisen, dass alle Ordinalzahlen eine Eigenschaft P haben, genügt es zu zeigen: (1) P(0), (2) P(α) → P(α+1) für alle α, (3) wenn P(β) für alle β < α gilt, dann auch P(α) für Limes-Ordinalzahlen α. Sie wird für Beweise über wohlgeordnete Klassen und für transfinite Rekursion (Definition von Funktionen auf allen Ordinalzahlen) verwendet.",
        difficulty = 4,
        funFact = null
    ),

    // ── FRAGE 43 ──────────────────────────────────────────────────────────────
    Question(
        categoryId = 12,
        questionText = "Was ist der 'Satz von Löwenheim-Skolem' und welche paradoxe Konsequenz hat er?",
        answerA = "Jedes konsistente Axiomensystem hat ein abzählbares Modell — auch die Mengenlehre mit überabzählbaren Mengen.",
        answerB = "Jede überabzählbare Struktur hat kein abzählbares Teilmodell.",
        answerC = "Formale Systeme mit unendlichen Modellen haben nur überabzählbare Modelle.",
        answerD = "Jedes Modell der Arithmetik ist isomorph zu ℕ.",
        correctAnswer = 0,
        explanation = "Löwenheim-Skolem: Jede Theorie mit einem unendlichen Modell hat auch ein abzählbares Modell. Das 'Skolem-Paradoxon': ZFC spricht über überabzählbare Mengen, aber ZFC selbst hat ein abzählbares Modell — in dem 'überabzählbar' bedeutet: 'keine Bijektion zu ℕ existiert innerhalb des Modells', obwohl eine solche von außen existieren kann.",
        difficulty = 4,
        funFact = "Skolem nannte dieses Paradoxon 1922 ein 'relatives Unendlichkeitsprinzip' — Überabzählbarkeit ist kein absolutes Konzept, sondern relativ zu einem Modell. Dies erschütterte die Hoffnung auf eine eindeutige Mengenlehre."
    ),

    // ── FRAGE 44 ──────────────────────────────────────────────────────────────
    Question(
        categoryId = 12,
        questionText = "Was ist ein 'Cantor-Raum' in der Topologie?",
        answerA = "Der metrische Raum aller reellen Zahlen mit der üblichen Metrik.",
        answerB = "Die Menge {0,1}^ℕ aller unendlichen binären Folgen mit der Produkttopologie — ein kompakter, perfekter, total unzusammenhängender Raum.",
        answerC = "Die Ordinalzahl ω mit der Ordnungstopologie.",
        answerD = "Ein Raum, in dem jede Menge abzählbar ist.",
        correctAnswer = 1,
        explanation = "Der Cantor-Raum {0,1}^ℕ ist das Produkt abzählbar vieler zweipunktiger Mengen. Er ist homöomorph zur Cantor-Menge (in [0,1]). Seine Mächtigkeit ist 2^ℵ₀ = |ℝ|. Jeder kompakte, metrische, perfekte, total unzusammenhängende Raum ist homöomorph zum Cantor-Raum (Cantor-Satz).",
        difficulty = 4,
        funFact = "Der Cantor-Raum ist ein 'universeller' Raum für kompakte metrische Räume: Jeder solche Raum ist ein kontinuierliches Bild des Cantor-Raums — ein tiefer Satz der deskriptiven Topologie."
    ),

    // ── FRAGE 45 ──────────────────────────────────────────────────────────────
    Question(
        categoryId = 12,
        questionText = "Welche Aussage über die Menge der algebraischen Zahlen ist korrekt?",
        answerA = "Die algebraischen Zahlen sind überabzählbar, da es unendlich viele Polynomgrade gibt.",
        answerB = "Die algebraischen Zahlen sind abzählbar unendlich — Cantor bewies dies 1874 als erstes.",
        answerC = "Alle reellen Zahlen sind algebraisch.",
        answerD = "Die algebraischen Zahlen bilden eine endliche Menge.",
        correctAnswer = 1,
        explanation = "Eine Zahl heißt algebraisch, wenn sie Nullstelle eines Polynoms mit ganzzahligen Koeffizienten ist. Die Menge der Polynome mit ganzzahligen Koeffizienten ist abzählbar (abzählbare Vereinigung endlicher Mengen), jedes hat endlich viele Nullstellen — also sind die algebraischen Zahlen abzählbar. Die nicht-algebraischen Zahlen heißen transzendent (z.B. π, e).",
        difficulty = 4,
        funFact = "Cantor bewies 1874 als unmittelbare Konsequenz: Die meisten reellen Zahlen sind transzendent — die algebraischen bilden nur eine 'kleine' (Maß-null) Teilmenge von ℝ, obwohl transzendente Zahlen wie π sehr schwer zu finden sind."
    ),

    // ── FRAGE 46 ──────────────────────────────────────────────────────────────
    Question(
        categoryId = 12,
        questionText = "Was ist ein 'messbares Kardinal' (measurable cardinal)?",
        answerA = "Eine Kardinalzahl κ, die durch Lebesgue-Maß gemessen werden kann.",
        answerB = "Eine überabzählbare Kardinalzahl κ, die ein κ-vollständiges nicht-triviales {0,1}-wertiges Maß trägt — ihre Existenz übersteigt ZFC und impliziert viele Konsistenzergebnisse.",
        answerC = "Jede reguläre Kardinalzahl unter ℵ_ω.",
        answerD = "Eine Kardinalzahl, die in Gödels konstruierbarem Universum L liegt.",
        correctAnswer = 1,
        explanation = "Eine messbare Kardinalzahl κ trägt ein κ-vollständiges ultrafilter-basiertes Maß. Ihre Existenz kann in ZFC nicht bewiesen werden — sie ist ein 'Large Cardinal Axiom'. Aus der Existenz eines messbaren Kardinals folgt: L ≠ V (das Universum ist nicht konstruierbar) und 0# existiert (eine bestimmte mengenlehre-theoretische Struktur).",
        difficulty = 4,
        funFact = "Die Theorie der 'großen Kardinale' ist ein aktives Forschungsgebiet und versucht, die Lücken aufzufüllen, die Gödels Unvollständigkeitssatz hinterlässt — durch immer stärkere Konsistenzaxiome."
    ),

    // ── FRAGE 47 ──────────────────────────────────────────────────────────────
    Question(
        categoryId = 12,
        questionText = "Was ist der Unterschied zwischen 'potentieller' und 'aktualer' Unendlichkeit?",
        answerA = "Potentielle Unendlichkeit ist unendlich groß, aktuale Unendlichkeit ist unendlich klein.",
        answerB = "Potentielle Unendlichkeit bezeichnet einen Prozess ohne Ende (immer weiter zählbar); aktuale Unendlichkeit bezeichnet eine vollendete unendliche Gesamtheit als fertiges Objekt.",
        answerC = "Beide Konzepte sind in der modernen Mathematik identisch.",
        answerD = "Potentielle Unendlichkeit ist abzählbar, aktuale Unendlichkeit ist überabzählbar.",
        correctAnswer = 1,
        explanation = "Aristoteles unterschied: potentiell unendlich = die Möglichkeit immer weiterzumachen (wie die natürlichen Zahlen im Sinne von 'es gibt immer eine nächste'). Aktual unendlich = die Gesamtheit aller natürlichen Zahlen als fertige Menge. Cantor benutzte die aktuale Unendlichkeit — Kronecker und andere Finitisten lehnten sie ab.",
        difficulty = 4,
        funFact = "Aristoteles lehnte aktuale Unendlichkeit in der Physik ab: Das Universum könne nicht unendlich groß sein. Heutige Kosmologen sind uneinig, ob das Universum räumlich endlich oder unendlich ist."
    ),

    // ── FRAGE 48 ──────────────────────────────────────────────────────────────
    Question(
        categoryId = 12,
        questionText = "Was ist das 'Axiom der Fundierung' (Fundierungsaxiom, Regularitätsaxiom) in ZFC?",
        answerA = "Jede nichtleere Menge A enthält ein Element m ∈ A mit A ∩ m = ∅ — das verhindert unendliche absteigende ∈-Ketten wie ... ∈ x₂ ∈ x₁ ∈ x₀.",
        answerB = "Jede Menge hat ein kleinstes Element bezüglich ∈.",
        answerC = "Die leere Menge ist in jeder anderen Menge enthalten.",
        answerD = "Mengen können sich nicht selbst enthalten, außer bei Klassen.",
        correctAnswer = 0,
        explanation = "Das Fundierungsaxiom (ZF): ∀A (A ≠ ∅ → ∃m ∈ A: A ∩ m = ∅). Konsequenzen: x ∉ x für alle Mengen x (verhindert Selbstreferenz), keine unendlichen absteigenden ∈-Ketten. In der 'Nicht-wohlgegründeten Mengenlehre' (Non-well-founded Set Theory) wird dieses Axiom durch Antifundierungsaxiome ersetzt.",
        difficulty = 4,
        funFact = "Peter Aczel entwickelte 1988 Antifundierungs-Mengenlehre, in der zirkuläre Mengen wie x = {x} erlaubt sind. Diese wird in der Informatik für die Semantik zirkulärer Prozesse (Coinduktion) verwendet."
    ),

    // ── FRAGE 49 ──────────────────────────────────────────────────────────────
    Question(
        categoryId = 12,
        questionText = "Was ist die 'Theorie der Typen' (Type Theory) und warum entwickelte Russell sie?",
        answerA = "Eine Programmiersprachen-Theorie, entwickelt um typsichere Software zu schreiben.",
        answerB = "Russells Lösung für sein Paradoxon: Mengen werden in Typen hierarchisch eingeordnet — eine Menge darf nur Elemente niedrigerer Typen enthalten, was Selbstreferenz verhindert.",
        answerC = "Eine Klassifikation algebraischer Strukturen nach Axiomen.",
        answerD = "Eine Theorie, die zeigt, dass alle mathematischen Objekte Zahlen sind.",
        correctAnswer = 1,
        explanation = "Russell entwickelte 1908 die Typentheorie: Objekte werden in Typen eingeteilt (Typ 0: Individuen, Typ 1: Mengen von Individuen, Typ 2: Mengen von Mengen...). Aussagen sind nur sinnvoll, wenn die Typen passen. Eine Menge vom Typ n kann nur Elemente vom Typ n-1 enthalten — Russells Menge R wäre dann nicht formulierbar.",
        difficulty = 4,
        funFact = "Homotopy Type Theory (HoTT), entwickelt ab 2005, verknüpft Typentheorie mit algebraischer Topologie und bildet die Grundlage für moderne Beweisassistenten wie Coq, Lean und Agda."
    ),

    // ── FRAGE 50 ──────────────────────────────────────────────────────────────
    Question(
        categoryId = 12,
        questionText = "Was besagt der 'Satz von Zorn' (Zornsches Lemma) und was ist seine Bedeutung für die Mengenlehre?",
        answerA = "Jede nichtleere Menge hat ein maximales Element.",
        answerB = "Wenn jede totalgeordnete Teilmenge (Kette) einer partiell geordneten Menge eine obere Schranke hat, dann besitzt die geordnete Menge mindestens ein maximales Element.",
        answerC = "In jeder wohlgeordneten Menge gibt es ein größtes Element.",
        answerD = "Jede endliche partiell geordnete Menge hat ein eindeutiges Maximum.",
        correctAnswer = 1,
        explanation = "Das Zornsche Lemma (Max Zorn, 1935): Wenn in einer partiell geordneten Menge (M, ≤) jede Kette (totalgeordnete Teilmenge) eine obere Schranke in M hat, dann hat M ein maximales Element. Das Zornsche Lemma ist äquivalent zum Auswahlaxiom und zum Wohlordnungssatz. Es wird verwendet zum Beweis: Jeder Vektorraum hat eine Basis, jeder Ring hat ein maximales Ideal, etc.",
        difficulty = 4,
        funFact = "Der polnische Mathematiker Kazimierz Kuratowski bewies das Lemma 1922, also vor Zorn. In Europa heißt es daher oft 'Kuratowski-Zorn-Lemma'. In Amerika hat sich 'Zorns Lemma' durchgesetzt — ein Beispiel für das Stigler-Gesetz: Entdeckungen werden selten nach dem ersten Entdecker benannt."
    )
)

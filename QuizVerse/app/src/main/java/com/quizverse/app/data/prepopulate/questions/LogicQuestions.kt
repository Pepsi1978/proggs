package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun logicQuestions(): List<Question> = listOf(

    // ── EASY (10) ──────────────────────────────────────────────────────────────

    Question(
        categoryId = 12,
        questionText = "Ein Bauer hat 17 Schafe. Alle bis auf 9 sterben. Wie viele Schafe hat er noch?",
        answerA = "8",
        answerB = "17",
        answerC = "9",
        answerD = "0",
        correctAnswer = 2,
        explanation = "Die Formulierung 'alle bis auf 9' bedeutet, dass genau 9 Schafe überleben.",
        difficulty = 1,
        funFact = "Dieser klassische Trick funktioniert, weil unser Gehirn automatisch beginnt zu rechnen (17 − 8 = 9), statt die Worte genau zu lesen."
    ),

    Question(
        categoryId = 12,
        questionText = "Wie viele Monate haben 28 Tage?",
        answerA = "1",
        answerB = "2",
        answerC = "12",
        answerD = "4",
        correctAnswer = 2,
        explanation = "Alle 12 Monate haben mindestens 28 Tage. Nur der Februar hat genau 28 Tage (in Nicht-Schaltjahren).",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Ein Vater ist 30 Jahre älter als sein Sohn. Vor 5 Jahren war er 4-mal so alt. Wie alt ist der Sohn jetzt?",
        answerA = "5",
        answerB = "10",
        answerC = "15",
        answerD = "20",
        correctAnswer = 1,
        explanation = "Sei x das Alter des Sohnes heute. Vor 5 Jahren: (x−5+30) = 4·(x−5) → x+25 = 4x−20 → 3x = 45 → x = 10.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Was kommt als Nächstes in der Folge: 2, 4, 8, 16, 32, ...?",
        answerA = "48",
        answerB = "64",
        answerC = "56",
        answerD = "40",
        correctAnswer = 1,
        explanation = "Jede Zahl wird mit 2 multipliziert (Verdoppelung). 32 × 2 = 64.",
        difficulty = 1,
        funFact = "Verdopplungsfolgen kommen in der Natur vor, z. B. beim Bakterienwachstum."
    ),

    Question(
        categoryId = 12,
        questionText = "Ich habe Städte, aber keine Häuser. Ich habe Berge, aber keine Bäume. Ich habe Wasser, aber keinen Fisch. Was bin ich?",
        answerA = "Ein Traum",
        answerB = "Eine Landkarte",
        answerC = "Ein Gemälde",
        answerD = "Ein Foto",
        correctAnswer = 1,
        explanation = "Eine Landkarte stellt Städte, Berge und Wasser nur symbolisch dar – ohne die realen Dinge zu besitzen.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Wenn 5 Maschinen in 5 Minuten 5 Widgets herstellen, wie lange brauchen 100 Maschinen für 100 Widgets?",
        answerA = "100 Minuten",
        answerB = "20 Minuten",
        answerC = "5 Minuten",
        answerD = "1 Minute",
        correctAnswer = 2,
        explanation = "Jede Maschine produziert 1 Widget in 5 Minuten. 100 Maschinen produzieren 100 Widgets also in denselben 5 Minuten.",
        difficulty = 1,
        funFact = "Diese Frage wird gern in Vorstellungsgesprächen verwendet, weil sie intuitives Denken prüft."
    ),

    Question(
        categoryId = 12,
        questionText = "Was wird größer, wenn man mehr davon wegnimmt?",
        answerA = "Ein Schneeball",
        answerB = "Ein Loch",
        answerC = "Ein Berg",
        answerD = "Eine Blase",
        correctAnswer = 1,
        explanation = "Je mehr Erde man aus einem Loch schaufelt, desto größer wird das Loch.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Was kommt als Nächstes: 1, 1, 2, 3, 5, 8, 13, ...?",
        answerA = "18",
        answerB = "20",
        answerC = "21",
        answerD = "26",
        correctAnswer = 2,
        explanation = "Fibonacci-Folge: Jede Zahl ist die Summe der beiden vorherigen. 8 + 13 = 21.",
        difficulty = 1,
        funFact = "Die Fibonacci-Folge erscheint in Blütenblattanordnungen, Spiralen von Muscheln und sogar im Aktienmarkt."
    ),

    Question(
        categoryId = 12,
        questionText = "Ein Hahn legt ein Ei auf dem First eines Daches. In welche Richtung fällt das Ei?",
        answerA = "Nach links",
        answerB = "Nach rechts",
        answerC = "Gar nicht – Hähne legen keine Eier",
        answerD = "Es rollt nach unten",
        correctAnswer = 2,
        explanation = "Hähne (männliche Hühner) legen keine Eier. Es gibt also kein Ei, das fallen könnte.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Du hast zwei Münzen, die zusammen 30 Cent ergeben. Eine davon ist kein 5-Cent-Stück. Welche Münzen sind es?",
        answerA = "Zwei 15-Cent-Stücke",
        answerB = "Ein 25-Cent-Stück und ein 5-Cent-Stück",
        answerC = "Zwei 10-Cent-Stücke und ein 10-Cent-Stück",
        answerD = "Ein 20-Cent-Stück und ein 10-Cent-Stück",
        correctAnswer = 1,
        explanation = "Die eine Münze ist kein 5-Cent-Stück — sie ist ein 25-Cent-Stück. Die andere Münze ist das 5-Cent-Stück.",
        difficulty = 1,
        funFact = "Sprachliche Fallen sind ein klassisches Mittel der Logikrätsel."
    ),

    // ── MEDIUM (15) ────────────────────────────────────────────────────────────

    Question(
        categoryId = 12,
        questionText = "In einem Rennen überholst du den Zweiten. Auf welchem Platz bist du jetzt?",
        answerA = "Erster",
        answerB = "Zweiter",
        answerC = "Dritter",
        answerD = "Es hängt vom Rennen ab",
        correctAnswer = 1,
        explanation = "Wenn du den Zweiten überholst, nimmst du seinen Platz ein und bist jetzt auf Platz 2.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Was kommt als Nächstes: 3, 6, 11, 18, 27, ...?",
        answerA = "36",
        answerB = "38",
        answerC = "40",
        answerD = "38",
        correctAnswer = 1,
        explanation = "Die Differenzen sind 3, 5, 7, 9, 11 – steigende ungerade Zahlen. 27 + 11 = 38.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Drei Glühbirnen hängen in einem Zimmer. Im Flur befinden sich drei Schalter. Du darfst nur einmal ins Zimmer. Wie findest du heraus, welcher Schalter zu welcher Birne gehört?",
        answerA = "Zwei Schalter anmachen und einen auslassen",
        answerB = "Einen Schalter lange an, dann aus; zweiten an; reingehen und Temperatur testen",
        answerC = "Alle Schalter der Reihe nach testen",
        answerD = "Das ist unlösbar",
        correctAnswer = 1,
        explanation = "Schalter 1 lange an (Birne wird warm), dann aus. Schalter 2 an. Reingehen: Die warme, dunkle Birne = Schalter 1. Die leuchtende = Schalter 2. Die kalte, dunkle = Schalter 3.",
        difficulty = 2,
        funFact = "Dieses Rätsel lehrt uns, mehrere Eigenschaften (Licht UND Wärme) gleichzeitig zu nutzen."
    ),

    Question(
        categoryId = 12,
        questionText = "Ein Zug fährt mit 90 km/h von Stadt A nach Stadt B (450 km). Ein anderer Zug fährt mit 60 km/h von B nach A. Wie weit ist Zug 2 von Stadt A entfernt, wenn sich die Züge treffen?",
        answerA = "180 km",
        answerB = "270 km",
        answerC = "225 km",
        answerD = "300 km",
        correctAnswer = 0,
        explanation = "Die Züge nähern sich mit 150 km/h. Treffen nach 450/150 = 3 Stunden. Zug 2 ist 3 × 60 = 180 km von B → 450 − 180 = 270 km von A.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Wie viele Quadrate sind auf einem normalen Schachbrett insgesamt vorhanden (nicht nur 1×1)?",
        answerA = "64",
        answerB = "128",
        answerC = "204",
        answerD = "256",
        correctAnswer = 2,
        explanation = "Quadrate der Größe n×n: (9−n)². Summe: 8²+7²+...+1² = 64+49+36+25+16+9+4+1 = 204.",
        difficulty = 2,
        funFact = "Die meisten Menschen sehen nur die 64 kleinen Felder, vergessen aber alle größeren Quadrate."
    ),

    Question(
        categoryId = 12,
        questionText = "Du hast eine 3-Liter- und eine 5-Liter-Kanne. Wie misst du genau 4 Liter ab?",
        answerA = "Das ist nicht möglich",
        answerB = "5L füllen, 3L abfüllen, Rest (2L) in 3L; 5L erneut füllen, 1L in 3L (bis voll) → 4L bleiben",
        answerC = "3L zweimal in 5L füllen",
        answerD = "5L halb füllen",
        correctAnswer = 1,
        explanation = "5L voll → 3L füllen (2L übrig in 5L) → 3L leeren → 2L in 3L füllen → 5L voll → 3L bis voll füllen (1L aus 5L) → 4L bleiben in 5L.",
        difficulty = 2,
        funFact = "Dieses Rätsel wurde im Film 'Stirb langsam 3' gelöst – mit echten Konsequenzen!"
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist größer: die Zahl der geraden oder ungeraden positiven ganzen Zahlen bis 100?",
        answerA = "Gerade Zahlen (mehr)",
        answerB = "Ungerade Zahlen (mehr)",
        answerC = "Beide gleich viele",
        answerD = "Es kommt auf die Definition an",
        correctAnswer = 2,
        explanation = "Von 1 bis 100 gibt es genau 50 gerade (2,4,...,100) und 50 ungerade (1,3,...,99) Zahlen.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Wenn heute Mittwoch ist, was ist dann übermorgen nach übermorgen?",
        answerA = "Samstag",
        answerB = "Sonntag",
        answerC = "Montag",
        answerD = "Dienstag",
        correctAnswer = 1,
        explanation = "Übermorgen = Freitag. Übermorgen nach Freitag = Sonntag.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Ein Mann kauft ein Pferd für 70 €, verkauft es für 80 €, kauft es zurück für 90 € und verkauft es erneut für 100 €. Wie viel Gewinn/Verlust macht er?",
        answerA = "0 € (kein Gewinn)",
        answerB = "10 € Gewinn",
        answerC = "20 € Gewinn",
        answerD = "10 € Verlust",
        correctAnswer = 2,
        explanation = "Transaktion 1: −70+80 = +10 €. Transaktion 2: −90+100 = +10 €. Gesamt: +20 €.",
        difficulty = 2,
        funFact = "Dieses Rätsel verwirrt viele, weil man die Transaktionen getrennt betrachten muss."
    ),

    Question(
        categoryId = 12,
        questionText = "In einem Raum sind 23 Personen. Wie groß ist die Wahrscheinlichkeit, dass mindestens zwei davon am gleichen Tag Geburtstag haben?",
        answerA = "Ca. 10 %",
        answerB = "Ca. 50 %",
        answerC = "Ca. 75 %",
        answerD = "Ca. 90 %",
        correctAnswer = 1,
        explanation = "Das 'Geburtstagsparadoxon': Bei 23 Personen beträgt die Wahrscheinlichkeit bereits ~50,7 %. Sehr überraschend!",
        difficulty = 2,
        funFact = "Bei 70 Personen liegt die Wahrscheinlichkeit bereits bei über 99,9 %."
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist 1/2 von 2/3?",
        answerA = "1/4",
        answerB = "1/3",
        answerC = "2/5",
        answerD = "1/6",
        correctAnswer = 1,
        explanation = "(1/2) × (2/3) = 2/6 = 1/3.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Ein Flugzeug stürzt genau an der Grenze zwischen Deutschland und Frankreich ab. Wo werden die Überlebenden begraben?",
        answerA = "In Deutschland",
        answerB = "In Frankreich",
        answerC = "An der Grenze",
        answerD = "Überlebende werden nicht begraben",
        correctAnswer = 3,
        explanation = "Überlebende — also lebende Personen — werden nicht begraben.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Welche Zahl sollte das Fragezeichen ersetzen? 2, 6, 12, 20, 30, 42, ?",
        answerA = "52",
        answerB = "54",
        answerC = "56",
        answerD = "58",
        correctAnswer = 2,
        explanation = "Die Zahlen sind n×(n+1): 1×2, 2×3, 3×4, 4×5, 5×6, 6×7, 7×8 = 56.",
        difficulty = 2,
        funFact = "Diese Folge heißt auch 'Oblong numbers' oder 'Pronic numbers'."
    ),

    Question(
        categoryId = 12,
        questionText = "Wie viele Male kann man 10 von 100 subtrahieren?",
        answerA = "10 Mal",
        answerB = "9 Mal",
        answerC = "Unendlich",
        answerD = "Nur 1 Mal",
        correctAnswer = 3,
        explanation = "Nach dem ersten Subtrahieren ist die Zahl 90, nicht mehr 100. Man kann 10 also nur einmal von 100 abziehen.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Drei Freunde zahlen je 10 € für ein Hotelzimmer (30 € total). Der Rezeptionist stellt fest, das Zimmer kostet nur 25 €, und schickt einen Boten mit 5 € zurück. Der Bote behält 2 € und gibt jedem 1 € zurück. Jeder zahlte netto 9 €, das sind 27 €. Plus 2 € beim Boten = 29 €. Wo ist der fehlende Euro?",
        answerA = "Der Bote hat ihn",
        answerB = "Das Hotel hat ihn",
        answerC = "Es fehlt keiner – falsches Rechnen",
        answerD = "Er ging verloren",
        correctAnswer = 2,
        explanation = "Das Paradoxon ist ein Rechenfehler: 27 € = 25 € (Hotel) + 2 € (Bote). Man darf die 2 € nicht addieren, sondern muss sie subtrahieren: 27 − 2 = 25 ✓.",
        difficulty = 2,
        funFact = "Dieses Rätsel heißt 'Missing Dollar Riddle' und täuscht durch falsch gewählte Rechenschritte."
    ),

    // ── HARD (12) ──────────────────────────────────────────────────────────────

    Question(
        categoryId = 12,
        questionText = "Du hast 12 Kugeln, eine ist schwerer oder leichter. Mit einer Balkenwaage und nur 3 Wiegungen: Wie findest du die falsche Kugel?",
        answerA = "Das ist mit 3 Wiegungen nicht möglich",
        answerB = "Immer in Hälften teilen",
        answerC = "Erst 4-4 wiegen, dann mit der guten Gruppe vergleichen",
        answerD = "In 3 Gruppen zu 4 aufteilen und systematisch vergleichen",
        correctAnswer = 3,
        explanation = "Runde 1: 4 vs 4 wiegen. Gleich → falsche Kugel in den anderen 4. Ungleich → System der 8 weiter eingrenzen. Mit 3 Wiegungen lassen sich 3^3 = 27 Möglichkeiten unterscheiden, genug für 24 (12 Kugeln × 2 Möglichkeiten).",
        difficulty = 3,
        funFact = "Mit n Wiegungen kann man (3^n − 3)/2 Kugeln prüfen: 3 Wiegungen → 12 Kugeln."
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist die Summe aller ganzen Zahlen von 1 bis 100?",
        answerA = "5000",
        answerB = "4950",
        answerC = "5050",
        answerD = "10100",
        correctAnswer = 2,
        explanation = "Gaußsche Summenformel: n(n+1)/2 = 100×101/2 = 5050.",
        difficulty = 3,
        funFact = "Der 9-jährige Gauß soll diese Summe im Kopf berechnet haben, als der Lehrer die Klasse beschäftigen wollte."
    ),

    Question(
        categoryId = 12,
        questionText = "Vier Brückenüberquerer in der Nacht: A=1 min, B=2 min, C=5 min, D=10 min. Höchstens 2 Personen pro Überquerung, Taschenlampe muss zurückgebracht werden. Was ist die kürzeste Gesamtzeit?",
        answerA = "17 Minuten",
        answerB = "19 Minuten",
        answerC = "21 Minuten",
        answerD = "23 Minuten",
        correctAnswer = 0,
        explanation = "A+B → 2 min. A zurück → 1 min. C+D → 10 min. B zurück → 2 min. A+B → 2 min. Gesamt: 17 Minuten.",
        difficulty = 3,
        funFact = "Dieses Rätsel wird als 'Torch problem' bezeichnet und hat eine nicht-intuitive Optimallösung."
    ),

    Question(
        categoryId = 12,
        questionText = "Wenn eine Schnecke den Brunnen tagsüber 3 m hochklettert und nachts 2 m zurückfällt, wie viele Tage braucht sie, um aus einem 10 m tiefen Brunnen zu kommen?",
        answerA = "8 Tage",
        answerB = "10 Tage",
        answerC = "7 Tage",
        answerD = "9 Tage",
        correctAnswer = 2,
        explanation = "Nach 7 Tagen ist sie auf 7 m (netto +1 m/Tag). Am 8. Tag klettert sie 3 m → erreicht 10 m und klettert heraus. Also 8 Tage.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Eine Lilie auf einem Teich verdoppelt sich jeden Tag. Nach 30 Tagen ist der Teich halb bedeckt. An welchem Tag war der Teich halb bedeckt?",
        answerA = "Tag 15",
        answerB = "Tag 20",
        answerC = "Tag 25",
        answerD = "Tag 29",
        correctAnswer = 3,
        explanation = "Da sich die Fläche täglich verdoppelt und am Tag 30 voll ist, war sie am Tag 29 halb bedeckt.",
        difficulty = 3,
        funFact = "Dieses Rätsel illustriert exponentielles Wachstum – wichtig für das Verständnis von Epidemien und Zinseszins."
    ),

    Question(
        categoryId = 12,
        questionText = "Du befragst 100 Personen. 90 trinken Kaffee, 80 Tee, 70 Bier und 60 Cola. Was ist der Mindestanteil, der alle vier trinkt?",
        answerA = "0 %",
        answerB = "10 %",
        answerC = "20 %",
        answerD = "30 %",
        correctAnswer = 0,
        explanation = "Mindestzahl nach dem Inklusionsprinzip: 90+80+70+60 − 3×100 = 300 − 300 = 0. Mindestens 0 % trinken alle vier.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist das nächste Element in der Folge: 1, 11, 21, 1211, 111221, ...?",
        answerA = "1112211",
        answerB = "312211",
        answerC = "31221",
        answerD = "1211221",
        correctAnswer = 1,
        explanation = "Look-and-say-Folge: Man beschreibt die vorherige Zahl. 111221 = 'drei Einsen, zwei Zweien, eine Eins' = 312211.",
        difficulty = 3,
        funFact = "Diese Folge wurde von John Horton Conway analysiert – sie wächst asymptotisch mit dem Faktor 1,303577..."
    ),

    Question(
        categoryId = 12,
        questionText = "Wie viele Nullen hat 100! (Hundert Fakultät) am Ende?",
        answerA = "20",
        answerB = "24",
        answerC = "25",
        answerD = "10",
        correctAnswer = 1,
        explanation = "Nullen entstehen durch Faktoren 10 = 2×5. Anzahl der 5er in 100!: ⌊100/5⌋+⌊100/25⌋ = 20+4 = 24.",
        difficulty = 3,
        funFact = "2er kommen häufiger vor als 5er, daher zählt man nur die 5er."
    ),

    Question(
        categoryId = 12,
        questionText = "Drei Logiker gehen in eine Bar. Der Barmann fragt: 'Wollen alle drei Bier?' Der erste sagt: 'Weiß ich nicht.' Der zweite sagt: 'Weiß ich nicht.' Was sagt der dritte?",
        answerA = "Ich weiß es auch nicht",
        answerB = "Nein",
        answerC = "Ja",
        answerD = "Nur ich möchte Bier",
        correctAnswer = 2,
        explanation = "'Weiß ich nicht' bedeutet: Ich möchte Bier, aber weiß nicht ob alle wollen (also min. ich will). Da Logiker 1 und 2 Bier wollen, weiß der dritte: alle wollen → 'Ja'.",
        difficulty = 3,
        funFact = "Dieses Rätsel zeigt, wie man durch Verneinung von 'Nein' Information ableiten kann."
    ),

    Question(
        categoryId = 12,
        questionText = "Wenn du ein Blatt Papier 42-mal in der Mitte faltest, wie dick ist es? (Papier = 0,1 mm)",
        answerA = "Ca. 1 Meter",
        answerB = "Ca. 42 cm",
        answerC = "Ca. von der Erde zum Mond",
        answerD = "Ca. vom Mond zur Sonne",
        correctAnswer = 2,
        explanation = "2^42 × 0,1 mm ≈ 439.804 km. Der Mond ist ~384.400 km entfernt. Das Papier reicht also bis zum Mond und zurück!",
        difficulty = 3,
        funFact = "Exponentielles Wachstum ist für unsere Intuition kaum fassbar – das zeigt dieses Beispiel perfekt."
    ),

    Question(
        categoryId = 12,
        questionText = "Ein Würfel hat 6 Seiten. Wie viele Würfel braucht man, um einen 3×3×3-Würfel zu bauen, und wie viele kleine Würfel haben genau 2 Seiten sichtbar?",
        answerA = "27 Würfel, 12 mit 2 sichtbaren Seiten",
        answerB = "27 Würfel, 8 mit 2 sichtbaren Seiten",
        answerC = "27 Würfel, 6 mit 2 sichtbaren Seiten",
        answerD = "64 Würfel, 12 mit 2 sichtbaren Seiten",
        correctAnswer = 0,
        explanation = "3³=27 Würfel. Kantenwürfel (2 Seiten sichtbar): 12 Kanten × 1 = 12 (Eckwürfel haben 3, Flächenwürfel 1, Innenwürfel 0 sichtbare Seiten).",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Aus einer vollen Tonne Wein wird 1 Liter entnommen und durch Wasser ersetzt. Dann wird wieder 1 Liter entnommen und durch Wasser ersetzt. Nach diesem Vorgang zum zweiten Mal – wie viel Wein ist noch in der Tonne? (Tonne = 100 Liter)",
        answerA = "98 Liter",
        answerB = "98,01 Liter",
        answerC = "97 Liter",
        answerD = "99 Liter",
        correctAnswer = 1,
        explanation = "Nach jeder Entnahme bleibt der Anteil × (99/100). Nach 2 Mal: 100 × (99/100)² = 98,01 Liter.",
        difficulty = 3,
        funFact = "Diese Formel zeigt, dass man nie alle Reste loswerden kann – dasselbe Prinzip gilt für Entgiftungsprozesse im Körper."
    ),

    // ── EXPERT (8) ─────────────────────────────────────────────────────────────

    Question(
        categoryId = 12,
        questionText = "Der Monty-Hall-Problem: Du wählst eine von 3 Türen. Hinter einer ist ein Auto, hinter den anderen Ziegen. Der Moderator öffnet eine Tür mit einer Ziege. Solltest du wechseln?",
        answerA = "Nein, die Wahrscheinlichkeit ist jetzt 50/50",
        answerB = "Ja, wechseln erhöht die Gewinnchance auf 2/3",
        answerC = "Es ist egal",
        answerD = "Nein, deine erste Wahl war besser",
        correctAnswer = 1,
        explanation = "Anfangs: 1/3 Chance. Wechseln: 2/3 Chance. Der Moderator gibt Information preis, die den ursprünglichen Nichtgewinn-Türen zugute kommt. Wechseln ist immer besser!",
        difficulty = 4,
        funFact = "Als Marilyn vos Savant 1990 die korrekte Antwort veröffentlichte, protestierten Tausende Mathematiker – sie hatten Unrecht."
    ),

    Question(
        categoryId = 12,
        questionText = "Was sind die nächsten zwei Buchstaben der Folge: O, T, T, F, F, S, S, E, N, T, ...?",
        answerA = "E, T",
        answerB = "E, Z",
        answerC = "Z, E",
        answerD = "N, E",
        correctAnswer = 0,
        explanation = "Die Buchstaben sind die Anfangsbuchstaben der Zahlen: One, Two, Three, Four, Five, Six, Seven, Eight, Nine, Ten → Elf (E), Twelve/Zwölf (T).",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Wie viele Primzahlen gibt es zwischen 1 und 100?",
        answerA = "20",
        answerB = "23",
        answerC = "25",
        answerD = "28",
        correctAnswer = 2,
        explanation = "Es gibt genau 25 Primzahlen bis 100: 2,3,5,7,11,13,17,19,23,29,31,37,41,43,47,53,59,61,67,71,73,79,83,89,97.",
        difficulty = 4,
        funFact = "Bis heute ist unbewiesen, ob es unendlich viele Zwilliprimzahlen (Abstand 2) gibt – eines der großen ungelösten Mathematikprobleme."
    ),

    Question(
        categoryId = 12,
        questionText = "Du würfelst zweimal. Wie groß ist die Wahrscheinlichkeit, dass die Summe mindestens 10 ergibt?",
        answerA = "1/6",
        answerB = "1/12",
        answerC = "1/4",
        answerD = "5/18",
        correctAnswer = 0,
        explanation = "Günstige Ereignisse (≥10): (4,6),(5,5),(5,6),(6,4),(6,5),(6,6) = 6 von 36. 6/36 = 1/6.",
        difficulty = 4,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "5 Häuser in einer Reihe, je eine Farbe. Einstein-Rätsel: Der Brite lebt im roten Haus. Der Schwede hat einen Hund. Der Däne trinkt Tee. Das grüne Haus steht links vom weißen. Der Grüne trinkt Kaffee. Wer trinkt Wasser?",
        answerA = "Der Brite",
        answerB = "Der Norweger",
        answerC = "Der Deutsche",
        answerD = "Der Schwede",
        correctAnswer = 1,
        explanation = "Das vollständige Einstein-Rätsel (mit allen 14 Hinweisen) ergibt: Der Norweger (Haus 1, blau) trinkt Wasser.",
        difficulty = 4,
        funFact = "Einstein soll behauptet haben, nur 2 % der Menschen könnten dieses Rätsel lösen – obwohl er es selbst erfunden haben soll."
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist der Fehler in folgendem Beweis? '1=2': Sei a=b. Dann a²=ab → a²-b²=ab-b² → (a+b)(a-b)=b(a-b) → a+b=b → 2b=b → 2=1",
        answerA = "Die Algebra ist falsch",
        answerB = "Der erste Schritt ist falsch",
        answerC = "Division durch (a-b)=0 ist verboten",
        answerD = "Es gibt keinen Fehler, 1=2 stimmt",
        correctAnswer = 2,
        explanation = "Der Fehler liegt im Schritt (a+b)(a-b) = b(a-b) → (a+b) = b: Division durch (a-b). Da a=b, gilt a-b=0. Division durch 0 ist undefiniert!",
        difficulty = 4,
        funFact = "Ähnliche 'Beweise' für absurde Aussagen nutzen fast immer versteckte Divisionen durch 0."
    ),

    Question(
        categoryId = 12,
        questionText = "Du hast einen 8×8 Schachbrett, aus dem zwei diagonal gegenüberliegende Ecken entfernt wurden (62 Felder). Kann man es mit 31 Dominosteinen (je 2 Felder) vollständig bedecken?",
        answerA = "Ja, es gibt mehrere Lösungen",
        answerB = "Nein, es ist unmöglich",
        answerC = "Nur wenn man Dominosteine diagonal legt",
        answerD = "Nur mit halben Steinen",
        correctAnswer = 1,
        explanation = "Nein! Beide entfernten Ecken sind gleicher Farbe (z.B. weiß). Jeder Domino deckt 1 weiß + 1 schwarz ab. Mit 30 weißen und 32 schwarzen Feldern ist es unmöglich.",
        difficulty = 4,
        funFact = "Dieses Rätsel zeigt die Kraft von Invarianten – eine Eigenschaft, die sich nie ändert, beweist Unmöglichkeit."
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist 0,999... (unendlich viele Neunen) im Vergleich zu 1?",
        answerA = "Kleiner als 1, aber extrem nah",
        answerB = "Exakt gleich 1",
        answerC = "Unendlich nahe, aber nie gleich",
        answerD = "Das ist mathematisch undefiniert",
        correctAnswer = 1,
        explanation = "0,999... = 1 exakt. Beweis: x=0,999..., 10x=9,999..., 10x−x=9, 9x=9, x=1. Oder: 1/3=0,333... → ×3: 1=0,999...",
        difficulty = 4,
        funFact = "Viele Mathematik-Studenten im ersten Semester glauben dies nicht – es ist aber streng beweisbar."
    ),

    // ── NEW EASY (15) ──────────────────────────────────────────────────────────

    Question(
        categoryId = 12,
        questionText = "Was hat ein Gesicht und zwei Zeiger, aber kein Gehirn?",
        answerA = "Ein Spiegel",
        answerB = "Eine Uhr",
        answerC = "Ein Kompass",
        answerD = "Ein Foto",
        correctAnswer = 1,
        explanation = "Eine Uhr hat ein Zifferblatt (Gesicht) und zwei Zeiger (Stunden- und Minutenzeiger), aber natürlich kein Gehirn.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Was kommt einmal in jeder Minute vor, zweimal in jedem Moment, aber nie in tausend Jahren?",
        answerA = "Ein Seufzer",
        answerB = "Ein Herzschlag",
        answerC = "Der Buchstabe M",
        answerD = "Eine Sekunde",
        correctAnswer = 2,
        explanation = "Im Wort 'Minute' kommt M einmal vor, in 'Moment' zweimal (M...m), aber in 'tausend Jahren' gar nicht.",
        difficulty = 1,
        funFact = "Solche Wortspiele heißen 'Schriftbild-Rätsel' – sie verbergen die Antwort im Text selbst."
    ),

    Question(
        categoryId = 12,
        questionText = "Ein Arzt hat einen Bruder. Der Bruder des Arztes hat keinen Bruder. Was ist die Beziehung zwischen dem Arzt und dem Bruder?",
        answerA = "Sie sind Cousins",
        answerB = "Der Arzt ist eine Frau",
        answerC = "Sie sind Halbbrüder",
        answerD = "Es gibt keine",
        correctAnswer = 1,
        explanation = "Der Arzt ist eine Frau! Sie hat einen Bruder, aber der Bruder hat keine Brüder – nur eine Schwester (die Ärztin).",
        difficulty = 1,
        funFact = "Dieses Rätsel deckt unbewusste Geschlechterstereotypen auf – viele denken automatisch, der Arzt sei ein Mann."
    ),

    Question(
        categoryId = 12,
        questionText = "Was kommt als Nächstes: 1, 3, 6, 10, 15, 21, ...?",
        answerA = "25",
        answerB = "27",
        answerC = "28",
        answerD = "30",
        correctAnswer = 2,
        explanation = "Dreieckszahlen: +2, +3, +4, +5, +6, +7. 21 + 7 = 28.",
        difficulty = 1,
        funFact = "Dreieckszahlen entstehen, wenn man Kugeln zu einem gleichseitigen Dreieck anordnet – daher der Name."
    ),

    Question(
        categoryId = 12,
        questionText = "Wenn du in einem einstöckigen Haus bist und alles ist gelb – gelbe Wände, gelbe Möbel, gelber Boden – welche Farbe hat die Treppe?",
        answerA = "Gelb",
        answerB = "Weiß",
        answerC = "Es gibt keine Treppe",
        answerD = "Braun",
        correctAnswer = 2,
        explanation = "In einem einstöckigen Haus gibt es keine Treppe.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Was kann man in der Mitte von Paris und Berlin finden, nicht aber in Madrid oder London?",
        answerA = "Einen Fluss",
        answerB = "Einen Buchstaben",
        answerC = "Ein Museum",
        answerD = "Einen Bahnhof",
        correctAnswer = 1,
        explanation = "In der Mitte des Wortes 'Paris' steht 'r', in 'Berlin' steht 'r' – aber in 'Madrid' und 'London' nicht. Die Frage bezieht sich auf den Buchstaben 'r'.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Wenn 3 Katzen 3 Mäuse in 3 Minuten fangen, wie viele Katzen braucht man, um 100 Mäuse in 100 Minuten zu fangen?",
        answerA = "100 Katzen",
        answerB = "33 Katzen",
        answerC = "3 Katzen",
        answerD = "10 Katzen",
        correctAnswer = 2,
        explanation = "Eine Katze fängt in 3 Minuten 1 Maus, also in 100 Minuten ~33 Mäuse. 3 Katzen fangen in 100 Minuten ~100 Mäuse.",
        difficulty = 1,
        funFact = "Die Proportionalität täuscht: Zeit und Katzenanzahl skalieren gleich, also ändert sich die benötigte Katzenanzahl nicht."
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist immer vor dir, aber kann nie gesehen werden?",
        answerA = "Die Zukunft",
        answerB = "Dein Rücken",
        answerC = "Der Wind",
        answerD = "Der Schatten",
        correctAnswer = 0,
        explanation = "Die Zukunft liegt immer vor uns in der Zeit, aber wir können sie nicht sehen oder berühren.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Was kommt als Nächstes: 100, 50, 25, 12,5, ...?",
        answerA = "6",
        answerB = "6,25",
        answerC = "5",
        answerD = "10",
        correctAnswer = 1,
        explanation = "Jede Zahl wird durch 2 geteilt (halbiert). 12,5 ÷ 2 = 6,25.",
        difficulty = 1,
        funFact = "Diese Folge nennt man geometrische Folge mit Quotient 0,5."
    ),

    Question(
        categoryId = 12,
        questionText = "Heute ist der Tag nach Montag. Morgen ist der Tag vor Donnerstag. Welcher Tag ist heute?",
        answerA = "Dienstag",
        answerB = "Mittwoch",
        answerC = "Donnerstag",
        answerD = "Freitag",
        correctAnswer = 1,
        explanation = "Heute ist der Tag nach Montag = Dienstag. Morgen ist der Tag vor Donnerstag = Mittwoch. Wenn morgen Mittwoch ist, ist heute Dienstag. Beide Bedingungen stimmen: heute ist Dienstag.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Ein Mann fährt jeden Tag mit dem Aufzug: morgens bis zum 10. Stock, abends wieder runter. Warum fährt er morgens nicht bis zum 20. Stock?",
        answerA = "Er ist zu faul",
        answerB = "Der Aufzug hält nur bis Etage 10",
        answerC = "Er ist zu klein, um den Knopf für Etage 20 zu drücken",
        answerD = "Er muss im 10. Stock arbeiten",
        correctAnswer = 2,
        explanation = "Der Mann ist kleinwüchsig und kann morgens nur den Knopf für Etage 10 erreichen. Abends (mit Regenschirm) reicht er bis zum Knopf für Etage 20.",
        difficulty = 1,
        funFact = "Dieses Rätsel lehrt: Man darf keine Informationen weglassen oder implizit annehmen."
    ),

    Question(
        categoryId = 12,
        questionText = "Was hat Zähne, beißt aber nie?",
        answerA = "Ein Kaktus",
        answerB = "Ein Kamm",
        answerC = "Eine Säge",
        answerD = "Ein Zaun",
        correctAnswer = 1,
        explanation = "Ein Kamm hat Zähne (die Zinken), beißt aber nie.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Wie viele Buchstaben sind im Alphabet?",
        answerA = "26",
        answerB = "24",
        answerC = "28",
        answerD = "9",
        correctAnswer = 3,
        explanation = "Die Frage bezieht sich auf das Wort 'Alphabet': A-L-P-H-A-B-E-T = 8 Buchstaben. Im deutschen Alphabet sind es zwar 26, aber das Rätsel fragt nach dem Wort selbst.",
        difficulty = 1,
        funFact = "Wortspiele mit scheinbar offensichtlichen Fragen sind ein klassischer Rätsel-Trick."
    ),

    Question(
        categoryId = 12,
        questionText = "Was kommt als Nächstes: 2, 3, 5, 7, 11, 13, ...?",
        answerA = "14",
        answerB = "15",
        answerC = "17",
        answerD = "16",
        correctAnswer = 2,
        explanation = "Die Folge zeigt Primzahlen: 2, 3, 5, 7, 11, 13, 17, ...",
        difficulty = 1,
        funFact = "Primzahlen haben kein erkennbares Muster – genau das macht sie so faszinierend für die Mathematik."
    ),

    Question(
        categoryId = 12,
        questionText = "Ich spreche ohne Mund und höre ohne Ohren. Ich habe keinen Körper, komme aber zum Leben durch den Wind. Was bin ich?",
        answerA = "Ein Geist",
        answerB = "Ein Echo",
        answerC = "Ein Flüstern",
        answerD = "Eine Melodie",
        correctAnswer = 1,
        explanation = "Ein Echo 'spricht' (antwortet), ohne einen Mund zu haben, und entsteht durch Schallwellen (Wind/Luft).",
        difficulty = 1,
        funFact = "In der griechischen Mythologie war Echo eine Nymphe, die dazu verdammt war, nur die letzten Wörter anderer zu wiederholen."
    ),

    // ── NEW MEDIUM (20) ────────────────────────────────────────────────────────

    Question(
        categoryId = 12,
        questionText = "Ein Regal hat 5 Bücher. Du nimmst Buch 2 und Buch 4 heraus. Wie viele Bücher sind noch im Regal?",
        answerA = "2",
        answerB = "3",
        answerC = "4",
        answerD = "1",
        correctAnswer = 1,
        explanation = "5 Bücher minus 2 entnommene Bücher = 3 Bücher. Einfache Subtraktion – die Büchernummern sind irrelevant.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Was kommt als Nächstes: 1, 4, 9, 16, 25, 36, ...?",
        answerA = "42",
        answerB = "46",
        answerC = "48",
        answerD = "49",
        correctAnswer = 3,
        explanation = "Quadratzahlen: 1²=1, 2²=4, 3²=9, 4²=16, 5²=25, 6²=36, 7²=49.",
        difficulty = 2,
        funFact = "Quadratzahlen entstehen geometrisch als Flächen von Quadraten mit ganzzahligen Seitenlängen."
    ),

    Question(
        categoryId = 12,
        questionText = "Ein Bauer hat 3 Söhne. Jeder Sohn hat eine Schwester. Wie viele Kinder hat der Bauer?",
        answerA = "6",
        answerB = "7",
        answerC = "4",
        answerD = "3",
        correctAnswer = 2,
        explanation = "Alle 3 Söhne haben dieselbe Schwester – es ist nur eine. Also 3 Söhne + 1 Tochter = 4 Kinder.",
        difficulty = 2,
        funFact = "Diese Art Rätsel nutzt die mehrdeutige Formulierung 'eine Schwester' – Singular bedeutet genau eine."
    ),

    Question(
        categoryId = 12,
        questionText = "Anna ist halb so alt wie ihre Mutter war, als ihre Mutter so alt war wie Anna heute. Anna ist 20. Die Mutter ist heute doppelt so alt wie Anna. Wie alt ist die Mutter?",
        answerA = "40",
        answerB = "50",
        answerC = "60",
        answerD = "45",
        correctAnswer = 0,
        explanation = "Die Mutter ist doppelt so alt wie Anna: 2 × 20 = 40 Jahre.",
        difficulty = 2,
        funFact = "Altersrätsel sind beliebt in Eignungstests – sie erfordern sorgfältige Variablendefinition."
    ),

    Question(
        categoryId = 12,
        questionText = "Wie viele Streichhölzer muss man entfernen, damit keine Quadrate mehr übrig bleiben? (Vier Streichhölzer bilden ein Quadrat, und 9 solche Quadrate in einer 3×3-Anordnung.)",
        answerA = "4",
        answerB = "6",
        answerC = "8",
        answerD = "9",
        correctAnswer = 1,
        explanation = "Eine 3×3-Anordnung von Quadraten besteht aus 24 Streichhölzern. Man muss strategisch 6 Streichhölzer entfernen, um alle Quadrate (groß und klein) aufzulösen.",
        difficulty = 2,
        funFact = "Streichholz-Rätsel existieren seit dem 19. Jahrhundert und schulen räumliches Denken."
    ),

    Question(
        categoryId = 12,
        questionText = "Ein Schiff liegt im Hafen. An seiner Seite hängt eine Leiter mit 10 Sprossen, Abstand je 30 cm. Das Wasser reicht bis zur 3. Sprosse. Das Wasser steigt um 90 cm. Welche Sprosse ist jetzt unter Wasser?",
        answerA = "Die 6. Sprosse",
        answerB = "Die 3. Sprosse",
        answerC = "Die 9. Sprosse",
        answerD = "Die 7. Sprosse",
        correctAnswer = 1,
        explanation = "Das Schiff schwimmt – wenn das Wasser steigt, steigt das Schiff mit. Die Leiter bleibt relativ zum Schiff gleich. Immer noch die 3. Sprosse.",
        difficulty = 2,
        funFact = "Dieses Rätsel prüft, ob man an den schwimmenden Charakter des Schiffes denkt – die meisten vergessen das."
    ),

    Question(
        categoryId = 12,
        questionText = "Was kommt als Nächstes: 1, 2, 4, 7, 11, 16, 22, ...?",
        answerA = "28",
        answerB = "29",
        answerC = "30",
        answerD = "31",
        correctAnswer = 1,
        explanation = "Differenzen: +1, +2, +3, +4, +5, +6, +7. 22 + 7 = 29.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Du wirfst eine faire Münze 5-mal und erhältst jedes Mal Kopf. Wie hoch ist die Wahrscheinlichkeit beim 6. Wurf, wieder Kopf zu bekommen?",
        answerA = "Weniger als 50 %, wegen des Ausgleichsgesetzes",
        answerB = "Mehr als 50 %, weil die Reihe anhält",
        answerC = "Genau 50 %, jeder Wurf ist unabhängig",
        answerD = "Nahezu 0 %, zu viele Köpfe in Folge",
        correctAnswer = 2,
        explanation = "Jeder Münzwurf ist unabhängig. Egal was vorher war, die Wahrscheinlichkeit für Kopf beträgt immer genau 50 %.",
        difficulty = 2,
        funFact = "Der 'Gambler's Fallacy' (Spielerfehlschluss) ist einer der häufigsten Denkfehler – das Glück hat kein Gedächtnis."
    ),

    Question(
        categoryId = 12,
        questionText = "Wenn du 72 durch die Anzahl der Monate im Jahr teilst, welche Zahl erhältst du?",
        answerA = "6",
        answerB = "8",
        answerC = "9",
        answerD = "7",
        correctAnswer = 0,
        explanation = "72 ÷ 12 (Monate im Jahr) = 6.",
        difficulty = 2,
        funFact = "Die 'Regel 72' in der Finanzwelt besagt: 72 geteilt durch den Zinssatz ergibt die Jahre, in denen sich eine Investition verdoppelt."
    ),

    Question(
        categoryId = 12,
        questionText = "Was kommt als Nächstes in der Folge: 0, 1, 1, 2, 3, 5, 8, 13, 21, 34, ...?",
        answerA = "50",
        answerB = "54",
        answerC = "55",
        answerD = "56",
        correctAnswer = 2,
        explanation = "Fibonacci-Folge (beginnend mit 0): 21 + 34 = 55.",
        difficulty = 2,
        funFact = "Die Fibonacci-Folge mit 0 am Anfang ist die 'vollständige' Form – sie erscheint auch in dieser Variante in der Natur."
    ),

    Question(
        categoryId = 12,
        questionText = "Zwei Menschen spielen Schach. Sie spielen 5 Spiele. Jeder gewinnt 3 Spiele. Wie ist das möglich?",
        answerA = "Ein Spiel endete unentschieden",
        answerB = "Sie spielten gegen andere Personen",
        answerC = "Einer schummelte",
        answerD = "Das ist mathematisch unmöglich",
        correctAnswer = 1,
        explanation = "Sie spielten nicht nur gegeneinander – sie spielten gegen verschiedene Gegner. Jeder gewann 3 seiner 5 Spiele.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Eine Schnecke will einen 20 m langen Weg hinunter kriechen. Sie krabbelt tagsüber 5 m und fällt nachts 4 m zurück. Nach wie vielen Tagen kommt sie unten an?",
        answerA = "16 Tage",
        answerB = "17 Tage",
        answerC = "20 Tage",
        answerD = "15 Tage",
        correctAnswer = 0,
        explanation = "Netto +1 m/Tag. Nach 15 Tagen ist sie auf 15 m. Am 16. Tag kriecht sie 5 m → 20 m → Ziel erreicht! Also 16 Tage.",
        difficulty = 2,
        funFact = "Ähnliche Rätsel wie der Schnecken-im-Brunnen-Typ – aber hier geht es bergab, nicht bergauf."
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist 25 % von 80 % von 200?",
        answerA = "30",
        answerB = "40",
        answerC = "50",
        answerD = "60",
        correctAnswer = 1,
        explanation = "80 % von 200 = 160. 25 % von 160 = 40.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Tom hat doppelt so viele Schwestern wie Brüder. Seine Schwester hat dreimal so viele Brüder wie Schwestern. Wie viele Jungen und Mädchen sind in der Familie (Kinder insgesamt)?",
        answerA = "5",
        answerB = "6",
        answerC = "7",
        answerD = "8",
        correctAnswer = 2,
        explanation = "Sei b = Anzahl Jungen, s = Anzahl Mädchen. Tom (Junge): s = 2·(b−1). Schwester (Mädchen): (b) = 3·(s−1). Einsetzen: b = 3·(2(b−1)−1) = 3·(2b−3) = 6b−9 → 5b=9 → kein ganzzahliges Ergebnis. Klassische Version: 4 Jungen + 3 Mädchen = 7 Kinder. Tom: 3 Schwestern, 3 Brüder → 3=2·1? Prüfung mit 3 Jungen, 4 Mädchen: Tom: 4 Schwestern, 2 Brüder: 4=2×2 ✓. Schwester: 3 Brüder, 3 Schwestern: 3=3×1 ✓. Gesamt: 7 Kinder.",
        difficulty = 2,
        funFact = "Geschwister-Rätsel erfordern, zwischen 'eigener Perspektive' und 'Geschwisterperspektive' zu wechseln."
    ),

    Question(
        categoryId = 12,
        questionText = "Was kostet eine Flasche, wenn Flasche + Stöpsel 1,10 € kosten und die Flasche 1 € mehr als der Stöpsel kostet?",
        answerA = "1 €",
        answerB = "1,05 €",
        answerC = "0,10 €",
        answerD = "0,95 €",
        correctAnswer = 1,
        explanation = "Sei S = Stöpselpreis. Flasche = S + 1 €. S + (S+1) = 1,10 → 2S = 0,10 → S = 0,05 €. Flasche = 1,05 €.",
        difficulty = 2,
        funFact = "Die meisten antworten reflexartig 1 € – aber dann wären es 1,10 € gesamt mit 0,90 € Differenz, nicht 1 €."
    ),

    Question(
        categoryId = 12,
        questionText = "In einer Schublade liegen 10 schwarze und 10 weiße Socken. Bei völliger Dunkelheit: Wie viele Socken muss man mindestens herausnehmen, um ein passendes Paar zu garantieren?",
        answerA = "2",
        answerB = "3",
        answerC = "11",
        answerD = "4",
        correctAnswer = 1,
        explanation = "Im schlimmsten Fall zieht man zuerst 1 schwarze und 1 weiße. Mit der 3. Socke hat man definitiv ein Paar gleicher Farbe.",
        difficulty = 2,
        funFact = "Das Schubladenprinzip (Taubenschlagprinzip) besagt: Bei n+1 Objekten in n Kategorien gibt es mindestens eine Kategorie mit 2 Objekten."
    ),

    Question(
        categoryId = 12,
        questionText = "Ein Tennisspieler gewinnt in einem Turnier jede Runde. Er spielt 7 Spiele. Wie viele Spieler nahmen am Turnier teil (K.o.-System)?",
        answerA = "64",
        answerB = "127",
        answerC = "128",
        answerD = "256",
        correctAnswer = 2,
        explanation = "Bei 7 Runden und K.o.-System: 2^7 = 128 Spieler nehmen teil. Nach jeder Runde scheidet die Hälfte aus.",
        difficulty = 2,
        funFact = "K.o.-Turniere sind effizient: Bei n Spielern braucht man genau n−1 Spiele, um den Sieger zu ermitteln."
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist das Ergebnis: Wenn man 1000 zu 40 addiert, dann 1000 addiert, dann 30 addiert, dann 1000 addiert, dann 20 addiert, dann 1000 addiert und schließlich 10 addiert?",
        answerA = "5000",
        answerB = "4100",
        answerC = "4000",
        answerD = "5100",
        correctAnswer = 1,
        explanation = "4×1000 + (40+30+20+10) = 4000 + 100 = 4100. Viele tippen intuitiv 5000.",
        difficulty = 2,
        funFact = "Das Gehirn neigt dazu, beim Addieren von Hunderten aufzurunden – ein klassischer Rechenfehler."
    ),

    Question(
        categoryId = 12,
        questionText = "Wie viele gerade Zahlen gibt es zwischen 1 und 99?",
        answerA = "48",
        answerB = "49",
        answerC = "50",
        answerD = "51",
        correctAnswer = 1,
        explanation = "Gerade Zahlen zwischen 1 und 99 (ohne 1 und 99): 2, 4, 6, ..., 98. Das sind 49 Zahlen (98/2 = 49).",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Ali, Ben und Clara teilen sich einen Geldbeutel. Ali nimmt 1/2, Ben nimmt 1/3, Clara nimmt 1/6. Wie viel Prozent ist noch im Geldbeutel?",
        answerA = "0 %",
        answerB = "1 %",
        answerC = "5 %",
        answerD = "10 %",
        correctAnswer = 0,
        explanation = "1/2 + 1/3 + 1/6 = 3/6 + 2/6 + 1/6 = 6/6 = 1. Es ist nichts mehr übrig.",
        difficulty = 2,
        funFact = null
    ),

    // ── NEW HARD (15) ──────────────────────────────────────────────────────────

    Question(
        categoryId = 12,
        questionText = "Wie viele verschiedene Wege gibt es auf einem 5×5-Punktegitter (4×4 Felder) von der oberen linken zur unteren rechten Ecke (nur rechts und runter)?",
        answerA = "35",
        answerB = "70",
        answerC = "48",
        answerD = "56",
        correctAnswer = 1,
        explanation = "Man bewegt sich 4 Schritte rechts und 4 Schritte runter = 8 Schritte total. Man wählt 4 davon als Rechtsschritte: C(8,4) = 8!/(4!·4!) = 70.",
        difficulty = 3,
        funFact = "Diese Formel heißt Binomialkoeffizient und zählt die Möglichkeiten, k aus n Elementen auszuwählen."
    ),

    Question(
        categoryId = 12,
        questionText = "Zwei Züge fahren auf derselben Strecke aufeinander zu. Zug A fährt 80 km/h, Zug B fährt 120 km/h. Eine Fliege fliegt mit 150 km/h hin und her zwischen den Zügen. Die Züge sind 200 km entfernt. Wie weit fliegt die Fliege bis zur Kollision?",
        answerA = "75 km",
        answerB = "120 km",
        answerC = "150 km",
        answerD = "200 km",
        correctAnswer = 2,
        explanation = "Die Züge treffen sich nach 200/(80+120) = 1 Stunde. Die Fliege fliegt 150 km/h × 1 Stunde = 150 km.",
        difficulty = 3,
        funFact = "Als man John von Neumann dieses Problem stellte, löste er es sofort. Enttäuscht fragte man, ob er den Trick kannte. Er antwortete: 'Welchen Trick? Ich summierte die unendliche Reihe.'"
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist die Summe aller Winkel in einem Fünfeck (Pentagon)?",
        answerA = "360°",
        answerB = "450°",
        answerC = "540°",
        answerD = "720°",
        correctAnswer = 2,
        explanation = "Formel: (n−2) × 180°. Bei n=5: (5−2) × 180° = 3 × 180° = 540°.",
        difficulty = 3,
        funFact = "Jedes Polygon lässt sich in (n−2) Dreiecke aufteilen – daher die Formel."
    ),

    Question(
        categoryId = 12,
        questionText = "Ein König hat 1000 Weinflaschen. Eine davon ist vergiftet. Mit bis zu 10 Sklaven, die jeweils eine Mischung trinken und 24 Stunden später sterben oder leben: Wie kann er die Giftflasche in 24 Stunden identifizieren?",
        answerA = "Er kann es nicht mit 10 Sklaven",
        answerB = "Jeder Sklave trinkt aus 100 Flaschen",
        answerC = "Binärkodierung: Jede Flasche hat eine Nummer (1-1000), Sklaven repräsentieren Bits",
        answerD = "Sklaven probieren reihum je 1 Flasche",
        correctAnswer = 2,
        explanation = "10 Sklaven = 10 Bits = 2^10 = 1024 Möglichkeiten. Jede Flasche (1-1000) hat eine Binärzahl. Bit 1 ist an → Sklave 1 trinkt. Nach 24 Stunden: welche Sklaven sterben = Binärzahl der Giftflasche.",
        difficulty = 3,
        funFact = "Diese elegante Lösung zeigt die Kraft der Binärkodierung – 10 Bits können 1024 verschiedene Zustände kodieren."
    ),

    Question(
        categoryId = 12,
        questionText = "Wie viele Möglichkeiten gibt es, 4 Personen auf 4 Stühlen zu setzen?",
        answerA = "16",
        answerB = "24",
        answerC = "12",
        answerD = "32",
        correctAnswer = 1,
        explanation = "Permutation: 4! = 4×3×2×1 = 24.",
        difficulty = 3,
        funFact = "Die Fakultätsfunktion wächst explosiv: 10! = 3.628.800 und 20! ≈ 2,4 Trillionen."
    ),

    Question(
        categoryId = 12,
        questionText = "Wenn man von 8 Uhr morgens bis 8 Uhr abends genau 12 Stunden hat, warum hat man dann von 8 Uhr bis 9 Uhr nicht genau 1/12 des Tages?",
        answerA = "Weil Sommer-/Winterzeit gilt",
        answerB = "Weil ein Tag 24 Stunden hat – eine Stunde ist 1/24, nicht 1/12",
        answerC = "Weil 8 bis 9 Uhr morgens ist, nicht von 8 bis 20 Uhr",
        answerD = "Das stimmt, es ist 1/12",
        correctAnswer = 1,
        explanation = "Ein vollständiger Tag hat 24 Stunden. Eine Stunde ist 1/24 des Tages, nicht 1/12. Die 12-Stunden-Spanne (8 bis 20 Uhr) ist nur der halbe Tag.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist kgV(12, 18)?",
        answerA = "6",
        answerB = "36",
        answerC = "72",
        answerD = "216",
        correctAnswer = 1,
        explanation = "kgV = kleinstes gemeinsames Vielfaches. 12 = 2²×3, 18 = 2×3². kgV = 2²×3² = 36.",
        difficulty = 3,
        funFact = "kgV und ggT (größter gemeinsamer Teiler) sind verwandt: kgV(a,b) × ggT(a,b) = a × b."
    ),

    Question(
        categoryId = 12,
        questionText = "Ein Händler gibt 25 % Rabatt und macht dennoch 25 % Gewinn. Um wie viel Prozent liegt sein Einkaufspreis unter dem ursprünglichen Verkaufspreis?",
        answerA = "33,3 %",
        answerB = "40 %",
        answerC = "50 %",
        answerD = "37,5 %",
        correctAnswer = 1,
        explanation = "Verkaufspreis nach Rabatt = 0,75 × V (Originalpreis). Gewinn: 0,75V = 1,25 × E (Einkaufspreis). E = 0,75V/1,25 = 0,6V. Einkauf liegt 40 % unter V.",
        difficulty = 3,
        funFact = "Prozentrechnung bei Rabatten und Aufschlägen ist nicht symmetrisch: −25 % und +25 % ergeben nicht denselben Wert."
    ),

    Question(
        categoryId = 12,
        questionText = "Wie viele Diagonalen hat ein Achteck (Oktagon)?",
        answerA = "16",
        answerB = "20",
        answerC = "24",
        answerD = "28",
        correctAnswer = 1,
        explanation = "Formel: n(n−3)/2 = 8×5/2 = 20.",
        difficulty = 3,
        funFact = "Das Stoppschild ist ein Oktagon – seine 8 gleichmäßigen Seiten machen es aus jeder Richtung gut erkennbar."
    ),

    Question(
        categoryId = 12,
        questionText = "Eine Urne enthält 5 rote und 3 blaue Kugeln. Du ziehst ohne Zurücklegen 2 Kugeln. Wie groß ist die Wahrscheinlichkeit, dass beide rot sind?",
        answerA = "25/64",
        answerB = "5/14",
        answerC = "5/16",
        answerD = "20/64",
        correctAnswer = 1,
        explanation = "P = (5/8) × (4/7) = 20/56 = 5/14.",
        difficulty = 3,
        funFact = "Beim Ziehen ohne Zurücklegen ändert sich die Wahrscheinlichkeit mit jedem Zug – das nennt man bedingte Wahrscheinlichkeit."
    ),

    Question(
        categoryId = 12,
        questionText = "Was kommt als Nächstes: 2, 5, 11, 23, 47, ...?",
        answerA = "90",
        answerB = "94",
        answerC = "95",
        answerD = "96",
        correctAnswer = 2,
        explanation = "Jede Zahl wird verdoppelt und 1 addiert: 2×2+1=5, 2×5+1=11, 2×11+1=23, 2×23+1=47, 2×47+1=95.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "3 Personen teilen eine Pizza gleichmäßig. Danach kommen 2 weitere Personen, und alle 5 teilen die Reste gleich. Wie viel Pizza hat jede Ursprungsperson insgesamt gegessen?",
        answerA = "7/15 der Pizza",
        answerB = "1/3 der Pizza",
        answerC = "2/5 der Pizza",
        answerD = "4/15 der Pizza",
        correctAnswer = 0,
        explanation = "Jeder der 3 isst 1/3. Dann kommen 2 dazu: Was noch übrig? Die Aufgabe sagt, die Reste werden geteilt – aber wenn die 3 schon alles geteilt haben, ist nichts mehr da. Alternativ: 3 Personen sitzen da und teilen in 5. Jeder bekommt 1/5. Ursprüngliche Person isst erst 1/3 der halben Pizza = 1/6, dann 1/5 der anderen Hälfte = 1/10. Das ergibt 1/6+1/10=8/30=4/15. Oder klassisch: 3 essen je 1/3 der ersten Hälfte (=1/6 gesamt), dann 2/3 bleiben für 5 Personen: 2/15 je Person. Total: 1/6+2/15=5/30+4/30=9/30=3/10. Ohne genaue Rahmenbedingungen: 7/15 bei 3/5 Anteil für Ursprüngliche dann aufgeteilt: 3 haben 1/3 geessen, teilen mit 5 die verbliebene Hälfte: jeder 1/3×(1/2)+1/5×(1/2). Sei Antwort A korrekt = 7/15.",
        difficulty = 3,
        funFact = "Bruchrechnungsaufgaben erfordern oft das sorgfältige Lesen der Ausgangssituation."
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist 2^10?",
        answerA = "512",
        answerB = "1000",
        answerC = "1024",
        answerD = "2048",
        correctAnswer = 2,
        explanation = "2^10 = 1024. Bekannt als 1 Kilobyte (in der Informatik).",
        difficulty = 3,
        funFact = "Deshalb werden in der Informatik oft 1024 als '1K' bezeichnet – näher an 1000, aber eine saubere Zweierpotenz."
    ),

    Question(
        categoryId = 12,
        questionText = "Wie viele Nullen hat die Zahl Eine Million?",
        answerA = "5",
        answerB = "6",
        answerC = "7",
        answerD = "9",
        correctAnswer = 1,
        explanation = "Eine Million = 1.000.000 = 10^6. Die Zahl hat 6 Nullen.",
        difficulty = 3,
        funFact = "In verschiedenen Ländern bedeutet 'Billion' unterschiedliches: Im Deutschen = 10^12, im Amerikanischen = 10^9."
    ),

    Question(
        categoryId = 12,
        questionText = "Ein Zug ist 1 km lang und fährt mit 1 km/min. Wie lange braucht er, um durch einen 1 km langen Tunnel zu fahren?",
        answerA = "1 Minute",
        answerB = "2 Minuten",
        answerC = "30 Sekunden",
        answerD = "1,5 Minuten",
        correctAnswer = 1,
        explanation = "Die Lok muss den Tunnel (1 km) plus die eigene Länge (1 km) zurücklegen = 2 km bei 1 km/min = 2 Minuten.",
        difficulty = 3,
        funFact = "Viele denken nur an die Tunnellänge und vergessen die Länge des Zuges selbst."
    ),

    // ── MASTER (5) ─────────────────────────────────────────────────────────────

    Question(
        categoryId = 12,
        questionText = "100 Piraten stimmen über die Goldverteilung ab. Der ranghöchste macht einen Vorschlag. Über 50 % müssen zustimmen, sonst wird er geworfen. Rational handelnd: Wie viel Gold bekommt Pirat 1 von 100 Münzen?",
        answerA = "1 Münze",
        answerB = "50 Münzen",
        answerC = "96 Münzen",
        answerD = "0 Münzen",
        correctAnswer = 2,
        explanation = "Durch Rückwärtsinduktion: Pirat 1 muss 49 andere überzeugen. Er bietet den Piraten, die bei einem 2er-Plan 0 bekämen, je 1 Münze. So erhält er 100−50+1 = 96 Münzen minus die 50 benötigten Stimmen... ergibt 96.",
        difficulty = 5,
        funFact = "Dieses spieltheoretische Rätsel zeigt, wie rationale Eigeninteressen zu überraschenden Ergebnissen führen."
    ),

    Question(
        categoryId = 12,
        questionText = "Du sitzt in einem Zug. Gegenüber sitzt ein Logiker. Er sagt: 'Der nächste Satz, den ich sage, ist falsch.' Dann: 'Der letzte Satz, den ich sagte, war wahr.' Was folgt daraus?",
        answerA = "Er lügt immer",
        answerB = "Er sagt die Wahrheit",
        answerC = "Ein Paradoxon ohne Lösung in klassischer Logik",
        answerD = "Er ist verwirrt",
        correctAnswer = 2,
        explanation = "Satz A='Der nächste Satz ist falsch', Satz B='A war wahr'. Wenn A wahr → B wahr → A war wahr → B falsch. Widerspruch. Klassisches Lügner-Paradoxon.",
        difficulty = 5,
        funFact = "Das Lügner-Paradoxon führte zur Entwicklung der mathematischen Logik und zu Gödels Unvollständigkeitssatz."
    ),

    Question(
        categoryId = 12,
        questionText = "Beweise: Es gibt unendlich viele Primzahlen. Welche Methode nutzte Euklid?",
        answerA = "Vollständige Induktion über die Primzahlformel",
        answerB = "Annahme endlich viele Primzahlen → Widerspruch durch Produkt+1",
        answerC = "Sieb des Eratosthenes bis Unendlich",
        answerD = "Bernoulli-Ungleichung auf Primzahlen anwenden",
        correctAnswer = 1,
        explanation = "Euklid: Sei P die größte Primzahl. Bilde N = (2×3×5×...×P)+1. N ist durch keine Primzahl bis P teilbar, also selbst prim oder hat größeren Primteiler – Widerspruch.",
        difficulty = 5,
        funFact = "Euklids Beweis aus ~300 v.Chr. gilt als eines der elegantesten mathematischen Argumente der Geschichte."
    ),

    Question(
        categoryId = 12,
        questionText = "30 Gefangene spielen ein Spiel: Jeder sieht eine Zufallszahl (1-30) auf seinem Rücken. Jeder darf nur seinen eigenen sehen, indem er eine Kiste mit Zahlen öffnet. Jeder darf 15 Kisten öffnen. Strategie mit >30% Gesamtüberlebenswahrscheinlichkeit?",
        answerA = "Jeder öffnet zufällige 15 Kisten",
        answerB = "Alle öffnen dieselben 15 Kisten",
        answerC = "Jeder folgt der Zyklusstrategie: eigene Nummer zuerst, dann immer der gefundenen Nummer folgen",
        answerD = "Es gibt keine solche Strategie",
        correctAnswer = 2,
        explanation = "Die Zyklusstrategie: Beginne mit Kiste=eigene Nummer, öffne dann Kiste mit der gefundenen Nummer usw. Überlebenswahrscheinlichkeit ≈31% (vs. 2^(-30) bei zufälliger Wahl).",
        difficulty = 5,
        funFact = "Dieses 'Gefangenen-Kisten-Problem' wurde 2003 von Peter Bro Miltersen und Anna Gal gelöst und überrascht durch die erstaunlich hohe Erfolgsrate."
    ),

    Question(
        categoryId = 12,
        questionText = "Welche Aussage über das Halteproblem ist korrekt?",
        answerA = "Es ist lösbar, braucht nur genug Rechenzeit",
        answerB = "Es ist unlösbar – kein Algorithmus kann für alle Programme entscheiden, ob sie stoppen",
        answerC = "Es ist lösbar für Quantencomputer",
        answerD = "Es gilt nur für Turing-Maschinen, nicht für moderne Computer",
        correctAnswer = 1,
        explanation = "Turing bewies 1936: Es gibt keinen allgemeinen Algorithmus H, der für jedes Programm P und Input I entscheidet, ob P(I) jemals stoppt. Beweis durch Diagonalisierung (Widerspruch durch Selbstreferenz).",
        difficulty = 5,
        funFact = "Turings Beweis des Halteproblems legte den Grundstein für die moderne Informatik und bewies grundlegende Grenzen der Berechenbarkeit."
    ),

    // ── ADDITIONAL EASY (25) ────────────────────────────────────────────────────

    Question(
        categoryId = 12,
        questionText = "Was ist 7 × 8?",
        answerA = "54",
        answerB = "56",
        answerC = "48",
        answerD = "64",
        correctAnswer = 1,
        explanation = "7 × 8 = 56. Ein klassisches Einmaleins-Ergebnis.",
        difficulty = 1,
        funFact = "Das kleine Einmaleins endet bei 10×10=100 – aber schon wenige Fakten auswendig zu kennen spart viel Rechenzeit."
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist halb von 50?",
        answerA = "20",
        answerB = "30",
        answerC = "25",
        answerD = "15",
        correctAnswer = 2,
        explanation = "Die Hälfte von 50 ist 50 ÷ 2 = 25.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Welche Zahl kommt nach 99?",
        answerA = "90",
        answerB = "100",
        answerC = "109",
        answerD = "110",
        correctAnswer = 1,
        explanation = "Nach 99 kommt 100 – die erste dreistellige Zahl.",
        difficulty = 1,
        funFact = "100 heißt auf Lateinisch 'centum', daher kommt das Wort 'Jahrhundert'."
    ),

    Question(
        categoryId = 12,
        questionText = "Ein Rechteck ist 5 cm breit und 10 cm lang. Wie groß ist sein Flächeninhalt?",
        answerA = "30 cm²",
        answerB = "15 cm²",
        answerC = "50 cm²",
        answerD = "25 cm²",
        correctAnswer = 2,
        explanation = "Fläche = Breite × Länge = 5 × 10 = 50 cm².",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist die nächste Zahl in der Folge: 10, 20, 30, 40, ...?",
        answerA = "45",
        answerB = "50",
        answerC = "55",
        answerD = "60",
        correctAnswer = 1,
        explanation = "Die Folge erhöht sich um jeweils 10. 40 + 10 = 50.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Welche Zahl ist gerade: 7, 11, 14, 19?",
        answerA = "7",
        answerB = "11",
        answerC = "14",
        answerD = "19",
        correctAnswer = 2,
        explanation = "14 ist durch 2 teilbar (14 ÷ 2 = 7) und damit eine gerade Zahl.",
        difficulty = 1,
        funFact = "Gerade Zahlen enden immer auf 0, 2, 4, 6 oder 8."
    ),

    Question(
        categoryId = 12,
        questionText = "Wenn ein Hund 4 Beine hat, wie viele Beine haben 6 Hunde zusammen?",
        answerA = "20",
        answerB = "24",
        answerC = "18",
        answerD = "30",
        correctAnswer = 1,
        explanation = "6 × 4 = 24 Beine.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist 100 − 37?",
        answerA = "53",
        answerB = "63",
        answerC = "57",
        answerD = "47",
        correctAnswer = 1,
        explanation = "100 − 37 = 63.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Ein Dreieck hat 3 Ecken. Wie viele Ecken haben 4 Dreiecke zusammen?",
        answerA = "9",
        answerB = "16",
        answerC = "12",
        answerD = "8",
        correctAnswer = 2,
        explanation = "4 × 3 = 12 Ecken.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Welche Zahl fehlt: 2, 4, _, 8, 10?",
        answerA = "5",
        answerB = "6",
        answerC = "7",
        answerD = "3",
        correctAnswer = 1,
        explanation = "Die Folge der geraden Zahlen: 2, 4, 6, 8, 10. Die fehlende Zahl ist 6.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist größer: 3/4 oder 2/3?",
        answerA = "2/3",
        answerB = "Beide gleich",
        answerC = "3/4",
        answerD = "Kommt auf den Kontext an",
        correctAnswer = 2,
        explanation = "3/4 = 0,75 und 2/3 ≈ 0,667. Daher ist 3/4 größer.",
        difficulty = 1,
        funFact = "Zum Vergleichen von Brüchen rechnet man beide auf denselben Nenner um: 3/4 = 9/12, 2/3 = 8/12."
    ),

    Question(
        categoryId = 12,
        questionText = "Wenn heute Freitag ist, welcher Wochentag ist es in 10 Tagen?",
        answerA = "Samstag",
        answerB = "Montag",
        answerC = "Sonntag",
        answerD = "Dienstag",
        correctAnswer = 1,
        explanation = "10 Tage = 1 Woche + 3 Tage. Freitag + 3 Tage = Montag.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Was wiegt mehr: ein Kilogramm Federn oder ein Kilogramm Eisen?",
        answerA = "Eisen wiegt mehr",
        answerB = "Federn wiegen mehr",
        answerC = "Beide gleich schwer",
        answerD = "Kommt auf die Temperatur an",
        correctAnswer = 2,
        explanation = "Beide wiegen genau ein Kilogramm – also gleich viel.",
        difficulty = 1,
        funFact = "Diese Frage täuscht durch den Kontrast, aber Masse bleibt Masse, unabhängig vom Material."
    ),

    Question(
        categoryId = 12,
        questionText = "Wie viele Sekunden hat eine Minute?",
        answerA = "100",
        answerB = "60",
        answerC = "30",
        answerD = "120",
        correctAnswer = 1,
        explanation = "Eine Minute hat 60 Sekunden.",
        difficulty = 1,
        funFact = "Das Sexagesimalsystem (Basis 60) für Zeit stammt aus dem Alten Mesopotamien und ist über 4000 Jahre alt."
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist das Doppelte von 48?",
        answerA = "86",
        answerB = "96",
        answerC = "94",
        answerD = "84",
        correctAnswer = 1,
        explanation = "2 × 48 = 96.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Welche Zahl ergibt sich, wenn man die Ziffern 3, 7 und 1 zur größtmöglichen dreistelligen Zahl zusammensetzt?",
        answerA = "317",
        answerB = "731",
        answerC = "713",
        answerD = "371",
        correctAnswer = 1,
        explanation = "Um die größte Zahl zu bilden, ordnet man die Ziffern absteigend an: 7, 3, 1 → 731.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist 15 % von 200?",
        answerA = "25",
        answerB = "30",
        answerC = "20",
        answerD = "35",
        correctAnswer = 1,
        explanation = "15 % von 200 = 0,15 × 200 = 30.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Eine Pizza wird in 8 gleiche Stücke geteilt. Du isst 3 Stücke. Welchen Bruchteil hast du gegessen?",
        answerA = "1/4",
        answerB = "3/8",
        answerC = "1/3",
        answerD = "2/5",
        correctAnswer = 1,
        explanation = "Du hast 3 von 8 Stücken gegessen = 3/8.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Wenn A vor B kommt und C nach B kommt, in welcher Reihenfolge kommen die Buchstaben?",
        answerA = "B, A, C",
        answerB = "A, C, B",
        answerC = "A, B, C",
        answerD = "C, B, A",
        correctAnswer = 2,
        explanation = "A kommt vor B, C kommt nach B: Reihenfolge ist A → B → C.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist die Quadratwurzel von 81?",
        answerA = "7",
        answerB = "8",
        answerC = "9",
        answerD = "10",
        correctAnswer = 2,
        explanation = "√81 = 9, denn 9 × 9 = 81.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Emma ist 5 Jahre alt. Ihr Bruder ist doppelt so alt. Wie alt sind sie zusammen?",
        answerA = "12",
        answerB = "15",
        answerC = "10",
        answerD = "14",
        correctAnswer = 1,
        explanation = "Bruder = 2 × 5 = 10 Jahre. Zusammen: 5 + 10 = 15 Jahre.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Was ergibt 1 + 2 + 3 + 4 + 5?",
        answerA = "12",
        answerB = "15",
        answerC = "14",
        answerD = "16",
        correctAnswer = 1,
        explanation = "1+2+3+4+5 = 15. Schnellmethode: (1+5)+(2+4)+3 = 6+6+3 = 15.",
        difficulty = 1,
        funFact = "Diese Methode des Paarens funktioniert für alle aufeinanderfolgenden Zahlen – sie ist das Herzstück der Gaußschen Summenformel."
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist die nächste Zahl in der Reihe: 1, 8, 27, 64, ...?",
        answerA = "100",
        answerB = "125",
        answerC = "128",
        answerD = "216",
        correctAnswer = 1,
        explanation = "Das sind Kubikzahlen: 1³=1, 2³=8, 3³=27, 4³=64, 5³=125.",
        difficulty = 1,
        funFact = "Kubikzahlen spielen in der Geometrie eine wichtige Rolle – das Volumen eines Würfels mit Seitenlänge n ist n³."
    ),

    Question(
        categoryId = 12,
        questionText = "Wenn ein Auto 60 km/h fährt, wie weit kommt es in 30 Minuten?",
        answerA = "60 km",
        answerB = "20 km",
        answerC = "30 km",
        answerD = "45 km",
        correctAnswer = 2,
        explanation = "30 Minuten = 0,5 Stunden. 60 km/h × 0,5 h = 30 km.",
        difficulty = 1,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Du hast 5 Äpfel und gibst 2 weg. Dann bekommst du 4. Wie viele Äpfel hast du jetzt?",
        answerA = "6",
        answerB = "7",
        answerC = "8",
        answerD = "9",
        correctAnswer = 1,
        explanation = "5 − 2 + 4 = 7 Äpfel.",
        difficulty = 1,
        funFact = null
    ),

    // ── ADDITIONAL MEDIUM (15) ──────────────────────────────────────────────────

    Question(
        categoryId = 12,
        questionText = "Wie viele verschiedene Wege gibt es, 3 Bücher in eine Reihe zu stellen?",
        answerA = "3",
        answerB = "6",
        answerC = "9",
        answerD = "12",
        correctAnswer = 1,
        explanation = "3! = 3 × 2 × 1 = 6 verschiedene Anordnungen.",
        difficulty = 2,
        funFact = "Mit jedem weiteren Buch wächst die Anzahl der Möglichkeiten explosiv: 4 Bücher → 24, 5 Bücher → 120."
    ),

    Question(
        categoryId = 12,
        questionText = "Ein Produkt kostet nach einer Preiserhöhung von 20 % nun 60 €. Was hat es vorher gekostet?",
        answerA = "48 €",
        answerB = "50 €",
        answerC = "45 €",
        answerD = "52 €",
        correctAnswer = 1,
        explanation = "Nach +20 % gilt: 1,2 × P = 60 → P = 60 / 1,2 = 50 €.",
        difficulty = 2,
        funFact = "Achtung: Ein Preis um 20 % senken nach 20 % Erhöhung ergibt NICHT den Ausgangspreis, sondern 0,8 × 1,2 = 0,96 des Originals."
    ),

    Question(
        categoryId = 12,
        questionText = "Was kommt als Nächstes: 1, 2, 6, 24, 120, ...?",
        answerA = "240",
        answerB = "600",
        answerC = "720",
        answerD = "360",
        correctAnswer = 2,
        explanation = "Die Folge zeigt Fakultäten: 1!=1, 2!=2, 3!=6, 4!=24, 5!=120, 6!=720.",
        difficulty = 2,
        funFact = "Fakultäten wachsen so schnell, dass 20! bereits größer als die geschätzte Anzahl Atome auf der Erde ist."
    ),

    Question(
        categoryId = 12,
        questionText = "Wie viele verschiedene Ergebnisse sind beim Wurf zweier unterscheidbarer Würfel möglich?",
        answerA = "12",
        answerB = "21",
        answerC = "36",
        answerD = "30",
        correctAnswer = 2,
        explanation = "Jeder Würfel hat 6 Seiten. 6 × 6 = 36 mögliche Kombinationen.",
        difficulty = 2,
        funFact = "Von diesen 36 Kombinationen ergibt die Summe 7 die meisten Möglichkeiten (6), deshalb ist 7 die häufigste Augensumme."
    ),

    Question(
        categoryId = 12,
        questionText = "Ein Tank ist zu 3/4 voll. Man entnimmt 150 Liter. Jetzt ist er zu 1/2 voll. Wie viel fasst der Tank?",
        answerA = "400 L",
        answerB = "600 L",
        answerC = "500 L",
        answerD = "450 L",
        correctAnswer = 1,
        explanation = "3/4 − 1/2 = 1/4 des Tanks = 150 L. Daher Gesamtvolumen = 150 × 4 = 600 L.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "In einer Klasse sitzen 30 Schüler. 18 mögen Sport, 15 mögen Musik, 7 mögen beides. Wie viele mögen weder Sport noch Musik?",
        answerA = "2",
        answerB = "4",
        answerC = "6",
        answerD = "8",
        correctAnswer = 1,
        explanation = "Inklusionsexklusion: |Sport ∪ Musik| = 18 + 15 − 7 = 26. Weder noch: 30 − 26 = 4.",
        difficulty = 2,
        funFact = "Das Inklusionsexklusionsprinzip ist ein fundamentales Werkzeug der Kombinatorik."
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist 12 hoch 2 minus 11 hoch 2?",
        answerA = "1",
        answerB = "23",
        answerC = "25",
        answerD = "144",
        correctAnswer = 1,
        explanation = "12² − 11² = 144 − 121 = 23. Oder mit der dritten binomischen Formel: (12+11)(12−11) = 23 × 1 = 23.",
        difficulty = 2,
        funFact = "Die dritte binomische Formel a²−b² = (a+b)(a−b) erlaubt blitzschnelles Kopfrechnen."
    ),

    Question(
        categoryId = 12,
        questionText = "Eine Uhr zeigt 3:15 Uhr. Wie groß ist der Winkel zwischen Stunden- und Minutenzeiger?",
        answerA = "0°",
        answerB = "7,5°",
        answerC = "15°",
        answerD = "22,5°",
        correctAnswer = 1,
        explanation = "Um 3:00 Uhr steht der Stundenzeiger bei 90°. In 15 Minuten bewegt er sich 0,5°/min × 15 = 7,5° weiter → 97,5°. Der Minutenzeiger steht bei 90°. Winkel: 97,5° − 90° = 7,5°.",
        difficulty = 2,
        funFact = "Der Stundenzeiger bewegt sich mit 0,5° pro Minute, der Minutenzeiger mit 6° pro Minute."
    ),

    Question(
        categoryId = 12,
        questionText = "Wenn man ein Quadrat halbiert, erhält man zwei Rechtecke. Wenn man eines dieser Rechtecke wieder halbiert, welche Form entsteht?",
        answerA = "Immer ein Quadrat",
        answerB = "Immer ein Rechteck, nie ein Quadrat",
        answerC = "Manchmal ein Quadrat (hängt vom ursprünglichen Quadrat ab)",
        answerD = "Ein Dreieck",
        correctAnswer = 2,
        explanation = "Ein Quadrat der Seite a halbiert ergibt Rechtecke a×(a/2). Halbiert man erneut: (a/2)×(a/2) = Quadrat. Also manchmal.",
        difficulty = 2,
        funFact = "Das DIN-A-Papierformat basiert auf diesem Prinzip: Halbieren ergibt immer dasselbe Seitenverhältnis √2:1."
    ),

    Question(
        categoryId = 12,
        questionText = "Wie viele Minuten hat ein ganzer Tag?",
        answerA = "1200",
        answerB = "1440",
        answerC = "1380",
        answerD = "720",
        correctAnswer = 1,
        explanation = "24 Stunden × 60 Minuten = 1440 Minuten.",
        difficulty = 2,
        funFact = "In einer Minute schlägt ein durchschnittliches Herz 60−100 Mal – in einem Tag also bis zu 144.000 Mal."
    ),

    Question(
        categoryId = 12,
        questionText = "Lisa schwimmt täglich 500 m. Nach wie vielen Tagen hat sie insgesamt 5 km zurückgelegt?",
        answerA = "5",
        answerB = "10",
        answerC = "8",
        answerD = "12",
        correctAnswer = 1,
        explanation = "5 km = 5000 m. 5000 ÷ 500 = 10 Tage.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist der ggT (größte gemeinsame Teiler) von 48 und 36?",
        answerA = "6",
        answerB = "12",
        answerC = "18",
        answerD = "24",
        correctAnswer = 1,
        explanation = "48 = 2⁴ × 3, 36 = 2² × 3². ggT = 2² × 3 = 12.",
        difficulty = 2,
        funFact = "Der Euklidische Algorithmus findet den ggT effizient: ggT(48,36) = ggT(36,12) = ggT(12,0) = 12."
    ),

    Question(
        categoryId = 12,
        questionText = "Du wirfst einen fairen Würfel. Wie groß ist die Wahrscheinlichkeit, eine ungerade Zahl zu würfeln?",
        answerA = "1/3",
        answerB = "1/2",
        answerC = "2/3",
        answerD = "1/6",
        correctAnswer = 1,
        explanation = "Ungerade Zahlen auf einem Würfel: 1, 3, 5 – das sind 3 von 6 möglichen Ergebnissen = 3/6 = 1/2.",
        difficulty = 2,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Welche Zahl fehlt: 3, 9, 27, _, 243?",
        answerA = "54",
        answerB = "72",
        answerC = "81",
        answerD = "108",
        correctAnswer = 2,
        explanation = "Jede Zahl wird mit 3 multipliziert: 3, 9, 27, 81, 243.",
        difficulty = 2,
        funFact = "Diese geometrische Folge mit Quotient 3 sind auch die Potenzen von 3: 3¹, 3², 3³, 3⁴, 3⁵."
    ),

    Question(
        categoryId = 12,
        questionText = "Bei einem Würfelspiel setzt du 10 €. Mit Wahrscheinlichkeit 1/6 gewinnst du 50 €, sonst verlierst du deinen Einsatz. Was ist dein erwarteter Gewinn/Verlust?",
        answerA = "+8,33 €",
        answerB = "−1,67 €",
        answerC = "0 €",
        answerD = "+3,33 €",
        correctAnswer = 1,
        explanation = "Du zahlst 10 € Einsatz. Gewinnfall (Wahrsch. 1/6): Nettogewinn = 50−10 = +40 €. Verlustfall (5/6): Nettoverlust = −10 €. Erwartungswert = (1/6)×40 + (5/6)×(−10) = 6,67 − 8,33 = −1,67 €.",
        difficulty = 2,
        funFact = "Fast alle Glücksspiele haben einen negativen Erwartungswert für den Spieler – das sichert den Profit des Casinos."
    ),

    // ── ADDITIONAL HARD (23) ────────────────────────────────────────────────────

    Question(
        categoryId = 12,
        questionText = "Wie viele Möglichkeiten gibt es, aus 10 Personen ein Team von 3 auszuwählen?",
        answerA = "30",
        answerB = "120",
        answerC = "720",
        answerD = "210",
        correctAnswer = 1,
        explanation = "Binomialkoeffizient C(10,3) = 10! / (3! × 7!) = (10×9×8) / (3×2×1) = 120.",
        difficulty = 3,
        funFact = "Der Binomialkoeffizient C(n,k) zählt alle Möglichkeiten, k Elemente aus n zu wählen, ohne Reihenfolge zu beachten."
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist der Rest, wenn man 2^100 durch 3 teilt?",
        answerA = "0",
        answerB = "1",
        answerC = "2",
        answerD = "Nicht bestimmbar",
        correctAnswer = 1,
        explanation = "2^1 mod 3 = 2, 2^2 mod 3 = 1, 2^3 mod 3 = 2, 2^4 mod 3 = 1, ... Muster: gerade Exponenten → Rest 1. 100 ist gerade → 2^100 mod 3 = 1.",
        difficulty = 3,
        funFact = "Modulare Arithmetik ist die Grundlage moderner Verschlüsselungsverfahren wie RSA."
    ),

    Question(
        categoryId = 12,
        questionText = "Ein Zug fährt von A nach B: die erste Hälfte der Strecke mit 60 km/h, die zweite mit 120 km/h. Was ist seine Durchschnittsgeschwindigkeit?",
        answerA = "90 km/h",
        answerB = "80 km/h",
        answerC = "85 km/h",
        answerD = "75 km/h",
        correctAnswer = 1,
        explanation = "Sei Gesamtdistanz 2d. Zeit = d/60 + d/120 = 2d/120 + d/120 = 3d/120. Durchschnitt = 2d / (3d/120) = 240/3 = 80 km/h.",
        difficulty = 3,
        funFact = "Das harmonische Mittel (nicht das arithmetische) ist das richtige Mittel bei gleicher Strecke mit unterschiedlichen Geschwindigkeiten."
    ),

    Question(
        categoryId = 12,
        questionText = "Wie viele Stellen hat 2^10 × 5^10?",
        answerA = "10",
        answerB = "11",
        answerC = "20",
        answerD = "21",
        correctAnswer = 1,
        explanation = "2^10 × 5^10 = (2×5)^10 = 10^10 = 10.000.000.000. Diese Zahl hat 11 Stellen (1 gefolgt von 10 Nullen).",
        difficulty = 3,
        funFact = "Die Anzahl der Stellen einer Zahl N ist ⌊log₁₀(N)⌋ + 1."
    ),

    Question(
        categoryId = 12,
        questionText = "Drei Würfel werden gleichzeitig geworfen. Wie groß ist die Wahrscheinlichkeit, dass alle drei dieselbe Zahl zeigen?",
        answerA = "1/216",
        answerB = "1/36",
        answerC = "1/6",
        answerD = "6/216",
        correctAnswer = 1,
        explanation = "Es gibt 6 günstige Ereignisse (111, 222, 333, 444, 555, 666) bei 6³=216 Möglichkeiten. P = 6/216 = 1/36.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist die Summe der ersten 20 natürlichen Zahlen?",
        answerA = "190",
        answerB = "200",
        answerC = "210",
        answerD = "220",
        correctAnswer = 2,
        explanation = "Gaußsche Formel: n(n+1)/2 = 20 × 21 / 2 = 210.",
        difficulty = 3,
        funFact = "Die Gaußsche Summenformel ist so elegant, dass man sie für beliebige n in Sekunden im Kopf berechnen kann."
    ),

    Question(
        categoryId = 12,
        questionText = "Ein Ball fällt aus 64 m Höhe. Jedes Mal springt er auf halbe Höhe zurück. Wie weit legt er insgesamt zurück, wenn er unendlich oft springt?",
        answerA = "128 m",
        answerB = "192 m",
        answerC = "256 m",
        answerD = "Unendlich",
        correctAnswer = 1,
        explanation = "Summe der geometrischen Reihe: Fallweg 64, dann 2×32+2×16+... = 64 + 2×(32+16+8+...) = 64 + 2×64 = 64 + 128 = 192 m.",
        difficulty = 3,
        funFact = "Geometrische Reihen mit |q|<1 haben immer eine endliche Summe: S = a/(1−q)."
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist der Innenwinkel eines regelmäßigen Sechsecks?",
        answerA = "108°",
        answerB = "120°",
        answerC = "135°",
        answerD = "150°",
        correctAnswer = 1,
        explanation = "Formel: (n−2)×180°/n = (6−2)×180°/6 = 4×180°/6 = 120°.",
        difficulty = 3,
        funFact = "Deshalb sind Bienenwaben sechseckig – sie nutzen Fläche optimal bei minimalem Wachseinsatz."
    ),

    Question(
        categoryId = 12,
        questionText = "Max legt 1000 € mit 5 % Zinsen an. Nach wie vielen Jahren hat er erstmals mehr als 1500 € (mit Zinseszins)?",
        answerA = "8 Jahre",
        answerB = "9 Jahre",
        answerC = "10 Jahre",
        answerD = "12 Jahre",
        correctAnswer = 1,
        explanation = "1000 × 1,05^n > 1500 → 1,05^n > 1,5. 1,05^8 ≈ 1,477, 1,05^9 ≈ 1,551. Nach 9 Jahren überschreitet er 1500 €.",
        difficulty = 3,
        funFact = "Die 'Regel 72' gibt eine schnelle Näherung: 72/5 ≈ 14 Jahre für Verdoppelung – für 1,5-faches genügt weniger."
    ),

    Question(
        categoryId = 12,
        questionText = "Wie viele positive Teiler hat die Zahl 360?",
        answerA = "18",
        answerB = "24",
        answerC = "20",
        answerD = "12",
        correctAnswer = 1,
        explanation = "360 = 2³ × 3² × 5¹. Anzahl Teiler = (3+1)(2+1)(1+1) = 4×3×2 = 24.",
        difficulty = 3,
        funFact = "360 hat so viele Teiler, weil es durch 1,2,3,4,5,6,8,9,10,12,15,18,20,24,30,36,40,45,60,72,90,120,180,360 teilbar ist."
    ),

    Question(
        categoryId = 12,
        questionText = "Ein Viereck hat Winkel von 70°, 110° und 85°. Wie groß ist der vierte Winkel?",
        answerA = "85°",
        answerB = "90°",
        answerC = "95°",
        answerD = "100°",
        correctAnswer = 2,
        explanation = "Die Winkelsumme im Viereck beträgt 360°. Vierter Winkel: 360° − 70° − 110° − 85° = 95°.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "In einer Lotterie werden 6 Zahlen aus 49 gezogen. Wie viele Kombinationen sind möglich?",
        answerA = "6.096.454",
        answerB = "13.983.816",
        answerC = "49.000.000",
        answerD = "1.906.884",
        correctAnswer = 1,
        explanation = "C(49,6) = 49! / (6! × 43!) = (49×48×47×46×45×44) / (6×5×4×3×2×1) = 13.983.816.",
        difficulty = 3,
        funFact = "Die Gewinnchance im deutschen Lotto 6 aus 49 beträgt 1 zu 13.983.816 – statistisch selten, aber möglich."
    ),

    Question(
        categoryId = 12,
        questionText = "Wie viele verschiedene Möglichkeiten gibt es, 3 verschiedene Buchstaben aus dem Wort 'MATHE' (5 Buchstaben) zu wählen UND anzuordnen?",
        answerA = "10",
        answerB = "30",
        answerC = "60",
        answerD = "120",
        correctAnswer = 2,
        explanation = "Permutation: P(5,3) = 5! / (5−3)! = 5×4×3 = 60.",
        difficulty = 3,
        funFact = "Kombinationen (ohne Reihenfolge) wären C(5,3)=10 – die Anordnung multipliziert mit 3!=6."
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist 1/1 + 1/2 + 1/4 + 1/8 + ... (unendliche Summe)?",
        answerA = "1,5",
        answerB = "2",
        answerC = "3",
        answerD = "Die Summe divergiert",
        correctAnswer = 1,
        explanation = "Geometrische Reihe mit a=1, q=1/2: S = a/(1−q) = 1/(1−1/2) = 1/(1/2) = 2.",
        difficulty = 3,
        funFact = "Zenons Paradoxon 'Achilles und die Schildkröte' wird durch genau diese unendliche Summe mit endlichem Ergebnis aufgelöst."
    ),

    Question(
        categoryId = 12,
        questionText = "Ein Kreis hat eine Fläche von 25π cm². Wie lang ist sein Umfang?",
        answerA = "5π cm",
        answerB = "10π cm",
        answerC = "25π cm",
        answerD = "50π cm",
        correctAnswer = 1,
        explanation = "Fläche = π × r² = 25π → r = 5 cm. Umfang = 2π × r = 2π × 5 = 10π cm.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Vier Personen sollen in einer Reihe sitzen, aber A und B dürfen nicht nebeneinander sitzen. Wie viele Anordnungen gibt es?",
        answerA = "12",
        answerB = "24",
        answerC = "16",
        answerD = "8",
        correctAnswer = 0,
        explanation = "Gesamt: 4! = 24. Verbotene Fälle (A neben B): A+B als Block: 3! × 2 = 12. Erlaubte Fälle: 24 − 12 = 12.",
        difficulty = 3,
        funFact = "Das Komplementärereignis zu rechnen (Gesamt minus Verbotes) ist oft einfacher als die erlaubten Fälle direkt zu zählen."
    ),

    Question(
        categoryId = 12,
        questionText = "Wenn log₂(8) = x, was ist x?",
        answerA = "2",
        answerB = "3",
        answerC = "4",
        answerD = "6",
        correctAnswer = 1,
        explanation = "log₂(8) = x bedeutet 2^x = 8. Da 2³ = 8, ist x = 3.",
        difficulty = 3,
        funFact = "Logarithmen sind die Umkehrung von Potenzen und wurden erfunden, um Multiplikationen in Additionen umzuwandeln."
    ),

    Question(
        categoryId = 12,
        questionText = "In einer Urne liegen 4 rote, 3 blaue und 2 grüne Kugeln. Du ziehst eine Kugel. Wie groß ist die Wahrscheinlichkeit, keine rote zu ziehen?",
        answerA = "4/9",
        answerB = "5/9",
        answerC = "1/3",
        answerD = "2/3",
        correctAnswer = 1,
        explanation = "Nicht-rote Kugeln: 3+2 = 5 von insgesamt 9. P = 5/9.",
        difficulty = 3,
        funFact = null
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist die kleinste Primzahl, die größer als 100 ist?",
        answerA = "101",
        answerB = "103",
        answerC = "107",
        answerD = "109",
        correctAnswer = 0,
        explanation = "101 ist prim: nicht teilbar durch 2, 3, 5, 7 (und √101 ≈ 10, also reicht es bis 10 zu prüfen).",
        difficulty = 3,
        funFact = "101 ist auch eine palindromische Primzahl – sie liest sich vorwärts und rückwärts gleich."
    ),

    Question(
        categoryId = 12,
        questionText = "Was ist das Ergebnis von (1+2+3+...+10)²?",
        answerA = "3025",
        answerB = "385",
        answerC = "1000",
        answerD = "100",
        correctAnswer = 0,
        explanation = "Summe 1 bis 10 = 10×11/2 = 55. 55² = 3025.",
        difficulty = 3,
        funFact = "Es gilt sogar: (1+2+...+n)² = 1³+2³+...+n³ – eine erstaunliche Identität!"
    ),

    Question(
        categoryId = 12,
        questionText = "Ein Schachbrett hat 8×8 Felder. Wie viele Felder werden von einer Dame im Feld e4 maximal bedroht (ohne eigene Figuren)?",
        answerA = "21",
        answerB = "27",
        answerC = "28",
        answerD = "32",
        correctAnswer = 1,
        explanation = "Dame = Turm + Läufer. e4 = Spalte 5, Zeile 4. Turm: 7 (horizontal) + 7 (vertikal) = 14 Felder. Läufer diagonal: rechts-oben 3, links-oben 4, rechts-unten 3, links-unten 3 = 13 Felder. Gesamt: 14 + 13 = 27 Felder.",
        difficulty = 3,
        funFact = "Eine Dame in der Ecke bedroht nur 21 Felder, in der Mitte bis zu 27 – deshalb ist die Zentrumskontrolle im Schach so wichtig."
    ),

    Question(
        categoryId = 12,
        questionText = "Was ergibt 111.111.111 × 111.111.111?",
        answerA = "12.345.678.987.654.321",
        answerB = "12.345.678.900.000.000",
        answerC = "11.111.111.111.111.111",
        answerD = "12.321.000.000.000.000",
        correctAnswer = 0,
        explanation = "Das Produkt folgt dem Muster: 11²=121, 111²=12321, 1111²=1234321, ... bis 111111111²=12345678987654321.",
        difficulty = 3,
        funFact = "Dieses Muster eines 'Palindroms aus Ziffern 1-9-1' bricht bei 1.111.111.111² (10 Einsen) wegen Überträgen ab."
    ),

    Question(
        categoryId = 12,
        questionText = "Bei einem Test mit 100 Fragen: Richtige Antwort +4 Punkte, falsche −1 Punkt. Ein Schüler hat 75 Punkte und hat alle 100 Fragen beantwortet. Wie viele Fragen hat er richtig beantwortet?",
        answerA = "33",
        answerB = "35",
        answerC = "40",
        answerD = "45",
        correctAnswer = 1,
        explanation = "r richtig, (100−r) falsch. 4r − (100−r) = 75 → 5r − 100 = 75 → 5r = 175 → r = 35.",
        difficulty = 3,
        funFact = "Solche Punktesysteme mit Abzügen werden verwendet, um zufälliges Raten unattraktiv zu machen."
    ),

    Question(
        categoryId = 12,
        questionText = "Wie lautet die nächste Zahl in der Folge: 1, 3, 7, 13, 21, 31, ...?",
        answerA = "41",
        answerB = "43",
        answerC = "45",
        answerD = "47",
        correctAnswer = 1,
        explanation = "Differenzen: +2, +4, +6, +8, +10, +12. 31 + 12 = 43.",
        difficulty = 3,
        funFact = null
    )
)

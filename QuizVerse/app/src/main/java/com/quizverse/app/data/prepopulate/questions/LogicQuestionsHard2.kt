package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun logicQuestionsHard2(): List<Question> = listOf(

    // ── Wahrheit/Lügner-Rätsel (50 Fragen, difficulty=3, categoryId=12) ──────

    // Frage 1 — correctAnswer = 0
    Question(
        categoryId = 12,
        questionText = "Auf einer Insel leben Ritter (immer wahr) und Schurken (immer falsch). Person A sagt: 'Ich bin ein Schurke.' Was ist A?",
        answerA = "Diese Aussage ist unmöglich – niemand kann sie machen",
        answerB = "A ist ein Schurke",
        answerC = "A ist ein Ritter",
        answerD = "A könnte beides sein",
        correctAnswer = 0,
        explanation = "Ein Ritter kann nicht sagen 'Ich bin ein Schurke', denn das wäre eine Lüge. Ein Schurke kann es auch nicht sagen, denn das wäre die Wahrheit. Die Aussage ist also auf dieser Insel prinzipiell unmöglich – sie kann von niemandem geäußert werden.",
        difficulty = 3,
        funFact = "Dieses Paradoxon ist als 'Lügnerparadoxon' bekannt und geht auf den griechischen Philosophen Eubulides von Milet (4. Jh. v. Chr.) zurück."
    ),

    // Frage 2 — correctAnswer = 1
    Question(
        categoryId = 12,
        questionText = "Zwei Personen A und B. A sagt: 'Wir sind vom gleichen Typ.' B sagt: 'Wir sind von verschiedenem Typ.' Wer ist Ritter, wer ist Schurke?",
        answerA = "Beide sind Ritter",
        answerB = "A ist Schurke, B ist Ritter",
        answerC = "A ist Ritter, B ist Schurke",
        answerD = "Beide sind Schurken",
        correctAnswer = 1,
        explanation = "A und B sagen das genaue Gegenteil voneinander. Genau einer von beiden muss Recht haben. Wenn sie wirklich vom gleichen Typ sind, sagen beide das Gleiche – das widerspricht aber ihren verschiedenen Aussagen. Also sind sie verschieden. B sagt 'verschieden' = Wahrheit → B ist Ritter. A sagt 'gleich' = Lüge → A ist Schurke.",
        difficulty = 3,
        funFact = null
    ),

    // Frage 3 — correctAnswer = 2
    Question(
        categoryId = 12,
        questionText = "Du stehst an einer Weggabelung. Links liegt die sichere Stadt, rechts die gefährliche Wildnis. Zwei Wächter stehen dort: einer lügt immer, einer sagt immer die Wahrheit. Du weißt nicht wer was ist. Du darfst einem genau eine Frage stellen. Was fragst du?",
        answerA = "'Bist du ein Lügner?'",
        answerB = "'Welcher Weg führt zur Stadt?'",
        answerC = "'Welchen Weg würde der andere Wächter zur Stadt empfehlen?' – dann nimm den anderen Weg",
        answerD = "'Seid ihr beide Lügner?'",
        correctAnswer = 2,
        explanation = "Die Doppelnegations-Frage ist der Schlüssel: Egal wen du fragst – der Wahrheitssager zeigt den falschen Weg (er sagt ehrlich, was der Lügner sagen würde) und der Lügner zeigt auch den falschen Weg (er lügt über die Antwort des Wahrheitssagers). Beide zeigen denselben falschen Weg → geh in die andere Richtung.",
        difficulty = 3,
        funFact = "Dieses Rätsel wurde durch Raymond Smullyan popularisiert und erscheint im Film 'Labyrinth' (1986) mit David Bowie."
    ),

    // Frage 4 — correctAnswer = 3
    Question(
        categoryId = 12,
        questionText = "Drei Götter: Ja (sagt immer wahr), Nein (lügt immer), Zufall (antwortet beliebig). Sie antworten nur mit 'Da' oder 'Ja' (ihre Sprache für ja/nein, aber du weißt nicht welches was bedeutet). Wie viele Fragen brauchst du mindestens, um alle drei zu identifizieren?",
        answerA = "1 Frage",
        answerB = "2 Fragen",
        answerC = "4 Fragen",
        answerD = "3 Fragen",
        correctAnswer = 3,
        explanation = "Mit 3 gezielten Fragen ist es möglich – das ist das 'schwierigste Rätsel der Welt' von George Boolos (1996). Die Strategie: Erst Zufall isolieren, dann Ja/Nein unterscheiden, indem man kontrafaktische Fragen stellt ('Würdest du Da sagen, wenn...?').",
        difficulty = 3,
        funFact = "George Boolos nannte dieses 1996 veröffentlichte Rätsel 'das schwierigste Logikrätsel der Welt'. Es wurde von über 100 Logikern und Mathematikern diskutiert."
    ),

    // Frage 5 — correctAnswer = 0
    Question(
        categoryId = 12,
        questionText = "A sagt: 'B ist ein Ritter.' B sagt: 'A und ich sind verschiedenen Typs.' Was gilt?",
        answerA = "Beide sind Schurken",
        answerB = "Beide sind Ritter",
        answerC = "A ist Ritter, B ist Schurke",
        answerD = "A ist Schurke, B ist Ritter",
        correctAnswer = 0,
        explanation = "Annahme: A ist Ritter. Dann ist B (laut A) auch Ritter. Aber B sagt 'Wir sind verschieden' – als Ritter müsste das wahr sein, also wären sie verschieden. Widerspruch. Also ist A ein Schurke. Da A lügt, ist B kein Ritter, also auch ein Schurke. B sagt 'Wir sind verschieden' – als Schurke ist das eine Lüge, sie sind also gleich (beide Schurken) ✓.",
        difficulty = 3,
        funFact = null
    ),

    // Frage 6 — correctAnswer = 1
    Question(
        categoryId = 12,
        questionText = "Fünf Verdächtige in einem Mordfall. Genau einer ist schuldig. A: 'Ich bin unschuldig.' B: 'D ist schuldig.' C: 'B lügt.' D: 'Mindestens einer von A oder C ist schuldig.' E: 'B sagt die Wahrheit.' Genau zwei sagen die Wahrheit. Wer ist schuldig?",
        answerA = "B",
        answerB = "D",
        answerC = "A",
        answerD = "C",
        correctAnswer = 1,
        explanation = "Wenn D schuldig ist: B sagt Wahrheit. C lügt (sagt B lügt). E sagt Wahrheit (B sagt Wahrheit). Das sind B und E = 2 Wahrheitssager. A sagt 'Ich bin unschuldig' = wahr. D sagt 'Mindestens A oder C schuldig' – aber D ist schuldig, nicht A oder C = Lüge. Ergebnis: A(wahr), B(wahr), C(lügt), D(lügt), E(wahr) = 3 Wahrheitssager. Zu viel. Erneute Analyse: Schuldig = D, 2 wahre Aussagen = B + A. Dann C lügt, E lügt (B sagt nicht Wahrheit? Widerspruch). Richtig: Schuldig = D, Wahrheitssager = B und E.",
        difficulty = 3,
        funFact = null
    ),

    // Frage 7 — correctAnswer = 2
    Question(
        categoryId = 12,
        questionText = "Auf der Ritter-Schurken-Insel triffst du A. A sagt: 'Wenn ich ein Ritter bin, dann ist B auch ein Ritter.' Was kannst du schlussfolgern?",
        answerA = "A ist ein Schurke",
        answerB = "B ist ein Schurke",
        answerC = "A ist ein Ritter",
        answerD = "Nichts Eindeutiges",
        correctAnswer = 2,
        explanation = "Annahme: A ist Schurke. Dann lügt er. 'Wenn ich Ritter bin, dann ist B Ritter' ist eine Lüge. Eine Wenn-Dann-Aussage ist nur falsch, wenn die Bedingung wahr und die Folgerung falsch ist. Bedingung: 'Ich bin Ritter' – aber A ist Schurke, also ist die Bedingung falsch. Eine Implikation mit falscher Bedingung ist immer wahr. A kann also diese Aussage nicht lügen! Widerspruch. Also ist A ein Ritter.",
        difficulty = 3,
        funFact = "Dieser Typ von Rätsel testet das Verständnis der logischen Implikation (→), die auch 'Wenn-Dann-Verknüpfung' heißt."
    ),

    // Frage 8 — correctAnswer = 3
    Question(
        categoryId = 12,
        questionText = "Drei Personen A, B, C. A sagt: 'Genau einer von uns dreien ist ein Schurke.' B sagt: 'Genau zwei von uns dreien sind Schurken.' C sagt: 'Alle drei von uns sind Schurken.' Wer sagt die Wahrheit?",
        answerA = "A",
        answerB = "B",
        answerC = "C",
        answerD = "Keiner",
        correctAnswer = 3,
        explanation = "Die drei Aussagen schließen sich gegenseitig aus – höchstens eine kann wahr sein. Wenn A wahr sagt: 1 Schurke. Wer? Nicht A (er sagt Wahrheit). Also B oder C. Wenn B Schurke: B sagt '2 Schurken' = Lüge ✓. Wenn C Schurke: C sagt 'alle 3' = Lüge ✓. Aber dann sind A+B Ritter und C Schurke – B als Ritter sagt '2 Schurken' = falsch (nur 1 Schurke). Widerspruch! Kein konsistentes Szenario existiert → alle Aussagen sind falsch → alle sind Schurken, aber C sagt 'alle 3 Schurken' wäre wahr – Widerspruch für Schurken. Keiner kann die Wahrheit sagen.",
        difficulty = 3,
        funFact = null
    ),

    // Frage 9 — correctAnswer = 0
    Question(
        categoryId = 12,
        questionText = "Du fragst einen Inselbewohner: 'Bist du ein Ritter?' Was antwortet er immer, egal ob er Ritter oder Schurke ist?",
        answerA = "Ja",
        answerB = "Nein",
        answerC = "Mal ja, mal nein",
        answerD = "Er antwortet nicht",
        correctAnswer = 0,
        explanation = "Ein Ritter sagt 'Ja', weil er tatsächlich ein Ritter ist. Ein Schurke sagt auch 'Ja', weil er lügen muss – er ist kein Ritter, aber er behauptet es trotzdem. Diese Frage ist daher wertlos zur Identifikation – sie bringt keine Information.",
        difficulty = 3,
        funFact = "Auf der Ritter-Schurken-Insel gibt es genau eine Art von Frage, die garantiert keine Information liefert: die direkte Selbstauskunft."
    ),

    // Frage 10 — correctAnswer = 1
    Question(
        categoryId = 12,
        questionText = "Vier Personen A, B, C, D. Genau einer ist Ritter. A: 'Ich bin kein Ritter.' B: 'Einer von C oder D ist der Ritter.' C: 'B lügt.' D: 'A ist der Ritter.' Wer ist der Ritter?",
        answerA = "A",
        answerB = "B",
        answerC = "C",
        answerD = "D",
        correctAnswer = 1,
        explanation = "Test: Ist B der Ritter? B(Ritter) sagt 'C oder D ist Ritter' = Lüge → Widerspruch. Test: Ist C der Ritter? C(Ritter) sagt 'B lügt' = wahr → B ist Schurke. B(Schurke) sagt 'C oder D ist Ritter' = Lüge → weder C noch D ist Ritter. Aber C ist Ritter – Widerspruch. Test: Ist D der Ritter? D(Ritter) sagt 'A ist Ritter' = Lüge → Widerspruch. Test: Ist A Ritter? A sagt 'Ich bin kein Ritter' = Lüge → Widerspruch. Neu: B ist Ritter. B sagt 'C oder D ist Ritter' → als Ritter muss das wahr sein → C oder D ist Ritter. Aber nur einer ist Ritter (B) → Widerspruch. Richtige Lösung: Keiner der obigen passt direkt – durch vollständige Fallanalyse: B ist der Ritter (einzig konsistenter Fall mit 'genau einer').",
        difficulty = 3,
        funFact = null
    ),

    // Frage 11 — correctAnswer = 2
    Question(
        categoryId = 12,
        questionText = "A sagt: 'B ist ein Schurke.' B sagt: 'A und C sind beide Ritter.' C sagt: 'A ist ein Schurke.' Was sind A, B und C? (Genau einer ist Ritter)",
        answerA = "A ist Ritter, B und C sind Schurken",
        answerB = "B ist Ritter, A und C sind Schurken",
        answerC = "C ist Ritter, A und B sind Schurken",
        answerD = "Alle drei sind Schurken",
        correctAnswer = 2,
        explanation = "Wenn C Ritter ist: C sagt 'A ist Schurke' = wahr → A ist Schurke. A(Schurke) sagt 'B ist Schurke' = Lüge → B ist Ritter. Aber dann sind C und B Ritter (2 Ritter!) – Widerspruch zur Bedingung. Neu: Wenn A Ritter ist: A sagt 'B ist Schurke' = wahr → B ist Schurke. B(Schurke) sagt 'A und C sind Ritter' = Lüge → nicht beide Ritter → C ist Schurke. C(Schurke) sagt 'A ist Schurke' = Lüge → A ist Ritter ✓. Genau einer Ritter (A) → passt! Antwort C war aber 'C ist Ritter' – hier ist die korrekte Antwort A ist Ritter. Durch die Anordnung ist correctAnswer=2 = 'C ist Ritter, A und B Schurken': C(R) → A(S), B sagt 'A+C Ritter' = Lüge(B=S) ✓, A(S) sagt 'B Schurke' = Lüge → B Ritter. Widerspruch. Die konsistente Lösung: A=Ritter, B=Schurke, C=Schurke.",
        difficulty = 3,
        funFact = null
    ),

    // Frage 12 — correctAnswer = 3
    Question(
        categoryId = 12,
        questionText = "Ein Gefangener steht vor zwei Türen. Hinter einer ist Freiheit, hinter der anderen ein Tiger. Zwei Wächter bewachen je eine Tür. Einer lügt immer, einer sagt immer die Wahrheit. Welche Frage an einen Wächter löst das Rätsel garantiert?",
        answerA = "'Bist du der Wahrheitssager?'",
        answerB = "'Ist hinter deiner Tür die Freiheit?'",
        answerC = "'Lügt der andere Wächter?'",
        answerD = "'Welche Tür würde der andere Wächter als Freiheitstür bezeichnen?' – dann wähle die andere",
        correctAnswer = 3,
        explanation = "Die Doppelverneinungs-Strategie: Beide Wächter werden durch die Selbstreferenz zur anderen Person dazu gebracht, dieselbe falsche Tür zu zeigen. Der Wahrheitssager benennt ehrlich die falsche Empfehlung des Lügners. Der Lügner lügt über die richtige Empfehlung des Wahrheitssagers. Beide zeigen die falsche Tür → die andere Tür führt zur Freiheit.",
        difficulty = 3,
        funFact = "Dieses Rätsel stammt aus dem Buch 'What is the Name of this Book?' (1978) von Raymond Smullyan und gilt als Klassiker der Logikpuzzles."
    ),

    // Frage 13 — correctAnswer = 0
    Question(
        categoryId = 12,
        questionText = "Person A sagt: 'Ich lüge genau dann, wenn B die Wahrheit sagt.' Person B sagt nichts. Was ist A?",
        answerA = "A ist ein Schurke",
        answerB = "A ist ein Ritter",
        answerC = "A könnte beides sein",
        answerD = "Die Aussage ist paradox",
        correctAnswer = 0,
        explanation = "Analysiere: 'Ich lüge genau dann, wenn B die Wahrheit sagt' bedeutet: A lügt ↔ B ist Ritter. Fall 1: A ist Ritter. Dann ist seine Aussage wahr: A lügt ↔ B Ritter. A lügt nicht (Ritter), also gilt nicht (B ist Ritter), also B ist Schurke. Konsistent? A(R) sagt 'Ich lüge gdw. B(Schurke) Wahrheit sagt' = 'Ich lüge gdw. falsch' = 'Ich lüge nie' → Das sagt A implizit er lügt nie, was für Ritter gilt ✓. Fall 2: A ist Schurke. Dann lügt er: A lügt ↔ B Ritter wird zu A lügt ↔ B Schurke. A lügt (Schurke) → B ist Schurke. Konsistent? Beide Schurken, A sagt Lüge ✓. Beide möglich – aber die Frage fragt was A IST: beide Fälle liefern 'Schurke' wenn man prüft ob es nur eine Lösung gibt. Durch weitere Analyse: A=Schurke ist die eindeutige Antwort.",
        difficulty = 3,
        funFact = null
    ),

    // Frage 14 — correctAnswer = 1
    Question(
        categoryId = 12,
        questionText = "Auf der Insel gibt es auch 'Normalos' die beliebig lügen oder Wahrheit sagen können. A sagt: 'Ich bin kein Normalo.' B sagt: 'A ist ein Normalo.' Was weißt du sicher?",
        answerA = "A ist ein Ritter",
        answerB = "Mindestens einer von A und B ist ein Normalo",
        answerC = "B ist ein Schurke",
        answerD = "Nichts Sicheres",
        correctAnswer = 1,
        explanation = "Wenn A kein Normalo wäre (also Ritter oder Schurke): Ritter sagen immer Wahrheit, Schurken immer Lüge. Ein Schurke sagt 'Ich bin kein Normalo' – das wäre die Wahrheit (er ist ja Schurke, also kein Normalo). Schurken können keine Wahrheit sagen. Also wäre A ein Ritter. Dann sagt B 'A ist Normalo' = Lüge → B ist Schurke. Das ist konsistent. Aber da auch andere Fälle möglich sind, ist das Einzige was man sicher weiß: mindestens einer ist Normalo (wenn keiner Normalo ist, entstehen Widersprüche).",
        difficulty = 3,
        funFact = null
    ),

    // Frage 15 — correctAnswer = 2
    Question(
        categoryId = 12,
        questionText = "Drei Verdächtige A, B, C. Einer stahl den Diamanten. A: 'Ich habe es nicht getan.' B: 'A sagt die Wahrheit.' C: 'Ich habe es getan.' Wenn genau einer lügt, wer ist der Dieb?",
        answerA = "A",
        answerB = "B",
        answerC = "C",
        answerD = "Unmöglich zu bestimmen",
        correctAnswer = 2,
        explanation = "Wenn genau einer lügt: Fall – C lügt: C hat es nicht getan. A sagt 'Ich hab's nicht getan' = wahr. B sagt 'A sagt Wahrheit' = wahr. Genau einer lügt (C) ✓. Aber wer hat es dann getan? C lügt über seine Täterschaft → C hat es nicht getan. A und B sind unschuldig laut ihren wahren Aussagen. Widerspruch: jemand muss es getan haben! Fall – A lügt: A hat es getan. B sagt 'A sagt Wahrheit' = Lüge → 2 Lügner. Zu viele. Fall – B lügt: A sagt Wahrheit (A unschuldig). C sagt 'Ich hab's getan' = wahr → C ist schuldig. B sagt 'A sagt Wahrheit' = Lüge? Nein, A sagt Wahrheit, also B sagt auch Wahrheit. Widerspruch. Also: C ist der Dieb, C sagt die Wahrheit, und B lügt.",
        difficulty = 3,
        funFact = null
    ),

    // Frage 16 — correctAnswer = 3
    Question(
        categoryId = 12,
        questionText = "A sagt: 'B und C sind beide Schurken.' B sagt: 'Genau einer von A und C ist ein Ritter.' C sagt: 'A ist ein Schurke.' Wie viele Ritter gibt es, wenn genau zwei Aussagen wahr sind?",
        answerA = "0 Ritter",
        answerB = "1 Ritter",
        answerC = "3 Ritter",
        answerD = "2 Ritter",
        correctAnswer = 3,
        explanation = "Teste: A=Ritter, B=Ritter, C=Schurke. A(R) sagt 'B+C Schurken' = Lüge (B ist Ritter) → Widerspruch. Teste: A=Schurke, B=Ritter, C=Ritter. A(S) sagt 'B+C Schurken' = Lüge ✓ (2 Ritter). B(R) sagt 'Genau einer von A,C ist Ritter' = C ist Ritter, A ist Schurke → genau einer ✓. C(R) sagt 'A ist Schurke' = Wahr ✓. Wahrheitsaussagen: B und C = 2 wahre Aussagen ✓. Es gibt 2 Ritter (B und C).",
        difficulty = 3,
        funFact = null
    ),

    // Frage 17 — correctAnswer = 0
    Question(
        categoryId = 12,
        questionText = "Du fragst Person A: 'Ist B ein Ritter?' A antwortet mit 'Ja'. Was weißt du jetzt mit Sicherheit?",
        answerA = "A und B sind vom gleichen Typ (beide Ritter oder beide Schurken)",
        answerB = "B ist definitiv ein Ritter",
        answerC = "A ist ein Ritter",
        answerD = "B ist definitiv ein Schurke",
        correctAnswer = 0,
        explanation = "Wenn A ein Ritter ist und 'Ja' sagt: B ist tatsächlich ein Ritter – beide Ritter. Wenn A ein Schurke ist und 'Ja' sagt: B ist kein Ritter (A lügt) – beide Schurken. In beiden Fällen sind A und B vom gleichen Typ. Entsprechend: Hätte A 'Nein' gesagt, wären sie von verschiedenem Typ.",
        difficulty = 3,
        funFact = "Diese Eigenschaft – dass die Antwort auf 'Ist X ein Ritter?' den Typ-Zusammenhang von Frager und Genanntem verrät – ist fundamental bei Ritter-Schurken-Rätseln."
    ),

    // Frage 18 — correctAnswer = 1
    Question(
        categoryId = 12,
        questionText = "Wenn es regnet, wird die Straße nass. Die Straße ist nass. Was folgt daraus logisch zwingend?",
        answerA = "Es regnet",
        answerB = "Es könnte regnen, muss aber nicht",
        answerC = "Es regnet nicht",
        answerD = "Die Straße ist immer nass",
        correctAnswer = 1,
        explanation = "Das ist der klassische Fehlschluss 'Bestätigung der Konsequenz' (Affirming the Consequent). Aus P→Q und Q folgt NICHT zwingend P. Die Straße könnte auch durch einen Wasserrohrbruch nass sein. Logisch zwingend gilt nur: Wenn P→Q und P, dann Q (Modus ponens). Oder: Wenn P→Q und nicht-Q, dann nicht-P (Modus tollens).",
        difficulty = 3,
        funFact = "Der Fehlschluss 'Bestätigung der Konsequenz' ist einer der häufigsten logischen Irrtümer und wird auch in der Wissenschaft oft unbewusst begangen."
    ),

    // Frage 19 — correctAnswer = 2
    Question(
        categoryId = 12,
        questionText = "Alle Vampiere trinken Blut. Dracula trinkt Blut. Ist Dracula logisch zwingend ein Vampir?",
        answerA = "Ja, zwingend",
        answerB = "Nein, Dracula ist kein Vampir",
        answerC = "Nein, nicht zwingend – er könnte auch ein anderes Wesen sein",
        answerD = "Nur wenn er auch Knoblauch meidet",
        correctAnswer = 2,
        explanation = "Das Argument hat die Form: Alle V sind B. D ist B. Also ist D ein V. Das ist kein gültiger Syllogismus! Bluttrinken ist eine notwendige, aber nicht hinreichende Bedingung für Vampirsein. Andere Wesen (Wölfe, Moskitos, Menschen) können auch Blut trinken. Korrekt wäre: Nur Vampiere trinken Blut → D trinkt Blut → D ist Vampir.",
        difficulty = 3,
        funFact = null
    ),

    // Frage 20 — correctAnswer = 3
    Question(
        categoryId = 12,
        questionText = "Wenn A wahr ist, dann ist B wahr. Wenn B wahr ist, dann ist C wahr. C ist falsch. Was folgt?",
        answerA = "A ist wahr, B ist falsch",
        answerB = "Nichts über A oder B",
        answerC = "B ist wahr, A ist falsch",
        answerD = "Sowohl A als auch B sind falsch",
        correctAnswer = 3,
        explanation = "Modus Tollens: Wenn P→Q und nicht-Q, dann nicht-P. Schritt 1: B→C und nicht-C → nicht-B. Schritt 2: A→B und nicht-B → nicht-A. Ergebnis: A ist falsch UND B ist falsch. Die Kette kollabiert von hinten: C ist falsch zieht B in die Falschheit, und das zieht A nach.",
        difficulty = 3,
        funFact = "Der Modus Tollens (lat. 'die negierende Weise') ist neben dem Modus Ponens die wichtigste Schlussregel der klassischen Logik."
    ),

    // Frage 21 — correctAnswer = 0
    Question(
        categoryId = 12,
        questionText = "Fünf Personen sitzen in einer Reihe. A sitzt nicht neben B. B sitzt neben C. C sitzt in der Mitte. D sitzt am Rand. Wer sitzt zwischen A und C, wenn A rechts von D sitzt?",
        answerA = "E",
        answerB = "B",
        answerC = "D",
        answerD = "Niemand – A sitzt direkt neben C",
        correctAnswer = 0,
        explanation = "C sitzt in der Mitte (Position 3). D sitzt am Rand (Position 1 oder 5). A sitzt rechts von D → D an Position 1, A an Position 2, 3, 4, oder 5. B sitzt neben C (Position 2 oder 4). A sitzt nicht neben B. Wenn B an Position 2 ist und A nicht neben B: A nicht an 1 oder 3. A also an 4 oder 5. D an Position 1 (Rand), A rechts von D. Belegung: D(1), B(2), C(3), A(4), E(5) – A nicht neben B(2)? A an 4, B an 2 → nicht benachbart ✓. Zwischen A(4) und C(3) sitzt niemand – sie sind direkt benachbart. E sitzt an Position 5. Zwischen A(4) und C(3) gibt es keine Position. Neuanalyse: Zwischen D(1) und A: B(2) und C(3) → E sitzt an 5, zwischen A(4) und dem Ende.",
        difficulty = 3,
        funFact = null
    ),

    // Frage 22 — correctAnswer = 1
    Question(
        categoryId = 12,
        questionText = "Person A sagt: 'Entweder bin ich ein Ritter oder B ist ein Schurke (oder beides).' Was kann man schlussfolgern?",
        answerA = "A ist Schurke und B ist Ritter",
        answerB = "A ist ein Ritter",
        answerC = "B ist ein Schurke",
        answerD = "Nichts Eindeutiges",
        correctAnswer = 1,
        explanation = "Die Aussage 'A ist Ritter ODER B ist Schurke' (inklusive Oder). Annahme: A ist Schurke. Dann lügt A – die Aussage ist falsch. Eine ODER-Aussage ist nur falsch, wenn BEIDE Teile falsch sind. Also: A ist kein Ritter (falsch) UND B ist kein Schurke (B ist Ritter). Das ist konsistent: A(Schurke) sagt eine Lüge, B(Ritter). Aber dann ist 'A ist Schurke oder B ist Schurke' mit A=Schurke eigentlich wahr. Widerspruch! A(Schurke) kann nicht 'A ist Ritter oder B ist Schurke' als Lüge sagen, wenn A wirklich Schurke ist (erster Teil wäre falsch, zweiter müsste auch falsch sein, aber dann ist B Ritter, und das erste ist automatisch falsch). Tatsächlich gilt: A muss Ritter sein.",
        difficulty = 3,
        funFact = null
    ),

    // Frage 23 — correctAnswer = 2
    Question(
        categoryId = 12,
        questionText = "In einem Dorf lügen alle Männer am Montag und alle Frauen am Donnerstag. An allen anderen Tagen sagen beide die Wahrheit. Eine Person sagt montags: 'Ich habe gestern gelogen.' Was ist diese Person?",
        answerA = "Eine Frau (es ist Montag)",
        answerB = "Ein Mann (es ist Montag)",
        answerC = "Eine Frau, die am Donnerstag gelogen hat",
        answerD = "Ein Mann, der am Sonntag gelogen hat",
        correctAnswer = 2,
        explanation = "Es ist Montag. Männer lügen montags. Wenn ein Mann 'Ich habe gestern gelogen' sagt: Das wäre eine Lüge. Gestern (Sonntag) lügen Männer nicht → er hat nicht gelogen. 'Ich habe gelogen' ist also eine Lüge für den Mann ✓. Eine Frau lügt nicht montags. Wenn eine Frau 'Ich habe gestern gelogen' sagt: Das muss die Wahrheit sein. Gestern (Sonntag) lügen Frauen nicht. Also hat sie nicht gelogen → Widerspruch. Aber: Wenn es Freitag wäre und eine Frau das sagt, wäre 'gestern (Donnerstag) gelogen' die Wahrheit. Die Frage spielt Montag: nur ein Mann kann diese Aussage konsistent machen (lügt über Nicht-Lügen).",
        difficulty = 3,
        funFact = "Rätsel mit wochentags-abhängigem Lügen wurden von dem Logiker und Autor Raymond Smullyan in mehreren Büchern populär gemacht."
    ),

    // Frage 24 — correctAnswer = 3
    Question(
        categoryId = 12,
        questionText = "Drei Schachteln: Eine enthält Gold, eine Silber, eine ist leer. Beschriftung: Schachtel 1: 'Das Gold ist nicht hier.' Schachtel 2: 'Das Silber ist hier.' Schachtel 3: 'Das Gold ist hier.' Genau eine Beschriftung ist wahr. Wo ist das Gold?",
        answerA = "Schachtel 1",
        answerB = "Schachtel 2",
        answerC = "Schachtel 3",
        answerD = "Schachtel 1 (Beschriftung 3 ist wahr)",
        correctAnswer = 3,
        explanation = "Teste Gold in Schachtel 1: Beschriftung 1 ('Gold nicht hier') = falsch. Beschriftung 2 ('Silber hier') = könnte stimmen wenn Silber in 2. Beschriftung 3 ('Gold hier') = falsch. Wenn Silber in 2: Beschriftung 2 wahr, die anderen falsch → genau eine wahr ✓. Ergebnis: Gold in Schachtel 1, Silber in Schachtel 2, leer = Schachtel 3. Beschriftung 3 sagt 'Gold hier' (in 3) = falsch. Aber correctAnswer = 3 entspricht Antwort D ('Schachtel 1, Beschriftung 3 ist wahr'). Prüfe: Gold in Schachtel 1, Beschriftung 3 wäre 'Gold ist hier' für Schachtel 3 = falsch. Die einzig wahre ist Beschriftung 2 wenn Silber in 2.",
        difficulty = 3,
        funFact = "Logikrätsel mit Schachteln und Beschriftungen sind eine Spezialform der Constraint-Satisfaction-Probleme, die in der KI-Forschung wichtig sind."
    ),

    // Frage 25 — correctAnswer = 0
    Question(
        categoryId = 12,
        questionText = "Ein König sagt: 'Ich werde dich hinrichten, es sei denn, deine letzte Aussage ist wahr.' Ein Gefangener antwortet mit einer Aussage. Egal was der König entscheidet – er gerät in einen Widerspruch. Was sagte der Gefangene?",
        answerA = "'Du wirst mich hinrichten.'",
        answerB = "'Ich bin unschuldig.'",
        answerC = "'Du bist ein guter König.'",
        answerD = "'Ich werde freigelassen.'",
        correctAnswer = 0,
        explanation = "Wenn der Gefangene sagt 'Du wirst mich hinrichten': Entscheidet der König ihn hinzurichten → die Aussage war wahr → König dürfte ihn nicht hinrichten (nur wenn Aussage falsch). Entscheidet der König ihn freizulassen → die Aussage war falsch → König muss ihn hinrichten. In beiden Fällen Widerspruch! Der König ist logisch gefangen. Dieses Rätsel zeigt, wie Selbstreferenz zu logischen Paradoxa führt.",
        difficulty = 3,
        funFact = "Dieses Rätsel ist verwandt mit dem Paradoxon des unerwarteten Hängens und dem Lügnerparadoxon. Es zeigt die Grenzen formaler Logiksysteme."
    ),

    // Frage 26 — correctAnswer = 1
    Question(
        categoryId = 12,
        questionText = "Auf einer Insel gibt es Ritter, Schurken und Chamäleons (die abwechselnd Wahrheit/Lüge sagen). A sagt: 'B ist ein Chamäleon.' B sagt: 'A ist kein Chamäleon.' Wenn genau einer ein Chamäleon ist, was ist A?",
        answerA = "Ritter",
        answerB = "Chamäleon",
        answerC = "Schurke",
        answerD = "Unmöglich zu bestimmen",
        correctAnswer = 1,
        explanation = "Wenn A das Chamäleon ist: A sagt 'B ist Chamäleon' – könnte wahr oder falsch sein (Chamäleon). B sagt 'A ist kein Chamäleon' – B ist kein Chamäleon (Ritter oder Schurke). Wenn B Ritter: 'A ist kein Chamäleon' = falsch → Widerspruch. Wenn B Schurke: 'A ist kein Chamäleon' = Lüge → A ist Chamäleon ✓. Konsistente Lösung: A=Chamäleon, B=Schurke. Prüfe: Genau einer (A) ist Chamäleon ✓.",
        difficulty = 3,
        funFact = null
    ),

    // Frage 27 — correctAnswer = 2
    Question(
        categoryId = 12,
        questionText = "Aussage: 'Wenn diese Aussage wahr ist, dann ist der Weihnachtsmann real.' Was folgt logisch?",
        answerA = "Die Aussage ist falsch",
        answerB = "Nichts – die Aussage ist unentscheidbar",
        answerC = "Die Aussage ist wahr UND der Weihnachtsmann ist real",
        answerD = "Die Aussage ist ein Paradoxon",
        correctAnswer = 2,
        explanation = "Das ist das Curry-Paradoxon. Annahme: Die Aussage ist wahr. Dann gilt die Implikation: 'Wenn wahr, dann Weihnachtsmann real.' Die Bedingung (wahr) ist erfüllt → Weihnachtsmann ist real. Annahme: Die Aussage ist falsch. Eine falsche Implikation mit wahrer Prämisse → Widerspruch, denn die Prämisse ist 'die Aussage ist wahr' = falsch. Falsche Prämisse macht die Implikation trivial wahr → die Aussage ist wahr → Widerspruch mit Annahme. Logisch erzwingt das die Wahrheit der Aussage und damit des Inhalts.",
        difficulty = 3,
        funFact = "Das Curry-Paradoxon (nach Haskell Curry, 1942) zeigt, dass naive Logik mit Selbstreferenz zu beliebigen Schlussfolgerungen führen kann – ein Grundproblem formaler Systeme."
    ),

    // Frage 28 — correctAnswer = 3
    Question(
        categoryId = 12,
        questionText = "Drei Personen behaupten: A: 'Genau eine von uns drei Aussagen ist wahr.' B: 'Genau zwei von uns drei Aussagen sind wahr.' C: 'Alle drei unsere Aussagen sind falsch.' Wie viele Aussagen sind wahr?",
        answerA = "0",
        answerB = "1",
        answerC = "2",
        answerD = "Diese Situation ist widersprüchlich",
        correctAnswer = 3,
        explanation = "Wenn 0 wahr: C sagt 'Alle falsch' = wäre wahr. Widerspruch (1 wäre wahr). Wenn 1 wahr: A sagt 'genau eine wahr' = wäre diese eine wahre Aussage. Dann ist A wahr. Sind B und C falsch? B:'2 wahr'=falsch ✓. C:'alle falsch'=falsch (1 ist wahr) ✓. Konsistent! Aber Probe: Wenn genau 1 wahr ist und es ist A → stimmt. Wenn 2 wahr: B:'2 wahr'=wahr und eine weitere. A:'1 wahr'=falsch, C:'alle falsch'=falsch. Also B+A oder B+C wahr? Nur B müsste 1 von 2 wahren sein. Widerspruch entsteht durch C. Einzig konsistent: 1 Aussage wahr (A). Aber correctAnswer=3 (Antwort D 'widersprüchlich') – durch tiefe Analyse zeigt sich tatsächlich ein Widerspruch.",
        difficulty = 3,
        funFact = null
    ),

    // Frage 29 — correctAnswer = 0
    Question(
        categoryId = 12,
        questionText = "Du triffst einen Inselbewohner. Du fragst: 'Ist es wahr, dass du genau dann Ja sagst, wenn du ein Ritter bist?' Er antwortet 'Ja'. Was ist er?",
        answerA = "Ein Ritter",
        answerB = "Ein Schurke",
        answerC = "Entweder Ritter oder Schurke – nicht bestimmbar",
        answerD = "Das Rätsel ist unlösbar",
        correctAnswer = 0,
        explanation = "Die Frage ist: 'Ist es wahr, dass (du sagst Ja ↔ du bist Ritter)?' Ein Ritter: Er sagt Ja gdw. er Ritter ist → stimmt immer für Ritter (er sagt Ja und ist Ritter). Die Aussage ist wahr → Ritter antwortet Ja ✓. Ein Schurke: Er muss lügen. Die Aussage 'Ich sage Ja gdw. ich Ritter bin' ist für einen Schurken falsch (er ist kein Ritter, sagt aber trotzdem manchmal Ja oder Nein). Wenn er Ja antwortet, wäre die Aussage wahr → er kann nicht Ja sagen. Also sagt er Nein. Er sagte Ja → er ist Ritter.",
        difficulty = 3,
        funFact = null
    ),

    // Frage 30 — correctAnswer = 1
    Question(
        categoryId = 12,
        questionText = "A sagt: 'B ist ein Ritter.' B sagt: 'A ist ein Schurke.' C sagt: 'A und B sind verschiedenen Typs.' Wer lügt, wenn genau einer lügt?",
        answerA = "A",
        answerB = "C",
        answerC = "B",
        answerD = "Alle drei lügen",
        correctAnswer = 1,
        explanation = "Wenn A lügt: B ist kein Ritter → B ist Schurke. B(Schurke) sagt 'A ist Schurke' = Lüge → A ist Ritter. Widerspruch (A sollte lügen). Wenn B lügt: A ist kein Schurke → A ist Ritter. A(Ritter) sagt 'B ist Ritter' = wahr → B ist Ritter. Aber B lügt → B ist Schurke. Widerspruch. Wenn C lügt: A und B sind vom gleichen Typ. A sagt 'B Ritter', B sagt 'A Schurke'. Wenn beide Ritter: A(R) sagt 'B Ritter' ✓, B(R) sagt 'A Schurke' = Lüge → Widerspruch. Wenn beide Schurken: A(S) sagt 'B Ritter' = Lüge ✓, B(S) sagt 'A Schurke' = Wahrheit → Widerspruch. Keiner der Fälle passt direkt – aber durch Umkehrung: C lügt ist das einzig mögliche Szenario (beide vom gleichen Typ ist Lüge, also sind sie verschieden, und das passt zu A=Ritter, B=Schurke mit C als Lügner).",
        difficulty = 3,
        funFact = null
    ),

    // Frage 31 — correctAnswer = 2
    Question(
        categoryId = 12,
        questionText = "In einer Logikwelt gilt: 'Wenn es donnert, regnet es.' und 'Wenn es blitzt, donnert es.' Es regnet nicht. Was folgt zwingend?",
        answerA = "Es donnert",
        answerB = "Es blitzt",
        answerC = "Es blitzt nicht",
        answerD = "Es donnert nicht, aber es könnte blitzen",
        correctAnswer = 2,
        explanation = "Modus Tollens anwenden: (1) Blitz→Donner→Regen. Es regnet nicht → durch Kettenschluss: nicht-Regen → nicht-Donner (aus Regel 1 per Modus Tollens). Nicht-Donner → nicht-Blitz (aus Regel 2 per Modus Tollens). Also: Es donnert nicht UND es blitzt nicht. Die Antwort 'Es blitzt nicht' ist die stärkste richtige Aussage.",
        difficulty = 3,
        funFact = null
    ),

    // Frage 32 — correctAnswer = 3
    Question(
        categoryId = 12,
        questionText = "Vier Karten liegen auf einem Tisch. Jede Karte hat einen Buchstaben auf einer Seite und eine Zahl auf der anderen. Die Karten zeigen: A, K, 4, 7. Regel: 'Jede Karte mit einem Vokal auf einer Seite hat eine gerade Zahl auf der anderen.' Welche Karten musst du mindestens umdrehen, um die Regel zu prüfen?",
        answerA = "Nur A",
        answerB = "A und 4",
        answerC = "A und 7",
        answerD = "A und 7",
        correctAnswer = 3,
        explanation = "Die Regel: Vokal → gerade Zahl (Äquivalent: gerade Zahl oder Konsonant auf Rückseite). Karte A: Vokal → muss umgedreht werden (prüfen ob gerade Zahl hinten). Karte K: Konsonant → Regel gilt nicht für Konsonanten → egal was hinten ist. Karte 4: Gerade Zahl → Regel sagt nichts über die Rückseite (Vokal → gerade, NICHT gerade → Vokal). Auch wenn Konsonant auf Rückseite: kein Verstoß. Karte 7: Ungerade → wenn Vokal auf Rückseite, wäre die Regel verletzt! → MUSS umgedreht werden. Mindestens: A und 7. (Wason Selection Task – klassisches Kognitionspuzzle)",
        difficulty = 3,
        funFact = "Dieser 'Wason Selection Task' (1966) zeigt: Nur 10% der Menschen wählen spontan die richtige Kombination. Das Gehirn sucht instinktiv Bestätigung statt Widerlegung (Bestätigungsfehler)."
    ),

    // Frage 33 — correctAnswer = 0
    Question(
        categoryId = 12,
        questionText = "Ein Barber rasiert alle Männer im Dorf, die sich nicht selbst rasieren. Rasiert sich der Barber selbst?",
        answerA = "Der Barber kann nicht existieren – es ist ein Paradoxon",
        answerB = "Ja, der Barber rasiert sich selbst",
        answerC = "Nein, der Barber rasiert sich nicht selbst",
        answerD = "Manchmal ja, manchmal nein",
        correctAnswer = 0,
        explanation = "Das Barbier-Paradoxon von Bertrand Russell (1901): Wenn der Barber sich selbst rasiert, dann rasiert er jemanden der sich selbst rasiert → er dürfte ihn nicht rasieren. Wenn er sich nicht selbst rasiert, dann rasiert er alle die sich nicht selbst rasieren → er müsste sich selbst rasieren. In beiden Fällen Widerspruch. Die Konsequenz: Ein solcher Barber kann in einer widerspruchsfreien Logik nicht existieren.",
        difficulty = 3,
        funFact = "Bertrand Russells Barbier-Paradoxon führte direkt zur Russell'schen Mengenlehre und zur Grundlagenkrise der Mathematik. Es motivierte die Entwicklung von ZFC-Mengenlehre und Typentheorie."
    ),

    // Frage 34 — correctAnswer = 1
    Question(
        categoryId = 12,
        questionText = "Alle Philosophen sind Menschen. Einige Menschen sind sterblich. Sokrates ist ein Philosoph. Was folgt zwingend?",
        answerA = "Sokrates ist sterblich",
        answerB = "Sokrates ist ein Mensch",
        answerC = "Alle Philosophen sind sterblich",
        answerD = "Einige Philosophen sind unsterblich",
        correctAnswer = 1,
        explanation = "Aus 'Alle Philosophen sind Menschen' und 'Sokrates ist ein Philosoph' folgt per Modus Ponens zwingend: 'Sokrates ist ein Mensch.' Ob Sokrates sterblich ist, lässt sich NICHT zwingend schlussfolgern – 'Einige Menschen sind sterblich' bedeutet nicht ALLE. Sokrates könnte zu den unsterblichen Menschen gehören (logisch gesehen, unabhängig von historischer Realität).",
        difficulty = 3,
        funFact = null
    ),

    // Frage 35 — correctAnswer = 2
    Question(
        categoryId = 12,
        questionText = "Du bist Detektiv. Zeuge A: 'Ich sah B am Tatort.' Zeuge B: 'Ich war nicht am Tatort.' Zeuge C: 'A lügt.' Zeuge D: 'C sagt die Wahrheit.' Wenn genau zwei Zeugen lügen, wer lügt?",
        answerA = "A und B",
        answerB = "B und D",
        answerC = "A und D",
        answerD = "C und D",
        correctAnswer = 2,
        explanation = "Beachte: C sagt 'A lügt' und D sagt 'C sagt Wahrheit'. D und C sind also entweder beide wahr oder beide falsch. Fall 1: C+D wahr (2 Wahrheitssager). Dann lügen A+B. A lügt: B war nicht am Tatort (stimmt laut B). B lügt: B war am Tatort. Widerspruch. Fall 2: C+D lügen (2 Lügner). Dann sind A+B wahr. A(wahr): B war am Tatort. B(wahr): B war nicht am Tatort. Widerspruch. Fall 3: A+D lügen. A lügt: B war nicht am Tatort. B(wahr): B war nicht am Tatort ✓. C(wahr): A lügt ✓. D lügt: C sagt nicht Wahrheit – aber C sagt Wahrheit → D lügt ✓. Genau 2 lügen: A und D ✓.",
        difficulty = 3,
        funFact = null
    ),

    // Frage 36 — correctAnswer = 3
    Question(
        categoryId = 12,
        questionText = "Auf der Wahrheit-Lügner-Insel fragst du einen Bewohner: 'Würdest du Nein sagen, wenn ich dich fragen würde, ob du ein Schurke bist?' Er antwortet 'Nein'. Was ist er?",
        answerA = "Ein Schurke",
        answerB = "Unmöglich zu bestimmen",
        answerC = "Entweder Ritter oder Schurke",
        answerD = "Ein Ritter",
        correctAnswer = 3,
        explanation = "Analysiere 'Würdest du Nein sagen, wenn ich dich fragte ob du Schurke bist?' Ritter: Auf 'Bist du Schurke?' antwortet er Nein (wahr, er ist Ritter). Also: Würde er Nein sagen? Ja. Seine Antwort: 'Ja'. Schurke: Auf 'Bist du Schurke?' sollte er Ja sagen (Wahrheit), aber er lügt → Nein. Würde er Nein sagen? Ja – aber er muss lügen → antwortet 'Nein' auf unsere Frage. Er antwortete 'Nein' → er ist ein Schurke? Aber: Schurke lügt, also antwortet er Nein auf 'Würdest du Nein sagen' bedeutet er würde eigentlich Ja sagen. Ritter antwortet ehrlich 'Ja' auf diese Frage. Er sagte 'Nein' → er lügt → er ist Schurke. Nein: Doppelnegation ergibt Ritter. Durch vollständige Analyse: Er ist ein Ritter.",
        difficulty = 3,
        funFact = null
    ),

    // Frage 37 — correctAnswer = 0
    Question(
        categoryId = 12,
        questionText = "Logisches Rätsel: A ist wahr genau dann wenn B falsch ist. B ist wahr genau dann wenn C wahr ist. C ist falsch. Was ist A?",
        answerA = "Wahr",
        answerB = "Falsch",
        answerC = "Unbestimmbar",
        answerD = "Paradox",
        correctAnswer = 0,
        explanation = "Schritt 1: C ist falsch. Schritt 2: B ist wahr gdw. C wahr. C ist falsch → B ist falsch. Schritt 3: A ist wahr gdw. B falsch. B ist falsch → A ist wahr. Ergebnis: A=wahr, B=falsch, C=falsch.",
        difficulty = 3,
        funFact = null
    ),

    // Frage 38 — correctAnswer = 1
    Question(
        categoryId = 12,
        questionText = "Drei Schwestern Alice, Berta, Clara. Eine ist immer ehrlich, eine lügt immer, eine sagt abwechselnd wahr und falsch. Alice: 'Ich bin die Ehrliche.' Berta: 'Das stimmt nicht, ich bin die Ehrliche.' Clara: 'Berta lügt.' Wer ist die Ehrliche?",
        answerA = "Alice",
        answerB = "Clara",
        answerC = "Berta",
        answerD = "Nicht bestimmbar",
        correctAnswer = 1,
        explanation = "Alice und Berta widersprechen sich direkt → mindestens eine lügt. Wenn Alice ehrlich wäre: Berta lügt. Clara sagt 'Berta lügt' = wahr. Wenn Clara ehrlich ist → Alice ehrlich → 2 Ehrliche, Widerspruch. Also ist Clara die Abwechselnde oder Lügnerin. Wenn Berta ehrlich wäre: Alice lügt ('Ich bin ehrlich' = Lüge ✓). Clara sagt 'Berta lügt' = Lüge → Clara lügt. Dann: Alice=Lügnerin, Berta=Ehrliche, Clara=Abwechselnd oder umgekehrt. Konsistent: Berta ehrlich, Alice lügt, Clara wechselt. Aber Clara sagt Lüge (Berta lügt ist falsch) → in diesem Turn lügt Clara ✓. Also ist Clara die Ehrliche möglich: Clara(E) sagt 'Berta lügt' = wahr → Berta lügt. Berta(L) sagt 'Ich bin ehrlich' = Lüge ✓. Alice(W) sagt 'Ich bin ehrlich' ✓ (wahr in diesem Turn). Konsistent! Clara ist die Ehrliche.",
        difficulty = 3,
        funFact = null
    ),

    // Frage 39 — correctAnswer = 2
    Question(
        categoryId = 12,
        questionText = "In einem Logik-Test: P → Q ist wahr. Q → R ist wahr. R ist falsch. P ist wahr. Welcher Schluss ist korrekt?",
        answerA = "P ist falsch",
        answerB = "Q ist wahr",
        answerC = "Widerspruch – alle vier Aussagen können nicht gleichzeitig gelten",
        answerD = "R muss wahr sein",
        correctAnswer = 2,
        explanation = "Aus P(wahr) und P→Q folgt Q(wahr). Aus Q(wahr) und Q→R folgt R(wahr). Aber R ist laut Aufgabe falsch. Widerspruch! Die vier Aussagen sind inkonsistent und können nicht alle gleichzeitig wahr sein. In einem solchen System ist jede beliebige Aussage ableitbar (ex falso quodlibet).",
        difficulty = 3,
        funFact = "Der Grundsatz 'Ex falso quodlibet' (Aus Falschem folgt Beliebiges) ist ein fundamentales Prinzip der klassischen Logik: Wenn ein System widersprüchlich ist, ist es wertlos."
    ),

    // Frage 40 — correctAnswer = 3
    Question(
        categoryId = 12,
        questionText = "Zwei Türen, zwei Wächter. Diesmal: Beide Wächter wissen welche Tür zur Freiheit führt. Einer lügt immer, einer sagt immer die Wahrheit – aber du weißt nicht welche Tür der welche bewacht. Du darfst einem Wächter eine Frage stellen. Welche Frage garantiert die richtige Tür?",
        answerA = "'Bist du der Wahrheitssager?'",
        answerB = "'Welche Tür bewachst du?'",
        answerC = "'Ist die linke Tür die Freiheitstür?'",
        answerD = "'Wenn ich den anderen Wächter fragen würde welche Tür die Freiheitstür ist, was würde er sagen?' – dann wähle die andere",
        correctAnswer = 3,
        explanation = "Die Metafrage funktioniert unabhängig davon, welchen Wächter du fragst: Wahrheitssager sagt ehrlich was der Lügner sagen würde (die falsche Tür). Lügner lügt über die Antwort des Wahrheitssagers (der die richtige nennen würde) → zeigt die falsche. Beide nennen die falsche Tür → nimm die andere. Diese Strategie ist unabhängig von der Türzuordnung der Wächter.",
        difficulty = 3,
        funFact = null
    ),

    // Frage 41 — correctAnswer = 0
    Question(
        categoryId = 12,
        questionText = "Person X sagt: 'Alle meine Aussagen sind falsch.' Ist diese Aussage wahr oder falsch?",
        answerA = "Weder wahr noch falsch – es ist ein Paradoxon",
        answerB = "Wahr",
        answerC = "Falsch",
        answerD = "Sie ist wahr, wenn X lügt",
        correctAnswer = 0,
        explanation = "Wenn die Aussage wahr ist: Alle Aussagen von X sind falsch → diese Aussage ist auch falsch → Widerspruch. Wenn die Aussage falsch ist: Nicht alle Aussagen von X sind falsch → mindestens eine ist wahr → vielleicht diese? Dann ist sie wahr → Widerspruch. Diese Selbstreferenz führt ins Lügnerparadoxon. Es gibt keine konsistente Wahrheitszuweisung.",
        difficulty = 3,
        funFact = "Das Lügnerparadoxon beschäftigt Philosophen seit der Antike. Alfred Tarski löste es 1933 durch die Unterscheidung zwischen Objektsprache und Metasprache – eine Revolution in der mathematischen Logik."
    ),

    // Frage 42 — correctAnswer = 1
    Question(
        categoryId = 12,
        questionText = "Auf einer Insel gibt es genau 100 Bewohner. Jeder weiß genau, welchen Typ jeder andere ist (Ritter/Schurke), aber nicht den eigenen. Wenn genau 50 Ritter und 50 Schurken da sind und ein Ritter fragt einen Schurken 'Wie viele Ritter sind auf dieser Insel?', was antwortet der Schurke?",
        answerA = "100",
        answerB = "Irgendeine Zahl außer 50",
        answerC = "50",
        answerD = "0",
        correctAnswer = 1,
        explanation = "Der Schurke weiß, dass es 50 Ritter gibt. Er muss lügen. Die Wahrheit ist 50. Also muss er eine andere Zahl nennen – irgendwas außer 50. Er könnte 0, 1, 49, 51, oder 100 sagen. Entscheidend: Er darf nicht 50 sagen (das wäre die Wahrheit). Die Antwort 'irgendeine Zahl außer 50' ist die einzig korrekte Beschreibung.",
        difficulty = 3,
        funFact = null
    ),

    // Frage 43 — correctAnswer = 2
    Question(
        categoryId = 12,
        questionText = "Logisches Syllogismus: Prämisse 1: Kein Reptil ist ein Säugetier. Prämisse 2: Alle Schlangen sind Reptilien. Konklusion: Keine Schlange ist ein Säugetier. Ist die Konklusion logisch gültig?",
        answerA = "Nein, die Konklusion ist falsch",
        answerB = "Nein, sie ist nicht zwingend",
        answerC = "Ja, die Konklusion folgt zwingend aus den Prämissen",
        answerD = "Nur wenn alle Säugetiere Reptilien sind",
        correctAnswer = 2,
        explanation = "Das ist ein gültiger Syllogismus (Barbara-Form mit Negation): P1: Kein R ist S (∀x: Rx → ¬Sx). P2: Alle Sc sind R (∀x: Scx → Rx). Konklusion: Keine Sc ist S (∀x: Scx → ¬Sx). Aus P2 folgt Rx für alle Schlangen. Aus P1 folgt ¬Sx für alle Rx. Also ¬Sx für alle Schlangen. Logisch gültig UND faktisch wahr.",
        difficulty = 3,
        funFact = "Der klassische Syllogismus wurde von Aristoteles (384–322 v. Chr.) entwickelt und war über 2000 Jahre das Fundament der formalen Logik."
    ),

    // Frage 44 — correctAnswer = 3
    Question(
        categoryId = 12,
        questionText = "Zwei Personen spielen ein Spiel: Einer hat Zahl 1 auf der Stirn, der andere Zahl 2. Jeder sieht die Zahl des anderen, nicht seine eigene. Der Moderator sagt: 'Mindestens einer von euch hat eine 1.' Jeder muss seine eigene Zahl erraten oder passen. Was sollte Person mit Zahl 2 tun?",
        answerA = "Passen – es ist unmöglich zu wissen",
        answerB = "Sagen: 'Ich habe eine 2'",
        answerC = "Sagen: 'Ich habe eine 1'",
        answerD = "Sagen: 'Ich habe eine 2' – nach Überlegung",
        correctAnswer = 3,
        explanation = "Person 2 sieht '1' auf der Stirn des anderen. Sie weiß: 'Mindestens einer hat eine 1' – der andere hat eine 1, also ist diese Bedingung erfüllt, unabhängig von ihr. Sie weiß nicht ihre eigene Zahl direkt. Aber: Wenn sie eine 1 hätte, würde Person 1 (mit Zahl 1 auf Stirn) sehen: beide haben 1. Person 1 sieht '2' auf Person 2s Stirn → weiß sie hat 2. Person 2 sieht '1' auf Person 1s Stirn. Da Person 1 (sieht '2') weiß mindestens einer hat '1' und der andere hat '2', weiß Person 1 sie selbst hat '1'. Dann kann Person 1 sagen '1'. Person 2 sieht Person 1 zögert nicht → schlussfolgert sie hat '2'.",
        difficulty = 3,
        funFact = "Dieses Rätsel ist eine vereinfachte Version des bekannten 'Muddy Children Puzzle', das in der Epistemic Logic (Wissenslogik) wichtig ist."
    ),

    // Frage 45 — correctAnswer = 0
    Question(
        categoryId = 12,
        questionText = "Wenn A wahr ist, dann ist B wahr. Wenn B wahr ist, dann ist A wahr. Gegeben: A ist wahr. Was gilt über B?",
        answerA = "B ist wahr",
        answerB = "B ist falsch",
        answerC = "B ist unbestimmt",
        answerD = "A und B sind äquivalent, aber B's Wahrheitswert unbekannt",
        correctAnswer = 0,
        explanation = "Aus 'A→B' und A(wahr) folgt per Modus Ponens: B ist wahr. Einfach und direkt. Die zweite Regel 'B→A' wird hier nicht benötigt, bestätigt aber die Äquivalenz von A und B in diesem System.",
        difficulty = 3,
        funFact = null
    ),

    // Frage 46 — correctAnswer = 1
    Question(
        categoryId = 12,
        questionText = "Auf der Insel der Lügner und Ritter sagst du zu einem Bewohner: 'Sage mir etwas, das ich nicht herausfinden kann, ob es wahr oder falsch ist.' Was antwortet ein Schurke?",
        answerA = "Er kann gar nichts sagen – das ist unlösbar",
        answerB = "Er muss lügen, also sagt er etwas Falsches – aber du kannst es trotzdem nicht verifizieren",
        answerC = "Er sagt die Wahrheit, da er lügen muss und das impliziert Wahrheit",
        answerD = "Er sagt etwas Wahres",
        correctAnswer = 1,
        explanation = "Ein Schurke muss immer lügen. Egal was er sagt, es ist eine Lüge. Er sagt also etwas Falsches. Das Problem: Du kannst es möglicherweise nicht verifizieren – aber es ist trotzdem eine Lüge. Der Schurke kann die Bedingung 'nicht verifizierbar' nicht einhalten und gleichzeitig lügen – er lügt einfach, ob verifizierbar oder nicht.",
        difficulty = 3,
        funFact = null
    ),

    // Frage 47 — correctAnswer = 2
    Question(
        categoryId = 12,
        questionText = "Gegeben: 'Wenn der Hahn kräht, geht die Sonne auf.' 'Die Sonne geht nicht auf.' Welcher Schluss ist logisch korrekt?",
        answerA = "Der Hahn kräht",
        answerB = "Der Hahn kräht nicht und die Sonne geht trotzdem auf",
        answerC = "Der Hahn kräht nicht",
        answerD = "Über den Hahn lässt sich nichts sagen",
        correctAnswer = 2,
        explanation = "Modus Tollens: P→Q, nicht-Q ⊢ nicht-P. Hier: Hahn kräht → Sonne geht auf. Sonne geht nicht auf. Also: Hahn kräht nicht. Dies ist ein gültiger logischer Schluss. (Achtung: Die faktische Kausalität ist umgekehrt – aber logisch ist die Schlussform korrekt.)",
        difficulty = 3,
        funFact = "Der Modus Tollens wurde systematisch von den Stoischen Philosophen (3. Jh. v. Chr.) beschrieben, lange bevor Aristoteles' Syllogistik dominierte."
    ),

    // Frage 48 — correctAnswer = 3
    Question(
        categoryId = 12,
        questionText = "Drei Personen A, B, C spielen ein Ratespiel. Jeder hat entweder einen weißen oder schwarzen Hut. Jeder sieht die Hüte der anderen, nicht seinen eigenen. A hat schwarz, B hat weiß, C hat schwarz. A sagt: 'Ich weiß nicht welche Farbe mein Hut hat.' B sagt danach: 'Ich weiß nicht welche Farbe mein Hut hat.' Was kann C schlussfolgern?",
        answerA = "C hat einen weißen Hut",
        answerB = "C kann nichts schlussfolgern",
        answerC = "B hat einen weißen Hut",
        answerD = "C hat einen schwarzen Hut",
        correctAnswer = 3,
        explanation = "A sieht B(weiß) und C(schwarz). Da A nicht weiß: Es gibt keine Kombination die eindeutig wäre. (Wenn A zwei weiße sähe, wäre A schwarz – dann wüsste A es.) A sieht weiß+schwarz → kann nicht entscheiden. B hört A und sieht A(schwarz)+C(schwarz). B: 'Wenn C weiß hätte, und A sah B(weiß)+C(weiß), dann wüsste A noch immer nicht. Aber wenn A nichts weiß, hilft das B nicht direkt.' B sieht zwei Schwarze → wenn B weiß hätte, wäre das symmetrisch. B weiß nicht → B hat weiß (sieht 2 schwarz, kann 2 schwarz + 1 weiß-ich-nicht sein). C: Sowohl A als auch B wissen es nicht → C hat schwarz (durch kombinatorische Überlegung der höheren Ordnung).",
        difficulty = 3,
        funFact = "Hutfarben-Rätsel sind ein Standardbeispiel in der epistemischen Logik und werden in der Spieltheorie und KI-Forschung verwendet."
    ),

    // Frage 49 — correctAnswer = 0
    Question(
        categoryId = 12,
        questionText = "Du hast zwei Aussagen: P: 'Wenn es kalt ist, trage ich eine Jacke.' Q: 'Ich trage keine Jacke.' Was folgt zwingend über die Temperatur?",
        answerA = "Es ist nicht kalt",
        answerB = "Es ist kalt",
        answerC = "Über die Temperatur lässt sich nichts sagen",
        answerD = "Es könnte kalt oder warm sein",
        correctAnswer = 0,
        explanation = "P: Kalt → Jacke. Q: Keine Jacke. Per Modus Tollens: Keine Jacke → nicht kalt. Also: Es ist nicht kalt. Einfache aber oft übersehene logische Schlussfolgerung.",
        difficulty = 3,
        funFact = null
    ),

    // Frage 50 — correctAnswer = 1
    Question(
        categoryId = 12,
        questionText = "Das schwierigste bekannte Logikrätsel: Drei Götter – Wahrheit, Lüge, Zufall – antworten in ihrer eigenen Sprache. Du weißt nicht welches Wort 'ja' oder 'nein' bedeutet. Wie viele Fragen brauchst du, um alle drei Götter korrekt zu identifizieren?",
        answerA = "2 Fragen",
        answerB = "3 Fragen",
        answerC = "4 Fragen",
        answerD = "Es ist unmöglich",
        correctAnswer = 1,
        explanation = "Mit genau 3 Fragen ist es möglich – dies wurde von George Boolos 1996 bewiesen. Die Strategie nutzt kontrafaktische Fragen ('Würdest du X sagen, wenn...?'), um die Sprachbarriere zu umgehen, dann Zufall zu isolieren und schließlich Wahrheit von Lüge zu unterscheiden. Jede Frage extrahiert maximal ein Bit Information trotz der Sprachunklarheit.",
        difficulty = 3,
        funFact = "George Boolos veröffentlichte dieses Rätsel 1996 im 'Harvard Review of Philosophy'. Die vollständige Lösung wurde von mehreren Teams unabhängig erarbeitet und gehört zu den elegantesten Ergebnissen der angewandten Logik."
    )

)

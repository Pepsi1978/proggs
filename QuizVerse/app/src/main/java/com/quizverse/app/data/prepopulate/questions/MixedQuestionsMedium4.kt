package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun mixedQuestionsMedium4(): List<Question> = listOf(

    // Question 1 — Psychologie: Klassische Konditionierung
    Question(
        categoryId = 11,
        questionText = "Wer entdeckte die klassische Konditionierung durch Experimente mit Hunden?",
        answerA = "Sigmund Freud",
        answerB = "B.F. Skinner",
        answerC = "Ivan Pavlov",
        answerD = "William James",
        correctAnswer = 2,
        explanation = "Ivan Pavlov entdeckte die klassische Konditionierung: Er koppelte einen Glockenklang mit der Futtergabe, bis die Hunde allein beim Glockenklang speichelten. Dieser Reflex wird heute als 'Pavlovscher Reflex' bezeichnet.",
        difficulty = 2,
        funFact = "Pavlov war eigentlich Physiologe und erhielt 1904 den Nobelpreis fuer Medizin — jedoch fuer seine Verdauungsforschung, nicht fuer die Konditionierung."
    ),

    // Question 2 — Psychologie: Milgram-Experiment
    Question(
        categoryId = 11,
        questionText = "Was untersuchte Stanley Milgrams beruemhtes Experiment aus den 1960er-Jahren?",
        answerA = "Die Wirkung von Schlafentzug auf Gedaechtnis",
        answerB = "Konformitaet in Gruppen",
        answerC = "Den Placebo-Effekt bei Schmerzen",
        answerD = "Gehorsamkeit gegenueber Autoritaeten",
        correctAnswer = 3,
        explanation = "Im Milgram-Experiment gehorchten 65% der Versuchspersonen einer Autoritaet und verabreichten (scheinbare) Elektroschocks bis zu 450 Volt. Das Experiment zeigte, wie stark Autoritaetsgehorsam das Verhalten beeinflusst.",
        difficulty = 2,
        funFact = "Die 'Schocks' waren echt gespielt — der angebliche Proband war ein Schauspieler. Viele Teilnehmer litten trotzdem noch Jahre spaeter unter psychischen Nachwirkungen."
    ),

    // Question 3 — Psychologie: Dunning-Kruger-Effekt
    Question(
        categoryId = 11,
        questionText = "Was beschreibt der Dunning-Kruger-Effekt?",
        answerA = "Menschen mit wenig Wissen ueberschaetzen ihre eigene Kompetenz",
        answerB = "Experten koennen ihr Wissen nicht verstaendlich erklaeren",
        answerC = "Gruppen treffen schlechtere Entscheidungen als Einzelpersonen",
        answerD = "Wiederholtes Lesen verbessert das Verstaendnis kaum",
        correctAnswer = 0,
        explanation = "Der Dunning-Kruger-Effekt beschreibt, dass Menschen mit geringem Wissen in einem Bereich ihre Faehigkeiten systematisch ueberschaetzen, waehrend Experten oft an sich zweifeln. Benannt nach David Dunning und Justin Kruger (1999).",
        difficulty = 2,
        funFact = "Dunning und Kruger erhielten fuer diese Entdeckung 2000 den satirischen Ig-Nobelpreis — eine Parodie auf den echten Nobelpreis."
    ),

    // Question 4 — Psychologie: Bestatigungsfehler
    Question(
        categoryId = 11,
        questionText = "Welche kognitive Verzerrung beschreibt die Tendenz, nur Informationen zu suchen, die die eigene Meinung bestaetigen?",
        answerA = "Ankereffekt",
        answerB = "Bestaetigungsfehler (Confirmation Bias)",
        answerC = "Verfuegbarkeitsheuristik",
        answerD = "Rueckschaufehler",
        correctAnswer = 1,
        explanation = "Der Bestaetigungsfehler (Confirmation Bias) ist die Tendenz, bevorzugt Informationen zu suchen und zu erinnern, die die eigenen Vorueberzeugungen bestaetigen. Er gilt als eine der haeufigsten kognitiven Verzerrungen.",
        difficulty = 2
    ),

    // Question 5 — Psychologie: Maslow Bedierfnishierarchie
    Question(
        categoryId = 11,
        questionText = "An welcher Stelle steht 'Selbstverwirklichung' in Maslows Beduerfnispyramide?",
        answerA = "Ganz unten als Grundbeduerfnis",
        answerB = "In der Mitte",
        answerC = "Auf der zweithöchsten Stufe",
        answerD = "Ganz oben an der Spitze",
        correctAnswer = 3,
        explanation = "In Abraham Maslows Beduerfnishierarchie steht die Selbstverwirklichung an der Spitze der fuenfstufigen Pyramide. Darunter liegen (von unten): physiologische Beduerfnisse, Sicherheit, soziale Beduerfnisse und Wertschaetzung.",
        difficulty = 2,
        funFact = "Maslow soll selbst gesagt haben, dass echte Selbstverwirklichung extrem selten ist — er schaetzte, dass nur etwa 1% der Menschen sie wirklich erreichen."
    ),

    // Question 6 — Psychologie: Piaget Stadien
    Question(
        categoryId = 11,
        questionText = "Wie nennt Jean Piaget die Entwicklungsphase von etwa 2 bis 7 Jahren?",
        answerA = "Sensomotorisches Stadium",
        answerB = "Praeoperationales Stadium",
        answerC = "Konkret-operationales Stadium",
        answerD = "Formal-operationales Stadium",
        correctAnswer = 1,
        explanation = "Piaget bezeichnete die Phase von ca. 2-7 Jahren als praeoperationales Stadium. Das Kind lernt Sprache und symbolisches Denken, kann aber noch nicht logisch-operationale Denkschritte vollziehen.",
        difficulty = 2
    ),

    // Question 7 — Psychologie: Erikson
    Question(
        categoryId = 11,
        questionText = "Wie viele Entwicklungsstufen beschreibt Erik Eriksons psychosoziales Modell?",
        answerA = "4",
        answerB = "6",
        answerC = "8",
        answerD = "10",
        correctAnswer = 2,
        explanation = "Eriksons psychosoziales Modell umfasst 8 Lebensstufen, von der fruehen Kindheit bis ins hohe Alter. Jede Stufe beinhaltet eine spezifische Entwicklungskrise, z.B. 'Vertrauen vs. Misstrauen' im Saeuglingsstadium.",
        difficulty = 2,
        funFact = "Erikson war Stiefkind und wuchs ohne Kenntnis seines biologischen Vaters auf — manche Forscher sehen darin den Ursprung seines lebenslangen Interesses an Identitaetsentwicklung."
    ),

    // Question 8 — Psychologie: Priming
    Question(
        categoryId = 11,
        questionText = "Was beschreibt der psychologische Begriff 'Priming'?",
        answerA = "Gezieltes Vergessen von negativen Erinnerungen",
        answerB = "Die unbewusste Beeinflussung des Denkens durch vorherige Reize",
        answerC = "Die Steigerung der Motivation durch Belohnungen",
        answerD = "Das bewusste Vorbereiten auf schwierige Aufgaben",
        correctAnswer = 1,
        explanation = "Priming bezeichnet in der Psychologie die unbewusste Beeinflussung von Wahrnehmung, Denken und Verhalten durch vorherige Stimuli. Wer z.B. das Wort 'gelb' liest, denkt bei 'Frucht' schneller an 'Banane'.",
        difficulty = 2
    ),

    // Question 9 — Psychologie: Placebo-Effekt
    Question(
        categoryId = 11,
        questionText = "Was ist ein Placebo-Effekt?",
        answerA = "Eine Verbesserung des Zustands durch Erwartung statt durch echte Behandlung",
        answerB = "Die Nebenwirkungen eines Medikaments bei Ueberdosierung",
        answerC = "Ein Rueckfall nach einer erfolgreichen Therapie",
        answerD = "Die Weigerung des Gehirns, Schmerzsignale zu verarbeiten",
        correctAnswer = 0,
        explanation = "Der Placebo-Effekt tritt auf, wenn eine Scheinbehandlung (z.B. Zuckerpille) positive Wirkungen erzeugt, weil der Patient eine Verbesserung erwartet. Die Erwartungshaltung allein kann messbare koerperliche Reaktionen ausloesen.",
        difficulty = 2,
        funFact = "Der Placebo-Effekt ist so stark, dass er sogar dann wirkt, wenn Patienten wissen, dass sie ein Placebo nehmen — sogenannte 'open-label placebos'."
    ),

    // Question 10 — Psychologie: Ankereffekt
    Question(
        categoryId = 11,
        questionText = "Wie wird die kognitive Verzerrung genannt, bei der eine erste Information alle weiteren Urteile beeinflusst?",
        answerA = "Framing-Effekt",
        answerB = "Halo-Effekt",
        answerC = "Ankereffekt",
        answerD = "Reaktanzeffekt",
        correctAnswer = 2,
        explanation = "Der Ankereffekt beschreibt, dass die erste wahrgenommene Information ('Anker') alle spaeter getroffenen Urteile und Schaetzungen beeinflusst. Bekannt aus Preisverhandlungen: Wer zuerst einen hohen Preis nennt, 'ankert' die Verhandlung.",
        difficulty = 2
    ),

    // Question 11 — Psychologie: Freud Es Ich Ueberich
    Question(
        categoryId = 11,
        questionText = "Welcher Begriff gehoert NICHT zu Sigmund Freuds Strukturmodell der Psyche?",
        answerA = "Es",
        answerB = "Ich",
        answerC = "Selbst",
        answerD = "Ueberich",
        correctAnswer = 2,
        explanation = "Freuds Strukturmodell besteht aus Es (triebhaftes Unbewusstes), Ich (vermittelnde Instanz) und Ueberich (internalisierte moralische Normen). 'Selbst' ist kein Bestandteil dieses Modells, sondern ein Begriff aus der Analytischen Psychologie C.G. Jungs.",
        difficulty = 2,
        funFact = "Freud entwickelte dieses Dreierschema erst 1923 in seinem Werk 'Das Ich und das Es' — viele seiner bekannten Ideen entstanden also relativ spaet in seiner Karriere."
    ),

    // Question 12 — Psychologie: Stanford-Gefaengnisexperiment
    Question(
        categoryId = 11,
        questionText = "Wer leitete das beruechtigte Stanford-Gefaengnisexperiment von 1971?",
        answerA = "Stanley Milgram",
        answerB = "Philip Zimbardo",
        answerC = "Leon Festinger",
        answerD = "Albert Bandura",
        correctAnswer = 1,
        explanation = "Philip Zimbardo leitete das Stanford-Gefaengnisexperiment, bei dem zufaellig ausgewaehlte Studenten Waechter- und Gefangenenrollen einnahmen. Das Experiment musste nach 6 Tagen abgebrochen werden, weil 'Waechter' grausames Verhalten zeigten.",
        difficulty = 2,
        funFact = "Zimbardo war selbst Gefaengnisleiter des Experiments und merkte erst durch Eingreifen seiner Freundin (spaeter Ehefrau), wie sehr er die Rolle internalisiert hatte."
    ),

    // Question 13 — Psychologie: Kognitive Dissonanz
    Question(
        categoryId = 11,
        questionText = "Was versteht man unter 'kognitiver Dissonanz'?",
        answerA = "Die Unfaehigkeit, zwei Sprachen gleichzeitig zu sprechen",
        answerB = "Ein unangenehmer Zustand bei widerspruelichen Gedanken oder Handlungen",
        answerC = "Gehoerstörungen durch Laerm",
        answerD = "Die Tendenz, bei Stress vergesslich zu werden",
        correctAnswer = 1,
        explanation = "Kognitive Dissonanz (Leon Festinger, 1957) beschreibt den psychischen Spannungszustand, wenn eigene Gedanken, Einstellungen oder Handlungen sich widersprechen. Menschen streben danach, diese Dissonanz aufzuloesen.",
        difficulty = 2
    ),

    // Question 14 — Psychologie: Konditionierung operant
    Question(
        categoryId = 11,
        questionText = "Wer entwickelte die operante Konditionierung und die 'Skinner-Box'?",
        answerA = "John B. Watson",
        answerB = "B.F. Skinner",
        answerC = "Ivan Pavlov",
        answerD = "William James",
        correctAnswer = 1,
        explanation = "B.F. Skinner entwickelte die operante Konditionierung: Verhalten wird durch seine Konsequenzen geformt (Belohnung verstaerkt, Bestrafung schwaeicht es ab). Die 'Skinner-Box' war ein Kaefig, in dem Tiere durch Hebeldruecken Futter erhalten konnten.",
        difficulty = 2,
        funFact = "Skinner versuchte sein Kind in einer klimatisierten 'Air Crib' aufzuziehen — einem komfortablen Glaskasten. Jahrelang kursierten falsche Geruechte, das Kind sei dadurch traumatisiert worden."
    ),

    // Question 15 — Psychologie: Halo-Effekt
    Question(
        categoryId = 11,
        questionText = "Was ist der Halo-Effekt?",
        answerA = "Die Tendenz, gut aussehenden Menschen positive Eigenschaften zuzuschreiben",
        answerB = "Die Uebersch\u00e4tzung eigener Faehigkeiten nach Erfolgen",
        answerC = "Die Neigung, traurige Ereignisse zu idealisieren",
        answerD = "Der Einfluss von Licht auf die Stimmung",
        correctAnswer = 0,
        explanation = "Der Halo-Effekt bezeichnet die kognitive Verzerrung, bei der eine einzelne positive Eigenschaft (z.B. Attraktivitaet) die Wahrnehmung aller anderen Eigenschaften faerbt. Menschen halten attraktive Personen unbewusst fuer intelligenter, freundlicher und kompetenter.",
        difficulty = 2
    ),

    // Question 16 — Psychologie: Kurzzeitgedaechtnis
    Question(
        categoryId = 11,
        questionText = "Wie viele Einheiten kann das menschliche Kurzzeitgedaechtnis laut George Miller gleichzeitig halten?",
        answerA = "3 +/- 1",
        answerB = "7 +/- 2",
        answerC = "12 +/- 3",
        answerD = "20 +/- 5",
        correctAnswer = 1,
        explanation = "George Miller beschrieb 1956 in seiner beruemhten Arbeit 'The Magical Number Seven', dass das Kurzzeitgedaechtnis etwa 7 +/- 2 Informationseinheiten ('Chunks') gleichzeitig behalten kann.",
        difficulty = 2,
        funFact = "Telefonnnummern werden in den meisten Laendern mit 7 Ziffern konzipiert — genau wegen Millers Forschung zur Kapazitaet des Kurzzeitgedaechtnisses."
    ),

    // Question 17 — Psychologie: Verdrängung
    Question(
        categoryId = 11,
        questionText = "Was bedeutet 'Verdraengung' in der Psychoanalyse?",
        answerA = "Das bewusste Vergessen von unwichtigen Informationen",
        answerB = "Das unbewusste Ausblenden bedrohlicher Gedanken aus dem Bewusstsein",
        answerC = "Die aktive Bekämpfung von Angsten durch Konfrontation",
        answerD = "Die Übertragung eigener Fehler auf andere Personen",
        correctAnswer = 1,
        explanation = "Verdraengung ist nach Freud ein zentraler Abwehrmechanismus: Angstmachende oder schmerzliche Inhalte werden unbewusst aus dem Bewusstsein ferngehalten, bleiben aber im Unbewussten wirksam.",
        difficulty = 2
    ),

    // Question 18 — Psychologie: Asch-Experiment
    Question(
        categoryId = 11,
        questionText = "Was zeigte Solomon Aschs Linien-Experiment aus den 1950ern?",
        answerA = "Menschen schoetzen Linienlaengen systematisch zu kurz",
        answerB = "Gruppenkonformitaet kann falsche Urteile herbeiführen",
        answerC = "Farbenblinde Menschen sind besser im Schaetzen von Laengen",
        answerD = "Stress verbessert die visuelle Wahrnehmung",
        correctAnswer = 1,
        explanation = "Im Asch-Experiment schlossen sich bis zu 75% der Versuchspersonen der offensichtlich falschen Antwort der Gruppe an, wenn andere Teilnehmer (die Mitspielende waren) die falsche Antwort gaben. Ein Beweis fuer Konformitaet.",
        difficulty = 2,
        funFact = "Asch war von den Ergebnissen selbst erschuettert — er hatte erwartet, dass Menschen nahezu immer das offensichtlich Richtige sagen wuerden."
    ),

    // Question 19 — Psychologie: Empathie vs Mitgefuehl
    Question(
        categoryId = 11,
        questionText = "Was unterscheidet 'Empathie' von 'Sympathie' in der Psychologie?",
        answerA = "Empathie ist nur eine Form von Sympathie fuer fremde Kulturen",
        answerB = "Empathie bedeutet, die Gefuehle anderer nachzufuehlen; Sympathie, Mitleid zu empfinden",
        answerC = "Empathie ist angeboren, Sympathie wird erlernt",
        answerD = "Es gibt keinen Unterschied, beide Begriffe sind synonym",
        correctAnswer = 1,
        explanation = "Empathie bezeichnet die Faehigkeit, sich in die Gefuehlslage anderer hineinzuversetzen und diese nachzuempfinden. Sympathie hingegen meint ein mitfuelendes Interesse oder Mitleid, ohne dieselbe emotionale Perspektive einzunehmen.",
        difficulty = 2
    ),

    // Question 20 — Psychologie: Panik- Angststoerung
    Question(
        categoryId = 11,
        questionText = "Welches Gehirnareal spielt die zentrale Rolle bei der Angstverarbeitung?",
        answerA = "Hippocampus",
        answerB = "Cerebellum",
        answerC = "Amygdala",
        answerD = "Praefrontaler Kortex",
        correctAnswer = 2,
        explanation = "Die Amygdala (Mandelkern) ist das zentrale Schaltzentrum fuer Angst und emotionale Reaktionen. Sie bewertet eingehende Reize auf Gefahr und loest die 'Kampf-oder-Flucht'-Reaktion aus.",
        difficulty = 2,
        funFact = "Die Amygdala reagiert auf Bedrohungen innerhalb von Millisekunden — schneller als das Bewusstsein. Deshalb erschrickt man oft, bevor man die Gefahr rational erkannt hat."
    ),

    // Question 21 — Psychologie: Big Five
    Question(
        categoryId = 11,
        questionText = "Welches der folgenden Merkmale gehoert NICHT zu den 'Big Five' der Persoenlichkeitspsychologie?",
        answerA = "Offenheit fuer Erfahrungen",
        answerB = "Vertraeglichkeit",
        answerC = "Risikobereitschaft",
        answerD = "Neurotizismus",
        correctAnswer = 2,
        explanation = "Die Big Five (OCEAN-Modell) umfassen: Offenheit, Gewissenhaftigkeit (Conscientiousness), Extraversion, Vertraeglichkeit (Agreeableness) und Neurotizismus. Risikobereitschaft ist kein offizieller Bestandteil des Modells.",
        difficulty = 2
    ),

    // Question 22 — Psychologie: Lerntypen
    Question(
        categoryId = 11,
        questionText = "Welche Art des Lernens beschreibt das Beobachten und Nachahmen anderer Personen?",
        answerA = "Habituation",
        answerB = "Modellernen (Beobachtungslernen)",
        answerC = "Operante Konditionierung",
        answerD = "Latentes Lernen",
        correctAnswer = 1,
        explanation = "Das Modellernen (auch Beobachtungslernen oder soziales Lernen) wurde von Albert Bandura erforscht. In seinem bekannten Bobo-Doll-Experiment imitierten Kinder aggressives Verhalten, das sie bei Erwachsenen beobachtet hatten.",
        difficulty = 2,
        funFact = "Banduras Bobo-Doll-Experimente aus den 1960ern loesten eine bis heute andauernde Debatte aus, ob Gewalt in Filmen und Spielen das Verhalten von Kindern beeinflusst."
    ),

    // Question 23 — Psychologie: Rueckschaufehler
    Question(
        categoryId = 11,
        questionText = "Was beschreibt der 'Rueckschaufehler' (Hindsight Bias)?",
        answerA = "Das Gefuehl, etwas schon immer gewusst zu haben, nachdem es eingetreten ist",
        answerB = "Die Tendenz, vergangene Entscheidungen im Rueckblick schoenreden",
        answerC = "Fehler, die durch zu viel Nachdenken entstehen",
        answerD = "Die Ueberschaetzung der Vorhersagbarkeit der Zukunft",
        correctAnswer = 0,
        explanation = "Der Rueckschaufehler bezeichnet das Phaenomen, nach einem Ereignis zu glauben, man haette es vorher schon gewusst oder kommen sehen. 'Das war doch klar!' ist ein typischer Ausdruck dieses Fehlers.",
        difficulty = 2
    ),

    // Question 24 — Psychologie: Sunk Cost Fallacy
    Question(
        categoryId = 11,
        questionText = "Was ist die 'Sunk-Cost-Falle'?",
        answerA = "Die Tendenz, bei sinkenden Aktienkursen zu kaufen",
        answerB = "Das Weiterverfolgen einer schlechten Entscheidung wegen bereits investierter Ressourcen",
        answerC = "Die Unterschaetzung zukuenftiger Kosten",
        answerD = "Untergegangene Schiffe als psychologische Metapher",
        correctAnswer = 1,
        explanation = "Die Sunk-Cost-Falle beschreibt das irrationale Weiterverfolgen einer Investition, weil bereits Ressourcen (Zeit, Geld, Muehe) investiert wurden — obwohl ein Abbruch rationaler waere. Typisch: 'Ich muss den Film zu Ende schauen, ich habe ja schon Geld bezahlt.'",
        difficulty = 2,
        funFact = "Auch Regierungen verfallen der Sunk-Cost-Falle: Das britisch-franzoesische Concorde-Projekt wurde jahrelang weiterfinanziert, obwohl klar war, dass es nie profitabel sein wuerde — heute ein Lehrbeispiel in Wirtschaftspsychologie."
    ),

    // Question 25 — Psychologie: Schlafen und Gedaechtnis
    Question(
        categoryId = 11,
        questionText = "In welcher Schlafphase wird das Gedaechtnis am staerksten konsolidiert?",
        answerA = "Einschlafphase (N1)",
        answerB = "Tiefschlaf (N3)",
        answerC = "REM-Schlaf",
        answerD = "Aufwachphase",
        correctAnswer = 2,
        explanation = "Im REM-Schlaf (Rapid Eye Movement) werden Erinnerungen verarbeitet und ins Langzeitgedaechtnis transferiert. Das Gehirn ist dabei fast so aktiv wie im Wachzustand — in dieser Phase traeumt man am intensivsten.",
        difficulty = 2
    ),

    // Question 26 — Psychologie: Attributionsfehler
    Question(
        categoryId = 11,
        questionText = "Was ist der 'fundamentale Attributionsfehler'?",
        answerA = "Die Tendenz, eigene Misserfolge der Situation zuzuschreiben",
        answerB = "Fehler beim Berechnen von Wahrscheinlichkeiten",
        answerC = "Das Ueberschaetzen von Persoenlichkeitseigenschaften und Unterschaetzen von Situationsfaktoren bei anderen",
        answerD = "Das Missdeuten von Koerpersprache in fremden Kulturen",
        correctAnswer = 2,
        explanation = "Der fundamentale Attributionsfehler beschreibt, dass wir das Verhalten anderer bevorzugt auf deren Charakter (intern) zurueckfuehren, statt auf situative Faktoren (extern). Wenn jemand zu spaet kommt, denken wir schnell: 'der ist unzuverlaessig' statt 'der steckte wohl im Stau'.",
        difficulty = 2
    ),

    // Question 27 — Psychologie: Soziale Hemmung
    Question(
        categoryId = 11,
        questionText = "Was bezeichnet 'Soziales Faulenzen' (Social Loafing) in der Psychologie?",
        answerA = "Die Beobachtung, dass Menschen in Gruppen weniger Einsatz zeigen als alleine",
        answerB = "Das Ablenken durch soziale Medien",
        answerC = "Schlechtere Lernleistung in Anwesenheit anderer",
        answerD = "Die Tendenz, bei Gruppenarbeiten Konflikte zu vermeiden",
        correctAnswer = 0,
        explanation = "Soziales Faulenzen (Social Loafing) bezeichnet das Phaenomen, dass Individuen in Gruppen weniger Anstrengung aufwenden als bei Einzelarbeit, weil die eigene Leistung weniger sichtbar und kontrollierbar ist.",
        difficulty = 2,
        funFact = "Der Effekt wurde erstmals 1913 von Max Ringelmann beobachtet: Beim Seilziehen mit mehr Personen sank die individuelle Kraftleistung — seitdem heisst er auch 'Ringelmann-Effekt'."
    ),

    // Question 28 — Psychologie: Selbstwirksamkeit
    Question(
        categoryId = 11,
        questionText = "Wer praegte den Begriff 'Selbstwirksamkeit' (Self-Efficacy) in der Psychologie?",
        answerA = "Carl Rogers",
        answerB = "Abraham Maslow",
        answerC = "Albert Bandura",
        answerD = "Martin Seligman",
        correctAnswer = 2,
        explanation = "Albert Bandura praegte den Begriff der Selbstwirksamkeit: Die Ueberzeugung, eine bestimmte Aufgabe erfolgreich bewaeltigen zu koennen. Eine hohe Selbstwirksamkeit korreliert stark mit Leistung, Ausdauer und psychischer Gesundheit.",
        difficulty = 2
    ),

    // Question 29 — Psychologie: Erlernte Hilflosigkeit
    Question(
        categoryId = 11,
        questionText = "Was beschreibt 'erlernte Hilflosigkeit' in der Psychologie?",
        answerA = "Ein Lernstil, bei dem Kinder von anderen abhaengig gemacht werden",
        answerB = "Das Aufgeben nach wiederholten Misserfolgen, obwohl Handeln moeglich waere",
        answerC = "Die Hilflosigkeit bei der Bedienung neuer Technologien",
        answerD = "Verlust motorischer Faehigkeiten durch fehlende Uebung",
        correctAnswer = 1,
        explanation = "Martin Seligman entdeckte erlernte Hilflosigkeit: Wenn Tiere (und Menschen) wiederholt unkontrollierbare Misserfolge erleben, hoeren sie auf zu handeln — selbst wenn Handeln moeglich waere. Dieser Mechanismus gilt als Modell fuer Depressionen.",
        difficulty = 2,
        funFact = "Seligman entwickelte aus dieser Forschung spaeter die 'Positive Psychologie' — er wollte nicht nur Leiden behandeln, sondern auch aktiv Glueck und Resilienz foerdern."
    ),

    // Question 30 — Psychologie: Frustrations-Aggressions-Hypothese
    Question(
        categoryId = 11,
        questionText = "Was besagt die Frustrations-Aggressions-Hypothese?",
        answerA = "Aggression entsteht immer aus Frustration",
        answerB = "Frustration fuehrt zu Rueckzug, nicht zu Aggression",
        answerC = "Nur Kinder zeigen frustrationsbedingte Aggression",
        answerD = "Aggression verstaerkt Frustration in einer Spirale",
        correctAnswer = 0,
        explanation = "Die Frustrations-Aggressions-Hypothese (Dollard et al., 1939) besagt, dass Frustration — das Blockieren eines Ziels — stets zu Aggression fuehrt. Spaetere Forschung zeigte, dass Frustration zwar Aggression beguenstigt, aber nicht zwingend auslost.",
        difficulty = 2
    ),

    // Question 31 — Psychologie: Fremd- vs. Selbstwahrnehmung
    Question(
        categoryId = 11,
        questionText = "Was beschreibt der 'Spotlight-Effekt' in der Psychologie?",
        answerA = "Die Wirkung von Buehnenbeleuchtung auf Redeangst",
        answerB = "Die Ueberschaetzung, wie sehr andere Menschen unser Verhalten beobachten",
        answerC = "Die positive Auswirkung von Aufmerksamkeit auf die Leistung",
        answerD = "Das Gefuehl, von der Gesellschaft ausgeschlossen zu werden",
        correctAnswer = 1,
        explanation = "Der Spotlight-Effekt beschreibt, dass Menschen systematisch ueberschaetzen, wie sehr andere ihre Fehler, ihr Aussehen oder ihr Verhalten wahrnehmen. In Wirklichkeit sind andere weitgehend mit sich selbst beschaeftigt.",
        difficulty = 2
    ),

    // Question 32 — Psychologie: Introversion Extraversion
    Question(
        categoryId = 11,
        questionText = "Wer praegte die psychologischen Begriffe 'Introversion' und 'Extraversion'?",
        answerA = "Sigmund Freud",
        answerB = "Carl Gustav Jung",
        answerC = "Alfred Adler",
        answerD = "William James",
        correctAnswer = 1,
        explanation = "Carl Gustav Jung praegte die Begriffe Introversion und Extraversion als Beschreibung grundlegender Persoenlichkeitsorientierungen: Introvertierte beziehen Energie aus sich selbst, Extravertierte aus der Aussenwelt.",
        difficulty = 2,
        funFact = "Jung und Freud waren urspruenglich enge Freunde und Kollegen, trennten sich aber 1912 im Streit — unter anderem wegen unterschiedlicher Auffassungen ueber das Unbewusste."
    ),

    // Question 33 — Psychologie: Arousal-Theorie
    Question(
        categoryId = 11,
        questionText = "Was besagt das Yerkes-Dodson-Gesetz?",
        answerA = "Hoehere Intelligenz fuehrt immer zu besserer Leistung",
        answerB = "Optimale Leistung entsteht bei mittlerem Erregungsniveau",
        answerC = "Schlaf verbessert Lernleistungen linear",
        answerD = "Soziale Unterstuetzung steigert die Motivation unbegrenzt",
        correctAnswer = 1,
        explanation = "Das Yerkes-Dodson-Gesetz (1908) beschreibt die umgekehrte U-Kurve zwischen Erregung (Arousal) und Leistung: Zu wenig Erregung fuehrt zu Untermotivation, zu viel zu Stress und Fehlern. Optimale Leistung entsteht bei mittlerem Arousal.",
        difficulty = 2
    ),

    // Question 34 — Psychologie: Gedaechtnistypen
    Question(
        categoryId = 11,
        questionText = "Wie nennt man Erinnerungen an persoenlich erlebte Ereignisse (z.B. Geburtstage)?",
        answerA = "Semantisches Gedaechtnis",
        answerB = "Prozedurales Gedaechtnis",
        answerC = "Episodisches Gedaechtnis",
        answerD = "Prospektives Gedaechtnis",
        correctAnswer = 2,
        explanation = "Das episodische Gedaechtnis speichert persoenlich erlebte Ereignisse mit Orts- und Zeitbezug (z.B. 'Mein Geburtstag letztes Jahr'). Das semantische Gedaechtnis hingegen speichert allgemeines Faktenwissen ohne persoenlichen Erlebnisbezug.",
        difficulty = 2
    ),

    // Question 35 — Psychologie: Autoritaerer Charakter
    Question(
        categoryId = 11,
        questionText = "Welche psychologische Stroemung untersuchte den 'autoritaeren Charakter' nach dem Zweiten Weltkrieg?",
        answerA = "Behaviorismus",
        answerB = "Humanistische Psychologie",
        answerC = "Kognitive Psychologie",
        answerD = "Kritische Theorie / Frankfurter Schule",
        correctAnswer = 3,
        explanation = "Theodor Adorno und Kollegen der Frankfurter Schule untersuchten nach dem 2. Weltkrieg den 'autoritaeren Charakter': Eine Persoenlichkeitsstruktur mit blindem Gehorsam gegenueber Autoritaeten, Vorurteilen und Konventionalismus.",
        difficulty = 2
    ),

    // Question 36 — Psychologie: Gestaltpsychologie
    Question(
        categoryId = 11,
        questionText = "Welcher Grundsatz beschreibt die Gestaltpsychologie am besten?",
        answerA = "Das Ganze ist mehr als die Summe seiner Teile",
        answerB = "Wahrnehmung ist rein subjektiv und kulturell bedingt",
        answerC = "Das Gehirn verarbeitet Informationen immer sequentiell",
        answerD = "Lernen geschieht ausschliesslich durch Wiederholung",
        correctAnswer = 0,
        explanation = "Der Kernsatz der Gestaltpsychologie lautet: 'Das Ganze ist mehr als die Summe seiner Teile.' Menschen nehmen Reize nicht isoliert wahr, sondern als bedeutungsvolles Ganzes. Bekannte Gestaltgesetze sind Naehe, Aehnlichkeit und Pragnanz.",
        difficulty = 2,
        funFact = "Die Gestaltpsychologie entstand um 1910 in Deutschland und Oesterreich. Alle drei Hauptvertreter — Wertheimer, Koehler, Koffka — emigrierten in den 1930ern in die USA und beinflussten massgeblich die amerikanische Kognitionswissenschaft."
    ),

    // Question 37 — Psychologie: Selbsterfuellende Prophezeiung
    Question(
        categoryId = 11,
        questionText = "Was ist eine 'selbsterfuellende Prophezeiung'?",
        answerA = "Eine Vorhersage, die aufgrund von Aberglauben eintritt",
        answerB = "Eine Erwartung, die das Verhalten so beeinflusst, dass sie sich bewahrheitet",
        answerC = "Ein Traum, der sich im wirklichen Leben wiederholt",
        answerD = "Ein Versprechen, das man sich selbst gibt",
        correctAnswer = 1,
        explanation = "Eine selbsterfuellende Prophezeiung tritt ein, wenn eine Erwartung das Verhalten so beeinflusst, dass sie sich tatsaechlich erfuellt. Beispiel: Ein Lehrer erwartet von einem Schueler schlechte Leistungen, behandelt ihn entsprechend, und der Schueler versagt tatsaechlich.",
        difficulty = 2
    ),

    // Question 38 — Psychologie: Verhaltensgenetik
    Question(
        categoryId = 11,
        questionText = "Welche Forschungsmethode nutzt die Psychologie, um den Einfluss von Genen und Umwelt auf Persoenlichkeit zu trennen?",
        answerA = "Laengsschnittstudien",
        answerB = "Zwillingsstudien",
        answerC = "Fallstudien",
        answerD = "Metaanalysen",
        correctAnswer = 1,
        explanation = "Zwillingsstudien vergleichen eineiige Zwillinge (gleiche Gene) mit zweieiigen Zwillingen (verschiedene Gene) — sowohl zusammen als auch getrennt aufgewachsen. So laesst sich der Einfluss von Erbanlagen (Natur) und Umwelt (Nurture) trennen.",
        difficulty = 2
    ),

    // Question 39 — Psychologie: Psychopharmaka
    Question(
        categoryId = 11,
        questionText = "Welcher Neurotransmitter steht vor allem im Fokus bei der medikamentosen Behandlung von Depressionen?",
        answerA = "Dopamin",
        answerB = "Acetylcholin",
        answerC = "Serotonin",
        answerD = "Noradrenalin",
        correctAnswer = 2,
        explanation = "Die meisten Antidepressiva (z.B. SSRIs wie Prozac/Fluoxetin) zielen auf den Serotonin-Haushalt ab. Serotonin gilt als 'Gluecks-Neurotransmitter' und beeinflusst Stimmung, Schlaf und Appetit.",
        difficulty = 2
    ),

    // Question 40 — Psychologie: Bindungstheorie
    Question(
        categoryId = 11,
        questionText = "Wer entwickelte die Bindungstheorie ('Attachment Theory')?",
        answerA = "Erik Erikson",
        answerB = "John Bowlby",
        answerC = "Lev Vygotsky",
        answerD = "Donald Winnicott",
        correctAnswer = 1,
        explanation = "John Bowlby entwickelte die Bindungstheorie: Kinder haben ein angeborenes Beduerfnis, eine enge emotionale Bindung zu einer Bezugsperson aufzubauen. Diese fruehe Bindung beeinflusst spaetere Beziehungen massgeblich.",
        difficulty = 2,
        funFact = "Mary Ainsworth erweiterte Bowlbys Theorie mit dem 'Fremde-Situation-Test' und identifizierte drei Bindungstypen: sicher, unsicher-vermeidend und unsicher-ambivalent."
    ),

    // Question 41 — Psychologie: Wahrnehmungsverzerrung
    Question(
        categoryId = 11,
        questionText = "Was versteht man unter 'selektiver Wahrnehmung'?",
        answerA = "Das Unvermogen, mehr als einen Reiz gleichzeitig wahrzunehmen",
        answerB = "Die Tendenz, nur Informationen wahrzunehmen, die zur eigenen Erwartung passen",
        answerC = "Wahrnehmungsstoerungen durch Drogen oder Erkrankungen",
        answerD = "Das bewusste Ignorieren unwichtiger Reize beim Lernen",
        correctAnswer = 1,
        explanation = "Selektive Wahrnehmung bedeutet, dass wir unbewusst bevorzugt das wahrnehmen, was unseren Erwartungen, Beduerffnissen oder Interessen entspricht. Wer z.B. ein Auto kaufen will, sieht plotzlich dieses Modell ueberall.",
        difficulty = 2
    ),

    // Question 42 — Psychologie: Prokrastination
    Question(
        categoryId = 11,
        questionText = "Was ist die psychologische Hauptursache von Prokrastination (Aufschieberitis)?",
        answerA = "Faulheit und mangelndes Interesse",
        answerB = "Schlechtes Zeitmanagement",
        answerC = "Emotionsregulation: Vermeidung unangenehmer Gefuehle",
        answerD = "Zu hohe Selbstdisziplin, die ins Gegenteil umschlaegt",
        correctAnswer = 2,
        explanation = "Neuere psychologische Forschung zeigt, dass Prokrastination kein Zeitmanagement-Problem ist, sondern ein Emotionsregulations-Problem: Menschen vermeiden Aufgaben, weil sie unangenehme Gefuehle wie Angst, Langeweile oder Frustration ausloesen.",
        difficulty = 2
    ),

    // Question 43 — Psychologie: Narzissmus
    Question(
        categoryId = 11,
        questionText = "Aus welcher griechischen Mythologie stammt der Begriff 'Narzissmus'?",
        answerA = "Narziss, ein Jaeger, der sich in sein eigenes Spiegelbild verliebte",
        answerB = "Narcissus, ein Krieger, der die Gotter herausforderte",
        answerC = "Narke, eine Meeresgottheit der Selbstvergessenheit",
        answerD = "Narkissos, Halbgott des ewigen Schlafes",
        correctAnswer = 0,
        explanation = "Der Begriff 'Narzissmus' stammt vom griechischen Mythos des Narziss: ein schoener Jaenger, der sich in sein eigenes Spiegelbild im Wasser verliebte und am Ufer starb. Sigmund Freud uebertrug den Begriff auf uebertriebene Selbstliebe in der Psychologie.",
        difficulty = 2,
        funFact = "Die Narzisse (Blume) ist nach demselben Mythenfigur benannt — sie waechst oft an Wasserraendern, als sei sie, wie Narziss, ins Wasser blickend."
    ),

    // Question 44 — Psychologie: Emotionstheorien
    Question(
        categoryId = 11,
        questionText = "Was besagt die James-Lange-Theorie der Emotionen?",
        answerA = "Emotionen entstehen durch Gedanken, die koerperliche Reaktionen ausloesen",
        answerB = "Koerperliche Reaktionen entstehen zuerst, dann interpretieren wir sie als Emotion",
        answerC = "Emotionen und koerperliche Reaktionen entstehen immer gleichzeitig",
        answerD = "Emotionen sind kulturell erlernt, nicht biologisch bedingt",
        correctAnswer = 1,
        explanation = "Die James-Lange-Theorie behauptet: Wir laufen nicht, weil wir Angst haben — wir haben Angst, weil wir laufen. Die koerperliche Reaktion (Herzrasen, Zittern) entsteht zuerst, das Gehirn interpretiert sie dann als Emotion.",
        difficulty = 2
    ),

    // Question 45 — Psychologie: Kognition
    Question(
        categoryId = 11,
        questionText = "Was bedeutet der Begriff 'Metakognition'?",
        answerA = "Unbewusstes Denken in Traumzustaenden",
        answerB = "Das Denken ueber das eigene Denken und Lernprozesse",
        answerC = "Die Faehigkeit, mehrere Gedanken gleichzeitig zu verfolgen",
        answerD = "Hoeherstufiges abstraktes Denken ab dem Erwachsenenalter",
        correctAnswer = 1,
        explanation = "Metakognition bezeichnet die Faehigkeit, das eigene Denken, Lernen und Problemloesen zu beobachten und zu steuern — also 'ueber sich selbst nachzudenken'. Metakognitive Faehigkeiten korrelieren stark mit schulischem und beruflichem Erfolg.",
        difficulty = 2
    ),

    // Question 46 — Psychologie: Deindividuation
    Question(
        categoryId = 11,
        questionText = "Was bezeichnet 'Deindividuation' in der Sozialpsychologie?",
        answerA = "Die Entstehung einer starken Einzelkaempfer-Mentalitaet",
        answerB = "Verlust der Selbstwahrnehmung in Gruppen, der Hemmschwellen senkt",
        answerC = "Die psychologische Anpassung an neue Lebensverhaeltnisse",
        answerD = "Die Dissoziation zwischen Koerper und Geist",
        correctAnswer = 1,
        explanation = "Deindividuation tritt in anonymen Gruppen auf: Wenn Einzelpersonen sich als Teil einer Masse fuehlen, sinkt die Selbstwahrnehmung, und sie handeln impulsiver oder aggressiver. Phaenomene wie Mob-Verhalten und Online-Hasskriminalitaet werden damit erklaert.",
        difficulty = 2
    ),

    // Question 47 — Psychologie: Soziale Identitaet
    Question(
        categoryId = 11,
        questionText = "Wer entwickelte die 'Soziale Identitaetstheorie', die erklaert, warum Menschen ihre Eigengruppe bevorzugen?",
        answerA = "Gordon Allport",
        answerB = "Henri Tajfel",
        answerC = "Kurt Lewin",
        answerD = "Muzafer Sherif",
        correctAnswer = 1,
        explanation = "Henri Tajfel entwickelte die Soziale Identitaetstheorie: Menschen definieren sich ueber Gruppenzugehoerigkeiten (Nationalitaet, Beruf, Verein) und tendieren dazu, die eigene Gruppe gegenueber Fremdgruppen aufzuwerten.",
        difficulty = 2,
        funFact = "Tajfel war selbst polnisch-juedischer Holocaust-Ueberlebender — sein persoenliches Erleben von Ausgrenzung pragte massgeblich seine Forschung zu Vorurteilen und Gruppenidentitaet."
    ),

    // Question 48 — Psychologie: Phobien
    Question(
        categoryId = 11,
        questionText = "Wie lautet der Fachbegriff fuer krankhafte Angst vor offenen, weiten Plaetzen?",
        answerA = "Klaustrophobie",
        answerB = "Xenophobie",
        answerC = "Agoraphobie",
        answerD = "Sozialphobie",
        correctAnswer = 2,
        explanation = "Agoraphobie (von griech. 'agora' = Marktplatz) bezeichnet die Angst vor offentlichen, weiten oder unuebersichtlichen Raeumen und Situationen, aus denen eine Flucht schwierig waere. Sie tritt haeufig mit Panikstörungen auf.",
        difficulty = 2
    ),

    // Question 49 — Psychologie: Stress
    Question(
        categoryId = 11,
        questionText = "Wer praegte in der Medizin und Psychologie den modernen Begriff des 'Stress'?",
        answerA = "Hans Selye",
        answerB = "Walter Cannon",
        answerC = "Richard Lazarus",
        answerD = "Aaron Beck",
        correctAnswer = 0,
        explanation = "Der austro-ungarische Arzt Hans Selye praegte in den 1930er-Jahren den Begriff 'Stress' in seiner heutigen Bedeutung. Er beschrieb das 'Allgemeine Anpassungssyndrom': die koerperliche Reaktion auf anhaltenden Stress in drei Phasen (Alarm, Widerstand, Erschoepfung).",
        difficulty = 2,
        funFact = "Selye entwickelte seine Stresstheorie zunaechst aus einem Versehen: Bei Experimenten mit Ratten beobachtete er unerwartete koerperliche Veraenderungen — und erkannte, dass es sich um Reaktionen auf Belastungen handelte."
    ),

    // Question 50 — Psychologie: Resilienz
    Question(
        categoryId = 11,
        questionText = "Was versteht man in der Psychologie unter 'Resilienz'?",
        answerA = "Die Faehigkeit, Traumata vollstaendig zu vergessen",
        answerB = "Psychische Widerstandsfaehigkeit und Erholung nach Krisen",
        answerC = "Die Tendenz, trotz Rueckschlaegen immer risikofreudiger zu werden",
        answerD = "Genetische Unempfindlichkeit gegenueber Stress",
        correctAnswer = 1,
        explanation = "Resilienz bezeichnet die psychische Widerstandsfaehigkeit, nach belastenden Ereignissen (Trauma, Verlust, Krise) in das seelische Gleichgewicht zurueckzufinden. Resilienz ist kein stabiles Merkmal, sondern ein erlernbarer und trainierbarer Prozess.",
        difficulty = 2,
        funFact = "Eine der laengsten Resilienzstudien begleitete 698 Kinder auf Hawaii ueber 40 Jahre. Schluesselfaktor fuer Resilienz war nicht Intelligenz oder soziale Herkunft, sondern eine einzige stabile Vertrauensperson in der Kindheit."
    )

)

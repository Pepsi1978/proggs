package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun musicQuestionsExpert2(): List<Question> = listOf(

    // ── Dirigiertechnik & Orchestermanagement (8) ────────────────────────────

    Question(
        categoryId = 5,
        questionText = "Welche Schlagtechnik setzt ein Dirigent ein, wenn er im 4/4-Takt den zweiten Schlag als 'Ausweichschlag' nach rechts ausfuehrt?",
        answerA = "Fermata-Technik",
        answerB = "Horizontale Subdivision",
        answerC = "Konventioneller Rechts-Schlag (klassische Ictus-Bewegung)",
        answerD = "Marcato-Auftakt",
        correctAnswer = 2,
        explanation = "Im klassischen 4/4-Schema schlaegt der Dirigent: 1=unten, 2=rechts (Ausweich), 3=links, 4=oben. Der zweite Schlag ist der nach rechts gehende Ictus-Punkt — eine Konvention, die auf Gestik-Schulen des 19. Jahrhunderts zurueckgeht.",
        difficulty = 4,
        funFact = "Verschiedene Dirigenten entwickeln individuelle Schlagbilder. Leonard Bernstein war bekannt fuer extrem expressive, grosse Gesten, waehrend Carlos Kleiber minimale, praezise Bewegungen bevorzugte — beide mit aussergewoehnlichem Ergebnis."
    ),

    Question(
        categoryId = 5,
        questionText = "Was versteht man unter dem Begriff 'Auftakt' (Anacrusis) in der Dirigiertechnik?",
        answerA = "Der letzte Taktschlag vor einer Generalpause",
        answerB = "Die vorbereitende Atembewegung des Dirigenten vor dem ersten Orchestereinsatz",
        answerC = "Der erste Schlag eines neuen Tempos nach einem Ritardando",
        answerD = "Das Zeichen fuer das Ende einer Kadenz",
        correctAnswer = 1,
        explanation = "Der Auftakt (Anacrusis oder Vorbereitungsschlag) ist die suggestive Aufwaertsbewegung des Taktstocks vor dem eigentlichen Einsatz. Er kommuniziert Tempo, Dynamik und Charakter gleichzeitig und ist das wichtigste Kommunikationsmittel des Dirigenten.",
        difficulty = 4,
        funFact = "Ein guter Auftakt zeigt dem Orchester alles: Wie schnell? Wie laut? Mit welchem Ton? Dirigenten wie Claudio Abbado konnten mit einem einzigen Auftakt ein ganzes Programm einleiten — ohne ein Wort zu sprechen."
    ),

    Question(
        categoryId = 5,
        questionText = "Welche Funktion hat die 'Konzertmeister'-Position im Sinfonieorchester?",
        answerA = "Verwaltung des Notenmaterials und der Stimmrechte",
        answerB = "Leitung der Blechblaeser und Schlagzeuger in Abwesenheit des Dirigenten",
        answerC = "Erster Geiger, Orchestervertreter und Vermittler zwischen Dirigent und Ensemble",
        answerD = "Koordination der Probenplaene und Gastdirigenten",
        correctAnswer = 2,
        explanation = "Der Konzertmeister (englisch: Concertmaster) ist der erste Sologeiger, repraesentiert das Orchester gegenueber dem Dirigenten, stimmt das Orchester ein, gibt die Bogenstriche vor und hat eine Vermittlerrolle zwischen Dirigent und Ensemble.",
        difficulty = 4,
        funFact = "In Orchestern ohne Dirigenten — wie dem Orpheus Chamber Orchestra — uebernimmt der Konzertmeister oft alle Dirigentenfunktionen. Das Orpheus-Modell gilt als demokratischstes Orchestermodell weltweit."
    ),

    Question(
        categoryId = 5,
        questionText = "Was bezeichnete Felix Weingartner in seinem 1895 erschienenen Traktat 'Ueber das Dirigieren' als Hauptfehler damaliger Dirigenten?",
        answerA = "Zu geringe Taktsicherheit und fehlerhafte Metronomzahlen",
        answerB = "Uebertriebenes Rubato und willkuerliche Tempomodifikationen nach Wagner-Manier",
        answerC = "Vernachlaessigung der Holzblaeser-Balance zugunsten der Streicher",
        answerD = "Mangelhafte Kenntnis der Partituren und Ablesen vom Blatt",
        correctAnswer = 1,
        explanation = "Weingartner kritisierte scharf den von Wagner propagierten 'Melos'-Dirigierstil mit extremen Temposchwankungen. Er pl\u00e4dierte fuer groessere Werktreue und Zurueckhaltung des Dirigenten gegenueber dem Notentext.",
        difficulty = 4,
        funFact = "Richard Wagner selbst dirigierte Beethovens 9. Symphonie mit erheblichen Tempofreiheiten. Sein Aufsatz 'Ueber das Dirigieren' (1869) beeinflusste Generationen von Dirigenten — sowohl im positiven als auch negativen Sinne."
    ),

    Question(
        categoryId = 5,
        questionText = "Was ist das 'Stick Technique'-Konzept nach Ilya Musin in der russischen Dirigierschule?",
        answerA = "Verwendung eines schweren Taktstocks fuer praezisere Schlaege bei grossen Orchestern",
        answerB = "Verbindung von Koerper-, Arm- und Handgelenksbewegungen zu einem organischen Bewegungsfluss",
        answerC = "Technik, bei der der Taktstock nur bei Fortissimo-Stellen eingesetzt wird",
        answerD = "Methode zur Kontrolle des Atems beim gleichzeitigen Singen und Dirigieren",
        correctAnswer = 1,
        explanation = "Ilya Musin (1904-1999) entwickelte an der Leningrader Konservatorium eine umfassende Schlagtechnik, die Koerper, Arm, Unterarm und Handgelenk als integrierte Einheit versteht. Seine Schueler umfassen Dirigenten wie Valery Gergiev und Semyon Bychkov.",
        difficulty = 4,
        funFact = "Musin unterrichtete ueber 60 Jahre lang und schrieb sein Hauptwerk 'The Technique of Conducting' erst mit 80 Jahren. Er gilt als Gruendervater der modernen russischen Dirigierschule."
    ),

    Question(
        categoryId = 5,
        questionText = "Welche Stimmung wird traditionell verwendet, wenn ein Orchester sich vor einem Konzert einstimmt?",
        answerA = "Das Oboen-A des Konzertmeisters bei 432 Hz",
        answerB = "Das A der Oboe bei 440 Hz (oder orchester-intern vereinbarter Frequenz)",
        answerC = "Das mittlere C des Klaviers bei 261,6 Hz",
        answerD = "Das A der ersten Geige nach Stimmgabel 442 Hz",
        correctAnswer = 1,
        explanation = "Das Einstimmen erfolgt auf das A der Oboe, weil die Oboe kaum verstimmt und ihren Ton gut haelt. Der Standard-Kammerton liegt bei 440 Hz (ISO 16), viele Orchester bevorzugen jedoch 442-444 Hz fuer einen helleren Klang.",
        difficulty = 4,
        funFact = "Die Berliner Philharmoniker stimmen auf 443 Hz, die Wiener Philharmoniker auf 443 Hz. Barockensembles stimmen oft auf 415 Hz — eine kleine Terz tiefer als modern. Diese Frequenzunterschiede veraendern die Klangfarbe spuerbar."
    ),

    Question(
        categoryId = 5,
        questionText = "Welchen Begriff verwendete Hans von Buelow fuer das Dirigieren aus dem Gedaechtnis (ohne Partitur auf dem Pult)?",
        answerA = "Memoria Musica",
        answerB = "Dirigat ad libitum",
        answerC = "Auswendigdirigieren",
        answerD = "Freihandleitung",
        correctAnswer = 2,
        explanation = "Hans von Buelow (1830-1894) war einer der ersten Dirigenten, der systematisch aus dem Gedaechtnis dirigierte. Er pragte den Begriff 'Auswendigdirigieren' und machte es zur kuenstlerischen Norm. Heute ist es Standardpraxis fuer die meisten Spitzendirigenten.",
        difficulty = 4,
        funFact = "Als von Buelow zum ersten Mal Beethovens 9. Symphonie auswendig dirigierte, war das Publikum so schockiert, dass einige glaubten, er koenne die Partitur gar nicht lesen. Heute gilt das Auswendigdirigieren als Zeichen hoechster musikalischer Reife."
    ),

    Question(
        categoryId = 5,
        questionText = "Was beschreibt der Fachbegriff 'Generalpause' (GP) in einer Orchesterpartitur?",
        answerA = "Eine Pause von mindestens vier Takten fuer alle Instrumente ausser den Schlagzeugern",
        answerB = "Das gleichzeitige Pausieren aller Stimmen fuer mindestens einen vollen Takt",
        answerC = "Eine Fermate auf der letzten Note eines Satzes",
        answerD = "Eine mehrtaktige Solopassage des Konzertmeisters",
        correctAnswer = 1,
        explanation = "Die Generalpause (GP) bezeichnet eine gleichzeitige Pause aller beteiligten Stimmen und Instrumente. Sie ist ein dramatisches Ausdrucksmittel und erfordert groesste Praezision des Dirigenten — zu fruehes oder zu spaetes Einsetzen zerstoert den Effekt vollstaendig.",
        difficulty = 4,
        funFact = "Die wohl beruehm teste Generalpause der Musikgeschichte findet sich in Haydns Symphonie Nr. 94 ('Mit dem Paukenschlag'). Die ploetzliche Stille vor dem Paukenschlag im zweiten Satz soll schlaefende Zuhoerer aufwecken — eine musikalische Pointe."
    ),

    // ── Musikpsychologie & Kognition (8) ─────────────────────────────────────

    Question(
        categoryId = 5,
        questionText = "Welches Phaenomen beschreibt die 'Mere Exposure'-Wirkung (Zajonc, 1968) in der Musikpsychologie?",
        answerA = "Die Tendenz, Musikstuecke nach mehrmaligem Hoeren attraktiver zu finden",
        answerB = "Den Effekt, dass bekannte Melodien schneller erinnert werden als unbekannte",
        answerC = "Die Praeferenz fuer einfache harmonische Strukturen gegenueber komplexen",
        answerD = "Die Gewoehnungsreaktion des Gehoers bei laengerer Laerm-Exposition",
        correctAnswer = 0,
        explanation = "Robert Zajoncs 'Mere Exposure Effect' zeigt, dass blosses wiederholtes Erleben eines Reizes — ohne Belohnung oder Bewertung — zu erhoehter Praeferenz fuehrt. In der Musik erklaert dies, warum Radiohits durch Wiederholung geliebter werden.",
        difficulty = 4,
        funFact = "Das Paradoxon: Overexposure kann den Effekt umkehren. Zu oft gespielte Songs werden als nervig empfunden — ein Effekt, den die Musikindustrie als 'Burnout' bezeichnet und der erklaert, warum Radiosender Rotationsplaene haben."
    ),

    Question(
        categoryId = 5,
        questionText = "Was ist 'Earworm' (Ohrwurm) aus neurowissenschaftlicher Sicht?",
        answerA = "Eine auditive Halluzination durch Schlafentzug",
        answerB = "Involuntaer wiederholtes mentales Abspielen eines musikalischen Fragments im Arbeitsgedaechtnis",
        answerC = "Ein Geraeusch in der Stille, das durch Tinnitusreaktion entsteht",
        answerD = "Die mentale Vorstellung von Musikinstrumenten beim Lesen von Partituren",
        correctAnswer = 1,
        explanation = "Ohrwuermer (INMI: Involuntary Musical Imagery) entstehen im auditiven Kortex durch einen 'Cognitive Itch' — ein unfertiges melodisches Fragment aktiviert eine Wiedeholungsschleife im Arbeitsgedaechtnis. Haeufig sind es melodische Fragmente mit offenen Sequenzen.",
        difficulty = 4,
        funFact = "Forscherin Victoria Williamson fand, dass die meisten Ohrwuermer nur 15-30 Sekunden lang sind. Der effektivste 'Heilmittel' ist laut Studien: das Stueck zu Ende zu spielen oder zu singen, damit das Gehirn die Sequenz als abgeschlossen verarbeiten kann."
    ),

    Question(
        categoryId = 5,
        questionText = "Was beschreibt das Konzept der 'Musical Expectancy' (Leonard Meyer, 1956)?",
        answerA = "Die angeborene Faehigkeit von Kindern, Rhythmus zu erkennen",
        answerB = "Die emotionale Reaktion entsteht durch das Spiel mit Erwatrungen und deren Erfullung oder Verletzung",
        answerC = "Die Erwartung des Publikums an das Programm eines Konzertabends",
        answerD = "Die Tendenz von Musikern, im Ensemble aufeinander zu warten",
        correctAnswer = 1,
        explanation = "Leonard Meyers Theorie besagt, dass musikalische Emotion durch das Spiel mit Erwartungen entsteht: Erwartet wird eine harmonische Aufloesung, eine rhythmische Betonung, eine melodische Richtung. Erfullung, Verzoegerung oder Verletzung dieser Erwartung erzeugt emotionale Reaktionen.",
        difficulty = 4,
        funFact = "David Huron erweiterte Meyers Theorie 2006 mit dem ITPRA-Modell (Imagination, Tension, Prediction, Reaction, Appraisal). Es erklaert, warum der Moment VOR dem gefuerchteten Paukenschlag oft aufregender ist als der Schlag selbst."
    ),

    Question(
        categoryId = 5,
        questionText = "Welchen Befund lieferte die 'Montreal Neurological Institute'-Studie von Salimpoor et al. (2011) zur Musik-Emotion?",
        answerA = "Musik aktiviert das visuelle Kortex-Areal, was Synasthesie bei Musikern erklaert",
        answerB = "Antizipatorische und erreichte Hohepunkte in Musik aktivieren das mesolimbische Dopaminsystem",
        answerC = "Traurige Musik senkt den Serotoninspiegel messbar unter Baselinewerte",
        answerD = "Rhythmische Musik synchronisiert die Herzrate mit dem Grundpuls des Stuecks",
        correctAnswer = 1,
        explanation = "Salimpoor et al. zeigten mit fMRT und PET, dass musikalische 'Chills' (Gaensehaut-Momente) mit Dopaminausschuettung im Nucleus accumbens verbunden sind — sowohl beim Hohepunkt als auch schon bei der Antizipation. Musik aktiviert dasselbe Belohnungssystem wie Nahrung oder Sex.",
        difficulty = 4,
        funFact = "Die Studie war revolutionaer, weil sie erstmals biologisch erklarte, warum Musik so maechtigen emotionalen Einfluss hat — obwohl sie keinen direkten Ueberlebenswert hat. Das 'Raetsel der Musik' als evolutionaeres Phaenomen ist bis heute ungeklaert."
    ),

    Question(
        categoryId = 5,
        questionText = "Was ist 'Absolute Pitch' (Absolutes Gehoer) und wie haeufig tritt es in der Allgemeinbevoelkerung auf?",
        answerA = "Die Faehigkeit, gehoegte Intervalle zu benennen; tritt bei ca. 5% auf",
        answerB = "Die Faehigkeit, einen Ton ohne Referenz zu identifizieren; tritt bei ca. 1:10.000 auf",
        answerC = "Die Faehigkeit, Obertoeneschwingungen wahrzunehmen; tritt bei Berufsmusikern haeufiger auf",
        answerD = "Die Faehigkeit, Tonsystem-Unterschiede (440 vs 432 Hz) zu hoeren; tritt universal auf",
        correctAnswer = 1,
        explanation = "Absolutes Gehoer (AP) ist die Faehigkeit, die Tonhoehe eines Klangs ohne externe Referenz zu identifizieren. Es tritt bei etwa 1 von 10.000 Menschen auf, jedoch bei Berufsmusikern und Menschen, die in fruehster Kindheit Toninstrumente erlernten, deutlich haeufiger.",
        difficulty = 4,
        funFact = "Interessant: In Ostasien (Japan, China) haben Musiker signifikant haeufiger absolutes Gehoer. Forscher vermuten, dass Tonensprachen wie Mandarin oder Kantonesisch, bei denen Tonhoehe Bedeutung traegt, das absolute Gehoer im Kindesalter foerdern."
    ),

    Question(
        categoryId = 5,
        questionText = "Welche Theorie erklaert die universelle Praeferenz fuer konsonante Klangverhaeltnisse (z.B. Quinte, Oktave)?",
        answerA = "Kulturelle Konditionierung durch westliche Tonskalen seit der Antike",
        answerB = "Koinzidenz von Obertonsreihen: Konsonante Intervalle haben viele gemeinsame Partialtoeneschwingungen",
        answerC = "Synchronisation von Gamma-Gehirnwellen mit einfachen Frequenzverhaeltnissen",
        answerD = "Evolutionaere Praeferenz fuer Kommunikationslaute mit niedrigem Geraeuschanteil",
        correctAnswer = 1,
        explanation = "Die Helmholtz-Theorie besagt, dass Konsonanz durch Koinzidenz von Partialtoeneschwingungen entsteht. Eine reine Quinte (3:2) hat viele gemeinsame Obertoeneschwingungen, waehrend eine kleine Sekunde (16:15) kaum gemeinsame Teiltoeneschwingungen hat und dadurch 'schwebend' klingt.",
        difficulty = 4,
        funFact = "Neugeborene zeigen bereits eine Praeferenz fuer konsonante Klangverhaeltnisse, bevor sie Musik gehoerenlernt haben. Das legt eine biologische, nicht nur kulturelle Grundlage fuer Konsonanzempfinden nahe — ein Hinweis auf evolutionaere Wurzeln des musikalischen Gehoers."
    ),

    Question(
        categoryId = 5,
        questionText = "Was beschreibt der Begriff 'Entrainment' in der Musikpsychologie und Neurowissenschaft?",
        answerA = "Die Faehigkeit des Gehoers, einzelne Stimmen in polyphonem Klang herauszufiltern",
        answerB = "Die Synchronisation oszillierender neuronaler oder physischer Rhythmen mit einem externen Rhythmustakt",
        answerC = "Der Lernprozess, bei dem Kinder Melodien nachsingen lernen",
        answerD = "Die Reaktion des autonomen Nervensystems auf Unterbrechungen im Rhythmus",
        correctAnswer = 1,
        explanation = "Entrainment bezeichnet die Tendenz zweier oszillierender Systeme, sich zu synchronisieren. In der Musik synchronisieren sich Gehirnoszillationen (EEG-Rhythmen), Herzrate und motorische Systeme mit dem Beat. Dies ist die Grundlage fuer das Mitwippen, Tanzen und Marschieren.",
        difficulty = 4,
        funFact = "Christiaan Huygens entdeckte Entrainment 1665 an Pendeluhren: Zwei nebeneinander aufgehaengte Uhren synchronisierten ihre Pendel durch minimale Vibrationen der gemeinsamen Wand. Musik nutzt dasselbe Prinzip, um ganze Menschenmassen zu synchronisieren."
    ),

    Question(
        categoryId = 5,
        questionText = "Welche Studie zeigte erstmals, dass Musik beim Lernen von Saeuglingssprache helfen kann ('Mozart-Effekt' Replikation)?",
        answerA = "Rauscher, Shaw & Ky (1993) — Ratten navigierten Labyrinthe schneller nach Mozart-Exposition",
        answerB = "Rauscher, Shaw & Ky (1993) — Studenten zeigten kurzfristige Verbesserung bei raumlichem Denken",
        answerC = "Schellenberg (2004) — Kinder mit Musikunterricht zeigten hoeheren IQ als Kontrollgruppen",
        answerD = "Hallam (2010) — Musik verbesserte die Leseleistung bei Grundschuelern messbar",
        correctAnswer = 1,
        explanation = "Rauscher, Shaw & Ky (1993) zeigten bei Studenten eine kurzfristige Verbesserung (ca. 15 Minuten) bei Aufgaben zum raeumlichen Denken nach Mozart-Hoeren. Dieser 'Mozart-Effekt' wurde massiv uebertrieben und fehlinterpretiert — spaeters Forschungen konnten ihn kaum reproduzieren.",
        difficulty = 4,
        funFact = "Der Mozart-Effekt fuehrte zu einer Milliarden-Dollar-Industrie mit Baby-Mozart-CDs und -Videos. Dabei zeigte die Originalstudie nur eine 15-minuetige Verbesserung bei Erwachsenen in einer sehr spezifischen Aufgabe — kein dauerhafter Intelligenzgewinn."
    ),

    // ── Akustische Architektur & Konzertsaele (8) ────────────────────────────

    Question(
        categoryId = 5,
        questionText = "Welcher akustische Parameter beschreibt die Zeit, in der ein Klang nach Abschalten der Schallquelle auf 1/1000000 (60 dB) seiner Ausgangslautstaerke abgefallen ist?",
        answerA = "Echo-Dichte (ED)",
        answerB = "Hallzeit (RT60)",
        answerC = "Raumimpulsantwort (RIR)",
        answerD = "Schallabsorptionskoeffizient (SAK)",
        correctAnswer = 1,
        explanation = "Die Nachhallzeit RT60 (Reverberation Time) ist die Zeit in Sekunden, in der der Schallpegel nach Abschalten der Quelle um 60 dB abfaellt — was einer Verringerung auf ein Millionstel der urspruenglichen Schallintensitaet entspricht. Sie ist der wichtigste Planungsparameter fuer Konzertsaele.",
        difficulty = 4,
        funFact = "Ideale RT60-Werte variieren stark nach Musiktyp: Kammermusik benoetigt 1,4-1,6 Sekunden, Sinfoniekonzerte 2,0-2,2 Sekunden, romantische Orgelmusik 3-5 Sekunden. Kathedralen wie Notre-Dame de Paris haben RT60-Werte von ueber 8 Sekunden."
    ),

    Question(
        categoryId = 5,
        questionText = "Welche Bauform des Konzertsaals gilt als akustisch ueberlegen fuer Sinfoniekonzerte und warum?",
        answerA = "Hufeisenform (wie klassische Opernhaeuser), weil sie mehr Zuschauer nah an die Buehne bringt",
        answerB = "Schuhkarton-Form (Shoebox), weil sie fruehe seitliche Reflexionen und hohe Hallzeit erzeugt",
        answerC = "Amphitheater (offen), weil natiuerliche Luft die Reflexionen minimiert und Klarheit maximiert",
        answerD = "Flaecherform (Fan-Shape), weil sie gleichmaessige Schallverteilung bei minimaler Hallzeit bietet",
        correctAnswer = 1,
        explanation = "Die 'Shoebox'-Form (langes Rechteck, hohe Seitenwaende) wie in der Berliner Philharmonie-Vorgaengerin oder dem Wiener Musikverein erzeugt wertvolle seitliche fruehe Reflexionen (ITDG unter 20ms), die fuer raeumliches Klangerlebnis und 'Halligkeit' entscheidend sind.",
        difficulty = 4,
        funFact = "Der Wiener Musikverein (1870) mit seiner Shoebox-Akustik gilt bis heute als Goldstandard. Leo Beranek analysierte ihn in 'Music, Acoustics and Architecture' (1962) und stellte fest, dass seine einzigartigen Reflexionsmuster durch 58 Karyatiden-Saeulen entstehen."
    ),

    Question(
        categoryId = 5,
        questionText = "Was ist der 'Intimacy Index' (Beranek) in der Konzertsaalakustik?",
        answerA = "Verhaltnis von Direkt- zu Nachhallschall beim Dirigentenpult",
        answerB = "Die anfaengliche Zeitverzoegerung (ITDG) zwischen Direktschall und erster Wandreflexion",
        answerC = "Mittlerer Abstand aller Sitzplaetze von der Buehne in Metern",
        answerD = "Prozentualer Anteil der Buehnenflaeche am Gesamtvolumen des Saals",
        correctAnswer = 1,
        explanation = "Der 'Intimacy Index' oder 'Initial Time Delay Gap' (ITDG) ist die Zeit in Millisekunden zwischen dem Eintreffen des Direktschalls und der ersten starken Seitenreflexion. Kleine ITDG-Werte (unter 20ms) erzeugen das Gefuehl von Intimaet und Praeenz — ein wesentliches Merkmal grosser Konzertsaele.",
        difficulty = 4,
        funFact = "Leo Beraneks revolutionaere Akustikforschung in den 1960er Jahren zeigte, dass der beste Saal nicht der groesste oder teuerste ist, sondern der mit dem kleinsten ITDG. Die Berliner Philharmonie hat trotz grossem Volumen hervorragende Intimity-Werte durch ihre Vineyard-Anordnung."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches Material wurde in der Berliner Philharmonie (1963, Architekt Hans Scharoun) vorrangig fuer die Deckenakustik eingesetzt?",
        answerA = "Glaswolle-Paneele hinter perforierten Stahlplatten fuer optimale Absorption",
        answerB = "Freigeformte Gipspaneele und Holzwolleplatten zur diffusen Schallverteilung",
        answerC = "Betonschalungsplatten mit variablen Winkelneigungen fuer gezielte Reflexionen",
        answerD = "Akustischer Schaumstoff mit Pyramidenstruktur zur Maximierung der Hallzeit",
        correctAnswer = 1,
        explanation = "Die Decke der Berliner Philharmonie besteht aus freigeformten, asymmetrischen Gipspaneelen, die den Schall diffus in alle Richtungen streuen. Sie wurden in enger Zusammenarbeit zwischen Architekt Scharoun und Akustiker Lothar Cremer entwickelt.",
        difficulty = 4,
        funFact = "Die Berliner Philharmonie revolutionierte den Konzertsaalbau mit dem 'Vineyard'-Prinzip: Das Orchester sitzt in der Mitte, das Publikum rundherum auf Terrassen. Zuvor war die Buehne immer vorne. Heute gilt dieses Konzept als wegweisend, wurde aber selten so erfolgreich kopiert."
    ),

    Question(
        categoryId = 5,
        questionText = "Welcher Effekt tritt auf, wenn zwei parallele Wandflaechen in einem Raum identische Reflexionseigenschaften haben und der Schall zwischen ihnen hin und her springt?",
        answerA = "Kammfiltereffekt",
        answerB = "Flatterecho",
        answerC = "Stehende Welle (Raummode)",
        answerD = "Haas-Effekt",
        correctAnswer = 1,
        explanation = "Flatterecho (Flutter Echo) entsteht, wenn Schall zwischen zwei parallelen, glatten Waenden repetitiv reflektiert wird und ein charakteristisches 'Flattern' oder 'Zwitschern' erzeugt. Es ist ein gefuerchteter akustischer Fehler in Raeumen und wird durch Schraegestellen der Waende oder Diffusoren vermieden.",
        difficulty = 4,
        funFact = "Leere Kirchen mit Steinwaenden produzieren exzellentes Flatterecho. Musiker berichten, dass Aufnahmen in solchen Raeumen einen einzigartigen 'lebendigen' Klang haben, der durch digitale Nachhall-Plugins nie vollstaendig reproduziert werden kann."
    ),

    Question(
        categoryId = 5,
        questionText = "Was versteht man unter dem akustischen Begriff 'Diffusion' im Kontext der Raumakustik?",
        answerA = "Die Ausbreitung von Schallwellen durch Waende in benachbarte Raeume",
        answerB = "Die gleichmaessige Verteilung von Schallereignissen ueber Ort und Zeit ohne diskrete Reflexionen",
        answerC = "Die Absorption von Schall durch poroese Materialien in den Waenden",
        answerD = "Die Mischung von direktem Schall und Nachhall am Ohr des Hoerenden",
        correctAnswer = 1,
        explanation = "Diffusion in der Raumakustik bezeichnet die gleichmaessige raeumliche und zeitliche Verteilung von Schallreflexionen. Diffusoren (z.B. QRD-Diffusoren nach Schroeder) zersteuern Schall in viele Richtungen, vermeiden Flatterecho und erzeugen einen 'runden', gleichmaessigen Hallklang.",
        difficulty = 4,
        funFact = "Manfred Schroeder entwickelte 1975 den mathematisch optimierten 'Quadratic Residue Diffuser' (QRD), dessen Tiefenmuster auf der Zahlentheorie basiert. Diese Diffusoren sind heute in fast jedem professionellen Studio und Konzertsaal zu finden."
    ),

    Question(
        categoryId = 5,
        questionText = "Welcher Architekt entwarf die Elbphilharmonie in Hamburg (eroeffnet 2017) und welches besondere akustische Konzept wurde verwendet?",
        answerA = "Frank Gehry; Vineyard-Anordnung mit Metallgitter-Balkonbruestungen",
        answerB = "Herzog & de Meuron (Architektur) + Yasuhisa Toyota (Akustik); 'White Skin'-Schalenplatten fuer Diffusion",
        answerC = "Zaha Hadid; parametrisch berechnete Betonschalen mit variablen Reflexionswinkeln",
        answerD = "Renzo Piano; modulares Holz-Panels-System mit einstellbarem Absorptionsgrad",
        correctAnswer = 1,
        explanation = "Die Elbphilharmonie wurde von Herzog & de Meuron entworfen. Akustiker Yasuhisa Toyota (Nagata Acoustics) entwickelte die charakteristischen 'White Skin'-Schalenplatten: 10.000 einzelne Gipsfaser-Paneele mit individuell berechneten Wellenformen, die Schall gleichmaessig diffus streuen.",
        difficulty = 4,
        funFact = "Jede der 10.000 Schalenplatten in der Elbphilharmonie ist ein Unikat mit einzigartigem Wellenprofil, das computergesteuert gefraest wurde. Die Entwicklung dauerte Jahre und die Kosten des Konzerthauses explodierten von 77 Millionen auf 866 Millionen Euro."
    ),

    Question(
        categoryId = 5,
        questionText = "Was ist der 'Haas-Effekt' (Precedence Effect) und welche Bedeutung hat er fuer die Raumakustik?",
        answerA = "Der Effekt, dass fruehe Reflexionen bis 30-40ms als Teil des Direktschalls wahrgenommen werden und die Lautstaerke erhoehen ohne Echo zu verursachen",
        answerB = "Die Tendenz des Gehoers, Schall aus der Richtung der groessten Lautstaerkequelle zu lokalisieren",
        answerC = "Das Phaenomen, dass bei hohen Frequenzen fruehe Reflexionen als Echo wahrgenommen werden",
        answerD = "Die psychoakustische Fusion zweier Schallquellen zu einer, wenn sie identisches Signalspektrum haben",
        correctAnswer = 0,
        explanation = "Der Haas-Effekt (1951) besagt, dass fruehe Reflexionen, die innerhalb von 20-30ms nach dem Direktschall eintreffen, nicht als separate Echoes wahrgenommen werden. Sie verschmelzen mit dem Direktschall und erhoehen scheinbar Lautstaerke und Fueller — die Grundlage fuer Beschallungsanlagen in grossen Hallen.",
        difficulty = 4,
        funFact = "Der Haas-Effekt wird in der Live-Beschallung von Stadien genutzt: Lautsprecher in der Mitte des Stadions koennen mit Verzoegerung betrieben werden, sodass das Gehirn den Klang der naeher gelegenen Lautsprecher als 'Original' hoert — obwohl der Buehnenlautsprecher laenger hat."
    ),

    // ── Musiknotation-Geschichte (8) ──────────────────────────────────────────

    Question(
        categoryId = 5,
        questionText = "Welches System der Musiknotation entwickelte Guido von Arezzo um 1000 n. Chr. und welches war sein wesentlicher Beitrag?",
        answerA = "Die Neumen-Notation: Zeichen ueber dem Text, die grobe Tonrichtung anzeigten",
        answerB = "Das Vier-Linien-System mit clef-Zeichen, das genaue Tonhoehen fixierte",
        answerC = "Die Mensural-Notation mit prazisen Notenwerten fuer mehrstimmige Musik",
        answerD = "Die Tabulaturnotation fuer Saiteninstrumente mit Griffpositionsangaben",
        correctAnswer = 1,
        explanation = "Guido von Arezzo entwickelte um 1025 das System mit Hilfslinien (die sich zu vier Linien entwickelten), Schluessel-Zeichen (Clef) und die Solmisation (Ut-Re-Mi-Fa-Sol-La). Dies eroeffnete erstmals die Moeglichkeit, genaue Tonhoehen schriftlich festzulegen.",
        difficulty = 4,
        funFact = "Guido von Arezzos 'Ut queant laxis'-Hymne war nicht nur sein didaktisches Werkzeug, sondern auch die Grundlage unserer heutigen Silben Do-Re-Mi-Fa-Sol-La-Si. Der erste Silben des Werks wurden zur Benennung der Toene verwendet — eine Genieleistung, die 1000 Jahre ueberlebt hat."
    ),

    Question(
        categoryId = 5,
        questionText = "Was war das 'Guidonische Hande' (Guido's Hand) im mittelalterlichen Musikunterricht?",
        answerA = "Eine Handzeichensprache fuer die Chorleitung ohne Taktstock",
        answerB = "Eine Mnemotechnik, bei der die Gelenke der Hand als Noten-Toene fungierten",
        answerC = "Ein Trainingsgerat fuer Lautenisten zum Strecken der Finger",
        answerD = "Das Freihand-Dirigieren ohne Partitur (nach Guido von Arezzo)",
        correctAnswer = 1,
        explanation = "Die 'Guidonische Hand' war ein Lehr-Hilfsmittel, bei dem jeder Fingergelenk-Punkt einem Ton des Hexachord-Systems zugeordnet war. Durch Zeigen auf verschiedene Handpunkte konnte ein Chorleiter dem Ensemble Tonhoehen ohne geschriebene Noten vermitteln.",
        difficulty = 4,
        funFact = "Die Guidonische Hand war das mittelalterliche Aequivalent heutiger Curwen-Handzeichen im Kodaly-System. Beide nutzen Koerpergestik als musikalisches Kommunikationsmittel — ein Prinzip, das Tausende von Jahren Schulunterricht beeinflusst hat."
    ),

    Question(
        categoryId = 5,
        questionText = "Was unterscheidet die 'Mensural-Notation' (13.-16. Jh.) von der Gregorianischen Neumen-Notation?",
        answerA = "Sie verwendete farbige Noten fuer verschiedene Instrumente in polyphonem Satz",
        answerB = "Sie kodierte prazise Notenwertdauer und eroeffnete damit mehrstimmige rhythmisch koordinierte Komposition",
        answerC = "Sie schrieb erstmals Dynamik-Angaben (forte/piano) und Bogensetzung vor",
        answerD = "Sie verwendete arabische Ziffern statt lateinischer Buchstaben fuer Tonhoehen",
        correctAnswer = 1,
        explanation = "Die Mensural-Notation (lat. mensura = Mass) von Franco von Koeln (ca. 1260) und spaeteren Theoretikern kodierte relative Notenwertverhaeltnisse: Longa, Brevis, Semibrevis, Minima. Dies ermoeglichte erstmals praezise Koordination mehrstimmiger Stimmen in der Polyphonie der Ars Antiqua und Ars Nova.",
        difficulty = 4,
        funFact = "Die Mensural-Notation hatte keine Taktstriche — diese wurden erst im 17. Jahrhundert einfuehrt. Saenger und Spieler orientierten sich an hierarchischen Notenwertverhaeltnissen (tempus, modus, prolatio). Das Entschluesseln alter Mensural-Partituren ist heute eine eigene musikwissenschaftliche Spezialdisziplin."
    ),

    Question(
        categoryId = 5,
        questionText = "Welcher Drucker veroeffentlichte 1501 die erste kommerziell gedruckte Musiksammlung ('Harmonice Musices Odhecaton')?",
        answerA = "Johann Gutenberg in Mainz mit beweglichen Metalltypen",
        answerB = "Ottaviano Petrucci in Venedig mit dem Mehrfachdruck-Verfahren",
        answerC = "Robert Estienne in Paris mit Holzschnitt-Notenplatten",
        answerD = "Christoph Plantin in Antwerpen mit Kupferstich-Technologie",
        correctAnswer = 1,
        explanation = "Ottaviano Petrucci (1466-1539) entwickelte in Venedig ein revolutionaeres Dreigang-Druckverfahren (Notenlinien, Noten, Text separat gedruckt) und veroeffentlichte 1501 das 'Harmonice Musices Odhecaton' — die erste in Europa kommerziell gedruckte mehrstimmige Musiksammlung.",
        difficulty = 4,
        funFact = "Petruccis Methode war so qualitaetsvoll, dass seine gedruckten Noten kaum von handgeschriebenen Manuskripten zu unterscheiden waren. Seine Drucke verbreiteten Obrechts, Josquins und anderer Meister Werke durch ganz Europa und waren der Buchdruck der Musik-Renaissance."
    ),

    Question(
        categoryId = 5,
        questionText = "Was ist eine 'Neume' in der mittelalterlichen Musiknotation?",
        answerA = "Ein Zeichen ueber dem Gesangstext, das Melodiebewegung andeutet, aber keine genaue Tonhoehe fixiert",
        answerB = "Ein Taktzeichen, das die Anzahl der Schlaege pro Takt angibt",
        answerC = "Ein Dynamiksymbol aus der Gregorianischen Choralnotation",
        answerD = "Ein Ornamentzeichen fuer Triller und Mordenten im Barock",
        correctAnswer = 0,
        explanation = "Neumen (griech.: Wink, Zeichen) sind Notation-Zeichen ueber dem Gesangstext, die dem Saenger die Richtung der Melodiebewegung anzeigen (hoeher/tiefer, steigend/fallend), aber keine praezisen Tonhoehen oder Rhythmen fixieren. Sie sind Gedaechtnis-Stuetzen fuer bereits bekannte Melodien.",
        difficulty = 4,
        funFact = "Die aeltesten Neumen-Handschriften stammen aus dem 9. Jahrhundert. Moderne Palaeographen koennen durch Vergleich verschiedener Handschriften-Fassungen der gleichen Melodie die urspruengliche Kontur rekonstruieren — eine musikwissenschaftliche Detektivarbeit."
    ),

    Question(
        categoryId = 5,
        questionText = "Was ist Tabulaturnotation und fuer welche Instrumente wurde sie hauptsaechlich verwendet?",
        answerA = "Eine Notation fuer Schlagzeug, die Trommelschlags-Muster durch Buchstaben kodiert",
        answerB = "Eine instrumentenspezifische Notation, die Griffpositionen oder Fingersaetze statt Tonhoehen angibt",
        answerC = "Eine vereinfachte Notation fuer Anfaenger mit Ziffernsystem statt Noten",
        answerD = "Eine Orgelnotation mit getrennten Systemen fuer Manuale und Pedal",
        correctAnswer = 1,
        explanation = "Tabulaturnotation bezeichnet alle Systeme, die Spielanweisungen (Griffpositionen, Bundreihe, Taste) statt abstrakter Tonhoehen notieren. Hauptanwendungen: Lautentabulatur (15.-17. Jh.), Gitarrentabulatur (modern), Orgelintavolatur. Verschiedene Laender verwendeten verschiedene Tabulatursysteme (italienisch, deutsch, franzoesisch).",
        difficulty = 4,
        funFact = "Moderne Gitarren-Tabs auf Websites wie Ultimate Guitar sind direkte Nachfolger der Renaissance-Lautentabulatur. Das 500 Jahre alte Notationsprinzip 'zeig mir wo ich den Finger hinlege' ueberlebt bis heute, weil es intuitiver ist als das klassische Notensystem."
    ),

    Question(
        categoryId = 5,
        questionText = "Was war die 'Ars Nova' (ca. 1310-1380) in der Geschichte der Musiknotation?",
        answerA = "Eine Bewegung, die Orgel-Improvisation durch schriftliche Notation zu ersetzen versuchte",
        answerB = "Eine Erweiterung der Mensural-Notation um kleinere Notenwerte und Tempusvielfalt, ermoeglicht durch Philippe de Vitry",
        answerC = "Die erste systematische Verwendung von Dynamikzeichen in mehrstimmigen Werken",
        answerD = "Eine Reform der Gregorianischen Notation durch Johannes de Muris und Marchetto da Padova",
        correctAnswer = 1,
        explanation = "Die Ars Nova ('Neue Kunst'), massgeblich durch Philippe de Vitrys gleichnamigen Traktat (ca. 1322) initiiert, revolutionierte die Mensural-Notation: Kleinere Notenwerte (Semibrevis, Minima), duple Metrum als gleichwertig zum triple Metrum, und groesstere rhythmische Komplexitaet. Sie eroeffnete den Weg zur isorhythmischen Motette.",
        difficulty = 4,
        funFact = "Guillaume de Machaut, der groesste Komponist der Ars Nova, schrieb die 'Messe de Nostre Dame' — die aelteste vollstaendig erhaltene Vertonung der Messe-Ordinarien, die von einem einzelnen Komponisten stammt. Sie klingt noch heute frisch und wird regelmaessig aufgefuehrt."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches Notationsprinzip fuegte Johann Sebastian Bach in seinen Choralen konsequent ein und das spaeter zum Standard wurde?",
        answerA = "Generalbass-Bezifferung unter allen Choraelen zur harmonischen Analyse",
        answerB = "Separate Stimmen auf getrennten Systemen (SATB) statt Stimmen auf einem Notensystem",
        answerC = "Fingersatz-Angaben fuer Cembalo und Orgel in allen vier Stimmen",
        answerD = "Taktartwechsel durch Ziffernangabe am Taktanfang statt ausgeschriebener Mensural-Zeichen",
        correctAnswer = 1,
        explanation = "Bachs Choralsaetze verwenden durchgehend vier separate Systeme fuer Sopran, Alt, Tenor, Bass (SATB-System). Obwohl nicht von Bach erfunden, machte seine konsequente Anwendung und die weite Verbreitung seiner Choralsaetze das SATB-Notensystem zum Standard fuer Chorpartituren.",
        difficulty = 4,
        funFact = "Bachs 371 Choralsaetze wurden nach seinem Tod von seinem Sohn Carl Philipp Emanuel gesammelt und veroeffentlicht. Sie wurden zum wichtigsten Lehrwerk der Harmonielehre in ganz Europa — und sind es bis heute in jedem Konservatorium weltweit."
    ),

    // ── Elektronische Klangsynthese & Synthesizergeschichte (8) ──────────────

    Question(
        categoryId = 5,
        questionText = "Welche Synthesemethode verwendet der klassische Moog-Synthesizer (1964) als Grundprinzip?",
        answerA = "FM-Synthese (Frequenzmodulation) mit sechs Operatoren",
        answerB = "Subtraktive Synthese: Saegezahn-Wellenform durch Filterung zu neuem Klang geformt",
        answerC = "Additive Synthese: Summierung einzelner Sinuswellen zu komplexen Klangen",
        answerD = "Wavetable-Synthese: Abspielen gespeicherter Wellenformabschnitte",
        correctAnswer = 1,
        explanation = "Robert Moogs Synthesizer (patentiert 1964) nutzt subtraktive Synthese: Harmonisch reiche Wellenformen (Saegezahn, Rechteck) werden durch spannungsgesteuerte Filter (VCF), Verstaerker (VCA) und Huelkurven-Generatoren (ADSR) modifiziert, um die spektrale Zusammensetzung zu reduzieren und zu formen.",
        difficulty = 4,
        funFact = "Der Moog-Synthesizer wurde durch Wendy Carlos' Album 'Switched-On Bach' (1968) weltberuehmt. Das Album verkaufte sich ueber eine Million Mal und bewies, dass ein 'elektronisches Instrument' klassische Musik mit kuenstlerischem Wert spielen kann — eine Revolution der Musikwelt."
    ),

    Question(
        categoryId = 5,
        questionText = "Was ist FM-Synthese und welches Geraet machte sie 1983 populaer?",
        answerA = "Filtermodulation: Variierung der Filterfrequenz im Rhythmus der Musik; popularisiert durch den Roland Jupiter-8",
        answerB = "Frequenzmodulation: Ein 'Carrier'-Operator wird von einem 'Modulator'-Operator in seiner Frequenz variiert; popularisiert durch den Yamaha DX7",
        answerC = "Fast-Fourier-Modulation: Echtzeit-Spektralanalyse und Resynthese; popularisiert durch den Fairlight CMI",
        answerD = "Formant-Modulation: Vokalklang-Synthese durch Formantenfilterung; popularisiert durch den Roland D-50",
        correctAnswer = 1,
        explanation = "FM-Synthese (John Chowning, Stanford 1967) moduliert die Frequenz eines 'Carrier'-Signals mit einem 'Modulator'-Signal. Bei hohen Modulationsindizes entstehen komplexe, metallische Klangfarben. Yamaha lizenzierte das Patent und baute den DX7 (1983), einen der meistverkauften Synthesizer aller Zeiten.",
        difficulty = 4,
        funFact = "Der Yamaha DX7 pragte den Sound der 1980er massiv: E-Piano, Glocken, Floetenklang — all das klingt nach DX7. Ueber 200.000 Einheiten wurden verkauft. John Chownings Royalties aus der Yamaha-Lizenz ermoeglichten das Center for Computer Research in Music and Acoustics (CCRMA) in Stanford."
    ),

    Question(
        categoryId = 5,
        questionText = "Was ist das Thereminvox und wer erfand es?",
        answerA = "Ein elektronisches Tasteninstrument mit Lichtschranken-Steuerung; erfunden von Maurice Martenot (1928)",
        answerB = "Ein beruehrungsloses elektronisches Instrument, das durch Abstandsmessung zu zwei Antennen Tonhoehe und Lautstaerke steuert; erfunden von Leon Theremin (1920)",
        answerC = "Ein Fruehes Synthesizer-Modell mit Bandaufnahme-Schnittstelle; erfunden von Robert Moog (1953)",
        answerD = "Ein elektromagnetisches Saiteinstrument aehnlich der Gitarre; erfunden von Ernst Buschmann (1915)",
        correctAnswer = 1,
        explanation = "Das Theremin (1920) ist ein elektronisches Instrument, das ohne Beruehrung gespielt wird: Die rechte Hand steuert durch Abstand zur vertikalen Antenne die Tonhoehe, die linke Hand durch Abstand zur horizontalen Bugelantenne die Lautstaerke. Erfunden von Leon Theremin (Lev Termen) in Russland.",
        difficulty = 4,
        funFact = "Clara Rockmore gilt als groesste Theremin-Virtuosin aller Zeiten. Leon Theremin, der Erfinder, folgte ihr nach New York und verlor sie an einen anderen Mann. Jahrzehnte spaeter, als 91-jaehriger, traf er Rockmore wieder — eine bewegende Begegnung, die im Film 'Theremin: An Electronic Odyssey' dokumentiert ist."
    ),

    Question(
        categoryId = 5,
        questionText = "Was war das 'Musique Concrete' und wer sind die Pioniere dieser Bewegung?",
        answerA = "Elektronische Musik mit konkreten Wellenformen statt synthetischen; Stockhausen und Boulez in Koeln",
        answerB = "Komponierte Musik aus aufgenommenen Alltagsgeraueschen und manipulierten Toenband-Klangen; Pierre Schaeffer und Pierre Henry in Paris",
        answerC = "Aleatorische Musik mit zufaellig bestimmten Parametern; John Cage und Morton Feldman in New York",
        answerD = "Elektronische Realzeit-Improvisation ohne Partitur; Karlheinz Stockhausen in Darmstadt",
        correctAnswer = 1,
        explanation = "Musique Concrete (konkrete Musik) wurde 1948 von Pierre Schaeffer am Pariser Rundfunk (RTF) entwickelt. Reale Klaenge (Geraeusche, Instrumente, Stimmen) wurden auf Tonband aufgenommen, manipuliert (rueckwaerts, verlangsamt, beschleunigt, geschnitten) und neu zusammengesetzt.",
        difficulty = 4,
        funFact = "Pierre Scheaffers 'Etude aux chemins de fer' (1948) aus Eisenbahngeraeuschen gilt als das erste vollstaendige Stueck Musique Concrete. Der Zug-Rhythmus wurde zum pulsierenden Klangmaterial. Pierre Henry spaeter erlangte Bekanntheit durch 'Psyche Rock' (1967), der als Vorlage fuer Futuramas Titelmelodie diente."
    ),

    Question(
        categoryId = 5,
        questionText = "Was ist 'Wavetable-Synthese' und wie unterscheidet sie sich von Sampling?",
        answerA = "Wavetable nutzt kurze Wellenform-Zyklen aus einer Tabelle und schleift sie, waehrend Sampling vollstaendige Aufnahmen abspielt",
        answerB = "Wavetable ist identisch mit Sampling, aber mit besserer Filterung und kuerzerer Latenz",
        answerC = "Wavetable interpoliert zwischen gespeicherten Spektralschnappschuessen, waehrend Sampling Zeitbereichs-Aufnahmen nutzt",
        answerD = "Wavetable verwendet additive Synthese mit vorberechneten Obertonsummierungen",
        correctAnswer = 0,
        explanation = "Wavetable-Synthese (Wolfgang Palm, PPG Wave, 1978) speichert kurze Wellenformzyklen (typisch 256-2048 Samples) in einer Tabelle. Diese Zyklen werden mit variabler Frequenz abgespielt und gescleift. Durch Scannen durch verschiedene Wavetables entstehen zeitlich veraenderliche Klangfarben. Sampling hingegen spielt vollstaendige, laengere Aufnahmen ab.",
        difficulty = 4,
        funFact = "Wolfgang Palms PPG Wave 2.2 (1982) war der erste kommerzielle Wavetable-Synthesizer und ein festes Werkzeug von Bands wie Depeche Mode und Talk Talk. Heute ist Wavetable-Synthese die Basis moderner Synthesizer wie dem Serum oder Vital."
    ),

    Question(
        categoryId = 5,
        questionText = "Was war der 'Mellotron' und welche Bands machten ihn beruehmt?",
        answerA = "Ein fruehes digitales Sampling-Keyboard mit Flash-Speicher fuer Orchesterklange; beruehmt durch Kraftwerk",
        answerB = "Ein Bandmaschinen-Keyboard, das voraufgenommene Instrumentenklange abspielt; beruehmt durch die Beatles und King Crimson",
        answerC = "Ein polyphoner Synthesizer mit analogem Filterbank-System; beruehmt durch Emerson, Lake & Palmer",
        answerD = "Ein elektronisches Akkordeon mit Bandantrieb; beruehmt durch Tangerine Dream",
        correctAnswer = 1,
        explanation = "Das Mellotron (1963, UK) enthielt fuer jede Taste einen Tonbandstreifen mit aufgenommenem Instrumentenklang (Floetenregister, Streichorchester, Choir). Beim Tastendruck bewegte sich das Band unter einem Lese-Kopf. Die Beatles nutzten es in 'Strawberry Fields Forever' (1967), King Crimson in 'In the Court of the Crimson King'.",
        difficulty = 4,
        funFact = "Das Mellotron hatte eine eingebaute Zeitbegrenzung: Da Bandstreifen nur 8 Sekunden lang waren, konnte keine Note laenger gehalten werden. Diese Limitation wurde zum Stilmerkmal. Jedes der 35 Tasten-Baender war ein eigener 8-Sekunden-Streifen — 35 Bandspuren in einem Keyboard."
    ),

    Question(
        categoryId = 5,
        questionText = "Welche Synthesemethode verwendet 'Granular Synthesis' als Grundprinzip?",
        answerA = "Zerlegung von Klang in tausende mikroskopische Klangatome (Grains, 1-50ms) und deren Rekomposition",
        answerB = "Addition von Sinuswellen mit zeitlich variablen Amplituden nach Fourier-Analyse",
        answerC = "Modulation von Resonanzfrequenzen durch koernigen Rauschsignal als Traeger",
        answerD = "Zerteilung des Frequenzspektrums in Granulaerfilterbanken fuer spektrale Transformation",
        correctAnswer = 0,
        explanation = "Granular Synthesis (Dennis Gabor, 1947; praktisch umgesetzt von Iannis Xenakis und Curtis Roads ab den 1970ern) zerlegt Klang in winzige Segmente ('Grains', 1-50ms). Durch Manipulation von Pitch, Dichte, Ueberlappung, Groesse und Verteilung dieser Grains entstehen voellig neuartige, 'texturale' Klange.",
        difficulty = 4,
        funFact = "Dennis Gabor, der Begruender der Holographie, entwickelte die Theorie der Granularsynthese als mathematisches Konzept. Er stellte fest, dass jeder Klang als zweidimensionale Zeit-Frequenz-Matrix aus elementaren Quanten beschrieben werden kann — eine Einsicht mit Parallelen zur Quantenmechanik."
    ),

    Question(
        categoryId = 5,
        questionText = "Was ist ADSR in der Synthesizertechnik und was bedeuten die einzelnen Buchstaben?",
        answerA = "Amplitude, Decay, Sustain, Release — die vier Parameter einer Huelkurve, die den zeitlichen Verlauf der Lautstaerke (oder anderer Parameter) beschreiben",
        answerB = "Attack, Duration, Speed, Resonance — Parameter fuer die Filtersteuerung in subtraktiver Synthese",
        answerC = "Analog, Digital, Synthesis, Resolution — Klassifikationsschema fuer Synthesizer-Typen",
        answerD = "Aftertouch, Detune, Sustain, Release — MIDI-Controller-Parameter fuer Expressionsteuerung",
        correctAnswer = 0,
        explanation = "ADSR (Attack, Decay, Sustain, Release) beschreibt den zeitlichen Verlauf einer Huelkurve: Attack = Anstiegszeit vom Null auf Maximum; Decay = Abfallzeit vom Maximum auf Sustain-Pegel; Sustain = gehaltener Pegel bei gedrueckter Taste; Release = Abklingzeit nach Loslassen. Erfunden von Vladimir Ussachevsky, popularisiert durch Robert Moog.",
        difficulty = 4,
        funFact = "Das ADSR-Prinzip ahmt natuerliche Klangentwicklung nach: Ein Klavier hat kurzen Attack, kurzen Decay, keinen Sustain (der Ton klingt ab), und mittlere Release. Eine Orgel hat kurzen Attack, keinen Decay, maximalen Sustain und kurzen Release. Diese Parameter definieren den 'Charakter' jedes Instruments."
    ),

    // ── Musikalische Forschung & Computational Musicology (5) ────────────────

    Question(
        categoryId = 5,
        questionText = "Was ist 'Music Information Retrieval' (MIR) als Forschungsfeld?",
        answerA = "Die Restaurierung und Digitalisierung historischer Musikhandschriften fuer Archivzwecke",
        answerB = "Die automatische Extraktion und Analyse musikalischer Informationen aus Audio- oder Notendaten mit algorithmischen Methoden",
        answerC = "Das legale Abrufen kommerziell geschuetzter Musik durch Streaming-Dienste",
        answerD = "Die Interview-basierte Erforschung des Musikalischen Gedaechtnisses bei Berufskuenstlern",
        correctAnswer = 1,
        explanation = "Music Information Retrieval (MIR) ist ein interdisziplinaeres Feld (Informatik, Musikwissenschaft, Signal-Verarbeitung), das Algorithmen zur automatischen Analyse, Klassifikation und Suche in Musikdaten entwickelt: Tonartenerkennung, Beat Tracking, Genre-Klassifikation, Chord Recognition, Melodiesuche.",
        difficulty = 4,
        funFact = "Die groesste MIR-Konferenz, ISMIR (International Society for Music Information Retrieval), existiert seit 2000. Praktische Anwendungen sind ueberall: Shazam erkennt Songs durch Fingerprint-Matching, Spotify nutzt MIR fuer Empfehlungen, YouTube erkennt Urheberrechtsverletzungen durch Audiofingerprinting."
    ),

    Question(
        categoryId = 5,
        questionText = "Was ist die 'Humdrum Toolkit' in der Computational Musicology?",
        answerA = "Eine Datenbank historischer Volksmelodien aus 150 Kulturen",
        answerB = "Ein Software-System zur formalen Repraesentation und Analyse von Noten- und Musik-Daten durch UNIX-Kommandozeilen-Werkzeuge",
        answerC = "Ein KI-System zur automatischen Harmonisierung von Melodien nach Bach-Regeln",
        answerD = "Eine Bibliothek digitalisierter Partituren klassischer Meisterwerke im XML-Format",
        correctAnswer = 1,
        explanation = "Humdrum Toolkit (David Huron, Ohio State, 1993) ist ein Set von UNIX-Kommandozeilen-Programmen zur Analyse musikalischer Daten. Es nutzt `**kern`-Format fuer Noten-Kodierung und ermoeglicht komplexe statistische Analysen von Melodie, Harmonie, Rhythmus ueber grosse Notenkorpora.",
        difficulty = 4,
        funFact = "David Huron nutzte Humdrum fuer sein Buch 'Sweet Anticipation' (2006), in dem er ueber 100.000 Melodien aus verschiedenen Kulturen analysierte, um Universalien der musikalischen Erfahrung zu finden. Humdrum bleibt ein Standardwerkzeug der Computational Musicology."
    ),

    Question(
        categoryId = 5,
        questionText = "Was zeigt das 'Corpus-Studie' von Burgoyne et al. (2011) ueber Jazz-Harmonik?",
        answerA = "Dass Jazz-Akkordfolgen statistisch weniger komplex sind als romantische Harmonik",
        answerB = "Analyse der 'Billboard Jazz Charts': Die haeufigsten Akkordfolgen im Jazz sind ueberraschend regelmaessig und folgen tonalen Gravitationsgesetzen",
        answerC = "Dass Jazz-Improvisation stark von kulturellen Faktoren abhaengt und kaum auf Theorie basiert",
        answerD = "Haeufigkeit der II-V-I-Progression in 7.000 transkribierten Jazz-Standards aus dem iRealPro-Datenbank",
        correctAnswer = 3,
        explanation = "Burgoynes 'McGill Billboard' Corpus analysierte 7.000 Jazz-Standards aus dem iRealPro-Fakebook. Die Studie zeigt, dass die II-V-I-Progression (Dm7-G7-Cmaj7 in C-Dur) mit Abstand die haeufigste Akkordfolge im Standard-Jazz ist und als harmonisches Grundmuster das gesamte Genre definiert.",
        difficulty = 4,
        funFact = "Das iRealPro-Fakebook mit ueber 6.000 Jazz-Standards ist heute nicht nur Musiker-App, sondern auch Forschungsdatenbank. Die statistischen Erkenntnisse ueber Jazz-Harmonik beeinflussen KI-Systeme wie OpenAI Jukebox und Google Magenta beim Generieren von Jazz-Musik."
    ),

    Question(
        categoryId = 5,
        questionText = "Was ist 'Optical Music Recognition' (OMR) und welche Hauptherausforderung besteht dabei?",
        answerA = "Die maschinelle Erkennung und Digitalisierung von gedruckten oder handgeschriebenen Notenpartituren",
        answerB = "Die optische Analyse von Buehnenlicht-Frequenzen zur automatischen Harmonieerkennung",
        answerC = "Ein Kamerasystem fuer blinde Musiker, das Noten vorliest",
        answerD = "Die Erkennung von Dirigenten-Gesten durch Computer-Vision fuer automatische Orchesterbegleitung",
        correctAnswer = 0,
        explanation = "Optical Music Recognition (OMR) ist das Pendant zu OCR fuer Musiknotation. Hauptherausforderungen: Notenschrift ist zweidimensional und kontextsensitiv (die Bedeutung eines Symbols haengt von Nachbarsymbolen ab), handgeschriebene Noten variieren stark, historische Notation folgt anderen Regeln.",
        difficulty = 4,
        funFact = "IMSLP (International Music Score Library Project) hat ueber 700.000 Partituren digitalisiert. Ein Grossteil liegt als Bild-Scan vor, nicht als editierbare Notation. OMR koennte diese Scans in bearbeitbare, transponierbare, midi-spielbare Partituren umwandeln — eine riesige musikwissenschaftliche Aufgabe."
    ),

    Question(
        categoryId = 5,
        questionText = "Was entdeckte der Musikforscher Alan Lomax mit seiner 'Cantometrics'-Methode (1968)?",
        answerA = "Universelle melodische Grundmuster, die in allen menschlichen Kulturen auftreten",
        answerB = "Korrelationen zwischen musikalischen Stilmerkmalen und soziokulturellen Strukturen verschiedener Gesellschaften",
        answerC = "Dass bestimmte Tonarten universell mit bestimmten Emotionen assoziiert werden",
        answerD = "Die Evolution der Pentatonik aus binaeralen Klang-Praeferenzen primitiver Voelker",
        correctAnswer = 1,
        explanation = "Alan Lomax entwickelte Cantometrics als kodiertes Analysesystem fuer Volksmusik weltweit. Er analysierte Aufnahmen aus ueber 200 Gesellschaften und entdeckte, dass musikalische Stilmerkmale (z.B. Gruppengesang vs. Sologesang, Stimmqualitaet, Rhythmusstruktur) mit sozialen Strukturen wie Hierarchie, Geschlechterrollen und Wirtschaftsform korrelieren.",
        difficulty = 4,
        funFact = "Alan Lomax war nicht nur Forscher, sondern auch Aktivist: Er dokumentierte und verbreitete amerikanische Folk- und Blues-Musik und machte Musiker wie Leadbelly und Woody Guthrie bekannt. Seine Feldaufnahmen bilden den Grundstock des Archivs der Library of Congress."
    ),

    // ── Historische Auffuehrungspraxis (5) ───────────────────────────────────

    Question(
        categoryId = 5,
        questionText = "Was ist 'Historische Auffuehrungspraxis' (Early Music/HIP) als musikwissenschaftliche Praxis?",
        answerA = "Die Auffuehrung alter Musik auf modernen Instrumenten unter Beachtung historischer Tempo-Angaben",
        answerB = "Die Wiederherstellung historisch authentischer Auffuehrungsbedingungen: Originalinstrumente, zeitgenoessische Spieltechniken und historisch informierte Interpretationspraxis",
        answerC = "Die wissenschaftliche Dokumentation vergangener Konzertauffuehrungen durch Archivforschung",
        answerD = "Die Anpassung alter Werke fuer moderne Besetzungen unter Erhalt der harmonischen Struktur",
        correctAnswer = 1,
        explanation = "Historisch Informierte Auffuehrungspraxis (HIP) zielt auf authentische Rekonstruktion: Kopien historischer Instrumente (Cembalo, Naturtrompete, Barockvioline mit Darmsaiten), historische Stimmton (415 Hz fuer Barock), zeitgenoessische Spieltechniken und Ornamentierungspraxis laut historischen Traktaten.",
        difficulty = 4,
        funFact = "Die HIP-Bewegung wurde in den 1950er-1960ern durch Musiker wie Nikolaus Harnoncourt und Gustav Leonhardt gepragt. Ihr Einfluss ist heute so gross, dass selbst 'moderne' Orchester historische Erkenntnisse in ihre Interpretationen einbeziehen."
    ),

    Question(
        categoryId = 5,
        questionText = "Welche Spieltechnik-Praxis unterscheidet die Barockgeige von der modernen Konzertgeige grundlegend?",
        answerA = "Barocke Geiger spielten ohne Kinnhalter und nutzten einen kuerzeren, leichteren Bogen mit anderem Gleichgewichtspunkt",
        answerB = "Barockgeiger spielten ausschliesslich im Flageolett und vermieden gegriffene Toene",
        answerC = "Barocke Geiger nutzten eine voellig andere Griffweise mit dem Daumen auf dem Griffbrett",
        answerD = "Barockgeige hatte keinen Steg und wurde wie eine Viola da gamba in der Knie gehalten",
        correctAnswer = 0,
        explanation = "Wesentliche Unterschiede: 1) Kein Kinnhalter (erst 1820 erfunden), die Geige wurde am Hals zwischen Kinn und Schulter gehalten. 2) Darmsaiten statt Metallsaiten — weniger Spannung, weicherer Klang. 3) Barockbogen (Tourte-Bogen noch nicht erfunden) — kuerzer, leichter, mit konvexer Stange. 4) Tieferer Steg, weniger Bogendruck.",
        difficulty = 4,
        funFact = "Die moderne Geigen-Haltetechnik mit Kinnhalter und Schulterkissen entstand erst im 19. Jahrhundert durch den Wunsch nach lauterem Spiel in grossen Hallen. Barockgeiger hatten bei bestimmten Passagen mehr Freiheit, da die Geige weniger fixiert war — ermoeglichte bestimmte Ornamente und Lagenwechsel."
    ),

    Question(
        categoryId = 5,
        questionText = "Was versteht man unter 'Basso Continuo' (Generalbass) in der Barockmusik und wer spielte ihn typischerweise?",
        answerA = "Die tiefste Instrumentalstimme eines Orchesters, gespielt von Kontrabass und Posaune",
        answerB = "Eine bezifferte Bassstimme, die Harmoniker (Cembalo, Orgel, Laute) zur Improvisierung von Akkorden nutzen, unterstuetzt von Bassinstrumenten",
        answerC = "Eine durchgehende Rhythmus-Basslinie im Stile des Ostinato, die alle Saetze eines Suites verbindet",
        answerD = "Die Komposition-Technik, Basslinien als kanonische Vorimitation der Melodie zu verwenden",
        correctAnswer = 1,
        explanation = "Basso Continuo (Generalbass) ist die Basis-Praxis des Barock: Eine Bassstimme wird von Harmonieinstrumenten (Cembalo, Laute, Theorbe, Orgel) durch Akkorde ausgefuellt, wobei Ziffern unter den Noten die Harmonik andeuten. Ergaenzt wird dies durch Melodiebass-Instrumente (Cello, Fagott, Violone).",
        difficulty = 4,
        funFact = "Der Generalbass war die 'Jazzimprovisation' des Barock: Cembalisten improvisierten Akkorde aus den Bassziffern, mit eigenem Ornament und Stil. Kein Basso-Continuo-Spiel war identisch — jeder Musiker brachte seine eigene Harmonisierungskunst ein. Das Aufschreiben dieser Improvisationen war nicht vorgesehen."
    ),

    Question(
        categoryId = 5,
        questionText = "Was ist 'Musica Ficta' in der mittelalterlichen und Renaissance-Auffuehrungspraxis?",
        answerA = "Fiktive, also nicht aufgefuehrte Musik, die nur in Traktaten beschrieben wird",
        answerB = "Die Praxis, bestimmte Toene (besonders H zu B oder F zu Fis) ausserhalb der Hexachord-Normen zu alterieren, um verbotene Quarten zu vermeiden",
        answerC = "Erfundene Texte fuer liturgische Musik, wenn der originale Text verloren war",
        answerD = "Die Notation von Ornamente als ausgeschriebene 'fiktive' Noten statt als Zeichen",
        correctAnswer = 1,
        explanation = "Musica Ficta bezeichnet die in der Praxis vorgenommenen Alterationen (Erhoehung oder Erniedrigung von Toenen), die im Notentext nicht angegeben, aber durch theoretische Regeln impliziert waren: Vermeidung des Tritonus ('diabolus in musica'), Leittonbewegungen vor Kadenzen, Hexachord-Anpassungen.",
        difficulty = 4,
        funFact = "Das Problem der Musica Ficta ist eines der groessten Raetsel der historischen Auffuehrungspraxis: Wir wissen, dass Alterationen vorkamen, aber oft nicht genau wo. Zwei Ensembles spielen dieselbe mittelalterliche Motette und eine klingt in e-Moll, die andere in E-Dur — beide koennen historisch korrekt sein."
    ),

    Question(
        categoryId = 5,
        questionText = "Welche Stimmungssysteme waren in der Barockzeit gebrae uchlich und warum wurden sie spaeter durch die gleichstuefige Stimmung abgeloest?",
        answerA = "Natuerliche Stimmung und Pythagoraische Stimmung; abgeloest wegen technischer Unmoglichkeit der Chromatik",
        answerB = "Mitteltoenige Stimmung und Wohltemperierte Stimmungen (z.B. Werckmeister III); abgeloest wegen Vereinheitlichung bei Klavierbau und modulationsreicherer Musik",
        answerC = "Just Intonation fuer alle Instrumente; abgeloest wegen mangelnder Praezision bei Blasinstrumenten",
        answerD = "Arabische Maqam-Stimmung adaptiert fuer europaische Instrumente; abgeloest durch franzoesische Aufklaerungsreformen",
        correctAnswer = 1,
        explanation = "Im Barock herrschten verschiedene Temperaturen: Mitteltoenige Stimmung (reine grosse Terzen, aber enge Quinten) war gut fuer diatonische Musik, schlecht fuer entfernte Tonarten. Wohltemperierte Systeme (Werckmeister, Kirnberger) ermoeglichten alle 24 Tonarten mit individuellen Klangcharakteren. Gleichstufige Stimmung (alle Quinten 2 Cent zu eng) vereinheitlichte alle Tonarten.",
        difficulty = 4,
        funFact = "Bachs 'Wohltemperiertes Klavier' war kein Beweis fuer gleichstufige Stimmung, sondern fuer 'wohltemperierte' Systeme, die alle Tonarten spielbar machten. Jede Tonart hatte einen eigenen Charakter — C-Dur klang anders als cis-Moll. In gleichstufiger Stimmung klingt alles gleich schwebend: Tonartencharaktere sind weg."
    ),

)

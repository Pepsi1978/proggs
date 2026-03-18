package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun sportQuestionsExpert(): List<Question> = listOf(

    // --- SPORTWISSENSCHAFT / BIOMECHANIK (10 Fragen) ---

    Question(
        categoryId = 6,
        questionText = "Welche biomechanische Groesse beschreibt das Verhaeltnis von Kraft zu Querschnittsflaeche eines Muskels und bestimmt dessen maximale Kontraktionskraft?",
        answerA = "Muskelfaserdichte",
        answerB = "Spezifische Muskelkraft",
        answerC = "Pennationswinkel",
        answerD = "Elastizitaetsmodul",
        correctAnswer = 1,
        explanation = "Die spezifische Muskelkraft (N/cm2) beschreibt die Kraft pro Querschnittsflaeche und betraegt beim Menschen etwa 15-30 N/cm2, unabhaengig von Trainingsstand.",
        difficulty = 4,
        funFact = "Trotz der scheinbaren Kraft-Grenze durch die spezifische Muskelkraft koennen Kraftsportler mehr heben, weil sie mehr Muskelmasse und bessere neuronale Ansteuerung haben."
    ),

    Question(
        categoryId = 6,
        questionText = "Der Stretch-Shortening-Cycle (SSC) nutzt elastische Energie aus dem dehnungsreflexvermittelten Muskel-Sehnen-Komplex. Welches Protein speichert dabei primaer elastische Energie in der Sehne?",
        answerA = "Myosin",
        answerB = "Aktin",
        answerC = "Titin",
        answerD = "Kollagen Typ I",
        correctAnswer = 2,
        explanation = "Titin ist ein Riesenprotein (Molekulargewicht ca. 3.000 kDa), das im Sarkomer als molekulare Feder fungiert und wesentlich zur Speicherung elastischer Energie beim SSC beitraegt.",
        difficulty = 4,
        funFact = "Das Titin-Gen ist mit 363 Exons das groesste bekannte Gen im menschlichen Genom und kodiert fuer das schwerste bekannte Protein."
    ),

    Question(
        categoryId = 6,
        questionText = "Bei welchem Gelenkwinkel (Knieflexion) erzeugt der Quadrizeps femoris sein maximales Drehmoment unter isometrischen Bedingungen?",
        answerA = "0-10 Grad (volle Extension)",
        answerB = "60-70 Grad",
        answerC = "90-100 Grad",
        answerD = "120-130 Grad",
        correctAnswer = 1,
        explanation = "Das maximale isometrische Drehmoment des Quadrizeps liegt bei ca. 60-70 Grad Knieflexion, da hier das Laengen-Spannungs-Verhaeltnis und der mechanische Hebelarm des Patellasehne optimal zusammenwirken.",
        difficulty = 4,
        funFact = "Dieses Wissen ist entscheidend fuer die Rehabilitation nach Kreuzbandriss, da in bestimmten Winkelbereichen die Schubkraft auf das Knie besonders hoch ist."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie bezeichnet man den physiologischen Mechanismus, durch den ein hochintensives Intervalltraining (HIIT) den Nachbrenneffekt (EPOC) erzeugt?",
        answerA = "Glykolytische Phosphorylierung",
        answerB = "Mitochondriale Biogenese",
        answerC = "Sauerstoffschuld durch PCr-Resynthese und Laktatabbau",
        answerD = "Anabole Proteinsynthese",
        correctAnswer = 2,
        explanation = "EPOC (Excess Post-exercise Oxygen Consumption) entsteht primaer durch die Resynthese von Phosphokreatin (PCr), den aeroben Laktatabbau, erhoehte Koerpertemperatur und Katecholamin-Ausschuettung.",
        difficulty = 4,
        funFact = "Der EPOC-Effekt nach HIIT kann bis zu 24-48 Stunden anhalten, ist aber absolut gesehen oft kleiner als oft behauptet – typischerweise nur 50-150 kcal extra."
    ),

    Question(
        categoryId = 6,
        questionText = "Was beschreibt der Begriff 'Muskelquerschnitt vs. Muskelquantitaet' im Kontext von Hypertrophie? Welcher Hypertrophietyp dominiert bei Kraftsport?",
        answerA = "Hyperplasie (Neubildung von Muskelfasern)",
        answerB = "Myofibrillaere Hypertrophie (Zunahme kontraktiler Proteine)",
        answerC = "Sarkoplasmatische Hypertrophie (Zunahme nicht-kontraktiler Fluessigkeit)",
        answerD = "Transiente Hypertrophie (Pump-Effekt durch Blutansammlung)",
        correctAnswer = 1,
        explanation = "Beim Kraftsport dominiert die myofibrillaere Hypertrophie: Zunahme von Aktin- und Myosin-Filamenten im Sarkomer. Die sarkoplasmatische Hypertrophie (mehr Glykogen, Wasser) tritt staerker bei Bodybuilding-Protokollen auf.",
        difficulty = 4,
        funFact = "Echte Hyperplasie (Muskelfaser-Neubildung) ist beim Menschen umstritten und wurde nur in Einzelstudien mit extremem Training nachgewiesen."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Konzept beschreibt den optimalen Trainingszustand direkt nach einer Supersaturation-Phase (Tapering) vor einem Wettkampf?",
        answerA = "Uebertraining-Syndrom",
        answerB = "Funktionelles Ueberreichen (Functional Overreaching)",
        answerC = "Peaking durch Superkompensation",
        answerD = "Detraining-Plateau",
        correctAnswer = 2,
        explanation = "Das Peaking durch Superkompensation bezeichnet den Zustand maximaler Leistungsbereitschaft nach einer gezielten Erholungsphase (Tapering), bei der der Koerper auf eine vorangegangene Belastungsphase reagiert.",
        difficulty = 4,
        funFact = "Das Tapering vor einem Wettkampf verkuerzt typischerweise das Trainingsvolumen um 40-60%, waehrend die Intensitaet erhalten bleibt, um Leistungsgewinne von 2-8% zu erzielen."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Parameter der Herzratenvariabilitaet (HRV) gilt als sensibelster Indikator fuer das autonome Nervensystem und wird am haeufigsten in der Sportwissenschaft verwendet?",
        answerA = "RMSSD (Root Mean Square of Successive Differences)",
        answerB = "SDNN (Standard Deviation of NN Intervals)",
        answerC = "pNN50 (Percentage of successive NN intervals >50 ms)",
        answerD = "LF/HF-Ratio (Low Frequency / High Frequency Power)",
        correctAnswer = 0,
        explanation = "RMSSD reflektiert primaer die parasympathische (vagale) Aktivitaet und ist stabiler bei Kurzzeitmessungen. Er gilt als Goldstandard fuer die Erholungsbeurteilung bei Sportlern.",
        difficulty = 4,
        funFact = "Profisportler zeigen oft deutlich hoehere RMSSD-Werte als Untrainierte, da regelmaeessiges Ausdauertraining den Vagotonus erhoht."
    ),

    Question(
        categoryId = 6,
        questionText = "Was versteht man unter dem 'Henneman-Groessenordnungs-Prinzip' (Size Principle) bei der motorischen Einheitenrekrutierung?",
        answerA = "Groessere Muskeln werden immer zuerst aktiviert",
        answerB = "Motorische Einheiten werden von kleinsten zu groessten rekrutiert",
        answerC = "Schnell-Zuckende Fasern (Typ IIx) werden immer zuerst aktiviert",
        answerD = "Die Rekrutierungsreihenfolge haengt von der Bewegungsgeschwindigkeit ab",
        correctAnswer = 1,
        explanation = "Das Henneman-Prinzip (1965): Motorische Einheiten werden gemaess ihrer Groesse rekrutiert – erst kleine, langsam-zuckende (Typ I), dann mittlere (Typ IIa), zuletzt grosse, schnell-zuckende (Typ IIx) Einheiten.",
        difficulty = 4,
        funFact = "Das Prinzip erklaert, warum Ausdauertraining die Typ-I-Fasern bevorzugt trainiert und maximal intensive Uebungen fuer die Rekrutierung von Typ-IIx-Fasern notwendig sind."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher biomechanische Vorteil hat eine hohe Sehnen-Compliance (Nachgiebigkeit) fuer Laeuf er bei der Energierueckgewinnung?",
        answerA = "Hoehere Laufgeschwindigkeit durch steifere Sehnen",
        answerB = "Bessere Stossdaempfung ohne Energierueckgewinnung",
        answerC = "Effiziente Speicherung und Rueckgabe elastischer Energie im Laufzyklus",
        answerD = "Reduktion der Muskelkraft durch Federung",
        correctAnswer = 2,
        explanation = "Compliance der Achillessehne erlaubt die Speicherung kinetischer Energie in der Landephase als elastische Energie, die dann in der Abstosspahse zurueckgegeben wird. Dies reduziert den aktiven Muskelenergieeinsatz erheblich.",
        difficulty = 4,
        funFact = "Die Achillessehne kann bis zu 35% der Energie eines jeden Laufschrittes speichern und zurueckgeben, was Laufen deutlich effizienter macht als Gehen."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Konzept beschreibt die 'Laktatschwelle 2' (LT2) bzw. 'anaerobe Schwelle' und wie wird sie in der sportmedizinischen Diagnostik praeziese bestimmt?",
        answerA = "4 mmol/L Laktat im Blut als fixe universelle Schwelle",
        answerB = "Der Punkt maximalen Laktat-Steady-States (MLSS) bei 30-minuetiger Dauerbelastung",
        answerC = "Beginn der Laktatbildung im Muskel",
        answerD = "Herzfrequenz bei 85% der maximalen Herzfrequenz",
        correctAnswer = 1,
        explanation = "Die Laktatmaximal-Steady-State (MLSS) gilt als genaueste Methode: Die hoechste Belastungsintensitaet, bei der Blutlaktat ueber 30 Minuten stabil bleibt (Anstieg < 1 mmol/L). Die fixe 4-mmol-Schwelle ist eine Naeherung und individuell variabel.",
        difficulty = 4,
        funFact = "Weltklasse-Ausdauerathleten koennen bis zu 8 mmol/L Laktat im Steady-State tolerieren, waehrend untrainierte Personen bereits bei 2 mmol/L ein MLSS erreichen."
    ),

    // --- OLYMPIA-HISTORIE / IOC-POLITIK (10 Fragen) ---

    Question(
        categoryId = 6,
        questionText = "Welche IOC-Sitzung 1894 in Paris gruendete die modernen Olympischen Spiele und wer war der erste IOC-Praesident?",
        answerA = "Demetrius Vikelas (Griechenland)",
        answerB = "Pierre de Coubertin (Frankreich)",
        answerC = "William Penny Brookes (England)",
        answerD = "Evangelos Zappas (Griechenland)",
        correctAnswer = 0,
        explanation = "Demetrius Vikelas war der erste IOC-Praesident (1894-1896). Pierre de Coubertin war zwar der Hauptinitiator der Bewegung und uebernahm das Praesidentenamt 1896, aber Vikelas hatte den ersten Vorsitz.",
        difficulty = 4,
        funFact = "Pierre de Coubertin war ueberzeugt, dass Griechenland die Olympischen Spiele dauerhaft ausrichten sollte, musste aber gegenueber praktischen Argumenten fuer einen rotierenden Veranstaltungsort nachgeben."
    ),

    Question(
        categoryId = 6,
        questionText = "Bei welchen Olympischen Spielen wurde das erste Mal das IOC-Doping-Kontrollprogramm systematisch eingefuehrt, und welche Substanz fuehrte zum ersten positiven Test eines Goldmedaillengewinners?",
        answerA = "Mexiko 1968 – Amphetamine (Hans-Guenther Lilge)",
        answerB = "Muenchen 1972 – Ephedrin (Rick DeMont)",
        answerC = "Montreal 1976 – Anabolika (Danuta Rosani)",
        answerD = "Seoul 1988 – Stanozolol (Ben Johnson)",
        correctAnswer = 3,
        explanation = "Obwohl Doping-Tests seit Mexiko 1968 existierten, war Ben Johnsons positiver Stanozolol-Test in Seoul 1988 der erste aufsehenerregende Fall eines Goldmedaillengewinners im 100m-Sprint, der das globale Bewusstsein veraenderte.",
        difficulty = 4,
        funFact = "Ben Johnsons 9,79 Sekunden in Seoul galten als 'Weltrekord', wurden aber aberkannt. Der offizielle Weltrekord wurde erst 2009 von Usain Bolt mit 9,58 Sekunden gebrochen."
    ),

    Question(
        categoryId = 6,
        questionText = "Was war der Kerninhalt der 'Declaration of Amateur Status' und wann wurde das Amateurprinzip offiziell aus der Olympischen Charta gestrichen?",
        answerA = "1971 – Profisportler durften nur nach IOC-Sondergenehmigung teilnehmen",
        answerB = "1981 – Das Amateurprinzip wurde vollstaendig aus den Eligibility-Regeln entfernt",
        answerC = "1992 – NBA-Profis durften erstmals am Basketball-Turnier teilnehmen",
        answerD = "2000 – Erstmalige Zulassung aller Profisportler ohne Ausnahme",
        correctAnswer = 1,
        explanation = "1981 strich das IOC unter Juan Antonio Samaranch das Wort 'Amateur' aus der Olympischen Charta. Ab 1992 (Barcelona) durften NBA-Profis im Basketball teilnehmen, was die praktische Umsetzung markierte.",
        difficulty = 4,
        funFact = "Jim Thorpe, zweifacher Goldmedaillengewinner 1912, wurde nachtraeglich disqualifiziert, weil er zuvor als Baseballspieler Geld verdient hatte – ein klassisches Opfer des Amateurprinzips."
    ),

    Question(
        categoryId = 6,
        questionText = "Welcher Konflikt fuehrte zum groeessten Olympia-Boykott der Geschichte (1980 Moskau) und welche Nationen folgten dem US-Boykottaufruf?",
        answerA = "Vietnamkrieg – 32 Nationen boykottierten",
        answerB = "Sowjetischer Einmarsch in Afghanistan – ca. 65 Nationen boykottierten",
        answerC = "Apartheid in Suedafrika – 28 Nationen boykottierten",
        answerD = "Kalter-Krieg-Eskalation – 80 Nationen boykottierten",
        correctAnswer = 1,
        explanation = "Als Reaktion auf den sowjetischen Einmarsch in Afghanistan (Dezember 1979) riefen die USA unter Praesident Carter zum Boykott auf. Ca. 65-66 Nationen blieben fern, obwohl einige Athleten unter olympischer Flagge teilnahmen.",
        difficulty = 4,
        funFact = "Als Gegenmassnahme boykottierte die Sowjetunion die Spiele 1984 in Los Angeles. Der Ost-Boykott umfasste 14 Staaten des Ostblocks und hinterliess viele Disziplinen ohne die weltbesten Athleten."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches IOC-Reformprogramm 'Agenda 2020' verabschiedete das IOC 2014 und welche fundamentale Aenderung brachte es beim Bewerbungsverfahren?",
        answerA = "Einfuehrung von Quotenregelungen fuer Frauen",
        answerB = "Dialog-basiertes Verfahren statt Wettbewerb um Ausrichtungsrechte",
        answerC = "Obligatorische Anti-Doping-Labors in Ausrichterstaedten",
        answerD = "Begrenzung auf maximal 10.000 Athleten pro Spiele",
        correctAnswer = 1,
        explanation = "Agenda 2020 ersetzte das kompetitive Ausschreibungsverfahren durch einen 'zielgerichteten Dialog' mit potenziellen Ausrichtern. Dies sollte kostspielige Bewerbungsrennen reduzieren und neue Bewerber anziehen.",
        difficulty = 4,
        funFact = "Die nachfolgende 'Agenda 2020+5' (2021) und die 'Olympic Agenda 2032' setzen diesen Weg fort und erlauben nun auch kontinuierlich rollende Vergaben ohne feste Bewerbungsfristen."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Entscheidung des IOC ermoeglichte es Russland, trotz staatlichen Doping-Programmes unter neutraler Flagge an den Spielen 2016 in Rio teilzunehmen?",
        answerA = "Vollstaendiger Freispruch durch CAS nach WADA-Untersuchung",
        answerB = "IOC lehnte einen Pauschalboykott ab und ueberliess die Entscheidung den Sportfachverbaenden",
        answerC = "Russland erkannte alle Versteosse an und kooperierte vollstaendig",
        answerD = "Politische Intervention der UN verhinderte einen Ausschluss",
        correctAnswer = 1,
        explanation = "Das IOC entschied, keinen Pauschalausschluss zu verhaengen, sondern die Entscheidung ueber individuelle Zulassungen den Internationalen Sportfachverbaenden zu ueberlassen. Dies war hochumstritten und fuehrte zu unterschiedlicher Handhabung.",
        difficulty = 4,
        funFact = "Der McLaren-Report (2016) enthullte ein staatlich gelenktes Doping-System bei den Winterspielen Sotschi 2014, bei dem Proben durch ein Loch in der Wand ausgetauscht wurden."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Gremium ist fuer die Aufnahme neuer Sportarten ins Olympische Programm zustaendig und nach welchen Kriterien wird entschieden?",
        answerA = "Generalversammlung aller Nationalen Olympischen Komitees",
        answerB = "IOC-Session auf Vorschlag der IOC-Exekutivkommission",
        answerC = "WADA und die zustaendigen Internationalen Sportfachverbaende gemeinsam",
        answerD = "Abstimmung der teilnehmenden Nationen der letzten Spiele",
        correctAnswer = 1,
        explanation = "Die IOC-Session (Vollversammlung aller IOC-Mitglieder) entscheidet auf Empfehlung der Exekutivkommission. Kriterien umfassen weltweite Verbreitung, Popularitaet, Anti-Doping-Compliance und programmatische Passung.",
        difficulty = 4,
        funFact = "Skateboard, Surfen, Sport Climbing und Karate wurden fuer Tokio 2020 aufgenommen. Karate wurde fuer Paris 2024 wieder gestrichen, waehrend Breakdance neu hinzukam."
    ),

    Question(
        categoryId = 6,
        questionText = "Was war die rechtliche Grundlage des Ausschlusses Suedafrikas von den Olympischen Spielen 1964-1992 und welche Organisation koordinierte diesen Ausschluss?",
        answerA = "UN-Resolution 2397 – direkt durchgesetzt durch die UN-Generalversammlung",
        answerB = "Olympische Charta Regel 50 – koordiniert durch das Supreme Council for Sport in Africa",
        answerC = "IOC-Entscheidung auf Basis der Olympischen Charta – unterstuetzt durch SANOC-Aufloesung",
        answerD = "Internationaler Gerichtshof Den Haag – koordiniert durch OAU",
        correctAnswer = 2,
        explanation = "Das IOC schloss Suedafrika 1970 aus wegen Verletzung der Nicht-Diskriminierungsprinzipien der Olympischen Charta. Der Supreme Council for Sport in Africa koordinierte internationalen Druck und drohte mit Massenboykott.",
        difficulty = 4,
        funFact = "Suedafrika wurde nach dem Ende der Apartheid und den ersten freien Wahlen 1994 wieder in die olympische Gemeinschaft aufgenommen und nahm 1992 in Barcelona erstmals als vereintes Team teil."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Regelung der Olympischen Charta verbietet politische Demonstrationen bei den Spielen und welche Athleten demonstrierten 1968 in Mexiko trotzdem auf dem Siegertreppchen?",
        answerA = "Regel 50 – Tommie Smith und John Carlos (USA, 200m)",
        answerB = "Regel 46 – Peter Norman und Lee Evans (Australien/USA)",
        answerC = "Regel 50 – Bob Beamon und Dick Fosbury (USA)",
        answerD = "Bylaw 1 – Wyomia Tyus und Mildrette Netter (USA)",
        correctAnswer = 0,
        explanation = "Tommie Smith (Gold) und John Carlos (Bronze) streckten die Faeuste in schwarzen Handschuhen beim 200m-Finale in die Hoehe – ein Symbol der Black Power Bewegung. Regel 50 verbietet politische Manifestationen, wofuer beide disqualifiziert wurden.",
        difficulty = 4,
        funFact = "Peter Norman, der australische Silbermedaillengewinner, trug als Geste der Solidaritaet ebenfalls ein OPHR-Abzeichen (Olympic Project for Human Rights) auf dem Siegertreppchen."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Dokument regelt die Beziehung zwischen IOC, Nationalen Olympischen Komitees (NOKs) und Internationalen Sportfachverbaenden (IFs) und definiert die Olympische Bewegung?",
        answerA = "Lausanner Vertrag fuer Sportrecht (1999)",
        answerB = "Olympische Charta (urspruenglich 1908 als 'Olympic Rules' formuliert)",
        answerC = "Brighton Declaration on Women and Sport (1994)",
        answerD = "Welt-Anti-Doping-Code (WADC, 2003)",
        correctAnswer = 1,
        explanation = "Die Olympische Charta ist das kodifizierte Grundprinzip-Dokument, das die Organisation und Leitung der Olympischen Bewegung regelt. Sie wurde in verschiedenen Fassungen weiterentwickelt, basiert aber auf fruehen Regeln von Coubertin.",
        difficulty = 4,
        funFact = "Die Olympische Charta enthalt auch das 'IOC-Regelwerk 40', das Athleten in der Olympia-Zeit (9 Tage vor und 3 Tage nach den Spielen) in ihrer Werbemoeglichkeit einschraenkt."
    ),

    // --- FUSSBALL-TAKTIK-ANALYSE (8 Fragen) ---

    Question(
        categoryId = 6,
        questionText = "Was versteht man unter dem taktischen Konzept des 'Gegenpressings' (Gegenpress) und welchem Trainer wird seine Popularisierung im modernen Fussball zugeschrieben?",
        answerA = "Sofortiges Pressen nach Ballverlust im Mittelfeld – Ralf Rangnick",
        answerB = "Hoher Pressing-Ansatz in der gegnerischen Haelfte – Johan Cruyff",
        answerC = "Defensives Kompaktstehen nach Ballverlust – Jose Mourinho",
        answerD = "Konterangriffe nach Ballgewinn – Pep Guardiola",
        correctAnswer = 0,
        explanation = "Gegenpress bezeichnet das unmittelbare, intensive Pressen direkt nach Ballverlust, um den Gegner am geordneten Aufbau zu hindern. Ralf Rangnick bei RB Salzburg/Leipzig und Juergen Klopp (beeinflusst von Rangnick) popularisierten es im modernen Fussball.",
        difficulty = 4,
        funFact = "Juergen Klopp beschrieb Gegenpress als seinen 'besten Spielmacher': 'Kein Spielmacher der Welt kann so schnell schalten wie ein gutes Gegenpressing-System.'"
    ),

    Question(
        categoryId = 6,
        questionText = "Was beschreibt das taktische Konzept der 'Halbposition' im modernen Fussball und welche Spielerposition nutzt sie am meisten?",
        answerA = "Spieler zwischen Aussenverteidiger und Innenverteidiger – Libero",
        answerB = "Spieler zwischen gegnerischen Linien im Halbraum – Halbstuermer/Zehner",
        answerC = "Mittelfeldspieler als Bindeglied – defensiver Mittelfeldspieler",
        answerD = "Spielmacher hinter der Spitze – klassischer Nummer 10",
        correctAnswer = 1,
        explanation = "Die Halbposition ('Halbraum') bezeichnet den Raum zwischen den Seitlinien und dem Zentrum. Spieler in dieser Position (oft inverted Winger oder Zehner) koennen in Schussposition dribbeln oder direkte Paesse spielen.",
        difficulty = 4,
        funFact = "Arjen Robben war der Meister der Halbraum-Nutzung: Als Rechtsaussenlaeufer zog er stets auf seinen linken Fuss und erzielte in dieser Bewegung beinahe mythologische Tore."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches Konzept beschreibt den taktischen Ansatz, bei dem Aussenverteidiger ins Mittelfeld oder sogar in den Sturm einruecken, waehrend Innenverteidiger breiter werden?",
        answerA = "Doppel-Pivot-System",
        answerB = "False-Full-Back (inverter Aussenverteidiger)",
        answerC = "Rauten-Mittelfeld",
        answerD = "Dreier-Abwehrkette mit Wingbacks",
        correctAnswer = 1,
        explanation = "Der False-Full-Back (Pep Guardiola bei Manchester City) laesst Aussenverteidiger (z.B. Trent Alexander-Arnold) ins Mittelfeld einruecken, wodurch numerische Ueberlegenheit im Mittelfeld und Breite durch Innenverteidiger erzeugt wird.",
        difficulty = 4,
        funFact = "Diese Taktik veroeffentlichte Pep Guardiola zuerst mit Joshua Kimmich in der Bundesliga (2019/20), dann systematisch mit Joao Cancelo und Alexander-Arnold bei Manchester City/Liverpool."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist der 'Expected Goals' (xG) Wert und welche Faktoren fliessen in ein typisches xG-Modell ein?",
        answerA = "Durchschnittliche Tore pro Spiel auf Basis historischer Daten",
        answerB = "Wahrscheinlichkeit eines Torschusses basierend auf Position, Winkel, Abstand und weiteren Faktoren",
        answerC = "Tore geteilt durch Schussanzahl als Effizienzquotient",
        answerD = "Gewichtetes Mittel aus Besitz und Torschussqualitaet",
        correctAnswer = 1,
        explanation = "xG gibt fuer jeden Schuss die historische Wahrscheinlichkeit an, dass dieser ein Tor wird. Faktoren: Distanz zum Tor, Schusswinkel, Kopf vs. Fuss, vorhergehende Passsituation (Assist-Typ), Torhueter-Position.",
        difficulty = 4,
        funFact = "Lionel Messi erzielte in seiner Barcelona-Zeit regelmaeessig mehr Tore als sein xG erwarten liess – ein Indikator fuer aussergewoehnliche Finesse, die statistische Modelle nicht vollstaendig erfassen."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches taktische Prinzip beschreibt das Aufrechterhalten von Kompaktheit durch vertikales und horizontales Enge-Halten der Mannschaftsteile und wird vor allem bei defensiven Systemen angewandt?",
        answerA = "High Block (hoher Pressing-Block)",
        answerB = "Mid Block (mittlerer Block)",
        answerC = "Low Block (tiefer Abwehrblock)",
        answerD = "Manndeckung (Man-Marking)",
        correctAnswer = 2,
        explanation = "Der Low Block bezeichnet eine tiefe, kompakte Defensivstruktur nah am eigenen Tor. Teams lassen den Gegner ins Mittelfeld kommen und verteidigen in engen Raumen. Typisch fuer defensiv ausgerichtete Aussenseiter gegen dominante Teams.",
        difficulty = 4,
        funFact = "Jose Mourinhos Inter Mailand schlug im CL-Halbfinale 2010 den FC Barcelona (Pep Guardiola) mit einem perfekten Low Block - ein Lehrbuchspiel dieser Taktik."
    ),

    Question(
        categoryId = 6,
        questionText = "Was beschreibt der Begriff 'PPDA' (Passes Allowed Per Defensive Action) in der modernen Fussball-Analytik?",
        answerA = "Passerfolgsrate in der letzten Felddrittel",
        answerB = "Massstaab fuer die Pressing-Intensitaet eines Teams",
        answerC = "Durchschnittliche Paesse bis zum Abschluss",
        answerD = "Verhaeltnis von vertikalen zu horizontalen Paessen",
        correctAnswer = 1,
        explanation = "PPDA misst die Pressing-Intensitaet: niedrigerer PPDA bedeutet mehr Pressing. Formel: Paesse des Gegners im eigenen Drittel / defensive Aktionen (Tacklings, Abfangen, Fouls) des pressenden Teams.",
        difficulty = 4,
        funFact = "Teams mit niedrigstem PPDA in Europa waren historisch Juergen Klopps Liverpool und Marcelo Bielsa's Leeds – beide bekannt fuer intensives Pressing."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Voraussetzung muss ein Spieler erfuellen, um als 'False 9' zu gelten, und welchem Trainer wird die taktische Entwicklung dieser Rolle zugeschrieben?",
        answerA = "Ein offensiver Mittelfeldspieler der als Stuermerspitze spielt – Pep Guardiola",
        answerB = "Ein Stuermerspitze der in tiefe Raeume faellt und die eigentliche Nummer 9 ersetzt – Pep Guardiola (mit Messi)",
        answerC = "Ein Mittelfeldspieler der defensiv und offensiv gleichermassen agiert – Johan Cruyff",
        answerD = "Ein Fluegelstuermer der zentral spielt – Marcelo Bielsa",
        correctAnswer = 1,
        explanation = "Pep Guardiola stellte Lionel Messi 2009/10 in der Mitte als Stuermerspitze auf, ohne echter Nummer 9 zu sein. Messi liess sich fallen, zog Verteidiger aus der Kette und schuf Raeume fuer Xavi/Iniesta.",
        difficulty = 4,
        funFact = "Tschechoslowakei-Legende Josef Masopust und Ungarn-Spieler Nandor Hidegkuti der 1950er gelten als fruehe Vorlaeufer des False-9-Konzepts, lange vor Guardiola."
    ),

    Question(
        categoryId = 6,
        questionText = "Was versteht man unter 'Ballbesitzfussball' (Possession Football) versus 'Transitionsfussball' und welcher Metriken-Unterschied kennzeichnet beide Ansaetze am deutlichsten?",
        answerA = "Ballbesitz > 60% vs. schnelle Konter mit weniger als 4 Paessen",
        answerB = "Anzahl kurzer Paesse vs. Anzahl langer Paesse",
        answerC = "Pressing-Hoehe vs. Defensivlinie-Tiefe",
        answerD = "Expected Goals (xG) generiert aus Ballbesitz vs. aus Gegenpressing",
        correctAnswer = 0,
        explanation = "Ballbesitz-Teams (>60% Ballbesitz, Barcelona-Stil) kontrollieren das Spiel durch Passnetzwerke. Transitions-Teams nutzen schnelle Umschaltmomente und erzielen Tore mit wenigen gezielten Paessen aus gesicherten Positionen.",
        difficulty = 4,
        funFact = "Atletico Madrid unter Diego Simeone ist das Paradebeispiel: Mit oft unter 40% Ballbesitz wurden sie zweimal Champions-League-Finalist und gewannen die Spanische Liga."
    ),

    // --- DOPING-WISSENSCHAFT (7 Fragen) ---

    Question(
        categoryId = 6,
        questionText = "Welcher biologische Passport der WADA ueberwacht Doping indirekt durch Laengszeitbeobachtung von Biomarkern und welche Werte sind primaer relevant?",
        answerA = "Testosteron/Epitestosteron-Ratio und LH-FSH-Balance",
        answerB = "Haematologisches Profil: Haemoglobin, Retikulozyten, OFF-Score",
        answerC = "Kortisol und IGF-1 als Wachstumshormonnachweise",
        answerD = "Laktatwerte und VO2max als Leistungsmarker",
        correctAnswer = 1,
        explanation = "Das Haematologische Modul des Biologischen Passes verfolgt Haemoglobin-Konzentration, Retikulozyten-Anteil und den OFF-Score. Abweichungen vom individuellen Basisprofil loesen Untersuchungen aus, auch ohne direkten Wirkstoffnachweis.",
        difficulty = 4,
        funFact = "Lance Armstrong wurde nicht durch direkten EPO-Nachweis uebermuehrt, sondern durch Zeugenaussagen. Heutige biologische Passport-Analysen haetten sein Blutprofil als hoch auffaellig markiert."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie funktioniert der Nachweis von synthetischem EPO (Erythropoetin) mittels Isotopenverhaeeltnisanalyse (IRMS)?",
        answerA = "EPO hat eine andere molekulare Masse als endogenes EPO",
        answerB = "Kohlenstoff-13/Kohlenstoff-12-Verhaeltnis unterscheidet sich zwischen synthetisch und biologisch produziertem EPO",
        answerC = "Synthetisches EPO hat einen anderen Glykosilierungsgrad als koerpereigenes EPO",
        answerD = "EPO hinterlaesst chemische Abbauprodukte, die endogenes EPO nicht hinterlaesst",
        correctAnswer = 2,
        explanation = "Der EPO-Nachweis basiert primaer auf Isoelektrischer Fokussierung: Synthetisches EPO (aus CHO-Zellen) hat andere Glykosilierungsmuster als endogenes EPO, was im Gel-Muster sichtbar wird. Die IRMS erkennt zudem Kohlenstoffisotopen-Unterschiede.",
        difficulty = 4,
        funFact = "EPO wurde erst 1989 als Doping-Substanz identifiziert, nachdem in den Jahren 1987-1990 mindestens 20 niederlaendische Radprofis unter mysteriosen Umstaenden starben – vermutlich durch EPO-Missbrauch."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist das 'Whereabouts-System' der WADA und welche rechtlichen Herausforderungen hat es in Europa erfahren?",
        answerA = "Dopingtest-Ankuendigungssystem 48 Stunden im Voraus – vom EuGH als unverhaeeltnismaessig eingestuft",
        answerB = "Aufenthaltsmeldepflicht fuer Athleten in Registered Testing Pools – Datenschutzbedenken in mehreren EU-Laendern",
        answerC = "Blutentnahme-Register fuer Hochleistungssportler – vom Deutschen Datenschutzbeauftragten gestoppt",
        answerD = "DNA-Datenbank fuer genetisches Doping – noch nicht ratifiziert in Europa",
        correctAnswer = 1,
        explanation = "Athleten im Registered Testing Pool muessen 60 Minuten taeglich verfuegbar sein und Aufenthaltsort melden. Datenschutzbedenken (DSGVO-Vereinbarkeit) wurden in Deutschland und Frankreich diskutiert; der EuGH bestaetigte 2023 die Grundrechtskonformitaet.",
        difficulty = 4,
        funFact = "Drei Whereabouts-Versaeumnisse innerhalb von 18 Monaten gelten als Verstoss gegen Anti-Doping-Regeln und koennen zu einer Sperre fuehren – auch ohne positiven Test."
    ),

    Question(
        categoryId = 6,
        questionText = "Wie wirkt Meldonium (Mildronate) physiologisch und warum wurde es 2016 auf die WADA-Verbotsliste gesetzt?",
        answerA = "Erhoehung der Erythrozytenzahl durch EPO-Aehnlichkeit",
        answerB = "Hemmung der Carnitin-Biosynthese, was beta-Oxidation modifiziert und Zellschutz unter Ischaeamie bewirkt",
        answerC = "Direkte Stimulierung der Proteinsynthese wie Anabolika",
        answerD = "Maskierung von Steroid-Metaboliten in Urinproben",
        correctAnswer = 1,
        explanation = "Meldonium hemmt die Gamma-Butyrobetain-Hydroxylase, reduziert Carnitin-Spiegel und verringert so den Fettsaeuretransport in Mitochondrien. Es soll Herzschutzeigenschaften bei Ischaeamie haben; die leistungssteigernde Wirkung ist umstritten, aber Bedenken reichten fuer die Verbotsliste.",
        difficulty = 4,
        funFact = "Maria Scharapova wurde 2016 nach einem positiven Meldonium-Test 15 Monate gesperrt. Sie beteuerte, das Mittel seit 2006 therapeutisch einzunehmen, hatte aber den Zeitpunkt der Aufnahme in die Verbotsliste uebersehen."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist 'Autologes Blut-Doping' und warum ist es besonders schwer nachzuweisen?",
        answerA = "Transfusion von Spender-Blut – schwer nachweisbar wegen schneller Verdaennung",
        answerB = "Reinfusion eigener Erythrozyten nach Einlagerung – kein Fremdprotein nachweisbar",
        answerC = "Mikrodosierung von EPO – Halbwertszeit zu kurz fuer Standardtests",
        answerD = "Sauerstofftraeger-Perfluorcarbon – haematologisch neutral",
        correctAnswer = 1,
        explanation = "Beim autologen Blut-Doping lagert ein Athlet eigenes Blut ein und re-infundiert es vor dem Wettkampf. Da keine Fremdstoffe verwendet werden, gibt es keinen direkten biochemischen Marker – der Nachweis erfolgt nur ueber Lagerungsmarker (Mikroteilchen) in den Blutbeuteln.",
        difficulty = 4,
        funFact = "Die Tour-de-France-Operation-Puerto (2006) enthuellte ein umfangreiches Blutdoping-Netzwerk. Blutbeutel von Radfahrern wurden mit Tiernamen kodiert gelagert – darunter 'Birillo' (Operacion Puerto)."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche Ausnahme erlaubt legale Einnahme verbotener Substanzen und welche Bedingungen muessen Athleten erfuellen?",
        answerA = "Terapeutische Ausnahmegenehmigung (TUE) – medizinische Notwendigkeit, keine leistungssteigernde Alternative",
        answerB = "Nationaler Fachverband kann fuer Kadersportler Dispensationen beantragen",
        answerC = "WADA erteilt auf Antrag bei internationalen Turnieren Sondergenehmigungen",
        answerD = "Athleten koennen verbotene Substanzen fuer Training aber nicht Wettkampf nutzen",
        correctAnswer = 0,
        explanation = "Terapeutische Ausnahmegenehmigungen (TUEs) erfordern: medizinische Diagnose, dass die Substanz notwendig ist, kein anderes nicht-verbotenes Mittel wirksam waere, und die Einnahme keine leistungssteigernde Wirkung ueber das normale Niveau hinaus entfaltet.",
        difficulty = 4,
        funFact = "Leaked Fancy-Bear-Hacker-Daten (2016) enthullten TUEs von Stars wie Serena Williams, Simone Biles und Bradley Wiggins. Alle Genehmigungen waren legal, zeigten aber das Spannungsfeld zwischen medizinischer Notwendigkeit und Fairness."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist 'Genetisches Doping' und welche Nachweismethoden werden fuer die Zukunft entwickelt?",
        answerA = "Manipulation von Mitochondrien-DNA zur Steigerung der Energieproduktion",
        answerB = "Einfuehrung von Genen oder Geneditierungen zur Verbesserung sportlicher Eigenschaften",
        answerC = "Verwendung von Wachstumsfaktor-Genen in Injektionen",
        answerD = "Epigenetische Modifikation durch Methylierungshemmer",
        correctAnswer = 1,
        explanation = "Genetisches Doping umfasst Gentherapie-Techniken (Virale Vektoren, CRISPR) zur Verbesserung von Muskelkraft (Myostatin-Hemmung), Ausdauer (VEGF-Hochregulierung) oder EPO-Produktion. WADA investiert in Nachweismethoden via Gen-Expressions-Profile.",
        difficulty = 4,
        funFact = "Der erste bekannte Fall von 'Gentherapie-Missbrauch' im Sport wurde 2019 veroeffentlicht: Ein deutscher Trainer soll Sportlern nicht-zugelassene CRISPR-Behandlungen angeboten haben."
    ),

    // --- SPORTRECHT / GOVERNANCE (5 Fragen) ---

    Question(
        categoryId = 6,
        questionText = "Welche Rechtsprechung des Court of Arbitration for Sport (CAS) hat das Verhaeltnis zwischen nationalem Sportrecht und internationalem Sportrecht fundamental definiert?",
        answerA = "CAS 2016/A/4643 – Sharapova: Nationales Recht geht vor internationalem Code",
        answerB = "CAS 1998/A/152 – Bosman: Freizuegigkeit gilt auch im Sportrecht",
        answerC = "CAS 2011/A/2384 – Pistorius: Sportliche Wettkampfbedingungen sind abschliessend durch IFs geregelt",
        answerD = "CAS 2005/C/841 – Canas: Arbitrationsklauseln sind bei Sportverbots-Regeln eingeschraenkt",
        correctAnswer = 2,
        explanation = "Der Pistorius-Fall (2011) stellte fest, dass der IAAF die Zustaendigkeit hat, Wettkampfregeln zu definieren, einschliesslich der Frage, ob technische Hilfsmittel einen unzulaessigen Vorteil bieten – nationale Antidiskriminierungsgesetze koennten dadurch eingeschraenkt werden.",
        difficulty = 4,
        funFact = "Oscar Pistorius gewann beim CAS und durfte an olympischen Qualifikationsturnieren teilnehmen. Er schied im Halbfinale der 400m bei den Spielen 2012 in London aus."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches EuGH-Urteil staerkte die Freizuegigkeit im europaeischen Profifussball fundamental und hatte welche konkrete Auswirkung?",
        answerA = "EuGH C-415/93 (Bosman, 1995) – Abschaffung von Transfergebuehren fuer auslaufende Vertraege und Abschaffung von Auslaenderquoten in EU",
        answerB = "EuGH C-176/96 (Lehtonen, 2000) – Transferfristen-Beschraenkungen sind rechtswidrig",
        answerC = "EuGH C-519/04 (Meca-Medina, 2006) – Dopingregeln fallen nicht unter EU-Wettbewerbsrecht",
        answerD = "EuGH C-325/08 (Olympique Lyonnais, 2010) – Ausbildungsentschaedigungen sind rechtswidrig",
        correctAnswer = 0,
        explanation = "Das Bosman-Urteil (1995) entschied, dass EU-Spieler nach Vertragsende abloesefreiechseln duerfen und EU-interne Auslaenderquoten gegen die EU-Arbeitnehmerfreizuegigkeit verstossen. Es revolutionierte den europaeischen Transfermarkt.",
        difficulty = 4,
        funFact = "Jean-Marc Bosman, der belgische Spieler, der diesen Fall ausloeste, profitierte ironischerweise kaum vom Urteil. Nach langen Rechtsstreitigkeiten hatte seine Karriere praktisch geendet."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist das 'Financial Fair Play' (FFP) der UEFA und welche Regulierung hat es im Jahr 2022 ersetzt?",
        answerA = "UEFA Club Licensing System – ersetzt durch UEFA Sustainability Framework",
        answerB = "Break-Even-Regelung (Ausgaben nicht hoeher als Einnahmen) – ersetzt durch Squad Cost Rule im neuen UEFA Club Financial Sustainability Regulations (CFSR)",
        answerC = "Schuldenobergrenze von 50 Mio. Euro – ersetzt durch Vermoegensbasiertes Limit",
        answerD = "Ausgabendeckel pro Transferfenster – durch CAS als wettbewerbswidrig aufgehoben",
        correctAnswer = 1,
        explanation = "Das originaere FFP (2011) mit seiner Break-Even-Regel wurde 2022 durch die UEFA Club Financial Sustainability Regulations (CFSR) ersetzt. Die neue 'Squad Cost Rule' begrenzt Gehaelter, Transferabschreibungen und Agentenhonorare auf 70% der Klubeinnahmen.",
        difficulty = 4,
        funFact = "Manchester City gewann 2023 vor dem CAS einen Fall gegen die UEFA bezueglich angeblicher FFP-Verstoeesse und wurde freigesprochen. Ein separates Premier-League-Verfahren ist noch anhaeengig."
    ),

    Question(
        categoryId = 6,
        questionText = "Welche internationale Konvention des Europarates bildet die voelkerrechtliche Grundlage fuer Anti-Doping-Massnahmen und wurde von der UNESCO durch welches Abkommen ergaenzt?",
        answerA = "Europaeische Anti-Doping-Konvention (1989) – UNESCO-Uebereinkommen gegen Doping im Sport (2005)",
        answerB = "Haager Konvention fuer Sportrecht (1984) – UN-Resolution zur Sportethik (1998)",
        answerC = "Madrider Protokoll gegen Doping (1991) – WADA-Gruendungsvertrag (1999)",
        answerD = "Warschauer Anti-Doping-Richtlinie (1993) – EU-Richtlinie 2004/24/EG",
        correctAnswer = 0,
        explanation = "Die Europarat-Anti-Doping-Konvention (1989) war die erste internationale Rechtsgrundlage. Das UNESCO-Uebereinkommen gegen Doping im Sport (2005) schuf erstmals eine weltweite voelkerrechtlich bindende Grundlage ausserhalb des Sports.",
        difficulty = 4,
        funFact = "Das UNESCO-Uebereinkommen (2005) machte den WADA-Code erstmals auch fuer Regierungen verbindlich, die es ratifiziert haben. Deutschland ratifizierte es 2007."
    ),

    Question(
        categoryId = 6,
        questionText = "Was besagt die 'Strict Liability'-Doktrin im Anti-Doping-Recht und welche Kritik wird an ihr geuebt?",
        answerA = "Athleten haften nur bei nachgewiesener Absicht – kritisiert fuer mangelnde Abschreckung",
        answerB = "Athleten sind verantwortlich fuer jede verbotene Substanz im Koerper, unabhaengig von Absicht – kritisiert als unverhaeeltnismaessig streng",
        answerC = "Trainer und Aerzte haften gemeinsam mit Athleten – kritisiert fuer kollektive Bestrafung",
        answerD = "Nationale Verbaende haften fuer Doping ihrer Mitglieder – kritisiert als strukturelle Verantwortung",
        correctAnswer = 1,
        explanation = "Strict Liability bedeutet: Ist eine Substanz nachweisbar, ist der Athlet schuldig – unabhaengig von Absicht, Fahrlasssigkeit oder Unwissenheit. Kritik: Unverhaeeltnismaessig bei Kontamination durch Nahrungsergaenzungsmittel oder Sabotage.",
        difficulty = 4,
        funFact = "Der Fall Caster Semenya (Hyperandrogenismus) zeigt die Grenzen sportrechtlicher Kategorien: Der EuGH musste 2025 erneut ueber die Verhaeeltnismaessigkeit von Testosteron-Grenzwerten urteilen."
    ),

    // --- SPORTSTATISTIK (5 Fragen) ---

    Question(
        categoryId = 6,
        questionText = "Welche statistische Methode wird in der modernen Sportanalytik zur Bewertung von Spielern verwendet, die kausal Faktoren von konfoundierenden Faktoren trennt?",
        answerA = "Pearson-Korrelationsanalyse",
        answerB = "Lineare Regression mit Kontrollvariablen",
        answerC = "Kausalinferenz-Methoden wie Regression Discontinuity Design oder Instrumentalvariablen",
        answerD = "Clusteranalyse fuer Spielertypen",
        correctAnswer = 2,
        explanation = "Um echte kausale Effekte (z.B. Wert eines Spielers) von Korrelationen zu trennen, werden Kausalinferenz-Methoden verwendet: Regression Discontinuity (z.B. Spieler knapp ueber/unter Vertragsschwelle), Difference-in-Differences oder Instrumentalvariablen.",
        difficulty = 4,
        funFact = "Die bahnbrechende Studie von Carreri et al. (2021) nutzte Kausalinferenz, um nachzuweisen, dass Schiedsrichter-Entscheidungen (Elfmeter) tatsaechlich messbar unfairer nach Heimniederlagestand werden."
    ),

    Question(
        categoryId = 6,
        questionText = "Was misst der 'Wins Above Replacement' (WAR)-Wert im Baseball und welches Grundproblem hat seine Anwendung auf andere Teamsportarten?",
        answerA = "Gewonnen Spiele durch einen Spieler – Problem: Baseball ist zu individuell fuer Teamuebertragung",
        answerB = "Leistung eines Spielers ueber einem hypothetischen Ersatzspieler in Siegen – Problem: Teaminteraktionen und Positionsabhaengigkeit",
        answerC = "Expected Wins basierend auf Runs – Problem: Verschiedene WAR-Formeln sind inkompatibel",
        answerD = "Offensive und defensive Leistung kombiniert – Problem: Keine einheitliche Definitionsbasis",
        correctAnswer = 1,
        explanation = "WAR quantifiziert den Wert eines Spielers in Gewinn-Einheiten ueber einem Liga-Ersatzspieler. Im Teamfuss ball scheitert die direkte Uebertragung an gegenseitigen Abhaengigkeiten, Systemanpassungen und der Schwierigkeit, individuelle Beitraege zu isolieren.",
        difficulty = 4,
        funFact = "Im Fussball existieren WAR-Aehnliche Metriken wie 'Goals Added' (American Soccer Analysis) oder 'VAEP' (Valuing Actions by Estimating Probabilities), die aber weniger etabliert sind als Baseball-WAR."
    ),

    Question(
        categoryId = 6,
        questionText = "Welches statistische Phaenomen erklaert, warum Spieler nach einer aussergewoehnlich guten Saison oft schlechter abschneiden, ohne dass sich ihre Faehigkeiten veraendert haben?",
        answerA = "Selektionsbias durch Medienberichterstattung",
        answerB = "Regression zur Mitte (Regression to the Mean)",
        answerC = "Survivorship Bias im Scouting",
        answerD = "Hawthorn-Effekt durch erhoehte Beobachtung",
        correctAnswer = 1,
        explanation = "Regression zur Mitte: Extreme Leistungen (sehr gut oder sehr schlecht) enthalten immer eine zuvaelligen Glucks-Komponente. Im Folgejahr tendiert diese Zufallskomponente zum Mittelwert, weshalb auf 'Ausnahme-Saisons' oft Rueckgaenge folgen.",
        difficulty = 4,
        funFact = "Der Sports Illustrated 'Jinx' (Athleten auf dem Cover spielen danach schlechter) ist klassisches Beispiel fuer Regression zur Mitte: Man landet nur auf dem Cover nach aussergewoehnlichen Leistungen."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist der 'Elo-Rating'-Algorithmus und wie wurde er vom Schachsport auf Mannschaftssportarten adaptiert?",
        answerA = "Punktesystem basierend auf gewonnenen Spielen – Adaptation durch gewichtete Torverhaeltnisse",
        answerB = "Wahrscheinlichkeitsbasiertes Bewertungssystem fuer relative Staerke – Adaptation durch Match-Gewichtung und K-Faktor-Anpassung",
        answerC = "Elo-Punkte basieren auf direkten Vergleichen – Adaptation durch Head-to-Head-Statistiken",
        answerD = "Ranking-System basierend auf Gegner-Qualitaet – direkt uebertragbar ohne Anpassung",
        correctAnswer = 1,
        explanation = "Das Elo-System berechnet Erwartungswerte aus Ratingunterschieden und passt Ratings nach Ergebnissen an. Im Fussball wird es durch K-Faktor-Variationen (hoeherer K = starkere Anpassung) und Gewichtung nach Turnierart adaptiert.",
        difficulty = 4,
        funFact = "FiveThirtyEight nutzte ein angepasstes Elo-System fuer Fussball-WM-Vorhersagen. Die FIFA selbst wechselte 2018 von einem problematischen Punktesystem zu einem Elo-basierten Ranking."
    ),

    Question(
        categoryId = 6,
        questionText = "Was versteht man unter 'Clutch Performance' in der Sportstatistik und was sagen Studien ueber ihre Existenz als stabiles Persoenlichkeitsmerkmal?",
        answerA = "Statistisch klar nachgewiesene Faehigkeit bestimmter Spieler, in Drucksituationen besser zu performen",
        answerB = "Statistisch weitgehend nicht nachgewiesenes Phaenomen – Leistungen in Drucksituationen folgen grob dem Normalwert",
        answerC = "Cognitive-Bias-gesteuertes Phaenomen, das nur von Fans wahrgenommen wird",
        answerD = "Biologisch begruendete Faehigkeit durch Kortisol-Resistenz messbar",
        correctAnswer = 1,
        explanation = "Mehrere statistische Studien (u.a. Berri & Schmidt im Basketball, diverse MLB-Analysen) konnten kein stabiles 'Clutch'-Merkmal nachweisen. Spieler zeigen keine konsistent ueberdurchschnittlichen Leistungen in Drucksituationen ueber mehrere Saisons.",
        difficulty = 4,
        funFact = "Michael Jordan gilt als ultimativer 'Clutch Player', doch selbst seine Statistiken zeigen, dass seine Erfolgsquote in letzten Sekunden nahe seiner Normalquote lag – er hatte nur oeufter die Ball in solchen Situationen."
    ),

    // --- NISCHENSPORTARTEN (5 Fragen) ---

    Question(
        categoryId = 6,
        questionText = "Was ist der 'Fosbury Flop' im Hochsprung und welche biomechanische Innovation machte Dick Fosbury damit revolutionaer?",
        answerA = "Rueckwaerts-Ueberqueren der Latte mit Bauch unten – ermoeglicht tieferen Schwerpunkt",
        answerB = "Rueckwaerts-Ueberqueren der Latte mit Ruecken nach oben – Schwerpunkt kann unterhalb der Latte verlaufen",
        answerC = "Seitwaerts-Ueberqueren mit gestrecktem Koerper – maximale Streckkraft",
        answerD = "Vorwaerts-Rotationstechnik – nutzt Drehmoment fuer hoeheren Abflug",
        correctAnswer = 1,
        explanation = "Dick Fosbury revolutionierte den Hochsprung 1968 mit dem Rueckensprung. Physikalisch genialen: Der Koerperschwerpunkt kann dabei unterhalb der Latte verlaufen (da Koerper gebogen ist), waehrend der gesamte Koerper die Latte ueberquert – moeglich nur mit weichen Matten.",
        difficulty = 4,
        funFact = "Vor der Einfuehrung weicher Schaumstoffmatten war der Rueckensprung gefaehrlich und unmoeglich. Die Technikrevolution waere ohne die gleichzeitige Materialtechnologie-Innovation nicht denkbar gewesen."
    ),

    Question(
        categoryId = 6,
        questionText = "Im Curling – welche physikalische Eigenschaft des Eises erklaert den 'curl' (die Kurve) des Steins, und welche Theorie gilt aktuell als wissenschaftlich plausibelste Erklaerung?",
        answerA = "Schmelzwasser-Film unter dem Stein durch Druck – klassische Theorie",
        answerB = "Asymmetrische Reibung der Steinkante durch Pebble-Oberflaeche kombiniert mit rotationsbedingten Kraeften",
        answerC = "Bernoulli-Effekt durch Steinrotation erzeugt Unterdruck",
        answerD = "Statische Elektrizitaet zwischen Stein und Eis erzeugt Richtungsaenderung",
        correctAnswer = 1,
        explanation = "Die plausibelste Theorie (Shegelski et al.): Die rotierenden Steine erzeugen durch die Pebble-Oberflaeche (kleine Eishoecker) asymmetrische Reibungseffekte. Vordere Kante gleitet anders als hintere Kante, was zum Curl fuehrt. Das vollstaendige Mechanismus ist noch nicht abschliessend gekleart.",
        difficulty = 4,
        funFact = "Curling-Besen koennen durch intensives Schrubben die Eisoberflaeche so veraendern, dass sie den Stein bis zu 3-4 Meter weiter gleiten lassen und die Richtung beeinflussen."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist 'Pelota Vasca' (Baskischer Handball) und welche Variante wurde historisch als schnellster Ballsport der Welt eingestuft?",
        answerA = "Racquetball-Variante aus dem Baskenland – Frontenis als schnellste Variante",
        answerB = "Wandball-Spiel mit blossem Hand oder Kelle – Jai Alai (Cesta Punta) mit Baellen bis 300 km/h",
        answerC = "Baskisches Squash gegen Wand – Remonte als geschwindigkeitssteilste Version",
        answerD = "Traditionelles Wurf-Fang-Spiel – Pala als Weltrekord-Variante",
        correctAnswer = 1,
        explanation = "Pelota Vasca bezeichnet eine Familie baskischer Wandballspiele. Jai Alai (Cesta Punta) – gespielt mit einem gebogenen Korb-Scoop – erzeugt Ballgeschwindigkeiten von bis zu 302 km/h und wurde im Guinness Buch als schnellster Ballsport der Welt gefuehrt.",
        difficulty = 4,
        funFact = "Jai Alai war in den USA in den 1970-80ern als Wettspiel extrem beliebt, besonders in Florida und Connecticut. Der Niedergang kam mit der Konkurrenz durch staatlich organisiertes Gluecksspiel."
    ),

    Question(
        categoryId = 6,
        questionText = "Was ist 'Korfball' und welches einzigartiges Prinzip unterscheidet es grundlegend von allen anderen Teamspielen?",
        answerA = "Kombination aus Basketball und Netball – gespielt auf kleinerem Feld",
        answerB = "Einzige gemischtgeschlechtliche Teamsportart als Pflichtprinzip – je 4 Maenner und 4 Frauen spielen zusammen",
        answerC = "Urspruenglich niederlaendischer Ringwurf-Sport ohne Koerperkontakt",
        answerD = "Fussball-Variante mit ovalen Toren und gemischten Regeln",
        correctAnswer = 1,
        explanation = "Korfball (1902 in den Niederlanden entstanden) schreibt als einzige Teamsportart weltweit gesetzlich vor, dass Maenner und Frauen zusammen in gemischten Teams spielen: Vier Maenner und vier Frauen pro Mannschaft, wobei Spieler nur gegen Gegner desselben Geschlechts verteidigen.",
        difficulty = 4,
        funFact = "Korfball wird in ueber 70 Laendern gespielt und hatte mehrmals Chancen auf Olympische Aufnahme. 2021 wurde es als IOC-anerkannte Sportart bestaetigt, ist aber noch nicht olympisch."
    ),

    Question(
        categoryId = 6,
        questionText = "Im Segeln – was ist der 'America's Cup' und welche technische Innovation bei der 72. Regatta 2013 veraenderte den Sport fundamental?",
        answerA = "Weltmeisterschaft im Einhand-Segeln – Einfuehrung von Kohlefaser-Masten",
        answerB = "Aeltester Sportpokal der Welt (seit 1851) – Einfuehrung von Tragflaechen-Foils an Katamaranen",
        answerC = "Atlantik-Uberquerungsrennen – Einfuehrung automatischer Steuerung",
        answerD = "Regatta unter Gleittechnik-Segeln – Einfuehrung von Unterwasser-Antrieben",
        correctAnswer = 1,
        explanation = "Der America's Cup (seit 1851) ist der aelteste noch ausgetragene internationale Sportpokal. 2013 (34. America's Cup) revolutionierten Tragflaechen-Foils die Katamarane: Boote heben aus dem Wasser und 'fliegen' mit Geschwindigkeiten ueber 50 Knoten (90+ km/h).",
        difficulty = 4,
        funFact = "Team USA (Oracle Team USA) gewann den 34. America's Cup nach dem groessten Comeback der Sportgeschichte: Sie lagen 1:8 zurueck und gewannen noch 9:8 – neun Siege in Folge gegen Emirates Team New Zealand."
    ),

)

package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun musicQuestionsMaster5(): List<Question> = listOf(

    // ── Klangfarbenanalyse / Spektralmorphologie (10) ────────────────────────

    // Q1
    Question(
        categoryId = 5,
        questionText = "Was bezeichnet der Begriff 'Spektralmorphologie' in der elektroakustischen Musikanalyse nach Denis Smalley?",
        answerA = "Die zeitliche Veraenderung des Klangspektrums — also wie sich Obertonstruktur, Rausch- und Tonanteile eines Klangs ueber die Zeit hinweg dynamisch entwickeln",
        answerB = "Die mathematische Klassifizierung von Klangspektren nach ihrer Fourier-Koeffizient-Verteilung",
        answerC = "Die graphische Darstellung von Klangereignissen in einem Sonagramm ohne Beruecksichtigung zeitlicher Entwicklung",
        answerD = "Die computergestuetzte Analyse von Amplitudenhuelven ohne Betrachtung der Frequenzstruktur",
        correctAnswer = 0,
        explanation = "Denis Smalleys Spektralmorphologie (ab 1986) beschreibt, wie sich Klangspektren im Zeitverlauf transformieren. Zentrale Konzepte sind 'Spectral Space' (Breite und Dichte des Spektrums), 'Motion' (Richtung und Geschwindigkeit spektraler Veraenderung) sowie Uebergaenge zwischen harmonischen und inharmonischen Klangtexturen. Das Modell ist deskriptiv, nicht vorschreibend, und wurde zum Standardvokabular der elektroakustischen Analyse.",
        difficulty = 5,
        funFact = "Smalley entwickelte die Spektralmorphologie als Reaktion auf den Mangel an Analysevokabular fuer elektroakustische Musik. Klassische Terminologie wie 'Melodie' oder 'Harmonie' griff bei Bandbandmusik nicht — er schuf ein systemisches Alternativvokabular."
    ),

    // Q2
    Question(
        categoryId = 5,
        questionText = "Was versteht man in der Timbre-Forschung unter dem Begriff 'spectral centroid' und welche perceptuelle Eigenschaft korreliert er?",
        answerA = "Den Schwerpunkt des Energiegewichts im Frequenzspektrum eines Klangs — er korreliert wahrnehmungspsychologisch mit der empfundenen 'Helligkeit' des Klangs",
        answerB = "Den Mittelwert aller Grundtoene in einem mehrstimmigen Klang, korreliert mit der wahrgenommenen Tonhoehe",
        answerC = "Die Frequenz, bei der die spektrale Energie ihr absolutes Maximum erreicht, korreliert mit dem Timbre-Identitaet",
        answerD = "Den geometrischen Mittelpunkt des Sonagramms, der die raeumliche Ausbreitung des Klangs anzeigt",
        correctAnswer = 0,
        explanation = "Der spectral centroid (Spektraler Schwerpunkt) ist der gewichtete Mittelwert der Frequenzkomponenten, wobei die Amplituden als Gewichte dienen. Ein hoher spectral centroid (viele Energie in hohen Frequenzen) entspricht einem wahrnehmungspsychologisch 'hellen' Klang (z.B. Becken, Cymbal). Ein tiefer centroid entspricht einem 'dunklen', bassschweren Klang. Er ist eine der wichtigsten akustischen Korrelate des Timbres.",
        difficulty = 5,
        funFact = "Der spectral centroid ist einer der am haeuigfsten berechneten Merkmale im Music Information Retrieval (MIR). Algorithmen wie Shazam verwenden Spektral-Features, um Musikstuecke zu identifizieren — der centroid ist dabei ein Kernelement."
    ),

    // Q3
    Question(
        categoryId = 5,
        questionText = "Was bezeichnet der Begriff 'inharmonicity' (Inharmonizitaet) in der Akustik von Saiteninstrumenten?",
        answerA = "Die Abweichung der realen Partialtone eines Klangs von der idealen harmonischen Reihe, verursacht durch Steifigkeit der Saite — fuehrt bei Klavieren zur charakteristischen 'Spreizung' der Obertone",
        answerB = "Die Verstimmung zwischen zwei gleichzeitig gespielten Noten, gemessen in Cent",
        answerC = "Das Verhaeltnis von Grundton zu dominantem Oberton, das die Klangfarbe eines Instruments bestimmt",
        answerD = "Die physikalische Eigenschaft von Metallsaiten, bestimmte Frequenzen staerker zu daempfen als andere",
        correctAnswer = 0,
        explanation = "Inharmonizitaet entsteht, weil reale Saiten nicht ideal biegsam sind — ihre Steifigkeit bewirkt, dass hoehere Partialtone leicht nach oben abweichen (geschaerft werden). Bei einem Konzertfluegel liegt die 88. Note bis zu 30 Cent ueber der theoretischen harmonischen Frequenz. Klavierstimmer kompensieren dies durch 'Stretchtuning': Die Oktaven werden leicht geweitet, damit der Klang konsistent klingt.",
        difficulty = 5,
        funFact = "Der Faktor Inharmonizitaet erklaert, warum Klavierakkorde nicht exakt 'rein' klingen koennen. Eine perfekt reine Stimmung nach harmonischen Obertoenen wuerde bei einem Klavier wie verstimmt klingen — der menschliche Hoersinn erwartet die 'gestreckte' Oktave."
    ),

    // Q4
    Question(
        categoryId = 5,
        questionText = "Welches akustische Phaenomen erklaert Pierre Schaeffer mit dem Begriff 'objet sonore' und wie grenzt er es vom konventionellen Musikton ab?",
        answerA = "Das Klangobjekt als in sich geschlossene perzeptive Einheit, die unabhaengig von ihrer Ursache oder semantischen Bedeutung analysiert wird — im Gegensatz zum Musikton, der immer in einem syntaktischen (melodisch-harmonischen) Kontext steht",
        answerB = "Ein elektronisch erzeugter Klang ohne physikalische Schallquelle, der nur ueber Lautsprecher existiert",
        answerC = "Die Gesamtheit aller Obertone eines Instrumentalklangs, die als Einheit wahrgenommen werden",
        answerD = "Ein Tonband-Ausschnitt, der rueckwaerts abgespielt wird und so seinen semantischen Ursprung verliert",
        correctAnswer = 0,
        explanation = "Schaeffers 'objet sonore' (aus 'Traite des objets musicaux', 1966) ist ein Kernkonzept der Musique Concrete. Es bezeichnet jeden Klang, der mit 'reduziertem Horen' (ecoute reduite) wahrgenommen wird: Man hoert den Klang als Klangobjekt an sich, abstrahiert von seiner Ursache (acousmatisches Horen). Der Musikton hingegen wird immer in seiner melodisch-harmonischen Funktion innerhalb eines Systems gehoert.",
        difficulty = 5,
        funFact = "Schaeffers 'ecoute reduite' war von Edmund Husserls phaenomenologischer Methode beeinflusst. Die Idee, Klang von seiner Ursache zu trennen, war philosophisch radikal und blieb umstritten — Stockhausen kritisierte Schaeffers Konzept als zu materialistisch."
    ),

    // Q5
    Question(
        categoryId = 5,
        questionText = "Was ist der Unterschied zwischen 'attack transient' und 'steady state' in der Klangfarbenanalyse und warum ist der Attack fuer die Timbre-Identifikation entscheidend?",
        answerA = "Der attack transient ist die kurze Einschwingphase mit schnell wechselnden Spektralanteilen, der steady state die stabile Phase danach. Versuche zeigen: Entfernt man den attack, sind Instrumente kaum erkennbar — er traegt die meisten Timbre-Informationen",
        answerB = "Der steady state ist die lauteste Phase des Klangs, der attack die lauteste — beide sind fuer Timbre gleich wichtig",
        answerC = "Attack bezeichnet die Frequenz des Grundtons zu Beginn, steady state die stabile Mittellage nach Vibrato-Einsatz",
        answerD = "Der attack transient bezeichnet das Ueberschwingen des Lautsprechers bei impulsiven Klaengen, der steady state die lineari sierte Wiedergabe danach",
        correctAnswer = 0,
        explanation = "Klassische Experimente von Grey und Moorer (1977) zeigten: Wenn man bei Aufnahmen von Klarinette, Trompete oder Geige nur den attack-Transient (erste 20-50 ms) entfernt und den steady state belaesst, werden die Instrumente kaum noch erkannt. Der attack ist spektral reich, chaotisch und aeusserst informativ — im steady state gleichen sich viele Instrumente stark an. Diese Erkenntnis beeinflusste die Syntheseforschung nachhaltig.",
        difficulty = 5,
        funFact = "Das Experiment mit dem 'abgeschnittenen Attack' ist ein Klassiker der Psychoakustik-Vorlesungen. Es demonstriert eindrucksvoll, wie wenig wir uns auf den 'stabilen' Anteil eines Klangs fuer die Instrumentenerkennung verlassen — unser Gehirn liest den Einsatz wie eine Unterschrift."
    ),

    // Q6
    Question(
        categoryId = 5,
        questionText = "Was bezeichnet die 'kritische Bandbreite' (critical bandwidth) im Modell des auditiven Systems nach Zwicker und wie beeinflusst sie die Wahrnehmung von Dissonanz?",
        answerA = "Den Frequenzbereich, innerhalb dessen zwei Sinusttoene miteinander interferieren und 'Rauheit' (roughness) erzeugen — Intervalle innerhalb einer kritischen Bandbreite wirken dissonant, weiter gespannte harmonisch",
        answerB = "Die maximale Frequenz, die das menschliche Ohr noch als einzelnen Ton wahrnehmen kann, bevor es als Rauschen interpretiert wird",
        answerC = "Den Frequenzbereich eines einzelnen Haarzell-Clusters auf der Basilarmembran, der fuer die Tonhoehenpraezision bestimmend ist",
        answerD = "Die minimale Frequenzdifferenz, unterhalb derer das Gehoer zwei Toene als identisch wahrnimmt",
        correctAnswer = 0,
        explanation = "Erich Zwickers Konzept der kritischen Bandbreite (ca. 1-2 Halbtone im mittleren Frequenzbereich, breit werdend in tiefen Lagen) beschreibt den Bereich, in dem die Basilarmembran zwei Frequenzen als interferierend wahrnimmt. Liegen zwei Toene innerhalb einer kritischen Bandbreite, entstehen Schwebungen und 'Rauheit' — dies ist der psychoakustische Kern des Dissonanzerlebens nach Helmholtz/Plomp/Levelt.",
        difficulty = 5,
        funFact = "Georg von Bekesy erhielt 1961 den Nobelpreis fuer Medizin fuer die Erforschung der Basilarmembran-Mechanik. Sein Wellenmodell der Membran ist die physikalische Grundlage fuer Zwickers kritische Bandbreiten-Konzept."
    ),

    // Q7
    Question(
        categoryId = 5,
        questionText = "Was ist ein 'Sonagramm' (Spektrogramm) und welche drei Dimensionen stellt es dar?",
        answerA = "Zeit (x-Achse), Frequenz (y-Achse) und Amplitude (Helligkeits- oder Farbintensitaet) — es zeigt, wie sich das Frequenzspektrum eines Klangs im Zeitverlauf veraendert",
        answerB = "Frequenz (x-Achse), Amplitude (y-Achse) und Phase (Farbkodie rung) als Snapshot eines einzelnen Zeitpunkts",
        answerC = "Zeit (x-Achse), Lautstaerke (y-Achse) und Tonhoehe (Farbkodierung) in einem vereinfachten Darstellungsmodell",
        answerD = "Frequenz (x-Achse), Zeit (y-Achse) und Formant-Energie (z-Achse) als dreidimensionales Volumenmodell",
        correctAnswer = 0,
        explanation = "Das Sonagramm (auch Spektrogramm) visualisiert drei Parameter gleichzeitig: Die x-Achse zeigt die Zeit, die y-Achse die Frequenz und die Helligkeit (oder Farbe) die Intensitaet der jeweiligen Frequenzkomponente zu jedem Zeitpunkt. Es ist das wichtigste visuelle Analyse-Werkzeug fuer Vokal-, Sprach- und Musikklanganalyse sowie fuer elektroakustische Komposition.",
        difficulty = 5,
        funFact = "Komponisten wie Ligeti nutzen Sonagramme als kreatives Werkzeug. Er soll gesagt haben, Musik 'wie eine mikroskopische Welt' sehen zu wollen — das Sonagramm machte das wortlich moeglich. Software wie Spear oder AudioSculpt erlaubt heute direktes Komponieren im Spektralbild."
    ),

    // Q8
    Question(
        categoryId = 5,
        questionText = "Welches Syntheseverfahren liegt dem Synthesizer Yamaha DX7 zugrunde und was macht es klanglich von anderen Syntheseverfahren (Subtraktion, Wavetable) verschieden?",
        answerA = "FM-Synthese (Frequenzmodulation): Ein Traegeroszillator wird in seiner Frequenz durch einen Modulatoroszillator variiert, was reiche inharmonische Seitenbandspartial erzeugt — gegenueber Subtraktion wesentlich komplexere, 'metallische' und 'glasige' Spektren",
        answerB = "Additive Synthese: Einzelne Sinusschwingungen werden zu komplexen Klangspektren addiert, was im Gegensatz zur Subtraktion von oben nach unten aufbaut",
        answerC = "Physical Modelling: Das physikalische Schwingungsverhalten realer Instrumente wird mathematisch simuliert, was natuerlichere Klanghuelln als FM oder Subtraktion erlaubt",
        answerD = "Granularsynthese: Kurze Klangkoerner werden zu Klangtexturen verkettert, was die charakteristischen 'glitzernden' Klangtexturen des DX7 erklaert",
        correctAnswer = 0,
        explanation = "John Chownings FM-Synthese (1967, Stanford) moduliert die Frequenz eines Traegeroszillators mit einem Modulatoroszillator. Das Verhaeltnis der Frequenzen (C:M-Ratio) bestimmt, ob harmonische oder inharmonische Seitenbaender entstehen. Der DX7 (1983) machte FM-Synthese massentauglich mit 6 Operatoren in 32 Algorithmen. Charakteristisch: metallische Glocken, glaeserne Baesse, 'digitale' Holzwinde.",
        difficulty = 5,
        funFact = "John Chowning verkaufte sein FM-Patent fuer wenig Geld an Yamaha — die Firma verdiente damit Milliarden. Der DX7 war das meistverkaufte Synthesizer-Modell aller Zeiten (ca. 200.000 Einheiten) und praegte den Sound der 1980er-Jahre definitiv."
    ),

    // Q9
    Question(
        categoryId = 5,
        questionText = "Was versteht man unter 'Granularsynthese' und wer entwickelte die theoretischen Grundlagen dieses Klangsynthesemodells?",
        answerA = "Ein Verfahren, bei dem Klang in Tausende kurze Mikrosegmente ('Koerner', 1-50 ms) zerlegt oder aus solchen aufgebaut wird — theoretisch grundgelegt von Gabor (1947) und kompositorisch entwickelt von Xenakis und Truax",
        answerB = "Eine Synthesemethode, die aus Granit-Resonanzen abgeleitete Spektren als Klangtexturen nutzt, entwickelt am IRCAM Paris",
        answerC = "Die Erzeugung von Weissrauschen durch statistische Granularverteilung, urspruenglich ein radiotechnisches Verfahren",
        answerD = "Eine Mikrochip-basierte Klangerzeugung, bei der Transistor-Rauschen als kreatives Ausgangsmaterial filtriert wird",
        correctAnswer = 0,
        explanation = "Dennis Gabors quantenmechanische Klangtheorie (1947) postulierte, dass jeder Klang als Summe von zeitlich lokalisierten Mikrosegmenten (Grains) dargestellt werden kann. Iannis Xenakis implementierte dies kompositorisch in Werken wie 'Analogique A+B' (1959). Barry Truax und Curtis Roads entwickelten Echtzeit-Granularsynthese-Software. Heute ist Granularsynthese ein Standardwerkzeug in Ableton Live, Max/MSP etc.",
        difficulty = 5,
        funFact = "Denis Gabor erhielt 1971 den Nobelpreis fuer Physik fuer die Erfindung der Holographie — seine Klangquantentheorie ist weniger bekannt, aber ebenso revolutionaer. Sie zeigt, dass Zeit und Frequenz im Klang ein Unschaerfeprinzip bilden, analog zu Heisenbergs Quantenmechanik."
    ),

    // Q10
    Question(
        categoryId = 5,
        questionText = "Was bezeichnet die 'Spektrale Verschmelzung' (spectral fusion) und unter welchen Bedingungen verschmelzen mehrere Partialtone zu einem einheitlichen Timbre-Eindruck?",
        answerA = "Partialtone verschmelzen zu einem einheitlichen Klangobjekt, wenn sie harmonisch verwandt sind, gemeinsam einsetzen (synchroner onset), gemeinsame Amplitudenhuelln zeigen und aus einer gemeinsamen raeumlichen Quelle kommen",
        answerB = "Zwei Klangquellen verschmelzen perceptuell, wenn ihr Frequenzabstand kleiner als eine kritische Bandbreite ist und ihre Phasenlage identisch ist",
        answerC = "Spektrale Verschmelzung bezeichnet das Phaenomen, dass tiefe Toene in der Raumakustik durch Reflexionen ihren Charakter verlieren und als Raumklang fusionieren",
        answerD = "Ein Syntheseprozess, bei dem zwei Klangspektren durch Kreuzfade (Morphing) zu einem neuen Timbre verbunden werden",
        correctAnswer = 0,
        explanation = "Albert Bregmans 'Auditory Scene Analysis' (1990) beschreibt Gruppieurungsprinzipien: Partialtone werden zu einem Klangobjekt fused, wenn sie harmonisch geordnet sind (gemeinsame fundamentale Frequenz implizieren), synchron einsetzen und enden, gemeinsame Amplitudenmodulation zeigen und aus einer Richtung kommen. Abweichungen — etwa asynchroner onset einer Komponente — lassen sie als separate Objekte hervortreten.",
        difficulty = 5,
        funFact = "Dieser Mechanismus wird in der elektroakustischen Komposition gezielt ausgenutzt: Durch subtile Asynchronien kann man einzelne Spektralkomponenten 'herausloesen' und als separate Klangebene erfahrbar machen — eine Technik, die Komponisten wie Jonathan Harvey in 'Mortuos Plango, Vivos Voco' meisterhaft einsetzen."
    ),

    // ── Notationstheorie / Graphische Notation (10) ─────────────────────────

    // Q11
    Question(
        categoryId = 5,
        questionText = "Welche Hauptwerke und Konzepte entwickelte Erhard Karkoschka in seinem 1966 erschienenen Standardwerk 'Das Schriftbild der Neuen Musik'?",
        answerA = "Karkoschka klassifizierte neue Notationsformen in vier Kategorien: exakt determinierte Notation, Rahmennotation (innerhalb vorgegebener Grenzen), Zeichennotation (fuer Klangereignisse ohne Tonhoehe) und graphische Notation (freie Bilder als Spielanweisung)",
        answerB = "Er entwickelte ein universales Notationssystem das alle Klangparameter in einem einzigen Liniensystem vereinigte und als 'Omniscore' bezeichnete",
        answerC = "Karkoschka systematisierte ausschliesslich elektronische Notation und entwickelte das Standardformat fuer Studiopartituren des WDR Koeln",
        answerD = "Er klassifizierte Notationsformen nach ihrer historischen Entwicklung vom Mittelalter bis zur Gegenwart ohne eigene Kategorisierung",
        correctAnswer = 0,
        explanation = "Karkoschkas 'Das Schriftbild der Neuen Musik' (1966, Moeck-Verlag) ist bis heute das grundlegende Referenzwerk zu Neuer Notation. Seine Vier-Kategorien-Einteilung — exakte Notation, Rahmennotation, Zeichennotation, graphische Notation — bietet das erste systematische Klassifikationsschema. Das Werk dokumentiert Hunderte von Notationsloesungen der Avantgarde und ist ein unverzichtbares Nachschlagewerk.",
        difficulty = 5,
        funFact = "Karkoschkas Buch erschien in einer Zeit, als neue Notationszeichen oft nicht standardisiert waren — jeder Komponist erfand eigene Symbole. Das Buch wirkte wie ein Woerterbuch in einer Zeit der Sprachenverwirrung und half Interpreten, neue Partituren zu erschliessen."
    ),

    // Q12
    Question(
        categoryId = 5,
        questionText = "Was ist das kompositorische Konzept hinter Cardews 'Treatise' (1963-67) und wie funktioniert die Partitur ohne verbale Spielanweisungen?",
        answerA = "Die 193-seitige graphische Partitur besteht ausschliesslich aus geometrischen Formen, Linien und Symbolen ohne jegliche Spielanweisungen — Interpreten muessen eigenstaendig Entsprechungen zwischen den visuellen Elementen und klanglichen Handlungen entwickeln",
        answerB = "Die Partitur gibt Spielzeitpunkte und Lautstaerken durch Positionierung von Punkten auf einem Raum-Zeit-Raster vor, ohne Tonhoehen zu definieren",
        answerC = "Cardew nutzt eine Bildsprache aus der Malerei (Mondrian-Stil) und gibt im Begleittext genaue Entsprechungen zwischen Farben und Klangparametern an",
        answerD = "Die Partitur besteht aus Fotografien konkreter Gegenstande, die als klanglich zu imitierende Objekte fungieren",
        correctAnswer = 0,
        explanation = "Cornelius Cardews 'Treatise' (1963-1967) ist ein Meilenstein der graphischen Notation. Die 193 Seiten zeigen geometrische Zeichen, Linien, Kurven und Symbole ohne jegliche Verbalerklaerung. Jede Auffuehrung erfordert, dass die Performer kollektiv hermeneutisch erschliessen, was die Zeichen fuer sie klanglich bedeuten. Das fuehrt zu radikal unterschiedlichen Realisierungen — und das ist Cardews Absicht.",
        difficulty = 5,
        funFact = "Cardew wurde spaeter marxistischer Aktivist und distanzierte sich von 'Treatise' als 'burgerliche Kunstmusik'. Paradoxerweise ist es sein beruhmtestes Werk geblieben. Die Partitur wurde 1967 bei Peters Edition veroffentlicht und ist heute ein Sammlerobjekt."
    ),

    // Q13
    Question(
        categoryId = 5,
        questionText = "Was versteht man unter 'Proportionalnotation' in der zeitgenoessischen Partitur und worin unterscheidet sie sich von metrisch-taktierter Notation?",
        answerA = "Bei der Proportionalnotation entspricht der horizontale Abstand zwischen Noten direkt ihrer relativen zeitlichen Dauer — lange Noten stehen weiter rechts, ohne Takt-Einteilung durch Taktstriche",
        answerB = "Eine Notationsform, bei der alle Notenwerte durch rationale Brueche (1/3, 2/5, etc.) ausgedrueckt werden statt durch traditionelle Notenwerte",
        answerC = "Die Notation von Proportionen zwischen verschiedenen Stimmen, waehrend jede Stimme intern in eigenem Tempo notiert wird",
        answerD = "Ein mittelalterliches Notationssystem der Mensuralnotation, das in der Neuen Musik wiederentdeckt wurde",
        correctAnswer = 0,
        explanation = "Proportionalnotation (space-time notation) verwendet den physischen horizontalen Abstand auf dem Notenblatt als direktes Mass fuer Zeit. Eine ganze Note, die doppelt so lang dauert wie eine halbe, nimmt auch doppelt so viel Platz ein. Es gibt keine Taktstriche — Interpreten messen Zeit raeumlich. Vorteil: Komplexe metrische Strukturen und Polyrhythmik werden lesbar. Bekannte Anwendungen: Stockhausens fruehe Klavierstuecke, Feldmans Werke.",
        difficulty = 5,
        funFact = "Morton Feldman war ein Meister der Proportionalnotation. Er sagte, er wolle Musik schreiben, die 'schwebt' — und die raeumliche Notation ohne Taktstriche gibt Interpreten die Freiheit, den Klang tatsaechlich in der Zeit schweben zu lassen."
    ),

    // Q14
    Question(
        categoryId = 5,
        questionText = "Welche Notationstechnik verwendet Witold Lutoslawski in seinen Werken ab 'Jeux venitiens' (1961) mit dem Begriff 'aleatory counterpoint'?",
        answerA = "Lutoslawski gibt jedem Instrumentalisten eine exakt notierte Passage, laesst aber den genauen zeitlichen Einsatz und die Koordination zwischen den Stimmen dem Zufall — gesteuert durch Dirigentensignale, die Abschnittsgrenzen markieren",
        answerB = "Er notiert alle Stimmen in graphischer Notation und laesst Interpreten frei improvisieren innerhalb vorgegebener Cluster-Grenzen",
        answerC = "Die Stimmen werden in konventioneller Notation geschrieben, aber Lutoslawski laesst den Interpreten frei entscheiden, welche Tonhoehe sie bei jedem Einsatz spielen",
        answerD = "Eine Kombinati on aus Proportionalnotation und traditionellen Taktstrichen, bei der nur die Rhythmik aleatorisch, Tonhoehen aber exakt vorgegeben sind",
        correctAnswer = 0,
        explanation = "Lutoslawskis 'kontrollierte Aleatorik' ist ein sorgfaeltig kalibriertes System: Jeder Musiker hat eine exakt notierte Passage ('ad libitum'-Abschnitt), die er so oft wie noetig wiederholt oder durchlaeuft, bis der Dirigent mit einem Signal den naechsten Abschnitt anzeigt. Die vertikale Koordination der Stimmen ist aleatorisch — es entstehen zufallige harmonische Konstellationen. Die makroskopische Form bleibt aber unter vollstaendiger kompositorischer Kontrolle.",
        difficulty = 5,
        funFact = "Lutoslawski entdeckte diese Technik, als er 1960 im Radio Cages 'Concert for Piano and Orchestra' hoerte — aber er entwickelte daraus eine radikale Gegenmethode: Wo Cage alles dem Zufall ueberlies, nutzten Lutoslawski den Zufall nur in streng kontrollierten Grenzen."
    ),

    // Q15
    Question(
        categoryId = 5,
        questionText = "Was sind die kompositorischen Grundprinzipien von Helmut Lachenmanns Begriff 'Musique concrete instrumentale'?",
        answerA = "Die Neudefinition des instrumentalen Klangs weg von Tonhoehe und Melodik hin zu physikalischen Klangerzeugungsprozessen (Reiben, Kratzen, Blasen ohne Anblasen): Der Klangerzeugungsakt selbst wird zum kompositorischen Material",
        answerB = "Die elektronische Manipulation von Orchesterklang in Echtzeit, sodass Instrumentalklang und Bandklang ununterscheidbar werden",
        answerC = "Die Uebertragung von Schaeffers Musique concrete auf Orchesterinstrumente durch Umstimmung und erweiterte Spieltechniken",
        answerD = "Die Verwendung von Alltagsgeraeuschen und Strassenlarm als Notation, die von Orchestermitgliedern imitiert wird",
        correctAnswer = 0,
        explanation = "Lachenmanns 'Musique concrete instrumentale' (Begriff gepraegt ca. 1969-1971) bezeichnet eine Kompositionshaltung, in der konventionelle Klangproduktion dekonstruiert wird. Statt Toene zu spielen erzeugen Instrumente Luftgeraeusche, Reib- und Schabtone, Schlaggeraeusche. Die physische Energie des Klangerzeugungsprozesses wird hoerbar gemacht. Hauptwerke: 'Air' (1969), 'Gran Torso' (1971-76).",
        difficulty = 5,
        funFact = "Lachenmanns Musik klingt fuer Ungewohnte oft wie 'nichts' — Geraeusche, Kratzen, Ploppen. Er wurde dafuer haeufig kritisiert. Heute gilt er als einer der bedeutendsten lebenden Komponisten. Sein Kammeroper 'Das Maedchen mit den Schwefholzern' (1990-96) hat die moderne Musiktheaterszene grundlegend veraendert."
    ),

    // Q16
    Question(
        categoryId = 5,
        questionText = "Was bezeichnet der Begriff 'Cluster-Notation' und wie entwickelte Henry Cowell das Konzept in seinen fruehen Klavierwerken?",
        answerA = "Cowell notierte Cluster als schwarze Balken auf dem System: Die Breite des Balkens zeigt den Tonraumbereich, ein 'o' drueber bedeutet weiss Tasten, 'x' schwarz Tasten. Er spielte sie mit Unterarm, Faust oder Ellenbogen",
        answerB = "Cluster werden als kreisfoermige Symbole mit Pfeil nach oben oder unten notiert, die chromatische Verdichtung im Hoch- oder Tiefregister anzeigen",
        answerC = "Cowell erfand eine numerische Notation: Eine Zahl gibt an, wie viele nebeneinanderliegende Halbtone gleichzeitig gespielt werden, unabhaengig vom Register",
        answerD = "Die Cluster-Notation benutzt Kreuzalterations-Gruppen auf einer Hilfslinie ausserhalb des Systems fuer jede Tonstufengruppe",
        correctAnswer = 0,
        explanation = "Henry Cowells Tone Clusters erschienen zuerst in 'The Tides of Manaunaun' (ca. 1912) und 'Dynamic Motion' (1916). Er entwickelte eine pragmatische Notationsform: ausgefuellte Rechteck-Balken auf der Notensystemhoehe zeigen den Tonraumbereich an, Buchstaben (w = white keys, b = black keys, wb = alle Tasten) die Tastenwahl. Er spielte mit Unterarm (pp bis ff) oder Ellenbogen fuer grosse Cluster.",
        difficulty = 5,
        funFact = "Cowell reichte 1912 die erste Cluster-Komposition ein — Charles Seeger, sein Lehrer, war skeptisch. Jahrzehnte spaeter wurden Cluster durch Penderecki und Ligeti zum zentralen Ausdrucksmittel der Neuen Musik. Cowell hatte sie im Alter von 15 Jahren 'erfunden'."
    ),

    // Q17
    Question(
        categoryId = 5,
        questionText = "Was ist der Unterschied zwischen 'offener Form' im Sinne von Earle Brown und der 'mobilen Komposition' bei Stockhausen, wie sie z.B. in 'Klavierstueck XI' (1956) realisiert wird?",
        answerA = "Browns 'December 1952' laesst Interpreten vollstaendig frei entscheiden, welche Klangereignisse sie auswaehlen und in welcher Reihenfolge; Stockhausens Klavierstueck XI gibt 19 Gruppen auf einem grossen Blatt vor, der Interpret waehlt jeweils spontan, welche Gruppe er als naechstes spielt, bis eine Gruppe dreimal besucht wurde",
        answerB = "Brown arbeitet mit graphischer Notation ohne Tonhoehen; Stockhausen verwendet vollstaendig notierte Abschnitte die nur in der Tempo-Beziehung aleatorisch sind",
        answerC = "Stockhausens offene Form laesst alle Parameter offen; Browns mobile Form gibt alle Parameter fest vor, nur die Reihenfolge ist frei",
        answerD = "Beide Konzepte sind identisch — 'offene Form' und 'mobile Komposition' sind synonyme Begriffe in der Avantgarde-Terminologie",
        correctAnswer = 0,
        explanation = "Earle Browns 'December 1952' (aus 'Folio') verwendet abstraktes graphisches Material — horizontale und vertikale Linien in einem weissen Feld — das Interpreten frei in Klang uebersetzen. Stockhausens 'Klavierstueck XI' ist anders konzipiert: 19 Gruppen mit exakter Notation stehen auf einem grossen Blatt; nach jeder Gruppe bestimmt der Interpret zufallig (Blickrichtung) die naechste. Die Form entsteht als Pfad durch das Materialangebot.",
        difficulty = 5,
        funFact = "Stockhausens Partitur des Klavierstuecks XI war urspruenglich so gross (93 x 53 cm), dass sie auf einem Notenstaender nicht auflag — ein Spezialstaender war noetig. Die physische Dimensionierung war Teil der Kompositionsidee: Der Interpret soll das Blatt 'wie eine Landschaft' ueberblicken."
    ),

    // Q18
    Question(
        categoryId = 5,
        questionText = "Was bezeichnet in der Notationstheorie der Begriff 'Textpartitur' und welche Werke sind kanonische Beispiele?",
        answerA = "Eine Kompositionsanweisung ausschliesslich in Prosa oder kurzen verbalen Instruktionen ohne Notensystem — klassische Beispiele sind Fluxus-Stuecke wie Yoko Onos 'Grapefruit' (1964) und Christian Wolffs fruehe 'Instructions'",
        answerB = "Eine Partitur, die aus literarischen Zitaten besteht, die Musiker vertonen oder in Klang uebersetzen sollen",
        answerC = "Eine verbale Beschreibung des Klangverlaufs, die als nachtraegliche Dokumentation einer Improvisation dient",
        answerD = "Die schriftliche Fixierung eines muendlich uebertragenen Volksmusik-Stuecks durch Notation in Wortsprache",
        correctAnswer = 0,
        explanation = "Textpartituren (instruction pieces) ersetzen das konventionelle Notationssystem vollstaendig durch verbale Anweisungen, oft in Prosa-Satzen oder Kurzgedichte. Yoko Onos 'Grapefruit' (1964) enthalt Instruktionen wie 'Imagine a painting, then scratch the canvas.' La Monte Youngs Werke von 1960 ('Draw a straight line and follow it') und Christian Wolffs 'Prose Collections' sind weitere kanonische Beispiele. Sie stellen die Grenzen zwischen Komposition, Konzept-Kunst und Auffuehrungsanweisung in Frage.",
        difficulty = 5,
        funFact = "Yoko Onos 'Grapefruit' (1964) war anfangs nur in kleiner Auflage erhaeltlich und gilt heute als Kultbuch der Konzeptkunst. John Lennon soll das Buch gelesen haben, bevor er Yoko Ono traf — sie schickte ihm ein Exemplar. Die Textpartitur wurde so zur Liebesgeschichte."
    ),

    // Q19
    Question(
        categoryId = 5,
        questionText = "Welche kompositorische und notatorische Idee steckt hinter Dieter Schnebels Konzept der 'Visible Music' und dem Werk 'Maulwerke' (1968-74)?",
        answerA = "Schnebel macht den Produktionsprozess von Klang sichtbar: In 'Maulwerke' werden Stimm-, Sprech- und Atemvorgaenge theatralisch aufgefuehrt und mit detaillierten Mund-Fotografien und Diagrammen des Vokaltrakts notiert",
        answerB = "Visible Music bezeichnet Schnebels System, bei dem Noten durch visuelle Objekte (Skulpturen, Zeichnungen) ersetzt werden, die Musiker intuitiv in Klang uebersetzen",
        answerC = "Schnebel verlangt, dass Orchestermusiker ohne Partitur spielen und der Dirigent die Musik durch visuelle Zeichen statt Takt-Schlagen leitet",
        answerD = "Eine Notationsform fuer stille Musik, bei der Gesten und Koerperbewegungen ohne Klangerzeugung als Musikauffuehrung definiert werden",
        correctAnswer = 0,
        explanation = "Dieter Schnebels 'Maulwerke' (1968-74) ist ein Hauptwerk der 'Visible Music'. Die Partitur enthaelt detaillierte Fotos und Diagramme des menschlichen Vokaltrakts, Mund- und Zungenpositionen, Atemkurven. Die Darsteller vollziehen Sprach-, Stimm- und Atemvorgaenge als autonome Theaterhandlung. Schnebel thematisiert die Materialitaet der menschlichen Stimme jenseits von Sprache und Gesang.",
        difficulty = 5,
        funFact = "Schnebel war auch Theologe und Musikwissenschaftler. Er sah Musik als Teil eines umfassenden Kulturs- und Kommunikationsprozesses. 'Maulwerke' ist ein Versuch, die alltagliche Materialitaet des Sprechens — etwas was wir nicht beachten — als Kunst sichtbar zu machen."
    ),

    // Q20
    Question(
        categoryId = 5,
        questionText = "Was versteht man unter der 'extended notation' bei Ferneyhough und warum bezeichnen Kritiker seinen Notationsstil oft als 'Neue Komplexitaet'?",
        answerA = "Ferneyhough nutzt extrem dicht geschriebene Notation mit multiplen simultanen Parametern (Polyrhythmen, Mikrotonalitaet, extended techniques, genaue Artikulationen) als kompositorisches Konzept: Die Ausfuehrbarkeitsgrenze ist kein Fehler, sondern Teil der Idee",
        answerB = "Extended Notation bedeutet bei Ferneyhough die Verwendung neuer graphischer Symbole fuer Klangereignisse, die ausserhalb des konventionellen Instrumentalbereichs liegen",
        answerC = "Ferneyhoughs Notationsstil basiert auf extremer Vereinfachung: Er reduziert alle Parameter auf das Wesentliche und laesst den Rest dem Interpreten",
        answerD = "Neue Komplexitaet bezeichnet Ferneyhoughs Methode, serielle und aleatorische Techniken in einer einzigen Partitur zu vereinen",
        correctAnswer = 0,
        explanation = "Brian Ferneyhoughs 'Neue Komplexitaet' (New Complexity, Begriff von Richard Toop) zeichnet sich durch exzessive Notationsdichte aus: Tuplets innerhalb von Tuplets (z.B. 5:4 innerhalb 7:6), simultanee Dynamikschichten, Mikrointervalle, extreme extended techniques, alles gleichzeitig notiert. Das Spielen der Partitur wie geschrieben ist oft am Rande des Moeglichen — Ferneyhough sieht dies als produktive Spannung zwischen Notation und Auffuehrung.",
        difficulty = 5,
        funFact = "Ferneyhoughs 'Unity Capsule' (1975) fuer Soloflote gilt als eines der schwierigsten Solomstuecke des Repertoires. Ferneyhough sagte dazu: 'Ich schreibe keine Musik fuer Interpreten, die alles koennen — ich schreibe Musik fuer Menschen, die versuchen, etwas zu tun, das sie nicht ganz koennen.'"
    ),

    // ── Historische Temperatur / Stimmtonforschung (8) ───────────────────────

    // Q21
    Question(
        categoryId = 5,
        questionText = "Was ist der grundlegende Unterschied zwischen 'Mitteltoeniger Stimmung' (meantone temperament) und 'Gleichstufiger Stimmung' (equal temperament) und welche Konsequenzen hat das fuer die Klangwirkung von Terzen?",
        answerA = "In der Mitteltoenigen Stimmung sind die grossen Terzen reiner (nahe am Verhaeltnis 5:4), waehrend Quinten minimal verkleinert werden. In der Gleichstufigen Stimmung sind alle Quinten gleich (ca. 2 Cent zu schmal) aber alle Terzen deutlich unreiner (ca. 14 Cent zu gross)",
        answerB = "Mitteltoenige Stimmung hat reine Quinten (3:2) und leicht verstimmte Terzen; Gleichstufige Stimmung hat reine Terzen (5:4) aber verstimmte Quinten",
        answerC = "Beide Stimmungssysteme unterscheiden sich nur in der Berechnung des Kammertones (a'), nicht in der Intervallstruktur",
        answerD = "Mitteltoenige Stimmung verwendet nur natur-reine Intervalle; Gleichstufige Stimmung verwendet ausschliesslich kompromisshafte Intervalle ohne reine Verhaeltnisse",
        correctAnswer = 0,
        explanation = "In der Mitteltoenigen Stimmung (ca. 1/4-Komma-Mitteltoenigkeit) werden die Quinten um ein Viertel des syntoni schen Kommas (5.38 Cent) verkleinert, was zur Folge hat, dass grosse Terzen nahezu rein (386 Cent statt 386.3 Cent bei 5:4) erklingen. In der Gleichstufigen Stimmung sind alle Quinten um 1.96 Cent verkleinert, aber die grossen Terzen sind 13.7 Cent zu gross — ein hoerbarer Unterschied, besonders in langsamen Saetzaetzen.",
        difficulty = 5,
        funFact = "Cembali aus dem 16./17. Jahrhundert klingen in Mitteltoeniger Stimmung grundlegend anders als auf modernen Instrumenten in Gleichstufigkeit. Historisch informierte Auffuehrungspraxis (HIP) verwendet diese alten Stimmungen, um den originalen Klangcharakter zu restituieren — ein Unterschied, den selbst Laien hoeren koennen."
    ),

    // Q22
    Question(
        categoryId = 5,
        questionText = "Was ist das 'pythagoraeische Komma' und wie entsteht es physikalisch?",
        answerA = "Die Differenz zwischen 12 reinen Quinten (3:2 hoch) und 7 Oktaven (2:1): 12 reine Quinten ergeben einen Ton, der 23.46 Cent hoeher liegt als die entsprechende Oktave — der Kreis schliesst sich nicht genau",
        answerB = "Das Verhaeltnis zwischen dem aeolischen und dem dorischen Modus in der pythagoraeischen Stimmungstheorie, ca. 81/80",
        answerC = "Der Frequenzunterschied zwischen dem grossen und dem kleinen Ganzton im pythagoraeischen System, entspricht 9:8 zu 10:9",
        answerD = "Die Abweichung der reinen Quinte (3:2) von der gleichstufigen Quinte, gemessen in Pythagoras eigener Einheit 'Diesis'",
        correctAnswer = 0,
        explanation = "Das pythagoraeische Komma (ca. 23.46 Cent) entsteht, weil 12 reine Quinten (3:2) = (3/2)^12 = 129.746... das 128-fache des Grundtons (= 7 Oktaven = 2^7 = 128) um diesen kleinen Betrag ueberschreiten. Da 129.746.../128 = 1.01364... entspricht das 23.46 Cent. In pythagoraeischer Stimmung muss eine der 12 Quinten (die 'Wolfsquinte') um diesen Betrag verkleinert werden — sie klingt extrem dissonant.",
        difficulty = 5,
        funFact = "Die Wolfsquinte bekam ihren Namen, weil sie wie ein Wolfsgeheul klang — so schrill und unrein war sie. Organisten und Cembali-Spieler vermieden bestimmte Tonarten rigoros, weil die Wolfsquinte dort unvermeidlich auftrat. Das war ein maechtiger kompositorischer Zwang."
    ),

    // Q23
    Question(
        categoryId = 5,
        questionText = "Was ist die 'Wohltemperierte Stimmung' nach Andreas Werkmeister und wie unterscheidet sie sich von der Gleichstufigkeit?",
        answerA = "Werkmeisters Temperatursysteme (1691) verteilen das pythagoraeische Komma ungleich auf die Quinten: Haeufig gespielte Tonarten haben reinere Quinten, seltenere Tonarten mehr Unreinheit — jede Tonart hat einen individuellen 'Charakter', im Gegensatz zur uniformen Gleichstufigkeit",
        answerB = "Wohltemperiert bedeutet bei Werkmeister identisch mit Gleichstufigkeit — er verwendete nur eine altere Terminologie",
        answerC = "Werkmeisters System basiert auf reinen Terzen (5:4) statt auf Quinten, was eine andere Verteilungsstrategie als spaetere Gleichstufigkeit ergibt",
        answerD = "Die Wohltemperierte Stimmung nutzt 19 statt 12 Toene pro Oktave, um alle wichtigen Intervalle rein darzustellen",
        correctAnswer = 0,
        explanation = "Andreas Werkmeister veroeffentlichte 1691 mehrere 'Wohltemperierte' Stimmungssysteme (Werkmeister I-IV). Das bekannteste (Werkmeister III) verteilt das Komma ungleich: 4 Quinten werden um 1/4 Komma verkleinert (C-G, G-D, D-A, B-F#), die uebrigen bleiben rein. Das Ergebnis sind Tonarten mit unterschiedlichen 'Charakteren' — C-Dur klingt ruhig, Es-Dur mysterioes. Bach schrieb das WTK moeglicherweise fuer eine Werkmeister-Stimmung, nicht fuer Gleichstufigkeit.",
        difficulty = 5,
        funFact = "Ob Bach das Wohltemperierte Klavier fuer Gleichstufigkeit oder eine Werkmeister-Stimmung schrieb, ist bis heute umstritten. Manche Musikwissenschaftler haben sogar behauptet, der Schnecken-Ornament auf dem Titelblatt sei selbst eine codierte Stimmungsvorschrift — ein musikhistorischer Krimi."
    ),

    // Q24
    Question(
        categoryId = 5,
        questionText = "Was ist das 'syntonische Komma' und in welchem Stimmungskontext spielt es eine zentrale Rolle?",
        answerA = "Das Verhaeltnis 81:80 (ca. 21.5 Cent) — die Differenz zwischen der pythagoraeischen grossen Terz (81:64) und der reinen grossen Terz (5:4 = 80:64). Es ist zentral in Mitteltoenig- und Reintonigen Stimmungen",
        answerB = "Die Differenz zwischen dem grossen und dem kleinen Halbton im mitteltoenigen System, entspricht ca. 25 Cent",
        answerC = "Ein Stimmungsproblem speziell bei Streichinstrumenten, bei dem offene Saiten gegenueber gegriffenen Toenen verstimmt klingen",
        answerD = "Das pythagoraeische Komma geteilt durch drei, das bei Dreitonalitaeten (Terzschichtungen) auftritt",
        correctAnswer = 0,
        explanation = "Das syntonische Komma (Didymisches Komma, 81:80 = 21.51 Cent) ist die Differenz zwischen der pythagoraeischen grossen Terz (vier reine Quinten nach oben, geteilt durch zwei = 81:64) und der akustisch reinen grossen Terz (5:4 = 80:64). In der 1/4-Komma-Mitteltoenigkeit wird jede Quinte um 1/4 syntonisches Komma verkleinert (5.38 Cent), sodass grosse Terzen rein werden.",
        difficulty = 5,
        funFact = "Der Name 'syntonisch' stammt vom griechischen Didymos (63 v. Chr. - 10 n. Chr.), der die reine Terz 5:4 erstmals klar beschrieb. Das Komma traegt auch seinen Namen: 'Didymisches Komma'. Zwei Jahrtausende spaeter ist es noch immer Gegenstand musikwissenschaftlicher Debatten."
    ),

    // Q25
    Question(
        categoryId = 5,
        questionText = "Was ist der 'Kammerton' (pitch standard) und wie hat er sich historisch veraendert — und warum ist das fuer die Auffuehrungspraxis relevant?",
        answerA = "Der Kammerton ist die internationale Referenzfrequenz fuer a' (heute 440 Hz seit 1939). Historisch schwankte er enorm: Barockkammerton lag oft bei a'=415 Hz, spaetere Orchesterpraxis bei 430-446 Hz. Hoehere Stimmtoene ermoeglichen helleren Klang aber belasten Saengerinen staerker",
        answerB = "Der Kammerton bezeichnet die Resonanzfrequenz des Konzertsaals, die fuer optimale Akustik eingehalten werden muss",
        answerC = "Eine historische Bezeichnung fuer den Grundton des Basso Continuo im Barockensemble, der alle Stimmen koordiniert",
        answerD = "Der Kammerton ist das a' auf 432 Hz, eine historisch reine Stimmung, die heute von alternativer Medizin bevorzugt wird",
        correctAnswer = 0,
        explanation = "Der Kammerton (a' = Referenzfrequenz) variierte historisch erheblich. Messungen an erhaltenen Barockinstumenten und Orgeln zeigen Spannweiten von a'=390 Hz (Chorton tief) bis a'=480 Hz (Kammerton hoch). Die ISO-Norm 16 (1939, bestaetigt 1975) standardisierte a'=440 Hz. In HIP-Auffuehrungen wird Barockmusik oft bei a'=415 Hz (genau einen Halbton tiefer als 440) gespielt, um historischen Klangcharakter zu erzielen.",
        difficulty = 5,
        funFact = "Die Behauptung, 432 Hz sei 'natuerlicher' oder 'heilsamer' als 440 Hz, ist wissenschaftlich nicht belegbar und ein weit verbreiteter Mythos im Internet. 440 Hz ist eine internationale Konvention — und historisch war der Kammerton oft noch hoeher, z.B. 446 Hz im Wiener Orchesterklang des 19. Jahrhunderts."
    ),

    // Q26
    Question(
        categoryId = 5,
        questionText = "Was versteht man unter 'Just Intonation' (Reintonigkeit) und welche praktischen Probleme entstehen bei ihrer Anwendung im Ensemble?",
        answerA = "Just Intonation verwendet ausschliesslich reine Intervalle mit einfachen Frequenzverhaeltnissen (Quinte 3:2, Terz 5:4, grosse Septime 15:8). Das Problem: Eine Reihe von Quinten oder Terzen fuehrt zu Komma-Verschiebungen — Melodien koennen die Tonalitaet unbemerkt in einen anderen Stimmungsbereich 'driften'",
        answerB = "Just Intonation bedeutet gleichstufig gestimmte Instrumente im Gegensatz zu historischen Temperaturen, die 'unjust' (ungerecht) waren",
        answerC = "Eine Stimmungspraxis, bei der alle Instrumente eines Ensembles exakt nach elektronischem Tuner stimmen, unabhaengig von den gespielten Akkorden",
        answerD = "Das Stimmungssystem mit 31 Toenen pro Oktave, das alle einfachen Frequenzverhaeltnisse bis zur Primzahl 7 darstellen kann",
        correctAnswer = 0,
        explanation = "Just Intonation (z.B. 5-Limit Just Intonation) nutzt nur Vielfache von 2, 3, 5 als Frequenzteiler. Das Komma-Problem: Spielt man vier aufsteigende reine Quinten von C aus (C-G-D-A-E) und vergleicht mit der direkten reinen Terz C-E, gibt es ein syntonisches Komma Abweichung (81:80). Melodisch kann sich die Stimmung unmerklich verschieben. Harry Partch loeste das Problem durch 43-Ton-Skalen und Spezialinstrumente.",
        difficulty = 5,
        funFact = "Harry Partch (1901-1974) baute seine eigenen Instrumente fuer Just Intonation — darunter das Cloud Chamber Bowl (aus Glasgefaessen des Manhattan-Projekts), das Spoils of War und den Chromelodeon. Er weigerte sich, Kompromisse einzugehen, und ist eine der radikalsten Figuren der amerikanischen Musikgeschichte."
    ),

    // Q27
    Question(
        categoryId = 5,
        questionText = "Was ist der 'Barockton' (baroque pitch) und welchen Einfluss hat er auf die Transposition von Basso-Continuo-Stimmen?",
        answerA = "Da der Barockton (a'=415 Hz) genau einen gleichstufigen Halbton unter dem modernen a'=440 Hz liegt, koennen moderne Pianisten Barockpartituren in der notierten Tonart spielen, wenn das Ensemble auf 415 Hz stimmt — oder eine Halbton hoeher transponieren, wenn das Ensemble auf 440 Hz spielt",
        answerB = "Barockton bezeichnet einen speziellen Klangcharakter (dunkel, vibratofrei) der Barockinstrumente, nicht eine bestimmte Stimmhoehe",
        answerC = "Der Barockton liegt bei a'=392 Hz (genau eine Quarte tiefer als modern), was zu grossen kompositorischen Einschraenkungen fuer Cembalo-Spieler fuehrt",
        answerD = "Der Begriff bezeichnet nur die Stimmung von Barocktrompeten in D, da diese Instrumente keinen anderen Ton spielen konnten",
        correctAnswer = 0,
        explanation = "In der historisch informierten Auffuehrungspraxis (HIP) wird Barockmusik meist bei a'=415 Hz gespielt — was einem gleichstufigen Halbton unter 440 Hz entspricht (440 / 2^(1/12) = 415.3 Hz). Dieser Zufall ermoeglicht praktische Transpositionstricks: Ein modernes Klavier stimmt auf 440 Hz, der Cembaliste auf 415 Hz — beide koennen in ihrer notierten Tonart spielen, wenn der Cembaliste eine Halbton tiefer klingt. Fuer Orgeln ist das aufwendig, da Orgeln fest gestimmt sind.",
        difficulty = 5,
        funFact = "Nicht alle Barockorchster spielten auf 415 Hz — das war auch im Barock regional sehr unterschiedlich. In Norddeutschland konnte der Chorton bis zu einem Ganztton tiefer liegen als der Kammerton (a'=392 Hz). Bach musste deshalb Transpositionspartituren fuer seine Kantaten schreiben, wenn er Orgel und Orchester kombinierte."
    ),

    // Q28
    Question(
        categoryId = 5,
        questionText = "Was ist 'Mikrotonalitaet' und welche historischen Vorlaefer gibt es vor der avantgardistischen Verwendung im 20. Jahrhundert?",
        answerA = "Mikrotonalitaet bezeichnet Intervalle kleiner als der gleichstufige Halbton. Historische Vorlaefer: arabische Maqam-Systeme (Vierteltone), altgriechische Enharmonik (Dritteltone in Enarmonion-Tetrachorden), 31-Ton-Orgeln des 16. Jahrhunderts (Nicola Vicentino)",
        answerB = "Mikrotonale Musik existierte erst ab dem 20. Jahrhundert — sie ist eine Erfindung von Juliaen Carrillo und Charles Ives ohne historische Vorlaefer",
        answerC = "Mikrotonalitaet bezeichnet ausschliesslich die Schwebungen in der Chormusik durch leichte Intonationsdifferenzen zwischen Saengern",
        answerD = "Historische Vorlaefer sind nur in aussereuropaeischer Musik vorhanden; europaeische Musik kannte bis 1900 keine Intervalle kleiner als den Halbton",
        correctAnswer = 0,
        explanation = "Mikrotonalitaet hat tiefe historische Wurzeln: Die altgriechische Enharmonik verwendete Mikroterviertel, arabisch-persische Maqam-Systeme kennen systematisch Dritteltone. Nicola Vicentino baute 1555 das 'Archicembalo' mit 31 Toenen pro Oktave fuer reine Stimmung. Im 20. Jh.: Juliaen Carrillo (16tel-, 32tel-Tone), Alois Haba (Vierteltone), Ivan Wyschnegradsky, Harry Partch (43-Ton-Skala), Giacinto Scelsi (Mikrogltissandi).",
        difficulty = 5,
        funFact = "Nicola Vicentino baute sein Archicembalo, um die angeblich verloren gegangene altgriechische Enharmonik wiederherzustellen — tatsaechlich ist unklar, ob die Griechen Mikrotone wirklich so melopraktisch einsetzten. Aber sein Instrument existiert noch: Ein Exemplar steht im Musikinstrumenten-Museum Berlin."
    ),

    // ── Raummusik / Klanginstallation (8) ───────────────────────────────────

    // Q29
    Question(
        categoryId = 5,
        questionText = "Was bezeichnet Stockhausens Begriff 'Raummusik' und wie setzte er ihn in 'Gruppen' fuer drei Orchester (1955-57) um?",
        answerA = "Drei Orchester sind im Raum um das Publikum herum verteilt (links, rechts, hinten), jedes mit eigenem Dirigenten. Klang kann als Wellen zwischen Orchestern wandern, sich ueberlagern oder veraeumlichen — raeumliche Bewegung ist kompositorisches Parameter",
        answerB = "Das Publikum sitzt auf rotierenden Stuhlen, waehrend das Orchester fest auf einer Buhne sitzt, um raeumliche Bewegung zu erzeugen",
        answerC = "Stockhausen verteilt Lautsprecher im gesamten Konzertsaal und ubertraegt das Orchester verraeumlicht durch Delay-Systeme",
        answerD = "Gruppen bedeutet, dass das Orchester in drei Gruppen aufgeteilt ist, die aber alle auf derselben Buhne sitzen und sequenziell spielen",
        correctAnswer = 0,
        explanation = "In 'Gruppen' (1955-57) postiert Stockhausen drei Orchester von je ca. 60 Musikern links, rechts und hinter dem Publikum, jedes mit eigenem Dirigenten. Die drei Orchester spielen in unterschiedlichen Tempi (polyrhythmisch koordiniert), koennen aber auch gemeinsame Hoehepunkte synchronisieren. Klaenge 'wandern' durch den Raum: Ein Motivfragment beginnt links, zieht durch das Zentrum nach rechts. Die Raumposition ist ein expliziter Kompositionsparameter.",
        difficulty = 5,
        funFact = "Die Uraufgabe von 'Gruppen' 1958 in Koeln mit drei Dirigenten (Stockhausen, Boulez, Maderna) war ein Medienereignis. Der Saal musste speziell umgebaut werden. Das Stueck gilt als Wegbereiter der Mehrkanal-Raumklangkomposition, die in der heutigen Surround-Technologie ihre Nachfolge findet."
    ),

    // Q30
    Question(
        categoryId = 5,
        questionText = "Was sind die akustischen Grundprinzipien von Bernhard Leitmeisters 'Ambisonics'-System und wie unterscheidet es sich von Stereo und 5.1-Surround?",
        answerA = "Ambisonics kodiert Schallfeld-Information als sphaerenharmonische Komponenten (W = omnidirektional, X/Y/Z = gerichtete Komponenten). Es ist kanalunabhaengig und kann auf beliebige Lautsprecher-Konfigurationen decodiert werden — im Gegensatz zu Stereo (2 Kanale, kanalgebunden) und 5.1 (festgelegte 6 Kanale ohne volle Hoehe)",
        answerB = "Ambisonics ist ein Mehrkanal-System mit 8 fest platzierten Lautsprechern im Wuerfel, das durch phasenversetzte Wiedergabe dreidimensionalen Klang erzeugt",
        answerC = "Das System wurde von Peter Fellgett entwickelt und verwendet ausschliesslich Zeitverzoegerungen ohne Amplitudenunterschiede zur Raumdarstellung",
        answerD = "Ambisonics ist der Markenname eines 1970er-Jahre-Quadrophonie-Systems mit vier Kanalen, das sich gegen Quadraphony von Sansui durchsetzte",
        correctAnswer = 0,
        explanation = "Ambisonics (entwickelt von Michael Gerzon und Peter Fellgett in den 1970ern am Oxford Mathematical Institute) verwendet B-Format-Kodierung: W (Druckkomponente), X (vorne-hinten), Y (links-rechts), Z (oben-unten). Diese kanalunabhaengige Kodierung kann auf jede Lautsprecher-Anordnung decodiert werden. Higher-Order Ambisonics (HOA) verwendet mehr Koeffizientenn fuer hoehere Aufloesung. Es ist Standard in VR-Audio und modernen Raumklang-Installationen.",
        difficulty = 5,
        funFact = "Google Spatial Audio und YouTube 360 verwenden Ambisonics als Format. Was als avantgardistisches Forschungsprojekt der 1970er Jahre begann, ist heute in jedem Smartphone als VR-Audio-Standard implementiert — Gerzon erlebte diesen Triumph nicht mehr, er starb 1996."
    ),

    // Q31
    Question(
        categoryId = 5,
        questionText = "Was ist die kompositorische Idee hinter Alvin Luciers 'I Am Sitting in a Room' (1969) und welches akustisches Phaenomen macht es hoerbar?",
        answerA = "Lucier spielt eine Aufnahme seiner Stimme im Raum ab, nimmt das Ergebnis auf und wiederholt den Prozess viele Male. Die Resonanzfrequenzen des Raumes werden mit jeder Generation verstaerkt, bis die Sprache in reine Raumresonanzen aufgeloest ist",
        answerB = "Er platziert Mikrofone in verschiedenen Raumecken und mischt die phasenversetzten Signale zu einer Raumklang-Skulptur",
        answerC = "Lucier singt im Raum und traegt dabei ein spezielles Helm-Mikrofon, das den Koerperschall direkt aufnimmt — die Raumakustik wird bewusst ausgeschlossen",
        answerD = "Eine Installation, bei der Sprecher in einem Anechoischen Raum aufgenommen werden und die Stille selbst zum kompositorischen Material wird",
        correctAnswer = 0,
        explanation = "Alvin Luciers 'I Am Sitting in a Room' (1969) ist ein Paradebeispiel fuer Raumakustik als kompositorisches Material. Er liest einen Text vor, spielt ihn im Raum ab, nimmt das Ergebnis auf und wiederholt. Die Eigenfrequenzen (Raummoden) des Raumes werden mit jedem Durchgang exponentziell verstaerkt. Nach 30-40 Durchgaengen ist die Sprache in reine Raumresonanz-Toene zerfallen. Der Raum wird sozusagen 'sichtbar'.",
        difficulty = 5,
        funFact = "Lucier stotterte und thematisierte in seiner Erklarung zu 'I Am Sitting in a Room' explizit seine Sprachbehinderung: Die Akkumulation der Raumfrequenzen wuerde sein Stottern 'glaetten'. Das Stueck hat damit eine zutiefst persoenliche Dimension und ist einer der eloquentesten Momente in der Geschichte der Konzeptkunst."
    ),

    // Q32
    Question(
        categoryId = 5,
        questionText = "Was sind die akustischen Bedingungen eines 'anechoischen Raums' (anechoic chamber) und welche psychoakustischen Erfahrungen beschreiben Personen, die dort laengere Zeit verbringen?",
        answerA = "Vollstaendig reflexionsfreier Raum mit Schallabsorption durch Schaumstoffkeile (>99% Absorption ab 100 Hz). Personen hoeren ihren eigenen Herzschlag, Blutfluss und Atemgeraeusche. Bei laengerer Exposition können Halluzinationen und Orientierungslosigkeit auftreten",
        answerB = "Ein Raum mit perfekter Raumakustik fuer Konzerte, in dem jeder Klang an jedem Platz mit identischer Qualitaet wahrgenommen wird",
        answerC = "Ein schallisolierter Raum ohne Aussengeraeusche, aber mit normaler Reflexion der Innenflaechen — fuer Tonstudio-Aufnahmen verwendet",
        answerD = "Ein zylindrischer Hohlraum mit kontrollierter Nachhallzeit, der die Akustik einer Konzertkirche simuliert",
        correctAnswer = 0,
        explanation = "Anechoische Kammern absorbieren typischerweise 99.99% des Schalls durch keilfoermige Absorber an Waenden, Boden und Decke. Messungen in Orfield Laboratories (Minneapolis) zeigen: Der lauteste angemessene Umgebungslaerm liegt bei -20 dB SPL. Personen hoeren dort Koerpervorgaenge (Herzschlag, Sehnenrauschen, Magenaktivitaet). Nach Minuten bis Stunden berichten viele von Desorientierung, da das Gehirn auf Raumreferenz durch Reflexionen angewiesen ist.",
        difficulty = 5,
        funFact = "John Cage besuchte einen anechoischen Raum an der Harvard University 1951 — die Erfahrung, trotzdem zwei Toene zu hoeren (hoch: Nervensystem, tief: Blutkreislauf), fuehrte ihn zur Erkenntnis: 'Absolute Stille existiert nicht.' Dieses Erlebnis war der konzeptuelle Ursprung von '4'33''."
    ),

    // Q33
    Question(
        categoryId = 5,
        questionText = "Was ist 'Akusmatik' als Konzept in der Musikgeschichte und wer pragte den Begriff in seiner modernen Bedeutung?",
        answerA = "Pierre Schaeffer uebertrug den Begriff 'Akusmatik' (Pythagoraeische Lehrmethode: Schueler hoeren den Lehrer hinter einem Vorhang) auf Musik, die ausschliesslich aus Lautsprechern erklingt ohne sichtbare Klangquelle",
        answerB = "Akusmatik bezeichnet die wissenschaftliche Disziplin der Raumakustik, gepraegte von Hermann von Helmholtz im 19. Jahrhundert",
        answerC = "Ein Kompositionsprinzip, bei dem das Publikum die Augen schliesst waehrend das Orchester spielt, um die visuelle Ablenkung zu eliminieren",
        answerD = "Akusmatik ist der mathematische Teilbereich der Signalverarbeitung, der sich mit der Invertierung von Raumimpulsantworten befasst",
        correctAnswer = 0,
        explanation = "Pierre Schaeffer uebertrug den pythagoraeischen Begriff (Akousmatikoi: Schueler, die Pythagoras hinter einem Vorhang lauschten, um nicht abgelenkt zu werden) auf Musik aus Lautsprechern ohne sichtbare Quelle. Akusmatische Musik ist per definitionem Musik, bei der die Klangquelle nicht sichtbar ist — was das 'ecoute reduite' (reduzierte Hoeren) foerdert. Die akusmatische Tradition umfasst Schaeffer, Francis Dhomont, Barry Truax und viele andere.",
        difficulty = 5,
        funFact = "Pythagoras soll angeblich hinter einem Vorhang unterrichtet haben — ob das historisch wahr ist, ist unklar. Schaeffers Analogie war trotzdem produktiv: Lautsprecher als moderner 'Vorhang' zwischen Klangobjekt und Hoerer. Der Begriff 'Akusmatik' hat sich in der elektroakustischen Musikwelt als Standardbezeichnung durchgesetzt."
    ),

    // Q34
    Question(
        categoryId = 5,
        questionText = "Was versteht man unter 'Klangskulptur' als Kunstgattung und welche Pioniere werden in der Kunstgeschichte genannt?",
        answerA = "Klangskulptur bezeichnet dreidimensionale Kunstobjekte, die selbst Klang erzeugen oder auf Klang reagieren. Pioniere: Harry Bertoia (Klangskulpturen, 1960er), Bernhard Leitner (Klang-Rauminstallationen), Jean Tinguely (kinetische Klangobjekte), und Max Neuhaus (elektronische Umgebungstoenkunst)",
        answerB = "Ausschliesslich Skulpturen die klingende Koerper (Glocken, Metallplatten) integrieren ohne elektronische Verstaerkung — rein akustische Objekte",
        answerC = "Klangskulptur bezeichnet Musikstuecke, die von der Form eines Bildwerks abgeleitet sind — z.B. Kompositionen nach Proportionen der Sagrada Familia",
        answerD = "Eine Unterkategorie der Minimal Music, bei der einfache repetitive Muster zu 'skulpturalen' Klangflaechenarchitekturen aufgeschichtet werden",
        correctAnswer = 0,
        explanation = "Klangskulpturen verbinden Bildende Kunst und Klang in dreidimensionalen Objekten oder Rauminstallationen. Harry Bertoias schwingungsfaehige Metallstange-Skulpturen erzeugten selbst Klang. Bernhard Leitner installierte Lautsprecheranordnungen, die Klang raeumlich als Koerpererlebnis erfahrbar machten. Max Neuhaus installierte dauerhaft klingende Umgebungsklang-Werke im oeffentlichen Raum (z.B. 'Times Square' in New York, 1977).",
        difficulty = 5,
        funFact = "Max Neuhaus' 'Times Square' ist ein dauerhaft installiertes Klangwerk unter einem Metroliuftschacht in Manhattan — ein tiefer, summender Ton, den taeglich Hunderttausende passieren ohne ihn als Kunst wahrzunehmen. Neuhaus nannte das 'listening' statt 'hearing': bewusstes statt passives Wahrnehmen des Raumes."
    ),

    // Q35
    Question(
        categoryId = 5,
        questionText = "Was ist das Konzept hinter Luigi Nonos spaeter 'Raummusik' in Werken wie 'Prometeo' (1984) und welche technologischen Mittel setzte er mit Hans Peter Haller ein?",
        answerA = "Nono verteilte Klang durch ein Netz von Live-Elektronik-Systemen raeumlich: Klang 'wanderte' dynamisch durch den Raum via Halaphon (Lautsprecherrotations-System), Raumfilterung und Echtzeit-Elektronik — Raum wurde zur kompositorischen Dramaturgie",
        answerB = "Nono verwendete ausschliesslich Akustik ohne Elektronik und platzierte Musiker in verschiedenen Etagen eines Kirchenraums fuer natuerlcihe Raumdiffusion",
        answerC = "'Prometeo' wurde in einem speziell gebauten Schiff (Barca) aufgefuehrt, das als schwimmende Konzertnhalle den Zuhoerern eine neue Raumbeziehung ermoeglichte",
        answerD = "Nono setzte computergesteuerte Roboter-Instrumente ein, die sich waehrend der Auffuehrung physisch im Raum bewegten",
        correctAnswer = 0,
        explanation = "Luigi Nonos 'Prometeo — Tragedia dell'ascolto' (1984) wurde in Venedigs San Lorenzo in einem speziell von Renzo Piano gebauten Holz-Pavillon (die 'Arca') uraufgefuehrt. Hans Peter Haller vom Experimentalstudio Freiburg entwickelte das Halaphon (bis zu 8 rotierende Lautsprechergruppen) und Live-Elektronik-Systeme. Klang wird durch den Raum geleitet, gefiltert, verraeumlicht und transformiert in Echtzeit. Raum wird dramaturgisches Element der 'Hoer-Tragoedie'.",
        difficulty = 5,
        funFact = "Renzo Piano (spaeter Architekt des Centre Pompidou und der New York Times Building) baute die 'Arca' als temporaere Holzstruktur innerhalb der Kirche San Lorenzo. Die Verbindung von Nono (linker Avantgardist), Piano (modernster Architekt) und venezianischer Kirchenraeumlichkeit war symbolisch aufgeladen und wurde als kulturelles Ereignis gefeiert."
    ),

    // Q36
    Question(
        categoryId = 5,
        questionText = "Welche historische Traditi on des venezianischen Mehrchoerigkeits-Prinzips (Cori spezzati) beeinflusste die Raummusik des 20. Jahrhunderts und wer waren die Hauptvertreter?",
        answerA = "Adrian Willaert und Giovanni Gabrieli entwickelten an der Markuskirche Venedig (deren Balkone und Emporen ideal waren) das polychore Prinzip: Mehrere Choere antworteten sich raeumlich — ein Vorlaefer der Raumklang-Idee des 20. Jahrhunderts",
        answerB = "Das venezianische Mehrchoerigkeitsprinzip war ausschliesslich eine liturgische Praxis ohne akustischen Raumeffekt — die Chore sangen nur zeitlich versetzt, nicht raeumlich getrennt",
        answerC = "Heinrich Schutz erfand das Mehrchoerigkeitsprinzip in Venedig, wohin er reiste, um es zu importieren — Gabrieli spielt in der Geschichte nur eine marginale Rolle",
        answerD = "Das Cori-spezzati-Prinzip ist eine moderne Falschinterpretation mittelalterlicher Notation — es gab in der Praxis nie raeumlich getrennte Chore",
        correctAnswer = 0,
        explanation = "Adrian Willaert (Kapellmeister San Marco ab 1527) und Giovanni Gabrieli entwickelten das Mehrchoerigkeitsprinzip in der ideal gebauten Markuskirche Venedig: Die Kirche hat zwei gegenueberliegende Chor-Emporen. Gabrielis 'Sacrae Symphoniae' (1597, 1615) nutzen die raeumliche Trennung expressiv — Klang springt zwischen Emporen, crescendiert und decrescendiert als raeumliche Bewegung. Heinrich Schuetz lernte diese Technik als Gabrielis Schueler und brachte sie nach Deutschland.",
        difficulty = 5,
        funFact = "Giovanni Gabrielis 'In Ecclesiis' fuer bis zu 19 Stimmen in 4 Choren gilt als Krone der venezianischen Mehrchoerigkeitskunst. In der Markuskirche, wo Gabrieli es schrieb und auffuehrte, klingt es bis heute einzigartig — die Akustik des Kirchenraums ist integral Teil des Stucks."
    ),

    // ── Musikaesthetik / Philosophie-Vertiefung (7) ─────────────────────────

    // Q37
    Question(
        categoryId = 5,
        questionText = "Was versteht Hanslick in 'Vom Musikalisch-Schoenen' (1854) unter dem Begriff 'toenend bewegte Formen' als Inhalt der Musik?",
        answerA = "Hanslick argumentiert, dass Musik keinen aussermusikalischen Inhalt (Gefuehle, Ideen) ausdrueckt, sondern ihr Inhalt die musikalischen Formen selbst sind — dynamisch sich entfaltende Klangverhaeltnisse, die autonomes aesthetisches Objekt sind",
        answerB = "Er beschreibt die Bewegung der Stimmen im Kontrapunkt als koerperliche Erfahrung, die metaphorisch als 'Tanz der Formen' den eigentlichen Musikinhalt bildet",
        answerC = "Hanslick meint damit das dramatische Schauspiel, das Musik in der Oper begleitet — die 'tonenden Formen' sind die singende Stimme im Buehnenraum",
        answerD = "Der Begriff bezeichnet Hanslicks Vorstellung, dass Musik Naturgeraeusche imitiert und ihr Inhalt die akustischen Muster der natuerlichen Welt sind",
        correctAnswer = 0,
        explanation = "Eduard Hanslicks 'Vom Musikalisch-Schoenen' (1854) ist das grundlegende Werk des musikalischen Formalismus. Sein Kern: Musik drueckt keine Gefuehle aus und hat keinen semantischen Inhalt — ihr 'Inhalt' sind 'toenend bewegte Formen'. Das bedeutet: Die autonomen Verhaeltnisse von Toenen, Rhythmen und harmonischen Beziehungen, wie sie sich zeitlich entwickeln, konstituieren das aesthetische Objekt der Musik ohne Aussenbezug.",
        difficulty = 5,
        funFact = "Hanslick und Wagner waren persoenliche Feinde. Hanslick lehnte Wagners Programm-Musik und die Idee des 'Gesamtkunstwerks' grundsaetzlich ab — Musik sollte autonom sein, nicht Dienerin des Dramas. Wagner raeachte sich: In 'Die Meistersinger' ist die Figur 'Beckmesser' ein satirisches Portait Hanslicks."
    ),

    // Q38
    Question(
        categoryId = 5,
        questionText = "Was ist Adornos Hauptkritik an der Kulturindustrie im Bereich der populaeren Musik, wie in 'Dialektik der Aufklaerung' (1944) dargestellt?",
        answerA = "Adorno kritisiert, dass die kapitalistische Kulturindustrie Musik zur standardisierten Ware macht, Pseudo-Individualitaet vortaeuscht und das Publikum zur passiven Konsumption konditioniert statt emanzipatorisches kritisches Denken zu foerdern",
        answerB = "Er kritisiert ausschliesslich Jazz als minderwertig gegenueber der europaeischen Kunstmusik, ohne die strukturellen Produktionsbedingungen zu thematisieren",
        answerC = "Adorno fordert staatliche Subventionierung der E-Musik gegenueber U-Musik als einziges Mittel gegen Kulturverfall",
        answerD = "Die Hauptkritik betrifft die technische Qualitaet von Tonaufnahmen, die Musik ihrer raeumlichen Lebendigkeit beraube und sie zur 'toten Konserve' mache",
        correctAnswer = 0,
        explanation = "Adorno und Horkheimer beschreiben in der 'Dialektik der Aufklaerung' (1944, Exilwerk in Los Angeles) die Kulturindustrie als System, das Waren-Charakter und aesthetische Autonomie untrennbar verbindet. Musik wird in Formeln standardisiert (Verse-Chorus, 3-Minuten-Struktur), waehrend Schein-Individualitaet (Interpreten-'Stars') suggeriert wird. Das passive Konsum-Publikum verliert Faehigkeit zur kritischen aesthetischen Erfahrung. Eine des einflussreichsten und umstrittensten Thesen der Musiksoziologie.",
        difficulty = 5,
        funFact = "Adorno war selbst professioneller Komponist (Schueler von Alban Berg) und spielte Klavier auf hohem Niveau. Sein Hass auf Jazz war partiell ein Missverstaendnis — er kannte vor allem den kommerzialisierten Tin-Pan-Alley-Jazz, nicht den Improvisations-Jazz von Parker oder Coltrane, den er vermutlich anders bewertet haette."
    ),

    // Q39
    Question(
        categoryId = 5,
        questionText = "Was versteht Roland Barthes unter dem 'grain de la voix' (Koernung der Stimme) in seinem gleichnamigen Essay (1972)?",
        answerA = "Barthes bezeichnet damit die individuelle koerperliche Materialitaet der Stimme — das Reiben von Zunge, Gaumen, Kehle im Gesang, das jenseits von Technik und semantischem Ausdruck eine unmittelbare Koerper-Lust vermittelt",
        answerB = "Der Begriff bezeichnet die Granulatstruktur des Tonsignals bei analogen Vinylaufnahmen, die Barthes aesthetisch der CD-Digitalaufnahme vorzieht",
        answerC = "Barthes meint damit die ethnische und kulturelle Faerbung einer Stimme, die ihre geografische Herkunft unveraenderbar einschreibt",
        answerD = "Der Ausdruck beschreibt die Vibrato-Charakteristik barocker Saengerinen, die Barthes als authentischer gegenueber modernem Belcanto bewertet",
        correctAnswer = 0,
        explanation = "Roland Barthes' 'Le grain de la voix' (1972) unterscheidet zwischen 'pheno-chant' (das Ausgedrueckte: Gefuehle, Texte, Phrasen) und 'geno-chant' (das Koerperliche: Materialitaet von Zunge, Gaumen, Stimmbander, Atem). Das 'grain' ist das geno-sang-Moment — der koerperliche Reibungswiderstand der Stimme, der nicht semantisch ist, aber unmittelbare Koerper-Lust (jouissance) erzeugt. Barthes vergleicht Dietrich Fischer-Dieskau (pheno-last) mit Charles Panzera (grain-reich).",
        difficulty = 5,
        funFact = "Barthes beschreibt das 'grain' einer Stimme als etwas, das man nicht mit Adjektiven beschreiben kann: 'Man kann die grain nicht erlaeutern — sie liefert sich der Erlaeuterung nicht aus.' Dies ist typisch fuer Barthes' semiologischen Stil: Er thematisiert, was sich Analyse entzieht, als das eigentlich Interessante."
    ),

    // Q40
    Question(
        categoryId = 5,
        questionText = "Was versteht Peter Kivy unter 'enhancionism' (Verstaerkungsthese) im Verhaeltnis von Musik und Emotion?",
        answerA = "Musik exprimiert Emotionen nicht direkt, sondern 'verstaerkt' (enhances) das aesthetische Erlebnis durch kontextuelle Wechselwirkung mit Gedanken und Erinnerungen des Hoerenden — eine moderatere Position zwischen Absolutismus und expressivem Kognitivismus",
        answerB = "Kivys These, dass Musik Emotionen ausloest, indem sie physiologische Erregungsreaktionen (arousal) verstaerkt, die zuvor schon latent vorhanden waren",
        answerC = "Ein Konzept, bei dem Programm-Musik die Faehigkeit hat, schwache visuelle Vorstellungen im Hoerer zu verstaerken und zu intensivieren",
        answerD = "Die Ansicht, dass lebende Auffuehrungen emotionaler wirken als Tonaufnahmen, weil raeumliche Praesenz des Performers die Emotion 'verstaerkt'",
        correctAnswer = 0,
        explanation = "Peter Kivy ist einer der fuehrenden analytischen Musikphilosophen. In 'Music Alone' (1990) und 'New Essays on Musical Understanding' (2001) diskutiert er verschiedene Emotionstheorien. Der 'Enhancionism' beschreibt, wie Musik im Kontext aesthetischer Erfahrung emotionale Faerbungen 'aufwertet' — nicht durch einfache Auslosung, sondern durch kognitive Interaktion zwischen Musik, Erinnerung und individuellem Hoerkontext. Es ist eine Mittelposition in der analytischen Aesthtik-Debatte.",
        difficulty = 5,
        funFact = "Kivy unterscheidet zwischen Musik, die Emotionen 'hat' (expressiv im Sinne von kognitiven Eigenschaften) und Musik, die Emotionen 'ausloest' (eine kausale Relation). Diese Distinktion klingt akademisch, ist aber praktisch relevant: Ist traurige Musik traurig, weil sie uns traurig macht, oder ist sie traurig, weil sie strukturelle Eigenschaften von Trauer hat — unabhaengig davon, ob wir uns traurig fuehlen?"
    ),

    // Q41
    Question(
        categoryId = 5,
        questionText = "Was ist das philosophische Kernkonzept von Vladimir Jankelewitchs Musikaesthetik, wie er es in 'La Musique et l'ineffable' (1961) entwickelt?",
        answerA = "Jankelewitsch argumentiert, dass das Wesen der Musik im 'Nicht-Sagbaren' liegt: Sie ist presque-rien (fast-nichts), das dennoch alles sagt — sie entgleitet dem Begriff, sobald man versucht sie zu beschreiben, und ist nur im Horeakt selbst praesent",
        answerB = "Er entwickelt eine streng formalistische Theorie, nach der Musik ausschliesslich durch ihre immanente Struktur verstanden werden kann, unabhaengig von Kontext und Hoerer",
        answerC = "Jankelewitsch vertritt eine Nachahmungstheorie: Musik imitiert die zeitliche Struktur von Emotionen, weshalb sie diese 'sage' ohne Worte zu brauchen",
        answerD = "Er verbindet Muskaesthetik mit Bergsonischem Zeitbegriff: Musik ist die reinste Verkoerperung des 'duree', des qualitativen Zeitflusses, den Bergson beschreibt",
        correctAnswer = 0,
        explanation = "Vladimir Jankelewitchs 'La Musique et l'ineffable' (1961) ist ein Schluesselwerk der franzoesischen Musikaesthetik. Sein Kerngedanke: Musik ist 'presque-rien' (fast-nichts) — sie hat keine Bedeutung im begrifflichen Sinn, entgleitet der Beschreibung, und ist nur im Akt des Hoerens real. Jankelewitsch verbindet Bergson, Husserl und eigene Kategorie des 'Insaisissable' (Ungreifbaren). Musik ist das Medium des Unsagbaren schlechthin.",
        difficulty = 5,
        funFact = "Jankelewitsch war nicht nur Philosoph, sondern auch Pianist und Debussy-Spezialist. Sein Buch 'Debussy et le mystere' ist eine der schoensten Verbindungen von philosophischer Reflexion und Musikanalyse. Er liebte die franzoesische Impressionismus-Tradition tief und sah in ihr den reinsten Ausdruck seiner aesthetischen Ideen."
    ),

    // Q42
    Question(
        categoryId = 5,
        questionText = "Was versteht man in der Musikaesthetik unter dem Konzept der 'autopoietischen Schleife' (Autopoiesis) nach Niklas Luhmanns Systemtheorie, angewendet auf Musik?",
        answerA = "Musik ist ein selbsterzeugendes System: Es produziert durch seine eigenen Operationen (Klang, Kommunikation ueber Klang) die Elemente, aus denen es besteht — es ist operational geschlossen, aber kognitiv offen fuer Umweltreize",
        answerB = "Die autopoietische Schleife bezeichnet die Feedback-Verbindung zwischen Dirigent und Orchester, durch die sich das Ensemble selbst reguliert",
        answerC = "Luhmann beschreibt damit den Kreislauf zwischen Komposition, Auffuehrung und Rezeption als selbsterhaltenden Reproduktionsprozess der Musikinstitution",
        answerD = "Ein Konzept aus der Computermusik: Algorithmen, die ihre eigenen Parameter in Echtzeit modifizieren und so 'selbstlernende' Kompositionssysteme erzeugen",
        correctAnswer = 0,
        explanation = "Niklas Luhmanns Systemtheorie beschreibt Gesellschaftssysteme als autopoietisch: Sie reproduzieren sich durch ihre eigenen Operationen. Angewendet auf Musik (z.B. durch Luhmann selbst in 'Die Kunst der Gesellschaft', 1995): Das Kunstsystem produziert durch Kommunikation ueber Kunstwerke (Kritik, Theorie, Auffuehrung) die Unterscheidungen, die es konstituieren. Musik als System erzeugt selbst die Unterscheidung musikalisch/nicht-musikalisch.",
        difficulty = 5,
        funFact = "Luhmanns Systemtheorie ist in der Musikwissenschaft umstritten, weil sie Musik von Intentionen und Subjekten abstrahiert. Kritiker wie Volker Kalisch sehen darin einen Verlust des Hoerenden als handelndes Subjekt. Begruender sehen es als Gewinn: Endlich eine Beschreibung von Musik, die nicht von Genie-Mythos und Komponisten-Intentionen abhaengt."
    ),

    // Q43
    Question(
        categoryId = 5,
        questionText = "Was ist der Kern von Susanne Langers Theorie der Musik als 'Symbol des Gefuehlslebens' in 'Philosophy in a New Key' (1942)?",
        answerA = "Langer argumentiert, Musik sei ein nicht-diskursives Symbol: Sie teilt die logische Form von emotionalen Erlebnissen (Spannung, Aufloesung, Fluss) ohne deren Inhalt zu benennen — sie ist ein 'virtuelles Symbol' fuer Gefuehlsstrukturen",
        answerB = "Langer behauptet, Musik druecke Gefuehle direkt aus, indem Komponisten ihre eigenen emotionalen Erfahrungen in klingende Formen uebersetzen",
        answerC = "Ihre Theorie besagt, Musik sei ein diskursives Zeichensystem wie Sprache, das jedoch Emotionen statt Begriffe als Zeicheninhalt verwendet",
        answerD = "Langer entwickelt eine evolutionsbiologische Theorie: Musik entstand als ritualisierter emotionaler Ausdruck und tragt diesen biologischen Kern als Symbol in sich",
        correctAnswer = 0,
        explanation = "Susanne Langers 'Philosophy in a New Key' (1942) unterscheidet diskursive Symbole (Sprache: Bedeutung durch Konvention, sequenziell) von praesentativen Symbolen (Musik: Bedeutung durch Gestalt, simultan). Musik ist ein praesentatives Symbol — es teilt die 'morphologische Verwandtschaft' (logische Form) mit Gefuehlsstrukturen, ohne deren spezifischen Inhalt zu bezeichnen. Ein Meilenstein der amerikanischen symbolischen Philosophie.",
        difficulty = 5,
        funFact = "Langer war eine der ersten einflussreichen Philosophinnen in einer maennlich dominierten Disziplin. 'Philosophy in a New Key' war ein Bestseller — fuer ein Philosophiebuch ungewoehnlich. Sie beeinflusste nicht nur Musikaesthetik, sondern auch Tanztheorie und visuelle Kunstphilosophie durch ihre Kategorie der 'virtual powers'."
    ),

    // ── Computermusik / Klangsynthese-Algorithmen (7) ───────────────────────

    // Q44
    Question(
        categoryId = 5,
        questionText = "Was ist 'Physical Modelling Synthesis' (Koerpermodell-Synthese) und worin unterscheidet es sich grundlegend von subtraktiver und additiver Synthese?",
        answerA = "Physical Modelling simuliert das physikalische Schwingungsverhalten realer Instrumente durch Differentialgleichungen (Saiten, Schallroehren, Membranenschwingung) — statt Klang direkt zu erzeugen, wird der physikalische Prozess der Klangentstehung modelliert",
        answerB = "Physical Modelling bezeichnet eine Synthesetechnik, bei der gescannte physische Objekte (Steine, Holz) als Wellenform-Samples verwendet werden",
        answerC = "Eine Technik, bei der reale Instrumentenaufnahmen durch algorithmische Transformation in neue Klange umgewandelt werden, ohne das Instrument selbst zu modellieren",
        answerD = "Die digitale Modellierung der Raumakustik physikalischer Konzertsale, die dann auf ansonsten trockene Aufnahmen angewendet wird",
        correctAnswer = 0,
        explanation = "Physical Modelling (PM) Synthese modelliert den physikalischen Prozess der Klangerzeugung: Fuer eine Geige werden Bogendruck, Saitensteifigkeit, Resonanzkoerper und Luftkopplung als mathematische Gleichungen formuliert und in Echtzeit geloest. Bekannte Algorithmen: Karplus-Strong (vereinfachtes Saitenmodell, 1983), waveguide synthesis (Julius Smith), CORDIS-ANIMA. Im Gegensatz dazu: Additive Synthese baut Klang aus Sinusteilen auf; Subtraktion filtert aus Rauschen.",
        difficulty = 5,
        funFact = "Der Karplus-Strong-Algorithmus (Kevin Karplus, Alex Strong, 1983) ist einer der elegantesten Algorithmen der Informatik: Eine kurze Rauschsequenz in einem Verzoegerungsloop mit Tiefpassfilter klingt verbluffend nach einer gezupften Saite. Die Saitenlaenge bestimmt die Grundfrequenz, die Filtercharakteristik den Klangcharakter — in wenigen Codezeilen."
    ),

    // Q45
    Question(
        categoryId = 5,
        questionText = "Was ist die 'Wavenet'-Architektur (DeepMind, 2016) und was ist ihre Bedeutung fuer die Klangsynthese?",
        answerA = "WaveNet ist ein autoregressive s neuronales Netz (dilated causal convolutions), das direkt Audiosignale Probe fuer Probe generiert und erstmals natuerlich klingende Sprachsynthese ohne konkatenative Methoden ermoeglichte",
        answerB = "Ein Kompressions-Algorithmus von DeepMind, der Audiodateien ohne Qualitaetsverlust auf 1/10 der Dateigroe sse reduziert",
        answerC = "Ein Echtzeit-Transpositions-Algorithmus, der Instrumentalklang ohne Zeitveraenderung in beliebige Tonarten transponiert",
        answerD = "WaveNet ist ein GAN-basiertes System (Generative Adversarial Network), bei dem Diskriminator und Generator gegeneinander audiogene Samples erzeugen",
        correctAnswer = 0,
        explanation = "WaveNet (Oord et al., DeepMind 2016) ist ein autoregressive s Deep-Learning-Modell, das Audiowellenformen direkt Probe fuer Probe (16000/s) generiert. Es verwendet gestapelte dilatierte kausale Convolutional Layers, die exponentiell wachsende Rezeptionsfelder ohne rekurrente Verbindungen ermoglichen. WaveNet revolutionierte Text-to-Speech: Google ersetzte seine Sprachsynthese durch WaveNet und reduzierte den 'Mean Opinion Score'-Gap zu menschlicher Sprache drastisch.",
        difficulty = 5,
        funFact = "WaveNet-Training war 2016 noch viel zu rechenintensiv fuer Echtzeit-Anwendung (1000x langsamer als Echtzeit). Google entwickelte dann 'Parallel WaveNet' (2017) mit Wissensdestillation, das Echtzeit erreichte. Heute steckt WaveNet-Technologie in Google Assistant-Stimmen, die Millionen taeglich hoeren."
    ),

    // Q46
    Question(
        categoryId = 5,
        questionText = "Was ist die MUSIC N-Programmiersprache und welche historische Bedeutung hat sie fuer die Computermusik?",
        answerA = "MUSIC N (MUSIC I-V, Bell Labs, 1957-68) von Max Mathews ist die erste Softwareumgebung fuer digitale Klangsynthese auf einem Computer. Sie fuehrte das Konzept von 'Unit Generators' (wiederverwendbare Klangsynthese-Bausteine) ein, das bis heute in Csound, SuperCollider und Max/MSP weiterlebt",
        answerB = "MUSIC N ist ein proprietaeres Notationssystem von IBM, das Partituren in MIDI-Daten konvertiert und auf fruehen Computern der 1970er ausfuehrte",
        answerC = "Eine Hochsprache des IRCAM Paris fuer algorithmische Komposition, die OpenMusic und der LISP-basierten LISP-Tradition voranging",
        answerD = "Das erste Betriebssystem fuer Sampler-Hardware (Fairlight CMI, E-mu Emulator), das die Basis fuer spaetere DAW-Software bildete",
        correctAnswer = 0,
        explanation = "Max Mathews entwickelte MUSIC I (1957) am Bell Labs als ersten Schritt digitaler Klangsynthese am Computer — er synthetisierte eine 17-sekuendige Melodie auf einem IBM 704. MUSIC IV (1963) und MUSIC V (1969) fuehrten das 'Unit Generator'-Konzept ein: wiederverwendbare Synthesemodule (Oszillatoren, Filter, Huellkurven) die zu Klangsynthese-Netzwerken verbunden werden. Alle modernen Synthesesprachen (Csound, SuperCollider, Pure Data, Max/MSP) sind direkte Nachkommen.",
        difficulty = 5,
        funFact = "Max Mathews liess 1961 den IBM 7094-Computer 'Daisy Bell (Bicycle Built for Two)' singen — die erste Computergesangsaufnahme der Geschichte. Als Arthur C. Clarke und Stanley Kubrick '2001: A Space Odyssey' produzierten, liessen sie den sterbenden HAL 9000 genau dieses Lied singen — ein Hommage an Mathews Pionierarbeit."
    ),

    // Q47
    Question(
        categoryId = 5,
        questionText = "Was ist 'Spectral Morphing' (Klangmorphing) als Synthesetechnik und welche Algorithmen werden typischerweise verwendet?",
        answerA = "Spektrales Morphing interpoliert zwischen den Kurzzeit-Spektren (STFT) zweier Klangquellen: Amplitude und Phase jedes Frequenzbands werden zeitlich geblended, wodurch ein Klang fliessend in einen anderen uebergeht — Algorithmen: Cross-synthesis, PSOLA, Vocoder-Hybride",
        answerB = "Eine Technik, bei der zwei Musikstuecke rhythmisch aneinander angepasst werden ohne ihre Spektralcharakteristik zu veraendern",
        answerC = "Spektrales Morphing ist ein Kompressionsverfahren, das aehnliche Spektralabschnitte eines Klangsignals zusammenfasst, um Dateigroe sse zu reduzieren",
        answerD = "Die mathematische Interpolation zwischen zwei MIDI-Partituren durch parametrische Mischung der Notenwerte",
        correctAnswer = 0,
        explanation = "Spektrales Morphing (Cross-Synthesis) analysiert beide Quellklange durch STFT (Kurzzeit-Fourier-Transformation) und interpoliert Amplitude und Phasenspektrum jedes Frames zeitlich. Bei 0% klingt es wie Klang A, bei 100% wie Klang B, dazwischen entstehen hybride Klanggestalten. Der Vocoder ist eine einfache Form: Spektralhuelle von Klang A wird auf Spektrum von Klang B angewendet. Werkzeuge: SPEAR, AudioSculpt (IRCAM), Max/MSP MSP-Objekte.",
        difficulty = 5,
        funFact = "Jonathan Harvey verwendete Spektrales Morphing in 'Mortuos Plango, Vivos Voco' (1980): Er morphte die Kirchenglocke von Winchester Cathedral mit der Knabenstimme seines Sohnes — die Glocke singt, die Stimme klingt wie Metall. Dieses Werk ist ein Meisterbeispiel fuer die expressiven Moeglichkeiten des Spektralmorphings."
    ),

    // Q48
    Question(
        categoryId = 5,
        questionText = "Was ist der 'Phase Vocoder' und welche zwei unabhaengigen Klangparameter erlaubt er zu manipulieren?",
        answerA = "Der Phase Vocoder trennt Zeitdehnng und Transposition durch Analyse des Phasenspektrums: Er erlaubt Zeitdehnung ohne Tonhoehenaenderung ('time stretching') und Transposition ohne Tempoveraenderung ('pitch shifting') — unmoeglich mit einfacher Geschwindigkeitsaenderung",
        answerB = "Ein System, das Phase und Amplitude eines Signals trennt und nur die Amplitude fuer Synthese verwendet, waehrend die Phase verworfen wird",
        answerC = "Ein analoger Hardware-Prozessor aus den 1970ern, der Phasenmodulation zur Erzeugung komplexer Klangfarben aus einfachen Sinustoenen verwendet",
        answerD = "Der Phase Vocoder ist eine Encoding-Methode fuer digitale Audiokompression, die Phasenkorrelation zwischen Stereokanalen ausnutzt",
        correctAnswer = 0,
        explanation = "Der Phase Vocoder (Flanagan & Golden, Bell Labs 1966; digital: James Moorer 1978) analysiert ein Audiosignal in Kurzzeit-Frames und verfolgt Frequenz und Amplitude jeder Spektralkomponente durch die Zeit. Durch Manipulation der Framerate kann Tempo und Pitch unabhaengig geaendert werden: Weniger Frames pro Sekunde = langsamer ohne tiefer; Skalierung der analysierten Frequenzen = hoeher ohne schneller. Grundlage jeder modernen Pitch-Correction und Time-Stretch-Software.",
        difficulty = 5,
        funFact = "Auto-Tune (Antares, 1997) basiert auf Phase-Vocoder-Prinzipien fuer das Pitch-Shifting. Cher's 'Believe' (1998) war die erste Popaufnahme, bei der Auto-Tune als kreatives Effektmittel (Extremeinstellung) bewusst eingesetzt wurde. Der Erfinder Andy Hildebrand war zuvor Seismologe — er uebertrug Algorithmen zur Oelfeld-Analyse auf Gesangs-Intonationskorrektur."
    ),

    // Q49
    Question(
        categoryId = 5,
        questionText = "Was ist die 'Csound'-Sprache und worin besteht ihr kompositorisches Potenzial fuer Klangsynthese-Algorithmen?",
        answerA = "Csound (Barry Vercoe, MIT 1986, direkte Nachfolge von MUSIC V) ist eine textbasierte Klangsynthese-Sprache mit Orchestra/Score-Trennung: Orchestra definiert Instrumente als Unit-Generator-Netzwerke, Score Noten als zeitlich platzierte Ereignisse — uneingeschraenkte Synthesekontrolle",
        answerB = "Eine grafische Programmierumgebung analog zu Max/MSP, die als Open-Source-Alternative fuer Hochschulen entwickelt wurde",
        answerC = "Csound ist ein Dateiformat (wie WAV oder AIFF) fuer die Speicherung von Algorithmusparametern die dann in beliebigen Synthese-Engines abgespielt werden koennen",
        answerD = "Ein vom IRCAM entwickeltes System fuer Echtzeit-Klangtransformation mit proprietaerer Hardwareanbindung",
        correctAnswer = 0,
        explanation = "Csound (Barry Vercoe, MIT 1986) ist eine direkte Nachfolge von Max Mathews MUSIC V. Die Architektur trennt 'Orchestra' (Instrumentendefinitionen als UGen-Netzwerke) von 'Score' (Partiturereignisse mit Zeit, Dauer, Parametern). Csound bietet hunderte eingebauter Unit Generators (Oszilatoren, Filter, Verzoegerungen, Spatializer, FFT-Operationen) und ist vollstaendig Open Source. Es ist nach wie vor eine Standard-Lehrumgebung an Musikinformatik-Hochschulen.",
        difficulty = 5,
        funFact = "Csound laeuft heute auch als JavaScript im Browser (Csound.js/WebAudio) und auf Smartphones. Was 1986 als Batch-Verarbeitungssystem entstand (eine Stunde Rechenzeit fuer eine Sekunde Klang) laeuft heute in Echtzeit auf einem Mobiltelefon — ein Zeugnis fuer den exponentiellen Anstieg der Rechenleistung in 40 Jahren."
    ),

    // Q50
    Question(
        categoryId = 5,
        questionText = "Was bezeichnet der Begriff 'Spectral Freezing' als Klangsynthese-Technik und was sind typische kompositorische Anwendungen?",
        answerA = "Spectral Freezing analysiert einen Klang per FFT und haelt ein einzelnes Spektralframe unendlich lang anhalten: Die Huellkurve wird gestoppt, das Spektrum eingefroren — erzeugt schwebende, statische Klangtexturen aus dynamischem Material",
        answerB = "Eine Aufnahmetechnik in kalten Umgebungen, die bestimmte Frequenzen durch physikalische Kondensation von Schallwellen verlaengert",
        answerC = "Ein Kompressionsalgorithmus, der stationaere Klanganteile durch einen einzigen Spektral-Snapshot kodiert um Redundanz zu reduzieren",
        answerD = "Die Technik, ein MIDI-Signal auf einem einzigen Akkord zu 'einfrieren' (hold), waehrend der Synthesizer die Klangfarbe kontinuierlich moduliert",
        correctAnswer = 0,
        explanation = "Spectral Freezing nutzt STFT-Analyse: Ein bestimmtes Zeitfenster des Klangs wird als Spektralframe extrahiert und kontinuierlich resynthetisiert — das Resultat ist ein zeitlich unbegrenzt andauernder Klang mit den spektralen Eigenschaften eines kurzen Moments. Kompositorische Anwendungen: Zeitdehnung bis zur Stasis, Extraktion von 'Essenz'-Klangmomenten, Uebergaenge zwischen Klaengen durch Einfrieren und Auftauen, Erzeugung von Textur-Schichten. Werkzeuge: Ableton Live 'Spectral Resonator', Max MSP, Kyma.",
        difficulty = 5,
        funFact = "Spectral Freezing ist eine der meistgenutzten Techniken in zeitgenoessischer elektronischer Musik und Film-Scoring. Ennio Morricone nutzte aehnliche Techniken analog (Magnetband-Loops), was er 'eternal notes' nannte. Die digitale Version ermoeglicht praezisere Kontrolle und wird regelmaessig in Filmmusik fuer surreale Zeitdehnungsmomente verwendet."
    )

)

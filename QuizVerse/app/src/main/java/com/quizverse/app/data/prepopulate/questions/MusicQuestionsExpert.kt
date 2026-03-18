package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun musicQuestionsExpert(): List<Question> = listOf(

    // ── Musikproduktion (10) ─────────────────────────────────────────────────

    Question(
        categoryId = 5,
        questionText = "Welche Abtastrate (Sample Rate) nutzt das professionelle Super-Audio-CD-Format (SACD) bei Einsatz von Direct Stream Digital (DSD)?",
        answerA = "96 kHz",
        answerB = "192 kHz",
        answerC = "2,8224 MHz",
        answerD = "768 kHz",
        correctAnswer = 2,
        explanation = "DSD64 arbeitet mit einer Abtastrate von 2,8224 MHz (64-faches der CD-Rate von 44,1 kHz) bei 1-Bit-Tiefe. Dies unterscheidet sich grundlegend vom PCM-Verfahren der CD und ermoeglicht theoretisch eine sehr hohe Zeitauflosung.",
        difficulty = 4,
        funFact = "Obwohl DSD eine Abtastrate von 2,8 MHz hat, ist der nutzbare Frequenzbereich durch Rauschen oberhalb von 20 kHz begrenzt — die eigentliche Audioqualitaet ist daher umstrittener als die Zahl vermuten laesst."
    ),

    Question(
        categoryId = 5,
        questionText = "Was beschreibt der Begriff 'Headroom' in der professionellen Audioproduktion?",
        answerA = "Die Frequenzbreite des Mischpults",
        answerB = "Der Pegelabstand zwischen dem durchschnittlichen Pegel und dem Clipppunkt",
        answerC = "Die maximale Hallzeit eines Studioraums",
        answerD = "Die Anzahl verfuegbarer Eingangs-Kanaele",
        correctAnswer = 1,
        explanation = "Headroom bezeichnet den Pegelabstand (in dB) zwischen dem typischen Arbeitspegel (z.B. -18 dBFS) und dem maximalen Pegel, bevor Clipping auftritt (0 dBFS im digitalen Bereich). Ausreichend Headroom verhindert ungewollte Verzerrungen bei transienten Signalen.",
        difficulty = 4,
        funFact = "Im analogen Bereich war ein Headroom von 20-24 dB ueber dem Nominalwert ueblich. Im digitalen Audio ist echter Headroom nur durch niedrigere Arbeitspegel zu erreichen, da 0 dBFS eine absolute Grenze darstellt."
    ),

    Question(
        categoryId = 5,
        questionText = "Welche Technik nutzt ein Multiband-Kompressor im Gegensatz zu einem Breitband-Kompressor?",
        answerA = "Er verdoppelt das Signal und vergleicht beide Haelften",
        answerB = "Er teilt das Frequenzspektrum in Baender auf und komprimiert jedes unabhaengig",
        answerC = "Er arbeitet ausschliesslich mit negativen Dezibelwerten",
        answerD = "Er verwendet eine analoge Roehrentechnologie fuer jeden Kanal",
        correctAnswer = 1,
        explanation = "Ein Multiband-Kompressor unterteilt das Eingangssignal mittels Crossover-Filter in mehrere Frequenzbaender (z.B. Bass, Mitten, Hoehen) und wendet auf jedes Band eigene Kompressionsparameter an. So laesst sich z.B. ein dumpfer Bass komprimieren, ohne dass Hoehen beeinflusst werden.",
        difficulty = 4,
        funFact = "Der L316 Multiband-Kompressor von Sony wurde in den 1990ern zum Industriestandard im Mastering. Moderne Plug-ins wie FabFilter Pro-MB erlauben dynamische Baender, die sich nur bei Bedarf aktivieren."
    ),

    Question(
        categoryId = 5,
        questionText = "Was ist 'Parallel Compression' (auch 'New York Compression' genannt)?",
        answerA = "Zwei Kompressoren werden seriell hintereinandergeschaltet",
        answerB = "Das stark komprimierte Signal wird dem unkomprimierten Signal beigemischt",
        answerC = "Ein Stereosignal wird auf Mono reduziert und neu komprimiert",
        answerD = "Ein Sidechain-Signal steuert einen zweiten Kompressor",
        correctAnswer = 1,
        explanation = "Bei Parallel Compression laeuft das Signal auf zwei Wegen: einmal unveraendert und einmal stark komprimiert. Beide Signale werden anschliessend gemischt. Das Ergebnis verbindet die Dynamik des Originals mit der Dichte und Sustain des komprimierten Signals.",
        difficulty = 4,
        funFact = "Der Begriff 'New York Compression' entstand, weil Tontechniker in New Yorker Studios in den 1970ern diese Technik besonders haeufig fuer Schlagzeuggruppen einsetzten, um Punch und Transparenz gleichzeitig zu erzielen."
    ),

    Question(
        categoryId = 5,
        questionText = "Welche Phasenlage beschreibt das sogenannte 'Mid-Side' (M/S) Recording-Verfahren?",
        answerA = "Beide Mikrofone sind gleichphasig auf denselben Punkt gerichtet",
        answerB = "Ein Mittenkanal (Summe) wird mit einem Differenzkanal (Achter) kombiniert",
        answerC = "Zwei Mikrofone sind im 90-Grad-Winkel angeordnet und gleichphasig",
        answerD = "Ein Stereo-Mikrofon nimmt Schall aus entgegengesetzten Richtungen auf",
        correctAnswer = 1,
        explanation = "M/S-Technik kombiniert ein Mittenkanal-Mikrofon (Niere oder Kugel, Richtung Schallquelle) mit einem Acht-Mikrofon quer dazu. Durch Addition und Subtraktion (L = M+S, R = M-S) entsteht ein Stereopaar. Besonders praktisch: Die Stereobreite ist nachtraeglich am Mischpult veraenderbar.",
        difficulty = 4,
        funFact = "Alan Blumlein, der britische Ingenieur und Erfinder, entwickelte die M/S-Methode bereits 1934. Er hielt auch das Patent auf Stereoton und gilt als einer der bedeutendsten Tontechniker der Geschichte."
    ),

    Question(
        categoryId = 5,
        questionText = "Was bezeichnet 'Spectral Repair' in modernen Audio-Editoren wie iZotope RX?",
        answerA = "Eine Methode zur Frequenzerhohung durch harmonische Synthese",
        answerB = "Die automatische Transposierung von Audiosignalen in andere Tonarten",
        answerC = "Die Rekonstruktion beschaedigter oder unterbrechungsbetroffener Audiobereiche anhand des Spektrums",
        answerD = "Ein EQ-Algorithmus zur Anpassung an Raumakustik",
        correctAnswer = 2,
        explanation = "Spectral Repair analysiert das Zeit-Frequenz-Spektrum (Spektrogramm) eines Audiosignals und kann markierte Bereiche anhand der umgebenden Spektralinformation rekonstruieren. Dabei werden Frequenzbaender 'interpoliert' wie ein Bild-Inpainting-Algorithmus.",
        difficulty = 4,
        funFact = "iZotope RX wurde urspruenglich als Restaurierungssoftware fuer historische Aufnahmen entwickelt. Heute wird es im Film-Postproduktions-Bereich weltweit als Standard eingesetzt — z.B. zum Entfernen von Atemgeraeusch oder Telefonklingeln aus Dialogaufnahmen."
    ),

    Question(
        categoryId = 5,
        questionText = "Was ist das Prinzip des 'Fletcher-Munson-Effekts' in der Produktionspraxis?",
        answerA = "Hohe Frequenzen verlieren bei grosser Lautstaerke an Wahrnehmbarkeit",
        answerB = "Die menschliche Lautstaerkewahrnehmung aendert sich frequenzabhaengig je nach Schalldruck",
        answerC = "Tiefe Frequenzen klingen bei niedrigem Pegel lauter als Hoehen",
        answerD = "Die Stereobreite nimmt mit steigendem Pegel zu",
        correctAnswer = 1,
        explanation = "Fletcher und Munson ermittelten 1933, dass das menschliche Gehoer bei niedrigem Pegel Baesse und Hoehen weniger stark wahrnimmt als Mitten. Bei hohem Pegel gleicht sich die Kurve an. Daraus ergeben sich Isophone-Kurven (spaeter als Equal Loudness Contours nach ISO 226 standardisiert).",
        difficulty = 4,
        funFact = "Der 'Loudness'-Schalter an alten HiFi-Verstaerkern basiert direkt auf dem Fletcher-Munson-Effekt: Er boosted bei niedrigem Pegel Baessel und Hoehen, um die natuerliche Unempfindlichkeit des Ohrs auszugleichen."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches Verfahren nutzt ein Vocoder, um synthetische Sprache oder Gesang zu erzeugen?",
        answerA = "Er transponiert ein Rohsignal in Echtzeit durch FFT-basierte Pitchverschiebung",
        answerB = "Er uebertraegt die Huellkurve eines Traegers (Sprache) auf einen Synthesizer-Ton",
        answerC = "Er ersetzt Vokale durch gesampelte Phoneme aus einer Datenbank",
        answerD = "Er moduliert ein Sinussignal mit dem Rauschanteil der Stimme",
        correctAnswer = 1,
        explanation = "Ein Vocoder analysiert das Frequenzspektrum eines Analysesignals (meist Sprache) und extrahiert dessen Filterhuellkurve. Diese Huellkurve wird dann auf ein Traetersignal (meist ein harmonisch reiches Synthesizer-Signal) angewendet. Das Ergebnis klingt wie eine 'sprechende Maschine'.",
        difficulty = 4,
        funFact = "Homer Dudley entwickelte den Vocoder 1939 fuer Bell Labs urspruenglich als Sprachkompressionsverfahren fuer Telegrafie-Uebertragungen. Erst in den 1970ern entdeckten Musiker wie Wendy Carlos und Stevie Wonder das kreative Potenzial des Geraets."
    ),

    Question(
        categoryId = 5,
        questionText = "Was ist 'Bit Depth' (Wortlaenge) in der Digitalaudio-Technik und welche praktische Auswirkung hat eine groessere Wortlaenge?",
        answerA = "Sie bestimmt die maximale Abtastrate und damit den hoerbaren Frequenzbereich",
        answerB = "Sie legt fest, wie viele Amplitudenstufen pro Sample kodierbar sind, was den Dynamikumfang bestimmt",
        answerC = "Sie gibt die Anzahl der verfuegbaren Audioeffekte im Signal an",
        answerD = "Sie beschreibt die Laenge des Audiosignals in Minuten",
        correctAnswer = 1,
        explanation = "Jedes Bit Wortlaenge ergibt theoretisch ca. 6 dB mehr Dynamikumfang. CD-Audio mit 16 Bit hat daher etwa 96 dB Dynamikumfang. 24-Bit-Audio erreicht ca. 144 dB. In der Praxis begrenzt das Rauschen analoger Schaltkreise die nutzbare Aufloesung auf etwa 20-21 Bit.",
        difficulty = 4,
        funFact = "Das menschliche Gehoer hat einen nutzbaren Dynamikumfang von etwa 120 dB (Hoerschwelle bis Schmerzgrenze). 20-Bit-Audio wuerde theoretisch bereits ausreichen — der Sprung von 24 auf 32 Bit ist in der Praxis hoertechnisch bedeutungslos, aber numerisch bequem fuer DSP-Berechnungen."
    ),

    Question(
        categoryId = 5,
        questionText = "Welche Funktion hat ein 'De-Esser' in der Vokalproduktion?",
        answerA = "Er entfernt ungewollte Atemgeraeusche aus Gesangsaufnahmen",
        answerB = "Er reduziert gezielt zischende Sibilanten (S, Z, Sch) im Vokalspektrum",
        answerC = "Er reguliert den Abstand zwischen Stimme und Hintergrundmusik",
        answerD = "Er verstaerkt selektiv Konsonanten, um Verstaendlichkeit zu erhoehen",
        correctAnswer = 1,
        explanation = "Ein De-Esser ist ein frequenzselektiver Kompressor oder Limiter, der im Bereich von etwa 5-10 kHz angreift und dort Pegelspitzen reduziert, die durch Sibilanten (Zischlaute wie S, Z, Sch) entstehen. Diese koennen bei naheher Mikrofonierung besonders intensiv werden.",
        difficulty = 4,
        funFact = "Der erste kommerzielle De-Esser war der dbx 902, der Mitte der 1970er Jahre erschien. Moderne Plugins verwenden oft dynamisches EQing oder spektrale Subtraktionstechniken fuer transparentere Ergebnisse."
    ),

    // ── Akustik und Tontechnik (10) ──────────────────────────────────────────

    Question(
        categoryId = 5,
        questionText = "Was ist der Unterschied zwischen 'Nachhallzeit RT60' und 'Frueher Reflexionszeit EDT' in der Raumakustik?",
        answerA = "RT60 misst den Frequenzgang, EDT den Zeitverlauf des Signals",
        answerB = "RT60 beschreibt den vollstaendigen Pegelabfall um 60 dB, EDT nur die ersten 10 dB extrapoliert auf 60 dB",
        answerC = "EDT misst Nachhall bei Frequenzen unter 250 Hz, RT60 bei allen anderen",
        answerD = "RT60 wird im Freifeld gemessen, EDT im geschlossenen Raum",
        correctAnswer = 1,
        explanation = "RT60 (Reverberation Time) beschreibt die Zeit, bis der Schallpegel nach Abschalten der Quelle um 60 dB gefallen ist. EDT (Early Decay Time) misst den Abfall nur in den ersten 10 dB und extrapoliert diesen auf 60 dB — EDT reagiert empfindlicher auf fruehe Reflexionen und korreliert staerker mit dem subjektiven Eindruck von Raeumen.",
        difficulty = 4,
        funFact = "Der Berliner Philharmoniker-Saal gilt mit einer RT60 von ca. 2,0 Sekunden bei mittleren Frequenzen als akustisch nahezu ideal fuer sinfonische Musik. Opernhaeuser werden dagegen oft mit laengerer Nachhallzeit fuer Gesang optimiert."
    ),

    Question(
        categoryId = 5,
        questionText = "Was beschreibt das Phael 'Kammfiltereffekt' (Comb Filtering) in der Tontechnik?",
        answerA = "Ein Verzerrungseffekt bei Uebersteuerung von Roehrenverstaerkern",
        answerB = "Periodische Einbrueche und Peaks im Frequenzgang durch Ueberlagerung direkt- und reflektiertem Signal",
        answerC = "Die akustische Absorption tiefer Frequenzen durch poroese Materialien",
        answerD = "Ein Fehler im Analog-Digital-Wandler bei hohen Pegeln",
        correctAnswer = 1,
        explanation = "Kammfilterung entsteht, wenn ein Signal mit einer zeitverzoegerten Kopie seiner selbst ueberlagert wird. Die Interferenz erzeugt Ausleschungen bei Frequenzen, bei denen die Verzoegerung einer halben Wellenlaenge entspricht, und Verstaerkungen bei ganzzahligen Wellenlaengen. Im Frequenzgang sieht dies aus wie ein Kamm.",
        difficulty = 4,
        funFact = "Kammfilterung ist der unerwuenschte Effekt beim Abspielen desselben Signals auf zwei nahe beieinander stehenden Lautsprechern. Der Flanging-Effekt in der Musikproduktion nutzt denselben Mechanismus jedoch kreativ durch eine sich aendernde Verzoegerungszeit."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches Kondensator-Mikrofon-Prinzip nutzt eine externe Polarisationsspannung (Bias-Spannung) und welches ist intern aufgeladen?",
        answerA = "Baendchenmikrofon (extern) vs. Elektret-Mikrofon (intern)",
        answerB = "True Condenser (extern) vs. Elektret-Mikrofon (intern)",
        answerC = "Dynamisches Mikrofon (extern) vs. Kondensatormikrofon (intern)",
        answerD = "Grossmembran (intern) vs. Kleinmembran (extern)",
        correctAnswer = 1,
        explanation = "Klassische Kondensatormikrofone (True Condenser) benoetigen eine externe Polarisationsspannung von 48-60 V (z.B. ueber Phantomspeisung). Elektret-Mikrofone haben eine permanent geladene Membran oder Backplate (PTFE-Material) und benoetigen keine externe Hochspannung — nur geringe Versorgung fuer den Vorverstärker.",
        difficulty = 4,
        funFact = "Elektret-Mikrofone wurden 1964 von James West und Gerhard Sessler bei Bell Labs erfunden. Heute sind ueber 90% aller weltweit hergestellten Mikrofone Elektret-Mikrofone — sie stecken in jedem Smartphone, Laptop und Headset."
    ),

    Question(
        categoryId = 5,
        questionText = "Was ist das Nyquist-Shannon-Abtasttheorem und welche konkrete Frequenz-Obergrenze gilt fuer CD-Audio (44,1 kHz)?",
        answerA = "Das Signal muss mindestens dreimal pro Sekunde abgetastet werden; Obergrenze: 14,7 kHz",
        answerB = "Die Abtastrate muss mindestens doppelt so hoch sein wie die hoechste Signalfrequenz; Obergrenze: 22,05 kHz",
        answerC = "Das Signal muss mit seiner eigenen Frequenz abgetastet werden; Obergrenze: 44,1 kHz",
        answerD = "Die Abtastrate bestimmt die minimale Frequenz; Obergrenze: 88,2 kHz",
        correctAnswer = 1,
        explanation = "Das Nyquist-Shannon-Theorem besagt: Zur verzerrungsfreien Rekonstruktion eines Signals muss die Abtastrate mindestens doppelt so gross sein wie die hoechste im Signal enthaltene Frequenz. Bei 44,1 kHz Abtastrate koennen Frequenzen bis maximal 22,05 kHz digital erfasst werden.",
        difficulty = 4,
        funFact = "Das menschliche Gehoer endet bei etwa 20 kHz, daher war 44,1 kHz fuer die CD bewusst so gewaehlt. Der leichte Ueberschuss von 2,05 kHz gibt dem Anti-Aliasing-Filter genug Raum, um Artefakte (Aliasing) zu vermeiden."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches Phaenomen erklaert, warum geschlossene Kopfhoerer bei tiefen Frequenzen oft einen Bassbooost erzeugen?",
        answerA = "Proximityeffekt durch das Mikrofon des Headsets",
        answerB = "Der eingeschlossene Luftraum zwischen Membran und Ohr wirkt als Resonanzvolumen und hebt tiefe Frequenzen an",
        answerC = "Die magnetische Feldstaerke des Antriebs nimmt bei tiefen Frequenzen zu",
        answerD = "Tiefe Frequenzen reflektieren innerhalb des Ohrkanals staerker als hohe",
        correctAnswer = 1,
        explanation = "Bei geschlossenen Kopfhoerern bildet das Ohrpolster mit der Ohrmuschel und der Membran ein geschlossenes Luftvolumen. Dieses verhaeelt sich wie ein akustisches Tiefpasssystem und verstaerkt tiefe Frequenzen durch Resonanz. Das Volumen und die Compliance bestimmen die Resonanzfrequenz.",
        difficulty = 4,
        funFact = "HiFi-Kopfhoerehersteller wie Sennheiser (HD 800) nutzen besonders grosse offene Gehaeuse, um diesen Kammerresonanz-Effekt zu minimieren und einen lineareren Frequenzgang zu erzielen — auf Kosten der Schallisolierung."
    ),

    Question(
        categoryId = 5,
        questionText = "Was ist 'Quantisierungsrauschen' im digitalen Audio und wie wird es in der Praxis reduziert?",
        answerA = "Rauschen durch elektromagnetische Einstrahlung in den ADC; Abschirmung reduziert es",
        answerB = "Rundungsfehler beim Quantisieren analoger Amplitudenwerte auf diskrete Stufen; Dithering reduziert es",
        answerC = "Rauschen durch Abtastjitter im DAC; Oversampling reduziert es",
        answerD = "Hochfrequenzrauschen des Vorverstärkers; ein Tiefpassfilter reduziert es",
        correctAnswer = 1,
        explanation = "Quantisierungsrauschen entsteht, weil ein analoges Signal nicht beliebig fein in diskrete Amplitudenwerte aufgeteilt werden kann. Die Rundungsfehler klingen wie ein Verzerrungsprodukt, das harmonisch mit dem Signal korreliert. Dithering (gezieltes Hinzufuegen von kleinem Rauschen) dekorreliert diesen Effekt und macht ihn weniger hoerbar.",
        difficulty = 4,
        funFact = "Ohne Dithering klingt leises Ausblenden von 24-Bit-Audio auf 16 Bit wie 'Granching' — ein Artefakt, das besonders bei klassischer Musik hoerbar ist. Mit Dithering ist der Uebergang selbst bei professionellen Monitoren nicht mehr zu hoeren."
    ),

    Question(
        categoryId = 5,
        questionText = "Was beschreibt der 'Proximity-Effekt' bei Richtmikrofonen und bei welchen Mikrofon-Charakteristiken tritt er auf?",
        answerA = "Eine Anhebung der Hochfrequenzen bei nahes Besprechen; tritt bei Kugelcharakteristik auf",
        answerB = "Eine Anhebung der Tieffrequenzen bei nahem Besprechen; tritt bei Niere, Superniere und Acht auf, nicht bei Kugel",
        answerC = "Eine Abnahme der Empfindlichkeit bei seitlicher Beschallung; tritt bei allen Charakteristiken auf",
        answerD = "Eine Verstaerkung der Mitten durch Stehwellenbildung; tritt bei Hyperniere auf",
        correctAnswer = 1,
        explanation = "Der Proximity-Effekt ist ein physikalisches Phaenomen bei Druckgradienten-Mikrofonen: Bei sehr nahem Besprechen (unter 30 cm) werden tiefe Frequenzen ueberproportional stark aufgenommen. Kugelcharakteristiken (reine Druckempfaenger) zeigen keinen Proximity-Effekt, da sie kein Gradientenglied haben.",
        difficulty = 4,
        funFact = "Radiomoderatoren nutzen den Proximity-Effekt oft bewusst, um ihrer Stimme mehr Waerme und Tiefe zu verleihen ('Radio-Voice'). Frank Sinatras Tontechniker nutzten ihn bereits in den 1950ern systematisch fuer sein charakteristisches Klangbild."
    ),

    Question(
        categoryId = 5,
        questionText = "Was versteht man unter 'Haas-Effekt' (Praezedenzeffekt) in der Psychoakustik?",
        answerA = "Das Gehoer lokalisiert ein Signal in Richtung der ersten Schallquelle, wenn eine zweite Quelle innerhalb von 30-40 ms folgt",
        answerB = "Tiefe Frequenzen klingen laeuter, wenn ein Echo nach 100 ms folgt",
        answerC = "Hoch frequente Toel werden raeumlich weiter entfernt wahrgenommen als tiefe",
        answerD = "Bei identischen Pegeln zweier Quellen lokalisiert das Gehoer zur Quelle mit dem helleren Klang",
        correctAnswer = 0,
        explanation = "Der Haas-Effekt (1951 von Helmut Haas beschrieben) besagt: Wenn ein erstes Signal einem zweiten identischen Signal um bis zu ca. 30-40 ms vorausgeht, lokalisiert das Gehoer den Schall zur ersten Quelle — selbst wenn die zweite Quelle deutlich lauter ist (bis zu ca. 10 dB). Das Gehoer fusioniert beide Ereignisse zu einem.",
        difficulty = 4,
        funFact = "Der Haas-Effekt wird in der Live-Beschallung gezielt genutzt: Delay-Lautsprecher fuer das Publikum im hinteren Bereich werden so eingestellt, dass ihr Signal einige Millisekunden nach dem Hauptlautsprecher kommt — Zuhoerer hoeren die Stimme aus Richtung der Buehne, obwohl der nahere Lautsprecher lauter ist."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches Phykaenomen tritt bei der sogenannten 'Maskierung' in der Audiokodierung (z.B. MP3) auf?",
        answerA = "Laute Signalanteile machen das Gehoer voruebergehend unempfindlich fuer leise Signalanteile in der Naehe",
        answerB = "Hohe Frequenzen uebertoenen grundsaetzlich tiefe Frequenzen bei gleichem Pegel",
        answerC = "Schnelle Transienten verursachen Verzerrungen, die durch Filterung beseitigt werden muessen",
        answerD = "Stereo-Differenzsignale werden automatisch lautsstaerker wahrgenommen als Monosignale",
        correctAnswer = 0,
        explanation = "Psychoakustische Maskierung beschreibt, dass das Gehoer leise Tele in der zeitlichen und frequenziellen Naehe starker Signale nicht wahrnehmen kann. MP3 und AAC nutzen dieses Phaenomen: In Frequenzbereichen, die von lauten Signalen maskiert werden, werden die leisen Signalanteile einfach weggelassen, ohne dass dies hoerbar ist.",
        difficulty = 4,
        funFact = "Das MP3-Format (MPEG-1 Audio Layer III) wurde 1987 am Fraunhofer-Institut in Erlangen entwickelt. Der Algorithmus reduziert die Datenmenge um bis zu 90% gegenueber unkomprimiertem Audio, indem er nur das kodiert, was das Gehoer tatsaechlich wahrnehmen kann."
    ),

    Question(
        categoryId = 5,
        questionText = "Was ist der Unterschied zwischen 'True Peak' und 'Sample Peak' in der Lautheitsmessung nach EBU R128?",
        answerA = "Sample Peak misst Spitzen zwischen Abtastpunkten durch Oversampling-Interpolation; True Peak misst tatsaechliche digitale Maximalwerte",
        answerB = "True Peak berechnet den Maximalwert durch Interpolation zwischen Abtastpunkten; Sample Peak zeigt nur den hoechsten digital gespeicherten Wert",
        answerC = "True Peak ist der RMS-Wert, Sample Peak der momentane Maximalwert",
        answerD = "Sample Peak bezieht sich auf 44,1-kHz-Signale, True Peak auf 96-kHz-Signale",
        correctAnswer = 1,
        explanation = "Im digitalen Signal koennen Spitzen zwischen zwei Abtastpunkten auftreten, die hoher als beide Abtastwerte sind. True Peak berechnet durch Oversampling (mindestens 4x) diese Inter-Sample-Peaks. EBU R128 empfiehlt einen True-Peak-Maximalwert von -1 dBTP, um Clipping bei der D/A-Wandlung zu vermeiden.",
        difficulty = 4,
        funFact = "Das EBU-R-128-Lautheitsstandard aus dem Jahr 2010 revolutionierte die Fernsehbranche: Fruher schrien Werbeunterbrechungen dem Zuhoerer entgegen, weil keine Lautheitsregulierung existierte. Heute muss jeder Sender in Europa nach LUFS normiert ausstrahlen."
    ),

    // ── Musiktheorie fortgeschritten (10) ────────────────────────────────────

    Question(
        categoryId = 5,
        questionText = "Was ist die 'Tristan-Quinte' (Tristan-Akkord) und in welchem Werk tritt er auf?",
        answerA = "Ein geminderter Dreiklang in h-Moll aus Bachs Matthaeus-Passion",
        answerB = "Ein halbverminderter Septakkord mit erhoehter None aus Wagners 'Tristan und Isolde' (1865)",
        answerC = "Ein uebermaeassiger Dreiklang in B-Dur aus Liszts 'Faust-Symphonie'",
        answerD = "Ein Dominantseptakkord ohne Terz aus Schuberts Winterreise",
        correctAnswer = 1,
        explanation = "Der Tristan-Akkord (f-h-dis-gis) eroeffnet Richard Wagners Oper 'Tristan und Isolde' von 1865. Er ist theoretisch ein halbverminderter Septakkord, seine tonale Aufloesungs-Uneindeutigkeit gilt als Ausgangspunkt der Chromatisierung und schliesslich der Atonalitaet in der europaeischen Kunstmusik.",
        difficulty = 4,
        funFact = "Der Tristan-Akkord loest sich direkt in einen Dominantseptakkord auf — aber dieser findet wiederum keine Auflosung. Diese endlose harmonische Spannung ist das kompositorische Symbol fuer die unerfuellte Liebe der Oper: Wagner nannte es 'das Sehnen nach Auflosung, das nie erfuellt wird'."
    ),

    Question(
        categoryId = 5,
        questionText = "Was beschreibt die 'Reihentechnik' (Zwoelftonmethode) von Arnold Schoenberg?",
        answerA = "Alle 12 Halbtoen werden in einer festgelegten Reihe geordnet, die als melodisches und harmonisches Material dient",
        answerB = "Jedem der 12 Halbtoen wird ein fixer dynamischer Wert zugewiesen",
        answerC = "Die Komposition basiert auf einer 12-taktigen Grundstruktur, die permutiert wird",
        answerD = "Das Stueck wird in allen 12 Dur-Tonarten gleichzeitig gespielt",
        correctAnswer = 0,
        explanation = "Schoenbergs Zwoelftontechnik (ab 1921) ordnet alle 12 Halbtoen der chromatischen Skala zu einer 'Reihe', in der jeder Ton genau einmal erscheint. Diese Reihe kann als Original, Umkehrung, Krebs (rueckwaerts) und Krebsumkehrung verwendet werden und dient als gesamte Grundlage fuer das Stueck.",
        difficulty = 4,
        funFact = "Schoenbergs Schueler Alban Berg und Anton Webern entwickelten die Zwoelftontechnik auf sehr unterschiedliche Weisen weiter. Webern zerlegte die Reihe in symmetrische Mini-Strukturen, Bergs Musik klingt trotz Reihentechnik oft tonal und expressiv — wie in seinem Violinkonzert (1935)."
    ),

    Question(
        categoryId = 5,
        questionText = "Wie viele Kirchentonsarten (Modi) sind im mittelalterlichen Oktoechos-System enthalten und wie heissen die authentischen Kirchentonarten?",
        answerA = "6 Modi: Ionisch, Dorisch, Phrygisch, Lydisch, Mixolydisch, Aeolisch",
        answerB = "8 Modi; 4 authentische: Dorisch, Phrygisch, Lydisch, Mixolydisch",
        answerC = "4 Modi: Dorisch, Phrygisch, Lydisch, Mixolydisch (ohne Plagalformen)",
        answerD = "12 Modi nach Glarean; 6 authentische inklusive Ionisch und Aeolisch",
        correctAnswer = 1,
        explanation = "Das Oktoechos (achttoeniges System) umfasst 8 Kirchentonsarten: 4 authentische (Dorisch auf D, Phrygisch auf E, Lydisch auf F, Mixolydisch auf G) und 4 plagale Ableitungen (Hypodorisch, Hypophrygisch, Hypolydisch, Hypomixolydisch), die die Finalis behalten, aber eine Quarte tiefer beginnen.",
        difficulty = 4,
        funFact = "Heinrich Glarean fuehrte 1547 in seinem Werk 'Dodekachordon' Ionisch (entspricht modernem Dur) und Aeolisch (entspricht natuerlichem Moll) als zwei weitere authentische Modi ein — womit er das heutige System von 12 Kirchentonarten vervollstaendigte, das in der Jazztheorie bis heute verwendet wird."
    ),

    Question(
        categoryId = 5,
        questionText = "Was ist ein 'augmented sixth chord' (uebermaeassiger Sextakkord) und welche drei klassischen Varianten gibt es?",
        answerA = "Ein Akkord mit erhoehter Sexte; Varianten: Neapolitaner, Neapolitan Second, Lydischer Akkord",
        answerB = "Ein Akkord mit erhoehter Sexte ueber der Subdominante; Varianten: Italienisch, Franzoesisch, Deutsch",
        answerC = "Ein Dominantseptakkord mit erhoehter Quinte; Varianten: Dorisch, Lydisch, Mixolydisch",
        answerD = "Ein Septakkord mit erniedrigter Sexte; Varianten: Englisch, Spanisch, Russisch",
        correctAnswer = 1,
        explanation = "Der uebermaeassige Sextakkord enthaelt als Charakteristikum eine uebermaeassige Sexte (zwischen erniedrigter 6. und erhoehter 4. Stufe). Alle drei Varianten loesen sich in die Dominante auf. Ital.: nur die ueberm. Sexte + Terz; Frz.: zusaetzlich die uebermaeassige Quarte; Dt.: stattdessen reine Quinte (identisch mit Dominantseptakkord in enharmonischer Schreibung).",
        difficulty = 4,
        funFact = "Der Deutsche Sextakkord klingt enharmonisch identisch mit einem Dominantseptakkord und kann damit als 'Pivot' fuer ueberraschende Modulationen genutzt werden. Beethoven und Schubert setzten diese enharmonische Verwechslung gezielt ein."
    ),

    Question(
        categoryId = 5,
        questionText = "Was beschreibt 'Spektrale Musik' (Spectral Music) als Kompositionsmethode?",
        answerA = "Musik, die ausschliesslich auf weissen Rauschen und gefilterten Frequenzbaendern basiert",
        answerB = "Eine Kompositionstechnik, bei der die akustische Spektralanalyse von Klaengen als harmonisches und strukturelles Material dient",
        answerC = "Musik, die in mehreren Tonarten gleichzeitig komponiert ist",
        answerD = "Eine algorithmische Kompositionsmethode basierend auf Fourier-Reihen",
        correctAnswer = 1,
        explanation = "Die Spektralkomposition (ab ca. 1970, franzoesische IRCAM-Schule um Gerard Grisey und Tristan Murail) analysiert den Klang eines Instruments mit Fast Fourier Transformation und leitet aus den Oberttoenen harmonische und rhythmische Strukturen ab. Ein E auf dem Horn erzeugt z.B. eine spezifische Obertonreihe, die zur harmonischen Sprache wird.",
        difficulty = 4,
        funFact = "Gerard Griseys 'Partiels' (1975) fuer 18 Musiker gilt als das Gruendungswerk der Spektralmusik. Das Werk basiert vollstaendig auf dem Klangspektrum eines tiefen E auf der Posaune. Grisey sagte: 'Wir sind Musiker und unser Modell ist der Klang, nicht die Literatur.'"
    ),

    Question(
        categoryId = 5,
        questionText = "Was ist eine 'Soggetto cavato' und in welchem historischen Kontext wird diese Technik verwendet?",
        answerA = "Eine Variationstechnik der Barockmusik, bei der ein Thema schrittweise verkuerzt wird",
        answerB = "Eine Renaissance-Technik, bei der aus Vokalsilben eines Textes oder Namens Noten abgeleitet werden",
        answerC = "Ein polyphoner Kontrapunktstil des 15. Jahrhunderts mit zwei Oberstimmen",
        answerD = "Eine Modulationstechnik in der Klassik, bei der Themen enharmonisch umdeutiert werden",
        correctAnswer = 1,
        explanation = "Soggetto cavato (ital.: 'herausgemeisseltes Thema') ist eine Renaissance-Technik, bei der aus den Vokalen eines Textes oder Namens Solmisationssilben (ut, re, mi, fa, sol, la) abgeleitet werden. Josquin des Prez komponierte so seine Messe 'Hercules Dux Ferrariae' (ca. 1503), indem er den Namen des Herzogs in eine Melodie uebersetzte.",
        difficulty = 4,
        funFact = "Diese Technik ist ein fruehes Beispiel von 'musikalischer Kryptographie'. Im Barock codierte Johann Sebastian Bach seinen Namen B-A-C-H als musikalisches Motiv (H-A-C-H in dt. Notation) und verwendete es in der Kunst der Fuge und anderen spaeten Werken."
    ),

    Question(
        categoryId = 5,
        questionText = "Was unterscheidet 'isorhythmische Motette' von gewoehnlichen Motetten des Mittelalters?",
        answerA = "Die isorhythmische Motette verwendet immer genau drei Stimmen mit festem Stimmkreuzungsverbot",
        answerB = "Sie basiert auf einem Tenor mit fester, zyklisch wiederholter Rhythmusstruktur (Talea) unabhaengig von der Melodiewiederholung (Color)",
        answerC = "Sie ist ausschliesslich fuer liturgischen Gebrauch bestimmt und wird ohne Harmonie gesungen",
        answerD = "Sie verwendet stets denselben Text in allen Stimmen, der isometrisch gesetzt wird",
        correctAnswer = 1,
        explanation = "Die isorhythmische Motette (14.-15. Jh., v.a. Guillaume de Machaut und Philippe de Vitry) hat im Tenor eine periodisch wiederholte Rhythmusstruktur (Talea) und eine separat wiederholte Melodiestruktur (Color). Talea und Color laufen unabhaengig und erzeugen so komplexe polyphone Texturen durch ihre unterschiedliche Laenge.",
        difficulty = 4,
        funFact = "Philippe de Vitry nannte seine neue Technik 'Ars Nova' (neue Kunst) — das gleichnamige Traktat von 1322 beschreibt das modernere rhythmische System, das mit der 'Ars Antiqua' Leonins und Perotins brach und komplexere Rhythmen ermoeglichte."
    ),

    Question(
        categoryId = 5,
        questionText = "Was beschreibt 'serielle Musik' (Integrale Serialismus) in der Kompositionspraxis nach 1945?",
        answerA = "Die Anwendung der Zwoelftontechnik ausschliesslich auf Melodielinien",
        answerB = "Die Ausdehnung der Reihentechnik auf alle musikalischen Parameter: Toene, Rhythmik, Dynamik und Artikulation",
        answerC = "Eine Kompositionstechnik, bei der alle Parameter zufaellig bestimmt werden",
        answerD = "Die serielle Reihenfolge von Modulationen durch alle 12 Tonarten",
        correctAnswer = 1,
        explanation = "Integraler oder totaler Serialismus (Boulez, Stockhausen, Nono, ca. 1950-1960) wendet das Prinzip der geordneten Reihe nicht nur auf Toene an, sondern auf alle Parameter: Tonhoehe, Tondauer, Lautstaerke, Klangfarbe und Artikulation haben je eigene Reihen. Das Ergebnis ist eine hochkomplexe, scheinbar zufaellige aber streng organisierte Klangsprache.",
        difficulty = 4,
        funFact = "Pierre Boulezs 'Structures Ia' (1952) fuer zwei Klaviere gilt als extremstes Beispiel des integralen Serialismus. Boulez selbst distanzierte sich spaeter von dieser Phase: 'Wir wollten alles kontrollieren — und das Ergebnis klang zunehmend wie Zufall.'"
    ),

    Question(
        categoryId = 5,
        questionText = "Welche harmonische Funktion hat der 'Neapolitanische Sextakkord' und auf welcher Stufe baut er auf?",
        answerA = "Er ist ein Subdominantakkord mit erhoehter Quinte auf der III. Stufe",
        answerB = "Er ist ein erniedrigter Grossterzdreiklang auf der II. Stufe in erster Umkehrung (Sextakkord) mit subdominantischer Funktion",
        answerC = "Er ist der Dominantseptakkord auf der V. Stufe mit erhoehter Quinte in Grundlage",
        answerD = "Er ist ein verminderter Vierklang auf der VII. Stufe in zweiter Umkehrung",
        correctAnswer = 1,
        explanation = "Der Neapolitaner (N6) ist ein Dur-Dreiklang auf der erniedrigten II. Stufe (bII) in erster Umkehrung (Sextakkord). In c-Moll waere er Des-F-As in der Lage F-As-Des. Er hat subdominantische Funktion und loest sich typischerweise ueber die Dominante zur Tonika auf. Sein Klang ist besonders dunkel und expressiv.",
        difficulty = 4,
        funFact = "Der Begriff 'Neapolitanisch' ist historisch ungenau — der Akkord wurde weltweit verwendet, nicht nur in Neapel. Die Bezeichnung stammt vermutlich daher, dass Opernkomponisten der Neapolitanischen Schule (A. Scarlatti, Pergolesi) ihn besonders haeufig einsetzten."
    ),

    // ── Instrumentenbau (8) ──────────────────────────────────────────────────

    Question(
        categoryId = 5,
        questionText = "Welches Holz gilt fuer den Geigenbau als traditionelles Idealmaterial fuer Boden und Zargen und welches fuer die Decke?",
        answerA = "Eiche (Boden) und Buche (Decke)",
        answerB = "Ahorn (Boden und Zargen) und Fichte (Decke)",
        answerC = "Kirsche (Boden) und Eiche (Decke)",
        answerD = "Pappel (Boden) und Zeder (Decke)",
        correctAnswer = 1,
        explanation = "Seit Jahrhunderten gilt Ahorn (Acer pseudoplatanus) fuer Boden und Zargen der Geige als Standardmaterial — besonders geflamter Ahorn aus Bosnien oder den Alpen. Fuer die schwingende Decke wird Fichte (Picea abies) verwendet, die ein sehr guenstiges Verhaeltnis von Steifigkeit zu Dichte hat und damit optimale Klangeigenschaften bietet.",
        difficulty = 4,
        funFact = "Antonio Stradivaris Geigen wurden in einer 'Kleinen Eiszeit' zwischen 1645 und 1715 gefertigt, als kalte Sommer besonders langsam gewachsenes, dichteres Holz erzeugten. Wissenschaftler sehen darin einen moeglichen Grund fuer den unuebertroffen klanglichen Ruf der Stradivari."
    ),

    Question(
        categoryId = 5,
        questionText = "Was ist die Funktion der 'Basslake' im Inneren einer Streichinstrument-Zarge?",
        answerA = "Sie verbindet Decke und Boden und uebertraegt Kraeft vom Steg auf den Boden",
        answerB = "Sie ist eine Holzleiste, die unter dem Basssteg verlaeuft und die Decke gegen den Zug der tiefen Saite verstaerkt",
        answerC = "Sie verstaerkt die Zargen im Bereich des Halsansatzes",
        answerD = "Sie ist die Aussparung fuer das Kinnhalter-System",
        correctAnswer = 1,
        explanation = "Die Bassbalken ist ein Holzleiste (ca. 270 mm lang, 5 mm breit), die im Inneren der Decke laengst unterhalb des linken Steigfusses verklebt ist. Sie verteilt den Zug und die Schwingungen der Basssaiten ueber die Decke und verleiht der Decke zusaetzliche Steifigkeit gegen den Saitenzug.",
        difficulty = 4,
        funFact = "Der Stimmstock (Seele) im Inneren der Geige steht senkrecht zwischen Decke und Boden unter dem rechten Steigfuss — er uebertraegt Hochfrequenzschwingungen direkt auf den Boden und ist entscheidend fuer den Klangtransfer. Geigenbauer sprechen davon, dass der Stimmstock 'die Seele des Instruments' sei."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches Prinzip unterscheidet eine 'Doppelrohrblatt'-Klarinette nicht von Oboe und Fagott?",
        answerA = "Die Klarinette hat ein Einfachrohrblatt; Oboe und Fagott haben Doppelrohrblatter",
        answerB = "Die Klarinette ist ein offenes Rohr; Oboe und Fagott sind geschlossene Rohre",
        answerC = "Die Klarinette ueberblaesst zur Duodezime; Oboe und Fagott ueberblaessen zur Oktave",
        answerD = "Die Klarinette hat keinen Klappenmechanismus; Oboe und Fagott haben das Boehm-System",
        correctAnswer = 0,
        explanation = "Die Klarinette verwendet ein einzelnes Rohrblatt (Einfachrohrblatt), das gegen den Mundstuesteg schwingt. Oboe und Fagott haben zwei Rohrblatter (Doppelrohrblatt), die gegeneinander schwingen. Dieser Unterschied erklaert auch das andere Overblaseverhalten: Die Klarinette als zylindrisches Rohr ueberblaest zur Duodezime (12 Halbtoel), Oboe und Fagott als Kegelrohr zur Oktave.",
        difficulty = 4,
        funFact = "Die Boehm-Klappenentwicklung von Theobald Boehm (1794-1881) revolutionierte urspruenglich die Floete (1832) und wurde dann auf Klarinette uebertragen. Das Boehm-System ermoeglicht erstmals eine vollkommen grifftechnisch logische Tastatur, die alle Tonarten ohne extreme Griff-Akrobatik spielbar macht."
    ),

    Question(
        categoryId = 5,
        questionText = "Was ist das akustische Wirkprinzip eines 'Helmholtz-Resonators' und wie wird er im Instrumentenbau eingesetzt?",
        answerA = "Er verstaerkt Hohefrequenzen durch Stehwellenbildung in einem Rohr bestimmter Laenge",
        answerB = "Ein Hohlraumresonator mit einer Oeffnung, der bei bestimmter Frequenz durch Massentraegheit der Luftsaeule resoniert; wird z.B. in Gitarren und Lauten eingesetzt",
        answerC = "Er absorbiert tiefe Frequenzen durch destruktive Interferenz und wird in Konzertsaelen verbaut",
        answerD = "Er erzeugt Oberttoene durch nichtlineare Schwingung und wird in Blechblasinstrumenten genutzt",
        correctAnswer = 1,
        explanation = "Ein Helmholtz-Resonator besteht aus einem Luftvolumen (z.B. Gitarrenkorpus) mit einer Oeffnung (Schalloch). Die Luftmasse in der Oeffnung wirkt als Feder-Masse-System und resoniert bei einer spezifischen Frequenz (Helmholtz-Frequenz). Bei Gitarren erzeugt dieser Effekt den charakteristischen 'Korpus-Resonanz'-Basspeak.",
        difficulty = 4,
        funFact = "Hermann von Helmholtz entwickelte diese Resonatoren im 19. Jahrhundert als Analysewerkezeug fuer einzelne Frequenzen — ohne Frequenzanalysator oder Computer. Er konnte so die Zusammensetzung von Klang-spektren untersuchen, indem er den Resonator ans Ohr hielt."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches Material wird traditionell fuer die Herstellung von Cellosaiten in der Historischen Auffuehrungspraxis (HIP) verwendet?",
        answerA = "Edelstahl mit Kunststoffumwicklung",
        answerB = "Getrocknete Schafssermdaerme (Darmsaiten), ggf. mit Metallspinnung",
        answerC = "Synthetisches Nylon mit Silberspinnung",
        answerD = "Kohlefasergeflecht mit Aluminiummantel",
        correctAnswer = 1,
        explanation = "Historische Darm-Saiten bestehen aus gereinigtem und getrocknetem Schafsdarm (Lamm-Daerme sind bevorzugt). Tiefe Saiten werden mit Metalldrahr (Silber, Kupfer oder Aluminium) umsponnen, um Masse ohne zu grosses Volumen zu erzielen. Im Vergleich zu Stahlsaiten klingen Darmsaiten warmer und oberttonreicher, sind aber klimaempfindlicher.",
        difficulty = 4,
        funFact = "Der Begriff 'Catgut' fuer Darmsaiten ist irreführend — Katzendarm wurde nie verwendet. Das Wort leitet sich wahrscheinlich von 'Kitgut' (Geigendarm) oder vom englischen 'cattle' (Vieh) ab. Bis ins 20. Jahrhundert wurden Darmsaiten auch fuer Chirurgie und Tennisschlaeger verwendet."
    ),

    Question(
        categoryId = 5,
        questionText = "Was ist die Aufgabe der 'Mensurlehre' im Geigen- und Cembalobau?",
        answerA = "Sie legt die idealen Proportionsverhaeltnisse von Mensur, Mensurlaenge und Klangraum mathematisch fest",
        answerB = "Sie beschreibt die Lehre der optimalen Mensurverhaeltnisse: Die Beziehung zwischen schwindender Saitenlaenge und dem resultierenden Frequenzverhaeltnis",
        answerC = "Sie definiert die Toleranzen der Holzdicken bei der Decken- und Bodenwoelbung",
        answerD = "Sie gibt die chemische Zusammensetzung historischer Lacke und Beizen vor",
        correctAnswer = 1,
        explanation = "Die Mensur bezeichnet im Streichinstrumentenbau die schwingende Saitenlaenge zwischen Sattel und Steg. In der Mensur-Lehre werden die Proportionen festgelegt: Bei der Halbteilung der Mensur (Quinte der Geige: Oktave) ergibt sich exakt die doppelte Frequenz (Oktavregel). Die spezifische Mensur beeinflusst massgeblich Ansprache, Klangfarbe und Spielbarkeit.",
        difficulty = 4,
        funFact = "Die Stradivari-Geige hat eine Mensur von 328 mm — Antonio Stradivari weicht damit bewusst von der vorherigen Amati-Tradition (ca. 322 mm) ab. Auch dieser millimetergrosse Unterschied wird als Faktor fuer den charakteristischen Stradivari-Klang diskutiert."
    ),

    Question(
        categoryId = 5,
        questionText = "Was ist das 'Quetschendes Rohr'-Prinzip bei Blechblasinstrumenten und wie beeinflusst es die Klangerzeugung?",
        answerA = "Die Lippen des Blaesers vibrieren und regen als Doppelrohrblatt die Luftsaule im Instrument an",
        answerB = "Ein Metallrohrblatt vibriert im Mundsteck und moduliert den Luftstrom",
        answerC = "Die Lippen schliessen und oeffnen sich, erzeugen periodische Druckpulse, die durch Resonanzfrequenz des Rohres selektiert werden",
        answerD = "Luft wird durch ein gezacktes Metallgitter gepresst und dadurch in Schwingung versetzt",
        correctAnswer = 2,
        explanation = "Bei Blechblasinstrumenten pressen die Lippen des Spielers gegeneinander und lassen sich durch den Luftdruck periodisch oeffnen und schliessen. Diese Impulse erzeugen ein breites Spektrum von Oberttoenen. Das Rohr des Instruments selektiert durch seine Eigenresonanz bestimmte Frequenzen und ermoeglicht so gezieltes Spielen einzelner Toene (Naturttoene).",
        difficulty = 4,
        funFact = "Die physikalische Modellierung von Blechblasinstrumenten ist extrem komplex, weil die Lippen des Spielers nicht-linear mit dem Luftdruck im Instrument interagieren — das ist ein nichtlineares Rueckkopplungssystem. Physiker und Akustiker erforschen dieses System bis heute."
    ),

    Question(
        categoryId = 5,
        questionText = "Welches Prinzip liegt der Stimmgabeltemperierung im Orgelbau zugrunde, wenn eine 'gleichschwebende Temperierung' hergestellt wird?",
        answerA = "Jede reine Quinte wird um 1/12 eines Pythagoraeischen Kommas verkleinert",
        answerB = "Jede grosse Terz wird auf ihr reines Verhaeltnis 5:4 gestimmt, alle Quinten bleiben rein",
        answerC = "Alle Oktaven werden um 2 Cent erweitet, alle Quinten entsprechend angepasst",
        answerD = "Die Grundtoene werden nach der Fibonacci-Folge abgestimmt",
        correctAnswer = 0,
        explanation = "Gleichschwebende Temperierung verteilt das Pythagoraeische Komma (ca. 23,46 Cent), das bei 12 reinen Quinten in einem Kreis aufhaeauft, gleichmaessig auf alle 12 Quinten. Jede Quinte wird um 1/12 Komma = ca. 1,955 Cent verkleinert. Dadurch sind alle Tonarten gleich 'verstimmt' und alle Quinten gleich schwebend.",
        difficulty = 4,
        funFact = "Johann Sebastian Bach komponierte das 'Wohltemperierte Klavier' (1722/1742) als Demonstration, dass ein temperiertes Instrument in allen 24 Tonarten spielbar ist — aber er meinte wahrscheinlich 'wohltemperiert' (eine der vielen historischen Temperaturen), nicht notwendigerweise die gleichschwebende Stimmung."
    ),

    // ── Musikwissenschaft (7) ────────────────────────────────────────────────

    Question(
        categoryId = 5,
        questionText = "Was ist die 'Affektenlehre' im Barock und wie beeinflusste sie die Kompositionspraxis?",
        answerA = "Eine Lehre, nach der Musik bestimmte Gesten imitieren soll, um Koerperreaktionen beim Hoerer auszuloesen",
        answerB = "Ein rhetorisches Konzept, nach dem Musik spezifische Affekte (Gemuetsbewegungen) repraesentieren und ausloesen soll, wobei ein Stueck typischerweise einen Affekt durchhaelt",
        answerC = "Eine Spieltechnik fuer Cembalo-Musiker, bei der die dynamische Variation durch Artikulation ersetzt wird",
        answerD = "Eine Systematics der Tonsymbolik, nach der jede Note einer Planetensphare entspricht",
        correctAnswer = 1,
        explanation = "Die Affektenlehre (ital. teoria degli affetti) war ein zentrales aesthetisches Konzept des Barock, das auf der antiken Rhetorik basiert. Musik solle einen bestimmten Affekt (z.B. Trauer, Freude, Zorn) kohaerent durch das gesamte Stueck ausdrucken. Komponisten verwendeten dafuer spezifische Intervalle, Rhythmen und Figuren als konventionelle Ausdruckszeichen.",
        difficulty = 4,
        funFact = "Johann Mattheson beschrieb in seiner 'Vollkommenen Capellmeister' (1739) detailliert, welche musikalischen Mittel welche Affekte ausloesen: Chromatik fuer Schmerz, punktierte Rhythmen fuer Freude und Majestaat, absteigende Linien fuer Trauer. Diese Systematik zeigt, wie systematisch das Barock-Denken ueber musikalische Bedeutung war."
    ),

    Question(
        categoryId = 5,
        questionText = "Was versteht die Musikwissenschaft unter dem Begriff 'musikalische Topik' (topic theory)?",
        answerA = "Die Systematik der Oberstimmen-Fuehrung in mehrstimmiger Vokalmusik des 15. Jahrhunderts",
        answerB = "Wiederkehrende musikalische Typen oder 'Typen' mit konventionell assoziiertem Ausdrucksgehalt, die Hoerer erkennen und deuten koennen",
        answerC = "Die Analyse von Tonart-Hierarchien in der Funktionstheorie",
        answerD = "Die Lehre der melodischen Floskel-Systematik in gregorianischem Choral",
        correctAnswer = 1,
        explanation = "Topic Theory (Leonard Ratner 1980, Kofi Agawu) beschreibt musikalische 'Topoi' — konventionelle Ausdruckstypen wie Jagdmusik, Militaermarsch, Menuett, Sturm-und-Drang-Stil, Volksmusikton. In der Klassik sind Topoii als allgemein verstaendliche Zeichensysteme eingesetzt, die Hoerern Bedeutung und Kontext signalisieren.",
        difficulty = 4,
        funFact = "Mozarts Klaviersonate in A-Dur KV 331 (mit dem Rondo alla turca) wechselt im dritten Satz zum 'Alla turca'-Topos — turkische Janitscharenmusik war im Wien des 18. Jahrhunderts hoechst modisch und wurde als exotisches, energiegeladenes Zeichen eingesetzt."
    ),

    Question(
        categoryId = 5,
        questionText = "Was beschreibt die 'Schenkerschen Analyse' als musiktheoretische Methode?",
        answerA = "Eine statistische Analyse der Haeufigkeit von Akkorden in einem Repertoire",
        answerB = "Eine hierarchische Reduktionsmethode, die tonale Musik auf grundlegende Stimmfuehrungs-Urlinien und den Ur-Satz reduziert",
        answerC = "Ein System zur Klassifikation von Kadenzen in der tonalen Musik",
        answerD = "Eine Methode zur Analyse der Instrumentierung und Orchesterbesetzung",
        correctAnswer = 1,
        explanation = "Heinrich Schenkers Analyse (Freie Satz, 1935) versteht tonale Musik als mehrschichtiges System: An der Oberflaeche steht der Vordergrund (tatsaechliche Noten), darunter der Mittelgrund (harmonisch-melodische Zwischenstruktur) und zuletzt der Hintergrund — die 'Ursatz' aus Urliniesang (3-2-1 oder 5-4-3-2-1 in der Oberstimme) ueber einem I-V-I-Bassbrechung.",
        difficulty = 4,
        funFact = "Schenkers Methode ist in der angloamerikanischen Musiktheorie sehr einflussreich, in Deutschland weniger. Kritiker werfen ihr vor, sie privilegiere einen sehr spezifischen Kanon (deutsch-oesterreichische Klassik/Romantik) und sei kaum auf andere Kulturen anwendbar."
    ),

    Question(
        categoryId = 5,
        questionText = "Was ist der 'Fallobord'-Mechanismus im historischen Hammerfluegel (Fortepiano) und wie unterscheidet er sich vom modernen Fluegel?",
        answerA = "Eine Daempfungsvorrichtung, die bei Tastenanschlag den Daempfer anhebt und nicht die Saite abschlaegt",
        answerB = "Ein Lederhammer-Mechanismus, bei dem der Hammer nach dem Anschlag durch Schwerkraft faellt (Prellmechanik) — im Gegensatz zur doppelten Ausloesemechanik moderner Fluegel",
        answerC = "Ein Kniehebel-System zur Variation der Klangfaerbe durch Schiebung der Klaviatur",
        answerD = "Eine Stimmungsvorrichtung mit verschiebbaren Wirbelstiften im oberen Resonanzboden",
        correctAnswer = 1,
        explanation = "Die historische Wiener Prellmechanik (Stein-Mechanik, ca. 1770) laeest den Hammer nach dem Anschlag durch eigene Schwerkraft zurueckfallen — ohne die doppelte Auslosung (Repetitionsmechanik) moderner Bruggmueller-Steinway-Mechaniken. Dadurch war schnelle Repetition auf demselben Ton schwieriger, aber der Anschlag war direkter und feinfuehliger.",
        difficulty = 4,
        funFact = "Mozart lobte ausdruecklich die Hammerfluegel von Andreas Stein und verfasste 1777 einen Brief an seinen Vater, in dem er Steins Instrumente als den besten Fluegeln, die er je gespielt habe, beschreibt. Die Prellmechanik ermoeglichte genau die differenzierte Anschlag-Kontrolle, die Mozarts Klavierstil verlangt."
    ),

    Question(
        categoryId = 5,
        questionText = "Welche Konsequenz hat das Prinzip der 'Modalitaet' fuer das Verstaendnis gregorianischer Melodien?",
        answerA = "Gregorianische Choraele sind immer in Moll-Tonart und verwenden ausschliesslich die natuerliche Mollskala",
        answerB = "Melodien werden nicht nach Dur/Moll unterschieden, sondern nach ihrer Finalis, Ambitus und charakteristischen Wendungen im modalen System",
        answerC = "Alle Noten eines Chorals haben gleiche Dauer ohne rhythmische Differenzierung",
        answerD = "Gregorianische Melodien sind immer einstimmig und werden ohne Begleitung nach festen Metren gesungen",
        correctAnswer = 1,
        explanation = "Das gregorianische Modalsystem kennt kein Dur/Moll, sondern definiert jeden Modus durch Finalis (Schlusstonl), Tenor (Rezitationston) und Ambitus (Umfang). Ein dorischer Modus auf D unterscheidet sich fundamental von einem phrygischen Modus auf E, obwohl beide dieselben diatonischen Toene verwenden — allein durch die Funktion der einzelnen Stufen.",
        difficulty = 4,
        funFact = "Die Pflege und Wiederherstellung des gregorianischen Chorals ist eng mit der Abtei Solesmes in Frankreich verbunden. Guido Nippold und Pater Joseph Pothier rekostruierten im 19. Jahrhundert auf Basis mittelalterlicher Handschriften die urspruengliche rhythmische Freiheit des Chorals."
    ),

    Question(
        categoryId = 5,
        questionText = "Was versteht man unter 'Musica Ficta' in der mittelalterlichen und Renaissance-Musik?",
        answerA = "Noten, die ausserhalb des hexachordalen Systems liegen und vom Saenger improvisatorisch erniedrigt oder erhoht werden",
        answerB = "Fiktive Notenbezeichnungen, die in Handschriften fuer verlorene Stimmen stehen",
        answerC = "Eine improvisierte Gesangstechnik, bei der Melodien frei verziert werden",
        answerD = "Stimmfuehrungsregeln fuer den verbotenen Tritonus in polyphoner Musik",
        correctAnswer = 0,
        explanation = "Musica Ficta bezeichnet in der mittelalterlichen Musiktheorie Toene, die nicht im regulaeren Hexachordsystem (ut-re-mi-fa-sol-la) stehen, z.B. Fis oder B. Saenger fuegten sie implizit hinzu, ohne dass sie notiert wurden, um harte Intervalle (besonders den Tritonus mi contra fa) zu vermeiden oder glatte Kadenzen zu erzeugen.",
        difficulty = 4,
        funFact = "Die Rekonstruktion von Musica Ficta ist ein zentrales Problem der historischen Auffuehrungspraxis: Da die Alterierungen nicht notiert sind, muessen Ausfuehrende entscheiden, wo sie eingesetzt werden sollen — verschiedene Editionen einer Motette koennen dadurch klanglich erheblich differieren."
    ),

    Question(
        categoryId = 5,
        questionText = "Was ist die 'Sonatensatzform' und welche drei Hauptabschnitte hat sie in ihrer klassischen Ausformung?",
        answerA = "Eine einteilige Form mit Exposition, Durchfuehrung und Reprise; nur ein Thema wird verwendet",
        answerB = "Eine dreiteilige Form: Exposition (Einfoehrung mindestens zweier kontrastierender Themen), Durchfuehrung (motivische Verarbeitung), Reprise (Rueckkehr beider Themen in der Grundtonart)",
        answerC = "Eine zweiteilige Form: A-Teil (Grundtonart) und B-Teil (Dominanttonart) ohne Reprise",
        answerD = "Eine fuenfteilige Rondoform: ABACA mit festem Ritornell in der Grundtonart",
        correctAnswer = 1,
        explanation = "Die Sonatensatzform ist das zentrale Formprinzip der klassischen und romantischen Instrumentalmusik. Die Exposition stellt mindestens zwei Themen in verschiedenen Tonarten vor (Tonika und Dominante bzw. Mediante), die Durchfuehrung verarbeitet das motivische Material harmonisch und formal frei, die Reprise kehrt zu beiden Themen in der Grundtonart zurueck.",
        difficulty = 4,
        funFact = "Der Begriff 'Sonatensatzform' ist eine musiktheoretische Abstraktion des 19. Jahrhunderts — Haydn, Mozart und Beethoven kannten diesen Begriff nicht. A.B. Marx pragte den Begriff 1845. Die tatsaechliche Praxis der Meister war viel flexibler als jedes theoretische Schema suggeriert."
    ),

    // ── Ethnomusicology (5) ──────────────────────────────────────────────────

    Question(
        categoryId = 5,
        questionText = "Was ist das Tonsystem des 'Pelog' in der javanischen Gamelan-Musik und wie unterscheidet es sich von 'Slendro'?",
        answerA = "Pelog ist ein pentatonisches aequidistantes Tonsystem; Slendro ist heptatonisch und unregelmaessig",
        answerB = "Pelog ist ein heptatonisches unregelmaessiges System mit Halbtoen; Slendro ist ein pentatonisches annaehernd aequidistantes System",
        answerC = "Pelog verwendet ausschliesslich Ganztoenschritte; Slendro hat drei verschiedene Intervalgroessen",
        answerD = "Beide Systeme sind identisch, aber Pelog wird im Westen gespielt und Slendro im Osten Javas",
        correctAnswer = 1,
        explanation = "Pelog ist ein 7-toeniges Tonsystem mit unregelmaessigen, teils kleinen Intervallen (aehnlich Halbtoen) — in der Praxis werden meist 5 aus 7 Toenen fuer eine Modalitaet ausgewaehlt. Slendro ist 5-toenig mit nahezu gleichen Intervallen von etwa 240 Cent (knapp zwischen Ganzton und kleiner Terz). Beide Systeme sind nicht mit westlicher temperierter Stimmung identisch.",
        difficulty = 4,
        funFact = "Claude Debussy hoerte 1889 auf der Pariser Weltausstellung zum ersten Mal Gamelan-Musik — ein Erlebnis, das seine Kompositionssprache nachhaltig pragte. Die parallelen Akkordfolgen und die Pentatonik in vielen Werken Debussys gelten als direkte Reaktion auf diesen Klangeindruck."
    ),

    Question(
        categoryId = 5,
        questionText = "Was beschreibt das Konzept der 'Heterophonie' in der Ethnomusicologie?",
        answerA = "Mehrstimmigkeit, bei der alle Stimmen exakt dieselbe Melodie in verschiedenen Oktaven singen",
        answerB = "Simultane Auffuehrung derselben melodischen Linie durch mehrere Musiker, wobei jeder leichte Variationen einbringt",
        answerC = "Eine improvisierte Variationstechnik, bei der Solisten abwechselnd eine feste Melodie ornamentieren",
        answerD = "Polyphonie, bei der alle Stimmen unabhaengige Melodien spielen ohne harmonische Beziehung",
        correctAnswer = 1,
        explanation = "Heterophonie bezeichnet eine Musizierpraxis, bei der mehrere Ausfuehrende gleichzeitig dieselbe Melodie spielen oder singen, jeder aber mit individuellen Verzierungen, Rhythmusvariationen oder leicht abweichenden Toenen. Dies ist charakteristisch fuer viele asiatische, arabische und osteuropaeische Musiktraditionen.",
        difficulty = 4,
        funFact = "Der Begriff 'Heterophonie' wurde vom deutschen Musikforscher Erich Moritz von Hornbostel in den 1900ern fuer seine vergleichenden Studien gepragt. Die systematische Vergleichende Musikwissenschaft (Comparative Musicology), Vorlaeuferin der Ethnomusicologie, wurde massgeblich von Hornbostel und Carl Stumpf am Berliner Phonogrammarchiv entwickelt."
    ),

    Question(
        categoryId = 5,
        questionText = "Was ist das 'Hornbostel-Sachs-System' und wie klassifiziert es Musikinstrumente?",
        answerA = "Es klassifiziert Musikinstrumente nach Spieltechnik (gezupft, gestrichen, geblasen, geschlagen)",
        answerB = "Es ist ein Dezimalklassifikationssystem, das Instrumente nach dem schwingenden Material in Chordophone, Aerophone, Membranophone, Idiophone und (spater) Elektrophone gliedert",
        answerC = "Es ordnet Instrumente nach geografischer Herkunft in Europa, Asien, Afrika und Amerika",
        answerD = "Es klassifiziert Instrumente nach ihrer Tonumfang und Stimmung in verschiedene Gruppen",
        correctAnswer = 1,
        explanation = "Das Hornbostel-Sachs-System (1914) ist das international anerkannte Standardsystem zur Klassifikation von Musikinstrumenten. Die vier Hauptklassen: Idiophone (das Material selbst schwingt, z.B. Glocke, Xylophon), Membranophone (gespannte Membran schwingt, z.B. Trommel), Chordophone (Saite schwingt, z.B. Geige, Harfe), Aerophone (Luftsaeule schwingt, z.B. Floete, Trompete). Elektrophone wurden 1940 als fuenfte Klasse ergaenzt.",
        difficulty = 4,
        funFact = "Das Hornbostel-Sachs-System ist so universell, dass es sogar fruehistorische Instrumente aus Grabungen korrekt klassifizieren kann — ein 35.000 Jahre alter Knochenflotte aus der Swaebischen Alb ist eindeutig ein Aerophon vom Typ Floete."
    ),

    Question(
        categoryId = 5,
        questionText = "Was ist 'Maqam' im arabischen und tuerischen Musiksystem und wie unterscheidet er sich von einem europaischen Modus?",
        answerA = "Maqam ist identisch mit einem europaischen Modus, aber mit anderen Namen bezeichnet",
        answerB = "Maqam ist ein komplexes System, das neben Tonskala auch charakteristische Melodiefiguren, Einstiegstoene, Klimaxtoene und improvisatorische Konventionen umfasst",
        answerC = "Maqam beschreibt nur die Stimmung des Instruments, nicht die melodische Struktur",
        answerD = "Maqam ist eine Rhythmusstruktur (Iqa) ohne festgelegte Tonhoehen",
        correctAnswer = 1,
        explanation = "Ein Maqam (arab.: Ort, Stellung) ist weit mehr als eine Tonleiter: Er umfasst spezifische Intervallverhaeltnisse (oft Vierteltoene), charakteristische melodische Formeln (Motivik), bevorzugte Anfangs- und Endtoene, einen emotionalen Charakter und Konventionen fuer Improvisation (Taqsim). Es gibt ueber 70 klassische Maqamat.",
        difficulty = 4,
        funFact = "Das Viertelton-System der arabischen Musik entsteht nicht durch willkuerliche Aufteilung, sondern aus der historischen Entwicklung natuerlicher Bund-Positionen auf der Ud (Laute). Zeitgenoessische arabische Musiker diskutieren lebhaft ueber Standardisierung versus regionale Vielfalt der Vierteltoene."
    ),

    Question(
        categoryId = 5,
        questionText = "Was versteht die Ethnomusicologie unter dem Begriff 'Musikkulturkreis' und welche methodische Kritik richtet sich gegen dieses Konzept?",
        answerA = "Ein Kreissymbol auf Notenpapier, das kulturspezifische Zeitvorstellungen darstellt; kritisiert als zu simplistisch",
        answerB = "Ein geografisch definierter Bereich mit gemeinsamen Musikmerkmalen; kritisiert wegen Essentialisierung und Vernachlaessigung interkultureller Austauschprozesse",
        answerC = "Das Kreisen der Melodie um einen zentralen Ton; kritisiert wegen falscher universeller Anwendung",
        answerD = "Eine Gruppe von Musikern, die gemeinsam Musik ueben; kritisiert wegen mangelnder Repraesentativitaet",
        correctAnswer = 1,
        explanation = "Der Musikkulturkreis (Curt Sachs, von Hornbostel) versuchte, geografische Regionen mit gemeinsamen Musikmerkmalen zu kartieren — aehnlich wie Kulturkreistheorien in der Ethnologie. Die Kritik richtet sich gegen die Tendenz zur Essentialisierung (jede Kultur habe eine feste Identitaet), Vernachlaessigung von Wandel und interkulturellem Austausch sowie gegen den impliziten Eurozentrismus.",
        difficulty = 4,
        funFact = "Die moderne Ethnomusicologie, vertreten durch Bruno Nettl und Alan Merriam, hat den deterministischen Kulturkreis-Ansatz weitgehend aufgegeben. Stattdessen steht heute die 'Thick Description' (dichte Beschreibung) im Mittelpunkt: Musik wird im sozialen, politischen und historischen Kontext ihrer Gemeinschaft verstanden."
    ),

)

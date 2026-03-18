package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun sportQuestionsMaster5(): List<Question> = listOf(

    // ── MASTER (difficulty = 5) ── 50 questions ──────────────────────────────

    // ── Biomechanik (8) ───────────────────────────────────────────────────────

    // Question 1 – Biomechanik: Streckreflex im Sprint
    Question(
        categoryId = 6,
        questionText = "Welcher biomechanische Parameter beschreibt das Verhaeltnis zwischen Bodenkontaktzeit und Flugzeit beim Sprintlauf und gilt als primaerer Effizienzindikator fuer die Schrittfrequenz?",
        answerA = "Duty Factor (DF)",
        answerB = "Ground Reaction Force Index (GRFI)",
        answerC = "Specific Mechanical Energy (SME)",
        answerD = "Vertical Stiffness Coefficient (VSC)",
        correctAnswer = 0,
        explanation = "Der Duty Factor (DF) ist definiert als tc / (tc + tf), wobei tc die Kontaktzeit und tf die Flugzeit ist. Spitzensrpinter erreichen DF-Werte unter 0,30, was bedeutet, dass sie weniger als 30 % jedes Schrittzyklus am Boden verbringen. Ein niedriger DF korreliert stark mit hoeherer maximaler Sprintgeschwindigkeit.",
        difficulty = 5,
        funFact = "Usain Bolt erreichte bei seinem Weltrekordlauf 2009 in Berlin eine Kontaktzeit von ca. 83 ms und eine Flugzeit von ca. 120 ms — ein Duty Factor von etwa 0,41, was zeigt, dass selbst Weltrekordlaeufer keinen extremen DF aufweisen, sondern ueber die Kraftimpulse pro Schritt glaenzen."
    ),

    // Question 2 – Biomechanik: Elastische Energie in der Achillessehne
    Question(
        categoryId = 6,
        questionText = "Wie viel Prozent der mechanischen Energie, die beim Laufen in der Achillessehne waehrend der Bodenphase gespeichert wird, kann durch passiven Rueckfederungseffekt (elastisches Rebound-Prinzip) wiedergewonnen werden?",
        answerA = "Etwa 25-35 %",
        answerB = "Etwa 50-60 %",
        answerC = "Etwa 75-80 %",
        answerD = "Nahezu 100 %",
        correctAnswer = 2,
        explanation = "Studien zeigen, dass die Achillessehne beim Laufen bis zu 75-80 % der gespeicherten elastischen Energie als kinetische Energie zurueckgibt. Dieser passive Energierueckgewinn reduziert den aktiven Muskelaufwand erheblich und macht den menschlichen Laufgang energetisch effizienter als rein muskelgetriebene Bewegung.",
        difficulty = 5,
        funFact = "Die Achillessehne kann beim Sprinten eine Kraft von ueber 12 kN (ca. 12-faches Koerpergewicht) aufnehmen und gibt die gespeicherte Energie innerhalb von ca. 50 Millisekunden wieder ab — schneller als jeder aktive Muskelkontraktionsprozess."
    ),

    // Question 3 – Biomechanik: Winkelimpuls im Stabhochsprung
    Question(
        categoryId = 6,
        questionText = "Welches biomechanische Prinzip erklaert, warum ein Stabhochspringer beim Abheben vom Boden einen moeglichst kleinen Abhebwinkel (flache Anlaufbahn) anstreben soll?",
        answerA = "Maximierung des Drehimpulses fuer die spaetere Koerperrotation ueber der Latte",
        answerB = "Maximierung der kinetischen Energie des Anlaufs, die in potenzielle Energie und elastische Stabenergie umgewandelt wird",
        answerC = "Minimierung des Luftwiderstands durch optimale Koerperneigung beim Einstich",
        answerD = "Maximierung der Normalkraft auf den Stab fuer optimale Biegung",
        correctAnswer = 1,
        explanation = "Der flache Anlaufwinkel erhoeht die horizontale Geschwindigkeit beim Einstich, die als kinetische Energie (0,5 * m * v²) vollstaendig in potenzielle Energie (Koerpermasseanteil) und elastische Deformationsenergie des Stabs umgewandelt wird. Je hoeher v beim Einstich, desto mehr Gesamtenergie steht fuer die Uebungshoehe zur Verfuegung.",
        difficulty = 5,
        funFact = "Moderne Carbon-Fiber-Staebe koennen Energie mit einem Wirkungsgrad von ueber 95 % zurueckgeben. Armand Duplantis (Weltrekord 6,26 m) laeuft mit ca. 9,5 m/s an — die kinetische Energie dieser Geschwindigkeit entspricht theoretisch einer Sprunghhoehe von 4,59 m, der Rest kommt durch Kraft und Technik."
    ),

    // Question 4 – Biomechanik: Dehnungs-Verkuergungs-Zyklus
    Question(
        categoryId = 6,
        questionText = "Wie wird der biomechanische Mechanismus bezeichnet, bei dem ein Muskel-Sehnen-Komplex zunaechst exzentrisch gedehnt und dann konzentrisch verkuerzt wird, um mehr Leistung als bei rein konzentrischer Kontraktion zu erzeugen?",
        answerA = "Myotatischer Reflex-Booster (MRB)",
        answerB = "Stretch-Shortening Cycle (SSC)",
        answerC = "Force-Velocity Enhancement (FVE)",
        answerD = "Titin-Assisted Contraction (TAC)",
        correctAnswer = 1,
        explanation = "Der Stretch-Shortening Cycle (SSC) beschreibt die Abfolge exzentrischer Dehnung, kurze isometrische Phase und konzentrische Kontraktion. Der SSC erhoert die Kraft- und Leistungsabgabe durch drei Mechanismen: elastische Energiespeicherung in Sehnen, myotatischen Dehnreflex und Potenzierung der Aktomyosin-Querbrueckenbildung.",
        difficulty = 5,
        funFact = "Das Titin-Protein, das laengste bekannte Eiweissmolekuel im menschlichen Koerper (ca. 3.700 kDa), wirkt wie eine molekulare Feder innerhalb der Sarkomere und traegt zum SSC bei, indem es bei Dehnung Energie speichert und die Aktomyosin-Bindung foerdert."
    ),

    // Question 5 – Biomechanik: Wurfparabeln
    Question(
        categoryId = 6,
        questionText = "Bei welchem Abwurfwinkel wird die maximale Wurfweite beim Hammerwerfen theoretisch erreicht, und warum weicht der in der Praxis optimale Winkel davon ab?",
        answerA = "45° theoretisch; in der Praxis 42-44°, weil der Abwurfpunkt deutlich hoeher als der Landepunkt liegt",
        answerB = "45° theoretisch; in der Praxis 38-40°, weil die Luftwiderstandskraft den Wurfkoerper verlangsamt",
        answerC = "30° theoretisch und praktisch identisch, weil der Hammer durch Rotation zusaetzlichen Auftrieb erhaelt",
        answerD = "60° theoretisch; in der Praxis 45°, weil die Zentrifugalkraft den optimalen Winkel nach oben verschiebt",
        correctAnswer = 0,
        explanation = "In der klassischen Projektilmechanik (gleiche Abwurf- und Landehhoehe, kein Luftwiderstand) ist 45° optimal. Da der Abwurfpunkt beim Hammerwerfen jedoch ca. 1,5-2 m ueber dem Landepunkt liegt, verschiebt sich der optimale Winkel auf ca. 42-44°. Dies ergibt sich aus der modifizierten Wurfweiten-Gleichung fuer unterschiedliche Abwurf- und Auftreffhoehen.",
        difficulty = 5,
        funFact = "Ryan Crouser, Weltrekordhalter im Kugelstossen (23,56 m), wuerde theoretisch bei 45° am weitesten stossen. In der Praxis stosst er bei ca. 38-40° — der niedrigere Winkel ist optimal weil der Abwurfpunkt deutlich hoeher als das Auftreffpunkt liegt und weil der Sportler bei niedrigerem Winkel mehr horizontale Kraft entwickeln kann."
    ),

    // Question 6 – Biomechanik: Kraftvektor im Gewichtheben
    Question(
        categoryId = 6,
        questionText = "In der Sportwissenschaft beschreibt der Begriff 'Bar Path' im Gewichtheben die horizontale Displacement-Kurve der Langhantel. Welchen charakteristischen Verlauf zeigt ein technisch effizienter Clean beim Weltklasse-Gewichtheber?",
        answerA = "Gerade vertikale Linie ohne jede horizontale Abweichung",
        answerB = "Leichte S-Kurve: die Stange bewegt sich zuerst minimal nach vorn, dann zurueck ueber die Fussmitte, dann beim Hochziehen wieder nach vorn",
        answerC = "J-Kurve: die Stange bewegt sich stark nach vorn, dann bogenfoermig zurueck",
        answerD = "Pendelfoermige Kurve mit mehrfachem Vorwaerts-Rueckwaerts-Wechsel entsprechend der Koerperrotation",
        correctAnswer = 1,
        explanation = "Beim technisch korrekten Clean beschreibt die Stange eine charakteristische S-Kurve: Vom Boden bewegt sie sich zunachst leicht nach anterior, waehrend die Knie geschoben werden. Dann kehrt sie zur Fussmitte zurueck (Knieschub-Phase), und beim explosiven Zweizug (Double Extension) bewegt sie sich mit dem Koerper wieder leicht anterior. Diese Kurve minimiert den Drehimpuls und die fuer den Fang benotige Umlenkungsarbeit.",
        difficulty = 5,
        funFact = "Moderne Forschung mit Force Plates und 3D-Kinematik-Tracking zeigt, dass Weltklasse-Gewichtheber die horizontale Stangenverschiebung auf unter 5 cm halten, waehrend Anfaenger oft 15-20 cm Abweichung zeigen — eine der genauesten Messmethoden fuer Technikniveau im Gewichtheben."
    ),

    // Question 7 – Biomechanik: Drehmomentvektoren im Radfahren
    Question(
        categoryId = 6,
        questionText = "Welche biomechanische Groesse beschreibt am praezisesten die Effizienz der Tretbewegung beim Radfahren und wird in modernen Power-Metern als Qualitaetsmass verwendet?",
        answerA = "Pedalling Effectiveness Index (PEI): Verhaeltnis von Normalkraft zu totaler Pedalkraft",
        answerB = "Torque Efficiency (TE): prozentualer Anteil des positiven Drehmoments am Gesamtdrehmoment ueber eine volle Umdrehung",
        answerC = "Cleat Force Vector (CFV): Winkel zwischen Kraftvektor und Pedalflaeche",
        answerD = "Crank Arm Moment Ratio (CAMR): Drehmoment bezogen auf die Kurbelllaenge",
        correctAnswer = 1,
        explanation = "Die Torque Efficiency (TE) misst den prozentualen Anteil des positiven Drehmoments (Kraft in Vorwaertsrichtung) am Gesamtdrehmoment ueber einen vollstaendigen 360°-Pedalzyklus. Hohe TE bedeutet, dass der Fahrer auch im oberen und unteren Totpunkt noch effektive Kraft uebertraegt. Profis erreichen TE-Werte von 60-70 %, Freizeitfahrer oft nur 40-50 %.",
        difficulty = 5,
        funFact = "Der Garmin Vector Pedalkraftmesser und aehnliche Systeme koennen den Kraftvektor in 60 Punkten pro Umdrehung messen und visualisieren — Profiradteams nutzen diese Daten fuer individualisiertes Kurbelllaengen-Optimierung und Sattelhoeheneinstellung auf wenige Millimeter genau."
    ),

    // Question 8 – Biomechanik: Schwimmtechnik Wellenwiderstand
    Question(
        categoryId = 6,
        questionText = "Welches hydrodynamische Phaenomen begrenzt die maximale Rumpfgeschwindigkeit eines Freistil-Schwimmers unabhaengig von seiner Kraftleistung, und wie wird die theoretische Huellgeschwindigkeit berechnet?",
        answerA = "Kavitation: Druckabfall unter den Dampfdruck des Wassers; berechnet als v_kav = sqrt(2 * P_atm / rho)",
        answerB = "Rumpfgeschwindigkeit (Hull Speed): Wellenwiderstandsgrenze; berechnet als v_h = 1,34 * sqrt(L_wl) in Knoten",
        answerC = "Laminar-turbulenter Uebergang: Reynolds-Zahl-Grenze; berechnet als Re = v * L / nu",
        answerD = "Viskositaetsgrenze: Grenzschicht-Abloesung; berechnet als v_sep = mu / (rho * delta)",
        correctAnswer = 1,
        explanation = "Das Konzept der Hull Speed aus der Nautik wird in der Schwimmbiomechanik analogisch angewendet: Ein Schwimmer erzeugt beim Gleiten eine Bugwelle, deren Laenge von der Koerperlaenge abhaengt. Die theoretische Grenzgeschwindigkeit errechnet sich aus der Wellenlaenge des Buksystems. Technisch ist der Wellenwiderstand durch Koerperstreckung und optimale Wasserlage minimierbar.",
        difficulty = 5,
        funFact = "Michael Phelps' aussergewoehnliche Anthropometrie — Spannweite von 201 cm bei 193 cm Koerpergroesse plus relativ kurze Beine — verleiht ihm hydrodynamische Vorteile, weil sein langer Rumpf die effektive Wasserlinienlaenge maximiert und damit die theoretische Rumpfgeschwindigkeitsgrenze anhebt."
    ),

    // ── Trainingslehre & Periodisierung (7) ──────────────────────────────────

    // Question 9 – Periodisierung: Matveyev-Modell
    Question(
        categoryId = 6,
        questionText = "Der sowjetische Sportwissenschaftler Lew Matveyev entwickelte in den 1960er-Jahren das klassische Periodisierungsmodell. Welche drei Hauptphasen unterscheidet dieses Modell, und welche inverse Beziehung gilt dabei?",
        answerA = "Akkumulation, Transformation, Realisation — Volumen und Spezifitaet sind invers korreliert",
        answerB = "Allgemeine Vorbereitungsperiode, spezielle Vorbereitungsperiode, Wettkampfperiode — Volumen und Intensitaet sind invers korreliert",
        answerC = "Hypertrophie, Kraft, Leistung — Wiederholungszahl und Gewicht sind invers korreliert",
        answerD = "Mesozyklus, Mikrozyklus, Makrozyklus — Ruhezeit und Arbeitszeit sind invers korreliert",
        correctAnswer = 1,
        explanation = "Matveyevs traditionelles Modell unterteilt den Jahreszyklus in Allgemeine Vorbereitungsperiode (hohes Volumen, niedrige Intensitaet), Spezielle Vorbereitungsperiode (sinkendes Volumen, steigende Intensitaet) und Wettkampfperiode (niedrigstes Volumen, hoechste Intensitaet). Die inverse Volumen-Intensitaets-Beziehung ist das Kernprinzip dieses Modells.",
        difficulty = 5,
        funFact = "Matveyevs Buch 'Periodisierung des sportlichen Trainings' (1965) war urspruenglich als vertrauliches Sowjet-Dokument klassifiziert. Als es in den 1970er Jahren im Westen bekannt wurde, revolutionierte es die Trainingswissenschaft weltweit — Tudor Bompa popularisierte es fuer westliche Sportler."
    ),

    // Question 10 – Trainingslehre: Superkompensation
    Question(
        categoryId = 6,
        questionText = "Das Superkompensationsmodell beschreibt die zeitliche Reaktion des Organismus auf Trainingsreize. Welche wissenschaftliche Kritik wird an diesem Modell regelmaessig geaeussert?",
        answerA = "Das Modell unterschaetzt die Regenerationszeit; die tatsaechliche Superkompensation dauert 72-96 Stunden, nicht wie angenommen 24-48 Stunden",
        answerB = "Das Modell ist wissenschaftlich stark vereinfacht: Einzelne Kapazitaeten (Kraft, Ausdauer, Technik) superkompensieren zu unterschiedlichen Zeitpunkten und koennen nicht durch eine einzige Kurve dargestellt werden",
        answerC = "Das Modell gilt nur fuer Ausdauersportarten; im Krafttraining gibt es keine Superkompensation, sondern nur Hypertrophie",
        answerD = "Das Modell ist korrekt, wird aber falsch angewendet, weil Sportler den Gipfelpunkt nicht exakt bestimmen koennen",
        correctAnswer = 1,
        explanation = "Die Hauptkritik am Superkompensationsmodell ist seine wissenschaftliche Vereinfachung: Unterschiedliche physiologische Systeme (Glykogen, Muskelprotein, neuromuskulaere Funktion, Hormonsystem) regenerieren und superkompensieren zu voellig unterschiedlichen Zeitpunkten. Ein einheitliches Superkompensationsfenster existiert nicht — die Realitaet ist ein komplexes Mosaik verschiedener zeitversetzter Adaptationsprozesse.",
        difficulty = 5,
        funFact = "Gluykogen-Superkompensation geschieht am schnellsten (24-48 h), waehrend strukturelle Muskelprotein-Adaptationen 48-72 h benoetigen und neurales Lernen wochen- bis monatelang anhaelt. Diese zeitliche Entkopplung macht das einfache Superkompensationsmodell wissenschaftlich problematisch."
    ),

    // Question 11 – Trainingslehre: Blockperiodisierung nach Issurin
    Question(
        categoryId = 6,
        questionText = "Wer entwickelte das Konzept der Blockperiodisierung als Alternative zu Matveyevs Modell, und was ist der grundlegende Unterschied in der Belastungsstruktur?",
        answerA = "Charlie Francis: Statt Volumen-Intensitaets-Wellengang werden alle Faehigkeiten gleichzeitig trainiert (konjugierte Methode)",
        answerB = "Vladimir Issurin: Statt simultaner Entwicklung vieler Faehigkeiten werden konzentrierte Trainingsblocks mit je 1-3 Zielfaehigkeiten sequenziell aneinandergereiht",
        answerC = "Yuri Verkhoshansky: Statt Matveyevs Wellenstruktur wird ein einziger langer Akkumulationsblock vor der Wettkampfsaison absolviert",
        answerD = "Tudor Bompa: Statt eines Jahreszyklus werden mehrere gleichwertige Zyklen (Doppelperiodisierung) geplant",
        correctAnswer = 1,
        explanation = "Vladimir Issurin entwickelte die Blockperiodisierung, bei der konzentrierte Trainingsblocks (Akkumulationsblock, Transmutationsblock, Realisationsblock) nacheinander geschaltet werden. Jeder Block hat 1-3 priorisierte Ziele, was Training-Residuen der vorherigen Faehigkeiten nutzt, waehrend neue Anpassungen aufgebaut werden. Dies steht im Kontrast zu Matveyevs gleichzeitiger Entwicklung vieler Faehigkeiten.",
        difficulty = 5,
        funFact = "Issurin entwickelte das Blockmodell basierend auf Daten aus der sowjetischen Sportschwimmschule. Schwimmer, die nach dem Blockmodell trainierten, zeigten statistisch signifikant bessere Wettkampfpeakleistungen als nach traditioneller Periodisierung — ein Befund, der spaeter in anderen Sportarten repliziert wurde."
    ),

    // Question 12 – Trainingslehre: VO2max-Kinetik
    Question(
        categoryId = 6,
        questionText = "Was beschreibt der Begriff 'Oxygen Uptake Kinetics' (VO2-Kinetik) in der Sportwissenschaft, und welcher Parameter charakterisiert die Schnelligkeit des Anstiegs?",
        answerA = "Die Gesamtmenge des aufgenommenen Sauerstoffs; charakterisiert durch den VO2max-Absolutwert in ml/min",
        answerB = "Die Zeitdauer bis zur vollstaendigen Erschoepfung bei maximaler Belastung; charakterisiert durch die Time-to-Exhaustion",
        answerC = "Die Geschwindigkeit der VO2-Anpassung an eine neue Belastungsstufe; charakterisiert durch die Zeitkonstante tau (Zeit bis 63 % des Steady-State-Werts)",
        answerD = "Die Kapazitaet des Herz-Kreislauf-Systems; charakterisiert durch das Schlagvolumen in ml/Schlag",
        correctAnswer = 2,
        explanation = "Die VO2-Kinetik beschreibt, wie schnell die Sauerstoffaufnahme nach dem Belastungsbeginn dem neuen Steady-State-Level folgt. Die Zeitkonstante tau (in Sekunden) gibt an, nach welcher Zeit 63 % des neuen Gleichgewichts erreicht sind. Gut trainierte Ausdauersportler haben kleinere tau-Werte (schnellere Kinetik), was bedeutet, dass sie schneller in den aeroben Gleichgewichtszustand gelangen und die anaerobe Energiebereitstellung minimieren.",
        difficulty = 5,
        funFact = "Elite-Ausdauersportler koennen tau-Werte unter 15 Sekunden erreichen, waehrend Untrainierte Werte von 40-60 Sekunden aufweisen. Dieser Unterschied erklaert, warum Profis auch bei Belastungsbeginn weniger Laktat akkumulieren — ihre Mitochondrien sind metabolisch schneller 'hochgefahren'."
    ),

    // Question 13 – Trainingslehre: Krukovets-Prinzip
    Question(
        categoryId = 6,
        questionText = "Was beschreibt das in der Gewichthebelehre bekannte 'Prinzip der konjugierten Sequenzmethode' (nach Verkhoshansky), und fuer welche Sportart wurde es urspruenglich entwickelt?",
        answerA = "Gleichzeitiges Training aller biomotorischen Faehigkeiten; entwickelt fuer den Mehrkampf",
        answerB = "Spezifische Kraftentwicklung durch plyometrische Uebungen als Hauptbelastung vor Maximalkrafttraining; entwickelt fuer den Weitsprung und Sprint",
        answerC = "Sequenzielle Konzentration der Belastung: erst maximaler Kraftblock (Schockblock), dann Ausnutzung des Langzeit-Trainingseffekts in der Wettkampfphase; entwickelt fuer Sprung- und Wurfathleten",
        answerD = "Rotation verschiedener Uebungen im Wochenzyklus, um Anpassungsplateaus zu vermeiden; entwickelt fuer Gewichtheber",
        correctAnswer = 2,
        explanation = "Verkhoshanskys Konjugierte Sequenzmethode (auch Schocktraining) sah vor, einen konzentrierten Block maximaler Kraftbelastung (Schockblock) zu absolvieren, der den Organismus stark ermuedete. Die eigentliche Leistungssteigerung trat dann verzoeigert (Delayed Training Effect, DTE) in der nachfolgenden wettkampfnahen Phase auf. Entwickelt fuer Sprung- und Wurfathleten der UdSSR.",
        difficulty = 5,
        funFact = "Verkhoshansky testete diesen Ansatz an sowjetischen Weitspringern und Dreispringern in den 1960er-70er Jahren. Die Weltrekorde des sowjetischen Dreispringers Viktor Saneyev (1968-1976) werden teilweise auf diese fortschrittliche Periodisierungsmethode zurueckgefuehrt."
    ),

    // Question 14 – Trainingslehre: Herzfrequenzvariabilitaet
    Question(
        categoryId = 6,
        questionText = "Der RMSSD-Wert ist ein wichtiger HRV-Parameter im Trainingsmonitoring. Was misst er genau, und wie wird er interpretiert?",
        answerA = "Root Mean Square of Successive Differences: Standardabweichung aufeinanderfolgender RR-Intervalle; hoher Wert = gute parasympathische Aktivitaet und Erholungsbereitschaft",
        answerB = "Ratio of Maximum to Minimum Systolic Deviation: Blutdruckvariabilitaet; hoher Wert = kardiovaskulaere Fitness",
        answerC = "Relative Mean Systolic-to-Stroke Duration: Herzeffizienzmass; niedriger Wert = hoehere Ausdauerleistung",
        answerD = "Running Mean of Spectral Strength Distribution: frequenzbasiertes HRV-Mass; mittlerer Wert = optimale Belastung",
        correctAnswer = 0,
        explanation = "RMSSD (Root Mean Square of Successive Differences) berechnet die Quadratwurzel des Mittelwerts der quadrierten Differenzen aufeinanderfolgender NN-Intervalle. Hohe RMSSD-Werte spiegeln hohe parasympathische (vagale) Aktivitaet wider, was auf gute Erholung und Bereitschaft fuer intensives Training hinweist. Niedrige Werte deuten auf Uebertraining oder unzureichende Regeneration hin.",
        difficulty = 5,
        funFact = "Weltklasse-Ausdauersportler zeigen Ruhe-RMSSD-Werte von teilweise ueber 100 ms, waehrend gesunde, untrainierte Erwachsene typischerweise 20-50 ms aufweisen. Olympiasieger im Triathlon und Radsport werden regelmassig mit Werten ueber 150 ms dokumentiert."
    ),

    // Question 15 – Trainingslehre: RPE-Skala Borg
    Question(
        categoryId = 6,
        questionText = "Gunnar Borg entwickelte die originale RPE-Skala (Ratings of Perceived Exertion) mit Werten von 6 bis 20. Welches physiologische Korrelat bildete die Grundlage fuer diese spezifische Skalierung?",
        answerA = "Die Skala entspricht der Herzfrequenz dividiert durch 10; Wert 15 = ca. 150 Schlaege pro Minute",
        answerB = "Die Skala entspricht der Blutlaktatkonzentration multipliziert mit 2; Wert 14 = ca. 7 mmol/l",
        answerC = "Die Skala entspricht der Sauerstoffaufnahme als Prozentsatz der VO2max; Wert 16 = 80 % VO2max",
        answerD = "Die Skala entspricht dem relativen Energieverbrauch in kcal/min; Wert 18 = 18 kcal/min",
        correctAnswer = 0,
        explanation = "Borg konzipierte die 6-20-Skala bewusst so, dass ein Multiplikator von 10 die ungefaehre Herzfrequenz ergibt: RPE 6 entspricht einer HF von ~60 bpm (Ruhe), RPE 20 entspricht ~200 bpm (maximale Belastung). Diese direkte Kopplung an die Herzfrequenz machte die Skala zum einfach kommunizierbaren Monitoringinstrument ohne Messgeraete.",
        difficulty = 5,
        funFact = "Borgs spaetere CR10-Skala (Category-Ratio, 0-10) wurde spezifisch fuer die Wahrnehmung von Schmerz und lokaler Muskelbelastung entwickelt, waehrend die originale Skala primaer die kardiopulmonale Beanspruchung erfasst. In der Intensitaetssteuerung werden heute beide komplementaer eingesetzt."
    ),

    // ── Sporternaehrung (7) ───────────────────────────────────────────────────

    // Question 16 – Sporternaehrung: Kohlenhydrat-Loading
    Question(
        categoryId = 6,
        questionText = "Das klassische Carbohydrate-Loading-Protokoll nach Astrand (1967) umfasst eine Depletionsphase gefolgt von einer Superkomensationsphase. Welches neuere Protokoll erzielt aehnliche Glykogenwerte ohne die gefuerchtete Depletionsphase?",
        answerA = "Das 3-Tages-Modifizierte Protokoll nach Sherman (1981): Kein Erschoepfungstraining, stattdessen moderate Intensitaetsreduktion + hohe Kohlenhydratzufuhr in den letzten 3 Tagen",
        answerB = "Das High-Fructose-Protokoll: Fruktose-dominant ernaehren fuer 48 h vor dem Wettkampf aktiviert spezifische Glykolysepfade",
        answerC = "Das LCHF-Switching-Protokoll: Ketogene Ernaehrung fuer 5 Tage, dann 24h Kohlenhydrat-Refeed vor dem Wettkampf",
        answerD = "Das Glycogen-Priming-Protokoll: Intravenose Glukoseloesung 12 h vor dem Wettkampf maximiert Leberglykogen",
        correctAnswer = 0,
        explanation = "Sherman et al. (1981) zeigten, dass ein modifiziertes Protokoll ohne die belastende Depletionsphase (ersetzte durch normales Training + progressive Intensitaetsreduktion) in den letzten 3 Tagen mit hoher Kohlenhydratzufuhr (70 % der Kalorien) aehnlich hohe Muskelglykogenwerte erzielte. Dieses Protokoll ist heute Standard, da es Ermuedung und Verletzungsrisiko der Depletionsphase vermeidet.",
        difficulty = 5,
        funFact = "Muskelglykogen kann durch optimales Loading von ca. 80-100 mmol/kg Nassgewicht auf 150-200 mmol/kg erhoeht werden. Dies verlaengert die Zeit bis zur Erschoepfung bei submaximalem Training (~70 % VO2max) nachweislich um 20-45 Minuten."
    ),

    // Question 17 – Sporternaehrung: Protein-Timing
    Question(
        categoryId = 6,
        questionText = "Wie wird das Konzept des 'Anabolen Fensters' (Anabolic Window of Opportunity) in der aktuellen Sporternaehrungsforschung bewertet, und was zeigen Meta-Analysen dazu?",
        answerA = "Es ist eindeutig bestaetigt: Proteinkonsum innerhalb von 30 Minuten nach dem Training verdoppelt die Muskelproteinsynthese gegenueber spaeterem Konsum",
        answerB = "Es ist stark ueberbewertet: Aktuelle Meta-Analysen zeigen, dass der Effekt des Post-Workout-Timings minimal ist, wenn die Gesamtproteinzufuhr und -verteilung ueber den Tag stimmt",
        answerC = "Es gilt nur fuer aeltere Athleten (>50 Jahre) mit altersbedingter anaboler Resistenz, nicht fuer junge Sportler",
        answerD = "Es existiert nur bei gleichzeitiger Einnahme von schnellen und langsamen Proteinen (Whey + Casein-Kombination)",
        correctAnswer = 1,
        explanation = "Aktuelle systematische Reviews und Meta-Analysen (u.a. Schoenfeld & Aragon, 2013, 2018) zeigen, dass das 'Anabole Fenster' stark ueberschaetzt wurde. Entscheidender fuer die Muskelproteinsynthese ist die Gesamtproteinzufuhr (~1,6-2,2 g/kg/Tag), eine regelmaessige Verteilung ueber 3-5 Mahlzeiten und ausreichend Leucin pro Portion (~2-3 g). Das Timing hat wenn ueberhaupt, dann einen sehr kleinen zusaetzlichen Effekt.",
        difficulty = 5,
        funFact = "Leucin, die verzweigtkettige Aminosaeure, wirkt als primaerer mTOR-Aktivator und loest die Muskelproteinsynthese aus. Die Leucin-Schwelle (~2-3 g pro Mahlzeit, entspricht ~25-40 g hochwertiges Protein) ist relevanter als das genaue Timing der Proteinzufuhr."
    ),

    // Question 18 – Sporternaehrung: Relative Energy Deficiency
    Question(
        categoryId = 6,
        questionText = "Was bezeichnet der medizinische Begriff 'Relative Energy Deficiency in Sport' (RED-S), und wodurch unterscheidet er sich konzeptionell von der frueheren 'Weiblichen Athletentriade'?",
        answerA = "RED-S ist ein neuer Begriff fuer dieselbe Erkrankung; der einzige Unterschied ist, dass RED-S auch Kraftsportler einschliesst, waehrend die Triade nur Ausdauersportlerinnen betraf",
        answerB = "RED-S beschreibt den breiteren Effekt einer negativen Energieverfuegbarkeit auf ueber 10 Koerpersysteme bei Athleten beider Geschlechter; die Triade beschrieb nur drei Komponenten bei Frauen (Ernaehrung, Knochen, Menstruation)",
        answerC = "RED-S bezieht sich ausschliesslich auf klinische Essstoerungen; die Triade beschrieb auch nicht-klinische Ernaehrungsdefizite",
        answerD = "RED-S ist eine IOC-Klassifikation; die Triade war eine NCAA-Klassifikation; beide beschreiben dasselbe Syndrom mit unterschiedlichen diagnostischen Schwellenwerten",
        correctAnswer = 1,
        explanation = "Das IOC fuehrte 2014 den Begriff RED-S ein, um das konzeptionell engere Triadenmodell zu ersetzen. RED-S beschreibt die Auswirkungen negativer Energieverfuegbarkeit auf Stoffwechsel, Immunsystem, Knochen, Haematologie, Wachstum, Herzfunktion, mentale Gesundheit und sportliche Leistung — bei Athleten aller Geschlechter. Die Weibliche Triade fokussierte nur auf drei Aspekte bei Frauen.",
        difficulty = 5,
        funFact = "Energieverfuegbarkeit (EA) wird berechnet als: EA = Energieaufnahme - Trainingsenergieverbrauch, bezogen auf fettfreie Koerpermasse (kcal/kg FFM/Tag). Werte unter 30 kcal/kg FFM/Tag gelten als kritisch; optimale EA liegt bei >45 kcal/kg FFM/Tag. Interessanterweise benoetigen viele Elite-Ausdauerathletinnen aktives Monitoring, da sie intuitiv unter die kritische Grenze essen."
    ),

    // Question 19 – Sporternaehrung: Carnitin-Transport
    Question(
        categoryId = 6,
        questionText = "L-Carnitin wird haeufig als leistungssteigerndes Supplement vermarktet. Was zeigt die aktuelle Studienlage zur oralen L-Carnitin-Supplementierung bei Gesunden, und was ist der limitierende Faktor?",
        answerA = "Orales L-Carnitin erhoeht nachweislich die Fettoxidation um 20-30 %; der limitierende Faktor ist die Bioverfuegbarkeit",
        answerB = "Orales L-Carnitin erhoeht signifikant die Muskel-Carnitin-Konzentration und verbessert dadurch die Ausdauerleistung bei Hochtrainierten",
        answerC = "Orales L-Carnitin erhoeht die Muskel-Carnitin-Konzentration kaum, weil die Aufnahme in Muskelzellen insulinabhaengig ist; nur in Kombination mit Kohlenhydraten (Insulinanstieg) ist ein messbarer Muskelcarnitin-Anstieg moeglich",
        answerD = "L-Carnitin-Supplementierung ist klinisch wirkungslos, weil der Koerper ausreichend endogenes Carnitin synthetisiert und orales Carnitin vollstaendig im Darm abgebaut wird",
        correctAnswer = 2,
        explanation = "Studien von Wall et al. (2011) und anderen zeigten, dass orales L-Carnitin ohne Kohlenhydrate die Muskel-Carnitin-Konzentration nicht messbar erhoht, da die Aufnahme in Muskeln vom Insulinsignal abhaengt. Nur wenn Carnitin zusammen mit ausreichend Kohlenhydraten eingenommen wird (die Insulin ausschuetten), steigt die Muskelcarnitin-Konzentration nachweislich — was jedoch fuer Leistungssportler im energiereduzierten Modus problematisch ist.",
        difficulty = 5,
        funFact = "Veganer und Vegetarier haben nachweilch niedrigere Plasma-Carnitin-Spiegel als Fleischesser, da Carnitin hauptsaechlich in rotem Fleisch vorkommt. Trotz niedrigerer Carnitin-Spiegel zeigen sie keine konsistente Leistungsbeeintraechtigung — der Koerper kann Carnitin aus Lysin und Methionin selbst synthetisieren."
    ),

    // Question 20 – Sporternaehrung: Natrium-Phosphat-Loading
    Question(
        categoryId = 6,
        questionText = "Natrium-Phosphat-Loading (NaHPO4) ist eine wenig bekannte ergogene Massnahme. Welchen physiologischen Mechanismus erklaert die potenzielle Leistungssteigerung?",
        answerA = "Erhoehung des Blut-pH-Werts (Pufferung von Laktat), aehnlich wie Natriumbikarbonat-Loading",
        answerB = "Steigerung der 2,3-Diphosphoglycerat (2,3-DPG)-Konzentration in Erythrozyten, was die Sauerstoffabgabe des Haemoglobins im Gewebe verbessert",
        answerC = "Steigerung der Kreatinphosphat-Resynthese in Muskelzellen durch zusaetzliche Phosphatgruppen",
        answerD = "Steigerung der ATP-Produktion in Mitochondrien durch Foerderung der oxidativen Phosphorylierung",
        correctAnswer = 1,
        explanation = "Phosphat-Loading erhoht die 2,3-DPG-Konzentration in Erythrozyten. 2,3-DPG ist ein allosterischer Modulator des Haemoglobins, der die Sauerstoffaffinitaet senkt und damit die Sauerstoffabgabe ans Gewebe bei niedrigem pO2 verbessert (Rechtsverschiebung der O2-Bindungskurve). Studien zeigen damit Verbesserungen der VO2max und Ausdauerleistung von 5-12 %, was eine der staerksten nachgewiesenen ergogenen Wirkungen ist.",
        difficulty = 5,
        funFact = "Trotz der wissenschaftlich gut belegten Wirksamkeit (mehrere kontrollierte Studien zeigen konsistente Verbesserungen) ist Natrium-Phosphat-Loading unter Sportlern wenig bekannt und nicht auf der WADA-Verbotsliste — ein legales Supplement mit vergleichbarer Wirkung wie Hoehhentraining."
    ),

    // Question 21 – Sporternaehrung: Natriumbikarbonat-Pufferung
    Question(
        categoryId = 6,
        questionText = "Natriumbikarbonat (Backpulver) wird als ergogenes Mittel fuer Kurzzeitausdauerleistungen (1-10 Minuten) eingesetzt. Welches Dosierungsprotokoll minimiert gastrointestinale Nebenwirkungen bei maximaler Pufferwirkung?",
        answerA = "5-7 g/kg KG einmalig 30 Minuten vor der Belastung",
        answerB = "0,3 g/kg KG einmalig, 60-90 Minuten vor Belastung (Standarddosierung ohne Nebeneffekte)",
        answerC = "0,3 g/kg KG verteilt auf 3-4 kleine Dosen ueber 30-60 Minuten ('Micro-Dosing'), kombiniert mit kohlenhydratreicher Mahlzeit",
        answerD = "0,1 g/kg KG intravenoes direkt vor der Belastung (nur im klinischen Kontext)",
        correctAnswer = 2,
        explanation = "Die Standarddosierung von 0,3 g/kg KG ist wirkungsvoell, verursacht jedoch bei vielen Athleten Uebelkeit, Blaeungen und Durchfall. Studien (u.a. Hilton et al., 2019) zeigten, dass die Aufteilung in 3-4 kleinere Portionen ueber einen laengeren Zeitraum (Micro-Dosing) mit einer kohlenhydratreichen Mahlzeit ahnliche Blutbikarbonat-Peaks erzeugt, aber die gastrointestinalen Beschwerden signifikant reduziert.",
        difficulty = 5,
        funFact = "Sebastian Coe, der britische Mittelstreckler und spaetere IAAF-Praesident, soll Natriumbikarbonat bei seinen Weltrekord-Laeufen in den 1980er Jahren genutzt haben — damals noch kaum erforscht und voellig legal. Erst jahrzehntspaeter wurde der Mechanismus sportwissenschaftlich gruendlich untersucht."
    ),

    // Question 22 – Sporternaehrung: Koffein-Adenosin-Antagonismus
    Question(
        categoryId = 6,
        questionText = "Koffein wirkt als kompetitiver Adenosin-Rezeptor-Antagonist. Welcher spezifische Rezeptorsubtyp ist fuer die leistungssteigernde Wirkung bei sportlicher Betatigung am relevantesten, und ueber welchen Mechanismus?",
        answerA = "A1-Rezeptor: Blockade hemmt die Kaliumkanaloffnung in Skelettmuskeln, was die Kontraktionskraft erhoht",
        answerB = "A2A-Rezeptor im Striatum: Blockade steigert die Dopaminwirkung, was Motivation und Schmerztoleranz erhoht",
        answerC = "A2B-Rezeptor in Gefaessen: Blockade foerdert Vasokonstriktion in Gehirngefaessen, was Blutdruck und Durchblutung steigert",
        answerD = "A3-Rezeptor: Blockade hemmt die Histaminausschuettung, was Ermuedungsgefuehl reduziert",
        correctAnswer = 1,
        explanation = "Der A2A-Rezeptor im Striatum ist besonders relevant fuer die psychoaktive und leistungssteigernde Wirkung von Koffein. Seine Blockade erhoht die Dopaminsignalgebung, was Motivation, Stimmung und Schmerzwahrnehmung positiv beeinflusst. Gleichzeitig blockiert Koffein A1-Rezeptoren, was die Inhibition von Neurotransmitterfreisetzung aufhebt und die neuronale Erregung steigert — beide Mechanismen tragen zur Leistungssteigerung bei.",
        difficulty = 5,
        funFact = "Individuell variierende Koffeinwirkung haengt stark vom CYP1A2-Genotyp ab (Enzym fuer Koffeinabbau) und vom ADORA2A-Genotyp (A2A-Rezeptor-Gen). Traeger bestimmter ADORA2A-Varianten profitieren deutlich mehr von Koffein als andere — personalisierte Ernaehrung basierend auf Genotypen koennte kunftig Koffeindosierungen individualisieren."
    ),

    // ── Sportpsychologie (6) ──────────────────────────────────────────────────

    // Question 23 – Sportpsychologie: Choking under Pressure
    Question(
        categoryId = 6,
        questionText = "In der Sportpsychologie werden zwei konkurrierende Theorien zum Phaenomen 'Choking under Pressure' diskutiert. Welche sind das, und welchen unterschiedlichen Mechanismus postulieren sie?",
        answerA = "Arousal-Theorie (Yerkes-Dodson): Zu hohes Erregungsniveau stoert motorische Kontrolle; Appraisal-Theorie: Negative Beurteilung der Situation erzeugt Angst",
        answerB = "Distraction-Theorie: Leistungsrelevante Aufmerksamkeit wird durch druckinduzierte Gedanken abgezogen; Explicit Monitoring-Theorie: Druck fuehrt zu uebersteigerter Bewusstheit automatisierter Bewegungen, was sie stoert",
        answerC = "Catastrophe-Theorie: Plotzlicher Leistungseinbruch bei Uberschreiten der Angst-Erregungsschwelle; Flow-Theorie: Optimaler Leistungszustand wird durch externe Druckfaktoren unterbrochen",
        answerD = "Self-Efficacy-Theorie: Geringes Selbstvertrauen untergabt die Leistung; Ego-Depletion-Theorie: Willensressourcen werden durch Selbstkontrolle erschoepft",
        correctAnswer = 1,
        explanation = "Die zwei Haupttheorien sind die Distraction-Theorie (Beilock & Carr) und die Explicit Monitoring-Theorie (Beilock). Distraction-Theorie: Druck fuehrt zu Sorgen und selbstbezogenen Gedanken, die Arbeitsgedaechtnis und Aufmerksamkeit von der Aufgabe ablenken. Explicit Monitoring-Theorie: Druck erhoht die Aufmerksamkeit auf automatisierte Prozesse (wie eine Golfschwung-Mechanik), was hochtrainierte Bewegungsautomatismen stoert und Leistungsabfall verursacht.",
        difficulty = 5,
        funFact = "Forschungen von Beilock et al. zeigten, dass Experten (Golfer, Fussballer) mehr unter Choking leiden als Anfaenger, weil ihre Bewegungen staerker automatisiert sind. Ein Trick: Experten sollen 'Dum-dum-dum'-Rhythmen sprechen oder irrelevante Melodien summen, um explizites Monitoring zu unterdruecken — eine kontraintuitiv effektive Methode."
    ),

    // Question 24 – Sportpsychologie: Mental-Toughness-Konstrukt
    Question(
        categoryId = 6,
        questionText = "Das 4C-Modell der Mental Toughness nach Clough et al. (2002) unterscheidet vier Komponenten. Welche sind das, und welche fuenfte Komponente wurde in spateren Modellen ergaenzt?",
        answerA = "Control, Commitment, Challenge, Confidence — spaeter ergaenzt um Composure (Selbstregulation unter Druck)",
        answerB = "Control, Commitment, Challenge, Confidence — spaeter ergaenzt um Coping (aktive Stressbewaeltigung)",
        answerC = "Courage, Consistency, Concentration, Character — spaeter ergaenzt um Competitiveness",
        answerD = "Calmness, Concentration, Courage, Commitment — spaeter ergaenzt um Confidence",
        correctAnswer = 0,
        explanation = "Cloughs MTQ48-Modell unterscheidet Control (Gefuehl der Kontrolle ueber Leben und Emotionen), Commitment (Zielstrebigkeit und Ausdauer), Challenge (Herausforderungen als Chance sehen) und Confidence (Selbstvertrauen in Faehigkeiten und zwischenmenschlich). Neuere Forschungen (u.a. Gucciardi, 2017) ergaenzen Composure als fuenfte Komponente — die Faehigkeit, unter extremem Druck emotionale Stabilitaet zu bewahren.",
        difficulty = 5,
        funFact = "Das MTQ48 (Mental Toughness Questionnaire 48) ist das meistgenutzte standardisierte Instrument zur Messung mentaler Staerke im Sport. Kritiker bemangeln jedoch die Trennschaerfe zum Konstrukt 'Resilience' — die Frage, ob Mental Toughness ein eigenstaendiges Konstrukt oder ein Teilaspekt von Resilienz und psychologischem Kapital ist, wird wissenschaftlich noch diskutiert."
    ),

    // Question 25 – Sportpsychologie: Flow-Zustand
    Question(
        categoryId = 6,
        questionText = "Mihaly Csikszentmihalyi beschrieb neun Dimensionen des Flow-Zustands. Welche ist fuer Hochleistungssportler die schwierigste zu erreichen, und welcher psychologische Mechanismus erklaert dies?",
        answerA = "Verlust des Selbstbewusstseins: schwierig weil Hochleistungssportler durch jahrelanges Selbst-Monitoring hypervigilant gegenueber eigenen Leistungen geworden sind",
        answerB = "Handlungs-Bewusstseins-Verschmelzung: schwierig weil hochautomatisierte Bewegungen bei Steigerung des Bewusstseins kollabieren (Explicit Monitoring)",
        answerC = "Zeitverzerrung: schwierig weil Sportler durch Taktik und Spielstand staendig zeitbewusst bleiben muessen",
        answerD = "Paradoxe der Kontrolle: schwierig weil echte Kontrolle im Wettkampf oft nicht moeglich ist und das Gefuehl illusorisch bleibt",
        correctAnswer = 0,
        explanation = "Der Verlust des Selbstbewusstseins (Loss of Self-Consciousness) gilt als besonders schwer erreichbar fuer Hochleistungssportler. Jahrelanges Training durch Videoanalyse, Coaches und Medienaufmerksamkeit foerdert hypervigilantes Selbst-Monitoring. Im Wettkampf fuehrt dieser Mechanismus dazu, dass Sportler sich selbst beobachten, anstatt vollstaendig in der Handlung aufzugehen — was Flow paradoxerweise verhindert.",
        difficulty = 5,
        funFact = "Susan Jackson interviewte Olympiasieger und entdeckte, dass Flow bei Athleten haeufig unbeabsichtigt auftritt und kaum kontrollierbar ist — was paradox klingt, da Flow-Induktions-Techniken (Routine, Entspannung, Fokus) trotzdem die Wahrscheinlichkeit erhoehen. Das Paradox des Flow: Je mehr man ihn anstrebt, desto unwahrscheinlicher wird er."
    ),

    // Question 26 – Sportpsychologie: Attribution nach Weiner
    Question(
        categoryId = 6,
        questionText = "Bernard Weiners Attributionstheorie unterscheidet Ursachenzuschreibungen nach zwei Dimensionen. Wie heissen diese, und welche Kombinationen sind fuer Sportler langfristig am guenstigsten beziehungsweise unguestigsten?",
        answerA = "Internal/External und Stabil/Instabil — guenstigst: interne-instabile Attribution bei Niederlagen; unguestigst: externe-stabile Attribution bei Erfolgen",
        answerB = "Internal/External und Kontrollierbar/Unkontrollierbar — guenstigst: internal-kontrollierbare Attribution bei Niederlagen; unguestigst: external-unkontrollierbare Attribution bei Niederlagen",
        answerC = "Dispositional/Situational und Stabil/Instabil — guenstigst: situational-instabile bei Niederlagen; unguestigst: dispositional-stabile bei Niederlagen",
        answerD = "Self-serving/Other-serving und Stabil/Instabil — guenstigst: other-serving-stabile bei Siegen; unguestigst: self-serving-stabile bei Niederlagen",
        correctAnswer = 0,
        explanation = "Weiners zwei-dimensionales Modell unterscheidet Lokalitaet (internal vs. external) und Stabilitaet (stabil vs. instabil). Fuer Sportler ist die guenstigste Attribution bei Niederlagen 'internal-instabil' (z.B. 'Ich habe heute nicht optimal trainiert' — veraenderbar!), was Motivation foerdert. Unguestigst ist 'external-stabil' bei Niederlagen (z.B. 'Der Gegner ist immer besser') oder bei Erfolgen (z.B. 'Ich hatte Glueck'), was weder Lernen noch Selbstwirksamkeit foerdert.",
        difficulty = 5,
        funFact = "Spaetere Erweiterungen fuegten eine dritte Dimension hinzu: Kontrollierbarkeit. Die Kombination internal-instabil-kontrollierbar bei Misserfolgen ('Ich habe nicht genuegend trainiert, das kann ich aendern') korreliert am staerksten mit adaptiver Motivation und langfristigem Leistungswachstum."
    ),

    // Question 27 – Sportpsychologie: Selbstwirksamkeit nach Bandura
    Question(
        categoryId = 6,
        questionText = "Albert Bandura identifizierte vier Quellen der Selbstwirksamkeitserwartung. Welche ist laut Forschung die wirksamste Quelle im Hochleistungssport, und warum?",
        answerA = "Verbale Ueberzeugung (Ermutigung durch Coaches): wirksamste Quelle, weil Coaches die groesste Autoritaet im Sport haben",
        answerB = "Physiologische und affektive Zustande (Koerpergefuehl): wirksamste Quelle, weil Athleten ihre Leistungsbereitschaft direkt spueren",
        answerC = "Mastery Experience (eigene Erfolgserlebnisse): wirksamste Quelle, weil direktes, eigenes Erleben die staerkste und dauerhafteste Ueberzeugung erzeugt",
        answerD = "Stellvertretendes Erleben (Vorbild-Beobachtung): wirksamste Quelle bei Experten, weil Beobachtung von Gleichaltrigen die realistischste Leistungseinschaetzung liefert",
        correctAnswer = 2,
        explanation = "Bandura bezeichnete Mastery Experiences (eigene Erfolgserlebnisse) als staerkste Quelle der Selbstwirksamkeit, da eigene Leistungsnachweise authentisch und unveraenderlich sind. Im Hochleistungssport wird dies durch gestufte Erfolgserlebnisse im Training genutzt: Athleten werden zunaechst in leichteren Situationen an den Erfolg herangefuehrt, bevor die Schwierigkeit eskaliert wird.",
        difficulty = 5,
        funFact = "Forschungen zur Selbstwirksamkeit im Sport zeigen, dass verbale Ueberzeugung durch Coaches zwar wichtig ist, aber allein instabile Selbstwirksamkeit erzeugt. Ein Athlet, der nur durch Coaching-Lob motiviert wird, ist verletzbar gegenueber kritischem Feedback — daher ist die Kombination aller vier Quellen optimal."
    ),

    // Question 28 – Sportpsychologie: Mentales Training / PETTLEP
    Question(
        categoryId = 6,
        questionText = "Das PETTLEP-Modell des motorischen Imagery (Vorstellungstraining) nennt sieben Schlueselaspekte fuer effektives mentales Training. Was bedeutet das Akronym?",
        answerA = "Position, Environment, Task, Timing, Learning, Emotion, Perspective",
        answerB = "Physical, Environmental, Temporal, Task, Learning, Emotion, Perspective",
        answerC = "Physical, Effort, Tension, Timing, Learning, Emotion, Performance",
        answerD = "Position, Environment, Timing, Tension, Learner, Expectation, Perspective",
        correctAnswer = 1,
        explanation = "PETTLEP steht fuer: Physical (koerperliche Position waehrend der Vorstellung), Environmental (reale Umgebungsreize nutzen), Temporal (Vorstellung in Echtzeit-Geschwindigkeit), Task (sportartspezifisch und aufgabenrelevant), Learning (Vorstellungsinhalt an Lernstand anpassen), Emotion (emotionale Beteiligung einschliessen), Perspective (interne oder externe Perspektive). Das Modell von Holmes & Collins (2001) basiert auf neurowissenschaftlichen Erkenntnissen zur funktionellen Aequivalenz von Vorstellung und realer Ausfuehrung.",
        difficulty = 5,
        funFact = "fMRI-Studien zeigen, dass mentales Training aehnliche Bereiche des motorischen Kortex aktiviert wie reale Bewegungsausfuehrung. Pianisten, die Stuecke nur mental uebt (ohne Klaviertasten zu beruehren), zeigen nachweisbare kortikale Reorganisation — ein Beweis fuer die Neuroplastizitaet durch Imagination."
    ),

    // ── Sportrecht & CAS-Urteile (5) ─────────────────────────────────────────

    // Question 29 – Sportrecht: CAS-Verfahren
    Question(
        categoryId = 6,
        questionText = "Der Court of Arbitration for Sport (CAS) mit Sitz in Lausanne ist das hoechste Sportschiedsgericht. Welches Urteil des Schweizer Bundesgerichts aus dem Jahr 2003 festigte endgueltig die rechtliche Bindungswirkung von CAS-Schiedsklauseln in Sportregelwerken?",
        answerA = "Urteil im Fall Elmar Gundel vs. FEI: Erstmals wurde CAS-Schiedsgerichtsbarkeit als rechtmaessig anerkannt",
        answerB = "Urteil im Fall Lazutina/Danilova vs. IOC: Das Schweizer Bundesgericht anerkannte die Unabhaengigkeit des CAS nach strukturellen Reformen von 1994",
        answerC = "Urteil im Fall Ben Johnson vs. IAAF: Erstmals wurde Dopingsperre durch CAS-Urteil in staatliches Recht uebertragen",
        answerD = "Urteil im Fall Pistorius vs. IAAF: CAS hob das Startverbot fuer Prothesen-Laeufer auf und schuf Praezedenz fuer Diskriminierungsschutz",
        correctAnswer = 1,
        explanation = "Das Schweizer Bundesgericht bestaetgte in seinem Urteil (Lazutina vs. CIO, BGE 129 III 445) 2003 die Unabhaengigkeit des CAS nach den strukturellen Reformen von 1994, die das ICAS (Internationaler Rat fuer Sportschiedsgerichtsbarkeit) einfuehrten. Damit galt der CAS als ausreichend unabhaengig von der Sportorganisationen (IOC), um als legitimes Schiedsgericht anerkannt zu werden. Dieses Urteil ist ein Grundstein fuer die Rechtssicherheit im internationalen Sportrecht.",
        difficulty = 5,
        funFact = "Das erste CAS-Urteil ueberhaupt (1984) betraf einen Ruderstreit bei den Olympischen Spielen. Es dauerte bis 1994 — nach erheblicher Kritik wegen IOC-Naehe — bis der CAS durch strukturelle Reformen tatsaechlich unabhaengig wurde und sein Schiedssprueche internationales Vertrauen genossen."
    ),

    // Question 30 – Sportrecht: WADA-Code
    Question(
        categoryId = 6,
        questionText = "Welches Prinzip im WADA-Code 2021 regelt, dass eine Anti-Doping-Regelverlaetzung auch dann vorliegt, wenn eine verbotene Substanz nachgewiesen wird, auch wenn der Athlet keine Schuld nachweislich hat, und wie heisst das entsprechende Rechtsprinzip?",
        answerA = "In dubio pro reo (Im Zweifel fuer den Angeklagten): Gilt auch im WADA-Code und entlastet bei nachgewiesener Schuldlosigkeit vollstaendig",
        answerB = "Strict Liability (Gefaehrdungshaftung): Der Nachweis der verbotenen Substanz genuegt fuer eine Regelverlaetzung; Schuldlosigkeit kann nur die Strafe reduzieren, nicht den Befund widerlegen",
        answerC = "No Fault or Negligence Principle: Nachgewiesene Schuldlosigkeit fuehrt automatisch zur Nullsperre",
        answerD = "Reverse Burden of Proof: Der Athlet muss aktiv seine Unschuld beweisen, kann aber bei Erfolg vollstaendig freigesprochen werden",
        correctAnswer = 1,
        explanation = "Das Strict Liability-Prinzip ist das Kernprinzip im WADA-Code: Allein der analytische Nachweis einer verbotenen Substanz in der A-Probe genuegt, um eine Anti-Doping-Regelverlaetzung festzustellen. Die Frage der individuellen Schuld oder Fahrlassigkeit ist nur fuer die Strafbemessung relevant — voellige Schuldlosigkeit kann die Sperre auf null reduzieren (No Fault or Negligence-Ausnahme), aber die Regelverlaetzung selbst bestehen lassen.",
        difficulty = 5,
        funFact = "Maria Scharapowa erhielt 2016 eine 15-monatige Sperre (reduziert von urspruenglich 2 Jahren) unter dem Strict-Liability-Prinzip fuer Meldonium, das kurz zuvor auf die Verbotsliste gesetzt wurde. Das CAS anerkannte zwar keinen Betrug, aber die Regelverlaetzung blieb bestehen — ein klassischer Strict-Liability-Fall."
    ),

    // Question 31 – Sportrecht: Transfersystem FIFA
    Question(
        categoryId = 6,
        questionText = "Das Bosman-Urteil des Europaeischen Gerichtshofs (EuGH, 1995) veraenderte das Fussballer-Transfersystem fundamental. Was genau urteilte der EuGH, und welche zwei Aspekte des Transfersystems wurden fuer europarechtswidrig erklaert?",
        answerA = "Transfergebuhren fuer Vertragsspieler verstossen gegen EU-Kartellrecht; nationale Spielerbeschraenkungen pro Team verstossen gegen den freien Dienstleistungsverkehr",
        answerB = "Abloesegebuehren fuer Spieler nach Vertragsende verstossen gegen Arbeitnehmerfreizuegigkeit; nationale Quoten-Regelungen (Auslaenderbeschraenkungen in Vereinsmannschaften) verstossen gegen Diskriminierungsverbot",
        answerC = "Langzeitvertraege ueber 5 Jahre sind unzulaessig; Transferfenster-Beschraenkungen verletzen das Recht auf freie Berufswahl",
        answerD = "FIFA-Transferregulierungen gelten nicht fuer EU-Buerger; nationale Verbandssatzungen haben Vorrang vor FIFA-Statuten im EU-Raum",
        correctAnswer = 1,
        explanation = "Der EuGH erklaerte in C-415/93 (Union Royale Belge vs. Bosman) zwei Aspekte fuer europarechtswidrig: Erstens, Transfergebuehren fuer Spieler nach Vertragsende verletzen die Arbeitnehmerfreizuegigkeit (Art. 45 AEUV), da sie den Vereinswechsel faktisch verhinderten. Zweitens, nationale Quoten (Auslaenderregeln in Ligaspielen) verstoessen gegen das Diskriminierungsverbot. Seitdem koennen EU-Spieler nach Vertragsende transferfrei wechseln.",
        difficulty = 5,
        funFact = "Jean-Marc Bosman, der belgische Fussballspieler, fuer den das Urteil benannt ist, klagte gegen RFC Luttich wegen einer Transfersperrung trotz abgelaufenem Vertrag. Ironie des Schicksals: Bosman selbst verdiente an seiner Namensgebung kaum etwas — er erhielt eine geringe Entschaedigung und starb 2023 in finanziell bescheidenen Verhaeltnissen."
    ),

    // Question 32 – Sportrecht: CAS-Urteil Russland/WADA
    Question(
        categoryId = 6,
        questionText = "Das CAS-Urteil vom Dezember 2020 im Fall WADA vs. RUSADA reduzierte die urspruenglich von WADA geforderte vierjahrige Sperre Russlands auf zwei Jahre. Welche Hauptbegrue ndung fuehrte das CAS fuer die Reduzierung an?",
        answerA = "Verjaehrung: Teile der Vorwuerfe lagen laenger als vier Jahre zurueck und waren daher nicht mehr sanktionierbar",
        answerB = "Verhaltensmaessige Kooperation: RUSADA hatte nach 2018 ausreichend mit WADA kooperiert, was eine Milderung rechtfertigte",
        answerC = "Beweisstandard: Ein Teil der Manipulationsvorwuerfe war nicht auf dem erforderlichen Beweisniveau ('Comfortable Satisfaction') belegt; Russland war nur fuer belegte Verstatosse, nicht fuer alle behaupteten Manipulationen zu sanktionieren",
        answerD = "Proportionality principle: Eine Vierjahressperre war verhaeltnismaessig zu lang fuer die als bewiesen angesehenen Versttosse",
        correctAnswer = 2,
        explanation = "Das CAS bestaettigte die Grundvorwuerfe (Staatsdoping, LIMS-Daten-Manipulation), stellte aber fest, dass nur ein Teil der Anschuldigungen das fuer Sportschiedsgerichtsbarkeit erforderliche Beweisstandard 'Comfortable Satisfaction' (etwas unter dem zivilrechtlichen Standard 'Balance of Probabilities', aber ueber 'reasonable doubt') erfuellte. Fuer nicht belegte Einzelvorwuerfe konnte keine Sanktion ausgesprochen werden, was die Gesamtstrafe auf zwei Jahre reduzierte.",
        difficulty = 5,
        funFact = "Als Ergebnis trat Russland bei den Olympischen Spielen Tokio 2021 und Peking 2022 unter dem Kuerzel 'ROC' (Russian Olympic Committee) an — ohne Nationalflagge und ohne Nationalhymne. Diese Form der symbolischen, aber nicht vollstaendigen Sanktion ist ein Kompromiss, den viele Dopingbekaempfer als unzureichend kritisierten."
    ),

    // Question 33 – Sportrecht: Geschlechtsidentitaet im Sport
    Question(
        categoryId = 6,
        questionText = "Welche Richtlinien-Geschichte beschreibt die Entwicklung des IOC bezueglich Athleten mit Unterschieden in der Geschlechtsentwicklung (DSD) und Transgender-Athleten von 2004 bis zum IOC Framework 2021?",
        answerA = "2004 Stockholm Konsensus: Chirurgische Geschlechtsanpassung + 2 Jahre Hormontherapie erforderlich. 2015 IOC-Richtlinie: Kein Operationserfordernis mehr; Testosterongrenzwert 10 nmol/L. 2021 IOC Framework: Principles-based approach, kein universeller Grenzwert, sportartspezifische Regelungen",
        answerB = "2004: Biologisches Geschlecht entscheidend; 2015: Selbstidentifikation genuegt; 2021: Medizinischer Nachweis erforderlich",
        answerC = "2004: Nur geborene Frauen durften an Frauenwettbewerben teilnehmen; 2015: Transgender-Frauen nach Hormontherapie erlaubt; 2021: Vollstaendige Inklusion ohne Beschraenkungen",
        answerD = "2004-2021: Keine Aenderung der Grundsatzpolitik; Variationen nur in nationalen Sportverbandssatzungen",
        correctAnswer = 0,
        explanation = "Die IOC-Politik durchlief drei Phasen: 2004 (Stockholm Konsensus) verlangte chirurgische Geschlechtsanpassung und 2-jaehrige Hormontherapie. 2015 entfiel das Operationserfordernis; Transgender-Frauen mussten Testosteron unter 10 nmol/L halten. Das 2021 IOC Framework ersetzte Grenzwerte durch Prinzipien (Inklusion, Fairness, Schadensminimierung) und delegierte sportartspezifische Regelungen an die Weltfachnerverbaende.",
        difficulty = 5,
        funFact = "World Athletics (IAAF) setzte 2023 einen strengeren Testosteron-Grenzwert von 2,5 nmol/L fuer transgender Frauen im internationalen Leistungssport und schloss Athleten mit DSD (wie Caster Semenya) von 400-m-bis-1-Meile-Distanzen aus, sofern sie ihren natuerlicherweise hohen Testosteronspiegel nicht medikamentoes senken. Semenya klagte — bislang ohne dauerhaften Erfolg."
    ),

    // ── Statistik-Rekorde & olympische Kuriositaeten (8) ─────────────────────

    // Question 34 – Statistik: Consecutive Grand Slams
    Question(
        categoryId = 6,
        questionText = "Welcher Tennisspieler haelt den Rekord fuer die meisten aufeinanderfolgenden Grand-Slam-Halbfinalteilnahmen, und wie viele waren es?",
        answerA = "Roger Federer mit 36 aufeinanderfolgenden Grand-Slam-Halbfinalteilnahmen (2004-2013)",
        answerB = "Novak Djokovic mit 30 aufeinanderfolgenden Grand-Slam-Halbfinalteilnahmen (2010-2018)",
        answerC = "Rafael Nadal mit 26 aufeinanderfolgenden Grand-Slam-Halbfinalteilnahmen (2006-2012)",
        answerD = "Jimmy Connors mit 20 aufeinanderfolgenden Grand-Slam-Halbfinalteilnahmen (1974-1980)",
        correctAnswer = 0,
        explanation = "Roger Federer erreichte von Wimbledon 2004 bis zu den US Open 2013 sagenhafte 36 aufeinanderfolgende Grand-Slam-Halbfinalteilnahmen — ein Rekord der im Maennertennis als wohl unschlagbar gilt. Dieser erstreckte sich ueber fast eine Dekade und 9 Spieljahre, was die aussergewoehnliche Konsistenz Federers auf dem hoechsten Niveau dokumentiert.",
        difficulty = 5,
        funFact = "Federers 36 consecutive Grand Slam Semi-Finals spannt eine Periode von Februar 2004 bis September 2013. In dieser Zeit gewann er 11 der 36 Turniere. Der einzige Spieler der Aera, der konsistent in diese Semi-Finals einzog, war er selbst — Djokovic und Nadal konnten diese Konstanz nie ueber eine vergleichbare Zeitspanne halten."
    ),

    // Question 35 – Statistik: NBA-Rekorde
    Question(
        categoryId = 6,
        questionText = "Welcher NBA-Spieler erzielte in einer einzigen Saison die hoehere durchschnittliche Punktzahl als Wilt Chamberlain in seiner Rekord-Saison 1961/62, in der er 50,4 Punkte pro Spiel erzielte?",
        answerA = "Michael Jordan in der Saison 1986/87 mit 37,1 Punkten pro Spiel",
        answerB = "Kein Spieler — Chamberlains 50,4 PPG-Durchschnitt ist bis heute der hoechste NBA-Saisonschnitt aller Zeiten",
        answerC = "Elgin Baylor in der Saison 1961/62 mit 38,3 Punkten pro Spiel",
        answerD = "Jerry West in der Saison 1965/66 mit 31,3 Punkten pro Spiel, kombiniert mit 9,7 Assists",
        correctAnswer = 1,
        explanation = "Wilt Chamberlains 50,4 Punkte pro Spiel in der Saison 1961/62 ist bis heute der hoechste NBA-Saisonschnitt aller Zeiten und gilt als einer der unschlagbarsten Rekorde des Sports. In dieser Saison erzielte er auch seinen beruehmen 100-Punkte-Spiel (2. Maerz 1962 gegen die New York Knicks). Kein Spieler hat seither auch nur annaehernd 50 PPG fuer eine ganze Saison erreicht.",
        difficulty = 5,
        funFact = "In der Saison 1961/62 bestritt Chamberlain alle 80 regulaeren Saisonspiele, ohne auch nur eine einzige Minute wegen Foulingproblemen ausgewechselt zu werden — denn er wurde in dieser Saison kein einziges Mal in Foul-Trouble disqualifiziert. Das allein ist schon eine aussergewoehnliche Leistung fuer einen Center-Spieler."
    ),

    // Question 36 – Olympische Kuriositaeten: Marathon 1904
    Question(
        categoryId = 6,
        questionText = "Der Marathon bei den Olympischen Spielen 1904 in St. Louis gilt als kuriositaetenstes Rennen der olympischen Geschichte. Was geschah mit dem 'Erstplatzierten' Fred Lorz, und warum?",
        answerA = "Lorz wurde wegen Dopingmissbrauchs disqualifiziert; er hatte Strychnin eingenommen, das damals als Stimulans verwendet wurde",
        answerB = "Lorz fuhr den groessten Teil der Strecke im Auto mit, stieg kurz vor dem Ziel aus und ueberquerte als Erster die Ziellinie; er wurde disqualifiziert und Thomas Hicks zum Sieger erklaert",
        answerC = "Lorz wurde disqualifiziert, weil er die falsche Route lief und dadurch eine Abkuerzung nahm, die ihn 3 km weniger laufen liess",
        answerD = "Lorz wurde disqualifiziert, weil er nicht US-amerikanischer Staatsbuerger war und damit nach den damaligen IOC-Regeln nicht startberechtigt gewesen waere",
        correctAnswer = 1,
        explanation = "Fred Lorz fuhr ab Meile 9 den Rest der 40 km im Auto seines Managers mit, stieg kurz vor dem Ziel aus und lief als scheinbarer Erstplatzierter ins Stadion. Nach seiner Disqualifikation wurde Thomas Hicks zum Sieger erklaert — der selbst bereits Strychnin und Brandy als Stimulans erhalten hatte. Das Rennen 1904 gilt als bizarrster Marathon der olympischen Geschichte.",
        difficulty = 5,
        funFact = "Der kubanische Athlet Felix Carvajal nahm ohne Qualifikation am Marathon teil, nachdem er per Anhalter von Havanna nach New Orleans gereist war und dabei sein Geld verspielte. Er lief im Alltag-Outfit, aass unterwegs Aepfel von einem Obststand und belegte trotzdem Platz 4 — einige der gestolenen Aepfel waren jedoch verdorben und verursachten ihm Magenkrampfe, die ihn seinen verdienten Podiumsplatz kosteten."
    ),

    // Question 37 – Olympische Kuriositaeten: Einzige Nation Gold
    Question(
        categoryId = 6,
        questionText = "Bei welchen Olympischen Sommerspielen gewann eine einzige Nation alle Goldmedaillen in der Leichtathletik, und welche Nation war das?",
        answerA = "USA bei den Spielen 1932 in Los Angeles",
        answerB = "Keine Nation hat jemals alle Leichtathletik-Goldmedaillen bei Olympischen Spielen gewonnen",
        answerC = "USA bei den Spielen 1904 in St. Louis (aufgrund des geringen internationalen Teilnehmerfelds)",
        answerD = "Sowjetunion bei den Spielen 1980 in Moskau nach dem Boykott westlicher Nationen",
        correctAnswer = 2,
        explanation = "Bei den Olympischen Spielen 1904 in St. Louis gewannen die USA faktisch nahezu alle Goldmedaillen in der Leichtathletik — das Ereignis war so schlecht international besucht, dass viele 'Weltspitzen'-Athleten gar nicht antraten. Nur wenige auslaendische Athleten nahmen teil, und die USA dominierten das Medaillenspiegel derart, dass internationale Konkurrenz kaum stattfand. Es war der Tiefpunkt des olympischen Gedankens in der Fruehzeit.",
        difficulty = 5,
        funFact = "1904 war auch das Jahr, in dem die 'Anthropology Days' stattfanden — ein skandaloeses Parallelereignis, bei dem indigene Voelker ('Wilden') in angestammten Sportarten vorhaendig gemacht wurden, um ihre Unterlegenheit gegenueber 'zivilisierten' Sportlern zu demonstrieren. Pierre de Coubertin nannte das Ereignis spaeter schaemend ein 'abartiges Treiben'."
    ),

    // Question 38 – Statistik: Dauerrekord Gewichtheben
    Question(
        categoryId = 6,
        questionText = "Naim Suleymanoglu ('Pocket Hercules') gilt als einer der aussergewoehnlichsten Gewichtheber aller Zeiten. Was ist das praezise Verhaeltnis, das seinen Legendenstatus unterstreicht, und welchen Dreifach-Weltrekord hielt er bei den Olympischen Spielen 1988?",
        answerA = "Er hob das 4-Fache seines eigenen Koerpergewichts; Dreifach-Weltrekord in Reissen, Stossen und Zweikampf in der 60-kg-Klasse",
        answerB = "Er hob das 3,5-Fache seines Koerpergewichts; erste Person der Geschichte, die alle drei olympischen Gewichte (Reissen, Stossen, Zweikampf) in einer Olympischen Klasse gleichzeitig als Weltrekord haelt",
        answerC = "Er hob das 3-Fache seines Koerpergewichts; Weltrekord in der Koerpergewicht-zu-Hebeleistung-Relation uebertraf alle anderen Sportler aller Epochen",
        answerD = "Er hob das 2,5-Fache seines Koerpergewichts; Weltrekord durch Kombination aus extrem kleiner Koerpermasse (56 kg) und hoher absoluter Hebelleistung",
        correctAnswer = 0,
        explanation = "Naim Suleymanoglu hob in Seoul 1988 das Vierfache seines Koerpergewichts (bei 60 kg KG hob er 240 kg Zweikampf) und stellte gleichzeitig Weltrekorde in Reissen, Stossen und Zweikampf auf. Er ist der einzige Gewichtheber, der drei aufeinanderfolgende olympische Goldmedaillen in derselben Kategorie gewann (1988, 1992, 1996) und gilt als bester Pound-for-Pound-Heber der Geschichte.",
        difficulty = 5,
        funFact = "Suleymanoglu wurde als Naim Suleimanov in Bulgarien geboren und ist muslimischer Herkunft. Als Bulgarien seiner ethnischen Tuerkischen Minderheit 1986 bulgarische Namen aufzwang, floh er mit Hilfe der tuerkischen Regierung — die Tuerkei soll Bulgarien 1 Million US-Dollar gezahlt haben, um ihn als Sportler zu uebernehmen. Danach startete er fuer die Tuerkei und wurde zur Nationalikone."
    ),

    // Question 39 – Statistik: Fussballer-Minuten-Rekorde
    Question(
        categoryId = 6,
        questionText = "Welcher Torwart haelt den Weltrekord fuer die laengste Zeit ohne Gegentor in einem internationalen Pflichtspiel-Wettbewerb, und wie viele Minuten umfasst dieser Rekord?",
        answerA = "Iker Casillas (Spanien): 1.024 Minuten waehrend der Weltmeisterschaft 2010 und der EM 2012",
        answerB = "Gianluigi Buffon (Italien): 974 Minuten in der Serie A zwischen 2015 und 2016",
        answerC = "Lev Yashin (Sowjetunion): 1.142 Minuten in internationalen Spielen der 1960er-Jahre",
        answerD = "Emiliano Martinez (Argentinien): 2021-2022 erzielte er eine Zeichenserie von 1.172 Minuten in Copa America und WM-Qualifikation",
        correctAnswer = 0,
        explanation = "Iker Casillas stellte mit Spanien einen Rekord von 1.024 aufeinanderfolgenden Minuten ohne Gegentor auf, der die EM 2008, die WM 2010 (mit 7 von 7 Spielen zu null) und die EM 2012 umspannte. Diese Serie ist die laengste dokumentierte Torhuerter-Negativ-Serie in offiziellen FIFA/UEFA-Wettbewerben. Spanien erzielte in dieser Phase das Triple (EM 2008 - WM 2010 - EM 2012).",
        difficulty = 5,
        funFact = "Spaniens 'Tiki-Taka'-System unter Luis Aragones (EM 2008) und Vicente del Bosque (WM 2010, EM 2012) war so ballbesitzdominant, dass Gegner schlicht kaum zum Abschluss kamen. Das System revolutionierte den Fussball weltweit — Klubs wie Barcelona und Bayern Muenchen uebernahmen die Prinzipien, und Casillas profitierte von einem nahezu impermeablen Mannschaftssystem."
    ),

    // Question 40 – Statistik: Sprinter-Zeitreduktion
    Question(
        categoryId = 6,
        questionText = "Um wieviel Millisekunden wurde der 100-m-Weltrekord der Maenner von 1968 (Jim Hines, 9,95 s) bis 2009 (Usain Bolt, 9,58 s) in 41 Jahren verbessert, und wie verhaelt sich das zur theoretischen biomechanischen Leistungsgrenze?",
        answerA = "370 ms Verbesserung; laut biomechanischen Modellen betraegt die absolute Untergrenze etwa 9,40-9,48 s, Bolt ist also bereits sehr nah an der menschlichen Grenze",
        answerB = "270 ms Verbesserung; die theoretische Untergrenze liegt bei etwa 8,80-9,00 s, es gibt also noch erhebliches Verbesserungspotenzial",
        answerC = "170 ms Verbesserung; die theoretische Untergrenze liegt bei genau 9,48 s, die Bolt 2009 bereits unterschritten hat",
        answerD = "470 ms Verbesserung; da Bolt den Weltrekord auch ohne optimalen Start hatte, waere 9,44 s ohne Fehlstart theoretisch moeglich",
        correctAnswer = 0,
        explanation = "Hines' 9,95 s (1968) minus Bolts 9,58 s (2009) ergibt 370 ms Verbesserung in 41 Jahren. Biomechanische Modelle von Weyand, Bundle und anderen setzen die menschliche Leistungsgrenze bei ca. 9,40-9,48 s an, basierend auf maximaler Schrittkraft, Frequenz und Bodenreaktionskraftlimiten. Bolt bei 9,58 s liegt damit bereits sehr nah an den theoretischen Grenzen — weitere signifikante Verbesserungen sind biologisch kaum moeglich.",
        difficulty = 5,
        funFact = "Bolts 9,58-s-Lauf bei der WM 2009 in Berlin beinhaltet eine Phase von 60-80 m, in der er eine Durchschnittsgeschwindigkeit von 12,4 m/s (44,72 km/h) erreichte. Die Spitzengeschwindigkeit bei ca. 65 m betrug 12,42 m/s — das ist die dokumentierte Hoechstgeschwindigkeit im Sprint-Wettkampf, gemessen mit Hochfrequenz-GPS und Radar-Systemen."
    ),

    // ── Sport-Technologie (7) ─────────────────────────────────────────────────

    // Question 41 – Sport-Technologie: Hawk-Eye-System
    Question(
        categoryId = 6,
        questionText = "Das Hawk-Eye-Ballverfolgungssystem wurde 2001 von Paul Hawkins in England patentiert und erstmals im Cricket eingesetzt. Welche mathematische Methode liegt der Trajektorien-Berechnung zugrunde, und wie gross ist der dokumentierte mittlere Fehler?",
        answerA = "Fourier-Transformation zur Frequenzanalyse der Kamerasignale; mittlerer Fehler unter 1 mm",
        answerB = "Triangulation mit min. 6 hochfrequenten Kameras (bis 340 fps) und 3D-Kurvenanpassung mittels Bayes-Filter; mittlerer Fehler 3,6 mm laut unabhaengigen Tests",
        answerC = "Laser-Interferometrie kombiniert mit Hochgeschwindigkeitskameras; mittlerer Fehler unter 0,5 mm",
        answerD = "Radar-Doppler-Technik kombiniert mit KI-basierter Bildverarbeitung; mittlerer Fehler 5-10 mm",
        correctAnswer = 1,
        explanation = "Hawk-Eye nutzt typischerweise 6-10 hochfrequente Kameras (bis 340 fps), die das Objekt aus verschiedenen Winkeln erfassen. Triangulationsalgorithmen berechnen die 3D-Position in jedem Frame, und ein Bayes-Filter (Kalman-Filter-Variante) glaettet die Trajektorie und extrapoliert bei verdeckten Frames. Unabhaengige Tests (z.B. ITF-Studie im Tennis) dokumentieren einen mittleren Fehler von 3,6 mm — gut genug fuer Linienentscheidungen, aber nicht perfekt.",
        difficulty = 5,
        funFact = "Im Cricket wird Hawk-Eye auch fuer LBW (Leg Before Wicket)-Entscheidungen genutzt, obwohl die Balltrajektorie nach dem Koerpertreffer des Batsmen extrapoliert (vorhergesagt) wird — nicht gemessen. Das fuehrt zu Diskussionen, da die extrapolierte Flugbahn nur eine statistische Vorhersage ist, keine physikalische Messung."
    ),

    // Question 42 – Sport-Technologie: VAR-Technik
    Question(
        categoryId = 6,
        questionText = "Das Video Assistant Referee (VAR)-System im Fussball nutzt fuer Abseits-Entscheidungen eine Semi-Automatic Offside Technology (SAOT). Welche technische Innovation wurde bei der WM 2022 in Katar erstmals eingesetzt?",
        answerA = "12 dedizierte Hochgeschwindigkeitskameras (50 fps) mit KI-basierter Koerper-Punkt-Erkennung in 3D, kombiniert mit einem Sensor im Spielball, der Position und Ballkontakt auf 500 Datenpunkte pro Sekunde misst",
        answerB = "Infrarot-Sensoren in den Spielfeldlinien, die Ballkontakt und Abseitsposition durch Bodenvibrationen erkennen",
        answerC = "GPS-Chips in den Spielerschuhen (alle 22 Spieler + Ball) fuer Real-Time-Positionserfassung auf 2 cm genau",
        answerD = "Computer-Vision mit 4K-Drohnen-Kameras, die aus der Vogelperspektive Abseitslinien automatisch einzeichnen",
        correctAnswer = 0,
        explanation = "Die SAOT bei der WM 2022 kombinierte zwei Systeme: 12 dedizierte Hawk-Eye-Kameras mit 50 fps und KI-basierter 3D-Pose-Estimation (29 Koerperpunkte pro Spieler), sowie einen Adidas-Al Rihla-Ball mit eingebettetem inertialen Messsystem (500 Hz), das den exakten Moment des Schusses/Passes und die Ballposition millimeter-genau erfasste. Die KI zeichnete automatisch die Abseits-Linie ohne manuelles Setzen von Linien.",
        difficulty = 5,
        funFact = "Die SAOT reduzierte die durchschnittliche Entscheidungsdauer fuer Abseits-VAR-Checks von 70 Sekunden (WM 2018, manuelle Linienzeichnung) auf unter 24 Sekunden bei der WM 2022. Dies reduzierte Unterbrechungszeiten erheblich — eines der groessten Kritikpunkte am urspruenglichen VAR-System."
    ),

    // Question 43 – Sport-Technologie: Zeitmessung im Sprint
    Question(
        categoryId = 6,
        questionText = "Wie funktioniert die vollautomatische Zeitmessung (FAT) bei olympischen Sprintveranstaltungen, und was ist der technische Unterschied zur manuellen Handzeitnahme in Bezug auf Reaktionszeit und Genauigkeit?",
        answerA = "FAT startet mit dem Startschuss-Signal und stoppt per Lichtschranke; manuelle Zeit startet mit dem Knall (Schall-Verzoegerung) und hat Reaktionszeit-Fehler von 0,1-0,3 s — deshalb wird manuelle Zeit mit +0,24 s korrigiert",
        answerB = "FAT startet mit dem Startschuss und stoppt per Kamera-Frame-Analyse bei Koerperzieleinlauf; manuell hat typisch 0,05 s Reaktionszeit, deshalb wird manuell mit +0,05 s korrigiert",
        answerC = "FAT und manuelle Zeit werden kombiniert: FAT stoppt bei Lichtschranke, Handzeitnahme erfasst den Sieger-Eindruck; der Mittelwert gilt als offizielle Zeit",
        answerD = "FAT basiert auf Doppler-Radar zur kontinuierlichen Geschwindigkeitsmessung; die Zeit ergibt sich durch Integration der Geschwindigkeit ueber die Streckendistanz",
        correctAnswer = 0,
        explanation = "Die vollautomatische Zeitmessung startet synchron mit dem Startschuss (elektrisches Signal) und stoppt durch Kamera-basierte Bildanalyse und/oder Lichtschranke beim Zieleinlauf. Bei manueller Zeitnahme existiert eine Schallverzoegerungs-Latenz (Schall breitet sich mit ~340 m/s aus) plus eine menschliche Reaktionszeit von ca. 150-200 ms. IAAF/World Athletics standardisiert den Korrekturfaktor auf +0,24 s bei Umrechnung manueller in automatische Zeiten.",
        difficulty = 5,
        funFact = "Omega ist seit 1932 offizieller Zeitnehmer der Olympischen Spiele. Bei den Spielen 1948 in London fuehrte Omega das erste Fotofinish-System ein. Heute werden bei Olympia bis zu 10.000 Einzel-Messungen pro Sekunde durchgefuehrt — die offiziellen Zeiten sind auf 1/100 Sekunde genau, koennen aber auf 1/1000 berechnet werden."
    ),

    // Question 44 – Sport-Technologie: Schwimmanzug-Technologie
    Question(
        categoryId = 6,
        questionText = "Die LZR-Racer-Anzuege (Speedo, 2008) fuehrten zum massenhaften Weltrekordbruch und wurden 2010 verboten. Welche spezifischen technischen Merkmale machten diese Anz uege so effektiv, und welche FINA-Regel schraenkte sie seitdem ein?",
        answerA = "Carbon-Fiber-Verstaerkungen im Rumpfbereich; FINA-Regel: Kein Material mit E-Modul >5 GPa erlaubt",
        answerB = "Kompressionskraft (Tiefenkomprimeierung des Koerpers zur Reibungsminimierung), Polyurethan-Paneele (hydrophober als Textil, ermoeglicht Lufteinschluss unter Wasser) und Ultraschallschweissen statt Naehte; FINA-Regel: Anzuege muessen aus Textilem Material sein, maximale Koerperabdeckung eingeschraenkt (Maenner bis Knie, Frauen von Schulter bis Knie)",
        answerC = "Elektrisch geladene Polymere, die Wasserreaktion verringern; FINA-Regel: Keine elektronischen Bestandteile in Wettkampfanzuegen",
        answerD = "Eingebettete Mikro-Auftriebskoerper aus Neopren; FINA-Regel: Schaumstoff und Neopren verboten, nur Fasertextilien erlaubt",
        correctAnswer = 1,
        explanation = "Die LZR-Racer kombinierten drei Schlueeltechnologien: 1) Extreme Kompression des Rumpfes, die Muskeln stabilisiert und Wellenbewegungen reduziert; 2) Polyurethan-Paneele, die hydrophober als Textil sind und Luftblasen einschliessen (Auftriebseffekt); 3) Ultraschall-geschweisste Naehte ohne Reibungs-Naehtpunkte. Die FINA reagierte 2010 mit der Regel, dass Wettkampfanzuege aus Textilem Gewebematerial bestehen muessen, ohne Kunstoff-Paneele.",
        difficulty = 5,
        funFact = "Bei den Olympischen Spielen 2008 in Peking und den WM 2009 wurden 94 % aller Weltrekorde von Sportlern in Polyurethan-Anzuegen aufgestellt. In einem einzigen WM-Jahr (2009) wurden 43 Weltrekorde gebrochen — mehr als in den vorangegangenen 10 Jahren zusammen. Die Wissenschaftler sprechen von 'technologischem Doping'."
    ),

    // Question 45 – Sport-Technologie: GPS-Tracking im Teamsport
    Question(
        categoryId = 6,
        questionText = "GPS-Trackers in Westen (Player Tracking Units) sind heute Standard im Profi-Teamsport. Welches spezifische Bewegungsmetrik-System wurde von Sami Hyypiä bei Liverpool FC mitentwickelt und ist seitdem industriestandard?",
        answerA = "Total Distance, High-Speed Running Distance (>19,8 km/h) und Sprinting Distance (>25,2 km/h) als drei-stufige Geschwindigkeitskategorisierung",
        answerB = "Expected Physical Load (xPL) unter Verwendung von GNSS und inertialem Messeinheit, das Kontakte, Sprints und Belastungsspitzen in einem Score integriert",
        answerC = "Physical Performance Index (PPI): Kombination aus km/h-Zonen, Beschleunigungen, Abbremsungen und Herzfrequenz-Daten",
        answerD = "High-Metabolic Load Distance (HMLD): Strecke, die ueber einem bestimmten metabolischen Power-Schwellenwert (25 W/kg) zurueckgelegt wird, kombiniert aus Geschwindigkeit und Beschleunigung",
        correctAnswer = 3,
        explanation = "High-Metabolic Load Distance (HMLD) ist der moderne Industriestandard, der einfache Geschwindigkeitszonen ersetzt. HMLD berechnet die metabolische Leistung (in W/kg) aus Geschwindigkeit und Beschleunigung/Verzoegerung und summiert die Strecke ueber einem Schwellenwert (typisch 25 W/kg). Dies erfasst auch intensive Aktionen bei niedriger Geschwindigkeit (z.B. explosive Richtungswechsel), die reine Geschwindigkeitsmessung verfehlt.",
        difficulty = 5,
        funFact = "Moderne Player Tracking Units (z.B. Catapult, STATSports) erfassen nicht nur GPS (10 Hz), sondern auch 3-Achsen-Beschleunigung (1000 Hz), Gyroskop und Magnetometer. Bei der WM 2022 hatte FIFA erstmals offizielle Ball-Tracking (500 Hz) kombiniert mit Spieler-Tracking — die kombinierte Datenmenge eines einzigen Spiels ueberstieg 10 GB."
    ),

    // Question 46 – Sport-Technologie: Wind-Legale Messungen
    Question(
        categoryId = 6,
        questionText = "Im Sprint und Sprung muss eine Leistung 'windlegal' sein (Windguenstig max. +2,0 m/s). Welche technische Methode wird fuer die offizielle Windmessung genutzt, und welche wissenschaftliche Kritik wird an der derzeitigen Methode geaeussert?",
        answerA = "Ultraschall-Anemometer misst 10-Sekunden-Mittelwert waehrend des Rennens; Kritik: Messstelle ist 50 m vom Startpunkt entfernt, Windbedingungen koennen entlang der 100-m-Bahn variieren",
        answerB = "Laser-Doppler-Anemometer misst kontinuierlich in Echtzeit entlang der Bahn; Kritik: zu teuer fuer kleinere Veranstaltungen",
        answerC = "Mechanisches Schalenkreuzanemometer wird 10 Sekunden lang abgelesen; Kritik: mechanische Traegheit verzoegert Messung um 0,3-0,5 s",
        answerD = "Ballonmeteorologie misst Windprofil 2 m ueber Boden; Kritik: Messungen erfolgen nur vor dem Rennen, nicht waehrend",
        correctAnswer = 0,
        explanation = "World Athletics schreibt ein Anemometer (typischerweise Ultraschall oder Schalenkreuz) vor, das waehrend des Rennens 10 Sekunden lang (beim 100 m ab dem Startschuss) misst und den Mittelwert als offiziellen Windwert ausgibt. Wissenschaftliche Kritik: 1) Ein einzelner Messpunkt 50 m entfernt repraesentiert nicht die gesamte Bahn; 2) Windgusts sind zeitlich ungleichmaessig verteilt; 3) Die 10-Sekunden-Mittelung glaettet mogliche kurzfristige, entscheidende Windbeguenstigungen waehrend des kritischen Geschwindigkeitsgipfels.",
        difficulty = 5,
        funFact = "Ben Johnsons 100-m-Weltrekord von 9,83 s bei der WM 1987 in Rom wurde zuerst mit +1,0 m/s Windunterstuetzung gemessen — windlegal. Sein 9,79 s in Seoul 1988 wurde wegen Dopings annulliert. Heute gilt Johnsons Seoul-Lauf trotz Annullierung als leistungsphysiologisch faszinierendstes Rennen — der Wind betrug nur +1,1 m/s."
    ),

    // Question 47 – Sport-Technologie: Sportgeraete-Materialwissenschaft
    Question(
        categoryId = 6,
        questionText = "Carbon-Fiber-Verbundwerkstoffe (CFRP) dominieren den modernen Hochleistungssport. Welcher spezifische Eigenschaft macht Hochmodul-Carbonfasern (HM-CF) ideal fuer Sportgeraete wie Stabhochsprung-Staebe und Tennisrahmen?",
        answerA = "Extrem hohe Zugfestigkeit kombiniert mit niedrigem E-Modul (niedrige Steifigkeit), was grosse elastische Verformung bei hoher Rueckstellkraft ermoeglicht",
        answerB = "Sehr hohes Steifigkeits-zu-Gewichts-Verhaeltnis (spezifischer E-Modul) kombiniert mit anisotropen Eigenschaften, die durch Faserorientierung gezielt gesteuert werden koennen",
        answerC = "Negative thermische Ausdehnungskoeffizient, der Temperaturstabilitaet bei Wettkampfgeraeten sicherstellt",
        answerD = "Maximale Schlagzaehigkeit (Impact Resistance) durch die Faserverstaerkung, die Bruchpropagation verhindert",
        correctAnswer = 1,
        explanation = "Hochmodul-Carbonfasern zeichnen sich durch ihr aussergewoehnliches spezifisches Steifigkeits-Verhaeltnis aus (E-Modul bis 500 GPa bei Dichte ~1,8 g/cm³, verglichen mit Stahl: E-Modul 210 GPa, Dichte 7,8 g/cm³). Entscheidend ist die Anisotropie: Durch gezielte Faserorientierung kann die Steifigkeit in bestimmten Richtungen maximiert und in anderen minimiert werden — ideal fuer Stabhochsprung-Staebe, die in eine Richtung biegen aber nicht in eine andere, oder Tennisrahmen, die bestimmte Vibrationsmoden daempfen sollen.",
        difficulty = 5,
        funFact = "Der erste Stabhochsprung-Weltrekord mit einem Carbonstab wurde 1981 von Billy Olson aufgestellt. Vorher dominierten Fibreglas-Staebe. Der Wechsel zu Carbon reduzierte das Gewicht des Stabs um bis zu 30 % bei gleicher oder hoeherer Steifigkeit — ein entscheidender Vorteil, da der Laeufer den Stab in der Anlaufphase moeglichst energiesparend fuehren muss."
    ),

    // ── Sportgeographie (7) ───────────────────────────────────────────────────

    // Question 48 – Sportgeographie: Hoehentraining
    Question(
        categoryId = 6,
        questionText = "Welcher physiologische Mechanismus erklaert den 'Living High, Training Low'-Ansatz (LHTL) im Hoehentraining, und bei welcher Hoehenlage wird er typischerweise angewendet?",
        answerA = "Schlafen auf 2.000-2.500 m (Hypoxie-Stimulus fuer EPO-Ausschuettung und Erythropoese), Trainieren auf Meeresniveau (vollstaendige Sauerstoffverfuegbarkeit fuer maximale Trainingsqualitaet und -intensitaet)",
        answerB = "Schlafen auf 1.000-1.500 m (moderate Hypoxie), Training in Druckkammern auf Meereshoeheneinstellung (Normobaric Hypoxia durch Stickstoff-Anreicherung)",
        answerC = "Schlafen auf 3.500-4.000 m (Extremhypoxie fuer maximalen Haematokrit-Anstieg), Training auf 2.000 m (Balance zwischen Hypoxie-Stimulus und Trainingsqualitaet)",
        answerD = "Schlafen auf Meeresniveau (Normoxie fuer Regeneration), Training auf 3.000 m (Hypoxie als Trainingsreiz ohne Schlafqualitaetsverlust)",
        correctAnswer = 0,
        explanation = "LHTL kombiniert das Beste beider Ansaetze: Schlaf/Aufenthalt auf 2.000-2.500 m (optimal fuer EPO-Ausschuettung, Erythropoese-Stimulation durch Hypoxia-Inducible Factor HIF-1alpha) mit Training auf Meeresniveau (maximale Sauerstoffverfuegbarkeit erlaubt hohe Trainingsintensitaet, die auf der Hoehe wegen Hypoxie-Ermuedung nicht moeglich waere). Pionier-Studie: Levine & Stray-Gundersen (1997).",
        difficulty = 5,
        funFact = "Schweizerische Sportwissenschaftler entwickelten 'Altitude Tents' (Hoehenzelte), die LHTL zuhause ermoeglichen: Schlafen in einem Zelt mit kuenstlich verduennter Luft (Hypoxie) und Training im normalen Umfeld. Obwohl physiologisch aehnlich effektiv wie echtes Hoehentraining, wird der psychologische Erholungseffekt echter Gebirgsumgebung als nicht replizierbar betrachtet."
    ),

    // Question 49 – Sportgeographie: WM-Austragungsorte
    Question(
        categoryId = 6,
        questionText = "Bei welcher Fussball-Weltmeisterschaft wurden erstmals Spiele auf mehr als einem Kontinent ausgetragen, und welche geographische Besonderheit war das Ergebnis der politischen Entscheidung?",
        answerA = "WM 2002 (Japan/Suedkorea): Spiele auf zwei asiatischen Laendern — erste Doppel-Gastgeberschaft der WM-Geschichte",
        answerB = "WM 2026 (USA/Kanada/Mexiko): Erstmals werden Spiele in drei verschiedenen Laendern auf zwei Kontinenten (Nord- und Mittelamerika) ausgetragen",
        answerC = "WM 2022 (Katar): Erstmals in einem nahostlichen Land, was geografisch-kulturell einen neuen Kontinent erschloss",
        answerD = "WM 1994 (USA): Erstmals ausserhalb von Europa und Suedamerika, was eine neue geopolitische Richtung markierte",
        correctAnswer = 0,
        explanation = "Die WM 2002 war die erste in Asien und die erste mit Doppel-Gastgeberschaft (Japan und Suedkorea). Als geographische Besonderheit wurden Spiele erstmals auf zwei verschiedene Laender desselben Kontinents aufgeteilt, mit Spielen in 10 japanischen und 10 suedkoreanischen Stadten. Die politisch-diplomatisch heikle Japan-Korea-Beziehung machte die Koordination besonders komplex.",
        difficulty = 5,
        funFact = "Die WM 2002 brachte einen der groessten Upset-Momente der Fussballgeschichte: Suedkorea besiegte im Viertelfinale Spanien und wurde halbfinale Teilnehmer — eine Sensation, die von Befuerwortern als asiatisches Sportwunder und von Kritikern als kontroverse Schiedsrichterleistungen analysiert wird. Der Gastgeber profitierte vom Heimvorteil in einer noch nie dagewesenen Weise."
    ),

    // Question 50 – Sportgeographie: Olympische Spiele und Hoehenlage
    Question(
        categoryId = 6,
        questionText = "Bei welchen Olympischen Spielen lieferte die extreme Hoehenlag des Austragungsortes den wissenschaftlichen Beweis fuer den Einfluss duenner Luft auf Sprint- und Wurfleistungen, und welcher Weltrekord bei diesen Spielen wird bis heute mit dem Hoehenbonus verbunden?",
        answerA = "Olympia 1956, Melbourne (Australien): Extreme Hitze statt Hoehe beeinflusste die Leistungen; Weltrekord im Speerwurf hielt 12 Jahre",
        answerB = "Olympia 1968, Mexico City (2.240 m Meereshoehe): Bob Beamons Weitsprung-Weltrekord von 8,90 m, der erst 1991 gebrochen wurde und als 'Jump of the Century' gilt",
        answerC = "Olympia 1980, Moskau (158 m): Politischer Boykott liess nur wenige Konkurrenten zu, Weltrekorde gelten als windbedingt beeintraechtigt",
        answerD = "Olympia 1972, Muenchen: Neue Kunststoff-Bahnen erzielten Spurts, die fruehere Weltrekorde ueberboten und den Hoeheneffekt simulierten",
        correctAnswer = 1,
        explanation = "Mexico City 1968 (2.240 m Meereshoehe, ca. 23 % weniger Luftdichte als auf Meeresniveau) war das wissenschaftliche Natuerexperiment zum Hoeheneffekt im Sport. Bob Beamons Weltrekord von 8,90 m im Weitsprung (Vorgaenger: 8,35 m) war um 55 cm entfernt — ein unvorstellbarer Sprung, der von Beamon selbst als 'cataleptic shock' beschrieben wurde. Der Hoehenbonus wird auf ca. 10-15 cm geschaetzt; der Rest war schlicht ueberragende Leistung.",
        difficulty = 5,
        funFact = "Bei denselben Spielen 1968 wurde der Fosbury Flop erstmals olympisch eingesetzt: Dick Fosbury gewann Gold mit 2,24 m und revolutionierte den Hochsprung. Zuvor dominierte die Westerroll-Technik. Heute springen alle Hochsprung-Weltklasse-Athleten Fosbury Flop — die Technik, die in Mexico City debuetiertert, hat die alten Techniken vollstaendig verdraengt."
    )

)

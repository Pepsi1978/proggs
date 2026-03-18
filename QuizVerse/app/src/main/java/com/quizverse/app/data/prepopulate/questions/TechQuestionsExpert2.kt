package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun techQuestionsExpert2(): List<Question> = listOf(

    // Question 1 — Kryptografie: Elliptische Kurven
    Question(
        categoryId = 7,
        questionText = "Welche mathematische Operation ist bei elliptischen Kurven die Grundlage des Diffie-Hellman-Schluesseltauschverfahrens (ECDH)?",
        answerA = "Multiplikation zweier Punkte auf der Kurve",
        answerB = "Wiederholte Punktaddition (skalare Multiplikation) auf der Kurve",
        answerC = "Berechnung des groessten gemeinsamen Teilers zweier Kurvenparameter",
        answerD = "Exponentiation des Kurvenparameters modulo einer Primzahl",
        correctAnswer = 1,
        explanation = "ECDH basiert auf der skalaren Multiplikation: Ein Punkt P wird k-mal zu sich selbst addiert (k*P). Die Umkehrung — k aus k*P zu berechnen — ist das diskrete Logarithmusproblem auf elliptischen Kurven, das als recheninfeasible gilt.",
        difficulty = 4,
        funFact = "Skalare Multiplikation mit k=256 Bit erfordert dank Double-and-Add nur ~512 Punktoperationen statt 2^256."
    ),

    // Question 2 — Kryptografie: Post-Quantum
    Question(
        categoryId = 7,
        questionText = "Welches NIST-standardisierte Post-Quantum-Verfahren basiert auf dem 'Module Learning With Errors' (MLWE)-Problem?",
        answerA = "SPHINCS+",
        answerB = "Classic McEliece",
        answerC = "CRYSTALS-Kyber (ML-KEM)",
        answerD = "XMSS",
        correctAnswer = 2,
        explanation = "CRYSTALS-Kyber, jetzt als ML-KEM standardisiert (FIPS 203), basiert auf dem MLWE-Problem ueber Modul-Gitter. Es ist ein Key Encapsulation Mechanism (KEM) und gilt als eines der effizientesten quantensicheren Verfahren.",
        difficulty = 4,
        funFact = "Kyber-768 hat oeffentliche Schluessel von nur 1184 Bytes — deutlich kompakter als RSA-3072 mit aehnlichem Sicherheitsniveau."
    ),

    // Question 3 — Kryptografie: Lattice-based
    Question(
        categoryId = 7,
        questionText = "Was beschreibt das 'Shortest Vector Problem' (SVP) in der Gitterkryptografie?",
        answerA = "Den kuerzesten Weg zwischen zwei Knoten in einem Gittergraphen zu finden",
        answerB = "Den kuerzesten Nicht-Null-Vektor in einem n-dimensionalen Gitter zu finden",
        answerC = "Die kleinste Primzahl innerhalb eines Gitterbereichs zu bestimmen",
        answerD = "Den optimalen Gitterpunkt fuer eine gegebene Fehlerkorrektur zu berechnen",
        correctAnswer = 1,
        explanation = "SVP verlangt, den kuerzesten Nicht-Null-Vektor in einem Gitter L = {sum(a_i * b_i) | a_i aus Z} zu finden. Fuer hohe Dimensionen ist SVP NP-hart und gilt auch fuer Quantencomputer als schwer — die Grundlage gitterbasierten Kryptos.",
        difficulty = 4,
        funFact = "Der beste bekannte klassische SVP-Algorithmus (BKZ) laeuft in Zeit 2^(0.292n) — fuer n=1000 also astronomisch lange."
    ),

    // Question 4 — Kryptografie: Zero-Knowledge Proofs
    Question(
        categoryId = 7,
        questionText = "Welche drei Eigenschaften muss ein Zero-Knowledge Proof (ZKP) zwingend erfuellen?",
        answerA = "Vollstaendigkeit, Korrektheit (Soundness), Zero-Knowledge-Eigenschaft",
        answerB = "Vertraulichkeit, Integritaet, Verfuegbarkeit",
        answerC = "Zufall, Deterministik, Homomorphie",
        answerD = "Bindung, Verbergung, Oeffnung",
        correctAnswer = 0,
        explanation = "Ein ZKP muss vollstaendig sein (ehrlicher Beweiser ueberzeugt Verifizierer), korrekt (unehrlicher Beweiser kann nicht ueberzeugen) und Zero-Knowledge (Verifizierer lernt nichts ausser der Wahrheit der Aussage). Diese Triade wurde 1985 von Goldwasser, Micali und Rackoff definiert.",
        difficulty = 4,
        funFact = "Ali-Baba-Hoehle: Das bekannteste ZKP-Anschauungsbeispiel erklaert alle drei Eigenschaften ohne eine einzige Gleichung."
    ),

    // Question 5 — Kryptografie: Zero-Knowledge Proofs
    Question(
        categoryId = 7,
        questionText = "Was unterscheidet einen zk-SNARK von einem zk-STARK hinsichtlich Vertrauensannahmen?",
        answerA = "zk-SNARKs benoetigen kein Trusted Setup, STARKs schon",
        answerB = "zk-SNARKs benoetigen ein Trusted Setup, STARKs sind trustless",
        answerC = "Beide benoetigen ein Trusted Setup, STARKs sind jedoch schneller",
        answerD = "Beide sind trustless, STARKs haben aber kleinere Proofs",
        correctAnswer = 1,
        explanation = "zk-SNARKs benoetigen eine 'Ceremony' (Trusted Setup), bei der geheime Parameter erzeugt werden. Werden diese nicht korrekt geloescht, kann ein Angreifer falsche Proofs erstellen. zk-STARKs kommen ohne Trusted Setup aus und beruhen nur auf kollisionsresistenten Hash-Funktionen.",
        difficulty = 4,
        funFact = "Zcashs 'Powers of Tau'-Ceremony 2018 involvierte ueberall auf der Welt verteilte Teilnehmer — einer davon soll seinen Computer danach im Feuer zerstoert haben."
    ),

    // Question 6 — Kryptografie: Homomorphe Verschluesselung
    Question(
        categoryId = 7,
        questionText = "Welche homomorphe Verschluesselung erlaubt SOWOHL Addition als auch Multiplikation auf verschluesselten Daten (vollstaendig homomorph)?",
        answerA = "Paillier-Kryptosystem",
        answerB = "ElGamal-Kryptosystem",
        answerC = "BGV/BFV/CKKS-Schemata (FHE nach Gentry)",
        answerD = "RSA im Raw-Modus",
        correctAnswer = 2,
        explanation = "Vollstaendig homomorphe Verschluesselung (FHE) wurde 2009 von Craig Gentry realisiert. Heutige Schemata wie BGV, BFV (ganzzahlig) und CKKS (Gleitkomma-Approximation) erlauben beliebige Berechnungen auf Geheimtexten. Paillier unterstuetzt nur Addition (partiell homomorph).",
        difficulty = 4,
        funFact = "FHE-Berechnungen sind noch 10.000–1.000.000x langsamer als Klartextberechnungen — aber Hardware-Beschleuniger (Google, IBM) reduzieren diesen Faktor rasant."
    ),

    // Question 7 — Kryptografie: Elliptische Kurven
    Question(
        categoryId = 7,
        questionText = "Warum gilt die NIST-Kurve P-256 in Teilen der Kryptografie-Gemeinschaft als weniger vertrauenswuerdig als Curve25519?",
        answerA = "P-256 hat einen kleineren Schluessel und ist damit unsicherer",
        answerB = "Die Kurvenparameter von P-256 wurden von der NSA ohne transparente Erklaerung ihrer Herkunft gewaehlt",
        answerC = "P-256 unterstuetzt kein ECDH, sondern nur ECDSA",
        answerD = "Curve25519 ist schneller, weil sie eine binaere statt eine Primzahl-Kurve verwendet",
        correctAnswer = 1,
        explanation = "Die 'Seeds' der NIST-Kurven (z.B. P-256) wurden mittels SHA-1 aus geheimen Ausgangswerten generiert. Da die NSA diese Ausgangswerte nicht erklaert hat, besteht theoretischer Verdacht auf eine Backdoor. Curve25519 wurde von Daniel Bernstein mit vollstaendig transparenten, nachvollziehbaren Kriterien gewaehlt.",
        difficulty = 4,
        funFact = "Nach den Snowden-Enthueallungen 2013 zog die NIST den Dual_EC_DRBG-Standard zurueck — ein Zufallsgenerator, der tatsaechlich eine NSA-Backdoor enthielt."
    ),

    // Question 8 — Netzwerk: BGP Hijacking
    Question(
        categoryId = 7,
        questionText = "Welcher Mechanismus verhindert BGP-Hijacking durch Verifikation der IP-Praefix-Ursprungsrechte?",
        answerA = "DNSSEC (DNS Security Extensions)",
        answerB = "RPKI (Resource Public Key Infrastructure)",
        answerC = "IPsec mit IKEv2",
        answerD = "BGPSEC Path Validation ohne RPKI",
        correctAnswer = 1,
        explanation = "RPKI verknuepft IP-Praefix-Eigentumsrechte kryptografisch mit Route Origin Authorizations (ROAs). Router validieren anhand signierter Zertifikate, ob ein AS berechtigt ist, ein bestimmtes Praefix zu annoncieren — und verwerfen ungueltige Routen.",
        difficulty = 4,
        funFact = "2010 wurde 15% des weltweiten Internetverkehrs 18 Minuten lang versehentlich ueber China Telecom umgeleitet — ein klassischer BGP-Hijack ohne boese Absicht."
    ),

    // Question 9 — Netzwerk: BGP Internals
    Question(
        categoryId = 7,
        questionText = "Was ist der Unterschied zwischen iBGP und eBGP bezueglich der TTL und des AS_PATH-Attributs?",
        answerA = "iBGP verwendet TTL=1 und fuegt dem AS_PATH die eigene AS-Nummer hinzu; eBGP verwendet TTL=255 und fuegt nichts hinzu",
        answerB = "eBGP verwendet TTL=1 und fuegt dem AS_PATH die eigene AS-Nummer hinzu; iBGP verwendet TTL=255 und veraendert AS_PATH nicht",
        answerC = "Beide verwenden TTL=64; der Unterschied liegt nur in der Next-Hop-Behandlung",
        answerD = "iBGP und eBGP sind identisch, der Unterschied entsteht erst durch Route Reflectors",
        correctAnswer = 1,
        explanation = "eBGP (External BGP) zwischen verschiedenen AS verwendet standardmaessig TTL=1 (direkt verbundene Nachbarn) und fuegt beim Weiterleiten die eigene AS-Nummer in AS_PATH ein. iBGP (Internal BGP) innerhalb eines AS verwendet TTL=255 und veraendert AS_PATH nicht, um Routing-Loops intern zu vermeiden.",
        difficulty = 4,
        funFact = "Das iBGP-Full-Mesh-Problem: Bei n Routern braucht man n*(n-1)/2 Sessions. Bei 100 Routern sind das 4950 Sessions — deshalb wurden Route Reflectors (RFC 4456) erfunden."
    ),

    // Question 10 — Netzwerk: MPLS
    Question(
        categoryId = 7,
        questionText = "Was ist der Zweck des 'Penultimate Hop Popping' (PHP) in MPLS-Netzwerken?",
        answerA = "Der vorletzte Router im LSP entfernt das MPLS-Label, damit der letzte Router keine Label-Lookup-Operation durchfuehren muss",
        answerB = "Der letzte Router im LSP fuegt ein zweites Label hinzu, um QoS zu ermoeglichen",
        answerC = "PHP bezeichnet das Hinzufuegen eines Explicit-Null-Labels am Eintrittspunkt des LSP",
        answerD = "Der vorletzte Router kopiert das Paket fuer Redundanz an zwei Ausgabe-Interfaces",
        correctAnswer = 0,
        explanation = "Beim PHP entfernt der vorletzte Router (Penultimate Hop) das MPLS-Label bereits, bevor das Paket den Egress-LSR (letzten Router) erreicht. Der Egress-LSR muss dann nur noch normale IP-Weiterleitung (ohne Label-Lookup) durchfuehren, was die Performance verbessert.",
        difficulty = 4,
        funFact = "Ohne PHP muesste der Egress-LSR zwei Lookups machen: Label-Swap + IP-Routing. PHP eliminiert den ersten Lookup und spart messbare CPU-Zeit bei Hochlast."
    ),

    // Question 11 — Netzwerk: SDN
    Question(
        categoryId = 7,
        questionText = "Welches Protokoll definiert die Kommunikation zwischen SDN-Controller (Control Plane) und Netzwerk-Geraeten (Data Plane) in OpenFlow?",
        answerA = "NETCONF ueber SSH",
        answerB = "OpenFlow Protocol ueber TLS/TCP auf Port 6633/6653",
        answerC = "RESTCONF ueber HTTPS",
        answerD = "OVSDB Management Protocol",
        correctAnswer = 1,
        explanation = "OpenFlow-Controller kommunizieren mit Switches ueber das OpenFlow-Protokoll auf TCP (Port 6653, aeltere Implementierungen 6633), optional mit TLS gesichert. Der Controller installiert Flow-Eintraege in die Flow-Tables der Switches und steuert so das Weiterleitungsverhalten zentral.",
        difficulty = 4,
        funFact = "OpenFlow wurde 2007 an der Stanford University entwickelt. Netzwerke wie Googles B4-WAN verwenden SDN, um die WAN-Auslastung auf ueber 95% zu steigern — klassische OSPF-Netzwerke erreichen typisch nur 30-40%."
    ),

    // Question 12 — Netzwerk: NFV
    Question(
        categoryId = 7,
        questionText = "Was beschreibt der Begriff 'VNF Chaining' (Service Function Chaining) in NFV-Architekturen?",
        answerA = "Das Verbinden mehrerer physischer Netzwerkgeraete in einer Kette zur Redundanz",
        answerB = "Die geordnete Sequenz von virtuellen Netzwerkfunktionen, die ein Datenpaket durchlaufen muss",
        answerC = "Das Klonen von VNFs fuer horizontale Skalierung unter Last",
        answerD = "Die Verkettung von VLAN-Tags fuer Multi-Tenant-Netzwerke",
        correctAnswer = 1,
        explanation = "Service Function Chaining (SFC, RFC 7665) definiert eine geordnete Abfolge von VNFs (z.B. Firewall → IDS → Load Balancer), die ein Paket sequentiell durchlaeuft. Der Network Service Header (NSH) traegt dabei Metadaten zur Steuerung des Pfads durch die Service Chain.",
        difficulty = 4,
        funFact = "Vor NFV mussten Telcos fuer jeden Netzwerkdienst dedizierte Hardware kaufen und einbauen — oft Monate Vorlaufzeit. Mit VNF-Chaining dauert das Deployment Minuten per Software."
    ),

    // Question 13 — Compiler: SSA-Form
    Question(
        categoryId = 7,
        questionText = "Was ist die zentrale Eigenschaft der Static Single Assignment (SSA) Form und wozu dient sie im Compiler-Backend?",
        answerA = "Jede Variable wird genau einmal im Programm definiert; dies vereinfacht Datenflussanalysen und Optimierungen wie Constant Propagation",
        answerB = "Alle Variablen werden als Konstanten behandelt, was dynamische Allokation eliminiert",
        answerC = "Jede Variable erhaelt genau einen Registerplatz, was Register Allocation trivialisiert",
        answerD = "Schleifen werden in SSA automatisch entrollt, da Schleifeninduktionsvariablen einmalig definiert werden",
        correctAnswer = 0,
        explanation = "In SSA wird jede Variable genau einmal (statisch) zugewiesen — bei mehrfacher Zuweisung werden neue Versionen (x1, x2, ...) erzeugt. An Kontrollfluss-Zusammenfuehrungspunkten werden Phi-Funktionen (φ) eingefuegt. Dies macht Datenabhaengigkeiten explizit und vereinfacht Optimierungen wie Dead Code Elimination, Constant Folding und Global Value Numbering.",
        difficulty = 4,
        funFact = "SSA wurde 1988 von Cytron et al. bei IBM entwickelt. Heute nutzen alle modernen Compiler (GCC, LLVM, HotSpot JIT) SSA intern als zentrale Zwischendarstellung."
    ),

    // Question 14 — Compiler: LLVM IR
    Question(
        categoryId = 7,
        questionText = "Welche Aussage beschreibt LLVM IR (Intermediate Representation) korrekt?",
        answerA = "LLVM IR ist eine plattformspezifische Assemblersprache fuer x86-64",
        answerB = "LLVM IR ist eine typisierte SSA-Form mit unendlich vielen virtuellen Registern, die plattformunabhaengig ist",
        answerC = "LLVM IR ist eine Hochsprachen-IR, die nur von Clang erzeugt werden kann",
        answerD = "LLVM IR entspricht dem Java-Bytecode und laeuft auf der JVM",
        correctAnswer = 1,
        explanation = "LLVM IR ist eine typisierte, plattformunabhaengige Zwischendarstellung in SSA-Form mit unendlich vielen (virtuellen) Registern (%reg). Sie kann als Text (.ll), Binaer (.bc, Bitcode) oder in-memory vorliegen. Verschiedene Frontends (Clang, Rust/rustc, Swift) erzeugen LLVM IR, das dann vom LLVM-Backend fuer beliebige Zielarchitekturen optimiert und kompiliert wird.",
        difficulty = 4,
        funFact = "Rust kompiliert ueber LLVM IR — deshalb profitiert Rust-Code automatisch von allen LLVM-Optimierungen wie Auto-Vectorization und Link-Time Optimization (LTO)."
    ),

    // Question 15 — Compiler: Register Allocation
    Question(
        categoryId = 7,
        questionText = "Welches NP-vollstaendige Problem bildet die theoretische Grundlage der Register Allocation durch Graph-Faerbung?",
        answerA = "Das Travelling-Salesman-Problem",
        answerB = "Das k-Graph-Faerbungsproblem (k-Coloring Problem)",
        answerC = "Das Rucksackproblem (Knapsack Problem)",
        answerD = "Das 3-SAT-Problem",
        correctAnswer = 1,
        explanation = "Register Allocation wird als Graph-Faerbungsproblem modelliert: Knoten repraesentieren Variablen (Live Ranges), Kanten verbinden Variablen, die gleichzeitig 'lebendig' sind (sich in ihren Live Ranges ueberlappen) und daher verschiedene Register benoetigen. Der Graph muss mit k Farben gefaerbt werden (k = Anzahl der Register). Da k-Coloring NP-vollstaendig ist, verwenden Compiler Heuristiken (Chaitin, George-Appel).",
        difficulty = 4,
        funFact = "Wenn eine Variable nicht gefaerbt werden kann (Spilling), wird sie in den Stack-Speicher ausgelagert. Gutes Register Allocation kann die Ausfuehrungsgeschwindigkeit um 20-40% verbessern."
    ),

    // Question 16 — Compiler: Loop Optimierungen
    Question(
        categoryId = 7,
        questionText = "Was bewirkt 'Loop Interchange' als Compiler-Optimierung und wann ist sie vorteilhaft?",
        answerA = "Schleifenkoerper werden dupliziert, um Sprungkosten zu reduzieren",
        answerB = "Die Reihenfolge verschachtelter Schleifen wird vertauscht, um Cache-Lokalitaet bei Array-Zugriffen zu verbessern",
        answerC = "Eine Schleife wird in kleinere Schleifen aufgeteilt, um SIMD-Vektorisierung zu ermoeglichen",
        answerD = "Schleifenzaehler werden invertiert, um negative Inkremente zu vermeiden",
        correctAnswer = 1,
        explanation = "Bei verschachtelten Schleifen und zeilenweiser Array-Speicherung (C, Rust) ist es wichtig, dass der innerste Schleifen-Index den schnellsten Speicherzugriff indiziert. Loop Interchange vertauscht aeussere und innere Schleife, wenn dies die raeumliche Cache-Lokalitaet verbessert (sequentielle Speicherzugriffe statt Spruenge um ganze Zeilen).",
        difficulty = 4,
        funFact = "Falsche Schleifenreihenfolge bei einer 1024x1024-Matrix kann die Ausfuehrungszeit durch Cache-Misses um den Faktor 10-50x verlaengern — Loop Interchange loest das automatisch."
    ),

    // Question 17 — Compiler: Loop Optimierungen
    Question(
        categoryId = 7,
        questionText = "Was ist 'Polyhedral Model' (Polyhedral Compilation) und welche Optimierungen ermoeglicht es?",
        answerA = "Ein Modell zur statischen Typisierung von Schleifenvariablen in funktionalen Sprachen",
        answerB = "Ein mathematisches Framework, das Schleifen als polyedrische Raeume modelliert und dadurch Loop Tiling, Loop Fusion und parallele Ausfuehrung automatisch ableitet",
        answerC = "Ein Cache-Vorhersagemodell auf Basis von Polygonen zur Laufzeit-Optimierung",
        answerD = "Ein Registermodell fuer SIMD-Einheiten mit vektorisierten Schleifenzaehlern",
        correctAnswer = 1,
        explanation = "Das Polyhedral Model repraesentiert Schleifennester als ganzzahlige Polyeder (Polytope) im Iterations-/Datenraum. Affine Transformationen auf diesen Polytopen entsprechen Compiler-Optimierungen (Tiling, Interchange, Skewing, Fusion). Tools wie PLUTO und Polly (LLVM-Plugin) nutzen dieses Modell fuer automatische Parallelisierung und Cache-Optimierung.",
        difficulty = 4,
        funFact = "GCC hat seit Version 4.4 das Graphite-Framework eingebaut, das auf dem Polyhedral Model basiert — aktivierbar mit -floop-nest-optimize."
    ),

    // Question 18 — Formale Methoden: Model Checking
    Question(
        categoryId = 7,
        questionText = "Was beschreibt das 'State Space Explosion Problem' beim Model Checking?",
        answerA = "Das Modell waechst exponentiell mit der Anzahl der parallelen Prozesse, was den vollstaendigen Zustandsraum oft unberechenbar macht",
        answerB = "Zustandsraeume koennen durch falsche Modellierung in Endlosschleifen geraten",
        answerC = "Der Model Checker benoetigt exponentiell viel Speicher fuer die CTL-Formel, nicht fuer das Modell",
        answerD = "Parallelausfuehrung des Model Checkers erzeugt redundante Zustaende und verfaelscht das Ergebnis",
        correctAnswer = 0,
        explanation = "Beim Model Checking wird der vollstaendige Zustandsraum eines Systems aufgezaehlt. Bei n parallelen Prozessen mit je k Zustaenden waechst der Gesamtzustandsraum als k^n (exponentiell). Schon wenige Prozesse koennen Milliarden von Zustaenden erzeugen. Gegenmassnahmen: Partial Order Reduction, Symbolic Model Checking (BDDs), Abstraction.",
        difficulty = 4,
        funFact = "Edmund Clarke, E. Allen Emerson und Joseph Sifakis erhielten 2007 den Turing Award fuer die Erfindung des Model Checking — eine der wenigen formalen Methoden, die in der Industrie (Intel, NASA) Einzug hielt."
    ),

    // Question 19 — Formale Methoden: Temporale Logik
    Question(
        categoryId = 7,
        questionText = "Was bedeutet die CTL-Formel 'AG(EF p)' in der Computation Tree Logic?",
        answerA = "Auf allen Pfaden gilt: Es gibt einen Pfad, auf dem p irgendwann gilt",
        answerB = "Fuer alle Zustaende auf allen Pfaden gilt: Von jedem erreichbaren Zustand aus existiert ein Pfad, auf dem p irgendwann gilt",
        answerC = "Es gibt einen globalen Zustand, von dem aus p immer gilt",
        answerD = "Auf allen Pfaden gilt p global und es gibt einen Pfad, auf dem p nicht gilt",
        correctAnswer = 1,
        explanation = "AG bedeutet 'fuer alle Pfade, global' (auf allen Zustaenden aller Pfade). EF bedeutet 'es existiert ein Pfad, auf dem irgendwann' (Existenz-Erreichbarkeit). AG(EF p) bedeutet: Von jedem im System erreichbaren Zustand aus gibt es immer noch einen Pfad, von dem p irgendwann erreicht wird. Dies modelliert 'Deadlock-Freiheit' oder 'Liveness' — das System kommt nie in eine Sackgasse.",
        difficulty = 4,
        funFact = "CTL kann nicht alle LTL-Eigenschaften ausdruecken und umgekehrt — die Logiken sind inkomparabel. CTL* vereinigt beide, ist aber exponentiell teurer zu pruefen."
    ),

    // Question 20 — Formale Methoden: Theorem Proving
    Question(
        categoryId = 7,
        questionText = "Was ist der Kern-Unterschied zwischen automatischem Model Checking und interaktivem Theorem Proving?",
        answerA = "Model Checking prueft nur Sicherheitseigenschaften; Theorem Proving nur Lebendigkeitseigenschaften",
        answerB = "Model Checking ist vollautomatisch und auf endliche Zustandsraeume beschraenkt; Theorem Proving ist manuell/halbautomatisch und kann unendliche Strukturen behandeln",
        answerC = "Theorem Proving benoetigt eine CTL-Formel; Model Checking arbeitet mit Hoare-Tripeln",
        answerD = "Model Checking kann keine Concurrency pruefen; Theorem Proving schon",
        correctAnswer = 1,
        explanation = "Model Checking ist vollautomatisch und exhaustiv, aber auf endliche (oder handhabbare) Zustandsraeume beschraenkt (State Explosion). Theorem Proving (z.B. Coq, Isabelle/HOL, Lean) kann beliebige mathematische Aussagen ueber unendliche Strukturen beweisen, erfordert aber menschliche Fuehrung bei der Beweiskonstruktion.",
        difficulty = 4,
        funFact = "Das seL4-Mikrobetriebssystem (2009) wurde mit Isabelle/HOL vollstaendig formal verifiziert — Beweis: ~200.000 Zeilen Proof-Skript fuer ~8750 Zeilen C-Code."
    ),

    // Question 21 — Kryptografie: Signaturen auf elliptischen Kurven
    Question(
        categoryId = 7,
        questionText = "Warum ist es katastrophal, bei ECDSA denselben Nonce k zweimal fuer zwei verschiedene Nachrichten zu verwenden?",
        answerA = "Die Signatur wird ungueltig und kann nicht verifiziert werden",
        answerB = "Der private Schluessel kann algebraisch aus den zwei Signaturen berechnet werden",
        answerC = "Die Signatur-Laenge verdoppelt sich und verraet den Nonce",
        answerD = "Die Hash-Funktion kollabiert und erzeugt denselben Digest fuer beide Nachrichten",
        correctAnswer = 1,
        explanation = "In ECDSA gilt: s = k^(-1)(H(m) + r*d) mod n, wobei d der private Schluessel ist. Werden zwei Signaturen (s1, r) und (s2, r) mit gleichem k fuer verschiedene Nachrichten H(m1), H(m2) erstellt, ergibt s1-s2 = k^(-1)(H(m1)-H(m2)), woraus k und dann d direkt berechnet werden kann. Sony's PlayStation 3 wurde 2010 genau so geknackt.",
        difficulty = 4,
        funFact = "EdDSA (z.B. Ed25519) loest dieses Problem durch deterministischen Nonce: k wird aus dem privaten Schluessel und der Nachricht per Hash berechnet — Zufallsquelle unnoetig."
    ),

    // Question 22 — Netzwerk: SDN Data Plane
    Question(
        categoryId = 7,
        questionText = "Was ist P4 (Programming Protocol-Independent Packet Processors) und wie unterscheidet es sich von OpenFlow?",
        answerA = "P4 ist eine Erweiterung von OpenFlow fuer IPv6; OpenFlow unterstuetzt nur IPv4",
        answerB = "P4 ist eine domainspezifische Sprache, die die Data Plane selbst programmiert; OpenFlow nur den Controller-Switch-Dialog",
        answerC = "P4 ersetzt den SDN-Controller; OpenFlow ist das Protokoll zwischen Controllern",
        answerD = "P4 und OpenFlow sind Synonyme fuer dasselbe IETF-Standardprotokoll",
        correctAnswer = 1,
        explanation = "P4 ist eine DSL, mit der die Packet-Processing-Pipeline des Switches selbst definiert wird: Parser, Match-Action-Tabellen, Deparser. OpenFlow legt fest, wie der Controller Flow-Eintraege in vordefinierte Tabellen schreibt. P4 ermoeglicht vollstaendig benutzerdefinierte Protokoll-Header und Forwarding-Logik — protokollunabhaengig.",
        difficulty = 4,
        funFact = "Intel Tofino und Google's Jupiter-Switches unterstuetzen P4. Mit P4 koennen neue Protokolle wie QUIC oder kundenspezifische Tunneling-Formate direkt im ASIC verarbeitet werden."
    ),

    // Question 23 — Compiler: Alias Analysis
    Question(
        categoryId = 7,
        questionText = "Warum ist Pointer-Aliasing ein zentrales Hindernis fuer Compiler-Optimierungen und wie hilft das 'restrict'-Schluesselwort in C?",
        answerA = "Aliasing verursacht Stack-Overflows; 'restrict' begrenzt die Rekursionstiefe",
        answerB = "Aliasing bedeutet, zwei Pointer koennen auf denselben Speicher zeigen, was Neuanordnung von Zugriffen verhindert; 'restrict' garantiert, dass ein Pointer exklusiven Zugriff hat",
        answerC = "Aliasing bezeichnet doppelte Variablendefinitionen in SSA; 'restrict' eliminiert Phi-Funktionen",
        answerD = "Aliasing tritt nur bei virtuellen Funktionsaufrufen auf; 'restrict' schaltet Virtual Dispatch ab",
        correctAnswer = 1,
        explanation = "Wenn zwei Pointer auf denselben Speicher zeigen koennen (aliasieren), darf der Compiler Lese-/Schreib-Operationen nicht umordnen oder in Register cachen. Mit 'restrict' in C garantiert der Programmierer, dass der Pointer der einzige Zugriffspfad auf diesen Speicherbereich ist — der Compiler darf dann aggressiver optimieren (z.B. SIMD-Vektorisierung von Memcpy-Schleifen).",
        difficulty = 4,
        funFact = "Die BLAS-Bibliotheken (Lineare Algebra) sind massiv auf 'restrict' angewiesen. Ohne diese Garantie waere Auto-Vectorization fuer Matrix-Multiplikation fast unmoeglich."
    ),

    // Question 24 — Kryptografie: Post-Quantum Signaturen
    Question(
        categoryId = 7,
        questionText = "Auf welchem mathematischen Problem basiert CRYSTALS-Dilithium (ML-DSA), das vom NIST als Post-Quantum-Signaturverfahren standardisiert wurde?",
        answerA = "Modulares Learning With Errors (MLWE) und Module Short Integer Solution (MSIS)",
        answerB = "Shortest Vector Problem in binaeren Gitter-Codes",
        answerC = "Multivariate quadratische Gleichungssysteme",
        answerD = "Isogenie-Probleme auf supersingularen elliptischen Kurven",
        correctAnswer = 0,
        explanation = "Dilithium (jetzt ML-DSA, FIPS 204) basiert auf MLWE (fuer die Schluessel-Erzeugung) und MSIS (Module Short Integer Solution, fuer die Signatur-Sicherheit). Das Fiat-Shamir-Paradigma mit 'Commitment-Response'-Struktur und Rejection Sampling erzeugt Signaturen, deren Groesse vom geheimen Schluessel unabhaengig ist.",
        difficulty = 4,
        funFact = "ML-DSA-Signaturen sind mit 2420-4595 Bytes deutlich groesser als RSA-256 oder ECDSA-64 Bytes — ein Preis fuer Quantensicherheit."
    ),

    // Question 25 — Netzwerk: MPLS Traffic Engineering
    Question(
        categoryId = 7,
        questionText = "Was ist der Unterschied zwischen RSVP-TE und LDP als Label-Distributing-Protokolle in MPLS-Netzwerken?",
        answerA = "RSVP-TE signalisiert Labels mit expliziter Pfad-Angabe und Bandbreiten-Reservierung; LDP verteilt Labels hop-by-hop nach IGP-Routing ohne Ressourcenreservierung",
        answerB = "LDP ist neuer als RSVP-TE und hat Traffic Engineering eingebaut; RSVP-TE ist veraltet",
        answerC = "RSVP-TE und LDP sind identisch, unterscheiden sich nur im encapsulation-Format",
        answerD = "LDP reserviert Bandbreite end-to-end; RSVP-TE verteilt Labels zufaellig",
        correctAnswer = 0,
        explanation = "LDP (Label Distribution Protocol, RFC 5036) verteilt Labels automatisch entlang des IGP-Pfads ohne Traffic-Engineering-Faehigkeiten. RSVP-TE (RFC 3209) ermoeglicht die explizite Angabe des gesamten Label Switched Path (LSP) und reserviert entlang des Pfads Bandbreite — essentiell fuer Traffic Engineering und QoS-Garantien.",
        difficulty = 4,
        funFact = "SR-MPLS (Segment Routing) loest beide ab: Kein verteiltes Signalisierungsprotokoll mehr noetig — der Quell-Router kodiert den gesamten Pfad als Label-Stack im Paket-Header."
    ),

    // Question 26 — Formale Methoden: Hoare-Logik
    Question(
        categoryId = 7,
        questionText = "Was bedeutet ein Hoare-Tripel {P} C {Q} und wie unterscheidet sich partielle von totaler Korrektheit?",
        answerA = "{P} C {Q}: Wenn C terminiert, gilt nach C die Bedingung Q — partielle Korrektheit; totale Korrektheit verlangt zusaetzlich, dass C immer terminiert",
        answerB = "{P} C {Q}: P ist die Nachbedingung, Q die Vorbedingung; bei partieller Korrektheit darf Q falsch sein",
        answerC = "Partielle Korrektheit bedeutet, P gilt nur teilweise; totale Korrektheit verlangt P immer wahr",
        answerD = "Hoare-Tripel beschreiben nur Schleifen; C muss ein Loop-Invariant sein",
        correctAnswer = 0,
        explanation = "Ein Hoare-Tripel {P} C {Q} bedeutet: Wenn P vor der Ausfuehrung von C gilt und C terminiert, dann gilt danach Q. Dies ist partielle Korrektheit. Totale Korrektheit ({P} C {Q} total) verlangt zusaetzlich den Beweis der Terminierung von C — typischerweise durch ein Terminierungsargument (Ranging Function / Variant).",
        difficulty = 4,
        funFact = "Tony Hoare entwickelte die Hoare-Logik 1969. Er erfand auch das Quicksort-Algorithmus — und bezeichnete die NULL-Referenz als seinen 'billion-dollar mistake'."
    ),

    // Question 27 — Compiler: Inlining und IPO
    Question(
        categoryId = 7,
        questionText = "Was ist 'Link-Time Optimization' (LTO) und welchen Vorteil bietet es gegenueber klassischer Compilation Unit-Optimierung?",
        answerA = "LTO optimiert den Linker-Vorgang selbst durch paralleles Laden von Objektdateien",
        answerB = "LTO ermoeglicht Optimierungen ueber Compilation-Unit-Grenzen hinweg, z.B. Cross-Module-Inlining und globale Dead Code Elimination",
        answerC = "LTO komprimiert Objektdateien vor dem Linking, um den Speicherbedarf zu reduzieren",
        answerD = "LTO ist ein Synonym fuer Profile-Guided Optimization (PGO)",
        correctAnswer = 1,
        explanation = "Klassische Optimierung findet nur innerhalb einer Translation Unit statt. LTO (auch LTCG — Link Time Code Generation bei MSVC) verarbeitet das ganze Programm als eine Einheit beim Linken: Funktionen koennen ueber Modulgrenzen hinweg geinlined werden, globale Analysen erkennen toteen Code, und Interprocedural Optimization (IPO) wirkt global.",
        difficulty = 4,
        funFact = "LLVM's ThinLTO ist eine skalierbare LTO-Variante, die Module parallel optimiert und nur die noetigsten Zusammenfassungen austauscht — im Gegensatz zu Full LTO, das alle Bitcode-Dateien zusammenfuehrt."
    ),

    // Question 28 — Kryptografie: Hashfunktionen
    Question(
        categoryId = 7,
        questionText = "Was ist ein 'Length Extension Attack' und gegen welche Hash-Konstruktionen ist sie wirksam?",
        answerA = "Ein Angriff, der durch Anhaengen von Daten an eine bekannte Nachricht einen gueltigen MAC ohne Kenntnis des Secrets erzeugt — wirksam gegen MD5 und SHA-1/SHA-2 im Merkle-Damgaard-Design",
        answerB = "Ein Angriff, der die Laenge des geheimen Schluessels durch Brute Force bestimmt",
        answerC = "Ein Angriff, der Hash-Kollisionen durch verlaengerte Eingaben erzeugt",
        answerD = "Ein Seitenkanal-Angriff, der die Ausfuehrungszeit des Hash-Algorithmus misst",
        correctAnswer = 0,
        explanation = "Merkle-Damgaard-Konstruktionen (MD5, SHA-1, SHA-256) sind angreifbar: Da der innere Zustand am Ende des Hash der Ausgabe entspricht, kann ein Angreifer mit Hash(secret||message) Daten anhaengen und Hash(secret||message||padding||extension) ohne Kenntnis von 'secret' berechnen. SHA-3 (Keccak, Schwamm-Konstruktion) und HMAC sind resistent.",
        difficulty = 4,
        funFact = "Viele Web-Applikationen der 2000er-Jahre nutzten SHA256(secret + user_data) als MAC — und waren alle fuer Length Extension anfaellig. HMAC-SHA256 waere korrekt gewesen."
    ),

    // Question 29 — Netzwerk: QUIC-Protokoll Internals
    Question(
        categoryId = 7,
        questionText = "Warum eliminiert QUIC das 'Head-of-Line Blocking' Problem, das bei HTTP/2 ueber TCP auftritt?",
        answerA = "QUIC verwendet UDP und multiplext Streams unabhaengig — ein verlorenes Paket blockiert nur seinen eigenen Stream, nicht andere",
        answerB = "QUIC verwendet TCP mit mehreren Verbindungen parallel und vermeidet so Blockierung",
        answerC = "QUIC eliminiert Paketverluste durch Forward Error Correction (FEC) vollstaendig",
        answerD = "QUIC verwendet Prioritaets-Tags in TCP-Optionen, um kritische Streams bevorzugt zu behandeln",
        correctAnswer = 0,
        explanation = "HTTP/2 multiplext Streams ueber eine TCP-Verbindung. Da TCP eine geordnete, zuverlaessige Byte-Stream-Abstraktion bietet, blockiert ein verlorenes TCP-Segment ALLE Streams (HOL-Blocking auf TCP-Ebene). QUIC implementiert Zuverlaessigkeit und Stream-Multiplexing selbst ueber UDP: Paketverluste betreffen nur den jeweiligen QUIC-Stream, andere koennen weiter empfangen werden.",
        difficulty = 4,
        funFact = "Google entwickelte QUIC intern als 'gQUIC' ab 2012. HTTP/3 (RFC 9114) ist standardisiert und macht QUIC zum Transport fuer das gesamte moderne Web."
    ),

    // Question 30 — Compiler: Vektorisierung
    Question(
        categoryId = 7,
        questionText = "Was ist 'SLP Vectorization' (Superword Level Parallelism) und wie unterscheidet es sich von Loop Vectorization?",
        answerA = "SLP vektorisiert isomorphe skalare Operationen ausserhalb von Schleifen (z.B. float-Berechnungen in einem Basisblock); Loop Vectorization vektorisiert Schleifen",
        answerB = "SLP vektorisiert Speicherzugriffe in Schleifen; Loop Vectorization vektorisiert arithmetische Operationen",
        answerC = "SLP und Loop Vectorization sind identisch — der Unterschied ist nur der Kontext (mit/ohne Pragma)",
        answerD = "SLP ist ein veraltetes Verfahren fuer SIMD-1-Breite; Loop Vectorization fuer SIMD-4 und breiter",
        correctAnswer = 0,
        explanation = "SLP Vectorization (Larsen & Amarasinghe, 2000) erkennt isomorphe — strukturell identische — skalare Operationen innerhalb eines Basisblocks (kein Loop noetig) und fasst sie zu SIMD-Operationen zusammen. Beispiel: vier unabhaengige float-Additionen werden zu einer _mm_add_ps-Instruktion. Loop Vectorization braucht eine Schleife als Erkennungsmuster.",
        difficulty = 4,
        funFact = "LLVM hat beide Vectorizer: den 'Loop Vectorizer' und den 'SLP Vectorizer'. Clang aktiviert SLP-Vectorization standardmaessig ab -O2."
    ),

    // Question 31 — Kryptografie: Seitenkanal-Angriffe
    Question(
        categoryId = 7,
        questionText = "Was ist ein 'Timing Attack' auf kryptografische Implementierungen und wie wird er durch 'Constant-Time Programming' verhindert?",
        answerA = "Ein Angriff, der Netzwerklatenz misst, um den Serverstandort zu ermitteln; verhindert durch geografische Verteilung",
        answerB = "Ein Angriff, der Ausfuehrungszeitunterschiede misst, um geheime Werte abzuleiten; verhindert durch zeitlich invariante Code-Pfade ohne datenabhaengige Verzweigungen",
        answerC = "Ein Angriff, der den CPU-Takt misst; verhindert durch Frequenz-Randomisierung",
        answerD = "Ein Denial-of-Service-Angriff auf zeitkritische Systeme; verhindert durch Rate Limiting",
        correctAnswer = 1,
        explanation = "Timing Attacks messen feine Zeitunterschiede in kryptografischen Operationen, die von Geheimnissen abhaengen (z.B. fruehzeitiger Abbruch bei Vergleich, bedingte Sprungbefehle). Constant-Time Code vermeidet datenabhaengige Branches und Memory-Lookups (Table Lookups koennen Cache-Timing-Seitenkanal oeffnen) — alle Pfade brauchen gleich lang.",
        difficulty = 4,
        funFact = "AES-128 war lange anfaellig fuer Cache-Timing-Angriffe (AES-NI loest das). RSA-Implementierungen waren ebenfalls anfaellig — Kocher's Paper 1996 startete das gesamte Feld der Seitenkanal-Analyse."
    ),

    // Question 32 — Formale Methoden: Abstraction Refinement
    Question(
        categoryId = 7,
        questionText = "Was ist das CEGAR-Verfahren (Counterexample-Guided Abstraction Refinement) im Model Checking?",
        answerA = "Ein Verfahren, das durch automatische Abstraktions-Verfeinerung basierend auf gefundenen Gegenbeispielen den Zustandsraum handhabbar haelt",
        answerB = "Ein Verfahren, das Gegenbeispiele aus Testfaellen generiert und als Model-Checker-Eingabe nutzt",
        answerC = "Ein SAT-Solver-Algorithmus zur Verifikation von Hardware-Schaltkreisen",
        answerD = "Eine Technik zur parallelen Ausfuehrung mehrerer Model Checker auf demselben Modell",
        correctAnswer = 0,
        explanation = "CEGAR beginnt mit einer groben Abstraktion des Systems (kleiner Zustandsraum), prueoft eine Eigenschaft, und wenn ein Gegenbeispiel gefunden wird: Ist es ein echtes Gegenbeispiel? Nein → die Abstraktion ist zu grob (spurious counterexample) → verfeinere die Abstraktion genau fuer diesen Pfad → wiederhole. So wachsen nur die relevanten Teile des Zustandsraums.",
        difficulty = 4,
        funFact = "CEGAR ist in SLAM (Microsoft) und Blast implementiert. SLAM wurde eingesetzt, um Windows-Geraetetreiber formal zu verifizieren — und fand hunderte reale Bugs."
    ),

    // Question 33 — Netzwerk: BGP Security
    Question(
        categoryId = 7,
        questionText = "Was prueft BGPSEC (RFC 8205) zusaetzlich zu RPKI und welches Problem verbleibt trotzdem?",
        answerA = "BGPSEC prueft die Gueltigkeit jedes AS-Hops im AS_PATH durch kryptografische Signaturen, verhindert aber keine legitimen AS, die Traffic absichtlich fehlleiten",
        answerB = "BGPSEC prueft die IP-Praefix-Eigentumsrechte und loest damit alle BGP-Sicherheitsprobleme vollstaendig",
        answerC = "BGPSEC ist ein Ersatz fuer RPKI und benoetigt keine PKI-Infrastruktur",
        answerD = "BGPSEC prueft nur den Origin-AS, nicht den vollstaendigen Pfad",
        correctAnswer = 0,
        explanation = "RPKI prueft nur den Origin-AS (wer darf ein Praefix annoncieren). BGPSEC erweitert dies auf den vollstaendigen AS_PATH: Jedes AS signiert kryptografisch, dass es eine Route von seinem Nachbarn empfangen hat. Problem: Ein AS kann trotzdem Traffic attract und legitimate weiterleiten (Interception-Angriff) oder Route-Leak begehen — BGPSEC verhindert keine Policy-Verletzungen.",
        difficulty = 4,
        funFact = "Die globale RPKI-Adoptionsrate liegt 2024 bei ca. 45% der annonciierten Praefixe — ein grosser Fortschritt, aber weit von vollstaendigem Schutz entfernt."
    ),

    // Question 34 — Compiler: Garbage Collection
    Question(
        categoryId = 7,
        questionText = "Was ist der Unterschied zwischen 'Tri-Color Marking' beim Garbage Collector und warum benoetigt ein concurrent GC eine 'Write Barrier'?",
        answerA = "Tri-Color Marking klassifiziert Objekte in weiss (unbesucht), grau (in Bearbeitung), schwarz (fertig); Write Barriers verhindern, dass ein schwarzes Objekt auf ein weisses zeigt (Invariante), wenn der Mutator parallel laeuft",
        answerB = "Tri-Color Marking verwendet drei Generationen; Write Barriers schreiben Generationswechsel in ein Journal",
        answerC = "Weisse Objekte sind frei, graue sind im Einsatz, schwarze werden geloescht; Write Barriers loggen alle Schreibzugriffe fuer den Stop-the-World-Pauses",
        answerD = "Tri-Color Marking ist eine Technik fuer Reference Counting; Write Barriers verhindern Zyklen",
        correctAnswer = 0,
        explanation = "Im Tri-Color-Algorithmus sind Objekte: weiss = nicht besucht (potenziell Garbage), grau = besucht aber Referenzen noch nicht verfolgt, schwarz = vollstaendig verarbeitet. Invariante: Ein schwarzes Objekt darf nicht direkt auf ein weisses zeigen. Wenn der Mutator (Programm) parallel laeuft, kann er eine Referenz von grau nach schwarz verschieben und eine neue weisse Referenz einfuehren. Write Barriers erkennen diese Verletzung und markieren betroffene Objekte neu.",
        difficulty = 4,
        funFact = "Go's GC (ab Version 1.5) ist vollstaendig concurrent mit Write Barriers. Java's ZGC und Shenandoah sind ebenfalls weitgehend concurrent mit sub-Millisekunden Pause-Zeiten."
    ),

    // Question 35 — Kryptografie: MPC (Multi-Party Computation)
    Question(
        categoryId = 7,
        questionText = "Was ist das Ziel von Secure Multi-Party Computation (MPC) und welches Grundprotokoll ermoeglicht die Addition geheimer Zahlen?",
        answerA = "MPC ermoeglicht mehreren Parteien, gemeinsam eine Funktion ueber ihre privaten Eingaben zu berechnen, ohne diese preiszugeben; Addition ist moeglich durch additive Secret Sharing (Shamir-Shares oder XOR-Shares)",
        answerB = "MPC ist eine Erweiterung von TLS fuer mehr als zwei Verbindungspartner; Addition wird durch Diffie-Hellman modelliert",
        answerC = "MPC ermoeglicht parallele Verschluesselung auf mehreren CPUs; Addition ist durch homomorphe Paillier-Verschluesselung moeglich",
        answerD = "MPC teilt den privaten Schluessel auf mehrere HSMs auf; Addition der Schluessel-Shares liefert den vollstaendigen Schluessel",
        correctAnswer = 0,
        explanation = "MPC berechnet f(x1,...,xn) sodass Partei i nur xi kennt und am Ende jeder das Ergebnis aber nicht die Eingaben der anderen erfahrt. Additive Secret Shares: Partei A erzeugt zufaellige Shares (s_A, x-s_A) fuer ihren Wert x. Zur Addition zweier geheimen Werte addieren die Parteien ihre Shares lokal — das Ergebnis ist der Share des Gesamtergebnisses, ohne dass Einzelwerte bekannt werden.",
        difficulty = 4,
        funFact = "Amazon, Apple und Google setzen MPC fuer Private Set Intersection ein — z.B. um zu pruefen, ob ein Kontakt auch die App nutzt, ohne die Kontaktlisten beider Parteien preiszugeben."
    ),

    // Question 36 — Netzwerk: VXLAN und Overlay-Netzwerke
    Question(
        categoryId = 7,
        questionText = "Welches Problem loest VXLAN (Virtual Extensible LAN) in modernen Rechenzentren und wie adressiert es die VLAN-Limitierung?",
        answerA = "VXLAN loest IP-Adressknappheit durch NAT-Encapsulation; es ersetzt VLANs vollstaendig durch subnetzbasierte Segmentierung",
        answerB = "VXLAN encapsuliert Layer-2-Frames in UDP/IP, erweitert den VLAN-Adressraum von 4094 auf 16 Millionen (24-Bit VNI) und ermoeglicht L2-Overlays ueber L3-Netzwerke",
        answerC = "VXLAN ist ein Ersatz fuer MPLS und unterstuetzt Traffic Engineering durch VNI-basierte LSPs",
        answerD = "VXLAN loest das Spanning-Tree-Problem durch verteiltes L3-Routing im Underlay",
        correctAnswer = 1,
        explanation = "802.1Q-VLANs sind auf 4094 (12-Bit VLAN-ID) beschraenkt — zu wenig fuer Multi-Tenant-Rechenzentren. VXLAN (RFC 7348) encapsuliert L2-Frames in UDP-Paketen mit einem 24-Bit Virtual Network Identifier (VNI), was 16 Millionen virtuelle Netze erlaubt. L2-Overlay laeuft ueber L3-Underlay, was flexible Netzwerk-Topologien ohne physische L2-Einschraenkungen ermoeglicht.",
        difficulty = 4,
        funFact = "VMware NSX, Amazon VPC und Azure VNET verwenden alle Overlay-Technologien (VXLAN oder GENEVE). GENEVE ist der Nachfolger mit erweiterbarem Options-Header."
    ),

    // Question 37 — Formale Methoden: SAT-Solving
    Question(
        categoryId = 7,
        questionText = "Welcher Algorithmus bildet die Grundlage moderner SAT-Solver und was ist das DPLL-Verfahren?",
        answerA = "DPLL (Davis-Putnam-Logemann-Loveland) ist ein Backtracking-Algorithmus mit Unit Propagation und Pure Literal Elimination fuer konjunktive Normalform",
        answerB = "DPLL ist ein polynomzeitiger Algorithmus, der SAT in bestimmten Faellen exakt loest",
        answerC = "DPLL ist ein genetischer Algorithmus fuer approximate SAT-Solving",
        answerD = "DPLL ist ein BDD-basierter Algorithmus zur Darstellung boolscher Formeln",
        correctAnswer = 0,
        explanation = "DPLL (1960/1962) ist ein vollstaendiger Backtracking-Algorithmus fuer SAT: Unit Propagation (erzwingt Literale in Einheitsklauseln), Pure Literal Elimination (setzt Literale, die nur in einer Polaritaet vorkommen), Branching (waehlt ein Literal und teilt in zwei Faelle). Moderne CDCL-Solver (Conflict-Driven Clause Learning, z.B. MiniSAT, CaDiCaL) erweitern DPLL um nicht-chronologisches Backtracking.",
        difficulty = 4,
        funFact = "CDCL-SAT-Solver koennen industrielle Instanzen mit Millionen Variablen loesen. Bounded Model Checking, Chip-Verifikation und Constraint-Solving in Compilern setzen alle auf CDCL."
    ),

    // Question 38 — Kryptografie: Authenticated Encryption
    Question(
        categoryId = 7,
        questionText = "Was ist der Unterschied zwischen AES-GCM und ChaCha20-Poly1305 und in welchem Kontext bevorzugt man welches?",
        answerA = "AES-GCM ist softwarebasiert und schneller auf mobilen CPUs; ChaCha20-Poly1305 erfordert Hardware-AES-Beschleuniger",
        answerB = "AES-GCM benoetigt Hardware-AES-Beschleuniger (AES-NI) fuer gute Performance; ChaCha20-Poly1305 ist softwareeffizient und bevorzugt auf Geraeten ohne AES-NI",
        answerC = "Beide sind identisch — der Unterschied liegt nur in der Schluesselloenge",
        answerD = "AES-GCM ist ein MAC; ChaCha20-Poly1305 ist eine AEAD-Konstruktion mit separater Verschluesselung",
        correctAnswer = 1,
        explanation = "AES-GCM (AEAD) ist extrem schnell auf CPUs mit AES-NI-Instruktionen (x86, ARM v8). Ohne Hardware-Beschleunigung ist AES langsam und anfaellig fuer Timing-Angriffe. ChaCha20-Poly1305 (RFC 8439, Daniel Bernstein) ist rein software-effizient, constant-time und auf allen Plattformen schnell. TLS 1.3 unterstuetzt beide; mobile Geraete ohne AES-NI bevorzugen ChaCha20.",
        difficulty = 4,
        funFact = "Android und iOS-Geraete haben seit Jahren AES-NI, deshalb verwenden beide Plattformen AES-GCM fuer Datei-Verschluesselung. TLS-Verbindungen von Chrome auf Low-End-Android-Geraeten nutzen ChaCha20."
    ),

    // Question 39 — Netzwerk: IS-IS vs OSPF
    Question(
        categoryId = 7,
        questionText = "Welcher fundamentale Unterschied besteht zwischen IS-IS und OSPF als Link-State-Routing-Protokolle hinsichtlich der Protokollschicht?",
        answerA = "IS-IS laeuft direkt ueber Layer 2 (eigenes PDU-Format), OSPF laeuft ueber IP (Protocol 89); IS-IS ist daher unabhaengig von der IP-Adressierung",
        answerB = "OSPF laeuft ueber UDP, IS-IS ueber TCP; OSPF ist deshalb zuverlaessiger",
        answerC = "IS-IS unterstuetzt nur IPv6, OSPF unterstuetzt nur IPv4",
        answerD = "IS-IS verwendet Link-State Advertisements, OSPF verwendet Distance-Vector-Updates",
        correctAnswer = 0,
        explanation = "OSPF (Open Shortest Path First) wird als IP-Protokoll ueber die IP-Schicht transportiert (Protocol-Nummer 89). IS-IS (Intermediate System to Intermediate System) laeuft direkt ueber Layer-2-Frames (eigenes CLNP/ISO PDU-Format) und ist vollstaendig unabhaengig von IP. Daher kann IS-IS gleichzeitig IPv4 und IPv6 routen (Multi-Topology IS-IS) ohne Protokollaenderungen.",
        difficulty = 4,
        funFact = "Die meisten grossen ISP-Backbones (AT&T, Level 3, Telia) verwenden IS-IS statt OSPF — hauptsaechlich wegen seiner Protokollunabhaengigkeit und besseren Skalierbarkeit bei grossen Netzen."
    ),

    // Question 40 — Compiler: JIT-Kompilierung
    Question(
        categoryId = 7,
        questionText = "Was ist 'Speculative Optimization' in JIT-Compilern (z.B. V8, HotSpot) und was passiert bei einer 'Deoptimization'?",
        answerA = "Der JIT spekuliert ueber Typinformationen zur Laufzeit und erzeugt optimierten Code; bei falscher Spekulation (Guard-Failure) wird auf interpretierten Code zurueckgefallen (Deopt)",
        answerB = "Spekulative Optimierung bedeutet, Code prophylaktisch vorab zu kompilieren; Deoptimierung loescht den generierten Code",
        answerC = "Der JIT spekuliert ueber Speicherlayout; Deoptimierung verschiebt Objekte in den Heap",
        answerD = "Spekulative Optimierung ist ein Synonym fuer Profile-Guided Optimization; Deoptimierung tritt bei Profil-Aenderungen auf",
        correctAnswer = 0,
        explanation = "JIT-Compiler beobachten Typ-Feedback (z.B. 'Funktion wird immer mit Integer aufgerufen') und generieren spezialisierten, hochoptimierten Code mit Guards (Pruefungen der Annahmen). Falls ein Guard fehlschlaegt (z.B. Funktion erhaelt ploetzlich ein Float), wird Deoptimierung ausgeloest: Ausfuehrungszustand wird auf den Interpreter-Zustand zurueckgerollt ('On-Stack Replacement' rueckwaerts).",
        difficulty = 4,
        funFact = "V8's 'Maglev' und 'Turbofan' sind zwei JIT-Tier-Compiler. JavaScript-Code kann bis zu 5 Optimierungsstufen durchlaufen, bevor er vollstaendig optimiert ist — oder durch Deoptimieriung zurueckfaellt."
    ),

    // Question 41 — Kryptografie: Key Derivation
    Question(
        categoryId = 7,
        questionText = "Welchen Vorteil bietet HKDF (HMAC-based Key Derivation Function, RFC 5869) gegenueber direktem SHA-256 zur Schluessel-Ableitung?",
        answerA = "HKDF ist schneller als SHA-256 auf mobilen Prozessoren",
        answerB = "HKDF trennt explizit Extract (Entropie-Kondensation) und Expand (Schluessel-Ableitung) und produziert kryptografisch unabhaengige Schluessel mit klaren Domaintrennungen",
        answerC = "HKDF verwendet AES statt HMAC und ist damit quantensicher",
        answerD = "HKDF kann direkt aus Passwoertern Schluessel ableiten ohne Salt",
        correctAnswer = 1,
        explanation = "HKDF hat zwei Phasen: Extract(salt, IKM) → PRK (Pseudo-Random Key) kondensiert ungleichmaessige Entropie in einen uniformen PRK. Expand(PRK, info, L) → OKM leitet aus PRK mehrere unabhaengige Schluessel ab, wobei 'info' als Domain-Separator wirkt. Direktes SHA-256(IKM) hat keine Domaintrennung und kann bei niedriger IKM-Qualitaet schwache Ausgaben erzeugen.",
        difficulty = 4,
        funFact = "TLS 1.3 verwendet HKDF als zentrale KDF fuer alle Schluessel: Handshake Keys, Traffic Keys und Resume Keys werden alle via HKDF-Extract und HKDF-Expand aus dem Shared Secret abgeleitet."
    ),

    // Question 42 — Formale Methoden: Type Theory
    Question(
        categoryId = 7,
        questionText = "Was ist der Curry-Howard-Isomorphismus und welche Verbindung stellt er zwischen Logik und Typsystemen her?",
        answerA = "Der Isomorphismus besagt, dass Typen Aussagen entsprechen, Programme Beweise, und ein wohlgetyptes Programm somit einen mathematischen Beweis darstellt",
        answerB = "Der Isomorphismus zeigt, dass jede Logik in ein Typsystem uebersetzt werden kann, ohne Semantikverlust",
        answerC = "Curry-Howard besagt, dass funktionale Programme immer korrekt sind, wenn sie den Typchecker passieren",
        answerD = "Der Isomorphismus verbindet Lambda-Kalkuel mit Turingmaschinen und beweist deren Aequivalenz",
        correctAnswer = 0,
        explanation = "Curry-Howard: Typen = logische Aussagen, Programme = Beweise, Typkonstruktoren = logische Konnektoren. Beispiel: A→B entspricht dem Implikationstyp (Funktion), A×B dem Konjunktionstyp (Produkt), leerer Typ Bottom entspricht False. Ein Programm mit Typ T ist ein konstruktiver Beweis von T. Dependent Type Systems (Coq, Lean, Agda) nutzen dies fuer formale Verifikation.",
        difficulty = 4,
        funFact = "'Propositions as Types' — Philip Wadler's beruehhmter Vortrag. Lean 4, der Theorem-Prover von Microsoft Research, basiert vollstaendig auf diesem Isomorphismus und wird zur Formalisierung ganzer mathematischer Theorien (Mathlib) eingesetzt."
    ),

    // Question 43 — Netzwerk: TCP Congestion Control
    Question(
        categoryId = 7,
        questionText = "Was unterscheidet BBR (Bottleneck Bandwidth and Round-Trip Propagation Time) von klassischen verlustbasierten TCP-Congestion-Control-Algorithmen wie CUBIC?",
        answerA = "BBR misst direkt Bandbreite und RTT-Modell; CUBIC reagiert auf Paketverlust als Ueberlastungssignal — BBR verursacht deshalb weniger unnoetige Rate-Reduzierungen bei shallow Buffers",
        answerB = "BBR verwendet keinen Slow Start; CUBIC startet immer mit Slow Start",
        answerC = "CUBIC ist modellbasiert; BBR reaktiv — BBR reagiert auf ECN-Markierungen, CUBIC nicht",
        answerD = "BBR ist ein UDP-basierter Algorithmus; CUBIC nur fuer TCP",
        correctAnswer = 0,
        explanation = "CUBIC und Reno reagieren auf Paketverlust als primaerem Ueberlastsignal — bei Bufferbloat (volle Puffer, aber kein Verlust) senden sie zu langsam. BBR (Google, 2016) schlaetzt kontinuierlich Bottleneck-Bandwidth (BtlBw) und Minimum-RTT (RTprop), haelt die Rate bei ~BtlBw und vermeidet es, Buffer unnoetig zu fuellen. BBR ignoriert Verluste als primaeres Signal.",
        difficulty = 4,
        funFact = "Google hat BBR fuer YouTube und Google.com eingesetzt und dabei 75% besseren Durchsatz bei hohem Paketverlust gemessen. BBRv3 (2023) adressiert Fairnessprobleme gegenueber CUBIC-Verbindungen."
    ),

    // Question 44 — Kryptografie: Threshold Cryptography
    Question(
        categoryId = 7,
        questionText = "Was ist Shamir's Secret Sharing und wie garantiert ein (t, n)-Schwellwert-Schema Sicherheit?",
        answerA = "Das Secret wird in n Teile aufgeteilt; jeder Teil alleine reicht zur Rekonstruktion, aber alle n Teile zusammen sind noetig fuer vollstaendige Sicherheit",
        answerB = "Das Secret wird als Polynom vom Grad t-1 kodiert; beliebige t von n Shares genuegen zur Rekonstruktion; weniger als t Shares verraten keinerlei Information (informationstheoretisch sicher)",
        answerC = "Das Secret wird XOR-geteilt; t Shares genuegen fuer Rekonstruktion; Sicherheit basiert auf der Schwierigkeit des diskreten Logarithmus",
        answerD = "Shamir's Schema teilt den privaten RSA-Schluessel auf n Primzahlen auf; mindestens t Primzahlen sind zur Faktorisierung noetig",
        correctAnswer = 1,
        explanation = "Shamir's Secret Sharing (1979) kodiert das Secret als f(0) eines zufaelligen Polynoms f vom Grad t-1. Jeder der n Teilnehmer erhaelt einen Punkt (i, f(i)). Lagrange-Interpolation mit t Punkten rekonstruiert f eindeutig und liefert f(0). Mit weniger als t Punkten gibt es unendlich viele kompatible Polynome — das Schema ist informationstheoretisch sicher (auch gegen unbegrenzte Rechenleistung).",
        difficulty = 4,
        funFact = "Cloudflare und Amazon CloudHSM nutzen Threshold-Kryptografie fuer die Verwaltung ihrer Root CAs. DNSSEC-Root-Key-Ceremony: 7 Key Custodians weltweit, 3 genuegen zur Wiederherstellung."
    ),

    // Question 45 — Compiler: Abstract Interpretation
    Question(
        categoryId = 7,
        questionText = "Was ist Abstrakte Interpretation als statische Analyse-Methode und wofuer wird sie z.B. im Astrée-Analyser eingesetzt?",
        answerA = "Abstrakte Interpretation fuehrt Programme mit abstrakten Wertebereichen (statt konkreten Werten) aus, um Eigenschaften aller moeglichen Ausfuehrungen zu approximieren — ohne falschen Negativen (sound)",
        answerB = "Abstrakte Interpretation ist eine Form von Symbolic Execution, bei der alle Ausfuehrungspfade vollstaendig exploriert werden",
        answerC = "Abstrakte Interpretation abstrahiert den Quellcode zu einer Zwischensprache fuer plattformunabhaengige Analyse",
        answerD = "Abstrakte Interpretation ist ein SAT-basiertes Verfahren zur Elimination toter Code-Pfade",
        correctAnswer = 0,
        explanation = "Abstrakte Interpretation (Cousot & Cousot, 1977) interpretiert Programme nicht auf konkreten Werten, sondern auf abstrakten Domaenen (z.B. Intervalle, Octagon-Domain). Die Analyse ist sound: Wenn sie kein Fehler meldet, gibt es keinen (keine falsche Negative). Astrée wurde eingesetzt, um den Flugsoftware-Code des Airbus A380 (130.000 Zeilen C) auf Laufzeitfehler (Division by Zero, Buffer Overflow) zu pruefen — mit null falsch Positiven.",
        difficulty = 4,
        funFact = "Astrée analysiert avionics-kritischen Code in Minuten und findet Fehler, die Tester in Jahren nicht gefunden haetten. Es ist zertifiziert fuer DO-178C Level A (hoechste Sicherheitsstufe in der Luftfahrt)."
    ),

    // Question 46 — Netzwerk: EVPN
    Question(
        categoryId = 7,
        questionText = "Was ist EVPN (Ethernet VPN, RFC 7432) und welchen Vorteil bietet es gegenueber klassischen L2-VPN-Technologien wie VPLS?",
        answerA = "EVPN ist eine Erweiterung von MPLS-TE fuer Ethernet-Frames; VPLS ist veraltet und unterstuetzt kein Multicast",
        answerB = "EVPN verwendet BGP als Control Plane fuer MAC/IP-Erreichbarkeit und ermoeglicht effizienten Multi-Homing, Active-Active-Redundanz und ARP-Suppression; VPLS floodet unbekannte Frames",
        answerC = "EVPN und VPLS sind identisch; der Unterschied liegt nur in der Implementierung des Underlay-Protokolls",
        answerD = "EVPN ist nur fuer Data-Center-Interconnect relevant; VPLS skaliert besser in WAN-Netzwerken",
        correctAnswer = 1,
        explanation = "VPLS (Virtual Private LAN Service) floodet unbekannte Unicast- und Broadcast-Frames im Netz (wie ein echter Switch) — schlecht skalierbar. EVPN nutzt BGP-MP (Multi-Protocol BGP) als Control Plane: MAC-Adressen und IP-Bindings werden als BGP-NLRI verteilt. Vorteile: ARP-Proxy (kein Flooding noetig), Active-Active Multi-Homing (Ethernet Segment Identifier), schnelleres Konvergenzverhalten.",
        difficulty = 4,
        funFact = "EVPN/VXLAN ist heute der De-facto-Standard fuer Data-Center-Fabrics bei grossen Hyperscalern. Cisco's ACI, VMware's NSX und Cumulus Linux unterstuetzen alle EVPN als Control-Plane."
    ),

    // Question 47 — Kryptografie: Oblivious RAM
    Question(
        categoryId = 7,
        questionText = "Was ist Oblivious RAM (ORAM) und welches Sicherheitsproblem loest es bei Cloudspeicher-Szenarien?",
        answerA = "ORAM verschluesselt Daten im RAM, damit ein privilegierter Prozess sie nicht lesen kann",
        answerB = "ORAM verbirgt Zugriffsmuster (welche Speicheradressen wann gelesen/geschrieben werden) vor einem neugierigen Server, der verschluesselte Daten speichert",
        answerC = "ORAM ist eine Technik zur speichersicheren Programmierung ohne Buffer-Overflows",
        answerD = "ORAM ermoeglicht Random-Access in verschluesselten Datenbanken ohne Entschluesselung",
        correctAnswer = 1,
        explanation = "Auch wenn Daten verschluesselt sind, verraten Zugriffsmuster (welche Bloecke wann zugegriffen werden) dem Speicherserver Information. ORAM (Goldreich & Ostrovsky, 1996) randomisiert und shuffelt Datenbloecke so, dass der Server aus den Zugriffsmustern nichts ableiten kann. Path ORAM (Stefanov et al., 2013) ist eine praktische Konstruktion mit O(log n) Overhead.",
        difficulty = 4,
        funFact = "ORAM wird in Intel SGX-Enklaven und Trusted Execution Environments eingesetzt, wenn der Host-OS das Zugriffsmuster nicht lernen soll — z.B. bei privatem Datenbankzugriff."
    ),

    // Question 48 — Formale Methoden: Separation Logic
    Question(
        categoryId = 7,
        questionText = "Was ist Separation Logic und welches Kernpraedikat 'P * Q' (Separating Conjunction) bedeutet?",
        answerA = "Separation Logic ist eine Erweiterung der Hoare-Logik fuer Heap-Manipulation; P * Q bedeutet, der Heap kann in zwei disjunkte Teile aufgeteilt werden, fuer die P bzw. Q gelten",
        answerB = "Separation Logic ist eine modale Logik fuer parallele Ausfuehrung; P * Q bedeutet P und Q gelten gleichzeitig in verschiedenen Threads",
        answerC = "'P * Q' ist das Staroperator fuer universelle Quantifikation ueber alle Heap-Elemente",
        answerD = "Separation Logic erweitert CTL um Heap-Referenzen; P * Q bedeutet P gilt in allen, Q in einigen Zustaenden",
        correctAnswer = 0,
        explanation = "Separation Logic (O'Hearn & Reynolds, 2000/2002) erweitert Hoare-Logik um einen separierenden Konjunktionsoperator: {P * Q} bedeutet, der aktuelle Heap laesst sich in zwei disjunkte Teile teilen, auf denen P bzw. Q separat gelten. Dies ermoeglicht lokales Reasoning ueber Heap-Zeiger und Speicher-Aliasing — die Grundlage von Tools wie Facebook Infer und dem VeriFast Verifier.",
        difficulty = 4,
        funFact = "Facebook Infer (Open Source, basiert auf Separation Logic) analysiert taegliche Code-Commits bei Facebook, Instagram und WhatsApp automatisch auf Null-Pointer-Dereferences und Memory-Leaks — und hat hunderte reale Bugs vor dem Deployment gefunden."
    ),

    // Question 49 — Netzwerk: Segment Routing
    Question(
        categoryId = 7,
        questionText = "Was ist Segment Routing (SR-MPLS/SRv6) und wie vereinfacht es das MPLS-Netzwerk-Management?",
        answerA = "Segment Routing ersetzt MPLS durch ein vollstaendig IP-basiertes Forwarding ohne Labels",
        answerB = "Bei Segment Routing kodiert der Ingress-Router den vollstaendigen Forwarding-Pfad als geordnete Liste von Segmenten (SIDs) im Paket-Header — kein verteiltes Signalisierungsprotokoll (RSVP/LDP) noetig",
        answerC = "Segment Routing ist ein SDN-Protokoll zwischen Controller und Switches; Segmente sind Flow-Table-Eintraege",
        answerD = "SRv6 fuegt IPv6-Adressen als MPLS-Labels hinzu und ist kompatibel mit IPv4-only-Netzwerken",
        correctAnswer = 1,
        explanation = "Segment Routing (RFC 8402) kodiert den Forwarding-Path als geordnete Segment-Liste im Paket selbst (MPLS-Label-Stack bei SR-MPLS; SRv6-Header bei SRv6). Kein per-flow-State in Transit-Routern, kein RSVP-TE-Signalisierungsprotokoll noetig. Der Quell-Router (oder ein SDN-Controller) berechnet und kodiert den Pfad; Transit-Router fuehren nur die Segment-Instruktionen aus.",
        difficulty = 4,
        funFact = "Segment Routing ist der Kernmechanismus hinter Googles TE-System und bei fast allen grossen Telekommmunikationsanbietern im Rollout. SRv6 kodiert Segmente als IPv6-Adressen und ermoeglicht Network Slicing fuer 5G-Netze."
    ),

    // Question 50 — Compiler: Undefined Behavior
    Question(
        categoryId = 7,
        questionText = "Warum darf ein C/C++-Compiler bei Undefined Behavior (UB) beliebigen Code erzeugen und welche gefaehrliche Optimierung macht er typischerweise bei vorzeichenbehaftetem Integer-Overflow?",
        answerA = "Der Compiler muss UB immer als Laufzeitfehler behandeln und einen Trap einfuegen",
        answerB = "Der Compiler darf annehmen, dass UB niemals auftritt; bei signed Integer-Overflow (UB in C/C++) kann er Ueberlaufpruefungen als immer-false eliminieren, was zu sicherheitskritischen Bugs fuehrt",
        answerC = "UB ist nur bei Zeiger-Dereferenzen relevant; Integer-Overflow ist wohldefiniert in C",
        answerD = "Bei UB wechselt der Compiler in einen Safe-Mode und erzeugt konservativeren Code",
        correctAnswer = 1,
        explanation = "In C/C++ ist signed Integer-Overflow Undefined Behavior. Der Compiler darf annehmen, dass UB nicht eintritt. Beispiel: 'if (x + 1 > x)' — der Compiler kann schlussfolgern: 'Falls x signed int ist, kann x+1 nie ueberlaufen (UB), also ist x+1 immer > x, also ist die Bedingung immer true und kann eliminiert werden.' Solche Optimierungen entfernen Sicherheits-Pruefungen (Overflow-Checks) unsichtbar.",
        difficulty = 4,
        funFact = "Clang und GCC haben beide UB-Sanitizer (-fsanitize=undefined), die UB zur Laufzeit detektieren. Der Linux-Kernel und Chrome werden regelmaessig mit UBSan gebaut, um versteckte UB-Bugs zu finden."
    )
)

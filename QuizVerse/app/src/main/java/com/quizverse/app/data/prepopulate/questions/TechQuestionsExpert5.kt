package com.quizverse.app.data.prepopulate.questions

import com.quizverse.app.data.database.entities.Question

fun techQuestionsExpert5(): List<Question> = listOf(

    // Question 1 — Quantencomputing: Shors Algorithmus
    Question(
        categoryId = 7,
        questionText = "Welche mathematische Operation macht Shors Algorithmus effizienter als alle bekannten klassischen Algorithmen zur Faktorisierung?",
        answerA = "Diskrete Fourier-Transformation ueber einem Galois-Koerper",
        answerB = "Quanten-Fourier-Transformation zur Periodenbestimmung einer modularen Funktion",
        answerC = "Grover-Suche in einem unstrukturierten Suchraum",
        answerD = "Amplitude Amplification kombiniert mit HHL-Invertierung",
        correctAnswer = 1,
        explanation = "Shors Algorithmus nutzt die Quanten-Fourier-Transformation um die Periode r der Funktion f(x) = a^x mod N zu finden. Aus dieser Periode laesst sich der ggT(a^(r/2) ± 1, N) und damit ein Primfaktor von N effizient berechnen.",
        difficulty = 4,
        funFact = "Shors Algorithmus laeuft in O((log N)^3) — klassische Siebmethoden benoetigen sub-exponentielle aber super-polynomielle Zeit. Ein 4000-Qubit-Quantencomputer koennte RSA-2048 in Stunden brechen."
    ),

    // Question 2 — Quantencomputing: Fehlerkorrektur
    Question(
        categoryId = 7,
        questionText = "Welcher Quanten-Fehlerkorrekturcode codiert ein logisches Qubit in neun physikalische Qubits und schuetzt sowohl vor Bit- als auch vor Phasenfehlern?",
        answerA = "Steane-Code [[7,1,3]]",
        answerB = "Surface Code",
        answerC = "Shor-Code [[9,1,3]]",
        answerD = "Bacon-Shor-Code",
        correctAnswer = 2,
        explanation = "Der Shor-Code codiert ein logisches Qubit in 9 physikalische Qubits: Drei Gruppen zu je drei Qubits schuetzen vor Bit-Flip-Fehlern (repetition code), waehrend die Phasenfehler durch eine Hadamard-transformierte Ebene korrigiert werden.",
        difficulty = 4,
        funFact = "Der Shor-Code war der erste Quantenfehlerkorrekturcode und wurde 1995 von Peter Shor entwickelt, dem selben Forscher der auch den Faktorisierungsalgorithmus erfand."
    ),

    // Question 3 — Quantencomputing: Verschraenkung
    Question(
        categoryId = 7,
        questionText = "Was beschreibt das No-Communication-Theorem in Bezug auf Quantenverschraenkung?",
        answerA = "Verschraenkte Qubits koennen keine Information ueber Lichtgeschwindigkeit uebertragen",
        answerB = "Quantenmessungen koennen nicht deterministisch gesteuert werden",
        answerC = "Quantenzustaende koennen nicht ohne Stoerung kopiert werden",
        answerD = "Bell-Ungleichungen koennen durch lokale verborgene Variablen erklaert werden",
        correctAnswer = 0,
        explanation = "Das No-Communication-Theorem besagt, dass die Messung eines verschraenkten Qubits zwar den Zustand des anderen instantan beeinflusst, aber kein nutzbares Signal uebertragen werden kann, da die Messergebnisse zufallig sind und erst durch klassischen Kanal verglichen werden muessen.",
        difficulty = 4,
        funFact = "Quantenteleportation verwendet Verschraenkung, erfordert aber immer einen klassischen Kommunikationskanal — sie ist deshalb kein Geraet fuer ueberlichtschnelle Kommunikation, sondern ein Quantenzustandstransfer."
    ),

    // Question 4 — Blockchain: Merkle-Trees
    Question(
        categoryId = 7,
        questionText = "Wie berechnet Bitcoin den Merkle-Root eines Blocks mit einer ungeraden Anzahl von Transaktionen?",
        answerA = "Eine Leer-Transaktion (null-bytes) wird als Platzhalter hinzugefuegt",
        answerB = "Die letzte Transaktion wird dupliziert um eine gerade Anzahl zu erhalten",
        answerC = "Der Hash der letzten Transaktion wird direkt in die naechste Ebene propagiert",
        answerD = "Die Transaktionen werden in einem binaeren Suchbaum anstelle eines Merkle-Trees angeordnet",
        correctAnswer = 1,
        explanation = "Bitcoin dupliziert bei ungerader Anzahl die letzte Transaktion (oder ihren Hash) auf der betreffenden Ebene, um einen vollstaendigen binaeren Baum zu bilden. Diese Methode ist in allen Bitcoin-Implementierungen identisch und Bestandteil des Konsens-Protokolls.",
        difficulty = 4,
        funFact = "Diese Duplikations-Methode fuehrte zu einer bekannten Schwachstelle: Bestimmte Transaktionspaare koennen den gleichen Merkle-Root erzeugen (CVE-2012-2459), weshalb BIP-341 dieses Problem mit Taproot adressiert."
    ),

    // Question 5 — Blockchain: Konsens
    Question(
        categoryId = 7,
        questionText = "Was unterscheidet Byzantine Fault Tolerance (BFT) von Crash Fault Tolerance (CFT) in verteilten Konsens-Algorithmen?",
        answerA = "BFT toleriert mehr Knotenausfaelle als CFT bei gleicher Knotenanzahl",
        answerB = "BFT schuetzt vor aktiv boesartigen Knoten die falsche Nachrichten senden, CFT nur vor stillen Ausfaellen",
        answerC = "CFT ist asynchron, BFT setzt synchrone Kommunikation voraus",
        answerD = "BFT wird nur fuer Blockchain verwendet, CFT fuer klassische Datenbanken",
        correctAnswer = 1,
        explanation = "CFT-Algorithmen (wie Raft) nehmen an, dass ausgefallene Knoten einfach schweigen. BFT-Algorithmen (wie PBFT) tolerieren zusaetzlich Knoten, die aktiv luegen, falsche oder widerspruechliche Nachrichten senden — das erfordert mindestens 3f+1 Knoten bei f boesartigen.",
        difficulty = 4,
        funFact = "Das Byzantine-Generals-Problem wurde 1982 von Lamport, Shostak und Pease formuliert. Der Name stammt von der Analogie byzantinischer Generaele, die per Boten koordinieren muessen waehrend einige Generaele Verraeter sein koennten."
    ),

    // Question 6 — Cybersecurity: Zero-Trust
    Question(
        categoryId = 7,
        questionText = "Welches Kernprinzip unterscheidet Zero-Trust-Architektur von traditionellen perimeterbasierten Sicherheitsmodellen?",
        answerA = "Zero-Trust verwendet ausschliesslich Ende-zu-Ende-Verschluesselung fuer alle Kommunikation",
        answerB = "Zero-Trust vertraut keinem Nutzer oder Geraet implizit, unabhaengig von Netzwerk-Position",
        answerC = "Zero-Trust ersetzt Firewalls durch Verhaltensanalyse mit KI",
        answerD = "Zero-Trust setzt voraus dass alle Endgeraete in einer DMZ betrieben werden",
        correctAnswer = 1,
        explanation = "Zero-Trust folgt dem Motto 'never trust, always verify': Jede Anfrage wird authentifiziert, autorisiert und kontinuierlich validiert — egal ob sie von innen oder aussen kommt. Damit wird die klassische Annahme aufgehoben, dass internes Netz gleich vertrauenswuerdig ist.",
        difficulty = 4,
        funFact = "John Kindervag praegte 2010 den Begriff 'Zero Trust' bei Forrester Research. Nach dem SolarWinds-Angriff 2020 hat die US-Regierung Zero-Trust-Architektur per Executive Order fuer alle Bundesbehoerden verpflichtend gemacht."
    ),

    // Question 7 — Cybersecurity: SSRF
    Question(
        categoryId = 7,
        questionText = "Welche Technik kann ein Angreifer bei einer Server-Side Request Forgery (SSRF)-Schwachstelle nutzen um Cloud-Instanz-Metadaten auszulesen?",
        answerA = "Ausnutzung von HTTP-Response-Splitting um Cache-Poisoning auszuloesen",
        answerB = "Anfragen an die link-lokale Adresse 169.254.169.254 die in AWS/GCP/Azure Metadaten bereitstellt",
        answerC = "DNS-Rebinding um dieselbe IP fuer interne und externe Hostnamen zu verwenden",
        answerD = "HTTP-CONNECT-Proxying durch den Webserver in das interne Netzwerk",
        correctAnswer = 1,
        explanation = "Cloud-Anbieter stellen Instanz-Metadaten (Credentials, Tokens, IAM-Rollen) unter der link-lokalen Adresse 169.254.169.254 bereit. Bei SSRF kann ein Angreifer den Server dazu bringen, diese Adresse abzufragen und temporaere AWS-Zugangsdaten zu stehlen.",
        difficulty = 4,
        funFact = "Der Capital One-Datenbreach 2019 mit 100 Millionen betroffenen Kunden wurde durch eine SSRF-Schwachstelle in einer WAF ausgefuehrt, die es ermoeglichte, EC2-Instanz-Metadaten und damit IAM-Credentials abzurufen."
    ),

    // Question 8 — Virtualisierung: Hypervisor-Typen
    Question(
        categoryId = 7,
        questionText = "Was ist der wesentliche Unterschied zwischen einem Typ-1 und einem Typ-2 Hypervisor hinsichtlich Privilegienstufe?",
        answerA = "Typ-1 laeuft im Ring 0 direkt auf Hardware, Typ-2 laeuft als Prozess im Benutzer-Modus auf einem Host-OS",
        answerB = "Typ-1 unterstuetzt nur Paravirtualisierung, Typ-2 nur vollstaendige Virtualisierung",
        answerC = "Typ-2 hat direkten Hardwarezugriff, Typ-1 ist auf Software-Emulation angewiesen",
        answerD = "Typ-1 verwendet Intel VT-x, Typ-2 AMD-V als Hardwareunterstuetzung",
        correctAnswer = 0,
        explanation = "Typ-1-Hypervisoren (VMware ESXi, Hyper-V, Xen) laufen direkt auf der Hardware in Ring 0 ohne Host-Betriebssystem. Typ-2-Hypervisoren (VirtualBox, VMware Workstation) laufen als Anwendung im Host-OS und haben damit mehr Latenz und Overhead.",
        difficulty = 4,
        funFact = "Der Begriff 'Hypervisor' stammt aus IBM-Mainframe-Zeiten der 1960er Jahre. Der VM/370-Hypervisor (1972) war einer der ersten, der mehrere OS-Instanzen auf einer Hardware isoliert betrieb."
    ),

    // Question 9 — Virtualisierung: Container vs VM
    Question(
        categoryId = 7,
        questionText = "Welche Linux-Kernel-Features bilden die technische Basis fuer Container-Isolation in Docker?",
        answerA = "KVM und QEMU fuer Hardware-Virtualisierung auf Prozessebene",
        answerB = "Namespaces fuer Isolation und cgroups fuer Ressourcenbeschraenkung",
        answerC = "SELinux-Mandatory-Access-Control und AppArmor-Profile",
        answerD = "eBPF-Programme und XDP fuer Netzwerk-Isolation",
        correctAnswer = 1,
        explanation = "Docker-Container nutzen Linux-Namespaces (PID, NET, MNT, UTS, IPC, USER) fuer Prozess-Isolation und Control Groups (cgroups) fuer die Begrenzung von CPU, RAM und I/O-Ressourcen. Es gibt keinen eigenen Kernel pro Container.",
        difficulty = 4,
        funFact = "LXC (Linux Containers) existierte bereits vor Docker und nutzt dieselben Kernel-Mechanismen. Docker hat 2013 Popularitaet gewonnen durch das Image-Format und die Toolchain, nicht durch neue Kernel-Technologie."
    ),

    // Question 10 — Microservices: Service Mesh
    Question(
        categoryId = 7,
        questionText = "Welche Aufgabe uebernimmt der Sidecar-Proxy in einem Service-Mesh wie Istio?",
        answerA = "Er ersetzt den API-Gateway und uebernimmt die gesamte externe Authentifizierung",
        answerB = "Er verwaltet Service-Discovery durch Abfrage von etcd oder Consul",
        answerC = "Er interceptiert den gesamten ein- und ausgehenden Netzwerkverkehr des Pods fuer mTLS, Observability und Traffic-Management",
        answerD = "Er fuehrt Health-Checks durch und entscheidet ob ein Pod aus dem Load-Balancer entfernt wird",
        correctAnswer = 2,
        explanation = "Der Envoy-Sidecar-Proxy in Istio wird jedem Pod beigefuegt und interceptiert via iptables-Regeln den gesamten Netzwerkverkehr. Er implementiert mTLS, Distributed Tracing, Circuit-Breaking und Retries ohne Aenderung des Anwendungscodes.",
        difficulty = 4,
        funFact = "Das Sidecar-Muster stammt aus der Welt der Motorraeder: Ein Beiwagenfahrer kann Aufgaben uebernehmen ohne den Fahrer abzulenken. Envoy wurde urspruenglich bei Lyft entwickelt und 2016 als Open Source veroeffentlicht."
    ),

    // Question 11 — Microservices: Saga-Pattern
    Question(
        categoryId = 7,
        questionText = "Welchen Konsistenzansatz verwendet das Saga-Pattern fuer verteilte Transaktionen in Microservices?",
        answerA = "Two-Phase-Commit mit einem zentralen Koordinator-Service",
        answerB = "Eventual Consistency durch Kompensations-Transaktionen bei Fehlern",
        answerC = "Serielle Ausfuehrung aller Transaktionen in einem single-threaded Event-Loop",
        answerD = "Verteiltes Locking mit Lease-basiertem Timeout",
        correctAnswer = 1,
        explanation = "Das Saga-Pattern teilt eine verteilte Transaktion in lokale Transaktionen pro Service auf. Bei einem Fehler werden bereits ausgefuehrte Schritte durch Kompensations-Transaktionen rueckgaengig gemacht, anstatt einen globalen Rollback zu versuchen. Das ergibt Eventual Consistency statt ACID.",
        difficulty = 4,
        funFact = "Das Saga-Pattern wurde 1987 von Hector Garcia-Molina und Kenneth Salem in einem Datenbankpaper beschrieben — lange bevor Microservices ein Begriff waren. Es wurde fuer long-lived transactions in monolithischen Datenbanken entwickelt."
    ),

    // Question 12 — API-Design: REST vs GraphQL
    Question(
        categoryId = 7,
        questionText = "Was ist das N+1-Problem in GraphQL und wie loest DataLoader es?",
        answerA = "N+1 beschreibt die ueberfluessige Serialisierung von N Feldern plus dem Root-Objekt; DataLoader batcht Schema-Compilierungen",
        answerB = "N+1 entsteht wenn fuer eine Liste von N Objekten je eine separate Datenbankabfrage ausgefuehrt wird; DataLoader batcht und dedupliziert Queries",
        answerC = "N+1 beschreibt zu viele Resolver-Ebenen in verschachtelten Queries; DataLoader begrenzt die Rekursionstiefe",
        answerD = "N+1 ist ein HTTP/2-Multiplexing-Problem bei parallelen GraphQL-Anfragen",
        correctAnswer = 1,
        explanation = "Beim N+1-Problem loest ein GraphQL-Resolver fuer jedes Objekt in einer Liste eine eigene Datenbankabfrage aus. DataLoader (von Facebook) sammelt alle Anfragen eines Event-Loop-Ticks, fuehrt sie als eine einzige Batch-Query aus und verteilt die Ergebnisse.",
        difficulty = 4,
        funFact = "DataLoader wurde 2015 von Lee Byron bei Facebook entwickelt. Das gleiche Prinzip existiert in anderen Sprachen unter Namen wie 'Fetch' (Haskell/Hasql) oder 'Absinthe Dataloader' (Elixir/Phoenix)."
    ),

    // Question 13 — API-Design: gRPC
    Question(
        categoryId = 7,
        questionText = "Welchen Serialisierungsformat verwendet gRPC standardmaessig und welchen Hauptvorteil bietet es gegenueber JSON?",
        answerA = "MessagePack — kleinere Binaerdateien bei identischer JSON-Semantik",
        answerB = "Protocol Buffers — typsicheres Schema mit kompakterem Binaerformat und schnellerer De-/Serialisierung",
        answerC = "Apache Avro — Schema-Evolution ohne Versionsnummern durch Fingerprint-basierte Kompatibilitaet",
        answerD = "CBOR — standardisiertes binaeres JSON-aequivalentes Format nach RFC 7049",
        correctAnswer = 1,
        explanation = "gRPC verwendet Protocol Buffers (protobuf) als Standard-Serialisierungsformat. Protobuf ist typsicher, schema-definiert (.proto-Dateien), erzeugt kleinere Nachrichten als JSON und ist 5-10x schneller bei De-/Serialisierung, da kein Text-Parsing noetig ist.",
        difficulty = 4,
        funFact = "Protocol Buffers werden intern bei Google seit 2001 verwendet und sind laut Google um den Faktor 3-10 kleiner und 20-100x schneller als XML. gRPC selbst wurde 2015 als Open-Source-Version des internen Stubby-Systems veroeffentlicht."
    ),

    // Question 14 — Software-Architektur: CQRS
    Question(
        categoryId = 7,
        questionText = "Was ist die Kernidee hinter Command Query Responsibility Segregation (CQRS)?",
        answerA = "Alle Datenbankzugriffe werden in einem Repository-Pattern gekapselt das Commands und Queries gleichermassen bedient",
        answerB = "Lese- und Schreiboperationen werden in separaten Modellen und oft separaten Datenspeichern getrennt",
        answerC = "Befehle werden asynchron per Message-Queue verarbeitet, Abfragen synchron per REST",
        answerD = "Das Domain-Modell wird in Command-Handler und Query-Handler aufgeteilt, die denselben Datenspeicher nutzen",
        correctAnswer = 1,
        explanation = "CQRS trennt das Schreibmodell (Commands, die den Zustand aendern) vom Lesemodell (Queries, die den Zustand abfragen). Oft wird ein separater, denormalisierter Read-Store fuer hochperformante Abfragen verwendet, der durch Events aus dem Write-Store befuellt wird.",
        difficulty = 4,
        funFact = "CQRS wurde von Greg Young 2010 popularisiert und baut auf Bertrand Meyers Command-Query-Separation-Prinzip (CQS) aus den 1980ern auf. Kombiniert mit Event Sourcing entsteht ein vollstaendig audit-faehiges System."
    ),

    // Question 15 — Software-Architektur: Event Sourcing
    Question(
        categoryId = 7,
        questionText = "Welches Problem loest Event Sourcing gegenueber traditioneller CRUD-Persistenz?",
        answerA = "Es vermeidet optimistische Sperrkonflikte durch append-only Schreiboperationen",
        answerB = "Es speichert den vollstaendigen Aenderungsverlauf als unveraenderliche Event-Sequenz statt nur des aktuellen Zustands",
        answerC = "Es ermoeglicht horizontale Skalierung durch Sharding des Event-Stores nach Aggregate-ID",
        answerD = "Es eliminiert Object-Relational-Mapping-Overhead durch direkte Event-zu-Tabellen-Abbildung",
        correctAnswer = 1,
        explanation = "Event Sourcing speichert jeden Zustandswechsel als unveraenderliches Event im Event-Store. Der aktuelle Zustand wird durch Replay aller Events rekonstruiert. Dies ermoeglicht vollstaendige Auditierbarkeit, Zeitreisen in vergangene Zustaende und vereinfacht die Fehleranalyse.",
        difficulty = 4,
        funFact = "Git ist ein prominentes Beispiel fuer Event Sourcing: Jeder Commit ist ein unveraenderliches Event, der HEAD-Zustand ergibt sich durch Replay der gesamten Commit-Geschichte."
    ),

    // Question 16 — Algorithmen: Amortisierte Analyse
    Question(
        categoryId = 7,
        questionText = "Warum hat das Einfuegen in einen dynamischen Array (z.B. Java ArrayList) eine amortisierte Zeitkomplexitaet von O(1) obwohl gelegentlich O(n) benoetigt wird?",
        answerA = "Weil Java JIT-Optimierungen das Kopieren durch SIMD-Instruktionen beschleunigen",
        answerB = "Weil Verdoppelungs-Strategie sicherstellt dass teure Resize-Operationen exponentiell selten auftreten und die Kosten gleichmaessig verteilt werden",
        answerC = "Weil das Betriebssystem Virtual Memory nutzt und physisches Kopieren vermeidet",
        answerD = "Weil der Array nur wachst, nicht schrumpft, und deshalb die durchschnittliche Fuellrate konstant bleibt",
        correctAnswer = 1,
        explanation = "Bei jeder Verdopplung muessen n Elemente kopiert werden, aber der naechste Resize passiert erst nach weiteren n Einfuegungen. Durch die Potentialfunktion-Methode verteilt sich der Kopieraufwand auf alle n vorherigen Einfuegeoperationen, was O(1) amortisiert ergibt.",
        difficulty = 4,
        funFact = "Die amortisierte Analyse wurde von Robert Tarjan 1985 formalisiert. Das Accounting-Method-Verfahren ist eine intuitive Alternative: Jede Einfuege-Operation 'bezahlt' auch 1 Token fuer eine kuenftige Kopier-Operation."
    ),

    // Question 17 — Algorithmen: Komplexitaet
    Question(
        categoryId = 7,
        questionText = "Was beweist die Existenz einer polynomialen Verifikation fuer NP-vollstaendige Probleme, und warum loest das P-vs-NP nicht?",
        answerA = "Polynomiale Verifikation impliziert polynomiale Loesung wenn Nichtdeterminismus zugelassen wird",
        answerB = "Polynomiale Verifikation einer Loesung garantiert nicht die polynomiale Findbarkeit einer Loesung — Erraten und Pruefen sind fundamental verschieden",
        answerC = "Polynomiale Verifikation existiert nur bei NP-Problemen die kein NP-hartes Reduktions-Ziel sind",
        answerD = "Polynomiale Verifikation beweist dass alle NP-Probleme auf SAT reduzierbar sind",
        correctAnswer = 1,
        explanation = "NP (Nondeterministic Polynomial) bedeutet: Ein geratener Kandidat kann in Polynomialzeit verifiziert werden. Ob man den Kandidaten auch in Polynomialzeit finden kann (P) ist das ungeloeste P-vs-NP-Problem. Verifikation und Findung sind algorithmuisch verschiedene Probleme.",
        difficulty = 4,
        funFact = "Das Millennium-Problem P vs NP ist mit einer Million Dollar dotiert. Die meisten Informatiker glauben P != NP, aber kein Beweis existiert. Ein polynomialer Algorithmus fuer SAT wuerde Kryptografie, Logistik und KI revolutionieren."
    ),

    // Question 18 — Algorithmen: Graphen
    Question(
        categoryId = 7,
        questionText = "Welche Datenstruktur ermoeglicht es, den Dijkstra-Algorithmus von O(V^2) auf O((V+E) log V) zu beschleunigen?",
        answerA = "Fibonacci-Heap als Priority-Queue mit amortisiertem O(1) Decrease-Key",
        answerB = "Binaerer Min-Heap als Priority-Queue statt eines unsortierten Arrays",
        answerC = "Segment-Tree fuer effiziente Bereichsminima-Abfragen der Kantenkanten",
        answerD = "Skip-List als Alternative zu binaeren Suchbaeumen fuer geordnete Distanzen",
        correctAnswer = 1,
        explanation = "Dijkstra mit einem unsortierten Array benoetigt O(V^2) fuer V Extract-Min-Operationen. Mit einem binaeren Min-Heap als Priority-Queue kostet jede Extract-Min-Operation O(log V), was die Gesamtkomplexitaet auf O((V+E) log V) senkt.",
        difficulty = 4,
        funFact = "Theoretisch optimal waere ein Fibonacci-Heap mit amortisiertem O(1) Decrease-Key, was O(V log V + E) ergibt. In der Praxis ist der binaere Heap aufgrund besserer Cache-Lokalitaet jedoch haeufig schneller als der Fibonacci-Heap."
    ),

    // Question 19 — Speicherverwaltung: Garbage Collection
    Question(
        categoryId = 7,
        questionText = "Warum benoetigt ein tri-color marking Garbage Collector eine Write-Barrier?",
        answerA = "Um Zirkelreferenzen zwischen alten und neuen Generationen zu erkennen",
        answerB = "Um zu verhindern dass ein laufendes Programm einen weissen Knoten direkt von einem schwarzen Knoten aus erreichbar macht ohne ihn grau zu markieren",
        answerC = "Um den GC-Thread zu pausieren wenn der Mutator-Thread Speicher alloziert",
        answerD = "Um den Stack-Scan effizienter zu gestalten wenn der GC concurrent laeuft",
        correctAnswer = 1,
        explanation = "Beim tri-color-Marking sind schwarze Knoten (fertig gescannt) und weisse Knoten (noch nicht erreicht) problematisch: Wenn der Mutator einen weissen Knoten direkt von einem schwarzen aus erreichbar macht (ohne ihn grau zu machen), wuerde der GC ihn faelschlicherweise als Muell einsammeln. Die Write-Barrier verhindert dies.",
        difficulty = 4,
        funFact = "Der Go-Garbage-Collector verwendet seit Version 1.5 einen concurrent tri-color-Marker mit hybridischer Write-Barrier (Dijkstra + Yuasa), was GC-Pausen von Sekunden auf unter 1 Millisekunde reduziert hat."
    ),

    // Question 20 — Speicherverwaltung: Memory Model
    Question(
        categoryId = 7,
        questionText = "Was beschreibt der Begriff 'false sharing' in der Parallelprogrammierung?",
        answerA = "Zwei Threads die versehentlich dieselbe globale Variable ohne Synchronisation lesen",
        answerB = "Zwei Threads die verschiedene Variablen modifizieren die auf derselben Cache-Line liegen und dadurch Cache-Invalidierungen ausloesen",
        answerC = "Ein Deadlock der durch zyklische Abhaengigkeiten zwischen zwei Threads entsteht",
        answerD = "Das Phaenomen dass JIT-Compiler Speicherzugriffe umordern und Linearisierbarkeit verletzen",
        correctAnswer = 1,
        explanation = "False Sharing tritt auf wenn zwei Threads unterschiedliche Variablen beschreiben, die zufaellig auf derselben Cache-Line (typisch 64 Bytes) liegen. Jede Schreiboperation invalidiert die Cache-Line des anderen CPU-Kerns und erzwingt Cache-Kohärenzverkehr, obwohl keine echte Datenteilung vorliegt.",
        difficulty = 4,
        funFact = "False Sharing kann Programme um Faktor 100 verlangsamen. Die Loesung ist Padding: Variablen werden auf eigene Cache-Lines ausgerichtet (alignment). In Java gibt es dafuer @Contended-Annotation, in C++ alignas()."
    ),

    // Question 21 — Parallelprogrammierung: Lock-Free
    Question(
        categoryId = 7,
        questionText = "Was garantiert ein lock-free Algorithmus im Gegensatz zu einem wait-free Algorithmus?",
        answerA = "Lock-free garantiert dass kein Thread unbegrenzt warten muss; wait-free garantiert nur Fortschritt fuer mindestens einen Thread",
        answerB = "Lock-free garantiert Systemfortschritt (mindestens ein Thread macht Fortschritt); wait-free garantiert individuellen Fortschritt fuer jeden Thread in begrenzter Zeit",
        answerC = "Lock-free Algorithmen verwenden keine Mutexe; wait-free verzichtet zusaetzlich auf CAS-Operationen",
        answerD = "Beide garantieren dasselbe, wait-free hat aber schwaecher Guarantees bei hoher Contention",
        correctAnswer = 1,
        explanation = "Lock-free bedeutet: Das System als Ganzes macht immer Fortschritt — mindestens ein Thread schreitet voran. Wait-free ist staerker: Jeder einzelne Thread schreitet in einer bounded Anzahl von Schritten voran, unabhaengig von anderen Threads.",
        difficulty = 4,
        funFact = "Maurice Herlihy entwickelte 1991 die Theorie der lock-free und wait-free Datenstrukturen. CAS (Compare-And-Swap) ist die fundamentale Primitive fuer lock-free Programmierung und ist auf moderner Hardware eine atomare Instruktion."
    ),

    // Question 22 — Parallelprogrammierung: Memory Ordering
    Question(
        categoryId = 7,
        questionText = "Was bedeutet 'Sequentially Consistent' (seq_cst) als Memory-Ordering-Constraint in C++ Atomics?",
        answerA = "Alle atomaren Operationen werden in der Compile-Zeit-Reihenfolge ausgefuehrt ohne Reordering",
        answerB = "Es existiert eine einzige totale Ordnung aller seq_cst-Operationen die von allen Threads beobachtet wird",
        answerC = "Schreiboperationen werden sofort fuer alle anderen Threads sichtbar ohne Cache-Flush-Verzoegerung",
        answerD = "Der Compiler darf keine Optimierungen an atomaren Variablen vornehmen",
        correctAnswer = 1,
        explanation = "seq_cst (sequentielle Konsistenz) garantiert eine globale totale Ordnung aller seq_cst-Operationen ueber alle Threads hinweg. Jeder Thread sieht dieselbe Reihenfolge. Dies ist die staerkste und teuerste Garantie, da sie oft Memory-Fence-Instruktionen erfordert.",
        difficulty = 4,
        funFact = "Leslie Lamport definierte sequentielle Konsistenz 1979. Auf modernen CPUs kostet seq_cst typisch eine MFENCE-Instruktion auf x86, die den Memory-Subsystem-Buffer leert und sehr teuer ist (hunderte Zyklen)."
    ),

    // Question 23 — Netzwerk: TCP
    Question(
        categoryId = 7,
        questionText = "Welchen Mechanismus verwendet TCP um bei einer Netzwerkueberlastung die Senderate zu reduzieren, bevor explizites ECN-Feedback vorliegt?",
        answerA = "Slow-Start: Die Congestion-Window wird halbiert wenn ein Timeout eintritt",
        answerB = "Paket-Verlust-Erkennung: TCP interpretiert Paketverlust (Timeout oder 3 doppelte ACKs) als Ueberlastungssignal und reduziert Congestion-Window",
        answerC = "Bandwidth-Delay-Product-Messung: TCP schraubt die Window-Size auf das optimale BDP herunter",
        answerD = "RTT-Varianz-Monitoring: Steigt die RTT-Varianz, wird das Fenster geometrisch verkleinert",
        correctAnswer = 1,
        explanation = "TCP-Congestion-Control (Reno/Cubic) interpretiert Paketverlust als Stausignal. Bei 3 doppelten ACKs (Fast Retransmit) wird die Congestion-Window halbiert (Fast Recovery). Bei Timeout wird auf 1 MSS (Maximum Segment Size) zurueckgesetzt und Slow-Start neu gestartet.",
        difficulty = 4,
        funFact = "BBR (Bottleneck Bandwidth and RTT) ist Googles modernes TCP-Congestion-Control-Verfahren aus 2016, das RTT und Bandbreite modelliert statt Verlust zu beobachten. Es ist auf Google-Servern aktiv und kann Throughput um Faktor 2700x verbessern."
    ),

    // Question 24 — Netzwerk: TLS
    Question(
        categoryId = 7,
        questionText = "Was ist der Zweck von Perfect Forward Secrecy (PFS) in TLS und welche Key-Exchange-Methoden bieten es?",
        answerA = "PFS verschluesselt den privaten Schluessel des Servers mit einem temporaeren Sitzungsschluessel",
        answerB = "PFS stellt sicher dass vergangene Sitzungen nicht entschluesselt werden koennen wenn der langfristige Server-Schluessel spaeter kompromittiert wird, durch ephemere Diffie-Hellman-Schluesseltausch-Verfahren (ECDHE, DHE)",
        answerC = "PFS rotiert den Server-Schluessel nach jeder TLS-Session automatisch",
        answerD = "PFS verwendet Certificate-Pinning um Man-in-the-Middle-Angriffe zu verhindern",
        correctAnswer = 1,
        explanation = "Mit PFS (ECDHE/DHE) werden fuer jede TLS-Session ephemere Schluesselepaare generiert die nach der Sitzung vernichtet werden. Selbst wenn ein Angreifer den langfristigen privaten Schluessel des Servers erlangt, kann er aufgezeichnete vergangene Sitzungen nicht entschluesseln.",
        difficulty = 4,
        funFact = "Ohne PFS (z.B. bei RSA-Key-Exchange) konnte die NSA laut Snowden-Dokumenten massenhaft verschluesselten TLS-Traffic aufzeichnen und spaeter entschluesseln wenn der Server-Schluessel erlangt wurde. TLS 1.3 hat RSA-Key-Exchange vollstaendig gestrichen."
    ),

    // Question 25 — Betriebssysteme: Scheduling
    Question(
        categoryId = 7,
        questionText = "Was ist das Priority-Inversion-Problem und wie loest Priority Inheritance es?",
        answerA = "Ein hochprioritaerer Thread wartet auf eine Ressource die ein niederprioritaerer Thread haelt; Priority Inheritance erhoehe temporaer die Prioritaet des Halters",
        answerB = "Zwei Threads der gleichen Prioritaet wechseln sich im Round-Robin ab; Priority Inheritance vergibt einen Bonus an den laenger wartenden Thread",
        answerC = "Ein Thread erbt die Prioritaet seines Eltern-Threads; das Problem tritt auf wenn Kinder blockiert werden",
        answerD = "Der Scheduler bevorzugt I/O-gebundene Threads; Priority Inheritance gleicht CPU-gebundene Threads aus",
        correctAnswer = 0,
        explanation = "Priority Inversion: Thread H (hoch) wartet auf Mutex den Thread L (niedrig) haelt. Thread M (mittel) preemptiert L und blockiert indirekt H. Priority Inheritance loest dies indem L temporaer Hs Prioritaet erbt solange es den Mutex haelt.",
        difficulty = 4,
        funFact = "Der Mars Pathfinder Rover 1997 hatte ein schwerwiegendes Priority-Inversion-Problem das zu System-Resets fuehrte. Der Bug wurde im Betrieb durch Aktivierung von Priority Inheritance im VxWorks-RTOS per Upload gefixt."
    ),

    // Question 26 — Betriebssysteme: Virtual Memory
    Question(
        categoryId = 7,
        questionText = "Was ist Thrashing im Kontext von virtuellem Speicher und welche CPU-Architektur-Metrik signalisiert es?",
        answerA = "Thrashing ist exzessives Swapping von Pages zwischen RAM und Disk; gemessen als hohe Page-Fault-Rate",
        answerB = "Thrashing ist Cache-Invalidierung durch uebermaessige Context-Switches; gemessen als TLB-Miss-Rate",
        answerC = "Thrashing ist Memory-Fragmentierung durch viele kleine Allokatoren; gemessen als externe Fragmentierungsrate",
        answerD = "Thrashing ist ueberlasteter Virtual-Memory-Subsystem durch zu viele gemappte Dateien; gemessen als mmap-Fehlerrate",
        correctAnswer = 0,
        explanation = "Thrashing tritt auf wenn das Working-Set der aktiven Prozesse den physischen RAM uebersteigt. Das OS verbringt mehr Zeit mit Page-In/Page-Out als mit nuetzlicher Arbeit. Sichtbar durch hohe Page-Fault-Rate (Major Faults die Disk-I/O erfordern) und niedrige CPU-Auslastung trotz Last.",
        difficulty = 4,
        funFact = "Peter Denning beschrieb 1968 das Working-Set-Modell als Loesung fuer Thrashing: Nur Prozesse werden ausgefuehrt deren Working-Set vollstaendig in den RAM passt. Moderne OS nutzen Working-Set-basiertes Page-Replacement."
    ),

    // Question 27 — Datenbanken: Indexierung
    Question(
        categoryId = 7,
        questionText = "Warum ist ein B+-Baum einem B-Baum fuer Datenbankindizes vorzuziehen?",
        answerA = "B+-Baeume unterstuetzen mehr Schreiboperationen pro Sekunde durch Write-Ahead-Logging",
        answerB = "Alle Datensaetze liegen im B+-Baum in den Blaettern und sind verkettet, was Range-Scans effizienter macht als beim B-Baum der Daten in allen Knoten speichert",
        answerC = "B+-Baeume haben eine hoehere Faktor-Ordnung als B-Baeume und sind daher flacher",
        answerD = "B+-Baeume unterstuetzen mehrdimensionale Indexierung durch Split-Dimension-Heuristiken",
        correctAnswer = 1,
        explanation = "Im B+-Baum speichern innere Knoten nur Schluessellwerte (keine Datensaetze), was mehr Schluessel pro Knoten und damit kleinere Baumhoehe ermoeglicht. Alle Datensaetze liegen in den Blaettern die verkettet sind, was Range-Queries durch sequentielles Traversieren der Blatt-Ebene sehr effizient macht.",
        difficulty = 4,
        funFact = "PostgreSQL, MySQL (InnoDB), Oracle und SQL Server verwenden alle B+-Baeume als Standard-Indexstruktur. MongoDB verwendet ebenfalls B+-Baeume in seiner WiredTiger-Storage-Engine."
    ),

    // Question 28 — Datenbanken: MVCC
    Question(
        categoryId = 7,
        questionText = "Wie loest Multiversion Concurrency Control (MVCC) das Reader-Writer-Blocking-Problem?",
        answerA = "MVCC verwendet optimistisches Locking bei dem Konflikte erst beim Commit erkannt werden",
        answerB = "MVCC speichert mehrere Versionen jeder Zeile, so dass Leser immer eine konsistente Snapshot-Version sehen ohne Schreiber zu blockieren",
        answerC = "MVCC partitioniert Tabellen nach Transaktions-ID um parallele Zugriffe zu isolieren",
        answerD = "MVCC delegiert Konflikte an den Anwendungscode durch explizite Versionsnummern in jeder Zeile",
        correctAnswer = 1,
        explanation = "MVCC haelt mehrere Versionen jeder Datenzeile (mit Transaktions-IDs als Versionsstempel). Ein Leser sieht die Snapshot-Version zum Transaktionsstart-Zeitpunkt, waehrend Schreiber neue Versionen anlegen. Leser blockieren keine Schreiber und umgekehrt.",
        difficulty = 4,
        funFact = "PostgreSQL war die erste weit verbreitete Datenbank mit MVCC (1987, damals Postgres von Michael Stonebraker). InnoDB implementiert MVCC durch Undo-Logs die im Rollback-Segment gespeichert werden."
    ),

    // Question 29 — Compiler: JIT vs AOT
    Question(
        categoryId = 7,
        questionText = "Welchen Vorteil hat Just-in-Time-Compilation (JIT) gegenueber Ahead-of-Time-Compilation (AOT) zur Laufzeit?",
        answerA = "JIT-kompilierter Code startet schneller weil keine Kompilierungsphase vor der Ausfuehrung noetig ist",
        answerB = "JIT kann laufzeitspezifische Informationen (tatsaechliche Typen, Aufrufhaeufigkeiten) nutzen um aggressivere Optimierungen als AOT durchzufuehren",
        answerC = "JIT-Code ist kompakter weil der Compiler den tatsaechlichen Arbeitsspeicherlayout kennt",
        answerD = "JIT vermeidet Speicherfragmentierung durch Umordnung von Objekten waehrend der Kompilierung",
        correctAnswer = 1,
        explanation = "JIT-Compiler profitieren von Laufzeitprofiling-Daten: Sie kennen welche Methoden haeufig aufgerufen werden (Hot Paths), welche tatsaechlichen Typen bei polymorphen Aufrufen vorkommen, und koennen daraufhin spezialisierte, hochoptimierte Maschinen-Code-Varianten erzeugen.",
        difficulty = 4,
        funFact = "HotSpot JVM nutzt Tiered Compilation: Level 1 (interpretiert) → Level 2 (C1 leicht optimiert) → Level 4 (C2 aggressiv optimiert mit Profiling). C2 kann JIT-kompilierten Code erzeugen der schneller als equivalenter C++-Code ist."
    ),

    // Question 30 — Compiler: SSA
    Question(
        categoryId = 7,
        questionText = "Was ist Static Single Assignment (SSA) Form und welche Optimierungen werden dadurch vereinfacht?",
        answerA = "SSA ist eine IR-Form wo jede Variable genau einmal definiert wird; vereinfacht Constant Propagation, Dead-Code-Elimination und Register-Allokation",
        answerB = "SSA ist eine Optimierungstechnik die Variablen in CPU-Register einbuergert bevor Code generiert wird",
        answerC = "SSA ist eine Form von Loop-Unrolling wobei jede Iteration eigene Variablennamen erhaelt",
        answerD = "SSA ist eine Datenfluss-Analyse-Methode die Pointer-Aliasing zur Compile-Zeit entscheidet",
        correctAnswer = 0,
        explanation = "In SSA-Form wird jede Variable genau einmal zugewiesen. Bei Merging mehrerer Kontrollfluss-Pfade werden Phi-Funktionen eingefuegt. SSA vereinfacht Datenfluss-Analysen erheblich: Definitionen und Verwendungen sind direkt verknuepft, was Constant Propagation, Strength Reduction und Dead-Code-Elimination vereinfacht.",
        difficulty = 4,
        funFact = "SSA wurde 1988 bei IBM Research entwickelt und wird heute in LLVM, GCC, V8, HotSpot und praktisch allen modernen Compilern als interne IR verwendet. Phi-Funktionen an Knotenpunkten des Control-Flow-Graphs sind das zentrale Konzept."
    ),

    // Question 31 — Netzwerk-Sicherheit: DNSSEC
    Question(
        categoryId = 7,
        questionText = "Wie verifiziert DNSSEC die Authentizitaet eines DNS-Records ohne den Inhalt zu verschluesseln?",
        answerA = "DNSSEC verschluesselt DNS-Antworten mit TLS und der Resolver prueft das Server-Zertifikat",
        answerB = "DNSSEC signiert Resource-Record-Sets mit dem Zone-Private-Key; Resolver verifizieren die Signatur (RRSIG) anhand des oeffentlichen Zone-Keys (DNSKEY) der durch eine Vertrauenskette bis zum Root validiert wird",
        answerC = "DNSSEC verwendet HMAC-basierte Message-Authentication-Codes die zwischen Resolver und Authoritative-Server vereinbart werden",
        answerD = "DNSSEC prueft DNS-Records gegen eine Blockchain-gespeicherte Kopie der Zonendaten",
        correctAnswer = 1,
        explanation = "DNSSEC fuegt kryptografische Signaturen (RRSIG-Records) zu DNS-Zonen hinzu. Die Zone wird mit einem Zone-Signing-Key (ZSK) signiert, der selbst durch einen Key-Signing-Key (KSK) signiert ist. Der Root-Zone-KSK bildet den Vertrauensanker (Trust Anchor) fuer die gesamte Hierarchie.",
        difficulty = 4,
        funFact = "Der DNSSEC Root-KSK wird alle fuenf Jahre in einer aufwendigen Key-Rollover-Zeremonie gewechselt, die oeffentlich live uebertragen wird. Im Oktober 2018 fand der erste Root-KSK-Rollover statt — eine Panne haette Internet-Resolver fuer Millionen User kaputt gemacht."
    ),

    // Question 32 — Containerisierung: Kubernetes
    Question(
        categoryId = 7,
        questionText = "Welche Rolle spielt etcd in einem Kubernetes-Cluster?",
        answerA = "etcd ist der Container-Runtime-Interface-Adapter der containerd mit dem kubelet verbindet",
        answerB = "etcd ist der hochverfuegbare Key-Value-Store der den gesamten Cluster-Zustand speichert und als Single Source of Truth fuer alle Kubernetes-Komponenten dient",
        answerC = "etcd ist der Netzwerk-Plugin-Manager der CNI-Plugins koordiniert",
        answerD = "etcd ist ein Sidecar-Controller der Health-Checks fuer Pods durchfuehrt und bei Ausfaellen Neustarts ausfuehrt",
        correctAnswer = 1,
        explanation = "etcd ist ein verteilter Key-Value-Store basierend auf dem Raft-Konsens-Algorithmus. In Kubernetes ist etcd die einzige persistente Komponente: Alle Objekte (Pods, Services, ConfigMaps etc.) werden dort gespeichert. Verlust von etcd bedeutet Verlust des gesamten Cluster-Zustands.",
        difficulty = 4,
        funFact = "etcd wurde urspruenglich von CoreOS fuer die Konfigurationsverwaltung ihres Linux-Distributions-Clusters entwickelt. Der Name kommt von Unix /etc (Konfigurationsverzeichnis) + 'd' fuer distributed."
    ),

    // Question 33 — Kubernetes: Scheduling
    Question(
        categoryId = 7,
        questionText = "Wie entscheidet der Kubernetes-Scheduler auf welchem Node ein Pod platziert wird?",
        answerA = "Durch Round-Robin ueber alle verfuegbaren Nodes mit ausreichend freiem RAM",
        answerB = "Durch zwei Phasen: Filtering (welche Nodes erfuellen Constraints?) und Scoring (welcher Node ist am geeignetsten?) — der hoechst bewertete Node wird gewaehlt",
        answerC = "Der Node mit dem geringsten CPU-Verbrauch wird immer bevorzugt um homogene Last zu erreichen",
        answerD = "Pod-Placement wird von kubelet auf dem Zielnode entschieden, nicht vom zentralen Scheduler",
        correctAnswer = 1,
        explanation = "Der kube-scheduler arbeitet in zwei Phasen: Im Filtering-Schritt werden Nodes ausgeschlossen die Anforderungen nicht erfuellen (Ressourcen, Taints, NodeSelector, Affinitaeten). Im Scoring-Schritt werden verbleibende Nodes gewichtet bewertet (z.B. LeastRequestedPriority, BalancedResourceAllocation).",
        difficulty = 4,
        funFact = "Kubernetes-Scheduling ist NP-schwer im allgemeinen Fall (bin-packing-Problem). Der Scheduler verwendet daher Heuristiken. Fuer spezielle Anforderungen kann ein eigener Custom-Scheduler implementiert und neben dem Standard-Scheduler betrieben werden."
    ),

    // Question 34 — Security: OAuth 2.0
    Question(
        categoryId = 7,
        questionText = "Welchen Angriff verhindert der 'state'-Parameter im OAuth 2.0 Authorization-Code-Flow?",
        answerA = "Token-Hijacking durch verschluesselte Weiterleitung von Access-Tokens im URL-Fragment",
        answerB = "Cross-Site-Request-Forgery (CSRF) durch Bindung des Authorization-Code an die originale Client-Session",
        answerC = "Open-Redirect-Angriffe durch Signierung der Redirect-URI im Authorization-Request",
        answerD = "Replay-Angriffe durch Einmalbenutzbarkeit des Authorization-Codes",
        correctAnswer = 1,
        explanation = "Der state-Parameter ist ein CSRF-Schutz: Der Client generiert einen zufaelligen Wert, sendet ihn im Authorization-Request und prueft beim Callback ob der zurueckgegebene state-Wert identisch ist. Ohne diesen Check koennte ein Angreifer einen User dazu bringen, einen fremden Authorization-Code an seine Session zu binden.",
        difficulty = 4,
        funFact = "Der PKCE (Proof Key for Code Exchange)-Erweiterung fuer OAuth 2.0 loest das gleiche Problem auf elegantere Weise und macht state teilweise redundant. OAuth 2.1 macht PKCE verpflichtend fuer alle Clients einschliesslich vertraulicher Server-Clients."
    ),

    // Question 35 — Security: JWT
    Question(
        categoryId = 7,
        questionText = "Welche kritische Schwachstelle entsteht wenn ein JWT-Server den 'alg'-Header-Parameter ohne Validierung akzeptiert?",
        answerA = "Der Angreifer kann einen abgelaufenen Token reaktivieren indem er den exp-Claim manipuliert",
        answerB = "Der Angreifer kann 'alg: none' setzen um die Signatur vollstaendig zu entfernen und beliebige Claims zu faelschen",
        answerC = "Der Angreifer kann von RS256 auf HS256 wechseln und den oeffentlichen RSA-Schluessel als HMAC-Geheimnis verwenden",
        answerD = "Beide B und C sind bekannte JWT-Angriffsvektoren",
        correctAnswer = 3,
        explanation = "'alg: none' erlaubt unsignierte Token wenn der Server den Header nicht ablehnt. Der 'alg-confusion'-Angriff wechselt von RS256 zu HS256: Der Server verifiziert dann mit dem oeffentlichen RSA-Schluessel als HMAC-Secret, das der Angreifer kennt. Beide Angriffe wurden in vielen JWT-Libraries demonstriert.",
        difficulty = 4,
        funFact = "Tim McLean entdeckte 2015 die 'alg: none' und Algorithmen-Konfusions-Angriffe und veroeffentlichte sie als 'Critical vulnerabilities in JSON Web Token libraries'. Daraufhin wurden zahlreiche populaere Libraries gepatcht."
    ),

    // Question 36 — Datenstrukturen: Bloom Filter
    Question(
        categoryId = 7,
        questionText = "Was ist die Fehlercharakteristik eines Bloom-Filters und warum kann er trotzdem nuetzlich sein?",
        answerA = "Er hat False-Negatives aber keine False-Positives; nuetzlich wenn Fehlalarme akzeptabel sind",
        answerB = "Er hat False-Positives aber keine False-Negatives; nuetzlich als schnelle Vorfiltere die teure Lookups bei sicheren Misses vermeiden",
        answerC = "Er hat sowohl False-Positives als auch False-Negatives; nuetzlich durch einstellbare Fehlerrate",
        answerD = "Er hat keine Fehler bei korrekter Hash-Funktion-Wahl; Nutzbarkeit haengt von Kollisionsresistenz ab",
        correctAnswer = 1,
        explanation = "Ein Bloom-Filter kann 'vielleicht vorhanden' oder 'definitiv nicht vorhanden' sagen. False-Positives (faelschlich 'vorhanden' gemeldet) sind moeglich, aber False-Negatives nie. Damit kann er teure Lookups (z.B. Datenbankabfragen) sicher abkuerzen: Nur bei positivem Filter-Ergebnis muss die echte Datenbank befragt werden.",
        difficulty = 4,
        funFact = "Google BigTable, Apache Cassandra, PostgreSQL und viele CDNs verwenden Bloom-Filter um Lookups in leeren Sets zu vermeiden. Akamai nutzt Bloom-Filter um zu verhindern dass selten abgefragte Objekte den Cache verdraengen."
    ),

    // Question 37 — Datenstrukturen: LSM-Tree
    Question(
        categoryId = 7,
        questionText = "Welchen Schreib-/Lese-Kompromiss macht ein Log-Structured-Merge-Tree (LSM-Tree) im Vergleich zu einem B+-Baum?",
        answerA = "LSM optimiert Lesezugriffe durch Vorsortierfierung; B+-Baeume optimieren Schreibzugriffe durch Append-Only",
        answerB = "LSM optimiert Schreibzugriffe durch sequentielle Append-Writes; Lesezugriffe sind langsamer weil mehrere SSTables durchsucht werden muessen",
        answerC = "LSM hat bessere Lese- und Schreibperformance als B+-Baeume ist aber auf SSDs nicht einsetzbar",
        answerD = "LSM und B+-Baeume haben gleiche asymptotische Komplexitaet, LSM ist nur auf verteilten Systemen effizienter",
        correctAnswer = 1,
        explanation = "LSM-Trees puffern Schreiboperationen im Speicher (MemTable) und schreiben sie sequentiell als sortierte SSTable-Dateien auf Disk. Sequentielle Writes sind sehr schnell. Lesezugriffe muessen aber mehrere SSTable-Ebenen und den MemTable absuchen, was durch Bloom-Filter und Compaction amortisiert wird.",
        difficulty = 4,
        funFact = "RocksDB (Meta/Facebook), Apache Cassandra, LevelDB, InfluxDB und Apache HBase verwenden alle LSM-Trees. Das Compaction-Problem (das Zusammenfuehren von SSTables) ist eine der schwierigsten Designentscheidungen bei LSM-Tree-basierten Storage-Engines."
    ),

    // Question 38 — Betriebssystem: eBPF
    Question(
        categoryId = 7,
        questionText = "Was ermoeglicht eBPF in Linux-Kerneln und was unterscheidet es von traditionellen Kernel-Modulen?",
        answerA = "eBPF ermoeglicht sichere Ausfuehrung von verifizierten Programmen im Kernel-Kontext ohne Kernel-Neukompilierung, mit Sandbox-Sicherheit durch Verifier",
        answerB = "eBPF ist ein Netzwerk-Framework fuer hochperformante Paketverarbeitung das nur im Netzwerk-Stack verwendet werden kann",
        answerC = "eBPF ermoeglicht Userspace-Treiber die direkt auf Hardware zugreifen ohne Kernel-Intervention",
        answerD = "eBPF ist eine JIT-Erweiterung fuer den Linux-Kernel die bestehende Kernel-Module automatisch beschleunigt",
        correctAnswer = 0,
        explanation = "eBPF-Programme werden vor der Ausfuehrung durch einen Verifier statisch analysiert (kein unendlicher Loop, kein ungueltige Speicherzugriff). Sie laufen im Kernel-Kontext und koennen auf Kernel-Datenstrukturen zugreifen, sind aber sicherer als Kernel-Module die beliebigen Code ausfuehren koennen.",
        difficulty = 4,
        funFact = "eBPF wird von Cilium fuer Kubernetes-Networking, von Meta fuer Load-Balancing, von Netflix fuer Performance-Tracing und von Linux-Security-Tools wie Falco verwendet. Linus Torvalds bezeichnete eBPF als 'die wichtigste Kernel-Technologie der letzten Dekade'."
    ),

    // Question 39 — Distributed Systems: Konsistenzmodelle
    Question(
        categoryId = 7,
        questionText = "Was beschreibt Linearizability (Atomizitaet) als Konsistenzmodell in verteilten Systemen?",
        answerA = "Alle Operationen werden global in einer einzigen totalen Reihenfolge ausgefuehrt die der real-time-Reihenfolge entspricht",
        answerB = "Schreiboperationen werden sofort fuer alle Repliken sichtbar ohne Propagations-Verzoegerung",
        answerC = "Lesende Operationen sehen immer den Zustand nach dem letzten Commit in der Transaktionshistorie",
        answerD = "Alle Threads eines Prozesses sehen Operationen in der Reihenfolge ihrer Programmreihenfolge",
        correctAnswer = 0,
        explanation = "Linearizability bedeutet: Jede Operation erscheint instantan an einem bestimmten Zeitpunkt zwischen ihrem Aufruf und ihrer Rueckkehr zu geschehen. Das Ergebnis ist wie eine einzelne sequentielle Ausfuehrung die mit den real-time-Uberlappungen aller Operationen konsistent ist.",
        difficulty = 4,
        funFact = "Linearizability ist staerker als Sequential Consistency (Lamport) und Serializability. Amazon DynamoDB und Google Spanner bieten Linearizability fuer Einzelobjekt-Operationen. Spanner erreicht globale Konsistenz durch GPS- und Atomic-Clock-basierte TrueTime-API."
    ),

    // Question 40 — Distributed Systems: Vector Clocks
    Question(
        categoryId = 7,
        questionText = "Was koennen Vector-Clocks (Lamport) erkennen, was skalare Lamport-Timestamps nicht koennen?",
        answerA = "Vector-Clocks koennen kausale Abhaengigkeiten zwischen Ereignissen erkennen und gleichzeitige (concurrent) Ereignisse identifizieren",
        answerB = "Vector-Clocks koennen physikalische Zeitunterschiede zwischen Knoten messen und kompensieren",
        answerC = "Vector-Clocks ermoeglichen eine totale Ordnung aller Ereignisse in einem verteilten System",
        answerD = "Vector-Clocks erkennen Byzantine-Fehler bei denen Knoten falsche Timestamps senden",
        correctAnswer = 0,
        explanation = "Lamport-Timestamps geben eine partielle Ordnung: Wenn A vor B passiert, gilt ts(A) < ts(B). Aber ts(A) < ts(B) impliziert nicht zwingend kausale Abhaengigkeit. Vector-Clocks speichern einen Zeitstempel pro Knoten und koennen exakt bestimmen ob Ereignisse kausal abhaengig oder konkurrent sind.",
        difficulty = 4,
        funFact = "Vector-Clocks wurden 1988 unabhaengig von Colin Fidge und Friedemann Mattern entwickelt. Amazon Dynamo verwendete Vector-Clocks fuer Konflikterkennnung bei gleichzeitigen Schreiboperationen, wechselte aber 2012 zu vereinfachten Dotted-Version-Vectors."
    ),

    // Question 41 — Security: Side-Channel-Angriffe
    Question(
        categoryId = 7,
        questionText = "Wie nutzt ein Timing-Side-Channel-Angriff gegen eine RSA-Implementierung den Zeitverbrauch aus?",
        answerA = "Der Angreifer misst die Gesamtlaufzeit der Entschluesselung und leitet daraus die Schluessellange ab",
        answerB = "Unterschiedliche Zeitverbraeuche bei der Square-and-Multiply-Operation je nach Bit-Wert des privaten Schluessels erlauben bit-weises Rekonstruieren des Schluessels",
        answerC = "Der Angreifer unterbricht die Entschluesselung nach fester Zeit und leitet den Zwischenzustand des Registers ab",
        answerD = "Timing-Angriffe messen Netzwerklatenz-Schwankungen die durch CPU-Cache-Belegung verursacht werden",
        correctAnswer = 1,
        explanation = "Bei der Modular-Exponentiation mit Square-and-Multiply dauert die Multiplikation laenger als das Quadrieren. Da Multiplikation nur bei Bit=1 des Exponenten ausgefuehrt wird, koennen praezise Zeitmessungen (Nanosekunden) bit-weise den privaten Schluessel rekonstruieren.",
        difficulty = 4,
        funFact = "Paul Kocher beschrieb 1996 Timing-Angriffe auf RSA. Die Gegenmassnahme ist 'constant-time' Implementierung: alle Code-Pfade muessen gleich lang dauern. OpenSSL und NSS verwenden Montgomery-Multiplication mit Blinding-Faktoren als Gegenmasssnahme."
    ),

    // Question 42 — Security: Spectre/Meltdown
    Question(
        categoryId = 7,
        questionText = "Welcher CPU-Mechanismus ist die fundamentale Ursache der Meltdown-Schwachstelle?",
        answerA = "Hyper-Threading teilt CPU-Mikroarchitektur-State zwischen Threads unterschiedlicher Sicherheitsstufen",
        answerB = "Out-of-Order-Execution fuehrt Speicherzugriffe auf privilegierte Speicherbereiche spekulativ aus bevor Berechtigungen geprueft wurden — Ergebnisse bleiben als Cache-Seitenkanal sichtbar",
        answerC = "Branch-Prediction trainiert den CPU mit falschen Sprungzielen und leakt dabei geschuetzten Speicher",
        answerD = "DRAM-Row-Hammer erlaubt Bit-Flips in benachbarten Speicherzellen durch Hammering-Zugriffsmusters",
        correctAnswer = 1,
        explanation = "Meltdown: Der CPU fuehrt spekulativ Befehle aus, auch wenn Zugriffsrechte fehlen. Der Page-Fault tritt auf, aber der Wert des unauthorisierten Speicherzugriffs liegt noch im CPU-Cache. Durch Cache-Timing (Flush+Reload) kann ein Angreifer diesen Wert auslesen. Betrifft hauptsaechlich Intel-CPUs.",
        difficulty = 4,
        funFact = "Meltdown und Spectre wurden im Dezember 2017 von Google Project Zero und unabhaengigen Forschern oeffentlich gemacht. Die Patches (KPTI/Kaiser fuer Meltdown) reduzierten Performance von bestimmten Workloads um 5-30%. Seit dann finden Forscher regelmaessig neue Varianten."
    ),

    // Question 43 — Programmiersprachen: Typsysteme
    Question(
        categoryId = 7,
        questionText = "Was ist der Unterschied zwischen strukturellem und nominellem Typsystem in einer Programmiersprache?",
        answerA = "Strukturelle Typsysteme prufen Typkompatibilitaet anhand der Struktur (Felder, Methoden); nominelle Typsysteme nur anhand des deklarierten Typnamens",
        answerB = "Strukturelle Typsysteme erlauben implizite Typkonvertierung; nominelle Typsysteme verlangen explizites Casting",
        answerC = "Strukturelle Typsysteme sind dynamisch; nominelle Typsysteme sind statisch und werden zur Compile-Zeit geprueft",
        answerD = "Strukturelle Typsysteme erlauben Mehrfachvererbung; nominelle Typsysteme sind auf Einfachvererbung beschraenkt",
        correctAnswer = 0,
        explanation = "Beim strukturellen Typsystem (Go, TypeScript) ist ein Typ kompatibel wenn er alle erforderlichen Felder und Methoden besitzt — unabhaengig vom Namen. Beim nominellen Typsystem (Java, C#, Rust) muessen Typen explizit als kompatibel deklariert werden (implements, extends).",
        difficulty = 4,
        funFact = "Go verwendet strukturelles Typsystem fuer Interfaces: Ein Typ implementiert ein Interface automatisch wenn er alle Methoden hat, ohne 'implements' zu schreiben. Das ermoeglicht retroaktive Anpassung an Interfaces die nach dem Typ erstellt wurden."
    ),

    // Question 44 — Programmiersprachen: Ownership
    Question(
        categoryId = 7,
        questionText = "Welches Problem loest Rusts Ownership-System ohne Garbage Collector?",
        answerA = "Es verhindert Integer-Overflow durch Compile-Zeit-Pruefung aller arithmetischen Operationen",
        answerB = "Es garantiert Memory-Safety und verhindert Data-Races durch Compile-Zeit-Analyse von Speicherbesitz und Ausleihe-Regeln",
        answerC = "Es eliminiert Null-Pointer-Exceptions durch ein algebraisches Option-Typsystem",
        answerD = "Es verwaltet automatisch Thread-Pooling durch den Borrow-Checker",
        correctAnswer = 1,
        explanation = "Rusts Ownership-System mit Borrow-Checker prueft zur Compile-Zeit: Jeder Wert hat genau einen Eigentuemer, Borrows koennen entweder mehrfach unveraenderlich oder einmalig veraenderlich sein. Das eliminiert Use-after-free, Double-free und Data-Races ohne Runtime-Overhead eines GC.",
        difficulty = 4,
        funFact = "Microsoft und Google haben unabhaengig berechnet, dass 70% ihrer Security-Patches auf Memory-Safety-Fehler zurueckgehen (use-after-free, buffer-overflow etc.). Rust eliminiert diese Klasse von Fehlern strukturell — deshalb wird es fuer Linux-Kernel, Android und Chrome-Teile eingesetzt."
    ),

    // Question 45 — Machine Learning Infrastruktur: GPU-Architektur
    Question(
        categoryId = 7,
        questionText = "Warum sind GPUs fuer Matrix-Multiplikation in neuronalen Netzen so viel effizienter als CPUs?",
        answerA = "GPUs haben hoeheren Takt und groesseren L3-Cache der Matrix-Daten cached",
        answerB = "GPUs haben tausende von einfacheren Kernen fuer massive Datenparallelitaet und hohe Speicherbandbreite (HBM) fuer matrix-typische Zugriffsmuster",
        answerC = "GPUs verwenden Fixkomma-Arithmetik die schneller als Gleitkomma-Arithmetik von CPUs ist",
        answerD = "GPUs haben spezialisierte Out-of-Order-Execution fuer lineare Algebra Instruktionen",
        correctAnswer = 1,
        explanation = "Matrix-Multiplikation ist hochgradig parallel: Jede Ausgabezelle kann unabhaengig berechnet werden. GPUs mit tausenden CUDA/ROCm-Kernen und hoher HBM-Speicherbandbreite (TB/s) sind ideal. CPUs haben wenige maechtiger Kerne optimiert fuer sequentielle, komplexe Steuerfluesse.",
        difficulty = 4,
        funFact = "NVIDIA A100 hat 6912 CUDA-Kerne und 2 TB/s Speicherbandbreite (HBM2e). Spezialisierte Tensor-Cores koennen INT8-Matrix-Multiplikation mit 312 TeraOps/Sekunde ausfuehren. Zum Vergleich: Ein Server-CPU-Kern schafft etwa 1-2 GFlops bei Gleitkomma."
    ),

    // Question 46 — Netzwerk-Protokolle: HTTP/3
    Question(
        categoryId = 7,
        questionText = "Welches fundamentale Problem von TCP loest HTTP/3 durch den Wechsel zu QUIC?",
        answerA = "TCP-Overhead durch dreifachen Handshake wird durch QUIC-0RTT-Verbindungswiederaufnahme eliminiert",
        answerB = "Head-of-Line-Blocking auf Transport-Schicht: Bei einem verlorenen TCP-Segment blockiert der gesamte Stream; QUIC implementiert Stream-Multiplexing ohne diese Abhaengigkeit",
        answerC = "TCP unterstuetzt keine Verbindungsmigration bei IP-Aenderung (z.B. WLAN zu LTE-Wechsel)",
        answerD = "Sowohl A als auch B und C sind korrekte Probleme die QUIC loest",
        correctAnswer = 3,
        explanation = "QUIC loest mehrere TCP-Probleme gleichzeitig: Head-of-Line-Blocking (jeder QUIC-Stream ist unabhaengig, ein verlorenes Paket blockiert nicht andere Streams), Connection Migration (Connection-ID statt IP-Tupel), und 0-RTT Resume bei bekannten Servern.",
        difficulty = 4,
        funFact = "QUIC wurde 2012 bei Google entwickelt und ist seit 2021 als RFC 9000 standardisiert. Laut Google reduziert QUIC die Latenz bei YouTube um 9% und die Rebuffering-Rate um 18%. Meta und Cloudflare haben QUIC ebenfalls weitgehend ausgerollt."
    ),

    // Question 47 — DevOps: GitOps
    Question(
        categoryId = 7,
        questionText = "Was ist das Kernprinzip von GitOps und wie unterscheidet es sich von traditionellem CI/CD?",
        answerA = "GitOps automatisiert Deployments durch Webhooks die bei jedem Push ausgefuehrt werden",
        answerB = "Das Git-Repository ist die Single Source of Truth fuer den gewuenschten Infrastruktur-Zustand; ein Operator prueft kontinuierlich ob tatsaechlicher Zustand mit Git uebereinstimmt und korrigiert Abweichungen",
        answerC = "GitOps ersetzt CI/CD-Pipelines durch Git-native Merge-Request-basierte Genehmigungsworkflows",
        answerD = "GitOps bedeutet dass alle Infrastruktur-Aenderungen per Git-Commit dokumentiert werden muessen bevor manuelle Ausfuehrung erlaubt ist",
        correctAnswer = 1,
        explanation = "GitOps (Weaveworks, 2017) folgt dem Pull-Modell: Ein Operator (Argo CD, Flux) laeuft im Cluster und prueft kontinuierlich ob der Live-Zustand dem in Git definierten gewuenschten Zustand entspricht. Abweichungen werden automatisch korrigiert — 'desired state' Reconciliation statt imperativer Deployments.",
        difficulty = 4,
        funFact = "GitOps wurde 2017 von Alexis Richardson (Weaveworks) begruendet. Der Begriff leitet sich vom Unix-Philosophie 'everything is a file' ab: In GitOps ist Git das 'Filesystem' fuer Infrastrukturzustand. Argo CD ist mit ueber 15.000 GitHub-Stars das populaerste GitOps-Tool."
    ),

    // Question 48 — Betriebssysteme: Dateisysteme
    Question(
        categoryId = 7,
        questionText = "Was ist Copy-on-Write (CoW) als Dateisystem-Eigenschaft und welchen Vorteil bietet es fuer Snapshots?",
        answerA = "CoW kopiert Dateien bei jedem Lesevorgang in einen privaten Puffer um Schreibkonflikte zu vermeiden",
        answerB = "CoW schreibt veraenderte Datenbloecke nie ueber alte Bloecke sondern an neue Speicherorte; Snapshots sind deshalb sofortige Referenzen auf den alten Zustand ohne vollstaendiges Kopieren",
        answerC = "CoW erstellt bei jedem Commit eine vollstaendige Kopie des Dateisystems fuer atomare Rollbacks",
        answerD = "CoW komprimiert Datenduplikate automatisch indem identische Bloecke eine gemeinsame physische Kopie teilen",
        correctAnswer = 1,
        explanation = "CoW-Dateisysteme (ZFS, Btrfs, APFS) schreiben geaenderte Daten immer an neuen Speicherorten und aktualisieren dann Metadaten-Pointer. Snapshots sind nur Referenzen auf den alten Pointer-Baum — kostenlos zu erstellen und konsistent ohne I/O-Pause.",
        difficulty = 4,
        funFact = "ZFS mit CoW und Checksummen erkennt 'silent data corruption' (Bit-Rot) automatisch und kann mit Redundanz selbst reparieren. Apple's APFS (seit macOS 10.13 / iOS 10.3) basiert ebenfalls auf CoW und macht iPhone-Backups dadurch schneller."
    ),

    // Question 49 — Programmiersprachen: Garbage Collector Generationen
    Question(
        categoryId = 7,
        questionText = "Was ist die 'generational hypothesis' und warum rechtfertigt sie generationalen Garbage Collection?",
        answerA = "Neuere Objekte sind weniger wahrscheinlich referenziert als aeltere — daher werden junge Objekte seltener gescannt",
        answerB = "Die meisten Objekte sterben jung (haben kurze Lebensdauer) — daher lohnt es sich junge Objekte haeufig zu sammeln da die Ausbeute hoch ist",
        answerC = "Objekte der gleichen Generation haben wahrscheinlich Referenzen zueinander — daher koennen Generationen unabhaengig gescannt werden",
        answerD = "Grosse Objekte ueberleben laenger als kleine — daher werden verschiedene Groessenklassen in separaten Generationen verwaltet",
        correctAnswer = 1,
        explanation = "Die Generationenhypothese besagt: Die meisten Objekte werden kurz nach ihrer Alloziierung unerreichbar. Ein generationaler GC sammelt haeufig die 'Young Generation' (Eden, Survivor) wo die Ausbeute hoch ist, und seltener die 'Old Generation' wo langlebige Objekte effizient gespeichert werden.",
        difficulty = 4,
        funFact = "Die Generationenhypothese wurde 1984 von David Ungar bei der Entwicklung des Self-Sprachensystems empirisch beobachtet. JVMs Young-Generation-GC (Minor GC) laeuft typisch alle paar Sekunden und dauert Millisekunden; der Major GC viele Minuten auseinander."
    ),

    // Question 50 — Netzwerk: BGP
    Question(
        categoryId = 7,
        questionText = "Was ist ein BGP-Hijacking-Angriff und welches fundamentale Designproblem von BGP ermoeglicht ihn?",
        answerA = "Ein Angreifer fluted BGP-Router mit gefaelschten Keep-Alive-Paketen und verursacht Session-Drops",
        answerB = "Ein autonomes System annonciert oeffentlich IP-Prefixe die ihm nicht gehoeren; BGP vertraut Prefix-Ankuendigungen ohne kryptografische Verifikation des Eigentuemers",
        answerC = "Ein Angreifer nutzt AS-Path-Manipulation um Pakete durch kompromittierte Netzwerke umzuleiten ohne die Ziel-IP zu veraendern",
        answerD = "BGP-Hijacking nutzt lange Konvergenzzeiten aus um Pakete waehrend der Routingpfad-Updates abzufangen",
        correctAnswer = 1,
        explanation = "BGP wurde 1989 ohne Authentifizierung designed: Jedes AS kann beliebige IP-Prefixe annoncieren. Andere Router akzeptieren dies standardmaessig. RPKI (Resource Public Key Infrastructure) loest dies durch kryptografische Zertifikate die IP-Prefixe an AS-Nummern binden.",
        difficulty = 4,
        funFact = "BGP-Hijacking ist erschreckend haeufig: 2010 leitete China Telecom versehentlich 15% des weltweiten Internet-Traffics durch chinesische Router. 2018 wurde AWS-Route53-Traffic durch BGP-Hijacking fuer MyEtherWallet-Nutzer umgeleitet um Kryptowährungen zu stehlen."
    )
)

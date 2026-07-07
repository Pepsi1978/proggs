using System.Threading;
using System.Threading.Tasks;

namespace VoiceAgent.Core
{
    /// <summary>
    /// Ein spezialisierter Unteragent, den der Hauptagent ("Boss") fuer eine erkannte Aufgabe
    /// beauftragen kann. Dies ist das GRUNDGERUEST der Unteragenten-Vision (Manifest Baustein 2).
    ///
    /// EHRLICH: Computer Use — das echte Steuern des Rechners — ist bewusst NOCH NICHT Teil
    /// dieser Schnittstelle. Das ist sicherheitskritisch und eine eigene, abgesicherte Stufe.
    /// Aktuelle Unteragenten arbeiten rein in der App (z.B. etwas ins Gedaechtnis schreiben).
    /// </summary>
    public interface ISubAgent
    {
        /// <summary>Kurzer Name (erscheint in der Turn-Trace bei Delegation).</summary>
        string Name { get; }

        /// <summary>Wofuer der Unteragent zustaendig ist (fuer Mensch + spaetere LLM-Auswahl).</summary>
        string Description { get; }

        /// <summary>
        /// WIE man diesen Helfer anspricht: 1-2 konkrete Beispiel-Saetze + was dann passiert.
        /// ABSICHTLICH OHNE Default (Frank-Anforderung 2026-06-10): jeder neue Helfer MUSS sein
        /// Anwendungs-Wissen mitliefern, sonst kompiliert das Projekt nicht (Poka-Yoke Stufe 3 —
        /// das "Wie" kann nie wieder vergessen werden). Geht mit Name+Description LIVE in den
        /// System-Prompt des Boss (AgentCapabilities.BuildHelpersBlock) und ins Verstehens-Gehirn:
        /// der Agent kann dem Nutzer erklaeren, wie man ihn nutzt, und routet freie
        /// Formulierungen besser.
        /// </summary>
        string HowTo { get; }

        /// <summary>Ob dieser Unteragent die Aufgabe uebernehmen kann.</summary>
        bool CanHandle(string task);

        /// <summary>
        /// Die generische Default-Rueckfrage. Benannt, damit der Hauptagent erkennt, wann ein
        /// Helfer KEINE spezifische Frage liefert — dann darf er die Frage durch die VERSTANDENE
        /// Rueckfrage des Verstehens-Schritts ersetzen (Read-back statt Rohtext-Echo, Almanach 1.3).
        /// </summary>
        const string GenericConfirmationQuestion = "Soll ich das fuer dich erledigen?";

        /// <summary>
        /// Kurze, vorlesbare Verstaendnis-Rueckfrage, die der Hauptagent stellt, BEVOR der
        /// Unteragent die Aufgabe ausfuehrt (Manifest Abschnitt 6: erst zurueckfragen, dann tun).
        /// Default ist generisch; Unteragenten duerfen eine spezifischere Formulierung liefern.
        /// </summary>
        string ConfirmationQuestion(string task) => GenericConfirmationQuestion;

        /// <summary>
        /// Wenn true, macht der Unteragent seine Bestaetigung SELBST (mehrstufig, z.B. um erst den
        /// konkreten Befehl abzuleiten und ihn dann zu zeigen) — der Hauptagent ueberspringt dann
        /// das generische Gate und ruft den Unteragenten direkt. Default false (normales Gate).
        /// </summary>
        bool HandlesOwnConfirmation => false;

        /// <summary>
        /// True, solange der Unteragent auf eine Folge-Antwort (z.B. "ja ausfuehren") wartet. Der
        /// Hauptagent leitet die naechste Eingabe dann direkt an ihn weiter — auch wenn sie nicht
        /// als Aufgabe erkannt wird. Default false.
        /// </summary>
        bool AwaitingFollowup => false;

        /// <summary>Fuehrt die Aufgabe aus und liefert eine kurze, vorlesbare Antwort.</summary>
        Task<string> HandleAsync(string task, CancellationToken ct = default);
    }
}

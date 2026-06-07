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

        /// <summary>Ob dieser Unteragent die Aufgabe uebernehmen kann.</summary>
        bool CanHandle(string task);

        /// <summary>Fuehrt die Aufgabe aus und liefert eine kurze, vorlesbare Antwort.</summary>
        Task<string> HandleAsync(string task, CancellationToken ct = default);
    }
}

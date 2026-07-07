using System.Collections.Generic;
using VoiceAgent.Diagnostics;

namespace VoiceAgent.Core
{
    /// <summary>
    /// Verzeichnis der verfuegbaren Unteragenten. Der Hauptagent findet hier den passenden
    /// Helfer fuer eine erkannte Aufgabe. Spaeter kann der Hauptagent hier auch dynamisch
    /// neue Unteragenten registrieren (Manifest Baustein 2).
    /// </summary>
    public sealed class SubAgentRegistry
    {
        private readonly List<ISubAgent> _agents = new();

        public IReadOnlyList<ISubAgent> All => _agents;

        public void Register(ISubAgent agent)
        {
            _agents.Add(agent);
            Log.Info("Unteragent registriert", new { name = agent.Name, desc = agent.Description });
        }

        /// <summary>Findet den ersten zustaendigen Unteragenten fuer die Aufgabe (oder null).</summary>
        public ISubAgent? Find(string task)
        {
            foreach (var a in _agents)
            {
                // Ein defekter Unteragent darf die Suche nicht sprengen (Probe statt Crash).
                try { if (a.CanHandle(task)) return a; }
                catch (System.Exception ex) { Log.Error($"Unteragent '{a.Name}': CanHandle warf", ex); }
            }
            return null;
        }

        /// <summary>Findet einen Unteragenten anhand seines Namens (case-insensitiv) — fuer den LLM-Router.</summary>
        public ISubAgent? FindByName(string? name)
        {
            if (string.IsNullOrWhiteSpace(name)) return null;
            foreach (var a in _agents)
                if (string.Equals(a.Name, name, System.StringComparison.OrdinalIgnoreCase)) return a;
            return null;
        }
    }
}

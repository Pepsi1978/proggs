using System.Collections.Generic;

namespace VoiceAgent.Core
{
    /// <summary>
    /// Datengetriebene Definition eines benutzerdefinierten Unteragenten — der Bauplan eines
    /// Helfers OHNE eigenen Code. Aus so einer Definition entsteht zur Laufzeit ein
    /// <see cref="PromptSubAgent"/>. Wird als JSON persistiert (CustomAgentStore) und kann
    /// per Sprache ("bau mir einen Helfer der …") oder spaeter per GUI erzeugt werden.
    ///
    /// Bewusst eine Klasse mit settable Properties (nicht record) — das ist die robusteste,
    /// quellgenerator-freie Form fuer System.Text.Json (BP 3).
    /// </summary>
    public sealed class SubAgentDefinition
    {
        /// <summary>Kurzer Name des Helfers (erscheint in der Turn-Trace bei Delegation).</summary>
        public string Name { get; set; } = string.Empty;

        /// <summary>Wofuer der Helfer zustaendig ist (Mensch + LLM-Router lesen das).</summary>
        public string Description { get; set; } = string.Empty;

        /// <summary>Stichwoerter, bei denen der Helfer (als Fallback zum LLM-Router) zustaendig ist.</summary>
        public List<string> Triggers { get; set; } = new();

        /// <summary>Der System-Prompt, der das Verhalten des Helfers bestimmt (sein "Charakter").</summary>
        public string SystemPrompt { get; set; } = string.Empty;
    }
}

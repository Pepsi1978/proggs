using System;
using System.Collections.Generic;
using VoiceAgent.Services.Llm;

namespace VoiceAgent.Core
{
    /// <summary>Eine gespeicherte Unterhaltung (eine Session) inkl. Verlauf und laufender Zusammenfassung.</summary>
    public sealed class ChatSession
    {
        public string Id { get; set; } = Guid.NewGuid().ToString("N");
        public string Title { get; set; } = DefaultTitle;
        public DateTimeOffset CreatedAt { get; set; } = DateTimeOffset.Now;
        public DateTimeOffset UpdatedAt { get; set; } = DateTimeOffset.Now;
        public bool Pinned { get; set; }

        /// <summary>Verdichtete Zusammenfassung der bereits komprimierten aelteren Turns ("" = keine).</summary>
        public string Summary { get; set; } = "";

        /// <summary>Wortwoertlicher Gespraechsverlauf (User/Assistant).</summary>
        public List<LlmMessage> History { get; set; } = new();

        public const string DefaultTitle = "Neue Session";

        /// <summary>Leichte Kopie der Metadaten fuer die Sidebar.</summary>
        public SessionInfo ToInfo() => new(Id, Title, UpdatedAt, Pinned);
    }

    /// <summary>Leichte Metadaten fuer die Sidebar (ohne den Verlauf zu laden).</summary>
    public sealed record SessionInfo(string Id, string Title, DateTimeOffset UpdatedAt, bool Pinned);
}

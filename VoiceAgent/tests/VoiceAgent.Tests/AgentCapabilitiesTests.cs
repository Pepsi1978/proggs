using VoiceAgent.Core;

namespace VoiceAgent.Tests
{
    public class AgentCapabilitiesTests
    {
        // Im Test-Kontext liegt keine capabilities.md neben der Test-DLL -> der eingebaute
        // Fallback greift. Beide Faehigkeiten-Abschnitte muessen trotzdem vorhanden sein.
        [Fact]
        public void BuildBlock_HasHeaderAndBothSections()
        {
            var block = AgentCapabilities.BuildBlock();
            Assert.Contains("FAEHIGKEITEN", block);
            Assert.Contains("Das kann ich JETZT", block);
            Assert.Contains("Das kann ich NOCH NICHT", block);
        }

        [Fact]
        public void BuildBlock_MentionsReminders()
            => Assert.Contains("Erinnerung", AgentCapabilities.BuildBlock());

        // ---- LIVE-Helfer-Liste (Vorfall 2026-06-10: statische capabilities.md war veraltet,
        // der Agent verneinte den Helfer-Baumeister). Die Liste kommt aus der Registry und
        // muss JEDEN registrierten Helfer mit Name + Zustaendigkeit nennen. ----

        private sealed class FakeSub : ISubAgent
        {
            public string Name { get; init; } = "Fake";
            public string Description { get; init; } = "Tut Testdinge.";
            public string HowTo { get; init; } = "Sag 'mach Testdinge'.";
            public bool CanHandle(string task) => false;
            public Task<string> HandleAsync(string task, CancellationToken ct = default)
                => Task.FromResult("ok");
        }

        [Fact]
        public void BuildHelpersBlock_ListsEveryRegisteredAgentWithDescription()
        {
            var agents = new List<ISubAgent>
            {
                new FakeSub { Name = "Helfer-Baumeister", Description = "Legt auf Wunsch neue, eigene Helfer an." },
                new FakeSub { Name = "Erinnerung", Description = "Merkt sich Erinnerungen." },
            };
            var block = AgentCapabilities.BuildHelpersBlock(agents);
            Assert.Contains("EINSATZBEREITEN HELFER", block);
            Assert.Contains("Helfer-Baumeister: Legt auf Wunsch neue, eigene Helfer an.", block);
            Assert.Contains("Erinnerung: Merkt sich Erinnerungen.", block);
            Assert.Contains("verneine KEINEN", block);   // die Anweisung gegen falsches Verneinen
            // Das WIE muss mitkommen (Frank 2026-06-10): Anwendungs-Beispiele pro Helfer.
            Assert.Contains("So nutzt man ihn:", block);
            Assert.Contains("Sag 'mach Testdinge'.", block);
        }

        [Fact]
        public void BuildHelpersBlock_EmptyWhenNoAgents()
        {
            Assert.Equal(string.Empty, AgentCapabilities.BuildHelpersBlock(null));
            Assert.Equal(string.Empty, AgentCapabilities.BuildHelpersBlock(new List<ISubAgent>()));
        }
    }
}

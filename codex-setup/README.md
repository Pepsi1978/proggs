# Codex Setup

Dieser Ordner ist die plattformuebergreifende, synchronisierte Regelbasis fuer Codex.

Wofuer dieser Ordner da ist:
- nur fuer Codex
- nicht fuer Claude Code
- fuer die Synchronisation von Codex auf macOS zu Codex auf Windows
- fuer die Synchronisation von Codex auf Windows zu Codex auf macOS
- als gemeinsame, synchronisierte Regelquelle fuer Codex auf beiden Plattformen

Zweck:
- Regeln fuer Codex zwischen macOS und Windows synchron halten
- `Codex-Regeln/` als bisherigen Einzelordner ersetzen
- Claude-Code-Konfiguration bewusst davon trennen

Grundsatz:
- Dieser Ordner ist nur fuer Codex gedacht.
- `claude-code-setup/` ist nur fuer Claude Code gedacht und fuer Codex keine Regelquelle.
- Wenn Regeltexte an mehreren Stellen im Repository auseinanderlaufen, ist fuer Codex dieser Ordner die massgebliche lokale Referenz.

Git-Sonderregel fuer diesen Ordner:
- `codex-setup/` ist der einzige Ordner im Repository, in den Codex eigenstaendig committen und pushen darf.
- Der Zweck ist die plattformuebergreifende Synchronisation von Codex-Regeln zwischen macOS und Windows.
- Fuer alle anderen Ordner im Repository gilt weiter: kein `git push` ohne ausdrueckliche Benutzeranweisung.

Struktur:
- `rules/global.md`: dauerhafte globale Regeln fuer Codex auf allen Plattformen

Pfadbeispiele:
- macOS: `/Users/frank/Codex/codex-setup/`
- Windows: `C:\Users\barwa\Codex\codex-setup\`

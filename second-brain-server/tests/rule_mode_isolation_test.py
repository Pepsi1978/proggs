#!/usr/bin/env python3
"""Statischer Cross-Client-Test: Nur der explizite R-Modus darf Regeln anlegen."""
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
REPO = ROOT.parent


def check(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def main() -> int:
    agent = (ROOT / "agent" / "app.py").read_text(encoding="utf-8")
    dashboard_app = (ROOT / "dashboard" / "app.py").read_text(encoding="utf-8")
    dashboard_html = (ROOT / "dashboard" / "static" / "index.html").read_text(encoding="utf-8")
    settings = (REPO / "CortexAndroid" / "app" / "src" / "main" / "java" / "de" / "frank" / "cortex" / "data" / "SettingsStore.kt").read_text(encoding="utf-8")
    screen = (REPO / "CortexAndroid" / "app" / "src" / "main" / "java" / "de" / "frank" / "cortex" / "ui" / "chat" / "ChatScreen.kt").read_text(encoding="utf-8")
    viewmodel = (REPO / "CortexAndroid" / "app" / "src" / "main" / "java" / "de" / "frank" / "cortex" / "ui" / "chat" / "ChatViewModel.kt").read_text(encoding="utf-8")

    check('return m if m in {"auto", "smalltalk", "save", "rule", "search"}' in agent,
          "Server akzeptiert rule als eigenen Kontextmodus")
    check("rules.is_rule_request(user_text)" not in agent,
          "Kein normaler Agentenpfad darf Regelwörter heuristisch auswerten")
    check('"name": "schreibe_regel"' not in agent,
          "Allgemeiner Hauptagent besitzt kein verstecktes Regel-Schreibwerkzeug")
    check('mode = _norm_context_mode(context_mode)' in agent and
          'if mode == "rule":\n        return _process_rule_turn(user_text, pending, mode_revision)' in agent,
          "Regelentwurf ist an den expliziten R-Modus gebunden")
    check('"action": "rule_confirm"' in agent and 'pending": {"mode": "rule_confirm"' in agent,
          "Regelmodus behält die bestehende Ja/Nein-Bestätigung")
    check('pending.get("mode_revision", -1)' in agent and 'rules.add_rule' in agent,
          "Auch die finale Regelbestätigung ist an R und dessen Modus-Revision gebunden")
    check('search_only = mode == "search"' in agent and '"Harte Suchgrenze erzwingt query und verhindert Mutationen"' in agent,
          "Lupe besitzt eine harte serverseitige read-only Grenze")
    check('allowed = {"durchsuche_gedaechtnis", "lade_eintrag", "lies_eintrags_kategorien"}' in agent and
          'if used_memory and not search_only:' in agent,
          "Lupe erhält nur Qdrant-Lesewerkzeuge und schreibt keine Abrufmetadaten")
    check('mode != "rule" and pending and pending.get("mode") == "rule_confirm"' in agent,
          "Ein Regelentwurf wird beim Verlassen von R verworfen")
    check('pending.get("mode_revision", -1)' in agent and "context_mode_revision" in dashboard_app and
          "chatModeRevision" in dashboard_html and "contextModeRevision" in viewmodel,
          "Dashboard, Android und Server binden Regelbestätigungen an dieselbe Modus-Revision")
    # Seit 2026-07-14 verwirft der Android-Flush Outbox-Eintraege NICHT mehr per Revisions-
    # Vergleich: der globale Zaehler stieg bei JEDEM Moduswechsel (auch dem Auto-Reset direkt
    # nach dem Einreihen) und loeschte gemerkte Regel-Auftraege still — trotz gegenteiliger
    # Chat-Zusage. Ein eingereihtes Item ist ein bestaetigter Sende-Wunsch; die Bindung von
    # Regel-BESTAETIGUNGEN an die Modus-Revision erzwingt der Server selbst (mode_revision).
    check("BEWUSST KEINE Revisions-Pruefung mehr" in viewmodel and
          "context_mode_revision = item.contextModeRevision" in viewmodel and
          '"Veralteten Regel-Auftrag nach Moduswechsel verworfen"' not in viewmodel,
          "Android sendet gemerkte R-Outbox-Aufträge zuverlässig nach (kein stilles Verwerfen)")
    session_store = (REPO / "CortexAndroid" / "app" / "src" / "main" / "java" / "de" / "frank" / "cortex" / "data" / "ChatSessionStore.kt").read_text(encoding="utf-8")
    check('val options: List<ChatOption> = emptyList()' in session_store and
          'put("options", encodeOptions(message.options))' in session_store and
          'options = decodeOptions(cursor.getStringOrNull(12))' in session_store and
          'options = options.takeIf { it.isNotEmpty() }' in viewmodel,
          "Android persistiert Bestätigungsknöpfe verlustfrei über Prozessneustarts")
    check('label !is String || send !is String' in session_store and
          'val actionGap = ((actionRowWidth - 288.dp) / 5).coerceIn(0.dp, 9.dp)' in screen,
          "Optionswerte bleiben exakt und die Aktionszeile passt ihre Abstände auf schmalen Geräten an")
    # Seit 0.79.0 liegt die Chat-Validierung im gemeinsamen Helfer _build_chat_payload
    # (identisch fuer /api/chat UND /api/chat/stream) — der Anker folgt dem Refactor,
    # die Pruef-Intention (kein .strip() am Speicherinhalt) bleibt wortgleich.
    dashboard_chat_start = dashboard_app.index("def _build_chat_payload")
    dashboard_chat = dashboard_app[dashboard_chat_start:dashboard_app.index("\n@app.", dashboard_chat_start)]
    check('const text = ta.value;' in dashboard_html and 'if(!text.trim()) return;' in dashboard_html and
          'text = body.get("text") or ""' in dashboard_chat and 'if not text.strip():' in dashboard_chat and
          'text = (body.get("text") or "").strip()' not in dashboard_chat and
          'quote = user_text' in agent,
          "Zweiturn-Speicherinhalt bleibt inklusive äußerer Leerzeichen und Zeilenumbrüche wortwörtlich")
    process_turn = agent[agent.index("def _process_turn"):agent.index('@app.post("/chat"')]
    check('def _process_rule_turn' in agent and
          process_turn.index('if mode == "rule":\n        return _process_rule_turn') < process_turn.index('brain_categories()'),
          "R wird vollständig lokal vor jedem Qdrant-Kategorienabruf verarbeitet")
    check('if (actionRowWidth >= 342.dp) Spacer(Modifier.weight(1f))' in screen and
          '_SUCCESSFUL_CHAT_ACTIONS' in agent and agent.count('in _SUCCESSFUL_CHAT_ACTIONS') == 2,
          "Schmale Aktionszeile hat exakt fünf Abstände und Chat/Stream teilen Erfolgsaktionen")
    check("val contextMode: String = SettingsStore.contextMode" in viewmodel and
          ".putString(\"context_mode\", mode)" in settings,
          "Android persistiert Modus und Revision gemeinsam über Prozessneustarts")
    check('if mode == "rule":' in agent and 'memory_edit = False' in agent,
          "R verwirft manipulierte Qdrant-Editierparameter vor dem Regelentwurf")
    check('@app.post("/rules"' not in agent and 'text: "str | None"' not in agent,
          "Agent-API erlaubt weder direkte Regelanlage noch Textänderung außerhalb von R")
    check('def update_rule(path: str, rule_id: str, *, enabled: bool):' in (REPO / "second-brain-server" / "agent" / "rules.py").read_text(encoding="utf-8"),
          "Auch die Regel-Speicherschicht kann Texte außerhalb von R nicht ändern")

    check('("auto", "save", "rule", "search")' in dashboard_app,
          "Dashboard-Proxy reicht rule an den Agenten durch")
    search_at = dashboard_html.index('data-mode="search"')
    rule_at = dashboard_html.index('data-mode="rule"')
    save_at = dashboard_html.index('data-mode="save"')
    check(search_at < rule_at < save_at, "Dashboard zeigt R zwischen Lupe und Diskette")
    check('["auto","save","rule","search"]' in dashboard_html,
          "Dashboard behält den R-Modus im lokalen UI-Zustand")
    check('@app.post("/api/rules")' not in dashboard_app and 'chatRuleAdd' not in dashboard_html and
          'apiSend("/api/rules/"+encodeURIComponent(t.id),"PUT",{text:v})' not in dashboard_html,
          "Dashboard-Einstellungen können R weder bei Anlage noch Textbearbeitung umgehen")

    check('const val CONTEXT_MODE_RULE = "rule"' in settings and "CONTEXT_MODE_RULE ->" in settings,
          "Android besitzt Regelmodus und geschützten Modus-Prompt")
    android_search = screen.index('SettingsStore.CONTEXT_MODE_SEARCH to "Suchen"')
    android_rule = screen.index('SettingsStore.CONTEXT_MODE_RULE to "Regeln"')
    android_save = screen.index('SettingsStore.CONTEXT_MODE_SAVE to "Speichern"')
    check(android_search < android_rule < android_save, "Android zeigt R zwischen Lupe und Diskette")
    action_row = screen[screen.index("// Action row"):screen.index("private fun ContextPromptDialog")]
    check(screen.count("onOpenContextPrompts") == 3 and action_row.index("// G - Improve") < action_row.index("// K - Kontext-Prompts") < action_row.index("Spacer(Modifier.weight(1f))"),
          "Android-Kontextknopf liegt in der unteren Aktionszeile neben G")
    check("contextMode != SettingsStore.CONTEXT_MODE_RULE" in screen,
          "Titel und Kategorie werden im Regelmodus ausgeblendet")
    check("FlowRow(" in screen and 'contentDescription = "Kontext-Prompts öffnen"' in screen,
          "Android bricht die Modusgruppen schmalbildtauglich um und beschriftet K barrierefrei")
    check('option.send == "regel bearbeiten"' in screen and '"regel speichern: $text"' in screen,
          "Android kann einen Regelentwurf vor der Bestätigung bearbeiten")
    update_mode = viewmodel[viewmodel.index("fun updateContextMode"):viewmodel.index("fun updateResponseSize")]
    check("SettingsStore.CONTEXT_MODE_RULE" in update_mode and "else -> SettingsStore.CONTEXT_MODE_AUTO" in update_mode,
          "ViewModel validiert den Regelmodus statt beliebige Moduswerte zu übernehmen")
    check("state.contextMode in setOf(SettingsStore.CONTEXT_MODE_SAVE, SettingsStore.CONTEXT_MODE_RULE)" in viewmodel,
          "Offline-Outbox erhält Speicher- und Regelaufträge mit ihrem Modus")

    print("PASS: Regelmodus ist server- und clientseitig isoliert; R sitzt zwischen Lupe und Diskette")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except AssertionError as exc:
        print(f"FAIL: {exc}")
        raise SystemExit(1)

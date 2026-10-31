"""Die REVIEW-Zeile eines Auftrags im Antigravity CLI, gemeinsam für Mac- und Server-Brücke."""


def antigravity_review_zeile(auftrag: dict) -> str:
    """Frank hat vor dem Start in Jarvis gewählt: verbindlich, und die Sitzung darf am Ende nie danach fragen (Profilregel 22 wäre sonst blockierend)."""
    if not auftrag.get("review"):
        return "REVIEW: Nein (von Frank vor dem Start in Jarvis entschieden, verbindlich: kein Review, und frag am Ende nicht nach einem Review)\n"
    umfang = auftrag.get("review_umfang")
    if umfang == "sehr_ausfuehrlich":
        return ("REVIEW: Ja, sehr ausführlich (von Frank vor dem Start in Jarvis gewählt, verbindlich: nach der Programmierung automatisch die Änderungen dieser Sitzung "
                "sehr gründlich auf Fehler prüfen, mit bis zu 100 Befunden, die Befunde beheben und den Pflicht-Abschluss damit abschließen; frag nicht nach einem Review)\n")
    # Ohne Umfang (ältere App oder älterer Relay) stammt „Standard“ nicht von Frank: nicht als seine Wahl ausgeben, und in der Meldung nennen.
    herkunft = "von Frank vor dem Start in Jarvis gewählt" if umfang == "standard" else "Umfang nicht übermittelt, nenne das in der Fertig-Meldung"
    return (f"REVIEW: Ja, Standard ({herkunft}, verbindlich: nach der Programmierung automatisch den gesamten Diff "
            "gründlich auf Fehler prüfen, die Befunde beheben und den Pflicht-Abschluss damit abschließen; frag nicht nach einem Review)\n")

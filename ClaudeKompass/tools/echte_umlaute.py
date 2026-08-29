# -*- coding: utf-8 -*-
"""Wandelt Ersatzschreibungen in echte Umlaute — kontrolliert, nicht blind.

Warum nicht einfach ersetzen: „neue" darf nicht zu „nü" werden und „dass" nicht zu „daß".
Deshalb zwei getrennte Wege:

  ae/oe/ue -> ä/ö/ü  ueber eine AUSNAHMELISTE (die allermeisten Vorkommen sind echte
              Ersatzschreibungen; die wenigen legitimen stehen unten)
  ss -> ß            ueber eine POSITIVLISTE (hier ist es umgekehrt: „ss" ist meistens
              richtig, nur eine ueberschaubare Zahl Woerter braucht das Eszett)

Jede Ersetzung wird ausgegeben, damit sie nachgeprueft werden kann.
"""
import io
import re
import sys

# --- Woerter, in denen ae/oe/ue KEIN Ersatz fuer einen Umlaut ist ------------------------
# Meist, weil zwei Silben aufeinandertreffen (neu-e, Steu-erung) oder es englisch ist.
AUSNAHMEN = {
    # Deutsche Woerter, in denen u und e zu verschiedenen Silben gehoeren, obwohl ein
    # Konsonant davorsteht: ak-tu-ell, zu-erst, indi-vi-du-ell. Die Vokalregel erkennt das
    # nicht, weil sie nur den Buchstaben davor ansieht.
    "aktuell", "aktuelle", "aktuellen", "aktueller", "aktuelles", "aktuellem",
    "Aktuell", "Aktuelle", "Aktuellen", "Aktueller", "Aktuelles",
    "zuerst", "Zuerst",
    "individuell", "individuelle", "individuellen", "individueller", "individuelles",
    "manuell", "manuelle", "manuellen", "manueller", "manuelles",
    "virtuell", "virtuelle", "virtuellen", "virtueller", "virtuelles",
    "eventuell", "eventuelle", "eventuellen", "eventueller", "eventuelles",
    "punktuell", "graduell", "sexuell", "rituell", "spirituell", "intellektuell",
    "Duell", "Duelle", "Statue", "Statuen", "Revue", "Silhouette",

    # Englische Woerter, bei denen die Vokalregel ebenfalls nicht greift — „value" wuerde
    # sonst zu „valü".
    "value", "values", "Value", "Values", "argue", "rescue", "continue", "issue",
    "issues", "Issue", "Issues", "venue", "revenue", "avenue", "cue", "clue", "glue",
    "true", "blue", "due", "sue", "Tue", "unique", "league",
}

# --- Woerter, die ein Eszett brauchen ----------------------------------------------------
# Nach langem Vokal oder Doppellaut. Alles andere behaelt sein Doppel-s.
ESZETT = {
    "heisst": "heißt", "heissen": "heißen", "Heisst": "Heißt",
    "schliesst": "schließt", "schliessen": "schließen", "schliesse": "schließe",
    "abschliessen": "abschließen", "abschliessend": "abschließend",
    "abschliessende": "abschließende", "abschliessenden": "abschließenden",
    "ausschliesslich": "ausschließlich", "schliesslich": "schließlich",
    "schliesst": "schließt", "einschliesslich": "einschließlich",
    "verschliesst": "verschließt", "aufschliesst": "aufschließt",
    "gross": "groß", "grosse": "große", "grossen": "großen", "grosser": "großer",
    "grosses": "großes", "grossem": "großem", "Gross": "Groß", "Grosse": "Große",
    "Grossen": "Großen", "groesser": "größer", "groessere": "größere",
    "groesseren": "größeren", "groesserer": "größerer", "groesseres": "größeres",
    "groesste": "größte", "groessten": "größten", "groesster": "größter",
    "Groesse": "Größe", "groesse": "größe", "Groessen": "Größen",
    "Grossteil": "Großteil", "grossartig": "großartig", "grosszuegig": "großzügig",
    "grosszuegige": "großzügige", "grosszuegiger": "großzügiger",
    "ausserhalb": "außerhalb", "ausser": "außer", "aussen": "außen",
    "Ausserdem": "Außerdem", "ausserdem": "außerdem", "aeusserst": "äußerst",
    "aeussere": "äußere", "aeusseren": "äußeren", "aeussert": "äußert",
    "weiss": "weiß", "weisst": "weißt", "Weiss": "Weiß", "weisse": "weiße",
    "weissen": "weißen",
    "Massstab": "Maßstab", "massgeblich": "maßgeblich", "Massnahme": "Maßnahme",
    "Massnahmen": "Maßnahmen", "gemaess": "gemäß", "Mass": "Maß", "Masse": "Maße",
    "Massen": "Maßen",
    "fliessend": "fließend", "fliessende": "fließende", "fliessenden": "fließenden",
    "fliesst": "fließt", "fliessen": "fließen", "Fluss": "Fluss",
    "Strasse": "Straße", "Strassen": "Straßen",
    "Fuss": "Fuß", "Fusses": "Fußes",
    "stossen": "stoßen", "stoesst": "stößt", "anstossen": "anstoßen",
    "Anstoss": "Anstoß", "Stoss": "Stoß",
    "reisst": "reißt", "beisst": "beißt", "giesst": "gießt",
    "Spass": "Spaß", "spassig": "spaßig",
    "Preis": "Preis",
    "Verstoss": "Verstoß", "verstoesst": "verstößt", "Verstoesse": "Verstöße",
    "regelmaessig": "regelmäßig", "regelmaessige": "regelmäßige",
    "regelmaessigen": "regelmäßigen", "unregelmaessig": "unregelmäßig",
    "zuverlaessig": "zuverlässig", "zuverlaessige": "zuverlässige",
    "zuverlaessiger": "zuverlässiger",
    "anschliessen": "anschließen", "anschliessend": "anschließend",
    "anschliessende": "anschließende", "anschliessenden": "anschließenden",
    "angestossen": "angestoßen", "gestossen": "gestoßen", "stiess": "stieß",
    "ausschliesst": "ausschließt", "ausschliessen": "ausschließen",
    "draussen": "draußen", "Draussen": "Draußen",
    "dreissig": "dreißig", "Dreissig": "Dreißig", "vierzig": "vierzig",
    "erfahrungsgemaess": "erfahrungsgemäß", "sinngemaess": "sinngemäß",
    "standardmaessig": "standardmäßig", "standardmaessige": "standardmäßige",
    "planmaessig": "planmäßig", "ordnungsgemaess": "ordnungsgemäß",
    "liess": "ließ", "Liess": "Ließ", "liessen": "ließen", "liesse": "ließe",
    "schliessende": "schließende", "schliessenden": "schließenden",
    "Schliessen": "Schließen", "Schliesst": "Schließt",
    "Ausschliessen": "Ausschließen", "Anschliessend": "Anschließend",
    "Schutzmassnahme": "Schutzmaßnahme", "Schutzmassnahmen": "Schutzmaßnahmen",
    "schutzmassnahme": "schutzmaßnahme",
    "weiterweisst": "weiterweißt", "weisst": "weißt",
    "einigermassen": "einigermaßen", "gleichermassen": "gleichermaßen",
    "massvoll": "maßvoll", "uebermaessig": "übermäßig", "gemaessigt": "gemäßigt",
}

MUSTER = re.compile(r"[A-Za-zÄÖÜäöüß]+")

# Steht vor dem „e" ein Vokal oder ein q, gehoert das Buchstabenpaar zu zwei verschiedenen
# Lauten und ist KEIN Ersatz fuer einen Umlaut:
#   Ba-u-en, ne-u-e, Ste-u-erung, Beq-u-em, Q-u-ellen, teu-er, Feu-er
# Diese eine Regel erledigt die grosse Mehrheit der Faelle; die Ausnahmeliste oben faengt
# nur noch das ab, was ihr entgeht (praktisch: englische Woerter wie „value").
VORHER_KEIN_UMLAUT = set("aeiouAEIOUqQ")


def wandle_wort(wort):
    """Liefert das Wort mit echten Umlauten — oder unveraendert, wenn nichts zu tun ist."""
    if wort in ESZETT:
        return ESZETT[wort]
    if wort in AUSNAHMEN:
        return wort

    if wort in ERZWUNGEN:
        return ERZWUNGEN[wort]

    ergebnis = []
    stelle = 0
    while stelle < len(wort):
        paar = wort[stelle:stelle + 2]
        umlaut = PAARE.get(paar)
        davor = wort[stelle - 1] if stelle > 0 else ""
        # An einer Morphemgrenze gilt die Vokalregel nicht: In „ge-ändert" endet die
        # Vorsilbe auf einem Vokal, danach steht trotzdem ein Umlaut.
        an_vorsilbe = stelle == 2 and wort[:2].lower() == "ge"
        if umlaut and (an_vorsilbe or davor not in VORHER_KEIN_UMLAUT):
            ergebnis.append(umlaut)
            stelle += 2
        else:
            ergebnis.append(wort[stelle])
            stelle += 1
    return "".join(ergebnis)


# Zusammensetzungen, deren erstes Glied auf einem Vokal endet. Dort greift die Vokalregel
# ebenfalls faelschlich, und eine allgemeine Regel dafuer gaebe es nur mit einem
# Wortschatz — fuer die paar Faelle ist die Liste ehrlicher.
ERZWUNGEN = {
    "Dateiaenderung": "Dateiänderung",
    "Dateiaenderungen": "Dateiänderungen",
    "Dateiaenderungs": "Dateiänderungs",
    "mitgeaenderte": "mitgeänderte",
    "mitgeaenderten": "mitgeänderten",
    "ausgeaendert": "ausgeändert",
    "durchgeaendert": "durchgeändert",
}

PAARE = {
    "ae": "ä", "oe": "ö", "ue": "ü",
    "Ae": "Ä", "Oe": "Ö", "Ue": "Ü",
    # Durchgehende Grossschreibung wird zur Betonung benutzt („ZUSAMMENGEFUEHRT").
    "AE": "Ä", "OE": "Ö", "UE": "Ü",
}


def wandle_text(text, protokoll):
    """Wandelt einen ganzen Text und vermerkt jede Ersetzung in [protokoll]."""
    def ersetze(treffer):
        alt = treffer.group(0)
        neu = wandle_wort(alt)
        if neu != alt:
            protokoll[alt] = neu
        return neu
    return MUSTER.sub(ersetze, text)

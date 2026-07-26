#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Check 9 — Portugiesisch-Varianten-Trennung (pt-BR ↔ pt-PT).

Praktisch alle LLMs defaulten auf pt-BR und streuen BR-Vokabular auch dann ein,
wenn explizit pt-PT angefragt wurde (und umgekehrt). Dieses Script prueft
BIDIREKTIONAL ob die Uebersetzung sauber zur jeweiligen Variante passt.

Empirische Daten (BestJournal-App, April 2026):
  - pt-PT Uebersetzung: 108 você-Vorkommen, 97 Salvar/Excluir/Compartilhar-Leakage,
    29 Retrospectiva statt Retrospetiva (AO 1990) — ~580 systematische Fixes
  - pt-BR darf KEINE PT-PT-Vokabeln haben und umgekehrt

Usage:
    python3 check_pt_variants.py --locale pt-PT --path /path/to/values-pt-rPT/strings.xml
    python3 check_pt_variants.py --locale pt-BR --path /path/to/values-pt-rBR/strings.xml

    (Auch akzeptiert: --variant pt-PT / --variant pt-BR fuer Rueckwaerts-Kompatibilitaet.)

Exit-Codes:
    0 = OK (keine Variant-Leakage gefunden)
    1 = Leakage gefunden (Bericht in stdout, KEIN Auto-Fix — manuell korrigieren)
    2 = Fehler
"""
import argparse
import os
import re
import sys


# Marker-Paare: (pt-PT-Wort, pt-BR-Wort)
MARKERS = [
    ("utilizador", "usuário"),
    ("aplicação", "aplicativo"),
    ("guardar", "salvar"),
    ("definições", "configurações"),
    ("palavra-passe", "senha"),
    ("transferir", "baixar"),
    ("telemóvel", "celular"),
    ("ficheiro", "arquivo"),
    ("eliminar", "excluir"),
    ("ecrã", "tela"),
    ("iniciar sessão", "fazer login"),
    ("partilhar", "compartilhar"),
    ("subscrição", "assinatura"),
    ("câmara", "câmera"),
    ("registo", "registro"),
    ("tu", "você"),
    ("nós", "a gente"),
    ("stress", "estresse"),
    ("retrospetiva", "retrospectiva"),
    ("aceder", "acessar"),
    ("gerir", "gerenciar"),
]

# Gerundium-Indikatoren: In pt-PT VERBOTEN, "-ando/-endo/-indo" nach "estar/está/estou"
GERUND_PATTERNS = [r"\b(est[aoáãou]\w*)\s+\w+(ando|endo|indo)\b"]


def extract_visible(text):
    """Extrahiere nur user-sichtbare String-Inhalte (kein Resource-Name, keine Kommentare)."""
    parts = []
    for m in re.finditer(
        r"<string[^>]*>([^<]*(?:<(?!/string>)[^<]*)*)</string>", text, re.DOTALL
    ):
        parts.append(m.group(1))
    for m in re.finditer(
        r"<item[^>]*>([^<]*(?:<(?!/item>)[^<]*)*)</item>", text, re.DOTALL
    ):
        parts.append(m.group(1))
    return "\n".join(parts)


def main():
    parser = argparse.ArgumentParser(
        description="Portugiesisch-Varianten-Trennung (pt-PT ↔ pt-BR)"
    )
    # --locale ist neuer Standard, --variant bleibt als Alias fuer Rueckwaerts-Kompatibilitaet
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument(
        "--locale",
        choices=["pt-PT", "pt-BR"],
        help="Zielvariante der Uebersetzung (neuer Standard, gleiches Format wie andere Scripts)",
    )
    group.add_argument(
        "--variant",
        choices=["pt-PT", "pt-BR"],
        help="Zielvariante (alter Parameter-Name, weiterhin akzeptiert)",
    )
    parser.add_argument(
        "--path", required=True, help="Absoluter Pfad zur strings.xml"
    )
    args = parser.parse_args()
    # Aufloesung — egal welcher Parameter, in args.variant zusammenfuehren
    args.variant = args.locale or args.variant

    if not os.path.isfile(args.path):
        print(f"FEHLER: Datei nicht gefunden: {args.path}", file=sys.stderr)
        sys.exit(2)

    try:
        with open(args.path, "r", encoding="utf-8") as f:
            content = f.read()
    except (OSError, UnicodeDecodeError) as e:
        print(f"FEHLER: Konnte {args.path} nicht lesen: {e}", file=sys.stderr)
        sys.exit(2)

    visible = extract_visible(content)
    label_forbidden = "pt-BR" if args.variant == "pt-PT" else "pt-PT"

    print(f"=== Check 9 — Varianten-Trennung fuer {args.variant} ===")
    print(f"Suche nach {label_forbidden}-Woertern (sollten NICHT vorkommen):\n")

    total_leakage = 0
    findings = []

    for pt, br in MARKERS:
        forbidden = br if args.variant == "pt-PT" else pt
        correct = pt if args.variant == "pt-PT" else br
        pattern = r"\b" + re.escape(forbidden) + r"\b"
        matches = re.findall(pattern, visible, re.IGNORECASE)
        if matches:
            total_leakage += len(matches)
            findings.append((forbidden, correct, len(matches)))

    # Gerundium-Check (nur fuer pt-PT — BR-Form ist verboten)
    gerund_matches = []
    if args.variant == "pt-PT":
        for pat in GERUND_PATTERNS:
            for m in re.finditer(pat, visible):
                gerund_matches.append(m.group(0))

    if not findings and not gerund_matches:
        print(f"OK: 0 {label_forbidden}-Kontaminationen — saubere {args.variant}-Uebersetzung")
        sys.exit(0)

    for forbidden, correct, count in findings:
        print(f"  WARN {forbidden!r} (sollte: {correct!r}): {count}x")
    if gerund_matches:
        total_leakage += len(gerund_matches)
        print(
            f"  WARN BR-Gerundium (sollte 'a + Infinitiv'): {len(gerund_matches)}x"
        )
        for g in gerund_matches[:5]:
            print(f"    Beispiel: {g}")

    print(f"\nFEHLER: {total_leakage} Varianten-Verletzungen gefunden")
    print(
        "MUESSEN korrigiert werden BEVOR der Commit erfolgt. Kein Auto-Fix — "
        "Kontext-sensitive Korrektur durch LLM-Verbesserungs-Pass."
    )
    sys.exit(1)


if __name__ == "__main__":
    main()

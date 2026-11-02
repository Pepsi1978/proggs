"""Effort-Stufen je Modell für die Server-Konsole: dieselbe Tabelle wie Jarvis/.../faehigkeit/OpenLauncherEfforts.kt (Vorlage: OpenCodeVariantCatalog.GetLauncherLevels).

Ändert sich die Tabelle dort, muss sie hier mit. Einzige Abweichung vom PC: Claude Opus 5.5 kennt Windows nur aus seinem gemerkten Stand.
"""
from __future__ import annotations

BREIT = ["low", "medium", "high"]
GPT52 = ["none", "low", "medium", "high", "xhigh"]
AKTUELL = ["low", "medium", "high", "xhigh", "max", "ultra"]
LUNA = ["low", "medium", "high", "xhigh", "max"]
SOL_FAMILIE = {"gpt-6-astra", "gpt-6.1-sol", "gpt-6.1-terra", "gpt-5.6-terra"}
KIMI = {"k3.1-pro", "k3.1-pro-highspeed", "k3", "k3-256k", "kimi-for-coding"}
ANTHROPIC_FUENF = {"claude-opus-5-5", "claude-opus-5", "claude-fable-5-1", "claude-fable-5", "claude-opus-4-8", "claude-opus-4-7", "claude-sonnet-5-5", "claude-sonnet-5",
                   "claude-haiku-5-5", "claude-haiku-4-5"}
NVIDIA_DENKEN = {"z-ai/glm-5.2", "minimaxai/minimax-m3", "google/gemma-4-31b-it", "nvidia/nemotron-3-nano-omni-30b-a3b-reasoning", "nvidia/nemotron-3-nano-30b-a3b",
                 "nvidia/llama-3.1-nemotron-ultra-253b-v1", "nvidia/llama-3.3-nemotron-super-49b-v1.5", "nvidia/llama-3.3-nemotron-super-49b-v1",
                 "nvidia/nvidia-nemotron-nano-9b-v2", "nvidia/llama-3.1-nemotron-nano-8b-v1"}


def immer_fest(anbieter: str, slug: str) -> bool:
    """Die aktuellen OpenAI-Modelle zeigt OpenLauncher immer aus der festen Tabelle, nie aus einem gemerkten Stand."""
    return anbieter.lower() == "openai" and (slug in SOL_FAMILIE or slug == "gpt-6-luna")


def _anthropic(roh: str) -> list:
    s = roh.removesuffix("[1m]")
    if s in ANTHROPIC_FUENF:
        return ["low", "medium", "high", "xhigh", "max"]
    if s in ("claude-opus-4-6", "claude-sonnet-4-6"):
        return ["low", "medium", "high", "max"]
    return BREIT if s == "claude-opus-4-5" else []


def _gpt(s: str, codex_sonst: list) -> list | None:
    """Die gemeinsamen GPT-Regeln von direktem OpenAI und OpenRouter; None: Der Aufrufer entscheidet über den Rest."""
    if s in SOL_FAMILIE:
        return AKTUELL
    if s == "gpt-6-luna":
        return LUNA
    if "-chat" in s:
        return ["medium"]
    if "pro" in s:
        return ["medium", "high", "xhigh"] if "gpt-5." in s else ["high"]
    if "codex" in s:
        return GPT52 if any(v in s for v in ("gpt-5.3", "gpt-5.4", "gpt-5.5")) else codex_sonst
    if s.startswith("gpt-6"):
        return GPT52
    if s.startswith("gpt-5.1"):
        return ["none", "low", "medium", "high"]
    return GPT52 if s.startswith("gpt-5.") else None


def _openai(s: str) -> list:
    if s not in SOL_FAMILIE and s != "gpt-6-luna" and ("-mini" in s or "-nano" in s):
        return []
    stufe = _gpt(s, BREIT)
    if stufe is not None:
        return stufe
    return ["minimal", "low", "medium", "high"] if s.startswith("gpt-5") else []


def _zen(s: str) -> list:
    if s == "deepseek-v4-flash-free":
        return ["low", "medium", "high", "max"]
    if s in ("mimo-v2.5-free", "nemotron-3-ultra-free"):
        return BREIT
    return ["none", "high"] if s == "north-mini-code-free" else []


def _nvidia(s: str) -> list:
    if s == "stepfun-ai/step-3.7-flash":
        return ["minimal", "low", "medium", "high", "xhigh", "max"]
    if s in ("openai/gpt-oss-120b", "openai/gpt-oss-20b"):
        return BREIT
    if s == "mistralai/mistral-medium-3.5-128b":
        return ["none", "high"]
    return ["none", "thinking"] if s in NVIDIA_DENKEN else []


def _glm52(s: str) -> bool:
    return "glm-5.2" in s or "glm-5-2" in s or "glm-5p2" in s


def _openai_kompatibel(kennung: str) -> list:
    stufe = _gpt(kennung.removeprefix("openai/"), ["low", "medium", "high", "xhigh"])
    return stufe if stufe is not None else ["none", "minimal", "low", "medium", "high", "xhigh"]


def _openrouter(s: str) -> list:
    bekannt = _glm52(s) or s.startswith(("openai/gpt-6", "openai/gpt-oss", "openai/gpt-5", "deepseek/deepseek-v4", "xiaomi/mimo-v2.5")) or any(
        t in s for t in ("nemotron-3-ultra", "nemotron-3-nano-omni", "nemotron-3-super", "laguna-m", "lfm-2.5-1.2b-thinking", "gemma-4-31b"))
    if not bekannt:
        return []
    if _glm52(s):
        return ["high", "xhigh"]
    return _openai_kompatibel(s) if s.startswith("openai/") or "gpt" in s else BREIT


def stufen(anbieter: str, slug_roh: str) -> list:
    slug = slug_roh.strip().lower().removeprefix("openrouter/")
    a = anbieter.lower()
    if a == "openai":
        return _openai(slug)
    if a == "opencode":
        return _zen(slug)
    if a == "nvidia":
        return _nvidia(slug)
    if a == "anthropic":
        return _anthropic(slug)
    if a == "kimi-code-plan-global":
        return ["low", "high", "max"] if slug in KIMI else []
    if a == "google":
        return ["medium", "high"] if slug.startswith("gemini-4-argon") else BREIT
    return _openrouter(slug) if a == "openrouter" else []


def modell_string(anbieter: str, slug: str) -> str:
    """Wie ModelEntry.ModelString: Trägt der Slug den Anbieter schon vorn, kommt er nicht doppelt (außer bei NVIDIA, dort gehört er auch dann davor)."""
    if not anbieter:
        return slug
    return slug if anbieter.lower() != "nvidia" and slug.lower().startswith(f"{anbieter.lower()}/") else f"{anbieter}/{slug}"


def fuer_cli(anbieter: str, slug: str, cli: str, gemerkt: dict) -> list:
    """Was die Konsole für dieses Modell in diesem CLI anbietet: der am PC gemerkte Stand (effort-stufen.json), sonst die Tabelle; aktuelle OpenAI-Modelle immer die Tabelle."""
    if immer_fest(anbieter, slug):
        return stufen(anbieter, slug)
    schluessel = modell_string(anbieter, slug)
    wert = gemerkt.get(f"{schluessel} [{'claude-code' if cli == 'claude' else cli}]")
    # Der Stand ohne CLI-Zusatz gehört zum Zugang über OpenCode; für Codex gilt er nicht (MainViewModel am PC).
    if wert is None and cli != "codex":
        wert = gemerkt.get(schluessel)
    return wert if isinstance(wert, list) else stufen(anbieter, slug)

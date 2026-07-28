// Wiederholungslogik fuer KI-Laeufe. Vorher: drei Versuche mit `attempt * 2000` Millisekunden.
// Bei einer echten Kapazitaetsdrosselung des Anbieters (HTTP 503) war das Budget nach sechs
// Sekunden aufgebraucht — und weil alle gleichzeitig laufenden Schritte exakt gleich lang warteten,
// schlugen sie danach im Gleichschritt erneut auf und rissen den ganzen Lauf mit.

// Sechs Versuche decken mit dem Backoff unten rund zwei Minuten Anbieterstoerung ab. Das ist die
// Groessenordnung, in der 503-Wellen wieder abklingen.
export const maxModelAttempts = 6;

const baseDelayMs = 2_000;
const maxDelayMs = 45_000;

// Exponentiell UND mit Jitter: die Verdopplung ueberbrueckt laengere Stoerungen, der Zufallsanteil
// bricht den Gleichschritt paralleler Schritte auf (bugs/apis/openai-api.md F17/F18).
export function retryDelayMs(attempt: number, retryAfterMs?: number, random: () => number = Math.random): number {
  const exponential = Math.min(maxDelayMs, baseDelayMs * 2 ** Math.max(0, attempt - 1));
  const jittered = Math.round(exponential / 2 + random() * (exponential / 2));
  // Nennt der Anbieter selbst eine Wartezeit, ist sie die Untergrenze — kuerzer zu warten heisst,
  // die naechste Absage schon eingekauft zu haben.
  if (typeof retryAfterMs === "number" && Number.isFinite(retryAfterMs) && retryAfterMs > 0) return Math.min(maxDelayMs, Math.max(jittered, Math.round(retryAfterMs)));
  return jittered;
}

// Nur Ueberlast-Signale duerfen die Drosselung ausloesen. Ein 400er ist ein Fehler im Aufruf und
// wird durch Warten nicht besser.
export function isOverloadError(error: unknown): boolean {
  if (!error || typeof error !== "object") return false;
  const details = error as { statusCode?: unknown; upstreamStatus?: unknown; message?: unknown };
  if (details.upstreamStatus === 429 || details.upstreamStatus === 503 || details.upstreamStatus === 529) return true;
  return typeof details.message === "string" && /\b(?:429|503|529)\b/.test(details.message);
}

export type ThrottleState = { activeLimit: number; openUntil: number };

// Adaptive Drosselung: Solange der Anbieter ueberlastet meldet, laufen weniger Schritte
// gleichzeitig. Erholt er sich, geht die Nebenlaeufigkeit schrittweise wieder hoch. Das ist die
// zweite Verteidigungslinie — die Wiederholung repariert den Einzelfall, die Drosselung nimmt die
// Ursache weg (acht gleichzeitige Streams auf ein Konto).
export class UpstreamThrottle {
  #limit: number;
  #openUntil = 0;
  #active = 0;
  #waiting: Array<() => void> = [];

  constructor(private readonly fullLimit: number, private readonly overloadLimit = 2, private readonly recoveryMs = 60_000, private readonly now: () => number = Date.now) {
    this.#limit = fullLimit;
  }

  get state(): ThrottleState {
    this.#recover();
    return { activeLimit: this.#limit, openUntil: this.#openUntil };
  }

  #recover(): void {
    if (this.#openUntil && this.now() >= this.#openUntil) {
      this.#openUntil = 0;
      this.#limit = this.fullLimit;
    }
  }

  // Nach einer Ueberlastmeldung faellt die Grenze sofort; sie steigt erst nach einer ruhigen
  // Erholungsphase wieder — ein einzelner 503 soll den Lauf nicht dauerhaft ausbremsen.
  penalize(): void {
    this.#limit = Math.min(this.#limit, this.overloadLimit);
    this.#openUntil = this.now() + this.recoveryMs;
  }

  async acquire(): Promise<() => void> {
    this.#recover();
    // Erneut pruefen statt einmalig warten: waehrend des Wartens kann die Grenze durch eine
    // Ueberlastmeldung weiter gefallen sein.
    while (this.#active >= this.#limit) {
      await new Promise<void>((resolve) => this.#waiting.push(resolve));
      this.#recover();
    }
    this.#active += 1;
    let released = false;
    return () => {
      if (released) return;
      released = true;
      this.#active -= 1;
      this.#recover();
      // Nur so viele wecken, wie unter die aktuelle Grenze passen: nach einer Drosselung darf die
      // Warteschlange nicht auf einen Schlag wieder alles losschicken.
      while (this.#waiting.length && this.#active < this.#limit) this.#waiting.shift()!();
    };
  }
}

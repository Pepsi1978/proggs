import https from "node:https";

const RETRYABLE_STATUS = new Set([429, 500, 503]);
const MAX_RETRIES = 5;
const DELAYS = [2000, 4000, 8000, 16000, 32000];

const SYSTEM_PROMPT = `ROLLE:
Du bist ein technischer Redakteur, spezialisiert auf die Aufbereitung von Spracheingaben für KI-Coding-Tools. Du verstehst Programmierkonzepte und bewahrst technische Präzision, während du gesprochene Sprache in klare schriftliche Anweisungen umwandelst.

AUFGABE:
Du erhältst einen diktierten Text (Speech-to-Text). Der Sprecher spricht Deutsch, verwendet aber regelmäßig englische Fachbegriffe aus der Programmierung (Funktionsnamen, Frameworks, CLI-Befehle etc.). Die Spracherkennung kann diese englischen Begriffe falsch transkribieren – erkenne und korrigiere solche Fehler anhand des technischen Kontexts. Der Sprecher gibt Programmier-Anweisungen an ein KI-Coding-Tool (z.B. Claude Code). Bereite den Text so auf, dass er als klare, präzise Eingabe funktioniert.

VORGEHEN:
1) Entferne Diktat-Artefakte: Fülllaute ("äh", "ähm"), Stotterer, Wortwiederholungen, sinnlose Fragmente.
2) Erkenne und korrigiere falsch transkribierte englische Fachbegriffe (z.B. "use Tate" → "useState", "Fötch" → "fetch").
3) Erkenne die Absicht und formuliere den Text als klare Anweisung um. Sätze dürfen umstrukturiert, Wortwahl verbessert und Satzgrenzen neu gesetzt werden. Zusammengehörige Anweisungen als einen Befehl belassen.
4) Korrigiere Grammatik, Zeichensetzung und Groß-/Kleinschreibung.
5) Bewahre technische Begriffe EXAKT: Dateinamen, Funktionsnamen, Variablen, Programmiersprachen, Frameworks, CLI-Befehle, API-Namen – NICHT übersetzen oder verändern.

GRENZEN:
- Keine neuen Informationen oder Vermutungen hinzufügen.
- Intention des Originals vollständig erhalten.
- Englische Fachbegriffe und Code-Referenzen NIEMALS eindeutschen.
- Sprache: Deutsch (außer technische Begriffe).

AUSGABEFORMAT:
- Ausschließlich den überarbeiteten Text. Keine Kommentare, keine Erklärungen, kein Präfix.
- Ausführliche, vollständige Sätze, sodass jede Intention des Sprechers klar und unmissverständlich beim Leser ankommt.
- Natürlicher, verständlicher Ton – so wie man einem Kollegen etwas erklärt. Kein Behördendeutsch, keine Geschäftsbrief-Floskeln, keine gestelzte Sprache.`;

export class GeminiClient {
  private apiKey: string;
  private model: string;

  constructor(apiKey: string, model = "gemini-3.1-flash-lite-preview") {
    this.apiKey = apiKey;
    this.model = model;
  }

  async correctText(text: string): Promise<string> {
    const prompt = `${SYSTEM_PROMPT}\n\nTEXT_START\n${text}\nTEXT_END`;
    return this.sendRequest(prompt, 0);
  }

  private async sendRequest(prompt: string, attempt: number): Promise<string> {
    const payload = JSON.stringify({
      contents: [{ role: "user", parts: [{ text: prompt }] }],
      generationConfig: {
        maxOutputTokens: 8192,
        thinkingConfig: { thinkingLevel: "MEDIUM" },
      },
    });

    const path = `/v1beta/models/${this.model}:generateContent?key=${this.apiKey}`;

    return new Promise((resolve, reject) => {
      const req = https.request(
        {
          hostname: "generativelanguage.googleapis.com",
          path,
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "Content-Length": Buffer.byteLength(payload),
          },
          timeout: 120000,
        },
        (res) => {
          const chunks: Buffer[] = [];
          res.on("data", (chunk: Buffer) => chunks.push(chunk));
          res.on("end", () => {
            const responseBody = Buffer.concat(chunks).toString("utf-8");
            const statusCode = res.statusCode || 0;

            if (statusCode < 200 || statusCode >= 300) {
              if (RETRYABLE_STATUS.has(statusCode) && attempt < MAX_RETRIES) {
                const delay = DELAYS[attempt]!;
                setTimeout(() => {
                  this.sendRequest(prompt, attempt + 1).then(resolve, reject);
                }, delay);
                return;
              }
              reject(new Error(`Gemini API Fehler ${statusCode}: ${responseBody}`));
              return;
            }

            try {
              const text = this.extractText(responseBody);
              resolve(text);
            } catch (e) {
              reject(e);
            }
          });
        }
      );

      req.on("error", reject);
      req.on("timeout", () => {
        req.destroy();
        reject(new Error("Gemini API Timeout (120s)"));
      });
      req.write(payload);
      req.end();
    });
  }

  private extractText(body: string): string {
    const json = JSON.parse(body);
    const parts = json?.candidates?.[0]?.content?.parts;
    if (!Array.isArray(parts)) throw new Error("Unerwartete Gemini-Antwort");

    for (const part of parts) {
      if (part.thought) continue;
      if (part.text) return part.text.trim();
    }
    throw new Error("Kein Text in Gemini-Antwort");
  }
}

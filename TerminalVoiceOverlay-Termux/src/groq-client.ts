import { readFileSync } from "node:fs";
import https from "node:https";
import { basename } from "node:path";

const RETRYABLE_STATUS = new Set([429, 500, 503]);
const MAX_RETRIES = 3;
const DELAYS = [2000, 4000, 8000];

export class GroqWhisperClient {
  private apiKey: string;
  private model: string;
  private lang: string;

  constructor(apiKey: string, model = "whisper-large-v3", lang = "de") {
    this.apiKey = apiKey;
    this.model = model;
    this.lang = lang;
  }

  async transcribe(filePath: string): Promise<string> {
    return this.sendRequest(filePath, 0);
  }

  private async sendRequest(filePath: string, attempt: number): Promise<string> {
    const audioData = readFileSync(filePath);
    const boundary = `----FormBoundary${Date.now()}`;

    const fields = [
      ["model", this.model],
      ["language", this.lang],
      ["response_format", "text"],
      ["temperature", "0"],
    ];

    let body = Buffer.alloc(0);
    for (const [key, value] of fields) {
      body = Buffer.concat([
        body,
        Buffer.from(`--${boundary}\r\nContent-Disposition: form-data; name="${key}"\r\n\r\n${value}\r\n`),
      ]);
    }

    body = Buffer.concat([
      body,
      Buffer.from(
        `--${boundary}\r\nContent-Disposition: form-data; name="file"; filename="${basename(filePath)}"\r\nContent-Type: audio/wav\r\n\r\n`
      ),
      audioData,
      Buffer.from(`\r\n--${boundary}--\r\n`),
    ]);

    return new Promise((resolve, reject) => {
      const req = https.request(
        {
          hostname: "api.groq.com",
          path: "/openai/v1/audio/transcriptions",
          method: "POST",
          headers: {
            Authorization: `Bearer ${this.apiKey}`,
            "Content-Type": `multipart/form-data; boundary=${boundary}`,
            "Content-Length": body.length,
          },
          timeout: 180000,
        },
        (res) => {
          const chunks: Buffer[] = [];
          res.on("data", (chunk: Buffer) => chunks.push(chunk));
          res.on("end", () => {
            const responseBody = Buffer.concat(chunks).toString("utf-8").trim();
            const statusCode = res.statusCode || 0;

            if (statusCode >= 200 && statusCode < 300 && responseBody) {
              resolve(responseBody);
              return;
            }

            if (RETRYABLE_STATUS.has(statusCode) && attempt < MAX_RETRIES) {
              const delay = DELAYS[attempt]!;
              setTimeout(() => {
                this.sendRequest(filePath, attempt + 1).then(resolve, reject);
              }, delay);
              return;
            }

            reject(new Error(`Groq API Fehler ${statusCode}: ${responseBody}`));
          });
        }
      );

      req.on("error", reject);
      req.on("timeout", () => {
        req.destroy();
        reject(new Error("Groq API Timeout (180s)"));
      });
      req.write(body);
      req.end();
    });
  }
}

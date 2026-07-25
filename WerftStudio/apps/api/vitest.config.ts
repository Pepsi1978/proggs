import { defineConfig } from "vitest/config";

// Ohne diesen Ausschluss laeuft Vitest zusaetzlich die kompilierten Tests aus dist/ mit. Die sind
// nach jedem Quellcode-Wechsel veraltet und melden Fehler, die es im Quellstand gar nicht gibt.
export default defineConfig({
  test: { include: ["src/**/*.test.ts"], exclude: ["**/node_modules/**", "**/dist/**"] }
});

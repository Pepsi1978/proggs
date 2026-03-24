#!/usr/bin/env node

import fs from "node:fs/promises";
import path from "node:path";

const REPO_ROOT = process.env.GEMINI_REPO_ROOT || "C:\\Users\\barwa\\GeminiCLI";

const WHITEBOARDS = {
  GEMINI: path.join(REPO_ROOT, "Gemini-Setup", "agent-memory", "shared", "MEMORY.md"),
  Gemini: path.join(REPO_ROOT, "Gemini-Setup", "agent-memory", "shared", "MEMORY.md"),
  CODEX: path.join(REPO_ROOT, "codex-setup", "agent-memory", "shared", "MEMORY.md"),
};

async function fileExists(filePath) {
  try {
    await fs.access(filePath);
    return true;
  } catch {
    return false;
  }
}

async function readWhiteboard(filePath) {
  if (!(await fileExists(filePath))) return "";
  return await fs.readFile(filePath, "utf8");
}

function extractFixesAndIntelligence(content) {
  const items = [];
  
  // 1. Archivierte Fixes (HTML Kommentare)
  const archiveRegex = /<!-- ARCHIV \((.*?)\): (.*?) -->/g;
  let match;
  while ((match = archiveRegex.exec(content)) !== null) {
    items.push({ date: match[1], description: match[2], type: "Fix (Archiv)" });
  }

  // 2. Umgesetzte Forschung & Intelligence (Status: UMGESETZT oder GEFIXT)
  const intelligenceRegex = /- \*\*\[(.*?)\] (.*?)\*\* — Status: (UMGESETZT|GEFIXT) .*? \| Quelle: (.*?) \| Empfehlung: (.*?)\n\s+(.*)/g;
  while ((match = intelligenceRegex.exec(content)) !== null) {
    items.push({
      date: match[1],
      title: match[2],
      status: match[3],
      source_url: match[4],
      recommendation: match[5],
      details: match[6],
      type: "Intelligence (Umgesetzt)"
    });
  }

  // 3. Ausführliche GEFIXT-Einträge in der Offene-Fehler-Sektion
  const detailedFixRegex = /### (.*?) — Source: (.*?) — (.*?)\n\*\*Context:\*\* (.*?)\n\*\*Source:\*\* (.*?)\n\*\*Symptom:\*\* (.*?)\n\*\*Ursache:\*\* (.*?)\n\*\*Betroffene Dateien:\*\* (.*?)\n\*\*Reproduktion:\*\* (.*?)\n\*\*Fix-Details & Begruendung:\*\* (.*?)\n\*\*Status:\*\* GEFIXT/gs;
  while ((match = detailedFixRegex.exec(content)) !== null) {
    items.push({
      date: match[1],
      description: match[3],
      context: match[4],
      symptom: match[6],
      cause: match[7],
      fix: match[9],
      type: "Fix (Detailliert)"
    });
  }

  return items;
}

async function main() {
  console.log("Gemini CLI Delta Bruecke — Lerne von Gemini und Codex\n");

  const geminiContent = await readWhiteboard(WHITEBOARDS.GEMINI);
  const GeminiContent = await readWhiteboard(WHITEBOARDS.Gemini);
  const codexContent = await readWhiteboard(WHITEBOARDS.CODEX);

  const geminiItems = extractFixesAndIntelligence(geminiContent);
  const GeminiItems = extractFixesAndIntelligence(GeminiContent);
  const codexItems = extractFixesAndIntelligence(codexContent);

  console.log(`Gemini Bekannt: ${geminiItems.length}`);
  console.log(`Gemini Eintraege: ${GeminiItems.length}`);
  console.log(`Codex Eintraege: ${codexItems.length}\n`);

  // Identifiziere neue Eintraege
  const newGeminiItems = GeminiItems.filter(item => !geminiItems.some(gi => 
    (gi.description === item.description && gi.description !== undefined) || 
    (gi.title === item.title && gi.title !== undefined)
  ));
  
  const newCodexItems = codexItems.filter(item => !geminiItems.some(gi => 
    (gi.description === item.description && gi.description !== undefined) || 
    (gi.title === item.title && gi.title !== undefined)
  ));

  if (newGeminiItems.length > 0) {
    console.log("--- NEUE ERKENNTNISSE VON Gemini ---");
    newGeminiItems.forEach(item => {
      console.log(`[${item.date}] ${item.type}: ${item.description || item.title}`);
      if (item.fix) console.log(`  Fix-Logik: ${item.fix.trim().substring(0, 200)}...`);
      if (item.details) console.log(`  Details: ${item.details.trim().substring(0, 200)}...`);
    });
    console.log("");
  }

  if (newCodexItems.length > 0) {
    console.log("--- NEUE ERKENNTNISSE VON CODEX ---");
    newCodexItems.forEach(item => {
      console.log(`[${item.date}] ${item.type}: ${item.description || item.title}`);
      if (item.fix) console.log(`  Fix-Logik: ${item.fix.trim().substring(0, 200)}...`);
      if (item.details) console.log(`  Details: ${item.details.trim().substring(0, 200)}...`);
    });
    console.log("");
  }

  if (newGeminiItems.length === 0 && newCodexItems.length === 0) {
    console.log("Gemini ist auf dem neuesten Stand. Keine neuen Fixes oder Intelligence-Hebel gefunden.");
  }
}

main().catch(console.error);


import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import {
	CallToolRequestSchema,
	ListToolsRequestSchema,
} from "@modelcontextprotocol/sdk/types.js";

// ── Config ──────────────────────────────────────────────────────────────────
const NIM_API_URL = "https://integrate.api.nvidia.com/v1/chat/completions";
const NIM_MODEL = "nvidia/nemotron-3-super-120b-a12b";
const NIM_API_KEY = process.env.NIM_API_KEY ?? "";

// QuizVerse categories for reference in prompts
const CATEGORIES: Record<number, string> = {
	1: "Weltgeographie",
	2: "Wissenschaft & Natur",
	3: "Geschichte",
	4: "Film & Fernsehen",
	5: "Musik",
	6: "Sport",
	7: "Technologie",
	8: "Essen & Trinken",
	9: "Tierwelt",
	10: "Sprache & Literatur",
	11: "Alle Kategorien",
	12: "Logik & Denksport",
	13: "Hertha BSC",
	14: "Borussia Dortmund",
	15: "Fußball",
	16: "Gesundheit & Medizin",
	17: "Politik & Gesellschaft",
};

// ── NIM API call ────────────────────────────────────────────────────────────
async function callNemotron(
	prompt: string,
	maxTokens = 4096,
	temperature = 0.7,
): Promise<string> {
	if (!NIM_API_KEY) {
		throw new Error(
			"NIM_API_KEY not set. Get your key at build.nvidia.com and set it as environment variable.",
		);
	}

	// Nemotron 3 Super is a reasoning model — it uses tokens for thinking
	// before producing the answer. We need extra headroom for the reasoning phase.
	const effectiveMaxTokens = Math.max(maxTokens * 2, 8192);

	const response = await fetch(NIM_API_URL, {
		method: "POST",
		headers: {
			Authorization: `Bearer ${NIM_API_KEY}`,
			"Content-Type": "application/json",
		},
		body: JSON.stringify({
			model: NIM_MODEL,
			messages: [{ role: "user", content: prompt }],
			max_tokens: effectiveMaxTokens,
			temperature,
		}),
	});

	if (!response.ok) {
		const errorText = await response.text();
		throw new Error(`NIM API error ${response.status}: ${errorText}`);
	}

	// Nemotron 3 Super returns reasoning in reasoning_content and answer in content
	const data = (await response.json()) as {
		choices: {
			message: {
				content: string | null;
				reasoning_content?: string | null;
			};
		}[];
	};

	const msg = data.choices[0]?.message;
	// Prefer content (actual answer), fall back to reasoning_content if content is empty
	return msg?.content ?? msg?.reasoning_content ?? "";
}

// ── Quiz generation ─────────────────────────────────────────────────────────
interface QuizQuestion {
	questionText: string;
	answerA: string;
	answerB: string;
	answerC: string;
	answerD: string;
	correctAnswer: number;
	explanation: string;
	difficulty: number;
	funFact: string | null;
}

function buildQuizPrompt(
	topic: string,
	count: number,
	difficulty: string,
	language: string,
): string {
	return `Generate exactly ${count} quiz questions about "${topic}" in ${language}.

Rules:
- Each question has exactly 4 answer options (A, B, C, D)
- correctAnswer is 0 for A, 1 for B, 2 for C, 3 for D
- Distribute correct answers evenly across A/B/C/D
- difficulty is a number: ${difficulty}
- explanation should be 1-2 sentences explaining why the answer is correct
- funFact is an optional interesting related fact (can be null)
- Questions must be factually accurate
- Wrong answers must be plausible but clearly wrong
- No duplicate questions

Respond ONLY with a JSON array, no other text:
[
  {
    "questionText": "...",
    "answerA": "...",
    "answerB": "...",
    "answerC": "...",
    "answerD": "...",
    "correctAnswer": 0,
    "explanation": "...",
    "difficulty": 1,
    "funFact": "..." or null
  }
]`;
}

function questionsToKotlin(
	questions: QuizQuestion[],
	categoryId: number,
	functionName: string,
): string {
	const lines: string[] = [
		"package com.quizverse.app.data.prepopulate.questions",
		"",
		"import com.quizverse.app.data.database.entities.Question",
		"",
		`fun ${functionName}(): List<Question> = listOf(`,
	];

	for (let i = 0; i < questions.length; i++) {
		const q = questions[i];
		const comma = i < questions.length - 1 ? "," : "";
		const funFactLine =
			q.funFact !== null
				? `        funFact = ${JSON.stringify(q.funFact)}`
				: "        funFact = null";

		lines.push(`    Question(
        categoryId = ${categoryId},
        questionText = ${JSON.stringify(q.questionText)},
        answerA = ${JSON.stringify(q.answerA)},
        answerB = ${JSON.stringify(q.answerB)},
        answerC = ${JSON.stringify(q.answerC)},
        answerD = ${JSON.stringify(q.answerD)},
        correctAnswer = ${q.correctAnswer},
        explanation = ${JSON.stringify(q.explanation)},
        difficulty = ${q.difficulty},
${funFactLine}
    )${comma}`);
		if (comma) lines.push("");
	}

	lines.push(")");
	lines.push("");
	return lines.join("\n");
}

function parseQuestions(raw: string): QuizQuestion[] {
	// Extract JSON array from response (Nemotron might add markdown fences)
	const jsonMatch = raw.match(/\[[\s\S]*\]/);
	if (!jsonMatch) {
		throw new Error("Could not find JSON array in Nemotron response");
	}

	const parsed = JSON.parse(jsonMatch[0]) as QuizQuestion[];
	// Validate and sanitize
	return parsed.map((q) => ({
		questionText: String(q.questionText ?? ""),
		answerA: String(q.answerA ?? ""),
		answerB: String(q.answerB ?? ""),
		answerC: String(q.answerC ?? ""),
		answerD: String(q.answerD ?? ""),
		correctAnswer: Math.min(3, Math.max(0, Number(q.correctAnswer) || 0)),
		explanation: String(q.explanation ?? ""),
		difficulty: Math.min(5, Math.max(1, Number(q.difficulty) || 1)),
		funFact: q.funFact ? String(q.funFact) : null,
	}));
}

// ── MCP Server setup ────────────────────────────────────────────────────────
const server = new Server(
	{ name: "nemo-mcp", version: "1.0.0" },
	{ capabilities: { tools: {} } },
);

// List available tools
server.setRequestHandler(ListToolsRequestSchema, async () => ({
	tools: [
		{
			name: "nemo_ask",
			description:
				"Ask Nemotron 3 Super 120B a question. Free knowledge worker for general knowledge tasks, explanations, brainstorming, and text generation.",
			inputSchema: {
				type: "object" as const,
				properties: {
					prompt: {
						type: "string",
						description: "The question or task for Nemotron",
					},
					max_tokens: {
						type: "number",
						description: "Max response tokens (default: 4096)",
					},
					temperature: {
						type: "number",
						description: "Creativity 0.0-1.0 (default: 0.7)",
					},
				},
				required: ["prompt"],
			},
		},
		{
			name: "nemo_quiz",
			description:
				"Generate quiz questions for QuizVerse using parallel Nemotron workers. Each worker generates a batch of questions. Output is Kotlin code in QuizVerse format.",
			inputSchema: {
				type: "object" as const,
				properties: {
					topic: {
						type: "string",
						description:
							'Quiz topic, e.g. "Weltgeographie", "Geschichte", "Technologie"',
					},
					category_id: {
						type: "number",
						description: `Category ID (1-17): ${Object.entries(CATEGORIES)
							.map(([id, name]) => `${id}=${name}`)
							.join(", ")}`,
					},
					workers: {
						type: "number",
						description:
							"Number of parallel Nemotron workers (default: 5, max: 20)",
					},
					questions_per_worker: {
						type: "number",
						description:
							"Questions each worker generates (default: 25, max: 50)",
					},
					difficulty: {
						type: "string",
						description:
							'Difficulty filter: "1" (Easy) to "5" (Master), or "1-5" for mixed (default: "1-5")',
					},
					language: {
						type: "string",
						description: "Question language (default: Deutsch)",
					},
					function_prefix: {
						type: "string",
						description:
							'Kotlin function name prefix (default: derived from topic, e.g. "geoQuestionsNemo")',
					},
				},
				required: ["topic", "category_id"],
			},
		},
		{
			name: "nemo_research",
			description:
				"Deep knowledge research on any topic using Nemotron 3 Super. Returns structured research with categories, details, and recommendations. Perfect for app development research (e.g. outdoor activities, recipes, fitness exercises).",
			inputSchema: {
				type: "object" as const,
				properties: {
					topic: {
						type: "string",
						description:
							'Research topic, e.g. "Outdoor activities in Germany", "Italian recipes", "Yoga poses"',
					},
					format: {
						type: "string",
						description:
							'Output format: "json", "kotlin_data", "markdown", "csv" (default: "markdown")',
					},
					depth: {
						type: "string",
						description:
							'Research depth: "overview" (quick), "detailed" (thorough), "comprehensive" (maximum) (default: "detailed")',
					},
					count: {
						type: "number",
						description: "Number of items/results to generate (default: 20)",
					},
					language: {
						type: "string",
						description: "Output language (default: Deutsch)",
					},
					fields: {
						type: "string",
						description:
							'Comma-separated fields to include, e.g. "name,description,difficulty,location,duration"',
					},
				},
				required: ["topic"],
			},
		},
		{
			name: "nemo_generate",
			description:
				"Generate structured data or content using Nemotron 3 Super. Use for seed data, lists, descriptions, categories — any structured content generation.",
			inputSchema: {
				type: "object" as const,
				properties: {
					prompt: {
						type: "string",
						description: "What to generate",
					},
					format: {
						type: "string",
						description:
							'Output format: "json", "kotlin", "typescript", "csv", "markdown" (default: "json")',
					},
					count: {
						type: "number",
						description: "Number of items to generate (default: 10)",
					},
					language: {
						type: "string",
						description: "Content language (default: Deutsch)",
					},
				},
				required: ["prompt"],
			},
		},
		{
			name: "nemo_summarize",
			description:
				"Summarize text using Nemotron 3 Super. Free alternative for text summarization tasks.",
			inputSchema: {
				type: "object" as const,
				properties: {
					text: { type: "string", description: "Text to summarize" },
					style: {
						type: "string",
						description:
							'Summary style: "brief", "detailed", "bullets" (default: "brief")',
					},
					language: {
						type: "string",
						description: "Output language (default: same as input)",
					},
				},
				required: ["text"],
			},
		},
		{
			name: "nemo_translate",
			description:
				"Translate text using Nemotron 3 Super. Free translation between languages.",
			inputSchema: {
				type: "object" as const,
				properties: {
					text: { type: "string", description: "Text to translate" },
					target_language: {
						type: "string",
						description:
							'Target language (e.g. "English", "Deutsch", "French")',
					},
					source_language: {
						type: "string",
						description: "Source language (auto-detected if not specified)",
					},
				},
				required: ["text", "target_language"],
			},
		},
	],
}));

// Handle tool calls
server.setRequestHandler(CallToolRequestSchema, async (request) => {
	const { name, arguments: args } = request.params;

	try {
		switch (name) {
			// ── nemo_ask ──────────────────────────────────────────────────────
			case "nemo_ask": {
				const prompt = String(args?.prompt ?? "");
				const maxTokens = Number(args?.max_tokens) || 4096;
				const temperature = Number(args?.temperature) || 0.7;
				const result = await callNemotron(prompt, maxTokens, temperature);
				return { content: [{ type: "text", text: result }] };
			}

			// ── nemo_quiz ─────────────────────────────────────────────────────
			case "nemo_quiz": {
				const topic = String(args?.topic ?? "");
				const categoryId = Number(args?.category_id) || 1;
				const workers = Math.min(20, Math.max(1, Number(args?.workers) || 5));
				const questionsPerWorker = Math.min(
					50,
					Math.max(5, Number(args?.questions_per_worker) || 25),
				);
				const difficulty = String(args?.difficulty ?? "1-5");
				const language = String(args?.language ?? "Deutsch");
				const functionPrefix =
					String(args?.function_prefix ?? "") ||
					topic
						.toLowerCase()
						.replace(/[^a-z0-9äöü]/g, "")
						.replace(/^./, (c) => c.toLowerCase()) + "QuestionsNemo";

				// Launch parallel workers
				const workerPromises = Array.from({ length: workers }, (_, i) =>
					callNemotron(
						buildQuizPrompt(topic, questionsPerWorker, difficulty, language),
						8192,
						0.8 + (i % 3) * 0.05, // slight variation for diversity
					)
						.then((raw) => parseQuestions(raw))
						.catch((err) => {
							console.error(`Worker ${i + 1} failed: ${err}`);
							return [] as QuizQuestion[];
						}),
				);

				const results = await Promise.all(workerPromises);
				const allQuestions = results.flat();

				// Generate Kotlin files (one per worker batch)
				const kotlinFiles: string[] = [];
				let offset = 0;
				for (let i = 0; i < results.length; i++) {
					if (results[i].length === 0) continue;
					const funcName = `${functionPrefix}${i + 1}`;
					const kotlin = questionsToKotlin(results[i], categoryId, funcName);
					kotlinFiles.push(
						`// ── File: ${funcName.charAt(0).toUpperCase() + funcName.slice(1)}.kt ──\n${kotlin}`,
					);
					offset += results[i].length;
				}

				const successWorkers = results.filter((r) => r.length > 0).length;
				const summary = [
					`=== Nemo Quiz Generation Complete ===`,
					`Topic: ${topic} (Category ${categoryId}: ${CATEGORIES[categoryId] ?? "Unknown"})`,
					`Workers: ${successWorkers}/${workers} successful`,
					`Total questions: ${allQuestions.length}`,
					`Difficulty: ${difficulty}`,
					`Language: ${language}`,
					``,
					...kotlinFiles,
				].join("\n");

				return { content: [{ type: "text", text: summary }] };
			}

			// ── nemo_research ────────────────────────────────────────────────
			case "nemo_research": {
				const rTopic = String(args?.topic ?? "");
				const rFormat = String(args?.format ?? "markdown");
				const rDepth = String(args?.depth ?? "detailed");
				const rCount = Number(args?.count) || 20;
				const rLang = String(args?.language ?? "Deutsch");
				const rFields = args?.fields ? String(args.fields) : "";

				const depthInstructions: Record<string, string> = {
					overview: `Give a brief overview with ${rCount} key items.`,
					detailed: `Provide detailed research with ${rCount} items, each with thorough descriptions.`,
					comprehensive: `Provide the most comprehensive research possible with ${rCount} items, including details, context, pros/cons, and recommendations.`,
				};

				const formatInstructions: Record<string, string> = {
					json: "Output as a JSON array of objects.",
					kotlin_data: "Output as Kotlin data class instances.",
					markdown: "Output as structured Markdown with headers and lists.",
					csv: "Output as CSV with header row.",
				};

				const fieldsInstruction = rFields
					? `Each item must include these fields: ${rFields}.`
					: "";

				const prompt = `Research topic: "${rTopic}" in ${rLang}.
${depthInstructions[rDepth] ?? depthInstructions.detailed}
${formatInstructions[rFormat] ?? formatInstructions.markdown}
${fieldsInstruction}
Be factual and accurate. Include practical, actionable information.`;

				const result = await callNemotron(prompt, 8192, 0.6);
				return { content: [{ type: "text", text: result }] };
			}

			// ── nemo_generate ────────────────────────────────────────────────
			case "nemo_generate": {
				const gPrompt = String(args?.prompt ?? "");
				const gFormat = String(args?.format ?? "json");
				const gCount = Number(args?.count) || 10;
				const gLang = String(args?.language ?? "Deutsch");

				const formatMap: Record<string, string> = {
					json: "Output ONLY a valid JSON array, no other text.",
					kotlin: "Output as Kotlin code (data class instances or lists).",
					typescript: "Output as TypeScript code (typed arrays or objects).",
					csv: "Output as CSV with header row.",
					markdown: "Output as structured Markdown.",
				};

				const prompt = `${gPrompt}
Generate exactly ${gCount} items in ${gLang}.
${formatMap[gFormat] ?? formatMap.json}`;

				const result = await callNemotron(prompt, 8192, 0.7);
				return { content: [{ type: "text", text: result }] };
			}

			// ── nemo_summarize ────────────────────────────────────────────────
			case "nemo_summarize": {
				const text = String(args?.text ?? "");
				const style = String(args?.style ?? "brief");
				const language = args?.language ? String(args.language) : "";

				const styleInstructions: Record<string, string> = {
					brief: "Summarize in 2-3 sentences.",
					detailed: "Provide a detailed summary covering all key points.",
					bullets: "Summarize as a bullet point list of key points.",
				};

				const prompt = `${styleInstructions[style] ?? styleInstructions.brief}${language ? ` Answer in ${language}.` : ""}\n\nText:\n${text}`;
				const result = await callNemotron(prompt, 2048, 0.3);
				return { content: [{ type: "text", text: result }] };
			}

			// ── nemo_translate ────────────────────────────────────────────────
			case "nemo_translate": {
				const text = String(args?.text ?? "");
				const targetLang = String(args?.target_language ?? "English");
				const sourceLang = args?.source_language
					? ` from ${args.source_language}`
					: "";

				const prompt = `Translate the following text${sourceLang} to ${targetLang}. Only output the translation, nothing else.\n\nText:\n${text}`;
				const result = await callNemotron(prompt, 4096, 0.2);
				return { content: [{ type: "text", text: result }] };
			}

			default:
				return {
					content: [{ type: "text", text: `Unknown tool: ${name}` }],
					isError: true,
				};
		}
	} catch (error) {
		const message = error instanceof Error ? error.message : String(error);
		return {
			content: [{ type: "text", text: `Nemo error: ${message}` }],
			isError: true,
		};
	}
});

// ── Start server ────────────────────────────────────────────────────────────
async function main() {
	const transport = new StdioServerTransport();
	await server.connect(transport);
	console.error("Nemo MCP server running (Nemotron 3 Super 120B via NIM API)");
}

main().catch((err) => {
	console.error("Failed to start Nemo MCP server:", err);
	process.exit(1);
});

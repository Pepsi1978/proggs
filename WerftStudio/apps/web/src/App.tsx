import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { DesignDocument } from "@werft/contracts";
import { Archive, ArrowLeft, Box, Check, ChevronLeft, CircleUserRound, Code2, Download, FileArchive, FileText, FolderOpen, Grid2X2, History, Home, Image, LayoutDashboard, Maximize2, MessageSquare, Minimize2, Minus, Moon, MoreHorizontal, MousePointer2, Palette, PanelLeft, PanelRight, PenLine, Pipette, Play, Plus, RotateCcw, Search, Send, Settings, Share2, Sparkles, Sun, Trash2, Undo2, Upload, Users, WandSparkles, X, ZoomIn, ZoomOut } from "lucide-react";
import { type FormEvent, type PointerEvent as ReactPointerEvent, type ReactNode, useEffect, useRef, useState } from "react";
import { Link, Navigate, NavLink, Route, Routes, useLocation, useNavigate, useParams } from "react-router-dom";
import { api, apiEventStream, apiFormProgress, ApiError } from "./api";
import { canvasZoomFromWheel, fitZoomAndOffset, offsetForZoomAtPoint, physicalZoomForDevice, type CanvasPoint } from "./canvas-navigation";
import { androidStartDevices, aspectRatioLabel, type StageDevice, deviceLabel, referenceDevices, stageDeviceFor, stageDeviceForDesign } from "./reference-devices";
import { hexColour, tokenTitle } from "./design-colours";
import { chatStepText, designFileLabel } from "./chat-progress";
import { type ToolMode, useUi } from "./store";

type Project = { id: string; name: string; type: string; fidelity: string; platforms: string[]; activeVersion: number; updatedAt: string; previewPath?: string };
type ProviderId = "openai-codex" | "openrouter";
type ModelSelection = { provider: ProviderId; model: string; effort?: string };
type OpenRouterEndpoint = { providerName: string; providerSlug: string; tag: string; endpointId: string; promptPerToken: number; completionPerToken: number; cacheReadPerToken: number; contextLength: number; maxCompletionTokens?: number; quantization: string; throughputLast30m?: number; uptimeLast5m?: number; status: number };
type ProviderModel = { provider: ProviderId; id: string; name: string; contextLength?: number; efforts: string[]; defaultEffort?: string; endpoint?: OpenRouterEndpoint };
type OpenRouterModelDetails = { model: ProviderModel; endpoints: OpenRouterEndpoint[] };
type ModelCatalog = { models: ProviderModel[]; selection?: ModelSelection; openrouterError?: string; selectionError?: string };
const modelKey = (model: Pick<ProviderModel, "provider" | "id">) => JSON.stringify([model.provider, model.id]);
const providerName = (provider: ProviderId) => provider === "openai-codex" ? "OpenAI · Codex OAuth" : "OpenRouter · API";
const modelOptionName = (model: ProviderModel) => model.endpoint ? `${model.name} · ${model.endpoint.providerName}` : model.name;
const endpointKey = (endpoint: OpenRouterEndpoint) => endpoint.endpointId || endpoint.tag || endpoint.providerSlug;
const endpointPrice = (value: number) => (value * 1_000_000).toLocaleString("en-US", { minimumFractionDigits: 0, maximumFractionDigits: 4 });
const endpointStatus = (endpoint: OpenRouterEndpoint) => endpoint.status === 0 ? "ok" : endpoint.status === -2 ? "eingeschränkt" : endpoint.status === -5 ? "gestört" : `Status ${endpoint.status}`;
const endpointStatusKind = (endpoint: OpenRouterEndpoint): "success" | "warning" | "error" => endpoint.status >= 0 && !(endpoint.uptimeLast5m !== undefined && endpoint.uptimeLast5m < 80) ? "success" : endpoint.status === -2 ? "warning" : "error";
const previewOriginFor = () => `${window.location.protocol}//${window.location.hostname}:8444`;
type Me = { id: string; email: string; name: string; role: string; organizationName: string };
type ImportFile = { path: string; size: number; mime: string };
// Fuer jedes Geraeteformat kann eine eigene Fassung des Designs aufgebaut sein; ohne sie zeigt die
// Buehne die Grundfassung, die die zusaetzliche Flaeche nicht nutzt.
type DesignVariant = { width: number; height: number; previewPath: string };
type ProjectImport = { imported: false } | { imported: true; entryPath: string; reconstructed: boolean; fileCount: number; totalBytes: number; revision: number; files: ImportFile[]; previewWidth: number; previewHeight: number; previewDevice: string; variants: DesignVariant[]; previewPath?: string };
type ReconstructionTodo = { label: string; status: "pending" | "running" | "completed" };
type ReconstructionProgress = { phase: string; message: string; processedFiles: number; totalFiles: number; processedBytes: number; totalBytes: number; todos: ReconstructionTodo[]; phaseProgress?: number | null | undefined; elapsedMs?: number | undefined; estimatedRemainingMs?: number | null | undefined; completedOperations?: number | undefined; totalOperations?: number | undefined; retryCount?: number | undefined; currentOperation?: string | undefined; entryPath?: string; revision?: number };
type ReconstructionJob = { id: string; status: "queued" | "running" | "completed" | "failed"; progress: number; result: ReconstructionProgress | null; errorCode: string | null; attempts?: number; heartbeatAt?: string };
type ImportProgressState = { percent: number; phaseProgress: number | null; message: string; loaded: number; total: number; elapsedMs: number; estimatedRemainingMs: number | null; currentOperation?: string | undefined; completedOperations?: number | undefined; totalOperations?: number | undefined; retryCount?: number | undefined; todos: ReconstructionTodo[] };
const labels: Record<string, string> = { prototype: "Prototyp", presentation: "Präsentation", document: "Dokument", template: "Vorlage", canvas: "Freie Fläche", web: "Web", android: "Android", ios: "iOS", ipados: "iPadOS", macos: "macOS", windows: "Windows" };
const examples = [
  ["Banking App „Fluss“", "iOS · Android", "Konten, Zahlungen und Tagesüberblick mit ruhiger Typografie.", "Prototyp"],
  ["CRM „Atlas“", "Responsive Web", "Pipeline-Board, Kontakte und Berichte für Vertriebsteams.", "Prototyp"],
  ["Bestellterminal", "Windows", "Kiosk-Flow mit großen Touchzielen und Fluent-Mustern.", "Prototyp"],
  ["Produkt-Pitch", "Präsentation", "12 Folien mit Sprechernotizen und Diagramm-Layouts.", "Präsentation"],
  ["Reiseplaner", "Android", "Onboarding, Suche und Buchung nach Material 3.", "Prototyp"],
  ["Preisliste 2026", "Dokument", "A4-Dokument mit Tabellen und Druckansicht.", "Dokument"],
];

const pause = (milliseconds: number) => new Promise((resolve) => window.setTimeout(resolve, milliseconds));
function formatDuration(milliseconds: number): string {
  const totalSeconds = Math.max(0, Math.ceil(milliseconds / 1_000));
  if (totalSeconds < 60) return `${totalSeconds} Sek.`;
  const totalMinutes = Math.floor(totalSeconds / 60), seconds = totalSeconds % 60;
  if (totalMinutes < 60) return `${totalMinutes} Min.${seconds ? ` ${seconds} Sek.` : ""}`;
  const hours = Math.floor(totalMinutes / 60), minutes = totalMinutes % 60;
  return `${hours} Std.${minutes ? ` ${minutes} Min.` : ""}`;
}
function ProgressMeters({ percent, phaseProgress, elapsedMs, estimatedRemainingMs, currentOperation }: { percent: number; phaseProgress?: number | null | undefined; elapsedMs?: number | undefined; estimatedRemainingMs?: number | null | undefined; currentOperation?: string | undefined }) {
  return (
    <div className="progress-meters">
      <div className="progress-meter">
        <div><span>Gesamtfortschritt</span><strong>{Math.round(percent)} %</strong></div>
        <progress aria-label="Gesamtfortschritt" value={percent} max="100" />
      </div>
      <div className="progress-meter secondary">
        <div><span>{currentOperation ?? "Aktueller Schritt"}</span><strong>{phaseProgress == null ? "läuft" : `${Math.round(phaseProgress)} %`}</strong></div>
        {phaseProgress == null ? <progress aria-label="Aktueller Schritt läuft" max="100" /> : <progress aria-label="Fortschritt des aktuellen Schritts" value={phaseProgress} max="100" />}
      </div>
      <small>{elapsedMs == null ? "Laufzeit startet" : `Laufzeit ${formatDuration(elapsedMs)}`} · {estimatedRemainingMs == null ? "Restzeit wird kalibriert" : `noch etwa ${formatDuration(estimatedRemainingMs)}`}</small>
    </div>
  );
}
type RebuildViewport = { width: number; height: number; device: string };
async function reconstructProject(projectId: string, onProgress: (job: ReconstructionJob) => void, retryFailed = false, force = false, viewport?: RebuildViewport): Promise<ReconstructionJob> {
  let started = await api<{ jobId: string }>(`/projects/${projectId}/design/reconstruct`, { method: "POST", body: JSON.stringify({ retryFailed, force, ...(viewport ? { viewport } : {}) }) });
  let networkRetries = 0;
  let interruptedRestarts = 0;
  for (;;) {
    let job: ReconstructionJob;
    try {
      job = await api<ReconstructionJob>(`/jobs/${started.jobId}`);
      networkRetries = 0;
    } catch (error) {
      if (networkRetries >= 75) throw error;
      networkRetries += 1;
      await pause(800);
      continue;
    }
    onProgress(job);
    if (job.status === "completed") return job;
    if (job.status === "failed" && job.errorCode === "RECONSTRUCT_INTERRUPTED" && interruptedRestarts < 1) {
      interruptedRestarts += 1;
      started = await api<{ jobId: string }>(`/projects/${projectId}/design/reconstruct`, { method: "POST", body: JSON.stringify({ retryFailed: true, ...(viewport ? { viewport } : {}) }) });
      continue;
    }
    if (job.status === "failed") throw new ApiError(job.errorCode ?? "RECONSTRUCT_FAILED", job.result?.message ?? "Die HTML-Rekonstruktion ist fehlgeschlagen.", 500);
    await pause(800);
  }
}

// Ein laufender KI-Auftrag muss SICHTBAR laufen: wandernde Punkte, die verstrichene Zeit und worauf
// gerade gearbeitet wird. Ohne dieses Zeichen ist nicht erkennbar, ob noch etwas passiert.
function WorkingIndicator({ detail, step, onCancel }: { detail: string; step?: string; onCancel?: () => void }) {
  const [seconds, setSeconds] = useState(0);
  useEffect(() => {
    const timer = window.setInterval(() => setSeconds((value) => value + 1), 1_000);
    return () => window.clearInterval(timer);
  }, []);
  const minutes = Math.floor(seconds / 60);
  return (
    <div className="assistant-message working-message">
      <span className="working-line">
        <span className="working-dots" aria-hidden="true"><i /><i /><i /></span>
        {step || "Die KI arbeitet am Design …"} {minutes}:{String(seconds % 60).padStart(2, "0")}
      </span>
      <small>{detail} · das kann je nach Modell ein bis zwei Minuten dauern</small>
      {onCancel && <button type="button" className="working-cancel" onClick={onCancel}>Abbrechen</button>}
    </div>
  );
}

function Button({ children, variant = "secondary", className = "", ...props }: { children: ReactNode; variant?: "primary" | "secondary" | "ghost" | "danger"; className?: string } & React.ButtonHTMLAttributes<HTMLButtonElement>) {
  return (
    <button className={`button ${variant} ${className}`} {...props}>
      {children}
    </button>
  );
}
function IconButton({ label, children, className = "", ...props }: { label: string; children: ReactNode; className?: string } & React.ButtonHTMLAttributes<HTMLButtonElement>) {
  return (
    <button className={`icon-button ${className}`} aria-label={label} title={label} {...props}>
      {children}
    </button>
  );
}
function Status({ kind = "neutral", children }: { kind?: "success" | "warning" | "error" | "info" | "neutral"; children: ReactNode }) {
  return (
    <span className={`status ${kind}`}>
      <i />
      {children}
    </span>
  );
}
function Modal({ title, children, width = "540px", className = "", onClose }: { title: string; children: ReactNode; width?: string; className?: string; onClose(): void }) {
  return (
    <div className="modal-backdrop" onMouseDown={onClose}>
      <section className={`modal ${className}`} style={{ maxWidth: width }} onMouseDown={(e) => e.stopPropagation()} role="dialog" aria-modal="true" aria-label={title}>
        <header>
          <h2>{title}</h2>
          <IconButton label="Schließen" onClick={onClose}>
            <X size={16} />
          </IconButton>
        </header>
        {children}
      </section>
    </div>
  );
}

function Onboarding() {
  const nav = useNavigate();
  const [step, setStep] = useState(0);
  const [selected, setSelected] = useState<string[][]>([[], [], ["iOS"], []]);
  const data = [
    ["Was beschreibt deine Rolle?", "Bestimmt Startansicht und Vorschläge.", ["Produktdesign", "Produktmanagement", "Entwicklung", "Markenverantwortung", "Review"]],
    ["Was möchtest du meistens erstellen?", "Mehrfachauswahl möglich.", ["Prototypen", "Präsentationen", "Dokumente", "Freie Arbeitsflächen"]],
    ["Für welche Plattformen entwirfst du?", "Steuert Gerätepresets und Komponenten.", ["Responsive Web", "Android", "iOS", "iPadOS", "macOS", "Windows"]],
    ["Wie soll die KI verbunden werden?", "Später jederzeit änderbar.", ["Organisationsstandard", "Eigenen Endpunkt verbinden", "Später einrichten"]],
  ] as const;
  const current = data[step]!;
  const single = step === 0 || step === 3;
  const toggle = (value: string) => setSelected((all) => all.map((list, index) => (index === step ? (single ? [value] : list.includes(value) ? list.filter((x) => x !== value) : [...list, value]) : list)));
  return (
    <main className="center-page">
      <section className="onboarding-card">
        <div className="steps">
          {data.map((_, i) => (
            <i className={i <= step ? "active" : ""} key={i} />
          ))}
        </div>
        <h1>{current[0]}</h1>
        <p>{current[1]}</p>
        <div className="choice-list">
          {current[2].map((item) => (
            <Button key={item} className={selected[step]!.includes(item) ? "selected" : ""} onClick={() => toggle(item)}>
              {item}
            </Button>
          ))}
        </div>
        <footer>
          <Button onClick={() => setStep(Math.max(0, step - 1))}>Zurück</Button>
          <span />
          <Button variant="ghost" onClick={() => nav("/app/designs")}>
            Überspringen
          </Button>
          <Button variant="primary" onClick={() => (step === 3 ? nav("/app/designs") : setStep(step + 1))}>
            {step === 3 ? "Los geht’s" : "Weiter"}
          </Button>
        </footer>
      </section>
    </main>
  );
}

function AppRail() {
  return (
    <nav className="app-rail" aria-label="Hauptnavigation">
      <Link className="mark" to="/app/designs">
        W
      </Link>
      <NavLink to="/app/designs" title="Designs">
        <Grid2X2 />
      </NavLink>
      <NavLink to="/app/examples" title="Beispiele">
        <LayoutDashboard />
      </NavLink>
      <NavLink to="/app/design-systems" title="Designsysteme">
        <Palette />
      </NavLink>
      <span />
      <NavLink to="/app/settings/models" title="Einstellungen">
        <Settings />
      </NavLink>
    </nav>
  );
}
function HubShell({ children, title, actions }: { children: ReactNode; title: string; actions?: ReactNode }) {
  const { theme, setTheme } = useUi();
  return (
    <div className="shell" data-theme={theme}>
      <AppRail />
      <div className="shell-main">
        <header className="hub-header">
          <h1>{title}</h1>
          <span />
          {actions}
          <IconButton label="Theme wechseln" onClick={() => setTheme(theme === "dark" ? "light" : "dark")}>
            {theme === "dark" ? <Sun /> : <Moon />}
          </IconButton>
        </header>
        {children}
      </div>
    </div>
  );
}

function Hub() {
  const [query, setQuery] = useState("");
  const [filter, setFilter] = useState("Alle");
  const [newOpen, setNewOpen] = useState(false);
  const [importOpen, setImportOpen] = useState(false);
  const client = useQueryClient();
  const projects = useQuery({
    queryKey: ["projects"],
    queryFn: () => api<Project[]>("/projects"),
  });
  const remove = useMutation({ mutationFn: (id: string) => api<{ deleted: boolean }>(`/projects/${id}`, { method: "DELETE" }), onSuccess: () => client.invalidateQueries({ queryKey: ["projects"] }) });
  const list = (projects.data ?? []).filter((p) => (filter === "Alle" || labels[p.type] === filter || p.platforms.some((x) => labels[x] === filter)) && p.name.toLowerCase().includes(query.toLowerCase()));
  return (
    <HubShell
      title="Designs"
      actions={
        <>
          <div className="search">
            <Search />
            <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Projekte durchsuchen …" />
          </div>
          <Button onClick={() => setImportOpen(true)}>
            <Upload size={15} /> Importieren
          </Button>
          <Button variant="primary" onClick={() => setNewOpen(true)}>
            <Plus size={15} /> Neues Design
          </Button>
        </>
      }
    >
      <main className="hub-content">
        <div className="chips">
          {["Alle", "Prototyp", "Präsentation", "Dokument", "Web", "Android", "iOS", "Windows"].map((x) => (
            <button className={filter === x ? "active" : ""} key={x} onClick={() => setFilter(x)}>
              {x}
            </button>
          ))}
        </div>
        <h2 className="eyebrow">Zuletzt bearbeitet</h2>
        {remove.error && <p className="field-error">{remove.error instanceof ApiError ? remove.error.message : "Das Projekt konnte nicht gelöscht werden."}</p>}
        {projects.isLoading ? (
          <div className="empty">Projekte werden geladen …</div>
        ) : projects.isError ? (
          <div className="empty error">Projekte konnten nicht geladen werden. API und Datenbank prüfen.</div>
        ) : list.length ? (
          <div className="project-grid">
            {list.map((project) => (
              <Link to={`/app/projects/${project.id}/studio/canvas`} className="project-card" key={project.id}>
                <button
                  className="card-delete"
                  title="Projekt löschen"
                  disabled={remove.isPending}
                  onClick={(event) => {
                    event.preventDefault();
                    event.stopPropagation();
                    if (window.confirm(`Projekt „${project.name}“ wirklich löschen? Importierte Dateien werden mit entfernt.`)) remove.mutate(project.id);
                  }}
                >
                  <X size={14} />
                </button>
                <ProjectThumbnail name={project.name} previewPath={project.previewPath} />
                <div className="card-body">
                  <div>
                    <strong>{project.name}</strong>
                    <em>{labels[project.type] ?? project.type}</em>
                  </div>
                  <div className="platforms">
                    {project.platforms.map((p) => (
                      <span key={p}>{labels[p] ?? p}</span>
                    ))}
                  </div>
                  <small>v{project.activeVersion} · zuletzt bearbeitet</small>
                </div>
              </Link>
            ))}
          </div>
        ) : (
          <div className="empty">
            <strong>Keine Treffer</strong>
            <span>Für die aktive Suche oder den Filter gibt es keine Projekte.</span>
            <Button
              onClick={() => {
                setQuery("");
                setFilter("Alle");
              }}
            >
              Filter zurücksetzen
            </Button>
          </div>
        )}
      </main>
      {importOpen && <ImportProject onClose={() => setImportOpen(false)} />}
      {newOpen && <NewProject onClose={() => setNewOpen(false)} />}
    </HubShell>
  );
}

// Die Karte zeigt das Design selbst, nicht ein Streifenmuster. Wie gross das Design ist, meldet die
// Vorschau; erst mit dieser Zahl laesst sich der Bildschirm VOLLSTAENDIG in die Kachel einpassen —
// ein fester Verkleinerungsfaktor zeigte je nach Projekt nur eine Ecke oder eine leere Flaeche.
function ProjectThumbnail({ name, previewPath }: { name: string; previewPath: string | undefined }) {
  const boxRef = useRef<HTMLDivElement>(null);
  const frameRef = useRef<HTMLIFrameElement>(null);
  const [design, setDesign] = useState({ width: 412, height: 915 });
  const [box, setBox] = useState({ width: 0, height: 0 });
  const [shown, setShown] = useState(false);
  useEffect(() => {
    const handleMessage = (event: MessageEvent) => {
      const data = event.data as { source?: string; action?: string; width?: number; height?: number } | null;
      if (!data || data.source !== "werft-preview-canvas" || data.action !== "size") return;
      if (event.source !== frameRef.current?.contentWindow) return;
      if (!Number.isFinite(data.width) || !Number.isFinite(data.height) || !data.width || !data.height) return;
      setDesign((current) => (current.width === Math.round(data.width!) && current.height === Math.round(data.height!) ? current : { width: Math.round(data.width!), height: Math.round(data.height!) }));
      setShown(true);
    };
    window.addEventListener("message", handleMessage);
    return () => window.removeEventListener("message", handleMessage);
  }, []);
  // Meldet eine Vorschau keine Groesse — etwa weil ihr Inhalt erst im Browser entsteht und leer
  // bleibt —, wird sie nach kurzer Frist trotzdem gezeigt. Sonst haette die Karte gar nichts, und
  // gerade an so einem Projekt sieht man am ehesten, dass mit ihm etwas nicht stimmt.
  const showAnyway = () => window.setTimeout(() => setShown(true), 1_200);
  useEffect(() => {
    const element = boxRef.current;
    if (!element || typeof ResizeObserver !== "function") return;
    const measure = () => {
      const rect = element.getBoundingClientRect();
      setBox((current) => (Math.abs(current.width - rect.width) < 1 && Math.abs(current.height - rect.height) < 1 ? current : { width: rect.width, height: rect.height }));
    };
    measure();
    const observer = new ResizeObserver(measure);
    observer.observe(element);
    return () => observer.disconnect();
  }, []);
  if (!previewPath) return <div className="preview thumb" ref={boxRef}><span>Noch keine Vorschau · Design im Studio aufbauen</span></div>;
  const separator = previewPath.includes("?") ? "&" : "?";
  // Skaliert wird auf die BREITE der Kachel und oben angesetzt: ein hochformatiger Bildschirm waere
  // vollstaendig eingepasst nur noch ein fingerbreiter Streifen, auf dem nichts zu erkennen ist. So
  // steht der Kopf des Designs gross da — wie in jedem Entwurfswerkzeug. Ein flaches Design, das
  // ganz hineinpasst, wird stattdessen mittig gesetzt.
  const scale = box.width ? box.width / design.width : 0;
  const offsetY = design.height * scale < box.height ? (box.height - design.height * scale) / 2 : 0;
  return (
    <div className={`preview thumb${shown ? " shown" : ""}`} ref={boxRef}>
      <iframe
        ref={frameRef}
        className="card-preview"
        title={`Vorschau ${name}`}
        src={`${previewOriginFor()}${previewPath}${separator}werftStage=1`}
        loading="lazy"
        tabIndex={-1}
        scrolling="no"
        onLoad={showAnyway}
        sandbox="allow-scripts allow-same-origin"
        style={{ width: design.width, height: design.height, transform: `translate(0, ${offsetY}px) scale(${scale})` }}
      />
    </div>
  );
}

function ImportProject({ onClose }: { onClose(): void }) {
  const nav = useNavigate();
  const client = useQueryClient();
  const [name, setName] = useState("Importiertes Design");
  const [platform, setPlatform] = useState("web");
  const [projectType, setProjectType] = useState("prototype");
  const [frontendOnly, setFrontendOnly] = useState(true);
  const [files, setFiles] = useState<File[]>([]);
  const [progress, setProgress] = useState<ImportProgressState | null>(null);
  const selectFiles = (selected: FileList | null) => {
    const next = Array.from(selected ?? []);
    setFiles(next);
    const first = next[0];
    if (!first) return;
    const root = first.webkitRelativePath.split("/")[0];
    const inferred = root || first.name.replace(/\.(zip|html?|json|werft)$/i, "");
    if (inferred) setName(inferred.slice(0, 120));
  };
  const upload = useMutation({
    mutationFn: async () => {
      const uploadStartedAt = Date.now();
      const form = new FormData();
      form.append("name", name);
      form.append("platform", platform);
      form.append("type", projectType);
      form.append("frontendOnly", frontendOnly ? "true" : "false");
      for (const file of files) {
        form.append("fileSize", String(file.size));
        form.append("files", file, file.webkitRelativePath || file.name);
      }
      setProgress({ percent: 0, phaseProgress: 0, message: "Projektdateien werden hochgeladen.", loaded: 0, total: files.reduce((sum, file) => sum + file.size, 0), elapsedMs: 0, estimatedRemainingMs: null, currentOperation: "Projektdateien hochladen", todos: [] });
      const imported = await apiFormProgress<{ projectId: string; requiresReconstruction: boolean }>("/imports", form, (loaded, total) => {
        const knownTotal = total || files.reduce((sum, file) => sum + file.size, 0);
        const elapsedMs = Date.now() - uploadStartedAt;
        setProgress({ percent: knownTotal ? Math.min(35, loaded / knownTotal * 35) : 5, phaseProgress: knownTotal ? Math.min(100, loaded / knownTotal * 100) : null, message: "Projektdateien werden vollständig eingelesen.", loaded, total: knownTotal, elapsedMs, estimatedRemainingMs: loaded > 0 && knownTotal > loaded ? elapsedMs / loaded * (knownTotal - loaded) : null, currentOperation: "Projektdateien hochladen", todos: [] });
      });
      let reconstructionCompleted = true;
      if (imported.requiresReconstruction) {
        try {
          await reconstructProject(imported.projectId, (job) => setProgress({
            percent: 35 + job.progress * 0.65,
            message: job.result?.message ?? "UI-Quellen werden verarbeitet.",
            loaded: job.result?.processedBytes ?? 0,
            total: job.result?.totalBytes ?? 0,
            phaseProgress: job.result?.phaseProgress ?? null,
            elapsedMs: job.result?.elapsedMs ?? 0,
            estimatedRemainingMs: job.result?.estimatedRemainingMs ?? null,
            currentOperation: job.result?.currentOperation ?? "UI-Quellen verarbeiten",
            completedOperations: job.result?.completedOperations,
            totalOperations: job.result?.totalOperations,
            retryCount: job.result?.retryCount,
            todos: job.result?.todos ?? []
          }));
        } catch {
          reconstructionCompleted = false;
          setProgress((current) => ({ percent: current?.percent ?? 35, phaseProgress: current?.phaseProgress ?? null, message: "Projekt importiert; die HTML-Rekonstruktion kann im Studio erneut gestartet werden.", loaded: current?.loaded ?? 0, total: current?.total ?? 0, elapsedMs: current?.elapsedMs ?? 0, estimatedRemainingMs: null, currentOperation: current?.currentOperation, completedOperations: current?.completedOperations, totalOperations: current?.totalOperations, retryCount: current?.retryCount, todos: current?.todos ?? [] }));
        }
      }
      if (reconstructionCompleted) setProgress((current) => ({ percent: 100, phaseProgress: 100, message: "Design vollständig importiert, geprüft und gespeichert.", loaded: current?.total ?? 0, total: current?.total ?? 0, elapsedMs: current?.elapsedMs ?? 0, estimatedRemainingMs: 0, currentOperation: "Import abgeschlossen", completedOperations: current?.completedOperations, totalOperations: current?.totalOperations, retryCount: current?.retryCount, todos: current?.todos ?? [] }));
      return imported;
    },
    onSuccess: async ({ projectId }) => {
      await client.invalidateQueries({ queryKey: ["projects"] });
      onClose();
      nav(`/app/projects/${projectId}/studio/canvas`);
    },
  });
  return (
    <Modal title="Design importieren" width="680px" className="import-modal" onClose={upload.isPending ? () => {} : onClose}>
      <div className="modal-body import-dialog">
        <p className="subtle">Misst Farben, Abmessungen, Typografie und Effekte exakt aus den Projektquellen und baut daraus jeden Bildschirm einzeln als durchklickbare, direkt bearbeitbare HTML-Version — anschließend wird das Ergebnis gegen die gemessenen Werte nachgeprüft.</p>
        <label>
          Projektname
          <input value={name} maxLength={120} onChange={(event) => setName(event.target.value)} />
        </label>
        <label>
          Um was für eine App handelt es sich?
          <div className="chips">
            {[
              ["android", "Android"],
              ["ios", "iOS"],
              ["windows", "Windows"],
              ["web", "Web"],
              ["macos", "macOS"],
              ["ipados", "iPadOS"],
            ].map(([id, label]) => (
              <button type="button" className={platform === id ? "active" : ""} key={id} onClick={() => setPlatform(id!)}>
                {label}
              </button>
            ))}
          </div>
        </label>
        <label>
          Projektart
          <div className="chips">
            {[
              ["prototype", "App / Prototyp"],
              ["presentation", "Präsentation"],
              ["document", "Dokument"],
              ["template", "Vorlage"],
              ["canvas", "Freie Fläche"],
            ].map(([id, label]) => (
              <button type="button" className={projectType === id ? "active" : ""} key={id} onClick={() => setProjectType(id!)}>
                {label}
              </button>
            ))}
          </div>
        </label>
        <div className="import-choices">
          <label className="import-choice">
            <FolderOpen />
            <strong>Projektordner wählen</strong>
            <span>Für vollständige App-, Software- und Prototyp-Repositories</span>
            <input type="file" multiple ref={(node) => node?.setAttribute("webkitdirectory", "")} onChange={(event) => selectFiles(event.target.files)} />
          </label>
          <label className="import-choice">
            <FileArchive />
            <strong>ZIP oder Designdatei wählen</strong>
            <span>ZIP, HTML, Werft-JSON oder mehrere zusammengehörige Dateien</span>
            <input type="file" multiple onChange={(event) => selectFiles(event.target.files)} />
          </label>
        </div>
        {files.length > 0 && (
          <div className="import-summary">
            <Check />
            <span>
              <strong>
                {files.length} Datei{files.length === 1 ? "" : "en"} ausgewählt
              </strong>
              <small>
                {files
                  .slice(0, 3)
                  .map((file) => file.webkitRelativePath || file.name)
                  .join(" · ")}
                {files.length > 3 ? " · …" : ""}
              </small>
            </span>
          </div>
        )}
        <div className="setting-row">
          <span>
            <strong>Nur Frontend übernehmen</strong>
            <small>Übernimmt sämtliche First-Party-UI-Quellen, Navigationen, Themes, Ressourcen, Bilder und Fonts; Backend-Code bleibt außen vor. Build-Ausgaben, Werkzeugberichte und Abhängigkeiten werden generell nicht importiert — sie enthalten kein Design und würden den Import nur verlangsamen.</small>
          </span>
          <button type="button" className={`switch ${frontendOnly ? "on" : ""}`} onClick={() => setFrontendOnly(!frontendOnly)}>
            <i />
          </button>
        </div>
        <p className="subtle">Die Projektgröße begrenzt nicht die Designanalyse: große UI-Quellen werden vollständig in überprüfbare Verarbeitungspakete zerlegt, statt still übersprungen oder zusammengequetscht zu werden.</p>
        {progress && (
          <section className="import-progress">
            <div><strong aria-live="polite" aria-atomic="true">{progress.message}</strong><span>{Math.round(progress.percent)} %</span></div>
            <ProgressMeters percent={progress.percent} phaseProgress={progress.phaseProgress} elapsedMs={progress.elapsedMs} estimatedRemainingMs={progress.estimatedRemainingMs} currentOperation={progress.currentOperation} />
            {progress.total > 0 && <small>{progress.loaded.toLocaleString("de-DE")} von {progress.total.toLocaleString("de-DE")} Bytes verarbeitet</small>}
            {(progress.totalOperations ?? 0) > 0 && <small>{progress.completedOperations ?? 0} von {progress.totalOperations} KI-Schritten abgeschlossen{progress.retryCount ? ` · ${progress.retryCount} Verbindungswiederholung${progress.retryCount === 1 ? "" : "en"}` : ""}</small>}
            {progress.todos.length > 0 && <div className="import-todos">{progress.todos.map((todo) => <span className={todo.status} key={todo.label}>{todo.status === "completed" ? <Check /> : <i />}{todo.label}</span>)}</div>}
          </section>
        )}
        {upload.error && <p className="field-error">{upload.error instanceof ApiError ? upload.error.message : "Das Projekt konnte nicht importiert werden."}</p>}
      </div>
      <div className="modal-actions">
        <Button disabled={upload.isPending} onClick={onClose}>Abbrechen</Button>
        <Button variant="primary" disabled={!files.length || !name.trim() || upload.isPending} onClick={() => upload.mutate()}>
          {upload.isPending ? `${Math.round(progress?.percent ?? 0)} % · Design wird aufgebaut …` : "Importieren & öffnen"}
        </Button>
      </div>
    </Modal>
  );
}

function NewProject({ onClose }: { onClose(): void }) {
  const nav = useNavigate();
  const client = useQueryClient();
  const [name, setName] = useState("Unbenanntes Design");
  const [type, setType] = useState("prototype");
  const [platforms, setPlatforms] = useState(["android"]);
  const [prompt, setPrompt] = useState("");
  const [deviceId, setDeviceId] = useState(androidStartDevices[0]!.id);
  const initialDevice = stageDeviceFor(deviceId, false);
  const create = useMutation({
    mutationFn: () =>
      api<{ projectId: string }>("/projects", {
        method: "POST",
        body: JSON.stringify({
          name,
          type,
          fidelity: "high_fidelity",
          platforms,
          prompt,
          designSystemVersionId: null,
          aiProfile: "standard",
          ...(platforms.includes("android") && initialDevice ? { viewport: { width: initialDevice.width, height: initialDevice.height, device: initialDevice.label } } : {}),
        }),
      }),
    onSuccess: async (data) => {
      await client.invalidateQueries({ queryKey: ["projects"] });
      nav(`/app/projects/${data.projectId}/studio/questions`);
    },
  });
  return (
    <Modal title="Neues Design" width="680px" onClose={onClose}>
      <div className="modal-body">
        <label>
          Projektart
          <div className="type-grid">
            {[
              ["prototype", "Prototyp"],
              ["presentation", "Präsentation"],
              ["document", "Dokument"],
              ["template", "Vorlage"],
              ["canvas", "Freie Fläche"],
            ].map(([id, label]) => (
              <button className={type === id ? "active" : ""} onClick={() => setType(id!)} key={id}>
                <Box />
                {label}
              </button>
            ))}
          </div>
        </label>
        <div className="form-row">
          <label>
            Name
            <input value={name} onChange={(e) => setName(e.target.value)} />
          </label>
          <label>
            Qualität
            <select>
              <option>High Fidelity</option>
              <option>Wireframe</option>
            </select>
          </label>
        </div>
        <label>
          Was soll entstehen?
          <textarea value={prompt} onChange={(e) => setPrompt(e.target.value)} rows={3} placeholder="Beschreibe die App, Seite oder Präsentation …" />
        </label>
        <label>
          Zielplattformen
          <div className="chips">
            {[
              ["web", "Responsive Web"],
              ["android", "Android"],
              ["ios", "iOS"],
              ["ipados", "iPadOS"],
              ["macos", "macOS"],
              ["windows", "Windows"],
            ].map(([id, label]) => (
              <button className={platforms.includes(id!) ? "active" : ""} key={id} onClick={() => setPlatforms((p) => (p.includes(id!) ? p.filter((x) => x !== id) : [...p, id!]))}>
                {label}
              </button>
            ))}
          </div>
        </label>
        {platforms.includes("android") && (
          <div className="android-start-device">
            <label>
              Android-Startgerät
              <select value={deviceId} onChange={(event) => setDeviceId(event.target.value)}>
                {androidStartDevices.map((device) => <option key={device.id} value={device.id}>{device.name}</option>)}
              </select>
            </label>
          </div>
        )}
        {create.error && <p className="field-error">{create.error.message}</p>}
      </div>
      <div className="modal-actions">
        <small>Erwartete Dauer: abhängig vom gewählten Provider</small>
        <span />
        <Button onClick={onClose}>Abbrechen</Button>
        <Button variant="primary" disabled={create.isPending || !platforms.length} onClick={() => create.mutate()}>
          Starten · Rückfragen öffnen
        </Button>
      </div>
    </Modal>
  );
}

function Examples() {
  return (
    <HubShell title="Beispielgalerie" actions={<small>Beispiele werden nie automatisch als Projekt angelegt</small>}>
      <main className="hub-content">
        <div className="example-grid">
          {examples.map(([name, platform, desc, tag]) => (
            <article className="example-card" key={name}>
              <div className="preview">
                <span>preview: {name}</span>
              </div>
              <div className="card-body">
                <div>
                  <strong>{name}</strong>
                  <em>{tag}</em>
                </div>
                <span className="platform-chip">{platform}</span>
                <p>{desc}</p>
                <Button variant="ghost">Als Ausgangspunkt verwenden</Button>
              </div>
            </article>
          ))}
        </div>
      </main>
    </HubShell>
  );
}

const dsCards = [
  ["Fluss DS", "Veröffentlicht", "v3 · iOS, Web · 3 Projekte", "#3157D5"],
  ["Atlas DS", "Entwurf", "v1 · Web · 1 Projekt", "#0C7A5B"],
  ["Material-Basis", "Veraltet", "v2 · Android · schreibgeschützt", "#7C3AED"],
];
function DesignSystems() {
  return (
    <HubShell
      title="Designsysteme"
      actions={
        <Button variant="primary">
          <Plus /> Neues Designsystem
        </Button>
      }
    >
      <main className="hub-content">
        <div className="ds-grid">
          {dsCards.map(([name, status, meta, color]) => (
            <article className="ds-card" key={name}>
              <div className="ds-preview">
                <i style={{ background: color }} />
                <i style={{ background: "#12202F" }} />
                <i style={{ borderColor: color }} />
              </div>
              <div className="card-body">
                <div>
                  <strong>{name}</strong>
                  <Status kind={status === "Veröffentlicht" ? "success" : status === "Entwurf" ? "warning" : "neutral"}>{status}</Status>
                </div>
                <small>{meta}</small>
                <Link className="button secondary" to="/app/design-systems/fluss/versions/v3/review">
                  Öffnen & prüfen
                </Link>
              </div>
            </article>
          ))}
        </div>
      </main>
    </HubShell>
  );
}
function DsReview() {
  const nav = useNavigate();
  const [reviews, setReviews] = useState<Record<string, string>>({});
  const groups = ["Typografie", "Farben", "Layout", "Komponenten", "Marke"];
  return (
    <BackPage
      title="Fluss Designsystem · Prüfung"
      onBack={() => nav("/app/design-systems")}
      actions={
        <>
          <span className="subtle">Geprüft: {Object.keys(reviews).length} / 5</span>
          <Button variant="primary" disabled={Object.keys(reviews).length < 5}>
            Veröffentlichen
          </Button>
        </>
      }
    >
      <section className="content-stack">
        <Card title="Prüfkarten" sub="Jede Gruppe bewerten. Kritische Kontrast- und Lizenzprobleme blockieren die Veröffentlichung.">
          {groups.map((name) => (
            <div className="review-row" key={name}>
              <div>
                <strong>{name}</strong>
                <small>Token, Zustände, Plattformen und Barrierefreiheit</small>
              </div>
              <Status kind={reviews[name] === "ok" ? "success" : reviews[name] === "rework" ? "warning" : "neutral"}>{reviews[name] === "ok" ? "Freigegeben" : reviews[name] === "rework" ? "Nacharbeiten" : "Offen"}</Status>
              <Button onClick={() => setReviews({ ...reviews, [name]: "ok" })}>Sieht gut aus</Button>
              <Button onClick={() => setReviews({ ...reviews, [name]: "rework" })}>Nacharbeiten</Button>
            </div>
          ))}
        </Card>
        <Card title="Farben" sub="Kühle Grundfläche, weiße Oberflächen und fokussierter blauer Akzent.">
          <div className="swatches">
            {["#F5F7FA", "#FFFFFF", "#D8DEE8", "#12202F", "#5D6978", "#3157D5", "#2848B4", "#0C7A5B", "#8A5A00", "#9B1C1C", "#2E74B5"].map((c) => (
              <i key={c} style={{ background: c }} title={c} />
            ))}
          </div>
        </Card>
        <Card title="Typografie · Inter Variable · JetBrains Mono">
          <div className="type-sample">
            <b>Display 28 / 700 · Präzision, Ruhe, Kontrolle</b>
            <strong>Titel 20 / 650 · Panelüberschriften und Dialoge</strong>
            <span>Text 14 / 400 · Die Leinwand ist der Mittelpunkt.</span>
            <code>screens/dashboard.screen · geändert vor 2 Min</code>
          </div>
        </Card>
      </section>
    </BackPage>
  );
}
function BackPage({ title, onBack, actions, children }: { title: string; onBack(): void; actions?: ReactNode; children: ReactNode }) {
  const { theme } = useUi();
  return (
    <div className="back-page" data-theme={theme}>
      <header>
        <IconButton label="Zurück" onClick={onBack}>
          <ArrowLeft />
        </IconButton>
        <h1>{title}</h1>
        <span />
        {actions}
      </header>
      <main>{children}</main>
    </div>
  );
}
function Card({ title, sub, children }: { title: string; sub?: string; children: ReactNode }) {
  return (
    <section className="surface-card">
      <h2>{title}</h2>
      {sub && <p className="subtle">{sub}</p>}
      {children}
    </section>
  );
}

function SettingsPage() {
  const nav = useNavigate();
  const { theme, setTheme } = useUi();
  return (
    <BackPage title="Einstellungen" onBack={() => nav("/app/designs")} actions={<IconButton className="settings-theme-toggle" label="Theme wechseln" onClick={() => setTheme(theme === "dark" ? "light" : "dark")}>{theme === "dark" ? <Sun /> : <Moon />}</IconButton>}>
      <div className="settings-layout">
        <nav>
          <NavLink to="/app/settings/models">Modelle & Provider</NavLink>
          <StorageCleanupButton />
        </nav>
        <div className="settings-content">
          <DisplayCalibration />
          <ProviderSettings />
        </div>
      </div>
    </BackPage>
  );
}
function DisplayCalibration() {
  const { monitorPixelsPerMillimeter, setMonitorPixelsPerMillimeter } = useUi();
  const [linePixels, setLinePixels] = useState(Math.round((monitorPixelsPerMillimeter ?? 96 / 25.4) * 100));
  const calibrated = monitorPixelsPerMillimeter !== null && Math.abs(linePixels / 100 - monitorPixelsPerMillimeter) < 0.001;
  return (
    <Card title="Monitor kalibrieren" sub="Nur so kann Werft ein Gerät auf diesem Monitor in seiner echten physischen Größe anzeigen.">
      <div className="display-calibration">
        <p>Halte ein Lineal an die grüne Strecke und passe sie auf genau 100 mm an.</p>
        <div className="calibration-ruler-wrap"><div className="calibration-ruler" style={{ width: linePixels }}><span>100 mm</span></div></div>
        <input aria-label="Länge der 100-mm-Kalibrierstrecke" type="range" min="200" max="800" step="1" value={linePixels} onChange={(event) => setLinePixels(Number(event.target.value))} />
        <div className="modal-actions">
          {monitorPixelsPerMillimeter !== null && <Status kind={calibrated ? "success" : "warning"}>{calibrated ? "Für diesen Monitor kalibriert" : "Änderung noch nicht gespeichert"}</Status>}
          <span />
          {monitorPixelsPerMillimeter !== null && <Button onClick={() => { setMonitorPixelsPerMillimeter(null); setLinePixels(Math.round(96 / 25.4 * 100)); }}>Kalibrierung löschen</Button>}
          <Button variant="primary" onClick={() => setMonitorPixelsPerMillimeter(linePixels / 100)}>Kalibrierung speichern</Button>
        </div>
      </div>
    </Card>
  );
}
function StorageCleanupButton() {
  const cleanup = useMutation({ mutationFn: () => api<{ removedIncompleteUploads: number; removedObjects: number; objectStoreFreedBytes: number; buildCacheFreedBytes: number; freedBytes: number; usedBytes: number }>("/storage/cleanup", { method: "POST", body: "{}" }) });
  const freed = cleanup.data?.freedBytes ?? 0;
  const freedText = freed >= 1024 ** 3
    ? `${(freed / 1024 ** 3).toLocaleString("de-DE", { maximumFractionDigits: 2 })} GB`
    : `${(freed / 1024 ** 2).toLocaleString("de-DE", { maximumFractionDigits: 1 })} MB`;
  const message = cleanup.data
    ? freed > 0
      ? `${freedText} freigegeben`
      : `Cleanup ausgeführt · Server belegt ${(cleanup.data.usedBytes / 1024 ** 3).toLocaleString("de-DE", { maximumFractionDigits: 1 })} GB`
    : undefined;
  return (
    <div className="storage-cleanup">
      <button disabled={cleanup.isPending} onClick={() => window.confirm("Ungenutzten Docker-Build-Cache und verwaiste Werft-Dateien löschen? Alle aktiven Designs und ihre Quelldateien bleiben erhalten.") && cleanup.mutate()}>
        <Trash2 />
        <span>{cleanup.isPending ? "Server wird aufgeräumt …" : "Cache leeren"}</span>
      </button>
      {message && <small>{message}</small>}
      {cleanup.error && <small className="field-error">{cleanup.error instanceof ApiError ? cleanup.error.message : "Cache konnte nicht geleert werden."}</small>}
    </div>
  );
}
function ProviderSettings() {
  const [dialog, setDialog] = useState<"providers" | "openai" | "openrouter" | null>(null);
  const [device, setDevice] = useState<{ authId: string; userCode: string; verificationUri: string; expiresAt: string; interval: number }>();
  const [apiKey, setApiKey] = useState("");
  const [routerModelId, setRouterModelId] = useState("");
  const [inspectedModel, setInspectedModel] = useState<OpenRouterModelDetails>();
  const [selectedEndpointKey, setSelectedEndpointKey] = useState("");
  const [selectedKey, setSelectedKey] = useState("");
  const [effort, setEffort] = useState("");
  const client = useQueryClient();
  const openai = useQuery({ queryKey: ["provider", "openai"], queryFn: () => api<{ connected: boolean; status: string; email?: string; accountId?: string }>("/providers/openai") });
  const openrouter = useQuery({ queryKey: ["provider", "openrouter"], queryFn: () => api<{ connected: boolean; status: string; label?: string }>("/providers/openrouter") });
  const catalog = useQuery({ queryKey: ["provider-models"], queryFn: () => api<ModelCatalog>("/providers/models") });
  const refresh = async () => { await Promise.all([client.invalidateQueries({ queryKey: ["provider"] }), client.invalidateQueries({ queryKey: ["provider-models"] })]); };
  const start = useMutation({ mutationFn: () => api<{ authId: string; userCode: string; verificationUri: string; expiresAt: string; interval: number }>("/providers/openai/auth/start", { method: "POST", body: "{}" }), onSuccess: (data) => { setDevice(data); window.open(data.verificationUri, "_blank", "noopener,noreferrer"); } });
  const poll = useMutation({ mutationFn: (authId: string) => api<{ status: string; connected: boolean; interval?: number }>("/providers/openai/auth/poll", { method: "POST", body: JSON.stringify({ authId }) }), onSuccess: async (data) => { if (data.connected) { setDevice(undefined); setDialog(null); await refresh(); } else if (data.status === "expired") setDevice(undefined); else if (data.interval) setDevice((current) => (current ? { ...current, interval: Math.max(3, Math.min(30, data.interval!)) } : current)); } });
  const connectOpenRouter = useMutation({ mutationFn: () => api("/providers/openrouter", { method: "POST", body: JSON.stringify({ apiKey }) }), onSuccess: async () => { setApiKey(""); setDialog(null); await refresh(); } });
  const disconnectOpenAi = useMutation({ mutationFn: () => api("/providers/openai", { method: "DELETE" }), onSuccess: refresh });
  const disconnectOpenRouter = useMutation({ mutationFn: () => api("/providers/openrouter", { method: "DELETE" }), onSuccess: refresh });
  const testOpenAi = useMutation({ mutationFn: () => api<{ ok: boolean; elapsedMs: number }>("/providers/openai/test", { method: "POST", body: "{}" }) });
  const testOpenRouter = useMutation({ mutationFn: () => api<{ ok: boolean; elapsedMs: number }>("/providers/openrouter/test", { method: "POST", body: "{}" }) });
  const inspectOpenRouterModel = useMutation({
    mutationFn: ({ model }: { model: string; preferred?: OpenRouterEndpoint }) => api<OpenRouterModelDetails>("/providers/openrouter/models/inspect", { method: "POST", body: JSON.stringify({ model }) }),
    onMutate: () => { setInspectedModel(undefined); setSelectedEndpointKey(""); },
    onSuccess: (data, variables) => {
      setInspectedModel(data);
      const previous = variables.preferred;
      const preferred = previous && ((previous.endpointId ? data.endpoints.find((endpoint) => endpoint.endpointId === previous.endpointId) : undefined) ?? (previous.tag ? data.endpoints.find((endpoint) => endpoint.tag === previous.tag) : undefined) ?? data.endpoints.find((endpoint) => endpoint.providerSlug === previous.providerSlug));
      setSelectedEndpointKey(endpointKey(preferred ?? data.endpoints[0]!));
    }
  });
  const saveOpenRouterModel = useMutation({
    mutationFn: ({ model, endpoint }: { model: string; endpoint: OpenRouterEndpoint }) => api("/providers/openrouter/models", { method: "PUT", body: JSON.stringify({ model, endpointId: endpoint.endpointId || undefined, providerTag: endpoint.tag || undefined, providerSlug: endpoint.providerSlug }) }),
    onSuccess: async () => { setRouterModelId(""); setInspectedModel(undefined); setSelectedEndpointKey(""); await refresh(); }
  });
  const removeOpenRouterModel = useMutation({ mutationFn: (model: string) => api("/providers/openrouter/models", { method: "DELETE", body: JSON.stringify({ model }) }), onSuccess: refresh });
  const saveSelection = useMutation({ mutationFn: (selection: ModelSelection) => api("/providers/default-model", { method: "PATCH", body: JSON.stringify(selection) }), onSuccess: refresh });
  useEffect(() => { if (!device || poll.isPending) return; const timer=setTimeout(()=>poll.mutate(device.authId),device.interval*1000); return()=>clearTimeout(timer); },[device,poll.isPending,poll.mutate]);
  useEffect(() => {
    const models = catalog.data?.models ?? [], selection = catalog.data?.selection;
    if (!models.length) return;
    const model = selection ? models.find((entry) => entry.provider === selection.provider && entry.id === selection.model) ?? models[0]! : models[0]!;
    setSelectedKey(modelKey(model));
    setEffort(selection?.effort ?? model.defaultEffort ?? model.efforts[0] ?? "");
  }, [catalog.data]);
  const selectedModel = catalog.data?.models.find((model) => modelKey(model) === selectedKey);
  const chooseModel = (value: string) => {
    setSelectedKey(value);
    const model = catalog.data?.models.find((entry) => modelKey(entry) === value);
    setEffort(model?.defaultEffort ?? model?.efforts[0] ?? "");
  };
  const save = () => {
    if (!selectedModel) return;
    saveSelection.mutate({ provider: selectedModel.provider, model: selectedModel.id, ...(selectedModel.efforts.length && effort ? { effort } : {}) });
  };
  const routerModels = catalog.data?.models.filter((model) => model.provider === "openrouter") ?? [];
  const selectedEndpoint = inspectedModel?.endpoints.find((endpoint) => endpointKey(endpoint) === selectedEndpointKey);
  const mutationError = openai.error || openrouter.error || catalog.error || connectOpenRouter.error || disconnectOpenAi.error || disconnectOpenRouter.error || testOpenAi.error || testOpenRouter.error || inspectOpenRouterModel.error || saveOpenRouterModel.error || removeOpenRouterModel.error || saveSelection.error;
  return (
    <>
      <div className="section-head">
        <div><h2>Modellprovider</h2><p className="subtle">Verbinde Anbieter und wähle anschließend ein Modell für alle KI-Funktionen.</p></div>
        <Button variant="primary" onClick={() => setDialog("providers")}>
          Verbindung hinzufügen
        </Button>
      </div>
      <div className="provider-grid">
        <Card title="OpenAI · Codex OAuth">
          <small>ChatGPT-Anmeldung · Tokens nur verschlüsselt auf dem Server</small>
          <Status kind={openai.data?.connected ? "success" : "neutral"}>{openai.data?.connected ? "Verbunden" : "Nicht verbunden"}</Status>
          {openai.data?.connected && <small>{openai.data.email || openai.data.accountId || "OpenAI-Konto"}</small>}
          <div className="caps"><span>GPT-5.6</span><span>Reasoning</span></div>
          <div className="provider-actions">{openai.data?.connected ? <><Button disabled={testOpenAi.isPending} onClick={() => testOpenAi.mutate()}>{testOpenAi.isPending ? "Test läuft …" : "Testen"}</Button><Button variant="danger" disabled={disconnectOpenAi.isPending} onClick={() => window.confirm("OpenAI-Verbindung wirklich trennen?") && disconnectOpenAi.mutate()}>Trennen</Button></> : <Button onClick={() => setDialog("openai")}>Mit OpenAI verbinden</Button>}</div>
          {testOpenAi.data && <small className="field-success">Live getestet · {testOpenAi.data.elapsedMs} ms</small>}
        </Card>
        <Card title="OpenRouter · API">
          <small>Modelle verschiedener Anbieter über einen OpenRouter-API-Key</small>
          <Status kind={openrouter.data?.connected ? "success" : "neutral"}>{openrouter.data?.connected ? "Verbunden" : "Nicht verbunden"}</Status>
          {openrouter.data?.connected && <small>{openrouter.data.label || "OpenRouter API-Key"}</small>}
          <div className="caps"><span>{catalog.isLoading ? "Modelle werden geladen" : `${routerModels.length} persönliche Modelle`}</span><span>Fester Provider</span></div>
          <div className="provider-actions">{openrouter.data?.connected ? <><Button disabled={testOpenRouter.isPending} onClick={() => testOpenRouter.mutate()}>{testOpenRouter.isPending ? "Key wird geprüft …" : "Key prüfen"}</Button><Button onClick={() => setDialog("openrouter")}>Key ersetzen</Button><Button variant="danger" disabled={disconnectOpenRouter.isPending} onClick={() => window.confirm("OpenRouter-Verbindung wirklich trennen?") && disconnectOpenRouter.mutate()}>Trennen</Button></> : <Button onClick={() => setDialog("openrouter")}>Mit OpenRouter verbinden</Button>}</div>
          {testOpenRouter.data && <small className="field-success">API-Key gültig · {testOpenRouter.data.elapsedMs} ms</small>}
        </Card>
      </div>
      {openrouter.data?.connected && <Card title="Persönliche OpenRouter-Modelle" sub="Füge nur die Modell-ID ein, die du auf OpenRouter kopiert hast. Werft lädt ausschließlich die Provider dieses Modells.">
        <form className="router-model-add" onSubmit={(event) => { event.preventDefault(); const model = routerModelId.trim(); if (model) inspectOpenRouterModel.mutate({ model }); }}>
          <label>OpenRouter-Modell-ID<input value={routerModelId} onChange={(event) => setRouterModelId(event.target.value)} placeholder="anthropic/claude-sonnet-4.5" autoComplete="off" /></label>
          <Button type="submit" variant="primary" disabled={!routerModelId.trim() || inspectOpenRouterModel.isPending}>{inspectOpenRouterModel.isPending ? "Provider werden geladen …" : "Modell prüfen"}</Button>
        </form>
        <small className="router-model-hint">Die ID findest du über den Kopier-Button auf <a href="https://openrouter.ai/models" target="_blank" rel="noreferrer">openrouter.ai/models</a>. Es wird keine vollständige Modellliste geladen.</small>
        {routerModels.length > 0 && <div className="personal-model-list">{routerModels.map((model) => <div className="personal-model" key={model.id}>
          <div><strong>{model.name}</strong><code>{model.id}</code>{model.endpoint && <small>Provider: {model.endpoint.providerName} · {endpointPrice(model.endpoint.promptPerToken)} / {endpointPrice(model.endpoint.completionPerToken)} $ pro 1M Tokens</small>}</div>
          <div><Button disabled={inspectOpenRouterModel.isPending} onClick={() => { setRouterModelId(model.id); inspectOpenRouterModel.mutate({ model: model.id, ...(model.endpoint ? { preferred: model.endpoint } : {}) }); }}>Provider ändern</Button><Button variant="danger" disabled={removeOpenRouterModel.isPending} onClick={() => window.confirm(`${model.name} aus deinen OpenRouter-Modellen entfernen?`) && removeOpenRouterModel.mutate(model.id)}>Entfernen</Button></div>
        </div>)}</div>}
        {!routerModels.length && !catalog.isLoading && <div className="empty router-model-empty">Noch kein persönliches OpenRouter-Modell hinzugefügt.</div>}
        {inspectedModel && <section className="provider-picker">
          <header><div><h3>{inspectedModel.model.name}</h3><code>{inspectedModel.model.id}</code></div><Status kind="info">{inspectedModel.endpoints.length} Provider</Status></header>
          <div className="provider-table-wrap"><table className="provider-table"><thead><tr><th>Provider</th><th>Input $/1M</th><th>Output $/1M</th><th>Cache $/1M</th><th>Kontext</th><th>Throughput</th><th>Status</th></tr></thead><tbody>{inspectedModel.endpoints.map((endpoint) => <tr className={endpointKey(endpoint) === selectedEndpointKey ? "selected" : ""} key={endpointKey(endpoint)}>
            <td><label><input type="radio" name="openrouter-provider" value={endpointKey(endpoint)} checked={endpointKey(endpoint) === selectedEndpointKey} onChange={() => setSelectedEndpointKey(endpointKey(endpoint))} /><span><strong>{endpoint.providerName}</strong><code>{endpoint.tag}</code></span></label></td>
            <td>{endpointPrice(endpoint.promptPerToken)}</td><td>{endpointPrice(endpoint.completionPerToken)}</td><td>{endpoint.cacheReadPerToken > 0 ? endpointPrice(endpoint.cacheReadPerToken) : "--"}</td><td>{endpoint.contextLength.toLocaleString("de-DE")}</td><td>{endpoint.throughputLast30m === undefined ? "--" : `${endpoint.throughputLast30m.toLocaleString("de-DE", { maximumFractionDigits: 1 })} tps`}</td><td><Status kind={endpointStatusKind(endpoint)}>{endpointStatus(endpoint)}</Status></td>
          </tr>)}</tbody></table></div>
          <div className="modal-actions"><Button onClick={() => { setInspectedModel(undefined); setSelectedEndpointKey(""); }}>Abbrechen</Button><span/><Button variant="primary" disabled={!selectedEndpoint || saveOpenRouterModel.isPending} onClick={() => selectedEndpoint && saveOpenRouterModel.mutate({ model: inspectedModel.model.id, endpoint: selectedEndpoint })}>{saveOpenRouterModel.isPending ? "Wird verbunden …" : "Modell mit Provider verbinden"}</Button></div>
        </section>}
      </Card>}
      {catalog.data?.models.length ? <Card title="Standardmodell" sub="Dieses Modell wird für Gespräche, Designänderungen und den Neuaufbau aus Quellen verwendet.">
        <div className="form-grid model-settings"><label>Modell<select value={selectedKey} onChange={(event) => chooseModel(event.target.value)}>{(["openai-codex", "openrouter"] as ProviderId[]).map((provider) => { const models = catalog.data!.models.filter((model) => model.provider === provider); return models.length ? <optgroup label={providerName(provider)} key={provider}>{models.map((model) => <option value={modelKey(model)} key={modelKey(model)}>{modelOptionName(model)}</option>)}</optgroup> : null; })}</select></label>{selectedModel?.efforts.length ? <label>Effort<select value={effort} onChange={(event) => setEffort(event.target.value)}>{selectedModel.efforts.map((value) => <option value={value} key={value}>{value}</option>)}</select></label> : <label>Reasoning<span className="read-only-field">Vom Modell gesteuert</span></label>}</div>
        <div className="modal-actions">{saveSelection.isSuccess && <Status kind="success">Gespeichert</Status>}<span/><Button variant="primary" disabled={!selectedModel || saveSelection.isPending} onClick={save}>{saveSelection.isPending ? "Wird gespeichert …" : "Standardmodell speichern"}</Button></div>
      </Card> : !catalog.isLoading && <Card title="Noch kein Modell verfügbar" sub="Verbinde OpenAI oder OpenRouter, um ein Standardmodell auszuwählen."><span /></Card>}
      {catalog.data?.openrouterError && <p className="field-error">{catalog.data.openrouterError}</p>}
      {catalog.data?.selectionError && <p className="field-error">{catalog.data.selectionError}</p>}
      {mutationError && <p className="field-error">{mutationError instanceof ApiError ? mutationError.message : "Die Provider-Einstellung konnte nicht geändert werden."}</p>}
      {dialog === "providers" && <Modal title="Neue Verbindung hinzufügen" onClose={() => setDialog(null)}><div className="provider-choice"><button onClick={() => setDialog("openai")}><strong>OpenAI · Codex OAuth</strong><span>GPT-5.6 mit deiner ChatGPT-Anmeldung</span></button><button onClick={() => setDialog("openrouter")}><strong>OpenRouter · API</strong><span>Modelle verschiedener Anbieter per API-Key</span></button></div></Modal>}
      {dialog === "openai" && (
        <Modal title="OpenAI mit Codex verbinden" onClose={() => {setDialog(null);setDevice(undefined)}}>
          <div className="modal-body oauth-dialog">
            {!device ? <><p>Werft öffnet die offizielle OpenAI-Geräteseite. Dein Passwort wird niemals an Werft übermittelt.</p><Button variant="primary" disabled={start.isPending} onClick={()=>start.mutate()}>{start.isPending?"Code wird angefordert …":"Browser-Code anfordern"}</Button></> : <><p>Gib diesen Code auf der OpenAI-Seite ein (Klick kopiert ihn):</p><button className="device-code" onClick={()=>navigator.clipboard.writeText(device.userCode)} title="Code kopieren">{device.userCode}</button><a className="button secondary" href={device.verificationUri} target="_blank" rel="noreferrer">OpenAI-Seite öffnen · auth.openai.com/codex/device</a><Status kind="info">Warte auf Bestätigung im Browser …</Status><small>Der Code läuft um {new Date(device.expiresAt).toLocaleTimeString("de-DE",{hour:"2-digit",minute:"2-digit"})} Uhr ab.</small></>}
            {(start.error||poll.error)&&<p className="field-error">{(start.error||poll.error) instanceof ApiError?(start.error||poll.error)?.message:"OpenAI-Anmeldung fehlgeschlagen."}</p>}
          </div>
          <div className="modal-actions">
            <Button onClick={() => {setDialog(null);setDevice(undefined)}}>Schließen</Button>
          </div>
        </Modal>
      )}
      {dialog === "openrouter" && <Modal title="OpenRouter verbinden" onClose={() => { setDialog(null); setApiKey(""); }}><form onSubmit={(event) => { event.preventDefault(); if (apiKey.trim()) connectOpenRouter.mutate(); }}><div className="modal-body"><p>Erstelle einen API-Key unter <a href="https://openrouter.ai/keys" target="_blank" rel="noreferrer">openrouter.ai/keys</a>. Der Key wird verschlüsselt auf dem Server gespeichert und nie an den Browser zurückgegeben.</p><label>API-Key<input autoFocus type="password" autoComplete="off" value={apiKey} onChange={(event) => setApiKey(event.target.value)} placeholder="sk-or-v1-…" /></label>{connectOpenRouter.error && <p className="field-error">{connectOpenRouter.error instanceof ApiError ? connectOpenRouter.error.message : "OpenRouter konnte nicht verbunden werden."}</p>}</div><div className="modal-actions"><Button type="button" onClick={() => { setDialog(null); setApiKey(""); }}>Abbrechen</Button><Button type="submit" variant="primary" disabled={!apiKey.trim() || connectOpenRouter.isPending}>{connectOpenRouter.isPending ? "Wird geprüft …" : "Verbinden"}</Button></div></form></Modal>}
    </>
  );
}
type ImportQuestion = { id: string; kind: "entry" | "reconstruct"; question: string; why: string; options: Array<{ value: string; label: string; hint?: string }> };
// Die Rückfragen kommen aus dem importierten Projekt selbst und werden nur gezeigt, solange für
// dessen Darstellung wirklich etwas fehlt. Jede Antwort wirkt sofort auf die Vorschau.
function ImportQuestions({ projectId }: { projectId: string }) {
  const client = useQueryClient();
  const [dismissed, setDismissed] = useState<string[]>([]);
  const questions = useQuery({ queryKey: ["import-questions", projectId], queryFn: () => api<{ questions: ImportQuestion[] }>(`/projects/${projectId}/import/questions`) });
  const refresh = async () => {
    await client.invalidateQueries({ queryKey: ["project-import", projectId] });
    await client.invalidateQueries({ queryKey: ["import-questions", projectId] });
  };
  const answer = useMutation({
    mutationFn: async ({ question, value }: { question: ImportQuestion; value: string }) => {
      if (question.kind === "entry") return api(`/projects/${projectId}/import/entry`, { method: "PATCH", body: JSON.stringify({ path: value }) });
      if (value === "reconstruct") return reconstructProject(projectId, () => {}, false, true);
      setDismissed((all) => [...all, question.id]);
      return null;
    },
    onSuccess: refresh
  });
  const open = (questions.data?.questions ?? []).filter((item) => !dismissed.includes(item.id));
  if (!open.length) return null;
  return (
    <section className="import-questions">
      {open.map((question) => (
        <article key={question.id}>
          <strong>{question.question}</strong>
          <small>{question.why}</small>
          <div className="chips">
            {question.options.map((option) => (
              <button type="button" key={option.value} disabled={answer.isPending} title={option.hint ?? option.label} onClick={() => answer.mutate({ question, value: option.value })}>
                {option.label}
                {option.hint && <em>{option.hint}</em>}
              </button>
            ))}
          </div>
        </article>
      ))}
      {answer.isPending && <small className="subtle">Wird angewendet … das Aufbauen aus Quellen dauert einige Minuten.</small>}
      {answer.error && <p className="field-error">{answer.error instanceof ApiError ? answer.error.message : "Die Antwort konnte nicht angewendet werden."}</p>}
    </section>
  );
}

// Fortlaufende Kennung fuer Auftraege an die Vorschau. NICHT die Uhrzeit: beim Ziehen im
// Farbwaehler entstehen mehrere Aenderungen in derselben Millisekunde, und alle bis auf die erste
// gingen verloren — die Farbe blieb dann sichtbar unveraendert.
let auftragsZaehler = 0;
const naechsterAuftrag = () => (auftragsZaehler += 1);

function Studio() {
  const { projectId = "" } = useParams();
  const nav = useNavigate();
  const location = useLocation();
  const client = useQueryClient();
  const tab = location.pathname.split("/").at(-1) ?? "canvas";
  const { theme, leftOpen, rightOpen, toggleLeft, toggleRight, setLeftOpen, setRightOpen, mode, setMode, zoom, setZoom, setTheme, setModal, modal } = useUi();
  const project = useQuery({
    queryKey: ["project", projectId],
    queryFn: () => api<Project>(`/projects/${projectId}`),
  });
  const design = useQuery({
    queryKey: ["design", projectId],
    queryFn: () => api<{ revision: number; document: DesignDocument }>(`/projects/${projectId}/design-document`),
  });
  const draftFrame = design.data?.document.frames[0];
  const imported = useQuery({
    queryKey: ["project-import", projectId],
    queryFn: () => api<ProjectImport>(`/projects/${projectId}/import`),
  });
  const [accent, setAccent] = useState("#3157D5");
  const [radius, setRadius] = useState(18);
  const [darkPreview, setDarkPreview] = useState(false);
  const [canvasFullscreen, setCanvasFullscreen] = useState(false);
  // Das Referenzgeraet gilt fuer die Buehne: leer heisst „so gross wie das Design selbst".
  const [deviceId, setDeviceId] = useState("");
  const [landscape, setLandscape] = useState(false);
  const initializedDraftDevice = useRef<string | null>(null);
  useEffect(() => {
    if (imported.data?.imported !== false || draftFrame?.platform !== "android" || initializedDraftDevice.current === projectId) return;
    initializedDraftDevice.current = projectId;
    const device = referenceDevices.find((entry) => (entry.width === draftFrame.width && entry.height === draftFrame.height) || (entry.width === draftFrame.height && entry.height === draftFrame.width));
    if (!device) return;
    setDeviceId(device.id);
    setLandscape(device.width === draftFrame.height && device.height === draftFrame.width);
  }, [draftFrame, imported.data?.imported, projectId]);
  const referenceDevice = stageDeviceFor(deviceId, landscape);
  const designDevice = imported.data?.imported
    ? stageDeviceForDesign(imported.data.previewWidth, imported.data.previewHeight, landscape)
    : draftFrame ? stageDeviceForDesign(draftFrame.width, draftFrame.height, landscape) : null;
  const stageDevice = referenceDevice ?? designDevice;
  // Der Neuaufbau wird OBEN bedient, laeuft aber unten auf der Buehne. Deshalb liegt er hier und
  // wird nach unten gereicht — sonst gaebe es zwei Auftraege, die nichts voneinander wissen.
  const [rebuildProgress, setRebuildProgress] = useState<ReconstructionJob | null>(null);
  const rebuild = useMutation({
    mutationFn: ({ retryFailed, force, viewport }: { retryFailed: boolean; force?: boolean; viewport?: RebuildViewport }) => reconstructProject(projectId, setRebuildProgress, retryFailed, force ?? false, viewport),
    onSuccess: () => client.invalidateQueries({ queryKey: ["project-import", projectId] })
  });
  // Welcher Bildschirm gerade zu sehen ist, entscheidet meist, was ein Änderungswunsch meint.
  const [visibleScreen, setVisibleScreen] = useState<string | null>(null);
  const [chat, setChat] = useState("");
  // Die Höhe des Eingabefelds zieht der Griff darüber: nach oben größer, nach unten kleiner.
  const [composerHeight, setComposerHeight] = useState(92);
  const composerDrag = useRef<{ pointerId: number; y: number; height: number } | null>(null);
  const startComposerResize = (event: ReactPointerEvent<HTMLDivElement>) => {
    event.preventDefault();
    event.currentTarget.setPointerCapture(event.pointerId);
    composerDrag.current = { pointerId: event.pointerId, y: event.clientY, height: composerHeight };
  };
  const moveComposerResize = (event: ReactPointerEvent<HTMLDivElement>) => {
    const start = composerDrag.current;
    if (!start || start.pointerId !== event.pointerId) return;
    setComposerHeight(Math.min(460, Math.max(56, start.height + (start.y - event.clientY))));
  };
  const endComposerResize = (event: ReactPointerEvent<HTMLDivElement>) => {
    if (composerDrag.current?.pointerId !== event.pointerId) return;
    composerDrag.current = null;
    if (event.currentTarget.hasPointerCapture(event.pointerId)) event.currentTarget.releasePointerCapture(event.pointerId);
  };
  const [panelTab, setPanelTab] = useState<"board" | "chat" | "comments">("board");
  const commentStorageKey = `werft-comments-${projectId}`;
  const [comments, setComments] = useState<DesignComment[]>(() => {
    try { return JSON.parse(localStorage.getItem(`werft-comments-${projectId}`) ?? "[]") as DesignComment[]; }
    catch { return []; }
  });
  useEffect(() => { try { localStorage.setItem(commentStorageKey, JSON.stringify(comments.slice(-200))); } catch { /* Speicher voll: Kommentare bleiben in der Sitzung */ } }, [comments, commentStorageKey]);
  // Das Design Board links kennt alle Bildschirme des importierten Projekts und hebt den gerade
  // sichtbaren hervor; ein Klick schickt die Bühne rechts auf den gewählten Bildschirm.
  const [boardScreens, setBoardScreens] = useState<PreviewScreen[]>([]);
  const [activeScreenId, setActiveScreenId] = useState<string | null>(null);
  const [screenRequest, setScreenRequest] = useState<{ screenId: string; nonce: number } | null>(null);
  // Die Erscheinungen des Designs: gemeldet aus der Vorschau, gewählt im Design Board. Die Wahl gilt
  // für alle Bildschirme zugleich — ein halb umgeschaltetes Design wäre wertlos.
  const [boardThemes, setBoardThemes] = useState<DesignTheme[]>([]);
  const [activeThemeId, setActiveThemeId] = useState("");
  // Ob das Umschalten im Design wirklich ankommt: ein mit festen Farbwerten aufgebautes Design
  // reagiert nicht — dann wird das gesagt, statt einen wirkungslosen Schalter anzubieten.
  const [themesEffective, setThemesEffective] = useState(true);
  const [themeRequest, setThemeRequest] = useState<{ themeId: string; nonce: number } | null>(null);
  // Die Farbregler arbeiten mit den Token, die der Aufbau aus den Projektquellen erzeugt hat.
  const [designTokens, setDesignTokens] = useState<Record<string, string>>({});
  // Anpassungen werden JE ERSCHEINUNG gefuehrt: Hell und Dunkel haben eigene Farben, ein gemeinsamer
  // Satz wuerde die eine Erscheinung mit den Werten der anderen ueberschreiben.
  const [tokenOverrides, setTokenOverrides] = useState<Record<string, Record<string, string>>>({});
  const [tuneRequest, setTuneRequest] = useState<{ overrides: Record<string, string>; nonce: number } | null>(null);
  const activeOverrides = tokenOverrides[activeThemeId] ?? {};
  const applyOverrides = (next: Record<string, Record<string, string>>, themeId: string) => {
    setTokenOverrides(next);
    setTuneRequest({ overrides: next[themeId] ?? {}, nonce: naechsterAuftrag() });
  };
  const tuneToken = (name: string, value: string) => applyOverrides({ ...tokenOverrides, [activeThemeId]: { ...activeOverrides, [name]: value } }, activeThemeId);
  const resetToken = (name: string) => {
    const remaining = { ...activeOverrides };
    delete remaining[name];
    applyOverrides({ ...tokenOverrides, [activeThemeId]: remaining }, activeThemeId);
  };
  // Beim Wechsel der Erscheinung gehen deren eigene Anpassungen mit; sonst bliebe der Satz der
  // vorherigen Erscheinung im Design stehen.
  const chooseTheme = (themeId: string) => {
    setActiveThemeId(themeId);
    setThemeRequest({ themeId, nonce: naechsterAuftrag() });
    setTuneRequest({ overrides: tokenOverrides[themeId] ?? {}, nonce: naechsterAuftrag() });
  };
  // Ohne importiertes Design gibt es kein Board — dann bleibt das Gespräch der Einstieg, statt einen
  // leeren Reiter vorzublenden.
  const boardAvailable = imported.data?.imported === true;
  const activeTab = panelTab === "board" && !boardAvailable ? "chat" : panelTab;
  const chatStorageKey = `werft-chat-${projectId}`;
  const [messages, setMessages] = useState<Array<{ role: "user" | "assistant"; text: string }>>(() => {
    try { return JSON.parse(localStorage.getItem(`werft-chat-${projectId}`) ?? "[]") as Array<{ role: "user" | "assistant"; text: string }>; }
    catch { return []; }
  });
  useEffect(() => { try { localStorage.setItem(chatStorageKey, JSON.stringify(messages.slice(-200))); } catch { /* Speicher voll: Verlauf bleibt dann nur in der Sitzung */ } }, [messages, chatStorageKey]);
  // Deterministisch aus den Projektquellen gemessene Erscheinungen — ohne KI-Lauf. Sie sind der
  // Rettungsanker für Designs, die vor der Theme-Unterstützung aufgebaut wurden.
  const measuredThemes = useQuery({
    queryKey: ["themes", projectId],
    queryFn: () => api<{ themes: DesignTheme[]; css: string; overrideCss: string }>(`/projects/${projectId}/themes`),
    enabled: boardAvailable,
    staleTime: 5 * 60 * 1000
  });
  // Nachgereicht wird, sobald das Dokument weniger Erscheinungen kennt als gemessen wurden ODER das
  // Umschalten dort wirkungslos bliebe, weil die Farben fest im Stylesheet stehen.
  const themeCss = measuredThemes.data && ((measuredThemes.data.themes.length > boardThemes.length) || !themesEffective)
    ? [measuredThemes.data.css, measuredThemes.data.overrideCss].filter(Boolean).join("\n")
    : undefined;
  const modelCatalog = useQuery({ queryKey: ["provider-models"], queryFn: () => api<ModelCatalog>("/providers/models") });
  const [runModelKey, setRunModelKey] = useState("");
  const [runEffort, setRunEffort] = useState("");
  useEffect(() => {
    const models = modelCatalog.data?.models ?? [];
    if (!models.length || models.some((model) => modelKey(model) === runModelKey)) return;
    const preferred = modelCatalog.data?.selection;
    const model = preferred ? models.find((entry) => entry.provider === preferred.provider && entry.id === preferred.model) ?? models[0]! : models[0]!;
    setRunModelKey(modelKey(model));
    setRunEffort(preferred?.effort ?? model.defaultEffort ?? model.efforts[0] ?? "");
  }, [modelCatalog.data, runModelKey]);
  const runModel = modelCatalog.data?.models.find((model) => modelKey(model) === runModelKey);
  const chooseRunModel = (value: string) => {
    setRunModelKey(value);
    const model = modelCatalog.data?.models.find((entry) => modelKey(entry) === value);
    setRunEffort(model?.defaultEffort ?? model?.efforts[0] ?? "");
  };
  // Textänderungen laufen deterministisch ohne KI: der Endpunkt ersetzt nur, wenn der alte
  // Wortlaut genau einmal vorkommt — sonst meldet er die Mehrdeutigkeit statt zu raten.
  const textEdit = useMutation({
    mutationFn: ({ before, after }: { before: string; after: string }) => api<{ applied: boolean; path?: string }>(`/projects/${projectId}/import/text`, { method: "POST", body: JSON.stringify({ before, after }) }),
    onSuccess: async (data) => {
      if (data.applied) setMessages((all) => [...all, { role: "assistant", text: `Text geändert in ${data.path}.` }]);
      await client.invalidateQueries({ queryKey: ["project-import", projectId] });
      await client.invalidateQueries({ queryKey: ["import-file", projectId] });
    },
    onError: (error) => setMessages((all) => [...all, { role: "assistant", text: error instanceof ApiError ? `Text nicht geändert: ${error.message}` : "Die Textänderung ist fehlgeschlagen." }])
  });
  // Was gerade passiert — live aus dem Ereignisstrom des Servers. Ohne diese Meldungen sieht man
  // waehrend eines ein- bis zweiminuetigen Laufs nichts und weiss nicht, ob ueberhaupt gearbeitet wird.
  const [chatStep, setChatStep] = useState<string>("");
  const chatAbort = useRef<AbortController | null>(null);
  const chatRun = useMutation({
    // Der Verlauf geht MIT: erst dadurch weiss das Modell, was vorher besprochen und geaendert wurde,
    // statt jede Nachricht als ersten Satz eines fremden Gespraechs zu behandeln.
    mutationFn: ({ message, target }: { message: string; target?: MarkTarget }) => {
      chatAbort.current?.abort();
      const controller = new AbortController();
      chatAbort.current = controller;
      setChatStep("Auftrag wird vorbereitet …");
      return apiEventStream<{ reply: string; changedFiles: string[]; skipped?: string[]; revision: number; rounds?: number; editsApplied?: number; durationMs?: number }>(`/projects/${projectId}/chat`, {
        message,
        history: messages.slice(-12).map((entry) => ({ role: entry.role, text: entry.text.slice(0, 6000) })),
        ...(runModel ? { provider: runModel.provider, model: runModel.id } : {}),
        ...(runModel?.efforts.length && runEffort ? { effort: runEffort } : {}),
        ...(target?.screenName || visibleScreen ? { screen: target?.screenName || visibleScreen } : {}),
        ...(target ? { target: { selector: target.selector, html: target.html, label: target.label, screenName: target.screenName } } : {})
      }, (event, data) => setChatStep(chatStepText(event, data)), controller.signal);
    },
    onSettled: () => { chatAbort.current = null; setChatStep(""); },
    onSuccess: async (data) => {
      const parts = [data.reply];
      if (data.changedFiles.length) {
        const detail = [
          data.editsApplied ? `${data.editsApplied} Änderung${data.editsApplied === 1 ? "" : "en"}` : "",
          (data.rounds ?? 1) > 1 ? `${data.rounds} Runden` : "",
          data.durationMs ? `${Math.round(data.durationMs / 1000)} s` : ""
        ].filter(Boolean).join(" · ");
        parts.push(`✓ Geändert: ${[...new Set(data.changedFiles.map(designFileLabel))].join(", ")}${detail ? ` (${detail})` : ""}`);
      }
      if (data.skipped?.length) parts.push(`⚠ Offen geblieben:\n${data.skipped.join("\n")}`);
      setMessages((all) => [...all, { role: "assistant", text: parts.join("\n\n") }]);
      // Ein laufender Kommentar bekommt sein Ergebnis: „angewendet" nur, wenn wirklich Dateien
      // geschrieben wurden — sonst stuende dort ein Erfolg, den es nicht gab.
      setComments((all) => all.map((comment) => comment.status !== "läuft" ? comment : {
        ...comment,
        status: data.changedFiles.length ? "angewendet" : "ohne Änderung",
        detail: data.changedFiles.length ? [...new Set(data.changedFiles.map(designFileLabel))].join(", ") : data.reply.slice(0, 200)
      }));
      await client.invalidateQueries({ queryKey: ["project-import", projectId] });
      await client.invalidateQueries({ queryKey: ["import-file", projectId] });
      // Die gemessenen Erscheinungen stammen aus dem Dokument, das die KI gerade geaendert hat.
      // Ohne diese Neuvermessung legte die Vorschau den ALTEN Farbstand wieder darueber.
      await client.invalidateQueries({ queryKey: ["themes", projectId] });
    },
    onError: async (error) => {
      // Ein Abbruch ist kein Fehler: bereits übernommene Änderungen sind gespeichert, und die Ansicht
      // muss sie zeigen — sonst stünde im Studio ein Stand, den es auf dem Server nicht mehr gibt.
      const abgebrochen = error instanceof DOMException && error.name === "AbortError";
      setMessages((all) => [...all, { role: "assistant", text: abgebrochen ? "Abgebrochen. Was bis dahin übernommen wurde, ist gespeichert." : error instanceof ApiError ? `Fehler: ${error.message}` : "Der KI-Lauf ist fehlgeschlagen. Bitte erneut versuchen." }]);
      setComments((all) => all.map((comment) => comment.status !== "läuft" ? comment : { ...comment, status: abgebrochen ? "ohne Änderung" : "fehlgeschlagen", detail: abgebrochen ? "Abgebrochen." : error instanceof ApiError ? error.message : "Der KI-Lauf ist fehlgeschlagen." }));
      await client.invalidateQueries({ queryKey: ["project-import", projectId] });
      await client.invalidateQueries({ queryKey: ["import-file", projectId] });
      await client.invalidateQueries({ queryKey: ["themes", projectId] });
    }
  });
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if ((e.target as HTMLElement).matches("input,textarea,select,[contenteditable]")) return;
      const map: Record<string, ToolMode> = {
        v: "interact",
        s: "select",
        c: "comment",
        e: "edit",
        d: "draw",
        f: "pick",
      };
      if (map[e.key.toLowerCase()]) setMode(map[e.key.toLowerCase()]!);
      if (e.key === "0") setZoom(1);
      if (e.key === "1") toggleLeft();
      if (e.key === "2") toggleRight();
      if (e.key === "?") setModal("keys");
      if (e.key === "Escape") setModal(null);
    };
    addEventListener("keydown", handler);
    return () => removeEventListener("keydown", handler);
  }, [setMode, setZoom, toggleLeft, toggleRight, setModal]);
  // Im Vollbild sind beide Leisten zunaechst zu — es soll nur das Design zu sehen sein. Beim
  // Verlassen kommt GENAU der Stand zurueck, der vorher galt; sonst waeren die Leisten nach einem
  // einzigen Vollbild-Ausflug dauerhaft verschwunden.
  const panelsBeforeFullscreen = useRef<{ left: boolean; right: boolean } | null>(null);
  const restorePanels = () => {
    const before = panelsBeforeFullscreen.current;
    panelsBeforeFullscreen.current = null;
    if (!before) return;
    setLeftOpen(before.left);
    setRightOpen(before.right);
  };
  useEffect(() => {
    const leaveFullscreen = () => { setCanvasFullscreen(false); restorePanels(); };
    const handleFullscreenChange = () => { if (!document.fullscreenElement) leaveFullscreen(); };
    const handleFullscreenEscape = (event: KeyboardEvent) => { if (event.key === "Escape") leaveFullscreen(); };
    document.addEventListener("fullscreenchange", handleFullscreenChange);
    document.addEventListener("keydown", handleFullscreenEscape);
    return () => {
      document.removeEventListener("fullscreenchange", handleFullscreenChange);
      document.removeEventListener("keydown", handleFullscreenEscape);
    };
  }, []);
  const enterCanvasFullscreen = () => {
    nav(`/app/projects/${projectId}/studio/canvas`);
    panelsBeforeFullscreen.current = { left: leftOpen, right: rightOpen };
    setLeftOpen(false);
    setRightOpen(false);
    setCanvasFullscreen(true);
    if (document.documentElement.requestFullscreen) void document.documentElement.requestFullscreen().catch((error) => console.warn("Browser-Vollbild konnte nicht gestartet werden", error));
  };
  const exitCanvasFullscreen = () => {
    setCanvasFullscreen(false);
    restorePanels();
    if (document.fullscreenElement) void document.exitFullscreen().catch((error) => console.warn("Browser-Vollbild konnte nicht beendet werden", error));
  };
  const canRebuild = imported.data?.imported === true;
  const showDevicePicker = canRebuild || draftFrame?.platform === "android";
  // Gibt es fuer das gewaehlte Geraet bereits eine eigene Fassung? Dann zeigt die Buehne sie; sonst
  // steht dort die Grundfassung, die die Flaeche des Geraets nicht ausnutzt.
  const deviceVariant = referenceDevice && imported.data?.imported
    ? imported.data.variants.find((variant) => variant.width === referenceDevice.width && variant.height === referenceDevice.height)
    : undefined;
  const rebuildLabel = referenceDevice
    ? deviceVariant ? `Fassung für ${referenceDevice.label} neu aufbauen` : `Design für ${referenceDevice.label} aufbauen`
    : imported.data?.imported && imported.data.reconstructed ? "Design neu aus den Quellen aufbauen" : "Design aus den Quellen aufbauen";
  // Mit gewaehltem Geraet wird FUER DESSEN Flaeche gebaut — genau das macht aus dem Rahmen eine
  // echte Geraeteansicht statt eines zu gross gezogenen Handybildschirms.
  const startRebuild = () => rebuild.mutate({
    retryFailed: true,
    force: imported.data?.imported === true && imported.data.reconstructed,
    ...(referenceDevice ? { viewport: { width: referenceDevice.width, height: referenceDevice.height, device: referenceDevice.label } } : {})
  });
  if (project.isError) return <Navigate to="/app/designs" />;
  return (
    <div className={`studio${canvasFullscreen ? " canvas-fullscreen" : ""}`} data-theme={theme}>
      {canvasFullscreen && (
        <div className="canvas-fullscreen-controls">
          <IconButton label="Bearbeitungsleiste ein- oder ausblenden" className={leftOpen ? "on" : ""} onClick={toggleLeft}><PanelLeft /></IconButton>
          {canRebuild && <IconButton label={rebuildLabel} disabled={rebuild.isPending} onClick={startRebuild}><RotateCcw /></IconButton>}
          <IconButton label="Vollbild beenden" onClick={exitCanvasFullscreen}><Minimize2 /></IconButton>
        </div>
      )}
      <header className="project-header">
        <IconButton label="Zum Hub" onClick={() => nav("/app/designs")}>
          <ArrowLeft />
        </IconButton>
        <strong>{project.data?.name ?? "Projekt wird geladen …"}</strong>
        <span />
        {/* Referenzgeraet: das Design wird auf der Flaeche gezeigt, die das gewaehlte Geraet einer
            App wirklich laesst — zugeklappt und aufgeklappt sind eigene Eintraege, hoch und quer
            eine eigene Achse daneben. */}
        {showDevicePicker && (
          <div className="device-picker">
            <select value={deviceId} title="Referenzgerät für die Bühne" aria-label="Referenzgerät" onChange={(event) => setDeviceId(event.target.value)}>
              <option value="">Größe des Designs</option>
              {referenceDevices.map((device) => (
                <option key={device.id} value={device.id}>{deviceLabel(device)}</option>
              ))}
            </select>
            <div className="segments orientation" role="group" aria-label="Ausrichtung">
              <button type="button" className={landscape ? "" : "active"} title="Hochformat" onClick={() => setLandscape(false)}>Hoch</button>
              <button type="button" className={landscape ? "active" : ""} title="Querformat — Ansicht um 90 Grad gedreht" onClick={() => setLandscape(true)}>Quer</button>
            </div>
            {stageDevice && <code title={`${stageDevice.width} × ${stageDevice.height} CSS-Pixel · ${referenceDevices.find((device) => device.id === deviceId)?.source ?? "Designmaß"}`}>{aspectRatioLabel(stageDevice.width, stageDevice.height)}</code>}
          </div>
        )}
        <IconButton label="Bearbeitungsleiste ein- oder ausblenden" className={leftOpen ? "on" : ""} onClick={toggleLeft}>
          <PanelLeft />
        </IconButton>
        {!canRebuild && (
          <IconButton label="Feineinstellungen ein- oder ausblenden" className={rightOpen ? "on" : ""} onClick={toggleRight}>
            <PanelRight />
          </IconButton>
        )}
        {canRebuild && (
          <IconButton label={rebuildLabel} disabled={rebuild.isPending} onClick={startRebuild}>
            <RotateCcw />
          </IconButton>
        )}
        <IconButton label="Design im Vollbild anzeigen" onClick={enterCanvasFullscreen}>
          <Maximize2 />
        </IconButton>
        <IconButton label="Theme" onClick={() => setTheme(theme === "dark" ? "light" : "dark")}>
          {theme === "dark" ? <Sun /> : <Moon />}
        </IconButton>
        <IconButton label="Projekt als ZIP herunterladen" onClick={() => { if (imported.data?.imported) window.open(`/api/v1/projects/${projectId}/export.zip`, "_blank"); else setModal("export"); }}>
          <Download />
        </IconButton>
      </header>
      <div className="studio-body">
        {leftOpen && (
          <aside className="left-panel">
            <div className="panel-tabs">
              {boardAvailable && <button className={activeTab === "board" ? "active" : ""} onClick={() => setPanelTab("board")}>Screens</button>}
              <button className={activeTab === "chat" ? "active" : ""} onClick={() => setPanelTab("chat")}>Gespräch</button>
              {boardAvailable && <button className={activeTab === "comments" ? "active" : ""} onClick={() => setPanelTab("comments")}>Kommentare{comments.length ? ` (${comments.length})` : ""}</button>}
            </div>
            {activeTab === "comments" && (
              <div className="comment-list">
                {comments.length === 0 ? (
                  <div className="empty">Noch keine Kommentare. Wähle oben rechts das Kommentarwerkzeug, markiere einen Bereich und beschreibe die Änderung.</div>
                ) : [...comments].reverse().map((comment) => (
                  <article className={`comment-entry ${comment.status === "angewendet" ? "done" : comment.status === "fehlgeschlagen" ? "failed" : ""}`} key={comment.id}>
                    <header>
                      <strong title={comment.selector}>{comment.label}</strong>
                      {/* Ein laufender Auftrag zeigt das auch hier — sonst wirkt die Liste still. */}
                      <em>{comment.status === "läuft" ? <><span className="working-dots" aria-hidden="true"><i /><i /><i /></span> läuft</> : comment.status}</em>
                    </header>
                    <p>{comment.text}</p>
                    {comment.detail && <small>{comment.detail}</small>}
                    <footer>
                      {comment.screenId && <button type="button" onClick={() => setScreenRequest({ screenId: comment.screenId, nonce: naechsterAuftrag() })}>{comment.screenName || "Bildschirm"} zeigen</button>}
                      <span>{new Date(comment.at).toLocaleString("de-DE")}</span>
                    </footer>
                  </article>
                ))}
              </div>
            )}
            {activeTab === "board" && (
              <div className="design-board">
                {/* Die Erscheinung steht ganz oben: sie gilt für ALLE Bildschirme, nicht für den
                    gerade sichtbaren. Ohne diese Zeile wäre nur das erste Theme erreichbar. */}
                {boardThemes.length > 1 && (
                  <div className="board-themes">
                    <p className="board-count">Erscheinung</p>
                    <div className="board-theme-row">
                      {boardThemes.map((theme) => (
                        <button
                          type="button"
                          key={theme.id}
                          className={`board-theme${theme.id === activeThemeId ? " active" : ""}`}
                          title={theme.name}
                          onClick={() => chooseTheme(theme.id)}
                        >
                          {/* Die Farbe liegt als Bild ÜBER dem weißen Grund: halbdurchsichtige
                              Designfarben bleiben so erkennbar statt mit dem Panel zu verschmelzen. */}
                          <i style={theme.color ? { backgroundImage: `linear-gradient(${theme.color}, ${theme.color})` } : undefined} />
                          {themeLabel(theme, boardThemes)}
                        </button>
                      ))}
                    </div>
                    {!themesEffective && <small className="board-theme-hint">Dieses Design wurde mit festen Farbwerten aufgebaut — das Umschalten wirkt erst nach „Neu aufbauen“ unten rechts an der Bühne.</small>}
                  </div>
                )}
                {boardScreens.length === 0 ? (
                  <div className="empty">Die Bildschirme werden gelesen, sobald die Vorschau steht.</div>
                ) : (
                  <>
                    <p className="board-count">{boardScreens.length} Bildschirm{boardScreens.length === 1 ? "" : "e"}</p>
                    {boardScreens.map((screen) => (
                      <button
                        type="button"
                        className={`board-screen${screen.id === activeScreenId ? " active" : ""}`}
                        key={screen.id}
                        onClick={() => setScreenRequest({ screenId: screen.id, nonce: naechsterAuftrag() })}
                      >
                        <span className="board-screen-name">{screen.name}</span>
                        {screen.isStart && <em title="Startbildschirm">Start</em>}
                        {screen.links.length > 0 && <small>{screen.links.length} Verknüpfung{screen.links.length === 1 ? "" : "en"}</small>}
                      </button>
                    ))}
                  </>
                )}
              </div>
            )}
            {activeTab === "chat" && <div className="messages">
              {messages.length === 0 && !chatRun.isPending && <div className="empty">Noch keine Nachrichten. Beschreibe unten eine Änderung am Design.</div>}
              {messages.map((m, i) => (
                <div className={m.role === "user" ? "user-message" : "assistant-message"} key={i}>
                  {m.text}
                </div>
              ))}
              {/* Der Umfang wird serverseitig aus dem Wunsch bestimmt — hier stand vorher „Bildschirm X",
                  auch wenn der Wunsch das ganze Design meinte. Angezeigt wird deshalb, WOMIT gearbeitet
                  wird und was gerade sichtbar ist, statt eine Behauptung über den Umfang. */}
              {chatRun.isPending && <WorkingIndicator
                detail={`${runModel?.name ?? "Standardmodell"}${visibleScreen ? ` · sichtbar: ${visibleScreen}` : ""}`}
                step={chatStep}
                onCancel={() => chatAbort.current?.abort()}
              />}
            </div>}
            {activeTab === "chat" && <form
              className="composer"
              onSubmit={(e) => {
                e.preventDefault();
                const message = chat.trim();
                if (!message || chatRun.isPending) return;
                setMessages((all) => [...all, { role: "user", text: message }]);
                setChat("");
                chatRun.mutate({ message });
              }}
            >
              {/* Der Griff über dem Feld zieht die Eingabe auf: nach oben größer, nach unten kleiner. */}
              <div
                className="composer-grip"
                title="Höhe des Eingabefelds ziehen"
                onPointerDown={startComposerResize}
                onPointerMove={moveComposerResize}
                onPointerUp={endComposerResize}
                onPointerCancel={endComposerResize}
              />
              <div className="composer-input">
                <textarea style={{ height: composerHeight }} value={chat} onChange={(e) => setChat(e.target.value)} placeholder="Beschreibe eine Änderung …" />
              </div>
              <footer>
                {runModel ? (
                  // Modell und Effort stehen dort, wo sie benutzt werden — nicht nur in den
                  // Einstellungen. Jede Wahl schickt die andere unverändert mit.
                  <>
                    <select
                      aria-label="Modell für den nächsten Lauf"
                      value={runModelKey}
                      disabled={chatRun.isPending}
                      onChange={(event) => chooseRunModel(event.target.value)}
                    >
                      {(["openai-codex", "openrouter"] as ProviderId[]).map((provider) => { const models = modelCatalog.data!.models.filter((model) => model.provider === provider); return models.length ? <optgroup label={providerName(provider)} key={provider}>{models.map((model) => <option value={modelKey(model)} key={modelKey(model)}>{modelOptionName(model)}</option>)}</optgroup> : null; })}
                    </select>
                    {runModel.efforts.length > 0 && <select
                      aria-label="Effort für den nächsten Lauf"
                      value={runEffort}
                      disabled={chatRun.isPending}
                      onChange={(event) => setRunEffort(event.target.value)}
                    >
                      {runModel.efforts.map((value) => <option value={value} key={value}>{value}</option>)}
                    </select>}
                  </>
                ) : (
                  <Link to="/app/settings/models">Kein Provider verbunden — jetzt verbinden</Link>
                )}
                <Button variant="primary" className="composer-send" aria-label="Senden" title="Senden" disabled={chatRun.isPending || !chat.trim()}><Send /></Button>
              </footer>
            </form>}
          </aside>
        )}
        <section className="workspace">
          {imported.data?.imported && <ImportQuestions projectId={projectId} />}
          <Canvas
            key={projectId}
            projectId={projectId}
            accent={accent}
            radius={radius}
            dark={darkPreview}
            zoom={zoom}
            setZoom={setZoom}
            mode={mode}
            setMode={setMode}
            imported={imported.data}
            device={stageDevice}
            frame={draftFrame}
            deviceVariant={deviceVariant}
            rebuild={{ start: (options) => rebuild.mutate(options), pending: rebuild.isPending, error: rebuild.error, progress: rebuildProgress }}
            onScreenChange={setVisibleScreen}
            onScreensChange={setBoardScreens}
            onActiveScreenChange={setActiveScreenId}
            screenRequest={screenRequest}
            commentPending={chatRun.isPending}
            onTokensChange={setDesignTokens}
            tuneRequest={tuneRequest}
            themeRequest={themeRequest}
            themeCss={themeCss}
            onThemesChange={(themes, activeId, effective) => { setBoardThemes(themes); setActiveThemeId(activeId); if (effective !== undefined) setThemesEffective(effective); }}
            tokens={designTokens}
            overrides={activeOverrides}
            onTuneToken={tuneToken}
            onResetToken={resetToken}
            onColourPick={() => { /* Die aufgenommene Farbe wird direkt an der Stelle angeboten. */ }}
            onTextEdit={(before, after) => textEdit.mutate({ before, after })}
            onComment={(target, text) => {
              // Der Kommentar landet sichtbar im Gespräch UND in der Kommentarliste, damit
              // nachvollziehbar bleibt, welche Anweisung auf welches Element gewirkt hat.
              setMessages((all) => [...all, { role: "user", text: `Markierung „${target.label || target.selector}“: ${text}` }]);
              setComments((all) => [...all, { id: `${Date.now()}`, label: target.label || target.selector, selector: target.selector, screenId: target.screenId, screenName: target.screenName, text, at: new Date().toISOString(), status: "läuft" }]);
              setPanelTab("comments");
              chatRun.mutate({ message: text, target });
            }}
          />
        </section>
        {rightOpen && !imported.data?.imported && (
          <aside className="right-panel">
            <header>
              <div>
                <strong>Feineinstellungen</strong>
                <small>Wirken sofort, ohne KI-Lauf</small>
              </div>
              <Button
                variant="ghost"
                onClick={() => {
                  setAccent("#3157D5");
                  setRadius(18);
                  setDarkPreview(false);
                }}
              >
                Alle zurücksetzen
              </Button>
            </header>
            <div className="inspector">
              <label>
                Akzentfarbe
                <div className="color-row">
                  {["#3157D5", "#0C7A5B", "#7C3AED", "#B4530A"].map((c) => (
                    <button className={accent === c ? "active" : ""} onClick={() => setAccent(c)} style={{ background: c }} key={c} />
                  ))}
                </div>
              </label>
              <label>
                Kartenform
                <div className="segments">
                  <button className={radius === 18 ? "active" : ""} onClick={() => setRadius(18)}>
                    Weich
                  </button>
                  <button className={radius === 8 ? "active" : ""} onClick={() => setRadius(8)}>
                    Klar
                  </button>
                </div>
              </label>
              <div className="setting-row">
                <span>Dunkle Vorschau</span>
                <button className={`switch ${darkPreview ? "on" : ""}`} onClick={() => setDarkPreview(!darkPreview)}>
                  <i />
                </button>
              </div>
              <Button
                variant="ghost"
                onClick={() =>
                  api(`/projects/${projectId}/versions`, {
                    method: "POST",
                    body: JSON.stringify({
                      reason: "Checkpoint: Feineinstellungen",
                      baseRevision: design.data?.revision ?? 0,
                    }),
                  })
                }
              >
                Als Version speichern
              </Button>
            </div>
          </aside>
        )}
      </div>
      {modal === "keys" && <KeyboardModal onClose={() => setModal(null)} />} {modal === "share" && <ShareModal onClose={() => setModal(null)} />} {modal === "export" && <ExportModal onClose={() => setModal(null)} />} {modal === "present" && <Present accent={accent} onClose={() => setModal(null)} />}
    </div>
  );
}

type PreviewScreen = { id: string; name: string; isStart: boolean; links: string[] };
// Eine waehlbare Erscheinung des Designs: Hell, Dunkel oder ein eigenes Farbthema des Projekts.
type DesignTheme = { id: string; name: string; kind: "light" | "dark" | "other"; color: string };
// „AppTheme (Nacht)" sagt nichts; besteht die Auswahl nur aus Hell und Dunkel, heissen sie auch so.
function themeLabel(theme: DesignTheme, all: DesignTheme[]): string {
  const simple = all.length <= 2 && all.every((entry) => entry.kind !== "other");
  if (simple) return theme.kind === "dark" ? "Dunkel" : "Hell";
  return theme.name.replace(/\s*\((?:Dunkel|Nacht)\)\s*$/i, "").trim() || theme.id;
}
// Der markierte Bereich: Rechteck fuer den Rahmen in der Vorschau, Selektor und woertlicher
// Ausschnitt, damit die Aenderung genau dieses Element trifft.
type MarkTarget = { rect: { x: number; y: number; width: number; height: number }; selector: string; label: string; html: string; screenId: string; screenName: string };
// Ein abgeschickter Kommentar bleibt nachvollziehbar: WELCHE Anweisung auf WELCHES Element wirkte
// und was daraus wurde. Ohne diese Ansicht waere nach dem Absenden nicht mehr erkennbar, was
// bereits verlangt wurde.
type DesignComment = { id: string; label: string; selector: string; screenId: string; screenName: string; text: string; at: string; status: "läuft" | "angewendet" | "ohne Änderung" | "fehlgeschlagen"; detail?: string };
const entryNamePattern = /^(?:start|home|main|dashboard|launch|overview|onboarding|welcome)/i;
const gateNamePattern = /(?:lock|locked|permission|error|loading|splash|auth|login|signin|gate)/i;

// Ein Sperr- oder Anmeldebildschirm ist ein Zwischenzustand, kein Einstieg. Steht er im Design als
// Startbildschirm, wird trotzdem der eigentliche Einstieg gezeigt — sonst beginnt jeder Rundgang
// durch die App vor einer verschlossenen Tür.
function startScreenOf(list: PreviewScreen[]): PreviewScreen | undefined {
  if (!list.length) return undefined;
  const marked = list.find((screen) => screen.isStart);
  if (marked && !gateNamePattern.test(marked.name)) return marked;
  return list.find((screen) => entryNamePattern.test(screen.name)) ?? list.find((screen) => !gateNamePattern.test(screen.name)) ?? marked ?? list[0];
}

// Die Bühne zeigt GENAU EINEN Bildschirm — so groß wie möglich, vollständig sichtbar und echt
// durchklickbar, genau wie die App auf dem Gerät. Kein Nebeneinander, keine Leisten: nur das Design.
// Eine im Design aufgenommene Farbe: was dort gezeichnet wird und aus welchem Token es stammt.
// `kind` ist die technische Rolle (background/text/border). Mit ihr laesst sich eine Farbe auch
// dann aendern, wenn keine Variable dahintersteht: geaendert wird dann die Farbe selbst, ueberall
// wo sie in dieser Rolle gezeichnet wird.
type ColourEntry = { role: string; kind: string; value: string; hex: string; token: string; exact: boolean };
type ColourPick = { label: string; selector: string; screenName: string; entries: ColourEntry[]; at: number; rect?: { x: number; y: number; width: number; height: number } };
function DesignStage({ previewOrigin, previewPath, previewWidth, previewHeight, device, zoom, setZoom, onScreenChange, onScreensChange, onActiveScreenChange, screenRequest, onComment, commentPending, onTokensChange, tuneRequest, onTextEdit, onThemesChange, themeRequest, themeCss, onColourPick, tokens, overrides, onTuneToken, onResetToken }: {
  previewOrigin: string;
  previewPath: string;
  previewWidth: number;
  previewHeight: number;
  device: StageDevice;
  zoom: number;
  setZoom(value: number): void;
  onScreenChange(name: string | null): void;
  onScreensChange(list: PreviewScreen[]): void;
  onActiveScreenChange(screenId: string | null): void;
  screenRequest: { screenId: string; nonce: number } | null;
  onComment(target: MarkTarget, text: string): void;
  commentPending: boolean;
  onTokensChange(tokens: Record<string, string>): void;
  tuneRequest: { overrides: Record<string, string>; nonce: number } | null;
  onTextEdit(before: string, after: string): void;
  onThemesChange(themes: DesignTheme[], activeThemeId: string, effective?: boolean): void;
  themeRequest: { themeId: string; nonce: number } | null;
  themeCss: string | undefined;
  onColourPick(pick: ColourPick): void;
  tokens: Record<string, string>;
  overrides: Record<string, string>;
  onTuneToken(name: string, value: string): void;
  onResetToken(name: string): void;
}) {
  const viewportRef = useRef<HTMLDivElement>(null);
  const frameRef = useRef<HTMLIFrameElement>(null);
  const commentRef = useRef<HTMLFormElement>(null);
  const pointerPan = useRef<{ pointerId: number; x: number; y: number } | null>(null);
  const previewPan = useRef<CanvasPoint | null>(null);
  const userAdjusted = useRef(false);
  const activeRef = useRef<string | null>(null);
  const sizeRef = useRef({ width: previewWidth, height: previewHeight });
  const [offset, setOffset] = useState<CanvasPoint>({ x: 0, y: 0 });
  const [panning, setPanning] = useState(false);
  // Ohne Referenzgeraet misst sich das Dokument selbst. Ist eines gewaehlt, gilt AUSSCHLIESSLICH
  // dessen Flaeche — sonst zoege die Selbstmessung die Buehne sofort wieder auf die Groesse zurueck,
  // mit der das Design aufgebaut wurde, und die Geraetewahl bliebe wirkungslos.
  const [measuredSize, setMeasuredSize] = useState({ width: previewWidth, height: previewHeight });
  const size = device ? { width: device.width, height: device.height } : measuredSize;
  // Die Groesse der Buehne und die des Eingabefensters entscheiden, wohin das Fenster passt. Ohne
  // beide Werte laesst sich nicht garantieren, dass es vollstaendig sichtbar bleibt.
  const [viewportSize, setViewportSize] = useState({ width: 0, height: 0 });
  const [commentSize, setCommentSize] = useState({ width: 300, height: 168 });
  const [screens, setScreens] = useState<PreviewScreen[]>([]);
  const [history, setHistory] = useState<string[]>([]);
  const { mode, setMode, monitorPixelsPerMillimeter } = useUi();
  const physicalZoom = device?.physicalWidthMm && monitorPixelsPerMillimeter
    ? physicalZoomForDevice(device.width, device.physicalWidthMm, monitorPixelsPerMillimeter)
    : null;
  const [physicalSizeHighlight, setPhysicalSizeHighlight] = useState(false);
  const markMode = mode === "comment";
  const textMode = mode === "edit";
  const pickMode = mode === "pick";
  const [marker, setMarker] = useState<MarkTarget | null>(null);
  const [commentText, setCommentText] = useState("");
  // Der aufgenommene Bereich: umrandet auf der Buehne, mit Farbwaehlern direkt daneben.
  const [pickedArea, setPickedArea] = useState<ColourPick | null>(null);
  // Was gerade unter dem Zeiger liegt — noch vor dem Klick sichtbar.
  const [hoverArea, setHoverArea] = useState<{ label: string; hex: string; textHex: string; rect: { x: number; y: number; width: number; height: number } } | null>(null);
  // Die Erscheinungen des DESIGNS, nicht die des Studios: importierte Apps bringen fast immer
  // mehrere mit — ohne Auswahl bliebe alles ausser der ersten unerreichbar.
  const [designThemes, setDesignThemes] = useState<DesignTheme[]>([]);
  const [designThemeId, setDesignThemeId] = useState("");
  const themesRef = useRef<DesignTheme[]>([]);

  // Der aktive Bildschirm wird nach oben gemeldet, damit das Design Board links ihn hervorheben
  // kann. Der Ref bleibt die Wahrheit fuer den Verlauf; die Meldung ist nur die Anzeige.
  const setActive = (screenId: string | null) => { activeRef.current = screenId; onActiveScreenChange(screenId); };
  useEffect(() => { onScreensChange(screens); }, [screens, onScreensChange]);
  // Der Markierungsmodus lebt im Vorschau-Dokument; die Bühne sagt ihm nur, ob er an ist. Beim
  // Verlassen verschwindet auch der Rahmen, damit keine tote Markierung stehen bleibt.
  useEffect(() => {
    tellFrame({ action: "mark", on: markMode });
    if (!markMode) { setMarker(null); setCommentText(""); }
  }, [markMode, screens.length]);
  useEffect(() => { tellFrame({ action: "text", on: textMode }); }, [textMode, screens.length]);
  useEffect(() => { tellFrame({ action: "pick", on: pickMode }); if (!pickMode) { setPickedArea(null); setHoverArea(null); } }, [pickMode, screens.length]);

  const separator = previewPath.includes("?") ? "&" : "?";
  const stageUrl = `${previewOrigin}${previewPath}${separator}werftStage=1`;
  const tellFrame = (message: Record<string, unknown>) => {
    try { frameRef.current?.contentWindow?.postMessage({ source: "werft-studio-canvas", ...message }, "*"); }
    catch (error) { console.warn("Vorschau nicht erreichbar", error); }
  };
  // Der ganze Bildschirm muss immer hineinpassen — oben, unten, links und rechts nichts abgeschnitten.
  const fitStage = () => {
    const rect = viewportRef.current?.getBoundingClientRect();
    if (!rect || !rect.width || !rect.height) return;
    const result = fitZoomAndOffset({ width: rect.width, height: rect.height }, sizeRef.current, 20);
    setZoom(result.zoom);
    setOffset(result.offset);
  };
  const resetView = () => { userAdjusted.current = false; fitStage(); };

  useEffect(() => { sizeRef.current = size; if (!userAdjusted.current) fitStage(); }, [size.width, size.height]);
  useEffect(() => {
    const viewport = viewportRef.current;
    if (!viewport || typeof ResizeObserver !== "function") return;
    const measureViewport = () => {
      const rect = viewport.getBoundingClientRect();
      setViewportSize((current) => current.width === rect.width && current.height === rect.height ? current : { width: rect.width, height: rect.height });
    };
    measureViewport();
    const observer = new ResizeObserver(() => { measureViewport(); if (!userAdjusted.current) fitStage(); });
    observer.observe(viewport);
    return () => observer.disconnect();
  }, []);
  // Das Eingabefenster wird gemessen, sobald es steht: nur mit seiner echten Hoehe laesst es sich so
  // einklemmen, dass unten nichts abgeschnitten wird.
  useEffect(() => {
    const element = commentRef.current;
    if (!element || typeof ResizeObserver !== "function") return;
    const observer = new ResizeObserver(() => {
      const rect = element.getBoundingClientRect();
      if (rect.width && rect.height) setCommentSize((current) => Math.abs(current.width - rect.width) < 1 && Math.abs(current.height - rect.height) < 1 ? current : { width: rect.width, height: rect.height });
    });
    observer.observe(element);
    return () => observer.disconnect();
  }, [marker?.selector]);

  const zoomAt = (clientX: number, clientY: number, nextZoom: number) => {
    const rect = viewportRef.current?.getBoundingClientRect();
    if (!rect) return;
    userAdjusted.current = true;
    const point = { x: clientX - rect.left, y: clientY - rect.top };
    setOffset((current) => offsetForZoomAtPoint(current, point, zoom, nextZoom));
    setZoom(nextZoom);
  };
  const wheelZoom = (deltaY: number) => {
    const nextZoom = canvasZoomFromWheel(zoom, deltaY, physicalZoom);
    if (physicalZoom && nextZoom === physicalZoom && zoom !== physicalZoom) setPhysicalSizeHighlight(true);
    return nextZoom;
  };
  // Das Rad ohne Strg bewegt das Design, statt es zu vergroessern: gescrollt wird dort, wo das
  // Original scrollt — und sonst wandert die Buehne, damit das Rad nie wirkungslos bleibt.
  const panBy = (deltaX: number, deltaY: number) => {
    if (!deltaX && !deltaY) return;
    userAdjusted.current = true;
    setOffset((current) => ({ x: current.x - deltaX, y: current.y - deltaY }));
  };
  const goBack = () => {
    const previous = history.at(-1);
    if (!previous) return;
    setHistory((entries) => entries.slice(0, -1));
    // Vorher setzen: der zurückgemeldete Wechsel darf nicht erneut in den Verlauf wandern.
    setActive(previous);
    tellFrame({ action: "screen", screenId: previous });
  };
  const goStart = () => {
    const start = startScreenOf(screens);
    if (!start) return;
    setHistory([]);
    setActive(start.id);
    tellFrame({ action: "screen", screenId: start.id });
  };

  useEffect(() => {
    const handleMessage = (event: MessageEvent) => {
      const data = event.data as { source?: string; action?: string; screens?: PreviewScreen[]; screenId?: string; screenName?: string; width?: number; height?: number; x?: number; y?: number; deltaX?: number; deltaY?: number; ctrlKey?: boolean } | null;
      if (!data || event.source !== frameRef.current?.contentWindow) return;
      if (data.source === "werft-preview-screen") {
        const id = typeof data.screenId === "string" ? data.screenId : null;
        const previous = activeRef.current;
        if (id && previous && previous !== id) setHistory((entries) => [...entries, previous]);
        setActive(id);
        onScreenChange(typeof data.screenName === "string" ? data.screenName : null);
        // Automatisch einpassen, bis der Benutzer Zoom oder Position selbst festgelegt hat.
        requestAnimationFrame(() => { if (!userAdjusted.current) fitStage(); });
        return;
      }
      if (data.source !== "werft-preview-canvas") return;
      if (data.action === "text-edit") {
        const edit = data as unknown as { before?: string; after?: string };
        if (typeof edit.before === "string" && typeof edit.after === "string") onTextEdit(edit.before, edit.after);
        return;
      }
      if (data.action === "mark-target") {
        const target = data as unknown as MarkTarget;
        if (target.rect && Number.isFinite(target.rect.width)) { setMarker(target); setCommentText(""); }
        return;
      }
      // Schon beim Darueberfahren steht die erkannte Farbe da — man sieht vor dem Klick, was
      // getroffen wird.
      if (data.action === "colour-hover") {
        const hover = data as unknown as { label?: string; hex?: string; textHex?: string; rect?: { x: number; y: number; width: number; height: number } };
        setHoverArea(hover.rect && Number.isFinite(hover.rect.width) ? { label: hover.label ?? "", hex: hover.hex ?? "", textHex: hover.textHex ?? "", rect: hover.rect } : null);
        return;
      }
      // Die aufgenommene Farbe wird DIREKT an der angeklickten Stelle zum Ändern angeboten — und
      // zugleich an die Farbleiste gemeldet.
      if (data.action === "colour-pick") {
        const picked = data as unknown as { label?: string; selector?: string; screenName?: string; entries?: ColourEntry[]; rect?: { x: number; y: number; width: number; height: number } };
        const pick: ColourPick = {
          label: picked.label ?? "",
          selector: picked.selector ?? "",
          screenName: picked.screenName ?? "",
          entries: (picked.entries ?? []).filter((entry) => entry && entry.value),
          at: Date.now(),
          ...(picked.rect && Number.isFinite(picked.rect.width) ? { rect: picked.rect } : {})
        };
        setPickedArea(pick);
        onColourPick(pick);
        return;
      }
      // Nach dem Umschalten der Erscheinung gelten andere Farbwerte — die Regler bekommen sie sofort.
      if (data.action === "tokens") {
        onTokensChange((data as unknown as { tokens?: Record<string, string> }).tokens ?? {});
        return;
      }
      if (data.action === "screens") {
        const list = (data.screens ?? []).filter((screen) => screen && typeof screen.id === "string");
        setScreens(list);
        const themePayload = data as { themes?: DesignTheme[]; activeThemeId?: string; themesEffective?: boolean };
        const themes = (themePayload.themes ?? []).filter((theme) => theme && typeof theme.id === "string");
        const activeId = typeof themePayload.activeThemeId === "string" ? themePayload.activeThemeId : "";
        themesRef.current = themes;
        setDesignThemes(themes);
        setDesignThemeId(activeId);
        onThemesChange(themes, activeId, themePayload.themesEffective !== false);
        onTokensChange((data as { tokens?: Record<string, string> }).tokens ?? {});
        // Beim Öffnen steht der Startbildschirm — nicht der, den das Dokument zufällig zuerst führt.
        const start = startScreenOf(list);
        if (start && activeRef.current === null) { setActive(start.id); tellFrame({ action: "screen", screenId: start.id }); }
        return;
      }
      // Das Design kann sich auch selbst umschalten — dann folgt die Anzeige der Wahl.
      if (data.action === "theme-changed") {
        const changed = data as { themeId?: string };
        if (typeof changed.themeId === "string") { setDesignThemeId(changed.themeId); onThemesChange(themesRef.current, changed.themeId); }
        return;
      }
      if (data.action === "size" && Number.isFinite(data.width) && Number.isFinite(data.height)) {
        const next = { width: Math.round(data.width!), height: Math.round(data.height!) };
        setMeasuredSize((current) => (current.width === next.width && current.height === next.height ? current : next));
        return;
      }
      const frame = frameRef.current;
      if (!frame || !Number.isFinite(data.x) || !Number.isFinite(data.y) || !frame.offsetWidth || !frame.offsetHeight) return;
      const rect = frame.getBoundingClientRect();
      const point = { x: rect.left + data.x! * (rect.width / frame.offsetWidth), y: rect.top + data.y! * (rect.height / frame.offsetHeight) };
      // Vergroessern nur mit Strg — sonst wandert die Ansicht, genau wie beim Scrollen in der App.
      if (data.action === "wheel" && Number.isFinite(data.deltaY)) {
        if (data.ctrlKey) zoomAt(point.x, point.y, wheelZoom(data.deltaY!));
        else panBy(Number.isFinite(data.deltaX) ? data.deltaX! : 0, data.deltaY!);
      }
      else if (data.action === "pan-start") { previewPan.current = point; setPanning(true); }
      else if (data.action === "pan-move" && previewPan.current) {
        const start = previewPan.current;
        userAdjusted.current = true;
        setOffset((current) => ({ x: current.x + point.x - start.x, y: current.y + point.y - start.y }));
        previewPan.current = point;
      } else if (data.action === "pan-end") { previewPan.current = null; setPanning(false); }
    };
    window.addEventListener("message", handleMessage);
    return () => window.removeEventListener("message", handleMessage);
  }, [zoom, physicalZoom, history.length]);

  useEffect(() => {
    const viewport = viewportRef.current;
    if (!viewport) return;
    const handleWheel = (event: WheelEvent) => {
      event.preventDefault();
      if (event.ctrlKey) zoomAt(event.clientX, event.clientY, wheelZoom(event.deltaY));
      else panBy(event.deltaX, event.deltaY);
    };
    viewport.addEventListener("wheel", handleWheel, { passive: false });
    return () => viewport.removeEventListener("wheel", handleWheel);
  }, [zoom, physicalZoom]);

  // Zurück und Start ohne sichtbare Leiste: die Tastatur genügt, die Schaltflächen erscheinen nur,
  // wenn die Maus in die Ecke fährt.
  useEffect(() => {
    const handleKey = (event: KeyboardEvent) => {
      if ((event.target as HTMLElement | null)?.matches?.("input,textarea,select,[contenteditable]")) return;
      if (event.key === "Backspace" || (event.altKey && event.key === "ArrowLeft")) { event.preventDefault(); goBack(); }
      if (event.key === "Home") { event.preventDefault(); goStart(); }
    };
    addEventListener("keydown", handleKey);
    return () => removeEventListener("keydown", handleKey);
  }, [history, screens]);

  // Klick im Design Board: wie ein Klick im Design selbst — der bisherige Bildschirm wandert in den
  // Verlauf, damit die Rücktaste auch von der Navigation aus zurückführt.
  useEffect(() => {
    if (!screenRequest || screenRequest.screenId === activeRef.current) return;
    const previous = activeRef.current;
    if (previous) setHistory((entries) => [...entries, previous]);
    setActive(screenRequest.screenId);
    tellFrame({ action: "screen", screenId: screenRequest.screenId });
  }, [screenRequest?.nonce]);

  // Das Referenzgeraet wird dem Dokument mitgeteilt: der Rahmen bekommt die Geraeteflaeche, damit
  // die Media-Queries des Designs genau so greifen wie auf dem Geraet, und die Bildschirme im
  // Dokument werden auf dieselbe Flaeche gezwungen. Danach wird wieder vollstaendig eingepasst.
  useEffect(() => {
    tellFrame({ action: "viewport", width: device?.width ?? 0, height: device?.height ?? 0 });
    // Ohne Geraet gilt wieder die Groesse des Designs. Sie wird auf den bekannten Ausgangswert
    // gesetzt, bis die Vorschau selbst gemessen hat — sonst bliebe die zuletzt erzwungene stehen.
    if (!device) setMeasuredSize({ width: previewWidth, height: previewHeight });
    userAdjusted.current = false;
    requestAnimationFrame(() => { if (!userAdjusted.current) fitStage(); });
  }, [device?.id, device?.width, device?.height, screens.length]);
  // Farbregler wirken sofort im Design — ohne KI-Lauf und auf allen Bildschirmen zugleich.
  useEffect(() => { if (tuneRequest) tellFrame({ action: "tune", overrides: tuneRequest.overrides }); }, [tuneRequest?.nonce]);
  // Die Wahl aus dem Design Board gilt für ALLE Bildschirme: umgeschaltet wird am Dokument selbst,
  // nicht je Bildschirm — sonst wäre das Design nach dem ersten Klick uneinheitlich.
  useEffect(() => { if (themeRequest?.themeId) tellFrame({ action: "theme", themeId: themeRequest.themeId }); }, [themeRequest?.nonce]);
  // Ein vor dieser Fassung aufgebautes Design trägt nur EINE Erscheinung in sich. Die aus den
  // Projektquellen gemessenen Blöcke werden nachgereicht — damit ist es ohne Neuaufbau umschaltbar.
  useEffect(() => { if (themeCss) tellFrame({ action: "theme-css", css: themeCss }); }, [themeCss, screens.length]);

  const startPan = (event: ReactPointerEvent<HTMLDivElement>) => {
    const onEmptySurface = event.button === 0 && event.target === viewportRef.current;
    if (event.button !== 1 && !onEmptySurface) return;
    event.preventDefault();
    event.currentTarget.setPointerCapture(event.pointerId);
    pointerPan.current = { pointerId: event.pointerId, x: event.clientX, y: event.clientY };
    setPanning(true);
  };
  const movePan = (event: ReactPointerEvent<HTMLDivElement>) => {
    const previous = pointerPan.current;
    if (!previous || previous.pointerId !== event.pointerId) return;
    userAdjusted.current = true;
    setOffset((current) => ({ x: current.x + event.clientX - previous.x, y: current.y + event.clientY - previous.y }));
    pointerPan.current = { pointerId: event.pointerId, x: event.clientX, y: event.clientY };
  };
  const endPan = (event: ReactPointerEvent<HTMLDivElement>) => {
    if (pointerPan.current?.pointerId !== event.pointerId) return;
    pointerPan.current = null;
    if (event.currentTarget.hasPointerCapture(event.pointerId)) event.currentTarget.releasePointerCapture(event.pointerId);
    setPanning(false);
  };

  // Das Eingabefenster sitzt am markierten Element, wird aber IMMER in die sichtbare Fläche
  // gezwungen: erst unter das Element, sonst darüber, und notfalls an den Rand geklemmt. Es darf
  // nie halb hinter der Kante liegen — dort liesse sich nichts eintippen.
  const edge = 12, gap = 10;
  const commentPlacement = (() => {
    if (!marker) return undefined;
    const anchorLeft = offset.x + marker.rect.x * zoom;
    const anchorTop = offset.y + marker.rect.y * zoom;
    const anchorBottom = anchorTop + marker.rect.height * zoom;
    if (!viewportSize.width || !viewportSize.height) return { left: Math.max(edge, anchorLeft), top: Math.max(edge, anchorBottom + gap) };
    const clamp = (value: number, min: number, max: number) => Math.min(Math.max(value, min), Math.max(min, max));
    const below = anchorBottom + gap;
    const above = anchorTop - gap - commentSize.height;
    const fitsBelow = below + commentSize.height <= viewportSize.height - edge;
    const top = fitsBelow ? below : above >= edge ? above : clamp(below, edge, viewportSize.height - commentSize.height - edge);
    return {
      left: clamp(anchorLeft, edge, viewportSize.width - commentSize.width - edge),
      top: clamp(top, edge, Math.max(edge, viewportSize.height - commentSize.height - edge))
    };
  })();

  // Das Farbfenster sitzt am erkannten Bereich, wird aber immer in die sichtbare Flaeche gezwungen —
  // sonst laege es bei einem Bereich am unteren Rand halb ausserhalb und waere nicht bedienbar.
  const pickPlacement = (() => {
    const rect = pickedArea?.rect;
    if (!rect) return undefined;
    const width = 272, height = 46 + (pickedArea?.entries.length ?? 0) * 34;
    const anchorLeft = offset.x + rect.x * zoom;
    const anchorTop = offset.y + rect.y * zoom;
    const anchorBottom = anchorTop + rect.height * zoom;
    if (!viewportSize.width || !viewportSize.height) return { left: Math.max(edge, anchorLeft), top: Math.max(edge, anchorBottom + gap) };
    const clamp = (value: number, min: number, max: number) => Math.min(Math.max(value, min), Math.max(min, max));
    const below = anchorBottom + gap;
    const above = anchorTop - gap - height;
    const top = below + height <= viewportSize.height - edge ? below : above >= edge ? above : clamp(below, edge, viewportSize.height - height - edge);
    return { left: clamp(anchorLeft, edge, viewportSize.width - width - edge), top: clamp(top, edge, Math.max(edge, viewportSize.height - height - edge)) };
  })();

  return (
    <div
      ref={viewportRef}
      className={`canvas-viewport stage-viewport${panning ? " panning" : ""}`}
      onPointerDown={startPan}
      onPointerMove={movePan}
      onPointerUp={endPan}
      onPointerCancel={endPan}
      onDoubleClick={resetView}
      onAuxClick={(event) => { if (event.button === 1) event.preventDefault(); }}
    >
      <div className={`canvas-pan-content stage-surface${physicalSizeHighlight ? " physical-size" : ""}`} style={{ transform: `translate(${offset.x}px, ${offset.y}px) scale(${zoom})`, width: size.width, height: size.height }} onAnimationEnd={() => setPhysicalSizeHighlight(false)}>
        <iframe
          ref={frameRef}
          title="Design"
          src={stageUrl}
          style={{ width: size.width, height: size.height }}
          sandbox="allow-scripts allow-same-origin allow-forms allow-modals allow-popups allow-downloads allow-pointer-lock"
          allow="autoplay; fullscreen"
        />
        {marker && <div className="mark-frame" style={{ left: marker.rect.x, top: marker.rect.y, width: marker.rect.width, height: marker.rect.height }} />}
        {pickedArea?.rect && <div className="mark-frame pick-frame" style={{ left: pickedArea.rect.x, top: pickedArea.rect.y, width: pickedArea.rect.width, height: pickedArea.rect.height }} />}
      </div>
      {/* Die Farbe wird DORT geaendert, wo sie aufgenommen wurde: der erkannte Bereich ist umrandet,
          sein Farbwaehler steht daneben und wirkt sofort. Den Weg ueber die Liste rechts braucht
          dafuer niemand mehr. */}
      {pickMode && hoverArea && !pickedArea && (
        <div className="pick-hover" style={{ left: offset.x + hoverArea.rect.x * zoom, top: Math.max(4, offset.y + hoverArea.rect.y * zoom - 30) }}>
          <i style={{ backgroundImage: `linear-gradient(${hoverArea.hex || "transparent"}, ${hoverArea.hex || "transparent"})` }} />
          <span>{hoverArea.hex || "ohne eigene Fläche"}</span>
          <em>{hoverArea.label}</em>
        </div>
      )}
      {pickedArea && (
        <div className="pick-panel" style={pickPlacement} onPointerDown={(event) => event.stopPropagation()}>
          <header>
            <strong title={pickedArea.selector}>{pickedArea.label || pickedArea.selector}</strong>
            <button type="button" title="Schließen" onClick={() => setPickedArea(null)}><X /></button>
          </header>
          {pickedArea.entries.length === 0 && <small>An dieser Stelle wird keine eigene Farbe gezeichnet.</small>}
          {pickedArea.entries.map((entry) => {
            // Steht hinter der Farbe eine wirksame Variable, wird sie geaendert; sonst die Farbe
            // selbst — dann ist auch eine fest ins Markup geschriebene Flaeche regelbar.
            const key = entry.exact && entry.token ? entry.token : `${entry.kind || "background"}|${entry.value}`;
            const current = overrides[key] ?? (entry.token ? tokens[entry.token] ?? entry.value : entry.value);
            const changed = Boolean(overrides[key]);
            return (
              <label className="pick-row" key={`${entry.role}:${entry.value}`}>
                <span>{entry.role}<small>{hexColour(current) || entry.hex || entry.value}</small></span>
                <input
                  type="color"
                  value={hexColour(current) || "#000000"}
                  title={entry.exact && entry.token ? `Ändert ${tokenTitle(entry.token)} — wirkt sofort` : "Ändert diese Farbe überall dort, wo sie so gezeichnet wird — wirkt sofort"}
                  onChange={(event) => onTuneToken(key, event.target.value)}
                />
                <button type="button" className="token-reset" disabled={!changed} title={changed ? "Diese Farbe zurücksetzen" : "Unverändert"} onClick={() => onResetToken(key)}>
                  <Undo2 />
                </button>
              </label>
            );
          })}
        </div>
      )}
      {/* Das Eingabefenster steht ÜBER der Bühne, nicht in ihr: innerhalb wurde es vom Rand des
          Designs abgeschnitten und war nicht mehr benutzbar. Hier ist es immer vollständig sichtbar. */}
      {marker && (
        <form
          ref={commentRef}
          className="mark-comment"
          style={commentPlacement}
          onSubmit={(event) => {
            event.preventDefault();
            const text = commentText.trim();
            if (!text || commentPending) return;
            onComment(marker, text);
            setCommentText("");
            setMarker(null);
          }}
        >
          <strong title={marker.selector}>{marker.label || marker.selector}</strong>
          <textarea
            autoFocus
            rows={2}
            value={commentText}
            placeholder="Was soll hier anders sein?"
            onChange={(event) => setCommentText(event.target.value)}
            onKeyDown={(event) => { if (event.key === "Escape") { setMarker(null); setCommentText(""); } }}
          />
          <footer>
            <button type="button" onClick={() => { setMarker(null); setCommentText(""); }}>Verwerfen</button>
            <button type="submit" className="primary" disabled={!commentText.trim() || commentPending}>{commentPending ? "läuft …" : "Senden"}</button>
          </footer>
        </form>
      )}
      <div className="stage-modes">
        <button type="button" className={!markMode && !textMode && !pickMode ? "active" : ""} onClick={() => setMode("interact")} title="Interagieren (V)"><MousePointer2 /></button>
        <button type="button" className={textMode ? "active" : ""} onClick={() => setMode("edit")} title="Text direkt ändern (E) — wirkt sofort, ohne KI"><PenLine /></button>
        <button type="button" className={markMode ? "active" : ""} onClick={() => setMode("comment")} title="Bereich markieren und kommentieren (C)"><MessageSquare /></button>
        <button type="button" className={pickMode ? "active" : ""} onClick={() => setMode(pickMode ? "interact" : "pick")} title="Farbe im Design aufnehmen (F) — zeigt, welche Farbe hier gilt und welcher Regler sie ändert"><Pipette /></button>
        {designThemes.length > 1 && (
          <button
            type="button"
            title={`Erscheinung wechseln — gerade: ${themeLabel(designThemes.find((theme) => theme.id === designThemeId) ?? designThemes[0]!, designThemes)}`}
            onClick={() => {
              const index = designThemes.findIndex((theme) => theme.id === designThemeId);
              const next = designThemes[(index + 1 + designThemes.length) % designThemes.length]!;
              setDesignThemeId(next.id);
              onThemesChange(designThemes, next.id);
              tellFrame({ action: "theme", themeId: next.id });
            }}
          >
            {(designThemes.find((theme) => theme.id === designThemeId)?.kind ?? "light") === "dark" ? <Sun /> : <Moon />}
          </button>
        )}
      </div>
      <div className="stage-hint">
        {history.length > 0 && <button type="button" onClick={goBack} title="Zurück (Rücktaste)"><ChevronLeft /></button>}
        {screens.length > 1 && <button type="button" onClick={goStart} title="Zum Startbildschirm (Pos 1)"><Home /></button>}
      </div>
    </div>
  );
}

const tools: [ToolMode, string, ReactNode][] = [
  ["interact", "Interagieren", <MousePointer2 />],
  ["select", "Auswählen", <MousePointer2 />],
  ["comment", "Kommentar", <MessageSquare />],
  ["edit", "Bearbeiten", <PenLine />],
  ["draw", "Zeichnen", <WandSparkles />],
];
// Der Neuaufbau wird oben in der Kopfleiste bedient; die Buehne zeigt nur noch seinen Fortschritt.
type RebuildControl = { start(options: { retryFailed: boolean; force?: boolean; viewport?: RebuildViewport }): void; pending: boolean; error: Error | null; progress: ReconstructionJob | null };
function Canvas({ projectId, accent, radius, dark, zoom, setZoom, mode, setMode, imported, device, frame, deviceVariant, rebuild, onScreenChange, onScreensChange, onActiveScreenChange, screenRequest, onComment, commentPending, onTokensChange, tuneRequest, onTextEdit, onThemesChange, themeRequest, themeCss, onColourPick, tokens, overrides, onTuneToken, onResetToken }: { projectId: string; accent: string; radius: number; dark: boolean; zoom: number; setZoom(v: number): void; mode: ToolMode; setMode(v: ToolMode): void; imported: ProjectImport | undefined; device: StageDevice; frame: DesignDocument["frames"][number] | undefined; deviceVariant: DesignVariant | undefined; rebuild: RebuildControl; onScreenChange(name: string | null): void; onScreensChange(list: PreviewScreen[]): void; onActiveScreenChange(screenId: string | null): void; screenRequest: { screenId: string; nonce: number } | null; onComment(target: MarkTarget, text: string): void; commentPending: boolean; onTokensChange(tokens: Record<string, string>): void; tuneRequest: { overrides: Record<string, string>; nonce: number } | null; onTextEdit(before: string, after: string): void; onThemesChange(themes: DesignTheme[], activeThemeId: string, effective?: boolean): void; themeRequest: { themeId: string; nonce: number } | null; themeCss: string | undefined; onColourPick(pick: ColourPick): void; tokens: Record<string, string>; overrides: Record<string, string>; onTuneToken(name: string, value: string): void; onResetToken(name: string): void }) {
  const previewOrigin = `${window.location.protocol}//${window.location.hostname}:8444`;
  const viewportRef = useRef<HTMLDivElement>(null);
  const pointerPanRef = useRef<{ pointerId: number; x: number; y: number } | null>(null);
  const [offset, setOffset] = useState<CanvasPoint>({ x: 0, y: 0 });
  const [panning, setPanning] = useState(false);
  const monitorPixelsPerMillimeter = useUi((state) => state.monitorPixelsPerMillimeter);
  const physicalZoom = device?.physicalWidthMm && monitorPixelsPerMillimeter
    ? physicalZoomForDevice(device.width, device.physicalWidthMm, monitorPixelsPerMillimeter)
    : null;
  const [physicalSizeHighlight, setPhysicalSizeHighlight] = useState(false);
  const reconstructionProgress = rebuild.progress;
  const zoomAt = (clientX: number, clientY: number, nextZoom: number) => {
    const rect = viewportRef.current?.getBoundingClientRect();
    if (!rect) return;
    const point = { x: clientX - rect.left, y: clientY - rect.top };
    setOffset((current) => offsetForZoomAtPoint(current, point, zoom, nextZoom));
    setZoom(nextZoom);
  };
  const zoomAtCenter = (nextZoom: number) => {
    const rect = viewportRef.current?.getBoundingClientRect();
    if (rect) zoomAt(rect.left + rect.width / 2, rect.top + rect.height / 2, nextZoom);
    else setZoom(nextZoom);
  };
  const wheelZoom = (deltaY: number) => {
    const nextZoom = canvasZoomFromWheel(zoom, deltaY, physicalZoom);
    if (physicalZoom && nextZoom === physicalZoom && zoom !== physicalZoom) setPhysicalSizeHighlight(true);
    return nextZoom;
  };
  const handlePointerDown = (event: ReactPointerEvent<HTMLDivElement>) => {
    // Mittlere Maustaste ueberall, linke nur auf der freien Flaeche zwischen den Rahmen.
    if (event.button !== 1 && !(event.button === 0 && event.target === viewportRef.current)) return;
    event.preventDefault();
    event.currentTarget.setPointerCapture(event.pointerId);
    pointerPanRef.current = { pointerId: event.pointerId, x: event.clientX, y: event.clientY };
    setPanning(true);
  };
  const handlePointerMove = (event: ReactPointerEvent<HTMLDivElement>) => {
    const previous = pointerPanRef.current;
    if (!previous || previous.pointerId !== event.pointerId) return;
    setOffset((current) => ({ x: current.x + event.clientX - previous.x, y: current.y + event.clientY - previous.y }));
    pointerPanRef.current = { pointerId: event.pointerId, x: event.clientX, y: event.clientY };
  };
  const endPointerPan = (event: ReactPointerEvent<HTMLDivElement>) => {
    if (pointerPanRef.current?.pointerId !== event.pointerId) return;
    pointerPanRef.current = null;
    if (event.currentTarget.hasPointerCapture(event.pointerId)) event.currentTarget.releasePointerCapture(event.pointerId);
    setPanning(false);
  };
  useEffect(() => { setOffset({ x: 0, y: 0 }); }, [projectId]);
  // Auf der Leinwand gilt dieselbe Regel wie in der Buehne: das Rad allein bewegt die Ansicht,
  // vergroessert wird nur mit gedrueckter Strg-Taste.
  useEffect(() => {
    const viewport = viewportRef.current;
    if (!viewport) return;
    const handleWheel = (event: WheelEvent) => {
      event.preventDefault();
      if (event.ctrlKey) zoomAt(event.clientX, event.clientY, wheelZoom(event.deltaY));
      else setOffset((current) => ({ x: current.x - event.deltaX, y: current.y - event.deltaY }));
    };
    viewport.addEventListener("wheel", handleWheel, { passive: false });
    return () => viewport.removeEventListener("wheel", handleWheel);
  }, [zoom, setZoom, physicalZoom, imported?.imported]);
  useEffect(() => {
    if (imported?.imported && !imported.previewPath && !rebuild.pending && !rebuild.error) rebuild.start({ retryFailed: false });
  }, [imported?.imported, imported?.imported ? imported.previewPath : undefined, projectId]);
  const viewportProps = {
    ref: viewportRef,
    className: `canvas-viewport${panning ? " panning" : ""}`,
    onPointerDown: handlePointerDown,
    onPointerMove: handlePointerMove,
    onPointerUp: endPointerPan,
    onPointerCancel: endPointerPan,
    onAuxClick: (event: ReactPointerEvent<HTMLDivElement>) => { if (event.button === 1) event.preventDefault(); },
  };
  const contentStyle = { transform: `translate(${offset.x}px, ${offset.y}px) scale(${zoom})` };
  if (imported?.imported) {
    return (
      <div className="canvas imported-canvas">
        {imported.previewPath ? (
          <DesignStage
            key={`${projectId}:${imported.revision}:${deviceVariant?.previewPath ?? "basis"}`}
            previewOrigin={previewOrigin}
            previewPath={deviceVariant?.previewPath ?? imported.previewPath}
            previewWidth={imported.previewWidth}
            previewHeight={imported.previewHeight}
            device={device}
            zoom={zoom}
            setZoom={setZoom}
            onScreenChange={onScreenChange}
            onScreensChange={onScreensChange}
            onActiveScreenChange={onActiveScreenChange}
            screenRequest={screenRequest}
            onComment={onComment}
            commentPending={commentPending}
            onTokensChange={onTokensChange}
            tuneRequest={tuneRequest}
            onTextEdit={onTextEdit}
            onThemesChange={onThemesChange}
            themeRequest={themeRequest}
            themeCss={themeCss}
            onColourPick={onColourPick}
            tokens={tokens}
            overrides={overrides}
            onTuneToken={onTuneToken}
            onResetToken={onResetToken}
          />
        ) : (
          <div className="empty reconstruction-state">
            <strong>{rebuild.error ? "Die HTML-Version konnte noch nicht fertiggestellt werden." : "Das Ist-Design wird automatisch als HTML aufgebaut."}</strong>
            {!rebuild.error && <ProgressMeters percent={reconstructionProgress?.progress ?? 0} phaseProgress={reconstructionProgress?.result?.phaseProgress} elapsedMs={reconstructionProgress?.result?.elapsedMs} estimatedRemainingMs={reconstructionProgress?.result?.estimatedRemainingMs} currentOperation={reconstructionProgress?.result?.currentOperation} />}
            <span>{reconstructionProgress?.result?.message ?? "Alle UI-Quellen, Themes, Ressourcen und Abmessungen werden geprüft."}</span>
            {reconstructionProgress?.result?.totalFiles ? <small>{reconstructionProgress.result.processedFiles} von {reconstructionProgress.result.totalFiles} UI-Dateien verarbeitet</small> : null}
            {(reconstructionProgress?.result?.totalOperations ?? 0) > 0 ? <small>{reconstructionProgress?.result?.completedOperations ?? 0} von {reconstructionProgress?.result?.totalOperations} KI-Schritten abgeschlossen{reconstructionProgress?.result?.retryCount ? ` · ${reconstructionProgress.result.retryCount} Verbindungswiederholung${reconstructionProgress.result.retryCount === 1 ? "" : "en"}` : ""}</small> : null}
            {reconstructionProgress?.result?.todos && <div className="import-todos">{reconstructionProgress.result.todos.map((todo) => <span className={todo.status} key={todo.label}>{todo.status === "completed" ? <Check /> : <i />}{todo.label}</span>)}</div>}
            {rebuild.error && <Button variant="primary" onClick={() => rebuild.start({ retryFailed: true })}>Rekonstruktion erneut starten</Button>}
            {rebuild.error && <p className="field-error">{rebuild.error instanceof ApiError ? rebuild.error.message : "Die Rekonstruktion ist fehlgeschlagen. Bitte erneut versuchen."}</p>}
          </div>
        )}
        {/* Der Aufbau laeuft in Minuten und muss sichtbar bleiben — aber nur waehrend er laeuft.
            Sonst steht ueber dem Design dauerhaft Text, der nicht zum Design gehoert. */}
        {imported.previewPath && rebuild.pending && (
          <div className="stage-progress">
            <span>{reconstructionProgress?.result?.message ?? "Design wird aus den Quellen aufgebaut."}</span>
            <ProgressMeters percent={reconstructionProgress?.progress ?? 0} phaseProgress={reconstructionProgress?.result?.phaseProgress} elapsedMs={reconstructionProgress?.result?.elapsedMs} estimatedRemainingMs={reconstructionProgress?.result?.estimatedRemainingMs} currentOperation={reconstructionProgress?.result?.currentOperation} />
          </div>
        )}
        {/* Scheitert der Neuaufbau, waehrend schon ein Design steht, blieb das bisher still: der
            Knopf oben ging aus, und nichts sagte warum. Der Grund gehoert an die Buehne. */}
        {imported.previewPath && !rebuild.pending && rebuild.error && (
          <div className="stage-progress stage-error">
            <strong>Der Neuaufbau ist fehlgeschlagen — das bisherige Design bleibt unverändert.</strong>
            <span>{rebuild.error instanceof ApiError ? rebuild.error.message : "Der Aufbau aus den Quellen konnte nicht abgeschlossen werden."}</span>
            <Button variant="primary" onClick={() => rebuild.start({ retryFailed: true, force: imported.reconstructed })}>Erneut versuchen</Button>
          </div>
        )}
      </div>
    );
  }
  return (
    <div className="canvas">
      <div className="canvas-toolbar">
        <IconButton label="Vorschau aktualisieren">
          <RotateCcw />
        </IconButton>
        <span>{frame?.platform === "android" ? `${device?.label ?? frame.device} · ${device?.width ?? frame.width} × ${device?.height ?? frame.height}` : "iPhone 15 · 390"}</span>
        {tools.map(([id, label, icon]) => (
          <button className={mode === id ? "active" : ""} onClick={() => setMode(id)} key={id}>
            {icon}
            {label}
          </button>
        ))}
        <IconButton label="Verkleinern" onClick={() => zoomAtCenter(zoom - 0.1)}>
          <ZoomOut />
        </IconButton>
        <b>{Math.round(zoom * 100)} %</b>
        <IconButton label="Vergrößern" onClick={() => zoomAtCenter(zoom + 0.1)}>
          <ZoomIn />
        </IconButton>
      </div>
      {mode === "draw" && (
        <div className="draw-toolbar">
          <button className="active">Pfeil</button>
          <button>Linie</button>
          <button>Rechteck</button>
          <button>Ellipse</button>
          <button>Frei</button>
          <i style={{ background: "#E23D3D" }} />
          <i style={{ background: "#3157D5" }} />
          <Button variant="primary">An KI senden</Button>
        </div>
      )}
      <div {...viewportProps} className={`frame-scroller ${viewportProps.className}`}>
        <div className="canvas-pan-content frame-content" style={contentStyle}>
          <div className="frames">
            {frame?.platform === "android" ? (
              <AndroidFrame accent={accent} dark={dark} width={device?.width ?? frame.width} height={device?.height ?? frame.height} device={device?.label ?? frame.device} physicalSizeHighlight={physicalSizeHighlight} onPhysicalSizeHighlightEnd={() => setPhysicalSizeHighlight(false)} />
            ) : (
              <>
                <PhoneFrame name="Onboarding" accent={accent} radius={radius} dark={dark} onboarding />
                <PhoneFrame name="Dashboard" accent={accent} radius={radius} dark={dark} />
                <AndroidFrame accent={accent} dark={dark} />
                <WindowsFrame accent={accent} dark={dark} />
                <WebFrame accent={accent} dark={dark} />
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
const tx = [
  ["R", "REWE Markt", "Lebensmittel", "−34,90 €"],
  ["M", "Gehalt Juli", "Eingang · Muster GmbH", "+3.120,00 €"],
  ["B", "BVG Ticket", "Mobilität", "−49,00 €"],
];
function PhoneFrame({ name, accent, radius, dark, onboarding = false }: { name: string; accent: string; radius: number; dark: boolean; onboarding?: boolean }) {
  const style = {
    "--preview-accent": accent,
    "--preview-radius": `${radius}px`,
  } as React.CSSProperties;
  return (
    <div className={`frame-wrap ${dark ? "preview-dark" : ""}`} style={style}>
      <label>
        <strong>{name}</strong>
        <span>iOS · iPhone 15 · Standard</span>
        <code>v14</code>
      </label>
      <div className="phone-frame">
        <div className="phone-status">
          <b>9:41</b>
          <i />
        </div>
        {onboarding ? (
          <div className="onboarding-preview">
            <b className="fluss-mark">f</b>
            <h2>Willkommen bei Fluss</h2>
            <p>Dein Konto, deine Karten und dein Tagesüberblick · an einem ruhigen Ort.</p>
            <button>Konto eröffnen</button>
            <button>Ich habe bereits ein Konto</button>
          </div>
        ) : (
          <>
            <div className="mobile-content">
              <p>Guten Morgen, Frank</p>
              <h2>Dein Tag im Überblick</h2>
              <div className="balance">
                <small>Girokonto · DE89 …4021</small>
                <b>2.847,12 €</b>
                <div>
                  <span>Senden</span>
                  <span>Anfordern</span>
                  <span>Karte</span>
                </div>
              </div>
              <h3>Letzte Umsätze</h3>
              {tx.map(([i, n, c, a]) => (
                <div className="tx" key={n}>
                  <i>{i}</i>
                  <span>
                    <b>{n}</b>
                    <small>{c}</small>
                  </span>
                  <strong>{a}</strong>
                </div>
              ))}
            </div>
            <div className="mobile-nav">
              <span>Übersicht</span>
              <span>Zahlungen</span>
              <span>Karten</span>
              <span>Mehr</span>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
function AndroidFrame({ accent, dark, width = 390, height = 760, device = "Pixel 9", physicalSizeHighlight = false, onPhysicalSizeHighlightEnd }: { accent: string; dark: boolean; width?: number; height?: number; device?: string; physicalSizeHighlight?: boolean; onPhysicalSizeHighlightEnd?(): void }) {
  return (
    <div className={`frame-wrap ${dark ? "preview-dark" : ""}`} style={{ "--preview-accent": accent } as React.CSSProperties}>
      <label>
        <strong>Dashboard</strong>
        <span>Android · {device}</span>
        <code>v14</code>
      </label>
      <div className={`android-frame${physicalSizeHighlight ? " physical-size" : ""}`} style={{ width, height }} onAnimationEnd={onPhysicalSizeHighlightEnd}>
        <div className="phone-status">
          <b>9:41</b>
        </div>
        <header>
          <h2>Fluss</h2>
          <i>F</i>
        </header>
        <div className="mobile-content">
          <div className="balance">
            <small>Girokonto · DE89 …4021</small>
            <b>2.847,12 €</b>
          </div>
          <div className="android-actions">
            <button>Senden</button>
            <button>Anfordern</button>
            <button>Karte</button>
          </div>
          <h3>Letzte Umsätze</h3>
          {tx.map(([i, n, c, a]) => (
            <div className="tx bare" key={n}>
              <i>{i}</i>
              <span>
                <b>{n}</b>
                <small>{c}</small>
              </span>
              <strong>{a}</strong>
            </div>
          ))}
        </div>
        <button className="fab">+</button>
        <div className="mobile-nav">
          <span>Übersicht</span>
          <span>Zahlungen</span>
          <span>Karten</span>
        </div>
      </div>
    </div>
  );
}
function WindowsFrame({ accent, dark }: { accent: string; dark: boolean }) {
  return (
    <div className={`frame-wrap ${dark ? "preview-dark" : ""}`} style={{ "--preview-accent": accent } as React.CSSProperties}>
      <label>
        <strong>Kontenübersicht</strong>
        <span>Windows · Fluent · Standard</span>
        <code>v14</code>
      </label>
      <div className="windows-frame">
        <header>
          <b>f</b>
          <span>Fluss für Windows · Kontenübersicht</span>
          <i>─　□　×</i>
        </header>
        <div className="windows-body">
          <nav>
            {["Konten", "Zahlungen", "Karten", "Berichte"].map((x) => (
              <span className={x === "Konten" ? "active" : ""} key={x}>
                {x}
              </span>
            ))}
          </nav>
          <main>
            <div className="section-head">
              <h2>Konten</h2>
              <Button variant="primary">Neue Überweisung</Button>
              <Button>Exportieren</Button>
            </div>
            <div className="account-grid">
              {[
                ["Girokonto", "2.847,12 €"],
                ["Tagesgeld", "12.400,00 €"],
                ["Kreditkarte", "−412,80 €"],
              ].map(([x, y]) => (
                <div key={x}>
                  <small>{x}</small>
                  <b>{y}</b>
                </div>
              ))}
            </div>
            <div className="table">
              {tx.map(([, n, c, a]) => (
                <div key={n}>
                  <b>{n}</b>
                  <span>{c}</span>
                  <strong>{a}</strong>
                </div>
              ))}
            </div>
          </main>
        </div>
      </div>
    </div>
  );
}
function WebFrame({ accent, dark }: { accent: string; dark: boolean }) {
  return (
    <div className={`frame-wrap ${dark ? "preview-dark" : ""}`} style={{ "--preview-accent": accent } as React.CSSProperties}>
      <label>
        <strong>Marketing-Startseite</strong>
        <span>Responsive Web · Desktop 1280</span>
        <code>v14</code>
      </label>
      <div className="web-frame">
        <div className="browser">
          <i />
          <i />
          <i />
          <span>fluss.example.de</span>
        </div>
        <nav>
          <b>f　Fluss</b>
          <span>Konto</span>
          <span>Karten</span>
          <span>Sparen</span>
          <span>Hilfe</span>
          <em />
          <span>Anmelden</span>
          <button>Konto eröffnen</button>
        </nav>
        <main>
          <div>
            <h1>Banking, das dich atmen lässt.</h1>
            <p>Konto, Karten und Tagesüberblick · ohne Lärm, ohne Kleingedrucktes, in drei Minuten eröffnet.</p>
            <Button variant="primary">Jetzt starten</Button> <Button>App ansehen</Button>
            <div className="stats">
              <b>
                240k<small>Konten</small>
              </b>
              <b>
                4,8 ★<small>App-Bewertung</small>
              </b>
              <b>
                0 €<small>Kontoführung</small>
              </b>
            </div>
          </div>
          <div className="preview">
            <code>app-screenshot · dashboard</code>
          </div>
        </main>
      </div>
    </div>
  );
}
function Files({ projectId, imported }: { projectId: string; imported: ProjectImport | undefined }) {
  const files = ["onboarding.screen", "dashboard.screen", "balance-card.cmp", "tx-row.cmp", "tabbar.cmp", "tokens.css", "typography.css", "logo.svg", "karten-visual.png"];
  const [selected, setSelected] = useState(files[1]!);
  if (imported?.imported) return <ImportedFiles projectId={projectId} imported={imported} />;
  return (
    <div className="files-view">
      <aside>
        <div className="search">
          <Search />
          <input placeholder="Dateien durchsuchen …" />
        </div>
        {files.map((x) => (
          <button className={selected === x ? "active" : ""} onClick={() => setSelected(x)} key={x}>
            <FileText />
            {x}
          </button>
        ))}
      </aside>
      <main>
        <Card title={selected}>
          <code>fluss/{selected}</code>
          <div className="preview">
            <span>preview: {selected}</span>
          </div>
          <div className="modal-actions">
            <Status kind="warning">Geändert</Status>
            <span />
            <Button>Diff ansehen</Button>
            <Button>Herunterladen</Button>
            <Button variant="primary">Auf Leinwand öffnen</Button>
          </div>
        </Card>
      </main>
    </div>
  );
}
function ImportedFiles({ projectId, imported }: { projectId: string; imported: Extract<ProjectImport, { imported: true }> }) {
  const client = useQueryClient();
  const editable = imported.files.filter((file) => file.mime.startsWith("text/") || file.mime.startsWith("application/json") || file.mime.startsWith("image/svg+xml"));
  const [selected, setSelected] = useState(editable[0]?.path ?? "");
  const source = useQuery({ queryKey: ["import-file", projectId, selected, imported.revision], queryFn: () => api<{ path: string; content: string; revision: number }>(`/projects/${projectId}/import/file?path=${encodeURIComponent(selected)}`), enabled: Boolean(selected) });
  const [content, setContent] = useState("");
  useEffect(() => { if (source.data) setContent(source.data.content); }, [source.data]);
  const save = useMutation({ mutationFn: () => api<{ revision: number }>(`/projects/${projectId}/import/file`, { method: "PUT", body: JSON.stringify({ path: selected, content, baseRevision: imported.revision }) }), onSuccess: async () => { await client.invalidateQueries({ queryKey: ["project-import", projectId] }); await client.invalidateQueries({ queryKey: ["import-file", projectId] }); } });
  const setEntry = useMutation({ mutationFn: (path: string) => api<{ entryPath: string }>(`/projects/${projectId}/import/entry`, { method: "PATCH", body: JSON.stringify({ path }) }), onSuccess: () => client.invalidateQueries({ queryKey: ["project-import", projectId] }) });
  const selectedIsHtml = /\.html?$/i.test(selected);
  return <div className="files-view imported-files"><aside><div className="search"><Search/><input placeholder="Dateien durchsuchen …" readOnly/></div>{imported.files.map((file) => { const canEdit=editable.some((item)=>item.path===file.path); const isEntry=file.path===imported.entryPath; return <button className={selected===file.path?"active":""} disabled={!canEdit} title={isEntry?"Aktuelle Startseite":canEdit?"Bearbeiten":`${file.mime} bleibt unverändert erhalten`} onClick={()=>setSelected(file.path)} key={file.path}><FileText/>{file.path}{isEntry?" ★":""}</button> })}</aside><main><Card title={selected || "Keine Textdatei"}>{source.isLoading?<div className="empty">Datei wird geladen …</div>:source.isError?<p className="field-error">Die Datei konnte nicht geladen werden.</p>:selected?<textarea className="source-editor" value={content} spellCheck={false} onChange={(event)=>setContent(event.target.value)}/>:<div className="empty">Dieses Projekt enthält keine direkt bearbeitbare Textdatei.</div>}<div className="modal-actions"><Status kind={content!==source.data?.content?"warning":"success"}>{content!==source.data?.content?"Ungespeichert":"Gespeichert"}</Status>{selectedIsHtml&&selected===imported.entryPath&&<Status kind="info">Startseite der Vorschau</Status>}<span/>{selectedIsHtml&&selected!==imported.entryPath&&<Button disabled={setEntry.isPending} onClick={()=>setEntry.mutate(selected)}>{setEntry.isPending?"Wird gesetzt …":"Als Startseite verwenden"}</Button>}<Button variant="primary" disabled={!selected||content===source.data?.content||save.isPending} onClick={()=>save.mutate()}>{save.isPending?"Speichert …":"Speichern & Vorschau aktualisieren"}</Button></div>{(save.error||setEntry.error)&&<p className="field-error">{(save.error||setEntry.error) instanceof ApiError?(save.error||setEntry.error)?.message:"Aktion fehlgeschlagen."}</p>}</Card></main></div>;
}
function Questions() {
  const qs = ["Welche Aufgabe steht im Mittelpunkt?", "Welche Stimmung soll die Oberfläche vermitteln?", "Brauchst du von Beginn an einen Dunkelmodus?", "Wie viele Screens im ersten Wurf?"];
  return (
    <div className="questions">
      <main>
        {qs.map((q, i) => (
          <Card title={q} sub="Diese Antwort steuert Struktur, Token und Plattformmapping." key={q}>
            <div className="chips">
              {[i === 0 ? "Tagesüberblick" : "Ruhig & vertrauensvoll", "Entscheide für mich", "Später"].map((x) => (
                <button key={x}>{x}</button>
              ))}
            </div>
          </Card>
        ))}
      </main>
      <aside>
        <Card title="Zusammenfassung">
          <p>Unbeantwortete Fragen werden als KI-Annahme gekennzeichnet.</p>
        </Card>
        <Card title="Erzeugt wird">
          <strong>2 Screens · iOS · High Fidelity</strong>
          <Button variant="primary">Generierung starten</Button>
        </Card>
      </aside>
    </div>
  );
}
function Variants({ onAdopt }: { onAdopt(a: string, r: number, d: boolean): void }) {
  return (
    <div className="variants">
      <h2>Designrichtungen im Vergleich</h2>
      <div>
        {[
          ["Ruhig Blau", "#3157D5", 16, false],
          ["Tiefgrün Klar", "#0C7A5B", 7, false],
          ["Nacht Violett", "#7C3AED", 16, true],
        ].map(([name, a, r, d]) => (
          <Card title={name as string} key={name as string}>
            <div className="variant-preview" style={{ background: d ? "#141B25" : "#F3F5F8" }}>
              <i style={{ background: a as string, borderRadius: `${r}px` }} />
            </div>
            <Button variant="primary" onClick={() => onAdopt(a as string, r as number, d as boolean)}>
              Übernehmen
            </Button>
            <Button>Duplizieren</Button>
          </Card>
        ))}
      </div>
    </div>
  );
}
function KeyboardModal({ onClose }: { onClose(): void }) {
  return (
    <Modal title="Leinwand bedienen" width="460px" onClose={onClose}>
      <div className="modal-body key-list">
        {[
          ["Vergrößern / verkleinern", "Mausrad"],
          ["Design verschieben", "Mittlere Maustaste ziehen"],
          ["Wieder vollständig einpassen", "Doppelklick daneben"],
          ["Einen Schritt zurück", "Rücktaste"],
          ["Zum Startbildschirm", "Pos 1"],
          ["Bearbeitungsleiste", "1"],
          ["Übersicht", "?"],
        ].map(([x, y]) => (
          <div key={x}>
            <span>{x}</span>
            <code>{y}</code>
          </div>
        ))}
      </div>
    </Modal>
  );
}
function ShareModal({ onClose }: { onClose(): void }) {
  const [on, setOn] = useState(true);
  return (
    <Modal title="„Fluss · Mobile Banking“ teilen" onClose={onClose}>
      <div className="modal-body">
        <div className="invite">
          <input placeholder="Person oder Gruppe einladen …" />
          <Button variant="primary">Einladen</Button>
        </div>
        {["Frank K. · Verwalten", "Miriam T. · Kommentieren", "Jonas R. · Anzeigen"].map((x) => (
          <div className="member" key={x}>
            <b>{x.slice(0, 2)}</b>
            <span>{x}</span>
            <select>
              <option>Verwalten</option>
              <option>Bearbeiten</option>
              <option>Kommentieren</option>
              <option>Anzeigen</option>
            </select>
          </div>
        ))}
        <div className="setting-row">
          <span>
            <strong>Link-Freigabe</strong>
            <small>Jede Person mit Link kann kommentieren</small>
          </span>
          <button className={`switch ${on ? "on" : ""}`} onClick={() => setOn(!on)}>
            <i />
          </button>
        </div>
        {on && (
          <>
            <input value="https://werft.app/s/fluss-8kq2" readOnly />
            <div className="check-row">
              <label>
                <input type="checkbox" /> Passwort
              </label>
              <label>
                Ablauf{" "}
                <select>
                  <option>30 Tage</option>
                  <option>7 Tage</option>
                  <option>Nie</option>
                </select>
              </label>
              <label>
                <input type="checkbox" defaultChecked /> Download erlauben
              </label>
            </div>
          </>
        )}
      </div>
      <div className="modal-actions">
        <span />
        <Button variant="primary" onClick={onClose}>
          Fertig
        </Button>
      </div>
    </Modal>
  );
}
function ExportModal({ onClose }: { onClose(): void }) {
  const [format, setFormat] = useState("Projekt-ZIP");
  const [running, setRunning] = useState(false);
  const formats = ["Projekt-ZIP", "Eigenständiges HTML", "PDF", "PPTX", "PNG / SVG", "Design-Spezifikation", "Entwicklerübergabe"];
  return (
    <Modal title="Exportieren" width="720px" onClose={onClose}>
      <div className="export-body">
        <div>
          {formats.map((x) => (
            <button className={format === x ? "active" : ""} onClick={() => setFormat(x)} key={x}>
              <FileArchive />
              <span>
                <strong>{x}</strong>
                <small>Versionierte Ausgabe mit Qualitätsbericht</small>
              </span>
            </button>
          ))}
        </div>
        <aside>
          <Card title="Zusammenfassung">
            <p>
              Version v14 · 5 Frames
              <br />
              Format: {format}
              <br />
              Fonts eingebettet · Links aktiv
            </p>
          </Card>
          {format === "Entwicklerübergabe" && (
            <Card title="Übergabe">
              <select>
                <option>React + Tailwind</option>
                <option>SwiftUI</option>
                <option>Jetpack Compose</option>
                <option>WinUI 3</option>
              </select>
              <code>Baue Onboarding und Dashboard aus dem versionierten Paket.</code>
            </Card>
          )}
        </aside>
      </div>
      <div className="modal-actions">
        {running && <Status kind="info">Export läuft · du kannst weiterarbeiten</Status>}
        <span />
        <Button onClick={onClose}>Schließen</Button>
        <Button variant="primary" onClick={() => setRunning(true)}>
          Export starten
        </Button>
      </div>
    </Modal>
  );
}
function Present({ accent, onClose }: { accent: string; onClose(): void }) {
  const [step, setStep] = useState(0);
  return (
    <div className="presentation">
      <div style={{ transform: "scale(.88)" }}>
        <PhoneFrame name={step === 0 ? "Onboarding" : "Dashboard"} accent={accent} radius={18} dark={false} onboarding={step === 0} />
      </div>
      <nav>
        <button onClick={() => setStep(Math.max(0, step - 1))}>‹ Zurück</button>
        <button onClick={() => setStep(0)}>Neustart</button>
        <span>iPhone 15 · {step + 1} / 2</span>
        <button onClick={() => setStep(1)}>Weiter ›</button>
        <button onClick={onClose}>Beenden ×</button>
      </nav>
    </div>
  );
}

export function App() {
  const { theme } = useUi();
  useEffect(() => {
    document.documentElement.dataset.theme = theme;
  }, [theme]);
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/app/designs" />} />
      <Route path="/onboarding" element={<Onboarding />} />
      <Route path="/app/designs" element={<Hub />} />
      <Route path="/app/examples" element={<Examples />} />
      <Route path="/app/design-systems" element={<DesignSystems />} />
      <Route path="/app/design-systems/:systemId/versions/:versionId/review" element={<DsReview />} />
      <Route path="/app/settings/:section" element={<SettingsPage />} />
      <Route path="/app/projects/:projectId/studio/:tab" element={<Studio />} />
      <Route path="*" element={<Navigate to="/app/designs" />} />
    </Routes>
  );
}

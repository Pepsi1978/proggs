import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Archive, ArrowLeft, Box, Check, ChevronLeft, CircleUserRound, Code2, Download, FileArchive, FileText, FolderOpen, Grid2X2, History, Image, LayoutDashboard, MessageSquare, Minus, Moon, MoreHorizontal, MousePointer2, Palette, PanelLeft, PanelRight, PenLine, Play, Plus, RotateCcw, Search, Settings, Share2, Sparkles, Sun, Upload, Users, WandSparkles, X, ZoomIn, ZoomOut } from "lucide-react";
import { type FormEvent, type ReactNode, useEffect, useState } from "react";
import { Link, Navigate, NavLink, Route, Routes, useLocation, useNavigate, useParams } from "react-router-dom";
import { api, apiForm, ApiError } from "./api";
import { type ToolMode, useUi } from "./store";

type Project = { id: string; name: string; type: string; fidelity: string; platforms: string[]; activeVersion: number; updatedAt: string };
type Me = { id: string; email: string; name: string; role: string; organizationName: string };
type ImportFile = { path: string; size: number; mime: string };
type ProjectImport = { imported: false } | { imported: true; entryPath: string; fileCount: number; totalBytes: number; revision: number; files: ImportFile[]; previewPath?: string };
const labels: Record<string, string> = { prototype: "Prototyp", presentation: "Präsentation", document: "Dokument", template: "Vorlage", canvas: "Freie Fläche", web: "Web", android: "Android", ios: "iOS", ipados: "iPadOS", macos: "macOS", windows: "Windows" };
const examples = [
  ["Banking App „Fluss“", "iOS · Android", "Konten, Zahlungen und Tagesüberblick mit ruhiger Typografie.", "Prototyp"],
  ["CRM „Atlas“", "Responsive Web", "Pipeline-Board, Kontakte und Berichte für Vertriebsteams.", "Prototyp"],
  ["Bestellterminal", "Windows", "Kiosk-Flow mit großen Touchzielen und Fluent-Mustern.", "Prototyp"],
  ["Produkt-Pitch", "Präsentation", "12 Folien mit Sprechernotizen und Diagramm-Layouts.", "Präsentation"],
  ["Reiseplaner", "Android", "Onboarding, Suche und Buchung nach Material 3.", "Prototyp"],
  ["Preisliste 2026", "Dokument", "A4-Dokument mit Tabellen und Druckansicht.", "Dokument"],
];

function Button({ children, variant = "secondary", className = "", ...props }: { children: ReactNode; variant?: "primary" | "secondary" | "ghost" | "danger"; className?: string } & React.ButtonHTMLAttributes<HTMLButtonElement>) {
  return (
    <button className={`button ${variant} ${className}`} {...props}>
      {children}
    </button>
  );
}
function IconButton({ label, children, ...props }: { label: string; children: ReactNode } & React.ButtonHTMLAttributes<HTMLButtonElement>) {
  return (
    <button className="icon-button" aria-label={label} title={label} {...props}>
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
function Modal({ title, children, width = "540px", onClose }: { title: string; children: ReactNode; width?: string; onClose(): void }) {
  return (
    <div className="modal-backdrop" onMouseDown={onClose}>
      <section className="modal" style={{ maxWidth: width }} onMouseDown={(e) => e.stopPropagation()} role="dialog" aria-modal="true" aria-label={title}>
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
      <NavLink to="/app/settings/personal" title="Einstellungen">
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
                <div className="preview">
                  <span>preview: {project.name.toLowerCase()}</span>
                </div>
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

function ImportProject({ onClose }: { onClose(): void }) {
  const nav = useNavigate();
  const client = useQueryClient();
  const [name, setName] = useState("Importiertes Design");
  const [files, setFiles] = useState<File[]>([]);
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
    mutationFn: () => {
      const form = new FormData();
      form.append("name", name);
      for (const file of files) form.append("files", file, file.webkitRelativePath || file.name);
      return apiForm<{ projectId: string }>("/imports", form);
    },
    onSuccess: async ({ projectId }) => {
      await client.invalidateQueries({ queryKey: ["projects"] });
      onClose();
      nav(`/app/projects/${projectId}/studio/canvas`);
    },
  });
  return (
    <Modal title="Design importieren" width="680px" onClose={onClose}>
      <div className="modal-body import-dialog">
        <p className="subtle">Importiert vollständige Claude-Designs-Projekte mit HTML, CSS, JavaScript, Bildern, Fonts, Videos und Tönen. Die Verzeichnisstruktur und Interaktionen bleiben erhalten.</p>
        <label>
          Projektname
          <input value={name} maxLength={120} onChange={(event) => setName(event.target.value)} />
        </label>
        <div className="import-choices">
          <label className="import-choice">
            <FolderOpen />
            <strong>Projektordner wählen</strong>
            <span>Empfohlen für Claude Designs mit HTML und Begleitdateien</span>
            <input type="file" multiple ref={(node) => node?.setAttribute("webkitdirectory", "")} onChange={(event) => selectFiles(event.target.files)} />
          </label>
          <label className="import-choice">
            <FileArchive />
            <strong>ZIP oder Designdatei wählen</strong>
            <span>ZIP, HTML, Werft-JSON oder mehrere zusammengehörige Dateien</span>
            <input type="file" multiple accept=".zip,.html,.htm,.json,.werft,.css,.js,.svg,.png,.jpg,.jpeg,.webp,.gif,.pdf,.mp3,.wav,.ogg,.m4a,.mp4,.webm,.woff,.woff2,.ttf,.otf" onChange={(event) => selectFiles(event.target.files)} />
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
        <p className="subtle">Maximal 5.000 Dateien, 100 MB pro Datei und 300 MB insgesamt. ZIP-Pfade werden vor dem Entpacken geprüft.</p>
        {upload.error && <p className="field-error">{upload.error instanceof ApiError ? upload.error.message : "Das Projekt konnte nicht importiert werden."}</p>}
      </div>
      <div className="modal-actions">
        <Button onClick={onClose}>Abbrechen</Button>
        <Button variant="primary" disabled={!files.length || !name.trim() || upload.isPending} onClick={() => upload.mutate()}>
          {upload.isPending ? "Projekt wird importiert …" : "Importieren & öffnen"}
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
  const [platforms, setPlatforms] = useState(["ios"]);
  const [prompt, setPrompt] = useState("");
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
  return (
    <div className="back-page">
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
  const location = useLocation();
  const page = location.pathname.split("/").at(-1) ?? "personal";
  const { theme, setTheme } = useUi();
  return (
    <BackPage title="Einstellungen" onBack={() => nav("/app/designs")}>
      <div className="settings-layout">
        <nav>
          {[
            ["personal", "Persönlich"],
            ["models", "Modelle & Provider"],
          ].map(([id, label]) => (
            <NavLink key={id} to={`/app/settings/${id}`}>
              {label}
            </NavLink>
          ))}
        </nav>
        <div className="settings-content">
          {page === "personal" && (
            <>
              <Card title="Profil">
                <div className="form-grid">
                  <label>
                    Name
                    <input defaultValue="Frank K." />
                  </label>
                  <label>
                    E-Mail
                    <input value="frank@example.de" readOnly />
                  </label>
                  <label>
                    Sprache
                    <select>
                      <option>Deutsch</option>
                      <option>English</option>
                    </select>
                  </label>
                  <label>
                    Zeitzone
                    <select>
                      <option>Europe/Berlin</option>
                      <option>UTC</option>
                    </select>
                  </label>
                </div>
              </Card>
              <Card title="Erscheinung">
                <div className="setting-row">
                  <span>Modus</span>
                  <div className="segments">
                    <button className={theme === "light" ? "active" : ""} onClick={() => setTheme("light")}>
                      Hell
                    </button>
                    <button className={theme === "dark" ? "active" : ""} onClick={() => setTheme("dark")}>
                      Dunkel
                    </button>
                    <button>System</button>
                  </div>
                </div>
                <div className="setting-row">
                  <span>Dichte</span>
                  <div className="segments">
                    <button className="active">Komfortabel</button>
                    <button>Kompakt</button>
                  </div>
                </div>
                <div className="setting-row">
                  <span>Reduzierte Bewegung</span>
                  <button className="switch">
                    <i />
                  </button>
                </div>
              </Card>
              <Card title="KI-Standard">
                <div className="setting-row">
                  <span>Modellprofil</span>
                  <select>
                    <option>Standard</option>
                    <option>Sparsam</option>
                    <option>Höchste Qualität</option>
                    <option>Lokal</option>
                  </select>
                </div>
                <div className="setting-row">
                  <span>Budgetwarnung ab</span>
                  <input defaultValue="25 € / Monat" />
                </div>
              </Card>
            </>
          )}
          {page === "models" && <ProviderSettings />}
        </div>
      </div>
    </BackPage>
  );
}
function ProviderSettings() {
  const [open, setOpen] = useState(false);
  const [device, setDevice] = useState<{ authId: string; userCode: string; verificationUri: string; expiresAt: string; interval: number }>();
  const client = useQueryClient();
  const connection = useQuery({ queryKey: ["provider", "openai"], queryFn: () => api<{ connected: boolean; status: string; email?: string; accountId?: string; settings?: { model?: string; effort?: string; fast?: boolean } }>("/providers/openai") });
  const [model,setModel]=useState("gpt-5.6-sol"),[effort,setEffort]=useState("high"),[fast,setFast]=useState(false);
  const start = useMutation({ mutationFn: () => api<{ authId: string; userCode: string; verificationUri: string; expiresAt: string; interval: number }>("/providers/openai/auth/start", { method: "POST", body: "{}" }), onSuccess: (data) => { setDevice(data); window.open(data.verificationUri, "_blank", "noopener,noreferrer"); } });
  const poll = useMutation({ mutationFn: (authId: string) => api<{ status: string; connected: boolean; interval?: number }>("/providers/openai/auth/poll", { method: "POST", body: JSON.stringify({ authId }) }), onSuccess: async (data) => { if (data.connected) { setDevice(undefined); setOpen(false); await client.invalidateQueries({ queryKey: ["provider", "openai"] }); } else if (data.status === "expired") setDevice(undefined); else if (data.interval) setDevice((current) => (current ? { ...current, interval: Math.max(3, Math.min(30, data.interval!)) } : current)); } });
  const disconnect = useMutation({ mutationFn: () => api<{ connected: boolean }>("/providers/openai", { method: "DELETE" }), onSuccess: () => client.invalidateQueries({ queryKey: ["provider", "openai"] }) });
  const saveSettings=useMutation({mutationFn:()=>api("/providers/openai/settings",{method:"PATCH",body:JSON.stringify({model,effort,fast})}),onSuccess:()=>client.invalidateQueries({queryKey:["provider","openai"]})});
  const testProvider=useMutation({mutationFn:async()=>{await api("/providers/openai/settings",{method:"PATCH",body:JSON.stringify({model,effort,fast})});return api<{ok:boolean;elapsedMs:number}>("/providers/openai/test",{method:"POST",body:"{}"})}});
  useEffect(() => { if (!device || poll.isPending) return; const timer=setTimeout(()=>poll.mutate(device.authId),device.interval*1000); return()=>clearTimeout(timer); },[device,poll.isPending,poll.mutate]);
  useEffect(()=>{const settings=connection.data?.settings;if(settings?.model)setModel(settings.model);if(settings?.effort)setEffort(settings.effort);if(typeof settings?.fast==="boolean")setFast(settings.fast)},[connection.data?.settings]);
  return (
    <>
      <div className="section-head">
        <p className="subtle">Verbindungen sind anbieterneutral. Credentials bleiben ausschließlich auf dem Server.</p>
        <Button variant="primary" onClick={() => setOpen(true)}>
          Verbindung hinzufügen
        </Button>
      </div>
      <div className="provider-grid">
        <Card title="OpenAI · Codex OAuth">
          <small>ChatGPT-Anmeldung · Tokens nur verschlüsselt auf dem Server</small>
          <Status kind={connection.data?.connected ? "success" : "neutral"}>{connection.data?.connected ? "Verbunden" : "Nicht verbunden"}</Status>
          {connection.data?.connected && <small>{connection.data.email || connection.data.accountId || "OpenAI-Konto"}</small>}
          <div className="caps"><span>GPT-5.6</span><span>Reasoning</span><span>Tools</span></div>
          {connection.data?.connected ? <Button variant="danger" onClick={()=>disconnect.mutate()}>Verbindung trennen</Button> : <Button onClick={()=>setOpen(true)}>Mit OpenAI verbinden</Button>}
        </Card>
        {[
          ["Werft Intern", "Eigenes Modell · EU-Region", "Verbunden", "success"],
          ["Lokal · Ollama", "Selbst gehostet", "Ungültig", "error"],
        ].map(([name, kind, status, tone]) => (
          <Card title={name!} key={name}>
            <small>{kind}</small>
            <Status kind={tone as "success" | "warning" | "error"}>{status}</Status>
            <div className="caps">
              <span>Text</span>
              <span>Reasoning</span>
              <span>Tools</span>
            </div>
            <Button>Erneut testen</Button>
          </Card>
        ))}
      </div>
      {connection.data?.connected&&<Card title="GPT-5.6 Modell & Thinking" sub="Gilt für Planung, Design, Code und visuelle Prüfung über deine Codex-Verbindung."><div className="form-grid model-settings"><label>Modell<select value={model} onChange={(event)=>setModel(event.target.value)}><option value="gpt-5.6-sol">GPT-5.6 Sol</option><option value="gpt-5.6-terra">GPT-5.6 Terra</option><option value="gpt-5.6-luna">GPT-5.6 Luna</option></select></label><label>Thinking / Effort<select value={effort} onChange={(event)=>setEffort(event.target.value)}><option value="none">None</option><option value="low">Low</option><option value="medium">Medium</option><option value="high">High</option><option value="xhigh">Extra High</option></select></label></div><div className="setting-row"><span><strong>Fast / Priority</strong><small>Verwendet dasselbe Modell mit service_tier=priority.</small></span><button className={`switch ${fast?"on":""}`} onClick={()=>setFast(!fast)}><i/></button></div><div className="modal-actions">{saveSettings.isSuccess&&<Status kind="success">Gespeichert</Status>}{testProvider.isSuccess&&<Status kind="success">Live getestet · {testProvider.data.elapsedMs} ms</Status>}{(saveSettings.error||testProvider.error)&&<p className="field-error">{(saveSettings.error||testProvider.error) instanceof ApiError?(saveSettings.error||testProvider.error)?.message:"Modellkonfiguration fehlgeschlagen."}</p>}<span/><Button disabled={testProvider.isPending} onClick={()=>testProvider.mutate()}>{testProvider.isPending?"Test läuft …":"Verbindung testen"}</Button><Button variant="primary" disabled={saveSettings.isPending} onClick={()=>saveSettings.mutate()}>Modellwahl speichern</Button></div></Card>}
      <Card title="Routing-Matrix" sub="Welches Modell übernimmt welche Aufgabe.">
        {["Planung", "Design", "Code", "Visuelle Prüfung", "Bildgenerierung"].map((x) => (
          <div className="routing-row" key={x}>
            <strong>{x}</strong>
            <select>
              <option>Werft Intern</option>
              {connection.data?.connected&&<option>{model.replace("gpt-5.6-","GPT-5.6 ")}{fast?" Fast":""} · {effort}</option>}
              <option>Lokal · Ollama</option>
            </select>
          </div>
        ))}
      </Card>
      {open && (
        <Modal title="OpenAI mit Codex verbinden" onClose={() => {setOpen(false);setDevice(undefined)}}>
          <div className="modal-body oauth-dialog">
            {!device ? <><p>Werft öffnet die offizielle OpenAI-Geräteseite. Dein Passwort wird niemals an Werft übermittelt.</p><Button variant="primary" disabled={start.isPending} onClick={()=>start.mutate()}>{start.isPending?"Code wird angefordert …":"Browser-Code anfordern"}</Button></> : <><p>Gib diesen Code auf der OpenAI-Seite ein (Klick kopiert ihn):</p><button className="device-code" onClick={()=>navigator.clipboard.writeText(device.userCode)} title="Code kopieren">{device.userCode}</button><a className="button secondary" href={device.verificationUri} target="_blank" rel="noreferrer">OpenAI-Seite öffnen · auth.openai.com/codex/device</a><Status kind="info">Warte auf Bestätigung im Browser …</Status><small>Der Code läuft um {new Date(device.expiresAt).toLocaleTimeString("de-DE",{hour:"2-digit",minute:"2-digit"})} Uhr ab.</small></>}
            {(start.error||poll.error)&&<p className="field-error">{(start.error||poll.error) instanceof ApiError?(start.error||poll.error)?.message:"OpenAI-Anmeldung fehlgeschlagen."}</p>}
          </div>
          <div className="modal-actions">
            <Button onClick={() => {setOpen(false);setDevice(undefined)}}>Schließen</Button>
          </div>
        </Modal>
      )}
    </>
  );
}
function Studio() {
  const { projectId = "" } = useParams();
  const nav = useNavigate();
  const location = useLocation();
  const client = useQueryClient();
  const tab = location.pathname.split("/").at(-1) ?? "canvas";
  const { theme, leftOpen, rightOpen, toggleLeft, toggleRight, mode, setMode, zoom, setZoom, setTheme, setModal, modal } = useUi();
  const project = useQuery({
    queryKey: ["project", projectId],
    queryFn: () => api<Project>(`/projects/${projectId}`),
  });
  const design = useQuery({
    queryKey: ["design", projectId],
    queryFn: () => api<{ revision: number; document: unknown }>(`/projects/${projectId}/design-document`),
  });
  const imported = useQuery({
    queryKey: ["project-import", projectId],
    queryFn: () => api<ProjectImport>(`/projects/${projectId}/import`),
  });
  const [accent, setAccent] = useState("#3157D5");
  const [radius, setRadius] = useState(18);
  const [darkPreview, setDarkPreview] = useState(false);
  const [chat, setChat] = useState("");
  const [messages, setMessages] = useState<Array<{ role: "user" | "assistant"; text: string }>>([]);
  const connection = useQuery({ queryKey: ["provider", "openai"], queryFn: () => api<{ connected: boolean; settings?: { model?: string; fast?: boolean } }>("/providers/openai") });
  const chatRun = useMutation({
    mutationFn: (message: string) => api<{ reply: string; changedFiles: string[]; skipped?: string[]; revision: number }>(`/projects/${projectId}/chat`, { method: "POST", body: JSON.stringify({ message }) }),
    onSuccess: async (data) => {
      const parts = [data.reply];
      if (data.changedFiles.length) parts.push(`✓ Geänderte Dateien: ${data.changedFiles.join(", ")}`);
      if (data.skipped?.length) parts.push(`⚠ Nicht angewendet:\n${data.skipped.join("\n")}`);
      setMessages((all) => [...all, { role: "assistant", text: parts.join("\n\n") }]);
      await client.invalidateQueries({ queryKey: ["project-import", projectId] });
      await client.invalidateQueries({ queryKey: ["import-file", projectId] });
    },
    onError: (error) => setMessages((all) => [...all, { role: "assistant", text: error instanceof ApiError ? `Fehler: ${error.message}` : "Der KI-Lauf ist fehlgeschlagen. Bitte erneut versuchen." }])
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
  if (project.isError) return <Navigate to="/app/designs" />;
  return (
    <div className="studio" data-theme={theme}>
      <header className="project-header">
        <IconButton label="Zum Hub" onClick={() => nav("/app/designs")}>
          <ArrowLeft />
        </IconButton>
        <strong>{project.data?.name ?? "Projekt wird geladen …"}</strong>
        <Status kind="success">Gespeichert</Status>
        <span className="platform-chip">{project.data?.platforms.map((p) => labels[p]).join(" · ")}</span>
        <code>v{project.data?.activeVersion ?? 1}</code>
        <span />
        <IconButton label="Gesprächspanel" onClick={toggleLeft}>
          <PanelLeft />
        </IconButton>
        <IconButton label="Inspektor" onClick={toggleRight}>
          <PanelRight />
        </IconButton>
        <IconButton label="Theme" onClick={() => setTheme(theme === "dark" ? "light" : "dark")}>
          {theme === "dark" ? <Sun /> : <Moon />}
        </IconButton>
        <IconButton label="Projekt als ZIP herunterladen" onClick={() => { if (imported.data?.imported) window.open(`/api/v1/projects/${projectId}/export.zip`, "_blank"); else setModal("export"); }}>
          <Download />
        </IconButton>
        <Button onClick={() => setModal("present")}>Präsentieren</Button>
        <Button variant="primary" onClick={() => setModal("share")}>
          Teilen
        </Button>
        <b className="avatar">FK</b>
      </header>
      <div className="studio-body">
        {leftOpen && (
          <aside className="left-panel">
            <div className="panel-tabs">
              <button className="active">Gespräch</button>
              <button>Kommentare</button>
              <button>Verlauf</button>
            </div>
            <div className="messages">
              {messages.length === 0 && !chatRun.isPending && <div className="empty">Noch keine Nachrichten. Beschreibe unten eine Änderung am Design.</div>}
              {messages.map((m, i) => (
                <div className={m.role === "user" ? "user-message" : "assistant-message"} key={i}>
                  {m.text}
                </div>
              ))}
              {chatRun.isPending && <div className="assistant-message">Die KI arbeitet am Design … das kann je nach Modell ein bis zwei Minuten dauern.</div>}
            </div>
            <form
              className="composer"
              onSubmit={(e) => {
                e.preventDefault();
                const message = chat.trim();
                if (!message || chatRun.isPending) return;
                setMessages((all) => [...all, { role: "user", text: message }]);
                setChat("");
                chatRun.mutate(message);
              }}
            >
              <textarea value={chat} onChange={(e) => setChat(e.target.value)} placeholder="Beschreibe eine Änderung …" />
              <footer>
                <span>Modell: {connection.data?.connected ? `${connection.data.settings?.model ?? "GPT-5.6"}${connection.data.settings?.fast ? " · Fast" : ""}` : "kein Provider verbunden"}</span>
                <Button variant="primary" disabled={chatRun.isPending || !chat.trim()}>{chatRun.isPending ? "KI-Lauf läuft …" : "Senden"}</Button>
              </footer>
            </form>
          </aside>
        )}
        <section className="workspace">
          <nav className="workspace-tabs">
            {[
              ["canvas", "Leinwand"],
              ["files", "Design Files"],
              ["variants", "Varianten"],
              ["questions", "Rückfragen"],
            ].map(([id, label]) => (
              <NavLink key={id} to={`/app/projects/${projectId}/studio/${id}`}>
                {label}
              </NavLink>
            ))}
          </nav>
          {tab === "canvas" && <Canvas projectId={projectId} accent={accent} radius={radius} dark={darkPreview} zoom={zoom} setZoom={setZoom} mode={mode} setMode={setMode} imported={imported.data} />} {tab === "files" && <Files projectId={projectId} imported={imported.data} />}
          {tab === "questions" && <Questions />}
          {tab === "variants" && (
            <Variants
              onAdopt={(a, r, d) => {
                setAccent(a);
                setRadius(r);
                setDarkPreview(d);
                nav(`/app/projects/${projectId}/studio/canvas`);
              }}
            />
          )}
        </section>
        {rightOpen && (
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
      <footer className="statusbar">
        <span>Zoom {Math.round(zoom * 100)} %</span>
        <code>{project.data?.name ?? "Projekt"}</code>
        <span />
        <Status kind={chatRun.isPending ? "info" : "success"}>{chatRun.isPending ? "KI-Lauf: läuft …" : "KI-Lauf: bereit"}</Status>
        <span>Vorschau aktuell</span>
        <button onClick={() => setModal("keys")}>?</button>
      </footer>
      {modal === "keys" && <KeyboardModal onClose={() => setModal(null)} />} {modal === "share" && <ShareModal onClose={() => setModal(null)} />} {modal === "export" && <ExportModal onClose={() => setModal(null)} />} {modal === "present" && <Present accent={accent} onClose={() => setModal(null)} />}
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
function Canvas({ projectId, accent, radius, dark, zoom, setZoom, mode, setMode, imported }: { projectId: string; accent: string; radius: number; dark: boolean; zoom: number; setZoom(v: number): void; mode: ToolMode; setMode(v: ToolMode): void; imported: ProjectImport | undefined }) {
  const client = useQueryClient();
  const reconstruct = useMutation({
    mutationFn: () => api<{ entryPath: string; revision: number }>(`/projects/${projectId}/design/reconstruct`, { method: "POST", body: "{}" }),
    onSuccess: () => client.invalidateQueries({ queryKey: ["project-import", projectId] })
  });
  if (imported?.imported) {
    const previewOrigin = `${window.location.protocol}//${window.location.hostname}:8444`;
    const looksLikeDesktopApp = imported.files.some((file) => /\.(xaml|csproj|sln|kt|swift)$/i.test(file.path));
    return (
      <div className="canvas imported-canvas">
        <div className="canvas-toolbar imported-toolbar">
          <Status kind={imported.previewPath ? "success" : "warning"}>{imported.previewPath ? "Original importiert" : "Kein Web-Frontend erkannt"}</Status>
          <span>{imported.entryPath || "Keine Startseite festgelegt"}</span>
          <small>
            {imported.fileCount} Dateien · {(imported.totalBytes / 1024 / 1024).toLocaleString("de-DE", { maximumFractionDigits: 1 })} MB
          </small>
        </div>
        {imported.previewPath ? (
          <iframe title="Interaktive importierte Designvorschau" src={`${previewOrigin}${imported.previewPath}`} sandbox="allow-scripts allow-same-origin allow-forms allow-modals allow-popups allow-downloads allow-pointer-lock" allow="autoplay; fullscreen" />
        ) : (
          <div className="empty">
            <strong>Dieses Projekt enthält keine eindeutige Web-Startseite.</strong>
            <span>
              {looksLikeDesktopApp
                ? "Es sieht nach einer Desktop- oder Mobil-App aus (z. B. WPF/XAML, Kotlin oder Swift) — deren Oberfläche kann der Browser nicht direkt darstellen. Alle Dateien wurden trotzdem vollständig importiert."
                : "Es wurde keine geeignete HTML-Startseite gefunden. Alle Dateien wurden trotzdem vollständig importiert."}
            </span>
            <span>Unter „Design Files“ kannst du jede HTML-Datei öffnen und „Als Startseite verwenden“ wählen — oder lass die KI das Design direkt aus dem App-Quellcode nachbauen:</span>
            <Button variant="primary" disabled={reconstruct.isPending} onClick={() => reconstruct.mutate()}>
              {reconstruct.isPending ? "KI rekonstruiert das Design … (bis zu einigen Minuten)" : "Design mit KI aus dem App-Code rekonstruieren"}
            </Button>
            {reconstruct.error && <p className="field-error">{reconstruct.error instanceof ApiError ? reconstruct.error.message : "Die Rekonstruktion ist fehlgeschlagen. Bitte erneut versuchen."}</p>}
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
        <span>iPhone 15 · 390</span>
        {tools.map(([id, label, icon]) => (
          <button className={mode === id ? "active" : ""} onClick={() => setMode(id)} key={id}>
            {icon}
            {label}
          </button>
        ))}
        <IconButton label="Verkleinern" onClick={() => setZoom(zoom - 0.1)}>
          <ZoomOut />
        </IconButton>
        <b>{Math.round(zoom * 100)} %</b>
        <IconButton label="Vergrößern" onClick={() => setZoom(zoom + 0.1)}>
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
      <div className="frame-scroller">
        <div className="frames" style={{ transform: `scale(${zoom})` }}>
          <PhoneFrame name="Onboarding" accent={accent} radius={radius} dark={dark} onboarding />
          <PhoneFrame name="Dashboard" accent={accent} radius={radius} dark={dark} />
          <AndroidFrame accent={accent} dark={dark} />
          <WindowsFrame accent={accent} dark={dark} />
          <WebFrame accent={accent} dark={dark} />
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
function AndroidFrame({ accent, dark }: { accent: string; dark: boolean }) {
  return (
    <div className={`frame-wrap ${dark ? "preview-dark" : ""}`} style={{ "--preview-accent": accent } as React.CSSProperties}>
      <label>
        <strong>Dashboard</strong>
        <span>Android · Pixel 9 · Standard</span>
        <code>v14</code>
      </label>
      <div className="android-frame">
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
    <Modal title="Tastaturkürzel" width="440px" onClose={onClose}>
      <div className="modal-body key-list">
        {[
          ["Interagieren / Auswählen", "V · S"],
          ["Kommentar / Bearbeiten / Zeichnen", "C · E · D"],
          ["Zoom größer / kleiner", "+ · −"],
          ["Zoom 100 %", "0"],
          ["Gesprächspanel", "1"],
          ["Inspektor", "2"],
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

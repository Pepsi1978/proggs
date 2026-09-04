import { readFile, writeFile } from 'node:fs/promises';
import { join } from 'node:path';
const source = process.argv[2];
if (!source) throw new Error('Quellverzeichnis fehlt');
const metadata = JSON.parse(await readFile(new URL('package.json', import.meta.url), 'utf8'));
const upstreamVersion = process.argv[3] ?? '0.153.2';
if (!/^\d+\.\d+\.\d+(?:-[\w.-]+)?$/.test(upstreamVersion)) throw new Error('Ungültige Codex-Version');
async function replace(file, before, after) {
  const path = join(source, 'codex-rs', file);
  const text = (await readFile(path, 'utf8')).replaceAll('\r\n', '\n');
  if (text.includes(after)) return;
  if (text.split(before).length !== 2) throw new Error(`Unpassender Codex-Quellstand: ${file}`);
  await writeFile(path, text.replace(before, after));
}
await replace('tui/src/chatwidget.rs', 'mod status_surfaces;', 'mod status_surfaces;\nmod local_cost;');
await replace('tui/src/chatwidget.rs', '    token_info: Option<TokenUsageInfo>,',
  '    local_cost: local_cost::LocalCost,\n    token_info: Option<TokenUsageInfo>,');
await replace('tui/src/chatwidget/constructor.rs', '            token_info: None,',
  '            local_cost: super::local_cost::LocalCost::default(),\n            token_info: None,');
await replace('tui/src/chatwidget.rs', '        self.refresh_status_line_if_workspace_headline_due();',
  `        self.refresh_status_line_if_workspace_headline_due();
        if self.local_cost.refresh_due()
            && self.configured_status_line_items().iter().any(|item| item == "estimated-thread-cost")
        {
            self.refresh_status_line();
        }`);
await replace('tui/src/chatwidget/status_surfaces.rs',
  `            StatusLineItem::EstimatedThreadCost => self
                .estimated_thread_usage()
                .and_then(|usage| usage.estimated_usage_usd_micros)
                .and_then(format_estimated_usd_micros),`,
  `            StatusLineItem::EstimatedThreadCost => {
                self.frame_requester.schedule_frame_in(Duration::from_secs(2));
                Some(self.local_cost.display(
                    self.current_rollout_path.as_deref(),
                    &self.config.codex_home,
                ))
            },`);
const modulePath = join(source, 'codex-rs/tui/src/chatwidget/local_cost.rs');
const moduleText = (await readFile(new URL('local_cost.rs', import.meta.url), 'utf8')).replaceAll('\r\n', '\n');
const installedModule = await readFile(modulePath, 'utf8').catch(() => '');
if (installedModule.replaceAll('\r\n', '\n') !== moduleText) await writeFile(modulePath, moduleText);
const manifest = join(source, 'codex-rs/Cargo.toml');
const cargo = (await readFile(manifest, 'utf8')).replaceAll('\r\n', '\n');
const workspace = cargo.match(/\[workspace\.package\]([\s\S]*?)(?=\n\[|$)/);
const version = workspace?.[1].match(/^version = "([^"]+)"$/m)?.[1];
if (!version || version.replace(/-cost\..*$/, '') !== upstreamVersion) {
  throw new Error('Die Codex-Quellen passen nicht zur installierten CLI-Version.');
}
const updated = cargo.replace(workspace[0], workspace[0].replace(
  /^version = "[^"]+"$/m, `version = "${upstreamVersion}-cost.${metadata.version}"`));
if (updated !== cargo) await writeFile(manifest, updated);

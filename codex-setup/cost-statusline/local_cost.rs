//! Local API-equivalent cost estimate, using the OpenCode sidebar price export.
//! Rollout replay preserves the model/tier of each request across resume and compaction.
use serde_json::Value;
use std::fs::File;
use std::io::{BufRead, BufReader, Seek, SeekFrom};
use std::path::{Path, PathBuf};
use std::time::{Duration, Instant};

#[derive(Default)]
pub(super) struct LocalCost {
    path: Option<PathBuf>,
    offset: u64,
    prices: Value,
    model: String,
    provider: String,
    tier: String,
    previous: Option<Value>,
    usd: f64,
    incomplete: bool,
    refreshed_at: Option<Instant>,
}

fn count(value: &Value, name: &str) -> f64 {
    value[name].as_f64().unwrap_or(0.0).max(0.0)
}

impl LocalCost {
    pub(super) fn refresh_due(&self) -> bool {
        self.refreshed_at.is_none_or(|time| time.elapsed() >= Duration::from_secs(2))
    }

    pub(super) fn display(&mut self, path: Option<&Path>, codex_home: &Path) -> String {
        self.refreshed_at = Some(Instant::now());
        let Some(path) = path else {
            return "Kosten …".into();
        };
        if self.path.as_deref() != Some(path) {
            *self = Self {
                path: Some(path.to_path_buf()),
                prices: std::fs::read(codex_home.join("cost-prices.json"))
                    .ok()
                    .and_then(|bytes| serde_json::from_slice(&bytes).ok())
                    .unwrap_or_default(),
                provider: "openai".into(),
                tier: "default".into(),
                refreshed_at: Some(Instant::now()),
                ..Self::default()
            };
        }
        let Ok(file) = File::open(path) else {
            return "Kosten n/v".into();
        };
        let Ok(metadata) = file.metadata() else {
            return "Kosten n/v".into();
        };
        if metadata.len() < self.offset {
            // A truncated history cannot justify a smaller displayed session total.
            self.incomplete = true;
            return format!("Kosten ≥${:.2}", self.usd);
        }
        let mut reader = BufReader::new(file);
        if reader.seek(SeekFrom::Start(self.offset)).is_err() {
            return "Kosten n/v".into();
        }
        let start = self.offset;
        let mut line = Vec::new();
        while self.offset < metadata.len() && self.offset - start < 8 * 1024 * 1024 {
            line.clear();
            match reader.read_until(b'\n', &mut line) {
                Ok(0) | Err(_) => break,
                Ok(_) if line.last() != Some(&b'\n') => break,
                Ok(bytes) => self.offset += bytes as u64,
            }
            match serde_json::from_slice::<Value>(&line) {
                Ok(event) => self.observe(&event),
                Err(_) => self.incomplete = true,
            }
        }
        if self.offset < metadata.len() {
            return format!("Kosten ≈${:.2} …", self.usd);
        }
        if self.prices["models"].is_null() {
            return "Kosten n/v (Preise)".into();
        }
        if self.incomplete {
            format!("Kosten ≥${:.2} (teilw.)", self.usd)
        } else {
            format!("Kosten ≈${:.2}", self.usd)
        }
    }

    fn observe(&mut self, event: &Value) {
        let payload = &event["payload"];
        match event["type"].as_str() {
            Some("session_meta") => {
                if let Some(provider) = payload["model_provider"].as_str() {
                    self.provider = provider.into();
                }
            }
            Some("turn_context") => {
                if let Some(model) = payload["model"].as_str() {
                    self.model = model.into();
                }
            }
            Some("event_msg") => match payload["type"].as_str() {
                Some("thread_settings_applied") => {
                    let settings = &payload["thread_settings"];
                    if let Some(model) = settings["model"].as_str() {
                        self.model = model.into();
                    }
                    if let Some(provider) = settings["model_provider_id"].as_str() {
                        self.provider = provider.into();
                    }
                    self.tier = settings["service_tier"]
                        .as_str()
                        .unwrap_or("default")
                        .into();
                }
                Some("token_count") => self.observe_usage(&payload["info"]),
                _ => {}
            },
            _ => {}
        }
    }

    fn observe_usage(&mut self, info: &Value) {
        let total = &info["total_token_usage"];
        let usage = &info["last_token_usage"];
        if !total.is_object() || !usage.is_object() {
            return;
        }
        if let Some(previous) = &self.previous {
            // Quota updates repeat the preceding token event. Compaction can lower totals.
            if count(total, "total_tokens") <= count(previous, "total_tokens") {
                self.previous = Some(total.clone());
                return;
            }
        } else if count(total, "total_tokens") > count(usage, "total_tokens") {
            self.incomplete = true;
        }
        self.previous = Some(total.clone());
        if let Some(usd) = self.request_cost(usage) {
            self.usd += usd;
        } else {
            self.incomplete = true;
        }
    }

    fn request_cost(&self, usage: &Value) -> Option<f64> {
        if self.provider != "openai" {
            return None;
        }
        let model = self.model.strip_suffix("-fast").unwrap_or(&self.model);
        let tier = if self.model.ends_with("-fast") {
            "priority"
        } else {
            &self.tier
        };
        let base = &self.prices["models"][model][tier];
        if !base.is_object() {
            return None;
        }
        let input = count(usage, "input_tokens");
        if base["unsupportedAbove"]
            .as_f64()
            .is_some_and(|limit| input > limit)
        {
            return None;
        }
        let context_tier = base["tiers"].as_array().and_then(|tiers| {
            tiers
                .iter()
                .filter(|entry| {
                    entry["tier"]["type"] == "context" && input > count(&entry["tier"], "size")
                })
                .max_by(|a, b| count(&a["tier"], "size").total_cmp(&count(&b["tier"], "size")))
        });
        let price = |key: &str| -> Option<f64> {
            context_tier
                .and_then(|entry| entry[key].as_f64())
                .or_else(|| base[key].as_f64())
                .filter(|value| value.is_finite() && *value >= 0.0)
        };
        let read = count(usage, "cached_input_tokens");
        let write = count(usage, "cache_write_input_tokens");
        let output = count(usage, "output_tokens");
        let reasoning = count(usage, "reasoning_output_tokens");
        if read + write > input || reasoning > output {
            return None;
        }
        // Codex totals INCLUDE caches and reasoning; OpenCode's price math receives disjoint buckets.
        let buckets = [
            (input - read - write, price("input")),
            (read, price("cache_read")),
            (write, price("cache_write")),
            (output - reasoning, price("output")),
            (reasoning, price("reasoning").or_else(|| price("output"))),
        ];
        let mut usd = 0.0;
        for (tokens, rate) in buckets {
            if tokens > 0.0 {
                usd += tokens * rate? / 1_000_000.0;
            }
        }
        Some(usd)
    }
}

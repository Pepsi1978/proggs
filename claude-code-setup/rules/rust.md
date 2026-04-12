---
paths:
  - "**/*.rs"
  - "**/Cargo.toml"
  - "**/Cargo.lock"
---

# Rust Development Rules

- Use Rust 2024 edition, latest stable features
- Prefer `thiserror` for library errors, `anyhow` for application errors
- Use `clap` for CLI argument parsing
- Build release: `cargo build --release`
- Cross-compile for Windows: `cargo build --release --target x86_64-pc-windows-gnu` (MinGW linker configured in ~/.cargo/config.toml)
- Run clippy before committing: `cargo clippy -- -D warnings`
- Format code: `cargo fmt`
- Run tests: `cargo nextest run` (faster parallel test runner, falls back to `cargo test`)
- Watch mode for development: `cargo watch -x check -x test` (auto-rebuild on file changes)
- Avoid `unsafe` unless performance-critical and well-documented
- Use `serde` for serialization, `tokio` for async runtime
- Security audit: `cargo audit` before release (checks crates for known CVEs)
- Policy check: `cargo deny check` (licenses, advisories, bans — configure via deny.toml)

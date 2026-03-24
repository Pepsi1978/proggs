---
paths:
  - "**/*"
---

# Termux/Android Platform Rules

## Environment Constraints
- Platform: Android/Termux on aarch64 (no root, no sudo, no systemd)
- Package manager: `pkg` (not apt, brew, or winget)
- Home: /data/data/com.termux/files/home
- TMPDIR: /data/data/com.termux/files/usr/tmp (not /tmp)
- No Docker, no VM, no Swift, no .NET, no Xcode

## Tool Availability Overrides
- **Biome**: NOT available on ARM64-Android — use `npx prettier` instead for JS/TS formatting
- **Bun**: NOT available on Android — use `node`/`npm`/`npx` instead
- **rustup**: Use `pkg install rust` (rustup has host-triple issues on android)
- **golangci-lint**: Use `go vet ./...` as fallback if not installed

## Shebang Rule (CRITICAL)
- NEVER use `#!/usr/bin/env` — it doesn't exist on Termux
- ALWAYS use `#!/data/data/com.termux/files/usr/bin/bash` (or node, python3)
- When installing npm global packages: check if shebang needs patching

## Disk Space Awareness
- Device storage is limited — always check `df -h ~` before large operations
- Prefer lightweight tools over heavy ones
- Clean caches regularly: `npm cache clean --force && apt clean`

## Notifications
- Use `termux-notification` instead of desktop notification systems
- Use `termux-toast` for quick non-persistent messages
- Use `termux-vibrate` for haptic feedback

---
name: trailofbits-scan-apk
description: Scans Android APKs for Firebase security misconfigurations
---

## Arguments
<apk-file-or-directory>

## Allowed Tools
- Bash
- Read
- Grep
- Glob

# Scan APK for Firebase Misconfigurations

**Arguments:** $ARGUMENTS

Parse the APK path from arguments. If empty, ask for the path.

Invoke the `firebase-apk-scanner` skill with the APK path for the full workflow.

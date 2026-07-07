---
name: trailofbits-burp-search
description: Searches Burp Suite project files for security analysis
---

## Arguments
<burp-file> [operation]

## Allowed Tools
- Bash
- Read

# Search Burp Suite Project Files

**Arguments:** $ARGUMENTS

Parse arguments:
1. **Burp file** (required): Path to .burp project file
2. **Operation** (optional): `auditItems`, `proxyHistory.*`, `responseHeader='...'`, `responseBody='...'`

Invoke the `burpsuite-project-parser` skill with these arguments for the full workflow.

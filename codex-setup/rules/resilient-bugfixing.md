# Resilient Bugfixing for Codex Setup

Directive 3 for Codex environment work:

1. Identify the symptom.
2. Ask "Warum?" at least three times to find the root cause.
3. Check related surfaces:
   - same error class
   - same component
   - same dependency
4. Implement a durable fix with defense in depth.
5. Run an 8-point future-failure review before considering the fix complete.

Required qualities:
- defensive
- update-resistant
- platform-aware
- documented
- non-regressive

Never ship a one-off environment patch when the underlying class can be prevented.

---
name: synced-plugins-abschalten
description: Von claude.ai synchronisierte Plugins werden per enabledPlugins "name@synced": false abgeschaltet
metadata:
  type: reference
---

Die Plugins unter `minimal/plugins/synced/` (Marktplatz knowledge-work-plugins) kommen vom claude.ai-Konto. In den Settings schaltet `"enabledPlugins": {"<name>@synced": false}` sie ab. Der Schlüssel steht so im claude.exe-Code, nicht `@knowledge-work-plugins`. Am 21.09.2026 wurden 17 ungenutzte Plugins in allen sechs Profilen (Windows ClaudeCode + ClaudeCodeMac, je minimal/standard/strict) abgeschaltet. Solche Änderungen immer in allen Profilen machen. Aktiv bleiben pdf-viewer und productivity. Siehe [[aktiver-config-ordner-openlauncher-profil]].

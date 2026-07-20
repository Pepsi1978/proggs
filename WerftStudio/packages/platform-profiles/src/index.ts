export const platformProfiles = {
  web: { devices: ["Mobile 390", "Tablet 768", "Desktop 1280"], minTouch: 44, navigation: ["header", "sidebar", "tabs"] },
  android: { devices: ["Pixel 9 390x760"], minTouch: 48, navigation: ["top-app-bar", "bottom-navigation", "navigation-rail"] },
  ios: { devices: ["iPhone 15 390x760"], minTouch: 44, navigation: ["navigation-stack", "tab-bar", "sheet"] },
  ipados: { devices: ["iPad 1024x1366"], minTouch: 44, navigation: ["split-view", "sidebar", "toolbar"] },
  macos: { devices: ["Window 1280x800"], minTouch: 28, navigation: ["sidebar", "toolbar", "menu"] },
  windows: { devices: ["Window 900x580"], minTouch: 32, navigation: ["navigation-view", "command-bar", "info-bar"] }
} as const;

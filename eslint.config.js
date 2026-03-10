import globals from "globals";
import js from "@eslint/js";

export default [
  {
    files: ["Tampermonkey/**/*.user.js"],
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: "script",
      globals: {
        ...globals.browser,
        ...globals.greasemonkey,
        // Tampermonkey API
        GM_getValue: "readonly",
        GM_setValue: "readonly",
        GM_xmlhttpRequest: "readonly",
        GM_addStyle: "readonly",
        GM_registerMenuCommand: "readonly",
        GM_notification: "readonly",
        GM_setClipboard: "readonly",
        GM_info: "readonly",
        unsafeWindow: "readonly",
      },
    },
    rules: {
      // Fehler die Probleme verursachen
      "no-undef": "error",
      "no-unused-vars": ["warn", { "argsIgnorePattern": "^_" }],
      "no-redeclare": "error",
      "no-dupe-keys": "error",
      "no-duplicate-case": "error",
      "no-unreachable": "error",
      "no-constant-condition": "warn",
      "no-empty": "warn",
      "no-extra-semi": "warn",
      "use-isnan": "error",
      "valid-typeof": "error",

      // Deaktiviert - zu streng fuer Tampermonkey-Skripte
      "no-eval": "off",
      "no-implied-eval": "off",
      "strict": "off",
    },
  },
];

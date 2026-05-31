// Service worker for Prompt Board.
// Enables the native click-to-toggle behavior: a click on the toolbar icon
// opens the side panel, a second click closes it. Chrome handles the toggle
// itself once openPanelOnActionClick is true.

function enableToggle() {
	chrome.sidePanel
		.setPanelBehavior({ openPanelOnActionClick: true })
		.catch((err) =>
			console.error("Prompt Board: setPanelBehavior failed", err),
		);
}

// Run on install/update and on every service-worker startup.
chrome.runtime.onInstalled.addListener(enableToggle);
enableToggle();

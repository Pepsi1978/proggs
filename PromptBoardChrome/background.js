// Service worker for Prompt Board.
// 1. Enables the native click-to-toggle behavior: a click on the toolbar icon
//    opens the side panel, a second click closes it. Chrome handles the toggle
//    itself once openPanelOnActionClick is true.
// 2. Completes the side panel's "refresh" action: the panel stores the active
//    tab id and calls chrome.runtime.reload(); that tears down the panel, so the
//    freshly started service worker reloads that page here on startup.

function enableToggle() {
	chrome.sidePanel
		.setPanelBehavior({ openPanelOnActionClick: true })
		.catch((err) =>
			console.error("Prompt Board: setPanelBehavior failed", err),
		);
}

function finishPendingReload() {
	chrome.storage.local.get("pendingReloadTabId", (res) => {
		const tabId = res && res.pendingReloadTabId;
		if (typeof tabId === "number") {
			chrome.storage.local.remove("pendingReloadTabId");
			chrome.tabs.reload(tabId).catch(() => {
				/* tab may have been closed in the meantime */
			});
		}
	});
}

// Run on install/update and on every service-worker startup.
chrome.runtime.onInstalled.addListener(enableToggle);
enableToggle();
finishPendingReload();

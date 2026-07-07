// Service worker for Prompt Board.
// 1. Enables the native click-to-toggle behavior for the side panel.
// 2. Completes the side panel's "refresh" action (reload extension + page).
// 3. Injects the content script into tabs that were ALREADY OPEN when the
//    extension loaded/updated — Chrome only auto-injects manifest content
//    scripts into tabs loaded afterwards, so without this the user had to
//    reload each page once before insertion worked.

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

// Inject content.js into all currently open http/https tabs. The content script
// has a load guard, so re-injecting an already-covered tab is a harmless no-op.
async function injectIntoOpenTabs() {
	try {
		const tabs = await chrome.tabs.query({
			url: ["http://*/*", "https://*/*"],
		});
		for (const tab of tabs) {
			if (typeof tab.id !== "number") continue;
			chrome.scripting
				.executeScript({
					target: { tabId: tab.id, allFrames: true },
					files: ["content.js"],
				})
				.catch(() => {
					/* some pages reject injection (PDF viewer, blocked origins) — ignore */
				});
		}
	} catch (_) {
		/* tabs.query can fail on restricted profiles — ignore */
	}
}

chrome.runtime.onInstalled.addListener(() => {
	enableToggle();
	injectIntoOpenTabs();
});
chrome.runtime.onStartup.addListener(injectIntoOpenTabs);

// Runs on every service-worker startup (covers the refresh-button reload too).
enableToggle();
finishPendingReload();
injectIntoOpenTabs();

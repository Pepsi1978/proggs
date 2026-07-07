const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const { google } = require("googleapis");

const PLAY_SERVICE_ACCOUNT_JSON = defineSecret("PLAY_SERVICE_ACCOUNT_JSON");

const PACKAGE_NAME = "com.bestjournal.app";
const LIFETIME_PRODUCT_ID = "bestjournal_lifetime";

exports.getSubscriptionStatus = onCall(
	{
		secrets: [PLAY_SERVICE_ACCOUNT_JSON],
		// App Check disabled until Play Integrity is fully wired up in Firebase
		// Console (SHA-256 fingerprints, app registration). The function only
		// returns subscription metadata for a given purchaseToken — leakage
		// risk is minimal (someone would need to know your token already).
		enforceAppCheck: false,
		region: "europe-west1",
		timeoutSeconds: 30,
		memory: "256MiB",
		cpu: 1,
	},
	async (request) => {
		const purchaseToken = request.data?.purchaseToken;
		const productId = request.data?.productId;

		if (!purchaseToken || typeof purchaseToken !== "string") {
			throw new HttpsError(
				"invalid-argument",
				"purchaseToken is required and must be a string",
			);
		}
		if (!productId || typeof productId !== "string") {
			throw new HttpsError(
				"invalid-argument",
				"productId is required and must be a string",
			);
		}

		let credentials;
		try {
			credentials = JSON.parse(PLAY_SERVICE_ACCOUNT_JSON.value());
		} catch (err) {
			console.error("Failed to parse service account JSON:", err.message);
			throw new HttpsError("internal", "Service account configuration error");
		}

		const auth = new google.auth.GoogleAuth({
			credentials,
			scopes: ["https://www.googleapis.com/auth/androidpublisher"],
		});

		const androidpublisher = google.androidpublisher({ version: "v3", auth });

		// Loop-12 fix (Frank, 2026-05-01): the Lifetime in-app product is
		// queried via the Products API, not the Subscriptions API. The
		// previous code only spoke to subscriptionsv2.get, so a Lifetime
		// refund/revocation could not be detected server-side and the
		// app's BillingClient cache kept the user marked as Subscribed.
		if (productId === LIFETIME_PRODUCT_ID) {
			try {
				const response = await androidpublisher.purchases.products.get({
					packageName: PACKAGE_NAME,
					productId: productId,
					token: purchaseToken,
				});
				const product = response.data;
				// purchaseState: 0 = PURCHASED, 1 = CANCELLED, 2 = PENDING.
				// A revoked/refunded Lifetime returns purchaseState=1 or
				// 404. Both must downgrade the app to Free.
				if (product.purchaseState !== 0) {
					console.log(
						`Lifetime purchaseState=${product.purchaseState} (not PURCHASED) ` +
							`— treating as NotFound`,
					);
					throw new HttpsError(
						"not-found",
						"Lifetime purchase no longer active",
					);
				}
				return {
					subscriptionState: "LIFETIME_ACTIVE",
					basePlanId: null,
					offerId: null,
					productId: productId,
					expiryTime: null, // Lifetime never expires.
					autoRenewing: false, // Lifetime is one-time, never renews.
					latestOrderId: product.orderId ?? null,
					offerPhase: null,
					currentPriceMicros: null,
					currentPriceCurrency: null,
				};
			} catch (err) {
				if (err instanceof HttpsError) throw err;
				const status = err?.response?.status;
				const message = err?.response?.data?.error?.message || err.message;
				console.error(
					`Products API error (status=${status}):`,
					message,
					"purchaseToken=",
					purchaseToken.substring(0, 12) + "...",
				);
				if (status === 404 || status === 410) {
					throw new HttpsError(
						"not-found",
						"Lifetime purchase not found or refunded",
					);
				}
				if (status === 401 || status === 403) {
					throw new HttpsError(
						"permission-denied",
						"Service account lacks permission to read product purchase",
					);
				}
				throw new HttpsError(
					"internal",
					`Failed to fetch lifetime purchase: ${message}`,
				);
			}
		}

		try {
			const response = await androidpublisher.purchases.subscriptionsv2.get({
				packageName: PACKAGE_NAME,
				token: purchaseToken,
			});

			const sub = response.data;
			const lineItem = Array.isArray(sub.lineItems) ? sub.lineItems[0] : null;
			const offerDetails = lineItem?.offerDetails;
			// CRITICAL FIX: Google's recurringPrice on the autoRenewingPlan is
			// the AUTHORITATIVE current price after the intro phase ends. The
			// BillingClient on Android only knows the offer's pricingPhases
			// list (intro + recurring) and cannot tell which phase is active —
			// so the app needs us to deliver the truth here.
			const recurring = lineItem?.autoRenewingPlan?.recurringPrice;
			const currentPriceMicros =
				recurring?.units != null
					? Number(recurring.units) * 1_000_000 + (recurring.nanos ?? 0) / 1000
					: null;

			// Industry-standard renewal detection. Google issues a NEW order
			// id on every successful renewal — comparing this against a stored
			// value tells the app exactly when a renewal happened.
			const latestOrderId =
				lineItem?.latestSuccessfulOrderId ?? lineItem?.latestOrderId ?? null;

			// Loop-7 fix #2 (live-discovered 2026-04-30 with Frank): Google's
			// Subscriptions API v2 has neither a `lineItem.offerPhase` field
			// (the legacy code's wishful guess) nor a reliable signal that
			// changes when the intro cycles are over — `offerDetails.offerId`
			// stays populated for the entire lifetime of an offer-based plan,
			// even after the discount has elapsed. The actual phase therefore
			// has to be derived in the client by comparing the
			// recurringPrice we return here with the base-plan list price the
			// BillingClient already knows. We pass `offerPhase: null` so the
			// client computes it locally; offerId is forwarded for diagnosis.
			const offerId = offerDetails?.offerId ?? null;
			const offerPhase = null;

			// Loop-7 fix #3 (live-discovered 2026-04-30 with Frank): the
			// previous code derived autoRenewing from "is the autoRenewingPlan
			// object present at all". That object stays populated even after
			// the user cancels in the Play Store — Google only flips
			// `autoRenewEnabled` to false. Reading that flag directly fixes
			// the in-app dialog still announcing "Verlängert sich am X" for
			// hours after a real cancellation.
			const autoRenewEnabled =
				lineItem?.autoRenewingPlan?.autoRenewEnabled === true;

			return {
				subscriptionState: sub.subscriptionState ?? null,
				basePlanId: offerDetails?.basePlanId ?? null,
				offerId: offerDetails?.offerId ?? null,
				productId: lineItem?.productId ?? null,
				expiryTime: lineItem?.expiryTime ?? null,
				autoRenewing: autoRenewEnabled,
				latestOrderId: latestOrderId,
				offerPhase: offerPhase,
				// Authoritative current price (after intro phase if any).
				currentPriceMicros: currentPriceMicros,
				currentPriceCurrency: recurring?.currencyCode ?? null,
			};
		} catch (err) {
			const status = err?.response?.status;
			const message = err?.response?.data?.error?.message || err.message;
			console.error(
				`Subscription API error (status=${status}):`,
				message,
				"purchaseToken=",
				purchaseToken.substring(0, 12) + "...",
			);
			if (status === 404 || status === 410) {
				throw new HttpsError("not-found", "Subscription not found or expired");
			}
			if (status === 401 || status === 403) {
				throw new HttpsError(
					"permission-denied",
					"Service account lacks permission to read subscription",
				);
			}
			throw new HttpsError(
				"internal",
				`Failed to fetch subscription: ${message}`,
			);
		}
	},
);

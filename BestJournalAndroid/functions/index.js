const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const { google } = require("googleapis");

const PLAY_SERVICE_ACCOUNT_JSON = defineSecret("PLAY_SERVICE_ACCOUNT_JSON");

const PACKAGE_NAME = "com.bestjournal.app";

exports.getSubscriptionStatus = onCall(
	{
		secrets: [PLAY_SERVICE_ACCOUNT_JSON],
		enforceAppCheck: true,
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

		try {
			const response = await androidpublisher.purchases.subscriptionsv2.get({
				packageName: PACKAGE_NAME,
				token: purchaseToken,
			});

			const sub = response.data;
			const lineItem = Array.isArray(sub.lineItems) ? sub.lineItems[0] : null;
			const offerDetails = lineItem?.offerDetails;

			return {
				subscriptionState: sub.subscriptionState ?? null,
				basePlanId: offerDetails?.basePlanId ?? null,
				offerId: offerDetails?.offerId ?? null,
				productId: lineItem?.productId ?? null,
				expiryTime: lineItem?.expiryTime ?? null,
				autoRenewing: lineItem?.autoRenewingPlan != null,
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

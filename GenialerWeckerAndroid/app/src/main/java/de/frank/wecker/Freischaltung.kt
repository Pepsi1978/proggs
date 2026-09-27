package de.frank.wecker

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class FreischaltungsZustand(
    val test: Testzeitraum.Stand,
    val gekauft: Boolean,
    /** Preis wie ihn Google Play liefert (inkl. Währung); null = noch nicht geladen oder nicht verfügbar. */
    val preis: String? = null,
    val playBereit: Boolean = false,
    val hinweis: String = "",
    val ausstehend: Boolean = false,
) {
    /** Neue Wecker anlegen, bearbeiten und einschalten. Klingeln, Schlummern und Stopp sind nie gesperrt. */
    val darfBearbeiten: Boolean get() = gekauft || test.aktiv
}

/**
 * Sieben Tage Test und einmaliger Google-Play-Kauf (lebenslang). Kein Server und keine fingierte
 * Bestätigung: freigeschaltet ist nur ein Kauf im Zustand PURCHASED, der bestätigt (acknowledged) ist.
 * Der Kauf-Cache gilt offline weiter und wird nur widerrufen, wenn Google Play erfolgreich antwortet
 * und keinen Kauf mehr kennt – nie bei „Play nicht erreichbar“ (z. B. außerhalb des Play Stores installiert).
 */
object Freischaltung : PurchasesUpdatedListener {
    const val PRODUKT = "genialer_wecker_lifetime"
    private const val TEST_SPEICHER = "freischaltung_test"   // im Backup enthalten (nur diese Datei)
    private const val KAUF_SPEICHER = "freischaltung_kauf"   // bewusst nicht im Backup: Play stellt den Kauf wieder her
    private const val SETUP_ZEITLIMIT_MS = 15_000L

    private lateinit var app: Context
    private var client: BillingClient? = null
    private var produkt: ProductDetails? = null
    /** Aufgaben, die auf den laufenden Verbindungsaufbau warten; verhindert ein zweites paralleles startConnection. */
    private val wartend = mutableListOf<() -> Unit>()
    private var verbindetGerade = false
    private val haupt = Handler(Looper.getMainLooper())
    private val _zustand = MutableStateFlow(FreischaltungsZustand(Testzeitraum.Stand(0, 0), gekauft = false))
    val zustand: StateFlow<FreischaltungsZustand> = _zustand.asStateFlow()

    /** Aus der Oberfläche aufrufen (Start und Rückkehr in die App); nie aus Weck- oder Boot-Pfaden. */
    fun aktualisieren(context: Context) {
        app = context.applicationContext
        val gekauft = app.getSharedPreferences(KAUF_SPEICHER, Context.MODE_PRIVATE).getBoolean("gekauft", false)
        _zustand.update { it.copy(test = testStand(), gekauft = gekauft) }
        verbinden { kaeufeAbfragen(); produktLaden() }
    }

    /** Testzeitraum neu berechnen und die höchste gesehene Zeit fortschreiben. */
    fun testStand(): Testzeitraum.Stand {
        val prefs = app.getSharedPreferences(TEST_SPEICHER, Context.MODE_PRIVATE)
        val installiert = runCatching { app.packageManager.getPackageInfo(app.packageName, 0).firstInstallTime }.getOrDefault(0L)
        val jetzt = System.currentTimeMillis()
        val anker = prefs.getLong("anker", 0L).takeIf { it > 0 }
        val stand = Testzeitraum.berechne(installiert, anker, jetzt, prefs.getLong("hoechste_zeit", 0L))
        prefs.edit().putLong("anker", stand.start).putLong("hoechste_zeit", stand.wirksameZeit).apply()
        return stand
    }

    fun pruefeTest() { if (::app.isInitialized) _zustand.update { it.copy(test = testStand()) } }

    fun kaufen(activity: Activity) {
        val details = produkt
        val c = client
        if (c == null || !c.isReady) { hinweis("Google Play ist gerade nicht erreichbar. Bitte später erneut versuchen."); verbinden { produktLaden() }; return }
        if (details == null) { hinweis("Das Produkt ist in Google Play noch nicht verfügbar. Ein Kauf ist deshalb gerade nicht möglich."); produktLaden(); return }
        val parameter = BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).build())).build()
        val ergebnis = c.launchBillingFlow(activity, parameter)
        if (ergebnis.responseCode != BillingClient.BillingResponseCode.OK) hinweis(fehlertext(ergebnis))
    }

    fun wiederherstellen() {
        hinweis("Käufe werden bei Google Play abgefragt …")
        verbinden { kaeufeAbfragen(meldeErgebnis = true) }
    }

    override fun onPurchasesUpdated(ergebnis: BillingResult, kaeufe: MutableList<Purchase>?) {
        when (ergebnis.responseCode) {
            BillingClient.BillingResponseCode.OK -> verarbeite(kaeufe.orEmpty(), vollstaendig = false, meldeErgebnis = true)
            BillingClient.BillingResponseCode.USER_CANCELED -> hinweis("Kauf abgebrochen. Es wurde nichts berechnet.")
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> kaeufeAbfragen(meldeErgebnis = true)
            else -> hinweis(fehlertext(ergebnis))
        }
    }

    private fun verbinden(danach: () -> Unit) {
        val c = client ?: BillingClient.newBuilder(app).setListener(this)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .enableAutoServiceReconnection()
            .build().also { client = it }
        if (c.isReady) { danach(); return }
        // onResume und „Kauf wiederherstellen“ können sich überschneiden: nur ein Aufbau, die Aufgaben warten.
        wartend += danach
        if (verbindetGerade) return
        verbindetGerade = true
        var fertig = false
        // Eigenes Zeitlimit (Bug 141): hängt der Aufbau, gibt es eine ehrliche Meldung statt ewigen Wartens.
        haupt.postDelayed({ if (!fertig) { fertig = true; verbindetGerade = false; wartend.clear(); _zustand.update { it.copy(playBereit = false, hinweis = "Google Play antwortet nicht. Starte bei Bedarf den Play Store neu und versuch es erneut.") } } }, SETUP_ZEITLIMIT_MS)
        c.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(ergebnis: BillingResult) {
                haupt.post {
                    if (fertig && ergebnis.responseCode != BillingClient.BillingResponseCode.OK) return@post
                    fertig = true
                    verbindetGerade = false
                    val aufgaben = wartend.toList(); wartend.clear()
                    if (ergebnis.responseCode == BillingClient.BillingResponseCode.OK) {
                        _zustand.update { it.copy(playBereit = true) }
                        aufgaben.forEach { it() }
                    } else _zustand.update { it.copy(playBereit = false, hinweis = fehlertext(ergebnis)) }
                }
            }
            // Kein eigener Reconnect hier (Bug 3); die Bibliothek verbindet selbst neu, jede Aktion prüft isReady.
            override fun onBillingServiceDisconnected() { haupt.post { _zustand.update { it.copy(playBereit = false) } } }
        })
    }

    private fun produktLaden() {
        val c = client?.takeIf { it.isReady } ?: return
        val parameter = QueryProductDetailsParams.newBuilder().setProductList(listOf(
            QueryProductDetailsParams.Product.newBuilder().setProductId(PRODUKT).setProductType(BillingClient.ProductType.INAPP).build())).build()
        c.queryProductDetailsAsync(parameter) { ergebnis, antwort ->
            val details = antwort.productDetailsList.firstOrNull { it.productId == PRODUKT }
            haupt.post {
                produkt = details
                _zustand.update { it.copy(preis = details?.oneTimePurchaseOfferDetails?.formattedPrice) }
                if (ergebnis.responseCode != BillingClient.BillingResponseCode.OK && !_zustand.value.gekauft) hinweis(fehlertext(ergebnis))
            }
        }
    }

    private fun kaeufeAbfragen(meldeErgebnis: Boolean = false) {
        val c = client?.takeIf { it.isReady } ?: return
        c.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()) { ergebnis, kaeufe ->
            haupt.post {
                if (ergebnis.responseCode == BillingClient.BillingResponseCode.OK) verarbeite(kaeufe, vollstaendig = true, meldeErgebnis = meldeErgebnis)
                else if (meldeErgebnis) hinweis(fehlertext(ergebnis))
            }
        }
    }

    /** @param vollstaendig true = vollständige Liste aus queryPurchasesAsync; nur dann darf widerrufen werden. */
    private fun verarbeite(kaeufe: List<Purchase>, vollstaendig: Boolean, meldeErgebnis: Boolean) {
        val (stand, offen) = KaufLogik.bewerte(kaeufe.map {
            KaufLogik.Kauf(it.products, it.purchaseState == Purchase.PurchaseState.PURCHASED,
                it.purchaseState == Purchase.PurchaseState.PENDING, it.isAcknowledged, it.purchaseToken)
        }, PRODUKT)
        offen.forEach(::bestaetigen)
        when (stand) {
            KaufLogik.Zustand.GEKAUFT -> speichereKauf(true, if (meldeErgebnis) "Lebenslang freigeschaltet. Danke für deinen Kauf!" else "")
            KaufLogik.Zustand.BESTAETIGUNG_NOETIG -> hinweis("Kauf erhalten – die Freischaltung wird bei Google Play bestätigt …")
            KaufLogik.Zustand.AUSSTEHEND -> _zustand.update { it.copy(ausstehend = true, hinweis = "Google Play bestätigt die Zahlung noch. Die Freischaltung folgt automatisch, sobald sie abgeschlossen ist.") }
            KaufLogik.Zustand.KEINER -> if (vollstaendig) speichereKauf(false, if (meldeErgebnis) "Für dieses Google-Konto wurde kein Kauf gefunden." else "")
        }
    }

    private fun bestaetigen(token: String) {
        val c = client?.takeIf { it.isReady } ?: return
        c.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(token).build()) { ergebnis ->
            haupt.post {
                // Erst nach erfolgreicher Bestätigung dauerhaft freischalten; sonst beim nächsten Start erneut (Bug 14).
                if (ergebnis.responseCode == BillingClient.BillingResponseCode.OK) speichereKauf(true, "Lebenslang freigeschaltet. Danke für deinen Kauf!")
                else hinweis("Die Bestätigung bei Google Play ist fehlgeschlagen; sie wird beim nächsten Start wiederholt. ${fehlertext(ergebnis)}")
            }
        }
    }

    private fun speichereKauf(gekauft: Boolean, meldung: String) {
        app.getSharedPreferences(KAUF_SPEICHER, Context.MODE_PRIVATE).edit().putBoolean("gekauft", gekauft).apply()
        _zustand.update { it.copy(gekauft = gekauft, ausstehend = false, hinweis = meldung) }
    }

    private fun hinweis(text: String) = _zustand.update { it.copy(hinweis = text) }

    fun fehlertext(ergebnis: BillingResult): String = when (ergebnis.responseCode) {
        BillingClient.BillingResponseCode.BILLING_UNAVAILABLE ->
            "Kauf über Google Play ist auf diesem Gerät gerade nicht möglich – etwa weil die App nicht aus dem Play Store installiert wurde."
        BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> "Das Produkt ist in Google Play noch nicht verfügbar."
        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE, BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
        BillingClient.BillingResponseCode.NETWORK_ERROR -> "Google Play ist gerade nicht erreichbar. Bitte später erneut versuchen."
        BillingClient.BillingResponseCode.USER_CANCELED -> "Kauf abgebrochen. Es wurde nichts berechnet."
        else -> "Google Play meldet einen Fehler (${ergebnis.responseCode})."
    }
}

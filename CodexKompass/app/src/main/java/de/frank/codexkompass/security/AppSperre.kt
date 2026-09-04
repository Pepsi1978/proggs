package de.frank.codexkompass.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import de.frank.codexkompass.data.EinstellungenStore
import de.frank.codexkompass.observability.KompassLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Sperrt die App per Fingerabdruck, Gesicht oder Gerätecode (Referenz, Baustein I).
 *
 * Zwei Entscheidungen, die aus schlechten Erfahrungen stammen:
 *  - Es wird IMMER auch der Gerätecode als Rückfall zugelassen. Ein nasser Finger darf nicht
 *    dazu führen, dass man an die eigenen Daten nicht mehr herankommt.
 *  - Beim EINSCHALTEN wird einmal bestätigt. Sonst schaltet jemand die Sperre ein, ohne dass
 *    auf dem Gerät überhaupt ein Verfahren eingerichtet ist — und sperrt sich selbst aus.
 *
 * Ausgelöst wird die Sperre beim Kaltstart und wenn die App länger als die eingestellte Zeit
 * im Hintergrund war.
 */
class AppSperre(private val einstellungen: EinstellungenStore) : DefaultLifecycleObserver {

    private val _gesperrt = MutableStateFlow(einstellungen.appSperreAktiv)
    val gesperrt: StateFlow<Boolean> = _gesperrt.asStateFlow()

    private var imHintergrundSeit = 0L

    fun beobachte() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStop(owner: LifecycleOwner) {
        imHintergrundSeit = System.currentTimeMillis()
    }

    override fun onStart(owner: LifecycleOwner) {
        if (!einstellungen.appSperreAktiv) {
            _gesperrt.value = false
            return
        }
        val schwelle = einstellungen.sperreNachSekunden
        if (schwelle < 0) return // „nie" — einmal entsperrt bleibt entsperrt
        if (imHintergrundSeit == 0L) return
        val vergangen = (System.currentTimeMillis() - imHintergrundSeit) / 1_000
        if (vergangen >= schwelle) {
            KompassLog.info(
                "AppSperre",
                "onStart",
                "App wird nach Hintergrundzeit gesperrt",
                mapOf("sekunden" to vergangen, "schwelle" to schwelle),
            )
            _gesperrt.value = true
        }
    }

    /** Wird beim Start gerufen: gesperrt starten, wenn die Sperre aktiv ist. */
    fun beimKaltstart() {
        _gesperrt.value = einstellungen.appSperreAktiv
    }

    fun frageAb(
        activity: FragmentActivity,
        titel: String = "Codex Kompass entsperren",
        beiErfolg: () -> Unit,
        beiFehler: (String) -> Unit,
    ) {
        val verfuegbarkeit = pruefeVerfuegbarkeit(activity)
        if (verfuegbarkeit != null) {
            beiFehler(verfuegbarkeit)
            return
        }
        val abfrage = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(ergebnis: BiometricPrompt.AuthenticationResult) {
                    _gesperrt.value = false
                    KompassLog.info("AppSperre", "frageAb", "Entsperrt")
                    beiErfolg()
                }

                override fun onAuthenticationError(code: Int, meldung: CharSequence) {
                    // Ein Abbruch durch den Benutzer ist kein Fehler, den man ihm vorhalten muss.
                    if (code == BiometricPrompt.ERROR_USER_CANCELED ||
                        code == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        code == BiometricPrompt.ERROR_CANCELED
                    ) {
                        return
                    }
                    KompassLog.warn("AppSperre", "frageAb", "Entsperren fehlgeschlagen", mapOf("code" to code))
                    beiFehler(meldung.toString())
                }
            },
        )
        abfrage.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(titel)
                .setSubtitle("Mit Fingerabdruck, Gesicht oder Gerätecode")
                .setAllowedAuthenticators(ERLAUBTE_VERFAHREN)
                .build(),
        )
    }

    /**
     * Prüft, ob überhaupt ein Verfahren zur Verfügung steht.
     *
     * Liefert `null`, wenn alles bereit ist, sonst eine Meldung im Klartext. Genau diese
     * Prüfung verhindert, dass man die Sperre einschaltet und sich damit aussperrt.
     */
    fun pruefeVerfuegbarkeit(activity: FragmentActivity): String? {
        val manager = BiometricManager.from(activity)
        return when (manager.canAuthenticate(ERLAUBTE_VERFAHREN)) {
            BiometricManager.BIOMETRIC_SUCCESS -> null
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
            ->
                "Dieses Gerät bietet kein Verfahren zum Entsperren an. Die App-Sperre lässt " +
                    "sich deshalb nicht einschalten."
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                "Auf diesem Gerät ist noch kein Fingerabdruck, kein Gesicht und kein Code " +
                    "eingerichtet. Richte das zuerst in den Android-Einstellungen ein."
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED ->
                "Für das Entsperren fehlt ein Sicherheitsupdate von Android."
            else ->
                "Das Entsperren steht auf diesem Gerät gerade nicht zur Verfügung."
        }
    }

    /** Wird gerufen, wenn die Sperre in den Einstellungen eingeschaltet wird. */
    fun schalteEin(activity: FragmentActivity, beiErfolg: () -> Unit, beiFehler: (String) -> Unit) {
        frageAb(
            activity = activity,
            titel = "App-Sperre einschalten",
            beiErfolg = {
                einstellungen.appSperreAktiv = true
                _gesperrt.value = false
                beiErfolg()
            },
            beiFehler = beiFehler,
        )
    }

    fun schalteAus() {
        einstellungen.appSperreAktiv = false
        _gesperrt.value = false
        KompassLog.info("AppSperre", "schalteAus", "App-Sperre ausgeschaltet")
    }

    companion object {
        /**
         * Starke Biometrie ODER der Gerätecode. Der Code ist bewusst mit dabei: Ohne ihn
         * bliebe man bei einem nassen Finger vor der eigenen App stehen.
         */
        const val ERLAUBTE_VERFAHREN =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL

        /** Die Wartezeiten, die in den Einstellungen zur Wahl stehen. */
        val WARTEZEITEN = listOf(
            0 to "Sofort",
            60 to "Nach 1 Minute",
            300 to "Nach 5 Minuten",
            -1 to "Nie (nur beim Start)",
        )
    }
}

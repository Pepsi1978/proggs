package de.frank.entropyreducer.data.remote.zepp

import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-CBC-Verschluesselung fuer den Zepp-Login-Body (Schritt 1 des Logins).
 *
 * Hintergrund: Die Zepp-Cloud akzeptiert E-Mail/Passwort nur als
 * AES-CBC-verschluesselten Form-Encoded-Body — Klartext wuerde mit 400 abgelehnt.
 * Schluessel und IV sind fix in der Zepp-App eingebaut (extrahiert aus dem
 * Open-Source-Reverse-Engineering von argrento/huami-token).
 *
 * - Algorithmus: AES/CBC/PKCS5Padding
 * - Schluessel: 16 Byte ASCII "xeNtBVqzDc6tuNTh"
 * - IV: 16 Byte ASCII "MAAAYAAAAAAAAABg"
 *
 * Das ist KEIN sicherheitskritischer Crypto-Code — der Zweck ist, die App-Identitaet
 * gegenueber Zepp vorzutaeuschen. Die echte Sicherheit kommt durch HTTPS + Zepps
 * eigene Auth.
 */
internal object ZeppCrypto {
    private val KEY = "xeNtBVqzDc6tuNTh".toByteArray(Charsets.US_ASCII)
    private val IV = "MAAAYAAAAAAAAABg".toByteArray(Charsets.US_ASCII)
    private const val TRANSFORM = "AES/CBC/PKCS5Padding"

    fun encryptLoginPayload(plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(KEY, "AES"), IvParameterSpec(IV))
        return cipher.doFinal(plaintext)
    }

    /** Form-Encoded-Body bauen wie urllib.parse.urlencode in Python (doseq=True). */
    fun urlEncodeForm(params: List<Pair<String, String>>): String {
        return params.joinToString("&") { (k, v) ->
            "${urlEncode(k)}=${urlEncode(v)}"
        }
    }

    private fun urlEncode(s: String): String =
        java.net.URLEncoder.encode(s, "UTF-8")
            // urllib nutzt RFC 3986; URLEncoder kodiert Leerzeichen als "+",
            // das passt fuer application/x-www-form-urlencoded.
}

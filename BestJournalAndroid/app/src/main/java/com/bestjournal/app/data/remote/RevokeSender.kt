package com.bestjournal.app.data.remote

import android.accounts.Account
import android.content.Context
import com.bestjournal.app.R
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * M1 — Versand des § 356a BGB Widerrufs via Gmail-API.
 *
 * Der Button in den Settings loest ueber diese Klasse den Widerruf DIREKT aus der App
 * aus. Es werden zwei E-Mails versendet:
 *   1. An den Entwickler (dev.app.support@gmail.com) mit Absender = angemeldetes Google-Konto
 *   2. Eine Eingangsbestaetigung an den Nutzer selbst (an sein Google-Konto)
 *
 * Parallelstruktur zu [FeedbackSender] — gleicher OAuth-Flow (Gmail.send-Scope),
 * gleicher MIME-Aufbau, gleicher Exception-Typ ([FeedbackNeedConsentException]) wenn der
 * Nutzer die Gmail-Berechtigung noch nicht erteilt hat.
 *
 * Rechtsgrundlage: § 356a BGB (ab 19.06.2026) verlangt einen zweistufigen Widerrufsbutton
 * der den Widerruf DIREKT ausloest (nicht nur ein Mail-Programm oeffnet) und eine
 * automatische Eingangsbestaetigung an den Verbraucher versendet.
 */
object RevokeSender {

    private const val DEV_EMAIL = "dev.app.support@gmail.com"
    private const val GMAIL_SEND_URL = "https://gmail.googleapis.com/gmail/v1/users/me/messages/send"
    private const val GMAIL_SCOPE = "oauth2:https://www.googleapis.com/auth/gmail.send"

    /**
     * @return `null` on success, otherwise a human-readable error message to display.
     * @throws FeedbackNeedConsentException if the user still needs to grant Gmail permission.
     */
    suspend fun send(
        context: Context,
        accountEmail: String,
        subject: String,
        devBody: String,
        userSubject: String,
        userBody: String,
    ): String? = withContext(Dispatchers.IO) {
        val account = Account(accountEmail, "com.google")
        val token = try {
            GoogleAuthUtil.getToken(context, account, GMAIL_SCOPE)
        } catch (e: UserRecoverableAuthException) {
            throw FeedbackNeedConsentException(e.intent ?: android.content.Intent())
        } catch (e: Exception) {
            return@withContext context.getString(R.string.settings_feedback_token_error, e.message ?: "")
        }

        try {
            // 1. Widerrufs-E-Mail an den Entwickler
            val devMessage = buildRawEmail(
                from = accountEmail,
                to = DEV_EMAIL,
                subject = subject,
                body = devBody,
            )
            sendViaGmailApi(context, token, devMessage)

            // 2. Eingangsbestaetigung an den Nutzer (§ 356a BGB)
            val userMessage = buildRawEmail(
                from = accountEmail,
                to = accountEmail,
                subject = userSubject,
                body = userBody,
            )
            sendViaGmailApi(context, token, userMessage)

            null
        } catch (e: Exception) {
            e.message ?: context.getString(R.string.settings_feedback_unknown_error)
        }
    }

    private fun buildRawEmail(
        from: String,
        to: String,
        subject: String,
        body: String,
    ): String {
        val subjectEncoded =
            android.util.Base64.encodeToString(
                subject.toByteArray(Charsets.UTF_8),
                android.util.Base64.NO_WRAP,
            )
        return "From: $from\r\n" +
            "To: $to\r\n" +
            "Subject: =?UTF-8?B?$subjectEncoded?=\r\n" +
            "Content-Type: text/plain; charset=UTF-8\r\n" +
            "\r\n" +
            body
    }

    private fun sendViaGmailApi(context: Context, token: String, rawEmail: String) {
        val encoded =
            android.util.Base64.encodeToString(
                rawEmail.toByteArray(Charsets.UTF_8),
                android.util.Base64.URL_SAFE or
                    android.util.Base64.NO_PADDING or
                    android.util.Base64.NO_WRAP,
            )
        val json = """{"raw":"$encoded"}"""

        val url = URL(GMAIL_SEND_URL)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Authorization", "Bearer $token")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        conn.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(json) }

        val code = conn.responseCode
        val body =
            try {
                if (code in 200..299) conn.inputStream.bufferedReader().readText()
                else {
                    conn.errorStream?.bufferedReader()?.readText()
                        ?: context.getString(R.string.settings_feedback_no_error_body)
                }
            } catch (_: Exception) {
                context.getString(R.string.settings_feedback_read_error)
            }
        conn.disconnect()

        if (code !in 200..299) {
            throw RuntimeException(context.getString(R.string.settings_feedback_api_error, code, body))
        }
    }
}

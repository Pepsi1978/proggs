package com.bestjournal.app.data.remote

import android.accounts.Account
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class FeedbackNeedConsentException(val consentIntent: Intent) : Exception("Gmail-Zugriff muss erlaubt werden")

object FeedbackSender {

    private const val DEV_EMAIL = "dev.app.support@gmail.com"
    private const val GMAIL_SEND_URL = "https://gmail.googleapis.com/gmail/v1/users/me/messages/send"
    private const val GMAIL_SCOPE = "oauth2:https://www.googleapis.com/auth/gmail.send"

    /**
     * Send feedback to dev + confirmation to user via Gmail API.
     * @throws FeedbackNeedConsentException if user needs to grant Gmail permission first
     */
    /**
     * @return error message or null on success
     */
    suspend fun send(
        context: Context,
        accountEmail: String,
        feedbackText: String,
        subject: String = "Best Journal Feedback",
    ): String? = withContext(Dispatchers.IO) {
        val account = Account(accountEmail, "com.google")
        val token = try {
            GoogleAuthUtil.getToken(context, account, GMAIL_SCOPE)
        } catch (e: UserRecoverableAuthException) {
            throw FeedbackNeedConsentException(e.intent ?: Intent())
        } catch (e: Exception) {
            return@withContext "Token-Fehler: ${e.message}"
        }

        try {
            // 1. Send feedback to developer
            val devMessage = buildRawEmail(
                from = accountEmail,
                to = DEV_EMAIL,
                subject = subject,
                body = "Feedback von: $accountEmail\n\n$feedbackText"
            )
            sendViaGmailApi(token, devMessage)

            // 2. Send confirmation copy to user
            val userMessage = buildRawEmail(
                from = accountEmail,
                to = accountEmail,
                subject = "Dein Feedback an Best Journal",
                body = "Diese Nachricht haben Sie an den Entwickler geschickt.\nEr wird sich bei Ihnen melden.\n\n" +
                        "--- Ihre Nachricht ---\n$feedbackText\n\n" +
                        "Mit freundlichen Gr\u00fc\u00dfen\nDEV.FRANK"
            )
            sendViaGmailApi(token, userMessage)

            null // success
        } catch (e: Exception) {
            e.message ?: "Unbekannter Fehler"
        }
    }

    private fun buildRawEmail(from: String, to: String, subject: String, body: String): String {
        val subjectEncoded = android.util.Base64.encodeToString(subject.toByteArray(), android.util.Base64.NO_WRAP)
        return "From: $from\r\n" +
                "To: $to\r\n" +
                "Subject: =?UTF-8?B?$subjectEncoded?=\r\n" +
                "Content-Type: text/plain; charset=UTF-8\r\n" +
                "\r\n" +
                body
    }

    private fun sendViaGmailApi(token: String, rawEmail: String) {
        val encoded = android.util.Base64.encodeToString(
            rawEmail.toByteArray(Charsets.UTF_8),
            android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP
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
        val body = try {
            if (code in 200..299) conn.inputStream.bufferedReader().readText()
            else conn.errorStream?.bufferedReader()?.readText() ?: "no body"
        } catch (_: Exception) { "read error" }
        conn.disconnect()

        if (code !in 200..299) {
            throw RuntimeException("Gmail API $code: $body")
        }
    }
}

package com.entropyjournal.data.remote

import android.accounts.Account
import android.content.Context
import com.google.android.gms.auth.GoogleAuthUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

/**
 * Sends feedback emails via Gmail API using the signed-in Google account.
 * No external services needed — uses the user's own Gmail authentication.
 */
object FeedbackSender {

    private const val DEV_EMAIL = "dev.app.support@gmail.com"
    private const val GMAIL_SEND_URL = "https://gmail.googleapis.com/gmail/v1/users/me/messages/send"
    private const val GMAIL_SCOPE = "oauth2:https://www.googleapis.com/auth/gmail.send"

    /**
     * Send feedback to the developer and a confirmation copy to the user.
     * @return true if both emails were sent successfully
     */
    suspend fun send(
        context: Context,
        accountEmail: String,
        feedbackText: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val account = Account(accountEmail, "com.google")
            val token = GoogleAuthUtil.getToken(context, account, GMAIL_SCOPE)

            // 1. Send feedback to developer
            val devMessage = buildRawEmail(
                from = accountEmail,
                to = DEV_EMAIL,
                subject = "Entropy Journal Feedback",
                body = "Feedback von: $accountEmail\n\n$feedbackText"
            )
            sendViaGmailApi(token, devMessage)

            // 2. Send confirmation to user
            val userMessage = buildRawEmail(
                from = accountEmail,
                to = accountEmail,
                subject = "Dein Feedback an Entropy Journal",
                body = "Diese Nachricht haben Sie an den Entwickler geschickt.\nEr wird sich bei Ihnen melden.\n\n" +
                        "--- Ihre Nachricht ---\n$feedbackText\n\n" +
                        "Mit freundlichen Gr\u00fc\u00dfen\nDEV.FRANK"
            )
            sendViaGmailApi(token, userMessage)

            true
        } catch (_: Exception) {
            false
        }
    }

    private fun buildRawEmail(from: String, to: String, subject: String, body: String): String {
        return "From: $from\r\n" +
                "To: $to\r\n" +
                "Subject: =?UTF-8?B?${Base64.getEncoder().encodeToString(subject.toByteArray())}?=\r\n" +
                "Content-Type: text/plain; charset=UTF-8\r\n" +
                "\r\n" +
                body
    }

    private fun sendViaGmailApi(token: String, rawEmail: String) {
        val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(rawEmail.toByteArray())
        val json = """{"raw":"$encoded"}"""

        val url = URL(GMAIL_SEND_URL)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Authorization", "Bearer $token")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        conn.outputStream.bufferedWriter().use { it.write(json) }

        val code = conn.responseCode
        conn.disconnect()
        if (code !in 200..299) {
            throw RuntimeException("Gmail API error: $code")
        }
    }
}

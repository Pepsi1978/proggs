package de.frank.jarvis.mcp

import android.content.Context
import de.frank.jarvis.data.Einstellungen
import fi.iki.elonen.NanoHTTPD
import java.io.ByteArrayInputStream
import java.io.DataInputStream

/**
 * HTTP-Zugang zum MCP-Server, nur auf dem Handy selbst (127.0.0.1). Von außen kommt man ausschließlich
 * über den Tunnel herein — und nur mit der geheimen Adresse `/j/<Geheimnis>/mcp`. Alles andere ist 404.
 */
class HttpServer(context: Context, port: Int) : NanoHTTPD("127.0.0.1", port) {
    private val einstellungen = Einstellungen.get(context)
    private val mcp = McpServer(context)

    override fun serve(session: IHTTPSession): Response {
        if (session.uri.trimEnd('/') != "/j/${einstellungen.geheimnis}/mcp") {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found")
        }
        return when (session.method) {
            Method.POST -> {
                val laenge = session.headers["content-length"]?.toIntOrNull() ?: 0
                if (laenge <= 0 || laenge > MAX_RUMPF) return json(Response.Status.BAD_REQUEST, """{"jsonrpc":"2.0","id":null,"error":{"code":-32600,"message":"Leere oder zu große Anfrage"}}""")
                val rumpf = ByteArray(laenge).also { DataInputStream(session.inputStream).readFully(it) }.toString(Charsets.UTF_8)
                val antwort = mcp.verarbeite(rumpf)
                if (antwort == null) newFixedLengthResponse(Response.Status.ACCEPTED, MIME_PLAINTEXT, "") else json(Response.Status.OK, antwort)
            }
            // Kein Server-Stream: Der Server antwortet immer direkt auf den POST.
            else -> newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, MIME_PLAINTEXT, "Method not allowed").apply { addHeader("Allow", "POST") }
        }
    }

    private fun json(status: Response.Status, text: String): Response {
        val bytes = text.toByteArray(Charsets.UTF_8)
        return newFixedLengthResponse(status, "application/json", ByteArrayInputStream(bytes), bytes.size.toLong())
    }

    companion object {
        private const val MAX_RUMPF = 1_000_000
    }
}

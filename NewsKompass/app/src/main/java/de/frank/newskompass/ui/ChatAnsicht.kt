package de.frank.newskompass.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.newskompass.NewsApplication
import de.frank.newskompass.data.model.Meldung
import de.frank.newskompass.news.ChatNachricht
import de.frank.newskompass.news.ChatRolle
import de.frank.newskompass.news.NachrichtenChat
import de.frank.newskompass.news.QuellenFilter
import de.frank.newskompass.news.SprachStufe
import de.frank.newskompass.tts.VorleseZustand
import de.frank.newskompass.ui.theme.LocalKompassFarben

/** Startfragen, solange das Gespräch noch leer ist — ein Tipp schickt sie ab. */
private val VORSCHLAEGE = listOf(
    "Fasse das in drei Sätzen zusammen",
    "Erklär mir das ganz einfach",
    "Was sind die Hintergründe?",
    "Was bedeutet das für mich?",
    "Was ist daran umstritten?",
    "Gibt es schon Neues dazu?",
)

/**
 * Das Gespräch mit der KI über eine Meldung, bildschirmfüllend über den Nachrichten.
 *
 * Unten wie in einem Messenger: Ist das Feld leer, steht dort das Mikrofon (tippen, sprechen,
 * noch einmal tippen — die Frage geht sofort ab und die Antwort wird vorgelesen); sobald getippt
 * wird, wird es zum Senden-Knopf. Während die KI schreibt, hält Stopp die Antwort an.
 */
@Composable
fun ChatAnsicht(
    app: NewsApplication,
    meldung: Meldung,
    farbe: Color,
    schliessen: () -> Unit,
    oeffneEinstellungen: () -> Unit,
) {
    val kontext = LocalContext.current
    val chat by app.chat.zustand.collectAsStateWithLifecycle()
    val vorlesen by app.vorleser.zustand.collectAsStateWithLifecycle()
    val verlauf = chat.gespraeche[meldung.id].orEmpty()
    val antwortetHier = chat.antwortetFuer == meldung.id
    val antwortetAnderswo = chat.antwortetFuer != null && !antwortetHier
    var eingabe by rememberSaveable(meldung.id) { mutableStateOf("") }
    var leerenFragen by remember { mutableStateOf(false) }
    val mikrofonErlaubnis = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { erteilt ->
        app.chat.erlaubnisErhalten(erteilt)
    }

    BackHandler(onBack = schliessen)
    // Beim Schließen: keine offene Aufnahme und kein Vorlesen aus dem Chat zurücklassen.
    DisposableEffect(meldung.id) {
        app.chat.sichtbar(meldung.id)
        onDispose {
            app.chat.sichtbar(null)
            app.chat.brichAufnahmeAb()
            app.chat.loescheMeldung()
            if (app.vorleser.zustand.value.quelleId.startsWith("chat-")) app.vorleser.stoppe()
        }
    }

    fun schicke(text: String) {
        if (text.isBlank() || chat.antwortetFuer != null) return
        app.chat.sende(meldung, text)
        eingabe = ""
    }

    val liste = rememberLazyListState()
    // Einleitung + Nachrichten + (laufende Antwort oder Vorschläge).
    val letzter = verlauf.size + (if (antwortetHier || verlauf.isEmpty()) 1 else 0)
    LaunchedEffect(verlauf.size, antwortetHier) {
        // Beim Öffnen eines leeren Gesprächs bleibt die Einleitung oben stehen.
        if (verlauf.isNotEmpty() || antwortetHier) liste.animateScrollToItem(letzter)
    }
    // Während die Antwort hereinläuft, mitwandern — aber nur, wenn man unten ist und nicht gerade zurückliest.
    LaunchedEffect(chat.teilText.length) {
        val unten = liste.layoutInfo.visibleItemsInfo.lastOrNull()?.index == letzter
        if (antwortetHier && unten) liste.scrollBy(10_000f)
    }

    if (leerenFragen) {
        AlertDialog(
            onDismissRequest = { leerenFragen = false },
            title = { Text("Gespräch leeren?") },
            text = { Text("Das Gespräch zu dieser Meldung wird gelöscht. Danach fängst du von vorn an.") },
            confirmButton = {
                TextButton(onClick = {
                    leerenFragen = false
                    app.chat.leere(meldung.id)
                }) { Text("Leeren") }
            },
            dismissButton = { TextButton(onClick = { leerenFragen = false }) { Text("Abbrechen") } },
        )
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().imePadding()) {
            ChatKopf(
                titel = meldung.titel,
                autoVorlesen = chat.autoVorlesen,
                kannLeeren = verlauf.isNotEmpty() || antwortetHier,
                zurueck = schliessen,
                schalteVorlesen = { app.chat.setzeAutoVorlesen(!chat.autoVorlesen) },
                leeren = { leerenFragen = true },
            )
            LazyColumn(
                state = liste,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item(key = "einleitung") { Einleitung(meldung, farbe, chat.autoVorlesen) }
                items(verlauf, key = { it.id }) { nachricht ->
                    Blase(
                        nachricht = nachricht,
                        farbe = farbe,
                        vorlesen = vorlesen,
                        quelleId = NachrichtenChat.vorleseId(meldung.id, nachricht.id),
                        lies = { app.vorleser.schalteUm(NachrichtenChat.vorleseId(meldung.id, nachricht.id), nachricht.text) },
                        beenden = { app.vorleser.stoppe() },
                        nochmal = if (nachricht.fehler && nachricht.id == verlauf.lastOrNull()?.id && chat.antwortetFuer == null) {
                            { app.chat.wiederhole(meldung) }
                        } else {
                            null
                        },
                    )
                }
                if (antwortetHier) {
                    item(key = "schreibt") { SchreibtGerade(chat.teilText, farbe) }
                } else if (verlauf.isEmpty()) {
                    item(key = "vorschlaege") { Vorschlaege(farbe, aktiv = chat.antwortetFuer == null) { schicke(it) } }
                }
            }

            AnimatedVisibility(visible = chat.meldung.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Row(Modifier.padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.WarningAmber, null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(10.dp))
                        Text(chat.meldung, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        if (chat.zuEinstellungen) {
                            TextButton(onClick = {
                                app.chat.loescheMeldung()
                                schliessen()
                                oeffneEinstellungen()
                            }) { Text("Einstellungen") }
                        } else {
                            TextButton(onClick = app.chat::loescheMeldung) { Text("OK") }
                        }
                    }
                }
            }

            EingabeZeile(
                text = eingabe,
                aendere = { eingabe = it },
                stufe = chat.sprachStufe,
                antwortet = antwortetHier,
                gesperrt = antwortetAnderswo,
                senden = { schicke(eingabe) },
                stoppen = app.chat::stoppeAntwort,
                mikrofon = {
                    val erlaubt = ContextCompat.checkSelfPermission(kontext, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                    app.chat.tippeMikrofon(meldung, erlaubt) { mikrofonErlaubnis.launch(Manifest.permission.RECORD_AUDIO) }
                },
            )
        }
    }
}

@Composable
private fun ChatKopf(
    titel: String,
    autoVorlesen: Boolean,
    kannLeeren: Boolean,
    zurueck: () -> Unit,
    schalteVorlesen: () -> Unit,
    leeren: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp))
            .background(Brush.linearGradient(LocalKompassFarben.current.kopfVerlauf)),
    ) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = zurueck) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Zurück zu den Nachrichten", tint = Color.White)
            }
            Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                Text("MIT DER KI DISKUTIEREN", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
                Text(titel, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = schalteVorlesen) {
                Icon(
                    if (autoVorlesen) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                    if (autoVorlesen) "Antworten werden vorgelesen (ausschalten)" else "Antworten nur auf Tipp vorlesen (alle vorlesen einschalten)",
                    tint = Color.White,
                )
            }
            IconButton(onClick = leeren, enabled = kannLeeren) {
                Icon(Icons.Rounded.DeleteSweep, "Gespräch leeren", tint = Color.White.copy(alpha = if (kannLeeren) 1f else 0.4f))
            }
        }
    }
}

/** Oben im Chat: worüber gesprochen wird und wie es geht. */
@Composable
private fun Einleitung(meldung: Meldung, farbe: Color, autoVorlesen: Boolean) {
    Surface(
        color = farbe.copy(alpha = 0.10f),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = farbe, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Frag mich alles zu dieser Meldung", style = MaterialTheme.typography.titleSmall, color = farbe)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Tippe deine Frage oder Meinung ein — oder tippe unten aufs Mikrofon, sprich und tippe noch einmal. " +
                    if (autoVorlesen) "Jede Antwort lese ich dir vor." else "Antworten auf gesprochene Fragen lese ich dir vor, alle anderen per Lautsprecher-Knopf.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (meldung.wann.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Meldung von ${meldung.wann}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Vorschlaege(farbe: Color, aktiv: Boolean, waehle: (String) -> Unit) {
    Column(Modifier.widthIn(max = 760.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Zum Beispiel:",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp),
        )
        VORSCHLAEGE.forEach { vorschlag ->
            Surface(
                onClick = { waehle(vorschlag) },
                enabled = aktiv,
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, farbe.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(vorschlag, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Rounded.Send, "Abschicken", tint = farbe, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun Blase(
    nachricht: ChatNachricht,
    farbe: Color,
    vorlesen: VorleseZustand,
    quelleId: String,
    lies: () -> Unit,
    beenden: () -> Unit,
    nochmal: (() -> Unit)?,
) {
    val kontext = LocalContext.current
    val meins = nachricht.rolle == ChatRolle.DU
    Box(Modifier.widthIn(max = 760.dp).fillMaxWidth(), contentAlignment = if (meins) Alignment.CenterEnd else Alignment.CenterStart) {
        if (meins) {
            Surface(
                color = farbe,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 6.dp),
                modifier = Modifier.padding(start = 48.dp),
            ) {
                SelectionContainer {
                    Text(
                        nachricht.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                    )
                }
            }
        } else {
            Surface(
                color = if (nachricht.fehler) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(topStart = 6.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
                modifier = Modifier.padding(end = 28.dp),
            ) {
                Column(Modifier.padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (nachricht.fehler) Icons.Rounded.WarningAmber else Icons.Rounded.AutoAwesome,
                            null,
                            tint = if (nachricht.fehler) MaterialTheme.colorScheme.error else farbe,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (nachricht.fehler) "Das hat nicht geklappt" else "News Kompass KI",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (nachricht.fehler) MaterialTheme.colorScheme.onErrorContainer else farbe,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    val absaetze = remember(nachricht.text) { nachricht.text.split(Regex("\n{2,}")).map(String::trim).filter(String::isNotBlank) }
                    SelectionContainer {
                        Column {
                            absaetze.forEachIndexed { i, absatz ->
                                if (i > 0) Spacer(Modifier.height(10.dp))
                                Text(
                                    absatz,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (nachricht.fehler) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                    if (nachricht.abgebrochen) {
                        Spacer(Modifier.height(6.dp))
                        Text("Angehalten", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (nachricht.quellen.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(nachricht.quellen) { adresse ->
                                AssistChip(
                                    onClick = { runCatching { kontext.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(adresse))) } },
                                    label = { Text(domainVon(adresse), maxLines = 1) },
                                    leadingIcon = { Icon(Icons.Rounded.Link, null, Modifier.size(AssistChipDefaults.IconSize)) },
                                    shape = RoundedCornerShape(50),
                                )
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        if (nochmal != null) {
                            TextButton(onClick = nochmal) {
                                Icon(Icons.Rounded.Refresh, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Nochmal versuchen")
                            }
                        } else if (!nachricht.fehler) {
                            LautsprecherKnopf(vorlesen, quelleId, farbe, lies, beenden, rahmen = true)
                        }
                    }
                }
            }
        }
    }
}

/** Die Antwort, während sie entsteht — erst „denkt nach“, dann Wort für Wort. */
@Composable
private fun SchreibtGerade(teilText: String, farbe: Color) {
    val sauber = remember(teilText) { QuellenFilter.entferne(teilText) }
    Box(Modifier.widthIn(max = 760.dp).fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(topStart = 6.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
            modifier = Modifier.padding(end = 28.dp),
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(16.dp), color = farbe, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (sauber.isBlank()) "Denkt nach und prüft die Fakten …" else "Schreibt …",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = farbe,
                    )
                }
                if (sauber.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(sauber, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun EingabeZeile(
    text: String,
    aendere: (String) -> Unit,
    stufe: SprachStufe,
    antwortet: Boolean,
    gesperrt: Boolean,
    senden: () -> Unit,
    stoppen: () -> Unit,
    mikrofon: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp, shadowElevation = 8.dp) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 12.dp, end = 10.dp, top = 8.dp, bottom = 10.dp)) {
            AnimatedVisibility(visible = stufe != SprachStufe.BEREIT) {
                Text(
                    if (stufe == SprachStufe.NIMMT_AUF) "Ich höre zu … tippe auf Stopp, dann geht deine Frage ab." else "Ich verstehe dich …",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (stufe == SprachStufe.NIMMT_AUF) Color(0xFFDC2626) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 6.dp, bottom = 6.dp),
                )
            }
            Row(verticalAlignment = Alignment.Bottom) {
                OutlinedTextField(
                    value = text,
                    onValueChange = aendere,
                    placeholder = { Text(if (antwortet) "Die KI antwortet …" else "Frage oder Meinung eingeben …") },
                    enabled = stufe == SprachStufe.BEREIT,
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { if (!antwortet && !gesperrt) senden() }),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                val knopfVerlauf = LocalKompassFarben.current.knopfVerlauf
                when {
                    antwortet -> RundKnopf(
                        flaeche = Brush.linearGradient(knopfVerlauf),
                        beschreibung = "Antwort anhalten",
                        onClick = stoppen,
                    ) { Icon(Icons.Rounded.Stop, null, tint = Color.White, modifier = Modifier.size(28.dp)) }
                    text.isNotBlank() && stufe == SprachStufe.BEREIT -> RundKnopf(
                        flaeche = Brush.linearGradient(knopfVerlauf),
                        beschreibung = "Senden",
                        enabled = !gesperrt,
                        onClick = senden,
                    ) { Icon(Icons.AutoMirrored.Rounded.Send, null, tint = Color.White, modifier = Modifier.size(24.dp)) }
                    else -> RundKnopf(
                        flaeche = if (stufe == SprachStufe.NIMMT_AUF) {
                            Brush.linearGradient(listOf(Color(0xFFEF4444), Color(0xFFB91C1C)))
                        } else {
                            Brush.linearGradient(knopfVerlauf)
                        },
                        beschreibung = if (stufe == SprachStufe.NIMMT_AUF) "Aufnahme beenden und Frage senden" else "Frage einsprechen",
                        enabled = stufe != SprachStufe.VERSTEHT && !gesperrt,
                        puls = stufe == SprachStufe.NIMMT_AUF,
                        onClick = mikrofon,
                    ) {
                        when (stufe) {
                            SprachStufe.BEREIT -> Icon(Icons.Rounded.Mic, null, tint = Color.White, modifier = Modifier.size(28.dp))
                            SprachStufe.NIMMT_AUF -> Icon(Icons.Rounded.Stop, null, tint = Color.White, modifier = Modifier.size(28.dp))
                            SprachStufe.VERSTEHT -> CircularProgressIndicator(Modifier.size(24.dp), color = Color.White, strokeWidth = 3.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RundKnopf(
    flaeche: Brush,
    beschreibung: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    puls: Boolean = false,
    inhalt: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = Color.Transparent,
        shadowElevation = 6.dp,
        modifier = Modifier
            .size(56.dp)
            .pulsWenn(puls, bis = 1.08f, dauerMs = 700)
            .semantics { contentDescription = beschreibung },
    ) {
        Box(
            Modifier.fillMaxSize().background(flaeche).graphicsLayer { alpha = if (enabled) 1f else 0.45f },
            contentAlignment = Alignment.Center,
        ) { inhalt() }
    }
}

private fun domainVon(adresse: String): String =
    runCatching { Uri.parse(adresse).host.orEmpty().removePrefix("www.") }.getOrDefault(adresse).ifBlank { adresse }

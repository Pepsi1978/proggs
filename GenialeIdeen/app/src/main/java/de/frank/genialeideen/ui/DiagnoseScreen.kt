package de.frank.genialeideen.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import de.frank.genialeideen.observability.IdeenLog
import de.frank.genialeideen.observability.LogStufe
import de.frank.genialeideen.ui.theme.LocalGold
import de.frank.genialeideen.ui.theme.Semantisch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DiagnoseScreen(
    viewModel: IdeenViewModel,
    aufZurueck: () -> Unit,
) {
    val gold = LocalGold.current
    val context = LocalContext.current
    val theme by viewModel.theme.collectAsState()
    val zeilen by IdeenLog.zeilen.collectAsState()
    var stufe by remember { mutableStateOf<LogStufe?>(null) }
    val berichte = remember { viewModel.absturzberichte() }

    val gefiltert = zeilen.filter { stufe == null || it.stufe == stufe }
    val datei = IdeenLog.logDatei()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(gold.hintergrund),
    ) {
        IdeenKopfleiste(
            titel = "Diagnose",
            themeWahl = theme,
            aufEinstellungen = aufZurueck,
            voran = {
                Box(
                    modifier = Modifier.size(38.dp).druckEffekt(aufZurueck),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Zurück",
                        tint = gold.primaer,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(6.dp))
            },
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StufenChip("Alles", stufe == null) { stufe = null }
            LogStufe.entries.forEach { eintrag ->
                StufenChip(eintrag.name, stufe == eintrag) { stufe = eintrag }
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StufenChip("Protokoll teilen", false) {
                val quelle = datei ?: return@StufenChip
                runCatching {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.dateien",
                        quelle,
                    )
                    val teilen = Intent(Intent.ACTION_SEND)
                        .setType("text/plain")
                        .putExtra(Intent.EXTRA_STREAM, uri)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    context.startActivity(Intent.createChooser(teilen, "Protokoll teilen"))
                }.onFailure {
                    viewModel.zeige(
                        Meldung("Das Protokoll liess sich nicht teilen: ${it.message}", istFehler = true),
                    )
                }
            }
            StufenChip("Protokoll leeren", false) { viewModel.leereProtokoll() }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Datei: ${datei?.name ?: "noch keine"} · ${(datei?.length() ?: 0L) / 1024} KB · " +
                "${berichte.size} Absturzberichte",
            style = MaterialTheme.typography.labelSmall,
            color = gold.textGedaempft,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        if (berichte.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StufenChip("Berichte verwerfen", false) { viewModel.verwerfeAbstuerze() }
            }
        }

        Spacer(Modifier.height(10.dp))

        if (gefiltert.isEmpty()) {
            Leerzustand(
                symbol = "🩺",
                ueberschrift = "Nichts protokolliert",
                satz = "Sobald die App etwas Nennenswertes tut, steht es hier — neueste zuerst.",
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().navigationBarsPadding(),
                contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 24.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(gefiltert) { zeile ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(gold.flaeche)
                            .border(1.dp, gold.rahmen, RoundedCornerShape(12.dp))
                            .padding(10.dp),
                    ) {
                        Row {
                            Text(
                                zeile.stufe.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = when (zeile.stufe) {
                                    LogStufe.ERROR -> Semantisch.fehler
                                    LogStufe.WARN -> Semantisch.warnung
                                    LogStufe.INFO -> Semantisch.info
                                    LogStufe.DEBUG -> gold.textGedaempft
                                },
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                ZEIT.format(Date(zeile.zeitpunkt)),
                                style = MaterialTheme.typography.labelSmall,
                                color = gold.textGedaempft,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${zeile.modul}.${zeile.funktion}: ${zeile.nachricht}",
                            style = MaterialTheme.typography.bodySmall,
                            color = gold.textPrimaer,
                        )
                        if (zeile.kontext.isNotEmpty()) {
                            Text(
                                zeile.kontext.entries.joinToString(" · ") { "${it.key}=${it.value}" },
                                style = MaterialTheme.typography.labelSmall,
                                color = gold.textGedaempft,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StufenChip(text: String, gewaehlt: Boolean, aufTipp: () -> Unit) {
    val gold = LocalGold.current
    Box(
        modifier = Modifier
            .druckEffekt(aufTipp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (gewaehlt) gold.primaer.copy(alpha = 0.2f) else Color.Transparent)
            .border(1.dp, if (gewaehlt) gold.primaer else gold.rahmen, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = if (gewaehlt) gold.primaer else gold.textGedaempft,
        )
    }
}

private val ZEIT = SimpleDateFormat("HH:mm:ss", Locale.GERMANY)

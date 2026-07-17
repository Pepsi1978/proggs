package de.frank.karteikartenlernen.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.karteikartenlernen.text.ArticleBlock
import de.frank.karteikartenlernen.text.parseResearchArticle
import de.frank.karteikartenlernen.ui.theme.LocalAppPalette
import de.frank.karteikartenlernen.ui.theme.Newsreader

@Composable
fun ResearchArticle(answer: String, paragraphColor: Color = LocalAppPalette.current.muted) {
    val c = LocalAppPalette.current
    val uriHandler = LocalUriHandler.current
    Column {
        parseResearchArticle(answer).forEach { block ->
            when (block) {
                is ArticleBlock.Heading -> Text(
                    text = block.text,
                    color = c.text,
                    fontFamily = Newsreader,
                    fontSize = if (block.level == 1) 22.sp else 19.sp,
                    lineHeight = 25.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 18.dp, bottom = 5.dp),
                )
                is ArticleBlock.Paragraph -> Text(
                    text = block.text,
                    color = paragraphColor,
                    fontFamily = Newsreader,
                    fontSize = 15.5.sp,
                    lineHeight = 25.sp,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                is ArticleBlock.Source -> Text(
                    text = block.title,
                    color = c.accent2,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable { runCatching { uriHandler.openUri(block.url) } }
                        .padding(vertical = 5.dp),
                )
            }
        }
    }
}

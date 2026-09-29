package de.frank.aufgaben.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Jedes Design bleibt in einer Farbfamilie: Akzent, zweiter Akzent und dritter Ton sind Abstufungen
 * derselben Farbe. Rot gibt es nur für Warnungen (überfällig, löschen).
 */
fun farbenFuer(design: Design, dunkel: Boolean): Farben = when (design) {
    Design.ORANGE -> if (dunkel) Farben(
        design, true, Color(0xFF0A0908), Color(0xFF14100D), Color(0xFFFF7A1A), Color(0xFFB8520F), Color(0xFF3A2414),
        Color(0x17FFFFFF), Color(0x29FFFFFF), Color(0x33FFD9B8), Color(0xFF000000),
        Color(0xFFF7F1EC), Color(0xFFC9B8AA), Color(0xFF8C7B6E),
        Color(0xFFFF8A1F), Color(0xFFFFA64D), Color(0xFFE56A12), Color(0xFF1A0E05), Color(0xFFFF5C4D), Color(0xFFFF9A3D), 22.dp,
    ) else Farben(
        design, false, Color(0xFFFFF8F1), Color(0xFFFFEBD8), Color(0xFFFFC08A), Color(0xFFFFD9B5), Color(0xFFFFE9D2),
        Color(0xA8FFFFFF), Color(0xE0FFFFFF), Color(0xD9FFFFFF), Color(0xFF8A3D06),
        Color(0xFF2A1A0E), Color(0xFF6E5343), Color(0xFFA88E7E),
        Color(0xFFEE6A0C), Color(0xFFF6892E), Color(0xFFC9560A), Color(0xFFFFFFFF), Color(0xFFD63B2F), Color(0xFFE0700F), 22.dp,
    )
    Design.AURORA -> if (dunkel) Farben(
        design, true, Color(0xFF0B0A18), Color(0xFF14112A), Color(0xFF5B45D6), Color(0xFF3A2C8F), Color(0xFF231B55),
        Color(0x17FFFFFF), Color(0x29FFFFFF), Color(0x38D8CFFF), Color(0xFF000000),
        Color(0xFFF1EFFA), Color(0xFFBDB6DC), Color(0xFF837CA6),
        Color(0xFF9D86FF), Color(0xFFB4A3FF), Color(0xFF7C62F0), Color(0xFF120C30), Color(0xFFFF6B6B), Color(0xFFA995FF), 26.dp,
    ) else Farben(
        design, false, Color(0xFFF6F4FD), Color(0xFFECE8FA), Color(0xFFCFC4FB), Color(0xFFE2DBFB), Color(0xFFEDE9FD),
        Color(0xA8FFFFFF), Color(0xE0FFFFFF), Color(0xD9FFFFFF), Color(0xFF3B2A8C),
        Color(0xFF1D1A33), Color(0xFF5B5775), Color(0xFF9793AE),
        Color(0xFF5B3FE0), Color(0xFF7A62EE), Color(0xFF4A31C4), Color(0xFFFFFFFF), Color(0xFFD9363E), Color(0xFF6A50E8), 26.dp,
    )
    Design.GARTEN -> if (dunkel) Farben(
        design, true, Color(0xFF08110C), Color(0xFF0E1C14), Color(0xFF2E7D55), Color(0xFF1C5238), Color(0xFF15301F),
        Color(0x17FFFFFF), Color(0x29FFFFFF), Color(0x33CFF1DC), Color(0xFF000000),
        Color(0xFFEDF6F0), Color(0xFFB0CBBA), Color(0xFF76937F),
        Color(0xFF5CC98E), Color(0xFF7DD6A5), Color(0xFF3DAE72), Color(0xFF05140C), Color(0xFFFF6B6B), Color(0xFF6BD19A), 30.dp,
    ) else Farben(
        design, false, Color(0xFFF3F8F3), Color(0xFFE6F1E8), Color(0xFFBFE3CC), Color(0xFFD7EEDC), Color(0xFFE8F5EB),
        Color(0xA8FFFFFF), Color(0xE0FFFFFF), Color(0xD9FFFFFF), Color(0xFF1F5E40),
        Color(0xFF142A1C), Color(0xFF4B6555), Color(0xFF879C8E),
        Color(0xFF217A50), Color(0xFF3A9467), Color(0xFF1A6641), Color(0xFFFFFFFF), Color(0xFFD9363E), Color(0xFF2A8A5A), 30.dp,
    )
    Design.KOSMOS -> if (dunkel) Farben(
        design, true, Color(0xFF04070F), Color(0xFF0A1226), Color(0xFF2A5BD6), Color(0xFF173A8F), Color(0xFF0F2250),
        Color(0x17FFFFFF), Color(0x29FFFFFF), Color(0x38BFD6FF), Color(0xFF000000),
        Color(0xFFEAF1FC), Color(0xFFA9B9D6), Color(0xFF6D7D9C),
        Color(0xFF6AA6FF), Color(0xFF8DBBFF), Color(0xFF4C8BF0), Color(0xFF020814), Color(0xFFFF6B6B), Color(0xFF7FB2FF), 14.dp,
    ) else Farben(
        design, false, Color(0xFFF2F6FD), Color(0xFFE4ECFA), Color(0xFFC3D5F8), Color(0xFFD9E4FA), Color(0xFFEAF0FC),
        Color(0xA8FFFFFF), Color(0xE0FFFFFF), Color(0xD9FFFFFF), Color(0xFF1B3A8C),
        Color(0xFF101B33), Color(0xFF4A5775), Color(0xFF8893AB),
        Color(0xFF2A62E0), Color(0xFF4A7EEA), Color(0xFF1F4FC0), Color(0xFFFFFFFF), Color(0xFFD9363E), Color(0xFF3570E3), 14.dp,
    )
}

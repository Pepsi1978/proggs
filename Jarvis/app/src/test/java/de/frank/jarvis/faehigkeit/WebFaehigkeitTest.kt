package de.frank.jarvis.faehigkeit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class WebFaehigkeitTest {
    @Test fun htmlWirdZuLesbaremText() {
        val html = "<html><head><title>T</title><style>p{color:red}</style></head><body><script>alert(1)</script>" +
            "<h1>Überschrift</h1><p>Erster&nbsp;Absatz &amp; mehr</p><div>Zweiter<br>Teil</div></body></html>"
        val text = WebFaehigkeit.htmlZuText(html)
        assertEquals("Überschrift\nErster Absatz & mehr\nZweiter\nTeil", text)
        assertFalse(text.contains("alert"))
    }
}

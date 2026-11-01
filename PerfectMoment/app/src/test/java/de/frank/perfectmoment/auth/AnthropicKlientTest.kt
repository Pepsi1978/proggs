package de.frank.perfectmoment.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnthropicKlientTest {
    @Test
    fun acceptsEveryAnthropicKeyPrefix() {
        assertTrue(AnthropicKlient.istApiSchluessel("sk-ant-api03-abc"))
        assertTrue(AnthropicKlient.istApiSchluessel("sk-ant-user-abc"))
    }

    @Test
    fun rejectsSubscriptionTokensAndForeignKeys() {
        assertFalse(AnthropicKlient.istApiSchluessel("sk-ant-oat01-abc"))
        assertFalse(AnthropicKlient.istApiSchluessel("sk-proj-abc"))
        assertFalse(AnthropicKlient.istApiSchluessel(""))
    }
}

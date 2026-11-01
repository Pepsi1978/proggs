package de.frank.perfectmoment.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnthropicKlientTest {
    @Test
    fun acceptsEveryAnthropicKeyPrefix() {
        listOf("sk-ant-api03-abc", "sk-ant-user-abc").forEach {
            assertTrue(it, AnthropicKlient.istApiSchluessel(it))
            assertTrue(it, AnthropicKlient.wirktWieAnthropicSchluessel(it))
        }
    }

    @Test
    fun blocksOnlyWhatCertainlyCannotWork() {
        listOf("", "sk-ant-oat01-abc", "sk-ant-…", "sk‑ant-abc").forEach {
            assertFalse(it, AnthropicKlient.istApiSchluessel(it))
            assertFalse(it, AnthropicKlient.wirktWieAnthropicSchluessel(it))
        }
    }

    @Test
    fun foreignKeyOnlyWarnsAndLeavesTheVerdictToTheApi() {
        assertTrue(AnthropicKlient.istApiSchluessel("sk-proj-abc"))
        assertFalse(AnthropicKlient.wirktWieAnthropicSchluessel("sk-proj-abc"))
    }
}

package de.frank.perfectmoment.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnthropicKlientTest {
    @Test
    fun acceptsEveryAnthropicKeyPrefix() {
        listOf("sk-ant-api03-abcdefghijkl", "sk-ant-user-abcdefghijklmn").forEach {
            assertTrue(it, AnthropicKlient.istApiSchluessel(it))
        }
    }

    @Test
    fun rejectsTokensForeignSecretsAndBrokenValues() {
        listOf(
            "",
            "sk-ant-",
            "sk-ant-oat01-abcdefghijkl",
            "sk-ant-ort01-abcdefghijkl",
            "sk-ant-sid01-abcdefghijkl",
            "sk-ant-admin01-abcdefghijkl",
            "sk-proj-abcdefghijklmnopqrst",
            "AIzaabcdefghijklmnopqrstuvwxyz",
            "sk-ant-api03-abcdefghijkl…",
            "sk‑ant-api03-abcdefghijkl",
        ).forEach { assertFalse(it, AnthropicKlient.istApiSchluessel(it)) }
    }
}

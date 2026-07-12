package de.frank.cortex.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CodexFastModelTest {
    @Test
    fun `fast aliases use base model with priority tier`() {
        listOf("sol", "terra", "luna").forEach { family ->
            val config = codexModelRequestConfig("gpt-5.6-$family-fast")

            assertEquals("gpt-5.6-$family", config.model)
            assertEquals("priority", config.serviceTier)
        }
    }

    @Test
    fun `normal models do not request priority tier`() {
        listOf("sol", "terra", "luna").forEach { family ->
            val model = "gpt-5.6-$family"
            val config = codexModelRequestConfig(model)

            assertEquals(model, config.model)
            assertNull(config.serviceTier)
        }
    }
}

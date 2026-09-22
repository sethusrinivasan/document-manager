package com.app.paperstow.presentation.about

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SbomParseTest {
    @Test
    fun `parses cyclonedx metadata and components`() {
        val json = """
            {
              "bomFormat": "CycloneDX",
              "specVersion": "1.5",
              "metadata": {
                "timestamp": "2026-09-22T22:00:00Z",
                "component": { "name": "Paperstow", "version": "1.1.0" },
                "properties": [
                  { "name": "build:variant", "value": "debug" }
                ]
              },
              "components": [
                { "group": "androidx.core", "name": "core-ktx", "version": "1.18.0", "purl": "pkg:maven/androidx.core/core-ktx@1.18.0" }
              ]
            }
        """.trimIndent()
        val sbom = parseSbom(json)
        assertEquals("1.5", sbom.specVersion)
        assertEquals("Paperstow", sbom.appName)
        assertEquals("1.1.0", sbom.appVersion)
        assertEquals("debug", sbom.variant)
        assertEquals(1, sbom.components.size)
        assertEquals("core-ktx", sbom.components[0].name)
    }
}

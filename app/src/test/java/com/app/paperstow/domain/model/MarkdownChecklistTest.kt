package com.app.paperstow.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MarkdownChecklistTest {

    @Test
    fun `round trip keeps title notes and checks`() {
        val original = MarkdownChecklist(
            title = "Trip plans",
            notes = "For John Doe and Jane Doe",
            items = listOf(
                ChecklistItem("Book flights", done = true),
                ChecklistItem("Pack medicines", done = false)
            )
        )
        val parsed = MarkdownChecklist.parse(original.toMarkdown())
        assertEquals(original.title, parsed.title)
        assertEquals(original.notes, parsed.notes)
        assertEquals(2, parsed.items.size)
        assertTrue(parsed.items[0].done)
        assertEquals("Book flights", parsed.items[0].text)
        assertFalse(parsed.items[1].done)
    }

    @Test
    fun `plain bullets become open items`() {
        val parsed = MarkdownChecklist.parse("# Packing\n\n- Passport\n* Tickets\n")
        assertEquals("Packing", parsed.title)
        assertEquals(listOf("Passport", "Tickets"), parsed.items.map { it.text })
        assertTrue(parsed.items.none { it.done })
    }
}

package com.app.paperstow.domain.model

/**
 * Tiny markdown checklist used for plans and action items.
 * Understands headings, notes, `- [ ]` / `- [x]`, and plain `-` / `*` bullets.
 */
data class ChecklistItem(
    val text: String,
    val done: Boolean = false
)

data class MarkdownChecklist(
    val title: String = "",
    val notes: String = "",
    val items: List<ChecklistItem> = emptyList()
) {
    fun toMarkdown(): String {
        val parts = mutableListOf<String>()
        val heading = title.trim()
        if (heading.isNotEmpty()) parts += "# $heading"
        val body = notes.trim()
        if (body.isNotEmpty()) {
            if (parts.isNotEmpty()) parts += ""
            parts += body
        }
        if (items.isNotEmpty()) {
            if (parts.isNotEmpty()) parts += ""
            items.forEach { item ->
                val mark = if (item.done) "x" else " "
                parts += "- [$mark] ${item.text.trim()}"
            }
        }
        return parts.joinToString("\n") + if (parts.isEmpty()) "" else "\n"
    }

    companion object {
        private val heading = Regex("""^\s{0,3}#{1,6}\s+(.*)$""")
        private val checkbox = Regex("""^\s*[-*+]\s+\[([ xX])]\s+(.*)$""")
        private val bullet = Regex("""^\s*[-*+]\s+(.*)$""")

        fun parse(markdown: String): MarkdownChecklist {
            val title = StringBuilder()
            val notes = StringBuilder()
            val items = mutableListOf<ChecklistItem>()
            markdown.replace("\r\n", "\n").lines().forEach { raw ->
                val line = raw.trimEnd()
                when {
                    title.isEmpty() && items.isEmpty() && notes.isEmpty() && heading.matches(line) ->
                        title.append(heading.matchEntire(line)!!.groupValues[1].trim())
                    checkbox.matches(line) -> {
                        val match = checkbox.matchEntire(line)!!
                        items += ChecklistItem(
                            text = match.groupValues[2].trim(),
                            done = match.groupValues[1].equals("x", ignoreCase = true)
                        )
                    }
                    bullet.matches(line) ->
                        items += ChecklistItem(text = bullet.matchEntire(line)!!.groupValues[1].trim())
                    line.isBlank() -> {
                        if (notes.isNotEmpty() && !notes.endsWith("\n\n")) notes.append('\n')
                    }
                    else -> {
                        if (notes.isNotEmpty() && !notes.endsWith("\n")) notes.append('\n')
                        notes.append(line)
                    }
                }
            }
            return MarkdownChecklist(
                title = title.toString().trim(),
                notes = notes.toString().trim(),
                items = items
            )
        }

        fun starter(title: String = "Trip plans"): MarkdownChecklist = MarkdownChecklist(
            title = title,
            notes = "Tick items as you finish them. Add your own below.",
            items = listOf(
                ChecklistItem("Confirm tickets and seats"),
                ChecklistItem("Pack medicines and chargers"),
                ChecklistItem("Share the itinerary")
            )
        )
    }
}

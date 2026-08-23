package com.app.traveldocs.data.local

import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores tag-to-color mappings in SharedPreferences.
 *
 * Colors are stored as ARGB hex integers (e.g., 0xFFE3F2FD).
 * Each tag can have its own color for visual differentiation in the UI.
 * If no color is set, a default palette color is derived from the tag name hash.
 */
@Singleton
class TagColorStore @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS = "tag_colors"

        // Predefined palette — visually distinct, accessible on white backgrounds
        val PALETTE = listOf(
            0xFFE3F2FD.toInt(),  // Light Blue
            0xFFE8F5E9.toInt(),  // Light Green
            0xFFFFF3E0.toInt(),  // Light Orange
            0xFFFCE4EC.toInt(),  // Light Pink
            0xFFEDE7F6.toInt(),  // Light Purple
            0xFFFFFDE7.toInt(),  // Light Yellow
            0xFFE0F7FA.toInt(),  // Light Cyan
            0xFFF3E5F5.toInt(),  // Light Lavender
            0xFFEFEBE9.toInt(),  // Light Brown
            0xFFE8EAF6.toInt(),  // Light Indigo
            0xFFF1F8E9.toInt(),  // Lime tint
            0xFFFFF8E1.toInt(),  // Amber tint
        )
    }

    private fun prefs() = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * Get the color for a tag. Falls back to a deterministic palette color based on name hash.
     */
    fun getColor(tagName: String): Int {
        val stored = prefs().getInt("color_$tagName", -1)
        if (stored != -1) return stored
        // Deterministic fallback from name hash
        val idx = (tagName.hashCode().and(0x7FFFFFFF)) % PALETTE.size
        return PALETTE[idx]
    }

    /**
     * Set a specific color for a tag.
     */
    fun setColor(tagName: String, color: Int) {
        prefs().edit().putInt("color_$tagName", color).apply()
    }

    /**
     * Remove stored color (revert to auto-derived).
     */
    fun clearColor(tagName: String) {
        prefs().edit().remove("color_$tagName").apply()
    }
}

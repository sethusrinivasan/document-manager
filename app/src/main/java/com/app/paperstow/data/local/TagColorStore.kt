package com.app.paperstow.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Optional folder photo for each tag. Photos stay on this phone.
 */
@Singleton
class TagColorStore @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS = "tag_colors"

        val PALETTE = listOf(
            0xFF1E88E5.toInt(),
            0xFF00C853.toInt(),
            0xFFFF6D00.toInt(),
            0xFFFF1744.toInt(),
            0xFFAA00FF.toInt(),
            0xFFFFD600.toInt(),
            0xFF00B8D4.toInt(),
            0xFFF50057.toInt(),
            0xFF304FFE.toInt(),
            0xFF64DD17.toInt(),
            0xFFFFAB00.toInt(),
            0xFF00BFA5.toInt(),
            0xFFD500F9.toInt(),
            0xFFFF3D00.toInt(),
            0xFF2979FF.toInt(),
            0xFF76FF03.toInt()
        )

        val EMOJIS = listOf(
            "✈️", "🏨", "🛂", "🎫", "🚗", "💊", "📝", "📍",
            "👨‍👩‍👧", "💼", "🔑", "💳", "📄", "✅", "🎒", "🌍",
            "🚆", "🚢", "🏠", "⭐", "🔔", "💡"
        )

        fun contrastOn(color: Int): Int {
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            val luminance = (0.299 * r + 0.587 * g + 0.114 * b)
            return if (luminance > 160) 0xFF212121.toInt() else 0xFFFFFFFF.toInt()
        }
    }

    private fun prefs() = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun iconDir(): File = File(context.filesDir, "tag_icons").apply { mkdirs() }

    fun getColor(tagName: String): Int {
        val stored = prefs().getInt("color_$tagName", -1)
        if (stored != -1) return stored
        val idx = (tagName.hashCode().and(0x7FFFFFFF)) % PALETTE.size
        return PALETTE[idx]
    }

    fun setColor(tagName: String, color: Int) {
        prefs().edit().putInt("color_$tagName", color).apply()
    }

    fun getEmoji(tagName: String): String? {
        val stored = prefs().getString("emoji_$tagName", null)
        if (!stored.isNullOrBlank()) return stored
        return defaultEmoji(tagName)
    }

    fun setEmoji(tagName: String, emoji: String?) {
        val edit = prefs().edit()
        if (emoji.isNullOrBlank()) edit.remove("emoji_$tagName")
        else edit.putString("emoji_$tagName", emoji)
        edit.apply()
    }

    fun imageFile(tagName: String): File? {
        val file = File(iconDir(), fileName(tagName))
        return if (file.exists() && file.length() > 0) file else null
    }

    fun saveImage(tagName: String, uri: Uri): Boolean {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val decoded = BitmapFactory.decodeStream(input) ?: return false
                val size = maxOf(decoded.width, decoded.height)
                val scaled = if (size > 256) {
                    val factor = 256f / size
                    Bitmap.createScaledBitmap(
                        decoded,
                        (decoded.width * factor).toInt().coerceAtLeast(1),
                        (decoded.height * factor).toInt().coerceAtLeast(1),
                        true
                    )
                } else decoded
                val out = File(iconDir(), fileName(tagName))
                out.outputStream().use { stream ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                }
                if (scaled !== decoded) scaled.recycle()
            } ?: return false
            true
        }.getOrDefault(false)
    }

    fun clearImage(tagName: String) {
        File(iconDir(), fileName(tagName)).delete()
    }

    fun isShownOnHome(tagName: String): Boolean =
        prefs().getBoolean("show_home_$tagName", true)

    fun setShownOnHome(tagName: String, shown: Boolean) {
        prefs().edit().putBoolean("show_home_$tagName", shown).apply()
    }

    fun renameLook(oldName: String, newName: String) {
        if (oldName == newName) return
        setColor(newName, getColor(oldName))
        prefs().getString("emoji_$oldName", null)?.let { setEmoji(newName, it) }
        setShownOnHome(newName, isShownOnHome(oldName))
        val oldFile = File(iconDir(), fileName(oldName))
        if (oldFile.exists()) oldFile.copyTo(File(iconDir(), fileName(newName)), overwrite = true)
        clearLook(oldName)
    }

    fun clearLook(tagName: String) {
        prefs().edit()
            .remove("color_$tagName")
            .remove("emoji_$tagName")
            .remove("show_home_$tagName")
            .apply()
        clearImage(tagName)
    }

    private fun fileName(tagName: String): String {
        val safe = tagName.lowercase().replace(Regex("[^a-z0-9._-]"), "_").take(40)
        return "${safe}_${tagName.hashCode() and 0x7FFFFFFF}.jpg"
    }

    private fun defaultEmoji(tagName: String): String? {
        val n = tagName.lowercase()
        return when {
            n.contains("passport") -> "🛂"
            n.contains("visa") -> "🎫"
            n.contains("ticket") || n.contains("flight") -> "✈️"
            n.contains("hotel") -> "🏨"
            n.contains("health") || n.contains("insurance") || n.contains("vaccine") -> "💊"
            n.contains("note") -> "📝"
            n.contains("plan") || n.contains("check") -> "✅"
            n.contains("trail") -> "📍"
            n.contains("car") || n.contains("rental") -> "🚗"
            n.contains("sample") || n.contains("trip") -> "🌍"
            else -> null
        }
    }
}

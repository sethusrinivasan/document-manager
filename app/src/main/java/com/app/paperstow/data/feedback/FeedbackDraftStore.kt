package com.app.paperstow.data.feedback

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class FeedbackDraft(
    val id: String,
    val liked: String,
    val disliked: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun isBlank(): Boolean = liked.isBlank() && disliked.isBlank()

    fun preview(): String {
        val likedBit = liked.trim().take(40)
        val dislikedBit = disliked.trim().take(40)
        return when {
            likedBit.isNotEmpty() && dislikedBit.isNotEmpty() -> "Liked: $likedBit"
            likedBit.isNotEmpty() -> likedBit
            dislikedBit.isNotEmpty() -> "Not liked: $dislikedBit"
            else -> "Empty draft"
        }
    }

    fun asNoteBody(): String = buildString {
        appendLine("What I liked")
        appendLine(liked.trim().ifBlank { "(none)" })
        appendLine()
        appendLine("What I did not like")
        appendLine(disliked.trim().ifBlank { "(none)" })
    }
}

@Singleton
class FeedbackDraftStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun currentId(): String? = prefs.getString(KEY_CURRENT, null)

    fun draftsNewestFirst(): List<FeedbackDraft> =
        readAll().sortedByDescending { it.updatedAt }.take(MAX_DRAFTS)

    fun get(id: String): FeedbackDraft? = readAll().firstOrNull { it.id == id }

    fun currentOrNew(): FeedbackDraft {
        val id = currentId()
        if (id != null) get(id)?.let { return it }
        val draft = FeedbackDraft(
            id = UUID.randomUUID().toString(),
            liked = "",
            disliked = "",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        upsert(draft, makeCurrent = true)
        return draft
    }

    fun startNew(): FeedbackDraft {
        val existing = currentOrNew()
        if (existing.isBlank()) return existing
        val draft = FeedbackDraft(
            id = UUID.randomUUID().toString(),
            liked = "",
            disliked = "",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        upsert(draft, makeCurrent = true)
        return draft
    }

    fun saveCurrent(liked: String, disliked: String): FeedbackDraft {
        val now = System.currentTimeMillis()
        val previous = currentOrNew()
        val draft = previous.copy(liked = liked, disliked = disliked, updatedAt = now)
        if (draft.isBlank()) {
            upsert(draft, makeCurrent = true)
            return draft
        }
        upsert(draft, makeCurrent = true)
        return draft
    }

    fun setCurrent(id: String) {
        if (get(id) != null) prefs.edit().putString(KEY_CURRENT, id).apply()
    }

    fun delete(id: String) {
        val remaining = readAll().filter { it.id != id }
        writeAll(remaining)
        if (currentId() == id) {
            val next = remaining.maxByOrNull { it.updatedAt }
            prefs.edit().putString(KEY_CURRENT, next?.id).apply()
        }
    }

    private fun upsert(draft: FeedbackDraft, makeCurrent: Boolean) {
        val others = readAll().filter { it.id != draft.id }
        val merged = (listOf(draft) + others).sortedByDescending { it.updatedAt }.take(MAX_DRAFTS)
        writeAll(merged)
        if (makeCurrent) prefs.edit().putString(KEY_CURRENT, draft.id).apply()
    }

    private fun readAll(): List<FeedbackDraft> {
        val raw = prefs.getString(KEY_DRAFTS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(
                        FeedbackDraft(
                            id = obj.getString("id"),
                            liked = obj.optString("liked"),
                            disliked = obj.optString("disliked"),
                            createdAt = obj.optLong("createdAt"),
                            updatedAt = obj.optLong("updatedAt")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun writeAll(drafts: List<FeedbackDraft>) {
        val array = JSONArray()
        drafts.forEach { draft ->
            array.put(
                JSONObject().apply {
                    put("id", draft.id)
                    put("liked", draft.liked)
                    put("disliked", draft.disliked)
                    put("createdAt", draft.createdAt)
                    put("updatedAt", draft.updatedAt)
                }
            )
        }
        prefs.edit().putString(KEY_DRAFTS, array.toString()).apply()
    }

    companion object {
        private const val PREFS = "feedback_drafts"
        private const val KEY_DRAFTS = "drafts"
        private const val KEY_CURRENT = "current_id"
        private const val MAX_DRAFTS = 20
    }
}

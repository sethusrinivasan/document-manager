package com.app.traveldocs.debug

import android.content.Context
import java.io.File

object TempFileCleanup {
    fun cleanSharedDocs(context: Context) {
        try {
            val dir = File(context.cacheDir, "shared_docs")
            if (dir.exists()) {
                val deleted = dir.listFiles()?.count { it.delete() } ?: 0
                if (deleted > 0) DebugLogger.d("TempCleanup", "Deleted $deleted temp files")
            }
        } catch (e: Exception) {
            DebugLogger.e("TempCleanup", "Cleanup failed", e)
        }
    }
}

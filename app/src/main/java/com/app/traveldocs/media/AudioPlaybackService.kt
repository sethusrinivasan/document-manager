package com.app.traveldocs.media

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.MediaBrowserServiceCompat
import com.app.traveldocs.debug.DebugLogger
import com.app.traveldocs.domain.model.Document
import com.app.traveldocs.domain.model.DocumentFormat
import com.app.traveldocs.domain.repository.DocumentFileStorage
import com.app.traveldocs.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * MediaBrowserService for Android Auto and system media controls.
 *
 * Exposes audio documents (MP3, M4A, WAV) organized by tags as a browsable media tree.
 * Android Auto connects to this service to show audio content and playback controls
 * on the car's head unit.
 *
 * Media tree structure:
 * ROOT
 * ├── All Audio (flat list)
 * └── By Tag
 *     ├── tag1 (audio files with this tag)
 *     └── tag2 (audio files with this tag)
 */
class AudioPlaybackService : MediaBrowserServiceCompat() {

    private lateinit var documentRepository: DocumentRepository
    private lateinit var fileStorage: DocumentFileStorage

    private lateinit var mediaSession: MediaSessionCompat
    private var mediaPlayer: MediaPlayer? = null
    private var currentDocId: String? = null
    private var playlist: List<Document> = emptyList()
    private var playlistIndex = 0

    companion object {
        private const val TAG = "AudioService"
        private const val ROOT_ID = "root"
        private const val ALL_AUDIO_ID = "all_audio"
        private const val BY_TAG_ID = "by_tag"
    }

    @dagger.hilt.EntryPoint
    @dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
    interface ServiceEntryPoint {
        fun documentRepository(): DocumentRepository
        fun fileStorage(): DocumentFileStorage
    }

    override fun onCreate() {
        super.onCreate()
        val entryPoint = dagger.hilt.android.EntryPointAccessors.fromApplication(applicationContext, ServiceEntryPoint::class.java)
        documentRepository = entryPoint.documentRepository()
        fileStorage = entryPoint.fileStorage()
        mediaSession = MediaSessionCompat(this, "DocumentManagerAudio").apply {
            setCallback(mediaSessionCallback)
            isActive = true
        }
        sessionToken = mediaSession.sessionToken
        DebugLogger.i(TAG, "AudioPlaybackService created")
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot {
        return BrowserRoot(ROOT_ID, null)
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        val items = mutableListOf<MediaBrowserCompat.MediaItem>()
        val audioDocs = getAudioDocuments()

        when (parentId) {
            ROOT_ID -> {
                items.add(createBrowsableItem(ALL_AUDIO_ID, "All Audio", "${audioDocs.size} tracks"))
                items.add(createBrowsableItem(BY_TAG_ID, "By Tag", "Organized by tags"))
            }
            ALL_AUDIO_ID -> {
                audioDocs.forEach { doc ->
                    items.add(createPlayableItem(doc))
                }
            }
            BY_TAG_ID -> {
                val tags = audioDocs.flatMap { it.tags }.map { it.name }.filter { !it.startsWith("__") }.distinct().sorted()
                tags.forEach { tag ->
                    val count = audioDocs.count { d -> d.tags.any { it.name == tag } }
                    items.add(createBrowsableItem("tag_$tag", tag, "$count tracks"))
                }
            }
            else -> {
                if (parentId.startsWith("tag_")) {
                    val tag = parentId.removePrefix("tag_")
                    audioDocs.filter { d -> d.tags.any { it.name == tag } }.forEach { doc ->
                        items.add(createPlayableItem(doc))
                    }
                }
            }
        }
        result.sendResult(items)
    }

    private fun getAudioDocuments(): List<Document> {
        return runBlocking {
            // Only show audio if the experimental feature is enabled
            if (!com.app.traveldocs.data.local.FeatureFlags.isAudioPlaybackEnabled(applicationContext)) return@runBlocking emptyList()
            documentRepository.getAll("default-member").first()
                .filter { it.format == DocumentFormat.AUDIO }
        }
    }

    private fun createBrowsableItem(id: String, title: String, subtitle: String): MediaBrowserCompat.MediaItem {
        val desc = MediaDescriptionCompat.Builder()
            .setMediaId(id)
            .setTitle(title)
            .setSubtitle(subtitle)
            .build()
        return MediaBrowserCompat.MediaItem(desc, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE)
    }

    private fun createPlayableItem(doc: Document): MediaBrowserCompat.MediaItem {
        val desc = MediaDescriptionCompat.Builder()
            .setMediaId(doc.id)
            .setTitle(doc.originalFileName ?: "Audio")
            .setSubtitle(doc.tags.filter { !it.name.startsWith("__") }.joinToString(", ") { it.name })
            .build()
        return MediaBrowserCompat.MediaItem(desc, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
    }

    private val mediaSessionCallback = object : MediaSessionCompat.Callback() {
        override fun onPlayFromMediaId(mediaId: String, extras: Bundle?) {
            DebugLogger.i(TAG, "Play: $mediaId")
            playDocument(mediaId)
        }

        override fun onPlay() {
            mediaPlayer?.start()
            updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
        }

        override fun onPause() {
            mediaPlayer?.pause()
            updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
        }

        override fun onStop() {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            updatePlaybackState(PlaybackStateCompat.STATE_STOPPED)
        }

        override fun onSkipToNext() {
            if (playlistIndex < playlist.size - 1) {
                playlistIndex++
                playDocument(playlist[playlistIndex].id)
            }
        }

        override fun onSkipToPrevious() {
            if (playlistIndex > 0) {
                playlistIndex--
                playDocument(playlist[playlistIndex].id)
            }
        }
    }

    private fun playDocument(docId: String) {
        try {
            mediaPlayer?.release()
            val bytes = runBlocking { fileStorage.retrieve(docId).getOrNull() } ?: return
            val doc = runBlocking { documentRepository.getById(docId) }

            // Write to temp file for MediaPlayer
            val tmpFile = File(cacheDir, "audio_playback.tmp")
            tmpFile.writeBytes(bytes)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(tmpFile.absolutePath)
                prepare()
                start()
                setOnCompletionListener { mediaSessionCallback.onSkipToNext() }
            }

            currentDocId = docId
            updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)

            // Update metadata for lock screen / Android Auto display
            val metadata = MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, docId)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, doc?.originalFileName ?: "Audio")
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, mediaPlayer!!.duration.toLong())
                .build()
            mediaSession.setMetadata(metadata)

            DebugLogger.i(TAG, "Playing: ${doc?.originalFileName} (${bytes.size / 1024}KB)")
        } catch (e: Exception) {
            DebugLogger.e(TAG, "Playback failed", e)
        }
    }

    private fun updatePlaybackState(state: Int) {
        val position = mediaPlayer?.currentPosition?.toLong() ?: 0L
        val stateBuilder = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_STOP or PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            )
            .setState(state, position, 1f)
        mediaSession.setPlaybackState(stateBuilder.build())
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        mediaSession.release()
        DebugLogger.i(TAG, "AudioPlaybackService destroyed")
        super.onDestroy()
    }
}

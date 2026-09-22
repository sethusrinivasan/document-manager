package com.app.paperstow.presentation.tags

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.app.paperstow.data.local.TagColorStore

@Composable
fun TagMark(
    tagName: String,
    store: TagColorStore,
    size: Dp = 32.dp,
    rounded: Boolean = true
) {
    val image = remember(tagName, store.imageFile(tagName)?.length()) {
        store.imageFile(tagName)?.let { BitmapFactory.decodeFile(it.absolutePath) }
    }
    val shape = if (rounded) CircleShape else RoundedCornerShape(8.dp)
    if (image != null) {
        Image(
            bitmap = image.asImageBitmap(),
            contentDescription = tagName,
            modifier = Modifier.size(size).clip(shape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            Modifier.size(size).clip(shape).background(Color(0xFFE8EEF5)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Folder, tagName, tint = Color(0xFF5C6B7A), modifier = Modifier.size(size * 0.55f))
        }
    }
}

/** Full-bleed cover for a folder tile — photo if set, otherwise a folder mark. */
@Composable
fun TagCover(
    tagName: String,
    store: TagColorStore,
    modifier: Modifier = Modifier,
    untagged: Boolean = false
) {
    val image = remember(tagName, store.imageFile(tagName)?.length()) {
        if (untagged) null else store.imageFile(tagName)?.let { BitmapFactory.decodeFile(it.absolutePath) }
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val icon = minOf(maxWidth, maxHeight) * 0.42f
        if (image != null) {
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = tagName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                Modifier.fillMaxSize().background(Color(0xFFE8EEF5)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Folder, tagName, tint = Color(0xFF5C6B7A), modifier = Modifier.size(icon))
            }
        }
    }
}

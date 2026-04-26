package com.fossift.asciicam.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fossift.asciicam.storage.CaptureRecord
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AsciiGalleryScreen(
    modifier: Modifier = Modifier,
    captures: List<CaptureRecord>,
    onBackToCamera: () -> Unit,
    onRefresh: () -> Unit,
    onDeleteCapture: (CaptureRecord) -> Unit,
    onExportCapture: (CaptureRecord) -> Unit,
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val selectedCapture = selectedIndex?.let { captures.getOrNull(it) }

    BackHandler(enabled = selectedIndex != null) {
        selectedIndex = null
    }

    LaunchedEffect(captures.size, selectedIndex) {
        if (selectedIndex != null && selectedCapture == null) {
            selectedIndex = null
        }
    }

    AnimatedContent(
        targetState = selectedIndex == null,
        label = "Gallery content transition",
        modifier = modifier.fillMaxSize(),
        transitionSpec = {
            (fadeIn() + expandVertically()).togetherWith(fadeOut() + shrinkVertically())
        },
    ) { isGridView ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF080A0F), Color(0xFF0D1218), Color(0xFF11161D)),
                    ),
                )
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header with back button and title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (isGridView) {
                    IconButton(
                        onClick = onBackToCamera,
                        modifier = Modifier.background(
                            color = Color(0x332A1E16),
                            shape = RoundedCornerShape(12.dp),
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to camera",
                            tint = Color(0xFFF4E8D8),
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isGridView) "MySCII Gallery" else "Full-screen view",
                        color = Color(0xFFF4E8D8),
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 22.sp,
                    )
                    Text(
                        text = if (isGridView) {
                            "${captures.size} saved captures"
                        } else {
                            selectedCapture?.let { prettyTimestamp(it.timestampUtc) } ?: ""
                        },
                        color = Color(0xFFCFBEAE),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                if (isGridView) {
                    TextButton(onClick = onRefresh) {
                        Text("Refresh")
                    }
                }
            }

            // Content area
            if (isGridView) {
                if (captures.isEmpty()) {
                    EmptyGalleryState(modifier = Modifier.weight(1f))
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 165.dp),
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 8.dp),
                    ) {
                        itemsIndexed(captures, key = { _, record -> record.pngFileName }) { index, record ->
                            GalleryCard(
                                record = record,
                                onOpen = { selectedIndex = index },
                                onExport = { onExportCapture(record) },
                            )
                        }
                    }
                }
            } else {
                GalleryViewer(
                    captures = captures,
                    initialIndex = selectedIndex ?: 0,
                    onPageChanged = { selectedIndex = it },
                    onDelete = { record -> onDeleteCapture(record) },
                    onExport = { record -> onExportCapture(record) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun GalleryCard(
    record: CaptureRecord,
    onOpen: () -> Unit,
    onExport: () -> Unit,
) {
    val context = LocalContext.current
    val captureFile = remember(record.pngFileName) {
        File(File(context.filesDir, "captures"), record.pngFileName)
    }
    val previewBitmap by rememberDecodedBitmap(captureFile, targetMaxDimension = 700)

    Crossfade(targetState = previewBitmap, label = "Preview image fade") {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0x662A1A11)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen),
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(128.dp)
                        .background(Color(0xFF120E0A), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0x55496D7E), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (it != null) {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "Saved capture preview (tap to view)",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp),
                            contentScale = ContentScale.Fit,
                        )
                    } else {
                        Text(
                            text = "Preview unavailable",
                            color = Color(0xFFBDAFA2),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                }
                Text(
                    text = prettyTimestamp(record.timestampUtc),
                    color = Color(0xFFDABDA6),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
                Button(
                    onClick = onExport,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F5E6A)),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text("Export", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun GalleryViewer(
    captures: List<CaptureRecord>,
    initialIndex: Int,
    onPageChanged: (Int) -> Unit,
    onDelete: (CaptureRecord) -> Unit,
    onExport: (CaptureRecord) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, (captures.size - 1).coerceAtLeast(0)),
        pageCount = { captures.size },
    )
    var showOriginal by remember { mutableStateOf(false) }
    val zoomLevels = remember { mutableStateMapOf<String, Float>() }

    LaunchedEffect(pagerState.currentPage) {
        if (captures.isNotEmpty()) {
            onPageChanged(pagerState.currentPage.coerceIn(0, captures.lastIndex))
        }
    }

    LaunchedEffect(captures.size) {
        if (captures.isNotEmpty()) {
            val safePage = pagerState.currentPage.coerceIn(0, captures.lastIndex)
            if (safePage != pagerState.currentPage) {
                pagerState.scrollToPage(safePage)
            }
        }
    }

    if (captures.isEmpty()) {
        EmptyGalleryState(modifier = modifier)
        return
    }

    val safeCurrentPage = pagerState.currentPage.coerceIn(0, captures.lastIndex)
    val currentRecord = captures[safeCurrentPage]
    val hasOriginalForCurrent = currentRecord.originalPngFileName != null
    val currentZoomKey = "${currentRecord.pngFileName}:${if (showOriginal) "original" else "ascii"}"
    val currentZoom = zoomLevels[currentZoomKey] ?: 1f

    LaunchedEffect(currentRecord.originalPngFileName) {
        if (!hasOriginalForCurrent) {
            showOriginal = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF10151C), RoundedCornerShape(20.dp))
            .border(1.dp, Color(0x55324A62), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Original capture",
                    color = Color(0xFFF6ECE1),
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 18.sp,
                )
                Text(
                    text = prettyTimestamp(currentRecord.timestampUtc),
                    color = Color(0xFFC8B7A8),
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.sp,
                )
            }
            Text(
                text = "${safeCurrentPage + 1} / ${captures.size}",
                color = Color(0xFFCFBEAE),
                style = MaterialTheme.typography.bodySmall,
            )
        }

        if (hasOriginalForCurrent) {
            Button(
                onClick = { showOriginal = !showOriginal },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF24374A)),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(if (showOriginal) "Show ASCII" else "Show Original")
            }
        }

        HorizontalPager(
            state = pagerState,
            userScrollEnabled = true,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) { page ->
            val record = captures[page]
            val context = LocalContext.current
            val capturesDir = remember { File(context.filesDir, "captures") }
            val asciiFile = remember(record.pngFileName) {
                File(capturesDir, record.pngFileName)
            }
            val originalFile = remember(record.originalPngFileName) {
                record.originalPngFileName?.let { File(capturesDir, it) }
            }
            val selectedFile = if (showOriginal) originalFile ?: asciiFile else asciiFile
            val zoomKey = "${record.pngFileName}:${if (showOriginal) "original" else "ascii"}"
            val imageZoom = zoomLevels[zoomKey] ?: 1f
            val previewBitmap by rememberDecodedBitmap(selectedFile, targetMaxDimension = 2200)
            val transformState = rememberTransformableState { zoomChange, _, _ ->
                val currentZoom = zoomLevels[zoomKey] ?: 1f
                val nextZoom = (currentZoom * zoomChange).coerceIn(1f, 4f)
                zoomLevels[zoomKey] = nextZoom
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0B0F14), RoundedCornerShape(18.dp))
                    .border(1.dp, Color(0x55324A62), RoundedCornerShape(18.dp))
                    .transformable(
                        state = transformState,
                        canPan = { false },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                val bitmap = previewBitmap
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Full-size saved capture",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                            .graphicsLayer(
                                scaleX = imageZoom,
                                scaleY = imageZoom,
                            ),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Text(
                        text = "Capture file missing",
                        color = Color(0xFFE2C4B0),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(20.dp),
                    )
                }
            }
        }

        Text(
            text = if (showOriginal) {
                "Showing original preview. Pinch to zoom. Zoom: ${"%.2f".format(currentZoom)}x"
            } else {
                "Showing ASCII render. Pinch to zoom. Zoom: ${"%.2f".format(currentZoom)}x"
            },
            color = Color(0xFFB7A89A),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { onDelete(currentRecord) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B3F2A)),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Delete")
            }
            Button(
                onClick = { onExport(currentRecord) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F5E6A)),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Export")
            }
        }
    }
}

@Composable
private fun EmptyGalleryState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0x332A1E16), RoundedCornerShape(20.dp))
            .border(1.dp, Color(0x554A382D), RoundedCornerShape(20.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "No captures yet",
            color = Color(0xFFF4E6D8),
            style = MaterialTheme.typography.titleMedium,
            fontSize = 18.sp,
        )
        Text(
            text = "Take a capture in the camera view and it will appear here.",
            color = Color(0xFFD3C0AF),
            style = MaterialTheme.typography.bodySmall,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
    }
}

private fun prettyTimestamp(raw: String): String {
    return raw.replace('T', ' ').removeSuffix("Z").take(19)
}

@Composable
private fun rememberDecodedBitmap(file: File, targetMaxDimension: Int): androidx.compose.runtime.State<android.graphics.Bitmap?> {
    return produceState<android.graphics.Bitmap?>(
        initialValue = null,
        key1 = file.absolutePath,
        key2 = targetMaxDimension,
    ) {
        value = withContext(Dispatchers.IO) {
            decodeBitmapSampled(file, targetMaxDimension)
        }
    }
}

private fun decodeBitmapSampled(file: File, targetMaxDimension: Int): android.graphics.Bitmap? {
    if (!file.exists() || targetMaxDimension <= 0) {
        return null
    }

    return runCatching {
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(file.absolutePath, boundsOptions)

        val width = boundsOptions.outWidth
        val height = boundsOptions.outHeight
        if (width <= 0 || height <= 0) {
            return@runCatching null
        }

        var inSampleSize = 1
        var halfWidth = width / 2
        var halfHeight = height / 2
        while (halfWidth / inSampleSize >= targetMaxDimension && halfHeight / inSampleSize >= targetMaxDimension) {
            inSampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize.coerceAtLeast(1)
            inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
        }
        BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
    }.getOrNull()
}

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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fossift.asciicam.R
import com.fossift.asciicam.storage.CaptureRecord
import com.fossift.asciicam.ui.theme.AlertRed
import com.fossift.asciicam.ui.theme.CyberCyan
import com.fossift.asciicam.ui.theme.DarkBackground
import com.fossift.asciicam.ui.theme.DarkOutline
import com.fossift.asciicam.ui.theme.DarkSurface
import com.fossift.asciicam.ui.theme.DarkSurfaceElevated
import com.fossift.asciicam.ui.theme.TextMuted
import com.fossift.asciicam.ui.theme.TextPrimary
import com.fossift.asciicam.ui.theme.TextSecondary
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
    onCopyAscii: ((CaptureRecord) -> Unit)? = null,
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var recordToDelete by remember { mutableStateOf<CaptureRecord?>(null) }
    val selectedCapture = selectedIndex?.let { captures.getOrNull(it) }

    BackHandler(enabled = selectedIndex != null) {
        selectedIndex = null
    }

    LaunchedEffect(captures.size, selectedIndex) {
        if (selectedIndex != null && selectedCapture == null) {
            selectedIndex = null
        }
    }

    // Delete Confirmation Dialog
    recordToDelete?.let { targetRecord ->
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = {
                Text(
                    text = stringResource(id = R.string.delete_confirm_title),
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            text = {
                Text(
                    text = stringResource(id = R.string.delete_confirm_desc),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCapture(targetRecord)
                        recordToDelete = null
                        if (selectedIndex != null && captures.size <= 1) {
                            selectedIndex = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                ) {
                    Text(stringResource(id = R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) {
                    Text(stringResource(id = R.string.cancel), color = TextSecondary)
                }
            },
            containerColor = DarkSurface,
        )
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
                .navigationBarsPadding()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(DarkBackground, DarkSurface, Color(0xFF101622)),
                    ),
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                IconButton(
                    onClick = {
                        if (isGridView) onBackToCamera() else selectedIndex = null
                    },
                    modifier = Modifier
                        .background(DarkSurfaceElevated, RoundedCornerShape(14.dp))
                        .border(1.dp, DarkOutline, RoundedCornerShape(14.dp)),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary,
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isGridView) stringResource(id = R.string.gallery_title) else "Capture Viewer",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (isGridView) {
                            "${captures.size} photos saved"
                        } else {
                            selectedCapture?.let { prettyTimestamp(it.timestampUtc) } ?: ""
                        },
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                if (isGridView) {
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier
                            .background(DarkSurfaceElevated, RoundedCornerShape(14.dp))
                            .border(1.dp, DarkOutline, RoundedCornerShape(14.dp)),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = CyberCyan,
                        )
                    }
                }
            }

            // Content Area
            if (isGridView) {
                if (captures.isEmpty()) {
                    EmptyGalleryState(modifier = Modifier.weight(1f))
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
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
                                onCopy = onCopyAscii?.let { cb -> { cb(record) } },
                            )
                        }
                    }
                }
            } else {
                GalleryViewer(
                    captures = captures,
                    initialIndex = selectedIndex ?: 0,
                    onPageChanged = { selectedIndex = it },
                    onRequestDelete = { record -> recordToDelete = record },
                    onExport = { record -> onExportCapture(record) },
                    onCopy = onCopyAscii,
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
    onCopy: (() -> Unit)?,
) {
    val context = LocalContext.current
    val captureFile = remember(record.pngFileName) {
        File(File(context.filesDir, "captures"), record.pngFileName)
    }
    val previewBitmap by rememberDecodedBitmap(captureFile, targetMaxDimension = 600)

    Crossfade(targetState = previewBitmap, label = "Preview image fade") { bmp ->
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkOutline),
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
                        .height(130.dp)
                        .background(Color(0xFF070A0F), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Saved capture preview",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp),
                            contentScale = ContentScale.Fit,
                        )
                    } else {
                        Text(
                            text = "Loading…",
                            color = TextMuted,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Text(
                    text = prettyTimestamp(record.timestampUtc),
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 2.dp),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (onCopy != null) {
                        Button(
                            onClick = onCopy,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2B3C)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp), tint = CyberCyan)
                            Spacer(Modifier.width(4.dp))
                            Text("Copy", fontSize = 11.sp, color = CyberCyan)
                        }
                    }
                    Button(
                        onClick = onExport,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF004F58)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(14.dp), tint = TextPrimary)
                        Spacer(Modifier.width(4.dp))
                        Text("Share", fontSize = 11.sp, color = TextPrimary)
                    }
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
    onRequestDelete: (CaptureRecord) -> Unit,
    onExport: (CaptureRecord) -> Unit,
    onCopy: ((CaptureRecord) -> Unit)?,
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
            .background(DarkSurfaceElevated, RoundedCornerShape(22.dp))
            .border(1.dp, DarkOutline, RoundedCornerShape(22.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Page position indicator & Original toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                Text(
                    text = prettyTimestamp(currentRecord.timestampUtc),
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${safeCurrentPage + 1} of ${captures.size}",
                    color = CyberCyan,
                    style = MaterialTheme.typography.labelMedium,
                )
            }

            if (hasOriginalForCurrent) {
                Button(
                    onClick = { showOriginal = !showOriginal },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showOriginal) Color(0xFF004F58) else Color(0xFF1E2B3C),
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(if (showOriginal) "Show ASCII" else "Show Original", fontSize = 12.sp)
                }
            }
        }

        // Zoomable Preview Area
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = currentZoom <= 1.05f,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) { page ->
            val record = captures[page]
            val context = LocalContext.current
            val capturesDir = remember { File(context.filesDir, "captures") }
            val asciiFile = remember(record.pngFileName) { File(capturesDir, record.pngFileName) }
            val originalFile = remember(record.originalPngFileName) {
                record.originalPngFileName?.let { File(capturesDir, it) }
            }
            val selectedFile = if (showOriginal) originalFile ?: asciiFile else asciiFile
            val zoomKey = "${record.pngFileName}:${if (showOriginal) "original" else "ascii"}"
            val imageZoom = zoomLevels[zoomKey] ?: 1f
            val previewBitmap by rememberDecodedBitmap(selectedFile, targetMaxDimension = 2200)
            val transformState = rememberTransformableState { zoomChange, _, _ ->
                val curr = zoomLevels[zoomKey] ?: 1f
                zoomLevels[zoomKey] = (curr * zoomChange).coerceIn(1f, 4f)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF05070B), RoundedCornerShape(18.dp))
                    .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(18.dp))
                    .transformable(state = transformState, canPan = { false }),
                contentAlignment = Alignment.Center,
            ) {
                val bitmap = previewBitmap
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Full-size capture",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .graphicsLayer(scaleX = imageZoom, scaleY = imageZoom),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Text(
                        text = "File missing",
                        color = AlertRed,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        Text(
            text = "Pinch to zoom • ${"%.1f".format(currentZoom)}x",
            color = TextMuted,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        // Bottom Action Row: Delete, Copy ASCII, Export
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Button(
                onClick = { onRequestDelete(currentRecord) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A1815)),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = AlertRed, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Delete", color = AlertRed)
            }

            if (onCopy != null) {
                Button(
                    onClick = { onCopy(currentRecord) },
                    modifier = Modifier.weight(1.2f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2B3C)),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = CyberCyan, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Copy ASCII", color = CyberCyan, maxLines = 1)
                }
            }

            Button(
                onClick = { onExport(currentRecord) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF004F58)),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Default.Share, contentDescription = "Export", tint = TextPrimary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Share", color = TextPrimary)
            }
        }
    }
}

@Composable
private fun EmptyGalleryState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkSurfaceElevated, RoundedCornerShape(20.dp))
            .border(1.dp, DarkOutline, RoundedCornerShape(20.dp))
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(id = R.string.no_captures_title),
            color = TextPrimary,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(id = R.string.no_captures_desc),
            color = TextSecondary,
            style = MaterialTheme.typography.bodySmall,
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

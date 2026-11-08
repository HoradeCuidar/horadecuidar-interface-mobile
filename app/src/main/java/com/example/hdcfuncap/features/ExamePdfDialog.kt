package com.example.hdcfuncap.features

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.hdcfuncap.util.rethrowIfCancellation
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun ExamePdfDialog(document: PdfExame, onClose: () -> Unit) {
    var index by rememberSaveable(document.file.path) { mutableIntStateOf(0) }
    var pages by remember(document.file.path) { mutableIntStateOf(0) }
    var bitmap by remember(document.file.path) { mutableStateOf<Bitmap?>(null) }
    var error by remember(document.file.path) { mutableStateOf<String?>(null) }
    var loading by remember(document.file.path) { mutableStateOf(true) }
    var zoom by remember(index) { mutableFloatStateOf(1f) }
    var offset by remember(index) { mutableStateOf(Offset.Zero) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(document.file.path, index) {
        loading = true
        bitmap = null
        error = null
        try {
            val rendered = withContext(Dispatchers.IO) { renderExamePage(document.file, index) }
            pages = rendered.second
            bitmap = rendered.first
        } catch (e: Exception) {
            e.rethrowIfCancellation()
            error = if (e is SecurityException) "Este PDF está protegido e não pode ser aberto no aplicativo."
                else "Não foi possível exibir este PDF. Tente baixá-lo para abrir em outro leitor."
        } finally {
            loading = false
        }
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Scaffold(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Fechar PDF") }
                    Text(document.titulo, Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    IconButton(onClick = { zoom = (zoom - 0.5f).coerceAtLeast(1f); offset = Offset.Zero },
                        enabled = !loading && bitmap != null && zoom > 1f) {
                        Icon(Icons.Outlined.ZoomOut, "Diminuir zoom")
                    }
                    IconButton(onClick = { zoom = (zoom + 0.5f).coerceAtMost(4f); offset = Offset.Zero },
                        enabled = !loading && bitmap != null && zoom < 4f) {
                        Icon(Icons.Outlined.ZoomIn, "Aumentar zoom")
                    }
                }
            },
            bottomBar = {
                Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { index-- }, enabled = !loading && index > 0) {
                        Icon(Icons.AutoMirrored.Filled.NavigateBefore, "Página anterior")
                    }
                    Text(if (pages > 0) "${index + 1} de $pages" else "PDF", Modifier.padding(horizontal = 16.dp))
                    IconButton(onClick = { index++ }, enabled = !loading && index + 1 < pages) {
                        Icon(Icons.AutoMirrored.Filled.NavigateNext, "Próxima página")
                    }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding).clipToBounds().onSizeChanged { viewport = it }, contentAlignment = Alignment.Center) {
                if (loading) CircularProgressIndicator()
                error?.let { Text(it, Modifier.padding(24.dp), color = MaterialTheme.colorScheme.error) }
                bitmap?.let { image ->
                    val transformState = rememberTransformableState { scaleChange, pan, _ ->
                        zoom = (zoom * scaleChange).coerceIn(1f, 4f)
                        val fit = minOf(viewport.width.toFloat() / image.width, viewport.height.toFloat() / image.height)
                        val maxX = ((image.width * fit * zoom - viewport.width) / 2).coerceAtLeast(0f)
                        val maxY = ((image.height * fit * zoom - viewport.height) / 2).coerceAtLeast(0f)
                        offset = if (zoom == 1f) Offset.Zero else Offset(
                            (offset.x + pan.x).coerceIn(-maxX, maxX),
                            (offset.y + pan.y).coerceIn(-maxY, maxY))
                    }
                    Image(image.asImageBitmap(), "Página ${index + 1} do exame", contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().transformable(transformState).graphicsLayer {
                            scaleX = zoom
                            scaleY = zoom
                            translationX = offset.x
                            translationY = offset.y
                        })
                }
            }
        }
    }
}

private fun renderExamePage(file: File, index: Int): Pair<Bitmap, Int> {
    val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    val renderer = try { PdfRenderer(descriptor) } catch (e: Exception) { descriptor.close(); throw e }
    return renderer.use { pdf ->
        require(pdf.pageCount > 0)
        pdf.openPage(index.coerceIn(0, pdf.pageCount - 1)).use { page ->
            val scale = minOf(1600f / page.width.coerceAtLeast(1), 2000f / page.height.coerceAtLeast(1), 2f)
            val bitmap = Bitmap.createBitmap((page.width * scale).toInt().coerceAtLeast(1),
                (page.height * scale).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
            try {
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap to pdf.pageCount
            } catch (e: Exception) { bitmap.recycle(); throw e }
        }
    }
}

package com.example.hdcfuncap.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.hdcfuncap.util.normalizarUrlImagem

@Composable
internal fun ExercicioImagem(
    urlImagem: String?,
    modifier: Modifier = Modifier,
    fallbackIconSize: Dp = 24.dp
) {
    val imagem = urlImagem.normalizarUrlImagem()
    if (imagem == null) {
        ExercicioImagemFallback(modifier, fallbackIconSize)
        return
    }
    SubcomposeAsyncImage(
        model = imagem,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFEAF8F0)),
        loading = { ExercicioImagemFallback(Modifier.fillMaxSize(), fallbackIconSize) },
        error = { ExercicioImagemFallback(Modifier.fillMaxSize(), fallbackIconSize) }
    )
}

@Composable
private fun ExercicioImagemFallback(modifier: Modifier, iconSize: Dp) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFEAF8F0)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.DirectionsWalk,
            contentDescription = null,
            tint = Color(0xFF57C47A),
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
internal fun TagsExercicio(tags: List<String>) {
    Text(
        text = tags.joinToString(" • "),
        color = Color(0xFF6B7280),
        fontSize = 12.sp,
        lineHeight = 17.sp
    )
}

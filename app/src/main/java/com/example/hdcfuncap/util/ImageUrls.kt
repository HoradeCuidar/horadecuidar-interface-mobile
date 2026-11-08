package com.example.hdcfuncap.util

internal fun String?.normalizarUrlImagem(): String? {
    val url = orEmpty().trim()
    if (url.isBlank()) return null
    return url.takeIf {
        it.startsWith("https://", ignoreCase = true) ||
            it.startsWith("http://", ignoreCase = true)
    }
}

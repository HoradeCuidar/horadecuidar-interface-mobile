package com.example.hdcfuncap.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImageUrlsTest {
    @Test
    fun handlesMissingOrBlankImages() {
        assertNull((null as String?).normalizarUrlImagem())
        assertNull("".normalizarUrlImagem())
        assertNull("   ".normalizarUrlImagem())
    }

    @Test
    fun preservesSupportedUrlsAndTrimsWhitespace() {
        assertEquals("https://example.com/image.png", " https://example.com/image.png ".normalizarUrlImagem())
        assertEquals("http://example.com/image.png", "http://example.com/image.png".normalizarUrlImagem())
        assertEquals("HTTPS://example.com/image.png", "HTTPS://example.com/image.png".normalizarUrlImagem())
    }

    @Test
    fun rejectsUnsupportedSchemesAndRelativePaths() {
        assertNull("file:///image.png".normalizarUrlImagem())
        assertNull("content://image.png".normalizarUrlImagem())
        assertNull("/image.png".normalizarUrlImagem())
        assertNull("data:image/png;base64,abc".normalizarUrlImagem())
    }
}

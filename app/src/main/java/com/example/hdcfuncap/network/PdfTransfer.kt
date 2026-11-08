package com.example.hdcfuncap.network

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

internal const val MAX_EXAME_PDF_BYTES = 20L * 1024 * 1024

internal fun copyExamePdf(input: InputStream, output: OutputStream, checkCancelled: () -> Unit = {}): Long {
    val header = ByteArray(5)
    var headerSize = 0
    while (headerSize < header.size) {
        checkCancelled()
        val read = input.read(header, headerSize, header.size - headerSize)
        if (read < 0) throw IOException("PDF incompleto")
        headerSize += read
    }
    if (!header.contentEquals("%PDF-".toByteArray(Charsets.US_ASCII))) throw IOException("Arquivo não é um PDF")
    output.write(header)
    var size = header.size.toLong()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var tail = ""
    while (true) {
        checkCancelled()
        val read = input.read(buffer)
        if (read < 0) break
        size += read
        if (size > MAX_EXAME_PDF_BYTES) throw IOException("PDF acima do limite de 20 MB")
        output.write(buffer, 0, read)
        tail = (tail + String(buffer, 0, read, Charsets.ISO_8859_1)).takeLast(1024)
    }
    if (!tail.contains("%%EOF")) throw IOException("PDF incompleto")
    return size
}

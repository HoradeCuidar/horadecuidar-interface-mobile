package com.example.hdcfuncap.network

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.CancellationException
import org.junit.Assert.*
import org.junit.Test

class PdfTransferTest {
    @Test fun validPdfIsCopiedUnchanged() {
        val bytes = "%PDF-1.4\nexample\n%%EOF\n".toByteArray()
        val destination = ByteArrayOutputStream()
        assertEquals(bytes.size.toLong(), copyExamePdf(ByteArrayInputStream(bytes), destination))
        assertArrayEquals(bytes, destination.toByteArray())
    }

    @Test fun invalidHeaderAndTruncatedFilesAreRejected() {
        for (contents in listOf("", "%PD", "<html>error</html>", "%PDF-1.4\ntruncated")) {
            assertThrows(IOException::class.java) {
                copyExamePdf(ByteArrayInputStream(contents.toByteArray()), ByteArrayOutputStream())
            }
        }
    }

    @Test fun largeFileIsRejectedWithoutLoadingItInMemory() {
        var position = 0L
        val header = "%PDF-".toByteArray()
        val source = object : java.io.InputStream() {
            override fun read(): Int = when {
                position < header.size -> header[(position++).toInt()].toInt()
                position++ < MAX_EXAME_PDF_BYTES + 1 -> 65
                else -> -1
            }
        }
        val destination = object : OutputStream() { override fun write(value: Int) {} }
        assertThrows(IOException::class.java) { copyExamePdf(source, destination) }
    }

    @Test fun cancellationInterruptsTheCopy() {
        val bytes = ("%PDF-1.4\n" + "a".repeat(20000) + "\n%%EOF").toByteArray()
        var checks = 0
        assertThrows(CancellationException::class.java) {
            copyExamePdf(ByteArrayInputStream(bytes), ByteArrayOutputStream()) {
                if (++checks > 2) throw CancellationException()
            }
        }
    }

    @Test fun eofMayCrossReadBoundaries() {
        val bytes = ("%PDF-" + "a".repeat(8190) + "%%EOF\n").toByteArray()
        val output = ByteArrayOutputStream()
        copyExamePdf(ByteArrayInputStream(bytes), output)
        assertArrayEquals(bytes, output.toByteArray())
    }
}

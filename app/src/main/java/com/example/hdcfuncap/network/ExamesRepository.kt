package com.example.hdcfuncap.network

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.example.hdcfuncap.storage.UserPreferences
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

internal class ExamesRepository(private val api: AuthApi, context: Context) {
    private val appContext = context.applicationContext
    private val preferences = UserPreferences(appContext)
    private val files = ConcurrentHashMap.newKeySet<File>()
    private val disposed = AtomicBoolean(false)

    suspend fun listar(patientId: Long): List<ExameResponse> {
        requirePatient(patientId)
        return api.getExames(patientId).examesDoPaciente(patientId).also { requirePatient(patientId) }
    }

    suspend fun obterPdf(patientId: Long, exameId: Long): File = withContext(Dispatchers.IO) {
        requirePatient(patientId)
        check(!disposed.get())
        val directory = File(appContext.cacheDir, "exames").apply { if (!exists() && !mkdirs()) throw IOException("Cache indisponível") }
        val file = File.createTempFile("exame-", ".pdf", directory)
        files.add(file)
        try {
            api.getArquivoExame(exameId).use { body ->
                if (body.contentLength() > MAX_EXAME_PDF_BYTES) throw IOException("PDF acima do limite de 20 MB")
                val jobContext = currentCoroutineContext()
                body.byteStream().use { input -> file.outputStream().use { output ->
                    copyExamePdf(input, output) { jobContext.ensureActive() }
                } }
            }
            requirePatient(patientId)
            currentCoroutineContext().ensureActive()
            check(!disposed.get())
            file
        } catch (e: Exception) {
            liberar(file)
            throw e
        }
    }

    suspend fun salvarPdf(patientId: Long, exameId: Long, destination: Uri) = withContext(Dispatchers.IO) {
        var file: File? = null
        var complete = false
        try {
            file = obterPdf(patientId, exameId)
            requirePatient(patientId)
            val jobContext = currentCoroutineContext()
            val output = appContext.contentResolver.openOutputStream(destination, "w") ?: throw IOException("Destino indisponível")
            output.use { stream -> file.inputStream().use { copyExamePdf(it, stream) { jobContext.ensureActive() } } }
            requirePatient(patientId)
            complete = true
        } finally {
            file?.let(::liberar)
            if (!complete) runCatching { DocumentsContract.deleteDocument(appContext.contentResolver, destination) }
        }
    }

    fun liberar(file: File) {
        files.remove(file)
        file.delete()
    }

    fun limpar() {
        files.toList().forEach(::liberar)
    }

    fun fechar() {
        disposed.set(true)
        limpar()
    }

    private suspend fun requirePatient(patientId: Long) {
        check(preferences.pacienteId.firstOrNull() == patientId) { "Sessão alterada" }
    }
}

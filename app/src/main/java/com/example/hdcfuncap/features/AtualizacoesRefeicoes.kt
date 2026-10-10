package com.example.hdcfuncap.features

import java.util.concurrent.atomic.AtomicLong

// Invalida os caches das outras telas somente depois de um registro confirmado.
internal object AtualizacoesRefeicoes {
    private val revisao = AtomicLong()
    val versao: Long get() = revisao.get()

    fun notificarRegistro() {
        revisao.incrementAndGet()
    }
}

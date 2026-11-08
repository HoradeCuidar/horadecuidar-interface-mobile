package com.example.hdcfuncap.network

internal fun OrientacaoFuncionalResponse.tagsNomes(): List<String> =
    tags.orEmpty()
        .mapNotNull { tag -> tag.nome?.trim()?.takeIf { it.isNotBlank() } }
        .distinct()

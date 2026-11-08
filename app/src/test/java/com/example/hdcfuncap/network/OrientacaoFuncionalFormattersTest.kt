package com.example.hdcfuncap.network

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OrientacaoFuncionalFormattersTest {
    @Test
    fun handlesAbsentTags() {
        assertTrue(parse("{}").tagsNomes().isEmpty())
        assertTrue(parse("""{"tags":[]}""").tagsNomes().isEmpty())
    }

    @Test
    fun trimsNamesAndRemovesBlankAndRepeatedTagsPreservingOrder() {
        val response = parse("""
            {"tags":[{"nome":" Alongamento "},{"nome":null},{"nome":" "},
             {"nome":"Alongamento"},{"nome":"Mobilidade"}]}
        """)
        assertEquals(listOf("Alongamento", "Mobilidade"), response.tagsNomes())
    }

    @Test
    fun preservesCaseSensitiveTagNames() {
        assertEquals(listOf("Leve", "leve"),
            parse("""{"tags":[{"nome":"Leve"},{"nome":"leve"}]}""").tagsNomes())
    }

    private fun parse(json: String): OrientacaoFuncionalResponse =
        Gson().fromJson(json, OrientacaoFuncionalResponse::class.java)
}

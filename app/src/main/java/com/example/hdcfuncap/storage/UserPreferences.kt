package com.example.hdcfuncap.storage // Verifique se o pacote é esse mesmo

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "configs_usuario")

class UserPreferences(private val context: Context) {

    companion object {
        val USERNAME_KEY = stringPreferencesKey("saved_username")
        val REMEMBER_ME_KEY = booleanPreferencesKey("remember_me")
        val TOKEN_KEY = stringPreferencesKey("jwt_token")

        val PACIENTE_ID_KEY = longPreferencesKey("paciente_id")
        val PACIENTE_NOME_KEY = stringPreferencesKey("paciente_nome")
        val FONT_SIZE_KEY = stringPreferencesKey("font_size")
    }
    val pacienteId: Flow<Long?> = context.dataStore.data.map { preferences ->
        preferences[PACIENTE_ID_KEY]
    }

    val pacienteNome: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PACIENTE_NOME_KEY] ?: ""
    }

    val token: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[TOKEN_KEY]
    }

    val savedUsername: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USERNAME_KEY] ?: ""
    }

    val isRememberMeChecked: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[REMEMBER_ME_KEY] ?: false
    }

    val fontSize: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[FONT_SIZE_KEY] ?: "media"
    }

    suspend fun savePaciente(id: Long, nome: String) {
        context.dataStore.edit { preferences ->
            preferences[PACIENTE_ID_KEY] = id
            preferences[PACIENTE_NOME_KEY] = nome
        }
    }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.remove(TOKEN_KEY)
            preferences.remove(PACIENTE_ID_KEY)
            preferences.remove(PACIENTE_NOME_KEY)
        }
    }

    suspend fun saveFontSize(fontSize: String) {
        context.dataStore.edit { preferences ->
            preferences[FONT_SIZE_KEY] = fontSize
        }
    }

    suspend fun saveUser(username: String, remember: Boolean) {
        context.dataStore.edit { preferences ->
            if (remember) {
                preferences[USERNAME_KEY] = username
                preferences[REMEMBER_ME_KEY] = true
            } else {
                preferences.remove(USERNAME_KEY)
                preferences[REMEMBER_ME_KEY] = false
            }
        }
    }

    suspend fun getAuthToken(): String? {
        return token.firstOrNull()
    }
}

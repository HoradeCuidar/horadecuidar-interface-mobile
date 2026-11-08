package com.example.hdcfuncap.storage // Verifique se o pacote é esse mesmo

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "configs_usuario")

data class SavedPatientSession(val pacienteId: Long, val token: String)

class UserPreferences(private val context: Context) {

    companion object {
        val USERNAME_KEY = stringPreferencesKey("saved_username")
        val REMEMBER_ME_KEY = booleanPreferencesKey("remember_me")
        val TOKEN_KEY = stringPreferencesKey("jwt_token")

        val PACIENTE_ID_KEY = longPreferencesKey("paciente_id")
        val PACIENTE_NOME_KEY = stringPreferencesKey("paciente_nome")
        val FONT_SIZE_KEY = stringPreferencesKey("font_size")
        val WATER_REMINDERS_KEY = booleanPreferencesKey("water_reminders_enabled")
        val CHECK_IN_REMINDERS_KEY = booleanPreferencesKey("check_in_reminders_enabled")
    }
    private val safeData = context.dataStore.data.catch { exception ->
        if (exception is IOException) {
            emit(emptyPreferences())
        } else {
            throw exception
        }
    }

    val pacienteId: Flow<Long?> = safeData.map { preferences ->
        preferences[PACIENTE_ID_KEY]
    }

    val pacienteNome: Flow<String> = safeData.map { preferences ->
        preferences[PACIENTE_NOME_KEY] ?: ""
    }

    val token: Flow<String?> = safeData.map { preferences ->
        preferences[TOKEN_KEY]
    }

    val savedUsername: Flow<String> = safeData.map { preferences ->
        preferences[USERNAME_KEY] ?: ""
    }

    val isRememberMeChecked: Flow<Boolean> = safeData.map { preferences ->
        preferences[REMEMBER_ME_KEY] ?: false
    }

    val fontSize: Flow<String> = safeData.map { preferences ->
        preferences[FONT_SIZE_KEY] ?: "media"
    }

    private val reminderData: Flow<Preferences?> = context.dataStore.data
        .map<Preferences, Preferences?> { it }
        .catch { exception ->
            if (exception is IOException) emit(null) else throw exception
        }

    val waterRemindersEnabled: Flow<Boolean> = reminderData.map { preferences ->
        preferences != null && (preferences[WATER_REMINDERS_KEY] ?: true)
    }

    val checkInRemindersEnabled: Flow<Boolean> = reminderData.map { preferences ->
        preferences != null && (preferences[CHECK_IN_REMINDERS_KEY] ?: true)
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

    suspend fun saveAuthenticatedSession(
        token: String,
        pacienteId: Long,
        pacienteNome: String,
        username: String,
        remember: Boolean
    ) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
            preferences[PACIENTE_ID_KEY] = pacienteId
            preferences[PACIENTE_NOME_KEY] = pacienteNome
            if (remember) {
                preferences[USERNAME_KEY] = username
                preferences[REMEMBER_ME_KEY] = true
            } else {
                preferences.remove(USERNAME_KEY)
                preferences[REMEMBER_ME_KEY] = false
            }
        }
    }

    suspend fun saveWaterRemindersEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[WATER_REMINDERS_KEY] = enabled
        }
    }

    suspend fun saveCheckInRemindersEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[CHECK_IN_REMINDERS_KEY] = enabled
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

    suspend fun getPatientSession(): SavedPatientSession? {
        val preferences = safeData.firstOrNull() ?: return null
        val id = preferences[PACIENTE_ID_KEY] ?: return null
        val token = preferences[TOKEN_KEY]?.takeIf { it.isNotBlank() } ?: return null
        return SavedPatientSession(id, token)
    }
}

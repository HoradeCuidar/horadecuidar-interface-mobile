package com.example.hdcfuncap.features

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.hdcfuncap.network.AuthApi
import com.example.hdcfuncap.network.PrescricaoMedicamentoResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PrescricoesViewModel(private val authApi: AuthApi) : ViewModel() {

    private val _prescricoesMedicamentos =
        MutableStateFlow<List<PrescricaoMedicamentoResponse>>(emptyList())
    val prescricoesMedicamentos: StateFlow<List<PrescricaoMedicamentoResponse>> =
        _prescricoesMedicamentos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun carregarPrescricoesMedicamentos(pacienteId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                _prescricoesMedicamentos.value = authApi.getPrescricoesMedicamentos(pacienteId)
            } catch (e: Exception) {
                _errorMessage.value = "Não foi possível carregar suas prescrições."
                _prescricoesMedicamentos.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}

class PrescricoesViewModelFactory(private val authApi: AuthApi) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PrescricoesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PrescricoesViewModel(authApi) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

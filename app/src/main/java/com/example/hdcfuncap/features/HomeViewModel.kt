package com.example.hdcfuncap.features

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.hdcfuncap.network.AuthApi
import com.example.hdcfuncap.network.MedicamentoHojeResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(private val authApi: AuthApi) : ViewModel() {

    private val _medicamentos = MutableStateFlow<List<MedicamentoHojeResponse>>(emptyList())
    val medicamentos: StateFlow<List<MedicamentoHojeResponse>> = _medicamentos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _pacienteNome = MutableStateFlow("João da Silva")
    val pacienteNome: StateFlow<String> = _pacienteNome.asStateFlow()
    fun carregarDados(pacienteId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val resposta = authApi.getMedicamentosHoje(pacienteId)
                _medicamentos.value = resposta
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}

class HomeViewModelFactory(private val authApi: AuthApi) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(authApi) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
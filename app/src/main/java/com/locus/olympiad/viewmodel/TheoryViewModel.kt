package com.locus.olympiad.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locus.olympiad.data.dao.TheoryModuleDao
import com.locus.olympiad.data.model.TheoryModule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TheoryUiState(
    val category: String = "Combinatorics",
    val modules: List<TheoryModule> = emptyList(),
    val selectedModule: TheoryModule? = null,
    val isLoading: Boolean = false,
    val isReady: Boolean = false
)

@HiltViewModel
class TheoryViewModel @Inject constructor(
    private val theoryModuleDao: TheoryModuleDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(TheoryUiState(isLoading = true))
    val uiState: StateFlow<TheoryUiState> = _uiState.asStateFlow()

    init {
        loadCategory("Combinatorics")
    }

    fun loadCategory(category: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(category = category, isLoading = true, isReady = false)
            val modules = theoryModuleDao.getModulesByCategorySync(category)
            val initialModule = modules.firstOrNull()
            val ready = initialModule != null &&
                    initialModule.latexContent.isNotBlank() &&
                    !initialModule.heuristicsAndTricks.isNullOrBlank()

            _uiState.value = _uiState.value.copy(
                category = category,
                modules = modules,
                selectedModule = initialModule,
                isLoading = false,
                isReady = ready
            )
        }
    }

    fun selectModule(module: TheoryModule) {
        val ready = module.latexContent.isNotBlank() && !module.heuristicsAndTricks.isNullOrBlank()
        _uiState.value = _uiState.value.copy(
            selectedModule = module,
            isReady = ready
        )
    }
}

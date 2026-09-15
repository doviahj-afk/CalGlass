package com.joshua.glasscalc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CalcMode { STANDARD, FORMULA }

data class CalcUiState(
    val mode: CalcMode = CalcMode.STANDARD,

    // Standard mode
    val expression: String = "",
    val standardResult: String? = null,
    val standardError: String? = null,

    // Formula mode
    val formulaName: String = "",
    val formulaCode: String = "result = ",
    val formulaResult: String? = null,
    val formulaError: String? = null,
    val formulaStdout: String = "",
    val savedFormulas: List<SavedFormula> = emptyList()
)

class CalculatorViewModel(private val formulaStore: FormulaStore) : ViewModel() {

    private val _uiState = MutableStateFlow(CalcUiState())
    val uiState: StateFlow<CalcUiState> = _uiState

    init {
        viewModelScope.launch {
            formulaStore.formulas.collect { list ->
                _uiState.update { it.copy(savedFormulas = list) }
            }
        }
    }

    fun setMode(mode: CalcMode) {
        _uiState.update { it.copy(mode = mode) }
    }

    // ---------- Standard calculator ----------

    fun onDigit(symbol: String) {
        _uiState.update {
            it.copy(
                expression = it.expression + symbol,
                standardResult = null,
                standardError = null
            )
        }
    }

    fun onClear() {
        _uiState.update { it.copy(expression = "", standardResult = null, standardError = null) }
    }

    fun onBackspace() {
        _uiState.update {
            it.copy(expression = it.expression.dropLast(1), standardError = null)
        }
    }

    fun onEquals() {
        val expr = _uiState.value.expression
        if (expr.isBlank()) return
        // The "basic" calculator is really just the same Python engine, with
        // the whole expression run as `result = <what the user typed>`.
        val outcome = PythonBridge.runFormula("result = $expr")
        _uiState.update {
            if (outcome.ok) {
                it.copy(standardResult = outcome.result, standardError = null)
            } else {
                it.copy(standardResult = null, standardError = outcome.error)
            }
        }
    }

    // ---------- Custom Python formula mode ----------

    fun onFormulaNameChange(name: String) {
        _uiState.update { it.copy(formulaName = name) }
    }

    fun onFormulaCodeChange(code: String) {
        _uiState.update { it.copy(formulaCode = code) }
    }

    fun runCurrentFormula() {
        val code = _uiState.value.formulaCode
        val outcome = PythonBridge.runFormula(code)
        _uiState.update {
            it.copy(
                formulaResult = outcome.result,
                formulaError = outcome.error,
                formulaStdout = outcome.stdout
            )
        }
    }

    fun saveCurrentFormula() {
        val name = _uiState.value.formulaName.ifBlank { return }
        val code = _uiState.value.formulaCode
        viewModelScope.launch {
            formulaStore.save(SavedFormula(name, code))
        }
    }

    fun loadFormula(formula: SavedFormula) {
        _uiState.update {
            it.copy(
                formulaName = formula.name,
                formulaCode = formula.code,
                formulaResult = null,
                formulaError = null,
                formulaStdout = ""
            )
        }
    }

    fun runSavedFormula(formula: SavedFormula) {
        loadFormula(formula)
        runCurrentFormula()
    }

    fun deleteFormula(name: String) {
        viewModelScope.launch { formulaStore.delete(name) }
    }
}

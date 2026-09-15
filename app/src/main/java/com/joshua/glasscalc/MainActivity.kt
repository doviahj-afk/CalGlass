package com.joshua.glasscalc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joshua.glasscalc.ui.theme.*

class CalculatorViewModelFactory(private val store: FormulaStore) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CalculatorViewModel(store) as T
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        PythonBridge.init(applicationContext)

        val store = FormulaStore(applicationContext)

        setContent {
            GlassCalcTheme {
                val viewModel: CalculatorViewModel =
                    viewModel(factory = CalculatorViewModelFactory(store))
                AppRoot(viewModel)
            }
        }
    }
}

@Composable
fun AppRoot(viewModel: CalculatorViewModel) {
    val state by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        LiquidGlassBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            ModeSwitcher(mode = state.mode, onModeChange = viewModel::setMode)
            Spacer(Modifier.height(16.dp))

            when (state.mode) {
                CalcMode.STANDARD -> StandardCalculator(state = state, vm = viewModel)
                CalcMode.FORMULA -> FormulaWorkspace(state = state, vm = viewModel)
            }
        }
    }
}

@Composable
fun ModeSwitcher(mode: CalcMode, onModeChange: (CalcMode) -> Unit) {
    GlassPane(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(4.dp)) {
            SegmentButton("Standard", mode == CalcMode.STANDARD, Modifier.weight(1f)) {
                onModeChange(CalcMode.STANDARD)
            }
            SegmentButton("Python Formula", mode == CalcMode.FORMULA, Modifier.weight(1f)) {
                onModeChange(CalcMode.FORMULA)
            }
        }
    }
}

@Composable
fun SegmentButton(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bg = if (selected) Color.White.copy(alpha = 0.20f) else Color.Transparent
    Box(
        modifier = modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) TextPrimary else TextSecondary, fontSize = 14.sp)
    }
}

// ---------------- Standard calculator ----------------

@Composable
fun StandardCalculator(state: CalcUiState, vm: CalculatorViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        GlassPane(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = state.expression.ifEmpty { "0" },
                    color = TextSecondary,
                    fontSize = 28.sp,
                    maxLines = 2
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = state.standardError?.let { "Error" }
                        ?: state.standardResult
                        ?: "",
                    color = if (state.standardError != null) AccentOrange else TextPrimary,
                    fontSize = 52.sp,
                    maxLines = 1
                )
            }
        }

        val rows = listOf(
            listOf("AC", "(", ")", "/"),
            listOf("7", "8", "9", "*"),
            listOf("4", "5", "6", "-"),
            listOf("1", "2", "3", "+"),
            listOf("0", ".", "⌫", "=")
        )

        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { symbol ->
                    val isOperator = symbol in setOf("/", "*", "-", "+", "=")
                    val isUtility = symbol in setOf("AC", "⌫", "(", ")")
                    GlassButton(
                        label = symbol,
                        tint = when {
                            symbol == "=" -> AccentBlue
                            isOperator -> AccentOrange
                            isUtility -> AccentPurple
                            else -> null
                        },
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        onClick = {
                            when (symbol) {
                                "AC" -> vm.onClear()
                                "⌫" -> vm.onBackspace()
                                "=" -> vm.onEquals()
                                else -> vm.onDigit(symbol)
                            }
                        }
                    )
                }
            }
        }
    }
}

// ---------------- Python formula workspace ----------------

@Composable
fun FormulaWorkspace(state: CalcUiState, vm: CalculatorViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {

        GlassPane(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                BasicTextField(
                    value = state.formulaName,
                    onValueChange = vm::onFormulaNameChange,
                    textStyle = TextStyle(color = TextPrimary, fontSize = 16.sp),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (state.formulaName.isEmpty()) {
                            Text("Formula name…", color = TextSecondary, fontSize = 16.sp)
                        }
                        inner()
                    }
                )
                Spacer(Modifier.height(12.dp))
                GlassPane(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp)
                ) {
                    BasicTextField(
                        value = state.formulaCode,
                        onValueChange = vm::onFormulaCodeChange,
                        textStyle = TextStyle(
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    )
                }

                Spacer(Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlassButton(
                        label = "Run",
                        tint = AccentGreen,
                        fontSize = 16.sp,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        onClick = vm::runCurrentFormula
                    )
                    GlassButton(
                        label = "Save",
                        tint = AccentBlue,
                        fontSize = 16.sp,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        onClick = vm::saveCurrentFormula
                    )
                }

                if (state.formulaResult != null || state.formulaError != null) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = state.formulaError?.let { "⚠ $it" } ?: "= ${state.formulaResult}",
                        color = if (state.formulaError != null) AccentOrange else AccentGreen,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                if (state.formulaStdout.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = state.formulaStdout,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Saved formulas", color = TextSecondary, fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(state.savedFormulas) { formula ->
                GlassPane(
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { vm.loadFormula(formula) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(formula.name, color = TextPrimary, fontSize = 15.sp)
                            Text(
                                formula.code.lines().firstOrNull().orEmpty(),
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1
                            )
                        }
                        Row {
                            IconButton(onClick = { vm.runSavedFormula(formula) }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Run", tint = AccentGreen)
                            }
                            IconButton(onClick = { vm.deleteFormula(formula.name) }) {
                                Icon(Icons.Default.Close, contentDescription = "Delete", tint = AccentOrange)
                            }
                        }
                    }
                }
            }
        }
    }
}

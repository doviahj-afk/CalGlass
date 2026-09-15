package com.joshua.glasscalc.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

private val Ink = Color(0xFFF5F7FF)
private val Glass = Color(0x33FFFFFF)
private val Accent = Color(0xFF4775CF)

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "CalGlass") {
        MaterialTheme { CalGlass() }
    }
}

@androidx.compose.runtime.Composable
private fun CalGlass() {
    var formulaMode by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF344B86), Color(0xFF151A2A), Color(0xFF4A265C)))).padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("CalGlass", color = Ink, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text("A calculator with a little more room to think.", color = Color(0xFFC2CAE0))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ModeButton("Standard", !formulaMode) { formulaMode = false }
            ModeButton("Formula", formulaMode) { formulaMode = true }
        }
        if (formulaMode) FormulaMode() else StandardMode()
    }
}

@androidx.compose.runtime.Composable
private fun ModeButton(label: String, selected: Boolean, action: () -> Unit) = Button(
    action, colors = ButtonDefaults.buttonColors(containerColor = if (selected) Accent else Glass), shape = RoundedCornerShape(18.dp)
) { Text(label) }

@androidx.compose.runtime.Composable
private fun StandardMode() {
    var expression by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("0") }
    val keys = listOf("C", "⌫", "(", ")", "7", "8", "9", "/", "4", "5", "6", "*", "1", "2", "3", "-", "0", ".", "=", "+")
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.fillMaxWidth().background(Glass, RoundedCornerShape(22.dp)).padding(18.dp), horizontalAlignment = Alignment.End) {
            Text(expression.ifBlank { "0" }, color = Color(0xFFC2CAE0), fontSize = 22.sp)
            Text(answer, color = Ink, fontSize = 42.sp, fontWeight = FontWeight.SemiBold)
        }
        keys.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { key -> Button(
                    onClick = {
                        when (key) {
                            "C" -> { expression = ""; answer = "0" }
                            "⌫" -> expression = expression.dropLast(1)
                            "=" -> answer = runCatching { format(ExpressionParser(expression).parse()) }.getOrElse { "Error" }
                            else -> expression += key
                        }
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (key in listOf("=", "+", "-", "*", "/")) Accent else Glass),
                    shape = RoundedCornerShape(15.dp)
                ) { Text(key, fontSize = 18.sp) } }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun FormulaMode() {
    var code by remember { mutableStateOf("# Assign your answer to result\nprincipal = 1000\nrate = 0.05\nyears = 3\nresult = principal * (1 + rate) ^ years") }
    var result by remember { mutableStateOf("Write result = an expression, then Run.") }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Formula mode", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Text("Use arithmetic, parentheses, named values, and ^ for powers.", color = Color(0xFFC2CAE0))
        OutlinedTextField(code, { code = it }, Modifier.fillMaxWidth().height(270.dp), label = { Text("Formula") }, textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, color = Ink))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Button({ result = runCatching { format(runFormula(code)) }.getOrElse { "Error: ${it.message}" } }, colors = ButtonDefaults.buttonColors(containerColor = Accent)) { Text("Run") }
            Text(result, color = Ink)
        }
    }
}

private fun runFormula(source: String): Double {
    val values = mutableMapOf<String, Double>()
    source.lineSequence().map { it.substringBefore("#").trim() }.filter { "=" in it }.forEach { line ->
        val parts = line.split("=", limit = 2)
        values[parts[0].trim()] = ExpressionParser(parts[1].trim(), values).parse()
    }
    return values["result"] ?: error("Add result = ...")
}

private fun format(value: Double) = if (value % 1.0 == 0.0) value.toLong().toString() else "%.10g".format(value)

private class ExpressionParser(private val text: String, private val names: Map<String, Double> = emptyMap()) {
    private var at = 0
    fun parse(): Double = add().also { skip(); require(at == text.length) { "Unexpected input" } }
    private fun add(): Double { var n = multiply(); while (true) { skip(); n = when { take('+') -> n + multiply(); take('-') -> n - multiply(); else -> return n } } }
    private fun multiply(): Double { var n = power(); while (true) { skip(); n = when { take('*') -> n * power(); take('/') -> n / power(); else -> return n } } }
    private fun power(): Double { var n = atom(); skip(); if (take('^')) n = kotlin.math.pow(n, power()); return n }
    private fun atom(): Double { skip(); if (take('(')) return add().also { expect(')') }; val start = at; while (at < text.length && (text[at].isLetter() || text[at] == '_')) at++; if (at > start) return names[text.substring(start, at)] ?: error("Unknown value"); val number = at; while (at < text.length && (text[at].isDigit() || text[at] == '.')) at++; return text.substring(number, at).toDouble() }
    private fun skip() { while (at < text.length && text[at].isWhitespace()) at++ }
    private fun take(c: Char) = (at < text.length && text[at] == c).also { if (it) at++ }
    private fun expect(c: Char) { skip(); require(take(c)) { "Expected $c" } }
}

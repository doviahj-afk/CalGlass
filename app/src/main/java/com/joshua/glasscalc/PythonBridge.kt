package com.joshua.glasscalc

import android.content.Context
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import org.json.JSONObject

/**
 * Thin wrapper around the embedded CPython interpreter (Chaquopy). Starts the
 * interpreter once and forwards formula strings to formula_engine.py.
 */
object PythonBridge {

    private var module: PyObject? = null

    fun init(context: Context) {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }
        module = Python.getInstance().getModule("formula_engine")
    }

    data class FormulaResult(
        val ok: Boolean,
        val result: String? = null,
        val error: String? = null,
        val stdout: String = ""
    )

    /**
     * Runs [code] with [variables] bound as top-level names. Never throws —
     * failures come back as FormulaResult(ok = false, error = ...).
     */
    fun runFormula(code: String, variables: Map<String, Double> = emptyMap()): FormulaResult {
        val mod = module ?: return FormulaResult(ok = false, error = "Python engine not initialised")
        return try {
            val variablesJson = JSONObject(variables).toString()
            val raw = mod.callAttr("run_formula", code, variablesJson).toString()
            val json = JSONObject(raw)
            if (json.optBoolean("ok", false)) {
                FormulaResult(
                    ok = true,
                    result = json.opt("result")?.toString(),
                    stdout = json.optString("stdout", "")
                )
            } else {
                FormulaResult(
                    ok = false,
                    error = json.optString("error", "Unknown error"),
                    stdout = json.optString("stdout", "")
                )
            }
        } catch (t: Throwable) {
            FormulaResult(ok = false, error = t.message ?: t.toString())
        }
    }
}

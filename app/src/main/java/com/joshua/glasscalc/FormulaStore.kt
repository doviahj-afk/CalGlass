package com.joshua.glasscalc

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

data class SavedFormula(val name: String, val code: String)

private val Context.dataStore by preferencesDataStore(name = "glasscalc_formulas")
private val FORMULAS_KEY = stringPreferencesKey("saved_formulas_json")

class FormulaStore(private val context: Context) {

    val formulas: Flow<List<SavedFormula>> = context.dataStore.data.map { prefs ->
        val raw = prefs[FORMULAS_KEY] ?: "[]"
        parse(raw)
    }

    suspend fun save(formula: SavedFormula) {
        context.dataStore.edit { prefs ->
            val current = parse(prefs[FORMULAS_KEY] ?: "[]").toMutableList()
            val existingIndex = current.indexOfFirst { it.name == formula.name }
            if (existingIndex >= 0) current[existingIndex] = formula else current.add(formula)
            prefs[FORMULAS_KEY] = serialize(current)
        }
    }

    suspend fun delete(name: String) {
        context.dataStore.edit { prefs ->
            val current = parse(prefs[FORMULAS_KEY] ?: "[]").filterNot { it.name == name }
            prefs[FORMULAS_KEY] = serialize(current)
        }
    }

    private fun parse(json: String): List<SavedFormula> {
        val array = JSONArray(json)
        return (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            SavedFormula(obj.getString("name"), obj.getString("code"))
        }
    }

    private fun serialize(list: List<SavedFormula>): String {
        val array = JSONArray()
        list.forEach {
            array.put(JSONObject().apply {
                put("name", it.name)
                put("code", it.code)
            })
        }
        return array.toString()
    }
}

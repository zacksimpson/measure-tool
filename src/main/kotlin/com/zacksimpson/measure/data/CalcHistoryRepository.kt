package com.zacksimpson.measure.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Serializable
data class CalcHistoryEntry(val result: String)

private val HISTORY_KEY = stringPreferencesKey("calc_history")
private const val MAX_HISTORY = 6

// shared across every calculator screen (Main, Fraction Calc, Convert Units), so
// switching tools mid-measurement still shows one combined recent-results log.
// capped at 6, most recent first, matching the item count OptionsScreen's no-scroll
// layout is already tuned for.
class CalcHistoryRepository(private val dataStore: DataStore<Preferences>) {

    private val serializer = ListSerializer(CalcHistoryEntry.serializer())

    val entries: Flow<List<CalcHistoryEntry>> = dataStore.data.map { prefs ->
        prefs[HISTORY_KEY]?.let(::decode) ?: emptyList()
    }

    suspend fun record(result: String) {
        dataStore.edit { prefs ->
            val current = prefs[HISTORY_KEY]?.let(::decode) ?: emptyList()
            val updated = (listOf(CalcHistoryEntry(result)) + current).take(MAX_HISTORY)
            prefs[HISTORY_KEY] = Json.encodeToString(serializer, updated)
        }
    }

    // a corrupt or unreadable value just means an empty history, not a crash.
    private fun decode(raw: String): List<CalcHistoryEntry> =
        runCatching { Json.decodeFromString(serializer, raw) }.getOrDefault(emptyList())
}

/*
 * Copyright (C) 2026 Vibhor Goel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package io.github.vibhor1102.macrion.feature.smart.config.ui.scenario.imageevents

import android.content.Context
import androidx.datastore.preferences.core.stringSetPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.vibhor1102.macrion.core.base.PreferencesDataStore
import io.github.vibhor1102.macrion.core.base.di.Dispatcher
import io.github.vibhor1102.macrion.core.base.di.HiltCoroutineDispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Device-local viewing preferences, independent of scenario contents and backups. */
@Singleton
class FolderExpansionPreferences @Inject constructor(
    @ApplicationContext context: Context,
    @Dispatcher(HiltCoroutineDispatchers.IO) dispatcher: CoroutineDispatcher,
) {
    private val dataStore = PreferencesDataStore(context, dispatcher, "event_folder_expansion")
    private val writes = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    init {
        // Keep writes ordered and alive even when the dialog's ViewModel is destroyed.
        CoroutineScope(dispatcher + SupervisorJob()).launch {
            for (write in writes) write()
        }
    }

    fun observe(scenarioId: Long): Flow<Set<String>> =
        flow {
            // A reopened dialog must read after any writes queued by the previous dialog.
            val ready = CompletableDeferred<Unit>()
            writes.send { ready.complete(Unit) }
            ready.await()
            emitAll(dataStore.data.map { it[key(scenarioId)].orEmpty() })
        }

    fun save(scenarioId: Long, folders: Set<String>) {
        if (scenarioId <= 0) return
        val snapshot = folders.toSet()
        writes.trySend {
            dataStore.edit { preferences ->
                val key = key(scenarioId)
                if (snapshot.isEmpty()) preferences.remove(key) else preferences[key] = snapshot
            }
        }
    }

    private fun key(scenarioId: Long) = stringSetPreferencesKey("scenario_$scenarioId")
}

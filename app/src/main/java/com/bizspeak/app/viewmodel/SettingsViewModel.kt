package com.bizspeak.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bizspeak.app.data.AppSettings
import com.bizspeak.app.data.Level
import com.bizspeak.app.data.Scenario
import com.bizspeak.app.data.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(private val store: SettingsStore) : ViewModel() {

    private val _settings = MutableStateFlow<AppSettings?>(null)
    val settings: StateFlow<AppSettings?> = _settings.asStateFlow()

    init {
        viewModelScope.launch {
            store.settings.collect { _settings.value = it }
        }
    }

    fun save(s: AppSettings) {
        viewModelScope.launch { store.save(s) }
    }

    fun update(level: Level? = null, scenario: Scenario? = null) {
        val cur = _settings.value ?: return
        viewModelScope.launch { store.save(cur.copy(level = level ?: cur.level, scenario = scenario ?: cur.scenario)) }
    }
}

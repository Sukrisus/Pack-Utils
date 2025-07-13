package com.packify.packaverse.ui.theme

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemeManager(private val context: Context) {
    private val sharedPreferences = context.getSharedPreferences("packify_prefs", Context.MODE_PRIVATE)
    private val _isDarkMode = MutableStateFlow(sharedPreferences.getBoolean("is_dark_mode", true))
    
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()
    
    fun toggleTheme() {
        val newValue = !_isDarkMode.value
        _isDarkMode.value = newValue
        sharedPreferences.edit().putBoolean("is_dark_mode", newValue).apply()
    }
    
    fun setTheme(isDark: Boolean) {
        _isDarkMode.value = isDark
        sharedPreferences.edit().putBoolean("is_dark_mode", isDark).apply()
    }
}

@Composable
fun rememberThemeManager(): ThemeManager {
    val context = LocalContext.current
    return remember { ThemeManager(context) }
}
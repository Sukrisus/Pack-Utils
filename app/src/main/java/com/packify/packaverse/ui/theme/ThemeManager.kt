package com.packify.packaverse.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemeManager(private val context: Context) {
    private val sharedPreferences = context.getSharedPreferences("packify_prefs", Context.MODE_PRIVATE)
    
    // Use system theme by default instead of hardcoded dark mode
    private val _isDarkMode = MutableStateFlow(
        sharedPreferences.getBoolean("is_dark_mode", false)
    )
    
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
    
    fun setSystemTheme(isSystemDark: Boolean) {
        if (!sharedPreferences.contains("is_dark_mode")) {
            _isDarkMode.value = isSystemDark
        }
    }
}

@Composable
fun rememberThemeManager(): ThemeManager {
    val context = LocalContext.current
    val isSystemInDarkTheme = isSystemInDarkTheme()
    
    return remember { ThemeManager(context) }.also { manager ->
        // Set system theme on first launch
        LaunchedEffect(isSystemInDarkTheme) {
            manager.setSystemTheme(isSystemInDarkTheme)
        }
    }
}
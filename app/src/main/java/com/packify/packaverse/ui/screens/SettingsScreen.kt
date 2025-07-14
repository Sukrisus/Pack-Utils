package com.packify.packaverse.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.packify.packaverse.ui.components.BottomNavigationBar
import com.packify.packaverse.ui.theme.rememberThemeManager
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit = {},
    onNavigateToDashboard: () -> Unit = {},
    viewModel: com.packify.packaverse.viewmodel.TexturePackViewModel? = null
) {
    val themeManager = rememberThemeManager()
    val isDarkMode by themeManager.isDarkMode.collectAsState()
    val context = LocalContext.current
    
    // Use SharedPreferences for persistent storage
    val sharedPreferences = context.getSharedPreferences("packify_settings", android.content.Context.MODE_PRIVATE)
    
    var autoSave by remember { mutableStateOf(sharedPreferences.getBoolean("auto_save", true)) }
    var highQualityExport by remember { mutableStateOf(sharedPreferences.getBoolean("high_quality_export", true)) }
    var showResetDialog by remember { mutableStateOf(false) }
    
    // Get current texture packs for reset functionality
    val texturePacks by viewModel?.texturePacks?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    
    // Save settings when they change
    LaunchedEffect(autoSave) {
        sharedPreferences.edit().putBoolean("auto_save", autoSave).apply()
    }
    
    LaunchedEffect(highQualityExport) {
        sharedPreferences.edit().putBoolean("high_quality_export", highQualityExport).apply()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Settings",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Normal
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            BottomNavigationBar(
                onNavigateToHome = onNavigateToHome,
                onNavigateToDashboard = onNavigateToDashboard,
                onNavigateToSettings = {},
                currentRoute = "settings"
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Appearance Section
            SettingsSection(
                title = "Appearance",
                icon = Icons.Default.Palette
            ) {
                SettingsSwitch(
                    title = "Dark Mode",
                    subtitle = "Use dark theme for the app",
                    icon = Icons.Default.DarkMode,
                    checked = isDarkMode,
                    onCheckedChange = { themeManager.toggleTheme() }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Project Settings Section
            SettingsSection(
                title = "Project Settings",
                icon = Icons.Default.Settings
            ) {
                SettingsSwitch(
                    title = "Auto Save",
                    subtitle = "Automatically save changes while editing",
                    icon = Icons.Default.Save,
                    checked = autoSave,
                    onCheckedChange = { autoSave = it }
                )
                
                SettingsSwitch(
                    title = "High Quality Export",
                    subtitle = "Export textures in maximum quality",
                    icon = Icons.Default.HighQuality,
                    checked = highQualityExport,
                    onCheckedChange = { highQualityExport = it }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Storage Section
            SettingsSection(
                title = "Storage",
                icon = Icons.Default.Storage
            ) {
                val storageStatus = if (viewModel?.hasStoragePermission() == true) {
                    "External Storage (Recommended)"
                } else {
                    "Internal Storage (Projects will be deleted with app)"
                }
                
                val projectsPath = viewModel?.getProjectsDirectoryPath() ?: "Unknown"
                
                SettingsItem(
                    title = "Storage Location",
                    subtitle = storageStatus,
                    icon = Icons.Default.Folder,
                    onClick = {}
                )
                
                SettingsItem(
                    title = "Projects Directory",
                    subtitle = projectsPath,
                    icon = Icons.Default.FolderOpen,
                    onClick = {}
                )
                
                if (viewModel?.hasStoragePermission() != true) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Grant storage permission to save projects externally so they won't be deleted when you uninstall the app.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 32.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // About Section
            SettingsSection(
                title = "About",
                icon = Icons.Default.Info
            ) {
                SettingsItem(
                    title = "Version",
                    subtitle = "1.0.0",
                    icon = Icons.Default.Apps,
                    onClick = {}
                )
                
                SettingsItem(
                    title = "Developer",
                    subtitle = "MCPE Texture Pack Maker",
                    icon = Icons.Default.Person,
                    onClick = {}
                )
                
                SettingsItem(
                    title = "License",
                    subtitle = "MIT License",
                    icon = Icons.Default.Description,
                    onClick = {}
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Reset Settings Button
            Button(
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF6B6B),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Restore, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset to Defaults")
            }
            
            // Share App Button
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Check out Packify - MCPE Texture Pack Editor! Create amazing texture packs for Minecraft PE!")
                        putExtra(Intent.EXTRA_SUBJECT, "Packify - MCPE Texture Pack Editor")
                    }
                    try {
                        context.startActivity(Intent.createChooser(shareIntent, "Share via"))
                    } catch (e: Exception) {
                        // Handle case where no app can handle the share intent
                        viewModel?.showError("No app available to share")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFB6C1),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share App")
            }
        }
        
        // Reset Dialog
        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text("Reset to Defaults") },
                text = { 
                    Column {
                        Text("Are you sure you want to reset all settings to their default values?")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("This will also reset all texture packs to their base textures.", 
                             fontSize = 14.sp, 
                             color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            // Reset all settings
                            themeManager.setTheme(true)
                            autoSave = true
                            highQualityExport = true
                            sharedPreferences.edit().clear().apply()
                            
                            // Reset all texture packs to default
                            viewModel?.let { vm ->
                                texturePacks.forEach { pack ->
                                    vm.resetToDefault(pack.id)
                                }
                            }
                            
                            showResetDialog = false
                        }
                    ) {
                        Text("Reset")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFFFFB6C1),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            content()
        }
    }
}

@Composable
fun SettingsSwitch(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFFFFB6C1),
                checkedTrackColor = Color(0xFFFFB6C1).copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}


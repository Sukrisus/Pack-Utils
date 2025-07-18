package com.packify.packaverse.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.foundation.clickable
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.with
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

sealed class SettingsPage {
    object Main : SettingsPage()
    object StorageLocation : SettingsPage()
    object ProjectsDirectory : SettingsPage()
    object Version : SettingsPage()
    object Developer : SettingsPage()
    object License : SettingsPage()
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit = {},
    onNavigateToDashboard: () -> Unit = {},
    viewModel: com.packify.packaverse.viewmodel.TexturePackViewModel? = null
) {
    var currentPage by remember { mutableStateOf<SettingsPage>(SettingsPage.Main) }
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
                        style = MaterialTheme.typography.titleLarge
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
                .systemBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    if (targetState is SettingsPage.Main && initialState !is SettingsPage.Main) {
                        // Backwards
                        (slideInHorizontally { -it } + fadeIn()) with (slideOutHorizontally { it } + fadeOut())
                    } else {
                        // Forwards
                        (slideInHorizontally { it } + fadeIn()) with (slideOutHorizontally { -it } + fadeOut())
                    }
                }
            ) { page ->
                when (page) {
                    SettingsPage.Main -> {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Appearance Section
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
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
                            }
                            // Project Settings Section
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
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
                            }
                            // Storage Section
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
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
                                        onClick = { currentPage = SettingsPage.StorageLocation }
                                    )
                                    SettingsItem(
                                        title = "Projects Directory",
                                        subtitle = projectsPath,
                                        icon = Icons.Default.FolderOpen,
                                        onClick = { currentPage = SettingsPage.ProjectsDirectory }
                                    )
                                    if (viewModel?.hasStoragePermission() != true) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Grant storage permission to save projects externally so they won't be deleted when you uninstall the app.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.padding(start = 32.dp)
                                        )
                                    }
                                }
                            }
                            // About Section
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                SettingsSection(
                                    title = "About",
                                    icon = Icons.Default.Info
                                ) {
                                    SettingsItem(
                                        title = "Version",
                                        subtitle = "1.0.0",
                                        icon = Icons.Default.Apps,
                                        onClick = { currentPage = SettingsPage.Version }
                                    )
                                    SettingsItem(
                                        title = "Developer",
                                        subtitle = "Yami_",
                                        icon = Icons.Default.Person,
                                        onClick = { currentPage = SettingsPage.Developer }
                                    )
                                    SettingsItem(
                                        title = "License",
                                        subtitle = "MIT License",
                                        icon = Icons.Default.Description,
                                        onClick = { currentPage = SettingsPage.License }
                                    )
                                }
                            }
                            // Reset Settings Button
                            FilledTonalButton(
                                onClick = { showResetDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Reset to Defaults", color = Color.White)
                            }
                            // Share App Button
                            FilledTonalButton(
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
                                        viewModel?.showError("No app available to share")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Share App", color = Color.White)
                            }
                        }
                    }
                    SettingsPage.StorageLocation -> {
                        SettingsSubPageM3(title = "Storage Location", onBack = { currentPage = SettingsPage.Main }) {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp)) {
                                    Text("Current Storage:", style = MaterialTheme.typography.titleMedium)
                                    Text(if (viewModel?.hasStoragePermission() == true) "External Storage" else "Internal Storage", fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(8.dp))
                                    Text("Projects will be deleted with the app if stored internally.", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                                    Spacer(Modifier.height(8.dp))
                                    Button(onClick = { /* TODO: Request/Change storage permission */ }, enabled = viewModel?.hasStoragePermission() != true) {
                                        Text("Grant Storage Permission")
                                    }
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Text("Tip: External storage is recommended for keeping your projects safe.", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                        }
                    }
                    SettingsPage.ProjectsDirectory -> {
                        SettingsSubPageM3(title = "Projects Directory", onBack = { currentPage = SettingsPage.Main }) {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp)) {
                                    Text("Directory Path:", style = MaterialTheme.typography.titleMedium)
                                    val path = viewModel?.getProjectsDirectoryPath() ?: "Unknown"
                                    Text(path, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(8.dp))
                                    Row {
                                        Button(onClick = { /* TODO: Copy path to clipboard */ }) { Text("Copy Path") }
                                        Spacer(Modifier.width(8.dp))
                                        Button(onClick = { /* TODO: Open in file manager */ }) { Text("Open Folder") }
                                    }
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Text("Tip: You can back up or share your projects by accessing this folder.", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                        }
                    }
                    SettingsPage.Version -> {
                        SettingsSubPageM3(title = "Version", onBack = { currentPage = SettingsPage.Main }) {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp)) {
                                    Text("App Version:", style = MaterialTheme.typography.titleMedium)
                                    Text("1.0.0", fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(8.dp))
                                    Text("Build: 100", fontSize = 13.sp)
                                    Spacer(Modifier.height(8.dp))
                                    Button(onClick = { /* TODO: Check for updates */ }) { Text("Check for Updates") }
                                    Spacer(Modifier.height(8.dp))
                                    Button(onClick = { /* TODO: Show changelog */ }) { Text("View Changelog") }
                                }
                            }
                        }
                    }
                    SettingsPage.Developer -> {
                        SettingsSubPageM3(title = "Developer", onBack = { currentPage = SettingsPage.Main }) {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp)) {
                                    Text("Developed by:", style = MaterialTheme.typography.titleMedium)
                                    Text("Yami_", fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(8.dp))
                                    Text("Contact:", style = MaterialTheme.typography.titleSmall)
                                    Text("Email: yami@example.com", fontSize = 13.sp)
                                    Text("GitHub: github.com/YamiDev", fontSize = 13.sp)
                                    Spacer(Modifier.height(8.dp))
                                    Button(onClick = { /* TODO: Open website */ }) { Text("Visit Website") }
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Text("Special thanks to all contributors and open source libraries!", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                        }
                    }
                    SettingsPage.License -> {
                        SettingsSubPageM3(title = "License", onBack = { currentPage = SettingsPage.Main }) {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 200.dp, max = 600.dp)
                                        .verticalScroll(rememberScrollState())
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        """MIT License\n\nCopyright (c) 2024 Yami_\n\nPermission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the \"Software\"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:\n\nThe above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.\n\nTHE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.""",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { /* TODO: Copy license to clipboard */ }) { Text("Copy License") }
                        }
                    }
                }
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
            .clickable { onClick() }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSubPageM3(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CenterAlignedTopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Top
            ) {
                content()
            }
        }
    }
}


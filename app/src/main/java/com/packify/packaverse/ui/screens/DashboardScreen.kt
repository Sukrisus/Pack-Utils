package com.packify.packaverse.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.packify.packaverse.data.TextureCategory
import com.packify.packaverse.data.TextureItem
import com.packify.packaverse.data.TexturePack
import com.packify.packaverse.viewmodel.TexturePackViewModel
import com.packify.packaverse.ui.components.BottomNavigationBar
import com.packify.packaverse.ui.components.TextureDropdown
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: TexturePackViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCategory: (TextureCategory) -> Unit
) {
    val categories = listOf(
        TextureCategory.ENTITY,
        TextureCategory.ENVIRONMENT,
        TextureCategory.ITEMS,
        TextureCategory.BLOCKS,
        TextureCategory.GUI,
        TextureCategory.PARTICLE,
        TextureCategory.MISC
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Addons Maker for Minecraft", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        bottomBar = {
            BottomNavigationBar(
                onNavigateToHome = { /* TODO: Implement navigation to Home */ },
                onNavigateToDashboard = {},
                onNavigateToSettings = onNavigateToSettings,
                currentRoute = "dashboard"
            )
        }
    ) { paddingValues ->
        val texturePacks by viewModel.texturePacks.collectAsState()
        var selectedPack by remember { mutableStateOf(texturePacks.firstOrNull()) }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .systemBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            PackSelectionCard(
                texturePacks = texturePacks,
                onPackSelected = { packId ->
                    selectedPack = texturePacks.find { it.id == packId }
                },
                onExportPack = { /* export logic */ }
            )
            categories.forEach { category ->
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp)
                        .clickable {
                            selectedPack?.let { onNavigateToCategory(category) }
                        },
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.medium),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (category) {
                                    TextureCategory.BLOCKS -> Icons.Default.ViewInAr
                                    TextureCategory.ITEMS -> Icons.Default.Inventory
                                    TextureCategory.ENTITY -> Icons.Default.Person
                                    TextureCategory.ENVIRONMENT -> Icons.Default.Landscape
                                    TextureCategory.GUI -> Icons.Default.Dashboard
                                    TextureCategory.PARTICLE -> Icons.Default.Star
                                    TextureCategory.MISC -> Icons.Default.MoreVert
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(24.dp))
                        Text(
                            text = category.displayName,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ShareDialog(
    texturePacks: List<TexturePack>,
    onDismiss: () -> Unit,
    onSharePack: (String) -> Unit,
    onShareApp: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Share") },
        text = {
            Column {
                Text("What would you like to share?")
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onShareApp,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFB6C1),
                        contentColor = Color.Black
                    )
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share App")
                }
                
                if (texturePacks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text("Or share a texture pack:")
                    
                    texturePacks.forEach { pack ->
                        Button(
                            onClick = { onSharePack(pack.id) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(Icons.Default.Archive, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(pack.name)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EmptyStateView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Texture,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color(0xFFFFB6C1)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No Texture Packs Found",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Create a texture pack to start editing textures",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

@Composable
fun TextureEditorContent(
    texturePacks: List<TexturePack>,
    textures: List<TextureItem>,
    viewModel: TexturePackViewModel,
    errorMessage: String?,
    successMessage: String?,
    onTextureSelected: (TextureItem) -> Unit,
    onPackSelected: (String) -> Unit,
    onExportPack: (String) -> Unit,
    onNavigateToTextureManagement: (TextureCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Pack Selection
        item {
            PackSelectionCard(
                texturePacks = texturePacks,
                onPackSelected = onPackSelected,
                onExportPack = onExportPack
            )
        }
        
        // Texture Categories
        items(TextureCategory.values()) { category ->
            TextureDropdown(
                category = category,
                textures = textures.filter { it.category == category },
                viewModel = viewModel,
                packId = texturePacks.firstOrNull()?.id ?: "",
                onTextureSelected = onTextureSelected,
                onNavigateToTextureManagement = onNavigateToTextureManagement
            )
        }
    }
}

@Composable
fun PackSelectionCard(
    texturePacks: List<TexturePack>,
    onPackSelected: (String) -> Unit,
    onExportPack: (String) -> Unit
) {
    var selectedPack by remember { mutableStateOf(texturePacks.firstOrNull()) }
    var showPackDialog by remember { mutableStateOf(false) }
    
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Current Pack: ${selectedPack?.name ?: "None"}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Row {
                    IconButton(onClick = { showPackDialog = true }) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = "Switch Pack")
                    }
                    
                    selectedPack?.let { pack ->
                        IconButton(onClick = { onExportPack(pack.id) }) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Export Pack")
                        }
                    }
                }
            }
            
            selectedPack?.let { pack ->
                Text(
                    text = pack.description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    
    if (showPackDialog) {
        AlertDialog(
            onDismissRequest = { showPackDialog = false },
            title = { Text("Select Texture Pack") },
            text = {
                LazyColumn {
                    items(texturePacks) { pack ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedPack?.id == pack.id) 
                                    Color(0xFFFFB6C1).copy(alpha = 0.3f) 
                                else 
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = pack.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = pack.description,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedPack?.let { pack ->
                            onPackSelected(pack.id)
                        }
                        showPackDialog = false
                    }
                ) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPackDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}




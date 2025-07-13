package com.packify.packaverse.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: TexturePackViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToTextureEditor: (String, String) -> Unit = { _, _ -> }
) {
    val texturePacks by viewModel.texturePacks.collectAsState()
    val textures by viewModel.textures.collectAsState()
    val isLoading by viewModel.isLoading
    val errorMessage by viewModel.errorMessage
    val successMessage by viewModel.successMessage
    val context = LocalContext.current
    
    var selectedPackId by remember { mutableStateOf<String?>(null) }
    var showShareDialog by remember { mutableStateOf(false) }
    
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        uri?.let { exportUri ->
            selectedPackId?.let { packId ->
                viewModel.exportTexturePack(packId, exportUri)
            }
        }
    }
    
    LaunchedEffect(Unit) {
        viewModel.loadTexturePacks()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Texture Editor",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Normal
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showShareDialog = true }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            BottomNavigationBar(
                onNavigateToHome = onNavigateBack,
                onNavigateToDashboard = {},
                onNavigateToSettings = onNavigateToSettings,
                currentRoute = "dashboard"
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color(0xFFFFB6C1)
                )
            }
        } else if (texturePacks.isEmpty()) {
            EmptyStateView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            TextureEditorContent(
                texturePacks = texturePacks,
                textures = textures,
                viewModel = viewModel,
                errorMessage = errorMessage,
                successMessage = successMessage,
                onTextureSelected = { texture ->
                    selectedPackId?.let { packId ->
                        onNavigateToTextureEditor(packId, texture.name)
                    }
                },
                onPackSelected = { packId ->
                    selectedPackId = packId
                },
                onExportPack = { packId ->
                    selectedPackId = packId
                    exportLauncher.launch("${texturePacks.find { it.id == packId }?.name ?: "texture_pack"}.zip")
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
        
        // Share Dialog
        if (showShareDialog) {
            ShareDialog(
                texturePacks = texturePacks,
                onDismiss = { showShareDialog = false },
                onSharePack = { packId ->
                    selectedPackId = packId
                    exportLauncher.launch("${texturePacks.find { it.id == packId }?.name ?: "texture_pack"}.zip")
                    showShareDialog = false
                },
                onShareApp = {
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
                        viewModel.showError("No app available to share")
                    }
                    showShareDialog = false
                }
            )
        }
        
        // Error/Success Messages
        errorMessage?.let { message ->
            LaunchedEffect(message) {
                viewModel.clearMessages()
            }
        }
        
        successMessage?.let { message ->
            LaunchedEffect(message) {
                viewModel.clearMessages()
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
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<TextureCategory?>(null) }
    
    // Load all textures when the first pack is available
    LaunchedEffect(texturePacks) {
        if (texturePacks.isNotEmpty() && textures.isEmpty()) {
            viewModel.loadAllTextures(texturePacks.first().id)
        }
    }
    
    if (selectedCategory == null) {
        // Main Dashboard View - Categories Grid
        LazyColumn(
            modifier = modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Pack Selection Section
            item {
                PackSelectionCard(
                    texturePacks = texturePacks,
                    onPackSelected = onPackSelected,
                    onExportPack = onExportPack
                )
            }
            
            // Title
            item {
                Text(
                    text = "ADDONS MAKER FOR MINECRAFT",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            // Categories Grid
            items(TextureCategory.values().chunked(2)) { categoryRow ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    categoryRow.forEach { category ->
                        CategoryCard(
                            category = category,
                            textureCount = textures.count { it.category == category },
                            onClick = { selectedCategory = category },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Fill remaining space if odd number of items
                    if (categoryRow.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    } else {
        // Category Library View - Show textures for selected category
        CategoryLibraryScreen(
            category = selectedCategory!!,
            textures = textures.filter { it.category == selectedCategory },
            viewModel = viewModel,
            packId = texturePacks.firstOrNull()?.id ?: "",
            onTextureSelected = onTextureSelected,
            onBackPressed = { selectedCategory = null }
        )
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

@Composable
fun CategoryCard(
    category: TextureCategory,
    textureCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = when (category) {
                    TextureCategory.ENTITIES -> Icons.Default.Person
                    TextureCategory.ENVIRONMENT -> Icons.Default.Landscape
                    TextureCategory.ITEMS -> Icons.Default.Inventory
                    TextureCategory.MAP -> Icons.Default.Map
                    TextureCategory.UI -> Icons.Default.TouchApp
                    TextureCategory.GUI -> Icons.Default.Dashboard
                    TextureCategory.COLORMAP -> Icons.Default.Palette
                },
                contentDescription = null,
                tint = Color(0xFFFFB6C1),
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = category.displayName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = "$textureCount textures",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun CategoryLibraryScreen(
    category: TextureCategory,
    textures: List<TextureItem>,
    viewModel: TexturePackViewModel,
    packId: String,
    onTextureSelected: (TextureItem) -> Unit,
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { 
            viewModel.addTexture(packId, category, uri)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Back Button and Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackPressed) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            
            Text(
                text = category.displayName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            
            Text(
                text = "${textures.size} textures",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Search Bar (placeholder)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(25.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Search",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Texture Grid
        if (textures.isNotEmpty()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                gridItems(textures) { texture ->
                    TextureGridItem(
                        texture = texture,
                        onClick = { onTextureSelected(texture) }
                    )
                }
            }
        } else {
            // Empty State
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No textures found",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Import textures to get started",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

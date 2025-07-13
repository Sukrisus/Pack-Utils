package com.packify.packaverse.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.packify.packaverse.data.TextureCategory
import com.packify.packaverse.data.TextureItem
import com.packify.packaverse.viewmodel.TexturePackViewModel
import com.packify.packaverse.ui.components.BottomNavigationBar
import com.packify.packaverse.ui.components.TextureDropdown
import kotlinx.coroutines.launch

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
    
    var selectedCategory by remember { mutableStateOf<TextureCategory?>(null) }
    var showTextureLibrary by remember { mutableStateOf(false) }
    var selectedPack by remember { mutableStateOf<com.packify.packaverse.data.TexturePack?>(null) }
    
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
        } else if (showTextureLibrary && selectedCategory != null && selectedPack != null) {
            TextureLibraryScreen(
                category = selectedCategory!!,
                packId = selectedPack!!.id,
                onTextureSelected = { texture ->
                    onNavigateToTextureEditor(selectedPack!!.id, texture.name)
                },
                onNavigateBack = { showTextureLibrary = false },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else if (texturePacks.isEmpty()) {
            EmptyStateView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            DashboardContent(
                texturePacks = texturePacks,
                onCategorySelected = { category, pack ->
                    selectedCategory = category
                    selectedPack = pack
                    showTextureLibrary = true
                },
                errorMessage = errorMessage,
                successMessage = successMessage,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    }
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
fun DashboardContent(
    texturePacks: List<com.packify.packaverse.data.TexturePack>,
    onCategorySelected: (TextureCategory, com.packify.packaverse.data.TexturePack) -> Unit,
    errorMessage: String?,
    successMessage: String?,
    modifier: Modifier = Modifier
) {
    var selectedPack by remember { mutableStateOf<com.packify.packaverse.data.TexturePack?>(null) }
    
    Column(modifier = modifier) {
        // Messages
        errorMessage?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        
        successMessage?.let { success ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = success,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        // Pack selector
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Select Texture Pack",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                if (texturePacks.isNotEmpty()) {
                    selectedPack = selectedPack ?: texturePacks.first()
                    
                    texturePacks.forEach { pack ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedPack?.id == pack.id,
                                onClick = { 
                                    selectedPack = pack
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFFFFB6C1)
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = pack.name,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
        
        // Category grid
        if (selectedPack != null) {
            Text(
                text = "Select Category",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(16.dp)
            )
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(TextureCategory.values()) { category ->
                    CategoryCard(
                        category = category,
                        onClick = { onCategorySelected(category, selectedPack!!) }
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryCard(
    category: TextureCategory,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val icon = when (category) {
                TextureCategory.BLOCKS -> Icons.Default.Texture
                TextureCategory.ITEMS -> Icons.Default.Inventory
                TextureCategory.ENTITY -> Icons.Default.Person
                TextureCategory.ENVIRONMENT -> Icons.Default.Landscape
                TextureCategory.GUI -> Icons.Default.Dashboard
                TextureCategory.PARTICLE -> Icons.Default.Scatter
                TextureCategory.MISC -> Icons.Default.Category
            }
            
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color(0xFFFFB6C1)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = category.displayName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun TextureLibraryScreen(
    category: TextureCategory,
    packId: String,
    onTextureSelected: (TextureItem) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var textures by remember { mutableStateOf<List<TextureItem>>(emptyList()) }
    
    // Load textures from assets
    LaunchedEffect(category) {
        scope.launch {
            val textureList = mutableListOf<TextureItem>()
            try {
                val assetManager = context.assets
                val files = assetManager.list("base/${category.assetPath}")
                files?.forEach { fileName ->
                    if (fileName.endsWith(".png")) {
                        val textureName = fileName.removeSuffix(".png")
                        textureList.add(
                            TextureItem(
                                name = textureName,
                                originalPath = "base/${category.assetPath}/$fileName",
                                mcpePath = "${category.mcpePath}$fileName",
                                category = category,
                                isCustom = false
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                // Handle error
            }
            textures = textureList
        }
    }
    
    Column(modifier = modifier) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = category.displayName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        // Texture grid
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 80.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(textures) { texture ->
                TextureItemCard(
                    texture = texture,
                    onClick = { onTextureSelected(texture) }
                )
            }
        }
    }
}

@Composable
fun TextureItemCard(
    texture: TextureItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    
    LaunchedEffect(texture) {
        try {
            val inputStream = context.assets.open(texture.originalPath)
            bitmap = BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            bitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = texture.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    filterQuality = FilterQuality.None
                )
            }
        }
    }
}
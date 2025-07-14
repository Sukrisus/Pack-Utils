package com.packify.packaverse.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.packify.packaverse.viewmodel.TexturePackViewModel
import coil.compose.AsyncImage
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.background
import java.io.IOException
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextureManagementScreen(
    category: TextureCategory,
    packId: String,
    viewModel: TexturePackViewModel,
    onNavigateBack: () -> Unit,
    onTextureSelected: (TextureItem) -> Unit,
    onOpenLibrary: (String) -> Unit, // new callback for opening the library
    navController: NavController
) {
    val context = LocalContext.current
    val textures by viewModel.textures.collectAsState()
    val categoryTextures = textures.filter { it.category == category }

    // State for which texture to replace
    var textureToReplace by remember { mutableStateOf<TextureItem?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val texture = textureToReplace
            if (texture != null) {
                viewModel.replaceTexture(packId, texture.mcpePath, it)
                textureToReplace = null
            } else {
                // Add new texture if not replacing
                viewModel.addTexture(packId, category, it)
            }
        }
    }

    val navController = navController
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    var showAssetDialog by remember { mutableStateOf(false) }
    var pendingAssetPath by remember { mutableStateOf<String?>(null) }

    // Listen for asset selection from library
    LaunchedEffect(Unit) {
        savedStateHandle?.getLiveData<String>("selectedAssetPath")?.observeForever { assetPath ->
            if (assetPath != null) {
                pendingAssetPath = assetPath
                showAssetDialog = true
                savedStateHandle.remove<String>("selectedAssetPath")
            }
        }
    }

    LaunchedEffect(category) {
        viewModel.loadTextures(packId, category)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "${category.displayName} Textures",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onOpenLibrary("base/${category.name.lowercase()}/") }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Texture")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .systemBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with category info
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
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
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = category.displayName,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${categoryTextures.size} textures",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    FilledTonalButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Texture")
                    }
                }
            }
            // Texture Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Only the + button as the first item
                item {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable { onOpenLibrary("base/${category.name.lowercase()}/") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Texture",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                // Only show actual textures, no placeholders
                items(categoryTextures) { texture ->
                    TextureGridItem(
                        texture = texture,
                        onClick = { onTextureSelected(texture) },
                        onEdit = {
                            navController.navigate("texture_editor/$packId/${texture.name}")
                        },
                        onImportFromGallery = {
                            textureToReplace = texture
                            imagePickerLauncher.launch("image/*")
                        }
                    )
                }
            }
        }
    }
    if (showAssetDialog && pendingAssetPath != null) {
        AlertDialog(
            onDismissRequest = {
                showAssetDialog = false
                pendingAssetPath = null
            },
            title = { Text("Texture Options") },
            text = {
                Column {
                    Button(onClick = {
                        showAssetDialog = false
                        // Launch image picker to import from device and replace the texture image
                        textureToReplace = null // No existing texture, so just import
                        imagePickerLauncher.launch("image/*")
                        pendingAssetPath = null
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Import from Device")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        showAssetDialog = false
                        // Add the asset as a new texture
                        val uri = Uri.parse(pendingAssetPath!!)
                        viewModel.addTexture(packId, category, uri)
                        // Reload textures and navigate to the editor for the new texture
                        viewModel.loadTextures(packId, category)
                        val fileName = pendingAssetPath!!.substringAfterLast('/')
                        val textureName = fileName.substringBeforeLast('.')
                        // Wait for the new texture to be available, then navigate
                        LaunchedEffect(viewModel.textures.value) {
                            val newTexture = viewModel.textures.value.find { it.name == textureName && it.category == category }
                            if (newTexture != null) {
                                navController.navigate("texture_editor/$packId/${newTexture.name}")
                                pendingAssetPath = null
                            }
                        }
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Edit")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = {
                    showAssetDialog = false
                    pendingAssetPath = null
                }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextureGridItem(
    texture: TextureItem,
    onClick: () -> Unit,
    onEdit: () -> Unit = {},
    onImportFromGallery: () -> Unit = {}
) {
    var showDialog by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .size(80.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = if (texture.originalPath.startsWith("asset://")) {
                    texture.originalPath.removePrefix("asset://")
                } else {
                    texture.originalPath
                },
                contentDescription = texture.name,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                contentScale = ContentScale.Crop,
                filterQuality = androidx.compose.ui.graphics.FilterQuality.None
            )
            if (texture.isCustom) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Color.Black.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp)
                        )
                )
                IconButton(
                    onClick = { showDialog = true },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = Color.White
                    )
                }
            }
        }
    }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Texture Options") },
            text = {
                Column {
                    Button(onClick = {
                        showDialog = false
                        onEdit()
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Edit")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        showDialog = false
                        onImportFromGallery()
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Import from Gallery")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// Stub for the new LibraryScreen composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(folderPath: String, onImageSelected: (String) -> Unit, onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    // List all .png files in the given assets folder
    val imageFiles = remember(folderPath) {
        try {
            context.assets.list(folderPath)?.filter { it.endsWith(".png") } ?: emptyList()
        } catch (e: IOException) {
            emptyList<String>()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Image from $folderPath") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (imageFiles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("No images found in $folderPath")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(12.dp)
            ) {
                items(imageFiles) { fileName ->
                    Card(
                        modifier = Modifier
                            .size(80.dp)
                            .clickable { onImageSelected("asset://$folderPath$fileName") },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        AsyncImage(
                            model = "file:///android_asset/$folderPath$fileName",
                            contentDescription = fileName,
                            modifier = Modifier.fillMaxSize().padding(4.dp),
                            contentScale = ContentScale.Crop,
                            filterQuality = FilterQuality.None
                        )
                    }
                }
            }
        }
    }
}
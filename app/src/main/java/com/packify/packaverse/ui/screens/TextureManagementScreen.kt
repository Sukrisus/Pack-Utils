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

@file:OptIn(ExperimentalMaterialApi::class)
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
                .padding(16.dp)
        ) {
            // Header with category info
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFB6C1).copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(12.dp)
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
                        tint = Color(0xFFFFB6C1),
                        modifier = Modifier.size(32.dp)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = category.displayName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${categoryTextures.size} textures",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFB6C1),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Texture")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Texture Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFFB6C1).copy(alpha = 0.3f))
                            .clickable { onOpenLibrary("base/${category.name.lowercase()}/") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Texture",
                            tint = Color(0xFFFF6B6B),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                items(categoryTextures) { texture ->
                    TextureGridItem(
                        texture = texture,
                        onClick = { onTextureSelected(texture) },
                        onEdit = {
                            // Navigate to the editor screen for this texture
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
}

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
        shape = CircleShape
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
                    .padding(4.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                filterQuality = androidx.compose.ui.graphics.FilterQuality.None
            )
            if (texture.isCustom) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Color.Black.copy(alpha = 0.3f),
                            CircleShape
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
                            contentScale = ContentScale.None,
                            filterQuality = FilterQuality.None
                        )
                    }
                }
            }
        }
    }
}
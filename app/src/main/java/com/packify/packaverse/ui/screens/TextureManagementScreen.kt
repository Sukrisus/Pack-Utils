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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextureManagementScreen(
    category: TextureCategory,
    packId: String,
    viewModel: TexturePackViewModel,
    onNavigateBack: () -> Unit,
    onTextureSelected: (TextureItem) -> Unit,
    onOpenLibrary: (String) -> Unit // new callback for opening the library
) {
    val context = LocalContext.current
    val textures by viewModel.textures.collectAsState()
    val categoryTextures = textures.filter { it.category == category }
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { 
            viewModel.addTexture(packId, category, uri)
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
                        onClick = { onTextureSelected(texture) }
                    )
                }
            }
        }
    }
}

@Composable
fun TextureGridItem(
    texture: TextureItem,
    onClick: () -> Unit
) {
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
            
            // Overlay for custom textures
            if (texture.isCustom) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Color.Black.copy(alpha = 0.3f),
                            CircleShape
                        )
                )
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Custom texture",
                    tint = Color.White,
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                )
            }
        }
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
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}
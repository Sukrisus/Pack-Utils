package com.packify.packaverse.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.packify.packaverse.data.TextureCategory
import com.packify.packaverse.data.TextureItem
import com.packify.packaverse.data.TexturePack
import com.packify.packaverse.viewmodel.TexturePackViewModel
import com.packify.packaverse.ui.components.BottomNavigationBar
import java.io.File

enum class DashboardView {
    CATEGORIES, LIBRARY, PREVIEW
}

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
    
    var currentView by remember { mutableStateOf(DashboardView.CATEGORIES) }
    var selectedCategory by remember { mutableStateOf<TextureCategory?>(null) }
    var selectedImage by remember { mutableStateOf<String?>(null) }
    var selectedImageName by remember { mutableStateOf<String?>(null) }
    var selectedPackId by remember { mutableStateOf<String?>(null) }
    var showShareDialog by remember { mutableStateOf(false) }
    var baseImages by remember { mutableStateOf<List<String>>(emptyList()) }
    
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        uri?.let { exportUri ->
            selectedPackId?.let { packId ->
                viewModel.exportTexturePack(packId, exportUri)
            }
        }
    }
    
    // Load base images when category is selected
    LaunchedEffect(selectedCategory) {
        selectedCategory?.let { category ->
            val categoryPath = when (category) {
                TextureCategory.ITEMS -> "base/items"
                TextureCategory.BLOCKS -> "base/blocks"
                TextureCategory.ENTITY -> "base/entity"
                TextureCategory.ENVIRONMENT -> "base/environment"
                TextureCategory.GUI -> "base/gui"
                TextureCategory.PARTICLE -> "base/particle"
                TextureCategory.MISC -> "base/misc"
            }
            
            try {
                val assetManager = context.assets
                val files = assetManager.list(categoryPath)?.toList() ?: emptyList()
                baseImages = files.filter { it.endsWith(".png") }
            } catch (e: Exception) {
                baseImages = emptyList()
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
                        text = when (currentView) {
                            DashboardView.CATEGORIES -> "Texture Editor"
                            DashboardView.LIBRARY -> selectedCategory?.displayName ?: "Library"
                            DashboardView.PREVIEW -> "Preview"
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Normal
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        when (currentView) {
                            DashboardView.CATEGORIES -> onNavigateBack()
                            DashboardView.LIBRARY -> currentView = DashboardView.CATEGORIES
                            DashboardView.PREVIEW -> currentView = DashboardView.LIBRARY
                        }
                    }) {
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentView) {
                DashboardView.CATEGORIES -> {
                    CategorySelectionView(
                        onCategorySelected = { category ->
                            selectedCategory = category
                            currentView = DashboardView.LIBRARY
                        }
                    )
                }
                DashboardView.LIBRARY -> {
                    ImageLibraryView(
                        category = selectedCategory!!,
                        images = baseImages,
                        onImageSelected = { imageName ->
                            selectedImage = "file:///android_asset/base/${selectedCategory!!.name.lowercase()}/$imageName"
                            selectedImageName = imageName
                            currentView = DashboardView.PREVIEW
                        }
                    )
                }
                DashboardView.PREVIEW -> {
                    ImagePreviewView(
                        imagePath = selectedImage!!,
                        imageName = selectedImageName!!,
                        category = selectedCategory!!,
                        texturePacks = texturePacks,
                        onEditTexture = { packId ->
                            selectedPackId = packId
                            onNavigateToTextureEditor(packId, selectedImageName!!)
                        }
                    )
                }
            }
        }
        
        // Share Dialog
        if (showShareDialog) {
            ShareDialog(
                texturePacks = texturePacks,
                onDismiss = { showShareDialog = false },
                onSharePack = { packId ->
                    selectedPackId = packId
                    val pack = texturePacks.find { it.id == packId }
                    pack?.let {
                        exportLauncher.launch("${it.name}.zip")
                    }
                    showShareDialog = false
                },
                onShareApp = {
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Check out Packify - MCPE Texture Pack Editor! Create amazing texture packs for Minecraft PE! Download now!")
                        putExtra(Intent.EXTRA_SUBJECT, "Packify - MCPE Texture Pack Editor")
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Packify"))
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
fun CategorySelectionView(
    onCategorySelected: (TextureCategory) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Select a category to browse textures",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        items(TextureCategory.values()) { category ->
            CategoryCard(
                category = category,
                onClick = { onCategorySelected(category) }
            )
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
            .height(80.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
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
            
            Text(
                text = category.displayName,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun ImageLibraryView(
    category: TextureCategory,
    images: List<String>,
    onImageSelected: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(images) { imageName ->
            Card(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clickable { onImageSelected(imageName) },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data("file:///android_asset/base/${category.name.lowercase()}/$imageName")
                            .build(),
                        contentDescription = imageName,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

@Composable
fun ImagePreviewView(
    imagePath: String,
    imageName: String,
    category: TextureCategory,
    texturePacks: List<TexturePack>,
    onEditTexture: (String) -> Unit
) {
    var selectedPackId by remember { mutableStateOf(texturePacks.firstOrNull()?.id) }
    var showPackSelection by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Circular Image Preview
        Card(
            modifier = Modifier.size(200.dp),
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imagePath)
                        .build(),
                    contentDescription = imageName,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = imageName.removeSuffix(".png"),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = category.displayName,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Pack Selection
        if (texturePacks.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPackSelection = true },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Archive,
                        contentDescription = null,
                        tint = Color(0xFFFFB6C1)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Selected Pack",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = texturePacks.find { it.id == selectedPackId }?.name ?: "Select a pack",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Edit Button
            selectedPackId?.let { packId ->
                Button(
                    onClick = { onEditTexture(packId) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFB6C1),
                        contentColor = Color.Black
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Edit Texture",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            Text(
                text = "No texture packs found. Create one first!",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
    
    // Pack Selection Dialog
    if (showPackSelection) {
        Dialog(onDismissRequest = { showPackSelection = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
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
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    texturePacks.forEach { pack ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    selectedPackId = pack.id
                                    showPackSelection = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedPackId == pack.id) 
                                    Color(0xFFFFB6C1).copy(alpha = 0.3f) 
                                else 
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = pack.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = pack.description,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
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
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Share",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
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
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "Or share a texture pack:",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    texturePacks.forEach { pack ->
                        OutlinedButton(
                            onClick = { onSharePack(pack.id) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Archive, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(pack.name)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}




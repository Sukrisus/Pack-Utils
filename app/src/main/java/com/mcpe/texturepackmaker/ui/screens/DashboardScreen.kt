package com.mcpe.texturepackmaker.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mcpe.texturepackmaker.data.TextureCategory
import com.mcpe.texturepackmaker.data.TextureItem
import com.mcpe.texturepackmaker.viewmodel.TexturePackViewModel
import com.mcpe.texturepackmaker.ui.components.BottomNavigationBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: TexturePackViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit = {}
) {
    val texturePacks by viewModel.texturePacks.collectAsState()
    val textures by viewModel.textures.collectAsState()
    val isLoading by viewModel.isLoading
    val errorMessage by viewModel.errorMessage
    val successMessage by viewModel.successMessage
    
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
fun TextureEditorContent(
    texturePacks: List<com.mcpe.texturepackmaker.data.TexturePack>,
    textures: List<TextureItem>,
    viewModel: TexturePackViewModel,
    errorMessage: String?,
    successMessage: String?,
    modifier: Modifier = Modifier
) {
    var selectedPack by remember { mutableStateOf<com.mcpe.texturepackmaker.data.TexturePack?>(null) }
    var selectedCategory by remember { mutableStateOf<TextureCategory?>(null) }
    
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
                                    selectedCategory = null
                                    viewModel.clearMessages()
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
        
        // Category selector
        if (selectedPack != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Texture Categories",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    LazyColumn {
                        items(TextureCategory.values()) { category ->
                            CategoryItem(
                                category = category,
                                isSelected = selectedCategory == category,
                                onClick = { 
                                    selectedCategory = category
                                    viewModel.loadTextures(selectedPack!!.id, category)
                                    viewModel.clearMessages()
                                }
                            )
                        }
                    }
                }
            }
        }
        
        // Texture list
        if (selectedCategory != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "${selectedCategory!!.displayName} Textures",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (textures.isEmpty()) {
                        Text(
                            text = "No textures found in this category. Add some texture files to get started.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    } else {
                        LazyColumn {
                            items(textures) { texture ->
                                TextureItem(
                                    texture = texture,
                                    onReplaceTexture = { uri ->
                                        viewModel.replaceTexture(selectedPack!!.id, texture.mcpePath, uri)
                                    }
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
fun CategoryItem(
    category: TextureCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFFFB6C1).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
                tint = if (isSelected) Color(0xFFFFB6C1) else MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = category.displayName,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                color = if (isSelected) Color(0xFFFFB6C1) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TextureItem(
    texture: TextureItem,
    onReplaceTexture: (Uri) -> Unit
) {
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onReplaceTexture(it) }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = texture.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = texture.mcpePath,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            Button(
                onClick = { imagePickerLauncher.launch("image/*") },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFB6C1),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Replace", fontSize = 12.sp)
            }
        }
    }
}


package com.packify.packaverse.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.packify.packaverse.data.TexturePack
import com.packify.packaverse.viewmodel.TexturePackViewModel
import androidx.core.content.FileProvider
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPackScreen(
    packId: String,
    viewModel: TexturePackViewModel,
    onNavigateBack: () -> Unit
) {
    val texturePacks by viewModel.texturePacks.collectAsState()
    val isLoading by viewModel.isLoading
    val errorMessage by viewModel.errorMessage
    val successMessage by viewModel.successMessage
    
    val texturePack = texturePacks.find { it.id == packId }
    
    var packName by remember { mutableStateOf("") }
    var packDescription by remember { mutableStateOf("") }
    var majorVersion by remember { mutableStateOf("1") }
    var minorVersion by remember { mutableStateOf("0") }
    var patchVersion by remember { mutableStateOf("0") }
    var minEngineMajor by remember { mutableStateOf("1") }
    var minEngineMinor by remember { mutableStateOf("16") }
    var minEnginePatch by remember { mutableStateOf("0") }
    
    LaunchedEffect(Unit) {
        viewModel.loadTexturePacks()
    }
    
    LaunchedEffect(texturePack) {
        texturePack?.let { pack ->
            packName = pack.name
            packDescription = pack.description
            majorVersion = pack.version.getOrElse(0) { 1 }.toString()
            minorVersion = pack.version.getOrElse(1) { 0 }.toString()
            patchVersion = pack.version.getOrElse(2) { 0 }.toString()
            // Default min_engine_version values
            minEngineMajor = "1"
            minEngineMinor = "16"
            minEnginePatch = "0"
        }
    }
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.updatePackIcon(packId, it)
        }
    }
    
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        uri?.let {
            viewModel.exportTexturePack(packId, it)
        }
    }
    
    val context = LocalContext.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Edit Texture Pack",
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
        }
    ) { paddingValues ->
        
        if (texturePack == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Texture pack not found",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
            return@Scaffold
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            
            // Messages
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
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
                        .padding(bottom = 16.dp),
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
            
            // Pack Icon Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Pack Icon",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Change the pack_icon.png file for your texture pack",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = Color.White
                        ),
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select New Icon")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Pack Details Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Pack Details",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Edit the manifest.json properties",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Pack name
                    OutlinedTextField(
                        value = packName,
                        onValueChange = { 
                            packName = it
                            viewModel.clearMessages()
                        },
                        label = { Text("Pack Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFFB6C1),
                            focusedLabelColor = Color(0xFFFFB6C1)
                        ),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Pack description
                    OutlinedTextField(
                        value = packDescription,
                        onValueChange = { 
                            packDescription = it
                            viewModel.clearMessages()
                        },
                        label = { Text("Pack Description") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFFB6C1),
                            focusedLabelColor = Color(0xFFFFB6C1)
                        ),
                        maxLines = 3
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Version fields
                    Text(
                        text = "Version",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = majorVersion,
                            onValueChange = { if (it.all { char -> char.isDigit() }) majorVersion = it },
                            label = { Text("Major") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFFB6C1),
                                focusedLabelColor = Color(0xFFFFB6C1)
                            )
                        )
                        OutlinedTextField(
                            value = minorVersion,
                            onValueChange = { if (it.all { char -> char.isDigit() }) minorVersion = it },
                            label = { Text("Minor") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFFB6C1),
                                focusedLabelColor = Color(0xFFFFB6C1)
                            )
                        )
                        OutlinedTextField(
                            value = patchVersion,
                            onValueChange = { if (it.all { char -> char.isDigit() }) patchVersion = it },
                            label = { Text("Patch") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFFB6C1),
                                focusedLabelColor = Color(0xFFFFB6C1)
                            )
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Min Engine Version fields
                    Text(
                        text = "Minimum Engine Version",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = minEngineMajor,
                            onValueChange = { if (it.all { char -> char.isDigit() }) minEngineMajor = it },
                            label = { Text("Major") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFFB6C1),
                                focusedLabelColor = Color(0xFFFFB6C1)
                            )
                        )
                        OutlinedTextField(
                            value = minEngineMinor,
                            onValueChange = { if (it.all { char -> char.isDigit() }) minEngineMinor = it },
                            label = { Text("Minor") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFFB6C1),
                                focusedLabelColor = Color(0xFFFFB6C1)
                            )
                        )
                        OutlinedTextField(
                            value = minEnginePatch,
                            onValueChange = { if (it.all { char -> char.isDigit() }) minEnginePatch = it },
                            label = { Text("Patch") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFFB6C1),
                                focusedLabelColor = Color(0xFFFFB6C1)
                            )
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Update button
                    Button(
                        onClick = {
                            val version = listOf(
                                majorVersion.toIntOrNull() ?: 1,
                                minorVersion.toIntOrNull() ?: 0,
                                patchVersion.toIntOrNull() ?: 0
                            )
                            val minEngineVersion = listOf(
                                minEngineMajor.toIntOrNull() ?: 1,
                                minEngineMinor.toIntOrNull() ?: 16,
                                minEnginePatch.toIntOrNull() ?: 0
                            )
                            viewModel.updateManifest(packId, packName, packDescription, version, minEngineVersion)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFB6C1),
                            contentColor = Color.Black
                        ),
                        enabled = !isLoading && packName.isNotBlank()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.Black
                            )
                        } else {
                            Text("UPDATE PACK")
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Export & Share Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Export & Share",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Export and share your texture pack",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { exportLauncher.launch("${texturePack.name}.mcpack") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFB6C1),
                                contentColor = Color.Black
                            ),
                            enabled = !isLoading
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("EXPORT")
                        }
                        
                        Button(
                            onClick = {
                                // Export the pack to a temporary file, then share it
                                val exportFile = java.io.File(context.cacheDir, "${texturePack.name}.mcpack")
                                viewModel.exportTexturePackToFile(packId, exportFile) { success ->
                                    if (success) {
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            context.packageName + ".provider",
                                            exportFile
                                        )
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/zip"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Texture Pack"))
                                    } else {
                                        viewModel.showError("Failed to export pack for sharing")
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF69B4),
                                contentColor = Color.White
                            ),
                            enabled = !isLoading
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SHARE")
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
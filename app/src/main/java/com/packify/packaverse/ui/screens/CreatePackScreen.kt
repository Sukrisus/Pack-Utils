package com.packify.packaverse.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.packify.packaverse.viewmodel.TexturePackViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePackScreen(
    viewModel: TexturePackViewModel,
    onNavigateBack: () -> Unit,
    onPackCreated: () -> Unit
) {
    var packName by remember { mutableStateOf("") }
    var packDescription by remember { mutableStateOf("") }
    
    val isLoading by viewModel.isLoading
    val errorMessage by viewModel.errorMessage
    val successMessage by viewModel.successMessage
    
    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            onPackCreated()
            viewModel.clearMessages()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Create Texture Pack",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Pack name input
            OutlinedTextField(
                value = packName,
                onValueChange = { 
                    packName = it
                    viewModel.clearMessages()
                },
                label = { Text("Pack Name") },
                placeholder = { Text("Enter pack name") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFFB6C1),
                    focusedLabelColor = Color(0xFFFFB6C1)
                ),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Pack description input
            OutlinedTextField(
                value = packDescription,
                onValueChange = { 
                    packDescription = it
                    viewModel.clearMessages()
                },
                label = { Text("Pack Description") },
                placeholder = { Text("Enter pack description") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFFB6C1),
                    focusedLabelColor = Color(0xFFFFB6C1)
                ),
                maxLines = 3
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Create button
            Button(
                onClick = {
                    if (packName.isNotBlank()) {
                        viewModel.createTexturePack(
                            name = packName,
                            description = packDescription.ifBlank { "My texture pack" }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFB6C1),
                    contentColor = Color.White
                ),
                shape = CircleShape,
                enabled = !isLoading && packName.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.Black
                    )
                } else {
                    Text(
                        text = "CREATE PACK",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Error message
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Info text
            Text(
                text = "This will create a new texture pack with the standard MCPE structure including manifest.json and texture directories.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}
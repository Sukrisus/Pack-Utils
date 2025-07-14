package com.packify.packaverse

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.packify.packaverse.ui.screens.HomeScreen
import com.packify.packaverse.ui.screens.CreatePackScreen
import com.packify.packaverse.ui.screens.EditPackScreen
import com.packify.packaverse.ui.screens.PackListScreen
import com.packify.packaverse.ui.screens.DashboardScreen
import com.packify.packaverse.ui.screens.SettingsScreen
import com.packify.packaverse.ui.screens.TextureEditorScreen
import com.packify.packaverse.ui.theme.PackifyTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import com.packify.packaverse.viewmodel.TexturePackViewModel
import android.annotation.TargetApi
import android.annotation.SuppressLint

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PackifyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PackifyApp()
                }
            }
        }
    }
}

@SuppressLint("NewApi")
@Composable
fun PackifyApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val viewModel: TexturePackViewModel = viewModel { 
        TexturePackViewModel(context.applicationContext as android.app.Application)
    }
    
    var hasStoragePermission by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    
    // Check if we have storage permission
    LaunchedEffect(Unit) {
        hasStoragePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            hasApi30ExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
        
        if (!hasStoragePermission) {
            showPermissionDialog = true
        }
    }
    
    // Permission launcher for Android 11+ (MANAGE_EXTERNAL_STORAGE)
    val manageStorageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        hasStoragePermission = Environment.isExternalStorageManager()
        showPermissionDialog = !hasStoragePermission
    }
    
    // Permission launcher for Android 10 and below
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasStoragePermission = permissions.values.all { it }
        showPermissionDialog = !hasStoragePermission
    }
    
    if (showPermissionDialog) {
        PermissionDialog(
            onGrantPermission = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    manageStorageLauncher.launch(intent)
                } else {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                    )
                }
            },
            onDismiss = {
                // Allow the app to continue without permission for now
                showPermissionDialog = false
            }
        )
    }
    
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                onNavigateToCreate = { navController.navigate("create") },
                onNavigateToPackList = { navController.navigate("pack_list") },
                onNavigateToDashboard = { navController.navigate("dashboard") },
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }
        
        composable("create") {
            CreatePackScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onPackCreated = { 
                    navController.popBackStack()
                    navController.navigate("pack_list")
                }
            )
        }
        
        composable("pack_list") {
            PackListScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onEditPack = { packId -> 
                    navController.navigate("edit_pack/$packId")
                }
            )
        }
        
        composable("edit_pack/{packId}") { backStackEntry ->
            val packId = backStackEntry.arguments?.getString("packId") ?: ""
            EditPackScreen(
                packId = packId,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable("dashboard") {
            DashboardScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToCategory = { category ->
                    // Navigate to the texture management screen for the selected pack and category
                    val selectedPackId = viewModel.texturePacks.value.firstOrNull()?.id ?: ""
                    if (selectedPackId.isNotEmpty()) {
                        navController.navigate("texture_management/$selectedPackId/${category.name}")
                    }
                }
            )
        }
        
        composable("settings") {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHome = { 
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onNavigateToDashboard = { 
                    navController.navigate("dashboard") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                },
                viewModel = viewModel
            )
        }
        
        composable("texture_editor/{packId}/{textureName}") { backStackEntry ->
            val packId = backStackEntry.arguments?.getString("packId") ?: ""
            val textureName = backStackEntry.arguments?.getString("textureName") ?: ""
            val texture = viewModel.getTextureByName(packId, textureName)
            texture?.let {
                TextureEditorScreen(
                    texture = it,
                    viewModel = viewModel,
                    packId = packId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
        
        composable("texture_management/{packId}/{category}") { backStackEntry ->
            val packId = backStackEntry.arguments?.getString("packId") ?: ""
            val categoryName = backStackEntry.arguments?.getString("category") ?: ""
            val category = com.packify.packaverse.data.TextureCategory.values().find { it.name == categoryName }
            category?.let {
                com.packify.packaverse.ui.screens.TextureManagementScreen(
                    category = it,
                    packId = packId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onTextureSelected = { texture ->
                        navController.navigate("texture_editor/$packId/${texture.name}")
                    }
                )
            }
        }
    }
}

@Composable
fun PermissionDialog(
    onGrantPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Storage Permission Required",
                fontSize = 18.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "This app needs storage permission to save your texture pack projects locally so they won't be deleted when you uninstall the app. Your projects will be saved in the 'packify/projects' folder.",
                textAlign = TextAlign.Justify,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        },
        confirmButton = {
            Button(
                onClick = onGrantPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Grant Permission")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Skip for now")
            }
        }
    )
}

@TargetApi(30)
fun hasApi30ExternalStorageManager(): Boolean {
    return try {
        Environment.isExternalStorageManager()
    } catch (e: Exception) {
        false
    }
}
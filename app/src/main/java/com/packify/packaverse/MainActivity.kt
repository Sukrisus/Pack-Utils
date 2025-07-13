package com.packify.packaverse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.platform.LocalContext
import com.packify.packaverse.viewmodel.TexturePackViewModel

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

@Composable
fun PackifyApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val viewModel: TexturePackViewModel = viewModel { 
        TexturePackViewModel(context.applicationContext as android.app.Application)
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
                onNavigateToTextureEditor = { packId, textureName ->
                    navController.navigate("texture_editor/$packId/$textureName")
                }
            )
        }
        
        composable("settings") {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHome = { 
                    navController.popBackStack("home", false)
                },
                onNavigateToDashboard = { 
                    navController.popBackStack("dashboard", false)
                }
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
    }
}
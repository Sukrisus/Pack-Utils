package com.mcpe.texturepackmaker

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
import com.mcpe.texturepackmaker.ui.screens.HomeScreen
import com.mcpe.texturepackmaker.ui.screens.CreatePackScreen
import com.mcpe.texturepackmaker.ui.screens.EditPackScreen
import com.mcpe.texturepackmaker.ui.screens.PackListScreen
import com.mcpe.texturepackmaker.ui.theme.MCPETexturePackMakerTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mcpe.texturepackmaker.viewmodel.TexturePackViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MCPETexturePackMakerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MCPETexturePackMakerApp()
                }
            }
        }
    }
}

@Composable
fun MCPETexturePackMakerApp() {
    val navController = rememberNavController()
    val viewModel: TexturePackViewModel = viewModel()
    
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                onNavigateToCreate = { navController.navigate("create") },
                onNavigateToPackList = { navController.navigate("pack_list") }
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
    }
}
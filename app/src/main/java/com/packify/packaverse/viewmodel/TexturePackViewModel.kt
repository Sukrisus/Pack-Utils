package com.packify.packaverse.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.packify.packaverse.data.TexturePack
import com.packify.packaverse.data.TextureItem
import com.packify.packaverse.data.TextureCategory
import com.packify.packaverse.repository.TexturePackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class TexturePackViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = TexturePackRepository(application.applicationContext)
    private val sharedPreferences = application.getSharedPreferences("packify_settings", Context.MODE_PRIVATE)
    
    private val _texturePacks = MutableStateFlow<List<TexturePack>>(emptyList())
    val texturePacks: StateFlow<List<TexturePack>> = _texturePacks.asStateFlow()
    
    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading
    
    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage
    
    private val _successMessage = mutableStateOf<String?>(null)
    val successMessage: State<String?> = _successMessage
    
    private val _textures = MutableStateFlow<List<TextureItem>>(emptyList())
    val textures: StateFlow<List<TextureItem>> = _textures.asStateFlow()
    
    private val _currentPackId = mutableStateOf<String?>(null)
    val currentPackId: State<String?> = _currentPackId
    
    private val _hasUnsavedChanges = mutableStateOf(false)
    val hasUnsavedChanges: State<Boolean> = _hasUnsavedChanges
    
    init {
        loadTexturePacks()
    }
    
    fun loadTexturePacks() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val packs = repository.getTexturePacks()
                _texturePacks.value = packs
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load texture packs: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadTextures(packId: String, category: TextureCategory) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val textures = repository.getTextures(packId, category)
                _textures.value = textures
                _currentPackId.value = packId
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load textures: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun replaceTexture(packId: String, texturePath: String, newTextureUri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            repository.replaceTexture(packId, texturePath, newTextureUri)
                .onSuccess { 
                    _successMessage.value = "Texture replaced successfully!"
                    _hasUnsavedChanges.value = true
                    
                    // Auto-save if enabled
                    if (isAutoSaveEnabled()) {
                        autoSaveCurrentProject()
                    }
                    
                    // Reload textures for the current category
                    val currentTextures = _textures.value
                    if (currentTextures.isNotEmpty()) {
                        loadTextures(packId, currentTextures.first().category)
                    }
                }
                .onFailure { 
                    _errorMessage.value = "Failed to replace texture: ${it.message}"
                }
            
            _isLoading.value = false
        }
    }
    
    fun addTexture(packId: String, category: TextureCategory, textureUri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            repository.addTexture(packId, category, textureUri)
                .onSuccess { 
                    _successMessage.value = "Texture added successfully!"
                    _hasUnsavedChanges.value = true
                    
                    // Auto-save if enabled
                    if (isAutoSaveEnabled()) {
                        autoSaveCurrentProject()
                    }
                    
                    // Reload textures for the current category
                    loadTextures(packId, category)
                }
                .onFailure { 
                    _errorMessage.value = "Failed to add texture: ${it.message}"
                }
            
            _isLoading.value = false
        }
    }
    
    fun createTexturePack(name: String, description: String, onSuccess: () -> Unit = {}) {
        if (name.isBlank()) {
            _errorMessage.value = "Pack name cannot be empty"
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            repository.createTexturePack(name, description)
                .onSuccess { 
                    _successMessage.value = "Texture pack created successfully!"
                    loadTexturePacks()
                    onSuccess()
                }
                .onFailure { 
                    _errorMessage.value = "Failed to create texture pack: ${it.message}"
                }
            
            _isLoading.value = false
        }
    }
    
    fun updatePackIcon(packId: String, iconUri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            repository.updatePackIcon(packId, iconUri)
                .onSuccess { 
                    _successMessage.value = "Pack icon updated successfully!"
                    _hasUnsavedChanges.value = true
                    
                    // Auto-save if enabled
                    if (isAutoSaveEnabled()) {
                        autoSaveCurrentProject()
                    }
                    
                    loadTexturePacks()
                }
                .onFailure { 
                    _errorMessage.value = "Failed to update pack icon: ${it.message}"
                }
            
            _isLoading.value = false
        }
    }
    
    fun updateManifest(packId: String, name: String, description: String, version: List<Int>, minEngineVersion: List<Int> = listOf(1, 16, 0)) {
        if (name.isBlank()) {
            _errorMessage.value = "Pack name cannot be empty"
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            repository.updateManifest(packId, name, description, version, minEngineVersion)
                .onSuccess { 
                    _successMessage.value = "Pack details updated successfully!"
                    _hasUnsavedChanges.value = true
                    
                    // Auto-save if enabled
                    if (isAutoSaveEnabled()) {
                        autoSaveCurrentProject()
                    }
                    
                    loadTexturePacks()
                }
                .onFailure { 
                    _errorMessage.value = "Failed to update pack details: ${it.message}"
                }
            
            _isLoading.value = false
        }
    }
    
    fun exportTexturePack(packId: String, outputUri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            repository.exportTexturePack(packId, outputUri)
                .onSuccess { 
                    _successMessage.value = "Texture pack exported successfully!"
                }
                .onFailure { 
                    _errorMessage.value = "Failed to export texture pack: ${it.message}"
                }
            
            _isLoading.value = false
        }
    }
    
    fun deleteTexturePack(packId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            repository.deleteTexturePack(packId)
                .onSuccess { 
                    _successMessage.value = "Texture pack deleted successfully!"
                    loadTexturePacks()
                }
                .onFailure { 
                    _errorMessage.value = "Failed to delete texture pack: ${it.message}"
                }
            
            _isLoading.value = false
        }
    }
    
    fun resetToDefault(packId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            repository.resetToDefault(packId)
                .onSuccess { 
                    _successMessage.value = "Pack reset to default textures successfully!"
                    _hasUnsavedChanges.value = false
                    loadTexturePacks()
                    
                    // Reload current textures if viewing this pack
                    if (_currentPackId.value == packId) {
                        val currentTextures = _textures.value
                        if (currentTextures.isNotEmpty()) {
                            loadTextures(packId, currentTextures.first().category)
                        }
                    }
                }
                .onFailure { 
                    _errorMessage.value = "Failed to reset pack: ${it.message}"
                }
            
            _isLoading.value = false
        }
    }
    
    fun saveCurrentProject() {
        _currentPackId.value?.let { packId ->
            viewModelScope.launch {
                _isLoading.value = true
                _errorMessage.value = null
                
                repository.saveProject(packId)
                    .onSuccess { 
                        _successMessage.value = "Project saved successfully!"
                        _hasUnsavedChanges.value = false
                    }
                    .onFailure { 
                        _errorMessage.value = "Failed to save project: ${it.message}"
                    }
                
                _isLoading.value = false
            }
        }
    }
    
    private fun autoSaveCurrentProject() {
        _currentPackId.value?.let { packId ->
            viewModelScope.launch {
                // Add small delay to avoid too frequent saves
                delay(2000)
                
                repository.saveProject(packId)
                    .onSuccess { 
                        _hasUnsavedChanges.value = false
                    }
                    .onFailure { 
                        // Silent failure for auto-save
                    }
            }
        }
    }
    
    private fun isAutoSaveEnabled(): Boolean {
        return sharedPreferences.getBoolean("auto_save", true)
    }
    
    fun getTexturePackById(packId: String): TexturePack? {
        return _texturePacks.value.find { it.id == packId }
    }
    
    fun getTextureByName(packId: String, textureName: String): TextureItem? {
        return _textures.value.find { it.name == textureName }
    }
    
    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}
package com.mcpe.texturepackmaker.viewmodel

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mcpe.texturepackmaker.data.TexturePack
import com.mcpe.texturepackmaker.repository.TexturePackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TexturePackViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = TexturePackRepository(application.applicationContext)
    
    private val _texturePacks = MutableStateFlow<List<TexturePack>>(emptyList())
    val texturePacks: StateFlow<List<TexturePack>> = _texturePacks.asStateFlow()
    
    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading
    
    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage
    
    private val _successMessage = mutableStateOf<String?>(null)
    val successMessage: State<String?> = _successMessage
    
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
                    loadTexturePacks()
                }
                .onFailure { 
                    _errorMessage.value = "Failed to update pack icon: ${it.message}"
                }
            
            _isLoading.value = false
        }
    }
    
    fun updateManifest(packId: String, name: String, description: String, version: List<Int>) {
        if (name.isBlank()) {
            _errorMessage.value = "Pack name cannot be empty"
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            repository.updateManifest(packId, name, description, version)
                .onSuccess { 
                    _successMessage.value = "Pack details updated successfully!"
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
    
    fun getTexturePackById(packId: String): TexturePack? {
        return _texturePacks.value.find { it.id == packId }
    }
    
    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}
package com.packify.packaverse.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.packify.packaverse.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class TexturePackRepository(private val context: Context) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val texturePacksDir = File(context.filesDir, "texture_packs")
    
    init {
        if (!texturePacksDir.exists()) {
            texturePacksDir.mkdirs()
        }
    }
    
    suspend fun createTexturePack(name: String, description: String): Result<TexturePack> = withContext(Dispatchers.IO) {
        try {
            val texturePack = TexturePack(
                name = name,
                description = description
            )
            
            val packDir = File(texturePacksDir, texturePack.id)
            packDir.mkdirs()
            
            // Create manifest.json
            val manifest = Manifest(
                header = Header(
                    name = name,
                    description = description
                ),
                modules = listOf(
                    Module()
                )
            )
            
            val manifestFile = File(packDir, "manifest.json")
            manifestFile.writeText(gson.toJson(manifest))
            
            // Create textures directory structure
            TextureCategory.values().forEach { category ->
                File(packDir, category.mcpePath).mkdirs()
            }
            
            // Save texture pack metadata
            saveTexturePackMetadata(texturePack.copy(folderPath = packDir.absolutePath))
            
            Result.success(texturePack)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getTexturePacks(): List<TexturePack> = withContext(Dispatchers.IO) {
        try {
            texturePacksDir.listFiles()?.mapNotNull { dir ->
                if (dir.isDirectory) {
                    val metadataFile = File(dir, "metadata.json")
                    if (metadataFile.exists()) {
                        gson.fromJson(metadataFile.readText(), TexturePack::class.java)
                    } else null
                } else null
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    suspend fun getTextures(packId: String, category: TextureCategory): List<TextureItem> = withContext(Dispatchers.IO) {
        try {
            val packDir = File(texturePacksDir, packId)
            val categoryDir = File(packDir, category.mcpePath)
            
            // First, load base textures from assets
            val baseTextures = loadBaseTextures(category)
            
            // Then, load custom textures from pack directory
            val customTextures = if (categoryDir.exists()) {
                categoryDir.listFiles()?.filter { it.isFile && it.extension.lowercase() in listOf("png", "jpg", "jpeg") }
                    ?.map { file ->
                        TextureItem(
                            name = file.nameWithoutExtension,
                            originalPath = file.absolutePath,
                            mcpePath = "${category.mcpePath}${file.name}",
                            category = category,
                            isCustom = true
                        )
                    } ?: emptyList()
            } else {
                emptyList()
            }
            
            // Combine base and custom textures
            baseTextures + customTextures
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun loadBaseTextures(category: TextureCategory): List<TextureItem> {
        return try {
            val assetPath = "base/${category.assetPath}"
            context.assets.list(assetPath)?.filter { it.lowercase().endsWith(".png") }
                ?.map { fileName ->
                    TextureItem(
                        name = fileName.substringBeforeLast("."),
                        originalPath = "asset://$assetPath/$fileName",
                        mcpePath = "${category.mcpePath}$fileName",
                        category = category,
                        isCustom = false
                    )
                } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    suspend fun replaceTexture(packId: String, texturePath: String, newTextureUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val packDir = File(texturePacksDir, packId)
            val textureFile = File(packDir, texturePath)
            
            // Ensure the directory exists
            textureFile.parentFile?.mkdirs()
            
            context.contentResolver.openInputStream(newTextureUri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream)
                
                FileOutputStream(textureFile).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }
                
                bitmap.recycle()
            }
            
            Result.success(textureFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun saveEditedTexture(packId: String, category: TextureCategory, textureName: String, bitmap: Bitmap): Result<String> = withContext(Dispatchers.IO) {
        try {
            val packDir = File(texturePacksDir, packId)
            val categoryDir = File(packDir, category.mcpePath)
            categoryDir.mkdirs()
            
            val textureFile = File(categoryDir, "$textureName.png")
            
            FileOutputStream(textureFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
            
            Result.success(textureFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun addTexture(packId: String, category: TextureCategory, textureUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val packDir = File(texturePacksDir, packId)
            val categoryDir = File(packDir, category.mcpePath)
            categoryDir.mkdirs()
            
            // Generate a unique filename
            val timestamp = System.currentTimeMillis()
            val fileName = "custom_texture_$timestamp.png"
            val textureFile = File(categoryDir, fileName)
            
            context.contentResolver.openInputStream(textureUri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream)
                
                FileOutputStream(textureFile).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }
                
                bitmap.recycle()
            }
            
            Result.success(textureFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updatePackIcon(packId: String, iconUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val packDir = File(texturePacksDir, packId)
            val iconFile = File(packDir, "pack_icon.png")
            
            context.contentResolver.openInputStream(iconUri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream)
                val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 128, 128, true)
                
                FileOutputStream(iconFile).use { outputStream ->
                    resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }
                
                bitmap.recycle()
                resizedBitmap.recycle()
            }
            
            Result.success(iconFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateManifest(packId: String, name: String, description: String, version: List<Int>, minEngineVersion: List<Int> = listOf(1, 16, 0)): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val packDir = File(texturePacksDir, packId)
            val manifestFile = File(packDir, "manifest.json")
            
            if (manifestFile.exists()) {
                val currentManifest = gson.fromJson(manifestFile.readText(), Manifest::class.java)
                val updatedManifest = currentManifest.copy(
                    header = currentManifest.header.copy(
                        name = name,
                        description = description,
                        version = version,
                        minEngineVersion = minEngineVersion
                    )
                )
                
                manifestFile.writeText(gson.toJson(updatedManifest))
                
                // Update metadata
                val texturePack = getTexturePackById(packId)
                texturePack?.let {
                    saveTexturePackMetadata(it.copy(
                        name = name,
                        description = description,
                        version = version,
                        modifiedAt = System.currentTimeMillis()
                    ))
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun exportTexturePack(packId: String, outputUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val packDir = File(texturePacksDir, packId)
            
            context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                ZipOutputStream(outputStream).use { zipOut ->
                    addFolderToZip(packDir, "", zipOut)
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteTexturePack(packId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val packDir = File(texturePacksDir, packId)
            packDir.deleteRecursively()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun resetToDefault(packId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val packDir = File(texturePacksDir, packId)
            
            // Delete all texture files but keep manifest and metadata
            val manifestFile = File(packDir, "manifest.json")
            val metadataFile = File(packDir, "metadata.json")
            val iconFile = File(packDir, "pack_icon.png")
            
            // Save important files temporarily
            val manifest = if (manifestFile.exists()) manifestFile.readText() else null
            val metadata = if (metadataFile.exists()) metadataFile.readText() else null
            val iconExists = iconFile.exists()
            val iconData = if (iconExists) iconFile.readBytes() else null
            
            // Clear all texture directories
            TextureCategory.values().forEach { category ->
                val categoryDir = File(packDir, category.mcpePath)
                if (categoryDir.exists()) {
                    categoryDir.deleteRecursively()
                }
                categoryDir.mkdirs()
            }
            
            // Restore important files
            manifest?.let { manifestFile.writeText(it) }
            metadata?.let { metadataFile.writeText(it) }
            iconData?.let { iconFile.writeBytes(it) }
            
            // Update modification time
            val texturePack = getTexturePackById(packId)
            texturePack?.let {
                saveTexturePackMetadata(it.copy(modifiedAt = System.currentTimeMillis()))
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun saveProject(packId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val packDir = File(texturePacksDir, packId)
            
            // Create backup directory
            val backupDir = File(packDir, "backup")
            backupDir.mkdirs()
            
            // Create timestamp for backup
            val timestamp = System.currentTimeMillis()
            val backupFile = File(backupDir, "backup_$timestamp.zip")
            
            // Create backup zip
            FileOutputStream(backupFile).use { outputStream ->
                ZipOutputStream(outputStream).use { zipOut ->
                    TextureCategory.values().forEach { category ->
                        val categoryDir = File(packDir, category.mcpePath)
                        if (categoryDir.exists()) {
                            addFolderToZip(categoryDir, category.mcpePath, zipOut)
                        }
                    }
                }
            }
            
            // Update modification time
            val texturePack = getTexturePackById(packId)
            texturePack?.let {
                saveTexturePackMetadata(it.copy(modifiedAt = System.currentTimeMillis()))
            }
            
            // Clean up old backups (keep only last 5)
            val backupFiles = backupDir.listFiles()?.sortedByDescending { it.lastModified() }
            if (backupFiles != null && backupFiles.size > 5) {
                backupFiles.drop(5).forEach { it.delete() }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun addFolderToZip(folder: File, parentPath: String, zipOut: ZipOutputStream) {
        folder.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                addFolderToZip(file, "$parentPath${file.name}/", zipOut)
            } else {
                val zipEntry = ZipEntry("$parentPath${file.name}")
                zipOut.putNextEntry(zipEntry)
                file.inputStream().use { it.copyTo(zipOut) }
                zipOut.closeEntry()
            }
        }
    }
    
    private fun saveTexturePackMetadata(texturePack: TexturePack) {
        val packDir = File(texturePacksDir, texturePack.id)
        val metadataFile = File(packDir, "metadata.json")
        metadataFile.writeText(gson.toJson(texturePack))
    }
    
    private fun getTexturePackById(packId: String): TexturePack? {
        val metadataFile = File(File(texturePacksDir, packId), "metadata.json")
        return if (metadataFile.exists()) {
            gson.fromJson(metadataFile.readText(), TexturePack::class.java)
        } else null
    }
}
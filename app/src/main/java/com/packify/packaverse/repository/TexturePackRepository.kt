package com.packify.packaverse.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.packify.packaverse.data.*
import com.packify.packaverse.viewmodel.TexturePackViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import android.annotation.TargetApi

class TexturePackRepository(private val context: Context) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    
    // Use external storage for projects
    private val externalProjectsDir: File? = if (hasStoragePermission()) {
        File(Environment.getExternalStorageDirectory(), "packify/projects").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    } else null
    
    // Fallback to internal storage if no external permission
    private val internalProjectsDir = File(context.filesDir, "texture_packs")
    
    private fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getApi30StoragePermission()
        } else {
            context.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    @TargetApi(30)
    private fun getApi30StoragePermission(): Boolean {
        return try {
            Environment.isExternalStorageManager()
        } catch (e: Exception) {
            false
        }
    }
    
    private fun getProjectsDir(): File {
        return externalProjectsDir ?: internalProjectsDir
    }
    
    init {
        // Ensure internal directory exists as fallback
        if (!internalProjectsDir.exists()) {
            internalProjectsDir.mkdirs()
        }
    }
    
    suspend fun createTexturePack(name: String, description: String): Result<TexturePack> = withContext(Dispatchers.IO) {
        try {
            val texturePack = TexturePack(
                name = name,
                description = description
            )
            
            val projectsDir = getProjectsDir()
            val packDir = File(projectsDir, texturePack.id)
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
            val projectsDir = getProjectsDir()
            projectsDir.listFiles()?.mapNotNull { dir ->
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
            val projectsDir = getProjectsDir()
            val packDir = File(projectsDir, packId)
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
            val projectsDir = getProjectsDir()
            val packDir = File(projectsDir, packId)
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
    
    private fun getProjectsUri(): Uri? {
        // This assumes you have a way to get the ViewModel or pass the URI in
        val sharedPreferences = context.getSharedPreferences("packify_settings", Context.MODE_PRIVATE)
        val uriString = sharedPreferences.getString("projects_saf_uri", null)
        return uriString?.let { Uri.parse(it) }
    }

    private fun getOrCreatePackDir(packName: String): DocumentFile? {
        val projectsUri = getProjectsUri() ?: return null
        val projectsDir = DocumentFile.fromTreeUri(context, projectsUri) ?: return null
        return projectsDir.findFile(packName) ?: projectsDir.createDirectory(packName)
    }

    private fun getOrCreateCategoryDir(packName: String, category: TextureCategory): DocumentFile? {
        val packDir = getOrCreatePackDir(packName) ?: return null
        val texturesDir = packDir.findFile("textures") ?: packDir.createDirectory("textures")
        return texturesDir?.findFile(category.assetPath) ?: texturesDir?.createDirectory(category.assetPath)
    }

    suspend fun saveEditedTexture(packId: String, category: TextureCategory, textureName: String, bitmap: Bitmap): Result<String> = withContext(Dispatchers.IO) {
        try {
            val projectsDir = getProjectsDir()
            val packDir = File(projectsDir, packId)
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
            val projectsDir = getProjectsDir()
            val packDir = File(projectsDir, packId)
            val categoryDir = File(packDir, category.mcpePath)
            categoryDir.mkdirs()

            // Use the original file name
            val fileName = when {
                textureUri.scheme == "asset" -> {
                    val assetPath = textureUri.toString().removePrefix("asset://")
                    assetPath.substringAfterLast('/')
                }
                textureUri.scheme == "content" || textureUri.scheme == "file" -> {
                    // Try to get file name from uri
                    var name: String? = null
                    context.contentResolver.query(textureUri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndexOpenable(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0 && cursor.moveToFirst()) {
                            name = cursor.getString(nameIndex)
                        }
                    }
                    name ?: File(textureUri.path ?: "").name
                }
                else -> File(textureUri.path ?: "").name
            }
            val textureFile = File(categoryDir, fileName)

            if (textureUri.scheme == "asset") {
                // Copy from assets to textures folder
                val assetPath = textureUri.toString().removePrefix("asset://")
                context.assets.open(assetPath).use { inputStream ->
                    FileOutputStream(textureFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            } else if (textureUri.scheme == "content" || textureUri.scheme == "file") {
                // Copy the file directly to preserve resolution
                context.contentResolver.openInputStream(textureUri)?.use { inputStream ->
                    FileOutputStream(textureFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            } else {
                // Fallback: decode and re-encode as Bitmap (should rarely happen)
                context.contentResolver.openInputStream(textureUri)?.use { inputStream ->
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    FileOutputStream(textureFile).use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    }
                    bitmap.recycle()
                }
            }

            Result.success(textureFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updatePackIcon(packId: String, iconUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val packDir = File(getProjectsDir(), packId)
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
            val packDir = File(getProjectsDir(), packId)
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
            val packDir = File(getProjectsDir(), packId)
            
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
            val packDir = File(getProjectsDir(), packId)
            packDir.deleteRecursively()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun resetToDefault(packId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val packDir = File(getProjectsDir(), packId)
            
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
            val packDir = File(getProjectsDir(), packId)
            
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
        val packDir = File(getProjectsDir(), texturePack.id)
        val metadataFile = File(packDir, "metadata.json")
        metadataFile.writeText(gson.toJson(texturePack))
    }
    
    private fun getTexturePackById(packId: String): TexturePack? {
        val metadataFile = File(File(getProjectsDir(), packId), "metadata.json")
        return if (metadataFile.exists()) {
            gson.fromJson(metadataFile.readText(), TexturePack::class.java)
        } else null
    }
}
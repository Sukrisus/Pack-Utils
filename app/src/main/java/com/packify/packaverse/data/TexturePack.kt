package com.packify.packaverse.data

import com.google.gson.annotations.SerializedName
import java.util.UUID

data class TexturePack(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val version: List<Int> = listOf(1, 0, 0),
    val iconPath: String? = null,
    val folderPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis()
)

data class Manifest(
    @SerializedName("format_version")
    val formatVersion: Int = 2,
    val header: Header,
    val modules: List<Module>
)

data class Header(
    val name: String,
    val description: String,
    val uuid: String = UUID.randomUUID().toString(),
    val version: List<Int> = listOf(1, 0, 0),
    @SerializedName("min_engine_version")
    val minEngineVersion: List<Int> = listOf(1, 16, 0)
)

data class Module(
    val type: String = "resources",
    val uuid: String = UUID.randomUUID().toString(),
    val version: List<Int> = listOf(1, 0, 0)
)

data class TextureItem(
    val name: String,
    val originalPath: String,
    val mcpePath: String,
    val category: TextureCategory,
    val isCustom: Boolean = false
)

enum class TextureCategory(val displayName: String, val mcpePath: String, val assetPath: String) {
    ENTITIES("Entities", "textures/entity/", "entity"),
    ENVIRONMENT("Environment", "textures/environment/", "environment"),
    ITEMS("Items", "textures/items/", "items"),
    MAP("Map", "textures/map/", "map"),
    UI("UI", "textures/ui/", "ui"),
    GUI("GUI", "textures/gui/", "gui"),
    COLORMAP("Colormap", "textures/colormap/", "colormap")
}
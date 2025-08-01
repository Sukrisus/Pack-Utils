package com.packify.packaverse.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.packify.packaverse.data.TextureItem
import com.packify.packaverse.viewmodel.TexturePackViewModel
import kotlinx.coroutines.launch
import java.io.InputStream
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.IntSize
import androidx.compose.material.icons.filled.AutoFixOff
import androidx.compose.ui.res.painterResource
import com.packify.packaverse.R
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color
import android.graphics.Bitmap as AndroidBitmap

// --- Layer data class ---
data class Layer(
    val id: Int,
    var name: String,
    var bitmap: AndroidBitmap,
    var visible: Boolean = true,
    var opacity: Float = 1f
)

// --- LayerPanel composable ---
@Composable
fun LayerPanel(
    layers: List<Layer>,
    selectedLayerId: Int,
    onSelectLayer: (Int) -> Unit,
    onAddLayer: () -> Unit,
    onRemoveLayer: (Int) -> Unit,
    onMoveLayer: (Int, Int) -> Unit,
    onToggleVisibility: (Int) -> Unit,
    onChangeOpacity: (Int, Float) -> Unit
) {
    Column(
        modifier = Modifier
            .width(180.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(8.dp)
    ) {
        Text("Layers", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn {
            itemsIndexed(layers.reversed()) { index, layer ->
                val realIndex = layers.size - 1 - index
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onSelectLayer(layer.id) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (layer.id == selectedLayerId) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = layer.visible,
                            onCheckedChange = { onToggleVisibility(layer.id) }
                        )
                        Text(layer.name, modifier = Modifier.weight(1f))
                        IconButton(onClick = { onRemoveLayer(layer.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Layer")
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Opacity", fontSize = 12.sp)
                        Slider(
                            value = layer.opacity,
                            onValueChange = { onChangeOpacity(layer.id, it) },
                            valueRange = 0.1f..1f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                                thumbColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        IconButton(onClick = {
                            if (realIndex > 0) onMoveLayer(realIndex, realIndex - 1)
                        }) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up")
                        }
                        IconButton(onClick = {
                            if (realIndex < layers.size - 1) onMoveLayer(realIndex, realIndex + 1)
                        }) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down")
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onAddLayer, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = "Add Layer")
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add Layer")
        }
    }
}

enum class EditorTool {
    BRUSH, ERASER, COLOR_PICKER, FILL, SPRAY_PAINT, PENCIL
}

enum class BrushShape {
    ROUND, SQUARE, DIAMOND, STAR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextureEditorScreen(
    texture: TextureItem,
    viewModel: TexturePackViewModel,
    packId: String,
    onNavigateBack: () -> Unit
) {
    var currentTool by remember { mutableStateOf(EditorTool.BRUSH) }
    var selectedColor by remember { mutableStateOf(Color.Red) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var hasUnsavedChanges by remember { mutableStateOf(false) }
    var canvasBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var brushSize by remember { mutableStateOf(1f) } // Default to 1 pixel

    // Undo/Redo stacks
    val undoStack = remember { mutableStateListOf<android.graphics.Bitmap>() }
    val redoStack = remember { mutableStateListOf<android.graphics.Bitmap>() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Load the bitmap from the texture path on first composition
    LaunchedEffect(texture.originalPath) {
        if (canvasBitmap == null) {
            val bmp: Bitmap? = try {
                if (texture.originalPath.startsWith("asset://")) {
                    val assetPath = texture.originalPath.removePrefix("asset://")
                    val inputStream: InputStream = context.assets.open(assetPath)
                    BitmapFactory.decodeStream(inputStream)
                } else {
                    BitmapFactory.decodeFile(texture.originalPath)
                }
            } catch (e: Exception) {
                null
            }
            canvasBitmap = bmp?.copy(Bitmap.Config.ARGB_8888, true)
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.replaceTexture(packId, texture.mcpePath, uri)
            hasUnsavedChanges = true
            showImportDialog = false
        }
    }

    fun pushUndo(bitmap: android.graphics.Bitmap?) {
        bitmap?.let {
            undoStack.add(it.copy(it.config, true))
            if (undoStack.size > 20) undoStack.removeAt(0)
        }
    }
    fun clearRedo() {
        redoStack.clear()
    }
    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack.add(canvasBitmap!!.copy(canvasBitmap!!.config, true))
            val popped = undoStack.removeAt(undoStack.lastIndex)
            canvasBitmap = popped.copy(popped.config, true)
        }
    }
    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack.add(canvasBitmap!!.copy(canvasBitmap!!.config, true))
            val popped = redoStack.removeAt(redoStack.lastIndex)
            canvasBitmap = popped.copy(popped.config, true)
        }
    }

    // Palette persistence: sync with ViewModel
    val palette by viewModel.customPalette.collectAsState()
    val customColorsState = remember { mutableStateListOf<Color>() }
    LaunchedEffect(palette) {
        customColorsState.clear()
        customColorsState.addAll(palette.map { Color(it) })
    }
    fun savePalette() {
        viewModel.saveCustomPalette(customColorsState.map { it.value.toInt() })
    }

    // --- Layer system state ---
    val initialBitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
    val layers = remember { mutableStateListOf(
        Layer(id = 0, name = "Layer 1", bitmap = initialBitmap.copy(initialBitmap.config, true))
    ) }
    var selectedLayerId by remember { mutableStateOf(0) }
    var showLayerPanel by remember { mutableStateOf(true) }
    var layerIdCounter by remember { mutableStateOf(1) }

    // --- Layer actions ---
    fun addLayer() {
        layers.add(
            Layer(
                id = layerIdCounter,
                name = "Layer ${layerIdCounter + 1}",
                bitmap = Bitmap.createBitmap(initialBitmap.width, initialBitmap.height, Bitmap.Config.ARGB_8888)
            )
        )
        selectedLayerId = layerIdCounter
        layerIdCounter++
    }
    fun removeLayer(id: Int) {
        if (layers.size > 1) {
            val idx = layers.indexOfFirst { it.id == id }
            layers.removeAt(idx)
            selectedLayerId = layers.lastOrNull()?.id ?: 0
        }
    }
    fun moveLayer(from: Int, to: Int) {
        if (from in layers.indices && to in layers.indices) {
            val l = layers.removeAt(from)
            layers.add(to, l)
        }
    }
    fun toggleVisibility(id: Int) {
        layers.find { it.id == id }?.let { it.visible = !it.visible }
    }
    fun changeOpacity(id: Int, value: Float) {
        layers.find { it.id == id }?.let { it.opacity = value }
    }
    fun mergeLayer(id: Int) {
        val idx = layers.indexOfFirst { it.id == id }
        if (idx > 0) {
            val lower = layers[idx - 1]
            val upper = layers[idx]
            val canvas = Canvas(lower.bitmap)
            val paint = Paint().apply { alpha = (upper.opacity * 255).toInt() }
            canvas.drawBitmap(upper.bitmap, 0f, 0f, paint)
            layers.removeAt(idx)
            selectedLayerId = lower.id
        }
    }
    fun renameLayer(id: Int, newName: String) {
        layers.find { it.id == id }?.name = newName
    }
    fun duplicateLayer(id: Int) {
        val orig = layers.find { it.id == id } ?: return
        val newId = layerIdCounter
        layers.add(
            Layer(
                id = newId,
                name = orig.name + " Copy",
                bitmap = orig.bitmap.copy(orig.bitmap.config, true),
                visible = orig.visible,
                opacity = orig.opacity
            )
        )
        selectedLayerId = newId
        layerIdCounter++
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Edit ${texture.name}",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (hasUnsavedChanges) {
                            showExitDialog = true
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showImportDialog = true }) {
                        Icon(Icons.Default.Upload, contentDescription = "Import from Device")
                    }
                    IconButton(onClick = { undo() }, enabled = undoStack.isNotEmpty()) {
                        Icon(Icons.Default.Undo, contentDescription = "Undo")
                    }
                    IconButton(onClick = { redo() }, enabled = redoStack.isNotEmpty()) {
                        Icon(Icons.Default.Redo, contentDescription = "Redo")
                    }
                    IconButton(onClick = {
                        canvasBitmap?.let { bitmap ->
                            viewModel.saveEditedTexture(packId, texture.category, texture.name, bitmap)
                            hasUnsavedChanges = false
                        }
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                    IconButton(onClick = {
                        if (hasUnsavedChanges) {
                            showExitDialog = true
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Exit")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .systemBarsPadding(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { currentTool = EditorTool.BRUSH }) {
                    Icon(Icons.Default.Brush, contentDescription = "Brush")
                }
                IconButton(onClick = { currentTool = EditorTool.ERASER }) {
                    Icon(painterResource(id = R.drawable.ic_eraser), contentDescription = "Eraser")
                }
                IconButton(onClick = { showColorPicker = true }) {
                    Icon(Icons.Default.ColorLens, contentDescription = "Color Picker")
                }
                IconButton(onClick = { showImportDialog = true }) {
                    Icon(Icons.Default.Upload, contentDescription = "Import from Device")
                }
                IconButton(onClick = {
                    canvasBitmap?.let { bitmap ->
                        viewModel.saveEditedTexture(packId, texture.category, texture.name, bitmap)
                        hasUnsavedChanges = false
                    }
                }) {
                    Icon(Icons.Default.Save, contentDescription = "Save")
                }
                IconButton(onClick = {
                    if (hasUnsavedChanges) {
                        showExitDialog = true
                    } else {
                        onNavigateBack()
                    }
                }) {
                    Icon(Icons.Default.Close, contentDescription = "Exit")
                }
            }
        }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .systemBarsPadding()
        ) {
            if (showLayerPanel) {
                LayerPanel(
                    layers = layers,
                    selectedLayerId = selectedLayerId,
                    onSelectLayer = { selectedLayerId = it },
                    onAddLayer = { addLayer() },
                    onRemoveLayer = { removeLayer(it) },
                    onMoveLayer = { from, to -> moveLayer(from, to) },
                    onToggleVisibility = { toggleVisibility(it) },
                    onChangeOpacity = { id, v -> changeOpacity(id, v) }
                )
            }
            Box(Modifier.weight(1f)) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (canvasBitmap == null) {
                        Text(
                            text = "Failed to load texture image.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            EnhancedTextureCanvas(
                                texture = texture,
                                currentTool = currentTool,
                                brushSize = brushSize, // Use the state variable
                                brushShape = BrushShape.ROUND, // Default shape
                                selectedColor = selectedColor,
                                opacity = 1f,
                                onDrawingChanged = {
                                    hasUnsavedChanges = true
                                },
                                onBitmapUpdated = { bitmap ->
                                    // Update the selected layer's bitmap
                                    layers.find { it.id == selectedLayerId }?.bitmap = bitmap
                                },
                                externalBitmap = layers.find { it.id == selectedLayerId }?.bitmap,
                                pushUndo = { bmp -> pushUndo(bmp) },
                                clearRedo = { clearRedo() }
                            )
                        }
                        // Add color palette below the canvas
                        PixelPalette(
                            selectedColor = selectedColor.toArgb(),
                            palette = palette.map { it.toInt() },
                            onColorSelected = { colorInt ->
                                selectedColor = Color(colorInt)
                            },
                            onAddColor = { colorInt ->
                                if (!palette.contains(colorInt)) {
                                    val newPalette = palette + colorInt
                                    viewModel.saveCustomPalette(newPalette)
                                }
                                selectedColor = Color(colorInt)
                            },
                            onDeleteColor = { colorsToDelete ->
                                val newPalette = palette.filter { it !in colorsToDelete }
                                viewModel.saveCustomPalette(newPalette)
                                selectedColor = Color.Red // Indicate deletion
                            }
                        )
                    }
                }
                // Toggle LayerPanel button
                IconButton(
                    onClick = { showLayerPanel = !showLayerPanel },
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Icon(
                        imageVector = if (showLayerPanel) Icons.Default.ChevronLeft else Icons.Default.ChevronRight,
                        contentDescription = if (showLayerPanel) "Hide Layers" else "Show Layers"
                    )
                }
            }
        }
        // Color picker dialog
        if (showColorPicker) {
            AdvancedColorPickerDialog(
                selectedColor = selectedColor,
                onColorSelected = { color ->
                    if (!customColorsState.contains(color)) {
                        customColorsState.add(color)
                        savePalette()
                    }
                    selectedColor = color
                    showColorPicker = false
                },
                onDismiss = { showColorPicker = false }
            )
        }
        // Import dialog
        if (showImportDialog) {
            AlertDialog(
                onDismissRequest = { showImportDialog = false },
                title = { Text("Import Texture") },
                text = { Text("Import a texture from your device gallery or files. This will replace the current texture.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            imagePickerLauncher.launch("image/*")
                        }
                    ) {
                        Text("Import")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showImportDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
        // Exit dialog
        if (showExitDialog) {
            AlertDialog(
                onDismissRequest = { showExitDialog = false },
                title = { Text("Unsaved Changes") },
                text = { Text("You have unsaved changes. Do you want to save before exiting?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            canvasBitmap?.let { bitmap ->
                                viewModel.saveEditedTexture(packId, texture.category, texture.name, bitmap)
                            }
                            hasUnsavedChanges = false
                            showExitDialog = false
                            onNavigateBack()
                        }
                    ) {
                        Text("Save & Exit")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showExitDialog = false
                            onNavigateBack()
                        }
                    ) {
                        Text("Exit Without Saving")
                    }
                }
            )
        }
    }
}

@Composable
fun AdvancedToolbar(
    currentTool: EditorTool,
    onToolSelected: (EditorTool) -> Unit,
    brushSize: Float,
    onBrushSizeChanged: (Float) -> Unit,
    brushShape: BrushShape,
    onBrushShapeChanged: (BrushShape) -> Unit,
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    opacity: Float,
    onOpacityChanged: (Float) -> Unit,
    onColorPickerClicked: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Tools Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(EditorTool.values()) { tool ->
                    AdvancedToolButton(
                        tool = tool,
                        isSelected = currentTool == tool,
                        onClick = { onToolSelected(tool) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Brush Size and Shape
            if (currentTool == EditorTool.BRUSH || currentTool == EditorTool.ERASER || currentTool == EditorTool.SPRAY_PAINT) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Size: ${brushSize.toInt()}",
                            fontSize = 14.sp
                        )
                        Slider(
                            value = brushSize,
                            onValueChange = onBrushSizeChanged,
                            valueRange = 1f..50f,
                            modifier = Modifier.fillMaxWidth(),
                            colors = SliderDefaults.colors(
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                                thumbColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    if (currentTool == EditorTool.BRUSH) {
                        Column {
                            Text(
                                text = "Shape",
                                fontSize = 14.sp
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(BrushShape.values()) { shape ->
                                    BrushShapeButton(
                                        shape = shape,
                                        isSelected = brushShape == shape,
                                        onClick = { onBrushShapeChanged(shape) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Color and Opacity
            if (currentTool == EditorTool.BRUSH || currentTool == EditorTool.FILL || currentTool == EditorTool.SPRAY_PAINT) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Opacity: ${(opacity * 100).toInt()}%",
                            fontSize = 14.sp
                        )
                        Slider(
                            value = opacity,
                            onValueChange = onOpacityChanged,
                            valueRange = 0.1f..1f,
                            modifier = Modifier.fillMaxWidth(),
                            colors = SliderDefaults.colors(
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                                thumbColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "Color",
                            fontSize = 14.sp
                        )
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(selectedColor, CircleShape)
                                .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .clickable { onColorPickerClicked() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdvancedToolButton(
    tool: EditorTool,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val (icon, description) = when (tool) {
        EditorTool.BRUSH -> Icons.Default.Brush to "Paint Brush"
        EditorTool.ERASER -> painterResource(id = R.drawable.ic_eraser) to "Eraser"
        EditorTool.COLOR_PICKER -> Icons.Default.ColorLens to "Color Picker"
        EditorTool.FILL -> Icons.Default.FormatPaint to "Fill Tool"
        EditorTool.SPRAY_PAINT -> Icons.Default.Grain to "Spray Paint"
        EditorTool.PENCIL -> Icons.Default.Edit to "Pencil"
    }

    Card(
        modifier = Modifier
            .size(56.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFFFB6C1) else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = CircleShape
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (tool) {
                EditorTool.ERASER -> Icon(
                    painter = painterResource(id = R.drawable.ic_eraser),
                    contentDescription = description,
                    tint = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                else -> Icon(
                    imageVector = icon as ImageVector,
                    contentDescription = description,
                    tint = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = description.take(4),
                fontSize = 10.sp,
                color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun BrushShapeButton(
    shape: BrushShape,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val icon = when (shape) {
        BrushShape.ROUND -> Icons.Default.Circle
        BrushShape.SQUARE -> Icons.Default.Square
        BrushShape.DIAMOND -> Icons.Default.Diamond
        BrushShape.STAR -> Icons.Default.Star
    }

    Card(
        modifier = Modifier
            .size(32.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFFFB6C1) else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = CircleShape
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = shape.name,
                tint = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun EnhancedTextureCanvas(
    texture: TextureItem,
    currentTool: EditorTool,
    brushSize: Float,
    brushShape: BrushShape,
    selectedColor: Color,
    opacity: Float,
    onDrawingChanged: () -> Unit,
    onBitmapUpdated: (android.graphics.Bitmap) -> Unit,
    externalBitmap: android.graphics.Bitmap? = null,
    pushUndo: (android.graphics.Bitmap?) -> Unit,
    clearRedo: () -> Unit
) {
    var canvasBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(externalBitmap) }
    var lastPoint by remember { mutableStateOf<Offset?>(null) }
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        val canvasSizePx = with(density) { minOf(maxWidth, maxHeight).toPx() }
        val bitmap = canvasBitmap
        if (bitmap != null) {
            Canvas(
                modifier = Modifier
                    .size(with(density) { canvasSizePx.toDp() })
                    .pointerInput(currentTool, brushSize, selectedColor, bitmap) {
                        detectTapGestures(
                            onTap = { offset ->
                                // Push undo and clear redo for tap
                                pushUndo(bitmap)
                                clearRedo()
                                val bmpX = (offset.x / canvasSizePx) * bitmap.width
                                val bmpY = (offset.y / canvasSizePx) * bitmap.height
                                if (currentTool == EditorTool.BRUSH || currentTool == EditorTool.PENCIL) {
                                    val canvas = Canvas(bitmap)
                                    val paint = Paint().apply {
                                        isAntiAlias = false
                                        color = selectedColor.copy(alpha = opacity).toArgb()
                                        style = Paint.Style.FILL
                                    }
                                    canvas.drawCircle(bmpX, bmpY, brushSize / 2, paint)
                                    onBitmapUpdated(bitmap.copy(bitmap.config, true))
                                    onDrawingChanged()
                                } else if (currentTool == EditorTool.ERASER) {
                                    val canvas = Canvas(bitmap)
                                    val paint = Paint().apply {
                                        isAntiAlias = false
                                        xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
                                        style = Paint.Style.FILL
                                    }
                                    canvas.drawCircle(bmpX, bmpY, brushSize / 2, paint)
                                    onBitmapUpdated(bitmap.copy(bitmap.config, true))
                                    onDrawingChanged()
                                }
                            }
                        )
                    }
                    .pointerInput(currentTool, brushSize, selectedColor, bitmap) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                // Push undo and clear redo at the start of every stroke
                                pushUndo(bitmap)
                                clearRedo()
                                val bmpX = (offset.x / canvasSizePx) * bitmap.width
                                val bmpY = (offset.y / canvasSizePx) * bitmap.height
                                lastPoint = Offset(bmpX, bmpY)
                                if (currentTool == EditorTool.BRUSH || currentTool == EditorTool.PENCIL) {
                                    val canvas = Canvas(bitmap)
                                    val paint = Paint().apply {
                                        isAntiAlias = false
                                        color = selectedColor.copy(alpha = opacity).toArgb()
                                        style = Paint.Style.FILL
                                    }
                                    canvas.drawCircle(bmpX, bmpY, brushSize / 2, paint)
                                    onBitmapUpdated(bitmap.copy(bitmap.config, true))
                                    onDrawingChanged()
                                } else if (currentTool == EditorTool.ERASER) {
                                    val canvas = Canvas(bitmap)
                                    val paint = Paint().apply {
                                        isAntiAlias = false
                                        xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
                                        style = Paint.Style.FILL
                                    }
                                    canvas.drawCircle(bmpX, bmpY, brushSize / 2, paint)
                                    onBitmapUpdated(bitmap.copy(bitmap.config, true))
                                    onDrawingChanged()
                                }
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val bmpX = (change.position.x / canvasSizePx) * bitmap.width
                                val bmpY = (change.position.y / canvasSizePx) * bitmap.height
                                val prev = lastPoint
                                if (prev != null && (currentTool == EditorTool.BRUSH || currentTool == EditorTool.PENCIL)) {
                                    val canvas = Canvas(bitmap)
                                    val paint = Paint().apply {
                                        isAntiAlias = false
                                        color = selectedColor.copy(alpha = opacity).toArgb()
                                        style = Paint.Style.STROKE
                                        strokeWidth = brushSize
                                        strokeCap = Paint.Cap.ROUND
                                    }
                                    canvas.drawLine(prev.x, prev.y, bmpX, bmpY, paint)
                                    onBitmapUpdated(bitmap.copy(bitmap.config, true))
                                    onDrawingChanged()
                                } else if (prev != null && currentTool == EditorTool.ERASER) {
                                    val canvas = Canvas(bitmap)
                                    val paint = Paint().apply {
                                        isAntiAlias = false
                                        xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
                                        style = Paint.Style.STROKE
                                        strokeWidth = brushSize
                                        strokeCap = Paint.Cap.ROUND
                                    }
                                    canvas.drawLine(prev.x, prev.y, bmpX, bmpY, paint)
                                    onBitmapUpdated(bitmap.copy(bitmap.config, true))
                                    onDrawingChanged()
                                }
                                lastPoint = Offset(bmpX, bmpY)
                            },
                            onDragEnd = {
                                lastPoint = null
                            }
                        )
                    }
            ) {
                drawImage(
                    image = bitmap.asImageBitmap(),
                    dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                    filterQuality = FilterQuality.None // Pixelated rendering
                )
            }
        }
    }
}

@Composable
fun ModernPalette(
    selectedColor: Color,
    customColors: List<Color>,
    onColorSelected: (Color) -> Unit,
    onAddCustomColor: (Color) -> Unit
) {
    var showColorDialog by remember { mutableStateOf(false) }
    val baseColors = listOf(
        Color.Red, Color.Green, Color.Blue, Color.Yellow,
        Color.Cyan, Color.Magenta, Color.Black, Color.White,
        Color.Gray, Color.DarkGray, Color.LightGray,
        Color(0xFFFF8C00), Color(0xFF9370DB), Color(0xFF20B2AA),
        Color(0xFFDC143C), Color(0xFF228B22), Color(0xFF4B0082)
    )
    val colors = baseColors + customColors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterStart),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            items(colors) { color ->
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(color, CircleShape)
                        .border(
                            width = if (selectedColor == color) 3.dp else 1.dp,
                            color = if (selectedColor == color) Color(0xFFFFB6C1) else MaterialTheme.colorScheme.outline,
                            shape = CircleShape
                        )
                        .clickable { onColorSelected(color) }
                )
            }
            item {
                IconButton(
                    onClick = { showColorDialog = true },
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color(0xFFFFB6C1), CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Color", tint = Color.White)
                }
            }
        }
    }
    if (showColorDialog) {
        ModernColorSelectorDialog(
            palette = customColors.map { it.value.toInt() },
            onAddColor = { color ->
                onAddCustomColor(color)
                showColorDialog = false
            },
            onDismiss = { showColorDialog = false }
        )
    }
}

@Composable
fun ModernColorSelectorDialog(
    palette: List<Int>,
    onAddColor: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    var hue by remember { mutableStateOf(0f) }
    var saturation by remember { mutableStateOf(1f) }
    var value by remember { mutableStateOf(1f) }
    val color = Color.hsv(hue, saturation, value)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Color") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Large color preview
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(color, CircleShape)
                        .border(2.dp, Color.Black, CircleShape)
                )
                Spacer(modifier = Modifier.height(16.dp))
                // Hue slider
                Text("Hue: ${hue.toInt()}")
                Slider(
                    value = hue,
                    onValueChange = { hue = it },
                    valueRange = 0f..360f,
                    colors = SliderDefaults.colors(
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        thumbColor = MaterialTheme.colorScheme.primary
                    )
                )
                // Saturation slider
                Text("Saturation: ${(saturation * 100).toInt()}%")
                Slider(
                    value = saturation,
                    onValueChange = { saturation = it },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        thumbColor = MaterialTheme.colorScheme.primary
                    )
                )
                // Value/Brightness slider
                Text("Brightness: ${(value * 100).toInt()}%")
                Slider(
                    value = value,
                    onValueChange = { value = it },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        thumbColor = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Palette:")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(palette) { c ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(c), CircleShape)
                                .border(2.dp, Color.Black, CircleShape)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onAddColor(Color.hsv(hue, saturation, value)) }) {
                Icon(Icons.Default.Add, contentDescription = "Add Color")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AdvancedColorPickerDialog(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    var red by remember { mutableStateOf(selectedColor.red) }
    var green by remember { mutableStateOf(selectedColor.green) }
    var blue by remember { mutableStateOf(selectedColor.blue) }
    var alpha by remember { mutableStateOf(selectedColor.alpha) }

    val currentColor = Color(
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt(),
        (alpha * 255).toInt()
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Advanced Color Picker") },
        text = {
            Column {
                // Color preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(currentColor, CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // RGB Sliders
                Text("Red: ${(red * 255).toInt()}")
                Slider(
                    value = red,
                    onValueChange = { red = it },
                    colors = SliderDefaults.colors(
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        thumbColor = MaterialTheme.colorScheme.primary
                    )
                )

                Text("Green: ${(green * 255).toInt()}")
                Slider(
                    value = green,
                    onValueChange = { green = it },
                    colors = SliderDefaults.colors(
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        thumbColor = MaterialTheme.colorScheme.primary
                    )
                )

                Text("Blue: ${(blue * 255).toInt()}")
                Slider(
                    value = blue,
                    onValueChange = { blue = it },
                    colors = SliderDefaults.colors(
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        thumbColor = MaterialTheme.colorScheme.primary
                    )
                )

                Text("Alpha: ${(alpha * 255).toInt()}")
                Slider(
                    value = alpha,
                    onValueChange = { alpha = it },
                    colors = SliderDefaults.colors(
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        thumbColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onColorSelected(currentColor)
                }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ColorButton(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(color, CircleShape)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) Color(0xFFFFB6C1) else MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
            .clickable { onClick() }
    )
}

@Composable
fun HSVColorWheel(
    modifier: Modifier = Modifier,
    color: Color,
    onColorChanged: (Color) -> Unit
) {
    // Simple HSV color wheel using Compose Canvas
    // For brevity, use a vertical hue slider and a square SV box
    var hue by remember { mutableStateOf(0f) }
    var saturation by remember { mutableStateOf(1f) }
    var value by remember { mutableStateOf(1f) }
    val currentColor = Color.hsv(hue, saturation, value)
    LaunchedEffect(color) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), hsv)
        hue = hsv[0]
        saturation = hsv[1]
        value = hsv[2]
    }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        // Hue slider
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .height(150.dp)
                    .width(24.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            val y = change.position.y.coerceIn(0f, 150f)
                            hue = (y / 150f) * 360f
                            onColorChanged(Color.hsv(hue, saturation, value))
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    for (i in 0..149) {
                        drawRect(
                            color = Color.hsv(i * 360f / 150f, 1f, 1f),
                            topLeft = androidx.compose.ui.geometry.Offset(0f, i.toFloat()),
                            size = androidx.compose.ui.geometry.Size(24f, 1f)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Hue")
        }
        Spacer(modifier = Modifier.width(16.dp))
        // SV box
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .pointerInput(hue) {
                        detectDragGestures { change, _ ->
                            val x = change.position.x.coerceIn(0f, 150f)
                            val y = change.position.y.coerceIn(0f, 150f)
                            saturation = x / 150f
                            value = 1f - (y / 150f)
                            onColorChanged(Color.hsv(hue, saturation, value))
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    for (i in 0..149) {
                        for (j in 0..149) {
                            drawRect(
                                color = Color.hsv(hue, i / 150f, 1f - (j / 150f)),
                                topLeft = androidx.compose.ui.geometry.Offset(i.toFloat(), j.toFloat()),
                                size = androidx.compose.ui.geometry.Size(1f, 1f)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Saturation/Value")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PixelPalette(
    selectedColor: Int,
    palette: List<Int>,
    onColorSelected: (Int) -> Unit,
    onAddColor: (Int) -> Unit,
    onDeleteColor: (List<Int>) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    val baseColors = listOf(
        Color.Red.toArgb(), Color.Green.toArgb(), Color.Blue.toArgb(), Color.Yellow.toArgb(),
        Color.Cyan.toArgb(), Color.Magenta.toArgb(), Color.Black.toArgb(), Color.White.toArgb(),
        Color.Gray.toArgb(), Color.DarkGray.toArgb(), Color.LightGray.toArgb(),
        Color(0xFFFF8C00).toArgb(), Color(0xFF9370DB).toArgb(), Color(0xFF20B2AA).toArgb(),
        Color(0xFFDC143C).toArgb(), Color(0xFF228B22).toArgb(), Color(0xFF4B0082).toArgb()
    )
    var selectedForDelete by remember { mutableStateOf<Set<Int>>(emptySet()) }
    val inDeleteMode = selectedForDelete.isNotEmpty()
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (inDeleteMode) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = {
                        onDeleteColor(selectedForDelete.toList())
                        selectedForDelete = emptySet()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White
                    ),
                    shape = CircleShape,
                    enabled = selectedForDelete.isNotEmpty()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Color")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { selectedForDelete = emptySet() },
                    colors = ButtonDefaults.buttonColors(),
                    shape = CircleShape
                ) {
                    Text("Cancel")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                items(palette) { colorInt ->
                    val color = Color(colorInt)
                    val isBase = baseColors.contains(colorInt)
                    val isSelected = selectedForDelete.contains(colorInt)
                    Box(
                        modifier = Modifier
                            .padding(6.dp)
                            .size(26.dp)
                            .background(color, CircleShape)
                            .border(
                                width = when {
                                    isSelected -> 3.dp
                                    selectedColor == colorInt -> 3.dp
                                    else -> 1.dp
                                },
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.error
                                    selectedColor == colorInt -> Color(0xFFFFB6C1)
                                    else -> MaterialTheme.colorScheme.outline
                                },
                                shape = CircleShape
                            )
                            .combinedClickable(
                                onClick = {
                                    if (inDeleteMode && !isBase) {
                                        selectedForDelete = selectedForDelete.toMutableSet().apply {
                                            if (isSelected) remove(colorInt) else add(colorInt)
                                        }
                                    } else if (!inDeleteMode) {
                                        onColorSelected(colorInt)
                                    }
                                },
                                onLongClick = {
                                    if (!isBase) {
                                        selectedForDelete = selectedForDelete.toMutableSet().apply { add(colorInt) }
                                    }
                                }
                            )
                    )
                }
                item {
                    IconButton(
                        onClick = { showDialog = true },
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(0xFFFFB6C1), CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Color", tint = Color.White)
                    }
                }
            }
        }
    }
    if (showDialog) {
        PixelPaletteColorDialog(
            existingColors = palette,
            onAdd = { colorInt ->
                onAddColor(colorInt)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
fun PixelPaletteColorDialog(
    existingColors: List<Int>,
    onAdd: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var hue by remember { mutableStateOf(0f) }
    var saturation by remember { mutableStateOf(1f) }
    var value by remember { mutableStateOf(1f) }
    val color = Color.hsv(hue, saturation, value)
    val colorInt = color.copy(alpha = 1f).toArgb()
    val isDuplicate = existingColors.contains(colorInt)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Color") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(color, CircleShape)
                        .border(2.dp, Color.Black, CircleShape)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Hue: ${hue.toInt()}")
                Slider(
                    value = hue,
                    onValueChange = { hue = it },
                    valueRange = 0f..360f,
                    colors = SliderDefaults.colors(
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        thumbColor = MaterialTheme.colorScheme.primary
                    )
                )
                Text("Saturation: ${(saturation * 100).toInt()}%")
                Slider(
                    value = saturation,
                    onValueChange = { saturation = it },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        thumbColor = MaterialTheme.colorScheme.primary
                    )
                )
                Text("Brightness: ${(value * 100).toInt()}%")
                Slider(
                    value = value,
                    onValueChange = { value = it },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        thumbColor = MaterialTheme.colorScheme.primary
                    )
                )
                if (isDuplicate) {
                    Text("Color already in palette", color = Color.Red, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (!isDuplicate) onAdd(colorInt) },
                enabled = !isDuplicate
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Color")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// Helper extension to convert Compose Path to a list of Android Points
private fun androidx.compose.ui.graphics.Path.asAndroidPathPoints(): List<Offset> {
    val points = mutableListOf<Offset>()
    this.asAndroidPath().apply {
        val pathMeasure = android.graphics.PathMeasure(this, false)
        val coords = FloatArray(2)
        var distance = 0f
        while (distance < pathMeasure.length) {
            pathMeasure.getPosTan(distance, coords, null)
            points.add(Offset(coords[0], coords[1]))
            distance += 1f
        }
    }
    return points
}
package com.packify.packaverse.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.packify.packaverse.data.TextureCategory
import com.packify.packaverse.data.TextureItem
import com.packify.packaverse.viewmodel.TexturePackViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

enum class EditorTool {
    BRUSH, ERASER, COLOR_PICKER, FILL, IMPORT, SAVE, EXIT
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
    var brushSize by remember { mutableStateOf(10f) }
    var brushShape by remember { mutableStateOf(BrushShape.ROUND) }
    var selectedColor by remember { mutableStateOf(Color.Red) }
    var customColors by remember { mutableStateOf(listOf<Color>()) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var hasUnsavedChanges by remember { mutableStateOf(false) }
    var opacity by remember { mutableStateOf(1f) }
    var canvasSize by remember { mutableStateOf(300.dp) }
    var strokePaths by remember { mutableStateOf<List<StrokePath>>(emptyList()) }
    var currentPath by remember { mutableStateOf<StrokePath?>(null) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { 
            viewModel.replaceTexture(packId, texture.mcpePath, uri)
            hasUnsavedChanges = true
            showImportDialog = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Edit ${texture.name}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Normal
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
                        Icon(Icons.Default.FileUpload, contentDescription = "Import")
                    }
                    IconButton(onClick = { showSaveDialog = true }) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
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
        ) {
            // Tool Bar
            ToolBar(
                currentTool = currentTool,
                onToolSelected = { tool ->
                    when (tool) {
                        EditorTool.SAVE -> showSaveDialog = true
                        EditorTool.EXIT -> {
                            if (hasUnsavedChanges) {
                                showExitDialog = true
                            } else {
                                onNavigateBack()
                            }
                        }
                        EditorTool.IMPORT -> showImportDialog = true
                        EditorTool.COLOR_PICKER -> showColorPicker = true
                        else -> currentTool = tool
                    }
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // Canvas Area
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    DrawingCanvas(
                        modifier = Modifier.size(canvasSize),
                        currentTool = currentTool,
                        brushSize = brushSize,
                        brushShape = brushShape,
                        selectedColor = selectedColor,
                        opacity = opacity,
                        strokePaths = strokePaths,
                        currentPath = currentPath,
                        onStrokePathChanged = { path ->
                            currentPath = path
                        },
                        onStrokeComplete = { path ->
                            strokePaths = strokePaths + path
                            currentPath = null
                            hasUnsavedChanges = true
                        }
                    )
                }
            }
            
            // Properties Panel
            PropertiesPanel(
                brushSize = brushSize,
                onBrushSizeChange = { brushSize = it },
                brushShape = brushShape,
                onBrushShapeChange = { brushShape = it },
                opacity = opacity,
                onOpacityChange = { opacity = it },
                selectedColor = selectedColor,
                customColors = customColors,
                onColorSelected = { color ->
                    selectedColor = color
                    if (!customColors.contains(color)) {
                        customColors = customColors + color
                    }
                },
                onShowColorPicker = { showColorPicker = true },
                modifier = Modifier.padding(16.dp)
            )
        }
    }
    
    // Color Picker Dialog
    if (showColorPicker) {
        ColorPickerDialog(
            currentColor = selectedColor,
            onColorSelected = { color ->
                selectedColor = color
                if (!customColors.contains(color)) {
                    customColors = customColors + color
                }
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false }
        )
    }
    
    // Save Dialog
    if (showSaveDialog) {
        SaveDialog(
            textureName = texture.name,
            textureCategory = texture.category,
            onSave = { category, name ->
                scope.launch {
                    // Save to textures/[dropdown name]/[image name] format
                    val categoryName = category.name.lowercase()
                    val fileName = "$name.png"
                    val path = "textures/$categoryName/$fileName"
                    
                    // Create the bitmap from the canvas
                    val bitmap = createBitmapFromCanvas(strokePaths, canvasSize, density)
                    
                    // Save the bitmap
                    viewModel.saveTextureToPath(packId, path, bitmap)
                    
                    hasUnsavedChanges = false
                    showSaveDialog = false
                }
            },
            onDismiss = { showSaveDialog = false }
        )
    }
    
    // Exit Dialog
    if (showExitDialog) {
        ExitDialog(
            onSave = { showSaveDialog = true },
            onExit = { 
                hasUnsavedChanges = false
                onNavigateBack()
            },
            onDismiss = { showExitDialog = false }
        )
    }
    
    // Import Dialog
    if (showImportDialog) {
        ImportDialog(
            onImportFromDevice = { imagePickerLauncher.launch("image/*") },
            onImportFromLibrary = { 
                // TODO: Implement library import
                showImportDialog = false
            },
            onDismiss = { showImportDialog = false }
        )
    }
}

@Composable
fun ToolBar(
    currentTool: EditorTool,
    onToolSelected: (EditorTool) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(EditorTool.values()) { tool ->
            ToolButton(
                tool = tool,
                isSelected = currentTool == tool,
                onClick = { onToolSelected(tool) }
            )
        }
    }
}

@Composable
fun ToolButton(
    tool: EditorTool,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFFFFB6C1) else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Icon(
            imageVector = when (tool) {
                EditorTool.BRUSH -> Icons.Default.Brush
                EditorTool.ERASER -> Icons.Default.CleaningServices
                EditorTool.COLOR_PICKER -> Icons.Default.Palette
                EditorTool.FILL -> Icons.Default.FormatPaint
                EditorTool.IMPORT -> Icons.Default.FileUpload
                EditorTool.SAVE -> Icons.Default.Save
                EditorTool.EXIT -> Icons.Default.ExitToApp
            },
            contentDescription = tool.name,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun DrawingCanvas(
    modifier: Modifier = Modifier,
    currentTool: EditorTool,
    brushSize: Float,
    brushShape: BrushShape,
    selectedColor: Color,
    opacity: Float,
    strokePaths: List<StrokePath>,
    currentPath: StrokePath?,
    onStrokePathChanged: (StrokePath) -> Unit,
    onStrokeComplete: (StrokePath) -> Unit
) {
    Canvas(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val newPath = StrokePath(
                            path = Path().apply { moveTo(offset.x, offset.y) },
                            color = selectedColor,
                            strokeWidth = brushSize,
                            shape = brushShape,
                            opacity = opacity,
                            isEraser = currentTool == EditorTool.ERASER
                        )
                        onStrokePathChanged(newPath)
                    },
                    onDrag = { change ->
                        currentPath?.let { path ->
                            val updatedPath = path.copy(
                                path = Path(path.path).apply {
                                    lineTo(change.position.x, change.position.y)
                                }
                            )
                            onStrokePathChanged(updatedPath)
                        }
                    },
                    onDragEnd = {
                        currentPath?.let { path ->
                            onStrokeComplete(path)
                        }
                    }
                )
            }
    ) {
        // Draw all completed paths
        strokePaths.forEach { strokePath ->
            drawPath(
                path = strokePath.path,
                color = strokePath.color.copy(alpha = strokePath.opacity),
                style = Stroke(
                    width = strokePath.strokeWidth,
                    cap = when (strokePath.shape) {
                        BrushShape.ROUND -> StrokeCap.Round
                        BrushShape.SQUARE -> StrokeCap.Square
                        else -> StrokeCap.Round
                    }
                )
            )
        }
        
        // Draw current path
        currentPath?.let { strokePath ->
            drawPath(
                path = strokePath.path,
                color = strokePath.color.copy(alpha = strokePath.opacity),
                style = Stroke(
                    width = strokePath.strokeWidth,
                    cap = when (strokePath.shape) {
                        BrushShape.ROUND -> StrokeCap.Round
                        BrushShape.SQUARE -> StrokeCap.Square
                        else -> StrokeCap.Round
                    }
                )
            )
        }
    }
}

@Composable
fun PropertiesPanel(
    brushSize: Float,
    onBrushSizeChange: (Float) -> Unit,
    brushShape: BrushShape,
    onBrushShapeChange: (BrushShape) -> Unit,
    opacity: Float,
    onOpacityChange: (Float) -> Unit,
    selectedColor: Color,
    customColors: List<Color>,
    onColorSelected: (Color) -> Unit,
    onShowColorPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Brush Size
            Text(
                text = "Brush Size: ${brushSize.toInt()}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Slider(
                value = brushSize,
                onValueChange = onBrushSizeChange,
                valueRange = 1f..50f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFFFB6C1),
                    activeTrackColor = Color(0xFFFFB6C1)
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Opacity
            Text(
                text = "Opacity: ${(opacity * 100).toInt()}%",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Slider(
                value = opacity,
                onValueChange = onOpacityChange,
                valueRange = 0.1f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFFFB6C1),
                    activeTrackColor = Color(0xFFFFB6C1)
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Color Palette
            Text(
                text = "Colors",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                // Default colors
                items(defaultColors) { color ->
                    ColorButton(
                        color = color,
                        isSelected = selectedColor == color,
                        onClick = { onColorSelected(color) }
                    )
                }
                
                // Custom colors
                items(customColors) { color ->
                    ColorButton(
                        color = color,
                        isSelected = selectedColor == color,
                        onClick = { onColorSelected(color) }
                    )
                }
                
                // Color picker button
                item {
                    Button(
                        onClick = onShowColorPicker,
                        modifier = Modifier.size(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.outline
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Color",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
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
                color = if (isSelected) Color.Black else Color.Gray,
                shape = CircleShape
            )
            .clickable { onClick() }
    )
}

@Composable
fun ColorPickerDialog(
    currentColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Select Color",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Color preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(currentColor, RoundedCornerShape(8.dp))
                        .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // RGB Sliders would go here in a real implementation
                // For now, we'll show a grid of colors
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(extendedColors) { color ->
                        ColorButton(
                            color = color,
                            isSelected = currentColor == color,
                            onClick = { onColorSelected(color) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onColorSelected(currentColor) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFB6C1)
                        )
                    ) {
                        Text("Select", color = Color.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun SaveDialog(
    textureName: String,
    textureCategory: TextureCategory,
    onSave: (TextureCategory, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(textureCategory) }
    var fileName by remember { mutableStateOf(textureName.removeSuffix(".png")) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Save Texture",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Category selection
                Text(
                    text = "Category",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    items(TextureCategory.values()) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category.displayName) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // File name input
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("File Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Will save to: textures/${selectedCategory.name.lowercase()}/$fileName.png",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onSave(selectedCategory, fileName) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFB6C1)
                        )
                    ) {
                        Text("Save", color = Color.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun ExitDialog(
    onSave: () -> Unit,
    onExit: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Unsaved Changes",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "You have unsaved changes. What would you like to do?",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onExit,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Exit without saving")
                    }
                    
                    Button(
                        onClick = onSave,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFB6C1)
                        )
                    ) {
                        Text("Save & Exit", color = Color.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun ImportDialog(
    onImportFromDevice: () -> Unit,
    onImportFromLibrary: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Import Image",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Button(
                    onClick = onImportFromDevice,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFB6C1)
                    )
                ) {
                    Icon(Icons.Default.PhoneAndroid, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import from Device", color = Color.Black)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedButton(
                    onClick = onImportFromLibrary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import from Library")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

// Data classes and utility functions
data class StrokePath(
    val path: Path,
    val color: Color,
    val strokeWidth: Float,
    val shape: BrushShape,
    val opacity: Float,
    val isEraser: Boolean = false
)

private fun createBitmapFromCanvas(
    strokePaths: List<StrokePath>,
    canvasSize: androidx.compose.ui.unit.Dp,
    density: androidx.compose.ui.unit.Density
): Bitmap {
    val sizeInPx = with(density) { canvasSize.toPx() }.toInt()
    val bitmap = Bitmap.createBitmap(sizeInPx, sizeInPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    // Fill with white background
    canvas.drawColor(android.graphics.Color.WHITE)
    
    // Draw all paths
    strokePaths.forEach { strokePath ->
        val paint = Paint().apply {
            color = strokePath.color.toArgb()
            strokeWidth = strokePath.strokeWidth
            style = Paint.Style.STROKE
            isAntiAlias = true
            alpha = (strokePath.opacity * 255).toInt()
            
            if (strokePath.isEraser) {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            }
        }
        
        canvas.drawPath(strokePath.path, paint)
    }
    
    return bitmap
}

private val defaultColors = listOf(
    Color.Red, Color.Blue, Color.Green, Color.Yellow,
    Color.Cyan, Color.Magenta, Color.Gray, Color.Black
)

private val extendedColors = listOf(
    Color.Red, Color(0xFFFF6B6B), Color(0xFFFF8E8E), Color(0xFFFFB3B3),
    Color.Blue, Color(0xFF4ECDC4), Color(0xFF45B7D1), Color(0xFF96CEB4),
    Color.Green, Color(0xFF6BCF7F), Color(0xFF4DD0E1), Color(0xFF81C784),
    Color.Yellow, Color(0xFFFD79A8), Color(0xFFFFD93D), Color(0xFFFAB1A0),
    Color.Cyan, Color.Magenta, Color.Gray, Color.Black, Color.White
)
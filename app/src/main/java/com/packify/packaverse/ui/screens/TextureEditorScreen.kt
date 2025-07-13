package com.packify.packaverse.ui.screens

import android.content.Intent
import android.graphics.Bitmap
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.packify.packaverse.data.TextureItem
import com.packify.packaverse.viewmodel.TexturePackViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.core.content.FileProvider
import android.graphics.BitmapFactory
import android.widget.Toast

enum class EditorTool {
    BRUSH, ERASER, COLOR_PICKER, FILL
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
    var selectedColor by remember { mutableStateOf(Color.Red) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var hasUnsavedChanges by remember { mutableStateOf(false) }
    var textureBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Initialize texture bitmap
    LaunchedEffect(texture) {
        try {
            val inputStream = context.assets.open("base/${texture.category.assetPath}/${texture.name}.png")
            textureBitmap = BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            // If texture not found in assets, create a default bitmap
            textureBitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        }
    }
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { 
            scope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    textureBitmap = BitmapFactory.decodeStream(inputStream)
                    hasUnsavedChanges = true
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to import texture", Toast.LENGTH_SHORT).show()
                }
            }
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
                            showSaveDialog = true
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Icon(Icons.Default.Upload, contentDescription = "Import")
                    }
                    IconButton(onClick = { 
                        scope.launch {
                            shareTexture(context, textureBitmap, texture.name)
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
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
            // Toolbar
            Toolbar(
                currentTool = currentTool,
                onToolSelected = { currentTool = it },
                brushSize = brushSize,
                onBrushSizeChanged = { brushSize = it },
                selectedColor = selectedColor,
                onColorSelected = { selectedColor = it },
                onColorPickerClicked = { showColorPicker = true }
            )
            
            // Canvas
            TextureCanvas(
                texture = texture,
                textureBitmap = textureBitmap,
                currentTool = currentTool,
                brushSize = brushSize,
                selectedColor = selectedColor,
                onDrawingChanged = { hasUnsavedChanges = true },
                onBitmapChanged = { newBitmap ->
                    textureBitmap = newBitmap
                    hasUnsavedChanges = true
                }
            )
            
            // Color Palette
            ColorPalette(
                selectedColor = selectedColor,
                onColorSelected = { selectedColor = it }
            )
        }
        
        // Save Dialog
        if (showSaveDialog) {
            AlertDialog(
                onDismissRequest = { showSaveDialog = false },
                title = { Text("Save Changes") },
                text = { Text("Do you want to save your changes?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                saveTexture(context, textureBitmap, packId, texture)
                                hasUnsavedChanges = false
                                showSaveDialog = false
                                onNavigateBack()
                            }
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showSaveDialog = false
                            if (!hasUnsavedChanges) {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Text("Don't Save")
                    }
                }
            )
        }
        
        // Color Picker Dialog
        if (showColorPicker) {
            ColorPickerDialog(
                selectedColor = selectedColor,
                onColorSelected = { 
                    selectedColor = it
                    showColorPicker = false
                },
                onDismiss = { showColorPicker = false }
            )
        }
    }
}

@Composable
fun Toolbar(
    currentTool: EditorTool,
    onToolSelected: (EditorTool) -> Unit,
    brushSize: Float,
    onBrushSizeChanged: (Float) -> Unit,
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
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
                    ToolButton(
                        tool = tool,
                        isSelected = currentTool == tool,
                        onClick = { onToolSelected(tool) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Brush Size Slider
            if (currentTool == EditorTool.BRUSH || currentTool == EditorTool.ERASER) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Size: ${brushSize.toInt()}",
                        fontSize = 14.sp,
                        modifier = Modifier.width(60.dp)
                    )
                    Slider(
                        value = brushSize,
                        onValueChange = onBrushSizeChanged,
                        valueRange = 1f..50f,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Color Picker
            if (currentTool == EditorTool.BRUSH || currentTool == EditorTool.FILL) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Color:",
                        fontSize = 14.sp,
                        modifier = Modifier.width(60.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(selectedColor, CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            .clickable { onColorPickerClicked() }
                    )
                }
            }
        }
    }
}

@Composable
fun ToolButton(
    tool: EditorTool,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val icon = when (tool) {
        EditorTool.BRUSH -> Icons.Default.Brush
        EditorTool.ERASER -> Icons.Default.Clear
        EditorTool.COLOR_PICKER -> Icons.Default.ColorLens
        EditorTool.FILL -> Icons.Default.FormatPaint
    }
    
    val contentDescription = when (tool) {
        EditorTool.BRUSH -> "Paint Brush"
        EditorTool.ERASER -> "Eraser"
        EditorTool.COLOR_PICKER -> "Color Picker"
        EditorTool.FILL -> "Fill Tool"
    }
    
    Card(
        modifier = Modifier
            .size(48.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFFFB6C1) else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TextureCanvas(
    texture: TextureItem,
    textureBitmap: Bitmap?,
    currentTool: EditorTool,
    brushSize: Float,
    selectedColor: Color,
    onDrawingChanged: () -> Unit,
    onBitmapChanged: (Bitmap) -> Unit
) {
    var path by remember { mutableStateOf(Path()) }
    var lastPoint by remember { mutableStateOf<Offset?>(null) }
    val density = LocalDensity.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                lastPoint = offset
                                path.reset()
                                path.moveTo(offset.x, offset.y)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val newPoint = change.position
                                lastPoint?.let { last ->
                                    path.quadraticBezierTo(
                                        last.x, last.y,
                                        (last.x + newPoint.x) / 2,
                                        (last.y + newPoint.y) / 2
                                    )
                                }
                                lastPoint = newPoint
                                onDrawingChanged()
                            },
                            onDragEnd = {
                                lastPoint = null
                                // Apply the drawing to the bitmap
                                textureBitmap?.let { bitmap ->
                                    val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                                    val canvas = Canvas(mutableBitmap)
                                    val paint = Paint().apply {
                                        color = selectedColor.toArgb()
                                        strokeWidth = brushSize
                                        style = Paint.Style.STROKE
                                        strokeCap = Paint.Cap.ROUND
                                        strokeJoin = Paint.Join.ROUND
                                        isAntiAlias = true
                                    }
                                    
                                    if (currentTool == EditorTool.ERASER) {
                                        paint.color = android.graphics.Color.TRANSPARENT
                                        paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
                                    }
                                    
                                    canvas.drawPath(path, paint)
                                    onBitmapChanged(mutableBitmap)
                                }
                            }
                        )
                    }
            ) {
                // Draw checkerboard background
                drawCheckerboardBackground(size)
                
                // Draw the texture bitmap
                textureBitmap?.let { bitmap ->
                    val imageBitmap = bitmap.asImageBitmap()
                    drawImage(
                        image = imageBitmap,
                        dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                        filterQuality = FilterQuality.None // Keep pixelated look
                    )
                }
                
                // Draw the current path
                if (currentTool == EditorTool.BRUSH) {
                    drawPath(
                        path = androidx.compose.ui.graphics.Path().apply {
                            addPath(path.asComposePath())
                        },
                        color = selectedColor,
                        style = Stroke(width = brushSize, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                } else if (currentTool == EditorTool.ERASER) {
                    drawPath(
                        path = androidx.compose.ui.graphics.Path().apply {
                            addPath(path.asComposePath())
                        },
                        color = Color.Red.copy(alpha = 0.5f),
                        style = Stroke(width = brushSize, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }
        }
    }
}

@Composable
fun ColorPalette(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit
) {
    val colors = listOf(
        Color.Red, Color.Green, Color.Blue, Color.Yellow,
        Color.Cyan, Color.Magenta, Color.Black, Color.White,
        Color.Gray, Color.DarkGray, Color.LightGray
    )
    
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
            Text(
                text = "Color Palette",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(colors) { color ->
                    ColorButton(
                        color = color,
                        isSelected = selectedColor == color,
                        onClick = { onColorSelected(color) }
                    )
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
            .size(32.dp)
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
fun ColorPickerDialog(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Color") },
        text = {
            // Simple color picker implementation
            Column {
                Text("Selected color will be applied to the brush tool.")
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(selectedColor, RoundedCornerShape(8.dp))
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

// Helper functions
private fun DrawScope.drawCheckerboardBackground(size: androidx.compose.ui.geometry.Size) {
    val cellSize = 10.dp.toPx()
    val cols = (size.width / cellSize).toInt()
    val rows = (size.height / cellSize).toInt()
    
    for (row in 0..rows) {
        for (col in 0..cols) {
            val color = if ((row + col) % 2 == 0) Color.White else Color.LightGray
            drawRect(
                color = color,
                topLeft = Offset(col * cellSize, row * cellSize),
                size = androidx.compose.ui.geometry.Size(cellSize, cellSize)
            )
        }
    }
}

private suspend fun saveTexture(
    context: android.content.Context,
    bitmap: Bitmap?,
    packId: String,
    texture: TextureItem
) {
    bitmap?.let { bmp ->
        try {
            val texturePacksDir = File(context.filesDir, "texture_packs")
            val packDir = File(texturePacksDir, packId)
            val texturesDir = File(packDir, "textures")
            val categoryDir = File(texturesDir, texture.category.displayName.lowercase())
            
            if (!categoryDir.exists()) {
                categoryDir.mkdirs()
            }
            
            val textureFile = File(categoryDir, "${texture.name}.png")
            val outputStream = FileOutputStream(textureFile)
            bmp.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.close()
            
            Toast.makeText(context, "Texture saved successfully!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to save texture: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

private suspend fun shareTexture(
    context: android.content.Context,
    bitmap: Bitmap?,
    textureName: String
) {
    bitmap?.let { bmp ->
        try {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            
            val stream = FileOutputStream(File(cachePath, "$textureName.png"))
            bmp.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()
            
            val newFile = File(cachePath, "$textureName.png")
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                newFile
            )
            
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, contentUri)
                type = "image/png"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(Intent.createChooser(shareIntent, "Share Texture"))
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to share texture: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
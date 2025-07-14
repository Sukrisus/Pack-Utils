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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
            redoStack.clear()
        }
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack.add(canvasBitmap!!.copy(canvasBitmap!!.config, true))
            canvasBitmap = undoStack.removeAt(undoStack.lastIndex)
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack.add(canvasBitmap!!.copy(canvasBitmap!!.config, true))
            canvasBitmap = redoStack.removeAt(redoStack.lastIndex)
        }
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
                    IconButton(onClick = { undo() }) {
                        Icon(Icons.Default.Undo, contentDescription = "Undo")
                    }
                    IconButton(onClick = { redo() }) {
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
                    Icon(Icons.Default.Clear, contentDescription = "Eraser")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            EnhancedTextureCanvas(
                texture = texture,
                currentTool = currentTool,
                brushSize = brushSize, // Use the state variable
                brushShape = BrushShape.ROUND, // Default shape
                selectedColor = selectedColor,
                opacity = 1f,
                onDrawingChanged = {
                    hasUnsavedChanges = true
                    pushUndo(canvasBitmap)
                },
                onBitmapUpdated = { bitmap ->
                    canvasBitmap = bitmap
                },
                externalBitmap = canvasBitmap
            )
        }
        // Color picker dialog
        if (showColorPicker) {
            AdvancedColorPickerDialog(
                selectedColor = selectedColor,
                onColorSelected = { color ->
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
                            modifier = Modifier.fillMaxWidth()
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
                            modifier = Modifier.fillMaxWidth()
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
                                .background(selectedColor, RoundedCornerShape(8.dp))
                                .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
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
        EditorTool.ERASER -> Icons.Default.Clear to "Eraser"
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
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
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
        shape = RoundedCornerShape(6.dp)
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
    externalBitmap: android.graphics.Bitmap? = null
) {
    var path by remember { mutableStateOf(Path()) }
    var lastPoint by remember { mutableStateOf<Offset?>(null) }
    var drawingPoints by remember { mutableStateOf(listOf<Offset>()) }
    var canvasBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(externalBitmap) }
    var canvasSize by remember { mutableStateOf<androidx.compose.ui.geometry.Size?>(null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.7f)
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
                    .onGloballyPositioned { coordinates ->
                        canvasSize = coordinates.size.toSize()
                    }
                    .pointerInput(currentTool, brushSize, selectedColor, externalBitmap) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                lastPoint = offset
                                drawingPoints = listOf(offset)
                                path.reset()
                                path.moveTo(offset.x, offset.y)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val newPoint = change.position
                                drawingPoints = drawingPoints + newPoint

                                lastPoint?.let { last ->
                                    when (currentTool) {
                                        EditorTool.BRUSH, EditorTool.PENCIL -> {
                                            path.lineTo(newPoint.x, newPoint.y)
                                        }
                                        EditorTool.ERASER -> {
                                            path.lineTo(newPoint.x, newPoint.y)
                                        }
                                        EditorTool.SPRAY_PAINT -> {
                                            repeat(5) {
                                                val randomX = newPoint.x + (Math.random() - 0.5).toFloat() * brushSize
                                                val randomY = newPoint.y + (Math.random() - 0.5).toFloat() * brushSize
                                                path.addCircle(randomX, randomY, 2f, Path.Direction.CW)
                                            }
                                        }
                                        else -> {}
                                    }
                                }
                                lastPoint = newPoint
                                onDrawingChanged()
                            },
                            onDragEnd = {
                                lastPoint = null
                                drawingPoints = emptyList()
                                // Commit the path to the bitmap
                                canvasBitmap?.let { bmp ->
                                    val canvas = Canvas(bmp)
                                    val paint = Paint().apply {
                                        isAntiAlias = true
                                        color = when (currentTool) {
                                            EditorTool.ERASER -> android.graphics.Color.TRANSPARENT
                                            else -> selectedColor.copy(alpha = opacity).toArgb()
                                        }
                                        style = Paint.Style.STROKE
                                        strokeWidth = brushSize
                                        strokeCap = Paint.Cap.ROUND
                                        strokeJoin = Paint.Join.ROUND
                                        xfermode = if (currentTool == EditorTool.ERASER) android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR) else null
                                    }
                                    // Convert Compose path to Android path
                                    val androidPath = android.graphics.Path()
                                    val pathPoints = path.asComposePath().asAndroidPathPoints()
                                    if (pathPoints.isNotEmpty()) {
                                        androidPath.moveTo(pathPoints[0].x, pathPoints[0].y)
                                        for (i in 1 until pathPoints.size) {
                                            androidPath.lineTo(pathPoints[i].x, pathPoints[i].y)
                                        }
                                        canvas.drawPath(androidPath, paint)
                                    }
                                    onBitmapUpdated(bmp.copy(bmp.config, true))
                                }
                                path.reset()
                            }
                        )
                    }
            ) {
                // Draw checkerboard background for transparency
                val checkerSize = 20f
                val checkerColor1 = Color.White
                val checkerColor2 = Color.LightGray
                for (x in 0 until (size.width / checkerSize).toInt() + 1) {
                    for (y in 0 until (size.height / checkerSize).toInt() + 1) {
                        val isEven = (x + y) % 2 == 0
                        val color = if (isEven) checkerColor1 else checkerColor2
                        drawRect(
                            color = color,
                            topLeft = Offset(x * checkerSize, y * checkerSize),
                            size = androidx.compose.ui.geometry.Size(checkerSize, checkerSize)
                        )
                    }
                }
                // Draw the bitmap as the background
                canvasBitmap?.let { bmp ->
                    drawImage(
                        image = bmp.asImageBitmap(),
                        topLeft = Offset.Zero,
                        alpha = 1f
                    )
                }
                // Draw the current path
                when (currentTool) {
                    EditorTool.BRUSH, EditorTool.PENCIL -> {
                        drawPath(
                            path = androidx.compose.ui.graphics.Path().apply {
                                addPath(path.asComposePath())
                            },
                            color = selectedColor.copy(alpha = opacity),
                            style = Stroke(
                                width = brushSize,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                    EditorTool.ERASER -> {
                        drawPath(
                            path = androidx.compose.ui.graphics.Path().apply {
                                addPath(path.asComposePath())
                            },
                            color = Color.Transparent,
                            style = Stroke(
                                width = brushSize,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            ),
                            blendMode = BlendMode.Clear
                        )
                    }
                    EditorTool.SPRAY_PAINT -> {
                        drawingPoints.forEach { point ->
                            repeat(3) {
                                val randomX = point.x + (Math.random() - 0.5).toFloat() * brushSize / 2
                                val randomY = point.y + (Math.random() - 0.5).toFloat() * brushSize / 2
                                drawCircle(
                                    color = selectedColor.copy(alpha = opacity * 0.3f),
                                    radius = 2f,
                                    center = Offset(randomX, randomY)
                                )
                            }
                        }
                    }
                    else -> {}
                }
                // Draw brush preview
                lastPoint?.let { point ->
                    when (brushShape) {
                        BrushShape.ROUND -> {
                            drawCircle(
                                color = selectedColor.copy(alpha = 0.3f),
                                radius = brushSize / 2,
                                center = point,
                                style = Stroke(width = 2f)
                            )
                        }
                        BrushShape.SQUARE -> {
                            drawRect(
                                color = selectedColor.copy(alpha = 0.3f),
                                topLeft = Offset(point.x - brushSize / 2, point.y - brushSize / 2),
                                size = androidx.compose.ui.geometry.Size(brushSize, brushSize),
                                style = Stroke(width = 2f)
                            )
                        }
                        BrushShape.DIAMOND -> {
                            val path = androidx.compose.ui.graphics.Path().apply {
                                moveTo(point.x, point.y - brushSize / 2)
                                lineTo(point.x + brushSize / 2, point.y)
                                lineTo(point.x, point.y + brushSize / 2)
                                lineTo(point.x - brushSize / 2, point.y)
                                close()
                            }
                            drawPath(
                                path = path,
                                color = selectedColor.copy(alpha = 0.3f),
                                style = Stroke(width = 2f)
                            )
                        }
                        BrushShape.STAR -> {
                            drawCircle(
                                color = selectedColor.copy(alpha = 0.3f),
                                radius = brushSize / 2,
                                center = point,
                                style = Stroke(width = 2f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdvancedColorPalette(
    selectedColor: Color,
    customColors: List<Color>,
    onColorSelected: (Color) -> Unit,
    onAddCustomColor: (Color) -> Unit
) {
    val colors = listOf(
        Color.Red, Color.Green, Color.Blue, Color.Yellow,
        Color.Cyan, Color.Magenta, Color.Black, Color.White,
        Color.Gray, Color.DarkGray, Color.LightGray,
        Color(0xFFFF8C00), Color(0xFF9370DB), Color(0xFF20B2AA),
        Color(0xFFDC143C), Color(0xFF228B22), Color(0xFF4B0082)
    ) + customColors

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

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Add Custom Color",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = {
                        val newColor = Color(
                            (Math.random() * 255).toInt(),
                            (Math.random() * 255).toInt(),
                            (Math.random() * 255).toInt()
                        )
                        onAddCustomColor(newColor)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFB6C1),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Custom Color",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Random")
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
                color = if (isSelected) Color(0xFFFFB6C1) else MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
            .clickable { onClick() }
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

    val currentColor = Color(red, green, blue, alpha)

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
                        .background(currentColor, RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                )

                Spacer(modifier = Modifier.height(16.dp))

                // RGB Sliders
                Text("Red: ${(red * 255).toInt()}")
                Slider(
                    value = red,
                    onValueChange = { red = it },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.Red,
                        activeTrackColor = Color.Red
                    )
                )

                Text("Green: ${(green * 255).toInt()}")
                Slider(
                    value = green,
                    onValueChange = { green = it },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.Green,
                        activeTrackColor = Color.Green
                    )
                )

                Text("Blue: ${(blue * 255).toInt()}")
                Slider(
                    value = blue,
                    onValueChange = { blue = it },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.Blue,
                        activeTrackColor = Color.Blue
                    )
                )

                Text("Alpha: ${(alpha * 255).toInt()}")
                Slider(
                    value = alpha,
                    onValueChange = { alpha = it },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.Gray,
                        activeTrackColor = Color.Gray
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
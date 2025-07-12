# MCPE Texture Pack Maker

A clean, modern Android app for creating and managing Minecraft Pocket Edition (MCPE) texture packs. Built with Kotlin and Jetpack Compose with Material 3 design.

## Features

- **Create New Texture Packs**: Generate proper MCPE texture pack structure with manifest.json
- **Edit Existing Packs**: Modify pack names, descriptions, and versions
- **Pack Icon Management**: Change pack_icon.png with automatic 128x128 resizing
- **Manifest Editing**: Full control over manifest.json properties
- **Export Functionality**: Export packs as .mcpack files for easy sharing
- **Material 3 Design**: Clean, modern UI with dark theme support
- **File Management**: Proper texture pack directory structure creation

## Screenshots

The app features a clean, minimal interface inspired by modern design principles:
- Dark theme with purple accent colors
- Simple home screen with clear action buttons
- Intuitive pack management and editing interface
- Bottom navigation for easy access to different sections

## Technical Details

### Architecture
- **MVVM Pattern**: Clean separation of concerns with ViewModel
- **Jetpack Compose**: Modern UI toolkit for native Android
- **Material 3**: Latest Material Design components
- **Kotlin Coroutines**: Asynchronous programming for file operations

### Key Components
- `TexturePackRepository`: Handles file operations and pack management
- `TexturePackViewModel`: Manages UI state and business logic
- `Manifest` & `TexturePack` data classes: Proper MCPE structure modeling
- Compose UI screens: Home, Create, Edit, and Pack List screens

### MCPE Compatibility
- Creates proper manifest.json format (version 2)
- Generates correct texture directory structure
- Handles pack icons with proper dimensions (128x128)
- Supports all MCPE texture categories (blocks, items, entities, etc.)

## Building and Installation

### Prerequisites
- Android Studio Hedgehog or later
- Android SDK with API level 34
- Kotlin 1.9.10 or later

### Setup
1. Clone the repository
2. Open in Android Studio
3. Sync the project with Gradle files
4. Run on an Android device or emulator (API 24+)

### Dependencies
- Jetpack Compose BOM 2023.10.01
- Material 3
- Navigation Compose
- Accompanist Permissions
- Gson for JSON handling

## Usage

### Creating a New Pack
1. Tap "CREATE" on the home screen
2. Enter pack name and description
3. Tap "CREATE PACK" to generate the structure

### Editing a Pack
1. Tap "MANAGE PACKS" from home
2. Select the pack you want to edit
3. Modify pack details, change icon, or update version
4. Use "EXPORT PACK" to create a shareable .mcpack file

### Pack Structure
The app creates the following structure:
```
pack_name/
├── manifest.json
├── pack_icon.png
├── textures/
│   ├── blocks/
│   ├── items/
│   ├── entity/
│   ├── environment/
│   ├── gui/
│   ├── particle/
│   └── misc/
└── metadata.json (app-specific)
```

## Contributing

This project is open source and welcomes contributions. Please ensure:
- Code follows Kotlin conventions
- UI changes maintain Material 3 design principles
- All features are tested on multiple Android versions
- Documentation is updated for new features

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- Material Design 3 guidelines
- Minecraft Pocket Edition for texture pack format specifications
- Android Jetpack Compose documentation and examples

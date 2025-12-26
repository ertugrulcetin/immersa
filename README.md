# Immersa

<p align="center">
  <img src="resources/public/img/logo.png" alt="Immersa Logo" width="200"/>
</p>

**A Web-based 3D Presentation Tool** - Create stunning 3D presentations with animated transitions between slides.

## 🎥 Demo

Watch Immersa in action: [Demo Video](https://www.youtube.com/watch?v=1yjtpf59jv0)

<p align="center">
  <img src="resources/public/img/app.png" alt="Immersa App Overview" width="800"/>
</p>

## What is Immersa?

Immersa is an innovative presentation tool that brings your presentations into the third dimension. Unlike traditional slide-based tools, Immersa allows you to:

- **Load 3D Models**: Import `.glb` 3D models into your presentation
- **Add Images**: Place 2D images in 3D space
- **Create Text**: Add 3D text elements to your slides
- **Animate Transitions**: When you move, rotate, or scale objects between slides, Immersa automatically creates smooth animated transitions

### How Slide Animation Works

The magic of Immersa lies in its interpolation system:

1. **Create Slide 1**: Position your 3D models, images, and text where you want them
2. **Duplicate Slide**: Create a new slide (which copies all objects)
3. **Reposition Objects**: Move, rotate, or scale objects to new positions on the new slide
4. **Present**: When presenting, Immersa smoothly interpolates between positions, creating fluid animations

This means if a car model is on the left in slide 1 and on the right in slide 2, it will smoothly animate from left to right during the transition!

## Features

- **3D Scene Editor**: Full 3D viewport with camera controls
- **GLB Model Support**: Import any `.glb` 3D model
- **Image Support**: Add images (JPG, PNG) to your 3D scene
- **3D Text**: Create and style 3D text elements
- **Animated Transitions**: Automatic smooth animations between slides
- **Presentation Mode**: Full-screen presentation with progress bar
- **Local Storage**: All data stored locally in your browser (IndexedDB)
- **Undo/Redo**: Full undo/redo support
- **Keyboard Shortcuts**: Quick access to common actions

## Getting Started

### Prerequisites

- **Node.js** (v16 or higher recommended)
- **npm** or **yarn**
- **Java** (JDK 11 or higher) - Required for ClojureScript compilation

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/ertugrulcetin/immersa.git
   cd immersa
   ```

2. **Install dependencies**
   ```bash
   npm install
   ```

3. **Start the development server**
   ```bash
   npm run watch
   ```

4. **Open in browser**
   Navigate to [http://localhost:8280](http://localhost:8280)

### Building for Production

```bash
npm run release
```

The production build will be in `resources/public/js/compiled/`.

### Running as Electron Desktop App

Immersa can run as a standalone desktop application using Electron.

1. **Start the development server first**
   ```bash
   npm run watch
   ```

2. **In a separate terminal, launch Electron**
   ```bash
   npm run electron
   ```

### Building Electron App

To create a distributable desktop application:

```bash
npm run electron:build
```

This will:
- Build the production version of the app
- Package it as a macOS DMG installer (Apple Silicon/ARM64)
- Output the installer to the `dist/` directory

**Note**: The current build configuration targets macOS ARM64. To build for other platforms, modify the `build` section in `package.json`.

## Usage Guide

### Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| `T` | Add text |
| `D` | Duplicate slide |
| `B` | Add blank slide |
| `Cmd/Ctrl + Z` | Undo |
| `Cmd/Ctrl + Shift + Z` | Redo |
| `Delete/Backspace` | Delete selected object |
| `Escape` | Exit present mode |
| `Arrow Keys` | Navigate slides (in present mode) |


### Exporting

Click **"Export"** to download your presentation as an `.edn` file. This file contains all slide data and can be loaded later.

## Technical Details

### Technology Stack

- **ClojureScript** - Primary programming language
- **shadow-cljs** - Build tool
- **Reagent** - React wrapper for ClojureScript
- **Re-frame** - State management
- **Babylon.js** - 3D rendering engine
- **IndexedDB** - Local data storage

### Project Structure

```
immersa/
├── src/immersa/
│   ├── common/           # Shared utilities
│   │   ├── local_storage.cljs  # IndexedDB storage
│   │   └── firebase.cljs       # Storage adapter
│   ├── scene/            # 3D scene management
│   │   ├── api/          # Babylon.js wrappers
│   │   ├── core.cljs     # Scene initialization
│   │   └── slide.cljs    # Slide/animation logic
│   ├── ui/               # User interface
│   │   ├── editor/       # Editor components
│   │   └── present/      # Presentation mode
│   └── presentations/    # Default presentations
├── resources/public/     # Static assets
│   ├── img/              # Images
│   ├── model/            # Sample 3D models
│   ├── shader/           # Custom shaders
│   └── index.html        # Entry point
└── shadow-cljs.edn       # Build configuration
```

### Data Storage

All data is stored locally in your browser using IndexedDB:

- **Presentations**: Slide data and structure
- **Thumbnails**: Slide preview images
- **Files**: Uploaded images and 3D models

Data persists across sessions. Clear browser data to reset.


## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

<p align="center">
  Made with ❤️ by <a href="https://github.com/ertugrulcetin">Ertuğrul Çetin</a>
</p>


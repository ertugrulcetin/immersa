# ParticleDeck

A particle network slide deck generator with stunning morphing animations.

## Features

- **Text-based Slides**: Create slides with markdown-style text formatting
- **Particle Network Backgrounds**: Animated particle network that morphs between geometric shapes
- **14 Built-in Shapes**: Sphere, ellipse, tree, 3D sphere, wave, scatter, grid, clusters, spiral, curve, bars, network, galaxy, and line
- **Real-time Preview**: See your presentation as you build it
- **Export to HTML**: Generate standalone HTML presentations
- **Keyboard Navigation**: Arrow keys to navigate between slides
- **Auto-save**: Slides are automatically saved to browser localStorage

## Usage

1. **Open** `index.html` in a web browser
2. **Create slides** using the "New Slide" button
3. **Edit content** using markdown syntax:
   - `# Heading 1` for large titles
   - `## Heading 2` for subtitles
   - `**bold**` for bold text
   - `*italic*` for italic text
4. **Choose a particle shape** from the dropdown
5. **Set text alignment** (left, center, right)
6. **Navigate** with arrow buttons or keyboard arrows
7. **Export** your presentation as a standalone HTML file

## Markdown Support

- Headings: `#`, `##`, `###`, `####`, `#####`, `######`
- Bold: `**text**` or `__text__`
- Italic: `*text*` or `_text_`
- Lists: `- item` or `1. item`
- Code: `` `code` ``
- Blockquotes: `> quote`

## Keyboard Shortcuts

- `←` / `→` - Navigate between slides
- `Ctrl/Cmd + N` - New slide
- `Delete` - Delete current slide (with confirmation)

## Technology

- Vanilla HTML/CSS/JavaScript
- Canvas API for particle rendering
- Google Fonts: Fraunces (serif), Inter (sans-serif), JetBrains Mono (monospace)
- LocalStorage for data persistence

## File Structure

```
particledeck/
├── index.html          # Main application
├── css/
│   ├── main.css       # Global styles
│   ├── editor.css     # Editor panel styles
│   └── preview.css    # Preview area styles
├── js/
│   ├── app.js         # Main application logic
│   ├── slideManager.js # Slide data management
│   ├── particles.js   # Particle system
│   ├── shapes.js      # Shape definitions
│   └── markdown.js    # Markdown parser
└── README.md
```

## Browser Compatibility

Works best in modern browsers (Chrome, Firefox, Safari, Edge).

## License

MIT

## Credits

Inspired by the Dubai gold aesthetic and network intelligence presentations.

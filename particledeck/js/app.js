/**
 * ParticleDeck - Main Application
 */

class ParticleDeckApp {
    constructor() {
        this.slideManager = new SlideManager();
        this.particleSystem = null;
        this.currentlyEditing = false;

        this.init();
    }

    init() {
        // Initialize particle system
        this.particleSystem = new ParticleSystem('particleCanvas');

        // Setup event listeners
        this.setupSlideManagerEvents();
        this.setupUIEvents();
        this.setupKeyboardShortcuts();

        // Initial render
        this.renderSlideList();
        this.renderCurrentSlide();
        this.updatePreviewControls();
    }

    /**
     * Setup slide manager event listeners
     */
    setupSlideManagerEvents() {
        this.slideManager.on('slideAdded', () => {
            this.renderSlideList();
        });

        this.slideManager.on('slideDeleted', () => {
            this.renderSlideList();
            this.renderCurrentSlide();
            this.updatePreviewControls();
        });

        this.slideManager.on('slideUpdated', () => {
            this.renderSlideList();
            this.renderCurrentSlide();
        });

        this.slideManager.on('slideSelected', () => {
            this.renderSlideList();
            this.renderCurrentSlide();
            this.loadSlideIntoEditor();
            this.updatePreviewControls();
        });
    }

    /**
     * Setup UI event listeners
     */
    setupUIEvents() {
        // Add slide button
        document.getElementById('addSlideBtn').addEventListener('click', () => {
            this.slideManager.addSlide();
        });

        // Delete slide button
        document.getElementById('deleteSlideBtn').addEventListener('click', () => {
            if (confirm('Are you sure you want to delete this slide?')) {
                this.slideManager.deleteSlide(this.slideManager.currentSlideIndex);
            }
        });

        // Content editor
        const contentEditor = document.getElementById('contentEditor');
        contentEditor.addEventListener('input', () => {
            this.updateCurrentSlideContent();
        });

        // Shape selector
        const shapeSelector = document.getElementById('shapeSelector');
        shapeSelector.addEventListener('change', () => {
            this.updateCurrentSlideShape();
        });

        // Custom shape input
        const customShapeInput = document.getElementById('customShapeInput');
        customShapeInput.addEventListener('blur', () => {
            if (customShapeInput.value.trim() !== '') {
                // For now, just use the closest matching shape
                // In future, could use AI to interpret custom shapes
                alert('Custom shape descriptions coming soon! Using default shape for now.');
            }
        });

        // Alignment buttons
        document.querySelectorAll('.alignment-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                const alignment = btn.getAttribute('data-align');
                this.updateCurrentSlideAlignment(alignment);

                // Update active state
                document.querySelectorAll('.alignment-btn').forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
            });
        });

        // Preview navigation
        document.getElementById('prevBtn').addEventListener('click', () => {
            this.slideManager.prevSlide();
        });

        document.getElementById('nextBtn').addEventListener('click', () => {
            this.slideManager.nextSlide();
        });

        // Export button
        document.getElementById('exportBtn').addEventListener('click', () => {
            this.exportPresentation();
        });

        // Present mode button
        document.getElementById('presentBtn').addEventListener('click', () => {
            this.enterPresentMode();
        });
    }

    /**
     * Setup keyboard shortcuts
     */
    setupKeyboardShortcuts() {
        document.addEventListener('keydown', (e) => {
            // Don't trigger shortcuts when typing in editor
            if (e.target.tagName === 'TEXTAREA' || e.target.tagName === 'INPUT') {
                return;
            }

            // Arrow keys for navigation
            if (e.key === 'ArrowRight') {
                this.slideManager.nextSlide();
            } else if (e.key === 'ArrowLeft') {
                this.slideManager.prevSlide();
            }

            // Ctrl/Cmd + N for new slide
            if ((e.ctrlKey || e.metaKey) && e.key === 'n') {
                e.preventDefault();
                this.slideManager.addSlide();
            }

            // Delete key to delete slide (with confirmation)
            if (e.key === 'Delete' && !e.shiftKey) {
                if (confirm('Delete current slide?')) {
                    this.slideManager.deleteSlide(this.slideManager.currentSlideIndex);
                }
            }
        });
    }

    /**
     * Render slide list in sidebar
     */
    renderSlideList() {
        const slideList = document.getElementById('slideList');
        const slides = this.slideManager.getAllSlides();

        slideList.innerHTML = '';

        slides.forEach((slide, index) => {
            const slideItem = document.createElement('div');
            slideItem.className = 'slide-item';
            if (index === this.slideManager.currentSlideIndex) {
                slideItem.classList.add('active');
            }

            const slideNumber = document.createElement('div');
            slideNumber.className = 'slide-number';
            slideNumber.textContent = index + 1;

            const slidePreviewText = document.createElement('div');
            slidePreviewText.className = 'slide-preview-text';
            slidePreviewText.textContent = this.slideManager.getSlidePreview(slide);

            const shapeBadge = document.createElement('div');
            shapeBadge.className = 'slide-shape-badge';
            shapeBadge.textContent = slide.shape;

            slideItem.appendChild(slideNumber);
            slideItem.appendChild(slidePreviewText);
            slideItem.appendChild(shapeBadge);

            slideItem.addEventListener('click', () => {
                this.slideManager.selectSlide(index);
            });

            slideList.appendChild(slideItem);
        });
    }

    /**
     * Render current slide in preview
     */
    renderCurrentSlide() {
        const slide = this.slideManager.getCurrentSlide();
        if (!slide) return;

        const previewContent = document.getElementById('previewContent');

        // Parse markdown and render
        const html = MarkdownParser.parse(slide.content);
        previewContent.innerHTML = html;

        // Apply alignment
        previewContent.className = 'preview-content';
        previewContent.classList.add(`align-${slide.alignment}`);

        // Trigger fade-in animation
        setTimeout(() => {
            previewContent.classList.add('visible');
        }, 50);

        // Update particle shape
        this.particleSystem.transitionToShape(slide.shape);
    }

    /**
     * Load current slide data into editor
     */
    loadSlideIntoEditor() {
        const slide = this.slideManager.getCurrentSlide();
        if (!slide) return;

        document.getElementById('contentEditor').value = slide.content;
        document.getElementById('shapeSelector').value = slide.shape;

        // Update alignment buttons
        document.querySelectorAll('.alignment-btn').forEach(btn => {
            btn.classList.remove('active');
            if (btn.getAttribute('data-align') === slide.alignment) {
                btn.classList.add('active');
            }
        });
    }

    /**
     * Update current slide content
     */
    updateCurrentSlideContent() {
        const content = document.getElementById('contentEditor').value;
        this.slideManager.updateSlide(this.slideManager.currentSlideIndex, { content });
    }

    /**
     * Update current slide shape
     */
    updateCurrentSlideShape() {
        const shape = document.getElementById('shapeSelector').value;
        this.slideManager.updateSlide(this.slideManager.currentSlideIndex, { shape });
    }

    /**
     * Update current slide alignment
     */
    updateCurrentSlideAlignment(alignment) {
        this.slideManager.updateSlide(this.slideManager.currentSlideIndex, { alignment });
    }

    /**
     * Update preview controls
     */
    updatePreviewControls() {
        const prevBtn = document.getElementById('prevBtn');
        const nextBtn = document.getElementById('nextBtn');
        const counter = document.getElementById('slideCounter');

        const current = this.slideManager.currentSlideIndex + 1;
        const total = this.slideManager.slides.length;

        counter.textContent = `${current} / ${total}`;

        prevBtn.disabled = this.slideManager.currentSlideIndex === 0;
        nextBtn.disabled = this.slideManager.currentSlideIndex === this.slideManager.slides.length - 1;
    }

    /**
     * Export presentation as HTML
     */
    exportPresentation() {
        const slides = this.slideManager.getAllSlides();

        // Generate HTML similar to the example
        let html = `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>ParticleDeck Presentation</title>
    <link href="https://fonts.googleapis.com/css2?family=Fraunces:ital,opsz,wght@0,9..144,300;0,9..144,600;0,9..144,700;1,9..144,400&family=Inter:wght@300;400;500;600;700&family=JetBrains+Mono:wght@300;400;500&display=swap" rel="stylesheet">
    <style>
        ${this.getExportCSS()}
    </style>
</head>
<body>
    <canvas id="geo-canvas"></canvas>
    <div class="deck-container">
`;

        slides.forEach((slide, index) => {
            const alignClass = slide.alignment === 'left' ? 'text-left' :
                             slide.alignment === 'right' ? 'text-right' : '';
            const activeClass = index === 0 ? 'active' : '';
            const content = MarkdownParser.parse(slide.content);

            html += `
        <div class="slide ${alignClass} ${activeClass}" data-shape="${slide.shape}">
            ${content}
        </div>
`;
        });

        html += `
    </div>
    <div class="slide-counter" id="slide-counter">1 / ${slides.length}</div>
    <div class="nav-controls">
        <button class="arrow-btn" onclick="prevSlide()">←</button>
        <button class="arrow-btn" onclick="nextSlide()">→</button>
    </div>
    <script>${this.getExportJS(slides)}</script>
</body>
</html>`;

        // Download HTML file
        const blob = new Blob([html], { type: 'text/html' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'presentation.html';
        a.click();
        URL.revokeObjectURL(url);
    }

    /**
     * Get CSS for exported presentation
     */
    getExportCSS() {
        return `/* Styles from the example HTML - abbreviated for space */
        :root { --bg-color: #020408; --dubai-gold: #C2A25F; --text-white: #f0f2f5; --text-grey: #94a3b8; }
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: 'Inter', sans-serif; background: #020408; color: #f0f2f5; height: 100vh; overflow: hidden; display: flex; justify-content: center; align-items: center; }
        #geo-canvas { position: absolute; top: 0; left: 0; width: 100%; height: 100%; z-index: 1; }
        .deck-container { position: relative; z-index: 10; width: 92%; max-width: 1600px; height: 90vh; display: flex; flex-direction: column; justify-content: center; align-items: center; }
        .slide { display: none; opacity: 0; height: 100%; width: 100%; flex-direction: column; justify-content: center; align-items: center; text-align: center; transition: opacity 1.2s ease; }
        .slide.active { display: flex; opacity: 1; }
        .slide.text-left { align-items: flex-start; text-align: left; padding-left: 8%; }
        .slide.text-right { align-items: flex-end; text-align: right; padding-right: 8%; }
        h1 { font-family: 'Fraunces', serif; font-weight: 700; font-size: clamp(3.5rem, 7vw, 5.5rem); line-height: 1.05; margin-bottom: 1.5rem; }
        h2 { font-family: 'Fraunces', serif; font-weight: 600; font-size: clamp(2.2rem, 4.5vw, 3.8rem); color: var(--dubai-gold); margin-bottom: 1.5rem; }
        h3 { font-weight: 500; text-transform: uppercase; letter-spacing: 0.3em; font-size: 0.9rem; color: var(--text-grey); margin-bottom: 1rem; }
        p { font-size: clamp(1.1rem, 2vw, 1.4rem); line-height: 1.6; margin-bottom: 1.5rem; }
        strong { color: var(--dubai-gold); font-weight: 600; }
        .nav-controls { position: absolute; bottom: 30px; right: 40px; z-index: 50; display: flex; gap: 15px; }
        .arrow-btn { background: rgba(255,255,255,0.05); border: 1px solid rgba(194,162,95,0.3); color: var(--dubai-gold); width: 50px; height: 50px; border-radius: 50%; font-size: 1.2rem; cursor: pointer; }
        .arrow-btn:hover { background: var(--dubai-gold); color: #000; }
        .slide-counter { position: absolute; bottom: 30px; left: 40px; z-index: 50; font-family: 'JetBrains Mono', monospace; font-size: 0.7rem; color: var(--text-grey); opacity: 0.5; }`;
    }

    /**
     * Get JavaScript for exported presentation
     */
    getExportJS(slides) {
        // Include the shapes and particle system code
        return `
        // Shapes definitions (abbreviated)
        const shapes = ${JSON.stringify(slides.map(s => s.shape))};
        let currentSlide = 0;
        const slides = document.querySelectorAll('.slide');
        const counter = document.getElementById('slide-counter');

        function updateSlide() {
            slides.forEach((s, i) => {
                s.classList.remove('active');
                if (i === currentSlide) s.classList.add('active');
            });
            counter.innerText = (currentSlide + 1) + ' / ' + slides.length;
            updateGeometry(currentSlide);
        }

        function nextSlide() {
            if (currentSlide < slides.length - 1) {
                currentSlide++;
                updateSlide();
            }
        }

        function prevSlide() {
            if (currentSlide > 0) {
                currentSlide--;
                updateSlide();
            }
        }

        document.addEventListener('keydown', (e) => {
            if (e.key === 'ArrowRight') nextSlide();
            if (e.key === 'ArrowLeft') prevSlide();
        });

        // Particle system (simplified version)
        const canvas = document.getElementById('geo-canvas');
        const ctx = canvas.getContext('2d');
        let width = canvas.width = window.innerWidth;
        let height = canvas.height = window.innerHeight;
        let particles = [];
        const numParticles = 750;

        class Particle {
            constructor() {
                this.x = Math.random() * width;
                this.y = Math.random() * height;
                this.tx = this.x;
                this.ty = this.y;
                this.size = Math.random() * 0.8 + 0.5;
                this.driftX = (Math.random()-0.5)*0.2;
                this.driftY = (Math.random()-0.5)*0.2;
            }
            update() {
                this.x += (this.tx - this.x)*0.0525 + this.driftX;
                this.y += (this.ty - this.y)*0.0525 + this.driftY;
            }
            draw() {
                ctx.beginPath();
                ctx.arc(this.x, this.y, this.size, 0, Math.PI*2);
                ctx.fill();
            }
        }

        for(let i=0; i<numParticles; i++) particles.push(new Particle());

        function updateGeometry(index) {
            const shape = shapes[index];
            const cx = width/2, cy = height/2;
            particles.forEach((p, i) => {
                // Simplified shape calculations
                if(shape === 'sphere') {
                    const a=Math.random()*Math.PI*2, r=Math.random()*300;
                    p.tx=cx+Math.cos(a)*r; p.ty=cy+Math.sin(a)*r;
                } else if(shape === 'spiral') {
                    const a=i*0.3, r=i*1.5;
                    p.tx=cx+Math.cos(a)*r; p.ty=cy+Math.sin(a)*r;
                } else {
                    const a=Math.random()*Math.PI*2, r=Math.random()*300;
                    p.tx=cx+Math.cos(a)*r; p.ty=cy+Math.sin(a)*r;
                }
            });
        }

        function animate() {
            ctx.clearRect(0,0,width,height);
            ctx.globalCompositeOperation='screen';
            ctx.fillStyle='#C2A25F';
            particles.forEach(p => { p.update(); p.draw(); });

            const maxDist = 110;
            ctx.beginPath();
            ctx.lineWidth = 0.5;
            for(let i=0; i<particles.length; i++) {
                for(let j=i+1; j<particles.length; j++) {
                    const dx=particles[i].x-particles[j].x, dy=particles[i].y-particles[j].y;
                    const dist=Math.sqrt(dx*dx+dy*dy);
                    if(dist<maxDist) {
                        ctx.strokeStyle='rgba(194,162,95,'+(1-dist/maxDist)*0.12+')';
                        ctx.moveTo(particles[i].x,particles[i].y);
                        ctx.lineTo(particles[j].x,particles[j].y);
                    }
                }
            }
            ctx.stroke();
            ctx.globalCompositeOperation='source-over';
            requestAnimationFrame(animate);
        }

        updateGeometry(0);
        animate();
        `;
    }

    /**
     * Enter present mode (fullscreen)
     */
    enterPresentMode() {
        alert('Present mode coming soon! For now, export as HTML and open in a browser.');
    }
}

// Initialize app when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    window.particleDeckApp = new ParticleDeckApp();
});

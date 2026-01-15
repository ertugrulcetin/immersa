/**
 * Slide Manager - Handles slide data and operations
 */

class SlideManager {
    constructor() {
        this.slides = [];
        this.currentSlideIndex = -1;
        this.listeners = {
            slideAdded: [],
            slideDeleted: [],
            slideUpdated: [],
            slideSelected: []
        };

        // Load from localStorage or create default slide
        this.loadFromStorage();
        if (this.slides.length === 0) {
            this.addSlide();
        }
    }

    /**
     * Add a new slide
     */
    addSlide() {
        const slide = {
            id: this.generateId(),
            content: '# New Slide\n\nYour content here...',
            shape: 'sphere',
            alignment: 'center',
            createdAt: Date.now()
        };

        this.slides.push(slide);
        this.currentSlideIndex = this.slides.length - 1;
        this.saveToStorage();
        this.emit('slideAdded', slide);
        this.emit('slideSelected', slide);

        return slide;
    }

    /**
     * Delete a slide by index
     */
    deleteSlide(index) {
        if (this.slides.length === 1) {
            alert('Cannot delete the last slide!');
            return false;
        }

        if (index < 0 || index >= this.slides.length) {
            return false;
        }

        const deletedSlide = this.slides.splice(index, 1)[0];

        // Adjust current slide index
        if (this.currentSlideIndex >= this.slides.length) {
            this.currentSlideIndex = this.slides.length - 1;
        }

        this.saveToStorage();
        this.emit('slideDeleted', { slide: deletedSlide, index });

        // Select the new current slide
        if (this.slides.length > 0) {
            this.selectSlide(this.currentSlideIndex);
        }

        return true;
    }

    /**
     * Update slide content
     */
    updateSlide(index, updates) {
        if (index < 0 || index >= this.slides.length) {
            return false;
        }

        Object.assign(this.slides[index], updates);
        this.saveToStorage();
        this.emit('slideUpdated', { slide: this.slides[index], index });

        return true;
    }

    /**
     * Select a slide
     */
    selectSlide(index) {
        if (index < 0 || index >= this.slides.length) {
            return false;
        }

        this.currentSlideIndex = index;
        this.emit('slideSelected', this.slides[index]);

        return true;
    }

    /**
     * Get current slide
     */
    getCurrentSlide() {
        return this.slides[this.currentSlideIndex] || null;
    }

    /**
     * Get slide by index
     */
    getSlide(index) {
        return this.slides[index] || null;
    }

    /**
     * Get all slides
     */
    getAllSlides() {
        return this.slides;
    }

    /**
     * Navigate to next slide
     */
    nextSlide() {
        if (this.currentSlideIndex < this.slides.length - 1) {
            this.selectSlide(this.currentSlideIndex + 1);
            return true;
        }
        return false;
    }

    /**
     * Navigate to previous slide
     */
    prevSlide() {
        if (this.currentSlideIndex > 0) {
            this.selectSlide(this.currentSlideIndex - 1);
            return true;
        }
        return false;
    }

    /**
     * Generate unique ID
     */
    generateId() {
        return 'slide_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
    }

    /**
     * Save to localStorage
     */
    saveToStorage() {
        try {
            localStorage.setItem('particledeck_slides', JSON.stringify(this.slides));
            localStorage.setItem('particledeck_currentIndex', this.currentSlideIndex.toString());
        } catch (e) {
            console.error('Failed to save to localStorage:', e);
        }
    }

    /**
     * Load from localStorage
     */
    loadFromStorage() {
        try {
            const savedSlides = localStorage.getItem('particledeck_slides');
            const savedIndex = localStorage.getItem('particledeck_currentIndex');

            if (savedSlides) {
                this.slides = JSON.parse(savedSlides);
            }

            if (savedIndex) {
                this.currentSlideIndex = parseInt(savedIndex, 10);
            }
        } catch (e) {
            console.error('Failed to load from localStorage:', e);
        }
    }

    /**
     * Export slides as JSON
     */
    exportAsJSON() {
        return JSON.stringify(this.slides, null, 2);
    }

    /**
     * Import slides from JSON
     */
    importFromJSON(json) {
        try {
            const imported = JSON.parse(json);
            if (Array.isArray(imported)) {
                this.slides = imported;
                this.currentSlideIndex = 0;
                this.saveToStorage();
                return true;
            }
        } catch (e) {
            console.error('Failed to import JSON:', e);
        }
        return false;
    }

    /**
     * Event listener system
     */
    on(event, callback) {
        if (this.listeners[event]) {
            this.listeners[event].push(callback);
        }
    }

    emit(event, data) {
        if (this.listeners[event]) {
            this.listeners[event].forEach(callback => callback(data));
        }
    }

    /**
     * Get slide preview text (first 50 chars)
     */
    getSlidePreview(slide) {
        const stripped = MarkdownParser.stripMarkdown(slide.content);
        return stripped.substring(0, 50) + (stripped.length > 50 ? '...' : '');
    }
}

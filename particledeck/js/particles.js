/**
 * Particle System for Network Background
 */

class Particle {
    constructor(canvasWidth, canvasHeight) {
        this.x = Math.random() * canvasWidth;
        this.y = Math.random() * canvasHeight;
        this.tx = this.x; // target x
        this.ty = this.y; // target y
        this.size = Math.random() * 0.8 + 0.5;
        this.driftX = (Math.random() - 0.5) * 0.2;
        this.driftY = (Math.random() - 0.5) * 0.2;
    }

    update() {
        // Smooth interpolation to target position with drift
        this.x += (this.tx - this.x) * 0.0525 + this.driftX;
        this.y += (this.ty - this.y) * 0.0525 + this.driftY;
    }

    draw(ctx) {
        ctx.beginPath();
        ctx.arc(this.x, this.y, this.size, 0, Math.PI * 2);
        ctx.fill();
    }

    setTarget(tx, ty) {
        this.tx = tx;
        this.ty = ty;
    }
}

class ParticleSystem {
    constructor(canvasId, particleCount = 750) {
        this.canvas = document.getElementById(canvasId);
        this.ctx = this.canvas.getContext('2d');
        this.particleCount = particleCount;
        this.particles = [];
        this.width = 0;
        this.height = 0;
        this.currentShape = 'sphere';
        this.animationId = null;
        this.maxConnectionDistance = 110;

        this.init();
    }

    init() {
        this.resize();
        this.initParticles();
        this.animate();

        // Handle window resize
        window.addEventListener('resize', () => {
            this.resize();
            this.updateParticleTargets(this.currentShape);
        });
    }

    resize() {
        this.width = this.canvas.width = window.innerWidth - 400; // Account for sidebar
        this.height = this.canvas.height = window.innerHeight;
    }

    initParticles() {
        this.particles = [];
        for (let i = 0; i < this.particleCount; i++) {
            this.particles.push(new Particle(this.width, this.height));
        }
    }

    updateParticleTargets(shapeName) {
        this.currentShape = shapeName;
        const positions = calculateShapePositions(shapeName, this.particleCount, this.width, this.height);

        positions.forEach((pos, i) => {
            if (this.particles[i]) {
                this.particles[i].setTarget(pos.x, pos.y);
            }
        });

        // Adjust connection distance based on shape
        this.updateConnectionDistance(shapeName);
    }

    updateConnectionDistance(shapeName) {
        // Different shapes look better with different connection distances
        const distanceMap = {
            'sphere': 110,
            'ellipse': 110,
            'tree': 85,
            'sphere3d': 110,
            'wave': 90,
            'scatter': 120,
            'grid': 50,
            'clusters': 100,
            'spiral': 80,
            'curve': 95,
            'bars': 60,
            'network': 75,
            'galaxy': 70,
            'line': 100
        };

        this.maxConnectionDistance = distanceMap[shapeName] || 110;
    }

    drawConnections() {
        const maxDistSq = this.maxConnectionDistance * this.maxConnectionDistance;

        this.ctx.beginPath();
        this.ctx.lineWidth = 0.5;

        for (let i = 0; i < this.particles.length; i++) {
            for (let j = i + 1; j < this.particles.length; j++) {
                const dx = this.particles[i].x - this.particles[j].x;
                const dy = this.particles[i].y - this.particles[j].y;
                const distSq = dx * dx + dy * dy;

                if (distSq < maxDistSq) {
                    const alpha = (1 - distSq / maxDistSq) * 0.12;
                    this.ctx.strokeStyle = `rgba(194, 162, 95, ${alpha})`;
                    this.ctx.moveTo(this.particles[i].x, this.particles[i].y);
                    this.ctx.lineTo(this.particles[j].x, this.particles[j].y);
                }
            }
        }

        this.ctx.stroke();
    }

    animate() {
        // Clear canvas
        this.ctx.clearRect(0, 0, this.width, this.height);

        // Set composite mode for glow effect
        this.ctx.globalCompositeOperation = 'screen';

        // Draw particles
        this.ctx.fillStyle = '#C2A25F';
        this.particles.forEach(particle => {
            particle.update();
            particle.draw(this.ctx);
        });

        // Draw connections
        this.drawConnections();

        // Reset composite mode
        this.ctx.globalCompositeOperation = 'source-over';

        // Continue animation loop
        this.animationId = requestAnimationFrame(() => this.animate());
    }

    destroy() {
        if (this.animationId) {
            cancelAnimationFrame(this.animationId);
        }
    }

    // Transition to a new shape
    transitionToShape(shapeName) {
        this.updateParticleTargets(shapeName);
    }
}

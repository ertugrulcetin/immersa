/**
 * Shape Definitions for Particle Network
 * Each shape function calculates target positions for particles
 */

const Shapes = {
    /**
     * Sphere - particles arranged in a circular shape
     */
    sphere: (index, total, width, height) => {
        const cx = width / 2;
        const cy = height / 2;
        const angle = Math.random() * Math.PI * 2;
        const radius = Math.random() * 300;
        return {
            x: cx + Math.cos(angle) * radius,
            y: cy + Math.sin(angle) * radius
        };
    },

    /**
     * Ellipse - flattened sphere
     */
    ellipse: (index, total, width, height) => {
        const cx = width / 2;
        const cy = height / 2;
        const angle = (index / total) * Math.PI * 2;
        const radius = 250;
        const jitter = (Math.random() - 0.5) * 50;
        return {
            x: cx + Math.cos(angle) * radius + jitter,
            y: cy + Math.sin(angle) * (radius * 0.6) + jitter
        };
    },

    /**
     * Tree Structure - hierarchical branching pattern
     */
    tree: (index, total, width, height) => {
        const cx = width / 2;
        const t = Math.random();
        const ty = height * (0.9 - t * 0.8);

        let branchCount = 1;
        if (t > 0.2) branchCount = 3;
        if (t > 0.5) branchCount = 5;
        if (t > 0.8) branchCount = 9;

        const branchIndex = index % branchCount;
        const spread = (branchIndex - (branchCount - 1) / 2) * (t * width * 0.4);

        return {
            x: cx + spread + (Math.random() - 0.5) * 30,
            y: ty
        };
    },

    /**
     * 3D Sphere - sphere with depth projection
     */
    sphere3d: (index, total, width, height) => {
        const cx = width / 2;
        const cy = height / 2;
        const angle = Math.random() * Math.PI * 2;
        const beta = Math.random() * Math.PI;
        const radius = 250;

        return {
            x: cx + radius * Math.sin(beta) * Math.cos(angle),
            y: cy + radius * Math.cos(beta)
        };
    },

    /**
     * Wave - sinusoidal wave pattern
     */
    wave: (index, total, width, height) => {
        const cy = height / 2;
        return {
            x: (index / total) * width,
            y: cy + Math.sin(index * 0.1) * 100
        };
    },

    /**
     * Scatter - random distribution
     */
    scatter: (index, total, width, height) => {
        const cx = width / 2;
        const cy = height / 2;
        return {
            x: cx + (Math.random() - 0.5) * width,
            y: cy + (Math.random() - 0.5) * height
        };
    },

    /**
     * Grid - rectangular grid pattern
     */
    grid: (index, total, width, height) => {
        const cols = 25;
        const col = index % cols;
        const row = Math.floor(index / cols);
        return {
            x: (col * 40) + (width / 2 - 400),
            y: (row * 30) + 150
        };
    },

    /**
     * Clusters - multiple grouped circles
     */
    clusters: (index, total, width, height) => {
        const cy = height / 2;
        const cluster = index % 3;
        const cxOffset = (cluster - 1) * 300;
        const angle = Math.random() * Math.PI * 2;
        const radius = Math.random() * 100;

        return {
            x: (width / 2 + cxOffset) + Math.cos(angle) * radius,
            y: cy + Math.sin(angle) * radius
        };
    },

    /**
     * Spiral - logarithmic spiral
     */
    spiral: (index, total, width, height) => {
        const cx = width / 2;
        const cy = height / 2;
        const angle = index * 0.3;
        const radius = index * 1.5;

        return {
            x: cx + Math.cos(angle) * radius,
            y: cy + Math.sin(angle) * radius
        };
    },

    /**
     * Curve - parabolic curve
     */
    curve: (index, total, width, height) => {
        const t = index / total;
        const jitter = (Math.random() - 0.5) * 50;

        return {
            x: t * width,
            y: height - (Math.pow(t, 2) * height * 0.8) - 100 + jitter
        };
    },

    /**
     * Bars - bar chart visualization
     */
    bars: (index, total, width, height) => {
        const numBars = 8;
        const barIndex = index % numBars;
        const barWidth = 40;
        const spacing = 80;
        const startX = width * 0.05;
        const maxHeight = height * 0.8;
        const thisBarHeight = (barIndex + 1) / numBars * maxHeight;

        return {
            x: startX + (barIndex * spacing) + (Math.random() * barWidth),
            y: height - 50 - (Math.random() * thisBarHeight)
        };
    },

    /**
     * Network - network node layout
     */
    network: (index, total, width, height) => {
        const nodes = [
            { x: 0.5, y: 0.5 },
            { x: 0.3, y: 0.25 },
            { x: 0.7, y: 0.25 },
            { x: 0.3, y: 0.75 },
            { x: 0.7, y: 0.75 },
            { x: 0.2, y: 0.5 }
        ];

        const target = nodes[index % nodes.length];
        return {
            x: target.x * width + (Math.random() - 0.5) * (width * 0.35),
            y: target.y * height + (Math.random() - 0.5) * (height * 0.35)
        };
    },

    /**
     * Galaxy - spiral galaxy pattern
     */
    galaxy: (index, total, width, height) => {
        const cx = width / 2;
        const cy = height / 2;
        const t = index / total;
        const angle = t * Math.PI * 16;
        const radius = t * width * 0.45;

        return {
            x: cx + Math.cos(angle) * radius + (Math.random() - 0.5) * 40,
            y: cy + Math.sin(angle) * radius + (Math.random() - 0.5) * 40
        };
    },

    /**
     * Line - diagonal line
     */
    line: (index, total, width, height) => {
        const cx = width / 2;
        const cy = height / 2;
        const t = index / total;
        const dx = (t * 900) - 450;
        const dy = -(t * 450) + 225;

        return {
            x: cx + dx,
            y: cy + dy
        };
    }
};

/**
 * Get available shape names
 */
function getAvailableShapes() {
    return Object.keys(Shapes);
}

/**
 * Calculate particle positions for a given shape
 */
function calculateShapePositions(shapeName, particleCount, canvasWidth, canvasHeight) {
    const shapeFunction = Shapes[shapeName] || Shapes.sphere;
    const positions = [];

    for (let i = 0; i < particleCount; i++) {
        positions.push(shapeFunction(i, particleCount, canvasWidth, canvasHeight));
    }

    return positions;
}

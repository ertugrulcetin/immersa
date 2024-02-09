precision highp float;

uniform vec2 u_resolution;
uniform float u_time;// Uniform for time

void main() {
    // Calculate the normalized position of the point on the screen
    vec2 pos = gl_FragCoord.xy / u_resolution.xy;

    // Create a color gradient that goes from green to blue across the x-coordinate
    vec3 colorFrom = vec3(1.0, 0.0, 0.0);// Green
    vec3 colorTo = vec3(0.0, 0.0, 0.0);// Blue
    vec3 color = mix(colorFrom, colorTo, pos.x);// Interpolate between green and blue based on x position

    // Calculate glow intensity based on time and x-coordinate
    float glowIntensity = sin(u_time + pos.x * 3.14159265) * 0.5 + 0.5;// Oscillate between 0 and 1

    // Adjust the alpha component based on glow intensity
    float alpha = glowIntensity;

    // Set the color with the glow effect
    gl_FragColor = vec4(color, alpha);// The final color is a combination of the gradient, glow and adjusted alpha
}

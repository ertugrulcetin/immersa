precision highp float;

uniform float transparency;
uniform float dissolve;
uniform samplerCube skybox1;
uniform samplerCube skybox2;
uniform sampler2D noiseTexture;

varying vec3 vPosition;

void main(void) {
    vec3 direction = normalize(vPosition);
    vec3 fromSky = textureCube(skybox2, direction).rgb;
    vec3 toSky = textureCube(skybox1, direction).rgb;

    // Map the direction vector to a 2D noise texture space
    vec2 noiseCoord = direction.xy * 0.5 + 0.5;
    float noiseValue = texture2D(noiseTexture, noiseCoord).r;

    // Use the noise value to create a threshold for the dissolve effect
    float visibility = smoothstep(dissolve - 0.1, dissolve + 0.1, noiseValue);

    gl_FragColor = vec4(mix(fromSky, toSky, visibility), clamp(1.0 - transparency, 0.0, 1.0));
}

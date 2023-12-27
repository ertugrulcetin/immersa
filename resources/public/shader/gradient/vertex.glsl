#if defined(WEBGL2) || defines(WEBGPU)
precision highp sampler2DArray;
#endif
precision highp float;

attribute vec3 position;
attribute vec2 uv;

uniform mat4 worldViewProjection;

varying vec3 vPosition;
varying vec2 v_uv;

void main(void) {
    vPosition = position;
    gl_Position = worldViewProjection * vec4(position, 1.0);
    v_uv = uv;
}

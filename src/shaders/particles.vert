#version 120

uniform mat4 mModelViewProjection;
attribute vec3 vVertex;

void main() {
    vec4 position = mModelViewProjection * vec4(vVertex, 1.0);
    gl_Position = vec4(position);
}

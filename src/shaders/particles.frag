#version 120

uniform vec3 fColor;


void main() {
    gl_FragColor = vec4(fColor, 1.0);
}

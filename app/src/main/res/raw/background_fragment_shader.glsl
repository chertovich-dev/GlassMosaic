precision mediump float;

uniform sampler2D u_TextureUnit;

varying vec2 v_TextureCoordinates;
varying vec3 v_Pos;

void main()
{
    vec3 centre = vec3(0.5, 0.5, 0.01);
    vec4 centralColor = vec4(0.4, 0.4, 0.4, 1.0);
    vec4 outerColor = vec4(0.0, 0.0, 0.0, 1.0);
    float distance = clamp(distance(centre, v_Pos) / 0.8, 0.0, 1.0);  // 1.6

    vec4 backgroundColor = mix(centralColor, outerColor, distance);
    vec4 textureColor = texture2D(u_TextureUnit, v_TextureCoordinates);

    gl_FragColor = mix(textureColor, backgroundColor, 0.99);
}


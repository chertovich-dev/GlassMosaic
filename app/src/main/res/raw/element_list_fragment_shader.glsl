precision mediump float;

uniform vec4 u_Color;
uniform sampler2D u_TextureUnit;

uniform vec3 u_LightPos;
uniform bool u_Solved;

varying vec2 v_TextureCoordinates;
varying vec3 v_Pos;

void main()
{
    vec4 textureColor = mix(texture2D(u_TextureUnit, v_TextureCoordinates), u_Color, 0.85);

    // Расстояние от источника света до фрагмента
    float posDist = distance(u_LightPos, v_Pos);

    float light = max(1.0 / (0.8 + (posDist * posDist)), 0.9);

    // Делаем элемент черно-белым, если он не собран
    if (!u_Solved) {
        float value = (textureColor.r + textureColor.g + textureColor.b) / 3.0;
        textureColor.r = value;
        textureColor.g = value;
        textureColor.b = value;
    }

    gl_FragColor = vec4(textureColor.r * light, textureColor.g * light, textureColor.b * light, textureColor.a);
}
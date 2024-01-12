precision mediump float;

uniform vec4 u_Color;
uniform sampler2D u_TextureUnit;

uniform vec3 u_LightPos;
uniform vec3 u_ElementCenter;
uniform float u_ElementSize;

varying vec2 v_TextureCoordinates;
varying vec3 v_Pos;

void main()
{
    vec4 textureColor = mix(texture2D(u_TextureUnit, v_TextureCoordinates), u_Color, 0.85);

    // Расстояние от источника света до фрагмента
    float posDist = distance(u_LightPos, v_Pos);

    // Расстояние от источника света до центра элемента
    float centerDist = distance(u_LightPos, u_ElementCenter);

    // Радиус окружности, в которую вписан элемент. Половина диагонали квадрата.
    float radius = u_ElementSize * sqrt(2.0) / 2.0;

    // Расстояние от источника света до окружности. Если источник света входит в окружность, то получаем 0.0
    float circleDist = max(centerDist - radius, 0.0);

    vec3 lightVec = u_LightPos - u_ElementCenter;

    float moveCoeff = circleDist / centerDist;

    // Позиция локального света. Находится на окружности или внутри нее.
    vec3 localLightPos = u_LightPos - lightVec * moveCoeff;

    // Расстояние между фраментом и локальным источником света
    float localLightDist = distance(v_Pos, localLightPos);

    float localLight = 1.2 - (localLightDist / u_ElementSize) * 0.5;

    vec4 localColor = vec4(textureColor.r * localLight, textureColor.g * localLight, textureColor.b * localLight,
        textureColor.a);

    float light = max(1.0 / (0.8 + (posDist * posDist)), 0.9);

    gl_FragColor = vec4(localColor.r * light, localColor.g * light, localColor.b * light, localColor.a);
}
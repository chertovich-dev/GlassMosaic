uniform mat4 u_Matrix;

attribute vec4 a_Position;
attribute vec2 a_TextureCoordinates;

varying vec2 v_TextureCoordinates;
varying vec3 v_Pos;


void main()
{
    gl_Position = u_Matrix * a_Position;
    v_TextureCoordinates = a_TextureCoordinates;
    v_Pos = vec3(a_Position);
}
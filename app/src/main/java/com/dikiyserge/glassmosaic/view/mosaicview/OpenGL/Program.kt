package com.dikiyserge.glassmosaic.view.mosaicview.OpenGL

import android.opengl.GLES31.*
import com.dikiyserge.glassmosaic.data.ALFA_MAX
import com.dikiyserge.glassmosaic.data.b
import com.dikiyserge.glassmosaic.data.g
import com.dikiyserge.glassmosaic.data.r
import java.nio.FloatBuffer

private const val U_MATRIX = "u_Matrix"
private const val U_COLOR = "u_Color"
private const val U_LIGHT_POS = "u_LightPos"
private const val U_TEXTURE_UNIT = "u_TextureUnit"
private const val U_ELEMENT_CENTER = "u_ElementCenter"
private const val U_ELEMENT_SIZE = "u_ElementSize"
private const val U_SOLVED = "u_Solved"

private const val A_POSITION = "a_Position"
private const val A_TEXTURE_COORDINATES = "a_TextureCoordinates"

class Program : GLObject() {
    private var _uMatrix = 0
    private var _uColor = 0
    private var _uLightPos = 0
    private var _uTextureUnit = 0
    private var _uElementCenter = 0
    private var _uElementSize = 0
    private var _uSolved = 0

    private var _aPosition = 0
    private var _aTextureCoordinates = 0

    val uMatrix
        get() = _uMatrix

    val uColor
        get() = _uColor

    val uLightPos
        get() = _uLightPos

    val uTextureUnit
        get() = _uTextureUnit

    val uElementCenter
        get() = _uElementCenter

    val uElementSize
        get() = _uElementSize

    val uSolved
        get() = _uSolved

    val aPosition
        get() = _aPosition

    val aTextureCoordinates
        get() = _aTextureCoordinates

    private fun linkProgram(vertexShader: Shader, fragmentShader: Shader) {
        id = glCreateProgram()

        if (id != 0) {
            glAttachShader(id, vertexShader.id)
            glAttachShader(id, fragmentShader.id)
            glLinkProgram(id)

            val linkStatus = IntArray(1)
            glGetProgramiv(id, GL_LINK_STATUS, linkStatus, 0)

            if (linkStatus[0] == 0) {
                glDeleteProgram(id)
                id = 0
            }
        }
    }

    private fun getLocations() {
        _uMatrix = glGetUniformLocation(id, U_MATRIX)
        _uColor = glGetUniformLocation(id, U_COLOR)
        _uLightPos = glGetUniformLocation(id, U_LIGHT_POS)
        _uTextureUnit = glGetUniformLocation(id, U_TEXTURE_UNIT)
        _uElementCenter = glGetUniformLocation(id, U_ELEMENT_CENTER)
        _uElementSize = glGetUniformLocation(id, U_ELEMENT_SIZE)
        _uSolved = glGetUniformLocation(id, U_SOLVED)

        _aPosition = glGetAttribLocation(id, A_POSITION)
        _aTextureCoordinates = glGetAttribLocation(id, A_TEXTURE_COORDINATES)
    }

    private fun setVertexes(a: Int, data: FloatBuffer) {
        glVertexAttribPointer(a, 2, GL_FLOAT, false, 0, data)
        glEnableVertexAttribArray(a)
    }

    fun setShaders(vertexShader: Shader, fragmentShader: Shader) {
        linkProgram(vertexShader, fragmentShader)

        if (isValid) {
            getLocations()
        }
    }

    fun use() {
        glUseProgram(id)
    }

    fun setMatrix(projectionMatrix: FloatArray) {
        glUniformMatrix4fv(_uMatrix, 1, false, projectionMatrix, 0)
    }

    fun setPosition(data: FloatBuffer) {
        setVertexes(_aPosition, data)
    }

    fun setTextureCoord(data: FloatBuffer) {
        setVertexes(_aTextureCoordinates, data)
    }

    fun setColor(color: Int, alfa: Float = ALFA_MAX) {
        glUniform4f(_uColor, r(color), g(color), b(color), alfa)
    }

    fun setLightPos(x: Float, y: Float) {
        glUniform3f(_uLightPos, x, y, 0f)
    }

    fun setElementCenter(x: Float, y: Float) {
        glUniform3f(_uElementCenter, x, y, 0f)
    }

    fun setElementSize(size: Float) {
        glUniform1f(_uElementSize, size)
    }

    fun setSolved(solved: Boolean) {
        val value = if (solved) 1 else 0
        glUniform1i(_uSolved, value)
    }
}
package com.dikiyserge.glassmosaic.view.mosaicview.OpenGL

import android.content.Context
import android.opengl.GLES31.*

class Shader(context: Context, resourceId: Int, private val shaderType: Int): GLObject() {
    private val code: String

    init {
        code = context.resources.openRawResource(resourceId).bufferedReader().use {
            it.readText()
        }

        compile()
    }

    private fun compile() {
        id = glCreateShader(shaderType)

        if (id != 0) {
            glShaderSource(id, code)
            glCompileShader(id)

            val compileStatus = IntArray(1)
            glGetShaderiv(id, GL_COMPILE_STATUS, compileStatus, 0)

            if (compileStatus[0] == 0) {
                glDeleteShader(id)
                id = 0
            }
        }
    }
}
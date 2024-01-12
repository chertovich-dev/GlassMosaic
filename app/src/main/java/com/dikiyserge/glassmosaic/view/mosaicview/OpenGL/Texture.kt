package com.dikiyserge.glassmosaic.view.mosaicview.OpenGL

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

import android.opengl.GLES31.*
import android.opengl.GLUtils.texImage2D
import com.dikiyserge.glassmosaic.view.log

class Texture(context: Context, resourceId: Int) : GLObject() {
    private val bitmap: Bitmap

    init {
        val options = BitmapFactory.Options()
        options.inScaled = false

        bitmap = BitmapFactory.decodeResource(context.resources, resourceId, options)!!
    }

    fun load() {
        val ids = intArrayOf(0)
        glGenTextures(1, ids, 0)

        id = ids[0]

        if (isValid) {
            glBindTexture(GL_TEXTURE_2D, id)

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)

            texImage2D(GL_TEXTURE_2D, 0, bitmap, 0)

            glGenerateMipmap(GL_TEXTURE_2D)

            // !! Вызывает ошибку
            //bitmap.recycle()


            glBindTexture(GL_TEXTURE_2D, 0)
        }
    }
}
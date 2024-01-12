package com.dikiyserge.glassmosaic.view.mosaicview.OpenGL

import android.content.Context
import android.graphics.Color.*
import android.opengl.GLES31.*
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.dikiyserge.glassmosaic.R
import com.dikiyserge.glassmosaic.data.*
import com.dikiyserge.glassmosaic.view.log
import com.dikiyserge.glassmosaic.view.mosaicview.CENTER_X
import com.dikiyserge.glassmosaic.view.mosaicview.CENTER_Y
import com.dikiyserge.glassmosaic.view.mosaicview.MosaicMode
import com.dikiyserge.glassmosaic.view.mosaicview.MosaicView
import java.util.Date
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGL10.EGL_LEVEL
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.opengles.GL10

private const val UNIT = 1.0f

private const val COLOR_FRAME = "#808080"
private const val COLOR_STROKE = "#FFFFFF"

private const val FRAME_WIDTH = 2.0f
private const val STROKE_WIDTH = 2.0f
private const val STROKE_WIDTH_ACTIVE = 3.75f

private const val ELEMENT_ALFA = 0.92f

private const val PROJECT_MATRIX_SIZE = 16

class MosaicRenderer(val context: Context, private val mosaicView: MosaicView) : GLSurfaceView.Renderer,
    GLSurfaceView.EGLConfigChooser {

    private val rectangle = Rectangle(Point(-2.0, -2.0), Point(3.0, 3.0))

    private val elementProgram = Program()
    private val elementListProgram = Program()
    private val lineProgram = Program()
    private val backgroundProgram = Program()

    private val elementTexture = Texture(context, R.drawable.element_texture)
    private val backgroundTexture = Texture(context, R.drawable.background_texture)

    private val _projectionMatrix = FloatArray(PROJECT_MATRIX_SIZE)
    private val projectionMatrix = FloatArray(PROJECT_MATRIX_SIZE)

    private var _scale = 1f
    private var scale = 1f

    var activeElement: Element? = null

    private val frameColor = parseColor(COLOR_FRAME)
    private val strokeColor = parseColor(COLOR_STROKE)

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        glClearColor(0.0f, 0.0f, 0.0f, 1f)

        glDisable(GL_DITHER)

        loadElementShaders()
        loadLineShaders()
        loadBackgroundShaders()

        elementTexture.load()
        backgroundTexture.load()
    }

    private fun loadElementShaders() {
        val elementVertexShader = Shader(context, R.raw.element_vertex_shader, GL_VERTEX_SHADER)
        val elementFragmentShader = Shader(context, R.raw.element_fragment_shader, GL_FRAGMENT_SHADER)
        val elementListFragmentShader = Shader(context, R.raw.element_list_fragment_shader, GL_FRAGMENT_SHADER)

        if (elementVertexShader.isValid) {
            if (elementFragmentShader.isValid) {
                elementProgram.setShaders(elementVertexShader, elementFragmentShader)
            }

            if (elementListFragmentShader.isValid) {
                elementListProgram.setShaders(elementVertexShader, elementListFragmentShader)
            }
        }
    }

    private fun loadLineShaders() {
        val lineVertexShader = Shader(context, R.raw.line_vertex_shader, GL_VERTEX_SHADER)
        val lineFragmentShader = Shader(context, R.raw.line_fragment_shader, GL_FRAGMENT_SHADER)

        if (lineVertexShader.isValid && lineFragmentShader.isValid) {
            lineProgram.setShaders(lineVertexShader, lineFragmentShader)
        }
    }

    private fun loadBackgroundShaders() {
        val backgroundVertexShader = Shader(context, R.raw.background_vertex_shader, GL_VERTEX_SHADER)
        val backgroundFragmentShader = Shader(context, R.raw.background_fragment_shader, GL_FRAGMENT_SHADER)

        if (backgroundVertexShader.isValid && backgroundFragmentShader.isValid) {
            backgroundProgram.setShaders(backgroundVertexShader, backgroundFragmentShader)
        }
    }

    private fun isElementSolved(mosaic: Mosaic, element: Element): Boolean {
        return if (mosaic.state == MosaicState.NEW) {
            false
        } else {
            element.fixed
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        mosaicView.onSize(width, height)
        glViewport(0, 0, width, height)
    }

    override fun chooseConfig(egl: EGL10, display: EGLDisplay): EGLConfig {
        val attribs = intArrayOf(EGL_LEVEL, 0,
            EGL10.EGL_RENDERABLE_TYPE, 4,
            EGL10.EGL_COLOR_BUFFER_TYPE, EGL10.EGL_RGB_BUFFER,
            EGL10.EGL_RED_SIZE, 8,
            EGL10.EGL_GREEN_SIZE, 8,
            EGL10.EGL_BLUE_SIZE, 8,
            EGL10.EGL_DEPTH_SIZE, 16,
            EGL10.EGL_SAMPLE_BUFFERS, 1,
            EGL10.EGL_SAMPLES, 4,  // This is for 4x MSAA.
            EGL10.EGL_NONE)

        val configs = arrayOfNulls<EGLConfig>(1)
        val configCount = IntArray(1)

        egl.eglChooseConfig(display, attribs, configs, 1, configCount)
        return configs[0]!!
    }
    
    override fun onDrawFrame(gl: GL10?) {
        glClear(GL_COLOR_BUFFER_BIT)

        // Фиксируем позицию вида
        scale = _scale
        copyProjectMatrix()

        when (mosaicView.mode) {
            MosaicMode.MOSAIC -> {
                drawBackground()

                val elementsZ = mosaicView.mosaic.elementsZ

                // Фиксируем координаты элементов
                for (element in elementsZ) {
                    element.refreshVertexes()
                }

                drawFrames(elementsZ)
                drawElements(mosaicView.mosaic, elementsZ, true)
            }

            MosaicMode.LIST -> {
                drawBackground()

                val startIndex = mosaicView.startMosaicIndex
                val endIndex = mosaicView.endMosaicIndex

                for (i in startIndex..endIndex) {
                    if (i in 0 until mosaicView.mosaics.count) {
                        val mosaic = mosaicView.mosaics[i]

                        if (mosaic != null) {
                            val elementsZ = mosaic.elementsZ

                            // Фиксируем координаты элементов
                            for (element in elementsZ) {
                                element.refreshVertexesForList()
                            }

                            drawElements(mosaic, elementsZ, false)
                        }
                    }
                }
            }
        }
    }

    private fun drawBackground() {
        backgroundProgram.use()
        backgroundProgram.setMatrix(projectionMatrix)

        backgroundProgram.setPosition(rectangle.triangleVertexes.data)

        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, backgroundTexture.id)
        glUniform1i(backgroundProgram.uTextureUnit, 0)
        backgroundProgram.setTextureCoord(rectangle.textureVertexes.data)

        glDrawArrays(GL_TRIANGLES, 0, rectangle.triangleVertexes.size)
    }

    private fun drawFrames(elements: List<Element>) {
        lineProgram.use()
        lineProgram.setMatrix(projectionMatrix)

        for (element in elements) {
            drawFrame(element)
        }
    }

    private fun drawFrame(element: Element) {
        val vertexes = element.frameVertexes
        lineProgram.setPosition(vertexes.data)

        glLineWidth(FRAME_WIDTH * scale)

        lineProgram.setColor(frameColor)

        glDrawArrays(GL_LINE_LOOP, 0, vertexes.size)
    }

    private fun drawElements(mosaic: Mosaic, elements: List<Element>, drawStrokes: Boolean) {
        for (element in elements) {
            drawTriangles(mosaic, element)

            if (drawStrokes) {
                drawStroke(element)
            }
        }
    }

    private fun drawTriangles(mosaic: Mosaic, element: Element) {
        val program = when (mosaicView.mode) {
            MosaicMode.MOSAIC -> elementProgram
            MosaicMode.LIST -> elementListProgram
        }

        program.use()
        program.setMatrix(projectionMatrix)

        val triangleVertexes = element.triangleVertexes

        program.setPosition(triangleVertexes.data)
        program.setColor(element.color, ELEMENT_ALFA)

        when (mosaicView.mode) {
            MosaicMode.MOSAIC -> {
                val center = element.center
                program.setLightPos(CENTER_X.toFloat(), CENTER_Y.toFloat())
                program.setElementCenter(center.x.toFloat(), center.y.toFloat())
                program.setElementSize(element.size.toFloat())

            }

            MosaicMode.LIST -> {
                val lightPosX = (CENTER_X * element.itemSize + element.itemX).toFloat()
                val lightPosY = (CENTER_Y * element.itemSize + element.itemY).toFloat()
                program.setLightPos(lightPosX, lightPosY)
                program.setSolved(isElementSolved(mosaic, element))
            }
        }

        val textureVertexes = element.textureVertexes
        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, elementTexture.id)
        glUniform1i(program.uTextureUnit, 0)

        program.setTextureCoord(textureVertexes.data)

        glDrawArrays(GL_TRIANGLES, 0, triangleVertexes.size)
    }

    private fun drawStroke(element: Element) {
        lineProgram.use()
        lineProgram.setMatrix(projectionMatrix)

        val vertexes = element.strokeVertexes

        lineProgram.setPosition(vertexes.data)

        lineProgram.setColor(strokeColor)

        val width = if (element == activeElement) STROKE_WIDTH_ACTIVE else STROKE_WIDTH

        glLineWidth(width)

        glDrawArrays(GL_LINE_LOOP, 0, vertexes.size)
    }

    fun setProjectionMatrix(ratio: Float, scrollX: Float, scrollY: Float, scale: Float) {
        val left = scrollX
        val top = scrollY

        val right = left + UNIT / scale
        val bottom = top + ratio / scale

        Matrix.orthoM(_projectionMatrix, 0, left, right, bottom, top, -UNIT, UNIT)

        _scale = scale
    }

    private fun copyProjectMatrix() {
        for (i in 0 until PROJECT_MATRIX_SIZE) {
            projectionMatrix[i] = _projectionMatrix[i]
        }
    }
}
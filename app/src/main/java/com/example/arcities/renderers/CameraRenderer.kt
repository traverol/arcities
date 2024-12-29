package com.example.arcities.renderers

import android.opengl.GLES11Ext
import android.opengl.GLES20
import com.google.ar.core.Frame
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer


public class CameraRenderer {
    val textureId: Int
    private val program: Int
    private val positionHandle: Int
    private val texCoordHandle: Int
    private val vertexBuffer: FloatBuffer
    private val texCoordBuffer: FloatBuffer

    init {
        // Initialize buffers
        vertexBuffer = ByteBuffer.allocateDirect(QUAD_COORDS.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        vertexBuffer.put(QUAD_COORDS)
        vertexBuffer.position(0)

        texCoordBuffer = ByteBuffer.allocateDirect(QUAD_TEXCOORDS.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        texCoordBuffer.put(QUAD_TEXCOORDS)
        texCoordBuffer.position(0)

        // Create texture
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)

        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        // Create shader program
        program = GLRenderer.createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        positionHandle = GLES20.glGetAttribLocation(program, "position")
        texCoordHandle = GLES20.glGetAttribLocation(program, "texCoord")
    }

    fun draw() {
        GLES20.glUseProgram(program)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)

        // Set vertex attributes
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glEnableVertexAttribArray(texCoordHandle)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
    }

    companion object {
        private const val VERTEX_SHADER = "attribute vec4 position;\n" +
                "attribute vec2 texCoord;\n" +
                "varying vec2 texCoordVarying;\n" +
                "void main() {\n" +
                "    gl_Position = position;\n" +
                "    texCoordVarying = texCoord;\n" +
                "}"

        private const val FRAGMENT_SHADER = "#extension GL_OES_EGL_image_external : require\n" +
                "precision mediump float;\n" +
                "uniform samplerExternalOES texture;\n" +
                "varying vec2 texCoordVarying;\n" +
                "void main() {\n" +
                "    vec2 rotatedCoord  = vec2(texCoordVarying.y, 1.0 - texCoordVarying.x);\n" +
                "    gl_FragColor = texture2D(texture, rotatedCoord);\n" +
                "}"

        private val QUAD_COORDS = floatArrayOf(
            -1f, -1f,  // bottom left
            1f, -1f,  // bottom right
            -1f, 1f,  // top left
            1f, 1f   // top right
        )

        private val QUAD_TEXCOORDS = floatArrayOf(
            0f, 1f,    // bottom left
            1f, 1f,    // bottom right
            0f, 0f,    // top left
            1f, 0f     // top right
        )
    }
}

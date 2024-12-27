// CubeRenderer.kt
package com.example.arcities

import android.opengl.GLES20
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

public class CubeRenderer {
    private val program: Int
    private val positionHandle: Int
    private val colorHandle: Int
    private val mvpMatrixHandle: Int
    private val vertexBuffer: FloatBuffer
    private val colorBuffer: FloatBuffer
    private val mvpMatrix = FloatArray(16)
    private var modelMatrix = FloatArray(16)

    companion object {
        private const val VERTEX_SHADER = """
            uniform mat4 uMVPMatrix;
            attribute vec4 vPosition;
            attribute vec4 vColor;
            varying vec4 fragmentColor;
            void main() {
                gl_Position = uMVPMatrix * vPosition;
                fragmentColor = vColor;
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            varying vec4 fragmentColor;
            void main() {
                gl_FragColor = fragmentColor;
            }
        """

        // Define vertices for a cube
        private val CUBE_COORDS = floatArrayOf(
            // Front face
            -0.1f, -0.1f,  0.1f,  // Bottom-left
            0.1f, -0.1f,  0.1f,  // Bottom-right
            0.1f,  0.1f,  0.1f,  // Top-right
            -0.1f,  0.1f,  0.1f,  // Top-left
            // Back face
            -0.1f, -0.1f, -0.1f,  // Bottom-left
            0.1f, -0.1f, -0.1f,  // Bottom-right
            0.1f,  0.1f, -0.1f,  // Top-right
            -0.1f,  0.1f, -0.1f   // Top-left
        )

        // Colors for each vertex of the cube
        private val CUBE_COLORS = floatArrayOf(
            // Front face (red)
            1.0f, 0.0f, 0.0f, 1.0f,
            1.0f, 0.0f, 0.0f, 1.0f,
            1.0f, 0.0f, 0.0f, 1.0f,
            1.0f, 0.0f, 0.0f, 1.0f,
            // Back face (blue)
            0.0f, 0.0f, 1.0f, 1.0f,
            0.0f, 0.0f, 1.0f, 1.0f,
            0.0f, 0.0f, 1.0f, 1.0f,
            0.0f, 0.0f, 1.0f, 1.0f
        )

        // Order to draw vertices as triangles
        private val CUBE_DRAW_ORDER = shortArrayOf(
            0, 1, 2, 0, 2, 3,  // Front face
            5, 4, 7, 5, 7, 6,  // Back face
            4, 0, 3, 4, 3, 7,  // Left face
            1, 5, 6, 1, 6, 2,  // Right face
            3, 2, 6, 3, 6, 7,  // Top face
            4, 5, 1, 4, 1, 0   // Bottom face
        )
    }

    init {
        // Initialize vertex buffer
        vertexBuffer = ByteBuffer.allocateDirect(CUBE_COORDS.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(CUBE_COORDS)
                position(0)
            }

        // Initialize color buffer
        colorBuffer = ByteBuffer.allocateDirect(CUBE_COLORS.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(CUBE_COLORS)
                position(0)
            }

        // Create shaders and program
        program = createProgram()
        positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
        colorHandle = GLES20.glGetAttribLocation(program, "vColor")
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
    }

    fun updateModelMatrix(position: FloatArray, scale: Float) {
        Matrix.setIdentityM(modelMatrix, 0)
        //Matrix.translateM(modelMatrix, 0, 0f, 0f, 2f)
        Matrix.translateM(modelMatrix, 0, position[0], position[1], position[2])
        Matrix.scaleM(modelMatrix, 0, scale, scale, scale)
    }

    fun draw(viewMatrix: FloatArray, projectionMatrix: FloatArray) {
        GLES20.glUseProgram(program)

        // Calculate the MVP matrix
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

        // Set vertex attributes
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glVertexAttribPointer(colorHandle, 4, GLES20.GL_FLOAT, false, 0, colorBuffer)

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glEnableVertexAttribArray(colorHandle)

        // Set MVP matrix
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        // Draw the cube
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, CUBE_DRAW_ORDER.size,
            GLES20.GL_UNSIGNED_SHORT,
            ByteBuffer.allocateDirect(CUBE_DRAW_ORDER.size * 2)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer()
                .put(CUBE_DRAW_ORDER)
                .position(0)
        )

        // Cleanup
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(colorHandle)
    }

    private fun createProgram(): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)

        return GLES20.glCreateProgram().also { program ->
            GLES20.glAttachShader(program, vertexShader)
            GLES20.glAttachShader(program, fragmentShader)
            GLES20.glLinkProgram(program)
        }
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }

    fun release() {
        GLES20.glDeleteProgram(program)
    }
}
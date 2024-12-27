package com.example.arcities

import android.content.Context
import android.opengl.GLES30
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer

class ObjectRenderer {
    companion object {
        private const val VERTEX_SHADER = """#version 300 es
            uniform mat4 u_MVPMatrix;
            layout(location = 0) in vec4 a_Position;
            layout(location = 1) in vec4 a_Color;
            out vec4 v_Color;
            
            void main() {
                v_Color = a_Color;
                gl_Position = u_MVPMatrix * a_Position;
            }"""

        private const val FRAGMENT_SHADER = """#version 300 es
            precision mediump float;
            in vec4 v_Color;
            out vec4 fragColor;
            
            void main() {
                fragColor = v_Color;
            }"""

        private val CUBE_COORDS = floatArrayOf(
            // Front face
            -1.0f, -1.0f,  1.0f,  // 0
            1.0f, -1.0f,  1.0f,  // 1
            1.0f,  1.0f,  1.0f,  // 2
            -1.0f,  1.0f,  1.0f,  // 3

            // Back face
            -1.0f, -1.0f, -1.0f,  // 4
            1.0f, -1.0f, -1.0f,  // 5
            1.0f,  1.0f, -1.0f,  // 6
            -1.0f,  1.0f, -1.0f   // 7
        )

        private val CUBE_COLORS = floatArrayOf(
            // Front face (red)
            1.0f, 0.0f, 0.0f, 1.0f,
            1.0f, 0.0f, 0.0f, 1.0f,
            1.0f, 0.0f, 0.0f, 1.0f,
            1.0f, 0.0f, 0.0f, 1.0f,

            // Back face (green)
            0.0f, 1.0f, 0.0f, 1.0f,
            0.0f, 1.0f, 0.0f, 1.0f,
            0.0f, 1.0f, 0.0f, 1.0f,
            0.0f, 1.0f, 0.0f, 1.0f
        )

        private val CUBE_INDICES = intArrayOf(
            0, 1, 2, 2, 3, 0,  // Front face
            4, 5, 6, 6, 7, 4,  // Back face
            0, 4, 7, 7, 3, 0,  // Left face
            1, 5, 6, 6, 2, 1,  // Right face
            3, 2, 6, 6, 7, 3,  // Top face
            0, 1, 5, 5, 4, 0   // Bottom face
        )
    }

    private var vaoId = IntArray(1)
    private var vboIds = IntArray(3)
    private var programId: Int = 0
    private var mvpMatrixUniform: Int = 0
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    fun createOnGlThread(context: Context) {
        // Generate and bind VAO
        GLES30.glGenVertexArrays(1, vaoId, 0)
        GLES30.glBindVertexArray(vaoId[0])

        // Generate VBOs
        GLES30.glGenBuffers(3, vboIds, 0)

        // Vertex buffer
        val vertexBuffer = ByteBuffer.allocateDirect(CUBE_COORDS.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(CUBE_COORDS)
                position(0)
            }

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboIds[0])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertexBuffer.capacity() * 4,
            vertexBuffer, GLES30.GL_STATIC_DRAW)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 0, 0)
        GLES30.glEnableVertexAttribArray(0)

        // Color buffer
        val colorBuffer = ByteBuffer.allocateDirect(CUBE_COLORS.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(CUBE_COLORS)
                position(0)
            }

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboIds[1])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, colorBuffer.capacity() * 4,
            colorBuffer, GLES30.GL_STATIC_DRAW)
        GLES30.glVertexAttribPointer(1, 4, GLES30.GL_FLOAT, false, 0, 0)
        GLES30.glEnableVertexAttribArray(1)

        // Index buffer
        val indexBuffer = ByteBuffer.allocateDirect(CUBE_INDICES.size * 4)
            .order(ByteOrder.nativeOrder())
            .asIntBuffer()
            .apply {
                put(CUBE_INDICES)
                position(0)
            }

        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, vboIds[2])
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, indexBuffer.capacity() * 4,
            indexBuffer, GLES30.GL_STATIC_DRAW)

        // Create and link shaders
        val vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)

        programId = GLES30.glCreateProgram().also { program ->
            GLES30.glAttachShader(program, vertexShader)
            GLES30.glAttachShader(program, fragmentShader)
            GLES30.glLinkProgram(program)
        }

        mvpMatrixUniform = GLES30.glGetUniformLocation(programId, "u_MVPMatrix")

        // Initialize model matrix
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0f, 0f, -3f)

        // Cleanup
        GLES30.glBindVertexArray(0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, 0)
    }

    private fun loadShader(type: Int, shaderCode: String): Int =
        GLES30.glCreateShader(type).also { shader ->
            GLES30.glShaderSource(shader, shaderCode)
            GLES30.glCompileShader(shader)
        }

    fun draw(viewMatrix: FloatArray, projectionMatrix: FloatArray) {
        GLES30.glUseProgram(programId)
        GLES30.glBindVertexArray(vaoId[0])

        // Calculate MVP matrix
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

        // Set MVP matrix
        GLES30.glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0)

        // Enable depth testing
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)

        // Draw cube
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, CUBE_INDICES.size,
            GLES30.GL_UNSIGNED_INT, 0)

        // Cleanup
        GLES30.glBindVertexArray(0)
        GLES30.glUseProgram(0)
    }

    val textureId: Int get() = 0
}

package com.example.arcities.renderers


import android.opengl.GLES20
import android.opengl.Matrix
import com.google.ar.core.Pose
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.random.Random
class CarRenderer {
    private val program: Int
    private val positionHandle: Int
    private val mvpMatrixHandle: Int
    private val colorHandle: Int
    private val vertexBuffer: FloatBuffer
    private val indexBuffer: ShortBuffer
    private val mvpMatrix = FloatArray(16)
    private val cars = mutableListOf<Car>()

    data class Car(
        val position: Pose,
        val color: FloatArray = randomCarColor()
    )

    init {
        program = GLRenderer.createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        positionHandle = GLES20.glGetAttribLocation(program, "position")
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        colorHandle = GLES20.glGetUniformLocation(program, "color")

        vertexBuffer = createCubeVertices()
        indexBuffer = createCubeIndices()
    }

    private fun createCubeVertices(): FloatBuffer {
        val size = 0.03f

        val vertices = floatArrayOf(
            // Front
            -size, -size,  size,  // 0. left-bottom-front
            size, -size,  size,  // 1. right-bottom-front
            size,  size,  size,  // 2. right-top-front
            -size,  size,  size,  // 3. left-top-front
            // Back
            -size, -size, -size,  // 4. left-bottom-back
            size, -size, -size,  // 5. right-bottom-back
            size,  size, -size,  // 6. right-top-back
            -size,  size, -size   // 7. left-top-back
        )

        return ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(vertices)
                position(0)
            }
    }

    private fun createCubeIndices(): ShortBuffer {
        val indices = shortArrayOf(
            0, 1, 2, 0, 2, 3,  // Front
            1, 5, 6, 1, 6, 2,  // Right
            5, 4, 7, 5, 7, 6,  // Back
            4, 0, 3, 4, 3, 7,  // Left
            3, 2, 6, 3, 6, 7,  // Top
            4, 5, 1, 4, 1, 0   // Bottom
        )

        return ByteBuffer.allocateDirect(indices.size * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
            .apply {
                put(indices)
                position(0)
            }
    }

    companion object {
        private const val MAX_CARS = 20
        private val CAR_COLORS = listOf(
            floatArrayOf(1.0f, 0.0f, 0.0f, 1.0f),  // Red
            floatArrayOf(0.0f, 1.0f, 0.0f, 1.0f),   // Green
            floatArrayOf(0.0f, 0.0f, 1.0f, 1.0f),  // Blue
        )

        private fun randomCarColor(): FloatArray {
            return CAR_COLORS[Random.nextInt(CAR_COLORS.size)]
        }

        private const val VERTEX_SHADER = """
            uniform mat4 uMVPMatrix;
            attribute vec4 position;
            void main() {
                gl_Position = uMVPMatrix * position;
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec4 color;
            void main() {
                gl_FragColor = color;
            }
        """
    }

    fun placeCarAtHit(pose: Pose): Boolean {
        if (cars.size >= MAX_CARS) {
            return false
        }
        cars.add(Car(pose))
        return true
    }

    fun draw(viewMatrix: FloatArray, projectionMatrix: FloatArray) {
        GLES20.glUseProgram(program)

        // Enable depth test
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        cars.forEach { car ->
            val modelMatrix = FloatArray(16)
            car.position.toMatrix(modelMatrix, 0)

            // Calculate MVP matrix
            Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
            Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

            // Set the color and MVP matrix
            GLES20.glUniform4fv(colorHandle, 1, car.color, 0)
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

            // Reset buffers position
            vertexBuffer.position(0)
            indexBuffer.position(0)

            // Set up vertex attributes
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)
            GLES20.glEnableVertexAttribArray(positionHandle)

            // Draw the cube
            GLES20.glDrawElements(
                GLES20.GL_TRIANGLES,       // mode
                36,                        // count
                GLES20.GL_UNSIGNED_SHORT,  // type
                indexBuffer as Buffer      // indices
            )
        }

        // Disable depth test
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
    }
}
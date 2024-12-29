package com.example.arcities.renderers


import android.opengl.GLES20
import android.opengl.Matrix
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class PlaneRenderer {
    private val program: Int
    private val positionHandle: Int
    private val mvpMatrixHandle: Int
    private val colorHandle: Int
    private val mvpMatrix = FloatArray(16)
    private val planes = mutableMapOf<Plane, FloatBuffer>()

    companion object {
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

    init {
        program = GLRenderer.createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        positionHandle = GLES20.glGetAttribLocation(program, "position")
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        colorHandle = GLES20.glGetUniformLocation(program, "color")
    }

    fun trackingPlanes(planes: MutableCollection<Plane>) {
        for (plane in planes) {
            if (plane.type == Plane.Type.HORIZONTAL_UPWARD_FACING &&
                plane.trackingState == TrackingState.TRACKING) {
                if (!this.planes.containsKey(plane)) {
                    onPlaneAdded(plane)
                }
            }
        }
    }

    fun onPlaneAdded(plane: Plane) {
        if (plane.type == Plane.Type.HORIZONTAL_UPWARD_FACING) {
            val vertices = createPlaneVertices(plane)
            planes[plane] = createVertexBuffer(vertices)
        }
    }

    private fun createPlaneVertices(plane: Plane): FloatArray {
        val halfExtentX = plane.extentX / 2.0f
        val halfExtentZ = plane.extentZ / 2.0f

        return floatArrayOf(
            -halfExtentX, 0.0f, -halfExtentZ,  // bottom left
            halfExtentX, 0.0f, -halfExtentZ,   // bottom right
            halfExtentX, 0.0f, halfExtentZ,    // top right
            -halfExtentX, 0.0f, halfExtentZ    // top left
        )
    }

    fun onPlaneRemoved(plane: Plane) {
        planes.remove(plane)
    }

    fun drawPlane(viewMatrix: FloatArray, projectionMatrix: FloatArray) {
        planes.forEach { (plane, vertexBuffer) ->
            if (plane.trackingState == TrackingState.TRACKING) {
                draw(viewMatrix, projectionMatrix, plane, vertexBuffer)
            }
        }
    }

    private fun createVertexBuffer(vertices: FloatArray): FloatBuffer {
        return ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(vertices)
                position(0)
            }
    }

    fun draw(viewMatrix: FloatArray, projectionMatrix: FloatArray, plane: Plane, vertexBuffer: FloatBuffer) {
        val modelMatrix = FloatArray(16)
        plane.centerPose.toMatrix(modelMatrix, 0)

        // Scale to match plane size
        val scaleX = plane.extentX
        val scaleZ = plane.extentZ
        Matrix.scaleM(modelMatrix, 0, scaleX, 1f, scaleZ)

        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

        GLES20.glUseProgram(program)

        vertexBuffer.let { buffer ->
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, buffer)
            GLES20.glEnableVertexAttribArray(positionHandle)

            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
            GLES20.glUniform4f(colorHandle, 1.0f, 1.0f, 0.0f, 0.2f)

            GLES20.glEnable(GLES20.GL_BLEND)
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4)
            GLES20.glDisable(GLES20.GL_BLEND)

            GLES20.glDisableVertexAttribArray(positionHandle)
        }
    }
}
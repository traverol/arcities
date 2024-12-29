package com.example.arcities.renderers

import android.opengl.GLES20
import android.opengl.Matrix
import com.google.ar.core.Plane
import com.google.ar.core.Pose
import com.google.ar.core.TrackingState
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.random.Random

class BuildingRenderer {
    private val MAX_PLANES = 5
    private val BUILDINGS_PER_PLANE = 10
    private val program: Int
    private val positionHandle: Int
    private val mvpMatrixHandle: Int
    private val colorHandle: Int
    private val mvpMatrix = FloatArray(16)
    private val vertexBuffer = createCubeVertices()
    private var rotationAngle = 0f
    private val rotationMatrix = FloatArray(16)
    private val planeBuildings = mutableMapOf<Plane, MutableList<Building>>()

    data class Building(
        val x: Float,
        val z: Float,
        val width: Float,
        val height: Float,
        val depth: Float,
        val color: FloatArray,
        val pose: Pose
    )

    init {
        program = GLRenderer.createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        positionHandle = GLES20.glGetAttribLocation(program, "position")
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        colorHandle = GLES20.glGetUniformLocation(program, "color")
    }

    private fun randomColor(): FloatArray {
        return floatArrayOf(
            Math.random().toFloat(),
            Math.random().toFloat(),
            Math.random().toFloat(),
            1.0f
        )
    }

    private fun randomHeight(): Float {
        return 0.2f + Math.random().toFloat() * 0.9f
    }

    private fun createBuildingsForPlane(plane: Plane) {
        if (plane.type != Plane.Type.HORIZONTAL_UPWARD_FACING ||
            plane.trackingState != TrackingState.TRACKING) {
            return
        }

        val buildingWidth = 0.08f
        val buildingDepth = 0.08f
        val buildingsForPlane = mutableListOf<Building>()
        val planePose = plane.centerPose

        // Get plane boundaries, to draw buildings within
        val extentX = plane.extentX
        val extentZ = plane.extentZ

        for (i in 0 until BUILDINGS_PER_PLANE) {
            val x = (Random.nextFloat() - 0.5f) * extentX * 0.8f  // 80% of plane width
            val z = (Random.nextFloat() - 0.5f) * extentZ * 0.8f  // 80% of plane length

            // Create pose relative to plane center
            val buildingPose = Pose(
                floatArrayOf(
                    planePose.tx() + x,
                    planePose.ty(),
                    planePose.tz() + z
                ),
                planePose.rotationQuaternion
            )

            if (plane.isPoseInPolygon(buildingPose)) {
                buildingsForPlane.add(
                    Building(
                        x = x,
                        z = z,
                        width = buildingWidth,
                        height = randomHeight(),
                        depth = buildingDepth,
                        color = randomColor(),
                        pose = buildingPose
                    )
                )
            }
        }

        planeBuildings[plane] = buildingsForPlane
        processedPlanes.add(plane)
    }

    private fun createCubeVertices(): FloatBuffer {
        val vertices = floatArrayOf(
            // Front face
            -0.5f, 0f, 0.5f,     // front-bottom-left
            0.5f, 0f, 0.5f,      // front-bottom-right
            -0.5f, 1f, 0.5f,     // front-top-left
            0.5f, 1f, 0.5f,      // front-top-right

            // Right face
            0.5f, 0f, 0.5f,      // right-bottom-front
            0.5f, 0f, -0.5f,     // right-bottom-back
            0.5f, 1f, 0.5f,      // right-top-front
            0.5f, 1f, -0.5f,     // right-top-back

            // Back face
            0.5f, 0f, -0.5f,     // back-bottom-right
            -0.5f, 0f, -0.5f,    // back-bottom-left
            0.5f, 1f, -0.5f,     // back-top-right
            -0.5f, 1f, -0.5f,    // back-top-left

            // Left face
            -0.5f, 0f, -0.5f,    // left-bottom-back
            -0.5f, 0f, 0.5f,     // left-bottom-front
            -0.5f, 1f, -0.5f,    // left-top-back
            -0.5f, 1f, 0.5f,     // left-top-front

            // Bottom face
            -0.5f, 0f, -0.5f,    // bottom-left-back
            0.5f, 0f, -0.5f,     // bottom-right-back
            -0.5f, 0f, 0.5f,     // bottom-left-front
            0.5f, 0f, 0.5f,      // bottom-right-front

            // Top face
            -0.5f, 1f, 0.5f,     // top-left-front
            0.5f, 1f, 0.5f,      // top-right-front
            -0.5f, 1f, -0.5f,    // top-left-back
            0.5f, 1f, -0.5f      // top-right-back
        )

        return ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(vertices)
                position(0)
            }
    }

    fun trackPlanes(planes: Collection<Plane>) {
        if (processedPlanes.size >= MAX_PLANES) {
            return
        }

        for (plane in planes) {
            if (processedPlanes.contains(plane)) {
                continue
            }
            if (plane.trackingState == TrackingState.TRACKING) {
                createBuildingsForPlane(plane)
            }
        }
    }

    fun draw(viewMatrix: FloatArray, projectionMatrix: FloatArray) {
        GLES20.glUseProgram(program)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        planeBuildings.values.flatten().forEach { building ->
            val modelMatrix = FloatArray(16)
            building.pose.toMatrix(modelMatrix, 0)

            Matrix.setIdentityM(rotationMatrix, 0)
            Matrix.rotateM(rotationMatrix, 0, rotationAngle, 0f, 1f, 0f)

            Matrix.multiplyMM(modelMatrix, 0, modelMatrix, 0, rotationMatrix, 0)
            Matrix.translateM(modelMatrix, 0, building.x, 0f, building.z)
            Matrix.scaleM(modelMatrix, 0, building.width, building.height, building.depth)

            Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
            Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)
            GLES20.glEnableVertexAttribArray(positionHandle)
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
            GLES20.glUniform4fv(colorHandle, 1, building.color, 0)

            vertexBuffer.position(0)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 24)
        }
    }

    companion object {

        public val processedPlanes = mutableSetOf<Plane>()

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
}
package com.example.arcities

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


public class GLRenderer(private var context: Context) : GLSurfaceView.Renderer {
    private lateinit var cameraRenderer: CameraRenderer
    private lateinit var arSession: Session
    //private lateinit var planeRenderer: PlaneRenderer
    //private lateinit var objectRenderer: ObjectRenderer
    private lateinit var cubeRenderer: CubeRenderer

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        try {
            // Initialize renderers
            cameraRenderer = CameraRenderer()
            //planeRenderer = PlaneRenderer()
            //objectRenderer = ObjectRenderer()
            //objectRenderer.createOnGlThread(context)

            cubeRenderer = CubeRenderer()
            //cubeRenderer.createOnGlThread(context)

            // Set camera texture name
            arSession.setCameraTextureName(cameraRenderer.textureId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create renderers", e)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        // Set viewport
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        // Clear buffers
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT or GLES20.GL_COLOR_BUFFER_BIT)

        try {
            // Update session and get current frame
            val frame = arSession.update()
            val camera = frame.camera

            // Draw background camera texture
            cameraRenderer.draw(frame)


            // Get projection matrix
            val projectionMatrix = FloatArray(16)
            camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100.0f)


            // Get view matrix
            val viewMatrix = FloatArray(16)
            camera.getViewMatrix(viewMatrix, 0)


            // Draw all tracked planes
            val planes = arSession.getAllTrackables(
                Plane::class.java
            )

            GLES20.glEnable(GLES20.GL_DEPTH_TEST)

            for (plane in planes) {
                if (plane.trackingState == TrackingState.TRACKING) {
                   //planeRenderer!!.draw(viewMatrix, projectionMatrix, plane)
                   //objectRenderer.draw(viewMatrix, projectionMatrix)
                    //cubeRenderer.updateModelMatrix(plane.centerPose.translation, 100.0f)
                    val planeCenterPose = plane.centerPose
                    val cubePosition = floatArrayOf(
                        planeCenterPose.tx(),
                        planeCenterPose.ty(),
                        planeCenterPose.tz()
                    )
                    cubeRenderer.updateModelMatrix(cubePosition, 1.0f)
                    cubeRenderer.draw(viewMatrix, projectionMatrix)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception on the OpenGL thread", e)
        } finally {
            GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        }
    }

    fun setSession(session: Session) {
        arSession = session
    }

    // Clean up resources
    fun cleanup() {

    }

    companion object {
        private const val TAG = "GLRenderer"

        // Helper method to check OpenGL errors
        fun checkGLError(tag: String?, label: String) {
            var error: Int
            while ((GLES20.glGetError().also { error = it }) != GLES20.GL_NO_ERROR) {
                Log.e(tag, "$label: glError $error")
                throw RuntimeException("$label: glError $error")
            }
        }

        fun loadShader(type: Int, shaderCode: String): Int {
            val shader = GLES20.glCreateShader(type)
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)

            GLRenderer.checkGLError(TAG, "Shader compilation")
            return shader
        }

        fun createProgram(vertexSource: String, fragmentSource: String): Int {
            val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource)
            val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)

            val program = GLES20.glCreateProgram()
            GLES20.glAttachShader(program, vertexShader)
            GLES20.glAttachShader(program, fragmentShader)
            GLES20.glLinkProgram(program)

            checkGLError(TAG, "Program creation")
            return program
        }
    }
}

package com.example.arcities.renderers

import android.app.Activity
import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import com.google.ar.core.Camera
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

public class GLRenderer(private var context: Context) : GLSurfaceView.Renderer {
    private lateinit var cameraRenderer: CameraRenderer
    private var arSession: Session? = null
    //private lateinit var planeRenderer: PlaneRenderer
    private lateinit var buildingRenderer: BuildingRenderer
    private lateinit var carRenderer: CarRenderer
    private var buildingPlane: Plane? = null
    private var currentToast: Toast? = null
    private var tap: MotionEvent? = null

    init {
        Log.d(TAG, "GLRenderer initialized with context: ${context.javaClass.simpleName}")
    }

    private fun showToastMessage(message: String) {
        Log.d(TAG, "Showing toast message: $message")
        (context as? Activity)?.runOnUiThread {
            currentToast?.cancel()
            currentToast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
            currentToast?.show()
        }
    }

    fun onTap(event: MotionEvent) {
        Log.d(TAG, "Tap received at x: ${event.x}, y: ${event.y}")
        tap = event
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.i(TAG, "Surface created")
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        try {
            Log.d(TAG, "Initializing renderers")
            cameraRenderer = CameraRenderer()
            //planeRenderer = PlaneRenderer()
            buildingRenderer = BuildingRenderer()
            carRenderer = CarRenderer()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to create renderers", e)
            throw RuntimeException("Surface creation failed", e)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.i(TAG, "Surface changed: width=$width, height=$height")
        GLES20.glViewport(0, 0, width, height)
        showToastMessage("scan surface to find plane")
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT or GLES20.GL_COLOR_BUFFER_BIT)

        try {
            arSession?.setCameraTextureName(CameraRenderer.textureId)
            val frame = arSession?.update()!!
            val camera = frame.camera


            if (camera.trackingState != TrackingState.TRACKING) {
                Log.w(TAG, "Camera not tracking. Current state: ${camera.trackingState}")
                return
            }

            val projectionMatrix = FloatArray(16)
            val viewMatrix = FloatArray(16)
            camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100.0f)
            camera.getViewMatrix(viewMatrix, 0)

            val planes = arSession?.getAllTrackables<Plane>(Plane::class.java)!!
            Log.v(TAG, "Found ${planes.size} trackable planes")

            //planeRenderer.trackingPlanes(planes)
            buildingRenderer.trackPlanes(planes)

            if (buildingPlane == null) {
                buildingPlane = planes.firstOrNull { it.type == Plane.Type.HORIZONTAL_UPWARD_FACING }
                if (buildingPlane != null) {
                    Log.i(TAG, "Found horizontal upward facing plane")
                }
            }

            if (tap != null) {
                Log.d(TAG, "Processing tap event")
                handleTap(frame, camera, tap)
                tap = null
            }

            cameraRenderer.draw()

            GLES20.glEnable(GLES20.GL_DEPTH_TEST)
            //planeRenderer.drawPlane(viewMatrix, projectionMatrix)
            buildingRenderer.draw(viewMatrix, projectionMatrix)
            carRenderer.draw(viewMatrix, projectionMatrix)

            frame.acquireCameraImage().close()

        } catch (e: Exception) {
            Log.e(TAG, "Exception on the OpenGL thread", e)
        } finally {
            GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        }
    }

    fun setSession(session: Session) {
        Log.i(TAG, "Setting AR session")
        arSession = session
    }

    private fun handleTap(frame: Frame, camera: Camera, event: MotionEvent?) {
        if (event == null) {
            Log.w(TAG, "Tap event is null")
            return
        }
        if (camera.trackingState != TrackingState.TRACKING) {
            Log.w(TAG, "Camera not tracking during tap handling")
            return
        }

        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels.toFloat()
        val screenHeight = displayMetrics.heightPixels.toFloat()

        Log.d(TAG, "Processing tap at x: ${event.x}, y: ${event.y} (screen: ${screenWidth}x${screenHeight})")

        val hitResultList = frame.hitTest(event.x, event.y)
        Log.d(TAG, "Hit test returned ${hitResultList.size} results")

        val firstHitResult = hitResultList.firstOrNull { hit ->
            val trackable = hit.trackable
            when (trackable) {
                is Plane -> {
                    val isValid = trackable.isPoseInPolygon(hit.hitPose) && buildingPlane == trackable
                    Log.v(TAG, "Checking plane hit - isPoseInPolygon: ${trackable.isPoseInPolygon(hit.hitPose)}, isMatchingPlane: ${buildingPlane == trackable}")
                    isValid
                }
                else -> false
            }
        }

        if (firstHitResult != null) {
            Log.d(TAG, "Valid hit result found, attempting to place car")
            if (carRenderer.placeCarAtHit(firstHitResult.hitPose)) {
                showToastMessage("Car placed")
            } else {
                showToastMessage("Max cars reached")
            }
        } else {
            Log.d(TAG, "No valid hit result, attempting alternative placement")
            CoroutineScope(Dispatchers.Default).launch {
                placeObjectAtTouchPosition(frame, event.x, event.y, screenWidth.toInt(), screenHeight.toInt())?.let { pose ->
                    withContext(Dispatchers.Main) {
                        Log.d(TAG, "Alternative placement successful at position: (${pose.tx()}, ${pose.ty()}, ${pose.tz()})")
                        showToastMessage("car placed")
                        carRenderer.placeCarAtHit(pose)
                    }
                } ?: Log.w(TAG, "Failed to find valid placement position")
            }
        }
    }

    //https://github.com/googlecreativelab/ar-drawing-java/blob/master/app/src/main/java/com/googlecreativelab/drawar/rendering/LineUtils.java
    private suspend fun placeObjectAtTouchPosition(frame: Frame, x: Float, y: Float, width: Int, height: Int): Pose? {
        Log.d(TAG, "Attempting to place object at touch position: ($x, $y)")

        if (BuildingRenderer.processedPlanes.isEmpty()) {
            Log.w(TAG, "No processed planes available for object placement")
            return null
        }

        val camera = frame.camera
        val viewMatrix = FloatArray(16)
        val projectionMatrix = FloatArray(16)
        camera.getViewMatrix(viewMatrix, 0)
        camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100.0f)

        val viewProjectionMatrix = FloatArray(16)
        Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        val ndcX = x * 2f / width - 1f
        val ndcY = 1f - y * 2f / height
        Log.v(TAG, "NDC coordinates: ($ndcX, $ndcY)")

        val nearPoint = floatArrayOf(ndcX, ndcY, -1f, 1f)
        val farPoint = floatArrayOf(ndcX, ndcY, 1f, 1f)

        val invertedViewProjection = FloatArray(16)
        if (!Matrix.invertM(invertedViewProjection, 0, viewProjectionMatrix, 0)) {
            Log.e(TAG, "Failed to invert view projection matrix")
            return null
        }

        val worldNear = FloatArray(4)
        val worldFar = FloatArray(4)
        Matrix.multiplyMV(worldNear, 0, invertedViewProjection, 0, nearPoint, 0)
        Matrix.multiplyMV(worldFar, 0, invertedViewProjection, 0, farPoint, 0)

        for (i in 0..2) {
            worldNear[i] /= worldNear[3]
            worldFar[i] /= worldFar[3]
        }

        val rayDirection = floatArrayOf(
            worldFar[0] - worldNear[0],
            worldFar[1] - worldNear[1],
            worldFar[2] - worldNear[2]
        )

        val length = sqrt(
            rayDirection[0] * rayDirection[0] +
                    rayDirection[1] * rayDirection[1] +
                    rayDirection[2] * rayDirection[2]
        )
        rayDirection[0] /= length
        rayDirection[1] /= length
        rayDirection[2] /= length

        // we only check for interaction between processed planes.
        val planes = BuildingRenderer.processedPlanes
        Log.d(TAG, "Checking intersection with ${planes.size} planes")

        for (plane in planes) {
            if (plane.trackingState == TrackingState.TRACKING) {
                val planePose = plane.centerPose
                val planeNormal = floatArrayOf(0f, 1f, 0f)

                val rayOrigin = worldNear.slice(0..2).toFloatArray()
                val denom = planeNormal[0] * rayDirection[0] +
                        planeNormal[1] * rayDirection[1] +
                        planeNormal[2] * rayDirection[2]

                if (abs(denom) > 0.000001f) {
                    val t = (
                            planeNormal[0] * (planePose.tx() - rayOrigin[0]) +
                                    planeNormal[1] * (planePose.ty() - rayOrigin[1]) +
                                    planeNormal[2] * (planePose.tz() - rayOrigin[2])
                            ) / denom

                    val hitPoint = floatArrayOf(
                        rayOrigin[0] + rayDirection[0] * t,
                        rayOrigin[1] + rayDirection[1] * t,
                        rayOrigin[2] + rayDirection[2] * t
                    )

                    Log.d(TAG, "Found intersection point: (${hitPoint[0]}, ${hitPoint[1]}, ${hitPoint[2]})")
                    return Pose(hitPoint, floatArrayOf(0f, 0f, 0f, 1f))
                }
            }
        }

        Log.w(TAG, "No valid intersection found with any plane")
        return null
    }

    companion object {
        private const val TAG = "GLRenderer"

        fun checkGLError(tag: String?, label: String) {
            var error: Int
            while ((GLES20.glGetError().also { error = it }) != GLES20.GL_NO_ERROR) {
                Log.e(tag, "$label: glError $error")
                throw RuntimeException("$label: glError $error")
            }
        }

        fun loadShader(type: Int, shaderCode: String): Int {
            Log.v(TAG, "Loading shader of type: $type")
            val shader = GLES20.glCreateShader(type)
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)

            val compileStatus = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
            if (compileStatus[0] != GLES20.GL_TRUE) {
                val error = GLES20.glGetShaderInfoLog(shader)
                Log.e(TAG, "Shader compilation failed: $error")
                throw RuntimeException("Shader compilation failed: $error")
            }
            return shader
        }

        fun createProgram(vertexSource: String, fragmentSource: String): Int {
            Log.d(TAG, "Creating shader program")
            val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource)
            val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)

            val program = GLES20.glCreateProgram()
            GLES20.glAttachShader(program, vertexShader)
            GLES20.glAttachShader(program, fragmentShader)
            GLES20.glLinkProgram(program)

            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] != GLES20.GL_TRUE) {
                val error = GLES20.glGetProgramInfoLog(program)
                Log.e(TAG, "Program linking failed: $error")
                throw RuntimeException("Program linking failed: $error")
            }

            Log.d(TAG, "Shader program created successfully")
            return program
        }
    }
}
package com.example.arcities

import android.annotation.SuppressLint
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.arcities.helpers.CameraPermissionHelper
import com.example.arcities.renderers.GLRenderer

class MainActivity : AppCompatActivity() {

    private val tag = "MainActivity"

    private val cameraPermissionHelper = CameraPermissionHelper(this)
    private var arCoreSessionManager: ARCoreSessionManager? = null
    private lateinit var surfaceView: GLSurfaceView
    private var renderer: GLRenderer? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(tag, "onCreate")

        arCoreSessionManager = ARCoreSessionManager(this)
        renderer = GLRenderer(this@MainActivity)
        surfaceView = GLSurfaceView(this)
        surfaceView.preserveEGLContextOnPause = true
        surfaceView.setEGLContextClientVersion(2)
        surfaceView.setRenderer(renderer)
        surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY)
        setContentView(surfaceView)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event?.let {
            if (it.action == MotionEvent.ACTION_UP) {
                renderer?.onTap(it)
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onResume() {
        super.onResume()
        Log.d(tag, "onResume")

        if (!cameraPermissionHelper.hasCameraPermission()) {
            if (!cameraPermissionHelper.shouldShowRequestPermissionRationale()) {
                cameraPermissionHelper.requestCameraPermission()
            } else {
                Toast.makeText(
                    this, "Camera permission is needed to run this application",
                    Toast.LENGTH_LONG
                ).show()
                return
            }
        } else {
            startSession()
        }
    }

    private fun startSession() {
        arCoreSessionManager?.resumeSession()
        if (arCoreSessionManager?.arSession == null) {
            return
        }
        renderer?.setSession(arCoreSessionManager!!.arSession!!)
        surfaceView.onResume();
    }

    override fun onPause() {
        super.onPause()
        Log.d(tag, "onPause")
        arCoreSessionManager?.pauseSession()
        surfaceView!!.onPause();
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CameraPermissionHelper.CAMERA_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == 0) {
                    Log.d(tag, "Camera permission granted")
                    startSession()
                } else {
                    Log.e(tag, "Camera permission denied")
                }
            }
        }
    }
}
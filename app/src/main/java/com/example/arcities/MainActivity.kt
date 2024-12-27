package com.example.arcities

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.exceptions.*
import javax.microedition.khronos.opengles.GL

class MainActivity : AppCompatActivity() {

    private val cameraPermissionHelper = CameraPermissionHelper(this)
    private var arCoreSessionManager: ARCoreSessionManager? = null
    private var surfaceView: GLSurfaceView? = null
    private var renderer: GLRenderer? = null

    //private lateinit var glSurfaceView: MYGLSurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arCoreSessionManager = ARCoreSessionManager(this)

        surfaceView = GLSurfaceView(this)
        surfaceView!!.setPreserveEGLContextOnPause(true)
        surfaceView!!.setEGLContextClientVersion(2)
        //surfaceView!!.setEGLConfigChooser(8, 8, 8, 8, 16, 0);

        renderer = GLRenderer(this)
        surfaceView!!.setRenderer(renderer)
        surfaceView!!.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY)
        setContentView(surfaceView)
        //glSurfaceView = MYGLSurfaceView(this, arCoreSessionManager!!)
        //setContentView(glSurfaceView)
    }

    override fun onResume() {
        super.onResume()


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
        }

        arCoreSessionManager?.resumeSession()
        renderer?.setSession(arCoreSessionManager!!.arSession!!)
        surfaceView!!.onResume();
    }

    override fun onPause() {
        super.onPause()
        arCoreSessionManager?.pauseSession()
        surfaceView!!.onPause();
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            CameraPermissionHelper.CAMERA_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == 0) {
                    //cameraPermissionHelper.setCameraPermission(true)
                } else {
                    // cameraPermissionHelper.setCameraPermission(false)
                }
            }
        }
    }
}

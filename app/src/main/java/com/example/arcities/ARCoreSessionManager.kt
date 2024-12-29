package com.example.arcities

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.*

class ARCoreSessionManager(private val context: Context) {
    private val tag = "ARCoreSessionManager"
    private var currentToast: Toast? = null

    var arSession: Session? = null
        private set

    init {
        Log.i(tag, "ARCoreSessionManager initialized with context: ${context.javaClass.simpleName}")
    }

    private fun showToast(message: String) {
        (context as? Activity)?.runOnUiThread {
            currentToast?.cancel()
            currentToast = Toast.makeText(context, message, Toast.LENGTH_LONG).apply { show() }
        }
    }

    private fun InitializeSession(): Boolean {
        Log.i(tag, "Initializing ARCore session")
        return try {
            val installStatus = ArCoreApk.getInstance().requestInstall(context as Activity?, true)
            Log.d(tag, "ARCore install status: $installStatus")

            when (installStatus) {
                ArCoreApk.InstallStatus.INSTALLED -> {
                    Log.d(tag, "ARCore is installed, creating session")
                    arSession = Session(context)

                    Log.d(tag, "Configuring ARCore session")
                    val config = Config(arSession).apply {
                        planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
                        depthMode = Config.DepthMode.AUTOMATIC
                        updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                        lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                        focusMode = Config.FocusMode.AUTO
                        instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
                    }

                    arSession?.configure(config)
                    arSession?.resume()
                    Log.i(tag, "ARCore session initialized and resumed successfully")
                    true
                }

                ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                    Log.i(tag, "ARCore installation requested")
                    false
                }

                else -> {
                    Log.w(tag, "Unexpected ARCore install status: $installStatus")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error initializing ARCore", e)

            val errorMessage = when (e) {
                is UnavailableUserDeclinedInstallationException -> "User declined ARCore installation"
                is UnavailableArcoreNotInstalledException -> "ARCore is not installed"
                is UnavailableDeviceNotCompatibleException -> "Device is not compatible with ARCore"
                is UnavailableSdkTooOldException -> "ARCore SDK is too old"
                is UnavailableApkTooOldException -> "ARCore APK is too old"
                else -> "Error initializing ARCore: ${e.message}"
            }

            Log.e(tag, errorMessage, e)
            showToast(errorMessage)
            throw e
        }
    }

    fun resumeSession() {
        Log.i(tag, "Attempting to resume ARCore session")
        if (arSession == null) {
            Log.d(tag, "No existing session found, initializing new session")
            InitializeSession()
        } else {
            try {
                arSession?.resume()
            } catch (e: Exception) {
                showToast("Failed to resume AR session: ${e.localizedMessage}")
            }
        }
    }

    fun pauseSession() {
        Log.i(tag, "Pausing ARCore session")
        try {
            arSession?.pause()
        } catch (e: Exception) {
            showToast("Error pausing AR session: ${e.localizedMessage}")
        }
    }
}
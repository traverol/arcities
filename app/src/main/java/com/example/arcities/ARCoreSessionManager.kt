package com.example.arcities

import android.app.Activity
import com.google.ar.core.Session
import android.content.Context
import android.widget.Toast
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.exceptions.*


class ARCoreSessionManager(private val context: Context) {
    public var arSession: Session? = null
        private set

    fun InitializeSession(): Boolean {
        return try {
            when (ArCoreApk.getInstance().requestInstall(context as Activity?, true)) {
                ArCoreApk.InstallStatus.INSTALLED -> {
                    arSession = Session(context)
                    val config = Config(arSession).apply {
                        planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
                        updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                        focusMode = Config.FocusMode.AUTO
                        lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                    }
                    arSession?.configure(config)
                    arSession?.resume()
                    true
                }

                ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                    false
                }
            }
        } catch (e: Exception) {
            Toast.makeText(
                context,"Error Initializing AR Core: " + e.message,
                Toast.LENGTH_LONG
            ).show()
            when (e) {
                is UnavailableUserDeclinedInstallationException,
                is UnavailableArcoreNotInstalledException,
                is UnavailableDeviceNotCompatibleException,
                is UnavailableSdkTooOldException,
                is UnavailableApkTooOldException -> false
                else -> throw e
            }
        }
    }

    fun resumeSession() {
        if (arSession == null) {
            InitializeSession()
        } else {
            arSession?.resume()
        }
    }

    fun pauseSession() {
        arSession?.pause()
    }

    fun updateSession(): Frame? {
        return arSession?.update()
    }
}
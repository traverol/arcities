package com.example.arcities

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class CameraPermissionHelper(private val context: Context) {

    companion object {
        const val CAMERA_PERMISSION_REQUEST_CODE = 100
    }

    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun shouldShowRequestPermissionRationale(): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(
            context as androidx.appcompat.app.AppCompatActivity,
            Manifest.permission.CAMERA
        )
    }

    fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            context as androidx.appcompat.app.AppCompatActivity,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST_CODE
        )
    }
}
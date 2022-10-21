package com.chickenprawn.util

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.chickenprawn.R
import com.chickenprawn.fileSelect.getActivity

// Gets the PostNotifications permissions for Android Version 33
class PermissionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("PermissionActivity", "Starting PermissionActivity")
        // Callback
        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Log.d("PermissionActivity", "After dialog permission is granted")
                finish()
            } else {
                Log.d("PermissionActivity", "After dialog permission is not granted")
                finish()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        == PackageManager.PERMISSION_GRANTED -> {
                    // Permission is granted
                    Log.d("PermissionActivity", "Notification Permission is already granted")
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {}
                else -> {
                    // Ask for the permission
                    // The registered ActivityResultCallback gets the result of this request.
                    Log.d("PermissionActivity", "Permission is not granted")
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

        }


    }
}
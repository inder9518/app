package com.example.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import com.example.receiver.VaultDeviceAdminReceiver

object AppLaunchUtils {

    /**
     * Checks if the app is configured as the active Device Owner of the system.
     */
    fun isDeviceOwner(context: Context): Boolean {
        return try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            dpm?.isDeviceOwnerApp(context.packageName) ?: false
        } catch (t: Throwable) {
            false
        }
    }

    /**
     * Set a package to be hidden/suspended system-wide (requires Device Owner).
     */
    fun setPackageHidden(context: Context, packageName: String, hidden: Boolean): Boolean {
        return try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager ?: return false
            val adminComponent = ComponentName(context, VaultDeviceAdminReceiver::class.java)
            if (dpm.isDeviceOwnerApp(context.packageName)) {
                dpm.setApplicationHidden(adminComponent, packageName, hidden)
                true
            } else {
                false
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            false
        }
    }

    /**
     * Extracts an app's launcher icon programmatically and converts it to [ImageBitmap] 
     * for seamless rendering in Jetpack Compose 'Image' components.
     */
    fun getAppIcon(context: Context, packageName: String): ImageBitmap? {
        return try {
            val pm = context.packageManager
            val drawable = pm.getApplicationIcon(packageName) ?: return null
            val bitmap = when (drawable) {
                is BitmapDrawable -> drawable.bitmap
                else -> {
                    // Normalize dimensions to prevent OutOfMemoryError on listing massive number of apps
                    val targetWidth = 120
                    val targetHeight = 120
                    val b = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(b)
                    drawable.setBounds(0, 0, targetWidth, targetHeight)
                    drawable.draw(canvas)
                    b
                }
            }
            bitmap.asImageBitmap()
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }

    /**
     * Standard secure Android launcher execution context. Launches the hidden/added 
     * application using the system's package launch intent. Preserves login session.
     */
    fun launchApplication(context: Context, packageName: String): Boolean {
        return try {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            false
        }
    }
}

package com.lensshortcut.vivo

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

class PermissionManager(private val context: Context) {

    /**
     * Проверка разрешения на отображение поверх других приложений
     */
    fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true  // На старых версиях Android разрешение не требуется
        }
    }

    /**
     * Запрос разрешения на отображение поверх других приложений
     */
    fun requestOverlayPermission(launcher: ActivityResultLauncher<Intent>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            try {
                launcher.launch(intent)
            } catch (e: Exception) {
                // Fallback для некоторых устройств
                val fallbackIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                launcher.launch(fallbackIntent)
            }
        }
    }

    /**
     * Проверка разрешения на уведомления (Android 13+)
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, 
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true  // На старых версиях разрешение не требуется
        }
    }

    /**
     * Проверка отключения оптимизации батареи
     */
    fun isBatteryOptimizationDisabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true  // На старых версиях оптимизации нет
        }
    }

    /**
     * Запрос отключения оптимизации батареи
     */
    fun requestBatteryOptimization(launcher: ActivityResultLauncher<Intent>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                launcher.launch(intent)
            } catch (e: Exception) {
                // Fallback к общим настройкам батареи
                val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                launcher.launch(fallbackIntent)
            }
        }
    }

    /**
     * Открытие настроек приложения
     */
    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback к общим настройкам приложений
            val fallbackIntent = Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
            context.startActivity(fallbackIntent)
        }
    }

    /**
     * Проверка, является ли устройство Vivo
     */
    fun isVivoDevice(): Boolean {
        return Build.MANUFACTURER.equals("vivo", ignoreCase = true) ||
               Build.BRAND.equals("vivo", ignoreCase = true) ||
               Build.PRODUCT.contains("vivo", ignoreCase = true)
    }
}

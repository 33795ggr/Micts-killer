package com.lensshortcut.vivo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                val preferences = context.getSharedPreferences("lens_shortcut_prefs", Context.MODE_PRIVATE)
                val serviceEnabled = preferences.getBoolean("service_enabled", false)
                val autostartEnabled = preferences.getBoolean("autostart_enabled", false)

                if (serviceEnabled && autostartEnabled) {
                    startServiceWithDelay(context)
                }
            }
        }
    }

    private fun startServiceWithDelay(context: Context) {
        // В Android 15 нельзя запускать foreground service напрямую из BOOT_COMPLETED
        // Используем WorkManager с задержкой
        val workRequest = OneTimeWorkRequestBuilder<ServiceStartWorker>()
            .setInitialDelay(5, TimeUnit.SECONDS)  // 5 секунд задержки
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }
}

/**
 * Worker для запуска сервиса с задержкой после загрузки
 * Это обходит ограничения Android 15 на запуск foreground services из BOOT_COMPLETED
 */
class ServiceStartWorker(
    context: Context, 
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        return try {
            val permissionManager = PermissionManager(applicationContext)

            // Проверяем разрешения перед запуском
            if (permissionManager.hasOverlayPermission()) {
                val intent = Intent(applicationContext, FloatingButtonService::class.java)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    applicationContext.startForegroundService(intent)
                } else {
                    applicationContext.startService(intent)
                }

                Result.success()
            } else {
                // Если нет разрешений, не запускаем сервис
                Result.failure()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}

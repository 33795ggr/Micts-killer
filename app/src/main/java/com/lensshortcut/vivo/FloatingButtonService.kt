package com.lensshortcut.vivo

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class FloatingButtonService : Service() {

    companion object {
        private const val TAG = "FloatingButtonService"
        private const val CHANNEL_ID = "floating_button_channel"
        private const val NOTIFICATION_ID = 1001
        private const val SCREENSHOT_REQUEST_CODE = 1000
    }

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var isFloatingViewAttached = false
    private var mediaProjectionManager: MediaProjectionManager? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "FloatingButtonService created")
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "FloatingButtonService started")

        // Обрабатываем результат MediaProjection если есть
        if (intent?.hasExtra("media_projection_data") == true) {
            handleMediaProjectionResult(intent)
        } else {
            showFloatingButton()
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannelCompat.Builder(
            CHANNEL_ID,
            NotificationManagerCompat.IMPORTANCE_LOW
        )
            .setName("Circle to Search Shortcut")
            .setDescription("Плавающая кнопка с скриншотом для Google Lens")
            .build()

        NotificationManagerCompat.from(this).createNotificationChannel(channel)
    }

    private fun createNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_search_category_default)
        .setContentTitle("Circle to Search активен")
        .setContentText("Нажми кнопку → скриншот → Google Lens")
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .build()

    private fun showFloatingButton() {
        if (isFloatingViewAttached) return

        try {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

            // Создаем floating view
            floatingView = LayoutInflater.from(this).inflate(R.layout.floating_button_layout, null)

            val floatingButton = floatingView?.findViewById<ImageView>(R.id.floating_lens_button)
            floatingButton?.setOnClickListener {
                takeScreenshotAndOpenGoogleLens()
            }

            // Настройки окна для overlay
            val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            )

            params.gravity = Gravity.TOP or Gravity.END
            params.x = 50
            params.y = 200

            // Добавляем drag functionality
            setupDragFunctionality(floatingView!!, params)

            windowManager?.addView(floatingView, params)
            isFloatingViewAttached = true

            Log.d(TAG, "Floating button displayed successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Error showing floating button: ${e.message}", e)
        }
    }

    private fun setupDragFunctionality(view: View, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(view, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val deltaX = Math.abs(event.rawX - initialTouchX)
                    val deltaY = Math.abs(event.rawY - initialTouchY)
                    if (deltaX < 10 && deltaY < 10) {
                        view.performClick()
                    }
                    true
                }
                else -> false
            }
        }
    }

    /**
     * НОВЫЙ НАДЕЖНЫЙ МЕТОД: СКРИНШОТ + GOOGLE LENS
     */
    private fun takeScreenshotAndOpenGoogleLens() {
        Log.d(TAG, "=== Taking screenshot and opening Google Lens ===")

        try {
            // МЕТОД 1: Пробуем системный скриншот (Android 9+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // Запускаем системный screenshot
                val screenshotIntent = Intent("android.intent.action.SCREENSHOT")
                screenshotIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                // После скриншота автоматически откроем Google Lens
                Handler(Looper.getMainLooper()).postDelayed({
                    openGoogleLens()
                }, 1500) // Даем время на создание скриншота

                try {
                    startActivity(screenshotIntent)
                    Log.d(TAG, "✅ System screenshot triggered")
                    showToast("📸 Скриншот → Google Lens открывается...")
                    return
                } catch (e: Exception) {
                    Log.d(TAG, "System screenshot failed, trying Google Lens directly")
                }
            }

            // МЕТОД 2: Прямо открываем Google Lens
            openGoogleLens()
            showToast("🔍 Google Lens открыт - выбери изображение из галереи")

        } catch (e: Exception) {
            Log.e(TAG, "Error taking screenshot: ${e.message}", e)
            showToast("❌ Ошибка. Откройте Google Lens вручную")
        }
    }

    private fun openGoogleLens() {
        try {
            // МЕТОД 1: Google Lens через URI
            val lensIntent = Intent(Intent.ACTION_VIEW, Uri.parse("google://lens"))
            lensIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(lensIntent)
            Log.d(TAG, "✅ Google Lens opened via URI")

        } catch (e: Exception) {
            Log.e(TAG, "Google Lens URI failed: ${e.message}")

            try {
                // МЕТОД 2: Google Lens через package
                val packageManager = packageManager
                val lensPackageIntent = packageManager.getLaunchIntentForPackage("com.google.ar.lens")
                if (lensPackageIntent != null) {
                    lensPackageIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(lensPackageIntent)
                    Log.d(TAG, "✅ Google Lens opened via package")
                } else {
                    // МЕТОД 3: Google Photos с Lens
                    val photosIntent = Intent(Intent.ACTION_VIEW)
                    photosIntent.setPackage("com.google.android.apps.photos")
                    photosIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(photosIntent)
                    Log.d(TAG, "✅ Google Photos opened for Lens")
                }

            } catch (e2: Exception) {
                Log.e(TAG, "All Google Lens methods failed: ${e2.message}")
                showToast("❌ Google Lens недоступен. Установите из Play Store")
                openPlayStore()
            }
        }
    }

    private fun openPlayStore() {
        try {
            val playStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.ar.lens"))
            playStoreIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(playStoreIntent)
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.google.ar.lens"))
            webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(webIntent)
        }
    }

    private fun handleMediaProjectionResult(intent: Intent) {
        // Будущая реализация для полноценного скриншота
        Log.d(TAG, "MediaProjection result received")
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "FloatingButtonService destroyed")
        hideFloatingButton()
    }

    private fun hideFloatingButton() {
        if (isFloatingViewAttached && floatingView != null) {
            try {
                windowManager?.removeView(floatingView)
                isFloatingViewAttached = false
                Log.d(TAG, "Floating button hidden")
            } catch (e: Exception) {
                Log.e(TAG, "Error hiding floating button: ${e.message}")
            }
        }
    }
}

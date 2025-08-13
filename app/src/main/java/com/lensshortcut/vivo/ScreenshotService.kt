package com.lensshortcut.vivo

import android.app.Service
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

class ScreenshotService : Service() {

    private lateinit var windowManager: WindowManager
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var screenDensity: Int = 0
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0
    private val handler = Handler(Looper.getMainLooper())

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_CAPTURE = "ACTION_CAPTURE"
        const val EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE"
        const val EXTRA_RESULT_DATA = "EXTRA_RESULT_DATA"
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        screenDensity = displayMetrics.densityDpi
        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)?.let { data ->
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1)
                startCapturePipeline(resultCode, data)
            }
            ACTION_CAPTURE -> captureAndShare()
            ACTION_STOP -> stopCapturePipeline()
        }
        return START_STICKY
    }

    private fun startCapturePipeline(resultCode: Int, data: Intent) {
        val notification = NotificationCompat.Builder(this, MainApplication.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Сервис Circle to Search активен")
            .setContentText("Плитка готова к работе.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_MIN) // Устанавливаем минимальный приоритет
            .build()
        startForeground(1, notification)

        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, data)
        MediaProjectionHolder.setMediaProjection(mediaProjection)

        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2)

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            screenWidth, screenHeight, screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )
    }

    private fun captureAndShare() {
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val delay = prefs.getInt("screenshot_delay", 700).toLong()

        handler.postDelayed({
            try {
                val image = imageReader?.acquireLatestImage()
                if (image == null) { return@postDelayed }

                val planes = image.planes
                val buffer = planes[0].buffer
                val pixelStride = planes[0].pixelStride
                val rowStride = planes[0].rowStride
                val rowPadding = rowStride - pixelStride * screenWidth

                var bitmap = Bitmap.createBitmap(
                    screenWidth + rowPadding / pixelStride, screenHeight, Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, screenWidth, screenHeight)
                image.close()

                shareToLens(bitmap)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, delay)
    }
    
    private fun shareToLens(bitmap: Bitmap) {
        try {
            val file = File(cacheDir, "screenshot.png")
            val fOut = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut)
            fOut.close()

            val fileUri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                file
            )
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                setPackage("com.google.ar.lens")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            try {
                startActivity(shareIntent)
            } catch (e: ActivityNotFoundException) {
                val genericShareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(genericShareIntent, "Поделиться скриншотом через...")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(chooser)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopCapturePipeline() {
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        MediaProjectionHolder.clear()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopCapturePipeline()
        super.onDestroy()
    }
}
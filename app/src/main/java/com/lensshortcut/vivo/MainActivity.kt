package com.lensshortcut.vivo

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.SeekBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.lensshortcut.vivo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val mediaProjectionResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val serviceIntent = Intent(this, ScreenshotService::class.java).apply {
                    action = ScreenshotService.ACTION_START
                    putExtra(ScreenshotService.EXTRA_RESULT_CODE, result.resultCode)
                    putExtra(ScreenshotService.EXTRA_RESULT_DATA, data)
                }
                ContextCompat.startForegroundService(this, serviceIntent)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Получаем SharedPreferences для хранения настроек
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        // Загружаем сохраненную задержку, по умолчанию ставим 700
        var currentDelay = prefs.getInt("screenshot_delay", 700)

        // Настройка текста и начального положения SeekBar
        binding.delayValueText.text = "$currentDelay мс"
        // Устанавливаем прогресс. Минимальное значение 300, поэтому вычитаем его
        binding.delaySeekbar.progress = currentDelay - 300

        // Слушатель изменений для SeekBar
        binding.delaySeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Минимальная задержка 300 мс, прибавляем к значению ползунка
                currentDelay = progress + 300
                binding.delayValueText.text = "$currentDelay мс"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Когда пользователь отпустил ползунок, сохраняем значение
                prefs.edit().putInt("screenshot_delay", currentDelay).apply()
            }
        })

        binding.activateButton.setOnClickListener {
            if (MediaProjectionHolder.getMediaProjection() != null) {
                val stopIntent = Intent(this, ScreenshotService::class.java).apply {
                    action = ScreenshotService.ACTION_STOP
                }
                startService(stopIntent)
            } else {
                val mediaProjectionManager =
                    getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                mediaProjectionResultLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateUi()
    }

    private fun updateUi() {
        if (MediaProjectionHolder.getMediaProjection() != null) {
            binding.statusText.text = "Статус: Сервис АКТИВЕН"
            binding.statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            binding.activateButton.text = "Остановить сервис"
        } else {
            binding.statusText.text = "Статус: Сервис НЕ АКТИВЕН"
            binding.statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            binding.activateButton.text = "Активировать сервис"
        }
    }
}
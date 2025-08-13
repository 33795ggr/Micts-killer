package com.lensshortcut.vivo

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.view.WindowManager
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.N)
class SearchTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        if (isLocked) return

        if (MediaProjectionHolder.getMediaProjection() != null) {
            // <<< ГЛАВНОЕ ИЗМЕНЕНИЕ: ЗАПУСКАЕМ СЕРВИС ЧЕРЕЗ НЕВИДИМЫЙ ДИАЛОГ >>>

            // 1. Создаем полностью прозрачный диалог
            val dialog = AlertDialog.Builder(this, android.R.style.Theme_Translucent_NoTitleBar)
                .create()
            
            // Устанавливаем системный тип окна, чтобы его можно было показать из сервиса
            dialog.window?.setType(WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            
            // 2. Устанавливаем слушатель, который сработает В МОМЕНТ ПОКАЗА диалога
            dialog.setOnShowListener {
                // 3. Как только диалог показался (и шторка начала закрываться), запускаем сервис
                val intent = Intent(this, ScreenshotService::class.java).apply {
                    action = ScreenshotService.ACTION_CAPTURE
                }
                startService(intent)
                
                // 4. Сразу же закрываем наш невидимый диалог
                // Небольшая задержка, чтобы система успела обработать все команды
                Handler(Looper.getMainLooper()).postDelayed({
                    if (dialog.isShowing) {
                        dialog.dismiss()
                    }
                }, 100)
            }
            
            // 5. Показываем диалог. Это действие заставит систему закрыть шторку.
            showDialog(dialog)

        } else {
            unlockAndRun {}
        }
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        if (MediaProjectionHolder.getMediaProjection() != null) {
            tile.state = Tile.STATE_ACTIVE
            tile.label = "Circle to Search"
            tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_search)
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.label = "Настроить Search"
            tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_search)
        }
        tile.updateTile()
    }
}
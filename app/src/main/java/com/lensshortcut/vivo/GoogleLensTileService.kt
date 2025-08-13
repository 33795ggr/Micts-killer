package com.lensshortcut.vivo

import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.N)
// Убедитесь, что имя класса совпадает с именем файла
class GoogleLensTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        if (MediaProjectionHolder.getMediaProjection() != null) {
            val intent = Intent(this, ScreenshotService::class.java).apply {
                // ИСПРАВЛЕНО: Используем правильное имя действия из ScreenshotService
                action = ScreenshotService.ACTION_CAPTURE
            }
            startService(intent)
        } else {
            val appIntent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivityAndCollapse(appIntent)
        }
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        if (MediaProjectionHolder.getMediaProjection() != null) {
            tile.state = Tile.STATE_ACTIVE
            tile.label = "Circle to Search"
            // Убедитесь, что у вас есть иконка с таким именем в папке res/drawable
            tile.icon = Icon.createWithResource(this, R.drawable.ic_launcher_foreground)
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.label = "Настроить Search"
            tile.icon = Icon.createWithResource(this, R.drawable.ic_launcher_foreground)
        }
        tile.updateTile()
    }
}
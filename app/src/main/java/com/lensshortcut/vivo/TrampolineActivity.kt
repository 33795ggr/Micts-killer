package com.lensshortcut.vivo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Эта Activity не имеет видимого интерфейса и является "трамплином".
 * Ее единственная задача — быть запущенной и сразу же закрыться.
 * Это нужно для того, чтобы безопасно свернуть шторку уведомлений
 * после нажатия на плитку на Android 12 и выше.
 */
class TrampolineActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Сразу же закрываем Activity после ее создания.
        finish()
    }
}
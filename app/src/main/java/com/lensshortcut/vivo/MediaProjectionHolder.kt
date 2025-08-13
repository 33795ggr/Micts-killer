package com.lensshortcut.vivo

import android.media.projection.MediaProjection
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Singleton-объект для хранения экземпляра MediaProjection.
 * Это позволяет нам сохранять разрешение на захват экрана между запусками
 * различных компонентов приложения (Activity, Service).
 */
object MediaProjectionHolder {
    private var mediaProjection: MediaProjection? = null
    private val lock = ReentrantLock()

    // Устанавливаем и регистрируем колбэк для очистки при остановке
    fun setMediaProjection(projection: MediaProjection?) {
        lock.withLock {
            mediaProjection = projection
            mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    super.onStop()
                    // Если пользователь остановил проекцию (например, через системный диалог),
                    // очищаем наш холдер.
                    clear()
                }
            }, null)
        }
    }

    fun getMediaProjection(): MediaProjection? {
        lock.withLock {
            return mediaProjection
        }
    }

    // Очищаем проекцию
    fun clear() {
        lock.withLock {
            mediaProjection?.stop()
            mediaProjection = null
        }
    }
}
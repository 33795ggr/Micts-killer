package com.lensshortcut.vivo

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.widget.Toast
import android.util.Log

class LensLauncher(private val context: Context) {

    companion object {
        private const val TAG = "LensLauncher"

        // ВСЕ ИЗВЕСТНЫЕ ПАКЕТЫ Google Lens (2025)
        private val GOOGLE_LENS_PACKAGES = arrayOf(
            "com.google.ar.lens",                    // Основной пакет Google Lens (как у пользователя)
            "com.google.android.googlequicksearchbox", // Google App с встроенным Lens
            "com.google.android.apps.lens",          // Альтернативный пакет
            "com.google.lens",                       // Возможный вариант
            "com.google.android.lens",               // Еще один вариант
            "com.android.lens"                       // Системный вариант
        )

        // URI схемы для запуска (универсальные)
        private val LENS_URI_SCHEMES = arrayOf(
            "google-lens://",
            "googleapp://lens", 
            "googleapp://lens-mode",
            "https://lens.google.com",
            "intent:#Intent;action=com.google.android.googlequicksearchbox.LENS_ACTIVITY;end"
        )

        // Intent действия для Lens
        private val LENS_INTENT_ACTIONS = arrayOf(
            "com.google.android.googlequicksearchbox.LENS_ACTIVITY",
            "com.google.ar.lens.LENS_ACTIVITY",
            "android.intent.action.VIEW",
            "android.media.action.IMAGE_CAPTURE"
        )

        // Активности для поиска в Google App
        private val LENS_ACTIVITY_KEYWORDS = arrayOf(
            "lens", "camera", "visual", "search", "scan"
        )
    }

    /**
     * Проверка доступности Google Lens (проверяем ВСЕ пакеты)
     */
    fun isGoogleLensAvailable(): Boolean {
        // Проверяем все известные пакеты
        for (packageName in GOOGLE_LENS_PACKAGES) {
            if (isPackageInstalled(packageName)) {
                Log.d(TAG, "Found Google Lens package: $packageName")
                return true
            }
        }

        Log.d(TAG, "No Google Lens packages found")
        return false
    }

    /**
     * Получение первого найденного пакета Lens
     */
    private fun getFirstAvailableLensPackage(): String? {
        for (packageName in GOOGLE_LENS_PACKAGES) {
            if (isPackageInstalled(packageName)) {
                return packageName
            }
        }
        return null
    }

    /**
     * УНИВЕРСАЛЬНЫЙ метод запуска Google Lens
     */
    fun launchGoogleLens(): Boolean {
        Log.d(TAG, "Starting universal Google Lens launch...")

        // Способ 1: URI схемы (самый надежный для всех устройств)
        if (tryLaunchViaUriSchemes()) {
            Log.d(TAG, "✓ Launched via URI scheme")
            return true
        }

        // Способ 2: Прямой запуск найденного пакета
        if (tryLaunchDirectPackage()) {
            Log.d(TAG, "✓ Launched via direct package")
            return true
        }

        // Способ 3: Поиск и запуск через Intent действия
        if (tryLaunchViaIntentActions()) {
            Log.d(TAG, "✓ Launched via intent actions")
            return true
        }

        // Способ 4: Поиск активностей в Google App
        if (tryLaunchViaActivitySearch()) {
            Log.d(TAG, "✓ Launched via activity search")
            return true
        }

        // Способ 5: Camera intent как fallback
        if (tryLaunchCameraFallback()) {
            Log.d(TAG, "✓ Launched camera as fallback")
            return true
        }

        // Способ 6: Предложение установки
        offerInstallGoogleLens()
        Log.d(TAG, "All launch methods failed")
        return false
    }

    /**
     * Способ 1: URI схемы (работает на большинстве устройств)
     */
    private fun tryLaunchViaUriSchemes(): Boolean {
        for (uriScheme in LENS_URI_SCHEMES) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriScheme)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }

                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    Log.d(TAG, "Success with URI: $uriScheme")
                    return true
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed URI $uriScheme: ${e.message}")
            }
        }
        return false
    }

    /**
     * Способ 2: Прямой запуск пакета
     */
    private fun tryLaunchDirectPackage(): Boolean {
        val lensPackage = getFirstAvailableLensPackage() ?: return false

        try {
            // Пробуем launcher intent
            val intent = context.packageManager.getLaunchIntentForPackage(lensPackage)
            intent?.let {
                it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                context.startActivity(it)
                Log.d(TAG, "Launched package: $lensPackage")
                return true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to launch package $lensPackage: ${e.message}")
        }

        // Пробуем основные активности
        val mainActivities = arrayOf(
            "$lensPackage.MainActivity",
            "$lensPackage.LensActivity", 
            "$lensPackage.CameraActivity",
            "$lensPackage.activities.MainActivity"
        )

        for (activityName in mainActivities) {
            try {
                val intent = Intent().apply {
                    setClassName(lensPackage, activityName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(intent)
                Log.d(TAG, "Launched activity: $activityName")
                return true
            } catch (e: Exception) {
                Log.v(TAG, "Activity not found: $activityName")
            }
        }

        return false
    }

    /**
     * Способ 3: Intent действия
     */
    private fun tryLaunchViaIntentActions(): Boolean {
        for (action in LENS_INTENT_ACTIONS) {
            try {
                val intent = Intent(action).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }

                // Пробуем с конкретными пакетами
                for (packageName in GOOGLE_LENS_PACKAGES) {
                    if (isPackageInstalled(packageName)) {
                        intent.setPackage(packageName)
                        if (intent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(intent)
                            Log.d(TAG, "Success with action: $action, package: $packageName")
                            return true
                        }
                    }
                }

                // Пробуем без указания пакета
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    Log.d(TAG, "Success with action: $action")
                    return true
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed action $action: ${e.message}")
            }
        }
        return false
    }

    /**
     * Способ 4: Поиск активностей по ключевым словам
     */
    private fun tryLaunchViaActivitySearch(): Boolean {
        for (packageName in GOOGLE_LENS_PACKAGES) {
            if (!isPackageInstalled(packageName)) continue

            try {
                val packageInfo = context.packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
                packageInfo.activities?.let { activities ->

                    for (activity in activities) {
                        for (keyword in LENS_ACTIVITY_KEYWORDS) {
                            if (activity.name.contains(keyword, ignoreCase = true)) {
                                try {
                                    val intent = Intent().apply {
                                        setClassName(packageName, activity.name)
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    }
                                    context.startActivity(intent)
                                    Log.d(TAG, "Launched via activity search: ${activity.name}")
                                    return true
                                } catch (e: Exception) {
                                    Log.v(TAG, "Failed activity ${activity.name}: ${e.message}")
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to search activities in $packageName: ${e.message}")
            }
        }
        return false
    }

    /**
     * Способ 5: Camera fallback
     */
    private fun tryLaunchCameraFallback(): Boolean {
        try {
            val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                Toast.makeText(context, "Открыта камера. Найдите Google Lens в приложениях", Toast.LENGTH_LONG).show()
                return true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed camera fallback: ${e.message}")
        }
        return false
    }

    /**
     * Способ 6: Предложение установки
     */
    private fun offerInstallGoogleLens() {
        val installOptions = arrayOf(
            "market://details?id=com.google.ar.lens",
            "https://play.google.com/store/apps/details?id=com.google.ar.lens",
            "market://details?id=com.google.android.googlequicksearchbox",
            "https://play.google.com/store/apps/details?id=com.google.android.googlequicksearchbox"
        )

        for (storeUrl in installOptions) {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(storeUrl)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return
            } catch (e: Exception) {
                Log.w(TAG, "Failed to open store: $storeUrl")
            }
        }

        Toast.makeText(context, "Установите Google Lens или Google App из Play Store", Toast.LENGTH_LONG).show()
    }

    /**
     * Проверка установленности пакета
     */
    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Получение всех установленных Lens пакетов
     */
    fun getInstalledLensPackages(): Map<String, String> {
        val installedPackages = mutableMapOf<String, String>()

        for (packageName in GOOGLE_LENS_PACKAGES) {
            if (isPackageInstalled(packageName)) {
                try {
                    val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
                    installedPackages[packageName] = packageInfo.versionName ?: "Unknown"
                } catch (e: Exception) {
                    installedPackages[packageName] = "Error getting version"
                }
            }
        }

        return installedPackages
    }

    /**
     * Полная диагностика для всех устройств
     */
    fun getUniversalDiagnostic(): String {
        val sb = StringBuilder()
        sb.appendLine("=== UNIVERSAL GOOGLE LENS DIAGNOSTIC ===")
        sb.appendLine("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        sb.appendLine("Android: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
        sb.appendLine("Date: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date())}")
        sb.appendLine()

        // Проверка всех пакетов
        sb.appendLine("📦 LENS PACKAGES CHECK:")
        val installedPackages = getInstalledLensPackages()
        if (installedPackages.isEmpty()) {
            sb.appendLine("   ❌ No Google Lens packages found")
        } else {
            installedPackages.forEach { (pkg, version) ->
                sb.appendLine("   ✅ $pkg (v$version)")
            }
        }
        sb.appendLine()

        // Проверка URI схем
        sb.appendLine("🔗 URI SCHEMES CHECK:")
        var foundWorkingUri = false
        for (uri in LENS_URI_SCHEMES) {
            val works = try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                intent.resolveActivity(context.packageManager) != null
            } catch (e: Exception) {
                false
            }
            sb.appendLine("   ${if (works) "✅" else "❌"} $uri")
            if (works) foundWorkingUri = true
        }
        sb.appendLine()

        // Проверка Intent действий
        sb.appendLine("⚡ INTENT ACTIONS CHECK:")
        var foundWorkingAction = false
        for (action in LENS_INTENT_ACTIONS) {
            val works = try {
                val intent = Intent(action)
                intent.resolveActivity(context.packageManager) != null
            } catch (e: Exception) {
                false
            }
            sb.appendLine("   ${if (works) "✅" else "❌"} $action")
            if (works) foundWorkingAction = true
        }
        sb.appendLine()

        // Общий статус
        val canLaunch = installedPackages.isNotEmpty() || foundWorkingUri || foundWorkingAction
        sb.appendLine("🎯 LAUNCH CAPABILITY: ${if (canLaunch) "✅ POSSIBLE" else "❌ NOT POSSIBLE"}")

        if (canLaunch) {
            sb.appendLine("   Recommended method: ${
                when {
                    foundWorkingUri -> "URI Scheme"
                    installedPackages.isNotEmpty() -> "Direct Package Launch"
                    foundWorkingAction -> "Intent Action"
                    else -> "Unknown"
                }
            }")
        } else {
            sb.appendLine("   💡 Install Google Lens or Google App from Play Store")
        }

        return sb.toString()
    }

    /**
     * Быстрая проверка совместимости
     */
    fun isDeviceCompatible(): Boolean {
        return getInstalledLensPackages().isNotEmpty() ||
               LENS_URI_SCHEMES.any { uri ->
                   try {
                       val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                       intent.resolveActivity(context.packageManager) != null
                   } catch (e: Exception) {
                       false
                   }
               }
    }
}

package com.void.desktop.data.storage

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

@Serializable
data class AppPreferences(
    val serverUrl: String = "",
    val accessToken: String = "",
    val userId: String = "",
    val userName: String = "",
    val serverId: String = "",
    val deviceId: String = UUID.randomUUID().toString(),
    val customVlcPath: String = ""
)

object PreferencesStorage {
    private val prefsDir = File(System.getProperty("user.home"), ".void-jellyfin")
    private val prefsFile = File(prefsDir, "prefs.json")

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    fun save(prefs: AppPreferences) {
        prefsDir.mkdirs()
        prefsFile.writeText(json.encodeToString(prefs))
    }

    fun load(): AppPreferences {
        return if (prefsFile.exists()) {
            try {
                json.decodeFromString<AppPreferences>(prefsFile.readText())
            } catch (e: Exception) {
                AppPreferences()
            }
        } else {
            AppPreferences()
        }
    }

    fun clear() {
        val deviceId = load().deviceId
        save(AppPreferences(deviceId = deviceId))
    }
}

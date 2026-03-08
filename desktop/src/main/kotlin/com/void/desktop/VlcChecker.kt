package com.void.desktop

import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Checks VLC availability and provides helpful error messages
 */
object VlcChecker {
    fun checkVlcInstallation(): VlcStatus {
        // Try to find VLC in standard locations
        val standardLocations = listOf(
            "C:\\Program Files\\VideoLAN\\VLC",
            "C:\\Program Files (x86)\\VideoLAN\\VLC",
            "${System.getenv("LOCALAPPDATA")}\\Programs\\VideoLAN\\VLC"
        )

        val foundLocations = standardLocations.map { File(it) }.filter { it.exists() }

        return when {
            foundLocations.isNotEmpty() -> {
                val vlcPath = foundLocations.firstOrNull()
                // Try to discover VLC
                try {
                    val discovery = NativeDiscovery()
                    val discovered = discovery.discover()
                    if (discovered) {
                        VlcStatus.Available(vlcPath?.absolutePath)
                    } else {
                        VlcStatus.InstalledButNotFound(foundLocations.map { it.absolutePath })
                    }
                } catch (e: Exception) {
                    VlcStatus.Error(e.message ?: "Unknown error")
                }
            }
            else -> VlcStatus.NotInstalled
        }
    }
}

sealed class VlcStatus {
    data class Available(val path: String?) : VlcStatus()
    data class InstalledButNotFound(val paths: List<String>) : VlcStatus()
    data class Error(val message: String) : VlcStatus()
    object NotInstalled : VlcStatus()

    fun isAvailable(): Boolean = this is Available
}

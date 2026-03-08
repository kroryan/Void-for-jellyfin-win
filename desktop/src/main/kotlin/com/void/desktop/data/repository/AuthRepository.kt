package com.void.desktop.data.repository

import com.void.desktop.data.api.ApiClient
import com.void.desktop.data.dto.AuthResponse
import com.void.desktop.data.dto.LoginRequest
import com.void.desktop.data.dto.ServerInfo
import com.void.desktop.data.storage.AppPreferences
import com.void.desktop.data.storage.PreferencesStorage

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val cause: Throwable? = null) : Result<Nothing>()
}

class AuthRepository {

    suspend fun checkServer(serverUrl: String): Result<ServerInfo> {
        return try {
            val api = ApiClient.createApi(serverUrl)
            val info = api.getServerInfo()
            Result.Success(info)
        } catch (e: Exception) {
            Result.Error("Cannot connect to server: ${e.message}", e)
        }
    }

    suspend fun login(
        serverUrl: String,
        username: String,
        password: String,
        deviceId: String
    ): Result<AppPreferences> {
        return try {
            val api = ApiClient.createApi(serverUrl)
            val authHeader = ApiClient.buildAuthHeader(deviceId = deviceId)
            val response: AuthResponse = api.authenticateByName(
                request = LoginRequest(username = username, pw = password),
                authorization = authHeader
            )
            val prefs = AppPreferences(
                serverUrl = serverUrl,
                accessToken = response.accessToken,
                userId = response.user.id,
                userName = response.user.name,
                serverId = response.serverId,
                deviceId = deviceId
            )
            PreferencesStorage.save(prefs)
            Result.Success(prefs)
        } catch (e: retrofit2.HttpException) {
            val msg = when (e.code()) {
                401 -> "Invalid username or password"
                403 -> "Access denied"
                404 -> "Server not found at this URL"
                else -> "Login failed: HTTP ${e.code()}"
            }
            Result.Error(msg, e)
        } catch (e: java.net.UnknownHostException) {
            Result.Error("Cannot reach server. Check URL and network connection.", e)
        } catch (e: Exception) {
            Result.Error("Login failed: ${e.message}", e)
        }
    }

    fun logout() {
        PreferencesStorage.clear()
    }

    fun getSavedSession(): AppPreferences = PreferencesStorage.load()

    fun isLoggedIn(): Boolean {
        val prefs = PreferencesStorage.load()
        return prefs.accessToken.isNotBlank() && prefs.serverUrl.isNotBlank()
    }
}

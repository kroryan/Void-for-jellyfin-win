package com.void.desktop.data.repository

import com.void.desktop.data.api.ApiClient
import com.void.desktop.data.dto.BaseItemDto
import com.void.desktop.data.dto.PlaybackInfoRequest
import com.void.desktop.data.storage.AppPreferences

class LibraryRepository(private val prefs: AppPreferences) {

    private val api get() = ApiClient.createApi(prefs.serverUrl)
    private val auth get() = ApiClient.buildAuthHeader(
        accessToken = prefs.accessToken,
        deviceId = prefs.deviceId
    )

    suspend fun getLibraries(): Result<List<BaseItemDto>> = try {
        val response = api.getUserViews(prefs.userId, auth)
        Result.Success(response.items)
    } catch (e: Exception) {
        Result.Error("Failed to load libraries: ${e.message}", e)
    }

    suspend fun getLibraryItems(
        libraryId: String,
        startIndex: Int = 0,
        limit: Int = 50,
        sortBy: String = "SortName"
    ): Result<List<BaseItemDto>> = try {
        val response = api.getItems(
            userId = prefs.userId,
            auth = auth,
            parentId = libraryId,
            sortBy = sortBy,
            startIndex = startIndex,
            limit = limit
        )
        Result.Success(response.items)
    } catch (e: Exception) {
        Result.Error("Failed to load items: ${e.message}", e)
    }

    suspend fun getResumeItems(): Result<List<BaseItemDto>> = try {
        val response = api.getResumeItems(prefs.userId, auth)
        Result.Success(response.items)
    } catch (e: Exception) {
        Result.Error("Failed to load resume items: ${e.message}", e)
    }

    suspend fun getLatestItems(libraryId: String? = null): Result<List<BaseItemDto>> = try {
        val items = api.getLatestItems(
            userId = prefs.userId,
            auth = auth,
            parentId = libraryId
        )
        Result.Success(items)
    } catch (e: Exception) {
        Result.Error("Failed to load latest items: ${e.message}", e)
    }

    suspend fun getItem(itemId: String): Result<BaseItemDto> = try {
        val item = api.getItem(
            userId = prefs.userId,
            itemId = itemId,
            auth = auth
        )
        Result.Success(item)
    } catch (e: Exception) {
        Result.Error("Failed to load item: ${e.message}", e)
    }

    suspend fun getStreamUrl(itemId: String): Result<String> = try {
        val playbackInfo = api.getPlaybackInfo(
            itemId = itemId,
            auth = auth,
            request = PlaybackInfoRequest(userId = prefs.userId)
        )
        val mediaSource = playbackInfo.mediaSources.firstOrNull()
            ?: return Result.Error("No media source available")
        val streamUrl = ApiClient.buildStreamUrl(
            serverUrl = prefs.serverUrl,
            itemId = itemId,
            mediaSourceId = mediaSource.id,
            accessToken = prefs.accessToken
        )
        Result.Success(streamUrl)
    } catch (e: Exception) {
        Result.Error("Failed to get stream URL: ${e.message}", e)
    }

    suspend fun markPlayed(itemId: String): Result<Unit> = try {
        api.markPlayed(prefs.userId, itemId, auth)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error("Failed to mark as played: ${e.message}", e)
    }

    suspend fun markUnplayed(itemId: String): Result<Unit> = try {
        api.markUnplayed(prefs.userId, itemId, auth)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error("Failed to mark as unplayed: ${e.message}", e)
    }

    suspend fun markFavorite(itemId: String, isFavorite: Boolean): Result<Unit> = try {
        if (isFavorite) {
            api.markFavorite(prefs.userId, itemId, auth)
        } else {
            api.unmarkFavorite(prefs.userId, itemId, auth)
        }
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error("Failed to update favorite: ${e.message}", e)
    }

    suspend fun getSeasons(seriesId: String): Result<List<BaseItemDto>> = try {
        val response = api.getSeasons(
            seriesId = seriesId,
            auth = auth,
            userId = prefs.userId
        )
        Result.Success(response.items)
    } catch (e: Exception) {
        Result.Error("Failed to load seasons: ${e.message}", e)
    }

    suspend fun getEpisodes(seriesId: String, seasonId: String? = null): Result<List<BaseItemDto>> = try {
        val response = api.getEpisodes(
            seriesId = seriesId,
            auth = auth,
            userId = prefs.userId,
            seasonId = seasonId
        )
        Result.Success(response.items)
    } catch (e: Exception) {
        Result.Error("Failed to load episodes: ${e.message}", e)
    }
}

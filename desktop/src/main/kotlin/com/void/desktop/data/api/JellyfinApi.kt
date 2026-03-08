package com.void.desktop.data.api

import com.void.desktop.data.dto.*
import retrofit2.http.*

interface JellyfinApi {

    @POST("Users/AuthenticateByName")
    suspend fun authenticateByName(
        @Body request: LoginRequest,
        @Header("X-Emby-Authorization") authorization: String
    ): AuthResponse

    @GET("System/Info/Public")
    suspend fun getServerInfo(): ServerInfo

    @GET("Users/{userId}/Views")
    suspend fun getUserViews(
        @Path("userId") userId: String,
        @Header("X-Emby-Authorization") auth: String
    ): LibraryResponse

    @GET("Users/{userId}/Items")
    suspend fun getItems(
        @Path("userId") userId: String,
        @Header("X-Emby-Authorization") auth: String,
        @Query("ParentId") parentId: String? = null,
        @Query("SortBy") sortBy: String = "SortName",
        @Query("SortOrder") sortOrder: String = "Ascending",
        @Query("Recursive") recursive: Boolean = false,
        @Query("Fields") fields: String = "PrimaryImageAspectRatio,BasicSyncInfo,Overview,Genres,MediaSources,MediaStreams",
        @Query("ImageTypeLimit") imageTypeLimit: Int = 1,
        @Query("EnableImageTypes") enableImageTypes: String = "Primary,Backdrop,Thumb",
        @Query("StartIndex") startIndex: Int = 0,
        @Query("Limit") limit: Int = 50
    ): LibraryResponse

    @GET("Users/{userId}/Items/Resume")
    suspend fun getResumeItems(
        @Path("userId") userId: String,
        @Header("X-Emby-Authorization") auth: String,
        @Query("Limit") limit: Int = 12,
        @Query("Fields") fields: String = "PrimaryImageAspectRatio,BasicSyncInfo,Overview,Genres"
    ): LibraryResponse

    @GET("Users/{userId}/Items/Latest")
    suspend fun getLatestItems(
        @Path("userId") userId: String,
        @Header("X-Emby-Authorization") auth: String,
        @Query("ParentId") parentId: String? = null,
        @Query("Fields") fields: String = "PrimaryImageAspectRatio,BasicSyncInfo,Overview,Genres",
        @Query("Limit") limit: Int = 16
    ): List<BaseItemDto>

    @GET("Users/{userId}/Items/{itemId}")
    suspend fun getItem(
        @Path("userId") userId: String,
        @Path("itemId") itemId: String,
        @Header("X-Emby-Authorization") auth: String,
        @Query("Fields") fields: String = "PrimaryImageAspectRatio,Overview,Genres,People,Taglines,MediaSources,MediaStreams"
    ): BaseItemDto

    @POST("Items/{itemId}/PlaybackInfo")
    suspend fun getPlaybackInfo(
        @Path("itemId") itemId: String,
        @Header("X-Emby-Authorization") auth: String,
        @Body request: PlaybackInfoRequest
    ): PlaybackInfoResponse

    @POST("Users/{userId}/PlayedItems/{itemId}")
    suspend fun markPlayed(
        @Path("userId") userId: String,
        @Path("itemId") itemId: String,
        @Header("X-Emby-Authorization") auth: String
    ): UserDataDto

    @DELETE("Users/{userId}/PlayedItems/{itemId}")
    suspend fun markUnplayed(
        @Path("userId") userId: String,
        @Path("itemId") itemId: String,
        @Header("X-Emby-Authorization") auth: String
    ): UserDataDto

    @POST("Users/{userId}/FavoriteItems/{itemId}")
    suspend fun markFavorite(
        @Path("userId") userId: String,
        @Path("itemId") itemId: String,
        @Header("X-Emby-Authorization") auth: String
    ): UserDataDto

    @DELETE("Users/{userId}/FavoriteItems/{itemId}")
    suspend fun unmarkFavorite(
        @Path("userId") userId: String,
        @Path("itemId") itemId: String,
        @Header("X-Emby-Authorization") auth: String
    ): UserDataDto

    @GET("Shows/{seriesId}/Episodes")
    suspend fun getEpisodes(
        @Path("seriesId") seriesId: String,
        @Header("X-Emby-Authorization") auth: String,
        @Query("UserId") userId: String,
        @Query("SeasonId") seasonId: String? = null,
        @Query("Fields") fields: String = "PrimaryImageAspectRatio,Overview",
        @Query("StartIndex") startIndex: Int = 0,
        @Query("Limit") limit: Int = 100
    ): LibraryResponse

    @GET("Shows/{seriesId}/Seasons")
    suspend fun getSeasons(
        @Path("seriesId") seriesId: String,
        @Header("X-Emby-Authorization") auth: String,
        @Query("UserId") userId: String,
        @Query("Fields") fields: String = "PrimaryImageAspectRatio"
    ): LibraryResponse

    @GET("Users/{userId}/Items")
    suspend fun searchItems(
        @Path("userId") userId: String,
        @Header("X-Emby-Authorization") auth: String,
        @Query("SearchTerm") searchTerm: String,
        @Query("IncludeItemTypes") includeItemTypes: String? = null,
        @Query("Recursive") recursive: Boolean = true,
        @Query("Fields") fields: String = "PrimaryImageAspectRatio,BasicSyncInfo,Overview,Genres",
        @Query("StartIndex") startIndex: Int = 0,
        @Query("Limit") limit: Int = 50
    ): LibraryResponse

    @GET("Users/{userId}/Items")
    suspend fun getFavoriteItems(
        @Path("userId") userId: String,
        @Header("X-Emby-Authorization") auth: String,
        @Query("IsFavorite") isFavorite: Boolean = true,
        @Query("Recursive") recursive: Boolean = true,
        @Query("Fields") fields: String = "PrimaryImageAspectRatio,BasicSyncInfo,Overview,Genres",
        @Query("SortBy") sortBy: String = "DateCreated",
        @Query("SortOrder") sortOrder: String = "Descending",
        @Query("StartIndex") startIndex: Int = 0,
        @Query("Limit") limit: Int = 50
    ): LibraryResponse
}

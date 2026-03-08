package com.void.desktop.data.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object ApiClient {

    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )
            .build()
    }

    fun createApi(baseUrl: String): JellyfinApi {
        val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(JellyfinApi::class.java)
    }

    fun buildAuthHeader(
        accessToken: String = "",
        deviceId: String,
        appVersion: String = "0.2.6"
    ): String {
        val sanitize = { s: String -> s.replace("\"", "").replace(",", "") }
        return buildString {
            append("MediaBrowser Client=\"Void\"")
            append(", Device=\"Windows Desktop\"")
            append(", DeviceId=\"${sanitize(deviceId)}\"")
            append(", Version=\"$appVersion\"")
            if (accessToken.isNotBlank()) {
                append(", Token=\"${sanitize(accessToken)}\"")
            }
        }
    }

    fun buildImageUrl(
        serverUrl: String,
        itemId: String,
        imageType: String = "Primary",
        tag: String? = null,
        accessToken: String,
        maxWidth: Int = 400
    ): String {
        val base = if (serverUrl.endsWith("/")) serverUrl.dropLast(1) else serverUrl
        return buildString {
            append("$base/Items/$itemId/Images/$imageType")
            append("?api_key=$accessToken")
            append("&maxWidth=$maxWidth")
            if (tag != null) append("&tag=$tag")
        }
    }

    fun buildBackdropUrl(
        serverUrl: String,
        itemId: String,
        tag: String? = null,
        accessToken: String,
        index: Int = 0
    ): String {
        val base = if (serverUrl.endsWith("/")) serverUrl.dropLast(1) else serverUrl
        return buildString {
            append("$base/Items/$itemId/Images/Backdrop/$index")
            append("?api_key=$accessToken")
            append("&maxWidth=1920")
            if (tag != null) append("&tag=$tag")
        }
    }

    fun buildStreamUrl(
        serverUrl: String,
        itemId: String,
        mediaSourceId: String,
        accessToken: String
    ): String {
        val base = if (serverUrl.endsWith("/")) serverUrl.dropLast(1) else serverUrl
        return "$base/Videos/$itemId/stream?Static=true&api_key=$accessToken&MediaSourceId=$mediaSourceId"
    }
}

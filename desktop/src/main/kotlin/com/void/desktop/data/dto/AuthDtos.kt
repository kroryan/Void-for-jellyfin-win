package com.void.desktop.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    @SerialName("Username") val username: String,
    @SerialName("Pw") val pw: String
)

@Serializable
data class AuthResponse(
    @SerialName("User") val user: UserInfo,
    @SerialName("AccessToken") val accessToken: String,
    @SerialName("ServerId") val serverId: String
)

@Serializable
data class UserInfo(
    @SerialName("Name") val name: String,
    @SerialName("Id") val id: String,
    @SerialName("HasPassword") val hasPassword: Boolean = false,
    @SerialName("HasConfiguredPassword") val hasConfiguredPassword: Boolean = false,
    @SerialName("HasConfiguredEasyPassword") val hasConfiguredEasyPassword: Boolean = false,
    @SerialName("EnableAutoLogin") val enableAutoLogin: Boolean? = false
)

@Serializable
data class ServerInfo(
    @SerialName("LocalAddress") val localAddress: String? = null,
    @SerialName("ServerName") val serverName: String? = null,
    @SerialName("Version") val version: String? = null,
    @SerialName("ProductName") val productName: String? = null,
    @SerialName("OperatingSystem") val operatingSystem: String? = null,
    @SerialName("Id") val id: String? = null,
    @SerialName("StartupWizardCompleted") val startupWizardCompleted: Boolean? = null
)

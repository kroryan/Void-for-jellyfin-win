package com.void.desktop.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LibraryResponse(
    @SerialName("Items") val items: List<BaseItemDto> = emptyList(),
    @SerialName("TotalRecordCount") val totalRecordCount: Int = 0
)

@Serializable
data class BaseItemDto(
    @SerialName("Id") val id: String,
    @SerialName("Name") val name: String? = null,
    @SerialName("Title") val title: String? = null,
    @SerialName("Type") val type: String? = null,
    @SerialName("CollectionType") val collectionType: String? = null,
    @SerialName("Overview") val overview: String? = null,
    @SerialName("ProductionYear") val productionYear: Int? = null,
    @SerialName("CommunityRating") val communityRating: Double? = null,
    @SerialName("RunTimeTicks") val runTimeTicks: Long? = null,
    @SerialName("PrimaryImageAspectRatio") val primaryImageAspectRatio: Double? = null,
    @SerialName("ImageTags") val imageTags: ImageTags? = null,
    @SerialName("BackdropImageTags") val backdropImageTags: List<String> = emptyList(),
    @SerialName("Genres") val genres: List<String> = emptyList(),
    @SerialName("IsFolder") val isFolder: Boolean = false,
    @SerialName("ChildCount") val childCount: Int? = null,
    @SerialName("UserData") val userData: UserDataDto? = null,
    @SerialName("MediaSources") val mediaSources: List<MediaSourceDto> = emptyList(),
    @SerialName("MediaStreams") val mediaStreams: List<MediaStreamDto> = emptyList(),
    @SerialName("Taglines") val taglines: List<String> = emptyList(),
    @SerialName("People") val people: List<PersonDto> = emptyList(),
    @SerialName("HasSubtitles") val hasSubtitles: Boolean = false,
    @SerialName("SeriesId") val seriesId: String? = null,
    @SerialName("SeriesName") val seriesName: String? = null,
    @SerialName("SeriesPrimaryImageTag") val seriesPrimaryImageTag: String? = null,
    @SerialName("SeasonId") val seasonId: String? = null,
    @SerialName("SeasonName") val seasonName: String? = null,
    @SerialName("ParentIndexNumber") val parentIndexNumber: Int? = null,
    @SerialName("IndexNumber") val indexNumber: Int? = null
) {
    val displayName: String get() = name ?: title ?: "Unknown"
    val runtimeMinutes: Int? get() = runTimeTicks?.let { (it / 600_000_000).toInt() }
}

@Serializable
data class ImageTags(
    @SerialName("Primary") val primary: String? = null,
    @SerialName("Banner") val banner: String? = null,
    @SerialName("Thumb") val thumb: String? = null,
    @SerialName("Logo") val logo: String? = null,
    @SerialName("Art") val art: String? = null
)

@Serializable
data class UserDataDto(
    @SerialName("IsFavorite") val isFavorite: Boolean = false,
    @SerialName("PlaybackPositionTicks") val playbackPositionTicks: Long = 0,
    @SerialName("PlayCount") val playCount: Int = 0,
    @SerialName("Played") val played: Boolean = false,
    @SerialName("LastPlayedDate") val lastPlayedDate: String? = null
)

@Serializable
data class PersonDto(
    @SerialName("Id") val id: String,
    @SerialName("Name") val name: String,
    @SerialName("Role") val role: String? = null,
    @SerialName("Type") val type: String? = null,
    @SerialName("PrimaryImageTag") val primaryImageTag: String? = null
)

@Serializable
data class MediaSourceDto(
    @SerialName("Id") val id: String,
    @SerialName("Name") val name: String? = null,
    @SerialName("Type") val type: String? = null,
    @SerialName("Container") val container: String? = null,
    @SerialName("Size") val size: Long? = null,
    @SerialName("Bitrate") val bitrate: Int? = null,
    @SerialName("Path") val path: String? = null,
    @SerialName("Protocol") val protocol: String? = null,
    @SerialName("RunTimeTicks") val runTimeTicks: Long? = null,
    @SerialName("MediaStreams") val mediaStreams: List<MediaStreamDto> = emptyList(),
    @SerialName("IsRemote") val isRemote: Boolean = false,
    @SerialName("SupportsTranscoding") val supportsTranscoding: Boolean = false,
    @SerialName("SupportsDirectStream") val supportsDirectStream: Boolean = false,
    @SerialName("SupportsDirectPlay") val supportsDirectPlay: Boolean = false
)

@Serializable
data class MediaStreamDto(
    @SerialName("Index") val index: Int,
    @SerialName("Type") val type: String,
    @SerialName("Codec") val codec: String? = null,
    @SerialName("Language") val language: String? = null,
    @SerialName("DisplayLanguage") val displayLanguage: String? = null,
    @SerialName("Title") val title: String? = null,
    @SerialName("DisplayTitle") val displayTitle: String? = null,
    @SerialName("IsDefault") val isDefault: Boolean = false,
    @SerialName("BitRate") val bitRate: Int? = null,
    @SerialName("Width") val width: Int? = null,
    @SerialName("Height") val height: Int? = null,
    @SerialName("Channels") val channels: Int? = null
)

@Serializable
data class PlaybackInfoRequest(
    @SerialName("UserId") val userId: String,
    @SerialName("DeviceProfile") val deviceProfile: DeviceProfile = DeviceProfile()
)

@Serializable
data class DeviceProfile(
    @SerialName("MaxStaticBitrate") val maxStaticBitrate: Int = 140_000_000,
    @SerialName("MaxStreamingBitrate") val maxStreamingBitrate: Int = 140_000_000,
    @SerialName("DirectPlayProfiles") val directPlayProfiles: List<DirectPlayProfile> = listOf(DirectPlayProfile()),
    @SerialName("TranscodingProfiles") val transcodingProfiles: List<TranscodingProfile> = emptyList()
)

@Serializable
data class DirectPlayProfile(
    @SerialName("Type") val type: String = "Video"
)

@Serializable
data class TranscodingProfile(
    @SerialName("Container") val container: String = "ts",
    @SerialName("Type") val type: String = "Video",
    @SerialName("AudioCodec") val audioCodec: String = "aac",
    @SerialName("VideoCodec") val videoCodec: String = "h264",
    @SerialName("Protocol") val protocol: String = "hls"
)

@Serializable
data class PlaybackInfoResponse(
    @SerialName("MediaSources") val mediaSources: List<MediaSourceDto> = emptyList(),
    @SerialName("PlaySessionId") val playSessionId: String? = null
)

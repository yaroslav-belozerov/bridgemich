package tech.tarakoshka.bridgemich.data.dtos

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Items(
    @SerialName("id") var id: String? = null,
    @SerialName("createdAt") var createdAt: String? = null,
    @SerialName("deviceAssetId") var deviceAssetId: String? = null,
    @SerialName("ownerId") var ownerId: String? = null,
    @SerialName("deviceId") var deviceId: String? = null,
    @SerialName("libraryId") var libraryId: String? = null,
    @SerialName("type") var type: String? = null,
    @SerialName("originalPath") var originalPath: String? = null,
    @SerialName("originalFileName") var originalFileName: String? = null,
    @SerialName("originalMimeType") var originalMimeType: String? = null,
    @SerialName("thumbhash") var thumbhash: String? = null,
    @SerialName("fileCreatedAt") var fileCreatedAt: String? = null,
    @SerialName("fileModifiedAt") var fileModifiedAt: String? = null,
    @SerialName("localDateTime") var localDateTime: String? = null,
    @SerialName("updatedAt") var updatedAt: String? = null,
    @SerialName("isFavorite") var isFavorite: Boolean? = null,
    @SerialName("isArchived") var isArchived: Boolean? = null,
    @SerialName("isTrashed") var isTrashed: Boolean? = null,
    @SerialName("visibility") var visibility: String? = null,
    @SerialName("duration") var duration: String? = null,
    @SerialName("livePhotoVideoId") var livePhotoVideoId: String? = null,
    @SerialName("people") var people: ArrayList<String> = arrayListOf(),
    @SerialName("checksum") var checksum: String? = null,
    @SerialName("isOffline") var isOffline: Boolean? = null,
    @SerialName("hasMetadata") var hasMetadata: Boolean? = null,
    @SerialName("duplicateId") var duplicateId: String? = null,
    @SerialName("resized") var resized: Boolean? = null,
    @SerialName("width") var width: Int? = null,
    @SerialName("height") var height: Int? = null,
    @SerialName("isEdited") var isEdited: Boolean? = null
)
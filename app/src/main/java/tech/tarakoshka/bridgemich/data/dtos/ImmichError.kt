package tech.tarakoshka.bridgemich.data.dtos

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ImmichError(
    @SerialName("message") val message: String,
    @SerialName("error") val error: String,
    @SerialName("statusCode") val statusCode: String
)
